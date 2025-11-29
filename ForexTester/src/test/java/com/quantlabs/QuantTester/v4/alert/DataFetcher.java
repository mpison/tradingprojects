package com.quantlabs.QuantTester.v4.alert;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DataFetcher {
    public static class OHLCDataItem {
        public long timestamp;
        public double open;
        public double high;
        public double low;
        public double close;
        public double volume;

        public OHLCDataItem(long timestamp, double open, double high, double low, double close, double volume) {
            this.timestamp = timestamp;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }
    }

    private OkHttpClient client;
    private ApiConfig apiConfig;

    public DataFetcher(ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
        initializeHttpClient();
    }

    private void initializeHttpClient() {
        this.client = new OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(chain -> {
                // Rate limiting for Yahoo
                if (chain.request().url().toString().contains("yahoo")) {
                    try {
						Thread.sleep(1000 / apiConfig.getYahooMaxQps());
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
                return chain.proceed(chain.request());
            })
            .build();
    }

    public void updateApiConfig(ApiConfig newConfig) {
        this.apiConfig = newConfig;
        initializeHttpClient(); // Reinitialize client with new settings
    }

    public ApiConfig getApiConfig() {
        return this.apiConfig;
    }

    public List<OHLCDataItem> fetchOHLC(String symbol, String timeframe, String startDate, String endDate, String source) 
            throws IOException, JSONException {
        if (source.equals("Alpaca")) {
            return fetchAlpacaOHLC(symbol, timeframe, startDate, endDate);
        } else {
            return fetchYahooOHLC(symbol, timeframe, startDate, endDate);
        }
    }

    private List<OHLCDataItem> fetchAlpacaOHLC(String symbol, String timeframe, String startDate, String endDate) 
            throws IOException, JSONException {
        String url = String.format("%s/stocks/%s/bars?timeframe=%s&start=%s&end=%s", 
            apiConfig.getAlpacaBaseUrl(), symbol, timeframe, startDate, endDate);
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("APCA-API-KEY-ID", apiConfig.getAlpacaApiKey())
                .addHeader("APCA-API-SECRET-KEY", apiConfig.getAlpacaSecretKey())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Alpaca API request failed: " + response.code() + " - " + response.message());
            }
            
            JSONObject json = new JSONObject(response.body().string());
            JSONArray bars = json.optJSONArray("bars");
            List<OHLCDataItem> data = new ArrayList<>();
            
            if (bars != null) {
                for (int i = 0; i < bars.length(); i++) {
                    JSONObject bar = bars.getJSONObject(i);
                    data.add(new OHLCDataItem(
                            bar.getLong("t") * 1000,
                            bar.getDouble("o"),
                            bar.getDouble("h"),
                            bar.getDouble("l"),
                            bar.getDouble("c"),
                            bar.getDouble("v")
                    ));
                }
            }
            return data;
        }
    }

    private List<OHLCDataItem> fetchYahooOHLC(String symbol, String timeframe, String startDate, String endDate) 
            throws IOException, JSONException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        long startTs = LocalDate.parse(startDate, formatter).atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC);
        long endTs = LocalDate.parse(endDate, formatter).atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC);
        
        String url = String.format("%s/%s?interval=%s&period1=%d&period2=%d", 
            apiConfig.getYahooBaseUrl(), symbol, timeframe, startTs, endTs);
            
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Yahoo API request failed: " + response.code() + " - " + response.message());
            }
            
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            JSONObject chart = json.optJSONObject("chart");
            
            if (chart == null) {
                throw new IOException("Invalid JSON response: 'chart' object missing");
            }
            
            JSONArray resultArray = chart.optJSONArray("result");
            if (resultArray == null || resultArray.length() == 0) {
                throw new IOException("Invalid JSON response: 'result' array missing or empty");
            }
            
            JSONObject result = resultArray.getJSONObject(0);
            JSONObject indicators = result.optJSONObject("indicators");
            if (indicators == null) {
                throw new IOException("Invalid JSON response: 'indicators' object missing");
            }
            
            JSONArray quoteArray = indicators.optJSONArray("quote");
            if (quoteArray == null || quoteArray.length() == 0) {
                throw new IOException("Invalid JSON response: 'quote' array missing or empty");
            }
            
            JSONObject quote = quoteArray.getJSONObject(0);
            JSONArray timestamp = result.optJSONArray("timestamp");
            JSONArray open = quote.optJSONArray("open");
            JSONArray high = quote.optJSONArray("high");
            JSONArray low = quote.optJSONArray("low");
            JSONArray close = quote.optJSONArray("close");
            JSONArray volume = quote.optJSONArray("volume");

            if (timestamp == null || open == null || high == null || low == null || close == null || volume == null) {
                throw new IOException("Invalid JSON response: One or more data arrays missing");
            }

            List<OHLCDataItem> data = new ArrayList<>();
            for (int i = 0; i < timestamp.length(); i++) {
                if (open.isNull(i) || high.isNull(i) || low.isNull(i) || close.isNull(i) || volume.isNull(i)) {
                    continue;
                }
                try {
                    data.add(new OHLCDataItem(
                            timestamp.getLong(i) * 1000,
                            open.getDouble(i),
                            high.getDouble(i),
                            low.getDouble(i),
                            close.getDouble(i),
                            volume.getDouble(i)
                    ));
                } catch (JSONException e) {
                    System.err.println("Error parsing data at index " + i + ": " + e.getMessage());
                }
            }
            
            if (data.isEmpty()) {
                throw new IOException("No valid OHLC data found in response");
            }
            return data;
        }
    }

	public OkHttpClient getClient() {
		return client;
	}

	public void setClient(OkHttpClient client) {
		this.client = client;
	}

	public void setApiConfig(ApiConfig apiConfig) {
		this.apiConfig = apiConfig;
	}
}