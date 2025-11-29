package com.quantlabs.core.indicators;

import java.util.ArrayList;
import java.util.List;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.core.enums.TrendDirectionEnum;

public class PSAR21TrendIndicator extends CachedIndicator<Num> {

    private final ParabolicSarIndicator fastSar;
    private final ParabolicSarIndicator slowSar;

    public PSAR21TrendIndicator(BarSeries series, double stepFast, double maxFast, double stepSlow, double maxSlow) {
        super(series);
        this.fastSar = new ParabolicSarIndicator(series, numOf(stepFast), numOf(maxFast));
        this.slowSar = new ParabolicSarIndicator(series, numOf(stepSlow), numOf(maxSlow));
    }

    @Override
    protected Num calculate(int index) {
        if (index < 2) return numOf(0);

        Num currentOpen = getBar(index).getOpenPrice();
        Num currentClose = getBar(index).getClosePrice();
        Num slowValue = slowSar.getValue(index);

        List<PSARSegment> segments = extractLast3Segments(index);

        if (segments.size() == 3) {
            PSARSegment seg0 = segments.get(0);
            PSARSegment seg2 = segments.get(2);
            if (seg0.getDirection() == seg2.getDirection()) {
                Num last0 = fastSar.getValue(seg0.getLastIndex());
                Num last2 = fastSar.getValue(seg2.getLastIndex());

                if (seg0.getDirection() == TrendDirectionEnum.VALID_UP_TREND && last2.isGreaterThan(last0)) {
                    if (currentOpen.isGreaterThan(last0)) {
                        return currentOpen.isGreaterThan(slowValue) ? numOf(TrendDirectionEnum.VALID_UP_TREND.ordinal()) : numOf(TrendDirectionEnum.VALID_SEMI_UP_TREND.ordinal()); // VALID_UP or SEMI_UP
                    }
                } else if (seg0.getDirection() == TrendDirectionEnum.VALID_DOWN_TREND && last2.isLessThan(last0)) {
                    if (currentOpen.isLessThan(last0)) {
                        return currentOpen.isLessThan(slowValue) ? numOf(TrendDirectionEnum.VALID_DOWN_TREND.ordinal()) : numOf(TrendDirectionEnum.VALID_SEMI_DOWN_TREND.ordinal()); // VALID_DOWN or SEMI_DOWN
                    }
                }
            }
        } else if (segments.size() == 2) {
            PSARSegment seg0 = segments.get(0);
            Num lastSAR = fastSar.getValue(seg0.getLastIndex());

            if (seg0.getDirection() == TrendDirectionEnum.VALID_UP_TREND && currentOpen.isGreaterThan(lastSAR)) {
                return currentOpen.isGreaterThan(slowValue) ? numOf(TrendDirectionEnum.VALID_UP_TREND.ordinal()) : numOf(TrendDirectionEnum.VALID_SEMI_UP_TREND.ordinal()); // VALID_UP or SEMI_UP
            } else if (seg0.getDirection() == TrendDirectionEnum.VALID_DOWN_TREND && currentOpen.isLessThan(lastSAR)) {
                return currentOpen.isLessThan(slowValue) ? numOf(TrendDirectionEnum.VALID_DOWN_TREND.ordinal()) : numOf(TrendDirectionEnum.VALID_SEMI_DOWN_TREND.ordinal()); // VALID_DOWN or SEMI_DOWN
            }
        } else {
            Num fast = fastSar.getValue(index);
            if (fast.isLessThan(currentClose)) return numOf(TrendDirectionEnum.VALID_UP_TREND.ordinal());
            if (fast.isGreaterThan(currentClose)) return numOf(TrendDirectionEnum.VALID_DOWN_TREND.ordinal());
        }

        return numOf(TrendDirectionEnum.INVALID_TREND.ordinal()); // INVALID_TREND
    }

    private List<PSARSegment> extractLast3Segments(int fromIndex) {
        List<PSARSegment> segments = new ArrayList<>();
        TrendDirectionEnum currentDirection = null;
        PSARSegment currentSegment = null;

        for (int i = fromIndex; i >= 1 && segments.size() < 3; i--) {
            Num fast = fastSar.getValue(i);
            Num close = getBar(i).getClosePrice();
            Num slow = slowSar.getValue(i);

            TrendDirectionEnum fastDir = fast.isGreaterThan(close) ? TrendDirectionEnum.VALID_DOWN_TREND : TrendDirectionEnum.VALID_UP_TREND;
            TrendDirectionEnum slowDir = slow.isGreaterThan(close) ? TrendDirectionEnum.VALID_DOWN_TREND : TrendDirectionEnum.VALID_UP_TREND;

            if (fastDir != slowDir) break;

            if (fastDir != currentDirection) {
                currentDirection = fastDir;
                currentSegment = new PSARSegment(currentDirection);
                segments.add(0, currentSegment);
            }

            currentSegment.addIndex(i);
        }
        return segments;
    }

    private org.ta4j.core.Bar getBar(int i) {
        return getBarSeries().getBar(i);
    }

}
