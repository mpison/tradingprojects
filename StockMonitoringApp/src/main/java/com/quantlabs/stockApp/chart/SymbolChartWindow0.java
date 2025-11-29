package com.quantlabs.stockApp.chart;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.time.ohlc.OHLCItem;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.quantlabs.stockApp.core.indicators.AdvancedVWAPStrategy.AdvancedVWAPIndicator;
import com.quantlabs.stockApp.data.StockDataProvider;
import com.quantlabs.stockApp.data.StockDataProviderFactory;

import okhttp3.OkHttpClient;

public class SymbolChartWindow0 extends JFrame {
	//private final String symbol;
    private final Set<String> indicators;
    private StockDataProvider dataProvider;
    private final Consumer<String> logConsumer;
    
    private OHLCSeries ohlcSeries;
    private OHLCSeries heikenAshiSeries;
    private Map<String, Object> indicatorSeriesMap;
    private DateAxis sharedAxis;
    private JFreeChart priceChart;
    private List<JFreeChart> macdCharts = new ArrayList<>();
    private JFreeChart indicatorChart;
    private JLabel crosshairLabel;
    private JComboBox<String> timeframeComboBox;
    private BarSeries ta4jSeries;
    private JSplitPane mainSplitPane;
    private JSplitPane indicatorSplitPane;
    
    private JCheckBox liveUpdateCheckBox;
    private JComboBox<Integer> updateIntervalComboBox;
    private javax.swing.Timer updateTimer;
    
    private JButton customizeIndicatorsButton;
    
    private TimeSeries volumeSeries;
    
    private JButton dataProviderConfigButton;
    private String currentDataProvider = "Alpaca"; // or "Yahoo" - default
    private Map<String, Object> providerConfig = new HashMap<>();
	private StockDataProviderFactory dataProviderFactory;
	
	//symbol textfieds
	private JTextField symbolTextField;
    private JButton changeSymbolButton;
    private String currentSymbol;

    public SymbolChartWindow0(String symbol, String initialTimeframe, Set<String> indicators,
            StockDataProvider dataProvider, Consumer<String> logConsumer) {
    	//this.symbol = symbol;
    	this.currentSymbol = symbol;
    	
        this.indicators = new HashSet<>(indicators); // Make mutable
        this.dataProvider = dataProvider;
        this.logConsumer = logConsumer;
        
        // Initialize default provider configuration
        initializeDefaultProviderConfig();
        createDataProviderFactory();
        
        setTitle(symbol + " Chart with Indicators");
        setSize(1200, 900);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        initializeComponents();
        createTimeframeSelector(initialTimeframe);
        createCustomizeButton(); 
        createDataProviderConfigButton(); 
        createSymbolInput(); // Add this line
        loadData(initialTimeframe);
        createCharts();
        setupLayout();
	}
    
