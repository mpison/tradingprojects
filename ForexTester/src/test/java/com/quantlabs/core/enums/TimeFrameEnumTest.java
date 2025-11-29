package com.quantlabs.core.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

public class TimeFrameEnumTest {

    @Test
    public void testFromValueValid() {
        assertEquals(TimeFrameEnum.PERIOD_M1, TimeFrameEnum.fromValue(1));
        assertEquals(TimeFrameEnum.PERIOD_H4, TimeFrameEnum.fromValue(240));
        assertEquals(TimeFrameEnum.PERIOD_MN1, TimeFrameEnum.fromValue(43200));
    }

    @Test
    public void testFromValueInvalid() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> TimeFrameEnum.fromValue(999));
        assertTrue(ex.getMessage().contains("Invalid TimeFrameEnum value"));
    }

    @Test
    public void testFromCodeValid() {
        assertEquals(TimeFrameEnum.PERIOD_M15, TimeFrameEnum.fromCode("M15"));
        assertEquals(TimeFrameEnum.PERIOD_H1, TimeFrameEnum.fromCode("h1")); // case-insensitive
        assertEquals(TimeFrameEnum.PERIOD_W1, TimeFrameEnum.fromCode("W1"));
    }

    @Test
    public void testFromCodeInvalid() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> TimeFrameEnum.fromCode("Z9"));
        assertTrue(ex.getMessage().contains("Invalid TimeFrameEnum code"));
    }

    @Test
    public void testToDuration() {
        assertEquals(Duration.ofMinutes(1), TimeFrameEnum.PERIOD_M1.toDuration());
        assertEquals(Duration.ofMinutes(240), TimeFrameEnum.PERIOD_H4.toDuration());
        assertEquals(Duration.ofMinutes(43200), TimeFrameEnum.PERIOD_MN1.toDuration());
    }

    @Test
    public void testToStringIsCode() {
        assertEquals("M5", TimeFrameEnum.PERIOD_M5.toString());
        assertEquals("H4", TimeFrameEnum.PERIOD_H4.toString());
        assertEquals("MN1", TimeFrameEnum.PERIOD_MN1.toString());
    }

    @Test
    public void testCodeAndDescription() {
        assertEquals("1 hour", TimeFrameEnum.PERIOD_H1.getDescription());
        assertEquals("H1", TimeFrameEnum.PERIOD_H1.getCode());
    }
}