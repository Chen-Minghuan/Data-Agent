package edu.zsc.ai.plugin.annotation;

import edu.zsc.ai.plugin.enums.CapabilityEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Capability marker annotation.
 * Used to mark capability interfaces with their corresponding capability identifier.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CapabilityMarker {
    
    /**
     * Capability identifier
     *
     * @return capability enum value
     */
    CapabilityEnum value();
}

