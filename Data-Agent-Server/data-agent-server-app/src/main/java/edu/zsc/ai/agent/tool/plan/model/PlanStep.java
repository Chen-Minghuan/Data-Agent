package edu.zsc.ai.agent.tool.plan.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single step in an execution plan.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanStep {

    @Description("Step order number (1-based)")
    private int order;

    @Description("What this step does")
    private String description;

    @Description("The SQL statement to execute in this step")
    private String sql;

    @Description("The table or object name involved")
    private String objectName;
}
