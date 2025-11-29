package com.quantlabs.QuantTester.test2.watchlist;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;

public class IndicatorEditorDialog extends JDialog {
    private IndicatorCombination combination;
    private DefaultListModel<Indicator> indicatorModel;
    private JList<Indicator> indicatorList;
    private JComboBox<String> typeComboBox;
    private JTextField timeframeField;
    private JSpinner shiftSpinner;
    private JPanel paramsPanel;
    private JButton addButton, updateButton, deleteButton;
    private boolean isLoadingIndicator = false;
    
    public IndicatorEditorDialog(JFrame parent, IndicatorCombination combination) {
        super(parent, "Edit Indicator Combination: " + combination.getName(), true);
        this.combination = combination;
        setSize(800, 600);
        setLayout(new BorderLayout());
        
        // Initialize components
        indicatorModel = new DefaultListModel<>();
        indicatorList = new JList<>(indicatorModel);
        indicatorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Load existing indicators
        for (Indicator indicator : combination.getIndicators()) {
            indicatorModel.addElement(indicator);
        }
        
        // Form components
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        typeComboBox = new JComboBox<>(new String[]{"Heikin-Ashi", "MACD", "RSI", "PSAR", "Stochastic", "Volume MA", "Specific Volume"});
        typeComboBox.addActionListener(e -> {
            if (!isLoadingIndicator) {
                updateParamsPanel();
            }
        });
        
        timeframeField = new JTextField("4h");
        shiftSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        
        formPanel.add(new JLabel("Indicator Type:"));
        formPanel.add(typeComboBox);
        formPanel.add(new JLabel("Timeframe:"));
        formPanel.add(timeframeField);
        formPanel.add(new JLabel("Shift:"));
        formPanel.add(shiftSpinner);
        
        // Dynamic parameters panel
        paramsPanel = new JPanel();
        updateParamsPanel();
        
        // Buttons
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");
        JButton doneButton = new JButton("Done");
        
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(doneButton);
        
        // Layout
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JScrollPane(indicatorList), BorderLayout.CENTER);
        
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(formPanel, BorderLayout.NORTH);
        rightPanel.add(paramsPanel, BorderLayout.CENTER);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(300);
        
        add(splitPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Event listeners
        addButton.addActionListener(e -> addIndicator());
        updateButton.addActionListener(e -> updateIndicator());
        deleteButton.addActionListener(e -> deleteIndicator());
        doneButton.addActionListener(e -> dispose());
        indicatorList.addListSelectionListener(e -> loadSelectedIndicator());
    }
    
