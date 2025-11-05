package edu.zsc.ai.plugin.context;

/**
 * Plugin runtime context interface.
 * Provides access to host services and resources.
 */
public interface PluginContext {
    
    /**
     * Get configuration value by key
     *
     * @param key configuration key
     * @return configuration value, or null if not found
     */
    String getConfig(String key);
    
    /**
     * Get configuration value by key with default value
     *
     * @param key configuration key
     * @param defaultValue default value if key not found
     * @return configuration value, or default value if not found
     */
    String getConfig(String key, String defaultValue);
    
    /**
     * Get configuration value with type conversion
     *
     * @param key configuration key
     * @param type target type class
     * @param <T> target type
     * @return configuration value converted to target type
     */
    <T> T getConfig(String key, Class<T> type);
    
    /**
     * Get plugin working directory
     *
     * @return plugin working directory path
     */
    String getWorkingDirectory();
    
    /**
     * Get plugin data directory for storing plugin-specific data
     *
     * @return plugin data directory path
     */
    String getDataDirectory();
    
    /**
     * Log info message
     *
     * @param message log message
     */
    void logInfo(String message);
    
    /**
     * Log warning message
     *
     * @param message log message
     */
    void logWarn(String message);
    
    /**
     * Log error message
     *
     * @param message log message
     */
    void logError(String message);
    
    /**
     * Log error message with exception
     *
     * @param message log message
     * @param throwable exception
     */
    void logError(String message, Throwable throwable);
}

