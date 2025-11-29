package com.quantlabs.stockApp.utils;

import java.time.Duration;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.Num;

public class WickCleanerUtil {
	
	
	static double maxWickRatio = 0.05; // allow wicks up to 5% of candle body
	
	public static BarSeries cleanBarSeries(BarSeries series) {
		
		return cleanBarSeries(series, maxWickRatio);
	}
	
	/**
     * Cleans abnormal wicks in a TA4J BarSeries by trimming unrealistic highs/lows.
     *
     * @param series        The original BarSeries (TA4J)
     * @param maxWickRatio  Maximum allowed wick size relative to the candle body (e.g., 0.05 = 5%)
     * @return BarSeries    A new BarSeries with cleaned highs/lows
     */
    public static BarSeries cleanBarSeries(BarSeries series, double maxWickRatio) {
        BarSeries cleanedSeries = new BaseBarSeries(series.getName() + "-cleaned");

        for (int i = 0; i < series.getBarCount(); i++) {
            Bar bar = series.getBar(i);

            double open = bar.getOpenPrice().doubleValue();
            double high = bar.getHighPrice().doubleValue();
            double low = bar.getLowPrice().doubleValue();
            double close = bar.getClosePrice().doubleValue();
            double volume = bar.getVolume().doubleValue();
            Duration timePeriod = bar.getTimePeriod();

            double body = Math.abs(close - open);
            double upperWick = high - Math.max(open, close);
            double lowerWick = Math.min(open, close) - low;

            // ✅ Wick cleaning logic
            if (upperWick > body * (1 + maxWickRatio)) {
                high = Math.max(open, close);  // trim unrealistic high wick
            }
            if (lowerWick > body * (1 + maxWickRatio)) {
                low = Math.min(open, close);   // trim unrealistic low wick
            }

            // ✅ Convert doubles back to TA4J Num
            Num openNum = cleanedSeries.numOf(open);
            Num highNum = cleanedSeries.numOf(high);
            Num lowNum = cleanedSeries.numOf(low);
            Num closeNum = cleanedSeries.numOf(close);
            Num volumeNum = cleanedSeries.numOf(volume);

            // ✅ Build new cleaned bar
            Bar cleanedBar = BaseBar.builder()
                    .timePeriod(timePeriod)
                    .endTime(bar.getEndTime())
                    .openPrice(openNum)
                    .highPrice(highNum)
                    .lowPrice(lowNum)
                    .closePrice(closeNum)
                    .volume(volumeNum)
                    .build();

            cleanedSeries.addBar(cleanedBar);
        }

        return cleanedSeries;
    }
}
