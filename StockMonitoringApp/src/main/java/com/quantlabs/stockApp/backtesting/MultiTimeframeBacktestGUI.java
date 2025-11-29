package com.quantlabs.stockApp.backtesting;

import com.quantlabs.stockApp.backtesting.*;
import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.data.SimpleConsoleLogger;
import com.quantlabs.stockApp.data.StockDataProviderFactory;
import com.quantlabs.stockApp.model.PriceData;
import okhttp3.OkHttpClient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MultiTimeframeBacktestGUI extends JFrame {
    private final ConsoleLogger logger;
    private final StockDataProviderFactory providerFactory;
    private BacktestingController controller;
    private Map<String, Object> providerConfig;
    
    // UI Components
    private JComboBox<String> symbolComboBox;
    private JList<String> timeframeList;
    private JList<String> strategyList;
    private JTextField initialCapitalField;
    private JSpinner startDateSpinner;
    private JSpinner endDateSpinner;
    private JSpinner startTimeSpinner;
    private JSpinner endTimeSpinner;
    private JTextArea logArea;
    private JTable resultsTable;
    private DefaultTableModel resultsModel;
    private JTable metricsTable;
    private DefaultTableModel metricsModel;
    private JComboBox<String> dataProviderComboBox;
    private JCheckBox useLiveDataCheckbox;
    private JProgressBar progressBar;
    
    public MultiTimeframeBacktestGUI(StockDataProviderFactory providerFactory) {
        this.logger = new SimpleConsoleLogger();
        this.providerFactory = providerFactory;
        this.providerConfig = new HashMap<>();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        updateProviderConfig(); // Initialize config
    }
    
    private void initializeComponents() {
        setTitle("Multi-Timeframe Stock Backtester");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Symbol selection
        symbolComboBox = new JComboBox<>(new String[]{
            "AAPL", "TSLA", "MSFT", "GOOGL", "AMZN", "SPY", "QQQ", "NVDA", "META", "NFLX"
        });
        
        // Timeframe selection
        String[] timeframes = {"1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min"};
        timeframeList = new JList<>(timeframes);
        timeframeList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        timeframeList.setSelectedIndices(new int[]{0, 1, 2, 3});
        
        // Strategy selection
        String[] strategies = {
            "RSI", "MACD", "MACD_359", "PSAR_001", "PSAR_005", 
            "HEIKEN_ASHI", "VWAP", "VOLUME_MA", "MOVING_AVERAGE"
        };
        strategyList = new JList<>(strategies);
        strategyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        strategyList.setSelectedIndices(new int[]{0, 1, 2, 3});
        
        // Capital field
        initialCapitalField = new JTextField("10000", 10);
        
        // Date pickers with spinners
        startDateSpinner = createDateSpinner(LocalDate.now().minusMonths(6));
        endDateSpinner = createDateSpinner(LocalDate.now());
        
        // Time spinners
        startTimeSpinner = createTimeSpinner(LocalTime.of(9, 30)); // Market open
        endTimeSpinner = createTimeSpinner(LocalTime.of(16, 0));   // Market close
        
        // Data provider selection
        dataProviderComboBox = new JComboBox<>(new String[]{"Alpaca", "Yahoo", "Polygon"});
        useLiveDataCheckbox = new JCheckBox("Use Live Data", true);
        
        // Log area
        logArea = new JTextArea(15, 80);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // Results table
        resultsModel = new DefaultTableModel(new Object[]{
            "Entry Time", "Exit Time", "Entry Price", "Exit Price", "Shares", "PnL", "Return %", "Holding Period"
        }, 0);
        resultsTable = new JTable(resultsModel);
        resultsTable.setAutoCreateRowSorter(true);
        
        // Metrics table
        metricsModel = new DefaultTableModel(new Object[]{"Metric", "Value"}, 0);
        metricsTable = new JTable(metricsModel);
        
        // Progress bar
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
    }
    
    private JSpinner createDateSpinner(LocalDate initialDate) {
        SpinnerDateModel model = new SpinnerDateModel();
        model.setValue(java.sql.Date.valueOf(initialDate));
        
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd");
        spinner.setEditor(editor);
        spinner.setToolTipText("Select date");
        
        return spinner;
    }
    
    private JSpinner createTimeSpinner(LocalTime initialTime) {
        SpinnerDateModel model = new SpinnerDateModel();
        // Combine with today's date to create a full datetime
        LocalDateTime dateTime = LocalDateTime.of(LocalDate.now(), initialTime);
        model.setValue(java.util.Date.from(dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant()));
        
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "HH:mm");
        spinner.setEditor(editor);
        spinner.setToolTipText("Select time");
        
        return spinner;
    }
    
    private LocalDateTime getStartDateTime() {
        java.util.Date startDate = (java.util.Date) startDateSpinner.getValue();
        java.util.Date startTime = (java.util.Date) startTimeSpinner.getValue();
        
        LocalDate date = startDate.toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate();
        LocalTime time = startTime.toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime();
            
        return LocalDateTime.of(date, time);
    }
    
    private LocalDateTime getEndDateTime() {
        java.util.Date endDate = (java.util.Date) endDateSpinner.getValue();
        java.util.Date endTime = (java.util.Date) endTimeSpinner.getValue();
        
        LocalDate date = endDate.toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate();
        LocalTime time = endTime.toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime();
            
        return LocalDateTime.of(date, time);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Main split pane
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setDividerLocation(0.4);
        
        // Top panel - Configuration
        JPanel configPanel = createConfigPanel();
        
        // Bottom panel - Results
        JSplitPane resultsSplitPane = createResultsSplitPane();
        
        mainSplitPane.setTopComponent(configPanel);
        mainSplitPane.setBottomComponent(resultsSplitPane);
        
        add(mainSplitPane, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
    
    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Backtest Configuration"));
        
        // Left panel - Basic settings
        JPanel leftPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);
        
        int row = 0;
        
        // Symbol
        gbc.gridx = 0; gbc.gridy = row;
        leftPanel.add(new JLabel("Symbol:"), gbc);
        gbc.gridx = 1;
        leftPanel.add(symbolComboBox, gbc);
        
        // Data Provider
        gbc.gridx = 0; gbc.gridy = ++row;
        leftPanel.add(new JLabel("Data Provider:"), gbc);
        gbc.gridx = 1;
        leftPanel.add(dataProviderComboBox, gbc);
        
        // Initial Capital
        gbc.gridx = 0; gbc.gridy = ++row;
        leftPanel.add(new JLabel("Initial Capital:"), gbc);
        gbc.gridx = 1;
        leftPanel.add(initialCapitalField, gbc);
        
        // Start Date
        gbc.gridx = 0; gbc.gridy = ++row;
        leftPanel.add(new JLabel("Start Date:"), gbc);
        gbc.gridx = 1;
        leftPanel.add(createDatePanel(true), gbc);
        
        // End Date
        gbc.gridx = 0; gbc.gridy = ++row;
        leftPanel.add(new JLabel("End Date:"), gbc);
        gbc.gridx = 1;
        leftPanel.add(createDatePanel(false), gbc);
        
        // Live Data Checkbox
        gbc.gridx = 1; gbc.gridy = ++row;
        leftPanel.add(useLiveDataCheckbox, gbc);
        
        // Quick date buttons
        gbc.gridx = 0; gbc.gridy = ++row;
        gbc.gridwidth = 2;
        leftPanel.add(createQuickDateButtons(), gbc);
        
        // Center panel - Timeframes
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Timeframes"));
        centerPanel.add(new JScrollPane(timeframeList), BorderLayout.CENTER);
        
        // Right panel - Strategies
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Strategies"));
        rightPanel.add(new JScrollPane(strategyList), BorderLayout.CENTER);
        
        // Combine panels
        JPanel configContent = new JPanel(new GridLayout(1, 3, 10, 0));
        configContent.add(leftPanel);
        configContent.add(centerPanel);
        configContent.add(rightPanel);
        
        panel.add(configContent, BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createDatePanel(boolean isStart) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        
        if (isStart) {
            panel.add(startDateSpinner, BorderLayout.CENTER);
            panel.add(startTimeSpinner, BorderLayout.EAST);
        } else {
            panel.add(endDateSpinner, BorderLayout.CENTER);
            panel.add(endTimeSpinner, BorderLayout.EAST);
        }
        
        return panel;
    }
    
    private JPanel createQuickDateButtons() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Quick Date Ranges"));
        
        JButton lastWeekBtn = new JButton("Last Week");
        JButton lastMonthBtn = new JButton("Last Month");
        JButton last3MonthsBtn = new JButton("Last 3 Months");
        JButton last6MonthsBtn = new JButton("Last 6 Months");
        JButton lastYearBtn = new JButton("Last Year");
        
        lastWeekBtn.addActionListener(e -> setQuickDateRange(7));
        lastMonthBtn.addActionListener(e -> setQuickDateRange(30));
        last3MonthsBtn.addActionListener(e -> setQuickDateRange(90));
        last6MonthsBtn.addActionListener(e -> setQuickDateRange(180));
        lastYearBtn.addActionListener(e -> setQuickDateRange(365));
        
        panel.add(lastWeekBtn);
        panel.add(lastMonthBtn);
        panel.add(last3MonthsBtn);
        panel.add(last6MonthsBtn);
        panel.add(lastYearBtn);
        
        return panel;
    }
    
    private void setQuickDateRange(int daysBack) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(daysBack);
        
        // Set dates
        startDateSpinner.setValue(java.sql.Date.valueOf(startDate));
        endDateSpinner.setValue(java.sql.Date.valueOf(endDate));
        
        // Set times to market hours
        startTimeSpinner.setValue(java.util.Date.from(
            LocalDateTime.of(startDate, LocalTime.of(9, 30))
                .atZone(java.time.ZoneId.systemDefault()).toInstant()));
        endTimeSpinner.setValue(java.util.Date.from(
            LocalDateTime.of(endDate, LocalTime.of(16, 0))
                .atZone(java.time.ZoneId.systemDefault()).toInstant()));
        
        log("Set date range to last " + daysBack + " days");
    }
    
    private JSplitPane createResultsSplitPane() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(0.3);
        
        // Top - Log area
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Execution Log"));
        
        // Bottom - Results tables
        JSplitPane tablesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        tablesSplitPane.setDividerLocation(0.7);
        
        JScrollPane tradesScrollPane = new JScrollPane(resultsTable);
        tradesScrollPane.setBorder(BorderFactory.createTitledBorder("Trade Results"));
        
        JScrollPane metricsScrollPane = new JScrollPane(metricsTable);
        metricsScrollPane.setBorder(BorderFactory.createTitledBorder("Performance Metrics"));
        
        tablesSplitPane.setLeftComponent(tradesScrollPane);
        tablesSplitPane.setRightComponent(metricsScrollPane);
        
        splitPane.setTopComponent(logScrollPane);
        splitPane.setBottomComponent(tablesSplitPane);
        
        return splitPane;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        
        JButton runButton = new JButton("Run Backtest");
        JButton optimizeButton = new JButton("Optimize Parameters");
        JButton exportButton = new JButton("Export Results");
        JButton clearButton = new JButton("Clear Results");
        JButton settingsButton = new JButton("Settings");
        
        runButton.addActionListener(this::runBacktest);
        optimizeButton.addActionListener(this::optimizeParameters);
        exportButton.addActionListener(this::exportResults);
        clearButton.addActionListener(this::clearResults);
        settingsButton.addActionListener(this::showSettings);
        
        panel.add(runButton);
        panel.add(optimizeButton);
        panel.add(exportButton);
        panel.add(clearButton);
        panel.add(settingsButton);
        panel.add(progressBar);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        dataProviderComboBox.addActionListener(e -> updateProviderConfig());
        useLiveDataCheckbox.addActionListener(e -> updateProviderConfig());
        
        // Add date validation
        startDateSpinner.addChangeListener(e -> validateDateRange());
        endDateSpinner.addChangeListener(e -> validateDateRange());
    }
    
    private void validateDateRange() {
        LocalDateTime start = getStartDateTime();
        LocalDateTime end = getEndDateTime();
        
        if (start.isAfter(end)) {
            log("Warning: Start date is after end date");
            startDateSpinner.setBackground(Color.PINK);
            endDateSpinner.setBackground(Color.PINK);
        } else {
            startDateSpinner.setBackground(Color.WHITE);
            endDateSpinner.setBackground(Color.WHITE);
        }
    }
    
    private void updateProviderConfig() {
        String provider = (String) dataProviderComboBox.getSelectedItem();
        providerConfig.clear();
        
        if ("Alpaca".equals(provider)) {
            providerConfig.put("adjustment", "all");
            providerConfig.put("feed", useLiveDataCheckbox.isSelected() ? "sip" : "iex");
        }
        // Add configurations for other providers if needed
    }
    
    private void runBacktest(ActionEvent e) {
        // Validate date range first
        LocalDateTime start = getStartDateTime();
        LocalDateTime end = getEndDateTime();
        
        if (start.isAfter(end)) {
            JOptionPane.showMessageDialog(this,
                "Start date cannot be after end date. Please adjust the date range.",
                "Invalid Date Range", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (start.isAfter(LocalDateTime.now())) {
            JOptionPane.showMessageDialog(this,
                "Start date cannot be in the future. Please adjust the date range.",
                "Invalid Date Range", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        SwingWorker<BacktestResult, String> worker = new SwingWorker<>() {
            @Override
            protected BacktestResult doInBackground() throws Exception {
                publish("Initializing backtest...");
                
                // Get configuration
                String symbol = (String) symbolComboBox.getSelectedItem();
                List<String> selectedTimeframes = timeframeList.getSelectedValuesList();
                List<String> selectedStrategies = strategyList.getSelectedValuesList();
                double initialCapital = Double.parseDouble(initialCapitalField.getText());
                LocalDateTime startDate = getStartDateTime();
                LocalDateTime endDate = getEndDateTime();
                String dataProvider = (String) dataProviderComboBox.getSelectedItem();
                
                // Validate inputs
                if (selectedTimeframes.isEmpty()) {
                    throw new Exception("Please select at least one timeframe");
                }
                if (selectedStrategies.isEmpty()) {
                    throw new Exception("Please select at least one strategy");
                }
                
                publish("Configuring backtest for " + symbol);
                publish("Date Range: " + startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + 
                       " to " + endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                publish("Timeframes: " + String.join(", ", selectedTimeframes));
                publish("Strategies: " + String.join(", ", selectedStrategies));
                
                // Initialize controller - FIXED: Use instance variable providerConfig
                controller = new BacktestingController(logger, providerFactory);
                controller.initializeBacktester(dataProvider, providerConfig);
                
                // Create backtest configuration
                BacktestConfig config = createBacktestConfig(
                    symbol, selectedTimeframes, selectedStrategies, 
                    initialCapital, startDate, endDate
                );
                
                publish("Running backtest with " + config.getStrategies().size() + " strategy combinations...");
                
                // Run backtest
                return controller.runMultiTimeframeBacktest(config);
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    log(message);
                }
            }
            
            @Override
            protected void done() {
                progressBar.setVisible(false);
                
                try {
                    BacktestResult result = get();
                    displayResults(result);
                    log("Backtest completed successfully!");
                    
                    // Show summary dialog
                    showSummaryDialog(result);
                    
                } catch (Exception ex) {
                    log("Error during backtest: " + ex.getMessage());
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(MultiTimeframeBacktestGUI.this,
                        "Backtest failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        worker.execute();
    }
    
    private BacktestConfig createBacktestConfig(String symbol, List<String> timeframes, 
                                              List<String> strategies, double initialCapital,
                                              LocalDateTime startDate, LocalDateTime endDate) {
        BacktestConfig.Builder builder = new BacktestConfig.Builder()
            .symbol(symbol)
            .timeframes(timeframes)
            .initialCapital(initialCapital)
            .dateRange(startDate, endDate);
        
        // Map strategy types to configurations
        for (String strategyType : strategies) {
            StrategyConfig strategyConfig = createStrategyConfig(strategyType);
            builder.strategy(strategyConfig);
        }
        
        return builder.build();
    }
    
    private StrategyConfig createStrategyConfig(String strategyType) {
        switch (strategyType) {
            case "RSI":
                return new StrategyConfig.Builder("RSI")
                    .timeframes(List.of("4H", "1H"))
                    .weight(1.0)
                    .build();
                    
            case "MACD":
                return new StrategyConfig.Builder("MACD")
                    .timeframes(List.of("4H", "1H", "30Min"))
                    .weight(1.2)
                    .build();
                    
            case "MACD_359":
                return new StrategyConfig.Builder("MACD_359")
                    .timeframes(List.of("1H", "30Min", "15Min"))
                    .weight(1.0)
                    .build();
                    
            case "PSAR_001":
                return new StrategyConfig.Builder("PSAR_001")
                    .timeframes(List.of("1H", "30Min"))
                    .weight(0.8)
                    .build();
                    
            case "PSAR_005":
                return new StrategyConfig.Builder("PSAR_005")
                    .timeframes(List.of("4H", "1H"))
                    .weight(0.9)
                    .build();
                    
            case "HEIKEN_ASHI":
                return new StrategyConfig.Builder("HEIKEN_ASHI")
                    .timeframes(List.of("4H", "1H"))
                    .weight(0.7)
                    .build();
                    
            case "VWAP":
                return new StrategyConfig.Builder("VWAP")
                    .timeframes(List.of("1H"))
                    .weight(0.6)
                    .build();
                    
            case "VOLUME_MA":
                return new StrategyConfig.Builder("VOLUME_MA")
                    .timeframes(List.of("1H", "30Min"))
                    .weight(0.5)
                    .build();
                    
            case "MOVING_AVERAGE":
                return new StrategyConfig.Builder("MOVING_AVERAGE")
                    .timeframes(List.of("4H", "1H"))
                    .weight(0.8)
                    .parameter("period", 20)
                    .parameter("type", "SMA")
                    .build();
                    
            default:
                throw new IllegalArgumentException("Unknown strategy type: " + strategyType);
        }
    }
    
    private void displayResults(BacktestResult result) {
        // Clear previous results
        resultsModel.setRowCount(0);
        metricsModel.setRowCount(0);
        
        // Add trades to table
        for (Trade trade : result.getTrades()) {
            double returnPct = ((trade.getExitPrice() - trade.getEntryPrice()) / trade.getEntryPrice()) * 100;
            long holdingHours = java.time.Duration.between(trade.getEntryTime(), trade.getExitTime()).toHours();
            String holdingPeriod = holdingHours < 24 ? 
                holdingHours + " hours" : (holdingHours / 24) + " days";
            
            resultsModel.addRow(new Object[]{
                trade.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                trade.getExitTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                String.format("%.2f", trade.getEntryPrice()),
                String.format("%.2f", trade.getExitPrice()),
                trade.getQuantity(),
                String.format("%.2f", trade.getPnl()),
                String.format("%.2f%%", returnPct),
                holdingPeriod
            });
        }
        
        // Add performance metrics
        Map<String, Double> metrics = result.getPerformanceMetrics();
        for (Map.Entry<String, Double> entry : metrics.entrySet()) {
            String value;
            if (entry.getKey().contains("%") || entry.getKey().contains("Rate")) {
                value = String.format("%.2f%%", entry.getValue());
            } else if (entry.getKey().contains("Ratio") || entry.getKey().contains("Factor")) {
                value = String.format("%.2f", entry.getValue());
            } else {
                value = String.format("$%.2f", entry.getValue());
            }
            metricsModel.addRow(new Object[]{entry.getKey(), value});
        }
        
        // Auto-resize columns
        for (int i = 0; i < resultsModel.getColumnCount(); i++) {
            resultsTable.getColumnModel().getColumn(i).setPreferredWidth(120);
        }
    }
    
    private void showSummaryDialog(BacktestResult result) {
        String summary = String.format(
            "<html><div style='width:300px;'>" +
            "<h3>Backtest Summary</h3>" +
            "<b>Total Return:</b> %.2f%%<br>" +
            "<b>Total PnL:</b> $%.2f<br>" +
            "<b>Win Rate:</b> %.2f%%<br>" +
            "<b>Total Trades:</b> %d<br>" +
            "<b>Sharpe Ratio:</b> %.2f<br>" +
            "<b>Max Drawdown:</b> %.2f%%<br>" +
            "<b>Profit Factor:</b> %.2f<br>" +
            "<b>Date Range:</b> %s to %s<br>" +
            "</div></html>",
            result.getTotalReturn(),
            result.getTotalPnL(),
            result.getWinRate(),
            result.getTotalTrades(),
            result.getSharpeRatio(),
            result.getMaxDrawdown(),
            result.getProfitFactor(),
            getStartDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            getEndDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
        
        JOptionPane.showMessageDialog(this, summary, "Backtest Complete", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void optimizeParameters(ActionEvent e) {
        log("Parameter optimization feature coming soon...");
        JOptionPane.showMessageDialog(this, 
            "Parameter optimization will be available in the next version.", 
            "Coming Soon", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void exportResults(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Backtest Results");
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            log("Export feature coming soon...");
            JOptionPane.showMessageDialog(this, 
                "Export functionality will be available in the next version.", 
                "Coming Soon", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void clearResults(ActionEvent e) {
        resultsModel.setRowCount(0);
        metricsModel.setRowCount(0);
        logArea.setText("");
        log("Results cleared.");
    }
    
    private void showSettings(ActionEvent e) {
        JDialog settingsDialog = new JDialog(this, "Application Settings", true);
        settingsDialog.setLayout(new BorderLayout());
        settingsDialog.setSize(400, 300);
        settingsDialog.setLocationRelativeTo(this);
        
        JPanel settingsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Add settings components
        settingsPanel.add(new JLabel("Default Capital:"));
        JTextField defaultCapitalField = new JTextField("10000");
        settingsPanel.add(defaultCapitalField);
        
        settingsPanel.add(new JLabel("Default Timeframes:"));
        JTextField defaultTimeframesField = new JTextField("1D,4H,1H,30Min");
        settingsPanel.add(defaultTimeframesField);
        
        settingsPanel.add(new JLabel("Risk per Trade (%):"));
        JTextField riskPerTradeField = new JTextField("10");
        settingsPanel.add(riskPerTradeField);
        
        JButton saveButton = new JButton("Save Settings");
        saveButton.addActionListener(ev -> {
            // Save settings logic here
            settingsDialog.dispose();
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(ev -> settingsDialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        settingsDialog.add(settingsPanel, BorderLayout.CENTER);
        settingsDialog.add(buttonPanel, BorderLayout.SOUTH);
        settingsDialog.setVisible(true);
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    @Override
    public void dispose() {
        if (controller != null) {
            controller.cleanup();
        }
        super.dispose();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getLookAndFeel());
                
                // Initialize dependencies
                OkHttpClient client = new OkHttpClient();
                StockDataProviderFactory providerFactory = new StockDataProviderFactory(
                    client, 
                    "PK4UOZQDJJZ6WBAU52XM",//System.getenv("ALPACA_API_KEY"),
                    "Fag4ha2D58VyL0okwXgBHD1IvhoptmI2KiacMaNG",//System.getenv("ALPACA_SECRET_KEY"),
                    "fMCQBX5yvQZL_s6Zi1r9iKBkMkNLzNcw"//System.getenv("POLYGON_API_KEY")
                );
                
                MultiTimeframeBacktestGUI gui = new MultiTimeframeBacktestGUI(providerFactory);
                gui.setVisible(true);
                
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                    "Failed to initialize application: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}