// PSAR21Trend identifies trend direction using dual Parabolic SAR configurations.
// It checks for trend breakout confirmations based on segment consistency and open price breakouts.
// Utilizes a custom PSAR21TrendIndicator that evaluates fast and slow PSAR segments
// and confirms valid trends when current open price breaks past consistent PSAR movement segments.
package com.quantlabs.core.trend;

import org.ta4j.core.BarSeries;

import com.quantlabs.core.enums.TimeFrameEnum;
import com.quantlabs.core.enums.TrendClassEnum;
import com.quantlabs.core.enums.TrendDirectionEnum;
import com.quantlabs.core.indicators.PSAR21TrendIndicator;

/**
 * PSAR21Trend is a trend detector that identifies directional market trends based on dual Parabolic SAR indicators.
 * It confirms trends by evaluating breakout conditions using fast and slow SAR settings.
 */
public class PSAR21Trend extends ABaseTrend {

	@Override
	public void calculate() {		
	}

	private double stepFast = 0.05;
	private double maxFast = 0.05;
	private double stepSlow = 0.01;
	private double maxSlow = 0.01;

	/**
     * Constructs PSAR21Trend with full configuration.
     * 
     * @param timeframe Timeframe for the trend
     * @param shift Index shift to apply for trend calculation
     * @param symbol Trading symbol (e.g., EURUSD)
     * @param isUseMomentum Whether to consider momentum (not used here)
     * @param series Price series
     */
	public PSAR21Trend(TimeFrameEnum timeframe, int shift, String symbol, boolean isUseMomentum, BarSeries series) {
		super(timeframe, shift, TrendClassEnum.PSAR21_TREND_CLASS, symbol, isUseMomentum, series);        		
	}

	@Override
	/**
     * Identifies the trend at a specific index using PSAR21TrendIndicator.
     * 
     * @param myIndex The index on the series to evaluate
     * @return TrendDirectionEnum corresponding to breakout behavior
     */
	public TrendDirectionEnum identifyTrend(int myIndex) {
		if (series == null || myIndex < 0 || myIndex >= series.getBarCount())
			return TrendDirectionEnum.INVALID_TREND;
		PSAR21TrendIndicator indicator = new PSAR21TrendIndicator(series, stepFast, maxFast, stepSlow, maxSlow);
		return TrendDirectionEnum.values()[indicator.getValue(myIndex).intValue()];
	}

	public void setStepFast(double stepFast) {
		this.stepFast = stepFast;
	}

	public void setMaxFast(double maxFast) {
		this.maxFast = maxFast;
	}

	public void setStepSlow(double stepSlow) {
		this.stepSlow = stepSlow;
	}

	public void setMaxSlow(double maxSlow) {
		this.maxSlow = maxSlow;
	}
}
