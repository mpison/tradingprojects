package com.quantlabs.stockApp.model;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dynamic column registry for PriceData using introspection
 * Automatically discovers and categorizes all fields
 */
public class PriceDataColumnRegistry {
    
    // Basic table columns (always displayed)
    public static final String SELECT = "Select";
    public static final String SYMBOL = "Symbol";
    public static final String LATEST_PRICE = "Latest Price";
    public static final String PREV_VOL = "Prev Vol";
    public static final String CURRENT_VOL = "Current Vol";
    public static final String PERCENT_CHANGE = "% Change";
    
    public static final List<String> BASIC_COLUMNS = Arrays.asList(
        SELECT, SYMBOL, LATEST_PRICE, PREV_VOL, CURRENT_VOL, PERCENT_CHANGE
    );
    
    // Field categories discovered via introspection
    private static final Map<String, List<FieldInfo>> FIELD_CATEGORIES = new LinkedHashMap<>();
    private static final Map<String, FieldInfo> ALL_FIELDS = new LinkedHashMap<>();
    private static final List<String> DISPLAYABLE_FIELDS = new ArrayList<>();
    private static final List<String> NUMERIC_FILTER_FIELDS = new ArrayList<>();
    
    static {
        initializeFieldRegistry();
    }
    
    /**
     * Field metadata container
     */
    public static class FieldInfo {
        private final String name;
        private final Class<?> type;
        private final String category;
        private final String displayName;
        private final boolean isNumeric;
        private final boolean isFilterable;
        
        public FieldInfo(String name, Class<?> type, String category, String displayName) {
            this.name = name;
            this.type = type;
            this.category = category;
            this.displayName = displayName != null ? displayName : name;
            this.isNumeric = isNumericType(type);
            this.isFilterable = isNumeric && !isSpecialField(name);
        }
        
        private boolean isNumericType(Class<?> type) {
            return type == double.class || type == Double.class ||
                   type == int.class || type == Integer.class ||
                   type == long.class || type == Long.class ||
                   type == float.class || type == Float.class;
        }
        
        private boolean isSpecialField(String fieldName) {
            return fieldName.equals("ticker") || 
                   fieldName.contains("typespecs") || 
                   fieldName.contains("indexes") ||
                   fieldName.contains("results") ||
                   fieldName.contains("zScoreResults");
        }
        
        // Getters
        public String getName() { return name; }
        public Class<?> getType() { return type; }
        public String getCategory() { return category; }
        public String getDisplayName() { return displayName; }
        public boolean isNumeric() { return isNumeric; }
        public boolean isFilterable() { return isFilterable; }
        
        @Override
        public String toString() {
            return String.format("FieldInfo{name='%s', type=%s, category='%s', numeric=%s, filterable=%s}",
                name, type.getSimpleName(), category, isNumeric, isFilterable);
        }
    }
    
    /**
     * Initialize the field registry using introspection
     */
    private static void initializeFieldRegistry() {
        FIELD_CATEGORIES.clear();
        ALL_FIELDS.clear();
        DISPLAYABLE_FIELDS.clear();
        NUMERIC_FILTER_FIELDS.clear();
        
        // Analyze PriceData class structure
        analyzePriceDataClass();
        
        // Build derived collections
        buildDerivedCollections();
    }
    
