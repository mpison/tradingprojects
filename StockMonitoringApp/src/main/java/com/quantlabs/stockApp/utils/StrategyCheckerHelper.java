package com.quantlabs.stockApp.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.quantlabs.stockApp.alert.model.VolumeAlertAnalyser;
import com.quantlabs.stockApp.alert.model.VolumeAlertConfig;
import com.quantlabs.stockApp.indicator.management.CustomIndicator;
import com.quantlabs.stockApp.indicator.management.IndicatorsManagementApp;
import com.quantlabs.stockApp.indicator.management.StrategyConfig;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

/**
 * Helper class for strategy-based filtering and checking operations
 */
public class StrategyCheckerHelper {
	private IndicatorsManagementApp indicatorsManagementApp;
	private ConcurrentHashMap<String, PriceData> priceDataMap;
	private Consumer<String> logger;

	// Bullish status definitions
	private static final Set<String> BULLISH_STATUSES = Set.of("Bullish Crossover", "Bullish", "Strong Uptrend",
			"Mild Uptrend", "↑ Uptrend", "Buy", "Uptrend", "Strong Buy");

	public StrategyCheckerHelper(IndicatorsManagementApp indicatorsManagementApp,
			ConcurrentHashMap<String, PriceData> priceDataMap, Consumer<String> logger) {
		this.indicatorsManagementApp = indicatorsManagementApp;
		this.priceDataMap = priceDataMap;
		this.logger = logger;
	}

	/**
	 * Get all enabled strategies from IndicatorsManagementApp
	 */
	public List<StrategyConfig> getEnabledStrategies() {
		List<StrategyConfig> enabledStrategies = new ArrayList<>();

		if (indicatorsManagementApp != null) {
			try {
				List<StrategyConfig> allStrategies = indicatorsManagementApp.getAllStrategies();
				if (allStrategies != null) {
					for (StrategyConfig strategy : allStrategies) {
						if (strategy.isEnabled()) {
							enabledStrategies.add(strategy);
						}
					}
				}
				log("Found " + enabledStrategies.size() + " enabled strategies");
			} catch (Exception e) {
				log("Error getting enabled strategies: " + e.getMessage());
			}
		}

		return enabledStrategies;
	}

	/**
	 * Get strategy by name from IndicatorsManagementApp
	 */
	public StrategyConfig getStrategyByName(String strategyName) {
		if (indicatorsManagementApp != null) {
			try {
				List<StrategyConfig> allStrategies = indicatorsManagementApp.getAllStrategies();
				if (allStrategies != null) {
					for (StrategyConfig strategy : allStrategies) {
						if (strategy.getName().equals(strategyName)) {
							return strategy;
						}
					}
				}
			} catch (Exception e) {
				log("Error getting strategy by name: " + e.getMessage());
			}
		}
		return null;
	}

	public IndicatorsManagementApp getIndicatorsManagementApp() {
		return indicatorsManagementApp;
	}

	public void setIndicatorsManagementApp(IndicatorsManagementApp indicatorsManagementApp) {
		this.indicatorsManagementApp = indicatorsManagementApp;
	}

	public ConcurrentHashMap<String, PriceData> getPriceDataMap() {
		return priceDataMap;
	}

	public void setPriceDataMap(ConcurrentHashMap<String, PriceData> priceDataMap) {
		this.priceDataMap = priceDataMap;
	}

	public Consumer<String> getLogger() {
		return logger;
	}

	public void setLogger(Consumer<String> logger) {
		this.logger = logger;
	}

