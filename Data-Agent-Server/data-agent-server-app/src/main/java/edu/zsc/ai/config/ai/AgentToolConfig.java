package edu.zsc.ai.config.ai;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ask.AskUserConfirmTool;
import edu.zsc.ai.agent.tool.chart.ChartTool;
import edu.zsc.ai.agent.tool.plan.ExitPlanModeTool;
import edu.zsc.ai.agent.tool.sql.GetObjectDetailTool;
import edu.zsc.ai.agent.tool.sql.SearchObjectsTool;
import edu.zsc.ai.agent.tool.sql.ExecuteSqlTool;
import edu.zsc.ai.agent.tool.todo.TodoTool;
import edu.zsc.ai.agent.tool.skill.ActivateSkillTool;
import edu.zsc.ai.common.enums.ai.AgentModeEnum;
import edu.zsc.ai.common.enums.ai.AgentTypeEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    /** Explorer gets only scoped discovery tools for schema exploration. */
    private static final Set<Class<?>> EXPLORER_ALLOWED = Set.of(
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

    public Map<ToolSpecification, ToolExecutor> buildToolExecutors(List<Object> agentTools) {
        if (CollectionUtils.isEmpty(agentTools)) {
            return Map.of();
        }

        List<ToolRegistration> registrations = agentTools.stream()
                .flatMap(tool -> resolveToolRegistrations(tool).stream())
                .toList();

        ToolSpecifications.validateSpecifications(registrations.stream()
                .map(ToolRegistration::specification)
                .collect(Collectors.toList()));

        Map<ToolSpecification, ToolExecutor> executors = new LinkedHashMap<>();
        for (ToolRegistration registration : registrations) {
            executors.put(
                    registration.specification(),
                    new DefaultToolExecutor(
                            registration.toolBean(),
                            registration.originalMethod(),
                            registration.invocableMethod()
                    )
            );
        }
        return executors;
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
     *   <li>EXPLORER — SearchObjectsTool, GetObjectDetailTool</li>
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

    private List<ToolRegistration> resolveToolRegistrations(Object toolBean) {
        Class<?> targetClass = AopUtils.getTargetClass(toolBean);
        if (targetClass == null) {
            return List.of();
        }

        List<ToolRegistration> registrations = new ArrayList<>();
        for (Method method : targetClass.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Tool.class)) {
                continue;
            }
            Method invocableMethod = AopUtils.selectInvocableMethod(method, toolBean.getClass());
            registrations.add(new ToolRegistration(
                    toolBean,
                    ToolSpecifications.toolSpecificationFrom(method),
                    method,
                    invocableMethod
            ));
        }
        return registrations;
    }

    private record ToolRegistration(
            Object toolBean,
            ToolSpecification specification,
            Method originalMethod,
            Method invocableMethod
    ) {
    }
}
