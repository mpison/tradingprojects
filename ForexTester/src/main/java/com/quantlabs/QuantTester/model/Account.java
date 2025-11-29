package com.quantlabs.QuantTester.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import com.quantlabs.QuantTester.tools.utils.RiskManagerUtil;

public class Account {
	public static final double CONTRACT_SIZE = Trade.CONTRACT_SIZE; // lot unit size
	private double balance;
	private double equity;
	private double margin;
	private double freeMargin;
	private double marginLevel;
	private double leverage;
	private double drawdown;
	private double stopOutLevel;
	private double marginCallLevel;

	public LinkedHashMap<String, Quote> latestQuotes = new LinkedHashMap<>(); // symbol -> latest quote

	public List<Trade> openTrades = new ArrayList<>();
	public List<Trade> tradeHistory = new ArrayList<>();
	public List<Trade> pendingOrders = new ArrayList<>();

	public Account(double initialBalance, double leverage, double marginCallLevel, double stopOutLevel) {
		this.balance = initialBalance;
		this.leverage = leverage;
		this.marginCallLevel = marginCallLevel;
		this.stopOutLevel = stopOutLevel;
	}

	/**
	 * Updates the latest quote for a symbol.
	 */
	public void updateQuote(Quote quote) {
		latestQuotes.put(quote.getSymbol(), quote);
	}

	/**
	 * Updates margin, equity, free margin, margin level, and drawdown using bid/ask
	 * for unrealized PnL.
	 */
	public void updateMetrics() {
		// Total used margin
		margin = openTrades.stream().mapToDouble(t -> t.lotSize * CONTRACT_SIZE / leverage).sum();

		// Unrealized PnL sum
		double openTradePnl = openTrades.stream().mapToDouble(t -> {
			Quote q = latestQuotes.get(t.symbol);
			return q != null ? t.getUnrealizedPnL(q.getBid(), q.getAsk()) : 0;
		}).sum();

		equity = balance + openTradePnl;

		freeMargin = RiskManagerUtil.calculateFreeMargin(equity, margin);

		marginLevel = margin > 0 ? (equity / margin) * 100 : 0;
		
		drawdown = balance > 0 ? ((balance - equity) / balance) * 100 : 0;
	}

	public double getBalance() {
		// updateMetrics();
		return balance;
	}

	public void setBalance(double balance) {
		this.balance = balance;
	}

	public double getEquity() {
		updateMetrics();
		return equity;
	}

	public void setEquity(double equity) {
		this.equity = equity;
	}

	public double getMargin() {
		updateMetrics();
		return margin;
	}

	public void setMargin(double margin) {
		this.margin = margin;
	}

	public double getFreeMargin() {
		updateMetrics();
		return freeMargin;
	}

	public void setFreeMargin(double freeMargin) {
		this.freeMargin = freeMargin;
	}

	public double getMarginLevel() {
		updateMetrics();
		return marginLevel;
	}

	public void setMarginLevel(double marginLevel) {
		this.marginLevel = marginLevel;
	}

	public double getLeverage() {
		updateMetrics();
		return leverage;
	}

	public void setLeverage(double leverage) {
		this.leverage = leverage;
	}

	public double getDrawdown() {
		updateMetrics();
		return drawdown;
	}

	public void setDrawdown(double drawdown) {
		this.drawdown = drawdown;
	}

	public double getStopOutLevel() {
		updateMetrics();
		return stopOutLevel;
	}

	public void setStopOutLevel(double stopOutLevel) {
		this.stopOutLevel = stopOutLevel;
	}

	public double getMarginCallLevel() {
		updateMetrics();
		return marginCallLevel;
	}

	public void setMarginCallLevel(double marginCallLevel) {
		this.marginCallLevel = marginCallLevel;
	}

	public List<Trade> getOpenTrades() {
		updateMetrics();
		return openTrades;
	}

	public void setOpenTrades(List<Trade> openTrades) {
		this.openTrades = openTrades;
	}

	public List<Trade> getTradeHistory() {
		updateMetrics();
		return tradeHistory;
	}

	public void setTradeHistory(List<Trade> tradeHistory) {
		this.tradeHistory = tradeHistory;
	}

	public List<Trade> getPendingOrders() {
		updateMetrics();
		return pendingOrders;
	}

	public void setPendingOrders(List<Trade> pendingOrders) {
		this.pendingOrders = pendingOrders;
	}

	public String accountMetricsSummary() {
		return String.format(
				"Balance: %.2f, Equity: %.2f, Margin: %.2f, Free: %.2f, MarginLvl: %.2f%%, Drawdown: %.2f%%", balance,
				equity, margin, freeMargin, marginLevel, drawdown);
	}
}