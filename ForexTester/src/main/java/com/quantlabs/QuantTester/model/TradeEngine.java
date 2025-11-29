package com.quantlabs.QuantTester.model;

import java.time.LocalDateTime;
import java.util.*;

public class TradeEngine {
	private final Account account;
	private static final double COMMISSION_PER_LOT = 7.0;
	private static final double TAX_RATE = 0.002;
	private static final double SWAP_LONG = -5.0;
	private static final double SWAP_SHORT = 3.0;

	public TradeEngine(Account account) {
		this.account = account;
	}

	public String openTrade(String symbol, Trade.OrderType type, double lotSize, double price, double sl, double tp,
							boolean trailingStop, String comment, LocalDateTime expirationTime) {
		
		double requiredMargin = lotSize * Account.CONTRACT_SIZE / account.getLeverage();
		
		boolean isPending = isPendingType(type);
		
		if (!isPending && account.getFreeMargin() < requiredMargin) {
			return "Rejected: Insufficient margin.";
		}
		
		Trade trade = new Trade(symbol, type, lotSize, price, sl, tp, trailingStop, comment, isPending, price, expirationTime, false);
		
		if (isPending) {
			account.pendingOrders.add(trade);
			return "Pending order placed: ID=" + trade.orderId;
		}else {
			trade = new Trade(symbol, type, lotSize, price, sl, tp, trailingStop, comment, isPending, 0, expirationTime, false);
		}
		
		account.openTrades.add(trade);
		return "Trade opened: ID=" + trade.orderId;
	}
	
	/**
     * Determine if OrderType should be treated as pending.
     */
    private boolean isPendingType(Trade.OrderType type) {
        return switch(type) {
            case BUY_LIMIT, SELL_LIMIT, BUY_STOP, SELL_STOP -> true;
            default -> false;
        };
    }


	/**
     * General close method: handles open and pending trades.
     */
    public void closeTrade(int orderId, double closePrice, String reason) {
        // Try open trades
        Iterator<Trade> it = account.openTrades.iterator();
        while (it.hasNext()) {
            Trade t = it.next();
            if (t.orderId == orderId) {
                t.close(closePrice);
                
                account.tradeHistory.add(t);
                
                // Adjust balance for closed portion
                // Adjust balance with realized PnL
                account.setBalance( account.getBalance() + t.getNetPnL());
                it.remove();
                System.out.println("Trade closed (" + reason + "): ID=" + orderId);
                return;
            }
        }
        // Try pending orders
        it = account.pendingOrders.iterator();
        while (it.hasNext()) {
            Trade t = it.next();
            if (t.orderId == orderId) {
                t.comment += " | Cancelled";
                account.tradeHistory.add(t);
                it.remove();
                System.out.println("Pending order cancelled: ID=" + orderId);
                return;
            }
        }
        System.out.println("Trade ID not found: " + orderId);
    }

    public void cancelPendingOrder(int orderId) {
        closeTrade(orderId, 0.0, "Cancelled");
    }

        /**
     * Close a trade or pending order by its ID using current market quote for price.
     */
    public void closeTradeByOrderId(int orderId, Quote quote) {
        double closePrice = 0.0;
        // Determine close price based on type if open
        for (Trade t : account.openTrades) {
            if (t.orderId == orderId) {
                closePrice = (t.type == Trade.OrderType.BUY) ? quote.getBid() : quote.getAsk();
                closeTrade(orderId, closePrice, "ByOrderId");
                return;
            }
        }
        // If pending
        for (Trade t : account.pendingOrders) {
            if (t.orderId == orderId) {
                closeTrade(orderId, 0.0, "ByOrderId");
                return;
            }
        }
        System.out.println("Trade ID not found for closeTradeByOrderId: " + orderId);
    }

	public void checkPendingOrders(Quote quote) {
		Iterator<Trade> iterator = account.pendingOrders.iterator();
		while (iterator.hasNext()) {
			Trade order = iterator.next();
			double price = isBuyOrder(order.type) ? quote.getAsk() : quote.getBid();
			if (order.expirationTime != null && LocalDateTime.now().isAfter(order.expirationTime)) {
				System.out.println("Order expired: ID=" + order.orderId);
				iterator.remove();
				continue;
			}
			if (shouldTrigger(order, price)) {
				double requiredMargin = order.lotSize * Account.CONTRACT_SIZE / account.getLeverage();
				if (account.getFreeMargin() < requiredMargin) {
					System.out.println("Rejected pending order due to insufficient margin: ID=" + order.orderId);
					iterator.remove();
				} else {
					activateOrder(order, quote);
					iterator.remove();
				}
			}
		}
	}

