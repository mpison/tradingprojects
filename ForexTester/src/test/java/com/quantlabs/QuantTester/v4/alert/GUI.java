package com.quantlabs.QuantTester.v4.alert;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.json.JSONObject;

public class GUI {
    private JFrame frame;
    private JTable inboxTable;
    private DefaultTableModel tableModel;
    private JTextArea logConsole;
    private JButton startMonitoringButton;
    private JButton stopMonitoringButton;
    private JButton deleteSelectedButton;
    private JButton prevPageButton;
    private JButton nextPageButton;
    private JButton addMessageButton;
    private JLabel pageLabel;
    private JLabel statusLabel;
    private JComboBox<String> dataSourceCombo;
    private JComboBox<String> recheckIntervalCombo;
    private JComboBox<Integer> pageSizeCombo;
    private JTextField searchField;
    private MessageManager messageManager;
    private Scheduler scheduler;
    private ConfigManager configManager;
    private DataFetcher dataFetcher;
    private AlertManager alertManager;
    
    // Settings components
    private JTextField yahooUrlField;
    private JSpinner yahooQpsSpinner;
    private JTextField alpacaUrlField;
    private JTextField alpacaKeyField;
    private JPasswordField alpacaSecretField;
    private JCheckBox buzzCheckBox;
    private JCheckBox gmailCheckBox;
    private JCheckBox oneSignalCheckBox;
    private JTextField gmailFromEmailField;
    private JPasswordField gmailPasswordField;
    private JTextField gmailToEmailField;
    private JTextField smtpPortField;
    private JTextField oneSignalAppIdField;
    private JTextField oneSignalApiKeyField;
    private JTextField oneSignalUserIdField;
    private JSpinner alertIntervalSpinner;
    
    static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public GUI(MessageManager messageManager, Scheduler scheduler, ConfigManager configManager, DataFetcher dataFetcher) {
        this.messageManager = messageManager;
        this.scheduler = scheduler;
        this.configManager = configManager;
        this.dataFetcher = dataFetcher;
        this.alertManager = null;
    }

    public void setAlertManager(AlertManager alertManager) {
        this.alertManager = alertManager;
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public void initialize() {
        frame = new JFrame("Stock Alert Monitoring System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 750);
        frame.setLayout(new BorderLayout());

        // Initialize default newest-first sorting
        messageManager.sortMessages(Comparator.comparing(MessageManager.Message::getTimestamp).reversed());
        
        // Status label
        statusLabel = new JLabel("Status: Monitoring not started");
        
        // Load alert configuration
        configManager.loadAlertConfig("alert_config.json", statusLabel);

        // Create tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Monitor", createMonitorPanel());
        tabbedPane.addTab("Settings", createSettingsTab());
        
       
        
        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.add(statusLabel, BorderLayout.SOUTH);
        frame.setVisible(true);

        updateTable();
    }

    private JPanel createMonitorPanel() {
        // Data source selection
        dataSourceCombo = new JComboBox<>(new String[]{"Yahoo", "Alpaca"});
        dataSourceCombo.setSelectedItem("Yahoo");
        dataSourceCombo.addActionListener(e -> scheduler.setDataSource((String) dataSourceCombo.getSelectedItem()));

        recheckIntervalCombo = new JComboBox<>(new String[]{"15 minutes", "1 hour", "4 hours"});

        JButton loadConfigButton = new JButton("Load Config");
        loadConfigButton.addActionListener(e -> {
            configManager.loadConfigFile(frame, statusLabel);
            scheduler.setConfigManager(configManager);
        });

        // Table model with columns
        tableModel = new DefaultTableModel(new Object[]{"Select", "Header", "Body", "Status", "Timestamp", "ID"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: return Boolean.class;
                    case 4: return LocalDateTime.class;
                    default: return String.class;
                }
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 3;
            }
        };

        // Create table with sorting
        inboxTable = new JTable(tableModel) {
            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 0: return Boolean.class;
                    case 4: return LocalDateTime.class;
                    default: return String.class;
                }
            }
        };

