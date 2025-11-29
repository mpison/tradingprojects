package com.quantlabs.stockApp.service.tradingview;

import com.quantlabs.stockApp.http.ApiHttpClient;
import com.quantlabs.stockApp.http.OkHttpClientImpl;
import com.quantlabs.stockApp.service.tradingview.TradingViewService;
import com.quantlabs.stockApp.exception.StockApiException;
import com.quantlabs.stockApp.model.PriceData;
import java.util.List;

public class MainTest {
    public static void main(String[] args) {
        // Initialize HTTP client
        ApiHttpClient httpClient = new OkHttpClientImpl();
        
        try {
            // Test TradingView Service
            //testTradingViewService(httpClient);
            
            //testPennyStockStandardMarketStocksByVolume(new TradingViewService(httpClient));
            
            //testPennyStocksPostMarketByVolume(new TradingViewService(httpClient));
            
            //testIndexStocksMarketByPreMarketVolume(new TradingViewService(httpClient));
            
        	//testIndexStocksMarketByStandardVolume(new TradingViewService(httpClient));
        	
        	//testIndexStocksMarketByPostMarketVolume(new TradingViewService(httpClient));
        	        	
        	//testStocksMarketBySymbol(new TradingViewService(httpClient), "AAPL");
            
        	//testStocksMarketBySymbol(new TradingViewService(httpClient), "MSFT");
            
            testGetStockBySymbol(new TradingViewService(httpClient), "ES");
        	
        } catch (StockApiException e) {
            System.err.println("Error testing services: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testTradingViewService(ApiHttpClient httpClient) throws StockApiException {
        System.out.println("=== Testing TradingView Service ===");
        TradingViewService tvService = new TradingViewService(httpClient);
        
		/*
		 * // Test Dow Jones testIndexRequest(tvService, "Dow Jones",
		 * tvService::getDowJones);
		 * 
		 * // Test Nasdaq testIndexRequest(tvService, "Nasdaq", tvService::getNasdaq);
		 * 
		 * // Test S&P 500 testIndexRequest(tvService, "S&P 500", tvService::getSP500);
		 * 
		 * // Test Russell 2000 testIndexRequest(tvService, "Russell 2000",
		 * tvService::getRussell2000);
		 * 
		 * // Test Penny Stocks by Volume testPennyStocks(tvService, "by Volume",
		 * tvService::getPennyStocksByVolume);
		 * 
		 * // Test Penny Stocks by % Change testPennyStocks(tvService, "by % Change",
		 * tvService::getPennyStocksByPercentChange);
		 */
        
        // Test Pre-Market Penny Stocks
        testPreMarketPennyStocks(tvService);
    }

	/*
	 * private static void testIndexRequest(TradingViewService service, String
	 * indexName, TradingViewTestFunction testFunction) throws StockApiException {
	 * System.out.println("\nTesting " + indexName + "..."); List<PriceData> result
	 * = testFunction.execute(); printResults(result); }
	 * 
	 * private static void testPennyStocks(TradingViewService service, String type,
	 * TradingViewTestFunction testFunction) throws StockApiException {
	 * System.out.println("\nTesting Penny Stocks " + type + "..."); List<PriceData>
	 * result = testFunction.execute(); printResults(result); }
	 */

    private static void testPreMarketPennyStocks(TradingViewService service) throws StockApiException {
        System.out.println("\nTesting Pre-Market Penny Stocks...");
        
        try {
            // For debugging - print the generated request payload
            if (service instanceof TradingViewService) {
                String requestPayload = ((TradingViewService)service).getPreMarketRequestPayload();
                System.out.println("Request Payload:\n" + requestPayload);
            }
            
            List<PriceData> result = service.getPennyStocksPreMarket();
            printResults(result);
            
            if (!result.isEmpty()) {
                PriceData first = result.get(0);
                System.out.println("\nPre-Market Details:");
                System.out.printf("Ticker: %s\n", first.getTicker());
                System.out.printf("Price: $%.2f\n", first.getLatestPrice());
                System.out.printf("Pre-Market Change: %.2f%%\n", first.getPercentChange());
                System.out.printf("Volume: %,d\n", first.getCurrentVolume());
            }
        } catch (Exception e) {
            System.err.println("Error testing pre-market stocks: " + e.getMessage());
            throw e;
        }
    }
    
    private static void testPennyStockStandardMarketStocksByVolume(TradingViewService service) throws StockApiException {
        System.out.println("\nTesting Standard Market Stocks by Volume...");
        
        try {
            // For debugging - print the generated request payload
            if (service instanceof TradingViewService) {
                String requestPayload = ((TradingViewService)service).getPennyStockStandardMarketRequestPayload();
                System.out.println("Request Payload:\n" + requestPayload);
            }
            
            List<PriceData> result = service.getPennyStockStandardMarketStocksByVolume();
            printResults(result);
            
            if (!result.isEmpty()) {
                PriceData first = result.get(0);
                System.out.println("\nStandard Market Details:");
                System.out.printf("Ticker: %s\n", first.getTicker());
                System.out.printf("Price: $%.2f\n", first.getLatestPrice());
                System.out.printf("Change: %.2f%%\n", first.getPercentChange());
                System.out.printf("Volume: %,d\n", first.getCurrentVolume());
            }
        } catch (Exception e) {
            System.err.println("Error testing standard market stocks: " + e.getMessage());
            throw e;
        }
    }
    
    private static void testPennyStocksPostMarketByVolume(TradingViewService service) throws StockApiException {
        System.out.println("\n=== Testing Penny Stocks - Post Market by Volume ===");
        
        try {
            String payload = service.getPennyStocksPostMarketRequestPayload();
            System.out.println("Post-Market Request Payload:\n" + payload);
            
            List<PriceData> result = service.getPennyStocksPostMarketByPostMarketVolume();
            printResults(result);
            
            if (!result.isEmpty()) {
                PriceData first = result.get(0);
                System.out.println("\nPost-Market Details:");
                System.out.printf("Ticker: %s\n", first.getTicker());
                System.out.printf("Price: $%.2f\n", first.getLatestPrice());
                System.out.printf("Post-Market Change: %.2f%%\n", first.getPostmarketChange());
                System.out.printf("Post-Market Volume: %,d\n", first.getPostmarketVolume());
            }
        } catch (Exception e) {
            System.err.println("Error testing post-market stocks: " + e.getMessage());
            throw e;
        }
    }
    
    private static void testIndexStocksMarketByPreMarketVolume(TradingViewService service) throws StockApiException {
        System.out.println("\n=== Testing Index Stocks - Pre-Market by Volume ===");
        
        try {
            String payload = service.getIndexStocksMarketRequestPayload();
            System.out.println("Index Stocks Request Payload:\n" + payload);
            
            List<PriceData> result = service.getIndexStocksMarketByPreMarketVolume();
            printResults(result);
            
            if (!result.isEmpty()) {
                PriceData first = result.get(0);
                System.out.println("\nIndex Stock Details:");
                System.out.printf("Ticker: %s\n", first.getTicker());
                System.out.printf("Price: $%.2f\n", first.getLatestPrice());
                System.out.printf("Pre-Market Change: %.2f%%\n", first.getPremarketChange());
                System.out.printf("Pre-Market Volume: %,d\n", first.getPremarketVolume());
                System.out.printf("Index: %s\n", first.getIndexes() != null && !first.getIndexes().isEmpty() ? 
                    first.getIndexes().get(0).get("name") : "N/A");
            }
        } catch (Exception e) {
            System.err.println("Error testing index stocks: " + e.getMessage());
            throw e;
        }
    }
    
    private static void testIndexStocksMarketByStandardVolume(TradingViewService service) throws StockApiException {
        System.out.println("\n=== Testing Index Stocks - Standard Market by Volume ===");
        
        try {
            String payload = service.getIndexStocksStandardMarketRequestPayload();
            System.out.println("Index Stocks Request Payload:\n" + payload);
            
            List<PriceData> result = service.getIndexStocksMarketByStandardMarketVolume();
            printResults(result);
            
            if (!result.isEmpty()) {
                PriceData first = result.get(0);
                System.out.println("\nIndex Stock Details:");
                System.out.printf("Ticker: %s\n", first.getTicker());
                System.out.printf("Price: $%.2f\n", first.getLatestPrice());
                System.out.printf("Daily Change: %.2f%%\n", first.getPercentChange());
                System.out.printf("Volume: %,d\n", first.getCurrentVolume());
                
                if (first.getIndexes() != null && !first.getIndexes().isEmpty()) {
                    System.out.println("Belongs to Indexes:");
                    first.getIndexes().forEach(index -> 
                        System.out.printf("  - %s (%s)\n", 
                            index.get("name"), 
                            index.get("proname")));
                }
            }
        } catch (Exception e) {
            System.err.println("Error testing index stocks: " + e.getMessage());
            throw e;
        }
    }
    
    private static void testIndexStocksMarketByPostMarketVolume(TradingViewService service) throws StockApiException {
        System.out.println("\n=== Testing Index Stocks - Post Market by Volume ===");
        
        try {
            String payload = service.getIndexStocksPostMarketRequestPayload();
            System.out.println("Post-Market Request Payload:\n" + payload);
            
            List<PriceData> result = service.getIndexStocksMarketByPostMarketVolume();
            printResults(result);
            
            if (!result.isEmpty()) {
                PriceData first = result.get(0);
                System.out.println("\nPost-Market Index Stock Details:");
                System.out.printf("Ticker: %s\n", first.getTicker());
                System.out.printf("Price: $%.2f\n", first.getLatestPrice());
                System.out.printf("Post-Market Change: %.2f%%\n", first.getPostmarketChange());
                System.out.printf("Post-Market Volume: %,d\n", first.getPostmarketVolume());
                
                if (first.getIndexes() != null && !first.getIndexes().isEmpty()) {
                    System.out.println("Index Membership:");
                    first.getIndexes().forEach(index -> 
                        System.out.printf("  - %s (%s)\n", 
                            index.get("name"), 
                            index.get("proname")));
                }
            }
        } catch (Exception e) {
            System.err.println("Error testing post-market index stocks: " + e.getMessage());
            throw e;
        }
    }
    
    
    private static void testStocksMarketBySymbol(TradingViewService service, String symbol) throws StockApiException {
        System.out.println("\n=== Testing Stocks by Symbol: " + symbol + " ===");
        
        try {
            String payload = service.getStocksMarketBySymbolRequestPayload(symbol);
            System.out.println("Request Payload:\n" + payload);
            
            List<PriceData> result = service.getStocksMarketBySymbol(symbol);
            printResults(result);
            
            if (!result.isEmpty()) {
                PriceData stock = result.get(0);
                System.out.println("\nStock Details:");
                System.out.printf("Ticker: %s\n", stock.getTicker());
                System.out.printf("Name: %s\n", stock.getName());
                System.out.printf("Price: $%.2f\n", stock.getLatestPrice());
                System.out.printf("Change: %.2f%%\n", stock.getPercentChange());
                System.out.printf("Volume: %,d\n", stock.getCurrentVolume());
                System.out.printf("Exchange: %s\n", stock.getExchange());
            } else {
                System.out.println("No results found for symbol: " + symbol);
            }
        } catch (Exception e) {
            System.err.println("Error testing stocks by symbol: " + e.getMessage());
            throw e;
        }
    }
    


    private static void printResults(List<PriceData> data) {
        System.out.printf("Received %d results\n", data.size());
        
        if (data.isEmpty()) {
            System.out.println("No data returned");
            return;
        }
        
        // Print first 3 results
        int displayCount = Math.min(data.size(), 3);
        for (int i = 0; i < displayCount; i++) {
            PriceData item = data.get(i);
            System.out.printf("%d. %s - Price: $%.2f (%.2f%%) Vol: %,d\n",
                    i + 1,
                    item.getTicker(),
                    item.getLatestPrice(),
                    item.getPercentChange(),
                    item.getCurrentVolume());
        }
        
        if (data.size() > displayCount) {
            System.out.println("... plus " + (data.size() - displayCount) + " more items");
        }
    }
    
    private static void testGetStockBySymbol(TradingViewService service, String symbol) throws StockApiException {
        System.out.println("\n=== Testing Get Stock by Symbol: " + symbol + " ===");
        
        try {
            String payload = service.getStockBySymbolRequestPayload(symbol);
            System.out.println("Request Payload:\n" + payload);
            
            PriceData result = service.getStockBySymbol(symbol);
            
            if (result != null) {
                System.out.println("\nStock Details:");
                System.out.printf("Ticker: %s\n", result.getTicker());
                System.out.printf("Name: %s\n", result.getName());
                System.out.printf("Price: $%.2f\n", result.getLatestPrice());
                System.out.printf("Premarket Change: %.2f%%\n", result.getPremarketChange());
                System.out.printf("Premarket Volume: %,d\n", result.getPremarketVolume());
                System.out.printf("Daily Volume: %,d\n", result.getCurrentVolume());
                System.out.printf("Exchange: %s\n", result.getExchange());
            } else {
                System.out.println("No results found for symbol: " + symbol);
            }
        } catch (Exception e) {
            System.err.println("Error testing stock by symbol: " + e.getMessage());
            throw e;
        }
    }

    @FunctionalInterface
    private interface TradingViewTestFunction {
        List<PriceData> execute() throws StockApiException;
    }
}