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

public class AlpacaStockDataProvider implements StockDataProvider {
    private final OkHttpClient client;
    private final String apiKey;
    private final String apiSecret;
    private static final String BASE_URL = "https://data.alpaca.markets/v2/stocks/%s/bars";

    public AlpacaStockDataProvider(OkHttpClient client, String apiKey, String apiSecret) {
        this.client = Objects.requireNonNull(client);
        this.apiKey = Objects.requireNonNull(apiKey);
        this.apiSecret = Objects.requireNonNull(apiSecret);
    }

    @Override
    public StockDataResult fetchStockData(String symbol, String timeframe, int limit,
                                        ZonedDateTime start, ZonedDateTime end) throws IOException {
        HttpUrl url = buildRequestUrl(symbol, timeframe, limit, start, end);
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

    private HttpUrl buildRequestUrl(String symbol, String timeframe, int limit,
                                  ZonedDateTime start, ZonedDateTime end) {
        return HttpUrl.parse(String.format(BASE_URL, symbol))
                .newBuilder()
                .addQueryParameter("timeframe", convertTimeframe(timeframe))
                .addQueryParameter("start", start.format(DateTimeFormatter.ISO_INSTANT))
                .addQueryParameter("end", end.format(DateTimeFormatter.ISO_INSTANT))
                .addQueryParameter("limit", String.valueOf(limit))
                .addQueryParameter("adjustment", "raw")
                .addQueryParameter("feed", "iex")
                .addQueryParameter("sort", "asc")
                .build();
    }

    private Request buildRequest(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .addHeader("APCA-API-KEY-ID", apiKey)
                .addHeader("APCA-API-SECRET-KEY", apiSecret)
                .build();
    }

    private void validateResponse(Response response) throws IOException {
        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "";
            throw new IOException(String.format(
                "API request failed with code %d: %s", 
                response.code(), errorBody));
        }
    }

    @Override
    public String convertTimeframe(String timeframe) {
        return switch (timeframe.toLowerCase()) {
            case "1m" -> "1Min";
            case "5m" -> "5Min";
            case "15m" -> "15Min";
            case "30m" -> "30Min";
            case "1h" -> "1H";
            case "4h" -> "4H";
            case "1d" -> "1D";
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        };
    }

    @Override
    public ZonedDateTime calculateStartTime(String timeframe, ZonedDateTime end) {
        return switch (convertTimeframe(timeframe)) {
            case "1Min" -> end.minusDays(7);
            case "5Min" -> end.minusDays(14);
            case "15Min", "30Min" -> end.minusDays(30);
            case "1H", "4H" -> end.minusDays(60);
            case "1D" -> end.minusDays(365);
            default -> end.minusDays(30);
        };
    }
}