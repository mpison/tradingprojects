package com.quantlabs.core.trend;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.core.enums.TimeFrameEnum;
import com.quantlabs.core.enums.TrendClassEnum;
import com.quantlabs.core.enums.TrendDirectionEnum;

public class PSAR2Trend extends PSARTrend {

    private double stepFast = 0.05;
    private double maxFast = 0.05;

    private ParabolicSarIndicator fastSar;

    public PSAR2Trend(TimeFrameEnum timeframe, int shift, String symbol, boolean isUseMomentum, BarSeries series) {
        super(timeframe, shift, symbol, isUseMomentum, series);
        this.setTrendClassEnum(TrendClassEnum.PSAR2_TREND_CLASS);
    }

    @Override
    public void calculate() {
        super.calculate(); // initializes slow SAR from parent
        fastSar = new ParabolicSarIndicator(getSeries(), getSeries().numOf(stepFast), getSeries().numOf(maxFast));
    }

    @Override
    public TrendDirectionEnum identifyTrend(int myShift) {
        if (getSeries() == null || getSeries().getBarCount() <= myShift) {
            return TrendDirectionEnum.INVALID_TREND;
        }

        int index = getSeries().getEndIndex() - myShift;
        if (index < 1 || index >= getSeries().getBarCount()) {
            return TrendDirectionEnum.INVALID_TREND;
        }

        Num price = getSeries().getBar(index).getClosePrice();
        Num slowSarVal = getPSarIndicator().getValue(index);
        Num fastSarVal = fastSar.getValue(index);

        boolean isUp = price.isGreaterThan(slowSarVal) && price.isGreaterThan(fastSarVal);
        boolean isDown = price.isLessThan(slowSarVal) && price.isLessThan(fastSarVal);

        if (isUp) return TrendDirectionEnum.VALID_UP_TREND;
        if (isDown) return TrendDirectionEnum.VALID_DOWN_TREND;

        return TrendDirectionEnum.INVALID_TREND;
    }

    public double getStepFast() {
        return stepFast;
    }

    public void setStepFast(double stepFast) {
        this.stepFast = stepFast;
    }

    public double getMaxFast() {
        return maxFast;
    }

    public void setMaxFast(double maxFast) {
        this.maxFast = maxFast;
    }

    public ParabolicSarIndicator getFastSar() {
        return fastSar;
    }
}