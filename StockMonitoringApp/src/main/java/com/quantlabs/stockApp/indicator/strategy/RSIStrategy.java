package com.quantlabs.stockApp.indicator.strategy;
	
import java.util.concurrent.ConcurrentHashMap;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class RSIStrategy extends AbstractIndicatorStrategy {
    public RSIStrategy(ConsoleLogger logger) {
		super(logger);
	}
    
    @Override
    public String getName() {
        return "RSI";
    }
    
    @Override
    public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        
        if (endIndex >= 13) {
            result.setRsi(rsi.getValue(endIndex).doubleValue());
        }
        
        result.setRsiTrend(determineTrend(result));
        
        calculateZscore(series,result,endIndex);
    }

	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
		// TODO Auto-generated method stub
		result.setRsiTrend(currentTrend);
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		double rsiValue = result.getRsi();
		if (rsiValue == Double.NaN) {
	        return "Neutral";
	    }
	    if (rsiValue > 70) {
	        return "Overbought";
	    } else if (rsiValue < 30) {
	        return "Oversold";
	    }
	    return "Neutral";
	}
	
	// In RSIStrategy.java - Add this method
	// In RSIStrategy.java - Replace the calculateZscore method with this:
	@Override
	public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
	    if (endIndex < 13) return 0.0;
	    
	    double rsiValue = result.getRsi();
	    
	    // If we can't get valid RSI, return 0
	    if (Double.isNaN(rsiValue)) {
	        return 0.0;
	    }
	    
	    double zscore = 0.0;
	    double maxPossibleScore = 0.0;
	    
	    // 1. RSI in oversold territory (high bullish probability) - 60 points
	    maxPossibleScore += 60;
	    if (rsiValue < 30) {
	        zscore += 60;
	    }
	    
	    // 2. RSI above 50 (bullish territory) - 20 points
	    maxPossibleScore += 20;
	    if (rsiValue > 50) {
	        zscore += 20;
	    }
	    
	    // 3. RSI increasing (momentum) - 20 points
	    maxPossibleScore += 20;
	    if (endIndex > 13) {
	        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
	        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
	        double prevRsi = rsi.getValue(endIndex - 1).doubleValue();
	        
	        if (!Double.isNaN(prevRsi) && rsiValue > prevRsi) {
	            zscore += 20;
	        }
	    }
	    
	    // Normalize to 100%
	    double normalizedZscore = maxPossibleScore > 0 ? (zscore / maxPossibleScore) * MAX_ZSCORE : 0.0;
	    
	    result.setRsiZscore(normalizedZscore);
	    return normalizedZscore;
	}
}