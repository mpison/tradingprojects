package com.quantlabs.stockApp.backtesting;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BacktestConfig {
    private String symbol;
    private List<String> timeframes;
    private double initialCapital;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<StrategyConfig> strategies;
    
    public BacktestConfig() {
        this.timeframes = new ArrayList<>();
        this.strategies = new ArrayList<>();
    }
    
    public static class Builder {
        private final BacktestConfig config;
        
        public Builder() {
            config = new BacktestConfig();
        }
        
        public Builder symbol(String symbol) {
            config.symbol = symbol;
            return this;
        }
        
        public Builder timeframe(String timeframe) {
            config.timeframes.add(timeframe);
            return this;
        }
        
        public Builder timeframes(List<String> timeframes) {
            config.timeframes = timeframes;
            return this;
        }
        
        public Builder initialCapital(double capital) {
            config.initialCapital = capital;
            return this;
        }
        
        public Builder dateRange(LocalDateTime start, LocalDateTime end) {
            config.startDate = start;
            config.endDate = end;
            return this;
        }
        
        public Builder strategy(StrategyConfig strategy) {
            config.strategies.add(strategy);
            return this;
        }
        
        public BacktestConfig build() {
            if (config.timeframes.isEmpty()) {
                config.timeframes = List.of("1D", "4H", "1H", "30Min", "15Min");
            }
            if (config.startDate == null) {
                config.startDate = LocalDateTime.now().minusMonths(6);
            }
            if (config.endDate == null) {
                config.endDate = LocalDateTime.now();
            }
            if (config.initialCapital == 0) {
                config.initialCapital = 10000;
            }
            return config;
        }
    }
    
    // Getters
    public String getSymbol() { return symbol; }
    public List<String> getTimeframes() { return timeframes; }
    public double getInitialCapital() { return initialCapital; }
    public LocalDateTime getStartDate() { return startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public List<StrategyConfig> getStrategies() { return strategies; }
}