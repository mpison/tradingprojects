package com.quantlabs.core.trend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;

import com.quantlabs.core.enums.TimeFrameEnum;
import com.quantlabs.core.enums.TrendDirectionEnum;

public class CandleTrendTest {

    private BarSeries createBasicSeriesUpDownFlat() {
        BarSeries series = new BaseBarSeries("test");
        ZonedDateTime now = ZonedDateTime.now();

        // Index 0: close < open (down)
        series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(0),
                series.numOf(1.12), series.numOf(1.13), series.numOf(1.11), series.numOf(1.10),
                series.numOf(100), series.numOf(1000)));

        // Index 1: close > open (up)
        series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(1),
                series.numOf(1.10), series.numOf(1.12), series.numOf(1.09), series.numOf(1.13),
                series.numOf(100), series.numOf(1000)));

        // Index 2: flat
        series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(2),
                series.numOf(1.15), series.numOf(1.15), series.numOf(1.15), series.numOf(1.15),
                series.numOf(100), series.numOf(1000)));

        return series;
    }

    @Test
    public void testStandardModeUpTrend() {
        BarSeries series = createBasicSeriesUpDownFlat();
        CandleTrend trend = new CandleTrend(TimeFrameEnum.PERIOD_M1, 1, "EURUSD", false, series);
        trend.setCompareMultiIndex(false);

        TrendDirectionEnum result = trend.identifyTrend();
        assertEquals(TrendDirectionEnum.VALID_UP_TREND, result);
    }

    @Test
    public void testStandardModeDownTrend() {
        BarSeries series = createBasicSeriesUpDownFlat();
        CandleTrend trend = new CandleTrend(TimeFrameEnum.PERIOD_M1, 2, "EURUSD", false, series);
        trend.setCompareMultiIndex(false);

        TrendDirectionEnum result = trend.identifyTrend();
        assertEquals(TrendDirectionEnum.VALID_DOWN_TREND, result);
    }

    @Test
    public void testStandardModeInvalidTrend() {
        BarSeries series = createBasicSeriesUpDownFlat();
        CandleTrend trend = new CandleTrend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);
        trend.setCompareMultiIndex(false);

        TrendDirectionEnum result = trend.identifyTrend();
        assertEquals(TrendDirectionEnum.INVALID_TREND, result); // Bar 0: close < open
    }

    @Test
    public void testMultiIndexModeUpTrend() {
        BarSeries series = createBasicSeriesUpDownFlat();
        CandleTrend trend = new CandleTrend(TimeFrameEnum.PERIOD_M1, 1, "EURUSD", false, series);
        trend.setCompareMultiIndex(true);

        // close[0] = 1.10, open[1] = 1.10 → INVALID
        TrendDirectionEnum result = trend.identifyTrend();
        assertEquals(TrendDirectionEnum.VALID_UP_TREND, result);
    }

    @Test
    public void testMultiIndexModeDownTrend() {
        BarSeries series = new BaseBarSeries("custom-downtrend");
        ZonedDateTime now = ZonedDateTime.now();

        // Bar 0 (index 0): Close lower than Bar 1's open
        series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(0),
                series.numOf(1.13),  // open
                series.numOf(1.13),  // high
                series.numOf(1.11),  // low
                series.numOf(1.08),  // close ← lower than open[1]
                series.numOf(100), series.numOf(1000)));

        // Bar 1 (index 1): higher open / latest or most current
        series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(1),
                series.numOf(1.10),  // open
                series.numOf(1.12),  // high
                series.numOf(1.09),  // low
                series.numOf(1.11),  // close
                series.numOf(100), series.numOf(1000)));

        CandleTrend trend = new CandleTrend(TimeFrameEnum.PERIOD_M1, 1, "EURUSD", false, series);
        trend.setCompareMultiIndex(true);

        TrendDirectionEnum result = trend.identifyTrend();
        assertEquals(TrendDirectionEnum.VALID_DOWN_TREND, result);
    }
}