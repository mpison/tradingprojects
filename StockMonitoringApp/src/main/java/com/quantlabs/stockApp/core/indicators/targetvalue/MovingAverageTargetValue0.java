package com.quantlabs.stockApp.core.indicators.targetvalue;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.HighPriceIndicator;
import org.ta4j.core.indicators.helpers.LowPriceIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.Num;

public class MovingAverageTargetValue0 extends CachedIndicator<Num> {

    private final Indicator<Num> highPrice;
    private final Indicator<Num> lowPrice;
    private final SMAIndicator slowMaHigh;
    private final SMAIndicator fastMaHigh;
    private final SMAIndicator slowMaLow;
    private final SMAIndicator fastMaLow;
    private final String trend;

    public MovingAverageTargetValue0(BarSeries series, String trend) {
        super(series);
        this.highPrice = new HighPriceIndicator(series);
        this.lowPrice = new LowPriceIndicator(series);
        this.slowMaHigh = new SMAIndicator(highPrice, 3);
        this.fastMaHigh = new SMAIndicator(highPrice, 8);
        this.slowMaLow = new SMAIndicator(lowPrice, 3);
        this.fastMaLow = new SMAIndicator(lowPrice, 8);
        this.trend = trend;
    }

    @Override
    protected Num calculate(int index) {
        // Get latest bar's high and low
        Num latestHigh = highPrice.getValue(index);
        Num latestLow = lowPrice.getValue(index);
        
        //System.out.println("series="+getBarSeries().getName()+",latestLow="+latestLow);
        
        // Calculate MA differences
        //Num fastMaDiff = fastMaHigh.getValue(index-1).minus(fastMaLow.getValue(index-1));
        Num slowMaDiff = slowMaHigh.getValue(index-1).minus(slowMaLow.getValue(index-1));
        
        // Average the differences
        Num maDiffAverage = slowMaDiff;//fastMaDiff.plus(slowMaDiff).dividedBy(numOf(2));
        
        // Calculate target value based on trend
        if ("UpTrend".equalsIgnoreCase(trend)) {
            return maDiffAverage.plus(latestLow);
        } else if ("DownTrend".equalsIgnoreCase(trend)) {
            return maDiffAverage.minus(latestHigh);
        } else {
            // Neutral trend - return just the MA difference average
            return maDiffAverage;
        }
    }
}