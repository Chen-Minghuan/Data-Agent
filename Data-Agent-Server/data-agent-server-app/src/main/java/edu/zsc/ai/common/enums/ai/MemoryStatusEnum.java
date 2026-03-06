package edu.zsc.ai.common.enums.ai;

import lombok.Getter;

/**
 * Long-term memory status. Maps to SMALLINT in DDL: 0=ACTIVE, 1=ARCHIVED.
 */
@Getter
public enum MemoryStatusEnum {

    ACTIVE(0),
    ARCHIVED(1);

    private final int code;

    MemoryStatusEnum(int code) {
        this.code = code;
    }
}
