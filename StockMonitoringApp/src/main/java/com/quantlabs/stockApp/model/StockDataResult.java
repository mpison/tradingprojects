package com.quantlabs.stockApp.model;

import java.time.ZonedDateTime;

/**
 * Represents the raw stock data response from an API along with the requested time range
 */
public class StockDataResult {
    public final String jsonResponse;
    public final ZonedDateTime start;
    public final ZonedDateTime end;

    public StockDataResult(String jsonResponse, ZonedDateTime start, ZonedDateTime end) {
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            throw new IllegalArgumentException("JSON response cannot be null or empty");
        }
        if (start == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        if (end == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        
        this.jsonResponse = jsonResponse;
        this.start = start;
        this.end = end;
    }

    // Getters
    public String getJsonResponse() {
        return jsonResponse;
    }

    public ZonedDateTime getStart() {
        return start;
    }

    public ZonedDateTime getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return String.format("StockDataResult{start=%s, end=%s, jsonResponseLength=%d}",
                start, end, jsonResponse.length());
    }
}