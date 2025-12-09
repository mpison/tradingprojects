package com.quantlabs.stockApp.indicator.strategy;

import org.ta4j.core.BarSeries;

import com.quantlabs.stockApp.core.indicators.VolumeIndicator;
import com.quantlabs.stockApp.core.indicators.VolumeMAIndicator;
import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class VolumeStrategyMA extends AbstractIndicatorStrategy{

	public VolumeStrategyMA(int period, ConsoleLogger logger) {
		super(logger);
		this.period = period;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "VOLUMEMA("+period+")";
	}

	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
		if(period == 20) {
			result.setVolume20Trend(currentTrend);
		}else {
			result.setCustomIndicatorValue(getName() + "_VOLUMEMATrend", currentTrend);
		}
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		if(period == 20) {
			return result.getVolume20Trend();
		}else {
			return result.getCustomIndicatorValue(getName() + "_VOLUMEMATrend");
		}
	}

	@Override
	public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
		int end = series.getEndIndex();
	    if (end >= 0) {
	    	
	    	VolumeIndicator vInd = new VolumeIndicator(series);
	    	
	    	double currentVol = vInd.getValue(series.getEndIndex()).doubleValue();
	    	
	    	VolumeMAIndicator v20Ind = new VolumeMAIndicator(series, period);
	    	
	    	double vma =  v20Ind.getValue(series.getEndIndex()).doubleValue();
            
            
            String trend = (currentVol >= vma) ? "Uptrend": "Downtrend";            
            
            //trend = String.format("%s(%d): %.2f, Trend: %s", "VMA", period, vma, trend);
            
            double zscore = calculateZscore(series,result,endIndex);
    	    
            String name = getName();
            
    	    if(period == 20) {
    	    	result.setVolume20Trend(trend);
    	    	result.setVolume20MA(vma);
    	    	result.setVolume20Zscore(zscore);
    	    }else {
    	    	result.setCustomIndicatorValue(name + "_VOLUMEMA", String.valueOf(vma));
	        	result.setCustomIndicatorValue(name + "_VOLUMEMATrend", String.valueOf(trend));
	        	result.setCustomIndicatorValue(name + "_VOLUMEMAZscore", String.valueOf(zscore));
    	    }
	    }
	    
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
	        double normalizedZscore = maxPossibleScore > 0 ? (zscore / maxPossibleScore) *  MAX_ZSCORE : 0.0;
	        
	        //result.setVolume20Zscore(normalizedZscore);
	        return normalizedZscore;
	    }
	    return 0.0;
	}

	public String getTrend(AnalysisResult analysisResult) {
		 
        if(getPeriod() == 20 ) {
	        String trend = analysisResult.getVolume20Trend();   
	        
	        return trend;//String.format("VolumeMA(%d): %.0f, Trend: %s", getPeriod(), volumeMA, trend);
        
        }else {
        	
        	String trendName = getName();
	        String trend = String.valueOf(analysisResult.getCustomIndicatorValue(trendName +"_VOLUMEMATrend"));     
	        
	        return trend;//String.format("VolumeMA(%d): %.0f, Trend: %s", getPeriod(), volumeMA, trend);
        
        }
	}

	public double getVolumeMA(AnalysisResult analysisResult) {
		 
        if(getPeriod() == 20 ) {
	        double volumeMA = analysisResult.getVolume20MA(); 
	        
	        return volumeMA;
        
        }else {
        	
        	String trendName = getName();
        	
	        double volumeMA= Double.valueOf(analysisResult.getCustomIndicatorValue(trendName +"_VOLUMEMA"));    
	        
	        return volumeMA;
        
        }
	}

}
