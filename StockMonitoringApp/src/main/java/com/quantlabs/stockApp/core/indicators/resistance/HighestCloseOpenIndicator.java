package com.quantlabs.stockApp.core.indicators.resistance;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public class HighestCloseOpenIndicator extends CachedIndicator<HighestCloseOpenIndicator.HighestValue> {
	private final String period;
	private final boolean useClose;
	private final String provider;
	private final String session;
	private final int sessionEndIndex;

	private static final ZoneId EASTERN_ZONE = ZoneId.of("America/New_York");
	private static final ZoneId PACIFIC_ZONE = ZoneId.of("America/Los_Angeles");

	public static class HighestValue {
		public final Num value;
		public final ZonedDateTime time; // Time will be in provider's timezone

		public HighestValue(Num value, ZonedDateTime time) {
			this.value = value;
			this.time = time;
		}

		@Override
		public String toString() {
			return String.format("HighestValue{value=%.2f, time=%s}", value.doubleValue(),
					time != null ? time.toString() : "null");
		}
	}

	public HighestCloseOpenIndicator(BarSeries series, String period, boolean useClose, String provider) {
		this(series, period, useClose, provider, "all", series.getEndIndex());
	}

	public HighestCloseOpenIndicator(BarSeries series, String period, boolean useClose, String provider, String session,
			int sessionEndIndex) {
		super(series);
		this.useClose = useClose;
		this.period = period.toUpperCase();
		this.provider = provider;
		this.session = session;
		this.sessionEndIndex = sessionEndIndex;
		validatePeriod();
	}

	private void validatePeriod() {
		if (!period.matches("\\d+[DWM]")) {
			throw new IllegalArgumentException(
					"Invalid period format. Expected formats like '1D', '2D', '5D', '7D', '1M'");
		}
	}

	@Override
	protected HighestValue calculate(int index) {
		if (index == 0 || getBarSeries().getBarCount() == 0) {
			return new HighestValue(numOf(0), null);
		}

		// Use the session-specific end index instead of the provided index
		int effectiveEndIndex = Math.min(index, sessionEndIndex);

		ZonedDateTime endDate = getBarSeries().getBar(effectiveEndIndex).getEndTime();
		// Convert to Eastern Time if provider is Yahoo
		endDate = endDate.withZoneSameInstant(EASTERN_ZONE);

		ZonedDateTime startDate = calculateStartDate(endDate);
		HighestValue highest = null;

		for (int i = effectiveEndIndex; i >= 0; i--) {
			ZonedDateTime barDate = getBarSeries().getBar(i).getEndTime();
			ZonedDateTime convertedBarDate = barDate;

			convertedBarDate = barDate.withZoneSameInstant(EASTERN_ZONE);

			if (convertedBarDate.isBefore(startDate)) {
				break;
			}

			// Filter by session if not "all"
			if (!"all".equalsIgnoreCase(session) && !isInSession(convertedBarDate, session)) {
				continue;
			}

			Num currentValue = useClose ? getBarSeries().getBar(i).getClosePrice()
					: getBarSeries().getBar(i).getOpenPrice();

			if (highest == null || currentValue.isGreaterThan(highest.value)) {
				// Store original bar date, not the converted one
				highest = new HighestValue(currentValue, convertedBarDate);
			}
		}

		return highest != null ? highest : new HighestValue(numOf(0), null);
	}

	private boolean isInSession(ZonedDateTime barTime, String sessionType) {
		int hour = barTime.getHour();
		int minute = barTime.getMinute();

		switch (sessionType.toLowerCase()) {
		case "premarket":
			// Premarket: 4:00 AM - 9:30 AM EST
			return (hour >= 4 && hour < 9) || (hour == 9 && minute < 30);

		case "standard":
			// Standard: 9:30 AM - 4:00 PM EST
			if (hour == 9 && minute >= 30)
				return true;
			return (hour > 9 && hour < 16) || (hour == 16 && minute == 0);

		case "postmarket":
			// Postmarket: 4:00 PM - 8:00 PM EST
			return (hour >= 16 && hour < 20);

		default:
			return true; // "all" session includes all bars
		}
	}

	private ZonedDateTime calculateStartDate(ZonedDateTime endDate) {
		int periodValue = Integer.parseInt(period.substring(0, period.length() - 1));
		char periodUnit = period.charAt(period.length() - 1);

		ZonedDateTime startDate;
		switch (periodUnit) {
		case 'D':
			startDate = endDate.minusDays(periodValue - 1);
			break;
		case 'W':
			startDate = endDate.minusWeeks(periodValue);
			break;
		case 'M':
			startDate = endDate.minusMonths(periodValue);
			break;
		default:
			throw new IllegalArgumentException("Unsupported period unit: " + periodUnit);
		}

		
		return startDate.toLocalDate().atStartOfDay(EASTERN_ZONE);
	}

	// Helper method to display time in correct timezone
	public ZonedDateTime getDisplayTime(ZonedDateTime originalTime) {
		if (originalTime == null)
			return null;
		return originalTime.withZoneSameInstant(EASTERN_ZONE);

	}
}