package edu.zsc.ai.plugin.capability;

import edu.zsc.ai.plugin.annotation.CapabilityMarker;
import edu.zsc.ai.plugin.enums.CapabilityEnum;
import edu.zsc.ai.plugin.exception.PluginException;
import edu.zsc.ai.plugin.model.ConnectionConfig;

import java.sql.Connection;

/**
 * Connection provider capability interface.
 * Plugins implementing this interface can establish and manage database connections.
 */
@CapabilityMarker(CapabilityEnum.CONNECTION)
public interface ConnectionProvider {
    
    /**
     * Establish a database connection based on the provided configuration.
     *
     * @param config connection configuration
     * @return database connection
     * @throws PluginException if connection fails
     */
    Connection connect(ConnectionConfig config) throws PluginException;
    
    /**
     * Test whether a connection can be established with the given configuration.
     * This method should not throw exceptions, but return false on failure.
     *
     * @param config connection configuration
     * @return true if connection test succeeds, false otherwise
     */
    boolean testConnection(ConnectionConfig config);
    
    /**
     * Close a database connection and release associated resources.
     *
     * @param connection the connection to close
     * @throws PluginException if closing the connection fails
     */
    void closeConnection(Connection connection) throws PluginException;
}

