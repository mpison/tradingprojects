// StockBacktesterApp.java
package com.quantlabs.stockApp.backtesting;

import com.quantlabs.stockApp.backtesting.MultiTimeframeBacktestGUI;
import com.quantlabs.stockApp.data.StockDataProviderFactory;
import okhttp3.OkHttpClient;

import javax.swing.*;

public class StockBacktesterApp {
    public static void main(String[] args) {
        // Set system properties for better Swing appearance
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        SwingUtilities.invokeLater(() -> {
            try {
                // Set cross-platform look and feel
                UIManager.setLookAndFeel(UIManager.getLookAndFeel());
                
                // Check for required environment variables
                String alpacaKey = "PK4UOZQDJJZ6WBAU52XM";//System.getenv("ALPACA_API_KEY");
                String alpacaSecret = "Fag4ha2D58VyL0okwXgBHD1IvhoptmI2KiacMaNG";//System.getenv("ALPACA_SECRET_KEY");
                
                if (alpacaKey == null || alpacaSecret == null) {
                    JOptionPane.showMessageDialog(null,
                        "Please set ALPACA_API_KEY and ALPACA_SECRET_KEY environment variables.\n\n" +
                        "You can get these from: https://app.alpaca.markets/",
                        "Configuration Required", JOptionPane.WARNING_MESSAGE);
                }
                
                // Initialize HTTP client with timeout
                OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
                
                // Create provider factory
                StockDataProviderFactory providerFactory = new StockDataProviderFactory(
                    client,
                    alpacaKey,
                    alpacaSecret,
                    "fMCQBX5yvQZL_s6Zi1r9iKBkMkNLzNcw"//System.getenv("POLYGON_API_KEY") // Optional
                );
                
                // Create and show main GUI
                MultiTimeframeBacktestGUI gui = new MultiTimeframeBacktestGUI(providerFactory);
                gui.setVisible(true);
                
                System.out.println("Stock Backtester Application Started Successfully");
                
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                    "Failed to start application: " + e.getMessage(),
                    "Startup Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}