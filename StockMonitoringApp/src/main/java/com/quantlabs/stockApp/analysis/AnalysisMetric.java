package com.quantlabs.stockApp.analysis;

public class AnalysisMetric {
    private final String name;
    private final String timeframe;
    private final double mean;
    private final double stdDev;
    private final Class<?> sourceClass;

    public AnalysisMetric(String name, String timeframe, double mean, double stdDev, Class<?> sourceClass) {
        this.name = name;
        this.timeframe = timeframe;
        this.mean = mean;
        this.stdDev = stdDev;
        this.sourceClass = sourceClass;
    }

    // Getters
    public String getName() { return name; }
    public String getTimeframe() { return timeframe; }
    public double getMean() { return mean; }
    public double getStdDev() { return stdDev; }
    public Class<?> getSourceClass() { return sourceClass; }
    
    public String getFullName() {
        return timeframe != null ? timeframe + "_" + name : name;
    }
}