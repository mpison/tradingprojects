package com.quantlabs.stockApp.indicator.management;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ta4j.core.BarSeries;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.indicator.strategy.AbstractIndicatorStrategy;
import com.quantlabs.stockApp.indicator.strategy.AdvancedVWAPIndicatorStrategy;
import com.quantlabs.stockApp.indicator.strategy.HeikenAshiStrategy;
import com.quantlabs.stockApp.indicator.strategy.HighestCloseOpenStrategy;
import com.quantlabs.stockApp.indicator.strategy.MACDStrategy;
import com.quantlabs.stockApp.indicator.strategy.PSARStrategy;
import com.quantlabs.stockApp.indicator.strategy.RSIStrategy;
import com.quantlabs.stockApp.indicator.strategy.TrendStrategy;
import com.quantlabs.stockApp.indicator.strategy.VolumeStrategyMA;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class StrategyExecutionService {
    private ConsoleLogger logger;
    private Map<String, AbstractIndicatorStrategy> strategyMap;
    
    // Add a map to store analysis results by strategy and timeframe
    private Map<String, Map<String, AnalysisResult>> analysisResults = new HashMap<>();
    
    public StrategyExecutionService(ConsoleLogger logger) {
        this.logger = logger;
        initializeStrategies();
    }
    
    private void initializeStrategies() {
        strategyMap = new HashMap<>();
        
        // Initialize all available strategies
        strategyMap.put("VWAP", new AdvancedVWAPIndicatorStrategy(logger));
        strategyMap.put("MACD", new MACDStrategy(12, 26, 9, "standard", logger));
        strategyMap.put("MACD(5,8,9)", new MACDStrategy(5, 8, 9, "fast", logger));
        strategyMap.put("PSAR(0.01)", new PSARStrategy(0.01, 0.2, "sensitive", logger));
        strategyMap.put("PSAR(0.05)", new PSARStrategy(0.05, 0.2, "standard", logger));
        strategyMap.put("HeikenAshi", new HeikenAshiStrategy(logger));
        strategyMap.put("RSI", new RSIStrategy(logger));
        strategyMap.put("HighestCloseOpen", new HighestCloseOpenStrategy(logger));
        strategyMap.put("Volume20MA", new VolumeStrategyMA(logger));
        strategyMap.put("Trend", new TrendStrategy(logger));
    }
    
    public void importStrategies(Object strategiesObj) {
        if (strategiesObj instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) strategiesObj;
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    Object strategyObj = jsonArray.get(i);
                    if (strategyObj instanceof JSONObject) {
                        JSONObject strategyJson = (JSONObject) strategyObj;
                        AbstractIndicatorStrategy strategy = createStrategyFromJson(strategyJson);
                        if (strategy != null) {
                            String strategyName = strategy.getName();
                            strategyMap.put(strategyName, strategy);
                            logger.log("✅ Imported and added to strategyMap: " + strategyName);
                        }
                    }
                } catch (Exception e) {
                    logger.log("❌ Error importing strategy at index " + i + ": " + e.getMessage());
                }
            }
        } else if (strategiesObj instanceof List) {
            List<?> strategiesList = (List<?>) strategiesObj;
            for (Object strategyObj : strategiesList) {
                try {
                    if (strategyObj instanceof Map) {
                        Map<?, ?> strategyMapData = (Map<?, ?>) strategyObj;
                        AbstractIndicatorStrategy strategy = createStrategyFromMap(strategyMapData);
                        if (strategy != null) {
                            String strategyName = strategy.getName();
                            strategyMap.put(strategyName, strategy);
                            logger.log("✅ Imported and added to strategyMap: " + strategyName);
                        }
                    } else if (strategyObj instanceof AbstractIndicatorStrategy) {
                        // Directly add if it's already an IndicatorStrategy instance
                        AbstractIndicatorStrategy strategy = (AbstractIndicatorStrategy) strategyObj;
                        strategyMap.put(strategy.getName(), strategy);
                        logger.log("✅ Added existing strategy to strategyMap: " + strategy.getName());
                    }
                } catch (Exception e) {
                    logger.log("❌ Error importing strategy: " + e.getMessage());
                }
            }
        }
        
        logger.log("📊 StrategyMap now contains " + strategyMap.size() + " strategies");
    }
    
    // Create strategy from JSON object
    private AbstractIndicatorStrategy createStrategyFromJson(JSONObject strategyJson) {
        try {
            String type = strategyJson.getString("type");
            String name = strategyJson.getString("name");
            
            switch (type.toUpperCase()) {
                case "VWAP":
                    return new AdvancedVWAPIndicatorStrategy(logger);
                    
                case "MACD":
                    int fast = strategyJson.optInt("fastPeriod", 12);
                    int slow = strategyJson.optInt("slowPeriod", 26);
                    int signal = strategyJson.optInt("signalPeriod", 9);
                    String macdType = strategyJson.optString("macdType", "standard");
                    return new MACDStrategy(fast, slow, signal, macdType, logger);
                    
                case "PSAR":
                    double initialAcceleration = strategyJson.optDouble("initialAcceleration", 0.02);
                    double maxAcceleration = strategyJson.optDouble("maxAcceleration", 0.2);
                    String psarType = strategyJson.optString("psarType", "standard");
                    return new PSARStrategy(initialAcceleration, maxAcceleration, psarType, logger);
                    
                case "HEIKENASHI":
                    return new HeikenAshiStrategy(logger);
                    
                case "RSI":
                    return new RSIStrategy(logger);
                    
                case "HIGHESTCLOSEOPEN":
                    return new HighestCloseOpenStrategy(logger);
                    
                case "VOLUME20MA":
                    return new VolumeStrategyMA(logger);
                    
                case "TREND":
                    return new TrendStrategy(logger);
                    
                default:
                    logger.log("⚠️ Unknown strategy type: " + type);
                    return null;
            }
        } catch (Exception e) {
            logger.log("❌ Error creating strategy from JSON: " + e.getMessage());
            return null;
        }
    }
    
    // Create strategy from Map - CORRECTED VERSION
    private AbstractIndicatorStrategy createStrategyFromMap(Map<?, ?> strategyMapData) {
        try {
            String type = getStringFromMap(strategyMapData, "type");
            String name = getStringFromMap(strategyMapData, "name");
            
            if (type == null || name == null) {
                logger.log("❌ Strategy missing type or name: " + strategyMapData);
                return null;
            }
            
            switch (type.toUpperCase()) {
                case "VWAP":
                    return new AdvancedVWAPIndicatorStrategy(logger);
                    
                case "MACD":
                    int fast = getIntFromMap(strategyMapData, "fastPeriod", 12);
                    int slow = getIntFromMap(strategyMapData, "slowPeriod", 26);
                    int signal = getIntFromMap(strategyMapData, "signalPeriod", 9);
                    String macdType = getStringFromMap(strategyMapData, "macdType", "standard");
                    return new MACDStrategy(fast, slow, signal, macdType, logger);
                    
                case "PSAR":
                    double initialAcceleration = getDoubleFromMap(strategyMapData, "initialAcceleration", 0.02);
                    double maxAcceleration = getDoubleFromMap(strategyMapData, "maxAcceleration", 0.2);
                    String psarType = getStringFromMap(strategyMapData, "psarType", "standard");
                    return new PSARStrategy(initialAcceleration, maxAcceleration, psarType, logger);
                    
                case "HEIKENASHI":
                    return new HeikenAshiStrategy(logger);
                    
                case "RSI":
                    return new RSIStrategy(logger);
                    
                case "HIGHESTCLOSEOPEN":
                    return new HighestCloseOpenStrategy(logger);
                    
                case "VOLUME20MA":
                    return new VolumeStrategyMA(logger);
                    
                case "TREND":
                    return new TrendStrategy(logger);
                    
                default:
                    logger.log("⚠️ Unknown strategy type: " + type);
                    return null;
            }
        } catch (Exception e) {
            logger.log("❌ Error creating strategy from Map: " + e.getMessage());
            return null;
        }
    }
    
    // Helper methods for safe map access
    private String getStringFromMap(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    private String getStringFromMap(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private int getIntFromMap(Map<?, ?> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                logger.log("⚠️ Could not parse int from key '" + key + "': " + value);
            }
        }
        return defaultValue;
    }
    
    private double getDoubleFromMap(Map<?, ?> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value != null) {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                logger.log("⚠️ Could not parse double from key '" + key + "': " + value);
            }
        }
        return defaultValue;
    }
    
    private boolean getBooleanFromMap(Map<?, ?> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return defaultValue;
    }
    
    public List<AbstractIndicatorStrategy> getStrategiesForConfig(StrategyConfig config) {
        List<AbstractIndicatorStrategy> strategies = new ArrayList<>();
        
        for (String indicatorName : config.getIndicators()) {
            AbstractIndicatorStrategy strategy = strategyMap.get(indicatorName);
            if (strategy != null) {
                strategies.add(strategy);
            } else {
                logger.log("⚠️ Strategy not found in strategyMap: " + indicatorName);
            }
        }
        
        return strategies;
    }
    
    public void executeStrategy(StrategyConfig config, BarSeries series, String timeframe) {
        if (!config.isEnabled()) {
            logger.log("Strategy '" + config.getName() + "' is disabled. Skipping execution.");
            return;
        }
        
        List<AbstractIndicatorStrategy> strategies = getStrategiesForConfig(config);
        logger.log(String.format("Executing strategy '%s' on timeframe %s with %d indicators", 
            config.getName(), timeframe, strategies.size()));
        
        // Create analysis result
        AnalysisResult result = new AnalysisResult();
        
        // Execute each indicator strategy and calculate Z-Scores
        for (AbstractIndicatorStrategy strategy : strategies) {
            try {
                logger.log(String.format("  - Executing %s indicator", strategy.getName()));
                
                // Calculate the main indicator
                strategy.calculate(series, result, series.getEndIndex());
                
                // Calculate Z-Score for this indicator
                strategy.calculateZscore(series, result, series.getEndIndex());
                
            } catch (Exception e) {
                logger.log("Error executing " + strategy.getName() + ": " + e.getMessage());
            }
        }
        
        // Store result in appropriate location
        storeAnalysisResult(config.getName(), timeframe, result);
    }
    
    /**
     * Store analysis result for a strategy and timeframe
     */
    private void storeAnalysisResult(String strategyName, String timeframe, AnalysisResult result) {
        analysisResults.computeIfAbsent(strategyName, k -> new HashMap<>())
                      .put(timeframe, result);
        
        logger.log(String.format("Stored analysis result for %s on %s timeframe", 
            strategyName, timeframe));
    }
    
    /**
     * Get analysis result for a specific strategy and timeframe
     */
    public AnalysisResult getAnalysisResult(String strategyName, String timeframe) {
        Map<String, AnalysisResult> strategyResults = analysisResults.get(strategyName);
        return strategyResults != null ? strategyResults.get(timeframe) : null;
    }
    
    /**
     * Get all analysis results for a strategy
     */
    public Map<String, AnalysisResult> getAnalysisResultsForStrategy(String strategyName) {
        return analysisResults.getOrDefault(strategyName, new HashMap<>());
    }
    
    /**
     * Get all analysis results across all strategies and timeframes
     */
    public Map<String, Map<String, AnalysisResult>> getAllAnalysisResults() {
        return new HashMap<>(analysisResults);
    }
    
    /**
     * Clear all stored analysis results
     */
    public void clearAnalysisResults() {
        analysisResults.clear();
        logger.log("Cleared all analysis results");
    }
    
    public Map<String, AbstractIndicatorStrategy> getStrategyMap() {
        return Collections.unmodifiableMap(strategyMap);
    }
    
    // Helper method to get strategy by name
    public AbstractIndicatorStrategy getStrategy(String name) {
        return strategyMap.get(name);
    }
    
    // Helper method to check if strategy exists
    public boolean hasStrategy(String name) {
        return strategyMap.containsKey(name);
    }
    
    // Method to get all strategy names
    public Set<String> getStrategyNames() {
        return strategyMap.keySet();
    }
    
    // Method to clear all strategies (optional)
    public void clearStrategies() {
        int previousSize = strategyMap.size();
        strategyMap.clear();
        logger.log("🧹 Cleared " + previousSize + " strategies from strategyMap");
    }
}