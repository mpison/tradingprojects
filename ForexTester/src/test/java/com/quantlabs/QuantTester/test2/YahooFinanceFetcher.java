package com.quantlabs.QuantTester.test2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jfree.data.xy.OHLCDataItem;
import org.json.JSONArray;
import org.json.JSONObject;

import com.quantlabs.QuantTester.test2.YahooFinanceFetcher.RateLimitingInterceptor;

import okhttp3.Cache;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Interceptor.Chain;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class YahooFinanceFetcher {
    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(new RateLimitingInterceptor())
        .cache(new Cache(new File("yahoo_cache"), 10 * 1024 * 1024)) // 10MB cache
        .build();

    public List<OHLCDataItem> fetchData(String symbol, String interval, int days) throws IOException {
        long period2 = System.currentTimeMillis() / 1000;
        long period1 = period2 - (days * 24 * 60 * 60);
        
        HttpUrl url = HttpUrl.parse("https://query1.finance.yahoo.com/v8/finance/chart/" + symbol)
                .newBuilder()
                .addQueryParameter("interval", interval)
                .addQueryParameter("period1", String.valueOf(period1))
                .addQueryParameter("period2", String.valueOf(period2))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response.code() + ": " + 
                    (response.body() != null ? response.body().string() : ""));
            }
            return parseYahooResponse(response.body().string());
        }
    }

    private List<OHLCDataItem> parseYahooResponse(String jsonResponse) throws IOException {
        List<OHLCDataItem> items = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONObject chart = json.getJSONObject("chart");
            JSONArray results = chart.getJSONArray("result");
            JSONObject result = results.getJSONObject(0);
            JSONObject indicators = result.getJSONObject("indicators");
            JSONArray quotes = indicators.getJSONArray("quote");
            JSONObject quote = quotes.getJSONObject(0);
            
            JSONArray timestamps = result.getJSONArray("timestamp");
            JSONArray opens = quote.getJSONArray("open");
            JSONArray highs = quote.getJSONArray("high");
            JSONArray lows = quote.getJSONArray("low");
            JSONArray closes = quote.getJSONArray("close");
            JSONArray volumes = quote.getJSONArray("volume");

            for (int i = 0; i < timestamps.length(); i++) {
                long timestamp = timestamps.getLong(i) * 1000;
                Date date = new Date(timestamp);
                
                double open = opens.isNull(i) ? 0 : opens.getDouble(i);
                double high = highs.isNull(i) ? 0 : highs.getDouble(i);
                double low = lows.isNull(i) ? 0 : lows.getDouble(i);
                double close = closes.isNull(i) ? 0 : closes.getDouble(i);
                double volume = volumes.isNull(i) ? 0 : volumes.getDouble(i);
                
                items.add(new OHLCDataItem(
                    date, open, high, low, close, volume
                ));
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse Yahoo response", e);
        }
        return items;
    }


    static class RateLimitingInterceptor implements Interceptor {
        private long lastRequestTime = 0;
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            synchronized (this) {
                long elapsed = System.currentTimeMillis() - lastRequestTime;
                if (elapsed < 1500) {
                    try {
                        Thread.sleep(1500 - elapsed);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                lastRequestTime = System.currentTimeMillis();
            }
            return chain.proceed(chain.request());
        }
    }
}