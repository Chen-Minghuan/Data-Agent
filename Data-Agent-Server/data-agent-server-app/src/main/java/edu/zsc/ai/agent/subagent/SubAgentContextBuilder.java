package edu.zsc.ai.agent.subagent;

import org.apache.commons.lang3.StringUtils;

/**
 * Builds context message for Explorer SubAgent invocations.
 */
public class SubAgentContextBuilder {

    private String instruction;
    private Long connectionId;
    private String context;

    private SubAgentContextBuilder() {}

    public static SubAgentContextBuilder builder() {
        return new SubAgentContextBuilder();
    }

    public SubAgentContextBuilder instruction(String instruction) {
        this.instruction = instruction;
        return this;
    }

    public SubAgentContextBuilder connectionId(Long connectionId) {
        this.connectionId = connectionId;
        return this;
    }

    public SubAgentContextBuilder context(String context) {
        this.context = context;
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("Instruction: ").append(instruction).append("\n");
        sb.append("Connection: ").append(connectionId != null ? connectionId : "").append("\n");

        if (StringUtils.isNotBlank(context)) {
            sb.append("\nContext:\n").append(context).append("\n");
        }

        return sb.toString();
    }
}
