package com.quantlabs.QuantTester.v4.alert;

public class ApiConfig {
    private String yahooBaseUrl;
    private int yahooMaxQps;
    private String alpacaBaseUrl;
    private String alpacaApiKey;
    private String alpacaSecretKey;

    public ApiConfig() {
        // Default values
        this.yahooBaseUrl = "https://query1.finance.yahoo.com/v8/finance/chart";
        this.yahooMaxQps = 2000;
        this.alpacaBaseUrl = "https://data.alpaca.markets/v2";
        this.alpacaApiKey = "YOUR_ALPACA_API_KEY";
        this.alpacaSecretKey = "YOUR_ALPACA_SECRET_KEY";
    }

    // Getters and setters
    public String getYahooBaseUrl() { return yahooBaseUrl; }
    public void setYahooBaseUrl(String yahooBaseUrl) { this.yahooBaseUrl = yahooBaseUrl; }
    
    public int getYahooMaxQps() { return yahooMaxQps; }
    public void setYahooMaxQps(int yahooMaxQps) { this.yahooMaxQps = yahooMaxQps; }
    
    public String getAlpacaBaseUrl() { return alpacaBaseUrl; }
    public void setAlpacaBaseUrl(String alpacaBaseUrl) { this.alpacaBaseUrl = alpacaBaseUrl; }
    
    public String getAlpacaApiKey() { return alpacaApiKey; }
    public void setAlpacaApiKey(String alpacaApiKey) { this.alpacaApiKey = alpacaApiKey; }
    
    public String getAlpacaSecretKey() { return alpacaSecretKey; }
    public void setAlpacaSecretKey(String alpacaSecretKey) { this.alpacaSecretKey = alpacaSecretKey; }
}