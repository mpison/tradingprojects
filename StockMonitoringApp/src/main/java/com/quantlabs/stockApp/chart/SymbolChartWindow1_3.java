package com.quantlabs.stockApp.chart;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
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

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
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

public class SymbolChartWindow1_3 extends JFrame {
    private final String symbol;
    private final Set<String> indicators;
    private final StockDataProvider dataProvider;
    private final Consumer<String> logConsumer;
    
    private OHLCSeries ohlcSeries;
    private Map<String, Object> indicatorSeriesMap;
    private DateAxis sharedAxis;
    private JFreeChart priceChart;
    private List<JFreeChart> macdCharts;
    private JFreeChart indicatorChart;
    private JLabel crosshairLabel;
    private JComboBox<String> timeframeComboBox;
    private BarSeries ta4jSeries;

    public SymbolChartWindow1_3(String symbol, String initialTimeframe, Set<String> indicators,
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
        ohlcSeries = new OHLCSeries(symbol);
        indicatorSeriesMap = new HashMap<>();
        sharedAxis = new DateAxis("Time");
        sharedAxis.setDateFormatOverride(new SimpleDateFormat("dd/MM HH:mm"));
        crosshairLabel = new JLabel("Hover over chart to view values");
        crosshairLabel.setFont(new Font("Monospaced", Font.PLAIN, 14));
    }

    private void createTimeframeSelector(String initialTimeframe) {
        String[] timeframes = {"1Min", "5Min", "15Min", "30Min", "1H", "4H", "1D", "1W"};
        timeframeComboBox = new JComboBox<>(timeframes);
        timeframeComboBox.setSelectedItem(initialTimeframe);
        timeframeComboBox.addActionListener(e -> reloadData((String) timeframeComboBox.getSelectedItem()));
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
        for (int i = 0; i < series.getBarCount(); i++) {
            Bar bar = series.getBar(i);
            ohlcSeries.add(new Second(Date.from(bar.getEndTime().toInstant())),
                         bar.getOpenPrice().doubleValue(),
                         bar.getHighPrice().doubleValue(),
                         bar.getLowPrice().doubleValue(),
                         bar.getClosePrice().doubleValue());
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
                else if (indicator.equals("HeikenAshi")) {
                    indicatorSeriesMap.put("Heiken Ashi", calculateHeikenAshi(ta4jSeries));
                }
                else if (indicator.startsWith("PSAR")) {
                    if (indicator.equals("PSAR(0.01)")) {
                        indicatorSeriesMap.put("PSAR(0.01)", calculatePSAR(ta4jSeries, 0.01, 0.2));
                    }
                    if (indicator.equals("PSAR(0.05)")) {
                        indicatorSeriesMap.put("PSAR(0.05)", calculatePSAR(ta4jSeries, 0.05, 0.2));
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
        // 1. Create Price Chart with PSAR
        OHLCSeriesCollection priceDataset = new OHLCSeriesCollection();
        priceDataset.addSeries(ohlcSeries);
        priceChart = ChartFactory.createCandlestickChart(
            symbol + " Price", "Time", "Price", priceDataset, false);
        
        XYPlot pricePlot = priceChart.getXYPlot();
        pricePlot.setDomainAxis(sharedAxis);
        addPSARIndicators(pricePlot);
        
        // 2. Create MACD Charts
        macdCharts = new ArrayList<>();
        createMACDCharts();
        
        // 3. Create Other Indicators Chart
        indicatorChart = createOtherIndicatorsChart();
        
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
                    new Ellipse2D.Double(-3, -3, 6, 6) : new Rectangle2D.Double(-3, -3, 6, 6));
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
        // Price chart
        XYPlot pricePlot = priceChart.getXYPlot();
        pricePlot.setDomainCrosshairVisible(true);
        pricePlot.setRangeCrosshairVisible(true);
        
        // MACD charts
        macdCharts.forEach(chart -> {
            XYPlot plot = chart.getXYPlot();
            plot.setDomainCrosshairVisible(true);
            plot.setRangeCrosshairVisible(true);
        });
        
        // Other indicators chart
        XYPlot indicatorPlot = indicatorChart.getXYPlot();
        indicatorPlot.setDomainCrosshairVisible(true);
        indicatorPlot.setRangeCrosshairVisible(true);
    }

    private void setupLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel chartPanel = new JPanel(new GridLayout(0, 1));
        
        // 1. Add Price Chart
        ChartPanel pricePanel = new ChartPanel(priceChart);
        chartPanel.add(pricePanel);
        
        // 2. Add MACD Charts
        for (JFreeChart macdChart : macdCharts) {
            chartPanel.add(new ChartPanel(macdChart));
        }
        
        // 3. Add Other Indicators Chart
        ChartPanel indicatorPanel = new ChartPanel(indicatorChart);
        chartPanel.add(indicatorPanel);
        
        // 4. Add Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(new JLabel("Timeframe:"));
        controlPanel.add(timeframeComboBox);
        
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(chartPanel, BorderLayout.CENTER);
        mainPanel.add(crosshairLabel, BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
        
        // Setup crosshair synchronization
        setupCrosshairSynchronization(pricePanel, indicatorPanel);
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
            ohlcSeries.clear();
            indicatorSeriesMap.clear();
            
            ZonedDateTime end = ZonedDateTime.now();
            ZonedDateTime start = dataProvider.calculateDefaultStartTime(timeframe, end);
            ta4jSeries = dataProvider.getHistoricalData(symbol, timeframe, 500, start, end);
            
            convertToOHLCSeries(ta4jSeries);
            calculateIndicators();
            
            // Update charts
            ((OHLCSeriesCollection) priceChart.getXYPlot().getDataset()).removeAllSeries();
            ((OHLCSeriesCollection) priceChart.getXYPlot().getDataset()).addSeries(ohlcSeries);
            
            // TODO: Update other charts similarly
            logConsumer.accept("Reloaded data for timeframe: " + timeframe);
        } catch (Exception e) {
            logConsumer.accept("Error reloading data: " + e.getMessage());
        }
    }
}