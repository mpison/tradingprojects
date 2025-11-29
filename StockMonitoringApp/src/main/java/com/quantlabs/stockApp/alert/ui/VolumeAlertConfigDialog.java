package com.quantlabs.stockApp.alert.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.util.Map;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.quantlabs.stockApp.alert.model.VolumeAlertConfig;
import com.quantlabs.stockApp.alert.model.VolumeAlertTimeframeConfig;

public class VolumeAlertConfigDialog extends JDialog {
    private VolumeAlertConfig config;
    private boolean saved = false;
    private DefaultTableModel tableModel;
    private JCheckBox enableVolumeAlertCheckbox;
    private JCheckBox enableVolume20MACheckbox;
    private JCheckBox onlyBullishSymbolsCheckbox;
    private JTable timeframeTable;
    
    // Define constants locally since they don't exist in VolumeAlertConfig
    private static final double MIN_PERCENTAGE = 0.0;
    private static final double MAX_PERCENTAGE = 1000.0;
    private static final double DEFAULT_PERCENTAGE = 100.0;
    
    public VolumeAlertConfigDialog(Frame parent, VolumeAlertConfig config, String[] timeframes) {
        super(parent, "Configure Volume Alert", true);
        this.config = createWorkingCopy(config); // Use manual copy instead of copy() method
        initializeUI(timeframes);
        pack();
        setLocationRelativeTo(parent);
    }
    
    // Manual copy creation
    private VolumeAlertConfig createWorkingCopy(VolumeAlertConfig original) {
        VolumeAlertConfig copy = new VolumeAlertConfig();
        copy.setEnabled(original.isEnabled());
        copy.setVolume20MAEnabled(original.isVolume20MAEnabled());
        copy.setOnlyBullishSymbols(original.isOnlyBullishSymbols());
        
        // Manually copy timeframe configs
        if (original.getTimeframeConfigs() != null) {
            for (Map.Entry<String, VolumeAlertTimeframeConfig> entry : original.getTimeframeConfigs().entrySet()) {
                if (entry.getValue() != null) {
                    VolumeAlertTimeframeConfig timeframeCopy = new VolumeAlertTimeframeConfig(
                        entry.getValue().isEnabled(), 
                        entry.getValue().getPercentage()
                    );
                    copy.setTimeframeConfig(entry.getKey(), timeframeCopy);
                }
            }
        }
        
        return copy;
    }
    
    private void initializeUI(String[] timeframes) {
        setLayout(new BorderLayout(10, 10));
        setMinimumSize(new Dimension(600, 550));
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JPanel volumeAlertConfigPanel = createVolumeAlertConfigPanel();
        JPanel volume20MAPanel = createVolume20MAPanel(timeframes);
        JPanel buttonPanel = createButtonPanel();
        
        mainPanel.add(volumeAlertConfigPanel, BorderLayout.NORTH);
        mainPanel.add(volume20MAPanel, BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createVolumeAlertConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Volume Alert Settings"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Master enable checkbox
        enableVolumeAlertCheckbox = new JCheckBox("Enable Volume Alert System", config.isEnabled());
        enableVolumeAlertCheckbox.setToolTipText("Master switch for volume-based alerts");
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(enableVolumeAlertCheckbox, gbc);
        
        // Only Bullish Symbols checkbox
        onlyBullishSymbolsCheckbox = new JCheckBox("Only Check Bullish Symbols", config.isOnlyBullishSymbols());
        onlyBullishSymbolsCheckbox.setToolTipText("If checked, only analyze symbols that passed bullish technical analysis. If unchecked, analyze all symbols in watchlist.");
        
        gbc.gridy = 1;
        panel.add(onlyBullishSymbolsCheckbox, gbc);
        
        // Add listeners
        enableVolumeAlertCheckbox.addActionListener(e -> updateComponentStates());
        
        return panel;
    }
    
    private JPanel createVolume20MAPanel(String[] timeframes) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Volume Spike Detection (vs 20-period MA)"));
        
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        enableVolume20MACheckbox = new JCheckBox("Enable Volume Spike Alerts", config.isVolume20MAEnabled());
        enableVolume20MACheckbox.setToolTipText("Alert when volume exceeds 20-period moving average by configured percentage");
        enableVolume20MACheckbox.addActionListener(e -> updateComponentStates());
        
        headerPanel.add(enableVolume20MACheckbox);
        
        // Create table
        String[] columnNames = {"Timeframe", "Enabled", "Volume % Above MA"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: return String.class;
                    case 1: return Boolean.class;
                    case 2: return Double.class;
                    default: return Object.class;
                }
            }
            
