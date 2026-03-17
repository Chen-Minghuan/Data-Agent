package edu.zsc.ai.agent.tool.sql;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.invocation.InvocationParameters;
import edu.zsc.ai.agent.annotation.AgentTool;
import edu.zsc.ai.agent.tool.ToolContext;
import edu.zsc.ai.agent.tool.model.AgentToolResult;
import edu.zsc.ai.context.RequestContext;
import edu.zsc.ai.util.ConnectionIdUtil;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchQuery;
import edu.zsc.ai.agent.tool.sql.model.ObjectSearchResponse;
import edu.zsc.ai.domain.service.db.DiscoveryService;
import edu.zsc.ai.plugin.constant.DatabaseObjectTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Pattern (fuzzy) search across connections for tables/views/functions.
 * Explorer SubAgent uses this for schema discovery.
 */
@AgentTool
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchObjectsTool {

    private final DiscoveryService discoveryService;

    @Tool({
            "Pattern search across all connections. Use SQL wildcards: %order%, %user_%.",
            "Required: objectNamePattern. Optional: connectionId, databaseName, schemaName.",
            "databaseName requires connectionId; schemaName requires connectionId + databaseName.",
            "Results capped at 100. objectType omitted = TABLE + VIEW."
    })
    public AgentToolResult searchObjects(
            @P("Search query parameters") ObjectSearchQuery query,
            InvocationParameters parameters) {
        try (var ctx = ToolContext.from(parameters)) {
            String objectNamePattern = query.getObjectNamePattern();
            String objectType = query.getObjectType();
            Long connectionId = ConnectionIdUtil.toLong(query.getConnectionId());
            if (connectionId == null) connectionId = RequestContext.getConnectionId();
            String databaseName = query.getDatabaseName();
            String schemaName = query.getSchemaName();

            log.info("[Tool] searchObjects, pattern={}, type={}, connectionId={}, database={}, schema={}",
                    objectNamePattern, objectType, connectionId, databaseName, schemaName);

            if (StringUtils.isNotBlank(schemaName) && StringUtils.isBlank(databaseName)) {
                return AgentToolResult.fail("schemaName requires databaseName to be specified.");
            }
            if (StringUtils.isNotBlank(databaseName) && connectionId == null) {
                return AgentToolResult.fail("databaseName requires connectionId to be specified.");
            }

            DatabaseObjectTypeEnum normalizedType = StringUtils.isNotBlank(objectType)
                    ? DatabaseObjectTypeEnum.parseQueryable(objectType)
                    : null;

            ObjectSearchResponse response = discoveryService.searchObjects(
                    objectNamePattern, normalizedType, connectionId, databaseName, schemaName);

            if (CollectionUtils.isEmpty(response.results())) {
                log.info("[Tool done] searchObjects -> empty");
                return ctx.timed(AgentToolResult.empty());
            }

            log.info("[Tool done] searchObjects, resultCount={}, truncated={}",
                    response.totalCount(), response.truncated());
            return ctx.timed(AgentToolResult.success(response));
        } catch (Exception e) {
            log.error("[Tool error] searchObjects, pattern={}", query.getObjectNamePattern(), e);
            String errorMsg = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
            return AgentToolResult.fail("Failed to search objects with pattern '" + query.getObjectNamePattern() + "': " + errorMsg);
        }
    }
}
