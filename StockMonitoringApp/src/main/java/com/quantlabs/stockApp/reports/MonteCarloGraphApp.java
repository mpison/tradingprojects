package com.quantlabs.stockApp.reports;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.data.StockDataProviderFactory;
import com.quantlabs.stockApp.model.PriceData;

import okhttp3.OkHttpClient;

public class MonteCarloGraphApp {
	private MonteCarloGraphController controller;
	private boolean initialized = false;
	private Set<String> topList = new HashSet<>();
	private Set<String> primarySymbols = new HashSet<>();

	private boolean initialUseCustomTimeRange = false;
	private ZonedDateTime initialStartTime;
	private ZonedDateTime initialEndTime;
	private String initialTimeframe = "1Min";

	// config
	private Map<String, Object> monteCarloConfig;
	private Map<String, PriceData> priceDataMap;
	private List<String> selectedColumns = new ArrayList<>();
	private Map<String, Object> graphSettings = new HashMap<>();

	// Update the existing constructors in MonteCarloGraphApp.java
	public MonteCarloGraphApp(List<String> symbols, String titleName) {
		initializeApp(symbols, titleName, null, null);
	}

	public MonteCarloGraphApp(List<String> symbols, String titleName, Set<String> topList) {
		this.topList = topList != null ? new HashSet<>(topList) : new HashSet<>();
		initializeApp(symbols, titleName, null, null);
	}

	// Update custom time range constructors
	public MonteCarloGraphApp(List<String> symbols, String titleName, ZonedDateTime startTime, ZonedDateTime endTime,
			String timeframe) {
		this.initialUseCustomTimeRange = true;
		this.initialStartTime = startTime;
		this.initialEndTime = endTime;
		this.initialTimeframe = timeframe != null ? timeframe : "1Min";
		initializeApp(symbols, titleName, null, null);
	}

	public MonteCarloGraphApp(List<String> symbols, StockDataProviderFactory providerFactory, ConsoleLogger logger,
			String titleName) {
		initializeApp(symbols, providerFactory, logger, titleName, null, null);
	}

	public MonteCarloGraphApp(List<String> symbols, StockDataProviderFactory providerFactory, ConsoleLogger logger,
			String titleName, Set<String> topList) {
		this.topList = topList != null ? new HashSet<>(topList) : new HashSet<>();
		initializeApp(symbols, providerFactory, logger, titleName, null, null);
	}

	// Update custom time range constructor with provider factory
	public MonteCarloGraphApp(List<String> symbols, StockDataProviderFactory providerFactory, ConsoleLogger logger,
			String titleName, ZonedDateTime startTime, ZonedDateTime endTime, String timeframe) {
		this.initialUseCustomTimeRange = true;
		this.initialStartTime = startTime;
		this.initialEndTime = endTime;
		this.initialTimeframe = timeframe != null ? timeframe : "1Min";
		initializeApp(symbols, providerFactory, logger, titleName, null, null);
	}

	// NEW CONSTRUCTORS with monteCarloConfig and priceDataMap
	public MonteCarloGraphApp(List<String> symbols, String titleName, Map<String, Object> monteCarloConfig,
			Map<String, PriceData> priceDataMap) {
		initializeApp(symbols, titleName, monteCarloConfig, priceDataMap);
	}

	public MonteCarloGraphApp(List<String> symbols, String titleName, Set<String> topList,
			Map<String, Object> monteCarloConfig, Map<String, PriceData> priceDataMap) {
		this.topList = topList != null ? new HashSet<>(topList) : new HashSet<>();
		initializeApp(symbols, titleName, monteCarloConfig, priceDataMap);
	}

	public MonteCarloGraphApp(List<String> symbols, StockDataProviderFactory providerFactory, ConsoleLogger logger,
			String titleName, Map<String, Object> monteCarloConfig, Map<String, PriceData> priceDataMap) {
		initializeApp(symbols, providerFactory, logger, titleName, monteCarloConfig, priceDataMap);
	}

	public MonteCarloGraphApp(List<String> symbols, StockDataProviderFactory providerFactory, ConsoleLogger logger,
			String titleName, Set<String> topList, Map<String, Object> monteCarloConfig,
			Map<String, PriceData> priceDataMap) {
		this.topList = topList != null ? new HashSet<>(topList) : new HashSet<>();
		initializeApp(symbols, providerFactory, logger, titleName, monteCarloConfig, priceDataMap);
	}

