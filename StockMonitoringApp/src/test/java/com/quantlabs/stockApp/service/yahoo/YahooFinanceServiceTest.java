package com.quantlabs.stockApp.service.yahoo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.quantlabs.stockApp.http.ApiHttpClient;
import com.quantlabs.stockApp.exception.StockApiException;
import com.quantlabs.stockApp.model.PriceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

class YahooFinanceServiceTest {

    @Mock
    private ApiHttpClient httpClient;
    
    private YahooFinanceService yahooService;
    private final String testCrumb = "testCrumb123";
    private final String sampleResponse = "{\r\n"
    		+ "    \"finance\": {\r\n"
    		+ "        \"result\": [\r\n"
    		+ "            {\r\n"
    		+ "                \"start\": 0,\r\n"
    		+ "                \"count\": 2,\r\n"
    		+ "                \"total\": 143,\r\n"
    		+ "                \"records\": [\r\n"
    		+ "                    {\r\n"
    		+ "                        \"regularMarketVolume\": {\r\n"
    		+ "                            \"raw\": 9037215,\r\n"
    		+ "                            \"fmt\": \"9.037M\",\r\n"
    		+ "                            \"longFmt\": \"9,037,215\"\r\n"
    		+ "                        },\r\n"
    		+ "                        \"marketCap\": {\r\n"
    		+ "                            \"raw\": 8.705450060943604E9,\r\n"
    		+ "                            \"fmt\": \"8.705B\",\r\n"
    		+ "                            \"longFmt\": \"8,705,450,060\"\r\n"
    		+ "                        },\r\n"
    		+ "                        \"indices\": [\r\n"
    		+ "                            \"^RUT\"\r\n"
    		+ "                        ],\r\n"
    		+ "                        \"ticker\": \"IONQ\",\r\n"
    		+ "                        \"avgDailyVol3m\": {\r\n"
    		+ "                            \"raw\": 2.3884251612903226E7,\r\n"
    		+ "                            \"fmt\": \"23.884M\",\r\n"
    		+ "                            \"longFmt\": \"23,884,251\"\r\n"
    		+ "                        },\r\n"
    		+ "                        \"regularMarketChange\": {\r\n"
    		+ "                            \"raw\": -0.799999,\r\n"
    		+ "                            \"fmt\": \"-0.80\"\r\n"
    		+ "                        },\r\n"
    		+ "                        \"companyName\": \"IonQ, Inc.\",\r\n"
    		+ "                        \"regularMarketChangePercent\": {\r\n"
    		+ "                            \"raw\": -1.94979,\r\n"
    		+ "                            \"fmt\": \"-1.95%\"\r\n"
    		+ "                        },\r\n"
    		+ "                        \"region\": \"us\",\r\n"
    		+ "                        \"fiftyTwoWeekHigh\": {\r\n"
    		+ "                            \"raw\": 54.74,\r\n"
    		+ "                            \"fmt\": \"54.74\"\r\n"
    		+ "                        },\r\n"
    		+ "                        \"fiftyTwoWeekLow\": {\r\n"
    		+ "                            \"raw\": 6.54,\r\n"
    		+ "                            \"fmt\": \"6.54\"\r\n"
    		+ "                        },\r\n"
    		+ "                        \"regularMarketPrice\": {\r\n"
    		+ "                            \"raw\": 40.23,\r\n"
    		+ "                            \"fmt\": \"40.23\"\r\n"
    		+ "                        }\r\n"
    		+ "                    },\r\n"
    		+ "                    {\r\n"
    		+ "                        \"regularMarketVolume\": {\r\n"
    		+ "                            \"raw\": 9131730,\r\n"
    		+ "                            \"fmt\": \"9.132M\",\r\n"
    		+ "                            \"longFmt\": \"9,131,730\"\r\n"
    		+ "                        },\r\n"
    		+ "                        \"marketCap\": {\r\n"
    		+ "                            \"raw\": 1.651941872618103E11,\r\n"
    		+ "                            \"fmt\": \"165.194B\",\r\n"
    		+ "                            \"longFmt\": \"165,194,187,261\"\r\n"
    		+ "                        },\r\n"
    		+ "                        \"indices\": [\r\n"
    		+ "                            \"^NDX\"\r\n"
    		+ "                        ],\r\n"
    		+ "                        \"ticker\": \"PDD\",\r\n"
    		+ "                        \"avgDailyVol3m\": {\r\n"
    		+ "                            \"raw\": 7488732.258064516,\r\n"
    		+ "                            \"fmt\": \"7.489M\",\r\n"
    		+ "                            \"longFmt\": \"7,488,732\"\r\n"
    		+ "                        },\r\n"
    		+ "                        \"regularMarketChange\": {\r\n"
    		+ "                            \"raw\": 4.21,\r\n"
    		+ "                            \"fmt\": \"4.21\"\r\n"
    		+ "                        },\r\n"
    		+ "                        \"companyName\": \"PDD Holdings Inc.\",\r\n"
    		+ "                        \"regularMarketChangePercent\": {\r\n"
    		+ "                            \"raw\": 3.66916,\r\n"
    		+ "                            \"fmt\": \"3.67%\"\r\n"
    		+ "                        },\r\n"
    		+ "                        \"region\": \"us\",\r\n"
    		+ "                        \"fiftyTwoWeekHigh\": {\r\n"
    		+ "                            \"raw\": 155.67,\r\n"
    		+ "                            \"fmt\": \"155.67\"\r\n"
    		+ "                        },\r\n"
    		+ "                        \"fiftyTwoWeekLow\": {\r\n"
    		+ "                            \"raw\": 87.11,\r\n"
    		+ "                            \"fmt\": \"87.11\"\r\n"
    		+ "                        },\r\n"
    		+ "                        \"regularMarketPrice\": {\r\n"
    		+ "                            \"raw\": 118.95,\r\n"
    		+ "                            \"fmt\": \"118.95\"\r\n"
    		+ "                        }\r\n"
    		+ "                    }\r\n"
    		+ "                ],\r\n"
    		+ "                \"userHasReadRecord\": false,\r\n"
    		+ "                \"useRecords\": true\r\n"
    		+ "            }\r\n"
    		+ "        ],\r\n"
    		+ "        \"error\": null\r\n"
    		+ "    }\r\n"
    		+ "}";

    @BeforeEach
    void setUp() throws StockApiException {
        MockitoAnnotations.openMocks(this);
        when(httpClient.executeYahooRequest(
            anyString(), 
            eq(testCrumb),
            anyString(),
            anyString()
        )).thenReturn(sampleResponse);
        
        yahooService = new YahooFinanceService(httpClient, testCrumb);
    }

    @Test
    void testGetSP500() throws StockApiException {
        List<PriceData> result = yahooService.getSP500();
        
        assertNotNull(result);
        assertEquals(2, result.size());
        
        PriceData apple = result.get(0);
        assertEquals("IONQ", apple.getTicker());
        assertEquals("IonQ, Inc.", apple.getName());
        //assertEquals(175.34, apple.getLatestPrice(), 0.001);
        //assertEquals(1.23, apple.getPercentChange(), 0.001);
        //assertEquals(12345678, apple.getCurrentVolume());
        
        verify(httpClient, times(1)).executeYahooRequest(
            anyString(),
            eq(testCrumb),
            eq("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"),
            eq("https://finance.yahoo.com")
        );
    }

    @Test
    void testServiceWithHttpError() throws StockApiException {
        when(httpClient.executeYahooRequest(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new StockApiException("API Error"));
        
        assertThrows(StockApiException.class, () -> yahooService.getSP500());
    }
}