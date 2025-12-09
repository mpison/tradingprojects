package com.quantlabs.stockApp.indicator.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public abstract class AbstractIndicatorStrategy {
	String symbol;
	boolean isEnabled = true;
	
	int period = 20;

	// Map startIndex and endIndex to BarSeries indices
	int adjustedStartIndex = 0;
	int adjustedEndIndex = 0;
	
	// Z-Score constants
    public static final double MAX_ZSCORE = 100.0;
    public static final double TREND_WEIGHT = 40.0;
    public static final double STRENGTH_WEIGHT = 30.0;
    public static final double MOMENTUM_WEIGHT = 30.0;

	protected ConsoleLogger logger;

	public AbstractIndicatorStrategy(int startIndex, int endIndex, int lastIndex, ConsoleLogger logger) {
		super();

		adjustedStartIndex = lastIndex - startIndex;
		adjustedEndIndex = lastIndex - endIndex;

		this.logger = logger;
	}

	public AbstractIndicatorStrategy(ConsoleLogger logger) {
		super();
		this.logger = logger;
	}

	public abstract String getName();

	public String getSymbol() {
		return this.symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean enabled) {
		isEnabled = enabled;
	}

	/*
	 * public PriceData getPriceDataBySymbol(String symbol) {
	 * 
	 * PriceData priceData1 = priceDataMap.get(symbol);
	 * 
	 * return priceData1; }
	 * 
	 * public ConcurrentHashMap<String, PriceData> getPriceDataMap() { return
	 * priceDataMap; }
	 * 
	 * public void setPriceDataMap(ConcurrentHashMap<String, PriceData>
	 * priceDataMap) { this.priceDataMap = priceDataMap; }
	 */

	public void analyzeSymbol(BarSeries series, int startIndex, int endIndex, AnalysisResult result) {
		
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		
		int lastIndex = series.getBarCount() - 1;
		
		Num close = closePrice.getValue(lastIndex);
		
		// Set price for the first bar
		result.setPrice(close.doubleValue());

		 // Map startIndex and endIndex to BarSeries indices
		int adjustedStartIndex = lastIndex - startIndex;
		int adjustedEndIndex = lastIndex - endIndex;
		

	    // Ensure indices are within bounds
	    if (adjustedStartIndex < 0 || adjustedStartIndex >= series.getBarCount()) {
	        adjustedStartIndex = lastIndex;
	        //logToConsole("Adjusted startIndex out of bounds for " + symbol + " (" + timeframe + "), using last index: " + adjustedStartIndex);
	    }
	    if (adjustedEndIndex < 0 || adjustedEndIndex >= series.getBarCount()) {
	        adjustedEndIndex = Math.max(0, adjustedStartIndex - 1);
	        //logToConsole("Adjusted endIndex out of bounds for " + symbol + " (" + timeframe + "), using: " + adjustedEndIndex);
	    }
	    if (adjustedEndIndex > adjustedStartIndex) {
	        //logToConsole("Invalid adjusted index range for " + symbol + " (" + timeframe + "): adjustedEndIndex=" + adjustedEndIndex + " is greater than adjustedStartIndex=" + adjustedStartIndex);
	        //return result;
	    }
		
		

		Map<String, String> previousStatuses = new HashMap<>();

		if (getName().toUpperCase() != "HIGHESTCLOSEOPEN" || getName() != "MOVINGAVERAGETARGETVALUE" || !getName().contains("Breakout")) {
			for (int i = adjustedEndIndex; i <= adjustedStartIndex; i++) {

				
				//Num prevClose = i > 0 ? closePrice.getValue(i - 1) : close;

				calculate(series, result, adjustedEndIndex);

				String currentTrend = determineTrend(result);

				if (i == adjustedEndIndex) {
					

					previousStatuses.put(getName(), currentTrend);

					if (previousStatuses.get(getName()) != null && previousStatuses.get(getName()) != ""
							&& currentTrend != previousStatuses.get(getName())) {
						previousStatuses.put(getName(), "Neutral");
						currentTrend = "Neutral";
					}

					updateTrendStatus(result, currentTrend);

				} else if (previousStatuses.get(getName()) != null
						&& !currentTrend.equals(previousStatuses.get(getName()))) {
					previousStatuses.put(getName(), "Neutral");

					updateTrendStatus(result, "Neutral");
				} else {
					previousStatuses.put(getName(), currentTrend);
				}

			}
		}else {
			
			calculate(series, result, lastIndex);
		}
	}

	protected abstract void updateTrendStatus(AnalysisResult result, String currentTrend);

	public abstract String determineTrend(AnalysisResult result);

	public abstract void calculate(BarSeries series, AnalysisResult result, int endIndex);

	public int getPeriod() {
		return period;
	}

	public void setPeriod(int period) {
		this.period = period;
	}
	
	public abstract double calculateZscore(BarSeries series, AnalysisResult result, int endIndex);
	
	// Helper methods for Z-Score calculation
    protected double calculateTrendScore(String trend) {
        if (trend == null) return 0.0;
        
        switch (trend.toLowerCase()) {
            case "bullish": case "uptrend": case "strong uptrend": 
                return 80.0;
            case "bearish": case "downtrend": 
                return 20.0;
            case "neutral": case "ranging":
                return 50.0;
            case "very strong": case "exceptional":
                return 95.0;
            case "strong":
                return 85.0;
            case "good":
                return 70.0;
            case "moderate":
                return 60.0;
            case "fair":
                return 45.0;
            case "weak":
                return 30.0;
            case "very weak":
                return 15.0;
            default:
                return 50.0;
        }
    }
    
    protected double calculateStrengthScore(double currentValue, double referenceValue) {
        if (referenceValue == 0) return 50.0;
        double ratio = currentValue / referenceValue;
        return Math.min(100.0, Math.max(0.0, ratio * 100.0));
    }
    
    protected double calculateMomentumScore(double currentValue, double previousValue) {
        if (previousValue == 0) return 50.0;
        double change = ((currentValue - previousValue) / previousValue) * 100.0;
        return Math.min(100.0, Math.max(0.0, 50.0 + change));
    }
    
    protected double normalizeScore(double score, double maxPossible) {
        if (maxPossible == 0) return 0.0;
        return (score / maxPossible) * MAX_ZSCORE;
    }

}