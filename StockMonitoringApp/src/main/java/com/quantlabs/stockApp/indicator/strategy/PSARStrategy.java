package com.quantlabs.stockApp.indicator.strategy;

import java.util.concurrent.ConcurrentHashMap;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class PSARStrategy extends AbstractIndicatorStrategy {
	private final double step;
	private final double maxStep;
	private final String variant;

	// Z-score calculation weights
	private static final double PRICE_POSITION_WEIGHT = 40.0;
	private static final double TREND_STRENGTH_WEIGHT = 30.0;
	private static final double ACCELERATION_WEIGHT = 20.0;
	private static final double DISTANCE_WEIGHT = 10.0;

	public PSARStrategy(double step, double maxStep, String variant, ConsoleLogger logger) {
		super(logger);
		this.step = step;
		this.maxStep = maxStep;
		this.variant = variant;
	}

	@Override
	public String getName() {
		return "PSAR(" + step + ")";
	}

	@Override
	public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
		if (endIndex < 0)
			return;

		try {
			ParabolicSarIndicator psar = new ParabolicSarIndicator(series, series.numOf(step), series.numOf(step),
					series.numOf(maxStep));

			Num currentClose = series.getBar(endIndex).getClosePrice();
			double psarValue = psar.getValue(endIndex).doubleValue();
			String trend = psarValue < currentClose.doubleValue() ? "Uptrend" : "Downtrend";
			
			// Calculate Z-score after setting PSAR values
			double zscore =	calculateZscore(series, result, endIndex);
			
			String name = getName();

			if ("PSAR(0.01)".equals(getName())) {
				result.setPsar001(psarValue);
				result.setPsar001Trend(trend);
				result.setPsar001Zscore(zscore);
			} else if ("PSAR(0.05)".equals(getName())) {
				result.setPsar005(psarValue);
				result.setPsar005Trend(trend);
				result.setPsar005Zscore(zscore);
			} else {				
				result.setCustomIndicatorValue(name + "_PSAR", String.valueOf(psarValue));
	        	result.setCustomIndicatorValue(name + "_PSARTrend", String.valueOf(trend));
	        	result.setCustomIndicatorValue(name + "_PSARZscore", String.valueOf(zscore));
			}

			

		} catch (Exception e) {
			if (logger != null) {
				logger.log("Error calculating PSAR for " + getName() + ": " + e.getMessage());
			}
		}
	}

	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
		if ("PSAR(0.01)".equals(getName())) {
			result.setPsar001Trend(currentTrend);
		} else if ("PSAR(0.05)".equals(getName())) {
			result.setPsar005Trend(currentTrend);
		} else {
			//result.setPsarTrend(currentTrend);
			result.setCustomIndicatorValue(getName() + "_PSARTrend", String.valueOf(currentTrend));
		}
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		String returnVal = "Neutral";

		if ("PSAR(0.01)".equals(getName())) {
			returnVal = determinePsarStatus(result.getPrice(), result.getPsar001());
			result.setPsar001Trend(returnVal);
		} else if ("PSAR(0.05)".equals(getName())) {
			returnVal = determinePsarStatus(result.getPrice(), result.getPsar005());
			result.setPsar005Trend(returnVal);
		} else {
			returnVal = determinePsarStatus(result.getPrice(), result.getPsar());
			
			result.setCustomIndicatorValue(getName() + "_PSARTrend", String.valueOf(returnVal));
			
		}

		return returnVal;
	}
	
	public double getPsarValue(AnalysisResult analysisResult) {
		double psarValue;
        
        String psarName = getName();
        
        //String trend;
        
		if (step == 0.01) {
            psarValue = analysisResult.getPsar001();
            //trend = analysisResult.getPsar001Trend();
        } else if(step== 0.05){
            psarValue = analysisResult.getPsar005();
            //trend = analysisResult.getPsar005Trend();
        } else {
            psarValue = Double.valueOf(analysisResult.getCustomIndicatorValue(psarName +"_PSAR"));
            //trend = String.valueOf(analysisResult.getCustomIndicatorValue(psarName +"_PSARTrend"));            
        }
		return psarValue;
	}

	public String getPsarTrend(AnalysisResult analysisResult) {		
    
	    String psarName = getName();
	    
	    String trend;
		if (step == 0.01) {
            //psarValue = analysisResult.getPsar001();
            trend = analysisResult.getPsar001Trend();
        } else if(step== 0.05){
            trend = analysisResult.getPsar005Trend();
        } else {
           
            trend = String.valueOf(analysisResult.getCustomIndicatorValue(psarName +"_PSARTrend"));            
        }
		return trend;
	}

	private String determinePsarStatus(double close, double psarValue) {
		if (Double.isNaN(close) || Double.isNaN(psarValue)) {
			return "Neutral";
		}
		if (close > psarValue) {
			return "Bullish";
		} else {
			return "Bearish";
		}
	}

	@Override
	public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
		if (endIndex < 0)
			return 0.0;

		try {
			ParabolicSarIndicator psar = new ParabolicSarIndicator(series, series.numOf(step), series.numOf(step),
					series.numOf(maxStep));

			double currentClose = series.getBar(endIndex).getClosePrice().doubleValue();
			double psarValue = psar.getValue(endIndex).doubleValue();

			double zscore = 0.0;
			double maxPossibleScore = 0.0;

			// 1. Price Position Relative to PSAR (40 points)
			maxPossibleScore += PRICE_POSITION_WEIGHT;
			if (currentClose > psarValue) {
				// Bullish - price above PSAR
				zscore += PRICE_POSITION_WEIGHT;
			} else {
				// Bearish - price below PSAR
				zscore += 0; // No points for bearish position
			}

			// 2. Trend Strength (30 points)
			maxPossibleScore += TREND_STRENGTH_WEIGHT;
			double trendStrengthScore = calculateTrendStrength(psar, endIndex, currentClose, psarValue);
			zscore += trendStrengthScore;

			// 3. PSAR Acceleration (20 points)
			maxPossibleScore += ACCELERATION_WEIGHT;
			double accelerationScore = calculateAccelerationScore(psar, endIndex);
			zscore += accelerationScore;

			// 4. Distance from PSAR (10 points)
			maxPossibleScore += DISTANCE_WEIGHT;
			double distanceScore = calculateDistanceScore(currentClose, psarValue);
			zscore += distanceScore;

			// Normalize using parent class method
			double normalizedZscore = normalizeScore(zscore, maxPossibleScore);

			// Store in appropriate result field
			if ("PSAR(0.01)".equals(getName())) {
				result.setPsar001Zscore(normalizedZscore);
			} else if ("PSAR(0.05)".equals(getName())) {
				result.setPsar005Zscore(normalizedZscore);
			} else {
				result.setPsarZscore(normalizedZscore);
			}

			if (logger != null) {
				logger.log(String.format(
						"PSAR(%s) Z-score: %.1f (Position: %s, Strength: %.1f, Accel: %.1f, Distance: %.1f)", step,
						normalizedZscore, currentClose > psarValue ? "Bullish" : "Bearish", trendStrengthScore,
						accelerationScore, distanceScore));
			}

			return normalizedZscore;

		} catch (Exception e) {
			if (logger != null) {
				logger.log("Error calculating PSAR Z-score for " + getName() + ": " + e.getMessage());
			}
			return 0.0;
		}
	}

	private double calculateTrendStrength(ParabolicSarIndicator psar, int endIndex, double currentClose,
			double psarValue) {
		if (endIndex < 2) {
			return TREND_STRENGTH_WEIGHT * 0.5; // Neutral if insufficient history
		}

		double strengthScore = 0.0;
		boolean isBullish = currentClose > psarValue;

		// Check consistency of recent PSAR signals
		int bullishCount = 0;
		int bearishCount = 0;
		int lookbackPeriod = Math.min(5, endIndex + 1); // Check last 5 periods or available data

		for (int i = 0; i < lookbackPeriod; i++) {
			int index = endIndex - i;
			if (index < 0)
				break;

			double historicalClose = psar.getBarSeries().getBar(index).getClosePrice().doubleValue();
			double historicalPsar = psar.getValue(index).doubleValue();

			if (historicalClose > historicalPsar) {
				bullishCount++;
			} else {
				bearishCount++;
			}
		}

		// Calculate consistency ratio
		double consistencyRatio = (double) (isBullish ? bullishCount : bearishCount) / lookbackPeriod;

		if (consistencyRatio >= 0.8) {
			strengthScore = TREND_STRENGTH_WEIGHT; // 100% - very consistent trend
		} else if (consistencyRatio >= 0.6) {
			strengthScore = TREND_STRENGTH_WEIGHT * 0.8; // 80% - strong trend
		} else if (consistencyRatio >= 0.4) {
			strengthScore = TREND_STRENGTH_WEIGHT * 0.6; // 60% - moderate trend
		} else {
			strengthScore = TREND_STRENGTH_WEIGHT * 0.3; // 30% - weak/choppy trend
		}

		return strengthScore;
	}

	private double calculateAccelerationScore(ParabolicSarIndicator psar, int endIndex) {
		if (endIndex < 2) {
			return ACCELERATION_WEIGHT * 0.5; // Neutral if insufficient history
		}

		double currentPsar = psar.getValue(endIndex).doubleValue();
		double previousPsar = psar.getValue(endIndex - 1).doubleValue();
		double olderPsar = psar.getValue(endIndex - 2).doubleValue();

		double currentChange = currentPsar - previousPsar;
		double previousChange = previousPsar - olderPsar;

		// Check if PSAR is accelerating in the current direction
		if (Math.abs(currentChange) > Math.abs(previousChange)) {
			// Acceleration detected - full points
			return ACCELERATION_WEIGHT;
		} else if (Math.abs(currentChange) == Math.abs(previousChange)) {
			// Constant rate - moderate points
			return ACCELERATION_WEIGHT * 0.7;
		} else {
			// Deceleration - minimal points
			return ACCELERATION_WEIGHT * 0.3;
		}
	}

	private double calculateDistanceScore(double currentClose, double psarValue) {
		double distance = Math.abs(currentClose - psarValue);
		double pricePercentage = (distance / currentClose) * 100;

		// Optimal distance is between 1% and 3% - too close or too far is less ideal
		if (pricePercentage >= 1.0 && pricePercentage <= 3.0) {
			return DISTANCE_WEIGHT; // Perfect distance
		} else if (pricePercentage < 0.5 || pricePercentage > 5.0) {
			return DISTANCE_WEIGHT * 0.3; // Too close or too far
		} else {
			return DISTANCE_WEIGHT * 0.7; // Reasonable distance
		}
	}

	// Helper method to get PSAR configuration for logging
	public String getConfiguration() {
		return String.format("PSAR(step=%.3f, max=%.3f, variant=%s)", step, maxStep, variant);
	}

// Getters for parameters
	public double getStep() {
		return step;
	}

	public double getMaxStep() {
		return maxStep;
	}

	public String getVariant() {
		return variant;
	}

	
}