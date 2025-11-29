package com.quantlabs.core.trend;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.core.enums.TimeFrameEnum;
import com.quantlabs.core.enums.TrendClassEnum;
import com.quantlabs.core.enums.TrendDirectionEnum;

public class PSARTrend extends ABaseTrend {

    private ParabolicSarIndicator psarIndicator;

    private double stepSlow = 0.01;
    private double maxSlow = 0.01;

    public PSARTrend(TimeFrameEnum timeframe, int shift, String symbol, boolean isUseMomentum, BarSeries series) {
        super(timeframe, shift, TrendClassEnum.PSAR_TREND_CLASS, symbol, isUseMomentum, series);
        this.series = series;
        calculate();
    }

    @Override
    public void calculate() {
        Num stepNum = series.numOf(stepSlow);
        Num maxNum = series.numOf(maxSlow);
        psarIndicator = new ParabolicSarIndicator(series, stepNum, maxNum);
    }

    @Override
    public TrendDirectionEnum identifyTrend(int myShift) {
        if (series == null || series.getBarCount() <= myShift) {
            return TrendDirectionEnum.INVALID_TREND;
        }

        int index = series.getEndIndex() - myShift;
        if (index < 0 || index >= series.getBarCount()) {
            return TrendDirectionEnum.INVALID_TREND;
        }

        double close = series.getBar(index).getClosePrice().doubleValue();
        double psar = psarIndicator.getValue(index).doubleValue();

        if (close > psar) {
            return TrendDirectionEnum.VALID_UP_TREND;
        } else if (close < psar) {
            return TrendDirectionEnum.VALID_DOWN_TREND;
        } else {
            return TrendDirectionEnum.INVALID_TREND;
        }
    }
    
    protected ParabolicSarIndicator getPSarIndicator() {
        return psarIndicator;
    }

    public double getPSAR(int index) {
        return psarIndicator.getValue(index).doubleValue();
    }

    // === Getters & Setters for step and max ===
    public double getStepSlow() {
        return stepSlow;
    }

    public void setStepSlow(double stepSlow) {
        this.stepSlow = stepSlow;
        calculate(); // reinitialize PSAR
    }

    public double getMaxSlow() {
        return maxSlow;
    }

    public void setMaxSlow(double maxSlow) {
        this.maxSlow = maxSlow;
        calculate(); // reinitialize PSAR
    }
}