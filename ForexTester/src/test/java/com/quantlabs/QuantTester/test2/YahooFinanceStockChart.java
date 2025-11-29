package com.quantlabs.QuantTester.test2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.SegmentedTimeline;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Day;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class YahooFinanceStockChart {

	private static final OkHttpClient client = new OkHttpClient();
	
	private static final int MARKET_OPEN_HOUR = 9;
    private static final int MARKET_OPEN_MINUTE = 30;
    private static final int MARKET_CLOSE_HOUR = 16;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("Yahoo Finance Chart");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(1200, 800);

			String symbol = "AAPL";
			long endTime = System.currentTimeMillis() / 1000;
			long startTime = endTime - (90 * 24 * 60 * 60); // 90 days ago

			try {
				JSONObject json = fetchYahooData(symbol, startTime, endTime);
				OHLCSeriesCollection dataset = parseResponseToOHLC(json);

				JFreeChart chart = createChart(symbol, dataset);
				frame.add(new ChartPanel(chart), BorderLayout.CENTER);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			frame.setVisible(true);
		});
	}

	private static JSONObject fetchYahooData(String symbol, long period1, long period2)
			throws IOException, JSONException {
		HttpUrl url = HttpUrl.parse("https://query1.finance.yahoo.com/v8/finance/chart/" + symbol).newBuilder()
				.addQueryParameter("interval", "1d") // Daily data
				.addQueryParameter("period1", String.valueOf(period1))
				.addQueryParameter("period2", String.valueOf(period2)).build();

		Request request = new Request.Builder().url(url).get().addHeader("accept", "application/json")
				.addHeader("User-Agent", "Mozilla/5.0").build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response.code() + ": "
						+ (response.body() != null ? response.body().string() : ""));
			}
			return new JSONObject(response.body().string());
		}
	}

	private static OHLCSeriesCollection parseResponseToOHLC(JSONObject json) throws JSONException {
		JSONObject chart = json.getJSONObject("chart");
		JSONArray results = chart.getJSONArray("result");
		JSONObject result = results.getJSONObject(0);
		JSONObject indicators = result.getJSONObject("indicators");
		JSONObject quote = indicators.getJSONArray("quote").getJSONObject(0);

		JSONArray timestamps = result.getJSONArray("timestamp");
		JSONArray opens = quote.getJSONArray("open");
		JSONArray highs = quote.getJSONArray("high");
		JSONArray lows = quote.getJSONArray("low");
		JSONArray closes = quote.getJSONArray("close");

		OHLCSeries series = new OHLCSeries("Stock Data");

		for (int i = 0; i < timestamps.length(); i++) {
			long timestamp = timestamps.getLong(i) * 1000; // Convert to milliseconds
			Date date = new Date(timestamp);
			Day day = new Day(date);

			double open = opens.getDouble(i);
			double high = highs.getDouble(i);
			double low = lows.getDouble(i);
			double close = closes.getDouble(i);

			series.add(day, open, high, low, close);
		}

		OHLCSeriesCollection dataset = new OHLCSeriesCollection();
		dataset.addSeries(series);
		return dataset;
	}

	private static JFreeChart createChart(String title, OHLCSeriesCollection dataset) {
		JFreeChart chart = ChartFactory.createCandlestickChart(title, "Date/Time", "Price ($)", dataset, true);

		XYPlot plot = chart.getXYPlot();

		// Configure date axis
		DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
		dateAxis.setDateFormatOverride(new SimpleDateFormat("MMM d HH:mm"));

		// Create timeline that excludes non-trading hours and weekends
		SegmentedTimeline timeline = createMarketHoursTimeline();
		dateAxis.setTimeline(timeline);

		// Visual styling
		plot.setBackgroundPaint(Color.WHITE);
		plot.setDomainGridlinePaint(new Color(240, 240, 240));
		plot.setRangeGridlinePaint(new Color(240, 240, 240));

		return chart;
	}

	private static SegmentedTimeline createMarketHoursTimeline() {
		// Base timeline with 1 hour segments
		SegmentedTimeline timeline = new SegmentedTimeline(SegmentedTimeline.HOUR_SEGMENT_SIZE, 5, // 7 days in week
				2); // 2 weekend days to exclude

		// Exclude non-market hours (before 9:30 AM and after 4 PM)
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		// Exclude hours before market open
		for (int hour = 0; hour < MARKET_OPEN_HOUR; hour++) {
			cal.set(Calendar.HOUR_OF_DAY, hour);
			timeline.addException(cal.getTime());
		}

		// Exclude minutes before market open (9:00-9:29)
		cal.set(Calendar.HOUR_OF_DAY, MARKET_OPEN_HOUR);
		for (int minute = 0; minute < MARKET_OPEN_MINUTE; minute++) {
			cal.set(Calendar.MINUTE, minute);
			timeline.addException(cal.getTime());
		}

		// Exclude hours after market close
		cal.set(Calendar.HOUR_OF_DAY, MARKET_CLOSE_HOUR);
		for (int hour = MARKET_CLOSE_HOUR + 1; hour < 24; hour++) {
			cal.set(Calendar.HOUR_OF_DAY, hour);
			timeline.addException(cal.getTime());
		}
		
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
		timeline.addException(cal.getTime());
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		timeline.addException(cal.getTime());
		

		return timeline;
	}
}