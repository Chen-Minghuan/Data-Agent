package edu.zsc.ai.agent.tool.plan;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
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
                    "Internal mode-switch tool for handing a complex request from execution flow into planning flow.",
                    "Use only when the runtime explicitly exposes this tool for a plan handoff.",
                    "",
                    "When to Use: write operations (DML/DDL), multi-step or multi-table tasks, vague goals, or when analysis suggests a dedicated planning pass.",
                    "Trigger signal: CHECKLIST_RECOMMENDATION|MULTI_STEP_DISCOVERED|UNEXPECTED_COMPLEXITY|IRREVERSIBLE_OPERATION|MULTI_TABLE_WRITE."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String enterPlanMode(
            @P("Brief reason for planning") String reason,
            @P("Trigger: CHECKLIST_RECOMMENDATION | MULTI_STEP_DISCOVERED | " +
                    "UNEXPECTED_COMPLEXITY | IRREVERSIBLE_OPERATION | MULTI_TABLE_WRITE")
            String triggerSignal) {
        log.info("[Tool] enterPlanMode, reason='{}', trigger='{}'", reason, triggerSignal);
        return ToolMessageSupport.sentence(
                "Plan mode was activated for trigger " + triggerSignal + ".",
                "Reason: " + reason + ".",
                "Stop direct execution and continue by producing a decision-ready plan for the user."
        );
    }
}