	public MonteCarloGraphApp(List<String> symbols, StockDataProviderFactory providerFactory, ConsoleLogger logger,
			String titleName, ZonedDateTime startTime, ZonedDateTime endTime, String timeframe,
			Map<String, Object> monteCarloConfig, Map<String, PriceData> priceDataMap) {
		this.initialUseCustomTimeRange = true;
		this.initialStartTime = startTime;
		this.initialEndTime = endTime;
		this.initialTimeframe = timeframe != null ? timeframe : "1Min";
		initializeApp(symbols, providerFactory, logger, titleName, monteCarloConfig, priceDataMap);
	}
	
	// New constructor with selected columns and graph settings
		public MonteCarloGraphApp(List<String> symbols, StockDataProviderFactory providerFactory, ConsoleLogger logger,
				String titleName, ZonedDateTime startTime, ZonedDateTime endTime, String timeframe,
				List<String> selectedColumns, Map<String, Object> graphSettings,
				Map<String, Object> monteCarloConfig, Map<String, PriceData> priceDataMap) {
			this.initialUseCustomTimeRange = true;
			this.initialStartTime = startTime;
			this.initialEndTime = endTime;
			this.initialTimeframe = timeframe != null ? timeframe : "1Min";
			this.selectedColumns = selectedColumns != null ? new ArrayList<>(selectedColumns) : new ArrayList<>();
			this.graphSettings = graphSettings != null ? new HashMap<>(graphSettings) : new HashMap<>();
			initializeApp(symbols, providerFactory, logger, titleName, monteCarloConfig, priceDataMap);
		}

	// New constructor with selected columns and graph settings
	public MonteCarloGraphApp(List<String> symbols, StockDataProviderFactory providerFactory, ConsoleLogger logger,
			String titleName, List<String> selectedColumns, Map<String, Object> graphSettings,
			Map<String, Object> monteCarloConfig, Map<String, PriceData> priceDataMap) {
		this.selectedColumns = selectedColumns != null ? new ArrayList<>(selectedColumns) : new ArrayList<>();
		this.graphSettings = graphSettings != null ? new HashMap<>(graphSettings) : new HashMap<>();
		initializeApp(symbols, providerFactory, logger, titleName, monteCarloConfig, priceDataMap);
	}

	// Empty constructor for manual initialization
	public MonteCarloGraphApp() {
		// Manual initialization required
	}

