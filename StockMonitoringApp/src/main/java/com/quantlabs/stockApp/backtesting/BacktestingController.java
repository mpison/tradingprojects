package com.quantlabs.stockApp.backtesting;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.data.StockDataProvider;
import com.quantlabs.stockApp.data.StockDataProviderFactory;
import com.quantlabs.stockApp.indicator.strategy.AdvancedVWAPIndicatorStrategy;
import com.quantlabs.stockApp.indicator.strategy.HeikenAshiStrategy;
import com.quantlabs.stockApp.indicator.strategy.AbstractIndicatorStrategy;
import com.quantlabs.stockApp.indicator.strategy.MACDStrategy;
import com.quantlabs.stockApp.indicator.strategy.MovingAverageStrategy;
import com.quantlabs.stockApp.indicator.strategy.PSARStrategy;
import com.quantlabs.stockApp.indicator.strategy.RSIStrategy;
import com.quantlabs.stockApp.indicator.strategy.VolumeStrategyMA;
import com.quantlabs.stockApp.model.PriceData;

public class BacktestingController {
    private final ConsoleLogger logger;
    private final StockDataProviderFactory providerFactory;
    private final Map<String, PriceData> priceDataMap;
    private EnhancedMultiTimeframeBacktester backtester;
    private MultiTimeframeDataManager dataManager;
    
    public BacktestingController(ConsoleLogger logger, StockDataProviderFactory providerFactory) {
        this.logger = logger;
        this.providerFactory = providerFactory;
        this.priceDataMap = new ConcurrentHashMap<>();
    }
    
    public void initializeBacktester(String dataProvider, Map<String, Object> providerConfig) {
        StockDataProvider stockDataProvider = providerFactory.getProvider(dataProvider, logger, providerConfig);
        this.dataManager = new MultiTimeframeDataManager(stockDataProvider, logger);
        this.backtester = new EnhancedMultiTimeframeBacktester(logger, dataManager, priceDataMap);
    }
    
    public BacktestResult runMultiTimeframeBacktest(BacktestConfig config) {
        if (backtester == null) {
            throw new IllegalStateException("Backtester not initialized. Call initializeBacktester first.");
        }
        
        logger.log("Starting multi-timeframe backtest for " + config.getSymbol());
        
        // Load data for all timeframes
        backtester.addTimeframeData(config.getSymbol(), config.getTimeframes(), 
                                  config.getStartDate(), config.getEndDate());
        
        // Configure strategies
        setupStrategies(config.getStrategies());
        
        // Run backtest
        return backtester.runBacktest(config.getSymbol(), config.getInitialCapital(),
                                    config.getStartDate(), config.getEndDate());
    }
    
    private void setupStrategies(List<StrategyConfig> strategyConfigs) {
        for (StrategyConfig config : strategyConfigs) {
            for (String timeframe : config.getTimeframes()) {
                AbstractIndicatorStrategy strategy = createStrategy(config);
                backtester.addStrategy(timeframe, strategy, config.getWeight());
            }
        }
    }
    
    private AbstractIndicatorStrategy createStrategy(StrategyConfig config) {
        switch (config.getType()) {
            case "RSI":
                return new RSIStrategy(logger);
            case "MACD":
                return new MACDStrategy(12, 26, 9, "MACD", logger);
            case "MACD_359":
                return new MACDStrategy(5, 8, 9, "MACD(5,8,9)", logger);
            case "PSAR_001":
                return new PSARStrategy(0.01, 0.02, "PSAR(0.01)", logger);
            case "PSAR_005":
                return new PSARStrategy(0.05, 0.02, "PSAR(0.05)", logger);
            case "HEIKEN_ASHI":
                return new HeikenAshiStrategy(logger);
            case "VWAP":
                return new AdvancedVWAPIndicatorStrategy(logger);
            case "VOLUME_MA":
                return new VolumeStrategyMA(20, logger);
            case "MOVING_AVERAGE":
                Map<String, Object> params = config.getParameters();
                int period = (int) params.getOrDefault("period", 20);
                String type = (String) params.getOrDefault("type", "SMA");
                return new MovingAverageStrategy(period, type, type + "(" + period + ")", logger);
            default:
                throw new IllegalArgumentException("Unknown strategy type: " + config.getType());
        }
    }
    
    public Map<String, PriceData> getPriceDataMap() {
        return priceDataMap;
    }
    
    public void cleanup() {
        if (dataManager != null) {
            dataManager.shutdown();
        }
    }
}