        // Configure table
        inboxTable.setAutoCreateRowSorter(false);
        inboxTable.removeColumn(inboxTable.getColumnModel().getColumn(5));
        inboxTable.getColumnModel().getColumn(0).setMaxWidth(50);
        inboxTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        inboxTable.getColumnModel().getColumn(2).setPreferredWidth(300);
        inboxTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        inboxTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        inboxTable.setRowHeight(25);

        // Set custom header renderer for sort indicator
        TableHeaderRenderer headerRenderer = new TableHeaderRenderer();
        headerRenderer.setSortedColumn(4);
        headerRenderer.setSortOrder(SortOrder.DESCENDING);
        inboxTable.getTableHeader().setDefaultRenderer(headerRenderer);

        // Status column editor
        inboxTable.getColumnModel().getColumn(3).setCellEditor(
            new DefaultCellEditor(new JComboBox<>(MessageManager.MessageStatus.values())) {
                @Override
                public boolean stopCellEditing() {
                    boolean stopped = super.stopCellEditing();
                    if (stopped) {
                        int row = inboxTable.getEditingRow();
                        int modelRow = inboxTable.convertRowIndexToModel(row);
                        String messageId = (String) tableModel.getValueAt(modelRow, 5);
                        MessageManager.Message msg = messageManager.getMessageById(messageId);
                        if (msg != null) {
                            msg.setStatus((MessageManager.MessageStatus) getCellEditorValue());
                        }
                    }
                    return stopped;
                }
            });

