package edu.zsc.ai.agent.tool.orchestrator;

import edu.zsc.ai.context.RequestContext;

/**
 * Shared utilities for SubAgent orchestrator tools.
 * Trace/span management has been moved to SubAgentObservabilityListener (AgentListener).
 */
abstract class SubAgentToolSupport {

    protected Long parseConversationId() {
        try {
            Long id = RequestContext.getConversationId();
            return id != null ? id : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }
}
