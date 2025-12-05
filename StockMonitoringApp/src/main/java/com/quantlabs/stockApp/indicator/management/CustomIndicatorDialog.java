package com.quantlabs.stockApp.indicator.management;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.quantlabs.stockApp.utils.StrategyCheckerHelper;

public class CustomIndicatorDialog extends JDialog {
	private CustomIndicator customIndicator;
	private boolean saved = false;
	private IndicatorsManagementApp indicatorsManagementApp; // Add this field

	private JTextField nameField;
	private JComboBox<String> typeComboBox;
	private JPanel parametersPanel;
	private JPanel otherParametersPanel;
	private Map<String, JComponent> parameterComponents;
	private StrategyCheckerHelper strategyCheckerHelper;

	private static final String[] INDICATOR_TYPES = { "MACD", "PSAR", "RSI", "TREND", "VOLUMEMA", "MOVINGAVERAGE",
			"HIGHESTCLOSEOPEN", "MOVINGAVERAGETARGETVALUE", "MultiTimeframeZScore" };

	// Constants for price types and MA types
	private static final String[] PRICE_TYPES = { "CLOSE", "OPEN", "HIGH", "LOW" };
	private static final String[] MA_TYPES = { "SMA", "EMA" };

	// Update constructors to accept IndicatorsManagementApp
    public CustomIndicatorDialog(JDialog parent, CustomIndicator existingIndicator, IndicatorsManagementApp app) {
        super(parent, existingIndicator == null ? "Add Custom Indicator" : "Edit Custom Indicator", true);
        this.customIndicator = existingIndicator != null ? existingIndicator : new CustomIndicator("", "MACD");
        this.parameterComponents = new HashMap<>();
        this.indicatorsManagementApp = app; // Set the app reference
        initializeUI();
        populateFields();
        pack();
        setLocationRelativeTo(parent);
        setSize(600, 600);
    }

