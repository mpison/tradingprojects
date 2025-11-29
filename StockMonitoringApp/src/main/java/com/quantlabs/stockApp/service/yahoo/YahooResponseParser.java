package com.quantlabs.stockApp.service.yahoo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.quantlabs.stockApp.model.PriceData;

public class YahooResponseParser {
	
	public static List<PriceData> parse(String json) throws JSONException {
	    List<PriceData> priceDataList = new ArrayList<>();
	    JSONObject root = new JSONObject(json);
	    JSONArray results = root.getJSONObject("finance").getJSONArray("result");

	    for (int i = 0; i < results.length(); i++) {
	        JSONObject result = results.getJSONObject(i);
	        boolean isNested = result.optBoolean("useRecords", false);
	        String arrayKey = isNested ? "records" : "quotes";
	        if (!result.has(arrayKey)) {
	            continue;
	        }
	        JSONArray dataArray = result.getJSONArray(arrayKey);
	        String tickerKey = isNested ? "ticker" : "symbol";
	        String nameKey = isNested ? "companyName" : "shortName";
	        String avgVolKey = isNested ? "avgDailyVol3m" : "averageDailyVolume3Month";

	        for (int j = 0; j < dataArray.length(); j++) {
	            JSONObject record = dataArray.getJSONObject(j);
	            if (record.has(tickerKey)) {
	                String ticker = record.getString(tickerKey);
	                
	                // Extract values using unified helpers
	                double latestPrice = getDoubleValue(record, "regularMarketPrice", 0);
	                long currentVolume = getLongValue(record, "regularMarketVolume", 0);
	                double percentChange = getDoubleValue(record, "regularMarketChangePercent", 0);
	                long averageVol = getLongValue(record, avgVolKey, 0);
	                double marketCap = getDoubleValue(record, "marketCap", 0);
	                
	                // Name with fallback
	                String name = record.optString(nameKey, "");
	                if (name.isEmpty()) {
	                    name = record.optString("companyshortname", "");
	                }
	                if (name.isEmpty()) {
	                    name = record.optString("longName", "");
	                }
	                
	                // Create builder with required fields
	                PriceData.Builder builder = new PriceData.Builder(ticker, latestPrice)
	                    .name(name)
	                    .latestPrice(latestPrice)
	                    .currentVolume(currentVolume)
	                    .percentChange(percentChange)
	                    .averageVol(averageVol)
	                    .prevLastDayPrice(getDoubleValue(record, "fiftyTwoWeekLow", 0))
	                    .marketCap(marketCap);

	                // Handle indices (map to indexes)
	                if (record.has("indices")) {
	                    JSONArray indicesArray = record.optJSONArray("indices");
	                    if (indicesArray != null) {
	                        List<Map<String, String>> indexes = new ArrayList<>();
	                        for (int k = 0; k < indicesArray.length(); k++) {
	                            String index = indicesArray.getString(k);
	                            indexes.add(Map.of("name", index));
	                        }
	                        builder.indexes(indexes);
	                    }
	                }

	                // Map additional optional fields
	                if (record.has("region")) {
	                    builder.description(record.optString("region", ""));
	                }
	                if (record.has("logoUrl")) {
	                    builder.logoid(record.optString("logoUrl", ""));
	                }
	                
	                // Handle optional numeric fields
	                builder.premarketClose(getDoubleValue(record, "fiftyTwoWeekHigh", 0));
	                builder.changeFromOpen(getDoubleValue(record, "regularMarketChange", 0));
	                
	                builder.currency(record.optString("currency", "USD"));

	                priceDataList.add(builder.build());
	            }
	        }
	    }
	    return priceDataList;
	}

	// Unified helper to get double (handles both flat and nested "raw")
	private static double getDoubleValue(JSONObject record, String key, double defaultValue) {
	    try {
	        Object obj = record.get(key);
	        if (obj instanceof Number) {
	            return ((Number) obj).doubleValue();
	        } else if (obj instanceof JSONObject) {
	            JSONObject nested = (JSONObject) obj;
	            if (nested.has("raw")) {
	                return nested.getDouble("raw");
	            }
	        }
	    } catch (JSONException e) {
	        // Fall through to default
	    }
	    return defaultValue;
	}

