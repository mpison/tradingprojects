package com.quantlabs.QuantTester;

import org.jfree.chart.*;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.*;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.jfree.data.xy.*;

import javax.swing.*;
import javax.swing.Timer;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class ForexChart_MACD_PSAR extends JFrame {
	private OHLCSeries ohlcSeries = new OHLCSeries("EUR/USD");
	private TimeSeries macdLineSeries = new TimeSeries("MACD Line");
	private TimeSeries signalSeries = new TimeSeries("Signal Line");
	private TimeSeries histogramSeries = new TimeSeries("Histogram");
	private TimeSeries psarSeries = new TimeSeries("PSAR");

	private TimeSeriesCollection macdLineDataset = new TimeSeriesCollection();
	private TimeSeriesCollection signalDataset = new TimeSeriesCollection();
	private TimeSeriesCollection histogramDataset = new TimeSeriesCollection();
	private TimeSeriesCollection psarDataset = new TimeSeriesCollection();

	private DateAxis sharedAxis;
	private JFreeChart priceChart;
	private JFreeChart macdChart;
	private JLabel crosshairLabel;

	public ForexChart_MACD_PSAR() {
		setTitle("Forex Chart with MACD and PSAR");
		setSize(1200, 900);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		macdLineDataset.addSeries(macdLineSeries);
		signalDataset.addSeries(signalSeries);
		histogramDataset.addSeries(histogramSeries);
		psarDataset.addSeries(psarSeries);
		sharedAxis = new DateAxis("Time");
		sharedAxis.setDateFormatOverride(new SimpleDateFormat("dd/MM HH:mm"));

		loadSampleData();

		priceChart = createPriceChart();
		macdChart = createMACDChart();

		ChartPanel pricePanel = new ChartPanel(priceChart);
		ChartPanel macdPanel = new ChartPanel(macdChart);
		syncCrosshairsAndValues(pricePanel, macdChart);

		JPanel panel = new JPanel(new GridLayout(3, 1));
		panel.add(pricePanel);
		panel.add(macdPanel);

		crosshairLabel = new JLabel("Hover over the chart to see values.");
		crosshairLabel.setFont(new Font("Monospaced", Font.PLAIN, 14));
		crosshairLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		panel.add(crosshairLabel);

		setContentPane(panel);
	}

	private JFreeChart createPriceChart() {
		OHLCSeriesCollection dataset = new OHLCSeriesCollection();
		dataset.addSeries(ohlcSeries);

		JFreeChart chart = ChartFactory.createCandlestickChart("EUR/USD", "Time", "Price", dataset, false);
		XYPlot plot = chart.getXYPlot();
		plot.setDomainAxis(sharedAxis);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);

		plot.setDataset(1, psarDataset);
		XYLineAndShapeRenderer psarRenderer = new XYLineAndShapeRenderer(false, true);
		psarRenderer.setSeriesPaint(0, Color.RED);
		plot.setRenderer(1, psarRenderer);

		return chart;
	}

	private JFreeChart createMACDChart() {
		JFreeChart chart = ChartFactory.createTimeSeriesChart("MACD", "Time", "MACD", macdLineDataset, false, false,
				false);
		XYPlot plot = chart.getXYPlot();
		plot.setDomainAxis(sharedAxis);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);

		plot.setDataset(1, signalDataset);
		XYLineAndShapeRenderer signalRenderer = new XYLineAndShapeRenderer(true, false);
		signalRenderer.setSeriesPaint(0, Color.BLUE);
		plot.setRenderer(1, signalRenderer);

		plot.setDataset(2, histogramDataset);
		XYBarRenderer histRenderer = new XYBarRenderer();
		histRenderer.setSeriesPaint(0, Color.GRAY);
		plot.setRenderer(2, histRenderer);

		return chart;
	}
	
	private double[] ema(double[] data, int span) {
        double[] result = new double[data.length];
        double alpha = 2.0 / (span + 1);
        result[0] = data[0];
        for (int i = 1; i < data.length; i++) {
            result[i] = alpha * data[i] + (1 - alpha) * result[i - 1];
        }
        return result;
    }

    private double[] subtract(double[] a, double[] b) {
        double[] r = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = a[i] - b[i];
        }
        return r;
    }

    private double[] calculatePSAR(double[] high, double[] low) {
        int len = high.length;
        double[] psar = new double[len];
        boolean uptrend = true;
        double af = 0.02;
        double maxAF = 0.2;
        double ep = high[0];
        psar[0] = low[0];

        for (int i = 1; i < len; i++) {
            psar[i] = psar[i - 1] + af * (ep - psar[i - 1]);

            if (uptrend) {
                if (high[i] > ep) {
                    ep = high[i];
                    af = Math.min(af + 0.02, maxAF);
                }
                if (low[i] < psar[i]) {
                    uptrend = false;
                    psar[i] = ep;
                    ep = low[i];
                    af = 0.02;
                }
            } else {
                if (low[i] < ep) {
                    ep = low[i];
                    af = Math.min(af + 0.02, maxAF);
                }
                if (high[i] > psar[i]) {
                    uptrend = true;
                    psar[i] = ep;
                    ep = high[i];
                    af = 0.02;
                }
            }
        }
        return psar;
    }


	private void loadSampleData() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 9);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);

		double[] closes = { 1.1000, 1.1020, 1.1045, 1.1030, 1.1055, 1.1080, 1.1070, 1.1095, 1.1110, 1.1100 };
		double[] highs = new double[closes.length];
		double[] lows = new double[closes.length];

		for (int i = 0; i < closes.length; i++) {
			highs[i] = closes[i] + 0.0015;
			lows[i] = closes[i] - 0.0015;
		}

		for (int i = 0; i < closes.length; i++) {
			Date date = cal.getTime();
			double close = closes[i];
			double open = close - 0.0010;
			double high = highs[i];
			double low = lows[i];

			ohlcSeries.add(new Second(date), open, high, low, close);
			cal.add(Calendar.MINUTE, 30);
		}

		double[] ema12 = ema(closes, 12);
		double[] ema26 = ema(closes, 26);
		double[] macdLine = subtract(ema12, ema26);
		double[] signal = ema(macdLine, 9);
		double[] hist = subtract(macdLine, signal);
		double[] psar = calculatePSAR(highs, lows);

		cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 9);

		for (int i = 0; i < closes.length; i++) {
			Date date = cal.getTime();
			RegularTimePeriod t = new Second(date);
			macdLineSeries.add(t, macdLine[i]);
			signalSeries.add(t, signal[i]);
			histogramSeries.add(t, hist[i]);
			psarSeries.add(t, psar[i]);
			cal.add(Calendar.MINUTE, 30);
		}
	}

	private void syncCrosshairsAndValues(ChartPanel pricePanel, JFreeChart macdChart) {
		pricePanel.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				XYPlot pricePlot = priceChart.getXYPlot();
				XYPlot macdPlot = macdChart.getXYPlot();
				ChartRenderingInfo info = pricePanel.getChartRenderingInfo();
				Point2D p = pricePanel.translateScreenToJava2D(e.getPoint());
				Rectangle2D dataArea = info.getPlotInfo().getDataArea();
				double x = pricePlot.getDomainAxis().java2DToValue(p.getX(), dataArea, pricePlot.getDomainAxisEdge());

				pricePlot.setDomainCrosshairValue(x);
				macdPlot.setDomainCrosshairValue(x);

				Date time = new Date((long) x);
				String timeStr = new SimpleDateFormat("dd/MM HH:mm").format(time);

				crosshairLabel.setText("🕒 " + timeStr);
			}
		});
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new ForexChart_MACD_PSAR().setVisible(true));
	}
}