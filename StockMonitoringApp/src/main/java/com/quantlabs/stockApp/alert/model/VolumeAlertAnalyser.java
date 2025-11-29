package com.quantlabs.stockApp.alert.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class VolumeAlertAnalyser {
    
    private VolumeAlertAnalyser() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Analyzes symbols for volume spikes - works for both bullish symbols only or all symbols
     */
    public static List<String> analyze(List<String> symbols, VolumeAlertConfig volumeAlertConfig,
            ConcurrentHashMap<String, PriceData> priceDataMap) {

        // This method already works for any list of symbols, so no changes needed
        if (!isVolumeAnalysisEnabled(volumeAlertConfig)) {
            return new ArrayList<>(symbols);
        }

        List<String> enabledTimeframes = getEnabledTimeframes(volumeAlertConfig);
        if (enabledTimeframes.isEmpty()) {
            return new ArrayList<>(symbols);
        }

        return symbols.parallelStream()
                .filter(symbol -> hasVolumeSpike(symbol, volumeAlertConfig, priceDataMap, enabledTimeframes))
                .collect(Collectors.toList());
    }

    /**
     * Analyzes a single symbol for volume spikes using your formula
     */
    public static boolean analyze(String symbol, VolumeAlertConfig volumeAlertConfig,
            ConcurrentHashMap<String, PriceData> priceDataMap) {

        if (!isVolumeAnalysisEnabled(volumeAlertConfig)) {
            return true;
        }

        List<String> enabledTimeframes = getEnabledTimeframes(volumeAlertConfig);
        return !enabledTimeframes.isEmpty() && 
               hasVolumeSpike(symbol, volumeAlertConfig, priceDataMap, enabledTimeframes);
    }

    /**
     * Enhanced analysis with detailed results using your exact formula
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
        
        if (volume <= 0 || volume20MA <= 0) {
            return false;
        }

        VolumeAlertTimeframeConfig timeframeConfig = volumeAlertConfig.getTimeframeConfig(timeframe);
        if (timeframeConfig == null) {
            return false;
        }

        double requiredPercentage = timeframeConfig.getPercentage();
        
        // YOUR EXACT FORMULA: volume20MA * (requiredPercentage / 100.0)
        double threshold = volume20MA * (requiredPercentage / 100.0);
        
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
        
        // YOUR EXACT FORMULA
        double threshold = volume20MA * (requiredPercentage / 100.0);
        
        // Calculate how much the current volume exceeds the threshold
        double excessOverThreshold = volume - threshold;
        double percentageAboveThreshold = (excessOverThreshold / volume20MA) * 100.0;
        boolean hasSpike = volume >= threshold;

        result.setVolume(volume);
        result.setVolume20MA(volume20MA);
        result.setRequiredPercentage(requiredPercentage);
        result.setActualPercentage(percentageAboveThreshold);
        result.setThreshold(threshold);
        result.setHasSpike(hasSpike);
        result.setExcessOverThreshold(excessOverThreshold);
        result.setPercentageAboveThreshold(percentageAboveThreshold);

        return result;
    }

    // Result classes
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
                    .mapToDouble(TimeframeVolumeResult::getPercentageAboveThreshold)
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
        private Double actualPercentage; // Percentage above 20MA: ((volume/volume20MA) - 1) * 100
        private Double threshold; // volume20MA * (requiredPercentage / 100.0)
        private Boolean hasSpike;
        private Double excessOverThreshold; // volume - threshold
        private Double percentageAboveThreshold; // (excessOverThreshold / volume20MA) * 100
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
        public Double getPercentageAboveThreshold() { return percentageAboveThreshold; }
        public void setPercentageAboveThreshold(Double percentageAboveThreshold) { this.percentageAboveThreshold = percentageAboveThreshold; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        /**
         * Helper method to get the interpretation of the threshold
         * Example: "100.0%" means volume must be at least equal to 20MA
         * "150.0%" means volume must be at least 1.5x the 20MA
         */
        public String getThresholdInterpretation() {
            if (requiredPercentage == null) return "N/A";
            return String.format("%.1f%% of 20MA (%.1fx)", 
                requiredPercentage, requiredPercentage / 100.0);
        }
    }
}