package com.quantlabs.stockApp.indicator.management;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

import com.quantlabs.stockApp.IStockDashboard;
import com.quantlabs.stockApp.indicator.management.CustomIndicator;
import com.quantlabs.stockApp.indicator.management.IndicatorsManagementApp;

public class CustomIndicatorsConfigDialog extends JDialog {
    private JPanel mainPanel;
    private JButton saveButton;
    private JButton cancelButton;
    private Map<String, Map<String, JCheckBox>> timeframeCustomIndicatorCheckboxes;
    private Set<CustomIndicator> availableCustomIndicators;
    private Map<String, Set<String>> selectedCombinations;
    private String[] allTimeframes;
    private IndicatorsManagementApp indicatorsManagementApp;
    private IStockDashboard parentDashboard;

    public CustomIndicatorsConfigDialog(JFrame parent, IStockDashboard dashboard, 
                                      String[] timeframes, IndicatorsManagementApp indicatorsApp) {
        super(parent, "Custom Indicators Configuration", true);
        this.parentDashboard = dashboard;
        this.allTimeframes = timeframes;
        this.indicatorsManagementApp = indicatorsApp;
        this.timeframeCustomIndicatorCheckboxes = new HashMap<>();
        this.selectedCombinations = new HashMap<>();
        
        initializeDialog();
        loadAvailableCustomIndicators();
        setupUI();
        pack();
        setLocationRelativeTo(parent);
        setSize(600, 500);
    }

    private void initializeDialog() {
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void loadAvailableCustomIndicators() {
        if (indicatorsManagementApp != null) {
            availableCustomIndicators = indicatorsManagementApp.getGlobalCustomIndicators();
        } else {
            availableCustomIndicators = new HashSet<>();
            JOptionPane.showMessageDialog(this, 
                "Indicators Management App not available. Please open it first.",
                "Warning", 
                JOptionPane.WARNING_MESSAGE);
        }
    }

    private void setupUI() {
        // Main panel with scroll
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel titleLabel = new JLabel("Configure Custom Indicators for Timeframes");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        if (availableCustomIndicators.isEmpty()) {
            JLabel noIndicatorsLabel = new JLabel("No custom indicators available. Please create them in Indicators Management.");
            noIndicatorsLabel.setForeground(Color.RED);
            noIndicatorsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            mainPanel.add(noIndicatorsLabel);
        } else {
            createTimeframeCustomIndicatorsPanels();
        }

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setPreferredSize(new Dimension(550, 350));

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");

        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveConfiguration();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void createTimeframeCustomIndicatorsPanels() {
        for (String timeframe : allTimeframes) {
            JPanel timeframePanel = new JPanel(new BorderLayout());
            timeframePanel.setBorder(BorderFactory.createTitledBorder(timeframe + " - Custom Indicators"));
            
            JPanel indicatorsPanel = new JPanel(new GridLayout(0, 2, 5, 5));
            indicatorsPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            Map<String, JCheckBox> indicatorCheckboxes = new HashMap<>();
            
            for (CustomIndicator customIndicator : availableCustomIndicators) {
                JCheckBox checkbox = new JCheckBox(customIndicator.getDisplayName());
                checkbox.setToolTipText("Type: " + customIndicator.getType() + 
                                      ", Parameters: " + customIndicator.getParameters());
                indicatorsPanel.add(checkbox);
                indicatorCheckboxes.put(customIndicator.getName(), checkbox);
            }

            timeframeCustomIndicatorCheckboxes.put(timeframe, indicatorCheckboxes);
            timeframePanel.add(indicatorsPanel, BorderLayout.CENTER);
            mainPanel.add(timeframePanel);
            mainPanel.add(Box.createVerticalStrut(5));
        }
    }

    private void saveConfiguration() {
        selectedCombinations.clear();
        
        for (String timeframe : allTimeframes) {
            Map<String, JCheckBox> indicatorCheckboxes = timeframeCustomIndicatorCheckboxes.get(timeframe);
            Set<String> selectedIndicators = new HashSet<>();
            
            for (Map.Entry<String, JCheckBox> entry : indicatorCheckboxes.entrySet()) {
                if (entry.getValue().isSelected()) {
                    selectedIndicators.add(entry.getKey());
                }
            }
            
            if (!selectedIndicators.isEmpty()) {
                selectedCombinations.put(timeframe, selectedIndicators);
            }
        }

        // Store in parent dashboard's global variable
        if (parentDashboard != null) {
            parentDashboard.setCustomIndicatorCombinations(selectedCombinations);
            
            // Update table columns to include the new custom indicator combinations
            parentDashboard.updateTableColumnsForCustomIndicators();
        }

        JOptionPane.showMessageDialog(this, 
            "Custom indicators configuration saved successfully!\n" +
            "Timeframes configured: " + selectedCombinations.size(),
            "Success", 
            JOptionPane.INFORMATION_MESSAGE);
        
        dispose();
    }

    public Map<String, Set<String>> getSelectedCombinations() {
        return Collections.unmodifiableMap(selectedCombinations);
    }

    public void setInitialConfiguration(Map<String, Set<String>> initialConfig) {
        if (initialConfig != null) {
            for (String timeframe : allTimeframes) {
                Map<String, JCheckBox> indicatorCheckboxes = timeframeCustomIndicatorCheckboxes.get(timeframe);
                if (indicatorCheckboxes != null && initialConfig.containsKey(timeframe)) {
                    Set<String> selectedIndicators = initialConfig.get(timeframe);
                    for (String indicatorName : selectedIndicators) {
                        JCheckBox checkbox = indicatorCheckboxes.get(indicatorName);
                        if (checkbox != null) {
                            checkbox.setSelected(true);
                        }
                    }
                }
            }
        }
    }
}