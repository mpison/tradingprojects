package com.quantlabs.stockApp.alert;

import java.util.*;
import java.util.prefs.Preferences;

import org.json.JSONObject;

import com.quantlabs.stockApp.IStockDashboard;
import com.quantlabs.stockApp.StockDashboardv1_22_5;
import com.quantlabs.stockApp.alert.model.ZScoreAlertConfig;

public class ZScoreAlertManager0 {
    private IStockDashboard dashboard;
    private Map<String, ZScoreAlertConfig> alertConfigs;
    private Map<String, List<String>> previousTopSymbols;
    private boolean enabled;

    public ZScoreAlertManager0(IStockDashboard dashboard) {
        this.dashboard = dashboard;
        this.alertConfigs = new HashMap<>();
        this.previousTopSymbols = new HashMap<>();
        loadConfiguration();
    }

    public void loadConfiguration() {
        Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_5.class);
        enabled = prefs.getBoolean("zscoreAlertEnabled", false);
        
        String configJson = prefs.get("zscoreAlertConfigs", "{}");
        try {
            JSONObject configObj = new JSONObject(configJson);
            for (String key : configObj.keySet()) {
                JSONObject config = configObj.getJSONObject(key);
                ZScoreAlertConfig alertConfig = new ZScoreAlertConfig(
                    config.getBoolean("enabled"),
                    config.getInt("monitorRange")
                );
                
                // Load previous top symbols as List
                String previousSymbolsStr = config.optString("previousTopSymbols", "");
                if (!previousSymbolsStr.isEmpty()) {
                    List<String> previousSymbols = Arrays.asList(previousSymbolsStr.split(","));
                    previousTopSymbols.put(key, new ArrayList<>(previousSymbols));
                }
                
                alertConfigs.put(key, alertConfig);
            }
        } catch (Exception e) {
            dashboard.logToConsole("Error loading Z-Score alert configuration: " + e.getMessage());
        }
    }

    public void checkZScoreAlerts() {
        if (!enabled) return;

        List<String> alerts = new ArrayList<>();
        
        for (Map.Entry<String, ZScoreAlertConfig> entry : alertConfigs.entrySet()) {
            String columnName = entry.getKey();
            ZScoreAlertConfig config = entry.getValue();
            
            if (!config.isEnabled()) continue;
            
            List<String> currentTopSymbols = getCurrentTopSymbols(columnName, config.getMonitorRange());
            List<String> previousTopSymbols = getPreviousTopSymbols(columnName);
            
            // Check for changes - now comparing ordered lists
            if (!previousTopSymbols.isEmpty() && !areListsEqual(currentTopSymbols, previousTopSymbols)) {
                String alertMessage = generateAlertMessage(columnName, currentTopSymbols, previousTopSymbols);
                alerts.add(alertMessage);
            }
            
            // Update previous symbols
            updatePreviousTopSymbols(columnName, currentTopSymbols);
        }
        
        // Update Monte Carlo windows if any changes detected
        updateMonteCarloWindows();
        
        // Trigger alerts if any changes detected
        if (!alerts.isEmpty()) {
            triggerAlerts(alerts);            
        }
        
        listZScoreResults();
    }
    
    
    public void listZScoreResults() {
        if (!enabled) {
            dashboard.logToConsole("Z-Score Alert is disabled");
            return;
        }

        List<String> alerts = new ArrayList<>();
        Set<String> allRankedSymbols = new LinkedHashSet<>();
        List<Set<String>> topSymbolsSets = new ArrayList<>(); // For intersection calculation
        
        dashboard.logToConsole("=== Z-SCORE RANKING RESULTS ===");
        
        for (Map.Entry<String, ZScoreAlertConfig> entry : alertConfigs.entrySet()) {
            String columnName = entry.getKey();
            ZScoreAlertConfig config = entry.getValue();
            
            if (!config.isEnabled()) continue;
            
            // Get ALL symbols from this Z-Score column, not just top ones
            List<String> allSymbolsInColumn = getAllSymbolsFromColumn(columnName);
            List<String> currentTopSymbols = getCurrentTopSymbols(columnName, config.getMonitorRange());
            List<String> previousTopSymbols = getPreviousTopSymbols(columnName);
                        
            String alertMessage = generateAlertMessage(columnName, currentTopSymbols, previousTopSymbols);
            alerts.add(alertMessage);
            
            // Log all symbols for this column
            dashboard.logToConsole(columnName + " ALL: " + String.join(", ", allSymbolsInColumn));
            
            // Add top symbols for intersection calculation
            topSymbolsSets.add(new LinkedHashSet<>(currentTopSymbols));
            
            // Add all symbols to the combined set
            allRankedSymbols.addAll(allSymbolsInColumn);
        }
        
        if (allRankedSymbols.isEmpty()) {
            dashboard.logToConsole("No enabled Z-Score columns to display");
        } else {
            // Summary line with all unique symbols from all rankings
            dashboard.logToConsole("ALL_RANKED_SYMBOLS: " + String.join(", ", allRankedSymbols));
            
            // Calculate and log intersections
            logIntersections(topSymbolsSets);
        }
        
        dashboard.logToConsole("=================================");
        
        // log to dashboard
        logToDashBoardConsole(alerts);
    }

    private void logIntersections(List<Set<String>> topSymbolsSets) {
        if (topSymbolsSets.size() < 2) {
            dashboard.logToConsole("INTERSECTION: Need at least 2 enabled Z-Score columns for intersection");
            return;
        }
        
        // Calculate intersection of all sets
        Set<String> intersection = new LinkedHashSet<>(topSymbolsSets.get(0));
        for (int i = 1; i < topSymbolsSets.size(); i++) {
            intersection.retainAll(topSymbolsSets.get(i));
        }
        
        // Log the intersection
        if (intersection.isEmpty()) {
            dashboard.logToConsole("INTERSECTION: No common symbols across all Z-Score rankings");
        } else {
            dashboard.logToConsole("INTERSECTION_ALL: " + String.join(", ", intersection));
        }
        
        // Calculate pairwise intersections for more detailed analysis
        dashboard.logToConsole("--- PAIRWISE INTERSECTIONS ---");
        for (int i = 0; i < topSymbolsSets.size(); i++) {
            for (int j = i + 1; j < topSymbolsSets.size(); j++) {
                Set<String> pairIntersection = new LinkedHashSet<>(topSymbolsSets.get(i));
                pairIntersection.retainAll(topSymbolsSets.get(j));
                
                if (!pairIntersection.isEmpty()) {
                    // Get the column names for this pair
                    String col1 = getColumnNameByIndex(i);
                    String col2 = getColumnNameByIndex(j);
                    dashboard.logToConsole("INTERSECTION " + col1 + " & " + col2 + ": " + String.join(", ", pairIntersection));
                }
            }
        }
    }

    private String getColumnNameByIndex(int index) {
        int currentIndex = 0;
        for (Map.Entry<String, ZScoreAlertConfig> entry : alertConfigs.entrySet()) {
            if (entry.getValue().isEnabled()) {
                if (currentIndex == index) {
                    return entry.getKey();
                }
                currentIndex++;
            }
        }
        return "Column_" + index;
    }

    private List<String> getAllSymbolsFromColumn(String columnName) {
        List<String> allSymbols = new ArrayList<>();
        
        try {
            // Find the column index
            int columnIndex = -1;
            for (int i = 0; i < dashboard.getTableModel().getColumnCount(); i++) {
                if (dashboard.getTableModel().getColumnName(i).equals(columnName)) {
                    columnIndex = i;
                    break;
                }
            }
            
            if (columnIndex == -1) return allSymbols;
            
            // Collect all symbols and their ranks with proper ordering
            List<SymbolRank> symbolRanks = new ArrayList<>();
            for (int row = 0; row < dashboard.getTableModel().getRowCount(); row++) {
                Object rankObj = dashboard.getTableModel().getValueAt(row, columnIndex);
                String symbol = (String) dashboard.getTableModel().getValueAt(row, 1); // Symbol column
                
                if (rankObj instanceof Number && symbol != null) {
                    int rank = ((Number) rankObj).intValue();
                    symbolRanks.add(new SymbolRank(symbol, rank));
                }
            }
            
            // Sort by rank (ascending - lower rank number is better)
            symbolRanks.sort(Comparator.comparingInt(SymbolRank::getRank));
            
            // Get ALL symbols in ranked order
            for (SymbolRank symbolRank : symbolRanks) {
                allSymbols.add(symbolRank.getSymbol());
            }
            
        } catch (Exception e) {
            dashboard.logToConsole("Error getting all symbols from column " + columnName + ": " + e.getMessage());
        }
        
        return allSymbols;
    }   
        
    private List<String> getCurrentTopSymbols(String columnName, int monitorRange) {
        List<String> topSymbols = new ArrayList<>();
        
        try {
            // Find the column index
            int columnIndex = -1;
            for (int i = 0; i < dashboard.getTableModel().getColumnCount(); i++) {
                if (dashboard.getTableModel().getColumnName(i).equals(columnName)) {
                    columnIndex = i;
                    break;
                }
            }
            
            if (columnIndex == -1) return topSymbols;
            
            // Collect symbols and their ranks with proper ordering
            List<SymbolRank> symbolRanks = new ArrayList<>();
            for (int row = 0; row < dashboard.getTableModel().getRowCount(); row++) {
                Object rankObj = dashboard.getTableModel().getValueAt(row, columnIndex);
                String symbol = (String) dashboard.getTableModel().getValueAt(row, 1); // Symbol column
                
                if (rankObj instanceof Number && symbol != null) {
                    int rank = ((Number) rankObj).intValue();
                    symbolRanks.add(new SymbolRank(symbol, rank));
                }
            }
            
            // Sort by rank (ascending - lower rank number is better)
            symbolRanks.sort(Comparator.comparingInt(SymbolRank::getRank));
            
            // Get top N symbols in order
            for (int i = 0; i < Math.min(monitorRange, symbolRanks.size()); i++) {
                topSymbols.add(symbolRanks.get(i).getSymbol());
            }
            
        } catch (Exception e) {
            dashboard.logToConsole("Error getting current top symbols for " + columnName + ": " + e.getMessage());
        }
        
        return topSymbols;
    }

    private List<String> getPreviousTopSymbols(String columnName) {
        return previousTopSymbols.getOrDefault(columnName, new ArrayList<>());
    }

    private void updatePreviousTopSymbols(String columnName, List<String> currentSymbols) {
        previousTopSymbols.put(columnName, new ArrayList<>(currentSymbols));
        
        // Also update the config for persistence
        ZScoreAlertConfig config = alertConfigs.get(columnName);
        if (config != null) {
            config.setPreviousTopSymbols(String.join(",", currentSymbols));
        }
    }
    
    public void updateMonteCarloWindows() {
        if (!enabled) return;

        for (Map.Entry<String, ZScoreAlertConfig> entry : alertConfigs.entrySet()) {
            String columnName = entry.getKey();
            ZScoreAlertConfig config = entry.getValue();
            
            if (!config.isEnabled() || !config.isMonteCarloEnabled()) continue;
            
            List<String> currentTopSymbols = getCurrentTopSymbols(columnName, config.getMonitorRange());
            
            // Notify the dialog to update Monte Carlo windows
            if (dashboard.getZScoreAlertConfigDialog() != null) {
                dashboard.getZScoreAlertConfigDialog().updateMonteCarloWindows(columnName, currentTopSymbols);
            }
        }
    }

    private boolean areListsEqual(List<String> list1, List<String> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        
        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                return false;
            }
        }
        
        return true;
    }

    private String generateAlertMessage(String columnName, List<String> current, List<String> previous) {
        StringBuilder message = new StringBuilder();
        message.append("📊 Z-Score Ranking Alert - ").append(columnName).append(":\n");
        
        // Check for ranking changes
        boolean hasChanges = false;
        
        for (int i = 0; i < Math.min(current.size(), previous.size()); i++) {
            String currentSymbol = current.get(i);
            String previousSymbol = previous.get(i);
            
            if (!currentSymbol.equals(previousSymbol)) {
                if (!hasChanges) {
                    message.append("Ranking Changes:\n");
                    hasChanges = true;
                }
                message.append(String.format("  Position %d: %s → %s\n", i + 1, previousSymbol, currentSymbol));
            }
        }
        
        // Check for new entries if lists are different sizes
        if (current.size() > previous.size()) {
            message.append("New Entries:\n");
            for (int i = previous.size(); i < current.size(); i++) {
                message.append(String.format("  Position %d: %s (NEW)\n", i + 1, current.get(i)));
            }
        }
        
        // Check for dropped entries
        if (previous.size() > current.size()) {
            message.append("Dropped Entries:\n");
            for (int i = current.size(); i < previous.size(); i++) {
                message.append(String.format("  Position %d: %s (DROPPED)\n", i + 1, previous.get(i)));
            }
        }
        
        // Always show current top symbols
        message.append("Current Top ").append(current.size()).append(":\n");
        for (int i = 0; i < current.size(); i++) {
            message.append(String.format("  %d. %s\n", i + 1, current.get(i)));
        }
        
        return message.toString();
    }

    private void triggerAlerts(List<String> alerts) {
        for (String alert : alerts) {
            dashboard.logToConsole(alert);
            
            // Trigger visual/audio alert if dashboard alerts are enabled
            if (dashboard.isAlarmActive()) {
                // Use existing alert system
                dashboard.startBuzzAlert(alert);
            }
        }
    }
    
    private void logToDashBoardConsole(List<String> alerts) {
    	for (String alert : alerts) {
            dashboard.logToConsole(alert);
        }    	
    }

    public void setAlertConfigs(Map<String, ZScoreAlertConfig> alertConfigs) {
        this.alertConfigs.clear();
        this.alertConfigs.putAll(alertConfigs);
        
        // Also update the previousTopSymbols tracking with List
        this.previousTopSymbols.clear();
        for (Map.Entry<String, ZScoreAlertConfig> entry : alertConfigs.entrySet()) {
            String previousSymbols = entry.getValue().getPreviousTopSymbols();
            if (previousSymbols != null && !previousSymbols.isEmpty()) {
                List<String> symbols = Arrays.asList(previousSymbols.split(","));
                this.previousTopSymbols.put(entry.getKey(), new ArrayList<>(symbols));
            }
        }
        
        // Save to preferences for backward compatibility
        saveToPreferences();
    }

    private void saveToPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(StockDashboardv1_22_5.class);
        prefs.putBoolean("zscoreAlertEnabled", enabled);
        
        JSONObject configObj = new JSONObject();
        for (Map.Entry<String, ZScoreAlertConfig> entry : alertConfigs.entrySet()) {
            JSONObject configJson = new JSONObject();
            configJson.put("enabled", entry.getValue().isEnabled());
            configJson.put("monitorRange", entry.getValue().getMonitorRange());
            
            // Save previous symbols as comma-separated list
            List<String> previousSymbols = previousTopSymbols.get(entry.getKey());
            if (previousSymbols != null && !previousSymbols.isEmpty()) {
                configJson.put("previousTopSymbols", String.join(",", previousSymbols));
            } else {
                configJson.put("previousTopSymbols", "");
            }
            
            configObj.put(entry.getKey(), configJson);
        }
        
        prefs.put("zscoreAlertConfigs", configObj.toString());
    }

    // Helper class to store symbol and rank together
    private static class SymbolRank {
        private final String symbol;
        private final int rank;
        
        public SymbolRank(String symbol, int rank) {
            this.symbol = symbol;
            this.rank = rank;
        }
        
        public String getSymbol() { return symbol; }
        public int getRank() { return rank; }
    }

    public Map<String, ZScoreAlertConfig> getAlertConfigs() {
        return new HashMap<>(alertConfigs);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveToPreferences();
    }

    public boolean isEnabled() {
        return enabled;
    }
}