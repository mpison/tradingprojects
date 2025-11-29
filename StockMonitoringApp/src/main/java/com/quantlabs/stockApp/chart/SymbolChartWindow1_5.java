package com.quantlabs.stockApp.chart;

import java.awt.BorderLayout;
import java.awt.Color;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
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
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import com.quantlabs.stockApp.core.indicators.HeikenAshiIndicator;
import com.quantlabs.stockApp.data.StockDataProvider;

public class SymbolChartWindow1_5 extends JFrame {
	private final String symbol;
    private final Set<String> indicators;
    private final StockDataProvider dataProvider;
    private final Consumer<String> logConsumer;
    
    private OHLCSeries ohlcSeries;
    private OHLCSeries heikenAshiSeries;
    private Map<String, Object> indicatorSeriesMap;
    private DateAxis sharedAxis;
    private JFreeChart priceChart;
    private List<JFreeChart> macdCharts;
    private JFreeChart indicatorChart;
    private JLabel crosshairLabel;
    private JComboBox<String> timeframeComboBox;
    private BarSeries ta4jSeries;
    private JSplitPane mainSplitPane;
    private JSplitPane indicatorSplitPane;
    
    private JCheckBox liveUpdateCheckBox;
    private JComboBox<Integer> updateIntervalComboBox;
    private javax.swing.Timer updateTimer;

    public SymbolChartWindow1_5(String symbol, String initialTimeframe, Set<String> indicators,
            StockDataProvider dataProvider, Consumer<String> logConsumer) {
		this.symbol = symbol;
		this.indicators = indicators;
		this.dataProvider = dataProvider;
		this.logConsumer = logConsumer;
		
		setTitle(symbol + " Chart with Indicators");
		setSize(1200, 900);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		initializeComponents();
		createTimeframeSelector(initialTimeframe);
		loadData(initialTimeframe);
		createCharts();
		setupLayout();
	}

