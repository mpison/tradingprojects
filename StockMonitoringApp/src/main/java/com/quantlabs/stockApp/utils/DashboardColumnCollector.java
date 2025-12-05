package com.quantlabs.stockApp.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.quantlabs.stockApp.model.PriceData;
import com.quantlabs.stockApp.reports.AnalysisResult;

/**
 * Collects and maps dashboard table column names to PriceData structure using introspection
 */
public class DashboardColumnCollector {
    
    // Patterns for different column types
    private static final Pattern TIMEFRAME_INDICATOR_PATTERN = Pattern.compile("^(\\w+)\\s+(.+)$");
    private static final Pattern ZSCORE_RANK_PATTERN = Pattern.compile("^ZScore_(.+)_Rank$");
    private static final Pattern ZSCORE_VALUE_PATTERN = Pattern.compile("^ZScore_(.+)_ZScore$");
    private static final Pattern BREAKOUT_COUNT_PATTERN = Pattern.compile("(.+)\\s+Breakout Count$");
    
    // Basic columns that are always present
    private static final Set<String> BASIC_COLUMNS = Set.of(
        "Select", "Symbol", "Latest Price", "Prev Vol", "Current Vol", "% Change"
    );
    
    // Dynamically discovered PriceData attributes
    private static final Set<String> PRICE_DATA_ATTRIBUTES = discoverPriceDataAttributes();
    private static final Set<String> ANALYSIS_RESULT_INDICATORS = discoverAnalysisResultIndicators();
    
    /**
     * Represents a mapped column with its source in PriceData
     */
    public static class ColumnMapping {
        private final String columnName;
        private final ColumnType type;
        private final String timeframe; // for indicator columns
        private final String indicator; // for indicator columns
        private final String zscoreStrategy; // for zscore columns
        private final String priceDataField; // for direct PriceData fields
        private final Class<?> fieldType; // field type for rendering
        
        public ColumnMapping(String columnName, ColumnType type, String timeframe, 
                           String indicator, String zscoreStrategy, String priceDataField, Class<?> fieldType) {
            this.columnName = columnName;
            this.type = type;
            this.timeframe = timeframe;
            this.indicator = indicator;
            this.zscoreStrategy = zscoreStrategy;
            this.priceDataField = priceDataField;
            this.fieldType = fieldType;
        }
        
        // Getters
        public String getColumnName() { return columnName; }
        public ColumnType getType() { return type; }
        public String getTimeframe() { return timeframe; }
        public String getIndicator() { return indicator; }
        public String getZscoreStrategy() { return zscoreStrategy; }
        public String getPriceDataField() { return priceDataField; }
        public Class<?> getFieldType() { return fieldType; }
        
        /**
         * Get the value from PriceData for this column
         */
        public Object getValue(PriceData priceData) {
            if (priceData == null) return null;
            
            try {
                switch (type) {
                    case BASIC:
                        return getBasicColumnValue(priceData, columnName);
                    case PRICE_DATA_ATTRIBUTE:
                        return getPriceDataAttributeValue(priceData, priceDataField);
                    case TIMEFRAME_INDICATOR:
                        return getTimeframeIndicatorValue(priceData, timeframe, indicator);
                    case ZSCORE_RANK:
                        return getZScoreRankValue(priceData, zscoreStrategy);
                    case ZSCORE_VALUE:
                        return getZScoreValue(priceData, zscoreStrategy);
                    case BREAKOUT_COUNT:
                        return getBreakoutCountValue(priceData, indicator);
                    default:
                        return null;
                }
            } catch (Exception e) {
                return null;
            }
        }
        
        private Object getBasicColumnValue(PriceData priceData, String columnName) {
            switch (columnName) {
                case "Select": return false; // Checkbox default
                case "Symbol": return priceData.getTicker();
                case "Latest Price": return priceData.getLatestPrice();
                case "Prev Vol": return priceData.getPreviousVolume();
                case "Current Vol": return priceData.getCurrentVolume();
                case "% Change": return priceData.getPercentChange();
                default: return null;
            }
        }
        
        private Object getPriceDataAttributeValue(PriceData priceData, String fieldName) {
            try {
                Field field = PriceData.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(priceData);
            } catch (Exception e) {
                return null;
            }
        }
        
        private Object getTimeframeIndicatorValue(PriceData priceData, String timeframe, String indicator) {
            if (priceData.getResults() == null) return null;
            
            AnalysisResult result = priceData.getResults().get(timeframe);
            if (result == null) return null;
            
            return getAnalysisResultValue(result, indicator);
        }
        
