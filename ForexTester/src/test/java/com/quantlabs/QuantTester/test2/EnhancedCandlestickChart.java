package com.quantlabs.QuantTester.test2;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.OHLCDataItem;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class EnhancedCandlestickChart extends JFrame {

	private BarSeries series;
	private Crosshair xCrosshair;
	private Crosshair yCrosshair;

	public EnhancedCandlestickChart() {
		super("Technical Analysis Chart with PSAR and MACD");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1200, 800);

		// Create sample data and convert to TA4J BarSeries
		OHLCDataset ohlcDataset = createDataset();
		this.series = convertToBarSeries(ohlcDataset);

		// Create main chart with candlesticks and PSAR
		JFreeChart mainChart = createMainChart(ohlcDataset);

		// Create MACD chart
		JFreeChart macdChart = createMACDChart();

		// Combine both charts
		CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(new DateAxis("Date"));
		combinedPlot.add(mainChart.getXYPlot(), 3); // 3/4 of space
		combinedPlot.add(macdChart.getXYPlot(), 1); // 1/4 of space
		combinedPlot.setGap(10.0);

		JFreeChart combinedChart = new JFreeChart("Technical Analysis", JFreeChart.DEFAULT_TITLE_FONT, combinedPlot,
				true);

		// Create chart panel
		ChartPanel chartPanel = new ChartPanel(combinedChart);
		chartPanel.setMouseWheelEnabled(true);
		chartPanel.setDomainZoomable(true);
		chartPanel.setRangeZoomable(true);

		// Add crosshair (alternative implementation for older JFreeChart)
		setupCrosshairs(chartPanel, mainChart.getXYPlot());

		add(chartPanel, BorderLayout.CENTER);
	}

	private void setupCrosshairs(ChartPanel chartPanel, XYPlot plot) {
		// For older JFreeChart versions:
		// chartPanel.setDomainCrosshairVisible(true);
		// chartPanel.setRangeCrosshairVisible(true);

		// These methods aren't available in older versions:
		// chartPanel.setDomainCrosshairLockedOnData(true);
		// chartPanel.setRangeCrosshairLockedOnData(true);

		// Customize crosshair appearance
		plot.setDomainCrosshairPaint(Color.GRAY);
		plot.setRangeCrosshairPaint(Color.GRAY);
		plot.setDomainCrosshairStroke(new BasicStroke(1f));
		plot.setRangeCrosshairStroke(new BasicStroke(1f));

		// Lock crosshairs to data points
		plot.setDomainCrosshairLockedOnData(true);
		plot.setRangeCrosshairLockedOnData(true);
	}

	private JFreeChart createMainChart(OHLCDataset ohlcDataset) {
		// Create candlestick chart
		JFreeChart chart = ChartFactory.createCandlestickChart("Price with PSAR", null, "Price", ohlcDataset, false);

		XYPlot plot = chart.getXYPlot();

		// Customize candlestick renderer
		CandlestickRenderer renderer = new CandlestickRenderer();
		renderer.setUpPaint(Color.GREEN);
		renderer.setDownPaint(Color.RED);
		renderer.setSeriesPaint(0, Color.BLACK);
		plot.setRenderer(renderer);

		// Add PSAR indicator
		addPSARIndicator(plot);

		// Customize axes
		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(new SimpleDateFormat("MMM-dd-yyyy"));

		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setAutoRangeIncludesZero(false);

		return chart;
	}

	private void addPSARIndicator(XYPlot plot) {
		// Calculate PSAR using TA4J
		ParabolicSarIndicator psar = new ParabolicSarIndicator(series);

		// Convert PSAR values to XYSeries
		XYSeries psarSeries = new XYSeries("PSAR");
		for (int i = 0; i < series.getBarCount(); i++) {
			ZonedDateTime date = series.getBar(i).getEndTime();
			double value = psar.getValue(i).doubleValue();
			psarSeries.add(date.toInstant().toEpochMilli(), value);
		}

		XYSeriesCollection psarDataset = new XYSeriesCollection(psarSeries);
		plot.setDataset(1, psarDataset);

		// Configure PSAR renderer
		XYLineAndShapeRenderer psarRenderer = new XYLineAndShapeRenderer();
		psarRenderer.setSeriesPaint(0, Color.BLUE);
		psarRenderer.setSeriesShapesVisible(0, true);
		psarRenderer.setSeriesShape(0, new Rectangle(3, 3));
		plot.setRenderer(1, psarRenderer);

		// Ensure PSAR is drawn on top of candles
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
	}

	private JFreeChart createMACDChart() {
		// Calculate MACD using TA4J
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		MACDIndicator macd = new MACDIndicator(closePrice);
		EMAIndicator signal = new EMAIndicator(macd, 9);

		// Convert to XYSeries
		XYSeries macdSeries = new XYSeries("MACD");
		XYSeries signalSeries = new XYSeries("Signal");
		XYSeries histogramSeries = new XYSeries("Histogram");

		for (int i = 0; i < series.getBarCount(); i++) {
			ZonedDateTime date = series.getBar(i).getEndTime();
			double macdValue = macd.getValue(i).doubleValue();
			double signalValue = signal.getValue(i).doubleValue();

			macdSeries.add(date.toInstant().toEpochMilli(), macdValue);
			signalSeries.add(date.toInstant().toEpochMilli(), signalValue);
			histogramSeries.add(date.toInstant().toEpochMilli(), macdValue - signalValue);
		}

		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(macdSeries);
		dataset.addSeries(signalSeries);
		dataset.addSeries(histogramSeries);

		// Create chart
		JFreeChart chart = ChartFactory.createXYLineChart("MACD", null, "Value", dataset);

		XYPlot plot = chart.getXYPlot();

		// Customize renderer
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setSeriesPaint(0, Color.BLUE); // MACD line
		renderer.setSeriesPaint(1, Color.RED); // Signal line
		renderer.setSeriesPaint(2, Color.GREEN); // Histogram
		renderer.setSeriesStroke(0, new BasicStroke(2.0f));
		renderer.setSeriesStroke(1, new BasicStroke(1.5f));
		renderer.setSeriesLinesVisible(2, false); // No line for histogram
		renderer.setSeriesShapesVisible(2, true);
		renderer.setSeriesShape(2, new Rectangle(4, 4));

		// Add zero line
		plot.addRangeMarker(new org.jfree.chart.plot.ValueMarker(0));

		return chart;
	}

	private BarSeries convertToBarSeries(OHLCDataset ohlcDataset) {
        List<Bar> bars = new ArrayList<>();
        Duration barDuration = Duration.ofDays(1); // Assuming daily bars
        //NumFactory numFactory = DoubleNum::valueOf;
        
		/*
		 * NumFactory numFactory;
		 * 
		 * for (int i = 0; i < ohlcDataset.getItemCount(0); i++) { Date date = new
		 * Date((long) ohlcDataset.getX(0, i)); Num open =
		 * numFactory.apply(ohlcDataset.getOpenValue(0, i)); Num high =
		 * numFactory.apply(ohlcDataset.getHighValue(0, i)); Num low =
		 * numFactory.apply(ohlcDataset.getLowValue(0, i)); Num close =
		 * numFactory.apply(ohlcDataset.getCloseValue(0, i)); Num volume =
		 * numFactory.apply(ohlcDataset.getVolumeValue(0, i));
		 * 
		 * bars.add(BaseBar.builder() .timePeriod( barDuration) .openPrice(open)
		 * .highPrice(high) .lowPrice(low) .closePrice(close) .volume(volume) .build());
		 * }
		 */
	    return new BaseBarSeriesBuilder()
	        .withBars(bars)
	        .withName("Stock")
	        .withNumTypeOf(DoubleNum.class)
	        .build();
    }

	private OHLCDataset createDataset() {
		// Extended sample data (30-50 data points recommended for indicators)
		OHLCDataItem[] data = new OHLCDataItem[] {
				// Include your OHLC data here
				// Example:
				new OHLCDataItem(new Date(123, 0, 1), 105.0, 109.0, 101.0, 107.0, 10000),
				new OHLCDataItem(new Date(123, 0, 2), 107.0, 112.0, 105.0, 111.0, 12000),
				// ... more data points
		};
		return new DefaultOHLCDataset("Price Data", data);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			new EnhancedCandlestickChart().setVisible(true);
		});
	}
}