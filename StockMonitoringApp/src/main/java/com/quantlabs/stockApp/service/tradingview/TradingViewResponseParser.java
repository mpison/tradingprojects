package com.quantlabs.stockApp.service.tradingview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.utils.JsonUtils;

public class TradingViewResponseParser {
	public List<PriceData> parse(String json) {
	    List<PriceData> priceDataList = new ArrayList<>();
	    
	    if (json == null || json.trim().isEmpty()) {
	        //logger.warning("Empty or null JSON input received");
	        return priceDataList;
	    }

	    try {
	        JSONObject root = new JSONObject(json);
	        JSONArray data = JsonUtils.getJSONArray(root, "data"); // Now using object version

	        Map<String, Integer> columnIndexMap = createColumnIndexMap();

	        for (int i = 0; i < data.length(); i++) {
	            try {
	                JSONObject item = JsonUtils.getJSONObject(data, i);
	                JSONArray values = JsonUtils.getJSONArray(item, "d");
	                
	                PriceData priceData = parsePriceData(values, columnIndexMap);
	                if (priceData != null) {
	                    priceDataList.add(priceData);
	                }
	            } catch (Exception e) {
	                //logger.log(Level.WARNING, "Error parsing item at index " + i, e);
	            }
	        }
	    } catch (Exception e) {
	        //logger.log(Level.SEVERE, "Error parsing TradingView response", e);
	    }

	    return priceDataList;
	}


	private Map<String, Integer> createColumnIndexMap() {
	    Map<String, Integer> columnIndexMap = new HashMap<>();
	    String[] columns = TradingViewRequestBuilder.PRE_MARKET_COLUMNS;
	    for (int i = 0; i < columns.length; i++) {
	        columnIndexMap.put(columns[i], i);
	    }
	    return columnIndexMap;
	}

