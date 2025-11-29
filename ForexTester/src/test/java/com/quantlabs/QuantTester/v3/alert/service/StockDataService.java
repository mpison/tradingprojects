package com.quantlabs.QuantTester.v3.alert.service;

import com.quantlabs.QuantTester.v3.alert.OHLCDataItem;
import com.quantlabs.QuantTester.v3.alert.StockDataResult;
import com.quantlabs.QuantTester.v3.alert.data.AlpacaStockDataProvider;
import com.quantlabs.QuantTester.v3.alert.data.StockDataProvider;
import com.quantlabs.QuantTester.v3.alert.data.YahooStockDataProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for fetching and processing stock market data from various providers
 */
public class StockDataService {
    private static final Logger logger = Logger.getLogger(StockDataService.class.getName());
    private final StockDataProvider dataProvider;
    
    /**
     * Creates a new StockDataService with the specified data provider
     * @param dataProvider The data provider implementation (Yahoo, Alpaca, etc.)
     */
    public StockDataService(StockDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    /**
     * Fetches OHLC data for a specific symbol and timeframe
     * @param symbol The stock symbol (e.g., "AAPL")
     * @param timeframe The timeframe (e.g., "1h", "4h", "1d")
     * @param limit The maximum number of data points to fetch
     * @return List of OHLCDataItems or null if fetch fails
     */
    public List<OHLCDataItem> fetchData(String symbol, String timeframe, int limit) {
        try {
            StockDataResult result = dataProvider.fetchStockData(symbol, timeframe, limit);
            if (result == null || !result.isValid()) {
                logger.warning("Invalid data result for " + symbol + " (" + timeframe + ")");
                return null;
            }
            return parseResponse(result.getResponse(), symbol, timeframe);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching data for " + symbol + " (" + timeframe + ")", e);
            return null;
        }
    }

    /**
     * Parses the API response into OHLC data items
     * @param response The raw API response string
     * @param symbol The stock symbol (for logging)
     * @param timeframe The timeframe (for logging)
     * @return List of parsed OHLCDataItems
     * @throws JSONException If response parsing fails
     */
    protected List<OHLCDataItem> parseResponse(String response, String symbol, String timeframe) throws JSONException {
        List<OHLCDataItem> dataItems = new ArrayList<>();
        JSONObject json = new JSONObject(response);
        
        if (dataProvider instanceof YahooStockDataProvider) {
            dataItems = parseYahooResponse(json, symbol, timeframe);
        } else if (dataProvider instanceof AlpacaStockDataProvider) {
            dataItems = parseAlpacaResponse(json, symbol, timeframe);
        }
        
        if (dataItems.isEmpty()) {
            logger.warning("No valid data points found for " + symbol + " (" + timeframe + ")");
            return null;
        }
        
        logger.info("Successfully parsed " + dataItems.size() + " data points for " + symbol);
        return dataItems;
    }

    private List<OHLCDataItem> parseYahooResponse(JSONObject json, String symbol, String timeframe) throws JSONException {
        List<OHLCDataItem> items = new ArrayList<>();
        JSONObject chart = json.getJSONObject("chart");
        
        if (chart.has("error") && !chart.isNull("error")) {
            String errorDesc = chart.getJSONObject("error").optString("description", "Unknown error");
            throw new JSONException("Yahoo API error: " + errorDesc);
        }

        JSONArray results = chart.getJSONArray("result");
        JSONObject firstResult = results.getJSONObject(0);
        JSONArray timestamps = firstResult.getJSONArray("timestamp");
        JSONObject indicators = firstResult.getJSONObject("indicators");
        JSONArray quotes = indicators.getJSONArray("quote");
        JSONObject quote = quotes.getJSONObject(0);

        JSONArray opens = quote.getJSONArray("open");
        JSONArray highs = quote.getJSONArray("high");
        JSONArray lows = quote.getJSONArray("low");
        JSONArray closes = quote.getJSONArray("close");
        JSONArray volumes = quote.getJSONArray("volume");

        for (int i = 0; i < timestamps.length(); i++) {
            try {
                if (opens.isNull(i) || highs.isNull(i) || lows.isNull(i) || closes.isNull(i) || volumes.isNull(i)) {
                    continue;
                }
                
                items.add(new OHLCDataItem(
                    timestamps.getLong(i) * 1000,
                    opens.getDouble(i),
                    highs.getDouble(i),
                    lows.getDouble(i),
                    closes.getDouble(i),
                    volumes.getLong(i)
                ));
            } catch (JSONException e) {
                logger.log(Level.WARNING, "Error parsing data point " + i + " for " + symbol, e);
            }
        }
        
        return items;
    }

    private List<OHLCDataItem> parseAlpacaResponse(JSONObject json, String symbol, String timeframe) throws JSONException {
        List<OHLCDataItem> items = new ArrayList<>();
        JSONArray bars = json.getJSONArray("bars");
        
        for (int i = 0; i < bars.length(); i++) {
            try {
                JSONObject bar = bars.getJSONObject(i);
                ZonedDateTime timestamp = ZonedDateTime.parse(bar.getString("t"));
                
                items.add(new OHLCDataItem(
                    timestamp.toInstant().toEpochMilli(),
                    bar.getDouble("o"),
                    bar.getDouble("h"),
                    bar.getDouble("l"),
                    bar.getDouble("c"),
                    bar.getLong("v")
                ));
            } catch (JSONException e) {
                logger.log(Level.WARNING, "Error parsing Alpaca data point " + i + " for " + symbol, e);
            }
        }
        
        return items;
    }

    /**
     * Gets the current data provider being used
     * @return The StockDataProvider implementation
     */
    public StockDataProvider getDataProvider() {
        return dataProvider;
    }

    /**
     * Checks if the service is currently connected to its data source
     * @return true if the last API call was successful, false otherwise
     */
    public boolean isConnected() {
        // Could implement actual connection checking
        return true;
    }
}