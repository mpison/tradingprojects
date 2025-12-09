package com.quantlabs.stockApp.reports;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ta4j.core.BarSeries;

public class AnalysisResult {
    // Price and basic analysis
    private double price;
    
    private double sma9;
    private double sma20;
    private double sma200;
    private String smaTrend;
    private double ema20;
    private String ema20Trend;
    private String trend;
        
    private double rsi;
    private String rsiTrend;
    
    // MACD indicators
    private double macd;
    private double macdSignal;
    private String macdStatus;
    private double macd359;
    private double macdSignal359;
    private String macd359Status;
    
    // PSAR indicators
    private double psar001;
    private String psar001Trend;
    private double psar005;
    private String psar005Trend;
    private double psar;
    private String psarTrend;
    
    // Heiken Ashi indicators
    private double heikenAshiClose;
    private double heikenAshiOpen;
    private String heikenAshiTrend;
    
    // Breakout counts
    private int macdBreakoutCount;
    private int macd359BreakoutCount;
    private int psar001BreakoutCount;
    private int psar005BreakoutCount;
    private int breakoutCount;
    private String breakoutSummary;
    
    // Advanced indicators
    private double highestCloseOpen;
    private String highestCloseOpenStatus;
    
    private Double movingAverageTargetValue;
    private Double movingAverageTargetValuePercentile;
    private String movingAverageTargetValueStatus;
    
    // Z-score fields (0-100 scale)
    private double macdZscore;
    private double macd359Zscore;
    private double rsiZscore;
    private double psarZscore;
    private double psar001Zscore;
    private double psar005Zscore;
    private double vwapZscore;
    private double volume20Zscore;
    private double heikenAshiZscore;
    private double highestCloseOpenZscore;
    private double trendZscore;
    private double movingAverageTargetValueZscore;
    
    // Custom values for extensibility
    private Map<String, Object> customValues = new HashMap<>();
    // Add to AnalysisResult class
    private Map<String, String> customIndicatorValues = new HashMap<>();
    
    private Double volume;       // current bar volume
    private Double volume20MA;   // 20-period SMA of volume
    private String volume20Trend;   // 20-period SMA of volume
    
    
    private Double vwap;
    private String vwapStatus;
    
    private long timestamp = System.currentTimeMillis();
    
    private BarSeries barSeries;
    
    // Getters and setters
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public double getSma9() {
		return sma9;
	}
	public void setSma9(double sma9) {
		this.sma9 = sma9;
	}
	public double getSma20() { return sma20; }
    public void setSma20(double sma20) { this.sma20 = sma20; }
    
    public double getSma200() { return sma200; }
    public void setSma200(double sma200) { this.sma200 = sma200; }
    
    public String getTrend() {
		return trend;
	}
	public void setTrend(String trend) {
		this.trend = trend;
	}
	public double getRsi() { return rsi; }
    public void setRsi(double rsi) { this.rsi = rsi; }
    
    public double getMacd() { return macd; }
    public void setMacd(double macd) { this.macd = macd; }
    
    public double getMacdSignal() { return macdSignal; }
    public void setMacdSignal(double macdSignal) { this.macdSignal = macdSignal; }
    
    public String getMacdStatus() { return macdStatus; }
    public void setMacdStatus(String macdStatus) { this.macdStatus = macdStatus; }
    
    public double getMacd359() { return macd359; }
    public void setMacd359(double macd359) { this.macd359 = macd359; }
    
    public double getMacdSignal359() { return macdSignal359; }
    public void setMacdSignal359(double macdSignal359) { this.macdSignal359 = macdSignal359; }
    
    public String getMacd359Status() { return macd359Status; }
    public void setMacd359Status(String macd359Status) { this.macd359Status = macd359Status; }
    
    public double getPsar001() { return psar001; }
    public void setPsar001(double psar001) { this.psar001 = psar001; }
    
    public String getPsar001Trend() { return psar001Trend; }
    public void setPsar001Trend(String psar001Trend) { this.psar001Trend = psar001Trend; }
    
    public double getPsar005() { return psar005; }
    public void setPsar005(double psar005) { this.psar005 = psar005; }
    
    public String getPsar005Trend() { return psar005Trend; }
    public void setPsar005Trend(String psar005Trend) { this.psar005Trend = psar005Trend; }   
    
    public double getPsar() {
		return psar;
	}
	public void setPsar(double psar) {
		this.psar = psar;
	}
	public String getPsarTrend() {
		return psarTrend;
	}
	public void setPsarTrend(String psarTrend) {
		this.psarTrend = psarTrend;
	}
	public double getHeikenAshiClose() { return heikenAshiClose; }
    public void setHeikenAshiClose(double heikenAshiClose) { this.heikenAshiClose = heikenAshiClose; }
    
