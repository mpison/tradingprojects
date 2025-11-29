package com.quantlabs.QuantTester.v4.alert;

public class StockMonitorApp {
    public static void main(String[] args) {
        ApiConfig apiConfig = new ApiConfig();
        ConfigManager configManager = new ConfigManager();
        MessageManager messageManager = new MessageManager();
        DataFetcher dataFetcher = new DataFetcher(apiConfig);
        IndicatorCalculator calculator = new IndicatorCalculator();
        Scheduler scheduler = new Scheduler(messageManager, dataFetcher, calculator, configManager);
        
        GUI gui = new GUI(messageManager, scheduler, configManager, dataFetcher);
        gui.initialize();
        
        AlertManager alertManager = new AlertManager(messageManager, configManager, gui.getStatusLabel());
        gui.setAlertManager(alertManager);
        
        long alertInterval = configManager.getAlertConfig().optLong("alertIntervalSeconds", 60);
        //alertManager.startAlerting(alertInterval);
    }
}