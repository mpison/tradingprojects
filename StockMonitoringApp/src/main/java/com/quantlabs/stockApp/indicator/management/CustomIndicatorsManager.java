package com.quantlabs.stockApp.indicator.management;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class CustomIndicatorsManager extends JDialog {
    private DefaultListModel<CustomIndicator> indicatorsModel;
    private JList<CustomIndicator> indicatorsList;
    private Map<String, Set<CustomIndicator>> customIndicatorsMap;
    private boolean saved = false;
    private IndicatorsManagementApp indicatorsManagementApp; // Add this field
    
 // Update constructors to accept IndicatorsManagementApp
    public CustomIndicatorsManager(JDialog parent, Map<String, Set<CustomIndicator>> customIndicatorsMap, 
                                   IndicatorsManagementApp app) {
        super(parent, "Manage Custom Indicators", true);
        this.customIndicatorsMap = customIndicatorsMap != null ? customIndicatorsMap : new HashMap<>();
        this.indicatorsManagementApp = app; // Set the app reference
        initializeUI();
        loadExistingIndicators();
        pack();
        setLocationRelativeTo(parent);
        setSize(600, 500);
    }
    
    // Also update the JFrame constructor
    public CustomIndicatorsManager(JFrame parent, Map<String, Set<CustomIndicator>> customIndicatorsMap,
                                   IndicatorsManagementApp app) {
        super(parent, "Manage Custom Indicators", true);
        this.customIndicatorsMap = customIndicatorsMap != null ? customIndicatorsMap : new HashMap<>();
        this.indicatorsManagementApp = app; // Set the app reference
        initializeUI();
        loadExistingIndicators();
        pack();
        setLocationRelativeTo(parent);
        setSize(600, 500);
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Indicators list
        indicatorsModel = new DefaultListModel<>();
        indicatorsList = new JList<>(indicatorsModel);
        indicatorsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        indicatorsList.setCellRenderer(new CustomIndicatorRenderer());
        
        JScrollPane listScrollPane = new JScrollPane(indicatorsList);
        listScrollPane.setBorder(BorderFactory.createTitledBorder("Custom Indicators"));
        
        // Control buttons
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add Custom Indicator");
        JButton editButton = new JButton("Edit Selected");
        JButton deleteButton = new JButton("Delete Selected");
        JButton duplicateButton = new JButton("Duplicate Selected");
        
        controlPanel.add(addButton);
        controlPanel.add(editButton);
        controlPanel.add(deleteButton);
        controlPanel.add(duplicateButton);
        
        // Button actions
        addButton.addActionListener(e -> showCustomIndicatorDialog(null));
        editButton.addActionListener(e -> editSelectedIndicator());
        deleteButton.addActionListener(e -> deleteSelectedIndicator());
        duplicateButton.addActionListener(e -> duplicateSelectedIndicator());
        
        mainPanel.add(listScrollPane, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save & Close");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            saved = true;
            dispose();
        });
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void loadExistingIndicators() {
        indicatorsModel.clear();
        Set<CustomIndicator> allIndicators = new HashSet<>();
        for (Set<CustomIndicator> indicators : customIndicatorsMap.values()) {
            allIndicators.addAll(indicators);
        }
        for (CustomIndicator indicator : allIndicators) {
            indicatorsModel.addElement(indicator);
        }
    }
    
    private void showCustomIndicatorDialog(CustomIndicator existingIndicator) {
        // Pass the indicatorsManagementApp to CustomIndicatorDialog
        CustomIndicatorDialog dialog = new CustomIndicatorDialog(this, existingIndicator, indicatorsManagementApp);
        dialog.setVisible(true);
        
        if (dialog.isSaved()) {
            CustomIndicator indicator = dialog.getCustomIndicator();
            if (existingIndicator == null) {
                indicatorsModel.addElement(indicator);
            } else {
                int index = indicatorsModel.indexOf(existingIndicator);
                indicatorsModel.set(index, indicator);
            }
        }
    }
    
    private void editSelectedIndicator() {
        CustomIndicator selected = indicatorsList.getSelectedValue();
        if (selected != null) {
            showCustomIndicatorDialog(selected);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a custom indicator to edit.");
        }
    }
    
    private void deleteSelectedIndicator() {
        CustomIndicator selected = indicatorsList.getSelectedValue();
        if (selected != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete '" + selected.getDisplayName() + "'?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
                
            if (confirm == JOptionPane.YES_OPTION) {
                indicatorsModel.removeElement(selected);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a custom indicator to delete.");
        }
    }
    
    private void duplicateSelectedIndicator() {
        CustomIndicator selected = indicatorsList.getSelectedValue();
        if (selected != null) {
            CustomIndicator duplicate = duplicateCustomIndicator(selected);
            duplicate.setName(duplicate.getName() + " (Copy)");
            indicatorsModel.addElement(duplicate);
        } else {
            JOptionPane.showMessageDialog(this, "Please select a custom indicator to duplicate.");
        }
    }
    
    private CustomIndicator duplicateCustomIndicator(CustomIndicator original) {
        Map<String, Object> params = new HashMap<>(original.getParameters());
        return new CustomIndicator(original.getName(), original.getType(), params);
    }
    
    public Map<String, Set<CustomIndicator>> getCustomIndicatorsMap() {
        // Convert the list model back to the map structure
        Map<String, Set<CustomIndicator>> result = new HashMap<>();
        for (int i = 0; i < indicatorsModel.size(); i++) {
            CustomIndicator indicator = indicatorsModel.get(i);
            // For now, we'll store all custom indicators in a general bucket
            // In a real implementation, you might want to organize them differently
            result.computeIfAbsent("all", k -> new HashSet<>()).add(indicator);
        }
        return result;
    }
    
    public boolean isSaved() { return saved; }
    
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