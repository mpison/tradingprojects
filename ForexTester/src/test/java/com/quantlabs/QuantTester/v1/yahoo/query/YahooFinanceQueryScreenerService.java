package com.quantlabs.QuantTester.v1.yahoo.query;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


/*
 * java call based query from https://finance.yahoo.com/research-hub/screener/day_gainers/
 */
public class YahooFinanceQueryScreenerService {

    private static final String BASE_URL = "https://query1.finance.yahoo.com";
    private static final String COOKIE = "tbla_id=b1e575b5-3983-49b1-be2c-666690b19e13-tuctf23bf6b; F=d=LTfYD5A9vJPBkgLe_6GBQux_9IZMRxUkRHo2nN2cNg--; PH=l=en-PH; Y=v=1&n=5rlmr5ed7g231&l=i84_a4diekz/o&p=m2lvvph00000000&iz=4114&r=ae&intl=ph; GUC=AQEACAJoW8hoj0Ig1gTF&s=AQAAAGLMw3Tk&g=aFqCJQ; A1=d=AQABBHI2KmgCEBGuM9CsvjLJg0ixmqiW-jgFEgEACALIW2iPaJkr0iMA_eMDAAcIcjYqaKiW-jgID-CO32qmSa2JTg5LC0nlsQkBBwoBMw&S=AQAAAuXFdrnQzCBtF2946U8Rbtk; A3=d=AQABBHI2KmgCEBGuM9CsvjLJg0ixmqiW-jgFEgEACALIW2iPaJkr0iMA_eMDAAcIcjYqaKiW-jgID-CO32qmSa2JTg5LC0nlsQkBBwoBMw&S=AQAAAuXFdrnQzCBtF2946U8Rbtk; fes-ds-navbadgestockpicks=1751857265846; _ga=GA1.1.47815853.1751400777; fes-ds-fes_1022_silver-evergreen-v2=1; fes-ds-fes_1004_silver-portfolios=1; fes-ds-1015_stockpicks-signedin-phase1=1; gpp=DBABBg~BVoIgACY.QA; gpp_sid=8; ucs=tr=1752802684000; OTH=v=2&s=0&d=eyJraWQiOiIwIiwiYWxnIjoiUlMyNTYifQ.eyJjdSI6eyJndWlkIjoiREYyM1k3R0VZU0ZKQTVJRFZNNUxJM0UySEUiLCJwZXJzaXN0ZW50Ijp0cnVlLCJzaWQiOiJkSDViWWhBNHpodlIifX0.2_TOhEVTLPXQ6AYF9-T0XnGIEI_lTIu5NrTNsTuLL-MIJbGYGIkeLl_SiZw6l8BqYmcodUDoWCljQGFa1B5WWVKyeKDGv4zNehsWYV-FMxf_xcAbTXdzA8p4K8_vaRyRZSML_U4nJ_ciRkFpef8r7IaMPM-i1j4_4T2tnsSG7dA; T=af=JnRzPTE3NTI3MTYyODQmcHM9YXQuRjFlNXd1em5HX1FLZGRYOFZzdy0t&d=bnMBeWFob28BZwFERjIzWTdHRVlTRkpBNUlEVk01TEkzRTJIRQFhYwFBTTNBWGVhcAFhbAFzaWVfa2Vuc291OQFzYwFkZXNrdG9wX3dlYgFmcwE5M0tScGdkb0xQcm8BenoBOUlyYm9CWWZIAWEBUUFFAWxhdAFvclBMb0IBbnUBMA--&kt=EAAcDyDyNWcFX9AG9jhoR3bpg--~I&ku=FAAEBtguy4YHVD6G14qEhZoHrIvqyMR9YAp_NhHUF3cfujfDzJ1hYjxKtppi07t5DgYu3_Tsfk.8n2kAzdL0uOCxIkp.9bAAH6mejyMlqxDPI9QqQFDZ_XdUkrjNd3nPBP3V4fJb8Z_DPQk.fjG.hbJyhniALORlHAPKkdCQpYWvfY-~E; OTHD=g=6B560D8B3186D99EF9D587845D1D7FE9E47848EED254115AB8B0FD19F7984839&s=F8A93A0F090185C8A841823A3CA4CF611C76038D08853E0E90BB5A31E6D4933A&b=bid-3hukml1k2kdji&j=ph&bd=10ece65a5f01738215a295a3da0cbbf2&gk=x1a9z-qJCjBBcc&sk=x1a9z-qJCjBBcc&bk=x1a9z-qJCjBBcc&iv=A95C197AE1FBA14D2A6D7678F3708C93&v=1&u=0; _ga_B40QGCQW3G=GS2.1.s1752822385$o2$g1$t1752824094$j60$l0$h0; axids=gam=y-13JtCs9G2uLgoIRc7vWRJ81qPE5qsL6gFEwh5sbUcKAaLQMVlg---A&dv360=eS13emZvR1BwRTJ1RU83dmhzOGNMWDNBdVRxTlkyZzBqNFBBMW03WVN0TXV4S0dGUjJLWHQxT3V6X1N2VzBKUTVXeWFxMn5B&ydsp=y-CUx9MZ9E2uK9v56QOTpeOz5TxRXz72EQrAhqgdnxB4GSMpU13_s3x4lp2_VAXtSr4nIu~A&tbla=y-.FFAnWdG2uKWYTY4a13MXsqwoiB8c_Qv9dYFGqA94vIyEmR3rA---A; A1S=d=AQABBHI2KmgCEBGuM9CsvjLJg0ixmqiW-jgFEgEACALIW2iPaJkr0iMA_eMDAAcIcjYqaKiW-jgID-CO32qmSa2JTg5LC0nlsQkBBwoBMw&S=AQAAAuXFdrnQzCBtF2946U8Rbtk; PRF=ft%3DthemeSwitchEducation%26dock-collapsed%3Dtrue%26pf-lv%3Dv1%26t%3DALAB%252BCLF%252BVALE%252BIREN%252BRXRX%252BQBTS%252BRIOT%252BQS%252BLCID%252BRMBS%252BNBIS%252B005930.KS%252B2223.SR%252B601288.SS%252B%255EGSPC; cmp=t=1753159695&j=0&u=1YNN; _ga_C5QRNK12P6=GS2.1.s1753160380$o1$g1$t1753160434$j6$l0$h0; _ga_BFY40XXE01=GS2.1.s1753159833$o1$g1$t1753160434$j5$l0$h0; ySID=v=1&d=2sBSm_kZ3A--";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private static String crumb = "1TD9XXQrX6i";

