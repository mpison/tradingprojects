package com.quantlabs.core.trend;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;

import com.quantlabs.core.enums.TimeFrameEnum;
import com.quantlabs.core.enums.TrendClassEnum;
import com.quantlabs.core.enums.TrendDirectionEnum;

public class StochasticTrend extends ABaseTrend {

    private StochasticOscillatorKIndicator stochasticK;
    private SMAIndicator stochasticD;

    private int kPeriod = 14;
    private int dPeriod = 3;

    private boolean useLevel = true;
    private int levelUp = 80;
    private int levelDown = 20;

    public StochasticTrend(TimeFrameEnum timeframe, int shift, String symbol, boolean isUseMomentum, BarSeries series) {
        super(timeframe, shift, TrendClassEnum.STOCH_TREND_CLASS, symbol, isUseMomentum, series);
        calculate();
    }

    @Override
    public void calculate() {
        this.stochasticK = new StochasticOscillatorKIndicator(series, kPeriod);
        this.stochasticD = new SMAIndicator(stochasticK, dPeriod);
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

        double istochMain = stochasticK.getValue(index).doubleValue();
        double istochSignal = stochasticD.getValue(index).doubleValue();

        if (!this.useLevel) {
            if (istochMain > istochSignal) {
                return TrendDirectionEnum.VALID_UP_TREND;
            } else if (istochMain < istochSignal) {
                return TrendDirectionEnum.VALID_DOWN_TREND;
            }
        } else {
            if (istochMain > istochSignal) {
                if (istochMain <= levelUp && istochSignal <= levelUp) {
                    return TrendDirectionEnum.VALID_UP_TREND;
                }
            } else if (istochMain <= istochSignal) {
                if (istochMain >= levelDown && istochSignal >= levelDown) {
                    return TrendDirectionEnum.VALID_DOWN_TREND;
                }
            }
        }

        return TrendDirectionEnum.INVALID_TREND;
    }

    // Accessors
    public double getStochasticK(int index) {
        return stochasticK.getValue(index).doubleValue();
    }

    public double getStochasticD(int index) {
        return stochasticD.getValue(index).doubleValue();
    }

    public void setKPeriod(int kPeriod) {
        this.kPeriod = kPeriod;
        calculate();
    }

    public void setDPeriod(int dPeriod) {
        this.dPeriod = dPeriod;
        calculate();
    }

    public void setUseLevel(boolean useLevel) {
        this.useLevel = useLevel;
    }

    public boolean isUseLevel() {
        return useLevel;
    }

    public void setLevelUp(int levelUp) {
        this.levelUp = levelUp;
    }

    public void setLevelDown(int levelDown) {
        this.levelDown = levelDown;
    }

    public int getLevelUp() {
        return levelUp;
    }

    public int getLevelDown() {
        return levelDown;
    }
}
