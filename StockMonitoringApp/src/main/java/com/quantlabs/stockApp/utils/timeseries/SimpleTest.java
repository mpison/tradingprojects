package com.quantlabs.stockApp.utils.timeseries;

import org.ta4j.core.*;
import java.time.ZonedDateTime;
import java.time.ZoneId;

public class SimpleTest {
    
    public static void main(String[] args) {
        System.out.println("=== Simple Bar Aggregation Test ===");
        
        // Create a simple 1-minute series with exactly 4 hours of data (240 bars)
        BarSeries oneMinuteSeries = new BaseBarSeries("1min");
        ZonedDateTime startTime = ZonedDateTime.of(2024, 1, 1, 9, 30, 0, 0, ZoneId.systemDefault());
        
        // Create 240 minutes of data (exactly 4 hours)
        for (int i = 0; i < 240; i++) {
            ZonedDateTime endTime = startTime.plusMinutes(i + 1);
            double open = 100.0 + (i * 0.01);
            double close = 100.5 + (i * 0.01);
            double high = close + 0.5;
            double low = open - 0.5;
            double volume = 1000 + (i * 10);
            
            oneMinuteSeries.addBar(endTime, open, high, low, close, volume);
        }
        
        System.out.println("Created " + oneMinuteSeries.getBarCount() + " 1-minute bars");
        System.out.println("Time range: " + oneMinuteSeries.getFirstBar().getEndTime() + 
                         " to " + oneMinuteSeries.getLastBar().getEndTime());
        
        // Aggregate to 4-hour bars
        BarSeries fourHourSeries = BarAggregator.buildCompleteSeries(
            oneMinuteSeries,
            Timeframe.ONE_MINUTE,
            Timeframe.FOUR_HOURS
        );
        
        System.out.println("\nAggregated to " + fourHourSeries.getBarCount() + " 4-hour bars");
        
        if (!fourHourSeries.isEmpty()) {
            Bar first4HBar = fourHourSeries.getFirstBar();
            System.out.println("\nFirst 4-hour bar:");
            System.out.println("Time: " + first4HBar.getEndTime());
            System.out.println("Open: " + first4HBar.getOpenPrice());
            System.out.println("High: " + first4HBar.getHighPrice());
            System.out.println("Low: " + first4HBar.getLowPrice());
            System.out.println("Close: " + first4HBar.getClosePrice());
            System.out.println("Volume: " + first4HBar.getVolume());
            
            // Verify aggregation
            Bar first1MinBar = oneMinuteSeries.getFirstBar();
            Bar last1MinBarInPeriod = oneMinuteSeries.getBar(239); // 240th bar (0-indexed)
            System.out.println("\nVerification:");
            System.out.println("First 1-min open: " + first1MinBar.getOpenPrice() + 
                             " = 4H open: " + first4HBar.getOpenPrice());
            System.out.println("Last 1-min close in period: " + last1MinBarInPeriod.getClosePrice() + 
                             " = 4H close: " + first4HBar.getClosePrice());
        }
    }
}