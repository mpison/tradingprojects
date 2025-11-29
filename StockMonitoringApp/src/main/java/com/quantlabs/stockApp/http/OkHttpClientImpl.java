package com.quantlabs.stockApp.http;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantlabs.stockApp.exception.StockApiException;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OkHttpClientImpl implements ApiHttpClient {
    private static final String HTTPS_SCANNER_TRADINGVIEW_COM_SYMBOL = "https://scanner.tradingview.com/symbol";
	// Yahoo Finance constants
    private static final String YAHOO_BASE_URL = "https://query1.finance.yahoo.com";
    private static final String YAHOO_COOKIE = "tbla_id=b1e575b5-3983-49b1-be2c-666690b19e13-tuctf23bf6b; F=d=LTfYD5A9vJPBkgLe_6GBQux_9IZMRxUkRHo2nN2cNg--; PH=l=en-PH; Y=v=1&n=5rlmr5ed7g231&l=i84_a4diekz/o&p=m2lvvph00000000&iz=4114&r=ae&intl=ph; GUC=AQEACAJoW8hoj0Ig1gTF&s=AQAAAGLMw3Tk&g=aFqCJQ; A1=d=AQABBHI2KmgCEBGuM9CsvjLJg0ixmqiW-jgFEgEACALIW2iPaJkr0iMA_eMDAAcIcjYqaKiW-jgID-CO32qmSa2JTg5LC0nlsQkBBwoBMw&S=AQAAAuXFdrnQzCBtF2946U8Rbtk; A3=d=AQABBHI2KmgCEBGuM9CsvjLJg0ixmqiW-jgFEgEACALIW2iPaJkr0iMA_eMDAAcIcjYqaKiW-jgID-CO32qmSa2JTg5LC0nlsQkBBwoBMw&S=AQAAAuXFdrnQzCBtF2946U8Rbtk; fes-ds-navbadgestockpicks=1751857265846; _ga=GA1.1.47815853.1751400777; fes-ds-fes_1022_silver-evergreen-v2=1; fes-ds-fes_1004_silver-portfolios=1; fes-ds-1015_stockpicks-signedin-phase1=1; gpp=DBABBg~BVoIgACY.QA; gpp_sid=8; ucs=tr=1752802684000; OTH=v=2&s=0&d=eyJraWQiOiIwIiwiYWxnIjoiUlMyNTYifQ.eyJjdSI6eyJndWlkIjoiREYyM1k3R0VZU0ZKQTVJRFZNNUxJM0UySEUiLCJwZXJzaXN0ZW50Ijp0cnVlLCJzaWQiOiJkSDViWWhBNHpodlIifX0.2_TOhEVTLPXQ6AYF9-T0XnGIEI_lTIu5NrTNsTuLL-MIJbGYGIkeLl_SiZw6l8BqYmcodUDoWCljQGFa1B5WWVKyeKDGv4zNehsWYV-FMxf_xcAbTXdzA8p4K8_vaRyRZSML_U4nJ_ciRkFpef8r7IaMPM-i1j4_4T2tnsSG7dA; T=af=JnRzPTE3NTI3MTYyODQmcHM9YXQuRjFlNXd1em5HX1FLZGRYOFZzdy0t&d=bnMBeWFob28BZwFERjIzWTdHRVlTRkpBNUlEVk01TEkzRTJIRQFhYwFBTTNBWGVhcAFhbAFzaWVfa2Vuc291OQFzYwFkZXNrdG9wX3dlYgFmcwE5M0tScGdkb0xQcm8BenoBOUlyYm9CWWZIAWEBUUFFAWxhdAFvclBMb0IBbnUBMA--&kt=EAAcDyDyNWcFX9AG9jhoR3bpg--~I&ku=FAAEBtguy4YHVD6G14qEhZoHrIvqyMR9YAp_NhHUF3cfujfDzJ1hYjxKtppi07t5DgYu3_Tsfk.8n2kAzdL0uOCxIkp.9bAAH6mejyMlqxDPI9QqQFDZ_XdUkrjNd3nPBP3V4fJb8Z_DPQk.fjG.hbJyhniALORlHAPKkdCQpYWvfY-~E; OTHD=g=6B560D8B3186D99EF9D587845D1D7FE9E47848EED254115AB8B0FD19F7984839&s=F8A93A0F090185C8A841823A3CA4CF611C76038D08853E0E90BB5A31E6D4933A&b=bid-3hukml1k2kdji&j=ph&bd=10ece65a5f01738215a295a3da0cbbf2&gk=x1a9z-qJCjBBcc&sk=x1a9z-qJCjBBcc&bk=x1a9z-qJCjBBcc&iv=A95C197AE1FBA14D2A6D7678F3708C93&v=1&u=0; _ga_B40QGCQW3G=GS2.1.s1752822385$o2$g1$t1752824094$j60$l0$h0; axids=gam=y-13JtCs9G2uLgoIRc7vWRJ81qPE5qsL6gFEwh5sbUcKAaLQMVlg---A&dv360=eS13emZvR1BwRTJ1RU83dmhzOGNMWDNBdVRxTlkyZzBqNFBBMW03WVN0TXV4S0dGUjJLWHQxT3V6X1N2VzBKUTVXeWFxMn5B&ydsp=y-CUx9MZ9E2uK9v56QOTpeOz5TxRXz72EQrAhqgdnxB4GSMpU13_s3x4lp2_VAXtSr4nIu~A&tbla=y-.FFAnWdG2uKWYTY4a13MXsqwoiB8c_Qv9dYFGqA94vIyEmR3rA---A; A1S=d=AQABBHI2KmgCEBGuM9CsvjLJg0ixmqiW-jgFEgEACALIW2iPaJkr0iMA_eMDAAcIcjYqaKiW-jgID-CO32qmSa2JTg5LC0nlsQkBBwoBMw&S=AQAAAuXFdrnQzCBtF2946U8Rbtk; PRF=ft%3DthemeSwitchEducation%26dock-collapsed%3Dtrue%26pf-lv%3Dv1%26t%3DALAB%252BCLF%252BVALE%252BIREN%252BRXRX%252BQBTS%252BRIOT%252BQS%252BLCID%252BRMBS%252BNBIS%252B005930.KS%252B2223.SR%252B601288.SS%252B%255EGSPC; cmp=t=1753159695&j=0&u=1YNN; _ga_C5QRNK12P6=GS2.1.s1753160380$o1$g1$t1753160434$j6$l0$h0; _ga_BFY40XXE01=GS2.1.s1753159833$o1$g1$t1753160434$j5$l0$h0; ySID=v=1&d=2sBSm_kZ3A--";

    // TradingView constants
    private static final String TRADINGVIEW_BASE_URL = "http://scanner.tradingview.com/america/";
    
    // Common constants
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private final OkHttpClient client;

    public OkHttpClientImpl() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String executeYahooRequest(String payload, String crumb, String userAgent, String origin) 
            throws StockApiException {
        try {
            HttpUrl url = HttpUrl.parse(YAHOO_BASE_URL + "/v1/finance/screener")
                    .newBuilder()
                    //.addQueryParameter("formatted", "true")
                    //.addQueryParameter("useRecordsResponse", "true")
                    //.addQueryParameter("lang", "en-US")
                    //.addQueryParameter("region", "US")
                    .addQueryParameter("crumb", crumb)
                    .addQueryParameter("x-crumb", crumb)
                    .build();
            
            RequestBody body = RequestBody.create(payload, JSON);
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    //.header("User-Agent", userAgent)
                    //.header("Content-Type", "application/json")
                    .header("Origin", origin)
                    .header("Cookie", YAHOO_COOKIE)
                    .header("priority", "u=1, i")
                    .header("x-crumb", crumb)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new StockApiException("Yahoo API error: " + response.code() + " - " + response.message());
                }
                
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new StockApiException("Empty Yahoo API response");
                }
                
                return responseBody.string();
            }
        } catch (IOException e) {
            throw new StockApiException("Yahoo API request failed"+ e);
        }
    }

    @Override
    public String executeTradingViewRequest(String payload, String userAgent, String origin) 
            throws StockApiException {
        try {
            HttpUrl url = HttpUrl.parse(TRADINGVIEW_BASE_URL + "/scan");
            
            RequestBody body = RequestBody.create(payload, JSON);
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .header("User-Agent", userAgent)
                    .header("Content-Type", "application/json")
                    .header("Origin", origin)
                    .header("Accept", "application/json")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new StockApiException("TradingView API error: " + response.code() + " - " + response.message());
                }
                
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new StockApiException("Empty TradingView API response");
                }
                
                return responseBody.string();
            }
        } catch (IOException e) {
            throw new StockApiException("TradingView API request failed" + e);
        }
    }

    @Override
    public String getYahooCrumb() throws StockApiException {
        try {
            HttpUrl url = HttpUrl.parse(YAHOO_BASE_URL + "/v1/test/getcrumb");
            
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Cookie", YAHOO_COOKIE)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new StockApiException("Failed to get crumb: " + response.code());
                }
                
                ResponseBody body = response.body();
                if (body == null) {
                    throw new StockApiException("Empty crumb response");
                }
                
                String crumb = body.string();
                if (crumb.isEmpty()) {
                    throw new StockApiException("Received empty crumb");
                }
                
                return crumb;
            }
        } catch (IOException e) {
            throw new StockApiException("Crumb request failed"+ e);
        }
    }
    
    @Override
    public String executeTradingViewSymbolRequest(String payload, String userAgent, String origin) 
            throws StockApiException {
        try {
            HttpUrl url = HttpUrl.parse(HTTPS_SCANNER_TRADINGVIEW_COM_SYMBOL);
            
            // Parse the payload to extract symbol and fields
            ObjectMapper mapper = new ObjectMapper();
            JsonNode payloadNode = mapper.readTree(payload);
            
            String symbol = payloadNode.get("symbol").asText();
            JsonNode fieldsNode = payloadNode.get("fields");
            
            // Build query parameters
            HttpUrl.Builder urlBuilder = url.newBuilder()
                    .addQueryParameter("symbol", symbol);
            
            // Add fields as comma-separated list
            if (fieldsNode != null && fieldsNode.isArray()) {
                StringBuilder fieldsBuilder = new StringBuilder();
                for (JsonNode field : fieldsNode) {
                    if (fieldsBuilder.length() > 0) {
                        fieldsBuilder.append(",");
                    }
                    fieldsBuilder.append(field.asText());
                }
                urlBuilder.addQueryParameter("fields", fieldsBuilder.toString());
            }
            
            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .get() // This endpoint uses GET method
                    //.header("User-Agent", userAgent)
                    //.header("Origin", origin)
                    //.header("Accept", "application/json")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
            	ResponseBody responseBody = response.body();
            	
                if (!response.isSuccessful()) {
                    throw new StockApiException("TradingView Symbol API error: " + response.code() + " - " + response.message() + "responsebody" + responseBody.string());
                }
                
                
                if (responseBody == null) {
                    throw new StockApiException("Empty TradingView Symbol API response");
                }
                
                return responseBody.string();
            }
        } catch (IOException e) {
            throw new StockApiException("TradingView Symbol API request failed" + e);
        } catch (Exception e) {
            throw new StockApiException("Error processing symbol request: " + e.getMessage());
        }
    }
    
    @Override
    public String executeTradingViewSearchSymbolRequest(String urlStr, String userAgent, String origin) 
            throws StockApiException {
        try {
            HttpUrl url = HttpUrl.parse(urlStr);
           
            // Build query parameters
            HttpUrl.Builder urlBuilder = url.newBuilder();
                        
            Request request = new Request.Builder()
                    .url(urlBuilder.build())
                    .get() // This endpoint uses GET method
                    //.header("User-Agent", userAgent)
                    .header("Origin", "https://www.tradingview.com")
                    //.header("Accept", "application/json")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new StockApiException("TradingView Symbol API error: " + response.code() + " - " + response.message());
                }
                
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new StockApiException("Empty TradingView Symbol API response");
                }
                
                return responseBody.string();
            }
        } catch (IOException e) {
            throw new StockApiException("TradingView Symbol API request failed" + e);
        } catch (Exception e) {
            throw new StockApiException("Error processing symbol request: " + e.getMessage());
        }
    }
}