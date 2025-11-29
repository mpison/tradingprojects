package com.quantlabs.stockApp.service.tradingview;

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

class TradingViewServiceTest {

    @Mock
    private ApiHttpClient httpClient;
    
    private TradingViewService tradingViewService;
	/*
	 * private final String sampleResponse = "{\"data\":[" +
	 * "{\"d\":[\"AAPL - Apple Inc.\",\"Tech company\",175.34,177.89,1.23,0.89,0,0,12345678,1.2,2000000000,\"USD\",\"Technology\",\"NASDAQ\",\"Buy\",10000000,\"NASDAQ\"]},"
	 * +
	 * "{\"d\":[\"MSFT - Microsoft\",\"Tech company\",325.56,328.12,0.89,0.45,0,0,8765432,1.1,1800000000,\"USD\",\"Technology\",\"NASDAQ\",\"Hold\",8000000,\"NASDAQ\"]}"
	 * + "]}";
	 */

    @BeforeEach
    void setUp() throws StockApiException {
        MockitoAnnotations.openMocks(this);
        /*when(httpClient.executeTradingViewRequest(anyString(), anyString(), anyString()))
            .thenReturn(sampleResponse);
        */
        tradingViewService = new TradingViewService(httpClient);
    }

	/*
	 * @Test void testGetDowJones() throws StockApiException { List<PriceData>
	 * result = tradingViewService.getDowJones();
	 * 
	 * assertNotNull(result); assertEquals(2, result.size());
	 * 
	 * PriceData apple = result.get(0); assertEquals("AAPL", apple.getTicker());
	 * assertEquals(175.34, apple.getLatestPrice(), 0.001); assertEquals(1.23,
	 * apple.getPercentChange(), 0.001); assertEquals(12345678,
	 * apple.getCurrentVolume());
	 * 
	 * verify(httpClient, times(1)).executeTradingViewRequest( anyString(),
	 * eq("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"
	 * ), eq("https://www.tradingview.com") ); }
	 */

	/*
	 * @Test void testServiceWithHttpError() throws StockApiException {
	 * when(httpClient.executeTradingViewRequest(anyString(), anyString(),
	 * anyString())) .thenThrow(new StockApiException("API Error"));
	 * 
	 * assertThrows(StockApiException.class, () ->
	 * tradingViewService.getDowJones()); }
	 */
    
