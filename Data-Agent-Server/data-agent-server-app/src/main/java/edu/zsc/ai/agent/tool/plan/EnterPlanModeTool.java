package edu.zsc.ai.agent.tool.plan;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import lombok.extern.slf4j.Slf4j;

/**
 * Agent-mode escalation tool: switches from Agent mode to Plan mode for complex tasks.
 * Uses IMMEDIATE return behavior to stop the agent stream so the backend can
 * seamlessly chain a Plan-mode agent on the same SSE connection.
 * <p>
 * The tool result stored in memory is kept minimal to save tokens on subsequent turns.
 */
@AgentTool
@Slf4j
public class EnterPlanModeTool {

    @Tool(
            value = {
                    "Switches to Plan mode — dramatically reduces risk of errors on complex tasks. ",
                    "In Plan mode you analyze thoroughly, explore all relevant schemas, and produce ",
                    "a structured execution plan before any SQL runs. This prevents costly mistakes ",
                    "on multi-step operations, irreversible changes, and cross-database work.",
                    "",
                    "Planning is cheap, recovering from a bad execution is expensive. ",
                    "Proactively enter Plan mode whenever the task is not a simple one-shot query. ",
                    "Typical signals (not exhaustive): ",
                    "- Write operations (DML/DDL), especially multi-table or with cascade effects; ",
                    "- User asks multiple questions or a multi-step task in one message; ",
                    "- Queries requiring multi-table JOINs or cross-database operations; ",
                    "- Data migrations, bulk updates, schema changes — any high-impact operation; ",
                    "- Vague or open-ended goals that need investigation before action; ",
                    "- The thinking tool's suggestPlanMode flag is true. ",
                    "If any signal applies, or you judge the task has non-trivial complexity, call this tool."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String enterPlanMode(
            @P("Brief reason for planning") String reason,
            @P("Trigger: CHECKLIST_RECOMMENDATION | MULTI_STEP_DISCOVERED | " +
                    "UNEXPECTED_COMPLEXITY | IRREVERSIBLE_OPERATION | MULTI_TABLE_WRITE")
            String triggerSignal) {
        log.info("[Tool] enterPlanMode, reason='{}', trigger='{}'", reason, triggerSignal);
        return "Entering Plan mode [" + triggerSignal + "]: " + reason;
    }
}
