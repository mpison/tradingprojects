package com.quantlabs.QuantTester.tools.utils;

/**
 * Computes overnight swap (rollover) in quote‐currency per position,
 * supporting both a preconfigured daily rate and a passed‐in rate.
 */
public class SwapCalculator {

	 /**
     * Daily interest-rate differential (decimal) for long (BUY) positions.
     */
    private static double DAILY_RATE_DIFF_LONG = 0.0050 / 365;

    /**
     * Daily interest-rate differential (decimal) for short (SELL) positions.
     */
    private static double DAILY_RATE_DIFF_SHORT = 0.0025 / 365;

    /**
     * Set the daily rate differential for long positions.
     */
    public static void setDailyRateDiffLong(double dailyRateDiffLong) {
        DAILY_RATE_DIFF_LONG = dailyRateDiffLong;
    }

    /**
     * Set the daily rate differential for short positions.
     */
    public static void setDailyRateDiffShort(double dailyRateDiffShort) {
        DAILY_RATE_DIFF_SHORT = dailyRateDiffShort;
    }


    /**
     * Compute swap using configured daily rates for long vs. short.
     *
     * @param lotSize          number of lots (e.g. 1.0)
     * @param openPrice        FX rate (price of 1 base unit in quote currency)
     * @param contractSize     units per lot (e.g. 100_000)
     * @param isTripleRollover true for a 3-day rollover (e.g. Wednesday)
     * @param isBuy            true if it's a long (BUY) position, false for short (SELL)
     * @return swap amount in quote currency (positive = credit, negative = debit)
     */
    public static double computeSwapAmount(
            double lotSize,
            double openPrice,
            double contractSize,
            boolean isTripleRollover,
            boolean isBuy
    ) {
        double notional = lotSize * contractSize * openPrice;
        double rate = isBuy ? DAILY_RATE_DIFF_LONG : DAILY_RATE_DIFF_SHORT;
        double dailySwap = notional * rate;
        int days = isTripleRollover ? 3 : 1;
        double total = dailySwap * days;
        return -total;//isBuy ? total : -total;
    }

    /**
     * Compute swap using a provided rate differential, adjusting sign for long vs. short.
     *
     * @param lotSize          number of lots
     * @param openPrice        FX rate (price of 1 base unit in quote currency)
     * @param contractSize     units per lot
     * @param dailyRateDiff    daily interest-rate differential (decimal)
     * @param isTripleRollover true for a 3-day rollover
     * @param isBuy            true for long (BUY), false for short (SELL)
     * @return swap amount in quote currency
     */
    public static double computeSwapAmount(
            double lotSize,
            double openPrice,
            double contractSize,
            double dailyRateDiff,
            boolean isTripleRollover,
            boolean isBuy
    ) {
        double notional = lotSize * contractSize * openPrice;
        double dailySwap = notional * dailyRateDiff;
        int days = isTripleRollover ? 3 : 1;
        double total = dailySwap * days;
        return -total;//isBuy ? total : -total;
    }

    // Example usage
    public static void main(String[] args) {
        // Using configured rate:
    	setDailyRateDiffLong(0.0050 / 365); // e.g. (R_USD – R_EUR) = 1.00% annual
    	setDailyRateDiffShort(0.0025 / 365);
        
        double swap1 = computeSwapAmount(1.0, 1.1000, 100_000, true, true);
        System.out.printf("Swap (configured rate): %.4f quote-currency units%n", swap1);

        // Using passed‐in rate:
        double customRate = 0.0075 / 365; // e.g. 0.75% annual diff
        double swap2 = computeSwapAmount(1.0, 1.1000, 100_000, customRate, false, false);
        System.out.printf("Swap (passed-in rate): %.4f quote-currency units%n", swap2);
    }
}
