package edu.zsc.ai.agent.tool.think;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.agent.tool.think.model.input.ThinkingRequest;
import edu.zsc.ai.agent.tool.think.model.output.ThinkingOutput;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AgentTool
@Slf4j
public class SequentialThinkingTool {

    private static final Pattern RISK_PATTERN = Pattern.compile(
            "(?i)(risk|danger|caution|warning|careful|unsafe|sensitive|pii|large table|no index|missing|ambiguous|conflict)",
            Pattern.CASE_INSENSITIVE);

    @Tool({
            "[GOAL] Structure your reasoning before acting. Helps prevent hallucination and missed risks.",
            "[WHEN] Call at the start of each new request, or when encountering ambiguity/write operations.",
            "[WHEN_NOT] Do not call for follow-up questions in an already-analyzed conversation. Do not call multiple times in a row without acting between calls.",
            "[INPUT] goal=what you want to achieve; analysis=your reasoning about state, gaps, risks, and plan; isWrite=true for INSERT/UPDATE/DELETE/DDL."
    })
    public AgentToolResult sequentialThinking(
            @P("Thinking request: goal + analysis + isWrite flag")
            ThinkingRequest request) {
        log.info("[Tool] sequentialThinking, goal={}, isWrite={}",
                request != null ? request.getGoal() : null,
                request != null && request.isWrite());
        try {
            validate(request);

            ThinkingOutput output = new ThinkingOutput();
            output.setSummary(buildSummary(request));
            output.setNextAction(suggestNextAction(request));
            output.setRisks(extractRisks(request));

            return AgentToolResult.success(output);
        } catch (Exception e) {
            log.error("[Tool error] sequentialThinking", e);
            return AgentToolResult.fail(e);
        }
    }

    private void validate(ThinkingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ThinkingRequest must not be null");
        }
        if (StringUtils.isBlank(request.getGoal())) {
            throw new IllegalArgumentException("goal must not be blank");
        }
        if (StringUtils.isBlank(request.getAnalysis())) {
            throw new IllegalArgumentException("analysis must not be blank");
        }
    }

    private String buildSummary(ThinkingRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Goal: ").append(request.getGoal()).append("\n");
        sb.append("Analysis: ").append(request.getAnalysis());
        if (request.isWrite()) {
            sb.append("\n[WRITE OPERATION] This task involves data modification. " +
                    "Ensure askUserConfirm is called before executeNonSelectSql.");
        }
        return sb.toString();
    }

    private String suggestNextAction(ThinkingRequest request) {
        if (request.isWrite()) {
            return "Assess impact, then call askUserConfirm before executing write SQL.";
        }
        return "Proceed with exploration or SQL execution as planned.";
    }

    private List<String> extractRisks(ThinkingRequest request) {
        List<String> risks = new ArrayList<>();
        String analysis = request.getAnalysis().toLowerCase();

        if (request.isWrite()) {
            risks.add("Write operation detected — must call askUserConfirm before executeNonSelectSql.");
        }

        Matcher matcher = RISK_PATTERN.matcher(analysis);
        while (matcher.find()) {
            String keyword = matcher.group(1).toLowerCase();
            switch (keyword) {
                case "large table" -> risks.add("Large table detected — consider adding WHERE/LIMIT.");
                case "pii", "sensitive" -> risks.add("Sensitive data — apply minimal disclosure.");
                case "ambiguous", "conflict" -> risks.add("Ambiguity detected — consider askUserQuestion to clarify.");
                case "no index", "missing" -> risks.add("Potential performance issue — verify indexes.");
            }
        }

        return risks;
    }
}
