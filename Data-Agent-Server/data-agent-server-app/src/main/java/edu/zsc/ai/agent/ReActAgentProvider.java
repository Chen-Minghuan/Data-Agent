package edu.zsc.ai.agent;

/**
 * Provides the appropriate ReActAgent for a given model name (e.g. qwen3-max, qwen3-max-thinking).
 */
public interface ReActAgentProvider {

    /**
     * Returns the ReActAgent for the given model name and prompt language.
     *
     * @param modelName model name (must be validated with ModelEnum beforehand)
     * @param language prompt language (e.g. en, zh). Unknown values should fallback to default.
     * @return the agent for that model
     */
    ReActAgent getAgent(String modelName, String language);
}
