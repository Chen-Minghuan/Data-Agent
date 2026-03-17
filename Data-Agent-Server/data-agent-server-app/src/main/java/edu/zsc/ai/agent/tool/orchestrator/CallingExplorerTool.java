package edu.zsc.ai.agent.tool.orchestrator;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.subagent.SubAgentContext;
import edu.zsc.ai.agent.subagent.SubAgentRequest;
import edu.zsc.ai.agent.subagent.contract.ExplorerResultEnvelope;
import edu.zsc.ai.agent.subagent.contract.ExplorerTask;
import edu.zsc.ai.agent.subagent.contract.ExplorerTaskResult;
import edu.zsc.ai.agent.subagent.contract.ExplorerTaskStatus;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.agent.tool.ToolContext;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.config.ai.SubAgentManager;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.context.RequestContextInfo;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Delegates schema exploration to Explorer SubAgent(s).
 * Accepts a list of ExplorerTask — each task spawns one Explorer SubAgent.
 * Multiple tasks run concurrently; results are returned task-by-task to MainAgent.
 */
@AgentTool
@Slf4j
@RequiredArgsConstructor
public class CallingExplorerTool extends SubAgentToolSupport {

    private final SubAgentManager subAgentManager;

    @Tool({
            "Delegates schema exploration to Explorer SubAgent(s).",
            "Use when: you need to understand database structure before generating SQL.",
            "Accepts a JSON array of tasks. Each task has: connectionId (required), instruction (required), context (optional).",
            "Each task spawns one Explorer SubAgent. Multiple tasks run concurrently.",
            "Returns: JSON object with taskResults[]. Each taskResult includes taskId, summaryText, objects, rawResponse.",
            "connectionId from getEnvironmentOverview.",
            "Flow: callingExplorerSubAgent -> confirm with user -> callingPlannerSubAgent -> confirm -> execute."
    })
    public AgentToolResult callingExplorerSubAgent(
            @P("JSON array of explorer tasks. Each element: {connectionId: number, instruction: string, context?: string}") String tasksJson,
            @P(value = "Optional timeout in seconds for each SubAgent. Defaults to configured timeout if not provided.", required = false) Long timeoutSeconds,
            InvocationParameters parameters) {

        try (var ctx = ToolContext.from(parameters)) {
            List<ExplorerTask> tasks = parseTasks(tasksJson);
            if (CollectionUtils.isEmpty(tasks)) {
                return AgentToolResult.fail("tasks is required. Provide a JSON array of {connectionId, instruction, context?}.");
            }

            log.info("[Tool] callingExplorerSubAgent, {} task(s), timeoutSeconds={}", tasks.size(), timeoutSeconds);

            if (tasks.size() == 1) {
                return invokeSingle(tasks.get(0), timeoutSeconds);
            }

            return invokeConcurrent(tasks, timeoutSeconds);
        } catch (Exception e) {
            log.error("[Tool error] callingExplorerSubAgent", e);
            return AgentToolResult.fail("callingExplorerSubAgent failed: " + e.getMessage());
        }
    }

    private List<ExplorerTask> parseTasks(String tasksJson) {
        if (StringUtils.isBlank(tasksJson)) return null;
        try {
            return JsonUtil.json2List(tasksJson, ExplorerTask.class);
        } catch (Exception e) {
            log.warn("[Tool] Failed to parse tasks JSON: {}", e.getMessage());
            return null;
        }
    }

    private AgentToolResult invokeSingle(ExplorerTask task, Long timeoutSeconds) {
        RequestContextInfo contextSnapshot = RequestContext.get();
        ExplorerTaskResult result = executeTask(
                task,
                timeoutSeconds,
                contextSnapshot,
                SubAgentContext.getParentToolCallId(),
                buildTaskId(contextSnapshot)
        );
        int objectCount = result.getObjects() != null ? result.getObjects().size() : 0;
        log.info("[Tool done] callingExplorerSubAgent: status={}, {} objects", result.getStatus(), objectCount);
        return AgentToolResult.success(JsonUtil.object2json(ExplorerResultEnvelope.builder()
                .taskResults(List.of(result))
                .build()));
    }

