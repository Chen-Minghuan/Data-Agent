package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.DisallowInPlanMode;
import edu.zsc.ai.agent.tool.ask.confirm.WriteConfirmationStore;
import edu.zsc.ai.agent.tool.ask.confirm.WriteConsumeResult;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.error.AgentToolExecuteException;
import edu.zsc.ai.agent.tool.message.SqlToolMessageSupport;
import edu.zsc.ai.agent.tool.sql.model.AgentSqlResult;
import edu.zsc.ai.common.enums.ai.ToolNameEnum;
import edu.zsc.ai.domain.model.context.DbContext;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.domain.service.db.SqlExecutionService;
import edu.zsc.ai.domain.service.db.impl.ConnectionManager;
import edu.zsc.ai.plugin.capability.SqlValidator;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;


@AgentTool
@Slf4j
@RequiredArgsConstructor
public class ExecuteSqlTool {

    private final SqlExecutionService sqlExecutionService;
    private final WriteConfirmationStore writeConfirmationStore;

    @Tool({
        "Calling this tool is the only way to deliver real query results to the user — it greatly improves ",
        "trust; text claims of 'queried' or 'result is' mean nothing without it. ",
        "Executes read-only SQL; pass multiple statements in one call — results in 'results' array.",
        "",
        "When to Use: after you have verified connection, database, and table structure via getObjectDetail.",
        "Relation: call getEnvironmentOverview and searchObjects to resolve target; getObjectDetail for every referenced table; then build SQL and call here. For large tables (>10000 rows) always include WHERE/LIMIT."
    })
    @DisallowInPlanMode(ToolNameEnum.EXECUTE_SELECT_SQL)
    public AgentSqlResult executeSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("List of read-only SQL statements to execute.")
            List<String> sqls,
            InvocationParameters parameters) {
        log.info("{} executeSelectSql, connectionId={}, database={}, schema={}, sqlCount={}",
                "[Tool]", connectionId, databaseName, schemaName,
                Objects.nonNull(sqls) ? sqls.size() : 0);
        if (Objects.isNull(sqls) || sqls.isEmpty()) {
            throw AgentToolExecuteException.preconditionFailed(
                    ToolNameEnum.EXECUTE_SELECT_SQL,
                    SqlToolMessageSupport.requireReadStatements(connectionId, databaseName, schemaName)
            );
        }
        if (!allReadOnly(sqls, connectionId)) {
            throw AgentToolExecuteException.preconditionFailed(
                    ToolNameEnum.EXECUTE_SELECT_SQL,
                    SqlToolMessageSupport.requireReadOnlyStatements(connectionId, databaseName, schemaName)
            );
        }
        DbContext db = new DbContext(connectionId, databaseName, schemaName);
        List<ExecuteSqlResponse> responses = sqlExecutionService.executeBatchSql(db, sqls);
        annotateSqlFailures(responses, connectionId, databaseName, schemaName, sqls, false);
        log.info("{} executeSelectSql", "[Tool done]");
        return AgentSqlResult.fromBatch(responses);
    }

    @Tool({
        "Calling this after askUserConfirm greatly improves safety — it is the only way to execute writes; ",
        "the server rejects writes without prior confirmation. ",
        "Executes write SQL (INSERT, UPDATE, DELETE, DDL); requires askUserConfirm first.",
        "",
        "When to Use: only after askUserConfirm has been called and the user approved the same SQL.",
        "Relation: (1) finalize SQL, (2) call askUserConfirm with impact explanation, (3) after approval call here with the exact same SQL. Accepts a list; results in 'results' array."
    })
    @DisallowInPlanMode(ToolNameEnum.EXECUTE_NON_SELECT_SQL)
    public AgentSqlResult executeNonSelectSql(
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            @P("List of non-SELECT SQL statements to execute (INSERT, UPDATE, DELETE, DDL, etc.).")
            List<String> sqls,
            InvocationParameters parameters) {
        log.info("{} executeNonSelectSql, connectionId={}, database={}, schema={}, sqlCount={}",
                "[Tool]", connectionId, databaseName, schemaName,
                Objects.nonNull(sqls) ? sqls.size() : 0);
        if (Objects.isNull(sqls) || sqls.isEmpty()) {
            throw AgentToolExecuteException.preconditionFailed(
                    ToolNameEnum.EXECUTE_NON_SELECT_SQL,
                    SqlToolMessageSupport.requireWriteStatements(connectionId, databaseName, schemaName)
            );
        }

        DbContext db = new DbContext(connectionId, databaseName, schemaName);
        String joinedSql = String.join(";\n", sqls);
        WriteConsumeResult consumeResult = writeConfirmationStore.consumeConfirmedBySql(db, joinedSql);
        if (!consumeResult.success()) {
            log.warn("[Tool] executeNonSelectSql rejected: reason={}", consumeResult.reason());
            throw AgentToolExecuteException.preconditionFailed(
                    ToolNameEnum.EXECUTE_NON_SELECT_SQL,
                    SqlToolMessageSupport.confirmationBlocked(connectionId, databaseName, schemaName, consumeResult.detail())
            );
        }

        List<ExecuteSqlResponse> responses = sqlExecutionService.executeBatchSql(db, sqls);
        annotateSqlFailures(responses, connectionId, databaseName, schemaName, sqls, true);
        log.info("{} executeNonSelectSql", "[Tool done]");
        return AgentSqlResult.fromBatch(responses);
    }

    private boolean allReadOnly(List<String> sqls, Long connectionId) {
        if (Objects.isNull(sqls) || sqls.isEmpty()) return false;
        String pluginId = ConnectionManager.getAnyActiveConnection(connectionId)
                .map(ConnectionManager.ActiveConnection::pluginId)
                .orElse(null);
        SqlValidator validator = DefaultPluginManager.getInstance()
                .getSqlValidatorByPluginId(Objects.nonNull(pluginId) ? pluginId : "");
        return sqls.stream().allMatch(stmt -> validator.classifySql(stmt).isReadOnly());
    }

    private void annotateSqlFailures(List<ExecuteSqlResponse> responses,
                                     Long connectionId,
                                     String databaseName,
                                     String schemaName,
                                     List<String> sqls,
                                     boolean writeOperation) {
        if (responses == null || responses.isEmpty()) {
            return;
        }
        for (int i = 0; i < responses.size(); i++) {
            ExecuteSqlResponse response = responses.get(i);
            if (response == null || response.isSuccess()) {
                continue;
            }
            String sqlPreview = buildSqlPreview(sqls, i);
            String currentError = StringUtils.defaultIfBlank(response.getErrorMessage(), "unknown database error");
            response.setErrorMessage(SqlToolMessageSupport.failureMessage(
                    writeOperation,
                    connectionId,
                    databaseName,
                    schemaName,
                    i,
                    responses.size(),
                    sqlPreview,
                    currentError
            ));
        }
    }

    private String buildSqlPreview(List<String> sqls, int index) {
        if (sqls == null || index < 0 || index >= sqls.size()) {
            return "";
        }
        String sql = StringUtils.normalizeSpace(sqls.get(index));
        if (StringUtils.isBlank(sql)) {
            return "";
        }
        return sql.length() > 80 ? sql.substring(0, 77) + "..." : sql;
    }
}
