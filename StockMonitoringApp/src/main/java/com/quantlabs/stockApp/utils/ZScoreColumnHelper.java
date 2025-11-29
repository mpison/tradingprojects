package com.quantlabs.stockApp.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.quantlabs.stockApp.IStockDashboard;
import com.quantlabs.stockApp.StockDashboardv1_22_4_1;
import com.quantlabs.stockApp.StockDashboardv1_22_5;
import com.quantlabs.stockApp.analysis.service.AnalysisMetricService;
import com.quantlabs.stockApp.analysis.service.HistoricalDataService;
import com.quantlabs.stockApp.analysis.service.ZScoreAnalysisService;
import com.quantlabs.stockApp.analysis.ui.ZScoreRankingGUI;
import com.quantlabs.stockApp.model.PriceData;

public class ZScoreColumnHelper {

	private final IStockDashboard dashboard;

	public ZScoreColumnHelper(IStockDashboard dashboard) {
		this.dashboard = dashboard;
	}

	public List<String> getZScoreCombinationNames() {
		try {
			Preferences prefs = Preferences.userNodeForPackage(ZScoreRankingGUI.class);
			String configStr = prefs.get(ZScoreRankingGUI.ZSCORE_PREFS_KEY, null);

			if (configStr != null) {
				JSONObject config = new JSONObject(configStr);
				if (config.has("combinations")) {
					JSONObject combinations = config.getJSONObject("combinations");
					return new ArrayList<>(combinations.keySet());
				}
			}
		} catch (Exception e) {
			dashboard.logToConsole("Error loading Z-Score combinations: " + e.getMessage());
		}
		return new ArrayList<>();
	}

	public Map<String, Map<String, Double>> getZScoreColumnValues(List<String> symbols, String combinationName,
			ZScoreRankingGUI zScoreRankingGUI, Map<String, PriceData> priceDataMap) {
		Map<String, Map<String, Double>> zScoreValues = new HashMap<>();

		try {
			ZScoreAnalysisService analysisService;

			if (zScoreRankingGUI != null) {
				// Use existing GUI's analysis service
				analysisService = zScoreRankingGUI.getAnalysisService();

				// Ensure configuration is loaded
				zScoreRankingGUI.loadConfiguration();
			} else {
				// Create a new analysis service instance
				// You'll need to inject the required dependencies or create a factory method
				analysisService = createAnalysisService();

				// Load configuration for the new service
				loadConfigurationForService(analysisService);
			}

			// Convert symbols to PriceData list
			List<PriceData> priceDataList = symbols.stream().filter(symbol -> priceDataMap.containsKey(symbol))
					.map(symbol -> priceDataMap.get(symbol)).collect(Collectors.toList());

			// Check if the combination exists
			if (!analysisService.getAvailableCombinations().contains(combinationName)) {
				dashboard.logToConsole("Z-Score combination not found: " + combinationName);
				return zScoreValues;
			}

			// Rank symbols using the selected combination
			List<Map.Entry<String, Double>> rankedSymbols = analysisService.rankSymbols(priceDataList, combinationName);

			// Create rank mapping
			int rank = 1;
			for (Map.Entry<String, Double> entry : rankedSymbols) {
				String symbol = entry.getKey();
				double zScore = entry.getValue();

				Map<String, Double> symbolValues = new HashMap<>();
				symbolValues.put("Rank", (double) rank);
				symbolValues.put("Z-Score", zScore);

				zScoreValues.put(symbol, symbolValues);
				rank++;
			}

			dashboard.logToConsole("Computed Z-Score rankings for " + rankedSymbols.size()
					+ " symbols using combination: " + combinationName);

		} catch (Exception e) {
			dashboard.logToConsole("Error computing Z-Score values: " + e.getMessage());
			e.printStackTrace();
		}

		return zScoreValues;
	}

