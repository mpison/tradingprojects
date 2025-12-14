package com.quantlabs.stockApp.config;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages configuration saving and loading for StockDashboard
 */
public class DashboardConfigManager {
    private Component parentComponent;
    private java.util.function.Consumer<String> logger;

    public DashboardConfigManager(Component parentComponent, java.util.function.Consumer<String> logger) {
        this.parentComponent = parentComponent;
        this.logger = logger;
    }

    /**
     * Save dashboard configuration to JSON file
     */
    public boolean saveConfigurationToFile(Map<String, Object> configData) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Dashboard Configuration");
        fileChooser.setSelectedFile(new File("dashboard_config.json"));

        int userSelection = fileChooser.showSaveDialog(parentComponent);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".json")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".json");
            }

            try {
                JSONObject config = new JSONObject();

                // Add metadata
                config.put("exportDate", new Date().toString());
                config.put("version", "1.0");
                config.put("application", "StockDashboard");

                // Save all configuration data with proper JSON conversion
                for (Map.Entry<String, Object> entry : configData.entrySet()) {
                    try {
                        Object jsonValue = convertToJSONCompatible(entry.getValue());
                        config.put(entry.getKey(), jsonValue);
                    } catch (Exception e) {
                        log("Warning: Could not save configuration key '" + entry.getKey() + "': " + e.getMessage());
                    }
                }

                // Write to file with pretty print
                try (PrintWriter out = new PrintWriter(fileToSave)) {
                    out.println(config.toString(4)); // Pretty print with 4-space indent
                }

                log("✅ Configuration saved successfully to: " + fileToSave.getAbsolutePath());
                
                JOptionPane.showMessageDialog(parentComponent, 
                    "Configuration saved successfully!\n\n" +
                    "File: " + fileToSave.getName() + "\n" +
                    "Location: " + fileToSave.getParent(),
                    "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                
                return true;

            } catch (Exception e) {
                log("❌ Error saving configuration: " + e.getMessage());
                JOptionPane.showMessageDialog(parentComponent, 
                    "Error saving configuration: " + e.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    /**
     * Load dashboard configuration from JSON file
     */
    public Map<String, Object> loadConfigurationFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Dashboard Configuration");

        int userSelection = fileChooser.showOpenDialog(parentComponent);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToLoad = fileChooser.getSelectedFile();

            try {
                String content = new String(Files.readAllBytes(fileToLoad.toPath()));
                JSONObject config = new JSONObject(new JSONTokener(content));

                Map<String, Object> configData = new HashMap<>();

                // Extract all configuration data with proper conversion
                Iterator<String> keys = config.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (!isMetadataKey(key)) { // Skip metadata keys
                        try {
                            Object value = convertFromJSON(config.get(key), key);
                            configData.put(key, value);
                        } catch (Exception e) {
                            log("Warning: Could not load configuration key '" + key + "': " + e.getMessage());
                        }
                    }
                }

                log("✅ Configuration loaded successfully from: " + fileToLoad.getAbsolutePath());
                
                JOptionPane.showMessageDialog(parentComponent, 
                    "Configuration loaded successfully!\n\n" +
                    "File: " + fileToLoad.getName() + "\n" +
                    "Export Date: " + config.optString("exportDate", "Unknown"),
                    "Load Successful", JOptionPane.INFORMATION_MESSAGE);
                
                return configData;

            } catch (Exception e) {
                log("❌ Error loading configuration: " + e.getMessage());
                JOptionPane.showMessageDialog(parentComponent, 
                    "Error loading configuration: " + e.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
        return new HashMap<>();
    }

    /**
     * Enhanced conversion to handle complex nested structures for JSON compatibility
     */
    private Object convertToJSONCompatible(Object value) {
        if (value == null) {
            return JSONObject.NULL;
        } else if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        } else if (value instanceof List) {
            JSONArray array = new JSONArray();
            for (Object item : (List<?>) value) {
                array.put(convertToJSONCompatible(item));
            }
            return array;
        } else if (value instanceof Set) {
            JSONArray array = new JSONArray();
            for (Object item : (Set<?>) value) {
                array.put(convertToJSONCompatible(item));
            }
            return array;
        } else if (value instanceof Map) {
            JSONObject obj = new JSONObject();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                String key = entry.getKey().toString();
                Object convertedValue = convertToJSONCompatible(entry.getValue());
                obj.put(key, convertedValue);
            }
            return obj;
        } else if (value instanceof Date) {
            return ((Date) value).getTime();
        } else {
            // For custom objects, convert to string representation
            return value.toString();
        }
    }

    /**
     * Enhanced conversion from JSON to handle complex nested structures
     * @param key2 
     */
    private Object convertFromJSON(Object jsonValue, String key2) {
        if (jsonValue == JSONObject.NULL || jsonValue == null) {
            return null;
        } else if (jsonValue instanceof JSONArray) {
            List<Object> list = new ArrayList<>();
            JSONArray array = (JSONArray) jsonValue;
            for (int i = 0; i < array.length(); i++) {
                list.add(convertFromJSON(array.get(i), key2));
            }
            return list;
        } else if (jsonValue instanceof JSONObject) {
            // Check if this is a complex object structure that needs special handling
            JSONObject obj = (JSONObject) jsonValue;
            
            if (key2.equals("customIndicatorCombinations" )) {
                return convertCustomIndicatorCombinations(obj);
            }
            
            // Handle indicatorConfig structure
            if (hasTimeframeKeys(obj)) {
                return convertIndicatorConfig(obj);
            }
            
            // Handle volumeAlertConfig structure
            if (obj.has("enabled") && obj.has("volume20MAEnabled") && obj.has("timeframeConfigs")) {
                return convertVolumeAlertConfig(obj);
            }
            
            // Handle zScoreAlertConfig structure
            if (obj.has("enabled") && obj.has("alertConfigs")) {
                return convertZScoreAlertConfig(obj);
            }
            
            // Handle zScoreColumnVisibility structure
            if (hasZScoreColumnKeys(obj)) {
                return convertZScoreColumnVisibility(obj);
            }
            
            // Handle columnVisibility array structure
            if (isColumnVisibilityArray(obj)) {
                return convertColumnVisibility(obj);
            }
            
            // Handle indicatorManagementConfig structure
            if (obj.has("customIndicators") || obj.has("strategyConfigs")) {
                return convertIndicatorManagementConfig(obj);
            }
            
            // Handle strategyConfig structure
            if (obj.has("enabledStrategies") || obj.has("strategyExecutions")) {
                return convertStrategyConfig(obj);
            }
            
            
            
            // Default case: convert to Map
            Map<String, Object> map = new HashMap<>();
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, convertFromJSON(obj.get(key), key));
            }
            return map;
        } else {
            return jsonValue;
        }
    }

    /**
     * Check if JSON object has timeframe keys (for indicatorConfig)
     */
    private boolean hasTimeframeKeys(JSONObject obj) {
        String[] timeframes = {"1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min"};
        for (String tf : timeframes) {
            if (obj.has(tf)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if JSON object has Z-Score column keys
     */
    private boolean hasZScoreColumnKeys(JSONObject obj) {
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.startsWith("ZScore_")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if this is a column visibility array structure
     */
    private boolean isColumnVisibilityArray(Object obj) {
        if (obj instanceof JSONArray) {
            JSONArray array = (JSONArray) obj;
            if (array.length() > 0) {
                Object first = array.get(0);
                if (first instanceof JSONObject) {
                    JSONObject firstObj = (JSONObject) first;
                    return firstObj.has("name") && firstObj.has("visible");
                }
            }
        }
        return false;
    }

    /**
     * Convert indicatorConfig JSON to Map structure
     */
    private Map<String, Object> convertIndicatorConfig(JSONObject indicatorConfig) {
        Map<String, Object> configMap = new HashMap<>();
        Iterator<String> keys = indicatorConfig.keys();
        while (keys.hasNext()) {
            String timeframe = keys.next();
            JSONObject tfConfig = indicatorConfig.getJSONObject(timeframe);
            
            Map<String, Object> tfConfigMap = new HashMap<>();
            tfConfigMap.put("enabled", tfConfig.getBoolean("enabled"));
            
            // Convert indicators array
            JSONArray indicatorsArray = tfConfig.getJSONArray("indicators");
            List<String> indicatorsList = new ArrayList<>();
            for (int i = 0; i < indicatorsArray.length(); i++) {
                indicatorsList.add(indicatorsArray.getString(i));
            }
            tfConfigMap.put("indicators", indicatorsList);
            
            // Convert indexRange
            JSONObject indexRange = tfConfig.getJSONObject("indexRange");
            Map<String, Object> indexRangeMap = new HashMap<>();
            indexRangeMap.put("startIndex", indexRange.getInt("startIndex"));
            indexRangeMap.put("endIndex", indexRange.getInt("endIndex"));
            tfConfigMap.put("indexRange", indexRangeMap);
            
            //Convert indexCounter
            int indexCounter = tfConfig.getInt("indexCounter");
            tfConfigMap.put("indexCounter", indexCounter);
            
            //Convert resistanceSettings
            JSONObject resistanceSettings = tfConfig.getJSONObject("resistanceSettings");
            Map<String, Object> resistanceSettingsMap = new HashMap<>();
            if(resistanceSettings.has("indexCounter") && resistanceSettings.has("session") && resistanceSettings.has("timeRange")) {
	            resistanceSettingsMap.put("indexCounter", resistanceSettings.getInt("indexCounter"));
	            resistanceSettingsMap.put("session", resistanceSettings.getString("session"));
	            resistanceSettingsMap.put("timeRange", resistanceSettings.getString("timeRange"));
	            tfConfigMap.put("resistanceSettings", resistanceSettings);
            }
            configMap.put(timeframe, tfConfigMap);
        }
        return configMap;
    }

    /**
     * Convert volumeAlertConfig JSON to Map structure
     */
    private Map<String, Object> convertVolumeAlertConfig(JSONObject volumeAlertConfig) {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("enabled", volumeAlertConfig.getBoolean("enabled"));
        configMap.put("volume20MAEnabled", volumeAlertConfig.getBoolean("volume20MAEnabled"));
        configMap.put("onlyBullishSymbols", volumeAlertConfig.getBoolean("onlyBullishSymbols"));
        
        // Convert timeframeConfigs
        JSONObject timeframeConfigs = volumeAlertConfig.getJSONObject("timeframeConfigs");
        Map<String, Object> timeframeConfigsMap = new HashMap<>();
        Iterator<String> tfKeys = timeframeConfigs.keys();
        while (tfKeys.hasNext()) {
            String timeframe = tfKeys.next();
            JSONObject tfConfig = timeframeConfigs.getJSONObject(timeframe);
            Map<String, Object> tfConfigMap = new HashMap<>();
            tfConfigMap.put("enabled", tfConfig.getBoolean("enabled"));
            tfConfigMap.put("percentage", tfConfig.getDouble("percentage"));
            timeframeConfigsMap.put(timeframe, tfConfigMap);
        }
        configMap.put("timeframeConfigs", timeframeConfigsMap);
        
        return configMap;
    }

    /**
     * Convert zScoreAlertConfig JSON to Map structure
     */
    private Map<String, Object> convertZScoreAlertConfig(JSONObject zScoreAlertConfig) {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("enabled", zScoreAlertConfig.getBoolean("enabled"));
        
        // Convert alertConfigs
        JSONObject alertConfigs = zScoreAlertConfig.getJSONObject("alertConfigs");
        Map<String, Object> alertConfigsMap = new HashMap<>();
        Iterator<String> alertKeys = alertConfigs.keys();
        while (alertKeys.hasNext()) {
            String alertName = alertKeys.next();
            JSONObject alertConfig = alertConfigs.getJSONObject(alertName);
            Map<String, Object> alertConfigMap = new HashMap<>();
            alertConfigMap.put("monitorRange", alertConfig.getInt("monitorRange"));
            alertConfigMap.put("alarmOn", alertConfig.getBoolean("alarmOn"));
            alertConfigMap.put("configId", alertConfig.getBoolean("configId"));
            alertConfigMap.put("monteCarloEnabled", alertConfig.getBoolean("monteCarloEnabled"));
            alertConfigMap.put("strategy", alertConfig.getBoolean("strategy"));
            alertConfigMap.put("enabled", alertConfig.getBoolean("enabled"));
            alertConfigMap.put("zScoreColumn", alertConfig.getBoolean("zScoreColumn"));
            alertConfigMap.put("previousTopSymbols", alertConfig.getString("previousTopSymbols"));
            alertConfigsMap.put(alertName, alertConfigMap);
        }
        configMap.put("alertConfigs", alertConfigsMap);
        
        return configMap;
    }

    /**
     * Convert zScoreColumnVisibility JSON to Map structure
     */
    private Map<String, Object> convertZScoreColumnVisibility(JSONObject zScoreColumnVisibility) {
        Map<String, Object> visibilityMap = new HashMap<>();
        Iterator<String> keys = zScoreColumnVisibility.keys();
        while (keys.hasNext()) {
            String columnName = keys.next();
            visibilityMap.put(columnName, zScoreColumnVisibility.getBoolean(columnName));
        }
        return visibilityMap;
    }

    /**
     * Convert columnVisibility array to List structure
     */
    private List<Object> convertColumnVisibility(Object columnVisibilityObj) {
        List<Object> columnVisibilityList = new ArrayList<>();
        
        if (columnVisibilityObj instanceof JSONArray) {
            JSONArray array = (JSONArray) columnVisibilityObj;
            for (int i = 0; i < array.length(); i++) {
                JSONObject columnEntry = array.getJSONObject(i);
                Map<String, Object> columnMap = new HashMap<>();
                columnMap.put("name", columnEntry.getString("name"));
                columnMap.put("visible", columnEntry.getBoolean("visible"));
                columnVisibilityList.add(columnMap);
            }
        }
        
        return columnVisibilityList;
    }

    /**
     * Convert indicatorManagementConfig JSON to Map structure
     */
    private Map<String, Object> convertIndicatorManagementConfig(JSONObject indicatorManagementConfig) {
        Map<String, Object> configMap = new HashMap<>();
        
        // Convert customIndicators
        if (indicatorManagementConfig.has("customIndicators")) {
            JSONArray customIndicatorsArray = indicatorManagementConfig.getJSONArray("customIndicators");
            List<Map<String, Object>> customIndicatorsList = new ArrayList<>();
            
            for (int i = 0; i < customIndicatorsArray.length(); i++) {
                JSONObject indicatorObj = customIndicatorsArray.getJSONObject(i);
                Map<String, Object> indicatorMap = new HashMap<>();
                indicatorMap.put("name", indicatorObj.getString("name"));
                indicatorMap.put("type", indicatorObj.getString("type"));
                indicatorMap.put("displayName", indicatorObj.getString("displayName"));
                
                // Convert parameters
                if (indicatorObj.has("parameters")) {
                    JSONObject parametersObj = indicatorObj.getJSONObject("parameters");
                    Map<String, Object> parametersMap = new HashMap<>();
                    Iterator<String> paramKeys = parametersObj.keys();
                    while (paramKeys.hasNext()) {
                        String paramKey = paramKeys.next();
                        parametersMap.put(paramKey, parametersObj.get(paramKey));
                    }
                    indicatorMap.put("parameters", parametersMap);
                }
                
                customIndicatorsList.add(indicatorMap);
            }
            configMap.put("customIndicators", customIndicatorsList);
        }
        
        // Convert strategyConfigs
        if (indicatorManagementConfig.has("strategyConfigs")) {
            JSONArray strategyConfigsArray = indicatorManagementConfig.getJSONArray("strategyConfigs");
            List<Map<String, Object>> strategyConfigsList = new ArrayList<>();
            
            for (int i = 0; i < strategyConfigsArray.length(); i++) {
                JSONObject strategyObj = strategyConfigsArray.getJSONObject(i);
                Map<String, Object> strategyMap = new HashMap<>();
                strategyMap.put("name", strategyObj.getString("name"));
                strategyMap.put("enabled", strategyObj.getBoolean("enabled"));
                strategyMap.put("description", strategyObj.optString("description", ""));
                
                // Convert timeframes
                if (strategyObj.has("timeframes")) {
                    JSONArray timeframesArray = strategyObj.getJSONArray("timeframes");
                    List<String> timeframesList = new ArrayList<>();
                    for (int j = 0; j < timeframesArray.length(); j++) {
                        timeframesList.add(timeframesArray.getString(j));
                    }
                    strategyMap.put("timeframes", timeframesList);
                }
                
                // Convert indicators
                if (strategyObj.has("indicators")) {
                    JSONArray indicatorsArray = strategyObj.getJSONArray("indicators");
                    List<String> indicatorsList = new ArrayList<>();
                    for (int j = 0; j < indicatorsArray.length(); j++) {
                        indicatorsList.add(indicatorsArray.getString(j));
                    }
                    strategyMap.put("indicators", indicatorsList);
                }
                
                // Convert conditions
                if (strategyObj.has("conditions")) {
                    JSONObject conditionsObj = strategyObj.getJSONObject("conditions");
                    Map<String, Object> conditionsMap = new HashMap<>();
                    Iterator<String> conditionKeys = conditionsObj.keys();
                    while (conditionKeys.hasNext()) {
                        String conditionKey = conditionKeys.next();
                        conditionsMap.put(conditionKey, conditionsObj.get(conditionKey));
                    }
                    strategyMap.put("conditions", conditionsMap);
                }
                
                strategyConfigsList.add(strategyMap);
            }
            configMap.put("strategyConfigs", strategyConfigsList);
        }
        
        return configMap;
    }

    /**
     * Convert strategyConfig JSON to Map structure
     */
    private Map<String, Object> convertStrategyConfig(JSONObject strategyConfig) {
        Map<String, Object> configMap = new HashMap<>();
        
        // Convert enabledStrategies
        if (strategyConfig.has("enabledStrategies")) {
            JSONArray enabledStrategiesArray = strategyConfig.getJSONArray("enabledStrategies");
            List<String> enabledStrategiesList = new ArrayList<>();
            for (int i = 0; i < enabledStrategiesArray.length(); i++) {
                enabledStrategiesList.add(enabledStrategiesArray.getString(i));
            }
            configMap.put("enabledStrategies", enabledStrategiesList);
        }
        
        // Convert strategyExecutions
        if (strategyConfig.has("strategyExecutions")) {
            JSONObject executionsObj = strategyConfig.getJSONObject("strategyExecutions");
            Map<String, Object> executionsMap = new HashMap<>();
            Iterator<String> executionKeys = executionsObj.keys();
            while (executionKeys.hasNext()) {
                String strategyName = executionKeys.next();
                JSONObject executionObj = executionsObj.getJSONObject(strategyName);
                Map<String, Object> executionMap = new HashMap<>();
                
                executionMap.put("lastExecuted", executionObj.getLong("lastExecuted"));
                executionMap.put("executionCount", executionObj.getInt("executionCount"));
                executionMap.put("successCount", executionObj.getInt("successCount"));
                
                executionsMap.put(strategyName, executionMap);
            }
            configMap.put("strategyExecutions", executionsMap);
        }
        
        return configMap;
    }

    /**
     * Check if a key is metadata (should not be loaded as configuration)
     */
    private boolean isMetadataKey(String key) {
        return key.equals("exportDate") || key.equals("version") || key.equals("application") || 
               key.equals("backup") || key.equals("shared");
    }

    /**
     * Save configuration to default location (auto-save)
     */
    public boolean saveConfigurationToDefaultLocation(Map<String, Object> configData) {
        try {
            File configDir = new File(System.getProperty("user.home"), ".stockdashboard");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File configFile = new File(configDir, "dashboard_config_" + timestamp + ".json");

            JSONObject config = new JSONObject();
            config.put("exportDate", new Date().toString());
            config.put("version", "1.0");
            config.put("application", "StockDashboard");

            // Save all configuration data with proper conversion
            for (Map.Entry<String, Object> entry : configData.entrySet()) {
                try {
                    Object jsonValue = convertToJSONCompatible(entry.getValue());
                    config.put(entry.getKey(), jsonValue);
                } catch (Exception e) {
                    log("Warning: Could not save configuration key '" + entry.getKey() + "': " + e.getMessage());
                }
            }

            try (PrintWriter out = new PrintWriter(configFile)) {
                out.println(config.toString(4));
            }

            log("✅ Configuration auto-saved to: " + configFile.getAbsolutePath());
            return true;

        } catch (Exception e) {
            log("❌ Error auto-saving configuration: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load the most recent configuration from default location
     */
    public Map<String, Object> loadConfigurationFromDefaultLocation() {
        try {
            File configDir = new File(System.getProperty("user.home"), ".stockdashboard");
            if (!configDir.exists()) {
                return new HashMap<>();
            }

            // Find the most recent config file
            File[] configFiles = configDir.listFiles((dir, name) -> name.startsWith("dashboard_config_") && name.endsWith(".json"));
            if (configFiles == null || configFiles.length == 0) {
                return new HashMap<>();
            }

            Arrays.sort(configFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
            File latestConfigFile = configFiles[0];

            String content = new String(Files.readAllBytes(latestConfigFile.toPath()));
            JSONObject config = new JSONObject(new JSONTokener(content));

            Map<String, Object> configData = new HashMap<>();
            Iterator<String> keys = config.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!isMetadataKey(key)) {
                    try {
                        Object value = convertFromJSON(config.get(key), key);
                        configData.put(key, value);
                    } catch (Exception e) {
                        log("Warning: Could not load configuration key '" + key + "': " + e.getMessage());
                    }
                }
            }

            log("✅ Configuration auto-loaded from: " + latestConfigFile.getAbsolutePath());
            return configData;

        } catch (Exception e) {
            log("❌ Error auto-loading configuration: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Backup current configuration
     */
    public boolean backupConfiguration(Map<String, Object> configData) {
        try {
            File backupDir = new File(System.getProperty("user.home"), ".stockdashboard/backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File backupFile = new File(backupDir, "backup_" + timestamp + ".json");

            JSONObject config = new JSONObject();
            config.put("exportDate", new Date().toString());
            config.put("version", "1.0");
            config.put("application", "StockDashboard");
            config.put("backup", true);

            for (Map.Entry<String, Object> entry : configData.entrySet()) {
                try {
                    Object jsonValue = convertToJSONCompatible(entry.getValue());
                    config.put(entry.getKey(), jsonValue);
                } catch (Exception e) {
                    log("Warning: Could not backup configuration key '" + entry.getKey() + "': " + e.getMessage());
                }
            }

            try (PrintWriter out = new PrintWriter(backupFile)) {
                out.println(config.toString(4));
            }

            log("✅ Configuration backed up to: " + backupFile.getAbsolutePath());
            return true;

        } catch (Exception e) {
            log("❌ Error creating backup: " + e.getMessage());
            return false;
        }
    }

    /**
     * Export configuration for sharing (removes sensitive data)
     */
    public boolean exportConfigurationForSharing(Map<String, Object> configData, List<String> sensitiveKeys) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Configuration for Sharing");
        fileChooser.setSelectedFile(new File("dashboard_config_share.json"));

        int userSelection = fileChooser.showSaveDialog(parentComponent);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".json")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".json");
            }

            try {
                JSONObject config = new JSONObject();
                config.put("exportDate", new Date().toString());
                config.put("version", "1.0");
                config.put("application", "StockDashboard");
                config.put("shared", true);

                // Filter out sensitive data
                for (Map.Entry<String, Object> entry : configData.entrySet()) {
                    if (!sensitiveKeys.contains(entry.getKey())) {
                        try {
                            Object jsonValue = convertToJSONCompatible(entry.getValue());
                            config.put(entry.getKey(), jsonValue);
                        } catch (Exception e) {
                            log("Warning: Could not export configuration key '" + entry.getKey() + "': " + e.getMessage());
                        }
                    }
                }

                try (PrintWriter out = new PrintWriter(fileToSave)) {
                    out.println(config.toString(4));
                }

                log("✅ Shared configuration exported to: " + fileToSave.getAbsolutePath());
                
                JOptionPane.showMessageDialog(parentComponent, 
                    "Configuration exported for sharing!\n\n" +
                    "File: " + fileToSave.getName() + "\n" +
                    "Sensitive data has been removed.",
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
                
                return true;

            } catch (Exception e) {
                log("❌ Error exporting shared configuration: " + e.getMessage());
                JOptionPane.showMessageDialog(parentComponent, 
                    "Error exporting configuration: " + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return false;
    }
    
    /**
     * Check if this is a globalCustomIndicators array structure
     */
    private boolean isGlobalCustomIndicatorsArray(Object obj) {
        if (obj instanceof JSONArray) {
            JSONArray array = (JSONArray) obj;
            if (array.length() > 0) {
                Object first = array.get(0);
                if (first instanceof JSONObject) {
                    JSONObject firstObj = (JSONObject) first;
                    return firstObj.has("name") && firstObj.has("type") && firstObj.has("displayName");
                }
            }
        }
        return false;
    }
    
    /**
     * Convert globalCustomIndicators JSON array to List structure
     */
    private List<Object> convertGlobalCustomIndicators(Object globalCustomIndicatorsObj) {
        List<Object> customIndicatorsList = new ArrayList<>();
        
        if (globalCustomIndicatorsObj instanceof JSONArray) {
            JSONArray array = (JSONArray) globalCustomIndicatorsObj;
            for (int i = 0; i < array.length(); i++) {
                JSONObject indicatorObj = array.getJSONObject(i);
                Map<String, Object> indicatorMap = new HashMap<>();
                
                // Extract basic properties
                indicatorMap.put("name", indicatorObj.getString("name"));
                indicatorMap.put("type", indicatorObj.getString("type"));
                indicatorMap.put("displayName", indicatorObj.getString("displayName"));
                
                // Extract parameters if they exist
                if (indicatorObj.has("parameters")) {
                    JSONObject parametersObj = indicatorObj.getJSONObject("parameters");
                    Map<String, Object> parametersMap = new HashMap<>();
                    Iterator<String> paramKeys = parametersObj.keys();
                    while (paramKeys.hasNext()) {
                        String paramKey = paramKeys.next();
                        Object paramValue = parametersObj.get(paramKey);
                        // Convert JSON number types to appropriate Java types
                        if (paramValue instanceof Number) {
                            Number num = (Number) paramValue;
                            // For integer parameters, convert to Integer
                            if (paramKey.equals("fast") || paramKey.equals("slow") || paramKey.equals("signal") || 
                                paramKey.equals("period") || paramKey.equals("sma1") || paramKey.equals("sma2") || 
                                paramKey.equals("sma3") || paramKey.equals("lookback")) {
                                parametersMap.put(paramKey, num.intValue());
                            } else if (paramKey.equals("step") || paramKey.equals("max")) {
                                // For double parameters, convert to Double
                                parametersMap.put(paramKey, num.doubleValue());
                            } else {
                                // Keep as Number for other cases
                                parametersMap.put(paramKey, paramValue);
                            }
                        } else {
                            parametersMap.put(paramKey, paramValue);
                        }
                    }
                    indicatorMap.put("parameters", parametersMap);
                }
                
                customIndicatorsList.add(indicatorMap);
            }
        }
        
        return customIndicatorsList;
    }

    /**
     * Convert customIndicatorCombinations JSON to Map structure
     */
    private Map<String, Object> convertCustomIndicatorCombinations(JSONObject customIndicatorCombinationsObj) {
        Map<String, Object> combinationsMap = new HashMap<>();
        Iterator<String> keys = customIndicatorCombinationsObj.keys();
        
        while (keys.hasNext()) {
            String timeframe = keys.next();
            JSONArray indicatorsArray = customIndicatorCombinationsObj.getJSONArray(timeframe);
            List<String> indicatorsList = new ArrayList<>();
            
            for (int i = 0; i < indicatorsArray.length(); i++) {
                indicatorsList.add(indicatorsArray.getString(i));
            }
            
            combinationsMap.put(timeframe, indicatorsList);
        }
        
        return combinationsMap;
    }

    /**
     * Check if this is a customIndicatorCombinations map structure
     */
    private boolean isCustomIndicatorCombinationsMap(Object obj) {
        if (obj instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject) obj;
            Iterator<String> keys = jsonObj.keys();
            if (keys.hasNext()) {
                String firstKey = keys.next();
                Object firstValue = jsonObj.get(firstKey);
                // Should be an array of strings (indicator names)
                return firstValue instanceof JSONArray;
            }
        }
        return false;
    }

    private void log(String message) {
        if (logger != null) {
            logger.accept(message);
        }
    }
}