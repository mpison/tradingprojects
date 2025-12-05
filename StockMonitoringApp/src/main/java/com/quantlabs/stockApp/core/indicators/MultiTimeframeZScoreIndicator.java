package com.quantlabs.stockApp.core.indicators;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.indicators.volume.VWAPIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.indicator.management.StrategyConfig;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

import java.time.ZonedDateTime;
import java.util.*;

public class MultiTimeframeZScoreIndicator extends CachedIndicator<Num> {

	private StrategyConfig strategyConfig;
	private PriceData priceData;
	private Map<String, BarSeries> timeframeSeries;
	private ConsoleLogger logger;

	// Indicator instances cache
	private Map<String, Map<String, CachedIndicator<Num>>> indicatorCache;

	public MultiTimeframeZScoreIndicator(BarSeries series, StrategyConfig strategyConfig, PriceData priceData,
			ConsoleLogger logger) {
		super(series);
		this.strategyConfig = strategyConfig;
		this.priceData = priceData;
		this.logger = logger;
		this.timeframeSeries = new HashMap<>();
		this.indicatorCache = new HashMap<>();
		initializeTimeframeSeries();
	}

	@Override
	protected Num calculate(int index) {
		if (strategyConfig == null || timeframeSeries.isEmpty()) {
			return numOf(0);
		}

		// Calculate Z-score for this specific bar index
		double zscore = calculateZScoreForIndex(index);
		return numOf(zscore);
	}

	/**
	 * Calculate Z-score for a specific bar index across all configured timeframes
	 */
	private double calculateZScoreForIndex(int baseIndex) {
		if (baseIndex < 0) {
			return 0.0;
		}

		Map<String, Double> timeframeScores = new HashMap<>();
		Map<String, Integer> timeframeWeights = getTimeframeWeights();

		// Calculate scores for each timeframe
		for (String timeframe : getSortedTimeframes()) {
			BarSeries timeframeSeries = this.timeframeSeries.get(timeframe);
			if (timeframeSeries == null)
				continue;

			// Get matching index in this timeframe
			int timeframeIndex = getMatchingHigherTimeframeIndex(getBarSeries(), baseIndex, timeframeSeries, timeframe);
			if (timeframeIndex < 0)
				continue;

			// Calculate Z-score for this timeframe
			double timeframeScore = calculateTimeframeZScore(timeframe, timeframeSeries, timeframeIndex);
			timeframeScores.put(timeframe, timeframeScore);
		}

		// Calculate weighted overall score
		return calculateWeightedOverallScore(timeframeScores, timeframeWeights);
	}

	/**
	 * Initialize BarSeries for each timeframe from PriceData results
	 */
	private void initializeTimeframeSeries() {
		if (priceData != null && priceData.getResults() != null) {
			for (Map.Entry<String, AnalysisResult> entry : priceData.getResults().entrySet()) {
				String timeframe = entry.getKey();
				AnalysisResult result = entry.getValue();
				if (result != null && result.getBarSeries() != null) {
					timeframeSeries.put(timeframe, result.getBarSeries());
					// Initialize indicator cache for this timeframe
					indicatorCache.put(timeframe, new HashMap<>());
				}
			}
		}
	}

	/**
	 * Get the index in a higher timeframe that matches the timestamp of a lower
	 * timeframe index
	 */
	private int getMatchingHigherTimeframeIndex(BarSeries lowerTimeframeSeries, int lowerIndex,
			BarSeries higherTimeframeSeries, String higherTimeframe) {
		if (lowerIndex < 0 || lowerIndex >= lowerTimeframeSeries.getBarCount()) {
			return -1;
		}

		ZonedDateTime targetTime = lowerTimeframeSeries.getBar(lowerIndex).getEndTime();

		// Find the bar in higher timeframe that contains this timestamp
		for (int i = higherTimeframeSeries.getBarCount() - 1; i >= 0; i--) {
			ZonedDateTime higherBarEnd = higherTimeframeSeries.getBar(i).getEndTime();
			ZonedDateTime higherBarStart = higherTimeframeSeries.getBar(i).getBeginTime();

			// Check if target time falls within this higher timeframe bar
			if (!targetTime.isBefore(higherBarStart) && !targetTime.isAfter(higherBarEnd)) {
				return i;
			}
		}

		return -1; // No matching bar found
	}

	/**
	 * Calculate timeframe conversion factors
	 */
	private int getTimeframeMinutes(String timeframe) {
		switch (timeframe) {
		case "1W":
			return 7 * 24 * 60;
		case "1D":
			return 24 * 60;
		case "4H":
			return 4 * 60;
		case "1H":
			return 60;
		case "30Min":
			return 30;
		case "15Min":
			return 15;
		case "5Min":
			return 5;
		case "1Min":
			return 1;
		default:
			return 1;
		}
	}

