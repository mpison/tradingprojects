package com.quantlabs.stockApp.reports;

import java.awt.Color;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.data.StockDataProviderFactory;
import com.quantlabs.stockApp.model.PriceData;

public class MonteCarloGraphController {
	private final List<String> initialSymbols;
	private MonteCarloGraphUI graphUI;
	private MonteCarloDataSourceManager dataSourceManager;
	private List<String> currentSymbols;
	private Set<String> primarySymbols = new HashSet<>();
	private String titleName;
	private Set<String> topList = new HashSet<>();
	private Map<String, Float> symbolThickness = new HashMap<>();
	private Map<String, Color> symbolColors = new HashMap<>();

	// Add these to MonteCarloGraphController
	private boolean useCustomTimeRange = false;
	private ZonedDateTime customStartTime;
	private ZonedDateTime customEndTime;
	private String customTimeframe = "1Min";

	// configs
	private Map<String, Object> monteCarloConfig = new HashMap<>();
	private Map<String, PriceData> priceDataMap = new HashMap<>();
	private List<String> selectedColumns = new ArrayList<>();
	private Map<String, Object> graphSettings = new HashMap<>();

	// Update existing constructors in MonteCarloGraphController to call new
	// constructor
	public MonteCarloGraphController(List<String> symbols, StockDataProviderFactory providerFactory,
			ConsoleLogger logger, String titleName) {
		this(symbols, providerFactory, logger, titleName, new HashSet<>(), false, null, null, "1Min", null, null,
				new ArrayList<>(), new HashMap<>());
	}

	public MonteCarloGraphController(List<String> symbols, StockDataProviderFactory providerFactory,
			ConsoleLogger logger, String titleName, Set<String> topList) {
		this(symbols, providerFactory, logger, titleName, topList, false, null, null, "1Min", null, null,
				new ArrayList<>(), new HashMap<>());
	}

	public MonteCarloGraphController(List<String> symbols, MonteCarloDataSourceManager dataSourceManager,
			String titleName) {
		this(symbols, dataSourceManager, titleName, new HashSet<>(), false, null, null, "1Min", null, null,
				new ArrayList<>(), new HashMap<>());
	}

	public MonteCarloGraphController(List<String> symbols, MonteCarloDataSourceManager dataSourceManager,
			String titleName, Set<String> topList) {
		this(symbols, dataSourceManager, titleName, topList, false, null, null, "1Min", null, null, new ArrayList<>(),
				new HashMap<>());
	}

	// NEW: Main constructor with all parameters
	public MonteCarloGraphController(List<String> symbols, StockDataProviderFactory providerFactory,
			ConsoleLogger logger, String titleName, Set<String> topList, boolean useCustomTimeRange,
			ZonedDateTime startTime, ZonedDateTime endTime, String timeframe, Map<String, Object> monteCarloConfig,
			Map<String, PriceData> priceDataMap, List<String> selectedColumns, Map<String, Object> graphSettings) {

		this.initialSymbols = new ArrayList<>(symbols);
		this.currentSymbols = new ArrayList<>(symbols);
		this.dataSourceManager = new MonteCarloDataSourceManager(providerFactory, logger);
		this.titleName = titleName;
		this.topList = topList != null ? new HashSet<>(topList) : new HashSet<>();
		this.useCustomTimeRange = useCustomTimeRange;
		this.customStartTime = startTime;
		this.customEndTime = endTime;
		this.customTimeframe = timeframe;
		this.monteCarloConfig = monteCarloConfig != null ? new HashMap<>(monteCarloConfig) : new HashMap<>();
		this.priceDataMap = priceDataMap != null ? new HashMap<>(priceDataMap) : new HashMap<>();
		this.selectedColumns = selectedColumns != null ? new ArrayList<>(selectedColumns) : new ArrayList<>();
		this.graphSettings = graphSettings != null ? new HashMap<>(graphSettings) : new HashMap<>();
	}

