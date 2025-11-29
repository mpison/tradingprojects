package com.quantlabs.stockApp.analysis.service;

import com.quantlabs.stockApp.analysis.model.AnalysisMetric;
import com.quantlabs.stockApp.analysis.model.ComparisonType;
import com.quantlabs.stockApp.analysis.model.ZScoreCombination;
import com.quantlabs.stockApp.model.PriceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZScoreAnalysisServiceTest {

    @Mock
    private AnalysisMetricService metricService;

    @Mock
    private HistoricalDataService historicalDataService;

    @Mock
    private CrossSymbolDataService crossSymbolService;

    @InjectMocks
    private ZScoreAnalysisService analysisService;

    @BeforeEach
    void setUp() {
        // Clear any existing combinations from the service
        analysisService.clearCombinations();
        reset(metricService, historicalDataService, crossSymbolService);
    }

    @Test
    void testAddAndGetCombination() {
        // Arrange
        Map<String, Double> weights = Map.of("price", 60.0, "volume", 40.0);
        ZScoreCombination combination = new ZScoreCombination("TEST_COMBO", weights);

        // Act
        analysisService.addCombination(combination);
        ZScoreCombination retrieved = analysisService.getCombination("TEST_COMBO");

        // Assert 
        assertNotNull(retrieved);
        assertEquals("TEST_COMBO", retrieved.getName());
        assertEquals(2, retrieved.getWeights().size());
        assertEquals(60.0, retrieved.getWeights().get("price"));
        assertEquals(40.0, retrieved.getWeights().get("volume"));
    }

    @Test
    void testGetAvailableCombinations() {
        // Arrange
        analysisService.addCombination(new ZScoreCombination("COMBO_1", Map.of("metric1", 100.0)));
        analysisService.addCombination(new ZScoreCombination("COMBO_2", Map.of("metric2", 100.0)));

        // Act
        List<String> available = analysisService.getAvailableCombinations();

        // Assert
        assertEquals(2, available.size()); // Only the ones we added, not including default ones
        assertTrue(available.contains("COMBO_1"));
        assertTrue(available.contains("COMBO_2"));
    }

    @Test
    void testRemoveCombination() {
        // Arrange
        analysisService.addCombination(new ZScoreCombination("TO_REMOVE", Map.of("metric", 100.0)));
        assertNotNull(analysisService.getCombination("TO_REMOVE"));

        // Act
        analysisService.removeCombination("TO_REMOVE");

        // Assert
        assertNull(analysisService.getCombination("TO_REMOVE"));
        assertFalse(analysisService.getAvailableCombinations().contains("TO_REMOVE"));
    }

    @Test
    void testUpdateCombinationWeights() {
        // Arrange
        analysisService.addCombination(new ZScoreCombination("UPDATE_TEST", Map.of("oldMetric", 100.0)));

        Map<String, Double> newWeights = Map.of("newMetric1", 60.0, "newMetric2", 40.0);

        // Act
        analysisService.updateCombinationWeights("UPDATE_TEST", newWeights);
        ZScoreCombination updated = analysisService.getCombination("UPDATE_TEST");

        // Assert
        assertNotNull(updated);
        assertEquals(2, updated.getWeights().size());
        assertEquals(60.0, updated.getWeights().get("newMetric1"));
        assertEquals(40.0, updated.getWeights().get("newMetric2"));
        assertNull(updated.getWeights().get("oldMetric"));
    }

    @Test
    void testRankSymbols_WithEmptyCombination() {
        // Arrange
        List<PriceData> symbols = Arrays.asList(
            new PriceData.Builder("AAPL", 150.0).build(),
            new PriceData.Builder("MSFT", 300.0).build()
        );

        analysisService.addCombination(new ZScoreCombination("EMPTY_COMBO", Map.of()));

        // Act
        List<Map.Entry<String, Double>> results = analysisService.rankSymbols(symbols, "EMPTY_COMBO");

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        // All symbols should have score 0.0 when combination has no metrics
        for (Map.Entry<String, Double> entry : results) {
            assertEquals(0.0, entry.getValue(), 0.001);
        }
    }

    @Test
    void testRankSymbols_WithUnknownCombination() {
        // Arrange
        List<PriceData> symbols = Arrays.asList(new PriceData.Builder("TEST", 100.0).build());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            analysisService.rankSymbols(symbols, "NON_EXISTENT");
        });
    }

    @Test
    void testRankSymbols_WithEmptySymbolList() {
        // Arrange
        List<PriceData> emptyList = Arrays.asList();
        analysisService.addCombination(new ZScoreCombination("TEST", Map.of("price", 100.0)));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            analysisService.rankSymbols(emptyList, "TEST");
        });
    }

    @Test
    void testRankSymbols_WithNullSymbolList() {
        // Arrange
        analysisService.addCombination(new ZScoreCombination("TEST", Map.of("price", 100.0)));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            analysisService.rankSymbols(null, "TEST");
        });
    }

    @Test
    void testRankSymbols_WithNullCombinationName() {
        // Arrange
        List<PriceData> symbols = Arrays.asList(new PriceData.Builder("TEST", 100.0).build());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            analysisService.rankSymbols(symbols, null);
        });
    }

    @Test
    void testRankSymbols_BasicFunctionality() {
        // Arrange
        List<PriceData> symbols = Arrays.asList(
            new PriceData.Builder("AAPL", 150.0).build(),
            new PriceData.Builder("MSFT", 300.0).build()
        );

        // Mock the historical data service to avoid NPE
        doNothing().when(historicalDataService).addPriceData(any(PriceData.class));
        
        // Mock the cross symbol service update
        doNothing().when(crossSymbolService).updateCurrentSymbols(anyList());
        
        // Mock the metric service update
        doNothing().when(metricService).updateWithNewData(any(PriceData.class));
        
        // Mock the metric service to return a metric and historical values
        AnalysisMetric mockMetric = new AnalysisMetric("price", null, 200.0, 50.0, PriceData.class);
        when(metricService.getMetric("price")).thenReturn(mockMetric);
        when(metricService.getHistoricalMean("price")).thenReturn(200.0);
        when(metricService.getHistoricalStdDev("price")).thenReturn(50.0);

        analysisService.addCombination(new ZScoreCombination("BASIC_TEST", Map.of("price", 100.0)));

        // Act
        List<Map.Entry<String, Double>> results = analysisService.rankSymbols(symbols, "BASIC_TEST");

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        // Verify the symbols are present
        assertTrue(results.stream().anyMatch(entry -> entry.getKey().equals("AAPL")));
        assertTrue(results.stream().anyMatch(entry -> entry.getKey().equals("MSFT")));
        
        // The actual calculation might be more complex, but let's verify the basic structure
        verify(historicalDataService, times(2)).addPriceData(any(PriceData.class));
        verify(crossSymbolService).updateCurrentSymbols(symbols);
        verify(metricService).updateWithNewData(symbols.get(0));
    }

    @Test
    void testRankSymbols_CrossSymbolComparison() {
        // Arrange
        List<PriceData> symbols = Arrays.asList(
            new PriceData.Builder("AAPL", 150.0).build(),
            new PriceData.Builder("MSFT", 300.0).build()
        );

        // Mock the service calls that happen in rankSymbols
        doNothing().when(historicalDataService).addPriceData(any(PriceData.class));
        doNothing().when(crossSymbolService).updateCurrentSymbols(anyList());
        doNothing().when(metricService).updateWithNewData(any(PriceData.class));
        
        // Mock cross-symbol service for the actual calculation
        when(crossSymbolService.calculateCrossSymbolMean("price")).thenReturn(225.0);
        when(crossSymbolService.calculateCrossSymbolStdDev("price")).thenReturn(75.0);

        // Mock metric with cross-symbol comparison
        AnalysisMetric mockMetric = new AnalysisMetric("price", null, 0.0, 0.0, PriceData.class, 
            ComparisonType.CROSS_SYMBOL, false);
        when(metricService.getMetric("price")).thenReturn(mockMetric);

        analysisService.addCombination(new ZScoreCombination("CROSS_SYMBOL_TEST", Map.of("price", 100.0)));

        // Act
        List<Map.Entry<String, Double>> results = analysisService.rankSymbols(symbols, "CROSS_SYMBOL_TEST");

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        // Verify the symbols are present and ranked
        assertTrue(results.stream().anyMatch(entry -> entry.getKey().equals("AAPL")));
        assertTrue(results.stream().anyMatch(entry -> entry.getKey().equals("MSFT")));
        
        // Verify cross-symbol service was called
        verify(crossSymbolService).calculateCrossSymbolMean("price");
        verify(crossSymbolService).calculateCrossSymbolStdDev("price");
    }

    @Test
    void testServiceInitialization() {
        // Arrange & Act - Just ensure the service can be initialized
        ZScoreAnalysisService service = new ZScoreAnalysisService(metricService, historicalDataService, crossSymbolService);

        // Assert
        assertNotNull(service);
        assertNotNull(service.getAvailableCombinations());
        // Should have the default technical combinations
        assertTrue(service.getAvailableCombinations().size() >= 4);
    }

    @Test
    void testCombinationPersistence() {
        // Arrange
        ZScoreCombination combo1 = new ZScoreCombination("PERSIST_1", Map.of("m1", 50.0, "m2", 50.0));
        ZScoreCombination combo2 = new ZScoreCombination("PERSIST_2", Map.of("m3", 100.0));

        // Act
        analysisService.addCombination(combo1);
        analysisService.addCombination(combo2);

        // Assert
        List<String> combinations = analysisService.getAvailableCombinations();
        assertEquals(2, combinations.size()); // Only the ones we added
        assertTrue(combinations.contains("PERSIST_1"));
        assertTrue(combinations.contains("PERSIST_2"));

        // Verify we can retrieve them
        assertNotNull(analysisService.getCombination("PERSIST_1"));
        assertNotNull(analysisService.getCombination("PERSIST_2"));
    }

    @Test
    void testUpdateNonExistentCombination() {
        // Arrange
        Map<String, Double> newWeights = Map.of("newMetric", 100.0);

        // Act
        analysisService.updateCombinationWeights("NON_EXISTENT", newWeights);

        // Assert - Should not throw exception, just do nothing
        assertNull(analysisService.getCombination("NON_EXISTENT"));
    }

    @Test
    void testRemoveNonExistentCombination() {
        // Act - Should not throw exception
        analysisService.removeCombination("NON_EXISTENT");

        // Assert - Combination should still not exist
        assertNull(analysisService.getCombination("NON_EXISTENT"));
    }

    @Test
    void testClearCombinations() {
        // Arrange
        analysisService.addCombination(new ZScoreCombination("COMBO_1", Map.of("m1", 100.0)));
        analysisService.addCombination(new ZScoreCombination("COMBO_2", Map.of("m2", 100.0)));
        assertEquals(2, analysisService.getAvailableCombinations().size());

        // Act
        analysisService.clearCombinations();

        // Assert
        assertEquals(0, analysisService.getAvailableCombinations().size());
        assertNull(analysisService.getCombination("COMBO_1"));
        assertNull(analysisService.getCombination("COMBO_2"));
    }

    @Test
    void testGetAllCombinations() {
        // Arrange
        analysisService.addCombination(new ZScoreCombination("COMBO_1", Map.of("m1", 100.0)));
        analysisService.addCombination(new ZScoreCombination("COMBO_2", Map.of("m2", 100.0)));

        // Act
        Map<String, ZScoreCombination> allCombinations = analysisService.getAllCombinations();

        // Assert
        assertEquals(2, allCombinations.size());
        assertNotNull(allCombinations.get("COMBO_1"));
        assertNotNull(allCombinations.get("COMBO_2"));
    }

    @Test
    void testSetAllCombinations() {
        // Arrange
        Map<String, ZScoreCombination> newCombinations = Map.of(
            "NEW_1", new ZScoreCombination("NEW_1", Map.of("m1", 100.0)),
            "NEW_2", new ZScoreCombination("NEW_2", Map.of("m2", 100.0))
        );

        // Act
        analysisService.setAllCombinations(newCombinations);

        // Assert
        assertEquals(2, analysisService.getAvailableCombinations().size());
        assertNotNull(analysisService.getCombination("NEW_1"));
        assertNotNull(analysisService.getCombination("NEW_2"));
    }
}