    private void updateParamsPanel() {
        paramsPanel.removeAll();
        String selectedType = (String) typeComboBox.getSelectedItem();
        Map<String, JComponent> paramFields = new LinkedHashMap<>();

        switch (selectedType) {
            case "MACD":
                paramFields.put("signalTimeFrame", new JSpinner(new SpinnerNumberModel(9, 1, 50, 1)));
                paramFields.put("shortTimeFrame", new JSpinner(new SpinnerNumberModel(12, 1, 50, 1)));
                paramFields.put("longTimeFrame", new JSpinner(new SpinnerNumberModel(26, 1, 50, 1)));
                break;
            case "RSI":
                paramFields.put("timeFrame", new JSpinner(new SpinnerNumberModel(14, 1, 50, 1)));
                paramFields.put("overbought", new JSpinner(new SpinnerNumberModel(70, 0, 100, 1)));
                paramFields.put("oversold", new JSpinner(new SpinnerNumberModel(30, 0, 100, 1)));
                break;
            case "PSAR":
                paramFields.put("accelerationFactor", new JSpinner(new SpinnerNumberModel(0.02, 0.001, 1, 0.001)));
                paramFields.put("maxAcceleration", new JSpinner(new SpinnerNumberModel(0.2, 0.001, 1, 0.001)));
                break;
            case "Stochastic":
                paramFields.put("kTimeFrame", new JSpinner(new SpinnerNumberModel(14, 1, 50, 1)));
                paramFields.put("dTimeFrame", new JSpinner(new SpinnerNumberModel(3, 1, 50, 1)));
                paramFields.put("overbought", new JSpinner(new SpinnerNumberModel(80, 0, 100, 1)));
                paramFields.put("oversold", new JSpinner(new SpinnerNumberModel(20, 0, 100, 1)));
                break;
            case "Volume MA":
                paramFields.put("maPeriod", new JSpinner(new SpinnerNumberModel(20, 1, 200, 1)));
                paramFields.put("maType", new JComboBox<>(new String[]{"SMA", "EMA", "WMA"}));
                paramFields.put("threshold", new JSpinner(new SpinnerNumberModel(0, 0, Double.MAX_VALUE, 1000)));
                break;
            case "Specific Volume":
                paramFields.put("comparison", new JComboBox<>(new String[]{"Above", "Below", "Equal"}));
                paramFields.put("volumeValue", new JSpinner(new SpinnerNumberModel(1000000, 0, Long.MAX_VALUE, 10000)));
                //paramFields.put("timeframe", new JComboBox<>(new String[]{"1m", "5m", "15m", "30m", "1h", "4h", "1d", "1w"}));
                break;
            default: // Heikin-Ashi
                // No parameters needed
        }

        paramsPanel.setLayout(new GridLayout(paramFields.size(), 2, 5, 5));
        for (Map.Entry<String, JComponent> entry : paramFields.entrySet()) {
            paramsPanel.add(new JLabel(entry.getKey() + ":"));
            paramsPanel.add(entry.getValue());
        }

        paramsPanel.revalidate();
        paramsPanel.repaint();
    }
    
    private boolean isDuplicateConfiguration(String type, String timeframe, int shift, Map<String, Object> params) {
        for (int i = 0; i < indicatorModel.size(); i++) {
            Indicator existing = indicatorModel.get(i);
            
            // Traditional null-safe equality checks
            boolean sameType = (existing.getType() == null ? type == null : existing.getType().equals(type));
            boolean sameTimeframe = (existing.getTimeframe() == null ? timeframe == null : existing.getTimeframe().equals(timeframe));
            boolean sameShift = existing.getShift() == shift;
            boolean sameParams = (existing.getParams() == null ? params == null : existing.getParams().equals(params));
            
            if (sameType && sameTimeframe && sameShift && sameParams) {
                return true;
            }
        }
        return false;
    }
    
