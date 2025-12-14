package com.quantlabs.stockApp.utils.timeseries;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.DoubleNum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class TimeframeTest {

    @Test
    void testFromAlpaca() {
        assertEquals(Timeframe.ONE_MINUTE, Timeframe.fromAlpaca("1Min"));
        assertEquals(Timeframe.FIVE_MINUTES, Timeframe.fromAlpaca("5Min"));
        assertEquals(Timeframe.ONE_HOUR, Timeframe.fromAlpaca("1Hour"));
        assertEquals(Timeframe.FOUR_HOURS, Timeframe.fromAlpaca("4Hour"));
        assertEquals(Timeframe.ONE_DAY, Timeframe.fromAlpaca("1Day"));
        assertEquals(Timeframe.ONE_WEEK, Timeframe.fromAlpaca("1Week"));
    }

    @Test
    void testFromAlpacaCaseInsensitive() {
        assertEquals(Timeframe.ONE_MINUTE, Timeframe.fromAlpaca("1min"));
        assertEquals(Timeframe.ONE_MINUTE, Timeframe.fromAlpaca("1MIN"));
    }

    @Test
    void testFromAlpacaInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Timeframe.fromAlpaca("invalid"));
    }

    @Test
    void testFromPolygon() {
        assertEquals(Timeframe.ONE_MINUTE, Timeframe.fromPolygon("1minute"));
        assertEquals(Timeframe.FIVE_MINUTES, Timeframe.fromPolygon("5minute"));
        assertEquals(Timeframe.ONE_HOUR, Timeframe.fromPolygon("1hour"));
        assertEquals(Timeframe.ONE_DAY, Timeframe.fromPolygon("1day"));
        assertEquals(Timeframe.ONE_WEEK, Timeframe.fromPolygon("1week"));
    }

    @Test
    void testFromYahoo() {
        assertEquals(Timeframe.ONE_MINUTE, Timeframe.fromYahoo("1m"));
        assertEquals(Timeframe.FIVE_MINUTES, Timeframe.fromYahoo("5m"));
        assertEquals(Timeframe.ONE_HOUR, Timeframe.fromYahoo("1h"));
        assertEquals(Timeframe.ONE_DAY, Timeframe.fromYahoo("1d"));
        assertEquals(Timeframe.ONE_WEEK, Timeframe.fromYahoo("1wk"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1min", "1Min", "1", "minute", "1MINUTE"})
    void testFromAliasOneMinute(String alias) {
        assertEquals(Timeframe.ONE_MINUTE, Timeframe.fromAlias(alias));
    }

    @Test
    void testGetAlpacaName() {
        assertEquals("1Min", Timeframe.ONE_MINUTE.getAlpacaName());
        assertEquals("5Min", Timeframe.FIVE_MINUTES.getAlpacaName());
        assertEquals("1Hour", Timeframe.ONE_HOUR.getAlpacaName());
        assertEquals("1Day", Timeframe.ONE_DAY.getAlpacaName());
        assertEquals("1Week", Timeframe.ONE_WEEK.getAlpacaName());
    }

    @Test
    void testGetPolygonName() {
        assertEquals("1minute", Timeframe.ONE_MINUTE.getPolygonName());
        assertEquals("5minute", Timeframe.FIVE_MINUTES.getPolygonName());
        assertEquals("1hour", Timeframe.ONE_HOUR.getPolygonName());
        assertEquals("1day", Timeframe.ONE_DAY.getPolygonName());
    }

    @Test
    void testGetYahooName() {
        assertEquals("1m", Timeframe.ONE_MINUTE.getYahooName());
        assertEquals("5m", Timeframe.FIVE_MINUTES.getYahooName());
        assertEquals("1h", Timeframe.ONE_HOUR.getYahooName());
        assertEquals("1d", Timeframe.ONE_DAY.getYahooName());
        assertEquals("1wk", Timeframe.ONE_WEEK.getYahooName());
    }

    @Test
    void testGetDisplayName() {
        assertEquals("1 Minute", Timeframe.ONE_MINUTE.getDisplayName());
        assertEquals("5 Minutes", Timeframe.FIVE_MINUTES.getDisplayName());
        assertEquals("1 Hour", Timeframe.ONE_HOUR.getDisplayName());
        assertEquals("1 Day", Timeframe.ONE_DAY.getDisplayName());
        assertEquals("1 Week", Timeframe.ONE_WEEK.getDisplayName());
    }

    @Test
    void testGetDuration() {
        assertEquals(Duration.ofMinutes(1), Timeframe.ONE_MINUTE.getDuration());
        assertEquals(Duration.ofMinutes(5), Timeframe.FIVE_MINUTES.getDuration());
        assertEquals(Duration.ofHours(1), Timeframe.ONE_HOUR.getDuration());
        assertEquals(Duration.ofHours(4), Timeframe.FOUR_HOURS.getDuration());
        assertEquals(Duration.ofDays(1), Timeframe.ONE_DAY.getDuration());
        assertEquals(Duration.ofDays(7), Timeframe.ONE_WEEK.getDuration());
    }

    @Test
    void testIsIntraday() {
        assertTrue(Timeframe.ONE_MINUTE.isIntraday());
        assertTrue(Timeframe.ONE_HOUR.isIntraday());
        assertTrue(Timeframe.FOUR_HOURS.isIntraday());
        assertFalse(Timeframe.ONE_DAY.isIntraday());
        assertFalse(Timeframe.ONE_WEEK.isIntraday());
    }

    @Test
    void testIsDailyOrLarger() {
        assertFalse(Timeframe.ONE_MINUTE.isDailyOrLarger());
        assertFalse(Timeframe.ONE_HOUR.isDailyOrLarger());
        assertFalse(Timeframe.FOUR_HOURS.isDailyOrLarger());
        assertTrue(Timeframe.ONE_DAY.isDailyOrLarger());
        assertTrue(Timeframe.ONE_WEEK.isDailyOrLarger());
    }

    @Test
    void testCanBeAggregatedFrom() {
        assertTrue(Timeframe.FOUR_HOURS.canBeAggregatedFrom(Timeframe.ONE_MINUTE));
        assertTrue(Timeframe.FOUR_HOURS.canBeAggregatedFrom(Timeframe.FIVE_MINUTES));
        assertTrue(Timeframe.FOUR_HOURS.canBeAggregatedFrom(Timeframe.ONE_HOUR));
        
        assertFalse(Timeframe.ONE_DAY.canBeAggregatedFrom(Timeframe.THIRTY_MINUTES));
        assertFalse(Timeframe.ONE_MINUTE.canBeAggregatedFrom(Timeframe.ONE_MINUTE));
    }

    @Test
    void testGetAggregationFactor() {
        assertEquals(240, Timeframe.FOUR_HOURS.getAggregationFactor(Timeframe.ONE_MINUTE));
        assertEquals(48, Timeframe.FOUR_HOURS.getAggregationFactor(Timeframe.FIVE_MINUTES));
        assertEquals(4, Timeframe.FOUR_HOURS.getAggregationFactor(Timeframe.ONE_HOUR));
        assertEquals(24, Timeframe.ONE_DAY.getAggregationFactor(Timeframe.ONE_HOUR));
    }

    @Test
    void testGetAggregationFactorInvalid() {
        assertThrows(IllegalArgumentException.class, 
            () -> Timeframe.ONE_DAY.getAggregationFactor(Timeframe.THIRTY_MINUTES));
    }

    @Test
    void testFromDuration() {
        assertEquals(Timeframe.ONE_MINUTE, Timeframe.fromDuration(Duration.ofMinutes(1)));
        assertEquals(Timeframe.FIVE_MINUTES, Timeframe.fromDuration(Duration.ofMinutes(5)));
        assertEquals(Timeframe.ONE_HOUR, Timeframe.fromDuration(Duration.ofHours(1)));
        assertEquals(Timeframe.ONE_DAY, Timeframe.fromDuration(Duration.ofDays(1)));
    }

    @Test
    void testFromDurationInvalid() {
        assertThrows(IllegalArgumentException.class, 
            () -> Timeframe.fromDuration(Duration.ofMinutes(13)));
    }
}
/*
class BarAggregatorTest {
    
    private BarSeries createMock1MinuteSeries() {
        List<Bar> bars = new ArrayList<>();
        ZonedDateTime startTime = ZonedDateTime.of(2024, 1, 1, 9, 30, 0, 0, ZoneId.systemDefault());
        
        // Create 10 minutes of data
        for (int i = 0; i < 10; i++) {
            ZonedDateTime endTime = startTime.plusMinutes(i + 1);
            BaseBar bar = new BaseBar(
                Duration.ofMinutes(1),
                endTime,
                DoubleNum.valueOf(100 + i),
                DoubleNum.valueOf(102 + i),
                DoubleNum.valueOf(99 + i),
                DoubleNum.valueOf(101 + i),
                DoubleNum.valueOf(1000 + i * 100),
                DoubleNum.valueOf(100000 + i * 10000),
                10 + i
            );
            bars.add(bar);
        }
        
        return new BaseBarSeries("1min", bars);
    }
    
    private BarSeries createMock4HourSeries() {
        List<Bar> bars = new ArrayList<>();
        ZonedDateTime startTime = ZonedDateTime.of(2024, 1, 1, 9, 30, 0, 0, ZoneId.systemDefault());
        
        // Create 2 four-hour bars
        for (int i = 0; i < 2; i++) {
            ZonedDateTime endTime = startTime.plusHours(4 * (i + 1));
            BaseBar bar = new BaseBar(
                Duration.ofHours(4),
                endTime,
                DoubleNum.valueOf(100),
                DoubleNum.valueOf(110),
                DoubleNum.valueOf(95),
                DoubleNum.valueOf(105),
                DoubleNum.valueOf(10000),
                DoubleNum.valueOf(1000000),
                100
            );
            bars.add(bar);
        }
        
        return new BaseBarSeries("4H", bars);
    }
    
    @Test
    @DisplayName("Test replaceLastBar with valid inputs")
    void testReplaceLastBar() {
        // Arrange
        BarSeries oneMinuteSeries = createMock1MinuteSeries();
        BarSeries fourHourSeries = createMock4HourSeries();
        
        int originalCount = fourHourSeries.getBarCount();
        
        // Act
        BarSeries result = BarAggregator.replaceLastBar(
            oneMinuteSeries,
            fourHourSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        
        // Assert
        assertNotNull(result);
        assertEquals(originalCount, result.getBarCount());
        assertNotSame(fourHourSeries, result); // Should return new instance
        
        // Last bar should be different
        Bar originalLastBar = fourHourSeries.getLastBar();
        Bar resultLastBar = result.getLastBar();
        assertNotEquals(originalLastBar.getClosePrice(), resultLastBar.getClosePrice());
        
        // First bar should remain the same
        assertEquals(
            fourHourSeries.getBar(0).getClosePrice(),
            result.getBar(0).getClosePrice()
        );
    }
    
    @Test
    @DisplayName("Test replaceLastBar with empty target series")
    void testReplaceLastBarEmptyTarget() {
        // Arrange
        BarSeries oneMinuteSeries = createMock1MinuteSeries();
        BarSeries emptySeries = new BaseBarSeries("empty");
        
        // Act
        BarSeries result = BarAggregator.replaceLastBar(
            oneMinuteSeries,
            emptySeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty()); // Should return empty series
    }
    
    @Test
    @DisplayName("Test replaceLastBar with insufficient source data")
    void testReplaceLastBarInsufficientData() {
        // Arrange
        BarSeries emptyMinuteSeries = new BaseBarSeries("empty");
        BarSeries fourHourSeries = createMock4HourSeries();
        
        // Act
        BarSeries result = BarAggregator.replaceLastBar(
            emptyMinuteSeries,
            fourHourSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        
        // Assert
        assertNotNull(result);
        assertEquals(fourHourSeries.getBarCount(), result.getBarCount());
        // Should return original series unchanged
    }
    
    @Test
    @DisplayName("Test replaceLastBar with incompatible timeframes")
    void testReplaceLastBarIncompatibleTimeframes() {
        // Arrange
        BarSeries oneMinuteSeries = createMock1MinuteSeries();
        BarSeries fourHourSeries = createMock4HourSeries();
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            BarAggregator.replaceLastBar(
                oneMinuteSeries,
                fourHourSeries,
                Timeframe.ONE_HOUR,  // Wrong source timeframe
                Timeframe.FOUR_HOURS
            )
        );
    }
    
    @Test
    @DisplayName("Test updateLastBarInRealTime - same period")
    void testUpdateLastBarInRealTimeSamePeriod() {
        // Arrange
        BarSeries oneMinuteSeries = createMock1MinuteSeries();
        BarSeries fourHourSeries = createMock4HourSeries();
        
        // Make sure last 1-minute bar is within last 4-hour period
        Bar last4HBar = fourHourSeries.getLastBar();
        ZonedDateTime last4HEnd = last4HBar.getEndTime();
        ZonedDateTime last4HStart = last4HEnd.minusHours(4);
        
        // Create a 1-minute bar within the same period
        ZonedDateTime withinPeriod = last4HStart.plusMinutes(30);
        BaseBar newMinuteBar = new BaseBar(
            Duration.ofMinutes(1),
            withinPeriod,
            DoubleNum.valueOf(110),
            DoubleNum.valueOf(112),
            DoubleNum.valueOf(108),
            DoubleNum.valueOf(111),
            DoubleNum.valueOf(2000),
            DoubleNum.valueOf(200000),
            20
        );
        
        // Add to series
        List<Bar> bars = new ArrayList<>();
        for (int i = 0; i < oneMinuteSeries.getBarCount(); i++) {
            bars.add(oneMinuteSeries.getBar(i));
        }
        bars.add(newMinuteBar);
        BarSeries updatedMinuteSeries = new BaseBarSeries("1min-updated", bars);
        
        // Act
        BarSeries result = BarAggregator.updateLastBarInRealTime(
            updatedMinuteSeries,
            fourHourSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        
        // Assert
        assertNotNull(result);
        assertEquals(fourHourSeries.getBarCount(), result.getBarCount());
    }
    
    @Test
    @DisplayName("Test updateLastBarInRealTime - new period")
    void testUpdateLastBarInRealTimeNewPeriod() {
        // Arrange
        BarSeries oneMinuteSeries = createMock1MinuteSeries();
        BarSeries fourHourSeries = createMock4HourSeries();
        
        // Get end time of last 4H bar
        Bar last4HBar = fourHourSeries.getLastBar();
        ZonedDateTime last4HEnd = last4HBar.getEndTime();
        
        // Create a 1-minute bar in the next 4-hour period
        ZonedDateTime nextPeriodStart = last4HEnd;
        ZonedDateTime withinNextPeriod = nextPeriodStart.plusMinutes(30);
        BaseBar newMinuteBar = new BaseBar(
            Duration.ofMinutes(1),
            withinNextPeriod,
            DoubleNum.valueOf(110),
            DoubleNum.valueOf(112),
            DoubleNum.valueOf(108),
            DoubleNum.valueOf(111),
            DoubleNum.valueOf(2000),
            DoubleNum.valueOf(200000),
            20
        );
        
        // Add to series
        List<Bar> bars = new ArrayList<>();
        for (int i = 0; i < oneMinuteSeries.getBarCount(); i++) {
            bars.add(oneMinuteSeries.getBar(i));
        }
        bars.add(newMinuteBar);
        BarSeries updatedMinuteSeries = new BaseBarSeries("1min-updated", bars);
        
        // Act
        BarSeries result = BarAggregator.updateLastBarInRealTime(
            updatedMinuteSeries,
            fourHourSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        
        // Assert
        assertNotNull(result);
        assertEquals(fourHourSeries.getBarCount() + 1, result.getBarCount());
    }
    
    @Test
    @DisplayName("Test buildCompleteSeries from 1-minute to 4-hour")
    void testBuildCompleteSeries() {
        // Arrange
        BarSeries oneMinuteSeries = createMock1MinuteSeries();
        
        // Act
        BarSeries result = BarAggregator.buildCompleteSeries(
            oneMinuteSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.getBarCount() > 0);
        assertEquals("4 Hours", result.getName());
    }
    
    @Test
    @DisplayName("Test buildCompleteSeries with insufficient data")
    void testBuildCompleteSeriesInsufficientData() {
        // Arrange
        BarSeries oneMinuteSeries = new BaseBarSeries("tiny");
        // Add only 2 minutes of data (need 240 for 4H)
        ZonedDateTime startTime = ZonedDateTime.now();
        for (int i = 0; i < 2; i++) {
            BaseBar bar = new BaseBar(
                Duration.ofMinutes(1),
                startTime.plusMinutes(i + 1),
                DoubleNum.valueOf(100),
                DoubleNum.valueOf(101),
                DoubleNum.valueOf(99),
                DoubleNum.valueOf(100),
                DoubleNum.valueOf(1000),
                DoubleNum.valueOf(10000),
                10
            );
            oneMinuteSeries.addBar(bar);
        }
        
        // Act
        BarSeries result = BarAggregator.buildCompleteSeries(
            oneMinuteSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty()); // Should be empty - not enough data for complete 4H bar
    }
    
    @Test
    @DisplayName("Test buildCompleteSeries with exact data for one period")
    void testBuildCompleteSeriesExactPeriod() {
        // Arrange
        BarSeries oneMinuteSeries = new BaseBarSeries("exact");
        ZonedDateTime startTime = ZonedDateTime.now();
        
        // Create exactly 240 minutes (4 hours) of 1-minute data
        for (int i = 0; i < 240; i++) {
            BaseBar bar = new BaseBar(
                Duration.ofMinutes(1),
                startTime.plusMinutes(i + 1),
                DoubleNum.valueOf(100 + (i * 0.01)),
                DoubleNum.valueOf(101 + (i * 0.01)),
                DoubleNum.valueOf(99 + (i * 0.01)),
                DoubleNum.valueOf(100 + (i * 0.01)),
                DoubleNum.valueOf(1000 + i),
                DoubleNum.valueOf(10000 + i * 10),
                10
            );
            oneMinuteSeries.addBar(bar);
        }
        
        // Act
        BarSeries result = BarAggregator.buildCompleteSeries(
            oneMinuteSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getBarCount()); // Should have exactly one 4H bar
        
        // Verify aggregated values
        Bar aggregatedBar = result.getLastBar();
        assertEquals(DoubleNum.valueOf(100.0), aggregatedBar.getOpenPrice()); // First bar's open
        assertEquals(DoubleNum.valueOf(102.39), aggregatedBar.getClosePrice()); // Last bar's close (100 + 239*0.01)
        assertTrue(aggregatedBar.getVolume().doubleValue() > 0);
    }
    
    @Test
    @DisplayName("Test validateInputs with null parameters")
    void testValidateInputs() {
        // Arrange
        BarSeries validSeries = createMock1MinuteSeries();
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            BarAggregator.replaceLastBar(null, validSeries, Timeframe.ONE_MINUTE, Timeframe.FOUR_HOURS)
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            BarAggregator.replaceLastBar(validSeries, null, Timeframe.ONE_MINUTE, Timeframe.FOUR_HOURS)
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            BarAggregator.replaceLastBar(validSeries, validSeries, null, Timeframe.FOUR_HOURS)
        );
        
        assertThrows(IllegalArgumentException.class, () ->
            BarAggregator.replaceLastBar(validSeries, validSeries, Timeframe.ONE_MINUTE, null)
        );
    }
    
    @Test
    @DisplayName("Test createSeriesWithReplacedLastBar")
    void testCreateSeriesWithReplacedLastBar() {
        // Arrange
        BarSeries original = createMock4HourSeries();
        int originalCount = original.getBarCount();
        
        ZonedDateTime newBarTime = original.getLastBar().getEndTime();
        BaseBar newBar = new BaseBar(
            Duration.ofHours(4),
            newBarTime,
            DoubleNum.valueOf(200),
            DoubleNum.valueOf(210),
            DoubleNum.valueOf(195),
            DoubleNum.valueOf(205),
            DoubleNum.valueOf(20000),
            DoubleNum.valueOf(2000000),
            200
        );
        
        // Act
        BarSeries result = BarAggregatorTestHelper.createSeriesWithReplacedLastBar(original, newBar);
        
        // Assert
        assertEquals(originalCount, result.getBarCount());
        assertEquals(newBar.getClosePrice(), result.getLastBar().getClosePrice());
        
        // All bars except last should be the same
        for (int i = 0; i < originalCount - 1; i++) {
            assertEquals(
                original.getBar(i).getClosePrice(),
                result.getBar(i).getClosePrice()
            );
        }
    }
    
    @Test
    @DisplayName("Test createSeriesWithNewBar")
    void testCreateSeriesWithNewBar() {
        // Arrange
        BarSeries original = createMock4HourSeries();
        int originalCount = original.getBarCount();
        
        ZonedDateTime newBarTime = original.getLastBar().getEndTime().plusHours(4);
        BaseBar newBar = new BaseBar(
            Duration.ofHours(4),
            newBarTime,
            DoubleNum.valueOf(200),
            DoubleNum.valueOf(210),
            DoubleNum.valueOf(195),
            DoubleNum.valueOf(205),
            DoubleNum.valueOf(20000),
            DoubleNum.valueOf(2000000),
            200
        );
        
        // Act
        BarSeries result = BarAggregatorTestHelper.createSeriesWithNewBar(original, newBar);
        
        // Assert
        assertEquals(originalCount + 1, result.getBarCount());
        assertEquals(newBar.getClosePrice(), result.getLastBar().getClosePrice());
        
        // All original bars should be the same
        for (int i = 0; i < originalCount; i++) {
            assertEquals(
                original.getBar(i).getClosePrice(),
                result.getBar(i).getClosePrice()
            );
        }
    }
}

// Test helper to access package-private methods
class BarAggregatorTestHelper {
    public static BarSeries createSeriesWithReplacedLastBar(BarSeries originalSeries, Bar newLastBar) {
        // Use reflection to access private method
        try {
            var method = BarAggregator.class.getDeclaredMethod(
                "createSeriesWithReplacedLastBar", 
                BarSeries.class, 
                Bar.class
            );
            method.setAccessible(true);
            return (BarSeries) method.invoke(null, originalSeries, newLastBar);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access private method", e);
        }
    }
    
    public static BarSeries createSeriesWithNewBar(BarSeries originalSeries, Bar newBar) {
        // Use reflection to access private method
        try {
            var method = BarAggregator.class.getDeclaredMethod(
                "createSeriesWithNewBar", 
                BarSeries.class, 
                Bar.class
            );
            method.setAccessible(true);
            return (BarSeries) method.invoke(null, originalSeries, newBar);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access private method", e);
        }
    }
}

class TimeframeUtilsTest {
    
    @Test
    @DisplayName("Test calculatePeriodStart for 1-minute timeframe")
    void testCalculatePeriodStartOneMinute() {
        // Arrange
        ZonedDateTime timestamp = ZonedDateTime.of(2024, 1, 1, 14, 23, 45, 0, ZoneId.systemDefault());
        
        // Act
        ZonedDateTime result = TimeframeUtils.calculatePeriodStart(timestamp, Timeframe.ONE_MINUTE);
        
        // Assert
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1, 14, 23, 0, 0, ZoneId.systemDefault());
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Test calculatePeriodStart for 5-minute timeframe")
    void testCalculatePeriodStartFiveMinutes() {
        // Arrange
        ZonedDateTime timestamp = ZonedDateTime.of(2024, 1, 1, 14, 23, 45, 0, ZoneId.systemDefault());
        
        // Act
        ZonedDateTime result = TimeframeUtils.calculatePeriodStart(timestamp, Timeframe.FIVE_MINUTES);
        
        // Assert
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1, 14, 20, 0, 0, ZoneId.systemDefault());
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Test calculatePeriodStart for 1-hour timeframe")
    void testCalculatePeriodStartOneHour() {
        // Arrange
        ZonedDateTime timestamp = ZonedDateTime.of(2024, 1, 1, 14, 23, 45, 0, ZoneId.systemDefault());
        
        // Act
        ZonedDateTime result = TimeframeUtils.calculatePeriodStart(timestamp, Timeframe.ONE_HOUR);
        
        // Assert
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1, 14, 0, 0, 0, ZoneId.systemDefault());
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Test calculatePeriodStart for 4-hour timeframe")
    void testCalculatePeriodStartFourHours() {
        // Arrange
        ZonedDateTime timestamp = ZonedDateTime.of(2024, 1, 1, 14, 23, 45, 0, ZoneId.systemDefault());
        
        // Act
        ZonedDateTime result = TimeframeUtils.calculatePeriodStart(timestamp, Timeframe.FOUR_HOURS);
        
        // Assert
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault());
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Test calculatePeriodStart for 1-day timeframe")
    void testCalculatePeriodStartOneDay() {
        // Arrange
        ZonedDateTime timestamp = ZonedDateTime.of(2024, 1, 1, 14, 23, 45, 0, ZoneId.systemDefault());
        
        // Act
        ZonedDateTime result = TimeframeUtils.calculatePeriodStart(timestamp, Timeframe.ONE_DAY);
        
        // Assert
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Test calculatePeriodStart for 1-week timeframe")
    void testCalculatePeriodStartOneWeek() {
        // Arrange - Thursday, Jan 4, 2024
        ZonedDateTime timestamp = ZonedDateTime.of(2024, 1, 4, 14, 23, 45, 0, ZoneId.systemDefault());
        
        // Act
        ZonedDateTime result = TimeframeUtils.calculatePeriodStart(timestamp, Timeframe.ONE_WEEK);
        
        // Assert - Should align to Monday, Jan 1, 2024
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Test calculatePeriodEnd")
    void testCalculatePeriodEnd() {
        // Arrange
        ZonedDateTime timestamp = ZonedDateTime.of(2024, 1, 1, 14, 23, 45, 0, ZoneId.systemDefault());
        
        // Act
        ZonedDateTime result = TimeframeUtils.calculatePeriodEnd(timestamp, Timeframe.ONE_HOUR);
        
        // Assert
        ZonedDateTime periodStart = TimeframeUtils.calculatePeriodStart(timestamp, Timeframe.ONE_HOUR);
        ZonedDateTime expected = periodStart.plusHours(1);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Test isPeriodBoundary")
    void testIsPeriodBoundary() {
        // Test at boundary
        ZonedDateTime boundaryTime = ZonedDateTime.of(2024, 1, 1, 9, 0, 0, 0, ZoneId.systemDefault());
        assertTrue(TimeframeUtils.isPeriodBoundary(boundaryTime, Timeframe.ONE_HOUR));
        
        // Test not at boundary
        ZonedDateTime nonBoundaryTime = ZonedDateTime.of(2024, 1, 1, 9, 30, 0, 0, ZoneId.systemDefault());
        assertFalse(TimeframeUtils.isPeriodBoundary(nonBoundaryTime, Timeframe.ONE_HOUR));
    }
    
    @Test
    @DisplayName("Test getCurrentPeriod")
    void testGetCurrentPeriod() {
        // Act
        TimeframeUtils.Period period = TimeframeUtils.getCurrentPeriod(Timeframe.ONE_HOUR);
        
        // Assert
        assertNotNull(period);
        assertNotNull(period.getStart());
        assertNotNull(period.getEnd());
        assertTrue(period.getEnd().isAfter(period.getStart()));
        assertEquals(Duration.ofHours(1), Duration.between(period.getStart(), period.getEnd()));
    }
}

// Integration tests
@Nested
@DisplayName("Integration Tests")
class IntegrationTests {
    
    @Test
    @DisplayName("Full integration test: 1m -> 4H -> 1D")
    void testFullIntegration() {
        // Create a full day of 1-minute data (390 minutes = 6.5 trading hours)
        BarSeries oneMinuteSeries = new BaseBarSeries("1min");
        ZonedDateTime startTime = ZonedDateTime.of(2024, 1, 1, 9, 30, 0, 0, ZoneId.systemDefault());
        
        for (int i = 0; i < 390; i++) {
            double basePrice = 100 + (i * 0.1);
            BaseBar bar = new BaseBar(
                Duration.ofMinutes(1),
                startTime.plusMinutes(i + 1),
                DoubleNum.valueOf(basePrice),
                DoubleNum.valueOf(basePrice + 0.5),
                DoubleNum.valueOf(basePrice - 0.5),
                DoubleNum.valueOf(basePrice + 0.1),
                DoubleNum.valueOf(1000 + i),
                DoubleNum.valueOf(10000 + i * 10),
                10
            );
            oneMinuteSeries.addBar(bar);
        }
        
        // Build 4H series from 1m data
        BarSeries fourHourSeries = BarAggregator.buildCompleteSeries(
            oneMinuteSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        
        assertFalse(fourHourSeries.isEmpty());
        System.out.println("4H bars created: " + fourHourSeries.getBarCount());
        
        // Build 1D series from 1m data
        BarSeries dailySeries = BarAggregator.buildCompleteSeries(
            oneMinuteSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.ONE_DAY
        );
        
        assertFalse(dailySeries.isEmpty());
        System.out.println("1D bars created: " + dailySeries.getBarCount());
        
        // Verify relationships
        assertEquals(Timeframe.FOUR_HOURS.getDisplayName(), fourHourSeries.getName());
        assertEquals(Timeframe.ONE_DAY.getDisplayName(), dailySeries.getName());
        
        // Verify 1D bar has higher volume than individual 4H bars
        if (!fourHourSeries.isEmpty() && !dailySeries.isEmpty()) {
            double total4HVolume = 0;
            for (int i = 0; i < fourHourSeries.getBarCount(); i++) {
                total4HVolume += fourHourSeries.getBar(i).getVolume().doubleValue();
            }
            
            double dailyVolume = dailySeries.getLastBar().getVolume().doubleValue();
            assertTrue(dailyVolume > 0);
        }
    }
    
    @Test
    @DisplayName("Test multiple API naming conventions integration")
    void testMultipleApiIntegration() {
        // Test that we can use different API names interchangeably
        Timeframe tf1 = Timeframe.fromAlpaca("1Min");
        Timeframe tf2 = Timeframe.fromPolygon("1minute");
        Timeframe tf3 = Timeframe.fromYahoo("1m");
        Timeframe tf4 = Timeframe.fromTradingView("1");
        
        assertEquals(Timeframe.ONE_MINUTE, tf1);
        assertEquals(Timeframe.ONE_MINUTE, tf2);
        assertEquals(Timeframe.ONE_MINUTE, tf3);
        assertEquals(Timeframe.ONE_MINUTE, tf4);
        
        // Verify they all have the same duration
        assertEquals(Duration.ofMinutes(1), tf1.getDuration());
        assertEquals(Duration.ofMinutes(1), tf2.getDuration());
        assertEquals(Duration.ofMinutes(1), tf3.getDuration());
        assertEquals(Duration.ofMinutes(1), tf4.getDuration());
    }
}*/