    private void createDataProviderFactory() {
        try {
            // Create OkHttpClient
            OkHttpClient client = new OkHttpClient();
            
            // Your API keys
            String apiKey = "PK4UOZQDJJZ6WBAU52XM";
            String apiSecret = "Fag4ha2D58VyL0okwXgBHD1IvhoptmI2KiacMaNG";
            String polygonApiKey = "fMCQBX5yvQZL_s6Zi1r9iKBkMkNLzNcw";
            
            // Create the factory
            this.dataProviderFactory = new StockDataProviderFactory(client, apiKey, apiSecret, polygonApiKey);
            
            logConsumer.accept("Data provider factory created successfully");
        } catch (Exception e) {
            logConsumer.accept("Error creating data provider factory: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeDefaultProviderConfig() {
        providerConfig = new HashMap<>();
        
        // You could load these from a properties file or system properties
        String defaultProvider = System.getProperty("stockapp.default.provider", "Alpaca");
        String defaultAdjustment = System.getProperty("stockapp.alpaca.adjustment", "all");
        String defaultFeed = System.getProperty("stockapp.alpaca.feed", "sip");
        
        providerConfig.put("provider", defaultProvider);
        providerConfig.put("adjustment", defaultAdjustment);
        providerConfig.put("feed", defaultFeed);
        
        currentDataProvider = defaultProvider;
        
        logConsumer.accept("Data provider configuration initialized: " + providerConfig);
    }

    private void initializeComponents() {
        ohlcSeries = new OHLCSeries(currentSymbol + " (Regular)");
        heikenAshiSeries = new OHLCSeries(currentSymbol + " (Heiken Ashi)");
        volumeSeries = new TimeSeries("Volume");
        indicatorSeriesMap = new HashMap<>();
        sharedAxis = new DateAxis("Time");
        sharedAxis.setDateFormatOverride(new SimpleDateFormat("dd/MM HH:mm"));
        crosshairLabel = new JLabel("Hover over chart to view values");
        crosshairLabel.setFont(new Font("Monospaced", Font.PLAIN, 14));
    }


 // Modify the createTimeframeSelector method
    private void createTimeframeSelector(String initialTimeframe) {
        String[] timeframes = {"1Min", "5Min", "15Min", "30Min", "1H", "4H", "1D", "1W"};
        timeframeComboBox = new JComboBox<>(timeframes);
        timeframeComboBox.setSelectedItem(initialTimeframe);
        timeframeComboBox.addActionListener(e -> {
            stopLiveUpdates();
            reloadData((String) timeframeComboBox.getSelectedItem());
        });

        // Live update controls
        liveUpdateCheckBox = new JCheckBox("Live Updates");
        liveUpdateCheckBox.addActionListener(e -> {
            if (liveUpdateCheckBox.isSelected()) {
                startLiveUpdates();
            } else {
                stopLiveUpdates();
            }
        });

        Integer[] intervals = {1, 5, 10, 15, 30, 60}; // seconds
        updateIntervalComboBox = new JComboBox<>(intervals);
        updateIntervalComboBox.setSelectedItem(5); // default to 5 seconds
        updateIntervalComboBox.addActionListener(e -> {
            if (liveUpdateCheckBox.isSelected()) {
                stopLiveUpdates();
                startLiveUpdates();
            }
        });

        // Initialize timer
        updateTimer = new javax.swing.Timer(0, e -> refreshLiveData());
        updateTimer.setRepeats(true);
    }
    
    private void createCustomizeButton() {
        customizeIndicatorsButton = new JButton("Customize Indicators");
        customizeIndicatorsButton.addActionListener(e -> showIndicatorCustomizationDialog());
    }
    
    private void createDataProviderConfigButton() {
    	dataProviderConfigButton = new JButton("Data Source");
        dataProviderConfigButton.setToolTipText("Configure data provider settings");
        dataProviderConfigButton.addActionListener(e -> showDataProviderConfigDialog());
    }

    private void showDataProviderConfigDialog() {
        DataProviderConfigDialog dialog = new DataProviderConfigDialog(
            this, currentDataProvider, providerConfig);
        dialog.setVisible(true);
        
        Map<String, Object> newConfig = dialog.getConfiguration();
        if (newConfig != null) {
            this.providerConfig.clear();
            this.providerConfig.putAll(newConfig);
            this.currentDataProvider = (String) newConfig.get("provider");
            
            // Apply configuration to data provider
            applyDataProviderConfiguration();
            
            // Reload data with new configuration
            stopLiveUpdates();
            reloadData((String) timeframeComboBox.getSelectedItem());
            
            logConsumer.accept("Data provider updated to: " + currentDataProvider + 
                              " with config: " + providerConfig);
        }
    }
    
    
    private void createSymbolInput() {
        // Create symbol text field with current symbol
        symbolTextField = new JTextField(currentSymbol, 10);
        symbolTextField.setToolTipText("Enter stock symbol (e.g., AAPL, TSLA, MSFT)");
        
        // Create change symbol button
        changeSymbolButton = new JButton("Change Symbol");
        changeSymbolButton.addActionListener(e -> changeSymbol());
        
        // Add Enter key listener to the text field
        symbolTextField.addActionListener(e -> changeSymbol());
    }

    private void changeSymbol() {
        String newSymbol = symbolTextField.getText().trim().toUpperCase();
        
        if (newSymbol.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a symbol", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (newSymbol.equals(currentSymbol)) {
            logConsumer.accept("Symbol is already: " + currentSymbol);
            return;
        }
        
        // Confirm symbol change
        int result = JOptionPane.showConfirmDialog(this,
            "Change symbol from " + currentSymbol + " to " + newSymbol + "?",
            "Confirm Symbol Change",
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            updateSymbol(newSymbol);
        } else {
            // Reset text field to current symbol
            symbolTextField.setText(currentSymbol);
        }
    }

    private void updateSymbol(String newSymbol) {
        try {
            stopLiveUpdates();
            
            // Update current symbol
            this.currentSymbol = newSymbol;
            
            // Update window title
            setTitle(newSymbol + " Chart with Indicators");
            
            // Clear existing data
            ohlcSeries.clear();
            heikenAshiSeries.clear();
            volumeSeries.clear();
            indicatorSeriesMap.clear();
            
            // Reload data with new symbol
            String timeframe = (String) timeframeComboBox.getSelectedItem();
            loadData(timeframe);
            
            // Recreate charts
            createCharts();
            
            // Refresh layout
            refreshChartLayout();
            
            logConsumer.accept("Symbol changed to: " + newSymbol);
            
        } catch (Exception e) {
            logConsumer.accept("Error changing symbol to " + newSymbol + ": " + e.getMessage());
            e.printStackTrace();
            
            // Revert to previous symbol on error
            symbolTextField.setText(currentSymbol);
            JOptionPane.showMessageDialog(this, 
                "Error loading symbol " + newSymbol + ": " + e.getMessage(), 
                "Symbol Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshChartLayout() {
        // Remove all components and rebuild the layout
        getContentPane().removeAll();
        setupLayout();
        revalidate();
        repaint();
        
        // Restart live updates if they were enabled
        if (liveUpdateCheckBox.isSelected()) {
            startLiveUpdates();
        }
    }
    
    private void applyDataProviderConfiguration() {
        try {
            // Recreate data provider with new configuration
            this.dataProvider = dataProviderFactory.getProvider(currentDataProvider, 
                message -> logConsumer.accept(message), providerConfig);
            
            logConsumer.accept("Data provider reconfigured: " + currentDataProvider);
            
            if ("Alpaca".equals(currentDataProvider)) {
                logConsumer.accept("Alpaca settings - Adjustment: " + 
                                  providerConfig.get("adjustment") + ", Feed: " + 
                                  providerConfig.get("feed"));
            }
        } catch (Exception e) {
            logConsumer.accept("Error applying data provider configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showIndicatorCustomizationDialog() {
        IndicatorSelectionDialog dialog = new IndicatorSelectionDialog(this, indicators);
        dialog.setVisible(true);
        
        Set<String> newIndicators = dialog.getSelectedIndicators();
        if (newIndicators != null) {
            this.indicators.clear();
            this.indicators.addAll(newIndicators);
            
            // Recalculate indicators and refresh charts
            calculateIndicators();
            updateIndicatorDatasets();
            refreshCharts();
            
            logConsumer.accept("Indicators updated: " + String.join(", ", indicators));
        }
    }
    
    private void refreshCharts() {
        // Clear existing charts
        macdCharts.clear();
        
        // Recreate charts with new indicators
        createCharts();
        
        // Refresh the layout
        getContentPane().removeAll();
        setupLayout();
        revalidate();
        repaint();
    }
    
 // Add these new methods for live updates
    private void startLiveUpdates() {
        int interval = (int) updateIntervalComboBox.getSelectedItem();
        updateTimer.setDelay(interval * 1000);
        updateTimer.start();
        logConsumer.accept("Live updates started (interval: " + interval + "s)");
    }

    private void stopLiveUpdates() {
        if (updateTimer.isRunning()) {
            updateTimer.stop();
            logConsumer.accept("Live updates stopped");
        }
    }

    private void refreshLiveData() {
        EventQueue.invokeLater(() -> {
            try {
                String timeframe = (String) timeframeComboBox.getSelectedItem();
                ZonedDateTime end = ZonedDateTime.now();
                // Use your existing calculateDefaultStartTime method
                ZonedDateTime start = dataProvider.calculateDefaultStartTime(timeframe, end);
                
                // Get fresh data - using your existing method signature
                BarSeries newSeries = dataProvider.getHistoricalData(currentSymbol, timeframe, 2, start, end);
                
                if (newSeries.getBarCount() > 0) {
                    Bar latestBar = newSeries.getLastBar();
                    Second latestSecond = new Second(Date.from(latestBar.getEndTime().toInstant()));
                    
                    // Check if we already have this period
                    boolean isNewPeriod = true;
                    if (ohlcSeries.getItemCount() > 0) {
                        OHLCItem lastItem = (OHLCItem) ohlcSeries.getDataItem(ohlcSeries.getItemCount() - 1);
                        isNewPeriod = !lastItem.getPeriod().equals(latestSecond);
                    }
                    
                    // Update or add data
                    if (isNewPeriod) {
                        // Add new candle
                        ohlcSeries.add(latestSecond,
                            latestBar.getOpenPrice().doubleValue(),
                            latestBar.getHighPrice().doubleValue(),
                            latestBar.getLowPrice().doubleValue(),
                            latestBar.getClosePrice().doubleValue());
                        volumeSeries.add(latestSecond, latestBar.getVolume().doubleValue());
                    } else {
                        // Update existing candle
                        ohlcSeries.remove(latestSecond);
                        ohlcSeries.add(latestSecond,
                            latestBar.getOpenPrice().doubleValue(),
                            latestBar.getHighPrice().doubleValue(),
                            latestBar.getLowPrice().doubleValue(),
                            latestBar.getClosePrice().doubleValue());
                        
                        // Update volume
                        volumeSeries.delete(latestSecond);
                        volumeSeries.add(latestSecond, latestBar.getVolume().doubleValue());
                    }
                    
                    // Update the series for indicators
                    if (ta4jSeries.getBarCount() > 0 && 
                        ta4jSeries.getLastBar().getEndTime().equals(latestBar.getEndTime())) {
                        // Replace last bar if it's the same period
                        ta4jSeries.addBar(latestBar, true);
                    } else {
                        // Add new bar
                        ta4jSeries.addBar(latestBar);
                    }
                    
                    // Recalculate indicators
                    calculateIndicators();
                    updateIndicatorDatasets();
                    
                    // Force redraw
                    priceChart.fireChartChanged();
                    if (indicatorChart != null) {
                        indicatorChart.fireChartChanged();
                    }
                    for (JFreeChart macdChart : macdCharts) {
                        macdChart.fireChartChanged();
                    }
                }
            } catch (Exception e) {
            	logConsumer.accept("Live update error for " + currentSymbol + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private void updateHeikenAshiSeries() {
        heikenAshiSeries.clear();
        if (ta4jSeries.getBarCount() == 0) return;
        
        // Initialize with first bar's values
        double prevHaClose = ta4jSeries.getBar(0).getClosePrice().doubleValue();
        double prevHaOpen = ta4jSeries.getBar(0).getOpenPrice().doubleValue();
        
        for (int i = 0; i < ta4jSeries.getBarCount(); i++) {
            Bar bar = ta4jSeries.getBar(i);
            Second second = new Second(Date.from(bar.getEndTime().toInstant()));
            
            // Calculate Heiken Ashi values
            double haClose = (bar.getOpenPrice().doubleValue() + 
                            bar.getHighPrice().doubleValue() + 
                            bar.getLowPrice().doubleValue() + 
                            bar.getClosePrice().doubleValue()) / 4;
            
            double haOpen = (prevHaOpen + prevHaClose) / 2;
            double haHigh = Math.max(bar.getHighPrice().doubleValue(), Math.max(haOpen, haClose));
            double haLow = Math.min(bar.getLowPrice().doubleValue(), Math.min(haOpen, haClose));
            
            heikenAshiSeries.add(second, haOpen, haHigh, haLow, haClose);
            
            // Store current values for next iteration
            prevHaOpen = haOpen;
            prevHaClose = haClose;
        }
    }

    private void updateIndicatorDatasets() {
        // Update MACD datasets
        for (JFreeChart macdChart : macdCharts) {
            String key = macdChart.getTitle().getText();
            if (indicatorSeriesMap.containsKey(key)) {
                Object indicatorData = indicatorSeriesMap.get(key);
                if (indicatorData instanceof TimeSeriesCollection) {
                    TimeSeriesCollection dataset = (TimeSeriesCollection) macdChart.getXYPlot().getDataset();
                    dataset.removeAllSeries();
                    TimeSeriesCollection newData = (TimeSeriesCollection) indicatorData;
                    for (int i = 0; i < newData.getSeriesCount(); i++) {
                        dataset.addSeries(newData.getSeries(i));
                    }
                }
            }
        }
        
        // Update other indicators dataset
        if (indicatorChart != null) {
            TimeSeriesCollection dataset = (TimeSeriesCollection) indicatorChart.getXYPlot().getDataset();
            dataset.removeAllSeries();
            for (Map.Entry<String, Object> entry : indicatorSeriesMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (!key.startsWith("MACD") && !key.startsWith("PSAR")) {
                    if (value instanceof TimeSeries) {
                        dataset.addSeries((TimeSeries) value);
                    }
                }
            }
        }
    }
   
    private void loadData(String timeframe) {
        try {
            ZonedDateTime end = ZonedDateTime.now();
            ZonedDateTime start = dataProvider.calculateDefaultStartTime(timeframe, end);
            
            // Clear existing data
            ohlcSeries.clear();
            heikenAshiSeries.clear();
            volumeSeries.clear();
            
            // Load new data
            ta4jSeries = dataProvider.getHistoricalData(currentSymbol, timeframe, 500, start, end);
            
            // Convert to chart series
            for (int i = 0; i < ta4jSeries.getBarCount(); i++) {
                Bar bar = ta4jSeries.getBar(i);
                Second second = new Second(Date.from(bar.getEndTime().toInstant()));
                
                // Regular OHLC
                ohlcSeries.add(second,
                    bar.getOpenPrice().doubleValue(),
                    bar.getHighPrice().doubleValue(),
                    bar.getLowPrice().doubleValue(),
                    bar.getClosePrice().doubleValue());
                
                // Volume
                volumeSeries.add(second, bar.getVolume().doubleValue());
            }
            
            // Calculate indicators
            calculateIndicators();
            
            logConsumer.accept("Data loaded for symbol: " + currentSymbol + " (" + timeframe + ")");
            
        } catch (Exception e) {
            logConsumer.accept("Error loading data: " + e.getMessage());
            e.printStackTrace();
            
            // Show error dialog
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, 
                    "Error loading symbol " + currentSymbol + ": " + e.getMessage(), 
                    "Data Load Error", 
                    JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private void convertToOHLCSeries(BarSeries series) {
        ohlcSeries.clear();
        heikenAshiSeries.clear();
        volumeSeries.clear();
                
        // Initialize first HA values with regular prices
        double prevHaClose = series.getBar(0).getClosePrice().doubleValue();
        double prevHaOpen = series.getBar(0).getOpenPrice().doubleValue();
        
        for (int i = 0; i < series.getBarCount(); i++) {
            Bar bar = series.getBar(i);
            Second second = new Second(Date.from(bar.getEndTime().toInstant()));
            
            // Regular OHLC
            ohlcSeries.add(second,
                         bar.getOpenPrice().doubleValue(),
                         bar.getHighPrice().doubleValue(),
                         bar.getLowPrice().doubleValue(),
                         bar.getClosePrice().doubleValue());
            
            // Volume
            volumeSeries.add(second, bar.getVolume().doubleValue());
            
            // Calculate Heiken Ashi OHLC
            double close = (bar.getOpenPrice().doubleValue() + 
                           bar.getHighPrice().doubleValue() + 
                           bar.getLowPrice().doubleValue() + 
                           bar.getClosePrice().doubleValue()) / 4;
            
            double open = (prevHaOpen + prevHaClose) / 2;
            double high = Math.max(bar.getHighPrice().doubleValue(), Math.max(open, close));
            double low = Math.min(bar.getLowPrice().doubleValue(), Math.min(open, close));
            
            heikenAshiSeries.add(second, open, high, low, close);
            
            // Store current values for next iteration
            prevHaOpen = open;
            prevHaClose = close;
        }
    }

    private void calculateIndicators() {
        indicatorSeriesMap.clear();
        
        // Always calculate Heiken Ashi (it's part of the price display)
        updateHeikenAshiSeries();
        
        for (String indicator : indicators) {
            try {
                if (indicator.startsWith("RSI")) {
                    int period = extractPeriod(indicator, 14);
                    indicatorSeriesMap.put("RSI(" + period + ")", calculateRSI(ta4jSeries, period));
                }
                else if (indicator.startsWith("MACD")) {
                    if (indicator.contains("5,8,9")) {
                        indicatorSeriesMap.put("MACD(5,8,9)", calculateMACD(ta4jSeries, 5, 8, 9));
                    } else {
                        indicatorSeriesMap.put("MACD(12,26,9)", calculateMACD(ta4jSeries, 12, 26, 9));
                    }
                }
                else if (indicator.startsWith("PSAR")) {
                    if (indicator.equals("PSAR(0.01)")) {
                        indicatorSeriesMap.put("PSAR(0.01)", calculatePSAR(ta4jSeries, 0.01, 0.01));
                    }
                    if (indicator.equals("PSAR(0.05)")) {
                        indicatorSeriesMap.put("PSAR(0.05)", calculatePSAR(ta4jSeries, 0.05, 0.05));
                    }
                }
                else if (indicator.startsWith("MA")) {
                    int period = extractPeriod(indicator, 20);
                    indicatorSeriesMap.put("MA(" + period + ")", calculateMA(ta4jSeries, period));
                }
                else if (indicator.equals("VWAP")) {
                    indicatorSeriesMap.put("VWAP", calculateVWAP(ta4jSeries));
                }
                // Note: HeikenAshi is already handled separately in updateHeikenAshiSeries()
            } catch (Exception e) {
                logConsumer.accept("Error calculating " + indicator + ": " + e.getMessage());
            }
        }
    }


    private int extractPeriod(String indicator, int defaultValue) {
        try {
            String periodStr = indicator.substring(indicator.indexOf('(') + 1, indicator.indexOf(')'));
            return Integer.parseInt(periodStr.split(",")[0]);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private TimeSeries calculateRSI(BarSeries series, int period) {
        TimeSeries rsiSeries = new TimeSeries("RSI(" + period + ")");
        RSIIndicator rsi = new RSIIndicator(new ClosePriceIndicator(series), period);
        
        for (int i = 0; i < series.getBarCount(); i++) {
            ZonedDateTime time = series.getBar(i).getEndTime();
            rsiSeries.add(new Second(Date.from(time.toInstant())), rsi.getValue(i).doubleValue());
        }
        return rsiSeries;
    }

    private TimeSeriesCollection calculateMACD(BarSeries series, int shortPeriod, int longPeriod, int signalPeriod) {
        TimeSeries macdLine = new TimeSeries("MACD Line");
        TimeSeries signalLine = new TimeSeries("MACD Signal");
        
        MACDIndicator macd = new MACDIndicator(new ClosePriceIndicator(series), shortPeriod, longPeriod);
        EMAIndicator signal = new EMAIndicator(macd, signalPeriod);
        
        for (int i = 0; i < series.getBarCount(); i++) {
            ZonedDateTime time = series.getBar(i).getEndTime();
            macdLine.add(new Second(Date.from(time.toInstant())), macd.getValue(i).doubleValue());
            signalLine.add(new Second(Date.from(time.toInstant())), signal.getValue(i).doubleValue());
        }
        
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(macdLine);
        dataset.addSeries(signalLine);
        return dataset;
    }
    
    private TimeSeries calculatePSAR(BarSeries series, double af, double maxAf) {
        TimeSeries psarSeries = new TimeSeries("PSAR(" + af + ")");
        ParabolicSarIndicator psar = new ParabolicSarIndicator(series, series.numOf(af), series.numOf(maxAf));
        
        for (int i = 0; i < series.getBarCount(); i++) {
            ZonedDateTime time = series.getBar(i).getEndTime();
            psarSeries.add(new Second(Date.from(time.toInstant())), psar.getValue(i).doubleValue());
        }
        return psarSeries;
    }
    
    private TimeSeries calculateMA(BarSeries series, int period) {
        TimeSeries maSeries = new TimeSeries("MA(" + period + ")");
        SMAIndicator sma = new SMAIndicator(new ClosePriceIndicator(series), period);
        
        for (int i = 0; i < series.getBarCount(); i++) {
            ZonedDateTime time = series.getBar(i).getEndTime();
            maSeries.add(new Second(Date.from(time.toInstant())), sma.getValue(i).doubleValue());
        }
        return maSeries;
    }

    private TimeSeries calculateVWAP(BarSeries series) {
        TimeSeries vwapSeries = new TimeSeries("VWAP");
        
        try {
            // Now using the standalone class
            AdvancedVWAPIndicator vwap = new AdvancedVWAPIndicator(series);
            
            for (int i = 0; i < series.getBarCount(); i++) {
                ZonedDateTime time = series.getBar(i).getEndTime();
                vwapSeries.add(new Second(Date.from(time.toInstant())), vwap.getValue(i).doubleValue());
            }
        } catch (Exception e) {
            logConsumer.accept("Error calculating VWAP: " + e.getMessage());
            e.printStackTrace();
        }
        
        return vwapSeries;
    }

    private void createCharts() {
        // 1. Create Price Chart with Heiken Ashi, Volume, and PSAR
        OHLCSeriesCollection priceDataset = new OHLCSeriesCollection();
        priceDataset.addSeries(ohlcSeries); // Regular candles
        priceDataset.addSeries(heikenAshiSeries); // Heiken Ashi candles
        
        priceChart = ChartFactory.createCandlestickChart(
            currentSymbol + " Price", "Time", "Price", priceDataset, false);
        
        XYPlot pricePlot = priceChart.getXYPlot();
        
        // Custom renderers for candles
        CandlestickRenderer regularRenderer = new CandlestickRenderer();
        regularRenderer.setSeriesPaint(0, Color.BLUE);
        regularRenderer.setUpPaint(Color.GREEN);
        regularRenderer.setDownPaint(Color.RED);
        
        CandlestickRenderer haRenderer = new CandlestickRenderer();
        haRenderer.setSeriesPaint(1, Color.ORANGE);
        haRenderer.setUpPaint(Color.GREEN);
        haRenderer.setDownPaint(Color.RED);
        
        pricePlot.setRenderer(0, regularRenderer);
        pricePlot.setRenderer(1, haRenderer);
        
        // 2. Add Volume
        TimeSeriesCollection volumeDataset = new TimeSeriesCollection(volumeSeries);
        XYBarRenderer volumeRenderer = new XYBarRenderer();
        volumeRenderer.setShadowVisible(false);
        volumeRenderer.setSeriesPaint(0, new Color(100, 100, 255, 150));
        
        NumberAxis volumeAxis = new NumberAxis("Volume");
        volumeAxis.setAutoRangeIncludesZero(true);
        
        pricePlot.setDataset(2, volumeDataset);
        pricePlot.setRangeAxis(2, volumeAxis);
        pricePlot.mapDatasetToRangeAxis(2, 2);
        pricePlot.setRenderer(2, volumeRenderer);
        
        // 3. Add all PSAR indicators to price chart (dataset index 3)
        int datasetIndex = 3;
        for (String key : indicatorSeriesMap.keySet()) {
            if (key.startsWith("PSAR")) {
                Object indicatorData = indicatorSeriesMap.get(key);
                if (indicatorData instanceof TimeSeries) {
                    TimeSeries psarSeries = (TimeSeries) indicatorData;
                    TimeSeriesCollection psarDataset = new TimeSeriesCollection(psarSeries);
                    
                    pricePlot.setDataset(datasetIndex, psarDataset);
                    
                    XYLineAndShapeRenderer psarRenderer = new XYLineAndShapeRenderer(false, true);
                    
                    // Different colors and shapes for different PSAR types
                    if (key.contains("0.01")) {
                        psarRenderer.setSeriesPaint(0, Color.RED);
                        psarRenderer.setSeriesShape(0, new Ellipse2D.Double(-3, -3, 6, 6));
                    } else {
                        psarRenderer.setSeriesPaint(0, Color.BLUE);
                        psarRenderer.setSeriesShape(0, new Rectangle2D.Double(-3, -3, 6, 6));
                    }
                    
                    pricePlot.setRenderer(datasetIndex, psarRenderer);
                    datasetIndex++;
                }
            }
        }
        
        // 4. Create MACD Charts
        macdCharts.clear();
        for (String key : indicatorSeriesMap.keySet()) {
            if (key.startsWith("MACD")) {
                Object indicatorData = indicatorSeriesMap.get(key);
                if (indicatorData instanceof TimeSeriesCollection) {
                    TimeSeriesCollection dataset = (TimeSeriesCollection) indicatorData;
                    
                    JFreeChart macdChart = ChartFactory.createTimeSeriesChart(
                        key, "Time", "Value", dataset, true, true, false);
                    
                    XYPlot plot = macdChart.getXYPlot();
                    
                    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
                    renderer.setSeriesPaint(0, Color.BLUE);    // MACD Line
                    renderer.setSeriesPaint(1, Color.ORANGE);  // Signal Line
                    plot.setRenderer(renderer);
                    
                    macdCharts.add(macdChart);
                }
            }
        }
        
        // 5. Add Moving Averages and VWAP to price chart
        for (String key : indicatorSeriesMap.keySet()) {
            if (key.startsWith("MA") || key.equals("VWAP")) {
                Object indicatorData = indicatorSeriesMap.get(key);
                if (indicatorData instanceof TimeSeries) {
                    TimeSeries maSeries = (TimeSeries) indicatorData;
                    TimeSeriesCollection maDataset = new TimeSeriesCollection(maSeries);
                    
                    pricePlot.setDataset(datasetIndex, maDataset);
                    
                    XYLineAndShapeRenderer maRenderer = new XYLineAndShapeRenderer(true, false);
                    
                    // Different colors for different MAs and VWAP
                    if (key.startsWith("MA(8)")) {
                        maRenderer.setSeriesPaint(0, Color.MAGENTA);
                        maRenderer.setSeriesStroke(0, new BasicStroke(1.5f));
                    } else if (key.startsWith("MA(20)")) {
                        maRenderer.setSeriesPaint(0, Color.ORANGE);
                        maRenderer.setSeriesStroke(0, new BasicStroke(1.5f));
                    } else if (key.startsWith("MA(200)")) {
                        maRenderer.setSeriesPaint(0, Color.RED);
                        maRenderer.setSeriesStroke(0, new BasicStroke(2.0f));
                    } else if (key.equals("VWAP")) {
                        maRenderer.setSeriesPaint(0, Color.BLUE);
                        maRenderer.setSeriesStroke(0, new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{5.0f, 5.0f}, 0.0f));
                    }
                    
                    pricePlot.setRenderer(datasetIndex, maRenderer);
                    datasetIndex++;
                }
            }
        }
        
        // 6. Setup Crosshairs
        setupCrosshairs();
    }
        

    private void setupCrosshairs() {
        // Price chart crosshairs
        XYPlot pricePlot = priceChart.getXYPlot();
        pricePlot.setDomainCrosshairVisible(true);
        pricePlot.setRangeCrosshairVisible(true);
        
        // MACD charts crosshairs
        for (JFreeChart macdChart : macdCharts) {
            XYPlot plot = macdChart.getXYPlot();
            plot.setDomainCrosshairVisible(true);
            plot.setRangeCrosshairVisible(true);
        }
        
        // Other indicators chart crosshairs (only if exists)
        if (indicatorChart != null) {
            XYPlot indicatorPlot = indicatorChart.getXYPlot();
            indicatorPlot.setDomainCrosshairVisible(true);
            indicatorPlot.setRangeCrosshairVisible(true);
        }
    }

    private void setupLayout() {
        // Main container
        JPanel mainContainer = new JPanel(new BorderLayout());
        
        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Add symbol input components
        controlPanel.add(new JLabel("Symbol:"));
        controlPanel.add(symbolTextField);
        controlPanel.add(changeSymbolButton);
        controlPanel.add(Box.createHorizontalStrut(10)); // Spacer
        
        controlPanel.add(new JLabel("Timeframe:"));
        controlPanel.add(timeframeComboBox);
        controlPanel.add(liveUpdateCheckBox);
        controlPanel.add(new JLabel("Interval:"));
        controlPanel.add(updateIntervalComboBox);
        controlPanel.add(customizeIndicatorsButton);
        controlPanel.add(dataProviderConfigButton); // Add this line
        
        mainContainer.add(controlPanel, BorderLayout.NORTH);
        
        // Chart area - use JSplitPane for main layout
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.6);
        
        // Price chart panel
        ChartPanel pricePanel = new ChartPanel(priceChart);
        pricePanel.setPreferredSize(new Dimension(1200, 500));
        mainSplitPane.setTopComponent(pricePanel);
        
        // Indicators panel (bottom)
        if (!macdCharts.isEmpty()) {
            JPanel indicatorsPanel = new JPanel(new BorderLayout());
            
            if (macdCharts.size() == 1) {
                indicatorsPanel.add(new ChartPanel(macdCharts.get(0)), BorderLayout.CENTER);
            } else {
                JSplitPane macdSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
                macdSplitPane.setTopComponent(new ChartPanel(macdCharts.get(0)));
                macdSplitPane.setBottomComponent(new ChartPanel(macdCharts.get(1)));
                indicatorsPanel.add(macdSplitPane, BorderLayout.CENTER);
            }
            
            mainSplitPane.setBottomComponent(indicatorsPanel);
        }
        
        mainContainer.add(mainSplitPane, BorderLayout.CENTER);
        mainContainer.add(crosshairLabel, BorderLayout.SOUTH);
        
        setContentPane(mainContainer);
        pack();
        
        // Setup synchronization
        setupChartSynchronization();
    }
    
    private void setupChartSynchronization() {
        List<ChartPanel> allPanels = new ArrayList<>();
        
        // Get the main container panel
        Container contentPane = getContentPane();
        if (!(contentPane instanceof JPanel)) return;
        
        // Find all ChartPanels in the component hierarchy
        findChartPanels((JPanel) contentPane, allPanels);
        
        if (allPanels.isEmpty()) return;
        
        // Create shared domain axis
        DateAxis sharedDomainAxis = new DateAxis("Time");
        sharedDomainAxis.setAutoRange(true);
        
        // Apply shared axis to all charts
        for (ChartPanel panel : allPanels) {
            XYPlot plot = (XYPlot) panel.getChart().getXYPlot();
            plot.setDomainAxis(sharedDomainAxis);
        }
        
        // Crosshair synchronization
        MouseMotionListener crosshairListener = new MouseMotionListener() {
            private ChartPanel currentSource;
            
            @Override
            public void mouseMoved(MouseEvent e) {
                currentSource = (ChartPanel) e.getSource();
                Point screenPoint = e.getPoint();
                Point2D javaPoint = currentSource.translateScreenToJava2D(screenPoint);
                ChartRenderingInfo info = currentSource.getChartRenderingInfo();
                
                if (info != null && javaPoint != null) {
                    Rectangle2D dataArea = info.getPlotInfo().getDataArea();
                    XYPlot sourcePlot = (XYPlot) currentSource.getChart().getXYPlot();
                    
                    double x = sourcePlot.getDomainAxis().java2DToValue(javaPoint.getX(), dataArea, 
                              sourcePlot.getDomainAxisEdge());
                    
                    // Sync domain crosshair across all charts
                    for (ChartPanel panel : allPanels) {
                        XYPlot plot = (XYPlot) panel.getChart().getXYPlot();
                        plot.setDomainCrosshairValue(x);
                        
                        // Only show range crosshair on the current chart
                        if (panel == currentSource) {
                            double y = plot.getRangeAxis().java2DToValue(javaPoint.getY(), dataArea, 
                                      plot.getRangeAxisEdge());
                            plot.setRangeCrosshairValue(y);
                        }
                    }
                    
                    updateCrosshairLabel(x);
                }
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }
        };
        
        // Add listener to all panels
        for (ChartPanel panel : allPanels) {
            panel.addMouseMotionListener(crosshairListener);
        }
    }

    // Helper method to recursively find all ChartPanels
    private void findChartPanels(Container container, List<ChartPanel> chartPanels) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof ChartPanel) {
                chartPanels.add((ChartPanel) comp);
            } else if (comp instanceof Container) {
                findChartPanels((Container) comp, chartPanels);
            }
        }
    }   
    
    private void updateCrosshairLabel(double xValue) {
        Date time = new Date((long) xValue);
        StringBuilder sb = new StringBuilder("<html>Time: <b>")
            .append(new SimpleDateFormat("dd/MM HH:mm").format(time)).append("</b>");
        
        // Add OHLC values
        OHLCItem ohlcItem = findNearestOhlcItem(time);
        if (ohlcItem != null) {
            sb.append(" | O:").append(String.format("%.4f", ohlcItem.getOpenValue()))
              .append(" H:").append(String.format("%.4f", ohlcItem.getHighValue()))
              .append(" L:").append(String.format("%.4f", ohlcItem.getLowValue()))
              .append(" C:").append(String.format("%.4f", ohlcItem.getCloseValue()));
        }
        
        // Add volume
        TimeSeriesDataItem volumeItem = findNearestTimeSeriesItem(volumeSeries, time);
        if (volumeItem != null) {
            sb.append(" | Vol:").append(String.format("%,.0f", volumeItem.getValue()));
        }
        
        // Add indicator values
        for (Map.Entry<String, Object> entry : indicatorSeriesMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (key.startsWith("MACD") && value instanceof TimeSeriesCollection) {
                TimeSeriesCollection dataset = (TimeSeriesCollection) value;
                TimeSeriesDataItem lineItem = findNearestTimeSeriesItem(dataset.getSeries(0), time);
                TimeSeriesDataItem signalItem = findNearestTimeSeriesItem(dataset.getSeries(1), time);
                if (lineItem != null && signalItem != null) {
                    sb.append("<br>").append(key).append(" Line: ").append(String.format("%.4f", lineItem.getValue()))
                      .append(", Signal: ").append(String.format("%.4f", signalItem.getValue()));
                }
            } else if (value instanceof TimeSeries) {
                TimeSeriesDataItem item = findNearestTimeSeriesItem((TimeSeries) value, time);
                if (item != null) {
                    sb.append("<br>").append(key).append(": ")
                      .append(String.format("%.4f", item.getValue()));
                }
            }
        }
        
        crosshairLabel.setText(sb.append("</html>").toString());
    }

    private OHLCItem findNearestOhlcItem(Date time) {
        long target = time.getTime();
        long minDiff = Long.MAX_VALUE;
        OHLCItem nearest = null;
        
        for (int i = 0; i < ohlcSeries.getItemCount(); i++) {
            OHLCItem item = (OHLCItem) ohlcSeries.getDataItem(i);
            long itemTime = item.getPeriod().getStart().getTime();
            if (Math.abs(itemTime - target) < minDiff) {
                minDiff = Math.abs(itemTime - target);
                nearest = item;
            }
        }
        return nearest;
    }

    private TimeSeriesDataItem findNearestTimeSeriesItem(TimeSeries series, Date time) {
        long target = time.getTime();
        long minDiff = Long.MAX_VALUE;
        TimeSeriesDataItem nearest = null;
        
        for (int i = 0; i < series.getItemCount(); i++) {
            TimeSeriesDataItem item = series.getDataItem(i);
            long itemTime = item.getPeriod().getStart().getTime();
            if (Math.abs(itemTime - target) < minDiff) {
                minDiff = Math.abs(itemTime - target);
                nearest = item;
            }
        }
        return nearest;
    }

    private void reloadData(String timeframe) {
        try {
            // Clear existing data
            ohlcSeries.clear();
            heikenAshiSeries.clear();
            indicatorSeriesMap.clear();
            
            // Load new data with current symbol
            ZonedDateTime end = ZonedDateTime.now();
            ZonedDateTime start = dataProvider.calculateDefaultStartTime(timeframe, end);
            ta4jSeries = dataProvider.getHistoricalData(currentSymbol, timeframe, 500, start, end);
            
            // Convert and calculate indicators
            convertToOHLCSeries(ta4jSeries);
            calculateIndicators();
            
            // Completely recreate the charts instead of trying to update existing ones
            createCharts();
            
            // Rebuild the UI layout
            getContentPane().removeAll();
            setupLayout();
            
            // Force UI update
            revalidate();
            repaint();
            
            logConsumer.accept("Successfully loaded data for " + currentSymbol + " with timeframe: " + timeframe);
        } catch (Exception e) {
            logConsumer.accept("Error reloading data for " + currentSymbol + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void dispose() {
        stopLiveUpdates();
        super.dispose();
    }
}