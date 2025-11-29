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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
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
import javax.swing.RowSorter;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.jdesktop.swingx.JXDatePicker;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
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
import com.quantlabs.stockApp.alert.ZScoreAlertManager;
import com.quantlabs.stockApp.alert.model.VolumeAlertAnalyser;
import com.quantlabs.stockApp.alert.model.VolumeAlertConfig;
import com.quantlabs.stockApp.alert.model.VolumeAlertTimeframeConfig;
import com.quantlabs.stockApp.alert.model.ZScoreAlertConfig;
import com.quantlabs.stockApp.alert.ui.VolumeAlertConfigDialog;
import com.quantlabs.stockApp.alert.ui.ZScoreAlertConfigDialog;
import com.quantlabs.stockApp.analysis.ui.ZScoreRankingGUI;
import com.quantlabs.stockApp.chart.SymbolChartWindow;
import com.quantlabs.stockApp.core.indicators.AdvancedVWAPStrategy;
import com.quantlabs.stockApp.core.indicators.HeikenAshiIndicator;
import com.quantlabs.stockApp.core.indicators.VolumeIndicator;
import com.quantlabs.stockApp.core.indicators.VolumeMAIndicator;
import com.quantlabs.stockApp.core.indicators.resistance.HighestCloseOpenIndicator;
import com.quantlabs.stockApp.core.indicators.targetvalue.MovingAverageTargetValue;
import com.quantlabs.stockApp.data.StockDataProvider;
import com.quantlabs.stockApp.data.StockDataProviderFactory;
import com.quantlabs.stockApp.exception.StockApiException;
import com.quantlabs.stockApp.indicator.strategy.HighestCloseOpenStrategy;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;
import com.quantlabs.stockApp.reports.MonteCarloGraphApp;
import com.quantlabs.stockApp.service.tradingview.TradingViewService;
import com.quantlabs.stockApp.service.yahoo.YahooFinanceService;
import com.quantlabs.stockApp.utils.PriceDataMapper;
import com.quantlabs.stockApp.utils.ZScoreCalculator;
import com.quantlabs.stockApp.utils.ZScoreColumnHelper;

import okhttp3.OkHttpClient;

public class StockDashboardv1_22_5 extends JFrame implements IStockDashboard{
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

	private final String[] allIndicators = { "Trend", "RSI", "MACD", "MACD Breakout", "MACD(5,8,9)", "HeikenAshi",
			"MACD(5,8,9) Breakout", "PSAR(0.01)", "PSAR(0.01) Breakout", "PSAR(0.05)", "PSAR(0.05) Breakout",
			"Breakout Count", "Action", "MovingAverageTargetValue", "HighestCloseOpen", "Volume", "Volume20MA", "VWAP"};

	/*
	 * private final String[] allIndicators = { "Trend", "RSI", "MACD",
	 * "MACD(5,8,9)", "PSAR(0.05)", "PSAR(0.01)", "Action", "Breakout Count" };
	 */

	// Add these near the top of your class with other constants
	private static final Set<String> BULLISH_STATUSES = Set.of("Bullish Crossover", "Bullish", "Strong Uptrend",
			"Mild Uptrend", "↑ Uptrend", "Buy", "Uptrend");

	private Set<String> selectedTimeframes = new HashSet<>(
			Arrays.asList("1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min"));

	private static final String BREAKOUT_COUNT_COLUMN_PATTERN = "Breakout";

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
	private final Map<String, List<String>> watchlists = new HashMap<>();
	private String currentWatchlist = "Default";
	private String currentDataSource = "Yahoo";
	private int currentRefreshInterval = 60; // Default interval in seconds

