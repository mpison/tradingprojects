package com.quantlabs.core.trend;

import com.quantlabs.core.enums.TimeFrameEnum;
import com.quantlabs.core.enums.TrendClassEnum;
import com.quantlabs.core.enums.TrendDirectionEnum;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

public class MACDTrend extends ABaseTrend {

    private int shortEmaBarCount = 12;
    private int longEmaBarCount = 26;
    private int signalBarCount = 9;

    private MACDIndicator macdIndicator;
    private EMAIndicator signalIndicator;

    public MACDTrend(TimeFrameEnum timeframe, int shift, String symbol, boolean isUseMomentum, BarSeries series) {
        super(timeframe, shift, TrendClassEnum.MACD_TREND_CLASS, symbol, isUseMomentum, series);
        setupIndicators();
    }

    private void setupIndicators() {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        macdIndicator = new MACDIndicator(closePrice, shortEmaBarCount, longEmaBarCount);
        signalIndicator = new EMAIndicator(macdIndicator, signalBarCount);
    }

    @Override
    public TrendDirectionEnum identifyTrend(int myShift) {
        int currentIndex = series.getEndIndex() - myShift;

        if (series == null || currentIndex < 0 || currentIndex >= series.getBarCount()) {
            return TrendDirectionEnum.INVALID_TREND;
        }

        double macdNow = getMain(currentIndex).doubleValue();
        double signalNow = getSignal(currentIndex).doubleValue();

        if (macdNow > signalNow) {
            return TrendDirectionEnum.VALID_UP_TREND;
        } else if (macdNow < signalNow) {
            return TrendDirectionEnum.VALID_DOWN_TREND;
        } else {
            return TrendDirectionEnum.INVALID_TREND;
        }
    }

    public Num getMain(int index) {
        return macdIndicator.getValue(index);
    }

    public Num getSignal(int index) {
        return signalIndicator.getValue(index);
    }

    @Override
    public void calculate() {
        setupIndicators();
    }

    // Configurable MACD parameters
    public int getShortEmaBarCount() {
        return shortEmaBarCount;
    }

    public void setShortEmaBarCount(int shortEmaBarCount) {
        this.shortEmaBarCount = shortEmaBarCount;
        setupIndicators();
    }

    public int getLongEmaBarCount() {
        return longEmaBarCount;
    }

    public void setLongEmaBarCount(int longEmaBarCount) {
        this.longEmaBarCount = longEmaBarCount;
        setupIndicators();
    }

    public int getSignalBarCount() {
        return signalBarCount;
    }

    public void setSignalBarCount(int signalBarCount) {
        this.signalBarCount = signalBarCount;
        setupIndicators();
    }
}