// [file name]: MonteCarloConfigManager.java
package com.quantlabs.stockApp.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages Monte Carlo graph configurations
 */
public class MonteCarloConfigManager {
    
    public static final String MONTE_CARLO_CONFIG_KEY = "monteCarloConfig";
    
    /**
     * Creates a default Monte Carlo configuration
     */
    public static Map<String, Object> createDefaultConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("version", "1.0");
        config.put("selectedColumns", new ArrayList<String>());
        config.put("graphSettings", createDefaultGraphSettings());
        return config;
    }
    
    private static Map<String, Object> createDefaultGraphSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("simulationCount", 1000);
        settings.put("timeHorizon", 252); // Trading days in a year
        settings.put("confidenceLevel", 0.95);
        settings.put("showPercentiles", true);
        settings.put("showMeanPath", true);
        settings.put("showConfidenceBands", true);
        return settings;
    }
    
    /**
     * Validates Monte Carlo configuration
     */
    public static boolean validateConfig(Map<String, Object> config) {
        if (config == null) return false;
        if (!config.containsKey("version")) return false;
        if (!config.containsKey("selectedColumns")) return false;
        if (!config.containsKey("graphSettings")) return false;
        return true;
    }
    
    /**
     * Merges configuration with default values
     */
    public static Map<String, Object> mergeWithDefaults(Map<String, Object> config) {
        Map<String, Object> defaultConfig = createDefaultConfig();
        
        if (config == null) {
            return defaultConfig;
        }
        
        // Merge settings
        Map<String, Object> merged = new HashMap<>(defaultConfig);
        merged.putAll(config);
        
        // Merge graph settings
        if (config.containsKey("graphSettings")) {
            Map<String, Object> configSettings = (Map<String, Object>) config.get("graphSettings");
            Map<String, Object> defaultSettings = (Map<String, Object>) defaultConfig.get("graphSettings");
            Map<String, Object> mergedSettings = new HashMap<>(defaultSettings);
            mergedSettings.putAll(configSettings);
            merged.put("graphSettings", mergedSettings);
        }
        
        return merged;
    }
}