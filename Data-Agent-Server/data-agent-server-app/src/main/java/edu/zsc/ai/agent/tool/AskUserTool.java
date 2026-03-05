package edu.zsc.ai.agent.tool;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.confirm.WriteConfirmationEntry;
import edu.zsc.ai.agent.confirm.WriteConfirmationStore;
import edu.zsc.ai.agent.tool.model.UserQuestion;
import edu.zsc.ai.common.constant.RequestContextConstant;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified tool class for asking user questions and requesting write confirmation.
 */
@AgentTool
@Slf4j
@RequiredArgsConstructor
public class AskUserTool {

    private final WriteConfirmationStore confirmationStore;

    @Tool(
            value = {
                    "[GOAL] Resolve business ambiguity before SQL generation/execution (intent, metric definition, time range, filters, target source).",
                    "[PRECHECK] Use this only for user-owned decisions that cannot be inferred from tools; tool-discoverable facts should be fetched via exploration tools first.",
                    "[WHEN] Call when intent is ambiguous, multiple same-name candidates exist, filter values are uncertain, or critical constraints are missing.",
                    "[INPUT] Each question should have 2-3 options (max 3), plus optional free text. Prefer concrete options over open-ended prompts.",
                    "[BOUNDARY] NEVER use askUserQuestion for write confirmation; write operations must use askUserConfirm.",
                    "[AFTER] Interpret answers, lock selected scope/constraints, then continue with exploration or SQL execution."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public List<UserQuestion> askUserQuestion(
            @P("List of questions to ask the user. Each question should have 2-3 options (maximum 3).")
            List<UserQuestion> questions) {

        log.info("[Tool] askUserQuestion, {} question(s)", questions == null ? 0 : questions.size());
        return questions;
    }

    @Tool(
            value = {
                    "[GOAL] Enforce explicit user approval before any write SQL (INSERT/UPDATE/DELETE/DDL).",
                    "[PRECHECK] SQL must already be concrete and impact-explained; do not call this for unresolved intent.",
                    "[WHEN] MUST be called before every write operation. Without it, executeNonSelectSql should be rejected.",
                    "[INPUT] Pass exact SQL, connectionId, and clear impact explanation; include database/schema only when applicable.",
                    "[AFTER] Only after user confirmation, call executeNonSelectSql with the same SQL and user-confirmed tableName context.",
                    "[FAILSAFE] If user rejects/cancels or context is missing, stop write execution and return to clarification/planning."
            },
            returnBehavior = ReturnBehavior.IMMEDIATE
    )
    public WriteConfirmationResult askUserConfirm(
            @P("The exact SQL statement to be executed (INSERT, UPDATE, DELETE, or DDL)")
            String sql,
            @P("Connection id from current session context")
            Long connectionId,
            @P(value = "Database (catalog) name from current session context; omit or null for operations not bound to a specific database (e.g. CREATE DATABASE)", required = false)
            String databaseName,
            @P(value = "Schema name from current session context; omit or null if not applicable", required = false)
            String schemaName,
            @P("Brief explanation of what this operation does and its potential impact")
            String explanation,
            InvocationParameters parameters) {

        log.info("[Tool] askUserConfirm, connectionId={}, database={}, schema={}, sqlLength={}",
                connectionId, databaseName, schemaName, sql != null ? sql.length() : 0);

        Long userId = parameters.get(RequestContextConstant.USER_ID);
        Long conversationId = parameters.get(RequestContextConstant.CONVERSATION_ID);

        if (userId == null || conversationId == null) {
            log.error("[Tool] askUserConfirm: missing context userId={} conversationId={}", userId, conversationId);
            return WriteConfirmationResult.error("User or conversation context is missing.");
        }

        WriteConfirmationEntry entry = confirmationStore.create(
                userId, conversationId, connectionId, sql, databaseName, schemaName);

        log.info("[Tool done] askUserConfirm, token={}", entry.getToken());
        return WriteConfirmationResult.builder()
                .confirmationToken(entry.getToken())
                .sqlPreview(sql)
                .explanation(explanation)
                .connectionId(connectionId)
                .databaseName(databaseName)
                .schemaName(schemaName)
                .expiresInSeconds(300)
                .build();
    }

    /**
     * Result returned to the frontend via SSE (serialized as JSON).
     * The frontend parses this as WriteConfirmPayload.
     */
    @Data
    @Builder
    public static class WriteConfirmationResult {
        private String confirmationToken;
        private String sqlPreview;
        private String explanation;
        private Long connectionId;
        private String databaseName;
        private String schemaName;
        private long expiresInSeconds;
        private boolean error;
        private String errorMessage;

        public static WriteConfirmationResult error(String message) {
            return WriteConfirmationResult.builder()
                    .error(true)
                    .errorMessage(message)
                    .build();
        }
    }
}
