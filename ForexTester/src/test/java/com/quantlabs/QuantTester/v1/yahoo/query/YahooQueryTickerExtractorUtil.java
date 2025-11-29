package com.quantlabs.QuantTester.v1.yahoo.query;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class YahooQueryTickerExtractorUtil {

    public static List<String> extractTickers(String json) throws JSONException {
        List<String> tickers = new ArrayList<>();

        JSONObject root = new JSONObject(json);
        JSONArray results = root.getJSONObject("finance")
                                .getJSONArray("result");

        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.getJSONObject(i);
            JSONArray records = result.getJSONArray("records");

            for (int j = 0; j < records.length(); j++) {
                JSONObject record = records.getJSONObject(j);
                if (record.has("ticker")) {
                    tickers.add(record.getString("ticker"));
                }
            }
        }

        return tickers;
    }

    public static void main(String[] args) throws JSONException {
        String jsonInput = "PASTE_YOUR_JSON_STRING_HERE"; // Load your JSON string here
        List<String> tickers = extractTickers(jsonInput);
        System.out.println("Tickers found:");
        tickers.forEach(System.out::println);
    }
}
