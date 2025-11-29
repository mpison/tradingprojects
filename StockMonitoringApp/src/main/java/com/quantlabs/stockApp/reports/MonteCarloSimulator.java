package com.quantlabs.stockApp.reports;

//MonteCarloSimulator.java
//This class handles the data fetching and computation for Monte Carlo simulation.
//It can be run independently to compute and print data, but is designed to be used by the graph app.

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MonteCarloSimulator {

	private final OkHttpClient client = new OkHttpClient();

	public Map<String, Map<ZonedDateTime, Double[]>> computeData(List<String> symbols, ZonedDateTime start,
			ZonedDateTime end) {
		Map<String, Map<ZonedDateTime, Double[]>> symbolData = new ConcurrentHashMap<>();
		for (String symbol : symbols) {
			try {
				BarSeries series = getBarSeries(symbol, "1Min", start, end);
				// Note: WickCleanerUtil.cleanBarSeries(series) is skipped as it's not provided
				// in the original code.
				Map<ZonedDateTime, Double> cumReturns = calculateCumulativeReturns(series);
				Map<ZonedDateTime, Double[]> dataMap = new HashMap<>();
				for (int i = 0; i < series.getBarCount(); i++) {
					Bar bar = series.getBar(i);
					ZonedDateTime time = bar.getEndTime();
					double volume = bar.getVolume().doubleValue();
					if (cumReturns.containsKey(time)) {
						dataMap.put(time, new Double[] { cumReturns.get(time), volume });
					}
				}
				if (!dataMap.isEmpty()) {
					symbolData.put(symbol, dataMap);
				} else {
					System.out.println("Skipping " + symbol + ": no valid data");
				}
				System.out.println(
						"Loaded " + symbol + " (" + (symbols.indexOf(symbol) + 1) + "/" + symbols.size() + ")");
			} catch (Exception e) {
				System.out.println("Error processing " + symbol + ": " + e.getMessage());
			}
		}
		return symbolData;
	}

	private BarSeries getBarSeries(String symbol, String timeframe, ZonedDateTime start, ZonedDateTime end) {
		long startTimestamp = start.toEpochSecond();
		long endTimestamp = end.toEpochSecond();
		String interval = timeframe.toLowerCase().replace("min", "m");
		String baseUrl = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol;
		HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
		urlBuilder.addQueryParameter("period1", String.valueOf(startTimestamp));
		urlBuilder.addQueryParameter("period2", String.valueOf(endTimestamp));
		urlBuilder.addQueryParameter("interval", interval);
		urlBuilder.addQueryParameter("includePrePost", "true");
		String url = urlBuilder.build().toString();
		Request request = new Request.Builder().url(url).build();
		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected code " + response);
			}
			String jsonString = response.body().string();
			JSONObject json = new JSONObject(jsonString);
			JSONObject chart = json.getJSONObject("chart");
			if (chart.has("error") && !chart.isNull("error")) {
				throw new JSONException("Yahoo API error: " + chart.getJSONObject("error").getString("description"));
			}
			JSONArray resultArray = chart.getJSONArray("result");
			if (resultArray.length() == 0) {
				throw new JSONException("No result in Yahoo response");
			}
			JSONObject resultObject = resultArray.getJSONObject(0);
			JSONArray timestampArray = resultObject.getJSONArray("timestamp");
			JSONObject indicators = resultObject.getJSONObject("indicators");
			JSONObject quoteObject = indicators.getJSONArray("quote").getJSONObject(0);
			JSONArray openArray = quoteObject.getJSONArray("open");
			JSONArray highArray = quoteObject.getJSONArray("high");
			JSONArray lowArray = quoteObject.getJSONArray("low");
			JSONArray closeArray = quoteObject.getJSONArray("close");
			JSONArray volumeArray = quoteObject.getJSONArray("volume");
			BarSeries series = new BaseBarSeriesBuilder().withName(symbol).build();
			for (int i = 0; i < timestampArray.length(); i++) {
				if (openArray.isNull(i) || highArray.isNull(i) || lowArray.isNull(i) || closeArray.isNull(i))
					continue;
				long ts = timestampArray.getLong(i) * 1000;
				ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
				double o = openArray.getDouble(i);
				double h = highArray.getDouble(i);
				double l = lowArray.getDouble(i);
				double c = closeArray.getDouble(i);
				double v = volumeArray.isNull(i) ? 0 : volumeArray.getDouble(i);
				series.addBar(dateTime, o, h, l, c, v);
			}
			return series;
		} catch (Exception e) {
			System.out.println("Error fetching data for " + symbol + ": " + e.getMessage());
			return new BaseBarSeriesBuilder().withName(symbol).build();
		}
	}

	private Map<ZonedDateTime, Double> calculateCumulativeReturns(BarSeries series) {
		Map<ZonedDateTime, Double> cumulativeReturns = new LinkedHashMap<>();
		if (series.getBarCount() < 2) {
			System.out.println("Insufficient data for Monte Carlo simulation: " + series.getName() + " has "
					+ series.getBarCount() + " bars");
			return cumulativeReturns;
		}
		double cumulative = 0;
		cumulativeReturns.put(series.getBar(0).getEndTime(), 0.0);
		for (int i = 1; i < series.getBarCount(); i++) {
			double prevClose = series.getBar(i - 1).getClosePrice().doubleValue();
			double currentClose = series.getBar(i).getClosePrice().doubleValue();
			ZonedDateTime time = series.getBar(i).getEndTime();
			if (prevClose <= 0 || currentClose <= 0 || Double.isNaN(prevClose) || Double.isNaN(currentClose)) {
				System.out.println("Invalid price data for " + series.getName() + " at index " + i + ": prevClose="
						+ prevClose + ", currentClose=" + currentClose);
				continue;
			}
			double returnPct = (currentClose - prevClose) / prevClose * 100;
			if (Double.isNaN(returnPct) || Double.isInfinite(returnPct)) {
				System.out.println(
						"Invalid return percentage for " + series.getName() + " at index " + i + ": " + returnPct);
				continue;
			}
			cumulative += returnPct;
			if (Double.isNaN(cumulative) || Double.isInfinite(cumulative)) {
				System.out.println(
						"Non-finite cumulative return for " + series.getName() + " at index " + i + ": " + cumulative);
				continue;
			}
			cumulativeReturns.put(time, cumulative);
		}
		if (cumulativeReturns.size() <= 1) {
			System.out.println("No valid cumulative returns calculated for " + series.getName());
		}
		return cumulativeReturns;
	}

	public static void main(String[] args) {
		List<String> symbols = Arrays.asList("NVDA", "AAPL");
		ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC);
		ZonedDateTime start = end.minusDays(7);
		MonteCarloSimulator simulator = new MonteCarloSimulator();
		Map<String, Map<ZonedDateTime, Double[]>> data = simulator.computeData(symbols, start, end);
		// Print sample output
		data.forEach((symbol, map) -> {
			System.out.println("Data for " + symbol + ": " + map.size() + " entries");
		});
	}
}