	private List<String> watchListException = Arrays.asList("Default", "YahooDOW", "YahooNASDAQ", "YahooSP500",
			"YahooRussel2k", "YahooPennyBy3MonVol", "YahooPennyByPercentChange", "TradingViewPennyStockPreMarket",
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

	private JTextField prevVolMinField;
	private JTextField prevVolMaxField;
	private JTextField currentVolMinField;
	private JTextField currentVolMaxField;


	YahooFinanceService yahooQueryFinance = new YahooFinanceService();

	TradingViewService tradingViewService = new TradingViewService();

	private JPanel controlPanel;
	private JButton listSymbolsButton;
	private JLabel statusLabel;
	private JButton checkNonBullishButton;

	private Map<String, Set<String>> timeframeIndicators = new LinkedHashMap<>();
	private Map<String, JCheckBox> timeframeCheckboxes = new HashMap<>();
	private Map<String, Map<String, JCheckBox>> indicatorCheckboxes = new HashMap<>();
	private final Map<String, JComboBox<String>> indicatorTimeRangeCombos = new HashMap<>();
	private final Map<String, JComboBox<String>> indicatorSessionCombos = new HashMap<>();
	private Map<String, Map<String, Integer>> timeframeIndexRanges = new HashMap<>();
	private Map<String, Map<String, JTextField>> timeframeIndexFields = new HashMap<>();

	private JPanel bullishTimeframePanel;
	private Map<String, JCheckBox> bullishTimeframeCheckboxes = new HashMap<>();

	private JCheckBox inheritCheckbox;
	private MessageManager messageManager;
	private ConfigManager configManager;

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

	//main table
	ConcurrentHashMap<String, PriceData> priceDataMap = new ConcurrentHashMap<String, PriceData>();
	
	//zscore on table
	private final ZScoreColumnHelper zScoreColumnHelper;	
	private ZScoreRankingGUI zScoreRankingGUI;
	private ZScoreAlertManager zScoreAlertManager;
	private final Map<String, JCheckBox> zScoreCheckboxes = new HashMap<>();
	private ZScoreAlertConfigDialog zScoreAlertConfigDialog;

	/*public class FilterableTableModel extends DefaultTableModel {
		private final Set<String> hiddenSymbols;
		private final Map<String, Integer> symbolToRowMap;
		private Map<String, double[]> columnFilters = new HashMap<>();

		// private Vector<String> columnIdentifiers;

		public FilterableTableModel(Vector<Vector<Object>> data, Vector<String> columnNames) {
			super(data, columnNames);
			this.hiddenSymbols = Collections.synchronizedSet(new HashSet<>());
			this.symbolToRowMap = new ConcurrentHashMap<>();
		}

		public FilterableTableModel(Object[] columnNames, int rowCount) {
			super(columnNames, rowCount);
			this.hiddenSymbols = Collections.synchronizedSet(new HashSet<>());
			this.symbolToRowMap = new ConcurrentHashMap<>();
			this.columnIdentifiers = new Vector<>(Arrays.asList((String[]) columnNames));
		}

		public void setColumnFilters(Map<String, double[]> newFilters) {
			this.columnFilters = newFilters != null ? new HashMap<>(newFilters) : new HashMap<>();
		}

		@Override
		public synchronized int getRowCount() {
			try {
				return super.getRowCount() - (hiddenSymbols != null ? hiddenSymbols.size() : 0);
			} catch (Exception e) {
				logToConsole("Error in getRowCount: " + e.getMessage());
				return 0;
			}
		}

		@Override
		public synchronized Object getValueAt(int row, int column) {
			try {
				int modelRow = convertViewRowToModel(row);
				return super.getValueAt(modelRow, column);
			} catch (Exception e) {
				logToConsole("Error in getValueAt: " + e.getMessage());
				return null;
			}
		}

		private synchronized int convertViewRowToModel(int viewRow) {
			try {
				int visibleCount = -1;
				for (int i = 0; i < super.getRowCount(); i++) {
					String symbol = (String) super.getValueAt(i, 1);
					if (symbol != null && !hiddenSymbols.contains(symbol)) {
						visibleCount++;
						if (visibleCount == viewRow) {
							return i;
						}
					}
				}
				return -1;
			} catch (Exception e) {
				logToConsole("Error in convertViewRowToModel: " + e.getMessage());
				return -1;
			}
		}

		@Override
		public synchronized void addRow(Object[] rowData) {
			try {
				super.addRow(rowData);
				if (rowData != null && rowData.length > 1 && rowData[1] instanceof String) {
					String symbol = (String) rowData[1];
					symbolToRowMap.put(symbol, super.getRowCount() - 1);
				}
			} catch (Exception e) {
				logToConsole("Error adding row: " + e.getMessage());
			}
		}

		@Override
		public synchronized void removeRow(int row) {
			try {
				String symbol = (String) getValueAt(row, 1);
				super.removeRow(row);
				if (symbol != null) {
					symbolToRowMap.remove(symbol);
					hiddenSymbols.remove(symbol);
				}
				rebuildSymbolMap();
			} catch (Exception e) {
				logToConsole("Error removing row: " + e.getMessage());
			}
		}

		private synchronized void rebuildSymbolMap() {
			try {
				symbolToRowMap.clear();
				for (int i = 0; i < super.getRowCount(); i++) {
					Object value = super.getValueAt(i, 1);
					if (value instanceof String) {
						symbolToRowMap.put((String) value, i);
					}
				}
			} catch (Exception e) {
				logToConsole("Error rebuilding symbol map: " + e.getMessage());
			}
		}

		public synchronized void toggleSymbolsVisibility(Set<String> symbolsToToggle) {
			try {
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
			} catch (Exception e) {
				logToConsole("Error in toggleSymbolsVisibility: " + e.getMessage());
			}
		}

		public synchronized boolean isSymbolHidden(String symbol) {
			try {
				return hiddenSymbols != null && hiddenSymbols.contains(symbol);
			} catch (Exception e) {
				logToConsole("Error in isSymbolHidden: " + e.getMessage());
				return false;
			}
		}

		public synchronized void showAllSymbols() {
			try {
				if (hiddenSymbols != null) {
					hiddenSymbols.clear();
					fireTableDataChanged();
				}
			} catch (Exception e) {
				logToConsole("Error in showAllSymbols: " + e.getMessage());
			}
		}

		@Override
		public synchronized boolean isCellEditable(int row, int column) {
			try {
				int modelRow = convertViewRowToModel(row);
				return modelRow != -1 && column == 0;
			} catch (Exception e) {
				logToConsole("Error in isCellEditable: " + e.getMessage());
				return false;
			}
		}

		@Override
		public synchronized Class<?> getColumnClass(int column) {
			
			String colName = getColumnName(column);
		    
		    // Z-Score column detection
		    if (colName != null) {
		        if (colName.startsWith("ZScore_")) {
		            if (colName.endsWith("_Rank")) {
		                return Integer.class;  // Use Integer for ranks
		            } else if (colName.endsWith("_ZScore")) {
		                return Double.class;   // Use Double for Z-Scores
		            }
		        }
		    }

			List<String> datacolumns = new ArrayList<>(Arrays.asList("postmarketClose", "premarketChange",
					"changeFromOpen", "percentChange", "postmarketChange", "gap", "premarketVolume", "currentVolume",
					"postmarketVolume", "previousVolume", "prevLastDayPrice", "averageVol", "analystRating",
					"premarketHigh", "high", "postmarketHigh", "premarketLow", "low", "postmarketLow",
					"premarketHighestPercentile", "marketHighestPercentile", "postmarketHighestPercentile"));

			if (colName == null) {
				return String.class; // Fallback
			}
			if (colName.equals("Select") || colName.equals("Checkbox")) {
				return Boolean.class;
			} else if (colName.equals("Symbol")) {
				return String.class;
			} else if (colName.equals("Price") || colName.equals("% Change") || colName.contains("Change")) {
				return Double.class;
			} else if (colName.equals("Volume") || colName.equals("Prev Volume") || colName.contains("Vol")) {
				return Long.class;
			} else if (colName.contains("Breakout") || colName.contains("Breakout Count")) {
				return BreakoutValue.class;
			} else if (datacolumns.contains(colName)) {
				return Double.class;
			}
			// Add more mappings for other specific columns if needed (e.g., RSI, MACD)
			return String.class;
		}

		@Override
		public synchronized void setValueAt(Object value, int row, int column) {
		    try {
		        String colName = getColumnName(column);
		        
		        // Handle Z-Score columns specifically
		        if (colName != null && colName.startsWith("ZScore_")) {
		            int modelRow = convertViewRowToModel(row);
		            if (modelRow != -1) {
		                // Ensure data type consistency
		                if (colName.endsWith("_Rank") && value instanceof Double) {
		                    value = ((Double) value).intValue();
		                }
		                super.setValueAt(value, modelRow, column);
		            }
		        } else {
		            // Existing logic for other columns
		            int modelRow = convertViewRowToModel(row);
		            if (modelRow != -1) {
		                super.setValueAt(value, modelRow, column);
		            }
		        }
		    } catch (Exception e) {
		        logToConsole("Error in setValueAt: " + e.getMessage());
		    }
		}

		// Update existing methods to use columnIdentifiers
		@Override
		public String getColumnName(int column) {
			return (String) this.columnIdentifiers.get(column);
		}

		@Override
		public int getColumnCount() {
			return columnIdentifiers.size();
		}

		// Add this method to remove columns
		public void removeColumn(int column) {
			try {
				// Remove from column identifiers
				columnIdentifiers.remove(column);

				// Remove from each row's data
				for (Object rowObj : dataVector) {
					if (rowObj instanceof Vector) {
						((Vector<?>) rowObj).remove(column);
					}
				}

				// Update column count by calling super's method
				// We don't need to directly modify columnCount since it's handled by the model
				fireTableStructureChanged();

				// Rebuild symbol map if needed
				if (column == 1) { // If removing symbol column
					rebuildSymbolMap();
				}

				// New: Refresh sorter (assuming dashboardTable is accessible; otherwise, expose
				// it)
				if (dashboardTable != null && dashboardTable.getRowSorter() instanceof TableRowSorter) {
					TableRowSorter<FilterableTableModel> sorter = (TableRowSorter<FilterableTableModel>) dashboardTable
							.getRowSorter();
					sorter.setModel(this); // Rebind model to sorter
					sorter.modelStructureChanged(); // Force sorter update
				}

				SwingUtilities.invokeLater(() -> updateTableRenderers());
			} catch (ArrayIndexOutOfBoundsException e) {
				logToConsole("Error removing column: " + e.getMessage());
			}
		}

		public Vector<?> getColumnIdentifiers() {
			return this.columnIdentifiers;
		}

		public boolean rowSatisfiesFilters(int modelRow) {
			for (Map.Entry<String, double[]> entry : columnFilters.entrySet()) {
				String colName = entry.getKey();
				int col = findColumn(colName);
				if (col == -1)
					continue;

				Object val = super.getValueAt(modelRow, col);
				if (val == null)
					return false;

				double v;
				if (val instanceof Number) {
					v = ((Number) val).doubleValue();
				} else {
					return false;
				}

				double[] range = entry.getValue();
				double min = range[0];
				double max = range[1];

				if (v < min || v > max) {
					return false;
				}
			}
			return true;
		}
	}
	*/
	public StockDashboardv1_22_5() {

		this.dataProviderFactory = new StockDataProviderFactory(client, apiKey, apiSecret, polygonApiKey);
		this.currentDataProvider = dataProviderFactory.getProvider(currentDataSource, this::logToConsole);
		
		//zscale
		this.zScoreColumnHelper = new ZScoreColumnHelper(this);
		this.zScoreAlertManager = new ZScoreAlertManager(this);

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
		watchlists.put("Default", Arrays.asList(symbols));
		watchlists.put("Tech Stocks", Arrays.asList("AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "META", "TSLA", "NFLX"));
		watchlists.put("Tech Stocks - Application",
				Arrays.asList("SAP", "CRM", "INTU", "NOW", "UBER", "SHOP", "ADBE", "ADP", "MSTR", "CDNS", "SNOW",
						"ADSK", "WDAY", "ROP", "TEAM", "PAYX", "DDOG", "FICO", "ANSS", "HUBS", "PTC", "TYL", "ZM",
						"GRAB", "SOUN", "WCT", "YMM", "BULL", "LYFT", "UBER", "PD", "WRD", "U", "YAAS", "SHOP",
						"FRSH", "AAPL", "BNZI", "KC", "YB", "LIF", "DUOL", "KARO", "BILL", "AEYE", "CVLT", "SPT", "IBTA", "DT", "DFIN", "BTDR", "MAPS",
						"WYFI", "PCOR", "CRNC", "GTM"));
		watchlists.put("Tech Stocks - Infrastructure",
				Arrays.asList("LIDR", "PLTR", "MSFT", "PATH", "CORZ", "CRWV", "OKTA", "AI", "TOST", "ORCL", "RZLV", "S",
						"XYZ", "S", "PANW", "CRWD", "SNPS", "FTNT", "NET", "CRWV", "XYZ", "ZS", "VRSN", "CHKP", "CPAY",
						"GDDY", "IOT", "AFRM", "GPN", "NTNX", "TWLO", "MDB", "CYBR", "DOX", "GTLB", "ATGL", "MCRP", "ODD", "CALX", "RBRK", "BB",
						"GRRR", "BKKT", "NUAI", "SUPX", "DFDV", "DVLT", "STNE", "CORZ", "PAGS", "ZETA", "NBIS", "XYZ", "CLBT", "SAIL"));
		watchlists.put("ETF", Arrays.asList("XLK", "QQQ", "SPY", "IGV", "VGT", "FTEC", "SMH", "ARKQ","CONL","GBTC","SOXL"));
		watchlists.put("Blue Chips", Arrays.asList("WMT", "JPM", "V", "MA", "XOM", "JNJ", "HD"));
		watchlists.put("Financial",
				Arrays.asList("V", "JPM", "MA", "AXP", "BAC", "COIN", "BLK", "WFC", "BLK", "MS", "C", "BX", "SOFI"));
		watchlists.put("SemiConductors",
				Arrays.asList("NVDA", "AVGO", "AMD", "TSM", "QCOM", "ARM", "MRVL", "NXPI", "MCHP", "ADI", "TXN", "MU",
						"INTC", "ON", "SWKS", "WOLF", "STM", "TXN", "LRCX", "MCHP", "COHR", "AMAT", "TER", "MTSI",
						"QRVO", "ONTO", "MPWR", "CRUS", "ENTG", "GFS", "AMAT", "AMKR", "LSCC", "ADI", "NXPI", "KLAC", "SMTC", "SKYT", "LAES", "POET",
						"PXLW", "LASR", "RMBS", "POWI", "VSH"));
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

		watchlists.put("YahooEcoIndex", Arrays.asList("^DJI", "^GSPC", "^IXIC", "^RUT", "SOXL", "SMH", "XLK"));
		watchlists.put("YahooDOW", Arrays.asList(""));
		watchlists.put("YahooNASDAQ", Arrays.asList(""));
		watchlists.put("YahooSP500", Arrays.asList(""));
		watchlists.put("YahooRussel2k", Arrays.asList(""));
		watchlists.put("YahooPennyBy3MonVol", Arrays.asList(""));
		watchlists.put("YahooPennyByPercentChange", Arrays.asList(""));

		watchlists.put("TradingViewPennyStockPreMarket", Arrays.asList(""));
		watchlists.put("TradingViewPennyStockStandardMarket", Arrays.asList(""));
		watchlists.put("TradingViewPennyStockPostMarket", Arrays.asList(""));
		watchlists.put("TradingViewIndexStocksByPreMarketVolume", Arrays.asList(""));
		watchlists.put("TradingViewIndexStocksByStandardMarketVolume", Arrays.asList(""));
		watchlists.put("TradingViewIndexStocksMarketByPostMarketVolume", Arrays.asList(""));

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
			
			// Initialize session combobox
	        JComboBox<String> sessionCombo = new JComboBox<>(new String[]{"all", "premarket", "standard", "postmarket"});
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

		// Add new configuration items
		JMenuItem saveConfigItem = new JMenuItem("Save Dashboard and Settings Conf");
		saveConfigItem.addActionListener(e -> saveConfigurationToFile());
		fileMenu.add(saveConfigItem);

		JMenuItem loadConfigItem = new JMenuItem("Load Dashboard and Settings Conf");
		loadConfigItem.addActionListener(e -> loadConfigurationFromFile());
		fileMenu.add(loadConfigItem);

		menuBar.add(fileMenu);
		
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
		    /*if (zScoreRankingGUI != null) {
		        zScoreRankingGUI.dispose();
		    }*/
		    		    
		    zScoreRankingGUI = new ZScoreRankingGUI(priceDataList);
		    zScoreRankingGUI.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		    zScoreRankingGUI.setVisible(true);
		    
		});
		zScoreMenu.add(zScoreManagementItem);
		menuBar.add(zScoreMenu);
		
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
	
	//zscores
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
	            FilterableTableModel newModel = new FilterableTableModel(newColumnNames.toArray(new String[0]), 0, this);

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
	            refreshData();

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
			Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_5.class);
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
			Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_5.class);
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
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save Configuration");
		fileChooser.setSelectedFile(new File("dashboard_config.json"));

		int userSelection = fileChooser.showSaveDialog(this);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToSave = fileChooser.getSelectedFile();
			if (!fileToSave.getName().toLowerCase().endsWith(".json")) {
				fileToSave = new File(fileToSave.getAbsolutePath() + ".json");
			}

