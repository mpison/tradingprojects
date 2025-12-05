package com.quantlabs.stockApp.indicator.management;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.json.JSONArray;
import org.json.JSONObject;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.model.WatchlistData;

public class IndicatorsManagementApp extends JFrame {
	private DefaultListModel<StrategyConfig> strategyListModel;
	private JList<StrategyConfig> strategyList;
	private StrategyExecutionService executionService;
	private ConsoleLogger logger;
	private JTextArea logTextArea;
	private JTextArea detailsTextArea;

	// Add these fields to the class
	private ConfigurationExporter configurationExporter;
	private ConfigurationImporter configurationImporter;

	// Global custom indicators storage
	private Set<CustomIndicator> globalCustomIndicators = new HashSet<>();
	// Global watchlists storage
	private Map<String, WatchlistData> globalWatchlists = new HashMap<>();

	private static final String CUSTOM_INDICATORS_FILE = "custom_indicators.dat";
	private static final String STRATEGIES_FILE = "strategies.dat";
	private static final String WATCHLISTS_FILE = "watchlists.dat";

	private static final String[] TIMEFRAMES = { "1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min" };
	
	private boolean isSaved = false;

	// Menu components
	private JMenuBar menuBar;
	private JMenu fileMenu, strategyMenu, toolsMenu, viewMenu;

	// a field to store strategy configurations
	private Map<String, Object> strategyConfigurations = new HashMap<>();

	// Initialize in constructor or setup method
	public IndicatorsManagementApp(StrategyExecutionService executionService, ConsoleLogger logger) {
		this.executionService = executionService;
		this.logger = logger;
		this.configurationExporter = new ConfigurationExporter();
		this.configurationImporter = new ConfigurationImporter();
		this.globalWatchlists = new HashMap<>(); // Initialize here
		initializeUI();
		setupData();
	}

	private void initializeUI() {
		setTitle("Indicators Management System - Independent Timeframe Indicators");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1200, 800);
		setLocationRelativeTo(null);

