package com.quantlabs.stockApp.indicator.management;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;

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
        private int rank;
        private Map<String, Double> timeframeScores;
        private Map<String, Double> weightedScores;
        private String strengthCategory;
        private Map<String, TimeframeWeight> timeframeWeights;
        private Map<String, Map<String, Double>> indicatorContributions; // New: track individual indicator contributions
        
        public ZScoreResult(String strategyName) {
            this.strategyName = strategyName;
            this.timeframeScores = new HashMap<>();
            this.weightedScores = new HashMap<>();
            this.timeframeWeights = new HashMap<>();
            this.indicatorContributions = new HashMap<>();
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
        public int getRank() {
			return rank;
		}

		public void setRank(int rank) {
			this.rank = rank;
		}

		public Map<String, Map<String, Double>> getIndicatorContributions() { return indicatorContributions; }
        public void setIndicatorContributions(Map<String, Map<String, Double>> indicatorContributions) { 
            this.indicatorContributions = indicatorContributions; 
        }
        
        /**
         * Add indicator contribution for a timeframe
         */
        public void addIndicatorContribution(String timeframe, String indicatorName, double contribution) {
            indicatorContributions.computeIfAbsent(timeframe, k -> new HashMap<>())
                                 .put(indicatorName, contribution);
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
                    
                    // Show indicator contributions if available
                    Map<String, Double> contributions = indicatorContributions.get(timeframe);
                    if (contributions != null && !contributions.isEmpty()) {
                        sb.append("    Indicator Contributions:\n");
                        for (Map.Entry<String, Double> contrib : contributions.entrySet()) {
                            sb.append(String.format("      %-20s: %5.1f\n", 
                                contrib.getKey(), contrib.getValue()));
                        }
                    }
                }
            }
            
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return String.format("ZScoreResult{strategy='%s', score=%.1f, rank=%d, category='%s', timeframes=%d}", 
                strategyName, overallScore, rank, strengthCategory, timeframeWeights.size());
        }
    }
    
    // Constant for maximum possible score
    public static final double MAX_ZSCORE = 100.0;
    
    // BACKWARD COMPATIBLE METHODS
    
    /**
     * Calculate overall Z-Score for multiple strategies (backward compatible)
     */
    public static Map<String, ZScoreResult> calculateOverallScores(PriceData priceData, 
                                                                  Map<String, Map<String, TimeframeWeight>> strategyWeights) {
        return calculateOverallScores(priceData, strategyWeights, null);
    }
    
    /**
     * Calculate Z-Score for a single strategy (backward compatible)
     */
    public static ZScoreResult calculateStrategyScore(PriceData priceData, 
                                                     String strategyName,
                                                     Map<String, TimeframeWeight> timeframeWeights) {
        return calculateStrategyScore(priceData, strategyName, timeframeWeights, null);
    }
    
    // ENHANCED METHODS WITH INDICATOR WEIGHTS SUPPORT
    
    /**
     * Calculate overall Z-Score for multiple strategies with indicator weights
     */
    public static Map<String, ZScoreResult> calculateOverallScores(PriceData priceData, 
                                                                  Map<String, Map<String, TimeframeWeight>> strategyWeights,
                                                                  Map<String, StrategyConfig> strategyConfigs) {
        Map<String, ZScoreResult> results = new HashMap<>();
        
        if (priceData == null || priceData.getResults() == null || priceData.getResults().isEmpty()) {
            return results;
        }
        
        for (Map.Entry<String, Map<String, TimeframeWeight>> entry : strategyWeights.entrySet()) {
            String strategyName = entry.getKey();
            Map<String, TimeframeWeight> timeframeWeights = entry.getValue();
            StrategyConfig strategyConfig = strategyConfigs != null ? strategyConfigs.get(strategyName) : null;
            
            ZScoreResult result = calculateStrategyScore(priceData, strategyName, timeframeWeights, strategyConfig);
            results.put(strategyName, result);
        }
        
        return results;
    }
    
    /**
     * Calculate Z-Score for a single strategy with indicator weights support
     */
    public static ZScoreResult calculateStrategyScore(PriceData priceData, 
                                                     String strategyName,
                                                     Map<String, TimeframeWeight> timeframeWeights,
                                                     StrategyConfig strategyConfig) {
    	 // Check if Z-Score is enabled for this strategy
        boolean zscoreEnabled = (boolean) strategyConfig.getParameters()
            .getOrDefault("zscoreEnabled", true);
        
        if (!zscoreEnabled) {
            ZScoreResult disabledResult = new ZScoreResult(strategyName);
            disabledResult.setOverallScore(0.0);
            disabledResult.setStrengthCategory("Disabled");
            return disabledResult;
        }
    	
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
                // Get indicator weights for this timeframe if configured
                Map<String, Integer> indicatorWeights = strategyConfig != null ? 
                    (Map<String, Integer>) strategyConfig.getParameters().get("indicatorWeights_" + timeframe) : null;
                
                double timeframeScore;
                Map<String, Double> indicatorContributions = new HashMap<>();
                
                if (indicatorWeights != null && !indicatorWeights.isEmpty()) {
                    timeframeScore = calculateTimeframeScoreWithWeights(analysisResult, indicatorWeights, indicatorContributions);
                } else {
                    timeframeScore = calculateTimeframeScore(analysisResult); // Equal weights
                    // For equal weights, we can still calculate contributions
                    calculateEqualWeightContributions(analysisResult, indicatorContributions);
                }
                
                result.getTimeframeScores().put(timeframe, timeframeScore);
                
                double weightedScore = timeframeScore * weight.getNormalizedWeight();
                result.getWeightedScores().put(timeframe, weightedScore);
                totalWeightedScore += weightedScore;
                calculatedTimeframes++;
                
                // Store indicator contributions
                result.getIndicatorContributions().put(timeframe, indicatorContributions);
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
     * Calculate score for a single timeframe using indicator weights
     */
    private static double calculateTimeframeScoreWithWeights(AnalysisResult analysisResult, 
                                                            Object indicatorWeightsObj,
                                                            Map<String, Double> indicatorContributions) {
        
        if (analysisResult == null || indicatorWeightsObj == null) {
            return calculateTimeframeScore(analysisResult); // Fallback to equal weights
        }
        
        try {
            Map<String, Integer> indicatorWeights;
            
            // Handle different weight object types
            if (indicatorWeightsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Integer> weightsMap = (Map<String, Integer>) indicatorWeightsObj;
                indicatorWeights = weightsMap;
            } else if (indicatorWeightsObj instanceof String) {
                // Legacy string format - parse it
                indicatorWeights = parseWeightsStringToMap((String) indicatorWeightsObj);
            } else {
                return calculateTimeframeScore(analysisResult); // Fallback
            }
            
            // Normalize indicator weights
            Map<String, Double> normalizedWeights = normalizeIndicatorWeights(indicatorWeights);
            
            double totalWeightedScore = 0.0;
            int calculatedIndicators = 0;
            
            for (Map.Entry<String, Double> entry : normalizedWeights.entrySet()) {
                String indicatorKey = entry.getKey();
                double weight = entry.getValue();
                double indicatorScore = getIndicatorScoreByKey(analysisResult, indicatorKey);
                
                if (indicatorScore > 0) {
                    double contribution = indicatorScore * weight;
                    totalWeightedScore += contribution;
                    calculatedIndicators++;
                    
                    // Store individual contribution
                    String displayName = getIndicatorDisplayName(indicatorKey);
                    indicatorContributions.put(displayName, contribution);
                }
            }
            
            return calculatedIndicators > 0 ? totalWeightedScore : 0.0;
            
        } catch (Exception e) {
            System.err.println("Error calculating weighted timeframe score: " + e.getMessage());
            return calculateTimeframeScore(analysisResult); // Fallback to equal weights
        }
    }

    /**
     * Normalize indicator weights from HashMap
     */
    private static Map<String, Double> normalizeIndicatorWeights(Map<String, Integer> indicatorWeights) {
        Map<String, Double> normalized = new HashMap<>();
        
        if (indicatorWeights == null || indicatorWeights.isEmpty()) {
            return normalized;
        }
        
        int totalWeight = indicatorWeights.values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalWeight > 0) {
            for (Map.Entry<String, Integer> entry : indicatorWeights.entrySet()) {
                normalized.put(entry.getKey(), (double) entry.getValue() / totalWeight);
            }
        } else {
            // Equal distribution if total is 0
            double equalWeight = 1.0 / indicatorWeights.size();
            for (String key : indicatorWeights.keySet()) {
                normalized.put(key, equalWeight);
            }
        }
        
        return normalized;
    }
    
    /**
     * Parse weights string like "{1W=25, 1D=25, 4H=25, 1H=25}" to Map
     */
    private static Map<String, Integer> parseWeightsStringToMap(String weightsString) {
        Map<String, Integer> weightsMap = new HashMap<>();
        
        if (weightsString == null || weightsString.trim().isEmpty()) {
            return weightsMap;
        }
        
        try {
            String content = weightsString.trim();
            if (content.startsWith("{")) {
                content = content.substring(1);
            }
            if (content.endsWith("}")) {
                content = content.substring(0, content.length() - 1);
            }
            content = content.trim();
            
            if (content.isEmpty()) {
                return weightsMap;
            }
            
            List<String> entries = splitMapEntries(content);
            
            for (String entry : entries) {
                String[] keyValue = entry.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String valueStr = keyValue[1].trim();
                    
                    try {
                        int value = Integer.parseInt(valueStr);
                        weightsMap.put(key, value);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid weight value: " + valueStr + " for timeframe: " + key);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing weights string: " + weightsString + " - " + e.getMessage());
        }
        
        return weightsMap;
    }
    
    
    /**
     * Enhanced version with better error handling and debugging
     */
    private static List<String> splitMapEntries(String content) {
        List<String> entries = new ArrayList<>();
        
        if (content == null || content.trim().isEmpty()) {
            return entries;
        }
        
        StringBuilder currentEntry = new StringBuilder();
        int parenDepth = 0;
        int bracketDepth = 0;
        int braceDepth = 0;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            // Track nesting levels for different bracket types
            switch (c) {
                case '(': parenDepth++; break;
                case ')': parenDepth--; break;
                case '[': bracketDepth++; break;
                case ']': bracketDepth--; break;
                case '{': braceDepth++; break;
                case '}': braceDepth--; break;
            }
            
            // Check for negative nesting (syntax error)
            if (parenDepth < 0 || bracketDepth < 0 || braceDepth < 0) {
                System.err.println("Warning: Unmatched closing bracket at position " + i + " in: " + content);
            }
            
            // Check if this comma is at top level (not inside any brackets)
            if (c == ',' && parenDepth == 0 && bracketDepth == 0 && braceDepth == 0) {
                String entry = currentEntry.toString().trim();
                if (!entry.isEmpty()) {
                    entries.add(entry);
                    System.out.println("Found entry: '" + entry + "'");
                } else {
                    System.err.println("Warning: Empty entry found at position " + i);
                }
                currentEntry = new StringBuilder();
            } else {
                currentEntry.append(c);
            }
        }
        
        // Add the last entry
        String lastEntry = currentEntry.toString().trim();
        if (!lastEntry.isEmpty()) {
            entries.add(lastEntry);
            System.out.println("Found last entry: '" + lastEntry + "'");
        }
        
        // Validate that we're back at top level
        if (parenDepth != 0 || bracketDepth != 0 || braceDepth != 0) {
            System.err.println("Warning: Unmatched brackets in content: " + content);
        }
        
        System.out.println("Successfully split '" + content + "' into " + entries.size() + " entries: " + entries);
        return entries;
    }
    
    
    
    /**
     * Calculate equal weight contributions for display purposes
     */
    private static void calculateEqualWeightContributions(AnalysisResult analysisResult, 
                                                         Map<String, Double> indicatorContributions) {
        if (analysisResult == null) return;
        
        List<Double> scores = new ArrayList<>();
        List<String> indicatorNames = new ArrayList<>();
        
        // Collect all available indicator Z-scores
        if (!Double.isNaN(analysisResult.getTrendZscore()) && analysisResult.getTrendZscore() > 0) {
            scores.add(analysisResult.getTrendZscore());
            indicatorNames.add("Trend");
        }
        if (!Double.isNaN(analysisResult.getMacdZscore()) && analysisResult.getMacdZscore() > 0) {
            scores.add(analysisResult.getMacdZscore());
            indicatorNames.add("MACD");
        }
        if (!Double.isNaN(analysisResult.getMacd359Zscore()) && analysisResult.getMacd359Zscore() > 0) {
            scores.add(analysisResult.getMacd359Zscore());
            indicatorNames.add("MACD(5,8,9)");
        }
        if (!Double.isNaN(analysisResult.getRsiZscore()) && analysisResult.getRsiZscore() > 0) {
            scores.add(analysisResult.getRsiZscore());
            indicatorNames.add("RSI");
        }
        if (!Double.isNaN(analysisResult.getPsarZscore()) && analysisResult.getPsarZscore() > 0) {
            scores.add(analysisResult.getPsarZscore());
            indicatorNames.add("PSAR");
        }
        if (!Double.isNaN(analysisResult.getPsar001Zscore()) && analysisResult.getPsar001Zscore() > 0) {
            scores.add(analysisResult.getPsar001Zscore());
            indicatorNames.add("PSAR(0.01)");
        }
        if (!Double.isNaN(analysisResult.getPsar005Zscore()) && analysisResult.getPsar005Zscore() > 0) {
            scores.add(analysisResult.getPsar005Zscore());
            indicatorNames.add("PSAR(0.05)");
        }
        if (!Double.isNaN(analysisResult.getVwapZscore()) && analysisResult.getVwapZscore() > 0) {
            scores.add(analysisResult.getVwapZscore());
            indicatorNames.add("VWAP");
        }
        if (!Double.isNaN(analysisResult.getVolume20Zscore()) && analysisResult.getVolume20Zscore() > 0) {
            scores.add(analysisResult.getVolume20Zscore());
            indicatorNames.add("Volume20MA");
        }
        if (!Double.isNaN(analysisResult.getHeikenAshiZscore()) && analysisResult.getHeikenAshiZscore() > 0) {
            scores.add(analysisResult.getHeikenAshiZscore());
            indicatorNames.add("HeikenAshi");
        }
        if (!Double.isNaN(analysisResult.getHighestCloseOpenZscore()) && analysisResult.getHighestCloseOpenZscore() > 0) {
            scores.add(analysisResult.getHighestCloseOpenZscore());
            indicatorNames.add("HighestCloseOpen");
        }
        
        if (scores.isEmpty()) return;
        
        double equalWeight = 1.0 / scores.size();
        for (int i = 0; i < scores.size(); i++) {
            double contribution = scores.get(i) * equalWeight;
            indicatorContributions.put(indicatorNames.get(i), contribution);
        }
    }    
    
    
    /**
     * Get indicator score by key (supports both standard and custom indicators)
     */
    private static double getIndicatorScoreByKey(AnalysisResult analysisResult, String indicatorKey) {
        if (analysisResult == null) return 0.0;
        
        if (indicatorKey.startsWith("STANDARD_")) {
            String indicatorName = indicatorKey.substring(9); // Remove "STANDARD_" prefix
            return getStandardIndicatorScore(analysisResult, indicatorName);
        } else if (indicatorKey.startsWith("CUSTOM_")) {
            String customIndicatorName = indicatorKey.substring(7); // Remove "CUSTOM_" prefix
            return getCustomIndicatorScore(analysisResult, customIndicatorName);
        }
        
        return 0.0;
    }
    
    /**
     * Get display name for indicator key
     */
    private static String getIndicatorDisplayName(String indicatorKey) {
        if (indicatorKey.startsWith("STANDARD_")) {
            return indicatorKey.substring(9);
        } else if (indicatorKey.startsWith("CUSTOM_")) {
            return indicatorKey.substring(7) + " (Custom)";
        }
        return indicatorKey;
    }
    
    /**
     * Get score for standard indicators
     */
    private static double getStandardIndicatorScore(AnalysisResult analysisResult, String indicatorName) {
        switch (indicatorName.toUpperCase()) {
            case "MACD": return !Double.isNaN(analysisResult.getMacdZscore()) ? analysisResult.getMacdZscore() : 0.0;
            case "MACD(5,8,9)": return !Double.isNaN(analysisResult.getMacd359Zscore()) ? analysisResult.getMacd359Zscore() : 0.0;
            case "RSI": return !Double.isNaN(analysisResult.getRsiZscore()) ? analysisResult.getRsiZscore() : 0.0;
            case "PSAR(0.01)": return !Double.isNaN(analysisResult.getPsar001Zscore()) ? analysisResult.getPsar001Zscore() : 0.0;
            case "PSAR(0.05)": return !Double.isNaN(analysisResult.getPsar005Zscore()) ? analysisResult.getPsar005Zscore() : 0.0;
            case "VWAP": return !Double.isNaN(analysisResult.getVwapZscore()) ? analysisResult.getVwapZscore() : 0.0;
            case "VOLUME20MA": return !Double.isNaN(analysisResult.getVolume20Zscore()) ? analysisResult.getVolume20Zscore() : 0.0;
            case "HEIKENASHI": return !Double.isNaN(analysisResult.getHeikenAshiZscore()) ? analysisResult.getHeikenAshiZscore() : 0.0;
            case "HIGHESTCLOSEOPEN": return !Double.isNaN(analysisResult.getHighestCloseOpenZscore()) ? analysisResult.getHighestCloseOpenZscore() : 0.0;
            case "TREND": return !Double.isNaN(analysisResult.getTrendZscore()) ? analysisResult.getTrendZscore() : 0.0;
            default: return 0.0;
        }
    }
    
    /**
     * Get score for custom indicators
     */
    private static double getCustomIndicatorScore(AnalysisResult analysisResult, String customIndicatorName) {
        // This would depend on how you store custom indicator scores
        // You might need to add a method to AnalysisResult to get custom indicator Z-scores
        Object customValue = analysisResult.getCustomValue("ZSCORE_" + customIndicatorName);
        if (customValue instanceof Number) {
            return ((Number) customValue).doubleValue();
        }
        
        // Alternative: check custom indicator values map
        String customScore = analysisResult.getCustomIndicatorValue(customIndicatorName);
        if (customScore != null && !customScore.equals("N/A")) {
            try {
                return Double.parseDouble(customScore);
            } catch (NumberFormatException e) {
                // If it's not a number, try to parse trend status
                return parseTrendStatusToScore(customScore);
            }
        }
        
        return 0.0;
    }
    
    /**
     * Parse trend status strings to numerical scores
     */
    private static double parseTrendStatusToScore(String trendStatus) {
        if (trendStatus == null) return 0.0;
        
        switch (trendStatus.toLowerCase()) {
            case "bullish": case "uptrend": case "strong uptrend": 
                return 80.0;
            case "bearish": case "downtrend": 
                return 20.0;
            case "neutral": case "ranging":
                return 50.0;
            case "very strong": case "exceptional":
                return 95.0;
            case "strong":
                return 85.0;
            case "good":
                return 70.0;
            case "moderate":
                return 60.0;
            case "fair":
                return 45.0;
            case "weak":
                return 30.0;
            case "very weak":
                return 15.0;
            default:
                return 50.0; // Default neutral score
        }
    }
    
    /**
     * Calculate score for a single timeframe (original equal-weight method)
     */
    private static double calculateTimeframeScore(AnalysisResult analysisResult) {
        if (analysisResult == null) {
            return 0.0;
        }
        
        List<Double> indicatorScores = new ArrayList<>();
        
        // Collect all available indicator Z-scores
        if (!Double.isNaN(analysisResult.getTrendZscore()) && analysisResult.getTrendZscore() > 0) {
            indicatorScores.add(analysisResult.getTrendZscore());
        }
        if (!Double.isNaN(analysisResult.getMacdZscore()) && analysisResult.getMacdZscore() > 0) {
            indicatorScores.add(analysisResult.getMacdZscore());
        }
        if (!Double.isNaN(analysisResult.getMacd359Zscore()) && analysisResult.getMacd359Zscore() > 0) {
            indicatorScores.add(analysisResult.getMacd359Zscore());
        }
        if (!Double.isNaN(analysisResult.getRsiZscore()) && analysisResult.getRsiZscore() > 0) {
            indicatorScores.add(analysisResult.getRsiZscore());
        }
        if (!Double.isNaN(analysisResult.getPsarZscore()) && analysisResult.getPsarZscore() > 0) {
            indicatorScores.add(analysisResult.getPsarZscore());
        }
        if (!Double.isNaN(analysisResult.getPsar001Zscore()) && analysisResult.getPsar001Zscore() > 0) {
            indicatorScores.add(analysisResult.getPsar001Zscore());
        }
        if (!Double.isNaN(analysisResult.getPsar005Zscore()) && analysisResult.getPsar005Zscore() > 0) {
            indicatorScores.add(analysisResult.getPsar005Zscore());
        }
        if (!Double.isNaN(analysisResult.getVwapZscore()) && analysisResult.getVwapZscore() > 0) {
            indicatorScores.add(analysisResult.getVwapZscore());
        }
        if (!Double.isNaN(analysisResult.getVolume20Zscore()) && analysisResult.getVolume20Zscore() > 0) {
            indicatorScores.add(analysisResult.getVolume20Zscore());
        }
        if (!Double.isNaN(analysisResult.getHeikenAshiZscore()) && analysisResult.getHeikenAshiZscore() > 0) {
            indicatorScores.add(analysisResult.getHeikenAshiZscore());
        }
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
     * Validate that indicator weights sum to 100 for a timeframe
     */
    public static boolean validateIndicatorWeights(Map<String, Integer> indicatorWeights) {
        if (indicatorWeights == null || indicatorWeights.isEmpty()) {
            return false;
        }
        
        int totalWeight = indicatorWeights.values().stream().mapToInt(Integer::intValue).sum();
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
    public static ZScoreCalculator.ZScoreResult getBestStrategy(Map<String, ZScoreCalculator.ZScoreResult> strategyScores) {
        if (strategyScores == null || strategyScores.isEmpty()) {
            return null;
        }
        
        return strategyScores.values().stream()
                .max(Comparator.comparingDouble(ZScoreCalculator.ZScoreResult::getOverallScore))
                .orElse(null);
    }
    
    /**
     * Get the worst performing strategy
     */
    public static ZScoreCalculator.ZScoreResult getWorstStrategy(Map<String, ZScoreCalculator.ZScoreResult> strategyScores) {
        if (strategyScores == null || strategyScores.isEmpty()) {
            return null;
        }
        
        return strategyScores.values().stream()
                .min(Comparator.comparingDouble(ZScoreCalculator.ZScoreResult::getOverallScore))
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
     * Generate a summary report for all strategies (MISSING METHOD ADDED)
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