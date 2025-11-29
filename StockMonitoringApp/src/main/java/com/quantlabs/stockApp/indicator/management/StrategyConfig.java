package com.quantlabs.stockApp.indicator.management;

import java.util.*;

public class StrategyConfig {
    private String name;
    private boolean enabled;
    private boolean alarmEnabled;
    private Map<String, Set<String>> timeframeIndicators;
    private Map<String, Set<CustomIndicator>> timeframeCustomIndicators;
    private Set<String> exclusiveSymbols;
    private boolean symbolExclusive;
    private Map<String, Object> parameters;
    private Map<String, Map<String, Object>> strategyParameters;
    
    // New field for optional watchlist
    private String watchlistName;
    
    public StrategyConfig() {
        this.timeframeIndicators = new HashMap<>();
        this.timeframeCustomIndicators = new HashMap<>();
        this.exclusiveSymbols = new HashSet<>();
        this.parameters = new HashMap<>();
        this.strategyParameters = new HashMap<>();
        this.enabled = true;
        this.symbolExclusive = false;
        this.alarmEnabled = false;
        this.watchlistName = null; // Optional - null means no watchlist
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isAlarmEnabled() { return alarmEnabled; }
    public void setAlarmEnabled(boolean alarmEnabled) { this.alarmEnabled = alarmEnabled; }
    
    public Map<String, Set<String>> getTimeframeIndicators() { return timeframeIndicators; }
    public void setTimeframeIndicators(Map<String, Set<String>> timeframeIndicators) { 
        this.timeframeIndicators = timeframeIndicators; 
    }
    
    public Map<String, Set<CustomIndicator>> getTimeframeCustomIndicators() { return timeframeCustomIndicators; }
    public void setTimeframeCustomIndicators(Map<String, Set<CustomIndicator>> timeframeCustomIndicators) { 
        this.timeframeCustomIndicators = timeframeCustomIndicators; 
    }
    
    public Set<String> getExclusiveSymbols() { return exclusiveSymbols; }
    public void setExclusiveSymbols(Set<String> exclusiveSymbols) { this.exclusiveSymbols = exclusiveSymbols; }
    
    public boolean isSymbolExclusive() { return symbolExclusive; }
    public void setSymbolExclusive(boolean symbolExclusive) { this.symbolExclusive = symbolExclusive; }
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    
    // Strategy-specific parameters
    public Map<String, Map<String, Object>> getStrategyParameters() { return strategyParameters; }
    public void setStrategyParameters(Map<String, Map<String, Object>> strategyParameters) { 
        this.strategyParameters = strategyParameters; 
    }
    
    // Watchlist methods
    public String getWatchlistName() { return watchlistName; }
    public void setWatchlistName(String watchlistName) { this.watchlistName = watchlistName; }
    
    public boolean hasWatchlist() {
        return watchlistName != null && !watchlistName.trim().isEmpty();
    }
    
    public void clearWatchlist() {
        this.watchlistName = null;
    }
    
    public void setParameterForStrategy(String strategyName, String parameterName, Object value) {
        strategyParameters.computeIfAbsent(strategyName, k -> new HashMap<>())
                         .put(parameterName, value);
    }
    
    public Object getParameterForStrategy(String strategyName, String parameterName) {
        Map<String, Object> params = strategyParameters.get(strategyName);
        return params != null ? params.get(parameterName) : null;
    }
    
    public Map<String, Object> getParametersForStrategy(String strategyName) {
        return strategyParameters.getOrDefault(strategyName, new HashMap<>());
    }
    
    // Helper methods for timeframe indicators
    public Set<String> getTimeframes() {
        Set<String> allTimeframes = new HashSet<>();
        allTimeframes.addAll(timeframeIndicators.keySet());
        allTimeframes.addAll(timeframeCustomIndicators.keySet());
        return allTimeframes;
    }
    
    public Set<String> getIndicatorsForTimeframe(String timeframe) {
        return timeframeIndicators.getOrDefault(timeframe, new HashSet<>());
    }
    
    public Set<CustomIndicator> getCustomIndicatorsForTimeframe(String timeframe) {
        return timeframeCustomIndicators.getOrDefault(timeframe, new HashSet<>());
    }
    
    public void setIndicatorsForTimeframe(String timeframe, Set<String> indicators) {
        timeframeIndicators.put(timeframe, indicators);
    }
    
    public void setCustomIndicatorsForTimeframe(String timeframe, Set<CustomIndicator> customIndicators) {
        timeframeCustomIndicators.put(timeframe, customIndicators);
    }
    
    public void addTimeframeWithIndicators(String timeframe, Set<String> indicators) {
        timeframeIndicators.put(timeframe, new HashSet<>(indicators));
    }
    
    public void addTimeframeWithCustomIndicators(String timeframe, Set<CustomIndicator> customIndicators) {
        timeframeCustomIndicators.put(timeframe, new HashSet<>(customIndicators));
    }
    
    public void removeTimeframe(String timeframe) {
        timeframeIndicators.remove(timeframe);
        timeframeCustomIndicators.remove(timeframe);
    }
    
    // Generate a unique key for strategy-timeframe combination
    public String getUniqueKey(String timeframe) {
        return name + "|" + timeframe;
    }
    
    // Generate unique key for all timeframes
    public Set<String> getAllUniqueKeys() {
        Set<String> keys = new HashSet<>();
        for (String timeframe : getTimeframes()) {
            keys.add(getUniqueKey(timeframe));
        }
        return keys;
    }
    
    // Get all indicators used across all timeframes
    public Set<String> getAllIndicators() {
        Set<String> allIndicators = new HashSet<>();
        for (Set<String> indicators : timeframeIndicators.values()) {
            allIndicators.addAll(indicators);
        }
        return allIndicators;
    }
    
    // Get all custom indicators used across all timeframes
    public Set<CustomIndicator> getAllCustomIndicators() {
        Set<CustomIndicator> allCustomIndicators = new HashSet<>();
        for (Set<CustomIndicator> customIndicators : timeframeCustomIndicators.values()) {
            allCustomIndicators.addAll(customIndicators);
        }
        return allCustomIndicators;
    }
    
    // Backward compatibility method - returns all indicators across all timeframes
    public Set<String> getIndicators() {
        return getAllIndicators();
    }
    
    public StrategyConfig duplicate() {
        StrategyConfig duplicate = new StrategyConfig();
        duplicate.setName(this.name);
        duplicate.setEnabled(this.enabled);
        duplicate.setAlarmEnabled(this.alarmEnabled);
        duplicate.setWatchlistName(this.watchlistName); // Copy watchlist
        
        // Deep copy timeframe indicators
        Map<String, Set<String>> copiedTimeframeIndicators = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : this.timeframeIndicators.entrySet()) {
            copiedTimeframeIndicators.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        duplicate.setTimeframeIndicators(copiedTimeframeIndicators);
        
        // Deep copy custom indicators
        Map<String, Set<CustomIndicator>> copiedTimeframeCustomIndicators = new HashMap<>();
        for (Map.Entry<String, Set<CustomIndicator>> entry : this.timeframeCustomIndicators.entrySet()) {
            copiedTimeframeCustomIndicators.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        duplicate.setTimeframeCustomIndicators(copiedTimeframeCustomIndicators);
        
        // Deep copy strategy parameters
        Map<String, Map<String, Object>> copiedStrategyParameters = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : this.strategyParameters.entrySet()) {
            copiedStrategyParameters.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        duplicate.setStrategyParameters(copiedStrategyParameters);
        
        duplicate.setExclusiveSymbols(new HashSet<>(this.exclusiveSymbols));
        duplicate.setSymbolExclusive(this.symbolExclusive);
        duplicate.setParameters(new HashMap<>(this.parameters));
        return duplicate;
    }
    
    @Override
    public String toString() {
        return String.format("StrategyConfig{name='%s', enabled=%s, alarmEnabled=%s, timeframeIndicators=%s, timeframeCustomIndicators=%s, symbolExclusive=%s, exclusiveSymbols=%s, watchlistName=%s}", 
            name, enabled, alarmEnabled, timeframeIndicators, timeframeCustomIndicators, symbolExclusive, exclusiveSymbols, watchlistName);
    }
}