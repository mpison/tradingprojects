package com.quantlabs.stockApp.core.indicators.targetvalue;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.helpers.OpenPriceIndicator;
import org.ta4j.core.num.Num;

/**
 * Custom indicator for calculating target value based on two moving averages and trend direction
 * Target = MA1 + (MA1 - MA2) for uptrend
 * Target = MA1 - (MA1 - MA2) for downtrend
 */
public class MovingAverageTargetValue extends CachedIndicator<Num> {
    private final BarSeries series;
    private final String trendDirection;
    private final int ma1Period;
    private final int ma2Period;
    private final String ma1PriceType;
    private final String ma2PriceType;
    private final String ma1Type;
    private final String ma2Type;
    
    private CachedIndicator<Num> ma1Indicator;
    private CachedIndicator<Num> ma2Indicator;

    // Original constructor for backward compatibility
    public MovingAverageTargetValue(BarSeries series, String trendDirection) {
        this(series, trendDirection, 20, 50, "CLOSE", "CLOSE", "SMA", "SMA");
    }
    
    // New constructor with full customization
    public MovingAverageTargetValue(BarSeries series, String trendDirection,
                                  int ma1Period, int ma2Period,
                                  String ma1PriceType, String ma2PriceType,
                                  String ma1Type, String ma2Type) {
        super(series);
        this.series = series;
        this.trendDirection = trendDirection;
        this.ma1Period = ma1Period;
        this.ma2Period = ma2Period;
        this.ma1PriceType = ma1PriceType != null ? ma1PriceType : "CLOSE";
        this.ma2PriceType = ma2PriceType != null ? ma2PriceType : "CLOSE";
        this.ma1Type = ma1Type != null ? ma1Type : "SMA";
        this.ma2Type = ma2Type != null ? ma2Type : "SMA";
        
        initializeIndicators();
    }
    
    private void initializeIndicators() {
        // Create first moving average indicator
        this.ma1Indicator = createMovingAverage(ma1PriceType, ma1Period, ma1Type);
        
        // Create second moving average indicator
        this.ma2Indicator = createMovingAverage(ma2PriceType, ma2Period, ma2Type);
    }
    
    private CachedIndicator<Num> createMovingAverage(String priceType, int period, String maType) {
        // Create the appropriate moving average based on price type and MA type
        switch (priceType.toUpperCase()) {
            case "OPEN":
                OpenPriceIndicator openPrice = new OpenPriceIndicator(series);
                if ("EMA".equalsIgnoreCase(maType)) {
                    return new EMAIndicator(openPrice, period);
                } else {
                    return new SMAIndicator(openPrice, period);
                }
                
            case "HIGH":
                HighPriceIndicator highPrice = new HighPriceIndicator(series);
                if ("EMA".equalsIgnoreCase(maType)) {
                    return new EMAIndicator(highPrice, period);
                } else {
                    return new SMAIndicator(highPrice, period);
                }
                
            case "LOW":
                LowPriceIndicator lowPrice = new LowPriceIndicator(series);
                if ("EMA".equalsIgnoreCase(maType)) {
                    return new EMAIndicator(lowPrice, period);
                } else {
                    return new SMAIndicator(lowPrice, period);
                }
                
            case "CLOSE":
            default:
                ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
                if ("EMA".equalsIgnoreCase(maType)) {
                    return new EMAIndicator(closePrice, period);
                } else {
                    return new SMAIndicator(closePrice, period);
                }
        }
    }

    @Override
    protected Num calculate(int index) {
        try {
            // Check if we have enough data for both MAs
            int maxPeriod = Math.max(ma1Period, ma2Period);
            if (index < maxPeriod - 1) {
                return numOf(Double.NaN);
            }
            
            Num ma1Value = ma1Indicator.getValue(index);
            Num ma2Value = ma2Indicator.getValue(index);
            
            if (ma1Value == null || ma2Value == null || ma1Value.isNaN() || ma2Value.isNaN()) {
                return numOf(Double.NaN);
            }
            
            Num difference = ma1Value.minus(ma2Value);
            Num targetValue;
            
			/*
			 * if ("UpTrend".equalsIgnoreCase(trendDirection)) {
			 * 
			 * targetValue = (difference.dividedBy(ma2Value)).multipliedBy(numOf(100));
			 * 
			 * } else if ("DownTrend".equalsIgnoreCase(trendDirection)) {
			 * 
			 * targetValue = (difference.dividedBy(ma1Value)).multipliedBy(numOf(100));
			 * 
			 * } else { // Neutral trend - return MA1 as target targetValue =
			 * (difference.dividedBy(ma1Value)).multipliedBy(numOf(100));
			 * 
			 * }
			 */
            targetValue = difference;
            
            return targetValue;
            
        } catch (Exception e) {
            return numOf(Double.NaN);
        }
    }
    
    // Helper methods to get individual MA values
    public double getMa1Value(int index) {
        try {
            Num value = ma1Indicator.getValue(index);
            return value != null ? value.doubleValue() : Double.NaN;
        } catch (Exception e) {
            return Double.NaN;
        }
    }
    
    public double getMa2Value(int index) {
        try {
            Num value = ma2Indicator.getValue(index);
            return value != null ? value.doubleValue() : Double.NaN;
        } catch (Exception e) {
            return Double.NaN;
        }
    }
    
    // Getters for configuration
    public int getMa1Period() { return ma1Period; }
    public int getMa2Period() { return ma2Period; }
    public String getMa1PriceType() { return ma1PriceType; }
    public String getMa2PriceType() { return ma2PriceType; }
    public String getMa1Type() { return ma1Type; }
    public String getMa2Type() { return ma2Type; }
    public String getTrendDirection() { return trendDirection; }
    
    // Helper method to get the current configuration as string
    public String getConfiguration() {
        return String.format("MA1: %s%d(%s) | MA2: %s%d(%s) | Trend: %s",
            ma1Type, ma1Period, ma1PriceType,
            ma2Type, ma2Period, ma2PriceType,
            trendDirection);
    }
}