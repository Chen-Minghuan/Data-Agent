package edu.zsc.ai.agent.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.common.constant.RequestContextConstant;
import edu.zsc.ai.domain.service.db.TableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Objects;


@AgentTool
@Slf4j
@RequiredArgsConstructor
public class TableTool {

    private final TableService tableService;

    @Tool({
        "[GOAL] Estimate schema breadth before object exploration.",
        "[PRECHECK] connection/catalog/schema should be selected; pattern should reflect user intent scope.",
        "[WHEN] Call before broad table searches to avoid uncontrolled listing in large schemas.",
        "[SAFETY] If count is high (e.g., >50), refine pattern and avoid blind enumeration.",
        "[AFTER] Continue with targeted searchObjects/getObjectNames or askUserQuestion for narrower scope."
    })
    public AgentToolResult countTables(
            @P(value = "Table name pattern. Supports '%' and '_' wildcards. Pass null or '%' to count all tables.", required = false) String tableNamePattern,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} countTables, pattern={}, connectionId={}, database={}, schema={}",
                "[Tool]", tableNamePattern, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }
            int count = tableService.searchTables(connectionId, databaseName, schemaName, tableNamePattern, userId).size();
            log.info("{} countTables, pattern={}, count={}", "[Tool done]", tableNamePattern, count);
            return AgentToolResult.success(count);
        } catch (Exception e) {
            log.error("{} countTables", "[Tool error]", e);
            return AgentToolResult.fail(e);
        }
    }

    @Tool({
        "[GOAL] Estimate data volume to enforce safe query shape.",
        "[PRECHECK] tableName should be resolved and fully qualified in user-confirmed scope.",
        "[WHEN] Call before SELECT to decide if WHERE/LIMIT/pagination is mandatory.",
        "[SAFETY] Large row counts (e.g., >10000) require bounded SQL; avoid unfiltered scans.",
        "[AFTER] Feed result into executeSelectSql planning and response risk explanation."
    })
    public AgentToolResult countTableRows(
            @P("The exact name of the table to count rows in") String tableName,
            @P("Connection id from current session context") Long connectionId,
            @P("Database (catalog) name from current session context") String databaseName,
            @P(value = "Schema name from current session context; omit if not used", required = false) String schemaName,
            InvocationParameters parameters) {
        log.info("{} countTableRows, tableName={}, connectionId={}, database={}, schema={}",
                "[Tool]", tableName, connectionId, databaseName, schemaName);
        try {
            Long userId = parameters.get(RequestContextConstant.USER_ID);
            if (Objects.isNull(userId)) {
                return AgentToolResult.noContext();
            }
            long count = tableService.countTableRows(connectionId, databaseName, schemaName, tableName, userId);
            log.info("{} countTableRows, tableName={}, count={}", "[Tool done]", tableName, count);
            return AgentToolResult.success(count);
        } catch (Exception e) {
            log.error("{} countTableRows, tableName={}", "[Tool error]", tableName, e);
            return AgentToolResult.fail(e);
        }
    }
}
