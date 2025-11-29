package com.quantlabs.stockApp.core.indicators;

import org.ta4j.core.*;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.*;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.*;
import java.time.ZonedDateTime;
import java.util.List;

public class AdvancedVWAPStrategy {
    
    // Price Source Enum
    public enum PriceSource {
        CLOSE, OPEN, HIGH, LOW, HL2, HLC3, OHLC4, TYPICAL
    }
    
    // Anchor Period Enum
    public enum AnchorPeriod {
        SESSION, WEEK, MONTH, QUARTER, YEAR, DECADE, CENTURY, CUSTOM
    }
    
    // 1. Advanced VWAP Indicator with Flexible Configuration
    public static class AdvancedVWAPIndicator extends CachedIndicator<Num> {
        private final PriceSource priceSource;
        private final AnchorPeriod anchorPeriod;
        private final int customPeriod;
        private final BarSeries barSeries;
        
        public AdvancedVWAPIndicator(BarSeries series) {
            this(series, PriceSource.CLOSE, AnchorPeriod.SESSION, 0);
        }
        
        public AdvancedVWAPIndicator(BarSeries series, PriceSource priceSource, 
                                   AnchorPeriod anchorPeriod, int customPeriod) {
            super(series);
            this.barSeries = series;
            this.priceSource = priceSource;
            this.anchorPeriod = anchorPeriod;
            this.customPeriod = customPeriod;
        }
        
        @Override
        protected Num calculate(int index) {
            int startIndex = calculateStartIndex(index);
            Num cumulativePriceVolume = getBarSeries().numOf(0);
            Num cumulativeVolume = getBarSeries().numOf(0);
            
            for (int i = startIndex; i <= index; i++) {
                Num price = getPriceForSource(i, priceSource);
                Num volume = getBarSeries().getBar(i).getVolume();
                
                cumulativePriceVolume = cumulativePriceVolume.plus(price.multipliedBy(volume));
                cumulativeVolume = cumulativeVolume.plus(volume);
            }
            
            return cumulativeVolume.isZero() ? getPriceForSource(index, priceSource) : 
                   cumulativePriceVolume.dividedBy(cumulativeVolume);
        }
        
        public int calculateStartIndex(int currentIndex) {
            switch (anchorPeriod) {
                case SESSION:
                    return findSessionStart(currentIndex);
                case WEEK:
                    return findWeekStart(currentIndex);
                case MONTH:
                    return findMonthStart(currentIndex);
                case QUARTER:
                    return findQuarterStart(currentIndex);
                case YEAR:
                    return findYearStart(currentIndex);
                case DECADE:
                    return findDecadeStart(currentIndex);
                case CENTURY:
                    return findCenturyStart(currentIndex);
                case CUSTOM:
                    return Math.max(0, currentIndex - customPeriod + 1);
                default:
                    return 0;
            }
        }
        
        private Num getPriceForSource(int index, PriceSource source) {
            Bar bar = getBarSeries().getBar(index);
            switch (source) {
                case OPEN:
                    return bar.getOpenPrice();
                case HIGH:
                    return bar.getHighPrice();
                case LOW:
                    return bar.getLowPrice();
                case CLOSE:
                    return bar.getClosePrice();
                case HL2:
                    return bar.getHighPrice().plus(bar.getLowPrice()).dividedBy(getBarSeries().numOf(2));
                case HLC3:
                    return bar.getHighPrice().plus(bar.getLowPrice()).plus(bar.getClosePrice())
                             .dividedBy(getBarSeries().numOf(3));
                case OHLC4:
                    return bar.getOpenPrice().plus(bar.getHighPrice())
                             .plus(bar.getLowPrice()).plus(bar.getClosePrice())
                             .dividedBy(getBarSeries().numOf(4));
                case TYPICAL:
                default:
                    return bar.getHighPrice().plus(bar.getLowPrice()).plus(bar.getClosePrice())
                             .dividedBy(getBarSeries().numOf(3));
            }
        }
        
        // Anchor Period Calculation Methods
        private int findSessionStart(int currentIndex) {
            return Math.max(0, currentIndex - 100);
        }
        
        private int findWeekStart(int currentIndex) {
            if (barSeries.getBarCount() == 0) return 0;
            
            ZonedDateTime currentTime = barSeries.getBar(currentIndex).getEndTime();
            ZonedDateTime weekStart = currentTime.minusDays(currentTime.getDayOfWeek().getValue() - 1)
                                              .withHour(0).withMinute(0).withSecond(0).withNano(0);
            return findIndexForTimestamp(weekStart, currentIndex);
        }
        
