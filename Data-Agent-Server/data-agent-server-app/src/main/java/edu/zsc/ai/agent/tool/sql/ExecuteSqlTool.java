package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.confirm.WriteConfirmationStore;
import edu.zsc.ai.agent.tool.annotation.AgentTool;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.agent.tool.model.AgentSqlResult;
import edu.zsc.ai.domain.model.dto.request.db.AgentExecuteSqlRequest;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.domain.service.db.SqlExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@AgentTool
@Slf4j
@RequiredArgsConstructor
public class ExecuteSqlTool {

    private final SqlExecutionService sqlExecutionService;
    private final WriteConfirmationStore writeConfirmationStore;

    @Tool({
        "[GOAL] Execute read-only SQL (SELECT/WITH/SHOW/EXPLAIN).",
        "[WHEN] Use after data source is resolved and schema is confirmed.",
        "[WHEN_NOT] Do not use for INSERT/UPDATE/DELETE/DDL — use executeNonSelectSql. Do not call before data source is resolved.",
        "[SAFETY] For large tables (>10000 rows), include WHERE/LIMIT."
    })
    public AgentSqlResult executeSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("The SELECT statement to execute.")
            String sql,
            InvocationParameters parameters) {
        log.info("{} executeSelectSql, connectionId={}, database={}, schema={}, sqlLength={}",
                "[Tool]", connectionId, databaseName, schemaName,
                sql != null ? sql.length() : 0);
        try {
            if (!isReadOnlySql(sql)) {
                return AgentSqlResult.fail("Only read-only statements (SELECT, WITH, SHOW, EXPLAIN) are allowed.");
            }
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (userId == null) {
                return AgentSqlResult.fail("User context is missing.");
            }
            AgentExecuteSqlRequest request = AgentExecuteSqlRequest.builder()
                    .connectionId(connectionId)
                    .databaseName(databaseName)
                    .schemaName(schemaName)
                    .sql(sql)
                    .userId(userId)
                    .build();
            ExecuteSqlResponse response = sqlExecutionService.executeSql(request);
            log.info("{} executeSelectSql", "[Tool done]");
            return AgentSqlResult.from(response);
        } catch (Exception e) {
            log.error("{} executeSelectSql", "[Tool error]", e);
            return AgentSqlResult.fail(e.getMessage());
        }
    }

    @Tool({
        "[GOAL] Execute write SQL (INSERT/UPDATE/DELETE/DDL) after user confirmation.",
        "[WHEN] Use only after askUserConfirm and user has confirmed.",
        "[WHEN_NOT] Do not use for read-only queries — use executeSelectSql. Do not call without prior askUserConfirm.",
        "[SAFETY] Server validates confirmation token; missing/expired confirmation is auto-rejected."
    })
    public AgentSqlResult executeNonSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("The non-SELECT statement to execute (INSERT, UPDATE, DELETE, DDL, etc.).")
            String sql,
            InvocationParameters parameters) {
        log.info("{} executeNonSelectSql, connectionId={}, database={}, schema={}, sqlLength={}",
                "[Tool]", connectionId, databaseName, schemaName,
                sql != null ? sql.length() : 0);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            Long conversationId = parameters.get(RequestContextConstant.CONVERSATION_ID);
            if (userId == null || conversationId == null) {
                return AgentSqlResult.fail("User or conversation context is missing.");
            }

            boolean consumed = writeConfirmationStore.consumeConfirmedBySql(
                    userId, conversationId, connectionId, databaseName, schemaName, sql);
            if (!consumed) {
                log.warn("[Tool] executeNonSelectSql rejected: no CONFIRMED token for userId={} conversationId={}",
                        userId, conversationId);
                return AgentSqlResult.fail("Write operation rejected: no valid user confirmation found. "
                        + "You must call askUserConfirm first and wait for the user to confirm.");
            }

            AgentExecuteSqlRequest request = AgentExecuteSqlRequest.builder()
                    .connectionId(connectionId)
                    .databaseName(databaseName)
                    .schemaName(schemaName)
                    .sql(sql)
                    .userId(userId)
                    .build();
            ExecuteSqlResponse response = sqlExecutionService.executeSql(request);
            log.info("{} executeNonSelectSql", "[Tool done]");
            return AgentSqlResult.from(response);
        } catch (Exception e) {
            log.error("{} executeNonSelectSql", "[Tool error]", e);
            return AgentSqlResult.fail(e.getMessage());
        }
    }

    private boolean isReadOnlySql(String sql) {
        if (sql == null || sql.isBlank()) return false;
        String stripped = sql.stripLeading().replaceAll("(?s)/\\*.*?\\*/", "").stripLeading();
        String firstWord = stripped.split("\\s+")[0].toUpperCase();
        return switch (firstWord) {
            case "SELECT", "WITH", "SHOW", "EXPLAIN" -> true;
            default -> false;
        };
    }
}
