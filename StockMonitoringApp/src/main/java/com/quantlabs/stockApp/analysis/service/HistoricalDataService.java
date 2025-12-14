package com.quantlabs.stockApp.analysis.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class HistoricalDataService {
    
    private final Map<String, List<Double>> historicalData = new HashMap<>();
    private final int maxHistoricalPoints = 100; // Keep last 100 data points per metric
    
    public void addDataPoint(String metricFullName, double value) {
        historicalData.computeIfAbsent(metricFullName, k -> new ArrayList<>()).add(value);
        
        // Trim to keep only the most recent data
        List<Double> data = historicalData.get(metricFullName);
        if (data.size() > maxHistoricalPoints) {
            data = data.subList(data.size() - maxHistoricalPoints, data.size());
            historicalData.put(metricFullName, data);
        }
    }
    
    public void addPriceData(PriceData priceData) { 
        // Use reflection to dynamically add all numeric metrics from PriceData
        Method[] methods = PriceData.class.getMethods();
        
        for (Method method : methods) {
            String methodName = method.getName();
            
            // Check if it's a getter method (starts with "get", not getClass, not getResults)
            if (methodName.startsWith("get") && methodName.length() > 3 && 
                !methodName.equals("getClass") && !methodName.equals("getResults")) {
                
                try {
                    // Invoke the getter method
                    Object value = method.invoke(priceData);
                    
                    // Only process numeric values (Double, double, Integer, int, Long, long, Float, float)
                    if (value instanceof Number) {
                        String metricName = methodName.substring(3);
                        metricName = Character.toLowerCase(metricName.charAt(0)) + metricName.substring(1);
                        
                        addDataPoint(metricName, ((Number) value).doubleValue());
                    }
                } catch (Exception e) {
                    // Skip methods that can't be invoked or have issues
                    //System.out.println("Warning: Could not process method " + methodName + ": " + e.getMessage());
                }
            }
        }
        
        // Add metrics from results map with timeframe prefixes
        addAnalysisResultMetrics(priceData);
    }

    private void addAnalysisResultMetrics(PriceData priceData) {
        try {
            Map<String, AnalysisResult> results = priceData.getResults();
            if (results == null || results.isEmpty()) {
                return;
            }
            
            // Get all available timeframes
            String[] timeframes = AnalysisMetricService.getAllTimeframes();
            
            for (String timeframe : timeframes) {
                AnalysisResult result = results.get(timeframe);
                if (result != null) {
                    addAnalysisResultMetricsForTimeframe(result, timeframe);
                }
            }
        } catch (Exception e) {
            //System.out.println("Warning: Could not process AnalysisResult metrics: " + e.getMessage());
        }
    }

    private void addAnalysisResultMetricsForTimeframe(AnalysisResult result, String timeframe) {
        Method[] methods = AnalysisResult.class.getMethods();
        
        for (Method method : methods) {
            String methodName = method.getName();
            
            // Check if it's a getter method (starts with "get", not getClass, not getCustomValues)
            if (methodName.startsWith("get") && methodName.length() > 3 && 
                !methodName.equals("getClass") && !methodName.equals("getCustomValues")) {
                
                try {
                    // Invoke the getter method
                    Object value = method.invoke(result);
                    
                    // Only process numeric values
                    if (value instanceof Number) {
                        String metricName = methodName.substring(3);
                        metricName = Character.toLowerCase(metricName.charAt(0)) + metricName.substring(1);
                        
                        String fullMetricName = timeframe + "_" + metricName;
                        addDataPoint(fullMetricName, ((Number) value).doubleValue());
                    }
                } catch (Exception e) {
                    // Skip methods that can't be invoked or have issues
                    //System.out.println("Warning: Could not process AnalysisResult method " + methodName + ": " + e.getMessage());
                }
            }
        }
        
        // Also process custom values if they exist
        try {
            Map<String, Object> customValues = result.getCustomValues();
            if (customValues != null) {
                for (Map.Entry<String, Object> entry : customValues.entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        String fullMetricName = timeframe + "_" + entry.getKey();
                        addDataPoint(fullMetricName, ((Number) entry.getValue()).doubleValue());
                    }
                }
            }
        } catch (Exception e) {
            //System.out.println("Warning: Could not process custom values: " + e.getMessage());
        }
    }
    
    private void addMetricValue(PriceData priceData, String metricName, Double value) {
        if (value != null) {
            addDataPoint(metricName, value);
        }
    }
    
    private void addMetricValue(PriceData priceData, String metricName, Long value) {
        if (value != null) {
            addDataPoint(metricName, value.doubleValue());
        }
    }
    
    private void addMetricValue(PriceData priceData, String metricName, Integer value) {
        if (value != null) {
            addDataPoint(metricName, value.doubleValue());
        }
    }
    
    public double calculateHistoricalMean(String metricFullName) {
        List<Double> data = historicalData.get(metricFullName);
        if (data == null || data.isEmpty()) {
            return getDefaultMean(metricFullName);
        }
        
        double sum = 0.0;
        for (double value : data) {
            sum += value;
        }
        return sum / data.size();
    }
    
    public double calculateHistoricalStdDev(String metricFullName) {
        List<Double> data = historicalData.get(metricFullName);
        if (data == null || data.size() < 2) {
            return getDefaultStdDev(metricFullName);
        }
        
        double mean = calculateHistoricalMean(metricFullName);
        double sumSquaredDifferences = 0.0;
        
        for (double value : data) {
            sumSquaredDifferences += Math.pow(value - mean, 2);
        }
        
        // Use sample standard deviation (n-1)
        return Math.sqrt(sumSquaredDifferences / (data.size() - 1));
    }
    
    private double getDefaultMean(String metricFullName) {
        // Default values as fallback
        if (metricFullName.contains("price") || metricFullName.contains("close") || 
            metricFullName.contains("open") || metricFullName.contains("high") || 
            metricFullName.contains("low")) {
            return 100.0;
        } else if (metricFullName.contains("currentVolume") || metricFullName.contains("vol")) {
            return 1000000.0;
        } else if (metricFullName.contains("rsi")) {
            return 50.0;
        } else if (metricFullName.contains("change") || metricFullName.contains("percent")) {
            return 0.0;
        }
        return 0.0;
    }
    
    private double getDefaultStdDev(String metricFullName) {
        // Default values as fallback
        if (metricFullName.contains("price") || metricFullName.contains("close") || 
            metricFullName.contains("open") || metricFullName.contains("high") || 
            metricFullName.contains("low")) {
            return 50.0;
        } else if (metricFullName.contains("currentVolume") || metricFullName.contains("vol")) {
            return 500000.0;
        } else if (metricFullName.contains("rsi")) {
            return 15.0;
        } else if (metricFullName.contains("change") || metricFullName.contains("percent")) {
            return 0.05;
        }
        return 1.0;
    }
    
    public int getDataPointCount(String metricFullName) {
        List<Double> data = historicalData.get(metricFullName);
        return data != null ? data.size() : 0;
    }
    
    public void clearHistoricalData() {
        historicalData.clear();
    }
}