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

public class PSARTrendTest {

    private BarSeries createSeries(boolean rising) {
        BarSeries series = new BaseBarSeries("psar-custom-test");
        ZonedDateTime now = ZonedDateTime.now();
        double basePrice = 1.200;

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
    public void testDefaultVsCustomPSARDifference() {
        BarSeries series = createSeries(false); // Falling trend

        PSARTrend defaultTrend = new PSARTrend(TimeFrameEnum.PERIOD_M1, 0, "USDJPY", false, series);
        double defaultPsar = defaultTrend.getPSAR(series.getEndIndex());

        PSARTrend customTrend = new PSARTrend(TimeFrameEnum.PERIOD_M1, 0, "USDJPY", false, series);
        customTrend.setStepSlow(0.01);
        customTrend.setMaxSlow(0.1);
        double customPsar = customTrend.getPSAR(series.getEndIndex());

        System.out.printf("Default PSAR: %.5f | Custom PSAR: %.5f%n", defaultPsar, customPsar);

        assertNotEquals(defaultPsar, customPsar, "Custom PSAR should differ from default");
    }

    @Test
    public void testDowntrendWithCustomParams() {
        BarSeries series = createSeries(false); // Falling trend

        PSARTrend trend = new PSARTrend(TimeFrameEnum.PERIOD_M1, 0, "USDJPY", false, series);
        trend.setStepSlow(0.01);
        trend.setMaxSlow(0.1);

        TrendDirectionEnum result = trend.identifyTrend(0);
        assertEquals(TrendDirectionEnum.VALID_DOWN_TREND, result, "Expected downtrend with custom PSAR");
    }

    @Test
    public void testUptrendWithCustomParams() {
        BarSeries series = createSeries(true); // Rising trend

        PSARTrend trend = new PSARTrend(TimeFrameEnum.PERIOD_M1, 0, "EURUSD", false, series);
        trend.setStepSlow(0.015);
        trend.setMaxSlow(0.18);

        TrendDirectionEnum result = trend.identifyTrend(0);
        assertEquals(TrendDirectionEnum.VALID_UP_TREND, result, "Expected uptrend with custom PSAR");
    }
}