package com.quantlabs.QuantTester.test2.watchlist;

import java.util.Objects;

/**
 * Represents a mapping between a watchlist and an indicator combination
 */
public class WatchlistMapping {
    private final String name;
    private final Watchlist watchlist;
    private final IndicatorCombination combination;

    /**
     * Creates a new watchlist mapping
     * @param name Unique name for the mapping
     * @param watchlist The watchlist to map
     * @param combination The indicator combination to map
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public WatchlistMapping(String name, Watchlist watchlist, IndicatorCombination combination) 
        throws IllegalArgumentException {
        
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Mapping name cannot be null or empty");
        }
        if (watchlist == null) {
            throw new IllegalArgumentException("Watchlist cannot be null");
        }
        if (combination == null) {
            throw new IllegalArgumentException("Indicator combination cannot be null");
        }
        
        this.name = name.trim();
        this.watchlist = watchlist;
        this.combination = combination;
    }

    public String getName() { 
        return name; 
    }
    
    public Watchlist getWatchlist() { 
        return watchlist; 
    }
    
    public IndicatorCombination getCombination() { 
        return combination; 
    }

    @Override
    public String toString() {
        return String.format("%s (%s → %s)", 
            name, 
            watchlist.getName(), 
            combination.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WatchlistMapping that = (WatchlistMapping) o;
        return name.equalsIgnoreCase(that.name) &&
               watchlist.equals(that.watchlist) &&
               combination.equals(that.combination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase(), watchlist, combination);
    }
    
    /**
     * Creates a deep copy of this mapping
     * @return A new WatchlistMapping with the same properties
     */
    public WatchlistMapping copy() {
        return new WatchlistMapping(name, watchlist, combination);
    }
    
    /**
     * Creates a new mapping with the same watchlist and combination but different name
     * @param newName The name for the cloned mapping
     * @return A new WatchlistMapping
     */
    public WatchlistMapping cloneWithName(String newName) {
        return new WatchlistMapping(newName, watchlist, combination);
    }
}