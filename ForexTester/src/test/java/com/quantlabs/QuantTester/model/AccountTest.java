package com.quantlabs.QuantTester.model;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountTest {
	@Test
	void testUpdateMetrics() {
		Account a = new Account(1000.0, 100.0, 50.0, 20.0);
		// Create two open trades with known PnL
		Trade t1 = new Trade("EURUSD", Trade.OrderType.BUY, 0.1, 1.1000, 1.0900, 1.1200, false, "", false, 0, null,	false);
		Trade t2 = new Trade("EURUSD", Trade.OrderType.SELL, 0.2, 1.2000, 1.2100, 1.1800, false, "", false, 0, null, false);
		a.openTrades.add(t1);
		a.openTrades.add(t2);
		// Update at current price
		double midPrice = 1.1100;

		double expectedMargin = (0.1 + 0.2) * Trade.CONTRACT_SIZE / 100.0;

		a.updateQuote(new Quote("EURUSD", midPrice - 1.1000, 1.2000 - midPrice));

		assertEquals(expectedMargin, a.getMargin(), 1e-6);
		
		// Equity = balance + unrealized PnL
		double pnl1 = t1.computePnl(midPrice - 1.1000); 
		double pnl2 =  t2.computePnl(1.2000 - midPrice);
		
		double expectedEquity = 1000.0 + pnl1 + pnl2;

		assertEquals(expectedEquity, a.getEquity(), 1e-6);
	}
}