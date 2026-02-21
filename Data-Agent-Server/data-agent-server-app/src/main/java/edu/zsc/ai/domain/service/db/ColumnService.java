package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.plugin.model.metadata.ColumnMetadata;

import java.util.List;

public interface ColumnService {

    List<ColumnMetadata> listColumns(Long connectionId, String catalog, String schema, String tableName, Long userId);
}