	/**
	 * Check if a symbol is non-bullish according to a specific strategy Only apply
	 * the strategy if the current watchlist matches the strategy's watchlist If
	 * strategy watchlist is null, proceed with checking
	 */
	public boolean checkStrategyNonBullish(String symbol, StrategyConfig strategyConfig, Set<String> selectedTimeframes,
			String currentWatchlist) {
		try {
			String strategyWatchlist = strategyConfig.getWatchlistName();

			// Check if strategy has a watchlist configured
			if (strategyWatchlist != null && !strategyWatchlist.isEmpty() && !strategyWatchlist.equals("N/A")) {
				// Only apply this strategy if the current watchlist matches the strategy's
				// watchlist
				if (!strategyWatchlist.equals(currentWatchlist)) {
					// log("⏭️ Skipping strategy '" + strategyConfig.getName() + "' for " + symbol +
					// " - strategy watchlist '" + strategyWatchlist + "' doesn't match current
					// watchlist '" + currentWatchlist + "'");
					return false; // Skip this strategy for this symbol
				} else {
					// log("✅ Applying strategy '" + strategyConfig.getName() + "' for " + symbol +
					// " - watchlist match: '" + currentWatchlist + "'");
				}
			} else {
				// Strategy has no watchlist - apply to all symbols
				// log("🔍 Executing strategy '" + strategyConfig.getName() + "' for " + symbol
				// + " (no watchlist filter)");
			}
			
			Set<String> exclusiveSymbols = strategyConfig.getExclusiveSymbols();
			
			if(!exclusiveSymbols.isEmpty() && !exclusiveSymbols.contains(symbol)) {
				return true;				
			}

			// Get the strategy's timeframes and indicators
			Set<String> strategyTimeframes = strategyConfig.getTimeframes();
			int checkedTimeframes = 0;
			int checkedIndicators = 0;

			for (String timeframe : strategyTimeframes) {
				// Skip if timeframe not selected in filter
				if (!selectedTimeframes.contains(timeframe)) {
					continue;
				}

				checkedTimeframes++;

				// Get indicators for this timeframe in the strategy
				Set<String> indicators = strategyConfig.getIndicatorsForTimeframe(timeframe);
				Set<CustomIndicator> customIndicators = strategyConfig.getCustomIndicatorsForTimeframe(timeframe);

				// Check each indicator
				for (String indicator : indicators) {
					String status = getIndicatorStatus(symbol, timeframe, indicator);
					checkedIndicators++;
					if (!isBullishStatus(status)) {
						// log("❌ Non-bullish found for " + symbol + " in " + timeframe + " - " +
						// indicator + ": " + status);
						return true; // Found non-bullish status
					}
				}

				// Check custom indicators
				for (CustomIndicator customIndicator : customIndicators) {
					String status = getCustomIndicatorStatus(symbol, timeframe, customIndicator);
					checkedIndicators++;
					if (!isBullishStatus(status)) {
						// log("❌ Non-bullish found for " + symbol + " in " + timeframe + " - Custom: "
						// + customIndicator.getDisplayName() + ": " + status);
						return true; // Found non-bullish status
					}
				}
			}

			if (checkedTimeframes > 0) {
				log("✅ Symbol " + symbol + " is bullish for strategy '" + strategyConfig.getName() + "' ("
						+ checkedTimeframes + " timeframes, " + checkedIndicators + " indicators checked)");
			}

			return false; // All indicators are bullish

		} catch (Exception e) {
			log("❌ Error checking strategy non-bullish for " + symbol + ": " + e.getMessage());
			return false;
		}
	}

	/**
	 * Check if a symbol is non-bullish according to multiple strategies Only apply
	 * strategies that match the current watchlist
	 */
	public boolean checkMultipleStrategiesNonBullish(String symbol, List<StrategyConfig> strategies,
			Set<String> selectedTimeframes, String currentWatchlist) {
		if (strategies == null || strategies.isEmpty()) {
			return false;
		}

		int totalStrategies = strategies.size();
		int skippedDueToWatchlist = 0;
		int executedStrategies = 0;

		for (StrategyConfig strategy : strategies) {
			String strategyWatchlist = strategy.getWatchlistName();

			// Skip strategy if it has a watchlist that doesn't match current watchlist
			if (strategyWatchlist != null && !strategyWatchlist.isEmpty()
					&& !strategyWatchlist.equals(currentWatchlist)) {
				skippedDueToWatchlist++;
				continue;
			}

			// Execute strategy checking
			if (checkStrategyNonBullish(symbol, strategy, selectedTimeframes, currentWatchlist)) {
				// log("🎯 Symbol " + symbol + " is non-bullish according to strategy: " +
				// strategy.getName());
				return true;
			}

			executedStrategies++;
		}

		// log("📊 Strategy check complete for " + symbol + ": " +
		// executedStrategies + " strategies executed, " +
		// skippedDueToWatchlist + " skipped due to watchlist mismatch");

		return false;
	}

	/**
	 * Get indicator status for a symbol and timeframe
	 */
	public String getIndicatorStatus(String symbol, String timeframe, String indicator) {
		try {
			PriceData priceData = priceDataMap.get(symbol);
			if (priceData != null && priceData.getResults() != null) {
				AnalysisResult result = priceData.getResults().get(timeframe);
				if (result != null) {
					return mapIndicatorToStatus(result, indicator);
				}
			}
		} catch (Exception e) {
			log("Error getting indicator status for " + symbol + " " + timeframe + " " + indicator + ": "
					+ e.getMessage());
		}
		return "Unknown";
	}

