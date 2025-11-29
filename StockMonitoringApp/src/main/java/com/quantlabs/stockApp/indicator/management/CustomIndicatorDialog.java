package com.quantlabs.stockApp.indicator.management;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class CustomIndicatorDialog extends JDialog {
	private CustomIndicator customIndicator;
	private boolean saved = false;

	private JTextField nameField;
	private JComboBox<String> typeComboBox;
	private JPanel parametersPanel;
	private JPanel otherParametersPanel;
	private Map<String, JComponent> parameterComponents;

	private static final String[] INDICATOR_TYPES = { "MACD", "PSAR", "RSI", "TREND", "VOLUMEMA", "MOVINGAVERAGE",
			"HIGHESTCLOSEOPEN", "MOVINGAVERAGETARGETVALUE" };

	// Constants for price types and MA types
	private static final String[] PRICE_TYPES = { "CLOSE", "OPEN", "HIGH", "LOW" };
	private static final String[] MA_TYPES = { "SMA", "EMA" };

	public CustomIndicatorDialog(JDialog parent, CustomIndicator existingIndicator) {
		super(parent, existingIndicator == null ? "Add Custom Indicator" : "Edit Custom Indicator", true);
		this.customIndicator = existingIndicator != null ? existingIndicator : new CustomIndicator("", "MACD");
		this.parameterComponents = new HashMap<>();
		initializeUI();
		populateFields();
		pack();
		setLocationRelativeTo(parent);
		setSize(600, 600); // Increased size for new parameters panel
	}

	// Also add a constructor that accepts JFrame
	public CustomIndicatorDialog(JFrame parent, CustomIndicator existingIndicator) {
		super(parent, existingIndicator == null ? "Add Custom Indicator" : "Edit Custom Indicator", true);
		this.customIndicator = existingIndicator != null ? existingIndicator : new CustomIndicator("", "MACD");
		this.parameterComponents = new HashMap<>();
		initializeUI();
		populateFields();
		pack();
		setLocationRelativeTo(parent);
		setSize(600, 600); // Increased size for new parameters panel
	}

	private void initializeUI() {
		setLayout(new BorderLayout(10, 10));

		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTHWEST;

		// Name field
		gbc.gridx = 0;
		gbc.gridy = 0;
		mainPanel.add(new JLabel("Indicator Name:*"), gbc);
		gbc.gridx = 1;
		gbc.gridwidth = 2;
		nameField = new JTextField(20);
		mainPanel.add(nameField, gbc);

		// Type combo box
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		mainPanel.add(new JLabel("Indicator Type:*"), gbc);
		gbc.gridx = 1;
		gbc.gridwidth = 2;
		typeComboBox = new JComboBox<>(INDICATOR_TYPES);
		typeComboBox.addActionListener(e -> updateParametersPanel());
		mainPanel.add(typeComboBox, gbc);

		// Parameters panel
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 3;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 0.6; // Reduced weight for parameters
		parametersPanel = new JPanel();
		parametersPanel.setBorder(BorderFactory.createTitledBorder("Indicator Parameters"));
		mainPanel.add(parametersPanel, gbc);

		// Other Parameters panel
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 3;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 0.4; // Allocated weight for other parameters
		otherParametersPanel = new JPanel();
		otherParametersPanel.setBorder(BorderFactory.createTitledBorder("Other Parameters"));
		mainPanel.add(otherParametersPanel, gbc);

		add(mainPanel, BorderLayout.CENTER);

		// Button panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton saveButton = new JButton("Save");
		JButton cancelButton = new JButton("Cancel");

		saveButton.addActionListener(e -> saveCustomIndicator());
		cancelButton.addActionListener(e -> dispose());

		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);

		add(buttonPanel, BorderLayout.SOUTH);

		// Initialize parameters panels
		updateParametersPanel();
		initializeOtherParametersPanel();
	}

	private void initializeOtherParametersPanel() {
	    otherParametersPanel.setLayout(new GridBagLayout());
	    GridBagConstraints gbc = new GridBagConstraints();
	    gbc.insets = new Insets(3, 3, 3, 3);
	    gbc.fill = GridBagConstraints.HORIZONTAL;
	    gbc.anchor = GridBagConstraints.NORTHWEST;
	    
	    int row = 0;
	    
	    // Last Index - Using JSpinner instead of JTextField
	    gbc.gridx = 0; gbc.gridy = row++;
	    otherParametersPanel.add(new JLabel("Last Index:"), gbc);
	    gbc.gridx = 1;
	    JSpinner lastIndexSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
	    lastIndexSpinner.setToolTipText("Set the last index value (numeric)");
	    otherParametersPanel.add(lastIndexSpinner, gbc);
	    parameterComponents.put("lastIndex", lastIndexSpinner);
	    
	    // Trend Checkbox
	    gbc.gridx = 0; gbc.gridy = row++;
	    JCheckBox trendCheckbox = new JCheckBox("Trend");
	    trendCheckbox.setToolTipText("Enable trend calculation");
	    otherParametersPanel.add(trendCheckbox, gbc);
	    parameterComponents.put("trend", trendCheckbox);
	    
	    // Value Checkbox
	    gbc.gridx = 0; gbc.gridy = row++;
	    JCheckBox valueCheckbox = new JCheckBox("Value");
	    valueCheckbox.setToolTipText("Enable value calculation");
	    otherParametersPanel.add(valueCheckbox, gbc);
	    parameterComponents.put("value", valueCheckbox);
	    
	    // Percentile Checkbox
	    gbc.gridx = 0; gbc.gridy = row++;
	    JCheckBox percentileCheckbox = new JCheckBox("Percentile");
	    percentileCheckbox.setToolTipText("Enable percentile calculation");
	    otherParametersPanel.add(percentileCheckbox, gbc);
	    parameterComponents.put("percentile", percentileCheckbox);
	    
	    // Target Value Checkbox
	    gbc.gridx = 0; gbc.gridy = row++;
	    JCheckBox targetValueCheckbox = new JCheckBox("Target Value");
	    targetValueCheckbox.setToolTipText("Enable target value calculation");
	    otherParametersPanel.add(targetValueCheckbox, gbc);
	    parameterComponents.put("targetValue", targetValueCheckbox);
	    
	    // Add filler to push content to top
	    gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
	    gbc.weighty = 1.0;
	    gbc.fill = GridBagConstraints.BOTH;
	    otherParametersPanel.add(new JPanel(), gbc);
	    
	    otherParametersPanel.revalidate();
	    otherParametersPanel.repaint();
	}

	private void updateParametersPanel() {
		parametersPanel.removeAll();
		// Clear only indicator-specific parameters, keep other parameters
		Map<String, JComponent> indicatorSpecificParams = new HashMap<>();
		for (Map.Entry<String, JComponent> entry : parameterComponents.entrySet()) {
			String key = entry.getKey();
			if (!key.equals("lastIndex") && !key.equals("trend") && !key.equals("value") && !key.equals("percentile")
					&& !key.equals("targetValue")) {
				indicatorSpecificParams.put(key, entry.getValue());
			}
		}
		parameterComponents.keySet().removeAll(indicatorSpecificParams.keySet());

		parametersPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTHWEST;

		int row = 0;

		String selectedType = (String) typeComboBox.getSelectedItem();

		switch (selectedType) {
		case "MACD":
			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Fast Period:"), gbc);
			gbc.gridx = 1;
			JSpinner macdFast = new JSpinner(new SpinnerNumberModel(12, 1, 50, 1));
			parametersPanel.add(macdFast, gbc);
			parameterComponents.put("fast", macdFast);

			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Slow Period:"), gbc);
			gbc.gridx = 1;
			JSpinner macdSlow = new JSpinner(new SpinnerNumberModel(26, 1, 100, 1));
			parametersPanel.add(macdSlow, gbc);
			parameterComponents.put("slow", macdSlow);

			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Signal Period:"), gbc);
			gbc.gridx = 1;
			JSpinner macdSignal = new JSpinner(new SpinnerNumberModel(9, 1, 50, 1));
			parametersPanel.add(macdSignal, gbc);
			parameterComponents.put("signal", macdSignal);
			break;

		case "PSAR":
			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Step:"), gbc);
			gbc.gridx = 1;
			JSpinner psarStep = new JSpinner(new SpinnerNumberModel(0.02, 0.001, 0.1, 0.001));
			parametersPanel.add(psarStep, gbc);
			parameterComponents.put("step", psarStep);

			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Max:"), gbc);
			gbc.gridx = 1;
			JSpinner psarMax = new JSpinner(new SpinnerNumberModel(0.2, 0.1, 1.0, 0.01));
			parametersPanel.add(psarMax, gbc);
			parameterComponents.put("max", psarMax);
			break;

		case "RSI":
			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Period:"), gbc);
			gbc.gridx = 1;
			JSpinner rsiPeriod = new JSpinner(new SpinnerNumberModel(14, 2, 50, 1));
			parametersPanel.add(rsiPeriod, gbc);
			parameterComponents.put("period", rsiPeriod);
			break;

		case "TREND":
			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("SMA 1 Period:"), gbc);
			gbc.gridx = 1;
			JSpinner trendSma1 = new JSpinner(new SpinnerNumberModel(9, 1, 50, 1));
			parametersPanel.add(trendSma1, gbc);
			parameterComponents.put("sma1", trendSma1);

			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("SMA 2 Period:"), gbc);
			gbc.gridx = 1;
			JSpinner trendSma2 = new JSpinner(new SpinnerNumberModel(20, 1, 100, 1));
			parametersPanel.add(trendSma2, gbc);
			parameterComponents.put("sma2", trendSma2);

			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("SMA 3 Period:"), gbc);
			gbc.gridx = 1;
			JSpinner trendSma3 = new JSpinner(new SpinnerNumberModel(200, 50, 500, 1));
			parametersPanel.add(trendSma3, gbc);
			parameterComponents.put("sma3", trendSma3);
			break;

		case "VOLUMEMA":
			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Period:"), gbc);
			gbc.gridx = 1;
			JSpinner volumePeriod = new JSpinner(new SpinnerNumberModel(20, 1, 100, 1));
			parametersPanel.add(volumePeriod, gbc);
			parameterComponents.put("period", volumePeriod);
			break;

		case "MOVINGAVERAGE":
			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Period:"), gbc);
			gbc.gridx = 1;
			JSpinner maPeriod = new JSpinner(new SpinnerNumberModel(20, 1, 200, 1));
			parametersPanel.add(maPeriod, gbc);
			parameterComponents.put("period", maPeriod);

			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Type:"), gbc);
			gbc.gridx = 1;
			JComboBox<String> maType = new JComboBox<>(new String[] { "SMA", "EMA", "WMA" });
			parametersPanel.add(maType, gbc);
			parameterComponents.put("type", maType);
			break;

		case "HIGHESTCLOSEOPEN":
			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Time Range:"), gbc);
			gbc.gridx = 1;
			JComboBox<String> hcoTimeRange = new JComboBox<>(new String[] { "1D", "1W", "1M", "3M", "1Y", "All" });
			parametersPanel.add(hcoTimeRange, gbc);
			parameterComponents.put("timeRange", hcoTimeRange);

			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Session:"), gbc);
			gbc.gridx = 1;
			JComboBox<String> hcoSession = new JComboBox<>(
					new String[] { "all", "premarket", "standard", "postmarket" });
			parametersPanel.add(hcoSession, gbc);
			parameterComponents.put("session", hcoSession);

			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Lookback Period:"), gbc);
			gbc.gridx = 1;
			JSpinner hcoLookback = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
			parametersPanel.add(hcoLookback, gbc);
			parameterComponents.put("lookback", hcoLookback);
			break;

		case "MOVINGAVERAGETARGETVALUE":
			// First Moving Average Configuration
			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("First MA - Period:"), gbc);
			gbc.gridx = 1;
			JSpinner ma1Period = new JSpinner(new SpinnerNumberModel(20, 1, 200, 1));
			parametersPanel.add(ma1Period, gbc);
			parameterComponents.put("ma1Period", ma1Period);

			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("First MA - Price Type:"), gbc);
			gbc.gridx = 1;
			JComboBox<String> ma1PriceType = new JComboBox<>(PRICE_TYPES);
			parametersPanel.add(ma1PriceType, gbc);
			parameterComponents.put("ma1PriceType", ma1PriceType);

			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("First MA - Type:"), gbc);
			gbc.gridx = 1;
			JComboBox<String> ma1Type = new JComboBox<>(MA_TYPES);
			parametersPanel.add(ma1Type, gbc);
			parameterComponents.put("ma1Type", ma1Type);

			// Second Moving Average Configuration
			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Second MA - Period:"), gbc);
			gbc.gridx = 1;
			JSpinner ma2Period = new JSpinner(new SpinnerNumberModel(50, 1, 200, 1));
			parametersPanel.add(ma2Period, gbc);
			parameterComponents.put("ma2Period", ma2Period);

			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Second MA - Price Type:"), gbc);
			gbc.gridx = 1;
			JComboBox<String> ma2PriceType = new JComboBox<>(PRICE_TYPES);
			parametersPanel.add(ma2PriceType, gbc);
			parameterComponents.put("ma2PriceType", ma2PriceType);

			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Second MA - Type:"), gbc);
			gbc.gridx = 1;
			JComboBox<String> ma2Type = new JComboBox<>(MA_TYPES);
			parametersPanel.add(ma2Type, gbc);
			parameterComponents.put("ma2Type", ma2Type);

			// Trend Configuration
			gbc.gridx = 0;
			gbc.gridy = row++;
			parametersPanel.add(new JLabel("Trend Source:"), gbc);
			gbc.gridx = 1;
			JComboBox<String> trendSource = new JComboBox<>(
					new String[] { "PSAR_0.01", "PSAR_0.05", "PRICE_ACTION", "CANDLE", "HEIKENASHI", "CUSTOM" });
			parametersPanel.add(trendSource, gbc);
			parameterComponents.put("trendSource", trendSource);

			// Add description label
			gbc.gridx = 0;
			gbc.gridy = row++;
			gbc.gridwidth = 2;
			JLabel descriptionLabel = new JLabel(
					"<html><i>Note: Target value = MA1 + (MA1 - MA2) for uptrend, MA1 - (MA1 - MA2) for downtrend</i></html>");
			descriptionLabel.setForeground(Color.GRAY);
			parametersPanel.add(descriptionLabel, gbc);
			break;
		}

		// Add filler to push content to top
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		parametersPanel.add(new JPanel(), gbc);

		parametersPanel.revalidate();
		parametersPanel.repaint();
	}

	private void populateFields() {
	    nameField.setText(customIndicator.getName());
	    typeComboBox.setSelectedItem(customIndicator.getType());
	    
	    // Populate parameter values for both indicator-specific and other parameters
	    Map<String, Object> params = customIndicator.getParameters();
	    for (Map.Entry<String, JComponent> entry : parameterComponents.entrySet()) {
	        String paramName = entry.getKey();
	        JComponent component = entry.getValue();
	        Object value = params.get(paramName);
	        
	        if (value != null) {
	            if (component instanceof JSpinner) {
	                ((JSpinner) component).setValue(value);
	            } else if (component instanceof JComboBox) {
	                ((JComboBox<?>) component).setSelectedItem(value);
	            } else if (component instanceof JCheckBox) {
	                ((JCheckBox) component).setSelected(Boolean.TRUE.equals(value));
	            }
	        }
	    }
	}

	private void saveCustomIndicator() {
	    // Validate inputs
	    if (nameField.getText().trim().isEmpty()) {
	        JOptionPane.showMessageDialog(this, "Please enter an indicator name.");
	        return;
	    }
	    
	    // Update custom indicator
	    customIndicator.setName(nameField.getText().trim());
	    customIndicator.setType((String) typeComboBox.getSelectedItem());
	    
	    // Update parameters - both indicator-specific and other parameters
	    Map<String, Object> params = new HashMap<>();
	    for (Map.Entry<String, JComponent> entry : parameterComponents.entrySet()) {
	        String paramName = entry.getKey();
	        JComponent component = entry.getValue();
	        
	        if (component instanceof JSpinner) {
	            params.put(paramName, ((JSpinner) component).getValue());
	        } else if (component instanceof JComboBox) {
	            params.put(paramName, ((JComboBox<?>) component).getSelectedItem());
	        } else if (component instanceof JCheckBox) {
	            params.put(paramName, ((JCheckBox) component).isSelected());
	        }
	    }
	    customIndicator.setParameters(params);
	    
	    saved = true;
	    dispose();
	}

	public boolean isSaved() {
		return saved;
	}

	public CustomIndicator getCustomIndicator() {
		return customIndicator;
	}
}