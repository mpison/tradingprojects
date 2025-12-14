package com.quantlabs.stockApp.service.tradingview;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.quantlabs.stockApp.exception.StockApiException;
import com.quantlabs.stockApp.http.ApiHttpClient;
import com.quantlabs.stockApp.http.OkHttpClientImpl;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.service.StockDataProvider;

public class TradingViewService extends StockDataProvider {
    private final TradingViewRequestBuilder requestBuilder;
    private final TradingViewResponseParser responseParser;
    private final ApiHttpClient httpClient;

    public TradingViewService(ApiHttpClient httpClient) {
        this.httpClient = httpClient;
        this.requestBuilder = new TradingViewRequestBuilder();
        this.responseParser = new TradingViewResponseParser();
    }
    
    public TradingViewService() {
        this.httpClient = new OkHttpClientImpl();
        this.requestBuilder = new TradingViewRequestBuilder();
        this.responseParser = new TradingViewResponseParser();
    }

    @Override
    public List<PriceData> getDowJones() throws StockApiException {
        return executeTradingViewRequest(requestBuilder.buildDowJonesRequest());
    }

    @Override
    public List<PriceData> getNasdaq() throws StockApiException {
        return executeTradingViewRequest(requestBuilder.buildNasdaqRequest());
    }

    @Override
    public List<PriceData> getSP500() throws StockApiException {
        return executeTradingViewRequest(requestBuilder.buildSP500Request());
    }

    @Override
    public List<PriceData> getRussell2000() throws StockApiException {
        return executeTradingViewRequest(requestBuilder.buildRussell2000Request());
    }

    @Override
    public List<PriceData> getPennyStocksByVolume() throws StockApiException {
        return executeTradingViewRequest(requestBuilder.buildPennyStocksByVolumeRequest());
    }

    @Override
    public List<PriceData> getPennyStocksByPercentChange() throws StockApiException {
        return executeTradingViewRequest(requestBuilder.buildPennyStocksByPercentChangeRequest());
    }

    public List<PriceData> getPennyStocksPreMarket() throws StockApiException {
        return executeTradingViewRequest(requestBuilder.buildPennyStocksPreMarketRequest());
    }

    private List<PriceData> executeTradingViewRequest(String request) throws StockApiException {
        try {
            String response = httpClient.executeTradingViewRequest(
                request,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
                "https://www.tradingview.com"
            );
            //System.out.println("response="+response);
            return responseParser.parse(response);
        } catch (Exception e) {
            throw new StockApiException("TradingView API request failed: " + e.getMessage());
        }
    }
    
    // In TradingViewService.java
 	public String getPreMarketRequestPayload() {
 	    return ((TradingViewRequestBuilder)requestBuilder).buildPennyStocksPreMarketRequest();
 	}
 	
 	public List<PriceData> getPennyStockStandardMarketStocksByVolume() throws StockApiException {
 	    return executeTradingViewRequest(requestBuilder.buildPennyStockStandardMarketStocksByVolumeRequest());
 	}

 	// Add this helper method for debugging
 	public String getPennyStockStandardMarketRequestPayload() {
 	    return ((TradingViewRequestBuilder)requestBuilder).buildPennyStockStandardMarketStocksByVolumeRequest();
 	}
 	
 	public List<PriceData> getPennyStocksPostMarketByPostMarketVolume() throws StockApiException {
 	    return executeTradingViewRequest(requestBuilder.buildPennyStocksPostMarketByVolumeRequest());
 	}

 	public String getPennyStocksPostMarketRequestPayload() {
 	    return requestBuilder.buildPennyStocksPostMarketByVolumeRequest();
 	}
 	
 	public List<PriceData> getIndexStocksMarketByPreMarketVolume() throws StockApiException {
 	    return executeTradingViewRequest(requestBuilder.buildIndexStocksMarketByPreMarketVolumeRequest());
 	}

 	public String getIndexStocksMarketRequestPayload() {
 	    return requestBuilder.buildIndexStocksMarketByPreMarketVolumeRequest();
 	}
 	
 	public List<PriceData> getIndexStocksMarketByStandardMarketVolume() throws StockApiException {
 	    return executeTradingViewRequest(requestBuilder.buildIndexStocksMarketByStandardVolumeRequest());
 	}

 	public String getIndexStocksStandardMarketRequestPayload() {
 	    return requestBuilder.buildIndexStocksMarketByStandardVolumeRequest();
 	}
 	
 	public List<PriceData> getIndexStocksMarketByPostMarketVolume() throws StockApiException {
 	    return executeTradingViewRequest(requestBuilder.buildIndexStocksMarketByPostMarketVolumeRequest());
 	}

 	public String getIndexStocksPostMarketRequestPayload() {
 	    return requestBuilder.buildIndexStocksMarketByPostMarketVolumeRequest();
 	}
 	
