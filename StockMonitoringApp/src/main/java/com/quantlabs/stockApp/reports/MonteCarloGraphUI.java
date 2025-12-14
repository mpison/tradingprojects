package com.quantlabs.stockApp.reports;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import org.jdesktop.swingx.JXDatePicker;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.DateRange;
import org.jfree.data.time.Minute;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import com.quantlabs.stockApp.model.PriceData;

public class MonteCarloGraphUI {
	private final List<String> symbols;
	private final MonteCarloDataSourceManager dataSourceManager;

	// UI Components
	private JComboBox<String> dataSourceCombo;
	private ChartFrame frame;
	private JFreeChart chart;
	private XYPlot plot;
	private TimeSeriesCollection dataset;
	private XYLineAndShapeRenderer renderer;
	private JPanel checkboxPanel;
	private JScrollPane checkboxScrollPane;

	// Data management
	private Map<String, Map<ZonedDateTime, Double[]>> symbolData = new ConcurrentHashMap<>();
	private Map<String, TimeSeries> symbolSeriesMap = new HashMap<>();
	private Map<String, JCheckBox> symbolCheckboxes = new HashMap<>();
	private List<String> currentSymbols;
	private Set<String> topList = new HashSet<>();

	private Set<String> primarySymbols = new HashSet<>();
	private Map<String, Float> primarySymbolThickness = new HashMap<>();
	private Map<String, Color> primarySymbolColors = new HashMap<>();

	// Live updates
	private ScheduledExecutorService montecarloLiveUpdateScheduler;
	private JComboBox<String> timeRangeCombo;
	private JComboBox<Integer> refreshIntervalCombo;
	private JButton startLiveButton;
	private JButton stopLiveButton;

	private String titleName = "";

	private Map<String, Float> symbolThickness = new HashMap<>();
	private Map<String, Color> symbolColors = new HashMap<>();
	private Map<String, Integer> symbolSeriesIndices = new HashMap<>();

	// MonteCarlo settings
	private Map<String, Object> monteCarloConfig;
	private Map<String, PriceData> priceDataMap;
	private List<String> selectedColumns;
	private Map<String, Object> graphSettings;

	// primary symbols
	// Add these instance variables to the class
	private JSpinner primaryThicknessSpinner;
	private JButton primaryColorButton;

	//
	private JRadioButton currentTimeRadio;
	private JRadioButton timeRangeRadio;
	private JXDatePicker startDatePicker;
	private JXDatePicker endDatePicker;
	private JComboBox<String> startHourCombo;
	private JComboBox<String> startMinuteCombo;
	private JComboBox<String> endHourCombo;
	private JComboBox<String> endMinuteCombo;

	private JComboBox<String> dialogTimeRangeCombo;
	private String currentTimeframe = "1Min";
	private JComboBox<String> timeframeCombo;
	private ButtonGroup timeRangeGroup;
	private boolean useCustomTimeRange = false;
	private Date savedStartDate;
	private Date savedEndDate;
	private String savedStartHour = "09";
	private String savedStartMinute = "30";
	private String savedEndHour = "16";
	private String savedEndMinute = "00";

	private JComboBox<String> displayOptionCombo;

	public MonteCarloGraphUI(List<String> symbols, MonteCarloDataSourceManager dataSourceManager) {
		this(symbols, dataSourceManager, "", new HashSet<>());
	}

	public MonteCarloGraphUI(List<String> symbols, MonteCarloDataSourceManager dataSourceManager, String titleName) {
		this(symbols, dataSourceManager, titleName, new HashSet<>());
	}

	public MonteCarloGraphUI(List<String> symbols, MonteCarloDataSourceManager dataSourceManager, String titleName,
			Set<String> topList) {
		this.currentSymbols = new ArrayList<>(symbols);
		this.symbols = new ArrayList<>(symbols);
		this.dataSourceManager = dataSourceManager;
		this.titleName = titleName;
		this.topList = topList != null ? new HashSet<>(topList) : new HashSet<>();

		// Ensure selectedColumns is initialized from monteCarloConfig
		if (this.selectedColumns == null || this.selectedColumns.isEmpty()) {
			@SuppressWarnings("unchecked")
			List<String> configColumns = (List<String>) this.monteCarloConfig.get("selectedColumns");
			if (configColumns != null) {
				this.selectedColumns = new ArrayList<>(configColumns);
				System.out.println("Loaded selectedColumns from config: " + this.selectedColumns.size() + " columns");
			}
		}
	}

	public MonteCarloGraphUI(List<String> symbols, MonteCarloDataSourceManager dataSourceManager, String titleName,
			Set<String> topList, boolean useCustomTimeRange, ZonedDateTime startTime, ZonedDateTime endTime,
			String timeframe, Map<String, Object> monteCarloConfig, Map<String, PriceData> priceDataMap,
			List<String> selectedColumns, Map<String, Object> graphSettings) {
		this.currentSymbols = new ArrayList<>(symbols);
		this.symbols = new ArrayList<>(symbols);
		this.dataSourceManager = dataSourceManager;
		this.titleName = titleName;
		this.topList = topList != null ? new HashSet<>(topList) : new HashSet<>();

// Initialize time range settings if provided
		if (useCustomTimeRange && startTime != null && endTime != null) {
			this.useCustomTimeRange = true;
			this.savedStartDate = Date.from(startTime.toInstant());
			this.savedEndDate = Date.from(endTime.toInstant());
			this.savedStartHour = String.format("%02d", startTime.getHour());
			this.savedStartMinute = String.format("%02d", startTime.getMinute());
			this.savedEndHour = String.format("%02d", endTime.getHour());
			this.savedEndMinute = String.format("%02d", endTime.getMinute());
			this.currentTimeframe = timeframe != null ? timeframe : "1Min";
			System.out.println("Initialized with custom time range: " + startTime + " to " + endTime + " timeframe: "
					+ this.currentTimeframe);
		}

		this.monteCarloConfig = monteCarloConfig != null ? new HashMap<>(monteCarloConfig) : new HashMap<>();
		this.priceDataMap = priceDataMap != null ? new HashMap<>(priceDataMap) : new HashMap<>();
		this.selectedColumns = selectedColumns != null ? new ArrayList<>(selectedColumns) : new ArrayList<>();
		this.graphSettings = graphSettings != null ? new HashMap<>(graphSettings) : new HashMap<>();
	}

	public void initialize() {
		createAndShowUI();
	}

	private void createAndShowUI() {
		loadInitialData();
	}

