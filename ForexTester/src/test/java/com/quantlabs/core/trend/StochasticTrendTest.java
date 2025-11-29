package com.quantlabs.core.trend;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;

import com.quantlabs.core.enums.TimeFrameEnum;
import com.quantlabs.core.enums.TrendDirectionEnum;

import java.time.Duration;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class StochasticTrendTest {

    private BarSeries createSeries(boolean rising) {
        BarSeries series = new BaseBarSeries("stoch-test");
        ZonedDateTime now = ZonedDateTime.now();
        double base = rising ? 1.00 : 1.20;

        for (int i = 0; i < 50; i++) {
            double offset = i * 0.001;
            double open = rising ? base + offset : base - offset;
            double close = rising ? open + 0.001 : open - 0.001;
            double high = Math.max(open, close) + 0.001;
            double low = Math.min(open, close) - 0.001;

            series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i),
                    series.numOf(open), series.numOf(high), series.numOf(low),
                    series.numOf(close), series.numOf(100), series.numOf(10000)));
        }

        return series;
    }
    
    private BarSeries createDowntrendSeriesForLevelTest() {
        BarSeries series = new BaseBarSeries("downtrend-series");
        ZonedDateTime now = ZonedDateTime.now();
        double base = 1.2000;

        for (int i = 0; i < 50; i++) {
            // Prices drop faster over time
            double drop = i * 0.002;
            double open = base - drop;
            double close = open - 0.0015;
            double high = open + 0.0005;
            double low = close - 0.0005;

            series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i),
                    series.numOf(open), series.numOf(high), series.numOf(low),
                    series.numOf(close), series.numOf(100), series.numOf(10000)));
        }

        return series;
    }

    @Test
    public void testDowntrendWithoutLevel() {
        BarSeries series = createSeries(true);
        StochasticTrend trend = new StochasticTrend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);
        trend.setUseLevel(false);

        TrendDirectionEnum result = trend.identifyTrend(0);
        assertEquals(TrendDirectionEnum.VALID_DOWN_TREND, result);
    }

    @Test
    public void testUptrendWithoutLevel() {
        BarSeries series = createSeries(false);
        StochasticTrend trend = new StochasticTrend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);
        trend.setUseLevel(false);

        TrendDirectionEnum result = trend.identifyTrend(0);
        assertEquals(TrendDirectionEnum.VALID_UP_TREND, result);
    }

    @Test
    public void testInvalidTrendWhenLevelBlocked() {
        BarSeries series = createDowntrendSeriesForLevelTest();
        StochasticTrend trend = new StochasticTrend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);

        trend.setUseLevel(true);
        trend.setLevelUp(85);
        trend.setLevelDown(30); // Should allow deep stochastic crossovers

        TrendDirectionEnum result = trend.identifyTrend(0);
        assertEquals(TrendDirectionEnum.INVALID_TREND, result, "Expected invalid trend inspite downtrend with crossover but below levelDown");
    }
    
    @Test
    public void testUptrendWithLevelConstraint2() {
        BarSeries series = createSeries(false);
        StochasticTrend trend = new StochasticTrend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);
        trend.setUseLevel(true);
        trend.setLevelUp(85);
        trend.setLevelDown(15);

        TrendDirectionEnum result = trend.identifyTrend(0);
        assertEquals(TrendDirectionEnum.VALID_UP_TREND, result);
    }

    @Test
    public void testDowntrendWithLevelConstraint() {
        BarSeries series = createSeries(true);
        StochasticTrend trend = new StochasticTrend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);
        trend.setUseLevel(true);
        trend.setLevelUp(40); // Force %K > %D but above levelUp
        trend.setLevelDown(10);

        TrendDirectionEnum result = trend.identifyTrend(0);
        assertEquals(TrendDirectionEnum.VALID_DOWN_TREND, result, "Expected downtrend with crossover above levelDown");
    }
}