	/**
	 * Map indicator name to actual status from AnalysisResult
	 */
	private String mapIndicatorToStatus(AnalysisResult result, String indicator) {
		if (result == null) {
			return "Unknown";
		}

		try {
			switch (indicator) {
			case "Trend":
				return result.getSmaTrend() != null ? result.getSmaTrend() : "Unknown";
			case "RSI":
				return result.getRsiTrend() != null ? result.getRsiTrend() : "Unknown";
			case "MACD":
				return result.getMacdStatus() != null ? result.getMacdStatus() : "Unknown";
			case "MACD(5,8,9)":
				return result.getMacd359Status() != null ? result.getMacd359Status() : "Unknown";
			case "PSAR(0.01)":
				return result.getPsar001Trend() != null ? result.getPsar001Trend() : "Unknown";
			case "PSAR(0.05)":
				return result.getPsar005Trend() != null ? result.getPsar005Trend() : "Unknown";
			case "HeikenAshi":
				return result.getHeikenAshiTrend() != null ? result.getHeikenAshiTrend() : "Unknown";
			case "VWAP":
				return result.getVwapStatus() != null ? result.getVwapStatus() : "Unknown";
			case "HighestCloseOpen":
				return result.getHighestCloseOpenStatus() != null ? result.getHighestCloseOpenStatus() : "Unknown";
			case "Action":
				// You might need to calculate this based on other indicators
				return "Unknown";
			default:
				return "Unknown";
			}
		} catch (Exception e) {
			log("Error mapping indicator to status: " + indicator + " - " + e.getMessage());
			return "Unknown";
		}
	}

	/**
	 * Get custom indicator status for a symbol and timeframe
	 */
	public String getCustomIndicatorStatus(String symbol, String timeframe, CustomIndicator customIndicator) {
		try {
			// Implementation depends on how custom indicators are stored and calculated
			// This is a placeholder - you'll need to implement based on your custom
			// indicator system
			PriceData priceData = priceDataMap.get(symbol);
			if (priceData != null && priceData.getResults() != null) {
				AnalysisResult result = priceData.getResults().get(timeframe);
				if (result != null) {
					// Check if custom indicator result is stored in AnalysisResult
					// You might need to extend AnalysisResult to support custom indicators
					String indicatorResult = result.getCustomIndicatorValue(customIndicator.getName());
					return indicatorResult; // Placeholder
				}
			}
		} catch (Exception e) {
			log("Error getting custom indicator status for " + symbol + " " + timeframe + ": " + e.getMessage());
		}
		return "Unknown";
	}

	/**
	 * Check if a status string is considered bullish
	 */
	public boolean isBullishStatus(String status) {
		if (status == null || status.isEmpty()) {
			return false;
		}

		String cleanStatus = cleanStatusString(status);
		return BULLISH_STATUSES.contains(cleanStatus);
	}

	/**
	 * Clean status string by removing additional information
	 */
	private String cleanStatusString(String status) {
		if (status == null) {
			return "";
		}

		String cleanStatus = status.trim();

		// Handle status strings with additional information
		if (cleanStatus.startsWith("↑") || cleanStatus.startsWith("↓")) {
			String[] parts = cleanStatus.split(" ", 2);
			if (parts.length >= 2) {
				cleanStatus = parts[0] + " " + parts[1];
			}
		}
		
		if(cleanStatus.contains("Trend:") || cleanStatus.contains("Status:")) {
			cleanStatus = extractTrendOrStatusSimple(cleanStatus);
		}

		if (cleanStatus.contains("@")) {
			String[] parts = cleanStatus.split(" ", 2);
			if (parts.length >= 1) {
				cleanStatus = parts[0];
			}
		}

		return cleanStatus;
	}
	
	public static String extractTrendOrStatusSimple(String input) {
	    if (input == null) return null;
	    
	    String[] keywords = {"Trend:", "Status:"};
	    
	    for (String keyword : keywords) {
	        int index = input.indexOf(keyword);
	        if (index != -1) {
	            int valueStart = index + keyword.length();
	            // Skip whitespace
	            while (valueStart < input.length() && Character.isWhitespace(input.charAt(valueStart))) {
	                valueStart++;
	            }
	            // Extract word until comma, space, or end of string
	            StringBuilder word = new StringBuilder();
	            for (int i = valueStart; i < input.length(); i++) {
	                char c = input.charAt(i);
	                if (Character.isLetterOrDigit(c) || c == '_') {
	                    word.append(c);
	                } else {
	                    break;
	                }
	            }
	            return word.length() > 0 ? word.toString() : null;
	        }
	    }
	    return null;
	}

	/**
	 * Check volume alerts for a symbol
	 */
	public boolean checkVolumeAlerts(String symbol, VolumeAlertConfig volumeAlertConfig) {
		try {
			return VolumeAlertAnalyser.analyze(symbol, volumeAlertConfig, priceDataMap);
		} catch (Exception e) {
			log("Error checking volume alerts for " + symbol + ": " + e.getMessage());
			return false;
		}
	}

