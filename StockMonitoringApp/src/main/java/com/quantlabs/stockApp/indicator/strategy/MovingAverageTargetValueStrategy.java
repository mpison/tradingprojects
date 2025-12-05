package com.quantlabs.stockApp.indicator.strategy;

import java.util.concurrent.ConcurrentHashMap;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.core.indicators.targetvalue.MovingAverageTargetValue;
import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;
import com.quantlabs.stockApp.indicator.management.CustomIndicator;

public class MovingAverageTargetValueStrategy extends AbstractIndicatorStrategy {
    private CustomIndicator customIndicator;
    private int ma1Period = 20;
    private int ma2Period = 50;
    private String ma1PriceType = "CLOSE";
    private String ma2PriceType = "CLOSE";
    private String ma1Type = "SMA";
    private String ma2Type = "SMA";
    private String trendSource = "PSAR_0.01";
    
    public MovingAverageTargetValueStrategy(ConsoleLogger logger) {
        super(logger);
    }
    
    public MovingAverageTargetValueStrategy(CustomIndicator customIndicator, ConsoleLogger logger) {
        super(logger);
        this.customIndicator = customIndicator;
        if (customIndicator != null) {
            loadParametersFromCustomIndicator();
        }
    }
    
    // New constructor with explicit parameters
    public MovingAverageTargetValueStrategy(int ma1Period, int ma2Period, 
                                          String ma1PriceType, String ma2PriceType,
                                          String ma1Type, String ma2Type, 
                                          String trendSource, ConsoleLogger logger) {
        super(logger);
        this.ma1Period = ma1Period;
        this.ma2Period = ma2Period;
        this.ma1PriceType = ma1PriceType != null ? ma1PriceType : "CLOSE";
        this.ma2PriceType = ma2PriceType != null ? ma2PriceType : "CLOSE";
        this.ma1Type = ma1Type != null ? ma1Type : "SMA";
        this.ma2Type = ma2Type != null ? ma2Type : "SMA";
        this.trendSource = trendSource != null ? trendSource : "PSAR_0.01";
    }
    
    private void loadParametersFromCustomIndicator() {
        if (customIndicator != null && customIndicator.getParameters() != null) {
            this.ma1Period = getIntParameter("ma1Period", 20);
            this.ma2Period = getIntParameter("ma2Period", 50);
            this.ma1PriceType = getStringParameter("ma1PriceType", "CLOSE");
            this.ma2PriceType = getStringParameter("ma2PriceType", "CLOSE");
            this.ma1Type = getStringParameter("ma1Type", "SMA");
            this.ma2Type = getStringParameter("ma2Type", "SMA");
            this.trendSource = getStringParameter("trendSource", "PSAR_0.01");
        }
    }
    
    @Override
    public String getName() {
        if (customIndicator != null) {
            return customIndicator.getDisplayName();
        }
        return String.format("MATarget(%s%d-%s%d)", ma1Type, ma1Period, ma2Type, ma2Period);
    }
    
