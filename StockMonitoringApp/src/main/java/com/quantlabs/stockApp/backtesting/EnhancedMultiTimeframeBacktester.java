package com.quantlabs.stockApp.backtesting;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.indicator.strategy.AbstractIndicatorStrategy;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class EnhancedMultiTimeframeBacktester {
    private final ConsoleLogger logger;
    private final MultiTimeframeDataManager dataManager;
    private final Map<String, BarSeries> timeframeSeries;
    private final List<TimeframeStrategyConfig> strategyConfigs;
    private final Map<String, PriceData> priceDataMap;
    
    public EnhancedMultiTimeframeBacktester(ConsoleLogger logger, 
                                          MultiTimeframeDataManager dataManager,
                                          Map<String, PriceData> priceDataMap) {
        this.logger = logger;
        this.dataManager = dataManager;
        this.timeframeSeries = new HashMap<>();
        this.strategyConfigs = new ArrayList<>();
        this.priceDataMap = priceDataMap;
    }
    
    public void addTimeframeData(String symbol, List<String> timeframes, 
                               LocalDateTime start, LocalDateTime end) {
        Map<String, BarSeries> data = dataManager.loadMultiTimeframeData(symbol, timeframes, start, end);
        timeframeSeries.putAll(data);
        dataManager.updatePriceDataMap(symbol, data, priceDataMap);
    }
    
    public void addStrategy(String timeframe, AbstractIndicatorStrategy strategy, double weight) {
        strategyConfigs.add(new TimeframeStrategyConfig(timeframe, strategy, weight));
    }
    
    public BacktestResult runBacktest(String symbol, double initialCapital, 
                                    LocalDateTime startDate, LocalDateTime endDate) {
        List<Trade> allTrades = new ArrayList<>();
        double capital = initialCapital;
        int position = 0;
        double entryPrice = 0;
        ZonedDateTime entryTime = null;
        
        BarSeries primarySeries = timeframeSeries.get("4H");
        if (primarySeries == null) {
            primarySeries = timeframeSeries.values().iterator().next();
            if (primarySeries == null) {
                throw new IllegalStateException("No timeframe data available");
            }
        }
        
        BarSeries filteredSeries = filterSeriesByDate(primarySeries, startDate, endDate);
        
        for (int i = 0; i < filteredSeries.getBarCount(); i++) {
            Map<String, AnalysisResult> currentAnalysis = analyzeAllTimeframes(i);
            TradingSignal signal = getCompositeSignal(currentAnalysis, i);
            
            if (position == 0 && signal == TradingSignal.BUY) {
                double currentPrice = filteredSeries.getBar(i).getClosePrice().doubleValue();
                position = calculatePositionSize(capital, currentPrice);
                if (position > 0) {
                    entryPrice = currentPrice;
                    entryTime = filteredSeries.getBar(i).getEndTime();
                    capital -= position * entryPrice;
                    logger.log("ENTER " + symbol + " at " + entryPrice + ", shares: " + position);
                }
            } else if (position > 0 && signal == TradingSignal.SELL) {
                double exitPrice = filteredSeries.getBar(i).getClosePrice().doubleValue();
                ZonedDateTime exitTime = filteredSeries.getBar(i).getEndTime();
                Trade trade = new Trade(entryTime, exitTime, entryPrice, exitPrice, position, symbol);
                allTrades.add(trade);
                capital += position * exitPrice;
                double pnl = (exitPrice - entryPrice) * position;
                logger.log("EXIT " + symbol + " at " + exitPrice + ", PnL: " + pnl);
                position = 0;
            }
        }
        
        if (position > 0) {
            double exitPrice = filteredSeries.getBar(filteredSeries.getBarCount() - 1).getClosePrice().doubleValue();
            ZonedDateTime exitTime = filteredSeries.getBar(filteredSeries.getBarCount() - 1).getEndTime();
            Trade trade = new Trade(entryTime, exitTime, entryPrice, exitPrice, position, symbol);
            allTrades.add(trade);
            capital += position * exitPrice;
        }
        
        return new BacktestResult(allTrades, initialCapital, capital, filteredSeries);
    }
    
    private int calculatePositionSize(double capital, double price) {
        double maxTradeValue = capital * 0.1;
        int shares = (int) (maxTradeValue / price);
        return Math.max(shares, 1);
    }
    
    private Map<String, AnalysisResult> analyzeAllTimeframes(int primaryIndex) {
        Map<String, AnalysisResult> analysisResults = new HashMap<>();
        
        for (String timeframe : timeframeSeries.keySet()) {
            BarSeries series = timeframeSeries.get(timeframe);
            int equivalentIndex = calculateEquivalentIndex(primaryIndex, timeframe, series);
            
            if (equivalentIndex >= 0 && equivalentIndex < series.getBarCount()) {
                AnalysisResult result = new AnalysisResult();
                result.setPrice(series.getBar(equivalentIndex).getClosePrice().doubleValue());
                result.setBarSeries(series);
                
                for (TimeframeStrategyConfig config : strategyConfigs) {
                    if (config.timeframe.equals(timeframe)) {
                        try {
                            config.strategy.setSymbol(series.getName().replace("_CONSOLIDATED", ""));
                            config.strategy.analyzeSymbol(series, equivalentIndex, equivalentIndex, result);
                        } catch (Exception e) {
                            logger.log("Error executing strategy " + config.strategy.getName() + ": " + e.getMessage());
                        }
                    }
                }
                analysisResults.put(timeframe, result);
            }
        }
        return analysisResults;
    }
    
    private TradingSignal getCompositeSignal(Map<String, AnalysisResult> analysisResults, int index) {
        Map<String, Double> timeframeWeights = getTimeframeWeights();
        double totalScore = 0;
        double totalWeight = 0;
        
        for (TimeframeStrategyConfig config : strategyConfigs) {
            AnalysisResult result = analysisResults.get(config.timeframe);
            if (result != null) {
                double strategyScore = calculateStrategyScore(config.strategy, result);
                double timeframeWeight = timeframeWeights.getOrDefault(config.timeframe, 1.0);
                double combinedWeight = config.weight * timeframeWeight;
                
                totalScore += strategyScore * combinedWeight;
                totalWeight += combinedWeight;
            }
        }
        
        double normalizedScore = totalWeight > 0 ? totalScore / totalWeight : 0;
        
        if (normalizedScore > 0.3) {
            return TradingSignal.BUY;
        } else if (normalizedScore < -0.3) {
            return TradingSignal.SELL;
        }
        return TradingSignal.HOLD;
    }
    
    private Map<String, Double> getTimeframeWeights() {
        return Map.of(
            "1D", 1.0, "4H", 0.9, "1H", 0.7, 
            "30Min", 0.5, "15Min", 0.3, "5Min", 0.2, "1Min", 0.1
        );
    }
    
    private double calculateStrategyScore(AbstractIndicatorStrategy strategy, AnalysisResult result) {
        String trend = strategy.determineTrend(result);
        
        if (trend.contains("Bullish") || trend.contains("Uptrend") || trend.contains("Buy")) {
            return 1.0;
        } else if (trend.contains("Bearish") || trend.contains("Downtrend") || trend.contains("Sell")) {
            return -1.0;
        } else if (trend.contains("Overbought")) {
            return -0.5;
        } else if (trend.contains("Oversold")) {
            return 0.5;
        }
        
        switch (strategy.getName()) {
            case "RSI":
                return calculateRSIScore(result);
            case "MACD":
                return calculateMACDScore(result);
            case "PSAR(0.01)":
                return calculatePSARScore(result, 0.01);
            case "PSAR(0.05)":
                return calculatePSARScore(result, 0.05);
            case "HeikenAshi":
                return calculateHeikenAshiScore(result);
            case "VWAP":
                return calculateVWAPScore(result);
            case "Volume20MA":
                return calculateVolumeScore(result);
            default:
                return 0;
        }
    }
    
    private double calculateRSIScore(AnalysisResult result) {
        double rsi = result.getRsi();
        if (Double.isNaN(rsi)) return 0;
        if (rsi < 30) return 1.0;
        if (rsi < 40) return 0.5;
        if (rsi > 70) return -1.0;
        if (rsi > 60) return -0.5;
        return 0;
    }
    
    private double calculateMACDScore(AnalysisResult result) {
        double macd = result.getMacd();
        double signal = result.getMacdSignal();
        if (Double.isNaN(macd) || Double.isNaN(signal)) return 0;
        double histogram = macd - signal;
        if (macd > signal && histogram > 0) return 1.0;
        if (macd < signal && histogram < 0) return -1.0;
        if (macd > 0 && signal > 0) return 0.5;
        if (macd < 0 && signal < 0) return -0.5;
        return 0;
    }
    
    private double calculatePSARScore(AnalysisResult result, double step) {
        double psarValue = step == 0.01 ? result.getPsar001() : result.getPsar005();
        double price = result.getPrice();
        if (Double.isNaN(psarValue)) return 0;
        return price > psarValue ? 1.0 : -1.0;
    }
    
    private double calculateHeikenAshiScore(AnalysisResult result) {
        double haClose = result.getHeikenAshiClose();
        double haOpen = result.getHeikenAshiOpen();
        if (Double.isNaN(haClose) || Double.isNaN(haOpen)) return 0;
        return haClose > haOpen ? 1.0 : -1.0;
    }
    
    private double calculateVWAPScore(AnalysisResult result) {
        double vwap = result.getVwap();
        double price = result.getPrice();
        if (Double.isNaN(vwap)) return 0;
        return price > vwap ? 0.7 : -0.7;
    }
    
    private double calculateVolumeScore(AnalysisResult result) {
        double volume = result.getVolume();
        double volumeMA = result.getVolume20MA();
        if (Double.isNaN(volume) || Double.isNaN(volumeMA)) return 0;
        return volume > volumeMA ? 0.3 : -0.1;
    }
    
    private int calculateEquivalentIndex(int primaryIndex, String timeframe, BarSeries targetSeries) {
        BarSeries primarySeries = timeframeSeries.get("4H");
        if (primarySeries == null) {
            primarySeries = timeframeSeries.values().iterator().next();
        }
        if (primaryIndex >= primarySeries.getBarCount()) {
            return targetSeries.getBarCount() - 1;
        }
        ZonedDateTime primaryTime = primarySeries.getBar(primaryIndex).getEndTime();
        for (int i = Math.min(primaryIndex, targetSeries.getBarCount() - 1); i >= 0; i--) {
            ZonedDateTime targetTime = targetSeries.getBar(i).getEndTime();
            if (!targetTime.isAfter(primaryTime)) {
                return i;
            }
        }
        return 0;
    }
    
    private BarSeries filterSeriesByDate(BarSeries series, LocalDateTime start, LocalDateTime end) {
        BaseBarSeries filtered = new BaseBarSeries(series.getName());
        ZonedDateTime startZdt = start.atZone(ZoneId.systemDefault());
        ZonedDateTime endZdt = end.atZone(ZoneId.systemDefault());
        
        for (int i = 0; i < series.getBarCount(); i++) {
            Bar bar = series.getBar(i);
            if (!bar.getEndTime().isBefore(startZdt) && !bar.getEndTime().isAfter(endZdt)) {
                filtered.addBar(bar.getEndTime(), 
                    bar.getOpenPrice(), bar.getHighPrice(), bar.getLowPrice(), 
                    bar.getClosePrice(), bar.getVolume());
            }
        }
        return filtered;
    }
    
    private static class TimeframeStrategyConfig {
        String timeframe;
        AbstractIndicatorStrategy strategy;
        double weight;
        
        TimeframeStrategyConfig(String timeframe, AbstractIndicatorStrategy strategy, double weight) {
            this.timeframe = timeframe;
            this.strategy = strategy;
            this.weight = weight;
        }
    }
    
    private enum TradingSignal {
        BUY, SELL, HOLD
    }
}