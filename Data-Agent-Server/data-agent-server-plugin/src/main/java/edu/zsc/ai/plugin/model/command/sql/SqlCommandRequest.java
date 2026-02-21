package edu.zsc.ai.plugin.model.command.sql;

import edu.zsc.ai.plugin.model.command.CommandRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Connection;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SqlCommandRequest implements CommandRequest {

    private Connection connection;

    private String originalSql;

    private String executeSql;

    private String database;

    private String schema;

    private boolean needTransaction;

    /** PreparedStatement parameters, null or empty uses Statement */
    private Object[] params;

    @Override
    public String getCommand() {
        return originalSql;
    }
}
