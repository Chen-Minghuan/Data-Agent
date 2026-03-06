package edu.zsc.ai.agent.tool.guard;

import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;

/**
 * Guard utility that prevents execution-class tools from running in Plan mode.
 */
public final class AgentModeGuard {

    private AgentModeGuard() {
    }

    /**
     * Throws IllegalStateException if the current mode is PLAN.
     * Call at the top of any tool method that must be blocked in Plan mode.
     */
    public static void assertNotPlanMode(InvocationParameters parameters, String toolName) {
        String mode = parameters.get(RequestContextConstant.AGENT_MODE);
        if (AgentModeEnum.PLAN.getCode().equalsIgnoreCase(mode)) {
            throw new IllegalStateException(
                    toolName + " is not available in Plan mode. "
                            + "Include the SQL in your plan via exitPlanMode instead.");
        }
    }
}
