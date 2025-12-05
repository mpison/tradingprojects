// [file name]: MonteCarloConfig.java
package com.quantlabs.stockApp.graph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration dialog for Monte Carlo graph column selection
 */
public class MonteCarloConfig extends JDialog {
    private JList<String> availableColumnsList;
    private JList<String> selectedColumnsList;
    private DefaultListModel<String> availableModel;
    private DefaultListModel<String> selectedModel;
    private JButton addButton;
    private JButton removeButton;
    private JButton addAllButton;
    private JButton removeAllButton;
    private JButton saveButton;
    private JButton cancelButton;
    
    private Map<String, Object> columnConfigData;
    private List<String> selectedColumnKeys;
    
    public MonteCarloConfig(JFrame parent, Map<String, Object> columnConfigData, List<String> currentSelection) {
        super(parent, "Monte Carlo Configuration", true);
        this.columnConfigData = columnConfigData;
        this.selectedColumnKeys = new ArrayList<>(currentSelection);
        
        initializeComponents();
        loadAvailableColumns();
        loadSelectedColumns();
        setupLayout();
        setupListeners();
        
        setSize(600, 400);
        setLocationRelativeTo(parent);
    }
    
    private void initializeComponents() {
        availableModel = new DefaultListModel<>();
        selectedModel = new DefaultListModel<>();
        
        availableColumnsList = new JList<>(availableModel);
        availableColumnsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        selectedColumnsList = new JList<>(selectedModel);
        selectedColumnsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        addButton = new JButton(">");
        addButton.setToolTipText("Add selected columns");
        
        removeButton = new JButton("<");
        removeButton.setToolTipText("Remove selected columns");
        
        addAllButton = new JButton(">>");
        addAllButton.setToolTipText("Add all columns");
        
        removeAllButton = new JButton("<<");
        removeAllButton.setToolTipText("Remove all columns");
        
        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
    }
    
    private void loadAvailableColumns() {
        if (columnConfigData != null && columnConfigData.containsKey("columnConfiguration")) {
            Map<String, Object> columnConfig = (Map<String, Object>) columnConfigData.get("columnConfiguration");
            
            // Load from allColumnNames
            if (columnConfig.containsKey("allColumnNames")) {
                List<String> allColumns = (List<String>) columnConfig.get("allColumnNames");
                for (String column : allColumns) {
                    // Only add if not already selected
                    if (!selectedColumnKeys.contains(column)) {
                        availableModel.addElement(column);
                    }
                }
            }
            
            // Load from other sections if needed
            if (columnConfig.containsKey("timeframeIndicators")) {
                Map<String, List<String>> timeframeIndicators = 
                    (Map<String, List<String>>) columnConfig.get("timeframeIndicators");
                for (Map.Entry<String, List<String>> entry : timeframeIndicators.entrySet()) {
                    String timeframe = entry.getKey();
                    for (String indicator : entry.getValue()) {
                        String columnName = timeframe + " " + indicator;
                        if (!selectedColumnKeys.contains(columnName) && !availableModel.contains(columnName)) {
                            availableModel.addElement(columnName);
                        }
                    }
                }
            }
        }
    }
    
    private void loadSelectedColumns() {
        for (String column : selectedColumnKeys) {
            selectedModel.addElement(column);
        }
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        
        // Main panel with lists and buttons
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Available columns panel
        JPanel availablePanel = new JPanel(new BorderLayout());
        availablePanel.setBorder(BorderFactory.createTitledBorder("Available Columns"));
        availablePanel.add(new JScrollPane(availableColumnsList), BorderLayout.CENTER);
        
        // Selected columns panel
        JPanel selectedPanel = new JPanel(new BorderLayout());
        selectedPanel.setBorder(BorderFactory.createTitledBorder("Selected Columns for Monte Carlo"));
        selectedPanel.add(new JScrollPane(selectedColumnsList), BorderLayout.CENTER);
        
        // Button panel between lists
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(addAllButton);
        buttonPanel.add(removeAllButton);
        
        // Add panels to main panel
        mainPanel.add(availablePanel, BorderLayout.WEST);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(selectedPanel, BorderLayout.EAST);
        
        // Button panel at bottom
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(saveButton);
        bottomPanel.add(cancelButton);
        
        add(mainPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void setupListeners() {
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<String> selected = availableColumnsList.getSelectedValuesList();
                for (String column : selected) {
                    availableModel.removeElement(column);
                    selectedModel.addElement(column);
                    selectedColumnKeys.add(column);
                }
            }
        });
        
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<String> selected = selectedColumnsList.getSelectedValuesList();
                for (String column : selected) {
                    selectedModel.removeElement(column);
                    availableModel.addElement(column);
                    selectedColumnKeys.remove(column);
                }
            }
        });
        
        addAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                while (!availableModel.isEmpty()) {
                    String column = availableModel.getElementAt(0);
                    availableModel.removeElementAt(0);
                    selectedModel.addElement(column);
                    selectedColumnKeys.add(column);
                }
            }
        });
        
        removeAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                while (!selectedModel.isEmpty()) {
                    String column = selectedModel.getElementAt(0);
                    selectedModel.removeElementAt(0);
                    availableModel.addElement(column);
                    selectedColumnKeys.remove(column);
                }
            }
        });
        
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //selectedColumnKeys.clear();
                dispose();
            }
        });
    }
    
    public List<String> getSelectedColumns() {
        return new ArrayList<>(selectedColumnKeys);
    }
}