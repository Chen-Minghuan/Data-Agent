package edu.zsc.ai.plugin.enums;

/**
 * Plugin capability enumeration.
 * Defines all possible capabilities that a plugin can support.
 */
public enum CapabilityEnum {
    
    /**
     * Connection management capability
     */
    CONNECTION("CONNECTION", "Ability to establish and manage database connections");
    
    private final String code;
    private final String description;
    
    CapabilityEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}

