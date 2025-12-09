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
import com.quantlabs.stockApp.indicator.management.IndicatorsManagementApp;
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
    
    //private static final String[] allIndicators = { "Trend", "RSI", "MACD", "MACD Breakout", "MACD(5,8,9)", "HeikenAshi",
     //       "MACD(5,8,9) Breakout", "PSAR(0.01)", "PSAR(0.01) Breakout", "PSAR(0.05)", "PSAR(0.05) Breakout",
      //      "Breakout Count", "Action", "MovingAverageTargetValue", "HighestCloseOpen", "Volume", "Volume20MA", "VWAP" };
    
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
	                            startRangeIndex, endRangeIndex, timeframe, symbol, result, null, null);
	                    
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
            Set<CustomIndicator> customIndicators, AnalysisResult result, PriceData priceData, IndicatorsManagementApp indicatorsManagementApp) {
        
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
	                            startRangeIndex, endRangeIndex, timeframe, symbol, result, priceData, indicatorsManagementApp);
	                    
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
     * @param indicatorsManagementApp 
     */
    private static String calculateCustomIndicatorValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, String timeframe, String symbol, AnalysisResult result, PriceData priceData, IndicatorsManagementApp indicatorsManagementApp) {
        
        if (customIndicator == null || series == null || series.getBarCount() == 0) {
            return "N/A";
        }
        
        String type = customIndicator.getType().toUpperCase();
        Map<String, Object> parameters = customIndicator.getParameters();
        
        try {
            switch (type) {
                case "MACD":
                    return calculateCustomMACDValue(customIndicator, series, startIndex, endIndex, parameters, result);
                    
                case "RSI":
                    return calculateCustomRSIValue(customIndicator, series, startIndex, endIndex, parameters, result);
                    
                case "PSAR":
                    return calculateCustomPSARValue(customIndicator, series, startIndex, endIndex, parameters, result);
                    
                case "TREND":
                    return calculateCustomTrendValue(customIndicator, series, startIndex, endIndex, parameters, result);
                    
                case "VOLUMEMA":
                    return calculateCustomVolumeMAValue(customIndicator, series, startIndex, endIndex, parameters, result);
                    
                case "MOVINGAVERAGE":
                    return calculateCustomMovingAverageValue(customIndicator, series, startIndex, endIndex, parameters, result);
                    
                case "HIGHESTCLOSEOPEN":
                    return calculateCustomHighestCloseOpenValue(customIndicator, series, startIndex, endIndex, parameters, timeframe, symbol, result);
                    
                case "VWAP":
                    return calculateCustomVWAPValue(customIndicator, series, startIndex, endIndex, parameters, result);
                    
                case "HEIKENASHI":
                    return calculateCustomHeikenAshiValue(customIndicator, series, startIndex, endIndex, parameters, result);
                    
                case "MOVINGAVERAGETARGETVALUE":
                    return calculateCustomMovingAverageTargetValue(customIndicator, series, startIndex, endIndex, parameters, timeframe, symbol, result, priceData);
                
                case "MULTITIMEFRAMEZSCORE":
                    return calculateCustomMultiTimeframeZScore(customIndicator, series, startIndex, endIndex, 
                                                         parameters, timeframe, symbol, result, priceData, indicatorsManagementApp);    
                    
                default:
                    return "Unknown indicator type: " + type;
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    private static String calculateCustomMultiTimeframeZScore(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters, 
            String timeframe, String symbol, AnalysisResult result, PriceData priceData, IndicatorsManagementApp indicatorsManagementApp) {
        
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
                strategyConfig, priceData, indicatorsManagementApp, null // logger
            );
            
            strategy.setIndicatorsManagementApp(indicatorsManagementApp);
            
            // Calculate Z-Score
            double zscore = strategy.calculateZscore(series, result, series.getEndIndex());
            
            // Store in result for later use
            result.addCustomValue("MultiTimeframeZScore_" + customIndicator.getName(), zscore);
            
            // Return formatted result
            //return String.format("Z-Score: %.1f/100 (Timeframes: %s)", zscore, String.join(", ", selectedTimeframes));
            
            return String.format("Z-Score: %.1f/100 (Timeframes: %s)", zscore, strategy.getAdditionalNotes());
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Custom indicator calculation methods
    private static String calculateCustomMACDValue(CustomIndicator customIndicator, BarSeries series, 
            int startIndex, int endIndex, Map<String, Object> parameters, AnalysisResult analysisResult) {
        
        int fast = getIntParameter(parameters, "fast", 12);
        int slow = getIntParameter(parameters, "slow", 26);
        int signal = getIntParameter(parameters, "signal", 9);
        
        // Use existing MACDStrategy to calculate
        MACDStrategy macdStrategy = new MACDStrategy(fast, slow, signal, customIndicator.getName(), null);
        //AnalysisResult tempResult = new AnalysisResult();
        macdStrategy.analyzeSymbol(series, startIndex, endIndex, analysisResult);
        
        /*
        String macdName = macdStrategy.getName();
        
        // Get MACD values from the result
        if("MACD(5,8,9)".equals(macdName)) {
        	double macdValue = analysisResult.getMacd359();
	        double signalValue = analysisResult.getMacdSignal359();
	        String status = analysisResult.getMacd359Status();
	        
	        return String.format("MACD: %.4f, Signal: %.4f, Status: %s", macdValue, signalValue, status);
        }else if(fast == 12 && slow == 26 && signal == 9){
        	
	        double macdValue = analysisResult.getMacd();
	        double signalValue = analysisResult.getMacdSignal();
	        String status = analysisResult.getMacdStatus();
	        
	        return String.format("MACD: %.4f, Signal: %.4f, Status: %s", macdValue, signalValue, status);
        }else{
        	
        	double macdValue = Double.valueOf(analysisResult.getCustomIndicatorValue(macdName +"_MACD"));
	        double signalValue = Double.valueOf(analysisResult.getCustomIndicatorValue(macdName +"_MACDSignal"));
	        String status = (String) analysisResult.getCustomIndicatorValue(macdName +"_MACDStatus");
	        
	        return String.format("MACD: %.4f, Signal: %.4f, Status: %s", macdValue, signalValue, status);
        }*/
        
        double macdValue = macdStrategy.getMACDValue(analysisResult);
        double signalValue = macdStrategy.getMACDSignal(analysisResult);
        String status = (String) macdStrategy.getMACDStatus(analysisResult);
        
        return String.format("MACD: %.4f, Signal: %.4f, Status: %s", macdValue, signalValue, status);
    }

    private static String calculateCustomRSIValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters, AnalysisResult analysisResult) {
        
        int period = getIntParameter(parameters, "period", 14);
        
        // Use existing RSIStrategy to calculate
        RSIStrategy rsiStrategy = (RSIStrategy) createCustomIndicatorStrategy(customIndicator);
        //AnalysisResult tempResult = new AnalysisResult();
        rsiStrategy.analyzeSymbol(series, startIndex, endIndex, analysisResult);
        
		/*
		 * if(rsiStrategy.getPeriod() == 14) { double rsiValue =
		 * analysisResult.getRsi(); String trend = analysisResult.getRsiTrend();
		 * 
		 * return String.format("RSI: %.2f, Trend: %s", rsiValue, trend); }else {
		 * 
		 * String rsiName = rsiStrategy.getName();
		 * 
		 * double rsiValue =
		 * Double.valueOf(analysisResult.getCustomIndicatorValue(rsiName +"_RSI"));
		 * String trend = (String) (analysisResult.getCustomIndicatorValue(rsiName
		 * +"_RSITrend"));
		 * 
		 * return String.format("RSI: %.2f, Trend: %s", rsiValue, trend); }
		 */
        
        double rsiValue = rsiStrategy.getRSIValue(analysisResult);
        String trend = (String)  rsiStrategy.getRSITrend(analysisResult);
        
        return String.format("RSI: %.2f, Trend: %s", rsiValue, trend);
    }

    private static String calculateCustomPSARValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters, AnalysisResult analysisResult) {
        
        //double step = getDoubleParameter(parameters, "step", 0.02);
        //double maxStep = getDoubleParameter(parameters, "max", 0.2);
        
        // Use existing PSARStrategy to calculate
        PSARStrategy psarStrategy = (PSARStrategy) createCustomIndicatorStrategy(customIndicator); 
        //new PSARStrategy(step, maxStep, customIndicator.getName(), null);
        //AnalysisResult tempResult = new AnalysisResult();
        psarStrategy.analyzeSymbol(series, startIndex, endIndex, analysisResult);
        
        // Determine which PSAR value to use based on step
        double psarValue;
        
        String psarName = psarStrategy.getName();
        
        String trend;
		/*
		 * if (step == 0.01) { psarValue = analysisResult.getPsar001(); trend =
		 * analysisResult.getPsar001Trend(); } else if(step== 0.05){ psarValue =
		 * analysisResult.getPsar005(); trend = analysisResult.getPsar005Trend(); } else
		 * { psarValue = Double.valueOf(analysisResult.getCustomIndicatorValue(psarName
		 * +"_PSAR")); trend =
		 * String.valueOf(analysisResult.getCustomIndicatorValue(psarName
		 * +"_PSARTrend")); }
		 */
        
        psarValue = psarStrategy.getPsarValue(analysisResult);
        trend = (String) psarStrategy.getPsarTrend(analysisResult);
        
        return String.format("PSAR: %.4f, Trend: %s", psarValue, trend);
    }

    private static String calculateCustomTrendValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters, AnalysisResult analysisResult) {
        
        // Use existing TrendStrategy to calculate
        TrendStrategy trendStrategy = (TrendStrategy) createCustomIndicatorStrategy(customIndicator); //new TrendStrategy(sma1, sma2, sma3, null);
        //AnalysisResult tempResult = new AnalysisResult();
        trendStrategy.analyzeSymbol(series, startIndex, endIndex, analysisResult);
        
		/*
		 * if(trendStrategy.getSma1() == 9 && trendStrategy.getSma2() == 20 &&
		 * trendStrategy.getSma3() == 200) { double sma20 = analysisResult.getSma20();
		 * double sma200 = analysisResult.getSma200(); String trend =
		 * analysisResult.getSmaTrend();
		 * 
		 * return String.format("SMA20: %.2f, SMA200: %.2f, Trend: %s", sma20, sma200,
		 * trend);
		 * 
		 * }else {
		 * 
		 * String trendName = trendStrategy.getName();
		 * 
		 * double sm1 = Double.valueOf(analysisResult.getCustomIndicatorValue(trendName
		 * +"_TREND1")); double sm2 =
		 * Double.valueOf(analysisResult.getCustomIndicatorValue(trendName +"_TREND1"));
		 * double sm3 = Double.valueOf(analysisResult.getCustomIndicatorValue(trendName
		 * +"_TREND1"));
		 * 
		 * String trend =
		 * String.valueOf(analysisResult.getCustomIndicatorValue(trendName
		 * +"_TRENDTrend"));
		 * 
		 * return String.format("SMA1: %.2f, SMA2: %.2f, SMA3: %.2f, Trend: %s", sm1,
		 * sm2, sm3, trend);
		 * 
		 * }
		 */
        
        return trendStrategy.getValueTrendValue(analysisResult);
    }

    private static String calculateCustomVolumeMAValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters, AnalysisResult analysisResult) {
        
        
        // Use existing VolumeStrategyMA to calculate
        VolumeStrategyMA volumeStrategy = (VolumeStrategyMA) createCustomIndicatorStrategy(customIndicator); //new VolumeStrategyMA(period, null);
        
        volumeStrategy.analyzeSymbol(series, startIndex, endIndex, analysisResult);
		/*
		 * 
		 * 
		 * if(volumeStrategy.getPeriod() == 20 ) { String trend =
		 * analysisResult.getVolume20Trend(); double volumeMA =
		 * analysisResult.getVolume20MA();
		 * 
		 * return String.format("VolumeMA(%d): %.0f, Trend: %s",
		 * volumeStrategy.getPeriod(), volumeMA, trend);
		 * 
		 * }else {
		 * 
		 * String trendName = volumeStrategy.getName();
		 * 
		 * double volumeMA=
		 * Double.valueOf(analysisResult.getCustomIndicatorValue(trendName
		 * +"_VOLUMEMA")); String trend =
		 * String.valueOf(analysisResult.getCustomIndicatorValue(trendName
		 * +"_VOLUMEMATrend"));
		 * 
		 * return String.format("VolumeMA(%d): %.0f, Trend: %s",
		 * volumeStrategy.getPeriod(), volumeMA, trend);
		 * 
		 * }
		 */
        
        String trend = volumeStrategy.getTrend(analysisResult);
        double volumeMA = volumeStrategy.getVolumeMA(analysisResult);
        
        return String.format("VolumeMA(%d): %.0f, Trend: %s", volumeStrategy.getPeriod(), volumeMA, trend);
    }

    
    private static String calculateCustomMovingAverageValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters,  AnalysisResult analysisResult) {
        
        int period = getIntParameter(parameters, "period", 20);
        String maType = getStringParameter(parameters, "type", "SMA");
        
        // Use the new MovingAverageStrategy
        MovingAverageStrategy maStrategy = (MovingAverageStrategy)createCustomIndicatorStrategy(customIndicator);//new MovingAverageStrategy(period, maType, customIndicator.getName(), null);
        //AnalysisResult tempResult = new AnalysisResult();
        
        // Use the last index for calculation (most recent value)
        int lastIndex = series.getEndIndex();
        maStrategy.calculate(series, analysisResult, lastIndex);
        
        // Get the moving average value from custom indicator storage
        String maValueStr = maStrategy.getMAValue(analysisResult);	//analysisResult.getCustomIndicatorValue(customIndicator.getName());
        String trend = maStrategy.getMATrend(analysisResult);		//analysisResult.getCustomIndicatorValue(customIndicator.getName() + "_MATrend");
        
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

    private static String calculateCustomHighestCloseOpenValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters, String timeframe, String symbol, AnalysisResult analysisResult) {
        
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
        
        //AnalysisResult tempResult = new AnalysisResult();
        hcoStrategy.analyzeSymbol(series, startIndex, endIndex, analysisResult);
        
        double highestValue = hcoStrategy.getHighestCloseOpen(analysisResult); 
        String status = hcoStrategy.getHighestCloseOpenStatus(analysisResult); 
        
        return String.format("Value: %.2f, Status: %s", highestValue, status);
    }

    private static String calculateCustomVWAPValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters, AnalysisResult result) {
        
        // Use existing VWAP strategy to calculate
        AdvancedVWAPIndicatorStrategy vwapStrategy = new AdvancedVWAPIndicatorStrategy(null);
        //AnalysisResult tempResult = new AnalysisResult();
        vwapStrategy.analyzeSymbol(series, startIndex, endIndex, result);
        
        String status = result.getVwapStatus();
        
        return String.format("VWAP Status: %s", status);
    }

    private static String calculateCustomHeikenAshiValue(CustomIndicator customIndicator, BarSeries series,
            int startIndex, int endIndex, Map<String, Object> parameters, AnalysisResult result) {
        
        // Use existing HeikenAshi strategy to calculate
        HeikenAshiStrategy haStrategy = new HeikenAshiStrategy(null);
        //AnalysisResult tempResult = new AnalysisResult();
        haStrategy.analyzeSymbol(series, startIndex, endIndex, result);
        
        double close = result.getHeikenAshiClose();
        double open = result.getHeikenAshiOpen();
        String trend = result.getHeikenAshiTrend();
        
        return String.format("Close: %.2f, Open: %.2f, Trend: %s", close, open, trend);
    }
    
    private static String calculateCustomMovingAverageTargetValue(CustomIndicator customIndicator, BarSeries series,
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
            //AnalysisResult tempResult = new AnalysisResult();
            
            // Calculate using the strategy
            strategy.calculate(series, result, lastIndex);
            
            // Get the target value
            Double targetValue = strategy.getMovingAverageTargetValue(result);
            Double targetPercentile = strategy.getMovingAverageTargetValuePercentile(result); 
            
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
    public static AbstractIndicatorStrategy createCustomIndicatorStrategy(CustomIndicator customIndicator) {
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
                    RSIStrategy rsiStrategy = new RSIStrategy(period, null);
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
                    return new TrendStrategy(sma1, sma2, sma3, null);
                    
                case "VOLUMEMA":
                    int volumePeriod = getIntParameter(parameters, "period", 20);
                    // You may need to modify VolumeStrategyMA to accept custom period
                    return new VolumeStrategyMA(volumePeriod, null);
                    
                case "MOVINGAVERAGE":
                    int maPeriod = getIntParameter(parameters, "period", 20);
                    String maType = getStringParameter(parameters, "type", "SMA");
                    // Create appropriate moving average strategy based on type
                    if ("EMA".equalsIgnoreCase(maType)) {
                        // You'd need an EMA strategy class
                    } else {
                        // Default to SMA - you may need an SMA strategy class
                    }
                    
                    return new MovingAverageStrategy(maPeriod, maType, customIndicator.getName(), null);
                    
                    
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