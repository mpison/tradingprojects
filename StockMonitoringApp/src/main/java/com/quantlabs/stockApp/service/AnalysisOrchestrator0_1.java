package com.quantlabs.stockApp.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.ta4j.core.BarSeries;

import com.quantlabs.stockApp.data.StockDataProvider;
import com.quantlabs.stockApp.indicator.management.CustomIndicator;
import com.quantlabs.stockApp.indicator.management.CustomIndicatorCalculator;
import com.quantlabs.stockApp.indicator.strategy.AbstractIndicatorStrategy;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class AnalysisOrchestrator0_1 {

	private static TechnicalAnalysisService technicalAnalysisService;
	private static StockDataProvider dataProvider;
	private ZonedDateTime startTime;
	private ZonedDateTime endTime;

	public AnalysisOrchestrator0_1(TechnicalAnalysisService technicalAnalysisService, StockDataProvider dataProvider) {
		this.technicalAnalysisService = technicalAnalysisService;
		this.dataProvider = dataProvider;
	}

	public static LinkedHashMap<String, AnalysisResult> analyzeSymbol(String symbol, Set<String> timeframes,
			Set<String> indicatorsToCalculate, boolean uptrendFilterEnabled,
			Map<String, JComboBox<String>> indicatorTimeRangeCombos,
			Map<String, JComboBox<String>> indicatorSessionCombos, Map<String, JTextField> indicatorIndexCounterFields,
			Map<String, Map<String, Integer>> timeframeIndexRanges, String currentDataSource,
			Map<String, Set<CustomIndicator>> customIndicatorCombinations, ZonedDateTime startTime, ZonedDateTime endTime) { // Change to Set<CustomIndicator>

		LinkedHashMap<String, AnalysisResult> results = new LinkedHashMap<>();
		String previousTimeframeConsensusStatus = null;

		for (String timeframe : timeframes) {
			boolean hasStandardIndicators = !indicatorsToCalculate.isEmpty();
			boolean hasCustomIndicators = customIndicatorCombinations != null
					&& customIndicatorCombinations.containsKey(timeframe)
					&& !customIndicatorCombinations.get(timeframe).isEmpty();

			if (hasStandardIndicators || hasCustomIndicators) {
				try {
					// Apply uptrend filter if enabled
					if (uptrendFilterEnabled && previousTimeframeConsensusStatus != null) {
						if (previousTimeframeConsensusStatus == null
								|| !isBullishStatus(previousTimeframeConsensusStatus)) {
							continue;
						}
					}

					// Get historical data
					//ZonedDateTime endTime = dataProvider.getCurrentTime();
					//ZonedDateTime startTime = dataProvider.calculateDefaultStartTime(timeframe, endTime);

					BarSeries series = dataProvider.getHistoricalData(symbol, timeframe, 1000, startTime, endTime);

					String timeRange = indicatorTimeRangeCombos.get(timeframe).getSelectedItem().toString();
					String session = indicatorSessionCombos.get(timeframe).getSelectedItem().toString();
					String indexCounterField = indicatorIndexCounterFields.get(timeframe).getText();

					int indexCounter;
					try {
						indexCounter = Integer.parseInt(indexCounterField);
					} catch (NumberFormatException e) {
						indexCounter = 5; // Default value
					}

					// Get custom indicators for this timeframe
					Set<CustomIndicator> timeframeCustomIndicators = null;
					if (hasCustomIndicators) {
						timeframeCustomIndicators = customIndicatorCombinations.get(timeframe);
					}

					// Perform technical analysis with both standard and custom indicators
					AnalysisResult result = TechnicalAnalysisService.performTechnicalAnalysis(series,
							indicatorsToCalculate, timeframe, symbol, timeRange, session, indexCounter,
							timeframeIndexRanges, currentDataSource, timeframeCustomIndicators);

					results.put(timeframe, result);

					if (uptrendFilterEnabled) {
						previousTimeframeConsensusStatus = determineConsensusStatus(result, indicatorsToCalculate);
					}

				} catch (Exception e) {
					// Handle analysis errors
					System.out.println(e);
				}
			}
		}

		return results;
	}

	// Add this method to your AnalysisOrchestrator class
	public Map<String, String> analyzeCustomIndicators(String symbol, String timeframe,
			Set<CustomIndicator> customIndicators) {
		Map<String, String> customIndicatorResults = new HashMap<>();

		try {
			// Get historical data with proper method calls
			ZonedDateTime endTime = dataProvider.getCurrentTime();
			ZonedDateTime startTime = dataProvider.calculateDefaultStartTime(timeframe, endTime);

			BarSeries series = dataProvider.getHistoricalData(symbol, timeframe, 1000, startTime, endTime);

			if (series == null || series.getBarCount() == 0) {
				return customIndicatorResults;
			}

			CustomIndicatorCalculator calculator = new CustomIndicatorCalculator();

			for (CustomIndicator customIndicator : customIndicators) {
				String result = calculator.calculateCustomIndicator(customIndicator, series, timeframe, symbol)
						.toString();
				customIndicatorResults.put(customIndicator.getName(), result);
			}

		} catch (Exception e) {
			// logToConsole("Error analyzing custom indicators for " + symbol + " " +
			// timeframe + ": " + e.getMessage());
		}

		return customIndicatorResults;
	}

	private AnalysisResult performOrderedTechnicalAnalysis(BarSeries series, Set<String> indicatorsToCalculate,
			String timeframe, String symbol) {

		AnalysisResult result = new AnalysisResult();
		if (series.getBarCount() == 0)
			return result;

		int endIndex = series.getEndIndex();
		result.setPrice(series.getBar(endIndex).getClosePrice().doubleValue());

		// Execute strategies in proper order (breakout counts first, then composite)
		Map<String, AbstractIndicatorStrategy> allStrategies = technicalAnalysisService.getStrategies();

		// First pass: individual breakout strategies
		for (String indicatorName : indicatorsToCalculate) {
			if (indicatorName.contains("Breakout") && !indicatorName.equals("Breakout Count")) {
				AbstractIndicatorStrategy strategy = allStrategies.get(indicatorName);
				if (strategy != null && strategy.isEnabled()) {
					strategy.calculate(series, result, endIndex);
				}
			}
		}

		// Second pass: regular indicators
		for (String indicatorName : indicatorsToCalculate) {
			if (!indicatorName.contains("Breakout")) {
				AbstractIndicatorStrategy strategy = allStrategies.get(indicatorName);
				if (strategy != null && strategy.isEnabled()) {
					strategy.calculate(series, result, endIndex);
				}
			}
		}

		// Third pass: composite breakout strategy (if requested)
		if (indicatorsToCalculate.contains("Breakout Count")) {
			AbstractIndicatorStrategy compositeStrategy = allStrategies.get("Breakout Count");
			if (compositeStrategy != null && compositeStrategy.isEnabled()) {
				compositeStrategy.calculate(series, result, endIndex);
			}
		}

		return result;
	}

	private static boolean isBullishStatus(String status) {
		return status != null && (status.contains("Bullish") || status.contains("Uptrend") || status.contains("↑")
				|| status.contains("Buy"));
	}

	private static String determineConsensusStatus(AnalysisResult result, Set<String> indicators) {
		int bullishCount = 0;
		int totalIndicators = 0;

		// Check each indicator's status
		if (indicators.contains("Trend") && result.getSma20() > 0 && result.getSma200() > 0
				&& result.getSma20() > result.getSma200()) {
			bullishCount++;
		}
		totalIndicators++;

		if (indicators.contains("RSI") && result.getRsi() > 50) {
			bullishCount++;
		}
		totalIndicators++;

		if (indicators.contains("MACD") && "Bullish".equals(result.getMacdStatus())) {
			bullishCount++;
		}
		totalIndicators++;

		if (indicators.contains("MACD(5,8,9)") && "Bullish".equals(result.getMacd359Status())) {
			bullishCount++;
		}
		totalIndicators++;

		if (indicators.contains("PSAR(0.01)") && result.getPsar001Trend() != null
				&& result.getPsar001Trend().contains("Uptrend")) {
			bullishCount++;
		}
		totalIndicators++;

		if (indicators.contains("PSAR(0.05)") && result.getPsar005Trend() != null
				&& result.getPsar005Trend().contains("Uptrend")) {
			bullishCount++;
		}
		totalIndicators++;

		if (indicators.contains("HeikenAshi") && "Uptrend".equals(result.getHeikenAshiTrend())) {
			bullishCount++;
		}
		totalIndicators++;

		return totalIndicators > 0 && (bullishCount * 100 / totalIndicators) >= 70 ? "Bullish" : "Bearish";
	}

	public static TechnicalAnalysisService getTechnicalAnalysisService() {
		return technicalAnalysisService;
	}

	public static void setTechnicalAnalysisService(TechnicalAnalysisService technicalAnalysisService) {
		AnalysisOrchestrator0_1.technicalAnalysisService = technicalAnalysisService;
	}

	public static StockDataProvider getDataProvider() {
		return dataProvider;
	}

	public static void setDataProvider(StockDataProvider dataProvider) {
		AnalysisOrchestrator0_1.dataProvider = dataProvider;
	}

	private static class TimeframeResult {
		final String timeframe;
		final AnalysisResult result;

		TimeframeResult(String timeframe, AnalysisResult result) {
			this.timeframe = timeframe;
			this.result = result;
		}
	}

	public void setTimeRange(ZonedDateTime startTime, ZonedDateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }
}