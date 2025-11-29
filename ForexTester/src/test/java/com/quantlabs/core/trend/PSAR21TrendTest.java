package com.quantlabs.core.trend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;

import com.quantlabs.core.enums.TimeFrameEnum;
import com.quantlabs.core.enums.TrendDirectionEnum;


public class PSAR21TrendTest {

    private BarSeries createUptrendSeries() {
        BarSeries series = new BaseBarSeries("uptrend");
        ZonedDateTime now = ZonedDateTime.now();
        for (int i = 0; i < 20; i++) {
            double open = 1.1000 + i * 0.0005;
            double close = open + 0.0004;
            double high = close + 0.0002;
            double low = open - 0.0002;
            series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i),
            		series.numOf(open), series.numOf(high), series.numOf(low),
            		series.numOf(close), series.numOf(1), series.numOf(1)));
        }
        return series;
    }

    private BarSeries createDowntrendSeries() {
        BarSeries series = new BaseBarSeries("downtrend");
        ZonedDateTime now = ZonedDateTime.now();
        for (int i = 0; i < 20; i++) {
            double open = 1.2000 - i * 0.0005;
            double close = open - 0.0004;
            double high = open - 0.0002;
            double low = close - 0.0002;
            series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i),
            		series.numOf(open), series.numOf(high), series.numOf(low),
            		series.numOf(close), series.numOf(1), series.numOf(1)));
        }
        return series;
    }

    @Test
    public void testValidUptrend() {
        BarSeries series = createUptrendSeries();
        PSAR21Trend trend = new PSAR21Trend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);
        TrendDirectionEnum trendResult = trend.identifyTrend(0);
        assertEquals(TrendDirectionEnum.VALID_UP_TREND, trendResult);
    }

    @Test
    public void testValidDowntrend() {
        BarSeries series = createDowntrendSeries();
        PSAR21Trend trend = new PSAR21Trend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);
        TrendDirectionEnum trendResult = trend.identifyTrend(0);
        assertEquals(TrendDirectionEnum.VALID_DOWN_TREND, trendResult);
    }

    @Test
    public void testInvalidTrendForEmptySeries() {
        BarSeries series = new BaseBarSeries("empty");
        PSAR21Trend trend = new PSAR21Trend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);
        trend.calculate();
        assertEquals(TrendDirectionEnum.INVALID_UNINITIALIZED, trend.getTrendResult());
    }
} 
