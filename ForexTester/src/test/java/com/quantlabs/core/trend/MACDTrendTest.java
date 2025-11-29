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

public class MACDTrendTest {

    private BarSeries createSeries(boolean rising) {
        BarSeries series = new BaseBarSeries("macd-test");
        ZonedDateTime now = ZonedDateTime.now();
        double basePrice = 1.100;

        for (int i = 0; i < 50; i++) {
            double offset = i * 0.001;
            double open = rising ? basePrice + offset : basePrice - offset;
            double close = open + (rising ? 0.001 : -0.001);
            double high = Math.max(open, close) + 0.001;
            double low = Math.min(open, close) - 0.001;

            series.addBar(new BaseBar(
                    Duration.ofMinutes(1),
                    now.plusMinutes(i),
                    series.numOf(open),
                    series.numOf(high),
                    series.numOf(low),
                    series.numOf(close),
                    series.numOf(100),
                    series.numOf(close * 100)
            ));
        }

        return series;
    }

    @Test
    public void testMACDTrendDetectsUptrend() {
        BarSeries series = createSeries(true); // Rising market
        MACDTrend macdTrend = new MACDTrend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);

        TrendDirectionEnum trend = macdTrend.identifyTrend(0);

        assertNotNull(trend, "Trend should not be null");
        assertEquals(TrendDirectionEnum.VALID_UP_TREND, trend, "Expected VALID_UP_TREND");

        int index = series.getEndIndex();
        System.out.printf("[UP] MACD: %.5f, Signal: %.5f, Trend: %s%n",
                macdTrend.getMain(index).doubleValue(),
                macdTrend.getSignal(index).doubleValue(),
                trend);
    }

    @Test
    public void testMACDTrendDetectsDowntrend() {
        BarSeries series = createSeries(false); // Falling market
        MACDTrend macdTrend = new MACDTrend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);

        TrendDirectionEnum trend = macdTrend.identifyTrend(0);

        assertNotNull(trend, "Trend should not be null");
        assertEquals(TrendDirectionEnum.VALID_DOWN_TREND, trend, "Expected VALID_DOWN_TREND");

        int index = series.getEndIndex();
        System.out.printf("[DOWN] MACD: %.5f, Signal: %.5f, Trend: %s%n",
                macdTrend.getMain(index).doubleValue(),
                macdTrend.getSignal(index).doubleValue(),
                trend);
    }

    @Test
    public void testMACDTrendMainAndSignalNotNull() {
        BarSeries series = createSeries(true);
        MACDTrend macdTrend = new MACDTrend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);

        int index = series.getEndIndex();
        assertNotNull(macdTrend.getMain(index), "MACD value should not be null");
        assertNotNull(macdTrend.getSignal(index), "Signal value should not be null");
    }
}