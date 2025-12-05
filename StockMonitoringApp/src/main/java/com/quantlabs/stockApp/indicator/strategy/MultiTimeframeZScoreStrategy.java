package com.quantlabs.stockApp.indicator.strategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.indicators.volume.VWAPIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.indicator.management.StrategyConfig;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

import java.time.ZonedDateTime;
import java.util.*;

public class MultiTimeframeZScoreStrategy extends AbstractIndicatorStrategy {
    
    private StrategyConfig strategyConfig;
    private PriceData priceData;
    private Map<String, BarSeries> timeframeSeries;
    
    // Indicator cache for performance
    private Map<String, Map<String, Object>> indicatorCache;
    
    public MultiTimeframeZScoreStrategy(StrategyConfig strategyConfig, PriceData priceData, ConsoleLogger logger) {
        super(logger);
        this.strategyConfig = strategyConfig;
        this.priceData = priceData;
        this.timeframeSeries = new HashMap<>();
        this.indicatorCache = new HashMap<>();
        initializeTimeframeSeries();
    }
    
    @Override
    public String getName() {
        return "MultiTimeframeZScore";
    }
    
    /**
     * Initialize BarSeries for each timeframe from PriceData results
     */
    private void initializeTimeframeSeries() {
        if (priceData != null && priceData.getResults() != null) {
            for (Map.Entry<String, AnalysisResult> entry : priceData.getResults().entrySet()) {
                String timeframe = entry.getKey();
                AnalysisResult result = entry.getValue();
                if (result != null && result.getBarSeries() != null) {
                    timeframeSeries.put(timeframe, result.getBarSeries());
                }
            }
        }
    }
    
    /**
     * Get the index in a higher timeframe that matches the timestamp of a lower timeframe index
     */
    private int getMatchingHigherTimeframeIndex(BarSeries lowerTimeframeSeries, int lowerIndex, 
                                               BarSeries higherTimeframeSeries, String higherTimeframe) {
        if (lowerIndex < 0 || lowerIndex >= lowerTimeframeSeries.getBarCount()) {
            return -1;
        }
        
        ZonedDateTime targetTime = lowerTimeframeSeries.getBar(lowerIndex).getEndTime();
        
        // Find the bar in higher timeframe that contains this timestamp
        for (int i = higherTimeframeSeries.getBarCount() - 1; i >= 0; i--) {
            ZonedDateTime higherBarEnd = higherTimeframeSeries.getBar(i).getEndTime();
            ZonedDateTime higherBarStart = higherTimeframeSeries.getBar(i).getBeginTime();
            
            // Check if target time falls within this higher timeframe bar
            if (!targetTime.isBefore(higherBarStart) && !targetTime.isAfter(higherBarEnd)) {
                return i;
            }
        }
        
        return -1; // No matching bar found
    }
    
    /**
     * Calculate timeframe conversion factors
     */
    private int getTimeframeMinutes(String timeframe) {
        switch (timeframe) {
            case "1W": return 7 * 24 * 60;
            case "1D": return 24 * 60;
            case "4H": return 4 * 60;
            case "1H": return 60;
            case "30Min": return 30;
            case "15Min": return 15;
            case "5Min": return 5;
            case "1Min": return 1;
            default: return 1;
        }
    }
    
    /**
     * Get all available timeframes sorted by duration (longest first)
     */
    private List<String> getSortedTimeframes() {
        List<String> timeframes = new ArrayList<>(timeframeSeries.keySet());
        timeframes.sort((tf1, tf2) -> 
            Integer.compare(getTimeframeMinutes(tf2), getTimeframeMinutes(tf1)));
        return timeframes;
    }
    
