package com.quantlabs.stockApp.analysis.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.quantlabs.stockApp.analysis.model.AnalysisMetric;
import com.quantlabs.stockApp.analysis.model.ComparisonType;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class AnalysisMetricService {
    
    private final Map<String, AnalysisMetric> metrics = new HashMap<>();
    //private static final String[] allTimeframes = { "1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min" };
    
    
    private static Set<String> allTimeframes = new HashSet<>(
			Arrays.asList("1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min"));
    
    private final HistoricalDataService historicalDataService;
    
    private Map<String, Set<String>> customIndicatorCombinations = new HashMap<>();
    
    
    public AnalysisMetricService(HistoricalDataService historicalDataService) {
    	this.historicalDataService = historicalDataService;
        initializePriceDataMetrics();
        initializeAnalysisResultMetrics();
    }
    
    public AnalysisMetricService(HistoricalDataService historicalDataService, Map<String, Set<String>> customIndicatorCombinations) {
    	this.historicalDataService = historicalDataService;
    	this.customIndicatorCombinations = customIndicatorCombinations;
        initializePriceDataMetrics();
        initializeAnalysisResultMetrics();
    }
    
    protected void initializePriceDataMetrics() {
        Method[] methods = PriceData.class.getMethods();
        
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith("get") && methodName.length() > 3 && 
                !methodName.equals("getClass") && !methodName.equals("getResults")) {
                
                String metricName = methodName.substring(3);
                metricName = Character.toLowerCase(metricName.charAt(0)) + metricName.substring(1);
                
                Class<?> returnType = method.getReturnType();
                if (returnType == Double.class || returnType == double.class || 
                    returnType == Integer.class || returnType == int.class ||
                    returnType == Long.class || returnType == long.class ||
                    returnType == Float.class || returnType == float.class) {
                    
                    double mean = historicalDataService.calculateHistoricalMean(metricName);
                    double stdDev = historicalDataService.calculateHistoricalStdDev(metricName);
                    
                    // FIXED: Use correct constructor
                    AnalysisMetric metric = new AnalysisMetric(metricName, null, mean, stdDev, PriceData.class);
                    
                    // Set comparison type based on metric type
                    if (metricName.toLowerCase().contains("count") || metricName.toLowerCase().contains("breakout")) {
                        metric.setComparisonType(ComparisonType.CROSS_SYMBOL);
                        metric.setLowerIsBetter(true);
                    } else if (metricName.equals("volume") || metricName.equals("currentVolume")) {
                        metric.setComparisonType(ComparisonType.CROSS_SYMBOL);
                        metric.setLowerIsBetter(false);
                    }
                    
                    addMetric(metric);
                }
            }
        }
    }

    protected void initializeAnalysisResultMetrics() {
        Method[] methods = AnalysisResult.class.getMethods();
        
        for (String timeframe : allTimeframes) {
            for (Method method : methods) {
                String methodName = method.getName();
                if (methodName.startsWith("get") && methodName.length() > 3 && 
                    !methodName.equals("getClass") && !methodName.equals("getCustomValues")) {
                    
                    String metricName = methodName.substring(3);
                    metricName = Character.toLowerCase(metricName.charAt(0)) + metricName.substring(1);
                    
                    Class<?> returnType = method.getReturnType();
                    if (returnType == Double.class || returnType == double.class || 
                        returnType == Integer.class || returnType == int.class ||
                        returnType == Long.class || returnType == long.class ||
                        returnType == Float.class || returnType == float.class) {
                        
                        String fullName = timeframe + "_" + metricName;
                        double mean = historicalDataService.calculateHistoricalMean(fullName);
                        double stdDev = historicalDataService.calculateHistoricalStdDev(fullName);
                        
                        // FIXED: Use correct constructor
                        AnalysisMetric metric = new AnalysisMetric(metricName, timeframe, mean, stdDev, AnalysisResult.class);
                        
                        // Set comparison type for technical indicators
                        if (metricName.contains("Breakout") || metricName.contains("Count")) {
                            metric.setComparisonType(ComparisonType.CROSS_SYMBOL);
                            metric.setLowerIsBetter(true);
                        } else if (metricName.contains("Percentile") || metricName.contains("Target")) {
                            metric.setComparisonType(ComparisonType.CROSS_SYMBOL);
                            metric.setLowerIsBetter(false);
                        }
                        
                        addMetric(metric);
                    }
                }
                
                if(methodName.equals("getCustomValues")) {
                	
                		Set<String> customIndicatorSet = customIndicatorCombinations.get(timeframe);
                		
                		if(customIndicatorSet != null && !customIndicatorSet.isEmpty()) {
	                		for(String metricName: customIndicatorSet) {
		                		//String metricName = methodName.substring(3);
		                		//metricName = Character.toLowerCase(metricName.charAt(0)) + metricName.substring(1);
		                	
								String fullName = timeframe + "_" + metricName;
								double mean = historicalDataService.calculateHistoricalMean(fullName);
								double stdDev = historicalDataService.calculateHistoricalStdDev(fullName);
		
								// FIXED: Use correct constructor
								AnalysisMetric metric = new AnalysisMetric(metricName, timeframe, mean, stdDev,
										AnalysisResult.class);
		
								// Set comparison type for technical indicators
								if (metricName.contains("Breakout") || metricName.contains("Count")) {
									metric.setComparisonType(ComparisonType.CROSS_SYMBOL);
									metric.setLowerIsBetter(true);
								} else if (metricName.contains("Percentile") || metricName.contains("Target")) {
									metric.setComparisonType(ComparisonType.CROSS_SYMBOL);
									metric.setLowerIsBetter(false);
								}
		
								addMetric(metric);
	                		}
                		}
                }
            }
        }
    }
    
    protected void addMetric(AnalysisMetric metric) {
        metrics.put(metric.getFullName(), metric);
    }
    
    public List<String> getAllMetricNames() {
        return new ArrayList<>(metrics.keySet());
    }
    
    public List<String> getPriceDataMetrics() {
        return metrics.values().stream()
            .filter(metric -> metric.getSourceClass() == PriceData.class)
            .map(AnalysisMetric::getFullName)
            .sorted()
            .collect(Collectors.toList());
    }
    
    public List<String> getTechnicalIndicators() {
        return metrics.values().stream()
            .filter(metric -> metric.getSourceClass() == AnalysisResult.class)
            .map(AnalysisMetric::getFullName)
            .sorted()
            .collect(Collectors.toList());
    }
    
    public List<String> getMetricsByTimeframe(String timeframe) {
        return metrics.values().stream()
            .filter(metric -> timeframe.equals(metric.getTimeframe()))
            .map(AnalysisMetric::getFullName)
            .sorted()
            .collect(Collectors.toList());
    }
    
    public AnalysisMetric getMetric(String fullName) {
        return metrics.get(fullName);
    }
    
    public double getHistoricalMean(String metricFullName) { 
    	return historicalDataService.calculateHistoricalMean(metricFullName);
    }
    
    public double getHistoricalStdDev(String metricFullName) {
        // Always compute fresh from historical data
        return historicalDataService.calculateHistoricalStdDev(metricFullName);
    }
    
    public void updateWithNewData(PriceData priceData) {
        // Add new data to historical service
        historicalDataService.addPriceData(priceData);
        
        // Recompute metrics with updated historical data
        metrics.values().stream()
            .filter(metric -> metric.getSourceClass() == PriceData.class)
            .forEach(metric -> {
                String fullName = metric.getFullName();
                double newMean = historicalDataService.calculateHistoricalMean(fullName);
                double newStdDev = historicalDataService.calculateHistoricalStdDev(fullName);
                metrics.put(fullName, new AnalysisMetric(
                    metric.getName(), metric.getTimeframe(), newMean, newStdDev, metric.getSourceClass()
                ));
            });
    }
    
    /*public void updateMetricSettings(String metricName, ComparisonType comparisonType, boolean lowerIsBetter) {
        // Update specific metric by name (without timeframe)
        metrics.values().stream()
            .filter(metric -> metric.getName().equals(metricName) && metric.getTimeframe() == null)
            .forEach(metric -> {
                metric.setComparisonType(comparisonType);
                metric.setLowerIsBetter(lowerIsBetter);
            });
        
        // Update all timeframe variants of the metric
        for (String timeframe : allTimeframes) {
            String fullName = timeframe + "_" + metricName;
            AnalysisMetric metric = metrics.get(fullName);
            if (metric != null) {
                metric.setComparisonType(comparisonType);
                metric.setLowerIsBetter(lowerIsBetter);
            }
        }
    }*/
    
    public void updateMetricSettings(String metricFullName, ComparisonType comparisonType, boolean lowerIsBetter) {
        AnalysisMetric metric = metrics.get(metricFullName);
        if (metric != null) {
            metric.setComparisonType(comparisonType);
            metric.setLowerIsBetter(lowerIsBetter);
        }
    }

    public void resetAllComparisonSettings() {
        for (AnalysisMetric metric : metrics.values()) {
            metric.setComparisonType(ComparisonType.HISTORICAL);
            metric.setLowerIsBetter(false);
            
            // Set specific defaults for certain metric types
            String metricName = metric.getName().toLowerCase();
            if (metricName.contains("count") || metricName.contains("breakout")) {
                metric.setComparisonType(ComparisonType.CROSS_SYMBOL);
                metric.setLowerIsBetter(true);
            } else if (metricName.contains("currentVolume") || metricName.contains("percent")) {
                metric.setComparisonType(ComparisonType.CROSS_SYMBOL);
                metric.setLowerIsBetter(false);
            }
        }
    }
    
    public static String[] getAllTimeframes() {
        return allTimeframes.toArray(new String[allTimeframes.size()]);
    }
    
    public static Set<String> getAllTimeframesSet(){
    	 return allTimeframes;
    }
    
    // Helper method to get all discovered PriceData field names
    public List<String> getDiscoveredPriceDataFields() {
        return metrics.values().stream()
            .filter(metric -> metric.getSourceClass() == PriceData.class)
            .map(AnalysisMetric::getName)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }
    
    // Helper method to get all discovered AnalysisResult field names
    public List<String> getDiscoveredAnalysisResultFields() {
        return metrics.values().stream()
            .filter(metric -> metric.getSourceClass() == AnalysisResult.class)
            .map(AnalysisMetric::getName)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

	public Map<String, Set<String>> getCustomIndicatorCombinations() {
		return customIndicatorCombinations;
	}

	public void setCustomIndicatorCombinations(Map<String, Set<String>> customIndicatorCombinations) {
		this.customIndicatorCombinations = customIndicatorCombinations;
	}
    
    
}