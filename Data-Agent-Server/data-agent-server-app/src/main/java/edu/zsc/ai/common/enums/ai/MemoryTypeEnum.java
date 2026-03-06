package edu.zsc.ai.common.enums.ai;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

/**
 * Supported long-term memory types.
 */
public enum MemoryTypeEnum {

    PREFERENCE,
    BUSINESS_RULE,
    KNOWLEDGE_POINT,
    GOLDEN_SQL_CASE,
    WORKFLOW_CONSTRAINT;

    public static MemoryTypeEnum fromValue(String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Memory type cannot be blank");
        }
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(v -> v.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported memory type: " + value));
    }
}
