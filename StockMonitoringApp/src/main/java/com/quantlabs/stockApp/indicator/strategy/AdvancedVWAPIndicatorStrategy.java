package com.quantlabs.stockApp.indicator.strategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.core.indicators.AdvancedVWAPStrategy;
import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class AdvancedVWAPIndicatorStrategy extends AbstractIndicatorStrategy{

	public AdvancedVWAPIndicatorStrategy(ConsoleLogger logger) {
		super(logger);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "VWAP";
	}

	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
		// TODO Auto-generated method stub
		result.setVwapStatus(currentTrend);
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		/*double vwapValue = result.getVwap();
		if (vwapValue == Double.NaN) {
	        return "Neutral";
	    }
	    if (vwapValue > 70) {
	        return "Overbought";
	    } else if (vwapValue < 30) {
	        return "Oversold";
	    }
	    return "Neutral";*/
		return result.getVwapStatus();
	}

	@Override
    public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
        if (series == null || series.getBarCount() == 0) return;
        
        AdvancedVWAPStrategy.AdvancedVWAPIndicator vwapIndicator = new AdvancedVWAPStrategy.AdvancedVWAPIndicator(series);
        
        if (endIndex >= 0 && endIndex < series.getBarCount()) {
            Num vwapValue = vwapIndicator.getValue(endIndex);
            Num currentPrice = series.getBar(endIndex).getClosePrice();
            
            // Set VWAP value
            result.setVwap(vwapValue.doubleValue());
            
            // Determine trend based on price vs VWAP
            String vwapStatus = currentPrice.isGreaterThan(vwapValue) ? "Uptrend" : "Downtrend";
            result.setVwapStatus(vwapStatus);
        }
        
        // Calculate Z-Score
        double zscore = calculateZscore(series, result, endIndex);
        result.setVwapZscore(zscore);
    }
	
	// In AdvancedVWAPIndicatorStrategy.java - Add this method
	@Override
	public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
	    if (series == null || endIndex < 0 || endIndex >= series.getBarCount()) {
	        return 0.0;
	    }
	    
	    AdvancedVWAPStrategy.AdvancedVWAPIndicator vwapIndicator = new AdvancedVWAPStrategy.AdvancedVWAPIndicator(series);
	    Num vwapValue = vwapIndicator.getValue(endIndex);
	    Num currentPrice = series.getBar(endIndex).getClosePrice();
	    
	    double zscore = 0.0;
	    
	    // 1. Basic trend direction - 60 points
	    if (currentPrice.isGreaterThan(vwapValue)) {
	        zscore += 60; // Above VWAP - bullish
	    } else {
	        zscore += 20; // Below VWAP - bearish
	    }
	    
	    // 2. Deviation strength - 40 points
	    double deviationPercent = ((currentPrice.doubleValue() - vwapValue.doubleValue()) / vwapValue.doubleValue()) * 100.0;
	    double deviationScore = Math.min(40, Math.abs(deviationPercent) * 4);
	    
	    if (currentPrice.isGreaterThan(vwapValue)) {
	        zscore += deviationScore; // Positive deviation adds to bullish
	    } else {
	        zscore += (40 - deviationScore); // Negative deviation reduces bearish
	    }
	    
	    result.setVwapZscore(zscore);
	    return zscore;
	}

	

}
