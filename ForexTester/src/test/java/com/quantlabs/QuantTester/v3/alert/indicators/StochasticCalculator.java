package com.quantlabs.QuantTester.v3.alert.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.json.JSONObject;

public class StochasticCalculator implements IndicatorCalculator {
    @Override
    public String calculate(BarSeries series, JSONObject params, int shift) {
        int kPeriod = params.optInt("kTimeFrame", 14);
        int dPeriod = params.optInt("dTimeFrame", 3);
        
        StochasticOscillatorKIndicator kIndicator = new StochasticOscillatorKIndicator(series, kPeriod);
        SMAIndicator dIndicator = new SMAIndicator(kIndicator, dPeriod);
        
        int lastIndex = series.getEndIndex();
        int currentIndex = lastIndex - shift;
        int prevIndex = currentIndex - 1;
        
        if (currentIndex < 0 || prevIndex < 0) {
            return null;
        }
        
        double kCurrent = kIndicator.getValue(currentIndex).doubleValue();
        double dCurrent = dIndicator.getValue(currentIndex).doubleValue();
        double kPrev = kIndicator.getValue(prevIndex).doubleValue();
        double dPrev = dIndicator.getValue(prevIndex).doubleValue();
        
        if (kPrev <= dPrev && kCurrent > dCurrent) return "Bullish Stochastic";
        if (kPrev >= dPrev && kCurrent < dCurrent) return "Bearish Stochastic";
        return null;
    }
}