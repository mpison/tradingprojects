package com.quantlabs.QuantTester.v3.alert.service;

import com.quantlabs.QuantTester.v3.alert.*;
import com.quantlabs.QuantTester.v3.alert.config.AppConfig;
import com.quantlabs.QuantTester.v3.alert.data.StockDataProvider;
import com.quantlabs.QuantTester.v3.alert.indicators.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

public class AlertService {
    private static final Logger logger = Logger.getLogger(AlertService.class.getName());
    
    private final MessageRepository messageRepository;
    private StockDataService stockDataService;
    private final IndicatorCalculatorFactory indicatorFactory;
    private AppConfig appConfig;
    private final Map<String, Long> alertTimestamps = new HashMap<>();
    private int apiCalls = 0;
    private long lastResetTime = System.currentTimeMillis();
    private static final int API_LIMIT = 2000;

    public AlertService(MessageRepository messageRepository,
                      StockDataService stockDataService,
                      IndicatorCalculatorFactory indicatorFactory) {
        this.messageRepository = messageRepository;
        this.stockDataService = stockDataService;
        this.indicatorFactory = indicatorFactory;
    }

    public void setConfig(AppConfig config) {
        this.appConfig = Objects.requireNonNull(config);
    }

    public void setDataProvider(StockDataProvider dataProvider) {
        this.stockDataService = new StockDataService(dataProvider);
    }

    public void checkForCrossovers() {
        if (appConfig == null) {
            throw new IllegalStateException("Configuration not loaded");
        }

        try {
            checkApiRateLimit();
            processConfig();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during crossover check", e);
        }
    }

    private void checkApiRateLimit() {
        if (System.currentTimeMillis() - lastResetTime >= 60_000) {
            apiCalls = 0;
            lastResetTime = System.currentTimeMillis();
        }
        if (apiCalls >= API_LIMIT) {
            throw new IllegalStateException("API rate limit exceeded");
        }
    }

    private void processConfig() throws JSONException {
        try {
            
            // Parse the JSON
            JSONObject configJson = appConfig.getConfig(); //new JSONObject(configString);
            
            if (configJson == null) {
                throw new JSONException("Configuration is empty");
            }
            
            // Validate required fields
            if (!configJson.has("mappings")) {
                throw new JSONException("Configuration missing 'mappings' array");
            }

            JSONArray mappings = configJson.getJSONArray("mappings");
            if (mappings.length() == 0) {
                logger.warning("Configuration contains no mappings");
                return;
            }
            
            // Process each mapping
            for (int i = 0; i < mappings.length(); i++) {
                try {
                    JSONObject mapping = mappings.getJSONObject(i);
                    validateMapping(mapping); // New validation method
                    processMapping(mapping, configJson);
                } catch (JSONException e) {
                    logger.log(Level.WARNING, "Error processing mapping at index " + i, e);
                    continue; // Skip invalid mapping but continue with others
                }
            }
        } catch (JSONException e) {
            logger.log(Level.SEVERE, "Invalid configuration JSON", e);
            throw e;
        }
    }

    // New helper method for mapping validation
    private void validateMapping(JSONObject mapping) throws JSONException {
        String[] requiredFields = {"name", "watchlist", "combination"};
        for (String field : requiredFields) {
            if (!mapping.has(field) || mapping.isNull(field)) {
                throw new JSONException("Missing required field in mapping: " + field);
            }
        }
    }

    private void processMapping(JSONObject mapping, JSONObject config) throws JSONException {
        String watchlistName = mapping.getString("watchlist");
        String combinationName = mapping.getString("combination");
        
        JSONObject watchlist = findWatchlist(config, watchlistName);
        JSONObject combination = findCombination(config, combinationName);
        
        if (watchlist == null || combination == null) {
            logger.warning(String.format("Invalid mapping: watchlist '%s' or combination '%s' not found", 
                watchlistName, combinationName));
            return;
        }

        processStocks(watchlist.getJSONArray("stocks"), 
                     combination.getJSONArray("indicators"), 
                     combinationName, 
                     watchlistName);
    }

    private void processStocks(JSONArray stocks, JSONArray indicators,
                             String combinationName, String watchlistName) throws JSONException {
        Set<String> timeframes = collectTimeframes(indicators);
        int maxShift = calculateMaxShift(indicators);

        for (int i = 0; i < stocks.length(); i++) {
            processStock(stocks.getJSONObject(i), indicators, combinationName, watchlistName, timeframes, maxShift);
        }
    }

    private void processStock(JSONObject stock, JSONArray indicators,
                            String combinationName, String watchlistName,
                            Set<String> timeframes, int maxShift) throws JSONException {
        String symbol = stock.getString("symbol");
        String alertKey = symbol + "_" + combinationName + "_combo";
        
        if (shouldSkipAlert(alertKey)) {
            return;
        }

        Map<String, List<OHLCDataItem>> timeframeData = fetchTimeframeData(symbol, timeframes, maxShift);
        if (timeframeData == null || timeframeData.isEmpty()) {
            return;
        }

        List<String> crossovers = evaluateIndicators(indicators, timeframeData);
        if (!crossovers.isEmpty()) {
            createAlert(symbol, combinationName, watchlistName, crossovers, alertKey);
        }
    }

