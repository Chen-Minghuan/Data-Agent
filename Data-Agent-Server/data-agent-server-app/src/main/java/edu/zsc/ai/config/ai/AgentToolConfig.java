package edu.zsc.ai.config.ai;

import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ask.AskUserConfirmTool;
import edu.zsc.ai.agent.tool.chart.ChartTool;
import edu.zsc.ai.agent.tool.plan.ExitPlanModeTool;
import edu.zsc.ai.agent.tool.sql.GetEnvironmentOverviewTool;
import edu.zsc.ai.agent.tool.sql.GetObjectDetailTool;
import edu.zsc.ai.agent.tool.sql.SearchObjectsTool;
import edu.zsc.ai.agent.tool.sql.ExecuteSqlTool;
import edu.zsc.ai.agent.tool.todo.TodoTool;
import edu.zsc.ai.agent.tool.skill.ActivateSkillTool;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class AgentToolConfig {

    // ── Mode-based filtering (existing) ──

    private static final Set<Class<?>> PLAN_MODE_DISABLED = Set.of(
            ExecuteSqlTool.class,
            ChartTool.class,
            AskUserConfirmTool.class
    );

    private static final Set<Class<?>> AGENT_MODE_DISABLED = Set.of(
            ExitPlanModeTool.class
    );

    // ── AgentType-based filtering (multi-agent architecture) ──

    /**
     * Tools EXCLUDED for each agent type.
     * MAIN: excludes SearchObjectsTool, GetObjectDetailTool — schema exploration via callingExplorerSubAgent.
     *       Keeps GetEnvironmentOverviewTool for connection list.
     * EXPLORER: uses allowlist (see EXPLORER_ALLOWED).
     * PLANNER: uses allowlist (see PLANNER_ALLOWED).
     */
    private static final Set<Class<?>> MAIN_EXCLUDED = Set.of(
            SearchObjectsTool.class,
            GetObjectDetailTool.class
    );

    /** Explorer gets discovery tools for schema exploration. */
    private static final Set<Class<?>> EXPLORER_ALLOWED = Set.of(
            GetEnvironmentOverviewTool.class,
            SearchObjectsTool.class,
            GetObjectDetailTool.class
    );

    /** Planner only gets TodoTool + ActivateSkillTool. */
    private static final Set<Class<?>> PLANNER_ALLOWED = Set.of(
            TodoTool.class,
            ActivateSkillTool.class
    );

    private static final Map<AgentTypeEnum, Set<Class<?>>> SUB_AGENT_ALLOWLISTS = Map.of(
            AgentTypeEnum.EXPLORER, EXPLORER_ALLOWED,
            AgentTypeEnum.PLANNER, PLANNER_ALLOWED
    );

    @Bean
    public List<Object> agentTools(ApplicationContext context) {
        return new ArrayList<>(context.getBeansWithAnnotation(AgentTool.class).values());
    }

    /**
     * Filter tools by AgentMode (AGENT vs PLAN). Existing behavior, unchanged.
     */
    public List<Object> filterTools(List<Object> agentTools, AgentModeEnum mode) {
        Set<Class<?>> disabled = (mode == AgentModeEnum.PLAN)
                ? PLAN_MODE_DISABLED
                : AGENT_MODE_DISABLED;
        return agentTools.stream()
                .filter(tool -> !matchesAny(tool, disabled))
                .toList();
    }

    /**
     * Filter tools by AgentType (MAIN / EXPLORER / PLANNER).
     * <ul>
     *   <li>MAIN — all except SearchObjectsTool, GetObjectDetailTool (has GetEnvironmentOverviewTool)</li>
     *   <li>EXPLORER — GetEnvironmentOverviewTool, SearchObjectsTool, GetObjectDetailTool</li>
     *   <li>PLANNER — only TodoTool + ActivateSkillTool</li>
     * </ul>
     */
    public List<Object> filterToolsByAgentType(List<Object> agentTools, AgentTypeEnum agentType) {
        if (agentType == AgentTypeEnum.MAIN) {
            return agentTools.stream()
                    .filter(tool -> !matchesAny(tool, MAIN_EXCLUDED))
                    .toList();
        }

        Set<Class<?>> allowlist = SUB_AGENT_ALLOWLISTS.get(agentType);
        if (allowlist == null) {
            throw new IllegalArgumentException("Unknown agent type: " + agentType);
        }
        return agentTools.stream()
                .filter(tool -> matchesAny(tool, allowlist))
                .toList();
    }

    /**
     * Check if tool is an instance of any class in the set.
     * Uses instanceof semantics so it works with subclasses and proxies.
     */
    private static boolean matchesAny(Object tool, Set<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            if (clazz.isInstance(tool)) {
                return true;
            }
        }
        return false;
    }
}
