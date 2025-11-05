package edu.zsc.ai.plugin.mysql;

import edu.zsc.ai.plugin.annotation.PluginInfo;
import edu.zsc.ai.plugin.context.PluginContext;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.exception.PluginException;

/**
 * MySQL 5.7 database plugin implementation.
 * Supports MySQL 5.7.x versions.
 * ConnectionProvider capability is inherited from AbstractMysqlPlugin.
 */
@PluginInfo(
    id = "mysql-5.7",
    name = "MySQL 5.7",
    version = "0.0.1",
    dbType = DbType.MYSQL,
    description = "MySQL 5.7 database plugin with full CRUD and metadata support",
    minDatabaseVersion = "5.7.0",
    maxDatabaseVersion = "7.9.99"
)
public class Mysql57Plugin extends AbstractMysqlPlugin {
    
    @Override
    protected String getDriverClassName() {
        return "com.mysql.jdbc.Driver";
    }
    
    @Override
    public void initialize(PluginContext context) throws PluginException {
        super.initialize(context);
        logInfo("MySQL 5.7 plugin initialized successfully");
    }
    
    @Override
    public void start() throws PluginException {
        super.start();
        logInfo("MySQL 5.7 plugin started successfully");
    }
    
    @Override
    public void stop() throws PluginException {
        super.stop();
        logInfo("MySQL 5.7 plugin stopped successfully");
    }
    
    @Override
    public void destroy() throws PluginException {
        logInfo("MySQL 5.7 plugin destroying resources");
        super.destroy();
    }
}

