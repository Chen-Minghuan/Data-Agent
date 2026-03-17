package edu.zsc.ai.agent.subagent;

/**
 * ThreadLocal context for the current SubAgent invocation.
 * Used to pass parentToolCallId from ChatStreamBridge (when emitting callingSubAgent)
 * to SubAgentStreamBridge (when emitting SubAgent's internal tool calls/results).
 */
public final class SubAgentContext {

    private static final ThreadLocal<String> PARENT_TOOL_CALL_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TASK_ID = new ThreadLocal<>();

    private SubAgentContext() {}

    public static void setParentToolCallId(String id) {
        if (id == null) {
            PARENT_TOOL_CALL_ID.remove();
            return;
        }
        PARENT_TOOL_CALL_ID.set(id);
    }

    public static String getParentToolCallId() {
        return PARENT_TOOL_CALL_ID.get();
    }

    public static void setTaskId(String id) {
        if (id == null) {
            TASK_ID.remove();
            return;
        }
        TASK_ID.set(id);
    }

    public static String getTaskId() {
        return TASK_ID.get();
    }

    public static void clear() {
        PARENT_TOOL_CALL_ID.remove();
        TASK_ID.remove();
    }
}
