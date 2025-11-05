package edu.zsc.ai.plugin.exception;

/**
 * Base exception for plugin-related errors.
 */
public class PluginException extends RuntimeException {
    
    /**
     * Error code
     */
    private final String errorCode;
    
    public PluginException(String message) {
        super(message);
        this.errorCode = PluginErrorCode.PLUGIN_ERROR;
    }
    
    public PluginException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public PluginException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = PluginErrorCode.PLUGIN_ERROR;
    }
    
    public PluginException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