	private PriceData parsePriceData(JSONArray values, Map<String, Integer> columnIndexMap) {
	    try {
	        // Extract ticker from name
	    	 String ticker = JsonUtils.getString(values, columnIndexMap.get("name"), "");
	         if (ticker.isEmpty()) {
	             //logger.warning("Skipping item with empty ticker");
	             return null;
	         }

	        // Handle typespecs array
	        List<String> typespecs = new ArrayList<>();
	        JSONArray typespecsArray = JsonUtils.getJSONArray(values, columnIndexMap.get("typespecs"));
	        for (int j = 0; j < typespecsArray.length(); j++) {
	            typespecs.add(typespecsArray.getString(j));
	        }

	        // Handle indexes.tr array
	        List<Map<String, String>> indexes = new ArrayList<>();
	        JSONArray indexesArray = JsonUtils.getJSONArray(values, columnIndexMap.get("indexes.tr"));
	        for (int j = 0; j < indexesArray.length(); j++) {
	            try {
	                JSONObject indexObj = indexesArray.getJSONObject(j);
	                Map<String, String> indexMap = new HashMap<>();
	                indexMap.put("name", JsonUtils.getString(indexObj, "name", ""));
	                indexMap.put("proname", JsonUtils.getString(indexObj, "proname", ""));
	                indexes.add(indexMap);
	            } catch (Exception e) {
	                //logger.log(Level.WARNING, "Error parsing index at position " + j, e);
	            }
	        }

	        // Build PriceData with safe defaults
	        return new PriceData.Builder(ticker, JsonUtils.getDouble(values, columnIndexMap.get("close")))
	            .name(JsonUtils.getString(values, columnIndexMap.get("name"), ""))
	            .description(JsonUtils.getString(values, columnIndexMap.get("description"), ""))
	            .logoid(JsonUtils.getString(values, columnIndexMap.get("logoid"), ""))
	            .updateMode(JsonUtils.getString(values, columnIndexMap.get("update_mode"), ""))
	            .type(JsonUtils.getString(values, columnIndexMap.get("type"), ""))
	            .typespecs(typespecs)
	            .premarketClose(JsonUtils.getDouble(values, columnIndexMap.get("premarket_close")))
	            .pricescale(JsonUtils.getInteger(values, columnIndexMap.get("pricescale")))
	            .minmov(JsonUtils.getInteger(values, columnIndexMap.get("minmov")))
	            .fractional(JsonUtils.getString(values, columnIndexMap.get("fractional"), ""))
	            .minmove2(JsonUtils.getInteger(values, columnIndexMap.get("minmove2")))
	            .currency(JsonUtils.getString(values, columnIndexMap.get("currency"), "USD"))
	            .close(JsonUtils.getDouble(values, columnIndexMap.get("close")))
	            .postmarketClose(JsonUtils.getDouble(values, columnIndexMap.get("postmarket_close")))
	            .premarketHigh(JsonUtils.getDouble(values, columnIndexMap.get("premarket_high")))
	            .high(JsonUtils.getDouble(values, columnIndexMap.get("high")))
	            .postmarketHigh(JsonUtils.getDouble(values, columnIndexMap.get("postmarket_high")))
	            .premarketChange(JsonUtils.getDouble(values, columnIndexMap.get("premarket_change")))
	            .changeFromOpen(JsonUtils.getDouble(values, columnIndexMap.get("change_from_open")))
	            .percentChange(JsonUtils.getDouble(values, columnIndexMap.get("change")))
	            .postmarketChange(JsonUtils.getDouble(values, columnIndexMap.get("postmarket_change")))
	            .premarketLow(JsonUtils.getDouble(values, columnIndexMap.get("premarket_low")))
	            .low(JsonUtils.getDouble(values, columnIndexMap.get("low")))
	            .postmarketLow(JsonUtils.getDouble(values, columnIndexMap.get("postmarket_low")))
	            .premarketVolume(JsonUtils.getLong(values, columnIndexMap.get("premarket_volume")))
	            .currentVolume(JsonUtils.getLong(values, columnIndexMap.get("volume")))
	            .postmarketVolume(JsonUtils.getLong(values, columnIndexMap.get("postmarket_volume")))
	            .averageVol(JsonUtils.getLong(values, columnIndexMap.get("average_volume_10d_calc")))
	            .gap(JsonUtils.getDouble(values, columnIndexMap.get("gap")))
	            .exchange(JsonUtils.getString(values, columnIndexMap.get("exchange"), ""))
	            .indexes(indexes)
	            .build();
	    } catch (Exception e) {
	        //logger.log(Level.WARNING, "Error building PriceData object", e);
	        return null;
	    }
	}
	
