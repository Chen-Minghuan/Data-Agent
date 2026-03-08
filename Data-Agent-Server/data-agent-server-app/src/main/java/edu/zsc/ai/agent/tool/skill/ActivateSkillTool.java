package edu.zsc.ai.agent.tool.skill;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.common.enums.ai.SkillEnum;
import edu.zsc.ai.config.ai.PromptConfig;
import lombok.extern.slf4j.Slf4j;

@AgentTool
@Slf4j
public class ActivateSkillTool {

    @Tool({
            "Load expert operational rules for a specific task type. Returns battle-tested ",
            "templates, patterns, and common pitfalls. Call BEFORE the first use of the ",
            "related tool in this conversation — skip if already loaded in this session.",
            "",
            "Valid skillName values: chart"
    })
    public String activateSkill(
            @P("Skill to load. MUST be one of: chart") String skillName) {
        SkillEnum skill = SkillEnum.fromName(skillName);
        if (skill == null) {
            return "Unknown skill: " + skillName
                    + ". Valid values: " + SkillEnum.validNames();
        }
        log.info("Skill activated: {}", skill.getSkillName());
        return PromptConfig.loadClassPathResource(skill.getResourcePath());
    }
}
