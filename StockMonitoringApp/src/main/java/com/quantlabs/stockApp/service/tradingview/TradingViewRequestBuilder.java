package com.quantlabs.stockApp.service.tradingview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quantlabs.stockApp.service.StockRequestBuilder;

public class TradingViewRequestBuilder extends StockRequestBuilder {
    static final String[] PRE_MARKET_COLUMNS = {
    		"name",
    	    "description",
    	    "logoid",
    	    "update_mode",
    	    "type",
    	    "typespecs",
    	    "premarket_close",
    	    "pricescale",
    	    "minmov",
    	    "fractional",
    	    "minmove2",
    	    "currency",
    	    "close",
    	    "postmarket_close",
    	    "premarket_high",
    	    "high",
    	    "postmarket_high",
    	    "premarket_change",
    	    "change",
    	    "change_from_open",
    	    "postmarket_change",
    	    "premarket_low",
    	    "low",
    	    "postmarket_low",
    	    "premarket_volume",
    	    "volume",
    	    "postmarket_volume",
    	    "market_cap_basic",
    	    "fundamental_currency_code",
    	    "sector.tr",
    	    "market",
    	    "sector",
    	    "AnalystRating",
    	    "AnalystRating.tr",
    	    "average_volume_10d_calc",
    	    "exchange",
    	    "indexes.tr",
    	    "gap"
    };

    public String buildPennyStocksPreMarketRequest() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode payload = mapper.createObjectNode();

            // Add fields in exact order
            ArrayNode columns = mapper.createArrayNode();
            for (String column : PRE_MARKET_COLUMNS) {
                columns.add(column);
            }
            payload.set("columns", columns);
            payload.set("filter", createPreMarketFilters(mapper));
            payload.put("ignore_unknown_fields", false);
            payload.set("options", mapper.createObjectNode().put("lang", "en"));
            payload.set("range", mapper.createArrayNode().add(0).add(100));
            payload.set("sort", mapper.createObjectNode()
                .put("sortBy", "premarket_volume")
                .put("sortOrder", "desc"));
            payload.set("symbols", mapper.createObjectNode());
            payload.set("markets", mapper.createArrayNode().add("america"));
            payload.set("filter2", createTypeFilter(mapper));

