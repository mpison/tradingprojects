package com.quantlabs.QuantTester.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TradeTest {
	@Test
	void testPnLComputationBuy() {
		Trade t = new Trade("EURUSD", Trade.OrderType.BUY, 1.0, 1.1000, 1.0900, 1.1100, false, "", false, 0, null, false);
		t.close(1.1050);
		double gross = (1.1050 - 1.1000) * Trade.CONTRACT_SIZE * 1.0;
		double commission = 7.0;
		double tax = Math.abs(gross) * 0.002;
		boolean wed = t.openTime.getDayOfWeek().getValue() == 3;
		int days = wed ? 3 : 1;
		double swap = -5.0 * days * 1.0;
		double expectedNet = gross - commission - tax + swap;
		assertEquals(expectedNet, t.getNetPnL(), 1e-6);
	}

	@Test
	void testPnLComputationSell() {
		Trade t = new Trade("EURUSD", Trade.OrderType.SELL, 2.0, 1.2000, 1.2100, 1.1900, false, "", false, 0, null, false);
		t.close(1.1950);
		double gross = (1.2000 - 1.1950) * Trade.CONTRACT_SIZE * 2.0;
		double commission = 7.0 * 2.0;
		double tax = Math.abs(gross) * 0.002;
		boolean wed = t.openTime.getDayOfWeek().getValue() == 3;
		int days = wed ? 3 : 1;
		double swap = 3.0 * days * 2.0;
		double expectedNet = gross - commission - tax + swap;
		assertEquals(expectedNet, t.getNetPnL(), 1e-6);
	}

	@Test
	void testUnrealizedPnLBuy() {
		Trade t = new Trade("EURUSD", Trade.OrderType.BUY, 1.0, 1.1000, 1.0900, 1.1200, false, "", false, 0, null, false);
		double unrealized = t.getUnrealizedPnL(1.1050, 1.1052);
		double expected = (1.1050 - 1.1000) * Trade.CONTRACT_SIZE * 1.0;
		assertEquals(expected, unrealized, 1e-6);
	}

	@Test
	void testUnrealizedPnLSell() {
		Trade t = new Trade("EURUSD", Trade.OrderType.SELL, 1.0, 1.2000, 1.2100, 1.1900, false, "", false, 0, null, false);
		double unrealized = t.getUnrealizedPnL(1.1998, 1.2000);
		double expected = (1.2000 - 1.2000) * Trade.CONTRACT_SIZE * 1.0;
		assertEquals(expected, unrealized, 1e-6);
	}
}