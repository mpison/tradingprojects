package com.quantlabs.stockApp.indicator.management;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CustomIndicator {
    private String name;
    private String type; // MACD, PSAR, RSI, TREND, VOLUMEMA, MOVINGAVERAGE, HIGHESTCLOSEOPEN
    private Map<String, Object> parameters;
    private String displayName;
    
    public CustomIndicator(String name, String type) {
        this.name = name;
        this.type = type;
        this.parameters = new HashMap<>();
        this.displayName = name;
    }
    
    public CustomIndicator(String name, String type, Map<String, Object> parameters) {
        this.name = name;
        this.type = type;
        this.parameters = new HashMap<>(parameters);
        this.displayName = generateDisplayName();
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { 
        this.parameters = parameters;
        this.displayName = generateDisplayName();
    }
    
    public String getDisplayName() { 
        if (displayName == null) {
            displayName = generateDisplayName();
        }
        return displayName;
    }
    
    public String generateGenericName() {
        switch (type.toUpperCase()) {
            case "MACD":
                int fast = (int) parameters.getOrDefault("fast", 12);
                int slow = (int) parameters.getOrDefault("slow", 26);
                int signal = (int) parameters.getOrDefault("signal", 9);
                return String.format("MACD(%d,%d,%d)", fast, slow, signal);
                
            case "PSAR":
                double step = (double) parameters.getOrDefault("step", 0.02);
                double max = (double) parameters.getOrDefault("max", 0.2);
                return String.format("PSAR(%.2f)", step, name);
                
            case "RSI":
                int period = (int) parameters.getOrDefault("period", 14);
                return String.format("RSI(%d)", period, name);
                
            case "TREND":
                int sma1 = (int) parameters.getOrDefault("sma1", 9);
                int sma2 = (int) parameters.getOrDefault("sma2", 20);
                int sma3 = (int) parameters.getOrDefault("sma3", 200);
                return String.format("TREND(%d,%d,%d)", sma1, sma2, sma3, name);
                
            case "VOLUMEMA":
                int volumePeriod = (int) parameters.getOrDefault("period", 20);
                return String.format("VOLUMEMA(%d)", volumePeriod , name);
                
            case "MOVINGAVERAGE":
                int maPeriod = (int) parameters.getOrDefault("period", 20);
                String maType = (String) parameters.getOrDefault("type", "SMA");
                return String.format("%s(%d)", maType, maPeriod, name);
                
            case "HIGHESTCLOSEOPEN":
                String timeRange = (String) parameters.getOrDefault("timeRange", "1D");
                String session = (String) parameters.getOrDefault("session", "all");
                int lookback = (int) parameters.getOrDefault("lookback", 5);
                return String.format("HIGHESTCLOSEOPEN(%s,%s,%d)", timeRange, session, lookback, name);
            case "MOVINGAVERAGETARGETVALUE":
            	
           	 	int ma1Period = (int) parameters.getOrDefault("ma1Period", 20);
                int ma2Period = (int) parameters.getOrDefault("ma2Period", 50);
                //String ma1PriceType = getStringParameter(parameters, "ma1PriceType", "CLOSE");
                //String ma2PriceType = getStringParameter(parameters, "ma2PriceType", "CLOSE");
                String ma1Type = (String) parameters.getOrDefault("ma1Type", "SMA");
                String ma2Type = (String) parameters.getOrDefault("ma2Type", "SMA");
                //String trendSource = getStringParameter(parameters, "trendSource", "PSAR_0.01");
                
               return String.format("MOVINGAVERAGETARGETVALUE(%s%d,%s%d)", ma1Type, ma1Period, ma2Type, ma2Period, name);    
            default:
                return name;
        }
    }
    
    private String generateDisplayName() {
        switch (type.toUpperCase()) {
            case "MACD":
                int fast = (int) parameters.getOrDefault("fast", 12);
                int slow = (int) parameters.getOrDefault("slow", 26);
                int signal = (int) parameters.getOrDefault("signal", 9);
                return String.format("MACD(%d,%d,%d) - %s", fast, slow, signal, name);
                
            case "PSAR":
                double step = (double) parameters.getOrDefault("step", 0.02);
                double max = (double) parameters.getOrDefault("max", 0.2);
                return String.format("PSAR(%.3f) - %s", step, name);
                
            case "RSI":
                int period = (int) parameters.getOrDefault("period", 14);
                return String.format("RSI(%d) - %s", period, name);
                
            case "TREND":
                int sma1 = (int) parameters.getOrDefault("sma1", 9);
                int sma2 = (int) parameters.getOrDefault("sma2", 20);
                int sma3 = (int) parameters.getOrDefault("sma3", 200);
                return String.format("TREND(%d,%d,%d) - %s", sma1, sma2, sma3, name);
                
            case "VOLUMEMA":
                int volumePeriod = (int) parameters.getOrDefault("period", 20);
                return String.format("VOLUMEMA(%d) - %s", volumePeriod , name);
                
            case "MOVINGAVERAGE":
                int maPeriod = (int) parameters.getOrDefault("period", 20);
                String maType = (String) parameters.getOrDefault("type", "SMA");
                return String.format("%s(%d) - %s", maType, maPeriod, name);
                
            case "HIGHESTCLOSEOPEN":
                String timeRange = (String) parameters.getOrDefault("timeRange", "1D");
                String session = (String) parameters.getOrDefault("session", "all");
                int lookback = (int) parameters.getOrDefault("lookback", 5);
                return String.format("HIGHESTCLOSEOPEN(%s,%s,%d) - %s", timeRange, session, lookback, name);
                
            case "MOVINGAVERAGETARGETVALUE":
            	
            	 int ma1Period = (int) parameters.getOrDefault("ma1Period", 20);
                 int ma2Period = (int) parameters.getOrDefault("ma2Period", 50);
                 //String ma1PriceType = getStringParameter(parameters, "ma1PriceType", "CLOSE");
                 //String ma2PriceType = getStringParameter(parameters, "ma2PriceType", "CLOSE");
                 String ma1Type = (String) parameters.getOrDefault("ma1Type", "SMA");
                 String ma2Type = (String) parameters.getOrDefault("ma2Type", "SMA");
                 //String trendSource = getStringParameter(parameters, "trendSource", "PSAR_0.01");
                 
                return String.format("MOVINGAVERAGETARGETVALUE(%s%d,%s%d) - %s", ma1Type, ma1Period, ma2Type, ma2Period, name);
            	
                
            default:            	
            	return String.format("%s - %s", type, name);
        }
    }
    
    public void setParameter(String key, Object value) {
        parameters.put(key, value);
        this.displayName = generateDisplayName();
    }
    
    public Object getParameter(String key) {
        return parameters.get(key);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomIndicator that = (CustomIndicator) o;
        return Objects.equals(name, that.name) && 
               Objects.equals(type, that.type) && 
               Objects.equals(parameters, that.parameters);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, type, parameters);
    }
    
    @Override
    public String toString() {
        return getDisplayName();
    }
    
    // Factory methods for common indicator types
    public static CustomIndicator createMACD(String name, int fast, int slow, int signal) {
        Map<String, Object> params = new HashMap<>();
        params.put("fast", fast);
        params.put("slow", slow);
        params.put("signal", signal);
        return new CustomIndicator(name, "MACD", params);
    }
    
    public static CustomIndicator createPSAR(String name, double step, double max) {
        Map<String, Object> params = new HashMap<>();
        params.put("step", step);
        params.put("max", max);
        return new CustomIndicator(name, "PSAR", params);
    }
    
    public static CustomIndicator createRSI(String name, int period) {
        Map<String, Object> params = new HashMap<>();
        params.put("period", period);
        return new CustomIndicator(name, "RSI", params);
    }
    
    public static CustomIndicator createTrend(String name, int sma1, int sma2, int sma3) {
        Map<String, Object> params = new HashMap<>();
        params.put("sma1", sma1);
        params.put("sma2", sma2);
        params.put("sma3", sma3);
        return new CustomIndicator(name, "TREND", params);
    }
    
    public static CustomIndicator createVolumeMA(String name, int period) {
        Map<String, Object> params = new HashMap<>();
        params.put("period", period);
        return new CustomIndicator(name, "VOLUMEMA", params);
    }
    
    // New factory methods for MovingAverage and HighestCloseOpen
    public static CustomIndicator createMovingAverage(String name, int period, String type) {
        Map<String, Object> params = new HashMap<>();
        params.put("period", period);
        params.put("type", type);
        return new CustomIndicator(name, "MOVINGAVERAGE", params);
    }
    
    public static CustomIndicator createHighestCloseOpen(String name, String timeRange, String session, int lookback) {
        Map<String, Object> params = new HashMap<>();
        params.put("timeRange", timeRange);
        params.put("session", session);
        params.put("lookback", lookback);
        return new CustomIndicator(name, "HIGHESTCLOSEOPEN", params);
    }
}