	// NEW: Constructor with data source manager
	public MonteCarloGraphController(List<String> symbols, MonteCarloDataSourceManager dataSourceManager,
			String titleName, Set<String> topList, boolean useCustomTimeRange, ZonedDateTime startTime,
			ZonedDateTime endTime, String timeframe, Map<String, Object> monteCarloConfig,
			Map<String, PriceData> priceDataMap, List<String> selectedColumns, Map<String, Object> graphSettings) {

		this.initialSymbols = new ArrayList<>(symbols);
		this.currentSymbols = new ArrayList<>(symbols);
		this.dataSourceManager = dataSourceManager;
		this.titleName = titleName;
		this.topList = topList != null ? new HashSet<>(topList) : new HashSet<>();
		this.useCustomTimeRange = useCustomTimeRange;
		this.customStartTime = startTime;
		this.customEndTime = endTime;
		this.customTimeframe = timeframe;
		this.monteCarloConfig = monteCarloConfig != null ? new HashMap<>(monteCarloConfig) : new HashMap<>();
		this.priceDataMap = priceDataMap != null ? new HashMap<>(priceDataMap) : new HashMap<>();
		this.selectedColumns = selectedColumns != null ? new ArrayList<>(selectedColumns) : new ArrayList<>();
		this.graphSettings = graphSettings != null ? new HashMap<>(graphSettings) : new HashMap<>();
	}

	public void showGraph() {
		SwingUtilities.invokeLater(() -> {
			this.graphUI = new MonteCarloGraphUI(initialSymbols, dataSourceManager, titleName, topList,
					useCustomTimeRange, customStartTime, customEndTime, customTimeframe, monteCarloConfig, priceDataMap,
					selectedColumns, graphSettings);

			// Initialize the UI first
			graphUI.initialize();

			// THEN set the primary symbols (after UI is created)
			graphUI.setPrimarySymbols(primarySymbols);

			// Pass the custom settings to UI
			graphUI.setSymbolThicknesses(symbolThickness);
			graphUI.setSymbolColors(symbolColors);
		});
	}

	public void toggleTopList() {
		if (this.graphUI != null) {
			this.graphUI.toggleTopList();
		}
	}

	public void updateSymbols(List<String> symbols) {
		List<String> updatedSymbols = graphUI.getCurrentSymbols();

		// List<String> updatedSymbols = new ArrayList<>(currentSymbols);

		// Add any symbols that don't already exist in currentSymbols
		for (String symbol : symbols) {
			if (!updatedSymbols.contains(symbol)) {
				updatedSymbols.add(symbol);
			}
		}

		this.currentSymbols = updatedSymbols;
		if (graphUI != null) {
			graphUI.updateSymbols(currentSymbols);
		}
	}

	/**
	 * Add symbols to the current list (avoiding duplicates)
	 */
	public void addSymbols(List<String> newSymbols) {
		List<String> updatedSymbols = new ArrayList<>(currentSymbols);
		for (String symbol : newSymbols) {
			if (!updatedSymbols.contains(symbol)) {
				updatedSymbols.add(symbol);
			}
		}
		updateSymbols(updatedSymbols);
	}

	/**
	 * Remove symbols from the current list
	 */
	public void removeSymbols(List<String> symbolsToRemove) {
		List<String> updatedSymbols = new ArrayList<>(currentSymbols);
		updatedSymbols.removeAll(symbolsToRemove);
		updateSymbols(updatedSymbols);
	}

	/**
	 * Get current symbols
	 */
	public List<String> getCurrentSymbols() {
		return new ArrayList<>(currentSymbols);
	}

	public void switchDataSource(String sourceName) {
		try {
			dataSourceManager.setDataSource(sourceName);
			if (graphUI != null) {
				graphUI.onDataSourceChanged(sourceName);
			}
		} catch (Exception e) {
			System.err.println("Failed to switch data source: " + e.getMessage());
		}
	}

