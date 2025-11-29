package com.quantlabs.stockApp.reports;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

import com.quantlabs.stockApp.data.StockDataProvider;
import com.quantlabs.stockApp.data.ConsoleLogger;

public class StockProviderMonteCarloDataSource implements MonteCarloDataSource {
    private final StockDataProvider stockDataProvider;
    private final String providerName;
    private final ConsoleLogger logger;

    public StockProviderMonteCarloDataSource(StockDataProvider stockDataProvider, String providerName, ConsoleLogger logger) {
        this.stockDataProvider = stockDataProvider;
        this.providerName = providerName;
        this.logger = logger;
    }

    @Override
    public Map<String, Map<ZonedDateTime, Double[]>> fetchData(List<String> symbols, ZonedDateTime start, ZonedDateTime end) {
        // Default to 1Min for backward compatibility
        return fetchData(symbols, start, end, "1Min");
    }

    @Override
    public Map<String, Map<ZonedDateTime, Double[]>> fetchData(List<String> symbols, ZonedDateTime start, ZonedDateTime end, String timeframe) {
        Map<String, Map<ZonedDateTime, Double[]>> result = new ConcurrentHashMap<>();
        
        // Convert UI timeframe to provider timeframe format
        String providerTimeframe = convertTimeframeForProvider(timeframe);
        
        for (String symbol : symbols) {
            try {
                // Fetch historical data with the specified timeframe
                BarSeries series = stockDataProvider.getHistoricalData(symbol, providerTimeframe, 10000, start, end);
                
                if (series.getBarCount() > 0) {
                    Map<ZonedDateTime, Double[]> dataMap = calculateCumulativeReturns(series);
                    if (!dataMap.isEmpty()) {
                        result.put(symbol, dataMap);
                        System.out.println("Loaded " + symbol + " data with timeframe " + timeframe + ": " + dataMap.size() + " entries");
                    }
                } else {
                    System.out.println("No data found for symbol: " + symbol + " with timeframe " + timeframe);
                }
            } catch (Exception e) {
                System.out.println("Error processing " + symbol + " with timeframe " + timeframe + ": " + e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * Convert UI timeframe format to provider-specific timeframe format
     */
    private String convertTimeframeForProvider(String uiTimeframe) {
        switch (uiTimeframe) {
            case "1W": return "1Week";
            case "1D": return "1Day";
            case "4H": return "4Hour";
            case "1H": return "1Hour";
            case "30Min": return "30Min";
            case "15Min": return "15Min";
            case "5Min": return "5Min";
            case "1Min": return "1Min";
            default: return "1Min"; // Default fallback
        }
    }


    private Map<ZonedDateTime, Double[]> calculateCumulativeReturns(BarSeries series) {
        Map<ZonedDateTime, Double[]> cumulativeReturns = new java.util.LinkedHashMap<>();
        
        if (series.getBarCount() < 2) {
            System.out.println("Insufficient data for Monte Carlo simulation: " + series.getName() + " has " + series.getBarCount() + " bars");
            return cumulativeReturns;
        }
        
        double cumulative = 0;
        cumulativeReturns.put(series.getBar(0).getEndTime(), new Double[]{0.0, series.getBar(0).getVolume().doubleValue()});
        
        for (int i = 1; i < series.getBarCount(); i++) {
            double prevClose = series.getBar(i - 1).getClosePrice().doubleValue();
            double currentClose = series.getBar(i).getClosePrice().doubleValue();
            ZonedDateTime time = series.getBar(i).getEndTime();
            double volume = series.getBar(i).getVolume().doubleValue();
            
            if (prevClose <= 0 || currentClose <= 0 || Double.isNaN(prevClose) || Double.isNaN(currentClose)) {
                System.out.println("Invalid price data for " + series.getName() + " at index " + i);
                continue;
            }
            
            double returnPct = (currentClose - prevClose) / prevClose * 100;
            if (Double.isNaN(returnPct) || Double.isInfinite(returnPct)) {
                System.out.println("Invalid return percentage for " + series.getName() + " at index " + i);
                continue;
            }
            
            cumulative += returnPct;
            if (Double.isNaN(cumulative) || Double.isInfinite(cumulative)) {
                System.out.println("Non-finite cumulative return for " + series.getName() + " at index " + i);
                continue;
            }
            
            cumulativeReturns.put(time, new Double[]{cumulative, volume});
        }
        
        if (cumulativeReturns.size() <= 1) {
            System.out.println("No valid cumulative returns calculated for " + series.getName());
        }
        
        return cumulativeReturns;
    }

    @Override
    public String getName() {
        return providerName;
    }

    @Override
    public boolean isAvailable() {
        try {
            // Test the provider by trying to get current time
            stockDataProvider.getCurrentTime();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}