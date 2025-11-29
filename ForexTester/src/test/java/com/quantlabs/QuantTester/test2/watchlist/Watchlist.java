package com.quantlabs.QuantTester.test2.watchlist;

import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListModel;

public class Watchlist {
    private String name;
    private DefaultListModel<Stock> stocks;
    private final Set<String> symbolSet; // To track existing symbols

    
    public Watchlist(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Watchlist name cannot be empty");
        }
        this.name = name.trim();
        this.stocks = new DefaultListModel<>();
        this.symbolSet = new HashSet<>();
    }
    
    public void addStock(Stock stock) throws IllegalArgumentException {
        if (stock == null) {
            throw new IllegalArgumentException("Stock cannot be null");
        }
        String symbol = stock.getSymbol();
        if (symbolSet.contains(symbol)) {
            throw new IllegalArgumentException("Stock with symbol '" + symbol + "' already exists");
        }
        stocks.addElement(stock);
        symbolSet.add(symbol);
    }

    public void removeStock(Stock stock) {
        if (stocks.removeElement(stock)) {
            symbolSet.remove(stock.getSymbol());
        }
    }
    
    public String getName() { return name; }
    public DefaultListModel<Stock> getStocks() { return stocks; }
    
    @Override
    public String toString() {
        return name + " (" + stocks.size() + " stocks)";
    }
}
