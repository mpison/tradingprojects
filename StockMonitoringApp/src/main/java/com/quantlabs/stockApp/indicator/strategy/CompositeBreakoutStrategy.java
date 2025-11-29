package com.quantlabs.stockApp.indicator.strategy;

import org.ta4j.core.BarSeries;

import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class CompositeBreakoutStrategy extends AbstractIndicatorStrategy {
    public CompositeBreakoutStrategy(ConsoleLogger logger) {
		super(logger);
		// TODO Auto-generated constructor stub
	}
    
    @Override
    public String getName() {
        return "Breakout Count";
    }
    
    @Override
    public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
        int totalBreakouts = 0;
        StringBuilder breakoutSummary = new StringBuilder();
        
        if (result.getMacdBreakoutCount() > 0) {
            totalBreakouts += result.getMacdBreakoutCount();
            breakoutSummary.append("MACD:").append(result.getMacdBreakoutCount()).append(" ");
        }
        
        if (result.getMacd359BreakoutCount() > 0) {
            totalBreakouts += result.getMacd359BreakoutCount();
            breakoutSummary.append("MACD359:").append(result.getMacd359BreakoutCount()).append(" ");
        }
        
        if (result.getPsar001BreakoutCount() > 0) {
            totalBreakouts += result.getPsar001BreakoutCount();
            breakoutSummary.append("PS1:").append(result.getPsar001BreakoutCount()).append(" ");
        }
        
        if (result.getPsar005BreakoutCount() > 0) {
            totalBreakouts += result.getPsar005BreakoutCount();
            breakoutSummary.append("PS5:").append(result.getPsar005BreakoutCount()).append(" ");
        }
        
        String displayValue = breakoutSummary.length() > 0
            ? breakoutSummary.toString().trim() + " (Total: " + totalBreakouts + ")"
            : "No Breakouts";
        
        result.setBreakoutCount(totalBreakouts);
        result.setBreakoutSummary(displayValue);
    }


	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		return "NA";
	}
	
	@Override
	public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
	    // This is a composite strategy, individual Z-scores are calculated by component strategies
	    return 0.0;
	}
}