        private Object getAnalysisResultValue(AnalysisResult result, String indicator) {
            try {
                // Map indicator names to AnalysisResult getters - using Map.ofEntries for many entries
                Map<String, String> indicatorToFieldMap = createIndicatorToFieldMap();
                
                String fieldName = indicatorToFieldMap.get(indicator);
                if (fieldName != null) {
                    Field field = AnalysisResult.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.get(result);
                }
                
                // Check custom indicators
                if (result.getCustomIndicatorValues().containsKey(indicator)) {
                    return result.getCustomIndicatorValue(indicator);
                }
                
            } catch (Exception e) {
                // Fallback to custom values
                return result.getCustomValue(indicator);
            }
            
            return null;
        }
        
        private static Map<String, String> createIndicatorToFieldMap() {
            return Map.ofEntries(
                Map.entry("Trend", "smaTrend"),
                Map.entry("RSI", "rsiTrend"),
                Map.entry("MACD", "macdStatus"),
                Map.entry("MACD Breakout", "macdStatus"),
                Map.entry("MACD(5,8,9)", "macd359Status"),
                Map.entry("MACD(5,8,9) Breakout", "macd359Status"),
                Map.entry("HeikenAshi", "heikenAshiTrend"),
                Map.entry("PSAR(0.01)", "psar001Trend"),
                Map.entry("PSAR(0.01) Breakout", "psar001Trend"),
                Map.entry("PSAR(0.05)", "psar005Trend"),
                Map.entry("PSAR(0.05) Breakout", "psar005Trend"),
                Map.entry("Breakout Count", "breakoutSummary"),
                Map.entry("Action", "breakoutSummary"),
                Map.entry("MovingAverageTargetValue", "movingAverageTargetValue"),
                Map.entry("HighestCloseOpen", "highestCloseOpenStatus"),
                Map.entry("Volume", "volume"),
                Map.entry("Volume20MA", "volume20MA"),
                Map.entry("VWAP", "vwapStatus")
            );
        }
        
        private Object getZScoreRankValue(PriceData priceData, String strategy) {
            if (priceData.getZScoreResults() == null) return null;
            
            // For Z-Score rank, you might need to calculate ranking
            // This is a placeholder - you'll need to implement actual ranking logic
            return priceData.getZScoreResults().get(strategy) != null ? 1 : null;
        }
        
        private Object getZScoreValue(PriceData priceData, String strategy) {
            if (priceData.getZScoreResults() == null) return null;
            
            var zscoreResult = priceData.getZScoreResults().get(strategy);
            return zscoreResult != null ? zscoreResult.getOverallScore() : null;
        }
        
        private Object getBreakoutCountValue(PriceData priceData, String indicator) {
            // This would depend on how breakout counts are stored
            // Placeholder implementation
            return 0;
        }
        
        @Override
        public String toString() {
            return String.format("ColumnMapping{name='%s', type=%s, timeframe=%s, indicator=%s, zscore=%s, field=%s}",
                columnName, type, timeframe, indicator, zscoreStrategy, priceDataField);
        }
    }
    
    public enum ColumnType {
        BASIC,
        PRICE_DATA_ATTRIBUTE,
        TIMEFRAME_INDICATOR,
        ZSCORE_RANK,
        ZSCORE_VALUE,
        BREAKOUT_COUNT,
        CUSTOM_INDICATOR
    }
    
    /**
     * Discover all PriceData attributes using introspection
     */
    private static Set<String> discoverPriceDataAttributes() {
        Set<String> attributes = new HashSet<>();
        
        try {
            Field[] fields = PriceData.class.getDeclaredFields();
            for (Field field : fields) {
                // Skip static fields and fields from Object class
                if (Modifier.isStatic(field.getModifiers()) || 
                    field.getDeclaringClass() == Object.class) {
                    continue;
                }
                
                // Skip complex objects and collections
                if (!isSimpleType(field.getType())) {
                    continue;
                }
                
                attributes.add(field.getName());
            }
        } catch (Exception e) {
            System.err.println("Error discovering PriceData attributes: " + e.getMessage());
        }
        
        return Collections.unmodifiableSet(attributes);
    }
    
