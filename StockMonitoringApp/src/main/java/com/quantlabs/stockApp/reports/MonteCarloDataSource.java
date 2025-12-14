package com.quantlabs.stockApp.reports;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public interface MonteCarloDataSource {
	Map<String, Map<ZonedDateTime, Double[]>> fetchData(List<String> symbols, ZonedDateTime start, ZonedDateTime end, boolean useCustomTimeRange);
    Map<String, Map<ZonedDateTime, Double[]>> fetchData(List<String> symbols, ZonedDateTime start, ZonedDateTime end, String timeframe, boolean useCustomTimeRange2);
    String getName();
    boolean isAvailable();
}