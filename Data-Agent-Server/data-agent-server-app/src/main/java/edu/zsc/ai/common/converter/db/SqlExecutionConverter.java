package edu.zsc.ai.common.converter.db;

import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.plugin.model.command.sql.SqlCommandResult;

public class SqlExecutionConverter {

    private SqlExecutionConverter() {
    }

    /**
     * Convert plugin SqlCommandResult to ExecuteSqlResponse DTO.
     */
    public static ExecuteSqlResponse toResponse(SqlCommandResult r) {
        if (r == null) {
            return null;
        }
        return ExecuteSqlResponse.builder()
                .success(r.isSuccess())
                .errorMessage(r.getErrorMessage())
                .executionTimeMs(r.getExecutionTime())
                .query(r.isQuery())
                .headers(r.getHeaders())
                .rows(r.getRows())
                .affectedRows(r.getAffectedRows())
                .build();
    }
}
