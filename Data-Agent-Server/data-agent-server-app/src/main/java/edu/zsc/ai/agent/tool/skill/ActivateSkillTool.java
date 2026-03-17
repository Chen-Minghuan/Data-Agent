package edu.zsc.ai.agent.tool.skill;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.common.enums.ai.SkillEnum;
import edu.zsc.ai.config.ai.PromptConfig;
import lombok.extern.slf4j.Slf4j;

@AgentTool
@Slf4j
public class ActivateSkillTool {

    @Tool({
            "Loads expert rules and templates for a capability, greatly improving output quality. ",
            "Skip if already loaded this session.",
            "",
            "When to Use:",
            "  - 'chart': before first renderChart call, loads ECharts rules and templates.",
            "  - 'sql-optimization': when optimizing complex SQL (3+ table JOIN, subqueries, or user requests optimization), ",
            "    loads index analysis, execution plan, and rewrite strategies.",
            "skillName must be one of: chart, sql-optimization."
    })
    public String activateSkill(
            @P("Skill to load. MUST be one of: chart, sql-optimization") String skillName) {
        SkillEnum skill = SkillEnum.fromName(skillName);
        if (skill == null) {
            return "Unknown skill: " + skillName
                    + ". Valid values: " + SkillEnum.validNames();
        }
        log.info("Skill activated: {}", skill.getSkillName());
        return PromptConfig.loadClassPathResource(skill.getResourcePath());
    }
}