    @Override
    public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
        try {
        	
        	//ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
    		
    		int lastIndex = series.getBarCount() - 1;
    		
    		Num close = series.getLastBar().getClosePrice();//closePrice.getValue(lastIndex);
    		
    		// Set price for the first bar
    		result.setPrice(close.doubleValue());
        	
        	
            String trendDirection = determineTrendDirection(result, series);
            
            // Override trend direction if custom trend source is specified
            if (!"PRICE_ACTION".equals(trendSource)) {
                trendDirection = determineTrendFromSource(result, series, trendSource, endIndex);
            }
            
            // Validate we have enough data
            int maxPeriod = Math.max(ma1Period, ma2Period);
            if (endIndex < maxPeriod - 1) {
                result.setMovingAverageTargetValue(null);
                result.setCustomIndicatorValue(getName(), "Insufficient Data");
                if (logger != null) {
                    logger.log("Insufficient data for MovingAverageTargetValue for " + symbol + 
                              " (need " + maxPeriod + " bars, have " + (endIndex + 1) + ")");
                }
                return;
            }
            
            MovingAverageTargetValue targetValue = new MovingAverageTargetValue(
                series, 
                trendDirection,
                ma1Period,
                ma2Period,
                ma1PriceType,
                ma2PriceType,
                ma1Type,
                ma2Type
            );
            
            if (endIndex >= 0) {
                Num value = targetValue.getValue(endIndex);
                if (value != null && !value.isNaN()) {
                    double targetValueDouble = value.doubleValue();
                    result.setMovingAverageTargetValue(targetValueDouble);
                    
                    // Calculate additional metrics for context
                    double currentPrice = series.getBar(endIndex).getClosePrice().doubleValue();
                    double difference =  currentPrice - targetValueDouble;
                    double percentDifference = (targetValueDouble / currentPrice) * 100;
                    
                    // Store in custom indicator values for display
                    String displayValue = String.format("%.2f (%.2f%%)", targetValueDouble, percentDifference);
                    result.setCustomIndicatorValue(getName(), displayValue);
                    
                    // Store detailed information
                    result.setCustomIndicatorValue(getName() + "_Details", 
                        String.format("MA1(%s%d): %.2f, MA2(%s%d): %.2f, Trend: %s (Source: %s)", 
                            ma1Type, ma1Period, targetValue.getMa1Value(endIndex),
                            ma2Type, ma2Period, targetValue.getMa2Value(endIndex),
                            trendDirection, trendSource));
                    
                    if (logger != null) {
                        logger.log(String.format("MovingAverageTargetValue for %s: %.2f (Current: %.2f, Diff: %.2f%%, Trend: %s, Source: %s)", 
                            symbol, targetValueDouble, currentPrice, percentDifference, trendDirection, trendSource));
                    }
                } else {
                    result.setMovingAverageTargetValue(null);
                    result.setCustomIndicatorValue(getName(), "N/A");
                    if (logger != null) {
                        logger.log("MovingAverageTargetValue is NaN or null for " + symbol);
                    }
                }
            } else {
                result.setMovingAverageTargetValue(null);
                result.setCustomIndicatorValue(getName(), "N/A");
                if (logger != null) {
                    logger.log("Empty series for MovingAverageTargetValue for " + symbol);
                }
            }
        } catch (Exception e) {
            result.setMovingAverageTargetValue(null);
            result.setCustomIndicatorValue(getName(), "Error");
            if (logger != null) {
                logger.log("Error calculating MovingAverageTargetValue for " + symbol + ": " + e.getMessage());
            }
            e.printStackTrace();
        }
    }
    
    private String determineTrendDirection(AnalysisResult result, BarSeries series) {
        // Use existing trend analysis if available
        if (result.getPsar005Trend() != null) {
            if (result.getPsar005Trend().contains("↑") || result.getPsar005Trend().contains("Up") || 
                result.getPsar005Trend().contains("Bullish")) {
                return "UpTrend";
            } else if (result.getPsar005Trend().contains("↓") || result.getPsar005Trend().contains("Down") ||
                      result.getPsar005Trend().contains("Bearish")) {
                return "DownTrend";
            }
        }

        if (result.getPsar001Trend() != null) {
            if (result.getPsar001Trend().contains("↑") || result.getPsar001Trend().contains("Up") ||
                result.getPsar001Trend().contains("Bullish")) {
                return "UpTrend";
            } else if (result.getPsar001Trend().contains("↓") || result.getPsar001Trend().contains("Down") ||
                      result.getPsar001Trend().contains("Bearish")) {
                return "DownTrend";
            }
        }

        // Fallback to candle analysis
        int lastIndex = series.getEndIndex();
        if (lastIndex > 0) {
            Bar lastBar = series.getBar(lastIndex);
            return lastBar.getClosePrice().isGreaterThan(lastBar.getOpenPrice()) ? "UpTrend" : "DownTrend";
        }

        return "Neutral";
    }
    
    private String determineTrendFromSource(AnalysisResult result, BarSeries series, String trendSource, int endIndex) {
        switch (trendSource) {
            case "PSAR_0.01":
                if (result.getPsar001Trend() != null) {
                    if (result.getPsar001Trend().contains("↑") || result.getPsar001Trend().contains("Up") ||
                        result.getPsar001Trend().contains("Bullish")) {
                        return "UpTrend";
                    } else if (result.getPsar001Trend().contains("↓") || result.getPsar001Trend().contains("Down") ||
                              result.getPsar001Trend().contains("Bearish")) {
                        return "DownTrend";
                    }
                }
                break;
                
            case "PSAR_0.05":
                if (result.getPsar005Trend() != null) {
                    if (result.getPsar005Trend().contains("↑") || result.getPsar005Trend().contains("Up") ||
                        result.getPsar005Trend().contains("Bullish")) {
                        return "UpTrend";
                    } else if (result.getPsar005Trend().contains("↓") || result.getPsar005Trend().contains("Down") ||
                              result.getPsar005Trend().contains("Bearish")) {
                        return "DownTrend";
                    }
                }
                break;
                
            case "CANDLE":
                return determineTrendFromCandle(series, endIndex);
                
            case "HEIKENASHI":
                return determineTrendFromHeikenAshi(result, series, endIndex);
                
            case "CUSTOM":
                // For custom trend, use a simple SMA crossover
                return determineCustomTrend(result);
                
            default:
                break;
        }
        return "Neutral";
    }
    
    private String determineTrendFromCandle(BarSeries series, int endIndex) {
        try {
            if (endIndex < 0) {
                return "Neutral";
            }
            
            Bar currentBar = series.getBar(endIndex);
            boolean isBullish = currentBar.getClosePrice().isGreaterThan(currentBar.getOpenPrice());
            
            // For more robust candle trend, check multiple recent candles
            if (endIndex > 0) {
                Bar previousBar = series.getBar(endIndex - 1);
                boolean wasBullish = previousBar.getClosePrice().isGreaterThan(previousBar.getOpenPrice());
                
                // If both current and previous candles are bullish/bearish, strengthen the signal
                if (isBullish && wasBullish) {
                    return "UpTrend";
                } else if (!isBullish && !wasBullish) {
                    return "DownTrend";
                }
            }
            
            return isBullish ? "UpTrend" : "DownTrend";
            
        } catch (Exception e) {
            if (logger != null) {
                logger.log("Error determining trend from candle: " + e.getMessage());
            }
            return "Neutral";
        }
    }
    
    private String determineTrendFromHeikenAshi(AnalysisResult result, BarSeries series, int endIndex) {
        try {
            // First try to get Heiken Ashi trend from existing analysis result
            if (result.getHeikenAshiTrend() != null && !result.getHeikenAshiTrend().isEmpty()) {
                String haTrend = result.getHeikenAshiTrend();
                if (haTrend.contains("Bullish") || haTrend.contains("Up") || haTrend.contains("↑")) {
                    return "UpTrend";
                } else if (haTrend.contains("Bearish") || haTrend.contains("Down") || haTrend.contains("↓")) {
                    return "DownTrend";
                }
            }
            
            // Fallback: Calculate simple Heiken Ashi trend manually
            if (endIndex < 1) {
                return "Neutral";
            }
            
            Bar currentBar = series.getBar(endIndex);
            Bar previousBar = series.getBar(endIndex - 1);
            
            // Simple Heiken Ashi calculation
            double haClose = (currentBar.getOpenPrice().doubleValue() + 
                            currentBar.getHighPrice().doubleValue() + 
                            currentBar.getLowPrice().doubleValue() + 
                            currentBar.getClosePrice().doubleValue()) / 4.0;
            
            double haOpen = (previousBar.getOpenPrice().doubleValue() + previousBar.getClosePrice().doubleValue()) / 2.0;
            
            boolean isBullish = haClose > haOpen;
            
            // Check for strong Heiken Ashi trend (multiple consecutive same-colored candles)
            if (endIndex > 2) {
                int bullishCount = 0;
                int bearishCount = 0;
                
                // Check last 3 Heiken Ashi candles
                for (int i = 0; i < 3 && (endIndex - i) >= 0; i++) {
                    int idx = endIndex - i;
                    Bar bar = series.getBar(idx);
                    Bar prevBar = (idx > 0) ? series.getBar(idx - 1) : bar;
                    
                    double currentHaClose = (bar.getOpenPrice().doubleValue() + 
                                           bar.getHighPrice().doubleValue() + 
                                           bar.getLowPrice().doubleValue() + 
                                           bar.getClosePrice().doubleValue()) / 4.0;
                    
                    double currentHaOpen = (prevBar.getOpenPrice().doubleValue() + prevBar.getClosePrice().doubleValue()) / 2.0;
                    
                    if (currentHaClose > currentHaOpen) {
                        bullishCount++;
                    } else {
                        bearishCount++;
                    }
                }
                
                // Strong trend if all 3 candles are same direction
                if (bullishCount == 3) return "UpTrend";
                if (bearishCount == 3) return "DownTrend";
            }
            
            return isBullish ? "UpTrend" : "DownTrend";
            
        } catch (Exception e) {
            if (logger != null) {
                logger.log("Error determining trend from Heiken Ashi: " + e.getMessage());
            }
            return "Neutral";
        }
    }
    
    private String determineCustomTrend(AnalysisResult result) {
        // Simple custom trend logic using SMA values
        // You can customize this based on your requirements
        double sma9 = result.getSma9();
        double sma20 = result.getSma20();
        
        if (!Double.isNaN(sma9) && !Double.isNaN(sma20)) {
            if (sma9 > sma20) {
                return "UpTrend";
            } else if (sma9 < sma20) {
                return "DownTrend";
            }
        }
        
        return "Neutral";
    }
    
    private int getIntParameter(String key, int defaultValue) {
        if (customIndicator != null && customIndicator.getParameters() != null) {
            Object value = customIndicator.getParameters().get(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value != null) {
                try {
                    return Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    if (logger != null) {
                        logger.log("⚠️ Could not parse int parameter '" + key + "': " + value);
                    }
                }
            }
        }
        return defaultValue;
    }
    
    private String getStringParameter(String key, String defaultValue) {
        if (customIndicator != null && customIndicator.getParameters() != null) {
            Object value = customIndicator.getParameters().get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return defaultValue;
    }

    @Override
    protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
        // This method is not used for target value calculation
        // Trend is determined internally based on the configuration
    }

    @Override
    public String determineTrend(AnalysisResult result) {
        // This strategy doesn't determine trend in the traditional sense
        // It uses trend to calculate target values
        return "N/A";
    }
    
 // In MovingAverageTargetValueStrategy.java - Add this method
    @Override
    public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
        Double targetValue = result.getMovingAverageTargetValue();
        double zscore = 0.0;
        
        if (targetValue != null && !Double.isNaN(targetValue)) {
            try {
                // Calculate the actual MA values for the endIndex
                MovingAverageTargetValue matv = new MovingAverageTargetValue(
                    series, 
                    determineTrendDirection(result, series),
                    ma1Period, ma2Period,
                    ma1PriceType, ma2PriceType,
                    ma1Type, ma2Type
                );
                
                double ma1Value = matv.getMa1Value(endIndex);
                double ma2Value = matv.getMa2Value(endIndex);
                
                if (!Double.isNaN(ma1Value) && !Double.isNaN(ma2Value)) {
                    // Calculate the absolute difference between MA values
                    double maValueDifference = Math.abs(ma1Value - ma2Value);
                    
                    // If targetValue (which is MA1 - MA2) is less than the absolute MA value difference, set 100 points
                    if (Math.abs(targetValue) <= maValueDifference) {
                        zscore = MAX_ZSCORE; // 100 points
                        zscore = normalizeScore(zscore, MAX_ZSCORE);
                    }
                }
                
            } catch (Exception e) {
                if (logger != null) {
                    logger.log("Error calculating MATarget Z-score: " + e.getMessage());
                }
            }
        }
        
        // Set it on the result
        result.setMovingAverageTargetValueZscore(zscore);
        return zscore;
    }
    
    // Factory method to create from CustomIndicator
    public static MovingAverageTargetValueStrategy fromCustomIndicator(CustomIndicator customIndicator, ConsoleLogger logger) {
        return new MovingAverageTargetValueStrategy(customIndicator, logger);
    }
    
    // Factory method to create with explicit parameters
    public static MovingAverageTargetValueStrategy withParameters(int ma1Period, int ma2Period, 
                                                                String ma1PriceType, String ma2PriceType,
                                                                String ma1Type, String ma2Type, 
                                                                String trendSource, ConsoleLogger logger) {
        return new MovingAverageTargetValueStrategy(ma1Period, ma2Period, ma1PriceType, ma2PriceType, 
                                                  ma1Type, ma2Type, trendSource, logger);
    }
    
    // Get configuration details for logging/debugging
    public String getConfigurationDetails() {
        return String.format("MA1: %s%d(%s), MA2: %s%d(%s), TrendSource: %s",
            ma1Type, ma1Period, ma1PriceType,
            ma2Type, ma2Period, ma2PriceType,
            trendSource);
    }
    
    // Getters for parameters
    public int getMa1Period() { return ma1Period; }
    public int getMa2Period() { return ma2Period; }
    public String getMa1PriceType() { return ma1PriceType; }
    public String getMa2PriceType() { return ma2PriceType; }
    public String getMa1Type() { return ma1Type; }
    public String getMa2Type() { return ma2Type; }
    public String getTrendSource() { return trendSource; }
    
    // Setters for parameters (optional, for dynamic configuration)
    public void setMa1Period(int ma1Period) { this.ma1Period = ma1Period; }
    public void setMa2Period(int ma2Period) { this.ma2Period = ma2Period; }
    public void setMa1PriceType(String ma1PriceType) { this.ma1PriceType = ma1PriceType; }
    public void setMa2PriceType(String ma2PriceType) { this.ma2PriceType = ma2PriceType; }
    public void setMa1Type(String ma1Type) { this.ma1Type = ma1Type; }
    public void setMa2Type(String ma2Type) { this.ma2Type = ma2Type; }
    public void setTrendSource(String trendSource) { this.trendSource = trendSource; }
}