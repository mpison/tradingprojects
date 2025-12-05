package com.quantlabs.stockApp.indicator.strategy;

import java.util.Map;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class MovingAverageStrategy extends AbstractIndicatorStrategy {
    private final int period;
    private final String type; // "SMA" or "EMA"
    private final String name;
    
    // Configuration constants
    private static final double STRONG_BULLISH_THRESHOLD = 2.0;
    private static final double WEAK_BULLISH_THRESHOLD = 0.5;
    private static final double STRONG_MOMENTUM_THRESHOLD = 0.5;
    private static final double WEAK_MOMENTUM_THRESHOLD = 0.1;
    
    public MovingAverageStrategy(int period, String type, String name, ConsoleLogger logger) {
        super(logger);
        this.period = period;
        this.type = type != null ? type.toUpperCase() : "SMA";
        this.name = name != null ? name : String.format("%s(%d)", this.type, period);
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
        if (endIndex < period - 1) {
            // Not enough data for calculation
            return; 
        }
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        double maValue;
        
        if ("EMA".equals(type)) {
            EMAIndicator ema = new EMAIndicator(closePrice, period);
            Num emaValue = ema.getValue(endIndex);
            maValue = emaValue != null ? emaValue.doubleValue() : Double.NaN;
        } else {
            // Default to SMA
            SMAIndicator sma = new SMAIndicator(closePrice, period);
            Num smaValue = sma.getValue(endIndex);
            maValue = smaValue != null ? smaValue.doubleValue() : Double.NaN;
        }
        
        // Store the moving average value in the result
        setMovingAverageValue(result, maValue);
        
        // Calculate Z-score first
        double zscore = calculateZscore(series, result, endIndex);
        
        // Determine trend based on Z-score
        String trend = determineTrendFromZscore(zscore);
        
        // Enhance with price/MA context
        double currentPrice = series.getBar(endIndex).getClosePrice().doubleValue();
        String priceMATrend = determineTrendFromPriceMA(currentPrice, maValue);
        
        // Combine both determinations for final trend
        String finalTrend = combineTrends(trend, priceMATrend);
        
        updateTrendStatus(result, finalTrend);
        
        // Store Z-score trend for future reference
        result.setCustomIndicatorValue(getName() + "_Zscore_Trend", trend);
        result.setCustomIndicatorValue(getName() + "_Zscore", String.valueOf(zscore));
    }
    
    @Override
    public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
        if (endIndex < period - 1) return 0.0;
        
        double maValue = getMovingAverageValue(result);
        double currentPrice = series.getBar(endIndex).getClosePrice().doubleValue();
        
        // If we can't get valid MA value, return 0
        if (Double.isNaN(maValue)) {
            return 0.0;
        }
        
        double zscore = 0.0;
        double maxPossibleScore = 0.0;
        
        // 1. Price position relative to MA - 40 points
        maxPossibleScore += 40;
        double priceRatio = currentPrice / maValue;
        if (priceRatio > 1.02) { // >2% above MA
            zscore += 40;
        } else if (priceRatio > 1.005) { // >0.5% above MA
            zscore += 30;
        } else if (priceRatio > 1.0) { // Above MA
            zscore += 20;
        } else if (priceRatio > 0.995) { // Close to MA
            zscore += 10;
        }
        
        // 2. MA direction (momentum) - 30 points
        maxPossibleScore += 30;
        if (endIndex > period) {
            double prevMaValue = getMovingAverageValueFromIndex(series, endIndex - 1);
            if (!Double.isNaN(prevMaValue) && prevMaValue != 0) {
                double maMomentum = (maValue - prevMaValue) / prevMaValue * 100;
                
                if (maMomentum > STRONG_MOMENTUM_THRESHOLD) {
                    zscore += 30; // Strong uptrend
                } else if (maMomentum > WEAK_MOMENTUM_THRESHOLD) {
                    zscore += 20; // Uptrend
                } else if (maMomentum > -WEAK_MOMENTUM_THRESHOLD) {
                    zscore += 10; // Flat
                }
            }
        }
        
        // 3. Price momentum relative to MA - 30 points
        maxPossibleScore += 30;
        if (endIndex > 0) {
            double prevPrice = series.getBar(endIndex - 1).getClosePrice().doubleValue();
            double prevMaValue = getMovingAverageValueFromIndex(series, endIndex - 1);
            
            if (!Double.isNaN(prevMaValue) && prevMaValue != 0) {
                double currentDistance = (currentPrice - maValue) / maValue;
                double prevDistance = (prevPrice - prevMaValue) / prevMaValue;
                
                if (currentDistance > prevDistance && currentDistance > 0) {
                    zscore += 30; // Accelerating above MA
                } else if (currentDistance > prevDistance) {
                    zscore += 20; // Improving position
                } else if (currentDistance > 0) {
                    zscore += 10; // Maintaining above MA
                }
            }
        }
        
        double normalizedZscore = normalizeScore(zscore, maxPossibleScore);
        result.setCustomIndicatorValue(getName() + "_Zscore", String.valueOf(normalizedZscore));
        return normalizedZscore;
    }
    
    @Override
    protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
        // Store trend status in a custom field for moving average
        result.setCustomIndicatorValue(getName() + "_Trend", currentTrend);
        
        // Also update the main trend field for common periods
        if (period == 9) {
            result.setSmaTrend(currentTrend);
        } else if (period == 20) {
            // You might want to add a specific field for SMA20 trend
        } else if (period == 200) {
            // You might want to add a specific field for SMA200 trend
        }
    }
    
    @Override
    public String determineTrend(AnalysisResult result) {
        // Try to get Z-score based trend first
        String zscoreTrend = result.getCustomIndicatorValue(getName() + "_Zscore_Trend");
        if (zscoreTrend != null && !zscoreTrend.equals("N/A")) {
            return zscoreTrend;
        }
        
        // Fallback to custom trend field
        String customTrend = result.getCustomIndicatorValue(getName() + "_Trend");
        if (customTrend != null && !customTrend.equals("N/A")) {
            return customTrend;
        }
        
        // Final fallback to price/MA comparison
        double maValue = getMovingAverageValue(result);
        double currentPrice = result.getPrice();
        
        if (Double.isNaN(maValue) || Double.isNaN(currentPrice)) {
            return "Neutral";
        }
        
        return determineTrendFromPriceMA(currentPrice, maValue);
    }
    
    private String determineTrendFromPriceMA(double price, double maValue) {
        if (Double.isNaN(price) || Double.isNaN(maValue) || maValue == 0) {
            return "Neutral";
        }
        
        double percentageDifference = ((price - maValue) / maValue) * 100;
        double absoluteDifference = Math.abs(percentageDifference);
        
        if (percentageDifference > STRONG_BULLISH_THRESHOLD) {
            return "Very Strong Bullish";
        } else if (percentageDifference > WEAK_BULLISH_THRESHOLD) {
            return "Bullish";
        } else if (percentageDifference > 0) {
            return "Slightly Bullish";
        } else if (percentageDifference > -WEAK_BULLISH_THRESHOLD) {
            return "Slightly Bearish";
        } else if (percentageDifference > -STRONG_BULLISH_THRESHOLD) {
            return "Bearish";
        } else {
            return "Very Strong Bearish";
        }
    }
    
    private String determineTrendFromZscore(double zscore) {
        if (zscore >= 80) return "Very Strong Bullish";
        if (zscore >= 65) return "Strong Bullish";
        if (zscore >= 55) return "Bullish";
        if (zscore >= 45) return "Slightly Bullish";
        if (zscore >= 35) return "Neutral Bullish";
        if (zscore >= 25) return "Neutral";
        if (zscore >= 15) return "Neutral Bearish";
        if (zscore >= 5) return "Slightly Bearish";
        if (zscore >= 1) return "Bearish";
        return "Strong Bearish";
    }
    
    private String combineTrends(String zscoreTrend, String priceMATrend) {
        // If both agree, use the stronger signal
        if (zscoreTrend.contains("Bullish") && priceMATrend.contains("Bullish")) {
            return getStrongerTrend(zscoreTrend, priceMATrend);
        }
        if (zscoreTrend.contains("Bearish") && priceMATrend.contains("Bearish")) {
            return getStrongerTrend(zscoreTrend, priceMATrend);
        }
        
        // If conflicting, prefer Z-score trend as it considers more factors
        return zscoreTrend;
    }
    
    private String getStrongerTrend(String trend1, String trend2) {
        int strength1 = getTrendStrength(trend1);
        int strength2 = getTrendStrength(trend2);
        return strength1 >= strength2 ? trend1 : trend2;
    }
    
    private int getTrendStrength(String trend) {
        if (trend.contains("Very Strong")) return 4;
        if (trend.contains("Strong")) return 3;
        if (!trend.contains("Slightly")) return 2;
        return 1;
    }
    
    private void setMovingAverageValue(AnalysisResult result, double value) {
        String indicatorName = getName();
        
        // Store in custom indicator values
        result.setCustomIndicatorValue(indicatorName, String.valueOf(value));
        result.setCustomIndicatorValue(indicatorName + "_Type", type);
        result.setCustomIndicatorValue(indicatorName + "_Period", String.valueOf(period));
        
        // Store in analysis result fields for common periods
        switch (period) {
            case 9:
                result.setSma9(value);
                break;
            case 20:
                result.setSma20(value);
                break;
            case 50:
                // Store in custom field for SMA50
                result.setCustomIndicatorValue("SMA50", String.valueOf(value));
                break;
            case 200:
                result.setSma200(value);
                break;
        }
    }
    
    private double getMovingAverageValue(AnalysisResult result) {
        // Try custom indicator value first
        String customValue = result.getCustomIndicatorValue(getName());
        if (customValue != null && !customValue.equals("N/A")) {
            try {
                return Double.parseDouble(customValue);
            } catch (NumberFormatException e) {
                // Continue to fallback options
            }
        }
        
        // Fallback to standard fields
        switch (period) {
            case 9: return result.getSma9();
            case 20: return result.getSma20();
            case 50: 
                String sma50Value = result.getCustomIndicatorValue("SMA50");
                if (sma50Value != null && !sma50Value.equals("N/A")) {
                    try {
                        return Double.parseDouble(sma50Value);
                    } catch (NumberFormatException e) {
                        // Continue
                    }
                }
                return Double.NaN;
            case 200: return result.getSma200();
            default: return Double.NaN;
        }
    }
    
    // Helper method to get MA value from specific index
    private double getMovingAverageValueFromIndex(BarSeries series, int index) {
        if (index < 0 || index >= series.getBarCount()) {
            return Double.NaN;
        }
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        if ("EMA".equals(type)) {
            EMAIndicator ema = new EMAIndicator(closePrice, period);
            Num emaValue = ema.getValue(index);
            return emaValue != null ? emaValue.doubleValue() : Double.NaN;
        } else {
            // Default to SMA
            SMAIndicator sma = new SMAIndicator(closePrice, period);
            Num smaValue = sma.getValue(index);
            return smaValue != null ? smaValue.doubleValue() : Double.NaN;
        }
    }
    
    // Additional technical analysis methods
    public double calculateMomentum(BarSeries series, int endIndex) {
        if (endIndex < period) return 0.0;
        
        double currentMA = getMovingAverageValueFromIndex(series, endIndex);
        double previousMA = getMovingAverageValueFromIndex(series, endIndex - 1);
        
        if (Double.isNaN(currentMA) || Double.isNaN(previousMA) || previousMA == 0) {
            return 0.0;
        }
        
        return ((currentMA - previousMA) / previousMA) * 100;
    }
    
    public double calculateSlope(BarSeries series, int endIndex, int lookback) {
        if (endIndex < period + lookback - 1) return 0.0;
        
        double currentMA = getMovingAverageValueFromIndex(series, endIndex);
        double previousMA = getMovingAverageValueFromIndex(series, endIndex - lookback);
        
        if (Double.isNaN(currentMA) || Double.isNaN(previousMA) || previousMA == 0) {
            return 0.0;
        }
        
        return ((currentMA - previousMA) / previousMA) * 100;
    }
    
    public double calculateDistanceFromMA(BarSeries series, int endIndex) {
        if (endIndex < period - 1) return 0.0;
        
        double currentPrice = series.getBar(endIndex).getClosePrice().doubleValue();
        double maValue = getMovingAverageValueFromIndex(series, endIndex);
        
        if (Double.isNaN(maValue) || maValue == 0) {
            return 0.0;
        }
        
        return ((currentPrice - maValue) / maValue) * 100;
    }
    
    // Getters for strategy configuration
    public int getPeriod() {
        return period;
    }
    
    public String getType() {
        return type;
    }
    
    /**
     * Factory method to create MovingAverageStrategy from CustomIndicator
     */
    public static MovingAverageStrategy fromCustomIndicator(com.quantlabs.stockApp.indicator.management.CustomIndicator customIndicator, ConsoleLogger logger) {
        if (customIndicator == null) {
            return new MovingAverageStrategy(20, "SMA", "SMA(20)", logger);
        }
        
        Map<String, Object> parameters = customIndicator.getParameters();
        int period = getIntParameter(parameters, "period", 20);
        String type = getStringParameter(parameters, "type", "SMA");
        String name = customIndicator.getName();
        
        return new MovingAverageStrategy(period, type, name, logger);
    }
    
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
    
    private static String getStringParameter(Map<String, Object> parameters, String key, String defaultValue) {
        if (parameters == null || !parameters.containsKey(key)) {
            return defaultValue;
        }
        Object value = parameters.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * Get trend strength as numerical value for comparison
     */
    public int getTrendStrengthValue(String trend) {
        return getTrendStrength(trend);
    }
    
    /**
     * Check if trend is bullish
     */
    public boolean isBullish(String trend) {
        return trend != null && trend.toLowerCase().contains("bullish");
    }
    
    /**
     * Check if trend is bearish
     */
    public boolean isBearish(String trend) {
        return trend != null && trend.toLowerCase().contains("bearish");
    }
    
    /**
     * Check if trend is neutral
     */
    public boolean isNeutral(String trend) {
        return trend != null && trend.toLowerCase().contains("neutral");
    }
}