package com.quantlabs.stockApp.service.yahoo;

import com.quantlabs.stockApp.http.ApiHttpClient;
import com.quantlabs.stockApp.http.OkHttpClientImpl;
import com.quantlabs.stockApp.exception.StockApiException;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.service.StockDataProvider;

import java.util.List;

public class YahooFinanceService extends StockDataProvider {
    private final YahooRequestBuilder requestBuilder;
    private final YahooResponseParser responseParser;
    private final ApiHttpClient httpClient;
    private static String crumb = "1TD9XXQrX6i";

    public YahooFinanceService(ApiHttpClient httpClient, String crumb) {
        this.httpClient = httpClient;
        this.crumb = crumb;
        this.requestBuilder = new YahooRequestBuilder();
        this.responseParser = new YahooResponseParser();
    }
    
    public YahooFinanceService() {
        this.httpClient = new OkHttpClientImpl();
        this.requestBuilder = new YahooRequestBuilder();
        this.responseParser = new YahooResponseParser();
    }

    @Override
    public List<PriceData> getDowJones() throws StockApiException {
        try {
            String request = requestBuilder.buildDowJonesRequest();
            String response = executeYahooRequest(request);
            return responseParser.parse(response);
        } catch (Exception e) {
            throw new StockApiException("Failed to get Dow Jones data: " + e.getMessage());
        }
    }

    @Override
    public List<PriceData> getNasdaq() throws StockApiException {
        try {
            String request = requestBuilder.buildNasdaqRequest();
            String response = executeYahooRequest(request);
            return responseParser.parse(response);
        } catch (Exception e) {
            throw new StockApiException("Failed to get Nasdaq data: " + e.getMessage());
        }
    }

    @Override
    public List<PriceData> getSP500() throws StockApiException {
        try {
            String request = requestBuilder.buildSP500Request();
            String response = executeYahooRequest(request);
            System.out.println("request: " + request);
            System.out.println("response: " + response);
            return responseParser.parse(response);
        } catch (Exception e) {
            throw new StockApiException("Failed to get S&P 500 data: " + e.getMessage());
        }
    }

    @Override
    public List<PriceData> getRussell2000() throws StockApiException {
        try {
            String request = requestBuilder.buildRussell2000Request();
            String response = executeYahooRequest(request);
            return responseParser.parse(response);
        } catch (Exception e) {
            throw new StockApiException("Failed to get Russell 2000 data: " + e.getMessage());
        }
    }

    @Override
    public List<PriceData> getPennyStocksByVolume() throws StockApiException {
        try {
            String request = requestBuilder.buildPennyStocksByVolumeRequest();
            String response = executeYahooRequest(request);
            return responseParser.parse(response);
        } catch (Exception e) {
            throw new StockApiException("Failed to get penny stocks by volume: " + e.getMessage());
        }
    }

    @Override
    public List<PriceData> getPennyStocksByPercentChange() throws StockApiException {
        try {
            String request = requestBuilder.buildPennyStocksByPercentChangeRequest();
            String response = executeYahooRequest(request);
            return responseParser.parse(response);
        } catch (Exception e) {
            throw new StockApiException("Failed to get penny stocks by percent change: " + e.getMessage());
        }
    }

    private String executeYahooRequest(String payload) throws StockApiException {
        try {
            return httpClient.executeYahooRequest(
                payload,
                crumb,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
                "https://finance.yahoo.com"
            );
        } catch (Exception e) {
            throw new StockApiException("Yahoo API request failed: " + e.getMessage());
        }
    }
}