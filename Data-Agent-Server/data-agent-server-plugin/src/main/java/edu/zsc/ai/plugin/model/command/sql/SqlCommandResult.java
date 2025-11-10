package edu.zsc.ai.plugin.model.command.sql;

import edu.zsc.ai.plugin.model.command.CommandResult;
import edu.zsc.ai.plugin.model.command.base.BaseCommandResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SqlCommandResult extends BaseCommandResult implements CommandResult {
    private String originalSql;

    private String executedSql;

    private int affectedRows;

    private boolean isQuery;

    private List<String> headers;

    private List<List<Object>> rows;

}
