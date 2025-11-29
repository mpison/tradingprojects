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

public class HeikenAshiTrendTest {

    private BarSeries createUptrendSeries() {
        BarSeries series = new BaseBarSeries("uptrend");
        ZonedDateTime now = ZonedDateTime.now();

        for (int i = 0; i < 50; i++) {
            double open = 1.10 + i * 0.0015;
            double close = open + 0.001;
            double high = close + 0.0005;
            double low = open - 0.0005;

            series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i),
                    series.numOf(open), series.numOf(high), series.numOf(low),
                    series.numOf(close), series.numOf(100), series.numOf(10000)));
        }

        return series;
    }

    private BarSeries createDowntrendSeries() {
        BarSeries series = new BaseBarSeries("downtrend");
        ZonedDateTime now = ZonedDateTime.now();

        for (int i = 0; i < 50; i++) {
            double open = 1.20 - i * 0.0015;
            double close = open - 0.001;
            double high = open + 0.0005;
            double low = close - 0.0005;

            series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i),
                    series.numOf(open), series.numOf(high), series.numOf(low),
                    series.numOf(close), series.numOf(100), series.numOf(10000)));
        }

        return series;
    }

    private BarSeries createFlatSeries() {
        BarSeries series = new BaseBarSeries("flat");
        ZonedDateTime now = ZonedDateTime.now();

        for (int i = 0; i < 50; i++) {
            double price = 1.15;
            series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i),
                    series.numOf(price), series.numOf(price), series.numOf(price),
                    series.numOf(price), series.numOf(100), series.numOf(10000)));
        }

        return series;
    }

    @Test
    public void testUptrend() {
        BarSeries series = createUptrendSeries();
        HeikenAshiTrend trend = new HeikenAshiTrend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);

        TrendDirectionEnum result = trend.identifyTrend(0);
        assertEquals(TrendDirectionEnum.VALID_UP_TREND, result);
    }

    @Test
    public void testDowntrend() {
        BarSeries series = createDowntrendSeries();
        HeikenAshiTrend trend = new HeikenAshiTrend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);

        TrendDirectionEnum result = trend.identifyTrend(0);
        assertEquals(TrendDirectionEnum.VALID_DOWN_TREND, result);
    }

    @Test
    public void testInvalidTrendDueToFlatCandles() {
        BarSeries series = createFlatSeries();
        HeikenAshiTrend trend = new HeikenAshiTrend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);

        TrendDirectionEnum result = trend.identifyTrend(0);
        assertEquals(TrendDirectionEnum.INVALID_TREND, result);
    }

    @Test
    public void testInvalidIndex() {
        BarSeries series = createUptrendSeries();
        HeikenAshiTrend trend = new HeikenAshiTrend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);

        TrendDirectionEnum result = trend.identifyTrend(1000); // way beyond series size
        assertEquals(TrendDirectionEnum.INVALID_TREND, result);
    }
}