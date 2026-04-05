package edu.zsc.ai.domain.service.agent.systemprompt;

import java.util.Set;

import edu.zsc.ai.common.enums.ai.ToolNameEnum;

/**
 * Prompt-time workflow guidance layered on top of a tool's stable description.
 *
 * <p>The tool description lives with the tool annotation and owns the capability contract:
 * what the tool does, what it needs, and how its outputs should be interpreted.
 *
 * <p>This config is for contextual prompt guidance only: when to choose the tool,
 * how to react to its results, and what next step it should unlock for a specific agent target.
 * It must not restate or weaken the underlying tool contract.
 */
public record ToolPromptConfig(
        ToolNameEnum toolName,
        String promptKey,
        Set<ToolPromptTarget> targets,
        int order
) {

    public boolean appliesTo(ToolPromptTarget target) {
        return target != null && targets.contains(target);
    }
}
