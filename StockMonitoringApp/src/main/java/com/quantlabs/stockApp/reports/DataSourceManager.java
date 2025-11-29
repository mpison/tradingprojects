package com.quantlabs.stockApp.reports;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class DataSourceManager {
    private final Map<String, DataSource> dataSources;
    private DataSource currentDataSource;
    
    public DataSourceManager() {
        this.dataSources = new HashMap<>();
        initializeDataSources();
    }
    
    private void initializeDataSources() {
        // Register available data sources
        DataSource yahooDataSource = new YahooDataSource();
        DataSource tradingViewDataSource = new TradingViewDataSource();
        
        dataSources.put(yahooDataSource.getName(), yahooDataSource);
        dataSources.put(tradingViewDataSource.getName(), tradingViewDataSource);
        
        // Set default data source
        this.currentDataSource = yahooDataSource;
    }
    
    public void setDataSource(String sourceName) {
        DataSource source = dataSources.get(sourceName);
        if (source != null && source.isAvailable()) {
            this.currentDataSource = source;
        } else {
            throw new IllegalArgumentException("Data source not available: " + sourceName);
        }
    }
    
    public DataSource getCurrentDataSource() {
        return currentDataSource;
    }
    
    public List<String> getAvailableDataSources() {
        return dataSources.keySet().stream()
                .filter(sourceName -> dataSources.get(sourceName).isAvailable())
                .collect(java.util.stream.Collectors.toList());
    }
    
    public Map<String, Map<ZonedDateTime, Double[]>> fetchData(List<String> symbols, ZonedDateTime start, ZonedDateTime end) {
        return currentDataSource.fetchData(symbols, start, end);
    }
    
    public String getCurrentDataSourceName() {
        return currentDataSource.getName();
    }
}