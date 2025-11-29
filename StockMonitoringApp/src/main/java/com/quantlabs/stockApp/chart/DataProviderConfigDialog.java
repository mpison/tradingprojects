package com.quantlabs.stockApp.chart;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class DataProviderConfigDialog extends JDialog {
    private final Map<String, Object> configuration;
    private JComboBox<String> providerComboBox;
    private JComboBox<String> adjustmentComboBox;
    private JComboBox<String> feedComboBox;
    private JPanel alpacaPanel;
    private boolean confirmed = false;

    public DataProviderConfigDialog(Frame parent, String currentProvider, Map<String, Object> currentConfig) {
        super(parent, "Data Provider Configuration", true);
        this.configuration = new HashMap<>(currentConfig);
        
        initializeComponents(currentProvider);
        setupLayout();
        updateAlpacaPanelVisibility(currentProvider.equals("Alpaca"));
        setLocationRelativeTo(parent);
        pack();
    }

    private void initializeComponents(String currentProvider) {
        // Provider selection
        String[] providers = {"Alpaca", "Yahoo"};
        providerComboBox = new JComboBox<>(providers);
        providerComboBox.setSelectedItem(currentProvider);
        providerComboBox.addActionListener(e -> {
            String selectedProvider = (String) providerComboBox.getSelectedItem();
            updateAlpacaPanelVisibility("Alpaca".equals(selectedProvider));
        });

        // Alpaca-specific parameters
        String[] adjustments = {"raw", "split", "dividend", "all"};
        adjustmentComboBox = new JComboBox<>(adjustments);
        adjustmentComboBox.setSelectedItem(configuration.getOrDefault("adjustment", "all"));

        String[] feeds = {"sip", "iex", "boats", "otc"};
        feedComboBox = new JComboBox<>(feeds);
        feedComboBox.setSelectedItem(configuration.getOrDefault("feed", "sip"));

        // Alpaca parameters panel
        alpacaPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        alpacaPanel.setBorder(BorderFactory.createTitledBorder("Alpaca Parameters"));
        alpacaPanel.add(new JLabel("Adjustment:"));
        alpacaPanel.add(adjustmentComboBox);
        alpacaPanel.add(new JLabel("Feed:"));
        alpacaPanel.add(feedComboBox);
    }

    private void setupLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Provider selection panel
        JPanel providerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        providerPanel.add(new JLabel("Data Provider:"));
        providerPanel.add(providerComboBox);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            confirmed = true;
            updateConfiguration();
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(providerPanel, BorderLayout.NORTH);
        mainPanel.add(alpacaPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void updateAlpacaPanelVisibility(boolean visible) {
        alpacaPanel.setVisible(visible);
        pack();
        setLocationRelativeTo(getParent());
    }

    private void updateConfiguration() {
        configuration.clear();
        configuration.put("provider", providerComboBox.getSelectedItem());
        
        if ("Alpaca".equals(providerComboBox.getSelectedItem())) {
            configuration.put("adjustment", adjustmentComboBox.getSelectedItem());
            configuration.put("feed", feedComboBox.getSelectedItem());
        }
    }

    public Map<String, Object> getConfiguration() {
        return confirmed ? new HashMap<>(configuration) : null;
    }

    public String getSelectedProvider() {
        return confirmed ? (String) configuration.get("provider") : null;
    }
}