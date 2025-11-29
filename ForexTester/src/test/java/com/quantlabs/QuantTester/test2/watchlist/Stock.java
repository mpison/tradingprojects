package com.quantlabs.QuantTester.test2.watchlist;

public class Stock {
    private String symbol;
    private String name;
    
    public Stock(String symbol, String name) {
    	if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock symbol cannot be empty");
        }
        if (!symbol.matches("[A-Z0-9.-]{1,10}")) { // Basic symbol validation
            throw new IllegalArgumentException("Invalid stock symbol format");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock name cannot be empty");
        }
        this.symbol = symbol.trim().toUpperCase();
        this.name = name.trim();
    }
    
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    
    @Override
    public String toString() {
        return symbol + " - " + name;
    }
}