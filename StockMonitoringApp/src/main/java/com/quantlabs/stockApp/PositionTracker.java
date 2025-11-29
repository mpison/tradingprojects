package com.quantlabs.stockApp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.Duration;
import java.time.ZonedDateTime;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.quantlabs.stockApp.core.indicators.HeikenAshiIndicator;
import com.quantlabs.stockApp.core.indicators.resistance.HighestCloseOpenIndicator;
import com.quantlabs.stockApp.core.indicators.resistance.HighestCloseOpenIndicator.HighestValue;
import com.quantlabs.stockApp.core.indicators.targetvalue.MovingAverageTargetValue;
import com.quantlabs.stockApp.data.StockDataProvider;
import com.quantlabs.stockApp.data.StockDataProviderFactory;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.model.StockDataResult;
import com.quantlabs.stockApp.utils.PriceDataMapper;

import okhttp3.OkHttpClient;

public class PositionTracker extends JFrame {
    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey = "PK4UOZQDJJZ6WBAU52XM";
    private final String apiSecret = "Fag4ha2D58VyL0okwXgBHD1IvhoptmI2KiacMaNG";
    private final String polygonApiKey = "fMCQBX5yvQZL_s6Zi1r9iKBkMkNLzNcw";

    private static final Map<String, Integer> ALPACA_TIME_LIMITS = Map.of("1Min", 4, "5Min", 7, "15Min", 7, "30Min", 30,
            "1H", 30, "4H", 30, "1D", 60);
    private static final Map<String, Integer> YAHOO_TIME_LIMITS = Map.of("1Min", 7, "5Min", 60, "15Min", 60, "30Min",
            60, "1H", 60, "4H", 60, "1D", 60);
    private static final Map<String, Integer> POLYGON_TIME_LIMITS = Map.of("1Min", 7, "5Min", 30, "15Min", 30, "30Min",
            60, "1H", 60, "4H", 90, "1D", 365, "1W", 730);

    private final String[] dataSources = { "Alpaca", "Yahoo", "Polygon" };
    private final String[] allTimeframes = { "1W", "1D", "1H", "30Min", "15Min", "5Min", "1Min" };
    private final String[] allIndicators = { "RSI", "MACD", "MovingAverageTargetValue", "HighestCloseOpen", "HeikenAshi", "MACD(5,8,9)", "PSAR(0.01,0.01)", "PSAR(0.05,0.05)" };

    private static final Set<String> BULLISH_STATUSES = Set.of("Strong Bullish", "Bullish", "Strong Uptrend", "Mild Uptrend", "Buy", "Uptrend");
    private static final Color BULLISH_COLOR = new Color(200, 255, 200);
    private static final Color BEARISH_COLOR = new Color(255, 200, 200);

    private Set<String> selectedTimeframes = new HashSet<>(Arrays.asList("1W", "1D", "1H", "30Min", "15Min", "5Min", "1Min"));
    private final Integer[] refreshIntervals = { 10, 30, 60, 120, 300, 600, 900 };
    private Map<String, Set<String>> selectedIndicatorsPerTimeframe = new HashMap<>();

    private JTable dashboardTable;
    private FilterableTableModel tableModel;
    private JButton refreshButton;
    private JButton startLiveButton;
    private JButton stopLiveButton;
    private JButton customIndicatorButton;
    private JComboBox<String> dataSourceComboBox;
    private JComboBox<Integer> refreshIntervalComboBox;
    private ScheduledExecutorService scheduler;
    private boolean isLive = false;
    private final Map<String, Color> statusColors = new HashMap<>();
    private String currentDataSource = "Yahoo";
    private int currentRefreshInterval = 60;

    private JTextArea consoleArea;
    private JScrollPane consoleScrollPane;
    private JPanel consolePanel;
    private JPanel controlPanel;

    private StockDataProviderFactory dataProviderFactory;
    private StockDataProvider currentDataProvider;
    private Map<String, PriceData> priceDataMap = new ConcurrentHashMap<>();
    private Map<String, BarSeries> barSeriesMap = new ConcurrentHashMap<>();

    private List<Position> positions = new ArrayList<>();
    private JButton addPositionButton;

    private class Position {
        String symbol;
        double quantity;
        double avgCost;

        public Position(String symbol, double quantity, double avgCost) {
            this.symbol = symbol;
            this.quantity = quantity;
            this.avgCost = avgCost;
        }
    }

