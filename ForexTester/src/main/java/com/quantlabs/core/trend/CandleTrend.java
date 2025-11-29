package com.quantlabs.core.trend;

import com.quantlabs.core.enums.TimeFrameEnum;
import com.quantlabs.core.enums.TrendClassEnum;
import com.quantlabs.core.enums.TrendDirectionEnum;

import org.ta4j.core.num.Num;

public class CandleTrend extends ABaseTrend {

    private boolean isCompareMultiIndex = false;

    public CandleTrend(TimeFrameEnum timeframe, int shift, String symbol, boolean isUseMomentum, org.ta4j.core.BarSeries series) {
        super(timeframe, shift, TrendClassEnum.CANDLE_TREND_CLASS, symbol, isUseMomentum, series);
    }

    @Override
    public void calculate() {
        // No calculation needed for this simple trend
    }

    @Override
    public TrendDirectionEnum identifyTrend(int myShift) {
        if (series == null || series.getBarCount() <= myShift) {
            return TrendDirectionEnum.INVALID_TREND;
        }

        int currentIndex = series.getEndIndex();
        int compareIndex = isCompareMultiIndex ? currentIndex - myShift : currentIndex - myShift;

        if (compareIndex < 0 || compareIndex >= series.getBarCount()) {
            return TrendDirectionEnum.INVALID_TREND;
        }

        Num open;
        Num close;

        if (isCompareMultiIndex) {
            // Compare close[0] vs open[shift]
            open = series.getBar(compareIndex).getOpenPrice();
            close = series.getBar(currentIndex).getClosePrice();
        } else {
            // Standard candle check: close[shift] vs open[shift]
            open = series.getBar(compareIndex).getOpenPrice();
            close = series.getBar(compareIndex).getClosePrice();
        }

        if (open == null || close == null || open.isLessThanOrEqual(series.numOf(0)) || close.isLessThanOrEqual(series.numOf(0))) {
            return TrendDirectionEnum.INVALID_TREND;
        }

        if (close.isGreaterThan(open)) {
            return TrendDirectionEnum.VALID_UP_TREND;
        } else if (close.isLessThan(open)) {
            return TrendDirectionEnum.VALID_DOWN_TREND;
        }

        return TrendDirectionEnum.INVALID_TREND;
    }

    public boolean isCompareMultiIndex() {
        return isCompareMultiIndex;
    }

    public void setCompareMultiIndex(boolean compareMultiIndex) {
        isCompareMultiIndex = compareMultiIndex;
    }
}