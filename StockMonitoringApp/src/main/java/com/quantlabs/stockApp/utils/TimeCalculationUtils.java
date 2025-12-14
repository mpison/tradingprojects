package com.quantlabs.stockApp.utils;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.jdesktop.swingx.JXDatePicker;

public class TimeCalculationUtils {
	public static ZonedDateTime calculateStartTime(String timeframe, ZonedDateTime end) {
		switch (timeframe) {
		case "1Min":
			return end.minusDays(7);
		case "5Min":
			return end.minusDays(7);
		case "15Min":
			return end.minusDays(7);
		case "30Min":
			return end.minusDays(57);
		case "1H":
			return end.minusDays(120);
		case "4H":
			return end.minusDays(500);
		case "1D":
			return end.minusDays(730);
		case "1W":
			return end.minusDays(3000);
		default:
			System.out.println("calculateStartTime = " + timeframe + " end: "+end);
			return end.minusDays(7);
		}
	}
	
	public static ZonedDateTime calculateStartTime(String timeframe, ZonedDateTime startTime, ZonedDateTime end) {
		switch (timeframe) {
		case "1Min":
			if(end.minusDays(7).isBefore(startTime)) {
				return startTime;
			}else {
				return end.minusDays(7);
			}
		case "5Min":
			if(end.minusDays(7).isBefore(startTime)) {
				return startTime;
			}else {
				return end.minusDays(7);
			}
		case "15Min":
			if(end.minusDays(7).isBefore(startTime)) {
				return startTime;
			}else {
				return end.minusDays(7);
			}
		case "30Min":
			if(end.minusDays(57).isBefore(startTime)) {
				return startTime;
			}else {
				return end.minusDays(57);
			}
		case "1H":
			if(end.minusDays(120).isBefore(startTime)) {
				return startTime;
			}else {
				return end.minusDays(120);
			}
		case "4H":
			if(end.minusDays(500).isBefore(startTime)) {
				return startTime;
			}else {
				return end.minusDays(500);
			}
		case "1D":
			if(end.minusDays(730).isBefore(startTime)) {
				return startTime;
			}else {
				return end.minusDays(730);
			}
		case "1W":
			if(end.minusDays(3000).isBefore(startTime)) {
				return startTime;
			}else {
				return end.minusDays(3000);
			}
		default:
			return end.minusDays(7);
		}
	}

	/**
	 * Calculates custom start time based on date picker selection
	 * 
	 * @param end             The end time to use if date picker is invalid
	 * @param useCustomRange  Flag indicating if custom range should be used
	 * @param startDatePicker The date picker component containing the start date
	 * @return Calculated start time
	 */
	public static ZonedDateTime calculateCustomStartTime(ZonedDateTime end, boolean useCustomRange,
			Object startDatePicker) {
		if (!useCustomRange) {
			return end.minusDays(1); // Default fallback
		}

		// Handle case where date picker isn't provided
		if (startDatePicker == null) {
			return end.minusDays(1);
		}

		// Handle different date picker implementations
		Date selectedDate = null;
		try {
			if (startDatePicker instanceof JXDatePicker) {
				selectedDate = ((JXDatePicker) startDatePicker).getDate();
			} else if (startDatePicker instanceof Date) {
				selectedDate = (Date) startDatePicker;
			}
		} catch (ClassCastException e) {
			// Log error and fallback
			System.err.println("Invalid date picker type: " + startDatePicker.getClass());
			return end.minusDays(1);
		}

		// Handle null or invalid date selection
		if (selectedDate == null) {
			return end.minusDays(1);
		}

		// Convert to ZonedDateTime using system default time zone
		ZonedDateTime start = selectedDate.toInstant().atZone(ZoneId.systemDefault())
				.withZoneSameInstant(ZoneOffset.UTC);

		// Validate that start is before end
		if (start.isAfter(end)) {
			// If start is after end, return 1 day before end
			return end.minusDays(1);
		}

		// Ensure minimum time difference (e.g., at least 1 minute)
		if (start.plusMinutes(1).isAfter(end)) {
			return end.minusMinutes(1);
		}

		return start;
	}

	/**
	 * Overloaded version with additional validation parameters
	 * 
	 * @param end             The end time reference
	 * @param useCustomRange  Whether to use custom range
	 * @param startDatePicker The date picker component
	 * @param minDuration     Minimum allowed duration between start and end
	 * @param maxDuration     Maximum allowed duration between start and end
	 * @return Calculated start time with duration constraints
	 */
	public static ZonedDateTime calculateCustomStartTime(ZonedDateTime end, boolean useCustomRange,
			Object startDatePicker, Duration minDuration, Duration maxDuration) {
		ZonedDateTime start = calculateCustomStartTime(end, useCustomRange, startDatePicker);

		// Apply minimum duration constraint
		if (minDuration != null) {
			ZonedDateTime minStart = end.minus(minDuration);
			if (start.isAfter(minStart)) {
				start = minStart;
			}
		}

		// Apply maximum duration constraint
		if (maxDuration != null) {
			ZonedDateTime maxStart = end.minus(maxDuration);
			if (start.isBefore(maxStart)) {
				start = maxStart;
			}
		}

		return start;
	}

	/**
	 * Helper method to validate date range
	 */
	public static boolean isValidDateRange(ZonedDateTime start, ZonedDateTime end) {
		return start != null && end != null && start.isBefore(end);
	}

	/**
	 * Helper method to calculate duration between two dates
	 */
	public static Duration calculateDuration(ZonedDateTime start, ZonedDateTime end) {
		if (!isValidDateRange(start, end)) {
			return Duration.ZERO;
		}
		return Duration.between(start, end);
	}

	
}