package com.quantlabs.stockApp.indicator.management;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractButton;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.json.JSONObject;

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
    private JCheckBox enableZScoreCheckbox;
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

        // Add tab change listener
        mainTabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int selectedIndex = mainTabbedPane.getSelectedIndex();
                String selectedTitle = mainTabbedPane.getTitleAt(selectedIndex);
                
                if ("Z-Score".equals(selectedTitle)) {
                    // Refresh indicator weights buttons when Z-Score tab is selected
                    refreshAllIndicatorWeightsButtons();
                }
            }
        });

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
        
        // Add listener to update Z-Score button states when selections change
        indicatorList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateIndicatorWeightsButtonsForTimeframe(timeframe);
            }
        });
        
        timeframeIndicatorLists.put(timeframe, indicatorList);

        JScrollPane indicatorScroll = new JScrollPane(indicatorList);
        indicatorScroll.setBorder(BorderFactory.createTitledBorder("Standard Indicators for " + timeframe));

        // Control buttons for standard indicators
        JPanel standardControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectAllStandardButton = new JButton("Select All");
        JButton clearAllStandardButton = new JButton("Clear All");

        selectAllStandardButton.addActionListener(e -> {
            indicatorList.setSelectionInterval(0, indicatorModel.size() - 1);
            updateIndicatorWeightsButtonsForTimeframe(timeframe);
        });
        clearAllStandardButton.addActionListener(e -> {
            indicatorList.clearSelection();
            updateIndicatorWeightsButtonsForTimeframe(timeframe);
        });

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
        
        // Add listener to update Z-Score button states when selections change
        customIndicatorList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateIndicatorWeightsButtonsForTimeframe(timeframe);
            }
        });
        
        timeframeCustomIndicatorLists.put(timeframe, customIndicatorList);

        JScrollPane customIndicatorScroll = new JScrollPane(customIndicatorList);
        customIndicatorScroll.setBorder(BorderFactory.createTitledBorder("Custom Indicators for " + timeframe));

        // Control buttons for custom indicators
        JPanel customControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectAllCustomButton = new JButton("Select All");
        JButton clearAllCustomButton = new JButton("Clear All");

        selectAllCustomButton.addActionListener(e -> {
            customIndicatorList.setSelectionInterval(0, customIndicatorModel.size() - 1);
            updateIndicatorWeightsButtonsForTimeframe(timeframe);
        });
        clearAllCustomButton.addActionListener(e -> {
            customIndicatorList.clearSelection();
            updateIndicatorWeightsButtonsForTimeframe(timeframe);
        });

        customControlPanel.add(selectAllCustomButton);
        customControlPanel.add(clearAllCustomButton);

        customPanel.add(customIndicatorScroll, BorderLayout.CENTER);
        customPanel.add(customControlPanel, BorderLayout.SOUTH);

        splitPane.setTopComponent(standardPanel);
        splitPane.setBottomComponent(customPanel);

        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }
    
    /**
     * Update indicator weights button for a specific timeframe when indicators change
     */
    private void updateIndicatorWeightsButtonsForTimeframe(String timeframe) {
        if (timeframeCheckboxes == null || timeframeWeightSpinners == null) {
            return; // Components not initialized yet
        }
        
        JCheckBox checkbox = timeframeCheckboxes.get(timeframe);
        if (checkbox != null) {
            // Find the button for this timeframe and update its state
            updateIndicatorWeightsButtonForTimeframe(timeframe, checkbox);
        }
    }

    /**
     * Update a specific timeframe's indicator weights button
     */
    private void updateIndicatorWeightsButtonForTimeframe(String timeframe, JCheckBox checkbox) {
        // Since we don't have direct references to the buttons, we need to find them
        // This is a safer approach that doesn't traverse the component hierarchy
        
        boolean timeframeEnabled = checkbox.isSelected();
        boolean hasIndicators = hasTimeframeIndicators(timeframe);
        
        // The button state will be updated when the user interacts with it
        // The actual UI update happens in the checkbox listener that's already set up
        
        // Log for debugging (remove in production)
        System.out.println("Timeframe " + timeframe + ": enabled=" + timeframeEnabled + ", hasIndicators=" + hasIndicators);
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
        
        // Enable checkbox at the top
        JPanel enablePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        enableZScoreCheckbox = new JCheckBox("Enable Z-Score Calculation"); // Initialize the field here
        enableZScoreCheckbox.setSelected(true); // Default to enabled
        enableZScoreCheckbox.addActionListener(e -> {
            boolean enabled = enableZScoreCheckbox.isSelected();
            setZScoreTabEnabled(panel, enabled);
        });
        enablePanel.add(enableZScoreCheckbox);
        
        // Info label
        JLabel infoLabel = new JLabel(
            "<html><b>Z-Score Configuration for Current Strategy</b><br>" +
            "Check timeframes to include in Z-Score calculation and configure weights (0-100).<br>" +
            "Only checked timeframes must total 100.</html>"
        );
        infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Timeframe weights panel
        JPanel weightsPanel = createTimeframeWeightsPanel();
        
        // Preview and controls panel
        JPanel previewPanel = createPreviewPanel();
        
        // Add components to main panel
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.add(infoLabel, BorderLayout.NORTH);
        contentPanel.add(weightsPanel, BorderLayout.CENTER);
        contentPanel.add(previewPanel, BorderLayout.SOUTH);
        
        panel.add(enablePanel, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Enable or disable all Z-Score tab components
     */
    private void setZScoreTabEnabled(Container container, boolean enabled) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JCheckBox && comp != enableZScoreCheckbox) {
                ((JCheckBox) comp).setEnabled(enabled);
            } else if (comp instanceof JSpinner) {
                ((JSpinner) comp).setEnabled(enabled);
            } else if (comp instanceof JButton) {
                ((JButton) comp).setEnabled(enabled);
            } else if (comp instanceof JLabel) {
                // Keep labels enabled for readability, but change color
                comp.setForeground(enabled ? Color.BLACK : Color.GRAY);
            } else if (comp instanceof Container) {
                setZScoreTabEnabled((Container) comp, enabled);
            }
        }
        
        // Also update the preview area
        if (scorePreviewArea != null) {
            scorePreviewArea.setEnabled(enabled);
            scorePreviewArea.setForeground(enabled ? Color.BLACK : Color.GRAY);
        }
        
        // Update validation label
        if (validationLabel != null) {
            if (!enabled) {
                validationLabel.setText("Z-Score calculation disabled");
                validationLabel.setForeground(Color.GRAY);
            } else {
                validationLabel.setText("");
                validationLabel.setForeground(Color.RED);
                updateWeightTotal(); // Refresh the display
            }
        }
        
        // Store the enabled state in strategy config
        strategyConfig.getParameters().put("zscoreEnabled", enabled);
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
            gbc.gridx = 0; gbc.gridy = row;
            JCheckBox timeframeCheckbox = new JCheckBox();
            timeframeCheckbox.addActionListener(e -> onTimeframeCheckboxChanged(timeframe, timeframeCheckbox));
            timeframeCheckboxes.put(timeframe, timeframeCheckbox);
            panel.add(timeframeCheckbox, gbc);
            
            // Timeframe label
            gbc.gridx = 1;
            JLabel timeframeLabel = new JLabel(timeframe + ":");
            panel.add(timeframeLabel, gbc);
            
            // Weight spinner
            gbc.gridx = 2;
            JSpinner weightSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
            weightSpinner.setEnabled(false); // Initially disabled until checkbox is checked
            weightSpinner.addChangeListener(e -> updateWeightTotal());
            timeframeWeightSpinners.put(timeframe, weightSpinner);
            panel.add(weightSpinner, gbc);
            
            // Indicator Weights Button
            gbc.gridx = 3;
            JButton indicatorWeightsButton = new JButton("⚙️"); // Gear icon
            indicatorWeightsButton.setToolTipText("Configure indicator weights for " + timeframe);
            
            // Initially enable button - we'll update state dynamically
            indicatorWeightsButton.setEnabled(true);
            
            // Update button state when checkbox changes
            timeframeCheckbox.addActionListener(e -> {
                updateIndicatorWeightsButtonState(timeframe, indicatorWeightsButton, timeframeCheckbox);
            });
            
            indicatorWeightsButton.addActionListener(e -> {
                if (timeframeCheckbox.isSelected() && hasTimeframeIndicators(timeframe)) {
                    showIndicatorWeightsDialog(timeframe);
                } else if (!timeframeCheckbox.isSelected()) {
                    JOptionPane.showMessageDialog(this, 
                        "Please enable timeframe '" + timeframe + "' first before configuring indicator weights.",
                        "Timeframe Not Enabled", 
                        JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "No indicators configured for timeframe: " + timeframe + "\n" +
                        "Please add indicators in the 'Timeframe Indicators' tab first.",
                        "No Indicators", 
                        JOptionPane.WARNING_MESSAGE);
                }
            });
            
            panel.add(indicatorWeightsButton, gbc);
            
            row++;
        }
        
        // Total weight display
        gbc.gridx = 0; gbc.gridy = row;
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
        
        // Update initial button states
        //updateAllIndicatorWeightsButtons();
        
        return panel;
    }
    
    /**
     * Update the state of all indicator weights buttons
     */
    private void updateAllIndicatorWeightsButtons() {
        for (String timeframe : ALL_TIMEFRAMES) {
            JCheckBox checkbox = timeframeCheckboxes.get(timeframe);
            if (checkbox != null) {
                // Find the button for this timeframe (it's the 4th component in the row)
                Component[] components = ((JPanel) getContentPane().getComponent(0)).getComponents();
                for (Component comp : components) {
                    if (comp instanceof JPanel) {
                        JPanel mainPanel = (JPanel) comp;
                        Component[] mainComponents = mainPanel.getComponents();
                        for (Component mainComp : mainComponents) {
                            if (mainComp instanceof JTabbedPane) {
                                JTabbedPane tabbedPane = (JTabbedPane) mainComp;
                                Component zscoreTab = tabbedPane.getComponentAt(4); // Z-Score tab index
                                if (zscoreTab instanceof JPanel) {
                                    JPanel zscorePanel = (JPanel) zscoreTab;
                                    // We need to find the button for this specific timeframe
                                    // This is complex, so we'll use a different approach
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Update the state of a specific indicator weights button
     */
    private void updateIndicatorWeightsButtonState(String timeframe, JButton button, JCheckBox checkbox) {
        boolean timeframeEnabled = checkbox.isSelected();
        boolean hasIndicators = hasTimeframeIndicators(timeframe);
        
        button.setEnabled(timeframeEnabled && hasIndicators);
        
        // Update tooltip based on state
        if (!timeframeEnabled) {
            button.setToolTipText("Enable timeframe '" + timeframe + "' first to configure indicator weights");
        } else if (!hasIndicators) {
            button.setToolTipText("No indicators configured for timeframe '" + timeframe + "' - add indicators first");
        } else {
            button.setToolTipText("Configure indicator weights for " + timeframe);
        }
    }

    /**
     * Update all indicator weights buttons when timeframe indicators change
     * Call this method when the user changes indicator selections in the Timeframe Indicators tab
     */
    public void updateIndicatorWeightsButtons() {
        for (String timeframe : ALL_TIMEFRAMES) {
            JCheckBox checkbox = timeframeCheckboxes.get(timeframe);
            if (checkbox != null) {
                // We need to find the button for this timeframe
                // Since we can't easily get the button reference, we'll update the state
                // when the user tries to click it or when the checkbox state changes
            }
        }
    }
    
    private void showIndicatorWeightsDialog(String timeframe) {
        IndicatorWeightsDialog dialog = new IndicatorWeightsDialog(this, timeframe, strategyConfig);
        dialog.setVisible(true);
        
        if (dialog.isSaved()) {
            // Update strategy config with indicator weights
            Map<String, Integer> indicatorWeights = dialog.getIndicatorWeights();
            strategyConfig.getParameters().put("indicatorWeights_" + timeframe, indicatorWeights);
            
            // Update preview if needed
            calculateScorePreview();
        }
    }

 // Inner class for Indicator Weights Dialog
    private class IndicatorWeightsDialog extends JDialog {
        private String timeframe;
        private StrategyConfig strategyConfig;
        private boolean saved = false;
        private Map<String, JSpinner> indicatorSpinners;
        
        // Store references to UI components directly
        private JLabel totalValueLabel;
        private JLabel validationLabel;
        
        public IndicatorWeightsDialog(JDialog parent, String timeframe, StrategyConfig strategyConfig) {
            super(parent, "Indicator Weights - " + timeframe, true);
            this.timeframe = timeframe;
            this.strategyConfig = strategyConfig;
            this.indicatorSpinners = new HashMap<>();
            this.totalValueLabel = new JLabel("0");
            this.validationLabel = new JLabel("");
            
            initializeUI();
            pack();
            setLocationRelativeTo(parent);
            setSize(400, 500);
        }
        
        private void initializeUI() {
            setLayout(new BorderLayout(10, 10));
            
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // Info label
            JLabel infoLabel = new JLabel(
                "<html><b>Configure Indicator Weights for " + timeframe + "</b><br>" +
                "Set weights for each indicator (0-100). Total must be 100.</html>"
            );
            infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            mainPanel.add(infoLabel, BorderLayout.NORTH);
            
            // Indicators panel
            JPanel indicatorsPanel = createIndicatorsPanel();
            JScrollPane scrollPane = new JScrollPane(indicatorsPanel);
            scrollPane.setBorder(BorderFactory.createTitledBorder("Indicators"));
            mainPanel.add(scrollPane, BorderLayout.CENTER);
            
            // Control panel
            JPanel controlPanel = createControlPanel();
            mainPanel.add(controlPanel, BorderLayout.SOUTH);
            
            add(mainPanel, BorderLayout.CENTER);
            
            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton saveButton = new JButton("Save");
            JButton cancelButton = new JButton("Cancel");
            
            saveButton.addActionListener(e -> saveWeights());
            cancelButton.addActionListener(e -> dispose());
            
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            
            add(buttonPanel, BorderLayout.SOUTH);
            
            // Initialize weights after UI is fully built
            loadExistingWeights();
        }
        
        private JPanel createIndicatorsPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(3, 3, 3, 3);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            
            // Get indicators for this timeframe
            Set<String> standardIndicators = strategyConfig.getIndicatorsForTimeframe(timeframe);
            Set<CustomIndicator> customIndicators = strategyConfig.getCustomIndicatorsForTimeframe(timeframe);
            
            int row = 0;
            
            // Add standard indicators
            if (!standardIndicators.isEmpty()) {
                gbc.gridx = 0; gbc.gridy = row++;
                gbc.gridwidth = 3;
                JLabel standardLabel = new JLabel("Standard Indicators:");
                standardLabel.setFont(standardLabel.getFont().deriveFont(Font.BOLD));
                panel.add(standardLabel, gbc);
                
                for (String indicator : standardIndicators) {
                    gbc.gridx = 0; gbc.gridy = row;
                    gbc.gridwidth = 1;
                    panel.add(new JLabel(indicator + ":"), gbc);
                    
                    gbc.gridx = 1;
                    JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
                    spinner.addChangeListener(e -> updateTotalWeights());
                    indicatorSpinners.put("STANDARD_" + indicator, spinner);
                    panel.add(spinner, gbc);
                    
                    gbc.gridx = 2;
                    JLabel percentLabel = new JLabel("%");
                    panel.add(percentLabel, gbc);
                    
                    row++;
                }
            }
            
            // Add custom indicators
            if (!customIndicators.isEmpty()) {
                gbc.gridx = 0; gbc.gridy = row++;
                gbc.gridwidth = 3;
                JLabel customLabel = new JLabel("Custom Indicators:");
                customLabel.setFont(customLabel.getFont().deriveFont(Font.BOLD));
                panel.add(customLabel, gbc);
                
                for (CustomIndicator indicator : customIndicators) {
                    gbc.gridx = 0; gbc.gridy = row;
                    gbc.gridwidth = 1;
                    
                    // Use display name for better readability
                    String displayName = indicator.getDisplayName();
                    JLabel indicatorLabel = new JLabel(displayName + ":");
                    indicatorLabel.setToolTipText("Type: " + indicator.getType() + ", Parameters: " + indicator.getParameters());
                    panel.add(indicatorLabel, gbc);
                    
                    gbc.gridx = 1;
                    JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
                    spinner.addChangeListener(e -> updateTotalWeights());
                    
                    // Use the actual indicator name as key, not display name
                    indicatorSpinners.put("CUSTOM_" + indicator.getName(), spinner);
                    panel.add(spinner, gbc);
                    
                    gbc.gridx = 2;
                    JLabel percentLabel = new JLabel("%");
                    panel.add(percentLabel, gbc);
                    
                    row++;
                }
            }
            
            // If no indicators are configured
            if (standardIndicators.isEmpty() && customIndicators.isEmpty()) {
                gbc.gridx = 0; gbc.gridy = row++;
                gbc.gridwidth = 3;
                JLabel noIndicatorsLabel = new JLabel("No indicators configured for this timeframe.");
                noIndicatorsLabel.setForeground(Color.GRAY);
                noIndicatorsLabel.setFont(noIndicatorsLabel.getFont().deriveFont(Font.ITALIC));
                panel.add(noIndicatorsLabel, gbc);
            }
            
            // Add filler
            gbc.gridx = 0; gbc.gridy = row;
            gbc.gridwidth = 3;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            panel.add(new JPanel(), gbc);
            
            return panel;
        }
        
        private JPanel createControlPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            
            // Total weights display
            JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JLabel totalLabel = new JLabel("Total Weight: ");
            totalValueLabel.setFont(totalValueLabel.getFont().deriveFont(Font.BOLD));
            totalPanel.add(totalLabel);
            totalPanel.add(totalValueLabel);
            
            // Validation label
            validationLabel.setForeground(Color.RED);
            
            // Control buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton autoDistributeButton = new JButton("Auto-Distribute");
            JButton clearAllButton = new JButton("Clear All");
            
            autoDistributeButton.addActionListener(e -> autoDistributeWeights());
            clearAllButton.addActionListener(e -> clearAllWeights());
            
            buttonPanel.add(autoDistributeButton);
            buttonPanel.add(clearAllButton);
            
            panel.add(totalPanel, BorderLayout.NORTH);
            panel.add(validationLabel, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);
            
            return panel;
        }
        
       private void loadLegacyWeights(Object weightsObj) {
            if (weightsObj instanceof String) {
                // Try to parse as Map string
                String weightsString = (String) weightsObj;
                if (weightsString.trim().startsWith("{") && weightsString.trim().endsWith("}")) {
                    try {
                        JSONObject weightsJson = parseMapStringToJSON(weightsString);
                        
                        // Load from the parsed JSON
                        for (Map.Entry<String, JSpinner> entry : indicatorSpinners.entrySet()) {
                            String indicatorKey = entry.getKey();
                            if (weightsJson.has(indicatorKey)) {
                                int weight = weightsJson.getInt(indicatorKey);
                                entry.getValue().setValue(weight);
                            }
                        }
                        return;
                    } catch (Exception e) {
                        System.err.println("Failed to parse legacy weights string: " + e.getMessage());
                    }
                }
            } else if (weightsObj instanceof Map) {
                // Handle Map object
                @SuppressWarnings("unchecked")
                Map<String, Integer> weightsMap = (Map<String, Integer>) weightsObj;
                
                for (Map.Entry<String, Integer> entry : weightsMap.entrySet()) {
                    String originalKey = entry.getKey();
                    JSpinner matchingSpinner = findMatchingSpinner(originalKey);
                    if (matchingSpinner != null) {
                        matchingSpinner.setValue(entry.getValue());
                    }
                }
            }
        }
               
        private void updateTotalWeights() {
            int total = 0;
            for (JSpinner spinner : indicatorSpinners.values()) {
                total += (Integer) spinner.getValue();
            }
            
            totalValueLabel.setText(total + "");
            
            if (indicatorSpinners.isEmpty()) {
                validationLabel.setText("ℹ️ No indicators configured for this timeframe");
                validationLabel.setForeground(Color.BLUE);
            } else if (total == 100) {
                validationLabel.setText("✓ Weights valid - total is 100");
                validationLabel.setForeground(Color.GREEN);
            } else {
                validationLabel.setText("✗ Weights invalid - total must be 100 (current: " + total + ")");
                validationLabel.setForeground(Color.RED);
            }
        }
        
        private void autoDistributeWeights() {
            int indicatorCount = indicatorSpinners.size();
            if (indicatorCount == 0) {
                JOptionPane.showMessageDialog(this, 
                    "No indicators are configured for this timeframe.", 
                    "No Indicators", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int baseWeight = 100 / indicatorCount;
            int remainder = 100 % indicatorCount;
            
            int count = 0;
            for (JSpinner spinner : indicatorSpinners.values()) {
                int weight = baseWeight + (count < remainder ? 1 : 0);
                spinner.setValue(weight);
                count++;
            }
            updateTotalWeights();
        }
        
        private void clearAllWeights() {
            for (JSpinner spinner : indicatorSpinners.values()) {
                spinner.setValue(0);
            }
            updateTotalWeights();
        }
        
        private void saveWeights() {
            int total = 0;
            for (JSpinner spinner : indicatorSpinners.values()) {
                total += (Integer) spinner.getValue();
            }
            
            if (total != 100) {
                int result = JOptionPane.showConfirmDialog(this,
                    "Total weights are " + total + " (must be 100). Save anyway?",
                    "Invalid Weights", JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            // Use HashMap to store weights
            Map<String, Integer> weightsMap = new HashMap<>();
            for (Map.Entry<String, JSpinner> entry : indicatorSpinners.entrySet()) {
                weightsMap.put(entry.getKey(), (Integer) entry.getValue().getValue());
            }
            
            // Store as HashMap directly
            strategyConfig.getParameters().put("indicatorWeights_" + timeframe, weightsMap);
            
            saved = true;
            dispose();
        }
        
        private void loadExistingWeights() {
            Object weightsObj = strategyConfig.getParameters().get("indicatorWeights_" + timeframe);
            
            if (weightsObj != null) {
                try {
                    Map<String, Integer> weightsMap;
                    
                    if (weightsObj instanceof Map) {
                        // Already a Map
                        @SuppressWarnings("unchecked")
                        Map<String, Integer> existingMap = (Map<String, Integer>) weightsObj;
                        weightsMap = existingMap;
                    } else if (weightsObj instanceof String) {
                        // Handle legacy String format
                        weightsMap = parseWeightsStringToMap((String) weightsObj);
                        // Migrate to HashMap
                        strategyConfig.getParameters().put("indicatorWeights_" + timeframe, weightsMap);
                    } else {
                        weightsMap = new HashMap<>();
                    }
                    
                    // Load weights from HashMap
                    for (Map.Entry<String, JSpinner> entry : indicatorSpinners.entrySet()) {
                        String indicatorKey = entry.getKey();
                        if (weightsMap.containsKey(indicatorKey)) {
                            Integer weight = weightsMap.get(indicatorKey);
                            if (weight != null) {
                                entry.getValue().setValue(weight);
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error loading indicator weights for " + timeframe + ": " + e.getMessage());
                    // Fallback: try flexible matching
                    loadWeightsWithFlexibleMatching(weightsObj);
                }
            }
            updateTotalWeights();
        }
        
        private void loadWeightsWithFlexibleMatching(Object weightsObj) {
            Map<String, Integer> weightsMap = convertWeightsObjectToMap(weightsObj);
            
            for (Map.Entry<String, Integer> weightEntry : weightsMap.entrySet()) {
                String originalKey = weightEntry.getKey();
                JSpinner matchingSpinner = findMatchingSpinner(originalKey);
                if (matchingSpinner != null) {
                    matchingSpinner.setValue(weightEntry.getValue());
                }
            }
        }
        
        private Map<String, Integer> convertWeightsObjectToMap(Object weightsObj) {
            if (weightsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Integer> weightsMap = (Map<String, Integer>) weightsObj;
                return weightsMap;
            } else if (weightsObj instanceof String) {
                return parseWeightsStringToMap((String) weightsObj);
            }
            return new HashMap<>();
        }
        
        // Keep the parseWeightsStringToMap method for legacy data
        private Map<String, Integer> parseWeightsStringToMap(String weightsString) {
            Map<String, Integer> weightsMap = new HashMap<>();
            
            if (weightsString == null || weightsString.trim().isEmpty()) {
                return weightsMap;
            }
            
            try {
                String content = weightsString.trim();
                if (content.startsWith("{")) {
                    content = content.substring(1);
                }
                if (content.endsWith("}")) {
                    content = content.substring(0, content.length() - 1);
                }
                content = content.trim();
                
                if (content.isEmpty()) {
                    return weightsMap;
                }
                
                List<String> entries = splitMapEntries(content);
                
                for (String entry : entries) {
                    String[] keyValue = entry.split("=", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String valueStr = keyValue[1].trim();
                        
                        try {
                            int value = Integer.parseInt(valueStr);
                            weightsMap.put(key, value);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid weight value: " + valueStr + " for key: " + key);
                        }
                    }
                }
                
            } catch (Exception e) {
                System.err.println("Error parsing weights string: " + weightsString + " - " + e.getMessage());
            }
            
            return weightsMap;
        }
        
        // Keep the flexible matching method
        private JSpinner findMatchingSpinner(String searchKey) {
            String cleanSearchKey = searchKey.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            
            for (Map.Entry<String, JSpinner> entry : indicatorSpinners.entrySet()) {
                String cleanEntryKey = entry.getKey().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                if (cleanEntryKey.equals(cleanSearchKey)) {
                    return entry.getValue();
                }
            }
            return null;
        }
        
        public Map<String, Integer> getIndicatorWeights() {
            Map<String, Integer> weights = new HashMap<>();
            for (Map.Entry<String, JSpinner> entry : indicatorSpinners.entrySet()) {
                weights.put(entry.getKey(), (Integer) entry.getValue().getValue());
            }
            return weights;
        }
        
        public boolean isSaved() {
            return saved;
        }
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
    
    /**
     * Set all timeframes selected (only those with indicators)
     */
    private void setAllTimeframesSelected(boolean selected) {
        for (Map.Entry<String, JCheckBox> entry : timeframeCheckboxes.entrySet()) {
            String timeframe = entry.getKey();
            JCheckBox checkbox = entry.getValue();
            
            // Only select if timeframe has indicators
            boolean hasIndicators = hasTimeframeIndicators(timeframe);
            if (hasIndicators) {
                checkbox.setSelected(selected);
                
                JSpinner spinner = timeframeWeightSpinners.get(timeframe);
                if (spinner != null) {
                    spinner.setEnabled(selected);
                    if (!selected) {
                        spinner.setValue(0);
                    }
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
    	
    	// Check if Z-Score is enabled
        if (!enableZScoreCheckbox.isSelected()) {
            scorePreviewArea.setText("Z-Score calculation is disabled.\n" +
                                   "Enable the checkbox above to calculate Z-Scores.");
            return;
        }
    	
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

        CustomIndicatorsManager manager = new CustomIndicatorsManager(this, allCustomIndicators, mainApp);
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
    	
    	// Migrate any existing data to HashMap format
        migrateStrategyWeightsToHashMap(strategyConfig);
        migrateZScoreParametersToHashMap(strategyConfig);
        
        // Migrate Z-Score parameters from String to Map
        migrateZScoreParameters(strategyConfig);
        
        // Load Z-Score enabled state - only if checkbox is initialized
        if (enableZScoreCheckbox != null) {
            boolean zscoreEnabled = (boolean) strategyConfig.getParameters()
                .getOrDefault("zscoreEnabled", true);
            enableZScoreCheckbox.setSelected(zscoreEnabled);
            
            // Apply enabled state to the tab
            if (timeframeTabs != null) {
                Component zscoreTab = null;
                for (int i = 0; i < timeframeTabs.getTabCount(); i++) {
                    if ("Z-Score".equals(timeframeTabs.getTitleAt(i))) {
                        zscoreTab = timeframeTabs.getComponentAt(i);
                        break;
                    }
                }
                if (zscoreTab instanceof JPanel) {
                    setZScoreTabEnabled((JPanel) zscoreTab, zscoreEnabled);
                }
            }
        }
    	
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
                        CustomIndicator listIndicator = model.get(i);
                        // Find matching custom indicator by name
                        boolean found = configuredCustomIndicators.stream()
                            .anyMatch(configured -> configured.getName().equals(listIndicator.getName()));
                        if (found) {
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
            }
            
            // Parse zscoreEnabledTimeframes
            if (zscoreEnabledTimeframesObj instanceof Map) {
                savedEnabledTimeframes = (Map<String, Boolean>) zscoreEnabledTimeframesObj;
            }
            
            for (String timeframe : ALL_TIMEFRAMES) {
                JSpinner spinner = timeframeWeightSpinners.get(timeframe);
                JCheckBox checkbox = timeframeCheckboxes.get(timeframe);
                
                if (spinner != null && checkbox != null) {
                    Integer savedWeight = savedWeights.get(timeframe);
                    Boolean wasEnabled = savedEnabledTimeframes.get(timeframe);
                    
                    // Only enable checkbox if timeframe has indicators
                    boolean hasIndicators = hasTimeframeIndicators(timeframe);
                    checkbox.setEnabled(hasIndicators);
                    
                    if (hasIndicators && Boolean.TRUE.equals(wasEnabled)) {
                        checkbox.setSelected(true);
                        spinner.setEnabled(true);
                    } else {
                        checkbox.setSelected(false);
                        spinner.setEnabled(false);
                    }
                    
                    if (savedWeight != null) {
                        spinner.setValue(savedWeight);
                    } else {
                        spinner.setValue(0);
                    }
                }
            }
            updateWeightTotal();
        } else {
            // Initialize checkboxes based on whether timeframe has indicators
            for (String timeframe : ALL_TIMEFRAMES) {
                JSpinner spinner = timeframeWeightSpinners.get(timeframe);
                JCheckBox checkbox = timeframeCheckboxes.get(timeframe);
                
                if (spinner != null && checkbox != null) {
                    boolean hasIndicators = hasTimeframeIndicators(timeframe);
                    checkbox.setEnabled(hasIndicators);
                    checkbox.setSelected(false);
                    spinner.setEnabled(false);
                    spinner.setValue(0);
                }
            }
            updateWeightTotal();
        }
        
        // Load indicator weights for each timeframe
        /*for (String timeframe : ALL_TIMEFRAMES) {
            Map<String, Integer> indicatorWeights = (Map<String, Integer>) 
                strategyConfig.getParameters().get("indicatorWeights_" + timeframe);
            
            if (indicatorWeights != null && !indicatorWeights.isEmpty()) {
                // Store the indicator weights - they will be loaded when the dialog opens
                // The actual loading happens in the IndicatorWeightsDialog.loadExistingWeights()
            }
        }*/
        
     // Load Z-Score weights and checkbox states - they should now be HashMaps
        Object zscoreWeightsObj = strategyConfig.getParameters().get("zscoreWeights");
        Object zscoreEnabledTimeframesObj = strategyConfig.getParameters().get("zscoreEnabledTimeframes");
        
        Map<String, Integer> savedWeights = new HashMap<>();
        Map<String, Boolean> savedEnabledTimeframes = new HashMap<>();
        
        if (zscoreWeightsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> weightsMap = (Map<String, Integer>) zscoreWeightsObj;
            savedWeights = weightsMap;
        }
        
        if (zscoreEnabledTimeframesObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Boolean> timeframesMap = (Map<String, Boolean>) zscoreEnabledTimeframesObj;
            savedEnabledTimeframes = timeframesMap;
        }
                
        refreshAllIndicatorWeightsButtons();
        
    }
    
    /**
     * Migrate existing indicator weights to HashMap format
     */
    private void migrateStrategyWeightsToHashMap(StrategyConfig strategyConfig) {
        Map<String, Object> parameters = strategyConfig.getParameters();
        
        for (String key : new ArrayList<>(parameters.keySet())) {
            if (key.startsWith("indicatorWeights_")) {
                Object weightsObj = parameters.get(key);
                
                if (weightsObj instanceof String) {
                    // Convert String to HashMap
                    Map<String, Integer> weightsMap = parseWeightsStringToMap((String) weightsObj);
                    parameters.put(key, weightsMap);
                    System.out.println("Migrated " + key + " from String to HashMap");
                } else if (weightsObj instanceof JSONObject) {
                    // Convert JSONObject to HashMap
                    Map<String, Integer> weightsMap = convertJSONObjectToWeightsMap((JSONObject) weightsObj);
                    parameters.put(key, weightsMap);
                    System.out.println("Migrated " + key + " from JSONObject to HashMap");
                }
                // If it's already a Map, leave it as is
            }
        }
    }

    /**
     * Convert JSONObject to weights HashMap
     */
    private Map<String, Integer> convertJSONObjectToWeightsMap(JSONObject json) {
        Map<String, Integer> weightsMap = new HashMap<>();
        
        if (json == null) {
            return weightsMap;
        }
        
        try {
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    int value = json.getInt(key);
                    weightsMap.put(key, value);
                } catch (Exception e) {
                    System.err.println("Invalid weight value in JSON for key: " + key);
                }
            }
        } catch (Exception e) {
            System.err.println("Error converting JSON to weights map: " + e.getMessage());
        }
        
        return weightsMap;
    }

    /**
     * Migrate Z-Score parameters to HashMap format
     */
    private void migrateZScoreParametersToHashMap(StrategyConfig strategyConfig) {
        Map<String, Object> parameters = strategyConfig.getParameters();
        
        // Migrate zscoreWeights
        Object zscoreWeightsObj = parameters.get("zscoreWeights");
        if (zscoreWeightsObj instanceof String) {
            Map<String, Integer> weightsMap = parseWeightsStringToMap((String) zscoreWeightsObj);
            parameters.put("zscoreWeights", weightsMap);
            System.out.println("Migrated zscoreWeights from String to HashMap");
        }
        
        // Migrate zscoreEnabledTimeframes
        Object zscoreEnabledTimeframesObj = parameters.get("zscoreEnabledTimeframes");
        if (zscoreEnabledTimeframesObj instanceof String) {
            Map<String, Boolean> timeframesMap = parseEnabledTimeframesStringToMap((String) zscoreEnabledTimeframesObj);
            parameters.put("zscoreEnabledTimeframes", timeframesMap);
            System.out.println("Migrated zscoreEnabledTimeframes from String to HashMap");
        }
    }
    
    private void refreshAllIndicatorWeightsButtons() {
        if (timeframeCheckboxes == null || timeframeWeightSpinners == null) {
            return; // Components not initialized
        }
        
        // Update each timeframe's checkbox and button state based on current indicator selections
        for (String timeframe : ALL_TIMEFRAMES) {
            JCheckBox checkbox = timeframeCheckboxes.get(timeframe);
            JSpinner spinner = timeframeWeightSpinners.get(timeframe);
            
            if (checkbox != null && spinner != null) {
                boolean hasIndicators = hasTimeframeIndicators(timeframe);
                
                // Enable/disable checkbox based on whether timeframe has indicators
                checkbox.setEnabled(hasIndicators);
                
                // Update tooltip to explain why checkbox might be disabled
                if (hasIndicators) {
                    checkbox.setToolTipText("Include " + timeframe + " in Z-Score calculation");
                } else {
                    checkbox.setToolTipText("No indicators configured for " + timeframe + " - add indicators in Timeframe Indicators tab");
                }
                
                // If checkbox was selected but now has no indicators, deselect it
                if (checkbox.isSelected() && !hasIndicators) {
                    checkbox.setSelected(false);
                    spinner.setEnabled(false);
                    spinner.setValue(0);
                }
                
                // Update spinner state based on checkbox selection
                spinner.setEnabled(checkbox.isSelected() && hasIndicators);
                
                // Update the corresponding indicator weights button state
                updateIndicatorWeightsButtonForTimeframe(timeframe, checkbox, hasIndicators);
            }
        }
        
        // Also update the weight total display
        updateWeightTotal();
    }
    
    /**
     * Convert zscoreWeightsObj from String to Map if needed
     */
    private Map<String, Integer> convertZScoreWeightsToMap(Object zscoreWeightsObj) {
        if (zscoreWeightsObj instanceof String) {
            String weightsString = (String) zscoreWeightsObj;
            return parseWeightsStringToMap(weightsString);
        } else if (zscoreWeightsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Integer> weightsMap = (Map<String, Integer>) zscoreWeightsObj;
            return weightsMap;
        } else if (zscoreWeightsObj instanceof JSONObject) {
            return convertJSONObjectToWeightsMap((JSONObject) zscoreWeightsObj);
        }
        return new HashMap<>();
    }

    /**
     * Convert zscoreEnabledTimeframesObj from String to Map if needed
     */
    private Map<String, Boolean> convertEnabledTimeframesToMap(Object zscoreEnabledTimeframesObj) {
        if (zscoreEnabledTimeframesObj instanceof String) {
            String timeframesString = (String) zscoreEnabledTimeframesObj;
            return parseEnabledTimeframesStringToMap(timeframesString);
        } else if (zscoreEnabledTimeframesObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Boolean> timeframesMap = (Map<String, Boolean>) zscoreEnabledTimeframesObj;
            return timeframesMap;
        } else if (zscoreEnabledTimeframesObj instanceof JSONObject) {
            return convertJSONObjectToEnabledTimeframesMap((JSONObject) zscoreEnabledTimeframesObj);
        }
        return new HashMap<>();
    }

    /**
     * Parse weights string like "{1W=25, 1D=25, 4H=25, 1H=25}" to Map
     */
    private Map<String, Integer> parseWeightsStringToMap(String weightsString) {
        Map<String, Integer> weightsMap = new HashMap<>();
        
        if (weightsString == null || weightsString.trim().isEmpty()) {
            return weightsMap;
        }
        
        try {
            String content = weightsString.trim();
            if (content.startsWith("{")) {
                content = content.substring(1);
            }
            if (content.endsWith("}")) {
                content = content.substring(0, content.length() - 1);
            }
            content = content.trim();
            
            if (content.isEmpty()) {
                return weightsMap;
            }
            
            List<String> entries = splitMapEntries(content);
            
            for (String entry : entries) {
                String[] keyValue = entry.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String valueStr = keyValue[1].trim();
                    
                    try {
                        int value = Integer.parseInt(valueStr);
                        weightsMap.put(key, value);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid weight value: " + valueStr + " for timeframe: " + key);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing weights string: " + weightsString + " - " + e.getMessage());
        }
        
        return weightsMap;
    }

    /**
     * Parse enabled timeframes string like "{1W=true, 1D=true, 4H=false}" to Map
     */
    private Map<String, Boolean> parseEnabledTimeframesStringToMap(String timeframesString) {
        Map<String, Boolean> timeframesMap = new HashMap<>();
        
        if (timeframesString == null || timeframesString.trim().isEmpty()) {
            return timeframesMap;
        }
        
        try {
            String content = timeframesString.trim();
            if (content.startsWith("{")) {
                content = content.substring(1);
            }
            if (content.endsWith("}")) {
                content = content.substring(0, content.length() - 1);
            }
            content = content.trim();
            
            if (content.isEmpty()) {
                return timeframesMap;
            }
            
            List<String> entries = splitMapEntries(content);
            
            for (String entry : entries) {
                String[] keyValue = entry.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String valueStr = keyValue[1].trim();
                    
                    boolean value = Boolean.parseBoolean(valueStr);
                    timeframesMap.put(key, value);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing enabled timeframes string: " + timeframesString + " - " + e.getMessage());
        }
        
        return timeframesMap;
    }
    
    /**
     * Convert JSONObject to enabled timeframes Map
     */
    private Map<String, Boolean> convertJSONObjectToEnabledTimeframesMap(JSONObject json) {
        Map<String, Boolean> timeframesMap = new HashMap<>();
        
        if (json == null) {
            return timeframesMap;
        }
        
        try {
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try {
                    boolean value = json.getBoolean(key);
                    timeframesMap.put(key, value);
                } catch (Exception e) {
                    System.err.println("Invalid boolean value in JSON for key: " + key);
                }
            }
        } catch (Exception e) {
            System.err.println("Error converting JSON to enabled timeframes map: " + e.getMessage());
        }
        
        return timeframesMap;
    }
    
    /**
     * Update indicator weights button for a specific timeframe
     */
    private void updateIndicatorWeightsButtonForTimeframe(String timeframe, JCheckBox checkbox, boolean hasIndicators) {
        boolean timeframeEnabled = checkbox.isSelected();
        
        // Find and update the button state
        updateIndicatorWeightsButton(timeframe, timeframeEnabled, hasIndicators);
    }

    
    /**
     * Search for the button in the tabbed pane
     */
    private void updateButtonInTabbedPane(JTabbedPane tabbedPane, String timeframe, boolean timeframeEnabled, boolean hasIndicators) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if ("Z-Score".equals(tabbedPane.getTitleAt(i))) {
                Component tabComponent = tabbedPane.getComponentAt(i);
                if (tabComponent instanceof JPanel) {
                    updateButtonInContainer((JPanel) tabComponent, timeframe, timeframeEnabled, hasIndicators);
                }
                break;
            }
        }
    }

    
    /**
     * Update a specific timeframe's indicator weights button
     */
    private void updateIndicatorWeightsButton(String timeframe, boolean timeframeEnabled, boolean hasIndicators) {
        // Since we don't store direct button references, we need to find the button in the UI
        // This is a safer approach that looks for the button in the known container
        
        Component[] components = getContentPane().getComponents();
        for (Component comp : components) {
            if (comp instanceof JTabbedPane) {
                JTabbedPane tabbedPane = (JTabbedPane) comp;
                Component zscoreTab = null;
                
                // Find the Z-Score tab
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    if ("Z-Score".equals(tabbedPane.getTitleAt(i))) {
                        zscoreTab = tabbedPane.getComponentAt(i);
                        break;
                    }
                }
                
                if (zscoreTab instanceof JPanel) {
                    JPanel zscorePanel = (JPanel) zscoreTab;
                    updateButtonInContainer(zscorePanel, timeframe, timeframeEnabled, hasIndicators);
                }
            }
        }
    }

    /**
     * Recursively find and update the button in the container
     */
    private void updateButtonInContainer(Container container, String timeframe, boolean timeframeEnabled, boolean hasIndicators) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                String tooltip = button.getToolTipText();
                if (tooltip != null && tooltip.contains(timeframe)) {
                    // This is the button for our timeframe - update its state
                    button.setEnabled(timeframeEnabled && hasIndicators);
                    
                    // Update tooltip based on state
                    if (!timeframeEnabled) {
                        button.setToolTipText("Enable timeframe '" + timeframe + "' first to configure indicator weights");
                    } else if (!hasIndicators) {
                        button.setToolTipText("No indicators configured for timeframe '" + timeframe + "' - add indicators first");
                    } else {
                        button.setToolTipText("Configure indicator weights for " + timeframe);
                    }
                    return;
                }
            }
            
            if (comp instanceof Container) {
                updateButtonInContainer((Container) comp, timeframe, timeframeEnabled, hasIndicators);
            }
        }
    }

    /**
     * Check if a timeframe has any indicators configured (standard or custom) in the CURRENT UI STATE
     */
    private boolean hasTimeframeIndicators(String timeframe) {
        JList<String> standardList = timeframeIndicatorLists.get(timeframe);
        JList<CustomIndicator> customList = timeframeCustomIndicatorLists.get(timeframe);
        
        boolean hasStandard = standardList != null && standardList.getSelectedIndices().length > 0;
        boolean hasCustom = customList != null && customList.getSelectedIndices().length > 0;
        
        return hasStandard || hasCustom;
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
        
        
     // Store Z-Score enabled state and weights
        boolean zscoreEnabled = enableZScoreCheckbox.isSelected();
        strategyConfig.getParameters().put("zscoreEnabled", zscoreEnabled);
        
        if (zscoreEnabled) {
            // Only store weights if Z-Score is enabled
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
        } else {
            // Clear weights if disabled
            strategyConfig.getParameters().put("zscoreWeights", new HashMap<>());
            strategyConfig.getParameters().put("zscoreEnabledTimeframes", new HashMap<>());
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

        // Validate that checked timeframes actually have indicators
        for (String timeframe : ALL_TIMEFRAMES) {
            JCheckBox checkbox = timeframeCheckboxes.get(timeframe);
            if (checkbox != null && checkbox.isSelected()) {
                boolean hasIndicators = hasTimeframeIndicators(timeframe);
                if (!hasIndicators) {
                    int result = JOptionPane.showConfirmDialog(this,
                        "Timeframe '" + timeframe + "' is selected for Z-Score but has no indicators configured.\n" +
                        "Do you want to continue?",
                        "No Indicators for Selected Timeframe",
                        JOptionPane.YES_NO_OPTION);
                        
                    if (result != JOptionPane.YES_OPTION) {
                        return;
                    }
                    break;
                }
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

        // Store Z-Score weights and enabled timeframes as HashMaps
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

        // Store indicator weights as HashMaps instead of JSON
        for (String timeframe : ALL_TIMEFRAMES) {
            // This will be handled by the IndicatorWeightsDialog.saveWeights() method
        }

        saved = true;
        dispose();
    }
    
      
    /**
     * Migrate Z-Score weights and enabled timeframes from String to Map format
     */
    private void migrateZScoreParameters(StrategyConfig strategyConfig) {
        Map<String, Object> parameters = strategyConfig.getParameters();
        
        // Migrate zscoreWeights
        Object zscoreWeightsObj = parameters.get("zscoreWeights");
        if (zscoreWeightsObj instanceof String) {
            Map<String, Integer> weightsMap = convertZScoreWeightsToMap(zscoreWeightsObj);
            parameters.put("zscoreWeights", weightsMap);
            System.out.println("Migrated zscoreWeights from String to Map");
        }
        
        // Migrate zscoreEnabledTimeframes
        Object zscoreEnabledTimeframesObj = parameters.get("zscoreEnabledTimeframes");
        if (zscoreEnabledTimeframesObj instanceof String) {
            Map<String, Boolean> timeframesMap = convertEnabledTimeframesToMap(zscoreEnabledTimeframesObj);
            parameters.put("zscoreEnabledTimeframes", timeframesMap);
            System.out.println("Migrated zscoreEnabledTimeframes from String to Map");
        }
    }

    /**
     * Parse Map string representation like "{STANDARD_MACD(5,8,9)=50, STANDARD_MACD=50}" to JSONObject
     */
    private JSONObject parseMapStringToJSON(String mapString) {
        JSONObject json = new JSONObject();
        
        if (mapString == null || mapString.trim().isEmpty()) {
            return json;
        }
        
        try {
            // Remove outer braces and trim
            String content = mapString.trim();
            if (content.startsWith("{")) {
                content = content.substring(1);
            }
            if (content.endsWith("}")) {
                content = content.substring(0, content.length() - 1);
            }
            content = content.trim();
            
            if (content.isEmpty()) {
                return json;
            }
            
            // Split by commas, but be careful of commas inside parentheses
            List<String> entries = splitMapEntries(content);
            
            for (String entry : entries) {
                String[] keyValue = entry.split("=", 2); // Split on first = only
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String valueStr = keyValue[1].trim();
                    
                    // Try to parse as integer
                    try {
                        int value = Integer.parseInt(valueStr);
                        json.put(key, value);
                    } catch (NumberFormatException e) {
                        // If not an integer, store as string
                        json.put(key, valueStr);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing map string: " + mapString + " - " + e.getMessage());
            throw new RuntimeException("Failed to parse map string: " + e.getMessage(), e);
        }
        
        return json;
    }


    /**
     * Split map entries, handling commas inside parentheses
     */
    private List<String> splitMapEntries(String content) {
        List<String> entries = new ArrayList<>();
        StringBuilder currentEntry = new StringBuilder();
        int parenDepth = 0;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '(') {
                parenDepth++;
            } else if (c == ')') {
                parenDepth--;
            }
            
            if (c == ',' && parenDepth == 0) {
                // This comma is at top level, split here
                String entry = currentEntry.toString().trim();
                if (!entry.isEmpty()) {
                    entries.add(entry);
                }
                currentEntry = new StringBuilder();
            } else {
                currentEntry.append(c);
            }
        }
        
        // Add the last entry
        String lastEntry = currentEntry.toString().trim();
        if (!lastEntry.isEmpty()) {
            entries.add(lastEntry);
        }
        
        return entries;
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