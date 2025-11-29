package com.quantlabs.core.enums;

public enum TrendClassEnum {
    BASE_TREND_CLASS(0),
    BOLLINGER_TREND_CLASS(1),
    CANDLE_TREND_CLASS(2),
    CANDLE_TREND_CLASSV2(22),
    HEIKEN_ASHI_TREND_CLASS(3),
    MACD_TREND_CLASS(4),
    MACDPSAR222_TREND_CLASS(422),
    PSAR_TREND_CLASS(5),
    PSARFAST_TREND_CLASS(6),
    PSAR2_TREND_CLASS(7),
    PSAR21_TREND_CLASS(71),
    STOCH_TREND_CLASS(8);

    private final int value;

    TrendClassEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static TrendClassEnum fromValue(int value) {
        for (TrendClassEnum t : values()) {
            if (t.value == value) {
                return t;
            }
        }
        throw new IllegalArgumentException("Invalid TrendClassNum value: " + value);
    }
}