	public void updateProviderConfiguration(String providerName, java.util.Map<String, Object> config) {
		dataSourceManager.updateProviderConfiguration(providerName, config);
	}

	public MonteCarloDataSourceManager getDataSourceManager() {
		return dataSourceManager;
	}

	// TopList methods
	public Set<String> getTopList() {
		return new HashSet<>(topList);
	}

	public void setTopList(Set<String> topList) {
		this.topList = topList != null ? new HashSet<>(topList) : new HashSet<>();
		if (graphUI != null) {
			graphUI.setTopList(this.topList);
		}
	}

	// Method to update all symbols (for Set EditList functionality)
	public void updateAllSymbols(List<String> newSymbols) {
		this.currentSymbols = new ArrayList<>(newSymbols);
		if (graphUI != null) {
			graphUI.updateAllSymbols(newSymbols);
		}
	}

	// Line thickness and color methods
	/**
	 * Set custom line thickness for a symbol
	 */
	public void setSymbolThickness(String symbol, float thickness) {
		symbolThickness.put(symbol.toUpperCase(), thickness);
		if (graphUI != null) {
			graphUI.setSymbolThickness(symbol, thickness);
		}
	}

	/**
	 * Set custom color for a symbol
	 */
	public void setSymbolColor(String symbol, Color color) {
		symbolColors.put(symbol.toUpperCase(), color);
		if (graphUI != null) {
			graphUI.setSymbolColor(symbol, color);
		}
	}

	/**
	 * Set multiple symbol thicknesses at once
	 */
	public void setSymbolThicknesses(Map<String, Float> thicknesses) {
		symbolThickness.clear();
		for (Map.Entry<String, Float> entry : thicknesses.entrySet()) {
			symbolThickness.put(entry.getKey().toUpperCase(), entry.getValue());
		}
		if (graphUI != null) {
			graphUI.setSymbolThicknesses(thicknesses);
		}
	}

	/**
	 * Set multiple symbol colors at once
	 */
	public void setSymbolColors(Map<String, Color> colors) {
		symbolColors.clear();
		for (Map.Entry<String, Color> entry : colors.entrySet()) {
			symbolColors.put(entry.getKey().toUpperCase(), entry.getValue());
		}
		if (graphUI != null) {
			graphUI.setSymbolColors(colors);
		}
	}

	/**
	 * Get symbol thickness map
	 */
	public Map<String, Float> getSymbolThickness() {
		return new HashMap<>(symbolThickness);
	}

	/**
	 * Get symbol color map
	 */
	public Map<String, Color> getSymbolColors() {
		return new HashMap<>(symbolColors);
	}

	/**
	 * Clear all symbol thickness settings
	 */
	public void clearSymbolThickness() {
		symbolThickness.clear();
		if (graphUI != null) {
			graphUI.clearSymbolThickness();
		}
	}

	/**
	 * Clear all symbol colors
	 */
	public void clearSymbolColors() {
		symbolColors.clear();
		if (graphUI != null) {
			graphUI.clearSymbolColors();
		}
	}

	// In MonteCarloGraphController.java - add these methods

	/**
	 * Get the UI component for window listener attachment
	 */
	public MonteCarloGraphUI getGraphUI() {
		return graphUI;
	}

	/**
	 * Add window listener to the UI frame
	 */
	public void addWindowListener(java.awt.event.WindowListener listener) {
		if (graphUI != null) {
			graphUI.addWindowListener(listener);
		}
	}

	public boolean checkIfClosed() {
		if (graphUI != null) {
			return graphUI.checkIfClosed();
		}
		return true;
	}

	// Primary symbols methods
	public void setPrimarySymbols(Set<String> primarySymbols) {
		this.primarySymbols = primarySymbols != null ? new HashSet<>(primarySymbols) : new HashSet<>();
		if (graphUI != null) {
			graphUI.setPrimarySymbols(this.primarySymbols);
		}
	}

