package edu.zsc.ai.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalized configuration for SubAgents.
 * Each SubAgent (Explorer, Planner) can have its own model, timeout, and loop limits.
 *
 * Example application.yml:
 * <pre>
 * agent:
 *   sub-agent:
 *     explorer:
 *       timeout-seconds: 120
 *     planner:
 *       timeout-seconds: 180
 *     max-explorer-loop: 3
 *     concurrent-timeout-seconds: 300
 * </pre>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "agent.sub-agent")
public class SubAgentProperties {

    private AgentConfig explorer = new AgentConfig(120, 8000);
    private AgentConfig planner = new AgentConfig(180, 16000);
    private int maxExplorerLoop = 3;
    /** Total token budget across all SubAgent calls within a single request */
    private int totalMaxTokens = 50000;

    @Data
    public static class AgentConfig {
        private long timeoutSeconds;
        /** Per-SubAgent token budget (estimated tokens); 0 = unlimited */
        private int maxTokens;

        public AgentConfig() {
            this.timeoutSeconds = 120;
            this.maxTokens = 8000;
        }

        public AgentConfig(long timeoutSeconds) {
            this(timeoutSeconds, 8000);
        }

        public AgentConfig(long timeoutSeconds, int maxTokens) {
            this.timeoutSeconds = timeoutSeconds;
            this.maxTokens = maxTokens;
        }
    }
}
