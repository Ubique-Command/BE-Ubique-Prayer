package com.ubique.prayer.communityunit.domain;

public enum UnitStatus {
    ACTIVE(0),
    INACTIVE(1);

    private final int code;

    UnitStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static UnitStatus fromCode(int code) {
        for (UnitStatus value : values()) {
            if (value.code == code) return value;
        }
        throw new IllegalArgumentException("Unknown UnitStatus code: " + code);
    }
}
