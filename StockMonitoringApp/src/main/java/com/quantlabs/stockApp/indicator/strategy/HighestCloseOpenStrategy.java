package com.quantlabs.stockApp.indicator.strategy;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.ta4j.core.BarSeries;
import org.ta4j.core.num.Num;

import com.quantlabs.stockApp.core.indicators.resistance.HighestCloseOpenIndicator;
import com.quantlabs.stockApp.data.ConsoleLogger;
import com.quantlabs.stockApp.reports.AnalysisResult;

public class HighestCloseOpenStrategy extends AbstractIndicatorStrategy {
		
	private String timeRange = "1D";
	
	private String session = "all";
	
	private int indexCounter = 5;
	
	private String tf;
	private String currentDataSource = "Yahoo";
	
	
	private static final ZoneId EASTERN_ZONE = ZoneId.of("America/New_York");
    private static final ZoneId PACIFIC_ZONE = ZoneId.of("America/Los_Angeles");
	
	
	private boolean enabled = true;
	
    public HighestCloseOpenStrategy(ConsoleLogger logger) {
		super(logger);
	}
    
    @Override
    public String getName() {
        return "HighestCloseOpen";
    }
    
    @Override
    public void calculate(BarSeries series, AnalysisResult result, int endIndex) {
    	            
            // Use the current analysis index
            int currentAnalysisIndex = series.getEndIndex();//Math.min(adjustedEndIndex, series.getEndIndex());
            int sessionEndIndex = HighestCloseOpenStrategy.calculateSessionEndIndex(series, currentAnalysisIndex, session, currentDataSource);

            if (sessionEndIndex != -1) {
                try {
                    // Get the highest value using the helper method
                    HighestCloseOpenIndicator.HighestValue highestVal = 
                        HighestCloseOpenStrategy.getHighestCloseOpenValue(
                            series, timeRange, session, currentDataSource, 
                            sessionEndIndex, indexCounter, currentAnalysisIndex);

                    if (highestVal != null && highestVal.value != null) {
                        // Now check for higher prices using indexCounter
                        boolean hasHigherPrice = HighestCloseOpenStrategy.checkForHigherPriceInLastNBars(
                            series, highestVal, indexCounter, currentAnalysisIndex);
                        
                        Num currentPrice = series.getBar(currentAnalysisIndex).getClosePrice();
                        boolean isCurrentDowntrend = !currentPrice.isGreaterThan(highestVal.value);
                        
                        // Determine final trend
                        boolean isUptrend = hasHigherPrice || !isCurrentDowntrend;

                        // Format timestamp
                        String timeStr = "N/A";
                        if (highestVal.time != null) {
                            timeStr = highestVal.time.format(DateTimeFormatter.ofPattern("MMM dd HH:mm z"));
                        }

                        result.setHighestCloseOpen(highestVal.value.doubleValue());
                        result.setHighestCloseOpenStatus(String.format("%s (%.2f @ %s) [Lookback: %d]", 
                                isUptrend ? "Uptrend" : "Downtrend",
                                highestVal.value.doubleValue(), timeStr, indexCounter));
                        
                        //previousStatuses.put("HighestCloseOpen", result.getHighestCloseOpenStatus());
                    } else {
                        // Handle null values
                        result.setHighestCloseOpen(Double.NaN);
                        result.setHighestCloseOpenStatus("N/A (No data)");
                        //previousStatuses.put("HighestCloseOpen", "N/A");
                        //logToConsole("Warning: No HighestCloseOpen data for " + symbol + " " + tf);
                    }
                    
                } catch (Exception e) {
                    //logToConsole("Error calculating HighestCloseOpen for " + symbol + " " + tf + ": " + e.getMessage());
                    result.setHighestCloseOpen(Double.NaN);
                    result.setHighestCloseOpenStatus("Error");
                    //previousStatuses.put("HighestCloseOpen", "Error");
                }
            } else {
                result.setHighestCloseOpen(Double.NaN);
                result.setHighestCloseOpenStatus("No session data");
                //previousStatuses.put("HighestCloseOpen", "No session data");
            }
        //}
            
            calculateZscore(series,result,endIndex);
    }
    
    public static int calculateSessionEndIndex(BarSeries series, int currentEndIndex, String session, String currentDataSource) {
        if ("all".equalsIgnoreCase(session)) {
            return currentEndIndex;
        }
        
        // Define session time boundaries (adjust these based on your market hours)
        ZonedDateTime currentBarTime = series.getBar(currentEndIndex).getEndTime();
        int hour = currentBarTime.getHour();
        int minute = currentBarTime.getMinute();
        
        switch (session.toLowerCase()) {
            case "premarket":
                // Premarket typically 4:00 AM - 9:30 AM EST
                // Find the last bar in premarket session
                for (int i = currentEndIndex; i >= 0; i--) {
                    ZonedDateTime barTime = series.getBar(i).getEndTime();
                    
                    ZonedDateTime convertedBarDate = barTime;
                    
                    convertedBarDate = barTime.withZoneSameInstant(EASTERN_ZONE);
                    
                                        
                    int barHour = convertedBarDate.getHour();
                    if (barHour < 9 || (barHour == 9 && convertedBarDate.getMinute() < 30)) {
                        return i;
                    }
                }
                break;
                
            case "standard":
                // Standard session typically 9:30 AM - 4:00 PM EST
                // Find the last bar in standard session
                for (int i = currentEndIndex; i >= 0; i--) {
                    ZonedDateTime barTime = series.getBar(i).getEndTime();
                    
                    
                    ZonedDateTime convertedBarDate = barTime;
                    
                    convertedBarDate = barTime.withZoneSameInstant(EASTERN_ZONE);
                                        
                    
                    int barHour = convertedBarDate.getHour();
                    if (barHour >= 9 && barHour < 16) {
                        if (barHour == 9 && convertedBarDate.getMinute() >= 30) {
                            return i;
                        } else if (barHour > 9 && barHour < 16) {
                            return i;
                        }
                    }
                }
                break;
                
            case "postmarket":
                // Postmarket typically 4:00 PM - 8:00 PM EST
                // Find the last bar in postmarket session
                for (int i = currentEndIndex; i >= 0; i--) {
                    ZonedDateTime barTime = series.getBar(i).getEndTime();
                    
                    
                    ZonedDateTime convertedBarDate = barTime;
                    
                    convertedBarDate = barTime.withZoneSameInstant(EASTERN_ZONE);
                                        
                    int barHour = convertedBarDate.getHour();
                    if (barHour >= 16 && barHour < 20) {
                        return i;
                    }
                }
                break;
        }
        
        return -1; // No data found for selected session
    }
    
