package edu.zsc.ai.common.enums.ai;

import lombok.Getter;

import java.util.Locale;

/**
 * Prompt language configuration: request language code + system prompt resource path.
 */
@Getter
public enum PromptLanguageEnum {

    EN("en", "prompt/system_agent_en.xml"),
    ZH("zh", "prompt/system_agent_zh.xml");

    private final String code;
    private final String systemPromptResource;

    PromptLanguageEnum(String code, String systemPromptResource) {
        this.code = code;
        this.systemPromptResource = systemPromptResource;
    }

    /**
     * Get the system prompt resource path for the given agent mode.
     * AGENT mode uses the default resource; PLAN mode uses the plan-specific resource.
     */
    public String getSystemPromptResource(AgentModeEnum mode) {
        if (mode == AgentModeEnum.PLAN) {
            // prompt/system_agent_en.xml → prompt/system_plan_en.xml
            return systemPromptResource.replace("system_agent_", "system_plan_");
        }
        return systemPromptResource;
    }

    /**
     * Resolve request language to prompt language.
     * Unknown/blank values fallback to EN by design.
     */
    public static PromptLanguageEnum fromRequestLanguage(String language) {
        if (language == null || language.isBlank()) {
            return EN;
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith(ZH.code)) {
            return ZH;
        }
        if (normalized.startsWith(EN.code)) {
            return EN;
        }
        return EN;
    }
}
