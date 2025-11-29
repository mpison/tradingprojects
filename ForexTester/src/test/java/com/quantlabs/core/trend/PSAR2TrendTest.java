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

public class PSAR2TrendTest {

    private BarSeries createDowntrendSeries() {
        BarSeries series = new BaseBarSeries("psar-downtrend");
        ZonedDateTime time = ZonedDateTime.now();
        double basePrice = 1.2000;

        for (int i = 0; i < 50; i++) {
            double drop = 0.0007 * i;
            double open = basePrice - drop;
            double close = open - 0.0010;
            double high = open + 0.0005;
            double low = close - 0.0005;

            series.addBar(new BaseBar(Duration.ofMinutes(1), time.plusMinutes(i),
                    series.numOf(open), series.numOf(high),
                    series.numOf(low), series.numOf(close),
                    series.numOf(100), series.numOf(1000)));
        }

        return series;
    }

    private BarSeries createUptrendSeries() {
        BarSeries series = new BaseBarSeries("psar-uptrend");
        ZonedDateTime time = ZonedDateTime.now();
        double basePrice = 1.1000;

        for (int i = 0; i < 50; i++) {
            double gain = 0.0007 * i;
            double open = basePrice + gain;
            double close = open + 0.0010;
            double high = close + 0.0005;
            double low = open - 0.0005;

            series.addBar(new BaseBar(Duration.ofMinutes(1), time.plusMinutes(i),
                    series.numOf(open), series.numOf(high),
                    series.numOf(low), series.numOf(close),
                    series.numOf(100), series.numOf(1000)));
        }

        return series;
    }

    private BarSeries createMixedTrendSeries() {
        BarSeries series = new BaseBarSeries("psar-mixedtrend");
        ZonedDateTime time = ZonedDateTime.now();
        double basePrice = 1.1500;

        // Slow uptrend (first 45 bars)
        for (int i = 0; i < 45; i++) {
            double open = basePrice + i * 0.0003;
            double close = open + 0.0002;
            double high = close + 0.0002;
            double low = open - 0.0002;

            series.addBar(new BaseBar(Duration.ofMinutes(1), time.plusMinutes(i),
                    series.numOf(open), series.numOf(high),
                    series.numOf(low), series.numOf(close),
                    series.numOf(100), series.numOf(1000)));
        }

        // Sharp drop (last 5 bars)
        double prevClose = series.getLastBar().getClosePrice().doubleValue();
        for (int i = 0; i < 5; i++) {
            double open = prevClose - 0.001;
            double close = open - 0.0005;
            double high = open + 0.0002;
            double low = close - 0.0002;
            prevClose = close;

            series.addBar(new BaseBar(Duration.ofMinutes(1), time.plusMinutes(45 + i),
                    series.numOf(open), series.numOf(high),
                    series.numOf(low), series.numOf(close),
                    series.numOf(100), series.numOf(1000)));
        }

        return series;
    }


    @Test
    public void testDowntrendIntersection() {
        BarSeries series = createDowntrendSeries();
        PSAR2Trend trend = new PSAR2Trend(TimeFrameEnum.PERIOD_M1, 1, "EURUSD", false, series);
        trend.calculate();

        TrendDirectionEnum result = trend.identifyTrend();
        assertEquals(TrendDirectionEnum.VALID_DOWN_TREND, result);
    }

    @Test
    public void testUptrendIntersection() {
        BarSeries series = createUptrendSeries();
        PSAR2Trend trend = new PSAR2Trend(TimeFrameEnum.PERIOD_M1, 1, "EURUSD", false, series);
        trend.calculate();

        TrendDirectionEnum result = trend.identifyTrend();
        assertEquals(TrendDirectionEnum.VALID_UP_TREND, result);
    }

    @Test
    public void testMixedTrendShouldBeInvalid() {
        BarSeries series = createMixedTrendSeries();
        PSAR2Trend trend = new PSAR2Trend(TimeFrameEnum.PERIOD_M1, 1, "EURUSD", false, series);
        trend.calculate();

        TrendDirectionEnum result = trend.identifyTrend();
        assertEquals(TrendDirectionEnum.INVALID_TREND, result);
    }
}