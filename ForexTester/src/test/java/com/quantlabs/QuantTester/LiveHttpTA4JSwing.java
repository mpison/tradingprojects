package com.quantlabs.QuantTester;

import java.awt.BorderLayout;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

public class LiveHttpTA4JSwing extends JFrame {
	private static final long serialVersionUID = 1L;

	private static final String API_KEY = System.getenv("APCA_API_KEY_ID");
	private static final String SECRET_KEY = System.getenv("APCA_API_SECRET_KEY");
	private OkHttpClient client = new OkHttpClient();
	private ObjectMapper mapper = new ObjectMapper();

	private List<String> symbols = Arrays.asList("AAPL", "TSLA", "GOOGL", "META", "ORCL", "PLTR", "NFLX", "NVDA");
	private List<String> timeframes = Arrays.asList("1Min", "5Min", "15Min", "30Min", "1Hour", "4Hour", "1Day");
	private Map<String, Integer> intervalMinutes = Map.of("1Min", 1, "5Min", 5, "15Min", 15, "30Min", 30, "1Hour", 60,
			"4Hour", 240, "1Day", 1440);

	private Map<String, BarSeries> seriesMap = new HashMap<>();
	private Map<String, ClosePriceIndicator> closeMap = new HashMap<>();
	private Map<String, RSIIndicator> rsiMap = new HashMap<>();
	private Map<String, MACDIndicator> macdMap = new HashMap<>();
	private Map<String, RSIIndicator> signalMap = new HashMap<>();
	private Map<String, StochasticOscillatorKIndicator> stochKMap = new HashMap<>();
	private Map<String, SMAIndicator> stochDMap = new HashMap<>();
	private Map<String, Double> prevMacd = new HashMap<>();
	private Map<String, Double> prevSignal = new HashMap<>();

	private DefaultTableModel model;

	public LiveHttpTA4JSwing() {
		super("Live Market Indicators");
		model = new DefaultTableModel(
				new String[] { "Symbol", "Timeframe", "RSI", "MACD", "Signal", "StochK", "StochD", "Crossover" }, 0);
		JTable table = new JTable(model);
		add(new JScrollPane(table), BorderLayout.CENTER);
		setSize(900, 400);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		initDataStructures();
		initTableRows();
		startScheduler();
	}

	private void initDataStructures() {
		for (String symbol : symbols) {
			for (String tf : timeframes) {
				String key = symbol + "_" + tf;
				BarSeries series = new BaseBarSeriesBuilder().withName(key + "_series").build();
				seriesMap.put(key, series);
				ClosePriceIndicator close = new ClosePriceIndicator(series);
				closeMap.put(key, close);
				rsiMap.put(key, new RSIIndicator(close, 14));
				macdMap.put(key, new MACDIndicator(close, 12, 26));
				signalMap.put(key, new RSIIndicator(macdMap.get(key), 9));
				stochKMap.put(key, new StochasticOscillatorKIndicator(series, 14));
				stochDMap.put(key, new SMAIndicator(stochKMap.get(key), 3));
				prevMacd.put(key, Double.NaN);
				prevSignal.put(key, Double.NaN);
			}
		}
	}

	private void initTableRows() {
		for (String symbol : symbols) {
			for (String tf : timeframes) {
				model.addRow(new Object[] { symbol, tf, "", "", "", "", "", "" });
			}
		}
	}

	private void startScheduler() {
		ScheduledExecutorService exec = Executors.newScheduledThreadPool(timeframes.size());
		for (String tf : timeframes) {
			int interval = intervalMinutes.get(tf);
			exec.scheduleAtFixedRate(() -> fetchAndUpdate(tf), 0, interval, TimeUnit.MINUTES);
		}
	}

	private void fetchAndUpdate(String tf) {
		for (String symbol : symbols) {
			String url = String.format(
					"https://data.alpaca.markets/v2/stocks/bars?symbols=%s&timeframe=%s&limit=2&adjustment=raw&feed=iex&sort=asc",
					symbol, tf);
			Request request = new Request.Builder().url(url).get().addHeader("accept", "application/json")
					.addHeader("APCA-API-KEY-ID", API_KEY).addHeader("APCA-API-SECRET-KEY", SECRET_KEY).build();
			try (Response response = client.newCall(request).execute()) {
				if (!response.isSuccessful())
					return;
				JsonNode root = mapper.readTree(response.body().string());
				JsonNode bars = root.path("bars").path(symbol);
				String key = symbol + "_" + tf;
				BarSeries series = seriesMap.get(key);
				for (JsonNode bar : bars) {
					ZonedDateTime endTime = ZonedDateTime.parse(bar.get("t").asText(), DateTimeFormatter.ISO_DATE_TIME);
					double open = bar.get("o").asDouble();
					double high = bar.get("h").asDouble();
					double low = bar.get("l").asDouble();
					double close = bar.get("c").asDouble();
					double volume = bar.get("v").asDouble();
					BaseBar newBar = new BaseBar(Duration.ofMinutes(intervalMinutes.get(tf)), endTime,
							series.numOf(open), series.numOf(high), series.numOf(low), series.numOf(close),
							series.numOf(volume), null);
					if (series.getEndIndex() < 0 || !series.getLastBar().getEndTime().equals(endTime)) {
						series.addBar(newBar);
					}
				}
				int idx = series.getEndIndex();
				double rsi = rsiMap.get(key).getValue(idx).doubleValue();
				double macd = macdMap.get(key).getValue(idx).doubleValue();
				double signal = signalMap.get(key).getValue(idx).doubleValue();
				double stochK = stochKMap.get(key).getValue(idx).doubleValue();
				double stochD = stochDMap.get(key).getValue(idx).doubleValue();
				double pM = prevMacd.get(key);
				double pS = prevSignal.get(key);
				String cross = "";
				if (!Double.isNaN(pM) && !Double.isNaN(pS)) {
					if (pM < pS && macd > signal)
						cross = "Bullish";
					else if (pM > pS && macd < signal)
						cross = "Bearish";
				}
				prevMacd.put(key, macd);
				prevSignal.put(key, signal);

				SwingUtilities.invokeLater(() -> {
					for (int row = 0; row < model.getRowCount(); row++) {
						if (model.getValueAt(row, 0).equals(symbol) && model.getValueAt(row, 1).equals(tf)) {
							model.setValueAt(String.format("%.2f", rsi), row, 2);
							model.setValueAt(String.format("%.4f", macd), row, 3);
							model.setValueAt(String.format("%.4f", signal), row, 4);
							model.setValueAt(String.format("%.2f", stochK), row, 5);
							model.setValueAt(String.format("%.2f", stochD), row, 6);
							int col = model.findColumn("Crossover");
							if (row >= 0 && row < model.getRowCount() && col != -1) {
								//model.setValueAt(cross, row, col);
							} else {
								System.err.printf("Cannot update table: invalid row %d or column %d%n", row, col);
							}
							break;
						}
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new LiveHttpTA4JSwing().setVisible(true));
	}
}