    /**
     * Discover AnalysisResult indicators using introspection
     */
    private static Set<String> discoverAnalysisResultIndicators() {
        Set<String> indicators = new HashSet<>();
        
        try {
            Field[] fields = AnalysisResult.class.getDeclaredFields();
            for (Field field : fields) {
                // Skip static fields, complex objects, and custom values
                if (Modifier.isStatic(field.getModifiers()) ||
                    field.getName().equals("customValues") ||
                    field.getName().equals("customIndicatorValues") ||
                    field.getName().equals("barSeries")) {
                    continue;
                }
                
                indicators.add(field.getName());
            }
        } catch (Exception e) {
            System.err.println("Error discovering AnalysisResult indicators: " + e.getMessage());
        }
        
        return Collections.unmodifiableSet(indicators);
    }
    
    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() || 
               type == String.class || 
               type == Double.class || 
               type == Double.TYPE ||
               type == Integer.class || 
               type == Integer.TYPE ||
               type == Long.class || 
               type == Long.TYPE ||
               type == Boolean.class || 
               type == Boolean.TYPE;
    }
    
    /**
     * Analyze all column names from the dashboard table and create mappings
     */
    public static Map<String, ColumnMapping> analyzeColumns(List<String> columnNames) {
        Map<String, ColumnMapping> mappings = new LinkedHashMap<>();
        
        for (String columnName : columnNames) {
            ColumnMapping mapping = analyzeColumn(columnName);
            if (mapping != null) {
                mappings.put(columnName, mapping);
            }
        }
        
        return Collections.unmodifiableMap(mappings);
    }
    
    /**
     * Analyze a single column name and create mapping
     */
    public static ColumnMapping analyzeColumn(String columnName) {
        // Check basic columns
        if (BASIC_COLUMNS.contains(columnName)) {
            return new ColumnMapping(columnName, ColumnType.BASIC, null, null, null, null, String.class);
        }
        
        // Check PriceData attributes
        if (PRICE_DATA_ATTRIBUTES.contains(columnName)) {
            Class<?> fieldType = getPriceDataFieldType(columnName);
            return new ColumnMapping(columnName, ColumnType.PRICE_DATA_ATTRIBUTE, null, null, null, columnName, fieldType);
        }
        
        // Check timeframe + indicator pattern (e.g., "30Min HeikenAshi")
        var timeframeMatcher = TIMEFRAME_INDICATOR_PATTERN.matcher(columnName);
        if (timeframeMatcher.matches()) {
            String timeframe = timeframeMatcher.group(1);
            String indicator = timeframeMatcher.group(2);
            return new ColumnMapping(columnName, ColumnType.TIMEFRAME_INDICATOR, timeframe, indicator, null, null, String.class);
        }
        
        // Check Z-Score rank pattern
        var zscoreRankMatcher = ZSCORE_RANK_PATTERN.matcher(columnName);
        if (zscoreRankMatcher.matches()) {
            String strategy = zscoreRankMatcher.group(1);
            return new ColumnMapping(columnName, ColumnType.ZSCORE_RANK, null, null, strategy, null, Integer.class);
        }
        
        // Check Z-Score value pattern
        var zscoreValueMatcher = ZSCORE_VALUE_PATTERN.matcher(columnName);
        if (zscoreValueMatcher.matches()) {
            String strategy = zscoreValueMatcher.group(1);
            return new ColumnMapping(columnName, ColumnType.ZSCORE_VALUE, null, null, strategy, null, Double.class);
        }
        
        // Check breakout count pattern
        var breakoutMatcher = BREAKOUT_COUNT_PATTERN.matcher(columnName);
        if (breakoutMatcher.matches()) {
            String indicator = breakoutMatcher.group(1);
            return new ColumnMapping(columnName, ColumnType.BREAKOUT_COUNT, null, indicator, null, null, Integer.class);
        }
        
        // Default to custom indicator
        return new ColumnMapping(columnName, ColumnType.CUSTOM_INDICATOR, null, columnName, null, null, String.class);
    }
    
    /**
     * Get the field type for a PriceData attribute
     */
    private static Class<?> getPriceDataFieldType(String fieldName) {
        try {
            Field field = PriceData.class.getDeclaredField(fieldName);
            return field.getType();
        } catch (Exception e) {
            return String.class; // Default fallback
        }
    }
    
    /**
     * Get all available PriceData attributes with their types
     */
    public static Map<String, Class<?>> getPriceDataAttributesWithTypes() {
        Map<String, Class<?>> attributes = new LinkedHashMap<>();
        
        for (String attribute : PRICE_DATA_ATTRIBUTES) {
            attributes.put(attribute, getPriceDataFieldType(attribute));
        }
        
        return Collections.unmodifiableMap(attributes);
    }
    
    /**
     * Get all available AnalysisResult indicators
     */
    public static Set<String> getAnalysisResultIndicators() {
        return ANALYSIS_RESULT_INDICATORS;
    }
    
    /**
     * Extract timeframes from column names
     */
    public static Set<String> extractTimeframes(List<String> columnNames) {
        return columnNames.stream()
            .map(TIMEFRAME_INDICATOR_PATTERN::matcher)
            .filter(java.util.regex.Matcher::matches)
            .map(matcher -> matcher.group(1))
            .collect(Collectors.toCollection(TreeSet::new));
    }
    
    /**
     * Extract indicators from column names
     */
    public static Set<String> extractIndicators(List<String> columnNames) {
        Set<String> indicators = new TreeSet<>();
        
        for (String columnName : columnNames) {
            var matcher = TIMEFRAME_INDICATOR_PATTERN.matcher(columnName);
            if (matcher.matches()) {
                indicators.add(matcher.group(2));
            }
        }
        
        return indicators;
    }
    
    /**
     * Extract Z-Score strategies from column names
     */
    public static Set<String> extractZScoreStrategies(List<String> columnNames) {
        Set<String> strategies = new TreeSet<>();
        
        for (String columnName : columnNames) {
            var rankMatcher = ZSCORE_RANK_PATTERN.matcher(columnName);
            var valueMatcher = ZSCORE_VALUE_PATTERN.matcher(columnName);
            
            if (rankMatcher.matches()) {
                strategies.add(rankMatcher.group(1));
            } else if (valueMatcher.matches()) {
                strategies.add(valueMatcher.group(1));
            }
        }
        
        return strategies;
    }
    
    /**
     * Generate a summary report of all column mappings
     */
    public static String generateColumnMappingReport(List<String> columnNames) {
        Map<String, ColumnMapping> mappings = analyzeColumns(columnNames);
        StringBuilder report = new StringBuilder();
        
        report.append("DASHBOARD COLUMN MAPPING REPORT\n");
        report.append("================================\n\n");
        report.append(String.format("Total Columns: %d\n", columnNames.size()));
        report.append(String.format("Mapped Columns: %d\n\n", mappings.size()));
        
        // Group by type
        Map<ColumnType, List<ColumnMapping>> byType = mappings.values().stream()
            .collect(Collectors.groupingBy(ColumnMapping::getType));
        
        for (ColumnType type : byType.keySet()) {
            report.append(String.format("%s COLUMNS (%d):\n", type, byType.get(type).size()));
            for (ColumnMapping mapping : byType.get(type)) {
                report.append(String.format("  %-30s", mapping.getColumnName()));
                
                switch (type) {
                    case TIMEFRAME_INDICATOR:
                        report.append(String.format(" -> %s.%s", mapping.getTimeframe(), mapping.getIndicator()));
                        break;
                    case ZSCORE_RANK:
                    case ZSCORE_VALUE:
                        report.append(String.format(" -> ZScoreResults.%s", mapping.getZscoreStrategy()));
                        break;
                    case PRICE_DATA_ATTRIBUTE:
                        report.append(String.format(" -> PriceData.%s", mapping.getPriceDataField()));
                        break;
                    case BASIC:
                        report.append(" -> Basic Table Column");
                        break;
                    case CUSTOM_INDICATOR:
                        report.append(" -> Custom Indicator");
                        break;
                }
                
                report.append(String.format(" [%s]\n", mapping.getFieldType().getSimpleName()));
            }
            report.append("\n");
        }
        
        // Statistics
        report.append("COLUMN TYPE STATISTICS:\n");
        for (ColumnType type : ColumnType.values()) {
            int count = byType.getOrDefault(type, Collections.emptyList()).size();
            report.append(String.format("  %-20s: %d columns\n", type, count));
        }
        
        return report.toString();
    }
    
    /**
     * Get column names by type
     */
    public static List<String> getColumnsByType(List<String> columnNames, ColumnType type) {
        Map<String, ColumnMapping> mappings = analyzeColumns(columnNames);
        return mappings.entrySet().stream()
            .filter(entry -> entry.getValue().getType() == type)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Check if a column is mapped to PriceData results
     */
    public static boolean isMappedToResults(String columnName) {
        ColumnMapping mapping = analyzeColumn(columnName);
        return mapping != null && mapping.getType() == ColumnType.TIMEFRAME_INDICATOR;
    }
    
    /**
     * Check if a column is mapped to Z-Score results
     */
    public static boolean isMappedToZScore(String columnName) {
        ColumnMapping mapping = analyzeColumn(columnName);
        return mapping != null && 
               (mapping.getType() == ColumnType.ZSCORE_RANK || 
                mapping.getType() == ColumnType.ZSCORE_VALUE);
    }
    
    /**
     * Refresh the attribute cache (useful if classes change at runtime)
     */
    public static void refreshAttributeCache() {
        // This would re-initialize the static sets
        // Note: In a real application, you might want to make this more sophisticated
        System.out.println("PriceData attribute cache refreshed");
    }
}