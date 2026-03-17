package edu.zsc.ai.agent.subagent.explorer;

import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.service.TokenStream;
import edu.zsc.ai.agent.subagent.SubAgent;
import edu.zsc.ai.agent.subagent.SubAgentContextBuilder;
import edu.zsc.ai.agent.subagent.SubAgentContext;
import edu.zsc.ai.agent.subagent.SubAgentObservabilityListener;
import edu.zsc.ai.agent.subagent.SubAgentRequest;
import edu.zsc.ai.agent.subagent.SubAgentStreamBridge;
import edu.zsc.ai.agent.subagent.contract.SchemaSummary;
import edu.zsc.ai.config.ai.SubAgentFactory;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import edu.zsc.ai.common.enums.ai.PromptEnum;
import edu.zsc.ai.config.ai.PromptConfig;
import edu.zsc.ai.config.ai.SubAgentProperties;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.domain.model.dto.response.agent.ChatResponseBlock;
import edu.zsc.ai.domain.service.agent.SseEmitterRegistry;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Explorer SubAgent implementation.
 * Uses GetEnvironmentOverviewTool, SearchObjectsTool, GetObjectDetailTool to explore schema and returns SchemaSummary.
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

        String taskId = SubAgentContext.getTaskId();
        log.info("Explorer invoke started: instruction='{}', connectionIds={}, hasContext={}, taskId={}",
                request.instruction(),
                request.connectionIds(),
                request.context() != null,
                taskId);

        String parentToolCallId = SubAgentContext.getParentToolCallId();

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

            InvocationParameters invocationParams = InvocationParameters.from(RequestContext.toMap());
            TokenStream tokenStream = agentService.explore(message, invocationParams);

            StringBuilder fullResponse = new StringBuilder();
            String parentId = SubAgentContext.getParentToolCallId();
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
            } catch (java.util.concurrent.TimeoutException te) {
                log.warn("[Explorer] timed out after {}s waiting for completion", properties.getExplorer().getTimeoutSeconds());
                throw te;
            }

            SchemaSummary summary = ExplorerResponseParser.parse(responseText);
            observer.emitComplete(summary.getSummaryText(), JsonUtil.object2json(summary));
            log.info("Explorer completed: {} object(s) found", summary.getObjects() != null ? summary.getObjects().size() : 0);
            return summary;

        } catch (Exception e) {
            observer.emitError(e.getMessage());
            log.error("Explorer SubAgent failed", e);
            throw new RuntimeException("Explorer SubAgent failed: " + e.getMessage(), e);
        }
    }

    private String buildMessage(SubAgentRequest request) {
        Long connectionId = CollectionUtils.isNotEmpty(request.connectionIds()) ? request.connectionIds().get(0) : null;
        return SubAgentContextBuilder.builder()
                .instruction(request.instruction())
                .connectionId(connectionId)
                .context(request.context())
                .build();
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
        String modelName = RequestContext.getModelName();
        if (StringUtils.isNotBlank(modelName)) {
            return modelName;
        }
        log.warn("No modelName in RequestContext, falling back to default");
        return "qwen3-max";
    }
}
