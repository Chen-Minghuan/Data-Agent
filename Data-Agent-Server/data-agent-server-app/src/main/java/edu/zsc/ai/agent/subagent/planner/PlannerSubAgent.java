package edu.zsc.ai.agent.subagent.planner;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.agent.subagent.SubAgent;
import edu.zsc.ai.agent.subagent.SubAgentObservabilityListener;
import edu.zsc.ai.agent.subagent.SubAgentStreamBridge;
import edu.zsc.ai.agent.subagent.contract.*;
import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.PromptEnum;
import edu.zsc.ai.config.ai.SubAgentFactory;
import edu.zsc.ai.config.ai.PromptConfig;
import edu.zsc.ai.config.ai.SubAgentProperties;
import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.RequestContext;
import org.apache.commons.lang3.StringUtils;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.service.agent.SseEmitterRegistry;
import edu.zsc.ai.util.JsonUtil;
import reactor.core.publisher.Sinks;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Planner SubAgent implementation.
 * Uses TodoTool for task tracking,
 * and ActivateSkillTool for optional SQL optimization.
 * Accepts SchemaSummary + user question, returns structured SqlPlan.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlannerSubAgent implements SubAgent<PlannerRequest, SqlPlan> {

    private final SubAgentFactory subAgentFactory;
    private final SubAgentProperties properties;
    private final SubAgentStreamBridge streamBridge;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.PLANNER;
    }

    @Override
    public SqlPlan invoke(PlannerRequest request) {
        Long conversationId = resolveConversationId();

        String parentToolCallId = AgentExecutionContext.getParentToolCallId();
        String taskId = AgentExecutionContext.getTaskId();

        // SSE progress emitter (replaces AgentListener-based observability)
        SubAgentObservabilityListener observer = new SubAgentObservabilityListener(
                AgentTypeEnum.PLANNER, conversationId, sseEmitterRegistry, null, taskId, parentToolCallId);
        log.info("Planner invoke started: instruction='{}', objectCount={}",
                request.getInstruction(),
                request.getSchemaSummary() != null && CollectionUtils.isNotEmpty(request.getSchemaSummary().getObjects())
                    ? request.getSchemaSummary().getObjects().size() : 0);

        observer.emitStart();

        try {
            String message = buildMessage(request);
            String systemPrompt = PromptConfig.getPrompt(PromptEnum.PLANNER);

            // Build fresh agent per invocation with AgentBuilder (via SubAgentFactory)
            PlannerAgentService agentService = subAgentFactory.buildPlannerAgent(
                    resolveModelName(), systemPrompt);

            HashMap<String, Object> invocationContext = new HashMap<>(RequestContext.toMap());
            invocationContext.putAll(AgentRequestContext.toMap());
            invocationContext.put(InvocationContextConstant.AGENT_TYPE, AgentTypeEnum.PLANNER.getCode());
            InvocationParameters invocationParams = InvocationParameters.from(invocationContext);
            TokenStream tokenStream = agentService.plan(message, invocationParams);
            log.info("[Planner] starting TokenStream, messageLength={}, model={}", message.length(), resolveModelName());

            StringBuilder fullResponse = new StringBuilder();
            String parentId = AgentExecutionContext.getParentToolCallId();
            Sinks.Many<ChatResponseBlock> sink = sseEmitterRegistry.get(conversationId).orElse(null);
            streamBridge.bridge(tokenStream, sink, parentId, null, fullResponse::append);

            CompletableFuture<String> future = new CompletableFuture<>();
            tokenStream.onCompleteResponse(response -> future.complete(fullResponse.toString()));
            tokenStream.onError(error -> future.completeExceptionally(error));
            tokenStream.start();

            String responseText = future.get(properties.getPlanner().getTimeoutSeconds(), TimeUnit.SECONDS);
            SqlPlan plan = PlannerResponseParser.parse(responseText);

            observer.emitComplete(plan.getSummaryText(), JsonUtil.object2json(plan));
            log.info("[Planner] completed, sqlBlockCount={}, stepCount={}",
                    CollectionUtils.size(plan.getSqlBlocks()),
                    CollectionUtils.size(plan.getPlanSteps()));
            return plan;

        } catch (TimeoutException e) {
            observer.emitError(e.getMessage());
            log.warn("[Planner] timed out after {}s", properties.getPlanner().getTimeoutSeconds(), e);
            throw new RuntimeException("Planner SubAgent timed out: " + e.getMessage(), e);
        } catch (Exception e) {
            observer.emitError(e.getMessage());
            log.error("[Planner] SubAgent failed", e);
            throw new RuntimeException("Planner SubAgent failed: " + e.getMessage(), e);
        }
    }

    private String buildMessage(PlannerRequest request) {
        StringBuilder sb = new StringBuilder();

        sb.append("## Instruction\n");
        sb.append(request.getInstruction()).append("\n\n");

        sb.append("## Schema Summary\n");
        sb.append(serializeSchemaSummary(request.getSchemaSummary())).append("\n");

        return sb.toString();
    }

    private String serializeSchemaSummary(SchemaSummary summary) {
        if (summary == null) {
            return "(no schema information available)\n";
        }

        StringBuilder sb = new StringBuilder();
        if (CollectionUtils.isNotEmpty(summary.getObjects())) {
            for (ExploreObject object : summary.getObjects()) {
                sb.append("### ").append(object.getObjectName());
                if (StringUtils.isNotBlank(object.getObjectType())) {
                    sb.append(" [").append(object.getObjectType()).append("]");
                }
                if (StringUtils.isNotBlank(object.getRelevance())) {
                    sb.append(" {relevance=").append(object.getRelevance()).append("}");
                }
                sb.append("\n");

                if (StringUtils.isNotBlank(object.getCatalog())) {
                    sb.append("Catalog: ").append(object.getCatalog()).append("\n");
                }
                if (StringUtils.isNotBlank(object.getSchema())) {
                    sb.append("Schema: ").append(object.getSchema()).append("\n");
                }
                if (StringUtils.isNotBlank(object.getObjectDdl())) {
                    sb.append("DDL:\n");
                    sb.append(object.getObjectDdl()).append("\n");
                }

                sb.append("\n");
            }
        } else {
            sb.append("(no structured objects available)\n\n");
        }
        if (StringUtils.isNotBlank(summary.getRawResponse())) {
            sb.append("## Explorer Raw Response\n");
            sb.append(summary.getRawResponse()).append("\n");
        }
        return sb.toString();
    }

    private Long resolveConversationId() {
        try {
            Long id = RequestContext.getConversationId();
            return id != null ? id : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private String resolveModelName() {
        String modelName = AgentRequestContext.getModelName();
        if (StringUtils.isNotBlank(modelName)) {
            return modelName;
        }
        log.warn("No modelName in AgentRequestContext, falling back to default");
        return "qwen3-max";
    }
}