    @Override
    public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
        // This method calculates Z-scores for a specific bar across all configured timeframes
        calculateZscore(series, result, endIndex);
    }
    
    @Override
    public double calculateZscore(BarSeries baseSeries, AnalysisResult result, int baseEndIndex) {
        if (baseEndIndex < 0 || strategyConfig == null) {
            return 0.0;
        }
        
        Map<String, Double> timeframeScores = new HashMap<>();
        Map<String, Map<String, Double>> indicatorContributions = new HashMap<>();
        
        // Get timeframe weights from strategy configuration
        Map<String, Integer> timeframeWeights = getTimeframeWeights();
        
        // Calculate scores for each timeframe
        for (String timeframe : getSortedTimeframes()) {
            BarSeries timeframeSeries = this.timeframeSeries.get(timeframe);
            if (timeframeSeries == null) continue;
            
            // Get matching index in this timeframe
            int timeframeIndex = getMatchingHigherTimeframeIndex(baseSeries, baseEndIndex, timeframeSeries, timeframe);
            if (timeframeIndex < 0) continue;
            
            // Calculate Z-score for this timeframe
            double timeframeScore = calculateTimeframeZScore(timeframe, timeframeSeries, timeframeIndex, indicatorContributions);
            timeframeScores.put(timeframe, timeframeScore);
        }
        
        // Calculate weighted overall score
        double totalWeight = 0.0;
        double weightedScoreSum = 0.0;
        
        for (Map.Entry<String, Double> entry : timeframeScores.entrySet()) {
            String timeframe = entry.getKey();
            double score = entry.getValue();
            int weight = timeframeWeights.getOrDefault(timeframe, 0);
            
            if (weight > 0) {
                weightedScoreSum += score * weight;
                totalWeight += weight;
            }
        }
        
        double overallZScore = totalWeight > 0 ? weightedScoreSum / totalWeight : 0.0;
        
        // Store results in AnalysisResult
        storeZScoreResults(result, overallZScore, timeframeScores, indicatorContributions, timeframeWeights);
        
        return overallZScore;
    }
    
    /**
     * Calculate Z-score for a specific timeframe and index
     */
    private double calculateTimeframeZScore(String timeframe, BarSeries series, int endIndex, 
                                          Map<String, Map<String, Double>> indicatorContributions) {
        if (endIndex < 0) return 0.0;
        
        // Get indicator weights for this timeframe
        Map<String, Integer> indicatorWeights = getIndicatorWeightsForTimeframe(timeframe);
        if (indicatorWeights.isEmpty()) return 0.0;
        
        double totalScore = 0.0;
        double totalWeight = 0.0;
        Map<String, Double> contributions = new HashMap<>();
        
        // Calculate each indicator's contribution
        for (Map.Entry<String, Integer> entry : indicatorWeights.entrySet()) {
            String indicatorName = entry.getKey();
            int weight = entry.getValue();
            
            if (weight > 0) {
                double indicatorScore = calculateIndicatorZScore(indicatorName, series, endIndex);
                double contribution = indicatorScore * weight;
                
                totalScore += contribution;
                totalWeight += weight;
                contributions.put(indicatorName, contribution);
            }
        }
        
        // Store contributions for this timeframe
        if (!contributions.isEmpty()) {
            indicatorContributions.put(timeframe, contributions);
        }
        
        return totalWeight > 0 ? totalScore / totalWeight : 0.0;
    }
    
    /**
     * Calculate Z-score for a specific indicator
     */
    private double calculateIndicatorZScore(String indicatorName, BarSeries series, int endIndex) {
        try {
            switch (indicatorName.toUpperCase()) {
                case "STANDARD_MACD":
                    return calculateMACDZScore(series, endIndex, 12, 26, 9);
                case "STANDARD_MACD(5,8,9)":
                    return calculateMACDZScore(series, endIndex, 5, 8, 9);
                case "STANDARD_RSI":
                    return calculateRSIZScore(series, endIndex, 14);
                case "STANDARD_PSAR(0.01)":
                    return calculatePSARZScore(series, endIndex, 0.01, 0.2);
                case "STANDARD_PSAR(0.05)":
                    return calculatePSARZScore(series, endIndex, 0.05, 0.2);
                case "STANDARD_VWAP":
                    return calculateVWAPZScore(series, endIndex);
                case "STANDARD_VOLUME20MA":
                    return calculateVolumeMAZScore(series, endIndex, 20);
                case "STANDARD_TREND":
                    return calculateTrendZScore(series, endIndex);
                case "STANDARD_HEIKENASHI":
                    return calculateHeikenAshiZScore(series, endIndex);
                case "STANDARD_HIGHESTCLOSEOPEN":
                    return calculateHighestCloseOpenZScore(series, endIndex);
                default:
                    // Check if it's a custom indicator
                    if (indicatorName.startsWith("CUSTOM_")) {
                        return calculateCustomIndicatorZScore(indicatorName, series, endIndex);
                    }
                    return 0.0;
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.log("Error calculating " + indicatorName + " Z-score: " + e.getMessage());
            }
            return 0.0;
        }
    }
    
    // Individual indicator Z-score calculations
    
    private double calculateMACDZScore(BarSeries series, int endIndex, int fast, int slow, int signal) {
        if (endIndex < Math.max(fast, slow) + signal) return 0.0;
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        MACDIndicator macd = new MACDIndicator(closePrice, fast, slow);
        EMAIndicator signalLine = new EMAIndicator(macd, signal);
        
        double currentMacd = macd.getValue(endIndex).doubleValue();
        double currentSignal = signalLine.getValue(endIndex).doubleValue();
        
        double score = 0.0;
        double maxScore = 0.0;
        
        // MACD above signal line
        maxScore += 40;
        if (currentMacd > currentSignal) score += 40;
        
        // Both above zero
        maxScore += 30;
        if (currentMacd > 0 && currentSignal > 0) score += 30;
        
        // Momentum (compared to previous)
        if (endIndex > 0) {
            maxScore += 30;
            double prevMacd = macd.getValue(endIndex - 1).doubleValue();
            if (currentMacd > prevMacd) score += 30;
        }
        
        return maxScore > 0 ? (score / maxScore) * 100.0 : 0.0;
    }
    
    private double calculateRSIZScore(BarSeries series, int endIndex, int period) {
        if (endIndex < period) return 0.0;
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, period);
        double rsiValue = rsi.getValue(endIndex).doubleValue();
        
        // RSI scoring: 30-70 is optimal range
        if (rsiValue >= 30 && rsiValue <= 70) {
            // Center around 50 gives highest score
            double distanceFrom50 = Math.abs(rsiValue - 50);
            return Math.max(0, 100 - (distanceFrom50 * 2));
        } else {
            // Outside optimal range - lower score
            return Math.max(0, 50 - Math.abs(rsiValue - 50));
        }
    }
    
    private double calculatePSARZScore(BarSeries series, int endIndex, double step, double max) {
        if (endIndex < 2) return 0.0;
        
        ParabolicSarIndicator psar = new ParabolicSarIndicator(series, 
            series.numOf(step), series.numOf(step), series.numOf(max));
        
        double currentClose = series.getBar(endIndex).getClosePrice().doubleValue();
        double psarValue = psar.getValue(endIndex).doubleValue();
        
        double score = 0.0;
        double maxScore = 0.0;
        
        // Price above PSAR (bullish)
        maxScore += 60;
        if (currentClose > psarValue) score += 60;
        
        // PSAR acceleration
        if (endIndex > 1) {
            maxScore += 40;
            double currentPsar = psar.getValue(endIndex).doubleValue();
            double prevPsar = psar.getValue(endIndex - 1).doubleValue();
            double olderPsar = psar.getValue(endIndex - 2).doubleValue();
            
            double currentChange = currentPsar - prevPsar;
            double previousChange = prevPsar - olderPsar;
            
            if (Math.abs(currentChange) > Math.abs(previousChange)) {
                score += 40; // Acceleration
            } else if (Math.abs(currentChange) == Math.abs(previousChange)) {
                score += 25; // Constant
            } else {
                score += 10; // Deceleration
            }
        }
        
        return maxScore > 0 ? (score / maxScore) * 100.0 : 0.0;
    }
    
    private double calculateVWAPZScore(BarSeries series, int endIndex) {
        if (endIndex < 1) return 0.0;
        
        VWAPIndicator vwap = new VWAPIndicator(series, 1); // Daily VWAP
        double vwapValue = vwap.getValue(endIndex).doubleValue();
        double currentPrice = series.getBar(endIndex).getClosePrice().doubleValue();
        
        // Score based on position relative to VWAP
        double deviation = ((currentPrice - vwapValue) / vwapValue) * 100;
        
        // Optimal: close to VWAP (±2%)
        if (Math.abs(deviation) <= 2) {
            return 90.0;
        } else if (deviation > 0) {
            // Above VWAP - bullish but watch for overextension
            return Math.max(50, 80 - (deviation - 2) * 2);
        } else {
            // Below VWAP - bearish
            return Math.max(20, 40 + (deviation + 2) * 2);
        }
    }
    
    private double calculateVolumeMAZScore(BarSeries series, int endIndex, int period) {
        if (endIndex < period) return 0.0;
        
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator volumeMA = new SMAIndicator(volume, period);
        
        double currentVolume = volume.getValue(endIndex).doubleValue();
        double volumeMAValue = volumeMA.getValue(endIndex).doubleValue();
        
        double ratio = currentVolume / volumeMAValue;
        
        // High volume is generally positive for momentum
        if (ratio >= 2.0) return 90.0; // Very high volume
        if (ratio >= 1.5) return 75.0; // High volume
        if (ratio >= 1.0) return 60.0; // Average volume
        if (ratio >= 0.7) return 40.0; // Below average
        return 20.0; // Low volume
    }
    
    private double calculateTrendZScore(BarSeries series, int endIndex) {
        if (endIndex < 200) return 0.0; // Need enough data for trend analysis
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma9 = new SMAIndicator(closePrice, 9);
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
        
        double price = closePrice.getValue(endIndex).doubleValue();
        double sma9Value = sma9.getValue(endIndex).doubleValue();
        double sma20Value = sma20.getValue(endIndex).doubleValue();
        double sma200Value = sma200.getValue(endIndex).doubleValue();
        
        double score = 0.0;
        double maxScore = 0.0;
        
        // Check alignment of moving averages (classic trend analysis)
        maxScore += 25;
        if (sma9Value > sma20Value && sma20Value > sma200Value) score += 25; // Strong uptrend
        
        maxScore += 25;
        if (price > sma9Value) score += 25; // Price above short MA
        
        maxScore += 25;
        if (price > sma200Value) score += 25; // Price above long MA
        
        // Slope analysis (simplified)
        if (endIndex > 5) {
            maxScore += 25;
            double currentSMA20 = sma20.getValue(endIndex).doubleValue();
            double previousSMA20 = sma20.getValue(endIndex - 5).doubleValue();
            if (currentSMA20 > previousSMA20) score += 25; // MA trending up
        }
        
        return maxScore > 0 ? (score / maxScore) * 100.0 : 0.0;
    }
    
    private double calculateHeikenAshiZScore(BarSeries series, int endIndex) {
        if (endIndex < 1) return 0.0;
        
        // Simplified Heiken Ashi calculation
        double currentClose = series.getBar(endIndex).getClosePrice().doubleValue();
        double currentOpen = series.getBar(endIndex).getOpenPrice().doubleValue();
        double prevClose = series.getBar(endIndex - 1).getClosePrice().doubleValue();
        double prevOpen = series.getBar(endIndex - 1).getOpenPrice().doubleValue();
        
        // Heiken Ashi close = (O + H + L + C) / 4
        double haClose = (currentOpen + series.getBar(endIndex).getHighPrice().doubleValue() +
                         series.getBar(endIndex).getLowPrice().doubleValue() + currentClose) / 4;
        
        // Heiken Ashi open = (previous HA Open + previous HA Close) / 2
        double haOpen = (prevOpen + prevClose) / 2;
        
        // Bullish if HA close > HA open
        if (haClose > haOpen) return 80.0;
        if (haClose < haOpen) return 20.0;
        return 50.0; // Neutral
    }
    
    private double calculateHighestCloseOpenZScore(BarSeries series, int endIndex) {
        int lookback = 5; // Default lookback
        if (endIndex < lookback) return 0.0;
        
        double currentClose = series.getBar(endIndex).getClosePrice().doubleValue();
        double highestClose = Double.MIN_VALUE;
        
        // Find highest close in lookback period
        for (int i = 0; i <= lookback && (endIndex - i) >= 0; i++) {
            double close = series.getBar(endIndex - i).getClosePrice().doubleValue();
            if (close > highestClose) {
                highestClose = close;
            }
        }
        
        // Score based on position relative to highest close
        if (currentClose >= highestClose) return 90.0; // At or above highest
        double ratio = currentClose / highestClose;
        return Math.max(20.0, ratio * 100.0); // Scale with proximity to highest
    }
    
    private double calculateCustomIndicatorZScore(String indicatorName, BarSeries series, int endIndex) {
        // TODO: Placeholder for custom indicator calculation
        // In practice, you would retrieve custom indicator parameters and calculate accordingly
        return 50.0; // Default neutral score
    }
    
    /**
     * Get timeframe weights from strategy configuration
     */
    private Map<String, Integer> getTimeframeWeights() {
        Map<String, Integer> weights = new HashMap<>();
        
        Object weightsObj = strategyConfig.getParameters().get("zscoreWeights");
        if (weightsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> configWeights = (Map<String, Integer>) weightsObj;
            weights.putAll(configWeights);
        }
        
        return weights;
    }
    
    /**
     * Get indicator weights for a specific timeframe
     */
    private Map<String, Integer> getIndicatorWeightsForTimeframe(String timeframe) {
        Map<String, Integer> weights = new HashMap<>();
        
        Object weightsObj = strategyConfig.getParameters().get("indicatorWeights_" + timeframe);
        if (weightsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> configWeights = (Map<String, Integer>) weightsObj;
            weights.putAll(configWeights);
        }
        
        return weights;
    }
    
    /**
     * Store Z-score results in AnalysisResult
     */
    private void storeZScoreResults(AnalysisResult result, double overallZScore, 
                                   Map<String, Double> timeframeScores,
                                   Map<String, Map<String, Double>> indicatorContributions,
                                   Map<String, Integer> timeframeWeights) {
        
        // Store overall Z-score
        result.addCustomValue("overallZScore", overallZScore);
        
        // Store timeframe scores
        result.addCustomValue("timeframeZScores", new HashMap<>(timeframeScores));
        
        // Store indicator contributions
        result.addCustomValue("indicatorContributions", new HashMap<>(indicatorContributions));
        
        // Store timeframe weights
        result.addCustomValue("timeframeWeights", new HashMap<>(timeframeWeights));
        
        // Calculate and store strength category
        String strengthCategory = determineStrengthCategory(overallZScore);
        result.addCustomValue("zScoreStrength", strengthCategory);
    }
    
    private String determineStrengthCategory(double zscore) {
        if (zscore >= 90) return "Exceptional";
        if (zscore >= 80) return "Very Strong";
        if (zscore >= 70) return "Strong";
        if (zscore >= 60) return "Good";
        if (zscore >= 50) return "Moderate";
        if (zscore >= 40) return "Fair";
        if (zscore >= 30) return "Weak";
        if (zscore >= 20) return "Very Weak";
        return "Poor";
    }
    
    @Override
    protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
        // Not used for Z-score indicator
    }
    
    @Override
    public String determineTrend(AnalysisResult result) {
        Object zscoreObj = result.getCustomValue("zScoreStrength");
        return zscoreObj != null ? zscoreObj.toString() : "Neutral";
    }
    
    /**
     * Utility method to get Z-score summary for logging
     */
    public String getZScoreSummary(AnalysisResult result) {
        StringBuilder summary = new StringBuilder();
        summary.append("Z-Score Analysis Summary:\n");
        
        Object overallObj = result.getCustomValue("overallZScore");
        if (overallObj instanceof Number) {
            double overall = ((Number) overallObj).doubleValue();
            summary.append(String.format("Overall Score: %.1f/100 (%s)\n", 
                overall, determineStrengthCategory(overall)));
        }
        
        Object timeframeScoresObj = result.getCustomValue("timeframeZScores");
        if (timeframeScoresObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Double> timeframeScores = (Map<String, Double>) timeframeScoresObj;
            summary.append("Timeframe Scores:\n");
            for (Map.Entry<String, Double> entry : timeframeScores.entrySet()) {
                summary.append(String.format("  %s: %.1f\n", entry.getKey(), entry.getValue()));
            }
        }
        
        return summary.toString();
    }
}