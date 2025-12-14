package com.quantlabs.stockApp.alert;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.SwingUtilities;

import com.quantlabs.stockApp.IStockDashboard;
import com.quantlabs.stockApp.alert.model.ZScoreAlertConfig;
import com.quantlabs.stockApp.alert.ui.ZScoreAlertConfigDialog;
import com.quantlabs.stockApp.indicator.management.StrategyConfig;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.MonteCarloGraphApp;
import com.quantlabs.stockApp.reports.MonteCarloGraphController;
import com.quantlabs.stockApp.reports.MonteCarloGraphUI;
import com.quantlabs.stockApp.utils.StrategyCheckerHelper;

public class ZScoreAlertManager {
    private boolean enabled;
    private Map<String, ZScoreAlertConfig> alertConfigs;
    private Map<String, MonteCarloGraphApp> monteCarloApps;
    private IStockDashboard dashboard;
    private StrategyCheckerHelper strategyCheckerHelper;
    
    // Cache for previous top symbols to detect changes
    private Map<String, List<String>> previousTopSymbols;
    
    Map<String, Object> monteCarloConfig;
	Map<String, PriceData> priceDataMap;

    public ZScoreAlertManager(IStockDashboard dashboard) {
        this.dashboard = dashboard;
        this.alertConfigs = new ConcurrentHashMap<>();
        this.monteCarloApps = new ConcurrentHashMap<>();
        this.previousTopSymbols = new ConcurrentHashMap<>();
        this.enabled = false;
    }

    public ZScoreAlertManager(IStockDashboard dashboard, StrategyCheckerHelper strategyCheckerHelper) {
        this(dashboard);
        this.strategyCheckerHelper = strategyCheckerHelper;
    }

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Map<String, ZScoreAlertConfig> getAlertConfigs() { return alertConfigs; }
    public void setAlertConfigs(Map<String, ZScoreAlertConfig> alertConfigs) { 
        this.alertConfigs.clear();
        if (alertConfigs != null) {
            this.alertConfigs.putAll(alertConfigs);
        }
    }

    public StrategyCheckerHelper getStrategyCheckerHelper() { return strategyCheckerHelper; }
    public void setStrategyCheckerHelper(StrategyCheckerHelper strategyCheckerHelper) { 
        this.strategyCheckerHelper = strategyCheckerHelper; 
    }

