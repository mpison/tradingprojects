package com.quantlabs.QuantTester.v3.alert.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.num.Num;

import com.quantlabs.core.indicators.HeikenAshiIndicator;

import org.json.JSONObject;

public class HeikinAshiCalculator implements IndicatorCalculator {
    @Override
    public String calculate(BarSeries series, JSONObject params, int shift) {
        HeikenAshiIndicator haIndicator = new HeikenAshiIndicator(series);
        
        int lastIndex = series.getEndIndex();
        int currentIndex = lastIndex - shift;
        int prevIndex = currentIndex - 1;
        
        if (currentIndex < 0 || prevIndex < 0) {
            return null;
        }
        
        Num haOpenCurrent = haIndicator.getHeikenAshiOpen(currentIndex);
        Num haCloseCurrent = haIndicator.getHeikenAshiClose(currentIndex);
        Num haOpenPrev = haIndicator.getHeikenAshiOpen(prevIndex);
        Num haClosePrev = haIndicator.getHeikenAshiClose(prevIndex);
        
        if (haClosePrev.doubleValue() <= haOpenPrev.doubleValue() && 
            haCloseCurrent.doubleValue() > haOpenCurrent.doubleValue()) {
            return "Bullish Heikin-Ashi";
        }
        if (haClosePrev.doubleValue() >= haOpenPrev.doubleValue() && 
            haCloseCurrent.doubleValue() < haOpenCurrent.doubleValue()) {
            return "Bearish Heikin-Ashi";
        }
        return null;
    }
}