    @Test
    void testGetPennyStocksPreMarket() throws StockApiException {
    	
    	String pennyJsonResponse = "{\"totalCount\":1103,\"data\":[{\"s\":\"AMEX:PMNT\",\"d\":[\"PMNT\",\"Perfect Moment Ltd.\",\"perfect-moment-ltd\",\"delayed_streaming_900\",\"stock\",[\"common\"],0.67,10000,1,\"false\",0,\"USD\",0.5563,0.4509,0.84,0.72,0.5563,130.7162534435262,91.5633608815427,-16.53413353338334,-18.94661154053568,0.5276,0.4771,0.4382,129854446,320677085,5313626,17466052.0786,\"USD\",\"Retail trade\",\"america\",\"Retail Trade\",\"Neutral\",\"Neutral\",34525375.3,\"AMEX\",null,129.51101928374655]},{\"s\":\"AMEX:SRXH\",\"d\":[\"SRXH\",\"SRX Health Solutions, Inc.\",\"better-choice-company\",\"delayed_streaming_900\",\"stock\",[\"common\"],0.3877,10000,1,\"false\",0,\"USD\",0.5,0.372,0.529,0.77999,0.5044,43.16838995568686,84.63810930576072,29.13223140495868,-25.6,0.27,0.3355,0.372,32831318,666685767,8544115,16599402.999999998,\"USD\",\"Health technology\",\"america\",\"Health Technology\",\"Neutral\",\"Neutral\",67932944.4,\"AMEX\",null,42.9837518463811]}]}\r\n"
    			+ "";
    	
    	when(httpClient.executeTradingViewRequest(anyString(), anyString(), anyString()))
        .thenReturn(pennyJsonResponse);
    	
    	
        // Log the request payload for debugging
        String requestPayload = tradingViewService.getPreMarketRequestPayload();
        System.out.println("Request Payload:\n" + requestPayload);

        // Call the service method
        List<PriceData> result = tradingViewService.getPennyStocksPreMarket();
        
        // Assert results
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Expected 2 results");
        
        // Verify first item (AAPL)
        PriceData first = result.get(0);
        assertEquals("PMNT", first.getTicker(), "Expected ticker to be PGEN");
        assertEquals(0.5563, first.getLatestPrice(), 0.001, "Expected latest price to be 0.5563");
        assertEquals(91.5633608815427, first.getPercentChange(), 0.001, "Expected percent change to be 91.5633608815427");
        assertEquals(320677085, first.getCurrentVolume(), "Expected volume to be 320677085");

        // Log results (mimicking MainTest)
        System.out.printf("Received %d results\n", result.size());
        int displayCount = Math.min(result.size(), 3);
        for (int i = 0; i < displayCount; i++) {
            PriceData item = result.get(i);
            System.out.printf("%d. %s - Price: $%.2f (%.2f%%) Vol: %,d\n",
                    i + 1, item.getTicker(), item.getLatestPrice(), item.getPercentChange(), item.getCurrentVolume());
        }
        if (result.size() > displayCount) {
            System.out.println("... plus " + (result.size() - displayCount) + " more items");
        }

        // Additional details for first item
        System.out.println("\nPre-Market Details:");
        System.out.printf("Ticker: %s\n", first.getTicker());
        System.out.printf("Price: $%.2f\n", first.getLatestPrice());
        System.out.printf("Pre-Market Change: %.2f%%\n", first.getPercentChange());
        System.out.printf("Volume: %,d\n", first.getCurrentVolume());

        // Verify HTTP client call
        verify(httpClient, times(1)).executeTradingViewRequest(
            anyString(),
            eq("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"),
            eq("https://www.tradingview.com")
        );
    }
    
    
    @Test
    void testGetPennyStockStandardMarketStocksByVolume() throws StockApiException {
        String standardMarketJsonResponse = "{\"totalCount\":1293,\"data\":[{\"s\":\"AMEX:SRXH\",\"d\":[\"SRXH\",\"SRX Health Solutions, Inc.\",\"better-choice-company\",\"delayed_streaming_900\",\"stock\",[\"common\"],0.3877,10000,1,\"false\",0,\"USD\",0.5,0.372,0.529,0.77999,0.5044,43.16838995568686,84.63810930576072,29.13223140495868,-25.6,0.27,0.3355,0.372,32831318,666685767,8544115,16599402.999999998,\"USD\",\"Health technology\",\"america\",\"Health Technology\",\"Neutral\",\"Neutral\",67932944.4,\"AMEX\",null,42.9837518463811]},{\"s\":\"NASDAQ:PGEN\",\"d\":[\"PGEN\",\"Precigen, Inc.\",\"precigen\",\"delayed_streaming_900\",\"stock\",[\"common\"],2.59,100,1,\"false\",0,\"USD\",2.94,2.86,4.03,3.4899,2.94,39.999999999999986,58.918918918918905,13.076923076923071,-2.721088435374152,2.59,2.51,2.76,28040370,173716132,2254168,876040344,\"USD\",\"Health technology\",\"america\",\"Health Technology\",\"Buy\",\"Buy\",20080221.8,\"NASDAQ\",[{\"name\":\"NASDAQ Composite\",\"proname\":\"NASDAQ:IXIC\"},{\"name\":\"Russell 2000\",\"proname\":\"TVC:RUT\"},{\"name\":\"Russell 3000\",\"proname\":\"TVC:RUA\"},{\"name\":\"NASDAQ Biotechnology\",\"proname\":\"NASDAQ:NBI\"},{\"name\":\"Mini-Russell 2000\",\"proname\":\"CBOEFTSE:MRUT\"}],40.54054054054054]}]}\r\n";
        
        when(httpClient.executeTradingViewRequest(anyString(), anyString(), anyString()))
            .thenReturn(standardMarketJsonResponse);
        
        // Get the request payload for debugging
        String requestPayload = tradingViewService.getPennyStockStandardMarketRequestPayload();
        System.out.println("Standard Market Request Payload:\n" + requestPayload);

        // Call the service method
        List<PriceData> result = tradingViewService.getPennyStockStandardMarketStocksByVolume();
        
        // Assert results
        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Expected 2 results");
        
        // Verify first item (AAPL)
        PriceData first = result.get(0);
        assertEquals("SRXH", first.getTicker(), "Expected ticker to be SRXH");
        assertEquals(0.5, first.getLatestPrice(), 0.001, "Expected latest price to be 0.5");
        assertEquals(84.63810930576072, first.getPercentChange(), 0.001, "Expected percent change to be 84.63810930576072");
        assertEquals(666685767, first.getCurrentVolume(), "Expected volume to be 666685767");

        // Verify HTTP client call
        verify(httpClient, times(1)).executeTradingViewRequest(
            anyString(),
            eq("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"),
            eq("https://www.tradingview.com")
        );
    }
    
    
    @Test
    void testGetPennyStocksPostMarketByPostMarketVolume() throws StockApiException {
        String postMarketJson = "{\"totalCount\":365,\"data\":[{\"s\":\"NYSE:HL\",\"d\":[\"HL\",\"Hecla Mining Company\",\"hecla-mining\",\"delayed_streaming_900\",\"stock\",[\"common\"],7.7,100,1,\"false\",0,\"USD\",7.67,7.68,7.72,7.83,7.6994,0.522193211488251,0.13054830287205987,0,0.13037809647978862,7.64,7.55,7.66,54223,24215241,7132279,5138774590,\"USD\",\"Non-energy minerals\",\"america\",\"Non-Energy Minerals\",\"Buy\",\"Buy\",26259105.7,\"NYSE\",[{\"name\":\"Russell 2000\",\"proname\":\"TVC:RUT\"},{\"name\":\"PHLX Gold/Silver Sector\",\"proname\":\"NASDAQ:XAU\"},{\"name\":\"Nasdaq US Small Cap Growth\",\"proname\":\"NASDAQ:NQUSSG\"},{\"name\":\"Russell 3000\",\"proname\":\"TVC:RUA\"},{\"name\":\"Mini-Russell 2000\",\"proname\":\"CBOEFTSE:MRUT\"}],0.13054830287205987]},{\"s\":\"NYSE:YMM\",\"d\":[\"YMM\",\"Full Truck Alliance Co. Ltd.\",\"full-truck-alliance-co-ltd\",\"delayed_streaming_900\",\"dr\",[\"\"],10.94,100,1,\"false\",0,\"USD\",11.06,11.17,11.08,11.165,11.18,1.578458681522748,2.692664809656462,2.2181146025878022,0.9945750452079514,10.81,10.78,11.06,39857,12715012,3989919,11567590213,\"USD\",\"Technology services\",\"america\",\"Technology Services\",\"StrongBuy\",\"Strong buy\",13946941.8,\"NYSE\",[{\"name\":\"NASDAQ Golden Dragon China\",\"proname\":\"NASDAQ:HXC\"}],0.46425255338905025]}]}\r\n"
        		+ "";
        
        when(httpClient.executeTradingViewRequest(anyString(), anyString(), anyString()))
            .thenReturn(postMarketJson);
        
        List<PriceData> result = tradingViewService.getPennyStocksPostMarketByPostMarketVolume();
        
        assertNotNull(result);
        assertEquals(2, result.size());
        
        PriceData stock = result.get(0);
        assertEquals("HL", stock.getTicker());
        assertEquals(7.67, stock.getLatestPrice(), 0.001);
        assertEquals(0.13054830287205987, stock.getPercentChange(), 0.001);
        assertEquals(7132279, stock.getPostmarketVolume());
        
        // Verify request payload
        String payload = tradingViewService.getPennyStocksPostMarketRequestPayload();
        assertTrue(payload.contains("postmarket_volume"));
        assertTrue(payload.contains("postmarket_change"));
    }

