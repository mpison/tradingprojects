package com.quantlabs.stockApp.utils.timeseries;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enum representing supported timeframes with multiple API naming conventions
 */
public enum Timeframe {
    // Minute timeframes
    ONE_MINUTE(Duration.ofMinutes(1), 
               "1Min", "1m", "1", "1min", "minute"),
    FIVE_MINUTES(Duration.ofMinutes(5), 
                "5Min", "5m", "5", "5min"),
    FIFTEEN_MINUTES(Duration.ofMinutes(15), 
                   "15Min", "15m", "15", "15min"),
    THIRTY_MINUTES(Duration.ofMinutes(30), 
                  "30Min", "30m", "30", "30min"),
    
    // Hourly timeframes
    ONE_HOUR(Duration.ofHours(1), 
            "1Hour", "1h", "60m", "60", "1hour", "hourly"),
    FOUR_HOURS(Duration.ofHours(4), 
              "4Hour", "4h", "240m", "240", "4hour"),
    
    // Daily timeframes
    ONE_DAY(Duration.ofDays(1), 
           "1Day", "1d", "24h", "1440m", "D", "daily", "day"),
    /*THREE_DAYS(Duration.ofDays(3), 
              "3Day", "3d", "72h", "D3", "3day"),*/
    
    // Weekly timeframes
    ONE_WEEK(Duration.ofDays(7), 
            "1Week", "1w", "7d", "168h", "W", "weekly", "week"),
    
    // Monthly timeframes (approximate)
    ONE_MONTH(Duration.ofDays(30),  // Approximate 30 days
             "1Month", "1M", "30d", "monthly", "month"),
    
    // Quarterly timeframes (approximate)
    THREE_MONTHS(Duration.ofDays(90),  // Approximate 90 days
                "3Month", "3M", "90d", "quarter", "quarterly"),
    
    // Yearly timeframes (approximate)
    ONE_YEAR(Duration.ofDays(365),  // Approximate 365 days
            "1Year", "1Y", "365d", "yearly", "year");
    
    private final Duration duration;
    private final String[] aliases;
    private static final Map<String, Timeframe> ALIAS_MAP = new HashMap<>();
    private static final Map<Duration, Timeframe> DURATION_MAP = new HashMap<>();
    
    static {
        // Build the alias map for quick lookup
        for (Timeframe tf : values()) {
            // Add enum name
            ALIAS_MAP.put(tf.name().toLowerCase(), tf);
            
            // Add Alpaca name
            ALIAS_MAP.put(tf.getAlpacaName().toLowerCase(), tf);
            
            // Add Polygon name
            ALIAS_MAP.put(tf.getPolygonName().toLowerCase(), tf);
            
            // Add Yahoo name
            ALIAS_MAP.put(tf.getYahooName().toLowerCase(), tf);
            
            // Add TradingView name
            ALIAS_MAP.put(tf.getTradingViewName().toLowerCase(), tf);
            
            // Add all aliases
            for (String alias : tf.aliases) {
                ALIAS_MAP.put(alias, tf);
            }
            
            // Add to duration map
            DURATION_MAP.put(tf.duration, tf);
        }
    }
    
    Timeframe(Duration duration, String... aliases) {
        this.duration = duration;
        this.aliases = aliases;
    }
    
    public Duration getDuration() {
        return duration;
    }
    
    public String[] getAliases() {
        return aliases;
    }
    
    /**
     * Get the standard Alpaca API timeframe name
     */
    public String getAlpacaName() {
        // Alpaca uses format like "1Min", "5Min", "1Hour", "1Day", "1Week", "1Month"
        switch (this) {
            case ONE_MINUTE: return "1Min";
            case FIVE_MINUTES: return "5Min";
            case FIFTEEN_MINUTES: return "15Min";
            case THIRTY_MINUTES: return "30Min";
            case ONE_HOUR: return "1Hour";
            case FOUR_HOURS: return "4Hour";
            case ONE_DAY: return "1Day";
            //case THREE_DAYS: return "3Day";
            case ONE_WEEK: return "1Week";
            case ONE_MONTH: return "1Month";
            case THREE_MONTHS: return "3Month";
            case ONE_YEAR: return "1Year";
            default: return this.name();
        }
    }
    