	private boolean isBuyOrder(Trade.OrderType type) {
		return type == Trade.OrderType.BUY || type == Trade.OrderType.BUY_LIMIT || type == Trade.OrderType.BUY_STOP;
	}

	private boolean shouldTrigger(Trade order, double price) {
		return switch (order.type) {
		case BUY_LIMIT -> price <= order.activationPrice;
		case SELL_LIMIT -> price >= order.activationPrice;
		case BUY_STOP -> price >= order.activationPrice;
		case SELL_STOP -> price <= order.activationPrice;
		default -> false;
		};
	}

	private void activateOrder(Trade order, Quote quote) {
		order.isPending = false;
		order.openTime = LocalDateTime.now();
		account.openTrades.add(order);
		System.out.println("Pending order activated: ID=" + order.orderId);
	}

	public void evaluateSLTPAndTrailing(Quote quote) {
		Iterator<Trade> iterator = account.openTrades.iterator();
		while (iterator.hasNext()) {
			Trade trade = iterator.next();
			double price = trade.type == Trade.OrderType.BUY ? quote.getBid() : quote.getAsk();
			updateTrailingStop(trade, price);
			if (hasHitSL(trade, price) || hasHitTP(trade, price)) {
				closeAndRecordTrade(trade, price);
				iterator.remove();
			}
		}
	}

	private void updateTrailingStop(Trade trade, double price) {
		if (!trade.trailingStop)
			return;
		if (trade.type == Trade.OrderType.BUY) {
			trade.highestPrice = Math.max(trade.highestPrice, price);
			trade.sl = Math.max(trade.sl, trade.highestPrice - 0.0020);
		} else {
			trade.lowestPrice = Math.min(trade.lowestPrice, price);
			trade.sl = Math.min(trade.sl, trade.lowestPrice + 0.0020);
		}
	}

	private boolean hasHitSL(Trade trade, double price) {
		return trade.type == Trade.OrderType.BUY ? price <= trade.sl : price >= trade.sl;
	}

	private boolean hasHitTP(Trade trade, double price) {
		return trade.type == Trade.OrderType.BUY ? price >= trade.tp : price <= trade.tp;
	}

	private void closeAndRecordTrade(Trade trade, double price) {
		trade.close(price);
		account.tradeHistory.add(trade);
		account.setBalance(account.getBalance() + trade.getNetPnL());
		String reason = hasHitSL(trade, price) ? "SL" : "TP";
		System.out.println("Trade closed by " + reason + ": ID=" + trade.orderId);
	}

	public void closeTradeManually(int orderId, double closePrice) {
		account.openTrades.removeIf(trade -> {
			if (trade.orderId == orderId && !trade.isClosed) {
				trade.close(closePrice);
				account.tradeHistory.add(trade);
				account.setBalance(account.getBalance() + trade.getNetPnL());
				return true;
			}
			return false;
		});
	}

	public void printTradeHistory() {
        for (Trade t : account.tradeHistory) {
            System.out.printf(
                "ID=%d | Symbol=%s | Type=%s | Lot=%.2f | Open=%.5f | Close=%.5f | Gross=%.2f | Comm=%.2f | Tax=%.2f | Swap=%.2f | Net=%.2f | Comment=%s%n",
                t.orderId, t.symbol, t.type, t.lotSize, t.openPrice, t.closePrice,
                t.getGrossPnL(), t.getCommission(), t.getTax(), t.getSwap(), t.getNetPnL(), t.comment
            );
        }
    }

	public void simulateWithQuotes(List<Quote> quotes, String timeframe) {
		Map<String, Integer> timeframeMap = getDefaultTimeframes();
		int step = timeframeMap.getOrDefault(timeframe.toUpperCase(), 1);
		List<Double> equityCurve = new ArrayList<>();

		for (int i = 0; i < quotes.size(); i++) {
			Quote q = quotes.get(i);
			// double midPrice = (q.getBid() + q.getAsk()) / 2.0;

			account.updateQuote(q);

			checkPendingOrders(q);

			evaluateSLTPAndTrailing(q);
			
			account.updateMetrics();

			equityCurve.add(account.getEquity());
			
			if (i % step == 0) {
				printAccountMetrics();
			}
		}
		plotEquityCurve(equityCurve);
	}

