package edu.zsc.ai.converter;

import edu.zsc.ai.model.dto.response.ConversationResponse;
import edu.zsc.ai.model.entity.AiConversation;

/**
 * Converter for conversation entity and response DTO
 *
 * @author zgq
 */
public class ConversationConverter {

    /**
     * Convert conversation entity to response DTO
     *
     * @param conversation conversation entity
     * @return conversation response DTO
     */
    public static ConversationResponse toResponse(AiConversation conversation) {
        if (conversation == null) {
            return null;
        }

        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setUserId(conversation.getUserId());
        response.setTitle(conversation.getTitle());
        response.setTokenCount(conversation.getTokenCount());
        response.setCreatedAt(conversation.getCreatedAt());
        response.setUpdatedAt(conversation.getUpdatedAt());

        return response;
    }
}