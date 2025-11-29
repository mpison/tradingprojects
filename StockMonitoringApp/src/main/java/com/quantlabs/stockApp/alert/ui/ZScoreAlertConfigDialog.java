package com.quantlabs.stockApp.alert.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.json.JSONObject;

import com.quantlabs.stockApp.IStockDashboard;
import com.quantlabs.stockApp.StockDashboardv1_22_5;
import com.quantlabs.stockApp.alert.ZScoreAlertManager;
import com.quantlabs.stockApp.alert.model.ZScoreAlertConfig;
import com.quantlabs.stockApp.indicator.management.IndicatorsManagementApp;
import com.quantlabs.stockApp.indicator.management.StrategyConfig;
import com.quantlabs.stockApp.reports.MonteCarloGraphApp;
import com.quantlabs.stockApp.reports.MonteCarloGraphController;
import com.quantlabs.stockApp.reports.MonteCarloGraphUI;
import com.quantlabs.stockApp.utils.ZScoreColumnHelper;

public class ZScoreAlertConfigDialog extends JDialog {
    private JCheckBox enableZScoreAlertCheckbox;
    private DefaultTableModel configTableModel;
    private JTable configTable;
    private Map<String, ZScoreAlertConfig> alertConfigs;
    private Map<String, MonteCarloGraphApp> monteCarloApps;
    private boolean saved = false;
    private IStockDashboard parentDashboard;
    private ZScoreColumnHelper zScoreColumnHelper;
    private IndicatorsManagementApp indicatorsManagementApp;
    private ZScoreAlertManager zScoreAlertManager;
    
    // Counter for generating unique IDs
    private int nextConfigId = 1;

