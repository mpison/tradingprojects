package com.quantlabs.QuantTester.v3.alert.config;

import org.json.JSONObject;
import org.json.JSONException;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

public class AppConfig {
    private JSONObject config;
    private static final Set<String> VALID_TIMEFRAMES = new HashSet<>(Arrays.asList(
        "1h", "4h", "1d", "1m", "5m", "15m", "30m"
    ));

    public AppConfig(String configContent) throws JSONException {
        this.config = new JSONObject(configContent);
        validateConfig();
    }

    private void validateConfig() throws JSONException {
        if (!config.has("mappings") || !config.has("combinations") || !config.has("watchlists")) {
            throw new JSONException("Missing required fields: mappings, combinations, or watchlists");
        }
        
        // Additional validation logic...
    }

    public JSONObject getConfig() {
        return config;
    }

    public static String getDefaultConfig() {
        return "{\n" +
               "  \"mappings\": [\n" +
               "    {\n" +
               "      \"name\": \"watchlistMap1\",\n" +
               "      \"watchlist\": \"Tech Stocks\",\n" +
               "      \"combination\": \"Combination1\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"combinations\": [\n" +
               "    {\n" +
               "      \"name\": \"Combination1\",\n" +
               "      \"indicators\": [\n" +
               "        {\n" +
               "          \"timeframe\": \"4h\",\n" +
               "          \"shift\": 0,\n" +
               "          \"type\": \"MACD\",\n" +
               "          \"params\": {\n" +
               "            \"signalTimeFrame\": 9,\n" +
               "            \"shortTimeFrame\": 12,\n" +
               "            \"longTimeFrame\": 26\n" +
               "          }\n" +
               "        }\n" +
               "      ]\n" +
               "    }\n" +
               "  ],\n" +
               "  \"watchlists\": [\n" +
               "    {\n" +
               "      \"name\": \"Tech Stocks\",\n" +
               "      \"stocks\": [\n" +
               "        {\n" +
               "          \"symbol\": \"AAPL\",\n" +
               "          \"name\": \"Apple Inc.\"\n" +
               "        }\n" +
               "      ]\n" +
               "    }\n" +
               "  ]\n" +
               "}";
    }
}