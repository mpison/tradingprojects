package com.quantlabs.stockApp.indicator.management;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class CustomIndicatorSelectionDialog extends JDialog {
    private JTextField nameField;
    private JList<CustomIndicator> availableIndicatorsList;
    private JList<CustomIndicator> selectedIndicatorsList;
    private DefaultListModel<CustomIndicator> availableModel;
    private DefaultListModel<CustomIndicator> selectedModel;
    private boolean saved = false;
    private IndicatorsManagementApp indicatorsManagementApp;
    
    public CustomIndicatorSelectionDialog(JDialog parent, 
                                        String existingName,
                                        Set<CustomIndicator> existingIndicators,
                                        IndicatorsManagementApp indicatorsManagementApp) {
        super(parent, "Custom Indicator Selection", true);
        this.indicatorsManagementApp = indicatorsManagementApp;
        initializeUI(existingName, existingIndicators);
        pack();
        setLocationRelativeTo(parent);
        setSize(600, 500);
    }
    
    private void initializeUI(String existingName, Set<CustomIndicator> existingIndicators) {
        setLayout(new BorderLayout(10, 10));
        
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Configuration name panel
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.setBorder(BorderFactory.createTitledBorder("Configuration Name"));
        nameField = new JTextField(30);
        if (existingName != null) {
            nameField.setText(existingName);
        }
        namePanel.add(new JLabel("Name:"));
        namePanel.add(nameField);
        
        // Indicators selection panel
        JPanel selectionPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        
        // Available indicators panel
        JPanel availablePanel = new JPanel(new BorderLayout());
        availablePanel.setBorder(BorderFactory.createTitledBorder("Available Custom Indicators"));
        
        availableModel = new DefaultListModel<>();
        availableIndicatorsList = new JList<>(availableModel);
        availableIndicatorsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        availableIndicatorsList.setCellRenderer(new CustomIndicatorRenderer());
        
        JScrollPane availableScrollPane = new JScrollPane(availableIndicatorsList);
        availablePanel.add(availableScrollPane, BorderLayout.CENTER);
        
        // Selected indicators panel
        JPanel selectedPanel = new JPanel(new BorderLayout());
        selectedPanel.setBorder(BorderFactory.createTitledBorder("Selected Custom Indicators"));
        
        selectedModel = new DefaultListModel<>();
        selectedIndicatorsList = new JList<>(selectedModel);
        selectedIndicatorsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        selectedIndicatorsList.setCellRenderer(new CustomIndicatorRenderer());
        
        JScrollPane selectedScrollPane = new JScrollPane(selectedIndicatorsList);
        selectedPanel.add(selectedScrollPane, BorderLayout.CENTER);
        
        selectionPanel.add(availablePanel);
        selectionPanel.add(selectedPanel);
        
        // Transfer buttons panel
        JPanel transferPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        JButton addButton = new JButton(">> Add >>");
        JButton removeButton = new JButton("<< Remove <<");
        JButton addAllButton = new JButton("Add All >>");
        JButton removeAllButton = new JButton("<< Remove All");
        
        addButton.addActionListener(e -> addSelectedIndicators());
        removeButton.addActionListener(e -> removeSelectedIndicators());
        addAllButton.addActionListener(e -> addAllIndicators());
        removeAllButton.addActionListener(e -> removeAllIndicators());
        
        transferPanel.add(addButton);
        transferPanel.add(removeButton);
        transferPanel.add(addAllButton);
        transferPanel.add(removeAllButton);
        
        // Add transfer panel between the two lists
        JPanel listsPanel = new JPanel(new BorderLayout());
        listsPanel.add(availablePanel, BorderLayout.WEST);
        listsPanel.add(transferPanel, BorderLayout.CENTER);
        listsPanel.add(selectedPanel, BorderLayout.EAST);
        
        mainPanel.add(namePanel, BorderLayout.NORTH);
        mainPanel.add(listsPanel, BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            if (validateInput()) {
                saved = true;
                dispose();
            }
        });
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Load data
        loadAvailableIndicators();
        loadExistingIndicators(existingIndicators);
    }
    
    private void loadAvailableIndicators() {
        availableModel.clear();
        if (indicatorsManagementApp != null) {
            Set<CustomIndicator> globalIndicators = indicatorsManagementApp.getGlobalCustomIndicators();
            for (CustomIndicator indicator : globalIndicators) {
                availableModel.addElement(indicator);
            }
        }
    }
    
    private void loadExistingIndicators(Set<CustomIndicator> existingIndicators) {
        selectedModel.clear();
        if (existingIndicators != null) {
            for (CustomIndicator indicator : existingIndicators) {
                selectedModel.addElement(indicator);
                // Remove from available list
                for (int i = 0; i < availableModel.size(); i++) {
                    if (availableModel.get(i).equals(indicator)) {
                        availableModel.remove(i);
                        break;
                    }
                }
            }
        }
    }
    
    private void addSelectedIndicators() {
        List<CustomIndicator> selected = availableIndicatorsList.getSelectedValuesList();
        for (CustomIndicator indicator : selected) {
            selectedModel.addElement(indicator);
            availableModel.removeElement(indicator);
        }
    }
    
    private void removeSelectedIndicators() {
        List<CustomIndicator> selected = selectedIndicatorsList.getSelectedValuesList();
        for (CustomIndicator indicator : selected) {
            availableModel.addElement(indicator);
            selectedModel.removeElement(indicator);
        }
    }
    
    private void addAllIndicators() {
        for (int i = 0; i < availableModel.size(); i++) {
            selectedModel.addElement(availableModel.get(i));
        }
        availableModel.clear();
    }
    
    private void removeAllIndicators() {
        for (int i = 0; i < selectedModel.size(); i++) {
            availableModel.addElement(selectedModel.get(i));
        }
        selectedModel.clear();
    }
    
    private boolean validateInput() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a configuration name.", 
                                        "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (selectedModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one custom indicator.", 
                                        "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return true;
    }
    
    public String getConfigurationName() {
        return nameField.getText().trim();
    }
    
    public Set<CustomIndicator> getSelectedIndicators() {
        Set<CustomIndicator> indicators = new HashSet<>();
        for (int i = 0; i < selectedModel.size(); i++) {
            indicators.add(selectedModel.get(i));
        }
        return indicators;
    }
    
    public boolean isSaved() {
        return saved;
    }
    
    // Custom renderer for custom indicators list
    private class CustomIndicatorRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof CustomIndicator) {
                CustomIndicator indicator = (CustomIndicator) value;
                setText(indicator.getDisplayName());
                
                // Add tooltip with details
                StringBuilder tooltip = new StringBuilder();
                tooltip.append("<html><b>").append(indicator.getDisplayName()).append("</b><br>");
                tooltip.append("Type: ").append(indicator.getType()).append("<br>");
                tooltip.append("Parameters: ").append(indicator.getParameters()).append("</html>");
                setToolTipText(tooltip.toString());
            }
            return this;
        }
    }
}