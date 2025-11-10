package edu.zsc.ai.plugin.connection;

import java.util.Properties;

/**
 * Builder for JDBC connection components.
 * Constructs JDBC URL and connection properties from configuration.
 */
public interface JdbcConnectionBuilder {

    /**
     * Build JDBC URL from connection configuration.
     *
     * @param config connection configuration
     * @param urlTemplate JDBC URL template (e.g., "jdbc:mysql://%s:%d/%s")
     * @param defaultPort default port if not specified in config
     * @return JDBC URL string
     */
    String buildUrl(ConnectionConfig config, String urlTemplate, int defaultPort);

    /**
     * Build connection properties from configuration.
     *
     * @param config connection configuration
     * @return Properties object with connection parameters
     */
    Properties buildProperties(ConnectionConfig config);
}

