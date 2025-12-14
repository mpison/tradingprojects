package com.quantlabs.stockApp.utils.timeseries;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

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
            ZonedDateTime endTime = startTime.plusMinutes(i + 1);
            oneMinuteSeries.addBar(
                endTime,
                basePrice,
                basePrice + 0.5,
                basePrice - 0.5,
                basePrice + 0.1,
                1000 + i
            );
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
            // Daily volume should be greater than or equal to sum of 4H volumes
            assertTrue(dailyVolume >= total4HVolume * 0.95); // Allow small rounding differences
        }
    }
    
    @Test
    @DisplayName("Test multiple API naming conventions integration")
    void testMultipleApiIntegration() {
        // Test that we can use different API names interchangeably
        Timeframe tf1 = Timeframe.fromAlpaca("1Min");
        Timeframe tf2 = Timeframe.fromPolygon("1minute");
        Timeframe tf3 = Timeframe.fromYahoo("1m");
        
        assertEquals(Timeframe.ONE_MINUTE, tf1);
        assertEquals(Timeframe.ONE_MINUTE, tf2);
        assertEquals(Timeframe.ONE_MINUTE, tf3);
        
        // Verify they all have the same duration
        assertEquals(Duration.ofMinutes(1), tf1.getDuration());
        assertEquals(Duration.ofMinutes(1), tf2.getDuration());
        assertEquals(Duration.ofMinutes(1), tf3.getDuration());
    }
    
    @Test
    @DisplayName("Test real-time update scenario")
    void testRealTimeUpdate() {
        // Create initial data
        BarSeries oneMinuteSeries = new BaseBarSeries("1min");
        ZonedDateTime startTime = ZonedDateTime.of(2024, 1, 1, 9, 30, 0, 0, ZoneId.systemDefault());
        
        // Create 240 minutes of data (one complete 4H period)
        for (int i = 0; i < 240; i++) {
            ZonedDateTime endTime = startTime.plusMinutes(i + 1);
            double price = 100 + (i * 0.01);
            oneMinuteSeries.addBar(endTime, price, price + 0.5, price - 0.5, price + 0.25, 1000);
        }
        
        // Build initial 4H series
        BarSeries fourHourSeries = BarAggregator.buildCompleteSeries(
            oneMinuteSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        
        assertEquals(1, fourHourSeries.getBarCount());
        double initialClose = fourHourSeries.getLastBar().getClosePrice().doubleValue();
        
        // Add more 1-minute data in the same 4H period
        for (int i = 240; i < 245; i++) {
            ZonedDateTime endTime = startTime.plusMinutes(i + 1);
            double price = 100 + (i * 0.01);
            oneMinuteSeries.addBar(endTime, price, price + 0.5, price - 0.5, price + 0.25, 1000);
        }
        
        // Update the 4H bar in real-time
        BarSeries updatedSeries = BarAggregator.updateLastBarInRealTime(
            oneMinuteSeries,
            fourHourSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        
        assertEquals(1, updatedSeries.getBarCount());
        double updatedClose = updatedSeries.getLastBar().getClosePrice().doubleValue();
        
        // The close should be updated
        assertNotEquals(initialClose, updatedClose);
        assertTrue(updatedClose > initialClose); // Price should have increased
    }
}