    private void initializeComponents() {
        ohlcSeries = new OHLCSeries(symbol + " (Regular)");
        heikenAshiSeries = new OHLCSeries(symbol + " (Heiken Ashi)");
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
                ZonedDateTime start = dataProvider.calculateDefaultStartTime(timeframe, end);
                
                // Get only the most recent data
                BarSeries newSeries = dataProvider.getHistoricalData(symbol, timeframe, 2, start, end);
                if (newSeries.getBarCount() > 0) {
                    Bar latestBar = newSeries.getLastBar();
                    Second latestSecond = new Second(Date.from(latestBar.getEndTime().toInstant()));
                    
                    // Check if we already have this time period
                    int itemCount = ohlcSeries.getItemCount();
                    boolean isNewPeriod = true;
                    
                    if (itemCount > 0) {
                        OHLCItem lastItem = (OHLCItem) ohlcSeries.getDataItem(itemCount - 1);
                        isNewPeriod = lastItem.getPeriod().getStart().getTime() != 
                                     latestBar.getEndTime().toInstant().toEpochMilli();
                    }
                    
                    if (isNewPeriod) {
                        // Add new candle
                        ohlcSeries.add(latestSecond,
                                latestBar.getOpenPrice().doubleValue(),
                                latestBar.getHighPrice().doubleValue(),
                                latestBar.getLowPrice().doubleValue(),
                                latestBar.getClosePrice().doubleValue());
                    } else {
                        // Update existing candle - need to remove and re-add
                        ohlcSeries.remove(latestSecond);
                        ohlcSeries.add(latestSecond,
                                latestBar.getOpenPrice().doubleValue(),
                                latestBar.getHighPrice().doubleValue(),
                                latestBar.getLowPrice().doubleValue(),
                                latestBar.getClosePrice().doubleValue());
                    }
                    
                    // Update Heiken Ashi
                    updateHeikenAshiSeries();
                    
                    // Update indicators
                    calculateIndicators();
                    updateIndicatorDatasets();
                    
                    // Force chart redraw
                    priceChart.fireChartChanged();
                    if (indicatorChart != null) {
                        indicatorChart.fireChartChanged();
                    }
                    for (JFreeChart macdChart : macdCharts) {
                        macdChart.fireChartChanged();
                    }
                }
            } catch (Exception e) {
                logConsumer.accept("Live update error: " + e.getMessage());
            }
        });
    }
    
    private void updateHeikenAshiSeries() {
        heikenAshiSeries.clear();
        HeikenAshiIndicator haIndicator = new HeikenAshiIndicator(ta4jSeries);
        
        // Track previous HA values
        double prevHaOpen = ta4jSeries.getBar(0).getOpenPrice().doubleValue();
        double prevHaClose = ta4jSeries.getBar(0).getClosePrice().doubleValue();
        
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
            
            // Add to series
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
                TimeSeriesCollection dataset = (TimeSeriesCollection) macdChart.getXYPlot().getDataset();
                dataset.removeAllSeries();
                TimeSeriesCollection newData = (TimeSeriesCollection) indicatorSeriesMap.get(key);
                for (int i = 0; i < newData.getSeriesCount(); i++) {
                    dataset.addSeries(newData.getSeries(i));
                }
            }
        }
        
        // Update other indicators dataset
        if (indicatorChart != null) {
            TimeSeriesCollection dataset = (TimeSeriesCollection) indicatorChart.getXYPlot().getDataset();
            dataset.removeAllSeries();
            for (Map.Entry<String, Object> entry : indicatorSeriesMap.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith("MACD") && !key.startsWith("PSAR")) {
                    if (entry.getValue() instanceof TimeSeries) {
                        dataset.addSeries((TimeSeries) entry.getValue());
                    }
                }
            }
        }
    }

    private void loadData(String timeframe) {
        try {
            ZonedDateTime end = ZonedDateTime.now();
            ZonedDateTime start = dataProvider.calculateDefaultStartTime(timeframe, end);
            ta4jSeries = dataProvider.getHistoricalData(symbol, timeframe, 500, start, end);
            convertToOHLCSeries(ta4jSeries);
            calculateIndicators();
        } catch (Exception e) {
            logConsumer.accept("Error loading data: " + e.getMessage());
        }
    }

    private void convertToOHLCSeries(BarSeries series) {
        ohlcSeries.clear();
        heikenAshiSeries.clear();
        
        HeikenAshiIndicator haIndicator = new HeikenAshiIndicator(series);
        
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

    private TimeSeries calculateHeikenAshi(BarSeries series) {
        TimeSeries haClose = new TimeSeries("HA Close");
        
        HeikenAshiIndicator haIndicator = new HeikenAshiIndicator(series);
        for (int i = 0; i < series.getBarCount(); i++) {
            ZonedDateTime time = series.getBar(i).getEndTime();
            haClose.add(new Second(Date.from(time.toInstant())), 
                       haIndicator.getValue(i).doubleValue());
        }
        return haClose;
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

    private void createCharts() {
        // 1. Create Price Chart with Heiken Ashi and PSAR
        OHLCSeriesCollection priceDataset = new OHLCSeriesCollection();
        priceDataset.addSeries(ohlcSeries);
        priceDataset.addSeries(heikenAshiSeries);
        
        priceChart = ChartFactory.createCandlestickChart(
            "", "Time", "Price", priceDataset, false);
        
        XYPlot pricePlot = priceChart.getXYPlot();
        pricePlot.setDomainAxis(sharedAxis);
        
        // Custom renderers
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
        
        addPSARIndicators(pricePlot);
        
        // 2. Create MACD Charts
        macdCharts = new ArrayList<>();
        for (String key : indicatorSeriesMap.keySet()) {
            if (key.startsWith("MACD")) {
                TimeSeriesCollection dataset = (TimeSeriesCollection) indicatorSeriesMap.get(key);
                
                JFreeChart chart = ChartFactory.createTimeSeriesChart(
                    key, "Time", "Value", dataset, true, true, false);
                
                XYPlot plot = chart.getXYPlot();
                plot.setDomainAxis(new DateAxis("Time"));
                
                XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
                renderer.setSeriesPaint(0, Color.BLUE);    // MACD Line
                renderer.setSeriesPaint(1, Color.ORANGE);  // Signal Line
                plot.setRenderer(renderer);
                
                macdCharts.add(chart);
            }
        }
        
        // 3. Create Other Indicators Chart only if needed
        boolean hasOtherIndicators = indicatorSeriesMap.keySet().stream()
            .anyMatch(key -> !key.startsWith("MACD") && !key.startsWith("PSAR"));
        
        if (hasOtherIndicators) {
            TimeSeriesCollection indicatorDataset = new TimeSeriesCollection();
            for (Map.Entry<String, Object> entry : indicatorSeriesMap.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith("MACD") && !key.startsWith("PSAR")) {
                    if (entry.getValue() instanceof TimeSeries) {
                        indicatorDataset.addSeries((TimeSeries) entry.getValue());
                    }
                }
            }
            
            indicatorChart = ChartFactory.createTimeSeriesChart(
                "Other Indicators", "Time", "Value", indicatorDataset, true, true, false);
        } else {
            indicatorChart = null;
        }
        
        // 4. Setup Crosshairs
        setupCrosshairs();
    }

    private void addPSARIndicators(XYPlot pricePlot) {
        int datasetIndex = 1;
        for (String key : indicatorSeriesMap.keySet()) {
            if (key.startsWith("PSAR")) {
                TimeSeries series = (TimeSeries) indicatorSeriesMap.get(key);
                TimeSeriesCollection dataset = new TimeSeriesCollection();
                dataset.addSeries(series);
                pricePlot.setDataset(datasetIndex, dataset);
                
                XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
                renderer.setSeriesPaint(0, key.contains("0.01") ? Color.RED : Color.BLUE);
                renderer.setSeriesShape(0, key.contains("0.01") ? 
                    new Ellipse2D.Double(-1, -1, 3, 3) : new Rectangle2D.Double(-1, -1, 3, 3));
                pricePlot.setRenderer(datasetIndex, renderer);
                datasetIndex++;
            }
        }
    }

    private void createMACDCharts() {
        for (String key : indicatorSeriesMap.keySet()) {
            if (key.startsWith("MACD")) {
                TimeSeriesCollection dataset = (TimeSeriesCollection) indicatorSeriesMap.get(key);
                
                JFreeChart chart = ChartFactory.createTimeSeriesChart(
                    "", "Time", "Value", dataset, true, true, false);
                
                XYPlot plot = chart.getXYPlot();
                plot.setDomainAxis(new DateAxis("Time"));
                
                XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
                renderer.setSeriesPaint(0, Color.BLUE);    // MACD Line
                renderer.setSeriesPaint(1, Color.ORANGE);  // Signal Line
                plot.setRenderer(renderer);
                
                macdCharts.add(chart);
            }
        }
    }

    private JFreeChart createOtherIndicatorsChart() {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (Map.Entry<String, Object> entry : indicatorSeriesMap.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("PSAR") && !key.startsWith("MACD")) {
                if (entry.getValue() instanceof TimeSeries) {
                    dataset.addSeries((TimeSeries) entry.getValue());
                }
            }
        }
        
        return ChartFactory.createTimeSeriesChart(
            "Other Indicators", "Time", "Value", dataset, true, true, false);
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
        // Main panel with split panes for adjustable heights
        mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.7); // 70% for price chart
        mainSplitPane.setDividerSize(5);
        
        // Price chart panel
        ChartPanel pricePanel = new ChartPanel(priceChart);
        pricePanel.setMinimumSize(new Dimension(1200, 400));
        pricePanel.setPreferredSize(new Dimension(1200, 600));
        pricePanel.setBorder(BorderFactory.createTitledBorder("Price Chart with Heiken Ashi"));
        
        // Indicators container panel
        JPanel indicatorsContainer = new JPanel(new BorderLayout());
        
        // MACD charts panel (adjustable)
        if (!macdCharts.isEmpty()) {
            JSplitPane macdSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            macdSplitPane.setDividerSize(5);
            
            List<ChartPanel> macdPanels = new ArrayList<>();
            for (JFreeChart macdChart : macdCharts) {
                ChartPanel panel = new ChartPanel(macdChart);
                panel.setMinimumSize(new Dimension(1200, 150));
                panel.setPreferredSize(new Dimension(1200, 200));
                panel.setBorder(BorderFactory.createTitledBorder(macdChart.getTitle().getText()));
                macdPanels.add(panel);
            }
            
            // Stack MACD panels vertically with adjustable dividers
            if (macdPanels.size() > 1) {
                macdSplitPane.setTopComponent(macdPanels.get(0));
                macdSplitPane.setBottomComponent(macdPanels.get(1));
                macdSplitPane.setResizeWeight(0.5);
            } else if (!macdPanels.isEmpty()) {
                macdSplitPane.setTopComponent(macdPanels.get(0));
            }
            
            indicatorsContainer.add(macdSplitPane, BorderLayout.CENTER);
        }
        
        // Other indicators panel (only if we have other indicators)
        boolean hasOtherIndicators = indicatorSeriesMap.keySet().stream()
            .anyMatch(key -> !key.startsWith("MACD") && !key.startsWith("PSAR"));
        
        if (hasOtherIndicators) {
            ChartPanel indicatorPanel = new ChartPanel(indicatorChart);
            indicatorPanel.setMinimumSize(new Dimension(1200, 150));
            indicatorPanel.setPreferredSize(new Dimension(1200, 200));
            indicatorPanel.setBorder(BorderFactory.createTitledBorder("Other Indicators"));
            
            JSplitPane indicatorsSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            indicatorsSplitPane.setDividerSize(5);
            
            if (macdCharts.isEmpty()) {
                // Only other indicators
                indicatorsSplitPane.setTopComponent(indicatorPanel);
            } else {
                // Both MACD and other indicators
                indicatorsSplitPane.setTopComponent(indicatorsContainer.getComponent(0));
                indicatorsSplitPane.setBottomComponent(indicatorPanel);
                indicatorsSplitPane.setResizeWeight(0.5);
            }
            
            indicatorsContainer.removeAll();
            indicatorsContainer.add(indicatorsSplitPane, BorderLayout.CENTER);
        }
        
        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(new JLabel("Timeframe:"));
        controlPanel.add(timeframeComboBox);
        controlPanel.add(liveUpdateCheckBox);
        controlPanel.add(new JLabel("Interval:"));
        controlPanel.add(updateIntervalComboBox);
        
        // Main container
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.add(controlPanel, BorderLayout.NORTH);
        
        if (indicatorsContainer.getComponentCount() > 0) {
            mainSplitPane.setTopComponent(pricePanel);
            mainSplitPane.setBottomComponent(indicatorsContainer);
            mainContainer.add(mainSplitPane, BorderLayout.CENTER);
        } else {
            // Only price chart if no indicators
            mainContainer.add(pricePanel, BorderLayout.CENTER);
        }
        
        mainContainer.add(crosshairLabel, BorderLayout.SOUTH);
        
        setContentPane(mainContainer);
        
        // Setup synchronization
        setupChartSynchronization(pricePanel, 
            hasOtherIndicators ? new ChartPanel(indicatorChart) : null);
    }
    
    private void setupChartSynchronization(ChartPanel pricePanel, ChartPanel indicatorPanel) {
        List<ChartPanel> allPanels = new ArrayList<>();
        allPanels.add(pricePanel);
        
        for (JFreeChart macdChart : macdCharts) {
            allPanels.add(new ChartPanel(macdChart));
        }
        
        // Add indicator panel if exists
        if (indicatorPanel != null) {
            allPanels.add(indicatorPanel);
        }

        // Create a shared domain axis for all charts
        DateAxis domainAxis = new DateAxis("Time");
        domainAxis.setAutoRange(true);
        
        // Apply the shared axis to all plots
        for (ChartPanel panel : allPanels) {
            XYPlot plot = (XYPlot) panel.getChart().getXYPlot();
            plot.setDomainAxis(domainAxis);
            
            // Enable zooming on the panel (not the plot)
            panel.setDomainZoomable(true);
            panel.setRangeZoomable(true);
        }

        // Crosshair synchronization
        MouseMotionListener crosshairListener = new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {}
            
            public void mouseMoved(MouseEvent e) {
                ChartPanel source = (ChartPanel) e.getSource();
                Point screenPoint = e.getPoint();
                Point2D javaPoint = source.translateScreenToJava2D(screenPoint);
                ChartRenderingInfo info = source.getChartRenderingInfo();
                
                if (info != null && javaPoint != null) {
                    Rectangle2D dataArea = info.getPlotInfo().getDataArea();
                    XYPlot sourcePlot = (XYPlot) source.getChart().getXYPlot();
                    
                    double x = sourcePlot.getDomainAxis().java2DToValue(javaPoint.getX(), dataArea, 
                              sourcePlot.getDomainAxisEdge());
                    double y = sourcePlot.getRangeAxis().java2DToValue(javaPoint.getY(), dataArea, 
                              sourcePlot.getRangeAxisEdge());
                    
                    // Sync all charts
                    for (ChartPanel panel : allPanels) {
                        XYPlot plot = (XYPlot) panel.getChart().getXYPlot();
                        plot.setDomainCrosshairValue(x);
                        if (panel == source) {
                            plot.setRangeCrosshairValue(y);
                        }
                    }
                    
                    updateCrosshairLabel(x);
                }
            }
        };
        
        allPanels.forEach(panel -> panel.addMouseMotionListener(crosshairListener));
    }
    
    private void setupCrosshairSynchronization(ChartPanel pricePanel, ChartPanel indicatorPanel) {
        List<ChartPanel> allPanels = new ArrayList<>();
        allPanels.add(pricePanel);
        allPanels.add(indicatorPanel);
        macdCharts.forEach(chart -> allPanels.add(new ChartPanel(chart)));
        
        MouseMotionListener listener = new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {}
            
            public void mouseMoved(MouseEvent e) {
                ChartPanel source = (ChartPanel) e.getSource();
                Point screenPoint = e.getPoint();
                Point2D javaPoint = source.translateScreenToJava2D(screenPoint);
                ChartRenderingInfo info = source.getChartRenderingInfo();
                
                if (info != null && javaPoint != null) {
                    Rectangle2D dataArea = info.getPlotInfo().getDataArea();
                    XYPlot sourcePlot = (XYPlot) source.getChart().getXYPlot();
                    
                    double x = sourcePlot.getDomainAxis().java2DToValue(javaPoint.getX(), dataArea, 
                              sourcePlot.getDomainAxisEdge());
                    double y = sourcePlot.getRangeAxis().java2DToValue(javaPoint.getY(), dataArea, 
                              sourcePlot.getRangeAxisEdge());
                    
                    // Sync all charts
                    for (ChartPanel panel : allPanels) {
                        XYPlot plot = (XYPlot) panel.getChart().getXYPlot();
                        plot.setDomainCrosshairValue(x);
                        if (panel == source) {
                            plot.setRangeCrosshairValue(y);
                        }
                    }
                    
                    updateCrosshairLabel(x);
                }
            }
        };
        
        allPanels.forEach(panel -> panel.addMouseMotionListener(listener));
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
        
        // Add indicator values
        for (Map.Entry<String, Object> entry : indicatorSeriesMap.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("MACD")) {
                TimeSeriesCollection dataset = (TimeSeriesCollection) entry.getValue();
                TimeSeriesDataItem lineItem = findNearestTimeSeriesItem(dataset.getSeries(0), time);
                TimeSeriesDataItem signalItem = findNearestTimeSeriesItem(dataset.getSeries(1), time);
                if (lineItem != null && signalItem != null) {
                    sb.append("<br>").append(key).append(" Line: ").append(String.format("%.4f", lineItem.getValue()))
                      .append(", Signal: ").append(String.format("%.4f", signalItem.getValue()));
                }
            } else if (entry.getValue() instanceof TimeSeries) {
                TimeSeriesDataItem item = findNearestTimeSeriesItem((TimeSeries) entry.getValue(), time);
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
            
            // Load new data
            ZonedDateTime end = ZonedDateTime.now();
            ZonedDateTime start = dataProvider.calculateDefaultStartTime(timeframe, end);
            ta4jSeries = dataProvider.getHistoricalData(symbol, timeframe, 500, start, end);
            
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
            
            logConsumer.accept("Successfully loaded data for timeframe: " + timeframe);
        } catch (Exception e) {
            logConsumer.accept("Error reloading data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void dispose() {
        stopLiveUpdates();
        super.dispose();
    }
}