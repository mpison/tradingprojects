package com.quantlabs.stockApp.reports;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.quantlabs.stockApp.data.StockDataProviderFactory;
import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.data.StockDataProvider;

import okhttp3.OkHttpClient;

public class MonteCarloDataSourceManager {
    private final Map<String, MonteCarloDataSource> dataSources;
    private MonteCarloDataSource currentDataSource;
    private final StockDataProviderFactory providerFactory;
    private final ConsoleLogger logger;
    
    public MonteCarloDataSourceManager(StockDataProviderFactory providerFactory, ConsoleLogger logger) {
        this.providerFactory = providerFactory;
        this.logger = logger;
        this.dataSources = new HashMap<>();
        initializeDataSources();
    }
    
    private void initializeDataSources() {
        // Initialize with default configuration
        java.util.Map<String, Object> defaultConfig = new HashMap<>();
        
        try {
            // Try to initialize Alpaca first
            StockDataProvider alpacaProvider = providerFactory.getProvider("Alpaca", logger, defaultConfig);
            MonteCarloDataSource alpacaDataSource = new StockProviderMonteCarloDataSource(alpacaProvider, "Alpaca", logger);
            dataSources.put("Alpaca", alpacaDataSource);
        } catch (Exception e) {
            System.out.println("Alpaca data source not available: " + e.getMessage());
        }
        
        try {
            // Initialize Yahoo as fallback
            StockDataProvider yahooProvider = providerFactory.getProvider("Yahoo", logger);
            MonteCarloDataSource yahooDataSource = new StockProviderMonteCarloDataSource(yahooProvider, "Yahoo Finance", logger);
            dataSources.put("Yahoo Finance", yahooDataSource);
        } catch (Exception e) {
            System.out.println("Yahoo data source not available: " + e.getMessage());
        }
        
        try {
            // Initialize Polygon if available
            StockDataProvider polygonProvider = providerFactory.getProvider("Polygon", logger);
            MonteCarloDataSource polygonDataSource = new StockProviderMonteCarloDataSource(polygonProvider, "Polygon", logger);
            dataSources.put("Polygon", polygonDataSource);
        } catch (Exception e) {
            System.out.println("Polygon data source not available: " + e.getMessage());
        }
        
        // Set default data source (Yahoo as most reliable fallback)
        if (dataSources.containsKey("Alpaca")) {
            this.currentDataSource = dataSources.get("Alpaca");
        } else if (!dataSources.isEmpty()) {
            this.currentDataSource = dataSources.values().iterator().next();
        } else {
            throw new IllegalStateException("No data sources available");
        }
    }
    
    public void setDataSource(String sourceName) {
        MonteCarloDataSource source = dataSources.get(sourceName);
        if (source != null && source.isAvailable()) {
            this.currentDataSource = source;
        } else {
            throw new IllegalArgumentException("Data source not available: " + sourceName);
        }
    }
    
    public MonteCarloDataSource getCurrentDataSource() {
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
    
    public Map<String, Map<ZonedDateTime, Double[]>> fetchData(List<String> symbols, ZonedDateTime start, ZonedDateTime end, String timeframe) {
        return currentDataSource.fetchData(symbols, start, end, timeframe);
    }
    
    public String getCurrentDataSourceName() {
        return currentDataSource.getName();
    }
    
    // Method to update provider configuration
    public void updateProviderConfiguration(String providerName, java.util.Map<String, Object> config) {
        try {
            StockDataProvider provider = providerFactory.getProvider(providerName, logger, config);
            MonteCarloDataSource dataSource = new StockProviderMonteCarloDataSource(provider, providerName, logger);
            dataSources.put(providerName, dataSource);
            
            // If this is the current data source, update it
            if (currentDataSource.getName().equals(providerName)) {
                this.currentDataSource = dataSource;
            }
        } catch (Exception e) {
            System.err.println("Failed to update provider configuration: " + e.getMessage());
        }
    }
}