    public ZScoreAlertConfigDialog(IStockDashboard parent, IndicatorsManagementApp indicatorsManagementApp, ZScoreAlertManager zScoreAlertManager) {
        this.setTitle("Z-Score Alert Configuration");
        this.setModal(true);
        
        this.parentDashboard = parent;
        this.zScoreColumnHelper = parent.getZScoreColumnHelper();
        this.indicatorsManagementApp = indicatorsManagementApp;
        this.zScoreAlertManager = zScoreAlertManager;
        
        this.alertConfigs = new HashMap<>();
        this.monteCarloApps = new HashMap<>();
        initializeUI();
        loadExistingConfig();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setSize(900, 500);
        setLocationRelativeTo(getParent());

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Enable Z-Score Alert panel
        JPanel enablePanel = new JPanel(new BorderLayout());
        enablePanel.setBorder(BorderFactory.createTitledBorder("Monitor Top Z-Score"));
        enableZScoreAlertCheckbox = new JCheckBox("Enable Z-Score Alert");
        enablePanel.add(enableZScoreAlertCheckbox, BorderLayout.WEST);

        // Add listener to enable/disable table
        enableZScoreAlertCheckbox.addActionListener(e -> {
            boolean enabled = enableZScoreAlertCheckbox.isSelected();
            configTable.setEnabled(enabled);
            configTable.getTableHeader().setEnabled(enabled);
            updateTableEditableState();
        });

        // Configuration table with new columns
        String[] columnNames = { "Enable", "Id", "Z-Score Column", "Strategy", "No of Top Symbols", "MonteCarlo Enabled", "is Alarm On" };
        configTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: return Boolean.class;
                    case 1: return String.class;
                    case 2: return String.class;
                    case 3: return String.class;
                    case 4: return String.class;
                    case 5: return Boolean.class;
                    case 6: return Boolean.class;
                    default: return Object.class;
                }
            }
        };

        configTable = new JTable(configTableModel);
        configTable.setRowHeight(25);
        configTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        configTable.getColumnModel().getColumn(1).setPreferredWidth(40);
        configTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        configTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        configTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        configTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        configTable.getColumnModel().getColumn(6).setPreferredWidth(80);

        // Set up combo box editors
        setupComboBoxEditors();

        // Set custom renderer
        configTable.setDefaultRenderer(Object.class, new ZScoreAlertCellRenderer());

        JScrollPane tableScroll = new JScrollPane(configTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Z-Score Alert Configurations"));

        // Button panel with CRUD operations
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Add Configuration");
        JButton deleteButton = new JButton("Delete Selected");
        JButton refreshButton = new JButton("Refresh Columns");
        JButton openAllMonteCarloButton = new JButton("Open All Monte Carlo Windows");
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        addButton.addActionListener(e -> addNewConfiguration());
        deleteButton.addActionListener(e -> deleteSelectedConfigurations());
        refreshButton.addActionListener(e -> refreshZScoreColumns());
        openAllMonteCarloButton.addActionListener(e -> openAllMonteCarloWindows());
        saveButton.addActionListener(e -> saveConfiguration());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(openAllMonteCarloButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        // Layout
        mainPanel.add(enablePanel, BorderLayout.NORTH);
        mainPanel.add(tableScroll, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void setupComboBoxEditors() {
        // Create combo box for Z-Score Column
        JComboBox<String> zScoreColumnComboBox = new JComboBox<>();
        updateZScoreColumnComboBox(zScoreColumnComboBox);
        
        // Create combo box for Strategy
        JComboBox<String> strategyComboBox = new JComboBox<>();
        updateStrategyComboBox(strategyComboBox);
        
        // Set the combo boxes as editors
        configTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(zScoreColumnComboBox));
        configTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(strategyComboBox));
    }

    private void updateZScoreColumnComboBox(JComboBox<String> comboBox) {
        comboBox.removeAllItems();
        List<String> availableColumns = getAvailableZScoreColumns();
        for (String column : availableColumns) {
            comboBox.addItem(column);
        }
        
        // Refresh when dropdown is opened
        comboBox.addActionListener(e -> {
            if (comboBox.isPopupVisible()) {
                String selected = (String) comboBox.getSelectedItem();
                comboBox.removeAllItems();
                List<String> refreshedColumns = getAvailableZScoreColumns();
                for (String column : refreshedColumns) {
                    comboBox.addItem(column);
                }
                if (selected != null && refreshedColumns.contains(selected)) {
                    comboBox.setSelectedItem(selected);
                }
            }
        });
    }

    private void updateStrategyComboBox(JComboBox<String> comboBox) {
        comboBox.removeAllItems();
        comboBox.addItem(""); // Empty option for no strategy filter
        comboBox.addItem("All Available Symbols"); // Special option for all symbols
        
        // Add enabled strategies
        if (indicatorsManagementApp != null) {
            List<StrategyConfig> enabledStrategies = getEnabledStrategies();
            for (StrategyConfig strategy : enabledStrategies) {
                comboBox.addItem(strategy.getName());
            }
        }
    }

    private List<StrategyConfig> getEnabledStrategies() {
        List<StrategyConfig> enabledStrategies = new ArrayList<>();
        if (indicatorsManagementApp != null) {
            List<StrategyConfig> allStrategies = indicatorsManagementApp.getAllStrategies();
            for (StrategyConfig strategy : allStrategies) {
                if (strategy.isEnabled()) {
                    enabledStrategies.add(strategy);
                }
            }
        }
        return enabledStrategies;
    }

    private void updateTableEditableState() {
        boolean enabled = enableZScoreAlertCheckbox.isSelected();
        configTableModel = new DefaultTableModel(
                new String[] { "Enable", "Id", "Z-Score Column", "Strategy", "No of Top Symbols", "MonteCarlo Enabled", "is Alarm On" }, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: return Boolean.class;
                    case 1: return String.class;
                    case 2: return String.class;
                    case 3: return String.class;
                    case 4: return String.class;
                    case 5: return Boolean.class;
                    case 6: return Boolean.class;
                    default: return Object.class;
                }
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return enabled && column != 1; // Don't allow editing ID
            }
        };

        populateTable();
        configTable.setModel(configTableModel);
        setupComboBoxEditors();
    }

    private void addNewConfiguration() {
        String configId = "config_" + nextConfigId++;
        
        List<String> availableColumns = getAvailableZScoreColumns();
        String defaultColumn = availableColumns.isEmpty() ? "" : availableColumns.get(0);
        
        Object[] rowData = {
            false,                    // Enable
            configId,                 // Id
            defaultColumn,            // Z-Score Column
            "",                       // Strategy
            "5",                      // No of Top Symbols
            false,                    // MonteCarlo Enabled
            false                     // is Alarm On
        };
        
        configTableModel.addRow(rowData);
        
        // Scroll to new row
        int newRow = configTableModel.getRowCount() - 1;
        configTable.setRowSelectionInterval(newRow, newRow);
        configTable.scrollRectToVisible(configTable.getCellRect(newRow, 0, true));
        
        parentDashboard.logToConsole("Added new Z-Score alert configuration: " + configId);
    }

    private void deleteSelectedConfigurations() {
        int[] selectedRows = configTable.getSelectedRows();
        if (selectedRows.length == 0) {
            showWarning("Please select one or more configurations to delete.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete " + selectedRows.length + " configuration(s)?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // Delete from bottom to top
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                int row = selectedRows[i];
                String configId = (String) configTableModel.getValueAt(row, 1);
                configTableModel.removeRow(row);
                
                // Close associated Monte Carlo window
                closeMonteCarloWindow(configId);
                
                parentDashboard.logToConsole("Deleted Z-Score alert configuration: " + configId);
            }
        }
    }

    private void refreshZScoreColumns() {
        setupComboBoxEditors();
        
        // Update configurations with invalid Z-Score columns
        List<String> availableColumns = getAvailableZScoreColumns();
        for (int i = 0; i < configTableModel.getRowCount(); i++) {
            String currentColumn = (String) configTableModel.getValueAt(i, 2);
            if (currentColumn != null && !availableColumns.contains(currentColumn)) {
                String newColumn = availableColumns.isEmpty() ? "" : availableColumns.get(0);
                configTableModel.setValueAt(newColumn, i, 2);
                parentDashboard.logToConsole("Updated invalid Z-Score column for configuration: " + configTableModel.getValueAt(i, 1));
            }
        }
        
        parentDashboard.logToConsole("Refreshed Z-Score columns. Available: " + availableColumns.size() + " columns");
    }

    private List<String> getAvailableZScoreColumns() {
        List<String> zScoreColumns = new ArrayList<>();
        if (parentDashboard.getTableModel() != null) {
            for (int i = 0; i < parentDashboard.getTableModel().getColumnCount(); i++) {
                String colName = parentDashboard.getTableModel().getColumnName(i);
                if (zScoreColumnHelper.isZScoreColumn(colName) && zScoreColumnHelper.isRankColumn(colName)) {
                    zScoreColumns.add(colName);
                }
            }
        }
        return zScoreColumns;
    }

    private void loadExistingConfig() {
        try {
            if (parentDashboard.getZScoreAlertManager() != null) {
                ZScoreAlertManager manager = parentDashboard.getZScoreAlertManager();
                enableZScoreAlertCheckbox.setSelected(manager.isEnabled());

                Map<String, ZScoreAlertConfig> managerConfigs = manager.getAlertConfigs();
                if (managerConfigs != null && !managerConfigs.isEmpty()) {
                    alertConfigs.clear();
                    alertConfigs.putAll(managerConfigs);
                } else {
                    loadFromPreferences();
                }
            } else {
                loadFromPreferences();
            }
        } catch (Exception e) {
            parentDashboard.logToConsole("Error loading from main dashboard: " + e.getMessage());
            loadFromPreferences();
        }

        populateTable();
        updateTableEditableState();
    }

    private void loadFromPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_5.class);
        boolean enabled = prefs.getBoolean("zscoreAlertEnabled", false);
        enableZScoreAlertCheckbox.setSelected(enabled);

        alertConfigs.clear();

        String configJson = prefs.get("zscoreAlertConfigs", "{}");
        try {
            if (!configJson.equals("{}")) {
                JSONObject configObj = new JSONObject(configJson);
                for (String key : configObj.keySet()) {
                    JSONObject config = configObj.getJSONObject(key);
                    
                    // Create new ZScoreAlertConfig with all fields
                    ZScoreAlertConfig alertConfig = new ZScoreAlertConfig(
                        config.getBoolean("enabled"),
                        config.getInt("monitorRange"),
                        config.optString("zScoreColumn", ""),
                        config.optString("strategy", "")
                    );
                    
                    alertConfig.setMonteCarloEnabled(config.optBoolean("monteCarloEnabled", false));
                    alertConfig.setAlarmOn(config.optBoolean("alarmOn", false));
                    alertConfig.setConfigId(config.optString("configId", ""));
                    
                    alertConfigs.put(key, alertConfig);
                }
            }
        } catch (Exception e) {
            parentDashboard.logToConsole("Error loading from preferences: " + e.getMessage());
        }
    }

    private void populateTable() {
        configTableModel.setRowCount(0);
        nextConfigId = 1;

        List<String> availableColumns = getAvailableZScoreColumns();
        
        if (alertConfigs.isEmpty()) {
            parentDashboard.logToConsole("No existing Z-Score alert configurations found");
            return;
        }

        // Convert alertConfigs to table rows
        for (Map.Entry<String, ZScoreAlertConfig> entry : alertConfigs.entrySet()) {
            ZScoreAlertConfig config = entry.getValue();
            
            // Use the config's ID or generate a new one
            String configId = config.getConfigId();
            if (configId == null || configId.isEmpty()) {
                configId = "config_" + nextConfigId++;
                config.setConfigId(configId);
            }
            
            String columnName = config.getZScoreColumn();
            String strategy = config.getStrategy();

            // Validate Z-Score column
            if (!availableColumns.contains(columnName)) {
                if (!availableColumns.isEmpty()) {
                    columnName = availableColumns.get(0);
                    parentDashboard.logToConsole("Updated missing Z-Score column for configuration: " + configId);
                } else {
                    parentDashboard.logToConsole("Skipping configuration " + configId + ": No Z-Score columns available");
                    continue;
                }
            }

            Object[] rowData = {
                config.isEnabled(),
                configId,
                columnName,
                strategy,
                String.valueOf(config.getMonitorRange()),
                config.isMonteCarloEnabled(),
                config.isAlarmOn()
            };
            
            configTableModel.addRow(rowData);
        }
        
        parentDashboard.logToConsole("Loaded " + configTableModel.getRowCount() + " Z-Score alert configurations");
    }

    private void saveConfiguration() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_5.class);
            prefs.putBoolean("zscoreAlertEnabled", enableZScoreAlertCheckbox.isSelected());

            JSONObject configObj = new JSONObject();
            alertConfigs.clear();

            for (int i = 0; i < configTableModel.getRowCount(); i++) {
                String configId = (String) configTableModel.getValueAt(i, 1);
                Boolean enabled = (Boolean) configTableModel.getValueAt(i, 0);
                String colName = (String) configTableModel.getValueAt(i, 2);
                String strategy = (String) configTableModel.getValueAt(i, 3);
                String rangeStr = (String) configTableModel.getValueAt(i, 4);
                Boolean monteCarloEnabled = (Boolean) configTableModel.getValueAt(i, 5);
                Boolean alarmOn = (Boolean) configTableModel.getValueAt(i, 6);

                if (colName != null && !colName.trim().isEmpty()) {
                    try {
                        int range = Integer.parseInt(rangeStr);
                        
                        // Create new ZScoreAlertConfig with all fields
                        ZScoreAlertConfig config = new ZScoreAlertConfig(enabled, range, colName, strategy);
                        config.setEnabled(enabled);
                        config.setMonitorRange(range);
                        config.setZScoreColumn(colName);
                        config.setStrategy(strategy);
                        config.setMonteCarloEnabled(monteCarloEnabled);
                        config.setAlarmOn(enabled);
                        config.setConfigId(configId);
                        //config.setPreviousTopSymbols(rangeStr);                        
                        
                        // Use unique key for storage
                        String configKey = config.getUniqueKey();
                        alertConfigs.put(configKey, config);

                        // Save to JSON with all fields
                        JSONObject configJson = new JSONObject();
                        configJson.put("enabled", enabled);
                        configJson.put("monitorRange", range);
                        configJson.put("zScoreColumn", colName);
                        configJson.put("strategy", strategy != null ? strategy : "");
                        configJson.put("monteCarloEnabled", monteCarloEnabled);
                        configJson.put("alarmOn", alarmOn);
                        configJson.put("configId", configId);
                        configObj.put(configKey, configJson);

                        handleMonteCarloWindowState(configKey, config, monteCarloEnabled, range, strategy);

                        parentDashboard.logToConsole("Saved configuration: " + configKey + 
                            " (Top " + range + " symbols, Strategy: " + (strategy.isEmpty() ? "None" : strategy) + ")");

                    } catch (NumberFormatException e) {
                        parentDashboard.logToConsole("Invalid range for " + colName + ": " + rangeStr);
                    }
                } else {
                    parentDashboard.logToConsole("Skipping configuration with empty Z-Score column: " + configId);
                }
            }

            prefs.put("zscoreAlertConfigs", configObj.toString());

            // Update the main dashboard's ZScoreAlertManager
            if (parentDashboard.getZScoreAlertManager() != null) {
                parentDashboard.getZScoreAlertManager().setEnabled(enableZScoreAlertCheckbox.isSelected());
                parentDashboard.getZScoreAlertManager().setAlertConfigs(alertConfigs);
            }

            saved = true;
            parentDashboard.logToConsole("Z-Score alert configuration saved successfully: " + alertConfigs.size() + " configurations");
            dispose();

        } catch (Exception e) {
            parentDashboard.logToConsole("Error saving Z-Score alert configuration: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error saving configuration: " + e.getMessage(), "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // The rest of the methods remain the same...
    private void handleMonteCarloWindowState(String configKey, ZScoreAlertConfig config, boolean monteCarloEnabled, int monitorRange, String strategy) {
        if (monteCarloEnabled) {
            openOrUpdateMonteCarloWindow(configKey, config, monitorRange, strategy);
        } else {
            closeMonteCarloWindow(configKey);
        }
    }

    @SuppressWarnings("null")
	private void openOrUpdateMonteCarloWindow(String configKey, ZScoreAlertConfig zScoreconfig, int monitorRange, String strategy) {
        String columnName = configKey.split("\\|")[0];
        List<String> topSymbols = zScoreAlertManager.getCurrentTopSymbolsWithStrategy(columnName, monitorRange, strategy);//getCurrentTopSymbols(columnName, monitorRange, strategy);

        if (topSymbols.isEmpty()) {
            parentDashboard.logToConsole("No symbols found for Monte Carlo: " + configKey);
            return; 
        }

        SwingUtilities.invokeLater(() -> {
        	
        	Map<String, MonteCarloGraphApp> monteCarloApps = zScoreAlertManager.getMonteCarloApps();
        	
        	        
        	String primarySymbol = indicatorsManagementApp.getGlobalWatchlists().get(parentDashboard.getCurrentWatchlistName()).getPrimarySymbol();
        	
        	Set<String> primarySymbolSet = new HashSet<String>();
        	
        	
        	if(!primarySymbol.isEmpty())
        		primarySymbolSet.add(primarySymbol);
        	
            if (monteCarloApps.containsKey(configKey)) {
                MonteCarloGraphApp existingApp = monteCarloApps.get(configKey);
                
                String apptitle = getMonteCarloTitle(parentDashboard, columnName, strategy);
                
                if(existingApp.checkIfClosed()) {
                	existingApp.showGraph();
                }
                
                if(!existingApp.getTitle().equals(apptitle)){
                	existingApp.updateTitle(apptitle);
                }
                
                if(primarySymbolSet.size() > 0)
                	existingApp.setPrimarySymbols(primarySymbolSet);
                
                if(topSymbols.size() > 0)
                	existingApp.setTopList(new HashSet<>(topSymbols));
                
                if (existingApp.isInitialized()) {                	
                    existingApp.updateSymbols(topSymbols);
                    monteCarloApps.put(configKey, existingApp);                    
                    parentDashboard.logToConsole("Updated Monte Carlo window for: " + configKey);
                } else {
                    existingApp.initialize(topSymbols, apptitle);
                }
                
                existingApp.toggleTopList();
                
                if(zScoreconfig.isAlarmOn()) {
                	existingApp.frameToFront();
                }
                
                if(!parentDashboard.getCurrentTimeRadio().isSelected()) {            		
            		ZonedDateTime start = parentDashboard.getStartDateTime();
					ZonedDateTime end = parentDashboard.getEndDateTime();
					
					existingApp.updateCustomTimeRange(start, end, "1Min");
            	}  
                
            } else {
                try {
                	
                	MonteCarloGraphApp newApp = null;
                	
                	if(parentDashboard.getCurrentTimeRadio().isSelected()) {
                		newApp = new MonteCarloGraphApp(topSymbols, getMonteCarloTitle(parentDashboard, columnName, strategy));	
                	}else {
                		ZonedDateTime start = parentDashboard.getStartDateTime();
						ZonedDateTime end = parentDashboard.getEndDateTime();
						newApp = new MonteCarloGraphApp(topSymbols, getMonteCarloTitle(parentDashboard, columnName, strategy), start, end, "1Min");
                	}                    
                    
                    if(primarySymbolSet.size() > 0)
                    	newApp.setPrimarySymbols(primarySymbolSet);
                    
                    if(topSymbols.size() > 0)
                    	newApp.setTopList(new HashSet<>(topSymbols));
                                        
                    monteCarloApps.put(configKey, newApp);
                    
                    // Wait for the app to initialize and then attach window listener
                    waitForInitializationAndAttachListener(newApp, configKey);
                    
                    parentDashboard.logToConsole("Opened Monte Carlo window for: " + configKey);
                } catch (Exception e) {
                    parentDashboard.logToConsole("Error opening Monte Carlo window for " + configKey + ": " + e.getMessage());
                }
            }
            
            zScoreAlertManager.setMonteCarloApps(monteCarloApps);
        });
    }
    
    /**
     * Wait for Monte Carlo app to initialize and then attach window listener
     */
    private void waitForInitializationAndAttachListener(MonteCarloGraphApp monteCarloApp, String configKey) {
        javax.swing.Timer timer = new javax.swing.Timer(3000, e -> {
            if (monteCarloApp.isInitialized() && monteCarloApp.getController() != null) {
                // App is initialized, now attach the window listener
                attachWindowListenerToInitializedApp(monteCarloApp, configKey);
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });
        timer.start();
    }

    /**
     * Attach window listener to an initialized Monte Carlo app
     */
    private void attachWindowListenerToInitializedApp(MonteCarloGraphApp monteCarloApp, String configKey) {
        try {
            MonteCarloGraphController controller = monteCarloApp.getController();
            if (controller != null && controller.getGraphUI() != null) {
                MonteCarloGraphUI graphUI = controller.getGraphUI();
                
                if (graphUI.getFrame() != null) {
                    graphUI.getFrame().addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override
                        public void windowClosing(java.awt.event.WindowEvent e) {
                            handleMonteCarloWindowClosing(configKey);
                        }
                        
                        @Override
                        public void windowClosed(java.awt.event.WindowEvent e) {
                            handleMonteCarloWindowClosed(configKey);
                        }
                    });
                    
                    parentDashboard.logToConsole("Successfully attached window listener to Monte Carlo frame for: " + configKey);
                } else {
                    parentDashboard.logToConsole("Frame not available yet for: " + configKey);
                }
            } else {
                parentDashboard.logToConsole("Graph UI not available for: " + configKey);
            }
        } catch (Exception e) {
            parentDashboard.logToConsole("Error attaching window listener to initialized app: " + e.getMessage());
        }
    }
    
    /**
     * Handle Monte Carlo window closing event
     */
    private void handleMonteCarloWindowClosing(String configKey) {
        parentDashboard.logToConsole("Monte Carlo window closing: " + configKey);
        // Don't remove from map yet - wait for windowClosed
    }

    /**
     * Handle Monte Carlo window closed event
     */
    private void handleMonteCarloWindowClosed(String configKey) {
        parentDashboard.logToConsole("Monte Carlo window closed: " + configKey);
        
        Map<String, ZScoreAlertConfig> monteCarloApps = zScoreAlertManager.getAlertConfigs();
        
        // Remove from the apps map
        if (monteCarloApps.containsKey(configKey)) {
            monteCarloApps.remove(configKey);
            parentDashboard.logToConsole("Removed Monte Carlo app from tracking: " + configKey);
        }
        
        zScoreAlertManager.setAlertConfigs(monteCarloApps);
        
        // Update the configuration to reflect that Monte Carlo is no longer enabled
        updateConfigMonteCarloState(configKey, false);
    }

    /**
     * Update configuration Monte Carlo state when window is closed
     */
    private void updateConfigMonteCarloState(String configKey, boolean enabled) {
        try {
            // Find the row in the table that corresponds to this configKey
            for (int i = 0; i < configTableModel.getRowCount(); i++) {
                String currentConfigId = (String) configTableModel.getValueAt(i, 1);
                String currentColName = (String) configTableModel.getValueAt(i, 2);
                String currentStrategy = (String) configTableModel.getValueAt(i, 3);
                
                String currentKey = currentColName + (currentStrategy != null && !currentStrategy.isEmpty() ? "|" + currentStrategy : "");
                
                if (currentKey.equals(configKey)) {
                    // Update the Monte Carlo Enabled checkbox in the table
                    configTableModel.setValueAt(enabled, i, 5); // Column 5 is MonteCarlo Enabled
                    parentDashboard.logToConsole("Updated table: Monte Carlo disabled for " + configKey);
                    break;
                }
            }
            
            // Also update the alertConfigs map
            if (alertConfigs.containsKey(configKey)) {
                ZScoreAlertConfig config = alertConfigs.get(configKey);
                config.setMonteCarloEnabled(enabled);
                parentDashboard.logToConsole("Updated config: Monte Carlo disabled for " + configKey);
            }
            
        } catch (Exception e) {
            parentDashboard.logToConsole("Error updating config Monte Carlo state: " + e.getMessage());
        }
    }

    public static String getMonteCarloTitle(IStockDashboard dashboard, String zColumnName, String strategy) {
        if (strategy != null && !strategy.isEmpty()) {
            return strategy + " - " + zColumnName + " - WatchList:" + dashboard.getCurrentWatchlistName() + " - " + "Monte Carlo";
        } else {
            return zColumnName + " - " + dashboard.getCurrentWatchlistName() + " - " + "Monte Carlo" ;
        }
    }

    private void closeMonteCarloWindow(String configKey) {
        if (monteCarloApps.containsKey(configKey)) {
            MonteCarloGraphApp app = monteCarloApps.get(configKey);
            app.close();
            monteCarloApps.remove(configKey);
            parentDashboard.logToConsole("Closed Monte Carlo window for: " + configKey);
        }
    }

    private java.util.List<String> getCurrentTopSymbols(String columnName, int monitorRange, String strategy) {
        java.util.List<String> symbols = new java.util.ArrayList<>();

        try {
            // Find the column index in the table
            int columnIndex = -1;
            for (int i = 0; i < parentDashboard.getTableModel().getColumnCount(); i++) {
                if (parentDashboard.getTableModel().getColumnName(i).equals(columnName)) {
                    columnIndex = i;
                    break;
                }
            }

            if (columnIndex == -1) {
                parentDashboard.logToConsole("Z-Score column not found: " + columnName);
                return symbols;
            }

            // Apply strategy filter if specified
            Set<String> filteredSymbols = null;
            if (strategy != null && !strategy.isEmpty() && !strategy.equals("All Available Symbols")) {
                filteredSymbols = getSymbolsForStrategy(strategy);
                parentDashboard.logToConsole("Applying strategy filter: " + strategy + " (" + 
                    (filteredSymbols != null ? filteredSymbols.size() : 0) + " symbols)");
            }

            // Collect symbols and their ranks with proper ordering
            List<SymbolRank> symbolRanks = new ArrayList<>();
            for (int row = 0; row < parentDashboard.getTableModel().getRowCount(); row++) {
                Object rankObj = parentDashboard.getTableModel().getValueAt(row, columnIndex);
                String symbol = (String) parentDashboard.getTableModel().getValueAt(row, 1); // Symbol column is typically at index 1

                if (rankObj instanceof Number && symbol != null) {
                    // Apply strategy filter
                    if (filteredSymbols != null && !filteredSymbols.contains(symbol)) {
                        continue;
                    }
                    
                    int rank = ((Number) rankObj).intValue();
                    symbolRanks.add(new SymbolRank(symbol, rank));
                }
            }

            // Sort by rank (ascending - lower rank number is better)
            symbolRanks.sort(Comparator.comparingInt(SymbolRank::getRank));

            // Get top N symbols in order
            for (int i = 0; i < Math.min(monitorRange, symbolRanks.size()); i++) {
                symbols.add(symbolRanks.get(i).getSymbol());
            }

            parentDashboard.logToConsole("Found " + symbols.size() + " top symbols for " + columnName + 
                " with strategy: " + (strategy != null && !strategy.isEmpty() ? strategy : "None") +
                " (requested: " + monitorRange + ")");

        } catch (Exception e) {
            parentDashboard.logToConsole("Error getting top symbols for Monte Carlo: " + e.getMessage());
            e.printStackTrace();
        }

        return symbols;
    }

    private Set<String> getSymbolsForStrategy(String strategyName) {
        Set<String> symbols = new HashSet<>();
        
        if (indicatorsManagementApp != null) {
            StrategyConfig strategy = indicatorsManagementApp.getStrategyConfigByName(strategyName);
            if (strategy != null) {
                if (strategy.hasWatchlist()) {
                    // Get symbols from watchlist
                    String watchlistName = strategy.getWatchlistName();
                    symbols = indicatorsManagementApp.getWatchlistSymbols(watchlistName);
                    parentDashboard.logToConsole("Using watchlist '" + watchlistName + "' for strategy '" + strategyName + "': " + symbols.size() + " symbols");
                } else if (strategy.isSymbolExclusive()) {
                    // Get exclusive symbols
                    symbols = strategy.getExclusiveSymbols();
                    parentDashboard.logToConsole("Using exclusive symbols for strategy '" + strategyName + "': " + symbols.size() + " symbols");
                } else {
                    // Strategy has no specific symbol restrictions - return empty set to indicate all symbols
                    parentDashboard.logToConsole("Strategy '" + strategyName + "' has no specific symbol restrictions - using all available symbols");
                    // Return empty set to indicate no filtering needed
                }
                
                // Log strategy details for debugging
                parentDashboard.logToConsole("Strategy '" + strategyName + "' details: " +
                    "enabled=" + strategy.isEnabled() + ", " +
                    "watchlist=" + strategy.getWatchlistName() + ", " +
                    "symbolExclusive=" + strategy.isSymbolExclusive() + ", " +
                    "exclusiveSymbolsCount=" + strategy.getExclusiveSymbols().size());
            } else {
                parentDashboard.logToConsole("Strategy not found: " + strategyName);
            }
        } else {
            parentDashboard.logToConsole("IndicatorsManagementApp not available for strategy filtering");
        }
        
        return symbols;
    }

    private void openAllMonteCarloWindows() { 
        int openedCount = 0;
        for (int i = 0; i < configTableModel.getRowCount(); i++) {
            String configId = (String) configTableModel.getValueAt(i, 1);
            String colName = (String) configTableModel.getValueAt(i, 2);
            String strategy = (String) configTableModel.getValueAt(i, 3);
            Boolean monteCarloEnabled = (Boolean) configTableModel.getValueAt(i, 5);

            if (monteCarloEnabled && colName != null && !colName.trim().isEmpty()) {
                try {
                    String rangeStr = (String) configTableModel.getValueAt(i, 4);
                    int range = Integer.parseInt(rangeStr);
                    String configKey = colName + (strategy != null && !strategy.isEmpty() ? "|" + strategy : "");
                    ZScoreAlertConfig zscoreConfigy = alertConfigs.get(configKey);
                    
                    openOrUpdateMonteCarloWindow(configKey, zscoreConfigy, range, strategy);
                    openedCount++;
                } catch (NumberFormatException e) {
                    parentDashboard.logToConsole("Invalid range for " + colName);
                }
            }
        }
        parentDashboard.logToConsole("Opened " + openedCount + " Monte Carlo windows");
    }

    public void updateMonteCarloWindows(String columnName, java.util.List<String> currentTopSymbols) {
        for (Map.Entry<String, ZScoreAlertConfig> entry : alertConfigs.entrySet()) {
            if (entry.getKey().startsWith(columnName)) {
                ZScoreAlertConfig config = entry.getValue();
                if (config.isMonteCarloEnabled() && monteCarloApps.containsKey(entry.getKey())) {
                    MonteCarloGraphApp app = monteCarloApps.get(entry.getKey());
                    if (app.isInitialized()) {
                        app.setTopList(new HashSet<>(currentTopSymbols));
                        app.updateSymbols(currentTopSymbols);
                        parentDashboard.logToConsole("Updated Monte Carlo symbols for: " + entry.getKey());
                    }
                }
            }
        }
    }

    public Map<String, ZScoreAlertConfig> getAlertConfigs() {
        return alertConfigs;
    }

    public boolean isZScoreAlertEnabled() {
        return enableZScoreAlertCheckbox.isSelected();
    }

    public boolean isSaved() {
        return saved;
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    // Custom cell renderer
    private class ZScoreAlertCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                     int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!enableZScoreAlertCheckbox.isSelected() && column != 1 && column != 2) {
                c.setBackground(Color.LIGHT_GRAY);
                c.setForeground(Color.DARK_GRAY);
            } else {
                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                } else {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }
            }

            return c;
        }
    }

    // Helper class for symbol ranking
    private static class SymbolRank {
        private final String symbol;
        private final int rank;

        public SymbolRank(String symbol, int rank) {
            this.symbol = symbol;
            this.rank = rank;
        }

        public String getSymbol() {
            return symbol;
        }

        public int getRank() {
            return rank;
        }
    }
}