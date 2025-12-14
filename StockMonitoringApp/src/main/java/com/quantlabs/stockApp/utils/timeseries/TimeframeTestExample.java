package com.quantlabs.stockApp.utils.timeseries;

public class TimeframeTestExample {
    
    public static void main(String[] args) {
        System.out.println("=== Complete Timeframe Test ===");
        
        // Test all timeframes exist
        System.out.println("\nAll Timeframes:");
        for (Timeframe tf : Timeframe.values()) {
            System.out.println("- " + tf.name() + " (" + tf.getDisplayName() + "): " + 
                             tf.getDuration().toMinutes() + " minutes");
        }
        
        // Test ONE_MONTH specifically
        System.out.println("\n=== ONE_MONTH Tests ===");
        Timeframe oneMonth = Timeframe.ONE_MONTH;
        System.out.println("ONE_MONTH duration: " + oneMonth.getDuration().toDays() + " days");
        System.out.println("ONE_MONTH display name: " + oneMonth.getDisplayName());
        System.out.println("ONE_MONTH Alpaca name: " + oneMonth.getAlpacaName());
        System.out.println("ONE_MONTH Polygon name: " + oneMonth.getPolygonName());
        System.out.println("ONE_MONTH Yahoo name: " + oneMonth.getYahooName());
        System.out.println("ONE_MONTH TradingView name: " + oneMonth.getTradingViewName());
        
        // Test conversions to ONE_MONTH
        System.out.println("\n=== Conversions to ONE_MONTH ===");
        testConversion("1Month", Timeframe::fromAlpaca);
        testConversion("1month", Timeframe::fromPolygon);
        testConversion("1mo", Timeframe::fromYahoo);
        testConversion("M", Timeframe::fromTradingView);
        testConversion("30d", Timeframe::fromAlias);
        
        // Test THREE_MONTHS
        System.out.println("\n=== THREE_MONTHS Tests ===");
        Timeframe threeMonths = Timeframe.THREE_MONTHS;
        System.out.println("THREE_MONTHS duration: " + threeMonths.getDuration().toDays() + " days");
        System.out.println("THREE_MONTHS display name: " + threeMonths.getDisplayName());
        System.out.println("THREE_MONTHS Alpaca name: " + threeMonths.getAlpacaName());
        System.out.println("THREE_MONTHS Yahoo name: " + threeMonths.getYahooName());
        
        // Test ONE_YEAR
        System.out.println("\n=== ONE_YEAR Tests ===");
        Timeframe oneYear = Timeframe.ONE_YEAR;
        System.out.println("ONE_YEAR duration: " + oneYear.getDuration().toDays() + " days");
        System.out.println("ONE_YEAR display name: " + oneYear.getDisplayName());
        System.out.println("ONE_YEAR Alpaca name: " + oneYear.getAlpacaName());
        System.out.println("ONE_YEAR Yahoo name: " + oneYear.getYahooName());
        
        // Test aggregation factors
        System.out.println("\n=== Aggregation Tests ===");
        testAggregation(Timeframe.ONE_MINUTE, Timeframe.FIVE_MINUTES, 5);
        testAggregation(Timeframe.ONE_MINUTE, Timeframe.ONE_HOUR, 60);
        testAggregation(Timeframe.ONE_MINUTE, Timeframe.FOUR_HOURS, 240);
        testAggregation(Timeframe.ONE_HOUR, Timeframe.ONE_DAY, 24);
        
        // Test monthly aggregation (should fail since it's approximate)
        try {
            testAggregation(Timeframe.ONE_DAY, Timeframe.ONE_MONTH, 30);
        } catch (IllegalArgumentException e) {
            System.out.println("Expected error for ONE_DAY -> ONE_MONTH: " + e.getMessage());
        }
        
        // Test utility methods
        System.out.println("\n=== Utility Methods ===");
        System.out.println("Intraday timeframes: " + 
            java.util.Arrays.stream(Timeframe.getIntradayTimeframes())
                .map(Timeframe::getDisplayName)
                .collect(java.util.stream.Collectors.joining(", ")));
        
        System.out.println("Daily+ timeframes: " + 
            java.util.Arrays.stream(Timeframe.getDailyAndLargerTimeframes())
                .map(Timeframe::getDisplayName)
                .collect(java.util.stream.Collectors.joining(", ")));
    }
    
    private static void testConversion(String input, java.util.function.Function<String, Timeframe> converter) {
        try {
            Timeframe tf = converter.apply(input);
            System.out.println(input + " -> " + tf.getDisplayName());
        } catch (Exception e) {
            System.out.println(input + " -> ERROR: " + e.getMessage());
        }
    }
    
    private static void testAggregation(Timeframe source, Timeframe target, int expectedFactor) {
        try {
            boolean canAggregate = target.canBeAggregatedFrom(source);
            if (canAggregate) {
                int factor = target.getAggregationFactor(source);
                System.out.println(target.getDisplayName() + " can be aggregated from " + 
                    source.getDisplayName() + " with factor " + factor + 
                    " (expected: " + expectedFactor + ")");
                if (factor == expectedFactor) {
                    System.out.println("  ✓ Correct!");
                } else {
                    System.out.println("  ✗ Incorrect!");
                }
            } else {
                System.out.println(target.getDisplayName() + " cannot be aggregated from " + 
                    source.getDisplayName());
            }
        } catch (Exception e) {
            System.out.println("Error testing " + target + " from " + source + ": " + e.getMessage());
        }
    }
}