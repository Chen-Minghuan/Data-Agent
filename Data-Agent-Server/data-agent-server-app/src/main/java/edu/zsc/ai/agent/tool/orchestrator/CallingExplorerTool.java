package edu.zsc.ai.agent.tool.orchestrator;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.subagent.SubAgentRequest;
import edu.zsc.ai.agent.subagent.contract.ExplorerResultEnvelope;
import edu.zsc.ai.agent.subagent.contract.ExplorerTask;
import edu.zsc.ai.agent.subagent.contract.ExplorerTaskResult;
import edu.zsc.ai.agent.subagent.contract.ExplorerTaskStatus;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.config.ai.SubAgentManager;
import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.AgentRequestContextInfo;
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
import java.util.concurrent.TimeoutException;
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
        List<ExplorerTask> tasks = parseTasks(tasksJson);
        if (CollectionUtils.isEmpty(tasks)) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                    "tasks is required. Provide a JSON array of {connectionId, instruction, context?}."
            );
        }

        log.info("[Tool] callingExplorerSubAgent, {} task(s), timeoutSeconds={}", tasks.size(), timeoutSeconds);

        if (tasks.size() == 1) {
            return invokeSingle(tasks.get(0), timeoutSeconds);
        }

        return invokeConcurrent(tasks, timeoutSeconds);
    }

    private List<ExplorerTask> parseTasks(String tasksJson) {
        if (StringUtils.isBlank(tasksJson)) {
            return null;
        }
        try {
            return JsonUtil.json2List(tasksJson, ExplorerTask.class);
        } catch (Exception e) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                    "tasksJson must be a valid JSON array of {connectionId, instruction, context?}."
            );
        }
    }

    private AgentToolResult invokeSingle(ExplorerTask task, Long timeoutSeconds) {
        RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
        AgentRequestContextInfo agentRequestContextSnapshot = AgentRequestContext.snapshot();
        ExplorerTaskResult result = executeTask(
                task,
                timeoutSeconds,
                requestContextSnapshot,
                agentRequestContextSnapshot,
                AgentExecutionContext.getParentToolCallId(),
                buildTaskId(requestContextSnapshot)
        );
        int objectCount = CollectionUtils.size(result.getObjects());
        log.info("[Tool done] callingExplorerSubAgent: status={}, {} objects", result.getStatus(), objectCount);
        return AgentToolResult.success(JsonUtil.object2json(ExplorerResultEnvelope.builder()
                .taskResults(List.of(result))
                .build()));
    }

    private AgentToolResult invokeConcurrent(List<ExplorerTask> tasks, Long timeoutSeconds) {
        RequestContextInfo requestContextSnapshot = RequestContext.snapshot();
        AgentRequestContextInfo agentRequestContextSnapshot = AgentRequestContext.snapshot();
        String parentToolCallId = AgentExecutionContext.getParentToolCallId();
        long timeout = timeoutSeconds != null && timeoutSeconds > 0
                ? timeoutSeconds
                : subAgentManager.getProperties().getExplorer().getTimeoutSeconds();

        List<CompletableFuture<ExplorerTaskResult>> futures = IntStream.range(0, tasks.size())
                .mapToObj(i -> {
                    ExplorerTask task = tasks.get(i);
                    String taskId = buildTaskId(requestContextSnapshot);
                    return CompletableFuture.supplyAsync(() -> executeTask(
                            task, timeoutSeconds, requestContextSnapshot, agentRequestContextSnapshot, parentToolCallId, taskId));
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

        } catch (TimeoutException e) {
            throw AgentToolExecuteException.executionFailed(
                    ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                    "Explorer timed out after " + timeout + "s",
                    "Concurrent Explorer timed out after " + timeout + "s",
                    true,
                    e
            );
        } catch (Exception e) {
            throw AgentToolExecuteException.executionFailed(
                    ToolNameEnum.CALLING_EXPLORER_SUB_AGENT,
                    "Concurrent Explorer failed: " + StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()),
                    "Concurrent Explorer orchestration failed",
                    true,
                    e
            );
        }
    }

    private ExplorerTaskResult executeTask(ExplorerTask task, Long timeoutSeconds,
                                           RequestContextInfo requestContextSnapshot,
                                           AgentRequestContextInfo agentRequestContextSnapshot,
                                           String parentToolCallId, String taskId) {
        log.info("[Explorer] task started, connectionId={}", task.getConnectionId());
        RequestContextInfo previousRequestContext = RequestContext.snapshot();
        AgentRequestContextInfo previousAgentRequestContext = AgentRequestContext.snapshot();
        if (requestContextSnapshot != null) {
            RequestContext.set(requestContextSnapshot);
        } else {
            RequestContext.clear();
        }
        if (agentRequestContextSnapshot != null) {
            AgentRequestContext.set(agentRequestContextSnapshot);
        } else {
            AgentRequestContext.clear();
        }
        String previousParentToolCallId = AgentExecutionContext.getParentToolCallId();
        String previousTaskId = AgentExecutionContext.getTaskId();
        AgentExecutionContext.setParentToolCallId(parentToolCallId);
        AgentExecutionContext.setTaskId(taskId);
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
            AgentExecutionContext.setParentToolCallId(previousParentToolCallId);
            AgentExecutionContext.setTaskId(previousTaskId);
            if (previousRequestContext != null) {
                RequestContext.set(previousRequestContext);
            } else {
                RequestContext.clear();
            }
            if (previousAgentRequestContext != null) {
                AgentRequestContext.set(previousAgentRequestContext);
            } else {
                AgentRequestContext.clear();
            }
        }
    }

    private String buildTaskId(RequestContextInfo requestContextSnapshot) {
        return "explore-" + (requestContextSnapshot != null ? requestContextSnapshot.getConversationId() : "0")
                + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
