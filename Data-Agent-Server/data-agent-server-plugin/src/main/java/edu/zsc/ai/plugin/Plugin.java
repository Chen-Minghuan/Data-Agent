package edu.zsc.ai.plugin;

import edu.zsc.ai.plugin.context.PluginContext;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.enums.PluginType;
import edu.zsc.ai.plugin.exception.PluginException;

import java.util.Set;


/**
 * Top-level database plugin interface.
 * All plugin implementations must inherit from this interface.
 */
public interface Plugin {
    
    // ========== Plugin Identification ==========
    
    /**
     * Get plugin unique identifier
     *
     * @return plugin id (lowercase, e.g., "mysql")
     */
    String getPluginId();
    
    /**
     * Get plugin display name
     *
     * @return plugin display name (e.g., "MySQL Database")
     */
    String getDisplayName();
    
    /**
     * Get plugin version
     *
     * @return plugin version (e.g., "1.0.0")
     */
    String getVersion();
    
    /**
     * Get database type
     *
     * @return database type
     */
    DbType getDbType();
    
    /**
     * Get plugin type
     *
     * @return plugin type (SQL/NoSQL)
     */
    PluginType getPluginType();
    
    /**
     * Get plugin description
     *
     * @return plugin description
     */
    String getDescription();
    
    /**
     * Get vendor/author name
     *
     * @return vendor name
     */
    String getVendor();
    
    /**
     * Get official website URL
     *
     * @return website URL
     */
    String getWebsite();
    
    // ========== Database Version Support ==========
    
    /**
     * Get minimum supported database version
     *
     * @return minimum database version (e.g., "5.7.0")
     */
    String getMinimumDatabaseVersion();
    
    /**
     * Get maximum supported database version.
     * Empty string means supporting all future versions.
     *
     * @return maximum database version (e.g., "5.7.99"), or empty string
     */
    String getMaximumDatabaseVersion();
    
    /**
     * Get all supported capabilities of this plugin.
     * Capabilities are automatically collected from implemented capability interfaces.
     *
     * @return set of capability identifiers
     */
    Set<String> getSupportedCapabilities();
    
    // ========== Lifecycle Methods ==========
    
    /**
     * Initialize plugin with context.
     * Called once when plugin is loaded.
     *
     * @param context plugin runtime context
     * @throws PluginException if initialization fails
     */
    default void initialize(PluginContext context) throws PluginException {
        // Default empty implementation
    }
    
    /**
     * Start plugin.
     * Called after initialization.
     *
     * @throws PluginException if start fails
     */
    default void start() throws PluginException {
        // Default empty implementation
    }
    
    /**
     * Stop plugin.
     * Called before plugin unload.
     *
     * @throws PluginException if stop fails
     */
    default void stop() throws PluginException {
        // Default empty implementation
    }
    
    /**
     * Destroy plugin and release all resources.
     * Called when plugin is being unloaded.
     *
     * @throws PluginException if destroy fails
     */
    default void destroy() throws PluginException {
        // Default empty implementation
    }
}