			try {
				JSONObject config = new JSONObject();

				// Save time range settings
				config.put("currentTimeRadio", currentTimeRadio != null && currentTimeRadio.isSelected());
				config.put("customRangeRadio", customRangeRadio != null && customRangeRadio.isSelected());
				if (startDatePicker != null && startDatePicker.getDate() != null) {
					config.put("startDate", startDatePicker.getDate().getTime());
				}
				if (endDatePicker != null && endDatePicker.getDate() != null) {
					config.put("endDate", endDatePicker.getDate().getTime());
				}

				// Save volume filters
				config.put("prevVolMin", prevVolMinField != null ? prevVolMinField.getText() : "");
				config.put("prevVolMax", prevVolMaxField != null ? prevVolMaxField.getText() : "");
				config.put("currentVolMin", currentVolMinField != null ? currentVolMinField.getText() : "");
				config.put("currentVolMax", currentVolMaxField != null ? currentVolMaxField.getText() : "");

				// Save live update settings
				config.put("checkBullish", checkBullishCheckbox != null && checkBullishCheckbox.isSelected());
				config.put("alarmOn", alarmOnCheckbox != null && alarmOnCheckbox.isSelected());
				config.put("refreshInterval",
						refreshIntervalComboBox != null ? refreshIntervalComboBox.getSelectedItem() : 60);
				config.put("symbolExceptions", symbolExceptionsField != null ? symbolExceptionsField.getText() : "");
				
				// Save Volume Alert Configuration
	            JSONObject volumeAlertConfigJson = new JSONObject();
	            volumeAlertConfigJson.put("enabled", volumeAlertConfig.isEnabled());
	            volumeAlertConfigJson.put("volume20MAEnabled", volumeAlertConfig.isVolume20MAEnabled());
	            volumeAlertConfigJson.put("onlyBullishSymbols", volumeAlertConfig.isOnlyBullishSymbols());
	            
	            JSONObject timeframeConfigsJson = new JSONObject();
	            for (Map.Entry<String, VolumeAlertTimeframeConfig> entry : volumeAlertConfig.getTimeframeConfigs().entrySet()) {
	                JSONObject timeframeConfigJson = new JSONObject();
	                timeframeConfigJson.put("enabled", entry.getValue().isEnabled());
	                timeframeConfigJson.put("percentage", entry.getValue().getPercentage());
	                timeframeConfigsJson.put(entry.getKey(), timeframeConfigJson);
	            }
	            volumeAlertConfigJson.put("timeframeConfigs", timeframeConfigsJson);
	            
	            config.put("volumeAlertConfig", volumeAlertConfigJson);

				// Save live update time restrictions
				if (startTimeSpinner != null && endTimeSpinner != null) {
					Date startTime = ((SpinnerDateModel) startTimeSpinner.getModel()).getDate();
					Date endTime = ((SpinnerDateModel) endTimeSpinner.getModel()).getDate();
					config.put("liveUpdateStartTime", startTime.getTime());
					config.put("liveUpdateEndTime", endTime.getTime());
				}
				
				// Save Z-Score state if the window is open
				if (zScoreRankingGUI != null && zScoreRankingGUI.isVisible()) {
				    JSONObject zScoreState = zScoreRankingGUI.exportState();
				    config.put("zScoreState", zScoreState);
				}
				
				// Save Z-Score Alert Configuration
	            JSONObject zScoreAlertConfigJson = new JSONObject();
	            zScoreAlertConfigJson.put("enabled", zScoreAlertManager.isEnabled());
	            
	            JSONObject zScoreAlertConfigsJson = new JSONObject();
	            Map<String, ZScoreAlertConfig> alertConfigs = zScoreAlertManager.getAlertConfigs();
	            for (Map.Entry<String, ZScoreAlertConfig> entry : alertConfigs.entrySet()) {
	                JSONObject alertConfigJson = new JSONObject();
	                alertConfigJson.put("enabled", entry.getValue().isEnabled());
	                alertConfigJson.put("monitorRange", entry.getValue().getMonitorRange());
	                alertConfigJson.put("monteCarloEnabled", entry.getValue().isMonteCarloEnabled());
	                alertConfigJson.put("alarmOn", entry.getValue().isAlarmOn());
	                alertConfigJson.put("previousTopSymbols", entry.getValue().getPreviousTopSymbols() != null ? entry.getValue().getPreviousTopSymbols() : "");
	                zScoreAlertConfigsJson.put(entry.getKey(), alertConfigJson);
	            }
	            zScoreAlertConfigJson.put("alertConfigs", zScoreAlertConfigsJson);
	            
	            config.put("zScoreAlertConfig", zScoreAlertConfigJson);

				// Save indicator configurations
				JSONObject indicatorConfig = new JSONObject();
				for (String tf : allTimeframes) {
					JSONObject tfConfig = new JSONObject();
					Set<String> indicators = timeframeIndicators.get(tf);
					tfConfig.put("enabled", indicators != null && !indicators.isEmpty());

					JSONArray indicatorArray = new JSONArray();
					for (String ind : allIndicators) {
						if (indicators != null && indicators.contains(ind)) {
							indicatorArray.put(ind);
						}
					}
					tfConfig.put("indicators", indicatorArray);
					// Save index ranges
					JSONObject indexRange = new JSONObject();
					indexRange.put("startIndex", timeframeIndexRanges.get(tf).get("startIndex"));
					indexRange.put("endIndex", timeframeIndexRanges.get(tf).get("endIndex"));
					tfConfig.put("indexRange", indexRange);
					indicatorConfig.put(tf, tfConfig);
				}
				config.put("indicatorConfig", indicatorConfig);
				config.put("uptrendFilterEnabled", uptrendFilterEnabled);

				// Save watchlist settings
				config.put("currentWatchlist", currentWatchlist != null ? currentWatchlist : "");
				config.put("currentDataSource", currentDataSource != null ? currentDataSource : "");

				// Save logger settings
				config.put("enableSaveLog", enableSaveLogCheckbox != null && enableSaveLogCheckbox.isSelected());
				config.put("logDirectory", logDirectoryField != null ? logDirectoryField.getText() : "");
				config.put("logFileName", logFileNameField != null ? logFileNameField.getText() : "");
				config.put("enableLogReports",
						enableLogReportsCheckbox != null && enableLogReportsCheckbox.isSelected());
				config.put("completeResults", completeResultsCheckbox != null && completeResultsCheckbox.isSelected());
				config.put("bullishChange", bullishChangeCheckbox != null && bullishChangeCheckbox.isSelected());
				config.put("bullishVolume", bullishVolumeCheckbox != null && bullishVolumeCheckbox.isSelected());

				// Save column visibility as an array to preserve order
				JSONArray columnVisibilityArray = new JSONArray();
				for (Object colName : tableModel.getColumnIdentifiers()) {
					JSONObject columnEntry = new JSONObject();
					int colIndex = findColumnIndex((String) colName);
					boolean visible = colIndex != -1
							&& dashboardTable.getColumnModel().getColumn(colIndex).getWidth() > 0;
					columnEntry.put("name", colName);
					columnEntry.put("visible", visible);
					columnVisibilityArray.put(columnEntry);
				}
				config.put("columnVisibility", columnVisibilityArray);
				
				// NEW: Save Z-Score column visibility
	            JSONObject zScoreColumnVisibility = new JSONObject();
	            for (Map.Entry<String, JCheckBox> entry : zScoreCheckboxes.entrySet()) {
	                zScoreColumnVisibility.put(entry.getKey(), entry.getValue().isSelected());
	            }
	            config.put("zScoreColumnVisibility", zScoreColumnVisibility);

				// Save checkbox states for specific PriceData columns
				config.put("showAveVolume", showAveVolumeCheckBox != null && showAveVolumeCheckBox.isSelected());
				config.put("showPrevLastDayPrice",
						showPrevLastDayPriceCheckBox != null && showPrevLastDayPriceCheckBox.isSelected());
				
				

				// Write to file
				try (PrintWriter out = new PrintWriter(fileToSave)) {
					out.println(config.toString(4)); // Pretty print with 4-space indent
				}

				JOptionPane.showMessageDialog(this, "Configuration saved successfully!", "Success",
						JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "Error saving configuration: " + e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
		}
	}