    private class FilterableTableModel extends DefaultTableModel {
        private final Set<String> hiddenSymbols;
        private final Map<String, Integer> symbolToRowMap;
        private Map<String, double[]> columnFilters = new HashMap<>();
        private Vector<String> columnIdentifiers;

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
            if (colName == null) {
                return String.class;
            }
            if (colName.equals("Select") || colName.equals("Checkbox")) {
                return Boolean.class;
            } else if (colName.equals("Symbol")) {
                return String.class;
            } else if (colName.equals("Quantity") || colName.equals("Avg Cost") || colName.equals("Total Cost")
                    || colName.equals("Current Price") || colName.equals("Current Value") || colName.equals("$ G/L")
                    || colName.equals("% G/L")) {
                return Double.class;
            }
            return String.class;
        }

        @Override
        public synchronized void setValueAt(Object value, int row, int column) {
            try {
                int modelRow = convertViewRowToModel(row);
                if (modelRow != -1) {
                    super.setValueAt(value, modelRow, column);
                }
            } catch (Exception e) {
                logToConsole("Error in setValueAt: " + e.getMessage());
            }
        }

        @Override
        public String getColumnName(int column) {
            return columnIdentifiers.get(column);
        }

        @Override
        public int getColumnCount() {
            return columnIdentifiers.size();
        }

        public void removeColumn(int column) {
            try {
                columnIdentifiers.remove(column);
                for (Object rowObj : dataVector) {
                    if (rowObj instanceof Vector) {
                        ((Vector<?>) rowObj).remove(column);
                    }
                }
                fireTableStructureChanged();
                if (column == 1) {
                    rebuildSymbolMap();
                }
                if (dashboardTable != null && dashboardTable.getRowSorter() instanceof TableRowSorter) {
                    TableRowSorter<FilterableTableModel> sorter = (TableRowSorter<FilterableTableModel>) dashboardTable.getRowSorter();
                    sorter.setModel(this);
                    sorter.modelStructureChanged();
                }
                SwingUtilities.invokeLater(() -> updateTableRenderers());
            } catch (ArrayIndexOutOfBoundsException e) {
                logToConsole("Error removing column: " + e.getMessage());
            }
        }

        public Vector getColumnIdentifiers() {
            return this.columnIdentifiers;
        }

