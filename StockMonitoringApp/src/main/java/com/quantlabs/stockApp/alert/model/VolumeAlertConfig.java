package com.quantlabs.stockApp.alert.model;

import java.util.HashMap;
import java.util.Map;

public class VolumeAlertConfig {
    private boolean enabled = false;
    private boolean volume20MAEnabled = false;
    private boolean onlyBullishSymbols = true; // Default to true for backward compatibility
    private Map<String, VolumeAlertTimeframeConfig> timeframeConfigs = new HashMap<>();
    
    public VolumeAlertConfig() {
        // Initialize with default values
    }
    
    public VolumeAlertConfig(boolean enabled, boolean volume20MAEnabled, boolean onlyBullishSymbols,
                           Map<String, VolumeAlertTimeframeConfig> timeframeConfigs) {
        this.enabled = enabled;
        this.volume20MAEnabled = volume20MAEnabled;
        this.onlyBullishSymbols = onlyBullishSymbols;
        this.timeframeConfigs = timeframeConfigs != null ? 
            new HashMap<>(timeframeConfigs) : new HashMap<>();
    }
    
    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isVolume20MAEnabled() { return volume20MAEnabled; }
    public void setVolume20MAEnabled(boolean volume20MAEnabled) { 
        this.volume20MAEnabled = volume20MAEnabled; 
    }
    
    public boolean isOnlyBullishSymbols() { return onlyBullishSymbols; }
    public void setOnlyBullishSymbols(boolean onlyBullishSymbols) { 
        this.onlyBullishSymbols = onlyBullishSymbols; 
    }
    
    public Map<String, VolumeAlertTimeframeConfig> getTimeframeConfigs() { 
        return new HashMap<>(timeframeConfigs); 
    }
    
    public void setTimeframeConfigs(Map<String, VolumeAlertTimeframeConfig> timeframeConfigs) { 
        this.timeframeConfigs = timeframeConfigs != null ? 
            new HashMap<>(timeframeConfigs) : new HashMap<>();
    }
    
    public VolumeAlertTimeframeConfig getTimeframeConfig(String timeframe) {
        return timeframeConfigs.get(timeframe);
    }
    
    public void setTimeframeConfig(String timeframe, VolumeAlertTimeframeConfig config) {
        if (config != null) {
            timeframeConfigs.put(timeframe, config);
        }
    }
    
    // ADD THIS COPY METHOD
    public VolumeAlertConfig copy() {
        Map<String, VolumeAlertTimeframeConfig> copiedConfigs = new HashMap<>();
        if (timeframeConfigs != null) {
            for (Map.Entry<String, VolumeAlertTimeframeConfig> entry : timeframeConfigs.entrySet()) {
                if (entry.getValue() != null) {
                    copiedConfigs.put(entry.getKey(), entry.getValue().copy());
                }
            }
        }
        return new VolumeAlertConfig(this.enabled, this.volume20MAEnabled, this.onlyBullishSymbols, copiedConfigs);
    }
    
    // Optional: Add validation and utility methods
    public boolean isValid() {
        if (!enabled) return true; // Disabled config is always valid
        
        if (volume20MAEnabled && timeframeConfigs != null) {
            for (VolumeAlertTimeframeConfig timeframeConfig : timeframeConfigs.values()) {
                if (timeframeConfig != null && timeframeConfig.isEnabled() && !timeframeConfig.isValid()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public boolean hasEnabledTimeframes() {
        if (timeframeConfigs == null) return false;
        return timeframeConfigs.values().stream()
            .anyMatch(config -> config != null && config.isEnabled());
    }
}