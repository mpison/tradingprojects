package com.quantlabs.stockApp.service.yahoo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class YahooRequestBuilder {
    public String buildDowJonesRequest() {
        return createPayload("^DJI");
    }

    public String buildNasdaqRequest() {
        return createPayload("^NDX");
    }

    public String buildSP500Request() {
        return createPayload("^SPX");
    }

    public String buildRussell2000Request() {
        return createPayload("^RUT");
    }

    public String buildPennyStocksByVolumeRequest() {
        return createPennyPayload(true);
    }

    public String buildPennyStocksByPercentChangeRequest() {
        return createPennyPayload(false);
    }

    private String createPayload(String index) {
        try {
            JSONArray includeFields = new JSONArray()
                .put("ticker").put("companyshortname").put("intradayprice")
                .put("intradaypricechange").put("percentchange").put("dayvolume")
                .put("avgdailyvol3m").put("intradaymarketcap").put("peratio.lasttwelvemonths")
                .put("day_open_price").put("fiftytwowklow").put("fiftytwowkhigh")
                .put("indices").put("region");

            JSONArray operands = new JSONArray()
                .put(createFilter("region", "eq", "us"))
                .put(createMarketCapFilter())
                .put(createFilter("intradayprice", "gt", 10))
                .put(createFilter("dayvolume", "gt", 15000))
                .put(createFilter("indices", "eq", index));

            return buildFinalPayload(includeFields, operands);
        } catch (JSONException e) {
            throw new RuntimeException("Error building Yahoo request", e);
        }
    }

    private String createPennyPayload(boolean byVolume) {
        try {
            JSONArray includeFields = new JSONArray()
                .put("ticker").put("companyshortname").put("intradayprice")
                .put("intradaypricechange").put("percentchange").put("dayvolume")
                .put("avgdailyvol3m").put("intradaymarketcap").put("peratio.lasttwelvemonths")
                .put("day_open_price").put("fiftytwowklow").put("fiftytwowkhigh")
                .put("indices").put("region");

            JSONArray operands = new JSONArray()
                .put(createFilter("region", "eq", "us"))
                .put(createFilter("intradayprice", "lt", 50))
                .put(createFilter("percentchange", "gt", byVolume ? 3 : 1))
                .put(createFilter("dayvolume", "gt", 500000))
                .put(createFilter("avgdailyvol3m", "gt", 500000));

            JSONObject query = new JSONObject()
                .put("operator", "and")
                .put("operands", operands);

            JSONObject payload = new JSONObject()
                .put("size", byVolume ? 200 : 100)
                .put("offset", 0)
                .put("sortType", "desc")
                .put("sortField", byVolume ? "avgdailyvol3m" : "percentchange")
                .put("includeFields", includeFields)
                .put("topOperator", "AND")
                .put("quoteType", "EQUITY")
                .put("query", query);

            return payload.toString(4);
        } catch (JSONException e) {
            throw new RuntimeException("Error building Yahoo penny stocks request", e);
        }
    }

    private JSONObject createFilter(String field, String operator, Object value) throws JSONException {
        return new JSONObject()
            .put("operator", "or")
            .put("operands", new JSONArray()
                .put(new JSONObject()
                    .put("operator", operator)
                    .put("operands", new JSONArray()
                        .put(field)
                        .put(value))));
    }

    private JSONObject createMarketCapFilter() throws JSONException {
        JSONArray marketCapFilters = new JSONArray()
            .put(new JSONObject()
                .put("operator", "btwn")
                .put("operands", new JSONArray()
                    .put("intradaymarketcap")
                    .put(2000000000L)
                    .put(10000000000L)))
            .put(new JSONObject()
                .put("operator", "btwn")
                .put("operands", new JSONArray()
                    .put("intradaymarketcap")
                    .put(10000000000L)
                    .put(100000000000L)))
            .put(new JSONObject()
                .put("operator", "gt")
                .put("operands", new JSONArray()
                    .put("intradaymarketcap")
                    .put(100000000000L)));

        return new JSONObject()
            .put("operator", "or")
            .put("operands", marketCapFilters);
    }

    private String buildFinalPayload(JSONArray includeFields, JSONArray operands) throws JSONException {
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

        return payload.toString(4);
    }
}