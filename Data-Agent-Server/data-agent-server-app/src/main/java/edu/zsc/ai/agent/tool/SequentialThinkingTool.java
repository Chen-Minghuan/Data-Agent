package edu.zsc.ai.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@AgentTool
@Slf4j
public class SequentialThinkingTool {

    @Tool({
            "[GOAL] Provide a stateless reasoning checkpoint to choose the safest next action in complex workflows.",
            "[PRECHECK] Prefer this by default before first SQL, because seemingly simple requests may hide source/scope/filter ambiguity.",
            "[WHEN] Call before major branch decisions: askUserQuestion vs exploration vs SQL vs askUserConfirm.",
            "[WRITE-SAFETY] For write paths, run this with phase=SAFETY before askUserConfirm.",
            "[BYPASS] Skip only for truly trivial read tasks where source is unique/resolved, schema is known, filters are explicit, and no write risk exists.",
            "[HOW] Pass current thought + progress flags. Tool validates structure and returns recommended next action.",
            "[BOUNDARY] Stateless only: no server-side history is persisted."
    })
    public AgentToolResult sequentialThinking(
            @P("Current thought content for this step") String thought,
            @P("Whether another thought step is needed") Boolean nextThoughtNeeded,
            @P("Current thought number, starting from 1") Integer thoughtNumber,
            @P("Estimated total thoughts needed, can be adjusted dynamically") Integer totalThoughts,
            @P(value = "Optional phase: INTENT/SOURCE/SCHEMA/SQL/SAFETY/EXECUTION/RESPONSE", required = false)
            String phase,
            @P(value = "Whether data source is already resolved (connection/database/schema)", required = false)
            Boolean sourceResolved,
            @P(value = "Whether this plan includes a write SQL operation", required = false)
            Boolean writeOperation,
            @P(value = "Whether user clarification is still needed", required = false)
            Boolean needUserQuestion) {
        log.info("[Tool] sequentialThinking, thoughtNumber={}, totalThoughts={}, phase={}",
                thoughtNumber, totalThoughts, phase);
        try {
            if (StringUtils.isBlank(thought)) {
                throw new IllegalArgumentException("thought must not be blank");
            }
            if (nextThoughtNeeded == null) {
                throw new IllegalArgumentException("nextThoughtNeeded must not be null");
            }
            int normalizedThoughtNumber = normalizePositive(thoughtNumber, "thoughtNumber");
            int normalizedTotalThoughts = normalizePositive(totalThoughts, "totalThoughts");
            if (normalizedThoughtNumber > normalizedTotalThoughts) {
                normalizedTotalThoughts = normalizedThoughtNumber;
            }

            ThinkingPhase normalizedPhase = normalizePhase(phase);
            boolean normalizedSourceResolved = Boolean.TRUE.equals(sourceResolved);
            boolean normalizedWriteOperation = Boolean.TRUE.equals(writeOperation);
            boolean normalizedNeedUserQuestion = Boolean.TRUE.equals(needUserQuestion);

            String recommendedNextAction = recommendAction(
                    normalizedPhase,
                    normalizedSourceResolved,
                    normalizedWriteOperation,
                    normalizedNeedUserQuestion,
                    nextThoughtNeeded
            );

            Map<String, Object> checklist = new LinkedHashMap<>();
            checklist.put("sourceResolved", normalizedSourceResolved);
            checklist.put("writeOperation", normalizedWriteOperation);
            checklist.put("needUserQuestion", normalizedNeedUserQuestion);
            checklist.put("writeSafetyGatePassed", !normalizedWriteOperation || ThinkingPhase.SAFETY.equals(normalizedPhase));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("thoughtNumber", normalizedThoughtNumber);
            result.put("totalThoughts", normalizedTotalThoughts);
            result.put("nextThoughtNeeded", nextThoughtNeeded);
            result.put("phase", normalizedPhase.name());
            result.put("recommendedNextAction", recommendedNextAction);
            result.put("checklist", checklist);
            result.put("note", "Stateless step only. No history was stored.");

            log.info("[Tool done] sequentialThinking, thoughtNumber={}, totalThoughts={}, action={}",
                    normalizedThoughtNumber, normalizedTotalThoughts, recommendedNextAction);
            return AgentToolResult.success(result);
        } catch (Exception e) {
            log.error("[Tool error] sequentialThinking", e);
            return AgentToolResult.fail(e);
        }
    }

    private int normalizePositive(Integer value, String fieldName) {
        if (value == null || value < 1) {
            throw new IllegalArgumentException(fieldName + " must be >= 1");
        }
        return value;
    }

    private ThinkingPhase normalizePhase(String phase) {
        if (StringUtils.isBlank(phase)) {
            return ThinkingPhase.INTENT;
        }
        try {
            return ThinkingPhase.valueOf(phase.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported phase: " + phase);
        }
    }

    private String recommendAction(ThinkingPhase phase,
                                   boolean sourceResolved,
                                   boolean writeOperation,
                                   boolean needUserQuestion,
                                   boolean nextThoughtNeeded) {
        if (!nextThoughtNeeded) {
            return "respond";
        }
        if (needUserQuestion) {
            return "askUserQuestion";
        }
        if (!sourceResolved) {
            return "getMyConnections -> getCatalogNames -> searchObjects";
        }
        if (writeOperation && !ThinkingPhase.SAFETY.equals(phase)) {
            return "askUserConfirm";
        }
        return switch (phase) {
            case SOURCE -> "searchObjects";
            case SCHEMA -> "getObjectDdl";
            case SQL, EXECUTION -> "executeSelectSql";
            case SAFETY -> writeOperation ? "askUserConfirm" : "executeSelectSql";
            case RESPONSE -> "respond";
            default -> "continue-thinking";
        };
    }

    private enum ThinkingPhase {
        INTENT,
        SOURCE,
        SCHEMA,
        SQL,
        SAFETY,
        EXECUTION,
        RESPONSE
    }
}
