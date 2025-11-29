package com.quantlabs.stockApp.indicator.strategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.core.indicators.AdvancedVWAPStrategy;
import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class AdvancedVWAPIndicatorStrategy extends AbstractIndicatorStrategy{

	public AdvancedVWAPIndicatorStrategy(ConsoleLogger logger) {
		super(logger);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "VWAP";
	}

	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
		// TODO Auto-generated method stub
		result.setVwapStatus(currentTrend);
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		double vwapValue = result.getVwap();
		if (vwapValue == Double.NaN) {
	        return "Neutral";
	    }
	    if (vwapValue > 70) {
	        return "Overbought";
	    } else if (vwapValue < 30) {
	        return "Oversold";
	    }
	    return "Neutral";
	}

	@Override
	public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
		// TODO Auto-generated method stub
		AdvancedVWAPStrategy.AdvancedVWAPIndicator vwapIndicator = new AdvancedVWAPStrategy.AdvancedVWAPIndicator(series);
		if (series.getEndIndex() >= 0) {
	        Num vwapValue = vwapIndicator.getValue(series.getEndIndex());
	        Num vwapClosePrice = series.getLastBar().getClosePrice();
	        String vwapStatus = vwapClosePrice.isGreaterThan(vwapValue) ? "Uptrend" : "Downtrend";
	        result.setVwapStatus(vwapStatus);
	        //previousStatuses.put("VWAP", vwapStatus);
		}
		
		calculateZscore(series,result,endIndex);
	}
	
	// In AdvancedVWAPIndicatorStrategy.java - Add this method
	@Override
	public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
	    String status = result.getVwapStatus();
	    double zscore = 0.0;
	    
	    // If status is "Uptrend", set 100 points
	    if (status != null && status.toLowerCase().contains("uptrend")) {
	        zscore = MAX_ZSCORE; // 100 points
	    }
	    
	    // Set it on the result
	    result.setVwapZscore(zscore);
	    return zscore;
	}

	

}
