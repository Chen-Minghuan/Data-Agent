package edu.zsc.ai.controller.db;

import cn.dev33.satoken.stp.StpUtil;
import edu.zsc.ai.domain.model.dto.request.db.ExecuteSqlRequest;
import edu.zsc.ai.domain.model.dto.response.base.ApiResponse;
import edu.zsc.ai.domain.model.dto.response.db.ExecuteSqlResponse;
import edu.zsc.ai.domain.service.db.SqlExecutionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for executing SQL in the middle SQL editor.
 * userId is taken from current login (StpUtil), not from request body.
 */
@Slf4j
@RestController
@RequestMapping("/api/db")
@RequiredArgsConstructor
public class SqlExecutionController {

    private final SqlExecutionService sqlExecutionService;

    @PostMapping("/sql/execute")
    public ApiResponse<ExecuteSqlResponse> executeSql(@RequestBody @Valid ExecuteSqlApiBody body) {
        long userId = StpUtil.getLoginIdAsLong();
        ExecuteSqlRequest request = ExecuteSqlRequest.builder()
                .connectionId(body.getConnectionId())
                .databaseName(body.getDatabaseName())
                .schemaName(body.getSchemaName())
                .sql(body.getSql())
                .userId(userId)
                .build();
        ExecuteSqlResponse response = sqlExecutionService.executeSql(request);
        return ApiResponse.success(response);
    }

    /**
     * Request body for SQL execution. userId is not accepted from client.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ExecuteSqlApiBody {
        @NotNull(message = "connectionId is required")
        private Long connectionId;
        private String databaseName;
        private String schemaName;
        @NotBlank(message = "sql cannot be null or empty")
        private String sql;
    }
}
