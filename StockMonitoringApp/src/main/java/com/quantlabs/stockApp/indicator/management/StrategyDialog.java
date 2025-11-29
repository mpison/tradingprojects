package com.quantlabs.stockApp.indicator.management;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;

import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class StrategyDialog extends JDialog {
    private StrategyConfig strategyConfig;
    private boolean saved = false;

    private JTextField nameField;
    private JCheckBox enabledCheckbox;
    private JCheckBox alarmEnabledCheckbox;
    private JCheckBox symbolExclusiveCheckbox;
    private JList<String> symbolList;
    private DefaultListModel<String> symbolModel;
    private JTextField symbolTextField;

    private JTabbedPane timeframeTabs;
    private Map<String, JList<String>> timeframeIndicatorLists;
    private Map<String, JList<CustomIndicator>> timeframeCustomIndicatorLists;
    private Set<String> availableIndicators;
    private Set<CustomIndicator> availableCustomIndicators;
    private JButton manageCustomIndicatorsButton;
    private JComboBox<String> watchlistComboBox;

    // Z-Score fields
    private Map<String, JSpinner> timeframeWeightSpinners;
    private Map<String, JCheckBox> timeframeCheckboxes;
    private JLabel totalWeightLabel;
    private JLabel validationLabel;
    private JTextArea scorePreviewArea;

    private static final String[] ALL_TIMEFRAMES = { "1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min" };
    private static final String[] SAMPLE_SYMBOLS = { "AAPL", "GOOGL", "MSFT", "TSLA", "AMZN", "META", "NVDA", "NFLX" };

    private IndicatorsManagementApp mainApp;

    public StrategyDialog(JFrame parent, StrategyConfig existingConfig, Set<String> availableIndicators,
                         Set<String> existingKeys, IndicatorsManagementApp mainApp) {
        super(parent, existingConfig == null ? "Add Strategy" : "Edit Strategy", true);
        this.strategyConfig = existingConfig != null ? existingConfig : new StrategyConfig();
        this.availableIndicators = availableIndicators;
        this.availableCustomIndicators = new HashSet<>();
        this.timeframeIndicatorLists = new HashMap<>();
        this.timeframeCustomIndicatorLists = new HashMap<>();
        this.mainApp = mainApp;

        // Initialize Z-Score collections
        this.timeframeWeightSpinners = new HashMap<>();
        this.timeframeCheckboxes = new HashMap<>();

        // Load global custom indicators from main application
        loadGlobalCustomIndicators();

        initializeUI(existingKeys);
        populateFields();
        pack();
        setLocationRelativeTo(parent);
        setSize(800, 700);
    }

    private void initializeUI(Set<String> existingKeys) {
        setLayout(new BorderLayout(10, 10));

        // Main form panel with tabs
        JTabbedPane mainTabbedPane = new JTabbedPane();

        // Basic Settings Tab
        mainTabbedPane.addTab("Basic Settings", createBasicSettingsPanel(existingKeys));

        // Timeframe Indicators Tab
        mainTabbedPane.addTab("Timeframe Indicators", createTimeframeIndicatorsPanel());

        // Strategy Parameters Tab
        mainTabbedPane.addTab("Strategy Parameters", createStrategyParametersPanel());

        // Symbols Tab
        mainTabbedPane.addTab("Symbols", createSymbolsPanel());

        // Z-Score Tab
        mainTabbedPane.addTab("Z-Score", createZScorePanel());

        add(mainTabbedPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> saveStrategy(existingKeys));
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Enter key to save
        getRootPane().setDefaultButton(saveButton);
    }

    private JPanel createBasicSettingsPanel(Set<String> existingKeys) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // Strategy name
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Strategy Name:*"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        nameField = new JTextField(25);
        panel.add(nameField, gbc);

        // Enabled checkbox
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        enabledCheckbox = new JCheckBox("Strategy Enabled");
        enabledCheckbox.setSelected(true);
        panel.add(enabledCheckbox, gbc);

        // Alarm enabled checkbox
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        alarmEnabledCheckbox = new JCheckBox("Enable Alarm");
        alarmEnabledCheckbox.setSelected(false);
        panel.add(alarmEnabledCheckbox, gbc);

        // Symbol exclusive checkbox
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        symbolExclusiveCheckbox = new JCheckBox("Apply to specific symbols only (optional)");
        symbolExclusiveCheckbox.addActionListener(e -> updateSymbolExclusivity());
        panel.add(symbolExclusiveCheckbox, gbc);

        // Watchlist selection
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Watchlist (optional):"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;

        // Create watchlist combo box
        List<String> watchlistNames = new ArrayList<>();
        watchlistNames.add(""); // Empty option for no watchlist
        watchlistNames.addAll(mainApp.getGlobalWatchlists().keySet());
        watchlistComboBox = new JComboBox<>(watchlistNames.toArray(new String[0]));
        panel.add(watchlistComboBox, gbc);

        // Manage custom indicators button
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        manageCustomIndicatorsButton = new JButton("Manage Custom Indicators");
        manageCustomIndicatorsButton.addActionListener(e -> manageCustomIndicators());
        panel.add(manageCustomIndicatorsButton, gbc);

        // Custom indicators info
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 3;
        JLabel customIndicatorsInfo = new JLabel("Available Custom Indicators: " + availableCustomIndicators.size());
        customIndicatorsInfo.setForeground(Color.BLUE);
        panel.add(customIndicatorsInfo, gbc);

        // Info label
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 3;
        JLabel infoLabel = new JLabel(
                "<html><i>Note: Each strategy-timeframe combination must be unique across all strategies.<br>"
                        + "Each timeframe can have its own independent set of indicators (standard and custom).<br>"
                        + "Strategy-specific parameters can be configured in the 'Strategy Parameters' tab.<br>"
                        + "Watchlists are optional - if set, strategy will only process symbols from the selected watchlist.</i></html>");
        infoLabel.setForeground(Color.BLUE);
        panel.add(infoLabel, gbc);

        // Add filler to push content to top
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), gbc);

        return panel;
    }

    private JPanel createTimeframeIndicatorsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Info label
        JLabel infoLabel = new JLabel("Configure independent indicators for each timeframe (standard and custom):");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(infoLabel, BorderLayout.NORTH);

        // Timeframe tabs
        timeframeTabs = new JTabbedPane();

        // Create a tab for each timeframe
        for (String timeframe : ALL_TIMEFRAMES) {
            timeframeTabs.addTab(timeframe, createTimeframeTab(timeframe));
        }

        panel.add(timeframeTabs, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTimeframeTab(String timeframe) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // Split pane for standard and custom indicators
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(0.5);

        // Standard indicators panel
        JPanel standardPanel = new JPanel(new BorderLayout());
        DefaultListModel<String> indicatorModel = new DefaultListModel<>();
        for (String indicator : availableIndicators) {
            indicatorModel.addElement(indicator);
        }

        JList<String> indicatorList = new JList<>(indicatorModel);
        indicatorList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        timeframeIndicatorLists.put(timeframe, indicatorList);

        JScrollPane indicatorScroll = new JScrollPane(indicatorList);
        indicatorScroll.setBorder(BorderFactory.createTitledBorder("Standard Indicators for " + timeframe));

        // Control buttons for standard indicators
        JPanel standardControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectAllStandardButton = new JButton("Select All");
        JButton clearAllStandardButton = new JButton("Clear All");

        selectAllStandardButton.addActionListener(e -> indicatorList.setSelectionInterval(0, indicatorModel.size() - 1));
        clearAllStandardButton.addActionListener(e -> indicatorList.clearSelection());

        standardControlPanel.add(selectAllStandardButton);
        standardControlPanel.add(clearAllStandardButton);

        standardPanel.add(indicatorScroll, BorderLayout.CENTER);
        standardPanel.add(standardControlPanel, BorderLayout.SOUTH);

        // Custom indicators panel
        JPanel customPanel = new JPanel(new BorderLayout());
        DefaultListModel<CustomIndicator> customIndicatorModel = new DefaultListModel<>();
        for (CustomIndicator indicator : availableCustomIndicators) {
            customIndicatorModel.addElement(indicator);
        }

        JList<CustomIndicator> customIndicatorList = new JList<>(customIndicatorModel);
        customIndicatorList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        customIndicatorList.setCellRenderer(new CustomIndicatorRenderer());
        timeframeCustomIndicatorLists.put(timeframe, customIndicatorList);

        JScrollPane customIndicatorScroll = new JScrollPane(customIndicatorList);
        customIndicatorScroll.setBorder(BorderFactory.createTitledBorder("Custom Indicators for " + timeframe));

        // Control buttons for custom indicators
        JPanel customControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectAllCustomButton = new JButton("Select All");
        JButton clearAllCustomButton = new JButton("Clear All");

        selectAllCustomButton.addActionListener(e -> customIndicatorList.setSelectionInterval(0, customIndicatorModel.size() - 1));
        clearAllCustomButton.addActionListener(e -> customIndicatorList.clearSelection());

        customControlPanel.add(selectAllCustomButton);
        customControlPanel.add(clearAllCustomButton);

        customPanel.add(customIndicatorScroll, BorderLayout.CENTER);
        customPanel.add(customControlPanel, BorderLayout.SOUTH);

        splitPane.setTopComponent(standardPanel);
        splitPane.setBottomComponent(customPanel);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStrategyParametersPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Info label
        JLabel infoLabel = new JLabel("<html>Strategy-specific parameters will be configured during execution.<br>"
                + "For strategies like HighestCloseOpen, parameters such as Time Range, Session, and Index Counter<br>"
                + "will be provided through the execution context.</html>");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(infoLabel, BorderLayout.NORTH);

        // Example parameters configuration (could be expanded for specific strategies)
        JPanel paramsPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        paramsPanel.setBorder(BorderFactory.createTitledBorder("Common Parameters (Example)"));

        paramsPanel.add(new JLabel("Default Time Range:"));
        JComboBox<String> timeRangeCombo = new JComboBox<>(new String[] { "1D", "1W", "1M", "3M", "1Y", "All" });
        paramsPanel.add(timeRangeCombo);

        paramsPanel.add(new JLabel("Default Session:"));
        JComboBox<String> sessionCombo = new JComboBox<>(new String[] { "All", "Premarket", "Standard", "Postmarket" });
        paramsPanel.add(sessionCombo);

        paramsPanel.add(new JLabel("Default Index Counter:"));
        JTextField indexCounterField = new JTextField("5");
        paramsPanel.add(indexCounterField);

        panel.add(paramsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSymbolsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Info label
        JLabel infoLabel = new JLabel("Optional: Specify symbols this strategy applies to exclusively");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(infoLabel, BorderLayout.NORTH);

        // Symbol input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(new JLabel("Add Symbol:"), BorderLayout.WEST);

        symbolTextField = new JTextField();
        inputPanel.add(symbolTextField, BorderLayout.CENTER);

        JButton addSymbolButton = new JButton("Add");
        addSymbolButton.addActionListener(e -> addSymbol());
        inputPanel.add(addSymbolButton, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.NORTH);

        // Symbol list
        symbolModel = new DefaultListModel<>();
        symbolList = new JList<>(symbolModel);
        JScrollPane symbolScroll = new JScrollPane(symbolList);
        symbolScroll.setBorder(BorderFactory.createTitledBorder("Exclusive Symbols"));

        // Control buttons for symbols
        JPanel symbolControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton removeSymbolButton = new JButton("Remove Selected");
        JButton addSampleSymbolsButton = new JButton("Add Sample Symbols");
        JButton clearSymbolsButton = new JButton("Clear All");

        removeSymbolButton.addActionListener(e -> removeSelectedSymbol());
        addSampleSymbolsButton.addActionListener(e -> addSampleSymbols());
        clearSymbolsButton.addActionListener(e -> clearSymbols());

        symbolControlPanel.add(removeSymbolButton);
        symbolControlPanel.add(addSampleSymbolsButton);
        symbolControlPanel.add(clearSymbolsButton);

        JPanel symbolListPanel = new JPanel(new BorderLayout());
        symbolListPanel.add(symbolScroll, BorderLayout.CENTER);
        symbolListPanel.add(symbolControlPanel, BorderLayout.SOUTH);

        panel.add(symbolListPanel, BorderLayout.CENTER);

        return panel;
    }

    // Z-Score Panel Implementation
    private JPanel createZScorePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Info label
        JLabel infoLabel = new JLabel(
            "<html><b>Z-Score Configuration for Current Strategy</b><br>" +
            "Check timeframes to include in Z-Score calculation and configure weights (0-100).<br>" +
            "Only checked timeframes must total 100.</html>"
        );
        infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(infoLabel, BorderLayout.NORTH);
        
        // Timeframe weights panel
        JPanel weightsPanel = createTimeframeWeightsPanel();
        panel.add(weightsPanel, BorderLayout.CENTER);
        
        // Preview and controls panel
        JPanel previewPanel = createPreviewPanel();
        panel.add(previewPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createTimeframeWeightsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Timeframe Weights (Only checked timeframes count toward 100 total)"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        timeframeWeightSpinners = new HashMap<>();
        timeframeCheckboxes = new HashMap<>();
        
        int row = 0;
        
        for (String timeframe : ALL_TIMEFRAMES) {
            // Checkbox
            gbc.gridx = 0;
            gbc.gridy = row;
            JCheckBox timeframeCheckbox = new JCheckBox();
            timeframeCheckbox.addActionListener(e -> onTimeframeCheckboxChanged(timeframe, timeframeCheckbox));
            timeframeCheckboxes.put(timeframe, timeframeCheckbox);
            panel.add(timeframeCheckbox, gbc);
            
            // Timeframe label
            gbc.gridx = 1;
            panel.add(new JLabel(timeframe + ":"), gbc);
            
            // Weight spinner
            gbc.gridx = 2;
            JSpinner weightSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
            weightSpinner.setEnabled(false); // Initially disabled until checkbox is checked
            weightSpinner.addChangeListener(e -> updateWeightTotal());
            timeframeWeightSpinners.put(timeframe, weightSpinner);
            panel.add(weightSpinner, gbc);
            
            row++;
        }
        
        // Total weight display
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Total Weight (checked timeframes):"), gbc);
        
        gbc.gridx = 2;
        gbc.gridwidth = 1;
        totalWeightLabel = new JLabel("0");
        totalWeightLabel.setFont(totalWeightLabel.getFont().deriveFont(Font.BOLD));
        panel.add(totalWeightLabel, gbc);
        
        // Validation label
        gbc.gridx = 0;
        gbc.gridy = row + 1;
        gbc.gridwidth = 3;
        validationLabel = new JLabel("");
        validationLabel.setForeground(Color.RED);
        panel.add(validationLabel, gbc);
        
        // Checkbox controls
        gbc.gridx = 0;
        gbc.gridy = row + 2;
        gbc.gridwidth = 3;
        JPanel checkboxControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton selectAllButton = new JButton("Select All");
        JButton selectNoneButton = new JButton("Select None");
        JButton selectCommonButton = new JButton("Select Common");
        
        selectAllButton.addActionListener(e -> setAllTimeframesSelected(true));
        selectNoneButton.addActionListener(e -> setAllTimeframesSelected(false));
        selectCommonButton.addActionListener(e -> selectCommonTimeframes());
        
        checkboxControls.add(selectAllButton);
        checkboxControls.add(selectNoneButton);
        checkboxControls.add(selectCommonButton);
        
        panel.add(checkboxControls, gbc);
        
        // Add filler to push content to top
        gbc.gridx = 0;
        gbc.gridy = row + 3;
        gbc.gridwidth = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), gbc);
        
        return panel;
    }
    
    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Score Preview"));
        
        scorePreviewArea = new JTextArea(6, 50);
        scorePreviewArea.setEditable(false);
        scorePreviewArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        scorePreviewArea.setText("Select timeframes and configure weights to see score calculation preview.");
        
        JScrollPane scrollPane = new JScrollPane(scorePreviewArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton calculateButton = new JButton("Calculate Preview");
        JButton autoDistributeButton = new JButton("Auto-Distribute Checked");
        
        calculateButton.addActionListener(e -> calculateScorePreview());
        autoDistributeButton.addActionListener(e -> autoDistributeWeights());
        
        buttonPanel.add(autoDistributeButton);
        buttonPanel.add(calculateButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void onTimeframeCheckboxChanged(String timeframe, JCheckBox checkbox) {
        JSpinner spinner = timeframeWeightSpinners.get(timeframe);
        if (spinner != null) {
            spinner.setEnabled(checkbox.isSelected());
            if (!checkbox.isSelected()) {
                spinner.setValue(0);
            }
            updateWeightTotal();
        }
    }
    
    private void setAllTimeframesSelected(boolean selected) {
        for (Map.Entry<String, JCheckBox> entry : timeframeCheckboxes.entrySet()) {
            String timeframe = entry.getKey();
            JCheckBox checkbox = entry.getValue();
            checkbox.setSelected(selected);
            
            JSpinner spinner = timeframeWeightSpinners.get(timeframe);
            if (spinner != null) {
                spinner.setEnabled(selected);
                if (!selected) {
                    spinner.setValue(0);
                }
            }
        }
        updateWeightTotal();
    }
    
    private void selectCommonTimeframes() {
        // Common timeframes for most strategies
        String[] commonTimeframes = {"1D", "4H", "1H", "30Min", "15Min"};
        
        for (Map.Entry<String, JCheckBox> entry : timeframeCheckboxes.entrySet()) {
            String timeframe = entry.getKey();
            JCheckBox checkbox = entry.getValue();
            boolean shouldSelect = Arrays.asList(commonTimeframes).contains(timeframe);
            checkbox.setSelected(shouldSelect);
            
            JSpinner spinner = timeframeWeightSpinners.get(timeframe);
            if (spinner != null) {
                spinner.setEnabled(shouldSelect);
                if (!shouldSelect) {
                    spinner.setValue(0);
                }
            }
        }
        updateWeightTotal();
    }
    
    private void updateWeightTotal() {
        int total = 0;
        int checkedCount = 0;
        
        for (String timeframe : ALL_TIMEFRAMES) {
            JSpinner spinner = timeframeWeightSpinners.get(timeframe);
            if (spinner != null && spinner.isEnabled()) {
                total += (Integer) spinner.getValue();
                checkedCount++;
            }
        }
        
        if (totalWeightLabel != null) {
            totalWeightLabel.setText(total + " (from " + checkedCount + " timeframes)");
        }
        
        if (validationLabel != null) {
            if (checkedCount == 0) {
                validationLabel.setText("ℹ️ No timeframes selected for Z-Score calculation");
                validationLabel.setForeground(Color.BLUE);
            } else if (total == 100) {
                validationLabel.setText("✓ Weights valid - total is 100 across " + checkedCount + " timeframes");
                validationLabel.setForeground(Color.GREEN);
            } else {
                validationLabel.setText("✗ Weights invalid - total must be 100 across checked timeframes (current: " + total + ")");
                validationLabel.setForeground(Color.RED);
            }
        }
    }
    
    private void autoDistributeWeights() {
        // Count checked timeframes
        int checkedCount = 0;
        for (String timeframe : ALL_TIMEFRAMES) {
            JSpinner spinner = timeframeWeightSpinners.get(timeframe);
            if (spinner != null && spinner.isEnabled()) {
                checkedCount++;
            }
        }
        
        if (checkedCount == 0) {
            JOptionPane.showMessageDialog(this, 
                "No timeframes are selected. Please check timeframes first.", 
                "No Timeframes Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Distribute 100 evenly among checked timeframes
        int baseWeight = 100 / checkedCount;
        int remainder = 100 % checkedCount;
        
        int count = 0;
        for (String timeframe : ALL_TIMEFRAMES) {
            JSpinner spinner = timeframeWeightSpinners.get(timeframe);
            if (spinner != null && spinner.isEnabled()) {
                int weight = baseWeight + (count < remainder ? 1 : 0);
                spinner.setValue(weight);
                count++;
            }
        }
        
        updateWeightTotal();
    }
    
    private void calculateScorePreview() {
        // Get only checked timeframes and their weights
        Map<String, ZScoreCalculator.TimeframeWeight> weights = getCurrentWeights();
        
        if (weights.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No timeframes are selected for Z-Score calculation.", 
                "No Timeframes Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (!ZScoreCalculator.validateWeights(weights)) {
            JOptionPane.showMessageDialog(this, 
                "Weights for checked timeframes must total 100 before calculating score.", 
                "Invalid Weights", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Create sample PriceData for preview (only with checked timeframes)
        PriceData sampleData = createSamplePriceData(weights.keySet());
        String currentStrategy = nameField.getText().trim();
        if (currentStrategy.isEmpty()) {
            currentStrategy = "Current Strategy";
        }
        
        ZScoreCalculator.ZScoreResult result = ZScoreCalculator.calculateStrategyScore(
            sampleData, currentStrategy, weights);
        
        // Display preview
        StringBuilder preview = new StringBuilder();
        preview.append("STRATEGY Z-SCORE PREVIEW: ").append(currentStrategy).append("\n");
        preview.append("==================================\n\n");
        preview.append(String.format("Overall Score: %.1f/100\n", result.getOverallScore()));
        preview.append(String.format("Strength: %s\n", result.getStrengthCategory()));
        preview.append(String.format("Timeframes Used: %d of %d\n\n", weights.size(), ALL_TIMEFRAMES.length));
        
        preview.append("Timeframe Breakdown:\n");
        preview.append("-------------------\n");
        for (String timeframe : ALL_TIMEFRAMES) {
            ZScoreCalculator.TimeframeWeight weight = weights.get(timeframe);
            if (weight != null) {
                Double score = result.getTimeframeScores().get(timeframe);
                Double weighted = result.getWeightedScores().get(timeframe);
                
                if (score != null && weighted != null) {
                    preview.append(String.format("✓ %-8s: %3d%% weight -> %5.1f raw -> %5.1f weighted\n", 
                        timeframe, weight.getWeight(), score, weighted));
                } else {
                    preview.append(String.format("✓ %-8s: %3d%% weight -> No data available\n", 
                        timeframe, weight.getWeight()));
                }
            } else {
                preview.append(String.format("✗ %-8s: Excluded from calculation\n", timeframe));
            }
        }
        
        scorePreviewArea.setText(preview.toString());
    }
    
    private Map<String, ZScoreCalculator.TimeframeWeight> getCurrentWeights() {
        Map<String, ZScoreCalculator.TimeframeWeight> weights = new HashMap<>();
        for (String timeframe : ALL_TIMEFRAMES) {
            JSpinner spinner = timeframeWeightSpinners.get(timeframe);
            if (spinner != null && spinner.isEnabled()) {
                int weight = (Integer) spinner.getValue();
                if (weight > 0) {
                    weights.put(timeframe, new ZScoreCalculator.TimeframeWeight(timeframe, weight));
                }
            }
        }
        return weights;
    }
    
    private PriceData createSamplePriceData(Set<String> includedTimeframes) {
        // Create sample data only for included timeframes using the Builder pattern
        PriceData sampleData = new PriceData.Builder("AAPL", 150.0)
            .name("Apple Inc.")
            .currency("USD")
            .build();
        
        LinkedHashMap<String, AnalysisResult> results = new LinkedHashMap<>();
        
        // Sample scores for different timeframes
        double[] sampleScores = {85.0, 72.0, 65.0, 58.0, 45.0, 38.0, 25.0, 15.0};
        int i = 0;
        
        for (String timeframe : ALL_TIMEFRAMES) {
            if (includedTimeframes.contains(timeframe) && i < sampleScores.length) {
                AnalysisResult result = new AnalysisResult();
                result.setTrendZscore(sampleScores[i]);
                result.setMacdZscore(sampleScores[i] - 5);
                result.setRsiZscore(sampleScores[i] + 3);
                results.put(timeframe, result);
                i++;
            }
        }
        
        sampleData.setResults(results);
        return sampleData;
    }

    // Existing methods from original StrategyDialog
    private void addSymbol() {
        String symbol = symbolTextField.getText().trim().toUpperCase();
        if (!symbol.isEmpty() && !symbolModel.contains(symbol)) {
            symbolModel.addElement(symbol);
            symbolTextField.setText("");
        }
    }

    private void removeSelectedSymbol() {
        int[] indices = symbolList.getSelectedIndices();
        for (int i = indices.length - 1; i >= 0; i--) {
            symbolModel.remove(indices[i]);
        }
    }

    private void addSampleSymbols() {
        for (String symbol : SAMPLE_SYMBOLS) {
            if (!symbolModel.contains(symbol)) {
                symbolModel.addElement(symbol);
            }
        }
    }

    private void clearSymbols() {
        symbolModel.clear();
    }

    private void updateSymbolExclusivity() {
        symbolExclusiveCheckbox.setSelected(symbolExclusiveCheckbox.isSelected());
    }

    private void loadGlobalCustomIndicators() {
        if (mainApp != null) {
            availableCustomIndicators.clear();
            availableCustomIndicators.addAll(mainApp.getGlobalCustomIndicators());
        }
    }

    private void saveGlobalCustomIndicators() {
        if (mainApp != null) {
            mainApp.setGlobalCustomIndicators(new HashSet<>(availableCustomIndicators));
        }
    }

    private void manageCustomIndicators() {
        // Create a map structure for the custom indicators manager
        Map<String, Set<CustomIndicator>> allCustomIndicators = new HashMap<>();
        allCustomIndicators.put("global", new HashSet<>(availableCustomIndicators));

        CustomIndicatorsManager manager = new CustomIndicatorsManager(this, allCustomIndicators);
        manager.setVisible(true);

        if (manager.isSaved()) {
            // Update available custom indicators from manager
            Map<String, Set<CustomIndicator>> updatedIndicators = manager.getCustomIndicatorsMap();
            availableCustomIndicators.clear();

            for (Set<CustomIndicator> indicators : updatedIndicators.values()) {
                availableCustomIndicators.addAll(indicators);
            }

            // Save to global storage
            saveGlobalCustomIndicators();

            // Refresh all timeframe tabs
            refreshCustomIndicatorsInAllTabs();

            // Update the UI to show count
            updateCustomIndicatorsCount();
        }
    }

    private void updateCustomIndicatorsCount() {
        // This would update the count label in the basic settings panel
        // Implementation depends on your specific UI structure
    }

    private void refreshCustomIndicatorsInAllTabs() {
        for (String timeframe : ALL_TIMEFRAMES) {
            JList<CustomIndicator> customList = timeframeCustomIndicatorLists.get(timeframe);
            if (customList != null) {
                DefaultListModel<CustomIndicator> model = (DefaultListModel<CustomIndicator>) customList.getModel();
                model.clear();
                for (CustomIndicator indicator : availableCustomIndicators) {
                    model.addElement(indicator);
                }
            }
        }
    }

    private void populateFields() {
        if (strategyConfig.getName() != null) {
            nameField.setText(strategyConfig.getName());
        }
        enabledCheckbox.setSelected(strategyConfig.isEnabled());
        alarmEnabledCheckbox.setSelected(strategyConfig.isAlarmEnabled());
        symbolExclusiveCheckbox.setSelected(strategyConfig.isSymbolExclusive());
        
        // Set watchlist if available
        if (strategyConfig.getWatchlistName() != null && !strategyConfig.getWatchlistName().isEmpty()) {
            watchlistComboBox.setSelectedItem(strategyConfig.getWatchlistName());
        } else {
            watchlistComboBox.setSelectedItem(""); // No watchlist
        }

        // Set indicators for each timeframe
        for (String timeframe : ALL_TIMEFRAMES) {
            // Standard indicators
            JList<String> indicatorList = timeframeIndicatorLists.get(timeframe);
            if (indicatorList != null) {
                Set<String> configuredIndicators = strategyConfig.getIndicatorsForTimeframe(timeframe);
                if (!configuredIndicators.isEmpty()) {
                    List<Integer> selectedIndices = new ArrayList<>();
                    DefaultListModel<String> model = (DefaultListModel<String>) indicatorList.getModel();
                    for (int i = 0; i < model.size(); i++) {
                        if (configuredIndicators.contains(model.get(i))) {
                            selectedIndices.add(i);
                        }
                    }
                    int[] indices = selectedIndices.stream().mapToInt(i -> i).toArray();
                    indicatorList.setSelectedIndices(indices);
                }
            }

            // Custom indicators
            JList<CustomIndicator> customList = timeframeCustomIndicatorLists.get(timeframe);
            if (customList != null) {
                Set<CustomIndicator> configuredCustomIndicators = strategyConfig.getCustomIndicatorsForTimeframe(timeframe);
                if (!configuredCustomIndicators.isEmpty()) {
                    List<Integer> selectedIndices = new ArrayList<>();
                    DefaultListModel<CustomIndicator> model = (DefaultListModel<CustomIndicator>) customList.getModel();
                    for (int i = 0; i < model.size(); i++) {
                        if (configuredCustomIndicators.contains(model.get(i))) {
                            selectedIndices.add(i);
                        }
                    }
                    int[] indices = selectedIndices.stream().mapToInt(i -> i).toArray();
                    customList.setSelectedIndices(indices);
                }
            }
        }

        // Set symbols
        symbolModel.clear();
        for (String symbol : strategyConfig.getExclusiveSymbols()) {
            symbolModel.addElement(symbol);
        }

     // Load Z-Score weights and checkbox states if they exist
        if (strategyConfig.getParameters().containsKey("zscoreWeights")) {
            Object zscoreWeightsObj = strategyConfig.getParameters().get("zscoreWeights");
            Object zscoreEnabledTimeframesObj = strategyConfig.getParameters().get("zscoreEnabledTimeframes");
            
            Map<String, Integer> savedWeights = new HashMap<>();
            Map<String, Boolean> savedEnabledTimeframes = new HashMap<>();
            
            // Parse zscoreWeights
            if (zscoreWeightsObj instanceof Map) {
                savedWeights = (Map<String, Integer>) zscoreWeightsObj;
            } else if (zscoreWeightsObj instanceof String) {
                savedWeights = parseMapString((String) zscoreWeightsObj, Integer.class);
            }
            
            // Parse zscoreEnabledTimeframes
            if (zscoreEnabledTimeframesObj instanceof Map) {
                savedEnabledTimeframes = (Map<String, Boolean>) zscoreEnabledTimeframesObj;
            } else if (zscoreEnabledTimeframesObj instanceof String) {
                savedEnabledTimeframes = parseMapString((String) zscoreEnabledTimeframesObj, Boolean.class);
            }
            
            for (String timeframe : ALL_TIMEFRAMES) {
                JSpinner spinner = timeframeWeightSpinners.get(timeframe);
                JCheckBox checkbox = timeframeCheckboxes.get(timeframe);
                
                if (spinner != null && checkbox != null) {
                    Integer savedWeight = savedWeights.get(timeframe);
                    Boolean wasEnabled = savedEnabledTimeframes.get(timeframe);
                    
                    checkbox.setSelected(Boolean.TRUE.equals(wasEnabled));
                    spinner.setEnabled(Boolean.TRUE.equals(wasEnabled));
                    
                    if (savedWeight != null) {
                        spinner.setValue(savedWeight);
                    }
                }
            }
            updateWeightTotal();
        }
    }
    
 // Simple parser for string maps like "{30Min=25, 4H=25, 5Min=25, 1Min=25}"
    private <T> Map<String, T> parseMapString(String mapString, Class<T> valueType) {
        Map<String, T> result = new HashMap<>();
        
        if (mapString == null || !mapString.startsWith("{") || !mapString.endsWith("}")) {
            return result;
        }
        
        try {
            // Remove braces and split by commas
            String content = mapString.substring(1, mapString.length() - 1);
            String[] pairs = content.split(",\\s*");
            
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String valueStr = keyValue[1].trim();
                    
                    if (valueType == Integer.class) {
                        result.put(key, valueType.cast(Integer.parseInt(valueStr)));
                    } else if (valueType == Boolean.class) {
                        result.put(key, valueType.cast(Boolean.parseBoolean(valueStr)));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing map string: " + mapString);
        }
        
        return result;
    }

    private void saveStrategy(Set<String> existingKeys) {
        // Validate inputs
        String strategyName = nameField.getText().trim();
        if (strategyName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a strategy name.");
            return;
        }

        // Check if at least one timeframe has indicators selected (standard or custom)
        boolean hasTimeframesWithIndicators = false;
        Map<String, Set<String>> timeframeIndicators = new HashMap<>();
        Map<String, Set<CustomIndicator>> timeframeCustomIndicators = new HashMap<>();

        for (String timeframe : ALL_TIMEFRAMES) {
            // Check standard indicators
            JList<String> indicatorList = timeframeIndicatorLists.get(timeframe);
            boolean hasStandardIndicators = indicatorList != null && indicatorList.getSelectedIndices().length > 0;

            // Check custom indicators
            JList<CustomIndicator> customList = timeframeCustomIndicatorLists.get(timeframe);
            boolean hasCustomIndicators = customList != null && customList.getSelectedIndices().length > 0;

            if (hasStandardIndicators || hasCustomIndicators) {
                hasTimeframesWithIndicators = true;

                // Add standard indicators
                if (hasStandardIndicators) {
                    Set<String> selectedIndicators = new HashSet<>();
                    for (int index : indicatorList.getSelectedIndices()) {
                        selectedIndicators.add(indicatorList.getModel().getElementAt(index));
                    }
                    timeframeIndicators.put(timeframe, selectedIndicators);
                }

                // Add custom indicators
                if (hasCustomIndicators) {
                    Set<CustomIndicator> selectedCustomIndicators = new HashSet<>();
                    for (int index : customList.getSelectedIndices()) {
                        selectedCustomIndicators.add(customList.getModel().getElementAt(index));
                    }
                    timeframeCustomIndicators.put(timeframe, selectedCustomIndicators);
                }
            }
        }

        if (!hasTimeframesWithIndicators) {
            JOptionPane.showMessageDialog(this,
                "Please select indicators (standard or custom) for at least one timeframe.");
            return;
        }

        // Check for unique strategy-timeframe combinations
        Set<String> newKeys = new HashSet<>();
        for (String timeframe : timeframeIndicators.keySet()) {
            String key = strategyName + "|" + timeframe;
            newKeys.add(key);

            // If editing, exclude the current strategy's own keys
            if (existingKeys.contains(key)
                    && (strategyConfig.getName() == null || !key.startsWith(strategyConfig.getName() + "|"))) {
                JOptionPane.showMessageDialog(this,
                    "Strategy '" + strategyName + "' already exists for timeframe '" + timeframe + "'.\n"
                            + "Please choose a different name or remove this timeframe.",
                    "Duplicate Strategy-Timeframe", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Also check custom indicator timeframes
        for (String timeframe : timeframeCustomIndicators.keySet()) {
            String key = strategyName + "|" + timeframe;
            newKeys.add(key);

            if (existingKeys.contains(key)
                    && (strategyConfig.getName() == null || !key.startsWith(strategyConfig.getName() + "|"))) {
                JOptionPane.showMessageDialog(this,
                    "Strategy '" + strategyName + "' already exists for timeframe '" + timeframe + "'.\n"
                            + "Please choose a different name or remove this timeframe.",
                    "Duplicate Strategy-Timeframe", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Validate Z-Score weights if configured
        Map<String, ZScoreCalculator.TimeframeWeight> weights = getCurrentWeights();
        int totalWeight = 0;
        for (ZScoreCalculator.TimeframeWeight weight : weights.values()) {
            totalWeight += weight.getWeight();
        }
        
        // Only validate if there are checked timeframes
        boolean hasCheckedTimeframes = !weights.isEmpty();
        
        if (hasCheckedTimeframes && totalWeight != 100) {
            int result = JOptionPane.showConfirmDialog(this,
                "Z-Score weights total " + totalWeight + " (must be 100 for checked timeframes).\n" +
                "Do you want to continue without valid Z-Score configuration?",
                "Z-Score Weights Warning",
                JOptionPane.YES_NO_OPTION);
                
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Update strategy config
        strategyConfig.setName(strategyName);
        strategyConfig.setEnabled(enabledCheckbox.isSelected());
        strategyConfig.setAlarmEnabled(alarmEnabledCheckbox.isSelected());
        strategyConfig.setSymbolExclusive(symbolExclusiveCheckbox.isSelected());
        strategyConfig.setTimeframeIndicators(timeframeIndicators);
        strategyConfig.setTimeframeCustomIndicators(timeframeCustomIndicators);
        
        // Set watchlist (can be null/empty)
        String selectedWatchlist = (String) watchlistComboBox.getSelectedItem();
        if (selectedWatchlist != null && !selectedWatchlist.trim().isEmpty()) {
            strategyConfig.setWatchlistName(selectedWatchlist.trim());
        } else {
            strategyConfig.setWatchlistName(null);
        }

        Set<String> selectedSymbols = new HashSet<>();
        for (int i = 0; i < symbolModel.size(); i++) {
            selectedSymbols.add(symbolModel.get(i));
        }
        strategyConfig.setExclusiveSymbols(selectedSymbols);

        // Store Z-Score weights and enabled timeframes in strategy config
        Map<String, Integer> zscoreWeights = new HashMap<>();
        Map<String, Boolean> zscoreEnabledTimeframes = new HashMap<>();
        
        for (String timeframe : ALL_TIMEFRAMES) {
            JSpinner spinner = timeframeWeightSpinners.get(timeframe);
            JCheckBox checkbox = timeframeCheckboxes.get(timeframe);
            if (spinner != null && checkbox != null) {
                boolean enabled = checkbox.isSelected();
                zscoreEnabledTimeframes.put(timeframe, enabled);
                
                if (enabled) {
                    zscoreWeights.put(timeframe, (Integer) spinner.getValue());
                }
            }
        }
        
        strategyConfig.getParameters().put("zscoreWeights", zscoreWeights);
        strategyConfig.getParameters().put("zscoreEnabledTimeframes", zscoreEnabledTimeframes);

        saved = true;
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }

    public StrategyConfig getStrategyConfig() {
        return strategyConfig;
    }

    // Custom renderer for custom indicators list
    private class CustomIndicatorRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
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