	private void loadInitialData() {
		System.out.println("Loading data for Monte Carlo simulation...");
		new SwingWorker<Map<String, Map<ZonedDateTime, Double[]>>, Void>() {
			@Override
			protected Map<String, Map<ZonedDateTime, Double[]>> doInBackground() {
				try {
					if (useCustomTimeRange) {
						// Use custom time range for initial load
						return fetchDataWithCustomTimeRange(currentSymbols);
					} else {
						// Use default current time range
						
						return fetchDataWithCurrentTime(currentSymbols);
						
						/*ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC);
						ZonedDateTime start = end.minusDays(7);
						return dataSourceManager.fetchData(currentSymbols, start, end, useCustomTimeRange);*/
					}
				} catch (Exception e) {
					System.err.println("Error in initial data load: " + e.getMessage());
					return new HashMap<>();
				}
			}

			@Override
			protected void done() {
				try {
					Map<String, Map<ZonedDateTime, Double[]>> result = get();
					if (result.isEmpty()) {
						JOptionPane.showMessageDialog(null, "No valid data to display for Monte Carlo graph", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					symbolData.putAll(result);
					createMonteCarloLineGraph();

					// Update chart title based on initial time range
					updateChartTitle();

				} catch (Exception e) {
					JOptionPane.showMessageDialog(null, "Error creating graph: " + e.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	private void createMonteCarloLineGraph() {
		dataset = new TimeSeriesCollection();
		String currentSource = dataSourceManager.getCurrentDataSourceName();
		chart = ChartFactory.createTimeSeriesChart(
				"Monte Carlo Simulation - Cumulative Returns (1min) - 7 Days - " + currentSource, "Date and Time",
				"Cumulative Return (%)", dataset, true, true, false);

		chart.setBackgroundPaint(Color.WHITE);
		plot = chart.getXYPlot();

		plot.setBackgroundPaint(Color.LIGHT_GRAY);
		plot.setDomainGridlinePaint(Color.WHITE);
		plot.setRangeGridlinePaint(Color.WHITE);

		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.setDomainCrosshairPaint(Color.BLACK);
		plot.setRangeCrosshairPaint(Color.BLACK);

		DateAxis timeAxis = (DateAxis) plot.getDomainAxis();
		timeAxis.setDateFormatOverride(new SimpleDateFormat("MM-dd HH:mm"));
		timeAxis.setAutoRange(true);

		renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setDefaultToolTipGenerator((ds, series, item) -> {
			TimeSeries ts = ((TimeSeriesCollection) ds).getSeries(series);
			RegularTimePeriod time = ts.getTimePeriod(item);
			Number value = ts.getValue(item);
			return String.format("%s: %s, %.2f%%", ts.getKey(), time, value.doubleValue());
		});

		// Create control panel
		JPanel controlPanel = createControlPanel();

		// Create symbol management panel
		JPanel symbolManagementPanel = createSymbolManagementPanel();

		// Create checkbox panel for symbols with scroll pane
		checkboxPanel = new JPanel();
		checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));

		// Create a wrapper panel to ensure proper scrolling
		JPanel wrapperPanel = new JPanel(new BorderLayout());
		wrapperPanel.add(checkboxPanel, BorderLayout.NORTH);

		// Initialize the scroll pane with proper settings
		checkboxScrollPane = new JScrollPane(wrapperPanel);
		checkboxScrollPane.setPreferredSize(new Dimension(180, 600));
		checkboxScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		checkboxScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		checkboxScrollPane.setBorder(BorderFactory.createTitledBorder("Symbols"));
		checkboxScrollPane.getVerticalScrollBar().setUnitIncrement(16);

		initializeCheckboxPanel();

		ChartPanel chartPanel = createChartPanel();

		// Create main frame
		frame = new ChartFrame("Monte Carlo Simulation - " + currentSource + " " + titleName, chart);
		frame.setLayout(new BorderLayout());

		// ADD SYMBOL MENU BAR
		JMenuBar menuBar = createSymbolMenuBar();
		frame.setJMenuBar(menuBar);

		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(controlPanel, BorderLayout.NORTH);
		topPanel.add(symbolManagementPanel, BorderLayout.CENTER);
		topPanel.add(chartPanel, BorderLayout.SOUTH);
		frame.add(topPanel, BorderLayout.CENTER);
		frame.add(checkboxScrollPane, BorderLayout.WEST);
		frame.setPreferredSize(new Dimension(1000, 650));
		frame.pack();
		frame.setVisible(true);

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (montecarloLiveUpdateScheduler != null) {
					montecarloLiveUpdateScheduler.shutdown();
				}
			}
		});

		// Initialize chart series after everything is set up
		updateChartSeries();

		// Add session markers AFTER the chart is fully set up
		SwingUtilities.invokeLater(() -> {
			try {
				Thread.sleep(500); // Wait for chart to initialize
				addSimpleSessionMarkers();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private JMenuBar createSymbolMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		// Symbol Menu (keep only symbol-related items)
		JMenu symbolMenu = new JMenu("Symbol");

		// Existing symbol menu items
		JMenuItem uncheckAllMenuItem = new JMenuItem("Uncheck All Symbols");
		uncheckAllMenuItem.addActionListener(e -> uncheckAllSymbols());

		JMenuItem checkAllMenuItem = new JMenuItem("Check All Symbols");
		checkAllMenuItem.addActionListener(e -> checkAllSymbols());

		JMenuItem setEditListMenuItem = new JMenuItem("Set EditList");
		setEditListMenuItem.addActionListener(e -> setEditList());

		JMenuItem editTopListMenuItem = new JMenuItem("Edit TopList");
		editTopListMenuItem.addActionListener(e -> editTopList());

		JMenuItem toggleTopListMenuItem = new JMenuItem("Toggle TopList");
		toggleTopListMenuItem.addActionListener(e -> toggleTopList());

		JMenuItem untoggleTopListMenuItem = new JMenuItem("UnToggle TopList");
		untoggleTopListMenuItem.addActionListener(e -> untoggleTopList());

		JMenuItem configurePrimarySymbolsMenuItem = new JMenuItem("Configure Primary Symbols");
		configurePrimarySymbolsMenuItem.addActionListener(e -> configurePrimarySymbols());

		JMenuItem configureLineStylesMenuItem = new JMenuItem("Configure Regular Symbols Styles");
		configureLineStylesMenuItem.addActionListener(e -> configureLineStyles());

		JMenuItem resetStylesMenuItem = new JMenuItem("Reset Line Styles");
		resetStylesMenuItem.addActionListener(e -> resetLineStyles());

		// Add only symbol-related items to Symbol menu
		symbolMenu.add(uncheckAllMenuItem);
		symbolMenu.add(checkAllMenuItem);
		symbolMenu.addSeparator();
		symbolMenu.add(setEditListMenuItem);
		symbolMenu.add(editTopListMenuItem);
		symbolMenu.add(toggleTopListMenuItem);
		symbolMenu.add(untoggleTopListMenuItem);
		symbolMenu.addSeparator();
		symbolMenu.add(configurePrimarySymbolsMenuItem);
		symbolMenu.add(configureLineStylesMenuItem);
		symbolMenu.add(resetStylesMenuItem);

		// NEW: Charts Menu
		JMenu chartsMenu = new JMenu("Charts");

		JMenuItem configureTimeRangeMenuItem = new JMenuItem("Configure Time Range");
		configureTimeRangeMenuItem.addActionListener(e -> configureTimeRange());

		JMenuItem dataSourceMenuItem = new JMenuItem("Data Source");
		dataSourceMenuItem.addActionListener(e -> showDataSourceDialog());

		JMenuItem customizeIndicatorsMenuItem = new JMenuItem("Customize Indicators");
		customizeIndicatorsMenuItem.addActionListener(e -> customizeIndicators());

		// Add items to Charts menu
		chartsMenu.add(configureTimeRangeMenuItem);
		chartsMenu.add(dataSourceMenuItem);
		chartsMenu.add(customizeIndicatorsMenuItem);

		// Add both menus to menu bar
		menuBar.add(symbolMenu);
		menuBar.add(chartsMenu);

		return menuBar;
	}

	private void showDataSourceDialog() {
		JDialog dialog = new JDialog(frame, "Select Data Source", true);
		dialog.setLayout(new BorderLayout(10, 10));
		dialog.setPreferredSize(new Dimension(400, 200));

		// Main panel
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Current data source label
		String currentSource = dataSourceManager.getCurrentDataSourceName();
		JLabel currentLabel = new JLabel("Current Data Source: " + currentSource);
		currentLabel.setFont(currentLabel.getFont().deriveFont(Font.BOLD));

		// Available sources combo box
		JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		comboPanel.add(new JLabel("Select Data Source:"));

		JComboBox<String> dataSourceCombo = new JComboBox<>();
		updateDataSourceCombo(dataSourceCombo); // Populate with available sources
		dataSourceCombo.setSelectedItem(currentSource);
		comboPanel.add(dataSourceCombo);

		// Available sources list
		JPanel availablePanel = new JPanel(new BorderLayout());
		availablePanel.setBorder(BorderFactory.createTitledBorder("Available Data Sources"));

		DefaultListModel<String> listModel = new DefaultListModel<>();
		List<String> availableSources = dataSourceManager.getAvailableDataSources();
		for (String source : availableSources) {
			listModel.addElement(source);
		}

		JList<String> availableList = new JList<>(listModel);
		availableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Select current source in list
		for (int i = 0; i < listModel.size(); i++) {
			if (listModel.getElementAt(i).equals(currentSource)) {
				availableList.setSelectedIndex(i);
				break;
			}
		}

		JScrollPane listScrollPane = new JScrollPane(availableList);
		listScrollPane.setPreferredSize(new Dimension(350, 80));
		availablePanel.add(listScrollPane, BorderLayout.CENTER);

		// Button panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton switchButton = new JButton("Switch Data Source");
		JButton cancelButton = new JButton("Cancel");

		switchButton.addActionListener(e -> {
			String selectedSource = (String) dataSourceCombo.getSelectedItem();
			if (selectedSource != null && !selectedSource.equals(currentSource)) {
				switchDataSource(selectedSource);
				dialog.dispose();
			} else {
				JOptionPane.showMessageDialog(dialog, "Please select a different data source", "No Change",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});

		cancelButton.addActionListener(e -> {
			dialog.dispose();
		});

		buttonPanel.add(cancelButton);
		buttonPanel.add(switchButton);

		// Add components to main panel
		mainPanel.add(currentLabel, BorderLayout.NORTH);
		mainPanel.add(comboPanel, BorderLayout.CENTER);
		mainPanel.add(availablePanel, BorderLayout.SOUTH);

		dialog.add(mainPanel, BorderLayout.CENTER);
		dialog.add(buttonPanel, BorderLayout.SOUTH);

		dialog.pack();
		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);
	}

	// Helper method to populate combo box
	private void updateDataSourceCombo(JComboBox<String> combo) {
		combo.removeAllItems();
		List<String> availableSources = dataSourceManager.getAvailableDataSources();
		for (String source : availableSources) {
			combo.addItem(source);
		}
	}

	private void customizeIndicators() {
		// Placeholder for indicators customization
		JOptionPane.showMessageDialog(frame, "Indicators customization feature coming soon!", "Customize Indicators",
				JOptionPane.INFORMATION_MESSAGE);
	}

	private void configureTimeRange() {
		JDialog dialog = new JDialog(frame, "Configure Time Range", true);
		dialog.setLayout(new BorderLayout(10, 10));
		dialog.setPreferredSize(new Dimension(550, 450));

		// Main panel
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Radio button panel
		JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		radioPanel.setBorder(BorderFactory.createTitledBorder("Time Range Mode"));

		currentTimeRadio = new JRadioButton("Current Time (Live Updates Available)");
		timeRangeRadio = new JRadioButton("Custom Time Range");

		timeRangeGroup = new ButtonGroup();
		timeRangeGroup.add(currentTimeRadio);
		timeRangeGroup.add(timeRangeRadio);

		// Set based on saved state
		currentTimeRadio.setSelected(!useCustomTimeRange);
		timeRangeRadio.setSelected(useCustomTimeRange);

		radioPanel.add(currentTimeRadio);
		radioPanel.add(timeRangeRadio);

		// Current Time configuration panel
		JPanel currentTimePanel = createCurrentTimePanel();
		currentTimePanel.setBorder(BorderFactory.createTitledBorder("Current Time Settings"));

		// Time range configuration panel
		JPanel timeRangePanel = createTimeRangePanel();
		timeRangePanel.setBorder(BorderFactory.createTitledBorder("Custom Time Range Settings"));

		// Update panel visibility based on saved state
		currentTimePanel.setVisible(!useCustomTimeRange);
		timeRangePanel.setVisible(useCustomTimeRange);

		// Add radio button listeners
		currentTimeRadio.addActionListener(e -> {
			currentTimePanel.setVisible(true);
			timeRangePanel.setVisible(false);
			updateControlPanelState(false);
			dialog.pack();
		});

		timeRangeRadio.addActionListener(e -> {
			currentTimePanel.setVisible(false);
			timeRangePanel.setVisible(true);
			updateControlPanelState(true);
			dialog.pack();
		});

		// Button panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton applyButton = new JButton("Apply");
		JButton cancelButton = new JButton("Cancel");

		applyButton.addActionListener(e -> {
			saveTimeRangeConfiguration();
			applyTimeRangeConfiguration();
			dialog.dispose();
		});

		cancelButton.addActionListener(e -> {
			dialog.dispose();
		});

		buttonPanel.add(cancelButton);
		buttonPanel.add(applyButton);

		// Add components to main panel
		mainPanel.add(radioPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		mainPanel.add(currentTimePanel);
		mainPanel.add(timeRangePanel);
		mainPanel.add(Box.createVerticalGlue());
		mainPanel.add(buttonPanel);

		dialog.add(mainPanel);
		dialog.pack();
		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);
	}

	private JPanel createCurrentTimePanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		// Time range combo box
		panel.add(new JLabel("Time Range:"));

		String[] timeRanges = { "1 Day", "2 Days", "3 Days", "4 Days", "5 Days", "6 Days", "7 Days", "8 Days" };
		dialogTimeRangeCombo = new JComboBox<>(timeRanges);

		// Set the saved or current selection
		if (timeRangeCombo != null) {
			// Use the current selection from the main control panel
			dialogTimeRangeCombo.setSelectedItem(timeRangeCombo.getSelectedItem());
		} else {
			// Default to 1 Day
			dialogTimeRangeCombo.setSelectedItem("1 Day");
		}

		panel.add(dialogTimeRangeCombo);

		// Timeframe information (read-only for current time mode)
		panel.add(new JLabel("Timeframe:"));
		JLabel timeframeLabel = new JLabel("1Min (Fixed)");
		timeframeLabel.setForeground(Color.GRAY);
		panel.add(timeframeLabel);

		// Information label about live updates
		JLabel infoLabel = new JLabel("Live updates and refresh intervals are available in this mode");
		infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC, 11f));
		infoLabel.setForeground(Color.BLUE);

		JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		infoPanel.add(infoLabel);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add(panel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		mainPanel.add(infoPanel);

		return mainPanel;
	}

	private JPanel createTimeRangePanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		// Initialize saved values if null (first time)
		if (savedStartDate == null) {
			savedStartDate = Date.from(ZonedDateTime.now().minusDays(1).toInstant());
			savedEndDate = new Date();
		}

		// Start date and time panel
		JPanel startPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		startPanel.add(new JLabel("Start:"));

		startDatePicker = new JXDatePicker();
		startDatePicker.setDate(savedStartDate); // Use saved value
		startDatePicker.setFormats(new SimpleDateFormat("yyyy-MM-dd"));
		startPanel.add(startDatePicker);

		startHourCombo = createTimeComboBox(0, 23);
		startMinuteCombo = createTimeComboBox(0, 59);

		// Set saved time values
		startHourCombo.setSelectedItem(savedStartHour);
		startMinuteCombo.setSelectedItem(savedStartMinute);

		startPanel.add(new JLabel("Time:"));
		startPanel.add(startHourCombo);
		startPanel.add(new JLabel(":"));
		startPanel.add(startMinuteCombo);

		// End date and time panel
		JPanel endPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		endPanel.add(new JLabel("End:"));

		endDatePicker = new JXDatePicker();
		endDatePicker.setDate(savedEndDate); // Use saved value
		endDatePicker.setFormats(new SimpleDateFormat("yyyy-MM-dd"));
		endPanel.add(endDatePicker);

		endHourCombo = createTimeComboBox(0, 23);
		endMinuteCombo = createTimeComboBox(0, 59);

		// Set saved time values
		endHourCombo.setSelectedItem(savedEndHour);
		endMinuteCombo.setSelectedItem(savedEndMinute);

		endPanel.add(new JLabel("Time:"));
		endPanel.add(endHourCombo);
		endPanel.add(new JLabel(":"));
		endPanel.add(endMinuteCombo);

		// Timeframe selection panel
		JPanel timeframePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		timeframePanel.add(new JLabel("Timeframe:"));

		String[] timeframes = { "1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min" };
		timeframeCombo = new JComboBox<>(timeframes);
		timeframeCombo.setSelectedItem(currentTimeframe); // Use current timeframe
		timeframePanel.add(timeframeCombo);

		// Information label
		JLabel infoLabel = new JLabel("Live updates are disabled in custom time range mode");
		infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC, 11f));
		infoLabel.setForeground(Color.RED);

		JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		infoPanel.add(infoLabel);

		panel.add(startPanel);
		panel.add(Box.createRigidArea(new Dimension(0, 5)));
		panel.add(endPanel);
		panel.add(Box.createRigidArea(new Dimension(0, 10)));
		panel.add(timeframePanel);
		panel.add(Box.createRigidArea(new Dimension(0, 5)));
		panel.add(infoPanel);

		return panel;
	}

	private JComboBox<String> createTimeComboBox(int min, int max) {
		JComboBox<String> combo = new JComboBox<>();
		for (int i = min; i <= max; i++) {
			combo.addItem(String.format("%02d", i));
		}
		combo.setPreferredSize(new Dimension(60, 25));
		return combo;
	}

	private void updateControlPanelState(boolean customTimeRangeEnabled) {
		if (timeRangeCombo != null) {
			timeRangeCombo.setEnabled(!customTimeRangeEnabled);
		}
		if (refreshIntervalCombo != null) {
			refreshIntervalCombo.setEnabled(!customTimeRangeEnabled);
		}
		if (startLiveButton != null) {
			startLiveButton.setEnabled(!customTimeRangeEnabled);
		}
		if (stopLiveButton != null) {
			// Stop live updates if switching to custom time range
			if (customTimeRangeEnabled && montecarloLiveUpdateScheduler != null) {
				stopLiveUpdates();
			}
			stopLiveButton.setEnabled(!customTimeRangeEnabled && montecarloLiveUpdateScheduler != null);
		}

		// Volume display combo is always enabled regardless of time range mode
		if (displayOptionCombo != null) {
			displayOptionCombo.setEnabled(true);
		}

		// Update the main control panel time range combo visibility if needed
		if (timeRangeCombo != null) {
			timeRangeCombo.setVisible(!customTimeRangeEnabled);
			// Find the label for time range and hide/show it too
			Component[] components = timeRangeCombo.getParent().getComponents();
			for (Component comp : components) {
				if (comp instanceof JLabel) {
					JLabel label = (JLabel) comp;
					if ("Time Range:".equals(label.getText())) {
						label.setVisible(!customTimeRangeEnabled);
						break;
					}
				}
			}
		}
	}

	private void applyTimeRangeConfiguration() {
		// This method now just applies the already-saved configuration
		if (useCustomTimeRange) {
			// Apply custom time range
			refreshDataWithCustomTimeRange();
		} else {
			// Use current time implementation
			refreshData();
		}

		// Update control panel state
		updateControlPanelState(useCustomTimeRange);
	}

	private void configureLineStyles() {
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

		// Create header panel
		JPanel headerPanel = new JPanel(new GridLayout(1, 3, 10, 5));
		headerPanel.add(new JLabel("Symbol", JLabel.CENTER));
		headerPanel.add(new JLabel("Thickness", JLabel.CENTER));
		headerPanel.add(new JLabel("Color", JLabel.CENTER));

		// Create scroll panel for symbols (only non-primary symbols)
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

		Map<String, JSpinner> thicknessSpinners = new HashMap<>();
		Map<String, JButton> colorButtons = new HashMap<>();

		// Get only non-primary symbols
		List<String> regularSymbols = getNonPrimarySymbols();

		// Create a row for each regular symbol
		for (String symbol : regularSymbols) {
			JPanel symbolRow = new JPanel(new GridLayout(1, 3, 10, 5));
			symbolRow.setMaximumSize(new Dimension(400, 40));
			symbolRow.setPreferredSize(new Dimension(400, 40));

			// Symbol label
			JLabel symbolLabel = new JLabel(symbol);
			symbolLabel.setHorizontalAlignment(JLabel.CENTER);
			symbolRow.add(symbolLabel);

			// Thickness spinner
			SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1.5, 0.5, 5.0, 0.5);
			JSpinner thicknessSpinner = new JSpinner(spinnerModel);

			// Set current thickness if exists
			Float currentThickness = symbolThickness.get(symbol);
			if (currentThickness != null) {
				thicknessSpinner.setValue(currentThickness);
			}

			thicknessSpinner.setToolTipText("Line thickness for " + symbol);
			symbolRow.add(thicknessSpinner);
			thicknessSpinners.put(symbol, thicknessSpinner);

			// Color button
			JButton colorButton = new JButton("Choose Color");
			Color currentColor = symbolColors.get(symbol);
			if (currentColor != null) {
				colorButton.setBackground(currentColor);
				colorButton.setForeground(getContrastColor(currentColor));
			} else {
				// Set default color based on symbol position for visual reference
				int index = regularSymbols.indexOf(symbol);
				Color defaultColor = getDistinctColor(index);
				colorButton.setBackground(defaultColor);
				colorButton.setForeground(getContrastColor(defaultColor));
			}

			colorButton.addActionListener(e -> {
				Color newColor = JColorChooser.showDialog(frame, "Choose color for " + symbol,
						colorButton.getBackground());
				if (newColor != null) {
					colorButton.setBackground(newColor);
					colorButton.setForeground(getContrastColor(newColor));
					// Apply immediately
					applyRegularSymbolStyle(symbol, newColor, (Double) thicknessSpinner.getValue());
				}
			});

			// Add listener to thickness spinner for immediate preview
			thicknessSpinner.addChangeListener(e -> {
				applyRegularSymbolStyle(symbol, colorButton.getBackground(), (Double) thicknessSpinner.getValue());
			});

			symbolRow.add(colorButton);
			colorButtons.put(symbol, colorButton);

			contentPanel.add(symbolRow);
			contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		}

		JScrollPane scrollPane = new JScrollPane(contentPanel);
		scrollPane.setPreferredSize(new Dimension(450, 400));
		scrollPane
				.setBorder(BorderFactory.createTitledBorder("Regular Symbols (" + regularSymbols.size() + " symbols)"));

		mainPanel.add(headerPanel, BorderLayout.NORTH);
		mainPanel.add(scrollPane, BorderLayout.CENTER);

		// Add control panel at bottom
		JPanel controlPanel = createRegularSymbolsControlPanel(thicknessSpinners, colorButtons);
		mainPanel.add(controlPanel, BorderLayout.SOUTH);

		// Show dialog
		int result = JOptionPane.showConfirmDialog(frame, mainPanel, "Configure Regular Symbols Line Styles",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (result == JOptionPane.OK_OPTION) {
			applyRegularSymbolsStyles(thicknessSpinners, colorButtons);
			JOptionPane.showMessageDialog(frame,
					"Regular symbols styles updated for " + regularSymbols.size() + " symbols", "Success",
					JOptionPane.INFORMATION_MESSAGE);
		} else {
			// If user cancelled, revert any preview changes
			updateChartSeries();
		}
	}

	private void refreshDataWithCustomTimeRange() {
		if (!useCustomTimeRange) {
			refreshData();
			return;
		}

		new SwingWorker<Map<String, Map<ZonedDateTime, Double[]>>, Void>() {
			@Override
			protected Map<String, Map<ZonedDateTime, Double[]>> doInBackground() {
				return fetchDataWithCustomTimeRange(getCheckedSymbols());
			}

			@Override
			protected void done() {
				try {
					Map<String, Map<ZonedDateTime, Double[]>> result = get();
					if (!result.isEmpty()) {
						symbolData.putAll(result);
						updateChartSeries();
						updateChartTitle();
						addSimpleSessionMarkers();
					}
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(frame,
							"Error updating graph with custom time range: " + ex.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	private void applyRegularSymbolStyle(String symbol, Color color, double thickness) {
		symbolThickness.put(symbol, (float) thickness);
		if (color != null && !color.equals(UIManager.getColor("Button.background"))) {
			symbolColors.put(symbol, color);
		}
		updateSymbolThickness(symbol, (float) thickness);
		updateSymbolColor(symbol, color);
	}

	private void applyRegularSymbolsStyles(Map<String, JSpinner> thicknessSpinners, Map<String, JButton> colorButtons) {
		for (String symbol : thicknessSpinners.keySet()) {
			JSpinner thicknessSpinner = thicknessSpinners.get(symbol);
			JButton colorButton = colorButtons.get(symbol);

			if (thicknessSpinner != null) {
				double thicknessValue = (Double) thicknessSpinner.getValue();
				symbolThickness.put(symbol, (float) thicknessValue);
			}

			if (colorButton != null) {
				Color color = colorButton.getBackground();
				if (!color.equals(UIManager.getColor("Button.background"))) {
					symbolColors.put(symbol, color);
				}
			}
		}
		updateChartSeries();
	}

	private JPanel createPrimarySymbolsConfigPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.setBorder(BorderFactory.createTitledBorder("Primary Symbols Configuration"));

		JLabel primaryLabel = new JLabel("Primary Symbols (comma separated):");
		JTextField primarySymbolsField = new JTextField(30);

		// Set current primary symbols
		String currentPrimaryText = String.join(", ", primarySymbols);
		primarySymbolsField.setText(currentPrimaryText);

		JButton applyPrimaryButton = new JButton("Apply Primary Symbols");
		applyPrimaryButton.addActionListener(e -> {
			String primaryText = primarySymbolsField.getText().trim();
			if (!primaryText.isEmpty()) {
				Set<String> newPrimarySymbols = parseSymbolsFromText(primaryText);
				setPrimarySymbols(newPrimarySymbols);
				JOptionPane.showMessageDialog(frame, "Primary symbols updated: " + String.join(", ", newPrimarySymbols),
						"Success", JOptionPane.INFORMATION_MESSAGE);
			}
		});

		panel.add(primaryLabel);
		panel.add(primarySymbolsField);
		panel.add(applyPrimaryButton);

		return panel;
	}

	private Set<String> parseSymbolsFromText(String text) {
		Set<String> symbols = new HashSet<>();
		String[] symbolArray = text.split(",");
		for (String symbol : symbolArray) {
			String cleanSymbol = symbol.trim().toUpperCase();
			if (!cleanSymbol.isEmpty()) {
				symbols.add(cleanSymbol);
			}
		}
		return symbols;
	}

	private JPanel createSymbolsStylePanel(boolean forPrimarySymbols) {
		JPanel panel = new JPanel(new BorderLayout(10, 10));

		// Create header panel
		JPanel headerPanel = new JPanel(new GridLayout(1, 3, 10, 5));
		headerPanel.add(new JLabel("Symbol", JLabel.CENTER));
		headerPanel.add(new JLabel("Thickness", JLabel.CENTER));
		headerPanel.add(new JLabel("Color", JLabel.CENTER));

		// Create scroll panel for symbols
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

		Map<String, JSpinner> thicknessSpinners = new HashMap<>();
		Map<String, JButton> colorButtons = new HashMap<>();

		// Get the appropriate symbol list
		List<String> symbols = forPrimarySymbols ? new ArrayList<>(primarySymbols) : getNonPrimarySymbols();

		String panelTitle = forPrimarySymbols ? "Primary Symbols" : "Regular Symbols";

		// Create a row for each symbol
		for (String symbol : symbols) {
			JPanel symbolRow = new JPanel(new GridLayout(1, 3, 10, 5));
			symbolRow.setMaximumSize(new Dimension(400, 40));
			symbolRow.setPreferredSize(new Dimension(400, 40));

			// Symbol label
			JLabel symbolLabel = new JLabel(symbol);
			symbolLabel.setHorizontalAlignment(JLabel.CENTER);
			if (forPrimarySymbols) {
				symbolLabel.setFont(symbolLabel.getFont().deriveFont(Font.BOLD));
			}
			symbolRow.add(symbolLabel);

			// Thickness spinner - different defaults for primary symbols
			double defaultThickness = forPrimarySymbols ? 3.0 : 1.5;
			double minThickness = forPrimarySymbols ? 2.0 : 0.5;
			double maxThickness = forPrimarySymbols ? 6.0 : 5.0;

			SpinnerNumberModel spinnerModel = new SpinnerNumberModel(defaultThickness, minThickness, maxThickness, 0.5);
			JSpinner thicknessSpinner = new JSpinner(spinnerModel);

			// Set current thickness if exists
			Float currentThickness = forPrimarySymbols ? primarySymbolThickness.get(symbol)
					: symbolThickness.get(symbol);
			if (currentThickness != null) {
				thicknessSpinner.setValue(currentThickness);
			}

			thicknessSpinner.setToolTipText("Line thickness for " + symbol);
			symbolRow.add(thicknessSpinner);
			thicknessSpinners.put(symbol, thicknessSpinner);

			// Color button
			JButton colorButton = new JButton("Choose Color");
			Color defaultColor = forPrimarySymbols ? Color.BLACK : getDistinctColor(symbols.indexOf(symbol));
			Color currentColor = forPrimarySymbols ? primarySymbolColors.get(symbol) : symbolColors.get(symbol);

			if (currentColor != null) {
				colorButton.setBackground(currentColor);
				colorButton.setForeground(getContrastColor(currentColor));
			} else {
				colorButton.setBackground(defaultColor);
				colorButton.setForeground(getContrastColor(defaultColor));
			}

			colorButton.addActionListener(e -> {
				Color newColor = JColorChooser.showDialog(frame, "Choose color for " + symbol,
						colorButton.getBackground());
				if (newColor != null) {
					colorButton.setBackground(newColor);
					colorButton.setForeground(getContrastColor(newColor));
					// Apply immediately for preview
					applySymbolStyle(symbol, newColor, (Double) thicknessSpinner.getValue(), forPrimarySymbols);
				}
			});

			// Add listener to thickness spinner for immediate preview
			thicknessSpinner.addChangeListener(e -> {
				applySymbolStyle(symbol, colorButton.getBackground(), (Double) thicknessSpinner.getValue(),
						forPrimarySymbols);
			});

			symbolRow.add(colorButton);
			colorButtons.put(symbol, colorButton);

			contentPanel.add(symbolRow);
			contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		}

		JScrollPane scrollPane = new JScrollPane(contentPanel);
		scrollPane.setPreferredSize(new Dimension(450, 300));
		scrollPane.setBorder(BorderFactory.createTitledBorder(panelTitle + " (" + symbols.size() + " symbols)"));

		panel.add(headerPanel, BorderLayout.NORTH);
		panel.add(scrollPane, BorderLayout.CENTER);

		// Add control panel at bottom
		JPanel controlPanel = createStyleControlPanel(thicknessSpinners, colorButtons, forPrimarySymbols);
		panel.add(controlPanel, BorderLayout.SOUTH);

		return panel;
	}

	private List<String> getNonPrimarySymbols() {
		List<String> nonPrimary = new ArrayList<>();
		for (String symbol : currentSymbols) {
			if (!primarySymbols.contains(symbol)) {
				nonPrimary.add(symbol);
			}
		}
		return nonPrimary;
	}

	private JPanel createStyleControlPanel(Map<String, JSpinner> thicknessSpinners, Map<String, JButton> colorButtons,
			boolean forPrimarySymbols) {
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

// Default thickness control
		JPanel defaultThicknessPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		defaultThicknessPanel.add(new JLabel("Default Thickness:"));

		double defaultThickness = forPrimarySymbols ? 3.0 : 1.5;
		SpinnerNumberModel defaultModel = new SpinnerNumberModel(defaultThickness, 0.5, 6.0, 0.5);
		JSpinner defaultThicknessSpinner = new JSpinner(defaultModel);
		JButton applyThicknessButton = new JButton("Apply to All");
		applyThicknessButton.addActionListener(e -> {
			double thickness = (Double) defaultThicknessSpinner.getValue();
			for (JSpinner spinner : thicknessSpinners.values()) {
				spinner.setValue(thickness);
			}
		});

		defaultThicknessPanel.add(defaultThicknessSpinner);
		defaultThicknessPanel.add(new JLabel("px"));
		defaultThicknessPanel.add(applyThicknessButton);

// Default color control - FIXED: Use final array to work around lambda restriction
		JPanel defaultColorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		defaultColorPanel.add(new JLabel("Default Color:"));

		JButton defaultColorButton = new JButton("Choose Color");
		JButton applyColorButton = new JButton("Apply to All");

// Use a final array to hold the color reference
		final Color[] defaultColorHolder = new Color[1];
		defaultColorHolder[0] = forPrimarySymbols ? Color.BLACK : null;

		if (defaultColorHolder[0] != null) {
			defaultColorButton.setBackground(defaultColorHolder[0]);
			defaultColorButton.setForeground(getContrastColor(defaultColorHolder[0]));
		}

		defaultColorButton.addActionListener(e -> {
			Color currentBg = defaultColorButton.getBackground();
			Color newDefaultColor = JColorChooser.showDialog(frame, "Choose default color", currentBg);
			if (newDefaultColor != null) {
				defaultColorButton.setBackground(newDefaultColor);
				defaultColorButton.setForeground(getContrastColor(newDefaultColor));
// Store the color in the holder array
				defaultColorHolder[0] = newDefaultColor;
			}
		});

		applyColorButton.addActionListener(e -> {
			if (defaultColorHolder[0] != null) {
				for (JButton colorButton : colorButtons.values()) {
					colorButton.setBackground(defaultColorHolder[0]);
					colorButton.setForeground(getContrastColor(defaultColorHolder[0]));
// Apply the style immediately for preview
					String symbol = getSymbolFromColorButton(colorButton, colorButtons);
					if (symbol != null) {
						JSpinner thicknessSpinner = thicknessSpinners.get(symbol);
						if (thicknessSpinner != null) {
							double thickness = (Double) thicknessSpinner.getValue();
							applySymbolStyle(symbol, defaultColorHolder[0], thickness, forPrimarySymbols);
						}
					}
				}
			} else {
				JOptionPane.showMessageDialog(frame, "Please choose a default color first", "No Color Selected",
						JOptionPane.WARNING_MESSAGE);
			}
		});

		defaultColorPanel.add(defaultColorButton);
		defaultColorPanel.add(applyColorButton);

		controlPanel.add(defaultThicknessPanel);
		controlPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		controlPanel.add(defaultColorPanel);

		return controlPanel;
	}

//Helper method to find symbol from color button
	private String getSymbolFromColorButton(JButton colorButton, Map<String, JButton> colorButtons) {
		for (Map.Entry<String, JButton> entry : colorButtons.entrySet()) {
			if (entry.getValue() == colorButton) {
				return entry.getKey();
			}
		}
		return null;
	}

	private void applySymbolStyle(String symbol, Color color, double thickness, boolean forPrimarySymbols) {
		if (forPrimarySymbols) {
			primarySymbolThickness.put(symbol, (float) thickness);
			if (color != null && !color.equals(UIManager.getColor("Button.background"))) {
				primarySymbolColors.put(symbol, color);
			}
			updateSymbolThickness(symbol, (float) thickness);
			updateSymbolColor(symbol, color);
		} else {
			symbolThickness.put(symbol, (float) thickness);
			if (color != null && !color.equals(UIManager.getColor("Button.background"))) {
				symbolColors.put(symbol, color);
			}
			updateSymbolThickness(symbol, (float) thickness);
			updateSymbolColor(symbol, color);
		}
	}

	/**
	 * Apply styles temporarily for preview
	 */
	private void applyTemporaryStyles(Map<String, JSpinner> thicknessSpinners, Map<String, JButton> colorButtons) {
		Map<String, Float> tempThickness = new HashMap<>();
		Map<String, Color> tempColors = new HashMap<>();

		for (String symbol : currentSymbols) {
			JSpinner thicknessSpinner = thicknessSpinners.get(symbol);
			JButton colorButton = colorButtons.get(symbol);

			if (thicknessSpinner != null) {
				double thicknessValue = (Double) thicknessSpinner.getValue();
				tempThickness.put(symbol, (float) thicknessValue);
			}

			if (colorButton != null) {
				Color color = colorButton.getBackground();
				// Only apply if not the default button color
				if (!color.equals(UIManager.getColor("Button.background"))) {
					tempColors.put(symbol, color);
				}
			}
		}

		// Apply temporary styles
		setSymbolThicknesses(tempThickness);
		setSymbolColors(tempColors);
	}

	/**
	 * Apply styles permanently
	 */
	private void applyStyles(Map<String, JSpinner> thicknessSpinners, Map<String, JButton> colorButtons) {
		Map<String, Float> newThickness = new HashMap<>();
		Map<String, Color> newColors = new HashMap<>();

		for (String symbol : currentSymbols) {
			JSpinner thicknessSpinner = thicknessSpinners.get(symbol);
			JButton colorButton = colorButtons.get(symbol);

			if (thicknessSpinner != null) {
				double thicknessValue = (Double) thicknessSpinner.getValue();
				newThickness.put(symbol, (float) thicknessValue);
			}

			if (colorButton != null) {
				Color color = colorButton.getBackground();
				// Only add if not the default button color
				if (!color.equals(UIManager.getColor("Button.background"))) {
					newColors.put(symbol, color);
				}
			}
		}

		setSymbolThicknesses(newThickness);
		setSymbolColors(newColors);
	}

	// Set EditList - prompt for dialog to edit all symbols
	private void setEditList() {
		String currentSymbolsText = String.join(", ", currentSymbols);

		String newSymbolsText = (String) JOptionPane.showInputDialog(frame, "Edit symbol list (comma separated):",
				"Set EditList", JOptionPane.PLAIN_MESSAGE, null, null, currentSymbolsText);

		if (newSymbolsText != null && !newSymbolsText.trim().isEmpty()) {
			// Parse the new symbols
			String[] symbolArray = newSymbolsText.split(",");
			List<String> newSymbols = new ArrayList<>();

			for (String symbol : symbolArray) {
				String cleanSymbol = symbol.trim().toUpperCase();
				if (!cleanSymbol.isEmpty() && !newSymbols.contains(cleanSymbol)) {
					newSymbols.add(cleanSymbol);
				}
			}

			if (!newSymbols.isEmpty()) {
				// Update the current symbols
				this.currentSymbols.clear();
				this.currentSymbols.addAll(newSymbols);

				// Update UI
				updateCheckboxPanel();
				refreshData();

				JOptionPane.showMessageDialog(frame,
						"Symbol list updated successfully!\nTotal symbols: " + newSymbols.size(), "Success",
						JOptionPane.INFORMATION_MESSAGE);

				System.out.println("Symbol list updated: " + newSymbols);
			}
		}
	}

	// NEW METHOD: Edit TopList - prompt for dialog to edit TopList
	private void editTopList() {
		String currentTopListText = String.join(", ", topList);

		String newTopListText = (String) JOptionPane.showInputDialog(frame, "Edit TopList (comma separated):",
				"Edit TopList", JOptionPane.PLAIN_MESSAGE, null, null, currentTopListText);

		if (newTopListText != null) {
			// Parse the new TopList
			String[] symbolArray = newTopListText.split(",");
			Set<String> newTopList = new HashSet<>();
			List<String> symbolsToAdd = new ArrayList<>();

			for (String symbol : symbolArray) {
				String cleanSymbol = symbol.trim().toUpperCase();
				if (!cleanSymbol.isEmpty()) {
					newTopList.add(cleanSymbol);

					// Check if symbol exists in current symbols, if not add it
					if (!currentSymbols.contains(cleanSymbol)) {
						symbolsToAdd.add(cleanSymbol);
					}
				}
			}

			// Update TopList
			this.topList = newTopList;

			// Add any new symbols from TopList to current symbols
			if (!symbolsToAdd.isEmpty()) {
				currentSymbols.addAll(symbolsToAdd);
				updateCheckboxPanel();
				refreshData();

				JOptionPane.showMessageDialog(frame,
						"TopList updated and " + symbolsToAdd.size() + " new symbols added to the list!", "Success",
						JOptionPane.INFORMATION_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(frame,
						"TopList updated successfully!\nTotal TopList symbols: " + newTopList.size(), "Success",
						JOptionPane.INFORMATION_MESSAGE);
			}

			System.out.println("TopList updated: " + newTopList);
		}
	}

	// NEW METHOD: Toggle TopList - check only TopList symbols, uncheck others
	public void toggleTopList() {

		SwingUtilities.invokeLater(() -> {

			if (topList.isEmpty()) {
				// JOptionPane.showMessageDialog(frame, "TopList is empty. Please edit TopList
				// first.", "Warning",
				// JOptionPane.WARNING_MESSAGE);
				return;
			}

			// Uncheck all symbols first
			uncheckAllSymbols();

			// Check only the symbols in TopList
			for (String symbol : topList) {
				JCheckBox checkbox = symbolCheckboxes.get(symbol);
				if (checkbox != null) {
					checkbox.setSelected(true);
				}
			}

			updateChartSeries(); // Update the chart to reflect changes
			System.out.println("Toggled TopList: Checked only " + topList.size() + " TopList symbols");

			/*
			 * JOptionPane.showMessageDialog( frame, "Checked only TopList symbols: " +
			 * String.join(", ", topList), "TopList Toggled",
			 * JOptionPane.INFORMATION_MESSAGE );
			 */
		});
	}

	// NEW METHOD: UnToggle TopList - check all symbols except TopList
	private void untoggleTopList() {
		if (topList.isEmpty()) {
			// If TopList is empty, just check all symbols
			checkAllSymbols();
			return;
		}

		// Check all symbols first
		checkAllSymbols();

		// Uncheck the symbols in TopList
		for (String symbol : topList) {
			JCheckBox checkbox = symbolCheckboxes.get(symbol);
			if (checkbox != null) {
				checkbox.setSelected(false);
			}
		}

		updateChartSeries(); // Update the chart to reflect changes
		System.out.println("UnToggled TopList: Checked all except " + topList.size() + " TopList symbols");

		/*
		 * JOptionPane.showMessageDialog( frame, "Checked all symbols except TopList: "
		 * + String.join(", ", topList), "TopList UnToggled",
		 * JOptionPane.INFORMATION_MESSAGE );
		 */
	}

	// FIXED: Uncheck All Symbols method
	private void uncheckAllSymbols() {
		// First uncheck all checkboxes
		for (JCheckBox checkbox : symbolCheckboxes.values()) {
			checkbox.setSelected(false);
		}

		// Then update the chart series (which will handle the removal)
		updateChartSeries();

		System.out.println("All symbols unchecked - chart cleared");
	}

	// FIXED: Check All Symbols method
	private void checkAllSymbols() {
		for (JCheckBox checkbox : symbolCheckboxes.values()) {
			checkbox.setSelected(true);
		}
		// Rebuild the dataset with all symbols
		updateChartSeries();
		System.out.println("All symbols checked");
	}

	// Add TopList getter and setter
	public Set<String> getTopList() {
		return new HashSet<>(topList);
	}

	public void setTopList(Set<String> topList) {
		this.topList = topList != null ? new HashSet<>(topList) : new HashSet<>();
	}

	// NEW METHOD: Update all symbols (for Set EditList functionality)
	public void updateAllSymbols(List<String> newSymbols) {
		this.currentSymbols.clear();
		this.currentSymbols.addAll(newSymbols);
		updateCheckboxPanel();
		refreshData();
	}

	private JPanel createControlPanel() {
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		// Data Source Selector
		controlPanel.add(new JLabel("Data Source:"));
		dataSourceCombo = new JComboBox<>();
		updateDataSourceCombo();
		dataSourceCombo.addActionListener(e -> {
			String selectedSource = (String) dataSourceCombo.getSelectedItem();
			if (selectedSource != null && !selectedSource.equals(dataSourceManager.getCurrentDataSourceName())) {
				switchDataSource(selectedSource);
			}
		});
		controlPanel.add(dataSourceCombo);

		// Time Range Selector
		controlPanel.add(new JLabel("Time Range:"));
		String[] timeRanges = { "1 Day", "2 Days", "3 Days", "4 Days", "5 Days", "6 Days", "7 Days", "8 Days" };
		timeRangeCombo = new JComboBox<>(timeRanges);
		timeRangeCombo.setSelectedItem("1 Day");
		timeRangeCombo.addActionListener(e -> {
			if (!useCustomTimeRange) {
				handleTimeRangeChange();
			}
		});
		controlPanel.add(timeRangeCombo);

		// Refresh Interval
		controlPanel.add(new JLabel("Refresh Interval (s):"));
		Integer[] refreshIntervals = { 10, 30, 60, 120, 300 };
		refreshIntervalCombo = new JComboBox<>(refreshIntervals);
		refreshIntervalCombo.setSelectedItem(60);
		controlPanel.add(refreshIntervalCombo);

		// Live Update Buttons
		startLiveButton = new JButton("Start Live Updates");
		stopLiveButton = new JButton("Stop Live Updates");
		stopLiveButton.setEnabled(false);

		startLiveButton.addActionListener(e -> startLiveUpdates());
		stopLiveButton.addActionListener(e -> stopLiveUpdates());

		controlPanel.add(startLiveButton);
		controlPanel.add(stopLiveButton);

		// REPLACED: Volume Display Combo Box instead of Checkbox
		controlPanel.add(new JLabel("Display Option:"));
		String[] volumeOptions = { "Display Current Volumes", "Display Current Total Volumes",
				"PriceData Assigned Attributes", "No Tooltip Display" };
		displayOptionCombo = new JComboBox<>(volumeOptions);
		displayOptionCombo.setSelectedItem("PriceData Assigned Attributes"); // Default
		controlPanel.add(displayOptionCombo);

		return controlPanel;
	}

	private JPanel createSymbolManagementPanel() {
		JPanel symbolManagementPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JTextField addSymbolField = new JTextField(8);
		JButton addSymbolButton = new JButton("Add Symbol");

		symbolManagementPanel.add(new JLabel("Add Symbol:"));
		symbolManagementPanel.add(addSymbolField);
		symbolManagementPanel.add(addSymbolButton);

		addSymbolButton.addActionListener(e -> {
			String inputSymbols = addSymbolField.getText().trim().toUpperCase();
			if (inputSymbols.isEmpty()) {
				JOptionPane.showMessageDialog(null, "Please enter a symbol", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			handleAddSymbols(inputSymbols);
			addSymbolField.setText("");
		});

		return symbolManagementPanel;
	}

	private ChartPanel createChartPanel() {
		ChartPanel chartPanel = new ChartPanel(chart);
		JLabel volumeLabel = new JLabel();
		volumeLabel.setBackground(Color.WHITE);
		volumeLabel.setOpaque(true);
		volumeLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		chartPanel.add(volumeLabel);

		chartPanel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				String selectedDisplayMode = (String) displayOptionCombo.getSelectedItem();

				Point2D p = chartPanel.translateScreenToJava2D(e.getPoint());
				PlotRenderingInfo plotInfo = chartPanel.getChartRenderingInfo().getPlotInfo();
				XYPlot plot = chart.getXYPlot();
				Rectangle2D dataArea = plotInfo.getDataArea();
				if (!dataArea.contains(p)) {
					volumeLabel.setVisible(false);
					return;
				}

				double x = plot.getDomainAxis().java2DToValue(p.getX(), dataArea, plot.getDomainAxisEdge());
				Date nearestDate = new Date((long) x);
				ZonedDateTime nearestTime = ZonedDateTime.ofInstant(nearestDate.toInstant(), ZoneOffset.UTC);

				ZonedDateTime closestTime = null;
				long minDiff = Long.MAX_VALUE;
				for (Map<ZonedDateTime, Double[]> dataMap : symbolData.values()) {
					for (ZonedDateTime time : dataMap.keySet()) {
						long diff = Math.abs(time.toEpochSecond() - nearestTime.toEpochSecond());
						if (diff < minDiff) {
							minDiff = diff;
							closestTime = time;
						}
					}
				}

				if (closestTime == null) {
					volumeLabel.setVisible(false);
					return;
				}

				// Handle different display modes
				switch (selectedDisplayMode) {
				case "Display Current Volumes":
					showCurrentVolumes(volumeLabel, e, closestTime);
					break;
				case "Display Current Total Volumes":
					showCurrentTotalVolumes(volumeLabel, e, closestTime);
					break;
				case "PriceData Assigned Attributes":
					showPriceDataAttributes(volumeLabel, e, closestTime);
					break;
				case "No Tooltip Display":
					break;
				default:
					volumeLabel.setVisible(false);
				}

				plot.setDomainCrosshairValue(x);
				plot.setRangeCrosshairValue(
						plot.getRangeAxis().java2DToValue(p.getY(), dataArea, plot.getRangeAxisEdge()));
			}
		});

		return chartPanel;
	}

	/**
	 * Shows current volumes at the specific time point (original implementation)
	 */
	private void showCurrentVolumes(JLabel volumeLabel, MouseEvent e, ZonedDateTime closestTime) {
		StringBuilder volumeInfo = new StringBuilder("<html><b>Volumes at "
				+ new SimpleDateFormat("MM-dd HH:mm").format(Date.from(closestTime.toInstant())) + "</b><br>");

		for (String symbol : currentSymbols) {
			if (!symbolCheckboxes.get(symbol).isSelected())
				continue;
			Map<ZonedDateTime, Double[]> dataMap = symbolData.get(symbol);
			if (dataMap != null && dataMap.containsKey(closestTime)) {
				double volume = dataMap.get(closestTime)[1];
				volumeInfo.append(String.format("%s: %.0f<br>", symbol, volume));
			}
		}
		volumeInfo.append("</html>");
		volumeLabel.setText(volumeInfo.toString());
		volumeLabel.setVisible(true);
		volumeLabel.setBounds(e.getX() + 10, e.getY() - 10, 200, 20 + currentSymbols.size() * 20);
	}

	/**
	 * Shows cumulative total volumes from start time to the current cursor time
	 */
	private void showCurrentTotalVolumes(JLabel volumeLabel, MouseEvent e, ZonedDateTime cursorTime) {
		StringBuilder volumeInfo = new StringBuilder("<html><b>Total Volumes up to "
				+ new SimpleDateFormat("MM-dd HH:mm").format(Date.from(cursorTime.toInstant())) + "</b><br>");

		// Find the earliest time in the dataset
		ZonedDateTime startTime = findEarliestTime();

		if (startTime == null) {
			volumeInfo.append("No data available</html>");
			volumeLabel.setText(volumeInfo.toString());
			volumeLabel.setVisible(true);
			volumeLabel.setBounds(e.getX() + 10, e.getY() - 10, 200, 40);
			return;
		}

		for (String symbol : currentSymbols) {
			if (!symbolCheckboxes.get(symbol).isSelected())
				continue;
			Map<ZonedDateTime, Double[]> dataMap = symbolData.get(symbol);
			if (dataMap != null && !dataMap.isEmpty()) {
				double totalVolume = calculateTotalVolumeUpToTime(dataMap, cursorTime);
				volumeInfo.append(String.format("%s: %.0f<br>", symbol, totalVolume));
			}
		}

		// Add time range information
		volumeInfo.append(String.format("<br><i>From: %s</i>",
				new SimpleDateFormat("MM-dd HH:mm").format(Date.from(startTime.toInstant()))));
		volumeInfo.append("</html>");

		volumeLabel.setText(volumeInfo.toString());
		volumeLabel.setVisible(true);
		volumeLabel.setBounds(e.getX() + 10, e.getY() - 10, 250, 30 + currentSymbols.size() * 20);
	}

	/**
	 * Shows PriceData assigned attributes for the specific time point
	 */
	private void showPriceDataAttributes(JLabel infoLabel, MouseEvent e, ZonedDateTime closestTime) {
		if (priceDataMap == null || priceDataMap.isEmpty()) {
			infoLabel.setText("<html><b>No PriceData available</b></html>");
			infoLabel.setVisible(true);
			infoLabel.setBounds(e.getX() + 10, e.getY() - 10, 200, 40);
			return;
		}

		// Get selected columns from monteCarloConfig
		@SuppressWarnings("unchecked")
		List<String> selectedColumns = (List<String>) monteCarloConfig.get("selectedColumns");

		if (selectedColumns == null || selectedColumns.isEmpty()) {
			infoLabel.setText("<html><b>No columns selected</b><br>Configure in monteCarloConfig</html>");
			infoLabel.setVisible(true);
			infoLabel.setBounds(e.getX() + 10, e.getY() - 10, 250, 60);
			return;
		}

		// Get display configuration
		@SuppressWarnings("unchecked")
		Map<String, Object> displayConfig = (Map<String, Object>) monteCarloConfig
				.getOrDefault("priceDataDisplayConfig", new HashMap<>());

		boolean useDashboardCollector = (boolean) displayConfig.getOrDefault("useDashboardCollector", true);
		boolean includeZScore = (boolean) displayConfig.getOrDefault("includeZScore", true);
		boolean showSymbolHeader = (boolean) displayConfig.getOrDefault("showSymbolHeader", true);
		int maxColumnsPerSymbol = (int) displayConfig.getOrDefault("maxColumnsPerSymbol", 10);

		StringBuilder info = new StringBuilder("<html><b>PriceData Attributes at ");
		info.append(new SimpleDateFormat("MM-dd HH:mm").format(Date.from(closestTime.toInstant())));
		info.append("</b><br>");

		// Use DashboardColumnCollector to analyze and map the selected columns
		Map<String, com.quantlabs.stockApp.utils.DashboardColumnCollector.ColumnMapping> columnMappings = com.quantlabs.stockApp.utils.DashboardColumnCollector
				.analyzeColumns(selectedColumns);

		int symbolCount = 0;
		for (String symbol : currentSymbols) {
			if (!symbolCheckboxes.get(symbol).isSelected())
				continue;

			PriceData priceData = priceDataMap.get(symbol);
			if (priceData == null)
				continue;

			symbolCount++;

			if (showSymbolHeader) {
				info.append("<br><b>").append(symbol).append(":</b><br>");
			} else {
				info.append("<br>");
			}

			int columnCount = 0;
			// Display each selected column using the mapping
			for (String columnName : selectedColumns) {
				if (columnCount >= maxColumnsPerSymbol) {
					info.append("  ... (more columns)<br>");
					break;
				}

				com.quantlabs.stockApp.utils.DashboardColumnCollector.ColumnMapping mapping = columnMappings
						.get(columnName);

				if (mapping != null) {
					Object value = mapping.getValue(priceData);
					if (value != null) {
						info.append(String.format("  %s: %s<br>", columnName, formatValue(value)));
						columnCount++;
					}
				} else {
					// Fallback: try direct field access
					Object value = getFallbackColumnValue(priceData, columnName);
					if (value != null) {
						info.append(String.format("  %s: %s<br>", columnName, formatValue(value)));
						columnCount++;
					}
				}
			}

			// Add Z-Score information if configured
			/*
			 * if (includeZScore && priceData.getZScoreResults() != null &&
			 * !priceData.getZScoreResults().isEmpty()) {
			 * info.append("  <i>Z-Scores:</i><br>"); for (Map.Entry<String,
			 * com.quantlabs.stockApp.indicator.management.ZScoreCalculator.ZScoreResult>
			 * entry : priceData .getZScoreResults().entrySet()) { if
			 * (selectedColumns.contains(entry.getKey())) { info.append(
			 * String.format("    %s: %.1f<br>", entry.getKey(),
			 * entry.getValue().getOverallScore())); } } }
			 */

			// Limit the number of symbols shown
			if (symbolCount >= 5) { // Show max 5 symbols to avoid clutter
				info.append("<br><i>... and " + (getCheckedSymbols().size() - 5) + " more symbols</i>");
				break;
			}
		}

		if (symbolCount == 0) {
			info.append("<br><i>No symbols selected or no PriceData available</i>");
		}

		info.append("</html>");
		infoLabel.setText(info.toString());
		infoLabel.setVisible(true);

		// Calculate appropriate size based on content
		int lines = info.toString().split("<br>").length;
		int estimatedHeight = 20 + lines * 16;
		int maxHeight = 400; // Max height to avoid going off-screen
		infoLabel.setBounds(e.getX() + 10, e.getY() - 10, 350, Math.min(estimatedHeight, maxHeight));
	}

	/**
	 * Fallback method to get column value when mapping is not available
	 */
	private Object getFallbackColumnValue(PriceData priceData, String columnName) {
		// Handle common column patterns
		if (columnName.toLowerCase().contains("price")) {
			return priceData.getLatestPrice();
		} else if (columnName.toLowerCase().contains("volume") || columnName.toLowerCase().contains("vol")) {
			if (columnName.toLowerCase().contains("prev") || columnName.toLowerCase().contains("previous")) {
				return priceData.getPreviousVolume();
			} else {
				return priceData.getCurrentVolume();
			}
		} else if (columnName.toLowerCase().contains("change") || columnName.toLowerCase().contains("%")) {
			return priceData.getPercentChange();
		} else if (columnName.equalsIgnoreCase("Symbol") || columnName.equalsIgnoreCase("Ticker")) {
			return priceData.getTicker();
		}

		return null;
	}

	/**
	 * Format value for display
	 */
	private String formatValue(Object value) {
		if (value == null)
			return "N/A";

		if (value instanceof Double) {
			double doubleValue = (Double) value;
			// Special handling for percentage values
			if (value.toString().contains("%") || (Math.abs(doubleValue) < 1 && Math.abs(doubleValue) > 0.001)) {
				return String.format("%.2f%%", doubleValue * 100);
			}

			// Format based on magnitude
			if (Math.abs(doubleValue) >= 1000000) {
				return String.format("$%,.1fM", doubleValue / 1000000);
			} else if (Math.abs(doubleValue) >= 1000) {
				return String.format("$%,.0f", doubleValue);
			} else if (Math.abs(doubleValue) >= 1) {
				return String.format("$%.2f", doubleValue);
			} else {
				return String.format("%.4f", doubleValue);
			}
		} else if (value instanceof Long) {
			long longValue = (Long) value;
			if (longValue >= 1000000) {
				return String.format("%,.1fM", longValue / 1000000.0);
			} else if (longValue >= 1000) {
				return String.format("%,.1fK", longValue / 1000.0);
			} else {
				return String.format("%,d", longValue);
			}
		} else if (value instanceof Integer) {
			return String.format("%,d", (Integer) value);
		} else if (value instanceof Boolean) {
			return (Boolean) value ? "✓" : "✗";
		} else if (value instanceof String) {
			String strValue = (String) value;
			// Truncate long strings
			/*
			 * if (strValue.length() > 30) { return strValue.substring(0, 27) + "..."; }
			 */
			return strValue;
		} else {
			return value.toString();
		}
	}

	/**
	 * Find the earliest time across all symbol data
	 */
	private ZonedDateTime findEarliestTime() {
		ZonedDateTime earliest = null;
		for (Map<ZonedDateTime, Double[]> dataMap : symbolData.values()) {
			for (ZonedDateTime time : dataMap.keySet()) {
				if (earliest == null || time.isBefore(earliest)) {
					earliest = time;
				}
			}
		}
		return earliest;
	}

	/**
	 * Calculate total volume for a symbol from start to the specified time
	 */
	private double calculateTotalVolumeUpToTime(Map<ZonedDateTime, Double[]> dataMap, ZonedDateTime upToTime) {
		double totalVolume = 0;

		// Sort the times to ensure we process in chronological order
		List<ZonedDateTime> sortedTimes = new ArrayList<>(dataMap.keySet());
		Collections.sort(sortedTimes);

		for (ZonedDateTime time : sortedTimes) {
			// Only include volumes up to and including the cursor time
			if (!time.isAfter(upToTime)) {
				Double[] data = dataMap.get(time);
				if (data != null && data.length > 1 && data[1] != null) {
					totalVolume += data[1];
				}
			} else {
				// We've passed the cursor time, stop accumulating
				break;
			}
		}

		return totalVolume;
	}

	private void initializeCheckboxPanel() {
		checkboxPanel.removeAll();
		symbolCheckboxes.clear();

		for (String symbol : currentSymbols) {
			addSymbolToCheckboxPanel(symbol);
		}

		checkboxPanel.revalidate();
		checkboxPanel.repaint();
	}

	private void addSimpleSessionMarkers() {
		if (plot == null)
			return;

		// Clear existing domain markers
		plot.clearDomainMarkers();

		// EST time zone
		ZoneId estZone = ZoneId.of("America/Los_Angeles");

		// Define session times in EST (4AM, 6:30AM, 1PM, 5PM)
		LocalTime[] sessionTimes = { LocalTime.of(1, 0), LocalTime.of(4, 0), // 4:00 AM EST
				LocalTime.of(6, 30), // 6:30 AM EST
				LocalTime.of(13, 0), // 1:00 PM EST
				LocalTime.of(17, 0) // 5:00 PM EST
		};

		// Get the current date range from the chart
		DateRange range = (DateRange) plot.getDomainAxis().getRange();
		if (range == null) {
			System.out.println("No date range available yet");
			return;
		}

		long startMillis = (long) range.getLowerBound();
		long endMillis = (long) range.getUpperBound();

		System.out.println("Chart range: " + new Date(startMillis) + " to " + new Date(endMillis));

		// Convert to EST for day calculation
		ZonedDateTime startEst = ZonedDateTime.ofInstant(Instant.ofEpochMilli(startMillis), estZone);
		ZonedDateTime endEst = ZonedDateTime.ofInstant(Instant.ofEpochMilli(endMillis), estZone);

		// Start from the beginning of the start day
		ZonedDateTime currentDay = startEst.toLocalDate().atStartOfDay(estZone);

		// Create markers for each day in the visible range
		while (!currentDay.isAfter(endEst)) {
			for (LocalTime sessionTime : sessionTimes) {
				ZonedDateTime markerDateTime = currentDay.with(sessionTime);
				long markerMillis = markerDateTime.toInstant().toEpochMilli();

				// Only add if within visible range
				if (markerMillis >= startMillis && markerMillis <= endMillis) {
					ValueMarker marker = new ValueMarker(markerMillis);
					marker.setPaint(new Color(255, 0, 0, 150)); // Semi-transparent red

					// Create dotted line stroke
					float[] dashPattern = { 5.0f, 5.0f }; // 5px dash, 5px gap
					BasicStroke dottedStroke = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f,
							dashPattern, 0.0f);
					marker.setStroke(dottedStroke);

					plot.addDomainMarker(marker);

					System.out.println("Added marker: " + markerDateTime + " (" + markerMillis + ")");
				}
			}

			// Move to next day
			currentDay = currentDay.plusDays(1);
		}

		// Force chart refresh
		chart.fireChartChanged();
		System.out.println("Session markers added successfully");

	}

	private void addSymbolToCheckboxPanel(String symbol) {
		JCheckBox checkbox = new JCheckBox(symbol, true);
		checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel symbolPanel = new JPanel(new BorderLayout());
		symbolPanel.setMaximumSize(new Dimension(170, 30));
		symbolPanel.setPreferredSize(new Dimension(170, 30));
		symbolPanel.add(checkbox, BorderLayout.CENTER);

		if (currentSymbols.size() > 1) {
			JButton removeButton = new JButton("X");
			removeButton.setPreferredSize(new Dimension(25, 20));
			removeButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
			removeButton.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 10));
			removeButton.addActionListener(e -> removeSymbol(symbol));
			symbolPanel.add(removeButton, BorderLayout.EAST);
		}

		checkboxPanel.add(symbolPanel);
		symbolCheckboxes.put(symbol, checkbox);

		// FIXED: Simplified checkbox action listener for visibility
		checkbox.addActionListener(e -> {
			updateChartSeries(); // Rebuild chart based on checkbox states
		});
	}

	private void saveTimeRangeConfiguration() {
		// Save the radio button selection
		useCustomTimeRange = timeRangeRadio.isSelected();

		if (!useCustomTimeRange) {
			// Save the selected time range from dialog combo box
			if (dialogTimeRangeCombo != null) {
				String selectedRange = (String) dialogTimeRangeCombo.getSelectedItem();
				// Also update the main control panel combo box if it exists
				if (timeRangeCombo != null) {
					timeRangeCombo.setSelectedItem(selectedRange);
				}
			}
			// Current time mode always uses 1Min timeframe
			currentTimeframe = "1Min";
		} else {
			// Save custom date and time values
			if (startDatePicker != null && endDatePicker != null) {
				savedStartDate = startDatePicker.getDate();
				savedEndDate = endDatePicker.getDate();

				savedStartHour = (String) startHourCombo.getSelectedItem();
				savedStartMinute = (String) startMinuteCombo.getSelectedItem();
				savedEndHour = (String) endHourCombo.getSelectedItem();
				savedEndMinute = (String) endMinuteCombo.getSelectedItem();
			}

			// Save selected timeframe
			if (timeframeCombo != null) {
				currentTimeframe = (String) timeframeCombo.getSelectedItem();
			}
		}

		System.out.println("Time range configuration saved:");
		System.out.println("  Custom Time Range: " + useCustomTimeRange);
		if (!useCustomTimeRange) {
			System.out.println("  Time Range: " + dialogTimeRangeCombo.getSelectedItem());
			System.out.println("  Timeframe: 1Min (Fixed)");
		} else {
			System.out.println("  Start: " + savedStartDate + " " + savedStartHour + ":" + savedStartMinute);
			System.out.println("  End: " + savedEndDate + " " + savedEndHour + ":" + savedEndMinute);
			System.out.println("  Timeframe: " + currentTimeframe);
		}
	}

	private void updateCheckboxPanel() {
		if (checkboxPanel == null) {
			System.out.println("Checkbox panel not initialized yet, skipping update");
			return;
		}

		checkboxPanel.removeAll();
		symbolCheckboxes.clear();

		for (String symbol : currentSymbols) {
			addSymbolToCheckboxPanel(symbol);
		}

		if (currentSymbols.size() > 15) {
			checkboxPanel.add(Box.createVerticalStrut(10));
		}

		checkboxPanel.revalidate();
		checkboxPanel.repaint();

		if (checkboxScrollPane != null) {
			checkboxScrollPane.revalidate();
			checkboxScrollPane.repaint();

			SwingUtilities.invokeLater(() -> {
				checkboxScrollPane.getVerticalScrollBar().setValue(0);
			});
		}
	}

	// Data source management methods
	private void updateDataSourceCombo() {
		dataSourceCombo.removeAllItems();
		List<String> availableSources = dataSourceManager.getAvailableDataSources();
		for (String source : availableSources) {
			dataSourceCombo.addItem(source);
		}
		dataSourceCombo.setSelectedItem(dataSourceManager.getCurrentDataSourceName());
	}

	private void switchDataSource(String sourceName) {
		try {
			dataSourceManager.setDataSource(sourceName);
			onDataSourceChanged(sourceName);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Failed to switch to " + sourceName + ": " + e.getMessage(),
					"Data Source Error", JOptionPane.ERROR_MESSAGE);
			updateDataSourceCombo();
		}
	}

	public void onDataSourceChanged(String newSource) {
		if (frame != null) {
			frame.setTitle("Monte Carlo Simulation - " + newSource + " " + titleName);
		}
		if (chart != null) {
			String selectedTimeRange = (String) timeRangeCombo.getSelectedItem();
			chart.setTitle("Monte Carlo Simulation - Cumulative Returns (1min) - " + selectedTimeRange + " - "
					+ newSource + " " + titleName);
		}
		refreshData();
	}

	// Symbol management methods
	private void handleAddSymbols(String inputSymbols) {
		String[] symbolArray = inputSymbols.split(",");
		List<String> newSymbols = new ArrayList<>();
		List<String> duplicateSymbols = new ArrayList<>();

		for (String symbol : symbolArray) {
			String cleanSymbol = symbol.trim();
			if (!cleanSymbol.isEmpty()) {
				if (currentSymbols.contains(cleanSymbol)) {
					duplicateSymbols.add(cleanSymbol);
				} else {
					newSymbols.add(cleanSymbol);
				}
			}
		}

		if (newSymbols.isEmpty()) {
			if (!duplicateSymbols.isEmpty()) {
				JOptionPane.showMessageDialog(null,
						"All symbols are already being tracked: " + String.join(", ", duplicateSymbols), "Info",
						JOptionPane.INFORMATION_MESSAGE);
			}
			return;
		}

		if (!duplicateSymbols.isEmpty()) {
			JOptionPane.showMessageDialog(null, "The following symbols are already being tracked and will be skipped: "
					+ String.join(", ", duplicateSymbols), "Duplicate Symbols", JOptionPane.WARNING_MESSAGE);
		}

		currentSymbols.addAll(newSymbols);
		loadSymbolsData(newSymbols);
	}

	private void loadSymbolsData(List<String> symbolsToLoad) {
		new SwingWorker<Map<String, Map<ZonedDateTime, Double[]>>, Void>() {
			@Override
			protected Map<String, Map<ZonedDateTime, Double[]>> doInBackground() {
				try {
					if (useCustomTimeRange) {
						// Use custom time range settings
						return fetchDataWithCustomTimeRange(symbolsToLoad);
					} else {
						// Use current time with selected time range
						return fetchDataWithCurrentTime(symbolsToLoad);
					}
				} catch (Exception e) {
					System.err.println("Error loading symbols data: " + e.getMessage());
					return new HashMap<>();
				}
			}

			@Override
			protected void done() {
				try {
					Map<String, Map<ZonedDateTime, Double[]>> result = get();
					List<String> successfullyAdded = new ArrayList<>();
					List<String> failedSymbols = new ArrayList<>();

					for (String symbol : symbolsToLoad) {
						if (result.containsKey(symbol) && !result.get(symbol).isEmpty()) {
							successfullyAdded.add(symbol);
							symbolData.put(symbol, result.get(symbol));
						} else {
							failedSymbols.add(symbol);
							currentSymbols.remove(symbol);
						}
					}

					updateCheckboxPanel();
					updateChartSeries();

					StringBuilder message = new StringBuilder();
					if (!successfullyAdded.isEmpty()) {
						message.append("Successfully added: ").append(String.join(", ", successfullyAdded));
						if (useCustomTimeRange) {
							message.append("\nUsing custom time range: ");
							message.append(new SimpleDateFormat("yyyy-MM-dd").format(savedStartDate));
							message.append(" ").append(savedStartHour).append(":").append(savedStartMinute);
							message.append(" to ");
							message.append(new SimpleDateFormat("yyyy-MM-dd").format(savedEndDate));
							message.append(" ").append(savedEndHour).append(":").append(savedEndMinute);
							message.append(" | Timeframe: ").append(currentTimeframe);
						} else {
							String selectedRange = timeRangeCombo != null ? (String) timeRangeCombo.getSelectedItem()
									: "1 Day";
							message.append("\nUsing time range: ").append(selectedRange);
							message.append(" | Timeframe: 1Min");
						}
					}
					if (!failedSymbols.isEmpty()) {
						if (message.length() > 0)
							message.append("\n\n");
						message.append("No data found for: ").append(String.join(", ", failedSymbols));
					}

					if (!successfullyAdded.isEmpty() || !failedSymbols.isEmpty()) {
						JOptionPane.showMessageDialog(null, message.toString(), "Add Symbols Result",
								JOptionPane.INFORMATION_MESSAGE);
					}

				} catch (Exception ex) {
					currentSymbols.removeAll(symbolsToLoad);
					JOptionPane.showMessageDialog(null, "Error adding symbols: " + ex.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	private Map<String, Map<ZonedDateTime, Double[]>> fetchDataWithCustomTimeRange(List<String> symbols) {
		try {
			// Use saved custom time range values
			Date startDate = savedStartDate;
			Date endDate = savedEndDate;

			int startHour = Integer.parseInt(savedStartHour);
			int startMinute = Integer.parseInt(savedStartMinute);
			int endHour = Integer.parseInt(savedEndHour);
			int endMinute = Integer.parseInt(savedEndMinute);

			// Create ZonedDateTime objects
			ZonedDateTime start = ZonedDateTime.ofInstant(startDate.toInstant(), ZoneId.systemDefault())
					.withHour(startHour).withMinute(startMinute).withSecond(0).withZoneSameInstant(ZoneOffset.UTC);

			ZonedDateTime end = ZonedDateTime.ofInstant(endDate.toInstant(), ZoneId.systemDefault()).withHour(endHour)
					.withMinute(endMinute).withSecond(0).withZoneSameInstant(ZoneOffset.UTC);

			System.out.println("Fetching data with custom time range for symbols: " + symbols);
			System.out.println("Start: " + start);
			System.out.println("End: " + end);
			System.out.println("Timeframe: " + currentTimeframe);

			// Use custom timeframe for custom time range mode
			return fetchDataWithTimeframe(symbols, start, end, currentTimeframe, useCustomTimeRange);
		} catch (Exception e) {
			System.err.println("Error in custom time range fetch: " + e.getMessage());
			return new HashMap<>();
		}
	}

	private Map<String, Map<ZonedDateTime, Double[]>> fetchDataWithCurrentTime(List<String> symbols) {
		try {
			String selected = timeRangeCombo != null ? (String) timeRangeCombo.getSelectedItem() : "1 Day";
			int days = Integer.parseInt(selected.split(" ")[0]);
			ZonedDateTime end = ZonedDateTime.now(ZoneId.of("America/New_York"));
			ZonedDateTime start = calculateStartTime(selected, end);
			start = start.withZoneSameInstant(ZoneOffset.UTC);
			end = end.withZoneSameInstant(ZoneOffset.UTC);

			System.out.println("Fetching data with current time range for symbols: " + symbols);
			System.out.println("Time Range: " + selected);
			System.out.println("Start: " + start);
			System.out.println("End: " + end);
			System.out.println("Timeframe: 1Min (Fixed)");

			// Current time mode always uses 1Min timeframe
			return fetchDataWithTimeframe(symbols, start, end, "1Min", useCustomTimeRange);
		} catch (Exception e) {
			System.err.println("Error in current time fetch: " + e.getMessage());
			return new HashMap<>();
		}
	}

	/*
	 * private boolean isTimeframeSupported(String timeframe) { // List of supported
	 * timeframes - adjust based on your data providers String[] supportedTimeframes
	 * = {"1Min", "5Min", "15Min", "30Min", "1Hour", "4Hour", "1Day", "1Week"};
	 * return Arrays.asList(supportedTimeframes).contains(timeframe); }
	 */

	private Map<String, Map<ZonedDateTime, Double[]>> fetchDataWithTimeframe(List<String> symbols, ZonedDateTime start,
			ZonedDateTime end, String timeframe, boolean useCustomTimeRange2) {
		try {
			/*
			 * if (!isTimeframeSupported(timeframe)) { System.out.println("Timeframe " +
			 * timeframe + " not supported, falling back to 1Min"); timeframe = "1Min"; }
			 */

			System.out.println("Using timeframe: " + timeframe);
			return dataSourceManager.fetchData(symbols, start, end, timeframe, useCustomTimeRange2);
		} catch (Exception e) {
			System.err.println("Error fetching data with timeframe " + timeframe + ": " + e.getMessage());
			// Fallback to 1Min if there's an error
			try {
				System.out.println("Falling back to 1Min timeframe due to error");
				return dataSourceManager.fetchData(symbols, start, end, "1Min", useCustomTimeRange2);
			} catch (Exception fallbackError) {
				System.err.println("Error with fallback timeframe: " + fallbackError.getMessage());
				return new HashMap<>();
			}
		}
	}

	private void removeSymbol(String symbol) {
		if (currentSymbols.size() <= 1) {
			JOptionPane.showMessageDialog(null, "Cannot remove the last symbol", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to remove " + symbol + "?",
				"Remove Symbol", JOptionPane.YES_NO_OPTION);

		if (confirm == JOptionPane.YES_OPTION) {
			currentSymbols.remove(symbol);
			symbolData.remove(symbol);

			TimeSeries series = symbolSeriesMap.get(symbol);
			if (series != null) {
				dataset.removeSeries(series);
				symbolSeriesMap.remove(symbol);
			}

			updateCheckboxPanel();
			JOptionPane.showMessageDialog(null, "Symbol " + symbol + " removed", "Success",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	// Time range and live update methods
	private void handleTimeRangeChange() {
		String selected = (String) timeRangeCombo.getSelectedItem();
		refreshData();
	}

	private void startLiveUpdates() {
		int interval = (Integer) refreshIntervalCombo.getSelectedItem();
		startLiveButton.setEnabled(false);
		stopLiveButton.setEnabled(true);
		refreshIntervalCombo.setEnabled(false);

		montecarloLiveUpdateScheduler = Executors.newSingleThreadScheduledExecutor();
		montecarloLiveUpdateScheduler.scheduleAtFixedRate(() -> {
			SwingUtilities.invokeLater(() -> {
				try {
					refreshData();
				} catch (Exception ex) {
					System.out.println("Error during live update: " + ex.getMessage());
				}
			});
		}, 0, interval, TimeUnit.SECONDS);
	}

	private void stopLiveUpdates() {
		if (montecarloLiveUpdateScheduler != null) {
			montecarloLiveUpdateScheduler.shutdown();
			try {
				if (!montecarloLiveUpdateScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					montecarloLiveUpdateScheduler.shutdownNow();
				}
			} catch (InterruptedException ex) {
				montecarloLiveUpdateScheduler.shutdownNow();
			}
		}
		startLiveButton.setEnabled(true);
		stopLiveButton.setEnabled(false);
		refreshIntervalCombo.setEnabled(true);
	}

	private void refreshData() {
		SwingUtilities.invokeLater(() -> {
			new SwingWorker<Map<String, Map<ZonedDateTime, Double[]>>, Void>() {
				@Override
				protected Map<String, Map<ZonedDateTime, Double[]>> doInBackground() {
					try {
						if (useCustomTimeRange) {
							// Use custom time range
							return fetchDataWithCustomTimeRange(getCheckedSymbols());
						} else {
							// Use current time with selected range
							return fetchDataWithCurrentTime(getCheckedSymbols());
						}
					} catch (Exception e) {
						System.err.println("Error refreshing data: " + e.getMessage());
						return new HashMap<>();
					}
				}

				@Override
				protected void done() {
					try {
						Map<String, Map<ZonedDateTime, Double[]>> result = get();
						symbolData.putAll(result);
						updateChartSeries();

						// Add session time markers
						addSimpleSessionMarkers();

						// Update chart title based on current mode
						updateChartTitle();

					} catch (Exception ex) {
						System.out.println("Error updating graph: " + ex.getMessage());
					}
				}
			}.execute();
		});
	}

	private List<String> getCheckedSymbols() {
		List<String> checkedSymbols = new ArrayList<>();
		for (String symbol : currentSymbols) {
			JCheckBox checkbox = symbolCheckboxes.get(symbol);
			if (checkbox != null && checkbox.isSelected()) {
				checkedSymbols.add(symbol);
			}
		}
		return checkedSymbols;
	}

	private void updateChartTitle() {
		String currentSource = dataSourceManager.getCurrentDataSourceName();
		String title;

		if (useCustomTimeRange) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			String startStr = dateFormat.format(savedStartDate) + " " + savedStartHour + ":" + savedStartMinute;
			String endStr = dateFormat.format(savedEndDate) + " " + savedEndHour + ":" + savedEndMinute;
			title = "Monte Carlo Simulation - " + currentTimeframe + " - Custom Range: " + startStr + " to " + endStr
					+ " - " + currentSource;
		} else {
			String selectedRange = timeRangeCombo != null ? (String) timeRangeCombo.getSelectedItem() : "1 Day";
			title = "Monte Carlo Simulation - 1Min - " + selectedRange + " - " + currentSource;
		}

		chart.setTitle(title);

		// Also update frame title if needed
		if (frame != null) {
			frame.setTitle(titleName + " Monte Carlo Simulation - " + currentSource);
		}
	}

	private ZonedDateTime calculateStartTime(String selected, ZonedDateTime end) {
		if (selected.equals("1 Day")) {
			LocalTime currentTime = end.toLocalTime();
			LocalTime preMarketOpen = LocalTime.of(4, 0);
			ZonedDateTime todayPreMarketOpen = end.with(preMarketOpen);
			if (currentTime.isBefore(preMarketOpen) || end.getDayOfWeek().getValue() >= 6) {
				ZonedDateTime start = todayPreMarketOpen.minusDays(1);
				while (start.getDayOfWeek().getValue() >= 6) {
					start = start.minusDays(1);
				}
				return start;
			} else {
				return todayPreMarketOpen;
			}
		} else {
			int days = Integer.parseInt(selected.split(" ")[0]);
			return end.minusDays(days);
		}
	}

	// Update the updateChartSeries method to use custom thickness and colors
	private void updateChartSeries() {
		// Clear the dataset completely
		dataset.removeAllSeries();
		symbolSeriesIndices.clear();

		int seriesIndex = 0;
		boolean hasVisibleSeries = false;

		for (String symbol : currentSymbols) {
			Map<ZonedDateTime, Double[]> dataMap = symbolData.get(symbol);
			if (dataMap == null || dataMap.isEmpty())
				continue;

			JCheckBox checkbox = symbolCheckboxes.get(symbol);
			if (checkbox == null) {
				// Create checkbox if it doesn't exist (shouldn't happen, but safety check)
				System.out.println("Warning: Checkbox not found for symbol: " + symbol);
				continue;
			}

			// Only add to chart if checkbox is selected AND we have data
			if (checkbox.isSelected()) {
				TimeSeries series = new TimeSeries(symbol);

				// Add all data points to the series
				for (Map.Entry<ZonedDateTime, Double[]> dataEntry : dataMap.entrySet()) {
					Date date = Date.from(dataEntry.getKey().toInstant());
					Double cumulativeReturn = dataEntry.getValue()[0];
					if (cumulativeReturn != null && !Double.isNaN(cumulativeReturn)) {
						series.addOrUpdate(new Minute(date), cumulativeReturn);
					}
				}

				if (series.getItemCount() > 0) {
					dataset.addSeries(series);

					// Apply styles - primary symbols take precedence
					Float thickness = primarySymbolThickness.get(symbol);
					if (thickness == null) {
						thickness = symbolThickness.get(symbol);
					}
					if (thickness == null) {
						// Default thickness: 3.0 for primary symbols, 1.5 for regular
						thickness = primarySymbols.contains(symbol) ? 3.0f : 1.5f;
					}
					renderer.setSeriesStroke(seriesIndex, new BasicStroke(thickness));

					// Apply colors - primary symbols take precedence
					Color seriesColor = primarySymbolColors.get(symbol);
					if (seriesColor == null) {
						seriesColor = symbolColors.get(symbol);
					}
					if (seriesColor != null) {
						renderer.setSeriesPaint(seriesIndex, seriesColor);
					} else {
						// Default color: black for primary symbols, distinct color for regular
						seriesColor = primarySymbols.contains(symbol) ? Color.BLACK : getDistinctColor(seriesIndex);
						renderer.setSeriesPaint(seriesIndex, seriesColor);
					}

					// Store the series index
					symbolSeriesIndices.put(symbol, seriesIndex);
					seriesIndex++;
					hasVisibleSeries = true;
				} else {
					System.out.println("No valid data points for symbol: " + symbol);
				}
			} else {
				System.out.println("Skipping symbol (unchecked): " + symbol);
			}
		}

		// Force chart refresh
		plot.setDataset(dataset);
		chart.fireChartChanged();

		// ADD THIS: Add markers AFTER the chart is updated and rendered
		SwingUtilities.invokeLater(() -> {
			try {
				Thread.sleep(100); // Small delay to ensure chart is rendered
				addSimpleSessionMarkers();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private Color getDistinctColor(int index) {
		Color[] colors = { Color.YELLOW, Color.BLUE, Color.RED, Color.GREEN, Color.MAGENTA, Color.ORANGE, Color.CYAN,
				Color.PINK, Color.BLACK };
		return colors[index % colors.length];
	}

	// Public methods for controller
	public void updateSymbols(List<String> newSymbols) {
		// this.currentSymbols.clear();
		// this.currentSymbols.addAll(newSymbols);
		this.currentSymbols = newSymbols;
		updateCheckboxPanel();
		refreshData();
	}

	public List<String> getCurrentSymbols() {
		return currentSymbols;
	}

	public void setCurrentSymbols(List<String> currentSymbols) {
		this.currentSymbols = currentSymbols;
	}

	public JComboBox<String> getDataSourceCombo() {
		return dataSourceCombo;
	}

	// Add these public methods to MonteCarloGraphUI.java

	/**
	 * Public method to trigger time range configuration
	 */
	public void showTimeRangeConfiguration() {
		// This will show the configuration dialog
		configureTimeRange(); // This calls the private method we already created
	}

	/**
	 * Check if custom time range is enabled
	 */
	public boolean isCustomTimeRangeEnabled() {
		return useCustomTimeRange;
	}

	/**
	 * Set custom time range programmatically
	 */
	public void setCustomTimeRange(ZonedDateTime start, ZonedDateTime end) {
		this.useCustomTimeRange = true;

		// Update the date pickers if they exist
		if (startDatePicker != null && endDatePicker != null) {
			startDatePicker.setDate(Date.from(start.toInstant()));
			endDatePicker.setDate(Date.from(end.toInstant()));

			// Update time combos
			startHourCombo.setSelectedItem(String.format("%02d", start.getHour()));
			startMinuteCombo.setSelectedItem(String.format("%02d", start.getMinute()));
			endHourCombo.setSelectedItem(String.format("%02d", end.getHour()));
			endMinuteCombo.setSelectedItem(String.format("%02d", end.getMinute()));
		}

		// Update control panel state
		updateControlPanelState(true);

		// Refresh data with custom time range
		refreshDataWithCustomTimeRange();
	}

	/**
	 * Set custom time range programmatically
	 */
	public void setCustomTimeRange(ZonedDateTime start, ZonedDateTime end, String timeframe) {
		this.useCustomTimeRange = true;
		this.currentTimeframe = timeframe != null ? timeframe : "1Min";

		// Update the saved values
		this.savedStartDate = Date.from(start.toInstant());
		this.savedEndDate = Date.from(end.toInstant());

		// Set default times if not provided
		if (start.getHour() == 0 && start.getMinute() == 0) {
			this.savedStartHour = "09";
			this.savedStartMinute = "30";
		} else {
			this.savedStartHour = String.format("%02d", start.getHour());
			this.savedStartMinute = String.format("%02d", start.getMinute());
		}

		if (end.getHour() == 0 && end.getMinute() == 0) {
			this.savedEndHour = "16";
			this.savedEndMinute = "00";
		} else {
			this.savedEndHour = String.format("%02d", end.getHour());
			this.savedEndMinute = String.format("%02d", end.getMinute());
		}

		// Update control panel state
		updateControlPanelState(true);

		// Refresh data with custom time range
		refreshDataWithCustomTimeRange();
	}

	/**
	 * Switch to current time mode with specific time range
	 */
	public void setCurrentTimeMode(String timeRange) {
		this.useCustomTimeRange = false;

		// Update the time range combo if provided
		if (timeRange != null && timeRangeCombo != null) {
			timeRangeCombo.setSelectedItem(timeRange);
		}

		// Update control panel state
		updateControlPanelState(false);

		// Refresh data with current time
		refreshData();
	}

	/**
	 * Get the main frame for window listener attachment
	 */
	public ChartFrame getFrame() {
		return frame;
	}

	/**
	 * Add window listener to the main frame
	 */
	public void addWindowListener(java.awt.event.WindowListener listener) {
		if (frame != null) {
			frame.addWindowListener(listener);
		}
	}

	/**
	 * Remove window listener from the main frame
	 */
	public void removeWindowListener(java.awt.event.WindowListener listener) {
		if (frame != null) {
			frame.removeWindowListener(listener);
		}
	}

	public boolean checkIfClosed() {
		if (frame != null) {
			if (!frame.isDisplayable()) {
				System.out.println("Window is CLOSED");
				return true;
			} else {
				System.out.println("Window is OPEN");
				System.out.println("Visible: " + frame.isVisible());
				System.out.println("Displayable: " + frame.isDisplayable());
				return false;
			}
		} else {
			System.out.println("Frame reference is null");
			return true;
		}
	}

	/**
	 * Set custom thickness for a symbol
	 */
	public void setSymbolThickness(String symbol, float thickness) {
		String upperSymbol = symbol.toUpperCase();
		symbolThickness.put(upperSymbol, thickness);
		updateSymbolThickness(upperSymbol, thickness);
	}

	/**
	 * Set custom color for a symbol
	 */
	public void setSymbolColor(String symbol, Color color) {
		String upperSymbol = symbol.toUpperCase();
		symbolColors.put(upperSymbol, color);
		updateSymbolColor(upperSymbol, color);
	}

	/**
	 * Set multiple symbol thicknesses at once
	 */
	public void setSymbolThicknesses(Map<String, Float> thicknesses) {
		symbolThickness.clear();
		for (Map.Entry<String, Float> entry : thicknesses.entrySet()) {
			String symbol = entry.getKey().toUpperCase();
			symbolThickness.put(symbol, entry.getValue());
			updateSymbolThickness(symbol, entry.getValue());
		}
	}

	/**
	 * Set multiple symbol colors at once
	 */
	public void setSymbolColors(Map<String, Color> colors) {
		symbolColors.clear();
		for (Map.Entry<String, Color> entry : colors.entrySet()) {
			String symbol = entry.getKey().toUpperCase();
			symbolColors.put(symbol, entry.getValue());
			updateSymbolColor(symbol, entry.getValue());
		}
	}

	/**
	 * Clear all symbol thickness settings
	 */
	public void clearSymbolThickness() {
		symbolThickness.clear();
		updateChartSeries(); // Refresh to use default thickness
	}

	/**
	 * Clear all symbol colors
	 */
	public void clearSymbolColors() {
		symbolColors.clear();
		updateChartSeries(); // Refresh to use default colors
	}

	/**
	 * Update thickness for a specific symbol
	 */
	private void updateSymbolThickness(String symbol, float thickness) {
		if (renderer == null)
			return;

		Integer seriesIndex = symbolSeriesIndices.get(symbol);
		if (seriesIndex != null) {
			renderer.setSeriesStroke(seriesIndex, new BasicStroke(thickness));
			chart.fireChartChanged();
		}
	}

	/**
	 * Update color for a specific symbol
	 */
	private void updateSymbolColor(String symbol, Color color) {
		if (renderer == null || color == null)
			return;

		Integer seriesIndex = symbolSeriesIndices.get(symbol);
		if (seriesIndex != null) {
			renderer.setSeriesPaint(seriesIndex, color);
			chart.fireChartChanged();
		}
	}

	/**
	 * Reset all line styles to default
	 */
	private void resetLineStyles() {
		int result = JOptionPane.showConfirmDialog(frame,
				"Are you sure you want to reset ALL line thickness and colors to default?\n\n" + "This will reset:\n"
						+ "- Regular symbols: thickness 1.5, distinct colors\n"
						+ "- Primary symbols: thickness 3.0, black color",
				"Reset All Line Styles", JOptionPane.YES_NO_OPTION);

		if (result == JOptionPane.YES_OPTION) {
			// Clear regular symbols styles
			symbolThickness.clear();
			symbolColors.clear();

			// Reset primary symbols to defaults
			for (String symbol : primarySymbols) {
				primarySymbolThickness.put(symbol, 3.0f);
				primarySymbolColors.put(symbol, Color.BLACK);
			}

			updateChartSeries();
			JOptionPane.showMessageDialog(frame, "All line styles reset to default", "Reset Complete",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/**
	 * Helper method to get contrasting text color
	 */
	private Color getContrastColor(Color color) {
		double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
		return luminance > 0.5 ? Color.BLACK : Color.WHITE;
	}

	public void setPrimarySymbols(Set<String> primarySymbols) {
		this.primarySymbols = primarySymbols != null ? new HashSet<>(primarySymbols) : new HashSet<>();
		ensurePrimarySymbolsInCurrentList();

		// If the UI is already created, update the chart
		if (plot != null) {
			updateChartSeries();
		}
	}

	public Set<String> getPrimarySymbols() {
		return new HashSet<>(primarySymbols);
	}

	private void ensurePrimarySymbolsInCurrentList() {
		for (String primarySymbol : primarySymbols) {
			if (!currentSymbols.contains(primarySymbol)) {
				currentSymbols.add(primarySymbol);
				System.out.println("Added primary symbol to current list: " + primarySymbol);
			}
		}

		// Only update the checkbox panel if it's already initialized
		if (checkboxPanel != null) {
			updateCheckboxPanel();
		}
	}

	private void configurePrimarySymbols() {
		JDialog dialog = new JDialog(frame, "Primary Symbols Configuration", true);
		dialog.setLayout(new BorderLayout(10, 10));
		dialog.setPreferredSize(new Dimension(600, 500));

		// Main content panel
		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

		// Primary symbols input panel
		JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
		inputPanel.setBorder(BorderFactory.createTitledBorder("Primary Symbols Definition"));

		JLabel instructionLabel = new JLabel("Enter primary symbols (comma separated):");
		JTextField symbolsField = new JTextField();

		// Set current primary symbols
		String currentPrimaryText = String.join(", ", primarySymbols);
		symbolsField.setText(currentPrimaryText);

		inputPanel.add(instructionLabel, BorderLayout.NORTH);
		inputPanel.add(symbolsField, BorderLayout.CENTER);

		// Style configuration panel
		JPanel stylePanel = createPrimarySymbolsStylePanel();

		// Button panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton applyButton = new JButton("Apply");
		JButton cancelButton = new JButton("Cancel");

		applyButton.addActionListener(e -> {
			String symbolsText = symbolsField.getText().trim();
			if (!symbolsText.isEmpty()) {
				Set<String> newPrimarySymbols = parseSymbolsFromText(symbolsText);
				setPrimarySymbols(newPrimarySymbols);

				// Apply the styles from the style panel
				applyPrimarySymbolsStyles();

				dialog.dispose();

				JOptionPane.showMessageDialog(frame,
						"Primary symbols configuration applied:\n" + "Symbols: " + String.join(", ", newPrimarySymbols)
								+ "\n" + "Default thickness: 3.0\n" + "Default color: Black",
						"Configuration Applied", JOptionPane.INFORMATION_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(dialog, "Please enter at least one symbol", "Input Required",
						JOptionPane.WARNING_MESSAGE);
			}
		});

		cancelButton.addActionListener(e -> {
			dialog.dispose();
		});

		buttonPanel.add(cancelButton);
		buttonPanel.add(applyButton);

		// Add components to main panel
		mainPanel.add(inputPanel, BorderLayout.NORTH);
		mainPanel.add(stylePanel, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		dialog.add(mainPanel);
		dialog.pack();
		dialog.setLocationRelativeTo(frame);
		dialog.setVisible(true);
	}

	private JPanel createPrimarySymbolsStylePanel() {
		JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.setBorder(BorderFactory.createTitledBorder("Primary Symbols Default Style"));

		// Style configuration
		JPanel styleConfigPanel = new JPanel(new GridLayout(2, 2, 10, 10));

		// Default thickness
		JLabel thicknessLabel = new JLabel("Default Thickness:");
		SpinnerNumberModel thicknessModel = new SpinnerNumberModel(3.0, 2.0, 6.0, 0.5);
		JSpinner thicknessSpinner = new JSpinner(thicknessModel);

		// Default color
		JLabel colorLabel = new JLabel("Default Color:");
		JButton colorButton = new JButton("Choose Color");
		colorButton.setBackground(Color.BLACK);
		colorButton.setForeground(Color.WHITE);

		// Store references for later access
		final JSpinner[] thicknessSpinnerRef = { thicknessSpinner };
		final JButton[] colorButtonRef = { colorButton };

		colorButton.addActionListener(e -> {
			Color newColor = JColorChooser.showDialog(frame, "Choose Default Color for Primary Symbols",
					colorButton.getBackground());
			if (newColor != null) {
				colorButton.setBackground(newColor);
				colorButton.setForeground(getContrastColor(newColor));
			}
		});

		styleConfigPanel.add(thicknessLabel);
		styleConfigPanel.add(thicknessSpinner);
		styleConfigPanel.add(colorLabel);
		styleConfigPanel.add(colorButton);

		// Preview panel
		JPanel previewPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		previewPanel.setBorder(BorderFactory.createTitledBorder("Preview"));

		JLabel previewLabel = new JLabel("Primary symbols will appear with thicker lines (3.0px) in black color");
		previewLabel.setFont(previewLabel.getFont().deriveFont(Font.ITALIC));
		previewPanel.add(previewLabel);

		panel.add(styleConfigPanel, BorderLayout.NORTH);
		panel.add(previewPanel, BorderLayout.CENTER);

		// Store the components for later access
		primaryThicknessSpinner = thicknessSpinner;
		primaryColorButton = colorButton;

		return panel;
	}

	private void applyPrimarySymbolsStyles() {
		if (primaryThicknessSpinner != null && primaryColorButton != null) {
			double thickness = (Double) primaryThicknessSpinner.getValue();
			Color color = primaryColorButton.getBackground();

			// Apply to all primary symbols
			for (String symbol : primarySymbols) {
				primarySymbolThickness.put(symbol, (float) thickness);
				primarySymbolColors.put(symbol, color);
			}

			// Update the chart
			updateChartSeries();
		}
	}

	private JPanel createRegularSymbolsControlPanel(Map<String, JSpinner> thicknessSpinners,
			Map<String, JButton> colorButtons) {
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

		// Default thickness control
		JPanel defaultThicknessPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		defaultThicknessPanel.add(new JLabel("Default Thickness:"));

		SpinnerNumberModel defaultModel = new SpinnerNumberModel(1.5, 0.5, 5.0, 0.5);
		JSpinner defaultThicknessSpinner = new JSpinner(defaultModel);
		JButton applyThicknessButton = new JButton("Apply to All");
		applyThicknessButton.addActionListener(e -> {
			double defaultThickness = (Double) defaultThicknessSpinner.getValue();
			for (JSpinner spinner : thicknessSpinners.values()) {
				spinner.setValue(defaultThickness);
			}
		});

		defaultThicknessPanel.add(defaultThicknessSpinner);
		defaultThicknessPanel.add(new JLabel("px"));
		defaultThicknessPanel.add(applyThicknessButton);

		controlPanel.add(defaultThicknessPanel);

		return controlPanel;
	}

	public void frameToFront() {
		SwingUtilities.invokeLater(() -> {
			frame.toFront();
			frame.setExtendedState(JFrame.NORMAL);
			frame.requestFocus();
		});

	}

	public void updateTitle(String newTitle) {
		this.titleName = newTitle;
		String currentSource = dataSourceManager.getCurrentDataSourceName();
		frame.setTitle("Monte Carlo Simulation - " + currentSource + " " + newTitle);
	}

	public String getTitle() {
		return titleName;
	}

	/**
	 * Update custom time range and refresh chart immediately
	 */
	public void updateCustomTimeRange(ZonedDateTime start, ZonedDateTime end, String timeframe) {
		SwingUtilities.invokeLater(() -> {
			this.useCustomTimeRange = true;
			this.currentTimeframe = timeframe != null ? timeframe : "1Min";

			// Update the saved values
			this.savedStartDate = Date.from(start.toInstant());
			this.savedEndDate = Date.from(end.toInstant());

			// Set times
			this.savedStartHour = String.format("%02d", start.getHour());
			this.savedStartMinute = String.format("%02d", start.getMinute());
			this.savedEndHour = String.format("%02d", end.getHour());
			this.savedEndMinute = String.format("%02d", end.getMinute());

			// Update control panel state
			updateControlPanelState(true);

			// Force refresh with new time range
			refreshDataWithCustomTimeRange();

			System.out.println(
					"Updated custom time range: " + start + " to " + end + " timeframe: " + this.currentTimeframe);
		});
	}

	/**
	 * Update current time mode with specific time range and refresh chart
	 * immediately
	 */
	public void updateCurrentTimeMode(String timeRange) {
		this.useCustomTimeRange = false;

		// Update the time range combo if provided
		if (timeRange != null && timeRangeCombo != null) {
			timeRangeCombo.setSelectedItem(timeRange);
		}

		// Update control panel state
		updateControlPanelState(false);

		// Force refresh with new time range
		refreshData();

		System.out.println("Updated current time mode with range: " + timeRange);
	}

	/**
	 * Force refresh the chart with current time range settings
	 */
	public void forceRefreshChart() {
		if (useCustomTimeRange) {
			refreshDataWithCustomTimeRange();
		} else {
			refreshData();
		}
		System.out.println("Forced chart refresh");
	}

	public Map<String, Object> getMonteCarloConfig() {
		return monteCarloConfig;
	}

	public void setMonteCarloConfig(Map<String, Object> monteCarloConfig) {
		this.monteCarloConfig = monteCarloConfig;
	}

	public Map<String, PriceData> getPriceDataMap() {
		return priceDataMap;
	}

	public void setPriceDataMap(Map<String, PriceData> priceDataMap) {
		this.priceDataMap = priceDataMap;
	}

	public List<String> getSelectedColumns() {
		return selectedColumns;
	}

	public void setSelectedColumns(List<String> selectedColumns) {
		this.selectedColumns = selectedColumns;
	}

	public Map<String, Object> getGraphSettings() {
		return graphSettings;
	}

	public void setGraphSettings(Map<String, Object> graphSettings) {
		this.graphSettings = graphSettings;
	}

}