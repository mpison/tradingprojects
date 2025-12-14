package com.quantlabs.stockApp.utils.timeseries;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.ta4j.core.*;
import org.ta4j.core.num.DoubleNum;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BarAggregatorTest {
    
    private BarSeries createMock1MinuteSeries() {
        BarSeries series = new BaseBarSeries("1min");
        ZonedDateTime startTime = ZonedDateTime.of(2024, 1, 1, 9, 30, 0, 0, ZoneId.systemDefault());
        
        // Create 10 minutes of data using addBar
        for (int i = 0; i < 10; i++) {
            ZonedDateTime endTime = startTime.plusMinutes(i + 1);
            double open = 100 + i;
            double high = 102 + i;
            double low = 99 + i;
            double close = 101 + i;
            double volume = 1000 + i * 100;
            
            series.addBar(endTime, open, high, low, close, volume);
        }
        
        return series;
    }
    
    private BarSeries createMock4HourSeries() {
        BarSeries series = new BaseBarSeries("4H");
        ZonedDateTime startTime = ZonedDateTime.of(2024, 1, 1, 9, 30, 0, 0, ZoneId.systemDefault());
        
        // Create 2 four-hour bars using addBar
        for (int i = 0; i < 2; i++) {
            ZonedDateTime endTime = startTime.plusHours(4 * (i + 1));
            double open = 100;
            double high = 110;
            double low = 95;
            double close = 105;
            double volume = 10000;
            
            series.addBar(endTime, open, high, low, close, volume);
        }
        
        return series;
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
        
        // Last bar should be different (since we're aggregating from 1-minute data)
        Bar originalLastBar = fourHourSeries.getLastBar();
        Bar resultLastBar = result.getLastBar();
        assertNotEquals(originalLastBar.getClosePrice(), resultLastBar.getClosePrice());
        
        // First bar should remain the same
        assertEquals(
            fourHourSeries.getBar(0).getClosePrice().doubleValue(),
            result.getBar(0).getClosePrice().doubleValue(),
            0.001
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
    @DisplayName("Test updateLastBarInRealTime - same period")
    void testUpdateLastBarInRealTimeSamePeriod() {
        // Arrange
        BarSeries oneMinuteSeries = createMock1MinuteSeries();
        BarSeries fourHourSeries = createMock4HourSeries();
        
        // Make sure we're in the same period
        Bar last4HBar = fourHourSeries.getLastBar();
        ZonedDateTime last4HEnd = last4HBar.getEndTime();
        ZonedDateTime last4HStart = last4HEnd.minusHours(4);
        
        // Create additional 1-minute bar within the same period
        ZonedDateTime withinPeriod = last4HStart.plusMinutes(30);
        
        // Add the bar to series
        oneMinuteSeries.addBar(withinPeriod, 110, 112, 108, 111, 2000);
        
        // Act
        BarSeries result = BarAggregator.updateLastBarInRealTime(
            oneMinuteSeries,
            fourHourSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        
        // Assert
        assertNotNull(result);
        assertEquals(fourHourSeries.getBarCount(), result.getBarCount());
    }
    
    @Test
    @DisplayName("Test buildCompleteSeries from 1-minute to 4-hour")
    void testBuildCompleteSeries() {
        // Arrange - need enough data for at least one 4H bar (240 minutes)
        BarSeries oneMinuteSeries = new BaseBarSeries("1min");
        ZonedDateTime startTime = ZonedDateTime.of(2024, 1, 1, 9, 30, 0, 0, ZoneId.systemDefault());
        
        // Create 240 minutes of data (exactly one 4H bar)
        for (int i = 0; i < 240; i++) {
            ZonedDateTime endTime = startTime.plusMinutes(i + 1);
            double price = 100 + (i * 0.01);
            oneMinuteSeries.addBar(endTime, price, price + 1, price - 1, price + 0.5, 1000);
        }
        
        // Act
        BarSeries result = BarAggregator.buildCompleteSeries(
            oneMinuteSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getBarCount());
        assertEquals("4 Hours", result.getName());
        
        // Verify aggregated values
        Bar aggregatedBar = result.getLastBar();
        assertEquals(100.0, aggregatedBar.getOpenPrice().doubleValue(), 0.001);
        assertEquals(101.98, aggregatedBar.getClosePrice().doubleValue(), 0.001); // 100 + 239*0.01 + 0.5
        assertTrue(aggregatedBar.getVolume().doubleValue() > 0);
    }
    
    @Test
    @DisplayName("Test buildCompleteSeries with exact data for one period")
    void testBuildCompleteSeriesExactPeriod() {
        // Arrange
        BarSeries oneMinuteSeries = new BaseBarSeries("exact");
        ZonedDateTime startTime = ZonedDateTime.now();
        
        // Create exactly 240 minutes (4 hours) of 1-minute data
        for (int i = 0; i < 240; i++) {
            ZonedDateTime endTime = startTime.plusMinutes(i + 1);
            double open = 100 + (i * 0.01);
            double close = 100 + (i * 0.01);
            double high = 101 + (i * 0.01);
            double low = 99 + (i * 0.01);
            double volume = 1000 + i;
            
            oneMinuteSeries.addBar(endTime, open, high, low, close, volume);
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
        assertEquals(100.0, aggregatedBar.getOpenPrice().doubleValue(), 0.001);
        assertEquals(102.39, aggregatedBar.getClosePrice().doubleValue(), 0.001); // Last bar's close
        assertEquals(268680.0, aggregatedBar.getVolume().doubleValue(), 0.001); // Sum of volumes
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
    @DisplayName("Test aggregation factor calculations")
    void testAggregationFactor() {
        // 4H = 240 minutes, 1m = 1 minute
        assertEquals(240, Timeframe.FOUR_HOURS.getAggregationFactor(Timeframe.ONE_MINUTE));
        
        // 1H = 60 minutes, 1m = 1 minute
        assertEquals(60, Timeframe.ONE_HOUR.getAggregationFactor(Timeframe.ONE_MINUTE));
        
        // 1D = 1440 minutes, 1H = 60 minutes
        assertEquals(24, Timeframe.ONE_DAY.getAggregationFactor(Timeframe.ONE_HOUR));
    }
    
    @Test
    @DisplayName("Test timeframe canBeAggregatedFrom")
    void testCanBeAggregatedFrom() {
        assertTrue(Timeframe.FOUR_HOURS.canBeAggregatedFrom(Timeframe.ONE_MINUTE));
        assertTrue(Timeframe.FOUR_HOURS.canBeAggregatedFrom(Timeframe.FIVE_MINUTES));
        assertTrue(Timeframe.ONE_HOUR.canBeAggregatedFrom(Timeframe.ONE_MINUTE));
        
        assertFalse(Timeframe.ONE_MINUTE.canBeAggregatedFrom(Timeframe.FOUR_HOURS)); // Can't go backwards
        assertFalse(Timeframe.ONE_DAY.canBeAggregatedFrom(Timeframe.THIRTY_MINUTES)); // Not divisible
    }
}