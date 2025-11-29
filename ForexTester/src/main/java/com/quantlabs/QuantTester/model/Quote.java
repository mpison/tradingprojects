package com.quantlabs.QuantTester.model;

import java.time.LocalDateTime;

public class Quote {
	private double bid;
	private double ask;
	private String symbol;
	private LocalDateTime timestamp;

	public Quote(String symbol, double bid, double ask) {
		this(symbol, bid, ask, LocalDateTime.now());
	}

	public Quote(String symbol, double bid, double ask, LocalDateTime timestamp) {
		this.symbol = symbol;
		this.bid = bid;
		this.ask = ask;
		this.timestamp = timestamp;
	}
	
	public Quote(String symbol, LocalDateTime timestamp, double bid, double ask) {
        this.symbol = symbol;
        this.timestamp = timestamp;
        this.bid = bid;
        this.ask = ask;
    }


	public double getBid() {
		return bid;
	}

	public double getAsk() {
		return ask;
	}

	public String getSymbol() {
		return symbol;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		return symbol + " | Bid: " + bid + " | Ask: " + ask + " @ " + timestamp;
	}

}