package com.quantlabs.stockApp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXDatePicker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.alert.AlertManager;
import com.quantlabs.stockApp.alert.ConfigManager;
import com.quantlabs.stockApp.alert.MessageManager;
import com.quantlabs.stockApp.yahoo.query.YahooFinanceQueryScreenerService;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StockDashboard extends JFrame {
	private final OkHttpClient client = new OkHttpClient();
	private final String apiKey = "PK4UOZQDJJZ6WBAU52XM";
	private final String apiSecret = "Fag4ha2D58VyL0okwXgBHD1IvhoptmI2KiacMaNG";

	private final String[] symbols = { "AAPL", "AVGO", "PLTR", "MSFT", "GOOGL", "AMZN", "TSLA", "NVDA", "META", "NFLX",
			"WMT", "JPM", "LLY", "V", "ORCL", "MA", "XOM", "COST", "JNJ", "HD", "UNH", "DLTR" };

	// private final String[] symbols = { "NVDA"};

	private static final Map<String, Integer> ALPACA_TIME_LIMITS = Map.of("1Min", 4, "5Min", 7, "15Min", 7, "30Min", 30,
			"1H", 30, "4H", 30, "1D", 60);

	private static final Map<String, Integer> YAHOO_TIME_LIMITS = Map.of("1Min", 7, "5Min", 60, "15Min", 60, "30Min",
			60, "1H", 60, "4H", 60, "1D", 60);

	private final String[] dataSources = { "Alpaca", "Yahoo" };
	private final String[] allTimeframes = { "1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min" };

	private final String[] allIndicators = { "Trend", "RSI", "MACD", "MACD Breakout", "MACD(5,8,9)",
			"MACD(5,8,9) Breakout", "PSAR(0.05)", "PSAR(0.05) Breakout", "PSAR(0.01)", "PSAR(0.01) Breakout",
			"Breakout Count", "Action" };

	/*
	 * private final String[] allIndicators = { "Trend", "RSI", "MACD",
	 * "MACD(5,8,9)", "PSAR(0.05)", "PSAR(0.01)", "Action", "Breakout Count" };
	 */

	// Add these near the top of your class with other constants
	private static final Set<String> BULLISH_STATUSES = Set.of("Bullish Crossover", "Bullish", "Strong Uptrend",
			"Mild Uptrend", "Neutral", "↑ Uptrend", "Buy");

	private static final String BREAKOUT_COUNT_COLUMN_PATTERN = "Breakout";

	private final Integer[] refreshIntervals = { 10, 30, 60, 120, 300 }; // Seconds

	private static final Color BULLISH_COLOR = new Color(200, 255, 200); // Light green
	private static final Color BEARISH_COLOR = new Color(255, 200, 200); // Light red

	// Add these with your other instance variables
	private List<Integer> indicatorColumnIndices;
	private boolean columnsInitialized = false;

	private JTable dashboardTable;
	private FilterableTableModel tableModel;
	private JButton refreshButton;
	private JButton startLiveButton;
	private JButton stopLiveButton;
	private JButton toggleVisibilityButton;

	private JCheckBox checkBullishCheckbox;
	private JCheckBox alarmOnCheckbox;
	private JButton stopAlarmButton;
	private AlertManager alertManager;
	private JTextField symbolExceptionsField;
	private ScheduledExecutorService buzzerScheduler;

	private boolean isAlarmActive = false;

	private JButton showAllButton;
	private JButton addWatchlistButton;
	private JButton editWatchlistButton;
	private JComboBox<String> dataSourceComboBox;
	private JComboBox<String> watchlistComboBox;
	private JComboBox<Integer> refreshIntervalComboBox;
	private ScheduledExecutorService scheduler;
	private boolean isLive = false;
	private final Map<String, Color> statusColors = new HashMap<>();
	private final Map<String, List<String>> watchlists = new HashMap<>();
	private String currentWatchlist = "Default";
	private String currentDataSource = "Yahoo";
	private int currentRefreshInterval = 60; // Default interval in seconds

	private List<String> watchListException = Arrays.asList("Default", "YahooDOW", "YahooNASDAQ", "YahooSP500",
			"YahooRussel2k", "YahooPennyBy3MonVol", "YahooPennyByPercentChange");

	private JRadioButton currentTimeRadio;
	private JRadioButton customRangeRadio;
	private ButtonGroup timeRangeGroup;
	private JLabel timeRangeLabel;
	private JButton clearCustomRangeButton;
	private JXDatePicker startDatePicker;
	private JXDatePicker endDatePicker;

	private JTextArea consoleArea;
	private JScrollPane consoleScrollPane;
	private JPanel consolePanel;
	private JPanel timeRangePanel;

	private JPanel filtersPanel;
	private JTextField prevVolMinField;
	private JTextField prevVolMaxField;
	private JTextField currentVolMinField;
	private JTextField currentVolMaxField;

	YahooFinanceQueryScreenerService yahooQueryFinance = new YahooFinanceQueryScreenerService();

	private JPanel controlPanel;
	private JButton listSymbolsButton;
	private JLabel statusLabel;
	private JButton checkNonBullishButton;
	private AbstractButton uncheckAllButton;

	private Map<String, Set<String>> timeframeIndicators = new LinkedHashMap<>();
	private Map<String, JCheckBox> timeframeCheckboxes = new HashMap<>();
	private Map<String, Map<String, JCheckBox>> indicatorCheckboxes = new HashMap<>();
	private JCheckBox inheritCheckbox;
	private MessageManager messageManager;
	private ConfigManager configManager;

	// Add this near your other instance variables
	private JTabbedPane tabbedPane;
	private JPanel settingsPanel;
	private JPanel liveUpdatesSettingsPanel;
	private JSpinner startTimeSpinner;
	private JSpinner endTimeSpinner;
	private JButton applySettingsButton;
	private JPanel watchlistPanel;
	private JPanel liveUpdatePanel;
	private JPanel tableActionsPanel;
	private AbstractButton openFilterButton;
	private JFrame filterWindow;
	private JButton configButton;

	private JCheckBox enableSaveLogCheckbox;
	private JTextField logDirectoryField;
	private JTextField logPathNameField;
	private JComboBox<Integer> logIntervalComboBox;
	private JCheckBox enableLogReportsCheckbox;
	private JCheckBox completeResultsCheckbox;
	private JCheckBox bullishChangeCheckbox;
	private JCheckBox bullishVolumeCheckbox;
	private ScheduledExecutorService logScheduler;
	private final Integer[] logIntervals = { 1, 15, 30, 60 }; // Minutes
	private StringBuilder consoleLogBuffer = new StringBuilder();
	
	private JTextField minVolumeField;
	private JTextField maxVolumeField;
	private JTextField emaPeriodField;
	private JTextField macdShortPeriodField;
	private JTextField macdLongPeriodField;
	private JTextField rsiPeriodField;
	private JComboBox<Integer> liveUpdateIntervalComboBox;
	private final Integer[] updateIntervals = { 1, 5, 10, 15, 30 }; // Minutes
	private Map<String, List<String>> watchlistStocks; // Map of watchlist name to stock 

	private class FilterableTableModel extends DefaultTableModel {
		private Set<String> hiddenSymbols = new HashSet<>();
		private Map<String, Integer> symbolToRowMap = new ConcurrentHashMap<>();

		public FilterableTableModel(Object[] columnNames, int rowCount) {
			super(columnNames, rowCount);
		}

		@Override
		public int getRowCount() {
			return super.getRowCount() == 0 ? 0 : super.getRowCount() - hiddenSymbols.size();
		}

		@Override
		public Object getValueAt(int row, int column) {
			int actualRow = getActualRowIndex(row);
			return super.getValueAt(actualRow, column);
		}

		private int getActualRowIndex(int visibleRow) {
			int actualRow = -1;
			int visibleCount = -1;
			while (visibleCount < visibleRow && actualRow < super.getRowCount() - 1) {
				actualRow++;
				String symbol = (String) super.getValueAt(actualRow, 1);
				if (!hiddenSymbols.contains(symbol)) {
					visibleCount++;
				}
			}
			return actualRow;
		}

		public void toggleSymbolsVisibility(Set<String> symbolsToToggle) {
			if (symbolsToToggle == null || symbolsToToggle.isEmpty()) {
				return;
			}
			for (String symbol : symbolsToToggle) {
				if (hiddenSymbols.contains(symbol)) {
					hiddenSymbols.remove(symbol);
				} else {
					hiddenSymbols.add(symbol);
				}
			}
			fireTableDataChanged();
		}

		public void showAllSymbols() {
			hiddenSymbols.clear();
			fireTableDataChanged();
		}

		public boolean isSymbolHidden(String symbol) {
			return hiddenSymbols.contains(symbol);
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return column == 0;
		}

		@Override
		public Class<?> getColumnClass(int column) {
			String columnName = dashboardTable.getColumnName(column);

			if (column == 0)
				return Boolean.class;
			if (column == 1)
				return String.class;
			if (column == 2)
				return Double.class;
			if (column == 3 || column == 4)
				return Long.class;
			if (column == 5)
				return Double.class;

			// Special handling for breakout columns
			if (columnName != null && columnName.contains("Breakout")) {
				return Integer.class; // We'll parse the breakout count as integer
			}

			return String.class;
		}

		@Override
		public void addRow(Object[] rowData) {
			super.addRow(rowData);
			String symbol = (String) rowData[1];
			symbolToRowMap.put(symbol, super.getRowCount() - 1);
			logToConsole("Added row for symbol: " + symbol + ", total rows: " + super.getRowCount());
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			int actualRow = getActualRowIndex(row);
			super.setValueAt(value, actualRow, column);
		}
	}

	private class PriceData {
		double latestPrice;
		long prevDailyVol;
		long currentDailyVol;
		double percentChange;

		PriceData(double latestPrice, long prevDailyVol, long currentDailyVol, double percentChange) {
			this.latestPrice = latestPrice;
			this.prevDailyVol = prevDailyVol;
			this.currentDailyVol = currentDailyVol;
			this.percentChange = percentChange;
		}
	}

	public StockDashboard() {
		initializeStatusColors();
		initializeWatchlists();
		initializeConfiguration();
		initializeAlertSystem();
		initializeFilterFields(); // Add this line
		setupUI();
		refreshData();
	}

	private void initializeAlertSystem() {
		// Initialize core alert components
		this.messageManager = new MessageManager();
		this.configManager = new ConfigManager();

		// Try to load alert configuration
		try {
			configManager.loadAlertConfig("config/alerts.json", statusLabel);
			this.alertManager = new AlertManager(messageManager, configManager, statusLabel);
			logToConsole("Alert system initialized successfully");
		} catch (Exception e) {
			logToConsole("Error initializing alert system: " + e.getMessage());
			// Create a fallback AlertManager with default config
			this.alertManager = new AlertManager(messageManager, new ConfigManager(), statusLabel);
		}
	}

	private void initializeStatusColors() {
		statusColors.put("Strong Uptrend", new Color(100, 255, 100));
		statusColors.put("Strong Downtrend", new Color(255, 100, 100));
		statusColors.put("Mild Uptrend", new Color(180, 255, 180));
		statusColors.put("Mild Downtrend", new Color(255, 180, 180));
		statusColors.put("Bullish", new Color(200, 255, 200));
		statusColors.put("Bearish", new Color(255, 200, 200));
		statusColors.put("Neutral", new Color(240, 240, 240));
		statusColors.put("Overbought", new Color(255, 150, 150));
		statusColors.put("Oversold", new Color(150, 255, 150));
		statusColors.put("Strong Buy", new Color(50, 200, 50));
		statusColors.put("Buy", new Color(120, 255, 120));
		statusColors.put("Sell", new Color(255, 120, 120));
		statusColors.put("Strong Sell", new Color(200, 50, 50));
		statusColors.put("↑ Uptrend", new Color(150, 255, 150));
		statusColors.put("↓ Downtrend", new Color(255, 150, 150));
	}

	private void initializeWatchlists() {
		watchlists.put("Default", Arrays.asList(symbols));
		watchlists.put("Tech Stocks", Arrays.asList("AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "META"));
		watchlists.put("Tech Stocks - Application",
				Arrays.asList("SAP", "CRM", "INTU", "NOW", "UBER", "SHOP", "ADBE", "ADP", "MSTR", "CDNS", "SNOW",
						"ADSK", "WDAY", "ROP", "TEAM", "PAYX", "DDOG", "FICO", "ANSS", "HUBS", "PTC", "TYL", "ZM",
						"GRAB", "SOUN", "WCT", "YMM", "BULL", "LYFT", "UBER", "PD", "WRD", "U", "YAAS", "SHOP",
						"FRSH"));
		watchlists.put("Tech Stocks - Infrastructure",
				Arrays.asList("LIDR", "PLTR", "MSFT", "PATH", "CORZ", "CRWV", "OKTA", "AI", "TOST", "ORCL", "RZLV", "S",
						"XYZ", "S", "PANW", "CRWD", "SNPS", "FTNT", "NET", "CRWV", "XYZ", "ZS", "VRSN", "CHKP", "CPAY",
						"GDDY", "IOT", "AFRM", "GPN", "NTNX", "TWLO", "MDB", "CYBR", "DOX", "GTLB"));
		watchlists.put("ETF", Arrays.asList("QQQ", "SPY", "IGV", "VGT", "FTEC", "SMH"));
		watchlists.put("Blue Chips", Arrays.asList("WMT", "JPM", "V", "MA", "XOM", "JNJ", "HD"));
		watchlists.put("Financial",
				Arrays.asList("V", "JPM", "MA", "AXP", "BAC", "COIN", "BLK", "WFC", "BLK", "MS", "C", "BX", "SOFI"));
		watchlists.put("SemiConductors", Arrays.asList("NVDA", "AVGO", "AMD", "TSM", "QCOM", "ARM", "MRVL", "NXPI",
				"MCHP", "ADI", "TXN", "MU", "INTC", "ON", "SWKS", "WOLF", "STM", "TXN"));
		watchlists.put("Health Care", Arrays.asList("LLY", "JNJ", "ABBV", "AMGN", "MRK", "PFE", "GILD", "BMY", "BIIB"));
		watchlists.put("Energy", Arrays.asList("XOM", "COP", "CVX", "TPL", "EOG", "HES", "FANG", "MPC", "PSX", "VLO"));
		watchlists.put("HighVolumes", Arrays.asList("CYCC", "XAGE", "KAPA", "PLRZ", "ANPA", "IBG", "CRML", "GRO",
				"USAR", "SBET", "MP", "SLDP", "OPEN", "TTD", "PROK"));
		watchlists.put("Top50",
				Arrays.asList("NVDA", "MSFT", "AAPL", "AMZN", "GOOGL", "META", "2223.SR", "AVGO", "TSM", "TSLA",
						"BRK-B", "JPM", "WMT", "ORCL", "LLY", "V", "TCEHY", "NFLX", "MA", "XOM", "COST", "JNJ", "PG",
						"PLTR", "1398.HK", "SAP", "HD", "BAC", "ABBV", "005930.KS", "KO", "601288.SS", "RMS.PA", "NVO",
						"ASML", "601939.SS", "BABA", "PM", "GE", "CSCO", "CVX", "IBM", "UNH", "AMD", "ROG.SW", "TMUS",
						"WFC", "NESN.SW", "PRX.AS"));
		watchlists.put("YahooDOW", Arrays.asList(""));
		watchlists.put("YahooNASDAQ", Arrays.asList(""));
		watchlists.put("YahooSP500", Arrays.asList(""));
		watchlists.put("YahooRussel2k", Arrays.asList(""));
		watchlists.put("YahooPennyBy3MonVol", Arrays.asList(""));
		watchlists.put("YahooPennyByPercentChange", Arrays.asList(""));

		currentWatchlist = "Default";
	}

	private void initializeConfiguration() {
		// Initialize timeframe-indicator mappings
		for (String tf : allTimeframes) {
			timeframeIndicators.put(tf, new HashSet<>());

			// Initialize indicator checkboxes map for each timeframe
			Map<String, JCheckBox> indMap = new HashMap<>();
			for (String ind : allIndicators) {
				indMap.put(ind, new JCheckBox(ind));
			}
			indicatorCheckboxes.put(tf, indMap);
		}

		// Set default selections (optional)
		timeframeIndicators.get("1D").addAll(Arrays.asList("RSI", "MACD", "MACD Breakout", "MACD(5,8,9)",
				"MACD(5,8,9) Breakout", "PSAR(0.05)", "PSAR(0.05) Breakout", "Breakout Count"));
		timeframeIndicators.get("4H").addAll(Arrays.asList("RSI", "MACD", "MACD(5,8,9)", "PSAR(0.05)", "PSAR(0.01)"));
		timeframeIndicators.get("30Min").addAll(Arrays.asList("MACD", "MACD(5,8,9)", "PSAR(0.05)", "PSAR(0.01)"));
	}

	private void initializeFilterFields() {
		prevVolMinField = new JTextField(10);
		prevVolMaxField = new JTextField(10);
		currentVolMinField = new JTextField(10);
		currentVolMaxField = new JTextField(10);

		// Set default values if needed
		prevVolMinField.setText("");
		prevVolMaxField.setText("");
		currentVolMinField.setText("");
		currentVolMaxField.setText("");
	}

	private void setupUI() {
		// 1. Main window configuration
		setTitle("Enhanced Stock Dashboard (IEX Feed)");
		setSize(1200, 700);
		setMinimumSize(new Dimension(800, 600));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		// 2. Create menu bar
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenuItem saveItem = new JMenuItem("Save Results to CSV");
		saveItem.addActionListener(e -> saveTableToCSV());
		fileMenu.add(saveItem);
		menuBar.add(fileMenu);
		setJMenuBar(menuBar);

		// 3. Create main tabbed pane
		tabbedPane = new JTabbedPane();

		// 4. Create Dashboard tab content
		JPanel dashboardPanel = new JPanel(new BorderLayout());

		// 4.1 Time range panel at top
		setupTimeRangePanel();
		dashboardPanel.add(timeRangePanel, BorderLayout.NORTH);

		// 4.2 Control panels (watchlist, live updates, table actions)
		setupControlPanel();

		// 4.3 Create a container for control panels and filters
		JPanel topContainer = new JPanel(new BorderLayout());
		topContainer.add(controlPanel, BorderLayout.CENTER);

		// 4.4 Volume filters panel
		// setupFiltersPanel();
		// topContainer.add(filtersPanel, BorderLayout.SOUTH);

		dashboardPanel.add(topContainer, BorderLayout.CENTER);

		// 4.5 Main table and console (in a split pane)
		setupMainContentArea();
		JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainSplitPane.setTopComponent(new JScrollPane(dashboardTable));
		mainSplitPane.setBottomComponent(consolePanel);
		mainSplitPane.setDividerLocation(0.7);
		mainSplitPane.setResizeWeight(0.7);
		mainSplitPane.setDividerSize(5);

		dashboardPanel.add(mainSplitPane, BorderLayout.SOUTH);

		// 5. Add Dashboard tab
		tabbedPane.addTab("Dashboard", dashboardPanel);

		// 6. Create and add Settings tab
		setupSettingsPanel();
		tabbedPane.addTab("Settings", settingsPanel);

		// 7. Add tabbed pane to main frame
		add(tabbedPane, BorderLayout.CENTER);

		// 8. Initialize column indices
		initializeColumnIndices();

		// 9. Add component resize listener
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				controlPanel.revalidate();
				controlPanel.repaint();
				logToConsole("Window resized to: " + getSize());
			}
		});
	}

	private void setupSettingsPanel() {
	    settingsPanel = new JPanel(new BorderLayout());
	    settingsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

	    // Main container for settings
	    JPanel settingsContainer = new JPanel();
	    settingsContainer.setLayout(new BoxLayout(settingsContainer, BoxLayout.Y_AXIS));

	    // 1. Live Updates Settings Panel
	    liveUpdatesSettingsPanel = new JPanel(new GridBagLayout());
	    liveUpdatesSettingsPanel.setBorder(BorderFactory.createTitledBorder("Live Updates Settings"));
	    GridBagConstraints gbc = new GridBagConstraints();
	    gbc.insets = new Insets(5, 5, 5, 5);
	    gbc.anchor = GridBagConstraints.WEST;
	    gbc.fill = GridBagConstraints.HORIZONTAL;

	    // Time restriction components
	    JLabel timeRestrictionLabel = new JLabel("Live Update Time Restrictions:");
	    JLabel startTimeLabel = new JLabel("Start Time:");
	    JLabel endTimeLabel = new JLabel("End Time:");

	    // Create time spinners (24-hour format)
	    SpinnerDateModel startModel = new SpinnerDateModel();
	    startTimeSpinner = new JSpinner(startModel);
	    startTimeSpinner.setEditor(new JSpinner.DateEditor(startTimeSpinner, "HH:mm"));

	    SpinnerDateModel endModel = new SpinnerDateModel();
	    endTimeSpinner = new JSpinner(endModel);
	    endTimeSpinner.setEditor(new JSpinner.DateEditor(endTimeSpinner, "HH:mm"));

	    // Apply button
	    applySettingsButton = new JButton("Apply Settings");
	    applySettingsButton.addActionListener(e -> saveLiveUpdateSettings());

	    // Layout for Live Updates Settings
	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.gridwidth = 2;
	    liveUpdatesSettingsPanel.add(timeRestrictionLabel, gbc);

	    gbc.gridwidth = 1;
	    gbc.gridy = 1;
	    liveUpdatesSettingsPanel.add(startTimeLabel, gbc);

	    gbc.gridx = 1;
	    liveUpdatesSettingsPanel.add(startTimeSpinner, gbc);

	    gbc.gridx = 0;
	    gbc.gridy = 2;
	    liveUpdatesSettingsPanel.add(endTimeLabel, gbc);

	    gbc.gridx = 1;
	    liveUpdatesSettingsPanel.add(endTimeSpinner, gbc);

	    gbc.gridx = 0;
	    gbc.gridy = 3;
	    gbc.gridwidth = 2;
	    gbc.anchor = GridBagConstraints.CENTER;
	    liveUpdatesSettingsPanel.add(applySettingsButton, gbc);

	    // Add to container
	    settingsContainer.add(liveUpdatesSettingsPanel);
	    settingsContainer.add(Box.createVerticalStrut(20)); // Spacer

	    // 2. Logger and Report Settings Panel
	    JPanel loggerSettingsPanel = new JPanel(new GridBagLayout());
	    loggerSettingsPanel.setBorder(BorderFactory.createTitledBorder("Logger and Report Settings"));
	    GridBagConstraints loggerGbc = new GridBagConstraints();
	    loggerGbc.insets = new Insets(5, 5, 5, 5);
	    loggerGbc.anchor = GridBagConstraints.WEST;
	    loggerGbc.fill = GridBagConstraints.HORIZONTAL;

	    // Enable Save Log Checkbox
	    enableSaveLogCheckbox = new JCheckBox("Enable Save Log");
	    enableSaveLogCheckbox.addActionListener(e -> {
	        boolean enabled = enableSaveLogCheckbox.isSelected();
	        logDirectoryField.setEnabled(enabled);
	        logPathNameField.setEnabled(enabled);
	        logIntervalComboBox.setEnabled(enabled);
	        if (!enabled && logScheduler != null) {
	            stopLogScheduler();
	        }
	    });

	    // Directory Field
	    logDirectoryField = new JTextField(20);
	    logDirectoryField.setEnabled(false);
	    
	    // Path Name Field
	    logPathNameField = new JTextField(20);
	    logPathNameField.setEnabled(false);
	    
	    // Interval ComboBox
	    logIntervalComboBox = new JComboBox<>(logIntervals);
	    logIntervalComboBox.setEnabled(false);
	    
	    // Enable Log Reports Checkbox
	    enableLogReportsCheckbox = new JCheckBox("Enable Log Reports");
	    enableLogReportsCheckbox.addActionListener(e -> {
	        boolean enabled = enableLogReportsCheckbox.isSelected();
	        completeResultsCheckbox.setEnabled(enabled);
	        bullishChangeCheckbox.setEnabled(enabled);
	        bullishVolumeCheckbox.setEnabled(enabled);
	    });

	    // Result Type Checkboxes
	    completeResultsCheckbox = new JCheckBox("Complete Results");
	    completeResultsCheckbox.setEnabled(false);
	    bullishChangeCheckbox = new JCheckBox("Bullish, Sort by Change");
	    bullishChangeCheckbox.setEnabled(false);
	    bullishVolumeCheckbox = new JCheckBox("Bullish, Sort by Volume");
	    bullishVolumeCheckbox.setEnabled(false);

	    // Layout for Logger Settings
	    loggerGbc.gridx = 0;
	    loggerGbc.gridy = 0;
	    loggerGbc.gridwidth = 2;
	    loggerSettingsPanel.add(enableSaveLogCheckbox, loggerGbc);

	    loggerGbc.gridwidth = 1;
	    loggerGbc.gridy = 1;
	    loggerGbc.gridx = 0;
	    loggerSettingsPanel.add(new JLabel("Directory:"), loggerGbc);
	    loggerGbc.gridx = 1;
	    loggerSettingsPanel.add(logDirectoryField, loggerGbc);

	    loggerGbc.gridy = 2;
	    loggerGbc.gridx = 0;
	    loggerSettingsPanel.add(new JLabel("Path Name:"), loggerGbc);
	    loggerGbc.gridx = 1;
	    loggerSettingsPanel.add(logPathNameField, loggerGbc);

	    loggerGbc.gridy = 3;
	    loggerGbc.gridx = 0;
	    loggerSettingsPanel.add(new JLabel("Interval (mins):"), loggerGbc);
	    loggerGbc.gridx = 1;
	    loggerSettingsPanel.add(logIntervalComboBox, loggerGbc);

	    loggerGbc.gridy = 4;
	    loggerGbc.gridx = 0;
	    loggerGbc.gridwidth = 2;
	    loggerSettingsPanel.add(enableLogReportsCheckbox, loggerGbc);

	    loggerGbc.gridy = 5;
	    loggerSettingsPanel.add(completeResultsCheckbox, loggerGbc);

	    loggerGbc.gridy = 6;
	    loggerSettingsPanel.add(bullishChangeCheckbox, loggerGbc);

	    loggerGbc.gridy = 7;
	    loggerSettingsPanel.add(bullishVolumeCheckbox, loggerGbc);

	    // Apply button for logger settings
	    JButton applyLoggerSettingsButton = new JButton("Apply Logger Settings");
	    applyLoggerSettingsButton.addActionListener(e -> applyLoggerSettings());
	    loggerGbc.gridy = 8;
	    loggerGbc.gridx = 0;
	    loggerGbc.gridwidth = 2;
	    loggerGbc.anchor = GridBagConstraints.CENTER;
	    loggerSettingsPanel.add(applyLoggerSettingsButton, loggerGbc);

	    // Add to container
	    settingsContainer.add(loggerSettingsPanel);
	    settingsContainer.add(Box.createVerticalStrut(20)); // Spacer

	    // Add container to main settings panel
	    settingsPanel.add(new JScrollPane(settingsContainer), BorderLayout.CENTER);

	    // Load saved settings
	    loadLiveUpdateSettings();
	    loadLoggerSettings();
	    applyLoggerSettings();
	}

	private void loadLoggerSettings() {
	    try {
	        Preferences prefs = Preferences.userNodeForPackage(StockDashboard.class);
	        enableSaveLogCheckbox.setSelected(prefs.getBoolean("enableSaveLog", false));
	        logDirectoryField.setText(prefs.get("logDirectory", System.getProperty("user.home")));
	        logPathNameField.setText(prefs.get("logPathName", "stock_dashboard"));
	        logIntervalComboBox.setSelectedItem(prefs.getInt("logInterval", 15));
	        enableLogReportsCheckbox.setSelected(prefs.getBoolean("enableLogReports", false));
	        completeResultsCheckbox.setSelected(prefs.getBoolean("completeResults", false));
	        bullishChangeCheckbox.setSelected(prefs.getBoolean("bullishChange", false));
	        bullishVolumeCheckbox.setSelected(prefs.getBoolean("bullishVolume", false));

	        // Enable/disable fields based on checkbox states
	        boolean logEnabled = enableSaveLogCheckbox.isSelected();
	        logDirectoryField.setEnabled(logEnabled);
	        logPathNameField.setEnabled(logEnabled);
	        logIntervalComboBox.setEnabled(logEnabled);

	        boolean reportsEnabled = enableLogReportsCheckbox.isSelected();
	        completeResultsCheckbox.setEnabled(reportsEnabled);
	        bullishChangeCheckbox.setEnabled(reportsEnabled);
	        bullishVolumeCheckbox.setEnabled(reportsEnabled);

	        logToConsole("Loaded logger settings");
	    } catch (Exception e) {
	        logToConsole("Error loading logger settings: " + e.getMessage());
	    }
	}

	// New method to save logger settings
	private void saveLoggerSettings() {
	    try {
	        Preferences prefs = Preferences.userNodeForPackage(StockDashboard.class);
	        prefs.putBoolean("enableSaveLog", enableSaveLogCheckbox.isSelected());
	        prefs.put("logDirectory", logDirectoryField.getText());
	        prefs.put("logPathName", logPathNameField.getText());
	        prefs.putInt("logInterval", (Integer) logIntervalComboBox.getSelectedItem());
	        prefs.putBoolean("enableLogReports", enableLogReportsCheckbox.isSelected());
	        prefs.putBoolean("completeResults", completeResultsCheckbox.isSelected());
	        prefs.putBoolean("bullishChange", bullishChangeCheckbox.isSelected());
	        prefs.putBoolean("bullishVolume", bullishVolumeCheckbox.isSelected());

	        logToConsole("Logger settings saved successfully");
	    } catch (Exception e) {
	        logToConsole("Error saving logger settings: " + e.getMessage());
	    }
	}

	// New method to apply logger settings
	private void applyLoggerSettings() {
	    saveLoggerSettings();
	    
	    if (enableSaveLogCheckbox.isSelected() || enableLogReportsCheckbox.isSelected()) {
	        startLogScheduler();
	    } else {
	        stopLogScheduler();
	    }
	}

	private void startLogScheduler() {
	    if (logScheduler != null) {
	        stopLogScheduler();
	    }

	    if (!enableSaveLogCheckbox.isSelected() && !enableLogReportsCheckbox.isSelected()) {
	        return;
	    }

	    logScheduler = Executors.newSingleThreadScheduledExecutor();
	    int interval = (Integer) logIntervalComboBox.getSelectedItem();
	    logScheduler.scheduleAtFixedRate(() -> {
	        SwingUtilities.invokeLater(() -> {
	            if (enableSaveLogCheckbox.isSelected()) {
	                saveConsoleLog();
	            }
	            if (enableLogReportsCheckbox.isSelected()) {
	                saveReportFiles();
	            }
	        });
	    }, 0, interval, TimeUnit.MINUTES);

	    logToConsole("Log scheduler started with interval: " + interval + " minutes");
	}

	private void saveConsoleLog() {
		if (consoleLogBuffer.length() == 0) {
			return;
		}

		String directory = logDirectoryField.getText();
		String pathName = logPathNameField.getText();
		String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String fileName = String.format("%s_%s.log", pathName, timestamp);

		File dir = new File(directory);
		if (!dir.exists() && !dir.mkdirs()) {
			logToConsole("Error: Could not create directory: " + directory);
			return;
		}

		File file = new File(dir, fileName);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(consoleLogBuffer.toString());
			logToConsole("Console log saved to: " + file.getAbsolutePath());
			consoleLogBuffer.setLength(0); // Clear buffer after saving
		} catch (IOException e) {
			logToConsole("Error saving console log: " + e.getMessage());
		}
	}

	// New method to save report files
	private void saveReportFiles() {
	    String directory = logDirectoryField.getText();
	    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File dir = new File(directory);
	    
	    if (!dir.exists() && !dir.mkdirs()) {
	        logToConsole("Error: Could not create directory: " + directory);
	        return;
	    }

	    // Get the current watchlist name
	    String watchlistName = watchlistComboBox.getSelectedItem() != null 
	        ? watchlistComboBox.getSelectedItem().toString().replaceAll("[^a-zA-Z0-9]", "_") 
	        : "default";

	    if (completeResultsCheckbox.isSelected()) {
	        saveTableToCSV(new File(dir, String.format("%s_complete_results_%s.csv", watchlistName, timestamp)), false, false);
	    }
	    
	    if (bullishChangeCheckbox.isSelected()) {
	        saveTableToCSV(new File(dir, String.format("%s_bullish_change_%s.csv", watchlistName, timestamp)), true, true);
	    }
	    
	    if (bullishVolumeCheckbox.isSelected()) {
	        saveTableToCSV(new File(dir, String.format("%s_bullish_volume_%s.csv", watchlistName, timestamp)), true, false);
	    }
	}

	// Modified saveTableToCSV to handle different report types
	private void saveTableToCSV(File fileToSave, boolean filterBullish, boolean sortByChange) {
	    try (PrintWriter writer = new PrintWriter(fileToSave)) {
	        // Write CSV header
	        for (int i = 0; i < dashboardTable.getColumnCount(); i++) {
	            writer.print(dashboardTable.getColumnName(i));
	            if (i < dashboardTable.getColumnCount() - 1) {
	                writer.print(",");
	            }
	        }
	        writer.println();

	        // Get rows to save
	        List<Integer> rowsToSave = getVisibleRowsInOrder();
	        
	        if (filterBullish) {
	            rowsToSave = rowsToSave.stream()
	                .filter(this::isRowBullish)
	                .collect(Collectors.toList());
	        }

	        if (sortByChange) {
	            rowsToSave.sort((r1, r2) -> {
	                Double change1 = (Double) dashboardTable.getValueAt(r1, 5);
	                Double change2 = (Double) dashboardTable.getValueAt(r2, 5);
	                return change2.compareTo(change1); // Descending order
	            });
	        } else if (filterBullish) {
	            rowsToSave.sort((r1, r2) -> {
	                Long volume1 = (Long) dashboardTable.getValueAt(r1, 4);
	                Long volume2 = (Long) dashboardTable.getValueAt(r2, 4);
	                return volume2.compareTo(volume1); // Descending order
	            });
	        }

	        // Write data rows
	        for (int row : rowsToSave) {
	            for (int col = 0; col < dashboardTable.getColumnCount(); col++) {
	                Object value = dashboardTable.getValueAt(row, col);

	                // Handle special cases
	                if (value instanceof Boolean) {
	                    writer.print(((Boolean) value) ? "1" : "0");
	                } else if (value instanceof BreakoutValue) {
	                    writer.print(((BreakoutValue) value).displayValue);
	                } else {
	                    // Escape quotes and commas in strings
	                    String strValue = String.valueOf(value).replace("\"", "\"\"").replace(",", "\\,");

	                    // Quote strings that contain commas or quotes
	                    if (strValue.contains(",") || strValue.contains("\"")) {
	                        strValue = "\"" + strValue + "\"";
	                    }
	                    writer.print(strValue);
	                }

	                if (col < dashboardTable.getColumnCount() - 1) {
	                    writer.print(",");
	                }
	            }
	            writer.println();
	        }

	        logToConsole("Results saved to: " + fileToSave.getAbsolutePath());
	    } catch (IOException ex) {
	        logToConsole("Error saving report file: " + ex.getMessage());
	    }
	}

	// New helper method to check if a row is bullish
	private boolean isRowBullish(int row) {
		for (int col : indicatorColumnIndices) {
			Object value = dashboardTable.getValueAt(row, col);
			if (value != null && BULLISH_STATUSES.contains(value.toString())) {
				return true;
			}
		}
		return false;
	}

	// New method to stop log scheduler
	private void stopLogScheduler() {
		if (logScheduler != null) {
			logScheduler.shutdown();
			try {
				if (!logScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					logScheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				logScheduler.shutdownNow();
				logToConsole("Error stopping log scheduler: " + e.getMessage());
			}
			logScheduler = null;
			logToConsole("Log scheduler stopped");
		}
	}

	private void saveLiveUpdateSettings() {
		try {
			// Get the time values
			Date startTime = ((SpinnerDateModel) startTimeSpinner.getModel()).getDate();
			Date endTime = ((SpinnerDateModel) endTimeSpinner.getModel()).getDate();

			// Convert to LocalTime
			LocalTime start = LocalTime.ofInstant(startTime.toInstant(), ZoneId.systemDefault());
			LocalTime end = LocalTime.ofInstant(endTime.toInstant(), ZoneId.systemDefault());

			// Validate time range
			if (start.equals(end)) {
				JOptionPane.showMessageDialog(this, "Start and end times cannot be the same", "Invalid Settings",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			// Save to preferences
			Preferences prefs = Preferences.userNodeForPackage(StockDashboard.class);
			prefs.put("liveUpdateEndTime", end.toString());

			JOptionPane.showMessageDialog(this, "Live update settings saved successfully!\n"
					+ "Updates will only run between " + start + " and " + end, "Success",
					JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error saving settings: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void loadLiveUpdateSettings() {
		try {
			Preferences prefs = Preferences.userNodeForPackage(StockDashboard.class);
			String startStr = prefs.get("liveUpdateStartTime", "09:30");
			String endStr = prefs.get("liveUpdateEndTime", "16:00");

			LocalTime start = LocalTime.parse(startStr);
			LocalTime end = LocalTime.parse(endStr);

			// Set spinner values
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, start.getHour());
			cal.set(Calendar.MINUTE, start.getMinute());
			startTimeSpinner.setValue(cal.getTime());

			cal.set(Calendar.HOUR_OF_DAY, end.getHour());
			cal.set(Calendar.MINUTE, end.getMinute());
			endTimeSpinner.setValue(cal.getTime());

			logToConsole("Loaded live update time range: " + start + " to " + end);
		} catch (Exception e) {
			logToConsole("Error loading live update settings: " + e.getMessage());
			// Set defaults if loading fails
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 9);
			cal.set(Calendar.MINUTE, 30);
			startTimeSpinner.setValue(cal.getTime());

			cal.set(Calendar.HOUR_OF_DAY, 16);
			cal.set(Calendar.MINUTE, 0);
			endTimeSpinner.setValue(cal.getTime());
		}
	}

	private void setupTimeRangePanel() {
		timeRangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
		timeRangePanel.setPreferredSize(new Dimension(1200, 50));

		// Time range components
		currentTimeRadio = new JRadioButton("Current Time", true);
		customRangeRadio = new JRadioButton("Custom Range");
		timeRangeGroup = new ButtonGroup();
		timeRangeGroup.add(currentTimeRadio);
		timeRangeGroup.add(customRangeRadio);

		// Date pickers
		startDatePicker = new JXDatePicker();
		startDatePicker.setFormats("yyyy-MM-dd");
		startDatePicker.setEnabled(false);
		endDatePicker = new JXDatePicker();
		endDatePicker.setFormats("yyyy-MM-dd");
		endDatePicker.setEnabled(false);

		// Clear button
		clearCustomRangeButton = new JButton("Clear Custom Range");
		clearCustomRangeButton.setEnabled(false);

		// Status label
		timeRangeLabel = new JLabel("Time Range: Current Time");

		// Add components
		timeRangePanel.add(new JLabel("Time Range:"));
		timeRangePanel.add(currentTimeRadio);
		timeRangePanel.add(customRangeRadio);
		timeRangePanel.add(new JLabel("Start:"));
		timeRangePanel.add(startDatePicker);
		timeRangePanel.add(new JLabel("End:"));
		timeRangePanel.add(endDatePicker);
		timeRangePanel.add(clearCustomRangeButton);
		timeRangePanel.add(timeRangeLabel);

		// Add listeners
		currentTimeRadio.addActionListener(e -> {
			startDatePicker.setEnabled(false);
			endDatePicker.setEnabled(false);
			clearCustomRangeButton.setEnabled(false);
			timeRangeLabel.setText("Time Range: Current Time");
		});

		customRangeRadio.addActionListener(e -> {
			startDatePicker.setEnabled(true);
			endDatePicker.setEnabled(true);
			clearCustomRangeButton.setEnabled(true);
			updateTimeRangeLabel();
		});

		clearCustomRangeButton.addActionListener(e -> {
			startDatePicker.setDate(null);
			endDatePicker.setDate(null);
			updateTimeRangeLabel();
		});

		add(timeRangePanel, BorderLayout.NORTH);
	}

	private void setupControlPanel() {
		controlPanel = new JPanel(new BorderLayout());

		// Initialize all sub-panels
		watchlistPanel = new JPanel(new GridBagLayout());
		watchlistPanel.setBorder(BorderFactory.createTitledBorder("Watchlist"));

		liveUpdatePanel = new JPanel(new GridBagLayout());
		liveUpdatePanel.setBorder(BorderFactory.createTitledBorder("Live Updates"));

		tableActionsPanel = new JPanel(new GridBagLayout());
		tableActionsPanel.setBorder(BorderFactory.createTitledBorder("Table Actions"));

		// Setup panel contents
		setupWatchlistPanel();
		setupLiveUpdatePanel();
		setupTableActionsPanel();

		// Combine them in a grid layout
		JPanel controlsGrid = new JPanel(new GridLayout(1, 3, 10, 10));
		controlsGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		controlsGrid.add(watchlistPanel);
		controlsGrid.add(liveUpdatePanel);
		controlsGrid.add(tableActionsPanel);

		// Status label at bottom
		statusLabel = new JLabel("Status: Ready");
		statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

		controlPanel.add(controlsGrid, BorderLayout.CENTER);
		controlPanel.add(statusLabel, BorderLayout.SOUTH);
	}

	private void setupWatchlistPanel() {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Data source combo
		dataSourceComboBox = new JComboBox<>(dataSources);
		dataSourceComboBox.setSelectedItem(currentDataSource);
		dataSourceComboBox.addActionListener(e -> {
			currentDataSource = (String) dataSourceComboBox.getSelectedItem();
			refreshData();
		});

		// Watchlist combo
		watchlistComboBox = new JComboBox<>(watchlists.keySet().toArray(new String[0]));
		watchlistComboBox.setSelectedItem(currentWatchlist);
		watchlistComboBox.addActionListener(e -> {
			currentWatchlist = (String) watchlistComboBox.getSelectedItem();
			refreshData();
		});

		// Buttons with action listeners
		addWatchlistButton = createButton("Add", new Color(100, 149, 237));
		addWatchlistButton.addActionListener(e -> addNewWatchlist());

		editWatchlistButton = createButton("Edit", new Color(100, 149, 237));
		editWatchlistButton.addActionListener(e -> editCurrentWatchlist());

		// Layout
		gbc.gridx = 0;
		gbc.gridy = 0;
		watchlistPanel.add(new JLabel("Data Source:"), gbc);

		gbc.gridx = 1;
		watchlistPanel.add(dataSourceComboBox, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		watchlistPanel.add(new JLabel("Watchlist:"), gbc);

		gbc.gridx = 1;
		watchlistPanel.add(watchlistComboBox, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		watchlistPanel.add(addWatchlistButton, gbc);

		gbc.gridx = 1;
		watchlistPanel.add(editWatchlistButton, gbc);
	}

	private void setupLiveUpdatePanel() {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Live update controls
		startLiveButton = createButton("Start", new Color(50, 200, 50));
		stopLiveButton = createButton("Stop", new Color(200, 50, 50));
		stopLiveButton.setEnabled(false);

		refreshIntervalComboBox = new JComboBox<>(refreshIntervals);
		refreshIntervalComboBox.setSelectedItem(currentRefreshInterval);
		refreshIntervalComboBox.addActionListener(e -> {
			currentRefreshInterval = (Integer) refreshIntervalComboBox.getSelectedItem();
			if (isLive) {
				stopLiveUpdates();
				startLiveUpdates();
			}
		});

		// Checkboxes
		checkBullishCheckbox = new JCheckBox("Check Bullish");
		alarmOnCheckbox = new JCheckBox("Alarm On");
		alarmOnCheckbox.addActionListener(e -> {
			isAlarmActive = alarmOnCheckbox.isSelected();
			logToConsole("Alarm active status changed to: " + isAlarmActive);
		});

		// Alarm controls
		stopAlarmButton = createButton("Stop Alarm", new Color(200, 50, 50));
		stopAlarmButton.addActionListener(e -> {
			isAlarmActive = false;
			alarmOnCheckbox.setSelected(false);
			stopBuzzer();
			logToConsole("Alarm stopped.");
		});

		symbolExceptionsField = new JTextField(8);

		// Start/Stop button actions
		startLiveButton.addActionListener(e -> {
			if (checkBullishCheckbox.isSelected()) {
				isAlarmActive = alarmOnCheckbox.isSelected();
				if (isAlarmActive && alertManager == null) {
					logToConsole("AlertManager is null, cannot enable alarms");
					isAlarmActive = false;
				}
			} else {
				isAlarmActive = false;
			}
			startLiveUpdates();
			statusLabel.setText("Status: Live updates running - last update at "
					+ LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
		});

		stopLiveButton.addActionListener(e -> {
			stopLiveUpdates();
			statusLabel.setText("Status: Live updates stopped at "
					+ LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
		});

		// Layout
		gbc.gridx = 0;
		gbc.gridy = 0;
		liveUpdatePanel.add(startLiveButton, gbc);

		gbc.gridx = 1;
		liveUpdatePanel.add(stopLiveButton, gbc);

		gbc.gridx = 2;
		liveUpdatePanel.add(new JLabel("Refresh (s):"), gbc);

		gbc.gridx = 3;
		liveUpdatePanel.add(refreshIntervalComboBox, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		liveUpdatePanel.add(checkBullishCheckbox, gbc);

		gbc.gridx = 1;
		liveUpdatePanel.add(alarmOnCheckbox, gbc);

		gbc.gridx = 2;
		gbc.gridwidth = 2;
		liveUpdatePanel.add(stopAlarmButton, gbc);

		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 2;
		liveUpdatePanel.add(new JLabel("Symbol Exceptions:"), gbc);

		gbc.gridx = 1;
		gbc.gridwidth = 3;
		liveUpdatePanel.add(symbolExceptionsField, gbc);
	}

	private void setupTableActionsPanel() {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Action buttons with all listeners
		refreshButton = createButton("Refresh", new Color(70, 130, 180));
		refreshButton.addActionListener(e -> {
			statusLabel.setText("Status: Refreshing data...");
			refreshData();
			statusLabel.setText(
					"Status: Data refreshed at " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
		});

		toggleVisibilityButton = createButton("Toggle Selected", new Color(255, 165, 0));
		toggleVisibilityButton.addActionListener(e -> toggleSelectedRowsVisibility());

		showAllButton = createButton("Show All", new Color(100, 149, 237));
		showAllButton.addActionListener(e -> showAllRows());

		listSymbolsButton = createButton("List Symbols", new Color(100, 149, 237));
		listSymbolsButton.addActionListener(e -> listVisibleSymbols());

		checkNonBullishButton = createButton("Check Non-Bullish", new Color(100, 149, 237));
		checkNonBullishButton.addActionListener(e -> checkBullishUptrendNeutralRows());

		uncheckAllButton = createButton("Uncheck All", new Color(100, 149, 237));
		uncheckAllButton.addActionListener(e -> uncheckAllRows());

		// Add Open Filter button here
		openFilterButton = createButton("Open Filter", new Color(100, 149, 237));
		openFilterButton.addActionListener(e -> openFilterWindow());

		configButton = createButton("Configure Indicators", new Color(100, 149, 237));
		configButton.addActionListener(e -> openConfigWindow());

		// Layout - 3 columns
		gbc.gridx = 0;
		gbc.gridy = 0;
		tableActionsPanel.add(refreshButton, gbc);

		gbc.gridx = 1;
		tableActionsPanel.add(toggleVisibilityButton, gbc);

		gbc.gridx = 2;
		tableActionsPanel.add(showAllButton, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		tableActionsPanel.add(listSymbolsButton, gbc);

		gbc.gridx = 1;
		tableActionsPanel.add(checkNonBullishButton, gbc);

		gbc.gridx = 2;
		tableActionsPanel.add(uncheckAllButton, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		tableActionsPanel.add(openFilterButton, gbc);

		gbc.gridx = 1;
		tableActionsPanel.add(configButton, gbc);

		// Empty cell to maintain grid alignment
		gbc.gridx = 2;
		tableActionsPanel.add(new JLabel(), gbc);
	}

	private void setupFiltersPanel() {
		filtersPanel = new JPanel(new GridBagLayout());
		filtersPanel.setBorder(BorderFactory.createTitledBorder("Volume Filters"));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Volume filter fields
		prevVolMinField = new JTextField(10);
		prevVolMaxField = new JTextField(10);
		currentVolMinField = new JTextField(10);
		currentVolMaxField = new JTextField(10);

		// Tooltips
		prevVolMinField.setToolTipText("Minimum previous daily volume");
		prevVolMaxField.setToolTipText("Maximum previous daily volume");
		currentVolMinField.setToolTipText("Minimum current daily volume");
		currentVolMaxField.setToolTipText("Maximum current daily volume");

		// Apply button
		JButton applyFiltersButton = createButton("Apply Filters", new Color(70, 130, 180));
		applyFiltersButton.addActionListener(e -> {
			refreshData();
			logToConsole("Volume filters applied");
		});

		// Clear button
		JButton clearFiltersButton = createButton("Clear Filters", new Color(200, 200, 200));
		clearFiltersButton.addActionListener(e -> {
			prevVolMinField.setText("");
			prevVolMaxField.setText("");
			currentVolMinField.setText("");
			currentVolMaxField.setText("");
			refreshData();
			logToConsole("Volume filters cleared");
		});

		// Layout
		gbc.gridx = 0;
		gbc.gridy = 0;
		filtersPanel.add(new JLabel("Prev Daily Vol:"), gbc);

		gbc.gridx = 1;
		filtersPanel.add(prevVolMinField, gbc);

		gbc.gridx = 2;
		filtersPanel.add(new JLabel("to"), gbc);

		gbc.gridx = 3;
		filtersPanel.add(prevVolMaxField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		filtersPanel.add(new JLabel("Current Daily Vol:"), gbc);

		gbc.gridx = 1;
		filtersPanel.add(currentVolMinField, gbc);

		gbc.gridx = 2;
		filtersPanel.add(new JLabel("to"), gbc);

		gbc.gridx = 3;
		filtersPanel.add(currentVolMaxField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		filtersPanel.add(applyFiltersButton, gbc);

		gbc.gridx = 2;
		filtersPanel.add(clearFiltersButton, gbc);
	}

	private void setupMainContentArea() {
		// Table setup
		String[] columnNames = createColumnNames();
		tableModel = new FilterableTableModel(columnNames, 0);
		dashboardTable = new JTable(tableModel) {
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				String symbol = (String) getValueAt(row, 1);
				if (((FilterableTableModel) getModel()).isSymbolHidden(symbol)) {
					return new JLabel();
				}

				Component c = super.prepareRenderer(renderer, row, column);

				if (column == 0) {
					Boolean value = (Boolean) getValueAt(row, column);
					JCheckBox checkBox = new JCheckBox();
					checkBox.setSelected(value != null && value);
					checkBox.setHorizontalAlignment(JLabel.CENTER);
					checkBox.setBackground(getBackground());
					return checkBox;
				}

				if (column == 1) {
					c.setBackground(new Color(245, 245, 245));
					c.setForeground(Color.BLACK);
					return c;
				}

				if (column == 2) {
					Object value = getValueAt(row, column);
					if (value instanceof Double) {
						c = super.prepareRenderer(renderer, row, column);
						((JLabel) c).setText(String.format("%.2f", value));
						c.setBackground(Color.WHITE);
						c.setForeground(Color.BLACK);
						return c;
					}
				}

				if (column == 3 || column == 4) {
					Object value = getValueAt(row, column);
					if (value instanceof Long) {
						c = super.prepareRenderer(renderer, row, column);
						((JLabel) c).setText(String.format("%d", value));
						c.setBackground(Color.WHITE);
						c.setForeground(Color.BLACK);
						return c;
					}
				}

				if (column == 5) {
					Object value = getValueAt(row, column);
					if (value instanceof Double) {
						c = super.prepareRenderer(renderer, row, column);
						double percent = (Double) value;
						((JLabel) c).setText(String.format("%.2f%%", percent));
						c.setBackground(percent >= 0 ? BULLISH_COLOR : BEARISH_COLOR);
						c.setForeground(Color.BLACK);
						return c;
					}
				}

				Object value = getValueAt(row, column);
				String status = (value != null) ? value.toString().trim() : "";
				if (status.startsWith("↑") || status.startsWith("↓")) {
					String trend = status.split(" ")[0] + " " + status.split(" ")[1];
					Color bgColor = statusColors.getOrDefault(trend, Color.WHITE);
					c.setBackground(bgColor);
					c.setForeground(bgColor.getRed() + bgColor.getGreen() + bgColor.getBlue() > 500 ? Color.BLACK
							: Color.WHITE);
					return c;
				}

				Color bgColor = statusColors.getOrDefault(status, Color.WHITE);
				c.setBackground(bgColor);
				int brightness = (bgColor.getRed() + bgColor.getGreen() + bgColor.getBlue()) / 3;
				c.setForeground(brightness > 150 ? Color.BLACK : Color.WHITE);
				return c;
			}
		};

		// Table configuration
		JCheckBox checkBoxEditor = new JCheckBox();
		checkBoxEditor.setHorizontalAlignment(JLabel.CENTER);
		dashboardTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(checkBoxEditor));

		// Add this during table initialization
		dashboardTable.setDefaultRenderer(BreakoutValue.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				if (value instanceof BreakoutValue) {
					value = ((BreakoutValue) value).displayValue;
				}
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		});

		dashboardTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		dashboardTable.setRowHeight(25);
		dashboardTable.setAutoCreateRowSorter(true);
		dashboardTable.setGridColor(new Color(220, 220, 220));
		dashboardTable.setShowGrid(false);
		dashboardTable.setIntercellSpacing(new Dimension(0, 1));

		updateTableColumnWidths();

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		for (int i = 3; i < dashboardTable.getColumnCount(); i++) {
			dashboardTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
		}

		DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
		leftRenderer.setHorizontalAlignment(JLabel.LEFT);
		dashboardTable.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);

		JTableHeader header = dashboardTable.getTableHeader();
		header.setBackground(new Color(70, 130, 180));
		header.setForeground(Color.WHITE);
		header.setFont(header.getFont().deriveFont(Font.BOLD));

		dashboardTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int viewRow = dashboardTable.rowAtPoint(e.getPoint());
				int viewCol = dashboardTable.columnAtPoint(e.getPoint());
				if (viewRow >= 0 && viewCol == 0) {
					String symbol = (String) dashboardTable.getValueAt(viewRow, 1);
					for (int i = 0; i < tableModel.getDataVector().size(); i++) {
						Vector<?> v = tableModel.getDataVector().get(i);
						if (v.get(1).toString().equals(symbol)) {
							boolean newVal = !Boolean.valueOf((boolean) v.get(0));
							tableModel.getDataVector().get(i).set(0, newVal);
							dashboardTable.repaint();
							break;
						}
					}
				}
			}
		});

		JScrollPane tableScrollPane = new JScrollPane(dashboardTable);
		tableScrollPane.setPreferredSize(new Dimension(1100, 500));

		// Console setup
		consoleArea = new JTextArea(5, 80);
		consoleArea.setEditable(false);
		consoleScrollPane = new JScrollPane(consoleArea);
		consolePanel = new JPanel(new BorderLayout());
		consolePanel.setPreferredSize(new Dimension(1200, 200));
		consolePanel.add(new JLabel("Console Output:"), BorderLayout.NORTH);
		consolePanel.add(consoleScrollPane, BorderLayout.CENTER);

		// Main split pane
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPane.setTopComponent(tableScrollPane);
		splitPane.setBottomComponent(consolePanel);
		splitPane.setDividerLocation(0.7);
		splitPane.setResizeWeight(0.7);
		splitPane.setDividerSize(5);

		add(splitPane, BorderLayout.CENTER);
	}

	private synchronized void initializeColumnIndices() {
		if (columnsInitialized)
			return;

		indicatorColumnIndices = new ArrayList<>();
		for (int i = 6; i < tableModel.getColumnCount(); i++) {
			String colName = dashboardTable.getColumnName(i);
			if (!shouldSkipColumn(colName)) {
				indicatorColumnIndices.add(i);
			}
		}
		columnsInitialized = true;
	}

	private void openConfigWindow() {
		JFrame configFrame = new JFrame("Indicator Configuration");
		configFrame.setSize(800, 600);
		configFrame.setLayout(new BorderLayout());

		// Main panel with scroll
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		JScrollPane scrollPane = new JScrollPane(mainPanel);

		// Create panels for each timeframe
		for (String tf : allTimeframes) {
			JPanel tfPanel = new JPanel(new BorderLayout());
			tfPanel.setBorder(BorderFactory.createTitledBorder(tf));

			// Timeframe checkbox
			JCheckBox tfCheckbox = new JCheckBox("Enable " + tf);
			tfCheckbox.setSelected(!timeframeIndicators.get(tf).isEmpty());
			timeframeCheckboxes.put(tf, tfCheckbox);

			// Indicators panel
			JPanel indPanel = new JPanel(new GridLayout(0, 2));
			Map<String, JCheckBox> currentIndicators = indicatorCheckboxes.get(tf);

			for (String ind : allIndicators) {
				JCheckBox cb = currentIndicators.get(ind);
				cb.setSelected(timeframeIndicators.get(tf).contains(ind));
				indPanel.add(cb);
			}

			tfPanel.add(tfCheckbox, BorderLayout.NORTH);
			tfPanel.add(indPanel, BorderLayout.CENTER);
			mainPanel.add(tfPanel);
		}

		// Inheritance option
		inheritCheckbox = new JCheckBox("Apply higher timeframe indicators to lower timeframes");
		mainPanel.add(inheritCheckbox);

		// Button panel
		JPanel buttonPanel = new JPanel();
		JButton saveButton = new JButton("Save");
		JButton cancelButton = new JButton("Cancel");

		saveButton.addActionListener(e -> {
			saveConfiguration();
			updateTableColumns();
			configFrame.dispose();
		});

		cancelButton.addActionListener(e -> configFrame.dispose());

		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);

		configFrame.add(scrollPane, BorderLayout.CENTER);
		configFrame.add(buttonPanel, BorderLayout.SOUTH);
		configFrame.setVisible(true);
	}

	private void saveConfiguration() {
		// Clear existing selections
		for (Set<String> indicators : timeframeIndicators.values()) {
			indicators.clear();
		}

		// Save new selections
		for (String tf : allTimeframes) {
			JCheckBox tfCB = timeframeCheckboxes.get(tf);
			if (tfCB != null && tfCB.isSelected()) {
				Map<String, JCheckBox> indCBs = indicatorCheckboxes.get(tf);
				for (Map.Entry<String, JCheckBox> entry : indCBs.entrySet()) {
					if (entry.getValue().isSelected()) {
						timeframeIndicators.get(tf).add(entry.getKey());
					}
				}
			}
		}

		// Handle inheritance if enabled
		if (inheritCheckbox.isSelected()) {
			applyIndicatorInheritance();
		}
	}

	private void openFilterWindow() {
		// Initialize fields if they don't exist
		if (prevVolMinField == null) {
			prevVolMinField = new JTextField(10);
			prevVolMaxField = new JTextField(10);
			currentVolMinField = new JTextField(10);
			currentVolMaxField = new JTextField(10);

			// Set tooltips
			prevVolMinField.setToolTipText("Minimum previous daily volume");
			prevVolMaxField.setToolTipText("Maximum previous daily volume");
			currentVolMinField.setToolTipText("Minimum current daily volume");
			currentVolMaxField.setToolTipText("Maximum current daily volume");
		}

		// Create window if it doesn't exist
		if (filterWindow == null) {
			filterWindow = new JFrame("Volume Filters");
			filterWindow.setSize(400, 250);
			filterWindow.setLayout(new BorderLayout());
			filterWindow.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

			JPanel filterContent = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.fill = GridBagConstraints.HORIZONTAL;

			// Previous Volume Row
			gbc.gridx = 0;
			gbc.gridy = 0;
			filterContent.add(new JLabel("Prev Daily Vol:"), gbc);

			gbc.gridx = 1;
			filterContent.add(prevVolMinField, gbc);

			gbc.gridx = 2;
			filterContent.add(new JLabel("to"), gbc);

			gbc.gridx = 3;
			filterContent.add(prevVolMaxField, gbc);

			// Current Volume Row
			gbc.gridx = 0;
			gbc.gridy = 1;
			filterContent.add(new JLabel("Current Daily Vol:"), gbc);

			gbc.gridx = 1;
			filterContent.add(currentVolMinField, gbc);

			gbc.gridx = 2;
			filterContent.add(new JLabel("to"), gbc);

			gbc.gridx = 3;
			filterContent.add(currentVolMaxField, gbc);

			// Button Panel
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

			JButton applyFiltersButton = createButton("Apply Filters", new Color(70, 130, 180));
			applyFiltersButton.addActionListener(e -> {
				refreshData();
				logToConsole("Volume filters applied");
			});

			JButton clearFiltersButton = createButton("Clear Filters", new Color(200, 200, 200));
			clearFiltersButton.addActionListener(e -> {
				prevVolMinField.setText("");
				prevVolMaxField.setText("");
				currentVolMinField.setText("");
				currentVolMaxField.setText("");
				refreshData();
				logToConsole("Volume filters cleared");
			});

			JButton closeButton = createButton("Close", new Color(200, 200, 200));
			closeButton.addActionListener(e -> filterWindow.setVisible(false));

			buttonPanel.add(applyFiltersButton);
			buttonPanel.add(clearFiltersButton);
			buttonPanel.add(closeButton);

			filterWindow.add(filterContent, BorderLayout.CENTER);
			filterWindow.add(buttonPanel, BorderLayout.SOUTH);

			// Center window relative to main frame
			filterWindow.setLocationRelativeTo(this);
		}

		// Show the window
		filterWindow.setVisible(true);
		filterWindow.toFront();
	}

	private void createFilterWindow() {
		filterWindow = new JFrame("Volume Filters");
		filterWindow.setSize(400, 250);
		filterWindow.setLayout(new BorderLayout());

		JPanel filterContent = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Volume filter fields (same as before)
		prevVolMinField = new JTextField(10);
		prevVolMaxField = new JTextField(10);
		currentVolMinField = new JTextField(10);
		currentVolMaxField = new JTextField(10);

		// Tooltips
		prevVolMinField.setToolTipText("Minimum previous daily volume");
		prevVolMaxField.setToolTipText("Maximum previous daily volume");
		currentVolMinField.setToolTipText("Minimum current daily volume");
		currentVolMaxField.setToolTipText("Maximum current daily volume");

		// Apply button
		JButton applyFiltersButton = createButton("Apply Filters", new Color(70, 130, 180));
		applyFiltersButton.addActionListener(e -> {
			refreshData();
			logToConsole("Volume filters applied");
		});

		// Clear button
		JButton clearFiltersButton = createButton("Clear Filters", new Color(200, 200, 200));
		clearFiltersButton.addActionListener(e -> {
			prevVolMinField.setText("");
			prevVolMaxField.setText("");
			currentVolMinField.setText("");
			currentVolMaxField.setText("");
			refreshData();
			logToConsole("Volume filters cleared");
		});

		// Close button
		JButton closeButton = createButton("Close", new Color(200, 200, 200));
		closeButton.addActionListener(e -> filterWindow.setVisible(false));

		// Layout
		gbc.gridx = 0;
		gbc.gridy = 0;
		filterContent.add(new JLabel("Prev Daily Vol:"), gbc);

		gbc.gridx = 1;
		filterContent.add(prevVolMinField, gbc);

		gbc.gridx = 2;
		filterContent.add(new JLabel("to"), gbc);

		gbc.gridx = 3;
		filterContent.add(prevVolMaxField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		filterContent.add(new JLabel("Current Daily Vol:"), gbc);

		gbc.gridx = 1;
		filterContent.add(currentVolMinField, gbc);

		gbc.gridx = 2;
		filterContent.add(new JLabel("to"), gbc);

		gbc.gridx = 3;
		filterContent.add(currentVolMaxField, gbc);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
		buttonPanel.add(applyFiltersButton);
		buttonPanel.add(clearFiltersButton);
		buttonPanel.add(closeButton);

		filterWindow.add(filterContent, BorderLayout.CENTER);
		filterWindow.add(buttonPanel, BorderLayout.SOUTH);

		// Center the window relative to main frame
		filterWindow.setLocationRelativeTo(this);
	}

	private void applyIndicatorInheritance() {
		// Start from the highest timeframe and propagate down
		for (int i = 0; i < allTimeframes.length - 1; i++) {
			String higherTF = allTimeframes[i];
			Set<String> higherIndicators = timeframeIndicators.get(higherTF);

			if (!higherIndicators.isEmpty()) {
				// Apply to all lower timeframes
				for (int j = i + 1; j < allTimeframes.length; j++) {
					String lowerTF = allTimeframes[j];
					timeframeIndicators.get(lowerTF).addAll(higherIndicators);

					// Update checkboxes to reflect inheritance
					Map<String, JCheckBox> lowerIndCBs = indicatorCheckboxes.get(lowerTF);
					for (String ind : higherIndicators) {
						if (lowerIndCBs.containsKey(ind)) {
							lowerIndCBs.get(ind).setSelected(true);
						}
					}
				}
			}
		}
	}

	private JPanel createFiltersPanel() {
		filtersPanel = new JPanel(new GridBagLayout());
		filtersPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Filters"));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;

		// Prev Daily Vol filter
		JLabel prevVolLabel = new JLabel("Prev Daily Vol:");
		prevVolMinField = new JTextField(8);
		prevVolMinField.setToolTipText("Minimum previous daily volume");
		prevVolMaxField = new JTextField(8);
		prevVolMaxField.setToolTipText("Maximum previous daily volume");

		// Current Daily Vol filter
		JLabel currentVolLabel = new JLabel("Current Daily Vol:");
		currentVolMinField = new JTextField(8);
		currentVolMinField.setToolTipText("Minimum current daily volume");
		currentVolMaxField = new JTextField(8);
		currentVolMaxField.setToolTipText("Maximum current daily volume");

		// Add components to panel
		gbc.gridx = 0;
		gbc.gridy = 0;
		filtersPanel.add(prevVolLabel, gbc);

		gbc.gridx = 1;
		filtersPanel.add(prevVolMinField, gbc);

		gbc.gridx = 2;
		filtersPanel.add(new JLabel("to"), gbc);

		gbc.gridx = 3;
		filtersPanel.add(prevVolMaxField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		filtersPanel.add(currentVolLabel, gbc);

		gbc.gridx = 1;
		filtersPanel.add(currentVolMinField, gbc);

		gbc.gridx = 2;
		filtersPanel.add(new JLabel("to"), gbc);

		gbc.gridx = 3;
		filtersPanel.add(currentVolMaxField, gbc);

		return filtersPanel;
	}

	private boolean passesVolumeFilters(long prevVol, long currentVol) {
		try {
			// Check previous volume filter - add null checks
			if (prevVolMinField != null && !prevVolMinField.getText().isEmpty()) {
				long min = Long.parseLong(prevVolMinField.getText());
				if (prevVol < min)
					return false;
			}
			if (prevVolMaxField != null && !prevVolMaxField.getText().isEmpty()) {
				long max = Long.parseLong(prevVolMaxField.getText());
				if (prevVol > max)
					return false;
			}

			// Check current volume filter - add null checks
			if (currentVolMinField != null && !currentVolMinField.getText().isEmpty()) {
				long min = Long.parseLong(currentVolMinField.getText());
				if (currentVol < min)
					return false;
			}
			if (currentVolMaxField != null && !currentVolMaxField.getText().isEmpty()) {
				long max = Long.parseLong(currentVolMaxField.getText());
				if (currentVol > max)
					return false;
			}

			return true;
		} catch (NumberFormatException e) {
			logToConsole("Invalid volume filter value: " + e.getMessage());
			return true; // Don't filter if there's invalid input
		}
	}

	private void checkBullishSymbolsAndLog() {
		if (!checkBullishCheckbox.isSelected()) {
			return;
		}

		Set<String> excludedSymbols = parseSymbolExceptions();
		List<String> bullishSymbols = new ArrayList<>();

		for (Object rowObj : tableModel.getDataVector()) {
			@SuppressWarnings("unchecked")
			Vector<Object> row = (Vector<Object>) rowObj;
			String symbol = (String) row.get(1);

			if (tableModel.isSymbolHidden(symbol) || excludedSymbols.contains(symbol)) {
				continue;
			}

			boolean isBullish = true;
			for (int j = 6; j < tableModel.getColumnCount(); j++) {
				String colName = dashboardTable.getColumnName(j);
				if (shouldSkipColumn(colName)) {
					continue;
				}

				Object value = row.get(j);
				String status = (value != null) ? value.toString().trim() : "";

				if (status.startsWith("↑") || status.startsWith("↓")) {
					status = status.split(" ")[0] + " " + status.split(" ")[1];
				}

				if (!BULLISH_STATUSES.contains(status)) {
					isBullish = false;
					break;
				}
			}

			if (isBullish) {
				bullishSymbols.add(symbol);
			}
		}

		handleBullishAlerts(bullishSymbols);
	}

	private Set<String> parseSymbolExceptions() {
		Set<String> excludedSymbols = new HashSet<>();
		String exceptionsText = symbolExceptionsField.getText().trim();
		if (!exceptionsText.isEmpty()) {
			excludedSymbols = Arrays.stream(exceptionsText.split("\\s*,\\s*")).map(String::trim)
					.filter(s -> !s.isEmpty()).map(String::toUpperCase).collect(Collectors.toSet());
			logToConsole("Excluding symbols: " + String.join(", ", excludedSymbols));
		}
		return excludedSymbols;
	}

	private List<String> findBullishSymbols(Set<String> validStatuses, Set<String> excludedSymbols) {
		return tableModel.getDataVector().stream().map(row -> (Vector<?>) row).filter(row -> {
			String symbol = (String) row.get(1);
			return !tableModel.isSymbolHidden(symbol) && !excludedSymbols.contains(symbol);
		}).filter(row -> isBullish(row, validStatuses)).map(row -> (String) row.get(1)).collect(Collectors.toList());
	}

	private boolean isBullish(Vector<?> row, Set<String> validStatuses) {
		if (!columnsInitialized) {
			initializeColumnIndices();
		}

		for (int colIndex : indicatorColumnIndices) {
			Object value = row.get(colIndex);
			String status = (value != null) ? value.toString().trim() : "";

			if (status.startsWith("↑") || status.startsWith("↓")) {
				status = status.split(" ")[0];
			}

			if (!validStatuses.contains(status)) {
				return false;
			}
		}
		return true;
	}

	private boolean shouldSkipColumn(String colName) {
		return colName.contains(BREAKOUT_COUNT_COLUMN_PATTERN);
	}

	private void handleBullishAlerts(List<String> bullishSymbols) {
		if (!bullishSymbols.isEmpty()) {
			String alertMessage = "Bullish Crossover Symbols: " + String.join(", ", bullishSymbols);
			logToConsole(alertMessage);

			if (alarmOnCheckbox.isSelected() && isAlarmActive) {
				if (alertManager != null) {
					startBuzzAlert(alertMessage);
				} else {
					logToConsole("AlertManager not available - cannot send buzz alert");
				}
			}
		} else {
			stopBuzzer();
		}
	}

	private void startBuzzAlert(String alertMessage) {
		if (buzzerScheduler == null || buzzerScheduler.isShutdown()) {
			buzzerScheduler = Executors.newSingleThreadScheduledExecutor();
			buzzerScheduler.scheduleAtFixedRate(() -> {
				SwingUtilities.invokeLater(() -> {
					if (isAlarmActive && alarmOnCheckbox.isSelected() && alertManager != null) {
						alertManager.sendBuzzAlert(alertMessage);
					} else {
						stopBuzzer();
					}
				});
			}, 0, 5, TimeUnit.SECONDS);
			logToConsole("Started buzzer for bullish symbols");
		}
	}

	private void stopBuzzer() {
		if (buzzerScheduler != null && !buzzerScheduler.isShutdown()) {
			buzzerScheduler.shutdown();
			try {
				if (!buzzerScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					buzzerScheduler.shutdownNow();
					logToConsole("Buzzer scheduler forcefully stopped after timeout.");
				} else {
					logToConsole("Buzzer stopped.");
				}
			} catch (InterruptedException e) {
				buzzerScheduler.shutdownNow();
				logToConsole("Error stopping buzzer: " + e.getMessage());
			}
			buzzerScheduler = null;
		}
	}

	private void uncheckAllRows() {
		Vector<?> rawData = tableModel.getDataVector();
		int updatedRows = 0;

		for (int i = 0; i < rawData.size(); i++) {
			@SuppressWarnings("unchecked")
			Vector<Object> row = (Vector<Object>) rawData.get(i);
			if ((Boolean) row.get(0)) {
				row.set(0, false);
				updatedRows++;
			}
		}

		tableModel.fireTableDataChanged();
		logToConsole("Unchecked " + updatedRows + " rows.");
	}

	private void checkBullishUptrendNeutralRows() {
		Vector<?> rawData = tableModel.getDataVector();
		int updatedRows = 0;

		for (int i = 0; i < rawData.size(); i++) {
			@SuppressWarnings("unchecked")
			Vector<Object> row = (Vector<Object>) rawData.get(i);
			String symbol = (String) row.get(1);

			if (tableModel.isSymbolHidden(symbol)) {
				continue;
			}

			boolean hasInvalidStatus = false;
			for (int j = 6; j < tableModel.getColumnCount(); j++) {
				String colName = dashboardTable.getColumnName(j);
				if (shouldSkipColumn(colName)) {
					continue;
				}

				Object value = row.get(j);
				String status = (value != null) ? value.toString().trim() : "";

				if (status.startsWith("↑") || status.startsWith("↓")) {
					status = status.split(" ")[0] + " " + status.split(" ")[1];
				}

				if (!BULLISH_STATUSES.contains(status)) {
					hasInvalidStatus = true;
					break;
				}
			}

			// Only check the checkbox if invalid statuses found
			if (hasInvalidStatus) {
				row.set(0, true); // Check the checkbox
				updatedRows++;
			} else {
				row.set(0, false); // Uncheck if no invalid statuses
			}
		}

		tableModel.fireTableDataChanged();
		logToConsole("Checked " + updatedRows + " rows with non-Bullish statuses.");
	}

	private void listVisibleSymbols() {
		StringBuilder sb = new StringBuilder();
		sb.append("Visible Symbols in Watchlist '").append(currentWatchlist).append("':\n");

		Vector<?> rawData = tableModel.getDataVector();
		List<String> visibleSymbols = new ArrayList<>();

		for (Object row : rawData) {
			Vector<?> rowVector = (Vector<?>) row;
			String symbol = (String) rowVector.get(1);
			if (!tableModel.isSymbolHidden(symbol)) {
				visibleSymbols.add(symbol);
			}
		}

		if (visibleSymbols.isEmpty()) {
			sb.append("No visible symbols.");
		} else {
			visibleSymbols.sort(String::compareTo);
			for (String symbol : visibleSymbols) {
				sb.append(symbol).append(",");
			}
		}

		logToConsole(sb.toString());
	}

	private void updateTableColumnWidths() {
		// Existing column widths...
		dashboardTable.getColumnModel().getColumn(0).setPreferredWidth(50);
		dashboardTable.getColumnModel().getColumn(1).setPreferredWidth(80);
		dashboardTable.getColumnModel().getColumn(2).setPreferredWidth(100);
		dashboardTable.getColumnModel().getColumn(3).setPreferredWidth(100);
		dashboardTable.getColumnModel().getColumn(4).setPreferredWidth(100);
		dashboardTable.getColumnModel().getColumn(5).setPreferredWidth(80);
		for (int i = 6; i < dashboardTable.getColumnCount(); i++) {
			// Make Breakout Count column wider
			String colName = dashboardTable.getColumnName(i);
			if (colName.contains("Breakout Count")) {
				dashboardTable.getColumnModel().getColumn(i).setPreferredWidth(150);
			} else {
				dashboardTable.getColumnModel().getColumn(i).setPreferredWidth(120);
			}
		}
	}

	private void updateTimeRangeLabel() {
		if (startDatePicker.getDate() != null && endDatePicker.getDate() != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			timeRangeLabel.setText("Time Range: " + sdf.format(startDatePicker.getDate()) + " to "
					+ sdf.format(endDatePicker.getDate()));
		} else {
			timeRangeLabel.setText("Time Range: Custom (dates not set)");
		}
	}

	private void logToConsole(String message) {
		SwingUtilities.invokeLater(() -> {
			String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
			String logMessage = String.format("[%s] %s%n", timestamp, message);
			consoleArea.append(logMessage);
			consoleLogBuffer.append(logMessage);
			consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
		});
	}

	private void addNewWatchlist() {
		String name = JOptionPane.showInputDialog(this, "Enter new watchlist name:");
		if (name != null && !name.trim().isEmpty()) {
			if (!watchlists.containsKey(name)) {
				watchlists.put(name, new ArrayList<>());
				watchlistComboBox.addItem(name);
				watchlistComboBox.setSelectedItem(name);
				currentWatchlist = name;
			} else {
				JOptionPane.showMessageDialog(this, "Watchlist already exists!", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void editCurrentWatchlist() {
		if (watchListException.contains(currentWatchlist)) {
			JOptionPane.showMessageDialog(this, "Cannot edit the Default watchlist", "Info",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		List<String> currentSymbols = watchlists.get(currentWatchlist);
		String symbolsText = String.join(",", currentSymbols);

		String newSymbols = JOptionPane.showInputDialog(this, "Edit symbols (comma separated):", symbolsText);

		if (newSymbols != null) {
			List<String> updatedSymbols = Arrays.asList(newSymbols.split("\\s*,\\s*"));
			watchlists.put(currentWatchlist, updatedSymbols);
			refreshData();
		}
	}

	private void showAllRows() {
		((FilterableTableModel) dashboardTable.getModel()).showAllSymbols();
		dashboardTable.repaint();
	}

	private JButton createButton(String text, Color bgColor) {
		JButton button = new JButton(text);
		button.setBackground(bgColor);
		button.setForeground(Color.WHITE);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setOpaque(true);
		return button;
	}

	private void toggleSelectedRowsVisibility() {
		FilterableTableModel model = (FilterableTableModel) dashboardTable.getModel();
		Set<String> symbolsToToggle = new HashSet<>();

		for (int viewRow = 0; viewRow < dashboardTable.getRowCount(); viewRow++) {
			int modelRow = dashboardTable.convertRowIndexToModel(viewRow);
			Boolean isSelected = (Boolean) model.getValueAt(viewRow, 0);
			String symbol = (String) model.getValueAt(viewRow, 1);

			if (isSelected != null && isSelected) {
				symbolsToToggle.add(symbol);
				model.setValueAt(false, viewRow, 0);
			}
		}

		if (!symbolsToToggle.isEmpty()) {
			model.toggleSymbolsVisibility(symbolsToToggle);
		}

		dashboardTable.repaint();
	}

	private String[] createColumnNames() {
		List<String> columns = new ArrayList<>(
				Arrays.asList("Select", "Symbol", "Latest Price", "Prev Daily Vol", "Current Daily Vol", "% Change"));

		for (String tf : allTimeframes) {
			Set<String> indicators = timeframeIndicators.get(tf);
			if (indicators.isEmpty())
				continue;

			for (String ind : allIndicators) {
				if (indicators.contains(ind)) {
					columns.add(tf + " " + ind);
				}
			}
		}

		return columns.toArray(new String[0]);
	}

	private void updateTableColumns() {
		String[] newColumnNames = createColumnNames();
		Vector<Vector<Object>> oldData = new Vector<>();

		if (tableModel != null && tableModel.getDataVector() != null) {
			Vector<?> rawData = tableModel.getDataVector();
			for (Object row : rawData) {
				if (row instanceof Vector) {
					@SuppressWarnings("unchecked")
					Vector<Object> rowData = (Vector<Object>) row;
					oldData.add(new Vector<>(rowData));
				}
			}
		}

		tableModel = new FilterableTableModel(newColumnNames, 0);
		dashboardTable.setModel(tableModel);

		for (Vector<Object> row : oldData) {
			try {
				String symbol = (String) row.get(1);
				PriceData priceData = fetchLatestPrice(symbol);
				Map<String, AnalysisResult> results = analyzeSymbol(symbol);
				addNewSymbolRow(symbol, priceData, results);
			} catch (Exception e) {
				logToConsole("Error restoring data for row: " + e.getMessage());
			}
		}

		updateTableColumnWidths();

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		for (int i = 3; i < dashboardTable.getColumnCount(); i++) {
			dashboardTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
		}

		DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
		leftRenderer.setHorizontalAlignment(JLabel.LEFT);
		dashboardTable.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);

		dashboardTable.repaint();
	}

	private void refreshData() {
		List<String> symbolsToProcess = new ArrayList<String>();

		if (watchListException.contains(currentWatchlist) && currentWatchlist != "Default") {
			switch (currentWatchlist) {
			case "YahooDOW":
				symbolsToProcess = yahooQueryFinance.getDowJones();
				break;
			case "YahooNASDAQ":
				symbolsToProcess = yahooQueryFinance.getNasDaq();
				break;
			case "YahooSP500":
				symbolsToProcess = yahooQueryFinance.getSP500();
				break;
			case "YahooRussel2k":
				symbolsToProcess = yahooQueryFinance.getRussel2k();
				break;
			case "YahooPennyBy3MonVol":
				symbolsToProcess = yahooQueryFinance.getPennyBy3MonVol();
				break;
			case "YahooPennyByPercentChange":
				symbolsToProcess = yahooQueryFinance.getPennyByPercentChange();
				break;
			}
		} else {
			symbolsToProcess = watchlists.get(currentWatchlist);
		}

		if (symbolsToProcess == null || symbolsToProcess.isEmpty()) {
			return;
		}

		if (!isLive) {
			tableModel.setRowCount(0);
			logToConsole("Cleared table model for manual refresh");
		} else {
			logToConsole("Preserving table model for live update");
		}

		FilterableTableModel model = (FilterableTableModel) dashboardTable.getModel();
		Set<String> visibleSymbols = new HashSet<>();

		for (int i = 0; i < model.getRowCount(); i++) {
			String symbol = (String) model.getValueAt(i, 1);
			if (!model.isSymbolHidden(symbol)) {
				visibleSymbols.add(symbol);
			}
		}

		if (visibleSymbols.isEmpty()) {
			model.showAllSymbols();
			visibleSymbols.addAll(symbolsToProcess);
		}

		ExecutorService executor = Executors.newFixedThreadPool(visibleSymbols.size());

		for (String symbol : visibleSymbols) {
			executor.execute(() -> {
				try {
					Map<String, AnalysisResult> results = analyzeSymbol(symbol);
					PriceData priceData = fetchLatestPrice(symbol);
					SwingUtilities.invokeLater(() -> {
						updateOrAddSymbolRow(symbol, priceData, results);
					});
				} catch (Exception e) {
					logToConsole("Error analyzing " + symbol + ": " + e.getMessage());
					SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
							"Error analyzing " + symbol + ": " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
				}
			});
		}
		executor.shutdown();

		// Check if any hidden symbols now pass filters
		Set<String> symbolsToShow = new HashSet<>();
		for (int i = 0; i < model.getRowCount(); i++) {
			String symbol = (String) model.getValueAt(i, 1);
			if (model.isSymbolHidden(symbol)) {
				Long prevVol = (Long) model.getValueAt(i, 3);
				Long currentVol = (Long) model.getValueAt(i, 4);
				if (passesVolumeFilters(prevVol, currentVol)) {
					symbolsToShow.add(symbol);
				}
			}
		}

		if (!symbolsToShow.isEmpty()) {
			model.toggleSymbolsVisibility(symbolsToShow);
		}

		try {
			if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
				executor.shutdownNow();
				logToConsole("Data refresh timed out after 60 seconds");
			} else {
				logToConsole("Table data population completed, row count: " + tableModel.getRowCount());
				checkBullishSymbolsAndLog();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			logToConsole("Data refresh interrupted: " + e.getMessage());
		}
	}

	private void updateOrAddSymbolRow(String symbol, PriceData priceData, Map<String, AnalysisResult> results) {
		// Apply volume filters
		if (!passesVolumeFilters(priceData.prevDailyVol, priceData.currentDailyVol)) {
			FilterableTableModel model = (FilterableTableModel) dashboardTable.getModel();
			// Check if symbol exists in table
			for (int i = 0; i < model.getRowCount(); i++) {
				String rowSymbol = (String) model.getValueAt(i, 1);
				if (rowSymbol.equals(symbol)) {
					// Hide the row if it doesn't pass filters
					if (!model.isSymbolHidden(symbol)) {
						model.toggleSymbolsVisibility(Collections.singleton(symbol));
					}
					return;
				}
			}
			// Don't add new row if it doesn't pass filters
			return;
		}

		FilterableTableModel model = (FilterableTableModel) dashboardTable.getModel();

		// Check if symbol already exists in table
		for (int i = 0; i < model.getRowCount(); i++) {
			String rowSymbol = (String) model.getValueAt(i, 1);
			if (rowSymbol.equals(symbol)) {
				// Show the row if it was hidden and now passes filters
				if (model.isSymbolHidden(symbol)) {
					model.toggleSymbolsVisibility(Collections.singleton(symbol));
				}
				updateRowData(model, i, priceData, results);
				return;
			}
		}

		// If symbol doesn't exist, add new row
		addNewSymbolRow(symbol, priceData, results);
	}

	private void updateRowData(FilterableTableModel model, int row, PriceData priceData,
			Map<String, AnalysisResult> results) {
		SwingUtilities.invokeLater(() -> {
			if (row < 0 || row >= model.getRowCount()) {
				logToConsole("Invalid row index: " + row + " for symbol update");
				return;
			}

			String symbol = (String) model.getValueAt(row, 1);
			model.setValueAt(Boolean.FALSE, row, 0); // Reset checkbox
			model.setValueAt(priceData.latestPrice, row, 2);
			model.setValueAt(priceData.prevDailyVol, row, 3);
			model.setValueAt(priceData.currentDailyVol, row, 4);
			model.setValueAt(priceData.percentChange, row, 5);

			int colIndex = 6; // Start of indicator columns

			// Iterate through timeframes in display order
			for (String tf : allTimeframes) {
				Set<String> indicatorsForTf = timeframeIndicators.get(tf);
				if (indicatorsForTf == null || indicatorsForTf.isEmpty()) {
					continue;
				}

				AnalysisResult result = results.get(tf);
				if (result == null) {
					// Skip all columns for this timeframe if no data
					colIndex += indicatorsForTf.size();
					continue;
				}

				// Process each indicator in consistent order
				for (String ind : allIndicators) {
					if (!indicatorsForTf.contains(ind)) {
						continue;
					}

					switch (ind) {
					case "Trend":
						model.setValueAt(getTrendStatus(result), row, colIndex++);
						break;
					case "RSI":
						model.setValueAt(getRsiStatus(result), row, colIndex++);
						break;
					case "MACD":
						model.setValueAt(getMacdStatus(result, "MACD"), row, colIndex++);
						break;
					case "MACD(5,8,9)":
						model.setValueAt(getMacdStatus(result, "MACD(5,8,9)"), row, colIndex++);
						break;
					case "PSAR(0.01)":
						model.setValueAt(formatPsarValue(result.psar001, result.psar001Trend), row, colIndex++);
						break;
					case "PSAR(0.05)":
						model.setValueAt(formatPsarValue(result.psar005, result.psar005Trend), row, colIndex++);
						break;
					case "Action":
						model.setValueAt(getActionRecommendation(result), row, colIndex++);
						break;
					case "Breakout Count":
						int totalBreakouts = 0;
						StringBuilder breakoutText = new StringBuilder();

						if (indicatorsForTf.contains("MACD")) {
							totalBreakouts += result.macdBreakoutCount;
							breakoutText.append("MACD:").append(result.macdBreakoutCount).append(" ");
						}
						if (indicatorsForTf.contains("MACD(5,8,9)")) {
							totalBreakouts += result.macd359BreakoutCount;
							breakoutText.append("M5:").append(result.macd359BreakoutCount).append(" ");
						}
						if (indicatorsForTf.contains("PSAR(0.01)")) {
							totalBreakouts += result.psar001BreakoutCount;
							breakoutText.append("PS1:").append(result.psar001BreakoutCount).append(" ");
						}
						if (indicatorsForTf.contains("PSAR(0.05)")) {
							totalBreakouts += result.psar005BreakoutCount;
							breakoutText.append("PS5:").append(result.psar005BreakoutCount).append(" ");
						}

						// Store both the numeric value for sorting and formatted string for display
						String displayValue = breakoutText.length() > 0 ? breakoutText.toString().trim() : "N/A";

						// Create a special holder object that maintains both values
						BreakoutValue breakoutValue = new BreakoutValue(totalBreakouts, displayValue);
						model.setValueAt(breakoutValue, row, colIndex++);
						break;
					default:
						model.setValueAt("N/A", row, colIndex++);
						break;
					}
				}
			}
			model.fireTableRowsUpdated(row, row);
		});
	}

	private void addNewSymbolRow(String symbol, PriceData priceData, Map<String, AnalysisResult> results) {
		Vector<Object> row = new Vector<>();
		row.add(false); // Checkbox
		row.add(symbol);
		row.add(priceData.latestPrice);
		row.add(priceData.prevDailyVol);
		row.add(priceData.currentDailyVol);
		row.add(priceData.percentChange);

		// Process indicators in the same order as createColumnNames()
		for (String tf : allTimeframes) {
			Set<String> indicatorsForTf = timeframeIndicators.get(tf);
			if (indicatorsForTf == null || indicatorsForTf.isEmpty()) {
				continue;
			}

			AnalysisResult result = results.get(tf);

			// Process each indicator in consistent order
			for (String ind : allIndicators) {
				if (!indicatorsForTf.contains(ind)) {
					continue;
				}

				if (result == null) {
					row.add("N/A");
					continue;
				}

				switch (ind) {
				case "Trend":
					row.add(getTrendStatus(result));
					break;
				case "RSI":
					row.add(getRsiStatus(result));
					break;
				case "MACD":
					row.add(getMacdStatus(result, "MACD"));
					break;
				case "MACD(5,8,9)":
					row.add(getMacdStatus(result, "MACD(5,8,9)"));
					break;
				case "PSAR(0.01)":
					row.add(formatPsarValue(result.psar001, result.psar001Trend));
					break;
				case "PSAR(0.05)":
					row.add(formatPsarValue(result.psar005, result.psar005Trend));
					break;
				case "Action":
					row.add(getActionRecommendation(result));
					break;
				case "MACD Breakout":
					row.add(result.macdBreakoutCount);
					break;
				case "MACD(5,8,9) Breakout":
					row.add(result.macd359BreakoutCount);
					break;
				case "PSAR(0.01) Breakout":
					row.add(result.psar001BreakoutCount);
					break;
				case "PSAR(0.05) Breakout":
					row.add(result.psar005BreakoutCount);
					break;
				case "Breakout Count":
					int totalBreakouts = 0;
					if (indicatorsForTf.contains("MACD")) {
						totalBreakouts += result.macdBreakoutCount;
					}
					if (indicatorsForTf.contains("MACD(5,8,9)")) {
						totalBreakouts += result.macd359BreakoutCount;
					}
					if (indicatorsForTf.contains("PSAR(0.01)")) {
						totalBreakouts += result.psar001BreakoutCount;
					}
					if (indicatorsForTf.contains("PSAR(0.05)")) {
						totalBreakouts += result.psar005BreakoutCount;
					}
					row.add(totalBreakouts); // Store just the number for sorting
					break;
				default:
					row.add("N/A");
					break;
				}
			}
		}

		tableModel.addRow(row);
		logToConsole("Added new row for symbol: " + symbol);
	}

	private String getMacdStatus(AnalysisResult result, String macdType) {
		if ("MACD(5,8,9)".equals(macdType)) {
			return result.macd359Status != null ? result.macd359Status : "Neutral";
		} else {
			return result.macdStatus != null ? result.macdStatus : "Neutral";
		}
	}

	private void startLiveUpdates() {
		if (isLive) {
			return;
		}

		isLive = true;
		startLiveButton.setEnabled(false);
		stopLiveButton.setEnabled(true);
		refreshButton.setEnabled(false);

		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(() -> {
			// Check time range before each refresh

			SwingUtilities.invokeLater(() -> {
				try {
					if (isWithinAllowedTimeRange()) {
						refreshData();
					} else {
						logToConsole("Skipped live update - outside allowed time range");
					}
					statusLabel.setText("Status: Live update at "
							+ LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
				} catch (Exception e) {
					logToConsole("Error during live update: " + e.getMessage());
				}
			});

		}, 0, currentRefreshInterval, TimeUnit.SECONDS);

		logToConsole("Live updates started (interval: " + currentRefreshInterval + "s)");
	}

	private boolean isWithinAllowedTimeRange() {
		try {
			Preferences prefs = Preferences.userNodeForPackage(StockDashboard.class);
			String startStr = prefs.get("liveUpdateStartTime", "09:30");
			String endStr = prefs.get("liveUpdateEndTime", "16:00");

			LocalTime start = LocalTime.parse(startStr);
			LocalTime end = LocalTime.parse(endStr);
			LocalTime now = LocalTime.now();

			// Handle overnight ranges (e.g., 22:00 to 06:00)
			if (start.isAfter(end)) {
				return !now.isAfter(end) || !now.isBefore(start);
			} else {
				return !now.isBefore(start) && !now.isAfter(end);
			}
		} catch (Exception e) {
			logToConsole("Error checking time range: " + e.getMessage());
			return true; // Default to allowing if there's an error
		}
	}

	private void stopLiveUpdates() {
		if (!isLive) {
			return;
		}
		isLive = false;
		startLiveButton.setEnabled(true);
		stopLiveButton.setEnabled(false);
		refreshButton.setEnabled(true);
		if (scheduler != null) {
			scheduler.shutdown();
			try {
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				scheduler.shutdownNow();
				logToConsole("Error stopping scheduler: " + e.getMessage());
			}
		}
	}

	private Map<String, AnalysisResult> analyzeSymbol(String symbol) {
		Map<String, AnalysisResult> results = new HashMap<>();

		for (String tf : allTimeframes) {
			if (!timeframeIndicators.get(tf).isEmpty()) {
				try {
					StockDataResult stockData = fetchStockData(symbol, tf, 1000);
					BarSeries series = parseJsonToBarSeries(stockData, symbol, tf);
					results.put(tf, performTechnicalAnalysis(series, timeframeIndicators.get(tf)));
				} catch (Exception e) {
					logToConsole("Error analyzing " + symbol + " " + tf + ": " + e.getMessage());
				}
			}
		}
		return results;
	}

	private StockDataResult fetchStockData(String symbol, String timeframe, int limit) throws IOException {
		ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC);
		ZonedDateTime start = calculateStartTime(timeframe, end);

		if (customRangeRadio.isSelected() && startDatePicker.getDate() != null && endDatePicker.getDate() != null) {
			try {
				start = startDatePicker.getDate().toInstant().atZone(ZoneOffset.UTC);
				end = endDatePicker.getDate().toInstant().atZone(ZoneOffset.UTC);
			} catch (Exception e) {
				logToConsole("Error parsing custom date range for " + symbol + ": " + e.getMessage());
			}
		}

		if ("Yahoo".equals(currentDataSource)) {
			return fetchYahooStockData(symbol, timeframe, limit, start, end);
		} else {
			return fetchAlpacaStockData(symbol, timeframe, limit, start, end);
		}
	}

	private StockDataResult fetchAlpacaStockData(String symbol, String timeframe, int limit, ZonedDateTime start,
			ZonedDateTime end) throws IOException {
		HttpUrl.Builder urlBuilder = HttpUrl.parse("https://data.alpaca.markets/v2/stocks/" + symbol + "/bars")
				.newBuilder().addQueryParameter("timeframe", timeframe)
				.addQueryParameter("start", start.format(DateTimeFormatter.ISO_INSTANT))
				.addQueryParameter("end", end.format(DateTimeFormatter.ISO_INSTANT))
				.addQueryParameter("limit", String.valueOf(limit)).addQueryParameter("adjustment", "raw")
				.addQueryParameter("feed", "iex").addQueryParameter("sort", "asc");

		Request request = new Request.Builder().url(urlBuilder.build()).get().addHeader("accept", "application/json")
				.addHeader("APCA-API-KEY-ID", apiKey).addHeader("APCA-API-SECRET-KEY", apiSecret).build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response + ": " + response.body().string());
			}
			return new StockDataResult(response.body().string(), start, end);
		}
	}

	private StockDataResult fetchYahooStockData(String symbol, String timeframe, int limit, ZonedDateTime start,
			ZonedDateTime end) throws IOException {
		String interval = convertTimeframeToYahooInterval(timeframe);

		long period1 = start.toEpochSecond();
		long period2 = end.toEpochSecond();

		HttpUrl url = HttpUrl.parse("https://query1.finance.yahoo.com/v8/finance/chart/" + symbol).newBuilder()
				.addQueryParameter("interval", interval).addQueryParameter("period1", String.valueOf(period1))
				.addQueryParameter("period2", String.valueOf(period2)).build();

		Request request = new Request.Builder().url(url).get().addHeader("accept", "application/json").build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response + ": " + response.body().string());
			}
			return new StockDataResult(response.body().string(), start, end);
		}
	}

	private String convertTimeframeToYahooInterval(String timeframe) {
		switch (timeframe) {
		case "1Min":
			return "1m";
		case "5Min":
			return "5m";
		case "15Min":
			return "15m";
		case "30Min":
			return "30m";
		case "1H":
			return "1h";
		case "4H":
			return "4h";
		case "1D":
			return "1d";
		case "1W":
			return "1wk";
		default:
			return "1d";
		}
	}

	private BarSeries parseJsonToBarSeries(StockDataResult stockData, String symbol, String timeframe) {
		if ("Yahoo".equals(currentDataSource)) {
			return parseYahooJsonToBarSeries(stockData, symbol, timeframe);
		} else {
			return parseAlpacaJsonToBarSeries(stockData, symbol, timeframe);
		}
	}

	private BarSeries parseYahooJsonToBarSeries(StockDataResult stockData, String symbol, String timeframe) {
		try {
			JSONObject json = new JSONObject(stockData.jsonResponse);
			JSONObject chart = json.getJSONObject("chart");
			JSONArray results = chart.getJSONArray("result");
			JSONObject firstResult = results.getJSONObject(0);

			JSONArray timestamps = firstResult.getJSONArray("timestamp");

			JSONObject indicators = firstResult.getJSONObject("indicators");
			JSONArray quotes = indicators.getJSONArray("quote");
			JSONObject quote = quotes.getJSONObject(0);

			JSONArray opens = quote.getJSONArray("open");
			JSONArray highs = quote.getJSONArray("high");
			JSONArray lows = quote.getJSONArray("low");
			JSONArray closes = quote.getJSONArray("close");
			JSONArray volumes = quote.getJSONArray("volume");

			BarSeries series = new BaseBarSeriesBuilder().withName(symbol).build();

			for (int i = 0; i < timestamps.length(); i++) {
				long timestamp = timestamps.getLong(i);
				ZonedDateTime time = ZonedDateTime.ofInstant(java.time.Instant.ofEpochSecond(timestamp),
						ZoneOffset.UTC);

				double open = opens.isNull(i) ? 0 : opens.getDouble(i);
				double high = highs.isNull(i) ? 0 : highs.getDouble(i);
				double low = lows.isNull(i) ? 0 : lows.getDouble(i);
				double close = closes.isNull(i) ? 0 : closes.getDouble(i);
				long volume = volumes.isNull(i) ? 0 : volumes.getLong(i);

				series.addBar(time, open, high, low, close, volume);
			}

			return series;
		} catch (JSONException e) {
			logToConsole("Error parsing Yahoo JSON for symbol " + symbol + ": " + e.getMessage());
			return new BaseBarSeriesBuilder().withName(symbol).build();
		}
	}

	private BarSeries parseAlpacaJsonToBarSeries(StockDataResult stockData, String symbol, String timeframe) {
		try {
			JSONObject json = new JSONObject(stockData.jsonResponse);

			if (!json.has("bars")) {
				throw new JSONException("No 'bars' field in response");
			}

			JSONArray bars;
			if (json.get("bars") instanceof JSONObject) {
				JSONObject barsObject = json.getJSONObject("bars");
				if (!barsObject.has(symbol)) {
					throw new JSONException("No data for symbol: " + symbol);
				}
				bars = barsObject.getJSONArray(symbol);
			} else {
				bars = json.getJSONArray("bars");
			}

			BarSeries series = new BaseBarSeriesBuilder().withName(symbol).build();

			for (int i = 0; i < bars.length(); i++) {
				JSONObject bar = bars.getJSONObject(i);
				ZonedDateTime time = ZonedDateTime.parse(bar.getString("t"), DateTimeFormatter.ISO_ZONED_DATE_TIME);

				series.addBar(time, bar.getDouble("o"), bar.getDouble("h"), bar.getDouble("l"), bar.getDouble("c"),
						bar.getLong("v"));
			}

			if (json.has("next_page_token")) {
				String nextPageToken = json.getString("next_page_token");
				if (nextPageToken != "null") {
					try {
						boolean hasNextPage = json.has("next_page_token");

						while (hasNextPage && nextPageToken != "null") {
							String nextPageData = fetchNextPage(symbol, nextPageToken, timeframe, stockData.start,
									stockData.end);
							JSONObject nextPageJson = new JSONObject(nextPageData);

							if (!nextPageJson.has("bars")) {
								throw new JSONException("No 'bars' field in response");
							}

							if (json.get("bars") instanceof JSONObject) {
								JSONObject nextPageBars = nextPageJson.getJSONObject("bars");
								if (!nextPageBars.has(symbol)) {
									throw new JSONException("No data for symbol: " + symbol);
								}
								bars = nextPageBars.getJSONArray(symbol);
							} else {
								bars = nextPageJson.getJSONArray("bars");
							}

							for (int i = 0; i < bars.length(); i++) {
								JSONObject bar = bars.getJSONObject(i);
								ZonedDateTime time = ZonedDateTime.parse(bar.getString("t"),
										DateTimeFormatter.ISO_ZONED_DATE_TIME);

								series.addBar(time, bar.getDouble("o"), bar.getDouble("h"), bar.getDouble("l"),
										bar.getDouble("c"), bar.getLong("v"));
							}

							hasNextPage = nextPageJson.has("next_page_token");
							nextPageToken = nextPageJson.getString("next_page_token");
						}
					} catch (Exception e) {
						logToConsole("Error fetching next page for " + symbol + ": " + e.getMessage());
					}
				}
			}

			return series;
		} catch (JSONException e) {
			logToConsole("Error parsing JSON for symbol " + symbol + ": " + e.getMessage());
			return new BaseBarSeriesBuilder().withName(symbol).build();
		}
	}

	private String fetchNextPage(String symbol, String nextPageToken, String timeframe, ZonedDateTime start,
			ZonedDateTime end) throws IOException {
		HttpUrl url = HttpUrl.parse("https://data.alpaca.markets/v2/stocks/" + symbol + "/bars").newBuilder()
				.addQueryParameter("start", start.format(DateTimeFormatter.ISO_INSTANT))
				.addQueryParameter("end", end.format(DateTimeFormatter.ISO_INSTANT))
				.addQueryParameter("timeframe", timeframe).addQueryParameter("limit", String.valueOf(1000))
				.addQueryParameter("adjustment", "raw").addQueryParameter("feed", "iex")
				.addQueryParameter("sort", "asc").addQueryParameter("page_token", nextPageToken).build();

		Request request = new Request.Builder().url(url).get().addHeader("accept", "application/json")
				.addHeader("APCA-API-KEY-ID", apiKey).addHeader("APCA-API-SECRET-KEY", apiSecret).build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response + ": " + response.body().string());
			}
			return response.body().string();
		}
	}

	private ZonedDateTime calculateStartTime(String timeframe, ZonedDateTime end) {
		switch (timeframe) {
		case "1Min":
			return end.minusDays(7);
		case "5Min":
			return end.minusDays(7);
		case "15Min":
			return end.minusDays(7);
		case "30Min":
		case "1H":
			return end.minusDays(60);
		case "4H":
			return end.minusDays(150);
		case "1D":
			return end.minusDays(730);
		case "1W":
			return end.minusDays(730);
		default:
			return end.minusDays(7);
		}
	}

	private class StockDataResult {
		String jsonResponse;
		ZonedDateTime start;
		ZonedDateTime end;

		public StockDataResult(String jsonResponse, ZonedDateTime start, ZonedDateTime end) {
			this.jsonResponse = jsonResponse;
			this.start = start;
			this.end = end;
		}
	}

	private PriceData fetchLatestPrice(String symbol) throws IOException, JSONException {
		if ("Yahoo".equals(currentDataSource)) {
			return fetchYahooLatestPrice(symbol);
		} else {
			return fetchAlpacaLatestPrice(symbol);
		}
	}

	private PriceData fetchYahooLatestPrice(String symbol) throws IOException, JSONException {
		HttpUrl url = HttpUrl.parse("https://query1.finance.yahoo.com/v8/finance/chart/" + symbol).newBuilder()
				.addQueryParameter("interval", "1d").addQueryParameter("range", "2d").build();

		Request request = new Request.Builder().url(url).get().addHeader("accept", "application/json").build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response + ": " + response.body().string());
			}

			String responseBody = response.body().string();
			JSONObject json = new JSONObject(responseBody);
			JSONObject chart = json.getJSONObject("chart");
			JSONArray results = chart.getJSONArray("result");
			JSONObject firstResult = results.getJSONObject(0);
			JSONObject meta = firstResult.getJSONObject("meta");
			JSONObject quote = firstResult.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0);
			JSONArray volumes = quote.getJSONArray("volume");

			double currentPrice = meta.getDouble("regularMarketPrice");
			double previousClose = meta.getDouble("chartPreviousClose");
			double percentChange = ((currentPrice - previousClose) / previousClose) * 100;
			long currentDailyVol = volumes.getLong(volumes.length() - 1);
			long prevDailyVol = volumes.length() > 1 ? volumes.getLong(volumes.length() - 2) : 0;

			return new PriceData(currentPrice, prevDailyVol, currentDailyVol, percentChange);
		}
	}

	private PriceData fetchAlpacaLatestPrice(String symbol) throws IOException, JSONException {
		Request request = new Request.Builder().url("https://data.alpaca.markets/v2/stocks/snapshots?symbols=" + symbol)
				.get().addHeader("accept", "application/json").addHeader("APCA-API-KEY-ID", apiKey)
				.addHeader("APCA-API-SECRET-KEY", apiSecret).build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response + ": " + response.body().string());
			}

			String responseBody = response.body().string();
			JSONObject json = new JSONObject(responseBody);
			JSONObject snapshot = json.getJSONObject(symbol);
			JSONObject latestTrade = snapshot.getJSONObject("latestTrade");
			JSONObject dailyBar = snapshot.getJSONObject("dailyBar");
			JSONObject prevDailyBar = snapshot.getJSONObject("prevDailyBar");

			double currentPrice = latestTrade.getDouble("p");
			double previousClose = prevDailyBar.getDouble("c");
			double percentChange = ((currentPrice - previousClose) / previousClose) * 100;
			long currentDailyVol = dailyBar.getLong("v");
			long prevDailyVol = prevDailyBar.getLong("v");

			return new PriceData(currentPrice, prevDailyVol, currentDailyVol, percentChange);
		}
	}

	private AnalysisResult performTechnicalAnalysis(BarSeries series, Set<String> indicatorsToCalculate) {
		AnalysisResult result = new AnalysisResult();
		if (series.getBarCount() == 0)
			return result;

		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		int endIndex = series.getEndIndex();
		Bar currentBar = series.getBar(endIndex);
		Num currentClose = currentBar.getClosePrice();
		result.setPrice(currentClose.doubleValue());

		// 1. Trend Analysis (SMA50/SMA200)
		if (indicatorsToCalculate.contains("Trend")) {
			SMAIndicator sma50 = new SMAIndicator(closePrice, 50);
			SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
			if (endIndex >= 49)
				result.setSma50(sma50.getValue(endIndex).doubleValue());
			if (endIndex >= 199)
				result.setSma200(sma200.getValue(endIndex).doubleValue());
		}

		// 2. RSI Analysis
		if (indicatorsToCalculate.contains("RSI")) {
			RSIIndicator rsi = new RSIIndicator(closePrice, 14);
			if (endIndex >= 13)
				result.setRsi(rsi.getValue(endIndex).doubleValue());
		}

		// 3. Standard MACD
		if (indicatorsToCalculate.contains("MACD")) {
			MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
			EMAIndicator signal = new EMAIndicator(macd, 9);
			if (endIndex >= 34) {
				result.setMacd(macd.getValue(endIndex).doubleValue());
				result.setMacdSignal(signal.getValue(endIndex).doubleValue());
				// Determine crossover status
				if (endIndex > 0) {
					double prevMacd = macd.getValue(endIndex - 1).doubleValue();
					double prevSignal = signal.getValue(endIndex - 1).doubleValue();
					result.setMacdStatus(determineMacdStatus(result.macd, result.macdSignal, prevMacd, prevSignal));
				}
			}
		}

		// 4. MACD(5,8,9)
		if (indicatorsToCalculate.contains("MACD(5,8,9)")) {
			MACDIndicator macd359 = new MACDIndicator(closePrice, 5, 8);
			EMAIndicator signal359 = new EMAIndicator(macd359, 9);
			if (endIndex >= 16) {
				result.setMacd359(macd359.getValue(endIndex).doubleValue());
				result.setMacdSignal359(signal359.getValue(endIndex).doubleValue());
				if (endIndex > 0) {
					double prevMacd = macd359.getValue(endIndex - 1).doubleValue();
					double prevSignal = signal359.getValue(endIndex - 1).doubleValue();
					result.setMacd359Status(
							determineMacdStatus(result.macd359, result.macdSignal359, prevMacd, prevSignal));
				}
			}
		}

		// 5. PSAR Indicators
		try {
			if (indicatorsToCalculate.contains("PSAR(0.01)")) {
				ParabolicSarIndicator psar001 = new ParabolicSarIndicator(series, series.numOf(0.01),
						series.numOf(0.01), series.numOf(0.01));
				if (endIndex >= 0) {
					result.setPsar001(psar001.getValue(endIndex).doubleValue());
					result.setPsar001Trend(result.psar001 < currentClose.doubleValue() ? "↑ Uptrend" : "↓ Downtrend");
				}
			}

			if (indicatorsToCalculate.contains("PSAR(0.05)")) {
				ParabolicSarIndicator psar005 = new ParabolicSarIndicator(series, series.numOf(0.05),
						series.numOf(0.05), series.numOf(0.05));
				if (endIndex >= 0) {
					result.setPsar005(psar005.getValue(endIndex).doubleValue());
					result.setPsar005Trend(result.psar005 < currentClose.doubleValue() ? "↑ Uptrend" : "↓ Downtrend");
				}
			}
		} catch (Exception e) {
			logToConsole("PSAR calculation error: " + e.getMessage());
		}

		// Calculate breakout counts
		if (indicatorsToCalculate.contains("Breakout Count")) {
			// MACD Breakout Count
			if (indicatorsToCalculate.contains("MACD")) {
				result.setMacdBreakoutCount(calculateBreakoutCount(series, i -> {
					if (i < 34)
						return 0;
					double macd = new MACDIndicator(closePrice, 12, 26).getValue(i).doubleValue();
					double signal = new EMAIndicator(new MACDIndicator(closePrice, 12, 26), 9).getValue(i)
							.doubleValue();
					return macd > signal ? 1 : -1;
				}));
			}

			// MACD(5,8,9) Breakout Count
			if (indicatorsToCalculate.contains("MACD(5,8,9)")) {
				result.setMacd359BreakoutCount(calculateBreakoutCount(series, i -> {
					if (i < 16)
						return 0;
					double macd = new MACDIndicator(closePrice, 5, 8).getValue(i).doubleValue();
					double signal = new EMAIndicator(new MACDIndicator(closePrice, 5, 8), 9).getValue(i).doubleValue();
					return macd > signal ? 1 : -1;
				}));
			}

			// PSAR(0.01) Breakout Count
			if (indicatorsToCalculate.contains("PSAR(0.01)")) {
				result.setPsar001BreakoutCount(calculateBreakoutCount(series, i -> {
					if (i < 1)
						return 0;
					double psar = new ParabolicSarIndicator(series, series.numOf(0.01), series.numOf(0.01),
							series.numOf(0.01)).getValue(i).doubleValue();
					return psar < series.getBar(i).getClosePrice().doubleValue() ? 1 : -1;
				}));
			}

			// PSAR(0.05) Breakout Count
			if (indicatorsToCalculate.contains("PSAR(0.05)")) {
				result.setPsar005BreakoutCount(calculateBreakoutCount(series, i -> {
					if (i < 1)
						return 0;
					double psar = new ParabolicSarIndicator(series, series.numOf(0.05), series.numOf(0.05),
							series.numOf(0.05)).getValue(i).doubleValue();
					return psar < series.getBar(i).getClosePrice().doubleValue() ? 1 : -1;
				}));
			}
		}

		// MACD Breakout Count
		if (indicatorsToCalculate.contains("MACD Breakout")) {
			result.setMacdBreakoutCount(calculateBreakoutCount(series, i -> {
				if (i < 34)
					return 0;
				double macd = new MACDIndicator(closePrice, 12, 26).getValue(i).doubleValue();
				double signal = new EMAIndicator(new MACDIndicator(closePrice, 12, 26), 9).getValue(i).doubleValue();
				return macd > signal ? 1 : -1;
			}));
		}

		// MACD(5,8,9) Breakout Count
		if (indicatorsToCalculate.contains("MACD(5,8,9) Breakout")) {
			result.setMacd359BreakoutCount(calculateBreakoutCount(series, i -> {
				if (i < 16)
					return 0;
				double macd = new MACDIndicator(closePrice, 5, 8).getValue(i).doubleValue();
				double signal = new EMAIndicator(new MACDIndicator(closePrice, 5, 8), 9).getValue(i).doubleValue();
				return macd > signal ? 1 : -1;
			}));
		}

		// PSAR Breakout Counts
		if (indicatorsToCalculate.contains("PSAR(0.01) Breakout")) {
			result.setPsar001BreakoutCount(calculateBreakoutCount(series, i -> {
				if (i < 1)
					return 0;
				double psar = new ParabolicSarIndicator(series, series.numOf(0.01), series.numOf(0.01),
						series.numOf(0.01)).getValue(i).doubleValue();
				return psar < series.getBar(i).getClosePrice().doubleValue() ? 1 : -1;
			}));
		}

		if (indicatorsToCalculate.contains("PSAR(0.05) Breakout")) {
			result.setPsar005BreakoutCount(calculateBreakoutCount(series, i -> {
				if (i < 1)
					return 0;
				double psar = new ParabolicSarIndicator(series, series.numOf(0.05), series.numOf(0.05),
						series.numOf(0.05)).getValue(i).doubleValue();
				return psar < series.getBar(i).getClosePrice().doubleValue() ? 1 : -1;
			}));
		}

		return result;
	}

	// Helper method to calculate breakout count
	private int calculateBreakoutCount(BarSeries series, Function<Integer, Integer> trendEvaluator) {
		int endIndex = series.getEndIndex();
		if (endIndex < 1)
			return 0;

		int count = 0;
		int currentTrend = trendEvaluator.apply(endIndex);

		for (int i = endIndex - 1; i >= 0; i--) {
			int trend = trendEvaluator.apply(i);
			if (trend != currentTrend) {
				break;
			}
			count++;
		}

		return count;
	}

	// Helper method for MACD status
	private String determineMacdStatus(double currentMacd, double currentSignal, double prevMacd, double prevSignal) {
		if (currentMacd > currentSignal && prevMacd <= prevSignal) {
			return "Bullish Crossover";
		} else if (currentMacd < currentSignal && prevMacd >= prevSignal) {
			return "Bearish Crossover";
		}
		return "Neutral";
	}

	private String formatPsarValue(double psarValue, String trend) {
		return String.format("%s (%.4f)", trend, psarValue);
	}

	private String getTrendStatus(AnalysisResult result) {
		if (result.price > result.sma50 && result.price > result.sma200) {
			return "Strong Uptrend";
		} else if (result.price < result.sma50 && result.price < result.sma200) {
			return "Strong Downtrend";
		} else if (result.price > result.sma50) {
			return "Mild Uptrend";
		} else {
			return "Mild Downtrend";
		}
	}

	private String getRsiStatus(AnalysisResult result) {
		if (result.rsi > 70)
			return "Overbought";
		if (result.rsi < 30)
			return "Oversold";
		if (result.rsi > 50)
			return "Bullish";
		return "Bearish";
	}

	private String getActionRecommendation(AnalysisResult result) {
		int score = 0;

		// Trend Analysis (SMA50 vs SMA200)
		if (result.sma50 > result.sma200 && result.price > result.sma50) {
			score += 2; // Bullish trend
		} else if (result.sma50 < result.sma200 && result.price < result.sma50) {
			score -= 2; // Bearish trend
		} else if (result.sma50 > result.sma200) {
			score += 1; // Mild bullish
		} else if (result.sma50 < result.sma200) {
			score -= 1; // Mild bearish
		}

		// RSI Analysis
		if (result.rsi < 30) {
			score += 2; // Oversold
		} else if (result.rsi > 70) {
			score -= 2; // Overbought
		} else if (result.rsi >= 50 && result.rsi <= 70) {
			score += 1; // Bullish momentum
		} else if (result.rsi >= 30 && result.rsi < 50) {
			score -= 1; // Bearish momentum
		}

		// MACD Analysis
		if ("Bullish Crossover".equals(result.macdStatus) || "Bullish Crossover".equals(result.macd359Status)) {
			score += 2;
		} else if ("Bearish Crossover".equals(result.macdStatus) || "Bearish Crossover".equals(result.macd359Status)) {
			score -= 2;
		} else if (result.macd > result.macdSignal || result.macd359 > result.macdSignal359) {
			score += 1;
		} else if (result.macd < result.macdSignal || result.macd359 < result.macdSignal359) {
			score -= 1;
		}

		// PSAR Analysis
		if ("↑ Uptrend".equals(result.psar001Trend) || "↑ Uptrend".equals(result.psar005Trend)) {
			score += 1;
		} else if ("↓ Downtrend".equals(result.psar001Trend) || "↓ Downtrend".equals(result.psar005Trend)) {
			score -= 1;
		}

		// Determine Recommendation
		if (score >= 6) {
			return "Strong Buy";
		} else if (score >= 3) {
			return "Buy";
		} else if (score <= -6) {
			return "Strong Sell";
		} else if (score <= -3) {
			return "Sell";
		} else {
			return "Neutral";
		}
	}

	private class AnalysisResult {
		double price;
		double sma50;
		double sma200;
		double rsi;
		double macd;
		double macdSignal;
		String macdStatus;
		double macd359;
		double macdSignal359;
		String macd359Status;
		double psar001;
		String psar001Trend;
		double psar005;
		String psar005Trend;

		int macdBreakoutCount;
		int macd359BreakoutCount;
		int psar001BreakoutCount;
		int psar005BreakoutCount;

		public void setPrice(double price) {
			this.price = price;
		}

		public void setSma50(double sma50) {
			this.sma50 = sma50;
		}

		public void setSma200(double sma200) {
			this.sma200 = sma200;
		}

		public void setRsi(double rsi) {
			this.rsi = rsi;
		}

		public void setMacd(double macd) {
			this.macd = macd;
		}

		public void setMacdSignal(double macdSignal) {
			this.macdSignal = macdSignal;
		}

		public void setMacdStatus(String macdStatus) {
			this.macdStatus = macdStatus;
		}

		public void setMacd359(double macd359) {
			this.macd359 = macd359;
		}

		public void setMacdSignal359(double macdSignal359) {
			this.macdSignal359 = macdSignal359;
		}

		public void setMacd359Status(String macd359Status) {
			this.macd359Status = macd359Status;
		}

		public void setPsar001(double psar001) {
			this.psar001 = psar001;
		}

		public void setPsar001Trend(String psar001Trend) {
			this.psar001Trend = psar001Trend;
		}

		public void setPsar005(double psar005) {
			this.psar005 = psar005;
		}

		public void setPsar005Trend(String psar005Trend) {
			this.psar005Trend = psar005Trend;
		}

		public void setMacdBreakoutCount(int macdBreakoutCount) {
			this.macdBreakoutCount = macdBreakoutCount;
		}

		public void setMacd359BreakoutCount(int macd359BreakoutCount) {
			this.macd359BreakoutCount = macd359BreakoutCount;
		}

		public void setPsar001BreakoutCount(int psar001BreakoutCount) {
			this.psar001BreakoutCount = psar001BreakoutCount;
		}

		public void setPsar005BreakoutCount(int psar005BreakoutCount) {
			this.psar005BreakoutCount = psar005BreakoutCount;
		}
	}

	private static class BreakoutValue implements Comparable<BreakoutValue> {
		final int sortValue;
		final String displayValue;

		public BreakoutValue(int sortValue, String displayValue) {
			this.sortValue = sortValue;
			this.displayValue = displayValue;
		}

		@Override
		public int compareTo(BreakoutValue o) {
			return Integer.compare(this.sortValue, o.sortValue);
		}

		@Override
		public String toString() {
			return displayValue;
		}
	}

	public AlertManager getAlertManager() {
		if (alertManager == null) {
			MessageManager messageManager = new MessageManager();
			ConfigManager configManager = new ConfigManager();
			alertManager = new AlertManager(messageManager, configManager, statusLabel);
		}
		return alertManager;
	}

	public void setAlertManager(AlertManager alertManager) {
		this.alertManager = alertManager;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			StockDashboard dashboard = new StockDashboard();
			dashboard.setVisible(true);
		});
	}

	// saving files:

	private void saveTableToCSV() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save Results as CSV");
		fileChooser.setSelectedFile(new File("stock_results.csv"));

		int userSelection = fileChooser.showSaveDialog(this);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToSave = fileChooser.getSelectedFile();
			if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
				fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
			}

			try (PrintWriter writer = new PrintWriter(fileToSave)) {
				// Write CSV header
				for (int i = 0; i < dashboardTable.getColumnCount(); i++) {
					writer.print(dashboardTable.getColumnName(i));
					if (i < dashboardTable.getColumnCount() - 1) {
						writer.print(",");
					}
				}
				writer.println();

				// Write data rows
				for (int row = 0; row < dashboardTable.getRowCount(); row++) {
					for (int col = 0; col < dashboardTable.getColumnCount(); col++) {
						Object value = dashboardTable.getValueAt(row, col);

						// Handle special cases
						if (value instanceof Boolean) {
							writer.print(((Boolean) value) ? "1" : "0");
						} else if (value instanceof BreakoutValue) {
							writer.print(((BreakoutValue) value).displayValue);
						} else {
							// Escape quotes and commas in strings
							String strValue = String.valueOf(value).replace("\"", "\"\"").replace(",", "\\,");

							// Quote strings that contain commas or quotes
							if (strValue.contains(",") || strValue.contains("\"")) {
								strValue = "\"" + strValue + "\"";
							}
							writer.print(strValue);
						}

						if (col < dashboardTable.getColumnCount() - 1) {
							writer.print(",");
						}
					}
					writer.println();
				}

				JOptionPane.showMessageDialog(this, "Results saved successfully to:\n" + fileToSave.getAbsolutePath(),
						"Export Complete", JOptionPane.INFORMATION_MESSAGE);
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Save Error",
						JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}
	}

	private List<Integer> getVisibleRowsInOrder() {
		List<Integer> rows = new ArrayList<>();
		for (int i = 0; i < dashboardTable.getRowCount(); i++) {
			int modelRow = dashboardTable.convertRowIndexToModel(i);
			String symbol = (String) tableModel.getValueAt(modelRow, 1);
			if (!tableModel.isSymbolHidden(symbol)) {
				rows.add(i);
			}
		}
		return rows;
	}
}