        public boolean rowSatisfiesFilters(int modelRow) {
            for (Map.Entry<String, double[]> entry : columnFilters.entrySet()) {
                String colName = entry.getKey();
                int col = findColumn(colName);
                if (col == -1) continue;

                Object val = super.getValueAt(modelRow, col);
                if (val == null) return false;

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

    public PositionTracker() {
        // Initialize selectedIndicatorsPerTimeframe with all indicators for each timeframe
        for (String timeframe : allTimeframes) {
            selectedIndicatorsPerTimeframe.put(timeframe, new HashSet<>(Arrays.asList(allIndicators)));
        }

        this.dataProviderFactory = new StockDataProviderFactory(client, apiKey, apiSecret, polygonApiKey);
        this.currentDataProvider = dataProviderFactory.getProvider(currentDataSource, this::logToConsole);

        initializeStatusColors();
        initializePositions();
        setupUI();
        refreshData();
    }

    private void initializePositions() {
        positions = new ArrayList<>();
    }

    private void initializeStatusColors() {
        statusColors.clear();
        statusColors.put("Strong Uptrend", new Color(100, 255, 100));
        statusColors.put("Uptrend", new Color(100, 255, 100));
        statusColors.put("Mild Uptrend", new Color(180, 255, 180));
        statusColors.put("Strong Downtrend", new Color(255, 100, 100));
        statusColors.put("Downtrend", new Color(255, 100, 100));
        statusColors.put("Mild Downtrend", new Color(255, 180, 180));
        statusColors.put("Bullish", new Color(200, 255, 200));
        statusColors.put("Bearish", new Color(255, 200, 200));
        statusColors.put("Buy", new Color(120, 255, 120));
        statusColors.put("Sell", new Color(255, 120, 120));
        statusColors.put("Neutral", new Color(240, 240, 240));
    }

    private void setupUI() {
        setTitle("Position Tracker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        String[] baseColumns = {"Select", "Symbol", "Quantity", "Avg Cost", "Total Cost", "Current Price", "Current Value", "$ G/L", "% G/L"};
        List<String> columnNames = new ArrayList<>(Arrays.asList(baseColumns));

        for (String timeframe : allTimeframes) {
            if (selectedTimeframes.contains(timeframe)) {
                Set<String> indicators = selectedIndicatorsPerTimeframe.getOrDefault(timeframe, new HashSet<>());
                for (String indicator : indicators) {
                    columnNames.add(timeframe + " " + indicator);
                }
            }
        }

        tableModel = new FilterableTableModel(columnNames.toArray(new String[0]), 0);
        dashboardTable = new JTable(tableModel);
        dashboardTable.setAutoCreateRowSorter(true);
        dashboardTable.setRowHeight(25);
        dashboardTable.setGridColor(Color.LIGHT_GRAY);

        updateTableRenderers();

        controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addPositionButton = new JButton("Add Position");
        addPositionButton.addActionListener(e -> addPosition());
        controlPanel.add(addPositionButton);

        customIndicatorButton = new JButton("Custom Indicator");
        customIndicatorButton.addActionListener(e -> openCustomIndicatorWindow());
        controlPanel.add(customIndicatorButton);

        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshData());
        controlPanel.add(refreshButton);

        startLiveButton = new JButton("Start Live Updates");
        stopLiveButton = new JButton("Stop Live Updates");
        stopLiveButton.setEnabled(false);
        refreshIntervalComboBox = new JComboBox<>(refreshIntervals);
        refreshIntervalComboBox.setSelectedItem(currentRefreshInterval);
        controlPanel.add(new JLabel("Refresh Interval (s):"));
        controlPanel.add(refreshIntervalComboBox);
        controlPanel.add(startLiveButton);
        controlPanel.add(stopLiveButton);

        dataSourceComboBox = new JComboBox<>(dataSources);
        dataSourceComboBox.setSelectedItem(currentDataSource);
        controlPanel.add(new JLabel("Data Source:"));
        controlPanel.add(dataSourceComboBox);

        consoleArea = new JTextArea(5, 50);
        consoleArea.setEditable(false);
        consoleScrollPane = new JScrollPane(consoleArea);
        consolePanel = new JPanel(new BorderLayout());
        consolePanel.add(consoleScrollPane, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(dashboardTable), BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
        add(consolePanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(1200, 800));
        pack();
        setLocationRelativeTo(null);

        startLiveButton.addActionListener(e -> startLiveUpdates());
        stopLiveButton.addActionListener(e -> stopLiveUpdates());
        dataSourceComboBox.addActionListener(e -> {
            currentDataSource = (String) dataSourceComboBox.getSelectedItem();
            currentDataProvider = dataProviderFactory.getProvider(currentDataSource, this::logToConsole);
            refreshData();
        });
    }

    private void openCustomIndicatorWindow() {
        JDialog dialog = new JDialog(this, "Customize Indicators", true);
        dialog.setLayout(new BorderLayout());
        dialog.setPreferredSize(new Dimension(600, 400));

        // Create panel for checkboxes
        JPanel checkboxPanel = new JPanel(new GridLayout(allTimeframes.length + 1, allIndicators.length + 1));
        checkboxPanel.add(new JLabel("")); // Top-left corner
        for (String indicator : allIndicators) {
            checkboxPanel.add(new JLabel(indicator));
        }

        Map<String, Map<String, JCheckBox>> checkboxMap = new HashMap<>();
        for (String timeframe : allTimeframes) {
            checkboxPanel.add(new JLabel(timeframe));
            Map<String, JCheckBox> indicatorCheckboxes = new HashMap<>();
            for (String indicator : allIndicators) {
                JCheckBox checkbox = new JCheckBox();
                checkbox.setSelected(selectedIndicatorsPerTimeframe.getOrDefault(timeframe, new HashSet<>()).contains(indicator));
                checkboxPanel.add(checkbox);
                indicatorCheckboxes.put(indicator, checkbox);
            }
            checkboxMap.put(timeframe, indicatorCheckboxes);
        }

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            // Update selectedIndicatorsPerTimeframe
            selectedIndicatorsPerTimeframe.clear();
            for (String timeframe : allTimeframes) {
                Set<String> selectedIndicators = new HashSet<>();
                for (String indicator : allIndicators) {
                    if (checkboxMap.get(timeframe).get(indicator).isSelected()) {
                        selectedIndicators.add(indicator);
                    }
                }
                selectedIndicatorsPerTimeframe.put(timeframe, selectedIndicators);
            }
            // Rebuild table with new columns
            rebuildTable();
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        dialog.add(checkboxPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void rebuildTable() {
        // Rebuild column names based on selected indicators
        String[] baseColumns = {"Select", "Symbol", "Quantity", "Avg Cost", "Total Cost", "Current Price", "Current Value", "$ G/L", "% G/L"};
        List<String> columnNames = new ArrayList<>(Arrays.asList(baseColumns));

        for (String timeframe : allTimeframes) {
            if (selectedTimeframes.contains(timeframe)) {
                Set<String> indicators = selectedIndicatorsPerTimeframe.getOrDefault(timeframe, new HashSet<>());
                for (String indicator : indicators) {
                    columnNames.add(timeframe + " " + indicator);
                }
            }
        }

        // Create new table model
        tableModel = new FilterableTableModel(columnNames.toArray(new String[0]), 0);
        dashboardTable.setModel(tableModel);
        dashboardTable.setAutoCreateRowSorter(true);
        updateTableRenderers();
        refreshData();
    }

    private void updateTableRenderers() {
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            String colName = tableModel.getColumnName(i);
            if (colName.equals("Select") || colName.equals("Checkbox")) {
                TableColumn column = dashboardTable.getColumnModel().getColumn(i);
                column.setCellEditor(new DefaultCellEditor(new JCheckBox()));
            } else if (colName.equals("Quantity") || colName.equals("Avg Cost") || colName.equals("Total Cost")
                    || colName.equals("Current Price") || colName.equals("Current Value") || colName.equals("$ G/L")
                    || colName.equals("% G/L")) {
                dashboardTable.getColumnModel().getColumn(i).setCellRenderer(new DoubleCellRenderer());
            } else {
                dashboardTable.getColumnModel().getColumn(i).setCellRenderer(new StatusCellRenderer());
            }
        }
    }

    private void addPosition() {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        JTextField symField = new JTextField();
        JTextField qtyField = new JTextField();
        JTextField avgField = new JTextField();
        panel.add(new JLabel("Symbol:"));
        panel.add(symField);
        panel.add(new JLabel("Quantity:"));
        panel.add(qtyField);
        panel.add(new JLabel("Avg Cost:"));
        panel.add(avgField);
        int result = JOptionPane.showConfirmDialog(this, panel, "Add Position", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String symbol = symField.getText().trim().toUpperCase();
                double qty = Double.parseDouble(qtyField.getText());
                double avg = Double.parseDouble(avgField.getText());
                positions.add(new Position(symbol, qty, avg));
                refreshData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input");
                logToConsole("Error adding position: " + ex.getMessage());
            }
        }
    }

    private void refreshData() {
        tableModel.setRowCount(0);

        List<String> symbols = positions.stream().map(p -> p.symbol).distinct().collect(Collectors.toList());
        if (symbols.isEmpty()) {
            logToConsole("No positions to display");
            return;
        }

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    for (String symbol : symbols) {
                        // Mock StockDataResult for price and bar series
                        String mockJson = "{}";
                        ZonedDateTime end = ZonedDateTime.now();
                        ZonedDateTime start = end.minusDays(1);
                        StockDataResult stockData = new StockDataResult(mockJson, start, end);
                        PriceData priceData = PriceDataMapper.mapToPriceData(stockData);
                        priceDataMap.put(symbol, priceData);

                        // Mock BarSeries for each timeframe
                        for (String timeframe : selectedTimeframes) {
                            BarSeries series = new BaseBarSeriesBuilder().withName(symbol + "_" + timeframe).build();
                            series.addBar(Duration.ofMinutes(1), end, priceData.getLatestPrice(),
                                         priceData.getLatestPrice() + 10,
                                         priceData.getLatestPrice() - 10,
                                         priceData.getLatestPrice(),
                                         priceData.getCurrentVolume() != null ? priceData.getCurrentVolume() : 1000);
                            barSeriesMap.put(symbol + "_" + timeframe, series);
                        }
                    }
                } catch (Exception e) {
                    logToConsole("Error fetching data: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                for (Position pos : positions) {
                    PriceData priceData = priceDataMap.get(pos.symbol);
                    if (priceData == null) {
                        logToConsole("No price data for " + pos.symbol);
                        continue;
                    }

                    double currentPrice = priceData.getLatestPrice();
                    double totalCost = pos.quantity * pos.avgCost;
                    double currentValue = pos.quantity * currentPrice;
                    double glDollar = currentValue - totalCost;
                    double glPercent = (totalCost != 0) ? (glDollar / totalCost) * 100 : 0;

                    Object[] row = new Object[tableModel.getColumnCount()];
                    row[0] = false;
                    row[1] = pos.symbol;
                    row[2] = pos.quantity;
                    row[3] = pos.avgCost;
                    row[4] = totalCost;
                    row[5] = currentPrice;
                    row[6] = currentValue;
                    row[7] = glDollar;
                    row[8] = glPercent;

                    try {
                        Map<String, Map<String, Object>> indicators = calculateIndicators(pos.symbol);
                        int colIndex = 9;
                        for (String timeframe : allTimeframes) {
                            if (selectedTimeframes.contains(timeframe)) {
                                Set<String> indicatorsForTimeframe = selectedIndicatorsPerTimeframe.getOrDefault(timeframe, new HashSet<>());
                                for (String indicator : indicatorsForTimeframe) {
                                    Object value = indicators.getOrDefault(timeframe, new HashMap<>()).get(indicator);
                                    row[colIndex++] = value != null ? value : "N/A";
                                }
                            }
                        }
                    } catch (Exception e) {
                        logToConsole("Error calculating indicators for " + pos.symbol + ": " + e.getMessage());
                    }

                    tableModel.addRow(row);
                }
                updateTableRenderers();
            }
        }.execute();
    }

    private Map<String, Map<String, Object>> calculateIndicators(String symbol) {
        Map<String, Map<String, Object>> indicators = new HashMap<>();
        for (String timeframe : selectedTimeframes) {
            Map<String, Object> timeframeIndicators = new HashMap<>();
            try {
                BarSeries series = barSeriesMap.get(symbol + "_" + timeframe);
                if (series == null || series.getBarCount() == 0) {
                    logToConsole("No bar series data for " + symbol + " (" + timeframe + ")");
                    continue;
                }

                ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

                Set<String> indicatorsForTimeframe = selectedIndicatorsPerTimeframe.getOrDefault(timeframe, new HashSet<>());
                for (String indicator : indicatorsForTimeframe) {
                    switch (indicator) {
                        case "RSI":
                            timeframeIndicators.put(indicator, new RSIIndicator(closePrice, 14).getValue(series.getBarCount() - 1).doubleValue());
                            break;
                        case "MACD":
                            timeframeIndicators.put(indicator, new MACDIndicator(closePrice, 12, 26).getValue(series.getBarCount() - 1).doubleValue());
                            break;
                        case "MovingAverageTargetValue":
                            timeframeIndicators.put(indicator, new MovingAverageTargetValue(series, "UpTrend").getValue(series.getBarCount() - 1).doubleValue());
                            break;
                        case "HighestCloseOpen":
                            HighestCloseOpenIndicator hcoi = new HighestCloseOpenIndicator(series, "1D", true, "Yahoo");
                            HighestValue hv = hcoi.getValue(series.getBarCount() - 1);
                            timeframeIndicators.put(indicator, hv != null && hv.value != null ? hv.value.doubleValue() : "N/A");
                            break;
                        case "HeikenAshi":
                            timeframeIndicators.put(indicator, new HeikenAshiIndicator(series).getValue(series.getBarCount() - 1).doubleValue());
                            break;
                        case "MACD(5,8,9)":
                            timeframeIndicators.put(indicator, new MACDIndicator(closePrice, 5, 8).getValue(series.getBarCount() - 1).doubleValue());
                            break;
                        case "PSAR(0.01,0.01)":
                            timeframeIndicators.put(indicator, new ParabolicSarIndicator(series, series.numOf(0.01), series.numOf(0.01)).getValue(series.getBarCount() - 1).doubleValue());
                            break;
                        case "PSAR(0.05,0.05)":
                            timeframeIndicators.put(indicator, new ParabolicSarIndicator(series, series.numOf(0.05), series.numOf(0.05)).getValue(series.getBarCount() - 1).doubleValue());
                            break;
                    }
                }
                indicators.put(timeframe, timeframeIndicators);
            } catch (Exception e) {
                logToConsole("Error calculating indicators for " + symbol + " (" + timeframe + "): " + e.getMessage());
            }
        }
        return indicators;
    }

    private void startLiveUpdates() {
        int interval = (Integer) refreshIntervalComboBox.getSelectedItem();
        startLiveButton.setEnabled(false);
        stopLiveButton.setEnabled(true);
        refreshIntervalComboBox.setEnabled(false);
        isLive = true;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::refreshData, 0, interval, TimeUnit.SECONDS);
    }

    private void stopLiveUpdates() {
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
        startLiveButton.setEnabled(true);
        stopLiveButton.setEnabled(false);
        refreshIntervalComboBox.setEnabled(true);
        isLive = false;
    }

    private void logToConsole(String message) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(message + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
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
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
            }
            return c;
        }
    }

    private class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (column == 0 || value instanceof Number) {
                return c;
            }

            String status = value != null ? value.toString() : "";

            if (BULLISH_STATUSES.stream().anyMatch(status::contains)) {
                c.setBackground(BULLISH_COLOR);
                c.setForeground(Color.BLACK);
            } else if (status.contains("Downtrend") || status.contains("Sell") || status.contains("Bearish")) {
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PositionTracker().setVisible(true));
    }
}