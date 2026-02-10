package edu.zsc.ai.domain.service.db;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.zsc.ai.domain.model.dto.request.db.ConnectionCreateRequest;
import edu.zsc.ai.domain.model.dto.response.db.ConnectionResponse;
import edu.zsc.ai.domain.model.entity.db.DbConnection;

import java.util.List;

public interface DbConnectionService extends IService<DbConnection> {

    DbConnection getByName(String name);

    DbConnection getOwnedById(Long id);

    DbConnection getOwnedById(Long id, Long userId);

    ConnectionResponse createConnection(ConnectionCreateRequest request);

    ConnectionResponse updateConnection(ConnectionCreateRequest request);

    ConnectionResponse getConnectionById(Long id);

    /**
     * Get connection by id for a specific user. When userId is null, uses current login (StpUtil).
     */
    ConnectionResponse getConnectionById(Long id, Long userId);

    List<ConnectionResponse> getAllConnections();

    /**
     * Get all connections for a specific user. When userId is null, uses current login (StpUtil).
     */
    List<ConnectionResponse> getAllConnections(Long userId);

    void deleteConnection(Long id);
}