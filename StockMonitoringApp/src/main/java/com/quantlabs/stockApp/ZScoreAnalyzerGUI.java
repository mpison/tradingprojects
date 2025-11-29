package com.quantlabs.stockApp;

import com.quantlabs.stockApp.model.PriceData;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class ZScoreAnalyzerGUI extends JFrame {
    
    private ZScoreAnalyzer analyzer;
    private JComboBox<String> combinationComboBox;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JTextArea individualScoresArea;
    private JTextField tickerField, priceField, volumeField, percentChangeField;
    private JTextField premarketPriceField, premarketVolumeField, premarketChangeField;
    private JTextField postmarketPriceField, postmarketVolumeField, postmarketChangeField;
    private JTextField prevPriceField, avgVolumeField;
    
    public ZScoreAnalyzerGUI() {
        analyzer = new ZScoreAnalyzer();
        initializeUI();
    }
    
    private void initializeUI() {
        setTitle("Z-Score Analyzer for Stock Data");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Input panel
        mainPanel.add(createInputPanel(), BorderLayout.NORTH);
        
        // Center panel with tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Combination Results", createResultsPanel());
        tabbedPane.addTab("Individual Scores", createIndividualScoresPanel());
        tabbedPane.addTab("Manage Combinations", createManagementPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        add(mainPanel);
    }
    
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new GridLayout(3, 6, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Price Data Input"));
        
        // Row 1
        inputPanel.add(new JLabel("Ticker:"));
        tickerField = new JTextField("AAPL");
        inputPanel.add(tickerField);
        
        inputPanel.add(new JLabel("Price:"));
        priceField = new JTextField("150.0");
        inputPanel.add(priceField);
        
        inputPanel.add(new JLabel("Volume:"));
        volumeField = new JTextField("2000000");
        inputPanel.add(volumeField);
        
        // Row 2
        inputPanel.add(new JLabel("% Change:"));
        percentChangeField = new JTextField("0.03");
        inputPanel.add(percentChangeField);
        
        inputPanel.add(new JLabel("Premarket Price:"));
        premarketPriceField = new JTextField("148.5");
        inputPanel.add(premarketPriceField);
        
        inputPanel.add(new JLabel("Premarket Volume:"));
        premarketVolumeField = new JTextField("75000");
        inputPanel.add(premarketVolumeField);
        
        // Row 3
        inputPanel.add(new JLabel("Premarket Change:"));
        premarketChangeField = new JTextField("0.01");
        inputPanel.add(premarketChangeField);
        
        inputPanel.add(new JLabel("Postmarket Price:"));
        postmarketPriceField = new JTextField("151.0");
        inputPanel.add(postmarketPriceField);
        
        inputPanel.add(new JLabel("Postmarket Volume:"));
        postmarketVolumeField = new JTextField("45000");
        inputPanel.add(postmarketVolumeField);
        
        // Additional fields
        inputPanel.add(new JLabel("Postmarket Change:"));
        postmarketChangeField = new JTextField("0.02");
        inputPanel.add(postmarketChangeField);
        
        inputPanel.add(new JLabel("Previous Price:"));
        prevPriceField = new JTextField("145.0");
        inputPanel.add(prevPriceField);
        
        inputPanel.add(new JLabel("Avg Volume:"));
        avgVolumeField = new JTextField("1500000");
        inputPanel.add(avgVolumeField);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton calculateButton = new JButton("Calculate All");
        calculateButton.addActionListener(e -> calculateAll());
        
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearFields());
        
        buttonPanel.add(clearButton);
        buttonPanel.add(calculateButton);
        
        JPanel mainInputPanel = new JPanel(new BorderLayout());
        mainInputPanel.add(inputPanel, BorderLayout.CENTER);
        mainInputPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        return mainInputPanel;
    }
    
    private JPanel createResultsPanel() {
        JPanel resultsPanel = new JPanel(new BorderLayout(5, 5));
        
        // Combination selection
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectionPanel.add(new JLabel("Select Combination:"));
        
        combinationComboBox = new JComboBox<>();
        updateCombinationComboBox();
        selectionPanel.add(combinationComboBox);
        
        JButton calculateButton = new JButton("Calculate Selected");
        calculateButton.addActionListener(e -> calculateSelectedCombination());
        selectionPanel.add(calculateButton);
        
        resultsPanel.add(selectionPanel, BorderLayout.NORTH);
        
        // Results table
        String[] columnNames = {"Metric", "Z-Score", "Interpretation"};
        tableModel = new DefaultTableModel(columnNames, 0);
        resultsTable = new JTable(tableModel);
        resultsTable.setRowHeight(25);
        
        JScrollPane scrollPane = new JScrollPane(resultsTable);
        resultsPanel.add(scrollPane, BorderLayout.CENTER);
        
        return resultsPanel;
    }
    
    private JPanel createIndividualScoresPanel() {
        JPanel individualPanel = new JPanel(new BorderLayout());
        
        individualScoresArea = new JTextArea(20, 50);
        individualScoresArea.setEditable(false);
        individualScoresArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(individualScoresArea);
        individualPanel.add(scrollPane, BorderLayout.CENTER);
        
        return individualPanel;
    }
    
    private JPanel createManagementPanel() {
        JPanel managementPanel = new JPanel(new BorderLayout(5, 5));
        
        // Current combinations list
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> combinationsList = new JList<>(listModel);
        updateCombinationsList(listModel);
        
        JScrollPane listScrollPane = new JScrollPane(combinationsList);
        listScrollPane.setBorder(BorderFactory.createTitledBorder("Current Combinations"));
        
        // Control panel
        JPanel controlPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        
        JButton refreshButton = new JButton("Refresh List");
        refreshButton.addActionListener(e -> updateCombinationsList(listModel));
        
        JButton viewDetailsButton = new JButton("View Details");
        viewDetailsButton.addActionListener(e -> showCombinationDetails(combinationsList.getSelectedValue()));
        
        JButton removeButton = new JButton("Remove Selected");
        removeButton.addActionListener(e -> removeCombination(combinationsList.getSelectedValue(), listModel));
        
        controlPanel.add(refreshButton);
        controlPanel.add(viewDetailsButton);
        controlPanel.add(removeButton);
        
        // Add new combination panel
        JPanel addPanel = new JPanel(new BorderLayout());
        addPanel.setBorder(BorderFactory.createTitledBorder("Add New Combination"));
        
        JTextField nameField = new JTextField();
        JTextArea metricsArea = new JTextArea(3, 20);
        metricsArea.setToolTipText("Enter metrics separated by commas (price, volume, percentChange, etc.)");
        
        JButton addButton = new JButton("Add Combination");
        addButton.addActionListener(e -> addNewCombination(
            nameField.getText(), 
            metricsArea.getText(),
            listModel
        ));
        
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.add(new JLabel("Combination Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Metrics (comma-separated):"));
        inputPanel.add(new JScrollPane(metricsArea));
        
        addPanel.add(inputPanel, BorderLayout.CENTER);
        addPanel.add(addButton, BorderLayout.SOUTH);
        
        // Layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, addPanel);
        splitPane.setDividerLocation(300);
        
        managementPanel.add(splitPane, BorderLayout.CENTER);
        managementPanel.add(controlPanel, BorderLayout.SOUTH);
        
        return managementPanel;
    }
    
    private PriceData createPriceDataFromInput() {
        try {
            return new PriceData.Builder(tickerField.getText(), Double.parseDouble(priceField.getText()))
                .currentVolume(Long.parseLong(volumeField.getText()))
                .averageVol(Double.parseDouble(avgVolumeField.getText()))
                .percentChange(Double.parseDouble(percentChangeField.getText()))
                .premarketClose(Double.parseDouble(premarketPriceField.getText()))
                .premarketVolume(Long.parseLong(premarketVolumeField.getText()))
                .premarketChange(Double.parseDouble(premarketChangeField.getText()))
                .postmarketClose(Double.parseDouble(postmarketPriceField.getText()))
                .postmarketVolume(Long.parseLong(postmarketVolumeField.getText()))
                .postmarketChange(Double.parseDouble(postmarketChangeField.getText()))
                .prevLastDayPrice(Double.parseDouble(prevPriceField.getText()))
                .build();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number format in input fields", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
    
    private void calculateAll() {
        PriceData priceData = createPriceDataFromInput();
        if (priceData == null) return;
        
        // Calculate individual scores
        Map<String, Double> individualScores = analyzer.calculateIndividualZScores(priceData);
        displayIndividualScores(individualScores);
        
        // Calculate all combinations
        Map<String, Map<String, Double>> allCombinations = analyzer.calculateAllCombinations(priceData);
        displayCombinationResults(allCombinations);
        
        updateCombinationComboBox();
    }
    
    private void calculateSelectedCombination() {
        PriceData priceData = createPriceDataFromInput();
        if (priceData == null) return;
        
        String selected = (String) combinationComboBox.getSelectedItem();
        if (selected != null) {
            Map<String, Double> result = analyzer.calculateCombinedZScores(priceData, selected);
            displaySingleCombination(selected, result);
        }
    }
    
    private void displayIndividualScores(Map<String, Double> scores) {
        StringBuilder sb = new StringBuilder();
        sb.append("INDIVIDUAL Z-SCORES\n");
        sb.append("===================\n\n");
        
        scores.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                String interpretation = getInterpretation(entry.getValue());
                sb.append(String.format("%-20s: %6.3f  (%s)%n", 
                    entry.getKey(), entry.getValue(), interpretation));
            });
        
        individualScoresArea.setText(sb.toString());
    }
    
    private void displayCombinationResults(Map<String, Map<String, Double>> allCombinations) {
        tableModel.setRowCount(0);
        
        allCombinations.forEach((combination, scores) -> {
            Double combinedScore = scores.get("combinedZScore");
            if (combinedScore != null) {
                String interpretation = getInterpretation(combinedScore);
                tableModel.addRow(new Object[]{
                    combination, 
                    String.format("%.3f", combinedScore),
                    interpretation
                });
            }
        });
    }
    
    private void displaySingleCombination(String combination, Map<String, Double> scores) {
        tableModel.setRowCount(0);
        
        scores.forEach((metric, score) -> {
            if (metric.equals("combinedZScore") || metric.equals("componentCount")) {
                String interpretation = metric.equals("combinedZScore") ? getInterpretation(score) : "";
                tableModel.addRow(new Object[]{
                    metric, 
                    String.format("%.3f", score),
                    interpretation
                });
            } else if (metric.endsWith("_Z")) {
                tableModel.addRow(new Object[]{
                    metric, 
                    String.format("%.3f", score),
                    getInterpretation(score)
                });
            }
        });
    }
    
    private String getInterpretation(double zScore) {
        if (zScore > 2.0) return "EXTREMELY HIGH";
        if (zScore > 1.5) return "VERY HIGH";
        if (zScore > 1.0) return "HIGH";
        if (zScore > 0.5) return "MODERATELY HIGH";
        if (zScore > -0.5) return "NORMAL";
        if (zScore > -1.0) return "MODERATELY LOW";
        if (zScore > -1.5) return "LOW";
        if (zScore > -2.0) return "VERY LOW";
        return "EXTREMELY LOW";
    }
    
    private void updateCombinationComboBox() {
        combinationComboBox.removeAllItems();
        for (String combination : analyzer.getAvailableCombinations()) {
            combinationComboBox.addItem(combination);
        }
    }
    
    private void updateCombinationsList(DefaultListModel<String> listModel) {
        listModel.clear();
        for (String combination : analyzer.getAvailableCombinations()) {
            listModel.addElement(combination);
        }
    }
    
    private void showCombinationDetails(String combination) {
        if (combination == null) return;
        
        List<String> metrics = analyzer.getCombinationMetrics(combination);
        if (metrics != null) {
            JOptionPane.showMessageDialog(this,
                "Combination: " + combination + "\nMetrics: " + String.join(", ", metrics),
                "Combination Details",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void removeCombination(String combination, DefaultListModel<String> listModel) {
        if (combination == null) return;
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Remove combination '" + combination + "'?",
            "Confirm Removal",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            analyzer.removeCombination(combination);
            updateCombinationsList(listModel);
            updateCombinationComboBox();
        }
    }
    
    private void addNewCombination(String name, String metricsText, DefaultListModel<String> listModel) {
        if (name.isEmpty() || metricsText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and metrics cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String[] metricsArray = metricsText.split(",");
        List<String> metrics = new ArrayList<>();
        for (String metric : metricsArray) {
            metrics.add(metric.trim());
        }
        
        analyzer.addCustomCombination(name, metrics);
        updateCombinationsList(listModel);
        updateCombinationComboBox();
        
        JOptionPane.showMessageDialog(this, "Combination added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void clearFields() {
        tickerField.setText("");
        priceField.setText("");
        volumeField.setText("");
        percentChangeField.setText("");
        premarketPriceField.setText("");
        premarketVolumeField.setText("");
        premarketChangeField.setText("");
        postmarketPriceField.setText("");
        postmarketVolumeField.setText("");
        postmarketChangeField.setText("");
        prevPriceField.setText("");
        avgVolumeField.setText("");
        
        tableModel.setRowCount(0);
        individualScoresArea.setText("");
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            ZScoreAnalyzerGUI gui = new ZScoreAnalyzerGUI();
            gui.setVisible(true);
        });
    }
}