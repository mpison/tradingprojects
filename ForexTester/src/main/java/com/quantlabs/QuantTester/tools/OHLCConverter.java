package com.quantlabs.QuantTester.tools;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.time.DayOfWeek;

public class OHLCConverter {

	/*
	 * // Utility method to parse date and time, handles null and invalid input
	 * private static LocalDateTime parseDateTime(String dateTimeStr) { if
	 * (dateTimeStr == null || dateTimeStr.trim().isEmpty()) { return null; // Or
	 * throw an exception, depending on your needs } try { return
	 * LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME); } catch
	 * (DateTimeParseException e) {
	 * System.err.println("Error parsing date/time string: " + dateTimeStr +
	 * ".  Skipping."); return null; // Or throw an exception } }
	 * 
	 * // Converts 1-minute OHLC data to the specified time frame. public static
	 * List<OhlcRecord> convertOHLC(List<OhlcRecord> existingData, OhlcRecord
	 * newOneMinuteData, Duration targetDuration) { if (newOneMinuteData == null ||
	 * targetDuration == null) { return existingData; // Handle null input }
	 * 
	 * if (targetDuration.isNegative() || targetDuration.isZero()) { throw new
	 * IllegalArgumentException("Target duration must be positive."); } if
	 * (targetDuration.toMinutes() < 1) { throw new
	 * IllegalArgumentException("Target duration must be >= 1 minute."); }
	 * 
	 * List<OhlcRecord> workingData = new ArrayList<>(existingData); //copy to not
	 * change original OhlcRecord previousOHLC = workingData.isEmpty() ? null :
	 * workingData.get(workingData.size() - 1);
	 * 
	 * // Skip weekends if (newOneMinuteData.getTimestamp().getDayOfWeek() ==
	 * DayOfWeek.SATURDAY || newOneMinuteData.getTimestamp().getDayOfWeek() ==
	 * DayOfWeek.SUNDAY) { return workingData; }
	 * 
	 * // Handle gaps if (previousOHLC != null) { Duration gap =
	 * Duration.between(previousOHLC.getTimestamp(),
	 * newOneMinuteData.getTimestamp()); if (gap.toMinutes() > 1) { LocalDateTime
	 * current = previousOHLC.getTimestamp().plusMinutes(1); while
	 * (current.isBefore(newOneMinuteData.getTimestamp())) { //check if current is
	 * weekend if (current.getDayOfWeek() != DayOfWeek.SATURDAY &&
	 * current.getDayOfWeek() != DayOfWeek.SUNDAY) { OhlcRecord fillGapOHLC = new
	 * OhlcRecord(current, previousOHLC.getOpen(), previousOHLC.getHigh(),
	 * previousOHLC.getLow(), previousOHLC.getClose(), previousOHLC.getVolume());
	 * workingData = processOHLC(workingData, fillGapOHLC, targetDuration); }
	 * current = current.plusMinutes(1); } } } workingData =
	 * processOHLC(workingData, newOneMinuteData, targetDuration); return
	 * workingData; }
	 * 
	 * private static List<OhlcRecord> processOHLC(List<OhlcRecord> workingData,
	 * OhlcRecord ohlc, Duration targetDuration) { if (workingData.isEmpty()) {
	 * workingData.add(ohlc); return workingData; }
	 * 
	 * OhlcRecord lastAggregated = workingData.get(workingData.size() - 1); Duration
	 * diff = Duration.between(lastAggregated.getTimestamp(), ohlc.getTimestamp());
	 * 
	 * if (diff.toMinutes() < targetDuration.toMinutes()) { // Merge with the last
	 * aggregated OHLC workingData.set(workingData.size() - 1,
	 * OHLC.merge(lastAggregated, ohlc)); } else { // Add a new aggregated OHLC
	 * workingData.add(ohlc); } return workingData; }
	 * 
	 * // Helper method to print OHLC data private static void
	 * printOHLCList(List<OhlcRecord> ohlcList, String timeframe) { if (ohlcList ==
	 * null || ohlcList.isEmpty()) { System.out.println("No " + timeframe +
	 * " OHLC data to display."); return; } System.out.println("\n" + timeframe +
	 * " OHLC Data:"); for (OhlcRecord ohlc : ohlcList) { System.out.println(ohlc);
	 * } }
	 * 
	 * public static void main(String[] args) { // Sample 1-minute OHLC data
	 * (replace with your actual data source) List<OhlcRecord> oneMinuteData = new
	 * ArrayList<>(Arrays.asList( new
	 * OhlcRecord(parseDateTime("2023-10-26T10:00:00"), 100.0, 100.5, 99.5, 100.2,
	 * 1000), // Thursday new OhlcRecord(parseDateTime("2023-10-27T10:01:00"),
	 * 100.2, 100.7, 100.1, 100.6, 1200), // Friday new
	 * OhlcRecord(parseDateTime("2023-10-28T10:02:00"), 100.6, 101.0, 100.5, 100.8,
	 * 1100), // Saturday new OhlcRecord(parseDateTime("2023-10-29T10:03:00"),
	 * 100.8, 101.2, 100.7, 101.1, 1300), // Sunday new
	 * OhlcRecord(parseDateTime("2023-10-30T10:04:00"), 101.1, 101.3, 100.9, 101.2,
	 * 1400), // Monday new OhlcRecord(parseDateTime("2023-10-30T10:05:00"), 101.2,
	 * 101.5, 101.0, 101.4, 1500), new
	 * OhlcRecord(parseDateTime("2023-10-30T10:06:00"), 101.4, 101.6, 101.3, 101.5,
	 * 1600) ));
	 * 
	 * // Initial conversion List<OhlcRecord> fiveMinuteData = convertOHLC(new
	 * ArrayList<>(), oneMinuteData.get(0), Duration.ofMinutes(5)); List<OhlcRecord>
	 * fifteenMinuteData = convertOHLC(new ArrayList<>(), oneMinuteData.get(0),
	 * Duration.ofMinutes(15)); List<OhlcRecord> oneHourData = convertOHLC(new
	 * ArrayList<>(), oneMinuteData.get(0), Duration.ofHours(1)); List<OhlcRecord>
	 * fourHourData = convertOHLC(new ArrayList<>(), oneMinuteData.get(0),
	 * Duration.ofHours(4)); List<OhlcRecord> oneDayData = convertOHLC(new
	 * ArrayList<>(), oneMinuteData.get(0), Duration.ofDays(1)); List<OhlcRecord>
	 * oneWeekData = convertOHLC(new ArrayList<>(), oneMinuteData.get(0),
	 * Duration.ofDays(7)); List<OhlcRecord> oneMonthData = convertOHLC(new
	 * ArrayList<>(), oneMinuteData.get(0), Duration.ofDays(30));
	 * 
	 * printOHLCList(fiveMinuteData, "5-Minute (Initial)");
	 * printOHLCList(fifteenMinuteData, "15-Minute (Initial)");
	 * printOHLCList(oneHourData, "1-Hour (Initial)"); printOHLCList(fourHourData,
	 * "4-Hour (Initial)"); printOHLCList(oneDayData, "1-Day (Initial)");
	 * printOHLCList(oneWeekData, "1-Week (Initial)"); printOHLCList(oneMonthData,
	 * "1-Month (Initial)");
	 * 
	 * // Simulate new 1-minute data entries and recompute efficiently for (int i =
	 * 1; i < oneMinuteData.size(); i++) { OHLC newOneMinuteData =
	 * oneMinuteData.get(i); System.out.println("\nAdding new 1-minute data: " +
	 * newOneMinuteData);
	 * 
	 * fiveMinuteData = convertOHLC(fiveMinuteData, newOneMinuteData,
	 * Duration.ofMinutes(5)); fifteenMinuteData = convertOHLC(fifteenMinuteData,
	 * newOneMinuteData, Duration.ofMinutes(15)); oneHourData =
	 * convertOHLC(oneHourData, newOneMinuteData, Duration.ofHours(1)); fourHourData
	 * = convertOHLC(fourHourData, newOneMinuteData, Duration.ofHours(4));
	 * oneDayData = convertOHLC(oneDayData, newOneMinuteData, Duration.ofDays(1));
	 * oneWeekData = convertOHLC(oneWeekData, newOneMinuteData, Duration.ofDays(7));
	 * oneMonthData = convertOHLC(oneMonthData, newOneMinuteData,
	 * Duration.ofDays(30));
	 * 
	 * printOHLCList(fiveMinuteData, "5-Minute"); printOHLCList(fifteenMinuteData,
	 * "15-Minute"); printOHLCList(oneHourData, "1-Hour");
	 * printOHLCList(fourHourData, "4-Hour"); printOHLCList(oneDayData, "1-Day");
	 * printOHLCList(oneWeekData, "1-Week"); printOHLCList(oneMonthData, "1-Month");
	 * } //Example of a new 1 minute data coming in OhlcRecord newOneMinuteData =
	 * new OhlcRecord(parseDateTime("2023-11-01T10:05:00"), 103.0, 103.5, 102.9,
	 * 103.2, 100); fiveMinuteData = convertOHLC(fiveMinuteData, newOneMinuteData,
	 * Duration.ofMinutes(5)); printOHLCList(fiveMinuteData, "5-Minute"); }
	 */
}

