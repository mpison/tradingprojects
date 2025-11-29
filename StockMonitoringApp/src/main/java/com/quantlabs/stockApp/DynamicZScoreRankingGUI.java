package com.quantlabs.stockApp;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class DynamicZScoreRankingGUI extends JFrame {
    
    private DynamicZScoreAnalyzer analyzer;
    private DefaultListModel<PriceData> portfolioListModel;
    private JComboBox<String> combinationComboBox;
    private JTable rankingTable;
    private DefaultTableModel rankingTableModel;
    
    // Cache of available methods from PriceData class
    private List<String> availableMetrics;
    
    public DynamicZScoreRankingGUI() {
        analyzer = new DynamicZScoreAnalyzer();
        portfolioListModel = new DefaultListModel<>();
        availableMetrics = discoverAvailableMetrics();
        initializeUI();
        loadSampleData();
    }
    
    // Use reflection to discover all available getter methods from PriceData and AnalysisResult
    private List<String> discoverAvailableMetrics() {
        List<String> metrics = new ArrayList<>();
        
        // Discover PriceData metrics
        Method[] methods = PriceData.class.getMethods();
        for (Method method : methods) {
            String methodName = method.getName();
            if (methodName.startsWith("get") && methodName.length() > 3 && !methodName.equals("getClass")) {
                String metricName = methodName.substring(3);
                metricName = Character.toLowerCase(metricName.charAt(0)) + metricName.substring(1);
                
                Class<?> returnType = method.getReturnType();
                if (returnType == Double.class || returnType == double.class || 
                    returnType == Integer.class || returnType == int.class ||
                    returnType == Long.class || returnType == long.class) {
                    metrics.add(metricName);
                }
            }
        }
        
        // Add AnalysisResult metrics
        metrics.addAll(analyzer.discoverAnalysisResultMetrics());
        
        return metrics;
    }
    
    private void initializeUI() {
        setTitle("Dynamic Z-Score Ranking Analyzer with Technical Indicators");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 1000);
        setLocationRelativeTo(null);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Portfolio", createPortfolioPanel());
        tabbedPane.addTab("Ranking", createRankingPanel());
        tabbedPane.addTab("Combination Manager", createCombinationManagerPanel());
        tabbedPane.addTab("Metric Explorer", createMetricExplorerPanel());
        tabbedPane.addTab("Technical Analysis", createTechnicalAnalysisPanel());
        
        add(tabbedPane);
    }
    
    private JPanel createPortfolioPanel() {
        JPanel portfolioPanel = new JPanel(new BorderLayout(5, 5));
        
        // Portfolio list
        JList<PriceData> portfolioList = new JList<>(portfolioListModel);
        portfolioList.setCellRenderer(new PriceDataListRenderer());
        JScrollPane listScrollPane = new JScrollPane(portfolioList);
        listScrollPane.setBorder(BorderFactory.createTitledBorder("Portfolio Symbols"));
        
        // Control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add Symbol");
        JButton removeButton = new JButton("Remove Selected");
        JButton clearButton = new JButton("Clear All");
        JButton loadSampleButton = new JButton("Load Sample Data");
        
        addButton.addActionListener(e -> showAddSymbolDialog());
        removeButton.addActionListener(e -> removeSelectedSymbol(portfolioList));
        clearButton.addActionListener(e -> clearPortfolio());
        loadSampleButton.addActionListener(e -> loadSampleData());
        
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(loadSampleButton);
        
        portfolioPanel.add(listScrollPane, BorderLayout.CENTER);
        portfolioPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        return portfolioPanel;
    }
    
    private JPanel createRankingPanel() {
        JPanel rankingPanel = new JPanel(new BorderLayout(5, 5));
        
        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(new JLabel("Select Combination:"));
        
        combinationComboBox = new JComboBox<>();
        updateCombinationComboBox();
        controlPanel.add(combinationComboBox);
        
        JButton rankButton = new JButton("Rank Symbols");
        JButton exportButton = new JButton("Export Results");
        
        rankButton.addActionListener(e -> rankSymbols());
        exportButton.addActionListener(e -> exportResults());
        
        controlPanel.add(rankButton);
        controlPanel.add(exportButton);
        
        // Results table
        String[] columnNames = {"Rank", "Symbol", "Price", "Z-Score", "Interpretation", "Percentile"};
        rankingTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        rankingTable = new JTable(rankingTableModel);
        rankingTable.setRowSorter(new TableRowSorter<>(rankingTableModel));
        rankingTable.setRowHeight(25);
        
        // Set custom renderers
        rankingTable.getColumnModel().getColumn(3).setCellRenderer(new ZScoreCellRenderer());
        rankingTable.getColumnModel().getColumn(5).setCellRenderer(new PercentileCellRenderer());
        
        JScrollPane tableScrollPane = new JScrollPane(rankingTable);
        
        rankingPanel.add(controlPanel, BorderLayout.NORTH);
        rankingPanel.add(tableScrollPane, BorderLayout.CENTER);
        
        return rankingPanel;
    }
    
    private JPanel createCombinationManagerPanel() {
        JPanel managerPanel = new JPanel(new BorderLayout(5, 5));
        
        // Current combinations list
        DefaultListModel<String> combinationsListModel = new DefaultListModel<>();
        JList<String> combinationsList = new JList<>(combinationsListModel);
        updateCombinationsList(combinationsListModel);
        
        JScrollPane listScrollPane = new JScrollPane(combinationsList);
        listScrollPane.setBorder(BorderFactory.createTitledBorder("Available Combinations"));
        
        // Combination details
        JTextArea detailsArea = new JTextArea(10, 40);
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane detailsScrollPane = new JScrollPane(detailsArea);
        detailsScrollPane.setBorder(BorderFactory.createTitledBorder("Combination Details"));
        
        combinationsList.addListSelectionListener(e -> {
            String selectedCombination = combinationsList.getSelectedValue();
            if (selectedCombination != null) {
                detailsArea.setText(getCombinationDetails(selectedCombination));
            }
        });
        
        // Control buttons
        JPanel controlPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        JButton viewDetailsButton = new JButton("View Details");
        JButton editButton = new JButton("Edit");
        JButton removeButton = new JButton("Remove");
        JButton addButton = new JButton("Add New");
        
        viewDetailsButton.addActionListener(e -> showCombinationDetails(combinationsList.getSelectedValue()));
        editButton.addActionListener(e -> editCombination(combinationsList.getSelectedValue(), combinationsListModel));
        removeButton.addActionListener(e -> removeCombination(combinationsList.getSelectedValue(), combinationsListModel));
        addButton.addActionListener(e -> showAddCombinationDialog(combinationsListModel));
        
        controlPanel.add(viewDetailsButton);
        controlPanel.add(editButton);
        controlPanel.add(removeButton);
        controlPanel.add(addButton);
        
        // Layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listScrollPane, detailsScrollPane);
        splitPane.setDividerLocation(200);
        
        managerPanel.add(splitPane, BorderLayout.CENTER);
        managerPanel.add(controlPanel, BorderLayout.SOUTH);
        
        return managerPanel;
    }
    
    private JPanel createMetricExplorerPanel() {
        JPanel explorerPanel = new JPanel(new BorderLayout());
        
        // Available metrics list
        DefaultListModel<String> metricsListModel = new DefaultListModel<>();
        availableMetrics.forEach(metricsListModel::addElement);
        
        JList<String> metricsList = new JList<>(metricsListModel);
        JScrollPane listScrollPane = new JScrollPane(metricsList);
        listScrollPane.setBorder(BorderFactory.createTitledBorder("Available Metrics"));
        
        // Metric details
        JTextArea detailsArea = new JTextArea(10, 50);
        detailsArea.setEditable(false);
        JScrollPane detailsScrollPane = new JScrollPane(detailsArea);
        detailsScrollPane.setBorder(BorderFactory.createTitledBorder("Metric Details"));
        
        metricsList.addListSelectionListener(e -> {
            String selectedMetric = metricsList.getSelectedValue();
            if (selectedMetric != null) {
                detailsArea.setText(getMetricDetails(selectedMetric));
            }
        });
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, detailsScrollPane);
        splitPane.setDividerLocation(200);
        
        explorerPanel.add(splitPane, BorderLayout.CENTER);
        return explorerPanel;
    }
    
    private JPanel createTechnicalAnalysisPanel() {
        JPanel techPanel = new JPanel(new BorderLayout(5, 5));
        
        // Technical indicators list
        DefaultListModel<String> techIndicatorsModel = new DefaultListModel<>();
        analyzer.getTechnicalIndicators().forEach(techIndicatorsModel::addElement);
        
        JList<String> techList = new JList<>(techIndicatorsModel);
        JScrollPane techScroll = new JScrollPane(techList);
        techScroll.setBorder(BorderFactory.createTitledBorder("Technical Indicators"));
        
        // Pre-defined technical combinations
        DefaultListModel<String> techCombinationsModel = new DefaultListModel<>();
        Arrays.asList("TECH_MOMENTUM", "TECH_TREND", "TECH_BREAKOUT", "TECH_COMPREHENSIVE")
            .forEach(techCombinationsModel::addElement);
        
        JList<String> techComboList = new JList<>(techCombinationsModel);
        JScrollPane techComboScroll = new JScrollPane(techComboList);
        techComboScroll.setBorder(BorderFactory.createTitledBorder("Technical Combinations"));
        
        // Details area
        JTextArea techDetailsArea = new JTextArea(10, 50);
        techDetailsArea.setEditable(false);
        JScrollPane techDetailsScroll = new JScrollPane(techDetailsArea);
        techDetailsScroll.setBorder(BorderFactory.createTitledBorder("Indicator Details"));
        
        techList.addListSelectionListener(e -> {
            String selectedIndicator = techList.getSelectedValue();
            if (selectedIndicator != null) {
                techDetailsArea.setText(getMetricDetails(selectedIndicator));
            }
        });
        
        techComboList.addListSelectionListener(e -> {
            String selectedCombo = techComboList.getSelectedValue();
            if (selectedCombo != null) {
                techDetailsArea.setText(getCombinationDetails(selectedCombo));
            }
        });
        
        // Layout
        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, techScroll, techComboScroll);
        leftSplit.setDividerLocation(200);
        
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, techDetailsScroll);
        mainSplit.setDividerLocation(300);
        
        techPanel.add(mainSplit, BorderLayout.CENTER);
        return techPanel;
    }
    
    private String getMetricDetails(String metricName) {
        try {
            StringBuilder details = new StringBuilder();
            details.append("Metric: ").append(metricName).append("\n");
            
            if (metricName.startsWith("ar_")) {
                details.append("Source: AnalysisResult\n");
                try {
                    String analysisResultMetric = metricName.substring(3);
                    analysisResultMetric = Character.toUpperCase(analysisResultMetric.charAt(0)) + analysisResultMetric.substring(1);
                    Method method = AnalysisResult.class.getMethod("get" + analysisResultMetric);
                    Class<?> returnType = method.getReturnType();
                    details.append("Return Type: ").append(returnType.getSimpleName()).append("\n");
                } catch (Exception e) {
                    details.append("Return Type: Unknown\n");
                }
            } else {
                details.append("Source: PriceData\n");
                try {
                    Method method = PriceData.class.getMethod("get" + Character.toUpperCase(metricName.charAt(0)) + metricName.substring(1));
                    Class<?> returnType = method.getReturnType();
                    details.append("Return Type: ").append(returnType.getSimpleName()).append("\n");
                } catch (Exception e) {
                    details.append("Return Type: Unknown\n");
                }
            }
            
            details.append("Current Mean: ").append(String.format("%.4f", analyzer.getHistoricalMean(metricName))).append("\n");
            details.append("Current Std Dev: ").append(String.format("%.4f", analyzer.getHistoricalStdDev(metricName))).append("\n");
            
            return details.toString();
        } catch (Exception e) {
            return "Error getting details for metric: " + metricName + "\n" + e.getMessage();
        }
    }
    
    private String getCombinationDetails(String combinationName) {
        Map<String, Double> weights = analyzer.getCombinationWeights(combinationName);
        if (weights == null) return "Combination not found: " + combinationName;
        
        StringBuilder details = new StringBuilder();
        details.append("Combination: ").append(combinationName).append("\n\n");
        details.append("Metrics and Weights:\n");
        
        double totalWeight = 0;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            details.append(String.format("  • %-20s: %6.1f%%\n", entry.getKey(), entry.getValue()));
            totalWeight += entry.getValue();
        }
        
        details.append("\nTotal Weight: ").append(String.format("%.1f%%", totalWeight));
        return details.toString();
    }
    
    private void showAddSymbolDialog() {
        JDialog dialog = new JDialog(this, "Add Symbol", true);
        dialog.setLayout(new GridLayout(0, 2, 5, 5));
        dialog.setSize(400, 300);
        
        JTextField tickerField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField volumeField = new JTextField();
        JTextField percentChangeField = new JTextField();
        JTextField avgVolumeField = new JTextField();
        JTextField prevPriceField = new JTextField();
        
        dialog.add(new JLabel("Ticker:"));
        dialog.add(tickerField);
        dialog.add(new JLabel("Price:"));
        dialog.add(priceField);
        dialog.add(new JLabel("Volume:"));
        dialog.add(volumeField);
        dialog.add(new JLabel("% Change:"));
        dialog.add(percentChangeField);
        dialog.add(new JLabel("Average Volume:"));
        dialog.add(avgVolumeField);
        dialog.add(new JLabel("Previous Price:"));
        dialog.add(prevPriceField);
        
        JButton addButton = new JButton("Add");
        JButton cancelButton = new JButton("Cancel");
        
        addButton.addActionListener(e -> {
            try {
                PriceData data = new PriceData.Builder(tickerField.getText(), Double.parseDouble(priceField.getText()))
                    .currentVolume(Long.parseLong(volumeField.getText()))
                    .percentChange(Double.parseDouble(percentChangeField.getText()))
                    .averageVol(Double.parseDouble(avgVolumeField.getText()))
                    .prevLastDayPrice(Double.parseDouble(prevPriceField.getText()))
                    .build();
                
                portfolioListModel.addElement(data);
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(new JLabel());
        dialog.add(buttonPanel);
        dialog.setVisible(true);
    }
    
    private void removeSelectedSymbol(JList<PriceData> list) {
        int selectedIndex = list.getSelectedIndex();
        if (selectedIndex != -1) {
            portfolioListModel.remove(selectedIndex);
        }
    }
    
    private void clearPortfolio() {
        portfolioListModel.clear();
    }
    
    private void loadSampleData() {
        clearPortfolio();
        
        // Sample data for demonstration
        PriceData[] sampleData = {
            new PriceData.Builder("AAPL", 150.0).currentVolume(2000000L).percentChange(0.03).averageVol(1500000.0).prevLastDayPrice(145.0).build(),
            new PriceData.Builder("MSFT", 300.0).currentVolume(1500000L).percentChange(0.015).averageVol(1200000.0).prevLastDayPrice(295.0).build(),
            new PriceData.Builder("GOOGL", 2700.0).currentVolume(800000L).percentChange(0.025).averageVol(900000.0).prevLastDayPrice(2630.0).build(),
            new PriceData.Builder("AMZN", 3200.0).currentVolume(1200000L).percentChange(-0.01).averageVol(1500000.0).prevLastDayPrice(3230.0).build(),
            new PriceData.Builder("TSLA", 800.0).currentVolume(2500000L).percentChange(0.05).averageVol(1800000.0).prevLastDayPrice(760.0).build()
        };
        
        for (PriceData data : sampleData) {
            portfolioListModel.addElement(data);
        }
    }
    
    private void rankSymbols() {
        if (portfolioListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Portfolio is empty. Please add symbols first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String selectedCombination = (String) combinationComboBox.getSelectedItem();
        if (selectedCombination == null) {
            JOptionPane.showMessageDialog(this, "Please select a combination first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Convert list model to list
        List<PriceData> symbols = new ArrayList<>();
        for (int i = 0; i < portfolioListModel.getSize(); i++) {
            symbols.add(portfolioListModel.getElementAt(i));
        }
        
        // Rank symbols
        List<Map.Entry<String, Double>> rankedSymbols = analyzer.rankSymbols(symbols, selectedCombination);
        
        // Display results
        rankingTableModel.setRowCount(0);
        int rank = 1;
        
        for (Map.Entry<String, Double> entry : rankedSymbols) {
            PriceData symbolData = symbols.stream()
                .filter(data -> data.getTicker().equals(entry.getKey()))
                .findFirst()
                .orElse(null);
            
            if (symbolData != null) {
                double percentile = (double) (rankedSymbols.size() - rank + 1) / rankedSymbols.size() * 100;
                String interpretation = getZScoreInterpretation(entry.getValue());
                
                rankingTableModel.addRow(new Object[]{
                    rank++,
                    entry.getKey(),
                    String.format("$%.2f", symbolData.getLatestPrice()),
                    String.format("%.3f", entry.getValue()),
                    interpretation,
                    String.format("%.1f%%", percentile)
                });
            }
        }
    }
    
    private void exportResults() {
        JOptionPane.showMessageDialog(this, "Export functionality would be implemented here", "Info", JOptionPane.INFORMATION_MESSAGE);
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
        
        String details = getCombinationDetails(combination);
        JOptionPane.showMessageDialog(this, details, "Combination Details: " + combination, JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void editCombination(String combination, DefaultListModel<String> listModel) {
        if (combination == null) return;
        
        Map<String, Double> currentWeights = analyzer.getCombinationWeights(combination);
        showEditCombinationDialog(combination, currentWeights, listModel);
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
    
    private void showAddCombinationDialog(DefaultListModel<String> listModel) {
        JDialog dialog = new JDialog(this, "Create New Combination", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(700, 500);
        
        // Available metrics panel
        DefaultListModel<String> availableMetricsModel = new DefaultListModel<>();
        availableMetrics.forEach(availableMetricsModel::addElement);
        
        JList<String> availableList = new JList<>(availableMetricsModel);
        JScrollPane availableScroll = new JScrollPane(availableList);
        availableScroll.setBorder(BorderFactory.createTitledBorder("Available Metrics"));
        
        // Selected metrics with weights panel
        DefaultTableModel selectedModel = new DefaultTableModel(new String[]{"Metric", "Weight %"}, 0);
        JTable selectedTable = new JTable(selectedModel);
        JScrollPane selectedScroll = new JScrollPane(selectedTable);
        selectedScroll.setBorder(BorderFactory.createTitledBorder("Selected Metrics with Weights"));
        
        // Control buttons
        JPanel controlPanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Add →");
        JButton removeButton = new JButton("← Remove");
        JButton clearButton = new JButton("Clear All");
        
        addButton.addActionListener(e -> {
            String selected = availableList.getSelectedValue();
            if (selected != null) {
                selectedModel.addRow(new Object[]{selected, 100.0});
                updateWeights(selectedModel);
            }
        });
        
        removeButton.addActionListener(e -> {
            int selectedRow = selectedTable.getSelectedRow();
            if (selectedRow != -1) {
                selectedModel.removeRow(selectedRow);
                updateWeights(selectedModel);
            }
        });
        
        clearButton.addActionListener(e -> {
            selectedModel.setRowCount(0);
        });
        
        controlPanel.add(addButton);
        controlPanel.add(removeButton);
        controlPanel.add(clearButton);
        
        // Combination name and final controls
        JPanel namePanel = new JPanel(new FlowLayout());
        JTextField nameField = new JTextField(20);
        namePanel.add(new JLabel("Combination Name:"));
        namePanel.add(nameField);
        
        JButton createButton = new JButton("Create Combination");
        JButton cancelButton = new JButton("Cancel");
        
        createButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty() || selectedModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(dialog, "Please provide a name and select at least one metric", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Map<String, Double> weights = new HashMap<>();
            double totalWeight = 0;
            
            for (int i = 0; i < selectedModel.getRowCount(); i++) {
                String metric = (String) selectedModel.getValueAt(i, 0);
                Double weight = Double.parseDouble(selectedModel.getValueAt(i, 1).toString());
                weights.put(metric, weight);
                totalWeight += weight;
            }
            
            // Normalize weights to sum to 100%
            if (Math.abs(totalWeight - 100.0) > 0.01) {
                final double finalTotalWeight = totalWeight;
                weights.replaceAll((k, v) -> v * 100.0 / finalTotalWeight);
            }
            
            analyzer.addCustomCombination(name, weights);
            updateCombinationComboBox();
            updateCombinationsList(listModel);
            dialog.dispose();
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);
        
        // Layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, availableScroll, selectedScroll);
        splitPane.setDividerLocation(250);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        
        dialog.add(namePanel, BorderLayout.NORTH);
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    private void showEditCombinationDialog(String combination, Map<String, Double> currentWeights, DefaultListModel<String> listModel) {
        JDialog dialog = new JDialog(this, "Edit Combination: " + combination, true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(700, 500);
        
        // Selected metrics with weights panel
        DefaultTableModel selectedModel = new DefaultTableModel(new String[]{"Metric", "Weight %"}, 0);
        
        for (Map.Entry<String, Double> entry : currentWeights.entrySet()) {
            selectedModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
        
        JTable selectedTable = new JTable(selectedModel);
        JScrollPane selectedScroll = new JScrollPane(selectedTable);
        selectedScroll.setBorder(BorderFactory.createTitledBorder("Metrics with Weights"));
        
        // Control buttons
        JPanel controlPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Save Changes");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            Map<String, Double> newWeights = new HashMap<>();
            double totalWeight = 0;
            
            for (int i = 0; i < selectedModel.getRowCount(); i++) {
                String metric = (String) selectedModel.getValueAt(i, 0);
                Double weight = Double.parseDouble(selectedModel.getValueAt(i, 1).toString());
                newWeights.put(metric, weight);
                totalWeight += weight;
            }
            
            // Normalize weights
            if (Math.abs(totalWeight - 100.0) > 0.01) {
                final double finalTotalWeight = totalWeight;
                newWeights.replaceAll((k, v) -> v * 100.0 / finalTotalWeight);
            }
            
            analyzer.updateCombinationWeights(combination, newWeights);
            updateCombinationComboBox();
            updateCombinationsList(listModel);
            dialog.dispose();
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        controlPanel.add(saveButton);
        controlPanel.add(cancelButton);
        
        dialog.add(selectedScroll, BorderLayout.CENTER);
        dialog.add(controlPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    private void updateWeights(DefaultTableModel model) {
        int rowCount = model.getRowCount();
        if (rowCount == 0) return;
        
        double equalWeight = 100.0 / rowCount;
        for (int i = 0; i < rowCount; i++) {
            model.setValueAt(equalWeight, i, 1);
        }
    }
    
    private String getZScoreInterpretation(double zScore) {
        if (zScore > 3.0) return "EXTREMELY BULLISH";
        if (zScore > 2.0) return "VERY BULLISH";
        if (zScore > 1.0) return "BULLISH";
        if (zScore > 0.5) return "SLIGHTLY BULLISH";
        if (zScore > -0.5) return "NEUTRAL";
        if (zScore > -1.0) return "SLIGHTLY BEARISH";
        if (zScore > -2.0) return "BEARISH";
        if (zScore > -3.0) return "VERY BEARISH";
        return "EXTREMELY BEARISH";
    }
    
    // Custom renderers
    class PriceDataListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof PriceData) {
                PriceData data = (PriceData) value;
                setText(String.format("%s - $%.2f (%.2f%%)", data.getTicker(), data.getLatestPrice(), 
                    data.getPercentChange() != null ? data.getPercentChange() * 100 : 0));
            }
            return this;
        }
    }
    
    class ZScoreCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (value instanceof String) {
                try {
                    double zScore = Double.parseDouble((String) value);
                    if (zScore > 1.0) {
                        c.setForeground(new Color(0, 100, 0)); // Dark green for bullish
                    } else if (zScore < -1.0) {
                        c.setForeground(new Color(139, 0, 0)); // Dark red for bearish
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                } catch (NumberFormatException e) {
                    c.setForeground(Color.BLACK);
                }
            }
            
            return c;
        }
    }
    
    class PercentileCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (value instanceof String && ((String) value).contains("%")) {
                try {
                    double percentile = Double.parseDouble(((String) value).replace("%", ""));
                    if (percentile > 80) {
                        c.setForeground(new Color(0, 100, 0)); // Top 20%
                    } else if (percentile < 20) {
                        c.setForeground(new Color(139, 0, 0)); // Bottom 20%
                    } else {
                        c.setForeground(Color.BLACK);
                    }
                } catch (NumberFormatException e) {
                    c.setForeground(Color.BLACK);
                }
            }
            
            return c;
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            DynamicZScoreRankingGUI gui = new DynamicZScoreRankingGUI();
            gui.setVisible(true);
        });
    }
}

class DynamicZScoreAnalyzer {
    private Map<String, Map<String, Double>> zScoreCombinations; // Combination name -> Map<Metric, Weight>
    private Map<String, Double> historicalMeans;
    private Map<String, Double> historicalStdDevs;
    
    public DynamicZScoreAnalyzer() {
        zScoreCombinations = new LinkedHashMap<>();
        historicalMeans = new HashMap<>();
        historicalStdDevs = new HashMap<>();
        initializeDefaultMetrics();
        initializeTechnicalCombinations();
    }
    
    private void initializeDefaultMetrics() {
        // PriceData metrics
        addMetric("price", 100.0, 50.0);
        addMetric("volume", 1500000.0, 800000.0);
        addMetric("percentChange", 0.02, 0.05);
        addMetric("averageVol", 1200000.0, 600000.0);
        addMetric("prevLastDayPrice", 95.0, 45.0);
        
        // Common AnalysisResult metrics with reasonable defaults
        addMetric("ar_rsi", 50.0, 15.0);
        addMetric("ar_sma20", 100.0, 50.0);
        addMetric("ar_sma200", 100.0, 50.0);
        addMetric("ar_macd", 0.0, 0.5);
        addMetric("ar_macdSignal", 0.0, 0.5);
        addMetric("ar_macd359", 0.0, 0.5);
        addMetric("ar_macdSignal359", 0.0, 0.5);
        addMetric("ar_psar001", 100.0, 50.0);
        addMetric("ar_psar005", 100.0, 50.0);
        addMetric("ar_heikenAshiClose", 100.0, 50.0);
        addMetric("ar_heikenAshiOpen", 100.0, 50.0);
        addMetric("ar_highestCloseOpen", 100.0, 50.0);
        addMetric("ar_movingAverageTargetValue", 110.0, 40.0);
        addMetric("ar_movingAverageTargetValuePercentile", 10.0, 8.0);
    }
    
    private void initializeTechnicalCombinations() {
        // Momentum strategy using RSI and MACD
        Map<String, Double> momentumWeights = new HashMap<>();
        momentumWeights.put("ar_rsi", 40.0);
        momentumWeights.put("ar_macd", 30.0);
        momentumWeights.put("ar_macd359", 30.0);
        addCustomCombination("TECH_MOMENTUM", momentumWeights);
        
        // Trend following strategy using moving averages
        Map<String, Double> trendWeights = new HashMap<>();
        trendWeights.put("ar_sma20", 35.0);
        trendWeights.put("ar_sma200", 35.0);
        trendWeights.put("ar_movingAverageTargetValuePercentile", 30.0);
        addCustomCombination("TECH_TREND", trendWeights);
        
        // Breakout strategy using PSAR and MACD
        Map<String, Double> breakoutWeights = new HashMap<>();
        breakoutWeights.put("ar_psar001", 25.0);
        breakoutWeights.put("ar_psar005", 25.0);
        breakoutWeights.put("ar_macd", 25.0);
        breakoutWeights.put("ar_macd359", 25.0);
        addCustomCombination("TECH_BREAKOUT", breakoutWeights);
        
        // Comprehensive technical analysis
        Map<String, Double> comprehensiveWeights = new HashMap<>();
        comprehensiveWeights.put("ar_rsi", 20.0);
        comprehensiveWeights.put("ar_macd", 20.0);
        comprehensiveWeights.put("ar_sma20", 15.0);
        comprehensiveWeights.put("ar_sma200", 15.0);
        comprehensiveWeights.put("ar_psar001", 15.0);
        comprehensiveWeights.put("ar_movingAverageTargetValuePercentile", 15.0);
        addCustomCombination("TECH_COMPREHENSIVE", comprehensiveWeights);
    }
    
    public List<String> discoverAnalysisResultMetrics() {
        List<String> metrics = new ArrayList<>();
        try {
            Method[] methods = AnalysisResult.class.getMethods();
            
            for (Method method : methods) {
                String methodName = method.getName();
                if (methodName.startsWith("get") && methodName.length() > 3 && 
                    !methodName.equals("getClass") && !methodName.equals("getCustomValues")) {
                    
                    String metricName = "ar_" + methodName.substring(3);
                    metricName = Character.toLowerCase(metricName.charAt(0)) + metricName.substring(1);
                    
                    Class<?> returnType = method.getReturnType();
                    if (returnType == Double.class || returnType == double.class || 
                        returnType == Integer.class || returnType == int.class ||
                        returnType == Long.class || returnType == long.class ||
                        returnType == Float.class || returnType == float.class) {
                        
                        metrics.add(metricName);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error discovering AnalysisResult metrics: " + e.getMessage());
        }
        return metrics;
    }
    
    public void addMetric(String metricName, double mean, double stdDev) {
        historicalMeans.put(metricName, mean);
        historicalStdDevs.put(metricName, stdDev);
    }
    
    public void updateMetricStats(String metricName, double newMean, double newStdDev) {
        if (historicalMeans.containsKey(metricName)) {
            historicalMeans.put(metricName, newMean);
            historicalStdDevs.put(metricName, newStdDev);
        }
    }
    
    public List<Map.Entry<String, Double>> rankSymbols(List<PriceData> symbols, String combinationName) {
        Map<String, Double> symbolScores = new HashMap<>();
        
        for (PriceData symbol : symbols) {
            double combinedScore = calculateWeightedZScore(symbol, combinationName);
            symbolScores.put(symbol.getTicker(), combinedScore);
        }
        
        return symbolScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(Collectors.toList());
    }
    
    private double calculateWeightedZScore(PriceData symbol, String combinationName) {
        Map<String, Double> weights = zScoreCombinations.get(combinationName);
        if (weights == null) return 0.0;
        
        double weightedSum = 0.0;
        double totalWeight = 0.0;
        
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            String metric = entry.getKey();
            double weight = entry.getValue();
            
            Double zScore = calculateIndividualZScore(symbol, metric);
            if (zScore != null) {
                weightedSum += zScore * weight;
                totalWeight += weight;
            }
        }
        
        return totalWeight > 0 ? weightedSum / totalWeight : 0.0;
    }
    
    public Double calculateIndividualZScore(PriceData symbol, String metric) {
        try {
            // Check if this is an AnalysisResult metric (prefixed with "ar_")
            if (metric.startsWith("ar_")) {
                // Get the AnalysisResult from PriceData
                Method getResultsMethod = PriceData.class.getMethod("getResults");
                @SuppressWarnings("unchecked")
                Map<String, AnalysisResult> results = (Map<String, AnalysisResult>) getResultsMethod.invoke(symbol);
                
                if (results == null || results.isEmpty()) {
                    return null;
                }
                
                // Use the latest analysis result
                AnalysisResult latestResult = results.values().iterator().next();
                
                // Remove the "ar_" prefix and capitalize the first letter
                String analysisResultMetric = metric.substring(3);
                analysisResultMetric = Character.toUpperCase(analysisResultMetric.charAt(0)) + analysisResultMetric.substring(1);
                
                Method analysisMethod = AnalysisResult.class.getMethod("get" + analysisResultMetric);
                Object value = analysisMethod.invoke(latestResult);
                
                if (value == null) return null;
                
                double numericValue;
                if (value instanceof Number) {
                    numericValue = ((Number) value).doubleValue();
                } else {
                    return null;
                }
                
                return calculateZScore(numericValue, metric);
            } else {
                // Handle regular PriceData metrics
                Method method = PriceData.class.getMethod("get" + Character.toUpperCase(metric.charAt(0)) + metric.substring(1));
                Object value = method.invoke(symbol);
                
                if (value == null) return null;
                
                double numericValue;
                if (value instanceof Number) {
                    numericValue = ((Number) value).doubleValue();
                } else {
                    return null;
                }
                
                return calculateZScore(numericValue, metric);
            }
        } catch (Exception e) {
            System.out.println("Error calculating Z-score for metric " + metric + ": " + e.getMessage());
            return null;
        }
    }
    
    private double calculateZScore(double value, String metric) {
        if (!historicalMeans.containsKey(metric) || !historicalStdDevs.containsKey(metric)) {
            return 0.0;
        }
        
        double mean = historicalMeans.get(metric);
        double stdDev = historicalStdDevs.get(metric);
        
        if (stdDev == 0) return 0.0;
        
        return (value - mean) / stdDev;
    }
    
    public void addCustomCombination(String name, Map<String, Double> weights) {
        zScoreCombinations.put(name, new HashMap<>(weights));
    }
    
    public void updateCombinationWeights(String combinationName, Map<String, Double> newWeights) {
        if (zScoreCombinations.containsKey(combinationName)) {
            zScoreCombinations.put(combinationName, new HashMap<>(newWeights));
        }
    }
    
    public void removeCombination(String combinationName) {
        zScoreCombinations.remove(combinationName);
    }
    
    public Map<String, Double> getCombinationWeights(String combinationName) {
        return zScoreCombinations.containsKey(combinationName) ? 
            new HashMap<>(zScoreCombinations.get(combinationName)) : null;
    }
    
    public List<String> getAvailableCombinations() {
        return new ArrayList<>(zScoreCombinations.keySet());
    }
    
    public List<String> getAvailableMetrics() {
        return new ArrayList<>(historicalMeans.keySet());
    }
    
    public List<String> getTechnicalIndicators() {
        return historicalMeans.keySet().stream()
            .filter(metric -> metric.startsWith("ar_"))
            .collect(Collectors.toList());
    }
    
    public List<String> getPriceDataMetrics() {
        return historicalMeans.keySet().stream()
            .filter(metric -> !metric.startsWith("ar_"))
            .collect(Collectors.toList());
    }
    
    public double getHistoricalMean(String metric) {
        return historicalMeans.getOrDefault(metric, 0.0);
    }
    
    public double getHistoricalStdDev(String metric) {
        return historicalStdDevs.getOrDefault(metric, 1.0);
    }
    
    public void autoCalculateHistoricals(List<PriceData> historicalData) {
        for (String metric : historicalMeans.keySet()) {
            List<Double> values = new ArrayList<>();
            
            for (PriceData data : historicalData) {
                Double value = calculateIndividualZScore(data, metric);
                if (value != null) {
                    values.add(value);
                }
            }
            
            if (!values.isEmpty()) {
                double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double stdDev = Math.sqrt(values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0));
                
                historicalMeans.put(metric, mean);
                historicalStdDevs.put(metric, stdDev);
            }
        }
    }
}