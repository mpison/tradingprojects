package com.quantlabs.QuantTester.model;

import java.time.LocalDateTime;
import java.util.*;

public class TradeEngineMain {
    public static void main(String[] args) {
        Account account = new Account(10000.0, 1000.0, 50.0, 20.0);
        TradeEngine engine = new TradeEngine(account);
        engine.openTrade("EURUSD", Trade.OrderType.BUY, 0.1, 1.1000, 1.0950, 1.1100, true, "First trade", null);
        engine.openTrade("EURUSD", Trade.OrderType.SELL_LIMIT, 0.1, 1.1200, 1.1250, 1.1100, false, "Sell limit", LocalDateTime.now().plusMinutes(10));
        
        List<Quote> tickData = new ArrayList<>();
        
        
        for (int i = 0; i <= (int)((1.1210 - 1.0990) / 0.0005); i++) {
            double p = 1.0990 + i * 0.0005;
            tickData.add(new Quote("EURUSD", LocalDateTime.now().plusSeconds(i), p, p));
        }
        engine.simulateWithQuotes(tickData, "M1");
        
        engine.printTradeHistory();
        engine.printAccountMetrics();
    }
}