	// Unified helper to get long (handles both flat and nested "raw")
	private static long getLongValue(JSONObject record, String key, long defaultValue) {
	    try {
	        Object obj = record.get(key);
	        if (obj instanceof Number) {
	            return ((Number) obj).longValue();
	        } else if (obj instanceof JSONObject) {
	            JSONObject nested = (JSONObject) obj;
	            if (nested.has("raw")) {
	                return nested.getLong("raw");
	            }
	        }
	    } catch (JSONException e) {
	        // Fall through to default
	    }
	    return defaultValue;
	}
	
    public static List<PriceData> parse2(String json) throws JSONException {
        List<PriceData> priceDataList = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray results = root.getJSONObject("finance").getJSONArray("result");

        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.getJSONObject(i);
            JSONArray records = result.getJSONArray("records");

            for (int j = 0; j < records.length(); j++) {
                JSONObject record = records.getJSONObject(j);
                if (record.has("ticker")) {
                    String ticker = record.getString("ticker");
                    // Create builder with the ticker immediately
                    PriceData data = new PriceData.Builder(ticker, record.optDouble("intradayprice", 0))
                        .name(record.optString("companyshortname", ""))
                        .latestPrice(record.optDouble("intradayprice", 0))
                        .currentVolume(record.optLong("dayvolume", 0))
                        .percentChange(record.optDouble("percentchange", 0))
                        .averageVol(record.optLong("avgdailyvol3m", 0))
                        .prevLastDayPrice(record.optDouble("day_open_price", 0))
                        .marketCap(record.optDouble("intradaymarketcap", 0))
                        .build();

                    priceDataList.add(data);
                }
            }
        }
        return priceDataList;
    }
    
    public static List<String> extractTickers(String json) throws JSONException {
        List<String> tickers = new ArrayList<>();

        JSONObject root = new JSONObject(json);
        JSONArray results = root.getJSONObject("finance")
                                .getJSONArray("result");

        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.getJSONObject(i);
            boolean isNested = result.optBoolean("useRecords", false);
            String arrayKey = isNested ? "records" : "quotes";
            if (!result.has(arrayKey)) {
                continue;
            }
            JSONArray dataArray = result.getJSONArray(arrayKey);
            String tickerKey = isNested ? "ticker" : "symbol";

            for (int j = 0; j < dataArray.length(); j++) {
                JSONObject record = dataArray.getJSONObject(j);
                if (record.has(tickerKey)) {
                    tickers.add(record.getString(tickerKey));
                }
            }
        }

        return tickers;
    }
    
    public static void main(String[] args) throws JSONException{
    	String jsonInput = "{\"finance\":{\"result\":[{\"start\":0,\"count\":100,\"total\":496,\"quotes\":[{\"language\":\"en-US\",\"region\":\"US\",\"quoteType\":\"EQUITY\",\"typeDisp\":\"Equity\",\"quoteSourceName\":\"Delayed Quote\",\"triggerable\":false,\"customPriceAlertConfidence\":\"LOW\",\"priceHint\":2,\"exchange\":\"NYQ\",\"fiftyTwoWeekHigh\":630.73,\"fiftyTwoWeekLow\":234.6,\"averageAnalystRating\":\"2.0 - Buy\",\"dividendYield\":2.91,\"shortName\":\"UnitedHealth Group Incorporated\",\"currency\":\"USD\",\"bid\":304.79,\"ask\":305.5,\"earningsTimestamp\":1753792200,\"earningsTimestampStart\":1760531400,\"earningsTimestampEnd\":1760531400,\"earningsCallTimestampStart\":1753793100,\"earningsCallTimestampEnd\":1753793100,\"isEarningsDateEstimate\":false,\"trailingAnnualDividendRate\":8.51,\"trailingPE\":13.160606,\"dividendRate\":8.84,\"trailingAnnualDividendYield\":0.03134554,\"marketState\":\"CLOSED\",\"epsTrailingTwelveMonths\":23.1,\"epsForward\":29.9,\"epsCurrentYear\":16.23546,\"priceEpsCurrentYear\":18.725063,\"sharesOutstanding\":905673984,\"bookValue\":104.667,\"fiftyDayAverage\":288.6524,\"fiftyDayAverageChange\":15.357605,\"fiftyDayAverageChangePercent\":0.05320449,\"twoHundredDayAverage\":444.73206,\"twoHundredDayAverageChange\":-140.72205,\"twoHundredDayAverageChangePercent\":-0.31641984,\"marketCap\":275333971968,\"forwardPE\":10.167559,\"priceToBook\":2.904545,\"sourceInterval\":15,\"exchangeDataDelayedBy\":0,\"exchangeTimezoneName\":\"America/New_York\",\"exchangeTimezoneShortName\":\"EDT\",\"gmtOffSetMilliseconds\":-14400000,\"esgPopulated\":false,\"tradeable\":false,\"cryptoTradeable\":false,\"hasPrePostMarketData\":true,\"firstTradeDateMilliseconds\":466867800000,\"postMarketChangePercent\":0.4638017,\"postMarketTime\":1755302400,\"postMarketPrice\":305.42,\"postMarketChange\":1.4100037,\"regularMarketChange\":32.52,\"regularMarketTime\":1755288002,\"regularMarketPrice\":304.01,\"regularMarketDayHigh\":310.3,\"regularMarketDayRange\":\"294.71 - 310.3\",\"regularMarketDayLow\":294.71,\"regularMarketVolume\":67495749,\"regularMarketPreviousClose\":271.49,\"bidSize\":1,\"askSize\":1,\"market\":\"us_market\",\"messageBoardId\":\"finmb_104673\",\"fullExchangeName\":\"NYSE\",\"longName\":\"UnitedHealth Group Incorporated\",\"financialCurrency\":\"USD\",\"regularMarketOpen\":301.71,\"averageDailyVolume3Month\":17987330,\"averageDailyVolume10Day\":21215570,\"corporateActions\":[{\"header\":\"Dividend\",\"message\":\"UNH announced a cash dividend of 2.21 with an ex-date of Sep. 15, 2025\",\"meta\":{\"eventType\":\"DIVIDEND\",\"dateEpochMs\":1757908800000,\"amount\":\"2.21\"}}],\"fiftyTwoWeekLowChange\":69.41,\"fiftyTwoWeekLowChangePercent\":0.2958653,\"fiftyTwoWeekRange\":\"234.6 - 630.73\",\"fiftyTwoWeekHighChange\":-326.71997,\"fiftyTwoWeekHighChangePercent\":-0.5180029,\"fiftyTwoWeekChangePercent\":-47.473995,\"dividendDate\":1750723200,\"regularMarketChangePercent\":11.9783,\"symbol\":\"UNH\"},{\"language\":\"en-US\",\"region\":\"US\",\"quoteType\":\"EQUITY\",\"typeDisp\":\"Equity\",\"quoteSourceName\":\"Delayed Quote\",\"triggerable\":true,\"customPriceAlertConfidence\":\"HIGH\",\"priceHint\":2,\"exchange\":\"NMS\",\"fiftyTwoWeekHigh\":262.72,\"fiftyTwoWeekLow\":116.56,\"averageAnalystRating\":\"1.6 - Buy\",\"shortName\":\"First Solar, Inc.\",\"currency\":\"USD\",\"bid\":190.03,\"ask\":200.2,\"earningsTimestamp\":1753992000,\"earningsTimestampStart\":1761681600,\"earningsTimestampEnd\":1761681600,\"earningsCallTimestampStart\":1753993800,\"earningsCallTimestampEnd\":1753993800,\"isEarningsDateEstimate\":true,\"trailingAnnualDividendRate\":0.0,\"trailingPE\":17.089743,\"trailingAnnualDividendYield\":0.0,\"marketState\":\"CLOSED\",\"epsTrailingTwelveMonths\":11.7,\"epsForward\":20.86,\"epsCurrentYear\":15.42707,\"priceEpsCurrentYear\":12.960983,\"sharesOutstanding\":107248000,\"bookValue\":79.686,\"fiftyDayAverage\":171.2734,\"fiftyDayAverageChange\":28.67659,\"fiftyDayAverageChangePercent\":0.16743165,\"twoHundredDayAverage\":165.42854,\"twoHundredDayAverageChange\":34.521454,\"twoHundredDayAverageChangePercent\":0.20867895,\"marketCap\":21444237312,\"forwardPE\":9.58533,\"priceToBook\":2.5092237,\"sourceInterval\":15,\"exchangeDataDelayedBy\":0,\"exchangeTimezoneName\":\"America/New_York\",\"exchangeTimezoneShortName\":\"EDT\",\"gmtOffSetMilliseconds\":-14400000,\"esgPopulated\":false,\"tradeable\":false,\"cryptoTradeable\":false,\"hasPrePostMarketData\":true,\"firstTradeDateMilliseconds\":1163773800000,\"postMarketChangePercent\":0.9252344,\"postMarketTime\":1755302203,\"postMarketPrice\":201.8,\"postMarketChange\":1.8500061,\"regularMarketChange\":19.9,\"regularMarketTime\":1755288001,\"regularMarketPrice\":199.95,\"regularMarketDayHigh\":206.6,\"regularMarketDayRange\":\"179.06 - 206.6\",\"regularMarketDayLow\":179.06,\"regularMarketVolume\":10901799,\"regularMarketPreviousClose\":180.05,\"bidSize\":1,\"askSize\":1,\"market\":\"us_market\",\"messageBoardId\":\"finmb_4593731\",\"fullExchangeName\":\"NasdaqGS\",\"longName\":\"First Solar, Inc.\",\"financialCurrency\":\"USD\",\"regularMarketOpen\":182.0,\"averageDailyVolume3Month\":4116111,\"averageDailyVolume10Day\":3007480,\"corporateActions\":[],\"fiftyTwoWeekLowChange\":83.39,\"fiftyTwoWeekLowChangePercent\":0.71542555,\"fiftyTwoWeekRange\":\"116.56 - 262.72\",\"fiftyTwoWeekHighChange\":-62.770004,\"fiftyTwoWeekHighChangePercent\":-0.23892358,\"fiftyTwoWeekChangePercent\":-12.448549,\"regularMarketChangePercent\":11.0525,\"displayName\":\"First Solar\",\"symbol\":\"FSLR\"},{\"language\":\"en-US\",\"region\":\"US\",\"quoteType\":\"EQUITY\",\"typeDisp\":\"Equity\",\"quoteSourceName\":\"Delayed Quote\",\"triggerable\":false,\"customPriceAlertConfidence\":\"LOW\",\"priceHint\":2,\"exchange\":\"NGM\",\"fiftyTwoWeekHigh\":130.08,\"fiftyTwoWeekLow\":29.89,\"averageAnalystRating\":\"2.9 - Hold\",\"shortName\":\"Enphase Energy, Inc.\",\"currency\":\"USD\",\"bid\":34.74,\"ask\":34.9,\"earningsTimestamp\":1753214400,\"earningsTimestampStart\":1761076800,\"earningsTimestampEnd\":1761076800,\"earningsCallTimestampStart\":1753216200,\"earningsCallTimestampEnd\":1753216200,\"isEarningsDateEstimate\":true,\"trailingAnnualDividendRate\":0.0,\"trailingPE\":27.007753,\"trailingAnnualDividendYield\":0.0,\"marketState\":\"CLOSED\",\"epsTrailingTwelveMonths\":1.29,\"epsForward\":3.66,\"epsCurrentYear\":2.60902,\"priceEpsCurrentYear\":13.353673,\"sharesOutstanding\":130751000,\"bookValue\":6.736,\"fiftyDayAverage\":38.1942,\"fiftyDayAverageChange\":-3.3541985,\"fiftyDayAverageChangePercent\":-0.087819576,\"twoHundredDayAverage\":55.55385,\"twoHundredDayAverageChange\":-20.713848,\"twoHundredDayAverageChangePercent\":-0.37286073,\"marketCap\":4555364864,\"forwardPE\":9.519126,\"priceToBook\":5.172209,\"sourceInterval\":15,\"exchangeDataDelayedBy\":0,\"exchangeTimezoneName\":\"America/New_York\",\"exchangeTimezoneShortName\":\"EDT\",\"gmtOffSetMilliseconds\":-14400000,\"esgPopulated\":false,\"tradeable\":false,\"cryptoTradeable\":false,\"hasPrePostMarketData\":true,\"firstTradeDateMilliseconds\":1333114200000,\"postMarketChangePercent\":...(truncated 259787 characters)...rong Buy\",\"dividendYield\":3.45,\"shortName\":\"Hasbro, Inc.\",\"currency\":\"USD\",\"bid\":81.05,\"ask\":81.2,\"earningsTimestamp\":1753273800,\"earningsTimestampStart\":1761222600,\"earningsTimestampEnd\":1761222600,\"earningsCallTimestampStart\":1753273800,\"earningsCallTimestampEnd\":1753273800,\"isEarningsDateEstimate\":true,\"trailingAnnualDividendRate\":2.8,\"dividendRate\":2.8,\"trailingAnnualDividendYield\":0.034761015,\"marketState\":\"CLOSED\",\"epsTrailingTwelveMonths\":-4.07,\"epsForward\":4.43,\"epsCurrentYear\":4.85559,\"priceEpsCurrentYear\":16.710636,\"sharesOutstanding\":140232992,\"bookValue\":1.72,\"fiftyDayAverage\":74.4582,\"fiftyDayAverageChange\":6.681801,\"fiftyDayAverageChangePercent\":0.08973895,\"twoHundredDayAverage\":64.4976,\"twoHundredDayAverageChange\":16.642403,\"twoHundredDayAverageChangePercent\":0.25803137,\"marketCap\":11378504704,\"forwardPE\":18.316029,\"priceToBook\":47.174416,\"sourceInterval\":15,\"exchangeDataDelayedBy\":0,\"exchangeTimezoneName\":\"America/New_York\",\"exchangeTimezoneShortName\":\"EDT\",\"gmtOffSetMilliseconds\":-14400000,\"esgPopulated\":false,\"tradeable\":false,\"cryptoTradeable\":false,\"hasPrePostMarketData\":true,\"firstTradeDateMilliseconds\":322151400000,\"postMarketChangePercent\":1.0598972,\"postMarketTime\":1755302222,\"postMarketPrice\":82.0,\"postMarketChange\":0.8600006,\"regularMarketChange\":0.589996,\"regularMarketTime\":1755288001,\"regularMarketPrice\":81.14,\"regularMarketDayHigh\":81.31,\"regularMarketDayRange\":\"80.13 - 81.31\",\"regularMarketDayLow\":80.13,\"regularMarketVolume\":1470389,\"regularMarketPreviousClose\":80.55,\"bidSize\":9,\"askSize\":9,\"market\":\"us_market\",\"messageBoardId\":\"finmb_277746\",\"fullExchangeName\":\"NasdaqGS\",\"longName\":\"Hasbro, Inc.\",\"financialCurrency\":\"USD\",\"regularMarketOpen\":80.93,\"averageDailyVolume3Month\":2259853,\"averageDailyVolume10Day\":2087920,\"corporateActions\":[{\"header\":\"Dividend\",\"message\":\"HAS announced a cash dividend of 0.70 with an ex-date of Aug. 20, 2025\",\"meta\":{\"eventType\":\"DIVIDEND\",\"dateEpochMs\":1755662400000,\"amount\":\"0.70\"}}],\"fiftyTwoWeekLowChange\":32.14,\"fiftyTwoWeekLowChangePercent\":0.65591836,\"fiftyTwoWeekRange\":\"49.0 - 81.31\",\"fiftyTwoWeekHighChange\":-0.16999817,\"fiftyTwoWeekHighChangePercent\":-0.0020907412,\"fiftyTwoWeekChangePercent\":22.327757,\"dividendDate\":1756857600,\"regularMarketChangePercent\":0.73246,\"displayName\":\"Hasbro\",\"symbol\":\"HAS\"},{\"language\":\"en-US\",\"region\":\"US\",\"quoteType\":\"EQUITY\",\"typeDisp\":\"Equity\",\"quoteSourceName\":\"Delayed Quote\",\"triggerable\":false,\"customPriceAlertConfidence\":\"LOW\",\"priceHint\":2,\"exchange\":\"NYQ\",\"fiftyTwoWeekHigh\":369.99,\"fiftyTwoWeekLow\":256.6,\"averageAnalystRating\":\"2.0 - Buy\",\"dividendYield\":4.24,\"shortName\":\"Public Storage\",\"currency\":\"USD\",\"bid\":277.0,\"ask\":284.56,\"earningsTimestamp\":1753905600,\"earningsTimestampStart\":1761854400,\"earningsTimestampEnd\":1761854400,\"earningsCallTimestampStart\":1746115200,\"earningsCallTimestampEnd\":1746115200,\"isEarningsDateEstimate\":false,\"trailingAnnualDividendRate\":12.0,\"trailingPE\":30.854961,\"dividendRate\":12.0,\"trailingAnnualDividendYield\":0.042721346,\"marketState\":\"CLOSED\",\"epsTrailingTwelveMonths\":9.17,\"epsForward\":10.63,\"epsCurrentYear\":10.64346,\"priceEpsCurrentYear\":26.58346,\"sharesOutstanding\":175452992,\"bookValue\":28.626,\"fiftyDayAverage\":289.2896,\"fiftyDayAverageChange\":-6.3496094,\"fiftyDayAverageChangePercent\":-0.02194897,\"twoHundredDayAverage\":302.13934,\"twoHundredDayAverageChange\":-19.19934,\"twoHundredDayAverageChangePercent\":-0.06354465,\"marketCap\":49642668032,\"forwardPE\":26.61712,\"priceToBook\":9.884022,\"sourceInterval\":15,\"exchangeDataDelayedBy\":0,\"exchangeTimezoneName\":\"America/New_York\",\"exchangeTimezoneShortName\":\"EDT\",\"gmtOffSetMilliseconds\":-14400000,\"esgPopulated\":false,\"tradeable\":false,\"cryptoTradeable\":false,\"hasPrePostMarketData\":true,\"firstTradeDateMilliseconds\":343405800000,\"postMarketChangePercent\":0.09189572,\"postMarketTime\":1755302169,\"postMarketPrice\":283.2,\"postMarketChange\":0.26000977,\"regularMarketChange\":2.04999,\"regularMarketTime\":1755288002,\"regularMarketPrice\":282.94,\"regularMarketDayHigh\":284.35,\"regularMarketDayRange\":\"280.66 - 284.35\",\"regularMarketDayLow\":280.66,\"regularMarketVolume\":577241,\"regularMarketPreviousClose\":280.89,\"bidSize\":1,\"askSize\":2,\"market\":\"us_market\",\"messageBoardId\":\"finmb_305520\",\"fullExchangeName\":\"NYSE\",\"longName\":\"Public Storage\",\"financialCurrency\":\"USD\",\"regularMarketOpen\":280.66,\"averageDailyVolume3Month\":847887,\"averageDailyVolume10Day\":739960,\"corporateActions\":[],\"fiftyTwoWeekLowChange\":26.339996,\"fiftyTwoWeekLowChangePercent\":0.102650024,\"fiftyTwoWeekRange\":\"256.6 - 369.99\",\"fiftyTwoWeekHighChange\":-87.04999,\"fiftyTwoWeekHighChangePercent\":-0.23527661,\"fiftyTwoWeekChangePercent\":-12.424165,\"dividendDate\":1750982400,\"regularMarketChangePercent\":0.729819,\"symbol\":\"PSA\"}],\"useRecords\":false}],\"error\":null}}\r\n"
    			+ "";
    	
    	System.out.println("jsonInput="+jsonInput);
    	
    	List<PriceData> priceDataList = parse(jsonInput);
        
        System.out.println("Tickers found:");
        priceDataList.forEach(System.out::println);
    }

    public static void main3(String[] args) throws JSONException {
        String jsonInput = "{\r\n"
        		+ "    \"finance\": {\r\n"
        		+ "        \"result\": [\r\n"
        		+ "            {\r\n"
        		+ "                \"start\": 0,\r\n"
        		+ "                \"count\": 3,\r\n"
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
        		+ "                        \"ticker\": \"IONQ\",\r\n"
        		+ "                        \"indices\": [\r\n"
        		+ "                            \"^RUT\"\r\n"
        		+ "                        ],\r\n"
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
        		+ "                        \"regularMarketPrice\": {\r\n"
        		+ "                            \"raw\": 40.23,\r\n"
        		+ "                            \"fmt\": \"40.23\"\r\n"
        		+ "                        },\r\n"
        		+ "                        \"fiftyTwoWeekLow\": {\r\n"
        		+ "                            \"raw\": 6.54,\r\n"
        		+ "                            \"fmt\": \"6.54\"\r\n"
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
        		+ "                        \"ticker\": \"PDD\",\r\n"
        		+ "                        \"indices\": [\r\n"
        		+ "                            \"^NDX\"\r\n"
        		+ "                        ],\r\n"
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
        		+ "                        \"regularMarketPrice\": {\r\n"
        		+ "                            \"raw\": 118.95,\r\n"
        		+ "                            \"fmt\": \"118.95\"\r\n"
        		+ "                        },\r\n"
        		+ "                        \"fiftyTwoWeekLow\": {\r\n"
        		+ "                            \"raw\": 87.11,\r\n"
        		+ "                            \"fmt\": \"87.11\"\r\n"
        		+ "                        }\r\n"
        		+ "                    },\r\n"
        		+ "                    {\r\n"
        		+ "                        \"regularMarketVolume\": {\r\n"
        		+ "                            \"raw\": 9186974,\r\n"
        		+ "                            \"fmt\": \"9.187M\",\r\n"
        		+ "                            \"longFmt\": \"9,186,974\"\r\n"
        		+ "                        },\r\n"
        		+ "                        \"marketCap\": {\r\n"
        		+ "                            \"raw\": 1.6208086999145508E10,\r\n"
        		+ "                            \"fmt\": \"16.208B\",\r\n"
        		+ "                            \"longFmt\": \"16,208,086,999\"\r\n"
        		+ "                        },\r\n"
        		+ "                        \"ticker\": \"TWLO\",\r\n"
        		+ "                        \"avgDailyVol3m\": {\r\n"
        		+ "                            \"raw\": 2979730.64516129,\r\n"
        		+ "                            \"fmt\": \"2.98M\",\r\n"
        		+ "                            \"longFmt\": \"2,979,730\"\r\n"
        		+ "                        },\r\n"
        		+ "                        \"regularMarketChange\": {\r\n"
        		+ "                            \"raw\": 4.83,\r\n"
        		+ "                            \"fmt\": \"4.83\"\r\n"
        		+ "                        },\r\n"
        		+ "                        \"companyName\": \"Twilio Inc.\",\r\n"
        		+ "                        \"regularMarketChangePercent\": {\r\n"
        		+ "                            \"raw\": 4.78977,\r\n"
        		+ "                            \"fmt\": \"4.79%\"\r\n"
        		+ "                        },\r\n"
        		+ "                        \"region\": \"us\",\r\n"
        		+ "                        \"fiftyTwoWeekHigh\": {\r\n"
        		+ "                            \"raw\": 151.95,\r\n"
        		+ "                            \"fmt\": \"151.95\"\r\n"
        		+ "                        },\r\n"
        		+ "                        \"logoUrl\": \"https://s.yimg.com/lb/brands/150x150_twilio.png\",\r\n"
        		+ "                        \"regularMarketPrice\": {\r\n"
        		+ "                            \"raw\": 105.67,\r\n"
        		+ "                            \"fmt\": \"105.67\"\r\n"
        		+ "                        },\r\n"
        		+ "                        \"fiftyTwoWeekLow\": {\r\n"
        		+ "                            \"raw\": 56.85,\r\n"
        		+ "                            \"fmt\": \"56.85\"\r\n"
        		+ "                        }\r\n"
        		+ "                    }\r\n"
        		+ "                ],\r\n"
        		+ "                \"userHasReadRecord\": false,\r\n"
        		+ "                \"useRecords\": true\r\n"
        		+ "            }\r\n"
        		+ "        ],\r\n"
        		+ "        \"error\": null\r\n"
        		+ "    }\r\n"
        		+ "}"; // Load your JSON string here
        List<String> tickers = extractTickers(jsonInput);
        System.out.println("Tickers found:");
        tickers.forEach(System.out::println);
        
        List<PriceData> priceDataList = parse(jsonInput);
        
        System.out.println("Tickers found:");
        priceDataList.forEach(System.out::println);
        
    }
}