	private void initializeApp(List<String> symbols, String titleName, Map<String, Object> monteCarloConfig,
			Map<String, PriceData> priceDataMap) {
		try {
			MonteCarloDataSourceManager dataSourceManager = createSimpleDataSourceManager();

// Pass configuration and data to controller
			this.controller = new MonteCarloGraphController(symbols, dataSourceManager, titleName, topList,
					initialUseCustomTimeRange, initialStartTime, initialEndTime, initialTimeframe, monteCarloConfig,
					priceDataMap, selectedColumns, graphSettings);

			this.monteCarloConfig = monteCarloConfig;
			this.priceDataMap = priceDataMap;
			this.initialized = true;

			SwingUtilities.invokeLater(() -> {
				controller.showGraph();
			});

			System.out.println("Monte Carlo Graph App initialized with symbols: " + symbols);
			if (initialUseCustomTimeRange) {
				System.out.println("Custom time range: " + initialStartTime + " to " + initialEndTime + " timeframe: "
						+ initialTimeframe);
			}
			if (monteCarloConfig != null) {
				System.out.println("Monte Carlo config loaded: " + monteCarloConfig.size() + " items");
			}
			if (priceDataMap != null) {
				System.out.println("Price data map provided: " + priceDataMap.size() + " symbols");
			}

		} catch (Exception e) {
			System.err.println("Failed to initialize Monte Carlo Graph: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void initializeApp(List<String> symbols, StockDataProviderFactory providerFactory, ConsoleLogger logger,
			String titleName, Map<String, Object> monteCarloConfig, Map<String, PriceData> priceDataMap) {
		try {
// Pass configuration and data to controller
			this.controller = new MonteCarloGraphController(symbols, providerFactory, logger, titleName, topList,
					initialUseCustomTimeRange, initialStartTime, initialEndTime, initialTimeframe, monteCarloConfig,
					priceDataMap, selectedColumns, graphSettings);

			this.monteCarloConfig = monteCarloConfig;
			this.priceDataMap = priceDataMap;
			this.initialized = true;

			SwingUtilities.invokeLater(() -> {
				controller.showGraph();
			});

			System.out.println("Monte Carlo Graph App initialized with symbols: " + symbols);
			if (initialUseCustomTimeRange) {
				System.out.println("Custom time range: " + initialStartTime + " to " + initialEndTime + " timeframe: "
						+ initialTimeframe);
			}
			if (monteCarloConfig != null) {
				System.out.println("Monte Carlo config loaded: " + monteCarloConfig.size() + " items");
			}
			if (priceDataMap != null) {
				System.out.println("Price data map provided: " + priceDataMap.size() + " symbols");
			}

		} catch (Exception e) {
			System.err.println("Failed to initialize Monte Carlo Graph: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void applyInitialTimeRange() {
		if (initialUseCustomTimeRange && initialStartTime != null && initialEndTime != null) {
			// Apply custom time range settings
			setCustomTimeRange(initialStartTime, initialEndTime, initialTimeframe);
			System.out.println("Applied initial custom time range: " + initialStartTime + " to " + initialEndTime
					+ " with timeframe " + initialTimeframe);
		} else {
			System.out.println("Using default current time range mode");
		}
	}

	/**
	 * Manual initialization method
	 * 
	 * @param titleName
	 */
	public void initialize(List<String> symbols, String titleName,  Map<String, Object> monteCarloConfig,
			Map<String, PriceData> priceDataMap) {
		if (!initialized) {
			initializeApp(symbols, titleName, monteCarloConfig, priceDataMap);
		}
	}

	/*
	 * public void initialize(List<String> symbols, String titleName, Set<String>
	 * topList) { this.topList = topList != null ? new HashSet<>(topList) : new
	 * HashSet<>(); if (!initialized) { initializeApp(symbols, titleName); } }
	 */

	/**
	 * Set custom time range programmatically
	 */
	public void setCustomTimeRange(ZonedDateTime start, ZonedDateTime end, String timeframe) {
		if (checkInitialization()) {
			controller.setCustomTimeRange(start, end, timeframe);
		}
	}

	/**
	 * Set custom time range using Date objects
	 */
	public void setCustomTimeRange(Date start, Date end, String timeframe) {
		if (checkInitialization()) {
			ZonedDateTime startZdt = ZonedDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault());
			ZonedDateTime endZdt = ZonedDateTime.ofInstant(end.toInstant(), ZoneId.systemDefault());
			controller.setCustomTimeRange(startZdt, endZdt, timeframe);
		}
	}

	/**
	 * Switch to current time mode with specific time range
	 */
	public void setCurrentTimeMode(String timeRange) {
		if (checkInitialization()) {
			controller.setCurrentTimeMode(timeRange);
		}
	}

	/**
	 * Get current time range configuration
	 */
	public String getCurrentTimeRangeMode() {
		if (checkInitialization()) {
			return controller.isCustomTimeRangeEnabled() ? "CUSTOM" : "CURRENT";
		}
		return "UNKNOWN";
	}

	// Add these methods to MonteCarloGraphApp.java

	/**
	 * Configure time range settings through the UI
	 */
	public void configureTimeRange() {
		if (checkInitialization()) {
			controller.configureTimeRange();
		}
	}

	/**
	 * Check if custom time range is currently enabled
	 */
	public boolean isCustomTimeRangeEnabled() {
		if (checkInitialization()) {
			return controller.isCustomTimeRangeEnabled();
		}
		return false;
	}

	/**
	 * Set custom time range programmatically
	 */
	public void setCustomTimeRange(ZonedDateTime start, ZonedDateTime end) {
		if (checkInitialization()) {
			controller.setCustomTimeRange(start, end);
		}
	}

	/**
	 * Set custom time range using Date objects
	 */
	public void setCustomTimeRange(Date start, Date end) {
		if (checkInitialization()) {
			ZonedDateTime startZdt = ZonedDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault());
			ZonedDateTime endZdt = ZonedDateTime.ofInstant(end.toInstant(), ZoneId.systemDefault());
			controller.setCustomTimeRange(startZdt, endZdt);
		}
	}

	/**
	 * Check if the app is properly initialized
	 */
	public boolean isInitialized() {
		return initialized && controller != null;
	}

	public boolean checkIfClosed() {
		if (controller != null) {
			return controller.checkIfClosed();
		}
		return true;
	}

	private MonteCarloDataSourceManager createSimpleDataSourceManager() {
		try {
			ConsoleLogger logger = new ConsoleLoggerImpl();
			StockDataProviderFactory providerFactory = createSimpleProviderFactory();
			return new MonteCarloDataSourceManager(providerFactory, logger);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create data source manager: " + e.getMessage(), e);
		}
	}

	private StockDataProviderFactory createSimpleProviderFactory() {
		OkHttpClient client = new OkHttpClient();
		String apiKey = "PK4UOZQDJJZ6WBAU52XM";
		String alpacaApiSecret = "Fag4ha2D58VyL0okwXgBHD1IvhoptmI2KiacMaNG";
		String polygonApiKey = "fMCQBX5yvQZL_s6Zi1r9iKBkMkNLzNcw";

		return new StockDataProviderFactory(client, apiKey, alpacaApiSecret, polygonApiKey);
	}

	public static void main(String[] args) {
		System.out.println("Starting Monte Carlo Graph App...");

		SwingUtilities.invokeLater(() -> {
			try {
				List<String> symbols = Arrays.asList("NVDA", "AAPL", "MSFT");
				Set<String> topList = new HashSet<>(Arrays.asList("NVDA", "AAPL"));
				MonteCarloGraphApp app = new MonteCarloGraphApp(symbols, "Sample Title", topList);

				// Check initialization status
				javax.swing.Timer timer = new javax.swing.Timer(1000, e -> {
					if (app.isInitialized()) {
						System.out.println("✓ Monte Carlo Graph App initialized successfully!");
						((javax.swing.Timer) e.getSource()).stop();

						// Test adding symbols after initialization
						List<String> newSymbols = app.addSymbols("TSLA, GOOGL");
						System.out.println("✓ Added symbols: " + newSymbols);
					}
				});
				timer.start();

			} catch (Exception e) {
				e.printStackTrace();
				javax.swing.JOptionPane.showMessageDialog(null,
						"Failed to initialize Monte Carlo Graph: " + e.getMessage(), "Initialization Error",
						javax.swing.JOptionPane.ERROR_MESSAGE);
			}
		});

		// Keep the main thread alive for a while to see initialization
		try {
			Thread.sleep(10000); // Wait 10 seconds
			System.out.println("Main method completed.");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	// Public methods for external control

	/**
	 * Update all symbols in the graph
	 */
	public void updateSymbols(List<String> symbols) {
		if (!checkInitialization())
			return;
		controller.updateSymbols(symbols);
	}

	/**
	 * Update all symbols in the graph
	 */
	public void toggleTopList() {
		if (!checkInitialization())
			return;
		controller.toggleTopList();
	}

	/**
	 * Switch to a different data source
	 */
	public void switchDataSource(String sourceName) {
		if (!checkInitialization())
			return;
		controller.switchDataSource(sourceName);
	}

	/**
	 * Add symbols to the graph after checking if they exist
	 * 
	 * @param symbolsInput Comma-separated string of symbols or List of symbols
	 * @return List of symbols that were successfully added
	 */
	public List<String> addSymbols(Object symbolsInput) {
		if (!checkInitialization())
			return new ArrayList<>();

		List<String> symbolsToAdd = parseSymbolsInput(symbolsInput);
		List<String> validSymbols = validateSymbols(symbolsToAdd);

		if (!validSymbols.isEmpty()) {
			controller.addSymbols(validSymbols);
		}

		return validSymbols;
	}

	/**
	 * Add a single symbol after checking if it exists
	 * 
	 * @param symbol The symbol to add
	 * @return true if symbol was added, false if it doesn't exist or is invalid
	 */
	public boolean addSymbol(String symbol) {
		if (!checkInitialization())
			return false;

		if (symbol == null || symbol.trim().isEmpty()) {
			return false;
		}

		List<String> validSymbols = validateSymbols(Arrays.asList(symbol.trim().toUpperCase()));
		if (!validSymbols.isEmpty()) {
			controller.addSymbols(validSymbols);
			return true;
		}
		return false;
	}

	/**
	 * Check if symbols exist in the data source
	 * 
	 * @param symbolsInput Comma-separated string of symbols or List of symbols
	 * @return List of symbols that exist and have data
	 */
	public List<String> checkSymbolsExist(Object symbolsInput) {
		if (!checkInitialization())
			return new ArrayList<>();

		List<String> symbolsToCheck = parseSymbolsInput(symbolsInput);
		return validateSymbols(symbolsToCheck);
	}

	/**
	 * Remove symbols from the graph
	 * 
	 * @param symbolsInput Comma-separated string of symbols or List of symbols to
	 *                     remove
	 */
	public void removeSymbols(Object symbolsInput) {
		if (!checkInitialization())
			return;

		List<String> symbolsToRemove = parseSymbolsInput(symbolsInput);
		controller.removeSymbols(symbolsToRemove);
	}

	/**
	 * Get the current list of symbols in the graph
	 * 
	 * @return List of current symbols
	 */
	public List<String> getCurrentSymbols() {
		if (!checkInitialization())
			return new ArrayList<>();
		return controller.getCurrentSymbols();
	}

	/**
	 * Close the application
	 */
	public void close() {
		if (controller != null) {
			// Add any cleanup logic here if needed
			System.out.println("Monte Carlo Graph App closed");
		}
		initialized = false;
	}

	// TopList methods
	public Set<String> getTopList() {
		return new HashSet<>(topList);
	}

	public void setTopList(Set<String> topList) {
		this.topList = topList != null ? new HashSet<>(topList) : new HashSet<>();
		if (controller != null) {
			controller.setTopList(this.topList);
		}
	}

	// Private helper methods

	private boolean checkInitialization() {
		if (!initialized || controller == null) {
			System.err.println("MonteCarloGraphApp not initialized. Call initialize() first.");
			return false;
		}
		return true;
	}

	private List<String> parseSymbolsInput(Object symbolsInput) {
		List<String> symbols = new ArrayList<>();

		if (symbolsInput instanceof String) {
			String inputStr = ((String) symbolsInput).trim();
			if (!inputStr.isEmpty()) {
				String[] symbolArray = inputStr.split(",");
				for (String symbol : symbolArray) {
					String cleanSymbol = symbol.trim().toUpperCase();
					if (!cleanSymbol.isEmpty()) {
						symbols.add(cleanSymbol);
					}
				}
			}
		} else if (symbolsInput instanceof List) {
			@SuppressWarnings("unchecked")
			List<String> inputList = (List<String>) symbolsInput;
			for (String symbol : inputList) {
				if (symbol != null) {
					String cleanSymbol = symbol.trim().toUpperCase();
					if (!cleanSymbol.isEmpty()) {
						symbols.add(cleanSymbol);
					}
				}
			}
		}

		return symbols;
	}

	private List<String> validateSymbols(List<String> symbols) {
		List<String> validSymbols = new ArrayList<>();
		MonteCarloDataSourceManager dataSourceManager = controller.getDataSourceManager();

		for (String symbol : symbols) {
			if (isSymbolValid(symbol, dataSourceManager)) {
				validSymbols.add(symbol);
			}
		}

		return validSymbols;
	}

	private boolean isSymbolValid(String symbol, MonteCarloDataSourceManager dataSourceManager) {
		// Skip if symbol is null or empty
		if (symbol == null || symbol.trim().isEmpty()) {
			return false;
		}

		String cleanSymbol = symbol.trim().toUpperCase();

		// Basic symbol format validation
		if (!isValidSymbolFormat(cleanSymbol)) {
			System.out.println("Invalid symbol format: " + cleanSymbol);
			return false;
		}

		// Check if symbol has data by trying to fetch a small amount of historical data
		try {
			ZonedDateTime end = ZonedDateTime.now();
			ZonedDateTime start = end.minusDays(1); // Only check last day

			// Use the current data source to check if symbol exists
			List<String> testSymbols = Arrays.asList(cleanSymbol);
			var result = dataSourceManager.fetchData(testSymbols, start, end, initialUseCustomTimeRange);

			boolean hasData = result.containsKey(cleanSymbol) && !result.get(cleanSymbol).isEmpty();

			if (hasData) {
				System.out.println("Symbol " + cleanSymbol + " has data available");
				return true;
			} else {
				System.out.println("No data found for symbol: " + cleanSymbol);
				return false;
			}

		} catch (Exception e) {
			System.out.println("Error checking symbol " + cleanSymbol + ": " + e.getMessage());
			return false;
		}
	}

	private boolean isValidSymbolFormat(String symbol) {
		// Basic symbol format validation
		// Symbols should be 1-5 characters, alphanumeric, and may include dots or
		// dashes
		if (symbol.length() < 1 || symbol.length() > 10) {
			return false;
		}

		// Allow letters, numbers, dots, and dashes
		return symbol.matches("^[A-Z0-9.-]+$");
	}

	/**
	 * Get the controller for window listener attachment
	 */
	public MonteCarloGraphController getController() {
		return controller;
	}

	/**
	 * Add window listener to the application
	 */
	public void addWindowListener(java.awt.event.WindowListener listener) {
		if (controller != null) {
			controller.addWindowListener(listener);
		}
	}

	/**
	 * Remove window listener from the application
	 */
	public void removeWindowListener(java.awt.event.WindowListener listener) {
		if (controller != null) {
			// Note: MonteCarloGraphController would need a removeWindowListener method
			// For now, we'll handle this through the UI directly if needed
		}
	}

	/**
	 * Remove window listener from the application
	 */
	public void showGraph() {
		if (controller != null) {
			// Note: MonteCarloGraphController would need a removeWindowListener method
			// For now, we'll handle this through the UI directly if needed
			controller.showGraph();
		}
	}

	// Primary symbols methods
	public void setPrimarySymbols(Set<String> primarySymbols) {
		this.primarySymbols = primarySymbols != null ? new HashSet<>(primarySymbols) : new HashSet<>();
		if (controller != null) {
			controller.setPrimarySymbols(this.primarySymbols);
		}
	}

	public Set<String> getPrimarySymbols() {
		return new HashSet<>(primarySymbols);
	}

	private static class ConsoleLoggerImpl implements ConsoleLogger {
		public void log(String message) {
			System.out.println(message);
		}

		public void log(String message, Throwable throwable) {
			System.out.println(message + ": " + throwable.getMessage());
			throwable.printStackTrace();
		}

		public void error(String message) {
			System.err.println("ERROR: " + message);
		}

		public void error(String message, Throwable throwable) {
			System.err.println("ERROR: " + message + ": " + throwable.getMessage());
			throwable.printStackTrace();
		}

		public void info(String message) {
			System.out.println("INFO: " + message);
		}

		public void debug(String message) {
			System.out.println("DEBUG: " + message);
		}
	}

	public void frameToFront() {
		controller.frameToFront();
	}

	public void updateTitle(String newTitle) {
		controller.updateTitle(newTitle);
	}

	public String getTitle() {
		return controller.getTitle();
	}

	/**
	 * Update custom time range and refresh chart immediately
	 */
	public void updateCustomTimeRange(ZonedDateTime start, ZonedDateTime end, String timeframe) {
		if (checkInitialization()) {
			controller.updateCustomTimeRange(start, end, timeframe);
		}
	}

	/**
	 * Update custom time range using Date objects and refresh chart immediately
	 */
	public void updateCustomTimeRange(Date start, Date end, String timeframe) {
		if (checkInitialization()) {
			ZonedDateTime startZdt = ZonedDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault());
			ZonedDateTime endZdt = ZonedDateTime.ofInstant(end.toInstant(), ZoneId.systemDefault());
			controller.updateCustomTimeRange(startZdt, endZdt, timeframe);
		}
	}

	/**
	 * Update current time mode with specific time range and refresh chart
	 * immediately
	 */
	public void updateCurrentTimeMode(String timeRange) {
		if (checkInitialization()) {
			controller.updateCurrentTimeMode(timeRange);
		}
	}

	/**
	 * Force refresh the chart with current time range settings
	 */
	public void forceRefreshChart() {
		if (checkInitialization()) {
			controller.forceRefreshChart();
		}
	}

	/**
	 * Get current time range settings
	 */
	public Map<String, Object> getCurrentTimeRangeSettings() {
		if (checkInitialization()) {
			return controller.getCurrentTimeRangeSettings();
		}
		return new HashMap<>();
	}

	public Map<String, Object> getMonteCarloConfig() {
		return monteCarloConfig;
	}

	public void setMonteCarloConfig(Map<String, Object> monteCarloConfig) {
		this.monteCarloConfig = monteCarloConfig;
		this.controller.setMonteCarloConfig(monteCarloConfig);
	}

	public Map<String, PriceData> getPriceDataMap() {
		return priceDataMap;
	}

	public void setPriceDataMap(Map<String, PriceData> priceDataMap) {
		this.priceDataMap = priceDataMap;
		this.controller.setPriceDataMap(priceDataMap);
	}

	public List<String> getSelectedColumns() {
		return selectedColumns;
	}

	public void setSelectedColumns(List<String> selectedColumns) {
		this.selectedColumns = selectedColumns;
	}

	public Map<String, Object> getGraphSettings() {
		return graphSettings;
	}

	public void setGraphSettings(Map<String, Object> graphSettings) {
		this.graphSettings = graphSettings;
	}

	
	
}