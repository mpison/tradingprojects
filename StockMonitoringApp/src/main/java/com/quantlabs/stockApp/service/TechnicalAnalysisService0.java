package com.quantlabs.stockApp.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.ta4j.core.BarSeries;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.indicator.strategy.AdvancedVWAPIndicatorStrategy;
import com.quantlabs.stockApp.indicator.strategy.BreakoutCountStrategy;
import com.quantlabs.stockApp.indicator.strategy.CompositeBreakoutStrategy;
import com.quantlabs.stockApp.indicator.strategy.HeikenAshiStrategy;
import com.quantlabs.stockApp.indicator.strategy.HighestCloseOpenStrategy;
import com.quantlabs.stockApp.indicator.strategy.AbstractIndicatorStrategy;
import com.quantlabs.stockApp.indicator.strategy.MACDStrategy;
import com.quantlabs.stockApp.indicator.strategy.MovingAverageTargetValueStrategy;
import com.quantlabs.stockApp.indicator.strategy.PSARStrategy;
import com.quantlabs.stockApp.indicator.strategy.RSIStrategy;
import com.quantlabs.stockApp.indicator.strategy.TrendStrategy;
import com.quantlabs.stockApp.indicator.strategy.VolumeStrategy;
import com.quantlabs.stockApp.indicator.strategy.VolumeStrategyMA;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class TechnicalAnalysisService0 {
    
    private static final Map<String, AbstractIndicatorStrategy> strategies  = new HashMap<>();;
    
	protected ConsoleLogger logger = null;
    
    public TechnicalAnalysisService0(ConsoleLogger logger) {        
        initializeDefaultStrategies();
		this.logger = logger;
    }
    
    private static final String[] allIndicators = { "Trend", "RSI", "MACD", "MACD Breakout", "MACD(5,8,9)", "HeikenAshi",
			"MACD(5,8,9) Breakout", "PSAR(0.01)", "PSAR(0.01) Breakout", "PSAR(0.05)", "PSAR(0.05) Breakout",
			"Breakout Count", "Action", "MovingAverageTargetValue", "HighestCloseOpen", "Volume", "Volume20MA", "VWAP" };
    
    private void initializeDefaultStrategies() {
    	// Register default strategies
        registerStrategy(new TrendStrategy(logger));
        registerStrategy(new RSIStrategy(logger));
        registerStrategy(new MACDStrategy(12, 26, 9, "MACD", logger));
        registerStrategy(new MACDStrategy(5, 8, 9, "MACD(5,8,9)", logger));
        registerStrategy(new PSARStrategy(0.01, 0.02, "PSAR(0.01)", logger));
        registerStrategy(new PSARStrategy(0.05, 0.02, "PSAR(0.05)", logger));
        
        // Register new strategies
        registerStrategy(new HeikenAshiStrategy(logger));
        registerStrategy(new BreakoutCountStrategy("MACD", logger));
        registerStrategy(new BreakoutCountStrategy("MACD(5,8,9)", logger));
        registerStrategy(new BreakoutCountStrategy("PSAR(0.01)", logger));
        registerStrategy(new BreakoutCountStrategy("PSAR(0.05)", logger));
        registerStrategy(new CompositeBreakoutStrategy(logger));
        
        registerStrategy(new MovingAverageTargetValueStrategy(logger));
        registerStrategy(new HighestCloseOpenStrategy(logger));
        
        registerStrategy(new AdvancedVWAPIndicatorStrategy(logger));
        registerStrategy(new VolumeStrategy(logger));
        registerStrategy(new VolumeStrategyMA(20, logger));
    }
    
    public void registerStrategy(AbstractIndicatorStrategy strategy) {
        strategies.put(strategy.getName(), strategy);
    }
    
    public void unregisterStrategy(String strategyName) {
        strategies.remove(strategyName);
    }
    
    public static AnalysisResult performTechnicalAnalysis(BarSeries series, Set<String> indicatorsToCalculate, String timeframe, String symbol, 
    		String timeRange, String session, 
            int indexCounter, Map<String, Map<String, Integer>> timeframeIndexRanges, String currentDataSource) {
        AnalysisResult result = new AnalysisResult();
        if (series.getBarCount() == 0) return result;
        
        //int endIndex = series.getEndIndex();
        //result.setPrice(series.getBar(endIndex).getClosePrice().doubleValue());
        
        
        // Get index ranges for the timeframe
	    int startRangeIndex = timeframeIndexRanges.get(timeframe).get("startIndex");
	    int endRangeIndex = timeframeIndexRanges.get(timeframe).get("endIndex");
	    //int lastIndex = series.getBarCount() - 1;

	    // Validate indices
	    if (startRangeIndex > endRangeIndex) {
	        //logToConsole("Invalid index range for " + symbol + " (" + timeframe + "): startIndex=" + startIndex + " is greater than endIndex=" + endIndex);
	       // return result;
	    }
       
        
        // Execute only the requested strategies
        for (String indicatorName : indicatorsToCalculate) {
            AbstractIndicatorStrategy strategy = strategies.get(indicatorName);
            strategy.setSymbol(symbol);
            if (strategy != null && strategy.isEnabled()) {
                try {
                	
                	if(strategy instanceof HighestCloseOpenStrategy ) {
                		HighestCloseOpenStrategy st1 = (HighestCloseOpenStrategy) strategy;
                		st1.setCurrentDataSource(currentDataSource);
                		st1.setTf(timeframe);
                		st1.setTimeRange(timeRange);
                		st1.setSession(session);
                		st1.setIndexCounter(indexCounter);
                		strategy = st1;
                	}                	
                	
                	strategy.analyzeSymbol(series, startRangeIndex, endRangeIndex, result);
                	
                    //strategy.calculate(series, result, endIndex);
                } catch (Exception e) {
                    // Handle calculation errors
                }
            }
        }
        
        return result;
    }
    
    public AbstractIndicatorStrategy getStrategy(String name) {
        return strategies.get(name);
    }
    
    public void setStrategyEnabled(String name, boolean enabled) {
        AbstractIndicatorStrategy strategy = strategies.get(name);
        if (strategy != null) {
            strategy.setEnabled(enabled);
        }
    }
    
    public Map<String, AbstractIndicatorStrategy> getStrategies() {
        return new HashMap<>(strategies);
    }
}