    @Test
    void testGetPennyStocksPostMarketByPostMarketVolume_ErrorHandling() throws StockApiException {
        when(httpClient.executeTradingViewRequest(anyString(), anyString(), anyString()))
            .thenThrow(new StockApiException("API Error"));
        
        assertThrows(StockApiException.class, 
            () -> tradingViewService.getPennyStocksPostMarketByPostMarketVolume());
    }
    
    
    @Test
    void testGetIndexStocksMarketByPreMarketVolume() throws StockApiException {
        String indexStocksJson = "{\"totalCount\":687,\"data\":[{\"s\":\"NASDAQ:PGEN\",\"d\":[\"PGEN\",\"Precigen, Inc.\",\"precigen\",\"delayed_streaming_900\",\"stock\",[\"common\"],2.59,100,1,\"false\",0,\"USD\",2.94,2.86,4.03,3.4899,2.94,39.999999999999986,58.918918918918905,13.076923076923071,-2.721088435374152,2.59,2.51,2.76,28040370,173716132,2254168,876040344,\"USD\",\"Health technology\",\"america\",\"Health Technology\",\"Buy\",\"Buy\",20080221.8,\"NASDAQ\",[{\"name\":\"NASDAQ Composite\",\"proname\":\"NASDAQ:IXIC\"},{\"name\":\"Russell 2000\",\"proname\":\"TVC:RUT\"},{\"name\":\"Russell 3000\",\"proname\":\"TVC:RUA\"},{\"name\":\"NASDAQ Biotechnology\",\"proname\":\"NASDAQ:NBI\"},{\"name\":\"Mini-Russell 2000\",\"proname\":\"CBOEFTSE:MRUT\"}],40.54054054054054]},{\"s\":\"NASDAQ:INTC\",\"d\":[\"INTC\",\"Intel Corporation\",\"intel\",\"delayed_streaming_900\",\"stock\",[\"common\"],25,100,1,\"false\",0,\"USD\",24.56,24.49,25.12,25.645,24.57,4.777870913663037,2.933780385582562,-1.7992802878848573,-0.2850162866449523,24.15,24.11,24.32,13705697,310158855,13349973,107499117662,\"USD\",\"Electronic technology\",\"america\",\"Electronic Technology\",\"Neutral\",\"Neutral\",133469471.5,\"NASDAQ\",[{\"name\":\"S\\u0026P 500\",\"proname\":\"SP:SPX\"},{\"name\":\"NASDAQ 100\",\"proname\":\"NASDAQ:NDX\"},{\"name\":\"NASDAQ Composite\",\"proname\":\"NASDAQ:IXIC\"},{\"name\":\"PHLX Semiconductor Sector\",\"proname\":\"NASDAQ:SOX\"},{\"name\":\"S\\u0026P 500 Information Technology\",\"proname\":\"SP:S5INFT\"},{\"name\":\"S\\u0026P 100\",\"proname\":\"SP:OEX\"},{\"name\":\"Russell 3000\",\"proname\":\"TVC:RUA\"},{\"name\":\"NASDAQ-100 Technology Sector\",\"proname\":\"NASDAQ:NDXT\"},{\"name\":\"Russell 1000\",\"proname\":\"TVC:RUI\"},{\"name\":\"NASDAQ Computer\",\"proname\":\"NASDAQ:IXCO\"},{\"name\":\"S\\u0026P 500 ESG\",\"proname\":\"CBOE:SPESG\"},{\"name\":\"NASDAQ CB Insights Metaverse US Index\",\"proname\":\"NASDAQ:NYMETA\"}],4.819782062028509]}]}\r\n"
        		+ "";
        
        when(httpClient.executeTradingViewRequest(anyString(), anyString(), anyString()))
            .thenReturn(indexStocksJson);
        
        List<PriceData> result = tradingViewService.getIndexStocksMarketByPreMarketVolume();
        
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verify first index stock
        PriceData spx = result.get(0);
        assertEquals("PGEN", spx.getTicker());
        assertEquals(2.94, spx.getLatestPrice(), 0.001);
        assertEquals(58.918918918918905, spx.getPercentChange(), 0.001);
        assertEquals(28040370, spx.getPremarketVolume());
        
        // Verify request payload contains index symbols
        String payload = tradingViewService.getIndexStocksMarketRequestPayload();
        assertTrue(payload.contains("SYML:SP;SPX"));
        assertTrue(payload.contains("SYML:NASDAQ;NDX"));
        assertTrue(payload.contains("premarket_volume"));
    }

