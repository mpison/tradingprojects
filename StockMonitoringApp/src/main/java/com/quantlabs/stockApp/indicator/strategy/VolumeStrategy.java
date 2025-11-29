package com.quantlabs.stockApp.indicator.strategy;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.helpers.VolumeIndicator;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class VolumeStrategy extends AbstractIndicatorStrategy{

	public VolumeStrategy(ConsoleLogger logger) {
		super(logger);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Volume";
	}

	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		// TODO Auto-generated method stub
		return "NA";
	}

	@Override
	public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
		int end = series.getEndIndex();
	    if (end >= 0) {
	        VolumeIndicator vInd = new VolumeIndicator(series);
	        result.setVolume(vInd.getValue(series.getEndIndex()).doubleValue());
	    }
	    
	    calculateZscore(series,result,endIndex);
	}
	
	@Override
	public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
	    // Volume alone doesn't determine bullish/bearish direction
	    return 0.0;
	}

}
