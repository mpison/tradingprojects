package com.quantlabs.stockApp;

import com.quantlabs.stockApp.model.PriceData;
import java.util.*;
import java.util.stream.Collectors;

public class ZScoreAnalyzer {
    
    // Configuration for Z-score combinations
    private Map<String, List<String>> zScoreCombinations;
    private Map<String, Double> historicalMeans;
    private Map<String, Double> historicalStdDevs;
    
    public ZScoreAnalyzer() {
        initializeCombinations();
        initializeHistoricalData();
    }
    
    private void initializeCombinations() {
        zScoreCombinations = new LinkedHashMap<>();
        
        // Basic combinations
        zScoreCombinations.put("PRICE_VOLUME", Arrays.asList("price", "currentVolume"));
        zScoreCombinations.put("PRICE_CHANGE", Arrays.asList("price", "percentChange"));
        zScoreCombinations.put("VOLUME_CHANGE", Arrays.asList("currentVolume", "percentChange"));
        
        // Market session combinations
        zScoreCombinations.put("PRE_MARKET", Arrays.asList("premarketPrice", "premarketVolume", "premarketChange"));
        zScoreCombinations.put("REGULAR_MARKET", Arrays.asList("price", "currentVolume", "changeFromOpen"));
        zScoreCombinations.put("POST_MARKET", Arrays.asList("postmarketPrice", "postmarketVolume", "postmarketChange"));
        
        // Advanced combinations
        zScoreCombinations.put("FULL_ANALYSIS", Arrays.asList(
            "price", "currentVolume", "percentChange", 
            "premarketPrice", "premarketVolume", "premarketChange",
            "postmarketPrice", "postmarketVolume", "postmarketChange",
            "volumeRatio", "priceGap"
        ));
        
        // Volume-focused combinations
        zScoreCombinations.put("VOLUME_INTENSITY", Arrays.asList(
            "currentVolume", "premarketVolume", "postmarketVolume",
            "volumeRatio", "volumePercentile"
        ));
        
        // Custom combinations can be added dynamically
    }
    
    private void initializeHistoricalData() {
        // These would typically come from a database or historical analysis
        historicalMeans = new HashMap<>();
        historicalStdDevs = new HashMap<>();
        
        // Example historical means (would be calculated from real data)
        historicalMeans.put("price", 50.0);
        historicalMeans.put("currentVolume", 1000000.0);
        historicalMeans.put("percentChange", 0.02);
        historicalMeans.put("premarketPrice", 49.5);
        historicalMeans.put("premarketVolume", 50000.0);
        historicalMeans.put("premarketChange", 0.01);
        historicalMeans.put("postmarketPrice", 50.5);
        historicalMeans.put("postmarketVolume", 30000.0);
        historicalMeans.put("postmarketChange", 0.005);
        historicalMeans.put("volumeRatio", 1.2);
        historicalMeans.put("priceGap", 0.01);
        historicalMeans.put("volumePercentile", 0.5);
        
        // Example standard deviations
        historicalStdDevs.put("price", 10.0);
        historicalStdDevs.put("currentVolume", 500000.0);
        historicalStdDevs.put("percentChange", 0.05);
        historicalStdDevs.put("premarketPrice", 2.0);
        historicalStdDevs.put("premarketVolume", 25000.0);
        historicalStdDevs.put("premarketChange", 0.03);
        historicalStdDevs.put("postmarketPrice", 2.0);
        historicalStdDevs.put("postmarketVolume", 15000.0);
        historicalStdDevs.put("postmarketChange", 0.02);
        historicalStdDevs.put("volumeRatio", 0.8);
        historicalStdDevs.put("priceGap", 0.03);
        historicalStdDevs.put("volumePercentile", 0.3);
    }
    
    public Map<String, Double> calculateIndividualZScores(PriceData priceData) {
        Map<String, Double> individualZScores = new HashMap<>();
        
        // Price-related Z-scores
        individualZScores.put("price", calculateZScore(priceData.getLatestPrice(), "price"));
        individualZScores.put("premarketPrice", calculateZScore(priceData.getPremarketClose(), "premarketPrice"));
        individualZScores.put("postmarketPrice", calculateZScore(priceData.getPostmarketClose(), "postmarketPrice"));
        
        // Volume-related Z-scores
        individualZScores.put("currentVolume", calculateZScore(priceData.getCurrentVolume(), "currentVolume"));
        individualZScores.put("premarketVolume", calculateZScore(priceData.getPremarketVolume(), "premarketVolume"));
        individualZScores.put("postmarketVolume", calculateZScore(priceData.getPostmarketVolume(), "postmarketVolume"));
        
        // Change-related Z-scores
        individualZScores.put("percentChange", calculateZScore(priceData.getPercentChange(), "percentChange"));
        individualZScores.put("premarketChange", calculateZScore(priceData.getPremarketChange(), "premarketChange"));
        individualZScores.put("postmarketChange", calculateZScore(priceData.getPostmarketChange(), "postmarketChange"));
        individualZScores.put("changeFromOpen", calculateZScore(priceData.getChangeFromOpen(), "percentChange")); // Using same as percentChange
        
        // Derived metrics
        individualZScores.put("volumeRatio", calculateVolumeRatioZScore(priceData));
        individualZScores.put("priceGap", calculatePriceGapZScore(priceData));
        individualZScores.put("volumePercentile", calculateVolumePercentileZScore(priceData));
        
        return individualZScores;
    }
    