	public PriceData parseSingleSymbol(String json) {
        if (json == null || json.trim().isEmpty()) {
            System.out.println("Empty or null JSON");
            return null;
        }

        try {
            // The root IS the data object, not wrapped in "data" key
            JSONObject data = new JSONObject(json);
            
            if (data.length() == 0) {
                System.out.println("Empty data object in response");
                return null;
            }
            
            //System.out.println("Data object keys: " + data.keySet()); // Debug

            // Extract ticker from name field
            String ticker = JsonUtils.getString(data, "name", "");
            if (ticker.isEmpty()) {
                System.out.println("Empty ticker in response");
                return null;
            }

            // Extract close price for required field
            double closePrice = JsonUtils.getDouble(data, "close", 0.0);
            
            return parseSingleSymbolData(data, ticker, closePrice);
            
        } catch (Exception e) {
            System.err.println("Error parsing single symbol response: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private PriceData parseSingleSymbolData(JSONObject data, String ticker, double closePrice) {
        try {
            // Handle typespecs array
            List<String> typespecs = new ArrayList<>();
            JSONArray typespecsArray = JsonUtils.getJSONArray(data, "typespecs");
            if (typespecsArray != null && typespecsArray.length() > 0) {
                for (int j = 0; j < typespecsArray.length(); j++) {
                    try {
                        typespecs.add(typespecsArray.getString(j));
                    } catch (JSONException e) {
                        System.err.println("Error parsing typespec at index " + j + ": " + e.getMessage());
                    }
                }
            }

            // Handle indexes.tr array
            List<Map<String, String>> indexes = new ArrayList<>();
            JSONArray indexesArray = JsonUtils.getJSONArray(data, "indexes.tr");
            if (indexesArray != null && indexesArray.length() > 0) {
                for (int j = 0; j < indexesArray.length(); j++) {
                    try {
                        JSONObject indexObj = indexesArray.getJSONObject(j);
                        Map<String, String> indexMap = new HashMap<>();
                        indexMap.put("name", JsonUtils.getString(indexObj, "name", ""));
                        indexMap.put("proname", JsonUtils.getString(indexObj, "proname", ""));
                        indexes.add(indexMap);
                    } catch (Exception e) {
                        System.err.println("Error parsing index at position " + j + ": " + e.getMessage());
                    }
                }
            }

            // Build PriceData
            PriceData.Builder builder = new PriceData.Builder(ticker, closePrice)
                .name(JsonUtils.getString(data, "name", ""))
                .description(JsonUtils.getString(data, "description", ""))
                .logoid(JsonUtils.getString(data, "logoid", ""))
                .updateMode(JsonUtils.getString(data, "update_mode", ""))
                .type(JsonUtils.getString(data, "type", ""))
                .typespecs(typespecs)
                .premarketClose(getDoubleFromObject(data, "premarket_close"))
                .pricescale(getIntegerFromObject(data, "pricescale"))
                .minmov(getIntegerFromObject(data, "minmov"))
                .fractional(JsonUtils.getString(data, "fractional", ""))
                .minmove2(getIntegerFromObject(data, "minmove2"))
                .currency(JsonUtils.getString(data, "currency", "USD"))
                .close(closePrice)
                .postmarketClose(getDoubleFromObject(data, "postmarket_close"))
                .premarketHigh(getDoubleFromObject(data, "premarket_high"))
                .high(getDoubleFromObject(data, "high"))
                .postmarketHigh(getDoubleFromObject(data, "postmarket_high"))
                .premarketChange(getDoubleFromObject(data, "premarket_change"))
                .changeFromOpen(getDoubleFromObject(data, "change_from_open"))
                .percentChange(getDoubleFromObject(data, "change"))
                .postmarketChange(getDoubleFromObject(data, "postmarket_change"))
                .premarketLow(getDoubleFromObject(data, "premarket_low"))
                .low(getDoubleFromObject(data, "low"))
                .postmarketLow(getDoubleFromObject(data, "postmarket_low"))
                .premarketVolume(getLongFromObject(data, "premarket_volume"))
                .currentVolume(getLongFromObject(data, "volume"))
                .postmarketVolume(getLongFromObject(data, "postmarket_volume"))
                .averageVol(getLongFromObject(data, "average_volume_10d_calc"))
                .gap(getDoubleFromObject(data, "gap"))
                .exchange(JsonUtils.getString(data, "exchange", ""))
                .indexes(indexes);

            PriceData result = builder.build();
            //System.out.println("Successfully parsed PriceData for: " + result.getTicker());
            //System.out.println("Price: $" + result.getLatestPrice());
            //System.out.println("Premarket Change: " + result.getPremarketChange() + "%");
            //System.out.println("Volume: " + result.getCurrentVolume());
            
            return result;
                
        } catch (Exception e) {
            System.err.println("Error building PriceData from single symbol: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Helper methods using direct JSONObject access
    private Double getDoubleFromObject(JSONObject obj, String key) {
        try {
            return obj.has(key) && !obj.isNull(key) ? obj.getDouble(key) : 0;
        } catch (JSONException e) {
            return (double) 0;
        }
    }

    private Integer getIntegerFromObject(JSONObject obj, String key) {
        try {
            return obj.has(key) && !obj.isNull(key) ? obj.getInt(key) : 0;
        } catch (JSONException e) {
            return 0;
        }
    }

    private Long getLongFromObject(JSONObject obj, String key) {
        try {
            return obj.has(key) && !obj.isNull(key) ? obj.getLong(key) : 0;
        } catch (JSONException e) {
            return (long) 0;
        }
    }

}