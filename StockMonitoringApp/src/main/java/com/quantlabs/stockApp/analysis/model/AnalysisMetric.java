package com.quantlabs.stockApp.analysis.model;

public class AnalysisMetric {
	private final String name;
	private final String timeframe;
	private final double mean;
	private final double stdDev;
	private final Class<?> sourceClass;
	private ComparisonType comparisonType;
	private boolean lowerIsBetter; // For fields like BreakoutCount where smaller values are better

	// Updated constructor with default values for comparison settings
	public AnalysisMetric(String name, String timeframe, double mean, double stdDev, Class<?> sourceClass) {
		this.name = name;
		this.timeframe = timeframe;
		this.mean = mean;
		this.stdDev = stdDev;
		this.sourceClass = sourceClass;
		this.comparisonType = ComparisonType.HISTORICAL; // Default
		this.lowerIsBetter = false; // Default: higher values are better
	}

	// Additional constructor with comparison settings
	public AnalysisMetric(String name, String timeframe, double mean, double stdDev, Class<?> sourceClass,
			ComparisonType comparisonType, boolean lowerIsBetter) {
		this.name = name;
		this.timeframe = timeframe;
		this.mean = mean;
		this.stdDev = stdDev;
		this.sourceClass = sourceClass;
		this.comparisonType = comparisonType;
		this.lowerIsBetter = lowerIsBetter;
	}

	// Getters
	public String getName() {
		return name;
	}

	public String getTimeframe() {
		return timeframe;
	}

	public double getMean() {
		return mean;
	}

	public double getStdDev() {
		return stdDev;
	}

	public Class<?> getSourceClass() {
		return sourceClass;
	}

	public String getFullName() {
		return timeframe != null ? timeframe + "_" + name : name;
	}

	public ComparisonType getComparisonType() {
		return comparisonType;
	}

	public boolean isLowerIsBetter() {
		return lowerIsBetter;
	}

	public void setComparisonType(ComparisonType comparisonType) {
		this.comparisonType = comparisonType;
	}

	public void setLowerIsBetter(boolean lowerIsBetter) {
		this.lowerIsBetter = lowerIsBetter;
	}
}