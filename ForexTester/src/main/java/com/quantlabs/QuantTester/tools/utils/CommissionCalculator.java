package com.quantlabs.QuantTester.tools.utils;

/**
 * CommissionCalculator.java
 * Utility to compute commission costs for forex trades.
 */
public class CommissionCalculator {

    /**
     * Compute commission based on a fixed commission per lot.
     *
     * @param lotSize          number of lots (e.g. 1.0)
     * @param commissionPerLot commission charged per lot in quote currency
     * @return total commission cost in quote currency
     */
    public static double computeCommissionPerLot(
            double lotSize,
            double commissionPerLot
    ) {
        return lotSize * commissionPerLot;
    }

    /**
     * Compute commission as a percentage of the notional value.
     *
     * @param lotSize               number of lots
     * @param openPrice             FX rate (price of 1 base unit in quote currency)
     * @param contractSize          units per lot
     * @param commissionRatePercent commission rate as a percentage (e.g. 0.02 for 2%)
     * @return total commission cost in quote currency
     */
    public static double computeCommissionFromPercentage(
            double lotSize,
            double openPrice,
            double contractSize,
            double commissionRatePercent
    ) {
        double notional = lotSize * contractSize * openPrice;
        return notional * (commissionRatePercent / 100.0);
    }
}