	/**
	 * Get all available timeframes sorted by duration (longest first)
	 */
	private List<String> getSortedTimeframes() {
		List<String> timeframes = new ArrayList<>(timeframeSeries.keySet());
		timeframes.sort((tf1, tf2) -> Integer.compare(getTimeframeMinutes(tf2), getTimeframeMinutes(tf1)));
		return timeframes;
	}

	/**
	 * Calculate Z-score for a specific timeframe and index
	 */
	private double calculateTimeframeZScore(String timeframe, BarSeries series, int index) {
		if (index < 0)
			return 0.0;

		// Get indicator weights for this timeframe
		Map<String, Integer> indicatorWeights = getIndicatorWeightsForTimeframe(timeframe);
		if (indicatorWeights.isEmpty())
			return 0.0;

		double totalScore = 0.0;
		double totalWeight = 0.0;

		// Calculate each indicator's contribution
		for (Map.Entry<String, Integer> entry : indicatorWeights.entrySet()) {
			String indicatorName = entry.getKey();
			int weight = entry.getValue();

			if (weight > 0) {
				double indicatorScore = calculateIndicatorZScore(indicatorName, timeframe, series, index);
				double contribution = indicatorScore * weight;

				totalScore += contribution;
				totalWeight += weight;
			}
		}

		return totalWeight > 0 ? totalScore / totalWeight : 0.0;
	}

	/**
	 * Calculate Z-score for a specific indicator using cached instances
	 */
	private double calculateIndicatorZScore(String indicatorName, String timeframe, BarSeries series, int index) {
		try {
			// Get or create the indicator instance
			CachedIndicator<Num> indicator = getCachedIndicator(indicatorName, timeframe, series);
			if (indicator == null)
				return 0.0;

			if (index >= indicator.getBarSeries().getBarCount()) {
				return 0.0;
			}

			Num value = indicator.getValue(index);
			if (value == null)
				return 0.0;

			return value.doubleValue();

		} catch (Exception e) {
			if (logger != null) {
				logger.log("Error calculating " + indicatorName + " Z-score for " + timeframe + ": " + e.getMessage());
			}
			return 0.0;
		}
	}

	/**
	 * Get or create cached indicator instance
	 */
	private CachedIndicator<Num> getCachedIndicator(String indicatorName, String timeframe, BarSeries series) {
		Map<String, CachedIndicator<Num>> timeframeIndicators = indicatorCache.get(timeframe);
		if (timeframeIndicators == null) {
			timeframeIndicators = new HashMap<>();
			indicatorCache.put(timeframe, timeframeIndicators);
		}

		return timeframeIndicators.computeIfAbsent(indicatorName, k -> createIndicator(indicatorName, series));
	}

	/**
	 * Factory method to create indicator instances
	 */
	private CachedIndicator<Num> createIndicator(String indicatorName, BarSeries series) {
		switch (indicatorName.toUpperCase()) {
		case "MACD_ZSCORE":
			return new MACDZScoreIndicator(series, 12, 26, 9);
		case "MACD(5,8,9)_ZSCORE":
			return new MACDZScoreIndicator(series, 5, 8, 9);
		case "RSI_ZSCORE":
			return new RSIZScoreIndicator(series, 14);
		case "PSAR(0.01)_ZSCORE":
			return new PSARZScoreIndicator(series, 0.01, 0.2);
		case "PSAR(0.05)_ZSCORE":
			return new PSARZScoreIndicator(series, 0.05, 0.2);
		case "VWAP_ZSCORE":
			return new VWAPZScoreIndicator(series);
		case "VOLUME20MA_ZSCORE":
			return new VolumeMAZScoreIndicator(series, 20);
		case "TREND_ZSCORE":
			return new TrendZScoreIndicator(series);
		case "HEIKENASHI_ZSCORE":
			return new HeikenAshiZScoreIndicator(series);
		case "HIGHESTCLOSEOPEN_ZSCORE":
			return new HighestCloseOpenZScoreIndicator(series, 5);
		default:
			// For custom indicators, you would have your own implementations
			if (indicatorName.startsWith("CUSTOM_")) {
				return createCustomIndicator(indicatorName, series);
			}
			return new NeutralZScoreIndicator(series); // Default neutral indicator
		}
	}

	/**
	 * Placeholder for custom indicator creation
	 */
	private CachedIndicator<Num> createCustomIndicator(String indicatorName, BarSeries series) {
		// Implement based on your custom indicator system
		// This would retrieve parameters from strategyConfig and create the appropriate
		// indicator
		return new NeutralZScoreIndicator(series);
	}

