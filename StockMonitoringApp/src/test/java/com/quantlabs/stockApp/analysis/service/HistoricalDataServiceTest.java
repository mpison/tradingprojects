package com.quantlabs.stockApp.analysis.service;

import com.quantlabs.stockApp.model.PriceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HistoricalDataServiceTest {

    private HistoricalDataService historicalService;

    @BeforeEach
    void setUp() {
        historicalService = new HistoricalDataService();
    }

    @Test
    void testAddDataPoint() {
        historicalService.addDataPoint("testMetric", 100.0);
        historicalService.addDataPoint("testMetric", 120.0);
        historicalService.addDataPoint("testMetric", 80.0);
        
        double mean = historicalService.calculateHistoricalMean("testMetric");
        double stdDev = historicalService.calculateHistoricalStdDev("testMetric");
        
        assertEquals(100.0, mean, 0.001);
        assertTrue(stdDev > 0);
    }

    @Test
    void testAddDataPointMaxLimit() {
        // Add more than maxHistoricalPoints (100) data points
        for (int i = 0; i < 150; i++) {
            historicalService.addDataPoint("overflowMetric", i * 1.0);
        }
        
        // Should only keep the most recent 100 points
        int count = historicalService.getDataPointCount("overflowMetric");
        assertEquals(100, count);
        
        // Mean should be calculated from recent values (50-149)
        double mean = historicalService.calculateHistoricalMean("overflowMetric");
        assertTrue(mean > 50 && mean < 149);
    }

    @Test
    void testAddPriceData() {
        PriceData priceData = new PriceData.Builder("TEST", 150.0)
            .currentVolume(2000000L)
            .percentChange(0.05)
            .averageVol(1500000.0)
            .premarketClose(148.0)
            .postmarketClose(152.0)
            .build();
        
        historicalService.addPriceData(priceData);
        
        // Verify various metrics were added
        assertTrue(historicalService.getDataPointCount("price") > 0);
        assertTrue(historicalService.getDataPointCount("volume") > 0);
        assertTrue(historicalService.getDataPointCount("percentChange") > 0);
        assertTrue(historicalService.getDataPointCount("averageVol") > 0);
    }

    @Test
    void testAddPriceDataWithNullValues() {
        PriceData priceData = new PriceData.Builder("TEST", 150.0)
            .currentVolume(null) // Null volume
            .percentChange(0.05)
            .premarketClose(null) // Null premarket
            .build();
        
        historicalService.addPriceData(priceData);
        
        // Should handle null values gracefully
        assertTrue(historicalService.getDataPointCount("price") > 0);
        assertTrue(historicalService.getDataPointCount("percentChange") > 0);
        assertEquals(0, historicalService.getDataPointCount("volume")); // Null volume shouldn't be added
        assertEquals(0, historicalService.getDataPointCount("premarketClose")); // Null premarket shouldn't be added
    }

    @Test
    void testCalculateHistoricalMean() {
        historicalService.addDataPoint("testMean", 100.0);
        historicalService.addDataPoint("testMean", 200.0);
        historicalService.addDataPoint("testMean", 300.0);
        
        double mean = historicalService.calculateHistoricalMean("testMean");
        assertEquals(200.0, mean, 0.001);
    }

    @Test
    void testCalculateHistoricalMeanEmptyData() {
        double mean = historicalService.calculateHistoricalMean("nonExistentMetric");
        assertEquals(0.0, mean, 0.001); // Default for empty data
        
        // Test with specific metric types for default values
        double priceMean = historicalService.calculateHistoricalMean("price");
        assertEquals(100.0, priceMean, 0.001); // Default for price metrics
        
        double volumeMean = historicalService.calculateHistoricalMean("volume");
        assertEquals(1000000.0, volumeMean, 0.001); // Default for volume metrics
    }

    @Test
    void testCalculateHistoricalStdDev() {
        historicalService.addDataPoint("testStdDev", 90.0);
        historicalService.addDataPoint("testStdDev", 100.0);
        historicalService.addDataPoint("testStdDev", 110.0);
        
        double stdDev = historicalService.calculateHistoricalStdDev("testStdDev");
        
        // Manual calculation: mean = 100, variance = ((10^2 + 0^2 + 10^2) / 2) = 100, stdDev = 10
        assertEquals(10.0, stdDev, 0.001);
    }

    @Test
    void testCalculateHistoricalStdDevSinglePoint() {
        historicalService.addDataPoint("singlePoint", 100.0);
        
        double stdDev = historicalService.calculateHistoricalStdDev("singlePoint");
        assertEquals(1.0, stdDev, 0.001); // Default for single data point
    }

    @Test
    void testCalculateHistoricalStdDevEmptyData() {
        double stdDev = historicalService.calculateHistoricalStdDev("nonExistentMetric");
        assertEquals(1.0, stdDev, 0.001); // Default for empty data
        
        // Test with specific metric types for default values
        double priceStdDev = historicalService.calculateHistoricalStdDev("price");
        assertEquals(50.0, priceStdDev, 0.001); // Default for price metrics
        
        double volumeStdDev = historicalService.calculateHistoricalStdDev("volume");
        assertEquals(500000.0, volumeStdDev, 0.001); // Default for volume metrics
    }

    @Test
    void testGetDataPointCount() {
        assertEquals(0, historicalService.getDataPointCount("emptyMetric"));
        
        historicalService.addDataPoint("testCount", 100.0);
        assertEquals(1, historicalService.getDataPointCount("testCount"));
        
        historicalService.addDataPoint("testCount", 200.0);
        assertEquals(2, historicalService.getDataPointCount("testCount"));
    }

    @Test
    void testClearHistoricalData() {
        historicalService.addDataPoint("toClear", 100.0);
        historicalService.addDataPoint("toClear", 200.0);
        
        assertEquals(2, historicalService.getDataPointCount("toClear"));
        
        historicalService.clearHistoricalData();
        
        assertEquals(0, historicalService.getDataPointCount("toClear"));
        assertEquals(0.0, historicalService.calculateHistoricalMean("toClear"), 0.001);
    }

    @Test
    void testDefaultMeanValues() {
        // Test default values for various metric types
        assertEquals(100.0, historicalService.calculateHistoricalMean("price"), 0.001);
        assertEquals(100.0, historicalService.calculateHistoricalMean("close"), 0.001);
        assertEquals(100.0, historicalService.calculateHistoricalMean("high"), 0.001);
        assertEquals(100.0, historicalService.calculateHistoricalMean("low"), 0.001);
        
        assertEquals(1000000.0, historicalService.calculateHistoricalMean("volume"), 0.001);
        assertEquals(1000000.0, historicalService.calculateHistoricalMean("currentVolume"), 0.001);
        
        assertEquals(50.0, historicalService.calculateHistoricalMean("rsi"), 0.001);
        assertEquals(0.0, historicalService.calculateHistoricalMean("percentChange"), 0.001);
    }

    @Test
    void testDefaultStdDevValues() {
        // Test default values for various metric types
        assertEquals(50.0, historicalService.calculateHistoricalStdDev("price"), 0.001);
        assertEquals(50.0, historicalService.calculateHistoricalStdDev("close"), 0.001);
        
        assertEquals(500000.0, historicalService.calculateHistoricalStdDev("volume"), 0.001);
        assertEquals(500000.0, historicalService.calculateHistoricalStdDev("currentVolume"), 0.001);
        
        assertEquals(15.0, historicalService.calculateHistoricalStdDev("rsi"), 0.001);
        assertEquals(0.05, historicalService.calculateHistoricalStdDev("percentChange"), 0.001);
    }

    @Test
    void testMultipleMetrics() {
        // Test that different metrics are stored separately
        historicalService.addDataPoint("metric1", 100.0);
        historicalService.addDataPoint("metric2", 200.0);
        historicalService.addDataPoint("metric1", 150.0);
        historicalService.addDataPoint("metric2", 250.0);
        
        assertEquals(2, historicalService.getDataPointCount("metric1"));
        assertEquals(2, historicalService.getDataPointCount("metric2"));
        
        assertEquals(125.0, historicalService.calculateHistoricalMean("metric1"), 0.001);
        assertEquals(225.0, historicalService.calculateHistoricalMean("metric2"), 0.001);
    }

    @Test
    void testNegativeValues() {
        historicalService.addDataPoint("negativeValues", -50.0);
        historicalService.addDataPoint("negativeValues", -30.0);
        historicalService.addDataPoint("negativeValues", -10.0);
        
        double mean = historicalService.calculateHistoricalMean("negativeValues");
        double stdDev = historicalService.calculateHistoricalStdDev("negativeValues");
        
        assertEquals(-30.0, mean, 0.001);
        assertTrue(stdDev > 0);
    }

    @Test
    void testZeroValues() {
        historicalService.addDataPoint("zeroValues", 0.0);
        historicalService.addDataPoint("zeroValues", 0.0);
        historicalService.addDataPoint("zeroValues", 0.0);
        
        double mean = historicalService.calculateHistoricalMean("zeroValues");
        double stdDev = historicalService.calculateHistoricalStdDev("zeroValues");
        
        assertEquals(0.0, mean, 0.001);
        assertEquals(0.0, stdDev, 0.001);
    }

    @Test
    void testVeryLargeValues() {
        historicalService.addDataPoint("largeValues", 1000000.0);
        historicalService.addDataPoint("largeValues", 2000000.0);
        historicalService.addDataPoint("largeValues", 3000000.0);
        
        double mean = historicalService.calculateHistoricalMean("largeValues");
        double stdDev = historicalService.calculateHistoricalStdDev("largeValues");
        
        assertEquals(2000000.0, mean, 0.001);
        assertTrue(stdDev > 0);
    }
}