package com.quantlabs.stockApp.analysis.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class CrossSymbolDataService {
    
    private final Map<String, Map<String, Double>> currentSymbolValues = new HashMap<>();
    private final Map<String, List<Double>> crossSymbolHistorical = new HashMap<>();
    
    public void updateCurrentSymbols(List<PriceData> symbols) {
        currentSymbolValues.clear();
        
        for (PriceData symbol : symbols) {
            String ticker = symbol.getTicker();
            
            // Store all PriceData metrics dynamically using reflection
            storePriceDataMetrics(ticker, symbol);
            
            // Store all AnalysisResult metrics with timeframe prefixes
            storeAnalysisResultMetrics(ticker, symbol);
        }
        
        // Update cross-symbol historical data
        updateCrossSymbolHistoricals();
    }

    private void storePriceDataMetrics(String ticker, PriceData symbol) {
        Method[] methods = PriceData.class.getMethods();
        
        for (Method method : methods) {
            String methodName = method.getName();
            
            // Check if it's a getter method (starts with "get", not getClass, not getResults)
            if (methodName.startsWith("get") && methodName.length() > 3 && 
                !methodName.equals("getClass") && !methodName.equals("getResults")) {
                
                try {
                    // Invoke the getter method
                    Object value = method.invoke(symbol);
                    
                    // Only process numeric values
                    if (value instanceof Number) {
                        String metricName = methodName.substring(3);
                        metricName = Character.toLowerCase(metricName.charAt(0)) + metricName.substring(1);
                        
                        storeValue(ticker, metricName, ((Number) value).doubleValue());
                    }
                } catch (Exception e) {
                    // Skip methods that can't be invoked or have issues
                    System.out.println("Warning: Could not process PriceData method " + methodName + ": " + e.getMessage());
                }
            }
        }
    }

    private void storeAnalysisResultMetrics(String ticker, PriceData symbol) {
        try {
            Map<String, AnalysisResult> results = symbol.getResults();
            if (results == null || results.isEmpty()) {
                return;
            }
            
            // Get all available timeframes
            String[] timeframes = {"1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min"};
            
            for (String timeframe : timeframes) {
                AnalysisResult result = results.get(timeframe);
                if (result != null) {
                    storeAnalysisResultMetricsForTimeframe(ticker, result, timeframe);
                }
            }
        } catch (Exception e) {
            System.out.println("Warning: Could not process AnalysisResult metrics: " + e.getMessage());
        }
    }

    private void storeAnalysisResultMetricsForTimeframe(String ticker, AnalysisResult result, String timeframe) {
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
                        storeValue(ticker, fullMetricName, ((Number) value).doubleValue());
                    }
                } catch (Exception e) {
                    // Skip methods that can't be invoked or have issues
                    System.out.println("Warning: Could not process AnalysisResult method " + methodName + ": " + e.getMessage());
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
                        storeValue(ticker, fullMetricName, ((Number) entry.getValue()).doubleValue());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Warning: Could not process custom values: " + e.getMessage());
        }
    }

    private void storeValue(String ticker, String metric, Double value) {
        if (value != null) {
            currentSymbolValues.computeIfAbsent(metric, k -> new HashMap<>()).put(ticker, value);
        }
    }

    private void updateCrossSymbolHistoricals() {
        for (String metric : currentSymbolValues.keySet()) {
            Map<String, Double> currentValues = currentSymbolValues.get(metric);
            List<Double> values = new ArrayList<>(currentValues.values());
            
            crossSymbolHistorical.put(metric, values);
        }
    }
       
    private void storeValue(String ticker, String metric, Long value) {
        if (value != null) {
            currentSymbolValues.computeIfAbsent(metric, k -> new HashMap<>()).put(ticker, value.doubleValue());
        }
    }   
    
    
    public double calculateCrossSymbolMean(String metric) {
        List<Double> values = crossSymbolHistorical.get(metric);
        if (values == null || values.isEmpty()) return 0.0;
        
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
    
    public double calculateCrossSymbolStdDev(String metric) {
        List<Double> values = crossSymbolHistorical.get(metric);
        if (values == null || values.size() < 2) return 1.0;
        
        double mean = calculateCrossSymbolMean(metric);
        double sumSquaredDiff = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .sum();
        
        return Math.sqrt(sumSquaredDiff / (values.size() - 1));
    }
    
    public double calculatePercentile(String metric, String ticker, boolean lowerIsBetter) { 
        Map<String, Double> values = currentSymbolValues.get(metric);
        if (values == null || !values.containsKey(ticker)) return 50.0;
        
        double currentValue = values.get(ticker);
        List<Double> allValues = new ArrayList<>(values.values());
        Collections.sort(allValues);
        
        // Count values less than current value
        int countLess = 0;
        int countEqual = 0;
        
        for (Double value : allValues) {
            if (value < currentValue) {
                countLess++;
            } else if (value.equals(currentValue)) {
                countEqual++;
            } else {
                break; // Values are sorted, so we can break early
            }
        }
        
        // Calculate percentile using standard formula: 
        // (countLess + 0.5 * countEqual) / totalCount * 100
        double percentile = (countLess + 0.5 * countEqual) / allValues.size() * 100;
        
        return lowerIsBetter ? 100 - percentile : percentile;
    }
    
    public Map<String, Double> getCurrentValues(String metric) {
        return currentSymbolValues.getOrDefault(metric, new HashMap<>());
    }
}