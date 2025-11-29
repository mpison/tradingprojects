package com.quantlabs.QuantTester.v3.alert.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import com.quantlabs.QuantTester.v3.alert.Message;
import com.quantlabs.QuantTester.v3.alert.service.StockMonitorController;

public class MainFrame extends JFrame {
    private final StockMonitorController controller;
    private JTable alertTable;
    private DefaultTableModel tableModel;
    private JTextArea logArea;
    private JButton startButton;
    private JButton stopButton;
    private JComboBox<String> dataSourceCombo;
    private Timer refreshTimer;

    public MainFrame(StockMonitorController controller) {
        this.controller = controller;
        initializeUI();
        loadDefaultConfig();
    }

    private void initializeUI() {
        setTitle("Stock Alert Monitor v3.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLayout(new BorderLayout());

        // Create main split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.5);

        // Alert table panel
        JPanel alertPanel = createAlertPanel();
        splitPane.setTopComponent(alertPanel);

        // Log panel
        JPanel logPanel = createLogPanel();
        splitPane.setBottomComponent(logPanel);

        add(splitPane, BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.NORTH);
        add(createStatusBar(), BorderLayout.SOUTH);

        // Start refresh timer
        refreshTimer = new Timer(5000, e -> refreshAlertTable());
        refreshTimer.start();
    }

    private void loadDefaultConfig() {
        try {
            controller.loadConfig(getDefaultConfig());
            logMessage("Default configuration loaded");
        } catch (Exception e) {
            showError("Failed to load default config: " + e.getMessage());
        }
    }

    private JPanel createAlertPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        tableModel = new DefaultTableModel(new Object[]{"Status", "Symbol", "Time", "Message"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        alertTable = new JTable(tableModel);
        alertTable.setAutoCreateRowSorter(true);
        alertTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        alertTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        alertTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        
        JScrollPane scrollPane = new JScrollPane(alertTable);
        panel.add(new JLabel("Recent Alerts:"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        alertTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showAlertDetails();
                }
            }
        });
        
        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        panel.add(new JLabel("Log Messages:"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Config load button
        JButton loadConfigButton = new JButton("Load Config");
        loadConfigButton.addActionListener(this::loadConfigFile);
        panel.add(loadConfigButton);
        
        // Data source selection only (timeframe removed)
        panel.add(new JLabel("Data Source:"));
        dataSourceCombo = new JComboBox<>(new String[]{"Yahoo", "Alpaca"});
        panel.add(dataSourceCombo);
        
        // Control buttons
        startButton = new JButton("Start Monitoring");
        startButton.addActionListener(this::startMonitoring);
        panel.add(startButton);
        
        stopButton = new JButton("Stop Monitoring");
        stopButton.setEnabled(false);
        stopButton.addActionListener(this::stopMonitoring);
        panel.add(stopButton);
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshAlertTable());
        panel.add(refreshButton);
        
        JButton clearButton = new JButton("Clear Logs");
        clearButton.addActionListener(e -> logArea.setText(""));
        panel.add(clearButton);
        
     // Add new inbox management buttons
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteSelectedMessages());
        panel.add(deleteButton);
        
        JButton markReadButton = new JButton("Mark as Read");
        markReadButton.addActionListener(e -> markAsRead());
        panel.add(markReadButton);
        
        return panel;
    }
    
