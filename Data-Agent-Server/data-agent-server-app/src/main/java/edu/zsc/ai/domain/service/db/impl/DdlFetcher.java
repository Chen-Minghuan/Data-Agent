package edu.zsc.ai.domain.service.db.impl;

import edu.zsc.ai.common.enums.db.DdlResourceTypeEnum;
import edu.zsc.ai.domain.service.db.ConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Slf4j
@Component
public class DdlFetcher {

    private final ConnectionService connectionService;

    public DdlFetcher(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    public String fetch(Long connectionId, String catalog, String schema, String name, Long userId,
                        DdlResourceTypeEnum resourceType,
                        Function<ConnectionManager.ActiveConnection, String> extractor) {
        log.info("Getting DDL for {}: connectionId={}, catalog={}, schema={}, name={}",
                resourceType.getValue(), connectionId, catalog, schema, name);
        connectionService.openConnection(connectionId, catalog, schema, userId);
        ConnectionManager.ActiveConnection active = ConnectionManager.getOwnedConnection(connectionId, catalog, schema, userId);
        String ddl = extractor.apply(active);
        log.debug("Successfully retrieved DDL for {}: {}", resourceType.getValue(), name);
        return ddl;
    }
}
