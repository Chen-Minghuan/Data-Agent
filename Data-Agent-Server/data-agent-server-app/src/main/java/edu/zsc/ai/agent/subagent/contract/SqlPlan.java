package edu.zsc.ai.agent.subagent.contract;

import lombok.Builder;
import lombok.Data;

/**
 * Planner SubAgent return structure.
 * rawResponse is the full natural language response from LLM, MainAgent reads it directly.
 */
@Data
@Builder
public class SqlPlan {
    /** Full LLM response, consumed directly by MainAgent */
    private String rawResponse;
    private int tokenUsage;
}
