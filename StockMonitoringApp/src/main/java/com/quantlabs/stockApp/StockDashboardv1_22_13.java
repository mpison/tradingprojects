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
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
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
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.jdesktop.swingx.JXDatePicker;
import org.json.JSONArray;
import org.json.JSONObject;

import com.quantlabs.stockApp.alert.AlertManager;
import com.quantlabs.stockApp.alert.ConfigManager;
import com.quantlabs.stockApp.alert.MessageManager;
import com.quantlabs.stockApp.alert.ZScoreAlertManager;
import com.quantlabs.stockApp.alert.model.VolumeAlertAnalyser;
import com.quantlabs.stockApp.alert.model.VolumeAlertConfig;
import com.quantlabs.stockApp.alert.model.VolumeAlertTimeframeConfig;
import com.quantlabs.stockApp.alert.model.ZScoreAlertConfig;
import com.quantlabs.stockApp.alert.ui.VolumeAlertConfigDialog;
import com.quantlabs.stockApp.alert.ui.ZScoreAlertConfigDialog;
import com.quantlabs.stockApp.analysis.ui.ZScoreRankingGUI;
import com.quantlabs.stockApp.chart.SymbolChartWindow;
import com.quantlabs.stockApp.config.DashboardConfigManager;
import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.data.StockDataProvider;
import com.quantlabs.stockApp.data.StockDataProviderFactory;
import com.quantlabs.stockApp.exception.StockApiException;
import com.quantlabs.stockApp.graph.MonteCarloConfig;
import com.quantlabs.stockApp.graph.MonteCarloConfigManager;
import com.quantlabs.stockApp.indicator.management.CustomIndicator;
import com.quantlabs.stockApp.indicator.management.CustomIndicatorsConfigDialog;
import com.quantlabs.stockApp.indicator.management.IndicatorsManagementApp;
import com.quantlabs.stockApp.indicator.management.StrategyConfig;
import com.quantlabs.stockApp.indicator.management.StrategyExecutionService;
import com.quantlabs.stockApp.indicator.management.ZScoreCalculator;
import com.quantlabs.stockApp.indicator.strategy.AbstractIndicatorStrategy;
import com.quantlabs.stockApp.indicator.strategy.MovingAverageTargetValueStrategy;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.model.PriceDataColumnRegistry;
import com.quantlabs.stockApp.model.WatchlistData;
import com.quantlabs.stockApp.reports.AnalysisResult;
import com.quantlabs.stockApp.reports.MonteCarloGraphApp;
import com.quantlabs.stockApp.service.AnalysisOrchestrator;
import com.quantlabs.stockApp.service.TechnicalAnalysisService;
import com.quantlabs.stockApp.service.tradingview.TradingViewService;
import com.quantlabs.stockApp.service.yahoo.YahooFinanceService;
import com.quantlabs.stockApp.utils.DashboardColumnCollector;
import com.quantlabs.stockApp.utils.PriceDataMapper;
import com.quantlabs.stockApp.utils.StrategyCheckerHelper;
import com.quantlabs.stockApp.utils.ZScoreColumnHelper;

import okhttp3.OkHttpClient;

public class StockDashboardv1_22_13 extends JFrame implements IStockDashboard {
	private final OkHttpClient client = new OkHttpClient();
	private final String apiKey = "PK4UOZQDJJZ6WBAU52XM";
	private final String apiSecret = "Fag4ha2D58VyL0okwXgBHD1IvhoptmI2KiacMaNG";
	private final String polygonApiKey = "fMCQBX5yvQZL_s6Zi1r9iKBkMkNLzNcw"; // Replace with your Polygon.io API key

	// private final String[] symbols = { "AAPL", "AVGO", "PLTR", "MSFT", "GOOGL",
	// "AMZN", "TSLA", "NVDA", "META", "NFLX",
	// "WMT", "JPM", "LLY", "V", "ORCL", "MA", "XOM", "COST", "JNJ", "HD", "UNH",
	// "DLTR" };

	private final String[] symbols = { "NVDA" };

	private static final Map<String, Integer> ALPACA_TIME_LIMITS = Map.of("1Min", 4, "5Min", 7, "15Min", 7, "30Min", 30,
			"1H", 30, "4H", 30, "1D", 60);

	private static final Map<String, Integer> YAHOO_TIME_LIMITS = Map.of("1Min", 7, "5Min", 60, "15Min", 60, "30Min",
			60, "1H", 60, "4H", 60, "1D", 60);

	private static final Map<String, Integer> POLYGON_TIME_LIMITS = Map.of("1Min", 7, "5Min", 30, "15Min", 30, "30Min",
			60, "1H", 60, "4H", 90, "1D", 365, "1W", 730);

	private final String[] dataSources = { "Alpaca", "Yahoo", "Polygon" };
	private final String[] allTimeframes = { "1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min" };

	private final String[] allIndicators = { "TREND", "RSI", "MACD", "MACD Breakout", "MACD(5,8,9)", "HeikenAshi",
			"MACD(5,8,9) Breakout", "PSAR(0.01)", "PSAR(0.01) Breakout", "PSAR(0.05)", "PSAR(0.05) Breakout",
			"Breakout Count", "Action", "MOVINGAVERAGETARGETVALUE", "HIGHESTCLOSEOPEN", "Volume", "VOLUMEMA(20)",
			"VWAP" };

	/*
	 * private final String[] allIndicators = { "Trend", "RSI", "MACD",
	 * "MACD(5,8,9)", "PSAR(0.05)", "PSAR(0.01)", "Action", "Breakout Count" };
	 */

	// Add these near the top of your class with other constants
	// private static final Set<String> BULLISH_STATUSES = Set.of("Strong Bullish",
	// "Bullish Crossover", "Bullish",
	// "Strong Uptrend", "Mild Uptrend", "↑ Uptrend", "Buy", "Uptrend");

	private static final Set<String> BULLISH_STATUSES = StrategyCheckerHelper.BULLISH_STATUSES;

	private Set<String> selectedTimeframes = new HashSet<>(
			Arrays.asList("1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min"));

	private static final String BREAKOUT_COUNT_COLUMN_PATTERN = "Breakout";

	private static final String ZSCORE_COLUMN_PATTERN = "Zscore_";

	private final Integer[] refreshIntervals = { 10, 30, 60, 120, 300, 600, 900 }; // Seconds

	private static final Color BULLISH_COLOR = new Color(200, 255, 200); // Light green
	private static final Color BEARISH_COLOR = new Color(255, 200, 200); // Light red

	private List<Integer> indicatorColumnIndices;
	private boolean columnsInitialized = false;

	private JTable dashboardTable;
	private FilterableTableModel tableModel;
	private JButton refreshButton;
	private JButton startLiveButton;
	private JButton stopLiveButton;
	private JButton toggleVisibilityButton;
	
	private boolean isProcessingUpdates = false;

	private JCheckBox checkBullishCheckbox;
	private VolumeAlertConfig volumeAlertConfig = new VolumeAlertConfig();
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
	private JComboBox<String> volumeSourceCombo;
	private JComboBox<String> watchlistComboBox;
	private JComboBox<Integer> refreshIntervalComboBox;
	private ScheduledExecutorService scheduler;
	private boolean isLive = false;
	private final Map<String, Color> statusColors = new HashMap<>();
	private final Map<String, WatchlistData> watchlists = new HashMap<>();
	private String currentWatchlist = "Default";
	private String currentDataSource = "Alpaca";
	private int currentRefreshInterval = 60; // Default interval in seconds

	private List<String> watchListException = Arrays.asList("YahooDOW", "YahooNASDAQ", "YahooSP500", "YahooRussel2k",
			"YahooPennyBy3MonVol", "YahooPennyByPercentChange", "TradingViewPennyStockPreMarket",
			"TradingViewPennyStockStandardMarket", "TradingViewPennyStockPostMarket",
			"TradingViewIndexStocksByPreMarketVolume", "TradingViewIndexStocksByStandardMarketVolume",
			"TradingViewIndexStocksMarketByPostMarketVolume");

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

	private JComboBox<String> startHourCombo;
	private JComboBox<String> startMinuteCombo;
	private JComboBox<String> endHourCombo;
	private JComboBox<String> endMinuteCombo;

	private JTextField prevVolMinField;
	private JTextField prevVolMaxField;
	private JTextField currentVolMinField;
	private JTextField currentVolMaxField;

	YahooFinanceService yahooQueryFinance = new YahooFinanceService();

	TradingViewService tradingViewService = new TradingViewService();

	private TechnicalAnalysisService technicalAnalysisService;
	private AnalysisOrchestrator analysisOrchestrator;

	private IndicatorsManagementApp indicatorsManagementApp;
	Set<CustomIndicator> loadedCustomIndicators = new HashSet<>();
	private StrategyExecutionService strategyExecutionService;

	private JPanel controlPanel;
	private JButton listSymbolsButton;
	private JLabel statusLabel;
	private JButton checkNonBullishButton;

	// indicator configuration
	private Map<String, Set<String>> timeframeIndicators = new LinkedHashMap<>();
	private Map<String, JCheckBox> timeframeCheckboxes = new HashMap<>();
	private Map<String, Map<String, JCheckBox>> indicatorCheckboxes = new HashMap<>();
	private final Map<String, JComboBox<String>> indicatorTimeRangeCombos = new HashMap<>();
	private final Map<String, JComboBox<String>> indicatorSessionCombos = new HashMap<>();
	private Map<String, Map<String, Integer>> timeframeIndexRanges = new HashMap<>();
	private Map<String, Map<String, JTextField>> timeframeIndexFields = new HashMap<>();
	private final Map<String, JTextField> indicatorIndexCounterFields = new HashMap<>();
	private final Map<String, Integer> timeframeIndexCounters = new HashMap<>();
	private Map<String, Set<String>> customIndicatorCombinations = new HashMap<>();

	private JPanel bullishTimeframePanel;
	private Map<String, JCheckBox> bullishTimeframeCheckboxes = new HashMap<>();

	private JCheckBox inheritCheckbox;
	private MessageManager messageManager;
	private ConfigManager configManager;
	private DashboardConfigManager dashboardConfigManager;

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

	private JCheckBox enableLogReportsCheckbox;
	private JCheckBox completeResultsCheckbox;
	private JCheckBox bullishChangeCheckbox;
	private JCheckBox bullishVolumeCheckbox;

	private JCheckBox uptrendFilterCheckbox;
	private boolean uptrendFilterEnabled = true;

	private JCheckBox showAveVolumeCheckBox;
	private JCheckBox showPrevLastDayPriceCheckBox;

	private ScheduledExecutorService logScheduler;

	private StringBuffer consoleLogBuffer = new StringBuffer();

	private JTextField logFileNameField;

	private StockDataProviderFactory dataProviderFactory;
	private StockDataProvider currentDataProvider;

	// main table
	ConcurrentHashMap<String, PriceData> priceDataMap = new ConcurrentHashMap<String, PriceData>();

	// zscore on table
	private final ZScoreColumnHelper zScoreColumnHelper;
	private ZScoreRankingGUI zScoreRankingGUI;
	private ZScoreAlertManager zScoreAlertManager;
	private final Map<String, JCheckBox> zScoreCheckboxes = new HashMap<>();
	private ZScoreAlertConfigDialog zScoreAlertConfigDialog;

	// strategies
	private StrategyCheckerHelper strategyCheckerHelper;

	// montecarlo config
	// Add near other configuration variables in StockDashboardv1_22_13
	private Map<String, Object> monteCarloConfig = new HashMap<>();
	private JMenu graphMenu;
	private JMenuItem monteCarloMenuItem;
	private JMenuItem monteCarloConfigMenuItem;

	private final ExecutorService executorService = Executors
			.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	

	/*
	 * public class FilterableTableModel extends DefaultTableModel { private final
	 * Set<String> hiddenSymbols; private final Map<String, Integer> symbolToRowMap;
	 * private Map<String, double[]> columnFilters = new HashMap<>();
	 * 
	 * // private Vector<String> columnIdentifiers;
	 * 
	 * public FilterableTableModel(Vector<Vector<Object>> data, Vector<String>
	 * columnNames) { super(data, columnNames); this.hiddenSymbols =
	 * Collections.synchronizedSet(new HashSet<>()); this.symbolToRowMap = new
	 * ConcurrentHashMap<>(); }
	 * 
	 * public FilterableTableModel(Object[] columnNames, int rowCount) {
	 * super(columnNames, rowCount); this.hiddenSymbols =
	 * Collections.synchronizedSet(new HashSet<>()); this.symbolToRowMap = new
	 * ConcurrentHashMap<>(); this.columnIdentifiers = new
	 * Vector<>(Arrays.asList((String[]) columnNames)); }
	 * 
	 * public void setColumnFilters(Map<String, double[]> newFilters) {
	 * this.columnFilters = newFilters != null ? new HashMap<>(newFilters) : new
	 * HashMap<>(); }
	 * 
	 * @Override public synchronized int getRowCount() { try { return
	 * super.getRowCount() - (hiddenSymbols != null ? hiddenSymbols.size() : 0); }
	 * catch (Exception e) { logToConsole("Error in getRowCount: " +
	 * e.getMessage()); return 0; } }
	 * 
	 * @Override public synchronized Object getValueAt(int row, int column) { try {
	 * int modelRow = convertViewRowToModel(row); return super.getValueAt(modelRow,
	 * column); } catch (Exception e) { logToConsole("Error in getValueAt: " +
	 * e.getMessage()); return null; } }
	 * 
	 * private synchronized int convertViewRowToModel(int viewRow) { try { int
	 * visibleCount = -1; for (int i = 0; i < super.getRowCount(); i++) { String
	 * symbol = (String) super.getValueAt(i, 1); if (symbol != null &&
	 * !hiddenSymbols.contains(symbol)) { visibleCount++; if (visibleCount ==
	 * viewRow) { return i; } } } return -1; } catch (Exception e) {
	 * logToConsole("Error in convertViewRowToModel: " + e.getMessage()); return -1;
	 * } }
	 * 
	 * @Override public synchronized void addRow(Object[] rowData) { try {
	 * super.addRow(rowData); if (rowData != null && rowData.length > 1 &&
	 * rowData[1] instanceof String) { String symbol = (String) rowData[1];
	 * symbolToRowMap.put(symbol, super.getRowCount() - 1); } } catch (Exception e)
	 * { logToConsole("Error adding row: " + e.getMessage()); } }
	 * 
	 * @Override public synchronized void removeRow(int row) { try { String symbol =
	 * (String) getValueAt(row, 1); super.removeRow(row); if (symbol != null) {
	 * symbolToRowMap.remove(symbol); hiddenSymbols.remove(symbol); }
	 * rebuildSymbolMap(); } catch (Exception e) {
	 * logToConsole("Error removing row: " + e.getMessage()); } }
	 * 
	 * private synchronized void rebuildSymbolMap() { try { symbolToRowMap.clear();
	 * for (int i = 0; i < super.getRowCount(); i++) { Object value =
	 * super.getValueAt(i, 1); if (value instanceof String) {
	 * symbolToRowMap.put((String) value, i); } } } catch (Exception e) {
	 * logToConsole("Error rebuilding symbol map: " + e.getMessage()); } }
	 * 
	 * public synchronized void toggleSymbolsVisibility(Set<String> symbolsToToggle)
	 * { try { if (symbolsToToggle == null || symbolsToToggle.isEmpty()) { return; }
	 * 
	 * for (String symbol : symbolsToToggle) { if (hiddenSymbols.contains(symbol)) {
	 * hiddenSymbols.remove(symbol); } else { hiddenSymbols.add(symbol); } }
	 * fireTableDataChanged(); } catch (Exception e) {
	 * logToConsole("Error in toggleSymbolsVisibility: " + e.getMessage()); } }
	 * 
	 * public synchronized boolean isSymbolHidden(String symbol) { try { return
	 * hiddenSymbols != null && hiddenSymbols.contains(symbol); } catch (Exception
	 * e) { logToConsole("Error in isSymbolHidden: " + e.getMessage()); return
	 * false; } }
	 * 
	 * public synchronized void showAllSymbols() { try { if (hiddenSymbols != null)
	 * { hiddenSymbols.clear(); fireTableDataChanged(); } } catch (Exception e) {
	 * logToConsole("Error in showAllSymbols: " + e.getMessage()); } }
	 * 
	 * @Override public synchronized boolean isCellEditable(int row, int column) {
	 * try { int modelRow = convertViewRowToModel(row); return modelRow != -1 &&
	 * column == 0; } catch (Exception e) { logToConsole("Error in isCellEditable: "
	 * + e.getMessage()); return false; } }
	 * 
	 * @Override public synchronized Class<?> getColumnClass(int column) {
	 * 
	 * String colName = getColumnName(column);
	 * 
	 * // Z-Score column detection if (colName != null) { if
	 * (colName.startsWith("ZScore_")) { if (colName.endsWith("_Rank")) { return
	 * Integer.class; // Use Integer for ranks } else if
	 * (colName.endsWith("_ZScore")) { return Double.class; // Use Double for
	 * Z-Scores } } }
	 * 
	 * List<String> datacolumns = new ArrayList<>(Arrays.asList("postmarketClose",
	 * "premarketChange", "changeFromOpen", "percentChange", "postmarketChange",
	 * "gap", "premarketVolume", "currentVolume", "postmarketVolume",
	 * "previousVolume", "prevLastDayPrice", "averageVol", "analystRating",
	 * "premarketHigh", "high", "postmarketHigh", "premarketLow", "low",
	 * "postmarketLow", "premarketHighestPercentile", "marketHighestPercentile",
	 * "postmarketHighestPercentile"));
	 * 
	 * if (colName == null) { return String.class; // Fallback } if
	 * (colName.equals("Select") || colName.equals("Checkbox")) { return
	 * Boolean.class; } else if (colName.equals("Symbol")) { return String.class; }
	 * else if (colName.equals("Price") || colName.equals("% Change") ||
	 * colName.contains("Change")) { return Double.class; } else if
	 * (colName.equals("Volume") || colName.equals("Prev Volume") ||
	 * colName.contains("Vol")) { return Long.class; } else if
	 * (colName.contains("Breakout") || colName.contains("Breakout Count")) { return
	 * BreakoutValue.class; } else if (datacolumns.contains(colName)) { return
	 * Double.class; } // Add more mappings for other specific columns if needed
	 * (e.g., RSI, MACD) return String.class; }
	 * 
	 * @Override public synchronized void setValueAt(Object value, int row, int
	 * column) { try { String colName = getColumnName(column);
	 * 
	 * // Handle Z-Score columns specifically if (colName != null &&
	 * colName.startsWith("ZScore_")) { int modelRow = convertViewRowToModel(row);
	 * if (modelRow != -1) { // Ensure data type consistency if
	 * (colName.endsWith("_Rank") && value instanceof Double) { value = ((Double)
	 * value).intValue(); } super.setValueAt(value, modelRow, column); } } else { //
	 * Existing logic for other columns int modelRow = convertViewRowToModel(row);
	 * if (modelRow != -1) { super.setValueAt(value, modelRow, column); } } } catch
	 * (Exception e) { logToConsole("Error in setValueAt: " + e.getMessage()); } }
	 * 
	 * // Update existing methods to use columnIdentifiers
	 * 
	 * @Override public String getColumnName(int column) { return (String)
	 * this.columnIdentifiers.get(column); }
	 * 
	 * @Override public int getColumnCount() { return columnIdentifiers.size(); }
	 * 
	 * // Add this method to remove columns public void removeColumn(int column) {
	 * try { // Remove from column identifiers columnIdentifiers.remove(column);
	 * 
	 * // Remove from each row's data for (Object rowObj : dataVector) { if (rowObj
	 * instanceof Vector) { ((Vector<?>) rowObj).remove(column); } }
	 * 
	 * // Update column count by calling super's method // We don't need to directly
	 * modify columnCount since it's handled by the model
	 * fireTableStructureChanged();
	 * 
	 * // Rebuild symbol map if needed if (column == 1) { // If removing symbol
	 * column rebuildSymbolMap(); }
	 * 
	 * // New: Refresh sorter (assuming dashboardTable is accessible; otherwise,
	 * expose // it) if (dashboardTable != null && dashboardTable.getRowSorter()
	 * instanceof TableRowSorter) { TableRowSorter<FilterableTableModel> sorter =
	 * (TableRowSorter<FilterableTableModel>) dashboardTable .getRowSorter();
	 * sorter.setModel(this); // Rebind model to sorter
	 * sorter.modelStructureChanged(); // Force sorter update }
	 * 
	 * SwingUtilities.invokeLater(() -> updateTableRenderers()); } catch
	 * (ArrayIndexOutOfBoundsException e) { logToConsole("Error removing column: " +
	 * e.getMessage()); } }
	 * 
	 * public Vector<?> getColumnIdentifiers() { return this.columnIdentifiers; }
	 * 
	 * public boolean rowSatisfiesFilters(int modelRow) { for (Map.Entry<String,
	 * double[]> entry : columnFilters.entrySet()) { String colName =
	 * entry.getKey(); int col = findColumn(colName); if (col == -1) continue;
	 * 
	 * Object val = super.getValueAt(modelRow, col); if (val == null) return false;
	 * 
	 * double v; if (val instanceof Number) { v = ((Number) val).doubleValue(); }
	 * else { return false; }
	 * 
	 * double[] range = entry.getValue(); double min = range[0]; double max =
	 * range[1];
	 * 
	 * if (v < min || v > max) { return false; } } return true; } }
	 */
	public StockDashboardv1_22_13() {

		this.dataProviderFactory = new StockDataProviderFactory(client, apiKey, apiSecret, polygonApiKey);
		this.currentDataProvider = dataProviderFactory.getProvider(currentDataSource, this::logToConsole);

		// Initialize analysis services
		this.technicalAnalysisService = new TechnicalAnalysisService(this::logToConsole);
		this.analysisOrchestrator = new AnalysisOrchestrator(technicalAnalysisService, currentDataProvider);
		// Initialize configuration manager
		this.dashboardConfigManager = new DashboardConfigManager(this, this::logToConsole);

		// zscale
		this.zScoreColumnHelper = new ZScoreColumnHelper(this);

		// Initialize StrategyExecutionService FIRST
		this.strategyExecutionService = createStrategyExecutionService();

		// Initialize IndicatorsManagementApp
		checkAndInstantiateIndicatorsManagementApp();

		initializeMonteCarloConfig();

		// Initialize strategy checker helper
		this.strategyCheckerHelper = new StrategyCheckerHelper(indicatorsManagementApp, priceDataMap,
				this::logToConsole);

		this.zScoreAlertManager = new ZScoreAlertManager(this, strategyCheckerHelper);

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
		statusColors.clear();

		// Uptrend variations
		statusColors.put("Strong Uptrend", new Color(100, 255, 100));
		statusColors.put("↑ Uptrend", new Color(100, 255, 100));
		statusColors.put("Uptrend", new Color(100, 255, 100));
		statusColors.put("Mild Uptrend", new Color(180, 255, 180));

		// Downtrend variations
		statusColors.put("Strong Downtrend", new Color(255, 100, 100));
		statusColors.put("↓ Downtrend", new Color(255, 100, 100));
		statusColors.put("Downtrend", new Color(255, 100, 100));
		statusColors.put("Mild Downtrend", new Color(255, 180, 180));

		// MACD statuses
		statusColors.put("Bullish Crossover", new Color(150, 255, 150));
		statusColors.put("Bearish Crossover", new Color(255, 150, 150));

		// RSI statuses
		statusColors.put("Overbought", new Color(255, 150, 150));
		statusColors.put("Oversold", new Color(150, 255, 150));
		statusColors.put("Bullish", new Color(200, 255, 200));
		statusColors.put("Bearish", new Color(255, 200, 200));

		// Action recommendations
		statusColors.put("Strong Buy", new Color(50, 200, 50));
		statusColors.put("Buy", new Color(120, 255, 120));
		statusColors.put("Sell", new Color(255, 120, 120));
		statusColors.put("Strong Sell", new Color(200, 50, 50));
		statusColors.put("Neutral", new Color(240, 240, 240));
	}

