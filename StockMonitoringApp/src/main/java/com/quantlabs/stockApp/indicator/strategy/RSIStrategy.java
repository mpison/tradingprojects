package com.quantlabs.stockApp.indicator.strategy;

import java.util.concurrent.ConcurrentHashMap;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class RSIStrategy extends AbstractIndicatorStrategy {
    private int period = 14;
    
    // Z-score calculation weights
    private static final double OVERSOLD_OVERBOUGHT_WEIGHT = 40.0;
    private static final double MOMENTUM_WEIGHT = 30.0;
    private static final double TREND_ALIGNMENT_WEIGHT = 20.0;
    private static final double DIVERGENCE_WEIGHT = 10.0;
    
    public RSIStrategy(ConsoleLogger logger) {
        super(logger);
    }
    
    public RSIStrategy(int period, ConsoleLogger logger) {
        super(logger);
        this.period = period;
    }
    
    @Override
    public String getName() {
        if (period == 14) {
            return "RSI";
        }
        return "RSI(" + period + ")";
    }
    
    @Override
    public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
        if (endIndex < period - 1) return;
        
        try {
            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            RSIIndicator rsi = new RSIIndicator(closePrice, period);
            
            if (endIndex >= period - 1) {
                double rsiValue = rsi.getValue(endIndex).doubleValue();
                result.setRsi(rsiValue);
                
                // Determine and set trend
                String trend = determineRSITrend(rsiValue);
                result.setRsiTrend(trend);
                
                // Calculate Z-score after setting RSI values
                calculateZscore(series, result, endIndex);
                
                if (logger != null) {
                    logger.log(String.format("RSI(%d) for %s: %.2f - %s", 
                        period, symbol, rsiValue, trend));
                }
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.log("Error calculating RSI for " + getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
        result.setRsiTrend(currentTrend);
    }

    @Override
    public String determineTrend(AnalysisResult result) {
        double rsiValue = result.getRsi();
        return determineRSITrend(rsiValue);
    }
    
    private String determineRSITrend(double rsiValue) {
        if (Double.isNaN(rsiValue)) {
            return "Neutral";
        }
        
        if (rsiValue > 70) {
            return "Overbought";
        } else if (rsiValue < 30) {
            return "Oversold";
        } else if (rsiValue > 55) {
            return "Bullish";
        } else if (rsiValue < 45) {
            return "Bearish";
        }
        return "Neutral";
    }
    
    @Override
    public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
        if (endIndex < period - 1) return 0.0;
        
        try {
            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            RSIIndicator rsi = new RSIIndicator(closePrice, period);
            
            double currentRsi = rsi.getValue(endIndex).doubleValue();
            
            if (Double.isNaN(currentRsi)) {
                result.setRsiZscore(0.0);
                return 0.0;
            }
            
            double zscore = 0.0;
            double maxPossibleScore = 0.0;
            
            // 1. Oversold/Overbought Zone Analysis (40 points)
            maxPossibleScore += OVERSOLD_OVERBOUGHT_WEIGHT;
            double zoneScore = calculateZoneScore(currentRsi);
            zscore += zoneScore;
            
            // 2. Momentum Analysis (30 points)
            maxPossibleScore += MOMENTUM_WEIGHT;
            double momentumScore = calculateMomentumScore(rsi, endIndex, currentRsi);
            zscore += momentumScore;
            
            // 3. Trend Alignment (20 points)
            maxPossibleScore += TREND_ALIGNMENT_WEIGHT;
            double trendAlignmentScore = calculateTrendAlignmentScore(currentRsi);
            zscore += trendAlignmentScore;
            
            // 4. Divergence Detection (10 points)
            maxPossibleScore += DIVERGENCE_WEIGHT;
            double divergenceScore = calculateDivergenceScore(series, rsi, endIndex);
            zscore += divergenceScore;
            
            // Normalize using parent class method
            double normalizedZscore = normalizeScore(zscore, maxPossibleScore);
            
            result.setRsiZscore(normalizedZscore);
            
            if (logger != null) {
                logger.log(String.format("RSI(%d) Z-score for %s: %.1f (Zone: %.1f, Momentum: %.1f, Trend: %.1f, Divergence: %.1f)", 
                    period, symbol, normalizedZscore, zoneScore, momentumScore, trendAlignmentScore, divergenceScore));
            }
            
            return normalizedZscore;
            
        } catch (Exception e) {
            if (logger != null) {
                logger.log("Error calculating RSI Z-score for " + getName() + ": " + e.getMessage());
            }
            result.setRsiZscore(0.0);
            return 0.0;
        }
    }
    
    private double calculateZoneScore(double currentRsi) {
        // Score based on RSI position in different zones
        if (currentRsi < 20) {
            return OVERSOLD_OVERBOUGHT_WEIGHT; // Extremely oversold - maximum bullish potential
        } else if (currentRsi < 30) {
            return OVERSOLD_OVERBOUGHT_WEIGHT * 0.8; // Oversold - strong bullish
        } else if (currentRsi > 80) {
            return 0; // Extremely overbought - maximum bearish
        } else if (currentRsi > 70) {
            return OVERSOLD_OVERBOUGHT_WEIGHT * 0.2; // Overbought - bearish
        } else if (currentRsi > 60) {
            return OVERSOLD_OVERBOUGHT_WEIGHT * 0.4; // Approaching overbought - slightly bearish
        } else if (currentRsi < 40) {
            return OVERSOLD_OVERBOUGHT_WEIGHT * 0.6; // Approaching oversold - slightly bullish
        } else {
            return OVERSOLD_OVERBOUGHT_WEIGHT * 0.5; // Neutral zone
        }
    }
    
    private double calculateMomentumScore(RSIIndicator rsi, int endIndex, double currentRsi) {
        if (endIndex < period) {
            return MOMENTUM_WEIGHT * 0.5; // Neutral if insufficient history
        }
        
        double momentumScore = 0.0;
        
        // Check recent RSI momentum
        double prevRsi = rsi.getValue(endIndex - 1).doubleValue();
        
        if (!Double.isNaN(prevRsi)) {
            double rsiChange = currentRsi - prevRsi;
            
            // Strong bullish momentum: RSI rising from oversold or in neutral zone
            if (rsiChange > 2.0) { // Significant upward movement
                momentumScore = MOMENTUM_WEIGHT;
            } else if (rsiChange > 0.5) {
                momentumScore = MOMENTUM_WEIGHT * 0.8;
            } else if (rsiChange > 0) {
                momentumScore = MOMENTUM_WEIGHT * 0.6;
            } else if (rsiChange > -0.5) {
                momentumScore = MOMENTUM_WEIGHT * 0.4;
            } else if (rsiChange > -2.0) {
                momentumScore = MOMENTUM_WEIGHT * 0.2;
            } else {
                momentumScore = 0; // Strong downward momentum
            }
            
            // Bonus for momentum in the right direction relative to zones
            if (currentRsi < 30 && rsiChange > 0) {
                momentumScore = Math.min(MOMENTUM_WEIGHT, momentumScore * 1.2); // 20% bonus
            } else if (currentRsi > 70 && rsiChange < 0) {
                momentumScore = Math.min(MOMENTUM_WEIGHT, momentumScore * 1.1); // 10% bonus
            }
        }
        
        return momentumScore;
    }
    
    private double calculateTrendAlignmentScore(double currentRsi) {
        // Score based on RSI position relative to trend-defining levels
        if (currentRsi > 60) {
            return TREND_ALIGNMENT_WEIGHT * 0.8; // Bullish alignment
        } else if (currentRsi > 50) {
            return TREND_ALIGNMENT_WEIGHT * 0.9; // Strong bullish alignment
        } else if (currentRsi > 45) {
            return TREND_ALIGNMENT_WEIGHT * 0.7; // Mild bullish alignment
        } else if (currentRsi > 40) {
            return TREND_ALIGNMENT_WEIGHT * 0.5; // Neutral
        } else if (currentRsi > 30) {
            return TREND_ALIGNMENT_WEIGHT * 0.3; // Mild bearish alignment
        } else {
            return TREND_ALIGNMENT_WEIGHT * 0.1; // Bearish alignment
        }
    }
    
    private double calculateDivergenceScore(BarSeries series, RSIIndicator rsi, int endIndex) {
        if (endIndex < period + 5) {
            return DIVERGENCE_WEIGHT * 0.5; // Neutral if insufficient history
        }
        
        try {
            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            double currentPrice = closePrice.getValue(endIndex).doubleValue();
            double currentRsi = rsi.getValue(endIndex).doubleValue();
            
            // Look for bullish divergence (price makes lower low, RSI makes higher low)
            boolean bullishDivergence = false;
            boolean bearishDivergence = false;
            
            // Check last 5 periods for divergence patterns
            for (int i = 1; i <= 5; i++) {
                if (endIndex - i - 1 < 0) break;
                
                double priceNow = currentPrice;
                double pricePrev = closePrice.getValue(endIndex - i).doubleValue();
                double priceOlder = closePrice.getValue(endIndex - i - 1).doubleValue();
                
                double rsiNow = currentRsi;
                double rsiPrev = rsi.getValue(endIndex - i).doubleValue();
                double rsiOlder = rsi.getValue(endIndex - i - 1).doubleValue();
                
                // Bullish divergence: price lower lows, RSI higher lows
                if (priceNow < pricePrev && pricePrev < priceOlder && 
                    rsiNow > rsiPrev && rsiPrev > rsiOlder) {
                    bullishDivergence = true;
                    break;
                }
                
                // Bearish divergence: price higher highs, RSI lower highs
                if (priceNow > pricePrev && pricePrev > priceOlder && 
                    rsiNow < rsiPrev && rsiPrev < rsiOlder) {
                    bearishDivergence = true;
                    break;
                }
            }
            
            if (bullishDivergence) {
                return DIVERGENCE_WEIGHT; // Full points for bullish divergence
            } else if (bearishDivergence) {
                return 0; // No points for bearish divergence
            } else {
                return DIVERGENCE_WEIGHT * 0.5; // Neutral if no clear divergence
            }
            
        } catch (Exception e) {
            return DIVERGENCE_WEIGHT * 0.5; // Neutral on error
        }
    }
    
    // Getters and setters
    public int getPeriod() { return period; }
    public void setPeriod(int period) { this.period = period; }
}