package com.quantlabs.QuantTester.v3.alert.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.json.JSONException;

import com.quantlabs.QuantTester.v3.alert.Message;
import com.quantlabs.QuantTester.v3.alert.config.AppConfig;
import com.quantlabs.QuantTester.v3.alert.data.AlpacaStockDataProvider;
import com.quantlabs.QuantTester.v3.alert.data.StockDataProvider;
import com.quantlabs.QuantTester.v3.alert.data.YahooStockDataProvider;
import com.quantlabs.QuantTester.v3.alert.indicators.IndicatorCalculatorFactory;

import okhttp3.OkHttpClient;

public class StockMonitorController {
    private final MessageRepository messageRepository;
    private StockDataProvider dataProvider;
    private final StockDataService stockDataService;
    private final AlertService alertService;
    private final IndicatorCalculatorFactory indicatorFactory;
    private ScheduledExecutorService scheduler;
    
    private AppConfig appConfig;

    public StockMonitorController() {
        this.messageRepository = new MessageRepository();
        this.indicatorFactory = new IndicatorCalculatorFactory();
        
        // Initialize with Yahoo as default provider
        StockDataProvider dataProvider = new YahooStockDataProvider(new OkHttpClient());
        this.stockDataService = new StockDataService(dataProvider);
        
        this.alertService = new AlertService(messageRepository, stockDataService, indicatorFactory);
    }

    public void startMonitoring() {
        if (!isConfigLoaded()) {
            throw new IllegalStateException("Configuration must be loaded first");
        }
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                alertService.checkForCrossovers();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, getPollingInterval(), TimeUnit.MINUTES);
    }
    
    private long getPollingInterval() {
        // Default polling interval (can be made configurable)
        return 15; // Check every 15 minutes
    }
    
    private long getInterval(String timeframe) {
        // Convert timeframe to polling interval in minutes
        return switch (timeframe) {
            case "1m" -> 1;
            case "5m" -> 5;
            case "15m" -> 15;
            case "30m" -> 30;
            case "1h" -> 60;
            case "4h" -> 240;
            case "1d" -> 1440;
            default -> 15; // Default to 15 minutes
        };
    }

    public void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    public List<Message> getMessages() {
        return messageRepository.getMessages();
    }

    public void deleteMessages(List<String> ids) {
        messageRepository.deleteMessages(ids);
    }

    public void updateMessageStatus(String id, Message.MessageStatus status) {
        messageRepository.updateMessageStatus(id, status);
    }

    public void addManualMessage(String header, String body, Message.MessageStatus status) {
        Message message = new Message(header, body, status, LocalDateTime.now());
        messageRepository.addMessage(message);
    }
    
    public void setDataSource(String source) {
        switch (source.toLowerCase()) {
            case "yahoo":
                this.dataProvider = new YahooStockDataProvider(new OkHttpClient());
                break;
            case "alpaca":
                this.dataProvider = new AlpacaStockDataProvider(
                    new OkHttpClient(), 
                    "your_api_key", 
                    "your_api_secret"
                );
                break;
            default:
                throw new IllegalArgumentException("Unknown data source: " + source);
        }
        this.alertService.setDataProvider(dataProvider);
    }
    
    public List<Message> getRecentAlerts() {
        return alertService.getRecentAlerts();
    }
    
    public void loadConfig(String configContent) throws JSONException {
        this.appConfig = new AppConfig(configContent);
        this.alertService.setConfig(appConfig);
    }
    
    public boolean isConfigLoaded() {
        return appConfig != null;
    }
    
    public void updateMessagesStatus(List<String> ids, Message.MessageStatus status) {
        messageRepository.updateMessagesStatus(ids, status);
    }

    public long getUnreadMessageCount() {
        return messageRepository.countUnreadMessages();
    }
}