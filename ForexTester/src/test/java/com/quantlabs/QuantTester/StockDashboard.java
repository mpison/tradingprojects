package com.quantlabs.QuantTester;

import okhttp3.*;
import org.ta4j.core.*;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.*;
import org.json.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import java.awt.*;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

public class StockDashboard extends JFrame {
    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey = "PK4UOZQDJJZ6WBAU52XM";
    private final String apiSecret = "Fag4ha2D58VyL0okwXgBHD1IvhoptmI2KiacMaNG";
    
    private final String[] symbols = {"AAPL", "MSFT", "GOOGL", "AMZN", "TSLA", "NVDA", "META", "NFLX"};
    private final String[] timeframes = {"1Min", "5Min", "15Min", "1H", "1D"};
    private final JTable dashboardTable;
    private final DefaultTableModel tableModel;
    private final JButton refreshButton;
    private final JButton startLiveButton;
    private final JButton stopLiveButton;
    private ScheduledExecutorService scheduler;
    private final Map<String, Color> statusColors = new HashMap<>();
    
    public StockDashboard() {
        // Initialize status colors
        statusColors.put("Bullish", new Color(200, 255, 200)); // Light green
        statusColors.put("Bearish", new Color(255, 200, 200)); // Light red
        statusColors.put("Neutral", new Color(240, 240, 240)); // Light gray
        statusColors.put("Overbought", new Color(255, 150, 150)); // Red
        statusColors.put("Oversold", new Color(150, 255, 150)); // Green
        
        // Set up UI
        setTitle("Stock Analysis Dashboard");
        setSize(1200, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        refreshButton = new JButton("Refresh");
        startLiveButton = new JButton("Start Live Updates");
        stopLiveButton = new JButton("Stop");
        stopLiveButton.setEnabled(false);
        
        controlPanel.add(refreshButton);
        controlPanel.add(startLiveButton);
        controlPanel.add(stopLiveButton);
        
        // Table setup
        String[] columnNames = createColumnNames();
        tableModel = new DefaultTableModel(columnNames, 0);
        dashboardTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (column > 0) { // Skip first column (symbol name)
                    String status = getValueAt(row, column).toString();
                    c.setBackground(statusColors.getOrDefault(status, Color.WHITE));
                }
                return c;
            }
        };
        
        dashboardTable.setRowHeight(25);
        dashboardTable.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(dashboardTable);
        
        // Add components to frame
        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        // Add action listeners
        refreshButton.addActionListener(e -> refreshData());
        startLiveButton.addActionListener(e -> startLiveUpdates());
        stopLiveButton.addActionListener(e -> stopLiveUpdates());
        
        // Initial data load
        refreshData();
    }
    
    private String[] createColumnNames() {
        List<String> columns = new ArrayList<>();
        columns.add("Symbol");
        
        for (String tf : timeframes) {
            columns.add(tf + " Trend");
            columns.add(tf + " RSI");
            columns.add(tf + " MACD");
        }
        
        return columns.toArray(new String[0]);
    }
    
    private void refreshData() {
        ExecutorService executor = Executors.newFixedThreadPool(symbols.length);
        tableModel.setRowCount(0);
        
        for (String symbol : symbols) {
            executor.execute(() -> {
                Map<String, AnalysisResult> results = analyzeSymbol(symbol);
                SwingUtilities.invokeLater(() -> addSymbolRow(symbol, results));
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
    
    private void addSymbolRow(String symbol, Map<String, AnalysisResult> results) {
        Vector<Object> row = new Vector<>();
        row.add(symbol);
        
        for (String timeframe : timeframes) {
            AnalysisResult result = results.get(timeframe);
            if (result != null) {
                row.add(getTrendStatus(result));
                row.add(getRsiStatus(result));
                row.add(getMacdStatus(result));
            } else {
                row.add("N/A");
                row.add("N/A");
                row.add("N/A");
            }
        }
        
        tableModel.addRow(row);
    }
    
    private String getTrendStatus(AnalysisResult result) {
        if (result.price > result.sma50 && result.price > result.sma200) {
            return "Bullish";
        } else if (result.price < result.sma50 && result.price < result.sma200) {
            return "Bearish";
        }
        return "Neutral";
    }
    
    private String getRsiStatus(AnalysisResult result) {
        if (result.rsi > 70) return "Overbought";
        if (result.rsi < 30) return "Oversold";
        if (result.rsi > 50) return "Bullish";
        return "Bearish";
    }
    
    private String getMacdStatus(AnalysisResult result) {
        if (result.macd > result.macdSignal) {
            return result.macdStatus.contains("Bullish") ? "Bullish" : "Neutral";
        } else {
            return result.macdStatus.contains("Bearish") ? "Bearish" : "Neutral";
        }
    }
    
    private void startLiveUpdates() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        
        startLiveButton.setEnabled(false);
        stopLiveButton.setEnabled(true);
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::refreshData, 0, 60, TimeUnit.SECONDS);
    }
    
    private void stopLiveUpdates() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        startLiveButton.setEnabled(true);
        stopLiveButton.setEnabled(false);
    }
    
    private String fetchStockData(String symbol, String timeframe, int limit) throws IOException {
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime start = end.minusDays(3); // Get data for past 3 days
        
        String url = String.format(
            "https://data.alpaca.markets/v2/stocks/bars?symbols=%s&timeframe=%s&start=%s&end=%s&limit=%d&adjustment=raw&feed=iex&sort=asc",
            symbol,
            timeframe,
            start.format(DateTimeFormatter.ISO_INSTANT),
            end.format(DateTimeFormatter.ISO_INSTANT),
            limit
        );
        
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
    
    private BarSeries parseJsonToBarSeries(String jsonResponse, String name) throws JSONException {
        JSONObject json = new JSONObject(jsonResponse);
        JSONArray bars = json.getJSONArray("bars");
        
        BarSeries series = new BaseBarSeriesBuilder().withName(name).build();
        
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
    }
    
    private AnalysisResult performTechnicalAnalysis(BarSeries series) {
        AnalysisResult result = new AnalysisResult();
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        int endIndex = series.getEndIndex();
        
        // Price
        result.price = closePrice.getValue(endIndex).doubleValue();
        
        // Trend Analysis (SMA 50/200)
        SMAIndicator sma50 = new SMAIndicator(closePrice, 50);
        SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
        result.sma50 = sma50.getValue(endIndex).doubleValue();
        result.sma200 = sma200.getValue(endIndex).doubleValue();
        
        // MACD (12,26,9)
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
        
        // RSI (14)
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        result.rsi = rsi.getValue(endIndex).doubleValue();
        
        return result;
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
            StockDashboard dashboard = new StockDashboard();
            dashboard.setVisible(true);
        });
    }
}