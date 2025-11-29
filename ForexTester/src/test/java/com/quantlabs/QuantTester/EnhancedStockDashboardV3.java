package com.quantlabs.QuantTester;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.OHLCDataItem;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
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
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.Num;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EnhancedStockDashboardV3 extends JFrame {
    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey = "PK4UOZQDJJZ6WBAU52XM";
    private final String apiSecret = "Fag4ha2D58VyL0okwXgBHD1IvhoptmI2KiacMaNG";
    private final String[] symbols = { "AAPL", "AVGO", "PLTR", "MSFT", "GOOGL", "AMZN", "TSLA", "NVDA", "META", "NFLX", "WMT", "JPM", "LLY", "V", "ORCL", "MA", "XOM", "COST", "JNJ", "HD", "UNH"};
    private final String[] timeframes = { "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min" };
    private final JTable dashboardTable;
    private final DefaultTableModel tableModel;
    private final JButton refreshButton;
    private final JButton startLiveButton;
    private final JButton stopLiveButton;
    private ScheduledExecutorService scheduler;
    private boolean isLive = false;
    private final Map<String, Color> statusColors = new HashMap<>();

    public EnhancedStockDashboardV3() {
        // Initialize status colors
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

        // Set up UI
        setTitle("Enhanced Stock Dashboard (IEX Feed)");
        setSize(2000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        refreshButton = new JButton("Refresh");
        startLiveButton = new JButton("Start Live Updates");
        stopLiveButton = new JButton("Stop Live Updates");
        stopLiveButton.setEnabled(false);

        // Style buttons
        startLiveButton.setBackground(new Color(50, 200, 50));
        startLiveButton.setForeground(Color.WHITE);
        stopLiveButton.setBackground(new Color(200, 50, 50));
        stopLiveButton.setForeground(Color.WHITE);
        refreshButton.setBackground(new Color(70, 130, 180));
        refreshButton.setForeground(Color.WHITE);

        controlPanel.add(refreshButton);
        controlPanel.add(startLiveButton);
        controlPanel.add(stopLiveButton);

        // Status label
        JLabel statusLabel = new JLabel("Status: Ready");
        controlPanel.add(statusLabel);

        // Table setup
        String[] columnNames = createColumnNames();
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        dashboardTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);

                if (column == 0) {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                    return c;
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

        // Configure table
        dashboardTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        dashboardTable.setRowHeight(30);
        dashboardTable.setAutoCreateRowSorter(true);
        dashboardTable.setGridColor(new Color(220, 220, 220));
        dashboardTable.setShowGrid(true);
        dashboardTable.setIntercellSpacing(new Dimension(1, 1));

        // Set column widths
        dashboardTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        dashboardTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        for (int i = 2; i < dashboardTable.getColumnCount(); i++) {
            dashboardTable.getColumnModel().getColumn(i).setPreferredWidth(120);
        }

        // Center align all columns except first two
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 2; i < dashboardTable.getColumnCount(); i++) {
            dashboardTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Left align symbol column
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        dashboardTable.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);

        // Style table header
        JTableHeader header = dashboardTable.getTableHeader();
        header.setBackground(new Color(70, 130, 180));
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD));

        // Add double-click listener
        dashboardTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    handleCellDoubleClick(evt);
                }
            }
        });

        // Create scroll pane
        JScrollPane scrollPane = new JScrollPane(dashboardTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(1900, 700));

        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Add action listeners
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

        // Initial data load
        refreshData();
    }
    
    private String[] createColumnNames() {
        List<String> columns = new ArrayList<>();
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
        ExecutorService executor = Executors.newFixedThreadPool(symbols.length);
        SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));

        for (String symbol : symbols) {
            executor.execute(() -> {
                try {
                    Map<String, AnalysisResult> results = analyzeSymbol(symbol);
                    double latestPrice = fetchLatestPrice(symbol);
                    SwingUtilities.invokeLater(() -> addSymbolRow(symbol, latestPrice, results));
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                            "Error analyzing " + symbol + ": " + e.getMessage()));
                }
            });
        }
        executor.shutdown();
    }

    private void startLiveUpdates() {
        if (isLive) return;

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
        if (!isLive) return;

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
                String jsonResponse = fetchStockData(symbol, timeframe, 100);
                BarSeries series = parseJsonToBarSeries(jsonResponse, symbol);

                if (series.getBarCount() > 20) {
                    results.put(timeframe, performTechnicalAnalysis(series));
                }
            } catch (Exception e) {
                System.err.println("Error analyzing " + symbol + " " + timeframe + ": " + e.getMessage());
            }
        }
        return results;
    }
    
    private String fetchStockData(String symbol, String timeframe, int limit) throws IOException {
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime start;
        
        // Set start date based on timeframe
        if (timeframe.equals("1Min") || timeframe.equals("5Min") || timeframe.equals("15Min")) {
            start = end.minusDays(7); // 7 days for minute timeframes
        } else if (timeframe.equals("30Min") || timeframe.equals("1H")) {
            start = end.minusDays(60); // 60 days for 30min and 1h
        } else if (timeframe.equals("4H") || timeframe.equals("1D")) {
            start = end.minusDays(120); // 120 days for 4h and daily
        } else {
            start = end.minusDays(7); // Default fallback
        }
        
        HttpUrl url = HttpUrl.parse("https://data.alpaca.markets/v2/stocks/" + symbol + "/bars")
            .newBuilder()
            .addQueryParameter("timeframe", timeframe)
            .addQueryParameter("start", start.format(DateTimeFormatter.ISO_INSTANT))
            .addQueryParameter("end", end.format(DateTimeFormatter.ISO_INSTANT))
            .addQueryParameter("limit", String.valueOf(limit))
            .addQueryParameter("adjustment", "raw")
            .addQueryParameter("feed", "iex")
            .addQueryParameter("sort", "asc")
            .build();
        
        Request request = new Request.Builder()
            .url(url)
            .get()
            .addHeader("accept", "application/json")
            .addHeader("APCA-API-KEY-ID", apiKey)
            .addHeader("APCA-API-SECRET-KEY", apiSecret)
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }
            return response.body().string();
        }
    }

    private BarSeries parseJsonToBarSeries(String jsonResponse, String symbol) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            
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
                
                series.addBar(time,
                    bar.getDouble("o"),
                    bar.getDouble("h"),
                    bar.getDouble("l"),
                    bar.getDouble("c"),
                    bar.getLong("v")
                );
            }
            
            return series;
        } catch (JSONException e) {
            System.err.println("Error parsing JSON for symbol " + symbol + ": " + e.getMessage());
            return new BaseBarSeriesBuilder().withName(symbol).build();
        }
    }

    private double fetchLatestPrice(String symbol) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url("https://data.alpaca.markets/v2/stocks/bars/latest?symbols=" + symbol + "&feed=iex")
                .get()
                .addHeader("accept", "application/json")
                .addHeader("APCA-API-KEY-ID", apiKey)
                .addHeader("APCA-API-SECRET-KEY", apiSecret)
                .build();

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

        // Basic indicators
        result.price = currentClose.doubleValue();

        // Moving averages
        SMAIndicator sma50 = new SMAIndicator(closePrice, 50);
        SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
        result.sma50 = sma50.getValue(endIndex).doubleValue();
        result.sma200 = sma200.getValue(endIndex).doubleValue();

        // MACD
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator macdSignal = new EMAIndicator(macd, 9);
        result.macd = macd.getValue(endIndex).doubleValue();
        result.macdSignal = macdSignal.getValue(endIndex).doubleValue();

        // MACD Status
        if (result.macd > result.macdSignal
                && macd.getValue(endIndex - 1).doubleValue() <= macdSignal.getValue(endIndex - 1).doubleValue()) {
            result.macdStatus = "Bullish Crossover";
        } else if (result.macd < result.macdSignal
                && macd.getValue(endIndex - 1).doubleValue() >= macdSignal.getValue(endIndex - 1).doubleValue()) {
            result.macdStatus = "Bearish Crossover";
        } else {
            result.macdStatus = "Neutral";
        }

        // RSI
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        result.rsi = rsi.getValue(endIndex).doubleValue();

        // PSAR Indicators
        try {
            ParabolicSarIndicator psar001 = new ParabolicSarIndicator(series, zero01, zero01);
            Num psar001Value = psar001.getValue(endIndex);
            result.psar001 = psar001Value.doubleValue();
            result.psar001Trend = psar001Value.isLessThan(currentClose) ? "↑ Uptrend" : "↓ Downtrend";

            ParabolicSarIndicator psar005 = new ParabolicSarIndicator(series, zero05, zero05);
            Num psar005Value = psar005.getValue(endIndex);
            result.psar005 = psar005Value.doubleValue();
            result.psar005Trend = psar005Value.isLessThan(currentClose) ? "↑ Uptrend" : "↓ Downtrend";

            // Validate PSAR values
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
        if (result.rsi > 70) return "Overbought";
        if (result.rsi < 30) return "Oversold";
        if (result.rsi > 50) return "Bullish";
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
        
        if (col < 2) return; // Skip symbol and price columns
        
        String symbol = (String) tableModel.getValueAt(row, 0);
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
                    JOptionPane.showMessageDialog(EnhancedStockDashboardV3.this,
                        "Error loading chart: " + e.getMessage(),
                        "Chart Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private String determineTimeframeFromColumn(int col) {
        int columnsPerTimeframe = 6;
        int timeframeIndex = (col - 2) / columnsPerTimeframe;
        return timeframes[timeframeIndex];
    }

    private String determineIndicatorFromColumn(int col) {
        int columnsPerTimeframe = 6;
        int indicatorIndex = (col - 2) % columnsPerTimeframe;
        
        switch(indicatorIndex) {
            case 0: return "TREND";
            case 1: return "RSI";
            case 2: return "MACD";
            case 3: return "PSAR_001";
            case 4: return "PSAR_005";
            case 5: return "ACTION";
            default: return "PRICE";
        }
    }

    // ... [Rest of the original methods remain unchanged until the end of the class] ...

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
            EnhancedStockDashboardV3 dashboard = new EnhancedStockDashboardV3();
            dashboard.setVisible(true);
        });
    }
}