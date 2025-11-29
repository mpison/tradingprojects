package com.quantlabs.QuantTester;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EnhancedStockDashboard extends JFrame {
    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey = "PK4UOZQDJJZ6WBAU52XM";
    private final String apiSecret = "Fag4ha2D58VyL0okwXgBHD1IvhoptmI2KiacMaNG";
    private final String[] symbols = {"AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "NVDA", "META", "NFLX"};
    private final String[] timeframes = {"1Min", "5Min", "15Min", "30Min", "1H", "4H", "1D"};
    private final JTable dashboardTable;
    private final DefaultTableModel tableModel;
    private final JButton refreshButton;
    private final JButton startLiveButton;
    private final JButton stopLiveButton;
    private ScheduledExecutorService scheduler;
    private boolean isLive = false;
    private final Map<String, Color> statusColors = new HashMap<>();

    public EnhancedStockDashboard() {
        // Initialize status colors
        statusColors.put("Bullish", new Color(200, 255, 200));
        statusColors.put("Bearish", new Color(255, 200, 200));
        statusColors.put("Neutral", new Color(240, 240, 240));
        statusColors.put("Overbought", new Color(255, 150, 150));
        statusColors.put("Oversold", new Color(150, 255, 150));
        statusColors.put("Strong Buy", new Color(100, 255, 100));
        statusColors.put("Buy", new Color(180, 255, 180));
        statusColors.put("Sell", new Color(255, 180, 180));
        statusColors.put("Strong Sell", new Color(255, 100, 100));

        // Set up UI
        setTitle("Enhanced Stock Dashboard (IEX Feed)");
        setSize(1400, 700);
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
                
                String value = getValueAt(row, column).toString();
                Color bgColor = statusColors.getOrDefault(value, Color.WHITE);
                c.setBackground(bgColor);
                
                if (bgColor.getRed() + bgColor.getGreen() + bgColor.getBlue() > 500) {
                    c.setForeground(Color.BLACK);
                } else {
                    c.setForeground(Color.WHITE);
                }
                
                return c;
            }
        };

        dashboardTable.setRowHeight(30);
        dashboardTable.setAutoCreateRowSorter(true);
        dashboardTable.setGridColor(new Color(220, 220, 220));
        dashboardTable.setShowGrid(true);
        dashboardTable.setIntercellSpacing(new Dimension(1, 1));

        // Center-align all columns except symbol
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 1; i < dashboardTable.getColumnCount(); i++) {
            dashboardTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JTableHeader header = dashboardTable.getTableHeader();
        header.setBackground(new Color(70, 130, 180));
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD));

        JScrollPane scrollPane = new JScrollPane(dashboardTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Add action listeners
        refreshButton.addActionListener(e -> {
            statusLabel.setText("Status: Refreshing data...");
            refreshData();
            statusLabel.setText("Status: Data refreshed at " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });

        startLiveButton.addActionListener(e -> {
            startLiveUpdates();
            statusLabel.setText("Status: Live updates running - last update at " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });

        stopLiveButton.addActionListener(e -> {
            stopLiveUpdates();
            statusLabel.setText("Status: Live updates stopped at " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });

        refreshData();
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

    private String[] createColumnNames() {
        List<String> columns = new ArrayList<>();
        columns.add("Symbol");
        
        for (String tf : timeframes) {
            columns.add(tf + " Trend");
            columns.add(tf + " RSI");
            columns.add(tf + " MACD");
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
                    SwingUtilities.invokeLater(() -> addSymbolRow(symbol, results));
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(this, "Error analyzing " + symbol + ": " + e.getMessage()));
                }
            });
        }
        
        executor.shutdown();
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
        ZonedDateTime start = end.minusDays(3);
        
        HttpUrl url = HttpUrl.parse("https://data.alpaca.markets/v2/stocks/" + symbol + "/bars")
            .newBuilder()
            .addQueryParameter("timeframe", timeframe)
            .addQueryParameter("start", start.format(DateTimeFormatter.ISO_INSTANT))
            .addQueryParameter("end", end.format(DateTimeFormatter.ISO_INSTANT))
            .addQueryParameter("limit", String.valueOf(limit))
            .addQueryParameter("adjustment", "raw")
            .addQueryParameter("feed", "iex")  // Using IEX feed
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
            
            // Handle both IEX and SIP feed response structures
            JSONArray bars;
            if (json.get("bars") instanceof JSONObject) {
                // SIP feed structure: {"bars": {"AAPL": [...]}}
                JSONObject barsObject = json.getJSONObject("bars");
                if (!barsObject.has(symbol)) {
                    throw new JSONException("No data for symbol: " + symbol);
                }
                bars = barsObject.getJSONArray(symbol);
            } else {
                // IEX feed structure: {"bars": [...]}
                bars = json.getJSONArray("bars");
            }
            
            BarSeries series = new BaseBarSeriesBuilder().withName(symbol).build();
            
            for (int i = 0; i < bars.length(); i++) {
                JSONObject bar = bars.getJSONObject(i);
                ZonedDateTime time = ZonedDateTime.parse(bar.getString("t"), DateTimeFormatter.ISO_ZONED_DATE_TIME);
                
                series.addBar(time,
                    bar.getDouble("o"),  // open
                    bar.getDouble("h"),  // high
                    bar.getDouble("l"),  // low
                    bar.getDouble("c"),  // close
                    bar.getLong("v")     // volume
                );
            }
            
            return series;
        } catch (JSONException e) {
            System.err.println("Error parsing JSON for symbol " + symbol + ": " + e.getMessage());
            return new BaseBarSeriesBuilder().withName(symbol).build(); // Return empty series
        }
    }

    private AnalysisResult performTechnicalAnalysis(BarSeries series) {
        AnalysisResult result = new AnalysisResult();
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        int endIndex = series.getEndIndex();
        
        result.price = closePrice.getValue(endIndex).doubleValue();
        
        SMAIndicator sma50 = new SMAIndicator(closePrice, 50);
        SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
        result.sma50 = sma50.getValue(endIndex).doubleValue();
        result.sma200 = sma200.getValue(endIndex).doubleValue();
        
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator macdSignal = new EMAIndicator(macd, 9);
        result.macd = macd.getValue(endIndex).doubleValue();
        result.macdSignal = macdSignal.getValue(endIndex).doubleValue();
        
        if (result.macd > result.macdSignal && 
            macd.getValue(endIndex-1).doubleValue() <= macdSignal.getValue(endIndex-1).doubleValue()) {
            result.macdStatus = "Bullish Crossover";
        } else if (result.macd < result.macdSignal && 
                 macd.getValue(endIndex-1).doubleValue() >= macdSignal.getValue(endIndex-1).doubleValue()) {
            result.macdStatus = "Bearish Crossover";
        } else {
            result.macdStatus = "Neutral";
        }
        
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        result.rsi = rsi.getValue(endIndex).doubleValue();
        
        return result;
    }

    private void addSymbolRow(String symbol, Map<String, AnalysisResult> results) {
        Vector<Object> row = new Vector<>();
        row.add(symbol);
        
        for (String timeframe : timeframes) {
            AnalysisResult result = results.get(timeframe);
            if (result != null) {
                row.add(getTrendStatus(result));
                row.add(getRsiStatus(result));
                row.add(getMacdStatus(result));
                row.add(getActionRecommendation(result));
            } else {
                row.add("N/A");
                row.add("N/A");
                row.add("N/A");
                row.add("N/A");
            }
        }
        
        tableModel.addRow(row);
    }

    private String getTrendStatus(AnalysisResult result) {
        if (result.price > result.sma50 && result.price > result.sma200) {
            return "↑ Strong Uptrend";
        } else if (result.price < result.sma50 && result.price < result.sma200) {
            return "↓ Strong Downtrend";
        } else if (result.price > result.sma50) {
            return "↑ Mild Uptrend";
        } else {
            return "↓ Mild Downtrend";
        }
    }

    private String getRsiStatus(AnalysisResult result) {
        if (result.rsi > 70) return "↓ Overbought";
        if (result.rsi < 30) return "↑ Oversold";
        if (result.rsi > 50) return "↑ Bullish";
        return "↓ Bearish";
    }

    private String getMacdStatus(AnalysisResult result) {
        if (result.macd > result.macdSignal) {
            return result.macdStatus.contains("Bullish") ? "↑ Bullish" : "Neutral";
        } else {
            return result.macdStatus.contains("Bearish") ? "↓ Bearish" : "Neutral";
        }
    }

    private String getActionRecommendation(AnalysisResult result) {
        boolean bullishTrend = result.price > result.sma50;
        boolean oversold = result.rsi < 30;
        boolean overbought = result.rsi > 70;
        boolean macdBullish = result.macd > result.macdSignal && result.macdStatus.contains("Bullish");
        boolean macdBearish = result.macd < result.macdSignal && result.macdStatus.contains("Bearish");
        
        if (bullishTrend && oversold && macdBullish) return "Strong Buy";
        if (!bullishTrend && overbought && macdBearish) return "Strong Sell";
        if (bullishTrend && macdBullish) return "Buy";
        if (!bullishTrend && macdBearish) return "Sell";
        return "Neutral";
    }

    private static class AnalysisResult {
        double price;
        double sma50;
        double sma200;
        double macd;
        double macdSignal;
        String macdStatus;
        double rsi;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EnhancedStockDashboard dashboard = new EnhancedStockDashboard();
            dashboard.setVisible(true);
        });
    }
}