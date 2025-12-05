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
                        result.setHighestCloseOpen(0);
                        result.setHighestCloseOpenStatus("N/A (No data)");
                        //previousStatuses.put("HighestCloseOpen", "N/A");
                        //logToConsole("Warning: No HighestCloseOpen data for " + symbol + " " + tf);
                    }
                    
                } catch (Exception e) {
                    //logToConsole("Error calculating HighestCloseOpen for " + symbol + " " + tf + ": " + e.getMessage());
                    result.setHighestCloseOpen(0);
                    result.setHighestCloseOpenStatus("Error");
                    //previousStatuses.put("HighestCloseOpen", "Error");
                }
            } else {
                result.setHighestCloseOpen(0);
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
        if (series == null || endIndex < 0 || endIndex >= series.getBarCount()) {
            return 0.0;
        }
        
        double zscore = 0.0;
        double maxPossibleScore = 0.0;
        
        // Get current values
        double currentPrice = result.getPrice();
        double highestValue = result.getHighestCloseOpen();
        String status = result.getHighestCloseOpenStatus();
        
        // Skip if no valid data
        if (Double.isNaN(highestValue) || Double.isNaN(currentPrice)) {
            result.setHighestCloseOpenZscore(0.0);
            return 0.0;
        }
        
        // 1. Trend Direction - 40 points
        maxPossibleScore += 40;
        boolean isUptrend = status != null && status.toLowerCase().contains("uptrend");
        if (isUptrend) {
            zscore += 40; // Uptrend
        } else {
            zscore += 10; // Downtrend
        }
        
        // 2. Price Proximity to Highest Value - 30 points
        maxPossibleScore += 30;
        double priceDifference = currentPrice - highestValue;
        double proximityScore = 0.0;
        
        if (priceDifference >= 0) {
            // At or above the highest value - strong bullish
            proximityScore = 30;
        } else {
            // Below the highest value - calculate how close
            double percentBelow = (Math.abs(priceDifference) / highestValue) * 100.0;
            if (percentBelow <= 1.0) {
                proximityScore = 25; // Very close (within 1%)
            } else if (percentBelow <= 3.0) {
                proximityScore = 20; // Close (within 3%)
            } else if (percentBelow <= 5.0) {
                proximityScore = 15; // Moderately close (within 5%)
            } else if (percentBelow <= 10.0) {
                proximityScore = 10; // Somewhat close (within 10%)
            } else {
                proximityScore = 5; // Far from highest value
            }
        }
        zscore += proximityScore;
        
        // 3. Recent Breakout Strength - 20 points
        maxPossibleScore += 20;
        if (isUptrend && priceDifference > 0) {
            // Recently broke above highest value
            double breakoutStrength = Math.min(20, (priceDifference / highestValue) * 1000);
            zscore += breakoutStrength;
        } else if (isUptrend) {
            // In uptrend but not above highest value yet
            zscore += 10;
        } else {
            // Downtrend
            zscore += 5;
        }
        
        // 4. Historical Context - 10 points
        maxPossibleScore += 10;
        if (endIndex > 0) {
            double previousPrice = series.getBar(endIndex - 1).getClosePrice().doubleValue();
            boolean previousWasUptrend = previousPrice >= highestValue;
            
            if (isUptrend && previousWasUptrend) {
                zscore += 10; // Consistent uptrend
            } else if (isUptrend && !previousWasUptrend) {
                zscore += 7; // New uptrend
            } else if (!isUptrend && previousWasUptrend) {
                zscore += 3; // Recent breakdown
            } else {
                zscore += 1; // Consistent downtrend
            }
        } else {
            zscore += 5; // First data point
        }
        
        // Normalize to 100%
        double normalizedZscore = normalizeScore(zscore, maxPossibleScore);
        
        result.setHighestCloseOpenZscore(normalizedZscore);
        return normalizedZscore;
    }

    // Alternative simpler version if you prefer:
    public double calculateZscoreSimple(BarSeries series, AnalysisResult result, int endIndex) {
        if (series == null || endIndex < 0 || endIndex >= series.getBarCount()) {
            return 0.0;
        }
        
        double currentPrice = result.getPrice();
        double highestValue = result.getHighestCloseOpen();
        String status = result.getHighestCloseOpenStatus();
        
        if (Double.isNaN(highestValue) || Double.isNaN(currentPrice)) {
            return 0.0;
        }
        
        double zscore = 0.0;
        
        // 1. Basic trend - 60 points
        boolean isUptrend = status != null && status.toLowerCase().contains("uptrend");
        if (isUptrend) {
            zscore += 60;
        } else {
            zscore += 20;
        }
        
        // 2. Price position relative to highest value - 40 points
        double priceRatio = currentPrice / highestValue;
        if (priceRatio >= 1.0) {
            zscore += 40; // Above highest value
        } else if (priceRatio >= 0.98) {
            zscore += 35; // Very close (within 2%)
        } else if (priceRatio >= 0.95) {
            zscore += 25; // Close (within 5%)
        } else if (priceRatio >= 0.90) {
            zscore += 15; // Somewhat close (within 10%)
        } else {
            zscore += 5; // Far from highest value
        }
        
        result.setHighestCloseOpenZscore(zscore);
        return zscore;
    }
}