	/**
	 * Calculate weighted overall score from timeframe scores
	 */
	private double calculateWeightedOverallScore(Map<String, Double> timeframeScores,
			Map<String, Integer> timeframeWeights) {
		double totalWeight = 0.0;
		double weightedScoreSum = 0.0;

		for (Map.Entry<String, Double> entry : timeframeScores.entrySet()) {
			String timeframe = entry.getKey();
			double score = entry.getValue();
			int weight = timeframeWeights.getOrDefault(timeframe, 0);

			if (weight > 0) {
				weightedScoreSum += score * weight;
				totalWeight += weight;
			}
		}

		return totalWeight > 0 ? weightedScoreSum / totalWeight : 0.0;
	}

	/**
	 * Get timeframe weights from strategy configuration
	 */
	private Map<String, Integer> getTimeframeWeights() {
		Map<String, Integer> weights = new HashMap<>();

		Object weightsObj = strategyConfig.getParameters().get("zscoreWeights");
		if (weightsObj instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Integer> configWeights = (Map<String, Integer>) weightsObj;
			weights.putAll(configWeights);
		}

		return weights;
	}

	/**
	 * Get indicator weights for a specific timeframe
	 */
	private Map<String, Integer> getIndicatorWeightsForTimeframe(String timeframe) {
		Map<String, Integer> weights = new HashMap<>();

		Object weightsObj = strategyConfig.getParameters().get("indicatorWeights_" + timeframe);
		if (weightsObj instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Integer> configWeights = (Map<String, Integer>) weightsObj;
			weights.putAll(configWeights);
		}

		return weights;
	}

	/**
	 * Get the Z-score value as double for a specific index
	 */
	public double getZScoreValue(int index) {
		return getValue(index).doubleValue();
	}

	/**
	 * Get Z-score values for a range of indices
	 */
	public double[] getZScoreValues(int startIndex, int endIndex) {
		if (startIndex < 0 || endIndex >= getBarSeries().getBarCount() || startIndex > endIndex) {
			return new double[0];
		}

		double[] values = new double[endIndex - startIndex + 1];
		for (int i = startIndex; i <= endIndex; i++) {
			values[i - startIndex] = getZScoreValue(i);
		}
		return values;
	}

	/**
	 * Get the strength category for a specific index
	 */
	public String getStrengthCategory(int index) {
		double zscore = getZScoreValue(index);
		return determineStrengthCategory(zscore);
	}

	private String determineStrengthCategory(double zscore) {
		if (zscore >= 90)
			return "Exceptional";
		if (zscore >= 80)
			return "Very Strong";
		if (zscore >= 70)
			return "Strong";
		if (zscore >= 60)
			return "Good";
		if (zscore >= 50)
			return "Moderate";
		if (zscore >= 40)
			return "Fair";
		if (zscore >= 30)
			return "Weak";
		if (zscore >= 20)
			return "Very Weak";
		return "Poor";
	}
}

// Example Z-Score Indicator implementations (you would have these in separate files)

/**
 * Base class for Z-Score indicators
 */
abstract class AbstractZScoreIndicator extends CachedIndicator<Num> {
	public AbstractZScoreIndicator(BarSeries series) {
		super(series);
	}

	protected double normalizeScore(double score, double maxPossible) {
		if (maxPossible == 0)
			return 0.0;
		return (score / maxPossible) * 100.0;
	}
}

/**
 * MACD Z-Score Indicator
 */
class MACDZScoreIndicator extends AbstractZScoreIndicator {
	private final int fastPeriod;
	private final int slowPeriod;
	private final int signalPeriod;
	private MACDIndicator macd;
	private EMAIndicator signal;

	public MACDZScoreIndicator(BarSeries series, int fastPeriod, int slowPeriod, int signalPeriod) {
		super(series);
		this.fastPeriod = fastPeriod;
		this.slowPeriod = slowPeriod;
		this.signalPeriod = signalPeriod;
		initializeIndicators();
	}

	private void initializeIndicators() {
		ClosePriceIndicator closePrice = new ClosePriceIndicator(getBarSeries());
		this.macd = new MACDIndicator(closePrice, fastPeriod, slowPeriod);
		this.signal = new EMAIndicator(macd, signalPeriod);
	}

	@Override
	protected Num calculate(int index) {
		int requiredBars = Math.max(fastPeriod, slowPeriod) + signalPeriod;
		if (index < requiredBars - 1)
			return numOf(0);

		double currentMacd = macd.getValue(index).doubleValue();
		double currentSignal = signal.getValue(index).doubleValue();

		double score = 0.0;
		double maxScore = 0.0;

		// MACD above signal line
		maxScore += 40;
		if (currentMacd > currentSignal)
			score += 40;

		// Both above zero
		maxScore += 30;
		if (currentMacd > 0 && currentSignal > 0)
			score += 30;

		// Momentum (compared to previous)
		if (index > 0) {
			maxScore += 30;
			double prevMacd = macd.getValue(index - 1).doubleValue();
			if (currentMacd > prevMacd)
				score += 30;
		}

		double normalizedScore = normalizeScore(score, maxScore);
		return numOf(normalizedScore);
	}
}