    /**
     * Check Z-Score alerts with strategy filtering
     */
    public void checkZScoreAlerts() {
        if (!isEnabled()) {
            return;
        }

        try {
            logToConsole("🔍 Checking Z-Score alerts...");
            
            for (Map.Entry<String, ZScoreAlertConfig> entry : alertConfigs.entrySet()) {
                String configKey = entry.getKey();
                ZScoreAlertConfig config = entry.getValue();
                
                if (!config.isEnabled()) {
                    continue;
                }

                String columnName = config.getZScoreColumn();
                String strategy = config.getStrategy();
                int monitorRange = config.getMonitorRange();
                
                logToConsole("Checking Z-Score alert: " + columnName + 
                            (config.hasStrategy() ? " with strategy: " + strategy : " (no strategy)"));
                
                // Get current top symbols with strategy filtering
                List<String> currentTopSymbols = getCurrentTopSymbolsWithStrategy(columnName, monitorRange, strategy);
                
                if (currentTopSymbols.isEmpty())  {
                    logToConsole("No symbols found for Z-Score alert: " + columnName);
                    continue;
                }
                
                // Check for changes and trigger alerts
                boolean hasTopListChanged = checkForSymbolChanges(configKey, config, currentTopSymbols);
                
                SwingUtilities.invokeLater(() -> {
	                // Update Monte Carlo window if enabled
	                if (config.isMonteCarloEnabled()) {
	                    updateMonteCarloWindow(configKey, config, currentTopSymbols, columnName, strategy, hasTopListChanged);
	                }
                
                });
            }
            
        } catch (Exception e) {
            logToConsole("Error checking Z-Score alerts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get current top symbols with strategy filtering
     */
    public List<String> getCurrentTopSymbolsWithStrategy(String columnName, int monitorRange, String strategy) {
        List<String> symbols = new ArrayList<>();
        
        try {
            // Find the column index
            int columnIndex = findColumnIndex(columnName);
            if (columnIndex == -1) {
                logToConsole("Z-Score column not found: " + columnName);
                return symbols;
            }

            // Get all symbols from the table
            List<String> allSymbols = getAllSymbolsFromTable();
            if (allSymbols.isEmpty()) {
                logToConsole("No symbols found in table for column: " + columnName);
                return symbols;
            }

            // Apply strategy filtering if specified
            List<String> filteredSymbols1 = allSymbols;
            if (strategy != null && !strategy.equals("All Available Symbols") && !strategy.trim().isEmpty() && strategyCheckerHelper != null) {
                filteredSymbols1 = applyStrategyFilter(allSymbols, strategy, columnName);
            }else if(strategy.equals("All Available Symbols" )){
            	filteredSymbols1 = allSymbols;
            }

            // Get ranked symbols from filtered list
            List<SymbolRank> symbolRanks = getRankedSymbols(columnIndex, filteredSymbols1);
            
            // Get top N symbols
            symbols = getTopNSymbols(symbolRanks, monitorRange);
            
            logToConsole("Found " + symbols.size() + "/" + monitorRange + 
                        " top symbols for " + columnName + 
                        (strategy != null && !strategy.isEmpty() ? " with strategy: " + strategy : ""));

        } catch (Exception e) {
            logToConsole("Error getting top symbols with strategy: " + e.getMessage());
            e.printStackTrace();
        }
        
        return symbols;
    }

    /**
     * Apply strategy filter to symbols
     */
    private List<String> applyStrategyFilter(List<String> symbols, String strategyName, String columnName) {
        List<String> filteredSymbols = new ArrayList<>();
        
        try {
            // Get current watchlist context
            String currentWatchlist = getCurrentWatchlist();
            
            // Get selected timeframes for strategy checking
            Set<String> selectedTimeframes = getSelectedTimeframesForStrategy();
            
            logToConsole("Applying strategy filter: " + strategyName + " to " + symbols.size() + " symbols");
            logToConsole("Watchlist context: " + (currentWatchlist != null && !currentWatchlist.isEmpty() ? currentWatchlist : "All"));
            logToConsole("Selected timeframes: " + String.join(", ", selectedTimeframes));
            
            // Get the strategy configuration
            if (strategyCheckerHelper != null) {
                // Get the StrategyConfig object by name
                StrategyConfig strategyConfig = strategyCheckerHelper.getStrategyByName(strategyName);
                
                if (strategyConfig == null) {
                    logToConsole("Error: Strategy not found: " + strategyName);
                    return filteredSymbols; // Return empty symbols if strategy not found
                }
                
                if (!strategyConfig.isEnabled()) {
                    logToConsole("Warning: Strategy '" + strategyName + "' is disabled - skipping filtering");
                    return filteredSymbols; // Return empty symbols if strategy is disabled
                }
                
                // Check each symbol against the strategy
                for (String symbol : symbols) {
                    boolean isBullish = strategyCheckerHelper.isSymbolBullishForStrategy(
                        symbol, strategyConfig, selectedTimeframes, currentWatchlist);
                    
                    if (isBullish) {
                        filteredSymbols.add(symbol);
                    }
                }
                
                logToConsole("Strategy filter result: " + filteredSymbols.size() + 
                            "/" + symbols.size() + " symbols passed strategy: " + strategyName);
            } else {
                logToConsole("Warning: StrategyCheckerHelper not available - skipping strategy filtering");
                return symbols;
            }
            
        } catch (Exception e) {
            logToConsole("Error applying strategy filter: " + e.getMessage());
            e.printStackTrace();
            // If strategy filtering fails, return all symbols
            return symbols;
        }
        
        return filteredSymbols;
    }

    /**
     * Get all symbols from the table
     */
    private List<String> getAllSymbolsFromTable() {
        List<String> symbols = new ArrayList<>();
        
        try {
            int symbolColumnIndex = findSymbolColumnIndex();
            if (symbolColumnIndex == -1) {
                logToConsole("Symbol column not found in table");
                return symbols;
            }
            
            if (dashboard.getTableModel() == null) {
                logToConsole("Table model not available");
                return symbols;
            }
            
            for (int row = 0; row < dashboard.getTableModel().getRowCount(); row++) {
                String symbol = (String) dashboard.getTableModel().getValueAt(row, symbolColumnIndex);
                if (symbol != null && !symbol.trim().isEmpty()) {
                    symbols.add(symbol.trim());
                }
            }
            
            logToConsole("Found " + symbols.size() + " symbols in table");
            
        } catch (Exception e) {
            logToConsole("Error getting symbols from table: " + e.getMessage());
        }
        
        return symbols;
    }

    /**
     * Get ranked symbols for the given column and symbol list
     */
    private List<SymbolRank> getRankedSymbols(int columnIndex, List<String> symbols) {
        List<SymbolRank> symbolRanks = new ArrayList<>();
        
        try {
            if (dashboard.getTableModel() == null) {
                return symbolRanks;
            }
            
            // Create a set for faster lookup
            Set<String> symbolSet = new HashSet<>(symbols);
            int symbolColumnIndex = findSymbolColumnIndex();
            
            if (symbolColumnIndex == -1) {
                logToConsole("Error: Cannot find symbol column for ranking");
                return symbolRanks;
            }
            
            for (int row = 0; row < dashboard.getTableModel().getRowCount(); row++) {
                Object rankObj = dashboard.getTableModel().getValueAt(row, columnIndex);
                String symbol = (String) dashboard.getTableModel().getValueAt(row, symbolColumnIndex);
                
                if (isValidSymbolAndRank(rankObj, symbol) && symbolSet.contains(symbol)) {
                    int rank = ((Number) rankObj).intValue();
                    symbolRanks.add(new SymbolRank(symbol, rank));
                }
            }
            
            // Sort by rank (ascending - lower rank number is better)
            symbolRanks.sort(Comparator.comparingInt(SymbolRank::getRank));
            
            logToConsole("Ranked " + symbolRanks.size() + " symbols for Z-Score analysis");
            
        } catch (Exception e) {
            logToConsole("Error getting ranked symbols: " + e.getMessage());
        }
        
        return symbolRanks;
    }

    /**
     * Get top N symbols from ranked list
     */
    private List<String> getTopNSymbols(List<SymbolRank> symbolRanks, int monitorRange) {
        List<String> topSymbols = new ArrayList<>();
        int count = Math.min(monitorRange, symbolRanks.size());
        
        for (int i = 0; i < count; i++) {
            topSymbols.add(symbolRanks.get(i).getSymbol());
        }
        
        return topSymbols;
    }

    /**
     * Check for symbol changes and trigger alerts
     */
    private boolean checkForSymbolChanges(String configKey, ZScoreAlertConfig config, List<String> currentTopSymbols) {
    	boolean returnVal = false; // nothing changed
        try {
            List<String> previousSymbols = previousTopSymbols.get(configKey);
            
            if (previousSymbols == null) {
                // First time - just store the current symbols
                previousTopSymbols.put(configKey, new ArrayList<>(currentTopSymbols));
                logToConsole("Initial top symbols stored for " + configKey + ": " + currentTopSymbols);
                return returnVal;
            }
            
            // Check for changes
            if (!previousSymbols.equals(currentTopSymbols)) {
                logToConsole("🎯 Z-Score alert triggered for " + configKey);
                logToConsole("   Previous: " + String.join(", ", previousSymbols));
                logToConsole("   Current:  " + String.join(", ", currentTopSymbols));
                
                // Find new symbols that entered the top list
                List<String> newSymbols = findNewSymbols(previousSymbols, currentTopSymbols);
                if (!newSymbols.isEmpty()) {
                    logToConsole("   New symbols: " + String.join(", ", newSymbols));
                    
                    // Trigger alarm if configured
                    if (config.isAlarmOn()) {
                        triggerAlarm(configKey, newSymbols, currentTopSymbols);
                    }
                }
                
                // Update stored symbols
                previousTopSymbols.put(configKey, new ArrayList<>(currentTopSymbols));
                
                // Store in config for persistence
                config.setPreviousTopSymbols(String.join(",", currentTopSymbols));
                returnVal = true;
            }
            
        } catch (Exception e) {
            logToConsole("Error checking symbol changes: " + e.getMessage());
        }
        return returnVal;
    }

    /**
     * Find new symbols that entered the top list
     */
    private List<String> findNewSymbols(List<String> previous, List<String> current) {
        List<String> newSymbols = new ArrayList<>();
        for (String symbol : current) {
            if (!previous.contains(symbol)) {
                newSymbols.add(symbol);
            }
        }
        return newSymbols;
    }

    /**
     * Trigger alarm for Z-Score alert
     */
    private void triggerAlarm(String configKey, List<String> newSymbols, List<String> currentTopSymbols) {
        try {
            String alertMessage = String.format(
                "Z-Score Alert: %s\nNew symbols: %s\nTop symbols: %s",
                configKey,
                String.join(", ", newSymbols),
                String.join(", ", currentTopSymbols)
            );
            
            logToConsole("🔔 ALARM TRIGGERED: " + alertMessage);
            
            // Use dashboard's alarm functionality
            if (dashboard.isAlarmActive()) {
                dashboard.startBuzzAlert(alertMessage);
            }
            
        } catch (Exception e) {
            logToConsole("Error triggering alarm: " + e.getMessage());
        }
    }

    /**
     * Update Monte Carlo window
     * @param strategy 
     * @param columnName 
     * @param hasTopListChanged 
     */
    private void updateMonteCarloWindow(String config, ZScoreAlertConfig zScoreconfig, List<String> currentTopSymbols, String columnName, String strategy, boolean hasTopListChanged) {
        try {
			/*
			 * if (monteCarloApps.containsKey(config)) { MonteCarloGraphApp app =
			 * monteCarloApps.get(config); if (app.isInitialized()) { app.setTopList(new
			 * HashSet<>(currentTopSymbols)); app.updateSymbols(currentTopSymbols);
			 * app.toggleTopList();
			 * 
			 * if(zScoreconfig.isAlarmOn()) { app.frameToFront(); }
			 * 
			 * logToConsole("Updated Monte Carlo window for: " + config); } }
			 */
            // Note: Monte Carlo windows are opened in the config dialog, not here
            
            if (monteCarloApps.containsKey(config)) {
            	
            	
                MonteCarloGraphApp existingApp = monteCarloApps.get(config);
                existingApp.setTopList(new HashSet<>(currentTopSymbols));
                
                String apptitle = ZScoreAlertConfigDialog.getMonteCarloTitle(dashboard, columnName, strategy);
                
                if (existingApp.isInitialized()) {
                    existingApp.updateSymbols(currentTopSymbols);
                    monteCarloApps.put(config, existingApp);
                    
                    logToConsole("Updated Monte Carlo window for: " + config);
                } else {
                    existingApp.initialize(currentTopSymbols, apptitle, monteCarloConfig, priceDataMap);
                }
                
                if(existingApp.checkIfClosed()) {
                	existingApp.showGraph();
                }
                
                if(!existingApp.getTitle().equals(apptitle)){
                	existingApp.updateTitle(apptitle);
                }
                
                existingApp.setPriceDataMap(priceDataMap);
                existingApp.setMonteCarloConfig(monteCarloConfig);
                
                waitForInitializationAndAttachListener(existingApp, config);
                
                existingApp.toggleTopList();
                
                if(zScoreconfig.isAlarmOn() && hasTopListChanged) {
                	existingApp.frameToFront();
                }
                
                if(!dashboard.getCurrentTimeRadio().isSelected()) {            		
            		ZonedDateTime start = dashboard.getStartDateTime();
					ZonedDateTime end = dashboard.getEndDateTime();
					
					existingApp.updateCustomTimeRange(start, end, "1Min");
            	}  
                
            } else if(!currentTopSymbols.isEmpty() || currentTopSymbols.size() > 0){
                try {
                	
                	String primarySymbol = strategyCheckerHelper.getIndicatorsManagementApp().getGlobalWatchlists().get(dashboard.getCurrentWatchlistName()).getPrimarySymbol();
                	
                	Set<String> primarySymbolSet = new HashSet<String>();
                	
                	primarySymbolSet.add(primarySymbol);
                	
                	MonteCarloGraphApp newApp = null;
                	
                	if(dashboard.getCurrentTimeRadio().isSelected()) {
                		newApp = new MonteCarloGraphApp(currentTopSymbols, ZScoreAlertConfigDialog.getMonteCarloTitle(dashboard, columnName, strategy));	
                	}else {
                		ZonedDateTime start = dashboard.getStartDateTime();
						ZonedDateTime end = dashboard.getEndDateTime();
						newApp = new MonteCarloGraphApp(currentTopSymbols, ZScoreAlertConfigDialog.getMonteCarloTitle(dashboard, columnName, strategy), start, end, "1Min");
                	}   
                	
                	
                    //MonteCarloGraphApp newApp = new MonteCarloGraphApp(currentTopSymbols, ZScoreAlertConfigDialog.getMonteCarloTitle(dashboard, columnName, strategy));
                    
                	newApp.setPriceDataMap(priceDataMap);
                	newApp.setMonteCarloConfig(monteCarloConfig);
                    
                    newApp.setPrimarySymbols(primarySymbolSet);
                    newApp.setTopList(new HashSet<>(currentTopSymbols));
                    monteCarloApps.put(config, newApp);
                    //newApp.toggleTopList();
					/*
					 * if(zScoreconfig.isAlarmOn()) { newApp.frameToFront(); }
					 */
                    // Wait for the app to initialize and then attach window listener
                    waitForInitializationAndAttachListener(newApp, config);
                    
                    
                   
                    logToConsole("Opened Monte Carlo window for: " + config);
                } catch (Exception e) {
                    logToConsole("Error opening Monte Carlo window for " + config + ": " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            logToConsole("Error updating Monte Carlo window: " + e.getMessage());
        }
    }
    
    /**
     * Wait for Monte Carlo app to initialize and then attach window listener
     */
    private void waitForInitializationAndAttachListener(MonteCarloGraphApp monteCarloApp, String configKey) {
        javax.swing.Timer timer = new javax.swing.Timer(3000, e -> {
            if (monteCarloApp.isInitialized() && monteCarloApp.getController() != null) {
                // App is initialized, now attach the window listener
                attachWindowListenerToInitializedApp(monteCarloApp, configKey);
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });
        timer.start();
    }

    /**
     * Attach window listener to an initialized Monte Carlo app
     */
    private void attachWindowListenerToInitializedApp(MonteCarloGraphApp monteCarloApp, String configKey) {
        try {
            MonteCarloGraphController controller = monteCarloApp.getController();
            if (controller != null && controller.getGraphUI() != null) {
                MonteCarloGraphUI graphUI = controller.getGraphUI();
                
                if (graphUI.getFrame() != null) {
                    graphUI.getFrame().addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override
                        public void windowClosing(java.awt.event.WindowEvent e) {
                            handleMonteCarloWindowClosing(configKey);
                        }
                        
                        @Override
                        public void windowClosed(java.awt.event.WindowEvent e) {
                            handleMonteCarloWindowClosed(configKey);
                        }
                    });
                    
                    logToConsole("Successfully attached window listener to Monte Carlo frame for: " + configKey);
                } else {
                    logToConsole("Frame not available yet for: " + configKey);
                }
            } else {
                logToConsole("Graph UI not available for: " + configKey);
            }
        } catch (Exception e) {
            logToConsole("Error attaching window listener to initialized app: " + e.getMessage());
        }
    }
    
    /**
     * Handle Monte Carlo window closing event
     */
    private void handleMonteCarloWindowClosing(String configKey) {
        logToConsole("Monte Carlo window closing: " + configKey);
        // Don't remove from map yet - wait for windowClosed
    }

    /**
     * Handle Monte Carlo window closed event
     */
    private void handleMonteCarloWindowClosed(String configKey) {
        logToConsole("Monte Carlo window closed: " + configKey);
        
        Map<String, ZScoreAlertConfig> monteCarloApps = getAlertConfigs();
        
        // Remove from the apps map
        if (monteCarloApps.containsKey(configKey)) {
            monteCarloApps.remove(configKey);
            logToConsole("Removed Monte Carlo app from tracking: " + configKey);
        }
        
        setAlertConfigs(monteCarloApps);
        
        // Update the configuration to reflect that Monte Carlo is no longer enabled
        updateConfigMonteCarloState(configKey, false);
    }

    /**
     * Update configuration Monte Carlo state when window is closed
     */
    private void updateConfigMonteCarloState(String configKey, boolean enabled) {
        try {
            // Find the row in the table that corresponds to this configKey
            /*for (int i = 0; i < configTableModel.getRowCount(); i++) {
                String currentConfigId = (String) configTableModel.getValueAt(i, 1);
                String currentColName = (String) configTableModel.getValueAt(i, 2);
                String currentStrategy = (String) configTableModel.getValueAt(i, 3);
                
                String currentKey = currentColName + (currentStrategy != null && !currentStrategy.isEmpty() ? "|" + currentStrategy : "");
                
                if (currentKey.equals(configKey)) {
                    // Update the Monte Carlo Enabled checkbox in the table
                    configTableModel.setValueAt(enabled, i, 5); // Column 5 is MonteCarlo Enabled
                    logToConsole("Updated table: Monte Carlo disabled for " + configKey);
                    break;
                }
            }*/
            
            // Also update the alertConfigs map
            if (alertConfigs.containsKey(configKey)) {
                ZScoreAlertConfig config = alertConfigs.get(configKey);
                config.setMonteCarloEnabled(enabled);
                logToConsole("Updated config: Monte Carlo disabled for " + configKey);
            }
            
        } catch (Exception e) {
            logToConsole("Error updating config Monte Carlo state: " + e.getMessage());
        }
    }

    // Helper methods

    /**
     * Get current watchlist from dashboard
     */
    private String getCurrentWatchlist() {
        // TODO: Implement this based on how your dashboard tracks the current watchlist
        // This could be from a combo box selection, current view, etc.
        // Return null or empty string if no specific watchlist is selected
        try {
            // Example implementation - adjust based on your dashboard
            if (dashboard != null) {
                // You might have a method like:
                return dashboard.getCurrentWatchlistName();
                // Or get it from the table model or current view
            }
        } catch (Exception e) {
            logToConsole("Error getting current watchlist: " + e.getMessage());
        }
        return ""; // Default to empty (all symbols)
    }

    /**
     * Get selected timeframes for strategy checking
     */
    private Set<String> getSelectedTimeframesForStrategy() {
        // TODO: Implement this based on how your application selects timeframes
        Set<String> timeframes = new HashSet<>();
        
        try {
            // Try to get from StrategyCheckerHelper first
        	if(dashboard != null) {
        		return dashboard.getSelectedBullishTimeframes(); 
        	}
            /*if (strategyCheckerHelper != null) {
                timeframes = strategyCheckerHelper.getSelectedTimeframesFromBullishCheckboxes();
            }*/
            
            // If no specific timeframes selected, use common defaults
            if (timeframes.isEmpty()) {
                timeframes = Set.of("1D", "4H", "1H"); // Default timeframes
                logToConsole("Using default timeframes for strategy checking: " + String.join(", ", timeframes));
            }
            
        } catch (Exception e) {
            logToConsole("Error getting selected timeframes: " + e.getMessage());
            // Fallback to default timeframes
            timeframes = Set.of("1D", "4H", "1H");
        }
        
        return timeframes;
    }

    /**
     * Find column index in table
     */
    private int findColumnIndex(String columnName) {
        if (dashboard.getTableModel() == null) {
            return -1;
        }
        
        for (int i = 0; i < dashboard.getTableModel().getColumnCount(); i++) {
            if (columnName.equals(dashboard.getTableModel().getColumnName(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find symbol column index
     */
    private int findSymbolColumnIndex() {
        if (dashboard.getTableModel() == null) {
            return -1;
        }
        
        // Try common column names for symbols
        String[] possibleSymbolColumns = {"Symbol", "Ticker", "Name", "Asset"};
        for (int i = 0; i < dashboard.getTableModel().getColumnCount(); i++) {
            String colName = dashboard.getTableModel().getColumnName(i);
            for (String possibleName : possibleSymbolColumns) {
                if (colName.equalsIgnoreCase(possibleName)) {
                    return i;
                }
            }
        }
        
        // If no specific symbol column found, try column 1 (common default)
        if (dashboard.getTableModel().getColumnCount() > 1) {
            return 1;
        }
        
        return -1;
    }

    /**
     * Validate symbol and rank
     */
    private boolean isValidSymbolAndRank(Object rankObj, String symbol) {
        return rankObj instanceof Number && 
               symbol != null && 
               !symbol.trim().isEmpty() &&
               ((Number) rankObj).intValue() > 0; // Rank should be positive
    }

    /**
     * Log to console through dashboard
     */
    private void logToConsole(String message) {
        if (dashboard != null) {
            dashboard.logToConsole("[ZScoreAlert] " + message);
        } else {
            System.out.println("[ZScoreAlert] " + message);
        }
    }

    // Monte Carlo management methods
    public void addMonteCarloApp(String configKey, MonteCarloGraphApp app) {
        monteCarloApps.put(configKey, app);
    }
    
    public void removeMonteCarloApp(String configKey) {
        if (monteCarloApps.containsKey(configKey)) {
            MonteCarloGraphApp app = monteCarloApps.get(configKey);
            app.close();
            monteCarloApps.remove(configKey);
        }
    }
    
    public void closeAllMonteCarloApps() {
        for (MonteCarloGraphApp app : monteCarloApps.values()) {
            app.close();
        }
        monteCarloApps.clear();
    }

    // Configuration management
    public void addAlertConfig(String key, ZScoreAlertConfig config) {
        alertConfigs.put(key, config);
    }
    
    public void removeAlertConfig(String key) {
        alertConfigs.remove(key);
        previousTopSymbols.remove(key);
        removeMonteCarloApp(key);
    }
    
    public void clearAllConfigs() {
        alertConfigs.clear();
        previousTopSymbols.clear();
        closeAllMonteCarloApps();
    }
    
    /**
     * List Z-Score results with strategy filtering
     */
    public void listZScoreResults() {
        try {
            logToConsole("📊 Listing Z-Score Results with Strategy Filtering");
            logToConsole("=".repeat(60));
            
            for (Map.Entry<String, ZScoreAlertConfig> entry : alertConfigs.entrySet()) {
                String configKey = entry.getKey();
                ZScoreAlertConfig config = entry.getValue();
                
                if (!config.isEnabled()) {
                    continue;
                }

                String columnName = config.getZScoreColumn();
                String strategy = config.getStrategy();
                int monitorRange = config.getMonitorRange();
                
                logToConsole("\n🔍 Configuration: " + configKey);
                logToConsole("   Z-Score Column: " + columnName);
                logToConsole("   Strategy: " + (config.hasStrategy() ? strategy : "None"));
                logToConsole("   Monitor Range: " + monitorRange);
                logToConsole("   Monte Carlo: " + (config.isMonteCarloEnabled() ? "Enabled" : "Disabled"));
                logToConsole("   Alarm: " + (config.isAlarmOn() ? "On" : "Off"));
                
                // Get current top symbols with strategy filtering
                List<String> currentTopSymbols = getCurrentTopSymbolsWithStrategy(columnName, monitorRange, strategy);
                
                if (currentTopSymbols.isEmpty()) {
                    logToConsole("   ❌ No symbols found");
                    continue;
                }
                
                // Display the results
                displayZScoreResults(columnName, currentTopSymbols, config);
                
                // Show previous symbols for comparison if available
                showPreviousSymbolsComparison(configKey, currentTopSymbols);
            }
            
            logToConsole("\n" + "=".repeat(60));
            logToConsole("📈 Z-Score Results Listing Complete");
            
        } catch (Exception e) {
            logToConsole("Error listing Z-Score results: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Display Z-Score results in a formatted way
     */
    private void displayZScoreResults(String columnName, List<String> topSymbols, ZScoreAlertConfig config) {
        try {
            logToConsole("   📈 Top " + topSymbols.size() + " Symbols:");
            
            // Find the column index to get actual Z-Score values
            int columnIndex = findColumnIndex(columnName);
            int symbolColumnIndex = findSymbolColumnIndex();
            
            if (columnIndex == -1 || symbolColumnIndex == -1) {
                logToConsole("      Cannot display detailed scores - column not found");
                // Just show symbols without scores
                for (int i = 0; i < topSymbols.size(); i++) {
                    logToConsole("      " + (i + 1) + ". " + topSymbols.get(i));
                }
                return;
            }
            
            // Get detailed information for each top symbol
            for (int i = 0; i < topSymbols.size(); i++) {
                String symbol = topSymbols.get(i);
                String rankInfo = getSymbolRankInfo(symbol, columnIndex, symbolColumnIndex);
                logToConsole("      " + (i + 1) + ". " + symbol + " " + rankInfo);
            }
            
            // Show strategy context if applicable
            if (config.hasStrategy()) {
                logToConsole("   🎯 Strategy Context: " + config.getStrategy());
                logToConsole("      Filtered from all available symbols using strategy criteria");
            }
            
            // Show monitoring status
            List<String> previousSymbols = previousTopSymbols.get(config.getUniqueKey());
            if (previousSymbols != null && !previousSymbols.isEmpty()) {
                List<String> newSymbols = findNewSymbols(previousSymbols, topSymbols);
                if (!newSymbols.isEmpty()) {
                    logToConsole("   🆕 New Symbols: " + String.join(", ", newSymbols));
                }
            }
            
        } catch (Exception e) {
            logToConsole("Error displaying Z-Score results: " + e.getMessage());
        }
    }

    /**
     * Get symbol rank information with Z-Score value
     */
    private String getSymbolRankInfo(String symbol, int rankColumnIndex, int symbolColumnIndex) {
        try {
            if (dashboard.getTableModel() == null) {
                return "(Rank: N/A)";
            }
            
            // Find the row for this symbol
            for (int row = 0; row < dashboard.getTableModel().getRowCount(); row++) {
                String currentSymbol = (String) dashboard.getTableModel().getValueAt(row, symbolColumnIndex);
                if (symbol.equals(currentSymbol)) {
                    Object rankObj = dashboard.getTableModel().getValueAt(row, rankColumnIndex);
                    if (rankObj instanceof Number) {
                        int rank = ((Number) rankObj).intValue();
                        
                        // Try to find the corresponding Z-Score value column
                        String zScoreValue = getZScoreValue(symbol, rankColumnIndex);
                        
                        if (zScoreValue != null) {
                            return String.format("(Rank: %d, Z-Score: %s)", rank, zScoreValue);
                        } else {
                            return String.format("(Rank: %d)", rank);
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logToConsole("Error getting symbol rank info for " + symbol + ": " + e.getMessage());
        }
        
        return "(Rank: N/A)";
    }

    /**
     * Get Z-Score value for a symbol (finds the corresponding Z-Score value column)
     */
    private String getZScoreValue(String symbol, int rankColumnIndex) {
        try {
            if (dashboard.getTableModel() == null) {
                return null;
            }
            
            String rankColumnName = dashboard.getTableModel().getColumnName(rankColumnIndex);
            String valueColumnName = rankColumnName.replace("_Rank", "_ZScore");
            
            // Find the value column index
            int valueColumnIndex = -1;
            for (int i = 0; i < dashboard.getTableModel().getColumnCount(); i++) {
                if (valueColumnName.equals(dashboard.getTableModel().getColumnName(i))) {
                    valueColumnIndex = i;
                    break;
                }
            }
            
            if (valueColumnIndex == -1) {
                return null;
            }
            
            int symbolColumnIndex = findSymbolColumnIndex();
            if (symbolColumnIndex == -1) {
                return null;
            }
            
            // Find the row for this symbol
            for (int row = 0; row < dashboard.getTableModel().getRowCount(); row++) {
                String currentSymbol = (String) dashboard.getTableModel().getValueAt(row, symbolColumnIndex);
                if (symbol.equals(currentSymbol)) {
                    Object valueObj = dashboard.getTableModel().getValueAt(row, valueColumnIndex);
                    if (valueObj instanceof Number) {
                        double zScore = ((Number) valueObj).doubleValue();
                        return String.format("%.3f", zScore);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logToConsole("Error getting Z-Score value for " + symbol + ": " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Show previous symbols comparison
     */
    private void showPreviousSymbolsComparison(String configKey, List<String> currentTopSymbols) {
        try {
            List<String> previousSymbols = previousTopSymbols.get(configKey);
            if (previousSymbols != null && !previousSymbols.isEmpty()) {
                List<String> newSymbols = findNewSymbols(previousSymbols, currentTopSymbols);
                List<String> removedSymbols = findNewSymbols(currentTopSymbols, previousSymbols); // Symbols that were removed
                
                if (!newSymbols.isEmpty() || !removedSymbols.isEmpty()) {
                    logToConsole("   🔄 Changes from previous check:");
                    if (!newSymbols.isEmpty()) {
                        logToConsole("      ➕ New: " + String.join(", ", newSymbols));
                    }
                    if (!removedSymbols.isEmpty()) {
                        logToConsole("      ➖ Removed: " + String.join(", ", removedSymbols));
                    }
                } else {
                    logToConsole("   ✅ No changes from previous check");
                }
            } else {
                logToConsole("   📝 First check - no previous data for comparison");
            }
        } catch (Exception e) {
            logToConsole("Error showing previous symbols comparison: " + e.getMessage());
        }
    }

    /**
     * List detailed Z-Score results for a specific configuration
     */
    public void listZScoreResults(String configKey) {
        try {
            ZScoreAlertConfig config = alertConfigs.get(configKey);
            if (config == null) {
                logToConsole("Configuration not found: " + configKey);
                return;
            }
            
            if (!config.isEnabled()) {
                logToConsole("Configuration is disabled: " + configKey);
                return;
            }
            
            logToConsole("📊 Detailed Z-Score Results for: " + configKey);
            logToConsole("=".repeat(60));
            
            String columnName = config.getZScoreColumn();
            String strategy = config.getStrategy();
            int monitorRange = config.getMonitorRange();
            
            logToConsole("Z-Score Column: " + columnName);
            logToConsole("Strategy: " + (config.hasStrategy() ? strategy : "None"));
            logToConsole("Monitor Range: " + monitorRange);
            
            // Get current top symbols with strategy filtering
            List<String> currentTopSymbols = getCurrentTopSymbolsWithStrategy(columnName, monitorRange, strategy);
            
            if (currentTopSymbols.isEmpty()) {
                logToConsole("❌ No symbols found");
                return;
            }
            
            // Display detailed results
            displayDetailedZScoreResults(columnName, currentTopSymbols, config);
            
        } catch (Exception e) {
            logToConsole("Error listing detailed Z-Score results: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Display detailed Z-Score results with additional information
     */
    private void displayDetailedZScoreResults(String columnName, List<String> topSymbols, ZScoreAlertConfig config) {
        try {
            int columnIndex = findColumnIndex(columnName);
            int symbolColumnIndex = findSymbolColumnIndex();
            
            if (columnIndex == -1 || symbolColumnIndex == -1) {
                logToConsole("Cannot display detailed results - columns not found");
                return;
            }
            
            logToConsole("\n🏆 Top " + topSymbols.size() + " Symbols (Ranked):");
            logToConsole("-".repeat(50));
            
            // Get all symbols with their ranks and Z-Scores for detailed display
            List<SymbolDetail> symbolDetails = getSymbolDetails(columnIndex, symbolColumnIndex, topSymbols);
            
            for (int i = 0; i < symbolDetails.size(); i++) {
                SymbolDetail detail = symbolDetails.get(i);
                logToConsole(String.format("   %2d. %-8s Rank: %2d, Z-Score: %7.3f", 
                    i + 1, detail.symbol, detail.rank, detail.zScore));
            }
            
            // Show strategy information
            if (config.hasStrategy()) {
                logToConsole("\n🎯 Strategy Information:");
                logToConsole("   Name: " + config.getStrategy());
                logToConsole("   Symbols filtered using strategy criteria");
                
                // Show total symbols before and after filtering if available
                List<String> allSymbols = getAllSymbolsFromTable();
                logToConsole("   Filtering: " + topSymbols.size() + "/" + allSymbols.size() + " symbols passed strategy");
            }
            
            // Show monitoring information
            logToConsole("\n📊 Monitoring Information:");
            logToConsole("   Configuration: " + config.getUniqueKey());
            logToConsole("   Monte Carlo: " + (config.isMonteCarloEnabled() ? "Active" : "Inactive"));
            logToConsole("   Alarm: " + (config.isAlarmOn() ? "Armed" : "Disarmed"));
            
            // Show change information
            List<String> previousSymbols = previousTopSymbols.get(config.getUniqueKey());
            if (previousSymbols != null) {
                List<String> newSymbols = findNewSymbols(previousSymbols, topSymbols);
                List<String> removedSymbols = findNewSymbols(topSymbols, previousSymbols);
                
                logToConsole("   Changes: " + newSymbols.size() + " new, " + removedSymbols.size() + " removed");
            }
            
        } catch (Exception e) {
            logToConsole("Error displaying detailed Z-Score results: " + e.getMessage());
        }
    }

    /**
     * Get detailed symbol information including rank and Z-Score
     */
    private List<SymbolDetail> getSymbolDetails(int rankColumnIndex, int symbolColumnIndex, List<String> symbols) {
        List<SymbolDetail> details = new ArrayList<>();
        
        try {
            if (dashboard.getTableModel() == null) {
                return details;
            }
            
            String rankColumnName = dashboard.getTableModel().getColumnName(rankColumnIndex);
            String valueColumnName = rankColumnName.replace("_Rank", "_ZScore");
            int valueColumnIndex = findColumnIndex(valueColumnName);
            
            // Create a set for faster lookup
            Set<String> symbolSet = new HashSet<>(symbols);
            
            for (int row = 0; row < dashboard.getTableModel().getRowCount(); row++) {
                String symbol = (String) dashboard.getTableModel().getValueAt(row, symbolColumnIndex);
                
                if (symbolSet.contains(symbol)) {
                    Object rankObj = dashboard.getTableModel().getValueAt(row, rankColumnIndex);
                    Object valueObj = valueColumnIndex != -1 ? dashboard.getTableModel().getValueAt(row, valueColumnIndex) : null;
                    
                    if (rankObj instanceof Number) {
                        int rank = ((Number) rankObj).intValue();
                        double zScore = 0.0;
                        
                        if (valueObj instanceof Number) {
                            zScore = ((Number) valueObj).doubleValue();
                        }
                        
                        details.add(new SymbolDetail(symbol, rank, zScore));
                    }
                }
            }
            
            // Sort by rank
            details.sort(Comparator.comparingInt(d -> d.rank));
            
        } catch (Exception e) {
            logToConsole("Error getting symbol details: " + e.getMessage());
        }
        
        return details;
    }

    
    
    public Map<String, MonteCarloGraphApp> getMonteCarloApps() {
		return monteCarloApps;
	}

	public void setMonteCarloApps(Map<String, MonteCarloGraphApp> monteCarloApps) {
		this.monteCarloApps = monteCarloApps;
	}

	public IStockDashboard getDashboard() {
		return dashboard;
	}

	public void setDashboard(IStockDashboard dashboard) {
		this.dashboard = dashboard;
	}

	public Map<String, List<String>> getPreviousTopSymbols() {
		return previousTopSymbols;
	}

	public void setPreviousTopSymbols(Map<String, List<String>> previousTopSymbols) {
		this.previousTopSymbols = previousTopSymbols;
	}

	public Map<String, Object> getMonteCarloConfig() {
		return monteCarloConfig;
	}

	public void setMonteCarloConfig(Map<String, Object> monteCarloConfig) {
		this.monteCarloConfig = monteCarloConfig;
	}

	public Map<String, PriceData> getPriceDataMap() {
		return priceDataMap;
	}

	public void setPriceDataMap(Map<String, PriceData> priceDataMap) {
		this.priceDataMap = priceDataMap;
	}



	/**
     * Helper class for symbol details
     */
    private static class SymbolDetail {
        final String symbol;
        final int rank;
        final double zScore;
        
        SymbolDetail(String symbol, int rank, double zScore) {
            this.symbol = symbol;
            this.rank = rank;
            this.zScore = zScore;
        }
    }

    /**
     * List all Z-Score configurations with their status
     */
    public void listAllConfigurations() {
        try {
            logToConsole("📋 All Z-Score Alert Configurations");
            logToConsole("=".repeat(60));
            
            if (alertConfigs.isEmpty()) {
                logToConsole("No configurations found");
                return;
            }
            
            int enabledCount = 0;
            int withStrategyCount = 0;
            int withMonteCarloCount = 0;
            
            for (Map.Entry<String, ZScoreAlertConfig> entry : alertConfigs.entrySet()) {
                String configKey = entry.getKey();
                ZScoreAlertConfig config = entry.getValue();
                
                String statusIcon = config.isEnabled() ? "✅" : "⏸️";
                String strategyIcon = config.hasStrategy() ? "🎯" : "🌐";
                String monteCarloIcon = config.isMonteCarloEnabled() ? "📊" : "   ";
                String alarmIcon = config.isAlarmOn() ? "🔔" : "🔕";
                
                logToConsole(String.format("%s %s %s %s %-40s", 
                    statusIcon, strategyIcon, monteCarloIcon, alarmIcon, configKey));
                
                logToConsole(String.format("     Column: %-20s Strategy: %-20s Range: %d", 
                    config.getZScoreColumn(),
                    config.hasStrategy() ? config.getStrategy() : "None",
                    config.getMonitorRange()));
                
                if (config.isEnabled()) {
                    enabledCount++;
                }
                if (config.hasStrategy()) {
                    withStrategyCount++;
                }
                if (config.isMonteCarloEnabled()) {
                    withMonteCarloCount++;
                }
            }
            
            logToConsole("\n📊 Summary:");
            logToConsole("   Total Configurations: " + alertConfigs.size());
            logToConsole("   Enabled: " + enabledCount);
            logToConsole("   With Strategy: " + withStrategyCount);
            logToConsole("   With Monte Carlo: " + withMonteCarloCount);
            
        } catch (Exception e) {
            logToConsole("Error listing all configurations: " + e.getMessage());
        }
    }

    // Symbol rank helper class
    private static class SymbolRank {
        private final String symbol;
        private final int rank;

        public SymbolRank(String symbol, int rank) {
            this.symbol = symbol;
            this.rank = rank;
        }

        public String getSymbol() {
            return symbol;
        }

        public int getRank() {
            return rank;
        }
    }

	
}