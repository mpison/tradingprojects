package com.quantlabs.stockApp.data;

import java.io.IOException;
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
import com.quantlabs.stockApp.utils.pricedata.PriceDataCalculator;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AlpacaDataProvider implements StockDataProvider {
    private String adjustment = "all";//"raw";
    private String feed = "sip";
    
    private final OkHttpClient client;
    private final String apiKey;
    private final String apiSecret;
    private final ConsoleLogger logger;

    public AlpacaDataProvider(OkHttpClient client, String apiKey, String apiSecret, ConsoleLogger logger) {
        this.client = client;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.logger = logger;
    }
    
    // Add configuration methods
    public void setAdjustment(String adjustment) {
        this.adjustment = adjustment;
    }
    
    public String getAdjustment() {
        return this.adjustment;
    }

    public void setFeed(String feed) {
        this.feed = feed;
    }

    private String getFeed() {
    	return this.feed;//(timeframe.equals("1Min") || timeframe.equals("5Min")) ? "sip" : "iex";
    }
    
    @Override
    public ZonedDateTime getCurrentTime() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }

    @Override
    public BarSeries getHistoricalData(String symbol, String timeframe, int limit, 
                                     ZonedDateTime start, ZonedDateTime end) throws IOException {
        String feed = getFeed();//getFeedForTimeframe(timeframe);
        String adjustment = getAdjustment();
        
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://data.alpaca.markets/v2/stocks/" + symbol + "/bars")
                .newBuilder()
                .addQueryParameter("timeframe", timeframe)
                .addQueryParameter("start", start.format(DateTimeFormatter.ISO_INSTANT))
                .addQueryParameter("end", end.format(DateTimeFormatter.ISO_INSTANT))
                .addQueryParameter("limit", String.valueOf(limit))
                .addQueryParameter("adjustment", adjustment)
                .addQueryParameter("feed", feed)
                .addQueryParameter("sort", "asc");

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .addHeader("accept", "application/json")
                .addHeader("APCA-API-KEY-ID", apiKey)
                .addHeader("APCA-API-SECRET-KEY", apiSecret)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }
            String responseBody = response.body().string();
            return parseAlpacaJsonToBarSeries(new StockDataResult(responseBody, start, end), symbol, timeframe);
        } catch (JSONException e) {
            throw new IOException("Error parsing JSON response", e);
        }
    }
    
    @Override
	public PriceData getPriceData(String symbol, ZonedDateTime start, ZonedDateTime end) throws IOException {
		// TODO Auto-generated method stub
    	
    	
    	// Fetch 1Min historical data
		BarSeries series = getHistoricalData(symbol, "1Min", 5000, start, end);
		
		if (series.getBarCount() == 0) {
		    throw new IOException("No historical data available for symbol: " + symbol);
		}
		
		// Calculate all PriceData attributes from the series
		PriceData priceData = PriceDataCalculator.calculatePriceDataFromSeries(series, symbol, "ranged");
    	
		//priceData.latestPrice = currentPrice;
        //priceData.previousClose = prevLastDayPrice;
        //priceData.percentChange = percentChange;
        //priceData.currentVolume = percentChange;
        //priceData.previousVolume = prevDailyVol;
        //priceData.prevLastDayPrice = prevLastDayPrice;

        // aveVolume will be set in refreshData
        return priceData;
	}

    @Override
    public PriceData getLatestPrice(String symbol) throws IOException {
        Request request = new Request.Builder()
                .url("https://data.alpaca.markets/v2/stocks/snapshots?symbols=" + symbol)
                .get()
                .addHeader("accept", "application/json")
                .addHeader("APCA-API-KEY-ID", apiKey)
                .addHeader("APCA-API-SECRET-KEY", apiSecret)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }
                        
            PriceData priceData =  computePriceDataStats(symbol);            
            System.out.print(false);
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            JSONObject snapshot = json.getJSONObject(symbol);
            JSONObject latestTrade = snapshot.getJSONObject("latestTrade");
            JSONObject latestQuote = snapshot.getJSONObject("latestQuote");
            JSONObject dailyBar = snapshot.getJSONObject("dailyBar");
            JSONObject prevDailyBar = snapshot.getJSONObject("prevDailyBar");

            double currentPrice = latestQuote.getDouble("ap");//latestTrade.getDouble("p");
            double previousClose = prevDailyBar.getDouble("c");
            double percentChange = ((currentPrice - previousClose) / previousClose) * 100;
            long currentDailyVol = dailyBar.getLong("v");
            long prevDailyVol = prevDailyBar.getLong("v");
            double prevLastDayPrice = prevDailyBar.getDouble("c");
            
            priceData.latestPrice = currentPrice;
            //priceData.previousClose = prevLastDayPrice;
            //priceData.percentChange = percentChange;
            //priceData.currentVolume = percentChange;
            priceData.previousVolume = prevDailyVol;
            priceData.prevLastDayPrice = prevLastDayPrice;

            // aveVolume will be set in refreshData
            return priceData;//new PriceData(currentPrice, prevDailyVol, currentDailyVol, percentChange, 0L, prevLastDayPrice);
        } catch (JSONException e) {
            throw new IOException("Error parsing JSON response", e);
        }
    }

	private PriceData computePriceDataStats(String symbol) throws IOException {
		ZonedDateTime end = ZonedDateTime.now();
		ZonedDateTime start = end.minusDays(7);
		
		// Fetch 1Min historical data
		BarSeries series = getHistoricalData(symbol, "1Min", 5000, start, end);
		
		if (series.getBarCount() == 0) {
		    throw new IOException("No historical data available for symbol: " + symbol);
		}
		
		// Calculate all PriceData attributes from the series
		return PriceDataCalculator.calculatePriceDataFromSeries(series, symbol, "latest");
	}

    private BarSeries parseAlpacaJsonToBarSeries(StockDataResult stockData, String symbol, String timeframe) {
        try {
            JSONObject json = new JSONObject(stockData.getJsonResponse());

            // Check if 'bars' field exists and is not null
            if (!json.has("bars") || json.isNull("bars")) {
                logger.log("No 'bars' data in response for symbol " + symbol + " with timeframe " + timeframe);
                
                System.out.println("No 'bars' data in response for symbol " + symbol + " with timeframe " + timeframe);
                
                return new BaseBarSeriesBuilder().withName(symbol).build(); // Return empty series
            }

            JSONArray bars;
            if (json.get("bars") instanceof JSONObject) {
                JSONObject barsObject = json.getJSONObject("bars");
                if (!barsObject.has(symbol) || barsObject.isNull(symbol)) {
                    logger.log("No data for symbol " + symbol + " in bars object with timeframe " + timeframe);
                    
                    System.out.println("No data for symbol " + symbol + " in bars object with timeframe " + timeframe);
                    
                    return new BaseBarSeriesBuilder().withName(symbol).build(); // Return empty series
                }
                bars = barsObject.getJSONArray(symbol);
            } else {
                bars = json.getJSONArray("bars");
            }

            BarSeries series = new BaseBarSeriesBuilder().withName(symbol).build();

            for (int i = 0; i < bars.length(); i++) {
                JSONObject bar = bars.getJSONObject(i);
                
                // Extract bar data with validation
                double open = bar.getDouble("o");
                double high = bar.getDouble("h");
                double low = bar.getDouble("l");
                double close = bar.getDouble("c");
                long volume = bar.getLong("v");
                ZonedDateTime time = ZonedDateTime.parse(
                    bar.getString("t"), 
                    DateTimeFormatter.ISO_ZONED_DATE_TIME
                );

                // Validate data before adding to series
                if (open <= 0 || high <= 0 || low <= 0 || close <= 0 || volume < 0 ||
                    Double.isNaN(open) || Double.isNaN(high) || Double.isNaN(low) || Double.isNaN(close)) {
                    logger.log("Skipping invalid bar for " + symbol + " at " + time + ": open=" + open + 
                                ", high=" + high + ", low=" + low + ", close=" + close + ", volume=" + volume);
                    System.out.println("Alpacca Skipping invalid bar for " + symbol + " at " + time + ": open=" + open + 
                            ", high=" + high + ", low=" + low + ", close=" + close + ", volume=" + volume);
                    continue; // Skip invalid bars
                }

                series.addBar(time, open, high, low, close, volume);
            }

            // Handle pagination if available
            while (json.has("next_page_token") && !json.isNull("next_page_token")) {
                String nextPageToken = json.getString("next_page_token");
                try {
                    String nextPageData = fetchNextPage(symbol, nextPageToken, timeframe, 
                                                      stockData.getStart(), stockData.getEnd());
                    json = new JSONObject(nextPageData);

                    if (!json.has("bars") || json.isNull("bars")) {
                        logger.log("No 'bars' data in next page response for symbol " + symbol);
                        System.out.println("Alpacca Error No 'bars' data in next page response for symbol " + symbol);
                        return series; // Return what we have so far
                    }

                    if (json.get("bars") instanceof JSONObject) {
                        JSONObject nextPageBars = json.getJSONObject("bars");
                        if (!nextPageBars.has(symbol) || nextPageBars.isNull(symbol)) {
                            logger.log("No data for symbol " + symbol + " in next page bars object");
                            System.out.println("Alpacca Error No data for symbol " + symbol + " in next page bars object");
                            return series; // Return what we have so far
                        }
                        bars = nextPageBars.getJSONArray(symbol);
                    } else {
                        bars = json.getJSONArray("bars");
                    }

                    for (int i = 0; i < bars.length(); i++) {
                        JSONObject bar = bars.getJSONObject(i);
                        ZonedDateTime time = ZonedDateTime.parse(
                            bar.getString("t"), 
                            DateTimeFormatter.ISO_ZONED_DATE_TIME
                        );
                        series.addBar(time, bar.getDouble("o"), bar.getDouble("h"), 
                                     bar.getDouble("l"), bar.getDouble("c"), bar.getLong("v"));
                    }
                } catch (Exception e) {
                    logger.log("Alpacca Error fetching next page for " + symbol + ": " + e.getMessage());
                    System.out.println("Alpacca Error fetching next page for " + symbol + ": " + e.getMessage());
                    break;
                }
            }

            if (series.getBarCount() == 0) {
                logger.log("No valid bars parsed for " + symbol + " with timeframe " + timeframe);
                System.out.println("No valid bars parsed for " + symbol + " with timeframe " + timeframe);
                return series; // Return empty series
            }
            
            series = WickCleanerUtil.cleanBarSeries(series);

            return series;
        } catch (JSONException e) {
            logger.log("Error parsing JSON for symbol " + symbol + ": " + e.getMessage());
            throw new RuntimeException("Error parsing JSON for symbol " + symbol + ": " + e.getMessage(), e);
        }
    }

    private String fetchNextPage(String symbol, String nextPageToken, String timeframe,
                               ZonedDateTime start, ZonedDateTime end) throws IOException {
        String feed = getFeed();
        String adjustment = getAdjustment();
        HttpUrl url = HttpUrl.parse("https://data.alpaca.markets/v2/stocks/" + symbol + "/bars")
                .newBuilder()
                .addQueryParameter("start", start.format(DateTimeFormatter.ISO_INSTANT))
                .addQueryParameter("end", end.format(DateTimeFormatter.ISO_INSTANT))
                .addQueryParameter("timeframe", timeframe)
                .addQueryParameter("limit", "1000")
                .addQueryParameter("adjustment", adjustment)
                .addQueryParameter("feed", feed)
                .addQueryParameter("sort", "asc")
                .addQueryParameter("page_token", nextPageToken)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .addHeader("APCA-API-KEY-ID", apiKey)
                .addHeader("APCA-API-SECRET-KEY", apiSecret)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }
            return response.body().string();
        }
    }
    
    @Override
    public StockDataResult fetchStockData(String symbol, String timeframe, int limit,
                                        ZonedDateTime start, ZonedDateTime end) throws IOException {
        if (start == null) {
            start = calculateDefaultStartTime(timeframe, end);
        }
        
        String feed = getFeed();
        String adjustment = getAdjustment();
        
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://data.alpaca.markets/v2/stocks/" + symbol + "/bars")
                .newBuilder()
                .addQueryParameter("timeframe", timeframe)
                .addQueryParameter("start", start.format(DateTimeFormatter.ISO_INSTANT))
                .addQueryParameter("end", end.format(DateTimeFormatter.ISO_INSTANT))
                .addQueryParameter("limit", String.valueOf(limit))
                .addQueryParameter("adjustment", adjustment)
                .addQueryParameter("feed", feed)
                .addQueryParameter("sort", "asc");

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .addHeader("accept", "application/json")
                .addHeader("APCA-API-KEY-ID", apiKey)
                .addHeader("APCA-API-SECRET-KEY", apiSecret)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + ": " + response.body().string());
            }
            return new StockDataResult(response.body().string(), start, end);
        }
    }

	
}