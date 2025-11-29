package com.quantlabs.stockApp.indicator.management;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantlabs.stockApp.model.WatchlistData;

public class ConfigurationImporter {
    private final ObjectMapper mapper;
    
    public ConfigurationImporter() {
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    // Updated method signature to use JFrame instead of JComponent
    public ImportResult importConfiguration(File jsonFile, JFrame parentFrame) {
        try {
            JsonNode root = mapper.readTree(jsonFile);
            
            // Version compatibility check
            String version = root.path("version").asText();
            if (!"1.0".equals(version)) {
                int result = JOptionPane.showConfirmDialog(parentFrame,
                    "This configuration file version (" + version + ") may not be compatible.\n" +
                    "Continue with import?",
                    "Version Warning", JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION) {
                    return new ImportResult(false, "Import cancelled due to version incompatibility");
                }
            }
            
            // Confirmation dialog
            int confirm = JOptionPane.showConfirmDialog(parentFrame,
                "This will replace all current strategies, custom indicators, and watchlists.\n" +
                "Continue with import?",
                "Confirm Import", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return new ImportResult(false, "Import cancelled by user");
            }
            
            ImportData importData = parseImportData(root);
            return new ImportResult(true, "Configuration imported successfully", importData);
            
        } catch (Exception e) {
            return new ImportResult(false, "Failed to import configuration: " + e.getMessage());
        }
    }
    
    // Rest of the class remains the same...
    private ImportData parseImportData(JsonNode root) {
        ImportData importData = new ImportData();
        List<String> warnings = new ArrayList<>();
        
        // Import strategies
        JsonNode strategiesNode = root.path("strategies");
        for (JsonNode strategyNode : strategiesNode) {
            try {
                StrategyConfig strategy = deserializeStrategy(strategyNode);
                importData.getStrategies().add(strategy);
            } catch (Exception e) {
                warnings.add("Failed to import strategy: " + e.getMessage());
            }
        }
        
        // Import custom indicators
        JsonNode indicatorsNode = root.path("customIndicators");
        for (JsonNode indicatorNode : indicatorsNode) {
            try {
                CustomIndicator indicator = deserializeCustomIndicator(indicatorNode);
                importData.getCustomIndicators().add(indicator);
            } catch (Exception e) {
                warnings.add("Failed to import custom indicator: " + e.getMessage());
            }
        }
        
        // Import watchlists
        JsonNode watchlistsNode = root.path("watchlists");
        Iterator<String> watchlistNames = watchlistsNode.fieldNames();
        while (watchlistNames.hasNext()) {
            String watchlistName = watchlistNames.next();
            try {
                JsonNode watchlistNode = watchlistsNode.path(watchlistName);
                Set<String> symbols = new HashSet<>();
                String primarySymbol = "";
                
                if (watchlistNode.isArray()) {
                    // Old format - array of symbols
                    for (JsonNode symbolNode : watchlistNode) {
                        symbols.add(symbolNode.asText());
                    }
                } else if (watchlistNode.isObject()) {
                    // New format - object with symbols and primarySymbol
                    JsonNode symbolsNode = watchlistNode.path("symbols");
                    for (JsonNode symbolNode : symbolsNode) {
                        symbols.add(symbolNode.asText());
                    }
                    primarySymbol = watchlistNode.path("primarySymbol").asText("");
                }
                
                importData.getWatchlists().put(watchlistName, new WatchlistData(symbols, primarySymbol));
            } catch (Exception e) {
                warnings.add("Failed to import watchlist '" + watchlistName + "': " + e.getMessage());
            }
        }
        
        importData.setWarnings(warnings);
        importData.setExportDate(root.path("exportDate").asText());
        importData.setOriginalApplication(root.path("application").asText());
        
        return importData;
    }
    
    private StrategyConfig deserializeStrategy(JsonNode strategyNode) {
        StrategyConfig strategy = new StrategyConfig();
        
        strategy.setName(strategyNode.path("name").asText());
        strategy.setEnabled(strategyNode.path("enabled").asBoolean(true));
        strategy.setAlarmEnabled(strategyNode.path("alarmEnabled").asBoolean(false));
        strategy.setSymbolExclusive(strategyNode.path("symbolExclusive").asBoolean(false));
        
        if (!strategyNode.path("watchlistName").isNull()) {
            strategy.setWatchlistName(strategyNode.path("watchlistName").asText());
        }
        
        JsonNode symbolsNode = strategyNode.path("exclusiveSymbols");
        Set<String> exclusiveSymbols = new HashSet<>();
        for (JsonNode symbolNode : symbolsNode) {
            exclusiveSymbols.add(symbolNode.asText());
        }
        strategy.setExclusiveSymbols(exclusiveSymbols);
        
        JsonNode parametersNode = strategyNode.path("parameters");
        if (parametersNode.isObject()) {
            Map<String, Object> parameters = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = parametersNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                parameters.put(field.getKey(), convertJsonValue(field.getValue()));
            }
            strategy.setParameters(parameters);
        }
        
        JsonNode strategyParamsNode = strategyNode.path("strategyParameters");
        if (strategyParamsNode.isObject()) {
            Map<String, Map<String, Object>> strategyParameters = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> indicatorFields = strategyParamsNode.fields();
            while (indicatorFields.hasNext()) {
                Map.Entry<String, JsonNode> indicatorField = indicatorFields.next();
                Map<String, Object> indicatorParams = new HashMap<>();
                Iterator<Map.Entry<String, JsonNode>> paramFields = indicatorField.getValue().fields();
                while (paramFields.hasNext()) {
                    Map.Entry<String, JsonNode> paramField = paramFields.next();
                    indicatorParams.put(paramField.getKey(), convertJsonValue(paramField.getValue()));
                }
                strategyParameters.put(indicatorField.getKey(), indicatorParams);
            }
            strategy.setStrategyParameters(strategyParameters);
        }
        
        JsonNode timeframeIndicatorsNode = strategyNode.path("timeframeIndicators");
        if (timeframeIndicatorsNode.isObject()) {
            Map<String, Set<String>> timeframeIndicators = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> timeframeFields = timeframeIndicatorsNode.fields();
            while (timeframeFields.hasNext()) {
                Map.Entry<String, JsonNode> timeframeField = timeframeFields.next();
                Set<String> indicators = new HashSet<>();
                for (JsonNode indicatorNode : timeframeField.getValue()) {
                    indicators.add(indicatorNode.asText());
                }
                timeframeIndicators.put(timeframeField.getKey(), indicators);
            }
            strategy.setTimeframeIndicators(timeframeIndicators);
        }
        
        JsonNode timeframeCustomNode = strategyNode.path("timeframeCustomIndicators");
        if (timeframeCustomNode.isObject()) {
            Map<String, Set<CustomIndicator>> timeframeCustomIndicators = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> timeframeFields = timeframeCustomNode.fields();
            while (timeframeFields.hasNext()) {
                Map.Entry<String, JsonNode> timeframeField = timeframeFields.next();
                Set<CustomIndicator> customIndicators = new HashSet<>();
                for (JsonNode customIndicatorNode : timeframeField.getValue()) {
                    customIndicators.add(deserializeCustomIndicator(customIndicatorNode));
                }
                timeframeCustomIndicators.put(timeframeField.getKey(), customIndicators);
            }
            strategy.setTimeframeCustomIndicators(timeframeCustomIndicators);
        }
        
        return strategy;
    }
    
