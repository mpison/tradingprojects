package com.quantlabs.QuantTester.v4.alert;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.json.JSONArray;
import org.json.JSONObject;

public class Scheduler {
    private final MessageManager messageManager;
    private final DataFetcher dataFetcher;
    private final IndicatorCalculator calculator;
    private ConfigManager configManager;
    private ScheduledExecutorService executor;
    private String dataSource = "Yahoo";

    public Scheduler(MessageManager messageManager,
                     DataFetcher dataFetcher,
                     IndicatorCalculator calculator,
                     ConfigManager configManager) {
        this.messageManager = messageManager;
        this.dataFetcher = dataFetcher;
        this.calculator = calculator;
        this.configManager = configManager;
    }

    // Setter to update configManager at runtime
    public void setConfigManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public void checkForCrossovers(JLabel statusLabel) {
        try {
            JSONObject cfg = configManager.getConfig();
            String symbol = cfg.getString("symbol");
            String timeframe = cfg.getString("timeframe");
            JSONArray indicators = cfg.getJSONArray("indicators");

            List<DataFetcher.OHLCDataItem> bars = dataFetcher.fetchOHLC(symbol, timeframe, 
                cfg.optString("startDate"), cfg.optString("endDate"), dataSource);

            for (int i = 0; i < indicators.length(); i++) {
                JSONObject indCfg = indicators.getJSONObject(i);
                String type = indCfg.getString("type");
                String result = calculator.calculateIndicator(type, bars, 
                    indCfg.optJSONObject("params"), indCfg.optInt("shift", 0), timeframe);
                
                if (result != null) {
                    messageManager.addMessage(
                        symbol + " " + type.toUpperCase() + " crossover",
                        result,
                        MessageManager.MessageStatus.NEW
                    );
                }
            }
            statusLabel.setText("Status: Last check at " + java.time.LocalDateTime.now());
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    public void startMonitoring(JComboBox<String> intervalCombo, JLabel statusLabel) {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        long delay = parseInterval(intervalCombo.getSelectedItem().toString());
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> checkForCrossovers(statusLabel), 0, delay, TimeUnit.MILLISECONDS);
        statusLabel.setText("Status: Monitoring every " + intervalCombo.getSelectedItem());
    }

    public void stopMonitoring(JLabel statusLabel) {
        if (executor != null) {
            executor.shutdownNow();
            statusLabel.setText("Status: Monitoring stopped");
        }
    }

    private long parseInterval(String text) {
        switch (text) {
        	case "1 minute": return Duration.ofMinutes(1).toMillis();
        	case "5 minutes": return Duration.ofMinutes(5).toMillis();
            case "15 minutes": return Duration.ofMinutes(15).toMillis();
            case "1 hour":     return Duration.ofHours(1).toMillis();
            case "4 hours":    return Duration.ofHours(4).toMillis();
            default:            return Duration.ofMinutes(15).toMillis();
        }
    }
}