            @Override
            public boolean isCellEditable(int row, int column) {
                return isTableEditable() && column > 0;
            }
        };
        
        // Populate table
        for (String timeframe : timeframes) {
            VolumeAlertTimeframeConfig timeframeConfig = config.getTimeframeConfig(timeframe);
            if (timeframeConfig == null) {
                timeframeConfig = new VolumeAlertTimeframeConfig(false, DEFAULT_PERCENTAGE);
                config.setTimeframeConfig(timeframe, timeframeConfig);
            }
            tableModel.addRow(new Object[]{
                timeframe, 
                timeframeConfig.isEnabled(), 
                timeframeConfig.getPercentage()
            });
        }
        
        timeframeTable = new JTable(tableModel);
        timeframeTable.setRowHeight(25);
        timeframeTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        timeframeTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        timeframeTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        timeframeTable.getColumnModel().getColumn(2).setCellRenderer(new PercentageRenderer());
        
        JScrollPane tableScrollPane = new JScrollPane(timeframeTable);
        tableScrollPane.setPreferredSize(new Dimension(400, 200));
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        
        JLabel infoLabel = new JLabel("Percentage: 100 = 100% of 20MA (equal volume), 150 = 150% of 20MA (1.5x volume)");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC, 11f));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
        panel.add(infoLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        JButton saveButton = new JButton("Save Configuration");
        JButton cancelButton = new JButton("Cancel");
        JButton defaultsButton = new JButton("Reset to Defaults");
        
        saveButton.addActionListener(this::saveConfiguration);
        cancelButton.addActionListener(e -> cancel());
        defaultsButton.addActionListener(e -> resetToDefaults());
        
        saveButton.setBackground(new Color(70, 130, 180));
        saveButton.setForeground(Color.WHITE);
        defaultsButton.setBackground(new Color(240, 240, 240));
        
        panel.add(defaultsButton);
        panel.add(cancelButton);
        panel.add(saveButton);
        
        return panel;
    }
    
    private void updateComponentStates() {
        boolean masterEnabled = enableVolumeAlertCheckbox.isSelected();
        boolean volume20MAEnabled = enableVolume20MACheckbox.isSelected();
        
        onlyBullishSymbolsCheckbox.setEnabled(masterEnabled);
        enableVolume20MACheckbox.setEnabled(masterEnabled);
        timeframeTable.setEnabled(masterEnabled && volume20MAEnabled);
        
        if (tableModel != null) {
            tableModel.fireTableStructureChanged();
        }
    }
    
    private boolean isTableEditable() {
        return enableVolumeAlertCheckbox.isSelected() && 
               enableVolume20MACheckbox.isSelected();
    }
    
    private void saveConfiguration(ActionEvent e) {
        if (!validateInputs()) {
            return;
        }
        
        // Update config from UI
        config.setEnabled(enableVolumeAlertCheckbox.isSelected());
        config.setOnlyBullishSymbols(onlyBullishSymbolsCheckbox.isSelected());
        config.setVolume20MAEnabled(enableVolume20MACheckbox.isSelected());
        
        // Update timeframe configurations
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String timeframe = (String) tableModel.getValueAt(i, 0);
            boolean enabled = (Boolean) tableModel.getValueAt(i, 1);
            double percentage = (Double) tableModel.getValueAt(i, 2);
            
            VolumeAlertTimeframeConfig timeframeConfig = new VolumeAlertTimeframeConfig(enabled, percentage);
            config.setTimeframeConfig(timeframe, timeframeConfig);
        }
        
        saved = true;
        dispose();
    }
    
    private boolean validateInputs() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            double percentage = (Double) tableModel.getValueAt(i, 2);
            if (percentage < MIN_PERCENTAGE || percentage > MAX_PERCENTAGE) {
                JOptionPane.showMessageDialog(this,
                    String.format("Invalid percentage for %s: %.1f%%\nMust be between %.1f%% and %.1f%%",
                        tableModel.getValueAt(i, 0), percentage,
                        MIN_PERCENTAGE, MAX_PERCENTAGE),
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                timeframeTable.setRowSelectionInterval(i, i);
                timeframeTable.setColumnSelectionInterval(2, 2);
                return false;
            }
        }
        return true;
    }
    
    private void resetToDefaults() {
        int result = JOptionPane.showConfirmDialog(this,
            "Reset all volume alert settings to defaults?",
            "Confirm Reset", JOptionPane.YES_NO_OPTION);
            
        if (result == JOptionPane.YES_OPTION) {
            // Create a new default config
            config = new VolumeAlertConfig();
            config.setOnlyBullishSymbols(true); // Set the new field to default
            refreshUIFromConfig();
        }
    }
    
    private void refreshUIFromConfig() {
        enableVolumeAlertCheckbox.setSelected(config.isEnabled());
        onlyBullishSymbolsCheckbox.setSelected(config.isOnlyBullishSymbols());
        enableVolume20MACheckbox.setSelected(config.isVolume20MAEnabled());
        
        // Refresh table
        tableModel.setRowCount(0);
        for (String timeframe : new String[]{"1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min"}) {
            VolumeAlertTimeframeConfig timeframeConfig = config.getTimeframeConfig(timeframe);
            if (timeframeConfig == null) {
                timeframeConfig = new VolumeAlertTimeframeConfig(false, DEFAULT_PERCENTAGE);
                config.setTimeframeConfig(timeframe, timeframeConfig);
            }
            tableModel.addRow(new Object[]{
                timeframe, 
                timeframeConfig.isEnabled(), 
                timeframeConfig.getPercentage()
            });
        }
        
        updateComponentStates();
    }
    
    private void cancel() {
        saved = false;
        dispose();
    }
    
    private static class PercentageRenderer extends DefaultTableCellRenderer {
        private final DecimalFormat formatter = new DecimalFormat("#,##0.0'%'");
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (value instanceof Double) {
                setText(formatter.format((Double) value));
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
            
            return this;
        }
    }
    
    public boolean isSaved() {
        return saved;
    }
    
    public VolumeAlertConfig getConfig() {
        return saved ? config : null;
    }
}