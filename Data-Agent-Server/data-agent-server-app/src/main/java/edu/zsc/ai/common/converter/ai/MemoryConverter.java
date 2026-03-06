package edu.zsc.ai.common.converter.ai;

import edu.zsc.ai.domain.model.dto.response.ai.MemoryCandidateResponse;
import edu.zsc.ai.domain.model.dto.response.ai.MemoryResponse;
import edu.zsc.ai.domain.model.entity.ai.AiMemory;
import edu.zsc.ai.domain.model.entity.ai.AiMemoryCandidate;

public final class MemoryConverter {

    private MemoryConverter() {
    }

    public static MemoryCandidateResponse toCandidateResponse(AiMemoryCandidate candidate) {
        if (candidate == null) {
            return null;
        }
        return MemoryCandidateResponse.builder()
                .id(candidate.getId())
                .conversationId(candidate.getConversationId())
                .candidateType(candidate.getCandidateType())
                .candidateContent(candidate.getCandidateContent())
                .reason(candidate.getReason())
                .createdAt(candidate.getCreatedAt())
                .build();
    }

    public static MemoryResponse toMemoryResponse(AiMemory memory) {
        if (memory == null) {
            return null;
        }
        return MemoryResponse.builder()
                .id(memory.getId())
                .status(memory.getStatus())
                .createdAt(memory.getCreatedAt())
                .build();
    }
}
