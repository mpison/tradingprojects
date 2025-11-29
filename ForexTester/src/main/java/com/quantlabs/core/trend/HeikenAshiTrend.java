package com.quantlabs.core.trend;

import com.quantlabs.core.enums.TimeFrameEnum;
import com.quantlabs.core.enums.TrendClassEnum;
import com.quantlabs.core.enums.TrendDirectionEnum;
import com.quantlabs.core.indicators.HeikenAshiIndicator;

import org.ta4j.core.BarSeries;
import org.ta4j.core.num.Num;

public class HeikenAshiTrend extends ABaseTrend {

    private HeikenAshiIndicator heikenAshiIndicator;

    public HeikenAshiTrend(TimeFrameEnum timeframe, int shift, String symbol, boolean isUseMomentum, BarSeries series) {
        super(timeframe, shift, TrendClassEnum.HEIKEN_ASHI_TREND_CLASS, symbol, isUseMomentum, series);
        calculate();
    }

    @Override
    public void calculate() {
        this.heikenAshiIndicator = new HeikenAshiIndicator(series);
    }

    @Override
    public TrendDirectionEnum identifyTrend(int myShift) {
        if (series == null || series.getBarCount() <= myShift) {
            return TrendDirectionEnum.INVALID_TREND;
        }

        int index = series.getEndIndex() - myShift;
        if (index < 1 || index >= series.getBarCount()) {
            return TrendDirectionEnum.INVALID_TREND;
        }

        Num open = heikenAshiIndicator.getHeikenAshiOpen(index);
        Num close = heikenAshiIndicator.getHeikenAshiClose(index);

        // Ensure both open and close are > 0
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

    public Num getHeikenAshiOpen(int index) {
        return heikenAshiIndicator.getHeikenAshiOpen(index);
    }

    public Num getHeikenAshiClose(int index) {
        return heikenAshiIndicator.getHeikenAshiClose(index);
    }

    public HeikenAshiIndicator getIndicator() {
        return heikenAshiIndicator;
    }
}