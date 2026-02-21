package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.domain.service.db.ColumnService;
import edu.zsc.ai.domain.service.db.ConnectionService;
import edu.zsc.ai.plugin.capability.ColumnProvider;
import edu.zsc.ai.plugin.manager.DefaultPluginManager;
import edu.zsc.ai.plugin.model.metadata.ColumnMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ColumnServiceImpl implements ColumnService {

    private final ConnectionService connectionService;

    @Override
    public List<ColumnMetadata> listColumns(Long connectionId, String catalog, String schema, String tableName, Long userId) {
        connectionService.openConnection(connectionId, catalog, schema, userId);

        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);

        ColumnProvider provider = DefaultPluginManager.getInstance().getColumnProviderByPluginId(active.pluginId());
        return provider.getColumns(active.connection(), catalog, schema, tableName);
    }
}
