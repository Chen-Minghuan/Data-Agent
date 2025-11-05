package edu.zsc.ai.plugin.enums;

/**
 * Plugin capability enumeration.
 * Defines all possible capabilities that a plugin can support.
 */
public enum Capability {
    
    /**
     * Connection management capability
     */
    CONNECTION("Connection Management", "Ability to establish and manage database connections");
    
    private final String name;
    private final String description;
    
    Capability(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
}