	//Helper method to create analysis service when GUI is not available
	private ZScoreAnalysisService createAnalysisService() {
		try {
			// You'll need to inject the proper dependencies here
			// This is a simplified version - adjust based on your actual dependencies
			com.quantlabs.stockApp.analysis.service.HistoricalDataService historicalDataService = new com.quantlabs.stockApp.analysis.service.HistoricalDataService();
			com.quantlabs.stockApp.analysis.service.AnalysisMetricService metricService = new com.quantlabs.stockApp.analysis.service.AnalysisMetricService(
					historicalDataService);

			return new ZScoreAnalysisService(metricService, historicalDataService);
		} catch (Exception e) {
			dashboard.logToConsole("Error creating ZScoreAnalysisService: " + e.getMessage());
			throw new RuntimeException("Failed to create ZScoreAnalysisService", e);
		}
	}

	//Helper method to load configuration for a standalone analysis service
	private void loadConfigurationForService(ZScoreAnalysisService analysisService) {
		try {
			// Load configuration from preferences
			Preferences prefs = Preferences.userNodeForPackage(ZScoreRankingGUI.class);
			String configStr = prefs.get(ZScoreRankingGUI.ZSCORE_PREFS_KEY, null);

			if (configStr != null) {
				JSONObject config = new JSONObject(configStr);

				// Load combinations into the analysis service
				if (config.has("combinations")) {
					JSONObject combinationsJson = config.getJSONObject("combinations");

					for (String comboName : combinationsJson.keySet()) {
						JSONObject comboJson = combinationsJson.getJSONObject(comboName);
						JSONObject weightsJson = comboJson.getJSONObject("weights");

						Map<String, Double> weights = new HashMap<>();
						for (String metric : weightsJson.keySet()) {
							weights.put(metric, weightsJson.getDouble(metric));
						}

						com.quantlabs.stockApp.analysis.model.ZScoreCombination combination = new com.quantlabs.stockApp.analysis.model.ZScoreCombination(
								comboName, weights);
						analysisService.addCombination(combination);
					}

					dashboard.logToConsole(
							"Loaded " + combinationsJson.keySet().size() + " Z-Score combinations from configuration");
				}
			} else {
				dashboard.logToConsole("No Z-Score configuration found, using default combinations");
			}
		} catch (Exception e) {
			dashboard.logToConsole("Error loading Z-Score configuration: " + e.getMessage());
		}
	}

//Alternative method that doesn't require ZScoreRankingGUI parameter
	public Map<String, Map<String, Double>> getZScoreColumnValues(List<String> symbols, String combinationName,
			Map<String, PriceData> priceDataMap) {
		return getZScoreColumnValues(symbols, combinationName, null, priceDataMap);
	}

	public List<String> generateZScoreColumnNames(List<String> combinations) {
		List<String> columnNames = new ArrayList<>();
		for (String combination : combinations) {
			columnNames.add("ZScore_" + combination + "_Rank");
			columnNames.add("ZScore_" + combination + "_ZScore");
		}
		return columnNames;
	}

	public boolean isZScoreColumn(String columnName) {
		return columnName != null && columnName.startsWith("ZScore_");
	}

	public String getCombinationNameFromColumn(String columnName) {
		if (!isZScoreColumn(columnName)) {
			return null;
		}
		// Remove "ZScore_" prefix and "_Rank"/"_ZScore" suffix
		String baseName = columnName.replace("ZScore_", "");
		if (baseName.endsWith("_Rank")) {
			return baseName.replace("_Rank", "");
		} else if (baseName.endsWith("_ZScore")) {
			return baseName.replace("_ZScore", "");
		}
		return baseName;
	}

	public boolean isRankColumn(String columnName) {
		return columnName != null && columnName.startsWith("ZScore_") && columnName.endsWith("_Rank");
	}

	public boolean isZScoreValueColumn(String columnName) {
		return columnName != null && columnName.startsWith("ZScore_") && columnName.endsWith("_ZScore");
	}
}