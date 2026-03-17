package edu.zsc.ai.config.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SubAgentProperties — Token 预算配置")
class SubAgentPropertiesTokenBudgetTest {

    @Test
    @DisplayName("Explorer 默认 maxTokens")
    void explorerDefaultMaxTokens() {
        SubAgentProperties props = new SubAgentProperties();
        assertEquals(8000, props.getExplorer().getMaxTokens());
    }

    @Test
    @DisplayName("Planner 默认 maxTokens")
    void sqlPlannerDefaultMaxTokens() {
        SubAgentProperties props = new SubAgentProperties();
        assertEquals(16000, props.getPlanner().getMaxTokens());
    }

    @Test
    @DisplayName("maxTokens 可配置覆盖")
    void maxTokensCanBeOverridden() {
        SubAgentProperties props = new SubAgentProperties();
        props.getExplorer().setMaxTokens(4000);
        props.getPlanner().setMaxTokens(32000);

        assertEquals(4000, props.getExplorer().getMaxTokens());
        assertEquals(32000, props.getPlanner().getMaxTokens());
    }

    @Test
    @DisplayName("totalMaxTokens 默认值")
    void totalMaxTokensDefault() {
        SubAgentProperties props = new SubAgentProperties();
        assertEquals(50000, props.getTotalMaxTokens());
    }

    @Test
    @DisplayName("totalMaxTokens 可配置覆盖")
    void totalMaxTokensCanBeOverridden() {
        SubAgentProperties props = new SubAgentProperties();
        props.setTotalMaxTokens(100000);
        assertEquals(100000, props.getTotalMaxTokens());
    }
}
