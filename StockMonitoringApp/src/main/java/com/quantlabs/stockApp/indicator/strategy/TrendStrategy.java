package com.quantlabs.stockApp.indicator.strategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class TrendStrategy extends AbstractIndicatorStrategy {
	public TrendStrategy(ConsoleLogger logger) {
		super(logger);
	}

	private boolean enabled = true;

	@Override
	public String getName() {
		return "Trend";
	}

	@Override
	public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		SMAIndicator sma = new SMAIndicator(closePrice, 9);
		SMAIndicator sma200 = new SMAIndicator(closePrice, 200);
		EMAIndicator ema = new EMAIndicator(closePrice, 20);

		Num smaValue = sma.getValue(endIndex);
		Num sma200Value = sma200.getValue(endIndex);
		Num emaValue = ema.getValue(endIndex);

		if (endIndex >= 9) {
			result.setSma20(smaValue.doubleValue());
		}
		if (endIndex >= 19) {
			result.setSma20(sma200Value.doubleValue());
		}
		if (endIndex >= 199) {
			result.setSma200(emaValue.doubleValue());
		}

		result.setSmaTrend(determineTrend(result));
		
		calculateZscore(series,result,endIndex);
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	private String determineTrend(Num close, Num sma, Num sma200, Num ema) {
		if (close == null || sma == null || sma200 == null || ema == null) {
			return "Neutral";
		}
		if (close.isGreaterThan(sma) && close.isGreaterThan(sma200) && sma.isGreaterThan(sma200)) {
			if (close.isGreaterThan(ema)) {
				return "Uptrend";
			} else {
				return "Uptrend";
			}
		} else if (close.isLessThan(sma) && close.isLessThan(sma200) && sma.isLessThan(sma200)) {
			if (close.isLessThan(ema)) {
				return "Downtrend";
			} else {
				return "Downtrend";
			}
		}
		return "Neutral";
	}

	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
		result.setSmaTrend(currentTrend);
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		if (result.getSma9() == Double.NaN || result.getSma20() == Double.NaN || result.getSma200() == Double.NaN) {
			return "Neutral";
		}
		if (result.getPrice() > result.getSma9() && result.getPrice() > result.getSma20()
				&& result.getPrice() > result.getSma200()) {
			return "Uptrend";
		} else if (result.getPrice() < result.getSma9() && result.getPrice() < result.getSma20()
				&& result.getPrice() < result.getSma200()) {
			return "Downtrend";
		}
		return "Neutral";
	}
	
	// In TrendStrategy.java - Alternative implementation with fixed 100 points
	@Override
	public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
	    double zscore = 0.0;
	    
	    // Fixed 100 points distribution
	    int totalPoints = 0;
	    int earnedPoints = 0;
	    
	    // 1. Price above SMA9 - 25 points (if data available)
	    if (!Double.isNaN(result.getSma9())) {
	        totalPoints += 25;
	        if (result.getPrice() > result.getSma9()) {
	            earnedPoints += 25;
	        }
	    }
	    
	    // 2. Price above SMA20 - 25 points (if data available)
	    if (!Double.isNaN(result.getSma20())) {
	        totalPoints += 25;
	        if (result.getPrice() > result.getSma20()) {
	            earnedPoints += 25;
	        }
	    }
	    
	    // 3. Price above SMA200 - 25 points (if data available)
	    if (!Double.isNaN(result.getSma200())) {
	        totalPoints += 25;
	        if (result.getPrice() > result.getSma200()) {
	            earnedPoints += 25;
	        }
	    }
	    
	    // 4. SMA9 above SMA20 - 15 points (if both available)
	    if (!Double.isNaN(result.getSma9()) && !Double.isNaN(result.getSma20())) {
	        totalPoints += 15;
	        if (result.getSma9() > result.getSma20()) {
	            earnedPoints += 15;
	        }
	    }
	    
	    // 5. SMA20 above SMA200 - 10 points (if both available)
	    if (!Double.isNaN(result.getSma20()) && !Double.isNaN(result.getSma200())) {
	        totalPoints += 10;
	        if (result.getSma20() > result.getSma200()) {
	            earnedPoints += 10;
	        }
	    }
	    
	    // Calculate percentage of available points earned
	    double normalizedZscore = totalPoints > 0 ? ((double) earnedPoints / totalPoints) * MAX_ZSCORE : 0.0;
	    
	    // Set it on the result
	    result.setTrendZscore(normalizedZscore);
	    return normalizedZscore;
	}

}