package com.quantlabs.stockApp.analysis.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.quantlabs.stockApp.analysis.model.AnalysisMetric;
import com.quantlabs.stockApp.analysis.model.ZScoreCombination;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class ZScoreAnalysisService {

	private final AnalysisMetricService metricService;
	private final HistoricalDataService historicalDataService;
	private final CrossSymbolDataService crossSymbolService;
	private final Map<String, ZScoreCombination> combinations = new LinkedHashMap<>();
	private List<PriceData> currentSymbols = new ArrayList<>();

	// FIXED: Constructor with proper parameters
    public ZScoreAnalysisService(AnalysisMetricService metricService, 
                                HistoricalDataService historicalDataService) {
        this.metricService = metricService;
        this.historicalDataService = historicalDataService;
        this.crossSymbolService = new CrossSymbolDataService(); // Initialize internally
        initializeTechnicalCombinations();
    }
    
    // Alternative constructor if you want to inject CrossSymbolDataService
    public ZScoreAnalysisService(AnalysisMetricService metricService,
                                HistoricalDataService historicalDataService,
                                CrossSymbolDataService crossSymbolService) {
        this.metricService = metricService;
        this.historicalDataService = historicalDataService;
        this.crossSymbolService = crossSymbolService;
        initializeTechnicalCombinations();
    }

	private void initializeTechnicalCombinations() {
		// Momentum strategy using RSI and MACD (1D timeframe)
		Map<String, Double> momentumWeights = new HashMap<>();
		momentumWeights.put("1D_rsi", 40.0);
		momentumWeights.put("1D_macd", 30.0);
		momentumWeights.put("1D_macd359", 30.0);
		addCombination(new ZScoreCombination("TECH_MOMENTUM_1D", momentumWeights));

		// Trend following strategy using moving averages (1D timeframe)
		Map<String, Double> trendWeights = new HashMap<>();
		trendWeights.put("1D_sma20", 35.0);
		trendWeights.put("1D_sma200", 35.0);
		trendWeights.put("1D_movingAverageTargetValuePercentile", 30.0);
		addCombination(new ZScoreCombination("TECH_TREND_1D", trendWeights));

		// Breakout strategy using PSAR and MACD (4H timeframe)
		Map<String, Double> breakoutWeights = new HashMap<>();
		breakoutWeights.put("4H_psar001", 25.0);
		breakoutWeights.put("4H_psar005", 25.0);
		breakoutWeights.put("4H_macd", 25.0);
		breakoutWeights.put("4H_macd359", 25.0);
		addCombination(new ZScoreCombination("TECH_BREAKOUT_4H", breakoutWeights));

		// Comprehensive technical analysis (1H timeframe)
		Map<String, Double> comprehensiveWeights = new HashMap<>();
		comprehensiveWeights.put("1H_rsi", 20.0);
		comprehensiveWeights.put("1H_macd", 20.0);
		comprehensiveWeights.put("1H_sma20", 15.0);
		comprehensiveWeights.put("1H_sma200", 15.0);
		comprehensiveWeights.put("1H_psar001", 15.0);
		comprehensiveWeights.put("1H_movingAverageTargetValuePercentile", 15.0);
		addCombination(new ZScoreCombination("TECH_COMPREHENSIVE_1H", comprehensiveWeights));
	}

	public void addCombination(ZScoreCombination combination) {
		combinations.put(combination.getName(), combination);
	}

	public void removeCombination(String combinationName) {
		combinations.remove(combinationName);
	}

	public ZScoreCombination getCombination(String combinationName) {
		return combinations.get(combinationName);
	}

	public List<String> getAvailableCombinations() {
		return new ArrayList<>(combinations.keySet());
	}

	public List<Map.Entry<String, Double>> rankSymbols(List<PriceData> symbols, String combinationName) { 
		
		if (symbols == null || symbols.isEmpty()) {
	        throw new IllegalArgumentException("Symbols list cannot be null or empty");
	    }
	    if (combinationName == null || combinationName.trim().isEmpty()) {
	        throw new IllegalArgumentException("Combination name cannot be null or empty");
	    }
	    if (!combinations.containsKey(combinationName)) {
	        throw new IllegalArgumentException("Unknown combination: " + combinationName);
	    }
		
		this.currentSymbols = symbols;

		// Update all data services
		for (PriceData symbol : symbols) {
			historicalDataService.addPriceData(symbol);
		}
		crossSymbolService.updateCurrentSymbols(symbols);

		if (!symbols.isEmpty()) {
			metricService.updateWithNewData(symbols.get(0));
		}

		// Rank symbols with enhanced Z-score calculation
		Map<String, Double> symbolScores = new HashMap<>();

		for (PriceData symbol : symbols) {
			double combinedScore = calculateWeightedZScore(symbol, combinationName);
			symbolScores.put(symbol.getTicker(), combinedScore);
		}

		return symbolScores.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
				.collect(Collectors.toList());
	}

	private double calculateWeightedZScore(PriceData symbol, String combinationName) {
	    ZScoreCombination combination = combinations.get(combinationName);
	    // combination is guaranteed to be non-null due to validation in rankSymbols
	    
	    Map<String, Double> weights = combination.getWeights();
	    double weightedSum = 0.0;
	    double totalWeight = 0.0;

	    for (Map.Entry<String, Double> entry : weights.entrySet()) {
	        String metricFullName = entry.getKey();
	        double weight = entry.getValue();

	        Double zScore = calculateEnhancedZScore(symbol, metricFullName);
	        if (zScore != null) {
	            weightedSum += zScore * weight;
	            totalWeight += weight;
	        }
	    }

	    return totalWeight > 0 ? weightedSum / totalWeight : 0.0;
	}

	private Double calculateEnhancedZScore(PriceData symbol, String metricFullName) {
		try {
			AnalysisMetric metric = metricService.getMetric(metricFullName);
			if (metric == null)
				return null;

			// Extract current value
			String[] parts = metricFullName.split("_", 2);
			String timeframe = parts.length > 1 ? parts[0] : null;
			String metricName = parts.length > 1 ? parts[1] : parts[0];

			Object value = extractMetricValue(symbol, metricName, timeframe);
			if (value == null)
				return null;

			double numericValue = ((Number) value).doubleValue();

			// Apply different comparison logic based on metric type
			switch (metric.getComparisonType()) {
			case CROSS_SYMBOL:
				return calculateCrossSymbolZScore(metricFullName, symbol.getTicker(), numericValue,
						metric.isLowerIsBetter());

			case ABSOLUTE:
				return calculateAbsoluteScore(metricFullName, numericValue, metric.isLowerIsBetter());

			case HYBRID:
				double historicalZ = calculateHistoricalZScore(numericValue, metricFullName);
				double crossSymbolZ = calculateCrossSymbolZScore(metricFullName, symbol.getTicker(), numericValue,
						metric.isLowerIsBetter());
				return (historicalZ + crossSymbolZ) / 2.0;

			case HISTORICAL:
			default:
				return calculateHistoricalZScore(numericValue, metricFullName);
			}

		} catch (Exception e) {
			System.out.println("Error calculating enhanced Z-score: " + e.getMessage());
			return null;
		}
	}

	// In calculateCrossSymbolZScore method:
	private double calculateCrossSymbolZScore(String metricFullName, String ticker, double value,
	        boolean lowerIsBetter) {
	    // Extract base metric name (remove timeframe if present)
	    String baseMetric;
	    if (metricFullName.contains("_")) {
	        String[] parts = metricFullName.split("_", 2);
	        baseMetric = parts[1]; // Get the metric name after timeframe
	    } else {
	        baseMetric = metricFullName; // Simple metric without timeframe
	    }

	    if (lowerIsBetter) {
	        double percentile = crossSymbolService.calculatePercentile(baseMetric, ticker, true);
	        return (percentile - 50.0) / 15.0;
	    } else {
	        double mean = crossSymbolService.calculateCrossSymbolMean(baseMetric);
	        double stdDev = crossSymbolService.calculateCrossSymbolStdDev(baseMetric);

	        if (stdDev == 0)
	            return 0.0;
	        return (value - mean) / stdDev;
	    }
	}

	private double calculateAbsoluteScore(String metricFullName, double value, boolean lowerIsBetter) {
		// For absolute comparison (like pure ranking)
		if (lowerIsBetter) {
			// Lower values get higher scores
			return -value; // Invert so lower values become higher scores
		} else {
			return value; // Higher values get higher scores
		}
	}

	private double calculateHistoricalZScore(double value, String metricFullName) {
		double mean = metricService.getHistoricalMean(metricFullName);
		double stdDev = metricService.getHistoricalStdDev(metricFullName);

		if (stdDev == 0)
			return 0.0;
		return (value - mean) / stdDev;
	}

	public Double calculateIndividualZScore(PriceData symbol, String metricFullName) {
		try {
			// Parse timeframe and metric name
			String[] parts = metricFullName.split("_", 2);
			String timeframe = parts.length > 1 ? parts[0] : null;
			String metricName = parts.length > 1 ? parts[1] : parts[0];

			Object value = extractMetricValue(symbol, metricName, timeframe);
			if (value == null)
				return null;

			double numericValue;
			if (value instanceof Number) {
				numericValue = ((Number) value).doubleValue();
			} else {
				return null;
			}

			return calculateZScore(numericValue, metricFullName);
		} catch (Exception e) {
			System.out.println("Error calculating Z-score for metric " + metricFullName + ": " + e.getMessage());
			return null;
		}
	}

	private Object extractMetricValue(PriceData symbol, String metricName, String timeframe) throws Exception {
		if (timeframe == null) {
			// PriceData metric - use reflection to get the method
			String methodName = "get" + Character.toUpperCase(metricName.charAt(0)) + metricName.substring(1);
			Method method = PriceData.class.getMethod(methodName);
			return method.invoke(symbol);
		} else {
			// AnalysisResult metric with timeframe
			Method getResultsMethod = PriceData.class.getMethod("getResults");
			@SuppressWarnings("unchecked")
			Map<String, AnalysisResult> results = (Map<String, AnalysisResult>) getResultsMethod.invoke(symbol);

			if (results == null)
				return null;

			AnalysisResult result = results.get(timeframe);
			if (result == null)
				return null;

			String methodName = "get" + Character.toUpperCase(metricName.charAt(0)) + metricName.substring(1);
			Method analysisMethod = AnalysisResult.class.getMethod(methodName);
			return analysisMethod.invoke(result);
		}
	}

	private double calculateZScore(double value, String metricFullName) {
		double mean = metricService.getHistoricalMean(metricFullName);
		double stdDev = metricService.getHistoricalStdDev(metricFullName);

		if (stdDev == 0)
			return 0.0;

		return (value - mean) / stdDev;
	}

	public void updateCombinationWeights(String combinationName, Map<String, Double> newWeights) {
		ZScoreCombination combination = combinations.get(combinationName);
		if (combination != null) {
			combinations.put(combinationName, new ZScoreCombination(combinationName, new HashMap<>(newWeights)));
		}
	}

	// Helper method to update all combinations with new data
	public void updateAllWithNewData(PriceData priceData) {
		historicalDataService.addPriceData(priceData);
		metricService.updateWithNewData(priceData);
	}
	
	public Map<String, ZScoreCombination> getAllCombinations() {
        return new HashMap<>(combinations);
    }
    
    // Add method to set all combinations (for loading)
    public void setAllCombinations(Map<String, ZScoreCombination> newCombinations) {
        combinations.clear();
        combinations.putAll(newCombinations);
    }
    
    // Clear all combinations
    public void clearCombinations() {
        combinations.clear();
    }
	
	
}