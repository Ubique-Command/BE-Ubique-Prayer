package com.ubique.prayer.communityunit.domain;

public enum UnitType {
    NEWCOMER(0),
    CELL(1),
    MINISTRY(2),
    INTERCESSION(3);

    private final int code;

    UnitType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static UnitType fromCode(int code) {
        for (UnitType value : values()) {
            if (value.code == code) return value;
        }
        throw new IllegalArgumentException("Unknown UnitType code: " + code);
    }
}
