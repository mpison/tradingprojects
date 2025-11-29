package com.quantlabs.core.indicators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.Num;

public class HeikenAshiIndicatorTest {

	private BarSeries createUptrendSeries() {
		BarSeries series = new BaseBarSeries("uptrend");
		ZonedDateTime now = ZonedDateTime.now();
		for (int i = 0; i < 20; i++) {
			double open = 1.10 + i * 0.001;
			double close = open + 0.001;
			double high = close + 0.0005;
			double low = open - 0.0005;
			series.addBar(new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i), series.numOf(open), series.numOf(high),
					series.numOf(low), series.numOf(close), series.numOf(100), series.numOf(10000)));
		}
		return series;
	}

	private BarSeries createFlatSeries() {
		BarSeries series = new BaseBarSeries("flat");
		ZonedDateTime now = ZonedDateTime.now();
		for (int i = 0; i < 20; i++) {
			double price = 1.15;
			series.addBar(
					new BaseBar(Duration.ofMinutes(1), now.plusMinutes(i), series.numOf(price), series.numOf(price),
							series.numOf(price), series.numOf(price), series.numOf(100), series.numOf(10000)));
		}
		return series;
	}

	@Test
	public void testHeikenAshiValuesAreNotNull() {
		BarSeries series = createUptrendSeries();
		HeikenAshiIndicator indicator = new HeikenAshiIndicator(series);

		for (int i = 0; i < series.getBarCount(); i++) {
			assertNotNull(indicator.getHeikenAshiOpen(i), "HA Open should not be null at " + i);
			assertNotNull(indicator.getHeikenAshiClose(i), "HA Close should not be null at " + i);
		}
	}

	@Test
	public void testHeikenAshiCloseCalculation() {
		BarSeries series = createUptrendSeries();
		HeikenAshiIndicator indicator = new HeikenAshiIndicator(series);

		Num expected = series.getBar(0).getOpenPrice().plus(series.getBar(0).getHighPrice())
				.plus(series.getBar(0).getLowPrice()).plus(series.getBar(0).getClosePrice()).dividedBy(series.numOf(4));

		assertEquals(expected, indicator.getHeikenAshiClose(0), "HA Close mismatch at index 0");
	}

	@Test
	public void testHeikenAshiOpenStartsWithBarOpen() {
		BarSeries series = createUptrendSeries();
		HeikenAshiIndicator indicator = new HeikenAshiIndicator(series);

		Num haOpen0 = indicator.getHeikenAshiOpen(0);
		Num barOpen0 = series.getBar(0).getOpenPrice();
		assertEquals(barOpen0, haOpen0, "First HA open must match original bar open");
	}

	@Test
	public void testFlatSeriesReturnsStableHAValues() {
		BarSeries series = createFlatSeries();
		HeikenAshiIndicator indicator = new HeikenAshiIndicator(series);

		Num expected = series.numOf(1.15);
		for (int i = 0; i < series.getBarCount(); i++) {
			assertEquals(expected, indicator.getHeikenAshiClose(i), "HA Close should match flat price");
		}
	}
}