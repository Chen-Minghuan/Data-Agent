package edu.zsc.ai.agent.tool.ask;

import java.util.List;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.ReturnBehavior;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.confirm.WriteConfirmationEntry;
import edu.zsc.ai.agent.confirm.WriteConfirmationStore;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
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
                    "[GOAL] Ask the user structured clarification/selection questions to resolve ambiguity.",
                    "[WHEN] Call when intent is ambiguous, multiple candidates exist, or critical constraints are missing.",
                    "[WHEN_NOT] Do not use for write operation confirmation — use askUserConfirm. Do not ask questions that tools can answer (e.g., table existence, schema info).",
                    "[INPUT] Each question should have 2-3 options (max 3). Prefer concrete options over open-ended prompts."
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
                    "[GOAL] Request explicit user approval before any write SQL (INSERT/UPDATE/DELETE/DDL).",
                    "[WHEN] MUST be called before every write operation. Without it, executeNonSelectSql will be rejected.",
                    "[WHEN_NOT] Do not use for clarification questions — use askUserQuestion. Do not call before SQL is finalized.",
                    "[INPUT] Pass exact SQL, connectionId, and clear impact explanation."
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