        private int findMonthStart(int currentIndex) {
            if (barSeries.getBarCount() == 0) return 0;
            
            ZonedDateTime currentTime = barSeries.getBar(currentIndex).getEndTime();
            ZonedDateTime monthStart = currentTime.withDayOfMonth(1)
                                                .withHour(0).withMinute(0).withSecond(0).withNano(0);
            return findIndexForTimestamp(monthStart, currentIndex);
        }
        
        private int findQuarterStart(int currentIndex) {
            if (barSeries.getBarCount() == 0) return 0;
            
            ZonedDateTime currentTime = barSeries.getBar(currentIndex).getEndTime();
            int month = currentTime.getMonthValue();
            int quarterStartMonth = ((month - 1) / 3) * 3 + 1;
            ZonedDateTime quarterStart = currentTime.withMonth(quarterStartMonth).withDayOfMonth(1)
                                                  .withHour(0).withMinute(0).withSecond(0).withNano(0);
            return findIndexForTimestamp(quarterStart, currentIndex);
        }
        
        private int findYearStart(int currentIndex) {
            if (barSeries.getBarCount() == 0) return 0;
            
            ZonedDateTime currentTime = barSeries.getBar(currentIndex).getEndTime();
            ZonedDateTime yearStart = currentTime.withDayOfYear(1)
                                               .withHour(0).withMinute(0).withSecond(0).withNano(0);
            return findIndexForTimestamp(yearStart, currentIndex);
        }
        
        private int findDecadeStart(int currentIndex) {
            if (barSeries.getBarCount() == 0) return 0;
            
            ZonedDateTime currentTime = barSeries.getBar(currentIndex).getEndTime();
            int year = currentTime.getYear();
            int decadeStartYear = (year / 10) * 10;
            ZonedDateTime decadeStart = currentTime.withYear(decadeStartYear).withDayOfYear(1)
                                                 .withHour(0).withMinute(0).withSecond(0).withNano(0);
            return findIndexForTimestamp(decadeStart, currentIndex);
        }
        
        private int findCenturyStart(int currentIndex) {
            if (barSeries.getBarCount() == 0) return 0;
            
            ZonedDateTime currentTime = barSeries.getBar(currentIndex).getEndTime();
            int year = currentTime.getYear();
            int centuryStartYear = (year / 100) * 100 + 1;
            ZonedDateTime centuryStart = currentTime.withYear(centuryStartYear).withDayOfYear(1)
                                                  .withHour(0).withMinute(0).withSecond(0).withNano(0);
            return findIndexForTimestamp(centuryStart, currentIndex);
        }
        
        private int findIndexForTimestamp(ZonedDateTime timestamp, int maxIndex) {
            for (int i = maxIndex; i >= 0; i--) {
                if (!barSeries.getBar(i).getEndTime().isBefore(timestamp)) {
                    return i;
                }
            }
            return 0;
        }
    }
    
    // 2. Enhanced VWAP Bands with Same Configuration
    public static class AdvancedVWAPBandsIndicator extends CachedIndicator<Num> {
        private final AdvancedVWAPIndicator vwap;
        private final PriceSource priceSource;
        private final double stdDevMultiplier;
        private final boolean upperBand;
        
        public AdvancedVWAPBandsIndicator(BarSeries series, PriceSource priceSource,
                                        AnchorPeriod anchorPeriod, int customPeriod,
                                        double stdDevMultiplier, boolean upperBand) {
            super(series);
            this.vwap = new AdvancedVWAPIndicator(series, priceSource, anchorPeriod, customPeriod);
            this.priceSource = priceSource;
            this.stdDevMultiplier = upperBand ? stdDevMultiplier : -stdDevMultiplier;
            this.upperBand = upperBand;
        }
        
        @Override
        protected Num calculate(int index) {
            Num vwapValue = vwap.getValue(index);
            Num stdDev = calculateStandardDeviation(index);
            
            return vwapValue.plus(stdDev.multipliedBy(getBarSeries().numOf(stdDevMultiplier)));
        }
        
        private Num calculateStandardDeviation(int index) {
            Num mean = vwap.getValue(index);
            Num sumSquaredDifferences = getBarSeries().numOf(0);
            int count = 0;
            
            int startIndex = vwap.calculateStartIndex(index);
            
            for (int i = startIndex; i <= index; i++) {
                Num price = getPriceForSource(i, priceSource);
                Num difference = price.minus(mean);
                sumSquaredDifferences = sumSquaredDifferences.plus(difference.pow(2));
                count++;
            }
            
            if (count == 0) return getBarSeries().numOf(0);
            
            Num variance = sumSquaredDifferences.dividedBy(getBarSeries().numOf(count));
            return variance.sqrt();
        }
        
