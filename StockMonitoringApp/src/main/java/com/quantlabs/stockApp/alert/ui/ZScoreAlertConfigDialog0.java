package com.quantlabs.stockApp.alert.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.json.JSONObject;

import com.quantlabs.stockApp.IStockDashboard;
import com.quantlabs.stockApp.StockDashboardv1_22_5;
import com.quantlabs.stockApp.alert.ZScoreAlertManager;
import com.quantlabs.stockApp.alert.model.ZScoreAlertConfig;
import com.quantlabs.stockApp.reports.MonteCarloGraphApp;
import com.quantlabs.stockApp.utils.ZScoreColumnHelper;

public class ZScoreAlertConfigDialog0 extends JDialog {
	private JCheckBox enableZScoreAlertCheckbox;
	private DefaultTableModel configTableModel;
	private JTable configTable;
	private Map<String, ZScoreAlertConfig> alertConfigs;
	private Map<String, MonteCarloGraphApp> monteCarloApps;
	private boolean saved = false;
	private IStockDashboard parentDashboard;
	private ZScoreColumnHelper zScoreColumnHelper;

	public ZScoreAlertConfigDialog0(IStockDashboard parent) {
		//super(parent, "Z-Score Alert Configuration", true);
		
		this.setTitle("Z-Score Alert Configuration");
		this.setModal(true);
		
		this.parentDashboard = parent;
		this.zScoreColumnHelper = parent.getZScoreColumnHelper();
		this.alertConfigs = new HashMap<>();
		this.monteCarloApps = new HashMap<>();
		initializeUI();
		loadExistingConfig();
	}

	private void initializeUI() {
		setLayout(new BorderLayout());
		setSize(800, 500); // Increased width for new column
		setLocationRelativeTo(getParent());

		// Main panel
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Enable Z-Score Alert panel
		JPanel enablePanel = new JPanel(new BorderLayout());
		enablePanel.setBorder(BorderFactory.createTitledBorder("Monitor Top Z-Score"));
		enableZScoreAlertCheckbox = new JCheckBox("Enable Z-Score Alert");
		enablePanel.add(enableZScoreAlertCheckbox, BorderLayout.WEST);

		// Add listener to enable/disable table
		enableZScoreAlertCheckbox.addActionListener(e -> {
			boolean enabled = enableZScoreAlertCheckbox.isSelected();
			configTable.setEnabled(enabled);
			configTable.getTableHeader().setEnabled(enabled);
			updateTableEditableState();
		});

		// Configuration table with new "is Alarm On" column
		String[] columnNames = { "Enable", "Z-Score Column", "No of Rank/Zscore", "MonteCarlo Enabled", "is Alarm On" };
		configTableModel = new DefaultTableModel(columnNames, 0) {
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				switch (columnIndex) {
				case 0:
					return Boolean.class;
				case 1:
					return String.class;
				case 2:
					return String.class;
				case 3:
					return Boolean.class;
				case 4:
					return Boolean.class;
				default:
					return Object.class;
				}
			}
		};

		configTable = new JTable(configTableModel);
		configTable.setRowHeight(25);
		configTable.getColumnModel().getColumn(0).setPreferredWidth(60);
		configTable.getColumnModel().getColumn(1).setPreferredWidth(200);
		configTable.getColumnModel().getColumn(2).setPreferredWidth(120);
		configTable.getColumnModel().getColumn(3).setPreferredWidth(120);
		configTable.getColumnModel().getColumn(4).setPreferredWidth(80);

		// Set custom renderer and editor for the table
		configTable.setDefaultRenderer(Object.class, new ZScoreAlertCellRenderer());

		JScrollPane tableScroll = new JScrollPane(configTable);
		tableScroll.setBorder(BorderFactory.createTitledBorder("Z-Score Column Configuration"));

		// Button panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton saveButton = new JButton("Save");
		JButton cancelButton = new JButton("Cancel");
		JButton refreshButton = new JButton("Refresh Columns");
		JButton openAllMonteCarloButton = new JButton("Open All Monte Carlo Windows");

		refreshButton.addActionListener(e -> refreshZScoreColumns());
		saveButton.addActionListener(e -> saveConfiguration());
		cancelButton.addActionListener(e -> dispose());
		openAllMonteCarloButton.addActionListener(e -> openAllMonteCarloWindows());

