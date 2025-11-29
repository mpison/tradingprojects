package com.quantlabs.stockApp.service.yahoo;

import com.quantlabs.stockApp.http.OkHttpClientImpl;
import com.quantlabs.stockApp.exception.StockApiException;
import com.quantlabs.stockApp.model.PriceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "true")
class YahooFinanceServiceIntegrationTest {

    private YahooFinanceService yahooService;
    private String crumb;

    @BeforeEach
    void setUp() throws StockApiException {
        OkHttpClientImpl httpClient = new OkHttpClientImpl();
        // Get fresh crumb for each test
        this.crumb = "1TD9XXQrX6i";//httpClient.getYahooCrumb();
        this.yahooService = new YahooFinanceService(httpClient, crumb);
    }

    @Test
    void testGetDowJones() throws StockApiException {
        List<PriceData> result = yahooService.getDowJones();
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // Verify at least one record has expected fields
        PriceData firstItem = result.get(0);
        assertNotNull(firstItem.getTicker());
        assertFalse(firstItem.getTicker().isEmpty());
        assertTrue(firstItem.getLatestPrice() > 0);
        assertNotNull(firstItem.getName());
    }

    @Test
    void testGetNasdaq() throws StockApiException {
        List<PriceData> result = yahooService.getNasdaq();
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        PriceData firstItem = result.get(0);
        assertNotNull(firstItem.getTicker());
        assertTrue(firstItem.getLatestPrice() > 0);
    }

    @Test
    void testGetSP500() throws StockApiException {
        List<PriceData> result = yahooService.getSP500();
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        PriceData firstItem = result.get(0);
        assertNotNull(firstItem.getTicker());
        assertTrue(firstItem.getLatestPrice() > 0);
    }

    @Test
    void testGetRussell2000() throws StockApiException {
        List<PriceData> result = yahooService.getRussell2000();
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        PriceData firstItem = result.get(0);
        assertNotNull(firstItem.getTicker());
        assertTrue(firstItem.getLatestPrice() > 0);
    }

    @Test
    void testGetPennyStocksByVolume() throws StockApiException {
        List<PriceData> result = yahooService.getPennyStocksByVolume();
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        PriceData firstItem = result.get(0);
        assertNotNull(firstItem.getTicker());
        assertTrue(firstItem.getLatestPrice() > 0);
        assertTrue(firstItem.getCurrentVolume() > 0);
    }

    @Test
    void testGetPennyStocksByPercentChange() throws StockApiException {
        List<PriceData> result = yahooService.getPennyStocksByPercentChange();
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        PriceData firstItem = result.get(0);
        assertNotNull(firstItem.getTicker());
        assertTrue(firstItem.getLatestPrice() > 0);
        assertNotNull(firstItem.getPercentChange());
    }
}