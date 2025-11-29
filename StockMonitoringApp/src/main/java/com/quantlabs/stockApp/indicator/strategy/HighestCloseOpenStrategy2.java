package com.quantlabs.stockApp.indicator.strategy;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.core.indicators.resistance.HighestCloseOpenIndicator;
import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class HighestCloseOpenStrategy2 extends AbstractIndicatorStrategy {
	
	private Map<String, JComboBox<String>> indicatorTimeRangeCombos = new HashMap<>();
	private String tf;
	private String currentDataSource = "Yahoo";
	private boolean enabled = true;
	
    public HighestCloseOpenStrategy2(ConsoleLogger logger) {
		super(logger);
	}
    
    @Override
    public String getName() {
        return "HighestCloseOpen";
    }
    
    @Override
    public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
    	JComboBox<String> timeRangeCombo = indicatorTimeRangeCombos.get(tf);
		if (timeRangeCombo != null) {
			String timeRange = timeRangeCombo.getSelectedItem().toString();
			// int periodDays = getPeriodInDays(timeRange);

			HighestCloseOpenIndicator highestClose = new HighestCloseOpenIndicator(series, timeRange, true,
					currentDataSource);
			HighestCloseOpenIndicator highestOpen = new HighestCloseOpenIndicator(series, timeRange, false,
					currentDataSource);

			HighestCloseOpenIndicator.HighestValue highestCloseVal = highestClose.getValue(series.getEndIndex());
			HighestCloseOpenIndicator.HighestValue highestOpenVal = highestOpen.getValue(series.getEndIndex());

			// Determine which is higher - close or open
			HighestCloseOpenIndicator.HighestValue highestVal = highestCloseVal.value
					.isGreaterThan(highestOpenVal.value) ? highestCloseVal : highestOpenVal;

			Num currentPrice = series.getLastBar().getClosePrice();
			boolean isDowntrend = !currentPrice.isGreaterThan(highestVal.value);

			// Format the timestamp with timezone conversion for Yahoo
			String timeStr = "N/A";
			if (highestVal.time != null) {
				ZonedDateTime displayTime = highestVal.time;

				// Convert to Pacific Time if data source is Yahoo
				if ("YAHOO".equalsIgnoreCase(currentDataSource)) {
					displayTime = highestVal.time.withZoneSameInstant(ZoneId.of("America/Los_Angeles"));
					timeStr = displayTime.format(DateTimeFormatter.ofPattern("MMM dd HH:mm")) + " PST";
				} else {
					// Keep original timezone for other data sources
					timeStr = displayTime.format(DateTimeFormatter.ofPattern("MMM dd HH:mm z"));
				}
			}

			result.setHighestCloseOpen(highestVal.value.doubleValue());
			result.setHighestCloseOpenStatus(String.format("%s (%.2f @ %s)", isDowntrend ? "Downtrend" : "Uptrend",
					highestVal.value.doubleValue(), timeStr));
		}
		
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

	public String getTf() {
		return tf;
	}

	public void setTf(String tf) {
		this.tf = tf;
	}

	public String getCurrentDataSource() {
		return currentDataSource;
	}

	public void setCurrentDataSource(String currentDataSource) {
		this.currentDataSource = currentDataSource;
	}

	public Map<String, JComboBox<String>> getIndicatorTimeRangeCombos() {
		return indicatorTimeRangeCombos;
	}
    
	public void setIndicatorTimeRangeCombos(Map<String, JComboBox<String>> indicatorTimeRangeCombos) {
		this.indicatorTimeRangeCombos = indicatorTimeRangeCombos;
	}

	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public double calculateZscore(BarSeries series, AnalysisResult result, int endIndex) {
	    String status = result.getHighestCloseOpenStatus();
	    double zscore = 0.0;
	    
	    // If status contains "Uptrend", set 100 points
	    if (status != null && status.toLowerCase().contains("uptrend")) {
	        zscore = MAX_ZSCORE; // 100 points
	    }
	    
	    // Set it on the result object
	    result.setHighestCloseOpenZscore(zscore);
	    
	    // Also store as custom indicator value for display
	    result.setCustomIndicatorValue(getName() + "_Zscore", String.format("%.1f", zscore));
	    
	    return zscore;
	}
}