	private void loadConfigurationFromFile() {
		
		initializeZScoreCheckboxes();
		
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Load Configuration");

		int userSelection = fileChooser.showOpenDialog(this);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToLoad = fileChooser.getSelectedFile();

			try {
				String content = new String(Files.readAllBytes(fileToLoad.toPath()));
				JSONObject config = new JSONObject(content);

				// Clear timeframeIndicators and initialize for all timeframes
				timeframeIndicators.clear();
				for (String tf : allTimeframes) {
					timeframeIndicators.put(tf, new HashSet<>());
				}

				// Load time range settings
				currentTimeRadio
						.setSelected(config.has("currentTimeRadio") ? config.getBoolean("currentTimeRadio") : false);
				customRangeRadio
						.setSelected(config.has("customRangeRadio") ? config.getBoolean("customRangeRadio") : false);
				if (config.has("startDate")) {
					startDatePicker.setDate(new Date(config.getLong("startDate")));
				}
				if (config.has("endDate")) {
					endDatePicker.setDate(new Date(config.getLong("endDate")));
				}

				// Load volume filters
				prevVolMinField.setText(config.has("prevVolMin") ? config.getString("prevVolMin") : "");
				prevVolMaxField.setText(config.has("prevVolMax") ? config.getString("prevVolMax") : "");
				currentVolMinField.setText(config.has("currentVolMin") ? config.getString("currentVolMin") : "");
				currentVolMaxField.setText(config.has("currentVolMax") ? config.getString("currentVolMax") : "");

				// Load live update settings
				checkBullishCheckbox
						.setSelected(config.has("checkBullish") ? config.getBoolean("checkBullish") : false);
				alarmOnCheckbox.setSelected(config.has("alarmOn") ? config.getBoolean("alarmOn") : false);
				refreshIntervalComboBox
						.setSelectedItem(config.has("refreshInterval") ? config.getInt("refreshInterval") : 60);
				symbolExceptionsField
						.setText(config.has("symbolExceptions") ? config.getString("symbolExceptions") : "");

				if (config.has("liveUpdateStartTime") && config.has("liveUpdateEndTime")) {
					Date startTime = new Date(config.getLong("liveUpdateStartTime"));
					Date endTime = new Date(config.getLong("liveUpdateEndTime"));
					((SpinnerDateModel) startTimeSpinner.getModel()).setValue(startTime);
					((SpinnerDateModel) endTimeSpinner.getModel()).setValue(endTime);
				}
				
				// Load Volume Alert Configuration
	            if (config.has("volumeAlertConfig")) {
	                JSONObject volumeAlertConfigJson = config.getJSONObject("volumeAlertConfig");
	                
	                volumeAlertConfig.setEnabled(volumeAlertConfigJson.optBoolean("enabled", false));
	                volumeAlertConfig.setVolume20MAEnabled(volumeAlertConfigJson.optBoolean("volume20MAEnabled", false));
	                volumeAlertConfig.setOnlyBullishSymbols(volumeAlertConfigJson.optBoolean("onlyBullishSymbols", true));
	                
	                if (volumeAlertConfigJson.has("timeframeConfigs")) {
	                    JSONObject timeframeConfigsJson = volumeAlertConfigJson.getJSONObject("timeframeConfigs");
	                    
	                    for (String tf : allTimeframes) {
	                        if (timeframeConfigsJson.has(tf)) {
	                            JSONObject timeframeConfigJson = timeframeConfigsJson.getJSONObject(tf);
	                            boolean enabled = timeframeConfigJson.optBoolean("enabled", false);
	                            double percentage = timeframeConfigJson.optDouble("percentage", 0.0);
	                            
	                            VolumeAlertTimeframeConfig timeframeConfig = new VolumeAlertTimeframeConfig(enabled, percentage);
	                            volumeAlertConfig.getTimeframeConfigs().put(tf, timeframeConfig);
	                        } else {
	                            // Initialize with defaults if not found
	                            volumeAlertConfig.getTimeframeConfigs().put(tf, new VolumeAlertTimeframeConfig(false, 0.0));
	                        }
	                    }
	                }
	                
	                logToConsole("Loaded volume alert configuration");
	            }

				// Load indicator configurations and populate timeframeIndicators and checkboxes
				if (config.has("indicatorConfig")) {
					JSONObject indicatorConfig = config.getJSONObject("indicatorConfig");
					System.out.println("Loading indicatorConfig: " + indicatorConfig.toString(2));
					for (String tf : allTimeframes) {
						Set<String> indicators = timeframeIndicators.get(tf);
						JCheckBox timeframeCheckbox = timeframeCheckboxes.get(tf);
						Map<String, JCheckBox> indCheckboxes = indicatorCheckboxes.get(tf);

						// Reset checkboxes to unselected
						if (timeframeCheckbox != null) {
							timeframeCheckbox.setSelected(false);
						}
						if (indCheckboxes != null) {
							for (JCheckBox indCheckbox : indCheckboxes.values()) {
								indCheckbox.setSelected(false);
							}
						}

						if (indicatorConfig.has(tf)) {
							JSONObject tfConfig = indicatorConfig.getJSONObject(tf);
							boolean enabled = tfConfig.has("enabled") ? tfConfig.getBoolean("enabled") : false;
							if (timeframeCheckbox != null) {
								timeframeCheckbox.setSelected(enabled);
							} else {
								System.err.println("Warning: Timeframe checkbox for '" + tf + "' not found, skipping.");
							}
							indicators.clear();
							if (enabled && tfConfig.has("indicators")) {
								if (indCheckboxes == null) {
									System.err.println("Warning: Indicator checkboxes for timeframe '" + tf
											+ "' not found, skipping.");
									continue;
								}
								JSONArray indArray = tfConfig.getJSONArray("indicators");
								System.out.println("Indicators for " + tf + ": " + indArray.toString());
								for (int i = 0; i < indArray.length(); i++) {
									String ind = indArray.getString(i);
									if (Arrays.asList(allIndicators).contains(ind)) {
										indicators.add(ind);
										JCheckBox indCheckbox = indCheckboxes.get(ind);
										if (indCheckbox != null) {
											indCheckbox.setSelected(true);
										} else {
											System.err.println("Warning: Checkbox for indicator '" + ind
													+ "' in timeframe '" + tf + "' not found, skipping.");
										}
									} else {
										System.err
												.println("Warning: Indicator '" + ind + "' not recognized, skipping.");
									}
								}
							}

							// Load index ranges
							if (tfConfig.has("indexRange")) {
								JSONObject indexRange = tfConfig.getJSONObject("indexRange");
								int startIndex = indexRange.getInt("startIndex");
								int endIndex = indexRange.getInt("endIndex");
								timeframeIndexRanges.get(tf).put("startIndex", startIndex);
								timeframeIndexRanges.get(tf).put("endIndex", endIndex);
								timeframeIndexFields.get(tf).get("startIndex").setText(String.valueOf(startIndex));
								timeframeIndexFields.get(tf).get("endIndex").setText(String.valueOf(endIndex));
							}
						}
					}
					// Log timeframeIndicators and checkbox states for debugging
					System.out.println("Loaded timeframeIndicators: " + timeframeIndicators);
					System.out.println("Timeframe checkbox states: "
							+ timeframeCheckboxes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
									e -> e.getValue() != null ? e.getValue().isSelected() : false)));
					System.out.println("Indicator checkbox states: " + indicatorCheckboxes.entrySet().stream()
							.collect(Collectors.toMap(Map.Entry::getKey,
									e -> e.getValue().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
											ie -> ie.getValue() != null ? ie.getValue().isSelected() : false)))));
				}

				// Load uptrend filter setting
				uptrendFilterEnabled = config.has("uptrendFilterEnabled") ? config.getBoolean("uptrendFilterEnabled")
						: false;
				if (uptrendFilterCheckbox != null) {
					uptrendFilterCheckbox.setSelected(uptrendFilterEnabled);
				} else {
					System.err.println("Warning: uptrendFilterCheckbox is null, skipping setting its state.");
				}

				// Load watchlist settings
				currentWatchlist = config.has("currentWatchlist") ? config.getString("currentWatchlist") : "";
				watchlistComboBox.setSelectedItem(currentWatchlist);
				currentDataSource = config.has("currentDataSource") ? config.getString("currentDataSource") : "";
				dataSourceComboBox.setSelectedItem(currentDataSource);

				// Load logger settings
				enableSaveLogCheckbox
						.setSelected(config.has("enableSaveLog") ? config.getBoolean("enableSaveLog") : false);
				logDirectoryField.setText(config.has("logDirectory") ? config.getString("logDirectory") : "");
				logFileNameField.setText(config.has("logFileName") ? config.getString("logFileName") : "");
				enableLogReportsCheckbox
						.setSelected(config.has("enableLogReports") ? config.getBoolean("enableLogReports") : false);
				completeResultsCheckbox
						.setSelected(config.has("completeResults") ? config.getBoolean("completeResults") : false);
				bullishChangeCheckbox
						.setSelected(config.has("bullishChange") ? config.getBoolean("bullishChange") : false);
				bullishVolumeCheckbox
						.setSelected(config.has("bullishVolume") ? config.getBoolean("bullishVolume") : false);

				// Update logger fields' enabled state
				boolean logEnabled = enableSaveLogCheckbox.isSelected();
				logDirectoryField.setEnabled(logEnabled);
				logFileNameField.setEnabled(logEnabled);
				boolean reportsEnabled = enableLogReportsCheckbox.isSelected();
				completeResultsCheckbox.setEnabled(reportsEnabled);
				bullishChangeCheckbox.setEnabled(reportsEnabled);
				bullishVolumeCheckbox.setEnabled(reportsEnabled);

				// Load column visibility
				if (config.has("columnVisibility")) {
					tableModel.setColumnIdentifiers(new Vector<>());
					tableModel.fireTableStructureChanged();

					Vector<Object> newColumnIdentifiers = new Vector<>();
					Map<String, Boolean> visibilityMap = new HashMap<>();

					Object columnVisibilityObj = config.get("columnVisibility");
					if (columnVisibilityObj instanceof JSONArray) {
						JSONArray columnVisibilityArray = (JSONArray) columnVisibilityObj;
						System.out.println("Loading columnVisibility (array): " + columnVisibilityArray.toString(2));
						for (int i = 0; i < columnVisibilityArray.length(); i++) {
							JSONObject columnEntry = columnVisibilityArray.getJSONObject(i);
							String colName = columnEntry.getString("name");
							boolean visible = columnEntry.getBoolean("visible");
							newColumnIdentifiers.add(colName);
							visibilityMap.put(colName, visible);
						}
					} else if (columnVisibilityObj instanceof JSONObject) {
						JSONObject columnVisibility = (JSONObject) columnVisibilityObj;
						System.out.println(
								"Loading columnVisibility (object, old format): " + columnVisibility.toString(2));
						System.err.println(
								"Warning: Old columnVisibility format detected. Column order may not be preserved.");
						for (String colName : columnVisibility.keySet()) {
							newColumnIdentifiers.add(colName);
							visibilityMap.put(colName, columnVisibility.getBoolean(colName));
						}
					}

					tableModel.setColumnIdentifiers(newColumnIdentifiers);
					System.out.println("Ordered columns from JSON: " + newColumnIdentifiers);

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

					System.out.println("Final table columns:");
					for (int i = 0; i < tableModel.getColumnCount(); i++) {
						System.out.println("Column " + i + ": " + tableModel.getColumnName(i));
					}
				}
				
				// NEW: Load Z-Score column visibility
	            if (config.has("zScoreColumnVisibility")) {
	                JSONObject zScoreVisibility = config.getJSONObject("zScoreColumnVisibility");
	                for (String key : zScoreVisibility.keySet()) {
	                    boolean isVisible = zScoreVisibility.getBoolean(key);
	                    if (zScoreCheckboxes.containsKey(key)) {
	                        zScoreCheckboxes.get(key).setSelected(isVisible);
	                    }
	                }
	                // Apply the loaded Z-Score column settings
	                updateZScoreColumns();
	            }
	            
				// Load Z-Score state if available
				if (config.has("zScoreState")) {
					JSONObject zScoreState = config.getJSONObject("zScoreState");
					if (zScoreRankingGUI != null && zScoreRankingGUI.isVisible()) {
						zScoreRankingGUI.importState(zScoreState);
					}
				}
		         
				// Load Z-Score Alert Configuration
	            if (config.has("zScoreAlertConfig")) {
	                JSONObject zScoreAlertConfigJson = config.getJSONObject("zScoreAlertConfig");
	                
	                boolean zScoreAlertEnabled = zScoreAlertConfigJson.optBoolean("enabled", false);
	                zScoreAlertManager.setEnabled(zScoreAlertEnabled);
	                
	                if (zScoreAlertConfigJson.has("alertConfigs")) {
	                    JSONObject alertConfigsJson = zScoreAlertConfigJson.getJSONObject("alertConfigs");
	                    Map<String, ZScoreAlertConfig> newAlertConfigs = new HashMap<>();
	                    
	                    for (String columnName : alertConfigsJson.keySet()) {
	                        JSONObject alertConfigJson = alertConfigsJson.getJSONObject(columnName);
	                        boolean enabled = alertConfigJson.optBoolean("enabled", false);
	                        int monitorRange = alertConfigJson.optInt("monitorRange", 5);
	                        boolean monteCarloEnabled = alertConfigJson.optBoolean("monteCarloEnabled", false);
	                        boolean alarmOn = alertConfigJson.optBoolean("alarmOn", false);
	                        String previousTopSymbols = alertConfigJson.optString("previousTopSymbols", "");
	                        
	                        ZScoreAlertConfig alertConfig = new ZScoreAlertConfig(enabled, monitorRange);
	                        alertConfig.setMonteCarloEnabled(monteCarloEnabled);
	                        alertConfig.setAlarmOn(alarmOn);
	                        alertConfig.setPreviousTopSymbols(previousTopSymbols);
	                        
	                        newAlertConfigs.put(columnName, alertConfig);
	                    }
	                    
	                    zScoreAlertManager.setAlertConfigs(newAlertConfigs);
	                }
	                
	                logToConsole("Loaded Z-Score alert configuration");
	            } 

				// Load checkbox states for specific PriceData columns
				if (showAveVolumeCheckBox != null) {
					showAveVolumeCheckBox
							.setSelected(config.has("showAveVolume") ? config.getBoolean("showAveVolume") : false);
				} else {
					System.err.println("Warning: showAveVolumeCheckBox is null, skipping setting its state.");
				}
				if (showPrevLastDayPriceCheckBox != null) {
					showPrevLastDayPriceCheckBox.setSelected(
							config.has("showPrevLastDayPrice") ? config.getBoolean("showPrevLastDayPrice") : false);
				} else {
					System.err.println("Warning: showPrevLastDayPriceCheckBox is null, skipping setting its state.");
				}

				// Refresh the table to apply changes and populate data
				SwingUtilities.invokeLater(this::refreshData);
				JOptionPane.showMessageDialog(this, "Configuration loaded successfully!", "Success",
						JOptionPane.INFORMATION_MESSAGE);
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, "Error loading configuration: " + e.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
		}
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
			Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_5.class);
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
			Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_5.class);
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
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// Data source combo
		dataSourceComboBox = new JComboBox<>(dataSources);
		dataSourceComboBox.setSelectedItem(currentDataSource);
		dataSourceComboBox.addActionListener(e -> {
			currentDataSource = (String) dataSourceComboBox.getSelectedItem();
			currentDataProvider = dataProviderFactory.getProvider(currentDataSource, this::logToConsole);
			refreshData();
		});

		// Volume Source combo box
		String[] volumeSources = { "Default", "Alpaca", "Yahoo", "Polygon", "TradingView" };
		volumeSourceCombo = new JComboBox<>(volumeSources);
		volumeSourceCombo.setSelectedItem("Default");
		volumeSourceCombo.setToolTipText(
				"Select the source for volume data (Prev Vol and Current Vol). 'Default' uses the Data Source.");
		volumeSourceCombo.addActionListener(e -> refreshData());

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
							new SymbolChartWindow(symbol, highestTimeframe, indicators, currentDataProvider,
									StockDashboardv1_22_5.this::logToConsole).setVisible(true);
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
	        JComboBox<String> sessionCombo = new JComboBox<>(new String[]{"all", "premarket", "standard", "postmarket"});
	        sessionCombo.setSelectedItem("all");
	        sessionCombo.setEnabled(highestCloseOpenCheckbox.isSelected());
			
			highestCloseOpenCheckbox.addActionListener(e -> {
				timeRangeCombo.setEnabled(highestCloseOpenCheckbox.isSelected());
				sessionCombo.setEnabled(highestCloseOpenCheckbox.isSelected());
			});

			resistancePanel.add(highestCloseOpenCheckbox);
			resistancePanel.add(new JLabel("Time Range:"));
			resistancePanel.add(timeRangeCombo);
			resistancePanel.add(new JLabel("Session:"));
	        resistancePanel.add(sessionCombo);

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
				 if (volumeCb.isSelected()) timeframeIndicators.get(tf).add("Volume"); else timeframeIndicators.get(tf).remove("Volume");
			});

			// 20MA volume checkbox
			JCheckBox volume20MACb = new JCheckBox("volume20MA");
			volume20MACb.setToolTipText("20-period SMA of volume");
			volume20MACb.setSelected(timeframeIndicators.get(tf).contains("Volume20MA"));
			volume20MACb.addActionListener(e -> {
				if (volume20MACb.isSelected()) timeframeIndicators.get(tf).add("Volume20MA"); else timeframeIndicators.get(tf).remove("Volume20MA");
			});

			indicatorCheckboxes
			    .computeIfAbsent(tf, k -> new LinkedHashMap<>())
			    .put("Volume", volumeCb);
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

		// Inheritance option
		inheritCheckbox = new JCheckBox("Apply higher timeframe indicators to lower timeframes");
		mainPanel.add(inheritCheckbox);

		uptrendFilterCheckbox = new JCheckBox("Call only lower timeframes when higher timeframe is same UpTrend", false);
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
	            }

	            // Save target value indicators
	            JPanel movingAveTargetValuePanel = (JPanel) ((JPanel) tfCB.getParent()).getComponent(3);
	            JCheckBox movingAveTargetValueCB = (JCheckBox) movingAveTargetValuePanel.getComponent(0);
	            if (movingAveTargetValueCB.isSelected()) {
	                timeframeIndicators.get(tf).add("MovingAverageTargetValue");
	            }
	        }
	    }

	    // Save the new setting
	    uptrendFilterEnabled = uptrendFilterCheckbox.isSelected();

	    // Handle inheritance if enabled
	    if (inheritCheckbox.isSelected()) {
	        applyIndicatorInheritance();
	    }

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
	    refreshData();
	    
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
	    List<String> bullishSymbols = new ArrayList<>();

	    // Get selected timeframes
	    Set<String> selectedTimeframes = new HashSet<>();
	    for (String tf : allTimeframes) {
	        if (bullishTimeframeCheckboxes.get(tf).isSelected()) {
	            selectedTimeframes.add(tf);
	        }
	    }

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

	            // Skip if timeframe not selected
	            String timeframe = colName.split(" ")[0];
	            if (!selectedTimeframes.contains(timeframe)) {
	                continue;
	            }

	            Object value = row.get(j);
	            String status = (value != null) ? value.toString().trim() : "";

	            if (status.startsWith("↑") || status.startsWith("↓")) {
	                status = status.split(" ")[0] + " " + status.split(" ")[1];
	            }

	            if (status.contains("@")) {
	                status = status.split(" ")[0];
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
	    
	    // NEW: Check if we should filter by volume alerts for all symbols or just bullish
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
	                
	    handleBullishAlerts(finalSymbols);
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
				|| colName.contains("MovingAverageTargetValue") || colName.contains("Volume20MA") || colName.contains("Volume");
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

				if (!selectedTimeframes.contains(colName.split(" ")[0])) {
					continue;
				}

				Object value = row.get(j);
				String status = (value != null) ? value.toString().trim() : "";

				if (status.startsWith("↑") || status.startsWith("↓")) {
					status = status.split(" ")[0] + " " + status.split(" ")[1];
				}

				if (status.contains("@")) {
					status = status.split(" ")[0];
				}

				if (!BULLISH_STATUSES.contains(status)) {
					hasInvalidStatus = true;
					break;
				}
			}
			
			if(!hasInvalidStatus) {
				hasInvalidStatus = !VolumeAlertAnalyser.analyze(symbol, volumeAlertConfig, priceDataMap);
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

				/*desiredSymbols.add(symbol);

				PriceData priceData = priceDataMap.get(symbol);

				String movingAverageTargetVal = priceData.getResults().entrySet().stream()
						.filter(entry -> entry.getValue().getMovingAverageTargetValue() != null) // ✅ skip nulls
						.map(entry -> " " + entry.getKey() + " matv: " + entry.getValue().getMovingAverageTargetValue()
								+ ", %tp: " + entry.getValue().getMovingAverageTargetValuePercentile())
						.collect(Collectors.joining(" | "));

				if (!movingAverageTargetVal.isEmpty())
					sb2.append(symbol).append(movingAverageTargetVal).append("\n");*/
			}
		}

		logToConsole(sb.toString());
		logToConsole("===================================================");
		logToConsole(sb2.toString());

		/*ConcurrentHashMap<String, PriceData> filteredPriceDataMap = priceDataMap.entrySet().parallelStream()
				.filter(entry -> entry.getKey() != null && entry.getValue() != null)
				.filter(entry -> desiredSymbols.contains(entry.getKey())).collect(Collectors.toMap(Map.Entry::getKey,
						Map.Entry::getValue, (oldVal, newVal) -> oldVal, ConcurrentHashMap::new));*/

		zScoreAlertManager.listZScoreResults();
		
		// todo: Z SCORE
		/*ZScoreCalculator calculator = new ZScoreCalculator();
		Map<String, Double> zScores = calculator.calculateZScores(filteredPriceDataMap);

		// Sort by Z-score descending (highest first)
		List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(zScores.entrySet());
		sortedEntries.sort((a, b) -> Double.compare(b.getValue(), a.getValue())); // Descending order

		// Print ranked results
		logToConsole("📊 Ranked Stock Candidates (Higher Z-Score = Better)");
		logToConsole("===================================================");
		for (int i = 0; i < sortedEntries.size(); i++) {
			Map.Entry<String, Double> entry = sortedEntries.get(i);
			logToConsole(String.format("%2d. %-6s: %.2f%n", i + 1, entry.getKey(), entry.getValue()));
		}

		// Alternatively, with more formatting:
		logToConsole("\n🎯 Top 10 Candidates:");
		logToConsole("Rank | Symbol | Z-Score");
		logToConsole("-----|--------|---------");
		for (int i = 0; i < Math.min(10, sortedEntries.size()); i++) {
			Map.Entry<String, Double> entry = sortedEntries.get(i);
			logToConsole(String.format("%4d | %-6s | %6.2f%n", i + 1, entry.getKey(), entry.getValue()));
		}*/
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
				FilterableTableModel newModel = new FilterableTableModel(newColumnNames.toArray(new String[0]), 0, this);

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
				refreshData();

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
		JFrame timeframeWindow = new JFrame("Select Timeframes");
		timeframeWindow.setSize(300, 300);
		timeframeWindow.setLayout(new GridLayout(9, 1));
		timeframeWindow.setLocationRelativeTo(this);

		Map<String, JCheckBox> timeframeCheckboxes = new HashMap<>();
		for (String timeframe : allTimeframes) {
			JCheckBox cb = new JCheckBox(timeframe, bullishTimeframeCheckboxes.get(timeframe).isSelected()); // All
																												// selected
																												// by
																												// default
			timeframeCheckboxes.put(timeframe, cb);
			timeframeWindow.add(cb);
		}

		JButton applyButton = new JButton("Apply");
		applyButton.addActionListener(ae -> {
			selectedTimeframes.clear();
			for (String timeframe : allTimeframes) {
				if (timeframeCheckboxes.get(timeframe).isSelected()) {
					selectedTimeframes.add(timeframe);
				}
			}
			checkBullishUptrendNeutralRows();
			timeframeWindow.dispose();
		});
		timeframeWindow.add(applyButton);

		timeframeWindow.setVisible(true);

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
				if (colName != null && colName.contains("MovingAverageTargetValue")) {
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
		if (startDatePicker.getDate() != null && endDatePicker.getDate() != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			timeRangeLabel.setText("Time Range: " + sdf.format(startDatePicker.getDate()) + " to "
					+ sdf.format(endDatePicker.getDate()));
		} else {
			timeRangeLabel.setText("Time Range: Custom (dates not set)");
		}
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
		/*if (watchListException.contains(currentWatchlist)) {
			JOptionPane.showMessageDialog(this, "Cannot edit the Default watchlist", "Info",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}*/

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
	        if (indicators.isEmpty()) continue;

	        for (String ind : allIndicators) {
	            if (indicators.contains(ind)) {
	                String indicatorColumnName = tf + " " + ind;
	                if (!columns.contains(indicatorColumnName)) {
	                    columns.add(indicatorColumnName);
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
	    
	    logToConsole("Table columns updated. Preserved " + oldColumnNames.size() + " columns, new structure has " + newColumnNames.length + " columns");
	}
	
	private void refreshData() {
		List<String> symbolsToProcess = new ArrayList<String>();

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
				
				List<String> watchListTmp = watchlists.get(currentWatchlist);
				
				for(String x: watchListTmp) {
					if(!symbolsToProcess.contains(x)) {
						symbolsToProcess.add(x);
					}
				}

			} catch (StockApiException e) {
				logToConsole("Error fetching refreshData" + e.getLocalizedMessage());
			}
		} else {
			symbolsToProcess = watchlists.get(currentWatchlist);
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

		// Create a CountDownLatch with count equal to number of symbols
		CountDownLatch latch = new CountDownLatch(visibleSymbols.size());
		ExecutorService executor = Executors.newFixedThreadPool(visibleSymbols.size());

		for (String symbol : visibleSymbols) {
			executor.execute(() -> {
				try {
					// Fetch analysis and price data
					LinkedHashMap<String, AnalysisResult> results = analyzeSymbol(symbol);
					PriceData priceData = fetchLatestPrice(symbol, results);

					// Adjust volume data using setters if different provider
					if (!"Default".equals(selectedVolumeSource) && !selectedVolumeSource.equals(selectedDataSource)) {
						PriceData volumePriceData = volumeProvider.getLatestPrice(symbol);
						priceData.previousVolume = volumePriceData.getPreviousVolume();
						priceData.currentVolume = volumePriceData.getCurrentVolume();
					}

					SwingUtilities.invokeLater(() -> {
						tableModel.fireTableDataChanged();
						updateOrAddSymbolRow(symbol, priceData, results);
						dashboardTable.revalidate();
						dashboardTable.repaint();
					});
				} catch (Exception e) {
					logToConsole("Error analyzing " + symbol + ": " + e.getMessage());
				} finally {
					// Decrement count when task is done (success or failure)
					latch.countDown();
				}
			});
		}

		// Shutdown executor after submitting all tasks
		executor.shutdown();

		// Create a new thread to wait for completion and then check bullish symbols
		new Thread(() -> {
			try {
				// Wait for all tasks to complete (with timeout to prevent hanging)
				latch.await(60, TimeUnit.SECONDS);

				// Check bullish symbols on the EDT once all updates are done
				SwingUtilities.invokeLater(() -> {
					
					//Refresh Z-Score columns after all data is loaded
					refreshZScoreColumns();
										
					checkAllAlerts();
					
					// Save logs/reports if enabled
					if (enableSaveLogCheckbox.isSelected() || enableLogReportsCheckbox.isSelected()) {
						if (enableSaveLogCheckbox.isSelected()) {
							saveConsoleLog();
						}
						if (enableLogReportsCheckbox.isSelected()) {
							saveReportFiles();
						}
					}
				});
			} catch (InterruptedException e) {
				logToConsole("Error waiting for symbol updates to complete: " + e.getMessage());
			}
		}).start();

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
	
	/**
	 * Main method to check all types of alerts after data refresh
	 */
	private void checkAllAlerts() {
	    // 1. Check Volume Alerts (independent - based on volume alert config)
	    checkAllVolumeAlerts();
	    
	    // 2. Check Z-Score Alerts (independent - based on Z-Score alert config)
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
	    }else {
	    	return;
	    }

	    List<String> symbolsWithVolumeSpikes = VolumeAlertAnalyser.analyze(symbolsToCheck, volumeAlertConfig, priceDataMap);
	    
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
	    if (visibleSymbols.isEmpty()) return;
	    
	    // Get all Z-Score columns that are currently visible
	    List<String> zScoreColumns = new ArrayList<>();
	    for (int i = 0; i < tableModel.getColumnCount(); i++) {
	        String colName = tableModel.getColumnName(i);
	        if (zScoreColumnHelper.isZScoreColumn(colName)) {
	            zScoreColumns.add(colName);
	        }
	    }
	    
	    if (zScoreColumns.isEmpty()) return;
	    
	    // For each Z-Score column, compute and update values
	    for (String zScoreCol : zScoreColumns) {
	        String combinationName = zScoreColumnHelper.getCombinationNameFromColumn(zScoreCol);
	        if (combinationName != null) {
	            Map<String, Map<String, Double>> zScoreValues = zScoreColumnHelper.getZScoreColumnValues(
	                visibleSymbols, combinationName, zScoreRankingGUI, priceDataMap);
	            
	            // Update table values with proper data types
	            for (int row = 0; row < tableModel.getRowCount(); row++) {
	                String symbol = (String) tableModel.getValueAt(row, 1);
	                if (symbol != null && zScoreValues.containsKey(symbol)) {
	                    Map<String, Double> symbolValues = zScoreValues.get(symbol);
	                    
	                    // Find the column index for this Z-Score combination
	                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
	                        String currentColName = tableModel.getColumnName(col);
	                        if (currentColName.equals(zScoreCol)) {
	                            // Update rank or Z-Score value based on column type with proper data types
	                            if (zScoreColumnHelper.isRankColumn(currentColName)) {
	                                // Convert to Integer for ranks
	                                Integer rankValue = symbolValues.get("Rank").intValue();
	                                tableModel.setValueAt(rankValue, row, col);
	                            } else if (zScoreColumnHelper.isZScoreValueColumn(currentColName)) {
	                                // Use Double for Z-Scores
	                                Double zScoreValue = symbolValues.get("Z-Score");
	                                tableModel.setValueAt(zScoreValue, row, col);
	                            }
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
					case "MovingAverageTargetValue":
						model.setValueAt(result.getMovingAverageTargetValue(), row, colIndex++);
						break;
					case "VWAP":
						model.setValueAt(result.getVwapStatus(), row, colIndex++);
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
					String status = "N/A";
					if (result.getHighestCloseOpenStatus() != null) {
						status = result.getHighestCloseOpenStatus();
					} else if (result.getHighestCloseOpen() != Double.MIN_VALUE) {
						status = "N/A (" + String.format("%.2f", result.getHighestCloseOpen()) + ")";
					}
					row.add(status);
					break;
				case "MovingAverageTargetValue":
					row.add(result.getMovingAverageTargetValue());
					break;
				case "Volume":
					row.add(result.getVolume());
					break;	
				case "Volume20MA":
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

			// Add the new resistance indicator if enabled
			/*if (indicatorsForTf.contains("HighestCloseOpen")) {
				row.add(result != null && result.getHighestCloseOpenStatus() != null
						? result.getHighestCloseOpenStatus()
						: "N/A");
			}*/
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
					if (isWithinAllowedTimeRange()) {
						refreshData();

						// Save logs/reports if enabled
						/*
						 * if (enableSaveLogCheckbox.isSelected() ||
						 * enableLogReportsCheckbox.isSelected()) { if
						 * (enableSaveLogCheckbox.isSelected()) { saveConsoleLog(); } if
						 * (enableLogReportsCheckbox.isSelected()) { saveReportFiles(); } }
						 */
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
			Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_5.class);
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

	// Modify methods to use the provider:
	private LinkedHashMap<String, AnalysisResult> analyzeSymbol(String symbol) {
		LinkedHashMap<String, AnalysisResult> results = new LinkedHashMap<String, AnalysisResult>();
		String previousTimeframeConsensusStatus = null;

		for (String tf : allTimeframes) {
			if (!timeframeIndicators.get(tf).isEmpty()) {
				try {

					// Apply uptrend filter if enabled
					if (uptrendFilterEnabled && previousTimeframeConsensusStatus != null) {
						// Only proceed if ALL indicators in higher timeframe agree on bullish status
						if (previousTimeframeConsensusStatus == null
								|| !BULLISH_STATUSES.contains(previousTimeframeConsensusStatus)) {
							logToConsole("Skipping " + tf + " for " + symbol
									+ " - higher timeframe indicators don't agree on bullish status");
							continue;
						}
					}

					BarSeries series = currentDataProvider.getHistoricalData(symbol, tf, 1000,
							calculateStartTime(tf, ZonedDateTime.now(ZoneOffset.UTC)),
							ZonedDateTime.now(ZoneOffset.UTC));

					AnalysisResult result = performTechnicalAnalysis(series, timeframeIndicators.get(tf), tf, symbol);
					results.put(tf, result);

					if (uptrendFilterEnabled) {
						// Store the consensus status for next timeframe
						previousTimeframeConsensusStatus = determineRelevantStatus(result, timeframeIndicators.get(tf));

						if (previousTimeframeConsensusStatus == null) {
							logToConsole(tf + " indicators don't agree on status for " + symbol);
						}
					}

				} catch (Exception e) {
					logToConsole("Error analyzing " + symbol + " " + tf + ": " + e.getMessage());
				}
			}
		}
		return results;
	}

	private String determineRelevantStatus(AnalysisResult result, Set<String> indicators) {
		// Check each enabled indicator against BULLISH_STATUSES
		if (indicators.contains("Trend")) {
			String trendStatus = getTrendStatus(result);
			if (!BULLISH_STATUSES.contains(trendStatus)) {
				return "N/A";
			}
		}

		if (indicators.contains("RSI")) {
			String rsiStatus = getRsiStatus(result);
			if (!BULLISH_STATUSES.contains(rsiStatus)) {
				return "N/A";
			}
		}

		if (indicators.contains("MACD") && result.getMacdStatus() != null) {
			if (!BULLISH_STATUSES.contains(result.getMacdStatus())) {
				return "N/A";
			}
		}

		if (indicators.contains("MACD(5,8,9)") && result.getMacd359Status() != null) {
			if (!BULLISH_STATUSES.contains(result.getMacd359Status())) {
				return "N/A";
			}
		}

		if (indicators.contains("PSAR(0.01)") && result.getPsar001Trend() != null) {
			String psar001Status = result.getPsar001Trend();
			if (!BULLISH_STATUSES.contains(psar001Status)) {
				return "N/A";
			}
		}

		if (indicators.contains("PSAR(0.05)") && result.getPsar005Trend() != null) {
			String psar005Status = result.getPsar005Trend();
			if (!BULLISH_STATUSES.contains(psar005Status)) {
				return "N/A";
			}
		}

		if (indicators.contains("HeikenAshi") && result.getHeikenAshiTrend() != null) {
			String heikenAshiStatus = result.getHeikenAshiTrend();
			if (!BULLISH_STATUSES.contains(heikenAshiStatus)) {
				return "N/A";
			}
		}

		if (indicators.contains("Action")) {
			String actionStatus = getActionRecommendation(result);
			if (!BULLISH_STATUSES.contains(actionStatus)) {
				return "N/A";
			}
		}

		// If we get here, all indicators are bullish
		return "Bullish";
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
			return end.minusDays(60);
		case "1H":
			return end.minusDays(120);
		case "4H":
			return end.minusDays(500);
		case "1D":
			return end.minusDays(730);
		case "1W":
			return end.minusDays(3000);
		default:
			return end.minusDays(7);
		}
	}

	private PriceData fetchLatestPrice(String symbol, LinkedHashMap<String, AnalysisResult> results)
			throws IOException {

		String selectedVolumeSource = (String) volumeSourceCombo.getSelectedItem();

		PriceData priceDataReturn = new PriceData();

		/*
		 * if (priceDataMap.size() > 0 && priceDataMap.containsKey(symbol)){
		 * priceDataReturn = getPriceDataBySymbol(symbol); } else {
		 */

		try {
			priceDataReturn = (PriceData) tradingViewService.getStockBySymbol(symbol);
			// priceDataMap.put(symbol, priceDataReturn);
		} catch (StockApiException e) {
			// TODO Auto-generated catch block
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

	
	private AnalysisResult performTechnicalAnalysis(BarSeries series, Set<String> indicators, String timeframe, String symbol) {
	    AnalysisResult result = new AnalysisResult();
	    if (series == null || series.getBarCount() == 0) {
	        logToConsole("No data available for " + symbol + " (" + timeframe + ")");
	        return result;
	    }else {
	    	result.setBarSeries(series);
	    }

	    // Get index ranges for the timeframe
	    int startIndex = timeframeIndexRanges.get(timeframe).get("startIndex");
	    int endIndex = timeframeIndexRanges.get(timeframe).get("endIndex");
	    int lastIndex = series.getBarCount() - 1;

	    // Validate indices
	    if (startIndex > endIndex) {
	        logToConsole("Invalid index range for " + symbol + " (" + timeframe + "): startIndex=" + startIndex + " is greater than endIndex=" + endIndex);
	        return result;
	    }

	    // Map startIndex and endIndex to BarSeries indices
	    int adjustedStartIndex = lastIndex - startIndex;
	    int adjustedEndIndex = lastIndex - endIndex;

	    // Ensure indices are within bounds
	    if (adjustedStartIndex < 0 || adjustedStartIndex >= series.getBarCount()) {
	        adjustedStartIndex = lastIndex;
	        logToConsole("Adjusted startIndex out of bounds for " + symbol + " (" + timeframe + "), using last index: " + adjustedStartIndex);
	    }
	    if (adjustedEndIndex < 0 || adjustedEndIndex >= series.getBarCount()) {
	        adjustedEndIndex = Math.max(0, adjustedStartIndex - 1);
	        logToConsole("Adjusted endIndex out of bounds for " + symbol + " (" + timeframe + "), using: " + adjustedEndIndex);
	    }
	    if (adjustedEndIndex > adjustedStartIndex) {
	        logToConsole("Invalid adjusted index range for " + symbol + " (" + timeframe + "): adjustedEndIndex=" + adjustedEndIndex + " is greater than adjustedStartIndex=" + adjustedStartIndex);
	        return result;
	    }

	    // Initialize indicators
	    ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
	    SMAIndicator sma = new SMAIndicator(closePrice, 20);
	    SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
	    EMAIndicator ema = new EMAIndicator(closePrice, 20);
	    RSIIndicator rsi = new RSIIndicator(closePrice, 14);
	    MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
	    EMAIndicator signal = new EMAIndicator(macd, 9);
	    MACDIndicator macd359 = new MACDIndicator(closePrice, 5, 8);
	    EMAIndicator signal359 = new EMAIndicator(macd359, 9);
	    ParabolicSarIndicator psar001 = new ParabolicSarIndicator(series, series.numOf(0.01), series.numOf(0.01), series.numOf(0.02));
	    ParabolicSarIndicator psar005 = new ParabolicSarIndicator(series, series.numOf(0.05), series.numOf(0.05), series.numOf(0.2));
	    HeikenAshiIndicator ha = new HeikenAshiIndicator(series);
	    AdvancedVWAPStrategy.AdvancedVWAPIndicator vwapIndicator = new AdvancedVWAPStrategy.AdvancedVWAPIndicator(series);
	    
	    // Track status for each indicator
	    Map<String, String> previousStatuses = new HashMap<>();

	    // Loop through the specified index range
	    for (int i = adjustedEndIndex; i <= adjustedStartIndex; i++) {
	        Num close = closePrice.getValue(i);
	        Num prevClose = i > 0 ? closePrice.getValue(i - 1) : close;
	        // Set price for the first bar
	        if (i == adjustedEndIndex) {
	            result.setPrice(close.doubleValue());
	        }

	        // Trend Analysis
	        if (indicators.contains("Trend")) {
	            Num smaValue = sma.getValue(i);
	            Num sma200Value = sma200.getValue(i);
	            Num emaValue = ema.getValue(i);
	            String currentTrend = determineTrend(close, smaValue, sma200Value, emaValue);
	            if (i == adjustedEndIndex) {
	                previousStatuses.put("Trend", currentTrend);
	                
	                if(previousStatuses.get("Trend") != null  && previousStatuses.get("Trend") != "" && currentTrend != previousStatuses.get("Trend")) {
	                	previousStatuses.put("Trend", "Neutral");
	                	currentTrend = "Neutral";
	                }	                
	                
	                if (i >= 19) {
	                    result.setSma20(smaValue.doubleValue());
	                }
	                if (i >= 199) {
	                    result.setSma200(sma200Value.doubleValue());
	                }
	                result.setSmaTrend(currentTrend);
	            } else if (previousStatuses.get("Trend") != null && !currentTrend.equals(previousStatuses.get("Trend"))) {
	                previousStatuses.put("Trend", "Neutral");
	                result.setSmaTrend("Neutral");
	            } else {
	                previousStatuses.put("Trend", currentTrend);
	            }
	        }

	        // RSI Analysis
	        if (indicators.contains("RSI")) {
	            Num rsiValue = rsi.getValue(i);
	            String rsiStatus = determineRsiStatus(rsiValue);
	            if (i == adjustedEndIndex) {
	                previousStatuses.put("RSI", rsiStatus);
	                
	                if(previousStatuses.get("RSI") != null  && previousStatuses.get("RSI") != "" && rsiStatus != previousStatuses.get("RSI")) {
	                	previousStatuses.put("RSI", "Neutral");
	                	rsiStatus = "Neutral";
	                }
	                
	                if (i >= 13) {
	                    result.setRsi(rsiValue.doubleValue());
	                }
	                result.setRsiTrend(rsiStatus);
	            } else if (previousStatuses.get("RSI") != null && !rsiStatus.equals(previousStatuses.get("RSI"))) {
	                previousStatuses.put("RSI", "Neutral");
	                result.setRsiTrend("Neutral");
	            } else {
	                previousStatuses.put("RSI", rsiStatus);
	            }
	        }

	        // MACD Analysis
	        if (indicators.contains("MACD")) {
	            Num macdLine = macd.getValue(i);
	            Num signalLine = signal.getValue(i);
	            String macdStatus = determineMacdStatus(macdLine, signalLine);
	            if (i == adjustedEndIndex) {
	                previousStatuses.put("MACD", macdStatus);
	                
	                if(previousStatuses.get("MACD") != null  && previousStatuses.get("MACD") != "" && macdStatus != previousStatuses.get("MACD")) {
	                	previousStatuses.put("MACD", "Neutral");
	                	macdStatus = "Neutral";
	                }
	                
	                if (i >= 34) { // 12 + 26 - 1 for MACD, +9 for EMA
	                    result.setMacd(macdLine.doubleValue());
	                    result.setMacdSignal(signalLine.doubleValue());
	                }
	                result.setMacdStatus(macdStatus);
	            } else if (previousStatuses.get("MACD") != null && !macdStatus.equals(previousStatuses.get("MACD"))) {
	                previousStatuses.put("MACD", "Neutral");
	                result.setMacdStatus("Neutral");
	            } else {
	                previousStatuses.put("MACD", macdStatus);
	            }	            
	        }

	        // MACD(5,8,9) Analysis
	        if (indicators.contains("MACD(5,8,9)")) {
	            Num macdLine = macd359.getValue(i);
	            Num signalLine = signal359.getValue(i);
	            String macdStatus = determineMacdStatus(macdLine, signalLine);
	            if (i == adjustedEndIndex) {
	                previousStatuses.put("MACD(5,8,9)", macdStatus);
	                
	                if(previousStatuses.get("MACD(5,8,9)") != null  && previousStatuses.get("MACD(5,8,9)") != "" && macdStatus != previousStatuses.get("MACD(5,8,9)")) {
	                	previousStatuses.put("MACD(5,8,9)", "Neutral");
	                	macdStatus = "Neutral";
	                }
	                
	                if (i >= 16) { // 5 + 8 - 1 for MACD, +9 for EMA
	                    result.setMacd359(macdLine.doubleValue());
	                    result.setMacdSignal359(signalLine.doubleValue());
	                }
	                result.setMacd359Status(macdStatus);
	            } else if (previousStatuses.get("MACD(5,8,9)") != null && !macdStatus.equals(previousStatuses.get("MACD(5,8,9)"))) {
	                previousStatuses.put("MACD(5,8,9)", "Neutral");
	                result.setMacd359Status("Neutral");
	            } else {
	                previousStatuses.put("MACD(5,8,9)", macdStatus);
	            }
	        }

	        // PSAR(0.01) Analysis
	        if (indicators.contains("PSAR(0.01)")) {
	            Num psarValue = psar001.getValue(i);
	            String psarStatus = determinePsarStatus(close, psarValue);	            
	            
	            if (i == adjustedEndIndex) {
	                previousStatuses.put("PSAR(0.01)", psarStatus);

		            if(previousStatuses.get("PSAR(0.01)") != null  && previousStatuses.get("PSAR(0.01)") != "" && psarStatus != previousStatuses.get("PSAR(0.01)")) {
	                	previousStatuses.put("PSAR(0.01)", "Neutral");
	                	psarStatus = "Neutral";
	                }
		            
	                if (i >= 1) {
	                    result.setPsar001(psarValue.doubleValue());
	                }
	                result.setPsar001Trend(psarStatus);
	            } else if (previousStatuses.get("PSAR(0.01)") != null && !psarStatus.equals(previousStatuses.get("PSAR(0.01)"))) {
	                previousStatuses.put("PSAR(0.01)", "Neutral");
	                result.setPsar001Trend("Neutral");
	            } else {
	                previousStatuses.put("PSAR(0.01)", psarStatus);
	            }	            
	        }

	        // PSAR(0.05) Analysis
	        if (indicators.contains("PSAR(0.05)")) {
	            Num psarValue = psar005.getValue(i);
	            String psarStatus = determinePsarStatus(close, psarValue);
	            	            
	            if (i == adjustedEndIndex) {
	                previousStatuses.put("PSAR(0.05)", psarStatus);	                

		            if(previousStatuses.get("PSAR(0.05)") != null  && previousStatuses.get("PSAR(0.05)") != "" && psarStatus != previousStatuses.get("PSAR(0.05)")) {
	                	previousStatuses.put("PSAR(0.05)", "Neutral");
	                	psarStatus = "Neutral";
	                }
	                
	                if (i >= 1) {
	                    result.setPsar005(psarValue.doubleValue());
	                }
	                result.setPsar005Trend(psarStatus);
	            } else if (previousStatuses.get("PSAR(0.05)") != null && !psarStatus.equals(previousStatuses.get("PSAR(0.05)"))) {
	                previousStatuses.put("PSAR(0.05)", "Neutral");
	                result.setPsar005Trend("Neutral");
	            } else {
	                previousStatuses.put("PSAR(0.05)", psarStatus);
	            }	            
	        }

	        // Heiken Ashi Analysis
	        if (indicators.contains("HeikenAshi")) {
	            String haStatus = ha.getValue(i).doubleValue() > ha.getHeikenAshiOpen(i).doubleValue() ? "Uptrend" : "Downtrend";
	            if (i == adjustedEndIndex) {
	                previousStatuses.put("HeikenAshi", haStatus);
	                
	                if(previousStatuses.get("HeikenAshi") != null  && previousStatuses.get("HeikenAshi") != "" && haStatus != previousStatuses.get("HeikenAshi")) {
	                	previousStatuses.put("HeikenAshi", "Neutral");
	                	haStatus = "Neutral";
	                }
	                
	                if (endIndex >= 0) {
	    				result.setHeikenAshiClose(ha.getValue(endIndex).doubleValue());
	    				result.setHeikenAshiOpen(ha.getHeikenAshiOpen(endIndex).doubleValue());
	    				result.setHeikenAshiTrend(haStatus);
	    			}
	                
	            } else if (previousStatuses.get("HeikenAshi") != null && !haStatus.equals(previousStatuses.get("HeikenAshi"))) {
	                previousStatuses.put("HeikenAshi", "Neutral");
	                result.setHeikenAshiTrend("Neutral");
	            } else {
	                previousStatuses.put("HeikenAshi", haStatus);
	            }
	        }

	        // Highest Close/Open Analysis
	        if (indicators.contains("HighestCloseOpen")) {
	        	JComboBox<String> timeRangeCombo = indicatorTimeRangeCombos.get(timeframe);
	        	JComboBox<String> sessionCombo = indicatorSessionCombos.get(timeframe);
	        	if (timeRangeCombo != null && sessionCombo != null) {
					String timeRange = timeRangeCombo.getSelectedItem().toString();
					String session = sessionCombo.getSelectedItem().toString();
					
					int sessionEndIndex = HighestCloseOpenStrategy.calculateSessionEndIndex(series, series.getEndIndex(), session, currentDataSource);

					if (sessionEndIndex != -1) {
						HighestCloseOpenIndicator highestClose = new HighestCloseOpenIndicator(series, timeRange, true, currentDataSource);
						HighestCloseOpenIndicator highestOpen = new HighestCloseOpenIndicator(series, timeRange, false, currentDataSource);
	
						HighestCloseOpenIndicator.HighestValue highestCloseVal = highestClose.getValue(series.getEndIndex());
						HighestCloseOpenIndicator.HighestValue highestOpenVal = highestOpen.getValue(series.getEndIndex());
	
						// Determine which is higher - close or open
						HighestCloseOpenIndicator.HighestValue highestVal = highestCloseVal.value
								.isGreaterThan(highestOpenVal.value) ? highestCloseVal : highestOpenVal;
	
						Num currentPrice = series.getLastBar().getClosePrice();
						boolean isDowntrend = !currentPrice.isGreaterThan(highestVal.value);
	
						// Format the timestamp with timezone conversion for Yahoo
						String timeStr = "N/A";
						if (highestVal.time != null) {
							ZonedDateTime displayTime = highestVal.time;
	
							// Convert to Pacific Time if data source is Yahoo
							if ("YAHOO".equalsIgnoreCase(currentDataSource)) {
								displayTime = highestVal.time.withZoneSameInstant(ZoneId.of("America/Los_Angeles"));
								timeStr = displayTime.format(DateTimeFormatter.ofPattern("MMM dd HH:mm")) + " PST";
							} else {
								// Keep original timezone for other data sources
								timeStr = displayTime.format(DateTimeFormatter.ofPattern("MMM dd HH:mm z"));
							}
						}
	
						result.setHighestCloseOpen(highestVal.value.doubleValue());
						result.setHighestCloseOpenStatus(String.format("%s (%.2f @ %s)", isDowntrend ? "Downtrend" : "Uptrend",
								highestVal.value.doubleValue(), timeStr));
						
						previousStatuses.put("HighestCloseOpen", result.getHighestCloseOpenStatus());
					}
				}
	        }

	        // Moving Average Target Value Analysis
	        if (indicators.contains("MovingAverageTargetValue")) {
	        	String trendDirection = determineTrendDirection(result, series);
				try {
					MovingAverageTargetValue targetValue = new MovingAverageTargetValue(series, trendDirection);
					if (series.getEndIndex() >= 0) {
						Num value = targetValue.getValue(series.getEndIndex());
						if (value != null && !value.isNaN()) {
							result.setMovingAverageTargetValue(value.doubleValue());
						} else {
							result.setMovingAverageTargetValue(null); // or Double.NaN
							logToConsole("MovingAverageTargetValue is NaN or null for " + symbol);
						}
					} else {
						result.setMovingAverageTargetValue(null);
						logToConsole("Empty series for MovingAverageTargetValue for " + symbol);
					}
				} catch (Exception e) {
					result.setMovingAverageTargetValue(null);
					logToConsole("Error calculating MovingAverageTargetValue for " + symbol + ": " + e.getMessage());
				}
	        }
	        
	        // Calculate breakout counts
			//if (indicators.contains("Breakout")) {
				// MACD Breakout Count
				if (indicators.contains("MACD Breakout")) {
					result.setMacdBreakoutCount(calculateBreakoutCount(series, a -> {
						if (a < 34)
							return 0;
						double macdbc = new MACDIndicator(closePrice, 12, 26).getValue(a).doubleValue();
						double signalbc = new EMAIndicator(new MACDIndicator(closePrice, 12, 26), 9).getValue(a)
								.doubleValue();
						return macdbc > signalbc ? 1 : -1;
					}));
				}

				// MACD(5,8,9) Breakout Count
				if (indicators.contains("MACD(5,8,9) Breakout")) {
					result.setMacd359BreakoutCount(calculateBreakoutCount(series, a -> {
						if (a < 16)
							return 0;
						double macdbc = new MACDIndicator(closePrice, 5, 8).getValue(a).doubleValue();
						double signalbc = new EMAIndicator(new MACDIndicator(closePrice, 5, 8), 9).getValue(a).doubleValue();
						return macdbc > signalbc ? 1 : -1;
					}));
				}

				// PSAR(0.01) Breakout Count
				if (indicators.contains("PSAR(0.01) Breakout")) {
					result.setPsar001BreakoutCount(calculateBreakoutCount(series, a -> {
						if (a < 1)
							return 0;
						double psar = new ParabolicSarIndicator(series, series.numOf(0.01), series.numOf(0.01),
								series.numOf(0.01)).getValue(a).doubleValue();
						return psar < series.getBar(a).getClosePrice().doubleValue() ? 1 : -1;
					}));
				}

				// PSAR(0.05) Breakout Count
				if (indicators.contains("PSAR(0.05) Breakout")) {
					result.setPsar005BreakoutCount(calculateBreakoutCount(series, a -> {
						if (a < 1)
							return 0;
						double psar = new ParabolicSarIndicator(series, series.numOf(0.05), series.numOf(0.05),
								series.numOf(0.05)).getValue(a).doubleValue();
						return psar < series.getBar(a).getClosePrice().doubleValue() ? 1 : -1;
					}));
				}
			//}
				
			if (indicators.contains("Volume") || indicators.contains("Volume20MA")) {
			    int end = series.getEndIndex();
			    if (end >= 0) {
			        // Current bar volume
			        if (indicators.contains("Volume")) {
			        	VolumeIndicator vInd = new VolumeIndicator(series);
			        	result.setVolume(vInd.getValue(series.getEndIndex()).doubleValue());
			        }

			        // 20-period SMA of volume
			        if (indicators.contains("Volume20MA")) {
			        	VolumeMAIndicator v20Ind = new VolumeMAIndicator(series, 20);
			            result.setVolume20MA(v20Ind.getValue(series.getEndIndex()).doubleValue());
			        }
			    }
			}
			
			if (indicators.contains("VWAP")) {
			    //AdvancedVWAPStrategy.AdvancedVWAPIndicator vwapIndicator = new AdvancedVWAPStrategy.AdvancedVWAPIndicator(series);
			    if (series.getEndIndex() >= 0) {
			        Num vwapValue = vwapIndicator.getValue(series.getEndIndex());
			        Num vwapClosePrice = series.getLastBar().getClosePrice();
			        String vwapStatus = vwapClosePrice.isGreaterThan(vwapValue) ? "Uptrend" : "Bearish";
			        result.setVwapStatus(vwapStatus);
			        previousStatuses.put("VWAP", vwapStatus);
				} /*
					 * else { result.addCustomValue("VWAP", "Neutral"); previousStatuses.put("VWAP",
					 * "Neutral"); }
					 */
			}

	        // Action Analysis
	        if (indicators.contains("Action")) {
	            String action = determineAction(close, prevClose, i, series);
	            if (i == adjustedEndIndex) {
	                previousStatuses.put("Action", action);
	                result.addCustomValue("Action", action);
	            } else if (previousStatuses.get("Action") != null && !action.equals(previousStatuses.get("Action"))) {
	                previousStatuses.put("Action", "Neutral");
	                result.addCustomValue("Action", "Neutral");
	            } else { 
	                previousStatuses.put("Action", action);
	            }
	        }	        
	        
	    }

	    // Set final statuses for indicators if not already set to "Neutral"
	    /*for (String indicator : indicators) {
	        if (!indicator.equals("Breakout Count") && !indicator.equals("MACD Breakout") && 
	            !indicator.equals("MACD(5,8,9) Breakout") && !indicator.equals("PSAR(0.01) Breakout") && 
	            !indicator.equals("PSAR(0.05) Breakout")) {
	            String status = previousStatuses.getOrDefault(indicator, "Neutral");
	            switch (indicator) {
	                case "Trend":
	                    result.setSmaTrend(status);
	                    break;
	                case "RSI":
	                    result.setRsiTrend(status);
	                    break;
	                case "MACD":
	                    result.setMacdStatus(status);
	                    break;
	                case "MACD(5,8,9)":
	                    result.setMacd359Status(status);
	                    break;
	                case "PSAR(0.01)":
	                    result.setPsar001Trend(status);
	                    break;
	                case "PSAR(0.05)":
	                    result.setPsar005Trend(status);
	                    break;
	                case "HeikenAshi":
	                    result.setHeikenAshiTrend(status);
	                    break;
	                case "HighestCloseOpen":
	                    result.setHighestCloseOpenStatus(status);
	                    break;
	                case "MovingAverageTargetValue":
	                case "Action":
	                    result.addCustomValue(indicator, status);
	                    break;
	            }
	        }
	    }*/

	    return result;
	}
	
	public PriceData getPriceDataBySymbol(String symbol) {

		PriceData priceData1 = priceDataMap.get(symbol);

		return priceData1;
	}	
	
	private String determineRsiStatus(Num rsiValue) {
	    if (rsiValue == null) {
	        return "Neutral";
	    }
	    if (rsiValue.isGreaterThanOrEqual(rsiValue.numOf(70))) {
	        return "Overbought";
	    } else if (rsiValue.isLessThanOrEqual(rsiValue.numOf(30))) {
	        return "Oversold";
	    }
	    return "Neutral";
	}

	private String determineMacdStatus(Num macdLine, Num signalLine) {
	    if (macdLine == null || signalLine == null) {
	        return "Neutral";
	    }
	    if (macdLine.isGreaterThan(signalLine)) {
	        return "Bullish";
	    } else if (macdLine.isLessThan(signalLine)) {
	        return "Bearish";
	    }
	    return "Neutral";
	}

	private String determinePsarStatus(Num close, Num psarValue) {
	    if (close == null || psarValue == null) {
	        return "Neutral";
	    }
	    if (close.isGreaterThan(psarValue)) {
	        return "Bullish";
	    } else {
	        return "Bearish";
	    }
	}
	
	private String determineAction(Num close, Num prevClose, int index, BarSeries series) {
	    if (close == null || prevClose == null) {
	        return "Hold";
	    }
	    if (close.isGreaterThan(prevClose)) {
	        return "Buy";
	    } else if (close.isLessThan(prevClose)) {
	        return "Sell";
	    }
	    return "Hold";
	}

	private String determineTrendDirection(AnalysisResult result, BarSeries series) {
		// Use existing trend analysis if available
		if (result.getPsar005Trend() != null) {
			if (result.getPsar005Trend().contains("↑") || result.getPsar005Trend().contains("Up")) {
				return "UpTrend";
			} else if (result.getPsar005Trend().contains("↓") || result.getPsar005Trend().contains("Down")) {
				return "DownTrend";
			}
		}

		if (result.getPsar001Trend() != null) {
			if (result.getPsar001Trend().contains("↑") || result.getPsar001Trend().contains("Up")) {
				return "UpTrend";
			} else if (result.getPsar001Trend().contains("↓") || result.getPsar001Trend().contains("Down")) {
				return "DownTrend";
			}
		}

		// Fallback to candle analysis
		// BarSeries series = series2.getSeries();
		int lastIndex = series.getEndIndex();
		if (lastIndex > 0) {
			Bar lastBar = series.getBar(lastIndex);
			return lastBar.getClosePrice().isGreaterThan(lastBar.getOpenPrice()) ? "UpTrend" : "DownTrend";
		}

		return "Neutral";
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

	private String determineTrend(Num close, Num sma, Num sma200, Num ema) {
		if (close == null || sma == null || sma200 == null || ema == null) {
			return "Neutral";
		}
		if (close.isGreaterThan(sma) && close.isGreaterThan(sma200) && sma.isGreaterThan(sma200)) {
			if (close.isGreaterThan(ema)) {
				return "Uptrend";
			} else {
				return "Uptrend";
			}
		} else if (close.isLessThan(sma) && close.isLessThan(sma200) && sma.isLessThan(sma200)) {
			if (close.isLessThan(ema)) {
				return "Downtrend";
			} else {
				return "Downtrend";
			}
		}
		return "Neutral";
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
		if (result.getPrice() > result.getSma20() && result.getPrice() > result.getSma200()) {
			return "Strong Uptrend";
		} else if (result.getPrice() < result.getSma20() && result.getPrice() < result.getSma200()) {
			return "Strong Downtrend";
		} /*else if (result.getPrice() > result.getSma20()) {
			return "Mild Uptrend";
		}*/ else {
			return "Neutral";
		}
	}

	private String getRsiStatus(AnalysisResult result) {
		if (result.getRsi() > 70)
			return "Overbought";
		if (result.getRsi() < 30)
			return "Oversold";
		if (result.getRsi() > 50)
			return "Bullish";
		return "Bearish";
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
	    //zScoreAlertConfigDialog = new ZScoreAlertConfigDialog(this, indi);
	    zScoreAlertConfigDialog.setVisible(true);
	    
	    if (zScoreAlertConfigDialog.isSaved()) {
	        zScoreAlertManager.setAlertConfigs(zScoreAlertConfigDialog.getAlertConfigs());
	        zScoreAlertManager.setEnabled(zScoreAlertConfigDialog.isZScoreAlertEnabled());
	        logToConsole("Z-Score alert configuration updated");
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
	
	public ZScoreAlertManager getZScoreAlertManager() {
	    return zScoreAlertManager;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			StockDashboardv1_22_5 dashboard = new StockDashboardv1_22_5();
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

	// Graphs
	private void showMonteCarloGraph() {
		List<String> symbols = getVisibleSymbols();
		if (symbols.isEmpty()) {
			JOptionPane.showMessageDialog(this, "No symbols selected for Monte Carlo graph", "Warning",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		new MonteCarloGraphApp(symbols, dataProviderFactory,this::logToConsole, "Manual View");
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
	
	private List<String> getVisibleSymbolList(){
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

	@Override
	public String getCurrentWatchlistName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getSelectedBullishTimeframes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Set<String>> getCustomIndicatorCombinations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCustomIndicatorCombinations(Map<String, Set<String>> combinations) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateTableColumnsForCustomIndicators() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public JRadioButton getCurrentTimeRadio() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ZonedDateTime getStartDateTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ZonedDateTime getEndDateTime() {
		// TODO Auto-generated method stub
		return null;
	}

}