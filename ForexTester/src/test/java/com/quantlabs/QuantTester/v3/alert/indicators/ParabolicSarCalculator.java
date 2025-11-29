package com.quantlabs.QuantTester.v3.alert.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.num.DecimalNum;
import org.json.JSONObject;

public class ParabolicSarCalculator implements IndicatorCalculator {
    @Override
    public String calculate(BarSeries series, JSONObject params, int shift) {
        double af = params.optDouble("accelerationFactor", 0.02);
        double maxAf = params.optDouble("maxAcceleration", 0.2);
        
        ParabolicSarIndicator psar = new ParabolicSarIndicator(series, 
            DecimalNum.valueOf(af), DecimalNum.valueOf(maxAf));
        
        int lastIndex = series.getEndIndex();
        int currentIndex = lastIndex - shift;
        int prevIndex = currentIndex - 1;
        
        if (currentIndex < 0 || prevIndex < 0) {
            return null;
        }
        
        double psarCurrent = psar.getValue(currentIndex).doubleValue();
        double priceCurrent = series.getBar(currentIndex).getClosePrice().doubleValue();
        double psarPrev = psar.getValue(prevIndex).doubleValue();
        double pricePrev = series.getBar(prevIndex).getClosePrice().doubleValue();
        
        if (pricePrev <= psarPrev && priceCurrent > psarCurrent) return "Bullish PSAR";
        if (pricePrev >= psarPrev && priceCurrent < psarCurrent) return "Bearish PSAR";
        return null;
    }
}