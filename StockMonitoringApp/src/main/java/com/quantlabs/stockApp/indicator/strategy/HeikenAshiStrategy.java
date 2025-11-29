package com.quantlabs.stockApp.indicator.strategy;

import java.util.concurrent.ConcurrentHashMap;

import org.ta4j.core.BarSeries;

import com.quantlabs.stockApp.core.indicators.HeikenAshiIndicator;
import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class HeikenAshiStrategy extends AbstractIndicatorStrategy {
    public HeikenAshiStrategy(ConsoleLogger logger) {
		super(logger);
		// TODO Auto-generated constructor stub
	}
    
    @Override
    public String getName() {
        return "HeikenAshi";
    }
    
    @Override
    public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
        if (endIndex < 0) return;
        
        HeikenAshiIndicator heikenAshi = new HeikenAshiIndicator(series);
        
        result.setHeikenAshiClose(heikenAshi.getValue(endIndex).doubleValue());
        result.setHeikenAshiOpen(heikenAshi.getHeikenAshiOpen(endIndex).doubleValue());
        
        // Determine trend
        String trend = determineTrend(result);
        result.setHeikenAshiTrend(trend);
        
        calculateZscore(series,result,endIndex);
    }   

	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {		
		result.setHeikenAshiTrend(currentTrend);
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		String trend = result.getHeikenAshiClose() > result.getHeikenAshiOpen() ? "Uptrend" : "Downtrend";
        result.setHeikenAshiTrend(trend);
		return trend;
	}
	
	// In HeikenAshiStrategy.java - Add this method
	@Override
	public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
	    if (endIndex < 0) return 0.0;
	    
	    HeikenAshiIndicator heikenAshi = new HeikenAshiIndicator(series);
	    
	    double haClose = heikenAshi.getValue(endIndex).doubleValue();
	    double haOpen = heikenAshi.getHeikenAshiOpen(endIndex).doubleValue();
	    
	    double zscore = 0.0;
	    double maxPossibleScore = 0.0;
	    
	    // 1. Bullish Heiken Ashi candle - 60 points
	    maxPossibleScore += 60;
	    if (haClose > haOpen) {
	        zscore += 60;
	    }
	    
	    // 2. Strong bullish (no lower wick) - 20 points
	    maxPossibleScore += 20;
	    if (haClose > haOpen) {
	        double haLow = heikenAshi.getHeikenAshiLow(endIndex).doubleValue();
	        if (haOpen == haLow) {
	            zscore += 20;
	        }
	    }
	    
	    // 3. Consecutive bullish candles - 20 points
	    maxPossibleScore += 20;
	    if (endIndex > 0) {
	        double prevHaClose = heikenAshi.getValue(endIndex - 1).doubleValue();
	        double prevHaOpen = heikenAshi.getHeikenAshiOpen(endIndex - 1).doubleValue();
	        
	        if (haClose > haOpen && prevHaClose > prevHaOpen) {
	            zscore += 20;
	        }
	    }
	    
	    // Normalize to 100%
	    double normalizedZscore = (zscore / maxPossibleScore) * MAX_ZSCORE;
	    
	    result.setHeikenAshiZscore(normalizedZscore);
	    return normalizedZscore;
	}
}