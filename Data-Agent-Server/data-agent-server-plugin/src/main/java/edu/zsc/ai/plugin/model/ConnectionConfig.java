package edu.zsc.ai.plugin.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Database connection configuration.
 * Contains all necessary information to establish a database connection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionConfig {
    
    /**
     * Database host address (e.g., "localhost", "192.168.1.100")
     */
    @NotBlank(message = "Host cannot be null or empty")
    private String host;
    
    /**
     * Database port (e.g., 3306 for MySQL, 5432 for PostgreSQL)
     */
    @Min(value = 1, message = "Port must be a positive integer")
    private Integer port;
    
    /**
     * Database name / schema name
     */
    private String database;
    
    /**
     * Username for authentication
     */
    private String username;
    
    /**
     * Password for authentication
     */
    private String password;
    
    /**
     * Additional connection properties (e.g., SSL settings, timeout, encoding)
     */
    @Builder.Default
    private Map<String, String> properties = new HashMap<>();
    
    /**
     * Path to external JDBC driver JAR file.
     * Required. Drivers are loaded dynamically from external JAR files.
     */
    @NotBlank(message = "Driver JAR path cannot be null or empty")
    private String driverJarPath;
    
    /**
     * Connection timeout in seconds
     */
    @Builder.Default
    @Min(value = 1, message = "Timeout must be at least 1 second")
    private Integer timeout = 30;
    
    /**
     * Add a connection property
     *
     * @param key property key
     * @param value property value
     */
    public void addProperty(String key, String value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(key, value);
    }
    
    /**
     * Get property value by key
     *
     * @param key property key
     * @return property value, or null if not found
     */
    public String getProperty(String key) {
        return properties != null ? properties.get(key) : null;
    }
}