	private void initializeWatchlists() {
		watchlists.put("Default", new WatchlistData(new HashSet<>(Arrays.asList(symbols)), ""));
		// watchlists.put("ECOINDEX - ETF", Arrays.asList("DIA", "QQQ", "SPY", "IWM",
		// ""));
		watchlists.put("Tech Stocks", new WatchlistData(
				new HashSet<>(Arrays.asList("AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "META", "TSLA", "NFLX")), ""));
		watchlists.put("Tech Stocks - Application",
				new WatchlistData(new HashSet<>(Arrays.asList("SAP", "CRM", "INTU", "NOW", "UBER", "SHOP", "ADBE",
						"ADP", "MSTR", "CDNS", "SNOW", "ADSK", "WDAY", "ROP", "TEAM", "PAYX", "DDOG", "FICO", "ANSS",
						"HUBS", "PTC", "TYL", "ZM", "GRAB", "SOUN", "WCT", "YMM", "BULL", "LYFT", "UBER", "PD", "WRD",
						"U", "YAAS", "SHOP", "FRSH", "AAPL", "BNZI", "KC", "YB", "LIF", "DUOL", "KARO", "BILL", "AEYE",
						"CVLT", "SPT", "IBTA", "DT", "DFIN", "BTDR", "MAPS", "WYFI", "PCOR", "CRNC", "GTM")), ""));
		watchlists.put("Tech Stocks - Infrastructure",
				new WatchlistData(new HashSet<>(Arrays.asList("LIDR", "PLTR", "MSFT", "PATH", "CORZ", "CRWV", "OKTA",
						"AI", "TOST", "ORCL", "RZLV", "S", "XYZ", "S", "PANW", "CRWD", "SNPS", "FTNT", "NET", "CRWV",
						"XYZ", "ZS", "VRSN", "CHKP", "CPAY", "GDDY", "IOT", "AFRM", "GPN", "NTNX", "TWLO", "MDB",
						"CYBR", "DOX", "GTLB", "ATGL", "MCRP", "ODD", "CALX", "RBRK", "BB", "GRRR", "BKKT", "NUAI",
						"SUPX", "DFDV", "DVLT", "STNE", "CORZ", "PAGS", "ZETA", "NBIS", "XYZ", "CLBT", "SAIL")), ""));
		watchlists.put("ECOINDEX - ETF", new WatchlistData(new HashSet<>(Arrays.asList("DIA", "QQQ", "SPY", "IVV",
				"VOO", "IWM", "XLK", "IGV", "VGT", "FTEC", "ARKQ", "CONL", "GBTC", "SOXL", "SMH", "XSD")), ""));

		watchlists.put("XLK - ECOINDEX",
				new WatchlistData(new HashSet<>(Arrays.asList("NVDA", "MSFT", "AAPL", "AVGO", "PLTR", "ORCL", "CSCO",
						"IBM", "AMD", "CRM", "APP", "NOW", "INTU", "MU", "QCOM", "LRCX", "TXN", "AMAT", "ACN", "APH",
						"ANET", "ADBE", "INTC", "KLAC", "PANW", "CRWD", "ADI", "CDNS", "SNPS", "MSI", "ADSK", "TEL",
						"GLW", "NXPI", "FTNT", "ROP", "WDAY", "STX", "DDOG", "MPWR", "DELL", "WDC", "FICO", "MCHP",
						"CTSH", "HPE", "KEYS", "TDY", "HPQ", "PTC", "SMCI", "NTAP", "FSLR", "TYL", "JBL", "VRSN", "TER",
						"CDW", "TRMB", "IT", "ON", "FFIV", "GDDY", "ZBRA", "GEN", "AKAM", "SWKS", "EPAM")), ""));

		watchlists.put("SOXL - ECOINDEX",
				new WatchlistData(new HashSet<>(Arrays.asList("AVGO", "NVDA", "AMD", "QCOM", "MU", "INTC", "TXN",
						"MRVL", "LRCX", "AMAT", "ASML", "KLAC", "TSM", "MPWR", "ADI", "NXPI", "MCHP", "ALAB", "TER",
						"CRDO", "ON", "ENTG", "SWKS", "RMBS", "NVMI", "MTSI", "ARM", "ASX", "STM", "UMC")), ""));

		watchlists.put("SMH - ECOINDEX",
				new WatchlistData(new HashSet<>(Arrays.asList("NVDA", "TSM", "AVGO", "ASML", "INTC", "AMAT", "LRCX",
						"MU", "KLAC", "AMD", "QCOM", "TXN", "ADI", "CDNS", "SNPS", "MRVL", "NXPI", "MPWR", "MCHP",
						"STM", "TER", "ON", "SWKS", "QRVO", "OLED")), ""));

		watchlists.put("SMH - ECOINDEX",
				new WatchlistData(new HashSet<>(Arrays.asList("TSLA", "KTOS", "TER", "PLTR", "AVAV", "RKLB", "ACHR",
						"AMD", "TRMB", "AMZN", "DE", "TSM", "NVDA", "IRDM", "LHX", "JOBY", "BWXT", "SYM", "PONY",
						"KMTUY", "GOOG", "CCO", "BIDU", "AUR", "SRTA", "ESLT", "OKLO", "SNPS", "LUNR", "BYDDY", "CAT",
						"TDY", "DASH", "QCOM", "ISRG", "KDK")), ""));

		// watchlists.put("Blue Chips", Arrays.asList("WMT", "JPM", "V", "MA", "XOM",
		// "JNJ", "HD"));
		// watchlists.put("Financial", Arrays.asList("V", "JPM", "MA", "AXP", "BAC",
		// "COIN", "BLK", "WFC", "BLK", "MS", "C", "BX", "SOFI"));
		watchlists
				.put("SemiConductors",
						new WatchlistData(new HashSet<>(Arrays.asList("NVDA", "AVGO", "AMD", "TSM", "QCOM", "ARM",
								"MRVL", "NXPI", "MCHP", "ADI", "TXN", "MU", "INTC", "ON", "SWKS", "WOLF", "STM", "TXN",
								"LRCX", "MCHP", "COHR", "AMAT", "TER", "MTSI", "QRVO", "ONTO", "MPWR", "CRUS", "ENTG",
								"GFS", "AMAT", "AMKR", "LSCC", "ADI", "NXPI", "KLAC", "SMTC", "SKYT", "LAES", "POET",
								"PXLW", "LASR", "RMBS", "POWI", "VSH")), ""));
		// watchlists.put("Health Care", Arrays.asList("LLY", "JNJ", "ABBV", "AMGN",
		// "MRK", "PFE", "GILD", "BMY", "BIIB"));

		// watchlists.put("Energy", Arrays.asList("XOM", "COP", "CVX", "TPL", "EOG",
		// "HES", "FANG", "MPC", "PSX", "VLO"));
		/*
		 * watchlists.put("HighVolumes", Arrays.asList("CYCC", "XAGE", "KAPA", "PLRZ",
		 * "ANPA", "IBG", "CRML", "GRO","USAR", "SBET", "MP", "SLDP", "OPEN", "TTD",
		 * "PROK")); watchlists.put("Top50", Arrays.asList("NVDA", "MSFT", "AAPL",
		 * "AMZN", "GOOGL", "META", "2223.SR", "AVGO", "TSM", "TSLA", "BRK-B", "JPM",
		 * "WMT", "ORCL", "LLY", "V", "TCEHY", "NFLX", "MA", "XOM", "COST", "JNJ", "PG",
		 * "PLTR", "1398.HK", "SAP", "HD", "BAC", "ABBV", "005930.KS", "KO",
		 * "601288.SS", "RMS.PA", "NVO", "ASML", "601939.SS", "BABA", "PM", "GE",
		 * "CSCO", "CVX", "IBM", "UNH", "AMD", "ROG.SW", "TMUS", "WFC", "NESN.SW",
		 * "PRX.AS"));
		 */

		watchlists.put("YahooEcoIndex", new WatchlistData(
				new HashSet<>(Arrays.asList("^DJI", "^GSPC", "^IXIC", "^RUT", "SOXL", "SMH", "XLK")), ""));
		watchlists.put("YahooDOW", new WatchlistData(new HashSet<>(Arrays.asList("")), ""));
		watchlists.put("YahooNASDAQ", new WatchlistData(new HashSet<>(Arrays.asList("")), ""));
		watchlists.put("YahooSP500", new WatchlistData(new HashSet<>(Arrays.asList("")), ""));
		watchlists.put("YahooRussel2k", new WatchlistData(new HashSet<>(Arrays.asList("")), ""));
		watchlists.put("YahooPennyBy3MonVol", new WatchlistData(new HashSet<>(Arrays.asList("")), ""));
		watchlists.put("YahooPennyByPercentChange", new WatchlistData(new HashSet<>(Arrays.asList("")), ""));

		watchlists.put("TradingViewPennyStockPreMarket", new WatchlistData(new HashSet<>(Arrays.asList("")), ""));
		watchlists.put("TradingViewPennyStockStandardMarket", new WatchlistData(new HashSet<>(Arrays.asList("")), ""));
		watchlists.put("TradingViewPennyStockPostMarket", new WatchlistData(new HashSet<>(Arrays.asList("")), ""));
		watchlists.put("TradingViewIndexStocksByPreMarketVolume",
				new WatchlistData(new HashSet<>(Arrays.asList("")), ""));
		watchlists.put("TradingViewIndexStocksByStandardMarketVolume",
				new WatchlistData(new HashSet<>(Arrays.asList("")), ""));
		watchlists.put("TradingViewIndexStocksMarketByPostMarketVolume",
				new WatchlistData(new HashSet<>(Arrays.asList("")), ""));

		currentWatchlist = "Default";
	}

	private void initializeConfiguration() {
		// Initialize timeframe-indicator mappings
		for (String tf : allTimeframes) {
			timeframeIndicators.put(tf, new HashSet<>());

			JComboBox<String> timeRangeCombo = new JComboBox<>(new String[] { "1D", "2D", "5D", "7D", "1M" });
			timeRangeCombo.setSelectedItem("1D");

			// Store the combobox for later retrieval
			if (!indicatorTimeRangeCombos.containsKey(tf)) {
				indicatorTimeRangeCombos.put(tf, timeRangeCombo);
			} else {
				timeRangeCombo.setSelectedItem(indicatorTimeRangeCombos.get(tf).getSelectedItem());
			}

			if (!indicatorIndexCounterFields.containsKey(tf)) {
				JTextField indexCounterField = new JTextField("5", 3); // Default value 5
				indicatorIndexCounterFields.put(tf, indexCounterField);
			}

			// Initialize session combobox
			JComboBox<String> sessionCombo = new JComboBox<>(
					new String[] { "all", "premarket", "standard", "postmarket" });
			sessionCombo.setSelectedItem("all");
			indicatorSessionCombos.put(tf, sessionCombo);

			if (!indicatorTimeRangeCombos.containsKey(tf)) {
				indicatorTimeRangeCombos.put(tf, timeRangeCombo);
			}

			// Initialize indicator checkboxes map for each timeframe
			Map<String, JCheckBox> indMap = new HashMap<>();
			for (String ind : allIndicators) {
				indMap.put(ind, new JCheckBox(ind));
			}
			indicatorCheckboxes.put(tf, indMap);

			// Initialize index ranges and fields for each timeframe
			Map<String, Integer> indexRange = new HashMap<>();
			indexRange.put("startIndex", 0);
			indexRange.put("endIndex", 0);
			timeframeIndexRanges.put(tf, indexRange);

			Map<String, JTextField> indexFields = new HashMap<>();
			indexFields.put("startIndex", new JTextField("0", 5));
			indexFields.put("endIndex", new JTextField("0", 5));
			timeframeIndexFields.put(tf, indexFields);
		}

		// Set default selections (optional)
		/*
		 * timeframeIndicators.get("1D").addAll(Arrays.asList("RSI", "MACD",
		 * "MACD Breakout", "MACD(5,8,9)", "MACD(5,8,9) Breakout", "PSAR(0.05)",
		 * "PSAR(0.05) Breakout", "Breakout Count"));
		 */

		/*
		 * timeframeIndicators.get("4H").addAll(Arrays.asList("MACD", "MACD(5,8,9)",
		 * "PSAR(0.05)", "PSAR(0.05) Breakout", "PSAR(0.01)", "PSAR(0.01) Breakout",
		 * "HeikenAshi")); timeframeIndicators.get("30Min").addAll(Arrays.asList("MACD",
		 * "MACD(5,8,9)", "PSAR(0.05)", "PSAR(0.01)", "HeikenAshi"));
		 * 
		 * timeframeIndicators.get("5Min").addAll(Arrays.asList("MACD", "MACD(5,8,9)",
		 * "PSAR(0.05)", "PSAR(0.01)", "HeikenAshi"));
		 * 
		 * timeframeIndicators.get("1Min").addAll(Arrays.asList("MACD", "MACD(5,8,9)",
		 * "PSAR(0.05)", "PSAR(0.01)", "HeikenAshi"));
		 */

		/*
		 * timeframeIndicators.get("4H").addAll(Arrays.asList("MACD", "MACD(5,8,9)",
		 * "PSAR(0.05)", "PSAR(0.05) Breakout", "PSAR(0.01)", "PSAR(0.01) Breakout",
		 * "HeikenAshi"));
		 */

		timeframeIndicators.get("30Min")
				.addAll(Arrays.asList("MACD", "MACD(5,8,9)", "PSAR(0.05)", "PSAR(0.01)", "HeikenAshi"));

		timeframeIndicators.get("5Min").addAll(
				Arrays.asList("MACD", "MACD(5,8,9)", "PSAR(0.05)", "PSAR(0.01)", "HeikenAshi", "HighestCloseOpen"));

		/*
		 * timeframeIndicators.get("1Min") .addAll(Arrays.asList("MACD", "MACD(5,8,9)",
		 * "PSAR(0.05)", "PSAR(0.01)", "HeikenAshi"));
		 */
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

	private void initializeZScoreCheckboxes() {
		// Make sure zScoreCheckboxes map is populated
		if (zScoreCheckboxes.isEmpty()) {
			// This will populate the zScoreCheckboxes map
			createZScoreAttributesPanel();
		}
	}

	private void initializeMonteCarloConfig() {
		// Load default configuration
		monteCarloConfig = MonteCarloConfigManager.createDefaultConfig();

		// Try to load saved configuration if available
		if (columnsConfigData.containsKey(MonteCarloConfigManager.MONTE_CARLO_CONFIG_KEY)) {
			try {
				Map<String, Object> savedConfig = (Map<String, Object>) columnsConfigData
						.get(MonteCarloConfigManager.MONTE_CARLO_CONFIG_KEY);
				if (MonteCarloConfigManager.validateConfig(savedConfig)) {
					monteCarloConfig = MonteCarloConfigManager.mergeWithDefaults(savedConfig);
					logToConsole("Loaded saved Monte Carlo configuration");
				}
			} catch (Exception e) {
				logToConsole("Error loading Monte Carlo configuration: " + e.getMessage());
			}
		}
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Shutting down thread pool...");
			shutdownExecutorService();
		}));
	}

	private void shutdownExecutorService() {
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
		}
	}

	private void setupUI() {
		// 1. Main window configuration
		setTitle("Enhanced Stock Dashboard (IEX Feed)");
		setSize(1200, 700);
		setMinimumSize(new Dimension(800, 600));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		registerShutdownHook();
		setLayout(new BorderLayout());

		// 2. Create menu bar
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenuItem saveItem = new JMenuItem("Save Results to CSV");
		saveItem.addActionListener(e -> saveTableToCSV());
		fileMenu.add(saveItem);

		// Add new configuration items
		JMenuItem saveConfigItem = new JMenuItem("Save Dashboard and Settings Conf");
		saveConfigItem.addActionListener(e -> saveConfigurationToFile());
		fileMenu.add(saveConfigItem);

		JMenuItem loadConfigItem = new JMenuItem("Load Dashboard and Settings Conf");
		loadConfigItem.addActionListener(e -> loadConfigurationFromFile());
		fileMenu.add(loadConfigItem);

		menuBar.add(fileMenu);

		// NEW: Table Action Menu
		JMenu tableActionMenu = new JMenu("Table Action");

		// Search Symbols menu item
		JMenuItem searchSymbolsItem = new JMenuItem("Search Symbol(s)");
		searchSymbolsItem.addActionListener(e -> openSearchSymbolsDialog());
		tableActionMenu.add(searchSymbolsItem);

		// Add separator and existing toggle functionality for consistency
		tableActionMenu.addSeparator();

		JMenuItem toggleSelectedItem = new JMenuItem("Toggle Selected Rows Visibility");
		toggleSelectedItem.addActionListener(e -> toggleSelectedRowsVisibility());
		tableActionMenu.add(toggleSelectedItem);

		JMenuItem showAllItem = new JMenuItem("Show All Rows");
		showAllItem.addActionListener(e -> showAllRows());
		tableActionMenu.add(showAllItem);

		menuBar.add(tableActionMenu);

		JMenu zScoreMenu = new JMenu("Z-Score");
		JMenuItem zScoreManagementItem = new JMenuItem("Z-Score Management");

		zScoreManagementItem.addActionListener(e -> {
			List<String> visibleSymbolList = getVisibleSymbolList();

			List<PriceData> priceDataList = new ArrayList<>();

			for (String symbol : visibleSymbolList) {

				PriceData priceData = priceDataMap.get(symbol);

				priceDataList.add(priceData);
			}

			// Close existing window if open
			/*
			 * if (zScoreRankingGUI != null) { zScoreRankingGUI.dispose(); }
			 */

			zScoreRankingGUI = new ZScoreRankingGUI(priceDataList, customIndicatorCombinations);
			zScoreRankingGUI.setCustomIndicatorCombinations(customIndicatorCombinations);
			zScoreRankingGUI.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			zScoreRankingGUI.setVisible(true);

		});
		zScoreMenu.add(zScoreManagementItem);
		menuBar.add(zScoreMenu);

		// NEW: Indicators Menu
		JMenu indicatorsMenu = new JMenu("Indicators");

		// Indicator Management Submenu
		JMenuItem indicatorManagementItem = new JMenuItem("Indicator Management");
		indicatorManagementItem.addActionListener(e -> openIndicatorManagement());
		indicatorsMenu.add(indicatorManagementItem);

		menuBar.add(indicatorsMenu);

		// Add Graph Menu
		JMenu graphMenu = new JMenu("Graph");

		// Monte Carlo submenu
		JMenu monteCarloMenu = new JMenu("MonteCarlo");

		// Monte Carlo Graph menu item
		JMenuItem monteCarloGraphItem = new JMenuItem("Show Monte Carlo Graph");
		monteCarloGraphItem.addActionListener(e -> showMonteCarloGraph());
		monteCarloMenu.add(monteCarloGraphItem);

		// Monte Carlo Config menu item
		JMenuItem monteCarloConfigItem = new JMenuItem("MonteCarlo Config");
		monteCarloConfigItem.addActionListener(e -> openMonteCarloConfigDialog());
		monteCarloMenu.add(monteCarloConfigItem);

		graphMenu.add(monteCarloMenu);
		menuBar.add(graphMenu);

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
			}
		});
	}

	private void openMonteCarloConfigDialog() {
		// Get current selected columns from config
		List<String> currentSelection = new ArrayList<>();
		if (monteCarloConfig.containsKey("selectedColumns")) {
			currentSelection = (List<String>) monteCarloConfig.get("selectedColumns");
		}

		// Open configuration dialog
		MonteCarloConfig dialog = new MonteCarloConfig(this, columnsConfigData, currentSelection);
		dialog.setVisible(true);

		// Get updated selection
		List<String> selectedColumns = dialog.getSelectedColumns();

		// Update configuration
		monteCarloConfig.put("selectedColumns", selectedColumns);

		// Save to columnsConfigData
		columnsConfigData.put(MonteCarloConfigManager.MONTE_CARLO_CONFIG_KEY, monteCarloConfig);

		logToConsole("Monte Carlo configuration updated. Selected columns: " + selectedColumns.size());
	}

	/**
	 * Opens a dialog to search for symbols and filters the table
	 */
	private void openSearchSymbolsDialog() {
		JDialog searchDialog = new JDialog(this, "Search Symbols", true);
		searchDialog.setLayout(new BorderLayout());
		searchDialog.setSize(400, 200);
		searchDialog.setLocationRelativeTo(this);

		// Main panel
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		// Instruction label
		JLabel instructionLabel = new JLabel("Enter symbols (comma-separated):");
		mainPanel.add(instructionLabel, BorderLayout.NORTH);

		// Text area for symbol input
		JTextArea symbolsTextArea = new JTextArea(4, 30);
		symbolsTextArea.setLineWrap(true);
		symbolsTextArea.setWrapStyleWord(true);

		// Add some common examples as placeholder text
		symbolsTextArea.setText("AAPL,TSLA,NVDA,MSFT,GOOGL");
		symbolsTextArea.selectAll(); // Select all text for easy replacement

		JScrollPane scrollPane = new JScrollPane(symbolsTextArea);
		mainPanel.add(scrollPane, BorderLayout.CENTER);

		// Button panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton searchButton = new JButton("Search");
		searchButton.addActionListener(e -> {
			String input = symbolsTextArea.getText().trim();
			if (!input.isEmpty()) {
				filterTableBySymbols(input);
				searchDialog.dispose();
			} else {
				JOptionPane.showMessageDialog(searchDialog, "Please enter at least one symbol", "Input Required",
						JOptionPane.WARNING_MESSAGE);
			}
		});

		JButton clearButton = new JButton("Clear Search");
		clearButton.addActionListener(e -> {
			clearSymbolFilter();
			searchDialog.dispose();
		});

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> searchDialog.dispose());

		buttonPanel.add(searchButton);
		buttonPanel.add(clearButton);
		buttonPanel.add(cancelButton);

		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		// Add keyboard shortcut (Enter to search, Escape to cancel)
		symbolsTextArea.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "search");
		symbolsTextArea.getActionMap().put("search", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				searchButton.doClick();
			}
		});

		searchDialog.add(mainPanel);
		searchDialog.setVisible(true);
	}

	/**
	 * Filters the table to show only the specified symbols
	 * 
	 * @param symbolInput Comma-separated list of symbols
	 */
	private void filterTableBySymbols(String symbolInput) {
		if (tableModel == null) {
			logToConsole("Table model not initialized");
			return;
		}

		try {
			// Parse the input symbols
			Set<String> targetSymbols = parseSymbolInput(symbolInput);

			if (targetSymbols.isEmpty()) {
				JOptionPane.showMessageDialog(this, "No valid symbols found in input", "Invalid Input",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			logToConsole("Searching for symbols: " + String.join(", ", targetSymbols));

			// Get all symbols in the table
			Set<String> allSymbols = getAllTableSymbols();

			// Find symbols to hide (all symbols except the target ones)
			Set<String> symbolsToHide = allSymbols.stream().filter(symbol -> !targetSymbols.contains(symbol))
					.collect(Collectors.toSet());

			// Apply the filter
			SwingUtilities.invokeLater(() -> {
				tableModel.toggleSymbolsVisibility(symbolsToHide);
				tableModel.fireTableDataChanged();
				dashboardTable.repaint();

				int visibleCount = allSymbols.size() - symbolsToHide.size();
				logToConsole("Filter applied. Showing " + visibleCount + " of " + allSymbols.size() + " symbols");

				// Update status
				statusLabel.setText("Status: Showing " + visibleCount + " symbols matching search");
			});

		} catch (Exception e) {
			logToConsole("Error filtering symbols: " + e.getMessage());
			JOptionPane.showMessageDialog(this, "Error filtering symbols: " + e.getMessage(), "Filter Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Parses the symbol input string into a set of normalized symbols
	 */
	private Set<String> parseSymbolInput(String input) {
		return Arrays.stream(input.split("\\s*,\\s*")).map(String::trim).filter(s -> !s.isEmpty())
				.map(String::toUpperCase).collect(Collectors.toSet());
	}

	/**
	 * Gets all symbols currently in the table
	 */
	private Set<String> getAllTableSymbols() {
		Set<String> allSymbols = new HashSet<>();
		Vector<?> rawData = tableModel.getDataVector();

		for (Object row : rawData) {
			@SuppressWarnings("unchecked")
			Vector<Object> rowVector = (Vector<Object>) row;
			String symbol = (String) rowVector.get(1); // Symbol is at index 1
			if (symbol != null && !symbol.trim().isEmpty()) {
				allSymbols.add(symbol.toUpperCase());
			}
		}

		return allSymbols;
	}

	/**
	 * Clears any symbol filter and shows all rows
	 */
	private void clearSymbolFilter() {
		SwingUtilities.invokeLater(() -> {
			tableModel.showAllSymbols();
			tableModel.fireTableDataChanged();
			dashboardTable.repaint();

			int totalCount = getAllTableSymbols().size();
			logToConsole("Search filter cleared. Showing all " + totalCount + " symbols");
			statusLabel.setText("Status: Showing all " + totalCount + " symbols");
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
			logFileNameField.setEnabled(enabled);
			if (!enabled) {
				stopLogScheduler();
			}
		});

		// Directory Field
		logDirectoryField = new JTextField(20);
		logDirectoryField.setEnabled(false);

		// File Name Field (changed from Path Name)
		logFileNameField = new JTextField(20);
		logFileNameField.setEnabled(false);

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
		loggerSettingsPanel.add(new JLabel("File Name:"), loggerGbc);
		loggerGbc.gridx = 1;
		loggerSettingsPanel.add(logFileNameField, loggerGbc);

		loggerGbc.gridy = 3;
		loggerGbc.gridx = 0;
		loggerGbc.gridwidth = 2;
		loggerSettingsPanel.add(enableLogReportsCheckbox, loggerGbc);

		loggerGbc.gridy = 4;
		loggerSettingsPanel.add(completeResultsCheckbox, loggerGbc);

		loggerGbc.gridy = 5;
		loggerSettingsPanel.add(bullishChangeCheckbox, loggerGbc);

		loggerGbc.gridy = 6;
		loggerSettingsPanel.add(bullishVolumeCheckbox, loggerGbc);

		// Apply button for logger settings
		JButton applyLoggerSettingsButton = new JButton("Apply Logger Settings");
		applyLoggerSettingsButton.addActionListener(e -> applyLoggerSettings());
		loggerGbc.gridy = 7;
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
	}

	private void openColumnVisibilityWindow() {
		JFrame columnVisibilityFrame = new JFrame("Manage Columns");
		columnVisibilityFrame.setSize(400, 600);

		// Main panel with scroll
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		// Section for basic columns
		JPanel basicPanel = new JPanel(new GridLayout(0, 1));
		basicPanel.setBorder(BorderFactory.createTitledBorder("Basic Columns"));

		// Store checkbox references
		Map<String, JCheckBox> columnCheckboxes = new HashMap<>();

		// Basic columns
		columnCheckboxes.put("Prev Vol",
				new JCheckBox("Show Previous Volume", dashboardTable.getColumnModel().getColumn(3).getWidth() > 0));
		columnCheckboxes.put("Current Vol",
				new JCheckBox("Show Current Volume", dashboardTable.getColumnModel().getColumn(4).getWidth() > 0));
		columnCheckboxes.put("% Change",
				new JCheckBox("Show % Change", dashboardTable.getColumnModel().getColumn(5).getWidth() > 0));

		basicPanel.add(columnCheckboxes.get("Prev Vol"));
		basicPanel.add(columnCheckboxes.get("Current Vol"));
		basicPanel.add(columnCheckboxes.get("% Change"));

		// PriceData columns section
		JPanel priceDataPanel = new JPanel(new GridLayout(0, 1));
		priceDataPanel.setBorder(BorderFactory.createTitledBorder("Price Data Attributes"));

		// Get all PriceData fields using reflection
		Field[] priceDataFields = PriceData.class.getDeclaredFields();
		for (Field field : priceDataFields) {
			if (!Modifier.isStatic(field.getModifiers())) {
				String fieldName = field.getName();
				boolean isVisible = isColumnVisible(fieldName);
				JCheckBox cb = new JCheckBox(fieldName, isVisible);
				columnCheckboxes.put(fieldName, cb);
				priceDataPanel.add(cb);
			}
		}

		// Z-Score Attributes Section using helper
		JPanel zScorePanel = createZScoreAttributesPanel();

		mainPanel.add(basicPanel);
		mainPanel.add(priceDataPanel);
		mainPanel.add(zScorePanel);

		// Button panel
		JPanel buttonPanel = new JPanel();
		JButton applyButton = new JButton("Apply");
		JButton cancelButton = new JButton("Cancel");

		applyButton.addActionListener(e -> {
			// Apply basic column visibility
			setColumnVisibility(dashboardTable, 3, columnCheckboxes.get("Prev Vol").isSelected());
			setColumnVisibility(dashboardTable, 4, columnCheckboxes.get("Current Vol").isSelected());
			setColumnVisibility(dashboardTable, 5, columnCheckboxes.get("% Change").isSelected());

			// Apply PriceData column visibility
			updatePriceDataColumns(columnCheckboxes);

			// Apply Z-Score column visibility
			updateZScoreColumns();

			columnVisibilityFrame.dispose();
		});

		cancelButton.addActionListener(e -> columnVisibilityFrame.dispose());

		buttonPanel.add(applyButton);
		buttonPanel.add(cancelButton);

		columnVisibilityFrame.add(new JScrollPane(mainPanel), BorderLayout.CENTER);
		columnVisibilityFrame.add(buttonPanel, BorderLayout.SOUTH);
		columnVisibilityFrame.setVisible(true);
	}

	// zscores
	private JPanel createZScoreAttributesPanel() {
		JPanel zScorePanel = new JPanel(new GridLayout(0, 1));
		zScorePanel.setBorder(BorderFactory.createTitledBorder("Z-Score Attributes"));

		// Get available Z-Score combinations using helper
		List<String> zScoreCombinations = zScoreColumnHelper.getZScoreCombinationNames();

		if (zScoreCombinations.isEmpty()) {
			zScorePanel.add(new JLabel("No Z-Score combinations available"));
		} else {
			for (String combination : zScoreCombinations) {
				// Create checkboxes for Rank and Z-Score for each combination
				String rankColumnName = "ZScore_" + combination + "_Rank";
				String zScoreColumnName = "ZScore_" + combination + "_ZScore";

				JCheckBox rankCheckbox = new JCheckBox(combination + " - Rank", isColumnVisible(rankColumnName));
				JCheckBox zScoreCheckbox = new JCheckBox(combination + " - Z-Score", isColumnVisible(zScoreColumnName));

				// Store in a class-level map for later retrieval
				zScoreCheckboxes.put(rankColumnName, rankCheckbox);
				zScoreCheckboxes.put(zScoreColumnName, zScoreCheckbox);

				zScorePanel.add(rankCheckbox);
				zScorePanel.add(zScoreCheckbox);
			}
		}

		return zScorePanel;
	}

	private void updateZScoreColumns() {
		SwingUtilities.invokeLater(() -> {
			try {
				// Get current data and column structure
				Vector<Vector<Object>> oldData = new Vector<>();
				Vector<String> oldColumnNames = new Vector<>();

				for (int i = 0; i < tableModel.getRowCount(); i++) {
					oldData.add(new Vector<>(tableModel.getDataVector().get(i)));
				}
				for (int i = 0; i < tableModel.getColumnCount(); i++) {
					oldColumnNames.add(tableModel.getColumnName(i));
				}

				// Create new column structure
				Vector<String> newColumnNames = new Vector<>();

				// Add existing columns that are not Z-Score columns using helper
				for (String oldColName : oldColumnNames) {
					if (!zScoreColumnHelper.isZScoreColumn(oldColName)) {
						newColumnNames.add(oldColName);
					}
				}

				// Add selected Z-Score columns
				for (Map.Entry<String, JCheckBox> entry : zScoreCheckboxes.entrySet()) {
					if (entry.getValue().isSelected()) {
						newColumnNames.add(entry.getKey());
					}
				}

				// Create new model with updated columns
				FilterableTableModel newModel = new FilterableTableModel(newColumnNames.toArray(new String[0]), 0,
						this);

				// Transfer all data
				for (Vector<Object> oldRow : oldData) {
					Vector<Object> newRow = new Vector<>();

					// Add existing data for non-Z-Score columns
					for (String newColName : newColumnNames) {
						Object value = "N/A"; // Default

						// Find matching column in old data
						int oldColIndex = oldColumnNames.indexOf(newColName);
						if (oldColIndex >= 0 && oldColIndex < oldRow.size()) {
							value = oldRow.get(oldColIndex);
						}
						newRow.add(value);
					}
					newModel.addRow(newRow);
				}

				// Update the table
				tableModel = newModel;
				dashboardTable.setModel(tableModel);

				updateTableColumnWidths();

				// Refresh data to compute Z-Score values
				// refreshData();

				dashboardTable.repaint();

			} catch (Exception e) {
				logToConsole("Error updating Z-Score columns: " + e.getMessage());
				e.printStackTrace();
			}
		});
	}

	public ZScoreAlertConfigDialog getZScoreAlertConfigDialog() {
		// You'll need to store a reference to the dialog when you create it
		return zScoreAlertConfigDialog;
	}

	private void loadLoggerSettings() {
		try {
			Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_13.class);
			enableSaveLogCheckbox.setSelected(prefs.getBoolean("enableSaveLog", false));
			logDirectoryField.setText(prefs.get("logDirectory", System.getProperty("user.home")));
			logFileNameField.setText(prefs.get("logFileName", "stock_data")); // Changed from logPathName
			enableLogReportsCheckbox.setSelected(prefs.getBoolean("enableLogReports", false));
			completeResultsCheckbox.setSelected(prefs.getBoolean("completeResults", false));
			bullishChangeCheckbox.setSelected(prefs.getBoolean("bullishChange", false));
			bullishVolumeCheckbox.setSelected(prefs.getBoolean("bullishVolume", false));

			// Enable/disable fields based on checkbox states
			boolean logEnabled = enableSaveLogCheckbox.isSelected();
			logDirectoryField.setEnabled(logEnabled);
			logFileNameField.setEnabled(logEnabled);

			boolean reportsEnabled = enableLogReportsCheckbox.isSelected();
			completeResultsCheckbox.setEnabled(reportsEnabled);
			bullishChangeCheckbox.setEnabled(reportsEnabled);
			bullishVolumeCheckbox.setEnabled(reportsEnabled);

			logToConsole("Loaded logger settings");
		} catch (Exception e) {
			logToConsole("Error loading logger settings: " + e.getMessage());
		}
	}

	private void saveLoggerSettings() {
		try {
			Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_13.class);
			prefs.putBoolean("enableSaveLog", enableSaveLogCheckbox.isSelected());
			prefs.put("logDirectory", logDirectoryField.getText());
			prefs.put("logFileName", logFileNameField.getText()); // Changed from logPathName
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
		int interval = (Integer) refreshIntervalComboBox.getSelectedItem(); // Align with refresh interval
		logScheduler.scheduleAtFixedRate(() -> {
			SwingUtilities.invokeLater(() -> {
				if (enableSaveLogCheckbox.isSelected() && consoleLogBuffer.length() > 0) {
					saveConsoleLog();
				}
				if (enableLogReportsCheckbox.isSelected()) {
					saveReportFiles();
				}
			});
		}, 0, interval, TimeUnit.SECONDS);
		logToConsole("Log scheduler started with " + interval + "-second interval");
	}

	private void saveConfigurationToFile() {
		try {
			// Prepare configuration data using your detailed structure
			Map<String, Object> configData = prepareDetailedConfigurationData();

			// Use configuration manager to save
			boolean success = dashboardConfigManager.saveConfigurationToFile(configData);

			if (success) {
				JOptionPane.showMessageDialog(this, "Configuration saved successfully!", "Success",
						JOptionPane.INFORMATION_MESSAGE);
			}

		} catch (Exception e) {
			logToConsole("❌ Error in save configuration process: " + e.getMessage());
			JOptionPane.showMessageDialog(this, "Error saving configuration: " + e.getMessage(), "Save Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void loadConfigurationFromFile() {
		try {
			initializeZScoreCheckboxes();

			// Use configuration manager to load
			Map<String, Object> configData = dashboardConfigManager.loadConfigurationFromFile();

			if (!configData.isEmpty()) {
				// Apply loaded configuration using your detailed method
				applyDetailedConfigurationData(configData);

				JOptionPane.showMessageDialog(this, "Configuration loaded successfully!", "Success",
						JOptionPane.INFORMATION_MESSAGE);
			}

		} catch (Exception e) {
			logToConsole("❌ Error in load configuration process: " + e.getMessage());
			JOptionPane.showMessageDialog(this, "Error loading configuration: " + e.getMessage(), "Load Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private Map<String, Object> prepareDetailedConfigurationData() {
		Map<String, Object> config = new HashMap<>();

		try {
			logToConsole("💾 Preparing detailed configuration data...");

			// 1. Save basic UI state
			saveBasicUIState(config);

			// 2. Save volume filters
			saveVolumeFilters(config);

			// 3. Save live update settings
			saveLiveUpdateSettings(config);

			// 4. Save Volume Alert Configuration
			saveVolumeAlertConfig(config);

			// 5. Save live update time restrictions
			saveLiveUpdateTimeRestrictions(config);

			// 6. Save indicator configurations (CRITICAL - this was likely missing)
			saveIndicatorConfigurations(config);

			// 7. Save uptrend filter setting
			config.put("uptrendFilterEnabled", uptrendFilterEnabled);

			// 8. Save watchlist settings
			saveWatchlistSettings(config);

			// 9. Save logger settings
			saveLoggerSettings(config);

			// 10. Save column visibility
			saveColumnVisibility(config);

			// 11. Save Z-Score column visibility
			saveZScoreColumnVisibility(config);

			// 12. Save checkbox states for specific PriceData columns
			savePriceDataColumnStates(config);

			// 13. Save Indicator Management Configuration
			saveIndicatorManagementConfig(config);

			// 14. Save customIndicatorCombinations
			saveCustomIndicatorCombinations(config);

			// 15. Save Z-Score Alert Configuration
			saveZScoreAlertConfig(config);

			// Save Monte Carlo configuration
			config.put(MonteCarloConfigManager.MONTE_CARLO_CONFIG_KEY, monteCarloConfig);

			logToConsole("✅ Prepared detailed configuration with " + config.size() + " items");

		} catch (Exception e) {
			logToConsole("❌ Error preparing detailed configuration data: " + e.getMessage());
			e.printStackTrace();
		}

		return config;
	}

	// Add these helper methods to ensure all data is saved:
	private void saveBasicUIState(Map<String, Object> config) {
		config.put("currentTimeRadio", currentTimeRadio != null && currentTimeRadio.isSelected());
		config.put("customRangeRadio", customRangeRadio != null && customRangeRadio.isSelected());

		if (startDatePicker != null && startDatePicker.getDate() != null) {
			config.put("startDate", startDatePicker.getDate().getTime());
		}
		if (endDatePicker != null && endDatePicker.getDate() != null) {
			config.put("endDate", endDatePicker.getDate().getTime());
		}
	}

	private void saveVolumeFilters(Map<String, Object> config) {
		config.put("prevVolMin", prevVolMinField != null ? prevVolMinField.getText() : "");
		config.put("prevVolMax", prevVolMaxField != null ? prevVolMaxField.getText() : "");
		config.put("currentVolMin", currentVolMinField != null ? currentVolMinField.getText() : "");
		config.put("currentVolMax", currentVolMaxField != null ? currentVolMaxField.getText() : "");
	}

	private void saveLiveUpdateSettings(Map<String, Object> config) {
		config.put("checkBullish", checkBullishCheckbox != null && checkBullishCheckbox.isSelected());
		config.put("alarmOn", alarmOnCheckbox != null && alarmOnCheckbox.isSelected());
		config.put("refreshInterval", refreshIntervalComboBox != null ? refreshIntervalComboBox.getSelectedItem() : 60);
		config.put("symbolExceptions", symbolExceptionsField != null ? symbolExceptionsField.getText() : "");
	}

	private void saveVolumeAlertConfig(Map<String, Object> config) {
		Map<String, Object> volumeAlertConfigMap = new HashMap<>();
		volumeAlertConfigMap.put("enabled", volumeAlertConfig.isEnabled());
		volumeAlertConfigMap.put("volume20MAEnabled", volumeAlertConfig.isVolume20MAEnabled());
		volumeAlertConfigMap.put("onlyBullishSymbols", volumeAlertConfig.isOnlyBullishSymbols());

		Map<String, Object> timeframeConfigsMap = new HashMap<>();
		for (Map.Entry<String, VolumeAlertTimeframeConfig> entry : volumeAlertConfig.getTimeframeConfigs().entrySet()) {
			Map<String, Object> timeframeConfigMap = new HashMap<>();
			timeframeConfigMap.put("enabled", entry.getValue().isEnabled());
			timeframeConfigMap.put("percentage", entry.getValue().getPercentage());
			timeframeConfigsMap.put(entry.getKey(), timeframeConfigMap);
		}
		volumeAlertConfigMap.put("timeframeConfigs", timeframeConfigsMap);
		config.put("volumeAlertConfig", volumeAlertConfigMap);
	}

	private void saveLiveUpdateTimeRestrictions(Map<String, Object> config) {
		if (startTimeSpinner != null && endTimeSpinner != null) {
			try {
				Date startTime = ((SpinnerDateModel) startTimeSpinner.getModel()).getDate();
				Date endTime = ((SpinnerDateModel) endTimeSpinner.getModel()).getDate();
				config.put("liveUpdateStartTime", startTime != null ? startTime.getTime() : 0);
				config.put("liveUpdateEndTime", endTime != null ? endTime.getTime() : 0);
			} catch (Exception e) {
				logToConsole("Warning: Could not save live update time restrictions: " + e.getMessage());
			}
		}
	}

	// CRITICAL FIX: This method was likely missing or incomplete
	private void saveIndicatorConfigurations(Map<String, Object> config) {
		Map<String, Object> indicatorConfigMap = new HashMap<>();

		for (String tf : allTimeframes) {
			Map<String, Object> tfConfigMap = new HashMap<>();
			Set<String> indicators = timeframeIndicators.get(tf);

			// Save enabled state
			boolean enabled = indicators != null && !indicators.isEmpty();
			tfConfigMap.put("enabled", enabled);

			// Save indicators list
			List<String> indicatorList = new ArrayList<>();
			if (indicators != null) {
				indicatorList.addAll(indicators);
			}
			tfConfigMap.put("indicators", indicatorList);

			// Save index ranges
			Map<String, Object> indexRangeMap = new HashMap<>();
			if (timeframeIndexRanges.containsKey(tf)) {
				Map<String, Integer> ranges = timeframeIndexRanges.get(tf);
				indexRangeMap.put("startIndex", ranges.getOrDefault("startIndex", 0));
				indexRangeMap.put("endIndex", ranges.getOrDefault("endIndex", 0));
			} else {
				indexRangeMap.put("startIndex", 0);
				indexRangeMap.put("endIndex", 0);
			}
			tfConfigMap.put("indexRange", indexRangeMap);

			// Save index counter
			tfConfigMap.put("indexCounter", timeframeIndexCounters.getOrDefault(tf, 5));

			// Save resistance settings
			Map<String, Object> resistanceSettingsMap = new HashMap<>();
			if (indicators != null && indicators.contains("HighestCloseOpen")) {
				JComboBox<String> timeRangeCombo = indicatorTimeRangeCombos.get(tf);
				JComboBox<String> sessionCombo = indicatorSessionCombos.get(tf);
				JTextField indexCounterField = indicatorIndexCounterFields.get(tf);

				if (timeRangeCombo != null) {
					resistanceSettingsMap.put("timeRange", timeRangeCombo.getSelectedItem());
				}
				if (sessionCombo != null) {
					resistanceSettingsMap.put("session", sessionCombo.getSelectedItem());
				}
				if (indexCounterField != null) {
					resistanceSettingsMap.put("indexCounter", indexCounterField.getText());
				}
			}
			tfConfigMap.put("resistanceSettings", resistanceSettingsMap);

			indicatorConfigMap.put(tf, tfConfigMap);
		}

		config.put("indicatorConfig", indicatorConfigMap);
		logToConsole("💾 Saved indicator configurations for " + allTimeframes.length + " timeframes");
	}

	private void saveWatchlistSettings(Map<String, Object> config) {
		config.put("currentWatchlist", currentWatchlist != null ? currentWatchlist : "Default");
		config.put("currentDataSource", currentDataSource != null ? currentDataSource : "Alpaca");

		// Save watchlists with WatchlistData format
		Map<String, Object> watchlistsConfig = new HashMap<>();
		for (Map.Entry<String, WatchlistData> entry : watchlists.entrySet()) {
			Map<String, Object> watchlistData = new HashMap<>();
			watchlistData.put("symbols", new ArrayList<>(entry.getValue().getSymbols()));
			watchlistData.put("primarySymbol", entry.getValue().getPrimarySymbol());
			watchlistsConfig.put(entry.getKey(), watchlistData);
		}
		config.put("watchlists", watchlistsConfig);
	}

	private void saveLoggerSettings(Map<String, Object> config) {
		config.put("enableSaveLog", enableSaveLogCheckbox != null && enableSaveLogCheckbox.isSelected());
		config.put("logDirectory", logDirectoryField != null ? logDirectoryField.getText() : "");
		config.put("logFileName", logFileNameField != null ? logFileNameField.getText() : "");
		config.put("enableLogReports", enableLogReportsCheckbox != null && enableLogReportsCheckbox.isSelected());
		config.put("completeResults", completeResultsCheckbox != null && completeResultsCheckbox.isSelected());
		config.put("bullishChange", bullishChangeCheckbox != null && bullishChangeCheckbox.isSelected());
		config.put("bullishVolume", bullishVolumeCheckbox != null && bullishVolumeCheckbox.isSelected());
	}

	private void saveColumnVisibility(Map<String, Object> config) {
		List<Map<String, Object>> columnVisibilityList = new ArrayList<>();
		if (tableModel != null) {
			for (int i = 0; i < tableModel.getColumnCount(); i++) {
				Map<String, Object> columnEntry = new HashMap<>();
				String colName = tableModel.getColumnName(i);
				boolean visible = dashboardTable.getColumnModel().getColumn(i).getWidth() > 0;
				columnEntry.put("name", colName);
				columnEntry.put("visible", visible);
				columnVisibilityList.add(columnEntry);
			}
		}
		config.put("columnVisibility", columnVisibilityList);
	}

	private void saveZScoreColumnVisibility(Map<String, Object> config) {
		Map<String, Boolean> zScoreColumnVisibilityMap = new HashMap<>();
		for (Map.Entry<String, JCheckBox> entry : zScoreCheckboxes.entrySet()) {
			zScoreColumnVisibilityMap.put(entry.getKey(), entry.getValue().isSelected());
		}
		config.put("zScoreColumnVisibility", zScoreColumnVisibilityMap);
	}

	private void savePriceDataColumnStates(Map<String, Object> config) {
		config.put("showAveVolume", showAveVolumeCheckBox != null && showAveVolumeCheckBox.isSelected());
		config.put("showPrevLastDayPrice",
				showPrevLastDayPriceCheckBox != null && showPrevLastDayPriceCheckBox.isSelected());
	}

	private void saveIndicatorManagementConfig(Map<String, Object> config) {
		if (indicatorsManagementApp != null) {
			try {
				JSONObject indicatorManagementConfig = indicatorsManagementApp.exportConfiguration();
				config.put("indicatorManagementConfig", indicatorManagementConfig.toString());

				// Save globalCustomIndicators
				Set<CustomIndicator> globalCustomIndicators = indicatorsManagementApp.getGlobalCustomIndicators();
				List<Map<String, Object>> customIndicatorsList = new ArrayList<>();

				for (CustomIndicator customIndicator : globalCustomIndicators) {
					Map<String, Object> indicatorMap = new HashMap<>();
					indicatorMap.put("name", customIndicator.getName());
					indicatorMap.put("type", customIndicator.getType());
					indicatorMap.put("displayName", customIndicator.getDisplayName());

					if (customIndicator.getParameters() != null) {
						indicatorMap.put("parameters", new HashMap<>(customIndicator.getParameters()));
					}
					customIndicatorsList.add(indicatorMap);
				}
				config.put("globalCustomIndicators", customIndicatorsList);

				logToConsole("✅ Saved indicator management configuration with " + globalCustomIndicators.size()
						+ " custom indicators");

			} catch (Exception e) {
				logToConsole("❌ Error exporting indicator management config: " + e.getMessage());
				// Create fallback
				Map<String, Object> fallbackConfig = new HashMap<>();
				fallbackConfig.put("customIndicators", new ArrayList<>());
				fallbackConfig.put("strategies", new ArrayList<>());
				config.put("indicatorManagementConfig", fallbackConfig);
				config.put("globalCustomIndicators", new ArrayList<>());
			}
		} else {
			logToConsole("⚠️ IndicatorsManagementApp not available for saving");
			config.put("globalCustomIndicators", new ArrayList<>());
		}
	}

	private void saveCustomIndicatorCombinations(Map<String, Object> config) {
		Map<String, Object> customCombinationsConfig = new HashMap<>();
		for (Map.Entry<String, Set<String>> entry : customIndicatorCombinations.entrySet()) {
			customCombinationsConfig.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		config.put("customIndicatorCombinations", customCombinationsConfig);
		logToConsole("💾 Saved custom indicator combinations: " + customIndicatorCombinations.size() + " timeframes");
	}

	private void saveZScoreAlertConfig(Map<String, Object> config) {
		Map<String, Object> zScoreAlertConfigMap = new HashMap<>();
		zScoreAlertConfigMap.put("enabled", zScoreAlertManager.isEnabled());

		Map<String, Object> alertConfigsMap = new HashMap<>();
		Map<String, ZScoreAlertConfig> alertConfigs = zScoreAlertManager.getAlertConfigs();
		for (Map.Entry<String, ZScoreAlertConfig> entry : alertConfigs.entrySet()) {
			Map<String, Object> alertConfigMap = new HashMap<>();
			ZScoreAlertConfig alertConfig = entry.getValue();
			alertConfigMap.put("enabled", alertConfig.isEnabled());
			alertConfigMap.put("monitorRange", alertConfig.getMonitorRange());
			alertConfigMap.put("zScoreColumn", alertConfig.getZScoreColumn());
			alertConfigMap.put("strategy", alertConfig.getStrategy() != null ? alertConfig.getStrategy() : "");
			alertConfigMap.put("monteCarloEnabled", alertConfig.isMonteCarloEnabled());
			alertConfigMap.put("alarmOn", alertConfig.isAlarmOn());
			alertConfigMap.put("configId", alertConfig.getConfigId());
			alertConfigMap.put("previousTopSymbols",
					alertConfig.getPreviousTopSymbols() != null ? alertConfig.getPreviousTopSymbols() : "");
			alertConfigsMap.put(entry.getKey(), alertConfigMap);
		}
		zScoreAlertConfigMap.put("alertConfigs", alertConfigsMap);
		config.put("zScoreAlertConfig", zScoreAlertConfigMap);
	}

	@SuppressWarnings("unchecked")
	private void applyDetailedConfigurationData(Map<String, Object> config) {
		try {
			logToConsole("🔧 Applying detailed configuration data...");

			// Initialize collections to avoid NPE
			initializeCollections();

			// 1. Load basic UI state with null checks
			loadBasicUIState(config);

			// 2. Load volume filters
			loadVolumeFilters(config);

			// 3. Load live update settings
			loadLiveUpdateSettings(config);

			// 4. Load Volume Alert Configuration
			loadVolumeAlertConfig(config);

			// 5. Load live update time restrictions
			loadLiveUpdateTimeRestrictions(config);

			// 6. Load indicator configurations (CRITICAL FIX)
			loadIndicatorConfigurations(config);

			// 7. Load uptrend filter setting
			loadUptrendFilterSetting(config);

			// 8. Load watchlist settings
			loadWatchlistSettings(config);

			// 9. Load logger settings
			loadLoggerSettings(config);

			// 10. Load column visibility
			loadColumnVisibility(config);

			// 11. Load Z-Score column visibility
			loadZScoreColumnVisibility(config);

			// 12. Load Indicator Management Configuration
			loadIndicatorManagementConfig(config);

			// 13. Load customIndicatorCombinations
			loadCustomIndicatorCombinations(config);

			// 14. Load Z-Score Alert Configuration
			loadZScoreAlertConfig(config);

			// 15. Load checkbox states for specific PriceData columns
			loadPriceDataColumnStates(config);

			loadMonteCarloConfig(config);

			// Refresh the table to apply changes
			SwingUtilities.invokeLater(this::refreshData);
			logToConsole("🎯 Detailed configuration applied successfully");

		} catch (Exception e) {
			logToConsole("❌ Error applying detailed configuration data: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void loadMonteCarloConfig(Map<String, Object> config) {
		// Load Monte Carlo configuration
		if (config.containsKey(MonteCarloConfigManager.MONTE_CARLO_CONFIG_KEY)) {
			try {
				monteCarloConfig = (Map<String, Object>) config.get(MonteCarloConfigManager.MONTE_CARLO_CONFIG_KEY);
				monteCarloConfig = MonteCarloConfigManager.mergeWithDefaults(monteCarloConfig);
				logToConsole("✅ Loaded Monte Carlo configuration");
			} catch (Exception e) {
				logToConsole("❌ Error loading Monte Carlo configuration: " + e.getMessage());
				monteCarloConfig = MonteCarloConfigManager.createDefaultConfig();
			}
		}
	}

	// Helper methods for applyDetailedConfigurationData
	private void initializeCollections() {
		// Ensure all collections are initialized
		if (timeframeIndicators == null) {
			timeframeIndicators = new LinkedHashMap<>();
		}
		if (customIndicatorCombinations == null) {
			customIndicatorCombinations = new HashMap<>();
		}

		// Initialize timeframeIndicators for all timeframes
		for (String tf : allTimeframes) {
			timeframeIndicators.putIfAbsent(tf, new HashSet<>());
		}
	}

	private void loadBasicUIState(Map<String, Object> config) {
		if (config.containsKey("currentTimeRadio") && currentTimeRadio != null) {
			currentTimeRadio.setSelected((Boolean) config.get("currentTimeRadio"));
		}
		if (config.containsKey("customRangeRadio") && customRangeRadio != null) {
			customRangeRadio.setSelected((Boolean) config.get("customRangeRadio"));
		}
		if (config.containsKey("startDate") && startDatePicker != null) {
			startDatePicker.setDate(new Date((Long) config.get("startDate")));
		}
		if (config.containsKey("endDate") && endDatePicker != null) {
			endDatePicker.setDate(new Date((Long) config.get("endDate")));
		}
	}

	private void loadVolumeFilters(Map<String, Object> config) {
		if (config.containsKey("prevVolMin") && prevVolMinField != null) {
			prevVolMinField.setText((String) config.get("prevVolMin"));
		}
		if (config.containsKey("prevVolMax") && prevVolMaxField != null) {
			prevVolMaxField.setText((String) config.get("prevVolMax"));
		}
		if (config.containsKey("currentVolMin") && currentVolMinField != null) {
			currentVolMinField.setText((String) config.get("currentVolMin"));
		}
		if (config.containsKey("currentVolMax") && currentVolMaxField != null) {
			currentVolMaxField.setText((String) config.get("currentVolMax"));
		}
	}

	private void loadLiveUpdateSettings(Map<String, Object> config) {
		if (config.containsKey("checkBullish") && checkBullishCheckbox != null) {
			checkBullishCheckbox.setSelected((Boolean) config.get("checkBullish"));
		}
		if (config.containsKey("alarmOn") && alarmOnCheckbox != null) {
			alarmOnCheckbox.setSelected((Boolean) config.get("alarmOn"));
		}
		if (config.containsKey("refreshInterval") && refreshIntervalComboBox != null) {
			refreshIntervalComboBox.setSelectedItem(config.get("refreshInterval"));
		}
		if (config.containsKey("symbolExceptions") && symbolExceptionsField != null) {
			symbolExceptionsField.setText((String) config.get("symbolExceptions"));
		}
	}

	private void loadVolumeAlertConfig(Map<String, Object> config) {
		if (config.containsKey("volumeAlertConfig")) {
			try {
				Map<String, Object> volumeAlertConfigMap = (Map<String, Object>) config.get("volumeAlertConfig");

				volumeAlertConfig.setEnabled((Boolean) volumeAlertConfigMap.getOrDefault("enabled", false));
				volumeAlertConfig
						.setVolume20MAEnabled((Boolean) volumeAlertConfigMap.getOrDefault("volume20MAEnabled", false));
				volumeAlertConfig
						.setOnlyBullishSymbols((Boolean) volumeAlertConfigMap.getOrDefault("onlyBullishSymbols", true));

				if (volumeAlertConfigMap.containsKey("timeframeConfigs")) {
					Map<String, Object> timeframeConfigsMap = (Map<String, Object>) volumeAlertConfigMap
							.get("timeframeConfigs");

					for (String tf : allTimeframes) {
						if (timeframeConfigsMap.containsKey(tf)) {
							Map<String, Object> timeframeConfigMap = (Map<String, Object>) timeframeConfigsMap.get(tf);
							boolean enabled = (Boolean) timeframeConfigMap.getOrDefault("enabled", false);
							double percentage = ((Number) timeframeConfigMap.getOrDefault("percentage", 0.0))
									.doubleValue();

							VolumeAlertTimeframeConfig timeframeConfig = new VolumeAlertTimeframeConfig(enabled,
									percentage);
							volumeAlertConfig.getTimeframeConfigs().put(tf, timeframeConfig);
						}
					}
				}
				logToConsole("✅ Loaded volume alert configuration");
			} catch (Exception e) {
				logToConsole("❌ Error loading volume alert config: " + e.getMessage());
			}
		}
	}

	private void loadLiveUpdateTimeRestrictions(Map<String, Object> config) {
		if (config.containsKey("liveUpdateStartTime") && config.containsKey("liveUpdateEndTime")
				&& startTimeSpinner != null && endTimeSpinner != null) {
			try {
				long startTime = (Long) config.get("liveUpdateStartTime");
				long endTime = (Long) config.get("liveUpdateEndTime");

				if (startTime > 0 && endTime > 0) {
					((SpinnerDateModel) startTimeSpinner.getModel()).setValue(new Date(startTime));
					((SpinnerDateModel) endTimeSpinner.getModel()).setValue(new Date(endTime));
				}
			} catch (Exception e) {
				logToConsole("❌ Error loading live update time restrictions: " + e.getMessage());
			}
		}
	}

	// CRITICAL FIX: This method needs to handle the indicator configuration
	// properly
	private void loadIndicatorConfigurations(Map<String, Object> config) {
		if (config.containsKey("indicatorConfig")) {
			try {
				Map<String, Object> indicatorConfigMap = (Map<String, Object>) config.get("indicatorConfig");
				logToConsole("📊 Loading indicator configurations...");

				for (String tf : allTimeframes) {
					Set<String> indicators = timeframeIndicators.get(tf);
					indicators.clear(); // Clear existing indicators

					if (indicatorConfigMap.containsKey(tf)) {
						Map<String, Object> tfConfigMap = (Map<String, Object>) indicatorConfigMap.get(tf);
						boolean enabled = (Boolean) tfConfigMap.getOrDefault("enabled", false);

						if (enabled && tfConfigMap.containsKey("indicators")) {
							List<String> indicatorList = (List<String>) tfConfigMap.get("indicators");
							indicators.addAll(indicatorList);
							logToConsole("   " + tf + ": " + indicatorList.size() + " indicators");
						}

						// Load index ranges
						if (tfConfigMap.containsKey("indexRange")) {
							Map<String, Object> indexRangeMap = (Map<String, Object>) tfConfigMap.get("indexRange");
							int startIndex = ((Number) indexRangeMap.getOrDefault("startIndex", 0)).intValue();
							int endIndex = ((Number) indexRangeMap.getOrDefault("endIndex", 0)).intValue();

							timeframeIndexRanges.put(tf, new HashMap<String, Integer>() {
								{
									put("startIndex", startIndex);
									put("endIndex", endIndex);
								}
							});
						}

						if (tfConfigMap.containsKey("indexCounter")) {
							int indexCounter = (int) tfConfigMap.getOrDefault("indexCounter", 5);
							timeframeIndexCounters.put(tf, indexCounter);
						}
						if (tfConfigMap.containsKey("resistanceSettings")) {
							JSONObject resistanceObject = (JSONObject) tfConfigMap.get("resistanceSettings");
							int intCounter = resistanceObject.getInt("indexCounter");
							String session = resistanceObject.getString("session");
							String timeRanged = resistanceObject.getString("timeRange");

							if (indicatorTimeRangeCombos != null) {
								// resistanceSettingsMap.put("timeRange", timeRangeCombo.getSelectedItem());

								indicatorTimeRangeCombos.get(tf).setSelectedItem(timeRanged);
							}
							if (indicatorSessionCombos != null) {
								// resistanceSettingsMap.put("session", sessionCombo.getSelectedItem());
								indicatorSessionCombos.get(tf).setSelectedItem(session);
							}
							if (indicatorIndexCounterFields != null) {
								if (indicatorIndexCounterFields.size() > 0) {
									indicatorIndexCounterFields.get(tf).setText(String.valueOf(intCounter));
								} else {
									JTextField indexCounterField = new JTextField(String.valueOf(intCounter), 3); // Default
																													// value
																													// 5

									indicatorIndexCounterFields.put(tf, indexCounterField);
								}
							}
						}
					}
				}
				logToConsole("✅ Loaded indicator configurations for " + timeframeIndicators.size() + " timeframes");
			} catch (Exception e) {
				logToConsole("❌ Error loading indicator configurations: " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			logToConsole("⚠️ No indicator configurations found in saved file");
		}
	}

	private void loadUptrendFilterSetting(Map<String, Object> config) {
		if (config.containsKey("uptrendFilterEnabled")) {
			uptrendFilterEnabled = (Boolean) config.get("uptrendFilterEnabled");
			if (uptrendFilterCheckbox != null) {
				uptrendFilterCheckbox.setSelected(uptrendFilterEnabled);
			}
		}
	}

	/**
	 * Convert JSONObject to Map (if not already present)
	 */
	@SuppressWarnings("unchecked")
	private Map<String, Object> convertJSONObjectToMap(JSONObject jsonObject) {
		if (jsonObject == null)
			return new HashMap<>();

		Map<String, Object> map = new HashMap<>();
		Iterator<String> keys = jsonObject.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Object value = jsonObject.get(key);

			if (value instanceof JSONObject) {
				map.put(key, convertJSONObjectToMap((JSONObject) value));
			} else if (value instanceof JSONArray) {
				map.put(key, convertJSONArrayToList((JSONArray) value));
			} else if (value == JSONObject.NULL) {
				map.put(key, null);
			} else {
				map.put(key, value);
			}
		}
		return map;
	}

	private void loadColumnVisibility(Map<String, Object> config) {
		if (config.containsKey("columnVisibility") && tableModel != null) {
			try {
				tableModel.setColumnIdentifiers(new Vector<>());
				tableModel.fireTableStructureChanged();

				Vector<Object> newColumnIdentifiers = new Vector<>();
				Map<String, Boolean> visibilityMap = new HashMap<>();

				Object columnVisibilityObj = config.get("columnVisibility");
				if (columnVisibilityObj instanceof List) {
					List<Map<String, Object>> columnVisibilityList = (List<Map<String, Object>>) columnVisibilityObj;
					logToConsole("Loading columnVisibility (list): " + columnVisibilityList);

					for (Map<String, Object> columnEntry : columnVisibilityList) {
						String colName = (String) columnEntry.get("name");
						boolean visible = (Boolean) columnEntry.get("visible");
						newColumnIdentifiers.add(colName);
						visibilityMap.put(colName, visible);
					}
				} else if (columnVisibilityObj instanceof Map) {
					Map<String, Boolean> columnVisibility = (Map<String, Boolean>) columnVisibilityObj;
					logToConsole("Loading columnVisibility (map, old format): " + columnVisibility);
					logToConsole("Warning: Old columnVisibility format detected. Column order may not be preserved.");

					for (String colName : columnVisibility.keySet()) {
						newColumnIdentifiers.add(colName);
						visibilityMap.put(colName, columnVisibility.get(colName));
					}
				}

				tableModel.setColumnIdentifiers(newColumnIdentifiers);
				logToConsole("Ordered columns from config: " + newColumnIdentifiers);

				for (Object colName : newColumnIdentifiers) {
					if (visibilityMap.getOrDefault(colName, false)) {
						for (int row = 0; row < tableModel.getRowCount(); row++) {
							tableModel.setValueAt(null, row, findColumnIndex((String) colName));
						}
					}
				}

				for (int i = 0; i < tableModel.getColumnCount(); i++) {
					String colName = tableModel.getColumnName(i);
					boolean isVisible = visibilityMap.getOrDefault(colName, true);
					setColumnVisibility(dashboardTable, i, isVisible);
				}

				tableModel.fireTableStructureChanged();
				logToConsole("Final table columns applied");

			} catch (Exception e) {
				logToConsole("❌ Error loading column visibility: " + e.getMessage());
			}
		}
	}

	private void loadCustomIndicatorCombinations(Map<String, Object> config) {
		if (config.containsKey("customIndicatorCombinations")) {
			try {
				Map<String, Object> loadedCombinations = (Map<String, Object>) config
						.get("customIndicatorCombinations");
				customIndicatorCombinations.clear();

				for (Map.Entry<String, Object> entry : loadedCombinations.entrySet()) {
					String timeframe = entry.getKey();
					List<String> indicatorNames = (List<String>) entry.getValue();
					customIndicatorCombinations.put(timeframe, new HashSet<>(indicatorNames));
				}

				logToConsole("✅ Loaded custom indicator combinations: " + customIndicatorCombinations.size()
						+ " timeframes");

				// Update table columns to include custom indicators
				updateTableColumnsForCustomIndicators();

			} catch (Exception e) {
				logToConsole("❌ Error loading custom indicator combinations: " + e.getMessage());
			}
		}
	}

	private void loadIndicatorManagementConfig(Map<String, Object> config) {
		if (config.containsKey("indicatorManagementConfig")) {
			try {
				Object indicatorConfigObj = config.get("indicatorManagementConfig");

				if (indicatorConfigObj instanceof String) {
					// JSON string format
					String jsonString = (String) indicatorConfigObj;
					JSONObject jsonObject = new JSONObject(jsonString);

					// Convert to map for the indicators management app
					Map<String, Object> indicatorConfigMap = convertJSONObjectToMap(jsonObject);
					indicatorManagementConfig.putAll(indicatorConfigMap);

				} else if (indicatorConfigObj instanceof Map) {
					// Map format
					Map<String, Object> indicatorConfigMap = (Map<String, Object>) indicatorConfigObj;
					indicatorManagementConfig.putAll(indicatorConfigMap);
				}

				// CRITICAL: Load globalCustomIndicators first
				if (config.containsKey("globalCustomIndicators")) {
					loadGlobalCustomIndicators(config);
				}

				// Initialize StrategyExecutionService with dashboard configuration
				if (strategyExecutionService == null) {
					strategyExecutionService = createStrategyExecutionService();
				}

				// Single call to handle everything
				checkAndInstantiateIndicatorsManagementApp();

				if (indicatorsManagementApp != null) {
					indicatorsManagementApp.importConfiguration(indicatorManagementConfig);
					indicatorsManagementApp.importWatchlists(watchlists);
					logToConsole("✅ Loaded indicator management configuration");
				} else {
					logToConsole("❌ IndicatorsManagementApp is null after initialization");
				}

			} catch (Exception e) {
				logToConsole("❌ Error importing indicator management config: " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			logToConsole("⚠️ No indicatorManagementConfig found in configuration");
		}
	}

	/**
	 * Load global custom indicators from configuration
	 */
	@SuppressWarnings("unchecked")
	private void loadGlobalCustomIndicators(Map<String, Object> config) {
		if (!config.containsKey("globalCustomIndicators")) {
			logToConsole("⚠️ No globalCustomIndicators found in configuration");
			return;
		}

		try {
			List<Map<String, Object>> customIndicatorsList = (List<Map<String, Object>>) config
					.get("globalCustomIndicators");
			logToConsole("📥 Loading " + customIndicatorsList.size() + " global custom indicators from configuration");

			Set<CustomIndicator> loadedCustomIndicators = new HashSet<>();

			for (Map<String, Object> indicatorMap : customIndicatorsList) {
				try {
					String name = (String) indicatorMap.get("name");
					String type = (String) indicatorMap.get("type");
					String displayName = (String) indicatorMap.get("displayName");

					logToConsole("🔧 Creating custom indicator: " + name + " (type: " + type + ")");

					// Create CustomIndicator based on type and parameters
					CustomIndicator customIndicator;
					if (indicatorMap.containsKey("parameters")) {
						Map<String, Object> parameters = (Map<String, Object>) indicatorMap.get("parameters");
						customIndicator = new CustomIndicator(name, type, parameters);
						logToConsole("   With parameters: " + parameters);
					} else {
						customIndicator = new CustomIndicator(name, type);
						logToConsole("   No parameters");
					}

					// Set display name if available
					if (displayName != null && !displayName.isEmpty()) {
						// Assuming CustomIndicator has a setDisplayName method
						// If not, you may need to modify the CustomIndicator class
						try {
							customIndicator.getClass().getMethod("setDisplayName", String.class).invoke(customIndicator,
									displayName);
						} catch (Exception e) {
							// Fallback if setDisplayName doesn't exist
							logToConsole("   Display name not set (method not available)");
						}
					}

					loadedCustomIndicators.add(customIndicator);
					logToConsole("✅ Loaded custom indicator: " + customIndicator.getName());

				} catch (Exception e) {
					logToConsole("❌ Error loading custom indicator from map: " + e.getMessage());
					e.printStackTrace();
				}
			}

			// Set the loaded custom indicators to the management app
			if (indicatorsManagementApp != null) {
				indicatorsManagementApp.setAllCustomIndicators(loadedCustomIndicators);
				logToConsole(
						"✅ Set " + loadedCustomIndicators.size() + " custom indicators to IndicatorsManagementApp");
			} else {
				logToConsole("⚠️ IndicatorsManagementApp is null, storing custom indicators for later");
				// Store for when IndicatorsManagementApp is created
				indicatorManagementConfig.put("globalCustomIndicators", customIndicatorsList);
			}

		} catch (Exception e) {
			logToConsole("❌ Error loading global custom indicators: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void loadLoggerSettings(Map<String, Object> config) {
		try {
			if (config.containsKey("enableSaveLog") && enableSaveLogCheckbox != null) {
				enableSaveLogCheckbox.setSelected((Boolean) config.get("enableSaveLog"));
			}
			if (config.containsKey("logDirectory") && logDirectoryField != null) {
				logDirectoryField.setText((String) config.get("logDirectory"));
			}
			if (config.containsKey("logFileName") && logFileNameField != null) {
				logFileNameField.setText((String) config.get("logFileName"));
			}
			if (config.containsKey("enableLogReports") && enableLogReportsCheckbox != null) {
				enableLogReportsCheckbox.setSelected((Boolean) config.get("enableLogReports"));
			}
			if (config.containsKey("completeResults") && completeResultsCheckbox != null) {
				completeResultsCheckbox.setSelected((Boolean) config.get("completeResults"));
			}
			if (config.containsKey("bullishChange") && bullishChangeCheckbox != null) {
				bullishChangeCheckbox.setSelected((Boolean) config.get("bullishChange"));
			}
			if (config.containsKey("bullishVolume") && bullishVolumeCheckbox != null) {
				bullishVolumeCheckbox.setSelected((Boolean) config.get("bullishVolume"));
			}

			// Update logger fields' enabled state
			boolean logEnabled = enableSaveLogCheckbox.isSelected();
			if (logDirectoryField != null)
				logDirectoryField.setEnabled(logEnabled);
			if (logFileNameField != null)
				logFileNameField.setEnabled(logEnabled);

			boolean reportsEnabled = enableLogReportsCheckbox.isSelected();
			if (completeResultsCheckbox != null)
				completeResultsCheckbox.setEnabled(reportsEnabled);
			if (bullishChangeCheckbox != null)
				bullishChangeCheckbox.setEnabled(reportsEnabled);
			if (bullishVolumeCheckbox != null)
				bullishVolumeCheckbox.setEnabled(reportsEnabled);

			logToConsole("✅ Loaded logger settings");

		} catch (Exception e) {
			logToConsole("❌ Error loading logger settings: " + e.getMessage());
		}
	}

	private void loadPriceDataColumnStates(Map<String, Object> config) {
		try {
			if (config.containsKey("showAveVolume") && showAveVolumeCheckBox != null) {
				showAveVolumeCheckBox.setSelected((Boolean) config.get("showAveVolume"));
			}
			if (config.containsKey("showPrevLastDayPrice") && showPrevLastDayPriceCheckBox != null) {
				showPrevLastDayPriceCheckBox.setSelected((Boolean) config.get("showPrevLastDayPrice"));
			}

			logToConsole("✅ Loaded PriceData column states");

		} catch (Exception e) {
			logToConsole("❌ Error loading PriceData column states: " + e.getMessage());
		}
	}

	private void loadWatchlistSettings(Map<String, Object> config) {
		try {
			if (config.containsKey("currentWatchlist") && watchlistComboBox != null) {
				String savedWatchlist = (String) config.get("currentWatchlist");
				if (watchlists.containsKey(savedWatchlist)) {
					watchlistComboBox.setSelectedItem(savedWatchlist);
					currentWatchlist = savedWatchlist;
				}
			}
			if (config.containsKey("currentDataSource") && dataSourceComboBox != null) {
				String savedDataSource = (String) config.get("currentDataSource");
				dataSourceComboBox.setSelectedItem(savedDataSource);
				currentDataSource = savedDataSource;
			}

			// Load watchlists with WatchlistData format
			if (config.containsKey("watchlists")) {
				Map<String, Object> watchlistsConfig = (Map<String, Object>) config.get("watchlists");
				watchlists.clear();

				for (Map.Entry<String, Object> entry : watchlistsConfig.entrySet()) {
					String watchlistName = entry.getKey();
					Object watchlistDataObj = entry.getValue();

					if (watchlistDataObj instanceof Map) {
						Map<String, Object> watchlistData = (Map<String, Object>) watchlistDataObj;

						// Get symbols
						Set<String> symbols = new HashSet<>();
						Object symbolsObj = watchlistData.get("symbols");
						if (symbolsObj instanceof List) {
							List<?> symbolsList = (List<?>) symbolsObj;
							for (Object symbolObj : symbolsList) {
								symbols.add(symbolObj.toString());
							}
						}

						// Get primary symbol
						String primarySymbol = "";
						Object primarySymbolObj = watchlistData.get("primarySymbol");
						if (primarySymbolObj != null) {
							primarySymbol = primarySymbolObj.toString();
						}

						watchlists.put(watchlistName, new WatchlistData(symbols, primarySymbol));
					}
				}

				/*
				 * if(config.containsKey("indicatorManagementConfig")) { Object
				 * indicatorManagementConfig = (Object) config.get("indicatorManagementConfig");
				 * 
				 * Map<String, Object> configIndicatorManagementConfig = new HashMap<String,
				 * Object>();
				 * 
				 * if (indicatorManagementConfig instanceof String) { // JSON string format
				 * String jsonString = (String) indicatorManagementConfig; JSONObject jsonObject
				 * = new JSONObject(jsonString);
				 * 
				 * // Convert to map for the indicators management app Map<String, Object>
				 * indicatorConfigMap = convertJSONObjectToMap(jsonObject);
				 * //indicatorManagementConfig.putAll(indicatorConfigMap);
				 * 
				 * } else if (indicatorManagementConfig instanceof Map) { // Map format
				 * Map<String, Object> indicatorConfigMap = (Map<String, Object>)
				 * indicatorManagementConfig;
				 * //indicatorManagementConfig.putAll(indicatorConfigMap); }
				 * 
				 * 
				 * }
				 */

				refreshWatchlistComboBox();
				logToConsole("✅ Loaded " + watchlists.size() + " watchlists with WatchlistData format");
			}

		} catch (Exception e) {
			logToConsole("❌ Error loading watchlist settings: " + e.getMessage());
		}
	}

	private void loadZScoreAlertConfig(Map<String, Object> config) {
		if (config.containsKey("zScoreAlertConfig")) {
			try {
				Map<String, Object> zScoreAlertConfigMap = (Map<String, Object>) config.get("zScoreAlertConfig");

				boolean zScoreAlertEnabled = (Boolean) zScoreAlertConfigMap.getOrDefault("enabled", false);
				zScoreAlertManager.setEnabled(zScoreAlertEnabled);

				if (zScoreAlertConfigMap.containsKey("alertConfigs")) {
					Map<String, Object> alertConfigsMap = (Map<String, Object>) zScoreAlertConfigMap
							.get("alertConfigs");
					Map<String, ZScoreAlertConfig> newAlertConfigs = new HashMap<>();

					for (Map.Entry<String, Object> entry : alertConfigsMap.entrySet()) {
						String columnName = entry.getKey();
						Map<String, Object> alertConfigMap = (Map<String, Object>) entry.getValue();

						boolean enabled = (Boolean) alertConfigMap.getOrDefault("enabled", false);
						int monitorRange = ((Number) alertConfigMap.getOrDefault("monitorRange", 5)).intValue();
						boolean monteCarloEnabled = (Boolean) alertConfigMap.getOrDefault("monteCarloEnabled", false);
						boolean alarmOn = (Boolean) alertConfigMap.getOrDefault("alarmOn", false);
						String previousTopSymbols = (String) alertConfigMap.getOrDefault("previousTopSymbols", "");

						ZScoreAlertConfig alertConfig = new ZScoreAlertConfig(enabled, monitorRange);
						alertConfig.setMonteCarloEnabled(monteCarloEnabled);
						alertConfig.setAlarmOn(alarmOn);
						alertConfig.setPreviousTopSymbols(previousTopSymbols);

						newAlertConfigs.put(columnName, alertConfig);
					}

					zScoreAlertManager.setAlertConfigs(newAlertConfigs);
				}

				logToConsole("✅ Loaded Z-Score alert configuration");

			} catch (Exception e) {
				logToConsole("❌ Error loading Z-Score alert configuration: " + e.getMessage());
			}
		}
	}

	private void loadZScoreColumnVisibility(Map<String, Object> config) {
		if (config.containsKey("zScoreColumnVisibility")) {
			try {
				Map<String, Boolean> zScoreVisibility = (Map<String, Boolean>) config.get("zScoreColumnVisibility");
				for (Map.Entry<String, Boolean> entry : zScoreVisibility.entrySet()) {
					if (zScoreCheckboxes.containsKey(entry.getKey())) {
						zScoreCheckboxes.get(entry.getKey()).setSelected(entry.getValue());
					}
				}
				// Apply the loaded Z-Score column settings
				updateZScoreColumns();

				logToConsole("✅ Loaded Z-Score column visibility for " + zScoreVisibility.size() + " columns");

			} catch (Exception e) {
				logToConsole("❌ Error loading Z-Score column visibility: " + e.getMessage());
			}
		}
	}

	// ... Add similar helper methods for the other load operations

	/**
	 * Apply column configuration when loading from file
	 */
	@SuppressWarnings("unchecked")
	private void applyColumnConfiguration(Map<String, Object> config) {
		if (!config.containsKey("columnConfiguration")) {
			logToConsole("⚠️ No column configuration found in saved file");
			return;
		}

		try {
			Map<String, Object> columnConfig = (Map<String, Object>) config.get("columnConfiguration");

			// 1. Restore basic column structure
			List<String> allColumnNames = (List<String>) columnConfig.get("allColumnNames");
			if (allColumnNames != null) {
				logToConsole("📋 Restoring " + allColumnNames.size() + " columns from configuration");

				// Create table model with restored columns
				FilterableTableModel newModel = new FilterableTableModel(allColumnNames.toArray(new String[0]), 0,
						this);

				// Transfer existing data if available
				if (tableModel != null && tableModel.getRowCount() > 0) {
					for (int i = 0; i < tableModel.getRowCount(); i++) {
						Vector<Object> newRow = new Vector<>();
						for (String colName : allColumnNames) {
							// Try to find matching data from old structure
							Object value = findMatchingData(colName, i);
							newRow.add(value != null ? value : "N/A");
						}
						newModel.addRow(newRow);
					}
				}

				tableModel = newModel;
				dashboardTable.setModel(tableModel);
				updateTableColumnWidths();
				updateTableRenderers();
			}

			// 2. Restore column mappings for data population
			if (columnConfig.containsKey("columnMappings")) {
				Map<String, Map<String, Object>> serializableMappings = (Map<String, Map<String, Object>>) columnConfig
						.get("columnMappings");

				Map<String, DashboardColumnCollector.ColumnMapping> columnMappings = new HashMap<>();
				for (Map.Entry<String, Map<String, Object>> entry : serializableMappings.entrySet()) {
					String columnName = entry.getKey();
					Map<String, Object> mappingData = entry.getValue();

					DashboardColumnCollector.ColumnType type = DashboardColumnCollector.ColumnType
							.valueOf((String) mappingData.get("type"));
					String timeframe = (String) mappingData.get("timeframe");
					String indicator = (String) mappingData.get("indicator");
					String zscoreStrategy = (String) mappingData.get("zscoreStrategy");
					String priceDataField = (String) mappingData.get("priceDataField");

					Class<?> fieldType;
					try {
						fieldType = Class.forName((String) mappingData.get("fieldType"));
					} catch (Exception e) {
						fieldType = String.class;
					}

					DashboardColumnCollector.ColumnMapping mapping = new DashboardColumnCollector.ColumnMapping(
							columnName, type, timeframe, indicator, zscoreStrategy, priceDataField, fieldType);
					columnMappings.put(columnName, mapping);
				}

				// Store mappings for later use
				columnsConfigData.put("currentColumnMappings", columnMappings);
				logToConsole("✅ Restored " + columnMappings.size() + " column mappings");
			}

			// 3. Log restored configuration
			logToConsole("🎯 Column configuration applied:");
			logToConsole("   - Total columns: " + allColumnNames.size());

			if (columnConfig.containsKey("timeframeIndicators")) {
				logToConsole("   - Timeframe indicators: "
						+ ((Map<?, ?>) columnConfig.get("timeframeIndicators")).size() + " timeframes");
			}

			if (columnConfig.containsKey("zScoreColumns")) {
				logToConsole("   - Z-Score columns: " + ((List<?>) columnConfig.get("zScoreColumns")).size());
			}

			if (columnConfig.containsKey("customIndicators")) {
				logToConsole("   - Custom indicators: " + ((Map<?, ?>) columnConfig.get("customIndicators")).size()
						+ " timeframes");
			}

		} catch (Exception e) {
			logToConsole("❌ Error applying column configuration: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to find matching data when restoring columns
	 */
	private Object findMatchingData(String columnName, int rowIndex) {
		if (tableModel == null)
			return null;

		try {
			// Try to find the column in the current table
			for (int col = 0; col < tableModel.getColumnCount(); col++) {
				if (tableModel.getColumnName(col).equals(columnName)) {
					return tableModel.getValueAt(rowIndex, col);
				}
			}
		} catch (Exception e) {
			// Ignore errors during data transfer
		}

		return null;
	}

	/**
	 * Prepare all configuration data for saving
	 */
	private Map<String, Object> prepareConfigurationData() {
		Map<String, Object> configData = new HashMap<>();

		try {
			// Time range settings
			configData.put("currentTimeRadio", currentTimeRadio != null && currentTimeRadio.isSelected());
			configData.put("customRangeRadio", customRangeRadio != null && customRangeRadio.isSelected());
			if (startDatePicker != null && startDatePicker.getDate() != null) {
				configData.put("startDate", startDatePicker.getDate());
			}
			if (endDatePicker != null && endDatePicker.getDate() != null) {
				configData.put("endDate", endDatePicker.getDate());
			}

			// Volume filters
			configData.put("prevVolMin", prevVolMinField != null ? prevVolMinField.getText() : "");
			configData.put("prevVolMax", prevVolMaxField != null ? prevVolMaxField.getText() : "");
			configData.put("currentVolMin", currentVolMinField != null ? currentVolMinField.getText() : "");
			configData.put("currentVolMax", currentVolMaxField != null ? currentVolMaxField.getText() : "");

			// Live update settings
			configData.put("checkBullish", checkBullishCheckbox != null && checkBullishCheckbox.isSelected());
			configData.put("alarmOn", alarmOnCheckbox != null && alarmOnCheckbox.isSelected());
			configData.put("refreshInterval",
					refreshIntervalComboBox != null ? refreshIntervalComboBox.getSelectedItem() : 60);
			configData.put("symbolExceptions", symbolExceptionsField != null ? symbolExceptionsField.getText() : "");

			// Watchlist settings
			configData.put("currentWatchlist", currentWatchlist != null ? currentWatchlist : "");
			configData.put("currentDataSource", currentDataSource != null ? currentDataSource : "");
			configData.put("watchlists", new HashMap<>(watchlists));

			// Indicator configurations
			configData.put("timeframeIndicators", new HashMap<>(timeframeIndicators));
			configData.put("uptrendFilterEnabled", uptrendFilterEnabled);

			// Logger settings
			configData.put("enableSaveLog", enableSaveLogCheckbox != null && enableSaveLogCheckbox.isSelected());
			configData.put("logDirectory", logDirectoryField != null ? logDirectoryField.getText() : "");
			configData.put("logFileName", logFileNameField != null ? logFileNameField.getText() : "");
			configData.put("enableLogReports",
					enableLogReportsCheckbox != null && enableLogReportsCheckbox.isSelected());
			configData.put("completeResults", completeResultsCheckbox != null && completeResultsCheckbox.isSelected());
			configData.put("bullishChange", bullishChangeCheckbox != null && bullishChangeCheckbox.isSelected());
			configData.put("bullishVolume", bullishVolumeCheckbox != null && bullishVolumeCheckbox.isSelected());

			// Indicator Management Configuration
			if (indicatorsManagementApp != null) {
				try {
					// Use the export method if available, otherwise use a default structure
					JSONObject indicatorManagementConfig = indicatorsManagementApp.exportConfiguration();
					configData.put("indicatorManagementConfig", indicatorManagementConfig);
					logToConsole("✅ Saved indicator management configuration");
				} catch (Exception e) {
					logToConsole("❌ Error exporting indicator management config: " + e.getMessage());
					// Create a basic structure as fallback
					Map<String, Object> fallbackConfig = new HashMap<>();
					fallbackConfig.put("customIndicators", new ArrayList<>());
					fallbackConfig.put("strategyConfigs", new ArrayList<>());
					configData.put("indicatorManagementConfig", fallbackConfig);
				}
			} else {
				logToConsole("⚠️ IndicatorsManagementApp not available for saving");
			}

			logToConsole("📊 Prepared configuration data with " + configData.size() + " items");

			logToConsole("📊 Prepared configuration data with " + configData.size() + " items");

		} catch (Exception e) {
			logToConsole("❌ Error preparing configuration data: " + e.getMessage());
		}

		return configData;
	}

	/**
	 * Auto-save configuration on application close
	 */
	private void autoSaveConfiguration() {
		try {
			Map<String, Object> configData = prepareConfigurationData();
			dashboardConfigManager.saveConfigurationToDefaultLocation(configData);
			dashboardConfigManager.backupConfiguration(configData);
		} catch (Exception e) {
			logToConsole("❌ Error during auto-save: " + e.getMessage());
		}
	}

	/**
	 * Convert Map to JSONObject
	 */
	private JSONObject convertMapToJSONObject(Map<String, Object> map) {
		if (map == null)
			return new JSONObject();

		JSONObject jsonObject = new JSONObject();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof Map) {
				jsonObject.put(entry.getKey(), convertMapToJSONObject((Map<String, Object>) value));
			} else if (value instanceof List) {
				JSONArray array = new JSONArray();
				for (Object item : (List<?>) value) {
					if (item instanceof Map) {
						array.put(convertMapToJSONObject((Map<String, Object>) item));
					} else {
						array.put(item);
					}
				}
				jsonObject.put(entry.getKey(), array);
			} else {
				jsonObject.put(entry.getKey(), value);
			}
		}
		return jsonObject;
	}

	/**
	 * Convert JSONArray to List
	 */
	private List<Object> convertJSONArrayToList(JSONArray jsonArray) {
		List<Object> list = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			Object value = jsonArray.get(i);
			if (value instanceof JSONObject) {
				list.add(convertJSONObjectToMap((JSONObject) value));
			} else if (value instanceof JSONArray) {
				list.add(convertJSONArrayToList((JSONArray) value));
			} else if (value == JSONObject.NULL) {
				list.add(null);
			} else {
				list.add(value);
			}
		}
		return list;
	}

	private void saveConsoleLog() {
		if (consoleLogBuffer.length() == 0) {
			return; // Skip if buffer is empty
		}

		File logDir = getWatchlistLogDirectory();
		if (logDir == null) {
			return; // Skip if directory creation failed
		}

		String fileName = logFileNameField.getText().trim();
		if (fileName.isEmpty()) {
			fileName = "stock_log";
		}
		if (!fileName.toLowerCase().endsWith(".log")) {
			fileName += ".log";
		}

		File logFile = new File(logDir, fileName);

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) { // Enable append mode
			writer.write(consoleLogBuffer.toString());
			writer.newLine(); // Add newline for clarity between appends
			logToConsole("Console log appended to: " + logFile.getAbsolutePath());
			consoleLogBuffer.setLength(0); // Clear buffer after writing
		} catch (IOException e) {
			logToConsole("Error appending to console log: " + e.getMessage());
		}
	}

	private void saveReportFiles() {
		File logDir = getWatchlistLogDirectory();
		if (logDir == null)
			return;

		String fileNamePrefix = logFileNameField.getText().trim();
		if (fileNamePrefix.isEmpty()) {
			fileNamePrefix = "stock_report";
		}

		String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

		if (completeResultsCheckbox.isSelected()) {
			saveTableToCSV(new File(logDir, String.format("%s_complete_%s.csv", fileNamePrefix, timestamp)), false,
					false);
		}

		if (bullishChangeCheckbox.isSelected()) {
			saveTableToCSV(new File(logDir, String.format("%s_bullish_change_%s.csv", fileNamePrefix, timestamp)), true,
					true);
		}

		if (bullishVolumeCheckbox.isSelected()) {
			saveTableToCSV(new File(logDir, String.format("%s_bullish_volume_%s.csv", fileNamePrefix, timestamp)), true,
					false);
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
				rowsToSave = rowsToSave.stream().filter(this::isRowBullish).collect(Collectors.toList());
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
			// return StrategyCheckerHelper.isBullishStatus(value.toString());

			if (value != null && BULLISH_STATUSES.contains(value.toString())) {
				return true;
			}

		}
		return false;
	}

	private void stopLogScheduler() {
		if (logScheduler != null) {
			logScheduler.shutdown();
			try {
				if (!logScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					logScheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				logScheduler.shutdownNow();
				logToConsole("Log scheduler interrupted during shutdown: " + e.getMessage());
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
			Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_13.class);
			prefs.put("liveUpdateStartTime", start.toString());
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
			Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_13.class);
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

	private void addDefaultPriceDataColumns() {
		// Default columns to show
		String[] defaultColumns = { "analystRating", "averageVol", "prevLastDayPrice" };

		for (String col : defaultColumns) {
			tableModel.addColumn(col);
		}

		dashboardTable.setModel(tableModel);
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
		endDatePicker = new JXDatePicker();
		endDatePicker.setFormats("yyyy-MM-dd");

		// Use JComboBox for hours and minutes - initialize as class variables
		String[] hours = new String[24];
		String[] minutes = new String[60];
		for (int i = 0; i < 24; i++) {
			hours[i] = String.format("%02d", i);
		}
		for (int i = 0; i < 60; i++) {
			minutes[i] = String.format("%02d", i);
		}

		startHourCombo = new JComboBox<>(hours);
		startMinuteCombo = new JComboBox<>(minutes);
		endHourCombo = new JComboBox<>(hours);
		endMinuteCombo = new JComboBox<>(minutes);

		// Set default times (09:30 and 16:00)
		startHourCombo.setSelectedItem("09");
		startMinuteCombo.setSelectedItem("30");
		endHourCombo.setSelectedItem("16");
		endMinuteCombo.setSelectedItem("00");

		// Clear button
		clearCustomRangeButton = new JButton("Clear Custom Range");

		// Status label
		timeRangeLabel = new JLabel("Time Range: Current Time");

		// Add components
		timeRangePanel.add(new JLabel("Time Range:"));
		timeRangePanel.add(currentTimeRadio);
		timeRangePanel.add(customRangeRadio);
		timeRangePanel.add(new JLabel("Start Date:"));
		timeRangePanel.add(startDatePicker);
		timeRangePanel.add(new JLabel("Start Time:"));
		timeRangePanel.add(startHourCombo);
		timeRangePanel.add(new JLabel(":"));
		timeRangePanel.add(startMinuteCombo);
		timeRangePanel.add(new JLabel("End Date:"));
		timeRangePanel.add(endDatePicker);
		timeRangePanel.add(new JLabel("End Time:"));
		timeRangePanel.add(endHourCombo);
		timeRangePanel.add(new JLabel(":"));
		timeRangePanel.add(endMinuteCombo);
		timeRangePanel.add(clearCustomRangeButton);
		timeRangePanel.add(timeRangeLabel);

		// Add listeners
		currentTimeRadio.addActionListener(e -> {
			startDatePicker.setEnabled(false);
			endDatePicker.setEnabled(false);
			startHourCombo.setEnabled(false);
			startMinuteCombo.setEnabled(false);
			endHourCombo.setEnabled(false);
			endMinuteCombo.setEnabled(false);
			clearCustomRangeButton.setEnabled(false);
			timeRangeLabel.setText("Time Range: Current Time");
		});

		customRangeRadio.addActionListener(e -> {
			startDatePicker.setEnabled(true);
			endDatePicker.setEnabled(true);
			startHourCombo.setEnabled(true);
			startMinuteCombo.setEnabled(true);
			endHourCombo.setEnabled(true);
			endMinuteCombo.setEnabled(true);
			clearCustomRangeButton.setEnabled(true);

			// Ensure dates are set when switching to custom range
			if (startDatePicker.getDate() == null) {
				startDatePicker.setDate(Date.from(ZonedDateTime.now().minusDays(7).toInstant()));
			}
			if (endDatePicker.getDate() == null) {
				endDatePicker.setDate(Date.from(ZonedDateTime.now().toInstant()));
			}

			updateTimeRangeLabel();
		});

		clearCustomRangeButton.addActionListener(e -> {
			startDatePicker.setDate(null);
			endDatePicker.setDate(null);
			// Reset to default times
			startHourCombo.setSelectedItem("09");
			startMinuteCombo.setSelectedItem("30");
			endHourCombo.setSelectedItem("16");
			endMinuteCombo.setSelectedItem("00");
			updateTimeRangeLabel();
		});

		// Initialize to Current Time mode
		startDatePicker.setEnabled(false);
		endDatePicker.setEnabled(false);
		startHourCombo.setEnabled(false);
		startMinuteCombo.setEnabled(false);
		endHourCombo.setEnabled(false);
		endMinuteCombo.setEnabled(false);
		clearCustomRangeButton.setEnabled(false);

		// Add listeners for updates
		startDatePicker.addActionListener(e -> updateTimeRangeLabel());
		endDatePicker.addActionListener(e -> updateTimeRangeLabel());
		startHourCombo.addActionListener(e -> updateTimeRangeLabel());
		startMinuteCombo.addActionListener(e -> updateTimeRangeLabel());
		endHourCombo.addActionListener(e -> updateTimeRangeLabel());
		endMinuteCombo.addActionListener(e -> updateTimeRangeLabel());

		add(timeRangePanel, BorderLayout.NORTH);
	}

	private void initializeTimeRangePanel() {
		// Set initial state based on default selection (Current Time)
		startDatePicker.setEnabled(false);
		endDatePicker.setEnabled(false);
		startTimeSpinner.setEnabled(false);
		endTimeSpinner.setEnabled(false);
		clearCustomRangeButton.setEnabled(false);

		// Set default dates for when custom range is selected
		startDatePicker.setDate(Date.from(ZonedDateTime.now().minusDays(7).toInstant()));
		endDatePicker.setDate(Date.from(ZonedDateTime.now().toInstant()));

		// Set default times
		setDefaultTimes();
	}

	private void setDefaultTimes() {
		try {
			// Set default start time to 09:30 (market open)
			Calendar startCal = Calendar.getInstance();
			startCal.set(Calendar.HOUR_OF_DAY, 9);
			startCal.set(Calendar.MINUTE, 30);
			startCal.set(Calendar.SECOND, 0);
			startCal.set(Calendar.MILLISECOND, 0);
			startTimeSpinner.setValue(startCal.getTime());

			// Set default end time to 16:00 (market close)
			Calendar endCal = Calendar.getInstance();
			endCal.set(Calendar.HOUR_OF_DAY, 16);
			endCal.set(Calendar.MINUTE, 0);
			endCal.set(Calendar.SECOND, 0);
			endCal.set(Calendar.MILLISECOND, 0);
			endTimeSpinner.setValue(endCal.getTime());

			// Ensure spinners are enabled when custom range is selected
			if (customRangeRadio.isSelected()) {
				startTimeSpinner.setEnabled(true);
				endTimeSpinner.setEnabled(true);
			}
		} catch (Exception e) {
			logToConsole("Error setting default times: " + e.getMessage());
		}
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
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Data source combo
		dataSourceComboBox = new JComboBox<>(dataSources);
		dataSourceComboBox.setSelectedItem(currentDataSource);
		dataSourceComboBox.addActionListener(e -> {
			currentDataSource = (String) dataSourceComboBox.getSelectedItem();
			currentDataProvider = dataProviderFactory.getProvider(currentDataSource, this::logToConsole);
			// refreshData();
		});

		// Volume Source combo box
		String[] volumeSources = { "Default", "Alpaca", "Yahoo", "Polygon", "TradingView" };
		volumeSourceCombo = new JComboBox<>(volumeSources);
		volumeSourceCombo.setSelectedItem("Alpaca");
		volumeSourceCombo.setToolTipText(
				"Select the source for volume data (Prev Vol and Current Vol). 'Default' uses the Data Source.");
		// volumeSourceCombo.addActionListener(e -> refreshData());

		// Watchlist combo
		watchlistComboBox = new JComboBox<>(watchlists.keySet().toArray(new String[0]));
		watchlistComboBox.setSelectedItem(currentWatchlist);
		watchlistComboBox.addActionListener(e -> {
			currentWatchlist = (String) watchlistComboBox.getSelectedItem();
			// refreshData();
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
		watchlistPanel.add(new JLabel("Volume Source:"), gbc);

		gbc.gridx = 1;
		watchlistPanel.add(volumeSourceCombo, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		watchlistPanel.add(new JLabel("Watchlist:"), gbc);

		gbc.gridx = 1;
		watchlistPanel.add(watchlistComboBox, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
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

		// Create Bullish Timeframe Checker panel
		bullishTimeframePanel = new JPanel(new GridLayout(0, 4)); // 4 columns
		bullishTimeframePanel.setBorder(BorderFactory.createTitledBorder("Bullish Timeframe Checker"));

		// Add checkboxes for each timeframe
		for (String tf : allTimeframes) {
			JCheckBox cb = new JCheckBox(tf, true); // All selected by default
			bullishTimeframeCheckboxes.put(tf, cb);
			bullishTimeframePanel.add(cb);
		}
		bullishTimeframePanel.setVisible(false); // Initially hidden

		// Show/hide timeframe panel based on bullish checkbox
		checkBullishCheckbox.addActionListener(e -> {
			bullishTimeframePanel.setVisible(checkBullishCheckbox.isSelected());
			liveUpdatePanel.revalidate();
			liveUpdatePanel.repaint();
		});

		// Alarm controls
		stopAlarmButton = createButton("Stop Alarm", new Color(200, 50, 50));
		stopAlarmButton.addActionListener(e -> {
			isAlarmActive = false;
			alarmOnCheckbox.setSelected(false);
			stopBuzzer();
			logToConsole("Alarm stopped.");
		});

		// Add this after the existing components
		JButton configureVolumeAlertButton = createButton("Configure Volume Alert", new Color(100, 149, 237));
		configureVolumeAlertButton.addActionListener(e -> openVolumeAlertConfigDialog());

		JButton configureZScoreAlertButton = createButton("Configure Z-Score Alert", new Color(100, 149, 237));
		configureZScoreAlertButton.addActionListener(e -> openZScoreAlertConfigDialog());

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
		gbc.gridwidth = 4;
		liveUpdatePanel.add(bullishTimeframePanel, gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.gridwidth = 2;
		liveUpdatePanel.add(configureVolumeAlertButton, gbc);

		gbc.gridx = 2;
		gbc.gridy = 4; // Adjust based on your layout
		gbc.gridwidth = 2;
		liveUpdatePanel.add(configureZScoreAlertButton, gbc);

		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 3;
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
		checkNonBullishButton.addActionListener(e -> {
			openNonBullishFilterWindow();
			/*
			 * JFrame timeframeWindow = new JFrame("Select Timeframes");
			 * timeframeWindow.setSize(300, 300); timeframeWindow.setLayout(new
			 * GridLayout(9, 1)); timeframeWindow.setLocationRelativeTo(this);
			 * 
			 * Map<String, JCheckBox> timeframeCheckboxes = new HashMap<>(); for (String
			 * timeframe : allTimeframes) { JCheckBox cb = new JCheckBox(timeframe, true);
			 * // All selected by default timeframeCheckboxes.put(timeframe, cb);
			 * timeframeWindow.add(cb); }
			 * 
			 * JButton applyButton = new JButton("Apply"); applyButton.addActionListener(ae
			 * -> { selectedTimeframes.clear(); for (String timeframe : allTimeframes) { if
			 * (timeframeCheckboxes.get(timeframe).isSelected()) {
			 * selectedTimeframes.add(timeframe); } } checkBullishUptrendNeutralRows();
			 * timeframeWindow.dispose(); }); timeframeWindow.add(applyButton);
			 * 
			 * timeframeWindow.setVisible(true);
			 */
		});

		// Add Select All button
		JButton selectAllButton = createButton("Select All", new Color(100, 149, 237));
		selectAllButton.addActionListener(e -> selectAllRows(true));

		// Add Manage Columns button
		JButton manageColumnsButton = createButton("Manage Columns", new Color(100, 149, 237));
		manageColumnsButton.addActionListener(e -> openColumnVisibilityWindow());

		// Add Open Filter button here
		openFilterButton = createButton("Open Filter", new Color(100, 149, 237));
		openFilterButton.addActionListener(e -> openFilterWindow());

		configButton = createButton("Configure Indicators", new Color(100, 149, 237));
		configButton.addActionListener(e -> openConfigWindow());

		JButton monteCarloButton = createButton("Show Monte Carlo Graph 1min", new Color(100, 149, 237));
		monteCarloButton.addActionListener(e -> showMonteCarloGraph());

		// Row 0
		gbc.gridx = 0;
		gbc.gridy = 0;
		tableActionsPanel.add(refreshButton, gbc);

		gbc.gridx = 1;
		tableActionsPanel.add(toggleVisibilityButton, gbc);

		gbc.gridx = 2;
		tableActionsPanel.add(showAllButton, gbc);

		// Row 1
		gbc.gridx = 0;
		gbc.gridy = 1;
		tableActionsPanel.add(listSymbolsButton, gbc);

		gbc.gridx = 1;
		tableActionsPanel.add(checkNonBullishButton, gbc);

		gbc.gridx = 2;
		tableActionsPanel.add(selectAllButton, gbc);

		// Row 2
		gbc.gridx = 0;
		gbc.gridy = 2;
		tableActionsPanel.add(openFilterButton, gbc);

		gbc.gridx = 1;
		tableActionsPanel.add(configButton, gbc);

		gbc.gridx = 2;
		tableActionsPanel.add(monteCarloButton, gbc);

		// Row 3 - New Select/Deselect All buttons
		gbc.gridx = 0;
		gbc.gridy = 3;
		tableActionsPanel.add(manageColumnsButton, gbc);

		gbc.gridx = 1;
		JButton deselectAllButton = createButton("Deselect All", new Color(100, 149, 237));
		deselectAllButton.addActionListener(e -> selectAllRows(false));
		tableActionsPanel.add(deselectAllButton, gbc);

		// Empty cell to maintain alignment (or add another button if needed)
		gbc.gridx = 2;
		tableActionsPanel.add(new JLabel(), gbc);
	}

	private void setupMainContentArea() {
		// Console setup
		consoleArea = new JTextArea(10, 30);
		consoleArea.setEditable(false);
		consoleScrollPane = new JScrollPane(consoleArea);
		consolePanel = new JPanel(new BorderLayout());
		consolePanel.add(consoleScrollPane, BorderLayout.CENTER);

		// Table setup
		dashboardTable = new JTable();
		dashboardTable.setAutoCreateRowSorter(true);

		// Create table model with basic columns
		String[] basicColumns = { "Select", "Symbol", "Latest Price", "Prev Vol", "Current Vol", "% Change" };
		tableModel = new FilterableTableModel(basicColumns, 0, this);

		// Add default PriceData columns
		addDefaultPriceDataColumns();

		// Add indicator columns
		updateTableColumns();

		dashboardTable.setModel(tableModel);
		dashboardTable.setRowSorter(new TableRowSorter<>(tableModel));
		dashboardTable.setRowHeight(25);
		dashboardTable.setGridColor(Color.LIGHT_GRAY);
		dashboardTable.setShowGrid(true);
		dashboardTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		// Set up checkbox column
		dashboardTable.getColumnModel().getColumn(0).setMaxWidth(30);
		dashboardTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
		dashboardTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
			private final JCheckBox checkBox = new JCheckBox();

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				int modelRow = table.convertRowIndexToModel(row);
				Boolean checked = (Boolean) table.getModel().getValueAt(modelRow, column);

				checkBox.setSelected(checked != null && checked);
				checkBox.setHorizontalAlignment(SwingConstants.CENTER);
				checkBox.setOpaque(true);

				if (isSelected) {
					checkBox.setBackground(table.getSelectionBackground());
					checkBox.setForeground(table.getSelectionForeground());
				} else {
					checkBox.setBackground(table.getBackground());
					checkBox.setForeground(table.getForeground());
				}

				return checkBox;
			}
		});

		// Double-click listener for symbol column (index 1)
		dashboardTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int column = dashboardTable.columnAtPoint(e.getPoint());
					int row = dashboardTable.rowAtPoint(e.getPoint());
					if (column == 1) { // Symbol column
						int modelRow = dashboardTable.convertRowIndexToModel(row);
						String symbol = (String) tableModel.getValueAt(modelRow, 1);
						// Find the highest timeframe with indicators for this row
						String highestTimeframe = null;
						for (String tf : allTimeframes) {
							if (!timeframeIndicators.get(tf).isEmpty()) {
								highestTimeframe = tf;
								break;
							}
						}
						if (highestTimeframe != null) {
							Set<String> indicators = new HashSet<>(timeframeIndicators.get(highestTimeframe));
							if (currentTimeRadio.isSelected()) {
								new SymbolChartWindow(symbol, highestTimeframe, indicators, currentDataProvider,
										StockDashboardv1_22_13.this::logToConsole).setVisible(true);
							} else {
								ZonedDateTime start = getStartDateTime();
								ZonedDateTime end = getEndDateTime();

								SymbolChartWindow.TimeRangeConfig customRange = new SymbolChartWindow.TimeRangeConfig(
										false, start, end);

								SymbolChartWindow symbolChartWindow = new SymbolChartWindow(symbol, highestTimeframe,
										indicators, currentDataProvider, StockDashboardv1_22_13.this::logToConsole,
										customRange);

								symbolChartWindow.setVisible(true);
							}
						} else {
							logToConsole("No indicators configured for any timeframe for symbol: " + symbol);
						}
					}
				}
			}
		});

		// Set renderers
		dashboardTable.setDefaultRenderer(Object.class, new StatusCellRenderer());
		dashboardTable.setDefaultRenderer(Double.class, new DoubleCellRenderer());
		dashboardTable.setDefaultRenderer(Long.class, new LongCellRenderer());
		dashboardTable.setDefaultRenderer(BreakoutValue.class, new BreakoutValueRenderer());

		// Wrap the table in a JScrollPane with explicit scrollbar policies
		JScrollPane tableScrollPane = new JScrollPane(dashboardTable);
		tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		dashboardTable.setPreferredScrollableViewportSize(new Dimension(800, 400));

		// Create table actions panel
		tableActionsPanel = new JPanel(new GridBagLayout());
		setupTableActionsPanel();

		// Create a main panel to hold table and actions
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(tableActionsPanel, BorderLayout.NORTH);
		mainPanel.add(tableScrollPane, BorderLayout.CENTER);

		// Create JSplitPane with table and console
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPanel, consolePanel);
		splitPane.setDividerLocation(0.7);

		// Add splitPane to the frame
		add(splitPane, BorderLayout.CENTER);

		// Initialize column widths
		updateTableColumnWidths();
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

			// Add Resistance Indicators panel
			JPanel resistancePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			resistancePanel.setBorder(BorderFactory.createTitledBorder("Resistance Indicators"));

			JCheckBox highestCloseOpenCheckbox = new JCheckBox("Highest close/open");
			highestCloseOpenCheckbox.setSelected(timeframeIndicators.get(tf).contains("HighestCloseOpen"));

			JComboBox<String> timeRangeCombo = new JComboBox<>(new String[] { "1D", "2D", "5D", "7D", "1M" });
			timeRangeCombo.setSelectedItem("1D");
			timeRangeCombo.setEnabled(highestCloseOpenCheckbox.isSelected());

			// Add Session combobox
			JComboBox<String> sessionCombo = new JComboBox<>(
					new String[] { "all", "premarket", "standard", "postmarket" });
			sessionCombo.setSelectedItem("all");
			sessionCombo.setEnabled(highestCloseOpenCheckbox.isSelected());

			// NEW: Add indexCounter field
			JLabel indexCounterLabel = new JLabel("Index Counter:");
			JTextField indexCounterField = new JTextField("5", 3); // Default value 5
			indexCounterField.setEnabled(highestCloseOpenCheckbox.isSelected());

			// Store the index counter field for later retrieval
			if (!indicatorIndexCounterFields.containsKey(tf)) {
				indicatorIndexCounterFields.put(tf, indexCounterField);
			} else {
				// Load existing value
				indexCounterField.setText(indicatorIndexCounterFields.get(tf).getText());
			}

			// Enable/disable based on checkbox
			highestCloseOpenCheckbox.addActionListener(e -> {
				timeRangeCombo.setEnabled(highestCloseOpenCheckbox.isSelected());
				sessionCombo.setEnabled(highestCloseOpenCheckbox.isSelected());
				indexCounterField.setEnabled(highestCloseOpenCheckbox.isSelected());
			});

			resistancePanel.add(highestCloseOpenCheckbox);
			resistancePanel.add(new JLabel("Time Range:"));
			resistancePanel.add(timeRangeCombo);
			resistancePanel.add(new JLabel("Session:"));
			resistancePanel.add(sessionCombo);
			resistancePanel.add(indexCounterLabel);
			resistancePanel.add(indexCounterField);

			JPanel targetValuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			targetValuePanel.setBorder(BorderFactory.createTitledBorder("Target Value Indicators"));

			JCheckBox movingAvgTargetCheckbox = new JCheckBox("Moving Average Target Value");
			targetValuePanel.add(movingAvgTargetCheckbox);

			// Store the combobox for later retrieval
			if (!indicatorTimeRangeCombos.containsKey(tf)) {
				indicatorTimeRangeCombos.put(tf, timeRangeCombo);
			} else {
				timeRangeCombo.setSelectedItem(indicatorTimeRangeCombos.get(tf).getSelectedItem());
			}

			if (!indicatorSessionCombos.containsKey(tf)) {
				indicatorSessionCombos.put(tf, sessionCombo);
			} else {
				sessionCombo.setSelectedItem(indicatorSessionCombos.get(tf).getSelectedItem());
			}

			// Add Index Bars panel
			JPanel indexBarsPanel = new JPanel(new GridBagLayout());
			indexBarsPanel.setBorder(BorderFactory.createTitledBorder("Index Bars"));
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.anchor = GridBagConstraints.WEST;
			gbc.fill = GridBagConstraints.HORIZONTAL;

			// Start Index
			gbc.gridx = 0;
			gbc.gridy = 0;
			indexBarsPanel.add(new JLabel("Start Index:"), gbc);

			JTextField startIndexField = timeframeIndexFields.get(tf).get("startIndex");
			startIndexField.setText(String.valueOf(timeframeIndexRanges.get(tf).get("startIndex")));
			startIndexField.setInputVerifier(new javax.swing.InputVerifier() {
				@Override
				public boolean verify(javax.swing.JComponent input) {
					if (input instanceof JTextField) {
						String text = ((JTextField) input).getText();
						try {
							Integer.parseInt(text);
							return true;
						} catch (NumberFormatException e) {
							return false;
						}
					}
					return true;
				}
			});
			gbc.gridx = 1;
			indexBarsPanel.add(startIndexField, gbc);

			// End Index
			gbc.gridx = 0;
			gbc.gridy = 1;
			indexBarsPanel.add(new JLabel("End Index:"), gbc);

			JTextField endIndexField = timeframeIndexFields.get(tf).get("endIndex");
			endIndexField.setText(String.valueOf(timeframeIndexRanges.get(tf).get("endIndex")));
			endIndexField.setInputVerifier(new javax.swing.InputVerifier() {
				@Override
				public boolean verify(javax.swing.JComponent input) {
					if (input instanceof JTextField) {
						String text = ((JTextField) input).getText();
						try {
							Integer.parseInt(text);
							return true;
						} catch (NumberFormatException e) {
							return false;
						}
					}
					return true;
				}
			});
			gbc.gridx = 1;
			indexBarsPanel.add(endIndexField, gbc);

			// === Volume Panel ===
			JPanel volumePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			volumePanel.setBorder(BorderFactory.createTitledBorder("Volume"));

			// Current bar volume checkbox
			JCheckBox volumeCb = new JCheckBox("volume");
			volumeCb.setToolTipText("Use current bar volume for this timeframe");
			volumeCb.setSelected(timeframeIndicators.get(tf).contains("Volume"));
			volumeCb.addActionListener(e -> {
				if (volumeCb.isSelected())
					timeframeIndicators.get(tf).add("Volume");
				else
					timeframeIndicators.get(tf).remove("Volume");
			});

			// 20MA volume checkbox
			JCheckBox volume20MACb = new JCheckBox("volume20MA");
			volume20MACb.setToolTipText("20-period SMA of volume");
			volume20MACb.setSelected(timeframeIndicators.get(tf).contains("Volume20MA"));
			volume20MACb.addActionListener(e -> {
				if (volume20MACb.isSelected())
					timeframeIndicators.get(tf).add("Volume20MA");
				else
					timeframeIndicators.get(tf).remove("Volume20MA");
			});

			indicatorCheckboxes.computeIfAbsent(tf, k -> new LinkedHashMap<>()).put("Volume", volumeCb);
			indicatorCheckboxes.get(tf).put("Volume20MA", volume20MACb);

			volumePanel.add(volumeCb);
			volumePanel.add(volume20MACb);

			// To accommodate the new panel, change tfPanel to use BoxLayout for vertical
			// stacking
			tfPanel.setLayout(new BoxLayout(tfPanel, BoxLayout.Y_AXIS));

			tfPanel.add(tfCheckbox);
			tfPanel.add(indPanel);
			tfPanel.add(resistancePanel);
			tfPanel.add(targetValuePanel);
			tfPanel.add(indexBarsPanel);
			tfPanel.add(volumePanel);
			mainPanel.add(tfPanel);
		}

		// Custom Indicators button
		JPanel customIndicatorsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton customIndicatorsButton = new JButton("Configure Custom Indicators");
		customIndicatorsButton.addActionListener(e -> {
			openCustomIndicatorsConfigDialog();
		});
		customIndicatorsPanel.add(customIndicatorsButton);
		mainPanel.add(customIndicatorsPanel);
		mainPanel.add(Box.createVerticalStrut(10));

		// Inheritance option
		inheritCheckbox = new JCheckBox("Apply higher timeframe indicators to lower timeframes");
		mainPanel.add(inheritCheckbox);

		uptrendFilterCheckbox = new JCheckBox("Call only lower timeframes when higher timeframe is same UpTrend",
				false);
		uptrendFilterCheckbox.setSelected(uptrendFilterEnabled);
		mainPanel.add(uptrendFilterCheckbox);

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

	/**
	 * Open custom indicators configuration dialog
	 */
	private void openCustomIndicatorsConfigDialog() {
		checkAndInstantiateIndicatorsManagementApp();

		CustomIndicatorsConfigDialog dialog = new CustomIndicatorsConfigDialog(this, this, allTimeframes,
				indicatorsManagementApp);

		// Set initial configuration if any exists
		if (!customIndicatorCombinations.isEmpty()) {
			dialog.setInitialConfiguration(customIndicatorCombinations);
		}

		dialog.setVisible(true);
	}

	/**
	 * Set custom indicator combinations from configuration
	 */
	public void setCustomIndicatorCombinations(Map<String, Set<String>> combinations) {
		this.customIndicatorCombinations.clear();
		this.customIndicatorCombinations.putAll(combinations);
		logToConsole("Custom indicator combinations updated: " + combinations.size() + " timeframes configured");
	}

	/**
	 * Get custom indicator combinations
	 */
	public Map<String, Set<String>> getCustomIndicatorCombinations() {
		return Collections.unmodifiableMap(customIndicatorCombinations);
	}

	/**
	 * Update table columns to include custom indicator combinations
	 */
	public void updateTableColumnsForCustomIndicators() {
		// This will trigger a table column refresh including custom indicators
		updateTableColumns();
	}

	/**
	 * Get custom indicator by name with proper debugging
	 */
	private CustomIndicator getCustomIndicatorByName(String name) {
		if (name == null || name.trim().isEmpty()) {
			logToConsole("⚠️ getCustomIndicatorByName: name is null or empty");
			return null;
		}

		logToConsole("🔍 Searching for custom indicator: '" + name + "'");

		// Check if indicatorsManagementApp is available
		if (indicatorsManagementApp == null) {
			logToConsole("❌ IndicatorsManagementApp is null - cannot find custom indicator");
			checkAndInstantiateIndicatorsManagementApp();
			if (indicatorsManagementApp == null) {
				return null;
			}
		}

		// Get all custom indicators
		Set<CustomIndicator> allCustomIndicators = indicatorsManagementApp.getGlobalCustomIndicators();
		logToConsole("📊 Total custom indicators available: " + allCustomIndicators.size());

		// Debug: List all available indicators
		for (CustomIndicator indicator : allCustomIndicators) {
			logToConsole("   Available: '" + indicator.getName() + "' (type: " + indicator.getType() + ")");
		}

		// Search for the indicator
		for (CustomIndicator indicator : allCustomIndicators) {
			if (name.equals(indicator.getName())) {
				logToConsole("✅ Found custom indicator: '" + name + "'");
				return indicator;
			}
		}

		logToConsole("❌ Custom indicator not found: '" + name + "'");
		return null;
	}

	private void saveConfiguration() {
		// Store the current column structure and data BEFORE clearing
		Vector<Vector<Object>> oldData = new Vector<>();
		Vector<String> oldColumnNames = new Vector<>();

		if (tableModel != null && tableModel.getDataVector() != null) {
			// Backup current data and column structure
			for (int i = 0; i < tableModel.getRowCount(); i++) {
				oldData.add(new Vector<>(tableModel.getDataVector().get(i)));
			}
			for (int i = 0; i < tableModel.getColumnCount(); i++) {
				oldColumnNames.add(tableModel.getColumnName(i));
			}
		}

		// Clear existing selections
		for (Set<String> indicators : timeframeIndicators.values()) {
			indicators.clear();
		}

		// Save new selections
		selectedTimeframes.clear();
		for (String tf : allTimeframes) {
			JCheckBox tfCB = timeframeCheckboxes.get(tf);

			try {
				int startIndex = Integer.parseInt(timeframeIndexFields.get(tf).get("startIndex").getText());
				int endIndex = Integer.parseInt(timeframeIndexFields.get(tf).get("endIndex").getText());
				timeframeIndexRanges.get(tf).put("startIndex", startIndex);
				timeframeIndexRanges.get(tf).put("endIndex", endIndex);
			} catch (NumberFormatException ex) {
				logToConsole("Invalid index value for timeframe " + tf + ": " + ex.getMessage());
			}

			if (tfCB != null && tfCB.isSelected()) {
				selectedTimeframes.add(tf);
				Map<String, JCheckBox> indCBs = indicatorCheckboxes.get(tf);
				for (Map.Entry<String, JCheckBox> entry : indCBs.entrySet()) {
					if (entry.getValue().isSelected()) {
						timeframeIndicators.get(tf).add(entry.getKey());
					}
				}

				// Save resistance indicators
				JPanel resistancePanel = (JPanel) ((JPanel) tfCB.getParent()).getComponent(2);
				JCheckBox highestCloseOpenCB = (JCheckBox) resistancePanel.getComponent(0);
				if (highestCloseOpenCB.isSelected()) {
					timeframeIndicators.get(tf).add("HighestCloseOpen");
					// Save session selection
					JComboBox<String> sessionCombo = (JComboBox<String>) resistancePanel.getComponent(4);
					if (sessionCombo != null) {
						indicatorSessionCombos.get(tf).setSelectedItem(sessionCombo.getSelectedItem());
					}

					// Save index counter value
					JTextField indexCounterField = (JTextField) resistancePanel.getComponent(6);
					if (indexCounterField != null) {
						indicatorIndexCounterFields.put(tf, indexCounterField);
					}
				}

				// Save target value indicators
				JPanel movingAveTargetValuePanel = (JPanel) ((JPanel) tfCB.getParent()).getComponent(3);
				JCheckBox movingAveTargetValueCB = (JCheckBox) movingAveTargetValuePanel.getComponent(0);
				if (movingAveTargetValueCB.isSelected()) {
					timeframeIndicators.get(tf).add(MovingAverageTargetValueStrategy.MATV_STRATEGY_NAME_CONSTANT);
				}
			}
		}

		// Save the new setting
		uptrendFilterEnabled = uptrendFilterCheckbox.isSelected();

		// Handle inheritance if enabled
		if (inheritCheckbox.isSelected()) {
			applyIndicatorInheritance();
		}

		// =========================================================================
		// NEW: COLLECT ALL COLUMN NAMES FOR MAPPING AND CONFIGURATION
		// =========================================================================

		List<String> allColumnNames = new ArrayList<>();
		Map<String, Object> columnConfiguration = new HashMap<>();

		// 1. Add basic columns
		allColumnNames.addAll(Arrays.asList("Select", "Symbol", "Latest Price", "Prev Vol", "Current Vol", "% Change"));
		columnConfiguration.put("basicColumns", new ArrayList<>(allColumnNames));

		// 2. Add PriceData attribute columns that are visible
		List<String> priceDataColumns = new ArrayList<>();
		for (String attribute : PriceDataColumnRegistry.getDisplayableFieldNames()) {
			if (isColumnVisible(attribute)) {
				allColumnNames.add(attribute);
				priceDataColumns.add(attribute);
			}
		}
		columnConfiguration.put("priceDataColumns", priceDataColumns);

		// 3. Add indicator columns from all timeframes
		Map<String, List<String>> timeframeIndicatorColumns = new HashMap<>();
		for (String tf : allTimeframes) {
			List<String> tfIndicators = new ArrayList<>();
			for (String indicator : timeframeIndicators.get(tf)) {
				String columnName = tf + " " + indicator;
				allColumnNames.add(columnName);
				tfIndicators.add(indicator);
			}
			timeframeIndicatorColumns.put(tf, tfIndicators);
		}
		columnConfiguration.put("timeframeIndicators", timeframeIndicatorColumns);

		// 4. Add Z-Score columns
		List<String> zScoreColumns = new ArrayList<>();
		for (Map.Entry<String, JCheckBox> entry : zScoreCheckboxes.entrySet()) {
			if (entry.getValue().isSelected()) {
				allColumnNames.add(entry.getKey());
				zScoreColumns.add(entry.getKey());
			}
		}
		columnConfiguration.put("zScoreColumns", zScoreColumns);

		// 5. Add custom indicator combinations
		Map<String, List<String>> customIndicatorColumns = new HashMap<>();
		for (Map.Entry<String, Set<String>> entry : customIndicatorCombinations.entrySet()) {
			String timeframe = entry.getKey();
			List<String> customIndicators = new ArrayList<>();
			for (String customIndicator : entry.getValue()) {
				String columnName = timeframe + " " + customIndicator;
				allColumnNames.add(columnName);
				customIndicators.add(customIndicator);
			}
			customIndicatorColumns.put(timeframe, customIndicators);
		}
		columnConfiguration.put("customIndicators", customIndicatorColumns);

		// 6. ANALYZE AND STORE COLUMN MAPPINGS
		Map<String, DashboardColumnCollector.ColumnMapping> columnMappings = DashboardColumnCollector
				.analyzeColumns(allColumnNames);

		// Convert to serializable format for saving
		Map<String, Map<String, Object>> serializableMappings = new HashMap<>();
		for (Map.Entry<String, DashboardColumnCollector.ColumnMapping> entry : columnMappings.entrySet()) {
			DashboardColumnCollector.ColumnMapping mapping = entry.getValue();
			Map<String, Object> mappingData = new HashMap<>();
			mappingData.put("type", mapping.getType().name());
			mappingData.put("timeframe", mapping.getTimeframe());
			mappingData.put("indicator", mapping.getIndicator());
			mappingData.put("zscoreStrategy", mapping.getZscoreStrategy());
			mappingData.put("priceDataField", mapping.getPriceDataField());
			mappingData.put("fieldType", mapping.getFieldType().getName());
			serializableMappings.put(entry.getKey(), mappingData);
		}

		columnConfiguration.put("columnMappings", serializableMappings);
		columnConfiguration.put("allColumnNames", allColumnNames);

		// 7. Generate and log column mapping report
		String mappingReport = DashboardColumnCollector.generateColumnMappingReport(allColumnNames);
		logToConsole("📊 COLUMN CONFIGURATION SAVED:");
		logToConsole(mappingReport);

		// 8. Store in main configuration
		columnsConfigData.put("columnConfiguration", columnConfiguration);

		// Log statistics
		logToConsole("💾 Saved column configuration:");
		logToConsole("   - Total columns: " + allColumnNames.size());
		logToConsole("   - Basic columns: " + ((List<?>) columnConfiguration.get("basicColumns")).size());
		logToConsole("   - PriceData columns: " + ((List<?>) columnConfiguration.get("priceDataColumns")).size());
		logToConsole("   - Timeframe indicators: " + timeframeIndicatorColumns.size() + " timeframes");
		logToConsole("   - Z-Score columns: " + zScoreColumns.size());
		logToConsole("   - Custom indicators: " + customIndicatorColumns.size() + " timeframes");
		logToConsole("   - Column mappings: " + columnMappings.size());

		// =========================================================================
		// END OF NEW COLUMN COLLECTION CODE
		// =========================================================================

		// Update table columns WITHOUT losing existing data
		updateTableColumnsPreservingData(oldData, oldColumnNames);
	}

	private void updateTableColumnsPreservingData(Vector<Vector<Object>> oldData, Vector<String> oldColumnNames) {
		String[] newColumnNames = createColumnNames();

		// Create new model with updated columns
		FilterableTableModel newModel = new FilterableTableModel(newColumnNames, 0, this);

		// Transfer data from old structure to new structure
		for (Vector<Object> oldRow : oldData) {
			Vector<Object> newRow = new Vector<>();

			// For each column in the new structure, find matching data from old structure
			for (String newColName : newColumnNames) {
				Object value = "N/A"; // Default value

				// Find matching column in old data
				int oldColIndex = oldColumnNames.indexOf(newColName);
				if (oldColIndex >= 0 && oldColIndex < oldRow.size()) {
					value = oldRow.get(oldColIndex);
				}

				newRow.add(value);
			}
			newModel.addRow(newRow);
		}

		// Update the table model
		tableModel = newModel;
		dashboardTable.setModel(tableModel);

		// Reapply all the table settings
		updateTableColumnWidths();
		updateTableRenderers();

		// Set up checkbox column renderer and editor
		dashboardTable.getColumnModel().getColumn(0).setMaxWidth(30);
		dashboardTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
		dashboardTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
			private final JCheckBox checkBox = new JCheckBox();

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				int modelRow = table.convertRowIndexToModel(row);
				Boolean checked = (Boolean) table.getModel().getValueAt(modelRow, column);

				checkBox.setSelected(checked != null && checked);
				checkBox.setHorizontalAlignment(SwingConstants.CENTER);
				checkBox.setOpaque(true);

				if (isSelected) {
					checkBox.setBackground(table.getSelectionBackground());
					checkBox.setForeground(table.getSelectionForeground());
				} else {
					checkBox.setBackground(table.getBackground());
					checkBox.setForeground(table.getForeground());
				}

				return checkBox;
			}
		});

		// Refresh the row sorter
		dashboardTable.setRowSorter(new TableRowSorter<>(tableModel));

		// Refresh data to repopulate indicators (this will keep the existing structure)
		// refreshData();

		logToConsole("Indicator configuration updated successfully");
	}

	private void openFilterWindow() {
		if (filterWindow != null && filterWindow.isVisible()) {
			filterWindow.toFront();
			return;
		}

		filterWindow = new JFrame("Filter Settings");
		filterWindow.setSize(400, 600);
		filterWindow.setLayout(new BorderLayout());

		// Get filterable columns (numerical PriceData attributes present in table)
		List<String> filterableColumns = new ArrayList<>();
		for (int c = 0; c < tableModel.getColumnCount(); c++) {
			String colName = tableModel.getColumnName(c);
			Class<?> cls = tableModel.getColumnClass(c);
			if ((cls == Double.class || cls == Long.class) && Arrays.asList("postmarketClose", "premarketChange",
					"changeFromOpen", "percentChange", "postmarketChange", "gap", "premarketVolume", "currentVolume",
					"postmarketVolume", "previousVolume", "prevLastDayPrice", "averageVol", "premarketHigh", "high",
					"postmarketHigh", "premarketLow", "low", "postmarketLow").contains(colName)) {
				filterableColumns.add(colName);
			}
		}
		// Sort alphabetically for better UX
		Collections.sort(filterableColumns);

		// Maps for min/max fields
		Map<String, JTextField> minFields = new HashMap<>();
		Map<String, JTextField> maxFields = new HashMap<>();

		// Main panel for filters
		JPanel filtersPanel = new JPanel();
		filtersPanel.setLayout(new BoxLayout(filtersPanel, BoxLayout.Y_AXIS));

		for (String colName : filterableColumns) {
			JPanel colPanel = new JPanel(new GridLayout(1, 4, 5, 5)); // Adjusted to 4 for better spacing
			colPanel.setBorder(BorderFactory.createTitledBorder(colName));

			JTextField minField = new JTextField(10);
			JTextField maxField = new JTextField(10);

			minFields.put(colName, minField);
			maxFields.put(colName, maxField);

			// Load existing filters (if any)
			if (tableModel.columnFilters.containsKey(colName)) {
				double[] range = tableModel.columnFilters.get(colName);
				if (range[0] > Double.NEGATIVE_INFINITY) {
					minField.setText(String.valueOf(range[0]));
				}
				if (range[1] < Double.POSITIVE_INFINITY) {
					maxField.setText(String.valueOf(range[1]));
				}
			}

			colPanel.add(new JLabel("Min:"));
			colPanel.add(minField);
			colPanel.add(new JLabel("Max:"));
			colPanel.add(maxField);

			filtersPanel.add(colPanel);
		}

		JScrollPane scrollPane = new JScrollPane(filtersPanel);
		filterWindow.add(scrollPane, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton applyButton = new JButton("Apply");
		JButton clearButton = new JButton("Clear");

		applyButton.addActionListener(e -> applyFilters(minFields, maxFields, filterableColumns));
		clearButton.addActionListener(e -> {
			minFields.values().forEach(field -> field.setText(""));
			maxFields.values().forEach(field -> field.setText(""));
			applyFilters(minFields, maxFields, filterableColumns);
		});

		buttonPanel.add(applyButton);
		buttonPanel.add(clearButton);
		filterWindow.add(buttonPanel, BorderLayout.SOUTH);

		filterWindow.setLocationRelativeTo(this);
		filterWindow.setVisible(true);
	}

	private void openVolumeAlertConfigDialog() {
		VolumeAlertConfigDialog dialog = new VolumeAlertConfigDialog(this, volumeAlertConfig, allTimeframes);
		dialog.setVisible(true);

		if (dialog.isSaved()) {
			volumeAlertConfig = dialog.getConfig();
			logToConsole("Volume alert configuration saved");
			// You can persist the configuration here if needed
		}
	}

	private void applyFilters(Map<String, JTextField> minFields, Map<String, JTextField> maxFields,
			List<String> filterableColumns) {
		Map<String, double[]> newFilters = new HashMap<>();

		boolean hasError = false;
		for (String col : filterableColumns) {
			String minTxt = minFields.get(col).getText().trim();
			String maxTxt = maxFields.get(col).getText().trim();

			if (!minTxt.isEmpty() || !maxTxt.isEmpty()) {
				double min = minTxt.isEmpty() ? Double.NEGATIVE_INFINITY : 0;
				double max = maxTxt.isEmpty() ? Double.POSITIVE_INFINITY : 0;

				try {
					if (!minTxt.isEmpty())
						min = Double.parseDouble(minTxt);
					if (!maxTxt.isEmpty())
						max = Double.parseDouble(maxTxt);

					if (min > max) {
						throw new NumberFormatException("Min > Max");
					}
					newFilters.put(col, new double[] { min, max });
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(filterWindow, "Invalid number for " + col + ": " + ex.getMessage(),
							"Error", JOptionPane.ERROR_MESSAGE);
					hasError = true;
				}
			}
		}

		if (hasError)
			return;

		// Update filters using setter
		tableModel.setColumnFilters(newFilters);

		// Apply hiding
		SwingUtilities.invokeLater(() -> {
			tableModel.showAllSymbols(); // Reset visibility

			Set<String> toHide = new HashSet<>();
			for (int i = 0; i < tableModel.getDataVector().size(); i++) { // Model rows
				if (!tableModel.rowSatisfiesFilters(i)) {
					String symbol = (String) tableModel.getValueAt(i, 1);
					if (symbol != null) {
						toHide.add(symbol);
					}
				}
			}

			tableModel.toggleSymbolsVisibility(toHide);
			tableModel.fireTableDataChanged();
			logToConsole("Filters applied successfully");
		});
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

	private boolean passesVolumeFilters(long previousVolume, long currentVolume) {
		try {
			// Check previous volume filter - add null checks
			if (prevVolMinField != null && !prevVolMinField.getText().isEmpty()) {
				long min = Long.parseLong(prevVolMinField.getText());
				if (previousVolume < min)
					return false;
			}
			if (prevVolMaxField != null && !prevVolMaxField.getText().isEmpty()) {
				long max = Long.parseLong(prevVolMaxField.getText());
				if (previousVolume > max)
					return false;
			}

			// Check current volume filter - add null checks
			if (currentVolMinField != null && !currentVolMinField.getText().isEmpty()) {
				long min = Long.parseLong(currentVolMinField.getText());
				if (currentVolume < min)
					return false;
			}
			if (currentVolMaxField != null && !currentVolMaxField.getText().isEmpty()) {
				long max = Long.parseLong(currentVolMaxField.getText());
				if (currentVolume > max)
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
		List<String> visibleSymbols = getVisibleSymbols(excludedSymbols);

		if (visibleSymbols.isEmpty()) {
			logToConsole("No visible symbols to check for bullish conditions");
			return;
		}

		// Get ALL enabled strategies from IndicatorsManagementApp
		List<StrategyConfig> allEnabledStrategies = new ArrayList<>();
		if (indicatorsManagementApp != null) {
			strategyCheckerHelper.setIndicatorsManagementApp(indicatorsManagementApp);
			allEnabledStrategies = strategyCheckerHelper.getEnabledStrategies();
		}

		// Filter strategies by current watchlist using helper
		List<StrategyConfig> applicableStrategies = strategyCheckerHelper
				.filterStrategiesByCurrentWatchlist(allEnabledStrategies, currentWatchlist);

		// Get selected timeframes from bullish timeframe checkboxes
		Set<String> selectedTimeframes = getSelectedBullishTimeframes();

		logToConsole("🔍 Checking bullish symbols using OR condition across " + applicableStrategies.size()
				+ " applicable strategies");
		logToConsole("   Selected timeframes: " + String.join(", ", selectedTimeframes));
		logToConsole("   Visible symbols: " + visibleSymbols.size());

		// Use OR condition - symbol is bullish if it matches ANY strategy
		List<String> bullishSymbols = strategyCheckerHelper.getBullishSymbolsForAnyStrategy(visibleSymbols,
				applicableStrategies, currentWatchlist, selectedTimeframes);

		// NEW: Check if we should filter by volume alerts for all symbols or just
		// bullish
		List<String> finalSymbols;
		if (volumeAlertConfig.isEnabled() && volumeAlertConfig.isVolume20MAEnabled()) {
			if (volumeAlertConfig.isOnlyBullishSymbols()) {
				// Original behavior - only check bullish symbols
				finalSymbols = VolumeAlertAnalyser.analyze(bullishSymbols, volumeAlertConfig, priceDataMap);
			} else {
				// NEW: Check all visible symbols in the table
				finalSymbols = checkVolumeAlertsForAllSymbols(excludedSymbols);
			}
		} else {
			finalSymbols = bullishSymbols;
		}

		// Use enhanced alert handling that considers strategy-level alarms
		handleEnhancedBullishAlerts(finalSymbols, applicableStrategies);
	}

	/**
	 * Enhanced bullish alert handling that considers strategy-level alarm settings
	 */
	private void handleEnhancedBullishAlerts(List<String> bullishSymbols, List<StrategyConfig> applicableStrategies) {
		if (bullishSymbols.isEmpty()) {
			stopBuzzer();
			return;
		}

		// Group bullish symbols by strategy for detailed logging
		Map<String, Set<String>> strategyToSymbolsMap = new HashMap<>();

		for (String symbol : bullishSymbols) {
			for (StrategyConfig strategy : applicableStrategies) {
				if (strategyCheckerHelper.isSymbolBullishForStrategy(symbol, strategy, getSelectedBullishTimeframes(),
						currentWatchlist)) {
					strategyToSymbolsMap.computeIfAbsent(strategy.getName(), k -> new HashSet<>()).add(symbol);
				}
			}
		}

		// Check which strategies should trigger alarms
		List<String> strategiesToAlarm = new ArrayList<>();
		Set<String> symbolsToAlarm = new HashSet<>();

		for (StrategyConfig strategy : applicableStrategies) {
			Set<String> strategySymbols = strategyToSymbolsMap.getOrDefault(strategy.getName(), new HashSet<>());

			if (!strategySymbols.isEmpty()) {
				boolean shouldStrategyAlarm = shouldStrategyTriggerAlarm(strategy);

				if (shouldStrategyAlarm) {
					strategiesToAlarm.add(strategy.getName());
					symbolsToAlarm.addAll(strategySymbols);

					logToConsole("🚨 Strategy '" + strategy.getName() + "' triggered alarm for symbols: "
							+ String.join(", ", strategySymbols));
				} else {
					logToConsole("📊 Strategy '" + strategy.getName() + "' found bullish symbols but alarms disabled: "
							+ String.join(", ", strategySymbols));
				}
			}
		}

		// Trigger alarm if any strategy requires it
		if (!strategiesToAlarm.isEmpty() && !symbolsToAlarm.isEmpty()) {
			String alertMessage = createEnhancedAlertMessage(strategiesToAlarm, symbolsToAlarm);
			triggerBullishAlert(alertMessage);
		} else {
			stopBuzzer();
			logToConsole("📊 Bullish symbols found but no strategies require alarms");
		}
	}

	/**
	 * Check if a strategy should trigger an alarm based on its settings and global
	 * settings
	 */
	private boolean shouldStrategyTriggerAlarm(StrategyConfig strategy) {
		// Strategy must have alarm enabled
		if (!strategy.isAlarmEnabled()) {
			return false;
		}

		// Global alarm must be active
		if (!isAlarmActive || !alarmOnCheckbox.isSelected()) {
			return false;
		}

		// Alert manager must be available
		if (alertManager == null) {
			logToConsole("⚠️ AlertManager not available for strategy: " + strategy.getName());
			return false;
		}

		return true;
	}

	/**
	 * Create detailed alert message showing which strategies triggered the alarm
	 */
	private String createEnhancedAlertMessage(List<String> strategiesToAlarm, Set<String> symbolsToAlarm) {
		StringBuilder message = new StringBuilder();
		message.append("🎯 BULLISH ALERT - Strategies: ").append(String.join(", ", strategiesToAlarm));
		message.append(" | Symbols: ").append(String.join(", ", symbolsToAlarm));
		return message.toString();
	}

	/**
	 * Trigger the actual bullish alert
	 */
	private void triggerBullishAlert(String alertMessage) {
		logToConsole(alertMessage);
		startBuzzAlert(alertMessage);
	}

	/**
	 * Get visible symbols excluding hidden ones and exceptions
	 */
	private List<String> getVisibleSymbols(Set<String> excludedSymbols) {
		List<String> visibleSymbols = new ArrayList<>();

		for (Object rowObj : tableModel.getDataVector()) {
			@SuppressWarnings("unchecked")
			Vector<Object> row = (Vector<Object>) rowObj;
			String symbol = (String) row.get(1);

			if (tableModel.isSymbolHidden(symbol) || excludedSymbols.contains(symbol)) {
				continue;
			}
			visibleSymbols.add(symbol);
		}

		return visibleSymbols;
	}

	/**
	 * Get selected timeframes from bullish timeframe checkboxes
	 */
	public Set<String> getSelectedBullishTimeframes() {
		Set<String> selectedTimeframes = new HashSet<>();
		for (String tf : allTimeframes) {
			if (bullishTimeframeCheckboxes.get(tf).isSelected()) {
				selectedTimeframes.add(tf);
			}
		}
		return selectedTimeframes;
	}

	/**
	 * NEW: Check volume alerts for all symbols in the table (not just bullish ones)
	 */
	private List<String> checkVolumeAlertsForAllSymbols(Set<String> excludedSymbols) {
		List<String> allSymbols = new ArrayList<>();

		for (Object rowObj : tableModel.getDataVector()) {
			@SuppressWarnings("unchecked")
			Vector<Object> row = (Vector<Object>) rowObj;
			String symbol = (String) row.get(1);

			if (tableModel.isSymbolHidden(symbol) || excludedSymbols.contains(symbol)) {
				continue;
			}
			allSymbols.add(symbol);
		}

		// Use the volume analyzer to filter symbols with volume spikes
		return VolumeAlertAnalyser.analyze(allSymbols, volumeAlertConfig, priceDataMap);
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

	private boolean shouldSkipColumn(String colName) {

		List<String> datacolumns = new ArrayList<>(Arrays.asList("indexes", "postmarketClose", "premarketChange",
				"changeFromOpen", "percentChange", "postmarketChange", "gap", "premarketVolume", "currentVolume",
				"postmarketVolume", "previousVolume", "prevLastDayPrice", "averageVol", "analystRating",
				"premarketHigh", "high", "postmarketHigh", "premarketLow", "low", "postmarketLow"));

		return datacolumns.contains(colName) || colName.contains(BREAKOUT_COUNT_COLUMN_PATTERN)
				|| colName.contains(ZSCORE_COLUMN_PATTERN)
				|| colName.contains(MovingAverageTargetValueStrategy.MATV_STRATEGY_NAME_CONSTANT)
				|| colName.contains("VOLUMEMA") || colName.contains("Volume") || colName.contains("Percentile");
	}

	public void startBuzzAlert(String alertMessage) {
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

	private void selectAllRows(boolean selected) {
		FilterableTableModel model = (FilterableTableModel) dashboardTable.getModel();
		Vector<?> rawData = model.getDataVector();

		for (int i = 0; i < rawData.size(); i++) {
			@SuppressWarnings("unchecked")
			Vector<Object> row = (Vector<Object>) rawData.get(i);
			row.set(0, selected); // Column 0 is the checkbox column
		}

		model.fireTableDataChanged();
		logToConsole((selected ? "Selected" : "Deselected") + " all rows");
	}

	private void listVisibleSymbols() {
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
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

		List<String> desiredSymbols = new ArrayList<String>();

		if (visibleSymbols.isEmpty()) {
			sb.append("No visible symbols.");
		} else {
			visibleSymbols.sort(String::compareTo);
			for (String symbol : visibleSymbols) {

				sb.append(symbol).append(",");

				/*
				 * desiredSymbols.add(symbol);
				 * 
				 * PriceData priceData = priceDataMap.get(symbol);
				 * 
				 * String movingAverageTargetVal = priceData.getResults().entrySet().stream()
				 * .filter(entry -> entry.getValue().getMovingAverageTargetValue() != null) // ✅
				 * skip nulls .map(entry -> " " + entry.getKey() + " matv: " +
				 * entry.getValue().getMovingAverageTargetValue() + ", %tp: " +
				 * entry.getValue().getMovingAverageTargetValuePercentile())
				 * .collect(Collectors.joining(" | "));
				 * 
				 * if (!movingAverageTargetVal.isEmpty())
				 * sb2.append(symbol).append(movingAverageTargetVal).append("\n");
				 */
			}
		}

		logToConsole(sb.toString());
		logToConsole("===================================================");
		logToConsole(sb2.toString());

		/*
		 * ConcurrentHashMap<String, PriceData> filteredPriceDataMap =
		 * priceDataMap.entrySet().parallelStream() .filter(entry -> entry.getKey() !=
		 * null && entry.getValue() != null) .filter(entry ->
		 * desiredSymbols.contains(entry.getKey())).collect(Collectors.toMap(Map.Entry::
		 * getKey, Map.Entry::getValue, (oldVal, newVal) -> oldVal,
		 * ConcurrentHashMap::new));
		 */

		zScoreAlertManager.listZScoreResults();

		// todo: Z SCORE
		/*
		 * ZScoreCalculator calculator = new ZScoreCalculator(); Map<String, Double>
		 * zScores = calculator.calculateZScores(filteredPriceDataMap);
		 * 
		 * // Sort by Z-score descending (highest first) List<Map.Entry<String, Double>>
		 * sortedEntries = new ArrayList<>(zScores.entrySet()); sortedEntries.sort((a,
		 * b) -> Double.compare(b.getValue(), a.getValue())); // Descending order
		 * 
		 * // Print ranked results
		 * logToConsole("📊 Ranked Stock Candidates (Higher Z-Score = Better)");
		 * logToConsole("==================================================="); for (int
		 * i = 0; i < sortedEntries.size(); i++) { Map.Entry<String, Double> entry =
		 * sortedEntries.get(i); logToConsole(String.format("%2d. %-6s: %.2f%n", i + 1,
		 * entry.getKey(), entry.getValue())); }
		 * 
		 * // Alternatively, with more formatting:
		 * logToConsole("\n🎯 Top 10 Candidates:");
		 * logToConsole("Rank | Symbol | Z-Score");
		 * logToConsole("-----|--------|---------"); for (int i = 0; i < Math.min(10,
		 * sortedEntries.size()); i++) { Map.Entry<String, Double> entry =
		 * sortedEntries.get(i); logToConsole(String.format("%4d | %-6s | %6.2f%n", i +
		 * 1, entry.getKey(), entry.getValue())); }
		 */
	}

	// columns
	private boolean isColumnVisible(String columnName) {
		for (int i = 0; i < tableModel.getColumnCount(); i++) {
			if (tableModel.getColumnName(i).equals(columnName)) {
				return true;
			}
		}
		return false;
	}

	private void setColumnVisibility(JTable table, int columnIndex, boolean visible) {
		TableColumn column = table.getColumnModel().getColumn(columnIndex);
		if (visible) {
			column.setMinWidth(50); // Minimum reasonable width
			column.setPreferredWidth(100); // Default width
			column.setMaxWidth(Integer.MAX_VALUE);
		} else {
			column.setMinWidth(0);
			column.setPreferredWidth(0);
			column.setMaxWidth(0);
		}
	}

	private void updatePriceDataColumns(Map<String, JCheckBox> priceDataCheckboxes) {
		SwingUtilities.invokeLater(() -> {
			try {

				TableRowSorter<FilterableTableModel> sorter = (TableRowSorter<FilterableTableModel>) dashboardTable
						.getRowSorter();
				List<? extends RowSorter.SortKey> sortKeys = sorter != null ? sorter.getSortKeys() : null;

				// 1. Get current data and column structure
				Vector<Vector<Object>> oldData = new Vector<>();
				Vector<String> oldColumnNames = new Vector<>();

				for (int i = 0; i < tableModel.getRowCount(); i++) {
					oldData.add(new Vector<>(tableModel.getDataVector().get(i)));
				}
				for (int i = 0; i < tableModel.getColumnCount(); i++) {
					oldColumnNames.add(tableModel.getColumnName(i));
				}

				// 2. Create new column structure
				Vector<String> newColumnNames = new Vector<>();

				// Add basic columns (0-5)
				for (int i = 0; i < 6; i++) {
					newColumnNames.add(oldColumnNames.get(i));
				}

				// Add selected PriceData columns
				for (Map.Entry<String, JCheckBox> entry : priceDataCheckboxes.entrySet()) {
					if (entry.getValue().isSelected() && isPriceDataColumn(entry.getKey())) {
						newColumnNames.add(entry.getKey());
					}
				}

				// Add all indicator columns (preserve them)
				for (int i = 6; i < oldColumnNames.size(); i++) {
					String colName = oldColumnNames.get(i);
					if (!isPriceDataColumn(colName) && !newColumnNames.contains(colName)) {
						newColumnNames.add(colName);
					}
				}

				// 3. Create new model with updated columns
				FilterableTableModel newModel = new FilterableTableModel(newColumnNames.toArray(new String[0]), 0,
						this);

				// 4. Transfer all data
				for (Vector<Object> oldRow : oldData) {
					Vector<Object> newRow = new Vector<>();

					// Add basic columns (0-5)
					for (int i = 0; i < 6; i++) {
						newRow.add(oldRow.get(i));
					}

					// Add PriceData columns
					for (int i = 6; i < newColumnNames.size(); i++) {
						String colName = newColumnNames.get(i);
						Object value = "N/A"; // Default

						// Find matching column in old data
						int oldColIndex = oldColumnNames.indexOf(colName);
						if (oldColIndex >= 0 && oldColIndex < oldRow.size()) {
							value = oldRow.get(oldColIndex);
						}
						newRow.add(value);
					}
					newModel.addRow(newRow);
				}

				// 5. Update the table
				tableModel = newModel;
				dashboardTable.setModel(tableModel);

				updateTableColumnWidths();

				// 6. Refresh data to repopulate indicators
				// refreshData();

				if (sortKeys != null && !sortKeys.isEmpty()) {
					dashboardTable.setRowSorter(new TableRowSorter<>(tableModel));
					dashboardTable.getRowSorter().setSortKeys(sortKeys);
				}

				dashboardTable.repaint();

			} catch (Exception e) {
				logToConsole("Error updating columns: " + e.getMessage());
				e.printStackTrace();
			}
		});
	}

	public void updateTableRenderers() {
		// Reset all renderers first
		for (int i = 0; i < dashboardTable.getColumnCount(); i++) {
			TableColumn col = dashboardTable.getColumnModel().getColumn(i);
			col.setCellRenderer(null); // Clear existing
		}

		// Apply default renderers
		dashboardTable.setDefaultRenderer(Double.class, new DoubleCellRenderer());
		dashboardTable.setDefaultRenderer(Long.class, new LongCellRenderer());
		dashboardTable.setDefaultRenderer(BreakoutValue.class, new BreakoutValueRenderer());

		// Ensure indicatorColumnIndices is initialized
		if (indicatorColumnIndices == null) {
			indicatorColumnIndices = new ArrayList<>();
		} else {
			indicatorColumnIndices.clear();
		}

		// Apply StatusCellRenderer to indicator columns
		for (int i = 0; i < dashboardTable.getColumnCount(); i++) {
			String colName = dashboardTable.getColumnName(i);
			if (colName != null && !colName.isEmpty()) {
				// Check if column name contains any indicator
				if (Arrays.stream(allIndicators).anyMatch(ind -> ind != null && colName.contains(ind))) {
					dashboardTable.getColumnModel().getColumn(i).setCellRenderer(new StatusCellRenderer());
					indicatorColumnIndices.add(i);
				}
			}
		}

		// Apply numeric renderers to Z-Score columns using helper
		for (int i = 0; i < dashboardTable.getColumnCount(); i++) {
			String colName = dashboardTable.getColumnName(i);
			if (zScoreColumnHelper.isZScoreColumn(colName)) {
				if (zScoreColumnHelper.isRankColumn(colName)) {
					dashboardTable.getColumnModel().getColumn(i).setCellRenderer(new LongCellRenderer());
				} else if (zScoreColumnHelper.isZScoreValueColumn(colName)) {
					dashboardTable.getColumnModel().getColumn(i).setCellRenderer(new DoubleCellRenderer());
				}
			}
		}

		columnsInitialized = true;
		dashboardTable.revalidate();
		dashboardTable.repaint();
	}

	private int findColumnIndex(String colName) {
		for (int i = 0; i < tableModel.getColumnCount(); i++) {
			if (colName.equals(tableModel.getColumnName(i))) {
				return i;
			}
		}
		return -1;
	}

	private void openNonBullishFilterWindow() {
		JFrame filterWindow = new JFrame("Non-Bullish Filter");
		filterWindow.setSize(450, 500);
		filterWindow.setLayout(new BorderLayout());
		filterWindow.setLocationRelativeTo(this);

		// Main panel with scroll
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		// Timeframe selection panel
		JPanel timeframePanel = new JPanel(new GridLayout(0, 2));
		timeframePanel.setBorder(BorderFactory.createTitledBorder("Select Timeframes"));

		Map<String, JCheckBox> timeframeCheckboxes = new HashMap<>();
		for (String timeframe : allTimeframes) {
			JCheckBox cb = new JCheckBox(timeframe, bullishTimeframeCheckboxes.get(timeframe).isSelected());
			timeframeCheckboxes.put(timeframe, cb);
			timeframePanel.add(cb);
		}

		// Strategy selection panel - ALWAYS REFRESH when window opens
		JPanel strategyPanel = new JPanel(new BorderLayout());
		strategyPanel.setBorder(BorderFactory.createTitledBorder("Select Strategies (Multiple Selection)"));

		// Create strategy list model that will be populated
		DefaultListModel<String> strategyListModel = new DefaultListModel<>();
		JList<String> strategyList = new JList<>(strategyListModel);
		strategyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		// Refresh strategies from IndicatorsManagementApp
		refreshStrategyList(strategyListModel);

		JScrollPane strategyScrollPane = new JScrollPane(strategyList);
		strategyScrollPane.setPreferredSize(new Dimension(400, 120));
		strategyPanel.add(strategyScrollPane, BorderLayout.CENTER);

		// Refresh button for strategies
		JPanel strategyButtonPanel = new JPanel(new FlowLayout());
		JButton refreshStrategiesButton = new JButton("Refresh Strategies");
		JButton selectAllStrategiesButton = new JButton("Select All");
		JButton deselectAllStrategiesButton = new JButton("Deselect All");

		refreshStrategiesButton.addActionListener(e -> {
			refreshStrategyList(strategyListModel);
			strategyList.repaint();
		});

		selectAllStrategiesButton.addActionListener(e -> {
			strategyList.setSelectionInterval(0, strategyListModel.getSize() - 1);
		});

		deselectAllStrategiesButton.addActionListener(e -> {
			strategyList.clearSelection();
		});

		strategyButtonPanel.add(refreshStrategiesButton);
		strategyButtonPanel.add(selectAllStrategiesButton);
		strategyButtonPanel.add(deselectAllStrategiesButton);
		strategyPanel.add(strategyButtonPanel, BorderLayout.SOUTH);

		// Add panels to main panel
		mainPanel.add(timeframePanel);
		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(strategyPanel);

		JScrollPane scrollPane = new JScrollPane(mainPanel);
		filterWindow.add(scrollPane, BorderLayout.CENTER);

		// Button panel
		JPanel buttonPanel = new JPanel(new FlowLayout());
		JButton applyButton = new JButton("Apply Filter");
		JButton cancelButton = new JButton("Cancel");

		applyButton.addActionListener(ae -> {
			// Get selected timeframes
			Set<String> selectedTimeframes = new HashSet<>();
			for (String timeframe : allTimeframes) {
				if (timeframeCheckboxes.get(timeframe).isSelected()) {
					selectedTimeframes.add(timeframe);
				}
			}

			// Get selected strategies
			List<String> selectedStrategyNames = strategyList.getSelectedValuesList();

			// Apply filtering using helper
			applyNonBullishFilterWithHelper(selectedTimeframes, selectedStrategyNames);
			filterWindow.dispose();
		});

		cancelButton.addActionListener(e -> filterWindow.dispose());

		buttonPanel.add(applyButton);
		buttonPanel.add(cancelButton);
		filterWindow.add(buttonPanel, BorderLayout.SOUTH);

		filterWindow.setVisible(true);
	}

	/**
	 * Refresh strategy list from IndicatorsManagementApp
	 */
	private void refreshStrategyList(DefaultListModel<String> strategyListModel) {
		strategyListModel.clear();

		if (indicatorsManagementApp != null) {
			try {
				strategyCheckerHelper.setIndicatorsManagementApp(indicatorsManagementApp);

				List<StrategyConfig> enabledStrategies = strategyCheckerHelper.getEnabledStrategies();

				for (StrategyConfig strategy : enabledStrategies) {
					String strategyDisplay = strategy.getName();// String.format("%s (%d timeframes)",
																// strategy.getName(), strategy.getTimeframes().size());
					strategyListModel.addElement(strategyDisplay);
				}

				if (enabledStrategies.isEmpty()) {
					strategyListModel.addElement("No enabled strategies found");
				} else {
					logToConsole("Refreshed strategies: " + enabledStrategies.size() + " enabled strategies loaded");
				}

			} catch (Exception e) {
				logToConsole("Error refreshing strategies: " + e.getMessage());
				strategyListModel.addElement("Error loading strategies");
			}
		} else {
			strategyListModel.addElement("Indicator Management not available");
		}
	}

	/**
	 * Apply non-bullish filter using AND condition - symbol is non-bullish only if
	 * it fails ALL strategies
	 */
	private void applyNonBullishFilterWithHelper(Set<String> selectedTimeframes, List<String> selectedStrategyNames) {
		Vector<?> rawData = tableModel.getDataVector();

		// Initialize StrategyExecutionService with dashboard configuration
		if (strategyExecutionService == null) {
			strategyExecutionService = createStrategyExecutionService();
		}
		// checkAndInstantiateIndicatorsManagementApp();

		// strategyCheckerHelper.setIndicatorsManagementApp(indicatorsManagementApp);
		strategyCheckerHelper.setPriceDataMap(priceDataMap);

		// Get visible symbols first
		List<String> visibleSymbols = getVisibleSymbolsForNonBullishCheck(rawData);

		if (visibleSymbols.isEmpty()) {
			logToConsole("No visible symbols to check for non-bullish conditions");
			return;
		}

		// Convert strategy names to StrategyConfig objects using helper
		List<StrategyConfig> selectedStrategies = new ArrayList<>();
		for (String strategyName : selectedStrategyNames) {
			StrategyConfig strategy = strategyCheckerHelper.getStrategyByName(strategyName);
			if (strategy != null) {
				selectedStrategies.add(strategy);
			}
		}

		// Get current watchlist
		String currentWatchlist = (String) watchlistComboBox.getSelectedItem();

		logToConsole("🎯 APPLYING NON-BULLISH FILTER (AND Condition)");
		logToConsole("   Symbol must be non-bullish for ALL selected strategies");
		logToConsole("   Strategies: " + selectedStrategies.size());
		logToConsole("   Timeframes: " + selectedTimeframes.size());
		logToConsole("   Watchlist: " + currentWatchlist);
		logToConsole("   Visible symbols: " + visibleSymbols.size());

		// Get non-bullish symbols per strategy
		Map<String, Set<String>> nonBullishPerStrategy = strategyCheckerHelper.getNonBullishSymbolsIntersection(
				visibleSymbols, selectedStrategies, selectedTimeframes, currentWatchlist);

		// Get intersection (symbols that are non-bullish for ALL strategies)
		Set<String> intersectionNonBullishSymbols = strategyCheckerHelper
				.getIntersectionNonBullishSymbols(nonBullishPerStrategy, selectedStrategies);

		// Print detailed results
		strategyCheckerHelper.printIntersectionNonBullishResults(nonBullishPerStrategy, intersectionNonBullishSymbols,
				selectedStrategies, visibleSymbols.size());

		// Apply volume alert check to intersection symbols
		Set<String> volumeNonBullishSymbols = checkVolumeAlertsForNonBullishSymbols(intersectionNonBullishSymbols);

		// Final non-bullish symbols = intersection symbols that also fail volume check
		// (if enabled)
		Set<String> finalNonBullishSymbols = new HashSet<>(intersectionNonBullishSymbols);
		if (volumeAlertConfig.isEnabled() && volumeAlertConfig.isVolume20MAEnabled()) {
			finalNonBullishSymbols.addAll(volumeNonBullishSymbols);
			logToConsole("\n🔊 Volume alerts applied to intersection symbols");
		}

		// Update table checkboxes based on AND condition
		updateNonBullishCheckboxes(rawData, finalNonBullishSymbols);

		tableModel.fireTableDataChanged();

		// Log final results
		logToConsole("\n✅ Non-bullish filter applied successfully (AND Condition)");
		logToConsole("   Intersection symbols: " + intersectionNonBullishSymbols.size());
		logToConsole("   Final non-bullish symbols: " + finalNonBullishSymbols.size());
		logToConsole("   Symbols checked: " + visibleSymbols.size());

		if (!finalNonBullishSymbols.isEmpty()) {
			logToConsole("   Non-bullish for ALL strategies: " + String.join(", ", finalNonBullishSymbols));
		}
	}

	/**
	 * Get visible symbols for non-bullish checking
	 */
	private List<String> getVisibleSymbolsForNonBullishCheck(Vector<?> rawData) {
		List<String> visibleSymbols = new ArrayList<>();
		for (int i = 0; i < rawData.size(); i++) {
			@SuppressWarnings("unchecked")
			Vector<Object> row = (Vector<Object>) rawData.get(i);
			String symbol = (String) row.get(1);

			if (!tableModel.isSymbolHidden(symbol)) {
				visibleSymbols.add(symbol);
			}
		}
		return visibleSymbols;
	}

	/**
	 * Check volume alerts for non-bullish symbols
	 */
	private Set<String> checkVolumeAlertsForNonBullishSymbols(Set<String> symbols) {
		Set<String> volumeNonBullishSymbols = new HashSet<>();

		if (volumeAlertConfig.isEnabled() && volumeAlertConfig.isVolume20MAEnabled()) {
			for (String symbol : symbols) {
				if (!strategyCheckerHelper.checkVolumeAlerts(symbol, volumeAlertConfig)) {
					volumeNonBullishSymbols.add(symbol);
					logToConsole("❌ " + symbol + " failed volume alert check");
				}
			}
		}

		return volumeNonBullishSymbols;
	}

	/**
	 * Update checkboxes based on non-bullish symbols
	 */
	private void updateNonBullishCheckboxes(Vector<?> rawData, Set<String> nonBullishSymbols) {
		int updatedRows = 0;

		for (int i = 0; i < rawData.size(); i++) {
			@SuppressWarnings("unchecked")
			Vector<Object> row = (Vector<Object>) rawData.get(i);
			String symbol = (String) row.get(1);

			if (tableModel.isSymbolHidden(symbol)) {
				continue;
			}

			// Check if symbol is in non-bullish set (OR condition)
			boolean isNonBullish = nonBullishSymbols.contains(symbol);

			// Only check the checkbox if symbol is non-bullish
			if (isNonBullish) {
				row.set(0, true); // Check the checkbox
				updatedRows++;
			} else {
				row.set(0, false); // Uncheck if not non-bullish
			}
		}

		logToConsole("   Updated checkboxes: " + updatedRows + " rows");
	}

	private void setupCellRenderers() {
		// Set default renderer for all cells
		dashboardTable.setDefaultRenderer(Object.class, new StatusCellRenderer());

		// Specialized renderers for specific types
		dashboardTable.setDefaultRenderer(Double.class, new DoubleCellRenderer());
		dashboardTable.setDefaultRenderer(Long.class, new LongCellRenderer());
		dashboardTable.setDefaultRenderer(BreakoutValue.class, new BreakoutValueRenderer());

		// Set up checkbox column renderer and editor
		TableColumn checkboxColumn = dashboardTable.getColumnModel().getColumn(0);
		checkboxColumn.setCellRenderer(new DefaultTableCellRenderer() {
			private final JCheckBox checkBox = new JCheckBox();

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				checkBox.setSelected(value != null && (Boolean) value);
				checkBox.setHorizontalAlignment(SwingConstants.CENTER);
				checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
				return checkBox;
			}
		});
		checkboxColumn.setCellEditor(new DefaultCellEditor(new JCheckBox()));

		// Set alignment for other columns
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

		DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
		leftRenderer.setHorizontalAlignment(SwingConstants.LEFT);

		for (int i = 0; i < dashboardTable.getColumnCount(); i++) {
			if (i == 1) { // Symbol column
				dashboardTable.getColumnModel().getColumn(i).setCellRenderer(leftRenderer);
			} else if (i != 0) { // Skip checkbox column
				dashboardTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
			}
		}

		dashboardTable.setDefaultRenderer(Double.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				// Highlight target values
				String colName = table.getColumnName(column);
				if (colName != null && colName.contains(MovingAverageTargetValueStrategy.MATV_STRATEGY_NAME_CONSTANT)) {
					c.setForeground(Color.BLUE);
					c.setFont(c.getFont().deriveFont(Font.BOLD));
				}
				return c;
			}
		});
	}

	private boolean isPriceDataColumn(String colName) {
		try {
			// Check if this is a field in PriceData class
			PriceData.class.getDeclaredField(colName);
			return true;
		} catch (NoSuchFieldException e) {
			return false;
		}
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
		if (currentTimeRadio.isSelected()) {
			timeRangeLabel.setText("Time Range: Current Time");
		} else {
			if (startDatePicker.getDate() != null && endDatePicker.getDate() != null) {
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

				String startDate = dateFormat.format(startDatePicker.getDate());
				String endDate = dateFormat.format(endDatePicker.getDate());

				// Get time from class variable comboboxes
				String startTime = startHourCombo.getSelectedItem() + ":" + startMinuteCombo.getSelectedItem();
				String endTime = endHourCombo.getSelectedItem() + ":" + endMinuteCombo.getSelectedItem();

				timeRangeLabel
						.setText(String.format("Time Range: %s %s to %s %s", startDate, startTime, endDate, endTime));
			} else {
				timeRangeLabel.setText("Time Range: Custom (dates not set)");
			}
		}
	}

	private JSpinner getStartTimeSpinner() {
		return (JSpinner) timeRangePanel.getComponent(7);
	}

	private JSpinner getEndTimeSpinner() {
		return (JSpinner) timeRangePanel.getComponent(9);
	}

	public synchronized void logToConsole(String message) {
		String timestampedMessage = String.format("[%s] %s%n", ZonedDateTime.now(ZoneId.of("America/Los_Angeles"))
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), message);

		// Append to consoleLogBuffer
		consoleLogBuffer.append(timestampedMessage);

		// Update UI (must be on EDT)
		SwingUtilities.invokeLater(() -> {
			consoleArea.append(timestampedMessage);
			consoleArea.setCaretPosition(consoleArea.getDocument().getLength()); // Scroll to bottom
		});
	}

	private void addNewWatchlist() {
		String name = JOptionPane.showInputDialog(this, "Enter new watchlist name:");
		if (name != null && !name.trim().isEmpty()) {
			if (!watchlists.containsKey(name)) {
				watchlists.put(name, new WatchlistData());
				watchlistComboBox.addItem(name);
				watchlistComboBox.setSelectedItem(name);
				currentWatchlist = name;
			} else {
				JOptionPane.showMessageDialog(this, "Watchlist already exists!", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void editCurrentWatchlist() {

		WatchlistData currentWatchListData = watchlists.get(currentWatchlist);

		Set<String> currentSymbols = currentWatchListData.getSymbols();

		String symbolsText = String.join(",", currentSymbols);

		String newSymbols = JOptionPane.showInputDialog(this, "Edit symbols (comma separated):", symbolsText);

		if (newSymbols != null) {
			// List<String> updatedSymbols = Arrays.asList(newSymbols.split("\\s*,\\s*"));
			// currentWatchListData.setSymbols(new HashSet<>(updatedSymbols));
			// watchlists.put(currentWatchlist, currentWatchListData);
			// refreshData();

			Set<String> updatedSymbols = new HashSet<>(Arrays.asList(newSymbols.split("\\s*,\\s*")));
			currentWatchListData.setSymbols(updatedSymbols);
			// Note: primary symbol remains unchanged unless you want to add logic to update
			// it
			refreshData();
		}

		/*
		 * 
		 * // FIX: Use the new constructor with WatchlistData WatchlistManagerDialog
		 * dialog = new WatchlistManagerDialog(this, watchlists);
		 * dialog.setVisible(true);
		 * 
		 * if (dialog.isSaved()) { watchlists.clear();
		 * watchlists.putAll(dialog.getWatchlists()); // This will now work
		 * refreshData(); }
		 */
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
		if (dashboardTable == null || tableModel == null) {
			return;
		}

		SwingUtilities.invokeLater(() -> {
			try {
				Set<String> symbolsToToggle = new HashSet<>();
				FilterableTableModel model = (FilterableTableModel) tableModel;

				// Check all rows in the model (not the view)
				for (int modelRow = 0; modelRow < model.getRowCount(); modelRow++) {
					Boolean isSelected = (Boolean) model.getValueAt(modelRow, 0);
					String symbol = (String) model.getValueAt(modelRow, 1);

					if (isSelected != null && isSelected && symbol != null) {
						symbolsToToggle.add(symbol);
						// Uncheck the checkbox
						model.setValueAt(Boolean.FALSE, modelRow, 0);
					}
				}

				if (!symbolsToToggle.isEmpty()) {
					model.toggleSymbolsVisibility(symbolsToToggle);
					if (dashboardTable.getRowSorter() != null) {
						dashboardTable.getRowSorter().allRowsChanged();
					}
					dashboardTable.repaint();
					logToConsole("Toggled visibility for " + symbolsToToggle.size() + " symbols");
				}
			} catch (Exception e) {
				logToConsole("Error in toggleSelectedRowsVisibility: " + e.getMessage());
			}
		});
	}

	private String[] createColumnNames() {
		// Start with the basic columns that are always present
		List<String> columns = new ArrayList<>(
				Arrays.asList("Select", "Symbol", "Latest Price", "Prev Vol", "Current Vol", "% Change"));

		// Add existing PriceData columns that are currently in the table
		if (tableModel != null) {
			for (int i = 6; i < tableModel.getColumnCount(); i++) {
				String colName = tableModel.getColumnName(i);
				// Only add non-indicator columns (PriceData and Z-Score columns)
				if (isPriceDataColumn(colName)) {
					if (!columns.contains(colName)) {
						columns.add(colName);
					}
				}
			}
		}

		// Add indicator columns for each timeframe
		for (String tf : allTimeframes) {
			Set<String> indicators = timeframeIndicators.get(tf);
			if (indicators.isEmpty())
				continue;

			for (String ind : allIndicators) {
				if (indicators.contains(ind)) {
					String indicatorColumnName = tf + " " + ind;
					if (!columns.contains(indicatorColumnName)) {
						columns.add(indicatorColumnName);
					}
				}
			}

			// Add custom indicator columns for this timeframe
			if (customIndicatorCombinations.containsKey(tf)) {
				Set<String> customIndicators = customIndicatorCombinations.get(tf);
				for (String customIndicatorName : customIndicators) {
					String customColumnName = tf + " " + customIndicatorName;
					if (!columns.contains(customColumnName)) {
						columns.add(customColumnName);
					}
				}
			}
		}

		// Add existing PriceData columns that are currently in the table
		if (tableModel != null) {
			for (int i = 6; i < tableModel.getColumnCount(); i++) {
				String colName = tableModel.getColumnName(i);
				// Only add non-indicator columns (PriceData and Z-Score columns)
				if (zScoreColumnHelper.isZScoreColumn(colName)) {
					if (!columns.contains(colName)) {
						columns.add(colName);
					}
				}
			}
		}

		return columns.toArray(new String[0]);
	}

	private void updateTableColumns() {
		// Backup ALL current data before changing columns
		Vector<Vector<Object>> oldData = new Vector<>();
		Vector<String> oldColumnNames = new Vector<>();

		if (tableModel != null && tableModel.getDataVector() != null) {
			for (int i = 0; i < tableModel.getRowCount(); i++) {
				oldData.add(new Vector<>(tableModel.getDataVector().get(i)));
			}
			for (int i = 0; i < tableModel.getColumnCount(); i++) {
				oldColumnNames.add(tableModel.getColumnName(i));
			}
		}

		// Create new column structure that includes preserved columns
		String[] newColumnNames = createColumnNames();

		// Create new model
		tableModel = new FilterableTableModel(newColumnNames, 0, this);
		dashboardTable.setModel(tableModel);

		// Transfer data from old structure to new structure
		for (Vector<Object> oldRow : oldData) {
			Vector<Object> newRow = new Vector<>();

			// For each column in the new structure, find matching data from old structure
			for (String newColName : newColumnNames) {
				Object value = "N/A"; // Default value

				// Find matching column in old data
				int oldColIndex = oldColumnNames.indexOf(newColName);
				if (oldColIndex >= 0 && oldColIndex < oldRow.size()) {
					value = oldRow.get(oldColIndex);
				}

				newRow.add(value);
			}
			tableModel.addRow(newRow);
		}

		// Reapply all table settings
		updateTableColumnWidths();
		updateTableRenderers();

		// Set up checkbox column
		dashboardTable.getColumnModel().getColumn(0).setMaxWidth(30);
		dashboardTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
		dashboardTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
			private final JCheckBox checkBox = new JCheckBox();

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				int modelRow = table.convertRowIndexToModel(row);
				Boolean checked = (Boolean) table.getModel().getValueAt(modelRow, column);

				checkBox.setSelected(checked != null && checked);
				checkBox.setHorizontalAlignment(SwingConstants.CENTER);
				checkBox.setOpaque(true);

				if (isSelected) {
					checkBox.setBackground(table.getSelectionBackground());
					checkBox.setForeground(table.getSelectionForeground());
				} else {
					checkBox.setBackground(table.getBackground());
					checkBox.setForeground(table.getForeground());
				}

				return checkBox;
			}
		});

		tableModel.fireTableStructureChanged();
		dashboardTable.revalidate();
		dashboardTable.repaint();

		logToConsole("Table columns updated. Preserved " + oldColumnNames.size() + " columns, new structure has "
				+ newColumnNames.length + " columns");
	}

	private boolean validateTimeRange() {
		if (customRangeRadio.isSelected()) {
			if (startDatePicker.getDate() == null || endDatePicker.getDate() == null) {
				JOptionPane.showMessageDialog(this, "Please select both start and end dates for custom range",
						"Time Range Error", JOptionPane.ERROR_MESSAGE);
				return false;
			}

			ZonedDateTime startTime = getStartDateTime();
			ZonedDateTime endTime = getEndDateTime();

			if (startTime.isAfter(endTime)) {
				JOptionPane.showMessageDialog(this, "Start time cannot be after end time", "Time Range Error",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}

			if (startTime.isAfter(ZonedDateTime.now())) {
				JOptionPane.showMessageDialog(this, "Start time cannot be in the future", "Time Range Error",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		return true;
	}

	private void refreshData() {

		// Validate time range first
		if (!validateTimeRange()) {
			return;
		}

		List<String> symbolsToProcess = new ArrayList<String>();

		// Log the time range being used
		ZonedDateTime startTime = getStartDateTime();
		ZonedDateTime endTime = getEndDateTime();

		logToConsole("Using time range: " + startTime + " to " + endTime);
		logToConsole("Time range mode: " + (currentTimeRadio.isSelected() ? "Current Time" : "Custom Range"));

		if (watchListException.contains(currentWatchlist) && currentWatchlist != "Default") {
			try {
				switch (currentWatchlist) {
				case "YahooDOW":
					priceDataMap = PriceDataMapper.mapByTicker(yahooQueryFinance.getDowJones());
					break;
				case "YahooNASDAQ":
					priceDataMap = PriceDataMapper.mapByTicker(yahooQueryFinance.getNasdaq());
					break;
				case "YahooSP500":
					priceDataMap = PriceDataMapper.mapByTicker(yahooQueryFinance.getSP500());
					break;
				case "YahooRussel2k":
					priceDataMap = PriceDataMapper.mapByTicker(yahooQueryFinance.getRussell2000());
					break;
				case "YahooPennyBy3MonVol":
					priceDataMap = PriceDataMapper.mapByTicker(yahooQueryFinance.getPennyStocksByVolume());
					break;
				case "YahooPennyByPercentChange":
					priceDataMap = PriceDataMapper.mapByTicker(yahooQueryFinance.getPennyStocksByPercentChange());
					break;
				case "TradingViewPennyStockPreMarket":
					priceDataMap = PriceDataMapper.mapByTicker(tradingViewService.getPennyStocksPreMarket());
					break;
				case "TradingViewPennyStockStandardMarket":
					priceDataMap = PriceDataMapper
							.mapByTicker(tradingViewService.getPennyStockStandardMarketStocksByVolume());
					break;
				case "TradingViewPennyStockPostMarket":
					priceDataMap = PriceDataMapper
							.mapByTicker(tradingViewService.getPennyStocksPostMarketByPostMarketVolume());
					break;
				case "TradingViewIndexStocksByPreMarketVolume":
					priceDataMap = PriceDataMapper
							.mapByTicker(tradingViewService.getIndexStocksMarketByPreMarketVolume());
					break;
				case "TradingViewIndexStocksByStandardMarketVolume":
					priceDataMap = PriceDataMapper
							.mapByTicker(tradingViewService.getIndexStocksMarketByStandardMarketVolume());
					break;
				case "TradingViewIndexStocksMarketByPostMarketVolume":
					priceDataMap = PriceDataMapper
							.mapByTicker(tradingViewService.getIndexStocksMarketByPostMarketVolume());
					break;
				}

				if (priceDataMap.size() > 0) {
					symbolsToProcess = PriceDataMapper.getSymbols(priceDataMap);
				}

				WatchlistData currentWatchlistData = watchlists.get(currentWatchlist);
				List<String> watchListTmp = new ArrayList<>(currentWatchlistData.getSymbols());

				for (String x : watchListTmp) {
					if (!symbolsToProcess.contains(x)) {
						symbolsToProcess.add(x);
					}
				}

			} catch (StockApiException e) {
				logToConsole("Error fetching refreshData" + e.getLocalizedMessage());
			}
		} else {

			WatchlistData currentWatchlistData = watchlists.get(currentWatchlist);
			symbolsToProcess = new ArrayList<>(currentWatchlistData.getSymbols());

		}

		if (symbolsToProcess == null || symbolsToProcess.isEmpty()) {
			logToConsole("No symbols in watchlist: " + currentWatchlist);
			return;
		}

		tableModel.setRowCount(0);

		FilterableTableModel model = (FilterableTableModel) dashboardTable.getModel();
		LinkedHashSet<String> visibleSymbols = new LinkedHashSet<>();

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

		// Get selected volume source
		String selectedDataSource = (String) dataSourceComboBox.getSelectedItem();
		String selectedVolumeSource = (String) volumeSourceCombo.getSelectedItem();
		StockDataProvider volumeProvider = getVolumeDataProvider(selectedVolumeSource, selectedDataSource);

		// Option 0

		// Create a CountDownLatch with count equal to number of symbols
		/*
		 * CountDownLatch latch = new CountDownLatch(visibleSymbols.size());
		 * ExecutorService executor =
		 * Executors.newFixedThreadPool(visibleSymbols.size()); List<Future<?>> futures
		 * = new ArrayList<>();
		 * 
		 * for (String symbol : visibleSymbols) {
		 * 
		 * futures.add(executor.submit(() -> { processSymbol(selectedDataSource,
		 * selectedVolumeSource, volumeProvider, latch, symbol); }));
		 * 
		 * }
		 * 
		 * // Wait for completion with timeout for (Future<?> future : futures) { try {
		 * future.get(30, TimeUnit.SECONDS); } catch (TimeoutException e) {
		 * future.cancel(true); logToConsole("Symbol processing timeout"); } catch
		 * (Exception e) { logToConsole("Error processing symbol: " + e.getMessage()); }
		 * }
		 * 
		 * // Shutdown executor after submitting all tasks executor.shutdown();
		 * 
		 * // Create a new thread to wait for completion and then check bullish symbols
		 * new Thread(() -> { try { // Wait for all tasks to complete (with timeout to
		 * prevent hanging) latch.await(60, TimeUnit.SECONDS);
		 * 
		 * // Check bullish symbols on the EDT once all updates are done
		 * SwingUtilities.invokeLater(() -> {
		 * 
		 * //Refresh Z-Score columns after all data is loaded refreshZScoreColumns();
		 * 
		 * checkAllAlerts();
		 * 
		 * // Save logs/reports if enabled if (enableSaveLogCheckbox.isSelected() ||
		 * enableLogReportsCheckbox.isSelected()) { if
		 * (enableSaveLogCheckbox.isSelected()) { saveConsoleLog(); } if
		 * (enableLogReportsCheckbox.isSelected()) { saveReportFiles(); } } }); } catch
		 * (InterruptedException e) {
		 * logToConsole("Error waiting for symbol updates to complete: " +
		 * e.getMessage()); } }).start();
		 */

		/**
		 * Option 1 // Create a CountDownLatch with count equal to number of symbols
		 * 
		 * // Use fixed thread pool with reasonable limit (8-16 threads) int threadCount
		 * = Math.min(visibleSymbols.size(), 12); // Cap at 12 threads max
		 * ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		 * CountDownLatch latch = new CountDownLatch(visibleSymbols.size());
		 * 
		 * // Track progress for better UX final int totalSymbols =
		 * visibleSymbols.size(); AtomicInteger processedCount = new AtomicInteger(0);
		 * 
		 * for (String symbol : visibleSymbols) { executor.submit(() -> { try { //
		 * Update progress int current = processedCount.incrementAndGet();
		 * SwingUtilities.invokeLater(() -> { statusLabel.setText(String.format("Status:
		 * Processing %d/%d symbols...", current, totalSymbols)); });
		 * 
		 * // Fetch analysis and price data LinkedHashMap<String, AnalysisResult>
		 * results = analyzeSymbol(symbol); PriceData priceData =
		 * fetchLatestPrice(symbol, results);
		 * 
		 * // Adjust volume data using setters if different provider if
		 * (!"Default".equals(selectedVolumeSource) &&
		 * !selectedVolumeSource.equals(selectedDataSource)) { PriceData volumePriceData
		 * = volumeProvider.getLatestPrice(symbol); priceData.previousVolume =
		 * volumePriceData.getPreviousVolume(); priceData.currentVolume =
		 * volumePriceData.getCurrentVolume(); }
		 * 
		 * SwingUtilities.invokeLater(() -> { updateOrAddSymbolRow(symbol, priceData,
		 * results); }); } catch (Exception e) { logToConsole("Error analyzing " +
		 * symbol + ": " + e.getMessage()); } finally { latch.countDown(); } }); }
		 * 
		 * // Shutdown executor and wait for completion in background thread
		 * executor.shutdown();
		 * 
		 * new Thread(() -> { try { // Wait for all tasks with timeout boolean completed
		 * = latch.await(90, TimeUnit.SECONDS);
		 * 
		 * if (!completed) { logToConsole("Warning: Some symbols timed out during
		 * processing"); }
		 * 
		 * SwingUtilities.invokeLater(() -> { refreshZScoreColumns(); checkAllAlerts();
		 * 
		 * // Save logs/reports if enabled if (enableSaveLogCheckbox.isSelected() ||
		 * enableLogReportsCheckbox.isSelected()) { if
		 * (enableSaveLogCheckbox.isSelected()) { saveConsoleLog(); } if
		 * (enableLogReportsCheckbox.isSelected()) { saveReportFiles(); } }
		 * 
		 * statusLabel.setText("Status: Refresh completed at " +
		 * LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
		 * 
		 * dashboardTable.revalidate(); dashboardTable.repaint(); }); } catch
		 * (InterruptedException e) { logToConsole("Refresh interrupted: " +
		 * e.getMessage()); } }).start();
		 */

		/** Option 2 **/
		// Use CompletableFuture with controlled parallelism
		/*
		 * int maxConcurrent = Math.min(visibleSymbols.size(), 20); // Max 8 concurrent
		 * calls SwingUtilities.invokeLater(() -> { List<CompletableFuture<Void>>
		 * futures = visibleSymbols.stream().map(symbol -> CompletableFuture.runAsync(
		 * () -> processSymbolWithTimeRange(symbol, selectedDataSource, volumeProvider,
		 * startTime, endTime), // processSymbol(symbol, selectedDataSource,
		 * volumeProvider),
		 * Executors.newFixedThreadPool(maxConcurrent))).collect(Collectors.toList());
		 * 
		 * // Wait for all completions
		 * 
		 * SwingUtilities.invokeLater(() -> { CompletableFuture<Void> allFutures =
		 * CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
		 * allFutures.thenRun(() -> { SwingUtilities.invokeLater(() -> {
		 * refreshZScoreColumns(); checkAllAlerts();
		 * statusLabel.setText("Status: Refresh completed"); });
		 * }).exceptionally(throwable -> {
		 * logToConsole("Refresh completed with errors: " + throwable.getMessage());
		 * return null; }); }); });
		 */
		// Submit all tasks
		
		long statNanoTime = System.nanoTime();
		
		List<CompletableFuture<Void>> futures = visibleSymbols.stream()
				.map(symbol -> CompletableFuture.runAsync(() -> processSymbolWithTimeRange(symbol, selectedDataSource,
						volumeProvider, startTime, endTime), executorService)) // Reuse the same pool
				.collect(Collectors.toList());

		// Handle completion
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRunAsync(() -> {
			SwingUtilities.invokeLater(() -> {
				
				long endNanoTime = System.nanoTime();
				
				long duration = endNanoTime - statNanoTime;
				
				System.out.println("CompletableFuture execution time "+ (duration / 1_000_000.0) + " milliseconds");
				
				logToConsole("CompletableFuture execution time for " + (duration / 1_000_000.0) + " milliseconds");
				
				
				long statNanoTime2 = System.nanoTime();
				
				refreshZScoreColumns();
				checkAllAlerts();
				
				duration = endNanoTime - statNanoTime2;
				
				System.out.println("CompletableFuture zscores execution time "+ (duration / 1_000_000.0) + " milliseconds");
				
				logToConsole("CompletableFuture zscores execution time for " + (duration / 1_000_000.0) + " milliseconds");
				statusLabel.setText("Status: Refresh completed");
				
				isProcessingUpdates = false;
			});
		}) // Ensure UI updates on EDT
				.exceptionally(throwable -> {
					SwingUtilities.invokeLater(() -> {
						logToConsole("Refresh completed with errors: " + throwable.getMessage());
						statusLabel.setText("Status: Error during refresh");
					});
					
					isProcessingUpdates = false;
					
					return null;
				});

		/*
		 * Option 4 CountDownLatch latch = new CountDownLatch(visibleSymbols.size());
		 * ExecutorService executor =
		 * Executors.newFixedThreadPool(visibleSymbols.size());
		 * 
		 * for (String symbol : visibleSymbols) { executor.execute(() -> {
		 * processSymbol(selectedDataSource, selectedVolumeSource, volumeProvider,
		 * latch, symbol); }); }
		 * 
		 * // Shutdown executor after submitting all tasks executor.shutdown();
		 * 
		 * // Create a new thread to wait for completion and then check bullish symbols
		 * new Thread(() -> { try { // Wait for all tasks to complete (with timeout to
		 * prevent hanging) latch.await(60, TimeUnit.SECONDS);
		 * 
		 * // Check bullish symbols on the EDT once all updates are done
		 * SwingUtilities.invokeLater(() -> {
		 * 
		 * //Refresh Z-Score columns after all data is loaded refreshZScoreColumns();
		 * 
		 * checkAllAlerts();
		 * 
		 * // Save logs/reports if enabled if (enableSaveLogCheckbox.isSelected() ||
		 * enableLogReportsCheckbox.isSelected()) { if
		 * (enableSaveLogCheckbox.isSelected()) { saveConsoleLog(); } if
		 * (enableLogReportsCheckbox.isSelected()) { saveReportFiles(); } } }); } catch
		 * (InterruptedException e) {
		 * logToConsole("Error waiting for symbol updates to complete: " +
		 * e.getMessage()); } }).start();
		 */

		// Rest of your existing code for checking hidden symbols...
		Set<String> symbolsToShow = new HashSet<>();
		for (int i = 0; i < model.getRowCount(); i++) {
			String symbol = (String) model.getValueAt(i, 1);
			if (model.isSymbolHidden(symbol)) {
				Long previousVolume = (Long) model.getValueAt(i, 3);
				Long currentVolume = (Long) model.getValueAt(i, 4);
				if (passesVolumeFilters(previousVolume, currentVolume)) {
					symbolsToShow.add(symbol);
				}
			}
		}

		if (!symbolsToShow.isEmpty()) {
			model.toggleSymbolsVisibility(symbolsToShow);
		}
	}

	private void processSymbolWithTimeRange(String symbol, String dataSource, StockDataProvider volumeProvider,
			ZonedDateTime startTime, ZonedDateTime endTime) {
		try {

			
			LinkedHashMap<String, AnalysisResult> results = analyzeSymbolWithTimeRange(symbol, startTime, endTime);
			
			logToConsole("Done fetching price data for " + symbol);
			// Re-compute custom indicators with the fetched price data
			
			

			try {
				if (currentTimeRadio.isSelected()) {
					PriceData priceData = fetchLatestPrice(symbol, results);

					if (!"Default".equals(volumeSourceCombo.getSelectedItem())
							&& !volumeSourceCombo.getSelectedItem().equals(dataSource)) {
						PriceData volumePriceData = volumeProvider.getLatestPrice(symbol);
						priceData.previousVolume = volumePriceData.getPreviousVolume();
						priceData.currentVolume = volumePriceData.getCurrentVolume();
					}

					
					
					SwingUtilities.invokeLater(() -> {
						
						
						
						LinkedHashMap<String, AnalysisResult> updatedResults = analyzeSymbolWithTimeRangeForCustomIndicators(
								symbol, startTime, endTime, results, priceData);
						
						

						updateOrAddSymbolRow(symbol, priceData, updatedResults != null ? updatedResults : results);
						
						
					});

				} else {
					PriceData priceData = fetchPriceDataTimeRange(symbol, results);

					if (!"Default".equals(volumeSourceCombo.getSelectedItem())
							&& !volumeSourceCombo.getSelectedItem().equals(dataSource)) {
						PriceData volumePriceData = volumeProvider.getLatestPrice(symbol);
						priceData.previousVolume = volumePriceData.getPreviousVolume();
						priceData.currentVolume = volumePriceData.getCurrentVolume();
					}
										
					// Re-compute custom indicators with the fetched price data
					SwingUtilities.invokeLater(() -> {
												
						LinkedHashMap<String, AnalysisResult> updatedResults = analyzeSymbolWithTimeRangeForCustomIndicators(
								symbol, startTime, endTime, results, priceData);
						
						updateOrAddSymbolRow(symbol, priceData, updatedResults != null ? updatedResults : results);
						
						
					});
				}

			} catch (IOException e) {
				logToConsole("❌ Error fetching price data for " + symbol + ": " + e.getMessage());
				e.printStackTrace();
			}

		} catch (Exception e) {
			logToConsole("❌ Error analyzing " + symbol + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	private LinkedHashMap<String, AnalysisResult> analyzeSymbolWithTimeRange(String symbol, ZonedDateTime startTime,
			ZonedDateTime endTime) {
		analysisOrchestrator.setDataProvider(currentDataProvider);

		// Pass the time range to the analysis orchestrator
		analysisOrchestrator.setTimeRange(startTime, endTime);

		Set<String> standardIndicators = timeframeIndicators.values().stream().flatMap(Set::stream)
				.collect(Collectors.toSet());

		logToConsole("analyzeSymbolWithTimeRange symbol" + symbol);

		// Convert custom indicator names to CustomIndicator objects
		Map<String, Set<CustomIndicator>> customIndicatorsMap = new HashMap<>();
		for (Map.Entry<String, Set<String>> entry : customIndicatorCombinations.entrySet()) {
			Set<CustomIndicator> customIndicators = new HashSet<>();
			for (String indicatorName : entry.getValue()) {
				CustomIndicator customIndicator = getCustomIndicatorByName(indicatorName);
				if (customIndicator != null) {
					customIndicators.add(customIndicator);
				}
			}
			customIndicatorsMap.put(entry.getKey(), customIndicators);
		}

		// Call analyzeSymbol with the provided time range
		LinkedHashMap<String, AnalysisResult> results = analysisOrchestrator.analyzeSymbol(symbol,
				currentTimeRadio.isSelected(), selectedTimeframes, standardIndicators, uptrendFilterEnabled,
				indicatorTimeRangeCombos, indicatorSessionCombos, indicatorIndexCounterFields, timeframeIndexRanges,
				currentDataSource, customIndicatorsMap, startTime, endTime);

		return results;
	}

	private LinkedHashMap<String, AnalysisResult> analyzeSymbolWithTimeRangeForCustomIndicators(String symbol,
			ZonedDateTime startTime, ZonedDateTime endTime, LinkedHashMap<String, AnalysisResult> results,
			PriceData priceData) {
		analysisOrchestrator.setDataProvider(currentDataProvider);

		checkAndInstantiateIndicatorsManagementApp();

		analysisOrchestrator.setIndicatorsManagementApp(indicatorsManagementApp);

		// Pass the time range to the analysis orchestrator
		analysisOrchestrator.setTimeRange(startTime, endTime);

		// Get all standard indicators
		Set<String> standardIndicators = timeframeIndicators.values().stream().flatMap(Set::stream)
				.collect(Collectors.toSet());

		// Convert custom indicator names to CustomIndicator objects
		Map<String, Set<CustomIndicator>> customIndicatorsMap = new HashMap<>();
		for (Map.Entry<String, Set<String>> entry : customIndicatorCombinations.entrySet()) {
			String timeframe = entry.getKey();
			Set<CustomIndicator> customIndicators = new HashSet<>();
			for (String indicatorName : entry.getValue()) {
				CustomIndicator customIndicator = getCustomIndicatorByName(indicatorName);
				if (customIndicator != null) {
					customIndicators.add(customIndicator);
					logToConsole("🔧 Adding custom indicator '" + indicatorName + "' for timeframe " + timeframe
							+ " in re-computation");
				} else {
					logToConsole("⚠️ Custom indicator '" + indicatorName + "' not found for timeframe " + timeframe
							+ " in re-computation");
				}
			}
			if (!customIndicators.isEmpty()) {
				customIndicatorsMap.put(entry.getKey(), customIndicators);
			}
		}

		logToConsole("📊 Re-computing custom indicators for " + symbol + " with " + customIndicatorsMap.size()
				+ " custom indicator timeframes");

		// Call analyzeSymbolReComputeCustomIndicator with custom indicators
		LinkedHashMap<String, AnalysisResult> updatedResults = analysisOrchestrator
				.analyzeSymbolReComputeCustomIndicator(symbol, selectedTimeframes, standardIndicators,
						uptrendFilterEnabled, indicatorTimeRangeCombos, indicatorSessionCombos,
						indicatorIndexCounterFields, timeframeIndexRanges, currentDataSource, customIndicatorsMap,
						startTime, endTime, results, priceData);

		// DEBUG: Check if custom indicators were processed in re-computation
		/*
		 * if (updatedResults != null) { for (Map.Entry<String, AnalysisResult> entry :
		 * updatedResults.entrySet()) { String timeframe = entry.getKey();
		 * AnalysisResult result = entry.getValue(); if (result != null &&
		 * customIndicatorsMap.containsKey(timeframe)) { Set<CustomIndicator>
		 * timeframeCustomIndicators = customIndicatorsMap.get(timeframe); for
		 * (CustomIndicator customIndicator : timeframeCustomIndicators) { String
		 * indicatorValue = result.getCustomIndicatorValue(customIndicator.getName());
		 * logToConsole("🔍 [Re-compute] " + timeframe + " " + customIndicator.getName()
		 * + " = " + indicatorValue);
		 * 
		 * // If still N/A, try to compute it manually if (indicatorValue == null ||
		 * indicatorValue.equals("N/A")) {
		 * logToConsole("⚠️ [Re-compute] Custom indicator still N/A, attempting manual computation"
		 * ); // You might need to add manual computation logic here based on your
		 * custom // indicator types } } } } } else {
		 * logToConsole("❌ [Re-compute] Updated results are null for symbol: " +
		 * symbol); }
		 */

		return updatedResults;
	}

	/**
	 * Main method to check all types of alerts after data refresh
	 */
	private void checkAllAlerts() {
		// 1. Check Volume Alerts (independent - based on volume alert config)
		checkAllVolumeAlerts();

		// Initialize StrategyExecutionService with dashboard configuration
		if (strategyExecutionService == null) {
			strategyExecutionService = createStrategyExecutionService();
		}

		checkAndInstantiateIndicatorsManagementApp();

		strategyCheckerHelper.setIndicatorsManagementApp(indicatorsManagementApp);
		strategyCheckerHelper.setPriceDataMap(priceDataMap);
		zScoreAlertManager.setStrategyCheckerHelper(strategyCheckerHelper);

		// 2. Check Z-Score Alerts (independent - based on Z-Score alert config)
		zScoreAlertManager.setMonteCarloConfig(monteCarloConfig);
		zScoreAlertManager.setPriceDataMap(priceDataMap);

		zScoreAlertManager.checkZScoreAlerts();

		// 3. Check Bullish Alerts (only if bullish checkbox is selected)
		if (checkBullishCheckbox.isSelected()) {
			checkBullishSymbolsAndLog();
		}
	}

	private void checkAllVolumeAlerts() {
		if (!volumeAlertConfig.isEnabled() || !volumeAlertConfig.isVolume20MAEnabled()) {
			return; // Volume alerts are disabled
		}

		Set<String> excludedSymbols = parseSymbolExceptions();
		List<String> symbolsToCheck;

		if (!volumeAlertConfig.isOnlyBullishSymbols()) {
			// Check all visible symbols in the table
			symbolsToCheck = getAllVisibleSymbols(excludedSymbols);
		} else {
			return;
		}

		List<String> symbolsWithVolumeSpikes = VolumeAlertAnalyser.analyze(symbolsToCheck, volumeAlertConfig,
				priceDataMap);

		handleVolumeAlerts(symbolsWithVolumeSpikes);
	}

	/**
	 * Get all visible symbols in the table
	 */
	private List<String> getAllVisibleSymbols(Set<String> excludedSymbols) {
		List<String> allSymbols = new ArrayList<>();

		for (Object rowObj : tableModel.getDataVector()) {
			@SuppressWarnings("unchecked")
			Vector<Object> row = (Vector<Object>) rowObj;
			String symbol = (String) row.get(1);

			if (tableModel.isSymbolHidden(symbol) || excludedSymbols.contains(symbol)) {
				continue;
			}
			allSymbols.add(symbol);
		}

		return allSymbols;
	}

	/**
	 * Handle volume spike alerts
	 */
	private void handleVolumeAlerts(List<String> symbolsWithVolumeSpikes) {
		if (!symbolsWithVolumeSpikes.isEmpty()) {
			String alertMessage = "Volume Spike Symbols: " + String.join(", ", symbolsWithVolumeSpikes);
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

	private void refreshZScoreColumns() {
		List<String> visibleSymbols = getVisibleSymbolList();
		if (visibleSymbols.isEmpty())
			return;

		// Get all Z-Score columns that are currently visible
		List<String> zScoreColumns = new ArrayList<>();
		for (int i = 0; i < tableModel.getColumnCount(); i++) {
			String colName = tableModel.getColumnName(i);
			if (zScoreColumnHelper.isZScoreColumn(colName)) {
				zScoreColumns.add(colName);
			}
		}

		if (zScoreColumns.isEmpty())
			return;

		logToConsole("🔄 Refreshing Z-Scores for " + visibleSymbols.size() + " symbols");

		// For each Z-Score column, compute and update values
		for (String zScoreCol : zScoreColumns) {
			String combinationName = zScoreColumnHelper.getCombinationNameFromColumn(zScoreCol);
			if (combinationName != null) {
				// Get Z-Score values using helper
				Map<String, Map<String, Double>> zScoreValues = zScoreColumnHelper.getZScoreColumnValues(visibleSymbols,
						combinationName, zScoreRankingGUI, priceDataMap);

				// Update both table AND priceDataMap
				for (int row = 0; row < tableModel.getRowCount(); row++) {
					String symbol = (String) tableModel.getValueAt(row, 1);
					if (symbol != null && zScoreValues.containsKey(symbol)) {
						Map<String, Double> symbolValues = zScoreValues.get(symbol);

						// Find the column index for this Z-Score combination
						for (int col = 0; col < tableModel.getColumnCount(); col++) {
							String currentColName = tableModel.getColumnName(col);
							if (currentColName.equals(zScoreCol)) {
								// Update table with proper data types
								if (zScoreColumnHelper.isRankColumn(currentColName)) {
									Integer rankValue = symbolValues.get("Rank").intValue();
									tableModel.setValueAt(rankValue, row, col);
								} else if (zScoreColumnHelper.isZScoreValueColumn(currentColName)) {
									Double zScoreValue = symbolValues.get("Z-Score");
									tableModel.setValueAt(zScoreValue, row, col);
								}

								// CRITICAL: Update priceDataMap with Z-Score results
								updatePriceDataWithZScore(symbol, combinationName, symbolValues);
								break;
							}
						}
					} else {
						// Set null values for symbols without Z-Score data
						for (int col = 0; col < tableModel.getColumnCount(); col++) {
							String currentColName = tableModel.getColumnName(col);
							if (currentColName.equals(zScoreCol)) {
								tableModel.setValueAt(null, row, col);
								break;
							}
						}
					}
				}
			}
		}

		tableModel.fireTableDataChanged();
		logToConsole("✅ Z-Scores refreshed and priceDataMap updated");
	}

	/**
	 * Update PriceData in priceDataMap with Z-Score results
	 */
	private void updatePriceDataWithZScore(String symbol, String combinationName, Map<String, Double> symbolValues) {
		try {
			PriceData priceData = priceDataMap.get(symbol);
			if (priceData == null) {
				logToConsole("⚠️ PriceData not found for symbol: " + symbol);
				return;
			}

			// Ensure Z-Score results map exists
			if (priceData.getZScoreResults() == null) {
				priceData.setZScoreResults(new HashMap<>());
			}

			// Create or update Z-Score result for this combination
			ZScoreCalculator.ZScoreResult zscoreResult = priceData.getZScoreResult(combinationName);
			if (zscoreResult == null) {
				zscoreResult = new ZScoreCalculator.ZScoreResult(combinationName);
			}

			// Update Z-Score result with new values
			if (symbolValues.containsKey("Z-Score")) {
				zscoreResult.setOverallScore(symbolValues.get("Z-Score"));
			}

			if (symbolValues.containsKey("Rank")) {
				// You might need to add a setRank method to ZScoreResult
				// If not available, store it in custom values or extend the class
				try {
					zscoreResult.getClass().getMethod("setRank", int.class).invoke(zscoreResult,
							symbolValues.get("Rank").intValue());
				} catch (Exception e) {
					// Fallback: store rank in custom values if setRank method doesn't exist
					logToConsole("⚠️ setRank method not available in ZScoreResult for " + symbol);
				}
			}

			// Update individual indicator scores if available in symbolValues
			updateIndividualZScores(zscoreResult, symbolValues);

			// Store the updated result
			priceData.addZScoreResult(combinationName, zscoreResult);

			priceDataMap.put(symbol, priceData);

			logToConsole("✅ Updated Z-Score for " + symbol + " [" + combinationName + "]: "
					+ symbolValues.getOrDefault("Z-Score", 0.0) + " (Rank: "
					+ symbolValues.getOrDefault("Rank", 0.0).intValue() + ")");

		} catch (Exception e) {
			logToConsole("❌ Error updating Z-Score for " + symbol + ": " + e.getMessage());
		}
	}

	/**
	 * Update individual Z-Score components if available
	 */
	private void updateIndividualZScores(ZScoreCalculator.ZScoreResult zscoreResult, Map<String, Double> symbolValues) {
		try {
			// List of possible individual Z-Score components
			String[] indicatorComponents = { "macdZscore", "macd359Zscore", "rsiZscore", "psarZscore", "psar001Zscore",
					"psar005Zscore", "vwapZscore", "volume20Zscore", "heikenAshiZscore", "highestCloseOpenZscore",
					"trendZscore", "movingAverageTargetValueZscore" };

			for (String component : indicatorComponents) {
				if (symbolValues.containsKey(component)) {
					try {
						zscoreResult.getClass().getMethod("set" + capitalize(component), double.class)
								.invoke(zscoreResult, symbolValues.get(component));
					} catch (Exception e) {
						// Method not available, skip this component
					}
				}
			}
		} catch (Exception e) {
			logToConsole("⚠️ Error updating individual Z-Scores: " + e.getMessage());
		}
	}

	/**
	 * Helper method to capitalize first letter (for method names)
	 */
	private String capitalize(String str) {
		if (str == null || str.isEmpty())
			return str;
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	private void updateOrAddSymbolRow(String symbol, PriceData priceData, Map<String, AnalysisResult> results) {
		// Apply volume filters
		if (!passesVolumeFilters(priceData.previousVolume, priceData.currentVolume)) {
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
			model.setValueAt(Boolean.FALSE, row, 0); // Reset checkbox
			model.setValueAt(priceData.latestPrice, row, 2);
			model.setValueAt(priceData.previousVolume, row, 3);
			model.setValueAt(priceData.currentVolume, row, 4);
			model.setValueAt(priceData.percentChange, row, 5);

			// Update PriceData columns
			for (int i = 6; i < model.getColumnCount(); i++) {
				String colName = model.getColumnName(i);
				try {
					Field field = PriceData.class.getDeclaredField(colName);
					field.setAccessible(true);
					Object value = field.get(priceData);
					model.setValueAt(value != null ? value : "N/A", row, i);
				} catch (NoSuchFieldException e) {
					// Not a PriceData column, skip
					continue;
				} catch (Exception e) {
					model.setValueAt("N/A", row, i);
				}
			}

			int colIndex = 6 + getPriceDataColumnCount(model); // Start of indicator columns after PriceData columns

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
						model.setValueAt(formatPsarValue(result.getPsar001(), result.getPsar001Trend()), row,
								colIndex++);
						break;
					case "PSAR(0.05)":
						model.setValueAt(formatPsarValue(result.getPsar005(), result.getPsar005Trend()), row,
								colIndex++);
						break;
					case "HeikenAshi":
						model.setValueAt(result.getHeikenAshiTrend(), row, colIndex++);
						break;
					case "Action":
						model.setValueAt(getActionRecommendation(result), row, colIndex++);
						break;
					case "Breakout Count":
						StringBuilder breakoutSummary = new StringBuilder();
						int totalBreakouts = 0;

						if (indicatorsForTf.contains("MACD")) {
							totalBreakouts += result.getBreakoutCount();
							breakoutSummary.append("MACD:").append(result.getBreakoutCount()).append(" ");
						}
						if (indicatorsForTf.contains("MACD(5,8,9)")) {
							totalBreakouts += result.getMacd359BreakoutCount();
							breakoutSummary.append("M5:").append(result.getMacd359BreakoutCount()).append(" ");
						}
						if (indicatorsForTf.contains("PSAR(0.01)")) {
							totalBreakouts += result.getPsar001BreakoutCount();
							breakoutSummary.append("PS1:").append(result.getPsar001BreakoutCount()).append(" ");
						}
						if (indicatorsForTf.contains("PSAR(0.05)")) {
							totalBreakouts += result.getPsar005BreakoutCount();
							breakoutSummary.append("PS5:").append(result.getPsar005BreakoutCount()).append(" ");
						}

						String displayValue = breakoutSummary.length() > 0
								? breakoutSummary.toString().trim() + " (Total: " + totalBreakouts + ")"
								: "No Breakouts";
						BreakoutValue breakoutValue = new BreakoutValue(totalBreakouts, displayValue);
						model.setValueAt(breakoutValue, row, colIndex++);
						break;
					case "HighestCloseOpen":
						String status = "N/A";
						if (result.getHighestCloseOpenStatus() != null) {
							status = result.getHighestCloseOpenStatus();
						} else if (result.getHighestCloseOpen() != Double.MIN_VALUE) {
							status = "N/A (" + String.format("%.2f", result.getHighestCloseOpen()) + ")";
						}
						model.setValueAt(status, row, colIndex++);
						break;
					case MovingAverageTargetValueStrategy.MATV_STRATEGY_NAME_CONSTANT:
						model.setValueAt(result.getMovingAverageTargetValue(), row, colIndex++);
						break;
					case "VWAP":
						model.setValueAt(result.getVwapStatus(), row, colIndex++);
						break;
					case "VOLUMEMA(20)":
						model.setValueAt(result.getVolume20MA(), row, colIndex++);
					default:
						model.setValueAt("N/A", row, colIndex++);
						break;
					}
				}
			}
			model.fireTableRowsUpdated(row, row);
		});
	}

	private int getPriceDataColumnCount(FilterableTableModel model) {
		int count = 0;
		for (int i = 6; i < model.getColumnCount(); i++) {
			String colName = model.getColumnName(i);
			try {
				PriceData.class.getDeclaredField(colName);
				count++;
			} catch (NoSuchFieldException e) {
				break;
			}
		}
		return count;
	}

	private void addNewSymbolRow(String symbol, PriceData priceData, Map<String, AnalysisResult> results) {
		Vector<Object> row = new Vector<>();
		row.add(false); // Checkbox
		row.add(symbol);
		row.add(priceData.latestPrice);
		row.add(priceData.previousVolume);
		row.add(priceData.currentVolume);
		row.add(priceData.percentChange);

		// Add PriceData values
		for (int i = 6; i < tableModel.getColumnCount(); i++) {
			String colName = tableModel.getColumnName(i);
			try {
				Field field = PriceData.class.getDeclaredField(colName);
				field.setAccessible(true);
				Object value = field.get(priceData);
				row.add(value != null ? value : "N/A");
			} catch (NoSuchFieldException e) {
				// Not a PriceData column, skip
				continue;
			} catch (Exception e) {
				row.add("N/A");
			}
		}

		// int colIndex = 6 + getPriceDataColumnCount(tableModel); // Start of indicator
		// columns after PriceData columns

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
					// colIndex++;
					continue;
				}

				switch (ind) {
				case "Trend":
				case "TREND":
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
					row.add(formatPsarValue(result.getPsar001(), result.getPsar001Trend()));
					break;
				case "PSAR(0.05)":
					row.add(formatPsarValue(result.getPsar005(), result.getPsar005Trend()));
					break;
				case "HeikenAshi":
					row.add(result.getHeikenAshiTrend());
					break;
				case "Action":
					row.add(getActionRecommendation(result));
					break;
				case "MACD Breakout":
					row.add(result.getMacdBreakoutCount());
					break;
				case "MACD(5,8,9) Breakout":
					row.add(result.getMacd359BreakoutCount());
					break;
				case "PSAR(0.01) Breakout":
					row.add(result.getPsar001BreakoutCount());
					break;
				case "PSAR(0.05) Breakout":
					row.add(result.getPsar005BreakoutCount());
					break;
				case "Breakout Count":
					StringBuilder breakoutSummary = new StringBuilder();
					int totalBreakouts = 0;

					if (indicatorsForTf.contains("MACD")) {
						totalBreakouts += result.getMacdBreakoutCount();
						breakoutSummary.append("MACD:").append(result.getMacdBreakoutCount()).append(" ");
					}
					if (indicatorsForTf.contains("MACD(5,8,9)")) {
						totalBreakouts += result.getMacd359BreakoutCount();
						breakoutSummary.append("M5:").append(result.getMacd359BreakoutCount()).append(" ");
					}
					if (indicatorsForTf.contains("PSAR(0.01)")) {
						totalBreakouts += result.getPsar001BreakoutCount();
						breakoutSummary.append("PS1:").append(result.getPsar001BreakoutCount()).append(" ");
					}
					if (indicatorsForTf.contains("PSAR(0.05)")) {
						totalBreakouts += result.getPsar005BreakoutCount();
						breakoutSummary.append("PS5:").append(result.getPsar005BreakoutCount()).append(" ");
					}

					String displayValue = breakoutSummary.length() > 0
							? breakoutSummary.toString().trim() + " (Total: " + totalBreakouts + ")"
							: "No Breakouts";
					row.add(new BreakoutValue(totalBreakouts, displayValue));
					break;
				case "HighestCloseOpen":
				case "HIGHESTCLOSEOPEN":
					String status = "N/A";
					if (result.getHighestCloseOpenStatus() != null) {
						status = result.getHighestCloseOpenStatus();
					} else if (result.getHighestCloseOpen() != Double.MIN_VALUE) {
						status = "N/A (" + String.format("%.2f", result.getHighestCloseOpen()) + ")";
					}
					row.add(status);
					break;
				case MovingAverageTargetValueStrategy.MATV_STRATEGY_NAME_CONSTANT:
					row.add(result.getMovingAverageTargetValue());
					break;
				case "Volume":
					row.add(result.getVolume());
					break;
				case "VOLUMEMA(20)":
					row.add(result.getVolume20MA());
					break;
				case "VWAP":
					row.add(result.getVwapStatus());
					break;
				default:
					row.add("N/A");
					break;
				}
			}

			// Process custom indicators for this timeframe
			if (customIndicatorCombinations.containsKey(tf)) {
				Set<String> customIndicators = customIndicatorCombinations.get(tf);
				for (String customIndicatorName : customIndicators) {
					if (result != null) {
						String customValue = result.getCustomIndicatorValue(customIndicatorName);
						row.add(customValue != null ? customValue : "N/A");
					} else {
						row.add("N/A");
					}
				}
			}

			// Add the new resistance indicator if enabled
			/*
			 * if (indicatorsForTf.contains("HighestCloseOpen")) { row.add(result != null &&
			 * result.getHighestCloseOpenStatus() != null ?
			 * result.getHighestCloseOpenStatus() : "N/A"); }
			 */
		}

		tableModel.addRow(row);
		logToConsole("Added new row for symbol: " + symbol);
	}

	private String getMacdStatus(AnalysisResult result, String macdType) {
		if ("MACD(5,8,9)".equals(macdType)) {
			return result.getMacd359Status() != null ? result.getMacd359Status() : "Neutral";
		} else {
			return result.getMacdStatus() != null ? result.getMacdStatus() : "Neutral";
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
			SwingUtilities.invokeLater(() -> {
				try {
					if (isWithinAllowedTimeRange() && !isProcessingUpdates()) {
						this.isProcessingUpdates = true;
						refreshData();

						// Save logs/reports if enabled
						/*
						 * if (enableSaveLogCheckbox.isSelected() ||
						 * enableLogReportsCheckbox.isSelected()) { if
						 * (enableSaveLogCheckbox.isSelected()) { saveConsoleLog(); } if
						 * (enableLogReportsCheckbox.isSelected()) { saveReportFiles(); } }
						 */
					} else if(isProcessingUpdates()) {
						logToConsole("Processing Updates in progress...");
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
	
	private boolean isProcessingUpdates() {
		return this.isProcessingUpdates ;
	}

	private boolean isWithinAllowedTimeRange() {
		try {
			Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_13.class);
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
		isProcessingUpdates = false;
		
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

	public ZonedDateTime getStartDateTime() {
		if (currentTimeRadio.isSelected()) {
			// Default to last 7 days for current time
			return ZonedDateTime.now(ZoneId.systemDefault()).minusDays(7);
		} else {
			// Use custom date and time
			Date startDate = startDatePicker.getDate();

			if (startDate != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(startDate);

				// Get time from class variable comboboxes
				int hour = Integer.parseInt((String) startHourCombo.getSelectedItem());
				int minute = Integer.parseInt((String) startMinuteCombo.getSelectedItem());

				cal.set(Calendar.HOUR_OF_DAY, hour);
				cal.set(Calendar.MINUTE, minute);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);

				return ZonedDateTime.ofInstant(cal.toInstant(), ZoneId.systemDefault());
			} else {
				// Fallback to default if custom range is selected but dates aren't set
				return ZonedDateTime.now(ZoneId.systemDefault()).minusDays(7);
			}
		}
	}

	public ZonedDateTime getEndDateTime() {
		if (currentTimeRadio.isSelected()) {
			return ZonedDateTime.now(ZoneId.systemDefault());
		} else {
			// Use custom date and time
			Date endDate = endDatePicker.getDate();

			if (endDate != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(endDate);

				// Get time from class variable comboboxes
				int hour = Integer.parseInt((String) endHourCombo.getSelectedItem());
				int minute = Integer.parseInt((String) endMinuteCombo.getSelectedItem());

				cal.set(Calendar.HOUR_OF_DAY, hour);
				cal.set(Calendar.MINUTE, minute);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);

				return ZonedDateTime.ofInstant(cal.toInstant(), ZoneId.systemDefault());
			} else {
				// Fallback to default if custom range is selected but dates aren't set
				return ZonedDateTime.now(ZoneId.systemDefault());
			}
		}
	}

	private PriceData fetchPriceDataTimeRange(String symbol, LinkedHashMap<String, AnalysisResult> results)
			throws IOException {

		String selectedVolumeSource = (String) volumeSourceCombo.getSelectedItem();
		// Get the selected time range
		ZonedDateTime startTime = getStartDateTime();
		ZonedDateTime endTime = getEndDateTime();

		PriceData priceDataReturn = new PriceData();

		try {
			priceDataReturn = (PriceData) tradingViewService.getStockBySymbol(symbol);
			// priceDataMap.put(symbol, priceDataReturn);
		} catch (StockApiException e) {
			logToConsole("Log message" + e.getMessage());
			;
		}
		// }

		PriceData priceDataFromDataProvider = currentDataProvider.getPriceData(symbol, startTime, endTime);

		// {"Default", "Alpaca", "Yahoo", "Polygon", "TradingView"};
		switch (selectedVolumeSource) {
		case "Alpaca":
			priceDataFromDataProvider.ticker = priceDataReturn.ticker;
			priceDataFromDataProvider.indexes = priceDataReturn.indexes;
			priceDataFromDataProvider.name = priceDataReturn.name;
			priceDataFromDataProvider.description = priceDataReturn.description;
			priceDataFromDataProvider.logoid = priceDataReturn.logoid;
			priceDataFromDataProvider.type = priceDataReturn.type;
			priceDataFromDataProvider.typespecs = priceDataReturn.typespecs;
			priceDataFromDataProvider.pricescale = priceDataReturn.pricescale;
			priceDataFromDataProvider.minmov = priceDataReturn.minmov;
			priceDataFromDataProvider.fractional = priceDataReturn.fractional;
			priceDataFromDataProvider.minmove2 = priceDataReturn.minmove2;
			priceDataFromDataProvider.currency = priceDataReturn.currency;
			priceDataFromDataProvider.exchange = priceDataReturn.exchange;
			priceDataReturn = priceDataFromDataProvider;
			break;
		case "Yahoo":
		case "Polygon":
			priceDataReturn.latestPrice = priceDataFromDataProvider.getLatestPrice();
			priceDataReturn.prevLastDayPrice = priceDataFromDataProvider.getPrevLastDayPrice();
			priceDataReturn.percentChange = priceDataFromDataProvider.getPercentChange();
			// priceDataReturn.averageVol = priceDataFromDataProvider.getAverageVol();
			priceDataReturn.currentVolume = priceDataFromDataProvider.getCurrentVolume();
			priceDataReturn.previousVolume = priceDataFromDataProvider.getPreviousVolume();
			break;
		// case "TradingView":
		// priceDataReturn.latestPrice = priceDataFromDataProvider.getLatestPrice();
		// priceDataReturn.prevLastDayPrice =
		// priceDataFromDataProvider.getPrevLastDayPrice();
		// priceDataReturn.previousVolume =
		// priceDataFromDataProvider.getPreviousVolume();
		// priceDataReturn.percentChange = priceDataFromDataProvider.getPercentChange();
		// priceDataReturn.averageVol = priceDataFromDataProvider.getAverageVol();
		// priceDataReturn.currentVolume = priceDataFromDataProvider.getCurrentVolume();
		// break;
		}

		priceDataReturn.prevLastDayPrice = priceDataFromDataProvider.getPrevLastDayPrice();
		priceDataReturn.previousVolume = priceDataFromDataProvider.getPreviousVolume();

		if (results != null) {
			priceDataReturn.setResults(results);
		}

		priceDataMap.put(symbol, priceDataReturn);

		return priceDataReturn;// currentDataProvider.getLatestPrice(symbol);
	}

	private PriceData fetchLatestPrice(String symbol, LinkedHashMap<String, AnalysisResult> results)
			throws IOException {

		String selectedVolumeSource = (String) volumeSourceCombo.getSelectedItem();

		PriceData priceDataReturn = new PriceData();

		try {
			priceDataReturn = (PriceData) tradingViewService.getStockBySymbol(symbol);
			// priceDataMap.put(symbol, priceDataReturn);
		} catch (StockApiException e) {
			logToConsole("Log message" + e.getMessage());
			;
		}
		// }

		PriceData priceDataFromDataProvider = currentDataProvider.getLatestPrice(symbol);

		// {"Default", "Alpaca", "Yahoo", "Polygon", "TradingView"};
		switch (selectedVolumeSource) {
		case "Alpaca":
			priceDataFromDataProvider.ticker = priceDataReturn.ticker;
			priceDataFromDataProvider.indexes = priceDataReturn.indexes;
			priceDataFromDataProvider.name = priceDataReturn.name;
			priceDataFromDataProvider.description = priceDataReturn.description;
			priceDataFromDataProvider.logoid = priceDataReturn.logoid;
			priceDataFromDataProvider.type = priceDataReturn.type;
			priceDataFromDataProvider.typespecs = priceDataReturn.typespecs;
			priceDataFromDataProvider.pricescale = priceDataReturn.pricescale;
			priceDataFromDataProvider.minmov = priceDataReturn.minmov;
			priceDataFromDataProvider.fractional = priceDataReturn.fractional;
			priceDataFromDataProvider.minmove2 = priceDataReturn.minmove2;
			priceDataFromDataProvider.currency = priceDataReturn.currency;
			priceDataFromDataProvider.exchange = priceDataReturn.exchange;
			priceDataReturn = priceDataFromDataProvider;
			break;
		case "Yahoo":
		case "Polygon":
			priceDataReturn.latestPrice = priceDataFromDataProvider.getLatestPrice();
			priceDataReturn.prevLastDayPrice = priceDataFromDataProvider.getPrevLastDayPrice();
			priceDataReturn.percentChange = priceDataFromDataProvider.getPercentChange();
			// priceDataReturn.averageVol = priceDataFromDataProvider.getAverageVol();
			priceDataReturn.currentVolume = priceDataFromDataProvider.getCurrentVolume();
			priceDataReturn.previousVolume = priceDataFromDataProvider.getPreviousVolume();
			break;
		// case "TradingView":
		// priceDataReturn.latestPrice = priceDataFromDataProvider.getLatestPrice();
		// priceDataReturn.prevLastDayPrice =
		// priceDataFromDataProvider.getPrevLastDayPrice();
		// priceDataReturn.previousVolume =
		// priceDataFromDataProvider.getPreviousVolume();
		// priceDataReturn.percentChange = priceDataFromDataProvider.getPercentChange();
		// priceDataReturn.averageVol = priceDataFromDataProvider.getAverageVol();
		// priceDataReturn.currentVolume = priceDataFromDataProvider.getCurrentVolume();
		// break;
		}

		priceDataReturn.prevLastDayPrice = priceDataFromDataProvider.getPrevLastDayPrice();
		priceDataReturn.previousVolume = priceDataFromDataProvider.getPreviousVolume();

		if (results != null) {
			priceDataReturn.setResults(results);
		}

		priceDataMap.put(symbol, priceDataReturn);

		return priceDataReturn;// currentDataProvider.getLatestPrice(symbol);
	}

	public PriceData getPriceDataBySymbol(String symbol) {

		PriceData priceData1 = priceDataMap.get(symbol);

		return priceData1;
	}

	private String formatPsarValue(double psarValue, String trend) {
		// Simplify the trend string to match other indicators
		if (trend.contains("↑")) {
			return "Uptrend";
		} else if (trend.contains("↓")) {
			return "Downtrend";
		}
		return trend;
	}

	private String getTrendStatus(AnalysisResult result) {
		/*
		 * if (result.getPrice() > result.getSma20() && result.getPrice() >
		 * result.getSma200()) { return "Strong Uptrend"; } else if (result.getPrice() <
		 * result.getSma20() && result.getPrice() < result.getSma200()) { return
		 * "Strong Downtrend"; }
		 */
		/*
		 * else if (result.getPrice() > result.getSma20()) { return "Mild Uptrend"; }
		 * else { return "Neutral"; }
		 */

		return result.getTrend();
	}

	private String getRsiStatus(AnalysisResult result) {
		/*
		 * if (result.getRsi() > 70) return "Overbought"; if (result.getRsi() < 30)
		 * return "Oversold"; if (result.getRsi() > 50) return "Bullish"; return
		 * "Bearish";
		 */
		return result.getRsiTrend();
	}

	private String getActionRecommendation(AnalysisResult result) {
		int score = 0;

		// Trend Analysis (SMA50 vs SMA200)
		if (result.getSma20() > result.getSma200() && result.getPrice() > result.getSma20()) {
			score += 2; // Bullish trend
		} else if (result.getSma20() < result.getSma200() && result.getPrice() < result.getSma20()) {
			score -= 2; // Bearish trend
		} else if (result.getSma20() > result.getSma200()) {
			score += 1; // Mild bullish
		} else if (result.getSma20() < result.getSma200()) {
			score -= 1; // Mild bearish
		}

		// RSI Analysis
		if (result.getRsi() < 30) {
			score += 2; // Oversold
		} else if (result.getRsi() > 70) {
			score -= 2; // Overbought
		} else if (result.getRsi() >= 50 && result.getRsi() <= 70) {
			score += 1; // Bullish momentum
		} else if (result.getRsi() >= 30 && result.getRsi() < 50) {
			score -= 1; // Bearish momentum
		}

		// MACD Analysis
		if ("Bullish Crossover".equals(result.getMacdStatus())
				|| "Bullish Crossover".equals(result.getMacd359Status())) {
			score += 2;
		} else if ("Bearish Crossover".equals(result.getMacdStatus())
				|| "Bearish Crossover".equals(result.getMacd359Status())) {
			score -= 2;
		} else if (result.getMacd() > result.getMacdSignal() || result.getMacd359() > result.getMacdSignal359()) {
			score += 1;
		} else if (result.getMacd() < result.getMacdSignal() || result.getMacd359() < result.getMacdSignal359()) {
			score -= 1;
		}

		// PSAR Analysis
		if ("↑ Uptrend".equals(result.getPsar001Trend()) || "↑ Uptrend".equals(result.getPsar005Trend())) {
			score += 1;
		} else if ("↓ Downtrend".equals(result.getPsar001Trend()) || "↓ Downtrend".equals(result.getPsar005Trend())) {
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

	// Add getter methods
	public ZScoreColumnHelper getZScoreColumnHelper() {
		return zScoreColumnHelper;
	}

	public FilterableTableModel getTableModel() {
		return tableModel;
	}

	public JTable getDashboardTable() {
		return dashboardTable;
	}

	public void setDashboardTable(JTable dashboardTable) {
		this.dashboardTable = dashboardTable;
	}

	public String getCurrentWatchlistName() {
		String currentWatchlistName = (String) watchlistComboBox.getSelectedItem();
		return currentWatchlistName;
	}

	public boolean isAlarmActive() {
		return isAlarmActive;
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

	private void openZScoreAlertConfigDialog() {

		if (strategyExecutionService == null) {
			strategyExecutionService = createStrategyExecutionService();
		}

		checkAndInstantiateIndicatorsManagementApp();

		strategyCheckerHelper.setIndicatorsManagementApp(indicatorsManagementApp);
		strategyCheckerHelper.setPriceDataMap(priceDataMap);
		zScoreAlertManager.setStrategyCheckerHelper(strategyCheckerHelper);
		zScoreAlertConfigDialog = new ZScoreAlertConfigDialog(this, indicatorsManagementApp, zScoreAlertManager);
		zScoreAlertConfigDialog.setPriceDataMap(priceDataMap);
		zScoreAlertConfigDialog.setMonteCarloConfig(monteCarloConfig);
		zScoreAlertConfigDialog.setVisible(true);

		if (zScoreAlertConfigDialog.isSaved()) {
			zScoreAlertManager.setAlertConfigs(zScoreAlertConfigDialog.getAlertConfigs());
			zScoreAlertManager.setEnabled(zScoreAlertConfigDialog.isZScoreAlertEnabled());
			logToConsole("Z-Score alert configuration updated");
		}
	}

	// Configuration storage in StockDashboard
	private Map<String, Object> columnsConfigData = new HashMap<>();
	private Map<String, Object> indicatorManagementConfig = new HashMap<>();

	private void openIndicatorManagement() {
		try {
			// Initialize StrategyExecutionService with dashboard configuration
			if (strategyExecutionService == null) {
				strategyExecutionService = createStrategyExecutionService();
			}

			checkAndInstantiateIndicatorsManagementApp();

			indicatorsManagementApp.setVisible(true);
			indicatorsManagementApp.toFront();

		} catch (Exception e) {
			logToConsole("Error opening Indicator Management: " + e.getMessage());
			JOptionPane.showMessageDialog(this, "Error opening Indicator Management: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void checkAndInstantiateIndicatorsManagementApp() {
		// Create or show the Indicators Management App
		if (indicatorsManagementApp == null || !indicatorsManagementApp.isVisible()) {
			ConsoleLogger indicatorsLogger = new ConsoleLogger() {
				@Override
				public void log(String message) {
					logToConsole("[Indicators] " + message);
				}
			};

			// Create IndicatorsManagementApp with the shared strategy service
			if (indicatorsManagementApp == null) {
				indicatorsManagementApp = new IndicatorsManagementApp(strategyExecutionService, indicatorsLogger);
				logToConsole("✅ Created new IndicatorsManagementApp instance");
			} else {
				indicatorsManagementApp.setExecutionService(strategyExecutionService);
				logToConsole("✅ Reused existing IndicatorsManagementApp instance");
			}

			// ✅ Load any saved globalCustomIndicators if they were stored before app
			// initialization
			if (indicatorManagementConfig != null && indicatorManagementConfig.containsKey("globalCustomIndicators")) {
				logToConsole("📥 Loading stored custom indicators into newly created app");
				loadGlobalCustomIndicatorsFromStoredConfig();
			}

			// Load any saved configuration
			if (indicatorManagementConfig != null && !indicatorManagementConfig.isEmpty()) {
				indicatorsManagementApp.importConfiguration(indicatorManagementConfig);
				indicatorsManagementApp.importWatchlists(watchlists);
				logToConsole("✅ Imported configuration to IndicatorsManagementApp");
			}

			// Verify that custom indicators are loaded
			verifyCustomIndicatorsLoaded();

			// Add window listener to handle closing
			indicatorsManagementApp.addWindowListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowClosing(java.awt.event.WindowEvent windowEvent) {
					// if(!indicatorsManagementApp.isSaved()) {
					saveFullConfigurationFromIndicatorsApp();
					autoSaveConfiguration();
					// }
				}

				@Override
				public void windowClosed(java.awt.event.WindowEvent windowEvent) {
					// if(!indicatorsManagementApp.isSaved()) {
					saveFullConfigurationFromIndicatorsApp();
					// }
				}
			});

			indicatorsManagementApp.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			indicatorsManagementApp.setLocationRelativeTo(this);
		} else {
			logToConsole("ℹ️ IndicatorsManagementApp already exists and is visible");
			verifyCustomIndicatorsLoaded();
		}
	}

	/**
	 * Load global custom indicators from stored config
	 */
	@SuppressWarnings("unchecked")
	private void loadGlobalCustomIndicatorsFromStoredConfig() {
		try {
			List<Map<String, Object>> storedCustomIndicators = (List<Map<String, Object>>) indicatorManagementConfig
					.get("globalCustomIndicators");
			if (storedCustomIndicators != null && !storedCustomIndicators.isEmpty()) {
				Set<CustomIndicator> loadedCustomIndicators = new HashSet<>();

				for (Map<String, Object> indicatorMap : storedCustomIndicators) {
					try {
						String name = (String) indicatorMap.get("name");
						String type = (String) indicatorMap.get("type");

						// Create CustomIndicator based on type and parameters
						CustomIndicator customIndicator;
						if (indicatorMap.containsKey("parameters")) {
							Map<String, Object> parameters = (Map<String, Object>) indicatorMap.get("parameters");
							customIndicator = new CustomIndicator(name, type, parameters);
						} else {
							customIndicator = new CustomIndicator(name, type);
						}

						loadedCustomIndicators.add(customIndicator);
						logToConsole("✅ Loaded stored custom indicator: " + customIndicator.getName());

					} catch (Exception e) {
						logToConsole("❌ Error loading stored custom indicator: " + e.getMessage());
					}
				}

				indicatorsManagementApp.setAllCustomIndicators(loadedCustomIndicators);
				logToConsole("✅ Loaded " + loadedCustomIndicators.size() + " stored custom indicators");

				// Remove from temporary storage
				indicatorManagementConfig.remove("globalCustomIndicators");
			}
		} catch (Exception e) {
			logToConsole("❌ Error loading stored custom indicators: " + e.getMessage());
		}
	}

	/**
	 * Verify that custom indicators are properly loaded
	 */
	private void verifyCustomIndicatorsLoaded() {
		if (indicatorsManagementApp == null) {
			logToConsole("❌ Cannot verify - IndicatorsManagementApp is null");
			return;
		}

		Set<CustomIndicator> currentIndicators = indicatorsManagementApp.getGlobalCustomIndicators();
		logToConsole("🔍 Verification - Current custom indicators: " + currentIndicators.size());

		for (CustomIndicator indicator : currentIndicators) {
			logToConsole("   ✓ " + indicator.getName() + " (" + indicator.getType() + ")");
		}

		// Also verify customIndicatorCombinations
		logToConsole("🔍 Custom indicator combinations: " + customIndicatorCombinations.size() + " timeframes");
		for (Map.Entry<String, Set<String>> entry : customIndicatorCombinations.entrySet()) {
			logToConsole("   " + entry.getKey() + ": " + entry.getValue());
		}
	}

	/**
	 * Load full configuration to IndicatorsManagementApp (saved config + current
	 * dashboard)
	 */
	private void loadFullConfigurationToIndicatorsApp() {
		if (indicatorsManagementApp == null)
			return;

		try {
			// 1. Load saved strategies and custom indicators if they exist
			if (hasSavedIndicatorManagementConfig()) {
				loadSavedConfigurationToIndicatorsApp();
			} else {
				// First time - initialize with empty configuration
				indicatorsManagementApp.clearAllStrategies();
				indicatorsManagementApp.clearAllCustomIndicators();
				logToConsole("🆕 Initialized Indicator Management with empty configuration");
			}

			// 2. Always load current watchlists from dashboard
			loadCurrentWatchlistToIndicatorsApp();

			// 3. Load current indicator configuration for suggestions
			loadCurrentIndicatorConfiguration();

			logToConsole("✅ Full configuration loaded to Indicator Management");

		} catch (Exception e) {
			logToConsole("❌ Error loading full configuration: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Check if there's saved indicator management configuration
	 */
	private boolean hasSavedIndicatorManagementConfig() {
		return indicatorManagementConfig != null && !indicatorManagementConfig.isEmpty()
				&& (indicatorManagementConfig.containsKey("strategies")
						|| indicatorManagementConfig.containsKey("customIndicators"));
	}

	/**
	 * Load saved configuration to IndicatorsManagementApp
	 */
	@SuppressWarnings("unchecked")
	private void loadSavedConfigurationToIndicatorsApp() {
		try {
			// Load strategies
			if (indicatorManagementConfig.containsKey("strategies")) {
				List<StrategyConfig> savedStrategies = (List<StrategyConfig>) indicatorManagementConfig
						.get("strategies");
				if (savedStrategies != null && !savedStrategies.isEmpty()) {
					indicatorsManagementApp.setAllStrategies(savedStrategies);
					logToConsole("✅ Loaded " + savedStrategies.size() + " saved strategies");
				}
			}

			// Load custom indicators
			if (indicatorManagementConfig.containsKey("customIndicators")) {
				Set<CustomIndicator> savedCustomIndicators = (Set<CustomIndicator>) indicatorManagementConfig
						.get("customIndicators");
				if (savedCustomIndicators != null && !savedCustomIndicators.isEmpty()) {
					indicatorsManagementApp.setAllCustomIndicators(savedCustomIndicators);
					logToConsole("✅ Loaded " + savedCustomIndicators.size() + " saved custom indicators");
				}
			}

			// Load custom indicators
			if (indicatorManagementConfig.containsKey("globalCustomIndicators")) {
				ArrayList<CustomIndicator> savedCustomIndicators = (ArrayList<CustomIndicator>) indicatorManagementConfig
						.get("globalCustomIndicators");
				if (savedCustomIndicators != null && !savedCustomIndicators.isEmpty()) {

					Set<CustomIndicator> customIndicatorsSet = new HashSet<CustomIndicator>();

					for (CustomIndicator c : savedCustomIndicators) {
						customIndicatorsSet.add(c);
					}

					indicatorsManagementApp.setAllCustomIndicators(customIndicatorsSet);
					logToConsole("✅ Loaded " + savedCustomIndicators.size() + " saved custom indicators");
				}
			}

			// Load watchlists if saved separately
			if (indicatorManagementConfig.containsKey("watchlists")) {
				Map<String, WatchlistData> savedWatchlists = (Map<String, WatchlistData>) indicatorManagementConfig
						.get("watchlists");
				if (savedWatchlists != null && !savedWatchlists.isEmpty()) {
					// Merge with current dashboard watchlists
					Map<? extends String, ? extends WatchlistData> currentWatchlists = convertAllWatchlistsToSharedFormat();
					savedWatchlists.putAll(currentWatchlists); // Current dashboard takes priority
					indicatorsManagementApp.setGlobalWatchlists(savedWatchlists);
					logToConsole("✅ Loaded " + savedWatchlists.size() + " saved watchlists");
				}
			}

		} catch (Exception e) {
			logToConsole("❌ Error loading saved configuration: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Save full configuration from IndicatorsManagementApp when it closes
	 */
	private void saveFullConfigurationFromIndicatorsApp() {
		if (indicatorsManagementApp == null)
			return;

		try {
			// 1. Save strategies
			List<StrategyConfig> currentStrategies = indicatorsManagementApp.getAllStrategies();
			indicatorManagementConfig.put("strategies", new ArrayList<>(currentStrategies));

			// 2. Save custom indicators (globalCustomIndicators)
			Set<CustomIndicator> currentCustomIndicators = indicatorsManagementApp.getGlobalCustomIndicators();
			indicatorManagementConfig.put("customIndicators",
					convertCustomIndicatorsToSerializable(currentCustomIndicators));
			indicatorManagementConfig.put("globalCustomIndicators",
					convertCustomIndicatorsToSerializable(currentCustomIndicators));

			// 3. Save watchlists (optional - you might want to keep these separate)
			Map<String, WatchlistData> currentWatchlists = indicatorsManagementApp.getGlobalWatchlists();
			indicatorManagementConfig.put("watchlists", new HashMap<>(currentWatchlists));

			// 4. Update dashboard watchlists with any new ones from indicators app
			updateDashboardWatchlistsFromIndicatorsApp(currentWatchlists);

			logToConsole("💾 Saved Indicator Management configuration:");
			logToConsole("   - Strategies: " + currentStrategies.size());
			logToConsole("   - Custom Indicators: " + currentCustomIndicators.size());
			logToConsole("   - Watchlists: " + currentWatchlists.size());

			// 5. Also save to persistent storage if needed
			// saveIndicatorManagementConfigToPreferences();

			indicatorsManagementApp.setSaved(true);

		} catch (Exception e) {
			logToConsole("❌ Error saving full configuration: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to convert CustomIndicator objects to serializable format
	 */
	private List<Map<String, Object>> convertCustomIndicatorsToSerializable(Set<CustomIndicator> customIndicators) {
		List<Map<String, Object>> serializableList = new ArrayList<>();

		for (CustomIndicator customIndicator : customIndicators) {
			Map<String, Object> indicatorMap = new HashMap<>();
			indicatorMap.put("name", customIndicator.getName());
			indicatorMap.put("type", customIndicator.getType());
			indicatorMap.put("displayName", customIndicator.getDisplayName());

			// Save parameters
			if (customIndicator.getParameters() != null) {
				indicatorMap.put("parameters", new HashMap<>(customIndicator.getParameters()));
			}

			serializableList.add(indicatorMap);
		}

		return serializableList;
	}

	/**
	 * Save indicator management configuration to preferences for persistence
	 */
	private void saveIndicatorManagementConfigToPreferences() {
		try {
			Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_13.class);

			// Convert configuration to JSON string for storage
			JSONObject configJson = new JSONObject();

			// Save strategies count and names for reference
			List<StrategyConfig> strategies = (List<StrategyConfig>) indicatorManagementConfig.get("strategies");
			if (strategies != null) {
				configJson.put("strategyCount", strategies.size());
				JSONArray strategyNames = new JSONArray();
				for (StrategyConfig strategy : strategies) {
					strategyNames.put(strategy.getName());
				}
				configJson.put("strategyNames", strategyNames);
			}

			// Save custom indicators count
			Set<CustomIndicator> customIndicators = (Set<CustomIndicator>) indicatorManagementConfig
					.get("customIndicators");
			if (customIndicators != null) {
				configJson.put("customIndicatorCount", customIndicators.size());
			}

			// Save watchlists count
			Map<String, Set<String>> watchlists = (Map<String, Set<String>>) indicatorManagementConfig
					.get("watchlists");
			if (watchlists != null) {
				configJson.put("watchlistCount", watchlists.size());
			}

			// Save timestamp
			configJson.put("lastSaved", System.currentTimeMillis());

			prefs.put("indicatorManagementConfig", configJson.toString());

			logToConsole("💾 Indicator Management configuration saved to preferences");

		} catch (Exception e) {
			logToConsole("❌ Error saving configuration to preferences: " + e.getMessage());
		}
	}

	/**
	 * Update dashboard watchlists from IndicatorsManagementApp
	 */
	private void updateDashboardWatchlistsFromIndicatorsApp(Map<String, WatchlistData> currentWatchlists) {
		try {
			// Filter out any system-generated watchlists if needed
			Map<String, WatchlistData> filteredWatchlists = new HashMap<>();
			for (Entry<String, WatchlistData> entry : currentWatchlists.entrySet()) {
				String watchlistName = entry.getKey();
				// Only import non-system watchlists (customize this filter as needed)
				if (!watchlistName.startsWith("SYSTEM_") && !watchlistName.startsWith("TEMP_")) {
					filteredWatchlists.put(watchlistName, entry.getValue());
				}
			}

			// Update dashboard watchlists
			updateWatchlistsFromSharedFormat(filteredWatchlists);

			// Refresh the watchlist combo box
			refreshWatchlistComboBox();

			logToConsole(
					"✅ Updated dashboard with " + filteredWatchlists.size() + " watchlists from Indicator Management");

		} catch (Exception e) {
			logToConsole("❌ Error updating dashboard watchlists: " + e.getMessage());
		}
	}

	/**
	 * Create StrategyExecutionService configured with current dashboard settings
	 */
	private StrategyExecutionService createStrategyExecutionService() {
		ConsoleLogger strategyLogger = new ConsoleLogger() {
			@Override
			public void log(String message) {
				logToConsole("[Strategy] " + message);
			}
		};

		StrategyExecutionService service = new StrategyExecutionService(strategyLogger);

		// Log available strategies
		Map<String, AbstractIndicatorStrategy> availableStrategies = service.getStrategyMap();
		logToConsole("Strategy service initialized with " + availableStrategies.size() + " strategies: "
				+ String.join(", ", availableStrategies.keySet()));

		return service;
	}

	/**
	 * Load the current watchlist from StockDashboard to IndicatorsManagementApp
	 */
	private void loadCurrentWatchlistToIndicatorsApp() {
		if (indicatorsManagementApp == null)
			return;

		try {
			// Get current watchlist name and symbols
			String currentWatchlistName = (String) watchlistComboBox.getSelectedItem();
			WatchlistData currentSymbols = watchlists.get(currentWatchlistName);

			if (currentSymbols != null && !currentSymbols.isEmpty()) {
				// Convert all watchlists to the format used by IndicatorsManagementApp
				Map<String, WatchlistData> allWatchlistsForIndicators = convertAllWatchlistsToSharedFormat();

				// Use the setter method to update watchlists in IndicatorsManagementApp
				Map<String, WatchlistData> watchlistsForIndicators = new HashMap<>();
				for (Entry<String, WatchlistData> entry : allWatchlistsForIndicators.entrySet()) {
					watchlistsForIndicators.put(entry.getKey(), entry.getValue());
				}
				indicatorsManagementApp.setGlobalWatchlists(watchlistsForIndicators);

				logToConsole("✅ Loaded " + allWatchlistsForIndicators.size() + " watchlists to Indicator Management");
				logToConsole(
						"   Current watchlist: " + currentWatchlistName + " (" + currentSymbols.size() + " symbols)");

			} else {
				logToConsole("⚠️ Current watchlist is empty: " + currentWatchlistName);
				// Still load other watchlists even if current is empty
				Map<String, WatchlistData> allWatchlistsForIndicators = convertAllWatchlistsToSharedFormat();

				Map<String, WatchlistData> watchlistsForIndicators = new HashMap<>();
				for (Entry<String, WatchlistData> entry : allWatchlistsForIndicators.entrySet()) {
					watchlistsForIndicators.put(entry.getKey(), entry.getValue());
				}
				indicatorsManagementApp.setGlobalWatchlists(watchlistsForIndicators);
			}

		} catch (Exception e) {
			logToConsole("❌ Error loading current watchlist: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Convert ALL watchlists from dashboard format to IndicatorsManagementApp
	 * format
	 */
	private Map<String, WatchlistData> convertAllWatchlistsToSharedFormat() {
		Map<String, WatchlistData> sharedWatchlists = new LinkedHashMap<>();

		for (Map.Entry<String, WatchlistData> entry : watchlists.entrySet()) {
			String watchlistName = entry.getKey();
			WatchlistData watchlistData = entry.getValue();

			if (watchlistData != null && !watchlistData.getSymbols().isEmpty()) {
				sharedWatchlists.put(watchlistName, watchlistData);
			}
		}

		return sharedWatchlists;
	}

	/**
	 * Load current indicator configuration from dashboard
	 */
	private void loadCurrentIndicatorConfiguration() {
		try {
			// Get current enabled timeframes and indicators
			Set<String> enabledTimeframes = getEnabledTimeframes();
			Set<String> enabledIndicators = getEnabledIndicators();

			logToConsole("📊 Current dashboard configuration:");
			logToConsole("   Timeframes: " + String.join(", ", enabledTimeframes));
			logToConsole("   Indicators: " + String.join(", ", enabledIndicators));

			// You could pass this configuration to IndicatorsManagementApp
			// if it has methods to accept external configuration

		} catch (Exception e) {
			logToConsole("Error loading indicator configuration: " + e.getMessage());
		}
	}

	/**
	 * Get currently enabled timeframes from dashboard
	 */
	private Set<String> getEnabledTimeframes() {
		Set<String> enabledTimeframes = new HashSet<>();
		for (String tf : allTimeframes) {
			Set<String> indicators = timeframeIndicators.get(tf);
			if (indicators != null && !indicators.isEmpty()) {
				enabledTimeframes.add(tf);
			}
		}
		return enabledTimeframes;
	}

	/**
	 * Get currently enabled indicators from dashboard
	 */
	private Set<String> getEnabledIndicators() {
		Set<String> enabledIndicators = new HashSet<>();
		for (String tf : allTimeframes) {
			Set<String> indicators = timeframeIndicators.get(tf);
			if (indicators != null) {
				enabledIndicators.addAll(indicators);
			}
		}
		return enabledIndicators;
	}

	/**
	 * Update this dashboard's watchlists from IndicatorsManagementApp format
	 */
	private void updateWatchlistsFromSharedFormat(Map<String, WatchlistData> filteredWatchlists) {
		watchlists.clear();

		for (Entry<String, WatchlistData> entry : filteredWatchlists.entrySet()) {
			watchlists.put(entry.getKey(), entry.getValue());
		}
	}

	// UPDATE: This method needs to work with both old and new callers
	private void refreshWatchlistComboBox() {
		String currentSelection = (String) watchlistComboBox.getSelectedItem();

		watchlistComboBox.removeAllItems();
		for (String watchlistName : watchlists.keySet()) {
			watchlistComboBox.addItem(watchlistName);
		}

		// Restore previous selection if it still exists
		if (watchlists.containsKey(currentSelection)) {
			watchlistComboBox.setSelectedItem(currentSelection);
			currentWatchlist = currentSelection;
		} else if (!watchlists.isEmpty()) {
			watchlistComboBox.setSelectedIndex(0);
			currentWatchlist = (String) watchlistComboBox.getSelectedItem();
		}
	}

	// Method to get symbols in old format (for backward compatibility)
	public List<String> getCurrentWatchlistSymbolsOldFormat() {
		WatchlistData watchlistData = watchlists.get(currentWatchlist);
		if (watchlistData != null) {
			return new ArrayList<>(watchlistData.getSymbols());
		}
		return new ArrayList<>();
	}

	// Method to get all watchlists in old format (for IndicatorsManagementApp)
	public Map<String, Set<String>> getAllWatchlistsOldFormat() {
		Map<String, Set<String>> oldFormat = new HashMap<>();
		for (Map.Entry<String, WatchlistData> entry : watchlists.entrySet()) {
			oldFormat.put(entry.getKey(), new HashSet<>(entry.getValue().getSymbols()));
		}
		return oldFormat;
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

	public ZScoreAlertManager getZScoreAlertManager() {
		return zScoreAlertManager;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			StockDashboardv1_22_13 dashboard = new StockDashboardv1_22_13();
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

	private File getWatchlistLogDirectory() {
		String watchlistName = watchlistComboBox.getSelectedItem().toString();
		String dateString = new SimpleDateFormat("MMddyyyy").format(new Date());

		// Use the configured directory or default to user home if not set
		String baseDir = logDirectoryField.getText().trim();
		if (baseDir.isEmpty()) {
			baseDir = System.getProperty("user.home");
		}

		File watchlistDir = new File(baseDir, watchlistName);
		File dateDir = new File(watchlistDir, dateString);

		// Create directories if they don't exist
		if (!dateDir.exists()) {
			if (!dateDir.mkdirs()) {
				logToConsole("Failed to create directory: " + dateDir.getAbsolutePath());
				return null;
			}
		}

		return dateDir;
	}

	private void showMonteCarloGraph() {
		// Get symbols from selected checkboxes
		List<String> symbols = getSelectedSymbolsForGraph();

		if (symbols.isEmpty()) {
			// If no symbols selected via checkboxes, get visible symbols
			symbols = getVisibleSymbolsForGraph();
		}

		if (symbols.isEmpty()) {
			JOptionPane.showMessageDialog(this, "No symbols selected for Monte Carlo graph", "Warning",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		// Get selected columns from Monte Carlo config
		List<String> selectedColumns = new ArrayList<>();
		if (monteCarloConfig.containsKey("selectedColumns")) {
			selectedColumns = (List<String>) monteCarloConfig.get("selectedColumns");
		}

		// Get graph settings
		Map<String, Object> graphSettings = new HashMap<>();
		if (monteCarloConfig.containsKey("graphSettings")) {
			graphSettings = (Map<String, Object>) monteCarloConfig.get("graphSettings");
		}

		// Create title with column count
		String title = "Monte Carlo Graph";
		if (!selectedColumns.isEmpty()) {
			title += " (" + selectedColumns.size() + " columns)";
		}

		// Create the MonteCarloGraphApp with configuration and price data
		if (currentTimeRadio.isSelected()) {
			new MonteCarloGraphApp(symbols, dataProviderFactory, this::logToConsole, title, selectedColumns,
					graphSettings, monteCarloConfig, priceDataMap);
		} else {
			ZonedDateTime start = getStartDateTime();
			ZonedDateTime end = getEndDateTime();
			new MonteCarloGraphApp(symbols, dataProviderFactory, this::logToConsole, title, start, end, "1Min",
					selectedColumns, graphSettings, monteCarloConfig, priceDataMap);
		}

		/*
		 * if (currentTimeRadio.isSelected()) { new MonteCarloGraphApp(symbols,
		 * dataProviderFactory, this::logToConsole, "Manual View"); } else {
		 * ZonedDateTime start = getStartDateTime(); ZonedDateTime end =
		 * getEndDateTime(); new MonteCarloGraphApp(symbols, dataProviderFactory,
		 * this::logToConsole, "Manual View", start, end, "1Min"); }
		 */
	}

	private List<String> getSelectedSymbolsForGraph() {
		List<String> symbols = new ArrayList<>();
		for (int i = 0; i < tableModel.getRowCount(); i++) {
			Boolean isSelected = (Boolean) tableModel.getValueAt(i, 0);
			if (isSelected != null && isSelected) {
				String symbol = (String) tableModel.getValueAt(i, 1);
				if (symbol != null && !symbol.trim().isEmpty()) {
					symbols.add(symbol);
				}
			}
		}
		return symbols;
	}

	private List<String> getVisibleSymbolsForGraph() {
		List<String> symbols = new ArrayList<>();
		for (int i = 0; i < tableModel.getRowCount(); i++) {
			String symbol = (String) tableModel.getValueAt(i, 1);
			if (symbol != null && !symbol.trim().isEmpty() && !tableModel.isSymbolHidden(symbol)) {
				symbols.add(symbol);
			}
		}
		return symbols;
	}

	private StockDataProvider getVolumeDataProvider(String volumeSource, String dataSource) {
		if ("Default".equals(volumeSource)) {
			return dataProviderFactory.getProvider(dataSource, this::logToConsole);
		}
		return dataProviderFactory.getProvider(volumeSource, this::logToConsole);
	}

	private List<String> getVisibleSymbols() {
		List<String> symbols = new ArrayList<>();
		for (int i = 0; i < tableModel.getRowCount(); i++) {
			Boolean isSelected = (Boolean) tableModel.getValueAt(i, 0); // Checkbox column
			if (isSelected != null && isSelected) {
				String symbol = (String) tableModel.getValueAt(i, 1); // Symbol column
				if (symbol != null && !symbol.trim().isEmpty()) {
					symbols.add(symbol);
				}
			}
		}
		if (symbols.isEmpty()) {
			logToConsole("No symbols selected in the table for Monte Carlo graph");
		}
		return symbols;
	}

	private List<String> getVisibleSymbolList() {
		Vector<?> rawData = tableModel.getDataVector();
		List<String> visibleSymbols = new ArrayList<>();

		for (Object row : rawData) {
			Vector<?> rowVector = (Vector<?>) row;
			String symbol = (String) rowVector.get(1);
			if (!tableModel.isSymbolHidden(symbol)) {
				visibleSymbols.add(symbol);
			}
		}

		return visibleSymbols;
	}

	private class DoubleCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value != null && value instanceof Double) {
				Double d = (Double) value;
				if (Double.isNaN(d) || d == null) {
					setText("N/A");
				} else {
					setText(String.format("%.2f", d));
				}
				setHorizontalAlignment(RIGHT);
			}
			if (isSelected) {
				c.setBackground(table.getSelectionBackground());
			}
			return c;
		}
	}

	private class LongCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value != null && value instanceof Long) {
				setText(String.format("%,d", (Long) value));
				setHorizontalAlignment(RIGHT);
			}
			if (isSelected) {
				c.setBackground(table.getSelectionBackground());
			}
			return c;
		}
	}

	private class BreakoutValueRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (value instanceof BreakoutValue) {
				value = ((BreakoutValue) value).displayValue;
			}
			return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
	}

	public class StatusCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			// Skip coloring for checkbox and numeric columns
			if (column == 0 || value instanceof Number) {
				return c;
			}

			String status = value != null ? value.toString() : "";

			// Apply coloring based on status
			// if
			// (StrategyCheckerHelper.BULLISH_STATUSES.stream().anyMatch(status::contains))
			// {
			if (BULLISH_STATUSES.stream().anyMatch(status::contains)) {
				c.setBackground(BULLISH_COLOR);
				c.setForeground(Color.BLACK);
			} else if (status.contains("↓") || status.contains("Downtrend") || status.contains("Sell")
					|| status.contains("Bearish")) {
				c.setBackground(BEARISH_COLOR);
				c.setForeground(Color.BLACK);
			} else {
				c.setBackground(table.getBackground());
				c.setForeground(table.getForeground());
			}

			if (isSelected) {
				c.setBackground(table.getSelectionBackground());
				c.setForeground(table.getSelectionForeground());
			}

			setHorizontalAlignment(SwingConstants.CENTER);
			return c;
		}
	}

	public JRadioButton getCurrentTimeRadio() {
		return currentTimeRadio;
	}

}