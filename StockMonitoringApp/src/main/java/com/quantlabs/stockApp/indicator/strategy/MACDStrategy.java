package com.quantlabs.stockApp.indicator.strategy;

import java.util.concurrent.ConcurrentHashMap;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class MACDStrategy extends AbstractIndicatorStrategy {
    private final int fastPeriod;
    private final int slowPeriod;
    private final int signalPeriod;
    private final String variant;
    
    public MACDStrategy(int fastPeriod, int slowPeriod, int signalPeriod, String variant, ConsoleLogger logger) {
        super(logger);
        this.fastPeriod = fastPeriod;
        this.slowPeriod = slowPeriod;
        this.signalPeriod = signalPeriod;
        this.variant = variant;        
    }
    
    @Override
    public String getName() {
        if(fastPeriod == 12 && slowPeriod == 26 && signalPeriod == 9) {
            return "MACD";
        }
        return "MACD(" + fastPeriod + "," + slowPeriod + "," + signalPeriod + ")";
    }
    
    @Override
    public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
        int requiredBars = Math.max(fastPeriod, slowPeriod) + signalPeriod;
        if (endIndex < requiredBars - 1) return;
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        MACDIndicator macd = new MACDIndicator(closePrice, fastPeriod, slowPeriod);
        EMAIndicator signal = new EMAIndicator(macd, signalPeriod);
        
        double currentMacd = macd.getValue(endIndex).doubleValue();
        double currentSignal = signal.getValue(endIndex).doubleValue();
        double prevMacd = endIndex > 0 ? macd.getValue(endIndex - 1).doubleValue() : Double.NaN;
        double prevSignal = endIndex > 0 ? signal.getValue(endIndex - 1).doubleValue() : Double.NaN;
        
        if ("MACD(5,8,9)".equals(getName())) {
            result.setMacd359(currentMacd);
            result.setMacdSignal359(currentSignal);
            String status = determineMacdStatus(currentMacd, currentSignal, prevMacd, prevSignal, series, result, endIndex);
            result.setMacd359Status(status);
        } else {
            result.setMacd(currentMacd);
            result.setMacdSignal(currentSignal);
            String status = determineMacdStatus(currentMacd, currentSignal, prevMacd, prevSignal, series, result, endIndex);
            result.setMacdStatus(status);
        }  
        
        // Z-score is already calculated in determineMacdStatus, so we don't call it again
    }
    
    private String determineMacdStatus(double currentMacd, double currentSignal, 
                                      double prevMacd, double prevSignal,
                                      BarSeries series, AnalysisResult result, int endIndex) {
        
        // Calculate the actual Z-score first
        double zscore = calculateZscore(series, result, endIndex);
        
        // Get historical context for momentum assessment
        double currentHistogram = currentMacd - currentSignal;
        double prevHistogram = !Double.isNaN(prevMacd) && !Double.isNaN(prevSignal) ? 
                              prevMacd - prevSignal : 0;
        
        // Check for recent crossovers
        boolean bullishCrossover = currentHistogram > 0 && prevHistogram <= 0;
        boolean bearishCrossover = currentHistogram < 0 && prevHistogram >= 0;
        
        // Determine status based on Z-score with crossover context
        return getStatusWithContext(zscore, bullishCrossover, bearishCrossover);
    }
    
    private String getStatusWithContext(double zscore, boolean bullishCrossover, boolean bearishCrossover) {
        String strength = getStrengthFromZscore(zscore);
        String direction = getDirectionFromZscore(zscore);
        
        // Handle crossover events
        if (bullishCrossover) {
            return "Bullish Crossover - " + strength;
        }
        if (bearishCrossover) {
            return "Bearish Crossover - " + strength;
        }
        
        return strength + " " + direction;
    }
    
    private String getStrengthFromZscore(double zscore) {
        if (zscore >= 90) return "Exceptional";
        if (zscore >= 80) return "Very Strong";
        if (zscore >= 70) return "Strong";
        if (zscore >= 60) return "Moderate";
        if (zscore >= 50) return "Slightly";
        if (zscore >= 40) return "Weak";
        if (zscore >= 30) return "Very Weak";
        return "Extremely Weak";
    }
    
    private String getDirectionFromZscore(double zscore) {
        return zscore >= 50 ? "Bullish" : "Bearish";
    }
    
    @Override
    protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
        if ("MACD".equals(getName())) {
            result.setMacdStatus(currentTrend);
        } else if ("MACD(5,8,9)".equals(getName())) {
            result.setMacd359Status(currentTrend);
        }
    }
    
    @Override
    public String determineTrend(AnalysisResult result) {
        // This method signature doesn't have access to BarSeries, so we can't calculate Z-score
        // We'll use a simplified version for backward compatibility
        if ("MACD".equals(getName())) {
            return result.getMacdStatus() != null ? result.getMacdStatus() : "Neutral";
        } else if ("MACD(5,8,9)".equals(getName())) {
            return result.getMacd359Status() != null ? result.getMacd359Status() : "Neutral";
        }
        return "Neutral";
    }
    
    // Your existing calculateZscore method remains unchanged
    @Override
    public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
        int requiredBars = Math.max(fastPeriod, slowPeriod) + signalPeriod;
        if (endIndex < requiredBars - 1) return 0.0;
        
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        MACDIndicator macd = new MACDIndicator(closePrice, fastPeriod, slowPeriod);
        EMAIndicator signal = new EMAIndicator(macd, signalPeriod);
        
        double currentMacd = macd.getValue(endIndex).doubleValue();
        double currentSignal = signal.getValue(endIndex).doubleValue();
        
        double zscore = 0.0;
        double maxPossibleScore = 0.0;
        
        // 1. Both MACD and Signal above zero (very bullish) - 30 points
        maxPossibleScore += 30;
        if (currentMacd > 0 && currentSignal > 0) {
            zscore += 30;
        }
        
        // 2. MACD above Signal (bullish) - 40 points
        maxPossibleScore += 40;
        if (currentMacd > currentSignal) {
            zscore += 40;
        }
        
        // 3. MACD increasing (bullish momentum) - 15 points
        maxPossibleScore += 15;
        if (endIndex > 0) {
            double prevMacd = macd.getValue(endIndex - 1).doubleValue();
            if (currentMacd > prevMacd) {
                zscore += 15;
            }
        }
        
        // 4. Signal increasing (confirmation) - 15 points
        maxPossibleScore += 15;
        if (endIndex > 0) {
            double prevSignal = signal.getValue(endIndex - 1).doubleValue();
            if (currentSignal > prevSignal) {
                zscore += 15;
            }
        }
        
        // Use the normalizeScore helper method from AbstractIndicatorStrategy
        double normalizedZscore = normalizeScore(zscore, maxPossibleScore);
        
        // Store in result
        if ("MACD(5,8,9)".equals(getName())) {
            result.setMacd359Zscore(normalizedZscore);
        } else {
            result.setMacdZscore(normalizedZscore);
        }
        
        return normalizedZscore;
    }
}