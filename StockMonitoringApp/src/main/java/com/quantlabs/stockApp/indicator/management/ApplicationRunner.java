package com.quantlabs.stockApp.indicator.management;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.data.SimpleConsoleLogger;
import com.quantlabs.stockApp.indicator.management.StrategyExecutionService;

import javax.swing.*;

public class ApplicationRunner {
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Failed to set system look and feel: " + e.getMessage());
        }
        
        // Initialize logger using your functional interface
        ConsoleLogger logger = new SimpleConsoleLogger();
        
        // Initialize strategy execution service
        StrategyExecutionService executionService = new StrategyExecutionService(logger);
        
        // Start the Swing application
        SwingUtilities.invokeLater(() -> {
            try {
                IndicatorsManagementApp app = new IndicatorsManagementApp(executionService, logger);
                app.setVisible(true);
                logger.log("Indicators Management Application started successfully");
            } catch (Exception e) {
                System.err.println("Failed to start application: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}