    private AgentToolResult invokeConcurrent(List<ExplorerTask> tasks, Long timeoutSeconds) {
        RequestContextInfo contextSnapshot = RequestContext.get();
        String parentToolCallId = SubAgentContext.getParentToolCallId();
        long timeout = timeoutSeconds != null && timeoutSeconds > 0
                ? timeoutSeconds
                : subAgentManager.getProperties().getExplorer().getTimeoutSeconds();

        List<CompletableFuture<ExplorerTaskResult>> futures = IntStream.range(0, tasks.size())
                .mapToObj(i -> {
                    ExplorerTask task = tasks.get(i);
                    String taskId = buildTaskId(contextSnapshot);
                    return CompletableFuture.supplyAsync(() -> executeTask(task, timeoutSeconds, contextSnapshot, parentToolCallId, taskId));
                })
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(timeout + 30, TimeUnit.SECONDS); // extra 30s grace for orchestration overhead

            List<ExplorerTaskResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            long successCount = results.stream().filter(result -> result.getStatus() == ExplorerTaskStatus.SUCCESS).count();
            long errorCount = results.size() - successCount;
            log.info("[Tool done] callingExplorerSubAgent (concurrent): {} success, {} error", successCount, errorCount);
            return AgentToolResult.success(JsonUtil.object2json(ExplorerResultEnvelope.builder()
                    .taskResults(results)
                    .build()));

        } catch (java.util.concurrent.TimeoutException e) {
            log.error("[Explorer] concurrent execution timed out after {}s", timeout);
            return AgentToolResult.fail("Explorer timed out after " + timeout + "s");
        } catch (Exception e) {
            log.error("[Tool error] concurrent Explorer failed", e);
            return AgentToolResult.fail("Concurrent Explorer failed: " + e.getMessage());
        }
    }

    private ExplorerTaskResult executeTask(ExplorerTask task, Long timeoutSeconds, RequestContextInfo contextSnapshot,
                                           String parentToolCallId, String taskId) {
        log.info("[Explorer] task started, connectionId={}", task.getConnectionId());
        RequestContextInfo previousContext = RequestContext.get();
        if (contextSnapshot != null) {
            RequestContext.set(contextSnapshot);
        }
        String previousParentToolCallId = SubAgentContext.getParentToolCallId();
        String previousTaskId = SubAgentContext.getTaskId();
        SubAgentContext.setParentToolCallId(parentToolCallId);
        SubAgentContext.setTaskId(taskId);
        try {
            SubAgentRequest request = new SubAgentRequest(
                    task.getInstruction(),
                    List.of(task.getConnectionId()),
                    task.getContext());
            SchemaSummary summary = subAgentManager.getExplorerSubAgent().invoke(request);
            return ExplorerTaskResult.builder()
                    .taskId(taskId)
                    .status(ExplorerTaskStatus.SUCCESS)
                    .summaryText(summary != null ? summary.getSummaryText() : null)
                    .objects(summary != null ? summary.getObjects() : List.of())
                    .rawResponse(summary != null ? summary.getRawResponse() : "")
                    .build();
        } catch (Exception e) {
            String errorMessage = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
            log.warn("[Explorer] task failed, connectionId={}, error={}", task.getConnectionId(), errorMessage);
            return ExplorerTaskResult.builder()
                    .taskId(taskId)
                    .status(ExplorerTaskStatus.ERROR)
                    .objects(List.of())
                    .rawResponse("")
                    .errorMessage(errorMessage)
                    .build();
        } finally {
            SubAgentContext.setParentToolCallId(previousParentToolCallId);
            SubAgentContext.setTaskId(previousTaskId);
            if (previousContext != null) {
                RequestContext.set(previousContext);
            } else if (contextSnapshot != null) {
                RequestContext.clear();
            }
        }
    }

    private String buildTaskId(RequestContextInfo contextSnapshot) {
        return "explore-" + (contextSnapshot != null ? contextSnapshot.getConversationId() : "0")
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
