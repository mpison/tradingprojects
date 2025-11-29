package com.quantlabs.QuantTester;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EnhancedStockDashboardV6 extends JFrame {
	private final OkHttpClient client = new OkHttpClient();
	private final String apiKey = "PK4UOZQDJJZ6WBAU52XM";
	private final String apiSecret = "Fag4ha2D58VyL0okwXgBHD1IvhoptmI2KiacMaNG";
	
	//top 20
	private final String[] symbols = { "AAPL", "AVGO", "PLTR", "MSFT", "GOOGL",
	 "AMZN", "TSLA", "NVDA", "META", "NFLX", "WMT", "JPM", "LLY", "V", "ORCL",
	 "MA", "XOM", "COST", "JNJ", "HD", "UNH" };
	
	
	//FILTERED
	//private final String[] symbols = {"AMZN", "AVGO", "MSFT", "WMT" };
	
	//ETFS
	//private final String[] symbols = { "QQQ", "SPY", "IGV", "VGT", "FTEC", "PTF", "SMH", "IGM", "IYW", "IXN"};
	
	private final String[] timeframes = { "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min" };
	private JTable dashboardTable;
	private FilterableTableModel tableModel;
	private JButton refreshButton;
	private JButton startLiveButton;
	private JButton stopLiveButton;
	private JButton toggleVisibilityButton;
	private JButton showAllButton; // New button
	private ScheduledExecutorService scheduler;
	private boolean isLive = false;
	private final Map<String, Color> statusColors = new HashMap<>();

	private class FilterableTableModel extends DefaultTableModel {
	    private Set<String> hiddenSymbols = new HashSet<>();
	    private Map<String, Integer> symbolToRowMap = new ConcurrentHashMap<>();

	    public FilterableTableModel(Object[] columnNames, int rowCount) {
	        super(columnNames, rowCount);
	    }

	    @Override
	    public int getRowCount() {
	        if (hiddenSymbols == null) {
	            hiddenSymbols = new HashSet<>();
	        }
	        return super.getRowCount() - hiddenSymbols.size();
	    }

	    @Override
	    public Object getValueAt(int row, int column) {
	        int actualRow = getActualRowIndex(row);
	        return super.getValueAt(actualRow, column);
	    }

	    private int getActualRowIndex(int visibleRow) {
	        int actualRow = -1;
	        int visibleCount = -1;

	        while (visibleCount < visibleRow) {
	            actualRow++;
	            String symbol = (String) super.getValueAt(actualRow, 1); // Column 1 is symbol
	            if (!hiddenSymbols.contains(symbol)) {
	                visibleCount++;
	            }
	        }
	        return actualRow;
	    }

	    public void toggleSymbolsVisibility(Set<String> symbolsToToggle) {
	        if (symbolsToToggle == null || symbolsToToggle.isEmpty()) {
	            return;
	        }
	        
	        for (String symbol : symbolsToToggle) {
	            if (hiddenSymbols.contains(symbol)) {
	                hiddenSymbols.remove(symbol);
	            } else {
	                hiddenSymbols.add(symbol);
	            }
	        }
	        fireTableDataChanged();
	    }


	    public void showAllSymbols() {
	        hiddenSymbols.clear();
	        fireTableDataChanged();
	    }

	    public boolean isSymbolHidden(String symbol) {
	        return hiddenSymbols.contains(symbol);
	    }

	    @Override
	    public boolean isCellEditable(int row, int column) {
	        return column == 0; // Only the checkbox column is editable
	    }

	    @Override
	    public Class<?> getColumnClass(int column) {
	        return column == 0 ? Boolean.class : super.getColumnClass(column);
	    }

	    @Override
	    public void addRow(Object[] rowData) {
	        super.addRow(rowData);
	        String symbol = (String) rowData[1]; // Column 1 is symbol
	        symbolToRowMap.put(symbol, super.getRowCount() - 1);
	    }
	}

	public EnhancedStockDashboardV6() {
		initializeStatusColors();
		setupUI();
		refreshData();
	}

	private void initializeStatusColors() {
		statusColors.put("Strong Uptrend", new Color(100, 255, 100));
		statusColors.put("Strong Downtrend", new Color(255, 100, 100));
		statusColors.put("Mild Uptrend", new Color(180, 255, 180));
		statusColors.put("Mild Downtrend", new Color(255, 180, 180));
		statusColors.put("Bullish", new Color(200, 255, 200));
		statusColors.put("Bearish", new Color(255, 200, 200));
		statusColors.put("Neutral", new Color(240, 240, 240));
		statusColors.put("Overbought", new Color(255, 150, 150));
		statusColors.put("Oversold", new Color(150, 255, 150));
		statusColors.put("Strong Buy", new Color(50, 200, 50));
		statusColors.put("Buy", new Color(120, 255, 120));
		statusColors.put("Sell", new Color(255, 120, 120));
		statusColors.put("Strong Sell", new Color(200, 50, 50));
		statusColors.put("↑ Uptrend", new Color(150, 255, 150));
		statusColors.put("↓ Downtrend", new Color(255, 150, 150));
	}

	private void setupUI() {
		setTitle("Enhanced Stock Dashboard (IEX Feed)");
		setSize(2000, 800);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
		refreshButton = createButton("Refresh", new Color(70, 130, 180));
		startLiveButton = createButton("Start Live Updates", new Color(50, 200, 50));
		stopLiveButton = createButton("Stop Live Updates", new Color(200, 50, 50));
		toggleVisibilityButton = createButton("Toggle Selected Rows", new Color(255, 165, 0));
		showAllButton = createButton("Show All Rows", new Color(100, 149, 237)); // Cornflower blue
		stopLiveButton.setEnabled(false);

		controlPanel.add(refreshButton);
		controlPanel.add(startLiveButton);
		controlPanel.add(stopLiveButton);
		controlPanel.add(toggleVisibilityButton);
		controlPanel.add(showAllButton);

		JLabel statusLabel = new JLabel("Status: Ready");
		controlPanel.add(statusLabel);

		String[] columnNames = createColumnNames();
		tableModel = new FilterableTableModel(columnNames, 0);
		dashboardTable = new JTable(tableModel) {
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				String symbol = (String) getValueAt(row, 1);
			    if (((FilterableTableModel) getModel()).isSymbolHidden(symbol)) {
			        return new JLabel();
			    }

				Component c = super.prepareRenderer(renderer, row, column);

				if (column == 0) {
					Boolean value = (Boolean) getValueAt(row, column);
		            JCheckBox checkBox = new JCheckBox();
		            checkBox.setSelected(value != null && value);
		            checkBox.setHorizontalAlignment(JLabel.CENTER);
		            checkBox.setBackground(getBackground());
		            return checkBox;
				}

				if (column == 1) {
					c.setBackground(new Color(245, 245, 245));
					c.setForeground(Color.BLACK);
					return c;
				}

				Object value = getValueAt(row, column);
				String status = (value != null) ? value.toString().trim() : "";

				if (status.startsWith("↑") || status.startsWith("↓")) {
					String trend = status.split(" ")[0] + " " + status.split(" ")[1];
					Color bgColor = statusColors.getOrDefault(trend, Color.WHITE);
					c.setBackground(bgColor);
					c.setForeground(bgColor.getRed() + bgColor.getGreen() + bgColor.getBlue() > 500 ? Color.BLACK
							: Color.WHITE);
					return c;
				}

				Color bgColor = statusColors.getOrDefault(status, Color.WHITE);
				c.setBackground(bgColor);
				int brightness = (bgColor.getRed() + bgColor.getGreen() + bgColor.getBlue()) / 3;
				c.setForeground(brightness > 150 ? Color.BLACK : Color.WHITE);

				return c;
			}
		};
		
		JCheckBox checkBoxEditor = new JCheckBox();
		checkBoxEditor.setHorizontalAlignment(JLabel.CENTER);
		dashboardTable.setDefaultEditor(Boolean.class, new DefaultCellEditor(checkBoxEditor));
		

		dashboardTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		dashboardTable.setRowHeight(25);
		dashboardTable.setAutoCreateRowSorter(true);
		dashboardTable.setGridColor(new Color(220, 220, 220));
		dashboardTable.setShowGrid(false);
		dashboardTable.setIntercellSpacing(new Dimension(0, 1));

		dashboardTable.getColumnModel().getColumn(0).setPreferredWidth(50);
		dashboardTable.getColumnModel().getColumn(1).setPreferredWidth(80);
		dashboardTable.getColumnModel().getColumn(2).setPreferredWidth(100);
		for (int i = 3; i < dashboardTable.getColumnCount(); i++) {
			dashboardTable.getColumnModel().getColumn(i).setPreferredWidth(120);
		}

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		for (int i = 3; i < dashboardTable.getColumnCount(); i++) {
			dashboardTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
		}

		DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
		leftRenderer.setHorizontalAlignment(JLabel.LEFT);
		dashboardTable.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);

		JTableHeader header = dashboardTable.getTableHeader();
		header.setBackground(new Color(70, 130, 180));
		header.setForeground(Color.WHITE);
		header.setFont(header.getFont().deriveFont(Font.BOLD));

		dashboardTable.addMouseListener(new MouseAdapter() {
			@Override
		    public void mousePressed(MouseEvent e) {
				int viewRow = dashboardTable.rowAtPoint(e.getPoint());
		        int viewCol = dashboardTable.columnAtPoint(e.getPoint());
		        
		        if (viewRow >= 0 && viewCol == 0) {  // Only handle checkbox column clicks
		            // Get the symbol from the visible row first
		            String symbol = (String) dashboardTable.getValueAt(viewRow, 1);
		            
		            // Find the actual model row for this symbol
		            for (int i = 0; i < tableModel.getDataVector().size(); i++) {
		            	Vector<?> v = tableModel.getDataVector().get(i);
		            	
		                if (v.get(1).toString().equals(symbol)) {
		                	boolean newVal = !Boolean.valueOf((boolean) v.get(0));
		                	tableModel.getDataVector().get(i).set(0, newVal);
		                	dashboardTable.repaint();
		                    break;
		                }
		            }		            
		        }
		    }
		});

		JScrollPane scrollPane = new JScrollPane(dashboardTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(1900, 700));

		refreshButton.addActionListener(e -> {
			statusLabel.setText("Status: Refreshing data...");
			refreshData();
			statusLabel.setText(
					"Status: Data refreshed at " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
		});

		startLiveButton.addActionListener(e -> {
			startLiveUpdates();
			statusLabel.setText("Status: Live updates running - last update at "
					+ LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
		});

		stopLiveButton.addActionListener(e -> {
			stopLiveUpdates();
			statusLabel.setText("Status: Live updates stopped at "
					+ LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
		});

		toggleVisibilityButton.addActionListener(e -> toggleSelectedRowsVisibility());

		showAllButton.addActionListener(e -> showAllRows());

		add(controlPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}

	private void showAllRows() {
	    ((FilterableTableModel) dashboardTable.getModel()).showAllSymbols();
	    dashboardTable.repaint();
	}

	private JButton createButton(String text, Color bgColor) {
		JButton button = new JButton(text);
		button.setBackground(bgColor);
		button.setForeground(Color.WHITE);
		return button;
	}

	private void toggleSelectedRowsVisibility() {
	    FilterableTableModel model = (FilterableTableModel) dashboardTable.getModel();
	    Set<String> symbolsToToggle = new HashSet<>();
	    
	    // Collect all selected symbols
	    for (int viewRow = 0; viewRow < dashboardTable.getRowCount(); viewRow++) {
	        int modelRow = dashboardTable.convertRowIndexToModel(viewRow);
	        Boolean isSelected = (Boolean) model.getValueAt(viewRow, 0);
	        String symbol = (String) model.getValueAt(viewRow, 1);

	        if (isSelected != null && isSelected) {
	            symbolsToToggle.add(symbol);
	            model.setValueAt(false, viewRow, 0); // Uncheck the checkbox
	        }
	    }
	    
	    // Toggle all selected symbols at once
	    if (!symbolsToToggle.isEmpty()) {
	        model.toggleSymbolsVisibility(symbolsToToggle);
	    }
	    
	    dashboardTable.repaint();
	}

	private String[] createColumnNames() {
		List<String> columns = new ArrayList<>();
		columns.add("Select");
		columns.add("Symbol");
		columns.add("Latest Price");

		for (String tf : timeframes) {
			columns.add(tf + " Trend");
			columns.add(tf + " RSI");
			columns.add(tf + " MACD");
			columns.add(tf + " PSAR(0.01)");
			columns.add(tf + " PSAR(0.05)");
			columns.add(tf + " Action");
		}

		return columns.toArray(new String[0]);
	}

	private void refreshData() {
	    // Get the current model and list of visible symbols
	    FilterableTableModel model = (FilterableTableModel) dashboardTable.getModel();
	    Set<String> visibleSymbols = new HashSet<>();
	    
	    // Collect all visible symbols
	    for (int i = 0; i < model.getRowCount(); i++) {
	        String symbol = (String) model.getValueAt(i, 1); // Column 1 contains symbols
	        if (!model.isSymbolHidden(symbol)) {
	            visibleSymbols.add(symbol);
	        }
	    }
	    
	    // If no symbols are visible (all hidden), show all symbols temporarily
	    if (visibleSymbols.isEmpty()) {
	        model.showAllSymbols();
	        for (String symbol : symbols) {
	            visibleSymbols.add(symbol);
	        }
	    }
	    
	    ExecutorService executor = Executors.newFixedThreadPool(visibleSymbols.size());
	    
	    for (String symbol : visibleSymbols) {
	        executor.execute(() -> {
	            try {
	                Map<String, AnalysisResult> results = analyzeSymbol(symbol);
	                double latestPrice = fetchLatestPrice(symbol);
	                SwingUtilities.invokeLater(() -> {
	                    // Update or add the row for this symbol
	                    updateOrAddSymbolRow(symbol, latestPrice, results);
	                });
	            } catch (Exception e) {
	                SwingUtilities.invokeLater(() -> 
	                    JOptionPane.showMessageDialog(this,
	                        "Error analyzing " + symbol + ": " + e.getMessage(),
	                        "Error",
	                        JOptionPane.ERROR_MESSAGE));
	            }
	        });
	    }
	    executor.shutdown();
	}
	
	private void updateOrAddSymbolRow(String symbol, double latestPrice, Map<String, AnalysisResult> results) {
	    FilterableTableModel model = (FilterableTableModel) dashboardTable.getModel();
	    
	    // Check if symbol already exists in the table
	    for (int i = 0; i < model.getRowCount(); i++) {
	        String rowSymbol = (String) model.getValueAt(i, 1);
	        if (rowSymbol.equals(symbol)) {
	            // Update existing row
	            updateRowData(model, i, latestPrice, results);
	            return;
	        }
	    }
	    
	    // Add new row if symbol doesn't exist
	    addNewSymbolRow(symbol, latestPrice, results);
	}

	private void updateRowData(FilterableTableModel model, int row, double latestPrice, 
	                         Map<String, AnalysisResult> results) {
	    model.setValueAt(String.format("%.2f", latestPrice), row, 2); // Price column
	    
	    // Update all timeframe columns
	    for (int i = 0; i < timeframes.length; i++) {
	        int baseCol = 3 + (i * 6); // Starting column for this timeframe
	        AnalysisResult result = results.get(timeframes[i]);
	        
	        if (result != null) {
	            model.setValueAt(getTrendStatus(result), row, baseCol);
	            model.setValueAt(getRsiStatus(result), row, baseCol + 1);
	            model.setValueAt(getMacdStatus(result), row, baseCol + 2);
	            model.setValueAt(formatPsarValue(result.psar001, result.psar001Trend), row, baseCol + 3);
	            model.setValueAt(formatPsarValue(result.psar005, result.psar005Trend), row, baseCol + 4);
	            model.setValueAt(getActionRecommendation(result), row, baseCol + 5);
	        }
	    }
	}

	private void addNewSymbolRow(String symbol, double latestPrice, Map<String, AnalysisResult> results) {
	    Vector<Object> row = new Vector<>();
	    row.add(false); // Checkbox
	    row.add(symbol);
	    row.add(String.format("%.2f", latestPrice));
	    
	    for (String timeframe : timeframes) {
	        AnalysisResult result = results.get(timeframe);
	        if (result != null) {
	            row.add(getTrendStatus(result));
	            row.add(getRsiStatus(result));
	            row.add(getMacdStatus(result));
	            row.add(formatPsarValue(result.psar001, result.psar001Trend));
	            row.add(formatPsarValue(result.psar005, result.psar005Trend));
	            row.add(getActionRecommendation(result));
	        } else {
	            row.add("N/A");
	            row.add("N/A");
	            row.add("N/A");
	            row.add("N/A");
	            row.add("N/A");
	            row.add("N/A");
	        }
	    }
	    tableModel.addRow(row);
	}

	private void startLiveUpdates() {
		if (isLive)
			return;

		isLive = true;
		startLiveButton.setEnabled(false);
		stopLiveButton.setEnabled(true);
		refreshButton.setEnabled(false);

		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(() -> {
			try {
				SwingUtilities.invokeLater(() -> refreshData());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 0, 60, TimeUnit.SECONDS);
	}

	private void stopLiveUpdates() {
		if (!isLive)
			return;

		isLive = false;
		startLiveButton.setEnabled(true);
		stopLiveButton.setEnabled(false);
		refreshButton.setEnabled(true);

		if (scheduler != null) {
			scheduler.shutdown();
			try {
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				scheduler.shutdownNow();
			}
		}
	}

	private Map<String, AnalysisResult> analyzeSymbol(String symbol) {
		Map<String, AnalysisResult> results = new ConcurrentHashMap<>();

		for (String timeframe : timeframes) {
			try {
				StockDataResult stockData = fetchStockData(symbol, timeframe, 1000);
				BarSeries series = parseJsonToBarSeries(stockData, symbol, timeframe);

				if (series.getBarCount() > 20) {
					results.put(timeframe, performTechnicalAnalysis(series));
				}
			} catch (Exception e) {
				System.err.println("Error analyzing " + symbol + " " + timeframe + ": " + e.getMessage());
			}
		}
		return results;
	}

	private StockDataResult fetchStockData(String symbol, String timeframe, int limit) throws IOException {
		ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC);
		ZonedDateTime start = calculateStartTime(timeframe, end);

		HttpUrl.Builder urlBuilder = HttpUrl.parse("https://data.alpaca.markets/v2/stocks/" + symbol + "/bars")
				.newBuilder().addQueryParameter("timeframe", timeframe)
				.addQueryParameter("start", start.format(DateTimeFormatter.ISO_INSTANT))
				.addQueryParameter("end", end.format(DateTimeFormatter.ISO_INSTANT))
				.addQueryParameter("limit", String.valueOf(limit)).addQueryParameter("adjustment", "raw")
				.addQueryParameter("feed", "iex").addQueryParameter("sort", "asc"); // Recommended to keep as asc for
																					// ta4j

		Request request = new Request.Builder().url(urlBuilder.build()).get().addHeader("accept", "application/json")
				.addHeader("APCA-API-KEY-ID", apiKey).addHeader("APCA-API-SECRET-KEY", apiSecret).build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response + ": " + response.body().string());
			}
			return new StockDataResult(response.body().string(), start, end);
		}
	}

	private BarSeries parseJsonToBarSeries(StockDataResult stockData, String symbol, String timeframe) {
		try {
			JSONObject json = new JSONObject(stockData.jsonResponse);

			if (!json.has("bars")) {
				throw new JSONException("No 'bars' field in response");
			}

			JSONArray bars;
			if (json.get("bars") instanceof JSONObject) {
				JSONObject barsObject = json.getJSONObject("bars");
				if (!barsObject.has(symbol)) {
					throw new JSONException("No data for symbol: " + symbol);
				}
				bars = barsObject.getJSONArray(symbol);
			} else {
				bars = json.getJSONArray("bars");
			}

			BarSeries series = new BaseBarSeriesBuilder().withName(symbol).build();

			for (int i = 0; i < bars.length(); i++) {
				JSONObject bar = bars.getJSONObject(i);
				ZonedDateTime time = ZonedDateTime.parse(bar.getString("t"), DateTimeFormatter.ISO_ZONED_DATE_TIME);

				series.addBar(time, bar.getDouble("o"), bar.getDouble("h"), bar.getDouble("l"), bar.getDouble("c"),
						bar.getLong("v"));
			}

			if (json.has("next_page_token")) {
				String nextPageToken = json.getString("next_page_token");
				if (nextPageToken != "null") {
					try {
						
						boolean hasNextPage = json.has("next_page_token");
						
						while(hasNextPage && nextPageToken != "null") {
							String nextPageData = fetchNextPage(symbol, nextPageToken, timeframe, stockData.start, stockData.end);
							JSONObject nextPageJson = new JSONObject(nextPageData);
							
							if (!nextPageJson.has("bars")) {
								throw new JSONException("No 'bars' field in response");
							}
							
							if (json.get("bars") instanceof JSONObject) {
								JSONObject nextPageBars = nextPageJson.getJSONObject("bars");
								if (!nextPageBars.has(symbol)) {
									throw new JSONException("No data for symbol: " + symbol);
								}
								bars = nextPageBars.getJSONArray(symbol);
							} else {
								bars = nextPageJson.getJSONArray("bars");
							}
							
							for (int i = 0; i < bars.length(); i++) {
								JSONObject bar = bars.getJSONObject(i);
								ZonedDateTime time = ZonedDateTime.parse(bar.getString("t"), DateTimeFormatter.ISO_ZONED_DATE_TIME);
	
								series.addBar(time, bar.getDouble("o"), bar.getDouble("h"), bar.getDouble("l"), bar.getDouble("c"),
										bar.getLong("v"));
							}
							
							hasNextPage = nextPageJson.has("next_page_token");
							nextPageToken = nextPageJson.getString("next_page_token");
						}
						
					} catch (Exception e) {
						System.err.println("Error fetching next page for " + symbol + ": " + e.getMessage());
					}
				}
			}

			return series;
		} catch (JSONException e) {
			System.err.println("Error parsing JSON for symbol " + symbol + ": " + e.getMessage());
			return new BaseBarSeriesBuilder().withName(symbol).build();
		}
	}

	private String fetchNextPage(String symbol, String nextPageToken, String timeframe, ZonedDateTime start, ZonedDateTime end) throws IOException {
				
		
		HttpUrl url = HttpUrl.parse("https://data.alpaca.markets/v2/stocks/" + symbol + "/bars").newBuilder()
				.addQueryParameter("start", start.format(DateTimeFormatter.ISO_INSTANT))
				.addQueryParameter("end", end.format(DateTimeFormatter.ISO_INSTANT))
				.addQueryParameter("timeframe", timeframe)
				.addQueryParameter("limit", String.valueOf(1000))
				.addQueryParameter("adjustment", "raw")
				.addQueryParameter("feed", "iex")
				.addQueryParameter("sort", "asc")
				.addQueryParameter("page_token", nextPageToken).build();

		Request request = new Request.Builder().url(url).get().addHeader("accept", "application/json")
				.addHeader("APCA-API-KEY-ID", apiKey).addHeader("APCA-API-SECRET-KEY", apiSecret).build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response + ": " + response.body().string());
			}
			return response.body().string();
		}
	}

	private ZonedDateTime calculateStartTime(String timeframe, ZonedDateTime end) {
		switch (timeframe) {
		case "1Min":
			return end.minusDays(4);
		case "5Min":
			return end.minusDays(7);
		case "15Min":
			return end.minusDays(7);
		case "30Min":
		case "1H":
			return end.minusDays(30);
		case "4H":
			return end.minusDays(30);
		case "1D":
			return end.minusDays(60);
		default:
			return end.minusDays(7);
		}
	}
	
	private static class StockDataResult {
	    String jsonResponse;
	    ZonedDateTime start;
	    ZonedDateTime end;

	    public StockDataResult(String jsonResponse, ZonedDateTime start, ZonedDateTime end) {
	        this.jsonResponse = jsonResponse;
	        this.start = start;
	        this.end = end;
	    }
	}


	private double fetchLatestPrice(String symbol) throws IOException, JSONException {
		/*
		 * try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) {
		 * Thread.currentThread().interrupt(); throw new
		 * IOException("Request was interrupted", e); }
		 */

		Request request = new Request.Builder()
				.url("https://data.alpaca.markets/v2/stocks/bars/latest?symbols=" + symbol + "&feed=iex").get()
				.addHeader("accept", "application/json").addHeader("APCA-API-KEY-ID", apiKey)
				.addHeader("APCA-API-SECRET-KEY", apiSecret).build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response + ": " + response.body().string());
			}

			String responseBody = response.body().string();
			JSONObject json = new JSONObject(responseBody);
			JSONObject bars = json.getJSONObject("bars");
			JSONObject symbolData = bars.getJSONObject(symbol);
			return symbolData.getDouble("c");
		}
	}

	private void addSymbolRow(String symbol, double latestPrice, Map<String, AnalysisResult> results) {
		Vector<Object> row = new Vector<>();
		row.add(false);
		row.add(symbol);
		row.add(String.format("%.2f", latestPrice));

		for (String timeframe : timeframes) {
			AnalysisResult result = results.get(timeframe);
			if (result != null) {
				row.add(getTrendStatus(result));
				row.add(getRsiStatus(result));
				row.add(getMacdStatus(result));
				row.add(formatPsarValue(result.psar001, result.psar001Trend));
				row.add(formatPsarValue(result.psar005, result.psar005Trend));
				row.add(getActionRecommendation(result));
			} else {
				row.add("N/A");
				row.add("N/A");
				row.add("N/A");
				row.add("N/A");
				row.add("N/A");
				row.add("N/A");
			}
		}
		tableModel.addRow(row);
	}

	private AnalysisResult performTechnicalAnalysis(BarSeries series) {
		AnalysisResult result = new AnalysisResult();
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		int endIndex = series.getEndIndex();
		Bar currentBar = series.getBar(endIndex);

		Num zero01 = series.numOf(0.01);
		Num zero05 = series.numOf(0.05);
		Num currentClose = currentBar.getClosePrice();

		result.price = currentClose.doubleValue();

		SMAIndicator sma50 = new SMAIndicator(closePrice, 50);
		SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
		result.sma50 = sma50.getValue(endIndex).doubleValue();
		result.sma200 = sma200.getValue(endIndex).doubleValue();

		MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
		EMAIndicator macdSignal = new EMAIndicator(macd, 9);
		result.macd = macd.getValue(endIndex).doubleValue();
		result.macdSignal = macdSignal.getValue(endIndex).doubleValue();

		if (result.macd > result.macdSignal
				&& macd.getValue(endIndex - 1).doubleValue() <= macdSignal.getValue(endIndex - 1).doubleValue()) {
			result.macdStatus = "Bullish Crossover";
		} else if (result.macd < result.macdSignal
				&& macd.getValue(endIndex - 1).doubleValue() >= macdSignal.getValue(endIndex - 1).doubleValue()) {
			result.macdStatus = "Bearish Crossover";
		} else {
			result.macdStatus = "Neutral";
		}

		RSIIndicator rsi = new RSIIndicator(closePrice, 14);
		result.rsi = rsi.getValue(endIndex).doubleValue();

		try {
			ParabolicSarIndicator psar001 = new ParabolicSarIndicator(series, zero01, zero01, zero01);
			Num psar001Value = psar001.getValue(endIndex);
			result.psar001 = psar001Value.doubleValue();
			result.psar001Trend = psar001Value.isLessThan(currentClose) ? "↑ Uptrend" : "↓ Downtrend";

			ParabolicSarIndicator psar005 = new ParabolicSarIndicator(series, zero05, zero05, zero05);
			Num psar005Value = psar005.getValue(endIndex);
			result.psar005 = psar005Value.doubleValue();
			result.psar005Trend = psar005Value.isLessThan(currentClose) ? "↑ Uptrend" : "↓ Downtrend";

			if (Double.isInfinite(result.psar001) || Double.isNaN(result.psar001)) {
				result.psar001 = result.price;
				result.psar001Trend = "N/A";
			}
			if (Double.isInfinite(result.psar005) || Double.isNaN(result.psar005)) {
				result.psar005 = result.price;
				result.psar005Trend = "N/A";
			}
		} catch (Exception e) {
			System.err.println("Error calculating PSAR: " + e.getMessage());
			result.psar001 = result.price;
			result.psar005 = result.price;
			result.psar001Trend = "Error";
			result.psar005Trend = "Error";
		}

		return result;
	}

	private String formatPsarValue(double psarValue, String trend) {
		return String.format("%s (%.4f)", trend, psarValue);
	}

	private String getTrendStatus(AnalysisResult result) {
		if (result.price > result.sma50 && result.price > result.sma200) {
			return "Strong Uptrend";
		} else if (result.price < result.sma50 && result.price < result.sma200) {
			return "Strong Downtrend";
		} else if (result.price > result.sma50) {
			return "Mild Uptrend";
		} else {
			return "Mild Downtrend";
		}
	}

	private String getRsiStatus(AnalysisResult result) {
		if (result.rsi > 70)
			return "Overbought";
		if (result.rsi < 30)
			return "Oversold";
		if (result.rsi > 50)
			return "Bullish";
		return "Bearish";
	}

	private String getMacdStatus(AnalysisResult result) {
		if (result.macd > result.macdSignal) {
			return "Bullish";
		} else {
			return "Bearish";
		}
	}

	private String getActionRecommendation(AnalysisResult result) {
		boolean bullishTrend = result.price > result.sma50;
		boolean oversold = result.rsi < 30;
		boolean overbought = result.rsi > 70;
		boolean macdBullish = result.macd > result.macdSignal && result.macdStatus.contains("Bullish");
		boolean macdBearish = result.macd < result.macdSignal && result.macdStatus.contains("Bearish");

		if (bullishTrend && oversold && macdBullish)
			return "Strong Buy";
		if (!bullishTrend && overbought && macdBearish)
			return "Strong Sell";
		if (bullishTrend && macdBullish)
			return "Buy";
		if (!bullishTrend && macdBearish)
			return "Sell";
		return "Neutral";
	}

	private void handleCellDoubleClick(MouseEvent evt) {
		int row = dashboardTable.rowAtPoint(evt.getPoint());
		int col = dashboardTable.columnAtPoint(evt.getPoint());

		if (col <= 1)
			return;

		String symbol = (String) tableModel.getValueAt(row, 1);
		String timeframe = determineTimeframeFromColumn(col);
		String indicatorType = determineIndicatorFromColumn(col);

		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				ChartWindow.showChart(symbol, timeframe, indicatorType, client, apiKey, apiSecret);
				return null;
			}

			@Override
			protected void done() {
				try {
					get();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(EnhancedStockDashboardV6.this,
							"Error loading chart: " + e.getMessage(), "Chart Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	private String determineTimeframeFromColumn(int col) {
		int columnsPerTimeframe = 6;
		int timeframeIndex = (col - 3) / columnsPerTimeframe;
		return timeframes[timeframeIndex];
	}

	private String determineIndicatorFromColumn(int col) {
		int columnsPerTimeframe = 6;
		int indicatorIndex = (col - 3) % columnsPerTimeframe;

		switch (indicatorIndex) {
		case 0:
			return "TREND";
		case 1:
			return "RSI";
		case 2:
			return "MACD";
		case 3:
			return "PSAR_001";
		case 4:
			return "PSAR_005";
		case 5:
			return "ACTION";
		default:
			return "PRICE";
		}
	}

	private static class AnalysisResult {
		double price;
		double sma50;
		double sma200;
		double macd;
		double macdSignal;
		String macdStatus;
		double rsi;
		double psar001;
		double psar005;
		String psar001Trend;
		String psar005Trend;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			EnhancedStockDashboardV6 dashboard = new EnhancedStockDashboardV6();
			dashboard.setVisible(true);
		});
	}
}