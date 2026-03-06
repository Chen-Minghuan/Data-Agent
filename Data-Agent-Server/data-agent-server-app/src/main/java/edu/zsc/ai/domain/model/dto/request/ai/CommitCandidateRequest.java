package edu.zsc.ai.domain.model.dto.request.ai;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommitCandidateRequest {

    @NotNull(message = "conversationId is required")
    private Long conversationId;

    @NotEmpty(message = "candidateIds cannot be empty")
    @Size(max = 50, message = "candidateIds must not exceed 50 items")
    private List<Long> candidateIds;
}