    private Set<String> collectTimeframes(JSONArray indicators) throws JSONException {
        Set<String> timeframes = new HashSet<>();
        for (int i = 0; i < indicators.length(); i++) {
            timeframes.add(indicators.getJSONObject(i).getString("timeframe"));
        }
        return timeframes;
    }

    private int calculateMaxShift(JSONArray indicators) throws JSONException {
        int maxShift = 0;
        for (int i = 0; i < indicators.length(); i++) {
            maxShift = Math.max(maxShift, indicators.getJSONObject(i).getInt("shift"));
        }
        return maxShift;
    }

    private boolean shouldSkipAlert(String alertKey) {
        long recheckInterval = 60 * 60_000; // 1 hour
        return alertTimestamps.containsKey(alertKey) && 
               System.currentTimeMillis() - alertTimestamps.get(alertKey) < recheckInterval;
    }

    private Map<String, List<OHLCDataItem>> fetchTimeframeData(String symbol, 
                                                             Set<String> timeframes, 
                                                             int maxShift) {
        Map<String, List<OHLCDataItem>> timeframeData = new HashMap<>();
        
        for (String timeframe : timeframes) {
            List<OHLCDataItem> data = stockDataService.fetchData(symbol, timeframe, maxShift + 1);
            if (data == null || data.isEmpty()) {
                logger.warning("Failed to fetch data for " + symbol + " (" + timeframe + ")");
                return null;
            }
            timeframeData.put(timeframe, data);
            apiCalls++;
        }
        
        return timeframeData;
    }

    private List<String> evaluateIndicators(JSONArray indicators, 
                                          Map<String, List<OHLCDataItem>> timeframeData) throws JSONException {
        List<String> crossovers = new ArrayList<>();
        
        for (int i = 0; i < indicators.length(); i++) {
            JSONObject indicator = indicators.getJSONObject(i);
            String result = evaluateIndicator(indicator, timeframeData);
            if (result == null) {
                return Collections.emptyList();
            }
            crossovers.add(result);
        }
        
        return crossovers;
    }

    private String evaluateIndicator(JSONObject indicator, 
                                   Map<String, List<OHLCDataItem>> timeframeData) {
        try {
            String indType = indicator.getString("type");
            String timeframe = indicator.getString("timeframe");
            int shift = indicator.getInt("shift");
            JSONObject params = indicator.getJSONObject("params");
            
            List<OHLCDataItem> data = timeframeData.get(timeframe);
            if (data == null) {
                logger.warning("No data available for timeframe: " + timeframe);
                return null;
            }
            
            IndicatorCalculator calculator = indicatorFactory.getCalculator(indType);
            if (calculator == null) {
                logger.warning("No calculator available for indicator: " + indType);
                return null;
            }
            
            return calculator.calculate(createBarSeries(data), params, shift);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error evaluating indicator", e);
            return null;
        }
    }

    private BarSeries createBarSeries(List<OHLCDataItem> data) {
        BarSeries series = new BaseBarSeries();
        for (OHLCDataItem item : data) {
            ZonedDateTime date = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(item.getTimestamp()), 
                ZoneId.systemDefault()
            );
            
            series.addBar(
                date,
                series.numOf(item.getOpen()),
                series.numOf(item.getHigh()),
                series.numOf(item.getLow()),
                series.numOf(item.getClose()),
                series.numOf(item.getVolume()),
                series.numOf(0) // Amount
            );
        }
        return series;
    }

    private void createAlert(String symbol, String combinationName, 
                           String watchlistName, List<String> crossovers, 
                           String alertKey) {
        String timestampStr = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        String header = String.format("[%s] %s: Alert triggered", timestampStr, symbol);
        String body = String.format("[%s] %s: %s criteria met in %s: %s",
            timestampStr,
            symbol,
            combinationName,
            watchlistName,
            String.join(", ", crossovers)
        );
        
        Message message = new Message(header, body, Message.MessageStatus.UNREAD, LocalDateTime.now());
        messageRepository.addMessage(message);
        alertTimestamps.put(alertKey, System.currentTimeMillis());
        
        logger.info("Created alert for " + symbol + ": " + header);
    }

    private JSONObject findWatchlist(JSONObject config, String name) throws JSONException {
        JSONArray watchlists = config.getJSONArray("watchlists");
        for (int i = 0; i < watchlists.length(); i++) {
            JSONObject wl = watchlists.getJSONObject(i);
            if (name.equals(wl.getString("name"))) {
                return wl;
            }
        }
        return null;
    }

    private JSONObject findCombination(JSONObject config, String name) throws JSONException {
        JSONArray combinations = config.getJSONArray("combinations");
        for (int i = 0; i < combinations.length(); i++) {
            JSONObject combo = combinations.getJSONObject(i);
            if (name.equals(combo.getString("name"))) {
                return combo;
            }
        }
        return null;
    }

    public List<Message> getRecentAlerts() {
        return messageRepository.getMessages().stream()
            .sorted(Comparator.comparing(Message::getTimestamp).reversed())
            .limit(100)
            .collect(Collectors.toList());
    }

    public void resetApiCounter() {
        apiCalls = 0;
        lastResetTime = System.currentTimeMillis();
    }
}