 	public List<PriceData> getStocksMarketBySymbol(String symbol) throws StockApiException {
 	    if (symbol == null || symbol.trim().isEmpty()) {
 	        throw new IllegalArgumentException("Symbol cannot be null or empty");
 	    }
 	    
 	   //
 	    if(symbol.equals("^GSPC")) {
 	    	symbol = "SPX";
 	    }
 	    
 	   symbol = symbol.replaceFirst("^\\^", "");
 	   
 	    return executeTradingViewRequest(requestBuilder.buildStocksMarketBySymbolRequest(symbol));
 	}

 	public String getStocksMarketBySymbolRequestPayload(String symbol) {
 	    return requestBuilder.buildStocksMarketBySymbolRequest(symbol);
 	}
 	
 	public PriceData getStockBySymbol(String symbol) throws StockApiException {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        
        if(symbol.equals("^GSPC")) {
 	    	symbol = "SPX";
 	    }
 	    
 	   symbol = symbol.replaceFirst("^\\^", "");
        
        //System.out.println("Searching for symbol: " + symbol);
        
        // First search for the symbol to get exchange information
        List<String> symbolVariants = getSymbolVariantsWithExchange(symbol);
        
        //System.out.println("Found " + symbolVariants.size() + " symbol variants: " + symbolVariants);
        
        // Try each symbol variant until one works
        for (int i = 0; i < symbolVariants.size(); i++) {
            String symbolVariant = symbolVariants.get(i);
            //System.out.println("Trying symbol variant " + (i + 1) + "/" + symbolVariants.size() + ": " + symbolVariant);
            
            try {
                PriceData result = executeSymbolRequest(requestBuilder.buildSymbolRequest(
                    symbolVariant, 
                    TradingViewRequestBuilder.PRE_MARKET_COLUMNS
                ));
                
                //System.out.println("Success with symbol variant: " + symbolVariant);
                return result;
                
            } catch (StockApiException e) {
                // Check if it's the "symbol_not_exists" error
                if (e.getMessage().contains("symbol_not_exists") || 
                    e.getMessage().contains("empty response")) {
                    System.out.println("Symbol variant failed: " + symbolVariant + " - " + e.getMessage());
                    // Try the next symbol variant
                    continue;
                }
                // If it's a different error, rethrow it
                throw e;
            }
        }
        
        throw new StockApiException("No valid symbol found for: " + symbol + 
                                   " after trying " + symbolVariants.size() + " variants");
    }

    private List<String> getSymbolVariantsWithExchange(String symbol) throws StockApiException {
        try {
            String searchUrl = "https://symbol-search.tradingview.com/symbol_search/v3/?text=" + 
                               symbol + "&exchange=&lang=en&domain=production&sort_by_country=US&promo=true";
            
            String response = httpClient.executeTradingViewSearchSymbolRequest(
                searchUrl,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
                "https://www.tradingview.com"
            );
            
            // Parse the response to extract exchange
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray symbols = jsonResponse.getJSONArray("symbols");
            
            if (symbols.length() == 0) {
                throw new StockApiException("No symbols found for: " + symbol);
            }
            
            List<String> symbolVariants = new ArrayList<>();
            
            // Create variants for all found symbols
            for (int i = 0; i < symbols.length(); i++) {
                try {
                    JSONObject symbolData = symbols.getJSONObject(i);
                    String symbolName = symbolData.getString("symbol");
                    String exchange = symbolData.getString("exchange");
                    
                    // Format as "EXCHANGE:SYMBOL"
                    String symbolVariant = exchange + ":" + symbolName;
                    symbolVariants.add(symbolVariant);
                    
                } catch (Exception e) {
                    // Skip malformed symbol entries
                    System.err.println("Skipping malformed symbol at index " + i + ": " + e.getMessage());
                }
            }
            
            if (symbolVariants.isEmpty()) {
                throw new StockApiException("No valid symbol variants found for: " + symbol);
            }
            
            return symbolVariants;
            
        } catch (Exception e) {
            throw new StockApiException("Symbol search failed for: " + symbol + " - " + e.getMessage());
        }
    }

    private PriceData executeSymbolRequest(String request) throws StockApiException {
        try {
            String response = httpClient.executeTradingViewSymbolRequest(
                request,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
                "https://www.tradingview.com"
            );
            
            // Check if the response contains the "symbol_not_exists" error
            if (response != null && (response.contains("symbol_not_exists") || 
                                     response.contains("empty response"))) {
                throw new StockApiException("Symbol not exists error: " + response);
            }
            
            return responseParser.parseSingleSymbol(response);
        } catch (Exception e) {
            // Check if it's already our specific error type
            if (e instanceof StockApiException) {
                throw (StockApiException) e;
            }
            throw new StockApiException("TradingView symbol API request failed: " + e.getMessage());
        }
    }

 	// Helper method for debugging
 	public String getStockBySymbolRequestPayload(String symbol) {
 	    return requestBuilder.buildSymbolRequest(symbol, TradingViewRequestBuilder.PRE_MARKET_COLUMNS);
 	}
}