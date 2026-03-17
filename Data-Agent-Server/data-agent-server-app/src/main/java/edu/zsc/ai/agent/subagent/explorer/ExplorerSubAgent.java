package edu.zsc.ai.agent.subagent.explorer;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.agent.subagent.SubAgent;
import edu.zsc.ai.agent.subagent.SubAgentObservabilityListener;
import edu.zsc.ai.agent.subagent.SubAgentPromptBuilder;
import edu.zsc.ai.agent.subagent.SubAgentRequest;
import edu.zsc.ai.agent.subagent.SubAgentStreamBridge;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.common.constant.InvocationContextConstant;
import edu.zsc.ai.config.ai.SubAgentFactory;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.PromptEnum;
import edu.zsc.ai.config.ai.PromptConfig;
import edu.zsc.ai.config.ai.SubAgentProperties;
import edu.zsc.ai.context.AgentExecutionContext;
import edu.zsc.ai.context.AgentRequestContext;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.service.agent.SseEmitterRegistry;
import edu.zsc.ai.util.ConnectionIdUtil;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Explorer SubAgent implementation.
 * Uses SearchObjectsTool and GetObjectDetailTool to explore schema and returns SchemaSummary.
 * Supports previousError for supplementary exploration. connectionIds: 1 = single Explorer, 2 = concurrent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExplorerSubAgent implements SubAgent<SubAgentRequest, SchemaSummary> {

    private final SubAgentFactory subAgentFactory;
    private final SubAgentProperties properties;
    private final SubAgentStreamBridge streamBridge;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Override
    public AgentTypeEnum getAgentType() {
        return AgentTypeEnum.EXPLORER;
    }

    @Override
    public SchemaSummary invoke(SubAgentRequest request) {
        Long conversationId = parseConversationId();

        String taskId = AgentExecutionContext.getTaskId();
        log.info("Explorer invoke started: instruction='{}', connectionIds={}, hasContext={}, taskId={}",
                request.instruction(),
                request.connectionIds(),
                StringUtils.isNotBlank(request.context()),
                taskId);

        String parentToolCallId = AgentExecutionContext.getParentToolCallId();

        // SSE progress emitter (replaces AgentListener-based observability)
        SubAgentObservabilityListener observer = new SubAgentObservabilityListener(
                AgentTypeEnum.EXPLORER, conversationId, sseEmitterRegistry, null, taskId, parentToolCallId,
                CollectionUtils.isNotEmpty(request.connectionIds()) ? request.connectionIds().get(0) : null);

        observer.emitStart();

        try {
            String message = buildMessage(request);
            String systemPrompt = PromptConfig.getPrompt(PromptEnum.EXPLORER);

            ExplorerAgentService agentService = subAgentFactory.buildExplorerAgent(
                    resolveModelName(), systemPrompt);

            InvocationParameters invocationParams = InvocationParameters.from(buildInvocationContext(request));
            TokenStream tokenStream = agentService.explore(message, invocationParams);

            StringBuilder fullResponse = new StringBuilder();
            String parentId = AgentExecutionContext.getParentToolCallId();
            Sinks.Many<ChatResponseBlock> sink = sseEmitterRegistry.get(conversationId).orElse(null);
            streamBridge.bridge(tokenStream, sink, parentId, null, fullResponse::append);

            CompletableFuture<String> future = new CompletableFuture<>();
            tokenStream.onCompleteResponse(response -> future.complete(fullResponse.toString()));
            tokenStream.onError(error -> future.completeExceptionally(error));
            log.info("[Explorer] starting TokenStream, conversationId={}, parentToolCallId={}", conversationId, parentId);
            tokenStream.start();

            String responseText;
            try {
                responseText = future.get(properties.getExplorer().getTimeoutSeconds(), TimeUnit.SECONDS);
            } catch (TimeoutException te) {
                log.warn("[Explorer] timed out after {}s waiting for completion", properties.getExplorer().getTimeoutSeconds());
                throw te;
            }

            SchemaSummary summary = ExplorerResponseParser.parse(responseText);
            observer.emitComplete(summary.getSummaryText(), JsonUtil.object2json(summary));
            log.info("Explorer completed: {} object(s) found", CollectionUtils.size(summary.getObjects()));
            return summary;

        } catch (Exception e) {
            observer.emitError(e.getMessage());
            log.error("Explorer SubAgent failed", e);
            throw new RuntimeException("Explorer SubAgent failed: " + e.getMessage(), e);
        }
    }

    private String buildMessage(SubAgentRequest request) {
        Long connectionId = CollectionUtils.isNotEmpty(request.connectionIds()) ? request.connectionIds().get(0) : null;
        return SubAgentPromptBuilder.builder()
                .instruction(request.instruction())
                .connectionId(connectionId)
                .allowedConnectionIds(request.connectionIds())
                .context(request.context())
                .build();
    }

    private Map<String, Object> buildInvocationContext(SubAgentRequest request) {
        Map<String, Object> invocationContext = new HashMap<>(RequestContext.toMap());
        invocationContext.putAll(AgentRequestContext.toMap());
        List<Long> allowedConnectionIds = request.connectionIds();
        Long defaultConnectionId = CollectionUtils.isNotEmpty(allowedConnectionIds) ? allowedConnectionIds.get(0) : null;

        invocationContext.put(InvocationContextConstant.AGENT_TYPE, AgentTypeEnum.EXPLORER.getCode());
        if (CollectionUtils.isNotEmpty(allowedConnectionIds)) {
            invocationContext.put(InvocationContextConstant.ALLOWED_CONNECTION_IDS, ConnectionIdUtil.toCsv(allowedConnectionIds));
            invocationContext.put(InvocationContextConstant.CONNECTION_ID, defaultConnectionId);
        } else {
            invocationContext.remove(InvocationContextConstant.ALLOWED_CONNECTION_IDS);
            invocationContext.remove(InvocationContextConstant.CONNECTION_ID);
        }
        return invocationContext;
    }

    private Long parseConversationId() {
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