	public Set<String> getPrimarySymbols() {
		return new HashSet<>(primarySymbols);
	}

	public void frameToFront() {
		if (graphUI != null) {
			graphUI.frameToFront();
		}
	}

	public void updateTitle(String newTitle) {
		if (graphUI != null) {
			graphUI.updateTitle(newTitle);
		}
	}

	public String getTitle() {
		if (graphUI != null) {
			return graphUI.getTitle();
		}
		return titleName;
	}

	// Add these methods to MonteCarloGraphController.java

	/**
	 * Configure time range settings
	 */
	public void configureTimeRange() {
		if (graphUI != null) {
			graphUI.showTimeRangeConfiguration();
		}
	}

	/**
	 * Check if custom time range is currently enabled
	 */
	public boolean isCustomTimeRangeEnabled() {
		if (graphUI != null) {
			return graphUI.isCustomTimeRangeEnabled();
		}
		return false;
	}

	/**
	 * Set custom time range
	 */
	public void setCustomTimeRange(ZonedDateTime start, ZonedDateTime end) {
		if (graphUI != null) {
			graphUI.setCustomTimeRange(start, end);
		}
	}

	/**
	 * Set custom time range programmatically
	 */
	public void setCustomTimeRange(ZonedDateTime start, ZonedDateTime end, String timeframe) {
		if (graphUI != null) {
			graphUI.setCustomTimeRange(start, end, timeframe);
		} else {
			// Store for later application when UI is created
			// You might want to add storage variables for this case
			System.out.println("UI not yet initialized, custom time range will be set when graph is shown");
		}
	}

	/**
	 * Switch to current time mode with specific time range
	 */
	public void setCurrentTimeMode(String timeRange) {
		if (graphUI != null) {
			graphUI.setCurrentTimeMode(timeRange);
		}
	}

	/**
	 * Update custom time range and refresh chart immediately
	 */
	public void updateCustomTimeRange(ZonedDateTime start, ZonedDateTime end, String timeframe) {
		if (graphUI != null) {
			graphUI.updateCustomTimeRange(start, end, timeframe);
		} else {
			System.out.println("UI not initialized, cannot update time range");
		}
	}

	/**
	 * Update current time mode with specific time range and refresh chart
	 * immediately
	 */
	public void updateCurrentTimeMode(String timeRange) {
		if (graphUI != null) {
			graphUI.updateCurrentTimeMode(timeRange);
		} else {
			System.out.println("UI not initialized, cannot update time range");
		}
	}

	/**
	 * Force refresh the chart with current time range settings
	 */
	public void forceRefreshChart() {
		if (graphUI != null) {
			graphUI.forceRefreshChart();
		} else {
			System.out.println("UI not initialized, cannot refresh chart");
		}
	}

	/**
	 * Get current time range settings
	 */
	public Map<String, Object> getCurrentTimeRangeSettings() {
		Map<String, Object> settings = new HashMap<>();
		if (graphUI != null) {
			settings.put("isCustomTimeRange", graphUI.isCustomTimeRangeEnabled());
			// You can add more settings here if needed
		}
		return settings;
	}

	public Map<String, Object> getMonteCarloConfig() {
		return monteCarloConfig;
	}

	public void setMonteCarloConfig(Map<String, Object> monteCarloConfig) {
		this.monteCarloConfig = monteCarloConfig;
		if (graphUI != null) {
			this.graphUI.setMonteCarloConfig(monteCarloConfig);
		}
	}

	public Map<String, PriceData> getPriceDataMap() {
		return priceDataMap;
	}

	public void setPriceDataMap(Map<String, PriceData> priceDataMap) {
		this.priceDataMap = priceDataMap;
		if (graphUI != null) {
			this.graphUI.setPriceDataMap(priceDataMap);
		}
	}

}