    /**
     * Get the Polygon.io API timeframe name
     */
    public String getPolygonName() {
        // Polygon uses minutes, hours, days, weeks, months, years
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + "minute";  // e.g., "1minute", "5minute"
        } else if (minutes < 1440) {
            return (minutes / 60) + "hour";  // e.g., "1hour", "4hour"
        } else if (minutes < 10080) {
            return (minutes / 1440) + "day";  // e.g., "1day", "3day"
        } else if (minutes < 43200) {  // 30 days in minutes
            return (minutes / 10080) + "week";  // e.g., "1week"
        } else if (minutes < 525600) {  // 365 days in minutes
            return (minutes / 43200) + "month";  // e.g., "1month", "3month"
        } else {
            return (minutes / 525600) + "year";  // e.g., "1year"
        }
    }
    
    /**
     * Get the Yahoo Finance API timeframe name
     */
    public String getYahooName() {
        // Yahoo uses 1m, 5m, 15m, 30m, 1h, 1d, 1wk, 1mo
        switch (this) {
            case ONE_MINUTE: return "1m";
            case FIVE_MINUTES: return "5m";
            case FIFTEEN_MINUTES: return "15m";
            case THIRTY_MINUTES: return "30m";
            case ONE_HOUR: return "1h";
            case ONE_DAY: return "1d";
            case ONE_WEEK: return "1wk";
            case ONE_MONTH: return "1mo";
            case THREE_MONTHS: return "3mo";
            case ONE_YEAR: return "1y";
            default: return "1d";  // Default to daily for others
        }
    }
    
    /**
     * Get the TradingView timeframe name
     */
    public String getTradingViewName() {
        switch (this) {
            case ONE_MINUTE: return "1";
            case FIVE_MINUTES: return "5";
            case FIFTEEN_MINUTES: return "15";
            case THIRTY_MINUTES: return "30";
            case ONE_HOUR: return "60";
            case FOUR_HOURS: return "240";
            case ONE_DAY: return "D";
            case ONE_WEEK: return "W";
            case ONE_MONTH: return "M";
            case THREE_MONTHS: return "3M";
            case ONE_YEAR: return "Y";
            default: return "D";
        }
    }
    
    /**
     * Find timeframe from any supported alias (case-insensitive)
     */
    public static Timeframe fromAlias(String alias) {
        if (alias == null || alias.trim().isEmpty()) {
            throw new IllegalArgumentException("Alias cannot be null or empty");
        }
        
        String normalized = alias.toLowerCase().trim();
        Timeframe tf = ALIAS_MAP.get(normalized);
        
        if (tf == null) {
            throw new IllegalArgumentException("Unknown timeframe alias: " + alias);
        }
        
        return tf;
    }
    
    /**
     * Find timeframe from Alpaca API name
     */
    public static Timeframe fromAlpaca(String alpacaName) {
        return fromAlias(alpacaName);
    }
    
    /**
     * Find timeframe from Polygon.io API name
     */
    public static Timeframe fromPolygon(String polygonName) {
        // Convert polygon format to our internal format
        String normalized = polygonName.toLowerCase().trim();
        
        // Handle common polygon formats
        if (normalized.endsWith("minute")) {
            // Convert "1minute" to "1m"
            String minutes = normalized.replace("minute", "");
            return fromAlias(minutes + "m");
        } else if (normalized.endsWith("hour")) {
            // Convert "1hour" to "1h"
            String hours = normalized.replace("hour", "");
            return fromAlias(hours + "h");
        } else if (normalized.endsWith("day")) {
            // Convert "1day" to "1d"
            String days = normalized.replace("day", "");
            return fromAlias(days + "d");
        } else if (normalized.endsWith("week")) {
            // Convert "1week" to "1w"
            String weeks = normalized.replace("week", "");
            return fromAlias(weeks + "w");
        } else if (normalized.endsWith("month")) {
            // Convert "1month" to "1M"
            String months = normalized.replace("month", "");
            return fromAlias(months + "M");
        } else if (normalized.endsWith("year")) {
            // Convert "1year" to "1Y"
            String years = normalized.replace("year", "");
            return fromAlias(years + "Y");
        }
        
        // Try direct lookup
        return fromAlias(normalized);
    }
    
    /**
     * Find timeframe from Yahoo Finance API name
     */
    public static Timeframe fromYahoo(String yahooName) {
        return fromAlias(yahooName);
    }
    
    /**
     * Find timeframe from TradingView name
     */
    public static Timeframe fromTradingView(String tvName) {
        // TradingView uses numbers for minutes/hours, letters for days/weeks
        String normalized = tvName.toUpperCase().trim();
        
        // Handle TradingView specific formats
        switch (normalized) {
            case "1": return ONE_MINUTE;
            case "5": return FIVE_MINUTES;
            case "15": return FIFTEEN_MINUTES;
            case "30": return THIRTY_MINUTES;
            case "60": return ONE_HOUR;
            case "240": return FOUR_HOURS;
            case "D": return ONE_DAY;
            case "W": return ONE_WEEK;
            case "M": return ONE_MONTH;
            case "3M": return THREE_MONTHS;
            case "Y": return ONE_YEAR;
            default: return fromAlias(normalized);
        }
    }
    
    /**
     * Find timeframe from Duration
     */
    public static Timeframe fromDuration(Duration duration) {
        Timeframe tf = DURATION_MAP.get(duration);
        if (tf == null) {
            // Try to find the closest match
            for (Timeframe timeframe : values()) {
                if (timeframe.duration.equals(duration)) {
                    return timeframe;
                }
            }
            throw new IllegalArgumentException("No matching timeframe for duration: " + duration);
        }
        return tf;
    }
    
    /**
     * Get the most common display name for this timeframe
     */
    public String getDisplayName() {
        switch (this) {
            case ONE_MINUTE: return "1 Minute";
            case FIVE_MINUTES: return "5 Minutes";
            case FIFTEEN_MINUTES: return "15 Minutes";
            case THIRTY_MINUTES: return "30 Minutes";
            case ONE_HOUR: return "1 Hour";
            case FOUR_HOURS: return "4 Hours";
            case ONE_DAY: return "1 Day";
            //case THREE_DAYS: return "3 Days";
            case ONE_WEEK: return "1 Week";
            case ONE_MONTH: return "1 Month";
            case THREE_MONTHS: return "3 Months";
            case ONE_YEAR: return "1 Year";
            default: return this.name();
        }
    }
    
    /**
     * Check if this timeframe is intraday (less than 1 day)
     */
    public boolean isIntraday() {
        return duration.toMinutes() < 1440;  // Less than 24 hours
    }
    
    /**
     * Check if this timeframe is daily or larger
     */
    public boolean isDailyOrLarger() {
        return duration.toMinutes() >= 1440;  // 24 hours or more
    }
    
    /**
     * Check if this timeframe can be aggregated from another timeframe
     * Returns true if sourceTimeframe is SMALLER than this timeframe AND
     * this timeframe's duration is divisible by sourceTimeframe's duration
     */
    public boolean canBeAggregatedFrom(Timeframe sourceTimeframe) {
        // Cannot aggregate from same or larger timeframe
        // We want to aggregate UP from smaller to larger
        if (this.duration.toMinutes() <= sourceTimeframe.duration.toMinutes()) {
            return false;  // Can't aggregate to same or smaller timeframe
        }
        
        // For approximate durations (month, quarter, year), we can't do exact aggregation
        // Check if both are standard durations (divisible by minutes)
        boolean sourceIsStandard = isStandardDuration(sourceTimeframe.duration);
        boolean targetIsStandard = isStandardDuration(this.duration);
        
        if (!sourceIsStandard || !targetIsStandard) {
            return false;  // Can't do exact aggregation with approximate durations
        }
        
        // Check if duration is divisible (e.g., 4H (240m) is divisible by 1H (60m))
        return this.duration.toMinutes() % sourceTimeframe.duration.toMinutes() == 0;
    }
    
    /**
     * Check if duration is standard (divisible by minutes without remainder)
     */
    private static boolean isStandardDuration(Duration duration) {
        // Check if duration is in whole minutes (no seconds/nanoseconds)
        return duration.getSeconds() % 60 == 0 && duration.getNano() == 0;
    }
    
    /**
     * Calculate how many source timeframe bars make up one bar of this timeframe
     */
    public int getAggregationFactor(Timeframe sourceTimeframe) {
        if (!canBeAggregatedFrom(sourceTimeframe)) {
            throw new IllegalArgumentException(
                String.format("Cannot aggregate %s from %s - either source is not smaller or durations not divisible", 
                this, sourceTimeframe));
        }
        return (int) (this.duration.toMinutes() / sourceTimeframe.duration.toMinutes());
    }
    
    /**
     * Get all available timeframes
     */
    public static Timeframe[] getAllTimeframes() {
        return values();
    }
    
    /**
     * Get all intraday timeframes
     */
    public static Timeframe[] getIntradayTimeframes() {
        return Arrays.stream(values())
            .filter(Timeframe::isIntraday)
            .toArray(Timeframe[]::new);
    }
    
    /**
     * Get all daily and larger timeframes
     */
    public static Timeframe[] getDailyAndLargerTimeframes() {
        return Arrays.stream(values())
            .filter(Timeframe::isDailyOrLarger)
            .toArray(Timeframe[]::new);
    }
    
    /**
     * Get timeframes that can be aggregated from a given source timeframe
     */
    public static List<Timeframe> getAggregatableTimeframes(Timeframe sourceTimeframe) {
        return Arrays.stream(values())
            .filter(tf -> tf.canBeAggregatedFrom(sourceTimeframe))
            .sorted(Comparator.comparing(tf -> tf.duration))
            .collect(Collectors.toList());
    }
}