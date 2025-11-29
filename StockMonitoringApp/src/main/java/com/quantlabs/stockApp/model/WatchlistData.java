package com.quantlabs.stockApp.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WatchlistData implements Serializable {
    private Set<String> symbols;
    private String primarySymbol;
    
    public WatchlistData() {
        this.symbols = new HashSet<>();
        this.primarySymbol = "";
    }
    
    public WatchlistData(Set<String> symbols, String primarySymbol) {
        this.symbols = new HashSet<>(symbols);
        this.primarySymbol = primarySymbol != null ? primarySymbol : "";
    }
    
    // Getters
    public Set<String> getSymbols() {
        return Collections.unmodifiableSet(symbols);
    }
    
    public String getPrimarySymbol() {
        return primarySymbol;
    }
    
    // Setters
    public void setPrimarySymbol(String primarySymbol) {
        // Only set if the symbol exists in the watchlist or is empty
        if (primarySymbol == null || primarySymbol.isEmpty() || symbols.contains(primarySymbol)) {
            this.primarySymbol = primarySymbol != null ? primarySymbol : "";
        }
    }
    
    public void setSymbols(Set<String> symbols) {
		this.symbols = symbols;
	}

	// Symbol management methods
    public boolean addSymbol(String symbol) {
        boolean added = symbols.add(symbol.toUpperCase());
        // If this is the first symbol and no primary is set, auto-set it as primary
        if (added && symbols.size() == 1 && (primarySymbol == null || primarySymbol.isEmpty())) {
            primarySymbol = symbol.toUpperCase();
        }
        return added;
    }
    
    public boolean removeSymbol(String symbol) {
        boolean removed = symbols.remove(symbol.toUpperCase());
        if (removed && symbol.equalsIgnoreCase(primarySymbol)) {
            primarySymbol = ""; // Clear primary if it was removed
        }
        return removed;
    }
    
    public void clearSymbols() {
        symbols.clear();
        primarySymbol = "";
    }
    
    public boolean containsSymbol(String symbol) {
        return symbols.contains(symbol.toUpperCase());
    }
    
    public int size() {
        return symbols.size();
    }
    
    public boolean isEmpty() {
        return symbols.isEmpty();
    }
    
    // Validation
    public boolean isValidPrimarySymbol(String symbol) {
        return symbol != null && !symbol.isEmpty() && symbols.contains(symbol.toUpperCase());
    }
    
    @Override
    public String toString() {
        return String.format("WatchlistData{symbols=%s, primarySymbol='%s'}", symbols, primarySymbol);
    }
    
    // Copy constructor
    public WatchlistData copy() {
        return new WatchlistData(this.symbols, this.primarySymbol);
    }
}