package com.quantlabs.stockApp.analysis.service;

import com.quantlabs.stockApp.model.PriceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CrossSymbolDataServiceTest {

    private CrossSymbolDataService crossSymbolService;
    private List<PriceData> testSymbols;

    @BeforeEach
    void setUp() {
        crossSymbolService = new CrossSymbolDataService();
        
        testSymbols = Arrays.asList(
            new PriceData.Builder("AAPL", 150.0)
                .currentVolume(2000000L)
                .percentChange(0.03)
                .averageVol(1500000.0)
                .build(),
            new PriceData.Builder("MSFT", 300.0)
                .currentVolume(1500000L)
                .percentChange(0.015)
                .averageVol(1200000.0)
                .build(),
            new PriceData.Builder("GOOGL", 2700.0)
                .currentVolume(800000L)
                .percentChange(0.025)
                .averageVol(900000.0)
                .build()
        );
    }

    @Test
    void testUpdateCurrentSymbols() {
        crossSymbolService.updateCurrentSymbols(testSymbols);
        
        // UPDATED: Use "latestPrice" instead of "price"
        Map<String, Double> priceValues = crossSymbolService.getCurrentValues("latestPrice");
        assertNotNull(priceValues);
        assertEquals(3, priceValues.size());
        assertEquals(150.0, priceValues.get("AAPL"), 0.001);
        assertEquals(300.0, priceValues.get("MSFT"), 0.001);
        assertEquals(2700.0, priceValues.get("GOOGL"), 0.001);
        
        // UPDATED: Use "currentVolume" instead of "volume"
        Map<String, Double> volumeValues = crossSymbolService.getCurrentValues("currentVolume");
        assertNotNull(volumeValues);
        assertEquals(3, volumeValues.size());
    }

    @Test
    void testCalculateCrossSymbolMean() {
        crossSymbolService.updateCurrentSymbols(testSymbols);
        
        // UPDATED: Use "latestPrice" instead of "price"
        double meanPrice = crossSymbolService.calculateCrossSymbolMean("latestPrice");
        double expectedMean = (150.0 + 300.0 + 2700.0) / 3;
        assertEquals(expectedMean, meanPrice, 0.001);
        
        // UPDATED: Use "currentVolume" instead of "volume"
        double meanVolume = crossSymbolService.calculateCrossSymbolMean("currentVolume");
        double expectedVolumeMean = (2000000.0 + 1500000.0 + 800000.0) / 3;
        assertEquals(expectedVolumeMean, meanVolume, 0.001);
    }

    @Test
    void testCalculateCrossSymbolStdDev() {
        crossSymbolService.updateCurrentSymbols(testSymbols);
        
        // UPDATED: Use "latestPrice" instead of "price"
        double stdDev = crossSymbolService.calculateCrossSymbolStdDev("latestPrice");
        assertTrue(stdDev > 0, "Standard deviation should be positive");
    }

    @Test
    void testCalculatePercentile() {
        crossSymbolService.updateCurrentSymbols(testSymbols);
        
        double aaplPercentile = crossSymbolService.calculatePercentile("latestPrice", "AAPL", false);
        double msftPercentile = crossSymbolService.calculatePercentile("latestPrice", "MSFT", false);
        double googlPercentile = crossSymbolService.calculatePercentile("latestPrice", "GOOGL", false);
        
        // With the new formula:
        // AAPL (150): ~16.67%, MSFT (300): 50.00%, GOOGL (2700): ~83.33%
        assertTrue(googlPercentile > msftPercentile);
        assertTrue(msftPercentile > aaplPercentile);
        
        // Approximate values due to floating point precision
        assertEquals(16.67, aaplPercentile, 0.1);
        assertEquals(50.00, msftPercentile, 0.1);
        assertEquals(83.33, googlPercentile, 0.1);
    }

    @Test
    void testCalculatePercentileLowerIsBetter() {
        crossSymbolService.updateCurrentSymbols(testSymbols);
        
        // When lower is better, AAPL (lowest price) should have highest percentile
        double aaplPercentile = crossSymbolService.calculatePercentile("latestPrice", "AAPL", true);
        double googlPercentile = crossSymbolService.calculatePercentile("latestPrice", "GOOGL", true);
        
        assertTrue(aaplPercentile > googlPercentile);
        
        // AAPL inverted percentile: 100 - 16.67 = 83.33
        // GOOGL inverted percentile: 100 - 83.33 = 16.67
        assertEquals(83.33, aaplPercentile, 0.1);
        assertEquals(16.67, googlPercentile, 0.1);
    }

    @Test
    void testGetCurrentValues() {
        crossSymbolService.updateCurrentSymbols(testSymbols);
        
        // UPDATED: Use "latestPrice" instead of "price"
        Map<String, Double> values = crossSymbolService.getCurrentValues("latestPrice");
        assertNotNull(values);
        assertEquals(3, values.size());
        assertTrue(values.containsKey("AAPL"));
        assertTrue(values.containsKey("MSFT"));
        assertTrue(values.containsKey("GOOGL"));
        
        // Test other metrics
        Map<String, Double> volumeValues = crossSymbolService.getCurrentValues("currentVolume");
        assertNotNull(volumeValues);
        assertEquals(3, volumeValues.size());
        
        Map<String, Double> percentChangeValues = crossSymbolService.getCurrentValues("percentChange");
        assertNotNull(percentChangeValues);
        assertEquals(3, percentChangeValues.size());
        
        Map<String, Double> averageVolValues = crossSymbolService.getCurrentValues("averageVol");
        assertNotNull(averageVolValues);
        assertEquals(3, averageVolValues.size());
    }

    // Add test for non-existent metric
    @Test
    void testNonExistentMetric() {
        crossSymbolService.updateCurrentSymbols(testSymbols);
        
        Map<String, Double> values = crossSymbolService.getCurrentValues("nonExistentMetric");
        assertNotNull(values);
        assertTrue(values.isEmpty());
        
        double mean = crossSymbolService.calculateCrossSymbolMean("nonExistentMetric");
        assertEquals(0.0, mean, 0.001);
        
        double stdDev = crossSymbolService.calculateCrossSymbolStdDev("nonExistentMetric");
        assertEquals(1.0, stdDev, 0.001);
        
        double percentile = crossSymbolService.calculatePercentile("nonExistentMetric", "AAPL", false);
        assertEquals(50.0, percentile, 0.001);
    }

    // Test that all supported metrics are stored
    @Test
    void testAllSupportedMetrics() {
        crossSymbolService.updateCurrentSymbols(testSymbols);
        
        // Test all metrics that should be stored
        String[] expectedMetrics = {"latestPrice", "currentVolume", "percentChange", "averageVol"};
        
        for (String metric : expectedMetrics) {
            Map<String, Double> values = crossSymbolService.getCurrentValues(metric);
            assertNotNull(values);
            assertEquals(3, values.size(), "Metric " + metric + " should have 3 values");
        }
    }
}