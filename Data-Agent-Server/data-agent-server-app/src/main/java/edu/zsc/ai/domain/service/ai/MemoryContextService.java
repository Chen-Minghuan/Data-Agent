package edu.zsc.ai.domain.service.ai;

public interface MemoryContextService {

    /**
     * Enriches the user message with relevant memory and candidate context.
     * Returns the original message if memory is disabled or parameters are null.
     */
    String buildEnrichedMessage(Long userId, Long conversationId, String userMessage);
}
