package com.quantlabs.stockApp.indicator.strategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class TrendStrategy extends AbstractIndicatorStrategy {

	private int sma1 = 9;
	private int sma2 = 20;
	private int sma3 = 200;

	public TrendStrategy(ConsoleLogger logger) {
		super(logger);
	}

	public TrendStrategy(int sma1, int sma2, int sma3, ConsoleLogger logger) {
		super(logger);
		this.sma1 = sma1;
		this.sma2 = sma2;
		this.sma3 = sma3;
	}

	private boolean enabled = true;

	// Z-score calculation weights
	private static final double PRICE_POSITION_WEIGHT = 40.0;
	private static final double MA_ALIGNMENT_WEIGHT = 35.0;
	private static final double TREND_STRENGTH_WEIGHT = 25.0;

	@Override
	public String getName() {
		if (sma1 == 9 && sma2 == 20 && sma3 == 200) {
			return "TREND";
		} else {
			return "TREND(" + sma1 + "," + sma2 + "," + sma3 + ")";
		}
	}

	@Override
	public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
		try {
			ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

			// Calculate different moving averages
			SMAIndicator sma9 = new SMAIndicator(closePrice, sma1);
			SMAIndicator sma20 = new SMAIndicator(closePrice, sma2);
			SMAIndicator sma200 = new SMAIndicator(closePrice, sma3);
			EMAIndicator ema20 = new EMAIndicator(closePrice, sma1);

			double sm1 = sma9.getValue(endIndex).doubleValue();
			double sm2 = sma20.getValue(endIndex).doubleValue();
			double sm3 = sma200.getValue(endIndex).doubleValue();

			double ema1 = ema20.getValue(endIndex).doubleValue();

			String name = getName();

			// Set values only if we have enough data
			if (sma1 == 9 && sma2 == 20 && sma3 == 200) { // 9-period SMA needs 9 bars, index 8 is the 9th bar
				result.setSma9(sm1);
				// 20-period SMA needs 20 bars
				result.setSma20(sm2);

				// 200-period SMA needs 200 bars
				result.setSma200(sm3);

				// 20-period EMA needs 20 bars
				result.setEma20(ema1);
			} else {
				result.setCustomIndicatorValue(name + "_TREND1", String.valueOf(sm1));
				result.setCustomIndicatorValue(name + "_TREND2", String.valueOf(sm2));
				result.setCustomIndicatorValue(name + "_TREND3", String.valueOf(sm3));

			}

			// Determine and set trend
			String trend = determineTrend(result);

			// Calculate Z-score after setting all values
			double zscore = calculateZscore(series, result, endIndex);

			if (sma1 == 9 && sma2 == 20 && sma3 == 200) {
				result.setTrend(trend);
				result.setTrendZscore(zscore);

				if (logger != null) {
					logger.log(String.format("Trend analysis for %s: %s (SMA1: %.2f, SMA2: %.2f, SMA3: %.2f)", symbol,
							trend, result.getSma9(), result.getSma20(), result.getSma200()));
				}

			} else {

				result.setCustomIndicatorValue(name + "_TREND1", String.valueOf(sm1));
				result.setCustomIndicatorValue(name + "_TREND2", String.valueOf(sm2));
				result.setCustomIndicatorValue(name + "_TREND3", String.valueOf(sm3));
				result.setCustomIndicatorValue(name + "_TREND", String.valueOf(trend));
				result.setCustomIndicatorValue(name + "_TRENDTrend", String.valueOf(trend));
				result.setCustomIndicatorValue(name + "_TRENDZscore", String.valueOf(zscore));
			}

		} catch (Exception e) {
			if (logger != null) {
				logger.log("Error calculating Trend indicators: " + e.getMessage());
			}
		}
	}

	public String getValueTrendValue(AnalysisResult analysisResult) {
		String returnVal = "TrendValue data error";
		try {
			if (getSma1() == 9 && getSma2() == 20 && getSma3() == 200) {
				double sma20 = analysisResult.getSma20();
				double sma200 = analysisResult.getSma200();
				String trend = analysisResult.getSmaTrend();

				return String.format("SMA20: %.2f, SMA200: %.2f, Trend: %s", sma20, sma200, trend);

			} else {

				String trendName = getName();

				double sm1 = Double.valueOf(analysisResult.getCustomIndicatorValue(trendName + "_TREND1"));
				double sm2 = Double.valueOf(analysisResult.getCustomIndicatorValue(trendName + "_TREND1"));
				double sm3 = Double.valueOf(analysisResult.getCustomIndicatorValue(trendName + "_TREND1"));

				String trend = String.valueOf(analysisResult.getCustomIndicatorValue(trendName + "_TRENDTrend"));

				return String.format("SMA1: %.2f, SMA2: %.2f, SMA3: %.2f, Trend: %s", sm1, sm2, sm3, trend);

			}
		} catch (Exception e) {
			return returnVal;
		}

	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getSma1() {
		return sma1;
	}

	public void setSma1(int sma1) {
		this.sma1 = sma1;
	}

	public int getSma2() {
		return sma2;
	}

	public void setSma2(int sma2) {
		this.sma2 = sma2;
	}

	public int getSma3() {
		return sma3;
	}

	public void setSma3(int sma3) {
		this.sma3 = sma3;
	}

	private String determineTrend(Num close, Num sma, Num sma200, Num ema) {
		if (close == null || sma == null || sma200 == null || ema == null) {
			return "Neutral";
		}
		if (close.isGreaterThan(sma) && close.isGreaterThan(sma200) && sma.isGreaterThan(sma200)) {
			return "Uptrend";
		} else if (close.isLessThan(sma) && close.isLessThan(sma200) && sma.isLessThan(sma200)) {
			return "Downtrend";
		}
		return "Neutral";
	}

	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
		if (sma1 == 9 && sma2 == 20 && sma3 == 200) {
			result.setSmaTrend(currentTrend);
		} else {
			result.setCustomIndicatorValue(getName() + "_TRENDTrend", String.valueOf(currentTrend));
		}
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		if (Double.isNaN(result.getSma9()) || Double.isNaN(result.getSma20()) || Double.isNaN(result.getSma200())) {
			return "Neutral";
		}

		double price = result.getPrice();
		double sma9 = 0;
		double sma20 = 0;
		double sma200 = 0;

		if (sma1 == 9 && sma2 == 20 && sma3 == 200) {
			sma9 = result.getSma9();
			sma20 = result.getSma20();
			sma200 = result.getSma200();
		} else {

			sma9 = Double.valueOf(result.getCustomIndicatorValue(getName() + "_TREND1"));
			sma20 = Double.valueOf(result.getCustomIndicatorValue(getName() + "_TREND2"));
			sma200 = Double.valueOf(result.getCustomIndicatorValue(getName() + "_TREND3"));
		}

		// Enhanced trend determination with multiple conditions
		boolean strongUptrend = price > sma9 && price > sma20 && price > sma200 && sma9 > sma20 && sma20 > sma200;
		boolean weakUptrend = price > sma9 && price > sma20 && price > sma200;
		boolean strongDowntrend = price < sma9 && price < sma20 && price < sma200 && sma9 < sma20 && sma20 < sma200;
		boolean weakDowntrend = price < sma9 && price < sma20 && price < sma200;

		if (strongUptrend) {
			return "Strong Uptrend";
		} else if (weakUptrend) {
			return "Uptrend";
		} else if (strongDowntrend) {
			return "Strong Downtrend";
		} else if (weakDowntrend) {
			return "Downtrend";
		} else {
			return "Neutral";
		}
	}

	@Override
	public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
		double zscore = 0.0;
		double maxPossibleScore = 0.0;

		double price = result.getPrice();
		double sma9 = result.getSma9();
		double sma20 = result.getSma20();
		double sma200 = result.getSma200();
		double ema20 = result.getEma20();

		// 1. Price Position Relative to MAs (40 points)
		maxPossibleScore += PRICE_POSITION_WEIGHT;
		double positionScore = calculatePricePositionScore(price, sma9, sma20, sma200, ema20);
		zscore += positionScore;

		// 2. Moving Average Alignment (35 points)
		maxPossibleScore += MA_ALIGNMENT_WEIGHT;
		double alignmentScore = calculateMAAlignmentScore(sma9, sma20, sma200, ema20);
		zscore += alignmentScore;

		// 3. Trend Strength and Consistency (25 points)
		maxPossibleScore += TREND_STRENGTH_WEIGHT;
		double strengthScore = calculateTrendStrengthScore(series, endIndex, price);
		zscore += strengthScore;

		// Normalize using parent class method
		double normalizedZscore = normalizeScore(zscore, maxPossibleScore);

		// result.setTrendZscore(normalizedZscore);

		if (logger != null) {
			logger.log(String.format("Trend Z-score for %s: %.1f (Position: %.1f, Alignment: %.1f, Strength: %.1f)",
					symbol, normalizedZscore, positionScore, alignmentScore, strengthScore));
		}

		return normalizedZscore;
	}

	private double calculatePricePositionScore(double price, double sma9, double sma20, double sma200, double ema20) {
		double score = 0.0;
		int availableIndicators = 0;
		int bullishSignals = 0;

		// Check price position relative to each MA
		if (!Double.isNaN(sma9)) {
			availableIndicators++;
			if (price > sma9)
				bullishSignals++;
		}

		if (!Double.isNaN(sma20)) {
			availableIndicators++;
			if (price > sma20)
				bullishSignals++;
		}

		if (!Double.isNaN(sma200)) {
			availableIndicators++;
			if (price > sma200)
				bullishSignals++;
		}

		if (!Double.isNaN(ema20)) {
			availableIndicators++;
			if (price > ema20)
				bullishSignals++;
		}

		if (availableIndicators > 0) {
			double bullishRatio = (double) bullishSignals / availableIndicators;

			if (bullishRatio >= 0.75) { // 75%+ of MAs are below price
				score = PRICE_POSITION_WEIGHT;
			} else if (bullishRatio >= 0.5) { // 50-74% of MAs are below price
				score = PRICE_POSITION_WEIGHT * 0.8;
			} else if (bullishRatio >= 0.25) { // 25-49% of MAs are below price
				score = PRICE_POSITION_WEIGHT * 0.4;
			} else { // Less than 25% of MAs are below price
				score = PRICE_POSITION_WEIGHT * 0.1;
			}
		} else {
			score = PRICE_POSITION_WEIGHT * 0.5; // Neutral if no data
		}

		return score;
	}

	private double calculateMAAlignmentScore(double sma9, double sma20, double sma200, double ema20) {
		double score = 0.0;
		int availablePairs = 0;
		int bullishAlignments = 0;

		// Check alignment between different MAs (bullish when shorter > longer)
		if (!Double.isNaN(sma9) && !Double.isNaN(sma20)) {
			availablePairs++;
			if (sma9 > sma20)
				bullishAlignments++;
		}

		if (!Double.isNaN(sma20) && !Double.isNaN(sma200)) {
			availablePairs++;
			if (sma20 > sma200)
				bullishAlignments++;
		}

		if (!Double.isNaN(sma9) && !Double.isNaN(sma200)) {
			availablePairs++;
			if (sma9 > sma200)
				bullishAlignments++;
		}

		if (!Double.isNaN(sma9) && !Double.isNaN(ema20)) {
			availablePairs++;
			if (sma9 > ema20)
				bullishAlignments++;
		}

		if (availablePairs > 0) {
			double alignmentRatio = (double) bullishAlignments / availablePairs;

			if (alignmentRatio >= 0.75) { // Perfect bullish alignment
				score = MA_ALIGNMENT_WEIGHT;
			} else if (alignmentRatio >= 0.5) { // Good alignment
				score = MA_ALIGNMENT_WEIGHT * 0.8;
			} else if (alignmentRatio >= 0.25) { // Mixed alignment
				score = MA_ALIGNMENT_WEIGHT * 0.4;
			} else { // Bearish alignment
				score = MA_ALIGNMENT_WEIGHT * 0.1;
			}
		} else {
			score = MA_ALIGNMENT_WEIGHT * 0.5; // Neutral if no data
		}

		return score;
	}

	private double calculateTrendStrengthScore(BarSeries series, int endIndex, double currentPrice) {
		if (endIndex < 5) {
			return TREND_STRENGTH_WEIGHT * 0.5; // Neutral if insufficient history
		}

		try {
			ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

			// Calculate recent price momentum
			double price5DaysAgo = closePrice.getValue(endIndex - 5).doubleValue();
			double priceChange = ((currentPrice - price5DaysAgo) / price5DaysAgo) * 100;

			// Calculate volatility (standard deviation of recent returns)
			double sumReturns = 0.0;
			double sumSquaredReturns = 0.0;
			int count = 0;

			for (int i = 0; i < 5 && (endIndex - i - 1) >= 0; i++) {
				double current = closePrice.getValue(endIndex - i).doubleValue();
				double previous = closePrice.getValue(endIndex - i - 1).doubleValue();
				double dailyReturn = ((current - previous) / previous) * 100;

				sumReturns += dailyReturn;
				sumSquaredReturns += dailyReturn * dailyReturn;
				count++;
			}

			if (count > 1) {
				double meanReturn = sumReturns / count;
				double variance = (sumSquaredReturns / count) - (meanReturn * meanReturn);
				double volatility = Math.sqrt(Math.max(0, variance));

				// Score based on strong momentum with low volatility (healthy trend)
				double momentumStrength = Math.min(10.0, Math.abs(priceChange)) / 10.0; // Normalize to 0-1
				double volatilityPenalty = Math.max(0, 1.0 - (volatility / 5.0)); // Penalize high volatility (>5%)

				double strengthScore = momentumStrength * volatilityPenalty * TREND_STRENGTH_WEIGHT;

				// Apply direction bonus/penalty
				if (priceChange > 2.0) { // Strong upward momentum
					strengthScore = Math.min(TREND_STRENGTH_WEIGHT, strengthScore * 1.2);
				} else if (priceChange < -2.0) { // Strong downward momentum
					strengthScore = Math.min(TREND_STRENGTH_WEIGHT, strengthScore * 0.8);
				}

				return strengthScore;
			}

		} catch (Exception e) {
			if (logger != null) {
				logger.log("Error calculating trend strength: " + e.getMessage());
			}
		}

		return TREND_STRENGTH_WEIGHT * 0.5; // Default neutral score
	}
}