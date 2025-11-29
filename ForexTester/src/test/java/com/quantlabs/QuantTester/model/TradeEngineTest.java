package com.quantlabs.QuantTester.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TradeEngineTest {
	private Account account;
	private TradeEngine engine;

	@BeforeEach
	void setup() {
		account = new Account(1000.0, 100.0, 50.0, 20.0);
		engine = new TradeEngine(account);
	}

	@Test
	void testOpenTradeInsufficientMargin() {
		String res = engine.openTrade("EURUSD", Trade.OrderType.BUY, 10.0, 1.1000, 1.0900, 1.1100, false, "", null);
		assertTrue(res.contains("Rejected"));
	}

	@Test
	void testPendingOrderActivation() {
		engine.openTrade("EURUSD", Trade.OrderType.BUY_LIMIT, 0.1, 1.1050, 1.0900, 1.1100, false, "", LocalDateTime.now().plusMinutes(5));
		// Simulate quotes with ask <= activation
		List<Quote> quotes = Arrays.asList(new Quote("EURUSD", LocalDateTime.now(), 1.1040, 1.1050));
		engine.simulateWithQuotes(quotes, "M1");
		assertEquals(0, engine.getAccount().openTrades.size());
		assertEquals(1, engine.getAccount().tradeHistory.size());
	}

	@Test
    void testCancelPendingOrder() {
        // Place pending order
        String res = engine.openTrade("EURUSD",
            Trade.OrderType.SELL_LIMIT,
            0.1,
            1.2000,
            1.2100,
            1.1900,
            false,
            "toCancel",
            LocalDateTime.now().plusMinutes(5)
        );
        int orderId = Integer.parseInt(res.replaceAll("\\D+", ""));
        // Cancel it
        engine.cancelPendingOrder(orderId);
        // Expect moved to history, and pending list empty
        assertTrue(account.pendingOrders.isEmpty());
        assertEquals(1, account.tradeHistory.size());
        Trade closed = account.tradeHistory.get(0);
        assertTrue(closed.comment.contains("Cancelled"));
    }
}
