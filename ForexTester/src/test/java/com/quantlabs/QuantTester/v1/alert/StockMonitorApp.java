package com.quantlabs.QuantTester.v1.alert;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
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
import javax.swing.table.DefaultTableModel;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import com.quantlabs.core.indicators.HeikenAshiIndicator;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StockMonitorApp {
    private JFrame frame;
    private JTable inboxTable;
    private DefaultTableModel tableModel;
    private List<Message> messages;
    private JTextArea logConsole;
    private JComboBox<String> recheckIntervalCombo;
    private JComboBox<String> dataSourceCombo;
    private JLabel statusLabel;
    private ScheduledExecutorService scheduler;
    private Map<String, Long> alertTimestamps;
    private int apiCalls = 0;
    private long lastResetTime = System.currentTimeMillis();
    private static final int API_LIMIT = 2000; // Calls per minute
    private String configContent;
    private OkHttpClient client;
    private static final Set<String> VALID_TIMEFRAMES = new HashSet<>(Arrays.asList("1h", "4h", "1d", "1m", "5m", "15m", "30m"));
    private String currentDataSource = "Yahoo";
    private String apiKey = "YOUR_ALPACA_API_KEY"; // Replace with actual key
    private String apiSecret = "YOUR_ALPACA_SECRET_KEY"; // Replace with actual secret
    private JDatePickerImpl startDatePicker;
    private JDatePickerImpl endDatePicker;
    private JRadioButton customRangeRadio;
    private JButton startMonitoringButton;
    private JButton stopMonitoringButton;
    private JButton deleteSelectedButton;
    private JButton prevPageButton;
    private JButton nextPageButton;
    private JLabel pageLabel;
    private int currentPage = 1;
    private static final int ROWS_PER_PAGE = 20;

    // Enum for message status
    public enum MessageStatus {
        OK, UNREAD, MESSAGE_SENT, ERROR
    }

    // Message class to store crossover alerts
    public static class Message {
        private String id; // Unique identifier
        private String header;
        private String body;
        private MessageStatus status;
        private LocalDateTime timestamp;

        public Message(String header, String body, MessageStatus status, LocalDateTime timestamp) {
            this.id = UUID.randomUUID().toString();
            this.header = header;
            this.body = body;
            this.status = status;
            this.timestamp = timestamp;
        }

        public String getId() { return id; }
        public String getHeader() { return header; }
        public String getBody() { return body; }
        public MessageStatus getStatus() { return status; }
        public void setStatus(MessageStatus status) { this.status = status; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    // OHLCDataItem class to hold stock data
    public static class OHLCDataItem {
        long timestamp;
        double open, high, low, close;
        long volume;

        public OHLCDataItem(long timestamp, double open, double high, double low, double close, long volume) {
            this.timestamp = timestamp;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }
    }

    // StockDataResult class to hold API response
    public static class StockDataResult {
        String response;
        ZonedDateTime start;
        ZonedDateTime end;

        public StockDataResult(String response, ZonedDateTime start, ZonedDateTime end) {
            this.response = response;
            this.start = start;
            this.end = end;
        }
    }

    public StockMonitorApp() {
        alertTimestamps = new HashMap<>();
        messages = new ArrayList<>();
        client = new OkHttpClient();
        configContent = getDefaultConfig();
        initializeGUI();
    }

    private String getDefaultConfig() {
        return "{\n" +
                "  \"mappings\": [\n" +
                "    {\n" +
                "      \"name\": \"watchlistMap1\",\n" +
                "      \"watchlist\": \"Tech Stocks\",\n" +
                "      \"combination\": \"Combination1\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"watchlistMap2\",\n" +
                "      \"watchlist\": \"Tech Stocks\",\n" +
                "      \"combination\": \"Combination2\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"combinations\": [\n" +
                "    {\n" +
                "      \"name\": \"Combination1\",\n" +
                "      \"indicators\": [\n" +
                "        {\n" +
                "          \"timeframe\": \"4h\",\n" +
                "          \"shift\": 0,\n" +
                "          \"type\": \"Heikin-Ashi\",\n" +
                "          \"params\": {}\n" +
                "        },\n" +
                "        {\n" +
                "          \"timeframe\": \"4h\",\n" +
                "          \"shift\": 0,\n" +
                "          \"type\": \"MACD\",\n" +
                "          \"params\": {\n" +
                "            \"signalTimeFrame\": 9,\n" +
                "            \"shortTimeFrame\": 12,\n" +
                "            \"longTimeFrame\": 26\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"timeframe\": \"4h\",\n" +
                "          \"shift\": 0,\n" +
                "          \"type\": \"PSAR\",\n" +
                "          \"params\": {\n" +
                "            \"accelerationFactor\": 0.01,\n" +
                "            \"maxAcceleration\": 0.01\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"timeframe\": \"4h\",\n" +
                "          \"shift\": 0,\n" +
                "          \"type\": \"PSAR\",\n" +
                "          \"params\": {\n" +
                "            \"accelerationFactor\": 0.05,\n" +
                "            \"maxAcceleration\": 0.05\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"Combination2\",\n" +
                "      \"indicators\": [\n" +
                "        {\n" +
                "          \"timeframe\": \"4h\",\n" +
                "          \"shift\": 0,\n" +
                "          \"type\": \"MACD\",\n" +
                "          \"params\": {\n" +
                "            \"signalTimeFrame\": 9,\n" +
                "            \"shortTimeFrame\": 12,\n" +
                "            \"longTimeFrame\": 26\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"timeframe\": \"4h\",\n" +
                "          \"shift\": 0,\n" +
                "          \"type\": \"PSAR\",\n" +
                "          \"params\": {\n" +
                "            \"accelerationFactor\": 0.01,\n" +
                "            \"maxAcceleration\": 0.01\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"timeframe\": \"4h\",\n" +
                "          \"shift\": 0,\n" +
                "          \"type\": \"RSI\",\n" +
                "          \"params\": {\n" +
                "            \"timeFrame\": 14\n" +
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"watchlists\": [\n" +
                "    {\n" +
                "      \"name\": \"Tech Stocks\",\n" +
                "      \"stocks\": [\n" +
                "        {\n" +
                "          \"symbol\": \"AAPL\",\n" +
                "          \"name\": \"Apple Inc.\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"symbol\": \"MSFT\",\n" +
                "          \"name\": \"Microsoft\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"symbol\": \"GOOGL\",\n" +
                "          \"name\": \"Alphabet\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"Finance Stocks\",\n" +
                "      \"stocks\": [\n" +
                "        {\n" +
                "          \"symbol\": \"JPM\",\n" +
                "          \"name\": \"JPMorgan Chase\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"symbol\": \"BAC\",\n" +
                "          \"name\": \"Bank of America\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    private void initializeGUI() {
        frame = new JFrame("Stock Alert Monitoring System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // Create split pane for inbox table and log console
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(300);

        // Inbox table for crossover alerts
        tableModel = new DefaultTableModel(new Object[]{"Select", "Header", "Body", "Status"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 3; // Checkbox and Status are editable
            }
        };
        inboxTable = new JTable(tableModel);
        JComboBox<MessageStatus> statusComboBox = new JComboBox<>(MessageStatus.values());
        inboxTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(statusComboBox));
        inboxTable.getColumnModel().getColumn(0).setMaxWidth(50); // Checkbox column
        inboxTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Header
        inboxTable.getColumnModel().getColumn(2).setPreferredWidth(300); // Body
        inboxTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Status
        inboxTable.setRowHeight(25);
        inboxTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = inboxTable.getSelectedRow();
                    if (row >= 0) {
                        int modelRow = inboxTable.convertRowIndexToModel(row);
                        String id = (String) tableModel.getValueAt(modelRow, 1);
                        for (Message msg : messages) {
                            if (msg.getHeader().equals(id)) {
                                JOptionPane.showMessageDialog(frame, msg.getBody(), "Full Message", JOptionPane.INFORMATION_MESSAGE);
                                break;
                            }
                        }
                    }
                }
            }
        });
        JLabel inboxLabel = new JLabel("Crossover Alerts");
        JPanel inboxPanel = new JPanel(new BorderLayout());
        inboxPanel.add(inboxLabel, BorderLayout.NORTH);
        inboxPanel.add(new JScrollPane(inboxTable), BorderLayout.CENTER);

        // Pagination controls
        JPanel paginationPanel = new JPanel(new FlowLayout());
        prevPageButton = new JButton("Previous");
        prevPageButton.addActionListener(e -> prevPage());
        paginationPanel.add(prevPageButton);
        pageLabel = new JLabel("Page 1 of 1");
        paginationPanel.add(pageLabel);
        nextPageButton = new JButton("Next");
        nextPageButton.addActionListener(e -> nextPage());
        paginationPanel.add(nextPageButton);
        deleteSelectedButton = new JButton("Delete Selected");
        deleteSelectedButton.addActionListener(e -> deleteSelectedMessages());
        paginationPanel.add(deleteSelectedButton);
        inboxPanel.add(paginationPanel, BorderLayout.SOUTH);
        splitPane.setTopComponent(inboxPanel);

        // Log console for API calls
        logConsole = new JTextArea();
        logConsole.setEditable(false);
        JLabel logLabel = new JLabel("API Log");
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.add(logLabel, BorderLayout.NORTH);
        logPanel.add(new JScrollPane(logConsole), BorderLayout.CENTER);
        splitPane.setBottomComponent(logPanel);

        frame.add(splitPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        // Data Source Combo Box
        controlPanel.add(new JLabel("Data Source: "));
        dataSourceCombo = new JComboBox<>(new String[]{"Yahoo", "Alpaca"});
        dataSourceCombo.setSelectedItem("Yahoo");
        dataSourceCombo.addActionListener(e -> currentDataSource = (String) dataSourceCombo.getSelectedItem());
        controlPanel.add(dataSourceCombo);

        // Recheck Interval Combo Box
        controlPanel.add(new JLabel("Recheck Interval: "));
        recheckIntervalCombo = new JComboBox<>(new String[]{"15 minutes", "1 hour", "4 hours"});
        controlPanel.add(recheckIntervalCombo);

        // Custom Date Range
        controlPanel.add(new JLabel("Custom Range: "));
        customRangeRadio = new JRadioButton("Enable");
        controlPanel.add(customRangeRadio);

        UtilDateModel startModel = new UtilDateModel();
        UtilDateModel endModel = new UtilDateModel();
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl startDatePanel = new JDatePanelImpl(startModel, p);
        JDatePanelImpl endDatePanel = new JDatePanelImpl(endModel, p);
        startDatePicker = new JDatePickerImpl(startDatePanel, new DateLabelFormatter());
        endDatePicker = new JDatePickerImpl(endDatePanel, new DateLabelFormatter());
        controlPanel.add(new JLabel("Start: "));
        controlPanel.add(startDatePicker);
        controlPanel.add(new JLabel("End: "));
        controlPanel.add(endDatePicker);

        JButton refreshButton = new JButton("Refresh Now");
        refreshButton.addActionListener(e -> checkForCrossovers());
        controlPanel.add(refreshButton);
        JButton loadConfigButton = new JButton("Load Config");
        loadConfigButton.addActionListener(e -> loadConfigFile());
        controlPanel.add(loadConfigButton);
        startMonitoringButton = new JButton("Start Monitoring");
        startMonitoringButton.addActionListener(e -> startMonitoring());
        controlPanel.add(startMonitoringButton);
        stopMonitoringButton = new JButton("Stop Monitoring");
        stopMonitoringButton.setEnabled(false);
        stopMonitoringButton.addActionListener(e -> stopMonitoring());
        controlPanel.add(stopMonitoringButton);
        JButton clearLogButton = new JButton("Clear Log");
        clearLogButton.addActionListener(e -> logConsole.setText(""));
        controlPanel.add(clearLogButton);
        JButton addMessageButton = new JButton("Add Message");
        addMessageButton.addActionListener(e -> addManualMessage());
        controlPanel.add(addMessageButton);
        frame.add(controlPanel, BorderLayout.NORTH);

        statusLabel = new JLabel("Status: Monitoring not started");
        frame.add(statusLabel, BorderLayout.SOUTH);

        frame.setVisible(true);
        updateTable();
    }

    private void addManualMessage() {
        JPanel panel = new JPanel(new GridLayout(3, 2));
        JTextField headerField = new JTextField();
        JTextArea bodyArea = new JTextArea(5, 20);
        JComboBox<MessageStatus> statusCombo = new JComboBox<>(MessageStatus.values());
        panel.add(new JLabel("Header:"));
        panel.add(headerField);
        panel.add(new JLabel("Body:"));
        panel.add(new JScrollPane(bodyArea));
        panel.add(new JLabel("Status:"));
        panel.add(statusCombo);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Add New Message", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String header = headerField.getText().trim();
            String body = bodyArea.getText().trim();
            MessageStatus status = (MessageStatus) statusCombo.getSelectedItem();
            if (!header.isEmpty() && !body.isEmpty()) {
                Message message = new Message(header, body, status, LocalDateTime.now());
                messages.add(message);
                updateTable();
            } else {
                JOptionPane.showMessageDialog(frame, "Header and Body cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        int startIndex = (currentPage - 1) * ROWS_PER_PAGE;
        int endIndex = Math.min(startIndex + ROWS_PER_PAGE, messages.size());
        for (int i = startIndex; i < endIndex; i++) {
            Message msg = messages.get(i);
            String shortBody = msg.getBody().length() > 50 ? msg.getBody().substring(0, 50) + "..." : msg.getBody();
            tableModel.addRow(new Object[]{false, msg.getHeader(), shortBody, msg.getStatus().toString()});
        }
        int totalPages = (int) Math.ceil((double) messages.size() / ROWS_PER_PAGE);
        pageLabel.setText("Page " + currentPage + " of " + Math.max(1, totalPages));
        prevPageButton.setEnabled(currentPage > 1);
        nextPageButton.setEnabled(currentPage < totalPages);
    }

    private void prevPage() {
        if (currentPage > 1) {
            currentPage--;
            updateTable();
        }
    }

    private void nextPage() {
        int totalPages = (int) Math.ceil((double) messages.size() / ROWS_PER_PAGE);
        if (currentPage < totalPages) {
            currentPage++;
            updateTable();
        }
    }

    private void deleteSelectedMessages() {
        List<String> idsToDelete = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if ((Boolean) tableModel.getValueAt(i, 0)) {
                idsToDelete.add((String) tableModel.getValueAt(i, 1));
            }
        }
        messages.removeIf(msg -> idsToDelete.contains(msg.getHeader()));
        updateTable();
    }

    private void loadConfigFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files", "json"));
        fileChooser.setCurrentDirectory(new java.io.File("."));
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                String newConfigContent = new String(Files.readAllBytes(fileChooser.getSelectedFile().toPath()));
                JSONObject config = new JSONObject(newConfigContent);
                if (!config.has("mappings") || !config.has("combinations") || !config.has("watchlists")) {
                    throw new JSONException("Missing required fields: mappings, combinations, or watchlists");
                }
                JSONArray mappings = config.getJSONArray("mappings");
                for (int i = 0; i < mappings.length(); i++) {
                    JSONObject mapping = mappings.getJSONObject(i);
                    if (!mapping.has("name") || !mapping.has("watchlist") || !mapping.has("combination")) {
                        throw new JSONException("Mapping at index " + i + " missing required fields");
                    }
                }
                JSONArray combinations = config.getJSONArray("combinations");
                for (int i = 0; i < combinations.length(); i++) {
                    JSONObject combination = combinations.getJSONObject(i);
                    if (!combination.has("name") || !combination.has("indicators")) {
                        throw new JSONException("Combination at index " + i + " missing required fields");
                    }
                    JSONArray indicators = combination.getJSONArray("indicators");
                    for (int j = 0; j < indicators.length(); j++) {
                        JSONObject indicator = indicators.getJSONObject(j);
                        if (!indicator.has("type") || !indicator.has("timeframe") || !indicator.has("shift") || !indicator.has("params")) {
                            throw new JSONException("Indicator at index " + j + " in combination " + combination.getString("name") + " missing required fields");
                        }
                        String timeframe = indicator.getString("timeframe");
                        if (!VALID_TIMEFRAMES.contains(convertTimeframeToYahooInterval(timeframe))) {
                            throw new JSONException("Invalid timeframe '" + timeframe + "' in indicator at index " + j + " in combination " + combination.getString("name"));
                        }
                    }
                }
                JSONArray watchlists = config.getJSONArray("watchlists");
                for (int i = 0; i < watchlists.length(); i++) {
                    JSONObject watchlist = watchlists.getJSONObject(i);
                    if (!watchlist.has("name") || !watchlist.has("stocks")) {
                        throw new JSONException("Watchlist at index " + i + " missing required fields");
                    }
                }
                configContent = newConfigContent;
                alertTimestamps.clear();
                messages.clear();
                updateTable();
                statusLabel.setText("Status: Configuration loaded from " + fileChooser.getSelectedFile().getName());
            } catch (IOException e) {
                statusLabel.setText("Status: Error loading file - " + e.getMessage());
                JOptionPane.showMessageDialog(frame, "Error reading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (JSONException e) {
                statusLabel.setText("Status: Invalid JSON format - " + e.getMessage());
                JOptionPane.showMessageDialog(frame, "Invalid JSON format: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            statusLabel.setText("Status: Config load cancelled, using default configuration");
        }
    }

    private void startMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    statusLabel.setText("Status: Warning - Previous monitoring did not terminate cleanly");
                }
            } catch (InterruptedException e) {
                statusLabel.setText("Status: Error stopping previous monitoring - " + e.getMessage());
                logConsole.append(String.format("[%s] Error stopping previous monitoring: %s\n",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), e.getMessage()));
                e.printStackTrace();
            }
        }
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkForCrossovers, 0, 1, TimeUnit.MINUTES);
        startMonitoringButton.setEnabled(false);
        stopMonitoringButton.setEnabled(true);
        statusLabel.setText("Status: Monitoring started at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    statusLabel.setText("Status: Warning - Monitoring did not terminate cleanly");
                } else {
                    statusLabel.setText("Status: Monitoring stopped at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                }
            } catch (InterruptedException e) {
                statusLabel.setText("Status: Error stopping monitoring - " + e.getMessage());
                logConsole.append(String.format("[%s] Error stopping monitoring: %s\n",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), e.getMessage()));
                e.printStackTrace();
            }
        }
        startMonitoringButton.setEnabled(true);
        stopMonitoringButton.setEnabled(false);
    }

    private void checkForCrossovers() {
        try {
            if (System.currentTimeMillis() - lastResetTime >= 60_000) {
                apiCalls = 0;
                lastResetTime = System.currentTimeMillis();
            }
            if (apiCalls >= API_LIMIT) {
                statusLabel.setText("Status: API limit reached, waiting...");
                logConsole.append(String.format("[%s] API limit reached (%d calls), waiting for reset\n",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), API_LIMIT));
                return;
            }

            statusLabel.setText("Status: Checking for crossovers...");
            JSONObject config = new JSONObject(configContent);

            JSONArray mappings = config.getJSONArray("mappings");
            for (int i = 0; i < mappings.length(); i++) {
                JSONObject mapping = mappings.getJSONObject(i);
                String watchlistName;
                String combinationName;
                try {
                    if (!mapping.has("name") || !mapping.has("watchlist") || !mapping.has("combination")) {
                        throw new JSONException("Mapping at index " + i + " missing required fields");
                    }
                    watchlistName = mapping.getString("watchlist");
                    combinationName = mapping.getString("combination");
                } catch (JSONException e) {
                    statusLabel.setText("Status: Error processing mapping at index " + i + ": " + e.getMessage());
                    logConsole.append(String.format("[%s] Error processing mapping at index %d: %s\n",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), i, e.getMessage()));
                    continue;
                }

                JSONArray watchlists = config.getJSONArray("watchlists");
                JSONObject watchlist = null;
                for (int j = 0; j < watchlists.length(); j++) {
                    if (watchlists.getJSONObject(j).getString("name").equals(watchlistName)) {
                        watchlist = watchlists.getJSONObject(j);
                        break;
                    }
                }

                JSONArray combinations = config.getJSONArray("combinations");
                JSONObject combination = null;
                for (int j = 0; j < combinations.length(); j++) {
                    if (combinations.getJSONObject(j).getString("name").equals(combinationName)) {
                        combination = combinations.getJSONObject(j);
                        break;
                    }
                }

                if (watchlist == null || combination == null) {
                    String errorMessage = String.format("Invalid mapping: watchlist '%s' or combination '%s' not found", watchlistName, combinationName);
                    statusLabel.setText("Status: " + errorMessage);
                    logConsole.append(String.format("[%s] %s\n",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), errorMessage));
                    continue;
                }

                JSONArray stocks = watchlist.getJSONArray("stocks");
                JSONArray indicators = combination.getJSONArray("indicators");

                Set<String> timeframes = new HashSet<>();
                int maxShift = 0;
                for (int k = 0; k < indicators.length(); k++) {
                    JSONObject indicator = indicators.getJSONObject(k);
                    timeframes.add(indicator.getString("timeframe"));
                    maxShift = Math.max(maxShift, indicator.getInt("shift"));
                }

                for (int j = 0; j < stocks.length(); j++) {
                    String symbol;
                    try {
                        symbol = stocks.getJSONObject(j).getString("symbol");
                    } catch (JSONException e) {
                        statusLabel.setText("Status: Error processing stock at index " + j + " in watchlist '" + watchlistName + "': " + e.getMessage());
                        logConsole.append(String.format("[%s] Error processing stock at index %d in watchlist '%s': %s\n",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), j, watchlistName, e.getMessage()));
                        continue;
                    }
                    String alertKey = symbol + "_" + combinationName + "_combo";
                    long recheckInterval = getRecheckInterval();
                    if (alertTimestamps.containsKey(alertKey)) {
                        long lastAlert = alertTimestamps.get(alertKey);
                        if (System.currentTimeMillis() - lastAlert < recheckInterval) {
                            continue;
                        }
                    }

                    Map<String, List<OHLCDataItem>> timeframeData = new HashMap<>();
                    boolean fetchFailed = false;
                    for (String timeframe : timeframes) {
                        List<OHLCDataItem> data = fetchData(symbol, timeframe, maxShift + 1);
                        if (data == null) {
                            fetchFailed = true;
                            break;
                        }
                        timeframeData.put(timeframe, data);
                        apiCalls++;
                    }

                    if (fetchFailed) {
                        continue;
                    }

                    List<String> crossovers = new ArrayList<>();
                    boolean allCriteriaMet = true;
                    for (int k = 0; k < indicators.length(); k++) {
                        JSONObject indicator = indicators.getJSONObject(k);
                        String indType;
                        String timeframe;
                        int shift;
                        JSONObject params;
                        try {
                            indType = indicator.getString("type");
                            timeframe = indicator.getString("timeframe");
                            shift = indicator.getInt("shift");
                            params = indicator.getJSONObject("params");
                        } catch (JSONException e) {
                            statusLabel.setText("Status: Error processing indicator at index " + k + " for symbol " + symbol + ": " + e.getMessage());
                            logConsole.append(String.format("[%s] Error processing indicator at index %d for symbol %s: %s\n",
                                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), k, symbol, e.getMessage()));
                            allCriteriaMet = false;
                            break;
                        }

                        List<OHLCDataItem> data = timeframeData.get(timeframe);
                        String crossover = calculateIndicator(indType, data, params, shift);
                        if (crossover == null) {
                            allCriteriaMet = false;
                            break;
                        }
                        crossovers.add(String.format("%s (shift: %d)", indType, shift));
                    }

                    if (allCriteriaMet && !crossovers.isEmpty()) {
                        String timestampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        String header = String.format("[%s] %s: All criteria met", timestampStr, symbol);
                        String body = String.format("[%s] %s: All criteria met for %s on %s: %s",
                                timestampStr, symbol, combinationName, watchlistName, String.join(", ", crossovers));
                        Message message = new Message(header, body, MessageStatus.OK, LocalDateTime.now());
                        messages.add(message);
                        updateTable();
                        alertTimestamps.put(alertKey, System.currentTimeMillis());
                    }
                }
            }
            statusLabel.setText("Status: Last check at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (Exception e) {
            statusLabel.setText("Status: Error - " + e.getMessage());
            logConsole.append(String.format("[%s] Error during crossover check: %s\n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), e.getMessage()));
            e.printStackTrace();
        }
    }

    private long getRecheckInterval() {
        String selected = (String) recheckIntervalCombo.getSelectedItem();
        switch (selected) {
            case "15 minutes": return 15 * 60_000;
            case "1 hour": return 60 * 60_000;
            case "4 hours": return 4 * 60 * 60_000;
            default: return 60 * 60_000;
        }
    }

    private StockDataResult fetchStockData(String symbol, String timeframe, int limit) throws IOException {
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime start = calculateYahooStartTime(timeframe, end);

        if (customRangeRadio.isSelected() && startDatePicker.getModel().getValue() != null && endDatePicker.getModel().getValue() != null) {
            try {
                start = ((Date) startDatePicker.getModel().getValue()).toInstant().atZone(ZoneOffset.UTC);
                end = ((Date) endDatePicker.getModel().getValue()).toInstant().atZone(ZoneOffset.UTC);
            } catch (Exception e) {
                statusLabel.setText("Status: Error parsing custom date range for " + symbol + ": " + e.getMessage());
                logConsole.append(String.format("[%s] Error parsing custom date range for %s: %s\n",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), symbol, e.getMessage()));
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
                .newBuilder()
                .addQueryParameter("timeframe", timeframe)
                .addQueryParameter("start", start.format(DateTimeFormatter.ISO_INSTANT))
                .addQueryParameter("end", end.format(DateTimeFormatter.ISO_INSTANT))
                .addQueryParameter("limit", String.valueOf(limit))
                .addQueryParameter("adjustment", "raw")
                .addQueryParameter("feed", "iex")
                .addQueryParameter("sort", "asc");

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .addHeader("accept", "application/json")
                .addHeader("APCA-API-KEY-ID", apiKey)
                .addHeader("APCA-API-SECRET-KEY", apiSecret)
                .build();

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
            case "4H": return "1h";
            case "1D": return "1d";
            default: return "1d";
        }
    }

    private ZonedDateTime calculateYahooStartTime(String timeframe, ZonedDateTime end) {
        switch (timeframe) {
            case "1Min": return end.minusDays(7);
            case "5Min":
            case "15Min":
            case "30Min":
            case "1H":
            case "4H":
            case "1D":
                return end.minusDays(60);
            default:
                return end.minusDays(7);
        }
    }

    public List<OHLCDataItem> fetchData(String symbol, String timeframe, int limit) {
        if (!VALID_TIMEFRAMES.contains(convertTimeframeToYahooInterval(timeframe))) {
            statusLabel.setText("Status: Unsupported timeframe " + timeframe + " for " + symbol);
            logConsole.append(String.format("[%s] Unsupported timeframe %s for %s\n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), timeframe, symbol));
            return null;
        }

        try {
            StockDataResult result = fetchStockData(symbol, timeframe, limit);
            return parseResponse(result.response, symbol, timeframe, currentDataSource);
        } catch (IOException e) {
            statusLabel.setText("Status: Network error fetching data for " + symbol + " (" + timeframe + "): " + e.getMessage());
            logConsole.append(String.format("[%s] Network error fetching data for %s (%s): %s\n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), symbol, timeframe, e.getMessage()));
            return null;
        } catch (JSONException e) {
            statusLabel.setText("Status: JSON parsing error for " + symbol + " (" + timeframe + "): " + e.getMessage());
            logConsole.append(String.format("[%s] JSON parsing error for %s (%s): %s\n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), symbol, timeframe, e.getMessage()));
            return null;
        } catch (Exception e) {
            statusLabel.setText("Status: Unexpected error fetching data for " + symbol + " (" + timeframe + "): " + e.getMessage());
            logConsole.append(String.format("[%s] Unexpected error fetching data for %s (%s): %s\n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), symbol, timeframe, e.getMessage()));
            e.printStackTrace();
            return null;
        }
    }

    private List<OHLCDataItem> parseResponse(String response, String symbol, String timeframe, String source) throws JSONException {
        logConsole.append(String.format("[%s] Raw API response for %s (%s): %s\n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), symbol, timeframe, response));

        try {
            if ("Yahoo".equals(source)) {
                JSONObject json = new JSONObject(response);
                JSONObject chart = json.getJSONObject("chart");

                if (chart.has("error") && !chart.isNull("error")) {
                    statusLabel.setText("Status: API error for " + symbol + " (" + timeframe + "): " + chart.getJSONObject("error").optString("description", "Unknown error"));
                    logConsole.append(String.format("[%s] API error for %s (%s): %s\n",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), symbol, timeframe, chart.getJSONObject("error").optString("description", "Unknown error")));
                    return null;
                }

                if (!chart.has("result") || chart.isNull("result") || chart.getJSONArray("result").length() == 0) {
                    statusLabel.setText("Status: No data returned for " + symbol + " (" + timeframe + ")");
                    logConsole.append(String.format("[%s] No data returned for %s (%s)\n",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), symbol, timeframe));
                    return null;
                }

                JSONArray results = chart.getJSONArray("result");
                JSONObject firstResult = results.getJSONObject(0);

                if (!firstResult.has("timestamp") || firstResult.isNull("timestamp")) {
                    statusLabel.setText("Status: Missing timestamp data for " + symbol + " (" + timeframe + ")");
                    logConsole.append(String.format("[%s] Missing timestamp data for %s (%s)\n",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), symbol, timeframe));
                    return null;
                }

                JSONArray timestamps = firstResult.getJSONArray("timestamp");
                JSONObject indicators = firstResult.getJSONObject("indicators");
                if (!indicators.has("quote") || indicators.isNull("quote") || indicators.getJSONArray("quote").length() == 0) {
                    statusLabel.setText("Status: Missing quote data for " + symbol + " (" + timeframe + ")");
                    logConsole.append(String.format("[%s] Missing quote data for %s (%s)\n",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), symbol, timeframe));
                    return null;
                }

                JSONArray quotes = indicators.getJSONArray("quote");
                JSONObject quote = quotes.getJSONObject(0);

                if (!quote.has("open") || !quote.has("high") || !quote.has("low") || !quote.has("close") || !quote.has("volume")) {
                    statusLabel.setText("Status: Missing OHLC or volume data for " + symbol + " (" + timeframe + ")");
                    logConsole.append(String.format("[%s] Missing OHLC or volume data for %s (%s)\n",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), symbol, timeframe));
                    return null;
                }

                JSONArray opens = quote.getJSONArray("open");
                JSONArray highs = quote.getJSONArray("high");
                JSONArray lows = quote.getJSONArray("low");
                JSONArray closes = quote.getJSONArray("close");
                JSONArray volumes = quote.getJSONArray("volume");

                List<OHLCDataItem> data = new ArrayList<>();
                for (int i = 0; i < timestamps.length(); i++) {
                    if (opens.isNull(i) || highs.isNull(i) || lows.isNull(i) || closes.isNull(i) || volumes.isNull(i)) {
                        continue;
                    }
                    data.add(new OHLCDataItem(
                            timestamps.getLong(i) * 1000,
                            opens.getDouble(i),
                            highs.getDouble(i),
                            lows.getDouble(i),
                            closes.getDouble(i),
                            volumes.getLong(i)
                    ));
                }

                if (data.isEmpty()) {
                    statusLabel.setText("Status: No valid OHLC data for " + symbol + " (" + timeframe + ")");
                    logConsole.append(String.format("[%s] No valid OHLC data for %s (%s)\n",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), symbol, timeframe));
                    return null;
                }

                return data;
            } else { // Alpaca
                JSONObject json = new JSONObject(response);
                if (!json.has("bars") || json.isNull("bars")) {
                    statusLabel.setText("Status: Missing bars data for " + symbol + " (" + timeframe + ")");
                    logConsole.append(String.format("[%s] Missing bars data for %s (%s)\n",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), symbol, timeframe));
                    return null;
                }

                JSONArray bars = json.getJSONArray("bars");
                List<OHLCDataItem> data = new ArrayList<>();
                for (int i = 0; i < bars.length(); i++) {
                    JSONObject bar = bars.getJSONObject(i);
                    if (!bar.has("t") || !bar.has("o") || !bar.has("h") || !bar.has("l") || !bar.has("c") || !bar.has("v")) {
                        continue;
                    }
                    ZonedDateTime timestamp = ZonedDateTime.parse(bar.getString("t"));
                    data.add(new OHLCDataItem(
                            timestamp.toInstant().toEpochMilli(),
                            bar.getDouble("o"),
                            bar.getDouble("h"),
                            bar.getDouble("l"),
                            bar.getDouble("c"),
                            bar.getLong("v")
                    ));
                }

                if (data.isEmpty()) {
                    statusLabel.setText("Status: No valid OHLC data for " + symbol + " (" + timeframe + ")");
                    logConsole.append(String.format("[%s] No valid OHLC data for %s (%s)\n",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), symbol, timeframe));
                    return null;
                }

                return data;
            }
        } catch (JSONException e) {
            statusLabel.setText("Status: JSON parsing error for " + symbol + " (" + timeframe + "): " + e.getMessage());
            logConsole.append(String.format("[%s] JSON parsing error for %s (%s): %s\n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), symbol, timeframe, e.getMessage()));
            throw e;
        }
    }

    private String calculateIndicator(String indicator, List<OHLCDataItem> data, JSONObject params, int shift) {
        if (data.size() < shift + 2) {
            statusLabel.setText("Status: Insufficient data for shift " + shift);
            logConsole.append(String.format("[%s] Insufficient data for shift %d\n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), shift));
            return null;
        }

        BarSeries series = new BaseBarSeries();
        for (OHLCDataItem item : data) {
            series.addBar(
                    LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(item.timestamp),
                            ZoneId.systemDefault()
                    ).atZone(ZoneId.systemDefault()),
                    item.open, item.high, item.low, item.close, item.volume
            );
        }

        try {
            int lastIndex = series.getEndIndex();
            int currentIndex = lastIndex - shift;
            int prevIndex = currentIndex - 1;
            if (currentIndex < 0 || prevIndex < 0) {
                statusLabel.setText("Status: Invalid shift " + shift + " for data size " + (lastIndex + 1));
                logConsole.append(String.format("[%s] Invalid shift %d for data size %d\n",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), shift, lastIndex + 1));
                return null;
            }

            if (indicator.equals("Heikin-Ashi")) {
                HeikenAshiIndicator haIndicator = new HeikenAshiIndicator(series);
                Num haOpenCurrent = haIndicator.getHeikenAshiOpen(currentIndex);
                Num haCloseCurrent = haIndicator.getHeikenAshiClose(currentIndex);
                Num haOpenPrev = haIndicator.getHeikenAshiOpen(prevIndex);
                Num haClosePrev = haIndicator.getHeikenAshiClose(prevIndex);
                if (haClosePrev.doubleValue() <= haOpenPrev.doubleValue() && haCloseCurrent.doubleValue() > haOpenCurrent.doubleValue()) {
                    return "Bullish Heikin-Ashi";
                }
                if (haClosePrev.doubleValue() >= haOpenPrev.doubleValue() && haCloseCurrent.doubleValue() < haOpenCurrent.doubleValue()) {
                    return "Bearish Heikin-Ashi";
                }
                return null;
            } else if (indicator.equals("MACD")) {
                int shortPeriod = params.optInt("shortTimeFrame", 12);
                int longPeriod = params.optInt("longTimeFrame", 26);
                int signalPeriod = params.optInt("signalTimeFrame", 9);
                MACDIndicator macd = new MACDIndicator(new ClosePriceIndicator(series), shortPeriod, longPeriod);
                SMAIndicator signal = new SMAIndicator(macd, signalPeriod);
                double macdCurrent = macd.getValue(currentIndex).doubleValue();
                double signalCurrent = signal.getValue(currentIndex).doubleValue();
                double macdPrev = macd.getValue(prevIndex).doubleValue();
                double signalPrev = signal.getValue(prevIndex).doubleValue();
                if (macdPrev <= signalPrev && macdCurrent > signalCurrent) return "Bullish MACD";
                if (macdPrev >= signalPrev && macdCurrent < signalCurrent) return "Bearish MACD";
                return null;
            } else if (indicator.equals("RSI")) {
                int period = params.optInt("timeFrame", 14);
                RSIIndicator rsi = new RSIIndicator(new ClosePriceIndicator(series), period);
                double rsiCurrent = rsi.getValue(currentIndex).doubleValue();
                double rsiPrev = rsi.getValue(prevIndex).doubleValue();
                if (rsiPrev <= 30 && rsiCurrent > 30) return "RSI Oversold Exit";
                if (rsiPrev >= 70 && rsiCurrent < 70) return "RSI Overbought Exit";
                if (rsiCurrent <= 30 || rsiCurrent >= 70) return null;
                return "normal";
            } else if (indicator.equals("PSAR")) {
                double af = params.optDouble("accelerationFactor", 0.02);
                double maxAf = params.optDouble("maxAcceleration", 0.2);
                ParabolicSarIndicator psar = new ParabolicSarIndicator(series, DecimalNum.valueOf(af), DecimalNum.valueOf(maxAf));
                double psarCurrent = psar.getValue(currentIndex).doubleValue();
                double priceCurrent = series.getBar(currentIndex).getClosePrice().doubleValue();
                double psarPrev = psar.getValue(prevIndex).doubleValue();
                double pricePrev = series.getBar(prevIndex).getClosePrice().doubleValue();
                if (pricePrev <= psarPrev && priceCurrent > psarCurrent) return "Bullish PSAR";
                if (pricePrev >= psarPrev && priceCurrent < psarCurrent) return "Bearish PSAR";
                return null;
            } else if (indicator.equals("Stochastic")) {
                int kPeriod = params.optInt("kTimeFrame", 14);
                int dPeriod = params.optInt("dTimeFrame", 3);
                StochasticOscillatorKIndicator kIndicator = new StochasticOscillatorKIndicator(series, kPeriod);
                SMAIndicator dIndicator = new SMAIndicator(kIndicator, dPeriod);
                double kCurrent = kIndicator.getValue(currentIndex).doubleValue();
                double dCurrent = dIndicator.getValue(currentIndex).doubleValue();
                double kPrev = kIndicator.getValue(prevIndex).doubleValue();
                double dPrev = dIndicator.getValue(prevIndex).doubleValue();
                if (kPrev <= dPrev && kCurrent > dCurrent) return "Bullish Stochastic";
                if (kPrev >= dPrev && kCurrent < dCurrent) return "Bearish Stochastic";
                return null;
            }
            return null;
        } catch (Exception e) {
            statusLabel.setText("Status: Error calculating indicator " + indicator + ": " + e.getMessage());
            logConsole.append(String.format("[%s] Error calculating indicator %s: %s\n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), indicator, e.getMessage()));
            return null;
        }
    }

    // DateLabelFormatter for JDatePicker
    public static class DateLabelFormatter extends JFormattedTextField.AbstractFormatter {
        private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public Object stringToValue(String text) throws java.text.ParseException {
            return LocalDate.parse(text, dateFormatter).atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        @Override
        public String valueToString(Object value) throws java.text.ParseException {
            if (value != null) {
                Instant instant = (Instant) value;
                return instant.atZone(ZoneOffset.UTC).format(dateFormatter);
            }
            return "";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(StockMonitorApp::new);
    }
}