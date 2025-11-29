package com.quantlabs.QuantTester;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
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

import com.quantlabs.QuantTester.v1.yahoo.query.YahooFinanceQueryScreenerService;
import com.quantlabs.QuantTester.v4.alert.AlertManager;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EnhancedStockDashboardV11 extends JFrame {
	private final OkHttpClient client = new OkHttpClient();
	private final String apiKey = "PK4UOZQDJJZ6WBAU52XM";
	private final String apiSecret = "Fag4ha2D58VyL0okwXgBHD1IvhoptmI2KiacMaNG";

	
	  private final String[] symbols = { "AAPL", "AVGO", "PLTR", "MSFT", "GOOGL",
	  "AMZN", "TSLA", "NVDA", "META", "NFLX", "WMT", "JPM", "LLY", "V", "ORCL",
	  "MA", "XOM", "COST", "JNJ", "HD", "UNH", "DLTR" };
	 
	//private final String[] symbols = { "NVDA"};

	
	private static final Map<String, Integer> ALPACA_TIME_LIMITS = Map.of("1Min", 4, "5Min", 7, "15Min", 7, "30Min", 30,
			"1H", 30, "4H", 30, "1D", 60);

	private static final Map<String, Integer> YAHOO_TIME_LIMITS = Map.of("1Min", 7, "5Min", 60, "15Min", 60, "30Min",
			60, "1H", 60, "4H", 60, "1D", 60);

	private final String[] dataSources = { "Alpaca", "Yahoo" };
	private final String[] allTimeframes = { "1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min" };
	private final String[] allIndicators = { "Trend", "RSI", "MACD", "MACD(5,8,9)", "PSAR(0.01)", "PSAR(0.05)",
			"Action" };
	private final Integer[] refreshIntervals = { 10, 30, 60, 120, 300 }; // Seconds

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

	private JPanel selectionPanel;
	private Map<String, JCheckBox> timeframeCheckBoxes = new HashMap<>();
	private Map<String, JCheckBox> indicatorCheckBoxes = new HashMap<>();
	private Set<String> selectedTimeframes = new HashSet<>();
	private Set<String> selectedIndicators = new HashSet<>();
	
	private JPanel filtersPanel;
	private JTextField prevVolMinField;
	private JTextField prevVolMaxField;
	private JTextField currentVolMinField;
	private JTextField currentVolMaxField;

	YahooFinanceQueryScreenerService yahooQueryFinance = new YahooFinanceQueryScreenerService();
	
	private JPanel controlPanel;
    private JButton listSymbolsButton;
    private JLabel statusLabel;
    private JButton checkBullishButton;
	private AbstractButton uncheckAllButton;

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
			switch (column) {
			case 0:
				return Boolean.class;
			case 1:
				return String.class;
			case 2:
				return Double.class;
			case 3:
				return Long.class;
			case 4:
				return Long.class;
			case 5:
				return Double.class;
			default:
				return String.class;
			}
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

	public EnhancedStockDashboardV11() {
		initializeStatusColors();
		initializeWatchlists();
		initializeSelections();
		setupUI();
		refreshData();
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
		watchlists.put("Tech Stocks - Application", Arrays.asList("SAP", "CRM", "INTU", "NOW", "UBER", "SHOP", "ADBE", "ADP"
				, "MSTR", "CDNS", "SNOW", "ADSK", "WDAY", "ROP", "TEAM", "PAYX", "DDOG", "FICO", "ANSS", "HUBS", "PTC", "TYL", "ZM", "GRAB"
				, "SOUN", "WCT", "YMM", "BULL", "LYFT", "UBER", "PD", "WRD", "U", "YAAS", "SHOP", "FRSH"));
		watchlists.put("Tech Stocks - Infrastructure", Arrays.asList("LIDR", "PLTR", "MSFT", "PATH", "CORZ", "CRWV", "OKTA", "AI"
				, "TOST", "ORCL", "RZLV", "S", "XYZ", "S", "PANW", "CRWD", "SNPS", "FTNT", "NET", "CRWV", "XYZ", "ZS", "VRSN", "CHKP"
				, "CPAY", "GDDY", "IOT", "AFRM", "GPN", "NTNX", "TWLO", "MDB", "CYBR", "DOX", "GTLB"));
		watchlists.put("ETF", Arrays.asList("QQQ", "SPY", "IGV", "VGT", "FTEC", "SMH"));
		watchlists.put("Blue Chips", Arrays.asList("WMT", "JPM", "V", "MA", "XOM", "JNJ", "HD"));
		watchlists.put("Financial",
				Arrays.asList("V", "JPM", "MA", "AXP", "BAC", "COIN", "BLK", "WFC", "BLK", "MS", "C", "BX", "SOFI"));
		watchlists.put("SemiConductors",
				Arrays.asList("NVDA", "AVGO", "AMD", "TSM", "QCOM", "ARM", "MRVL", "NXPI", "MCHP", "ADI", "TXN", "MU", "INTC", "ON", "SWKS" ,"WOLF" ,"STM" , "TXN"));
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

	private void initializeSelections() {
		selectedTimeframes.addAll(Arrays.asList("4H", "30Min" , "5Min"));
		selectedIndicators.addAll(Arrays.asList("MACD", "MACD(5,8,9)", "PSAR(0.01)", "PSAR(0.05)"));
	}

	private void setupUI() {
	    setTitle("Enhanced Stock Dashboard (IEX Feed)");
	    setSize(1200, 700);
	    setMinimumSize(new Dimension(800, 600));
	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    setLayout(new BorderLayout());

	    // Add resize listener to the JFrame
	    this.addComponentListener(new ComponentAdapter() {
	        @Override
	        public void componentResized(ComponentEvent e) {
	            controlPanel.revalidate();
	            controlPanel.repaint();
	            logToConsole("Window resized to: " + getSize());
	        }
	    });

	    // Time Range Panel
	    timeRangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
	    timeRangePanel.setPreferredSize(new Dimension(1200, 50));
	    currentTimeRadio = new JRadioButton("Current Time", true);
	    currentTimeRadio.setPreferredSize(new Dimension(100, 25));
	    customRangeRadio = new JRadioButton("Custom Range");
	    customRangeRadio.setPreferredSize(new Dimension(100, 25));
	    timeRangeGroup = new ButtonGroup();
	    timeRangeGroup.add(currentTimeRadio);
	    timeRangeGroup.add(customRangeRadio);

	    startDatePicker = new JXDatePicker();
	    startDatePicker.setFormats("yyyy-MM-dd");
	    startDatePicker.setEnabled(false);
	    startDatePicker.setPreferredSize(new Dimension(120, 25));
	    endDatePicker = new JXDatePicker();
	    endDatePicker.setFormats("yyyy-MM-dd");
	    endDatePicker.setEnabled(false);
	    endDatePicker.setPreferredSize(new Dimension(120, 25));

	    clearCustomRangeButton = new JButton("Clear Custom Range");
	    clearCustomRangeButton.setEnabled(false);
	    clearCustomRangeButton.setPreferredSize(new Dimension(140, 25));

	    timeRangeLabel = new JLabel("Time Range: Current Time");
	    timeRangeLabel.setPreferredSize(new Dimension(200, 25));

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

	    timeRangePanel.add(new JLabel("Time Range:"));
	    timeRangePanel.add(currentTimeRadio);
	    timeRangePanel.add(customRangeRadio);
	    timeRangePanel.add(new JLabel("Start:"));
	    timeRangePanel.add(startDatePicker);
	    timeRangePanel.add(new JLabel("End:"));
	    timeRangePanel.add(endDatePicker);
	    timeRangePanel.add(clearCustomRangeButton);
	    timeRangePanel.add(timeRangeLabel);

	    // Selection Panel
	    selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
	    selectionPanel.setPreferredSize(new Dimension(1200, 50));
	    selectionPanel.add(new JLabel("Timeframes:"));
	    for (String timeframe : allTimeframes) {
	        JCheckBox cb = new JCheckBox(timeframe, selectedTimeframes.contains(timeframe));
	        cb.setPreferredSize(new Dimension(80, 25));
	        timeframeCheckBoxes.put(timeframe, cb);
	        selectionPanel.add(cb);
	    }
	    selectionPanel.add(new JLabel("Indicators:"));
	    for (String indicator : allIndicators) {
	        JCheckBox cb = new JCheckBox(indicator, selectedIndicators.contains(indicator));
	        cb.setPreferredSize(new Dimension(100, 25));
	        indicatorCheckBoxes.put(indicator, cb);
	        selectionPanel.add(cb);
	    }
	    JButton applyChangesButton = createButton("Apply Changes", new Color(70, 130, 180));
	    applyChangesButton.setPreferredSize(new Dimension(120, 25));
	    applyChangesButton.addActionListener(e -> applyChanges());
	    selectionPanel.add(applyChangesButton);

	    // Console Panel
	    consoleArea = new JTextArea(5, 80);
	    consoleArea.setEditable(false);
	    consoleScrollPane = new JScrollPane(consoleArea);
	    consolePanel = new JPanel(new BorderLayout());
	    consolePanel.setPreferredSize(new Dimension(1200, 150));
	    consolePanel.add(new JLabel("Console Output:"), BorderLayout.NORTH);
	    consolePanel.add(consoleScrollPane, BorderLayout.CENTER);

	    // Control Panel with GridBagLayout
	    controlPanel = new JPanel(new GridBagLayout());
	    GridBagConstraints gbc = new GridBagConstraints();
	    gbc.insets = new Insets(5, 5, 5, 5);
	    gbc.fill = GridBagConstraints.BOTH;
	    gbc.weighty = 1.0;

	    // Watchlist Panel
	    JPanel watchlistPanel = new JPanel(new GridBagLayout());
	    watchlistPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Watchlist"));
	    GridBagConstraints panelGbc = new GridBagConstraints();
	    panelGbc.insets = new Insets(3, 3, 3, 3);
	    panelGbc.fill = GridBagConstraints.HORIZONTAL;
	    panelGbc.weightx = 1.0;

	    dataSourceComboBox = new JComboBox<>(dataSources);
	    dataSourceComboBox.setSelectedItem(currentDataSource);
	    dataSourceComboBox.setPreferredSize(new Dimension(120, 25));
	    dataSourceComboBox.addActionListener(e -> {
	        currentDataSource = (String) dataSourceComboBox.getSelectedItem();
	        refreshData();
	    });
	    panelGbc.gridx = 0;
	    panelGbc.gridy = 0;
	    watchlistPanel.add(new JLabel("Data Source:"), panelGbc);
	    panelGbc.gridx = 1;
	    watchlistPanel.add(dataSourceComboBox, panelGbc);

	    watchlistComboBox = new JComboBox<>(watchlists.keySet().toArray(new String[0]));
	    watchlistComboBox.setSelectedItem(currentWatchlist);
	    watchlistComboBox.setPreferredSize(new Dimension(120, 25));
	    watchlistComboBox.addActionListener(e -> {
	        currentWatchlist = (String) watchlistComboBox.getSelectedItem();
	        refreshData();
	    });
	    panelGbc.gridx = 0;
	    panelGbc.gridy = 1;
	    watchlistPanel.add(new JLabel("Watchlist:"), panelGbc);
	    panelGbc.gridx = 1;
	    watchlistPanel.add(watchlistComboBox, panelGbc);

	    addWatchlistButton = createButton("Add", new Color(100, 149, 237));
	    addWatchlistButton.setPreferredSize(new Dimension(80, 25));
	    addWatchlistButton.addActionListener(e -> addNewWatchlist());
	    editWatchlistButton = createButton("Edit", new Color(100, 149, 237));
	    editWatchlistButton.setPreferredSize(new Dimension(80, 25));
	    editWatchlistButton.addActionListener(e -> editCurrentWatchlist());
	    panelGbc.gridx = 0;
	    panelGbc.gridy = 2;
	    watchlistPanel.add(addWatchlistButton, panelGbc);
	    panelGbc.gridx = 1;
	    watchlistPanel.add(editWatchlistButton, panelGbc);

	    gbc.gridx = 0;
	    gbc.gridy = 0;
	    gbc.weightx = 0.33;
	    controlPanel.add(watchlistPanel, gbc);

	    // Live Update Panel
	    JPanel liveUpdatePanel = new JPanel(new GridBagLayout());
	    liveUpdatePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Live Updates"));
	    panelGbc.gridx = 0;
	    panelGbc.gridy = 0;

	    startLiveButton = createButton("Start", new Color(50, 200, 50));
	    startLiveButton.setPreferredSize(new Dimension(80, 25));
	    stopLiveButton = createButton("Stop", new Color(200, 50, 50));
	    stopLiveButton.setPreferredSize(new Dimension(80, 25));
	    stopLiveButton.setEnabled(false);
	    refreshIntervalComboBox = new JComboBox<>(refreshIntervals);
	    refreshIntervalComboBox.setSelectedItem(currentRefreshInterval);
	    refreshIntervalComboBox.setPreferredSize(new Dimension(80, 25));
	    refreshIntervalComboBox.addActionListener(e -> {
	        currentRefreshInterval = (Integer) refreshIntervalComboBox.getSelectedItem();
	        if (isLive) {
	            stopLiveUpdates();
	            startLiveUpdates();
	        }
	    });
	    panelGbc.gridx = 0;
	    panelGbc.gridy = 0;
	    liveUpdatePanel.add(startLiveButton, panelGbc);
	    panelGbc.gridx = 1;
	    liveUpdatePanel.add(stopLiveButton, panelGbc);
	    panelGbc.gridx = 2;
	    liveUpdatePanel.add(new JLabel("Refresh (s):"), panelGbc);
	    panelGbc.gridx = 3;
	    liveUpdatePanel.add(refreshIntervalComboBox, panelGbc);

	    checkBullishCheckbox = new JCheckBox("Check Bullish");
	    checkBullishCheckbox.setPreferredSize(new Dimension(100, 25));
	    alarmOnCheckbox = new JCheckBox("Alarm On");
	    alarmOnCheckbox.setPreferredSize(new Dimension(80, 25));
	    alarmOnCheckbox.addActionListener(e -> {
	        isAlarmActive = alarmOnCheckbox.isSelected();
	        logToConsole("Alarm active status changed to: " + isAlarmActive);
	    });
	    stopAlarmButton = createButton("Stop Alarm", new Color(200, 50, 50));
	    stopAlarmButton.setPreferredSize(new Dimension(100, 25));
	    panelGbc.gridx = 0;
	    panelGbc.gridy = 1;
	    liveUpdatePanel.add(checkBullishCheckbox, panelGbc);
	    panelGbc.gridx = 1;
	    liveUpdatePanel.add(alarmOnCheckbox, panelGbc);
	    panelGbc.gridx = 2;
	    panelGbc.gridwidth = 2;
	    liveUpdatePanel.add(stopAlarmButton, panelGbc);
	    panelGbc.gridwidth = 1;

	    symbolExceptionsField = new JTextField(8);
	    symbolExceptionsField.setPreferredSize(new Dimension(100, 25));
	    panelGbc.gridx = 0;
	    panelGbc.gridy = 2;
	    liveUpdatePanel.add(new JLabel("Symbol Exceptions:"), panelGbc);
	    panelGbc.gridx = 1;
	    panelGbc.gridwidth = 3;
	    liveUpdatePanel.add(symbolExceptionsField, panelGbc);
	    panelGbc.gridwidth = 1;

	    startLiveButton.addActionListener(e -> {
	        if (checkBullishCheckbox.isSelected()) {
	            isAlarmActive = alarmOnCheckbox.isSelected();
	            stopAlarmButton.setEnabled(isAlarmActive);
	            alarmOnCheckbox.setEnabled(true);
	            if (isAlarmActive && alertManager == null) {
	                logToConsole("AlertManager is null, cannot enable alarms");
	                isAlarmActive = false;
	                alarmOnCheckbox.setSelected(false);
	                stopAlarmButton.setEnabled(false);
	            }
	        } else {
	            isAlarmActive = false;
	            alarmOnCheckbox.setSelected(false);
	            stopAlarmButton.setEnabled(false);
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
	    stopAlarmButton.addActionListener(e -> {
	        isAlarmActive = false;
	        alarmOnCheckbox.setSelected(false);
	        stopBuzzer();
	        logToConsole("Alarm stopped.");
	    });

	    gbc.gridx = 1;
	    gbc.gridy = 0;
	    gbc.weightx = 0.33;
	    controlPanel.add(liveUpdatePanel, gbc);

	    // Table Actions Panel
	    JPanel tableActionsPanel = new JPanel(new GridBagLayout());
	    tableActionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Table Actions"));
	    panelGbc.gridx = 0;
	    panelGbc.gridy = 0;

	    refreshButton = createButton("Refresh", new Color(70, 130, 180));
	    refreshButton.setPreferredSize(new Dimension(80, 25));
	    toggleVisibilityButton = createButton("Toggle Selected", new Color(255, 165, 0));
	    toggleVisibilityButton.setPreferredSize(new Dimension(120, 25));
	    showAllButton = createButton("Show All", new Color(100, 149, 237));
	    showAllButton.setPreferredSize(new Dimension(80, 25));
	    panelGbc.gridx = 0;
	    panelGbc.gridy = 0;
	    tableActionsPanel.add(refreshButton, panelGbc);
	    panelGbc.gridx = 1;
	    tableActionsPanel.add(toggleVisibilityButton, panelGbc);
	    panelGbc.gridx = 2;
	    tableActionsPanel.add(showAllButton, panelGbc);

	    listSymbolsButton = createButton("List Symbols", new Color(100, 149, 237));
	    listSymbolsButton.setPreferredSize(new Dimension(100, 25));
	    checkBullishButton = createButton("Check Bullish", new Color(100, 149, 237));
	    checkBullishButton.setPreferredSize(new Dimension(100, 25));
	    uncheckAllButton = createButton("Uncheck All", new Color(100, 149, 237));
	    uncheckAllButton.setPreferredSize(new Dimension(100, 25));
	    panelGbc.gridx = 0;
	    panelGbc.gridy = 1;
	    tableActionsPanel.add(listSymbolsButton, panelGbc);
	    panelGbc.gridx = 1;
	    tableActionsPanel.add(checkBullishButton, panelGbc);
	    panelGbc.gridx = 2;
	    tableActionsPanel.add(uncheckAllButton, panelGbc);

	    refreshButton.addActionListener(e -> {
	        statusLabel.setText("Status: Refreshing data...");
	        refreshData();
	        statusLabel.setText("Status: Data refreshed at "
	                + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
	    });
	    toggleVisibilityButton.addActionListener(e -> toggleSelectedRowsVisibility());
	    showAllButton.addActionListener(e -> showAllRows());
	    listSymbolsButton.addActionListener(e -> listVisibleSymbols());
	    checkBullishButton.addActionListener(e -> checkBullishUptrendNeutralRows());
	    uncheckAllButton.addActionListener(e -> uncheckAllRows());

	    gbc.gridx = 2;
	    gbc.gridy = 0;
	    gbc.weightx = 0.33;
	    controlPanel.add(tableActionsPanel, gbc);

	    // Status Label
	    statusLabel = new JLabel("Status: Ready");
	    statusLabel.setPreferredSize(new Dimension(400, 25));
	    gbc.gridx = 0;
	    gbc.gridy = 1;
	    gbc.gridwidth = 3;
	    gbc.weightx = 1.0;
	    controlPanel.add(statusLabel, gbc);

	    // Wrap controlPanel in JScrollPane
	    JScrollPane controlScrollPane = new JScrollPane(controlPanel,
	            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
	            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    controlScrollPane.setPreferredSize(new Dimension(1200, 150));

	    // Create Filters Panel
	    JPanel filtersPanel = new JPanel(new GridBagLayout());
	    filtersPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Filters"));
	    GridBagConstraints filterGbc = new GridBagConstraints();
	    filterGbc.insets = new Insets(3, 3, 3, 3);
	    filterGbc.fill = GridBagConstraints.HORIZONTAL;
	    filterGbc.weightx = 1.0;

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

	    // Add components to filters panel
	    filterGbc.gridx = 0;
	    filterGbc.gridy = 0;
	    filtersPanel.add(prevVolLabel, filterGbc);
	    
	    filterGbc.gridx = 1;
	    filtersPanel.add(prevVolMinField, filterGbc);
	    
	    filterGbc.gridx = 2;
	    filtersPanel.add(new JLabel("to"), filterGbc);
	    
	    filterGbc.gridx = 3;
	    filtersPanel.add(prevVolMaxField, filterGbc);
	    
	    filterGbc.gridx = 0;
	    filterGbc.gridy = 1;
	    filtersPanel.add(currentVolLabel, filterGbc);
	    
	    filterGbc.gridx = 1;
	    filtersPanel.add(currentVolMinField, filterGbc);
	    
	    filterGbc.gridx = 2;
	    filtersPanel.add(new JLabel("to"), filterGbc);
	    
	    filterGbc.gridx = 3;
	    filtersPanel.add(currentVolMaxField, filterGbc);

	    // Add apply filters button
	    JButton applyFiltersButton = createButton("Apply Filters", new Color(70, 130, 180));
	    applyFiltersButton.setPreferredSize(new Dimension(120, 25));
	    applyFiltersButton.addActionListener(e -> refreshData());
	    filterGbc.gridx = 0;
	    filterGbc.gridy = 2;
	    filterGbc.gridwidth = 4;
	    filtersPanel.add(applyFiltersButton, filterGbc);

	    // Add the control panel and filters panel to a container panel
	    JPanel topContainerPanel = new JPanel(new BorderLayout());
	    topContainerPanel.add(controlScrollPane, BorderLayout.NORTH);
	    topContainerPanel.add(filtersPanel, BorderLayout.SOUTH);

	    alertManager = new AlertManager(null, null, timeRangeLabel);

	    // Table Setup
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
	                    c.setBackground(percent >= 0 ? new Color(200, 255, 200) : new Color(255, 200, 200));
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
	                c.setForeground(bgColor.getRed() + bgColor.getGreen() + bgColor.getBlue() > 500 ? Color.BLACK : Color.WHITE);
	                return c;
	            }
	            Color bgColor = statusColors.getOrDefault(status, Color.WHITE);
	            c.setBackground(bgColor);
	            int brightness = (bgColor.getRed() + bgColor.getGreen() + bgColor.getBlue()) / 3;
	            c.setForeground(brightness > 150 ? Color.BLACK : Color.WHITE);
	            return c;
	        }
	    };

	    JCheckBox checkBoxEditor = new JCheckBox();
	    checkBoxEditor.setHorizontalAlignment(JLabel.CENTER);
	    dashboardTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(checkBoxEditor));

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

	    JScrollPane tableScrollPane = new JScrollPane(dashboardTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
	            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    tableScrollPane.setPreferredSize(new Dimension(1100, 400));

	    JPanel mainContentPanel = new JPanel(new BorderLayout());
	    mainContentPanel.add(topContainerPanel, BorderLayout.NORTH);
	    mainContentPanel.add(tableScrollPane, BorderLayout.CENTER);

	    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	    splitPane.setTopComponent(mainContentPanel);
	    splitPane.setBottomComponent(consolePanel);
	    splitPane.setResizeWeight(0.8);
	    splitPane.setDividerLocation(500);

	    add(timeRangePanel, BorderLayout.NORTH);
	    add(selectionPanel, BorderLayout.SOUTH);
	    add(splitPane, BorderLayout.CENTER);
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
	        // Check previous volume filter
	        if (!prevVolMinField.getText().isEmpty()) {
	            long min = Long.parseLong(prevVolMinField.getText());
	            if (prevVol < min) return false;
	        }
	        if (!prevVolMaxField.getText().isEmpty()) {
	            long max = Long.parseLong(prevVolMaxField.getText());
	            if (prevVol > max) return false;
	        }

	        // Check current volume filter
	        if (!currentVolMinField.getText().isEmpty()) {
	            long min = Long.parseLong(currentVolMinField.getText());
	            if (currentVol < min) return false;
	        }
	        if (!currentVolMaxField.getText().isEmpty()) {
	            long max = Long.parseLong(currentVolMaxField.getText());
	            if (currentVol > max) return false;
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
        Set<String> validStatuses = new HashSet<>(Arrays.asList("Bullish Crossover", "Bullish", "Strong Uptrend",
                "Mild Uptrend", "Neutral", "↑ Uptrend", "Buy"));
        // Parse symbol exceptions from text field
        Set<String> excludedSymbols = new HashSet<>();
        String exceptionsText = symbolExceptionsField.getText().trim();
        if (!exceptionsText.isEmpty()) {
            String[] exceptions = exceptionsText.split("\\s*,\\s*");
            for (String symbol : exceptions) {
                if (!symbol.trim().isEmpty()) {
                    excludedSymbols.add(symbol.trim().toUpperCase());
                }
            }
            if (!excludedSymbols.isEmpty()) {
                logToConsole("Excluding symbols: " + String.join(", ", excludedSymbols));
            }
        }

        Vector<?> rawData = tableModel.getDataVector();
        logToConsole("checkBullishSymbolsAndLog: rawData size = " + (rawData != null ? rawData.size() : "null"));
        logToConsole("checkBullishSymbolsAndLog: Table row count = " + tableModel.getRowCount() + ", hidden symbols = " + ((FilterableTableModel) tableModel).hiddenSymbols.size());
        if (rawData == null || rawData.isEmpty()) {
            logToConsole("checkBullishSymbolsAndLog: No data in tableModel");
            return;
        }
        List<String> bullishSymbols = new ArrayList<>();
        for (int i = 0; i < rawData.size(); i++) {
            @SuppressWarnings("unchecked")
            Vector<Object> row = (Vector<Object>) rawData.get(i);
            String symbol = (String) row.get(1);
            if (tableModel.isSymbolHidden(symbol) || excludedSymbols.contains(symbol)) {
                continue;
            }
            boolean isBullish = true;
            for (int j = 6; j < tableModel.getColumnCount(); j++) {
                Object value = row.get(j);
                String status = (value != null) ? value.toString().trim() : "";
                if (status.startsWith("↑") || status.startsWith("↓")) {
                    status = status.split(" \\(")[0]; // Extract trend before parenthesis
                }
                if (!validStatuses.contains(status)) {
                    isBullish = false;
                    break;
                }
            }
            if (isBullish) {
                bullishSymbols.add(symbol);
            }
        }
        if (!bullishSymbols.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Bullish Crossover Symbols: ");
            sb.append(String.join(", ", bullishSymbols));
            logToConsole(sb.toString());
            if (alarmOnCheckbox.isSelected() && isAlarmActive && alertManager != null) {
                if (buzzerScheduler == null || buzzerScheduler.isShutdown()) {
                    buzzerScheduler = Executors.newSingleThreadScheduledExecutor();
                    String alertMessage = "Bullish Crossover detected for: " + String.join(", ", bullishSymbols);
                    buzzerScheduler.scheduleAtFixedRate(() -> {
                        SwingUtilities.invokeLater(() -> {
                            if (isAlarmActive && alarmOnCheckbox.isSelected() && alertManager != null) {
                                alertManager.sendBuzzAlert(alertMessage);
                                logToConsole("Buzz alert triggered for bullish symbols: " + sb.toString());
                            } else {
                                stopBuzzer();
                            }
                        });
                    }, 0, 5, TimeUnit.SECONDS);
                    logToConsole("Started buzzer for bullish symbols.");
                }
            } else if (alertManager == null) {
                logToConsole("AlertManager is null, cannot send buzz alert");
            }
        } else {
            stopBuzzer(); // Stop buzzing if no bullish symbols
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
		Set<String> validStatuses = new HashSet<>(Arrays.asList("Bullish Crossover", "Bullish", "Strong Uptrend",
				"Mild Uptrend", "Neutral", "↑ Uptrend"));
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
			for (int col = 6; col < tableModel.getColumnCount(); col++) {
				Object value = row.get(col);
				String status = (value != null) ? value.toString().trim() : "";

				if (status.startsWith("↑") || status.startsWith("↓")) {
					status = status.split(" ")[0] + " " + status.split(" ")[1];
				}

				if (!validStatuses.contains(status)) {
					hasInvalidStatus = true;
					break;
				}
			}

			if (hasInvalidStatus) {
				row.set(0, true);
				updatedRows++;
			}
		}

		tableModel.fireTableDataChanged();
		logToConsole(
				"Checked " + updatedRows + " rows with at least one non-Bullish, non-Uptrend, or non-Neutral status.");
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

	private void applyChanges() {
		selectedTimeframes.clear();
		for (Map.Entry<String, JCheckBox> entry : timeframeCheckBoxes.entrySet()) {
			if (entry.getValue().isSelected()) {
				selectedTimeframes.add(entry.getKey());
			}
		}

		selectedIndicators.clear();
		for (Map.Entry<String, JCheckBox> entry : indicatorCheckBoxes.entrySet()) {
			if (entry.getValue().isSelected()) {
				selectedIndicators.add(entry.getKey());
			}
		}

		updateTableColumns();

		Vector<?> rawData = tableModel.getDataVector();
		ExecutorService executor = Executors.newFixedThreadPool(4);

		for (int i = 0; i < rawData.size(); i++) {
			Vector<?> row = (Vector<?>) rawData.get(i);
			String symbol = (String) row.get(1);

			if (!tableModel.isSymbolHidden(symbol)) {
				final int rowIndex = i;
				executor.submit(() -> {
					try {
						Map<String, AnalysisResult> results = analyzeSymbol(symbol);
						PriceData priceData = fetchLatestPrice(symbol);

						SwingUtilities.invokeLater(() -> {
							updateRowData(tableModel, rowIndex, priceData, results);
						});
					} catch (Exception e) {
						SwingUtilities.invokeLater(() -> {
							logToConsole("Error updating data for " + symbol + ": " + e.getMessage());
						});
					}
				});
			}
		}

		executor.shutdown();
		try {
			if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
				executor.shutdownNow();
				logToConsole("Data refresh timed out after 60 seconds");
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			logToConsole("Data refresh interrupted: " + e.getMessage());
		}
	}

	private void updateTableColumnWidths() {
		dashboardTable.getColumnModel().getColumn(0).setPreferredWidth(50);
		dashboardTable.getColumnModel().getColumn(1).setPreferredWidth(80);
		dashboardTable.getColumnModel().getColumn(2).setPreferredWidth(100);
		dashboardTable.getColumnModel().getColumn(3).setPreferredWidth(100);
		dashboardTable.getColumnModel().getColumn(4).setPreferredWidth(100);
		dashboardTable.getColumnModel().getColumn(5).setPreferredWidth(80);
		for (int i = 6; i < dashboardTable.getColumnCount(); i++) {
			dashboardTable.getColumnModel().getColumn(i).setPreferredWidth(120);
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
			consoleArea.append(
					"[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + message + "\n");
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
		List<String> columns = new ArrayList<>();
		columns.add("Select");
		columns.add("Symbol");
		columns.add("Latest Price");
		columns.add("Prev Daily Vol");
		columns.add("Current Daily Vol");
		columns.add("% Change");

		//String[] orderedTimeframes = {"1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min" };

		for (String tf : this.allTimeframes) {
			if (selectedTimeframes.contains(tf)) {
				for (String ind : selectedIndicators) {
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

	private void updateRowData(FilterableTableModel model, int row, PriceData priceData, Map<String, AnalysisResult> results) {
	    SwingUtilities.invokeLater(() -> {
	        if (row < 0 || row >= model.getRowCount()) {
	            logToConsole("Invalid row index: " + row + " for symbol update, row count: " + model.getRowCount());
	            return;
	        }
	        if (priceData == null || results == null) {
	            logToConsole("Null priceData or results for row: " + row);
	            return;
	        }

	        String symbol = (String) model.getValueAt(row, 1);
	        model.setValueAt(Boolean.FALSE, row, 0); // Checkbox
	        model.setValueAt(priceData.latestPrice, row, 2); // Price
	        model.setValueAt(priceData.prevDailyVol, row, 3); // Previous Daily Volume
	        model.setValueAt(priceData.currentDailyVol, row, 4); // Current Daily Volume
	        model.setValueAt(priceData.percentChange, row, 5); // Percent Change

	        int colIndex = 6;
	        //String[] orderedTimeframes = { "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min" };
	        for (String tf : allTimeframes) {
	            if (selectedTimeframes.contains(tf)) {
	                AnalysisResult result = results.get(tf);
	                if (result != null) {
	                    for (String ind : selectedIndicators) {
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
	                        }
	                    }
	                } else {
	                    for (String ind : selectedIndicators) {
	                        model.setValueAt("N/A", row, colIndex++);
	                    }
	                }
	            }
	        }

	        model.fireTableRowsUpdated(row, row);
	        logToConsole("Updated row for symbol: " + symbol + " at index: " + row + ", columns updated: " + colIndex);
	    });
	}


	private void addNewSymbolRow(String symbol, PriceData priceData, Map<String, AnalysisResult> results) {
	    Vector<Object> row = new Vector<>();
	    row.add(false);
	    row.add(symbol);
	    row.add(priceData.latestPrice);
	    row.add(priceData.prevDailyVol);
	    row.add(priceData.currentDailyVol);
	    row.add(priceData.percentChange);

	    //String[] orderedTimeframes = { "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min" };
	    for (String tf : this.allTimeframes) {
	        if (selectedTimeframes.contains(tf)) {
	            AnalysisResult result = results.get(tf);
	            if (result != null) {
	                for (String ind : selectedIndicators) {
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
	                    }
	                }
	            } else {
	                for (String ind : selectedIndicators) {
	                    row.add("N/A");
	                }
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
			try {
				SwingUtilities.invokeLater(() -> refreshData());
			} catch (Exception e) {
				logToConsole("Error during live update: " + e.getMessage());
			}
		}, 0, currentRefreshInterval, TimeUnit.SECONDS);
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
		Map<String, AnalysisResult> results = new ConcurrentHashMap<>();

		for (String timeframe : selectedTimeframes) {
			try {
				StockDataResult stockData = fetchStockData(symbol, timeframe, 1000);
				BarSeries series = parseJsonToBarSeries(stockData, symbol, timeframe);
				results.put(timeframe, performTechnicalAnalysis(series));
			} catch (Exception e) {
				logToConsole("Error analyzing " + symbol + " " + timeframe + ": " + e.getMessage());
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

	private AnalysisResult performTechnicalAnalysis(BarSeries series) {
		AnalysisResult result = new AnalysisResult();
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		int endIndex = series.getEndIndex();
		if (endIndex < 0) {
			return result;
		}
		Bar currentBar = series.getBar(endIndex);
		Num currentClose = currentBar.getClosePrice();
		result.setPrice(currentClose.doubleValue());

		if (selectedIndicators.contains("Trend")) {
			SMAIndicator sma50 = new SMAIndicator(closePrice, 50);
			SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
			if (endIndex >= 49) {
				result.setSma50(sma50.getValue(endIndex).doubleValue());
			}
			if (endIndex >= 199) {
				result.setSma200(sma200.getValue(endIndex).doubleValue());
			}
		}

		if (selectedIndicators.contains("RSI")) {
			RSIIndicator rsi = new RSIIndicator(closePrice, 14);
			if (endIndex >= 13) {
				result.setRsi(rsi.getValue(endIndex).doubleValue());
			}
		}

		if (selectedIndicators.contains("MACD")) {
			MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
			EMAIndicator signal = new EMAIndicator(macd, 9);
			if (endIndex >= 34) { // 26 + 9 - 1
				result.setMacd(macd.getValue(endIndex).doubleValue());
				result.setMacdSignal(signal.getValue(endIndex).doubleValue());
				if (endIndex > 0) {
					double prevMacd = macd.getValue(endIndex - 1).doubleValue();
					double prevSignal = signal.getValue(endIndex - 1).doubleValue();
					if (result.macd > result.macdSignal && prevMacd <= prevSignal) {
						result.setMacdStatus("Bullish Crossover");
					} else if (result.macd < result.macdSignal && prevMacd >= prevSignal) {
						result.setMacdStatus("Bearish Crossover");
					} else {
						result.setMacdStatus("Neutral");
					}
				}
			}
		}

		if (selectedIndicators.contains("MACD(5,8,9)")) {
			MACDIndicator macd359 = new MACDIndicator(closePrice, 5, 8);
			EMAIndicator signal359 = new EMAIndicator(macd359, 9);
			if (endIndex >= 16) { // 8 + 9 - 1
				result.setMacd359(macd359.getValue(endIndex).doubleValue());
				result.setMacdSignal359(signal359.getValue(endIndex).doubleValue());
				if (endIndex > 0) {
					double prevMacd = macd359.getValue(endIndex - 1).doubleValue();
					double prevSignal = signal359.getValue(endIndex - 1).doubleValue();
					if (result.macd359 > result.macdSignal359 && prevMacd <= prevSignal) {
						result.setMacd359Status("Bullish Crossover");
					} else if (result.macd359 < result.macdSignal359 && prevMacd >= prevSignal) {
						result.setMacd359Status("Bearish Crossover");
					} else {
						result.setMacd359Status("Neutral");
					}
				}
			}
		}

		if (selectedIndicators.contains("PSAR(0.01)") || selectedIndicators.contains("PSAR(0.05)")) {
			try {
				Num zero01 = series.numOf(0.01);
				Num zero05 = series.numOf(0.05);
				if (selectedIndicators.contains("PSAR(0.01)")) {
					ParabolicSarIndicator psar001 = new ParabolicSarIndicator(series, zero01, zero01, zero01);
					if (endIndex >= 0) {
						result.setPsar001(psar001.getValue(endIndex).doubleValue());
						result.setPsar001Trend(
								result.psar001 < currentClose.doubleValue() ? "↑ Uptrend" : "↓ Downtrend");
						if (Double.isInfinite(result.psar001) || Double.isNaN(result.psar001)) {
							result.setPsar001(result.price);
							result.setPsar001Trend("N/A");
						}
					}
				}
				if (selectedIndicators.contains("PSAR(0.05)")) {
					ParabolicSarIndicator psar005 = new ParabolicSarIndicator(series, zero05, zero05, zero05);
					if (endIndex >= 0) {
						result.setPsar005(psar005.getValue(endIndex).doubleValue());
						result.setPsar005Trend(
								result.psar005 < currentClose.doubleValue() ? "↑ Uptrend" : "↓ Downtrend");
						if (Double.isInfinite(result.psar005) || Double.isNaN(result.psar005)) {
							result.setPsar005(result.price);
							result.setPsar005Trend("N/A");
						}
					}
				}
			} catch (Exception e) {
				logToConsole("Error calculating PSAR: " + e.getMessage());
				result.setPsar001(result.price);
				result.setPsar005(result.price);
				result.setPsar001Trend("N/A");
				result.setPsar005Trend("N/A");
			}
		}

		return result;
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
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			EnhancedStockDashboardV11 dashboard = new EnhancedStockDashboardV11();
			dashboard.setVisible(true);
		});
	}
}