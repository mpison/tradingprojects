package com.quantlabs.stockApp.indicator.strategy;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class BreakoutCountStrategy extends AbstractIndicatorStrategy {
    private final String breakoutType;
    
    public BreakoutCountStrategy(String breakoutType, ConsoleLogger logger) {
    	super(logger);
        this.breakoutType = breakoutType;
    }
    
    @Override
    public String getName() {
        return breakoutType + " Breakout";
    }
    
    @Override
    public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
        int count = 0;
        
        switch (breakoutType) {
            case "MACD":
                count = calculateMACDBreakoutCount(series, 12, 26, 9, endIndex);
                result.setMacdBreakoutCount(count);
                break;
                
            case "MACD(5,8,9)":
                count = calculateMACDBreakoutCount(series, 5, 8, 9, endIndex);
                result.setMacd359BreakoutCount(count);
                break;
                
            case "PSAR(0.01)":
                count = calculatePSARBreakoutCount(series, 0.01, 0.01, 0.02, endIndex);
                result.setPsar001BreakoutCount(count);
                break;
                
            case "PSAR(0.05)":
                count = calculatePSARBreakoutCount(series, 0.05, 0.05, 0.02, endIndex);
                result.setPsar005BreakoutCount(count);
                break;
        }
    }
    
    private int calculateMACDBreakoutCount(BarSeries series, int fastPeriod, int slowPeriod, int signalPeriod, int endIndex) {
        return calculateBreakoutCount(series, endIndex, i -> {
            int requiredBars = Math.max(fastPeriod, slowPeriod) + signalPeriod;
            if (i < requiredBars - 1) return 0;
            
            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            MACDIndicator macd = new MACDIndicator(closePrice, fastPeriod, slowPeriod);
            EMAIndicator signal = new EMAIndicator(macd, signalPeriod);
            
            double currentMacd = macd.getValue(i).doubleValue();
            double currentSignal = signal.getValue(i).doubleValue();
            
            return currentMacd > currentSignal ? 1 : -1;
        });
    }
    
    private int calculatePSARBreakoutCount(BarSeries series, double step, double initialStep, double maxStep, int endIndex) {
        return calculateBreakoutCount(series, endIndex, i -> {
            if (i < 1) return 0;
            
            ParabolicSarIndicator psar = new ParabolicSarIndicator(series, 
                series.numOf(initialStep), series.numOf(step), series.numOf(maxStep));
            
            double psarValue = psar.getValue(i).doubleValue();
            double closePrice = series.getBar(i).getClosePrice().doubleValue();
            
            return psarValue < closePrice ? 1 : -1;
        });
    }
    
    private int calculateBreakoutCount(BarSeries series, int endIndex, Function<Integer, Integer> trendEvaluator) {
        if (endIndex < 1) return 0;
        
        int count = 0;
        int currentTrend = trendEvaluator.apply(endIndex);
        
        for (int i = endIndex - 1; i >= 0; i--) {
            int trend = trendEvaluator.apply(i);
            if (trend != currentTrend) {
                break;
            }
            count++;
        }
        
        return count;
    }    

	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		// TODO Auto-generated method stub
		return null;
	}
	
	// TODO generate zscore calculation
	@Override
	public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
	    // Breakout count doesn't directly translate to bullish Z-score
	    return 0.0;
	}
}