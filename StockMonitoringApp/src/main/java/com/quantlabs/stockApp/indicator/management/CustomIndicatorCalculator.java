package com.quantlabs.stockApp.indicator.management;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.WMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.indicators.volume.VWAPIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.core.indicators.HeikenAshiIndicator;

/**
 * Service for calculating custom indicator values
 */
public class CustomIndicatorCalculator {
    
    public CustomIndicatorCalculator() {
        // Constructor can be expanded if needed
    }
    
    /**
     * Calculate custom indicator value based on type and parameters
     */
    public Object calculateCustomIndicator(CustomIndicator customIndicator, BarSeries series, 
                                         String timeframe, String symbol) {
        try {
            if (series == null || series.getBarCount() == 0) {
                return "No data available";
            }
            
            String type = customIndicator.getType().toUpperCase();
            Map<String, Object> params = customIndicator.getParameters();
            
            switch (type) {
                case "MACD":
                    return calculateCustomMACD(series, params);
                    
                case "RSI":
                    return calculateCustomRSI(series, params);
                    
                case "MOVINGAVERAGE":
                    return calculateCustomMovingAverage(series, params);
                    
                case "PSAR":
                    return calculateCustomPSAR(series, params);
                    
                case "TREND":
                    return calculateCustomTrend(series, params);
                    
                case "VOLUMEMA":
                    return calculateCustomVolumeMA(series, params);
                    
                case "VOLUME":
                    return calculateCustomVolume(series, params);
                    
                case "VWAP":
                    return calculateCustomVWAP(series, params);
                    
                case "HEIKENASHI":
                    return calculateCustomHeikenAshi(series, params);
                    
                case "HIGHESTCLOSEOPEN":
                    return calculateCustomHighestCloseOpen(series, params, timeframe, symbol);
                    
                default:
                    return "N/A - Unsupported type: " + type;
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    private String calculateCustomMACD(BarSeries series, Map<String, Object> params) {
        int fast = getIntParam(params, "fast", 12);
        int slow = getIntParam(params, "slow", 26);
        int signal = getIntParam(params, "signal", 9);
        
        int minBarsRequired = Math.max(fast, slow) + signal;
        int endIndex = series.getEndIndex();
        
        if (endIndex < minBarsRequired) {
            return "Insufficient data (need " + minBarsRequired + " bars)";
        }
        
        try {
            MACDIndicator macd = new MACDIndicator(new ClosePriceIndicator(series), fast, slow);
            EMAIndicator signalLine = new EMAIndicator(macd, signal);
            
            double macdValue = macd.getValue(endIndex).doubleValue();
            double signalValue = signalLine.getValue(endIndex).doubleValue();
            double histogram = macdValue - signalValue;
            
            String trend;
            if (macdValue > signalValue) {
                trend = "Bullish";
            } else {
                trend = "Bearish";
            }
            
            /*return String.format("%s (MACD: %.4f, Signal: %.4f, Hist: %.4f)", 
                               trend, macdValue, signalValue, histogram);*/
            
            return String.format("%s", trend);
            
        } catch (Exception e) {
            return "Calc Error: " + e.getMessage();
        }
    }
    
    private String calculateCustomRSI(BarSeries series, Map<String, Object> params) {
        int period = getIntParam(params, "period", 14);
        int endIndex = series.getEndIndex();
        
        if (endIndex < period) {
            return "Insufficient data (need " + period + " bars)";
        }
        
        try {
            RSIIndicator rsi = new RSIIndicator(new ClosePriceIndicator(series), period);
            double rsiValue = rsi.getValue(endIndex).doubleValue();
            
            String status;
            if (rsiValue > 70) {
                status = "Overbought";
            } else if (rsiValue < 30) {
                status = "Oversold";
            } else if (rsiValue > 50) {
                status = "Bullish";
            } else {
                status = "Bearish";
            }
            
            //return String.format("%s (%.2f)", status, rsiValue);
            
            return String.format("%s", status);
            
        } catch (Exception e) {
            return "Calc Error: " + e.getMessage();
        }
    }
    
    private String calculateCustomMovingAverage(BarSeries series, Map<String, Object> params) {
        int period = getIntParam(params, "period", 20);
        String maType = getStringParam(params, "type", "SMA");
        int endIndex = series.getEndIndex();
        
        if (endIndex < period) {
            return "Insufficient data (need " + period + " bars)";
        }
        
        try {
            Indicator<Num> maIndicator;
            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            
            if ("EMA".equalsIgnoreCase(maType)) {
                maIndicator = new EMAIndicator(closePrice, period);
            } else if ("WMA".equalsIgnoreCase(maType)) {
                maIndicator = new WMAIndicator(closePrice, period);
            } else {
                maIndicator = new SMAIndicator(closePrice, period); // Default SMA
            }
            
            double maValue = maIndicator.getValue(endIndex).doubleValue();
            double currentPrice = series.getBar(endIndex).getClosePrice().doubleValue();
            double difference = currentPrice - maValue;
            double percentDiff = (difference / maValue) * 100;
            
            String position = currentPrice > maValue ? "Above" : "Below";
            
            return String.format("%s MA (Price: %.2f, MA: %.2f, Diff: %.2f%%)", 
                               position, currentPrice, maValue, percentDiff);
            
        } catch (Exception e) {
            return "Calc Error: " + e.getMessage();
        }
    }
    
    private String calculateCustomPSAR(BarSeries series, Map<String, Object> params) {
        double step = getDoubleParam(params, "step", 0.02);
        double max = getDoubleParam(params, "max", 0.2);
        int endIndex = series.getEndIndex();
        
        if (endIndex < 2) {
            return "Insufficient data";
        }
        
        try {
            ParabolicSarIndicator psar = new ParabolicSarIndicator(series, 
                series.numOf(step), series.numOf(step), series.numOf(max));
            
            double psarValue = psar.getValue(endIndex).doubleValue();
            double currentPrice = series.getBar(endIndex).getClosePrice().doubleValue();
            
            String trend = currentPrice > psarValue ? "Bullish" : "Bearish";
            
            //return String.format("%s (Price: %.2f, PSAR: %.2f)", trend, currentPrice, psarValue);
            
            return String.format("%s", trend);
            
        } catch (Exception e) {
            return "Calc Error: " + e.getMessage();
        }
    }
    
    private String calculateCustomTrend(BarSeries series, Map<String, Object> params) {
        int sma1 = getIntParam(params, "sma1", 9);
        int sma2 = getIntParam(params, "sma2", 20);
        int sma3 = getIntParam(params, "sma3", 200);
        int endIndex = series.getEndIndex();
        
        int maxPeriod = Math.max(Math.max(sma1, sma2), sma3);
        if (endIndex < maxPeriod) {
            return "Insufficient data (need " + maxPeriod + " bars)";
        }
        
        try {
            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            SMAIndicator sma9 = new SMAIndicator(closePrice, sma1);
            SMAIndicator sma20 = new SMAIndicator(closePrice, sma2);
            SMAIndicator sma200 = new SMAIndicator(closePrice, sma3);
            
            double price = closePrice.getValue(endIndex).doubleValue();
            double sma9Value = sma9.getValue(endIndex).doubleValue();
            double sma20Value = sma20.getValue(endIndex).doubleValue();
            double sma200Value = sma200.getValue(endIndex).doubleValue();
            
            // Trend logic
            boolean aboveSMA9 = price > sma9Value;
            boolean aboveSMA20 = price > sma20Value;
            boolean aboveSMA200 = price > sma200Value;
            boolean sma9Above20 = sma9Value > sma20Value;
            boolean sma20Above200 = sma20Value > sma200Value;
            
            String trend;
            if (aboveSMA9 && aboveSMA20 && aboveSMA200 && sma9Above20 && sma20Above200) {
                trend = "Strong Uptrend";
            } else if (!aboveSMA9 && !aboveSMA20 && !aboveSMA200 && !sma9Above20 && !sma20Above200) {
                trend = "Downtrend";
            } else if (aboveSMA9 && aboveSMA20) {
                trend = "Mild Uptrend";
            } else if (!aboveSMA9 && !aboveSMA20) {
                trend = "Downtrend";
            } else {
                trend = "Neutral/Ranging";
            }
            
            //return String.format("%s (SMA%d: %.2f, SMA%d: %.2f, SMA%d: %.2f)", trend, sma1, sma9Value, sma2, sma20Value, sma3, sma200Value);
            
            return String.format("%s", trend);
            
        } catch (Exception e) {
            return "Calc Error: " + e.getMessage();
        }
    }
    
    private String calculateCustomVolumeMA(BarSeries series, Map<String, Object> params) {
        int period = getIntParam(params, "period", 20);
        int endIndex = series.getEndIndex();
        
        if (endIndex < period) {
            return "Insufficient data (need " + period + " bars)";
        }
        
        try {
            VolumeIndicator volume = new VolumeIndicator(series);
            SMAIndicator volumeMA = new SMAIndicator(volume, period);
            
            double currentVolume = volume.getValue(endIndex).doubleValue();
            double volumeMAValue = volumeMA.getValue(endIndex).doubleValue();
            double ratio = currentVolume / volumeMAValue;
            
            String volumeStatus;
            if (ratio > 2.0) {
                volumeStatus = "Strong Uptrend";
            } else if (ratio > 1.5) {
                volumeStatus = "Uptrend";
            } else if (ratio > 0.8) {
                volumeStatus = "Normal Volume";
            } else {
                volumeStatus = "Low Volume";
            }
            
            //return String.format("%s (Cur: %.0f, MA: %.0f, Ratio: %.2fx)", volumeStatus, currentVolume, volumeMAValue, ratio);
            return String.format("%s", volumeStatus);
        
        } catch (Exception e) {
            return "Calc Error: " + e.getMessage();
        }
    }
    
    private String calculateCustomVolume(BarSeries series, Map<String, Object> params) {
        int endIndex = series.getEndIndex();
        
        if (endIndex < 0) {
            return "No data";
        }
        
        try {
            VolumeIndicator volume = new VolumeIndicator(series);
            double currentVolume = volume.getValue(endIndex).doubleValue();
            
            // Calculate average volume for context
            /*double avgVolume = 0;
            int lookback = Math.min(20, endIndex + 1);
            for (int i = 0; i < lookback; i++) {
                avgVolume += volume.getValue(endIndex - i).doubleValue();
            }
            avgVolume /= lookback;
            
            double ratio = currentVolume / avgVolume;
            
            String volumeStatus;
            if (ratio > 3.0) {
                volumeStatus = "Extreme Volume";
            } else if (ratio > 2.0) {
                volumeStatus = "Very High";
            } else if (ratio > 1.5) {
                volumeStatus = "High";
            } else if (ratio > 0.8) {
                volumeStatus = "Normal";
            } else {
                volumeStatus = "Low";
            }
            
            return String.format("%s (%.0f, Avg: %.0f)", volumeStatus, currentVolume, avgVolume);*/
            return String.format("%s", currentVolume);
            
        } catch (Exception e) {
            return "Calc Error: " + e.getMessage();
        }
    }
    
    private String calculateCustomVWAP(BarSeries series, Map<String, Object> params) {
        int endIndex = series.getEndIndex();
        
        if (endIndex < 1) {
            return "Insufficient data";
        }
        
        try {
            // Simple VWAP calculation (you might want to use your existing VWAP implementation)
            VWAPIndicator vwap = new VWAPIndicator(series, 1); // Daily VWAP
            double vwapValue = vwap.getValue(endIndex).doubleValue();
            double currentPrice = series.getBar(endIndex).getClosePrice().doubleValue();
            double difference = currentPrice - vwapValue;
            double percentDiff = (difference / vwapValue) * 100;
            
            String position = currentPrice > vwapValue ? "Uptrend" : "DownTrend";
            
            //return String.format("%s (Price: %.2f, VWAP: %.2f, Diff: %.2f%%)", position, currentPrice, vwapValue, percentDiff);
            
            return String.format("%s", position);
            
        } catch (Exception e) {
            return "Calc Error: " + e.getMessage();
        }
    }
    
    private String calculateCustomHeikenAshi(BarSeries series, Map<String, Object> params) {
        int endIndex = series.getEndIndex();
        
        if (endIndex < 1) {
            return "Insufficient data";
        }
        
        try {
            HeikenAshiIndicator ha = new HeikenAshiIndicator(series);
            
            double haClose = ha.getValue(endIndex).doubleValue();
            double haOpen = ha.getHeikenAshiOpen(endIndex).doubleValue();
            
            String trend = haClose > haOpen ? "Bullish" : "Bearish";
            double bodySize = Math.abs(haClose - haOpen);
            
            // Check if it's a doji (small body)
            double avgBody = calculateAverageBodySize(series, ha, endIndex, 10);
            String candleType = (bodySize < avgBody * 0.1) ? "Doji" : "Normal";
            
            //return String.format("%s %s (Body: %.4f)", trend, candleType, bodySize);
            
            return String.format("%s", trend);
            
        } catch (Exception e) {
            return "Calc Error: " + e.getMessage();
        }
    }
    
    private String calculateCustomHighestCloseOpen(BarSeries series, Map<String, Object> params, 
                                                  String timeframe, String symbol) {
        try {
            String timeRange = getStringParam(params, "timeRange", "1D");
            String session = getStringParam(params, "session", "all");
            int indexCounter = getIntParam(params, "lookback", 5);
            
            // Use your existing HighestCloseOpen logic here
            // This is a simplified version - you'll want to integrate with your existing code
            
            int endIndex = series.getEndIndex();
            if (endIndex < indexCounter) {
                return "Insufficient data (need " + indexCounter + " bars)";
            }
            
            // Find highest close in the lookback period
            double highestClose = Double.MIN_VALUE;
            int highestIndex = -1;
            
            for (int i = 0; i <= indexCounter && (endIndex - i) >= 0; i++) {
                int index = endIndex - i;
                double close = series.getBar(index).getClosePrice().doubleValue();
                if (close > highestClose) {
                    highestClose = close;
                    highestIndex = index;
                }
            }
            
            double currentClose = series.getBar(endIndex).getClosePrice().doubleValue();
            String trend = currentClose >= highestClose ? "Uptrend" : "Downtrend";
            
            /*if (highestIndex != -1) {
                ZonedDateTime highestTime = series.getBar(highestIndex).getEndTime();
                String timeStr = highestTime.format(DateTimeFormatter.ofPattern("MMM dd HH:mm"));
                
                return String.format("%s (High: %.2f @ %s, Cur: %.2f)", 
                                   trend, highestClose, timeStr, currentClose);
            } else {
                return String.format("%s (High: %.2f, Cur: %.2f)", trend, highestClose, currentClose);
            }*/
            return String.format("%s", trend);
        } catch (Exception e) {
            return "Calc Error: " + e.getMessage();
        }
    }
    
    // Helper methods
    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    private double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value != null) {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    private String getStringParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private double calculateAverageBodySize(BarSeries series, HeikenAshiIndicator ha, int endIndex, int period) {
        double totalBody = 0;
        int count = Math.min(period, endIndex + 1);
        
        for (int i = 0; i < count; i++) {
            int index = endIndex - i;
            double haClose = ha.getValue(index).doubleValue();
            double haOpen = ha.getHeikenAshiOpen(index).doubleValue();
            totalBody += Math.abs(haClose - haOpen);
        }
        
        return totalBody / count;
    }
    
    /**
     * Check if there's enough data to calculate the indicator
     */
    public boolean hasEnoughData(CustomIndicator customIndicator, int availableBars) {
        String type = customIndicator.getType().toUpperCase();
        Map<String, Object> params = customIndicator.getParameters();
        
        switch (type) {
            case "MACD":
                int fast = getIntParam(params, "fast", 12);
                int slow = getIntParam(params, "slow", 26);
                int signal = getIntParam(params, "signal", 9);
                return availableBars >= Math.max(fast, slow) + signal;
                
            case "RSI":
                int rsiPeriod = getIntParam(params, "period", 14);
                return availableBars >= rsiPeriod;
                
            case "MOVINGAVERAGE":
                int maPeriod = getIntParam(params, "period", 20);
                return availableBars >= maPeriod;
                
            case "TREND":
                int sma1 = getIntParam(params, "sma1", 9);
                int sma2 = getIntParam(params, "sma2", 20);
                int sma3 = getIntParam(params, "sma3", 200);
                return availableBars >= Math.max(Math.max(sma1, sma2), sma3);
                
            case "VOLUMEMA":
                int volumePeriod = getIntParam(params, "period", 20);
                return availableBars >= volumePeriod;
                
            case "HIGHESTCLOSEOPEN":
                int lookback = getIntParam(params, "lookback", 5);
                return availableBars >= lookback;
                
            default:
                return availableBars >= 2; // Minimum for most indicators
        }
    }
    
    /**
     * Get the minimum bars required for a custom indicator
     */
    public int getMinimumBarsRequired(CustomIndicator customIndicator) {
        String type = customIndicator.getType().toUpperCase();
        Map<String, Object> params = customIndicator.getParameters();
        
        switch (type) {
            case "MACD":
                int fast = getIntParam(params, "fast", 12);
                int slow = getIntParam(params, "slow", 26);
                int signal = getIntParam(params, "signal", 9);
                return Math.max(fast, slow) + signal;
                
            case "RSI":
                return getIntParam(params, "period", 14);
                
            case "MOVINGAVERAGE":
                return getIntParam(params, "period", 20);
                
            case "TREND":
                int sma1 = getIntParam(params, "sma1", 9);
                int sma2 = getIntParam(params, "sma2", 20);
                int sma3 = getIntParam(params, "sma3", 200);
                return Math.max(Math.max(sma1, sma2), sma3);
                
            case "VOLUMEMA":
                return getIntParam(params, "period", 20);
                
            case "HIGHESTCLOSEOPEN":
                return getIntParam(params, "lookback", 5);
                
            default:
                return 2;
        }
    }
}