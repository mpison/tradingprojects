package com.quantlabs.stockApp.reports;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class YahooDataSource implements DataSource {
    private final MonteCarloSimulator simulator;

    public YahooDataSource() {
        this.simulator = new MonteCarloSimulator();
    }

    @Override
    public Map<String, Map<ZonedDateTime, Double[]>> fetchData(List<String> symbols, ZonedDateTime start, ZonedDateTime end) {
        return simulator.computeData(symbols, start, end);
    }

    @Override
    public String getName() {
        return "Yahoo Finance";
    }

    @Override
    public boolean isAvailable() {
        return true; // Yahoo is always available as fallback
    }
}