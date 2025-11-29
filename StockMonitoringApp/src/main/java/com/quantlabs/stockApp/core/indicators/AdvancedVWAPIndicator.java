package com.quantlabs.stockApp.core.indicators;

import org.ta4j.core.*;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;
import java.time.ZonedDateTime;

//TODO: check if use in the project then delete if not
public class AdvancedVWAPIndicator extends CachedIndicator<Num> {
    
    public enum PriceSource {
        CLOSE, OPEN, HIGH, LOW, HL2, HLC3, OHLC4, TYPICAL
    }
    
    public enum AnchorPeriod {
        SESSION, WEEK, MONTH, QUARTER, YEAR, DECADE, CENTURY, CUSTOM
    }
    
    private final PriceSource priceSource;
    private final AnchorPeriod anchorPeriod;
    private final int customPeriod;
    private final BarSeries barSeries;
    
    public AdvancedVWAPIndicator(BarSeries series) {
        this(series, PriceSource.TYPICAL, AnchorPeriod.SESSION, 0);
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
    
    // Anchor Period Calculation Methods (same as before)
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