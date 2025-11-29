package com.quantlabs.stockApp.yahoo.query;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class YahooRequestQueryBuilder {

    public static String createDowJonesPayload() {
        // Fields to include
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

        // Construct query operands
        JSONArray operands = new JSONArray();

        // 1. percentchange > 0
        try {
			/*operands.put(new JSONObject().put("operator", "or")
			        .put("operands", new JSONArray()
			                .put(new JSONObject().put("operator", "gt")
			                        .put("operands", new JSONArray().put("percentchange").put(0)))));*/
		
        // 2. region == "us"
        operands.put(new JSONObject().put("operator", "or")
                .put("operands", new JSONArray()
                        .put(new JSONObject().put("operator", "eq")
                                .put("operands", new JSONArray().put("region").put("us")))));

        // 3. intradaymarketcap between / gt
        JSONArray marketCapFilters = new JSONArray()
                .put(new JSONObject().put("operator", "btwn")
                        .put("operands", new JSONArray().put("intradaymarketcap").put(2000000000L).put(10000000000L)))
                .put(new JSONObject().put("operator", "btwn")
                        .put("operands", new JSONArray().put("intradaymarketcap").put(10000000000L).put(100000000000L)))
                .put(new JSONObject().put("operator", "gt")
                        .put("operands", new JSONArray().put("intradaymarketcap").put(100000000000L)));

        operands.put(new JSONObject().put("operator", "or").put("operands", marketCapFilters));

        // 4. intradayprice > 10
        operands.put(new JSONObject().put("operator", "or")
                .put("operands", new JSONArray()
                        .put(new JSONObject().put("operator", "gt")
                                .put("operands", new JSONArray().put("intradayprice").put(10)))));

        // 5. dayvolume > 15000
        operands.put(new JSONObject().put("operator", "or")
                .put("operands", new JSONArray()
                        .put(new JSONObject().put("operator", "gt")
                                .put("operands", new JSONArray().put("dayvolume").put(15000)))));

        // 6. indices == "^DJI"
        operands.put(new JSONObject().put("operator", "or")
                .put("operands", new JSONArray()
                        .put(new JSONObject().put("operator", "eq")
                                .put("operands", new JSONArray().put("indices").put("^DJI")))));

        // Query structure
        JSONObject query = new JSONObject()
                .put("operator", "and")
                .put("operands", operands);

        // Final payload
        JSONObject payload = new JSONObject()
                .put("size", 100)
                .put("offset", 0)
                .put("sortType", "desc")
                .put("sortField", "percentchange")
                .put("includeFields", includeFields)
                .put("topOperator", "AND")
                .put("query", query)
                .put("quoteType", "EQUITY");
        
        	return payload.toString(4); // Pretty print for readability
        
        } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}

        
    }
    
    public static String createNasDaqPayload() {
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

        // Operand groups
        JSONArray operands;
		try {
			operands = new JSONArray()
			    // percentchange > 0
			    .put(new JSONObject()
			        .put("operator", "or")
			        .put("operands", new JSONArray()
			            .put(new JSONObject()
			                .put("operator", "gt")
			                .put("operands", new JSONArray().put("percentchange").put(0))
			            )
			        )
			    )
			    // region == "us"
			    .put(new JSONObject()
			        .put("operator", "or")
			        .put("operands", new JSONArray()
			            .put(new JSONObject()
			                .put("operator", "eq")
			                .put("operands", new JSONArray().put("region").put("us"))
			            )
			        )
			    )
			    // intradaymarketcap in ranges
			    .put(new JSONObject()
			        .put("operator", "or")
			        .put("operands", new JSONArray()
			            .put(new JSONObject().put("operator", "btwn")
			                .put("operands", new JSONArray().put("intradaymarketcap").put(2_000_000_000L).put(10_000_000_000L)))
			            .put(new JSONObject().put("operator", "btwn")
			                .put("operands", new JSONArray().put("intradaymarketcap").put(10_000_000_000L).put(100_000_000_000L)))
			            .put(new JSONObject().put("operator", "gt")
			                .put("operands", new JSONArray().put("intradaymarketcap").put(100_000_000_000L)))
			        )
			    )
			    // intradayprice > 10
			    .put(new JSONObject()
			        .put("operator", "or")
			        .put("operands", new JSONArray()
			            .put(new JSONObject()
			                .put("operator", "gt")
			                .put("operands", new JSONArray().put("intradayprice").put(10))
			            )
			        )
			    )
			    // dayvolume > 15000
			    .put(new JSONObject()
			        .put("operator", "or")
			        .put("operands", new JSONArray()
			            .put(new JSONObject()
			                .put("operator", "gt")
			                .put("operands", new JSONArray().put("dayvolume").put(15000))
			            )
			        )
			    )
			    // indices == ^NDX
			    .put(new JSONObject()
			        .put("operator", "or")
			        .put("operands", new JSONArray()
			            .put(new JSONObject()
			                .put("operator", "eq")
			                .put("operands", new JSONArray().put("indices").put("^NDX"))
			            )
			        )
			    );
			
			JSONObject query = new JSONObject()
		            .put("operator", "and")
		            .put("operands", operands);

		        JSONObject payload = new JSONObject()
		            .put("size", 100)
		            .put("offset", 0)
		            .put("sortType", "desc")
		            .put("sortField", "percentchange")
		            .put("includeFields", includeFields)
		            .put("topOperator", "AND")
		            .put("quoteType", "EQUITY")
		            .put("query", query);

		        return payload.toString(4); // Pretty print with 4 spaces
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";

        
    }
    
    public static String createSP500Payload() {
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

        // Operand groups
        JSONArray operands;
		try {			

		        operands = new JSONArray()
		            // percentchange > 0
		            .put(new JSONObject().put("operator", "or")
		                .put("operands", new JSONArray().put(
		                    new JSONObject().put("operator", "gt")
		                        .put("operands", new JSONArray().put("percentchange").put(0))
		                ))
		            )
		            // region == "us"
		            .put(new JSONObject().put("operator", "or")
		                .put("operands", new JSONArray().put(
		                    new JSONObject().put("operator", "eq")
		                        .put("operands", new JSONArray().put("region").put("us"))
		                ))
		            )
		            // intradaymarketcap in 3 brackets
		            .put(new JSONObject().put("operator", "or")
		                .put("operands", new JSONArray()
		                    .put(new JSONObject().put("operator", "btwn")
		                        .put("operands", new JSONArray().put("intradaymarketcap").put(2_000_000_000L).put(10_000_000_000L)))
		                    .put(new JSONObject().put("operator", "btwn")
		                        .put("operands", new JSONArray().put("intradaymarketcap").put(10_000_000_000L).put(100_000_000_000L)))
		                    .put(new JSONObject().put("operator", "gt")
		                        .put("operands", new JSONArray().put("intradaymarketcap").put(100_000_000_000L)))
		                )
		            )
		            // intradayprice >= 5
		            .put(new JSONObject().put("operator", "or")
		                .put("operands", new JSONArray().put(
		                    new JSONObject().put("operator", "gte")
		                        .put("operands", new JSONArray().put("intradayprice").put(5))
		                ))
		            )
		            // dayvolume > 15000
		            .put(new JSONObject().put("operator", "or")
		                .put("operands", new JSONArray().put(
		                    new JSONObject().put("operator", "gt")
		                        .put("operands", new JSONArray().put("dayvolume").put(15000))
		                ))
		            )
		            // indices == ^SPX
		            .put(new JSONObject().put("operator", "or")
		                .put("operands", new JSONArray().put(
		                    new JSONObject().put("operator", "eq")
		                        .put("operands", new JSONArray().put("indices").put("^SPX"))
		                ))
		            )
		            // avgdailyvol3m > 1,000,000
		            .put(new JSONObject().put("operator", "or")
		                .put("operands", new JSONArray().put(
		                    new JSONObject().put("operator", "gt")
		                        .put("operands", new JSONArray().put("avgdailyvol3m").put(500_000))
		                ))
		            );

		        JSONObject query = new JSONObject()
		            .put("operator", "and")
		            .put("operands", operands);

		        JSONObject payload = new JSONObject()
		            .put("size", 100)
		            .put("offset", 0)
		            .put("sortType", "desc")
		            .put("sortField", "percentchange")
		            .put("includeFields", includeFields)
		            .put("topOperator", "AND")
		            .put("quoteType", "EQUITY")
		            .put("query", query);

		        return payload.toString(4); // Pretty-printed output
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "";        
    }
    
    
    public static String createRussel2kPayload() {
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

        // Operand groups
        JSONArray operands;
		try {			

		        operands = new JSONArray()
		            // percentchange > 0
		            .put(new JSONObject().put("operator", "or")
		                .put("operands", new JSONArray().put(
		                    new JSONObject().put("operator", "gt")
		                        .put("operands", new JSONArray().put("percentchange").put(0))
		                ))
		            )
		            // region == "us"
		            .put(new JSONObject().put("operator", "or")
		                .put("operands", new JSONArray().put(
		                    new JSONObject().put("operator", "eq")
		                        .put("operands", new JSONArray().put("region").put("us"))
		                ))
		            )
		            // intradaymarketcap in 3 brackets
		            .put(new JSONObject().put("operator", "or")
		                .put("operands", new JSONArray()
		                    .put(new JSONObject().put("operator", "btwn")
		                        .put("operands", new JSONArray().put("intradaymarketcap").put(2_000_000_000L).put(10_000_000_000L)))
		                    .put(new JSONObject().put("operator", "btwn")
		                        .put("operands", new JSONArray().put("intradaymarketcap").put(10_000_000_000L).put(100_000_000_000L)))
		                    .put(new JSONObject().put("operator", "gt")
		                        .put("operands", new JSONArray().put("intradaymarketcap").put(100_000_000_000L)))
		                )
		            )
		            // intradayprice >= 5
		            .put(new JSONObject().put("operator", "or")
		                .put("operands", new JSONArray().put(
		                    new JSONObject().put("operator", "gte")
		                        .put("operands", new JSONArray().put("intradayprice").put(5))
		                ))
		            )
		            // dayvolume > 15000
		            .put(new JSONObject().put("operator", "or")
		                .put("operands", new JSONArray().put(
		                    new JSONObject().put("operator", "gt")
		                        .put("operands", new JSONArray().put("dayvolume").put(15000))
		                ))
		            )
		            // indices == ^RUT
		            .put(new JSONObject().put("operator", "or")
		                .put("operands", new JSONArray().put(
		                    new JSONObject().put("operator", "eq")
		                        .put("operands", new JSONArray().put("indices").put("^RUT"))
		                ))
		            )
		            // avgdailyvol3m > 1,000,000
		            .put(new JSONObject().put("operator", "or")
		                .put("operands", new JSONArray().put(
		                    new JSONObject().put("operator", "gt")
		                        .put("operands", new JSONArray().put("avgdailyvol3m").put(500_000))
		                ))
		            );

		        JSONObject query = new JSONObject()
		            .put("operator", "and")
		            .put("operands", operands);

		        JSONObject payload = new JSONObject()
		            .put("size", 100)
		            .put("offset", 0)
		            .put("sortType", "desc")
		            .put("sortField", "percentchange")
		            .put("includeFields", includeFields)
		            .put("topOperator", "AND")
		            .put("quoteType", "EQUITY")
		            .put("query", query);

		        return payload.toString(4); // Pretty-printed output
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "";        
    }
    
    public static String createPennyBy3MonVolPayload() {
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

        JSONArray operands;
		try {
			operands = new JSONArray()
			    // region == "us"
			    .put(new JSONObject().put("operator", "or")
			        .put("operands", new JSONArray().put(
			            new JSONObject().put("operator", "eq")
			                .put("operands", new JSONArray().put("region").put("us"))
			        ))
			    )
			    // intradayprice < 50
			    .put(new JSONObject().put("operator", "or")
			        .put("operands", new JSONArray().put(
			            new JSONObject().put("operator", "lt")
			                .put("operands", new JSONArray().put("intradayprice").put(50))
			        ))
			    )
			    // percentchange > 1
			    .put(new JSONObject().put("operator", "or")
			        .put("operands", new JSONArray().put(
			            new JSONObject().put("operator", "gt")
			                .put("operands", new JSONArray().put("percentchange").put(3))
			        ))
			    )
			    // dayvolume > 1,000,000
			    .put(new JSONObject().put("operator", "or")
			        .put("operands", new JSONArray().put(
			            new JSONObject().put("operator", "gt")
			                .put("operands", new JSONArray().put("dayvolume").put(500_000))
			        ))
			    )
			    // avgdailyvol3m > 1,000,000
			    .put(new JSONObject().put("operator", "or")
			        .put("operands", new JSONArray().put(
			            new JSONObject().put("operator", "gt")
			                .put("operands", new JSONArray().put("avgdailyvol3m").put(500_000))
			        ))
			    );
			JSONObject query = new JSONObject()
		            .put("operator", "and")
		            .put("operands", operands);

		        JSONObject payload = new JSONObject()
		            .put("size", 200)
		            .put("offset", 0)
		            .put("sortType", "desc")
		            .put("sortField", "avgdailyvol3m")
		            .put("includeFields", includeFields)
		            .put("topOperator", "AND")
		            .put("quoteType", "EQUITY")
		            .put("query", query);

		        return payload.toString(4); // pretty-printed JSON string
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";

        
    }
    public static String createPennyByPercentChangePayload() {
	 // ✅ includeFields array
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
	
	    // ✅ query operands
	    JSONArray operands;
		try {
			operands = new JSONArray()
			    // region == "us"
			    .put(new JSONObject()
			        .put("operator", "or")
			        .put("operands", new JSONArray()
			            .put(new JSONObject()
			                .put("operator", "eq")
			                .put("operands", new JSONArray().put("region").put("us"))
			            )
			        )
			    )
			    // intradayprice < 20
			    .put(new JSONObject()
			        .put("operator", "or")
			        .put("operands", new JSONArray()
			            .put(new JSONObject()
			                .put("operator", "lt")
			                .put("operands", new JSONArray().put("intradayprice").put(50))
			            )
			        )
			    )
			    // percentchange > 3
			    .put(new JSONObject()
			        .put("operator", "or")
			        .put("operands", new JSONArray()
			            .put(new JSONObject()
			                .put("operator", "gt")
			                .put("operands", new JSONArray().put("percentchange").put(3))
			            )
			        )
			    )
			    // dayvolume > 1,000,000
			    .put(new JSONObject()
			        .put("operator", "or")
			        .put("operands", new JSONArray()
			            .put(new JSONObject()
			                .put("operator", "gt")
			                .put("operands", new JSONArray().put("dayvolume").put(500_000))
			            )
			        )
			    );
			
			// ✅ query object
		    JSONObject query = new JSONObject()
		        .put("operator", "and")
		        .put("operands", operands);
		
		    // ✅ final payload
		    JSONObject payload = new JSONObject()
		        .put("size", 100)
		        .put("offset", 0)
		        .put("sortType", "desc")
		        .put("sortField", "percentchange")
		        .put("includeFields", includeFields)
		        .put("topOperator", "AND")
		        .put("quoteType", "EQUITY")
		        .put("query", query);
		
		    return payload.toString(4); // pretty print with indentation
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	
	    
	}
    

    public static String createPennyByPercentChangePayload2() {
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

        JSONArray operands;
		try {
			operands = new JSONArray()
			    // region == "us"
			    .put(new JSONObject().put("operator", "or")
			        .put("operands", new JSONArray().put(
			            new JSONObject().put("operator", "eq")
			                .put("operands", new JSONArray().put("region").put("us"))
			        ))
			    )
			    // intradayprice < 50
			    .put(new JSONObject().put("operator", "or")
			        .put("operands", new JSONArray().put(
			            new JSONObject().put("operator", "lt")
			                .put("operands", new JSONArray().put("intradayprice").put(50))
			        ))
			    )
			    // percentchange > 1
			    .put(new JSONObject().put("operator", "or")
			        .put("operands", new JSONArray().put(
			            new JSONObject().put("operator", "gt")
			                .put("operands", new JSONArray().put("percentchange").put(1))
			        ))
			    )
			    // dayvolume > 1,000,000
			    .put(new JSONObject().put("operator", "or")
			        .put("operands", new JSONArray().put(
			            new JSONObject().put("operator", "gt")
			                .put("operands", new JSONArray().put("dayvolume").put(500_000))
			        ))
			    )
			    // avgdailyvol3m > 1,000,000
			    .put(new JSONObject().put("operator", "or")
			        .put("operands", new JSONArray().put(
			            new JSONObject().put("operator", "gt")
			                .put("operands", new JSONArray().put("avgdailyvol3m").put(500_000))
			        ))
			    );
			JSONObject query = new JSONObject()
		            .put("operator", "and")
		            .put("operands", operands);

		        JSONObject payload = new JSONObject()
		            .put("size", 200)
		            .put("offset", 0)
		            .put("sortType", "desc")
		            .put("sortField", "percentchange")
		            .put("includeFields", includeFields)
		            .put("topOperator", "AND")
		            .put("quoteType", "EQUITY")
		            .put("query", query);

		        return payload.toString(4); // pretty-printed JSON string
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";

        
    }


    public static void main(String[] args) throws JSONException {
        System.out.println(createDowJonesPayload());
    }
}
