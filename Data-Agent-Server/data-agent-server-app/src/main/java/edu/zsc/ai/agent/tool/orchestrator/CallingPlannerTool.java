package edu.zsc.ai.agent.tool.orchestrator;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.subagent.contract.ExplorerResultEnvelope;
import edu.zsc.ai.agent.subagent.contract.ExplorerTaskResult;
import edu.zsc.ai.agent.subagent.contract.ExplorerTaskStatus;
import edu.zsc.ai.agent.subagent.contract.ExploreObject;
import edu.zsc.ai.agent.subagent.contract.PlannerRequest;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.agent.subagent.contract.SqlPlan;
import edu.zsc.ai.agent.tool.ToolContext;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.config.ai.SubAgentManager;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Delegates SQL plan generation to Planner SubAgent.
 * Trace/span management is handled by SubAgentObservabilityListener (AgentListener).
 */
@AgentTool
@Slf4j
@RequiredArgsConstructor
public class CallingPlannerTool extends SubAgentToolSupport {

    private final SubAgentManager subAgentManager;

    @Tool({
            "Delegates SQL plan generation to Planner SubAgent.",
            "Use when: you already have schema context from callingExplorerSubAgent and need to produce SQL.",
            "Accepts either a SchemaSummary JSON or callingExplorerSubAgent taskResults envelope JSON.",
            "Returns: SqlPlan JSON with summaryText, planSteps, sqlBlocks, and rawResponse.",
            "Include optimization context (existing SQL, DDLs, indexes) in instruction if needed."
    })
    public AgentToolResult callingPlannerSubAgent(
            @P("Task instruction - describe what SQL to generate, include optimization context if needed") String instruction,
            @P("SchemaSummary JSON from a previous callingExplorerSubAgent result") String schemaSummaryJson,
            InvocationParameters parameters) {

        try (var ctx = ToolContext.from(parameters)) {
            log.info("[Tool] callingPlannerSubAgent, instruction={}", instruction);
            return invokePlanner(instruction, schemaSummaryJson);
        } catch (Exception e) {
            log.error("[Tool error] callingPlannerSubAgent", e);
            return AgentToolResult.fail("callingPlannerSubAgent failed: " + e.getMessage());
        }
    }

    private AgentToolResult invokePlanner(String instruction, String schemaSummaryJson) {
        if (schemaSummaryJson == null || schemaSummaryJson.isBlank()) {
            return AgentToolResult.fail("schemaSummaryJson is required for callingPlannerSubAgent. Call callingExplorerSubAgent first to get schema.");
        }

        SchemaSummary schemaSummary;
        try {
            schemaSummary = parseSchemaSummary(schemaSummaryJson);
        } catch (Exception e) {
            return AgentToolResult.fail("Failed to parse schemaSummaryJson: " + e.getMessage());
        }
        if ((schemaSummary.getObjects() == null || schemaSummary.getObjects().isEmpty())
                && (schemaSummary.getRawResponse() == null || schemaSummary.getRawResponse().isBlank())
                && (schemaSummary.getSummaryText() == null || schemaSummary.getSummaryText().isBlank())) {
            return AgentToolResult.fail("schemaSummaryJson does not contain any successful explorer task results. Call callingExplorerSubAgent again or fix the failed tasks first.");
        }

        PlannerRequest request = PlannerRequest.builder()
                .instruction(instruction)
                .schemaSummary(schemaSummary)
                .build();

        log.debug("[Planner] request built, schemaContext length={}", schemaSummaryJson.length());

        SqlPlan plan = subAgentManager.getPlannerSubAgent().invoke(request);

        log.info("[Tool done] callingPlannerSubAgent: response length={}",
                plan.getRawResponse() != null ? plan.getRawResponse().length() : 0);
        return AgentToolResult.success(JsonUtil.object2json(plan));
    }

    private SchemaSummary parseSchemaSummary(String schemaSummaryJson) {
        try {
            ExplorerResultEnvelope envelope = JsonUtil.json2Object(schemaSummaryJson, ExplorerResultEnvelope.class);
            if (envelope != null && envelope.getTaskResults() != null && !envelope.getTaskResults().isEmpty()) {
                List<ExploreObject> mergedObjects = new java.util.ArrayList<>();
                StringBuilder summaryText = new StringBuilder();
                StringBuilder rawResponse = new StringBuilder();
                for (ExplorerTaskResult taskResult : envelope.getTaskResults()) {
                    if (taskResult.getStatus() == ExplorerTaskStatus.ERROR) {
                        continue;
                    }
                    if (taskResult.getObjects() != null) {
                        mergedObjects.addAll(taskResult.getObjects());
                    }
                    if (taskResult.getSummaryText() != null && !taskResult.getSummaryText().isBlank()) {
                        if (summaryText.length() > 0) summaryText.append("\n");
                        summaryText.append(taskResult.getSummaryText());
                    }
                    if (taskResult.getRawResponse() != null && !taskResult.getRawResponse().isBlank()) {
                        if (rawResponse.length() > 0) rawResponse.append("\n\n");
                        rawResponse.append(taskResult.getRawResponse());
                    }
                }
                return SchemaSummary.builder()
                        .summaryText(summaryText.toString())
                        .rawResponse(rawResponse.toString())
                        .objects(mergedObjects)
                        .build();
            }
        } catch (Exception ignored) {
            // Fall through to plain SchemaSummary parsing.
        }
        return JsonUtil.json2Object(schemaSummaryJson, SchemaSummary.class);
    }
}
