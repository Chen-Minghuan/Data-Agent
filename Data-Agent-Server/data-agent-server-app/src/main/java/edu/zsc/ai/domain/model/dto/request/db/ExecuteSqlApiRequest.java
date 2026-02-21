package edu.zsc.ai.domain.model.dto.request.db;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * API request body for SQL execution (connectionId, databaseName, schemaName, sql).
 * userId is set from Sa-Token in controller.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteSqlApiRequest {

    @NotNull(message = "connectionId is required")
    private Long connectionId;

    private String databaseName;

    private String schemaName;

    @NotBlank(message = "SQL cannot be null or empty")
    private String sql;
}
