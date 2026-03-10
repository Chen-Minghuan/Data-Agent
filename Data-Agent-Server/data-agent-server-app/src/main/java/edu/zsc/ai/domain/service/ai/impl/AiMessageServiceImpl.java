package edu.zsc.ai.domain.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.ChatMessageType;
import edu.zsc.ai.agent.memory.ChatMemoryCompressor;
import edu.zsc.ai.agent.memory.MemoryUtil;
import edu.zsc.ai.common.enums.ai.MessageStatusEnum;
import edu.zsc.ai.domain.mapper.ai.AiMessageMapper;
import edu.zsc.ai.domain.model.entity.ai.StoredChatMessage;
import edu.zsc.ai.domain.service.ai.AiMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, StoredChatMessage>
        implements AiMessageService {

    @Override
    public List<StoredChatMessage> getByConversationIdOrderByCreatedAtAsc(Long conversationId) {
        LambdaQueryWrapper<StoredChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoredChatMessage::getConversationId, conversationId)
                .orderByAsc(StoredChatMessage::getCreatedAt)
                .orderByAsc(StoredChatMessage::getId);
        return list(wrapper);
    }

    @Override
    public void saveBatchMessages(List<StoredChatMessage> messages) {
        saveBatch(messages);
    }

    @Override
    @Transactional
    public int removeByConversationId(Long conversationId) {
        LambdaQueryWrapper<StoredChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoredChatMessage::getConversationId, conversationId);
        int count = (int) count(wrapper);
        if (count == 0) {
            return 0;
        }
        remove(wrapper);
        log.debug("Deleted {} messages for conversation {}", count, conversationId);
        return count;
    }

    @Override
    @Transactional
    public void replaceConversationMessages(Long conversationId, List<ChatMessage> messages) {
        removeByConversationId(conversationId);

        LocalDateTime baseTime = LocalDateTime.now();
        List<StoredChatMessage> toSave = new ArrayList<>(messages.size());

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            ChatMessage normalized = MemoryUtil.normalizeUserMessage(message);

            int status = (i == 0 && ChatMemoryCompressor.isSummaryMessage(message))
                    ? MessageStatusEnum.COMPRESSED.getCode()
                    : MessageStatusEnum.NORMAL.getCode();

            toSave.add(StoredChatMessage.builder()
                    .conversationId(conversationId)
                    .role(normalized.type().name())
                    .tokenCount(0)
                    .status(status)
                    .data(ChatMessageSerializer.messageToJson(normalized))
                    .createdAt(baseTime.plusNanos(i * 1000L))
                    .updatedAt(baseTime)
                    .build());
        }

        saveBatchMessages(toSave);
    }

    @Override
    public void updateLastAiMessageTokenCount(Long conversationId, Integer tokenCount) {
        if (conversationId == null || tokenCount == null) {
            return;
        }

        // Find the last AI message
        LambdaQueryWrapper<StoredChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StoredChatMessage::getConversationId, conversationId)
                .eq(StoredChatMessage::getRole, ChatMessageType.AI.name())
                .orderByDesc(StoredChatMessage::getCreatedAt)
                .orderByDesc(StoredChatMessage::getId)
                .last("LIMIT 1");

        StoredChatMessage lastAiMessage = getOne(queryWrapper);
        if (lastAiMessage == null) {
            log.warn("No AI message found for conversation {}", conversationId);
            return;
        }

        // Update token count
        LambdaUpdateWrapper<StoredChatMessage> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StoredChatMessage::getId, lastAiMessage.getId())
                .set(StoredChatMessage::getTokenCount, tokenCount)
                .set(StoredChatMessage::getUpdatedAt, java.time.LocalDateTime.now());

        boolean updated = update(updateWrapper);
        if (updated) {
            log.info("Updated token count for message {} in conversation {}: {} tokens",
                    lastAiMessage.getId(), conversationId, tokenCount);
        }
    }
}