    private final OkHttpClient client;

    public YahooFinanceQueryScreenerService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public static void main2(String[] args) {
        YahooFinanceQueryScreenerService yahooFinance = new YahooFinanceQueryScreenerService();
        
        try {
            // Step 1: Get fresh crumb
            String crumb = "1TD9XXQrX6i";//yahooFinance.getCrumb();
            System.out.println("Got crumb: " + crumb);
            
            // Step 2: Prepare payload
            String payload = createPayload();
            
            // Step 3: Make POST request
            String response = yahooFinance.postScreenerRequest(crumb, payload);
            
            // Step 4: Process response
            System.out.println("Response:");
            System.out.println(response);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    
    public static void main(String[] args) {
        YahooFinanceQueryScreenerService yahooFinance = new YahooFinanceQueryScreenerService();
        
        try {        	 
        	 List<String> tickers = yahooFinance.getDowJones();
             System.out.println("Tickers found:");
             tickers.forEach(System.out::println);
        	 
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    private String getCrumb() throws IOException {
        HttpUrl url = HttpUrl.parse(BASE_URL + "/v1/test/getcrumb");
        
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .header("Cookie", COOKIE)
                .build();
        
        // Store response in try-with-resources to ensure it gets closed
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response body");
            }
            
            String crumb = body.string();
            if (crumb.isEmpty()) {
                throw new IOException("Empty crumb received");
            }
            
            return crumb;
        }
    }
    
    public List<String> getDowJones(){
    	List<String> returnList = new ArrayList<String>();
    	
    	String dowJonesJsonRequestStr = YahooRequestQueryBuilder.createDowJonesPayload();
    	
    	try {
			String response = postScreenerRequest(crumb, dowJonesJsonRequestStr);
			
			returnList = YahooQueryTickerExtractorUtil.extractTickers(response);
			
		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return returnList;
    }
    
    public List<String> getNasDaq(){
    	List<String> returnList = new ArrayList<String>();
    	
    	String nasDaqJsonRequestStr = YahooRequestQueryBuilder.createNasDaqPayload();
    	
    	try {
			String response = postScreenerRequest(crumb, nasDaqJsonRequestStr);
			
			returnList = YahooQueryTickerExtractorUtil.extractTickers(response);
			
		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return returnList;
    }
    
    public List<String> getSP500(){
    	List<String> returnList = new ArrayList<String>();
    	
    	String sp500JsonRequestStr = YahooRequestQueryBuilder.createSP500Payload();
    	
    	try {
			String response = postScreenerRequest(crumb, sp500JsonRequestStr);
			
			returnList = YahooQueryTickerExtractorUtil.extractTickers(response);
			
		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return returnList;
    }
    
    public List<String> getRussel2k(){
    	List<String> returnList = new ArrayList<String>();
    	
    	String sp500JsonRequestStr = YahooRequestQueryBuilder.createRussel2kPayload();
    	
    	try {
			String response = postScreenerRequest(crumb, sp500JsonRequestStr);
			
			returnList = YahooQueryTickerExtractorUtil.extractTickers(response);
			
		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return returnList;
    }
    
    public List<String> getPennyBy3MonVol(){
    	List<String> returnList = new ArrayList<String>();
    	
    	String pennyBy3MonVolJsonRequestStr = YahooRequestQueryBuilder.createPennyBy3MonVolPayload();
    	
    	try {
			String response = postScreenerRequest(crumb, pennyBy3MonVolJsonRequestStr);
			
			returnList = YahooQueryTickerExtractorUtil.extractTickers(response);
			
		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return returnList;
    }
    
    public List<String> getPennyByPercentChange(){
    	List<String> returnList = new ArrayList<String>();
    	
    	String pennyByPercentChangeJsonRequestStr = YahooRequestQueryBuilder.createPennyByPercentChangePayload();
    	
    	try {
			String response = postScreenerRequest(crumb, pennyByPercentChangeJsonRequestStr);
			
			returnList = YahooQueryTickerExtractorUtil.extractTickers(response);
			
		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return returnList;
    }

    private String postScreenerRequest(String crumb, String payload) throws IOException {
        HttpUrl url = HttpUrl.parse(BASE_URL + "/v1/finance/screener")
                .newBuilder()
                .addQueryParameter("formatted", "true")
                .addQueryParameter("useRecordsResponse", "true")
                .addQueryParameter("lang", "en-US")
                .addQueryParameter("region", "US")
                .addQueryParameter("crumb", crumb)
                .addQueryParameter("x-crumb", crumb)
                .build();
        
        RequestBody body = RequestBody.create(payload, JSON);
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                //.header("Host", "query1.finance.yahoo.com")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36")
                //.header("Accept", "*/*")
                //.header("Accept-Language", "en-US,en;q=0.9")
                .header("Content-Type", "application/json")
                .header("Origin", "https://finance.yahoo.com")
                //.header("Referer", "https://finance.yahoo.com")
                .header("Cookie", COOKIE)
                .header("priority", "u=1, i")
                .header("x-crumb", crumb)
                .build();
        
        // Store response in try-with-resources to ensure it gets closed
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("Empty response body");
            }
            
            return responseBody.string();
        }
    }

    /*
    private static String createPayload() {
        return "{"
            + "\"size\":100,"
            + "\"offset\":0,"
            + "\"sortType\":\"asc\","
            + "\"sortField\":\"dayvolume\","
            + "\"includeFields\":[\"ticker\",\"companyshortname\",\"intradayprice\","
            + "\"intradaypricechange\",\"percentchange\",\"dayvolume\","
            + "\"avgdailyvol3m\",\"intradaymarketcap\",\"peratio.lasttwelvemonths\","
            + "\"day_open_price\",\"fiftytwowklow\",\"fiftytwowkhigh\","
            + "\"indices\",\"region\"],"
            + "\"topOperator\":\"AND\","
            + "\"quoteType\":\"EQUITY\","
            + "\"query\":{"
            +   "\"operator\":\"and\","
            +   "\"operands\":["
            +     "{"
            +       "\"operator\":\"or\","
            +       "\"operands\":["
            +         "{\"operator\":\"eq\",\"operands\":[\"region\",\"us\"]}"
            +       "]"
            +     "},"
            +     "{"
            +       "\"operator\":\"or\","
            +       "\"operands\":["
            +         "{\"operator\":\"btwn\",\"operands\":[\"intradaymarketcap\",2000000000,10000000000]},"
            +         "{\"operator\":\"btwn\",\"operands\":[\"intradaymarketcap\",10000000000,100000000000]},"
            +         "{\"operator\":\"gt\",\"operands\":[\"intradaymarketcap\",100000000000]}"
            +       "]"
            +     "},"
            +     "{"
            +       "\"operator\":\"or\","
            +       "\"operands\":["
            +         "{\"operator\":\"gt\",\"operands\":[\"dayvolume\",9000000]}"
            +       "]"
            +     "}"
            +   "]"
            + "}"
            + "}";
    }*/
    
    public static String createPayload() throws JSONException {
        // includeFields array
        JSONArray includeFields = new JSONArray()
                .put("ticker")
                .put("companyshortname")
                .put("intradayprice")
                .put("intradaypricechange")
                .put("percentchange")
                .put("dayvolume")
                .put("avgdailyvol3m")
                .put("intradaymarketcap")
                .put("peratio.lasttwelvemonths")
                .put("day_open_price")
                .put("fiftytwowklow")
                .put("fiftytwowkhigh")
                .put("indices")
                .put("region");

        // Region filter
        JSONObject regionFilter = new JSONObject()
                .put("operator", "eq")
                .put("operands", new JSONArray().put("region").put("us"));

        JSONObject regionGroup = new JSONObject()
                .put("operator", "or")
                .put("operands", new JSONArray().put(regionFilter));

        // Market cap filters
        JSONArray marketCapOperands = new JSONArray()
        	    .put(new JSONObject().put("operator", "btwn")
        	            .put("operands", new JSONArray().put("intradaymarketcap").put(2000000000L).put(10000000000L)))
        	    .put(new JSONObject().put("operator", "btwn")
        	            .put("operands", new JSONArray().put("intradaymarketcap").put(10000000000L).put(100000000000L)))
        	    .put(new JSONObject().put("operator", "gt")
        	            .put("operands", new JSONArray().put("intradaymarketcap").put(100000000000L)));

        JSONObject marketCapGroup = new JSONObject()
                .put("operator", "or")
                .put("operands", marketCapOperands);

        // Volume filter
        JSONObject volumeFilter = new JSONObject()
                .put("operator", "gt")
                .put("operands", new JSONArray().put("dayvolume").put(9000000));

        JSONObject volumeGroup = new JSONObject()
                .put("operator", "or")
                .put("operands", new JSONArray().put(volumeFilter));

        // Top-level query
        JSONArray mainOperands = new JSONArray()
                .put(regionGroup)
                .put(marketCapGroup)
                .put(volumeGroup);

        JSONObject query = new JSONObject()
                .put("operator", "and")
                .put("operands", mainOperands);

        // Final payload
        JSONObject payload = new JSONObject()
                .put("size", 100)
                .put("offset", 0)
                .put("sortType", "asc")
                .put("sortField", "dayvolume")
                .put("includeFields", includeFields)
                .put("topOperator", "AND")
                .put("quoteType", "EQUITY")
                .put("query", query);

        return payload.toString(4); // Pretty-printed JSON string
    }

    
	/*
	 * private static String createPayload() { try { return new JSONObject()
	 * .put("size", 100) .put("offset", 0) .put("sortType", "asc") .put("sortField",
	 * "dayvolume") .put("includeFields", new String[]{ "ticker",
	 * "companyshortname", "intradayprice", "intradaypricechange", "percentchange",
	 * "dayvolume" }) .put("topOperator", "AND") .put("quoteType", "EQUITY")
	 * .put("query", new JSONObject() .put("operator", "and") .put("operands", new
	 * JSONObject[]{ new JSONObject() .put("operator", "gt") .put("operands", new
	 * Object[]{"dayvolume", 10000000}) })) .toString(); } catch (JSONException e) {
	 * // TODO Auto-generated catch block e.printStackTrace(); } return null; }
	 */
}