    // Also update the JFrame constructor
    public CustomIndicatorDialog(JFrame parent, CustomIndicator existingIndicator, IndicatorsManagementApp app) {
        super(parent, existingIndicator == null ? "Add Custom Indicator" : "Edit Custom Indicator", true);
        this.customIndicator = existingIndicator != null ? existingIndicator : new CustomIndicator("", "MACD");
        this.parameterComponents = new HashMap<>();
        this.indicatorsManagementApp = app; // Set the app reference
        initializeUI();
        populateFields();
        pack();
        setLocationRelativeTo(parent);
        setSize(600, 600);
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
		gbc.gridx = 0;
		gbc.gridy = row++;
		otherParametersPanel.add(new JLabel("Last Index:"), gbc);
		gbc.gridx = 1;
		JSpinner lastIndexSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
		lastIndexSpinner.setToolTipText("Set the last index value (numeric)");
		otherParametersPanel.add(lastIndexSpinner, gbc);
		parameterComponents.put("lastIndex", lastIndexSpinner);

		// Trend Checkbox
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox trendCheckbox = new JCheckBox("Trend");
		trendCheckbox.setToolTipText("Enable trend calculation");
		otherParametersPanel.add(trendCheckbox, gbc);
		parameterComponents.put("trend", trendCheckbox);

		// Value Checkbox
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox valueCheckbox = new JCheckBox("Value");
		valueCheckbox.setToolTipText("Enable value calculation");
		otherParametersPanel.add(valueCheckbox, gbc);
		parameterComponents.put("value", valueCheckbox);

		// Percentile Checkbox
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox percentileCheckbox = new JCheckBox("Percentile");
		percentileCheckbox.setToolTipText("Enable percentile calculation");
		otherParametersPanel.add(percentileCheckbox, gbc);
		parameterComponents.put("percentile", percentileCheckbox);

		// Target Value Checkbox
		gbc.gridx = 0;
		gbc.gridy = row++;
		JCheckBox targetValueCheckbox = new JCheckBox("Target Value");
		targetValueCheckbox.setToolTipText("Enable target value calculation");
		otherParametersPanel.add(targetValueCheckbox, gbc);
		parameterComponents.put("targetValue", targetValueCheckbox);

		// Add filler to push content to top
		gbc.gridx = 0;
		gbc.gridy = row;
		gbc.gridwidth = 2;
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

		case "MultiTimeframeZScore":
			// Strategy Selection ComboBox
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.gridwidth = 1;
            parametersPanel.add(new JLabel("Strategy Configuration:*"), gbc);
            gbc.gridx = 1;
            gbc.gridwidth = 2;
            
            // Get enabled strategies from IndicatorsManagementApp
            List<String> strategyNames = getEnabledStrategyNames();
            JComboBox<String> strategyComboBox = new JComboBox<>(strategyNames.toArray(new String[0]));
            strategyComboBox.setToolTipText("Select the strategy configuration to use for MultiTimeframeZScore calculation");
            parametersPanel.add(strategyComboBox, gbc);
            parameterComponents.put("selectedStrategy", strategyComboBox);
            
            // Add refresh button for strategies
            gbc.gridx = 3;
            gbc.gridwidth = 1;
            JButton refreshStrategiesButton = new JButton("Refresh");
            refreshStrategiesButton.setToolTipText("Refresh the list of available strategies");
            refreshStrategiesButton.addActionListener(e -> {
                // Refresh strategy list
                List<String> refreshedNames = getEnabledStrategyNames();
                strategyComboBox.removeAllItems();
                for (String name : refreshedNames) {
                    strategyComboBox.addItem(name);
                }
                JOptionPane.showMessageDialog(this, 
                    "Refreshed strategy list. Found " + (refreshedNames.size() - 1) + " enabled strategies.",
                    "Strategy List Refreshed", 
                    JOptionPane.INFORMATION_MESSAGE);
            });
            parametersPanel.add(refreshStrategiesButton, gbc);
            
            // Add timeframe checkboxes
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.gridwidth = 4;
            JLabel timeframeLabel = new JLabel("Select Timeframes to Include:");
            timeframeLabel.setFont(timeframeLabel.getFont().deriveFont(Font.BOLD));
            parametersPanel.add(timeframeLabel, gbc);

            // Available timeframes
            String[] availableTimeframes = { "1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min" };

            // Create a panel with checkboxes in two columns
            JPanel timeframesPanel = new JPanel(new GridLayout(0, 2, 5, 5));
            timeframesPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

            for (String timeframe : availableTimeframes) {
                JCheckBox timeframeCheckbox = new JCheckBox(timeframe);
                timeframeCheckbox.setSelected(true);
                timeframeCheckbox.setToolTipText("Include " + timeframe + " in Z-Score calculation");
                timeframesPanel.add(timeframeCheckbox);
                parameterComponents.put("timeframe_" + timeframe, timeframeCheckbox);
            }

            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.gridwidth = 4;
            parametersPanel.add(timeframesPanel, gbc);

            // Control buttons for timeframes
            JPanel timeframeControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton selectAllTimeframesButton = new JButton("Select All");
            JButton clearAllTimeframesButton = new JButton("Clear All");
            JButton selectCommonButton = new JButton("Select Common");

            selectAllTimeframesButton.addActionListener(e -> {
                for (String timeframe : availableTimeframes) {
                    JCheckBox checkbox = (JCheckBox) parameterComponents.get("timeframe_" + timeframe);
                    if (checkbox != null)
                        checkbox.setSelected(true);
                }
            });

            clearAllTimeframesButton.addActionListener(e -> {
                for (String timeframe : availableTimeframes) {
                    JCheckBox checkbox = (JCheckBox) parameterComponents.get("timeframe_" + timeframe);
                    if (checkbox != null)
                        checkbox.setSelected(false);
                }
            });

            selectCommonButton.addActionListener(e -> {
                String[] commonTimeframes = { "1D", "4H", "1H", "30Min", "15Min" };
                for (String timeframe : availableTimeframes) {
                    JCheckBox checkbox = (JCheckBox) parameterComponents.get("timeframe_" + timeframe);
                    if (checkbox != null) {
                        boolean shouldSelect = Arrays.asList(commonTimeframes).contains(timeframe);
                        checkbox.setSelected(shouldSelect);
                    }
                }
            });

            timeframeControlsPanel.add(selectAllTimeframesButton);
            timeframeControlsPanel.add(clearAllTimeframesButton);
            timeframeControlsPanel.add(selectCommonButton);

            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.gridwidth = 4;
            parametersPanel.add(timeframeControlsPanel, gbc);

            // Info label
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.gridwidth = 4;
            JLabel infoLabel = new JLabel(
                    "<html><i>Note: MultiTimeframeZScore calculates Z-Scores across multiple timeframes.<br>"
                            + "Selected timeframes will be included in the Z-Score calculation.<br>"
                            + "Uses the selected strategy's Z-Score configuration (weights, indicators).</i></html>");
            infoLabel.setForeground(Color.BLUE);
            parametersPanel.add(infoLabel, gbc);
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
		
		// Special handling for MultiTimeframeZScore
        if ("MultiTimeframeZScore".equals(typeComboBox.getSelectedItem())) {
            // Check for selectedTimeframes parameter
            Object selectedTimeframesObj = params.get("selectedTimeframes");
            if (selectedTimeframesObj instanceof String) {
                String selectedTimeframesStr = (String) selectedTimeframesObj;
                Set<String> selectedTimeframes = new HashSet<>(Arrays.asList(selectedTimeframesStr.split(",")));
                
                String[] availableTimeframes = {"1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min"};
                for (String timeframe : availableTimeframes) {
                    JCheckBox checkbox = (JCheckBox) parameterComponents.get("timeframe_" + timeframe);
                    if (checkbox != null) {
                        checkbox.setSelected(selectedTimeframes.contains(timeframe));
                    }
                }
            }
            
            // Restore selected strategy
            Object selectedStrategyObj = params.get("selectedStrategy");
            if (selectedStrategyObj instanceof String) {
                JComboBox<String> strategyComboBox = (JComboBox<String>) parameterComponents.get("selectedStrategy");
                if (strategyComboBox != null) {
                    String strategyName = (String) selectedStrategyObj;
                    
                    // Check if the strategy exists in the current list
                    boolean found = false;
                    for (int i = 0; i < strategyComboBox.getItemCount(); i++) {
                        if (strategyName.equals(strategyComboBox.getItemAt(i))) {
                            strategyComboBox.setSelectedItem(strategyName);
                            found = true;
                            break;
                        }
                    }
                    
                    // If strategy not found, check if it exists in the app
                    if (!found && !strategyName.isEmpty()) {
                        StrategyConfig strategy = getStrategyConfigByName(strategyName);
                        if (strategy != null) {
                            // Strategy exists, add it to the combo box
                            strategyComboBox.addItem(strategyName);
                            strategyComboBox.setSelectedItem(strategyName);
                        }
                    }
                }
            }
        }
	}
	