        private Num getPriceForSource(int index, PriceSource source) {
            Bar bar = getBarSeries().getBar(index);
            switch (source) {
                case OPEN: return bar.getOpenPrice();
                case HIGH: return bar.getHighPrice();
                case LOW: return bar.getLowPrice();
                case CLOSE: return bar.getClosePrice();
                case HL2: return bar.getHighPrice().plus(bar.getLowPrice()).dividedBy(getBarSeries().numOf(2));
                case HLC3: return bar.getHighPrice().plus(bar.getLowPrice()).plus(bar.getClosePrice()).dividedBy(getBarSeries().numOf(3));
                case OHLC4: return bar.getOpenPrice().plus(bar.getHighPrice()).plus(bar.getLowPrice()).plus(bar.getClosePrice()).dividedBy(getBarSeries().numOf(4));
                case TYPICAL:
                default: return bar.getHighPrice().plus(bar.getLowPrice()).plus(bar.getClosePrice()).dividedBy(getBarSeries().numOf(3));
            }
        }
    }
    
    // 3. Strategy Builder with Flexible Configuration
    public static class VWAPStrategyBuilder {
        private BarSeries series;
        private PriceSource priceSource = PriceSource.TYPICAL;
        private AnchorPeriod anchorPeriod = AnchorPeriod.SESSION;
        private int customPeriod = 20;
        private double stdDevMultiplier = 1.0;
        private double stopLossPercent = 0.02;
        private double takeProfitPercent = 0.05;
        
        public VWAPStrategyBuilder(BarSeries series) {
            this.series = series;
        }
        
        public VWAPStrategyBuilder withPriceSource(PriceSource priceSource) {
            this.priceSource = priceSource;
            return this;
        }
        
        public VWAPStrategyBuilder withAnchorPeriod(AnchorPeriod anchorPeriod) {
            this.anchorPeriod = anchorPeriod;
            return this;
        }
        
        public VWAPStrategyBuilder withCustomPeriod(int customPeriod) {
            this.customPeriod = customPeriod;
            this.anchorPeriod = AnchorPeriod.CUSTOM;
            return this;
        }
        
        public VWAPStrategyBuilder withStdDevMultiplier(double multiplier) {
            this.stdDevMultiplier = multiplier;
            return this;
        }
        
        public VWAPStrategyBuilder withRiskManagement(double stopLoss, double takeProfit) {
            this.stopLossPercent = stopLoss;
            this.takeProfitPercent = takeProfit;
            return this;
        }
        
        public Strategy build() {
            AdvancedVWAPIndicator vwap = new AdvancedVWAPIndicator(
                series, priceSource, anchorPeriod, customPeriod);
            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            
            AdvancedVWAPBandsIndicator upperBand = new AdvancedVWAPBandsIndicator(
                series, priceSource, anchorPeriod, customPeriod, stdDevMultiplier, true);
            AdvancedVWAPBandsIndicator lowerBand = new AdvancedVWAPBandsIndicator(
                series, priceSource, anchorPeriod, customPeriod, stdDevMultiplier, false);
            
            // Trading Rules
            Rule entryRule = 
                new UnderIndicatorRule(closePrice, lowerBand)
                .and(new OverIndicatorRule(closePrice, vwap))
                .or(
                    new UnderIndicatorRule(closePrice, vwap)
                    .and(new OverIndicatorRule(closePrice, lowerBand))
                    .and(new StopLossRule(closePrice, series.numOf(1 - stopLossPercent)))
                );
            
            Rule exitRule = 
                new OverIndicatorRule(closePrice, upperBand)
                .or(new UnderIndicatorRule(closePrice, vwap))
                .or(new StopGainRule(closePrice, series.numOf(1 + takeProfitPercent)));
            
            return new BaseStrategy(entryRule, exitRule);
        }
    }
    
