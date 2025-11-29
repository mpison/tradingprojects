package com.quantlabs.stockApp.indicator.strategy;

import java.util.concurrent.ConcurrentHashMap;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class PSARStrategy extends AbstractIndicatorStrategy {
    private final double step;
    private final double maxStep;
    private final String variant;
    
    public PSARStrategy(double step, double maxStep, String variant, ConsoleLogger logger) {
    	super(logger);
        this.step = step;
        this.maxStep = maxStep;
        this.variant = variant;
    }
    
    @Override
    public String getName() {
        return "PSAR(" + step + ")";
    }
    
    @Override
    public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
        if (endIndex < 0) return;
        
        try {
            ParabolicSarIndicator psar = new ParabolicSarIndicator(series, 
                series.numOf(step), series.numOf(step), series.numOf(maxStep));
            
            Num currentClose = series.getBar(endIndex).getClosePrice();
            double psarValue = psar.getValue(endIndex).doubleValue();
            String trend = psarValue < currentClose.doubleValue() ? "Uptrend" : "Downtrend";
            
            if ("PSAR(0.01)".equals(getName())) {
                result.setPsar001(psarValue);
                result.setPsar001Trend(trend);
            } else if ("PSAR(0.05)".equals(getName())) {
                result.setPsar005(psarValue);
                result.setPsar005Trend(trend);
            }else {
            	result.setPsar(psarValue);
                result.setPsarTrend(trend);
            }
        } catch (Exception e) {
            // Handle exception
        }
        
        calculateZscore(series,result,endIndex);
    }

	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
		if ("PSAR(0.01)".equals(getName())) {
            //result.setPsar001(psarValue);
            result.setPsar001Trend(currentTrend);
        } else if ("PSAR(0.05)".equals(getName())) {
            //result.setPsar005(psarValue);
            result.setPsar005Trend(currentTrend);
        }
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		String returnVal = "";
		
		if ("PSAR(0.01)".equals(getName())) {            
			returnVal = determinePsarStatus(result.getPrice(), result.getPsar001());
            
            result.setPsar001Trend(returnVal);
        } else if ("PSAR(0.05)".equals(getName())) {
        	
        	returnVal = determinePsarStatus(result.getPrice(), result.getPsar005());
        	
            result.setPsar005Trend(returnVal);
        }
		
		return returnVal;
	}
	
	private String determinePsarStatus(double close, double psarValue) {
	    if (close == Double.NaN || psarValue == Double.NaN) {
	        return "Neutral";
	    }
	    if (close > psarValue) {
	        return "Bullish";
	    } else {
	        return "Bearish";
	    }
	}
	
	// In PSARStrategy.java - Add this method
	@Override
	public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
	    if (endIndex < 0) return 0.0;
	    
	    try {
	        ParabolicSarIndicator psar = new ParabolicSarIndicator(series, 
	            series.numOf(step), series.numOf(step), series.numOf(maxStep));
	        
	        double currentClose = series.getBar(endIndex).getClosePrice().doubleValue();
	        double psarValue = psar.getValue(endIndex).doubleValue();
	        
	        double zscore = 0.0;
	        double maxPossibleScore = 0.0;
	        
	        // 1. Price above PSAR (bullish) - 70 points
	        maxPossibleScore += 70;
	        if (currentClose > psarValue) {
	            zscore += 70;
	        }
	        
	        // 2. PSAR trending up (acceleration) - 30 points
	        maxPossibleScore += 30;
	        if (endIndex > 0) {
	            double prevPsar = psar.getValue(endIndex - 1).doubleValue();
	            if (psarValue > prevPsar) {
	                zscore += 30;
	            }
	        }
	        
	        // Normalize to 100%
	        double normalizedZscore = (zscore / maxPossibleScore) * MAX_ZSCORE;
	        
	        // Store in appropriate result field
	        if ("PSAR(0.01)".equals(getName())) {
	            result.setPsar001Zscore(normalizedZscore);
	        } else if ("PSAR(0.05)".equals(getName())) {
	            result.setPsar005Zscore(normalizedZscore);
	        } else {
	            result.setPsarZscore(normalizedZscore);
	        }
	        
	        return normalizedZscore;
	    } catch (Exception e) {
	        return 0.0;
	    }
	}
}