    @Test
    void testGetIndexStocksMarketByPreMarketVolume_EmptyResponse() throws StockApiException {
        when(httpClient.executeTradingViewRequest(anyString(), anyString(), anyString()))
            .thenReturn("{\"data\":[]}");
        
        List<PriceData> result = tradingViewService.getIndexStocksMarketByPreMarketVolume();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testGetIndexStocksMarketByStandardMarketVolume() throws StockApiException {
        String indexStocksJson = "{\"totalCount\":624,\"data\":[{\"s\":\"NASDAQ:PGEN\",\"d\":[\"PGEN\",\"Precigen, Inc.\",\"precigen\",\"delayed_streaming_900\",\"stock\",[\"common\"],2.59,100,1,\"false\",0,\"USD\",2.94,2.86,4.03,3.4899,2.94,39.999999999999986,58.918918918918905,13.076923076923071,-2.721088435374152,2.59,2.51,2.76,28040370,173716132,2254168,876040344,\"USD\",\"Health technology\",\"america\",\"Health Technology\",\"Buy\",\"Buy\",20080221.8,\"NASDAQ\",[{\"name\":\"NASDAQ Composite\",\"proname\":\"NASDAQ:IXIC\"},{\"name\":\"Russell 2000\",\"proname\":\"TVC:RUT\"},{\"name\":\"Russell 3000\",\"proname\":\"TVC:RUA\"},{\"name\":\"NASDAQ Biotechnology\",\"proname\":\"NASDAQ:NBI\"},{\"name\":\"Mini-Russell 2000\",\"proname\":\"CBOEFTSE:MRUT\"}],40.54054054054054]},{\"s\":\"NASDAQ:WULF\",\"d\":[\"WULF\",\"TeraWulf Inc.\",\"terawulf\",\"delayed_streaming_900\",\"stock\",[\"common\"],8.54,100,1,\"false\",0,\"USD\",8.97,8.91,8.95,9.23,9,-1.9517795637198816,2.9850746268656687,5.158264947245033,-0.6688963210702397,8.3,8.141,8.8606,3504989,108159937,2951139,3515579564.0000005,\"USD\",\"Technology services\",\"america\",\"Technology Services\",\"StrongBuy\",\"Strong buy\",67617865.7,\"NASDAQ\",[{\"name\":\"NASDAQ Composite\",\"proname\":\"NASDAQ:IXIC\"},{\"name\":\"Russell 2000\",\"proname\":\"TVC:RUT\"},{\"name\":\"Nasdaq US Small Cap Growth\",\"proname\":\"NASDAQ:NQUSSG\"},{\"name\":\"Russell 3000\",\"proname\":\"TVC:RUA\"},{\"name\":\"NASDAQ Computer\",\"proname\":\"NASDAQ:IXCO\"},{\"name\":\"Mini-Russell 2000\",\"proname\":\"CBOEFTSE:MRUT\"}],-2.066590126291636]}]}\r\n"
        		+ "";
        
        when(httpClient.executeTradingViewRequest(anyString(), anyString(), anyString()))
            .thenReturn(indexStocksJson);
        
        List<PriceData> result = tradingViewService.getIndexStocksMarketByStandardMarketVolume();
        
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verify first index stock
        PriceData spx = result.get(0);
        assertEquals("PGEN", spx.getTicker());
        assertEquals(2.94, spx.getLatestPrice(), 0.001);
        assertEquals(58.918918918918905, spx.getPercentChange(), 0.001);
        assertEquals(173716132, spx.getCurrentVolume());
        
        // Verify request payload contains index symbols and volume sort
        String payload = tradingViewService.getIndexStocksStandardMarketRequestPayload();
        assertTrue(payload.contains("SYML:SP;SPX"));
        //assertTrue(payload.contains("\"sortBy\":\"volume\""));
        //assertTrue(payload.contains("\"range\":[0,2]"));
    }

    @Test
    void testGetIndexStocksMarketByStandardMarketVolume_ErrorHandling() throws StockApiException {
        when(httpClient.executeTradingViewRequest(anyString(), anyString(), anyString()))
            .thenThrow(new StockApiException("API Error"));
        
        assertThrows(StockApiException.class, 
            () -> tradingViewService.getIndexStocksMarketByStandardMarketVolume());
    }
    
    @Test
    void testGetIndexStocksMarketByPostMarketVolume() throws StockApiException {
        String responseJson = "{\"totalCount\":515,\"data\":[{\"s\":\"NYSE:HL\",\"d\":[\"HL\",\"Hecla Mining Company\",\"hecla-mining\",\"delayed_streaming_900\",\"stock\",[\"common\"],7.7,100,1,\"false\",0,\"USD\",7.67,7.68,7.72,7.83,7.6994,0.522193211488251,0.13054830287205987,0,0.13037809647978862,7.64,7.55,7.66,54223,24215241,7132279,5138774590,\"USD\",\"Non-energy minerals\",\"america\",\"Non-Energy Minerals\",\"Buy\",\"Buy\",26259105.7,\"NYSE\",[{\"name\":\"Russell 2000\",\"proname\":\"TVC:RUT\"},{\"name\":\"PHLX Gold/Silver Sector\",\"proname\":\"NASDAQ:XAU\"},{\"name\":\"Nasdaq US Small Cap Growth\",\"proname\":\"NASDAQ:NQUSSG\"},{\"name\":\"Russell 3000\",\"proname\":\"TVC:RUA\"},{\"name\":\"Mini-Russell 2000\",\"proname\":\"CBOEFTSE:MRUT\"}],0.13054830287205987]},{\"s\":\"NASDAQ:MSFT\",\"d\":[\"MSFT\",\"Microsoft Corporation\",\"microsoft\",\"delayed_streaming_900\",\"stock\",[\"common\"],523.01,100,1,\"false\",0,\"USD\",520.17,520.35,524.47,526.1,520.7,0.10143928954217821,-0.44212218649518814,-0.49735065133806894,0.03460407174578766,521.7,519.08,519.6,98624,25213086,4831688,3866510093305,\"USD\",\"Technology services\",\"america\",\"Technology Services\",\"StrongBuy\",\"Strong buy\",20147050.2,\"NASDAQ\",[{\"name\":\"S\\u0026P 500\",\"proname\":\"SP:SPX\"},{\"name\":\"NASDAQ 100\",\"proname\":\"NASDAQ:NDX\"},{\"name\":\"Dow Jones Industrial Average\",\"proname\":\"DJ:DJI\"},{\"name\":\"NASDAQ Composite\",\"proname\":\"NASDAQ:IXIC\"},{\"name\":\"S\\u0026P 500 Information Technology\",\"proname\":\"SP:S5INFT\"},{\"name\":\"S\\u0026P 100\",\"proname\":\"SP:OEX\"},{\"name\":\"ISE CTA Cloud Computing\",\"proname\":\"NASDAQ:CPQ\"},{\"name\":\"Nasdaq US Large Cap Growth\",\"proname\":\"NASDAQ:NQUSLG\"},{\"name\":\"Russell 3000\",\"proname\":\"TVC:RUA\"},{\"name\":\"Dow Jones Composite Average\",\"proname\":\"DJ:DJA\"},{\"name\":\"NASDAQ-100 Technology Sector\",\"proname\":\"NASDAQ:NDXT\"},{\"name\":\"Russell 1000\",\"proname\":\"TVC:RUI\"},{\"name\":\"NASDAQ Computer\",\"proname\":\"NASDAQ:IXCO\"},{\"name\":\"S\\u0026P 500 ESG\",\"proname\":\"CBOE:SPESG\"},{\"name\":\"NYSE Arca Major Market\",\"proname\":\"NYSE:XMI\"},{\"name\":\"NASDAQ CB Insights Metaverse US Index\",\"proname\":\"NASDAQ:NYMETA\"}],0.05550451691930095]}]}\r\n"
        		+ "";
        
        when(httpClient.executeTradingViewRequest(anyString(), anyString(), anyString()))
            .thenReturn(responseJson);
        
        List<PriceData> result = tradingViewService.getIndexStocksMarketByPostMarketVolume();
        
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // Verify first index stock
        PriceData spx = result.get(0);
        assertEquals("HL", spx.getTicker());
        assertEquals(7.67, spx.getLatestPrice(), 0.001);
        assertEquals(0.13037809647978862, spx.getPostmarketChange(), 0.001);
        assertEquals(7132279, spx.getPostmarketVolume());
        
        // Verify request payload
        String payload = tradingViewService.getIndexStocksPostMarketRequestPayload();
        assertTrue(payload.contains("\"sortBy\":\"postmarket_volume\""));
        assertTrue(payload.contains("\"postmarket_change\""));
        //assertTrue(payload.contains("NYSE:HL"));
    }

    @Test
    void testGetIndexStocksMarketByPostMarketVolume_EmptyResponse() throws StockApiException {
        when(httpClient.executeTradingViewRequest(anyString(), anyString(), anyString()))
            .thenReturn("{\"data\":[]}");
        
        List<PriceData> result = tradingViewService.getIndexStocksMarketByPostMarketVolume();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    void testGetStocksMarketBySymbol() throws StockApiException {
        String symbol = "AAPL";
        String responseJson = "{\"totalCount\":1,\"data\":[{\"s\":\"NASDAQ:AAPL\",\"d\":[\"AAPL\",\"Apple Inc.\",\"apple\",\"delayed_streaming_900\",\"stock\",[\"common\"],233.56,100,1,\"false\",0,\"USD\",231.59,231.19,234.14,234.28,231.77,0.3350803333619731,-0.5112123034624958,-1.0299145299145285,-0.1727190293190577,232.45,229.335,230.88,352984,56038647,9617196,3436885784335,\"USD\",\"Electronic technology\",\"america\",\"Electronic Technology\",\"Buy\",\"Buy\",72713590.8999993,\"NASDAQ\",[{\"name\":\"S\\u0026P 500\",\"proname\":\"SP:SPX\"},{\"name\":\"NASDAQ 100\",\"proname\":\"NASDAQ:NDX\"},{\"name\":\"Dow Jones Industrial Average\",\"proname\":\"DJ:DJI\"},{\"name\":\"NASDAQ Composite\",\"proname\":\"NASDAQ:IXIC\"},{\"name\":\"S\\u0026P 500 Information Technology\",\"proname\":\"SP:S5INFT\"},{\"name\":\"S\\u0026P 100\",\"proname\":\"SP:OEX\"},{\"name\":\"Nasdaq US Large Cap Growth\",\"proname\":\"NASDAQ:NQUSLG\"},{\"name\":\"Russell 3000\",\"proname\":\"TVC:RUA\"},{\"name\":\"Dow Jones Composite Average\",\"proname\":\"DJ:DJA\"},{\"name\":\"NASDAQ-100 Technology Sector\",\"proname\":\"NASDAQ:NDXT\"},{\"name\":\"Russell 1000\",\"proname\":\"TVC:RUI\"},{\"name\":\"NASDAQ Computer\",\"proname\":\"NASDAQ:IXCO\"},{\"name\":\"S\\u0026P 500 ESG\",\"proname\":\"CBOE:SPESG\"},{\"name\":\"NASDAQ CB Insights Metaverse US Index\",\"proname\":\"NASDAQ:NYMETA\"}],0.524100008591803]}]}\r\n"
        		+ "";
        
        when(httpClient.executeTradingViewRequest(anyString(), anyString(), anyString()))
            .thenReturn(responseJson);
        
        List<PriceData> result = tradingViewService.getStocksMarketBySymbol(symbol);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        
        PriceData apple = result.get(0);
        assertEquals("AAPL", apple.getTicker());
        assertEquals(231.59, apple.getLatestPrice(), 0.001);
        assertEquals(56038647, apple.getCurrentVolume());
        
        // Verify request payload contains the symbol
        String payload = tradingViewService.getStocksMarketBySymbolRequestPayload(symbol);
        assertTrue(payload.contains("\"match\""));
        assertTrue(payload.contains("AAPL"));
    }

    @Test
    void testGetStocksMarketBySymbol_EmptySymbol() {
        assertThrows(IllegalArgumentException.class, 
            () -> tradingViewService.getStocksMarketBySymbol(""));
    }

    @Test
    void testGetStocksMarketBySymbol_NotFound() throws StockApiException {
        String symbol = "INVALID";
        when(httpClient.executeTradingViewRequest(anyString(), anyString(), anyString()))
            .thenReturn("{\"data\":[]}");
        
        List<PriceData> result = tradingViewService.getStocksMarketBySymbol(symbol);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}