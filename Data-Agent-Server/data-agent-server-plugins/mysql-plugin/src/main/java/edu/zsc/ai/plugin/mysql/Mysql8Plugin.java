package edu.zsc.ai.plugin.mysql;

import edu.zsc.ai.plugin.annotation.PluginInfo;
import edu.zsc.ai.plugin.context.PluginContext;
import edu.zsc.ai.plugin.enums.DbType;
import edu.zsc.ai.plugin.exception.PluginException;

/**
 * MySQL 8.0+ database plugin implementation.
 * Supports MySQL 8.0 and later versions.
 * ConnectionProvider capability is inherited from AbstractMysqlPlugin.
 */
@PluginInfo(
    id = "mysql-8",
    name = "MySQL 8.0+",
    version = "0.0.1",
    dbType = DbType.MYSQL,
    description = "MySQL 8.0+ database plugin with full CRUD and metadata support, including new features like CTE and window functions",
    minDatabaseVersion = "8.0.0"
)
public class Mysql8Plugin extends AbstractMysqlPlugin {
    
    @Override
    protected String getDriverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }
    
    @Override
    public void initialize(PluginContext context) throws PluginException {
        super.initialize(context);
        logInfo("MySQL 8.0+ plugin initialized successfully");
    }
    
    @Override
    public void start() throws PluginException {
        super.start();
        logInfo("MySQL 8.0+ plugin started successfully");
    }
    
    @Override
    public void stop() throws PluginException {
        super.stop();
        logInfo("MySQL 8.0+ plugin stopped successfully");
    }
    
    @Override
    public void destroy() throws PluginException {
        logInfo("MySQL 8.0+ plugin destroying resources");
        super.destroy();
    }
}