    /**
     * Checks if ANY of the last N bars have a price higher than the highest value
     * @param series The bar series
     * @param highestVal The current highest value found
     * @param indexCounter Number of previous bars to check
     * @param currentIndex The current analysis index
     * @return true if ANY bar in the last N has price > highestVal value
     */
    public static boolean checkForHigherPriceInLastNBars(BarSeries series, 
                                                 HighestCloseOpenIndicator.HighestValue highestVal,
                                                 int indexCounter, 
                                                 int currentIndex) {
        if (indexCounter <= 0 || highestVal == null || highestVal.value == null) {
            return false; // No checking required or invalid data
        }

        try {
            double highestValue = highestVal.value.doubleValue();
            int startCheckIndex = Math.max(0, currentIndex - indexCounter + 1);
            
            // Check each of the last N bars
            for (int i = startCheckIndex; i <= currentIndex; i++) {
                double barPrice = series.getBar(i).getClosePrice().doubleValue();
                if (barPrice > highestValue) {
                    return true; // Found a bar with price higher than the highest value
                }
            }
            
            return false; // No bar exceeded the highest value
        } catch (Exception e) {
            //logToConsole("Error checking for higher prices: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Helper method to get the highest close/open value with trend analysis using indexCounter
     */
    public static HighestCloseOpenIndicator.HighestValue getHighestCloseOpenValue(
        BarSeries series, String timeRange, String session, String dataSource, 
        int sessionEndIndex, int indexCounter, int currentAnalysisIndex) {
        
        // Create separate indicators for close and open
        HighestCloseOpenIndicator highestClose = new HighestCloseOpenIndicator(
            series, timeRange, true, dataSource, session, sessionEndIndex);
        HighestCloseOpenIndicator highestOpen = new HighestCloseOpenIndicator(
            series, timeRange, false, dataSource, session, sessionEndIndex);

        // Get values from both indicators
        HighestCloseOpenIndicator.HighestValue highestCloseVal = highestClose.getValue(sessionEndIndex);
        HighestCloseOpenIndicator.HighestValue highestOpenVal = highestOpen.getValue(sessionEndIndex);

        if (highestCloseVal != null && highestOpenVal != null && 
            highestCloseVal.value != null && highestOpenVal.value != null) {
            
            // Determine which is higher - close or open
            HighestCloseOpenIndicator.HighestValue highestVal = highestCloseVal.value
                    .isGreaterThan(highestOpenVal.value) ? highestCloseVal : highestOpenVal;
            
            // Check if any of last N bars exceeded the highest value using indexCounter
            boolean hasHigherPrice = checkForHigherPriceInLastNBars(
                series, highestVal, indexCounter, currentAnalysisIndex);
            
            // Get current price for trend determination
            Num currentPrice = series.getBar(currentAnalysisIndex).getClosePrice();
            boolean isCurrentDowntrend = !currentPrice.isGreaterThan(highestVal.value);
            
            // Determine final trend: Uptrend if ANY bar in last N exceeded the highest value
            boolean isUptrend = hasHigherPrice || !isCurrentDowntrend;
            
            // We need to return a HighestValue with the uptrend information
            // Since we can't modify the original HighestValue class, we'll create a wrapper
            // or use a different approach. For now, we'll store the trend in a custom way.
            return new HighestCloseOpenIndicator.HighestValue(highestVal.value, highestVal.time) {
                // You can add custom fields or methods here if needed
                // For now, we'll handle the trend logic in the calling code
            };
        }
        
        return null;
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


	public String getTimeRange() {
		return timeRange;
	}

	public void setTimeRange(String timeRange) {
		this.timeRange = timeRange;
	}

	public String getSession() {
		return session;
	}

	public void setSession(String session) {
		this.session = session;
	}

	public int getIndexCounter() {
		return indexCounter;
	}

	public void setIndexCounter(int indexCounter) {
		this.indexCounter = indexCounter;
	}

	@Override
	protected void updateTrendStatus(AnalysisResult result, String currentTrend) {
		result.setHighestCloseOpenStatus(currentTrend);
	}

	@Override
	public String determineTrend(AnalysisResult result) {
		return result.getHighestCloseOpenStatus();
	}
	
	// In HighestCloseOpenStrategy.java - Alternative implementation
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