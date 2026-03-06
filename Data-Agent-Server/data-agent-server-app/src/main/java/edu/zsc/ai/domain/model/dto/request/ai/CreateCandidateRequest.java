package edu.zsc.ai.domain.model.dto.request.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCandidateRequest {

    @NotNull(message = "conversationId is required")
    private Long conversationId;

    @NotBlank(message = "candidateType is required")
    private String candidateType;

    @NotBlank(message = "candidateContent is required")
    @Size(max = 4000, message = "candidateContent must not exceed 4000 characters")
    private String candidateContent;

    @Size(max = 2000, message = "reason must not exceed 2000 characters")
    private String reason;
}
