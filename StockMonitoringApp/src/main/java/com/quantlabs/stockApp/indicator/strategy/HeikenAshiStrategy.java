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
        double haHigh = heikenAshi.getHeikenAshiHigh(endIndex).doubleValue();
        double haLow = heikenAshi.getHeikenAshiLow(endIndex).doubleValue();
        
        double zscore = 0.0;
        double maxPossibleScore = 0.0;
        
        // 1. Bullish/Bearish candle direction - 40 points
        maxPossibleScore += 40;
        if (haClose > haOpen) {
            zscore += 40; // Bullish
        } else if (haClose < haOpen) {
            zscore += 10; // Bearish
        } else {
            zscore += 25; // Neutral/Doji
        }
        
        // 2. Candle strength (body size) - 30 points
        maxPossibleScore += 30;
        double bodySize = Math.abs(haClose - haOpen);
        double totalRange = haHigh - haLow;
        if (totalRange > 0) {
            double bodyRatio = bodySize / totalRange;
            zscore += (bodyRatio * 30);
        }
        
        // 3. Trend consistency - 30 points
        maxPossibleScore += 30;
        if (endIndex > 0) {
            double prevHaClose = heikenAshi.getValue(endIndex - 1).doubleValue();
            double prevHaOpen = heikenAshi.getHeikenAshiOpen(endIndex - 1).doubleValue();
            
            boolean currentBullish = haClose > haOpen;
            boolean previousBullish = prevHaClose > prevHaOpen;
            
            if (currentBullish == previousBullish) {
                zscore += 30; // Same trend continues
            } else {
                zscore += 10; // Trend reversal
            }
        } else {
            zscore += 15; // First candle, neutral score
        }
        
        // Normalize to 100%
        double normalizedZscore = normalizeScore(zscore, maxPossibleScore);
        
        result.setHeikenAshiZscore(normalizedZscore);
        return normalizedZscore;
    }
}