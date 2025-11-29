package com.quantlabs.core.indicators;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;

import java.util.ArrayList;
import java.util.List;

import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

public class HeikenAshiIndicator extends CachedIndicator<Num> {
    private final BarSeries series;
    private final List<Num> haOpens = new ArrayList<>();
    private final List<Num> haCloses = new ArrayList<>();

    public HeikenAshiIndicator(BarSeries series) {
        super(series);
        this.series = series;
        calculateHeikenAshi();
    }

    private void calculateHeikenAshi() {
        for (int i = 0; i < series.getBarCount(); i++) {
            Num close = series.getBar(i).getClosePrice();
            Num open = series.getBar(i).getOpenPrice();
            Num high = series.getBar(i).getHighPrice();
            Num low = series.getBar(i).getLowPrice();

            Num haClose = close.plus(open).plus(high).plus(low).dividedBy(series.numOf(4));
            haCloses.add(haClose);

            Num haOpen = (i == 0)
                ? series.getBar(0).getOpenPrice()
                : haOpens.get(i - 1).plus(haCloses.get(i - 1)).dividedBy(series.numOf(2));

            haOpens.add(haOpen);
        }
    }

    public Num getHeikenAshiOpen(int index) {
        return haOpens.get(index);
    }

    public Num getHeikenAshiClose(int index) {
        return haCloses.get(index);
    }

    @Override
    protected Num calculate(int index) {
        return getHeikenAshiClose(index);
    }

}