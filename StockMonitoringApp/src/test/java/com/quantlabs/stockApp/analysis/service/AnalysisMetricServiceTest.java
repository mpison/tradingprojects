package com.quantlabs.stockApp.analysis.service;

import com.quantlabs.stockApp.analysis.model.AnalysisMetric;
import com.quantlabs.stockApp.analysis.model.ComparisonType;
import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisMetricServiceTest {

    @Mock
    private HistoricalDataService historicalDataService;

    private AnalysisMetricService metricService;

    @BeforeEach
    void setUp() {
        when(historicalDataService.calculateHistoricalMean(anyString())).thenReturn(100.0);
        when(historicalDataService.calculateHistoricalStdDev(anyString())).thenReturn(20.0);
        
        metricService = new AnalysisMetricService(historicalDataService);
    }

    @Test
    void testServiceInitialization() {
        assertNotNull(metricService);
        assertTrue(metricService.getAllMetricNames().size() > 0);
    }

    @Test
    void testGetPriceDataMetrics() {
        List<String> priceDataMetrics = metricService.getPriceDataMetrics();
        
        assertNotNull(priceDataMetrics);
        assertFalse(priceDataMetrics.isEmpty());
        assertTrue(priceDataMetrics.contains("latestPrice"));
    }

    @Test
    void testGetTechnicalIndicators() {
        List<String> technicalIndicators = metricService.getTechnicalIndicators();
        
        assertNotNull(technicalIndicators);
        // Should contain timeframe-based metrics
        assertTrue(technicalIndicators.stream().anyMatch(s -> s.startsWith("1D_")));
    }

    @Test
    void testGetMetricsByTimeframe() {
        List<String> oneDayMetrics = metricService.getMetricsByTimeframe("1D");
        
        assertNotNull(oneDayMetrics);
        assertTrue(oneDayMetrics.stream().allMatch(s -> s.startsWith("1D_")));
    }

    @Test
    void testGetMetric() {
        AnalysisMetric metric = metricService.getMetric("latestPrice");
        
        assertNotNull(metric);
        assertEquals("latestPrice", metric.getName());
        assertEquals(100.0, metric.getMean(), 0.001);
        assertEquals(20.0, metric.getStdDev(), 0.001);
    }

    @Test
    void testGetHistoricalMean() {
        double mean = metricService.getHistoricalMean("latestPrice");
        assertEquals(100.0, mean, 0.001);
        
        verify(historicalDataService, atMost(2)).calculateHistoricalMean("latestPrice");
    }

    @Test
    void testGetHistoricalStdDev() {
        double stdDev = metricService.getHistoricalStdDev("latestPrice");
        assertEquals(20.0, stdDev, 0.001);
        
        verify(historicalDataService, atMost(2)).calculateHistoricalStdDev("latestPrice");
    }

    @Test
    void testUpdateWithNewData() {
        PriceData priceData = new PriceData.Builder("TEST", 150.0)
            .currentVolume(1000000L)
            .percentChange(0.05)
            .build();

        metricService.updateWithNewData(priceData); 

        verify(historicalDataService).addPriceData(priceData);
        verify(historicalDataService, atLeastOnce()).calculateHistoricalMean(anyString());
        verify(historicalDataService, atLeastOnce()).calculateHistoricalStdDev(anyString());
    }

    @Test
    void testUpdateMetricSettings() {
        String metricName = "latestPrice";
        
        metricService.updateMetricSettings(metricName, ComparisonType.CROSS_SYMBOL, true);
        
        AnalysisMetric metric = metricService.getMetric(metricName);
        assertNotNull(metric);
        assertEquals(ComparisonType.CROSS_SYMBOL, metric.getComparisonType());
        assertTrue(metric.isLowerIsBetter());
    }

    @Test
    void testResetAllComparisonSettings() {
        metricService.resetAllComparisonSettings();
        
        // Verify that metrics have been reset to defaults
        AnalysisMetric volumeMetric = metricService.getMetric("currentVolume");
        assertNotNull(volumeMetric);
        assertEquals(ComparisonType.CROSS_SYMBOL, volumeMetric.getComparisonType());
        assertFalse(volumeMetric.isLowerIsBetter());
    }

    @Test
    void testGetAllTimeframes() {
        String[] timeframes = metricService.getAllTimeframes();
        
        assertNotNull(timeframes);
        assertTrue(timeframes.length > 0);
        assertArrayEquals(new String[]{"1W", "1D", "4H", "1H", "30Min", "15Min", "5Min", "1Min"}, timeframes);
    }

    @Test
    void testGetDiscoveredPriceDataFields() {
        List<String> fields = metricService.getDiscoveredPriceDataFields();
        
        assertNotNull(fields);
        assertFalse(fields.isEmpty());
        assertTrue(fields.contains("latestPrice"));
        assertTrue(fields.contains("currentVolume"));
    }

    @Test
    void testGetDiscoveredAnalysisResultFields() {
        List<String> fields = metricService.getDiscoveredAnalysisResultFields();
        
        assertNotNull(fields);
        // Should contain technical indicator fields
        assertTrue(fields.stream().anyMatch(f -> f.toLowerCase().contains("rsi") || 
                                               f.toLowerCase().contains("macd")));
    }

    @Test
    void testMetricComparisonTypeDefaults() {
        // Test that count metrics default to cross-symbol with lower better
        AnalysisMetric countMetric = metricService.getMetric("someCount");
        if (countMetric != null) {
            assertEquals(ComparisonType.CROSS_SYMBOL, countMetric.getComparisonType());
            assertTrue(countMetric.isLowerIsBetter());
        }
        
        // Test that volume metrics default to cross-symbol with higher better
        AnalysisMetric volumeMetric = metricService.getMetric("currentVolume");
        assertNotNull(volumeMetric);
        assertEquals(ComparisonType.CROSS_SYMBOL, volumeMetric.getComparisonType());
        assertFalse(volumeMetric.isLowerIsBetter());
    }

    @Test
    void testNonExistentMetric() {
        AnalysisMetric metric = metricService.getMetric("nonExistentMetric");
        assertNull(metric);
    }

    @Test
    void testEmptyMetricsList() throws Exception {
        // Create a new service with empty historical data
        HistoricalDataService emptyService = new HistoricalDataService();
        AnalysisMetricService emptyMetricService = new AnalysisMetricService(emptyService);
        
        // Should still initialize without errors
        assertNotNull(emptyMetricService);
        assertTrue(emptyMetricService.getAllMetricNames().size() > 0);
    }
}