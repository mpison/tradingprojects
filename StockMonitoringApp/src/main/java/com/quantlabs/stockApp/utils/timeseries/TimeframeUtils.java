package com.quantlabs.stockApp.utils.timeseries;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;

/**
 * Utility class for timeframe calculations
 */
public class TimeframeUtils {
    
    /**
     * Calculates the start time of a period for a given timeframe
     */
    public static ZonedDateTime calculatePeriodStart(ZonedDateTime timestamp, Timeframe timeframe) {
        long minutes = timeframe.getDuration().toMinutes();
        
        if (minutes % 10080 == 0) { // Weekly multiples
            // Align to Monday 00:00
            return timestamp.withHour(0)
                           .withMinute(0)
                           .withSecond(0)
                           .withNano(0)
                           .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        } else if (minutes % 1440 == 0) { // Daily multiples
            // Align to day start
            return timestamp.withHour(0)
                           .withMinute(0)
                           .withSecond(0)
                           .withNano(0);
        } else if (minutes % 60 == 0) { // Hourly multiples
            int hour = timestamp.getHour();
            int hoursInPeriod = (int) (minutes / 60);
            int period = hour / hoursInPeriod;
            return timestamp.withHour(period * hoursInPeriod)
                           .withMinute(0)
                           .withSecond(0)
                           .withNano(0);
        } else { // Minute multiples
            int totalMinutes = timestamp.getHour() * 60 + timestamp.getMinute();
            int period = totalMinutes / (int) minutes;
            return timestamp.withHour((period * (int) minutes) / 60)
                           .withMinute((period * (int) minutes) % 60)
                           .withSecond(0)
                           .withNano(0);
        }
    }
    
    /**
     * Gets the end time of a period for a given timeframe
     */
    public static ZonedDateTime calculatePeriodEnd(ZonedDateTime timestamp, Timeframe timeframe) {
        return calculatePeriodStart(timestamp, timeframe).plus(timeframe.getDuration());
    }
    
    /**
     * Checks if a timestamp is at the boundary of a timeframe period
     */
    public static boolean isPeriodBoundary(ZonedDateTime timestamp, Timeframe timeframe) {
        ZonedDateTime periodStart = calculatePeriodStart(timestamp, timeframe);
        return timestamp.equals(periodStart);
    }
    
    /**
     * Gets the current period for a given timeframe
     */
    public static Period getCurrentPeriod(Timeframe timeframe) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime start = calculatePeriodStart(now, timeframe);
        ZonedDateTime end = start.plus(timeframe.getDuration());
        return new Period(start, end);
    }
    
    /**
     * Simple period class
     */
    public static class Period {
        private final ZonedDateTime start;
        private final ZonedDateTime end;
        
        public Period(ZonedDateTime start, ZonedDateTime end) {
            this.start = start;
            this.end = end;
        }
        
        public ZonedDateTime getStart() { return start; }
        public ZonedDateTime getEnd() { return end; }
    }
}