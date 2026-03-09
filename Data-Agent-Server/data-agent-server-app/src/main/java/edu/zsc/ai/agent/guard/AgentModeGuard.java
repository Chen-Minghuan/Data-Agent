package edu.zsc.ai.agent.guard;

import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.context.RequestContext;

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
    public static void assertNotPlanMode(ToolNameEnum tool) {
        String mode = RequestContext.getAgentMode();
        if (AgentModeEnum.PLAN.getCode().equalsIgnoreCase(mode)) {
            throw new IllegalStateException(
                    tool.getToolName() + " is disabled in Plan mode — execution tools cannot run during planning. "
                            + "Include your SQL in the plan steps and call exitPlanMode to present the plan to the user.");
        }
    }
}
