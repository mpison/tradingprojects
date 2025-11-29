package com.quantlabs.QuantTester.model.questdb;

public class OHCL {
	private String symbol;
	private String tf;
	private String broker;
	private float open, high, low, close;
	private long volume;
	private String timestamp;

	public OHCL(String symbol, String tf, String broker, float open, float high, float low, float close,
			long volume, String timestamp) {
		this.symbol = symbol;
		this.tf = tf;
		this.broker = broker;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.volume = volume;
		this.timestamp = timestamp;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public void setTf(String tf) {
		this.tf = tf;
	}

	public void setBroker(String broker) {
		this.broker = broker;
	}

	public void setOpen(float open) {
		this.open = open;
	}

	public void setHigh(float high) {
		this.high = high;
	}

	public void setLow(float low) {
		this.low = low;
	}

	public void setClose(float close) {
		this.close = close;
	}

	public void setVolume(long volume) {
		this.volume = volume;
	}

	
	public String getSymbol() {
		return symbol;
	}

	public String getTf() {
		return tf;
	}

	public String getBroker() {
		return broker;
	}

	public float getOpen() {
		return open;
	}

	public float getHigh() {
		return high;
	}

	public float getLow() {
		return low;
	}

	public float getClose() {
		return close;
	}

	public long getVolume() {
		return volume;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	
}