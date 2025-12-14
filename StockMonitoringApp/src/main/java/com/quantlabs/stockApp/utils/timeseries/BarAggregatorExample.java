package com.quantlabs.stockApp.utils.timeseries;
import org.ta4j.core.*;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.Random;

public class BarAggregatorExample {
    
    public static void main(String[] args) {
        System.out.println("=== Bar Aggregator Example (Using addBar) ===");
        
        // Step 1: Create and populate 1-minute bar series
        BarSeries oneMinuteSeries = createSample1MinuteData(250); // 250 minutes of data (more than 4 hours)
        System.out.println("Created 1-minute series with " + oneMinuteSeries.getBarCount() + " bars");
        System.out.println("Time range: " + oneMinuteSeries.getFirstBar().getEndTime() + 
                         " to " + oneMinuteSeries.getLastBar().getEndTime());
        
        // Step 2: Build complete 4H series from 1-minute data
        System.out.println("\nBuilding complete 4H series from 1-minute data...");
        BarSeries fourHourSeries = BarAggregator.buildCompleteSeries(
            oneMinuteSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        
        System.out.println("Built 4H series with " + fourHourSeries.getBarCount() + " bars");
        
        if (!fourHourSeries.isEmpty()) {
            System.out.println("First 4H bar time: " + fourHourSeries.getFirstBar().getEndTime());
            System.out.println("Last 4H bar time: " + fourHourSeries.getLastBar().getEndTime());
            System.out.println("Last 4H bar close: " + fourHourSeries.getLastBar().getClosePrice());
        }
        
        // Step 3: Add more 1-minute data (simulating real-time updates)
        System.out.println("\nAdding more 1-minute data for real-time update...");
        BarSeries updatedOneMinuteSeries = addMore1MinuteData(oneMinuteSeries, 30);
        System.out.println("Updated 1-minute series has " + updatedOneMinuteSeries.getBarCount() + " bars");
        
        // Step 4: Replace last 4H bar with updated data
        System.out.println("\nReplacing last 4H bar with current 1-minute data...");
        try {
            BarSeries updatedFourHourSeries = BarAggregator.replaceLastBar(
                updatedOneMinuteSeries,
                fourHourSeries,
                Timeframe.ONE_MINUTE,
                Timeframe.FOUR_HOURS
            );
            
            System.out.println("Success! Updated 4H series has " + updatedFourHourSeries.getBarCount() + " bars");
            
            // Compare last bars
            if (!fourHourSeries.isEmpty()) {
                Bar oldLastBar = fourHourSeries.getLastBar();
                Bar newLastBar = updatedFourHourSeries.getLastBar();
                
                System.out.println("\nLast Bar Comparison:");
                System.out.println("Time: " + oldLastBar.getEndTime());
                System.out.printf("Old Close: %.4f%n", oldLastBar.getClosePrice().doubleValue());
                System.out.printf("New Close: %.4f%n", newLastBar.getClosePrice().doubleValue());
                System.out.printf("Change: %.4f%n", 
                    newLastBar.getClosePrice().doubleValue() - oldLastBar.getClosePrice().doubleValue());
                System.out.printf("Old Volume: %.0f%n", oldLastBar.getVolume().doubleValue());
                System.out.printf("New Volume: %.0f%n", newLastBar.getVolume().doubleValue());
            }
            
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Step 5: Test real-time update
        System.out.println("\n=== Testing Real-time Update ===");
        BarSeries realTimeUpdated = BarAggregator.updateLastBarInRealTime(
            updatedOneMinuteSeries,
            fourHourSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        System.out.println("Real-time updated series has " + realTimeUpdated.getBarCount() + " bars");
        
        // Step 6: Test other timeframes
        System.out.println("\n=== Testing Other Timeframes ===");
        testOtherTimeframes(updatedOneMinuteSeries);
    }
    
    /**
     * Create sample 1-minute data using addBar method
     */
    private static BarSeries createSample1MinuteData(int numBars) {
        BarSeries series = new BaseBarSeries("1min");
        Random random = new Random(42);
        
        ZonedDateTime startTime = ZonedDateTime.of(2024, 1, 1, 9, 30, 0, 0, ZoneId.systemDefault());
        double basePrice = 100.0;
        
        for (int i = 0; i < numBars; i++) {
            ZonedDateTime endTime = startTime.plusMinutes(i + 1);
            
            // Generate price data
            double priceChange = (random.nextDouble() - 0.5) * 2.0;
            double open = basePrice + (i * 0.1) + priceChange;
            double close = open + (random.nextDouble() - 0.5) * 1.0;
            double high = Math.max(open, close) + random.nextDouble() * 0.5;
            double low = Math.min(open, close) - random.nextDouble() * 0.5;
            double volume = 1000 + random.nextDouble() * 9000;
            
            // Use addBar method
            series.addBar(endTime, open, high, low, close, volume);
            
            basePrice = close;
        }
        
        return series;
    }
    
    /**
     * Add more 1-minute data using addBar method
     */
    private static BarSeries addMore1MinuteData(BarSeries originalSeries, int additionalBars) {
        // Create new series
        BarSeries newSeries = new BaseBarSeries(originalSeries.getName());
        
        // Copy all original bars using addBar
        for (int i = 0; i < originalSeries.getBarCount(); i++) {
            Bar bar = originalSeries.getBar(i);
            newSeries.addBar(
                bar.getEndTime(),
                bar.getOpenPrice().doubleValue(),
                bar.getHighPrice().doubleValue(),
                bar.getLowPrice().doubleValue(),
                bar.getClosePrice().doubleValue(),
                bar.getVolume().doubleValue()
            );
        }
        
        Random random = new Random(123);
        Bar lastBar = originalSeries.getLastBar();
        ZonedDateTime lastTime = lastBar.getEndTime();
        double lastPrice = lastBar.getClosePrice().doubleValue();
        
        for (int i = 0; i < additionalBars; i++) {
            ZonedDateTime endTime = lastTime.plusMinutes(i + 1);
            
            double priceChange = (random.nextDouble() - 0.5) * 1.5;
            double open = lastPrice;
            double close = open + priceChange;
            double high = Math.max(open, close) + random.nextDouble() * 0.3;
            double low = Math.min(open, close) - random.nextDouble() * 0.3;
            double volume = 500 + random.nextDouble() * 1500;
            
            // Use addBar method
            newSeries.addBar(endTime, open, high, low, close, volume);
            
            lastPrice = close;
        }
        
        return newSeries;
    }
    
    /**
     * Test aggregation to other timeframes
     */
    private static void testOtherTimeframes(BarSeries oneMinuteSeries) {
        System.out.println("\n--- 1-Hour Aggregation ---");
        BarSeries oneHourSeries = BarAggregator.buildCompleteSeries(
            oneMinuteSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.ONE_HOUR
        );
        System.out.println("1-hour bars: " + oneHourSeries.getBarCount());
        
        System.out.println("\n--- 30-Minute Aggregation ---");
        BarSeries thirtyMinSeries = BarAggregator.buildCompleteSeries(
            oneMinuteSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.THIRTY_MINUTES
        );
        System.out.println("30-minute bars: " + thirtyMinSeries.getBarCount());
        
        System.out.println("\n--- 1-Day Aggregation ---");
        BarSeries oneDaySeries = BarAggregator.buildCompleteSeries(
            oneMinuteSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.ONE_DAY
        );
        System.out.println("1-day bars: " + oneDaySeries.getBarCount());
        
        // Test replacing last bar for 1-hour series
        if (!oneHourSeries.isEmpty()) {
            System.out.println("\n--- Replacing Last 1-Hour Bar ---");
            BarSeries updatedOneHour = BarAggregator.replaceLastBar(
                oneMinuteSeries,
                oneHourSeries,
                Timeframe.ONE_MINUTE,
                Timeframe.ONE_HOUR
            );
            System.out.println("Updated 1-hour bars: " + updatedOneHour.getBarCount());
        }
    }
}