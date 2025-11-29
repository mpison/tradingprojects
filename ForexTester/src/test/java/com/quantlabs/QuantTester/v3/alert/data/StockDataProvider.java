package com.quantlabs.QuantTester.v3.alert.data;

import java.io.IOException;
import java.time.ZonedDateTime;

import com.quantlabs.QuantTester.v3.alert.StockDataResult;

public interface StockDataProvider {
    StockDataResult fetchStockData(String symbol, String timeframe, int limit,
                                 ZonedDateTime start, ZonedDateTime end) throws IOException;
    StockDataResult fetchStockData(String symbol, String timeframe, int limit) throws IOException;
    String convertTimeframe(String timeframe);
    ZonedDateTime calculateStartTime(String timeframe, ZonedDateTime end);
}