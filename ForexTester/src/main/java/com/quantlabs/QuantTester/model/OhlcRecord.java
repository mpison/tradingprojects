package com.quantlabs.QuantTester.model;

import java.time.LocalDateTime;

public class OhlcRecord {
	private String timestamp;
    private String symbol;
    private String tf;
    private String broker;
    private double open;
    private double high;
    private double low;
    private double close;
    private double spread;
    private long ticks;
    private double liquidity;

    // Constructors, getters and setters
    public OhlcRecord() {}

    public OhlcRecord(String timestamp, String pair, String broker, 
                     double open, double high, double low, double close, 
                     double spread, long ticks, double liquidity) {
        this.timestamp = timestamp;
        this.symbol = pair;
        this.broker = broker;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.spread = spread;
        this.ticks = ticks;
        this.liquidity = liquidity;
    }

    // Getters and setters for all fields
    // ...
    
    @Override
    public String toString() {
        return String.format("OhlcRecord{timestamp=%s, pair='%s', broker='%s', open=%.4f, high=%.4f, low=%.4f, close=%.4f}",
                timestamp, symbol, broker, open, high, low, close);
    }

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getBroker() {
		return broker;
	}

	public void setBroker(String broker) {
		this.broker = broker;
	}

	public double getOpen() {
		return open;
	}

	public void setOpen(double open) {
		this.open = open;
	}

	public double getHigh() {
		return high;
	}

	public void setHigh(double high) {
		this.high = high;
	}

	public double getLow() {
		return low;
	}

	public void setLow(double low) {
		this.low = low;
	}

	public double getClose() {
		return close;
	}

	public void setClose(double close) {
		this.close = close;
	}

	public double getSpread() {
		return spread;
	}

	public void setSpread(double spread) {
		this.spread = spread;
	}

	public long getTicks() {
		return ticks;
	}

	public void setTicks(long ticks) {
		this.ticks = ticks;
	}

	public double getLiquidity() {
		return liquidity;
	}

	public void setLiquidity(double liquidity) {
		this.liquidity = liquidity;
	}

	public String getTf() {
		return tf;
	}

	public void setTf(String tf) {
		this.tf = tf;
	}
	
	
}
