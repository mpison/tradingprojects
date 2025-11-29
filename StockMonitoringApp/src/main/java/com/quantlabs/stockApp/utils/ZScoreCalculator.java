package com.quantlabs.stockApp.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class ZScoreCalculator {
    
    public Map<String, Double> calculateZScores(ConcurrentHashMap<String, PriceData> priceDataMap) {
        Map<String, Double> zScores = new HashMap<>();
        
        // First, collect all the relevant data for calculations
        List<StockMetrics> allMetrics = new ArrayList<>();
        Map<String, StockMetrics> metricsBySymbol = new HashMap<>();
        
        for (Map.Entry<String, PriceData> entry : priceDataMap.entrySet()) {
            String symbol = entry.getKey();
            PriceData priceData = entry.getValue();
            StockMetrics metrics = extractMetrics(symbol, priceData);
            
            if (metrics != null) {
                allMetrics.add(metrics);
                metricsBySymbol.put(symbol, metrics);
            }
        }
        
        if (allMetrics.isEmpty()) {
            return zScores;
        }
        
        // Calculate statistics for each metric - convert int to double for breakoutCount
        Stats breakoutStats = calculateStats(allMetrics, m -> (double) m.breakoutCount);
        Stats targetPriceStats = calculateStats(allMetrics, m -> m.targetPrice);
        Stats volumeStats = calculateStats(allMetrics, m -> (double) m.volume);
        Stats percentileStats = calculateStats(allMetrics, m -> m.percentileIncrease);
        
        // Calculate Z-scores for each stock - INVERT breakout count (lower is better)
        for (Map.Entry<String, StockMetrics> entry : metricsBySymbol.entrySet()) {
            String symbol = entry.getKey();
            StockMetrics metrics = entry.getValue();
            
            // Individual Z-scores - INVERT breakout count (mean - value instead of value - mean)
            double breakoutZ = (breakoutStats.mean - metrics.breakoutCount) / breakoutStats.stdDev;
            double targetPriceZ = (metrics.targetPrice - targetPriceStats.mean) / targetPriceStats.stdDev;
            double volumeZ = (metrics.volume - volumeStats.mean) / volumeStats.stdDev;
            double percentileZ = (metrics.percentileIncrease - percentileStats.mean) / percentileStats.stdDev;
            
            // Composite Z-score (weighted average)
            double compositeZ = (0.3 * breakoutZ) + (0.3 * targetPriceZ) + (0.2 * volumeZ) + (0.2 * percentileZ);
            
            zScores.put(symbol, compositeZ);
        }
        
        return zScores;
    }
    
    private StockMetrics extractMetrics(String symbol, PriceData priceData) {
        if (priceData == null || priceData.getResults() == null) {
            return null;
        }
        
        StockMetrics metrics = new StockMetrics();
        metrics.symbol = symbol;
        
        // Extract metrics from all timeframes
        Map<String, AnalysisResult> results = priceData.getResults();
        int totalBreakouts = 0;
        double totalTargetPrice = 0;
        double totalPercentileIncrease = 0;
        int timeframeCount = 0;
        
        for (AnalysisResult result : results.values()) {
            totalBreakouts += result.getBreakoutCount();
            if (result.getMovingAverageTargetValue() != null) {
                totalTargetPrice += result.getMovingAverageTargetValue();
            }
            if (result.getMovingAverageTargetValuePercentile() != null) {
                totalPercentileIncrease += result.getMovingAverageTargetValuePercentile();
            }
            timeframeCount++;
        }
        
        if (timeframeCount > 0) {
            metrics.breakoutCount = totalBreakouts;
            metrics.targetPrice = totalTargetPrice / timeframeCount;
            metrics.percentileIncrease = totalPercentileIncrease / timeframeCount;
        } else {
            metrics.breakoutCount = totalBreakouts;
            metrics.targetPrice = 0;
            metrics.percentileIncrease = 0;
        }
        
        // Use current volume - handle null case
        metrics.volume = priceData.getCurrentVolume() != null ? priceData.getCurrentVolume() : 0L;
        
        return metrics;
    }
    
    private Stats calculateStats(List<StockMetrics> metrics, Function<StockMetrics, Double> extractor) {
        double sum = 0;
        double sumSquared = 0;
        int count = 0;
        
        for (StockMetrics metric : metrics) {
            double value = extractor.apply(metric);
            sum += value;
            sumSquared += value * value;
            count++;
        }
        
        double mean = sum / count;
        double variance = (sumSquared / count) - (mean * mean);
        double stdDev = Math.sqrt(variance);
        
        // Avoid division by zero
        if (stdDev == 0) {
            stdDev = 0.0001;
        }
        
        return new Stats(mean, stdDev);
    }
    
    // Helper classes
    private static class StockMetrics {
        String symbol;
        int breakoutCount;       // int type
        double targetPrice;      // double type
        long volume;             // long type (since getCurrentVolume returns Long)
        double percentileIncrease; // double type
    }
    
    private static class Stats {
        double mean;
        double stdDev;
        
        Stats(double mean, double stdDev) {
            this.mean = mean;
            this.stdDev = stdDev;
        }
    }
}