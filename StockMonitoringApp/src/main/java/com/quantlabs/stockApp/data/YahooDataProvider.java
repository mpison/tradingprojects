package com.quantlabs.stockApp.data;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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

public class YahooDataProvider implements StockDataProvider {
    private final OkHttpClient client;
    private final ConsoleLogger logger;
    
    private static final ZoneId EDT_ZONE = ZoneId.of("America/New_York");
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    public YahooDataProvider(OkHttpClient client, ConsoleLogger logger) {
        this.client = client;
        this.logger = logger;
    }    

    @Override
    public BarSeries getHistoricalData(String symbol, String timeframe, int limit, 
                                     ZonedDateTime start, ZonedDateTime end) throws IOException {
        String interval = convertTimeframeToYahooInterval(timeframe);
        long period1 = start.toEpochSecond();
        long period2 = end.toEpochSecond();
        
        symbol = symbol.replaceFirst("^\\^", "%5E");

        HttpUrl url = HttpUrl.parse("https://query1.finance.yahoo.com/v8/finance/chart/" + symbol).newBuilder()
                .addQueryParameter("interval", interval)
                .addQueryParameter("period1", String.valueOf(period1))
                .addQueryParameter("period2", String.valueOf(period2))
                .addQueryParameter("includePrePost", "true")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }
            String responseBody = response.body().string();
            return parseYahooJsonToBarSeries(new StockDataResult(responseBody, start, end), symbol, timeframe);
        } catch (JSONException e) {
            throw new IOException("Error parsing JSON response", e);
        }
    }

    @Override
    public PriceData getLatestPrice(String symbol) throws IOException {
        HttpUrl url = HttpUrl.parse("https://query1.finance.yahoo.com/v8/finance/chart/" + symbol).newBuilder()
                .addQueryParameter("interval", "1m")
                .addQueryParameter("range", "2d")
                .addQueryParameter("includePrePost", "true")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            JSONArray results = json.getJSONObject("chart").getJSONArray("result");

            if (results.length() == 0) {
                throw new IOException("No data found in response for " + symbol);
            }

            JSONObject result = results.getJSONObject(0);
            JSONObject meta = result.getJSONObject("meta");
            JSONObject indicators = result.getJSONObject("indicators");
            JSONArray quotes = indicators.getJSONArray("quote");
            JSONObject quote = quotes.getJSONObject(0);

            JSONArray timestamps = result.getJSONArray("timestamp");
            JSONArray closes = quote.getJSONArray("close");
            JSONArray volumes = quote.getJSONArray("volume");

            LatestPriceResult priceResult = getLatestPriceAndVolumes(timestamps, closes, volumes);
            double prevLastDayPrice = meta.getDouble("chartPreviousClose");

            return new PriceData(
                priceResult.currentPrice,
                priceResult.prevDayVolume,
                priceResult.currentDayVolume,
                calculatePercentChange(priceResult.currentPrice, prevLastDayPrice),
                0L, // aveVolume set by refreshData
                prevLastDayPrice
            );
        } catch (JSONException e) {
            throw new IOException("Error parsing JSON response for " + symbol, e);
        }
    }
    
    private void printTimeSeries(JSONArray timestamps, JSONArray closes, JSONArray volumes) throws JSONException {
        System.out.println("\nTime Series Data (UTC -> EDT):");
        System.out.println("Timestamp (UTC)\t\tTimestamp (EDT)\t\tClose\tVolume");
        
        for (int i = 0; i < timestamps.length(); i++) {
            long timestamp = timestamps.getLong(i);
            ZonedDateTime utcTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC);
            ZonedDateTime edtTime = utcTime.withZoneSameInstant(EDT_ZONE);
            
            String closeStr = closes.isNull(i) ? "null" : String.format("%.2f", closes.getDouble(i));
            String volumeStr = volumes.isNull(i) ? "null" : String.format("%,d", volumes.getLong(i));
            
            System.out.printf("%s\t%s\t%s\t%s\n",
                utcTime.format(TIME_FORMATTER),
                edtTime.format(TIME_FORMATTER),
                closeStr,
                volumeStr);
        }
    }
    
    private LatestPriceResult getLatestPriceAndVolumes(JSONArray timestamps, JSONArray closes, JSONArray volumes) 
            throws JSONException {
        double currentPrice = 0;
        long currentDayVolume = 0;
        long prevDayVolume = 0;
        
        ZonedDateTime nowutc = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime nowedtTime = nowutc.withZoneSameInstant(EDT_ZONE);
        
        int currentDay = nowedtTime.getDayOfMonth();
        
        
        int previousDay = 0;
        
        for (int i = 0; i < timestamps.length(); i++) {
            if (closes.isNull(i) || volumes.isNull(i)) continue;
            
            long timestamp = timestamps.getLong(i);
            
            ZonedDateTime utcTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC);
            ZonedDateTime edtTime = utcTime.withZoneSameInstant(EDT_ZONE);
            
            // Track latest price
            if (i == timestamps.length() - 1 || closes.isNull(i + 1)) {
                currentPrice = closes.getDouble(i);
            }
            
            int parseDay = edtTime.getDayOfMonth();
            
            if(previousDay == 0) {
            	//currentDay = parseDay + 1;
            	previousDay = parseDay;
            }
            
            // Categorize volumes
            if (parseDay == currentDay) {
                currentDayVolume += volumes.getLong(i);
            } else {
                prevDayVolume += volumes.getLong(i);
            }
            
            //System.out.println("parseDay="+parseDay + " currentDay="+currentDay);
        }
        
        
        
        //System.out.printf("\nLatest Price: %.2f (Current Day Volume: %,d | Prev Day Volume: %,d)\n", currentPrice, currentDayVolume, prevDayVolume);
            
        return new LatestPriceResult(currentPrice, currentDayVolume, prevDayVolume);
    }

    private double calculatePercentChange(double currentPrice, double previousClose) {
        return ((currentPrice - previousClose) / previousClose) * 100;
    }

    // Helper class for returning multiple values
    private static class LatestPriceResult {
        final double currentPrice;
        final long currentDayVolume;
        final long prevDayVolume;
        
        LatestPriceResult(double currentPrice, long currentDayVolume, long prevDayVolume) {
            this.currentPrice = currentPrice;
            this.currentDayVolume = currentDayVolume;
            this.prevDayVolume = prevDayVolume;
        }
    }
    
    private long[] calculateCurrentAndPreviousDayVolumes(JSONArray timestamps, JSONArray volumes, ZoneId timezone) 
    	    throws JSONException {
    	    long currentDayVol = 0;
    	    long previousDayVol = 0;
    	    
    	    // Get current time in specified timezone
    	    ZonedDateTime now = ZonedDateTime.now(timezone);
    	    ZonedDateTime currentDayStart = now.withHour(0).withMinute(0).withSecond(0);
    	    ZonedDateTime previousDayStart = currentDayStart.minusDays(1);
    	    
    	    // Define market hours in EDT
    	    int preMarketStart = 4;  // 4:00 AM EDT
    	    int marketOpen = 9;      // 9:30 AM EDT (we'll use 9:00 as cutoff)
    	    int marketClose = 16;    // 4:00 PM EDT
    	    
    	    for (int i = 0; i < timestamps.length(); i++) {
    	        if (volumes.isNull(i)) continue;
    	        
    	        long timestamp = timestamps.getLong(i);
    	        ZonedDateTime time = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), timezone);
    	        int hour = time.getHour();
    	        
    	        // Only count standard market hours (9:30 AM - 4:00 PM EDT)
    	        boolean isMarketHours = hour >= marketOpen && hour < marketClose;
    	        
    	        if (time.isAfter(currentDayStart)) {
    	            if (isMarketHours) {
    	                currentDayVol += volumes.getLong(i);
    	            }
    	        } else if (time.isAfter(previousDayStart)) {
    	            if (isMarketHours) {
    	                previousDayVol += volumes.getLong(i);
    	            }
    	        }
    	    }
    	    
    	    return new long[]{currentDayVol, previousDayVol};
    	}

    private String convertTimeframeToYahooInterval(String timeframe) {
        switch (timeframe) {
            case "1Min": return "1m";
            case "5Min": return "5m";
            case "15Min": return "15m";
            case "30Min": return "30m";
            case "1H": return "1h";
            case "4H": return "4h";
            case "1D": return "1d";
            case "1W": return "1wk";
            default: return "1d";
        }
    }

    private BarSeries parseYahooJsonToBarSeries(StockDataResult stockData, String symbol, String timeframe) {
	    try {
	        JSONObject json = new JSONObject(stockData.jsonResponse);
	        JSONObject chart = json.getJSONObject("chart");
	        JSONArray results = chart.getJSONArray("result");
	        JSONObject firstResult = results.getJSONObject(0);
	        
	        // Check market state if available
	        String marketState = firstResult.optString("marketState", "REGULAR");
	        
	        JSONArray timestamps = firstResult.getJSONArray("timestamp");
	        JSONObject indicators = firstResult.getJSONObject("indicators");
	        JSONArray quotes = indicators.getJSONArray("quote");
	        JSONObject quote = quotes.getJSONObject(0);

	        JSONArray opens = quote.getJSONArray("open");
	        JSONArray highs = quote.getJSONArray("high");
	        JSONArray lows = quote.getJSONArray("low");
	        JSONArray closes = quote.getJSONArray("close");
	        JSONArray volumes = quote.getJSONArray("volume");

	        BarSeries series = new BaseBarSeriesBuilder().withName(symbol).build();

	        for (int i = 0; i < timestamps.length(); i++) {
	            if (opens.isNull(i) || highs.isNull(i) || lows.isNull(i) || closes.isNull(i) || volumes.isNull(i)) {
	                //logToConsole("Skipping invalid bar for " + symbol + " at index " + i + ": missing data");
	                continue;
	            }

	            double open = opens.getDouble(i);
	            double high = highs.getDouble(i);
	            double low = lows.getDouble(i);
	            double close = closes.getDouble(i);
	            long volume = volumes.getLong(i);

	            if (open <= 0 || high <= 0 || low <= 0 || close <= 0 || volume < 0 ||
	                Double.isNaN(open) || Double.isNaN(high) || Double.isNaN(low) || Double.isNaN(close)) {
	                //logToConsole("Skipping invalid bar for " + symbol + " at index " + i + ": invalid values");
	                continue;
	            }

	            long timestamp = timestamps.getLong(i);
	            ZonedDateTime time = ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC);
	            
	            // You could add market phase information to the bar if needed
	            series.addBar(time, open, high, low, close, volume);
	        }

	        if (series.getBarCount() == 0) {
	            //logToConsole("No valid bars parsed for " + symbol);
	        }
	        
	        series = WickCleanerUtil.cleanBarSeries(series);
	        
	        return series;
	    } catch (JSONException e) {
	        //logToConsole("Error parsing Yahoo JSON for symbol " + symbol + ": " + e.getMessage());
	        return new BaseBarSeriesBuilder().withName(symbol).build();
	    }
	}
    
    @Override
    public StockDataResult fetchStockData(String symbol, String timeframe, int limit,
                                        ZonedDateTime start, ZonedDateTime end) throws IOException {
    	
    	if (start == null) {
            start = calculateDefaultStartTime(timeframe, end);
        }    	
    	
        String interval = convertTimeframeToYahooInterval(timeframe);
        long period1 = start.toEpochSecond();
        long period2 = end.toEpochSecond();

        HttpUrl url = HttpUrl.parse("https://query1.finance.yahoo.com/v8/finance/chart/" + symbol).newBuilder()
                .addQueryParameter("interval", interval)
                .addQueryParameter("period1", String.valueOf(period1))
                .addQueryParameter("period2", String.valueOf(period2))
                .addQueryParameter("includePrePost", "true")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }
            return new StockDataResult(response.body().string(), start, end);
        }
    }

	@Override
	public PriceData getPriceData(String symbol, ZonedDateTime start, ZonedDateTime end) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
    
}