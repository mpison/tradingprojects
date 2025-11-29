package com.quantlabs.QuantTester.v3.alert.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.json.JSONObject;

public class MACDCalculator implements IndicatorCalculator {
    @Override
    public String calculate(BarSeries series, JSONObject params, int shift) {
        int shortPeriod = params.optInt("shortTimeFrame", 12);
        int longPeriod = params.optInt("longTimeFrame", 26);
        int signalPeriod = params.optInt("signalTimeFrame", 9);
        
        MACDIndicator macd = new MACDIndicator(new ClosePriceIndicator(series), shortPeriod, longPeriod);
        SMAIndicator signal = new SMAIndicator(macd, signalPeriod);
        
        int lastIndex = series.getEndIndex();
        int currentIndex = lastIndex - shift;
        int prevIndex = currentIndex - 1;
        
        if (currentIndex < 0 || prevIndex < 0) {
            return null;
        }
        
        double macdCurrent = macd.getValue(currentIndex).doubleValue();
        double signalCurrent = signal.getValue(currentIndex).doubleValue();
        double macdPrev = macd.getValue(prevIndex).doubleValue();
        double signalPrev = signal.getValue(prevIndex).doubleValue();
        
        if (macdPrev <= signalPrev && macdCurrent > signalCurrent) return "Bullish MACD";
        if (macdPrev >= signalPrev && macdCurrent < signalCurrent) return "Bearish MACD";
        return null;
    }
}