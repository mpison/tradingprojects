package com.quantlabs.stockApp.indicator.strategy;

import java.util.concurrent.ConcurrentHashMap;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class MACDStrategy extends AbstractIndicatorStrategy {
    private final int fastPeriod;
    private final int slowPeriod;
    private final int signalPeriod;
    private final String variant;
    
    public MACDStrategy(int fastPeriod, int slowPeriod, int signalPeriod, String variant, ConsoleLogger logger) {
    	super(logger);
    	this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.signalPeriod = signalPeriod;
        this.variant = variant;        
    }
    
    @Override
    public String getName() {
    	if(fastPeriod == 12 && slowPeriod == 26 && signalPeriod == 9) {
    		return "MACD";
    	}
        return "MACD(" + fastPeriod + "," + slowPeriod + "," + signalPeriod + ")";
    }
    
    @Override
    public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
        int requiredBars = Math.max(fastPeriod, slowPeriod) + signalPeriod;
        if (endIndex < requiredBars - 1) return;
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        MACDIndicator macd = new MACDIndicator(closePrice, fastPeriod, slowPeriod);
        EMAIndicator signal = new EMAIndicator(macd, signalPeriod);
        
        double currentMacd = macd.getValue(endIndex).doubleValue();
        double currentSignal = signal.getValue(endIndex).doubleValue();
        
        if ("MACD(5,8,9)".equals( getName())) {
            result.setMacd359(currentMacd);
            result.setMacdSignal359(currentSignal);
            if (endIndex > 0) {
                double prevMacd = macd.getValue(endIndex - 1).doubleValue();
                double prevSignal = signal.getValue(endIndex - 1).doubleValue();
                result.setMacd359Status(determineMacdStatus(currentMacd, currentSignal, prevMacd, prevSignal));
            }
        }
        else {//if ("MACD".equals( getName())) {
            result.setMacd(currentMacd);
            result.setMacdSignal(currentSignal);
            if (endIndex > 0) {
                double prevMacd = macd.getValue(endIndex - 1).doubleValue();
                double prevSignal = signal.getValue(endIndex - 1).doubleValue();
                result.setMacdStatus(determineMacdStatus(currentMacd, currentSignal, prevMacd, prevSignal));
            }
        }  
        
        calculateZscore(series,result,endIndex);
    }
    
    private String determineMacdStatus(double currentMacd, double currentSignal, double prevMacd, double prevSignal) {
        if (currentMacd > currentSignal && prevMacd >= prevSignal && currentMacd > prevMacd && currentSignal > prevSignal) {
            return "Strong Bullish";
        }
        if (currentMacd > currentSignal && prevMacd >= prevSignal) {
            return "Bullish";
        } else if (currentMacd > currentSignal && prevMacd <= prevSignal) {
            return "Bullish Crossover";
        } else if (currentMacd < currentSignal && prevMacd >= prevSignal) {
            return "Bearish";
        } else if (currentMacd < currentSignal && prevMacd >= prevSignal) {
            return "Bearish Crossover";
        }
        return "Neutral";
    }

	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
		if ("MACD".equals( getName())) {
			result.setMacdStatus(currentTrend);
		} else if ("MACD(5,8,9)".equals( getName())) {
			result.setMacd359Status(currentTrend);
		}
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		String returnVal = "";
		
		if ("MACD".equals( getName())) {
			
			double macdLine = result.getMacd();
			double signalLine = result.getMacdSignal();
			returnVal = determineMacdStatus(macdLine, signalLine);
			result.setMacdStatus(returnVal);
			
		} else if ("MACD(5,8,9)".equals( getName())) {
			
			double macdLine = result.getMacd359();
			double signalLine = result.getMacdSignal359();
			returnVal = determineMacdStatus(macdLine, signalLine);
			result.setMacd359Status(returnVal);
		}
		
		return returnVal;
	}
	
	private String determineMacdStatus(double macdLine, double signalLine) {
	    if (macdLine == Double.NaN || signalLine == Double.NaN) {
	        return "Neutral";
	    }
	    if (macdLine > signalLine) {
	        return "Bullish";
	    } else if (macdLine < signalLine) {
	        return "Bearish";
	    }
	    return "Neutral";
	}
	
	// In MACDStrategy.java - Add this method
	@Override
	public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
	    int requiredBars = Math.max(fastPeriod, slowPeriod) + signalPeriod;
	    if (endIndex < requiredBars - 1) return 0.0;
	    
	    ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
	    MACDIndicator macd = new MACDIndicator(closePrice, fastPeriod, slowPeriod);
	    EMAIndicator signal = new EMAIndicator(macd, signalPeriod);
	    
	    double currentMacd = macd.getValue(endIndex).doubleValue();
	    double currentSignal = signal.getValue(endIndex).doubleValue();
	    
	    double zscore = 0.0;
	    double maxPossibleScore = 0.0;
	    
	    // 1. Both MACD and Signal above zero (very bullish) - 30 points
	    maxPossibleScore += 30;
	    if (currentMacd > 0 && currentSignal > 0) {
	        zscore += 30;
	    }
	    
	    // 2. MACD above Signal (bullish) - 40 points
	    maxPossibleScore += 40;
	    if (currentMacd > currentSignal) {
	        zscore += 40;
	    }
	    
	    // 3. MACD increasing (bullish momentum) - 15 points
	    maxPossibleScore += 15;
	    if (endIndex > 0) {
	        double prevMacd = macd.getValue(endIndex - 1).doubleValue();
	        if (currentMacd > prevMacd) {
	            zscore += 15;
	        }
	    }
	    
	    // 4. Signal increasing (confirmation) - 15 points
	    maxPossibleScore += 15;
	    if (endIndex > 0) {
	        double prevSignal = signal.getValue(endIndex - 1).doubleValue();
	        if (currentSignal > prevSignal) {
	            zscore += 15;
	        }
	    }
	    
	    // Normalize to 100%
	    double normalizedZscore = (zscore / maxPossibleScore) * MAX_ZSCORE;
	    
	    // Store in result
	    if ("MACD(5,8,9)".equals(getName())) {
	        result.setMacd359Zscore(normalizedZscore);
	    } else {
	        result.setMacdZscore(normalizedZscore);
	    }
	    
	    return normalizedZscore;
	}
	
}