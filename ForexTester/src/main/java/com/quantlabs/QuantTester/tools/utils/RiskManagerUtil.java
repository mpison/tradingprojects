package com.quantlabs.QuantTester.tools.utils;

public class RiskManagerUtil {

    // Calculate used margin
    public static double calculateUsedMargin(double positionSize, int leverage) {
        return positionSize / leverage;
    }

    // Calculate free margin
    public static double calculateFreeMargin(double equity, double usedMargin) {
        return equity - usedMargin;
    }

    // Calculate margin level (%)
    public static double calculateMarginLevel(double equity, double usedMargin) {
        return (equity / usedMargin) * 100;
    }

    // Check if stop-out is triggered
    public static boolean isStopOutTriggered(double marginLevel, double brokerStopOutLevel) {
        return marginLevel <= brokerStopOutLevel;
    }

    // Example usage
    public static void main(String[] args) {
        // Inputs
        double accountBalance = 5000;      // Initial balance
        double openTradeProfit = -200;     // Current P&L of open trades
        double positionSize = 100000;      // 1 standard lot (100k units)
        int leverage = 50;                 // 50:1 leverage
        double brokerStopOutLevel = 50;    // Broker stops out at 50% margin

        // Calculations
        double equity = accountBalance + openTradeProfit;
        double usedMargin = calculateUsedMargin(positionSize, leverage);
        double freeMargin = calculateFreeMargin(equity, usedMargin);
        double marginLevel = calculateMarginLevel(equity, usedMargin);
        boolean stopOutTriggered = isStopOutTriggered(marginLevel, brokerStopOutLevel);

        // Output
        System.out.printf("Equity: $%.2f%n", equity);
        System.out.printf("Used Margin: $%.2f%n", usedMargin);
        System.out.printf("Free Margin: $%.2f%n", freeMargin);
        System.out.printf("Margin Level: %.2f%%%n", marginLevel);
        System.out.printf("Stop-Out Triggered: %s%n", stopOutTriggered ? "YES" : "NO");
    }
}