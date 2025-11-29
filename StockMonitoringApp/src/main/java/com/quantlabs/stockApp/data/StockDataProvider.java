package com.quantlabs.stockApp.data;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.ta4j.core.BarSeries;

import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.model.StockDataResult;
import com.quantlabs.stockApp.utils.TimeCalculationUtils;

public interface StockDataProvider {
	BarSeries getHistoricalData(String symbol, String timeframe, int limit, ZonedDateTime start, ZonedDateTime end)
			throws IOException;

	PriceData getLatestPrice(String symbol) throws IOException;

	PriceData getPriceData(String symbol, ZonedDateTime start, ZonedDateTime end) throws IOException;
	
	default StockDataResult fetchStockData(String symbol, String timeframe, int limit, ZonedDateTime start,
			ZonedDateTime end) throws IOException {
		// Default implementation using existing methods
		BarSeries series = getHistoricalData(symbol, timeframe, limit, start, end);
		return new StockDataResult(series.toString(), start, end); // Simple example
	}
	
	default ZonedDateTime calculateDefaultStartTime(String timeframe, ZonedDateTime end) {
        return TimeCalculationUtils.calculateStartTime(timeframe, end);
    }
	
    default ZonedDateTime getCurrentTime() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }

    default ZonedDateTime calculateDefaultStartTime(String timeframe, ZonedDateTime startTime, ZonedDateTime endTime) {
    	return TimeCalculationUtils.calculateStartTime(timeframe, startTime, endTime);
    }
}