package com.quantlabs.core.enums;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public enum TimeFrameEnum {

    PERIOD_CURRENT(0, "Current timeframe", "C"),

    // Minutes
    PERIOD_M1(1, "1 minute", "M1"),
    PERIOD_M2(2, "2 minutes", "M2"),
    PERIOD_M3(3, "3 minutes", "M3"),
    PERIOD_M4(4, "4 minutes", "M4"),
    PERIOD_M5(5, "5 minutes", "M5"),
    PERIOD_M6(6, "6 minutes", "M6"),
    PERIOD_M10(10, "10 minutes", "M10"),
    PERIOD_M12(12, "12 minutes", "M12"),
    PERIOD_M15(15, "15 minutes", "M15"),
    PERIOD_M20(20, "20 minutes", "M20"),
    PERIOD_M30(30, "30 minutes", "M30"),

    // Hours
    PERIOD_H1(60, "1 hour", "H1"),
    PERIOD_H2(120, "2 hours", "H2"),
    PERIOD_H3(180, "3 hours", "H3"),
    PERIOD_H4(240, "4 hours", "H4"),
    PERIOD_H6(360, "6 hours", "H6"),
    PERIOD_H8(480, "8 hours", "H8"),
    PERIOD_H12(720, "12 hours", "H12"),

    // Days and higher
    PERIOD_D1(1440, "1 day", "D1"),
    PERIOD_W1(10080, "1 week", "W1"),
    PERIOD_MN1(43200, "1 month", "MN1");

    private final int value;
    private final String description;
    private final String code;

    private static final Map<Integer, TimeFrameEnum> BY_VALUE = new HashMap<>();
    private static final Map<String, TimeFrameEnum> BY_CODE = new HashMap<>();

    static {
        for (TimeFrameEnum tf : values()) {
            BY_VALUE.put(tf.value, tf);
            BY_CODE.put(tf.code, tf);
        }
    }

    TimeFrameEnum(int value, String description, String code) {
        this.value = value;
        this.description = description;
        this.code = code;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }

    public static TimeFrameEnum fromValue(int value) {
        if (BY_VALUE.containsKey(value)) {
            return BY_VALUE.get(value);
        }
        throw new IllegalArgumentException("Invalid TimeFrameEnum value: " + value);
    }

    public static TimeFrameEnum fromCode(String code) {
        if (code == null) throw new IllegalArgumentException("Code cannot be null");
        TimeFrameEnum tf = BY_CODE.get(code.toUpperCase());
        if (tf == null) throw new IllegalArgumentException("Invalid TimeFrameEnum code: " + code);
        return tf;
    }

    public Duration toDuration() {
        return Duration.ofMinutes(value);
    }

    @Override
    public String toString() {
        return code;
    }
}