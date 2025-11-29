package com.quantlabs.stockApp.backtesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StrategyConfig {
    private String type;
    private List<String> timeframes;
    private double weight;
    private Map<String, Object> parameters;
    
    public StrategyConfig() {
        this.timeframes = new ArrayList<>();
        this.parameters = new HashMap<>();
        this.weight = 1.0;
    }
    
    public static class Builder {
        private final StrategyConfig config;
        
        public Builder(String type) {
            config = new StrategyConfig();
            config.type = type;
        }
        
        public Builder timeframe(String timeframe) {
            config.timeframes.add(timeframe);
            return this;
        }
        
        public Builder timeframes(List<String> timeframes) {
            config.timeframes = timeframes;
            return this;
        }
        
        public Builder weight(double weight) {
            config.weight = weight;
            return this;
        }
        
        public Builder parameter(String key, Object value) {
            config.parameters.put(key, value);
            return this;
        }
        
        public StrategyConfig build() {
            return config;
        }
    }
    
    // Getters
    public String getType() { return type; }
    public List<String> getTimeframes() { return timeframes; }
    public double getWeight() { return weight; }
    public Map<String, Object> getParameters() { return parameters; }
}