    private void addIndicator() {
        try {
            String type = (String) typeComboBox.getSelectedItem();
            String timeframe = timeframeField.getText().trim();
            int shift = (Integer) shiftSpinner.getValue();

            if (timeframe.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Timeframe is required", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Map<String, Object> params = new HashMap<>();
            Component[] components = paramsPanel.getComponents();
                        

            for (int i = 0; i < components.length; i += 2) {
                if (components[i] instanceof JLabel) {
                    String paramName = ((JLabel) components[i]).getText().replace(":", "");
                    JComponent valueComponent = (JComponent) components[i + 1];

                    if (valueComponent instanceof JSpinner) {
                        Object value = ((JSpinner) valueComponent).getValue();
                        params.put(paramName, value);
                    }
                    if (valueComponent instanceof JComboBox) {
                    	String value = (String)((JComboBox) valueComponent).getSelectedItem();
                        params.put(paramName, value);
                    }
                }
            }
            
            // Volume-specific validation
            if (type.equals("Specific Volume")) {
                long volumeValue;
                try {
                    Object value = params.get("volumeValue");
                    volumeValue = (value instanceof Number) ? ((Number) value).longValue() : Long.parseLong(value.toString());
                    if (volumeValue < 0) {
                        throw new IllegalArgumentException("Volume value cannot be negative");
                    }
                    params.put("volumeValue", volumeValue); // Ensure long storage
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid volume value: " + e.getMessage());
                }
            }
            else if (type.equals("Volume MA")) {
                // Validate MA parameters
                int maPeriod = (Integer) params.get("maPeriod");
                if (maPeriod <= 0) {
                    throw new IllegalArgumentException("MA period must be positive");
                }
            }

            // Check for duplicate configuration
            if (isDuplicateConfiguration(type, timeframe, shift, params)) {
                JOptionPane.showMessageDialog(this, 
                    "This indicator configuration already exists in the combination",
                    "Duplicate Indicator", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            Indicator newIndicator = new Indicator(type, timeframe, shift, params);
            indicatorModel.addElement(newIndicator);
            combination.setIndicators(new ArrayList<>(Collections.list(indicatorModel.elements())));
            clearForm();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateIndicator() {
        int selectedIndex = indicatorList.getSelectedIndex();
        if (selectedIndex != -1) {
            try {
                Indicator original = indicatorList.getSelectedValue();
                String newType = (String) typeComboBox.getSelectedItem();
                String newTimeframe = timeframeField.getText().trim();
                int newShift = (Integer) shiftSpinner.getValue();

                if (newTimeframe.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Timeframe is required", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                Map<String, Object> newParams = new HashMap<>();
                Component[] components = paramsPanel.getComponents();

                for (int i = 0; i < components.length; i += 2) {
                    if (components[i] instanceof JLabel) {
                        String paramName = ((JLabel) components[i]).getText().replace(":", "");
                        JComponent valueComponent = (JComponent) components[i + 1];

                        if (valueComponent instanceof JSpinner) {
                            Object value = ((JSpinner) valueComponent).getValue();
                            newParams.put(paramName, value);
                        }
                        
                        if (valueComponent instanceof JComboBox) {
                        	String value = (String)((JComboBox) valueComponent).getSelectedItem();
                        	newParams.put(paramName, value);
                        }
                    }
                }

                // Check for duplicate (excluding current indicator)
                boolean isDuplicate = false;
                for (int i = 0; i < indicatorModel.size(); i++) {
                    if (i == selectedIndex) continue;
                    
                    Indicator existing = indicatorModel.get(i);
                    if (existing.getType().equals(newType) &&
                        existing.getTimeframe().equals(newTimeframe) &&
                        existing.getShift() == newShift &&
                        existing.getParams().equals(newParams)) {
                        isDuplicate = true;
                        break;
                    }
                }

                if (isDuplicate) {
                    JOptionPane.showMessageDialog(this, 
                        "This indicator configuration already exists in the combination",
                        "Duplicate Indicator", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Indicator updatedIndicator = new Indicator(newType, newTimeframe, newShift, newParams);
                indicatorModel.set(selectedIndex, updatedIndicator);
                combination.setIndicators(new ArrayList<>(Collections.list(indicatorModel.elements())));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select an indicator to update", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void deleteIndicator() {
        int selectedIndex = indicatorList.getSelectedIndex();
        if (selectedIndex != -1) {
            indicatorModel.remove(selectedIndex);
            combination.setIndicators(new ArrayList<>(Collections.list(indicatorModel.elements())));
            clearForm();
        } else {
            JOptionPane.showMessageDialog(this, "Please select an indicator to delete", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadSelectedIndicator() {
        isLoadingIndicator = true; // Set loading flag
        
        Indicator selected = indicatorList.getSelectedValue();
        if (selected != null) {
            typeComboBox.setSelectedItem(selected.getType());
            timeframeField.setText(selected.getTimeframe());
            shiftSpinner.setValue(selected.getShift());

            // Update parameter fields
            updateParamsPanel();
            Map<String, Object> params = selected.getParams();
            Component[] components = paramsPanel.getComponents();

            for (int i = 0; i < components.length; i += 2) {
                if (components[i] instanceof JLabel) {
                    String paramName = ((JLabel) components[i]).getText().replace(":", "");
                    JComponent valueComponent = (JComponent) components[i + 1];

                    if (valueComponent instanceof JSpinner && params.containsKey(paramName)) {
                        ((JSpinner) valueComponent).setValue(params.get(paramName));
                    }
                }
            }
        }
        
        isLoadingIndicator = false; // Clear loading flag
    }
    
    private void clearForm() {
        timeframeField.setText("4h");
        shiftSpinner.setValue(0);
        indicatorList.clearSelection();
        updateParamsPanel(); // Reset to default fields
    }
}