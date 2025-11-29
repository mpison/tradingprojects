package com.quantlabs.stockApp.analysis.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
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
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import org.json.JSONArray;
import org.json.JSONObject;

import com.quantlabs.stockApp.analysis.model.AnalysisMetric;
import com.quantlabs.stockApp.analysis.model.ComparisonType;
import com.quantlabs.stockApp.analysis.model.ZScoreCombination;
import com.quantlabs.stockApp.analysis.service.AnalysisMetricService;
import com.quantlabs.stockApp.analysis.service.HistoricalDataService;
import com.quantlabs.stockApp.analysis.service.ZScoreAnalysisService;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class ZScoreRankingGUI extends JFrame {

	private HistoricalDataService historicalDataService;
	private AnalysisMetricService metricService;
	private ZScoreAnalysisService analysisService;
	private DefaultListModel<PriceData> portfolioListModel;
	private JComboBox<String> combinationComboBox;
	private JTable rankingTable;
	private JTable combinationsTable; 
	private DefaultTableModel rankingTableModel;
	
	private Map<String, Set<String>> customIndicatorCombinations = new HashMap<>();
	
	public static final String ZSCORE_PREFS_KEY = "zScoreConfiguration";
	
	private static final String COLUMN_PREFS_KEY = "additionalColumns";
	private Properties prefs = new Properties();

	public ZScoreRankingGUI() {
		try {
	        this.historicalDataService = new HistoricalDataService();
	        this.metricService = new AnalysisMetricService(historicalDataService);
	        this.analysisService = new ZScoreAnalysisService(metricService, historicalDataService);
	        this.portfolioListModel = new DefaultListModel<>();

	        initializeUI();
	        loadSampleData();
	        loadConfiguration();
	    } catch (Exception e) {
	        // Log error and show user-friendly message
	        System.err.println("Failed to initialize Z-Score GUI: " + e.getMessage());
	        e.printStackTrace();
	        JOptionPane.showMessageDialog(null, 
	            "Failed to initialize analysis services. Please restart the application.",
	            "Initialization Error", JOptionPane.ERROR_MESSAGE);
	    }
	}
	
	public ZScoreRankingGUI(List<PriceData> portfolio, Map<String, Set<String>> customIndicatorCombinations) {
	    this.historicalDataService = new HistoricalDataService();
	    this.customIndicatorCombinations = customIndicatorCombinations;
	    this.metricService = new AnalysisMetricService(historicalDataService, customIndicatorCombinations);
	    this.analysisService = new ZScoreAnalysisService(metricService, historicalDataService);
	    this.portfolioListModel = new DefaultListModel<>();
	    // Add portfolio data
	    if (portfolio != null) {
	        for (PriceData data : portfolio) {
	            portfolioListModel.addElement(data);
	        }
	    }
	    initializeUI();
	    loadConfiguration();
	    
	 // Only load sample if portfolio is empty
	    if (portfolio == null || portfolio.isEmpty()) {
	        loadSampleData();
	    }
	    
	    //loadInitialHistoricalData();  // Keep this if needed for historical setup; remove if it overrides portfolio
	}
	
	public ZScoreRankingGUI(List<PriceData> portfolio) {
	    this.historicalDataService = new HistoricalDataService();
	    this.metricService = new AnalysisMetricService(historicalDataService, customIndicatorCombinations);
	    this.analysisService = new ZScoreAnalysisService(metricService, historicalDataService);
	    this.portfolioListModel = new DefaultListModel<>();
	    // Add portfolio data
	    if (portfolio != null) {
	        for (PriceData data : portfolio) {
	            portfolioListModel.addElement(data);
	        }
	    }
	    initializeUI();
	    loadConfiguration();
	    
	 // Only load sample if portfolio is empty
	    if (portfolio == null || portfolio.isEmpty()) {
	        loadSampleData();
	    }
	    
	    //loadInitialHistoricalData();  // Keep this if needed for historical setup; remove if it overrides portfolio
	}

	private void initializeUI() {
		setTitle("Z-Score Ranking Analyzer with Advanced Comparison");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(1800, 1000); // Increased size for new features
		setLocationRelativeTo(null);
		
		// Add window listener to save configuration on close
	    addWindowListener(new WindowAdapter() {
	        @Override
	        public void windowClosing(WindowEvent e) {
	            saveConfiguration();
	        }
	    });

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Portfolio", createPortfolioPanel());
		tabbedPane.addTab("Ranking", createRankingPanel());
		tabbedPane.addTab("Combination Manager", createCombinationManagerPanel());
		tabbedPane.addTab("Comparison Settings", createComparisonSettingsPanel()); // NEW TAB
		tabbedPane.addTab("Metric Explorer", createMetricExplorerPanel());
		tabbedPane.addTab("Technical Analysis", createTechnicalAnalysisPanel());
		tabbedPane.addTab("Timeframe Analysis", createTimeframeAnalysisPanel());

		add(tabbedPane);
	}

	private void loadInitialHistoricalData() {
		// This would load from database or file in a real application
		System.out.println("Loading initial historical data...");

		// Example: Add some historical data points
		historicalDataService.addDataPoint("price", 145.0);
		historicalDataService.addDataPoint("price", 150.0);
		historicalDataService.addDataPoint("price", 155.0);
		historicalDataService.addDataPoint("price", 140.0);
		historicalDataService.addDataPoint("price", 160.0);

		historicalDataService.addDataPoint("currentVolume", 1800000.0);
		historicalDataService.addDataPoint("currentVolume", 2000000.0);
		historicalDataService.addDataPoint("currentVolume", 2200000.0);
		historicalDataService.addDataPoint("currentVolume", 1900000.0);
		historicalDataService.addDataPoint("currentVolume", 2100000.0);

		System.out.println("Historical mean for price: " + historicalDataService.calculateHistoricalMean("price"));
		System.out.println("Historical std dev for price: " + historicalDataService.calculateHistoricalStdDev("price"));
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

	    // Control panel with enhanced features
	    JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	    controlPanel.add(new JLabel("Select Combination:"));
	    
	    combinationComboBox = new JComboBox<>();
	    updateCombinationComboBox();
	    controlPanel.add(combinationComboBox);

	    // Add column management button
	    JButton manageColumnsButton = new JButton("Manage Columns");
	    manageColumnsButton.addActionListener(e -> showColumnManagerDialog());
	    controlPanel.add(manageColumnsButton);

	    JButton detailsButton = new JButton("Show Comparison Types");
	    detailsButton.addActionListener(e -> showCurrentCombinationDetails());
	    controlPanel.add(detailsButton);

	    JButton rankButton = new JButton("Rank Symbols");
	    JButton exportButton = new JButton("Export Results");

	    rankButton.addActionListener(e -> rankSymbols());
	    exportButton.addActionListener(e -> exportResults());

	    controlPanel.add(rankButton);
	    controlPanel.add(exportButton);

	    // Results table with dynamic columns - initialize properly
	    initializeRankingTable();
	    
	    // Create scroll pane with proper settings for horizontal scrolling
	    JScrollPane tableScrollPane = new JScrollPane(rankingTable,
	        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
	        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	    
	    tableScrollPane.setName("rankingScrollPane");
	    
	    // Ensure the table header is always visible
	    rankingTable.setAutoCreateRowSorter(true);
	    rankingTable.setFillsViewportHeight(true);
	    
	    // Add tooltip to indicate scrollability
	    tableScrollPane.setToolTipText("Scroll horizontally to see all columns");
	    
	    // Add a label to indicate many columns
	    JLabel scrollHintLabel = new JLabel("← → Scroll to see more columns");
	    scrollHintLabel.setForeground(Color.GRAY);
	    scrollHintLabel.setFont(scrollHintLabel.getFont().deriveFont(Font.ITALIC, 11f));
	    
	    JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	    hintPanel.add(scrollHintLabel);
	    
	    JPanel tableContainer = new JPanel(new BorderLayout());
	    tableContainer.add(tableScrollPane, BorderLayout.CENTER);
	    tableContainer.add(hintPanel, BorderLayout.SOUTH);

	    rankingPanel.add(controlPanel, BorderLayout.NORTH);
	    rankingPanel.add(tableContainer, BorderLayout.CENTER);

	    return rankingPanel;
	}
	
	private void initializeRankingTable() {
	    // Base columns that are always shown
	    String[] baseColumns = { "Rank", "Symbol", "Price", "Z-Score", "Interpretation", "Percentile" };
	    
	    // Load additional columns from preferences
	    List<String> additionalColumns = loadAdditionalColumns();
	    
	    // Combine base and additional columns
	    List<String> allColumns = new ArrayList<>();
	    allColumns.addAll(Arrays.asList(baseColumns));
	    allColumns.addAll(additionalColumns);
	    
	    // Create new table model
	    DefaultTableModel newModel = new DefaultTableModel(allColumns.toArray(new String[0]), 0) {
	        @Override
	        public boolean isCellEditable(int row, int column) {
	            return false;
	        }
	        
	        @Override
	        public Class<?> getColumnClass(int columnIndex) {
	            String columnName = getColumnName(columnIndex);
	            if (columnName.equals("Rank") || columnName.equals("Z-Score") || 
	                columnName.equals("Percentile") || additionalColumns.contains(columnName)) {
	                return Double.class;
	            }
	            return String.class;
	        }
	    };
	    
	    // Transfer existing data if any
	    if (rankingTableModel != null) {
	        for (int row = 0; row < rankingTableModel.getRowCount(); row++) {
	            Vector<Object> rowData = new Vector<>();
	            for (int col = 0; col < rankingTableModel.getColumnCount(); col++) {
	                rowData.add(rankingTableModel.getValueAt(row, col));
	            }
	            // Pad with nulls for new columns if needed
	            while (rowData.size() < newModel.getColumnCount()) {
	                rowData.add(null);
	            }
	            newModel.addRow(rowData);
	        }
	    }
	    
	    rankingTableModel = newModel;
	    
	    if (rankingTable == null) {
	        rankingTable = new JTable(rankingTableModel);
	        rankingTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // ← CRITICAL: Turn off auto-resize
	        rankingTable.setRowSorter(new TableRowSorter<>(rankingTableModel));
	        rankingTable.setRowHeight(25);
	    } else {
	        rankingTable.setModel(rankingTableModel);
	        rankingTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // ← Ensure it's off
	        rankingTable.setRowSorter(new TableRowSorter<>(rankingTableModel));
	    }
	    
	    // Set optimal column widths
	    setOptimalColumnWidths();
	    
	    // Set custom renderers
	    setColumnRenderers();
	}
	
	private void setOptimalColumnWidths() {
	    if (rankingTable == null) return;
	    
	    TableColumnModel columnModel = rankingTable.getColumnModel();
	    FontMetrics fontMetrics = rankingTable.getFontMetrics(rankingTable.getFont());
	    
	    for (int i = 0; i < rankingTableModel.getColumnCount(); i++) {
	        TableColumn column = columnModel.getColumn(i);
	        String columnName = rankingTableModel.getColumnName(i);
	        
	        // Calculate optimal width based on column name
	        int headerWidth = fontMetrics.stringWidth(columnName) + 20; // Padding
	        
	        // For numeric columns, calculate based on typical values
	        int contentWidth = 80; // Default width
	        
	        if (columnName.equals("Rank")) {
	            contentWidth = 50;
	        } else if (columnName.equals("Symbol")) {
	            contentWidth = 80;
	        } else if (columnName.equals("Price")) {
	            contentWidth = 90;
	        } else if (columnName.equals("Z-Score")) {
	            contentWidth = 80;
	        } else if (columnName.equals("Interpretation")) {
	            contentWidth = 120;
	        } else if (columnName.equals("Percentile")) {
	            contentWidth = 80;
	        } else if (columnName.toLowerCase().contains("currentVolume")) {
	            contentWidth = 100; // Wider for volume numbers
	        } else if (columnName.toLowerCase().contains("percent")) {
	            contentWidth = 90;
	        }
	        
	        // Use the wider of header or content width
	        column.setPreferredWidth(Math.max(headerWidth, contentWidth));
	        column.setMinWidth(50); // Minimum width to prevent disappearing columns
	        column.setMaxWidth(300); // Maximum width to prevent overly wide columns
	    }
	}
	
	private void setColumnRenderers() {
	    List<String> additionalColumns = loadAdditionalColumns();
	    
	    for (int i = 0; i < rankingTableModel.getColumnCount(); i++) {
	        String columnName = rankingTableModel.getColumnName(i);
	        
	        switch (columnName) {
	            case "Z-Score":
	                rankingTable.getColumnModel().getColumn(i).setCellRenderer(new ZScoreCellRenderer());
	                break;
	            case "Percentile":
	                rankingTable.getColumnModel().getColumn(i).setCellRenderer(new PercentileCellRenderer());
	                break;
	            default:
	                if (additionalColumns.contains(columnName)) {
	                    rankingTable.getColumnModel().getColumn(i).setCellRenderer(new NumericCellRenderer());
	                }
	                break;
	        }
	    }
	}
 
	private JPanel createCombinationManagerPanel() {
		JPanel managerPanel = new JPanel(new BorderLayout(5, 5));
	    
	    // Current combinations list with enhanced info
	    DefaultTableModel combinationsListModel = new DefaultTableModel(
	        new String[]{"Combination", "Metrics", "Primary Comparison"}, 0);
	    
	    updateCombinationsTable(combinationsListModel); // Use table method
	    
	    combinationsTable = new JTable(combinationsListModel);
	    JScrollPane listScrollPane = new JScrollPane(combinationsTable);
	    listScrollPane.setBorder(BorderFactory.createTitledBorder("Available Combinations"));

		// Combination details
		JTextArea detailsArea = new JTextArea(10, 40);
		detailsArea.setEditable(false);
		detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		JScrollPane detailsScrollPane = new JScrollPane(detailsArea);
		detailsScrollPane.setBorder(BorderFactory.createTitledBorder("Combination Details"));

		combinationsTable.getSelectionModel().addListSelectionListener(e -> {
			int selectedRow = combinationsTable.getSelectedRow();
			if (selectedRow >= 0) {
				String combination = (String) combinationsTable.getValueAt(selectedRow, 0);
				detailsArea.setText(getEnhancedCombinationDetails(combination));
			}
		});

		// Control buttons
		JPanel controlPanel = new JPanel(new GridLayout(1, 4, 5, 5));
		JButton viewDetailsButton = new JButton("View Details");
		JButton editButton = new JButton("Edit");
		JButton removeButton = new JButton("Remove");
		JButton addButton = new JButton("Add New");

		viewDetailsButton.addActionListener(e -> {
			int selectedRow = combinationsTable.getSelectedRow();
			if (selectedRow >= 0) {
				String combination = (String) combinationsTable.getValueAt(selectedRow, 0);
				showCombinationDetails(combination);
			}
		});

		editButton.addActionListener(e -> {
	        int selectedRow = combinationsTable.getSelectedRow();
	        if (selectedRow >= 0) {
	            String combination = (String) combinationsTable.getValueAt(selectedRow, 0);
	            editCombination(combination, combinationsListModel); // Pass table model
	        }
	    });
	    
	    removeButton.addActionListener(e -> {
	        int selectedRow = combinationsTable.getSelectedRow();
	        if (selectedRow >= 0) {
	            String combination = (String) combinationsTable.getValueAt(selectedRow, 0);
	            removeCombination(combination, combinationsListModel); // Pass table model
	        }
	    });
	    
	    addButton.addActionListener(e -> showAddCombinationDialog(combinationsListModel)); // Pass table model

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
	
	private void updateCombinationsTable() {
	    // Assume combinationsTable uses a DefaultTableModel
	    DefaultTableModel model = (DefaultTableModel) combinationsTable.getModel();
	    
	    // Clear existing rows
	    model.setRowCount(0);
	    
	    // Get all combinations
	    List<String> combinationNames = analysisService.getAvailableCombinations();
	    
	    // Add each combination to the table
	    for (String name : combinationNames) {
	        ZScoreCombination combination = analysisService.getCombination(name);
	        Map<String, Double> weights = combination.getWeights();
	        
	        // Format weights as a string or add individual columns
	        String weightsStr = weights.entrySet().stream()
	            .map(e -> e.getKey() + ": " + String.format("%.1f", e.getValue()))
	            .collect(Collectors.joining(", "));
	        
	        // Add row (adjust columns based on your table structure)
	        model.addRow(new Object[]{name, weightsStr});
	    }
	}

	private void updateCombinationsTable(DefaultTableModel tableModel) {
	    tableModel.setRowCount(0); // Clear existing rows
	    
	    for (String combination : analysisService.getAvailableCombinations()) {
	        ZScoreCombination combo = analysisService.getCombination(combination);
	        if (combo != null) {
	            // Determine primary comparison type
	            String primaryComparison = "Mixed";
	            long crossSymbolCount = combo.getWeights().keySet().stream()
	                .filter(metric -> {
	                    AnalysisMetric m = metricService.getMetric(metric);
	                    return m != null && m.getComparisonType() == ComparisonType.CROSS_SYMBOL;
	                })
	                .count();
	            
	            if (crossSymbolCount == combo.getWeights().size()) {
	                primaryComparison = "Cross-Symbol";
	            } else if (crossSymbolCount == 0) {
	                primaryComparison = "Historical";
	            }
	            
	            tableModel.addRow(new Object[]{
	                combination,
	                combo.getWeights().size() + " metrics",
	                primaryComparison
	            });
	        }
	    }
	}

	private String getEnhancedCombinationDetails(String combinationName) {
		ZScoreCombination combination = analysisService.getCombination(combinationName);
		if (combination == null)
			return "Combination not found: " + combinationName;

		StringBuilder details = new StringBuilder();
		details.append("Combination: ").append(combinationName).append("\n\n");
		details.append("Metrics, Weights and Comparison Settings:\n");
		details.append("-----------------------------------------\n");

		double totalWeight = 0;
		int crossSymbolCount = 0;
		int historicalCount = 0;
		int lowerBetterCount = 0;

		for (String metric : combination.getWeights().keySet()) {
			AnalysisMetric metricInfo = metricService.getMetric(metric);
			double weight = combination.getWeights().get(metric);

			if (metricInfo != null) {
				String comparisonType = metricInfo.getComparisonType().toString();
				String lowerBetter = metricInfo.isLowerIsBetter() ? "Yes" : "No";

				if (metricInfo.getComparisonType() == ComparisonType.CROSS_SYMBOL) {
					crossSymbolCount++;
				} else {
					historicalCount++;
				}

				if (metricInfo.isLowerIsBetter()) {
					lowerBetterCount++;
				}

				details.append(String.format("  • %-25s: %6.1f%% (%s, Lower Better: %s)\n", metric, weight,
						comparisonType, lowerBetter));
			} else {
				details.append(String.format("  • %-25s: %6.1f%% (Unknown settings)\n", metric, weight));
			}

			totalWeight += weight;
		}

		details.append("\nSummary:\n");
		details.append("--------\n");
		details.append(String.format("Total Weight: %.1f%%\n", totalWeight));
		details.append(String.format("Cross-Symbol Metrics: %d\n", crossSymbolCount));
		details.append(String.format("Historical Metrics: %d\n", historicalCount));
		details.append(String.format("Metrics where Lower is Better: %d\n", lowerBetterCount));

		return details.toString();
	}

	private JPanel createMetricExplorerPanel() {
		JPanel explorerPanel = new JPanel(new BorderLayout());

		// Available metrics list - SORTED BY NAME
		DefaultListModel<String> metricsListModel = new DefaultListModel<>();
		metricService.getAllMetricNames().stream().sorted() // Sort alphabetically
				.forEach(metricsListModel::addElement);

		JList<String> metricsList = new JList<>(metricsListModel);
		JScrollPane listScrollPane = new JScrollPane(metricsList);
		listScrollPane.setBorder(BorderFactory.createTitledBorder("Available Metrics (Sorted)"));

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

		// Technical indicators list - SORTED BY NAME with comparison info
		DefaultTableModel techIndicatorsModel = new DefaultTableModel(
				new String[] { "Technical Indicator", "Comparison Type", "Lower Better", "Timeframe" }, 0);

		metricService.getTechnicalIndicators().stream().sorted().forEach(metricFullName -> {
			AnalysisMetric metric = metricService.getMetric(metricFullName);
			if (metric != null) {
				techIndicatorsModel.addRow(new Object[] { metricFullName, metric.getComparisonType(),
						metric.isLowerIsBetter() ? "Yes" : "No", metric.getTimeframe() });
			}
		});

		JTable techTable = new JTable(techIndicatorsModel);
		JScrollPane techScroll = new JScrollPane(techTable);
		techScroll
				.setBorder(BorderFactory.createTitledBorder("Technical Indicators with Comparison Settings (Sorted)"));

		// Pre-defined technical combinations with comparison info
		DefaultTableModel techCombinationsModel = new DefaultTableModel(
				new String[] { "Combination", "Metrics Count", "Comparison Types" }, 0);

		analysisService.getAvailableCombinations().stream().filter(combo -> combo.startsWith("TECH_")).sorted()
				.forEach(comboName -> {
					ZScoreCombination combo = analysisService.getCombination(comboName);
					if (combo != null) {
						// Count different comparison types
						long crossSymbolCount = combo.getWeights().keySet().stream().filter(metric -> {
							AnalysisMetric m = metricService.getMetric(metric);
							return m != null && m.getComparisonType() == ComparisonType.CROSS_SYMBOL;
						}).count();

						long historicalCount = combo.getWeights().size() - crossSymbolCount;

						techCombinationsModel.addRow(new Object[] { comboName, combo.getWeights().size(),
								"Cross: " + crossSymbolCount + ", Historical: " + historicalCount });
					}
				});

		JTable techComboTable = new JTable(techCombinationsModel);
		JScrollPane techComboScroll = new JScrollPane(techComboTable);
		techComboScroll.setBorder(BorderFactory.createTitledBorder("Technical Combinations (Sorted)"));

		// Details area
		JTextArea techDetailsArea = new JTextArea(10, 50);
		techDetailsArea.setEditable(false);
		JScrollPane techDetailsScroll = new JScrollPane(techDetailsArea);
		techDetailsScroll.setBorder(BorderFactory.createTitledBorder("Combination Details"));

		// Add selection listeners
		techTable.getSelectionModel().addListSelectionListener(e -> {
			int selectedRow = techTable.getSelectedRow();
			if (selectedRow >= 0) {
				String metricName = (String) techTable.getValueAt(selectedRow, 0);
				techDetailsArea.setText(getMetricDetails(metricName));
			}
		});

		techComboTable.getSelectionModel().addListSelectionListener(e -> {
			int selectedRow = techComboTable.getSelectedRow();
			if (selectedRow >= 0) {
				String comboName = (String) techComboTable.getValueAt(selectedRow, 0);
				techDetailsArea.setText(getEnhancedCombinationDetails(comboName));
			}
		});

		// Layout
		JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, techScroll, techComboScroll);
		leftSplit.setDividerLocation(250);

		JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, techDetailsScroll);
		mainSplit.setDividerLocation(400);

		techPanel.add(mainSplit, BorderLayout.CENTER);
		return techPanel;
	}

	private JPanel createTimeframeAnalysisPanel() {
		JPanel timeframePanel = new JPanel(new BorderLayout(5, 5));

		// Timeframe selection
		JPanel timeframeSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		timeframeSelectionPanel.add(new JLabel("Select Timeframe:"));

		JComboBox<String> timeframeComboBox = new JComboBox<>(metricService.getAllTimeframes());
		timeframeSelectionPanel.add(timeframeComboBox);

		// Metrics for selected timeframe - SORTED BY NAME
		DefaultListModel<String> timeframeMetricsModel = new DefaultListModel<>();
		JList<String> timeframeMetricsList = new JList<>(timeframeMetricsModel);
		JScrollPane timeframeScroll = new JScrollPane(timeframeMetricsList);
		timeframeScroll.setBorder(BorderFactory.createTitledBorder("Metrics for Timeframe (Sorted)"));

		timeframeComboBox.addActionListener(e -> {
			String selectedTimeframe = (String) timeframeComboBox.getSelectedItem();
			if (selectedTimeframe != null) {
				timeframeMetricsModel.clear();
				metricService.getMetricsByTimeframe(selectedTimeframe).stream().sorted() // Sort alphabetically
						.forEach(timeframeMetricsModel::addElement);
			}
		});

		// Metric details
		JTextArea timeframeDetailsArea = new JTextArea(10, 50);
		timeframeDetailsArea.setEditable(false);
		JScrollPane timeframeDetailsScroll = new JScrollPane(timeframeDetailsArea);
		timeframeDetailsScroll.setBorder(BorderFactory.createTitledBorder("Metric Details"));

		timeframeMetricsList.addListSelectionListener(e -> {
			String selectedMetric = timeframeMetricsList.getSelectedValue();
			if (selectedMetric != null) {
				timeframeDetailsArea.setText(getMetricDetails(selectedMetric));
			}
		});

		// Trigger initial load
		timeframeComboBox.setSelectedIndex(0);

		// Layout
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, timeframeScroll, timeframeDetailsScroll);
		splitPane.setDividerLocation(300);

		timeframePanel.add(timeframeSelectionPanel, BorderLayout.NORTH);
		timeframePanel.add(splitPane, BorderLayout.CENTER);

		return timeframePanel;
	}

	private JPanel createComparisonSettingsPanel() {
		JPanel settingsPanel = new JPanel(new BorderLayout(5, 5));

		// Metrics table with comparison settings
		String[] columnNames = { "Metric", "Comparison Type", "Lower is Better", "Timeframe" };
		DefaultTableModel metricsModel = new DefaultTableModel(columnNames, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return column >= 1; // Only comparison settings are editable
			}

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				if (columnIndex == 1)
					return ComparisonType.class;
				if (columnIndex == 2)
					return Boolean.class;
				return String.class;
			}
		};

		// Populate with all metrics sorted by name
		metricService.getAllMetricNames().stream().sorted().forEach(metricFullName -> {
			AnalysisMetric metric = metricService.getMetric(metricFullName);
			if (metric != null) {
				metricsModel.addRow(new Object[] { metricFullName, metric.getComparisonType(), metric.isLowerIsBetter(),
						metric.getTimeframe() != null ? metric.getTimeframe() : "N/A" });
			}
		});

		JTable metricsTable = new JTable(metricsModel);
		metricsTable.getColumnModel().getColumn(1)
				.setCellEditor(new DefaultCellEditor(new JComboBox<>(ComparisonType.values())));
		metricsTable.getColumnModel().getColumn(2)
				.setCellEditor(new DefaultCellEditor(new JComboBox<>(new Boolean[] { true, false })));

		JScrollPane tableScroll = new JScrollPane(metricsTable);
		tableScroll.setBorder(BorderFactory.createTitledBorder("Metric Comparison Settings (Sorted)"));

		// Control buttons
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton saveButton = new JButton("Save Settings");
		JButton resetButton = new JButton("Reset to Defaults");

		saveButton.addActionListener(e -> saveComparisonSettings(metricsModel));
		resetButton.addActionListener(e -> resetComparisonSettings(metricsModel));

		buttonPanel.add(resetButton);
		buttonPanel.add(saveButton);

		settingsPanel.add(tableScroll, BorderLayout.CENTER);
		settingsPanel.add(buttonPanel, BorderLayout.SOUTH);

		return settingsPanel;
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
						.prevLastDayPrice(Double.parseDouble(prevPriceField.getText())).build();

				portfolioListModel.addElement(data);
				dialog.dispose();
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(dialog, "Invalid input: " + ex.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
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
	
	private void showCurrentCombinationDetails() {
		String selected = (String) combinationComboBox.getSelectedItem();
		if (selected != null) {
			ZScoreCombination combo = analysisService.getCombination(selected);
			if (combo != null) {
				StringBuilder details = new StringBuilder();
				details.append("Combination: ").append(selected).append("\n\n");
				details.append("Metrics and Comparison Types:\n");

				for (String metric : combo.getWeights().keySet()) {
					AnalysisMetric metricInfo = metricService.getMetric(metric);
					if (metricInfo != null) {
						details.append(String.format("  • %-25s: %6.1f%% (%s, %s)\n", metric,
								combo.getWeights().get(metric), metricInfo.getComparisonType(),
								metricInfo.isLowerIsBetter() ? "Lower Better" : "Higher Better"));
					}
				}

				JOptionPane.showMessageDialog(this, details.toString(), "Comparison Details: " + selected,
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	private void saveComparisonSettings(DefaultTableModel model) {
		try {
			for (int i = 0; i < model.getRowCount(); i++) {
				String metricFullName = (String) model.getValueAt(i, 0);
				ComparisonType comparisonType = (ComparisonType) model.getValueAt(i, 1);
				Boolean lowerIsBetter = (Boolean) model.getValueAt(i, 2);

				metricService.updateMetricSettings(metricFullName, comparisonType, lowerIsBetter);
			}
			JOptionPane.showMessageDialog(this, "Comparison settings saved successfully!", "Success",
					JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "Error saving settings: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void resetComparisonSettings(DefaultTableModel model) {
		int confirm = JOptionPane.showConfirmDialog(this, "Reset all comparison settings to defaults?", "Confirm Reset",
				JOptionPane.YES_NO_OPTION);

		if (confirm == JOptionPane.YES_OPTION) {
			metricService.resetAllComparisonSettings();
			refreshComparisonSettingsTable(model);
			JOptionPane.showMessageDialog(this, "Settings reset to defaults", "Info", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void refreshComparisonSettingsTable(DefaultTableModel model) {
		model.setRowCount(0);
		for (String metricFullName : metricService.getAllMetricNames()) {
			AnalysisMetric metric = metricService.getMetric(metricFullName);
			if (metric != null) {
				model.addRow(new Object[] { metricFullName, metric.getComparisonType(), metric.isLowerIsBetter(),
						metric.getTimeframe() != null ? metric.getTimeframe() : "N/A" });
			}
		}
	}
	
	private void refreshRankingTable() {
	    // Store current sorting
	    RowSorter<?> sorter = rankingTable.getRowSorter();
	    List<? extends RowSorter.SortKey> sortKeys = sorter != null ? sorter.getSortKeys() : null;
	    
	    // Store selection
	    int selectedRow = rankingTable.getSelectedRow();
	    
	    // Store scroll position
	    JViewport viewport = ((JScrollPane) rankingTable.getParent().getParent()).getViewport();
	    Point scrollPosition = viewport.getViewPosition();
	    
	    // Reinitialize the table
	    initializeRankingTable();
	    
	    // Reapply sorting if it existed
	    if (sorter != null && sortKeys != null && !sortKeys.isEmpty()) {
	        rankingTable.getRowSorter().setSortKeys(sortKeys);
	    }
	    
	    // Restore selection if possible
	    if (selectedRow >= 0 && selectedRow < rankingTable.getRowCount()) {
	        rankingTable.setRowSelectionInterval(selectedRow, selectedRow);
	    }
	    
	    // Restore scroll position
	    SwingUtilities.invokeLater(() -> {
	        viewport.setViewPosition(scrollPosition);
	    });
	    
	    // Repaint the UI
	    revalidate();
	    repaint();
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
				new PriceData.Builder("AAPL", 150.0).currentVolume(2000000L).percentChange(0.03).averageVol(1500000.0)
						.prevLastDayPrice(145.0).build(), 
				new PriceData.Builder("MSFT", 300.0).currentVolume(1500000L).percentChange(0.015).averageVol(1200000.0)
						.prevLastDayPrice(295.0).build(),
				new PriceData.Builder("GOOGL", 2700.0).currentVolume(800000L).percentChange(0.025).averageVol(900000.0)
						.prevLastDayPrice(2630.0).build(),
				new PriceData.Builder("AMZN", 3200.0).currentVolume(1200000L).percentChange(-0.01).averageVol(1500000.0)
						.prevLastDayPrice(3230.0).build(),
				new PriceData.Builder("TSLA", 800.0).currentVolume(2500000L).percentChange(0.05).averageVol(1800000.0)
						.prevLastDayPrice(760.0).build() };

		for (PriceData data : sampleData) {
			portfolioListModel.addElement(data);
		}
	}

	// In ZScoreRankingGUI.java, update the rankSymbols method
	private void rankSymbols() {
	    if (portfolioListModel.isEmpty()) {
	        JOptionPane.showMessageDialog(this, "Portfolio is empty. Please add symbols first.", "Error",
	                JOptionPane.ERROR_MESSAGE);
	        return;
	    }

	    String selectedCombination = (String) combinationComboBox.getSelectedItem();
	    if (selectedCombination == null) {
	        JOptionPane.showMessageDialog(this, "Please select a combination first.", "Error",
	                JOptionPane.ERROR_MESSAGE);
	        return;
	    }

	    // Convert list model to list
	    List<PriceData> symbols = new ArrayList<>();
	    for (int i = 0; i < portfolioListModel.getSize(); i++) {
	        symbols.add(portfolioListModel.getElementAt(i));
	    }

	    try {
	        // Update historical data with current portfolio
	        for (PriceData symbol : symbols) {
	            analysisService.updateAllWithNewData(symbol);
	        }

	        try {
	            // Rank symbols - this will now work with the fixed method signature
	            List<Map.Entry<String, Double>> rankedSymbols = analysisService.rankSymbols(symbols, selectedCombination);

	            // Display results
	            rankingTableModel.setRowCount(0);
	            int rank = 1;

	            for (Map.Entry<String, Double> entry : rankedSymbols) {
	            	PriceData symbolData = symbols.stream().filter(data -> data.getTicker() != null && data.getTicker().equals(entry.getKey()))
	                        .findFirst().orElse(null);

	                if (symbolData != null) {
	                    double percentile = (double) (rankedSymbols.size() - rank + 1) / rankedSymbols.size() * 100;
	                    String interpretation = getZScoreInterpretation(entry.getValue());

	                    // Create row with base data
	                    List<Object> rowData = new ArrayList<>();
	                    rowData.add(rank++);
	                    rowData.add(entry.getKey());
	                    rowData.add(String.format("$%.2f", symbolData.getLatestPrice()));
	                    rowData.add(entry.getValue());
	                    rowData.add(interpretation);
	                    rowData.add(percentile);

	                    // Add additional metric values
	                    List<String> additionalColumns = loadAdditionalColumns();
	                    for (String metric : additionalColumns) {
	                        try {
	                            Object metricValue = extractMetricValue(symbolData, metric, null);
	                            rowData.add(metricValue != null ? metricValue : 0.0);
	                        } catch (Exception ex) {
	                            rowData.add("N/A");
	                        }
	                    }

	                    rankingTableModel.addRow(rowData.toArray());
	                }
	            }
	        } catch (IllegalArgumentException e) {
	            JOptionPane.showMessageDialog(this, "Ranking error: " + e.getMessage(), "Error",
	                    JOptionPane.ERROR_MESSAGE);
	        }
	    } catch (Exception e) {
	        JOptionPane.showMessageDialog(this, "Error ranking symbols: " + e.getMessage(), "Error",
	                JOptionPane.ERROR_MESSAGE);
	    }
	}
	
	private void exportResults() {
		JOptionPane.showMessageDialog(this, "Export functionality would be implemented here", "Info",
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	private Object extractMetricValue(PriceData symbolData, String metricFullName, String timeframe) {
	    try {
	        String[] parts = metricFullName.split("_", 2);
	        String metricName = parts.length > 1 ? parts[1] : parts[0];
	        
	        // Use reflection to get the value from PriceData
	        String methodName = "get" + Character.toUpperCase(metricName.charAt(0)) + metricName.substring(1);
	        
	        String[] allTimeFrames = AnalysisMetricService.getAllTimeframes();
	        
	        ArrayList<String> allTimeFramesList = new ArrayList<>(Arrays.asList(allTimeFrames));
	        
	        if(allTimeFramesList.contains(parts[0])) {
	        	Map<String, AnalysisResult> analysisResultMap = symbolData.getResults();
	        	AnalysisResult analysisResult = analysisResultMap.get(parts[0]);
	        	
	        	if(!methodExists(AnalysisResult.class, methodName)) {
	        		
	        		methodName = metricName.charAt(0) + metricName.substring(1);
	        		
	        		String customResult = (String) analysisResult.getCustomIndicatorValue(methodName);
	        		if(customResult != null) {
	        			return customResult;
	        		}
	        	}
	        	
	        	Method method = AnalysisResult.class.getMethod(methodName);
	        	
	        	return method.invoke(analysisResult);
	        }else {	        
		        Method method = PriceData.class.getMethod(methodName);
		        return method.invoke(symbolData);
	        }
	    } catch (Exception e) {
	        System.out.println("Error extracting metric " + metricFullName + ": " + e.getMessage());
	        return null;
	    }
	}
	
	
	
	// Helper method
    public static boolean methodExists(Class<?> clazz, String methodNames) {
        try {
            Method method = clazz.getMethod(methodNames);
            return true; // Found it
        } catch (NoSuchMethodException e) {
            return false; // Not found
        }
    }

	// Helper method to update combination combo box
	private void updateCombinationComboBox() {
	    String previouslySelected = (String) combinationComboBox.getSelectedItem();
	    combinationComboBox.removeAllItems();
	    
	    List<String> combinations = analysisService.getAvailableCombinations();
	    for (String combination : combinations) {
	        combinationComboBox.addItem(combination);
	    }
	    
	    // Try to restore previous selection if it still exists
	    if (previouslySelected != null && combinations.contains(previouslySelected)) {
	        combinationComboBox.setSelectedItem(previouslySelected);
	    } else if (!combinations.isEmpty()) {
	        combinationComboBox.setSelectedIndex(0);
	    } else {
	        // No combinations available
	        combinationComboBox.addItem("No combinations available");
	        combinationComboBox.setEnabled(false);
	    }
	}
	
	private void refreshCombinationManagerTable() {
	    // Find the Combination Manager tab and refresh its table
	    Container container = getContentPane();
	    if (container instanceof JTabbedPane) {
	        JTabbedPane tabbedPane = (JTabbedPane) container;
	        
	        // Find the Combination Manager panel (tab index 2)
	        if (tabbedPane.getTabCount() > 2) {
	            Component comboManagerTab = tabbedPane.getComponentAt(2);
	            if (comboManagerTab instanceof JScrollPane) {
	                JScrollPane scrollPane = (JScrollPane) comboManagerTab;
	                Component view = scrollPane.getViewport().getView();
	                if (view instanceof JPanel) {
	                    JPanel panel = (JPanel) view;
	                    // Find the table in the panel
	                    findAndRefreshCombinationTable(panel);
	                }
	            } else if (comboManagerTab instanceof JPanel) {
	                findAndRefreshCombinationTable((JPanel) comboManagerTab);
	            }
	        }
	    }
	}

	private void findAndRefreshCombinationTable(Container container) {
	    for (Component comp : container.getComponents()) {
	        if (comp instanceof JScrollPane) {
	            JScrollPane scrollPane = (JScrollPane) comp;
	            Component view = scrollPane.getViewport().getView();
	            if (view instanceof JTable) {
	                JTable table = (JTable) view;
	                DefaultTableModel model = (DefaultTableModel) table.getModel();
	                updateCombinationsTable(model); // Refresh the table data
	                return;
	            }
	        } else if (comp instanceof Container) {
	            findAndRefreshCombinationTable((Container) comp);
	        }
	    }
	}

	private void updateCombinationsList(DefaultListModel<String> listModel) {
		listModel.clear();
		for (String combination : analysisService.getAvailableCombinations()) {
			listModel.addElement(combination);
		}
	}
	
	private void showColumnManagerDialog() {
	    JDialog dialog = new JDialog(this, "Manage Ranking Columns", true);
	    dialog.setLayout(new BorderLayout());
	    dialog.setSize(600, 500);
	    dialog.setLocationRelativeTo(this);

	    // Available metrics list - SORTED (exclude already selected ones)
	    DefaultListModel<String> availableMetricsModel = new DefaultListModel<>();
	    List<String> currentColumns = loadAdditionalColumns();
	    
	    metricService.getAllMetricNames().stream()
	        .sorted()
	        .filter(metric -> !currentColumns.contains(metric)) // Exclude already selected
	        .forEach(availableMetricsModel::addElement);

	    JList<String> availableList = new JList<>(availableMetricsModel);
	    availableList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	    JScrollPane availableScroll = new JScrollPane(availableList);
	    availableScroll.setBorder(BorderFactory.createTitledBorder("Available Metrics (Sorted)"));

	    // Currently selected columns
	    DefaultListModel<String> selectedModel = new DefaultListModel<>();
	    currentColumns.forEach(selectedModel::addElement);

	    JList<String> selectedList = new JList<>(selectedModel);
	    selectedList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	    JScrollPane selectedScroll = new JScrollPane(selectedList);
	    selectedScroll.setBorder(BorderFactory.createTitledBorder("Additional Columns to Show"));

	    // Control buttons
	    JButton addButton = new JButton("Add →");
	    JButton removeButton = new JButton("← Remove");
	    JButton moveUpButton = new JButton("↑ Move Up");
	    JButton moveDownButton = new JButton("↓ Move Down");
	    JButton clearButton = new JButton("Clear All");

	    addButton.addActionListener(e -> {
	        List<String> selected = availableList.getSelectedValuesList();
	        for (String metric : selected) {
	            if (!selectedModel.contains(metric)) {
	                selectedModel.addElement(metric);
	                availableMetricsModel.removeElement(metric); // Remove from available
	            }
	        }
	    });

	    removeButton.addActionListener(e -> {
	        List<String> selected = selectedList.getSelectedValuesList();
	        for (String metric : selected) {
	            selectedModel.removeElement(metric);
	            if (!availableMetricsModel.contains(metric)) {
	                availableMetricsModel.addElement(metric); // Add back to available
	            }
	        }
	        // Sort available list after adding back
	        List<String> availableListData = new ArrayList<>();
	        for (int i = 0; i < availableMetricsModel.size(); i++) {
	            availableListData.add(availableMetricsModel.get(i));
	        }
	        availableMetricsModel.clear();
	        availableListData.stream().sorted().forEach(availableMetricsModel::addElement);
	    });

	    moveUpButton.addActionListener(e -> {
	        int selectedIndex = selectedList.getSelectedIndex();
	        if (selectedIndex > 0) {
	            String item = selectedModel.get(selectedIndex);
	            selectedModel.remove(selectedIndex);
	            selectedModel.add(selectedIndex - 1, item);
	            selectedList.setSelectedIndex(selectedIndex - 1);
	        }
	    });

	    moveDownButton.addActionListener(e -> {
	        int selectedIndex = selectedList.getSelectedIndex();
	        if (selectedIndex >= 0 && selectedIndex < selectedModel.size() - 1) {
	            String item = selectedModel.get(selectedIndex);
	            selectedModel.remove(selectedIndex);
	            selectedModel.add(selectedIndex + 1, item);
	            selectedList.setSelectedIndex(selectedIndex + 1);
	        }
	    });

	    clearButton.addActionListener(e -> {
	        // Move all selected items back to available
	        List<String> allSelected = new ArrayList<>();
	        for (int i = 0; i < selectedModel.size(); i++) {
	            allSelected.add(selectedModel.get(i));
	        }
	        
	        for (String metric : allSelected) {
	            if (!availableMetricsModel.contains(metric)) {
	                availableMetricsModel.addElement(metric);
	            }
	        }
	        selectedModel.clear();
	        // Sort available list
	        List<String> availableListData = new ArrayList<>();
	        for (int i = 0; i < availableMetricsModel.size(); i++) {
	            availableListData.add(availableMetricsModel.get(i));
	        }
	        availableMetricsModel.clear();
	        availableListData.stream().sorted().forEach(availableMetricsModel::addElement);
	    });

	    JPanel controlPanel = new JPanel(new GridLayout(5, 1, 5, 5));
	    controlPanel.add(addButton);
	    controlPanel.add(removeButton);
	    controlPanel.add(moveUpButton);
	    controlPanel.add(moveDownButton);
	    controlPanel.add(clearButton);

	    // Main action buttons
	    JPanel actionPanel = new JPanel(new FlowLayout());
	    JButton saveButton = new JButton("Save");
	    JButton cancelButton = new JButton("Cancel");
	    JButton defaultButton = new JButton("Reset to Defaults");

	    saveButton.addActionListener(e -> {
	        List<String> newColumns = new ArrayList<>();
	        for (int i = 0; i < selectedModel.size(); i++) {
	            newColumns.add(selectedModel.get(i));
	        }
	        saveAdditionalColumns(newColumns);
	        
	        // Close dialog first
	        dialog.dispose();
	        
	        // Then refresh the table
	        refreshRankingTable();
	        
	        JOptionPane.showMessageDialog(ZScoreRankingGUI.this, 
	            "Columns updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
	    });

	    cancelButton.addActionListener(e -> dialog.dispose());

	    defaultButton.addActionListener(e -> {
	        selectedModel.clear();
	        availableMetricsModel.clear();
	        
	        // Reset to defaults
	        List<String> defaultColumns = Arrays.asList("currentVolume", "percentChange", "averageVol");
	        defaultColumns.forEach(selectedModel::addElement);
	        
	        // Reload available metrics excluding defaults
	        metricService.getAllMetricNames().stream()
	            .sorted()
	            .filter(metric -> !defaultColumns.contains(metric))
	            .forEach(availableMetricsModel::addElement);
	    });

	    actionPanel.add(saveButton);
	    actionPanel.add(cancelButton);
	    actionPanel.add(defaultButton);

	    // Layout
	    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, availableScroll, selectedScroll);
	    splitPane.setDividerLocation(250);

	    JPanel mainPanel = new JPanel(new BorderLayout());
	    mainPanel.add(splitPane, BorderLayout.CENTER);
	    mainPanel.add(controlPanel, BorderLayout.EAST);

	    dialog.add(mainPanel, BorderLayout.CENTER);
	    dialog.add(actionPanel, BorderLayout.SOUTH);

	    dialog.setVisible(true);
	}
	
	private List<String> loadAdditionalColumns() {
	    // Load from properties (in real app, use Preferences API or file)
	    try {
	        String columnsStr = prefs.getProperty(COLUMN_PREFS_KEY, "currentVolume,percentChange,averageVol");
	        return new ArrayList<>(Arrays.asList(columnsStr.split(",")));
	    } catch (Exception e) {
	        return new ArrayList<>(Arrays.asList("currentVolume", "percentChange", "averageVol"));
	    }
	}

	private void saveAdditionalColumns(List<String> columns) {
	    try {
	        String columnsStr = String.join(",", columns);
	        prefs.setProperty(COLUMN_PREFS_KEY, columnsStr);
	        // In real application, save to file: prefs.store(new FileOutputStream("prefs.properties"), null);
	    } catch (Exception e) {
	        System.err.println("Error saving column preferences: " + e.getMessage());
	    }
	}

	private void showCombinationDetails(String combination) {
		if (combination == null)
			return;

		String details = getCombinationDetails(combination);
		JOptionPane.showMessageDialog(this, details, "Combination Details: " + combination,
				JOptionPane.INFORMATION_MESSAGE);
	}

	private void editCombination(String combination, DefaultTableModel tableModel) {
	    if (combination == null) return;
	    
	    ZScoreCombination combo = analysisService.getCombination(combination);
	    if (combo == null) return;
	    
	    JDialog dialog = new JDialog(this, "Edit Combination: " + combination, true);
	    dialog.setLayout(new BorderLayout());
	    dialog.setSize(800, 550);
	    
	    // Create table with current combination metrics
	    DefaultTableModel editModel = new DefaultTableModel(
	        new String[]{"Metric", "Weight %", "Comparison Type", "Lower Better"}, 0);
	    
	    for (Map.Entry<String, Double> entry : combo.getWeights().entrySet()) {
	        String metric = entry.getKey();
	        AnalysisMetric metricInfo = metricService.getMetric(metric);
	        editModel.addRow(new Object[]{
	            metric,
	            entry.getValue(),
	            metricInfo != null ? metricInfo.getComparisonType() : ComparisonType.HISTORICAL,
	            metricInfo != null ? metricInfo.isLowerIsBetter() : false
	        });
	    }
	    
	    JTable editTable = new JTable(editModel);
	    editTable.getColumnModel().getColumn(2).setCellEditor(
	        new DefaultCellEditor(new JComboBox<>(ComparisonType.values())));
	    editTable.getColumnModel().getColumn(3).setCellEditor(
	        new DefaultCellEditor(new JComboBox<>(new Boolean[]{true, false})));
	    
	    JScrollPane editScroll = new JScrollPane(editTable);
	    editScroll.setBorder(BorderFactory.createTitledBorder("Edit Metrics and Settings"));
	    
	    // Available metrics for adding new ones
	    DefaultListModel<String> availableMetricsModel = new DefaultListModel<>();
	    metricService.getAllMetricNames().stream()
	        .sorted()
	        .filter(metric -> !combo.getWeights().containsKey(metric)) // Only show metrics not already in combination
	        .forEach(availableMetricsModel::addElement);
	    
	    JList<String> availableList = new JList<>(availableMetricsModel);
	    JScrollPane availableScroll = new JScrollPane(availableList);
	    availableScroll.setBorder(BorderFactory.createTitledBorder("Available Metrics to Add"));
	    
	    // Control buttons for editing
	    JPanel editControlPanel = new JPanel(new FlowLayout());
	    JButton addButton = new JButton("Add Metric →");
	    JButton removeButton = new JButton("← Remove Selected");
	    JButton autoWeightButton = new JButton("Auto Weight");
	    
	    addButton.addActionListener(e -> {
	        String selected = availableList.getSelectedValue();
	        if (selected != null) {
	            AnalysisMetric metricInfo = metricService.getMetric(selected);
	            editModel.addRow(new Object[]{
	                selected, 
	                0.0,
	                metricInfo != null ? metricInfo.getComparisonType() : ComparisonType.HISTORICAL,
	                metricInfo != null ? metricInfo.isLowerIsBetter() : false
	            });
	            autoWeightButton.doClick(); // Recalculate weights
	            availableMetricsModel.removeElement(selected); // Remove from available list
	        }
	    });
	    
	    removeButton.addActionListener(e -> {
	        int selectedRow = editTable.getSelectedRow();
	        if (selectedRow != -1) {
	            String removedMetric = (String) editModel.getValueAt(selectedRow, 0);
	            editModel.removeRow(selectedRow);
	            availableMetricsModel.addElement(removedMetric); // Add back to available list
	            autoWeightButton.doClick(); // Recalculate weights
	        }
	    });
	    
	    autoWeightButton.addActionListener(e -> {
	        int rowCount = editModel.getRowCount();
	        if (rowCount > 0) {
	            double equalWeight = 100.0 / rowCount;
	            for (int i = 0; i < rowCount; i++) {
	                editModel.setValueAt(equalWeight, i, 1);
	            }
	        }
	    });
	    
	    editControlPanel.add(addButton);
	    editControlPanel.add(removeButton);
	    editControlPanel.add(autoWeightButton);
	    
	    // Main control buttons
	    JPanel controlPanel = new JPanel(new FlowLayout());
	    JButton saveButton = new JButton("Save Changes");
	    JButton cancelButton = new JButton("Cancel");
	    
	    saveButton.addActionListener(e -> {
	        Map<String, Double> newWeights = new HashMap<>();
	        Map<String, ComparisonType> comparisonTypes = new HashMap<>();
	        Map<String, Boolean> lowerBetterSettings = new HashMap<>();
	        double totalWeight = 0;
	        
	        for (int i = 0; i < editModel.getRowCount(); i++) {
	            String metric = (String) editModel.getValueAt(i, 0);
	            Object weightObj = editModel.getValueAt(i, 1);
	            
	            Double weight = 0.0;
	            if(weightObj instanceof Number) {
	            	weight = (Double) weightObj;
	            }else if(weightObj instanceof String) {
	            	weight = (Double.valueOf((String) weightObj));
	            }
	            ComparisonType compType = (ComparisonType) editModel.getValueAt(i, 2);
	            Boolean lowerBetter = (Boolean) editModel.getValueAt(i, 3);
	            
	            if (weight != null && weight > 0) {
	                newWeights.put(metric, weight);
	                comparisonTypes.put(metric, compType);
	                lowerBetterSettings.put(metric, lowerBetter != null ? lowerBetter : false);
	                totalWeight += weight;
	            }
	        }
	        
	        if (newWeights.isEmpty()) {
	            JOptionPane.showMessageDialog(dialog, "Combination must have at least one metric", "Error", JOptionPane.ERROR_MESSAGE);
	            return;
	        }
	        
	        // Normalize weights
	        if (Math.abs(totalWeight - 100.0) > 0.01) {
	            final double finalTotalWeight = totalWeight;
	            newWeights.replaceAll((k, v) -> v * 100.0 / finalTotalWeight);
	        }
	        
	        // Update comparison settings
	        for (String metric : comparisonTypes.keySet()) {
	            metricService.updateMetricSettings(metric, comparisonTypes.get(metric), lowerBetterSettings.get(metric));
	        }
	        
	        // Update the combination
	        analysisService.updateCombinationWeights(combination, newWeights);
	        updateCombinationComboBox();
	        if (tableModel != null) {
	            updateCombinationsTable(tableModel); // Use table update method
	        }
	        
	        dialog.dispose();
	        JOptionPane.showMessageDialog(this, "Combination updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
	    });
	    
	    cancelButton.addActionListener(e -> dialog.dispose());
	    
	    controlPanel.add(saveButton);
	    controlPanel.add(cancelButton);
	    
	    // Layout
	    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, availableScroll, editScroll);
	    splitPane.setDividerLocation(250);
	    
	    JPanel mainPanel = new JPanel(new BorderLayout());
	    mainPanel.add(splitPane, BorderLayout.CENTER);
	    mainPanel.add(editControlPanel, BorderLayout.SOUTH);
	    
	    dialog.add(mainPanel, BorderLayout.CENTER);
	    dialog.add(controlPanel, BorderLayout.SOUTH);
	    
	    dialog.setVisible(true);
	}

	private void showAddCombinationDialog(DefaultTableModel tableModel) {
	    JDialog dialog = new JDialog(this, "Create New Combination", true);
	    dialog.setLayout(new BorderLayout());
	    dialog.setSize(800, 550);
	    
	    // Available metrics panel - SORTED BY NAME
	    DefaultListModel<String> availableMetricsModel = new DefaultListModel<>();
	    metricService.getAllMetricNames().stream()
	        .sorted()
	        .forEach(availableMetricsModel::addElement);
	    
	    JList<String> availableList = new JList<>(availableMetricsModel);
	    JScrollPane availableScroll = new JScrollPane(availableList);
	    availableScroll.setBorder(BorderFactory.createTitledBorder("Available Metrics (Sorted)"));
	    
	    // Selected metrics with weights and settings panel
	    DefaultTableModel selectedModel = new DefaultTableModel(
	        new String[]{"Metric", "Weight %", "Comparison Type", "Lower Better"}, 0) {
	        @Override
	        public Class<?> getColumnClass(int columnIndex) {
	            if (columnIndex == 1) return Double.class;
	            if (columnIndex == 2) return ComparisonType.class;
	            if (columnIndex == 3) return Boolean.class;
	            return String.class;
	        }
	    };
	    
	    JTable selectedTable = new JTable(selectedModel);
	    selectedTable.getColumnModel().getColumn(2).setCellEditor(
	        new DefaultCellEditor(new JComboBox<>(ComparisonType.values())));
	    selectedTable.getColumnModel().getColumn(3).setCellEditor(
	        new DefaultCellEditor(new JComboBox<>(new Boolean[]{true, false})));
	    
	    selectedTable.setRowHeight(25);
	    JScrollPane selectedScroll = new JScrollPane(selectedTable);
	    selectedScroll.setBorder(BorderFactory.createTitledBorder("Selected Metrics with Settings"));
	    
	    // Control buttons
	    JPanel controlPanel = new JPanel(new FlowLayout());
	    JButton addButton = new JButton("Add →");
	    JButton removeButton = new JButton("← Remove");
	    JButton clearButton = new JButton("Clear All");
	    JButton autoWeightButton = new JButton("Auto Weight");
	    
	    addButton.addActionListener(e -> {
	        String selected = availableList.getSelectedValue();
	        if (selected != null) {
	            AnalysisMetric metricInfo = metricService.getMetric(selected);
	            selectedModel.addRow(new Object[]{
	                selected, 
	                100.0,
	                metricInfo != null ? metricInfo.getComparisonType() : ComparisonType.HISTORICAL,
	                metricInfo != null ? metricInfo.isLowerIsBetter() : false
	            });
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
	    
	    autoWeightButton.addActionListener(e -> {
	        updateWeights(selectedModel);
	    });
	    
	    controlPanel.add(addButton);
	    controlPanel.add(removeButton);
	    controlPanel.add(clearButton);
	    controlPanel.add(autoWeightButton);
	    
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
	        
	        // Check if combination name already exists
	        if (analysisService.getAvailableCombinations().contains(name)) {
	            JOptionPane.showMessageDialog(dialog, "A combination with this name already exists. Please choose a different name.", "Error", JOptionPane.ERROR_MESSAGE);
	            return;
	        }
	        
	        try {
	            Map<String, Double> weights = new HashMap<>();
	            Map<String, ComparisonType> comparisonTypes = new HashMap<>();
	            Map<String, Boolean> lowerBetterSettings = new HashMap<>();
	            double totalWeight = 0;
	            
	            for (int i = 0; i < selectedModel.getRowCount(); i++) {
	                String metric = (String) selectedModel.getValueAt(i, 0);
	                Double weight = (Double) selectedModel.getValueAt(i, 1);
	                ComparisonType compType = (ComparisonType) selectedModel.getValueAt(i, 2);
	                Boolean lowerBetter = (Boolean) selectedModel.getValueAt(i, 3);
	                
	                if (weight == null || weight <= 0) {
	                    JOptionPane.showMessageDialog(dialog, "All weights must be positive numbers", "Error", JOptionPane.ERROR_MESSAGE);
	                    return;
	                }
	                
	                weights.put(metric, weight);
	                comparisonTypes.put(metric, compType);
	                lowerBetterSettings.put(metric, lowerBetter != null ? lowerBetter : false);
	                totalWeight += weight;
	            }
	            
	            // Normalize weights to sum to 100%
	            if (Math.abs(totalWeight - 100.0) > 0.01) {
	                final double finalTotalWeight = totalWeight;
	                weights.replaceAll((k, v) -> v * 100.0 / finalTotalWeight);
	            }
	            
	            // Update comparison settings for selected metrics
	            for (String metric : comparisonTypes.keySet()) {
	                metricService.updateMetricSettings(metric, comparisonTypes.get(metric), lowerBetterSettings.get(metric));
	            }
	            
	            analysisService.addCombination(new ZScoreCombination(name, weights));
	            updateCombinationComboBox();
	            if (tableModel != null) {
	                updateCombinationsTable(tableModel);
	            }
	            dialog.dispose();
	            
	            JOptionPane.showMessageDialog(this, 
	                "Combination '" + name + "' created successfully with " + weights.size() + " metrics!", 
	                "Success", JOptionPane.INFORMATION_MESSAGE);
	        } catch (Exception ex) {
	            JOptionPane.showMessageDialog(dialog, "Error creating combination: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
	        }
	    });
	    
	    cancelButton.addActionListener(e -> dialog.dispose());
	    
	    JPanel buttonPanel = new JPanel();
	    buttonPanel.add(createButton);
	    buttonPanel.add(cancelButton);
	    
	    // Layout
	    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, availableScroll, selectedScroll);
	    splitPane.setDividerLocation(300);
	    
	    JPanel mainPanel = new JPanel(new BorderLayout());
	    mainPanel.add(splitPane, BorderLayout.CENTER);
	    mainPanel.add(controlPanel, BorderLayout.SOUTH);
	    
	    dialog.add(namePanel, BorderLayout.NORTH);
	    dialog.add(mainPanel, BorderLayout.CENTER);
	    dialog.add(buttonPanel, BorderLayout.SOUTH);
	    
	    dialog.setVisible(true);
	}
	
	private void updateWeights(DefaultTableModel model) {
		int rowCount = model.getRowCount();
		if (rowCount == 0)
			return;

		double equalWeight = 100.0 / rowCount;
		for (int i = 0; i < rowCount; i++) {
			model.setValueAt(equalWeight, i, 1);
		}
	}

	private String getMetricDetails(String metricFullName) {
		try {
			StringBuilder details = new StringBuilder();
			details.append("Metric: ").append(metricFullName).append("\n");

			String[] parts = metricFullName.split("_", 2);
			if (parts.length > 1) {
				details.append("Timeframe: ").append(parts[0]).append("\n");
				details.append("Indicator: ").append(parts[1]).append("\n");
			} else {
				details.append("Price Data Metric\n");
			}

			details.append("Current Mean: ")
					.append(String.format("%.4f", metricService.getHistoricalMean(metricFullName))).append("\n");
			details.append("Current Std Dev: ")
					.append(String.format("%.4f", metricService.getHistoricalStdDev(metricFullName))).append("\n");

			return details.toString();
		} catch (Exception e) {
			return "Error getting details for metric: " + metricFullName;
		}
	}

	private String getCombinationDetails(String combinationName) {
		com.quantlabs.stockApp.analysis.model.ZScoreCombination combination = analysisService
				.getCombination(combinationName);
		if (combination == null)
			return "Combination not found: " + combinationName;

		StringBuilder details = new StringBuilder();
		details.append("Combination: ").append(combinationName).append("\n\n");
		details.append("Metrics and Weights:\n");

		double totalWeight = 0;
		for (Map.Entry<String, Double> entry : combination.getWeights().entrySet()) {
			details.append(String.format("  • %-20s: %6.1f%%\n", entry.getKey(), entry.getValue()));
			totalWeight += entry.getValue();
		}

		details.append("\nTotal Weight: ").append(String.format("%.1f%%", totalWeight));
		return details.toString();
	}

	private String getZScoreInterpretation(double zScore) {
		if (zScore > 3.0)
			return "EXTREMELY BULLISH";
		if (zScore > 2.0)
			return "VERY BULLISH";
		if (zScore > 1.0)
			return "BULLISH";
		if (zScore > 0.5)
			return "SLIGHTLY BULLISH";
		if (zScore > -0.5)
			return "NEUTRAL";
		if (zScore > -1.0)
			return "SLIGHTLY BEARISH";
		if (zScore > -2.0)
			return "BEARISH";
		if (zScore > -3.0)
			return "VERY BEARISH";
		return "EXTREMELY BEARISH";
	}
	
	private void removeCombination(String combination, DefaultTableModel tableModel) {
	    if (combination == null) {
	        JOptionPane.showMessageDialog(this, 
	            "Please select a combination to remove first.", 
	            "Error", JOptionPane.ERROR_MESSAGE);
	        return;
	    }
	    
	    // Get combination details for confirmation message
	    ZScoreCombination combo = analysisService.getCombination(combination);
	    String metricsInfo = "";
	    if (combo != null) {
	        metricsInfo = "\nContains " + combo.getWeights().size() + " metrics: " +
	                     String.join(", ", combo.getWeights().keySet());
	    }
	    
	    int confirm = JOptionPane.showConfirmDialog(this,
	        "Are you sure you want to remove the combination:\n\n" +
	        "Name: " + combination + 
	        metricsInfo +
	        "\n\nThis action cannot be undone.",
	        "Confirm Combination Removal",
	        JOptionPane.YES_NO_OPTION,
	        JOptionPane.WARNING_MESSAGE);
	    
	    if (confirm == JOptionPane.YES_OPTION) {
	        try {
	            // Remove the combination from the service
	            analysisService.removeCombination(combination);
	            
	            // Update the UI components
	            updateCombinationComboBox();
	            
	            if (tableModel != null) {
	                updateCombinationsTable(tableModel);
	            }
	            
	            JOptionPane.showMessageDialog(this, 
	                "Combination '" + combination + "' has been successfully removed.", 
	                "Removal Successful", JOptionPane.INFORMATION_MESSAGE);
	            
	        } catch (Exception e) {
	            JOptionPane.showMessageDialog(this, 
	                "Error removing combination: " + e.getMessage(), 
	                "Removal Error", JOptionPane.ERROR_MESSAGE);
	        }
	    }
	}
	
	public JSONObject exportState() {
	    JSONObject state = new JSONObject();
	    
	    // Export additional columns
	    List<String> additionalColumns = loadAdditionalColumns();
	    state.put("additionalColumns", new JSONArray(additionalColumns));
	    
	    // Export combinations
	    JSONObject combinationsJson = new JSONObject();
	    Map<String, ZScoreCombination> allCombinations = analysisService.getAllCombinations();
	    
	    for (String comboName : allCombinations.keySet()) {
	        ZScoreCombination combo = allCombinations.get(comboName);
	        JSONObject comboJson = new JSONObject();
	        
	        // Store weights
	        JSONObject weightsJson = new JSONObject();
	        for (Map.Entry<String, Double> entry : combo.getWeights().entrySet()) {
	            weightsJson.put(entry.getKey(), entry.getValue());
	        }
	        comboJson.put("weights", weightsJson);
	        
	        combinationsJson.put(comboName, comboJson);
	    }
	    state.put("combinations", combinationsJson);
	    
	    return state;
	}

	public void importState(JSONObject state) {
	    if (state.has("additionalColumns")) {
	        JSONArray columnsArray = state.getJSONArray("additionalColumns");
	        List<String> columns = new ArrayList<>();
	        for (int i = 0; i < columnsArray.length(); i++) {
	            columns.add(columnsArray.getString(i));
	        }
	        saveAdditionalColumns(columns);
	    }
	    
	    if (state.has("combinations")) {
	        JSONObject combinationsJson = state.getJSONObject("combinations");
	        Map<String, ZScoreCombination> newCombinations = new HashMap<>();
	        
	        for (String comboName : combinationsJson.keySet()) {
	            JSONObject comboJson = combinationsJson.getJSONObject(comboName);
	            JSONObject weightsJson = comboJson.getJSONObject("weights");
	            
	            Map<String, Double> weights = new HashMap<>();
	            for (String metric : weightsJson.keySet()) {
	                weights.put(metric, weightsJson.getDouble(metric));
	            }
	            
	            newCombinations.put(comboName, new ZScoreCombination(comboName, weights));
	        }
	        
	        analysisService.setAllCombinations(newCombinations);
	    }
	    
	    // Refresh UI components
	    refreshRankingTable();
	    updateCombinationComboBox();
	    //updateCombinationsTable();
	    //refreshCombinationManagerTable();
	    
	    if (combinationsTable != null) {
	        DefaultTableModel model = (DefaultTableModel) combinationsTable.getModel();
	        updateCombinationsTable(model);
	    }
	}
	
	public void saveConfiguration() {
	    try {
	        JSONObject config = new JSONObject();
	        
	        // Save combinations
	        JSONObject combinationsJson = new JSONObject();
	        Map<String, ZScoreCombination> allCombinations = analysisService.getAllCombinations();
	        for (Map.Entry<String, ZScoreCombination> entry : allCombinations.entrySet()) {
	            JSONObject comboData = new JSONObject();
	            JSONObject weightsJson = new JSONObject(entry.getValue().getWeights());
	            comboData.put("weights", weightsJson);
	            combinationsJson.put(entry.getKey(), comboData);
	        }
	        config.put("combinations", combinationsJson);
	        
	        // Save additional columns
	        
	        String columnsArray = prefs.getProperty(COLUMN_PREFS_KEY) != null ? "[" +prefs.getProperty(COLUMN_PREFS_KEY)+ "]" : "[]"; 
	        JSONArray columnsJson = new JSONArray(columnsArray);
	        config.put("additionalColumns", columnsJson);
	        
	        // Save to Preferences
	        Preferences prefs = Preferences.userNodeForPackage(ZScoreRankingGUI.class);
	        prefs.put(ZSCORE_PREFS_KEY, config.toString());
	    } catch (Exception e) {
	        System.err.println("Error saving Z-Score configuration: " + e.getMessage());
	    }
	}

	public void loadConfiguration() {
		try {
	        Preferences prefs = Preferences.userNodeForPackage(ZScoreRankingGUI.class);
	        String configStr = prefs.get(ZSCORE_PREFS_KEY, null);
	        
	        if (configStr != null) {
	            JSONObject config = new JSONObject(configStr);
	            importState(config);
	        } else {
	            // Ensure defaults exist even if no saved config
	            createDefaultConfiguration();
	        }
	        
	        // Refresh UI components after loading
	        refreshRankingTable();
	        updateCombinationComboBox();
	        updateCombinationsTable();
	        
	    } catch (Exception e) {
	        System.err.println("Error loading configuration, using defaults: " + e.getMessage());
	        createDefaultConfiguration();
	        updateCombinationsTable();
	    }
	}
	
	private void createDefaultConfiguration() {
	    // Default additional columns
	    List<String> defaultColumns = Arrays.asList("currentVolume", "percentChange", "averageVol");
	    saveAdditionalColumns(defaultColumns);
	    
	    // Create default combinations if none exist
	    if (analysisService.getAvailableCombinations().isEmpty()) {
	        // Add your default combinations here
	        Map<String, Double> defaultWeights = new HashMap<>();
	        defaultWeights.put("price", 40.0);
	        defaultWeights.put("currentVolume", 30.0);
	        defaultWeights.put("percentChange", 30.0);
	        
	        analysisService.addCombination(new ZScoreCombination("Default", defaultWeights));
	    }
	    
	    // Refresh UI
	    refreshRankingTable();
	    updateCombinationComboBox();
	    
	    // Save the default configuration
	    saveConfiguration();
	}
	
	
	
	public ZScoreAnalysisService getAnalysisService() {
		return analysisService;
	}

	public void setAnalysisService(ZScoreAnalysisService analysisService) {
		this.analysisService = analysisService;
	}



	// Custom renderers
	class NumericCellRenderer extends DefaultTableCellRenderer {
	    @Override
	    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, 
	            boolean hasFocus, int row, int column) {
	        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	        
	        // Format numeric values
	        if (value instanceof Number) {
	            Number num = (Number) value;
	            if (Math.abs(num.doubleValue()) >= 1000) {
	                setText(String.format("%,.0f", num.doubleValue()));
	            } else if (Math.abs(num.doubleValue()) >= 1) {
	                setText(String.format("%,.2f", num.doubleValue()));
	            } else {
	                setText(String.format("%,.4f", num.doubleValue()));
	            }
	            
	            // Color coding based on value (green for positive, red for negative)
	            if (num.doubleValue() > 0) {
	                c.setForeground(new Color(0, 100, 0)); // Dark green
	            } else if (num.doubleValue() < 0) {
	                c.setForeground(new Color(139, 0, 0)); // Dark red
	            } else {
	                c.setForeground(Color.BLACK);
	            }
	        }
	        
	        return c;
	    }
	}
	
	class PriceDataListRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
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
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
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
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
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

	public Map<String, Set<String>> getCustomIndicatorCombinations() {
		return customIndicatorCombinations;
	}

	public void setCustomIndicatorCombinations(Map<String, Set<String>> customIndicatorCombinations) {
		this.customIndicatorCombinations = customIndicatorCombinations;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				e.printStackTrace();
			}

			ZScoreRankingGUI gui = new ZScoreRankingGUI();
			gui.setVisible(true);
		});
	}
}