    /**
     * Analyze PriceData class using reflection
     */
    private static void analyzePriceDataClass() {
        try {
            Class<?> priceDataClass = PriceData.class;
            Field[] fields = priceDataClass.getDeclaredFields();
            
            for (Field field : fields) {
                // Skip static fields and fields from Object class
                if (Modifier.isStatic(field.getModifiers()) || 
                    field.getDeclaringClass() == Object.class) {
                    continue;
                }
                
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                String category = categorizeField(fieldName, fieldType);
                String displayName = generateDisplayName(fieldName);
                
                FieldInfo fieldInfo = new FieldInfo(fieldName, fieldType, category, displayName);
                
                // Store in categories
                FIELD_CATEGORIES.computeIfAbsent(category, k -> new ArrayList<>()).add(fieldInfo);
                ALL_FIELDS.put(fieldName, fieldInfo);
                
                // Add to appropriate lists
                if (isDisplayableField(fieldInfo)) {
                    DISPLAYABLE_FIELDS.add(fieldName);
                }
                if (fieldInfo.isFilterable()) {
                    NUMERIC_FILTER_FIELDS.add(fieldName);
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize PriceData column registry", e);
        }
    }
    
    /**
     * Categorize fields based on name patterns and types
     */
    private static String categorizeField(String fieldName, Class<?> fieldType) {
        // Price-related fields
        if (fieldName.toLowerCase().contains("price") || 
            fieldName.toLowerCase().contains("close") ||
            fieldName.toLowerCase().contains("high") ||
            fieldName.toLowerCase().contains("low") ||
            fieldName.toLowerCase().contains("open") ||
            fieldName.equals("latestPrice") ||
            fieldName.equals("gap")) {
            return "PRICE";
        }
        
        // Volume-related fields
        if (fieldName.toLowerCase().contains("volume") || 
            fieldName.toLowerCase().contains("vol")) {
            return "VOLUME";
        }
        
        // Percentile fields
        if (fieldName.toLowerCase().contains("percentile")) {
            return "PERCENTILE";
        }
        
        // Change fields
        if (fieldName.toLowerCase().contains("change")) {
            return "CHANGE";
        }
        
        // Metadata fields
        if (fieldName.equals("ticker") || fieldName.equals("name") || 
            fieldName.equals("description") || fieldName.equals("exchange") ||
            fieldName.equals("currency") || fieldName.equals("analystRating") ||
            fieldName.equals("type") || fieldName.equals("updateMode")) {
            return "METADATA";
        }
        
        // Technical metadata
        if (fieldName.equals("pricescale") || fieldName.equals("minmov") ||
            fieldName.equals("fractional") || fieldName.equals("minmove2")) {
            return "TECHNICAL";
        }
        
        // Collection fields
        if (fieldName.equals("typespecs") || fieldName.equals("indexes") ||
            fieldName.equals("results") || fieldName.equals("zScoreResults")) {
            return "COLLECTIONS";
        }
        
        // Default category
        return "OTHER";
    }
    
    /**
     * Generate user-friendly display names
     */
    private static String generateDisplayName(String fieldName) {
        // Handle camelCase conversion
        String displayName = fieldName.replaceAll("([A-Z])", " $1");
        displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
        
        // Special cases
        if (fieldName.equals("premarketClose")) return "Premarket Close";
        if (fieldName.equals("postmarketClose")) return "Postmarket Close";
        if (fieldName.equals("prevLastDayPrice")) return "Previous Day Price";
        if (fieldName.equals("averageVol")) return "Average Volume";
        if (fieldName.equals("analystRating")) return "Analyst Rating";
        
        return displayName;
    }
    
    /**
     * Determine if field should be displayed as a column
     */
    private static boolean isDisplayableField(FieldInfo fieldInfo) {
        return !fieldInfo.getCategory().equals("COLLECTIONS") && 
               !fieldInfo.getName().equals("ticker"); // Ticker is handled by SYMBOL column
    }
    
    /**
     * Build derived collections after introspection
     */
    private static void buildDerivedCollections() {
        // Sort categories for consistent ordering
        for (List<FieldInfo> fields : FIELD_CATEGORIES.values()) {
            fields.sort(Comparator.comparing(FieldInfo::getName));
        }
    }
    
    // Public API Methods
    
    /**
     * Get all fields organized by category
     */
    public static Map<String, List<FieldInfo>> getFieldsByCategory() {
        return Collections.unmodifiableMap(FIELD_CATEGORIES);
    }
    
    /**
     * Get all fields as a flat list
     */
    public static List<FieldInfo> getAllFields() {
        return new ArrayList<>(ALL_FIELDS.values());
    }
    
    /**
     * Get fields for a specific category
     */
    public static List<FieldInfo> getFieldsForCategory(String category) {
        return Collections.unmodifiableList(FIELD_CATEGORIES.getOrDefault(category, new ArrayList<>()));
    }
    
    /**
     * Get all displayable field names (for table columns)
     */
    public static List<String> getDisplayableFieldNames() {
        return Collections.unmodifiableList(DISPLAYABLE_FIELDS);
    }
    
    /**
     * Get all numeric filterable field names
     */
    public static List<String> getNumericFilterFieldNames() {
        return Collections.unmodifiableList(NUMERIC_FILTER_FIELDS);
    }
    
    /**
     * Get field info by name
     */
    public static FieldInfo getFieldInfo(String fieldName) {
        return ALL_FIELDS.get(fieldName);
    }
    
    /**
     * Check if a field exists
     */
    public static boolean hasField(String fieldName) {
        return ALL_FIELDS.containsKey(fieldName);
    }
    
    /**
     * Check if a field is numeric
     */
    public static boolean isNumericField(String fieldName) {
        FieldInfo info = ALL_FIELDS.get(fieldName);
        return info != null && info.isNumeric();
    }
    
    /**
     * Check if a field is filterable
     */
    public static boolean isFilterableField(String fieldName) {
        FieldInfo info = ALL_FIELDS.get(fieldName);
        return info != null && info.isFilterable();
    }
    
    /**
     * Get the display name for a field
     */
    public static String getDisplayName(String fieldName) {
        FieldInfo info = ALL_FIELDS.get(fieldName);
        return info != null ? info.getDisplayName() : fieldName;
    }
    
    /**
     * Get all categories
     */
    public static Set<String> getCategories() {
        return Collections.unmodifiableSet(FIELD_CATEGORIES.keySet());
    }
    
    /**
     * Refresh the registry (useful if PriceData class changes at runtime)
     */
    public static void refresh() {
        initializeFieldRegistry();
    }
    
    /**
     * Get fields matching a pattern in their names
     */
    public static List<FieldInfo> findFields(String pattern) {
        String lowerPattern = pattern.toLowerCase();
        return ALL_FIELDS.values().stream()
            .filter(field -> field.getName().toLowerCase().contains(lowerPattern) ||
                            field.getDisplayName().toLowerCase().contains(lowerPattern))
            .collect(Collectors.toList());
    }
    
    /**
     * Get fields of a specific type
     */
    public static List<FieldInfo> getFieldsByType(Class<?> type) {
        return ALL_FIELDS.values().stream()
            .filter(field -> field.getType().equals(type))
            .collect(Collectors.toList());
    }
    
    /**
     * Print field registry summary (for debugging)
     */
    public static void printRegistrySummary() {
        System.out.println("=== PriceData Column Registry Summary ===");
        System.out.println("Total fields: " + ALL_FIELDS.size());
        System.out.println("Displayable fields: " + DISPLAYABLE_FIELDS.size());
        System.out.println("Numeric filter fields: " + NUMERIC_FILTER_FIELDS.size());
        
        for (Map.Entry<String, List<FieldInfo>> entry : FIELD_CATEGORIES.entrySet()) {
            System.out.println("\nCategory: " + entry.getKey() + " (" + entry.getValue().size() + " fields)");
            for (FieldInfo field : entry.getValue()) {
                System.out.println("  - " + field.getName() + " [" + field.getType().getSimpleName() + 
                                 "] - Display: '" + field.getDisplayName() + "'" +
                                 (field.isNumeric() ? " [NUMERIC]" : "") +
                                 (field.isFilterable() ? " [FILTERABLE]" : ""));
            }
        }
    }
}