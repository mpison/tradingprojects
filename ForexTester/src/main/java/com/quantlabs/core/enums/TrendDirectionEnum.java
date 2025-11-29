package com.quantlabs.core.enums;

public enum TrendDirectionEnum {
	INVALID_UNINITIALIZED(-1),
	INVALID_TREND(0),
    VALID_UP_TREND(1),
    VALID_DOWN_TREND(2),
    VALID_SEMI_UP_TREND(3),
    VALID_SEMI_DOWN_TREND(4),
    HEDGE_UP_TREND(5),
    HEDGE_DOWN_TREND(6),
    INVALID_HEDGE(7);

    private final int value;

    TrendDirectionEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}