    private void initializeInboxTable() {
        alertTable.setModel(new AlertTableModel());
        alertTable.getColumnModel().getColumn(0).setPreferredWidth(30); // Checkbox column
        alertTable.getColumnModel().getColumn(1).setPreferredWidth(200); // Header column
        alertTable.getColumnModel().getColumn(2).setPreferredWidth(300); // Message column
        alertTable.getColumnModel().getColumn(3).setPreferredWidth(80); // Status column
        
        // Add checkbox renderer and editor
        alertTable.getColumnModel().getColumn(0).setCellRenderer(alertTable.getDefaultRenderer(Boolean.class));
        alertTable.getColumnModel().getColumn(0).setCellEditor(alertTable.getDefaultEditor(Boolean.class));
        
        // Add status combobox editor
        JComboBox<String> statusCombo = new JComboBox<>();
        for (Message.MessageStatus status : Message.MessageStatus.values()) {
            statusCombo.addItem(status.toString());
        }
        alertTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(statusCombo));
    }

    
    private void deleteSelectedMessages() {
        AlertTableModel model = (AlertTableModel) alertTable.getModel();
        List<String> idsToDelete = new ArrayList<>();
        
        for (int i = 0; i < model.getRowCount(); i++) {
            if ((Boolean) model.getValueAt(i, 0)) { // Check if checkbox is checked
                idsToDelete.add(model.getMessages().get(i).getId());
            }
        }
        
        if (!idsToDelete.isEmpty()) {
            controller.deleteMessages(idsToDelete);
            refreshAlertTable();
            logMessage("Deleted " + idsToDelete.size() + " messages");
        }
    }

    private void markAsRead() {
        AlertTableModel model = (AlertTableModel) alertTable.getModel();
        List<String> idsToUpdate = new ArrayList<>();
        
        for (int i = 0; i < model.getRowCount(); i++) {
            if ((Boolean) model.getValueAt(i, 0)) { // Check if checkbox is checked
                idsToUpdate.add(model.getMessages().get(i).getId());
            }
        }
        
        if (!idsToUpdate.isEmpty()) {
            controller.updateMessagesStatus(idsToUpdate, Message.MessageStatus.OK);
            refreshAlertTable();
            logMessage("Marked " + idsToUpdate.size() + " messages as read");
        }
    }

    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Status: Ready"));
        return panel;
    }

    private void loadConfigFile(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String configContent = new String(Files.readAllBytes(fileChooser.getSelectedFile().toPath()));
                controller.loadConfig(configContent);
                logMessage("Configuration loaded from: " + fileChooser.getSelectedFile().getName());
            } catch (IOException ex) {
                showError("File read error: " + ex.getMessage());
            } catch (Exception ex) {
                showError("Invalid configuration: " + ex.getMessage());
            }
        }
    }

    private void startMonitoring(ActionEvent e) {
        try {
            if (!controller.isConfigLoaded()) {
                int response = JOptionPane.showConfirmDialog(
                    this,
                    "No configuration loaded. Use default configuration?",
                    "Configuration Required",
                    JOptionPane.YES_NO_OPTION
                );
                
                if (response == JOptionPane.YES_OPTION) {
                    controller.loadConfig(getDefaultConfig());
                    logMessage("Default configuration loaded");
                } else {
                    return;
                }
            }
            
            String dataSource = (String) dataSourceCombo.getSelectedItem();
            controller.setDataSource(dataSource.toLowerCase());
            controller.startMonitoring(); // No timeframe parameter needed now
            
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            logMessage("Monitoring started using " + dataSource);
        } catch (Exception ex) {
            showError("Failed to start monitoring: " + ex.getMessage());
        }
    }

    private void stopMonitoring(ActionEvent e) {
        try {
            controller.stopMonitoring();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            logMessage("Monitoring stopped");
        } catch (Exception ex) {
            showError("Error stopping monitoring: " + ex.getMessage());
        }
    }

    private void showAlertDetails() {
        int selectedRow = alertTable.getSelectedRow();
        if (selectedRow >= 0) {
            String message = (String) tableModel.getValueAt(selectedRow, 3);
            JOptionPane.showMessageDialog(this, message, "Alert Details", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void refreshAlertTable() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            List<Message> alerts = controller.getRecentAlerts();
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (Message alert : alerts) {
                String[] parts = alert.getHeader().split(" ");
                String symbol = parts.length > 2 ? parts[2].replace(":", "") : "";
                
                tableModel.addRow(new Object[]{
                    alert.getStatus().toString(),
                    symbol,
                    alert.getTimestamp().format(formatter),
                    alert.getBody()
                });
            }
        });
    }

    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
            logMessage("ERROR: " + message);
        });
    }

    private String getDefaultConfig() {
        return "{\n" +
               "  \"mappings\": [\n" +
               "    {\n" +
               "      \"name\": \"default\",\n" +
               "      \"watchlist\": \"default\",\n" +
               "      \"combination\": \"default\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"combinations\": [\n" +
               "    {\n" +
               "      \"name\": \"default\",\n" +
               "      \"indicators\": [\n" +
               "        {\n" +
               "          \"timeframe\": \"1d\",\n" +
               "          \"shift\": 0,\n" +
               "          \"type\": \"RSI\",\n" +
               "          \"params\": {\"timeFrame\": 14}\n" +
               "        }\n" +
               "      ]\n" +
               "    }\n" +
               "  ],\n" +
               "  \"watchlists\": [\n" +
               "    {\n" +
               "      \"name\": \"default\",\n" +
               "      \"stocks\": [\n" +
               "        {\"symbol\": \"AAPL\", \"name\": \"Apple Inc.\"},\n" +
               "        {\"symbol\": \"MSFT\", \"name\": \"Microsoft Corp.\"},\n" +
               "        {\"symbol\": \"GOOGL\", \"name\": \"Alphabet Inc.\"}\n" +
               "      ]\n" +
               "    }\n" +
               "  ]\n" +
               "}";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                StockMonitorController controller = new StockMonitorController();
                MainFrame frame = new MainFrame(controller);
                frame.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, 
                    "Failed to initialize application: " + e.getMessage(), 
                    "Fatal Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}