package edu.zsc.ai.domain.service.db;

import edu.zsc.ai.plugin.model.metadata.PrimaryKeyMetadata;

import java.util.List;

public interface PrimaryKeyService {

    List<PrimaryKeyMetadata> listPrimaryKeys(Long connectionId, String catalog, String schema, String tableName, Long userId);
}
