package edu.zsc.ai.domain.service.ai.impl;

import edu.zsc.ai.agent.memory.MemoryUtil;
import edu.zsc.ai.config.ai.MemoryProperties;
import edu.zsc.ai.domain.model.entity.ai.AiMemoryCandidate;
import edu.zsc.ai.domain.service.ai.MemoryCandidateService;
import edu.zsc.ai.domain.service.ai.MemoryContextService;
import edu.zsc.ai.domain.service.ai.MemoryService;
import edu.zsc.ai.domain.service.ai.model.MemorySearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(MemoryProperties.class)
public class MemoryContextServiceImpl implements MemoryContextService {

    private final MemoryService memoryService;
    private final MemoryCandidateService memoryCandidateService;
    private final MemoryProperties memoryProperties;

    @Override
    public String buildEnrichedMessage(Long userId, Long conversationId, String userMessage) {
        if (!memoryProperties.isEnabled() || Objects.isNull(userId) || Objects.isNull(conversationId)) {
            return userMessage;
        }

        MemoryProperties.Retrieval retrieval = memoryProperties.getRetrieval();

        List<MemorySearchResult> memories = List.of();
        try {
            memories = memoryService.searchActiveMemories(
                    userMessage, retrieval.getPreloadTopK(), retrieval.getMinScore());
        } catch (Exception e) {
            log.warn("Failed to fetch memory context for user {}", userId, e);
        }

        List<AiMemoryCandidate> candidates = List.of();
        try {
            candidates = memoryCandidateService.listCurrentConversationCandidates(
                    conversationId, retrieval.getCandidateTopK());
        } catch (Exception e) {
            log.warn("Failed to fetch candidate context for conversation {}", conversationId, e);
        }

        return MemoryUtil.buildEnrichedMessage(userMessage, memories, candidates);
    }
}
