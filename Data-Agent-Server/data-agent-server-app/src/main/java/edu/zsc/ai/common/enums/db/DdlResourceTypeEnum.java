package edu.zsc.ai.common.enums.db;

public enum DdlResourceTypeEnum {

    TABLE("table"),
    VIEW("view"),
    FUNCTION("function"),
    PROCEDURE("procedure"),
    TRIGGER("trigger");

    private final String value;

    DdlResourceTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
