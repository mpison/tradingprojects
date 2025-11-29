package com.quantlabs.stockApp.indicator.strategy;

import org.ta4j.core.BarSeries;

import com.quantlabs.stockApp.core.indicators.VolumeIndicator;
import com.quantlabs.stockApp.core.indicators.VolumeMAIndicator;
import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class VolumeStrategyMA extends AbstractIndicatorStrategy{

	public VolumeStrategyMA(ConsoleLogger logger) {
		super(logger);
		period = 20;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Volume20MA";
	}

	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		return result.getVolume20Trend();
	}

	@Override
	public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
		int end = series.getEndIndex();
	    if (end >= 0) {
	    	
	    	VolumeIndicator vInd = new VolumeIndicator(series);
	        //result.setVolume(vInd.getValue(series.getEndIndex()).doubleValue());
	    	
	    	double currentVol = vInd.getValue(series.getEndIndex()).doubleValue();
	    	
	    	VolumeMAIndicator v20Ind = new VolumeMAIndicator(series, period);
	    	
	    	double vma =  v20Ind.getValue(series.getEndIndex()).doubleValue();
            result.setVolume20MA(vma);
            
            String trend = (currentVol >= vma) ? "Uptrend": "Downtrend";            
            
            trend = String.format("%s(%d): %.2f, Trend: %s", "VMA", period, vma, trend);
            
            result.setVolume20Trend(trend);
	    }
	    
	    calculateZscore(series,result,endIndex);
	}
	
	// In VolumeStrategyMA.java - Replace the calculateZscore method with this:
	@Override
	public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
	    int end = series.getEndIndex();
	    if (end >= 0) {
	        VolumeIndicator vInd = new VolumeIndicator(series);
	        double currentVol = vInd.getValue(series.getEndIndex()).doubleValue();
	        
	        // Use the correct constructor - single parameter
	        VolumeMAIndicator v20Ind = new VolumeMAIndicator(series, period);
	        double vma = v20Ind.getValue(series.getEndIndex()).doubleValue();
	        
	        // If we can't get valid volume MA, return 0
	        if (Double.isNaN(vma) || vma == 0) {
	            return 0.0;
	        }
	        
	        double zscore = 0.0;
	        double maxPossibleScore = 0.0;
	        
	        // 1. Volume above MA (bullish confirmation) - 50 points
	        maxPossibleScore += 50;
	        if (currentVol >= vma) {
	            zscore += 50;
	        }
	        
	        // 2. Strong volume spike - 30 points
	        maxPossibleScore += 30;
	        if (currentVol > vma * 1.5) {
	            zscore += 30;
	        }
	        
	        // 3. Volume increasing - 20 points
	        maxPossibleScore += 20;
	        if (end > 0) {
	            double prevVol = vInd.getValue(end - 1).doubleValue();
	            if (currentVol > prevVol) {
	                zscore += 20;
	            }
	        }
	        
	        // Normalize to 100%
	        double normalizedZscore = maxPossibleScore > 0 ? (zscore / maxPossibleScore) * MAX_ZSCORE : 0.0;
	        
	        result.setVolume20Zscore(normalizedZscore);
	        return normalizedZscore;
	    }
	    return 0.0;
	}

}
