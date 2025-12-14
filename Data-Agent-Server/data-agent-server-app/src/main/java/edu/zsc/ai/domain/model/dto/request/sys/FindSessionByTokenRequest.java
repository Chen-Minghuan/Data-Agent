package edu.zsc.ai.domain.model.dto.request.sys;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FindSessionByTokenRequest {
    @NotBlank
    private String accessToken;

    @NotNull
    private Long userId;
}
