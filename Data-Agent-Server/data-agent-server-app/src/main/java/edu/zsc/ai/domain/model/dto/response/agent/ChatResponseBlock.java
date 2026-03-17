package edu.zsc.ai.domain.model.dto.response.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.zsc.ai.common.enums.ai.MessageBlockEnum;
import edu.zsc.ai.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseBlock {

    /** JSON keys in tool call/result block data (must match frontend ToolCallData / ToolResultData). */
    public static final String DATA_KEY_ID = "id";
    public static final String DATA_KEY_TOOL_NAME = "toolName";
    public static final String DATA_KEY_ARGUMENTS = "arguments";
    public static final String DATA_KEY_RESULT = "result";
    /** True when tool execution failed (ToolExecutionResult.isError). */
    public static final String DATA_KEY_ERROR = "error";
    /** True when tool arguments are still streaming (partial), false when complete */
    public static final String DATA_KEY_STREAMING = "streaming";

    private static final String EMPTY = "";

    private String type;
    private String data;
    private Long conversationId;
    private boolean done;

    /** Parent tool call id when this block is from a SubAgent (nested under callingSubAgent). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String parentToolCallId;

    /** SubAgent task id for grouping concurrent Explorer task events. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String subAgentTaskId;

    public static ChatResponseBlock text(String data) {
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.TEXT.name())
                .data(data)
                .done(false)
                .build();
    }

    public static ChatResponseBlock thought(String data) {
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.THOUGHT.name())
                .data(data)
                .done(false)
                .build();
    }

    /**
     * End-of-stream block: done=true. conversationId can be injected by stream mapping.
     */
    public static ChatResponseBlock doneBlock() {
        return ChatResponseBlock.builder()
                .done(true)
                .build();
    }

    /**
     * End-of-stream block with metadata (e.g. tool usage stats).
     */
    public static ChatResponseBlock doneBlock(Map<String, Object> metadata) {
        return ChatResponseBlock.builder()
                .done(true)
                .data(JsonUtil.object2json(metadata))
                .build();
    }

    /**
     * Tool call block: data is JSON {"id":"...", "toolName":"...", "arguments":"..."}.
     * id is optional (from LangChain4j ToolExecutionRequest / PartialToolCall); used to merge streaming chunks and pair with TOOL_RESULT.
     */
    public static ChatResponseBlock toolCall(String id, String toolName, String arguments) {
        return toolCall(id, toolName, arguments, null);
    }

    /**
     * Tool call block with optional streaming indicator:
     * data is JSON {"id":"...", "toolName":"...", "arguments":"...", "streaming": true|false}.
     */
    public static ChatResponseBlock toolCall(String id, String toolName, String arguments, Boolean streaming) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        if (id != null && !id.isEmpty()) {
            map.put(DATA_KEY_ID, id);
        }
        map.put(DATA_KEY_TOOL_NAME, toolName != null ? toolName : EMPTY);
        map.put(DATA_KEY_ARGUMENTS, arguments != null ? arguments : EMPTY);
        if (streaming != null) {
            map.put(DATA_KEY_STREAMING, streaming);
        }
        String data = JsonUtil.object2json(map);
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.TOOL_CALL.name())
                .data(data)
                .done(false)
                .build();
    }

    /**
     * Status block: lightweight notification to the frontend (e.g. "compressing").
     * data is a status key that the frontend maps to a user-visible message.
     */
    public static ChatResponseBlock status(String statusKey) {
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.STATUS.name())
                .data(statusKey)
                .done(false)
                .build();
    }

    /**
     * Tool result block: data is JSON {"id":"...", "toolName":"...", "result":"...", "error": true|false}.
     * id matches the tool call id for pairing. error is true when tool execution failed (ToolExecution.hasFailed()).
     */
    public static ChatResponseBlock toolResult(String id, String toolName, String result, boolean isError) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (id != null && !id.isEmpty()) {
            map.put(DATA_KEY_ID, id);
        }
        map.put(DATA_KEY_TOOL_NAME, toolName != null ? toolName : EMPTY);
        map.put(DATA_KEY_RESULT, result != null ? result : EMPTY);
        map.put(DATA_KEY_ERROR, isError);
        String data = JsonUtil.object2json(map);
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.TOOL_RESULT.name())
                .data(data)
                .done(false)
                .build();
    }

    // ─── SubAgent lifecycle factory methods ──────────────────────────────

    public static ChatResponseBlock subAgentStart(String agentType, String parentToolCallId, String taskId) {
        return subAgentStart(agentType, parentToolCallId, taskId, null);
    }

    public static ChatResponseBlock subAgentStart(String agentType, String parentToolCallId, String taskId, Long connectionId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("agentType", agentType);
        if (connectionId != null) map.put("connectionId", connectionId);
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.SUB_AGENT_START.name())
                .data(JsonUtil.object2json(map))
                .parentToolCallId(parentToolCallId)
                .subAgentTaskId(taskId)
                .done(false).build();
    }

    public static ChatResponseBlock subAgentProgress(String agentType, String message, String parentToolCallId, String taskId) {
        return subAgentProgress(agentType, message, parentToolCallId, taskId, null);
    }

    public static ChatResponseBlock subAgentProgress(String agentType, String message, String parentToolCallId, String taskId, Long connectionId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("agentType", agentType);
        map.put("message", message);
        if (connectionId != null) map.put("connectionId", connectionId);
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.SUB_AGENT_PROGRESS.name())
                .data(JsonUtil.object2json(map))
                .parentToolCallId(parentToolCallId)
                .subAgentTaskId(taskId)
                .done(false).build();
    }

    public static ChatResponseBlock subAgentComplete(String agentType, String parentToolCallId, String taskId, int toolCount, Map<String, Integer> toolCounts) {
        return subAgentComplete(agentType, parentToolCallId, taskId, toolCount, toolCounts, null, null, null);
    }

    public static ChatResponseBlock subAgentComplete(String agentType, String parentToolCallId, String taskId, int toolCount,
                                                     Map<String, Integer> toolCounts, String summaryText, String resultJson) {
        return subAgentComplete(agentType, parentToolCallId, taskId, toolCount, toolCounts, summaryText, resultJson, null);
    }

    public static ChatResponseBlock subAgentComplete(String agentType, String parentToolCallId, String taskId, int toolCount,
                                                     Map<String, Integer> toolCounts, String summaryText, String resultJson, Long connectionId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("agentType", agentType);
        map.put("toolCount", toolCount);
        if (toolCounts != null) map.put("toolCounts", toolCounts);
        if (summaryText != null && !summaryText.isBlank()) map.put("summaryText", summaryText);
        if (resultJson != null && !resultJson.isBlank()) map.put("resultJson", resultJson);
        if (connectionId != null) map.put("connectionId", connectionId);
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.SUB_AGENT_COMPLETE.name())
                .data(JsonUtil.object2json(map))
                .parentToolCallId(parentToolCallId)
                .subAgentTaskId(taskId)
                .done(false).build();
    }

    public static ChatResponseBlock subAgentError(String agentType, String message, String parentToolCallId, String taskId) {
        return subAgentError(agentType, message, parentToolCallId, taskId, null);
    }

    public static ChatResponseBlock subAgentError(String agentType, String message, String parentToolCallId, String taskId, Long connectionId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("agentType", agentType);
        map.put("message", message);
        if (connectionId != null) map.put("connectionId", connectionId);
        return ChatResponseBlock.builder()
                .type(MessageBlockEnum.SUB_AGENT_ERROR.name())
                .data(JsonUtil.object2json(map))
                .parentToolCallId(parentToolCallId)
                .subAgentTaskId(taskId)
                .done(false).build();
    }

}
