package com.quantlabs.QuantTester.v3.alert;

import java.time.ZonedDateTime;

public class StockDataResult {
    private String response;
    private ZonedDateTime start;
    private ZonedDateTime end;

    public StockDataResult(String response, ZonedDateTime start, ZonedDateTime end) {
        this.response = response;
        this.start = start;
        this.end = end;
    }

    public String getResponse() { return response; }
    public ZonedDateTime getStart() { return start; }
    public ZonedDateTime getEnd() { return end; }
    
    public boolean isValid() {
        return this.response != null && !this.response.isEmpty();
    }
}