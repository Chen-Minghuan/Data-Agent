package edu.zsc.ai.agent.tool.think.model.input;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ThinkingRequest {

    @NotBlank(message = "goal must not be blank")
    @JsonPropertyDescription("Current goal: what you want to achieve in this reasoning step.")
    private String goal;

    @NotBlank(message = "analysis must not be blank")
    @JsonPropertyDescription("Free-text analysis: current state, identified gaps/risks, and your plan for next steps.")
    private String analysis;

    @JsonPropertyDescription("Whether this task involves write operations (INSERT/UPDATE/DELETE/DDL). Default false.")
    private boolean isWrite;
}
