package com.quantlabs.stockApp.indicator.management;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class ZScoreCalculator {
    
    public static class TimeframeWeight {
        private String timeframe;
        private int weight; // 0-100
        private double normalizedWeight; // 0.0-1.0
        
        public TimeframeWeight(String timeframe, int weight) {
            this.timeframe = timeframe;
            this.weight = weight;
        }
        
        // Getters and setters
        public String getTimeframe() { return timeframe; }
        public int getWeight() { return weight; }
        public double getNormalizedWeight() { return normalizedWeight; }
        public void setNormalizedWeight(double normalizedWeight) { this.normalizedWeight = normalizedWeight; }
        
        @Override
        public String toString() {
            return String.format("TimeframeWeight{timeframe='%s', weight=%d, normalizedWeight=%.2f}", 
                timeframe, weight, normalizedWeight);
        }
    }
    
    public static class ZScoreResult {
        private String strategyName;
        private double overallScore;
        private Map<String, Double> timeframeScores;
        private Map<String, Double> weightedScores;
        private String strengthCategory;
        private Map<String, TimeframeWeight> timeframeWeights;
        
        public ZScoreResult(String strategyName) {
            this.strategyName = strategyName;
            this.timeframeScores = new HashMap<>();
            this.weightedScores = new HashMap<>();
            this.timeframeWeights = new HashMap<>();
        }
        
        // Getters and setters
        public String getStrategyName() { return strategyName; }
        public double getOverallScore() { return overallScore; }
        public void setOverallScore(double overallScore) { this.overallScore = overallScore; }
        public Map<String, Double> getTimeframeScores() { return timeframeScores; }
        public Map<String, Double> getWeightedScores() { return weightedScores; }
        public String getStrengthCategory() { return strengthCategory; }
        public void setStrengthCategory(String strengthCategory) { this.strengthCategory = strengthCategory; }
        public Map<String, TimeframeWeight> getTimeframeWeights() { return timeframeWeights; }
        public void setTimeframeWeights(Map<String, TimeframeWeight> timeframeWeights) { 
            this.timeframeWeights = timeframeWeights; 
        }
        
        /**
         * Get detailed breakdown for display
         */
        public String getDetailedBreakdown() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Strategy: %s\n", strategyName));
            sb.append(String.format("Overall Score: %.1f/100\n", overallScore));
            sb.append(String.format("Strength: %s\n\n", strengthCategory));
            
            sb.append("Timeframe Breakdown:\n");
            for (Map.Entry<String, TimeframeWeight> entry : timeframeWeights.entrySet()) {
                String timeframe = entry.getKey();
                TimeframeWeight weight = entry.getValue();
                Double score = timeframeScores.get(timeframe);
                Double weighted = weightedScores.get(timeframe);
                
                if (score != null && weighted != null) {
                    sb.append(String.format("  %-8s: %3d%% -> %5.1f raw -> %5.1f weighted\n", 
                        timeframe, weight.getWeight(), score, weighted));
                }
            }
            
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return String.format("ZScoreResult{strategy='%s', score=%.1f, category='%s', timeframes=%d}", 
                strategyName, overallScore, strengthCategory, timeframeWeights.size());
        }
    }
    
    // Constant for maximum possible score
    public static final double MAX_ZSCORE = 100.0;
    
    /**
     * Calculate overall Z-Score for multiple strategies
     */
    public static Map<String, ZScoreResult> calculateOverallScores(PriceData priceData, 
                                                                  Map<String, Map<String, TimeframeWeight>> strategyWeights) {
        Map<String, ZScoreResult> results = new HashMap<>();
        
        if (priceData == null || priceData.getResults() == null || priceData.getResults().isEmpty()) {
            return results;
        }
        
        for (Map.Entry<String, Map<String, TimeframeWeight>> entry : strategyWeights.entrySet()) {
            String strategyName = entry.getKey();
            Map<String, TimeframeWeight> timeframeWeights = entry.getValue();
            
            ZScoreResult result = calculateStrategyScore(priceData, strategyName, timeframeWeights);
            results.put(strategyName, result);
        }
        
        return results;
    }
    
    /**
     * Calculate Z-Score for a single strategy
     */
    public static ZScoreResult calculateStrategyScore(PriceData priceData, 
                                                     String strategyName,
                                                     Map<String, TimeframeWeight> timeframeWeights) {
        ZScoreResult result = new ZScoreResult(strategyName);
        result.setTimeframeWeights(new HashMap<>(timeframeWeights));
        
        if (priceData == null || priceData.getResults() == null || priceData.getResults().isEmpty()) {
            result.setOverallScore(0.0);
            result.setStrengthCategory("No Data");
            return result;
        }
        
        // Normalize weights to sum to 1.0
        normalizeWeights(timeframeWeights);
        
        double totalWeightedScore = 0.0;
        int calculatedTimeframes = 0;
        
        // Calculate score for each timeframe
        for (Map.Entry<String, TimeframeWeight> entry : timeframeWeights.entrySet()) {
            String timeframe = entry.getKey();
            TimeframeWeight weight = entry.getValue();
            AnalysisResult analysisResult = priceData.getResults().get(timeframe);
            
            if (analysisResult != null) {
                double timeframeScore = calculateTimeframeScore(analysisResult);
                result.getTimeframeScores().put(timeframe, timeframeScore);
                
                double weightedScore = timeframeScore * weight.getNormalizedWeight();
                result.getWeightedScores().put(timeframe, weightedScore);
                totalWeightedScore += weightedScore;
                calculatedTimeframes++;
            } else {
                // No data for this timeframe
                result.getTimeframeScores().put(timeframe, 0.0);
                result.getWeightedScores().put(timeframe, 0.0);
            }
        }
        
        // If no timeframes had data, return zero score
        if (calculatedTimeframes == 0) {
            result.setOverallScore(0.0);
            result.setStrengthCategory("No Data");
        } else {
            result.setOverallScore(totalWeightedScore);
            result.setStrengthCategory(determineStrengthCategory(totalWeightedScore));
        }
        
        return result;
    }
    
    /**
     * Calculate score for a single timeframe based on all available indicators
     */
    private static double calculateTimeframeScore(AnalysisResult analysisResult) {
        if (analysisResult == null) {
            return 0.0;
        }
        
        List<Double> indicatorScores = new ArrayList<>();
        
        // Collect all available indicator Z-scores
        // Trend indicators
        if (!Double.isNaN(analysisResult.getTrendZscore()) && analysisResult.getTrendZscore() > 0) {
            indicatorScores.add(analysisResult.getTrendZscore());
        }
        
        // MACD indicators
        if (!Double.isNaN(analysisResult.getMacdZscore()) && analysisResult.getMacdZscore() > 0) {
            indicatorScores.add(analysisResult.getMacdZscore());
        }
        if (!Double.isNaN(analysisResult.getMacd359Zscore()) && analysisResult.getMacd359Zscore() > 0) {
            indicatorScores.add(analysisResult.getMacd359Zscore());
        }
        
        // RSI indicator
        if (!Double.isNaN(analysisResult.getRsiZscore()) && analysisResult.getRsiZscore() > 0) {
            indicatorScores.add(analysisResult.getRsiZscore());
        }
        
        // PSAR indicator
        if (!Double.isNaN(analysisResult.getPsarZscore()) && analysisResult.getPsarZscore() > 0) {
            indicatorScores.add(analysisResult.getPsarZscore());
        }
        
        // Volume indicators
        /*if (!Double.isNaN(analysisResult.getVolumeZscore()) && analysisResult.getVolumeZscore() > 0) {
            indicatorScores.add(analysisResult.getVolumeZscore());
        }*/
        
        // VWAP indicator
        if (!Double.isNaN(analysisResult.getVwapZscore()) && analysisResult.getVwapZscore() > 0) {
            indicatorScores.add(analysisResult.getVwapZscore());
        }
        
        // Heiken Ashi indicator
        if (!Double.isNaN(analysisResult.getHeikenAshiZscore()) && analysisResult.getHeikenAshiZscore() > 0) {
            indicatorScores.add(analysisResult.getHeikenAshiZscore());
        }
        
        // Highest Close Open indicator
        if (!Double.isNaN(analysisResult.getHighestCloseOpenZscore()) && analysisResult.getHighestCloseOpenZscore() > 0) {
            indicatorScores.add(analysisResult.getHighestCloseOpenZscore());
        }
        
        // Calculate average of all indicator scores for this timeframe
        if (indicatorScores.isEmpty()) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (Double score : indicatorScores) {
            sum += score;
        }
        
        return sum / indicatorScores.size();
    }
    
    /**
     * Normalize timeframe weights to sum to 1.0
     */
    private static void normalizeWeights(Map<String, TimeframeWeight> timeframeWeights) {
        if (timeframeWeights == null || timeframeWeights.isEmpty()) {
            return;
        }
        
        int totalWeight = 0;
        for (TimeframeWeight weight : timeframeWeights.values()) {
            totalWeight += weight.getWeight();
        }
        
        if (totalWeight > 0) {
            for (TimeframeWeight weight : timeframeWeights.values()) {
                double normalized = (double) weight.getWeight() / totalWeight;
                weight.setNormalizedWeight(normalized);
            }
        } else {
            // If total weight is 0, distribute equally
            double equalWeight = 1.0 / timeframeWeights.size();
            for (TimeframeWeight weight : timeframeWeights.values()) {
                weight.setNormalizedWeight(equalWeight);
            }
        }
    }
    
    /**
     * Determine strength category based on overall score
     */
    private static String determineStrengthCategory(double score) {
        if (score >= 90) return "Exceptional";
        if (score >= 80) return "Very Strong";
        if (score >= 70) return "Strong";
        if (score >= 60) return "Good";
        if (score >= 50) return "Moderate";
        if (score >= 40) return "Fair";
        if (score >= 30) return "Weak";
        if (score >= 20) return "Very Weak";
        return "Poor";
    }
    
    /**
     * Validate that weights sum to 100 for a strategy
     */
    public static boolean validateWeights(Map<String, TimeframeWeight> timeframeWeights) {
        if (timeframeWeights == null || timeframeWeights.isEmpty()) {
            return false;
        }
        
        int totalWeight = 0;
        for (TimeframeWeight weight : timeframeWeights.values()) {
            totalWeight += weight.getWeight();
        }
        return totalWeight == 100;
    }
    
    /**
     * Get combined score across all strategies (weighted average)
     */
    public static double getCombinedScore(Map<String, ZScoreResult> strategyScores) {
        if (strategyScores == null || strategyScores.isEmpty()) {
            return 0.0;
        }
        
        double totalScore = 0.0;
        for (ZScoreResult result : strategyScores.values()) {
            totalScore += result.getOverallScore();
        }
        
        return totalScore / strategyScores.size();
    }
    
    /**
     * Get the best performing strategy
     */
    public static ZScoreResult getBestStrategy(Map<String, ZScoreResult> strategyScores) {
        if (strategyScores == null || strategyScores.isEmpty()) {
            return null;
        }
        
        return strategyScores.values().stream()
                .max(Comparator.comparingDouble(ZScoreResult::getOverallScore))
                .orElse(null);
    }
    
    /**
     * Get the worst performing strategy
     */
    public static ZScoreResult getWorstStrategy(Map<String, ZScoreResult> strategyScores) {
        if (strategyScores == null || strategyScores.isEmpty()) {
            return null;
        }
        
        return strategyScores.values().stream()
                .min(Comparator.comparingDouble(ZScoreResult::getOverallScore))
                .orElse(null);
    }
    
    /**
     * Get strategies by strength category
     */
    public static Map<String, List<ZScoreResult>> getStrategiesByCategory(Map<String, ZScoreResult> strategyScores) {
        Map<String, List<ZScoreResult>> categorized = new HashMap<>();
        
        if (strategyScores == null) {
            return categorized;
        }
        
        for (ZScoreResult result : strategyScores.values()) {
            String category = result.getStrengthCategory();
            categorized.computeIfAbsent(category, k -> new ArrayList<>()).add(result);
        }
        
        return categorized;
    }
    
    /**
     * Get strategies sorted by score (highest first)
     */
    public static List<ZScoreResult> getStrategiesSortedByScore(Map<String, ZScoreResult> strategyScores) {
        if (strategyScores == null || strategyScores.isEmpty()) {
            return new ArrayList<>();
        }
        
        return strategyScores.values().stream()
                .sorted((a, b) -> Double.compare(b.getOverallScore(), a.getOverallScore()))
                .collect(Collectors.toList());
    }
    
    /**
     * Calculate confidence level based on score consistency across timeframes
     */
    public static double calculateConfidenceLevel(ZScoreResult result) {
        if (result == null || result.getTimeframeScores().isEmpty()) {
            return 0.0;
        }
        
        // Calculate standard deviation of timeframe scores
        List<Double> scores = new ArrayList<>(result.getTimeframeScores().values());
        double mean = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        double variance = scores.stream()
                .mapToDouble(score -> Math.pow(score - mean, 2))
                .average()
                .orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        
        // Lower standard deviation = higher confidence (more consistent scores)
        // Normalize to 0-100 scale (inverse relationship)
        double confidence = Math.max(0, 100 - (stdDev * 2));
        return Math.min(100, confidence);
    }
    
    /**
     * Generate a summary report for all strategies
     */
    public static String generateSummaryReport(Map<String, ZScoreResult> strategyScores) {
        if (strategyScores == null || strategyScores.isEmpty()) {
            return "No strategy scores available.";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("Z-SCORE STRATEGY ANALYSIS REPORT\n");
        report.append("================================\n\n");
        
        // Overall statistics
        double combinedScore = getCombinedScore(strategyScores);
        ZScoreResult bestStrategy = getBestStrategy(strategyScores);
        ZScoreResult worstStrategy = getWorstStrategy(strategyScores);
        
        report.append(String.format("Overall Combined Score: %.1f/100\n", combinedScore));
        report.append(String.format("Number of Strategies: %d\n\n", strategyScores.size()));
        
        // Best strategy
        if (bestStrategy != null) {
            report.append("BEST PERFORMING STRATEGY:\n");
            report.append(String.format("  %s: %.1f/100 (%s)\n", 
                bestStrategy.getStrategyName(), 
                bestStrategy.getOverallScore(),
                bestStrategy.getStrengthCategory()));
        }
        
        // Strategies by category
        Map<String, List<ZScoreResult>> byCategory = getStrategiesByCategory(strategyScores);
        report.append("\nSTRATEGIES BY STRENGTH CATEGORY:\n");
        for (Map.Entry<String, List<ZScoreResult>> entry : byCategory.entrySet()) {
            report.append(String.format("  %s: %d strategies\n", 
                entry.getKey(), entry.getValue().size()));
        }
        
        // Detailed breakdown
        report.append("\nDETAILED BREAKDOWN:\n");
        List<ZScoreResult> sortedStrategies = getStrategiesSortedByScore(strategyScores);
        for (ZScoreResult strategy : sortedStrategies) {
            double confidence = calculateConfidenceLevel(strategy);
            report.append(String.format("  %-30s: %5.1f/100 (%s) [Confidence: %.1f%%]\n", 
                strategy.getStrategyName(),
                strategy.getOverallScore(),
                strategy.getStrengthCategory(),
                confidence));
        }
        
        return report.toString();
    }
    
    /**
     * Calculate score for a specific indicator type across all timeframes
     */
    public static double calculateIndicatorTypeScore(PriceData priceData, String indicatorType) {
        if (priceData == null || priceData.getResults() == null) {
            return 0.0;
        }
        
        List<Double> scores = new ArrayList<>();
        
        for (AnalysisResult result : priceData.getResults().values()) {
            if (result != null) {
                double score = getIndicatorScore(result, indicatorType);
                if (score > 0) {
                    scores.add(score);
                }
            }
        }
        
        if (scores.isEmpty()) {
            return 0.0;
        }
        
        return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
    
    /**
     * Get score for a specific indicator type from AnalysisResult
     */
    private static double getIndicatorScore(AnalysisResult result, String indicatorType) {
        if (result == null) {
            return 0.0;
        }
        
        switch (indicatorType.toUpperCase()) {
            case "TREND":
                return !Double.isNaN(result.getTrendZscore()) ? result.getTrendZscore() : 0.0;
            case "MACD":
                return !Double.isNaN(result.getMacdZscore()) ? result.getMacdZscore() : 0.0;
            case "MACD359":
                return !Double.isNaN(result.getMacd359Zscore()) ? result.getMacd359Zscore() : 0.0;
            case "RSI":
                return !Double.isNaN(result.getRsiZscore()) ? result.getRsiZscore() : 0.0;
            case "PSAR":
                return !Double.isNaN(result.getPsarZscore()) ? result.getPsarZscore() : 0.0;
            /*case "VOLUME":
                return !Double.isNaN(result.getVolumeZscore()) ? result.getVolumeZscore() : 0.0;*/
            case "VWAP":
                return !Double.isNaN(result.getVwapZscore()) ? result.getVwapZscore() : 0.0;
            case "HEIKENASHI":
                return !Double.isNaN(result.getHeikenAshiZscore()) ? result.getHeikenAshiZscore() : 0.0;
            case "HIGHESTCLOSEOPEN":
                return !Double.isNaN(result.getHighestCloseOpenZscore()) ? result.getHighestCloseOpenZscore() : 0.0;
            default:
                return 0.0;
        }
    }
}