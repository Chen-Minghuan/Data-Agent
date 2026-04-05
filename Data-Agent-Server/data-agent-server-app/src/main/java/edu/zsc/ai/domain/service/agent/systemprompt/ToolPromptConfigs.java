package edu.zsc.ai.domain.service.agent.systemprompt;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import edu.zsc.ai.common.enums.ai.ToolNameEnum;

/**
 * Registry for prompt-time tool guidance.
 *
 * <p>Authoring rule:
 * tool descriptions explain capability contracts, while tool prompts explain workflow choices.
 * If a sentence would still be true without any surrounding task context, it probably belongs in
 * the tool description instead of this registry.
 *
 * <p>Precedence rule:
 * if a tool prompt ever conflicts with a tool's annotation-level contract, the annotation wins.
 */
public final class ToolPromptConfigs {

    private static final List<ToolPromptConfig> DEFAULTS = List.of(
            new ToolPromptConfig(ToolNameEnum.GET_DATABASES, "agent.tool_prompt.get_databases",
                    Set.of(ToolPromptTarget.MAIN_AGENT, ToolPromptTarget.MAIN_PLAN), 10),
            new ToolPromptConfig(ToolNameEnum.GET_SCHEMAS, "agent.tool_prompt.get_schemas",
                    Set.of(ToolPromptTarget.MAIN_AGENT, ToolPromptTarget.MAIN_PLAN), 20),
            new ToolPromptConfig(ToolNameEnum.SEARCH_OBJECTS, "agent.tool_prompt.search_objects",
                    Set.of(ToolPromptTarget.MAIN_AGENT, ToolPromptTarget.EXPLORER), 30),
            new ToolPromptConfig(ToolNameEnum.GET_OBJECT_DETAIL, "agent.tool_prompt.get_object_detail",
                    Set.of(ToolPromptTarget.MAIN_AGENT, ToolPromptTarget.EXPLORER, ToolPromptTarget.PLANNER), 40),
            new ToolPromptConfig(ToolNameEnum.CALLING_EXPLORER_SUB_AGENT, "agent.tool_prompt.calling_explorer_sub_agent",
                    Set.of(ToolPromptTarget.MAIN_AGENT, ToolPromptTarget.MAIN_PLAN), 50),
            new ToolPromptConfig(ToolNameEnum.CALLING_PLANNER_SUB_AGENT, "agent.tool_prompt.calling_planner_sub_agent",
                    Set.of(ToolPromptTarget.MAIN_AGENT, ToolPromptTarget.MAIN_PLAN), 60),
            new ToolPromptConfig(ToolNameEnum.EXECUTE_SELECT_SQL, "agent.tool_prompt.execute_select_sql",
                    Set.of(ToolPromptTarget.MAIN_AGENT, ToolPromptTarget.EXPLORER, ToolPromptTarget.PLANNER), 70),
            new ToolPromptConfig(ToolNameEnum.EXECUTE_NON_SELECT_SQL, "agent.tool_prompt.execute_non_select_sql",
                    Set.of(ToolPromptTarget.MAIN_AGENT, ToolPromptTarget.EXPLORER, ToolPromptTarget.PLANNER), 80),
            new ToolPromptConfig(ToolNameEnum.ASK_USER_QUESTION, "agent.tool_prompt.ask_user_question",
                    Set.of(ToolPromptTarget.MAIN_AGENT, ToolPromptTarget.MAIN_PLAN), 90),
            new ToolPromptConfig(ToolNameEnum.TODO_WRITE, "agent.tool_prompt.todo_write",
                    Set.of(ToolPromptTarget.MAIN_AGENT, ToolPromptTarget.MAIN_PLAN, ToolPromptTarget.EXPLORER, ToolPromptTarget.PLANNER), 100),
            new ToolPromptConfig(ToolNameEnum.ACTIVATE_SKILL, "agent.tool_prompt.activate_skill",
                    Set.of(ToolPromptTarget.MAIN_AGENT), 110),
            new ToolPromptConfig(ToolNameEnum.RENDER_CHART, "agent.tool_prompt.render_chart",
                    Set.of(ToolPromptTarget.MAIN_AGENT), 120),
            new ToolPromptConfig(ToolNameEnum.EXPORT_FILE, "agent.tool_prompt.export_file",
                    Set.of(ToolPromptTarget.MAIN_AGENT), 130),
            new ToolPromptConfig(ToolNameEnum.EXIT_PLAN_MODE, "agent.tool_prompt.exit_plan_mode",
                    Set.of(ToolPromptTarget.MAIN_PLAN), 140)
    );

    private ToolPromptConfigs() {
    }

    public static List<ToolPromptConfig> resolveFor(ToolPromptTarget target, boolean skillsAvailable) {
        return DEFAULTS.stream()
                .filter(config -> config.appliesTo(target))
                .filter(config -> skillsAvailable || config.toolName() != ToolNameEnum.ACTIVATE_SKILL)
                .sorted(Comparator.comparingInt(ToolPromptConfig::order))
                .toList();
    }
}
