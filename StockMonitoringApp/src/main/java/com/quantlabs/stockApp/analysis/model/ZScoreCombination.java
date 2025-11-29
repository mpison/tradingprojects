package com.quantlabs.stockApp.analysis.model;

import java.util.Map;

public class ZScoreCombination {
    private final String name;
    private final Map<String, Double> weights; // metricFullName -> weight percentage

    public ZScoreCombination(String name, Map<String, Double> weights) {
        this.name = name;
        this.weights = weights;
    }

    // Getters
    public String getName() { return name; }
    public Map<String, Double> getWeights() { return weights; }
}