    private CustomIndicator deserializeCustomIndicator(JsonNode indicatorNode) {
        String name = indicatorNode.path("name").asText();
        String type = indicatorNode.path("type").asText();
        
        JsonNode parametersNode = indicatorNode.path("parameters");
        Map<String, Object> parameters = new HashMap<>();
        if (parametersNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = parametersNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                parameters.put(field.getKey(), convertJsonValue(field.getValue()));
            }
        }
        
        return new CustomIndicator(name, type, parameters);
    }

    // Add this overloaded method to ConfigurationImporter.java
    public ImportResult importConfiguration(JSONObject jsonObject, JFrame parentFrame) {
        try {
            // Version compatibility check
            String version = jsonObject.optString("version", "1.0");
            if (!"1.0".equals(version)) {
                int result = JOptionPane.showConfirmDialog(parentFrame,
                    "This configuration file version (" + version + ") may not be compatible.\n" +
                    "Continue with import?",
                    "Version Warning", JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION) {
                    return new ImportResult(false, "Import cancelled due to version incompatibility");
                }
            }
            
            // Confirmation dialog
            int confirm = JOptionPane.showConfirmDialog(parentFrame,
                "This will replace all current strategies, custom indicators, and watchlists.\n" +
                "Continue with import?",
                "Confirm Import", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return new ImportResult(false, "Import cancelled by user");
            }
            
            ImportData importData = parseImportData(jsonObject);
            return new ImportResult(true, "Configuration imported successfully", importData);
            
        } catch (Exception e) {
            return new ImportResult(false, "Failed to import configuration: " + e.getMessage());
        }
    }

    // Add this overloaded parseImportData method
    private ImportData parseImportData(JSONObject root) {
        ImportData importData = new ImportData();
        List<String> warnings = new ArrayList<>();
        
        // Import strategies
        JSONArray strategiesArray = root.optJSONArray("strategies");
        if (strategiesArray != null) {
            for (int i = 0; i < strategiesArray.length(); i++) {
                try {
                    JSONObject strategyNode = strategiesArray.getJSONObject(i);
                    StrategyConfig strategy = deserializeStrategyFromJSON(strategyNode);
                    importData.getStrategies().add(strategy);
                } catch (Exception e) {
                    warnings.add("Failed to import strategy: " + e.getMessage());
                }
            }
        }
        
        // Import custom indicators
        JSONArray indicatorsArray = root.optJSONArray("customIndicators");
        if (indicatorsArray != null) {
            for (int i = 0; i < indicatorsArray.length(); i++) {
                try {
                    JSONObject indicatorNode = indicatorsArray.getJSONObject(i);
                    CustomIndicator indicator = deserializeCustomIndicatorFromJSON(indicatorNode);
                    importData.getCustomIndicators().add(indicator);
                } catch (Exception e) {
                    warnings.add("Failed to import custom indicator: " + e.getMessage());
                }
            }
        }
        
     // Import watchlists
        JSONObject watchlistsNode = root.optJSONObject("watchlists");
        if (watchlistsNode != null) {
            Iterator<String> watchlistNames = watchlistsNode.keys();
            while (watchlistNames.hasNext()) {
                String watchlistName = watchlistNames.next();
                try {
                    Object watchlistObj = watchlistsNode.get(watchlistName);
                    Set<String> symbols = new HashSet<>();
                    String primarySymbol = "";
                    
                    if (watchlistObj instanceof JSONArray) {
                        // Old format - array of symbols
                        JSONArray symbolsArray = (JSONArray) watchlistObj;
                        for (int i = 0; i < symbolsArray.length(); i++) {
                            symbols.add(symbolsArray.getString(i));
                        }
                    } else if (watchlistObj instanceof JSONObject) {
                        // New format - object with symbols and primarySymbol
                        JSONObject watchlistData = (JSONObject) watchlistObj;
                        JSONArray symbolsArray = watchlistData.optJSONArray("symbols");
                        if (symbolsArray != null) {
                            for (int i = 0; i < symbolsArray.length(); i++) {
                                symbols.add(symbolsArray.getString(i));
                            }
                        }
                        primarySymbol = watchlistData.optString("primarySymbol", "");
                    }
                    
                    importData.getWatchlists().put(watchlistName, new WatchlistData(symbols, primarySymbol));
                } catch (Exception e) {
                    warnings.add("Failed to import watchlist '" + watchlistName + "': " + e.getMessage());
                }
            }
        }
        
        importData.setWarnings(warnings);
        importData.setExportDate(root.optString("exportDate", ""));
        importData.setOriginalApplication(root.optString("application", ""));
        
        return importData;
    }

    private StrategyConfig deserializeStrategyFromJSON(JSONObject strategyNode) {
        StrategyConfig strategy = new StrategyConfig();
        
        strategy.setName(strategyNode.optString("name", ""));
        strategy.setEnabled(strategyNode.optBoolean("enabled", true));
        strategy.setAlarmEnabled(strategyNode.optBoolean("alarmEnabled", false));
        strategy.setSymbolExclusive(strategyNode.optBoolean("symbolExclusive", false));
        
        if (!strategyNode.isNull("watchlistName")) {
            strategy.setWatchlistName(strategyNode.optString("watchlistName", null));
        }
        
        // Exclusive symbols array
        JSONArray symbolsArray = strategyNode.optJSONArray("exclusiveSymbols");
        Set<String> exclusiveSymbols = new HashSet<>();
        if (symbolsArray != null) {
            for (int i = 0; i < symbolsArray.length(); i++) {
                exclusiveSymbols.add(symbolsArray.getString(i));
            }
        }
        strategy.setExclusiveSymbols(exclusiveSymbols);
        
        // Parameters object
        JSONObject parametersNode = strategyNode.optJSONObject("parameters");
        if (parametersNode != null) {
            Map<String, Object> parameters = new HashMap<>();
            Iterator<String> paramKeys = parametersNode.keys();
            while (paramKeys.hasNext()) {
                String key = paramKeys.next();
                parameters.put(key, convertJSONValue(parametersNode, key));
            }
            strategy.setParameters(parameters);
        }
        
        // Strategy parameters nested object
        JSONObject strategyParamsNode = strategyNode.optJSONObject("strategyParameters");
        if (strategyParamsNode != null) {
            Map<String, Map<String, Object>> strategyParameters = new HashMap<>();
            Iterator<String> indicatorKeys = strategyParamsNode.keys();
            while (indicatorKeys.hasNext()) {
                String indicatorKey = indicatorKeys.next();
                JSONObject indicatorParamsNode = strategyParamsNode.optJSONObject(indicatorKey);
                if (indicatorParamsNode != null) {
                    Map<String, Object> indicatorParams = new HashMap<>();
                    Iterator<String> paramKeys = indicatorParamsNode.keys();
                    while (paramKeys.hasNext()) {
                        String paramKey = paramKeys.next();
                        indicatorParams.put(paramKey, convertJSONValue(indicatorParamsNode, paramKey));
                    }
                    strategyParameters.put(indicatorKey, indicatorParams);
                }
            }
            strategy.setStrategyParameters(strategyParameters);
        }
        
        // Timeframe indicators object
        JSONObject timeframeIndicatorsNode = strategyNode.optJSONObject("timeframeIndicators");
        if (timeframeIndicatorsNode != null) {
            Map<String, Set<String>> timeframeIndicators = new HashMap<>();
            Iterator<String> timeframeKeys = timeframeIndicatorsNode.keys();
            while (timeframeKeys.hasNext()) {
                String timeframeKey = timeframeKeys.next();
                JSONArray indicatorsArray = timeframeIndicatorsNode.optJSONArray(timeframeKey);
                if (indicatorsArray != null) {
                    Set<String> indicators = new HashSet<>();
                    for (int i = 0; i < indicatorsArray.length(); i++) {
                        indicators.add(indicatorsArray.getString(i));
                    }
                    timeframeIndicators.put(timeframeKey, indicators);
                }
            }
            strategy.setTimeframeIndicators(timeframeIndicators);
        }
        
        // Timeframe custom indicators object
        JSONObject timeframeCustomNode = strategyNode.optJSONObject("timeframeCustomIndicators");
        if (timeframeCustomNode != null) {
            Map<String, Set<CustomIndicator>> timeframeCustomIndicators = new HashMap<>();
            Iterator<String> timeframeKeys = timeframeCustomNode.keys();
            while (timeframeKeys.hasNext()) {
                String timeframeKey = timeframeKeys.next();
                JSONArray customIndicatorsArray = timeframeCustomNode.optJSONArray(timeframeKey);
                if (customIndicatorsArray != null) {
                    Set<CustomIndicator> customIndicators = new HashSet<>();
                    for (int i = 0; i < customIndicatorsArray.length(); i++) {
                        JSONObject customIndicatorNode = customIndicatorsArray.getJSONObject(i);
                        customIndicators.add(deserializeCustomIndicatorFromJSON(customIndicatorNode));
                    }
                    timeframeCustomIndicators.put(timeframeKey, customIndicators);
                }
            }
            strategy.setTimeframeCustomIndicators(timeframeCustomIndicators);
        }
        
        return strategy;
    }

    private CustomIndicator deserializeCustomIndicatorFromJSON(JSONObject indicatorNode) {
        String name = indicatorNode.optString("name", "");
        String type = indicatorNode.optString("type", "");
        
        // Parameters object
        JSONObject parametersNode = indicatorNode.optJSONObject("parameters");
        Map<String, Object> parameters = new HashMap<>();
        if (parametersNode != null) {
            Iterator<String> paramKeys = parametersNode.keys();
            while (paramKeys.hasNext()) {
                String key = paramKeys.next();
                parameters.put(key, convertJSONValue(parametersNode, key));
            }
        }
        
        return new CustomIndicator(name, type, parameters);
    }

    private Object convertJSONValue(JSONObject jsonObject, String key) {
        if (jsonObject.isNull(key)) {
            return null;
        }
        
        // Try to determine the type based on the value
        Object value = jsonObject.get(key);
        if (value instanceof Boolean) {
            return value;
        } else if (value instanceof Integer) {
            return value;
        } else if (value instanceof Long) {
            return value;
        } else if (value instanceof Double) {
            return value;
        } else if (value instanceof String) {
            return value;
        } else {
            return value.toString();
        }
    }
    
    private Object convertJsonValue(JsonNode valueNode) {
        if (valueNode.isBoolean()) {
            return valueNode.asBoolean();
        } else if (valueNode.isInt()) {
            return valueNode.asInt();
        } else if (valueNode.isLong()) {
            return valueNode.asLong();
        } else if (valueNode.isDouble()) {
            return valueNode.asDouble();
        } else if (valueNode.isTextual()) {
            return valueNode.asText();
        } else if (valueNode.isNull()) {
            return null;
        } else {
            return valueNode.toString();
        }
    }
    
    public static class ImportResult {
        private final boolean success;
        private final String message;
        private final ImportData importData;
        
        public ImportResult(boolean success, String message) {
            this(success, message, null);
        }
        
        public ImportResult(boolean success, String message, ImportData importData) {
            this.success = success;
            this.message = message;
            this.importData = importData;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public ImportData getImportData() { return importData; }
    }
    
    public static class ImportData {
        private final List<StrategyConfig> strategies = new ArrayList<>();
        private final Set<CustomIndicator> customIndicators = new HashSet<>();
        private final Map<String, WatchlistData> watchlists = new HashMap<>();
        private final List<String> warnings = new ArrayList<>();
        private String exportDate;
        private String originalApplication;
        
        // Getters
        public List<StrategyConfig> getStrategies() { return strategies; }
        public Set<CustomIndicator> getCustomIndicators() { return customIndicators; }
        
        public Map<String, WatchlistData> getWatchlists() {
			return watchlists;
		}
		public List<String> getWarnings() { return warnings; }
        public String getExportDate() { return exportDate; }
        public String getOriginalApplication() { return originalApplication; }
        
        // Setters
        public void setWarnings(List<String> warnings) { this.warnings.addAll(warnings); }
        public void setExportDate(String exportDate) { this.exportDate = exportDate; }
        public void setOriginalApplication(String originalApplication) { this.originalApplication = originalApplication; }
    }
}