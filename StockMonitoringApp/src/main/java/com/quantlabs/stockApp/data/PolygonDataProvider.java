package com.quantlabs.stockApp.data;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.model.StockDataResult;
import com.quantlabs.stockApp.utils.WickCleanerUtil;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PolygonDataProvider implements StockDataProvider {
    private final OkHttpClient client;
    private final String apiKey;
    private final ConsoleLogger logger;

    public PolygonDataProvider(OkHttpClient client, String apiKey, ConsoleLogger logger) {
        this.client = client;
        this.apiKey = apiKey;
        this.logger = logger;
    }

    @Override
    public BarSeries getHistoricalData(String symbol, String timeframe, int limit, 
                                     ZonedDateTime start, ZonedDateTime end) throws IOException {
        String interval = convertTimeframeToPolygonInterval(timeframe);
        String multiplier = getPolygonMultiplier(timeframe);

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.polygon.io/v2/aggs/ticker/" + symbol + "/range/" + multiplier + "/" + interval + "/" + 
                start.format(DateTimeFormatter.ISO_LOCAL_DATE) + "/" + end.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .newBuilder()
                .addQueryParameter("adjusted", "false")
                .addQueryParameter("sort", "asc") // Changed to "asc" for oldest first
                .addQueryParameter("limit", String.valueOf(Math.min(limit, 50000))) // Max 50000 per documentation
                .addQueryParameter("apiKey", apiKey);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .addHeader("accept", "application/json")
                .build();

        logger.log("Requesting historical data for " + symbol + ": " + urlBuilder.build());

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "No body";
                throw new IOException("Unexpected code " + response + ": " + body);
            }
            String responseBody = response.body().string();
            return parsePolygonJsonToBarSeries(new StockDataResult(responseBody, start, end), symbol, timeframe);
        } catch (JSONException e) {
            throw new IOException("Error parsing JSON response", e);
        }
    }

    @Override
    public PriceData getLatestPrice(String symbol) throws IOException {
        // Fetch snapshot data
        HttpUrl snapshotUrl = HttpUrl.parse("https://api.polygon.io/v3/snapshot")
                .newBuilder()
                .addQueryParameter("ticker", symbol)
                .addQueryParameter("apiKey", apiKey)
                .build();

        Request snapshotRequest = new Request.Builder()
                .url(snapshotUrl)
                .get()
                .addHeader("accept", "application/json")
                .build();

        logger.log("Requesting snapshot for " + symbol + ": " + snapshotUrl);

        double currentPrice;
        double previousClose;
        long currentDayVolume;
        try (Response response = client.newCall(snapshotRequest).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "No body";
                throw new IOException("Unexpected code " + response + ": " + body);
            }

            String responseBody = response.body().string();
            logger.log("Snapshot response for " + symbol + ": " + responseBody);
            JSONObject json = new JSONObject(responseBody);
            JSONArray results = json.getJSONArray("results");
            if (results.length() == 0) {
                throw new IOException("No snapshot data returned for " + symbol);
            }
            JSONObject result = results.getJSONObject(0);

            currentPrice = result.getJSONObject("last_minute").getDouble("close"); // Use last_minute.close
            previousClose = result.getJSONObject("session").getDouble("previous_close");
            currentDayVolume = result.getJSONObject("session").getLong("volume");
        } catch (JSONException e) {
            throw new IOException("Error parsing snapshot JSON for " + symbol + ": " + e.getMessage(), e);
        }

        // Fetch previous day's volume
        long prevDayVolume = fetchPreviousDayVolume(symbol);

        double percentChange = ((currentPrice - previousClose) / previousClose) * 100;
        return new PriceData(currentPrice, prevDayVolume, currentDayVolume, percentChange, 0, previousClose);
    }

    private long fetchPreviousDayVolume(String symbol) throws IOException {
        // Calculate previous trading day
        ZonedDateTime now = ZonedDateTime.now(java.time.ZoneId.of("America/New_York"));
        ZonedDateTime previousDay = now.minusDays(1);
        
        // Adjust for weekends (skip Saturday and Sunday)
        while (previousDay.getDayOfWeek().getValue() >= 6) { // Saturday (6) or Sunday (7)
            previousDay = previousDay.minusDays(1);
        }

        String fromDate = previousDay.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String toDate = fromDate; // Single day

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.polygon.io/v2/aggs/ticker/" + symbol + "/range/1/day/" + fromDate + "/" + toDate)
                .newBuilder()
                .addQueryParameter("adjusted", "false")
                .addQueryParameter("sort", "desc")
                .addQueryParameter("limit", "1") // Need only one day
                .addQueryParameter("apiKey", apiKey);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .addHeader("accept", "application/json")
                .build();

        logger.log("Requesting previous day volume for " + symbol + ": " + urlBuilder.build());

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "No body";
                throw new IOException("Unexpected code " + response + ": " + body);
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            JSONArray results = json.getJSONArray("results");
            if (results.length() == 0) {
                logger.log("No data for previous day volume for " + symbol);
                return 0; // Fallback to 0 if no data
            }
            return results.getJSONObject(0).getLong("v");
        } catch (JSONException e) {
            logger.log("Error parsing previous day volume JSON for " + symbol + ": " + e.getMessage());
            return 0; // Fallback to 0 on error
        }
    }

    private BarSeries parsePolygonJsonToBarSeries(StockDataResult stockData, String symbol, String timeframe) {
        List<JSONObject> allBars = new ArrayList<>();

        try {
            JSONObject json = new JSONObject(stockData.getJsonResponse());

            if (!json.has("results")) {
                throw new JSONException("No 'results' field in response");
            }

            JSONArray bars = json.getJSONArray("results");
            for (int i = 0; i < bars.length(); i++) {
                allBars.add(bars.getJSONObject(i));
            }

            String nextUrl = json.optString("next_url", null);
            while (nextUrl != null) {
                logger.log("Fetching next page for " + symbol + ": " + nextUrl);
                String nextPageData = fetchNextPage(nextUrl);
                JSONObject nextPageJson = new JSONObject(nextPageData);
                JSONArray nextBars = nextPageJson.getJSONArray("results");
                for (int i = 0; i < nextBars.length(); i++) {
                    allBars.add(nextBars.getJSONObject(i));
                }
                nextUrl = nextPageJson.optString("next_url", null);
            }

            BarSeries series = new BaseBarSeriesBuilder().withName(symbol).build();

            for (JSONObject bar : allBars) {
                double open = bar.getDouble("o");
                double high = bar.getDouble("h");
                double low = bar.getDouble("l");
                double close = bar.getDouble("c");
                long volume = bar.getLong("v");
                long timestamp = bar.getLong("t") / 1000; // Polygon uses milliseconds

                if (open <= 0 || high <= 0 || low <= 0 || close <= 0 || volume < 0 ||
                    Double.isNaN(open) || Double.isNaN(high) || Double.isNaN(low) || Double.isNaN(close)) {
                    logger.log("Skipping invalid bar for " + symbol + ": invalid values");
                    continue;
                }

                ZonedDateTime time = ZonedDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(timestamp), 
                    java.time.ZoneId.of("America/New_York")
                );

                series.addBar(time, open, high, low, close, volume);
            }

            if (series.getBarCount() == 0) {
                logger.log("No valid bars parsed for " + symbol);
            }

            return WickCleanerUtil.cleanBarSeries(series);
        } catch (JSONException e) {
            logger.log("Error parsing Polygon JSON for symbol " + symbol + ": " + e.getMessage());
            return new BaseBarSeriesBuilder().withName(symbol).build();
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }

    private String fetchNextPage(String nextUrl) throws IOException {
        Request request = new Request.Builder()
                .url(nextUrl)
                .get()
                .addHeader("accept", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }
            return response.body().string();
        }
    }

    private String convertTimeframeToPolygonInterval(String timeframe) {
        switch (timeframe) {
            case "1Min": return "minute";
            case "5Min": return "minute";
            case "15Min": return "minute";
            case "30Min": return "minute";
            case "1H": return "hour";
            case "4H": return "hour";
            case "1D": return "day";
            case "1W": return "week";
            default: return "day";
        }
    }

    private String getPolygonMultiplier(String timeframe) {
        switch (timeframe) {
            case "1Min": return "1";
            case "5Min": return "5";
            case "15Min": return "15";
            case "30Min": return "30";
            case "1H": return "1";
            case "4H": return "4";
            case "1D": return "1";
            case "1W": return "1";
            default: return "1";
        }
    }

    @Override
    public StockDataResult fetchStockData(String symbol, String timeframe, int limit,
                                        ZonedDateTime start, ZonedDateTime end) throws IOException {
        if (start == null) {
            start = calculateDefaultStartTime(timeframe, end);
        }

        String interval = convertTimeframeToPolygonInterval(timeframe);
        String multiplier = getPolygonMultiplier(timeframe);

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.polygon.io/v2/aggs/ticker/" + symbol + "/range/" + multiplier + "/" + interval + "/" + 
                start.format(DateTimeFormatter.ISO_LOCAL_DATE) + "/" + end.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .newBuilder()
                .addQueryParameter("adjusted", "false")
                .addQueryParameter("sort", "asc") // Changed to "asc" for oldest first
                .addQueryParameter("limit", String.valueOf(Math.min(limit, 50000)))
                .addQueryParameter("apiKey", apiKey);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .addHeader("accept", "application/json")
                .build();

        logger.log("Requesting stock data for " + symbol + ": " + urlBuilder.build());

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String body = response.body() != null ? response.body().string() : "No body";
                throw new IOException("Unexpected code " + response + ": " + body);
            }
            return new StockDataResult(response.body().string(), start, end);
        }
    }

    public ZonedDateTime calculateDefaultStartTime(String timeframe, ZonedDateTime end) {
        Map<String, Integer> timeLimits = Map.of(
            "1Min", 7, "5Min", 30, "15Min", 30, "30Min", 60, "1H", 60, "4H", 90, "1D", 730, "1W", 730
        );
        Integer days = timeLimits.getOrDefault(timeframe, 7);
        return end.minusDays(days);
    }

	@Override
	public PriceData getPriceData(String symbol, ZonedDateTime start, ZonedDateTime end) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}