	private Map<String, Integer> getDefaultTimeframes() {
		Map<String, Integer> map = new HashMap<>();
		map.put("M1", 1);
		map.put("M5", 5);
		map.put("M15", 15);
		map.put("M30", 30);
		map.put("H1", 60);
		map.put("H4", 240);
		map.put("D1", 1440);
		map.put("W1", 10080);
		return map;
	}

	private void plotEquityCurve(List<Double> equityCurve) {
		System.out.println("--- Equity Curve ---");
		for (int i = 0; i < equityCurve.size(); i += Math.max(1, equityCurve.size() / 50)) {
			double equity = equityCurve.get(i);
			int bars = (int) ((equity - account.getBalance()) / 10);
			System.out.printf("%4d: %s %.2f%n", i, "|".repeat(Math.max(0, bars)), equity);
		}
	}

	public void evaluateStopOut() {
		if (account.getMarginLevel() < account.getStopOutLevel()) {
			System.out.println("Stop out triggered. Closing all trades.");
			for (Trade t : new ArrayList<>(account.openTrades)) {
				closeTradeManually(t.orderId, t.openPrice);
			}
		}
	}

	public void printAccountMetrics() {
		System.out.println(account.accountMetricsSummary());
	}

	public boolean modifyPendingOrder(int orderId, double newActivationPrice, LocalDateTime newExpirationTime) {
		for (Trade o : account.pendingOrders) {
			if (o.orderId == orderId) {
				o.activationPrice = newActivationPrice;
				o.expirationTime = newExpirationTime;
				System.out.println("Pending order modified: ID=" + orderId);
				return true;
			}
		}
		System.out.println("Pending order not found for modification: ID=" + orderId);
		return false;
	}

	public boolean modifyTradeSettings(int orderId, double newSL, double newTP, boolean newTrailingStop,
			String newComment) {
		for (Trade t : account.openTrades) {
			if (t.orderId == orderId) {
				t.sl = newSL;
				t.tp = newTP;
				t.trailingStop = newTrailingStop;
				t.comment = newComment;
				System.out.println("Trade settings modified: ID=" + orderId);
				return true;
			}
		}
		System.out.println("Open trade not found for modification: ID=" + orderId);
		return false;
	}

	public boolean partialCloseTrade(int orderId, double lotSizeToClose, double closePrice) {
		Iterator<Trade> iterator = account.openTrades.iterator();
		while (iterator.hasNext()) {
			Trade t = iterator.next();
			if (t.orderId == orderId && t.lotSize > lotSizeToClose) {
				double originalSize = t.lotSize;
				t.lotSize = lotSizeToClose;
				t.close(closePrice);
				account.tradeHistory.add(t);
				account.setBalance(account.getBalance() + t.getNetPnL());
				iterator.remove();
				double remaining = originalSize - lotSizeToClose;
				Trade remainder = new Trade(t.getSymbol(), t.type, remaining, closePrice, t.sl, t.tp, t.trailingStop, t.comment, false,
						0, null, false);
				account.openTrades.add(remainder);
				System.out.println("Partially closed " + lotSizeToClose + " lots on ID=" + orderId);
				return true;
			}
		}
		System.out.println("Trade not found or insufficient size for partial close: ID=" + orderId);
		return false;
	}

	public boolean modifyTradeLotSize(int orderId, double newLotSize) {
		for (Trade t : account.openTrades) {
			if (t.orderId == orderId) {
				double oldSize = t.lotSize;
				if (newLotSize < oldSize) {
					return partialCloseTrade(orderId, oldSize - newLotSize, t.openPrice);
				} else if (newLotSize > oldSize) {
					t.lotSize = newLotSize;
					System.out.println("Lot size increased: ID=" + orderId + " Old=" + oldSize + " New=" + newLotSize);
					return true;
				}
				System.out.println("Lot size unchanged: ID=" + orderId);
				return false;
			}
		}
		System.out.println("Trade not found for lot size modification: ID=" + orderId);
		return false;
	}

	public Account getAccount() {
		return account;
	}

}