// Add this new class in com.quantlabs.stockApp.core.indicators package
package com.quantlabs.stockApp.core.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public class VolumeIndicator extends CachedIndicator<Num> {

    public VolumeIndicator(BarSeries series) {
        super(series);
    }

    @Override
    protected Num calculate(int index) {
        return getBarSeries().getBar(index).getVolume();
    }
}