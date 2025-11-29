package com.quantlabs.stockApp.backtesting;

import java.time.ZonedDateTime;

public class Trade {
    private final ZonedDateTime entryTime;
    private final ZonedDateTime exitTime;
    private final double entryPrice;
    private final double exitPrice;
    private final int quantity;
    private final String symbol;
    private final double pnl;
    
    public Trade(ZonedDateTime entryTime, ZonedDateTime exitTime, double entryPrice, 
                 double exitPrice, int quantity, String symbol) {
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.entryPrice = entryPrice;
        this.exitPrice = exitPrice;
        this.quantity = quantity;
        this.symbol = symbol;
        this.pnl = (exitPrice - entryPrice) * quantity;
    }
    
    // Getters
    public ZonedDateTime getEntryTime() { return entryTime; }
    public ZonedDateTime getExitTime() { return exitTime; }
    public double getEntryPrice() { return entryPrice; }
    public double getExitPrice() { return exitPrice; }
    public int getQuantity() { return quantity; }
    public String getSymbol() { return symbol; }
    public double getPnl() { return pnl; }
}