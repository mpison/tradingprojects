package com.quantlabs.stockApp.utils.timeseries;
import org.ta4j.core.*;
import org.ta4j.core.num.Num;
import org.ta4j.core.num.DoubleNum;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Bar aggregator for converting between different timeframes
 */
public class BarAggregator {
	
	
	public static BarSeries replaceOrAddLastBar(BarSeries sourceSeries,
            BarSeries targetSeries,
            Timeframe sourceTimeframe,
            Timeframe targetTimeframe, ZonedDateTime endTime) {
		
		validateInputs(sourceSeries, targetSeries, sourceTimeframe, targetTimeframe);
        
        // Check if target timeframe can be aggregated from source timeframe
        if (!targetTimeframe.canBeAggregatedFrom(sourceTimeframe)) {
            throw new IllegalArgumentException(
                String.format("Cannot aggregate %s from %s", 
                targetTimeframe, sourceTimeframe));
        }
        
        if (targetSeries.isEmpty()) {
            return buildCompleteSeries(sourceSeries, sourceTimeframe, targetTimeframe);
        }
        
        // Get the time period for the last bar in target series
        Bar lastTargetBar = targetSeries.getLastBar();
        ZonedDateTime lastBarStart = lastTargetBar.getEndTime();//.isBefore(endTime) ?  endTime : lastTargetBar.getEndTime();
        
        if(lastTargetBar.getEndTime().equals(endTime)){
        	return replaceLastBar(sourceSeries, targetSeries, sourceTimeframe, targetTimeframe);
        }
        
        return null;
	}
    
    /**
     * Replaces the last bar in higher timeframe series with aggregated bar from source data
     * @param endTime 
     */
    public static BarSeries replaceLastBar(BarSeries sourceSeries,
                                           BarSeries targetSeries,
                                           Timeframe sourceTimeframe,
                                           Timeframe targetTimeframe) {
        
        validateInputs(sourceSeries, targetSeries, sourceTimeframe, targetTimeframe);
        
        // Check if target timeframe can be aggregated from source timeframe
        if (!targetTimeframe.canBeAggregatedFrom(sourceTimeframe)) {
            throw new IllegalArgumentException(
                String.format("Cannot aggregate %s from %s", 
                targetTimeframe, sourceTimeframe));
        }
        
        if (targetSeries.isEmpty()) {
            return buildCompleteSeries(sourceSeries, sourceTimeframe, targetTimeframe);
        }
        
        // Get the time period for the last bar in target series
        Bar lastTargetBar = targetSeries.getLastBar();
        ZonedDateTime lastBarStart = lastTargetBar.getEndTime();//.isBefore(endTime) ?  endTime : lastTargetBar.getEndTime();
        
        // Aggregate source bars for the same time period
        AggregatedBarData aggregatedData = aggregateBarsForPeriod(
            sourceSeries, 
            lastBarStart, 
            targetTimeframe.getDuration(),
            sourceTimeframe
        );
        
        if (aggregatedData == null) {
            return targetSeries;
        }
        
        // Create new series with replaced last bar
        return createSeriesWithReplacedLastBar(targetSeries, aggregatedData, targetTimeframe.getDuration());
    }
    
    /**
     * Updates the last bar in real-time as new source bars come in
     */
    public static BarSeries updateLastBarInRealTime(BarSeries sourceSeries,
                                                    BarSeries targetSeries,
                                                    Timeframe sourceTimeframe,
                                                    Timeframe targetTimeframe) {
        
        validateInputs(sourceSeries, targetSeries, sourceTimeframe, targetTimeframe);
        
        if (targetSeries.isEmpty()) {
            return buildCompleteSeries(sourceSeries, sourceTimeframe, targetTimeframe);
        }
        
        // For real-time updates, determine current period
        ZonedDateTime currentTime = sourceSeries.getLastBar().getEndTime();
        ZonedDateTime currentPeriodStart = TimeframeUtils.calculatePeriodStart(currentTime, targetTimeframe);
        ZonedDateTime currentPeriodEnd = currentPeriodStart.plus(targetTimeframe.getDuration());
        
        // Check if we're still in the same period as the last bar
        Bar lastTargetBar = targetSeries.getLastBar();
        ZonedDateTime lastBarStart = lastTargetBar.getEndTime().minus(targetTimeframe.getDuration());
        
        if (currentPeriodStart.equals(lastBarStart)) {
            // Same period - replace the last bar
            AggregatedBarData aggregatedData = aggregateBarsForExactPeriod(
                sourceSeries, 
                currentPeriodStart, 
                currentPeriodEnd,
                sourceTimeframe
            );
            
            if (aggregatedData != null) {
                return createSeriesWithReplacedLastBar(targetSeries, aggregatedData, targetTimeframe.getDuration());
            }
        } else {
            // New period - add a new bar
            AggregatedBarData aggregatedData = aggregateBarsForExactPeriod(
                sourceSeries, 
                currentPeriodStart, 
                currentPeriodEnd,
                sourceTimeframe
            );
            
            if (aggregatedData != null) {
                return createSeriesWithNewBar(targetSeries, aggregatedData, targetTimeframe.getDuration());
            }
        }
        
        return targetSeries;
    }
    
