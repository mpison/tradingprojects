package com.quantlabs.QuantTester;

import okhttp3.*;
import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.jfree.ui.Layer;
import org.jfree.data.time.ohlc.*;
import org.json.*;
import org.ta4j.core.*;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.helpers.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class ChartWindow extends JFrame {
    private final String symbol;
    private final String timeframe;
    private final String indicatorType;
    private final BarSeries series;
    
    public ChartWindow(String symbol, String timeframe, String indicatorType, 
                      OkHttpClient client, String apiKey, String apiSecret) throws IOException {
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.indicatorType = indicatorType;
        
        setTitle(String.format("%s - %s (%s)", symbol, timeframe, indicatorType));
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        // Fetch data
        String jsonResponse = fetchStockData(client, apiKey, apiSecret);
        this.series = parseJsonToBarSeries(jsonResponse);
        
        initUI();
    }
    
    private String fetchStockData(OkHttpClient client, String apiKey, String apiSecret) throws IOException {
        ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime start = end.minusDays(30);
        
        HttpUrl url = HttpUrl.parse("https://data.alpaca.markets/v2/stocks/" + symbol + "/bars").newBuilder()
                .addQueryParameter("timeframe", timeframe)
                .addQueryParameter("start", start.format(DateTimeFormatter.ISO_INSTANT))
                .addQueryParameter("end", end.format(DateTimeFormatter.ISO_INSTANT))
                .addQueryParameter("limit", "200")
                .addQueryParameter("adjustment", "raw")
                .addQueryParameter("feed", "iex")
                .addQueryParameter("sort", "asc")
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("APCA-API-KEY-ID", apiKey)
                .addHeader("APCA-API-SECRET-KEY", apiSecret)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            return response.body().string();
        }
    }
    
    private BarSeries parseJsonToBarSeries(String jsonResponse) {
        BarSeries series = new BaseBarSeriesBuilder().withName(symbol).build();
        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray bars = json.getJSONArray("bars");
            
            for (int i = 0; i < bars.length(); i++) {
                JSONObject bar = bars.getJSONObject(i);
                ZonedDateTime time = ZonedDateTime.parse(bar.getString("t"), DateTimeFormatter.ISO_ZONED_DATE_TIME);
                series.addBar(time, 
                    bar.getDouble("o"), 
                    bar.getDouble("h"), 
                    bar.getDouble("l"), 
                    bar.getDouble("c"), 
                    bar.getLong("v"));
            }
        } catch (JSONException e) {
            throw new RuntimeException("Error parsing JSON for symbol " + symbol, e);
        }
        return series;
    }
    
    private void initUI() {
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Main Chart with Price + Selected Indicator
        tabbedPane.addTab("Price + " + indicatorType, createMainChartPanel());
        
        // Individual indicator tabs
        tabbedPane.addTab("Price", createPriceChartPanel());
        
        switch(indicatorType) {
            case "RSI":
                tabbedPane.addTab("RSI", createRSIChartPanel());
                break;
            case "MACD":
                tabbedPane.addTab("MACD", createMACDChartPanel());
                break;
            case "TREND":
                tabbedPane.addTab("Moving Averages", createMATrendChartPanel());
                break;
            case "PSAR_001":
            case "PSAR_005":
                tabbedPane.addTab("PSAR", createPSARChartPanel());
                break;
        }
        
        tabbedPane.addTab("Volume", createVolumeChartPanel());
        add(tabbedPane);
    }

    private JPanel createMainChartPanel() {
        OHLCDataset ohlcDataset = createOHLCDataset();
        JFreeChart chart = ChartFactory.createCandlestickChart(
            symbol + " " + timeframe + " - " + indicatorType, 
            "Date", 
            "Price", 
            ohlcDataset, 
            false);
        
        XYPlot plot = chart.getXYPlot();
        
        // Add selected indicator to main chart
        switch(indicatorType) {
            case "RSI":
                addRSIToPlot(plot);
                break;
            case "MACD":
                addMACDToPlot(plot);
                break;
            case "TREND":
                addMovingAveragesToPlot(plot);
                break;
            case "PSAR_001":
                addPSARToPlot(plot, 0.01);
                break;
            case "PSAR_005":
                addPSARToPlot(plot, 0.05);
                break;
            case "ACTION":
                addAllIndicatorsToPlot(plot);
                break;
        }
        
        customizePriceChart(plot);
        return new ChartPanel(chart);
    }

    private void addRSIToPlot(XYPlot mainPlot) {
        RSIIndicator rsi = new RSIIndicator(new ClosePriceIndicator(series), 14);
        XYSeries rsiSeries = new XYSeries("RSI");
        
        for (int i = 0; i < series.getBarCount(); i++) {
            rsiSeries.add(i, rsi.getValue(i).doubleValue());
        }
        
        XYDataset rsiDataset = new XYSeriesCollection(rsiSeries);
        int datasetIndex = 1;
        mainPlot.setDataset(datasetIndex, rsiDataset);
        
        // Create secondary axis
        NumberAxis rsiAxis = new NumberAxis("RSI");
        rsiAxis.setRange(0, 100);
        int axisIndex = 1;
        mainPlot.setRangeAxis(axisIndex, rsiAxis);
        mainPlot.mapDatasetToRangeAxis(datasetIndex, axisIndex);
        
        // Configure renderer
        XYLineAndShapeRenderer rsiRenderer = new XYLineAndShapeRenderer();
        rsiRenderer.setSeriesPaint(0, Color.BLUE);
        rsiRenderer.setSeriesStroke(0, new BasicStroke(1.5f));
        mainPlot.setRenderer(datasetIndex, rsiRenderer);
        
        // CORRECT WAY TO ADD RANGE MARKERS:
        ValueMarker overbought = new ValueMarker(70);
        overbought.setPaint(Color.RED);
        overbought.setStroke(new BasicStroke(1));
        mainPlot.addRangeMarker(overbought);  // Adds to primary range axis by default
        
        ValueMarker oversold = new ValueMarker(30);
        oversold.setPaint(Color.GREEN);
        oversold.setStroke(new BasicStroke(1));
        mainPlot.addRangeMarker(oversold);
        
        ValueMarker middle = new ValueMarker(50);
        middle.setPaint(Color.GRAY);
        middle.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, 
            BasicStroke.JOIN_MITER, 1.0f, new float[] {5.0f, 5.0f}, 0.0f));
        mainPlot.addRangeMarker(middle);
    }

    private void addMACDToPlot(XYPlot mainPlot) {
        MACDIndicator macd = new MACDIndicator(new ClosePriceIndicator(series), 12, 26);
        EMAIndicator signal = new EMAIndicator(macd, 9);
        
        XYSeries macdSeries = new XYSeries("MACD");
        XYSeries signalSeries = new XYSeries("Signal");
        
        for (int i = 0; i < series.getBarCount(); i++) {
            macdSeries.add(i, macd.getValue(i).doubleValue());
            signalSeries.add(i, signal.getValue(i).doubleValue());
        }
        
        // Create dataset
        XYSeriesCollection macdDataset = new XYSeriesCollection();
        macdDataset.addSeries(macdSeries);
        macdDataset.addSeries(signalSeries);
        
        // Add dataset to plot (secondary dataset)
        int datasetIndex = 1;
        mainPlot.setDataset(datasetIndex, macdDataset);
        
        // Create and configure MACD axis
        NumberAxis macdAxis = new NumberAxis("MACD");
        int axisIndex = 1; // Secondary axis index
        mainPlot.setRangeAxis(axisIndex, macdAxis);
        mainPlot.mapDatasetToRangeAxis(datasetIndex, axisIndex);
        
        // Configure renderers
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);  // MACD line (series 0)
        renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        renderer.setSeriesPaint(1, Color.RED);   // Signal line (series 1)
        renderer.setSeriesStroke(1, new BasicStroke(1.0f));
        mainPlot.setRenderer(datasetIndex, renderer);
        
        // CORRECT WAY TO ADD ZERO LINE MARKER (using Layer)
        ValueMarker zeroLine = new ValueMarker(0, Color.BLACK, new BasicStroke(1));
        
        // First ensure we have the Layer class imported
        // import org.jfree.chart.plot.Layer;
        mainPlot.addRangeMarker(zeroLine, Layer.BACKGROUND);
        
        // If Layer is not available, use this alternative:
        // mainPlot.addRangeMarker(zeroLine); // Defaults to background layer
        
        // Customize appearance
        macdAxis.setAutoRangeIncludesZero(false); // Don't force zero to be visible
    }
    
    private void addMovingAveragesToPlot(XYPlot plot) {
        XYDataset ma50Dataset = createMovingAverageDataset(50);
        plot.setDataset(1, ma50Dataset);
        XYLineAndShapeRenderer ma50Renderer = new XYLineAndShapeRenderer();
        ma50Renderer.setSeriesPaint(0, Color.BLUE);
        ma50Renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        plot.setRenderer(1, ma50Renderer);
        
        XYDataset ma200Dataset = createMovingAverageDataset(200);
        plot.setDataset(2, ma200Dataset);
        XYLineAndShapeRenderer ma200Renderer = new XYLineAndShapeRenderer();
        ma200Renderer.setSeriesPaint(0, Color.RED);
        ma200Renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        plot.setRenderer(2, ma200Renderer);
    }

    private void addPSARToPlot(XYPlot plot, double af) {
        ParabolicSarIndicator psar = new ParabolicSarIndicator(series, 
            series.numOf(af), series.numOf(af));
        XYSeries psarSeries = new XYSeries("PSAR " + af);
        
        for (int i = 0; i < series.getBarCount(); i++) {
            double value = psar.getValue(i).doubleValue();
            if (!Double.isInfinite(value) && !Double.isNaN(value)) {
                psarSeries.add(i, value);
            }
        }
        
        XYDataset psarDataset = new XYSeriesCollection(psarSeries);
        plot.setDataset(1, psarDataset);
        
        XYLineAndShapeRenderer psarRenderer = new XYLineAndShapeRenderer();
        psarRenderer.setSeriesPaint(0, af == 0.01 ? Color.GREEN : Color.MAGENTA);
        psarRenderer.setSeriesShape(0, new Ellipse2D.Double(-3, -3, 6, 6));
        psarRenderer.setSeriesStroke(0, new BasicStroke(0));
        plot.setRenderer(1, psarRenderer);
    }

    private void addAllIndicatorsToPlot(XYPlot plot) {
        addMovingAveragesToPlot(plot);
        addRSIToPlot(plot);
        addMACDToPlot(plot);
        addPSARToPlot(plot, 0.01);
        addPSARToPlot(plot, 0.05);
    }

    private JPanel createPriceChartPanel() {
        OHLCDataset ohlcDataset = createOHLCDataset();
        JFreeChart chart = ChartFactory.createCandlestickChart(
            symbol + " Price", "Date", "Price", ohlcDataset, false);
        
        XYPlot plot = chart.getXYPlot();
        customizePriceChart(plot);
        return new ChartPanel(chart);
    }

    private JPanel createRSIChartPanel() {
        RSIIndicator rsi = new RSIIndicator(new ClosePriceIndicator(series), 14);
        XYSeries rsiSeries = new XYSeries("RSI");
        
        for (int i = 0; i < series.getBarCount(); i++) {
            rsiSeries.add(i, rsi.getValue(i).doubleValue());
        }
        
        XYDataset dataset = new XYSeriesCollection(rsiSeries);
        JFreeChart chart = ChartFactory.createXYLineChart(
            "RSI (14)", "Time", "RSI Value", dataset);
        
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        
        plot.addRangeMarker(new ValueMarker(70, Color.RED, new BasicStroke(1)));
        plot.addRangeMarker(new ValueMarker(30, Color.GREEN, new BasicStroke(1)));
        plot.addRangeMarker(new ValueMarker(50, Color.GRAY, new BasicStroke(1, BasicStroke.CAP_BUTT, 
            BasicStroke.JOIN_MITER, 1.0f, new float[] {5.0f, 5.0f}, 0.0f)));
        
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(0, 102, 204));
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        
        return new ChartPanel(chart);
    }

    private JPanel createMACDChartPanel() {
        MACDIndicator macd = new MACDIndicator(new ClosePriceIndicator(series), 12, 26);
        EMAIndicator signal = new EMAIndicator(macd, 9);
        
        XYSeries macdSeries = new XYSeries("MACD");
        XYSeries signalSeries = new XYSeries("Signal");
        
        for (int i = 0; i < series.getBarCount(); i++) {
            macdSeries.add(i, macd.getValue(i).doubleValue());
            signalSeries.add(i, signal.getValue(i).doubleValue());
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(macdSeries);
        dataset.addSeries(signalSeries);
        
        JFreeChart chart = ChartFactory.createXYLineChart(
            "MACD (12,26,9)", "Time", "Value", dataset);
        
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        
        XYLineAndShapeRenderer macdRenderer = new XYLineAndShapeRenderer();
        macdRenderer.setSeriesPaint(0, Color.BLUE);
        macdRenderer.setSeriesStroke(0, new BasicStroke(2.0f));
        
        XYLineAndShapeRenderer signalRenderer = new XYLineAndShapeRenderer();
        signalRenderer.setSeriesPaint(0, Color.RED);
        signalRenderer.setSeriesStroke(0, new BasicStroke(1.5f));
        
        plot.setRenderer(0, macdRenderer);
        plot.setRenderer(1, signalRenderer);
        
        plot.addRangeMarker(new ValueMarker(0, Color.BLACK, new BasicStroke(1)));
        
        return new ChartPanel(chart);
    }

    private JPanel createMATrendChartPanel() {
        OHLCDataset ohlcDataset = createOHLCDataset();
        JFreeChart chart = ChartFactory.createCandlestickChart(
            symbol + " Moving Averages", "Date", "Price", ohlcDataset, false);
        
        XYPlot plot = chart.getXYPlot();
        addMovingAveragesToPlot(plot);
        customizePriceChart(plot);
        
        chart.addSubtitle(new TextTitle("Blue: SMA 50   Red: SMA 200", 
            new Font("SansSerif", Font.PLAIN, 12)));
        
        return new ChartPanel(chart);
    }

    private JPanel createPSARChartPanel() {
        OHLCDataset ohlcDataset = createOHLCDataset();
        JFreeChart chart = ChartFactory.createCandlestickChart(
            symbol + " Parabolic SAR", "Date", "Price", ohlcDataset, false);
        
        XYPlot plot = chart.getXYPlot();
        addPSARToPlot(plot, 0.01);
        addPSARToPlot(plot, 0.05);
        customizePriceChart(plot);
        
        chart.addSubtitle(new TextTitle("Green: PSAR(0.01)   Magenta: PSAR(0.05)", 
            new Font("SansSerif", Font.PLAIN, 12)));
        
        return new ChartPanel(chart);
    }

    private JPanel createVolumeChartPanel() {
        TimeSeries volumeSeries = new TimeSeries("Volume");
        for (int i = 0; i < series.getBarCount(); i++) {
            Bar bar = series.getBar(i);
            RegularTimePeriod period = new Millisecond(Date.from(bar.getEndTime().toInstant()));
            volumeSeries.add(period, bar.getVolume().doubleValue());
        }
        TimeSeriesCollection volumeDataset = new TimeSeriesCollection(volumeSeries);

        TimeSeries volumeMASeries = new TimeSeries("Volume MA(20)");
        SMAIndicator volumeMA = new SMAIndicator(new VolumeIndicator(series), 20);
        for (int i = 0; i < series.getBarCount(); i++) {
            Bar bar = series.getBar(i);
            RegularTimePeriod period = new Millisecond(Date.from(bar.getEndTime().toInstant()));
            volumeMASeries.add(period, volumeMA.getValue(i).doubleValue());
        }
        TimeSeriesCollection volumeMADataset = new TimeSeriesCollection(volumeMASeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            symbol + " Volume", 
            "Date", 
            "Volume", 
            volumeDataset,
            true, 
            true, 
            false
        );

        XYPlot plot = chart.getXYPlot();
        XYBarRenderer volumeRenderer = new XYBarRenderer();
        volumeRenderer.setSeriesPaint(0, new Color(79, 129, 189));
        volumeRenderer.setShadowVisible(false);
        plot.setRenderer(volumeRenderer);
        
        plot.setDataset(1, volumeMADataset);
        XYLineAndShapeRenderer maRenderer = new XYLineAndShapeRenderer();
        maRenderer.setSeriesPaint(0, Color.RED);
        maRenderer.setSeriesStroke(0, new BasicStroke(1.5f));
        plot.setRenderer(1, maRenderer);

        plot.setBackgroundPaint(Color.WHITE);
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MMM-dd"));
        
        return new ChartPanel(chart);
    }

    private OHLCDataset createOHLCDataset() {
        int count = series.getBarCount();
        OHLCDataItem[] data = new OHLCDataItem[count];
        
        for (int i = 0; i < count; i++) {
            Bar bar = series.getBar(i);
            data[i] = new OHLCDataItem(
                Date.from(bar.getEndTime().toInstant()),
                bar.getOpenPrice().doubleValue(),
                bar.getHighPrice().doubleValue(),
                bar.getLowPrice().doubleValue(),
                bar.getClosePrice().doubleValue(),
                bar.getVolume().doubleValue()
            );
        }
        return new DefaultOHLCDataset(symbol, data);
    }

    private XYDataset createMovingAverageDataset(int barCount) {
        SMAIndicator sma = new SMAIndicator(new ClosePriceIndicator(series), barCount);
        XYSeries maSeries = new XYSeries("SMA " + barCount);
        
        for (int i = 0; i < series.getBarCount(); i++) {
            maSeries.add(i, sma.getValue(i).doubleValue());
        }
        
        return new XYSeriesCollection(maSeries);
    }

    private void customizePriceChart(XYPlot plot) {
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        CandlestickRenderer renderer = (CandlestickRenderer) plot.getRenderer();
        renderer.setUpPaint(Color.GREEN);
        renderer.setDownPaint(Color.RED);
        renderer.setUseOutlinePaint(true);
        
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MMM-dd"));
    }

    public static void showChart(String symbol, String timeframe, String indicatorType,
                               OkHttpClient client, String apiKey, String apiSecret) {
        SwingUtilities.invokeLater(() -> {
            try {
                ChartWindow window = new ChartWindow(symbol, timeframe, indicatorType, 
                                                   client, apiKey, apiSecret);
                window.setVisible(true);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                    "Failed to load chart: " + e.getMessage(),
                    "Chart Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}