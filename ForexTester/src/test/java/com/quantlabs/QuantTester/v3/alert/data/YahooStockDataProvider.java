package com.quantlabs.QuantTester.v3.alert.data;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.quantlabs.QuantTester.v3.alert.StockDataResult;

public class YahooStockDataProvider implements StockDataProvider {
    private final OkHttpClient client;
    private static final String BASE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/%s";

    public YahooStockDataProvider(OkHttpClient client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public StockDataResult fetchStockData(String symbol, String timeframe, int limit,
                                        ZonedDateTime start, ZonedDateTime end) throws IOException {
        HttpUrl url = buildRequestUrl(symbol, timeframe, start, end);
        Request request = buildRequest(url);
        
        try (Response response = client.newCall(request).execute()) {
            validateResponse(response);
            String responseBody = response.body() != null ? response.body().string() : "";
            return new StockDataResult(responseBody, start, end);
        }
    }

    @Override
    public StockDataResult fetchStockData(String symbol, String timeframe, int limit) throws IOException {
        ZonedDateTime end = ZonedDateTime.now();
        ZonedDateTime start = calculateStartTime(timeframe, end);
        return fetchStockData(symbol, timeframe, limit, start, end);
    }

    private HttpUrl buildRequestUrl(String symbol, String timeframe, 
                                  ZonedDateTime start, ZonedDateTime end) {
        return HttpUrl.parse(String.format(BASE_URL, symbol))
                .newBuilder()
                .addQueryParameter("interval", convertTimeframe(timeframe))
                .addQueryParameter("period1", String.valueOf(start.toEpochSecond()))
                .addQueryParameter("period2", String.valueOf(end.toEpochSecond()))
                .build();
    }

    private Request buildRequest(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .build();
    }

    private void validateResponse(Response response) throws IOException {
        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "";
            throw new IOException(String.format(
                "Yahoo API request failed with code %d: %s", 
                response.code(), errorBody));
        }
    }

    @Override
    public String convertTimeframe(String timeframe) {
        return switch (timeframe.toLowerCase()) {
            case "1min" -> "1m";
            case "5min" -> "5m";
            case "15min" -> "15m";
            case "30min" -> "30m";
            case "1h" -> "1h";
            case "4h" -> "4h"; // Yahoo doesn't have 4h, use 1h as fallback
            case "1d" -> "1d";
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        };
    }

    @Override
    public ZonedDateTime calculateStartTime(String timeframe, ZonedDateTime end) {
        return switch (convertTimeframe(timeframe)) {
            case "1m" -> end.minusDays(7);
            case "5m", "15m", "30m" -> end.minusDays(60);
            case "1h" -> end.minusDays(730); // 2 years
            case "1d" -> end.minusDays(365*5); // 5 years
            default -> end.minusDays(30);
        };
    }

    // Additional helper method for timeframe validation
    public boolean isValidTimeframe(String timeframe) {
        try {
            convertTimeframe(timeframe);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}