            String json = mapper.writeValueAsString(payload);
            //System.out.println("TradingView buildPennyStocksPreMarketRequest Generated Payload: \n " + json); // Log for debugging
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Error building pre-market request", e);
        }
    }
    
    public String buildPennyStockStandardMarketStocksByVolumeRequest() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode payload = mapper.createObjectNode();

            // Add columns - we can reuse PRE_MARKET_COLUMNS since they're similar
            ArrayNode columns = mapper.createArrayNode();
            for (String column : PRE_MARKET_COLUMNS) {
                columns.add(column);
            }
            payload.set("columns", columns);

            // Add filters - modified for standard market
            ArrayNode filters = mapper.createArrayNode();
            filters.add(createRangeFilter(mapper, "close", 0.5, 10));
            //ArrayNode analystRatings = mapper.createArrayNode().add("Buy").add("Neutral").add("StrongBuy");
            //filters.add(createRangeFilter(mapper, "AnalystRating", analystRatings));
            //filters.add(createComparisonFilter(mapper, "change_from_open", "greater", 0));
            //filters.add(createComparisonFilter(mapper, "change", "greater", 0));
            filters.add(createEqualFilter(mapper, "is_primary", true));
            payload.set("filter", filters);

            payload.put("ignore_unknown_fields", false);
            payload.set("options", mapper.createObjectNode().put("lang", "en"));
            payload.set("range", mapper.createArrayNode().add(0).add(100));
            payload.set("sort", mapper.createObjectNode()
                .put("sortBy", "volume")  // Sorting by volume
                .put("sortOrder", "desc"));
            payload.set("symbols", mapper.createObjectNode());
            payload.set("markets", mapper.createArrayNode().add("america"));
            payload.set("filter2", createTypeFilter(mapper));

            String json = mapper.writeValueAsString(payload);
            
            //System.out.println("TradingView buildPennyStockStandardMarketStocksByVolumeRequest Generated Payload: \n " + json); // Log for debugging
            
            return json;
            
            
        } catch (Exception e) {
            throw new RuntimeException("Error building standard market by volume request", e);
        }
    }
    
    public String buildPennyStocksPostMarketByVolumeRequest() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode payload = mapper.createObjectNode();

            // Reuse columns from existing constant
            ArrayNode columns = mapper.createArrayNode();
            for (String column : PRE_MARKET_COLUMNS) {
                columns.add(column);
            }
            payload.set("columns", columns);

            // Custom filters for post-market
            ArrayNode filters = mapper.createArrayNode();
            filters.add(createRangeFilter(mapper, "close", 0.5, 10));
            //ArrayNode analystRatings = mapper.createArrayNode().add("Buy").add("Neutral").add("StrongBuy");
            //filters.add(createRangeFilter(mapper, "AnalystRating", analystRatings));
            //filters.add(createComparisonFilter(mapper, "postmarket_change", "greater", 0));
            //filters.add(createComparisonFilter(mapper, "change", "greater", 0));
            filters.add(createEqualFilter(mapper, "is_primary", true));
            payload.set("filter", filters);

            // Standard configuration
            payload.put("ignore_unknown_fields", false);
            payload.set("options", mapper.createObjectNode().put("lang", "en"));
            payload.set("range", mapper.createArrayNode().add(0).add(100));
            
            // Sort by postmarket_volume in descending order
            payload.set("sort", mapper.createObjectNode()
                .put("sortBy", "postmarket_volume")
                .put("sortOrder", "desc"));
                
            payload.set("symbols", mapper.createObjectNode());
            payload.set("markets", mapper.createArrayNode().add("america"));
            
            // Reuse existing type filter
            payload.set("filter2", createTypeFilter(mapper));

            String json = mapper.writeValueAsString(payload);
            
            //System.out.println("TradingView buildPennyStocksPostMarketByVolumeRequest Generated Payload: \n " + json); // Log for debugging
            
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Error building post-market penny stocks request", e);
        }
    }
    
    public String buildIndexStocksMarketByPreMarketVolumeRequest() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode payload = mapper.createObjectNode();

            // Reuse columns from existing constant
            ArrayNode columns = mapper.createArrayNode();
            for (String column : PRE_MARKET_COLUMNS) {
                columns.add(column);
            }
            payload.set("columns", columns);

            // Custom filters for index stocks
            ArrayNode filters = mapper.createArrayNode();
            //filters.add(createEqualFilter(mapper, "is_blacklisted", false));
            //ArrayNode analystRatings = mapper.createArrayNode().add("Buy").add("Neutral").add("StrongBuy");
           // filters.add(createRangeFilter(mapper, "AnalystRating", analystRatings));
            //filters.add(createComparisonFilter(mapper, "premarket_change", "greater", 0));
            filters.add(createEqualFilter(mapper, "is_primary", true));
            payload.set("filter", filters);

            // Standard configuration
            payload.put("ignore_unknown_fields", false);
            payload.set("options", mapper.createObjectNode().put("lang", "en"));
            payload.set("range", mapper.createArrayNode().add(0).add(100));
            
            // Sort by premarket_volume in descending order
            payload.set("sort", mapper.createObjectNode()
                .put("sortBy", "premarket_volume")
                .put("sortOrder", "desc"));
                
            // Add index symbols
            ObjectNode symbols = mapper.createObjectNode();
            ArrayNode symbolset = mapper.createArrayNode()
                .add("SYML:SP;SPX")
                .add("SYML:NASDAQ;NDX")
                .add("SYML:DJ;DJI")
                .add("SYML:NASDAQ;IXIC")
                .add("SYML:TVC;RUT");
            symbols.set("symbolset", symbolset);
            payload.set("symbols", symbols);
            
            payload.set("markets", mapper.createArrayNode().add("america"));
            
            // Reuse existing type filter
            payload.set("filter2", createTypeFilter(mapper));

            String json = mapper.writeValueAsString(payload);
            
            //System.out.println("TradingView buildIndexStocksMarketByPreMarketVolumeRequest Generated Payload: \n " + json); // Log for debugging
            
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Error building index stocks pre-market volume request", e);
        }
    }
    
    public String buildIndexStocksMarketByStandardVolumeRequest() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode payload = mapper.createObjectNode();

            // Reuse columns from existing constant
            ArrayNode columns = mapper.createArrayNode();
            for (String column : PRE_MARKET_COLUMNS) {
                columns.add(column);
            }
            payload.set("columns", columns);

            // Custom filters for standard market index stocks
            ArrayNode filters = mapper.createArrayNode();
            //filters.add(createEqualFilter(mapper, "is_blacklisted", false));
            //ArrayNode analystRatings = mapper.createArrayNode().add("Buy").add("Neutral").add("StrongBuy");
            //filters.add(createRangeFilter(mapper, "AnalystRating", analystRatings));
            //filters.add(createComparisonFilter(mapper, "change_from_open", "greater", 0));
            //filters.add(createComparisonFilter(mapper, "change", "greater", 0));
            filters.add(createEqualFilter(mapper, "is_primary", true));
            payload.set("filter", filters);

            // Standard configuration
            payload.put("ignore_unknown_fields", false);
            payload.set("options", mapper.createObjectNode().put("lang", "en"));
            payload.set("range", mapper.createArrayNode().add(0).add(100)); // Increased range to 300
            
            // Sort by standard market volume in descending order
            payload.set("sort", mapper.createObjectNode()
                .put("sortBy", "volume")
                .put("sortOrder", "desc"));
                
            // Add index symbols
            ObjectNode symbols = mapper.createObjectNode();
            ArrayNode symbolset = mapper.createArrayNode()
                .add("SYML:NASDAQ;NDX")
                .add("SYML:DJ;DJI")
                .add("SYML:SP;SPX")
                .add("SYML:NASDAQ;IXIC")
                .add("SYML:TVC;RUT");
            symbols.set("symbolset", symbolset);
            payload.set("symbols", symbols);
            
            payload.set("markets", mapper.createArrayNode().add("america"));
            
            // Reuse existing type filter
            payload.set("filter2", createTypeFilter(mapper));

            String json = mapper.writeValueAsString(payload);
            
            //System.out.println("TradingView buildIndexStocksMarketByStandardVolumeRequest Generated Payload: \n " + json); // Log for debugging
            
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Error building index stocks standard market volume request", e);
        }
    }
    
    public String buildIndexStocksMarketByPostMarketVolumeRequest() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode payload = mapper.createObjectNode();

            // Reuse columns from existing constant
            ArrayNode columns = mapper.createArrayNode();
            for (String column : PRE_MARKET_COLUMNS) {
                columns.add(column);
            }
            payload.set("columns", columns);

            // Custom filters for post-market index stocks
            ArrayNode filters = mapper.createArrayNode();
            //filters.add(createEqualFilter(mapper, "is_blacklisted", false));
            //ArrayNode analystRatings = mapper.createArrayNode().add("Buy").add("Neutral").add("StrongBuy");
            //filters.add(createRangeFilter(mapper, "AnalystRating", analystRatings));
            //filters.add(createComparisonFilter(mapper, "postmarket_change", "greater", 0));
            filters.add(createEqualFilter(mapper, "is_primary", true));
            payload.set("filter", filters);

            // Standard configuration
            payload.put("ignore_unknown_fields", false);
            payload.set("options", mapper.createObjectNode().put("lang", "en"));
            payload.set("range", mapper.createArrayNode().add(0).add(100));
            
            // Sort by postmarket_volume in descending order
            payload.set("sort", mapper.createObjectNode()
                .put("sortBy", "postmarket_volume")
                .put("sortOrder", "desc"));
                
            // Add index symbols
            ObjectNode symbols = mapper.createObjectNode();
            ArrayNode symbolset = mapper.createArrayNode()
                .add("SYML:NASDAQ;NDX")
                .add("SYML:DJ;DJI")
                .add("SYML:SP;SPX")
                .add("SYML:NASDAQ;IXIC")
                .add("SYML:TVC;RUT");
            symbols.set("symbolset", symbolset);
            payload.set("symbols", symbols);
            
            payload.set("markets", mapper.createArrayNode().add("america"));
            
            // Reuse existing type filter
            payload.set("filter2", createTypeFilter(mapper));

            String json = mapper.writeValueAsString(payload);
            
            //System.out.println("TradingView buildIndexStocksMarketByPostMarketVolumeRequest Generated Payload: \n " + json); // Log for debugging
            
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Error building index stocks post-market volume request", e);
        }
    }
    
    public String buildStocksMarketBySymbolRequest(String symbol) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode payload = mapper.createObjectNode();

            // Reuse columns from existing constant
            ArrayNode columns = mapper.createArrayNode();
            for (String column : PRE_MARKET_COLUMNS) {
                columns.add(column);
            }
            payload.set("columns", columns);

            // Custom filters for symbol search
            ArrayNode filters = mapper.createArrayNode();
            filters.add(createMatchFilter(mapper, "name,description", symbol));
            filters.add(createEqualFilter(mapper, "is_primary", true));
            payload.set("filter", filters);

            // Standard configuration
            payload.put("ignore_unknown_fields", false);
            payload.set("options", mapper.createObjectNode().put("lang", "en"));
            payload.set("range", mapper.createArrayNode().add(0).add(100));
            payload.set("sort", mapper.createObjectNode()
                .put("sortBy", "premarket_volume")
                .put("sortOrder", "desc"));
            payload.set("symbols", mapper.createObjectNode());
            payload.set("markets", mapper.createArrayNode().add("america"));
            payload.set("filter2", createTypeFilter(mapper));

            String json = mapper.writeValueAsString(payload);
            
            //System.out.println("TradingView buildStocksMarketBySymbolRequest Generated Payload: \n " + json); // Log for debugging
            
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Error building stocks by symbol request for: " + symbol, e);
        }
    }

    // Add new helper method for match filter
    private ObjectNode createMatchFilter(ObjectMapper mapper, String field, String value) {
        ObjectNode filter = mapper.createObjectNode();
        filter.put("left", field);
        filter.put("operation", "match");
        filter.put("right", value);
        return filter;
    }

    private ArrayNode createPreMarketFilters(ObjectMapper mapper) {
        ArrayNode filters = mapper.createArrayNode();
        filters.add(createRangeFilter(mapper, "close", 0.5, 10));
        //filters.add(createComparisonFilter(mapper, "change", "greater", 0)); // Integer per expected JSON
        //ArrayNode analystRatings = mapper.createArrayNode().add("Buy").add("Neutral").add("StrongBuy");
        //filters.add(createRangeFilter(mapper, "AnalystRating", analystRatings));
        //filters.add(createComparisonFilter(mapper, "premarket_change", "greater", 0)); // Integer
        filters.add(createEqualFilter(mapper, "is_primary", true));
        return filters;
    }

    private ObjectNode createTypeFilter(ObjectMapper mapper) {
        ObjectNode filter = mapper.createObjectNode();
        filter.put("operator", "and");
        ArrayNode operands = mapper.createArrayNode();
        ObjectNode operationWrapper = mapper.createObjectNode();
        ObjectNode orOperation = mapper.createObjectNode();
        orOperation.put("operator", "or");
        ArrayNode orOperands = mapper.createArrayNode();
        orOperands.add(wrapOperation(mapper, createStockTypeOperation(mapper, "common")));
        orOperands.add(wrapOperation(mapper, createStockTypeOperation(mapper, "preferred")));
        orOperands.add(wrapOperation(mapper, createSimpleTypeOperation(mapper, "dr")));
        orOperands.add(wrapOperation(mapper, createFundOperation(mapper)));
        orOperation.set("operands", orOperands);
        operationWrapper.set("operation", orOperation);
        operands.add(operationWrapper);
        filter.set("operands", operands);
        return filter;
    }

    private ObjectNode wrapOperation(ObjectMapper mapper, ObjectNode innerOperation) {
        ObjectNode wrapper = mapper.createObjectNode();
        wrapper.set("operation", innerOperation);
        return wrapper;
    }

    private ObjectNode createStockTypeOperation(ObjectMapper mapper, String type) {
        ObjectNode operation = mapper.createObjectNode();
        operation.put("operator", "and");
        ArrayNode operands = mapper.createArrayNode();
        operands.add(wrapExpression(mapper, createExpression(mapper, "type", "equal", "stock")));
        operands.add(wrapExpression(mapper, createExpression(mapper, "typespecs", "has", mapper.createArrayNode().add(type))));
        operation.set("operands", operands);
        return operation;
    }

    private ObjectNode createSimpleTypeOperation(ObjectMapper mapper, String type) {
        ObjectNode operation = mapper.createObjectNode();
        operation.put("operator", "and");
        ArrayNode operands = mapper.createArrayNode();
        operands.add(wrapExpression(mapper, createExpression(mapper, "type", "equal", type)));
        operation.set("operands", operands);
        return operation;
    }

    private ObjectNode createFundOperation(ObjectMapper mapper) {
        ObjectNode operation = mapper.createObjectNode();
        operation.put("operator", "and");
        ArrayNode operands = mapper.createArrayNode();
        operands.add(wrapExpression(mapper, createExpression(mapper, "type", "equal", "fund")));
        operands.add(wrapExpression(mapper, createExpression(mapper, "typespecs", "has_none_of", mapper.createArrayNode().add("etf"))));
        operation.set("operands", operands);
        return operation;
    }

    private ObjectNode wrapExpression(ObjectMapper mapper, ObjectNode innerExpression) {
        ObjectNode wrapper = mapper.createObjectNode();
        wrapper.set("expression", innerExpression);
        return wrapper;
    }

    private ObjectNode createExpression(ObjectMapper mapper, String left, String operation, Object right) {
        ObjectNode expression = mapper.createObjectNode();
        expression.put("left", left);
        expression.put("operation", operation);
        if (right instanceof String) {
            expression.put("right", (String) right);
        } else if (right instanceof ArrayNode) {
            expression.set("right", (ArrayNode) right);
        } else if (right instanceof Number) {
            expression.put("right", ((Number) right).intValue()); // Use int for consistency with expected JSON
        } else if (right instanceof Boolean) {
            expression.put("right", (Boolean) right);
        }
        return expression;
    }

    private ObjectNode createRangeFilter(ObjectMapper mapper, String field, double min, double max) {
        ObjectNode filter = mapper.createObjectNode();
        filter.put("left", field);
        filter.put("operation", "in_range");
        filter.set("right", mapper.createArrayNode().add(min).add(max));
        return filter;
    }

    private ObjectNode createRangeFilter(ObjectMapper mapper, String field, ArrayNode values) {
        ObjectNode filter = mapper.createObjectNode();
        filter.put("left", field);
        filter.put("operation", "in_range");
        filter.set("right", values);
        return filter;
    }

    private ObjectNode createComparisonFilter(ObjectMapper mapper, String field, String operation, Object value) {
        ObjectNode filter = mapper.createObjectNode();
        filter.put("left", field);
        filter.put("operation", operation);
        if (value instanceof Number) {
            filter.put("right", ((Number) value).intValue()); // Keep as integer per expected JSON
        } else {
            filter.put("right", value.toString());
        }
        return filter;
    }

    private ObjectNode createEqualFilter(ObjectMapper mapper, String field, Object value) {
        ObjectNode filter = mapper.createObjectNode();
        filter.put("left", field);
        filter.put("operation", "equal");
        if (value instanceof Boolean) {
            filter.put("right", (Boolean) value);
        } else {
            filter.put("right", value.toString());
        }
        return filter;
    }

    public String buildSymbolRequest(String symbol, String[] fields) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode payload = mapper.createObjectNode();
            
            // Build fields parameter from the columns array
            ArrayNode fieldsArray = mapper.createArrayNode();
            for (String field : fields) {
                fieldsArray.add(field);
            }
            
            payload.set("fields", fieldsArray);
            payload.put("symbol", symbol);
            
            String json = mapper.writeValueAsString(payload);
            ///System.out.println("TradingView buildSymbolRequest Generated Payload: \n " + json);
            return json;
            
        } catch (Exception e) {
            throw new RuntimeException("Error building symbol request for: " + symbol, e);
        }
    }
}