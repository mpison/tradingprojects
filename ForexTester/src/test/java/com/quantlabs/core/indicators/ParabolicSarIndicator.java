package com.quantlabs.core.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.Num;

/**
 * Custom implementation of the Parabolic SAR indicator using TA4J.
 */
public class ParabolicSarIndicator extends AbstractIndicator<Num> {

    private final BarSeries series;
    private final Num afStep;
    private final Num afMax;

    public enum Direction { UP, DOWN }

    private final Num[] sarValues;
    private final Direction[] trendDirections;

    public ParabolicSarIndicator(BarSeries series, double afStep, double afMax) {
        super(series);
        this.series = series;
        this.afStep = numOf(afStep);
        this.afMax = numOf(afMax);
        this.sarValues = new Num[series.getBarCount()];
        this.trendDirections = new Direction[series.getBarCount()];
        calculateAll();
    }

    private void calculateAll() {
        if (series.isEmpty()) return;

        Num ep; // Extreme point
        Num sar;
        Num af = afStep;
        Direction direction;

        // Start with initial direction assumption
        int startIndex = 1;
        Num prevHigh = series.getBar(0).getHighPrice();
        Num prevLow = series.getBar(0).getLowPrice();
        Num currHigh = series.getBar(1).getHighPrice();
        Num currLow = series.getBar(1).getLowPrice();

        // Initial trend direction
        direction = currHigh.isGreaterThan(prevHigh) ? Direction.UP : Direction.DOWN;
        ep = direction == Direction.UP ? currHigh : currLow;
        sar = direction == Direction.UP ? prevLow : prevHigh;

        sarValues[0] = sar;
        trendDirections[0] = direction;

        for (int i = startIndex; i < series.getBarCount(); i++) {
            Num prevSar = sar;
            Direction prevDirection = direction;

            // Update SAR
            sar = sar.plus(af.multipliedBy(ep.minus(sar)));

            // Ensure SAR is within recent highs/lows
            if (direction == Direction.UP) {
                sar = sar.min(series.getBar(i - 1).getLowPrice()).min(series.getBar(i).getLowPrice());
            } else {
                sar = sar.max(series.getBar(i - 1).getHighPrice()).max(series.getBar(i).getHighPrice());
            }

            // Determine new direction
            boolean reverse = false;
            if (direction == Direction.UP && series.getBar(i).getLowPrice().isLessThan(sar)) {
                reverse = true;
                direction = Direction.DOWN;
                sar = ep;
                ep = series.getBar(i).getLowPrice();
                af = afStep;
            } else if (direction == Direction.DOWN && series.getBar(i).getHighPrice().isGreaterThan(sar)) {
                reverse = true;
                direction = Direction.UP;
                sar = ep;
                ep = series.getBar(i).getHighPrice();
                af = afStep;
            }

            if (!reverse) {
                if (direction == Direction.UP && series.getBar(i).getHighPrice().isGreaterThan(ep)) {
                    ep = series.getBar(i).getHighPrice();
                    af = af.min(afMax);
                    af = af.plus(afStep);
                } else if (direction == Direction.DOWN && series.getBar(i).getLowPrice().isLessThan(ep)) {
                    ep = series.getBar(i).getLowPrice();
                    af = af.min(afMax);
                    af = af.plus(afStep);
                }
            }

            sarValues[i] = sar;
            trendDirections[i] = direction;
        }
    }

    @Override
    public Num getValue(int index) {
        return sarValues[index] != null ? sarValues[index] : numOf(0);
    }

    public Direction getDirection(int index) {
        return trendDirections[index] != null ? trendDirections[index] : Direction.UP;
    }
}
