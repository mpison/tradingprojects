package com.quantlabs.stockApp.core.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public class HeikenAshiIndicator extends CachedIndicator<Num> {
    private final BarSeries series;
    private final Num[] haOpenCache;
    private final Num[] haCloseCache;
    private final Num[] haHighCache;
    private final Num[] haLowCache;

    public HeikenAshiIndicator(BarSeries series) {
        super(series);
        this.series = series;
        this.haOpenCache = new Num[series.getBarCount()];
        this.haCloseCache = new Num[series.getBarCount()];
        this.haHighCache = new Num[series.getBarCount()];
        this.haLowCache = new Num[series.getBarCount()];
        
        // Pre-calculate all values to ensure cache is populated
        for (int i = 0; i < series.getBarCount(); i++) {
            calculate(i);
        }
    } 

    @Override
    protected Num calculate(int index) {
        if (index < 0 || index >= series.getBarCount()) {
            return series.numOf(0);
        }

        Num open = series.getBar(index).getOpenPrice();
        Num high = series.getBar(index).getHighPrice();
        Num low = series.getBar(index).getLowPrice();
        Num close = series.getBar(index).getClosePrice();

        // Calculate Heiken Ashi Close
        Num haClose = open.plus(high).plus(low).plus(close).dividedBy(series.numOf(4));
        haCloseCache[index] = haClose;

        // Calculate Heiken Ashi Open
        Num haOpen;
        if (index == 0) {
            haOpen = open; // Use regular open for the first bar
        } else {
            // Ensure previous values are calculated first
            if (haOpenCache[index-1] == null || haCloseCache[index-1] == null) {
                calculate(index-1);
            }
            Num prevHaOpen = haOpenCache[index-1];
            Num prevHaClose = haCloseCache[index-1];
            haOpen = prevHaOpen.plus(prevHaClose).dividedBy(series.numOf(2));
        }
        haOpenCache[index] = haOpen;

        // Calculate Heiken Ashi High and Low
        Num haHigh = series.numOf(Math.max(high.doubleValue(), Math.max(haOpen.doubleValue(), haClose.doubleValue())));
        Num haLow = series.numOf(Math.min(low.doubleValue(), Math.min(haOpen.doubleValue(), haClose.doubleValue())));
        haHighCache[index] = haHigh;
        haLowCache[index] = haLow;

        return haClose;
    }

    public Num getHeikenAshiOpen(int index) {
        if (index < 0 || index >= series.getBarCount()) {
            return series.numOf(0);
        }
        if (haOpenCache[index] == null) {
            calculate(index);
        }
        return haOpenCache[index];
    }

    public Num getHeikenAshiHigh(int index) {
        if (index < 0 || index >= series.getBarCount()) {
            return series.numOf(0);
        }
        if (haHighCache[index] == null) {
            calculate(index);
        }
        return haHighCache[index];
    }

    public Num getHeikenAshiLow(int index) {
        if (index < 0 || index >= series.getBarCount()) {
            return series.numOf(0);
        }
        if (haLowCache[index] == null) {
            calculate(index);
        }
        return haLowCache[index];
    }

    @Override
    public Num getValue(int index) {
        if (index < 0 || index >= series.getBarCount()) {
            return series.numOf(0);
        }
        if (haCloseCache[index] == null) {
            calculate(index);
        }
        return haCloseCache[index];
    }
}