    // 4. Analysis Helper Class
    public static class VWAPAnalysis {
        public static void analyzeStrategy(String name, TradingRecord tradingRecord, BarSeries series) {
            System.out.println("\n=== " + name + " ANALYSIS ===");
            
            List<Position> positions = tradingRecord.getPositions();
            int tradeCount = positions.size();
            System.out.println("Total Positions: " + tradeCount);
            
            if (tradeCount > 0) {
                int winningTrades = 0;
                Num totalProfit = series.numOf(0);
                
                for (Position position : positions) {
                    if (position.getProfit().isPositive()) {
                        winningTrades++;
                    }
                    totalProfit = totalProfit.plus(position.getProfit());
                }
                
                double winRate = (double) winningTrades / tradeCount * 100;
                System.out.printf("Win Rate: %.2f%%%n", winRate);
                System.out.println("Total Profit: " + totalProfit);
                System.out.println("Average Profit per Trade: " + totalProfit.dividedBy(series.numOf(tradeCount)));
            }
        }
        
        public static void printCurrentVWAPLevels(BarSeries series, PriceSource priceSource, AnchorPeriod period) {
            if (series.getBarCount() == 0) return;
            
            int lastIndex = series.getEndIndex();
            AdvancedVWAPIndicator vwap = new AdvancedVWAPIndicator(series, priceSource, period, 0);
            AdvancedVWAPBandsIndicator upperBand = new AdvancedVWAPBandsIndicator(
                series, priceSource, period, 0, 1.0, true);
            AdvancedVWAPBandsIndicator lowerBand = new AdvancedVWAPBandsIndicator(
                series, priceSource, period, 0, 1.0, false);
            
            System.out.printf("%s VWAP (%s): %.4f | Upper: %.4f | Lower: %.4f%n",
                period, priceSource, 
                vwap.getValue(lastIndex).doubleValue(),
                upperBand.getValue(lastIndex).doubleValue(),
                lowerBand.getValue(lastIndex).doubleValue());
        }
        
        public static void compareAllPriceSources(BarSeries series, AnchorPeriod period) {
            System.out.println("\n=== VWAP COMPARISON BY PRICE SOURCE ===");
            for (PriceSource source : PriceSource.values()) {
                printCurrentVWAPLevels(series, source, period);
            }
        }
        
        public static void compareAllAnchorPeriods(BarSeries series, PriceSource priceSource) {
            System.out.println("\n=== VWAP COMPARISON BY ANCHOR PERIOD ===");
            for (AnchorPeriod period : AnchorPeriod.values()) {
                if (period != AnchorPeriod.CUSTOM) {
                    printCurrentVWAPLevels(series, priceSource, period);
                }
            }
        }
    }
    
    // 5. Comprehensive Usage Example
    public static void main(String[] args) {
        BarSeries series = loadYourPriceData();
        
        // Example strategies with different configurations
        Strategy dailyStrategy = new VWAPStrategyBuilder(series)
            .withPriceSource(PriceSource.TYPICAL)
            .withAnchorPeriod(AnchorPeriod.SESSION)
            .withStdDevMultiplier(1.0)
            .withRiskManagement(0.02, 0.05)
            .build();
        
        Strategy weeklyStrategy = new VWAPStrategyBuilder(series)
            .withPriceSource(PriceSource.OHLC4)
            .withAnchorPeriod(AnchorPeriod.WEEK)
            .withStdDevMultiplier(2.0)
            .withRiskManagement(0.015, 0.08)
            .build();
        
        Strategy customStrategy = new VWAPStrategyBuilder(series)
            .withPriceSource(PriceSource.HLC3)
            .withCustomPeriod(50)
            .withStdDevMultiplier(1.5)
            .build();
        
        // Run strategies
        BarSeriesManager manager = new BarSeriesManager(series);
        TradingRecord dailyRecord = manager.run(dailyStrategy);
        TradingRecord weeklyRecord = manager.run(weeklyStrategy);
        TradingRecord customRecord = manager.run(customStrategy);
        
        // Analyze results
        VWAPAnalysis.analyzeStrategy("Daily VWAP Strategy", dailyRecord, series);
        VWAPAnalysis.analyzeStrategy("Weekly VWAP Strategy", weeklyRecord, series);
        VWAPAnalysis.analyzeStrategy("Custom 50-period VWAP Strategy", customRecord, series);
        
        // Print comprehensive comparisons
        VWAPAnalysis.compareAllPriceSources(series, AnchorPeriod.SESSION);
        VWAPAnalysis.compareAllAnchorPeriods(series, PriceSource.TYPICAL);
    }
    
    // Mock data loader
    private static BarSeries loadYourPriceData() {
        BarSeries series = new BaseBarSeriesBuilder().withName("Sample").build();
        // Add your actual bar data here
        return series;
    }
}