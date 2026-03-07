package edu.zsc.ai.agent.tool.plan;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.agent.tool.plan.model.PlanStep;
import lombok.extern.slf4j.Slf4j;

/**
 * Plan-mode exit tool: presents the structured execution plan to the user.
 * Uses IMMEDIATE return behavior to pause the agent stream and wait for user decision.
 * <p>
 * The full plan data is conveyed to the frontend via TOOL_CALL arguments (streamed via SSE).
 * The tool result stored in memory is kept minimal to save tokens on subsequent turns.
 */
@AgentTool
@Slf4j
public class ExitPlanModeTool {

    @Tool(
            value = {
                    "[GOAL] Present the execution plan to the user and offer exit options.",
                    "[WHEN] Call when your plan is complete and ready for user review.",
                    "[WHEN_NOT] Do not call before plan is finalized. Do not call in Agent mode.",
                    "[INPUT] Structured plan with steps and SQL statements."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public String exitPlanMode(
            @P("Plan title / summary") String title,
            @P("List of planned steps, each with order, description, SQL, and objectName") List<PlanStep> steps) {

        int stepCount = steps != null ? steps.size() : 0;
        log.info("[Tool] exitPlanMode, title='{}', steps={}", title, stepCount);

        // Minimal result for memory — full plan data is in the tool call arguments
        return "Plan presented to user: " + title + " (" + stepCount + " steps).";
    }
}
