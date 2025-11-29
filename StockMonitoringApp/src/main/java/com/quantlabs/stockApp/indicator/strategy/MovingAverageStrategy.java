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
        
        // Also calculate trend based on price position relative to MA
        double currentPrice = series.getBar(endIndex).getClosePrice().doubleValue();
        String trend = determineTrendFromPriceMA(currentPrice, maValue);
        updateTrendStatus(result, trend);
        
        calculateZscore(series,result,endIndex);
    }
    
    @Override
    protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
        // Store trend status in a custom field for moving average
        result.setCustomIndicatorValue(getName() + "_Trend", currentTrend);
    }
    
    @Override
    public String determineTrend(AnalysisResult result) {
        double maValue = getMovingAverageValue(result);
        double currentPrice = result.getPrice();
        
        if (Double.isNaN(maValue) || Double.isNaN(currentPrice)) {
            return "Neutral";
        }
        
        return determineTrendFromPriceMA(currentPrice, maValue);
    }
    
    private String determineTrendFromPriceMA(double price, double maValue) {
        double difference = price - maValue;
        double percentageDifference = (difference / maValue) * 100;
        
        if (percentageDifference > 2.0) {
            return "Strong Bullish";
        } else if (percentageDifference > 0.5) {
            return "Bullish";
        } else if (percentageDifference < -2.0) {
            return "Strong Bearish";
        } else if (percentageDifference < -0.5) {
            return "Bearish";
        } else {
            return "Neutral";
        }
    }
    
    private void setMovingAverageValue(AnalysisResult result, double value) {
        // Store the moving average value in a custom field
        result.setCustomIndicatorValue(getName(), String.valueOf(value));
        
        // Also store in appropriate fields based on period for compatibility
        if (period == 9) {
            result.setSma9(value);
        } else if (period == 20) {
            result.setSma20(value);
        } else if (period == 200) {
            result.setSma200(value);
        }
    }
    
    private double getMovingAverageValue(AnalysisResult result) {
        // Try to get from custom indicator value first
        String customValue = result.getCustomIndicatorValue(getName());
        if (customValue != null) {
            try {
                return Double.parseDouble(customValue);
            } catch (NumberFormatException e) {
                // Fall through to standard fields
            }
        }
        
        // Fall back to standard fields based on period
        if (period == 9) {
            return result.getSma9();
        } else if (period == 20) {
            return result.getSma20();
        } else if (period == 200) {
            return result.getSma200();
        }
        
        return Double.NaN;
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
    
    // In MovingAverageStrategy.java - Add this helper method
    private double getMovingAverageValueFromIndex(BarSeries series, int index) {
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
    
    // In MovingAverageStrategy.java - Replace the entire calculateZscore method with this:
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
        
        // 1. Price above MA (bullish) - 50 points
        maxPossibleScore += 50;
        if (currentPrice > maValue) {
            zscore += 50;
        }
        
        // 2. MA trending up (momentum) - 30 points
        maxPossibleScore += 30;
        if (endIndex > period) {
            double prevMaValue = getMovingAverageValueFromIndex(series, endIndex - 1);
            if (!Double.isNaN(prevMaValue) && maValue > prevMaValue) {
                zscore += 30;
            }
        }
        
        // 3. Strong bullish trend (distance from MA) - 20 points
        maxPossibleScore += 20;
        double distancePercent = Math.abs(currentPrice - maValue) / maValue * 100;
        if (currentPrice > maValue && distancePercent > 2.0) {
            zscore += 20;
        }
        
        // Normalize to 100%
        double normalizedZscore = maxPossibleScore > 0 ? (zscore / maxPossibleScore) * MAX_ZSCORE : 0.0;
        
        result.setCustomIndicatorValue(getName() + "_Zscore", String.valueOf(normalizedZscore));
        return normalizedZscore;
    }
}