    public double getHeikenAshiOpen() { return heikenAshiOpen; }
    public void setHeikenAshiOpen(double heikenAshiOpen) { this.heikenAshiOpen = heikenAshiOpen; }
    
    public String getHeikenAshiTrend() { return heikenAshiTrend; }
    public void setHeikenAshiTrend(String heikenAshiTrend) { this.heikenAshiTrend = heikenAshiTrend; }
    
    public int getMacdBreakoutCount() { return macdBreakoutCount; }
    public void setMacdBreakoutCount(int macdBreakoutCount) { this.macdBreakoutCount = macdBreakoutCount; }
    
    public int getMacd359BreakoutCount() { return macd359BreakoutCount; }
    public void setMacd359BreakoutCount(int macd359BreakoutCount) { this.macd359BreakoutCount = macd359BreakoutCount; }
    
    public int getPsar001BreakoutCount() { return psar001BreakoutCount; }
    public void setPsar001BreakoutCount(int psar001BreakoutCount) { this.psar001BreakoutCount = psar001BreakoutCount; }
    
    public int getPsar005BreakoutCount() { return psar005BreakoutCount; }
    public void setPsar005BreakoutCount(int psar005BreakoutCount) { this.psar005BreakoutCount = psar005BreakoutCount; }
    
    public int getBreakoutCount() { return breakoutCount; }
    public void setBreakoutCount(int breakoutCount) { this.breakoutCount = breakoutCount; }
    
    public String getBreakoutSummary() { return breakoutSummary; }
    public void setBreakoutSummary(String breakoutSummary) { this.breakoutSummary = breakoutSummary; }
    
    public double getHighestCloseOpen() { return highestCloseOpen; }
    public void setHighestCloseOpen(double highestCloseOpen) { this.highestCloseOpen = highestCloseOpen; }
    
    public String getHighestCloseOpenStatus() { return highestCloseOpenStatus; }
    public void setHighestCloseOpenStatus(String highestCloseOpenStatus) { this.highestCloseOpenStatus = highestCloseOpenStatus; }
    
    public Double getMovingAverageTargetValue() { return movingAverageTargetValue; }
    public void setMovingAverageTargetValue(Double movingAverageTargetValue) { this.movingAverageTargetValue = movingAverageTargetValue; }
    
    public Double getMovingAverageTargetValuePercentile() {
    	if(this.movingAverageTargetValue!= null) {
    		if(this.movingAverageTargetValue > 0) {	
    			return (( this.movingAverageTargetValue) / this.price) * 100;
    		}else {
    			return ((this.movingAverageTargetValue) / this.price) * 100;
    		}
    	}
    	else
    		return (double) 0;
	}
    
    public void setMovingAverageTargetValuePercentile(Double movingAverageTargetValuePercentile) { 
        this.movingAverageTargetValuePercentile = movingAverageTargetValuePercentile; 
    }
    
    public Double getVwap() {
		return vwap;
	}
	public void setVwap(Double vwap) {
		this.vwap = vwap;
	}
	public String getVwapStatus() {
		return vwapStatus;
	}
	public void setVwapStatus(String vwapStatus) {
		this.vwapStatus = vwapStatus;
	}
	public Map<String, Object> getCustomValues() { return customValues; }
    public void setCustomValues(Map<String, Object> customValues) { this.customValues = customValues; }
    
    public void addCustomValue(String key, Object value) {
        customValues.put(key, value);
    }
    
    public Object getCustomValue(String key) {
        return customValues.get(key);
    }
    
    public void setCustomIndicatorValue(String indicatorName, String value) {
        customIndicatorValues.put(indicatorName, value);
    }

    public String getCustomIndicatorValue(String indicatorName) {
        return customIndicatorValues.getOrDefault(indicatorName, "N/A");
    }

    public Map<String, String> getCustomIndicatorValues() {
        return Collections.unmodifiableMap(customIndicatorValues);
    }
    
    
	public String getSmaTrend() {
		return smaTrend;
	}
	public void setSmaTrend(String smaTrend) {
		this.smaTrend = smaTrend;
	}
	public String getRsiTrend() {
		return rsiTrend;
	}
	public void setRsiTrend(String rsiTrend) {
		this.rsiTrend = rsiTrend;
	}
	public Double getVolume() {
		return volume;
	}
	public void setVolume(Double volume) {
		this.volume = volume;
	}
	public Double getVolume20MA() {
		return volume20MA;
	}
	public void setVolume20MA(Double volume20ma) {
		volume20MA = volume20ma;
	}	
	