    /**
     * Builds a complete target timeframe series from source data
     */
    public static BarSeries buildCompleteSeries(BarSeries sourceSeries,
                                                Timeframe sourceTimeframe,
                                                Timeframe targetTimeframe) {
        
        // Create new series
        BarSeries newSeries = new BaseBarSeries(targetTimeframe.getDisplayName());
        
        if (sourceSeries.isEmpty()) {
            return newSeries;
        }
        
        // Group source bars into target timeframe periods
        ZonedDateTime currentPeriodStart = null;
        List<Bar> currentPeriodBars = new ArrayList<>();
        
        for (int i = 0; i < sourceSeries.getBarCount(); i++) {
            Bar sourceBar = sourceSeries.getBar(i);
            ZonedDateTime barTime = sourceBar.getEndTime();
            ZonedDateTime periodStart = TimeframeUtils.calculatePeriodStart(barTime, targetTimeframe);
            
            if (currentPeriodStart == null || !periodStart.equals(currentPeriodStart)) {
                // New period - finalize previous period if any
                if (!currentPeriodBars.isEmpty()) {
                    AggregatedBarData aggregatedData = aggregateBarsData(currentPeriodBars);
                    if (aggregatedData != null) {
                        newSeries.addBar(
                            currentPeriodStart.plus(targetTimeframe.getDuration()),
                            aggregatedData.open,
                            aggregatedData.high,
                            aggregatedData.low,
                            aggregatedData.close,
                            aggregatedData.volume
                        );
                    }
                }
                
                // Start new period
                currentPeriodStart = periodStart;
                currentPeriodBars.clear();
            }
            
            currentPeriodBars.add(sourceBar);
        }
        
        // Add the last complete period if we have enough bars
        if (!currentPeriodBars.isEmpty()) {
            int expectedBars = targetTimeframe.getAggregationFactor(sourceTimeframe);
            if (currentPeriodBars.size() >= expectedBars) {
                AggregatedBarData aggregatedData = aggregateBarsData(currentPeriodBars);
                if (aggregatedData != null) {
                    newSeries.addBar(
                        currentPeriodStart.plus(targetTimeframe.getDuration()),
                        aggregatedData.open,
                        aggregatedData.high,
                        aggregatedData.low,
                        aggregatedData.close,
                        aggregatedData.volume
                    );
                }
            }
        }
        
        return newSeries;
    }
    
    /**
     * Aggregates bars for a specific period
     */
    private static AggregatedBarData aggregateBarsForPeriod(BarSeries sourceSeries,
                                                           ZonedDateTime periodStart,
                                                           Duration periodDuration,
                                                           Timeframe sourceTimeframe) {
        
        ZonedDateTime periodEnd = sourceSeries.getLastBar().getEndTime();// periodStart.plus(periodDuration);
        return aggregateBarsForExactPeriod(sourceSeries, periodStart, periodEnd, sourceTimeframe);
    }
    
