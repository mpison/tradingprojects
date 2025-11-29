package com.quantlabs.QuantTester.test2.watchlist;

import java.util.List;

//==================== Model Classes ====================
public class IndicatorCombination {
    private String name;
    private List<Indicator> indicators;
    
    public IndicatorCombination(String name, List<Indicator> indicators) {
        this.name = name;
        this.indicators = indicators;
    }
    
    public String getName() { return name; }
    public List<Indicator> getIndicators() { return indicators; }
    public void setIndicators(List<Indicator> indicators) { this.indicators = indicators; }
    
    @Override
    public String toString() {
        return name + " (" + indicators.size() + " indicators)";
    }
}
