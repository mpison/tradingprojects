package com.quantlabs.stockApp.reports;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public interface MonteCarloDataSource {
	Map<String, Map<ZonedDateTime, Double[]>> fetchData(List<String> symbols, ZonedDateTime start, ZonedDateTime end);
    Map<String, Map<ZonedDateTime, Double[]>> fetchData(List<String> symbols, ZonedDateTime start, ZonedDateTime end, String timeframe);
    String getName();
    boolean isAvailable();
}