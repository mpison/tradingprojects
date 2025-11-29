package com.quantlabs.stockApp.backtesting;

import org.ta4j.core.BarSeries;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class BacktestResult {
    private final List<Trade> trades;
    private final double initialCapital;
    private final double finalCapital;
    private final double totalReturn;
    private final double totalPnL;
    private final BarSeries series;
    private final double sharpeRatio;
    private final double maxDrawdown;
    private final double winRate;
    private final double profitFactor;
    private final double averageTrade;
    private final int totalTrades;
    
    public BacktestResult(List<Trade> trades, double initialCapital, double finalCapital, BarSeries series) {
        this.trades = trades;
        this.initialCapital = initialCapital;
        this.finalCapital = finalCapital;
        this.series = series;
        this.totalPnL = finalCapital - initialCapital;
        this.totalReturn = (finalCapital - initialCapital) / initialCapital * 100;
        this.totalTrades = trades.size();
        
        // Calculate metrics
        this.winRate = calculateWinRate();
        this.averageTrade = calculateAverageTrade();
        this.profitFactor = calculateProfitFactor();
        this.maxDrawdown = calculateMaxDrawdown();
        this.sharpeRatio = calculateSharpeRatio();
    }
    
    private double calculateWinRate() {
        if (trades.isEmpty()) return 0;
        long winningTrades = trades.stream().filter(t -> t.getPnl() > 0).count();
        return (double) winningTrades / trades.size() * 100;
    }
    
    private double calculateAverageTrade() {
        return trades.isEmpty() ? 0 : totalPnL / trades.size();
    }
    
    private double calculateProfitFactor() {
        double grossProfit = trades.stream().filter(t -> t.getPnl() > 0)
                                  .mapToDouble(Trade::getPnl).sum();
        double grossLoss = Math.abs(trades.stream().filter(t -> t.getPnl() < 0)
                                        .mapToDouble(Trade::getPnl).sum());
        return grossLoss > 0 ? grossProfit / grossLoss : grossProfit > 0 ? Double.POSITIVE_INFINITY : 0;
    }
    
    private double calculateMaxDrawdown() {
        if (trades.isEmpty()) return 0;
        // Simplified calculation - you can implement more sophisticated version
        return Math.abs(trades.stream().mapToDouble(Trade::getPnl).min().orElse(0));
    }
    
    private double calculateSharpeRatio() {
        if (trades.isEmpty()) return 0;
        // Simplified Sharpe ratio calculation
        double avgReturn = averageTrade;
        double stdDev = Math.sqrt(trades.stream()
            .mapToDouble(t -> Math.pow(t.getPnl() - avgReturn, 2))
            .average().orElse(0));
        return stdDev > 0 ? avgReturn / stdDev * Math.sqrt(252) : 0;
    }
    
    // Getters
    public List<Trade> getTrades() { return trades; }
    public double getTotalReturn() { return totalReturn; }
    public double getTotalPnL() { return totalPnL; }
    public double getInitialCapital() { return initialCapital; }
    public double getFinalCapital() { return finalCapital; }
    public int getTotalTrades() { return totalTrades; }
    public double getWinRate() { return winRate; }
    public double getSharpeRatio() { return sharpeRatio; }
    public double getMaxDrawdown() { return maxDrawdown; }
    public double getProfitFactor() { return profitFactor; }
    public double getAverageTrade() { return averageTrade; }
    
    public Map<String, Double> getPerformanceMetrics() {
        return Map.of(
            "Total Return (%)", totalReturn,
            "Total PnL", totalPnL,
            "Total Trades", (double) totalTrades,
            "Win Rate (%)", winRate,
            "Average Trade", averageTrade,
            "Profit Factor", profitFactor,
            "Sharpe Ratio", sharpeRatio,
            "Max Drawdown (%)", maxDrawdown
        );
    }
}