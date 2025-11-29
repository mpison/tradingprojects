package com.quantlabs.stockApp.backtesting;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.ta4j.core.BarSeries;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.data.StockDataProvider;
import com.quantlabs.stockApp.model.PriceData;

public class MultiTimeframeDataManager {
    private final StockDataProvider dataProvider;
    private final ConsoleLogger logger;
    private final ExecutorService executorService;
    private final Map<String, Map<String, BarSeries>> dataCache = new ConcurrentHashMap<>();
    
    public MultiTimeframeDataManager(StockDataProvider dataProvider, ConsoleLogger logger) {
        this.dataProvider = dataProvider;
        this.logger = logger;
        this.executorService = Executors.newFixedThreadPool(5);
    }
    
    public Map<String, BarSeries> loadMultiTimeframeData(String symbol, List<String> timeframes, 
                                                        LocalDateTime start, LocalDateTime end) {
        Map<String, BarSeries> timeframeData = new HashMap<>();
        List<Future<TimeframeDataResult>> futures = new ArrayList<>();
        
        ZonedDateTime startZdt = start.atZone(ZoneId.systemDefault());
        ZonedDateTime endZdt = end.atZone(ZoneId.systemDefault());
        
        for (String timeframe : timeframes) {
            Future<TimeframeDataResult> future = executorService.submit(() -> {
                try {
                    BarSeries series = dataProvider.getHistoricalData(symbol, timeframe, 10000, startZdt, endZdt);
                    return new TimeframeDataResult(timeframe, series, null);
                } catch (IOException e) {
                    return new TimeframeDataResult(timeframe, null, e);
                }
            });
            futures.add(future);
        }
        
        for (Future<TimeframeDataResult> future : futures) {
            try {
                TimeframeDataResult result = future.get(30, TimeUnit.SECONDS);
                if (result.series != null && result.series.getBarCount() > 0) {
                    timeframeData.put(result.timeframe, result.series);
                    logger.log("Loaded " + result.timeframe + " data: " + result.series.getBarCount() + " bars");
                } else {
                    logger.log("Warning: No data loaded for timeframe " + result.timeframe);
                }
            } catch (Exception e) {
                logger.log("Error loading timeframe data: " + e.getMessage());
            }
        }
        
        dataCache.put(symbol, timeframeData);
        return timeframeData;
    }
    
    public void updatePriceDataMap(String symbol, Map<String, BarSeries> timeframeData, 
                                  Map<String, PriceData> priceDataMap) {
        PriceData priceData = priceDataMap.computeIfAbsent(symbol, k -> new PriceData());
        // Update PriceData with analysis results
        priceDataMap.put(symbol, priceData);
    }
    
    public void clearCache(String symbol) {
        dataCache.remove(symbol);
    }
    
    public void clearAllCache() {
        dataCache.clear();
    }
    
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private static class TimeframeDataResult {
        final String timeframe;
        final BarSeries series;
        final Exception error;
        
        TimeframeDataResult(String timeframe, BarSeries series, Exception error) {
            this.timeframe = timeframe;
            this.series = series;
            this.error = error;
        }
    }
}