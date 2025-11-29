package com.quantlabs.QuantTester.v3.alert.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.json.JSONObject;

public class RSICalculator implements IndicatorCalculator {
    @Override
    public String calculate(BarSeries series, JSONObject params, int shift) {
        int period = params.optInt("timeFrame", 14);
        RSIIndicator rsi = new RSIIndicator(new ClosePriceIndicator(series), period);
        
        int lastIndex = series.getEndIndex();
        int currentIndex = lastIndex - shift;
        int prevIndex = currentIndex - 1;
        
        if (currentIndex < 0 || prevIndex < 0) {
            return null;
        }
        
        double rsiCurrent = rsi.getValue(currentIndex).doubleValue();
        double rsiPrev = rsi.getValue(prevIndex).doubleValue();
        
        if (rsiPrev <= 30 && rsiCurrent > 30) return "RSI Oversold Exit";
        if (rsiPrev >= 70 && rsiCurrent < 70) return "RSI Overbought Exit";
        if (rsiCurrent <= 30 || rsiCurrent >= 70) return null;
        return "normal";
    }
}