		// Add window listener to save data on close
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				saveAllData();
			}
		});

		// Create menu bar
		createMenuBar();

		// Main panel with border layout
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Strategy list on left
		strategyListModel = new DefaultListModel<>();
		strategyList = new JList<>(strategyListModel);
		strategyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		strategyList.setCellRenderer(new StrategyListRenderer());

		JScrollPane listScrollPane = new JScrollPane(strategyList);
		listScrollPane.setPreferredSize(new Dimension(350, 0));
		listScrollPane.setBorder(BorderFactory.createTitledBorder("Strategies (Multiple Selection Enabled)"));

		// Quick action buttons panel (minimal - only essential actions)
		JPanel quickActionPanel = createQuickActionPanel();

		// Details and log panel on right
		JPanel rightPanel = createRightPanel();

		// Add components to main panel
		mainPanel.add(quickActionPanel, BorderLayout.NORTH);
		mainPanel.add(listScrollPane, BorderLayout.WEST);
		mainPanel.add(rightPanel, BorderLayout.CENTER);

		add(mainPanel);

		// List selection listener
		strategyList.addListSelectionListener(e -> showStrategyDetails());
	}

	private void createMenuBar() {
		menuBar = new JMenuBar();

		// File Menu
		fileMenu = new JMenu("File");

		JMenuItem newStrategyItem = new JMenuItem("New Strategy");
		JMenuItem importItem = new JMenuItem("Import Strategies...");
		JMenuItem exportItem = new JMenuItem("Export Strategies...");
		JMenuItem exitItem = new JMenuItem("Exit");

		newStrategyItem.addActionListener(e -> showStrategyDialog(null));
		importItem.addActionListener(e -> importStrategies());
		exportItem.addActionListener(e -> exportStrategies());
		exitItem.addActionListener(e -> exitApplication());

		fileMenu.add(newStrategyItem);
		fileMenu.addSeparator();
		fileMenu.add(importItem);
		fileMenu.add(exportItem);
		fileMenu.addSeparator();
		fileMenu.add(exitItem);

		// Strategy Menu
		strategyMenu = new JMenu("Strategy");

		JMenuItem editStrategyItem = new JMenuItem("Edit Selected");
		JMenuItem duplicateStrategyItem = new JMenuItem("Duplicate Selected");
		JMenuItem deleteStrategyItem = new JMenuItem("Delete Selected");

		JMenu bulkMenu = new JMenu("Bulk Operations");
		JMenuItem enableSelectedItem = new JMenuItem("Enable Selected");
		JMenuItem disableSelectedItem = new JMenuItem("Disable Selected");
		JMenuItem toggleEnabledItem = new JMenuItem("Toggle Enabled Status");
		JMenuItem runSelectedItem = new JMenuItem("Run Selected Strategies");

		editStrategyItem.addActionListener(e -> editSelectedStrategy());
		duplicateStrategyItem.addActionListener(e -> duplicateSelectedStrategies());
		deleteStrategyItem.addActionListener(e -> deleteSelectedStrategies());

		enableSelectedItem.addActionListener(e -> enableSelectedStrategies());
		disableSelectedItem.addActionListener(e -> disableSelectedStrategies());
		toggleEnabledItem.addActionListener(e -> toggleSelectedStrategies());
		runSelectedItem.addActionListener(e -> runSelectedStrategies());

		bulkMenu.add(enableSelectedItem);
		bulkMenu.add(disableSelectedItem);
		bulkMenu.add(toggleEnabledItem);
		bulkMenu.addSeparator();
		bulkMenu.add(runSelectedItem);

		strategyMenu.add(editStrategyItem);
		strategyMenu.add(duplicateStrategyItem);
		strategyMenu.add(deleteStrategyItem);
		strategyMenu.addSeparator();
		strategyMenu.add(bulkMenu);

		// Tools Menu
		toolsMenu = new JMenu("Tools");

		JMenuItem manageIndicatorsItem = new JMenuItem("Manage Custom Indicators");
		JMenuItem manageWatchlistsItem = new JMenuItem("Manage Watchlists");
		JMenuItem validateItem = new JMenuItem("Validate All Strategies");
		JMenuItem refreshItem = new JMenuItem("Refresh Data");

		manageIndicatorsItem.addActionListener(e -> manageGlobalCustomIndicators());
		manageWatchlistsItem.addActionListener(e -> manageWatchlists());
		validateItem.addActionListener(e -> validateStrategies());
		refreshItem.addActionListener(e -> refreshData());

		toolsMenu.add(manageIndicatorsItem);
		toolsMenu.add(manageWatchlistsItem);
		toolsMenu.addSeparator();
		toolsMenu.add(validateItem);
		toolsMenu.add(refreshItem);

		// View Menu
		viewMenu = new JMenu("View");

		JMenuItem clearLogItem = new JMenuItem("Clear Log");
		JMenuItem saveLogItem = new JMenuItem("Save Log As...");
		JMenuItem showOverviewItem = new JMenuItem("Show Application Overview");

		clearLogItem.addActionListener(e -> logTextArea.setText(""));
		saveLogItem.addActionListener(e -> saveLogToFile());
		showOverviewItem.addActionListener(e -> showApplicationOverview());

		viewMenu.add(clearLogItem);
		viewMenu.add(saveLogItem);
		viewMenu.addSeparator();
		viewMenu.add(showOverviewItem);

		// Add menus to menu bar
		menuBar.add(fileMenu);
		menuBar.add(strategyMenu);
		menuBar.add(toolsMenu);
		menuBar.add(viewMenu);

		// Set the menu bar
		setJMenuBar(menuBar);
	}

	private JPanel createQuickActionPanel() {
		JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		quickPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Only essential quick actions
		JButton addButton = new JButton("Add Strategy");
		JButton runButton = new JButton("Run Selected");
		JButton validateButton = new JButton("Validate");
		JButton refreshButton = new JButton("Refresh");

		addButton.addActionListener(e -> showStrategyDialog(null));
		runButton.addActionListener(e -> runSelectedStrategies());
		validateButton.addActionListener(e -> validateStrategies());
		refreshButton.addActionListener(e -> refreshData());

		// Tooltips for quick actions
		addButton.setToolTipText("Create a new strategy (Ctrl+N)");
		runButton.setToolTipText("Run selected enabled strategies");
		validateButton.setToolTipText("Validate all strategies");
		refreshButton.setToolTipText("Refresh application data");

		quickPanel.add(addButton);
		quickPanel.add(runButton);
		quickPanel.add(validateButton);
		quickPanel.add(refreshButton);

		return quickPanel;
	}

	private JPanel createRightPanel() {
		JPanel rightPanel = new JPanel(new GridLayout(2, 1, 5, 5));

		// Strategy details panel
		JPanel detailsPanel = new JPanel(new BorderLayout());
		detailsPanel.setBorder(BorderFactory.createTitledBorder("Strategy Details"));

		detailsTextArea = new JTextArea();
		detailsTextArea.setEditable(false);
		detailsTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		JScrollPane detailsScrollPane = new JScrollPane(detailsTextArea);
		detailsPanel.add(detailsScrollPane, BorderLayout.CENTER);

		// Log panel
		JPanel logPanel = new JPanel(new BorderLayout());
		logPanel.setBorder(BorderFactory.createTitledBorder("Execution Log"));

		logTextArea = new JTextArea();
		logTextArea.setEditable(false);
		logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
		logTextArea.setBackground(new Color(240, 240, 240));
		JScrollPane logScrollPane = new JScrollPane(logTextArea);

		// Minimal log controls
		JPanel logControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton clearLogButton = new JButton("Clear");
		clearLogButton.addActionListener(e -> logTextArea.setText(""));
		logControlPanel.add(clearLogButton);

		logPanel.add(logScrollPane, BorderLayout.CENTER);
		logPanel.add(logControlPanel, BorderLayout.SOUTH);

		rightPanel.add(detailsPanel);
		rightPanel.add(logPanel);

		return rightPanel;
	}

	private void setupData() {
		// Load saved data
		loadAllData();

		// Add sample strategies if no strategies exist
		if (strategyListModel.isEmpty()) {
			addSampleStrategies();
		}

		// Add sample watchlists if none exist
		if (globalWatchlists.isEmpty()) {
			addSampleWatchlists();
		}

		// Log available indicators
		logToUI("🚀 Application initialized successfully");
		logToUI("   Available indicators: " + executionService.getStrategyMap().size());
		logToUI("   Available timeframes: " + String.join(", ", TIMEFRAMES));
		logToUI("   Global custom indicators: " + globalCustomIndicators.size());
		logToUI("   Strategies loaded: " + strategyListModel.size());
		logToUI("   Watchlists loaded: " + globalWatchlists.size());
		logToUI("   Enabled strategies: " + getEnabledStrategyCount() + "/" + strategyListModel.size());
		logToUI("   Use the menu bar for complete functionality");
	}

	// Data Persistence Methods
	private void saveAllData() {
		//saveStrategies();
		//saveCustomIndicators();
		//saveWatchlists();
		logToUI("💾 All data saved successfully");
	}

	private void loadAllData() {
		loadStrategies();
		loadCustomIndicators();
		loadWatchlists();
		logToUI("📂 All data loaded successfully");
	}

	private void saveStrategies() {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STRATEGIES_FILE))) {
			List<StrategyConfig> strategies = new ArrayList<>();
			for (int i = 0; i < strategyListModel.size(); i++) {
				strategies.add(strategyListModel.get(i));
			}
			oos.writeObject(strategies);
			logToUI("💾 Strategies saved: " + strategies.size());
		} catch (IOException e) {
			logToUI("❌ Error saving strategies: " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private void loadStrategies() {
		File file = new File(STRATEGIES_FILE);
		if (file.exists()) {
			try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
				List<StrategyConfig> strategies = (List<StrategyConfig>) ois.readObject();
				strategyListModel.clear();
				for (StrategyConfig strategy : strategies) {
					strategyListModel.addElement(strategy);
				}
				logToUI("📂 Strategies loaded: " + strategies.size());
			} catch (IOException | ClassNotFoundException e) {
				logToUI("❌ Error loading strategies: " + e.getMessage());
			}
		} else {
			logToUI("ℹ️ No saved strategies found - starting with empty list");
		}
	}

	private void saveCustomIndicators() {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CUSTOM_INDICATORS_FILE))) {
			oos.writeObject(new ArrayList<>(globalCustomIndicators));
			logToUI("💾 Custom indicators saved: " + globalCustomIndicators.size());
		} catch (IOException e) {
			logToUI("❌ Error saving custom indicators: " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private void loadCustomIndicators() {
		File file = new File(CUSTOM_INDICATORS_FILE);
		if (file.exists()) {
			try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
				List<CustomIndicator> indicators = (List<CustomIndicator>) ois.readObject();
				globalCustomIndicators.clear();
				globalCustomIndicators.addAll(indicators);
				logToUI("📂 Custom indicators loaded: " + indicators.size());
			} catch (IOException | ClassNotFoundException e) {
				logToUI("❌ Error loading custom indicators: " + e.getMessage());
			}
		} else {
			logToUI("ℹ️ No saved custom indicators found - starting with empty list");
		}
	}

	private void saveWatchlists() {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(WATCHLISTS_FILE))) {
			oos.writeObject(new HashMap<>(globalWatchlists));
			logToUI("💾 Watchlists saved: " + globalWatchlists.size());
		} catch (IOException e) {
			logToUI("❌ Error saving watchlists: " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private void loadWatchlists() {
	    File file = new File(WATCHLISTS_FILE);
	    if (file.exists()) {
	        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
	            Object loadedData = ois.readObject();
	            
	            if (loadedData instanceof Map) {
	                globalWatchlists = convertToWatchlistData((Map<String, ?>) loadedData);
	            }
	            logToUI("📂 Watchlists loaded: " + globalWatchlists.size());
	        } catch (IOException | ClassNotFoundException e) {
	            logToUI("❌ Error loading watchlists: " + e.getMessage());
	            globalWatchlists = new HashMap<>();
	        }
	    } else {
	        logToUI("ℹ️ No saved watchlists found - starting with empty list");
	        globalWatchlists = new HashMap<>();
	    }
	}

	private void addSampleStrategies() {
		// Sample strategy 1: Multi-Timeframe with various indicators
		StrategyConfig config1 = new StrategyConfig();
		config1.setName("Multi-Timeframe Trend Analysis");
		config1.setEnabled(true);
		config1.setAlarmEnabled(true);

		// Different indicators for different timeframes
		config1.addTimeframeWithIndicators("1W", new HashSet<>(Arrays.asList("MACD", "Trend")));
		config1.addTimeframeWithIndicators("1D", new HashSet<>(Arrays.asList("VWAP", "RSI", "PSAR(0.01)")));
		config1.addTimeframeWithIndicators("4H", new HashSet<>(Arrays.asList("HeikenAshi", "Volume20MA")));

		// Set a watchlist for this strategy
		config1.setWatchlistName("Technology Stocks");

		strategyListModel.addElement(config1);

		// Sample strategy 2: Daily focused strategy
		StrategyConfig config2 = new StrategyConfig();
		config2.setName("Daily Breakout Scanner");
		config2.setEnabled(true);
		config2.setAlarmEnabled(false);

		config2.addTimeframeWithIndicators("1D",
				new HashSet<>(Arrays.asList("HighestCloseOpen", "VWAP", "Volume20MA")));
		config2.addTimeframeWithIndicators("4H", new HashSet<>(Arrays.asList("PSAR(0.05)", "RSI")));

		strategyListModel.addElement(config2);

		// Sample strategy 3: Disabled strategy with watchlist
		StrategyConfig config3 = new StrategyConfig();
		config3.setName("Tech Stocks Analysis");
		config3.setEnabled(false);
		config3.setAlarmEnabled(true);
		config3.setSymbolExclusive(true);
		config3.setExclusiveSymbols(new HashSet<>(Arrays.asList("AAPL", "GOOGL", "MSFT")));

		config3.addTimeframeWithIndicators("1D", new HashSet<>(Arrays.asList("MACD", "VWAP")));
		config3.addTimeframeWithIndicators("1H", new HashSet<>(Arrays.asList("RSI", "PSAR(0.01)")));

		strategyListModel.addElement(config3);

		logToUI("📋 Created " + strategyListModel.size() + " sample strategies");
	}

	private void addSampleWatchlists() {
		// Sample watchlists
		Set<String> techStocks = new HashSet<>(Arrays.asList("AAPL", "GOOGL", "MSFT", "AMZN", "META", "NVDA", "TSLA"));
		Set<String> energyStocks = new HashSet<>(Arrays.asList("XOM", "CVX", "COP", "SLB", "EOG"));
		Set<String> financialStocks = new HashSet<>(Arrays.asList("JPM", "BAC", "WFC", "GS", "MS"));

		globalWatchlists.put("Technology Stocks", new WatchlistData(techStocks, "AAPL"));
		globalWatchlists.put("Energy Sector", new WatchlistData(energyStocks, "XOM"));
		globalWatchlists.put("Financial Services", new WatchlistData(financialStocks, "JPM"));

		logToUI("📋 Created " + globalWatchlists.size() + " sample watchlists");
	}

	// Application exit method
	private void exitApplication() {
		int result = JOptionPane.showConfirmDialog(this,
				"Are you sure you want to exit?\nAll data will be saved automatically.", "Confirm Exit",
				JOptionPane.YES_NO_OPTION);

		if (result == JOptionPane.YES_OPTION) {
			saveAllData();
			System.exit(0);
		}
	}

	// Watchlist Management Methods
	private void manageWatchlists() {
		WatchlistManagerDialog dialog = new WatchlistManagerDialog(this, globalWatchlists);
		dialog.setVisible(true);

		if (dialog.isSaved()) {
			globalWatchlists.clear();
			globalWatchlists.putAll(dialog.getWatchlists());
			saveWatchlists();
			logToUI("✅ Watchlists updated: " + globalWatchlists.size() + " watchlists available");

			// Update strategy list to reflect any watchlist changes
			strategyList.repaint();
			showStrategyDetails();
		}
	}

	// Bulk Strategy Operations
	private void enableSelectedStrategies() {
		List<StrategyConfig> selectedStrategies = strategyList.getSelectedValuesList();
		if (selectedStrategies.isEmpty()) {
			showWarning("Please select one or more strategies to enable.");
			return;
		}

		int count = 0;
		for (StrategyConfig strategy : selectedStrategies) {
			if (!strategy.isEnabled()) {
				strategy.setEnabled(true);
				count++;
			}
		}

		if (count > 0) {
			saveStrategies();
			strategyList.repaint();
			logToUI("✅ Enabled " + count + " strategies");
			showStrategyDetails();
		} else {
			logToUI("ℹ️ All selected strategies are already enabled");
		}
	}

	private void disableSelectedStrategies() {
		List<StrategyConfig> selectedStrategies = strategyList.getSelectedValuesList();
		if (selectedStrategies.isEmpty()) {
			showWarning("Please select one or more strategies to disable.");
			return;
		}

		int count = 0;
		for (StrategyConfig strategy : selectedStrategies) {
			if (strategy.isEnabled()) {
				strategy.setEnabled(false);
				count++;
			}
		}

		if (count > 0) {
			saveStrategies();
			strategyList.repaint();
			logToUI("⏸️ Disabled " + count + " strategies");
			showStrategyDetails();
		} else {
			logToUI("ℹ️ All selected strategies are already disabled");
		}
	}

	private void toggleSelectedStrategies() {
		List<StrategyConfig> selectedStrategies = strategyList.getSelectedValuesList();
		if (selectedStrategies.isEmpty()) {
			showWarning("Please select one or more strategies to toggle.");
			return;
		}

		int enabledCount = 0;
		int disabledCount = 0;

		for (StrategyConfig strategy : selectedStrategies) {
			if (strategy.isEnabled()) {
				strategy.setEnabled(false);
				disabledCount++;
			} else {
				strategy.setEnabled(true);
				enabledCount++;
			}
		}

		saveStrategies();
		strategyList.repaint();
		logToUI("🔄 Toggled " + selectedStrategies.size() + " strategies");
		logToUI("   Enabled: " + enabledCount + ", Disabled: " + disabledCount);
		showStrategyDetails();
	}

	private void runSelectedStrategies() {
		List<StrategyConfig> selectedStrategies = strategyList.getSelectedValuesList();
		if (selectedStrategies.isEmpty()) {
			showWarning("Please select one or more strategies to run.");
			return;
		}

		// Filter out disabled strategies
		List<StrategyConfig> enabledStrategies = selectedStrategies.stream().filter(StrategyConfig::isEnabled)
				.collect(Collectors.toList());

		if (enabledStrategies.isEmpty()) {
			showWarning("No enabled strategies selected. Please enable strategies before running.");
			return;
		}

		logToUI("🚀 BATCH EXECUTION: " + enabledStrategies.size() + " STRATEGIES");
		logToUI("=".repeat(50));

		for (int i = 0; i < enabledStrategies.size(); i++) {
			StrategyConfig strategy = enabledStrategies.get(i);
			logToUI("\n" + (i + 1) + "/" + enabledStrategies.size() + " Executing: " + strategy.getName());
			executeSingleStrategy(strategy);
		}

		logToUI("\n✅ Batch execution completed: " + enabledStrategies.size() + " strategies processed");

		JOptionPane.showMessageDialog(this,
				"Batch execution completed!\n\n" + "Processed: " + enabledStrategies.size() + " strategies\n"
						+ "Check the log for detailed results.",
				"Batch Execution Complete", JOptionPane.INFORMATION_MESSAGE);
	}

	private void executeSingleStrategy(StrategyConfig strategy) {
		// Log strategy configuration
		logToUI("  📊 Strategy: " + strategy.getName());
		logToUI("  🔔 Alarm: " + (strategy.isAlarmEnabled() ? "ENABLED" : "DISABLED"));

		// Log watchlist information if set
		if (strategy.getWatchlistName() != null && !strategy.getWatchlistName().isEmpty()) {
			WatchlistData watchlistSymbols = globalWatchlists.get(strategy.getWatchlistName());
			if (watchlistSymbols != null) {
				logToUI("  📋 Watchlist: " + strategy.getWatchlistName() + " (" + watchlistSymbols.size()
						+ " symbols)");
			} else {
				logToUI("  ⚠️  Watchlist not found: " + strategy.getWatchlistName());
			}
		} else if (strategy.isSymbolExclusive() && !strategy.getExclusiveSymbols().isEmpty()) {
			logToUI("  🔒 Exclusive Symbols: " + strategy.getExclusiveSymbols().size() + " symbols");
		} else {
			logToUI("  🌐 All Symbols: Applying to all available symbols");
		}

		// Execute for each timeframe
		for (String timeframe : strategy.getTimeframes()) {
			Set<String> indicators = strategy.getIndicatorsForTimeframe(timeframe);
			Set<CustomIndicator> customIndicators = strategy.getCustomIndicatorsForTimeframe(timeframe);

			if (indicators.isEmpty() && customIndicators.isEmpty()) {
				continue;
			}

			logToUI("  ⏰ " + timeframe + ": " + indicators.size() + " standard, " + customIndicators.size()
					+ " custom indicators");

			// Simulate execution
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}

		// Simulate alarm check
		if (strategy.isAlarmEnabled()) {
			try {
				Thread.sleep(50);
				boolean alarmTriggered = Math.random() > 0.8; // 20% chance
				if (alarmTriggered) {
					logToUI("  🔔 ALARM TRIGGERED for " + strategy.getName());
				} else {
					logToUI("  ✅ No alarm conditions detected");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		logToUI("  ✅ Completed: " + strategy.getName());
	}

	// Individual Strategy Operations
	private void editSelectedStrategy() {
		List<StrategyConfig> selectedStrategies = strategyList.getSelectedValuesList();
		if (selectedStrategies.isEmpty()) {
			showWarning("Please select a strategy to edit.");
			return;
		}

		if (selectedStrategies.size() > 1) {
			showWarning("Please select only one strategy to edit.");
			return;
		}

		showStrategyDialog(selectedStrategies.get(0));
	}

	private void deleteSelectedStrategies() {
		List<StrategyConfig> selectedStrategies = strategyList.getSelectedValuesList();
		if (selectedStrategies.isEmpty()) {
			showWarning("Please select one or more strategies to delete.");
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + selectedStrategies.size()
				+ " strategies?\n" + "This action cannot be undone.", "Confirm Bulk Delete", JOptionPane.YES_NO_OPTION);

		if (confirm == JOptionPane.YES_OPTION) {
			for (StrategyConfig strategy : selectedStrategies) {
				strategyListModel.removeElement(strategy);
			}

			saveStrategies();
			logToUI("🗑️ Deleted " + selectedStrategies.size() + " strategies");
			showStrategyDetails();
		}
	}

	private void duplicateSelectedStrategies() {
		List<StrategyConfig> selectedStrategies = strategyList.getSelectedValuesList();
		if (selectedStrategies.isEmpty()) {
			showWarning("Please select one or more strategies to duplicate.");
			return;
		}

		int count = 0;
		for (StrategyConfig original : selectedStrategies) {
			StrategyConfig duplicate = original.duplicate();
			duplicate.setName(original.getName() + " (Copy " + (count + 1) + ")");
			strategyListModel.addElement(duplicate);
			count++;
		}

		saveStrategies();
		logToUI("📋 Duplicated " + count + " strategies");
		showStrategyDetails();
	}

	// Helper method to get all existing unique keys
	private Set<String> getAllExistingKeys() {
		Set<String> allKeys = new HashSet<>();
		for (int i = 0; i < strategyListModel.size(); i++) {
			StrategyConfig config = strategyListModel.get(i);
			allKeys.addAll(config.getAllUniqueKeys());
		}
		return allKeys;
	}

	private void showStrategyDialog(StrategyConfig existingConfig) {
		Set<String> existingKeys = getAllExistingKeys();
		if (existingConfig != null) {
			// Remove the current strategy's keys when editing
			existingKeys.removeAll(existingConfig.getAllUniqueKeys());
		}

		StrategyDialog dialog = new StrategyDialog(this, existingConfig, executionService.getStrategyMap().keySet(),
				existingKeys, this);
		dialog.setVisible(true);

		if (dialog.isSaved()) {
			StrategyConfig config = dialog.getStrategyConfig();
			if (existingConfig == null) {
				strategyListModel.addElement(config);
				logToUI("✅ Added new strategy: " + config.getName());
				if (config.getWatchlistName() != null && !config.getWatchlistName().isEmpty()) {
					logToUI("   Watchlist: " + config.getWatchlistName());
				}
			} else {
				int index = strategyListModel.indexOf(existingConfig);
				strategyListModel.set(index, config);
				logToUI("✅ Updated strategy: " + config.getName());
				if (config.getWatchlistName() != null && !config.getWatchlistName().isEmpty()) {
					logToUI("   Watchlist: " + config.getWatchlistName());
				}
			}

			saveStrategies();
			showStrategyDetails();
		}
	}

	private void manageGlobalCustomIndicators() {
		Map<String, Set<CustomIndicator>> allCustomIndicators = new HashMap<>();
		allCustomIndicators.put("global", new HashSet<>(globalCustomIndicators));

		CustomIndicatorsManager manager = new CustomIndicatorsManager(this, allCustomIndicators, this);
		manager.setVisible(true);

		if (manager.isSaved()) {
			Map<String, Set<CustomIndicator>> updatedIndicators = manager.getCustomIndicatorsMap();
			globalCustomIndicators.clear();

			for (Set<CustomIndicator> indicators : updatedIndicators.values()) {
				globalCustomIndicators.addAll(indicators);
			}

			saveCustomIndicators();
			logToUI("✅ Global custom indicators updated: " + globalCustomIndicators.size() + " indicators available");
			showStrategyDetails();
		}
	}

	// Simplified export method
	private void exportStrategies() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Export Configuration to JSON");
		fileChooser.setSelectedFile(new File("indicators_configuration.json"));
		fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files (*.json)", "json"));

		int userSelection = fileChooser.showSaveDialog(this);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToSave = fileChooser.getSelectedFile();

			// Ensure .json extension
			if (!fileToSave.getName().toLowerCase().endsWith(".json")) {
				fileToSave = new File(fileToSave.getAbsolutePath() + ".json");
			}

			// Convert strategy list model to list
			List<StrategyConfig> strategies = new ArrayList<>();
			for (int i = 0; i < strategyListModel.size(); i++) {
				strategies.add(strategyListModel.get(i));
			}

			ConfigurationExporter.ExportResult result = configurationExporter.exportConfiguration(fileToSave,
					strategies, globalCustomIndicators, globalWatchlists, this);

			if (result.isSuccess()) {
				logToUI("💾 " + result.getMessage());
				logToUI("   Strategies: " + result.getStrategyCount());
				logToUI("   Custom Indicators: " + result.getIndicatorCount());
				logToUI("   Watchlists: " + result.getWatchlistCount());

				JOptionPane.showMessageDialog(this,
						"✅ Configuration exported successfully!\n\n" + "Strategies: " + result.getStrategyCount() + "\n"
								+ "Custom Indicators: " + result.getIndicatorCount() + "\n" + "Watchlists: "
								+ result.getWatchlistCount() + "\n" + "File: " + fileToSave.getName(),
						"Export Successful", JOptionPane.INFORMATION_MESSAGE);

			} else {
				logToUI("❌ " + result.getMessage());
				showWarning(result.getMessage());
			}
		}
	}

	// Simplified import method
	private void importStrategies() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Import Configuration from JSON");
		fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files (*.json)", "json"));

		int userSelection = fileChooser.showOpenDialog(this);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToOpen = fileChooser.getSelectedFile();

			ConfigurationImporter.ImportResult result = configurationImporter.importConfiguration(fileToOpen, this);

			if (result.isSuccess()) {
				ConfigurationImporter.ImportData importData = result.getImportData();

				// Update application data
				strategyListModel.clear();
				for (StrategyConfig strategy : importData.getStrategies()) {
					strategyListModel.addElement(strategy);
				}

				globalCustomIndicators.clear();
				globalCustomIndicators.addAll(importData.getCustomIndicators());

				globalWatchlists.clear();
				globalWatchlists.putAll(importData.getWatchlists());

				// Save all data
				saveAllData();

				// Log results
				logToUI("✅ " + result.getMessage());
				logToUI("   Strategies: " + importData.getStrategies().size());
				logToUI("   Custom Indicators: " + importData.getCustomIndicators().size());
				logToUI("   Watchlists: " + importData.getWatchlists().size());
				logToUI("   Original Export: " + importData.getExportDate());

				// Show warnings if any
				if (!importData.getWarnings().isEmpty()) {
					logToUI("⚠️  Import warnings:");
					for (String warning : importData.getWarnings()) {
						logToUI("   - " + warning);
					}
				}

				JOptionPane.showMessageDialog(this,
						"Configuration imported successfully!\n\n" + "Strategies: " + importData.getStrategies().size()
								+ "\n" + "Custom Indicators: " + importData.getCustomIndicators().size() + "\n"
								+ "Watchlists: " + importData.getWatchlists().size() + "\n"
								+ (importData.getWarnings().isEmpty() ? ""
										: "\nSome warnings occurred - check the log for details."),
						"Import Complete", JOptionPane.INFORMATION_MESSAGE);

			} else {
				logToUI("❌ " + result.getMessage());
				showWarning(result.getMessage());
			}
		}
	}

	private void validateStrategies() {
		logToUI("🔍 VALIDATING ALL STRATEGIES");
		logToUI("=".repeat(50));

		Set<String> allKeys = new HashSet<>();
		boolean hasDuplicates = false;
		int validStrategies = 0;
		int totalStrategies = strategyListModel.size();
		int totalTimeframeConfigs = 0;
		int totalCustomIndicators = 0;
		int enabledStrategies = 0;
		int strategiesWithWatchlists = 0;

		for (int i = 0; i < totalStrategies; i++) {
			StrategyConfig config = strategyListModel.get(i);
			Set<String> configKeys = config.getAllUniqueKeys();
			totalTimeframeConfigs += configKeys.size();
			totalCustomIndicators += config.getAllCustomIndicators().size();

			if (config.isEnabled()) {
				enabledStrategies++;
			}

			if (config.getWatchlistName() != null && !config.getWatchlistName().isEmpty()) {
				strategiesWithWatchlists++;
			}

			// Check for duplicates
			for (String key : configKeys) {
				if (allKeys.contains(key)) {
					logToUI("❌ DUPLICATE: " + key);
					hasDuplicates = true;
				} else {
					allKeys.add(key);
				}
			}

			// Validate strategy configuration
			if (config.getName() != null && !config.getName().trim().isEmpty() && !config.getTimeframes().isEmpty()) {
				validStrategies++;

				StringBuilder timeframeDetails = new StringBuilder();
				for (String timeframe : config.getTimeframes()) {
					Set<String> indicators = config.getIndicatorsForTimeframe(timeframe);
					Set<CustomIndicator> customIndicators = config.getCustomIndicatorsForTimeframe(timeframe);
					timeframeDetails.append(timeframe).append("(").append(indicators.size()).append(" standard, ")
							.append(customIndicators.size()).append(" custom) ");
				}

				String statusIcon = config.isEnabled() ? "✅" : "⏸️";
				String watchlistIcon = config.getWatchlistName() != null && !config.getWatchlistName().isEmpty() ? "📋"
						: "";

				logToUI(statusIcon + " " + watchlistIcon + " Valid: " + config.getName() + " [Alarm: "
						+ (config.isAlarmEnabled() ? "ON" : "OFF") + "] " + "[Timeframes: "
						+ timeframeDetails.toString().trim() + "]"
						+ (config.getWatchlistName() != null ? " [Watchlist: " + config.getWatchlistName() + "]" : ""));
			} else {
				logToUI("❌ Invalid: " + (config.getName() != null ? config.getName() : "Unnamed Strategy"));
			}
		}

		// Summary
		logToUI("\n📊 VALIDATION SUMMARY");
		logToUI("-".repeat(50));
		logToUI("Total Strategies: " + totalStrategies);
		logToUI("Enabled Strategies: " + enabledStrategies);
		logToUI("Valid Strategies: " + validStrategies + "/" + totalStrategies);
		logToUI("Strategies with Watchlists: " + strategiesWithWatchlists);
		logToUI("Total Timeframe Configurations: " + totalTimeframeConfigs);
		logToUI("Total Custom Indicators: " + totalCustomIndicators);
		logToUI("Global Custom Indicators Available: " + globalCustomIndicators.size());
		logToUI("Global Watchlists Available: " + globalWatchlists.size());

		if (!hasDuplicates) {
			logToUI("✅ No duplicate strategy-timeframe combinations found");
		} else {
			logToUI("⚠️  Duplicate strategy-timeframe combinations found");
		}

		if (!hasDuplicates && validStrategies == totalStrategies) {
			JOptionPane.showMessageDialog(this,
					"All strategies validated successfully!\n\n" + "✓ " + validStrategies + "/" + totalStrategies
							+ " strategies are valid\n" + "✓ " + enabledStrategies + " strategies enabled\n" + "✓ "
							+ strategiesWithWatchlists + " strategies with watchlists\n" + "✓ " + totalTimeframeConfigs
							+ " timeframe configurations\n" + "✓ " + totalCustomIndicators
							+ " custom indicators in use\n" + "✓ " + globalCustomIndicators.size()
							+ " global custom indicators available\n" + "✓ " + globalWatchlists.size()
							+ " global watchlists available\n" + "✓ No duplicate strategy-timeframe combinations",
					"Validation Complete", JOptionPane.INFORMATION_MESSAGE);
		} else {
			JOptionPane.showMessageDialog(this,
					"Validation completed with findings:\n\n" + validStrategies + "/" + totalStrategies
							+ " strategies are valid\n" + enabledStrategies + " strategies enabled\n"
							+ strategiesWithWatchlists + " strategies with watchlists\n"
							+ (hasDuplicates ? "⚠️  Duplicate strategy-timeframe combinations found\n"
									: "✓ No duplicates found\n")
							+ "Please check the log for details.",
					"Validation Complete", JOptionPane.WARNING_MESSAGE);
		}
	}

	private void refreshData() {
		logToUI("🔄 Refreshing application data...");
		loadAllData();
		showStrategyDetails();
		logToUI("✅ Data refresh completed");
		logToUI("   Strategies: " + strategyListModel.size());
		logToUI("   Custom Indicators: " + globalCustomIndicators.size());
		logToUI("   Watchlists: " + globalWatchlists.size());
		logToUI("   Enabled Strategies: " + getEnabledStrategyCount());
		logToUI("   Strategies with Watchlists: " + getStrategiesWithWatchlistsCount());
	}

	private void saveLogToFile() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save Log File");
		fileChooser.setSelectedFile(new File("strategy_execution_log.txt"));

		int userSelection = fileChooser.showSaveDialog(this);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToSave = fileChooser.getSelectedFile();
			try (PrintWriter writer = new PrintWriter(new FileWriter(fileToSave))) {
				writer.write(logTextArea.getText());
				logToUI("💾 Log saved to: " + fileToSave.getAbsolutePath());
				JOptionPane.showMessageDialog(this, "Log successfully saved to:\n" + fileToSave.getAbsolutePath(),
						"Log Saved", JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException e) {
				logToUI("❌ Error saving log: " + e.getMessage());
				showWarning("Error saving log file: " + e.getMessage());
			}
		}
	}

	private void showStrategyDetails() {
		List<StrategyConfig> selectedStrategies = strategyList.getSelectedValuesList();

		if (selectedStrategies.size() == 1) {
			showSingleStrategyDetails(selectedStrategies.get(0));
		} else if (selectedStrategies.size() > 1) {
			showMultipleStrategiesSummary(selectedStrategies);
		} else {
			showApplicationOverview();
		}
	}

	private void showSingleStrategyDetails(StrategyConfig strategy) {
		StringBuilder details = new StringBuilder();
		details.append("STRATEGY: ").append(strategy.getName()).append("\n");
		details.append("=").append("=".repeat(strategy.getName().length())).append("\n\n");

		details.append("STATUS: ").append(strategy.isEnabled() ? "✅ ENABLED" : "⏸️ DISABLED").append("\n");
		details.append("ALARM: ").append(strategy.isAlarmEnabled() ? "🔔 ENABLED" : "🔕 DISABLED").append("\n");
		details.append("SYMBOL EXCLUSIVE: ").append(strategy.isSymbolExclusive() ? "🔒 YES" : "🌐 NO").append("\n");

		// Watchlist information
		if (strategy.getWatchlistName() != null && !strategy.getWatchlistName().isEmpty()) {
			WatchlistData watchlistSymbols = globalWatchlists.get(strategy.getWatchlistName());
			if (watchlistSymbols != null) {
				details.append("WATCHLIST: ").append(strategy.getWatchlistName()).append(" (")
						.append(watchlistSymbols.size()).append(" symbols)").append("\n");
			} else {
				details.append("WATCHLIST: ❌ ").append(strategy.getWatchlistName()).append(" (NOT FOUND)").append("\n");
			}
		} else {
			details.append("WATCHLIST: 🌐 No watchlist set (applies to all symbols)").append("\n");
		}

		details.append("GLOBAL CUSTOM INDICATORS: ").append(globalCustomIndicators.size()).append(" available")
				.append("\n");
		details.append("GLOBAL WATCHLISTS: ").append(globalWatchlists.size()).append(" available").append("\n");
		details.append("LAST UPDATED: ").append(new Date()).append("\n\n");

		details.append("TIMEFRAME INDICATORS CONFIGURATION:\n");
		details.append("-".repeat(50)).append("\n");

		for (String timeframe : strategy.getTimeframes()) {
			Set<String> indicators = strategy.getIndicatorsForTimeframe(timeframe);
			Set<CustomIndicator> customIndicators = strategy.getCustomIndicatorsForTimeframe(timeframe);

			details.append("⏰ ").append(timeframe).append(":\n");

			if (!indicators.isEmpty()) {
				details.append("   📊 Standard Indicators (").append(indicators.size()).append("):\n");
				for (String indicator : indicators) {
					details.append("      • ").append(indicator).append("\n");
				}
			}

			if (!customIndicators.isEmpty()) {
				details.append("   🛠️  Custom Indicators (").append(customIndicators.size()).append("):\n");
				for (CustomIndicator customIndicator : customIndicators) {
					details.append("      • ").append(customIndicator.getDisplayName()).append("\n");
					details.append("        Type: ").append(customIndicator.getType()).append("\n");
					details.append("        Parameters: ").append(customIndicator.getParameters()).append("\n");
				}
			}

			if (indicators.isEmpty() && customIndicators.isEmpty()) {
				details.append("   ⚠️  No indicators configured\n");
			}
			details.append("\n");
		}

		// Statistics
		details.append("STATISTICS:\n");
		details.append("-".repeat(50)).append("\n");
		details.append("   Total Timeframes: ").append(strategy.getTimeframes().size()).append("\n");
		details.append("   Total Standard Indicators: ").append(strategy.getAllIndicators().size()).append("\n");
		details.append("   Total Custom Indicators: ").append(strategy.getAllCustomIndicators().size()).append("\n");
		details.append("   Unique Configurations: ").append(strategy.getAllUniqueKeys().size()).append("\n");
		details.append("   Global Resources Available:\n");
		details.append("     - Custom Indicators: ").append(globalCustomIndicators.size()).append("\n");
		details.append("     - Watchlists: ").append(globalWatchlists.size()).append("\n\n");

		detailsTextArea.setText(details.toString());
	}

	private void showMultipleStrategiesSummary(List<StrategyConfig> strategies) {
		StringBuilder details = new StringBuilder();
		details.append("MULTIPLE STRATEGIES SELECTED: ").append(strategies.size()).append("\n");
		details.append("=").append("=".repeat(40)).append("\n\n");

		int enabledCount = 0;
		int alarmEnabledCount = 0;
		int symbolExclusiveCount = 0;
		int watchlistCount = 0;
		int totalTimeframes = 0;
		int totalIndicators = 0;
		int totalCustomIndicators = 0;

		details.append("SELECTED STRATEGIES:\n");
		details.append("-".repeat(40)).append("\n");

		for (StrategyConfig strategy : strategies) {
			if (strategy.isEnabled())
				enabledCount++;
			if (strategy.isAlarmEnabled())
				alarmEnabledCount++;
			if (strategy.isSymbolExclusive())
				symbolExclusiveCount++;
			if (strategy.getWatchlistName() != null && !strategy.getWatchlistName().isEmpty())
				watchlistCount++;
			totalTimeframes += strategy.getTimeframes().size();
			totalIndicators += strategy.getAllIndicators().size();
			totalCustomIndicators += strategy.getAllCustomIndicators().size();

			String statusIcon = strategy.isEnabled() ? "✅" : "⏸️";
			String watchlistIcon = strategy.getWatchlistName() != null && !strategy.getWatchlistName().isEmpty() ? "📋"
					: "";
			details.append(statusIcon).append(watchlistIcon).append(" ").append(strategy.getName()).append("\n");
		}

		details.append("\nSUMMARY:\n");
		details.append("-".repeat(40)).append("\n");
		details.append("Enabled: ").append(enabledCount).append("/").append(strategies.size()).append("\n");
		details.append("Alarms Enabled: ").append(alarmEnabledCount).append("\n");
		details.append("Symbol Exclusive: ").append(symbolExclusiveCount).append("\n");
		details.append("With Watchlists: ").append(watchlistCount).append("\n");
		details.append("Total Timeframes: ").append(totalTimeframes).append("\n");
		details.append("Total Standard Indicators: ").append(totalIndicators).append("\n");
		details.append("Total Custom Indicators: ").append(totalCustomIndicators).append("\n");
		details.append("Global Resources:\n");
		details.append("  Custom Indicators: ").append(globalCustomIndicators.size()).append("\n");
		details.append("  Watchlists: ").append(globalWatchlists.size()).append("\n\n");

		details.append("BULK OPERATIONS AVAILABLE:\n");
		details.append("-".repeat(40)).append("\n");
		details.append("• Use Strategy menu for bulk operations\n");
		details.append("• Enable/Disable selected strategies\n");
		details.append("• Run all enabled strategies\n");
		details.append("• Delete/Duplicate selected strategies\n");

		detailsTextArea.setText(details.toString());
	}

	private void showApplicationOverview() {
		int enabledCount = getEnabledStrategyCount();
		int watchlistCount = getStrategiesWithWatchlistsCount();

		detailsTextArea.setText("No strategy selected\n\n"
				+ "Select one or more strategies from the list to view details.\n\n" + "APPLICATION OVERVIEW:\n"
				+ "-".repeat(50) + "\n" + "Total Strategies: " + strategyListModel.size() + "\n"
				+ "Enabled Strategies: " + enabledCount + "\n" + "Strategies with Watchlists: " + watchlistCount + "\n"
				+ "Global Custom Indicators: " + globalCustomIndicators.size() + "\n" + "Global Watchlists: "
				+ globalWatchlists.size() + "\n" + "Available Timeframes: " + String.join(", ", TIMEFRAMES) + "\n\n"
				+ "NAVIGATION:\n" + "-".repeat(50) + "\n" + "• File Menu: New strategy, Import/Export, Exit\n"
				+ "• Strategy Menu: Edit, Duplicate, Delete, Bulk operations\n"
				+ "• Tools Menu: Custom indicators, Watchlists, Validation\n"
				+ "• View Menu: Log management, Application overview\n\n"
				+ "Quick Actions: Use the buttons above for common tasks");
	}

	private int getEnabledStrategyCount() {
		int count = 0;
		for (int i = 0; i < strategyListModel.size(); i++) {
			if (strategyListModel.get(i).isEnabled()) {
				count++;
			}
		}
		return count;
	}

	private int getStrategiesWithWatchlistsCount() {
		int count = 0;
		for (int i = 0; i < strategyListModel.size(); i++) {
			if (strategyListModel.get(i).getWatchlistName() != null
					&& !strategyListModel.get(i).getWatchlistName().isEmpty()) {
				count++;
			}
		}
		return count;
	}

	private void logToUI(String message) {
		SwingUtilities.invokeLater(() -> {
			logTextArea.append(message + "\n");
			logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
			logger.log(message);
		});
	}

	private void showWarning(String message) {
		JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
		logToUI("⚠️ WARNING: " + message);
	}

	public void setGlobalCustomIndicators(Set<CustomIndicator> indicators) {
		globalCustomIndicators.clear();
		globalCustomIndicators.addAll(indicators);
		saveCustomIndicators();
	}

	/**
	 * Sets the global watchlists and clears the existing ones
	 * 
	 * @param newWatchlists the new watchlists to set
	 */
	public void setGlobalWatchlists(Map<String, WatchlistData> newWatchlists) {
		if (newWatchlists != null) {
			this.globalWatchlists.clear();
			this.globalWatchlists.putAll(newWatchlists);
			logToUI("✅ Watchlists updated: " + newWatchlists.size() + " watchlists loaded");

			// Refresh any UI components that display watchlists
			if (strategyList != null) {
				strategyList.repaint();
			}
		}
	}

	/**
	 * Clears all global watchlists
	 */
	public void clearGlobalWatchlists() {
		this.globalWatchlists.clear();
		logToUI("🗑️ All watchlists cleared");

		// Refresh UI if needed
		if (strategyList != null) {
			strategyList.repaint();
		}
	}

	/**
	 * Updates the strategy execution service with new strategies if needed
	 */
	public void updateStrategyExecutionService(StrategyExecutionService newService) {
		if (newService != null) {
			this.executionService = newService;
			logToUI("🔄 Strategy execution service updated");
		}
	}

	// Custom list renderer for strategies
	private class StrategyListRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if (value instanceof StrategyConfig) {
				StrategyConfig config = (StrategyConfig) value;
				String status = config.isEnabled() ? "✅" : "⏸️";
				String alarmIndicator = config.isAlarmEnabled() ? "🔔" : "🔕";
				String symbolIndicator = config.isSymbolExclusive() ? "🔒" : "🌐";
				String watchlistIndicator = (config.getWatchlistName() != null && !config.getWatchlistName().isEmpty())
						? "📋"
						: "";

				int totalIndicators = config.getAllIndicators().size() + config.getAllCustomIndicators().size();
				int totalTimeframes = config.getTimeframes().size();

				setText(String.format("%s %s %s %s %s (%d tf, %d ind)", status, alarmIndicator, symbolIndicator,
						watchlistIndicator, config.getName(), totalTimeframes, totalIndicators));

				// Color coding and styling
				if (!config.isEnabled()) {
					setForeground(Color.GRAY);
					setBackground(isSelected ? new Color(220, 220, 220) : list.getBackground());
					setFont(getFont().deriveFont(Font.ITALIC));
				} else {
					if (config.getWatchlistName() != null && !config.getWatchlistName().isEmpty()) {
						setForeground(isSelected ? list.getSelectionForeground() : Color.GREEN.darker());
						setBackground(isSelected ? list.getSelectionBackground() : new Color(230, 255, 230));
						setFont(getFont().deriveFont(Font.BOLD));
					} else if (config.isSymbolExclusive()) {
						setForeground(isSelected ? list.getSelectionForeground() : Color.BLUE.darker());
						setBackground(isSelected ? list.getSelectionBackground() : new Color(230, 240, 255));
						setFont(getFont().deriveFont(Font.BOLD));
					} else if (config.isAlarmEnabled()) {
						setForeground(isSelected ? list.getSelectionForeground() : Color.ORANGE.darker());
						setBackground(isSelected ? list.getSelectionBackground() : new Color(255, 240, 230));
						setFont(getFont().deriveFont(Font.BOLD));
					} else {
						setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
						setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
						setFont(getFont().deriveFont(Font.PLAIN));
					}
				}

				// Enhanced tooltip with watchlist information
				StringBuilder tooltip = new StringBuilder();
				tooltip.append("<html>");
				tooltip.append("<b>").append(config.getName()).append("</b><br>");
				tooltip.append("Status: ").append(config.isEnabled() ? "✅ Enabled" : "⏸️ Disabled").append("<br>");
				tooltip.append("Alarm: ").append(config.isAlarmEnabled() ? "🔔 Enabled" : "🔕 Disabled").append("<br>");
				tooltip.append("Symbol Exclusive: ").append(config.isSymbolExclusive() ? "🔒 Yes" : "🌐 No")
						.append("<br>");

				if (config.getWatchlistName() != null && !config.getWatchlistName().isEmpty()) {
					WatchlistData symbols = globalWatchlists.get(config.getWatchlistName());
					int symbolCount = symbols != null ? symbols.size() : 0;
					tooltip.append("Watchlist: ").append(config.getWatchlistName()).append(" (").append(symbolCount)
							.append(" symbols)<br>");
				} else {
					tooltip.append("Watchlist: None (applies to all symbols)<br>");
				}

				tooltip.append("Timeframes: ").append(totalTimeframes).append("<br>");
				tooltip.append("Total Indicators: ").append(totalIndicators).append("<br>");
				tooltip.append("Global Resources: ").append(globalCustomIndicators.size()).append(" indicators, ")
						.append(globalWatchlists.size()).append(" watchlists<br>");

				tooltip.append("<br><b>Timeframe Indicators:</b><br>");
				for (String timeframe : config.getTimeframes()) {
					Set<String> indicators = config.getIndicatorsForTimeframe(timeframe);
					Set<CustomIndicator> customIndicators = config.getCustomIndicatorsForTimeframe(timeframe);
					tooltip.append("• ").append(timeframe).append(": ").append(indicators.size()).append(" standard, ")
							.append(customIndicators.size()).append(" custom<br>");
				}
				tooltip.append("</html>");
				setToolTipText(tooltip.toString());
			}
			return this;
		}
	}

	// Getters for testing and external access
	public DefaultListModel<StrategyConfig> getStrategyListModel() {
		return strategyListModel;
	}

	public JList<StrategyConfig> getStrategyList() {
		return strategyList;
	}

	// In IndicatorsManagementApp.java

	/**
	 * Gets all strategies as a list for external access
	 */
	public List<StrategyConfig> getAllStrategies() {
		List<StrategyConfig> strategies = new ArrayList<>();
		for (int i = 0; i < strategyListModel.size(); i++) {
			strategies.add(strategyListModel.get(i));
		}
		return strategies;
	}

	/**
	 * Sets strategies from external source and clears existing ones
	 */
	public void setAllStrategies(List<StrategyConfig> strategies) {
		strategyListModel.clear();
		if (strategies != null) {
			for (StrategyConfig strategy : strategies) {
				strategyListModel.addElement(strategy);
			}
			logToUI("✅ Strategies loaded: " + strategies.size() + " strategies");
		}
	}

	/**
	 * Gets all custom indicators for external access
	 */
	public Set<CustomIndicator> getAllCustomIndicators() {
		return new HashSet<>(globalCustomIndicators);
	}

	/**
	 * Sets custom indicators from external source and clears existing ones
	 */
	public void setAllCustomIndicators(Set<CustomIndicator> customIndicators) {
		globalCustomIndicators.clear();
		if (customIndicators != null) {
			globalCustomIndicators.addAll(customIndicators);
			logToUI("✅ Custom indicators loaded: " + customIndicators.size() + " indicators");
		}
	}

	/**
	 * Sets custom indicators from external source and clears existing ones
	 */
	public void addAllCustomIndicators(Set<CustomIndicator> customIndicators) {
		if (customIndicators != null) {
			globalCustomIndicators.addAll(customIndicators);
			logToUI("✅ Custom indicators loaded: " + customIndicators.size() + " indicators");
		}
	}

	/**
	 * Clears all strategies
	 */
	public void clearAllStrategies() {
		strategyListModel.clear();
		logToUI("🗑️ All strategies cleared");
	}

	/**
	 * Clears all custom indicators
	 */
	public void clearAllCustomIndicators() {
		globalCustomIndicators.clear();
		logToUI("🗑️ All custom indicators cleared");
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				e.printStackTrace();
			}

			ConsoleLogger logger = new ConsoleLogger() {
				@Override
				public void log(String message) {
					System.out.println("[APP] " + message);
				}
			};

			StrategyExecutionService executionService = new StrategyExecutionService(logger);

			IndicatorsManagementApp app = new IndicatorsManagementApp(executionService, logger);
			app.setVisible(true);

			app.logToUI("🎯 Indicators Management Application Started Successfully");
			app.logToUI("   Version: 5.0.0");
			app.logToUI("   Features: Menu-driven interface, Optional watchlists, Bulk operations");
			app.logToUI("   Navigation: Use the menu bar for complete functionality");
		});
	}

	public JSONObject exportConfiguration() {

		// Convert strategy list model to list
		List<StrategyConfig> strategies = new ArrayList<>();
		for (int i = 0; i < strategyListModel.size(); i++) {
			strategies.add(strategyListModel.get(i));
		}

		JSONObject result = configurationExporter.createExportJSONObject(strategies, globalCustomIndicators,
				globalWatchlists, "");

		return result;
	}

	/*
	 * public void importConfiguration(Map<String, Object> config) { try {
	 * logger.log("Starting configuration import...");
	 * 
	 * // Handle custom indicators if (config.containsKey("customIndicators")) {
	 * Object customIndicators = config.get("customIndicators");
	 * importCustomIndicators(customIndicators); logger.log("✅ Imported " +
	 * globalCustomIndicators.size() + " custom indicators"); }
	 * 
	 * // Handle strategies (these are strategy configurations, not indicator //
	 * definitions) if (config.containsKey("strategies")) { Object strategies =
	 * config.get("strategies"); importStrategyConfigurations(strategies);
	 * logger.log("✅ Imported " + strategyConfigurations.size() +
	 * " strategy configurations"); }
	 * 
	 * // Handle watchlists if (config.containsKey("watchlists")) { Object
	 * watchlists = config.get("watchlists"); importWatchlists(watchlists);
	 * logger.log("✅ Imported " + globalWatchlists.size() + " watchlists"); }
	 * 
	 * // Optional: Trigger any post-import processing onConfigurationImported();
	 * 
	 * logger.log("✅ Configuration import completed successfully");
	 * 
	 * } catch (Exception e) { logger.log("❌ Error importing configuration: " +
	 * e.getMessage()); e.printStackTrace(); } }
	 */

	// Import strategy configurations (not indicator definitions)
	// Import strategy configurations (not indicator definitions)
	private void importStrategyConfigurations(Object strategies) {
		strategyConfigurations.clear(); // Clear existing configurations

		if (strategies instanceof JSONArray) {
			JSONArray strategiesArray = (JSONArray) strategies;
			for (int i = 0; i < strategiesArray.length(); i++) {
				try {
					JSONObject strategyJson = strategiesArray.getJSONObject(i);
					String strategyName = strategyJson.getString("name");
					strategyConfigurations.put(strategyName, strategyJson.toMap());

					// Log strategy details
					boolean enabled = strategyJson.getBoolean("enabled");
					String watchlistName = strategyJson.optString("watchlistName", "");

					logger.log("Imported strategy config: " + strategyName + " (enabled: " + enabled + ", watchlist: "
							+ watchlistName + ")");

					// Log timeframe indicators
					if (strategyJson.has("timeframeIndicators")) {
						JSONObject timeframeIndicators = strategyJson.getJSONObject("timeframeIndicators");
						Iterator<String> timeframes = timeframeIndicators.keys();
						while (timeframes.hasNext()) {
							String timeframe = timeframes.next();
							JSONArray indicators = timeframeIndicators.getJSONArray(timeframe);
							logger.log("  - " + timeframe + ": " + indicators.length() + " indicators");
						}
					}

				} catch (Exception e) {
					logger.log("Error parsing strategy configuration at index " + i + ": " + e.getMessage());
				}
			}
		} else if (strategies instanceof List) {
			List<?> strategiesList = (List<?>) strategies;
			for (Object strategyObj : strategiesList) {
				try {
					if (strategyObj instanceof Map) {
						Map<?, ?> strategyMap = (Map<?, ?>) strategyObj;
						String strategyName = getStringFromMap(strategyMap, "name");
						if (strategyName != null) {
							strategyConfigurations.put(strategyName, new HashMap<>(strategyMap));

							boolean enabled = getBooleanFromMap(strategyMap, "enabled", false);
							String watchlistName = getStringFromMap(strategyMap, "watchlistName", "");

							logger.log("Imported strategy config: " + strategyName + " (enabled: " + enabled
									+ ", watchlist: " + watchlistName + ")");
						}
					}
					if (strategyObj instanceof StrategyConfig) {
						StrategyConfig strategyConfig = (StrategyConfig) strategyObj;
						String strategyName = strategyConfig.getName();
						if (strategyName != null) {
							strategyConfigurations.put(strategyName, strategyConfig);

							boolean enabled = strategyConfig.isEnabled();//getBooleanFromMap(strategyMap, "enabled", false);
							String watchlistName = strategyConfig.getWatchlistName();//getStringFromMap(strategyMap, "watchlistName", "");

							logger.log("Imported strategy config: " + strategyName + " (enabled: " + enabled
									+ ", watchlist: " + watchlistName + ")");
						}
					}
				} catch (Exception e) {
					logger.log("Error parsing strategy configuration: " + e.getMessage());
				}
			}
		}
	}

	// Add these helper methods to the IndicatorsManagementApp class:

	// Helper methods for safe map access
	private String getStringFromMap(Map<?, ?> map, String key) {
		Object value = map.get(key);
		return value != null ? value.toString() : null;
	}

	private String getStringFromMap(Map<?, ?> map, String key, String defaultValue) {
		Object value = map.get(key);
		return value != null ? value.toString() : defaultValue;
	}

	private boolean getBooleanFromMap(Map<?, ?> map, String key, boolean defaultValue) {
		Object value = map.get(key);
		if (value instanceof Boolean) {
			return (Boolean) value;
		} else if (value != null) {
			return Boolean.parseBoolean(value.toString());
		}
		return defaultValue;
	}

	private int getIntFromMap(Map<?, ?> map, String key, int defaultValue) {
		Object value = map.get(key);
		if (value instanceof Number) {
			return ((Number) value).intValue();
		} else if (value != null) {
			try {
				return Integer.parseInt(value.toString());
			} catch (NumberFormatException e) {
				logger.log("⚠️ Could not parse int from key '" + key + "': " + value);
			}
		}
		return defaultValue;
	}

	private double getDoubleFromMap(Map<?, ?> map, String key, double defaultValue) {
		Object value = map.get(key);
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else if (value != null) {
			try {
				return Double.parseDouble(value.toString());
			} catch (NumberFormatException e) {
				logger.log("⚠️ Could not parse double from key '" + key + "': " + value);
			}
		}
		return defaultValue;
	}

	// Import custom indicators into the Set<CustomIndicator>
	private void importCustomIndicators(Object customIndicators) {
		globalCustomIndicators.clear(); // Clear existing indicators

		if (customIndicators instanceof JSONArray) {
			JSONArray indicatorsArray = (JSONArray) customIndicators;
			for (int i = 0; i < indicatorsArray.length(); i++) {
				try {
					JSONObject indicatorJson = indicatorsArray.getJSONObject(i);
					CustomIndicator indicator = createCustomIndicatorFromJson(indicatorJson);
					if (indicator != null) {
						globalCustomIndicators.add(indicator);
						logger.log("Added custom indicator: " + indicator.getName() + " (" + indicator.getDisplayName()
								+ ")");
					}
				} catch (Exception e) {
					logger.log("Error parsing custom indicator at index " + i + ": " + e.getMessage());
				}
			}
		} else if (customIndicators instanceof List) {
			List<?> indicatorsList = (List<?>) customIndicators;
			for (Object indicatorObj : indicatorsList) {
				try {
					if (indicatorObj instanceof Map) {
						Map<?, ?> indicatorMap = (Map<?, ?>) indicatorObj;
						CustomIndicator indicator = createCustomIndicatorFromMap(indicatorMap);
						if (indicator != null) {
							globalCustomIndicators.add(indicator);
							logger.log("Added custom indicator: " + indicator.getName() + " ("
									+ indicator.getDisplayName() + ")");
						}
					} else if (indicatorObj instanceof CustomIndicator) {
						globalCustomIndicators.add((CustomIndicator) indicatorObj);
					}
				} catch (Exception e) {
					logger.log("Error parsing custom indicator: " + e.getMessage());
				}
			}
		}
	}

	// Import watchlists into Map<String, Set<String>>
	public void importWatchlists(Object watchlists) {
		globalWatchlists.clear(); // Clear existing watchlists

		if (watchlists instanceof JSONArray) {
			JSONArray watchlistsArray = (JSONArray) watchlists;
			for (int i = 0; i < watchlistsArray.length(); i++) {
				try {
					JSONObject watchlistJson = watchlistsArray.getJSONObject(i);
					String watchlistName = watchlistJson.getString("name");
					JSONArray symbolsArray = watchlistJson.getJSONArray("symbols");

					Set<String> symbols = new HashSet<>();
					for (int j = 0; j < symbolsArray.length(); j++) {
						symbols.add(symbolsArray.getString(j));
					}

					String primarySymbol = watchlistJson.optString("primarySymbol", "");

					globalWatchlists.put(watchlistName, new WatchlistData(symbols, primarySymbol));
					logToUI("Added watchlist: " + watchlistName + " with " + symbols.size() + " symbols");

				} catch (Exception e) {
					logToUI("Error parsing watchlist at index " + i + ": " + e.getMessage());
				}
			}
		} else if (watchlists instanceof List) {
			List<?> watchlistsList = (List<?>) watchlists;
			for (Object watchlistObj : watchlistsList) {
				try {
					if (watchlistObj instanceof Map) {
						Map<?, ?> watchlistMap = (Map<?, ?>) watchlistObj;
						String watchlistName = (String) watchlistMap.get("name");
						List<?> symbolsList = (List<?>) watchlistMap.get("symbols");

						Set<String> symbols = new HashSet<>();
						for (Object symbolObj : symbolsList) {
							symbols.add(symbolObj.toString());
						}

						String primarySymbol = (String) watchlistMap.get("primarySymbol");

						globalWatchlists.put(watchlistName, new WatchlistData(symbols, primarySymbol));
						logToUI("Added watchlist: " + watchlistName + " with " + symbols.size() + " symbols");
					}
				} catch (Exception e) {
					logToUI("Error parsing watchlist: " + e.getMessage());
				}
			}
		} else if (watchlists instanceof Map) {
			// Handle the case where watchlists is already in the new format
			Map<?, ?> watchlistsMap = (Map<?, ?>) watchlists;
			for (Map.Entry<?, ?> entry : watchlistsMap.entrySet()) {
				if (entry.getKey() instanceof String && entry.getValue() instanceof WatchlistData) {
					globalWatchlists.put((String) entry.getKey(), (WatchlistData) entry.getValue());
				}
			}
		}
	}
	
	// Import watchlists into Map<String, Set<String>>
		public void importWatchlists(Map<String, WatchlistData> watchlists) {
			globalWatchlists.clear(); // Clear existing watchlists

			if (watchlists instanceof Map) {
				// Handle the case where watchlists is already in the new format
				Map<?, ?> watchlistsMap = (Map<?, ?>) watchlists;
				for (Map.Entry<?, ?> entry : watchlistsMap.entrySet()) {
					if (entry.getKey() instanceof String && entry.getValue() instanceof WatchlistData) {
						globalWatchlists.put((String) entry.getKey(), (WatchlistData) entry.getValue());
					}
				}
			}
		}

	// Helper method to create CustomIndicator from JSON
	private CustomIndicator createCustomIndicatorFromJson(JSONObject json) {
		try {
			CustomIndicator indicator = new CustomIndicator(json.getString("name"), json.getString("type"));
			indicator.setName(json.getString("name"));
			indicator.setType(json.getString("type"));

			// Set other properties based on your CustomIndicator class
			/*
			 * if (json.has("description")) {
			 * indicator.setDescription(json.getString("description")); } if
			 * (json.has("parameters")) { // Handle parameters based on your implementation
			 * JSONObject params = json.getJSONObject("parameters"); //
			 * indicator.setParameters(parseParameters(params)); } if (json.has("enabled"))
			 * { indicator.setEnabled(json.getBoolean("enabled")); }
			 */

			return indicator;
		} catch (Exception e) {
			logger.log("Error creating CustomIndicator from JSON: " + e.getMessage());
			return null;
		}
	}

	// Helper method to create CustomIndicator from Map
	private CustomIndicator createCustomIndicatorFromMap(Map<?, ?> map) {
		try {
			CustomIndicator indicator = new CustomIndicator((String) map.get("name"), (String) map.get("type"));
			indicator.setName((String) map.get("name"));
			indicator.setType((String) map.get("type"));
			indicator.setParameters((Map<String, Object>) map.get("parameters"));

			// Set other properties based on your CustomIndicator class
			/*
			 * if (map.containsKey("description")) { indicator.setDescription((String)
			 * map.get("description")); } if (map.containsKey("parameters")) { // Handle
			 * parameters based on your implementation Object params =
			 * map.get("parameters"); // indicator.setParameters(parseParameters(params)); }
			 * if (map.containsKey("enabled")) { indicator.setEnabled((Boolean)
			 * map.get("enabled")); }
			 */

			return indicator;
		} catch (Exception e) {
			logger.log("Error creating CustomIndicator from Map: " + e.getMessage());
			return null;
		}
	}

	// Add this method to initialize strategyListModel with imported data
	private void initializeStrategyListModelWithData() {
		strategyListModel.clear();

		for (Map.Entry<String, Object> entry : strategyConfigurations.entrySet ()) {
			try {
				String strategyName = entry.getKey();
				Object configObj = entry.getValue();

				StrategyConfig strategyConfig = createStrategyConfigFromMap(strategyName, configObj);
				if (strategyConfig != null) {
					strategyListModel.addElement(strategyConfig);
					logToUI("✅ Added to strategy list: " + strategyName);
				}
			} catch (Exception e) {
				logToUI("❌ Error creating StrategyConfig for: " + entry.getKey() + " - " + e.getMessage());
			}
		}

		logToUI("📊 Strategy list model updated: " + strategyListModel.size() + " strategies");
	}

	// Convert imported strategy configuration to StrategyConfig object
	private StrategyConfig createStrategyConfigFromMap(String strategyName, Object configObj) {
		try {
			if (configObj instanceof Map) {
				Map<?, ?> configMap = (Map<?, ?>) configObj;

				StrategyConfig strategyConfig = new StrategyConfig();
				strategyConfig.setName(strategyName);
				strategyConfig.setEnabled(getBooleanFromMap(configMap, "enabled", false));
				strategyConfig.setAlarmEnabled(getBooleanFromMap(configMap, "alarmEnabled", false));
				strategyConfig.setSymbolExclusive(getBooleanFromMap(configMap, "symbolExclusive", false));

				// Set watchlist name
				String watchlistName = getStringFromMap(configMap, "watchlistName", "");
				if (watchlistName != null && !watchlistName.isEmpty()) {
					strategyConfig.setWatchlistName(watchlistName);
				}

				// Set exclusive symbols if available
				if (configMap.containsKey("exclusiveSymbols")) {
					Object exclusiveSymbolsObj = configMap.get("exclusiveSymbols");
					if (exclusiveSymbolsObj instanceof List) {
						List<?> symbolsList = (List<?>) exclusiveSymbolsObj;
						Set<String> exclusiveSymbols = new HashSet<>();
						for (Object symbol : symbolsList) {
							exclusiveSymbols.add(symbol.toString());
						}
						strategyConfig.setExclusiveSymbols(exclusiveSymbols);
					}
				}

				// Handle timeframe indicators
				if (configMap.containsKey("timeframeIndicators")) {
					Object timeframeIndicatorsObj = configMap.get("timeframeIndicators");
					if (timeframeIndicatorsObj instanceof Map) {
						Map<?, ?> timeframeIndicators = (Map<?, ?>) timeframeIndicatorsObj;

						for (Map.Entry<?, ?> timeframeEntry : timeframeIndicators.entrySet()) {
							String timeframe = timeframeEntry.getKey().toString();
							Object indicatorsObj = timeframeEntry.getValue();

							if (indicatorsObj instanceof List) {
								List<?> indicatorsList = (List<?>) indicatorsObj;
								Set<String> indicators = new HashSet<>();
								for (Object indicator : indicatorsList) {
									indicators.add(indicator.toString());
								}
								strategyConfig.addTimeframeWithIndicators(timeframe, indicators);
							}
						}
					}
				}

				// Handle custom indicators for timeframes - CORRECTED VERSION
				if (configMap.containsKey("timeframeCustomIndicators")) {
					Object timeframeCustomIndicatorsObj = configMap.get("timeframeCustomIndicators");
					if (timeframeCustomIndicatorsObj instanceof Map) {
						Map<?, ?> timeframeCustomIndicators = (Map<?, ?>) timeframeCustomIndicatorsObj;

						for (Map.Entry<?, ?> timeframeEntry : timeframeCustomIndicators.entrySet()) {
							String timeframe = timeframeEntry.getKey().toString();
							Object customIndicatorsObj = timeframeEntry.getValue();

							if (customIndicatorsObj instanceof List) {
								List<?> customIndicatorsList = (List<?>) customIndicatorsObj;
								Set<CustomIndicator> customIndicators = new HashSet<>();

								for (Object customIndicatorObj : customIndicatorsList) {
									if (customIndicatorObj instanceof Map) {
										Map<?, ?> customIndicatorMap = (Map<?, ?>) customIndicatorObj;
										CustomIndicator customIndicator = createCustomIndicatorFromMap(
												customIndicatorMap);
										if (customIndicator != null) {
											customIndicators.add(customIndicator);
										}
									}
								}

								// Use the correct method from StrategyConfig
								if (!customIndicators.isEmpty()) {
									strategyConfig.addTimeframeWithCustomIndicators(timeframe, customIndicators);
									logToUI("   Added " + customIndicators.size() + " custom indicators to timeframe: "
											+ timeframe);
								}
							}
						}
					}
				}

				// Handle strategy parameters
				if (configMap.containsKey("strategyParameters")) {
					Object strategyParamsObj = configMap.get("strategyParameters");
					if (strategyParamsObj instanceof Map) {
						Map<?, ?> strategyParams = (Map<?, ?>) strategyParamsObj;
						// Convert to the correct type for StrategyConfig
						Map<String, Map<String, Object>> convertedStrategyParams = new HashMap<>();

						for (Map.Entry<?, ?> entry : strategyParams.entrySet()) {
							String strategyNameKey = entry.getKey().toString();
							Object paramsObj = entry.getValue();

							if (paramsObj instanceof Map) {
								Map<?, ?> paramsMap = (Map<?, ?>) paramsObj;
								Map<String, Object> convertedParams = new HashMap<>();

								for (Map.Entry<?, ?> paramEntry : paramsMap.entrySet()) {
									convertedParams.put(paramEntry.getKey().toString(), paramEntry.getValue());
								}
								convertedStrategyParams.put(strategyNameKey, convertedParams);
							}
						}
						strategyConfig.setStrategyParameters(convertedStrategyParams);
					}
				}

				// Handle general parameters
				if (configMap.containsKey("parameters")) {
					Object parametersObj = configMap.get("parameters");
					if (parametersObj instanceof Map) {
						Map<?, ?> paramsMap = (Map<?, ?>) parametersObj;
						Map<String, Object> convertedParams = new HashMap<>();

						for (Map.Entry<?, ?> paramEntry : paramsMap.entrySet()) {
							convertedParams.put(paramEntry.getKey().toString(), paramEntry.getValue());
						}
						strategyConfig.setParameters(convertedParams);
					}
				}

				logToUI("✅ Created StrategyConfig: " + strategyName + " (Timeframes: "
						+ strategyConfig.getTimeframes().size() + ", Indicators: "
						+ strategyConfig.getAllIndicators().size() + ", Custom Indicators: "
						+ strategyConfig.getAllCustomIndicators().size() + ")");

				return strategyConfig;
			} else if(configObj instanceof StrategyConfig) {
				return (StrategyConfig) configObj;
			}
		} catch (Exception e) {
			logToUI("❌ Error creating StrategyConfig: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	// Update your existing importConfiguration method to call the initialization:
	public void importConfiguration(Map<String, Object> config) {
		try {
			logToUI("Starting configuration import...");

			// Handle custom indicators
			if (config.containsKey("customIndicators")) {
				Object customIndicators = config.get("customIndicators");
				importCustomIndicators(customIndicators);
				logToUI("✅ Imported " + globalCustomIndicators.size() + " custom indicators");
			}

			// Handle strategies
			if (config.containsKey("strategies")) {
				Object strategies = config.get("strategies");
				importStrategyConfigurations(strategies);
				logToUI("✅ Imported " + strategyConfigurations.size() + " strategy configurations");

				// Initialize strategyListModel with imported strategies
				initializeStrategyListModelWithData(); // ← ADD THIS LINE
			}

			// Handle watchlists
			if (config.containsKey("watchlists")) {
				Object watchlists = config.get("watchlists");
				importWatchlists(watchlists);
				logToUI("✅ Imported " + globalWatchlists.size() + " watchlists");
			}

			// Optional: Trigger any post-import processing
			onConfigurationImported();

			logToUI("✅ Configuration import completed successfully");

		} catch (Exception e) {
			logToUI("❌ Error importing configuration: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	 /**
     * Calculate Z-Scores for all strategies on a symbol
     */
    public void calculateZScoresForSymbol(String symbol, PriceData priceData) {
        if (priceData == null || strategyListModel.isEmpty()) {
            return;
        }
        
        Map<String, Map<String, ZScoreCalculator.TimeframeWeight>> allStrategyWeights = new HashMap<>();
        
        // Collect weights from all strategies
        for (int i = 0; i < strategyListModel.size(); i++) {
            StrategyConfig strategy = strategyListModel.get(i);
            
            if (strategy.isEnabled()) {
                @SuppressWarnings("unchecked")
                Map<String, Integer> weightMap = (Map<String, Integer>) 
                    strategy.getParameters().get("zscoreWeights");
                
                if (weightMap != null && !weightMap.isEmpty()) {
                    Map<String, ZScoreCalculator.TimeframeWeight> weights = new HashMap<>();
                    for (Map.Entry<String, Integer> entry : weightMap.entrySet()) {
                        weights.put(entry.getKey(), 
                            new ZScoreCalculator.TimeframeWeight(entry.getKey(), entry.getValue()));
                    }
                    allStrategyWeights.put(strategy.getName(), weights);
                }
            }
        }
        
        // Calculate scores for all strategies
        if (!allStrategyWeights.isEmpty()) {
            Map<String, ZScoreCalculator.ZScoreResult> results = 
                ZScoreCalculator.calculateOverallScores(priceData, allStrategyWeights);
            
            // Store results in PriceData
            for (Map.Entry<String, ZScoreCalculator.ZScoreResult> entry : results.entrySet()) {
                priceData.addZScoreResult(entry.getKey(), entry.getValue());
            }
            
            // Log results
            logToUI(String.format("Z-Scores calculated for %s:", symbol));
            for (ZScoreCalculator.ZScoreResult result : priceData.getStrategiesByScore()) {
                logToUI(String.format("  %s: %.1f/100 (%s)", 
                    result.getStrategyName(), result.getOverallScore(), result.getStrengthCategory()));
            }
        }
    }
    
    /**
     * Get Z-Score summary for display in UI
     */
    public String getZScoreSummary(PriceData priceData) {
        if (priceData == null || priceData.getZScoreResults() == null || priceData.getZScoreResults().isEmpty()) {
            return "No Z-Score data available";
        }
        
        return ZScoreCalculator.generateSummaryReport(priceData.getZScoreResults());
    }

	// Also update the onConfigurationImported method to refresh the UI properly:
	private void onConfigurationImported() {
		// Refresh UI components
		SwingUtilities.invokeLater(() -> {
			strategyList.repaint();
			showStrategyDetails();
		});

		logToUI("🔄 UI refreshed with imported configuration");
		logToUI("   Strategies in list: " + strategyListModel.size());
		logToUI("   Custom indicators: " + globalCustomIndicators.size());
		logToUI("   Watchlists: " + globalWatchlists.size());
	}

	// Utility method to get all enabled strategies from strategyListModel
	public List<String> getEnabledStrategies() {
		List<String> enabled = new ArrayList<>();
		for (int i = 0; i < strategyListModel.size(); i++) {
			StrategyConfig config = strategyListModel.get(i);
			if (config.isEnabled()) {
				enabled.add(config.getName());
			}
		}
		return enabled;
	}

	// Utility method to get strategy configuration by name from the list model
	public StrategyConfig getStrategyConfigByName(String strategyName) {
		for (int i = 0; i < strategyListModel.size(); i++) {
			StrategyConfig config = strategyListModel.get(i);
			if (strategyName.equals(config.getName())) {
				return config;
			}
		}
		return null;
	}

	private void refreshUI() {
		// Update your UI components here
		logger.log("UI refreshed with " + globalCustomIndicators.size() + " indicators and " + globalWatchlists.size()
				+ " watchlists");
	}

	private void initializeComponents() {
		// Initialize any components that depend on the loaded configuration
		logger.log("Components initialized with imported configuration");
	}

	/**
	 * Convert WatchlistData to old format for compatibility
	 */
	private Map<String, Set<String>> convertWatchlistsToOldFormat(Map<String, WatchlistData> watchlistData) {
		Map<String, Set<String>> oldFormat = new HashMap<>();
		for (Map.Entry<String, WatchlistData> entry : watchlistData.entrySet()) {
			oldFormat.put(entry.getKey(), new HashSet<>(entry.getValue().getSymbols()));
		}
		return oldFormat;
	}

	/**
	 * Convert old format to WatchlistData
	 */
	private Map<String, WatchlistData> convertOldFormatToWatchlistData(Map<String, Set<String>> oldFormat) {
		Map<String, WatchlistData> newFormat = new HashMap<>();
		for (Map.Entry<String, Set<String>> entry : oldFormat.entrySet()) {
			newFormat.put(entry.getKey(), new WatchlistData(entry.getValue(), ""));
		}
		return newFormat;
	}
	
	/**
	 * Convert old format watchlists to new WatchlistData format
	 */
	@SuppressWarnings("unchecked")
	private Map<String, WatchlistData> convertToWatchlistData(Map<String, ?> oldWatchlists) {
	    Map<String, WatchlistData> newWatchlists = new HashMap<>();
	    
	    for (Map.Entry<String, ?> entry : oldWatchlists.entrySet()) {
	        String watchlistName = entry.getKey();
	        Object value = entry.getValue();
	        
	        if (value instanceof WatchlistData) {
	            newWatchlists.put(watchlistName, (WatchlistData) value);
	        } else if (value instanceof Set) {
	            Set<String> symbols = (Set<String>) value;
	            newWatchlists.put(watchlistName, new WatchlistData(symbols, ""));
	        }
	    }
	    
	    return newWatchlists;
	}

	// Getter methods
	public Set<CustomIndicator> getGlobalCustomIndicators() {
		return Collections.unmodifiableSet(globalCustomIndicators);
	}

	public Map<String, WatchlistData> getGlobalWatchlists() {
		return Collections.unmodifiableMap(globalWatchlists);
	}

	public StrategyExecutionService getStrategyExecutionService() {
		return executionService;
	}

	// Utility method to get watchlist symbols
	public Set<String> getWatchlistSymbols(String watchlistName) {
	    WatchlistData watchlistData = globalWatchlists.get(watchlistName);
	    return watchlistData != null ? watchlistData.getSymbols() : Collections.emptySet();
	}

	// Utility method to check if indicator exists
	public boolean hasCustomIndicator(String indicatorName) {
		return globalCustomIndicators.stream().anyMatch(indicator -> indicator.getName().equals(indicatorName));
	}

	public StrategyExecutionService getExecutionService() {
		return executionService;
	}

	public void setExecutionService(StrategyExecutionService executionService) {
		this.executionService = executionService;
	}

	public boolean isSaved() {
		return isSaved;
	}

	public void setSaved(boolean isSaved) {
		this.isSaved = isSaved;
	}

}