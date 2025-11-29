package com.quantlabs.stockApp.http;

import com.quantlabs.stockApp.exception.StockApiException;

public interface ApiHttpClient {
	// For Yahoo Finance requests
    String executeYahooRequest(String payload, String crumb, String userAgent, String origin) 
        throws StockApiException;
    
    // For TradingView requests
    String executeTradingViewRequest(String payload, String userAgent, String origin) 
        throws StockApiException;
    
    // For getting Yahoo crumb
    String getYahooCrumb() throws StockApiException;

    String executeTradingViewSymbolRequest(String payload, String userAgent, String origin) 
            throws StockApiException;
    
    String executeTradingViewSearchSymbolRequest(String url, String userAgent, String origin) 
            throws StockApiException;
}