package com.quantlabs.QuantTester.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.quantlabs.QuantTester.tools.utils.CommissionCalculator;
import com.quantlabs.QuantTester.tools.utils.SwapCalculator;

public class Trade {
	private static final double TAX_PER_TRADE = 0;

	private static final double COMMISSION_PER_LOT = 7.0;

	public static final double CONTRACT_SIZE = 100000.0;

	/**
	 * Daily interest‐rate differential (decimal), e.g. if (R_quote – R_base) =
	 * 0.50% annual and you use 365 days: 0.0050 / 365 ≈ 0.0000136986 Loaded/updated
	 * from your rates feed.
	 * 
	 * private static double DAILY_SWAP_RATE_DIFF = 0.0050 / 365;
	 */

	public enum OrderType {
		BUY, SELL, BUY_LIMIT, SELL_LIMIT, BUY_STOP, SELL_STOP
	}

	private static int counter = 0;

	public final int orderId;
	public final String symbol;
	public final OrderType type;
	public double lotSize;
	public final double openPrice;
	public double sl;
	public double tp;
	public boolean trailingStop;
	public String comment;
	public LocalDateTime openTime;
	public LocalDateTime closeTime;
	public double closePrice;
	public boolean isClosed;
	public boolean isPending;
	public double activationPrice;
	public LocalDateTime expirationTime;
	public double highestPrice;
	public double lowestPrice;

	private boolean isTripleRollover;

	// P&L fields
	private double grossPnL;
	private double commission;
	private double tax;
	private double swap;
	private double netPnL;

	public Trade(String symbol, OrderType type, double lotSize, double openPrice, double sl, double tp, boolean trailingStop,
			String comment, boolean isPending, double activationPrice, LocalDateTime expirationTime, boolean isTripleRollover) {
		this.orderId = ++counter;
		this.symbol = symbol;
		this.type = type;
		this.lotSize = lotSize;
		this.openPrice = openPrice;
		this.sl = sl;
		this.tp = tp;
		this.trailingStop = trailingStop;
		this.comment = comment;
		this.openTime = LocalDateTime.now();
		this.isClosed = false;
		this.isPending = isPending;
		this.activationPrice = activationPrice;
		this.expirationTime = expirationTime;
		this.highestPrice = openPrice;
		this.lowestPrice = openPrice;
		this.isTripleRollover = isTripleRollover;
	}

	public void close(double closePrice) {
		this.isClosed = true;
		this.closeTime = LocalDateTime.now();
		this.closePrice = closePrice;
		computePnL();
	}

	/** Apply swap using configured rate. */
	public void applySwap() {
		this.isTripleRollover = openTime.getDayOfWeek().getValue() == 3;
		
		this.swap = SwapCalculator.computeSwapAmount(this.lotSize, this.openPrice, CONTRACT_SIZE, this.isTripleRollover, type == OrderType.BUY);
	}

	/** Apply swap with a provided rate differential. */
	public void applySwap(double dailyRateDiff) {
		this.isTripleRollover = openTime.getDayOfWeek().getValue() == 3;
		this.swap = SwapCalculator.computeSwapAmount(this.lotSize, this.openPrice, CONTRACT_SIZE, dailyRateDiff, this.isTripleRollover, type == OrderType.BUY);
	}

	public void computePnL() {
		if (closeTime == null)
			return;

		double points = (type == OrderType.BUY ? closePrice - openPrice : openPrice - closePrice);

		this.grossPnL = points * CONTRACT_SIZE * lotSize;

		this.commission = CommissionCalculator.computeCommissionPerLot(lotSize, COMMISSION_PER_LOT);

		this.tax = Math.abs(grossPnL) * TAX_PER_TRADE;
		
		applySwap();

		this.netPnL = grossPnL - Math.abs(commission) - Math.abs(tax) - Math.abs(swap);
	}

	/**
	 * Compute profit and loss, incorporating swap, commission, and tax.
	 *
	 * @param closePrice       price at which the trade is closed
	 * @param commissionPerLot commission charged per lot in quote currency
	 * @param taxRatePercent   tax rate as a percentage (e.g. 20.0 for 20%)
	 * @return total PnL after commission and tax
	 */
	public double computePnl(double closePrice, double commissionPerLot, double taxRatePercent) {
		// 1. Calculate swap
		applySwap();

		// 2. Gross PnL: positive for BUY when price rises, SELL when price falls
		double priceDiff = (type == OrderType.BUY) ? (closePrice - openPrice) : (openPrice - closePrice);

		grossPnL = priceDiff * lotSize * CONTRACT_SIZE;

		// 3. Commission cost
		commission = CommissionCalculator.computeCommissionPerLot(lotSize, commissionPerLot);

		// 4. Net PnL before tax
		this.netPnL = grossPnL - Math.abs(commission) - Math.abs(swap);

		// 5. Tax on positive net profit
		double taxCost = 0.0;

		if (netPnL > 0) {
			taxCost = netPnL * (taxRatePercent / 100.0);
		}

		// 6. Final PnL after commission and tax
		return netPnL - taxCost;
	}

	// Overload: compute PnL based from closePrice
	public double computePnl(double closePrice) {
		
		double priceDiff = (type == OrderType.BUY) ? (closePrice - openPrice) : (openPrice - closePrice);
		this.grossPnL = priceDiff * lotSize * CONTRACT_SIZE;

		this.commission = CommissionCalculator.computeCommissionPerLot(lotSize, COMMISSION_PER_LOT);

		this.tax = Math.abs(grossPnL) * TAX_PER_TRADE;

		applySwap();

		this.netPnL = grossPnL - Math.abs(commission) - Math.abs(tax) - Math.abs(swap);

		return netPnL;
	}

	public double getUnrealizedPnL(double bid, double ask) {
		double price = (type == OrderType.BUY) ? bid : ask;
		return computePnl(price);
	}
	
	public double getGrossPnL() {
		computePnL();
		return grossPnL;
	}
	
	public double getGrossUnrealizedPnL(double bid, double ask) {
		getUnrealizedPnL(bid, ask);
		return grossPnL;
	}

	public double getCommission() {
		computePnL();
		return commission;
	}
	
	public double getCommissionUnrealizedPnL(double bid, double ask) {
		getUnrealizedPnL(bid, ask);
		return commission;
	}

	public double getTax() {
		computePnL();
		return tax;
	}
	
	public double getTaxUnrealizedPnL(double bid, double ask) {
		getUnrealizedPnL(bid, ask);
		return tax;
	}

	public double getSwap() {
		computePnL();
		return swap;
	}
	
	public double getSwapUnrealizedPnL(double bid, double ask) {
		getUnrealizedPnL(bid, ask);
		return swap;
	}

	public double getNetPnL() {
		computePnL();
		return netPnL;
	}
	
	public double getNetUnrealizedPnL(double bid, double ask) {
		getUnrealizedPnL(bid, ask);
		return netPnL;
	}

	public String getSymbol() {
		return symbol;
	}

	// Static helpers
	/*public static boolean cancelPendingOrderById(List<Trade> pending, int id) {
		return pending.removeIf(t -> t.orderId == id);
	}

	public static boolean deleteOrderById(List<Trade> trades, int id) {
		return trades.removeIf(t -> t.orderId == id);
	}*/
}
