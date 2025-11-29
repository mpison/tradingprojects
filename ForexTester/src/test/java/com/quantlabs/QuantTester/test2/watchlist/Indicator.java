package com.quantlabs.QuantTester.test2.watchlist;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Objects;

import java.util.Arrays;  // Add this import for deep hashCode calculation

public class Indicator {
    private String type;
    private String timeframe;
    private int shift;
    private Map<String, Object> params;
    
    public Indicator(String type, String timeframe, int shift, Map<String, Object> params) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Indicator type cannot be empty");
        }
        
        // Additional validation for Specific Volume
        if (type.equals("Specific Volume")) {
            if (!params.containsKey("comparison") || !params.containsKey("volumeValue")) {
                throw new IllegalArgumentException("Specific Volume requires comparison and volumeValue parameters");
            }
        }
        
        this.type = type.trim();
        this.timeframe = timeframe;
        this.shift = shift;
        this.params = new HashMap<String, Object>(params);
    }
    
    public String getType() { return type; }
    public String getTimeframe() { return timeframe; }
    public int getShift() { return shift; }
    public Map<String, Object> getParams() { return params; }
    
    @Override
    public String toString() {
        return type + " (" + timeframe + ", shift: " + shift + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Indicator indicator = (Indicator) o;
        
        // Traditional null-safe equality checks
        if (shift != indicator.shift) return false;
        if (type == null ? indicator.type != null : !type.equals(indicator.type)) return false;
        if (timeframe == null ? indicator.timeframe != null : !timeframe.equals(indicator.timeframe)) return false;
        if (params == null ? indicator.params != null : !params.equals(indicator.params)) return false;
        
        return true;
    }

    @Override
    public int hashCode() {
        // Traditional hashCode calculation
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (timeframe != null ? timeframe.hashCode() : 0);
        result = 31 * result + shift;
        result = 31 * result + (params != null ? params.hashCode() : 0);
        return result;
    }
}
