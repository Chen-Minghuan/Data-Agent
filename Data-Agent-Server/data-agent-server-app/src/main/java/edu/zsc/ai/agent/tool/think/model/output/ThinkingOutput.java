package edu.zsc.ai.agent.tool.think.model.output;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.util.List;

@Data
public class ThinkingOutput {

    @JsonPropertyDescription("Structured reasoning summary extracted from analysis.")
    private String summary;

    @JsonPropertyDescription("Suggested next action to take.")
    private String nextAction;

    @JsonPropertyDescription("Identified risks or concerns.")
    private List<String> risks;
}
