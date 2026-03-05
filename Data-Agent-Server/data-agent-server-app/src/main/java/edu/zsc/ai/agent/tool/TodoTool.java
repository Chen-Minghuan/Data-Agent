package edu.zsc.ai.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.tool.model.Todo;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AgentTool
@Slf4j
public class TodoTool {

    @Tool({
        "[GOAL] Expose execution plan to user for multi-step tasks (clarification → exploration → SQL → response).",
        "[WHEN] Call at the start of complex workflows (>=3 steps) before heavy tool execution.",
        "[INPUT] Provide unique todoId and complete step list (first step IN_PROGRESS, others NOT_STARTED).",
        "[AFTER] Keep this plan synchronized with real progress via todo_update."
    })
    public AgentToolResult todo_create(
            @P("Unique id for this todo list (e.g. 'task-1').")
            String todoId,
            @P("The complete list of planned tasks; first step IN_PROGRESS, others NOT_STARTED.")
            List<Todo> items) {
        log.info("[Tool] todo_create, todoId={}, itemsSize={}", todoId, items != null ? items.size() : 0);
        List<Todo> list = items != null && !items.isEmpty() ? items : List.of();
        log.info("[Tool done] todo_create, todoId={}, count={}", todoId, list.size());
        return AgentToolResult.success(buildTodoPayload(todoId, list));
    }

    @Tool({
        "[GOAL] Keep user-visible plan aligned with actual execution milestones.",
        "[WHEN] Call after each significant step (clarification answered, source resolved, SQL executed, chart rendered).",
        "[INPUT] Use same todoId as todo_create and send the full updated list.",
        "[AFTER] Ensure statuses are current before user-facing summaries."
    })
    public AgentToolResult todo_update(
            @P("Same todoId used in todo_create.")
            String todoId,
            @P("The complete updated list with revised statuses.")
            List<Todo> items) {
        log.info("[Tool] todo_update, todoId={}, itemsSize={}", todoId, items != null ? items.size() : 0);
        List<Todo> list = items != null && !items.isEmpty() ? items : List.of();
        log.info("[Tool done] todo_update, todoId={}, count={}", todoId, list.size());
        return AgentToolResult.success(buildTodoPayload(todoId, list));
    }

    @Tool({
        "[GOAL] Close progress context when workflow is fully completed.",
        "[WHEN] Call only after all tasks are COMPLETED and final response is ready.",
        "[INPUT] Pass todoId of the active plan; returns empty task payload.",
        "[AFTER] Do not keep stale task panels after completion."
    })
    public AgentToolResult todo_delete(
            @P("The todoId of the list to clear.")
            String todoId) {
        log.info("[Tool] todo_delete, todoId={}", todoId);
        log.info("[Tool done] todo_delete, todoId={}", todoId);
        return AgentToolResult.success(buildTodoPayload(todoId, List.of()));
    }

    /** Response format: { "todoId": string, "items": Todo[] }. Frontend uses todoId for single-box logic. */
    private static String buildTodoPayload(String todoId, List<Todo> items) {
        Map<String, Object> out = new HashMap<>();
        out.put("todoId", todoId != null ? todoId : "");
        out.put("items", items != null ? items : List.of());
        return JsonUtil.object2json(out);
    }
}
