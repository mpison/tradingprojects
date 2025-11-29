package com.quantlabs.stockApp.reports;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.List;

public interface DataSource {
    Map<String, Map<ZonedDateTime, Double[]>> fetchData(List<String> symbols, ZonedDateTime start, ZonedDateTime end);
    String getName();
    boolean isAvailable();
}