		buttonPanel.add(refreshButton);
		buttonPanel.add(openAllMonteCarloButton);
		buttonPanel.add(saveButton);
		buttonPanel.add(cancelButton);

		// Layout
		mainPanel.add(enablePanel, BorderLayout.NORTH);
		mainPanel.add(tableScroll, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		add(mainPanel);
	}

	private void updateTableEditableState() {
		boolean enabled = enableZScoreAlertCheckbox.isSelected();
		configTableModel = new DefaultTableModel(
				new String[] { "Enable", "Z-Score Column", "No of Rank/Zscore", "MonteCarlo Enabled", "is Alarm On" },
				0) {
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				switch (columnIndex) {
				case 0:
					return Boolean.class;
				case 1:
					return String.class;
				case 2:
					return String.class;
				case 3:
					return Boolean.class;
				case 4:
					return Boolean.class;
				default:
					return Object.class;
				}
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				return enabled && column != 1; // Only enable editing if alert is enabled, and don't allow editing
												// column names
			}
		};

		// Repopulate the table with new model
		populateTable();
		configTable.setModel(configTableModel);
	}

	private void refreshZScoreColumns() {
		populateTable();
		parentDashboard.logToConsole("Refreshed Z-Score columns for alert configuration");
	}

	private void loadExistingConfig() {
		try {
			// Try to load from the main dashboard's ZScoreAlertManager first
			if (parentDashboard.getZScoreAlertManager() != null) {
				ZScoreAlertManager manager = parentDashboard.getZScoreAlertManager();

				// Set the main enabled state
				enableZScoreAlertCheckbox.setSelected(manager.isEnabled());

				// Load individual configs from the manager
				Map<String, ZScoreAlertConfig> managerConfigs = manager.getAlertConfigs();
				if (managerConfigs != null && !managerConfigs.isEmpty()) {
					alertConfigs.clear();
					alertConfigs.putAll(managerConfigs);

					/*parentDashboard.logToConsole("DEBUG: Loaded Z-Score config from main dashboard manager");
					parentDashboard.logToConsole("DEBUG: Loaded " + alertConfigs.size() + " configurations");

					// Debug: Log what we loaded
					for (Map.Entry<String, ZScoreAlertConfig> entry : alertConfigs.entrySet()) {
						ZScoreAlertConfig config = entry.getValue();
						parentDashboard.logToConsole("DEBUG: From manager - " + entry.getKey() + ": enabled="
								+ config.isEnabled() + ", range=" + config.getMonitorRange() + ", monteCarlo="
								+ config.isMonteCarloEnabled() + ", alarmOn=" + config.isAlarmOn());
					}*/
				} else {
					// Fallback to preferences if no config in manager
					loadFromPreferences();
				}
			} else {
				// Fallback to preferences if no manager available
				loadFromPreferences();
			}
		} catch (Exception e) {
			parentDashboard.logToConsole("Error loading from main dashboard: " + e.getMessage());
			// Fallback to preferences
			loadFromPreferences();
		}

		populateTable();
		updateTableEditableState();
	}

	// Helper method to load from preferences (fallback)
	private void loadFromPreferences() {
		Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_5.class);
		boolean enabled = prefs.getBoolean("zscoreAlertEnabled", false);
		enableZScoreAlertCheckbox.setSelected(enabled);

		alertConfigs.clear();

		String configJson = prefs.get("zscoreAlertConfigs", "{}");
		//parentDashboard.logToConsole("DEBUG: Falling back to preferences: " + configJson);

		try {
			if (!configJson.equals("{}")) {
				JSONObject configObj = new JSONObject(configJson);
				for (String key : configObj.keySet()) {
					JSONObject config = configObj.getJSONObject(key);
					ZScoreAlertConfig alertConfig = new ZScoreAlertConfig(config.getBoolean("enabled"),
							config.getInt("monitorRange"));
					alertConfig.setMonteCarloEnabled(config.optBoolean("monteCarloEnabled", false));
					alertConfig.setAlarmOn(config.optBoolean("alarmOn", false));
					alertConfigs.put(key, alertConfig);
				}
			}
		} catch (Exception e) {
			parentDashboard.logToConsole("Error loading from preferences: " + e.getMessage());
		}
	}

	private void populateTable() {
		configTableModel.setRowCount(0);

		if (parentDashboard.getTableModel() == null) {
			parentDashboard.logToConsole("Table model not available for Z-Score alert configuration");
			return;
		}

		//parentDashboard.logToConsole("=== DEBUG: START populateTable ===");
		//parentDashboard.logToConsole("DEBUG: Looking for Z-Score columns in table...");

		// Get all Z-Score columns from the table model
		for (int i = 0; i < parentDashboard.getTableModel().getColumnCount(); i++) {
			String colName = parentDashboard.getTableModel().getColumnName(i);
			//parentDashboard.logToConsole("DEBUG: Checking column: " + colName);

			if (zScoreColumnHelper.isZScoreColumn(colName) && zScoreColumnHelper.isRankColumn(colName)) {
				//parentDashboard.logToConsole("DEBUG: Found Z-Score column: " + colName);

				// Check if we have a config for this column
				boolean hasConfig = alertConfigs.containsKey(colName);
				//parentDashboard.logToConsole("DEBUG: alertConfigs contains '" + colName + "': " + hasConfig);

				ZScoreAlertConfig config;
				if (hasConfig) {
					config = alertConfigs.get(colName);
					//parentDashboard.logToConsole("DEBUG: Retrieved config from alertConfigs");
					//parentDashboard.logToConsole("DEBUG: Config values - enabled: " + config.isEnabled()
					//		+ ", monteCarlo: " + config.isMonteCarloEnabled() + ", alarmOn: " + config.isAlarmOn());
				} else {
					config = new ZScoreAlertConfig(false, 5);
					//parentDashboard.logToConsole("DEBUG: Created new default config");
				}

				// Get values directly to avoid any method call issues
				boolean enabledValue = config.isEnabled();
				int rangeValue = config.getMonitorRange();
				boolean monteCarloValue = config.isMonteCarloEnabled();
				boolean alarmOnValue = config.isAlarmOn();

				//parentDashboard.logToConsole("DEBUG: Final values for table - enabled: " + enabledValue + ", range: "
				//		+ rangeValue + ", monteCarlo: " + monteCarloValue + ", alarmOn: " + alarmOnValue);

				// Add to table
				Object[] rowData = { enabledValue, colName, String.valueOf(rangeValue), monteCarloValue, alarmOnValue };

				configTableModel.addRow(rowData);
				//parentDashboard.logToConsole("DEBUG: Added row to table model");
			}
		}

		//parentDashboard.logToConsole("DEBUG: Total rows in table: " + configTableModel.getRowCount());
		//parentDashboard.logToConsole("=== DEBUG: END populateTable ===");

		// Debug table values
		//debugTableValues();
	}

	private void debugTableValues() {
		parentDashboard.logToConsole("DEBUG: Current table values:");
		for (int i = 0; i < configTableModel.getRowCount(); i++) {
			String colName = (String) configTableModel.getValueAt(i, 1);
			Boolean enabled = (Boolean) configTableModel.getValueAt(i, 0);
			String rangeStr = (String) configTableModel.getValueAt(i, 2);
			Boolean monteCarloEnabled = (Boolean) configTableModel.getValueAt(i, 3);
			Boolean alarmOn = (Boolean) configTableModel.getValueAt(i, 4);

			parentDashboard.logToConsole("DEBUG: Row " + i + " - " + colName + ": enabled=" + enabled + ", range="
					+ rangeStr + ", monteCarlo=" + monteCarloEnabled + ", alarmOn=" + alarmOn);
		}
	}

	private void saveConfiguration() {
		try {
			// Save to preferences for backward compatibility
			Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_5.class);
			prefs.putBoolean("zscoreAlertEnabled", enableZScoreAlertCheckbox.isSelected());

			JSONObject configObj = new JSONObject();
			alertConfigs.clear();

			for (int i = 0; i < configTableModel.getRowCount(); i++) {
				String colName = (String) configTableModel.getValueAt(i, 1);
				Boolean enabled = (Boolean) configTableModel.getValueAt(i, 0);
				String rangeStr = (String) configTableModel.getValueAt(i, 2);
				Boolean monteCarloEnabled = (Boolean) configTableModel.getValueAt(i, 3);
				Boolean alarmOn = (Boolean) configTableModel.getValueAt(i, 4);

				if (!colName.equals("No Z-Score columns available")) {
					try {
						int range = Integer.parseInt(rangeStr);
						ZScoreAlertConfig config = new ZScoreAlertConfig(enabled, range);
						config.setMonteCarloEnabled(monteCarloEnabled);
						config.setAlarmOn(alarmOn);
						alertConfigs.put(colName, config);

						JSONObject configJson = new JSONObject();
						configJson.put("enabled", enabled);
						configJson.put("monitorRange", range);
						configJson.put("monteCarloEnabled", monteCarloEnabled);
						configJson.put("alarmOn", alarmOn);
						configObj.put(colName, configJson);

						handleMonteCarloWindowState(colName, monteCarloEnabled, range);

					} catch (NumberFormatException e) {
						parentDashboard.logToConsole("Invalid range for " + colName + ": " + rangeStr);
					}
				}
			}

			prefs.put("zscoreAlertConfigs", configObj.toString());

			// CRITICAL: Update the main dashboard's ZScoreAlertManager
			if (parentDashboard.getZScoreAlertManager() != null) {
				parentDashboard.getZScoreAlertManager().setEnabled(enableZScoreAlertCheckbox.isSelected());
				parentDashboard.getZScoreAlertManager().setAlertConfigs(alertConfigs);
				//parentDashboard.logToConsole("DEBUG: Updated main dashboard ZScoreAlertManager with " + alertConfigs.size() + " configs");
			}

			saved = true;
			parentDashboard.logToConsole("Z-Score alert configuration saved successfully");
			dispose();

		} catch (Exception e) {
			parentDashboard.logToConsole("Error saving Z-Score alert configuration: " + e.getMessage());
			JOptionPane.showMessageDialog(this, "Error saving configuration: " + e.getMessage(), "Save Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void handleMonteCarloWindowState(String columnName, boolean monteCarloEnabled, int monitorRange) {
		if (monteCarloEnabled) {
			// Open or update Monte Carlo window
			openOrUpdateMonteCarloWindow(columnName, monitorRange);
		} else {
			// Close Monte Carlo window if open
			closeMonteCarloWindow(columnName);
		}
	}

	private void openOrUpdateMonteCarloWindow(String columnName, int monitorRange) {
		// Get current top symbols for this Z-Score column
		java.util.List<String> topSymbols = getCurrentTopSymbols(columnName, monitorRange);

		if (topSymbols.isEmpty()) {
			parentDashboard.logToConsole("No symbols found for Monte Carlo: " + columnName);
			return;
		}

		SwingUtilities.invokeLater(() -> {
			if (monteCarloApps.containsKey(columnName)) {
				// Update existing Monte Carlo window
				MonteCarloGraphApp existingApp = monteCarloApps.get(columnName);
				existingApp.setTopList(new HashSet<>(topSymbols));
				if (existingApp.isInitialized()) {
					existingApp.updateSymbols(topSymbols);
					parentDashboard.logToConsole("Updated Monte Carlo window for: " + columnName);
				} else {
					// Reinitialize if needed
					existingApp.initialize(topSymbols, columnName);
				}
			} else {
				// Create new Monte Carlo window
				try {
					MonteCarloGraphApp newApp = new MonteCarloGraphApp(topSymbols, columnName);
					newApp.setTopList(new HashSet<>(topSymbols));
					monteCarloApps.put(columnName, newApp);

					// Set custom title
					setMonteCarloWindowTitle(columnName, newApp);

					parentDashboard.logToConsole("Opened Monte Carlo window for: " + columnName);
				} catch (Exception e) {
					parentDashboard
							.logToConsole("Error opening Monte Carlo window for " + columnName + ": " + e.getMessage());
				}
			}
		});
	}

	private void setMonteCarloWindowTitle(String columnName, MonteCarloGraphApp app) {
		// This would require modifying MonteCarloGraphApp to support custom titles
		// For now, we'll log the intended title
		String title = "Monte Carlo - " + columnName + " - Top Symbols";
		parentDashboard.logToConsole("Monte Carlo Window Title: " + title);

		// If MonteCarloGraphApp supports custom titles, you would call:
		// app.setCustomTitle(title);
	}

	private void closeMonteCarloWindow(String columnName) {
		if (monteCarloApps.containsKey(columnName)) {
			MonteCarloGraphApp app = monteCarloApps.get(columnName);
			app.close();
			monteCarloApps.remove(columnName);
			parentDashboard.logToConsole("Closed Monte Carlo window for: " + columnName);
		}
	}

	private java.util.List<String> getCurrentTopSymbols(String columnName, int monitorRange) {
		// This should use the same logic as in
		// ZScoreAlertManager.getCurrentTopSymbols()
		java.util.List<String> symbols = new java.util.ArrayList<>();

		try {
			// Find the column index
			int columnIndex = -1;
			for (int i = 0; i < parentDashboard.getTableModel().getColumnCount(); i++) {
				if (parentDashboard.getTableModel().getColumnName(i).equals(columnName)) {
					columnIndex = i;
					break;
				}
			}

			if (columnIndex == -1)
				return symbols;

			// Collect symbols and their ranks with proper ordering
			List<SymbolRank> symbolRanks = new ArrayList<>();
			for (int row = 0; row < parentDashboard.getTableModel().getRowCount(); row++) {
				Object rankObj = parentDashboard.getTableModel().getValueAt(row, columnIndex);
				String symbol = (String) parentDashboard.getTableModel().getValueAt(row, 1); // Symbol column

				if (rankObj instanceof Number && symbol != null) {
					int rank = ((Number) rankObj).intValue();
					symbolRanks.add(new SymbolRank(symbol, rank));
				}
			}

			// Sort by rank (ascending - lower rank number is better)
			symbolRanks.sort(Comparator.comparingInt(SymbolRank::getRank));

			// Get top N symbols in order
			for (int i = 0; i < Math.min(monitorRange, symbolRanks.size()); i++) {
				symbols.add(symbolRanks.get(i).getSymbol());
			}

		} catch (Exception e) {
			parentDashboard.logToConsole("Error getting top symbols for Monte Carlo: " + e.getMessage());
		}

		return symbols;
	}

	private void openAllMonteCarloWindows() {
		for (int i = 0; i < configTableModel.getRowCount(); i++) {
			String colName = (String) configTableModel.getValueAt(i, 1);
			Boolean monteCarloEnabled = (Boolean) configTableModel.getValueAt(i, 3);

			if (monteCarloEnabled && !colName.equals("No Z-Score columns available")) {
				try {
					String rangeStr = (String) configTableModel.getValueAt(i, 2);
					int range = Integer.parseInt(rangeStr);
					openOrUpdateMonteCarloWindow(colName, range);
				} catch (NumberFormatException e) {
					parentDashboard.logToConsole("Invalid range for " + colName);
				}
			}
		}
	}

	// Method to update Monte Carlo windows when Z-Score rankings change
	public void updateMonteCarloWindows(String columnName, java.util.List<String> currentTopSymbols) {
		if (alertConfigs.containsKey(columnName)) {
			ZScoreAlertConfig config = alertConfigs.get(columnName);
			if (config.isMonteCarloEnabled() && monteCarloApps.containsKey(columnName)) {
				MonteCarloGraphApp app = monteCarloApps.get(columnName);
				if (app.isInitialized()) {
					app.setTopList(new HashSet<>(currentTopSymbols));
					app.updateSymbols(currentTopSymbols);
					parentDashboard.logToConsole("Updated Monte Carlo symbols for: " + columnName);
				}
			}
		}
	}

	public Map<String, ZScoreAlertConfig> getAlertConfigs() {
		return alertConfigs;
	}

	public boolean isZScoreAlertEnabled() {
		return enableZScoreAlertCheckbox.isSelected();
	}

	public boolean isSaved() {
		return saved;
	}

	// Custom cell renderer to handle disabled state
	private class ZScoreAlertCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (!enableZScoreAlertCheckbox.isSelected() && column != 1) {
				c.setBackground(Color.LIGHT_GRAY);
				c.setForeground(Color.DARK_GRAY);
			} else {
				if (isSelected) {
					c.setBackground(table.getSelectionBackground());
					c.setForeground(table.getSelectionForeground());
				} else {
					c.setBackground(table.getBackground());
					c.setForeground(table.getForeground());
				}
			}

			return c;
		}
	}

	// Helper class for symbol ranking
	private static class SymbolRank {
		private final String symbol;
		private final int rank;

		public SymbolRank(String symbol, int rank) {
			this.symbol = symbol;
			this.rank = rank;
		}

		public String getSymbol() {
			return symbol;
		}

		public int getRank() {
			return rank;
		}
	}
}