	public String getVolume20Trend() {
		return volume20Trend;
	}
	public void setVolume20Trend(String volume20Trend) {
		this.volume20Trend = volume20Trend;
	}
	public void setCustomIndicatorValues(Map<String, String> customIndicatorValues) {
		this.customIndicatorValues = customIndicatorValues;
	}
	public BarSeries getBarSeries() {
		return barSeries;
	}
	public void setBarSeries(BarSeries barSeries) {
		this.barSeries = barSeries;
	}
	public double getMacdZscore() {
		return macdZscore;
	}
	public void setMacdZscore(double macdZscore) {
		this.macdZscore = macdZscore;
	}
	public double getMacd359Zscore() {
		return macd359Zscore;
	}
	public void setMacd359Zscore(double macd359Zscore) {
		this.macd359Zscore = macd359Zscore;
	}
	public double getRsiZscore() {
		return rsiZscore;
	}
	public void setRsiZscore(double rsiZscore) {
		this.rsiZscore = rsiZscore;
	}
	public double getPsarZscore() {
		return psarZscore;
	}
	public void setPsarZscore(double psarZscore) {
		this.psarZscore = psarZscore;
	}
	public double getPsar001Zscore() {
		return psar001Zscore;
	}
	public void setPsar001Zscore(double psar001Zscore) {
		this.psar001Zscore = psar001Zscore;
	}
	public double getPsar005Zscore() {
		return psar005Zscore;
	}
	public void setPsar005Zscore(double psar005Zscore) {
		this.psar005Zscore = psar005Zscore;
	}
	public double getVwapZscore() {
		return vwapZscore;
	}
	public void setVwapZscore(double vwapZscore) {
		this.vwapZscore = vwapZscore;
	}
	public double getVolume20Zscore() {
		return volume20Zscore;
	}
	public void setVolume20Zscore(double volume20Zscore) {
		this.volume20Zscore = volume20Zscore;
	}
	public double getHeikenAshiZscore() {
		return heikenAshiZscore;
	}
	public void setHeikenAshiZscore(double heikenAshiZscore) {
		this.heikenAshiZscore = heikenAshiZscore;
	}
	
	// Add getter and setter
	public double getHighestCloseOpenZscore() { 
	    return highestCloseOpenZscore; 
	}

	public void setHighestCloseOpenZscore(double highestCloseOpenZscore) { 
	    this.highestCloseOpenZscore = highestCloseOpenZscore; 
	}
	public double getTrendZscore() {
		return trendZscore;
	}
	public void setTrendZscore(double trendZscore) {
		this.trendZscore = trendZscore;
	}
	public double getMovingAverageTargetValueZscore() {
		return movingAverageTargetValueZscore;
	}
	public void setMovingAverageTargetValueZscore(double movingAverageTargetValueZscore) {
		this.movingAverageTargetValueZscore = movingAverageTargetValueZscore;
	}
	public double getEma20() {
		return ema20;
	}
	public void setEma20(double ema20) {
		this.ema20 = ema20;
	}
	public String getEma20Trend() {
		return ema20Trend;
	}
	public void setEma20Trend(String ema20Trend) {
		this.ema20Trend = ema20Trend;
	}
	
	public long getTimestamp() { return timestamp; }
	
	public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
		
	public String getMovingAverageTargetValueStatus() {
		return movingAverageTargetValueStatus;
	}
	public void setMovingAverageTargetValueStatus(String movingAverageTargetValueStatus) {
		this.movingAverageTargetValueStatus = movingAverageTargetValueStatus;
	}
	public double getOverallZscore() {
	    int count = 0;
	    double total = 0.0;
	    
	    // Only include valid Z-scores (> 0)
	    if (macdZscore > 0) { total += macdZscore; count++; }
	    if (macd359Zscore > 0) { total += macd359Zscore; count++; }
	    if (rsiZscore > 0) { total += rsiZscore; count++; }
	    if (psar001Zscore > 0) { total += psar001Zscore; count++; }
	    if (psar005Zscore > 0) { total += psar005Zscore; count++; }
	    if (heikenAshiZscore > 0) { total += heikenAshiZscore; count++; }
	    if (trendZscore > 0) { total += trendZscore; count++; }
	    if (movingAverageTargetValueZscore > 0) { total += movingAverageTargetValueZscore; count++; }
	    if (highestCloseOpenZscore > 0) { total += highestCloseOpenZscore; count++; }
	    
	    return count > 0 ? total / count : 0.0;
	}

	public String getOverallSignal() {
	    double overall = getOverallZscore();
	    if (overall >= 80) return "Very Strong";
	    if (overall >= 70) return "Strong";
	    if (overall >= 60) return "Moderate";
	    if (overall >= 50) return "Slightly Bullish";
	    if (overall >= 40) return "Neutral";
	    if (overall >= 30) return "Slightly Bearish";
	    if (overall >= 20) return "Weak";
	    return "Very Weak";
	}
	
}