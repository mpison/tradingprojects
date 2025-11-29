package com.quantlabs.core.indicators;

import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

import com.quantlabs.core.enums.TrendDirectionEnum;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PSAR21TrendIndicatorTest {
	
	
	@Test
    public void testValidUptrendConfirmation() {
        BarSeries series = PSAR21TestUtils.createStrongUptrendSeries();
        PSAR21TrendIndicator indicator = new PSAR21TrendIndicator(series, 0.05, 0.05, 0.01, 0.01);
        Num result = indicator.getValue(series.getEndIndex());
        assertEquals(TrendDirectionEnum.VALID_UP_TREND.ordinal(), result.intValue());
    }

	@Test
    public void testValidDowntrendConfirmation() {
        BarSeries series = new BaseBarSeriesBuilder().withName("strong-downtrend").build();
        ZonedDateTime now = ZonedDateTime.now();
        for (int i = 0; i < 20; i++) {
            double open = 1.2000 - i * 0.0006;
            double close = open - 0.0004;
            double high = open - 0.0002;
            double low = close - 0.0002;
            series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i),
            		series.numOf(open), series.numOf(high), series.numOf(low),
            		series.numOf(close), series.numOf(1), series.numOf(1)));
        }
        PSAR21TrendIndicator indicator = new PSAR21TrendIndicator(series, 0.05, 0.05, 0.01, 0.01);
        Num result = indicator.getValue(series.getEndIndex());
        assertEquals(TrendDirectionEnum.VALID_DOWN_TREND.ordinal(), result.intValue());
    }

    @Test
    public void testInvalidTrend() {
        BarSeries series = PSAR21TestUtils.createMixedTrendSeries();
        PSAR21TrendIndicator indicator = new PSAR21TrendIndicator(series, 0.05, 0.05, 0.01, 0.01);
        Num result = indicator.getValue(series.getEndIndex());
        assertEquals(TrendDirectionEnum.INVALID_TREND.ordinal(), result.intValue());
    }

    @Test
    public void testSemiUptrendConfirmation() {
        BarSeries series = createSegmentedUptrendSeries();
        PSAR21TrendIndicator indicator = new PSAR21TrendIndicator(series, 0.05, 0.05, 0.01, 0.01);
        Num result = indicator.getValue(series.getEndIndex());
        assertEquals(TrendDirectionEnum.VALID_SEMI_UP_TREND.ordinal(), result.intValue());
    }

    @Test
    public void testSemiDowntrendConfirmation() {
        BarSeries series = createSegmentedDowntrendSeries();
        PSAR21TrendIndicator indicator = new PSAR21TrendIndicator(series, 0.05, 0.05, 0.01, 0.01);
        Num result = indicator.getValue(series.getEndIndex());
        assertEquals(TrendDirectionEnum.VALID_SEMI_DOWN_TREND.ordinal(), result.intValue());
    }

    private BarSeries createSegmentedUptrendSeries() {
        BarSeries series = new BaseBarSeries("uptrend");
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-05:30"));

        for (int i = 0; i < 5; i++) {
            double open = 1.1000 + i * 0.0005;
            double close = open + 0.0003;
            double high = close + 0.0002;
            double low = open - 0.0002;
            series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i),
                    series.numOf(open), series.numOf(high), series.numOf(low), series.numOf(close),
                    series.numOf(1), series.numOf(1)));
        }

        for (int i = 5; i < 9; i++) {
            double open = series.getLastBar().getClosePrice().doubleValue() - 0.001;
            double close = open - 0.0005;
            double high = open + 0.0003;
            double low = close - 0.0002;
            series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i),
                    series.numOf(open), series.numOf(high), series.numOf(low), series.numOf(close),
                    series.numOf(1), series.numOf(1)));
        }

        for (int i = 9; i < 14; i++) {
            double open = series.getLastBar().getClosePrice().doubleValue() + 0.0004;
            double close = open + 0.0003;
            double high = close + 0.0002;
            double low = open - 0.0002;
            series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i),
                    series.numOf(open), series.numOf(high), series.numOf(low), series.numOf(close),
                    series.numOf(1), series.numOf(1)));
        }

        double open = series.getLastBar().getClosePrice().doubleValue() + 0.0003;
        double close = open + 0.0002;
        double high = close + 0.0001;
        double low = open - 0.0002;
        series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(14),
                series.numOf(open), series.numOf(high), series.numOf(low), series.numOf(close),
                series.numOf(1), series.numOf(1)));

        return series;
    }

    private BarSeries createSegmentedDowntrendSeries() {
        BarSeries series = new BaseBarSeries("downtrend");
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-05:30"));

        for (int i = 0; i < 5; i++) {
            double open = 1.1200 - i * 0.0005;
            double close = open - 0.0003;
            double high = open + 0.0002;
            double low = close - 0.0002;
            series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i),
                    series.numOf(open), series.numOf(high), series.numOf(low), series.numOf(close),
                    series.numOf(1), series.numOf(1)));
        }

        for (int i = 5; i < 9; i++) {
            double open = series.getLastBar().getClosePrice().doubleValue() + 0.001;
            double close = open + 0.0005;
            double high = close + 0.0002;
            double low = open - 0.0002;
            series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i),
                    series.numOf(open), series.numOf(high), series.numOf(low), series.numOf(close),
                    series.numOf(1), series.numOf(1)));
        }

        for (int i = 9; i < 14; i++) {
            double open = series.getLastBar().getClosePrice().doubleValue() - 0.0004;
            double close = open - 0.0003;
            double high = open + 0.0002;
            double low = close - 0.0002;
            series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i),
                    series.numOf(open), series.numOf(high), series.numOf(low), series.numOf(close),
                    series.numOf(1), series.numOf(1)));
        }

        double open = series.getLastBar().getClosePrice().doubleValue() - 0.0003;
        double close = open - 0.0002;
        double high = open + 0.0001;
        double low = close - 0.0002;
        series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(14),
                series.numOf(open), series.numOf(high), series.numOf(low), series.numOf(close),
                series.numOf(1), series.numOf(1)));

        return series;
    }
}
