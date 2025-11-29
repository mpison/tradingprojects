package com.quantlabs.core.indicators;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;

public class PSAR21TestUtils {

	public static BarSeries createStrongUptrendSeries() {
		BarSeries series = new BaseBarSeries("strong-uptrend");
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-05:30"));

		for (int i = 0; i < 15; i++) {
			double open = 1.1000 + i * 0.0005;
			double close = open + 0.0003;
			double high = close + 0.0002;
			double low = open - 0.0002;
			series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i), series.numOf(open), series.numOf(high),
					series.numOf(low), series.numOf(close), series.numOf(1), series.numOf(1)));
		}
		return series;
	}

	public static BarSeries createStrongDowntrendSeries() {
		BarSeries series = new BaseBarSeries("strong-downtrend");
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-05:30"));

		for (int i = 0; i < 15; i++) {
			double open = 1.1200 - i * 0.0005;
			double close = open - 0.0003;
			double high = open + 0.0002;
			double low = close - 0.0002;
			series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i), series.numOf(open), series.numOf(high),
					series.numOf(low), series.numOf(close), series.numOf(1), series.numOf(1)));
		}
		return series;
	}

	public static BarSeries createMixedTrendSeries() {
	    BarSeries series = new BaseBarSeries("mixed-trend");
	    ZonedDateTime now = ZonedDateTime.now(ZoneId.of("GMT-05:30"));

	    double[] opens = {1.1000, 1.1004, 1.1002, 1.1005, 1.1001, 1.1003, 1.1000, 1.1004, 1.1002, 1.1003};
	    double[] closes = {1.1003, 1.1001, 1.1005, 1.1002, 1.1004, 1.1000, 1.1003, 1.1001, 1.1005, 1.1002};

	    for (int i = 0; i < opens.length; i++) {
	        double open = opens[i];
	        double close = closes[i];
	        double high = Math.max(open, close) + 0.0002;
	        double low = Math.min(open, close) - 0.0002;
	        series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i),
	            series.numOf(open), series.numOf(high), series.numOf(low), series.numOf(close),
	            series.numOf(1), series.numOf(1)));
	    }

	    return series;
	}
}
