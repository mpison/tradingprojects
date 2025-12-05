package com.quantlabs.stockApp.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ta4j.core.BarSeries;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.indicator.management.CustomIndicator;
import com.quantlabs.stockApp.indicator.management.StrategyConfig;
import com.quantlabs.stockApp.indicator.strategy.AbstractIndicatorStrategy;
import com.quantlabs.stockApp.indicator.strategy.AdvancedVWAPIndicatorStrategy;
import com.quantlabs.stockApp.indicator.strategy.BreakoutCountStrategy;
import com.quantlabs.stockApp.indicator.strategy.CompositeBreakoutStrategy;
import com.quantlabs.stockApp.indicator.strategy.HeikenAshiStrategy;
import com.quantlabs.stockApp.indicator.strategy.HighestCloseOpenStrategy;
import com.quantlabs.stockApp.indicator.strategy.MACDStrategy;
import com.quantlabs.stockApp.indicator.strategy.MovingAverageStrategy;
import com.quantlabs.stockApp.indicator.strategy.MovingAverageTargetValueStrategy;
import com.quantlabs.stockApp.indicator.strategy.MultiTimeframeZScoreStrategy;
import com.quantlabs.stockApp.indicator.strategy.PSARStrategy;
import com.quantlabs.stockApp.indicator.strategy.RSIStrategy;
import com.quantlabs.stockApp.indicator.strategy.TrendStrategy;
import com.quantlabs.stockApp.indicator.strategy.VolumeStrategy;
import com.quantlabs.stockApp.indicator.strategy.VolumeStrategyMA;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class TechnicalAnalysisService {
    
    private static final Map<String, AbstractIndicatorStrategy> strategies = new HashMap<>();
    
    protected ConsoleLogger logger = null;
    
    public TechnicalAnalysisService(ConsoleLogger logger) {        
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
        registerStrategy(new VolumeStrategyMA(logger));
        
        // Register common moving averages for easy access
        registerStrategy(new MovingAverageStrategy(9, "SMA", "SMA(9)", logger));
        registerStrategy(new MovingAverageStrategy(20, "SMA", "SMA(20)", logger));
        registerStrategy(new MovingAverageStrategy(50, "SMA", "SMA(50)", logger));
        registerStrategy(new MovingAverageStrategy(200, "SMA", "SMA(200)", logger));
        registerStrategy(new MovingAverageStrategy(20, "EMA", "EMA(20)", logger));
        registerStrategy(new MovingAverageStrategy(50, "EMA", "EMA(50)", logger));
    }
    
    public void registerStrategy(AbstractIndicatorStrategy strategy) {
        strategies.put(strategy.getName(), strategy);
    }
    
    public void unregisterStrategy(String strategyName) {
        strategies.remove(strategyName);
    }
    
    public static AnalysisResult performTechnicalAnalysis(BarSeries series, Set<String> indicatorsToCalculate, 
            String timeframe, String symbol, String timeRange, String session, 
            int indexCounter, Map<String, Map<String, Integer>> timeframeIndexRanges, String currentDataSource,
            Set<CustomIndicator> customIndicators) {
        
        AnalysisResult result = new AnalysisResult();
        if (series.getBarCount() == 0) return result;
        
        // Get index ranges for the timeframe
        int startRangeIndex = timeframeIndexRanges.get(timeframe).get("startIndex");
        int endRangeIndex = timeframeIndexRanges.get(timeframe).get("endIndex");

        // Validate indices
        if (startRangeIndex > endRangeIndex) {
            // Invalid index range, adjust or return empty result
            startRangeIndex = series.getEndIndex();
            endRangeIndex = series.getEndIndex();
        }
       
        // Execute standard strategies
        for (String indicatorName : indicatorsToCalculate) {
            AbstractIndicatorStrategy strategy = strategies.get(indicatorName);
            if (strategy != null && strategy.isEnabled()) {
                try {
                    strategy.setSymbol(symbol);
                    
                    // Configure strategy-specific parameters
                    if(strategy instanceof HighestCloseOpenStrategy) {
                        HighestCloseOpenStrategy st1 = (HighestCloseOpenStrategy) strategy;
                        st1.setCurrentDataSource(currentDataSource);
                        st1.setTf(timeframe);
                        st1.setTimeRange(timeRange);
                        st1.setSession(session);
                        st1.setIndexCounter(indexCounter);
                    }
                    
                    strategy.analyzeSymbol(series, startRangeIndex, endRangeIndex, result);
                    
                } catch (Exception e) {
                    // Handle calculation errors
                    System.err.println("Error calculating " + indicatorName + " for " + symbol + ": " + e.getMessage());
                }
            }
        }
        
        // Execute custom indicators and store results using setCustomIndicatorValue
        if (customIndicators != null && !customIndicators.isEmpty()) {
            for (CustomIndicator customIndicator : customIndicators) {
                try {
                    String customIndicatorName = customIndicator.getName();
                    String cType = customIndicator.getType();
                    
                    List<String> notListed = new ArrayList<>();
                    
                    notListed.add("MULTITIMEFRAMEZSCORE");
                    notListed.add("MOVINGAVERAGETARGETVALUE");
                    
                    if(!notListed.contains(cType.toUpperCase())) {
                    
	                    String customIndicatorResult = calculateCustomIndicatorValue(customIndicator, series, 
	                            startRangeIndex, endRangeIndex, timeframe, symbol, result, null);
	                    
	                    // Store the custom indicator result using the dedicated method
	                    result.setCustomIndicatorValue(customIndicatorName, customIndicatorResult);
                    
                    }
                    
                } catch (Exception e) {
                    // Handle custom indicator errors
                    System.err.println("Error calculating custom indicator " + customIndicator.getName() + 
                                     " for " + symbol + ": " + e.getMessage());
                    result.setCustomIndicatorValue(customIndicator.getName(), "Error: " + e.getMessage());
                }
            }
        }
        
        return result;
    }
    
    
    public static AnalysisResult performTechnicalAnalysisForCustomIndicators(BarSeries series, Set<String> indicatorsToCalculate, 
            String timeframe, String symbol, String timeRange, String session, 
            int indexCounter, Map<String, Map<String, Integer>> timeframeIndexRanges, String currentDataSource,
            Set<CustomIndicator> customIndicators, AnalysisResult result, PriceData priceData) {
        
        //AnalysisResult result = new AnalysisResult();
        if (series.getBarCount() == 0) return result;
        
        // Get index ranges for the timeframe
        int startRangeIndex = timeframeIndexRanges.get(timeframe).get("startIndex");
        int endRangeIndex = timeframeIndexRanges.get(timeframe).get("endIndex");

        // Validate indices
        if (startRangeIndex > endRangeIndex) {
            // Invalid index range, adjust or return empty result
            startRangeIndex = series.getEndIndex();
            endRangeIndex = series.getEndIndex();
        }
       
        // Execute standard strategies
        /*for (String indicatorName : indicatorsToCalculate) {
            IndicatorStrategy strategy = strategies.get(indicatorName);
            if (strategy != null && strategy.isEnabled()) {
                try {
                    strategy.setSymbol(symbol);
                    
                    // Configure strategy-specific parameters
                    if(strategy instanceof HighestCloseOpenStrategy) {
                        HighestCloseOpenStrategy st1 = (HighestCloseOpenStrategy) strategy;
                        st1.setCurrentDataSource(currentDataSource);
                        st1.setTf(timeframe);
                        st1.setTimeRange(timeRange);
                        st1.setSession(session);
                        st1.setIndexCounter(indexCounter);
                    }                
                    
                    strategy.analyzeSymbol(series, startRangeIndex, endRangeIndex, result);
                    
                } catch (Exception e) {
                    // Handle calculation errors
                    System.err.println("Error calculating " + indicatorName + " for " + symbol + ": " + e.getMessage());
                }
            }
        }*/
        
        // Execute custom indicators and store results using setCustomIndicatorValue
        if (customIndicators != null && !customIndicators.isEmpty()) {
            for (CustomIndicator customIndicator : customIndicators) {
                try {
                    String customIndicatorName = customIndicator.getName();
                    String cType = customIndicator.getType();
                    
                    List<String> listed = new ArrayList<>();
                    
                    listed.add("MULTITIMEFRAMEZSCORE");
                    listed.add("MOVINGAVERAGETARGETVALUE");
                    
                    if(listed.contains(cType.toUpperCase())) {
	                    String customIndicatorResult = calculateCustomIndicatorValue(customIndicator, series, 
	                            startRangeIndex, endRangeIndex, timeframe, symbol, result, priceData);
	                    
	                    // Store the custom indicator result using the dedicated method
	                    result.setCustomIndicatorValue(customIndicatorName, customIndicatorResult);
                    }
                    
                } catch (Exception e) {
                    // Handle custom indicator errors
                    System.err.println("Error calculating custom indicator " + customIndicator.getName() + 
                                     " for " + symbol + ": " + e.getMessage());
                    result.setCustomIndicatorValue(customIndicator.getName(), "Error: " + e.getMessage());
                }
            }
        }
        
        return result;
    }

    /**
     * Calculate value for a custom indicator
     * @param result 
     * @param priceData 
     */
    private static String calculateCustomIndicatorValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, String timeframe, String symbol, AnalysisResult result, PriceData priceData) {
        
        if (customIndicator == null || series == null || series.getBarCount() == 0) {
            return "N/A";
        }
        
        String type = customIndicator.getType().toUpperCase();
        Map<String, Object> parameters = customIndicator.getParameters();
        
        try {
            switch (type) {
                case "MACD":
                    return calculateMACDValue(customIndicator, series, startIndex, endIndex, parameters);
                    
                case "RSI":
                    return calculateRSIValue(customIndicator, series, startIndex, endIndex, parameters);
                    
                case "PSAR":
                    return calculatePSARValue(customIndicator, series, startIndex, endIndex, parameters);
                    
                case "TREND":
                    return calculateTrendValue(customIndicator, series, startIndex, endIndex, parameters);
                    
                case "VOLUMEMA":
                    return calculateVolumeMAValue(customIndicator, series, startIndex, endIndex, parameters);
                    
                case "MOVINGAVERAGE":
                    return calculateMovingAverageValue(customIndicator, series, startIndex, endIndex, parameters);
                    
                case "HIGHESTCLOSEOPEN":
                    return calculateHighestCloseOpenValue(customIndicator, series, startIndex, endIndex, parameters, timeframe, symbol);
                    
                case "VWAP":
                    return calculateVWAPValue(customIndicator, series, startIndex, endIndex, parameters);
                    
                case "HEIKENASHI":
                    return calculateHeikenAshiValue(customIndicator, series, startIndex, endIndex, parameters);
                    
                case "MOVINGAVERAGETARGETVALUE":
                    return calculateMovingAverageTargetValue(customIndicator, series, startIndex, endIndex, parameters, timeframe, symbol, result, priceData);
                
                case "MULTITIMEFRAMEZSCORE":
                    return calculateMultiTimeframeZScore(customIndicator, series, startIndex, endIndex, 
                                                         parameters, timeframe, symbol, result, priceData);    
                    
                default:
                    return "Unknown indicator type: " + type;
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    private static String calculateMultiTimeframeZScore(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters, 
            String timeframe, String symbol, AnalysisResult result, PriceData priceData) {
        
        try {
            // Get selected timeframes from parameters
            List<String> selectedTimeframes = new ArrayList<>();
            Object selectedTimeframesObj = parameters.get("selectedTimeframes");
            
            if (selectedTimeframesObj instanceof String) {
                String selectedTimeframesStr = (String) selectedTimeframesObj;
                if (!selectedTimeframesStr.isEmpty()) {
                    selectedTimeframes = Arrays.asList(selectedTimeframesStr.split(","));
                }
            } else {
                // Fallback: check individual timeframe parameters
                String[] availableTimeframes = {"1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min"};
                for (String tf : availableTimeframes) {
                    Object tfSelected = parameters.get("timeframe_" + tf);
                    if (tfSelected instanceof Boolean && (Boolean) tfSelected) {
                        selectedTimeframes.add(tf);
                    }
                }
            }
            
            if (selectedTimeframes.isEmpty()) {
                return "No timeframes selected";
            }
            
            // Get the strategy configuration (you'll need to pass this or retrieve it)
            // For now, we'll create a simple configuration
            StrategyConfig strategyConfig = new StrategyConfig();
            
            if(parameters.get("strategyConfig") != null) {
            	
            	strategyConfig = (StrategyConfig) parameters.get("strategyConfig");
            	
            }else {
            
	            strategyConfig.setName("MultiTimeframeZScore_" + customIndicator.getName());
	            
	            // Set timeframe weights (equal weights by default)
	            Map<String, Integer> timeframeWeights = new HashMap<>();
	            int weightPerTimeframe = 100 / selectedTimeframes.size();
	            int remainder = 100 % selectedTimeframes.size();
	            
	            int i = 0;
	            for (String tf : selectedTimeframes) {
	                int weight = weightPerTimeframe + (i < remainder ? 1 : 0);
	                timeframeWeights.put(tf, weight);
	                i++;
	            }
	            
	            strategyConfig.getParameters().put("zscoreWeights", timeframeWeights);
            }
            
            // Create and execute the MultiTimeframeZScoreStrategy
            MultiTimeframeZScoreStrategy strategy = new MultiTimeframeZScoreStrategy(
                strategyConfig, priceData, null // logger
            );
            
            // Calculate Z-Score
            double zscore = strategy.calculateZscore(series, result, series.getEndIndex());
            
            // Store in result for later use
            result.addCustomValue("MultiTimeframeZScore_" + customIndicator.getName(), zscore);
            
            // Return formatted result
            return String.format("Z-Score: %.1f/100 (Timeframes: %s)", zscore, String.join(", ", selectedTimeframes));
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Custom indicator calculation methods
    private static String calculateMACDValue(CustomIndicator customIndicator, BarSeries series, 
            int startIndex, int endIndex, Map<String, Object> parameters) {
        
        int fast = getIntParameter(parameters, "fast", 12);
        int slow = getIntParameter(parameters, "slow", 26);
        int signal = getIntParameter(parameters, "signal", 9);
        
        // Use existing MACDStrategy to calculate
        MACDStrategy macdStrategy = new MACDStrategy(fast, slow, signal, customIndicator.getName(), null);
        AnalysisResult tempResult = new AnalysisResult();
        macdStrategy.analyzeSymbol(series, startIndex, endIndex, tempResult);
        
        String macdName = macdStrategy.getName();
        
        // Get MACD values from the result
        if(!"MACD(5,8,9)".equals(macdName)) {
	        double macdValue = tempResult.getMacd();
	        double signalValue = tempResult.getMacdSignal();
	        String status = tempResult.getMacdStatus();
	        
	        return String.format("MACD: %.4f, Signal: %.4f, Status: %s", macdValue, signalValue, status);
        }else {
        	double macdValue = tempResult.getMacd359();
	        double signalValue = tempResult.getMacdSignal359();
	        String status = tempResult.getMacd359Status();
	        
	        return String.format("MACD: %.4f, Signal: %.4f, Status: %s", macdValue, signalValue, status);
        }
    }

    private static String calculateRSIValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters) {
        
        int period = getIntParameter(parameters, "period", 14);
        
        // Use existing RSIStrategy to calculate
        RSIStrategy rsiStrategy = new RSIStrategy(null);
        AnalysisResult tempResult = new AnalysisResult();
        rsiStrategy.analyzeSymbol(series, startIndex, endIndex, tempResult);
        
        double rsiValue = tempResult.getRsi();
        String trend = tempResult.getRsiTrend();
        
        return String.format("RSI: %.2f, Trend: %s", rsiValue, trend);
    }

    private static String calculatePSARValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters) {
        
        double step = getDoubleParameter(parameters, "step", 0.02);
        double maxStep = getDoubleParameter(parameters, "max", 0.2);
        
        // Use existing PSARStrategy to calculate
        PSARStrategy psarStrategy = new PSARStrategy(step, maxStep, customIndicator.getName(), null);
        AnalysisResult tempResult = new AnalysisResult();
        psarStrategy.analyzeSymbol(series, startIndex, endIndex, tempResult);
        
        // Determine which PSAR value to use based on step
        double psarValue;
        String trend;
        if (step == 0.01) {
            psarValue = tempResult.getPsar001();
            trend = tempResult.getPsar001Trend();
        } else if(step== 0.05){
            psarValue = tempResult.getPsar005();
            trend = tempResult.getPsar005Trend();
        } else {
            psarValue = tempResult.getPsar();
            trend = tempResult.getPsarTrend();
        }
        
        return String.format("PSAR: %.4f, Trend: %s", psarValue, trend);
    }

    private static String calculateTrendValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters) {
        
        // Use existing TrendStrategy to calculate
        TrendStrategy trendStrategy = new TrendStrategy(null);
        AnalysisResult tempResult = new AnalysisResult();
        trendStrategy.analyzeSymbol(series, startIndex, endIndex, tempResult);
        
        double sma20 = tempResult.getSma20();
        double sma200 = tempResult.getSma200();
        String trend = tempResult.getSmaTrend();
        
        return String.format("SMA20: %.2f, SMA200: %.2f, Trend: %s", sma20, sma200, trend);
    }

    private static String calculateVolumeMAValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters) {
        
        int period = getIntParameter(parameters, "period", 20);
        
        // Use existing VolumeStrategyMA to calculate
        VolumeStrategyMA volumeStrategy = new VolumeStrategyMA(null);
        volumeStrategy.setPeriod(period);
        AnalysisResult tempResult = new AnalysisResult();
        volumeStrategy.analyzeSymbol(series, startIndex, endIndex, tempResult);
        
        //double volumeMA = tempResult.getVolume20MA();
        
        return tempResult.getVolume20Trend();//String.format("VolumeMA(%d): %.0f", period, volumeMA);
    }

    
    private static String calculateMovingAverageValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters) {
        
        int period = getIntParameter(parameters, "period", 20);
        String maType = getStringParameter(parameters, "type", "SMA");
        
        // Use the new MovingAverageStrategy
        MovingAverageStrategy maStrategy = new MovingAverageStrategy(period, maType, customIndicator.getName(), null);
        AnalysisResult tempResult = new AnalysisResult();
        
        // Use the last index for calculation (most recent value)
        int lastIndex = series.getEndIndex();
        maStrategy.calculate(series, tempResult, lastIndex);
        
        // Get the moving average value from custom indicator storage
        String maValueStr = tempResult.getCustomIndicatorValue(customIndicator.getName());
        String trend = tempResult.getCustomIndicatorValue(customIndicator.getName() + "_Trend");
        
        double maValue;
        try {
            maValue = Double.parseDouble(maValueStr);
        } catch (NumberFormatException e) {
            maValue = Double.NaN;
        }
        
        if (Double.isNaN(maValue)) {
            return String.format("%s(%d): N/A", maType, period);
        } else {
            return String.format("%s(%d): %.2f, Trend: %s", maType, period, maValue, trend);
        }
    }
    
    /*private static String calculateMovingAverageValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters) {
        
        int period = getIntParameter(parameters, "period", 20);
        String maType = getStringParameter(parameters, "type", "SMA");
        
        // Use TrendStrategy for SMA calculation
        TrendStrategy trendStrategy = new TrendStrategy(null);
        AnalysisResult tempResult = new AnalysisResult();
        trendStrategy.analyzeSymbol(series, startIndex, endIndex, tempResult);
        
        double maValue;
        if (period <= 20) {
            maValue = tempResult.getSma20();
        } else {
            maValue = tempResult.getSma200();
        }
        
        return String.format("%s(%d): %.2f", maType, period, maValue);
    }*/

    private static String calculateHighestCloseOpenValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters, String timeframe, String symbol) {
        
        String hcoTimeRange = getStringParameter(parameters, "timeRange", "1D");
        String hcoSession = getStringParameter(parameters, "session", "all");
        int hcoLookback = getIntParameter(parameters, "lookback", 5);
        
        // Use existing HighestCloseOpenStrategy to calculate
        HighestCloseOpenStrategy hcoStrategy = new HighestCloseOpenStrategy(null);
        hcoStrategy.setTimeRange(hcoTimeRange);
        hcoStrategy.setSession(hcoSession);
        hcoStrategy.setIndexCounter(hcoLookback);
        hcoStrategy.setTf(timeframe);
        hcoStrategy.setSymbol(symbol);
        
        AnalysisResult tempResult = new AnalysisResult();
        hcoStrategy.analyzeSymbol(series, startIndex, endIndex, tempResult);
        
        double highestValue = tempResult.getHighestCloseOpen();
        String status = tempResult.getHighestCloseOpenStatus();
        
        return String.format("Value: %.2f, Status: %s", highestValue, status);
    }

    private static String calculateVWAPValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters) {
        
        // Use existing VWAP strategy to calculate
        AdvancedVWAPIndicatorStrategy vwapStrategy = new AdvancedVWAPIndicatorStrategy(null);
        AnalysisResult tempResult = new AnalysisResult();
        vwapStrategy.analyzeSymbol(series, startIndex, endIndex, tempResult);
        
        String status = tempResult.getVwapStatus();
        
        return String.format("VWAP Status: %s", status);
    }

    private static String calculateHeikenAshiValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters) {
        
        // Use existing HeikenAshi strategy to calculate
        HeikenAshiStrategy haStrategy = new HeikenAshiStrategy(null);
        AnalysisResult tempResult = new AnalysisResult();
        haStrategy.analyzeSymbol(series, startIndex, endIndex, tempResult);
        
        double close = tempResult.getHeikenAshiClose();
        double open = tempResult.getHeikenAshiOpen();
        String trend = tempResult.getHeikenAshiTrend();
        
        return String.format("Close: %.2f, Open: %.2f, Trend: %s", close, open, trend);
    }
    
    private static String calculateMovingAverageTargetValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters, String timeframe, String symbol, AnalysisResult result, PriceData priceData) {
        
        try {
            // Extract parameters with defaults
            int ma1Period = getIntParameter(parameters, "ma1Period", 20);
            int ma2Period = getIntParameter(parameters, "ma2Period", 50);
            String ma1PriceType = getStringParameter(parameters, "ma1PriceType", "CLOSE");
            String ma2PriceType = getStringParameter(parameters, "ma2PriceType", "CLOSE");
            String ma1Type = getStringParameter(parameters, "ma1Type", "SMA");
            String ma2Type = getStringParameter(parameters, "ma2Type", "SMA");
            String trendSource = getStringParameter(parameters, "trendSource", "PSAR_0.01");
            
            //other parameters
            int lastIndexParam = getIntParameter(parameters, "lastIndex", 0);
            boolean isTrendParam = getBooleanParameter(parameters, "trend", false);
            boolean isValueParam = getBooleanParameter(parameters, "value", false);
            boolean isPercentileParam = getBooleanParameter(parameters, "percentile", false);
            boolean isTargetValueParam = getBooleanParameter(parameters, "targetValue", false);
            
            int countDisplayEnabledParam = 0;
            
            
            if(isTrendParam)countDisplayEnabledParam++;
            if(isValueParam)countDisplayEnabledParam++;
            if(isPercentileParam)countDisplayEnabledParam++;
            if(isTargetValueParam)countDisplayEnabledParam++;
            
            // Use the last index for calculation (most recent value)
            int lastIndex = series.getEndIndex() - lastIndexParam;
            
            // Check if we have enough data
            int maxPeriod = Math.max(ma1Period, ma2Period);
            if (lastIndex < maxPeriod - 1) {
                return String.format("Insufficient data (need %d bars, have %d)", maxPeriod, lastIndex + 1);
            }
            
            // Create the strategy instance
            MovingAverageTargetValueStrategy strategy = new MovingAverageTargetValueStrategy(ma1Period, ma2Period, ma1PriceType, ma2PriceType, ma1Type, ma2Type, trendSource, null);
            strategy.setSymbol(symbol);
            
            // Create a temporary result object
            AnalysisResult tempResult = new AnalysisResult();
            
            // Calculate using the strategy
            strategy.calculate(series, tempResult, lastIndex);
            
            // Get the target value
            Double targetValue = tempResult.getMovingAverageTargetValue();
            Double targetPercentile = tempResult.getMovingAverageTargetValuePercentile();
            
            if (targetValue == null || Double.isNaN(targetValue)) {
                return "N/A - Calculation error";
            }
            
            // Get current price for context
            double currentPrice = series.getBar(lastIndex).getClosePrice().doubleValue();
            double difference = currentPrice-targetValue;
            double percentDifference = (difference / currentPrice) * 100;
            
            // Determine position relative to current price
            String position = targetValue > currentPrice ? "Above" : "Below";
            String trendDirection = percentDifference > 0 ? "Bullish" : "Bearish";
            
            double tagetFromHighest = priceData.getHigh() - targetValue;
            double tagetFromLow = (priceData.getLow() < 1000) ? priceData.getLow() + targetValue : 0;            
            
            if(countDisplayEnabledParam > 2) {            
            	//return String.format("Target: %.2f, TargetPerctle: %.2f, Current: %.2f, %s by %.2f%% (%s)", 
                //               targetValue, targetPercentile, currentPrice, position, Math.abs(percentDifference), trendDirection);
            	
            	return String.format("Target: %.2f - FrHigh: %.2f / FrLow: %.2f, TargetPerctle: %.2f, Current: %.2f, %s by %.2f%% (%s)", 
                        targetValue, tagetFromHighest, tagetFromLow, targetPercentile, currentPrice, position, Math.abs(percentDifference), trendDirection);
            	
            	//return String.format("Target: %.2f - FrHigh: %.2f / FrLow: %.2f, TargetPerctle: %.2f, Current: %.2f, %s by %.2f%% (%s)", 
                //      targetValue, tagetFromHighest, tagetFromLow, targetPercentile, currentPrice, position, Math.abs(percentDifference), trendDirection);
            }else {            
            	String returnParam = String.format("%.2f", targetPercentile);
            	if(isTrendParam) {
            		returnParam = String.format("Current: %.2f, %s by %.2f%% (%s)", currentPrice, position, Math.abs(percentDifference), trendDirection);
            	}
            	if(isValueParam || isTargetValueParam) {
            		returnParam = String.format("%.2f", targetValue);
            	}
            	if(isPercentileParam) {
            		returnParam = String.format("%.2f", targetPercentile);
            	}
            	return returnParam;
            }
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Keep the original method for backward compatibility
    public static AnalysisResult performTechnicalAnalysis(BarSeries series, Set<String> indicatorsToCalculate, 
            String timeframe, String symbol, String timeRange, String session, 
            int indexCounter, Map<String, Map<String, Integer>> timeframeIndexRanges, String currentDataSource) {
        
        return performTechnicalAnalysis(series, indicatorsToCalculate, timeframe, symbol, timeRange, session,
                indexCounter, timeframeIndexRanges, currentDataSource, null);
    }

    // Parameter helper methods (keep the same as before)
    private static int getIntParameter(Map<String, Object> parameters, String key, int defaultValue) {
        if (parameters == null || !parameters.containsKey(key)) {
            return defaultValue;
        }
        try {
            Object value = parameters.get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
 // Parameter helper methods (keep the same as before)
    private static boolean getBooleanParameter(Map<String, Object> parameters, String key, boolean defaultValue) {
        if (parameters == null || !parameters.containsKey(key)) {
            return defaultValue;
        }
        try {
            Object value = parameters.get(key);
            if (value instanceof Boolean) {
                return ((Boolean) value);
            } else if (value instanceof String) {
                return Boolean.valueOf((String) value);
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    
    /**
     * Creates strategy instances from custom indicator definitions
     */
    private static AbstractIndicatorStrategy createCustomIndicatorStrategy(CustomIndicator customIndicator) {
        if (customIndicator == null) return null;
        
        String type = customIndicator.getType().toUpperCase();
        Map<String, Object> parameters = customIndicator.getParameters();
        
        try {
            switch (type) {
                case "MACD":
                    int fast = getIntParameter(parameters, "fast", 12);
                    int slow = getIntParameter(parameters, "slow", 26);
                    int signal = getIntParameter(parameters, "signal", 9);
                    return new MACDStrategy(fast, slow, signal, customIndicator.getName(), null);
                    
                case "RSI":
                    int period = getIntParameter(parameters, "period", 14);
                    // Create a parameterized RSI strategy - you may need to modify RSIStrategy to accept period
                    RSIStrategy rsiStrategy = new RSIStrategy(null);
                    // Set custom period if your RSIStrategy supports it
                    return rsiStrategy;
                    
                case "PSAR":
                    double step = getDoubleParameter(parameters, "step", 0.02);
                    double maxStep = getDoubleParameter(parameters, "max", 0.2);
                    return new PSARStrategy(step, maxStep, customIndicator.getName(), null);
                    
                case "TREND":
                    int sma1 = getIntParameter(parameters, "sma1", 9);
                    int sma2 = getIntParameter(parameters, "sma2", 20);
                    int sma3 = getIntParameter(parameters, "sma3", 200);
                    // You may need to modify TrendStrategy to accept custom periods
                    return new TrendStrategy(null);
                    
                case "VOLUMEMA":
                    int volumePeriod = getIntParameter(parameters, "period", 20);
                    // You may need to modify VolumeStrategyMA to accept custom period
                    return new VolumeStrategyMA(null);
                    
                case "MOVINGAVERAGE":
                    int maPeriod = getIntParameter(parameters, "period", 20);
                    String maType = getStringParameter(parameters, "type", "SMA");
                    // Create appropriate moving average strategy based on type
                    if ("EMA".equalsIgnoreCase(maType)) {
                        // You'd need an EMA strategy class
                    } else {
                        // Default to SMA - you may need an SMA strategy class
                    }
                    return new TrendStrategy(null); // Fallback to trend strategy
                    
                case "HIGHESTCLOSEOPEN":
                    String hcoTimeRange = getStringParameter(parameters, "timeRange", "1D");
                    String hcoSession = getStringParameter(parameters, "session", "all");
                    int hcoLookback = getIntParameter(parameters, "lookback", 5);
                    HighestCloseOpenStrategy hcoStrategy = new HighestCloseOpenStrategy(null);
                    hcoStrategy.setTimeRange(hcoTimeRange);
                    hcoStrategy.setSession(hcoSession);
                    hcoStrategy.setIndexCounter(hcoLookback);
                    return hcoStrategy;
                    
                case "VWAP":
                    return new AdvancedVWAPIndicatorStrategy(null);
                    
                case "HEIKENASHI":
                    return new HeikenAshiStrategy(null);
                    
                case "MOVINGAVERAGETARGETVALUE":
                    int ma1Period = getIntParameter(parameters, "ma1Period", 20);
                    int ma2Period = getIntParameter(parameters, "ma2Period", 50);
                    String ma1PriceType = getStringParameter(parameters, "ma1PriceType", "CLOSE");
                    String ma2PriceType = getStringParameter(parameters, "ma2PriceType", "CLOSE");
                    String ma1Type = getStringParameter(parameters, "ma1Type", "SMA");
                    String ma2Type = getStringParameter(parameters, "ma2Type", "SMA");
                    String trendSource = getStringParameter(parameters, "trendSource", "PSAR_0.01");
                    
                    // Create strategy with explicit parameters
                    return MovingAverageTargetValueStrategy.withParameters(
                        ma1Period, ma2Period, ma1PriceType, ma2PriceType, 
                        ma1Type, ma2Type, trendSource, null
                    );  
                    
                default:
                    System.err.println("Unknown custom indicator type: " + type);
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Error creating custom indicator strategy for " + customIndicator.getName() + 
                             ": " + e.getMessage());
            return null;
        }
    }
    
    private static double getDoubleParameter(Map<String, Object> parameters, String key, double defaultValue) {
        if (parameters == null || !parameters.containsKey(key)) {
            return defaultValue;
        }
        try {
            Object value = parameters.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private static String getStringParameter(Map<String, Object> parameters, String key, String defaultValue) {
        if (parameters == null || !parameters.containsKey(key)) {
            return defaultValue;
        }
        Object value = parameters.get(key);
        return value != null ? value.toString() : defaultValue;
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
    
    /**
     * Method to register custom strategies dynamically
     */
    public void registerCustomStrategy(AbstractIndicatorStrategy strategy) {
        if (strategy != null) {
            strategies.put(strategy.getName(), strategy);
        }
    }
    
    /**
     * Method to check if a strategy exists
     */
    public boolean hasStrategy(String name) {
        return strategies.containsKey(name);
    }
}