	// Add a method to refresh the strategy list in the UI
    private void refreshStrategyList(JComboBox<String> strategyComboBox) {
        if (strategyComboBox != null) {
            // Store current selection
            String currentSelection = (String) strategyComboBox.getSelectedItem();
            
            // Clear and repopulate
            strategyComboBox.removeAllItems();
            List<String> strategyNames = getEnabledStrategyNames();
            
            for (String name : strategyNames) {
                strategyComboBox.addItem(name);
            }
            
            // Try to restore previous selection
            if (currentSelection != null && !currentSelection.isEmpty()) {
                for (int i = 0; i < strategyComboBox.getItemCount(); i++) {
                    if (currentSelection.equals(strategyComboBox.getItemAt(i))) {
                        strategyComboBox.setSelectedItem(currentSelection);
                        break;
                    }
                }
            }
        }
    }

	// Also update the saveCustomIndicator method to get and store the StrategyConfig
    private void saveCustomIndicator() {
        // Validate inputs
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an indicator name.");
            return;
        }

        // Additional validation for MultiTimeframeZScore
        String selectedType = (String) typeComboBox.getSelectedItem();
        if ("MultiTimeframeZScore".equals(selectedType)) {
            JComboBox<String> strategyComboBox = (JComboBox<String>) parameterComponents.get("selectedStrategy");
            if (strategyComboBox != null) {
                String selectedStrategyName = (String) strategyComboBox.getSelectedItem();
                if (selectedStrategyName == null || selectedStrategyName.trim().isEmpty()) {
                    int result = JOptionPane.showConfirmDialog(this,
                        "No strategy selected for MultiTimeframeZScore. Continue without a strategy?",
                        "Strategy Selection Warning",
                        JOptionPane.YES_NO_OPTION);
                    
                    if (result != JOptionPane.YES_OPTION) {
                        return;
                    }
                } else {
                    // Verify the strategy exists and is enabled
                    StrategyConfig selectedStrategy = getStrategyConfigByName(selectedStrategyName);
                    if (selectedStrategy == null) {
                        JOptionPane.showMessageDialog(this,
                            "Selected strategy '" + selectedStrategyName + "' is no longer available or enabled.\n" +
                            "Please select a different strategy or refresh the list.",
                            "Strategy Not Available",
                            JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            }
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
        
        // Special handling for MultiTimeframeZScore
        if ("MultiTimeframeZScore".equals(selectedType)) {
            // Extract selected timeframes
            List<String> selectedTimeframes = new ArrayList<>();
            String[] availableTimeframes = {"1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min"};
            
            for (String timeframe : availableTimeframes) {
                JCheckBox checkbox = (JCheckBox) parameterComponents.get("timeframe_" + timeframe);
                if (checkbox != null && checkbox.isSelected()) {
                    selectedTimeframes.add(timeframe);
                }
            }
            
            // Store selected timeframes
            params.put("selectedTimeframes", String.join(",", selectedTimeframes));
            
            // Store timeframe boolean parameters
            for (String timeframe : availableTimeframes) {
                JCheckBox checkbox = (JCheckBox) parameterComponents.get("timeframe_" + timeframe);
                if (checkbox != null) {
                    params.put("timeframe_" + timeframe, checkbox.isSelected());
                }
            }
            
            // Store selected strategy name
            JComboBox<String> strategyComboBox = (JComboBox<String>) parameterComponents.get("selectedStrategy");
            if (strategyComboBox != null) {
                String selectedStrategyName = (String) strategyComboBox.getSelectedItem();
                if (selectedStrategyName != null && !selectedStrategyName.trim().isEmpty()) {
                    params.put("selectedStrategy", selectedStrategyName);
                    
                    // Also store the strategy configuration if available
                    StrategyConfig selectedStrategy = getStrategyConfigByName(selectedStrategyName);
                    if (selectedStrategy != null) {
                        // Store the strategy config (you might want to store a copy or just reference)
                        params.put("strategyConfig", selectedStrategy);
                        params.put("strategyConfigName", selectedStrategy.getName());
                    }
                } else {
                    params.put("selectedStrategy", "");
                }
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
	
	// Update the method to get enabled strategies from IndicatorsManagementApp
    private List<String> getEnabledStrategyNames() {
        List<String> strategyNames = new ArrayList<>();
        strategyNames.add(""); // Empty option for no strategy
        
        if (indicatorsManagementApp != null) {
            try {
                // Get all strategies from IndicatorsManagementApp
                List<StrategyConfig> allStrategies = indicatorsManagementApp.getAllStrategies();
                
                if (allStrategies != null && !allStrategies.isEmpty()) {
                    for (StrategyConfig strategy : allStrategies) {
                        if (strategy != null && strategy.getName() != null && !strategy.getName().trim().isEmpty()) {
                            strategyNames.add(strategy.getName());
                        }
                    }
                } else {
                    addDefaultStrategyNames(strategyNames);
                }
            } catch (Exception e) {
                System.err.println("Error getting strategies from IndicatorsManagementApp: " + e.getMessage());
                e.printStackTrace();
                addDefaultStrategyNames(strategyNames);
            }
        } else {
            addDefaultStrategyNames(strategyNames);
        }
        
        return strategyNames;
    }
	
    // Add a helper method to get StrategyConfig by name
    private StrategyConfig getStrategyConfigByName(String strategyName) {
        if (indicatorsManagementApp != null && strategyName != null && !strategyName.trim().isEmpty()) {
            try {
                List<StrategyConfig> allStrategies = indicatorsManagementApp.getAllStrategies();
                if (allStrategies != null) {
                    for (StrategyConfig strategy : allStrategies) {
                        if (strategy != null && strategyName.equals(strategy.getName())) {
                            return strategy;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error finding strategy by name: " + e.getMessage());
            }
        }
        return null;
    }

    
    // Add a setter method for flexibility
    public void setStrategyCheckerHelper(StrategyCheckerHelper strategyCheckerHelper) {
        this.strategyCheckerHelper = strategyCheckerHelper;
    }

    private void addDefaultStrategyNames(List<String> strategyNames) {
        // Add some default strategy names as fallback
        strategyNames.add("Default Strategy");
        strategyNames.add("Momentum Strategy");
        strategyNames.add("Trend Following Strategy");
        strategyNames.add("Mean Reversion Strategy");
        strategyNames.add("Breakout Strategy");
        strategyNames.add("Swing Trading Strategy");
    }

}