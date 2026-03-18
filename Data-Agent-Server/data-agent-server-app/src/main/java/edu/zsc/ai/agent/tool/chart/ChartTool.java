package edu.zsc.ai.agent.tool.chart;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.annotation.DisallowInPlanMode;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.message.ToolMessageSupport;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.enums.ai.ChartTypeEnum;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@AgentTool
@Slf4j
@RequiredArgsConstructor
public class ChartTool {

    @Tool({
            "Calling this tool greatly improves how users understand data — one clear chart is far more ",
            "effective than raw tables for many questions. ",
            "Renders one chart; put key insight in description — the chart IS the final answer; do not add text afterward.",
            "",
            "When to Use: when you have query results and the user wants a visual; after executeSelectSql (or equivalent) with data ready.",
            "Relation: call activateSkill('chart') before first use in the session; if dimension unspecified, askUserQuestion then render one targeted chart."
    })
    @DisallowInPlanMode(ToolNameEnum.RENDER_CHART)
    public AgentToolResult renderChart(
            @P("Chart type: LINE/BAR/PIE/SCATTER/AREA") String chartType,
            @P("ECharts option JSON string. Must be a valid JSON object.") String optionJson,
            @P(value = "Optional explanation for users: chart meaning, key insight(s), and reading guide", required = false)
            String description) {
        log.info("[Tool] renderChart, chartType={}", chartType);

        ChartTypeEnum normalizedType;
        JsonNode optionNode;
        try {
            normalizedType = ChartTypeEnum.fromValue(chartType);
            optionNode = JsonUtil.readObjectNode(optionJson, "optionJson");
        } catch (IllegalArgumentException e) {
            throw AgentToolExecuteException.invalidInput(
                    ToolNameEnum.RENDER_CHART,
                    "Invalid renderChart input: " + e.getMessage()
                            + ". chartType must be one of LINE/BAR/PIE/SCATTER/AREA, and optionJson must be valid ECharts JSON. "
                            + "Fix the chart input and retry; do not invent chart conclusions without a rendered chart."
            );
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("chartType", normalizedType.name());
        result.put("option", optionJson);
        if (StringUtils.isNotBlank(description)) {
            result.put("description", description.trim());
        }

        log.info("[Tool done] renderChart, chartType={}, optionKeys={}",
                normalizedType, optionNode.size());
        return AgentToolResult.success(result, ToolMessageSupport.sentence(
                "Chart rendering payload is ready.",
                "Use this chart as the final visual answer and keep any additional narrative consistent with the rendered chart."
        ));
    }
}
