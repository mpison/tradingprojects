package com.quantlabs.stockApp.utils.pricedata;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Bar;
import com.quantlabs.stockApp.model.PriceData;

public class PriceDataCalculator {

    // Market session times in Eastern Time (Alpaca's primary timezone)
    private static final LocalTime PRE_MARKET_START = LocalTime.of(4, 0);  // 4:00 AM ET
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);      // 9:30 AM ET
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);     // 4:00 PM ET
    private static final LocalTime POST_MARKET_END = LocalTime.of(20, 0);  // 8:00 PM ET
    
    private static final ZoneId EASTERN_TIME = ZoneId.of("America/New_York");
    private static final int DECIMAL_PLACES = 2;

    public static PriceData calculatePriceDataFromSeries(BarSeries series, String symbol, String timerange) {
        if (series == null || series.getBarCount() == 0) {
            return createEmptyPriceData(symbol);
        }

        // Get the last trading day from the series
        ZonedDateTime lastTradingDay = null;
        
        if(timerange.equals("latest")) {
        	lastTradingDay = series.getBar(series.getBarCount() - 1).getEndTime ().withZoneSameInstant(ZoneId.systemDefault());
        	if(lastTradingDay.getHour() > 23) {
        		lastTradingDay = series.getBar(series.getBarCount() - 1).getBeginTime().withZoneSameInstant(ZoneId.systemDefault());
        	}
        }else {
        	lastTradingDay = series.getBar(series.getBarCount() - 1).getEndTime().withZoneSameInstant(ZoneId.systemDefault());
        }
        
        // Filter bars for the last trading day
        List<Bar> dailyBars = filterBarsByDate(series, lastTradingDay);
        
        if (dailyBars.isEmpty()) {
            return createEmptyPriceData(symbol);
        }

        // Separate bars by market session
        List<Bar> preMarketBars = filterBarsByTimeRange(dailyBars, PRE_MARKET_START, MARKET_OPEN);
        List<Bar> marketBars = filterBarsByTimeRange(dailyBars, MARKET_OPEN, MARKET_CLOSE);
        List<Bar> postMarketBars = filterBarsByTimeRange(dailyBars, MARKET_CLOSE, POST_MARKET_END);

        // Calculate session statistics including average volume per minute
        SessionStats preMarketStats = calculateSessionStats(preMarketBars, PRE_MARKET_START, MARKET_OPEN);
        SessionStats marketStats = calculateSessionStats(marketBars, MARKET_OPEN, MARKET_CLOSE);
        SessionStats postMarketStats = calculateSessionStats(postMarketBars, MARKET_CLOSE, POST_MARKET_END);

        // Get previous day's data (second to last day if available)
        PreviousDayData previousDayData = getPreviousDayData(series, lastTradingDay);
        double previousClose = previousDayData.closePrice;
        long prevDailyVol = previousDayData.totalVolume;

        // Calculate metrics
        double latestPrice = getLatestPrice(marketStats, postMarketStats, preMarketStats);
        
        double premarketChange = round(calculatePercentChange(preMarketStats.openPrice, preMarketStats.closePrice));
        double changeFromOpen = round(calculatePercentChange(marketStats.openPrice, marketStats.closePrice));
        double postmarketChange = round(calculatePercentChange(postMarketStats.openPrice, postMarketStats.closePrice));
        
        // Calculate highest percentile for each session
        double premarketHighestPercentile = round(calculateHighestPercentile(preMarketStats.openPrice, preMarketStats.highPrice));
        double marketHighestPercentile = round(calculateHighestPercentile(marketStats.openPrice, marketStats.highPrice));
        double postmarketHighestPercentile = round(calculateHighestPercentile(postMarketStats.openPrice, postMarketStats.highPrice));
        
        // Calculate lowest percentile for each session
        double premarketLowestPercentile = round(calculateLowestPercentile(preMarketStats.openPrice, preMarketStats.lowPrice));
        double marketLowestPercentile = round(calculateLowestPercentile(marketStats.openPrice, marketStats.lowPrice));
        double postmarketLowestPercentile = round(calculateLowestPercentile(postMarketStats.openPrice, postMarketStats.lowPrice));
        
        // Overall percent change from previous close
        double percentChange = round(calculatePercentChange(previousClose, latestPrice));

        // Calculate overall average volume per minute for the day
        long totalVolume = preMarketStats.totalVolume + marketStats.totalVolume + postMarketStats.totalVolume;
        long totalMinutes = preMarketStats.sessionMinutes + marketStats.sessionMinutes + postMarketStats.sessionMinutes;
        double averageVolPerMinute = round(totalMinutes > 0 ? (double) totalVolume / totalMinutes : 0.0);

        return new PriceData.Builder(symbol, round(latestPrice))
                .premarketClose(round(preMarketStats.closePrice))
                .premarketOpen(round(preMarketStats.openPrice))
                .premarketHigh(round(preMarketStats.highPrice))
                .premarketLow(round(preMarketStats.lowPrice))
                .premarketChange(premarketChange)
                .premarketVolume(preMarketStats.totalVolume)
                .premarketAvgVolPerMin(round(preMarketStats.avgVolumePerMinute))
                .premarketHighestPercentile(premarketHighestPercentile)
                .premarketLowestPercentile(premarketLowestPercentile)
                .changeFromOpen(changeFromOpen)
                .percentChange(percentChange)
                .currentVolume(totalVolume)
                .averageVol(averageVolPerMinute)
                .postmarketClose(round(postMarketStats.closePrice))
                .postmarketChange(postmarketChange)
                .postmarketVolume(postMarketStats.totalVolume)
                .postmarketAvgVolPerMin(round(postMarketStats.avgVolumePerMinute))
                .postmarketHighestPercentile(postmarketHighestPercentile)
                .postmarketLowestPercentile(postmarketLowestPercentile)
                .premarketHigh(round(preMarketStats.highPrice))
                .high(round(marketStats.highPrice))
                .postmarketHigh(round(postMarketStats.highPrice))
                .premarketLow(round(preMarketStats.lowPrice))
                .open(marketStats.openPrice)
                .low(round(marketStats.lowPrice))
                .postmarketLow(round(postMarketStats.lowPrice))
                .marketAvgVolPerMin(round(marketStats.avgVolumePerMinute))
                .marketHighestPercentile(marketHighestPercentile)
                .marketLowestPercentile(marketLowestPercentile)
                .prevLastDayPrice(round(previousClose))
                .previousVolume(prevDailyVol) // Add previous day's total volume
                .build();
    }

    private static PriceData createEmptyPriceData(String symbol) {
        return new PriceData.Builder(symbol, 0.0)
                .premarketClose(0.0)
                .premarketChange(0.0)
                .premarketVolume(0L)
                .premarketAvgVolPerMin(0.0)
                .premarketHighestPercentile(0.0)
                .premarketLowestPercentile(0.0)
                .changeFromOpen(0.0)
                .percentChange(0.0)
                .currentVolume(0L)
                .averageVol(0.0)
                .postmarketClose(0.0)
                .postmarketChange(0.0)
                .postmarketVolume(0L)
                .postmarketAvgVolPerMin(0.0)
                .postmarketHighestPercentile(0.0)
                .postmarketLowestPercentile(0.0)
                .premarketHigh(0.0)
                .high(0.0)
                .postmarketHigh(0.0)
                .premarketLow(0.0)
                .low(0.0)
                .postmarketLow(0.0)
                .marketAvgVolPerMin(0.0)
                .marketHighestPercentile(0.0)
                .marketLowestPercentile(0.0)
                .prevLastDayPrice(0.0)
                .previousVolume(0L) 
                .build();
    }

    private static double getLatestPrice(SessionStats marketStats, SessionStats postMarketStats, SessionStats preMarketStats) {
        if (marketStats.closePrice > 0) return marketStats.closePrice;
        if (postMarketStats.closePrice > 0) return postMarketStats.closePrice;
        return preMarketStats.closePrice;
    }

    private static List<Bar> filterBarsByDate(BarSeries series, ZonedDateTime targetDate) {
        List<Bar> filteredBars = new ArrayList<>();
        ZonedDateTime targetDateStart = targetDate.toLocalDate().atStartOfDay(EASTERN_TIME);
        ZonedDateTime targetDateEnd = targetDateStart.plusDays(1);

        for (int i = 0; i < series.getBarCount(); i++) {
            Bar bar = series.getBar(i);
            ZonedDateTime barTime = bar.getEndTime().withZoneSameInstant(EASTERN_TIME);
            
            if (!barTime.isBefore(targetDateStart) && barTime.isBefore(targetDateEnd)) {
                filteredBars.add(bar);
            }
        }
        return filteredBars;
    }

    private static List<Bar> filterBarsByTimeRange(List<Bar> bars, LocalTime startTime, LocalTime endTime) {
        List<Bar> filteredBars = new ArrayList<>();
        
        for (Bar bar : bars) {
            LocalTime barTime = bar.getEndTime().withZoneSameInstant(EASTERN_TIME).toLocalTime();
            
            if (!barTime.isBefore(startTime) && barTime.isBefore(endTime)) {
                filteredBars.add(bar);
            }
        }
        return filteredBars;
    }

    private static SessionStats calculateSessionStats(List<Bar> bars, LocalTime sessionStart, LocalTime sessionEnd) {
        SessionStats stats = new SessionStats();
        
        if (bars.isEmpty()) {
            return stats;
        }

        // Calculate session duration in minutes
        stats.sessionMinutes = Duration.between(sessionStart, sessionEnd).toMinutes();
        
        stats.openPrice = bars.get(0).getOpenPrice().doubleValue();
        stats.closePrice = bars.get(bars.size() - 1).getClosePrice().doubleValue();
        stats.highPrice = bars.get(0).getHighPrice().doubleValue();
        stats.lowPrice = bars.get(0).getLowPrice().doubleValue();
        
        for (Bar bar : bars) {
            double high = bar.getHighPrice().doubleValue();
            double low = bar.getLowPrice().doubleValue();
            long volume = bar.getVolume().longValue();
            
            stats.totalVolume += volume;
            stats.highPrice = Math.max(stats.highPrice, high);
            stats.lowPrice = Math.min(stats.lowPrice, low);
        }
        
        // Calculate average volume per minute for this session
        stats.avgVolumePerMinute = stats.sessionMinutes > 0 ? 
            (double) stats.totalVolume / stats.sessionMinutes : 0.0;
        
        return stats;
    }

    private static PreviousDayData getPreviousDayData(BarSeries series, ZonedDateTime currentDay) {
        ZonedDateTime previousDay = currentDay.minusDays(1);
        List<Bar> previousDayBars = filterBarsByDate(series, previousDay);
        
        PreviousDayData previousDayData = new PreviousDayData();
        
        if (previousDayBars.isEmpty()) {
            return previousDayData;
        }
        
        // Get the last bar of the previous day (market close) for the close price
        Bar lastBar = previousDayBars.get(previousDayBars.size() - 1);
        previousDayData.closePrice = lastBar.getClosePrice().doubleValue();
        
        // Calculate total volume for the entire previous day
        for (Bar bar : previousDayBars) {
            previousDayData.totalVolume += bar.getVolume().longValue();
        }
        
        return previousDayData;
    }

    private static double calculatePercentChange(double startPrice, double endPrice) {
        if (startPrice == 0) {
            return 0.0;
        }
        return ((endPrice - startPrice) / startPrice) * 100.0;
    }

    private static double calculateHighestPercentile(double openPrice, double highestHigh) {
        if (openPrice == 0) {
            return 0.0;
        }
        return ((highestHigh - openPrice) / openPrice) * 100.0;
    }
    
    private static double calculateLowestPercentile(double openPrice, double lowestLow) {
        if (openPrice == 0) {
            return 0.0;
        }
        return ((lowestLow - openPrice) / openPrice) * 100.0;
    }

    // Round to 2 decimal places using BigDecimal for precision
    private static double round(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return BigDecimal.valueOf(value)
                .setScale(DECIMAL_PLACES, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // Helper class to store session statistics
    private static class SessionStats {
        double openPrice = 0.0;
        double closePrice = 0.0;
        double highPrice = 0.0;
        double lowPrice = Double.MAX_VALUE;
        long totalVolume = 0L;
        long sessionMinutes = 0L;
        double avgVolumePerMinute = 0.0;
    }
    
 // Helper class to store previous day data
    private static class PreviousDayData {
        double closePrice = 0.0;
        long totalVolume = 0L;
    }
    
    
}