package edu.zsc.ai.domain.service.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemorySearchResult {

    private Long id;

    private String memoryType;

    private String content;

    private double score;

    private Long conversationId;
}
