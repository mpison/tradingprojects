package com.quantlabs.stockApp.alert.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class VolumeAlertAnalyser2 {
    
    // Private constructor to prevent instantiation
    private VolumeAlertAnalyser2() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Analyzes bullish symbols for volume spikes across configured timeframes
     * 
     * @param bullishSymbols List of symbols that passed bullish technical analysis
     * @param volumeAlertConfig Volume alert configuration
     * @param priceDataMap Map containing price and volume data for symbols
     * @return Filtered list of symbols that meet volume spike criteria
     */
    public static List<String> analyze(List<String> bullishSymbols, VolumeAlertConfig volumeAlertConfig,
            ConcurrentHashMap<String, PriceData> priceDataMap) {

        // Early return if volume alerts are disabled
        if (!isVolumeAnalysisEnabled(volumeAlertConfig)) {
            return new ArrayList<>(bullishSymbols);
        }

        // Get enabled timeframes once
        List<String> enabledTimeframes = getEnabledTimeframes(volumeAlertConfig);
        if (enabledTimeframes.isEmpty()) {
            return new ArrayList<>(bullishSymbols);
        }

        // Use parallel stream for better performance with large symbol lists
        return bullishSymbols.parallelStream()
                .filter(symbol -> hasVolumeSpike(symbol, volumeAlertConfig, priceDataMap, enabledTimeframes))
                .collect(Collectors.toList());
    }

    /**
     * Analyzes a single symbol for volume spikes
     * 
     * @param symbol Stock symbol to analyze
     * @param volumeAlertConfig Volume alert configuration
     * @param priceDataMap Map containing price and volume data
     * @return true if symbol has volume spike in any enabled timeframe
     */
    public static boolean analyze(String symbol, VolumeAlertConfig volumeAlertConfig,
            ConcurrentHashMap<String, PriceData> priceDataMap) {

        if (!isVolumeAnalysisEnabled(volumeAlertConfig)) {
            return true; // Volume analysis disabled, so symbol passes
        }

        List<String> enabledTimeframes = getEnabledTimeframes(volumeAlertConfig);
        return !enabledTimeframes.isEmpty() && 
               hasVolumeSpike(symbol, volumeAlertConfig, priceDataMap, enabledTimeframes);
    }

    /**
     * Enhanced analysis with detailed results
     * 
     * @param symbol Stock symbol to analyze
     * @param volumeAlertConfig Volume alert configuration
     * @param priceDataMap Map containing price and volume data
     * @return Detailed volume analysis result
     */
    public static VolumeAnalysisResult analyzeWithDetails(String symbol, VolumeAlertConfig volumeAlertConfig,
            ConcurrentHashMap<String, PriceData> priceDataMap) {

        VolumeAnalysisResult result = new VolumeAnalysisResult(symbol);
        
        if (!isVolumeAnalysisEnabled(volumeAlertConfig)) {
            result.setAnalysisEnabled(false);
            return result;
        }

        List<String> enabledTimeframes = getEnabledTimeframes(volumeAlertConfig);
        if (enabledTimeframes.isEmpty()) {
            result.setAnalysisEnabled(false);
            return result;
        }

        PriceData priceData = priceDataMap.get(symbol);
        if (priceData == null) {
            result.setError("No price data available for symbol: " + symbol);
            return result;
        }

        Map<String, AnalysisResult> analysisResultMap = priceData.getResults();
        if (analysisResultMap == null || analysisResultMap.isEmpty()) {
            result.setError("No analysis results available for symbol: " + symbol);
            return result;
        }

        // Analyze each enabled timeframe
        for (String timeframe : enabledTimeframes) {
            AnalysisResult analysisResult = analysisResultMap.get(timeframe);
            if (analysisResult != null) {
                TimeframeVolumeResult timeframeResult = analyzeTimeframe(
                    timeframe, analysisResult, volumeAlertConfig);
                result.addTimeframeResult(timeframe, timeframeResult);
            }
        }

        result.calculateOverallResult();
        return result;
    }

    /**
     * Batch analysis with detailed results for multiple symbols
     */
    public static Map<String, VolumeAnalysisResult> analyzeBatchWithDetails(
            List<String> symbols, VolumeAlertConfig volumeAlertConfig,
            ConcurrentHashMap<String, PriceData> priceDataMap) {

        return symbols.parallelStream()
                .collect(Collectors.toConcurrentMap(
                    symbol -> symbol,
                    symbol -> analyzeWithDetails(symbol, volumeAlertConfig, priceDataMap)
                ));
    }

    // Private helper methods

    private static boolean isVolumeAnalysisEnabled(VolumeAlertConfig config) {
        return config != null && config.isEnabled() && config.isVolume20MAEnabled();
    }

    private static List<String> getEnabledTimeframes(VolumeAlertConfig config) {
        if (config == null || config.getTimeframeConfigs() == null) {
            return Collections.emptyList();
        }
        
        return config.getTimeframeConfigs().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().isEnabled())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static boolean hasVolumeSpike(String symbol, VolumeAlertConfig volumeAlertConfig,
            ConcurrentHashMap<String, PriceData> priceDataMap, List<String> enabledTimeframes) {

        PriceData priceData = priceDataMap.get(symbol);
        if (priceData == null) {
            return false;
        }

        Map<String, AnalysisResult> analysisResultMap = priceData.getResults();
        if (analysisResultMap == null) {
            return false;
        }

        for (String timeframe : enabledTimeframes) {
            AnalysisResult analysisResult = analysisResultMap.get(timeframe);
            if (hasVolumeSpikeInTimeframe(analysisResult, volumeAlertConfig, timeframe)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasVolumeSpikeInTimeframe(AnalysisResult analysisResult, 
            VolumeAlertConfig volumeAlertConfig, String timeframe) {

        if (analysisResult == null || 
            analysisResult.getVolume() == null || 
            analysisResult.getVolume20MA() == null) {
            return false;
        }

        Double volume = analysisResult.getVolume();
        Double volume20MA = analysisResult.getVolume20MA();
        
        // Validate data
        if (volume <= 0 || volume20MA <= 0) {
            return false;
        }

        VolumeAlertTimeframeConfig timeframeConfig = volumeAlertConfig.getTimeframeConfig(timeframe);
        if (timeframeConfig == null) {
            return false;
        }

        double requiredPercentage = timeframeConfig.getPercentage();
        double threshold = volume20MA * (1 + requiredPercentage / 100.0);
        
        return volume >= threshold;
    }

    private static TimeframeVolumeResult analyzeTimeframe(String timeframe, AnalysisResult analysisResult,
            VolumeAlertConfig volumeAlertConfig) {

        TimeframeVolumeResult result = new TimeframeVolumeResult(timeframe);
        
        if (analysisResult.getVolume() == null || analysisResult.getVolume20MA() == null) {
            result.setError("Missing volume data");
            return result;
        }

        double volume = analysisResult.getVolume();
        double volume20MA = analysisResult.getVolume20MA();
        
        if (volume <= 0 || volume20MA <= 0) {
            result.setError("Invalid volume data");
            return result;
        }

        VolumeAlertTimeframeConfig timeframeConfig = volumeAlertConfig.getTimeframeConfig(timeframe);
        if (timeframeConfig == null) {
            result.setError("No configuration for timeframe");
            return result;
        }

        double requiredPercentage = timeframeConfig.getPercentage();
        double threshold = volume20MA * (1 + requiredPercentage / 100.0);
        double actualPercentage = ((volume / volume20MA) - 1) * 100.0;
        boolean hasSpike = volume >= threshold;

        result.setVolume(volume);
        result.setVolume20MA(volume20MA);
        result.setRequiredPercentage(requiredPercentage);
        result.setActualPercentage(actualPercentage);
        result.setThreshold(threshold);
        result.setHasSpike(hasSpike);
        result.setExcessOverThreshold(hasSpike ? actualPercentage - requiredPercentage : 0.0);

        return result;
    }

    // Result classes for detailed analysis

    public static class VolumeAnalysisResult {
        private final String symbol;
        private boolean analysisEnabled = true;
        private String error;
        private final Map<String, TimeframeVolumeResult> timeframeResults;
        private boolean overallHasSpike = false;
        private int spikeCount = 0;
        private double maxSpikePercentage = 0.0;

        public VolumeAnalysisResult(String symbol) {
            this.symbol = symbol;
            this.timeframeResults = new ConcurrentHashMap<>();
        }

        public void addTimeframeResult(String timeframe, TimeframeVolumeResult result) {
            timeframeResults.put(timeframe, result);
        }

        public void calculateOverallResult() {
            this.spikeCount = (int) timeframeResults.values().stream()
                    .filter(TimeframeVolumeResult::isHasSpike)
                    .count();
            this.overallHasSpike = spikeCount > 0;
            this.maxSpikePercentage = timeframeResults.values().stream()
                    .filter(TimeframeVolumeResult::isHasSpike)
                    .mapToDouble(TimeframeVolumeResult::getActualPercentage)
                    .max()
                    .orElse(0.0);
        }

        // Getters and setters
        public String getSymbol() { return symbol; }
        public boolean isAnalysisEnabled() { return analysisEnabled; }
        public void setAnalysisEnabled(boolean analysisEnabled) { this.analysisEnabled = analysisEnabled; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public Map<String, TimeframeVolumeResult> getTimeframeResults() { return Collections.unmodifiableMap(timeframeResults); }
        public boolean isOverallHasSpike() { return overallHasSpike; }
        public int getSpikeCount() { return spikeCount; }
        public double getMaxSpikePercentage() { return maxSpikePercentage; }
    }

    public static class TimeframeVolumeResult {
        private final String timeframe;
        private Double volume;
        private Double volume20MA;
        private Double requiredPercentage;
        private Double actualPercentage;
        private Double threshold;
        private Boolean hasSpike;
        private Double excessOverThreshold;
        private String error;

        public TimeframeVolumeResult(String timeframe) {
            this.timeframe = timeframe;
        }

        // Getters and setters
        public String getTimeframe() { return timeframe; }
        public Double getVolume() { return volume; }
        public void setVolume(Double volume) { this.volume = volume; }
        public Double getVolume20MA() { return volume20MA; }
        public void setVolume20MA(Double volume20MA) { this.volume20MA = volume20MA; }
        public Double getRequiredPercentage() { return requiredPercentage; }
        public void setRequiredPercentage(Double requiredPercentage) { this.requiredPercentage = requiredPercentage; }
        public Double getActualPercentage() { return actualPercentage; }
        public void setActualPercentage(Double actualPercentage) { this.actualPercentage = actualPercentage; }
        public Double getThreshold() { return threshold; }
        public void setThreshold(Double threshold) { this.threshold = threshold; }
        public Boolean isHasSpike() { return hasSpike; }
        public void setHasSpike(Boolean hasSpike) { this.hasSpike = hasSpike; }
        public Double getExcessOverThreshold() { return excessOverThreshold; }
        public void setExcessOverThreshold(Double excessOverThreshold) { this.excessOverThreshold = excessOverThreshold; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}