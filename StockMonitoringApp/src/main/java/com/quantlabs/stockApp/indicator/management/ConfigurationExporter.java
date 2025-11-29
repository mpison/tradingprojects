package com.quantlabs.stockApp.indicator.management;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quantlabs.stockApp.model.WatchlistData;

public class ConfigurationExporter {
    private final ObjectMapper mapper;
    
    public ConfigurationExporter() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    // Updated method signature to use JFrame instead of JComponent
    public ExportResult exportConfiguration(File jsonFile, 
            List<StrategyConfig> strategies,
            Set<CustomIndicator> customIndicators,
            Map<String, WatchlistData> watchlists,
            JFrame parentFrame) {
        try {
            // Check if file exists and confirm overwrite
            if (jsonFile.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(parentFrame,
                    "File already exists. Overwrite?",
                    "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
                if (overwrite != JOptionPane.YES_OPTION) {
                    return new ExportResult(false, "Export cancelled by user");
                }
            }
            
            String exportNotes = JOptionPane.showInputDialog(parentFrame, 
                "Enter export notes (optional):", 
                "Export Configuration", 
                JOptionPane.QUESTION_MESSAGE);
            
            ObjectNode root = createExportData(strategies, customIndicators, watchlists, exportNotes);
            mapper.writeValue(jsonFile, root);
            
            return new ExportResult(true, 
                "Configuration exported successfully to: " + jsonFile.getName(),
                strategies.size(), 
                customIndicators.size(), 
                watchlists.size());
            
        } catch (Exception e) {
            return new ExportResult(false, "Failed to export configuration: " + e.getMessage());
        }
    }
    
    // Rest of the class remains the same...
    private ObjectNode createExportData(List<StrategyConfig> strategies,
            Set<CustomIndicator> customIndicators,
            Map<String, WatchlistData> watchlists,
            String exportNotes) {
        ObjectNode root = mapper.createObjectNode();
        
        // Basic metadata
        root.put("version", "1.0");
        root.put("exportDate", Instant.now().toString());
        root.put("application", "Indicators Management System");
        
        // Strategies array
        ArrayNode strategiesArray = mapper.createArrayNode();
        for (StrategyConfig strategy : strategies) {
            strategiesArray.add(serializeStrategy(strategy));
        }
        root.set("strategies", strategiesArray);
        
        // Custom indicators array
        ArrayNode indicatorsArray = mapper.createArrayNode();
        for (CustomIndicator indicator : customIndicators) {
            indicatorsArray.add(serializeCustomIndicator(indicator));
        }
        root.set("customIndicators", indicatorsArray);
        
        // Watchlists object
        ObjectNode watchlistsObject = mapper.createObjectNode();
        for (Map.Entry<String, WatchlistData> entry : watchlists.entrySet()) {
            ObjectNode watchlistNode = mapper.createObjectNode();
            
            // Symbols array
            ArrayNode symbolsArray = mapper.createArrayNode();
            for (String symbol : entry.getValue().getSymbols()) {
                symbolsArray.add(symbol);
            }
            watchlistNode.set("symbols", symbolsArray);
            
            // Primary symbol
            watchlistNode.put("primarySymbol", entry.getValue().getPrimarySymbol());
            
            watchlistsObject.set(entry.getKey(), watchlistNode);
        }
        root.set("watchlists", watchlistsObject);
        
        // Metadata object
        ObjectNode metadata = createMetadata(strategies.size(), customIndicators.size(), 
                                           watchlists.size(), exportNotes);
        root.set("metadata", metadata);
        
        return root;
    }
    
    private ObjectNode createMetadata(int strategyCount, int indicatorCount, 
                                    int watchlistCount, String exportNotes) {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("totalStrategies", strategyCount);
        metadata.put("totalCustomIndicators", indicatorCount);
        metadata.put("totalWatchlists", watchlistCount);
        metadata.put("exportedBy", System.getProperty("user.name"));
        metadata.put("exportNotes", exportNotes != null ? exportNotes : "");
        
        ObjectNode compatibility = mapper.createObjectNode();
        compatibility.put("minVersion", "3.0.0");
        compatibility.put("maxVersion", "5.0.0");
        metadata.set("compatibility", compatibility);
        
        return metadata;
    }
    
    private ObjectNode serializeStrategy(StrategyConfig strategy) {
        ObjectNode strategyNode = mapper.createObjectNode();
        
        strategyNode.put("name", strategy.getName());
        strategyNode.put("enabled", strategy.isEnabled());
        strategyNode.put("alarmEnabled", strategy.isAlarmEnabled());
        strategyNode.put("symbolExclusive", strategy.isSymbolExclusive());
        
        if (strategy.getWatchlistName() != null) {
            strategyNode.put("watchlistName", strategy.getWatchlistName());
        } else {
            strategyNode.putNull("watchlistName");
        }
        
        // Exclusive symbols array
        ArrayNode symbolsArray = mapper.createArrayNode();
        for (String symbol : strategy.getExclusiveSymbols()) {
            symbolsArray.add(symbol);
        }
        strategyNode.set("exclusiveSymbols", symbolsArray);
        
        // Parameters object
        ObjectNode parametersNode = mapper.createObjectNode();
        for (Map.Entry<String, Object> entry : strategy.getParameters().entrySet()) {
            addDynamicValue(parametersNode, entry.getKey(), entry.getValue());
        }
        strategyNode.set("parameters", parametersNode);
        
        // Strategy parameters nested object
        ObjectNode strategyParamsNode = mapper.createObjectNode();
        for (Map.Entry<String, Map<String, Object>> indicatorEntry : strategy.getStrategyParameters().entrySet()) {
            ObjectNode indicatorParamsNode = mapper.createObjectNode();
            for (Map.Entry<String, Object> paramEntry : indicatorEntry.getValue().entrySet()) {
                addDynamicValue(indicatorParamsNode, paramEntry.getKey(), paramEntry.getValue());
            }
            strategyParamsNode.set(indicatorEntry.getKey(), indicatorParamsNode);
        }
        strategyNode.set("strategyParameters", strategyParamsNode);
        
        // Timeframe indicators object
        ObjectNode timeframeIndicatorsNode = mapper.createObjectNode();
        for (Map.Entry<String, Set<String>> timeframeEntry : strategy.getTimeframeIndicators().entrySet()) {
            ArrayNode indicatorsArray = mapper.createArrayNode();
            for (String indicator : timeframeEntry.getValue()) {
                indicatorsArray.add(indicator);
            }
            timeframeIndicatorsNode.set(timeframeEntry.getKey(), indicatorsArray);
        }
        strategyNode.set("timeframeIndicators", timeframeIndicatorsNode);
        
        // Timeframe custom indicators object
        ObjectNode timeframeCustomNode = mapper.createObjectNode();
        for (Map.Entry<String, Set<CustomIndicator>> timeframeEntry : strategy.getTimeframeCustomIndicators().entrySet()) {
            ArrayNode customIndicatorsArray = mapper.createArrayNode();
            for (CustomIndicator indicator : timeframeEntry.getValue()) {
                customIndicatorsArray.add(serializeCustomIndicator(indicator));
            }
            timeframeCustomNode.set(timeframeEntry.getKey(), customIndicatorsArray);
        }
        strategyNode.set("timeframeCustomIndicators", timeframeCustomNode);
        
        return strategyNode;
    }
    
    private ObjectNode serializeCustomIndicator(CustomIndicator indicator) {
        ObjectNode indicatorNode = mapper.createObjectNode();
        
        indicatorNode.put("name", indicator.getName());
        indicatorNode.put("type", indicator.getType());
        indicatorNode.put("displayName", indicator.getDisplayName());
        
        // Parameters object
        ObjectNode parametersNode = mapper.createObjectNode();
        for (Map.Entry<String, Object> entry : indicator.getParameters().entrySet()) {
            addDynamicValue(parametersNode, entry.getKey(), entry.getValue());
        }
        indicatorNode.set("parameters", parametersNode);
        
        return indicatorNode;
    }
    
    private void addDynamicValue(ObjectNode node, String key, Object value) {
        if (value instanceof String) {
            node.put(key, (String) value);
        } else if (value instanceof Integer) {
            node.put(key, (Integer) value);
        } else if (value instanceof Long) {
            node.put(key, (Long) value);
        } else if (value instanceof Double) {
            node.put(key, (Double) value);
        } else if (value instanceof Boolean) {
            node.put(key, (Boolean) value);
        } else if (value instanceof Float) {
            node.put(key, (Float) value);
        } else if (value != null) {
            node.put(key, value.toString());
        } else {
            node.putNull(key);
        }
    }


    // Add this overloaded method to ConfigurationExporter.java
    public JSONObject createExportJSONObject(List<StrategyConfig> strategies,
            Set<CustomIndicator> customIndicators,
            Map<String, WatchlistData> watchlists,
            String exportNotes) {
        JSONObject root = new JSONObject();
        
        // Basic metadata
        root.put("version", "1.0");
        root.put("exportDate", Instant.now().toString());
        root.put("application", "Indicators Management System");
        
        // Strategies array
        JSONArray strategiesArray = new JSONArray();
        for (StrategyConfig strategy : strategies) {
            strategiesArray.put(serializeStrategyToJSON(strategy));
        }
        root.put("strategies", strategiesArray);
        
        // Custom indicators array
        JSONArray indicatorsArray = new JSONArray();
        for (CustomIndicator indicator : customIndicators) {
            indicatorsArray.put(serializeCustomIndicatorToJSON(indicator));
        }
        root.put("customIndicators", indicatorsArray);
        
        // Watchlists object
        JSONObject watchlistsObject = new JSONObject();
        for (Map.Entry<String, WatchlistData> entry : watchlists.entrySet()) {
            JSONObject watchlistNode = new JSONObject();
            
            // Symbols array
            JSONArray symbolsArray = new JSONArray();
            for (String symbol : entry.getValue().getSymbols()) {
                symbolsArray.put(symbol);
            }
            watchlistNode.put("symbols", symbolsArray);
            
            // Primary symbol
            watchlistNode.put("primarySymbol", entry.getValue().getPrimarySymbol());
            
            watchlistsObject.put(entry.getKey(), watchlistNode);
        }
        root.put("watchlists", watchlistsObject);
        
        // Metadata object
        JSONObject metadata = createMetadataToJSON(strategies.size(), customIndicators.size(), 
                                                 watchlists.size(), exportNotes);
        root.put("metadata", metadata);
        
        return root;
    }

    private JSONObject serializeStrategyToJSON(StrategyConfig strategy) {
        JSONObject strategyNode = new JSONObject();
        
        strategyNode.put("name", strategy.getName());
        strategyNode.put("enabled", strategy.isEnabled());
        strategyNode.put("alarmEnabled", strategy.isAlarmEnabled());
        strategyNode.put("symbolExclusive", strategy.isSymbolExclusive());
        
        if (strategy.getWatchlistName() != null) {
            strategyNode.put("watchlistName", strategy.getWatchlistName());
        } else {
            strategyNode.put("watchlistName", JSONObject.NULL);
        }
        
        // Exclusive symbols array
        JSONArray symbolsArray = new JSONArray();
        for (String symbol : strategy.getExclusiveSymbols()) {
            symbolsArray.put(symbol);
        }
        strategyNode.put("exclusiveSymbols", symbolsArray);
        
        // Parameters object
        JSONObject parametersNode = new JSONObject();
        for (Map.Entry<String, Object> entry : strategy.getParameters().entrySet()) {
            addDynamicValueToJSON(parametersNode, entry.getKey(), entry.getValue());
        }
        strategyNode.put("parameters", parametersNode);
        
        // Strategy parameters nested object
        JSONObject strategyParamsNode = new JSONObject();
        for (Map.Entry<String, Map<String, Object>> indicatorEntry : strategy.getStrategyParameters().entrySet()) {
            JSONObject indicatorParamsNode = new JSONObject();
            for (Map.Entry<String, Object> paramEntry : indicatorEntry.getValue().entrySet()) {
                addDynamicValueToJSON(indicatorParamsNode, paramEntry.getKey(), paramEntry.getValue());
            }
            strategyParamsNode.put(indicatorEntry.getKey(), indicatorParamsNode);
        }
        strategyNode.put("strategyParameters", strategyParamsNode);
        
        // Timeframe indicators object
        JSONObject timeframeIndicatorsNode = new JSONObject();
        for (Map.Entry<String, Set<String>> timeframeEntry : strategy.getTimeframeIndicators().entrySet()) {
            JSONArray indicatorsArray = new JSONArray();
            for (String indicator : timeframeEntry.getValue()) {
                indicatorsArray.put(indicator);
            }
            timeframeIndicatorsNode.put(timeframeEntry.getKey(), indicatorsArray);
        }
        strategyNode.put("timeframeIndicators", timeframeIndicatorsNode);
        
        // Timeframe custom indicators object
        JSONObject timeframeCustomNode = new JSONObject();
        for (Map.Entry<String, Set<CustomIndicator>> timeframeEntry : strategy.getTimeframeCustomIndicators().entrySet()) {
            JSONArray customIndicatorsArray = new JSONArray();
            for (CustomIndicator indicator : timeframeEntry.getValue()) {
                customIndicatorsArray.put(serializeCustomIndicatorToJSON(indicator));
            }
            timeframeCustomNode.put(timeframeEntry.getKey(), customIndicatorsArray);
        }
        strategyNode.put("timeframeCustomIndicators", timeframeCustomNode);
        
        return strategyNode;
    }

    private JSONObject serializeCustomIndicatorToJSON(CustomIndicator indicator) {
        JSONObject indicatorNode = new JSONObject();
        
        indicatorNode.put("name", indicator.getName());
        indicatorNode.put("type", indicator.getType());
        indicatorNode.put("displayName", indicator.getDisplayName());
        
        // Parameters object
        JSONObject parametersNode = new JSONObject();
        for (Map.Entry<String, Object> entry : indicator.getParameters().entrySet()) {
            addDynamicValueToJSON(parametersNode, entry.getKey(), entry.getValue());
        }
        indicatorNode.put("parameters", parametersNode);
        
        return indicatorNode;
    }

    private JSONObject createMetadataToJSON(int strategyCount, int indicatorCount, 
                                          int watchlistCount, String exportNotes) {
        JSONObject metadata = new JSONObject();
        metadata.put("totalStrategies", strategyCount);
        metadata.put("totalCustomIndicators", indicatorCount);
        metadata.put("totalWatchlists", watchlistCount);
        metadata.put("exportedBy", System.getProperty("user.name"));
        metadata.put("exportNotes", exportNotes != null ? exportNotes : "");
        
        JSONObject compatibility = new JSONObject();
        compatibility.put("minVersion", "3.0.0");
        compatibility.put("maxVersion", "5.0.0");
        metadata.put("compatibility", compatibility);
        
        return metadata;
    }

    private void addDynamicValueToJSON(JSONObject node, String key, Object value) {
        if (value == null) {
            node.put(key, JSONObject.NULL);
        } else if (value instanceof String) {
            node.put(key, (String) value);
        } else if (value instanceof Integer) {
            node.put(key, (Integer) value);
        } else if (value instanceof Long) {
            node.put(key, (Long) value);
        } else if (value instanceof Double) {
            node.put(key, (Double) value);
        } else if (value instanceof Boolean) {
            node.put(key, (Boolean) value);
        } else if (value instanceof Float) {
            node.put(key, (Float) value);
        } else {
            node.put(key, value.toString());
        }
    }
    
    
    public static class ExportResult {
        private final boolean success;
        private final String message;
        private final int strategyCount;
        private final int indicatorCount;
        private final int watchlistCount;
        
        public ExportResult(boolean success, String message) {
            this(success, message, 0, 0, 0);
        }
        
        public ExportResult(boolean success, String message, int strategyCount, 
                          int indicatorCount, int watchlistCount) {
            this.success = success;
            this.message = message;
            this.strategyCount = strategyCount;
            this.indicatorCount = indicatorCount;
            this.watchlistCount = watchlistCount;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getStrategyCount() { return strategyCount; }
        public int getIndicatorCount() { return indicatorCount; }
        public int getWatchlistCount() { return watchlistCount; }
    }
}