    /**
     * Aggregates bars for an exact time period
     */
    private static AggregatedBarData aggregateBarsForExactPeriod(BarSeries sourceSeries,
                                                                ZonedDateTime periodStart,
                                                                ZonedDateTime periodEnd,
                                                                Timeframe sourceTimeframe) {
        
        List<Bar> barsInPeriod = new ArrayList<>();
        
        // Find all source bars in the specified period
        for (int i = 0; i < sourceSeries.getBarCount(); i++) {
            Bar sourceBar = sourceSeries.getBar(i);
            ZonedDateTime barTime = sourceBar.getEndTime();
            
            if (barTime.isBefore(periodStart)) {
                continue;
            }
            
            if (!barTime.isBefore(periodEnd)) {
                break;
            }
            
            barsInPeriod.add(sourceBar);
        }
        
        if (barsInPeriod.isEmpty()) {
            return null;
        }
        
        return aggregateBarsData(barsInPeriod);
    }
    
    /**
     * Aggregates multiple bars into aggregated data
     */
    private static AggregatedBarData aggregateBarsData(List<Bar> bars) {
        if (bars.isEmpty()) {
            return null;
        }
        
        Bar firstBar = bars.get(0);
        Bar lastBar = bars.get(bars.size() - 1);
        
        double open = firstBar.getOpenPrice().doubleValue();
        double close = lastBar.getClosePrice().doubleValue();
        
        // Calculate high and low
        double high = firstBar.getHighPrice().doubleValue();
        double low = firstBar.getLowPrice().doubleValue();
        
        for (Bar bar : bars) {
            double barHigh = bar.getHighPrice().doubleValue();
            double barLow = bar.getLowPrice().doubleValue();
            
            if (barHigh > high) {
                high = barHigh;
            }
            if (barLow < low) {
                low = barLow;
            }
        }
        
        // Calculate volume
        double volume = 0;
        for (Bar bar : bars) {
            volume += bar.getVolume().doubleValue();
        }
        
        return new AggregatedBarData(open, high, low, close, volume);
    }
    
    /**
     * Creates a new series with the last bar replaced
     */
    private static BarSeries createSeriesWithReplacedLastBar(BarSeries originalSeries, 
                                                            AggregatedBarData aggregatedData,
                                                            Duration timePeriod) {
        BarSeries newSeries = new BaseBarSeries(originalSeries.getName());
        
        // Copy all bars except the last one
        for (int i = 0; i < originalSeries.getBarCount() - 1; i++) {
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
        
        // Get the end time from the original last bar (same period)
        Bar originalLastBar = originalSeries.getLastBar();
        ZonedDateTime endTime = originalLastBar.getEndTime();
        
        // Add the new last bar
        newSeries.addBar(
            endTime,
            aggregatedData.open,
            aggregatedData.high,
            aggregatedData.low,
            aggregatedData.close,
            aggregatedData.volume
        );
        
        return newSeries;
    }
    
    /**
     * Creates a new series with an additional bar appended
     */
    private static BarSeries createSeriesWithNewBar(BarSeries originalSeries, 
                                                   AggregatedBarData aggregatedData,
                                                   Duration timePeriod) {
        BarSeries newSeries = new BaseBarSeries(originalSeries.getName());
        
        // Copy all existing bars
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
        
        // Calculate end time for new bar (one period after last bar)
        Bar lastBar = originalSeries.getLastBar();
        ZonedDateTime endTime = lastBar.getEndTime().plus(timePeriod);
        
        // Add the new bar
        newSeries.addBar(
            endTime,
            aggregatedData.open,
            aggregatedData.high,
            aggregatedData.low,
            aggregatedData.close,
            aggregatedData.volume
        );
        
        return newSeries;
    }
    
    private static void validateInputs(BarSeries sourceSeries,
                                       BarSeries targetSeries,
                                       Timeframe sourceTimeframe,
                                       Timeframe targetTimeframe) {
        if (sourceSeries == null || sourceSeries.isEmpty()) {
            throw new IllegalArgumentException("Source series cannot be null or empty");
        }
        if (targetSeries == null) {
            throw new IllegalArgumentException("Target series cannot be null");
        }
        if (sourceTimeframe == null || targetTimeframe == null) {
            throw new IllegalArgumentException("Timeframes cannot be null");
        }
    }
    
    /**
     * Helper class to store aggregated bar data
     */
    private static class AggregatedBarData {
        final double open;
        final double high;
        final double low;
        final double close;
        final double volume;
        
        AggregatedBarData(double open, double high, double low, double close, double volume) {
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }
    }
}