package com.quantlabs.stockApp.service.tradingview;

import com.quantlabs.stockApp.http.ApiHttpClient;
import com.quantlabs.stockApp.http.OkHttpClientImpl;
import com.quantlabs.stockApp.exception.StockApiException;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.service.StockDataProvider;

import java.util.List;

public class TradingViewService2 extends StockDataProvider {
    private final TradingViewRequestBuilder requestBuilder;
    private final TradingViewResponseParser responseParser;
    private final ApiHttpClient httpClient;

    public TradingViewService2(ApiHttpClient httpClient) {
        this.httpClient = httpClient;
        this.requestBuilder = new TradingViewRequestBuilder();
        this.responseParser = new TradingViewResponseParser();
    }
    
    public TradingViewService2() {
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
 	    return executeTradingViewRequest(requestBuilder.buildStocksMarketBySymbolRequest(symbol));
 	}

 	public String getStocksMarketBySymbolRequestPayload(String symbol) {
 	    return requestBuilder.buildStocksMarketBySymbolRequest(symbol);
 	}
 	
 	public PriceData getStockBySymbol(String symbol) throws StockApiException {
 	    if (symbol == null || symbol.trim().isEmpty()) {
 	        throw new IllegalArgumentException("Symbol cannot be null or empty");
 	    }
 	    
 	    return executeSymbolRequest(requestBuilder.buildSymbolRequest(
 	        symbol, 
 	        TradingViewRequestBuilder.PRE_MARKET_COLUMNS
 	    ));
 	}

 	private PriceData executeSymbolRequest(String request) throws StockApiException {
 	    try {
 	        String response = httpClient.executeTradingViewSymbolRequest(
 	            request,
 	            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
 	            "https://www.tradingview.com"
 	        );
 	        return responseParser.parseSingleSymbol(response);
 	    } catch (Exception e) {
 	        throw new StockApiException("TradingView symbol API request failed: " + e.getMessage());
 	    }
 	}

 	// Helper method for debugging
 	public String getStockBySymbolRequestPayload(String symbol) {
 	    return requestBuilder.buildSymbolRequest(symbol, TradingViewRequestBuilder.PRE_MARKET_COLUMNS);
 	}
}