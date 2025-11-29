package com.quantlabs.stockApp.utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.model.StockDataResult;

public class PriceDataMapper {
    public static ConcurrentHashMap<String, PriceData> mapByTicker(List<PriceData> priceDataList) {
        return priceDataList.stream()
                .collect(Collectors.toConcurrentMap(
                        PriceData::getTicker,
                        pd -> pd,
                        (existing, replacement) -> replacement,
                        ConcurrentHashMap::new));
    }

    public static List<String> getSymbols(Map<String, PriceData> priceDataMap) {
        return priceDataMap.keySet()
                .stream()
                .sorted()
                .collect(Collectors.toList());
    }

    // Mock implementation for mapToPriceData
    public static PriceData mapToPriceData(StockDataResult result) {
        // In a real implementation, parse result.getJsonResponse() to extract price data
        // For now, return mock PriceData
        return new PriceData.Builder("DUMMY", 100.0 + Math.random() * 50)
                .percentChange(0.0)
                .averageVol(1000000.0)
                .prevLastDayPrice(100.0)
                .currentVolume(100000L)
                .build();
    }
}