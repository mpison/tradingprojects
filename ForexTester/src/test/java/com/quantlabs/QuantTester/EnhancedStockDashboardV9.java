package com.quantlabs.QuantTester;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EnhancedStockDashboardV9 extends JFrame {
    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey = "PK4UOZQDJJZ6WBAU52XM";
    private final String apiSecret = "Fag4ha2D58VyL0okwXgBHD1IvhoptmI2KiacMaNG";
    
    private final String[] symbols = { "AAPL", "AVGO", "PLTR", "MSFT", "GOOGL",
     "AMZN", "TSLA", "NVDA", "META", "NFLX", "WMT", "JPM", "LLY", "V", "ORCL",
     "MA", "XOM", "COST", "JNJ", "HD", "UNH" , "DLTR"};
    
    private static final Map<String, Integer> ALPACA_TIME_LIMITS = Map.of(
        "1Min", 4,
        "5Min", 7,
        "15Min", 7,
        "30Min", 30,
        "1H", 30,
        "4H", 30,
        "1D", 60
    );

    private static final Map<String, Integer> YAHOO_TIME_LIMITS = Map.of(
        "1Min", 7,
        "5Min", 60,
        "15Min", 60,
        "30Min", 60,
        "1H", 60,
        "4H", 60,
        "1D", 60
    );
    
    private final String[] dataSources = { "Alpaca", "Yahoo" };
    private final String[] allTimeframes = { "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min" };
    private final String[] allIndicators = { "Trend", "RSI", "MACD", "MACD(5,8,9)", "PSAR(0.01)", "PSAR(0.05)", "Action" };
    
    private JTable dashboardTable;
    private FilterableTableModel tableModel;
    private JButton refreshButton;
    private JButton startLiveButton;
    private JButton stopLiveButton;
    private JButton toggleVisibilityButton;
    private JButton showAllButton;
    private JButton addWatchlistButton;
    private JButton editWatchlistButton;
    private JComboBox<String> dataSourceComboBox;
    private JComboBox<String> watchlistComboBox;
    private ScheduledExecutorService scheduler;
    private boolean isLive = false;
    private final Map<String, Color> statusColors = new HashMap<>();
    private final Map<String, List<String>> watchlists = new HashMap<>();
    private String currentWatchlist = "Default";
    private String currentDataSource = "Yahoo";
    
    private List<String> watchListException = Arrays.asList("Default" , "YahooDOW", "YahooNASDAQ" , "YahooSP500", "YahooRussel2k", "YahooPennyBy3MonVol", "YahooPennyByPercentChange");
    
    // New UI components for time range selection
    private JRadioButton currentTimeRadio;
    private JRadioButton customRangeRadio;
    private ButtonGroup timeRangeGroup;
    private JLabel timeRangeLabel;
    private JButton clearCustomRangeButton;
    private JXDatePicker startDatePicker;
    private JXDatePicker endDatePicker;
    
    // New console components
    private JTextArea consoleArea;
    private JScrollPane consoleScrollPane;
    private JPanel consolePanel;
    private JPanel timeRangePanel;
    
    // New components for indicator and timeframe selection
    private JPanel selectionPanel;
    private Map<String, JCheckBox> timeframeCheckBoxes = new HashMap<>();
    private Map<String, JCheckBox> indicatorCheckBoxes = new HashMap<>();
    private Set<String> selectedTimeframes = new HashSet<>();
    private Set<String> selectedIndicators = new HashSet<>();
    
    YahooFinanceQueryScreenerService yahooQueryFinance = new YahooFinanceQueryScreenerService();

    private class FilterableTableModel extends DefaultTableModel {
        private Set<String> hiddenSymbols = new HashSet<>();
        private Map<String, Integer> symbolToRowMap = new ConcurrentHashMap<>();

        public FilterableTableModel(Object[] columnNames, int rowCount) {
            super(columnNames, rowCount);
        }

        @Override
        public int getRowCount() {
            if (hiddenSymbols == null) {
                hiddenSymbols = new HashSet<>();
            }
            return super.getRowCount() - hiddenSymbols.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            int actualRow = getActualRowIndex(row);
            return super.getValueAt(actualRow, column);
        }

        private int getActualRowIndex(int visibleRow) {
            int actualRow = -1;
            int visibleCount = -1;

            while (visibleCount < visibleRow) {
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
            return column == 0 ? Boolean.class : super.getColumnClass(column);
        }

        @Override
        public void addRow(Object[] rowData) {
            super.addRow(rowData);
            String symbol = (String) rowData[1];
            symbolToRowMap.put(symbol, super.getRowCount() - 1);
        }
    }

    public EnhancedStockDashboardV9() {
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
        watchlists.put("ETF", Arrays.asList("QQQ", "SPY", "IGV", "VGT", "FTEC", "SMH"));
        watchlists.put("Blue Chips", Arrays.asList("WMT", "JPM", "V", "MA", "XOM", "JNJ", "HD"));
        watchlists.put("Financial", Arrays.asList("V", "JPM", "MA", "AXP", "BAC", "COIN", "BLK", "WFC", "BLK", "MS", "C", "BX", "SOFI"));
        watchlists.put("SemiConductors", Arrays.asList("NVDA", "AVGO", "AMD", "QCOM", "ADI", "TXN", "MU", "INTC", "ON", "SWKS"));
        watchlists.put("Health Care", Arrays.asList("LLY", "JNJ", "ABBV", "AMGN", "MRK", "PFE", "GILD", "BMY", "BIIB"));
        watchlists.put("Energy", Arrays.asList("XOM", "COP", "CVX", "TPL", "EOG", "HES", "FANG", "MPC", "PSX", "VLO"));
        watchlists.put("HighVolumes", Arrays.asList("CYCC", "XAGE", "KAPA", "PLRZ", "ANPA", "IBG", "CRML", "GRO", "USAR", "SBET","MP","SLDP","OPEN","TTD","PROK"));
        watchlists.put("Top50", Arrays.asList("NVDA", "MSFT", "AAPL", "AMZN", "GOOGL", "META", "2223.SR", "AVGO", "TSM", "TSLA"
                                            ,"BRK-B", "JPM", "WMT", "ORCL", "LLY", "V", "TCEHY", "NFLX", "MA", "XOM"
                                            ,"COST", "JNJ", "PG", "PLTR", "1398.HK", "SAP", "HD", "BAC", "ABBV", "005930.KS"
                                            ,"KO", "601288.SS", "RMS.PA", "NVO", "ASML", "601939.SS", "BABA", "PM", "GE"
                                            ,"CSCO", "CVX", "IBM", "UNH", "AMD", "ROG.SW", "TMUS", "WFC", "NESN.SW", "PRX.AS"));
        watchlists.put("YahooDOW", Arrays.asList(""));
        watchlists.put("YahooNASDAQ", Arrays.asList(""));
        watchlists.put("YahooSP500", Arrays.asList(""));
        watchlists.put("YahooRussel2k", Arrays.asList(""));
        watchlists.put("YahooPennyBy3MonVol", Arrays.asList(""));
        watchlists.put("YahooPennyByPercentChange", Arrays.asList(""));
        
        currentWatchlist = "YahooDOW";
    }

    private void initializeSelections() {
        // Initialize with specific timeframes selected by default
        selectedTimeframes.addAll(Arrays.asList("1D", "4H", "30Min"));
        selectedIndicators.addAll(Arrays.asList("RSI", "MACD", "MACD(5,8,9)", "PSAR(0.01)", "PSAR(0.05)"));
    }

    private void setupUI() {
        setTitle("Enhanced Stock Dashboard (IEX Feed)");
        setSize(2000, 1000);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create time range panel
        timeRangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        currentTimeRadio = new JRadioButton("Current Time", true);
        customRangeRadio = new JRadioButton("Custom Range");
        timeRangeGroup = new ButtonGroup();
        timeRangeGroup.add(currentTimeRadio);
        timeRangeGroup.add(customRangeRadio);
        
        startDatePicker = new JXDatePicker();
        startDatePicker.setFormats("yyyy-MM-dd");
        startDatePicker.setEnabled(false);
        endDatePicker = new JXDatePicker();
        endDatePicker.setFormats("yyyy-MM-dd");
        endDatePicker.setEnabled(false);
        
        clearCustomRangeButton = new JButton("Clear Custom Range");
        clearCustomRangeButton.setEnabled(false);
        
        timeRangeLabel = new JLabel("Time Range: Current Time");
        
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
        
        // Create selection panel for timeframes and indicators
        selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        selectionPanel.add(new JLabel("Timeframes:"));
        
        for (String timeframe : allTimeframes) {
            JCheckBox cb = new JCheckBox(timeframe, selectedTimeframes.contains(timeframe));
            timeframeCheckBoxes.put(timeframe, cb);
            selectionPanel.add(cb);
        }
        
        selectionPanel.add(new JLabel("Indicators:"));
        
        for (String indicator : allIndicators) {
            JCheckBox cb = new JCheckBox(indicator, selectedIndicators.contains(indicator));
            indicatorCheckBoxes.put(indicator, cb);
            selectionPanel.add(cb);
        }
        
        JButton applyChangesButton = createButton("Apply Changes", new Color(70, 130, 180));
        applyChangesButton.addActionListener(e -> applyChanges());
        selectionPanel.add(applyChangesButton);
        
        // Create console area
        consoleArea = new JTextArea(5, 80);
        consoleArea.setEditable(false);
        consoleScrollPane = new JScrollPane(consoleArea);
        consolePanel = new JPanel(new BorderLayout());
        consolePanel.add(new JLabel("Console Output:"), BorderLayout.NORTH);
        consolePanel.add(consoleScrollPane, BorderLayout.CENTER);
        
        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        
        dataSourceComboBox = new JComboBox<>(dataSources);
        dataSourceComboBox.setSelectedItem(currentDataSource);
        dataSourceComboBox.addActionListener(e -> {
            currentDataSource = (String) dataSourceComboBox.getSelectedItem();
            refreshData();
        });
        controlPanel.add(new JLabel("Data Source:"));
        controlPanel.add(dataSourceComboBox);
        
        watchlistComboBox = new JComboBox<>(watchlists.keySet().toArray(new String[0]));
        watchlistComboBox.setSelectedItem(currentWatchlist);
        watchlistComboBox.addActionListener(e -> {
            currentWatchlist = (String) watchlistComboBox.getSelectedItem();
            refreshData();
        });
        controlPanel.add(new JLabel("Watchlist:"));
        controlPanel.add(watchlistComboBox);
        
        addWatchlistButton = createButton("Add Watchlist", new Color(100, 149, 237));
        editWatchlistButton = createButton("Edit Watchlist", new Color(100, 149, 237));
        addWatchlistButton.addActionListener(e -> addNewWatchlist());
        editWatchlistButton.addActionListener(e -> editCurrentWatchlist());
        controlPanel.add(addWatchlistButton);
        controlPanel.add(editWatchlistButton);

        refreshButton = createButton("Refresh", new Color(70, 130, 180));
        startLiveButton = createButton("Start Live Updates", new Color(50, 200, 50));
        stopLiveButton = createButton("Stop Live Updates", new Color(200, 50, 50));
        toggleVisibilityButton = createButton("Toggle Selected Rows", new Color(255, 165, 0));
        showAllButton = createButton("Show All Rows", new Color(100, 149, 237));
        JButton listSymbolsButton = createButton("List Symbols", new Color(100, 149, 237));
        JButton checkBullishButton = createButton("Check Bullish/Uptrend/Neutral", new Color(100, 149, 237));
        JButton uncheckAllButton = createButton("Uncheck All", new Color(100, 149, 237));
        stopLiveButton.setEnabled(false);

        controlPanel.add(refreshButton);
        controlPanel.add(startLiveButton);
        controlPanel.add(stopLiveButton);
        controlPanel.add(toggleVisibilityButton);
        controlPanel.add(showAllButton);
        controlPanel.add(listSymbolsButton);
        controlPanel.add(checkBullishButton);
        controlPanel.add(uncheckAllButton);

        JLabel statusLabel = new JLabel("Status: Ready");
        controlPanel.add(statusLabel);

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

        listSymbolsButton.addActionListener(e -> listVisibleSymbols());
        checkBullishButton.addActionListener(e -> checkBullishUptrendNeutralRows());
        uncheckAllButton.addActionListener(e -> uncheckAllRows());

        JScrollPane tableScrollPane = new JScrollPane(dashboardTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScrollPane.setPreferredSize(new Dimension(1900, 600));

        refreshButton.addActionListener(e -> {
            statusLabel.setText("Status: Refreshing data...");
            refreshData();
            statusLabel.setText(
                    "Status: Data refreshed at " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });

        startLiveButton.addActionListener(e -> {
            startLiveUpdates();
            statusLabel.setText("Status: Live updates running - last update at "
                    + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });

        stopLiveButton.addActionListener(e -> {
            stopLiveUpdates();
            statusLabel.setText("Status: Live updates stopped at "
                    + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });

        toggleVisibilityButton.addActionListener(e -> toggleSelectedRowsVisibility());

        showAllButton.addActionListener(e -> showAllRows());

        // Create main content panel
        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.add(controlPanel, BorderLayout.NORTH);
        mainContentPanel.add(tableScrollPane, BorderLayout.CENTER);

        // Create split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(mainContentPanel);
        splitPane.setBottomComponent(consolePanel);
        splitPane.setResizeWeight(0.8);
        splitPane.setDividerLocation(700);

        add(timeRangePanel, BorderLayout.NORTH);
        add(selectionPanel, BorderLayout.SOUTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void uncheckAllRows() {
        Vector<?> rawData = tableModel.getDataVector();
        int updatedRows = 0;

        for (int i = 0; i < rawData.size(); i++) {
            @SuppressWarnings("unchecked")
            Vector<Object> row = (Vector<Object>) rawData.get(i);
            if ((Boolean) row.get(0)) {
                row.set(0, false); // Uncheck the Select checkbox
                updatedRows++;
            }
        }

        tableModel.fireTableDataChanged();
        logToConsole("Unchecked " + updatedRows + " rows.");
    }
    
    private void checkBullishUptrendNeutralRows() {
        Set<String> validStatuses = new HashSet<>(Arrays.asList(
            "Bullish Crossover", "Bullish", "Strong Uptrend", "Mild Uptrend", "Neutral", "↑ Uptrend"
        ));
        Vector<?> rawData = tableModel.getDataVector();
        int updatedRows = 0;

        for (int i = 0; i < rawData.size(); i++) {
            @SuppressWarnings("unchecked")
            Vector<Object> row = (Vector<Object>) rawData.get(i);
            String symbol = (String) row.get(1);
            
            // Skip hidden symbols
            if (tableModel.isSymbolHidden(symbol)) {
                continue;
            }

            boolean hasInvalidStatus = false;
            // Start from column 3 to skip Select, Symbol, and Latest Price
            for (int col = 3; col < tableModel.getColumnCount(); col++) {
                Object value = row.get(col);
                String status = (value != null) ? value.toString().trim() : "";
                
                // Handle PSAR columns which include "↑ Uptrend" or "↓ Downtrend"
                if (status.startsWith("↑") || status.startsWith("↓")) {
                    status = status.split(" ")[0] + " " + status.split(" ")[1];
                }
                
                if (!validStatuses.contains(status)) {
                    hasInvalidStatus = true;
                    break;
                }
            }

            if (hasInvalidStatus) {
                row.set(0, true); // Check the Select checkbox
                updatedRows++;
            }
        }

        tableModel.fireTableDataChanged();
        logToConsole("Checked " + updatedRows + " rows with at least one non-Bullish, non-Uptrend, or non-Neutral status.");
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
        // Update selected timeframes based on checkbox states
        selectedTimeframes.clear();
        for (Map.Entry<String, JCheckBox> entry : timeframeCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedTimeframes.add(entry.getKey());
            }
        }

        // Update selected indicators based on checkbox states
        selectedIndicators.clear();
        for (Map.Entry<String, JCheckBox> entry : indicatorCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedIndicators.add(entry.getKey());
            }
        }

        // Update table columns
        updateTableColumns();

        // Refresh data only for visible symbols
        Vector<?> rawData = tableModel.getDataVector();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        for (int i = 0; i < rawData.size(); i++) {
            Vector<?> row = (Vector<?>) rawData.get(i);
            String symbol = (String) row.get(1);
            
            // Only update visible symbols
            if (!tableModel.isSymbolHidden(symbol)) {
                final int rowIndex = i;
                executor.submit(() -> {
                    try {
                        Map<String, AnalysisResult> results = analyzeSymbol(symbol);
                        double latestPrice = results.getOrDefault(selectedTimeframes.stream().findFirst().orElse("1D"), new AnalysisResult()).price;
                        
                        SwingUtilities.invokeLater(() -> {
                            updateRowData(tableModel, rowIndex, latestPrice, results);
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
        for (int i = 3; i < dashboardTable.getColumnCount(); i++) {
            dashboardTable.getColumnModel().getColumn(i).setPreferredWidth(120);
        }
    }

    private void updateTimeRangeLabel() {
        if (startDatePicker.getDate() != null && endDatePicker.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            timeRangeLabel.setText("Time Range: " + sdf.format(startDatePicker.getDate()) + 
                                " to " + sdf.format(endDatePicker.getDate()));
        } else {
            timeRangeLabel.setText("Time Range: Custom (dates not set)");
        }
    }

    private void logToConsole(String message) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + message + "\n");
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
            JOptionPane.showMessageDialog(this, "Cannot edit the Default watchlist", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        List<String> currentSymbols = watchlists.get(currentWatchlist);
        String symbolsText = String.join(",", currentSymbols);
        
        String newSymbols = JOptionPane.showInputDialog(this, 
            "Edit symbols (comma separated):", 
            symbolsText);
        
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

        // Define the desired order of timeframes
        String[] orderedTimeframes = {"1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min"};
        
        // Add columns in the specified order, only for selected timeframes
        for (String tf : orderedTimeframes) {
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
        
        // Safely retrieve existing data
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

        // Create new table model with updated columns
        tableModel = new FilterableTableModel(newColumnNames, 0);
        dashboardTable.setModel(tableModel);

        // Restore data for existing symbols
        for (Vector<Object> row : oldData) {
            try {
                String symbol = (String) row.get(1);
                Object priceObj = row.get(2);
                double latestPrice = 0.0;
                
                // Safely parse the latest price
                if (priceObj != null && !priceObj.toString().isEmpty()) {
                    try {
                        latestPrice = Double.parseDouble(priceObj.toString());
                    } catch (NumberFormatException e) {
                        logToConsole("Error parsing latest price for " + symbol + ": " + e.getMessage());
                        continue; // Skip this row if price parsing fails
                    }
                }

                Map<String, AnalysisResult> results = analyzeSymbol(symbol);
                addNewSymbolRow(symbol, latestPrice, results);
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
    	
    	if(watchListException.contains(currentWatchlist) && currentWatchlist != "Default") {
    		switch(currentWatchlist) {
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
    	}else {
    	
    		symbolsToProcess = watchlists.get(currentWatchlist);
    	
    	}
    	
        if (symbolsToProcess == null || symbolsToProcess.isEmpty()) {
            return;
        }
        
        tableModel.setRowCount(0);
        
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
                    double latestPrice = fetchLatestPrice(symbol);
                    SwingUtilities.invokeLater(() -> {
                        updateOrAddSymbolRow(symbol, latestPrice, results);
                    });
                } catch (Exception e) {
                    logToConsole("Error analyzing " + symbol + ": " + e.getMessage());
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(this,
                            "Error analyzing " + symbol + ": " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE));
                }
            });
        }
        executor.shutdown();
    }
    
    private void updateOrAddSymbolRow(String symbol, double latestPrice, Map<String, AnalysisResult> results) {
        FilterableTableModel model = (FilterableTableModel) dashboardTable.getModel();
        
        for (int i = 0; i < model.getRowCount(); i++) {
            String rowSymbol = (String) model.getValueAt(i, 1);
            if (rowSymbol.equals(symbol)) {
                updateRowData(model, i, latestPrice, results);
                return;
            }
        }
        
        addNewSymbolRow(symbol, latestPrice, results);
    }

    private void updateRowData(FilterableTableModel model, int row, double latestPrice, 
            Map<String, AnalysisResult> results) {
		model.setValueAt(String.format("%.2f", latestPrice), row, 2);
		
		int colIndex = 3;
		for (String tf : selectedTimeframes) {
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

    private void addNewSymbolRow(String symbol, double latestPrice, Map<String, AnalysisResult> results) {
        Vector<Object> row = new Vector<>();
        row.add(false);
        row.add(symbol);
        row.add(String.format("%.2f", latestPrice));
        
        for (String tf : selectedTimeframes) {
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
        tableModel.addRow(row);
    }
    
    private String getMacdStatus(AnalysisResult result, String macdType) {
        if ("MACD(5,8,9)".equals(macdType)) {
            return result.macd359Status != null ? result.macd359Status : "Neutral";
        } else {
            return result.macdStatus != null ? result.macdStatus : "Neutral";
        }
    }

    private void startLiveUpdates() {
        if (isLive)
            return;

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
        }, 0, 60, TimeUnit.SECONDS);
    }

    private void stopLiveUpdates() {
        if (!isLive)
            return;

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

    private StockDataResult fetchAlpacaStockData(String symbol, String timeframe, int limit, 
                                              ZonedDateTime start, ZonedDateTime end) throws IOException {
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
    
    private StockDataResult fetchYahooStockData(String symbol, String timeframe, int limit,
                                             ZonedDateTime start, ZonedDateTime end) throws IOException {
        String interval = convertTimeframeToYahooInterval(timeframe);
        
        long period1 = start.toEpochSecond();
        long period2 = end.toEpochSecond();
        
        HttpUrl url = HttpUrl.parse("https://query1.finance.yahoo.com/v8/finance/chart/" + symbol)
                .newBuilder()
                .addQueryParameter("interval", interval)
                .addQueryParameter("period1", String.valueOf(period1))
                .addQueryParameter("period2", String.valueOf(period2))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }
            return new StockDataResult(response.body().string(), start, end);
        }
    }
    
    private String convertTimeframeToYahooInterval(String timeframe) {
        switch (timeframe) {
            case "1Min": return "1m";
            case "5Min": return "5m";
            case "15Min": return "15m";
            case "30Min": return "30m";
            case "1H": return "1h";
            case "4H": return "4h";
            case "1D": return "1d";
            default: return "1d";
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
                ZonedDateTime time = ZonedDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(timestamp),
                    ZoneOffset.UTC
                );

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
                        
                        while(hasNextPage && nextPageToken != "null") {
                            String nextPageData = fetchNextPage(symbol, nextPageToken, timeframe, stockData.start, stockData.end);
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
                                ZonedDateTime time = ZonedDateTime.parse(bar.getString("t"), DateTimeFormatter.ISO_ZONED_DATE_TIME);

                                series.addBar(time, bar.getDouble("o"), bar.getDouble("h"), bar.getDouble("l"), bar.getDouble("c"),
                                        bar.getLong("v"));
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

    private String fetchNextPage(String symbol, String nextPageToken, String timeframe, ZonedDateTime start, ZonedDateTime end) throws IOException {
        HttpUrl url = HttpUrl.parse("https://data.alpaca.markets/v2/stocks/" + symbol + "/bars").newBuilder()
                .addQueryParameter("start", start.format(DateTimeFormatter.ISO_INSTANT))
                .addQueryParameter("end", end.format(DateTimeFormatter.ISO_INSTANT))
                .addQueryParameter("timeframe", timeframe)
                .addQueryParameter("limit", String.valueOf(1000))
                .addQueryParameter("adjustment", "raw")
                .addQueryParameter("feed", "iex")
                .addQueryParameter("sort", "asc")
                .addQueryParameter("page_token", nextPageToken).build();

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
            return end.minusDays(30);
        case "4H":
            return end.minusDays(360); 
        case "1D":
            return end.minusDays(720);
        default:
            return end.minusDays(7);
        }
    }
    
    private static class StockDataResult {
        String jsonResponse;
        ZonedDateTime start;
        ZonedDateTime end;

        public StockDataResult(String jsonResponse, ZonedDateTime start, ZonedDateTime end) {
            this.jsonResponse = jsonResponse;
            this.start = start;
            this.end = end;
        }
    }
    
    private double fetchLatestPrice(String symbol) throws IOException, JSONException {
        if ("Yahoo".equals(currentDataSource)) {
            return fetchYahooLatestPrice(symbol);
        } else {
            return fetchAlpacaLatestPrice(symbol);
        }
    }
    
    private double fetchYahooLatestPrice(String symbol) throws IOException, JSONException {
        HttpUrl url = HttpUrl.parse("https://query1.finance.yahoo.com/v8/finance/chart/" + symbol)
                .newBuilder()
                .addQueryParameter("interval", "1d")
                .addQueryParameter("range", "1d")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .build();

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
            return meta.getDouble("regularMarketPrice");
        }
    }

    private double fetchAlpacaLatestPrice(String symbol) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url("https://data.alpaca.markets/v2/stocks/snapshots?symbols=" + symbol)
                .get()
                .addHeader("accept", "application/json")
                .addHeader("APCA-API-KEY-ID", apiKey)
                .addHeader("APCA-API-SECRET-KEY", apiSecret)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            JSONObject snapshot = json.getJSONObject(symbol);
            JSONObject latestTrade = snapshot.getJSONObject("latestTrade");
            return latestTrade.getDouble("p");
        }
    }

    private AnalysisResult performTechnicalAnalysis(BarSeries series) {
        AnalysisResult result = new AnalysisResult();
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        int endIndex = series.getEndIndex();
        Bar currentBar = series.getBar(endIndex);

        Num zero01 = series.numOf(0.01);
        Num zero05 = series.numOf(0.05);
        Num currentClose = currentBar.getClosePrice();

        result.price = currentClose.doubleValue();

        if (selectedIndicators.contains("Trend")) {
            SMAIndicator sma50 = new SMAIndicator(closePrice, 50);
            SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
            result.sma50 = sma50.getValue(endIndex).doubleValue();
            result.sma200 = sma200.getValue(endIndex).doubleValue();
        }

        if (selectedIndicators.contains("MACD")) {
            MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
            EMAIndicator macdSignal = new EMAIndicator(macd, 9);
            result.macd = macd.getValue(endIndex).doubleValue();
            result.macdSignal = macdSignal.getValue(endIndex).doubleValue();

            if (result.macd > result.macdSignal
                    && macd.getValue(endIndex - 1).doubleValue() <= macdSignal.getValue(endIndex - 1).doubleValue()) {
                result.macdStatus = "Bullish Crossover";
            } else if (result.macd < result.macdSignal
                    && macd.getValue(endIndex - 1).doubleValue() >= macdSignal.getValue(endIndex - 1).doubleValue()) {
                result.macdStatus = "Bearish Crossover";
            } else {
                result.macdStatus = "Neutral";
            }
        }

        if (selectedIndicators.contains("MACD(5,8,9)")) {
            MACDIndicator macd359 = new MACDIndicator(closePrice, 5, 8);
            EMAIndicator macdSignal359 = new EMAIndicator(macd359, 9);
            result.macd359 = macd359.getValue(endIndex).doubleValue();
            result.macdSignal359 = macdSignal359.getValue(endIndex).doubleValue();

            if (result.macd359 > result.macdSignal359
                    && macd359.getValue(endIndex - 1).doubleValue() <= macdSignal359.getValue(endIndex - 1).doubleValue()) {
                result.macd359Status = "Bullish Crossover";
            } else if (result.macd359 < result.macdSignal359
                    && macd359.getValue(endIndex - 1).doubleValue() >= macdSignal359.getValue(endIndex - 1).doubleValue()) {
                result.macd359Status = "Bearish Crossover";
            } else {
                result.macd359Status = "Neutral";
            }
        }

        if (selectedIndicators.contains("RSI")) {
            RSIIndicator rsi = new RSIIndicator(closePrice, 14);
            result.rsi = rsi.getValue(endIndex).doubleValue();
        }

        if (selectedIndicators.contains("PSAR(0.01)") || selectedIndicators.contains("PSAR(0.05)")) {
            try {
                if (selectedIndicators.contains("PSAR(0.01)")) {
                    ParabolicSarIndicator psar001 = new ParabolicSarIndicator(series, zero01, zero01, zero01);
                    Num psar001Value = psar001.getValue(endIndex);
                    result.psar001 = psar001Value.doubleValue();
                    result.psar001Trend = psar001Value.isLessThan(currentClose) ? "↑ Uptrend" : "↓ Downtrend";
                }

                if (selectedIndicators.contains("PSAR(0.05)")) {
                    ParabolicSarIndicator psar005 = new ParabolicSarIndicator(series, zero05, zero05, zero05);
                    Num psar005Value = psar005.getValue(endIndex);
                    result.psar005 = psar005Value.doubleValue();
                    result.psar005Trend = psar005Value.isLessThan(currentClose) ? "↑ Uptrend" : "↓ Downtrend";
                }

                if (Double.isInfinite(result.psar001) || Double.isNaN(result.psar001)) {
                    result.psar001 = result.price;
                    result.psar001Trend = "N/A";
                }
                if (Double.isInfinite(result.psar005) || Double.isNaN(result.psar005)) {
                    result.psar005 = result.price;
                    result.psar005Trend = "N/A";
                }
            } catch (Exception e) {
                logToConsole("Error calculating PSAR: " + e.getMessage());
                result.psar001 = result.price;
                result.psar005 = result.price;
                result.psar001Trend = "Error";
                result.psar005Trend = "Error";
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

    private String getMacdStatus(AnalysisResult result) {
        if (result.macd > result.macdSignal) {
            return "Bullish";
        } else {
            return "Bearish";
        }
    }

    private String getActionRecommendation(AnalysisResult result) {
        boolean bullishTrend = result.price > result.sma50;
        boolean oversold = result.rsi < 30;
        boolean overbought = result.rsi > 70;
        boolean macdBullish = result.macd > result.macdSignal && result.macdStatus.contains("Bullish");
        boolean macdBearish = result.macd < result.macdSignal && result.macdStatus.contains("Bearish");

        if (bullishTrend && oversold && macdBullish)
            return "Strong Buy";
        if (!bullishTrend && overbought && macdBearish)
            return "Strong Sell";
        if (bullishTrend && macdBullish)
            return "Buy";
        if (!bullishTrend && macdBearish)
            return "Sell";
        return "Neutral";
    }

    private static class AnalysisResult {
        double price;
        double sma50;
        double sma200;
        double macd;
        double macdSignal;
        String macdStatus;
        double macd359;
        double macdSignal359;
        String macd359Status;
        double rsi;
        double psar001;
        double psar005;
        String psar001Trend;
        String psar005Trend;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
        	EnhancedStockDashboardV9 dashboard = new EnhancedStockDashboardV9();
            dashboard.setVisible(true);
        });
    }
}