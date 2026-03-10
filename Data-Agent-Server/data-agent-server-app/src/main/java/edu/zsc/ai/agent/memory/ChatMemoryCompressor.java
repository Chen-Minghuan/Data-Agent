package edu.zsc.ai.agent.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import edu.zsc.ai.common.enums.ai.ModelEnum;
import edu.zsc.ai.domain.event.MemoryCompressionStartedEvent;
import edu.zsc.ai.domain.model.entity.ai.AiConversation;
import edu.zsc.ai.domain.service.ai.AiConversationService;
import edu.zsc.ai.domain.service.ai.CompressionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMemoryCompressor {

    static final String SUMMARY_PREFIX = "[CONVERSATION_SUMMARY]\n";

    private static final double COMPRESSION_RATIO = 0.75;
    private static final int MIN_MESSAGES_FOR_COMPRESSION = 4;

    private final AiConversationService aiConversationService;
    private final CompressionService compressionService;
    private final ApplicationEventPublisher eventPublisher;

    private final Set<Long> compressingConversations = ConcurrentHashMap.newKeySet();

    /**
     * Compresses the given messages if the conversation's accumulated token count
     * exceeds the model's memory threshold.
     *
     * @return compressed message list, or the original list if compression was not needed / failed
     */
    public List<ChatMessage> compressIfNeeded(Long conversationId, String modelName, List<ChatMessage> messages) {
        if (modelName == null || messages.size() < MIN_MESSAGES_FOR_COMPRESSION) {
            return messages;
        }

        if (!isTokenThresholdExceeded(conversationId, modelName)) {
            return messages;
        }

        if (!compressingConversations.add(conversationId)) {
            return messages;
        }

        try {
            eventPublisher.publishEvent(new MemoryCompressionStartedEvent(this, conversationId));
            return doCompress(conversationId, messages);
        } catch (Exception e) {
            log.warn("Compression failed for conversation {}, keeping original messages", conversationId, e);
            return messages;
        } finally {
            compressingConversations.remove(conversationId);
        }
    }

    public static boolean isSummaryMessage(ChatMessage message) {
        return message instanceof UserMessage um && um.singleText().startsWith(SUMMARY_PREFIX);
    }

    private boolean isTokenThresholdExceeded(Long conversationId, String modelName) {
        AiConversation conversation = aiConversationService.getById(conversationId);
        if (conversation == null || conversation.getTokenCount() == null) {
            return false;
        }

        int threshold = resolveMemoryThreshold(modelName);
        boolean exceeded = conversation.getTokenCount() >= threshold;
        if (exceeded) {
            log.info("Conversation {} token count ({}) >= threshold ({}) for model '{}', triggering compression",
                    conversationId, conversation.getTokenCount(), threshold, modelName);
        }
        return exceeded;
    }

    private List<ChatMessage> doCompress(Long conversationId, List<ChatMessage> messages) {
        int splitIndex = findCleanSplitPoint(messages,
                (int) Math.ceil(messages.size() * COMPRESSION_RATIO));

        List<ChatMessage> toCompress = messages.subList(0, splitIndex);
        List<ChatMessage> toKeep = messages.subList(splitIndex, messages.size());

        String summary = compressionService.compress(toCompress);
        log.info("Conversation {}: compressed {} messages into summary ({} chars), keeping {} recent",
                conversationId, toCompress.size(), summary.length(), toKeep.size());

        List<ChatMessage> result = new ArrayList<>(toKeep.size() + 1);
        result.add(UserMessage.from(SUMMARY_PREFIX + summary));
        result.addAll(toKeep);
        return result;
    }

    /**
     * Scans backward from targetIndex to find a clean split point — a UserMessage
     * or a standalone AiMessage (no tool execution requests) — to avoid splitting
     * a tool-call / tool-result pair.
     */
    private int findCleanSplitPoint(List<ChatMessage> messages, int targetIndex) {
        for (int i = targetIndex; i > 0; i--) {
            ChatMessage msg = messages.get(i);
            if (msg.type() == ChatMessageType.USER) {
                return i;
            }
            if (msg.type() == ChatMessageType.AI && !((AiMessage) msg).hasToolExecutionRequests()) {
                return i;
            }
        }
        return targetIndex;
    }

    private int resolveMemoryThreshold(String modelName) {
        try {
            return ModelEnum.fromModelName(modelName).getMemoryThreshold();
        } catch (IllegalArgumentException e) {
            log.warn("Unknown model '{}', falling back to QWEN3_MAX threshold", modelName);
            return ModelEnum.QWEN3_MAX.getMemoryThreshold();
        }
    }
}
