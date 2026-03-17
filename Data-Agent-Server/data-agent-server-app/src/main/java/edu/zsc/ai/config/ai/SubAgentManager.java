package edu.zsc.ai.config.ai;

import edu.zsc.ai.agent.subagent.explorer.ExplorerSubAgent;
import edu.zsc.ai.agent.subagent.planner.PlannerSubAgent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Aggregates SubAgent-related dependencies for orchestrator tools.
 * Reduces constructor parameter count in orchestrator tools.
 */
@Component
@Getter
@RequiredArgsConstructor
public class SubAgentManager {

    @Lazy
    private final ExplorerSubAgent explorerSubAgent;

    @Lazy
    private final PlannerSubAgent plannerSubAgent;

    private final SubAgentProperties properties;
}