	/**
	 * Get strategy statistics for logging
	 */
	public Map<String, Object> getStrategyStatistics(List<StrategyConfig> strategies) {
		Map<String, Object> stats = new HashMap<>();

		if (strategies != null) {
			stats.put("totalStrategies", strategies.size());

			int totalTimeframes = 0;
			int totalIndicators = 0;
			int totalCustomIndicators = 0;

			for (StrategyConfig strategy : strategies) {
				totalTimeframes += strategy.getTimeframes().size();
				for (String timeframe : strategy.getTimeframes()) {
					totalIndicators += strategy.getIndicatorsForTimeframe(timeframe).size();
					totalCustomIndicators += strategy.getCustomIndicatorsForTimeframe(timeframe).size();
				}
			}

			stats.put("totalTimeframes", totalTimeframes);
			stats.put("totalIndicators", totalIndicators);
			stats.put("totalCustomIndicators", totalCustomIndicators);
		}

		return stats;
	}

	/**
	 * Validate strategy configuration
	 */
	public boolean validateStrategy(StrategyConfig strategy) {
		if (strategy == null) {
			return false;
		}

		try {
			// Check if strategy has a name
			if (strategy.getName() == null || strategy.getName().trim().isEmpty()) {
				return false;
			}

			// Check if strategy has at least one timeframe
			if (strategy.getTimeframes().isEmpty()) {
				return false;
			}

			// Check if each timeframe has at least one indicator
			for (String timeframe : strategy.getTimeframes()) {
				Set<String> indicators = strategy.getIndicatorsForTimeframe(timeframe);
				Set<CustomIndicator> customIndicators = strategy.getCustomIndicatorsForTimeframe(timeframe);

				if (indicators.isEmpty() && customIndicators.isEmpty()) {
					return false;
				}
			}

			return true;

		} catch (Exception e) {
			log("Error validating strategy: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Get strategies that are applicable for given timeframes
	 */
	public List<StrategyConfig> filterStrategiesByTimeframes(List<StrategyConfig> strategies, Set<String> timeframes) {
		List<StrategyConfig> filteredStrategies = new ArrayList<>();

		for (StrategyConfig strategy : strategies) {
			for (String strategyTimeframe : strategy.getTimeframes()) {
				if (timeframes.contains(strategyTimeframe)) {
					filteredStrategies.add(strategy);
					break;
				}
			}
		}

		return filteredStrategies;
	}

	// In StrategyCheckerHelper.java

	/**
	 * Filter strategies by current watchlist - only include strategies that match
	 * current watchlist or have no watchlist
	 */
	public List<StrategyConfig> filterStrategiesByCurrentWatchlist(List<StrategyConfig> allStrategies,
			String currentWatchlist) {
		List<StrategyConfig> applicableStrategies = new ArrayList<>();

		for (StrategyConfig strategy : allStrategies) {
			String strategyWatchlist = strategy.getWatchlistName();

			// Include strategy if:
			// 1. Strategy has no watchlist (null or empty) - applies to all
			// 2. Strategy watchlist matches current watchlist
			if (strategyWatchlist == null || strategyWatchlist.isEmpty()
					|| strategyWatchlist.equals(currentWatchlist)) {
				applicableStrategies.add(strategy);
			}
		}

		log("📋 Strategy filtering: " + applicableStrategies.size() + " applicable strategies for watchlist '"
				+ currentWatchlist + "'");
		return applicableStrategies;
	}

	/**
	 * Check if a symbol is bullish according to ALL applicable strategies
	 */
	public boolean isSymbolBullishForAllStrategies(String symbol, List<StrategyConfig> applicableStrategies,
			String currentWatchlist, Set<String> selectedTimeframes) {
		if (applicableStrategies.isEmpty()) {
			log("⚠️ No applicable strategies for symbol " + symbol);
			return false;
		}

		int bullishStrategies = 0;
		int totalStrategies = applicableStrategies.size();

		for (StrategyConfig strategy : applicableStrategies) {
			boolean isBullishForStrategy = isSymbolBullishForStrategy(symbol, strategy, selectedTimeframes,
					currentWatchlist);

			if (isBullishForStrategy) {
				bullishStrategies++;
				// log("✅ " + symbol + " is bullish for strategy: " + strategy.getName());
			} else {
				// log("❌ " + symbol + " is NOT bullish for strategy: " + strategy.getName());
				// Early exit - if any strategy says not bullish, symbol is not bullish overall
				return false;
			}
		}

		// Symbol is only considered bullish if it passes ALL strategies
		boolean isOverallBullish = (bullishStrategies == totalStrategies);

		if (isOverallBullish) {
			log("🎯 " + symbol + " is BULLISH for ALL " + totalStrategies + " strategies");
		} else {
			log("📊 " + symbol + " passed " + bullishStrategies + "/" + totalStrategies + " strategies");
		}

		return isOverallBullish;
	}

	/**
	 * Check if a symbol is bullish for a specific strategy
	 */
	public boolean isSymbolBullishForStrategy(String symbol, StrategyConfig strategy, Set<String> selectedTimeframes,
			String currentWatchlist) {
		try {
			// Double-check watchlist match (should already be filtered, but just in case)
			String strategyWatchlist = strategy.getWatchlistName();
			if (strategyWatchlist != null && !strategyWatchlist.isEmpty()
					&& !strategyWatchlist.equals(currentWatchlist) && !strategyWatchlist.equals("N/A")) {
				// log("⏭️ Skipping strategy '" + strategy.getName() + "' for " + symbol + " -
				// watchlist mismatch");
				return true; // Skip but don't fail - this strategy doesn't apply to current watchlist
			}
			
			Set<String> exclusiveSymbols = strategy.getExclusiveSymbols();
			
			if(!exclusiveSymbols.isEmpty() && !exclusiveSymbols.contains(symbol)) {
				return false;				
			}

			// Get the strategy's timeframes and indicators
			Set<String> strategyTimeframes = strategy.getTimeframes();
			int checkedTimeframes = 0;
			int checkedIndicators = 0;

			for (String timeframe : strategyTimeframes) {
				// Skip if timeframe not selected in filter
				if (!selectedTimeframes.contains(timeframe)) {
					continue;
				}

				checkedTimeframes++;

				// Get indicators for this timeframe in the strategy
				Set<String> indicators = strategy.getIndicatorsForTimeframe(timeframe);
				Set<CustomIndicator> customIndicators = strategy.getCustomIndicatorsForTimeframe(timeframe);

				// Check each indicator
				for (String indicator : indicators) {
					String status = getIndicatorStatus(symbol, timeframe, indicator);
					checkedIndicators++;
					if (!isBullishStatus(status)) {
						// log("❌ Non-bullish found for " + symbol + " in " + timeframe + " - " +
						// indicator + ": " + status);
						return false; // Found non-bullish status
					}
				}

				// Check custom indicators
				for (CustomIndicator customIndicator : customIndicators) {
					String status = getCustomIndicatorStatus(symbol, timeframe, customIndicator);
					checkedIndicators++;
					if (!isBullishStatus(status)) {
						// log("❌ Non-bullish found for " + symbol + " in " + timeframe + " - Custom: "
						// + customIndicator.getDisplayName() + ": " + status);
						return false; // Found non-bullish status
					}
				}
			}

			if (checkedTimeframes > 0) {
				// log("✅ Symbol " + symbol + " is bullish for strategy '" + strategy.getName()
				// +
				// "' (" + checkedTimeframes + " timeframes, " + checkedIndicators + "
				// indicators checked)");
				return true;
			} else {
				// log("⚠️ No timeframes checked for " + symbol + " in strategy '" +
				// strategy.getName() + "'");
				return false; // If no timeframes were checked, consider it bearish
			}

		} catch (Exception e) {
			// log("❌ Error checking if symbol is bullish for strategy: " + symbol + " - " +
			// strategy.getName() + ": " + e.getMessage());
			return false;
		}
	}

	/**
	 * Get selected timeframes from bullish timeframe checkboxes This method should
	 * be called from StockDashboard and the result passed in
	 */
	public Set<String> getSelectedTimeframesFromBullishCheckboxes() {
		// This is a placeholder - the actual implementation should be in StockDashboard
		// since it has access to the UI components
		log("⚠️ getSelectedTimeframesFromBullishCheckboxes should be implemented in StockDashboard");
		return new HashSet<>();
	}

	/**
	 * Check if a symbol is bullish according to ANY applicable strategies (OR
	 * condition)
	 */
	public List<String> getBullishSymbolsForAnyStrategy(List<String> symbols, List<StrategyConfig> applicableStrategies,
			String currentWatchlist, Set<String> selectedTimeframes) {
		Map<String, Set<String>> strategyToSymbolsMap = new HashMap<>();
		List<String> bullishSymbols = new ArrayList<>();

		log("🔍 Checking bullish symbols using OR condition across " + applicableStrategies.size() + " strategies");
		log("   Selected timeframes: " + String.join(", ", selectedTimeframes));

		for (String symbol : symbols) {
			Set<String> matchingStrategies = new HashSet<>();

			for (StrategyConfig strategy : applicableStrategies) {
				if (isSymbolBullishForStrategy(symbol, strategy, selectedTimeframes, currentWatchlist)) {
					matchingStrategies.add(strategy.getName());

					// Track which symbols belong to which strategy
					strategyToSymbolsMap.computeIfAbsent(strategy.getName(), k -> new HashSet<>()).add(symbol);
				}
			}

			if (!matchingStrategies.isEmpty()) {
				bullishSymbols.add(symbol);
				log("✅ " + symbol + " is bullish for strategies: " + String.join(", ", matchingStrategies));
			}
		}

		// Print results per strategy
		printStrategyResults(strategyToSymbolsMap, applicableStrategies);

		return bullishSymbols;
	}

	/**
	 * Print detailed results per strategy
	 */
	private void printStrategyResults(Map<String, Set<String>> strategyToSymbolsMap, List<StrategyConfig> strategies) {
		log("\n📊 STRATEGY RESULTS SUMMARY (OR Condition)");
		log("=".repeat(60));

		int totalBullishSymbols = 0;

		for (StrategyConfig strategy : strategies) {
			String strategyName = strategy.getName();
			Set<String> symbols = strategyToSymbolsMap.getOrDefault(strategyName, new HashSet<>());

			String statusIcon = strategy.isEnabled() ? "✅" : "⏸️";
			String watchlistInfo = strategy.getWatchlistName() != null
					? " [Watchlist: " + strategy.getWatchlistName() + "]"
					: "";

			log(String.format("%s %-30s: %2d symbols %s", statusIcon, strategyName, symbols.size(), watchlistInfo));

			if (!symbols.isEmpty()) {
				log("     Symbols: " + String.join(", ", symbols));
			}

			totalBullishSymbols += symbols.size();
		}

		// Calculate unique symbols across all strategies
		Set<String> uniqueSymbols = new HashSet<>();
		strategyToSymbolsMap.values().forEach(uniqueSymbols::addAll);

		log("-".repeat(60));
		log(String.format("📈 Total unique bullish symbols: %d", uniqueSymbols.size()));
		log(String.format("🔢 Total strategy matches: %d", totalBullishSymbols));
		log(String.format("📋 Strategies with matches: %d/%d",
				(int) strategies.stream().filter(s -> strategyToSymbolsMap.containsKey(s.getName())).count(),
				strategies.size()));
	}

	/**
	 * Get bullish symbols for each strategy individually
	 */
	public Map<String, Set<String>> getBullishSymbolsPerStrategy(List<String> symbols,
			List<StrategyConfig> applicableStrategies, String currentWatchlist, Set<String> selectedTimeframes) {
		Map<String, Set<String>> strategySymbolsMap = new HashMap<>();

		for (StrategyConfig strategy : applicableStrategies) {
			Set<String> strategyBullishSymbols = new HashSet<>();
			String strategyName = strategy.getName();

			for (String symbol : symbols) {
				if (isSymbolBullishForStrategy(symbol, strategy, selectedTimeframes, currentWatchlist)) {
					strategyBullishSymbols.add(symbol);
				}
			}

			if (!strategyBullishSymbols.isEmpty()) {
				strategySymbolsMap.put(strategyName, strategyBullishSymbols);
			}
		}

		return strategySymbolsMap;
	}

	/**
	 * Check non-bullish symbols using OR condition - symbol is non-bullish if it
	 * fails ANY strategy
	 */
	public Map<String, Set<String>> getNonBullishSymbolsPerStrategy(List<String> symbols,
			List<StrategyConfig> strategies, Set<String> selectedTimeframes, String currentWatchlist) {
		Map<String, Set<String>> strategyNonBullishMap = new HashMap<>();

		log("🔍 Checking non-bullish symbols using OR condition across " + strategies.size() + " strategies");
		log("   Selected timeframes: " + String.join(", ", selectedTimeframes));

		for (StrategyConfig strategy : strategies) {
			Set<String> strategyNonBullishSymbols = new HashSet<>();
			String strategyName = strategy.getName();

			for (String symbol : symbols) {
				if (checkStrategyNonBullish(symbol, strategy, selectedTimeframes, currentWatchlist)) {
					strategyNonBullishSymbols.add(symbol);
				}
			}

			if (!strategyNonBullishSymbols.isEmpty()) {
				strategyNonBullishMap.put(strategyName, strategyNonBullishSymbols);
				// log("❌ Strategy '" + strategyName + "' found " +
				// strategyNonBullishSymbols.size() + " non-bullish symbols");
			} else {
				// log("✅ Strategy '" + strategyName + "' found 0 non-bullish symbols");
			}
		}

		return strategyNonBullishMap;
	}

	/**
	 * Print non-bullish results per strategy
	 */
	public void printNonBullishStrategyResults(Map<String, Set<String>> strategyNonBullishMap,
			List<StrategyConfig> strategies, int totalSymbols) {
		log("\n📊 NON-BULLISH STRATEGY RESULTS (OR Condition)");
		log("=".repeat(60));

		int totalNonBullishMatches = 0;

		for (StrategyConfig strategy : strategies) {
			String strategyName = strategy.getName();
			Set<String> nonBullishSymbols = strategyNonBullishMap.getOrDefault(strategyName, new HashSet<>());

			String statusIcon = strategy.isEnabled() ? "✅" : "⏸️";
			String watchlistInfo = strategy.getWatchlistName() != null
					? " [Watchlist: " + strategy.getWatchlistName() + "]"
					: "";

			log(String.format("%s %-30s: %2d symbols %s", statusIcon, strategyName, nonBullishSymbols.size(),
					watchlistInfo));

			if (!nonBullishSymbols.isEmpty()) {
				log("     Non-bullish: " + String.join(", ", nonBullishSymbols));
			}

			totalNonBullishMatches += nonBullishSymbols.size();
		}

		// Calculate unique non-bullish symbols across all strategies
		Set<String> uniqueNonBullishSymbols = new HashSet<>();
		strategyNonBullishMap.values().forEach(uniqueNonBullishSymbols::addAll);

		log("-".repeat(60));
		log(String.format("📉 Total unique non-bullish symbols: %d", uniqueNonBullishSymbols.size()));
		log(String.format("🔢 Total strategy non-bullish matches: %d", totalNonBullishMatches));
		log(String.format("📋 Strategies with non-bullish symbols: %d/%d", strategyNonBullishMap.size(),
				strategies.size()));

		if (!uniqueNonBullishSymbols.isEmpty()) {
			log("🎯 Non-bullish symbols to filter: " + String.join(", ", uniqueNonBullishSymbols));
		}
	}

	/**
	 * Check if symbol is non-bullish for ANY strategy (OR condition)
	 */
	public boolean isSymbolNonBullishForAnyStrategy(String symbol, List<StrategyConfig> strategies,
			Set<String> selectedTimeframes, String currentWatchlist) {
		for (StrategyConfig strategy : strategies) {
			if (checkStrategyNonBullish(symbol, strategy, selectedTimeframes, currentWatchlist)) {
				// log("❌ " + symbol + " is non-bullish for strategy: " + strategy.getName());
				return true;
			}
		}
		return false;
	}

	/**
	 * Get non-bullish symbols using AND condition - symbol is non-bullish only if
	 * it fails ALL strategies
	 */
	public Map<String, Set<String>> getNonBullishSymbolsIntersection(List<String> symbols,
			List<StrategyConfig> strategies, Set<String> selectedTimeframes, String currentWatchlist) {
		Map<String, Set<String>> strategyNonBullishMap = new HashMap<>();

		log("🔍 Checking non-bullish symbols using AND condition across " + strategies.size() + " strategies");
		log("   Selected timeframes: " + String.join(", ", selectedTimeframes));
		log("   Symbol must be non-bullish for ALL selected strategies");

		// First, get non-bullish symbols for each strategy
		for (StrategyConfig strategy : strategies) {
			Set<String> strategyNonBullishSymbols = new HashSet<>();
			String strategyName = strategy.getName();

			for (String symbol : symbols) {
				if (checkStrategyNonBullish(symbol, strategy, selectedTimeframes, currentWatchlist)) {
					strategyNonBullishSymbols.add(symbol);
				}
			}

			strategyNonBullishMap.put(strategyName, strategyNonBullishSymbols);
			// log("❌ Strategy '" + strategyName + "' found " +
			// strategyNonBullishSymbols.size() + " non-bullish symbols");
		}

		return strategyNonBullishMap;
	}

	/**
	 * Get intersection of non-bullish symbols across all strategies (AND condition)
	 */
	public Set<String> getIntersectionNonBullishSymbols(Map<String, Set<String>> strategyNonBullishMap,
			List<StrategyConfig> strategies) {
		if (strategies.isEmpty() || strategyNonBullishMap.isEmpty()) {
			return new HashSet<>();
		}

		// Start with the first strategy's non-bullish symbols
		Set<String> intersection = new HashSet<>(strategyNonBullishMap.get(strategies.get(0).getName()));

		// Intersect with all other strategies
		for (int i = 1; i < strategies.size(); i++) {
			String strategyName = strategies.get(i).getName();
			Set<String> strategySymbols = strategyNonBullishMap.get(strategyName);
			intersection.retainAll(strategySymbols);
		}

		return intersection;
	}

	/**
	 * Print intersection results
	 */
	public void printIntersectionNonBullishResults(Map<String, Set<String>> strategyNonBullishMap,
			Set<String> intersectionSymbols, List<StrategyConfig> strategies, int totalSymbols) {
		log("\n📊 NON-BULLISH INTERSECTION RESULTS (AND Condition)");
		log("=".repeat(60));
		log("🔍 Symbol must be non-bullish for ALL selected strategies");

		int totalNonBullishMatches = 0;

		for (StrategyConfig strategy : strategies) {
			String strategyName = strategy.getName();
			Set<String> nonBullishSymbols = strategyNonBullishMap.getOrDefault(strategyName, new HashSet<>());

			String statusIcon = strategy.isEnabled() ? "✅" : "⏸️";
			String watchlistInfo = strategy.getWatchlistName() != null
					? " [Watchlist: " + strategy.getWatchlistName() + "]"
					: "";

			log(String.format("%s %-30s: %2d non-bullish %s", statusIcon, strategyName, nonBullishSymbols.size(),
					watchlistInfo));

			totalNonBullishMatches += nonBullishSymbols.size();
		}

		log("-".repeat(60));
		log(String.format("🎯 Intersection (ALL strategies): %d symbols", intersectionSymbols.size()));
		log(String.format("📈 Union (ANY strategy): %d symbols",
				strategyNonBullishMap.values().stream().flatMap(Set::stream).collect(Collectors.toSet()).size()));
		log(String.format("📋 Total strategies: %d", strategies.size()));

		if (!intersectionSymbols.isEmpty()) {
			log("🔴 Non-bullish for ALL strategies: " + String.join(", ", intersectionSymbols));
		} else {
			log("✅ No symbols are non-bullish for ALL selected strategies");
		}

		// Show strategy agreement statistics
		printStrategyAgreementStats(strategyNonBullishMap, intersectionSymbols, strategies);
	}

	/**
	 * Print strategy agreement statistics
	 */
	private void printStrategyAgreementStats(Map<String, Set<String>> strategyNonBullishMap,
			Set<String> intersectionSymbols, List<StrategyConfig> strategies) {
		log("\n🔄 STRATEGY AGREEMENT STATISTICS");
		log("-".repeat(40));

		// Count how many strategies agree on each symbol in the intersection
		for (String symbol : intersectionSymbols) {
			int agreementCount = 0;
			List<String> agreeingStrategies = new ArrayList<>();

			for (StrategyConfig strategy : strategies) {
				String strategyName = strategy.getName();
				Set<String> nonBullishSymbols = strategyNonBullishMap.get(strategyName);
				if (nonBullishSymbols != null && nonBullishSymbols.contains(symbol)) {
					agreementCount++;
					agreeingStrategies.add(strategyName);
				}
			}

			log(String.format("   %s: %d/%d strategies agree", symbol, agreementCount, strategies.size()));
		}

		// Show symbols that almost made it to intersection (missing only 1 strategy)
		if (strategies.size() > 1) {
			Set<String> almostIntersection = getAlmostIntersectionSymbols(strategyNonBullishMap, strategies);
			if (!almostIntersection.isEmpty()) {
				log("\n🟡 Almost in intersection (missing 1 strategy):");
				for (String symbol : almostIntersection) {
					List<String> missingStrategies = new ArrayList<>();
					for (StrategyConfig strategy : strategies) {
						String strategyName = strategy.getName();
						Set<String> nonBullishSymbols = strategyNonBullishMap.get(strategyName);
						if (nonBullishSymbols == null || !nonBullishSymbols.contains(symbol)) {
							missingStrategies.add(strategyName);
						}
					}
					log(String.format("   %s: missing %s", symbol, String.join(", ", missingStrategies)));
				}
			}
		}
	}

	/**
	 * Get symbols that are non-bullish for all but one strategy
	 */
	private Set<String> getAlmostIntersectionSymbols(Map<String, Set<String>> strategyNonBullishMap,
			List<StrategyConfig> strategies) {
		Set<String> almostIntersection = new HashSet<>();

		// Get all symbols that appear in any strategy
		Set<String> allSymbols = strategyNonBullishMap.values().stream().flatMap(Set::stream)
				.collect(Collectors.toSet());

		for (String symbol : allSymbols) {
			int agreementCount = 0;
			for (StrategyConfig strategy : strategies) {
				String strategyName = strategy.getName();
				Set<String> nonBullishSymbols = strategyNonBullishMap.get(strategyName);
				if (nonBullishSymbols != null && nonBullishSymbols.contains(symbol)) {
					agreementCount++;
				}
			}

			// Symbol is non-bullish for all but one strategy
			if (agreementCount == strategies.size() - 1) {
				almostIntersection.add(symbol);
			}
		}

		return almostIntersection;
	}

	private void log(String message) {
		if (logger != null) {
			logger.accept("[StrategyChecker] " + message);
		}
	}
}