    private double calculateZScore(Double value, String metric) {
        if (value == null || !historicalMeans.containsKey(metric) || !historicalStdDevs.containsKey(metric)) {
            return 0.0;
        }
        
        double mean = historicalMeans.get(metric);
        double stdDev = historicalStdDevs.get(metric);
        
        if (stdDev == 0) {
            return 0.0; // Avoid division by zero
        }
        
        return (value - mean) / stdDev;
    }
    
    private double calculateZScore(Long value, String metric) {
        return calculateZScore(value != null ? value.doubleValue() : null, metric);
    }
    
    private double calculateVolumeRatioZScore(PriceData priceData) {
        if (priceData.getCurrentVolume() == null || priceData.getAverageVol() == 0) {
            return 0.0;
        }
        
        double volumeRatio = priceData.getCurrentVolume() / priceData.getAverageVol();
        return calculateZScore(volumeRatio, "volumeRatio");
    }
    
    private double calculatePriceGapZScore(PriceData priceData) {
        if (priceData.getPremarketClose() == null || priceData.getPrevLastDayPrice() == 0) {
            return 0.0;
        }
        
        double priceGap = (priceData.getPremarketClose() - priceData.getPrevLastDayPrice()) / priceData.getPrevLastDayPrice();
        return calculateZScore(priceGap, "priceGap");
    }
    
    private double calculateVolumePercentileZScore(PriceData priceData) {
        // This would typically be calculated from historical volume distribution
        // For simplicity, we'll use a placeholder
        double percentile = 0.5; // Default median
        return calculateZScore(percentile, "volumePercentile");
    }
    
    public Map<String, Double> calculateCombinedZScores(PriceData priceData, String combinationName) {
        if (!zScoreCombinations.containsKey(combinationName)) {
            throw new IllegalArgumentException("Unknown combination: " + combinationName);
        }
        
        Map<String, Double> individualZScores = calculateIndividualZScores(priceData);
        List<String> metrics = zScoreCombinations.get(combinationName);
        
        // Calculate weighted average Z-score (equal weights for simplicity)
        double sum = 0.0;
        int count = 0;
        
        for (String metric : metrics) {
            Double zScore = individualZScores.get(metric);
            if (zScore != null) {
                sum += zScore;
                count++;
            }
        }
        
        Map<String, Double> result = new HashMap<>();
        result.put("combinedZScore", count > 0 ? sum / count : 0.0);
        result.put("componentCount", (double) count);
        
        // Add individual scores for transparency
        for (String metric : metrics) {
            result.put(metric + "_Z", individualZScores.getOrDefault(metric, 0.0));
        }
        
        return result;
    }
    
    public Map<String, Map<String, Double>> calculateAllCombinations(PriceData priceData) {
        Map<String, Map<String, Double>> allResults = new LinkedHashMap<>();
        
        for (String combination : zScoreCombinations.keySet()) {
            allResults.put(combination, calculateCombinedZScores(priceData, combination));
        }
        
        return allResults;
    }
    
    public void addCustomCombination(String name, List<String> metrics) {
        zScoreCombinations.put(name, new ArrayList<>(metrics));
    }
    
    public void removeCombination(String name) {
        zScoreCombinations.remove(name);
    }
    
    public List<String> getAvailableCombinations() {
        return new ArrayList<>(zScoreCombinations.keySet());
    }
    
    public void updateHistoricalData(String metric, double newMean, double newStdDev) {
        historicalMeans.put(metric, newMean);
        historicalStdDevs.put(metric, newStdDev);
    }
    
    public List<String> getCombinationMetrics(String combinationName) {
        return zScoreCombinations.get(combinationName);
    }
    
    // Main method for standalone testing
    public static void main(String[] args) {
        ZScoreAnalyzer analyzer = new ZScoreAnalyzer();
        
        // Create sample PriceData
        PriceData sampleData = new PriceData.Builder("AAPL", 150.0)
            .currentVolume(2000000L)
            .averageVol(1500000.0)
            .percentChange(0.03)
            .premarketClose(148.5)
            .premarketVolume(75000L)
            .premarketChange(0.01)
            .postmarketClose(151.0)
            .postmarketVolume(45000L)
            .postmarketChange(0.02)
            .prevLastDayPrice(145.0)
            .build();
        
        System.out.println("=== Individual Z-Scores ===");
        Map<String, Double> individualScores = analyzer.calculateIndividualZScores(sampleData);
        individualScores.forEach((metric, score) -> 
            System.out.printf("%-20s: %.3f%n", metric, score));
        
        System.out.println("\n=== Combination Results ===");
        Map<String, Map<String, Double>> allCombinations = analyzer.calculateAllCombinations(sampleData);
        
        allCombinations.forEach((combination, scores) -> {
            System.out.println("\n" + combination + ":");
            scores.forEach((metric, score) -> 
                System.out.printf("  %-25s: %.3f%n", metric, score));
        });
        
        // Add a custom combination
        analyzer.addCustomCombination("CUSTOM_PRICE", Arrays.asList("price", "premarketPrice", "postmarketPrice"));
        
        System.out.println("\n=== Custom Combination ===");
        Map<String, Double> customResult = analyzer.calculateCombinedZScores(sampleData, "CUSTOM_PRICE");
        customResult.forEach((metric, score) -> 
            System.out.printf("%-25s: %.3f%n", metric, score));
        
        System.out.println("\nAvailable combinations: " + analyzer.getAvailableCombinations());
    }
}