        // Double-click to view message details
        inboxTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = inboxTable.getSelectedRow();
                    if (row >= 0) {
                        int modelRow = inboxTable.convertRowIndexToModel(row);
                        String messageId = (String) tableModel.getValueAt(modelRow, 5);
                        MessageManager.Message msg = messageManager.getMessageById(messageId);
                        if (msg != null) {
                            showMessageDetailsDialog(msg);
                        }
                    }
                }
            }
        });

        // Create search panel
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { filterMessages(); }
            public void insertUpdate(DocumentEvent e) { filterMessages(); }
            public void removeUpdate(DocumentEvent e) { filterMessages(); }
        });
        searchPanel.add(new JLabel("Search:"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Inbox panel with table
        JPanel inboxPanel = new JPanel(new BorderLayout());
        inboxPanel.add(new JScrollPane(inboxTable), BorderLayout.CENTER);
        
        // Pagination controls
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        pageSizeCombo = new JComboBox<>(new Integer[]{10, 20, 50, 100});
        pageSizeCombo.setSelectedItem(messageManager.getRowsPerPage());
        pageSizeCombo.setToolTipText("Items per page");
        pageSizeCombo.addActionListener(e -> {
            messageManager.setRowsPerPage((Integer)pageSizeCombo.getSelectedItem());
            messageManager.setCurrentPage(1);
            updateTable();
        });
        
        prevPageButton = new JButton("< Prev");
        prevPageButton.addActionListener(e -> {
            messageManager.prevPage();
            updateTable();
        });
        
        pageLabel = new JLabel();
        
        nextPageButton = new JButton("Next >");
        nextPageButton.addActionListener(e -> {
            messageManager.nextPage();
            updateTable();
        });
        
        deleteSelectedButton = new JButton("Delete Selected");
        deleteSelectedButton.addActionListener(e -> deleteSelectedMessages());
        
        // Add components to pagination panel
        paginationPanel.add(new JLabel("Items per page:"));
        paginationPanel.add(pageSizeCombo);
        paginationPanel.add(Box.createHorizontalStrut(20));
        paginationPanel.add(deleteSelectedButton);
        paginationPanel.add(Box.createHorizontalStrut(10));
        paginationPanel.add(prevPageButton);
        paginationPanel.add(pageLabel);
        paginationPanel.add(nextPageButton);
        
        inboxPanel.add(paginationPanel, BorderLayout.SOUTH);

        // Log panel
        logConsole = new JTextArea();
        logConsole.setEditable(false);
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.add(new JLabel("API Log"), BorderLayout.NORTH);
        logPanel.add(new JScrollPane(logConsole), BorderLayout.CENTER);

        // Split pane for inbox and log
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(400);
        splitPane.setTopComponent(inboxPanel);
        splitPane.setBottomComponent(logPanel);

        // Control panel with monitoring buttons and search
        JPanel controlPanel = new JPanel(new BorderLayout());
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton checkNowButton = new JButton("Check Now");
        checkNowButton.addActionListener(e -> scheduler.checkForCrossovers(statusLabel));

        startMonitoringButton = new JButton("Start Monitoring");
        stopMonitoringButton = new JButton("Stop Monitoring");
        stopMonitoringButton.setEnabled(false);

        startMonitoringButton.addActionListener(e -> {
            scheduler.startMonitoring(recheckIntervalCombo, statusLabel);
            startMonitoringButton.setEnabled(false);
            stopMonitoringButton.setEnabled(true);
            if (alertManager != null) {
                long alertInterval = configManager.getAlertConfig().optLong("alertIntervalSeconds", 60);
                alertManager.startAlerting(alertInterval);
            }
        });
        
        stopMonitoringButton.addActionListener(e -> {
            scheduler.stopMonitoring(statusLabel);
            startMonitoringButton.setEnabled(true);
            stopMonitoringButton.setEnabled(false);
            if (alertManager != null) {
                alertManager.stopAlerting();
            }
        });

        addMessageButton = new JButton("Add Message");
        addMessageButton.addActionListener(e -> showAddMessageDialog());

        buttonPanel.add(new JLabel("Data Source:"));
        buttonPanel.add(dataSourceCombo);
        buttonPanel.add(new JLabel("Recheck Interval:"));
        buttonPanel.add(recheckIntervalCombo);
        buttonPanel.add(loadConfigButton);
        buttonPanel.add(checkNowButton);
        buttonPanel.add(startMonitoringButton);
        buttonPanel.add(stopMonitoringButton);
        buttonPanel.add(addMessageButton);
        
        controlPanel.add(buttonPanel, BorderLayout.NORTH);
        controlPanel.add(searchPanel, BorderLayout.SOUTH);

        // Main monitor panel
        JPanel monitorPanel = new JPanel(new BorderLayout());
        monitorPanel.add(controlPanel, BorderLayout.NORTH);
        monitorPanel.add(splitPane, BorderLayout.CENTER);
        
        return monitorPanel;
    }

    private void filterMessages() {
        String searchText = searchField.getText();
        messageManager.applyFilter(searchText, TIMESTAMP_FORMATTER);
        inboxTable.setRowSorter(null);
        updateTable();
    }

    private JPanel createSettingsTab() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Yahoo Settings Panel
        JPanel yahooPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        yahooPanel.setBorder(BorderFactory.createTitledBorder("Yahoo Finance API Settings"));
        yahooPanel.add(new JLabel("Base URL:"));
        yahooUrlField = new JTextField(dataFetcher.getApiConfig().getYahooBaseUrl());
        yahooPanel.add(yahooUrlField);
        yahooPanel.add(new JLabel("Max QPS:"));
        yahooQpsSpinner = new JSpinner(new SpinnerNumberModel(
            dataFetcher.getApiConfig().getYahooMaxQps(), 1, 10000, 1));
        yahooPanel.add(yahooQpsSpinner);

        // Alpaca Settings Panel
        JPanel alpacaPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        alpacaPanel.setBorder(BorderFactory.createTitledBorder("Alpaca API Settings"));
        alpacaPanel.add(new JLabel("Base URL:"));
        alpacaUrlField = new JTextField(dataFetcher.getApiConfig().getAlpacaBaseUrl());
        alpacaPanel.add(alpacaUrlField);
        alpacaPanel.add(new JLabel("API Key:"));
        alpacaKeyField = new JTextField(dataFetcher.getApiConfig().getAlpacaApiKey());
        alpacaPanel.add(alpacaKeyField);
        alpacaPanel.add(new JLabel("Secret Key:"));
        alpacaSecretField = new JPasswordField();
        alpacaSecretField.setText(dataFetcher.getApiConfig().getAlpacaSecretKey());
        alpacaPanel.add(alpacaSecretField);

        // Alert Settings Panel
        JPanel alertPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        alertPanel.setBorder(BorderFactory.createTitledBorder("Alert Settings"));
        JSONObject alertConfig = configManager.getAlertConfig();
        
        buzzCheckBox = new JCheckBox("Enable Buzz Alert", alertConfig.optBoolean("useBuzz", false));
        alertPanel.add(buzzCheckBox);
        JButton testBuzzButton = new JButton("Test Buzz");
        testBuzzButton.addActionListener(e -> {
            if (alertManager != null) {
                alertManager.sendTestBuzzAlert();
            }
        });
        alertPanel.add(testBuzzButton);
        
        gmailCheckBox = new JCheckBox("Enable Gmail Alert", alertConfig.optBoolean("useGmail", false));
        alertPanel.add(gmailCheckBox);
        JButton testGmailButton = new JButton("Test Gmail");
        testGmailButton.addActionListener(e -> {
            if (alertManager != null) {
                alertManager.sendTestGmailAlert();
            }
        });
        alertPanel.add(testGmailButton);
        
        alertPanel.add(new JLabel("Gmail From Email:"));
        gmailFromEmailField = new JTextField(alertConfig.optString("gmailFromEmail", ""));
        alertPanel.add(gmailFromEmailField);
        
        alertPanel.add(new JLabel("Gmail Password:"));
        gmailPasswordField = new JPasswordField(alertConfig.optString("gmailPassword", ""));
        alertPanel.add(gmailPasswordField);
        
        alertPanel.add(new JLabel("Gmail To Email:"));
        gmailToEmailField = new JTextField(alertConfig.optString("gmailToEmail", ""));
        alertPanel.add(gmailToEmailField);
        
        alertPanel.add(new JLabel("SMTP Port:"));
        smtpPortField = new JTextField(alertConfig.optString("smtpPort", "587"));
        alertPanel.add(smtpPortField);
        
        oneSignalCheckBox = new JCheckBox("Enable OneSignal Alert", alertConfig.optBoolean("useOneSignal", false));
        alertPanel.add(oneSignalCheckBox);
        JButton testOneSignalButton = new JButton("Test OneSignal");
        testOneSignalButton.addActionListener(e -> {
            if (alertManager != null) {
                alertManager.sendTestOneSignalAlert();
            }
        });
        alertPanel.add(testOneSignalButton);
        
        alertPanel.add(new JLabel("OneSignal App ID:"));
        oneSignalAppIdField = new JTextField(alertConfig.optString("oneSignalAppId", ""));
        alertPanel.add(oneSignalAppIdField);
        
        alertPanel.add(new JLabel("OneSignal API Key:"));
        oneSignalApiKeyField = new JTextField(alertConfig.optString("oneSignalApiKey", ""));
        alertPanel.add(oneSignalApiKeyField);
        
        alertPanel.add(new JLabel("OneSignal User ID:"));
        oneSignalUserIdField = new JTextField(alertConfig.optString("oneSignalUserId", ""));
        alertPanel.add(oneSignalUserIdField);
        
        alertPanel.add(new JLabel("Alert Recheck Interval (seconds):"));
        alertIntervalSpinner = new JSpinner(new SpinnerNumberModel(
            (int) alertConfig.optLong("alertIntervalSeconds", 60), 10, 3600, 10));
        alertPanel.add(alertIntervalSpinner);

        // Save Button
        JButton saveSettingsButton = new JButton("Save Settings");
        saveSettingsButton.addActionListener(e -> saveApiSettings());

        settingsPanel.add(yahooPanel);
        settingsPanel.add(Box.createVerticalStrut(10));
        settingsPanel.add(alpacaPanel);
        settingsPanel.add(Box.createVerticalStrut(10));
        settingsPanel.add(alertPanel);
        settingsPanel.add(Box.createVerticalStrut(10));
        settingsPanel.add(saveSettingsButton);
        
        return settingsPanel;
    }

    private void showAddMessageDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        
        JTextField headerField = new JTextField();
        panel.add(new JLabel("Header:"));
        panel.add(headerField);
        
        JTextArea bodyArea = new JTextArea(5, 20);
        bodyArea.setLineWrap(true);
        panel.add(new JLabel("Body:"));
        panel.add(new JScrollPane(bodyArea));
        
        JComboBox<MessageManager.MessageStatus> statusCombo = 
            new JComboBox<>(MessageManager.MessageStatus.values());
        statusCombo.setSelectedItem(MessageManager.MessageStatus.NEW);
        panel.add(new JLabel("Status:"));
        panel.add(statusCombo);
        
        int result = JOptionPane.showConfirmDialog(
            frame,
            panel,
            "Add New Message",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            String header = headerField.getText().trim();
            String body = bodyArea.getText().trim();
            MessageManager.MessageStatus status = 
                (MessageManager.MessageStatus)statusCombo.getSelectedItem();
            
            if (!header.isEmpty() && !body.isEmpty()) {
                messageManager.addMessage(header, body, status);
                updateTable();
                scrollToTop();
            } else {
                JOptionPane.showMessageDialog(
                    frame,
                    "Header and body must be filled",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void showMessageDetailsDialog(MessageManager.Message msg) {
        JDialog dialog = new JDialog(frame, "Message Details", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(frame);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(new JLabel("Header:"), BorderLayout.WEST);
        JTextField headerField = new JTextField(msg.getHeader());
        headerField.setEditable(false);
        headerPanel.add(headerField, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(new JLabel("Status:"), BorderLayout.WEST);
        JComboBox<MessageManager.MessageStatus> statusCombo = new JComboBox<>(
            MessageManager.MessageStatus.values());
        statusCombo.setSelectedItem(msg.getStatus());
        statusCombo.addActionListener(e -> {
            msg.setStatus((MessageManager.MessageStatus) statusCombo.getSelectedItem());
            updateTable();
        });
        statusPanel.add(statusCombo, BorderLayout.CENTER);

        JPanel bodyPanel = new JPanel(new BorderLayout());
        bodyPanel.add(new JLabel("Body:"), BorderLayout.NORTH);
        JTextArea bodyArea = new JTextArea(msg.getBody());
        bodyArea.setEditable(false);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        bodyPanel.add(new JScrollPane(bodyArea), BorderLayout.CENTER);

        JPanel timestampPanel = new JPanel(new BorderLayout());
        timestampPanel.add(new JLabel("Timestamp:"), BorderLayout.WEST);
        JTextField timestampField = new JTextField(
            msg.getTimestamp().format(TIMESTAMP_FORMATTER));
        timestampField.setEditable(false);
        timestampPanel.add(timestampField, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        topPanel.add(headerPanel);
        topPanel.add(statusPanel);
        topPanel.add(timestampPanel);

        contentPanel.add(topPanel, BorderLayout.NORTH);
        contentPanel.add(bodyPanel, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void saveApiSettings() {
        try {
            // Save API settings
            ApiConfig newConfig = new ApiConfig();
            newConfig.setYahooBaseUrl(yahooUrlField.getText());
            newConfig.setYahooMaxQps((Integer) yahooQpsSpinner.getValue());
            newConfig.setAlpacaBaseUrl(alpacaUrlField.getText());
            newConfig.setAlpacaApiKey(alpacaKeyField.getText());
            newConfig.setAlpacaSecretKey(new String(alpacaSecretField.getPassword()));
            dataFetcher.updateApiConfig(newConfig);

            // Save alert settings
            JSONObject alertConfig = new JSONObject();
            alertConfig.put("useBuzz", buzzCheckBox.isSelected());
            alertConfig.put("useGmail", gmailCheckBox.isSelected());
            alertConfig.put("gmailFromEmail", gmailFromEmailField.getText());
            alertConfig.put("gmailPassword", new String(gmailPasswordField.getPassword()));
            alertConfig.put("gmailToEmail", gmailToEmailField.getText());
            alertConfig.put("smtpPort", smtpPortField.getText());
            alertConfig.put("useOneSignal", oneSignalCheckBox.isSelected());
            alertConfig.put("oneSignalAppId", oneSignalAppIdField.getText());
            alertConfig.put("oneSignalApiKey", oneSignalApiKeyField.getText());
            alertConfig.put("oneSignalUserId", oneSignalUserIdField.getText());
            alertConfig.put("alertIntervalSeconds", ((Number) alertIntervalSpinner.getValue()).longValue());
            configManager.saveAlertConfig("alert_config.json", alertConfig, statusLabel);

            // Restart alert system with new interval
            if (alertManager != null) {
                alertManager.stopAlerting();
                alertManager.startAlerting(((Number) alertIntervalSpinner.getValue()).longValue());
            }

            JOptionPane.showMessageDialog(frame, "Settings saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error saving settings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedMessages() {
        List<String> idsToDelete = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean selected = (Boolean) tableModel.getValueAt(i, 0);
            if (Boolean.TRUE.equals(selected)) {
                String messageId = (String) tableModel.getValueAt(i, 5);
                idsToDelete.add(messageId);
            }
        }
        messageManager.deleteMessages(idsToDelete);
        updateTable();
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        for (MessageManager.Message msg : messageManager.getPaginatedMessages()) {
            tableModel.addRow(new Object[]{
                Boolean.FALSE,
                msg.getHeader(),
                msg.getBody(),
                msg.getStatus().name(),
                msg.getTimestamp().format(TIMESTAMP_FORMATTER),
                msg.getId()
            });
        }
        updateSortIndicator();
        updatePagination();
    }

    private void updateSortIndicator() {
        TableHeaderRenderer headerRenderer = new TableHeaderRenderer();
        headerRenderer.setSortedColumn(4);
        headerRenderer.setSortOrder(SortOrder.DESCENDING);
        inboxTable.getTableHeader().setDefaultRenderer(headerRenderer);
        inboxTable.getTableHeader().repaint();
    }

    private void updatePagination() {
        int current = messageManager.getCurrentPage();
        int total = messageManager.getTotalPages();
        pageLabel.setText("Page " + current + " of " + total);
        prevPageButton.setEnabled(current > 1);
        nextPageButton.setEnabled(current < total);
    }

    private void scrollToTop() {
        JScrollBar vertical = ((JScrollPane)inboxTable.getParent().getParent()).getVerticalScrollBar();
        vertical.setValue(vertical.getMinimum());
    }

    private static class TableHeaderRenderer extends DefaultTableCellRenderer {
        private int sortedColumn = -1;
        private SortOrder sortOrder = SortOrder.UNSORTED;
        
        public void setSortedColumn(int column) { sortedColumn = column; }
        public void setSortOrder(SortOrder order) { sortOrder = order; }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setIcon(column == sortedColumn ? 
                (sortOrder == SortOrder.ASCENDING ? 
                    UIManager.getIcon("Table.ascendingSortIcon") :
                    UIManager.getIcon("Table.descendingSortIcon")) : null);
            return this;
        }
    }
}