/**
 * RSI Z-Score Indicator
 */
class RSIZScoreIndicator extends AbstractZScoreIndicator {
	private final int period;
	private RSIIndicator rsi;

	public RSIZScoreIndicator(BarSeries series, int period) {
		super(series);
		this.period = period;
		initializeIndicators();
	}

	private void initializeIndicators() {
		ClosePriceIndicator closePrice = new ClosePriceIndicator(getBarSeries());
		this.rsi = new RSIIndicator(closePrice, period);
	}

	@Override
	protected Num calculate(int index) {
		if (index < period)
			return numOf(0);

		double rsiValue = rsi.getValue(index).doubleValue();

		// RSI scoring: 30-70 is optimal range
		double score;
		if (rsiValue >= 30 && rsiValue <= 70) {
			// Center around 50 gives highest score
			double distanceFrom50 = Math.abs(rsiValue - 50);
			score = Math.max(0, 100 - (distanceFrom50 * 2));
		} else {
			// Outside optimal range - lower score
			score = Math.max(0, 50 - Math.abs(rsiValue - 50));
		}

		return numOf(score);
	}
}

/**
 * PSAR Z-Score Indicator
 */
class PSARZScoreIndicator extends AbstractZScoreIndicator {
	private final double step;
	private final double max;
	private ParabolicSarIndicator psar;

	public PSARZScoreIndicator(BarSeries series, double step, double max) {
		super(series);
		this.step = step;
		this.max = max;
		initializeIndicators();
	}

	private void initializeIndicators() {
		this.psar = new ParabolicSarIndicator(getBarSeries(), getBarSeries().numOf(step), getBarSeries().numOf(step),
				getBarSeries().numOf(max));
	}

	@Override
	protected Num calculate(int index) {
		if (index < 2)
			return numOf(0);

		double currentClose = getBarSeries().getBar(index).getClosePrice().doubleValue();
		double psarValue = psar.getValue(index).doubleValue();

		double score = 0.0;
		double maxScore = 0.0;

		// Price above PSAR (bullish)
		maxScore += 60;
		if (currentClose > psarValue)
			score += 60;

		// PSAR acceleration
		if (index > 1) {
			maxScore += 40;
			double currentPsar = psar.getValue(index).doubleValue();
			double prevPsar = psar.getValue(index - 1).doubleValue();
			double olderPsar = psar.getValue(index - 2).doubleValue();

			double currentChange = currentPsar - prevPsar;
			double previousChange = prevPsar - olderPsar;

			if (Math.abs(currentChange) > Math.abs(previousChange)) {
				score += 40; // Acceleration
			} else if (Math.abs(currentChange) == Math.abs(previousChange)) {
				score += 25; // Constant
			} else {
				score += 10; // Deceleration
			}
		}

		double normalizedScore = normalizeScore(score, maxScore);
		return numOf(normalizedScore);
	}
}

// Placeholder implementations for other indicators
class VWAPZScoreIndicator extends AbstractZScoreIndicator {
	public VWAPZScoreIndicator(BarSeries series) {
		super(series);
	}

	@Override
	protected Num calculate(int index) {
		return numOf(50.0);
	}
}

class VolumeMAZScoreIndicator extends AbstractZScoreIndicator {
	public VolumeMAZScoreIndicator(BarSeries series, int period) {
		super(series);
	}

	@Override
	protected Num calculate(int index) {
		return numOf(50.0);
	}
}

class TrendZScoreIndicator extends AbstractZScoreIndicator {
	public TrendZScoreIndicator(BarSeries series) {
		super(series);
	}

	@Override
	protected Num calculate(int index) {
		return numOf(50.0);
	}
}

class HeikenAshiZScoreIndicator extends AbstractZScoreIndicator {
	public HeikenAshiZScoreIndicator(BarSeries series) {
		super(series);
	}

	@Override
	protected Num calculate(int index) {
		return numOf(50.0);
	}
}

class HighestCloseOpenZScoreIndicator extends AbstractZScoreIndicator {
	public HighestCloseOpenZScoreIndicator(BarSeries series, int lookback) {
		super(series);
	}

	@Override
	protected Num calculate(int index) {
		return numOf(50.0);
	}
}

class NeutralZScoreIndicator extends AbstractZScoreIndicator {
	public NeutralZScoreIndicator(BarSeries series) {
		super(series);
	}

	@Override
	protected Num calculate(int index) {
		return numOf(50.0);
	}
}