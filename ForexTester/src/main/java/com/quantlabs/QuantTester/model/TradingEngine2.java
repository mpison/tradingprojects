package com.quantlabs.QuantTester.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class TradingEngine2 {
    
    public enum AssetType {
        FOREX, STOCK, CRYPTO
    }
    
    public enum TradeDirection {
        BUY, SELL
    }
    
    public enum OrderType {
        MARKET, LIMIT, STOP
    }
    
    public enum OrderStatus {
        PENDING, FILLED, CANCELLED, EXPIRED, REJECTED
    }
    
    private static abstract class Order {
        long orderId;
        String symbol;
        AssetType assetType;
        TradeDirection direction;
        double size;
        LocalDateTime creationTime;
        LocalDateTime expirationTime;
        OrderType orderType;
        OrderStatus status;
        
        Order(long orderId, String symbol, AssetType assetType, TradeDirection direction, 
             double size, OrderType orderType, LocalDateTime expirationTime) {
            this.orderId = orderId;
            this.symbol = symbol;
            this.assetType = assetType;
            this.direction = direction;
            this.size = size;
            this.orderType = orderType;
            this.creationTime = LocalDateTime.now();
            this.expirationTime = expirationTime;
            this.status = OrderStatus.PENDING;
        }
        
        abstract boolean shouldExecute(MarketPrice price);
        
        public boolean isExpired() {
            return expirationTime != null && LocalDateTime.now().isAfter(expirationTime);
        }
    }
    
    private static class LimitOrder extends Order {
        double limitPrice;
        
        LimitOrder(long orderId, String symbol, AssetType assetType, TradeDirection direction, 
                  double size, double limitPrice, LocalDateTime expirationTime) {
            super(orderId, symbol, assetType, direction, size, OrderType.LIMIT, expirationTime);
            this.limitPrice = limitPrice;
        }
        
        @Override
        boolean shouldExecute(MarketPrice price) {
            if (direction == TradeDirection.BUY) {
                return price.ask <= limitPrice; // Buy if ask price reaches our limit
            } else {
                return price.bid >= limitPrice; // Sell if bid price reaches our limit
            }
        }
    }
    
    private static class StopOrder extends Order {
        double stopPrice;
        
        StopOrder(long orderId, String symbol, AssetType assetType, TradeDirection direction, 
                 double size, double stopPrice, LocalDateTime expirationTime) {
            super(orderId, symbol, assetType, direction, size, OrderType.STOP, expirationTime);
            this.stopPrice = stopPrice;
        }
        
        @Override
        boolean shouldExecute(MarketPrice price) {
            if (direction == TradeDirection.BUY) {
                return price.ask >= stopPrice; // Buy stop triggers when price rises to stop level
            } else {
                return price.bid <= stopPrice; // Sell stop triggers when price falls to stop level
            }
        }
    }
    
    private static class Position {
        long positionId;
        String symbol;
        AssetType assetType;
        TradeDirection direction;
        double entryPrice;
        double size;
        LocalDateTime entryTime;
        double commissionPaid;
        double leverage;
        double marginUsed;
        Double stopLoss;
        Double takeProfit;
        Double trailingStop;
        double bestPrice;
        
        Position(long positionId, String symbol, AssetType assetType, TradeDirection direction, 
                double entryPrice, double size, double commissionPaid, 
                double leverage, Double stopLoss, Double takeProfit, Double trailingStop) {
            this.positionId = positionId;
            this.symbol = symbol;
            this.assetType = assetType;
            this.direction = direction;
            this.entryPrice = entryPrice;
            this.size = size;
            this.entryTime = LocalDateTime.now();
            this.commissionPaid = commissionPaid;
            this.leverage = leverage;
            this.marginUsed = calculateMarginUsed(entryPrice, size, leverage, assetType);
            this.stopLoss = stopLoss;
            this.takeProfit = takeProfit;
            this.trailingStop = trailingStop;
            this.bestPrice = entryPrice;
        }
        
        private double calculateMarginUsed(double price, double size, double leverage, AssetType assetType) {
            double notionalValue = price * size;
            double margin = notionalValue / leverage;
            
            switch (assetType) {
                case FOREX: return margin * 1.0;
                case STOCK: return margin * 1.5;
                case CRYPTO: return margin * 2.0;
                default: return margin;
            }
        }
        
        public boolean shouldClose(MarketPrice price) {
            double currentPrice = direction == TradeDirection.BUY ? price.bid : price.ask;
            
            // Update best price for trailing stop
            if (direction == TradeDirection.BUY) {
                bestPrice = Math.max(bestPrice, currentPrice);
            } else {
                bestPrice = Math.min(bestPrice, currentPrice);
            }
            
            // Check stop loss
            if (stopLoss != null) {
                if (direction == TradeDirection.BUY && currentPrice <= stopLoss) {
                    return true;
                }
                if (direction == TradeDirection.SELL && currentPrice >= stopLoss) {
                    return true;
                }
            }
            
            // Check take profit
            if (takeProfit != null) {
                if (direction == TradeDirection.BUY && currentPrice >= takeProfit) {
                    return true;
                }
                if (direction == TradeDirection.SELL && currentPrice <= takeProfit) {
                    return true;
                }
            }
            
            // Check trailing stop
            if (trailingStop != null) {
                double triggerPrice = direction == TradeDirection.BUY 
                    ? bestPrice - trailingStop 
                    : bestPrice + trailingStop;
                
                if (direction == TradeDirection.BUY && currentPrice <= triggerPrice) {
                    return true;
                }
                if (direction == TradeDirection.SELL && currentPrice >= triggerPrice) {
                    return true;
                }
            }
            
            return false;
        }
    }
    
    private AtomicLong orderIdGenerator = new AtomicLong(1);
    private AtomicLong positionIdGenerator = new AtomicLong(1);
    private List<Order> pendingOrders = new ArrayList<>();
    private List<Position> positions = new ArrayList<>();
    private double accountBalance = 100000;
    private final double taxRate = 0.20;
    private Map<String, Map<String, Double>> swapRates;
    
    public TradingEngine2() {
        initializeSwapRates();
    }
    
    private void initializeSwapRates() {
        swapRates = new HashMap<>();
        
        Map<String, Double> eurusdSwaps = new HashMap<>();
        eurusdSwaps.put("long", -0.0001);
        eurusdSwaps.put("short", -0.0002);
        swapRates.put("EURUSD", eurusdSwaps);
        
        Map<String, Double> gbpusdSwaps = new HashMap<>();
        gbpusdSwaps.put("long", -0.0002);
        gbpusdSwaps.put("short", -0.0003);
        swapRates.put("GBPUSD", gbpusdSwaps);
        
        Map<String, Double> btcusdSwaps = new HashMap<>();
        btcusdSwaps.put("long", -0.0005);
        btcusdSwaps.put("short", -0.001);
        swapRates.put("BTCUSD", btcusdSwaps);
        
        Map<String, Double> aaplSwaps = new HashMap<>();
        aaplSwaps.put("long", -0.0001);
        aaplSwaps.put("short", -0.00015);
        swapRates.put("AAPL", aaplSwaps);
    }
    
    private MarketPrice generateMarketPrice(String symbol, AssetType assetType) {
        double basePrice;
        double spread;
        
        switch (assetType) {
            case FOREX:
                basePrice = symbol.startsWith("EUR") ? 
                    ThreadLocalRandom.current().nextDouble(1.0, 1.5) : 
                    ThreadLocalRandom.current().nextDouble(1.2, 1.8);
                spread = ThreadLocalRandom.current().nextDouble(0.0001, 0.0005);
                break;
            case STOCK:
                basePrice = ThreadLocalRandom.current().nextDouble(100, 200);
                spread = ThreadLocalRandom.current().nextDouble(0.01, 0.05);
                break;
            case CRYPTO:
                basePrice = ThreadLocalRandom.current().nextDouble(30000, 60000);
                spread = ThreadLocalRandom.current().nextDouble(5, 20);
                break;
            default:
                throw new IllegalArgumentException("Unknown asset type");
        }
        
        // Simulate price movement
        double movement = ThreadLocalRandom.current().nextDouble(-0.02, 0.02) * basePrice;
        basePrice += movement;
        
        return new MarketPrice(basePrice, basePrice + spread);
    }
    
    public long placeMarketOrder(String symbol, AssetType assetType, TradeDirection direction, 
                               double size, double leverage, Double stopLoss, 
                               Double takeProfit, Double trailingStop) {
        MarketPrice price = generateMarketPrice(symbol, assetType);
        return executeOrder(symbol, assetType, direction, size, leverage, 
                          direction == TradeDirection.BUY ? price.ask : price.bid,
                          stopLoss, takeProfit, trailingStop);
    }
    
    public long placeLimitOrder(String symbol, AssetType assetType, TradeDirection direction, 
                              double size, double limitPrice, LocalDateTime expirationTime) {
        long orderId = orderIdGenerator.getAndIncrement();
        pendingOrders.add(new LimitOrder(orderId, symbol, assetType, direction, size, 
                                       limitPrice, expirationTime));
        System.out.printf("Placed limit order #%d: %s %s %s at %.5f, size: %.2f%n",
                        orderId, direction, symbol, assetType, limitPrice, size);
        return orderId;
    }
    
    public long placeStopOrder(String symbol, AssetType assetType, TradeDirection direction, 
                             double size, double stopPrice, LocalDateTime expirationTime) {
        long orderId = orderIdGenerator.getAndIncrement();
        pendingOrders.add(new StopOrder(orderId, symbol, assetType, direction, size, 
                                      stopPrice, expirationTime));
        System.out.printf("Placed stop order #%d: %s %s %s at %.5f, size: %.2f%n",
                        orderId, direction, symbol, assetType, stopPrice, size);
        return orderId;
    }
    
    public boolean modifyOrder(long orderId, Double newPrice, Double newSize, 
                             LocalDateTime newExpiration) {
        Optional<Order> orderOpt = pendingOrders.stream()
            .filter(o -> o.orderId == orderId && o.status == OrderStatus.PENDING)
            .findFirst();
        
        if (!orderOpt.isPresent()) {
            System.out.println("Order #" + orderId + " not found or not modifiable");
            return false;
        }
        
        Order order = orderOpt.get();
        if (newPrice != null) {
            if (order instanceof LimitOrder) {
                ((LimitOrder) order).limitPrice = newPrice;
            } else if (order instanceof StopOrder) {
                ((StopOrder) order).stopPrice = newPrice;
            }
        }
        if (newSize != null) {
            order.size = newSize;
        }
        if (newExpiration != null) {
            order.expirationTime = newExpiration;
        }
        
        System.out.println("Modified order #" + orderId);
        return true;
    }
    
    public boolean cancelOrder(long orderId) {
        Optional<Order> orderOpt = pendingOrders.stream()
            .filter(o -> o.orderId == orderId && o.status == OrderStatus.PENDING)
            .findFirst();
        
        if (!orderOpt.isPresent()) {
            System.out.println("Order #" + orderId + " not found or not cancellable");
            return false;
        }
        
        Order order = orderOpt.get();
        order.status = OrderStatus.CANCELLED;
        System.out.println("Cancelled order #" + orderId);
        return true;
    }
    
    private long executeOrder(String symbol, AssetType assetType, TradeDirection direction, 
                            double size, double leverage, double executionPrice,
                            Double stopLoss, Double takeProfit, Double trailingStop) {
        // Calculate commission (0.1% of trade value)
        double tradeValue = executionPrice * size;
        double commission = tradeValue * 0.001;
        
        // Calculate required margin
        double marginRequired = (tradeValue / leverage) * getMarginMultiplier(assetType);
        
        // Check if we have enough margin
        if (marginRequired > getAvailableMargin()) {
            System.out.println("Order rejected: Insufficient margin available");
            return -1;
        }
        
        long positionId = positionIdGenerator.getAndIncrement();
        Position position = new Position(positionId, symbol, assetType, direction, executionPrice, 
                                       size, commission, leverage, stopLoss, 
                                       takeProfit, trailingStop);
        positions.add(position);
        
        System.out.printf("\nExecuted order: %s %s %s at %.5f, size: %.2f, leverage: %.0fx%n",
                        direction, symbol, assetType, executionPrice, size, leverage);
        System.out.printf("Stop loss: %s, Take profit: %s, Trailing stop: %s%n",
                        formatPrice(stopLoss), formatPrice(takeProfit), formatPrice(trailingStop));
        System.out.printf("Margin used: %.2f, Available margin: %.2f%n",
                        marginRequired, getAvailableMargin());
        
        return positionId;
    }
    
    public void checkAndProcessOrders() {
        // Check for expired orders
        pendingOrders.stream()
            .filter(o -> o.status == OrderStatus.PENDING && o.isExpired())
            .forEach(o -> {
                o.status = OrderStatus.EXPIRED;
                System.out.println("Order #" + o.orderId + " expired");
            });
        
        // Remove cancelled and expired orders
        pendingOrders.removeIf(o -> o.status == OrderStatus.CANCELLED || 
                                  o.status == OrderStatus.EXPIRED || 
                                  o.status == OrderStatus.REJECTED);
        
        // Process pending orders
        List<Order> ordersToExecute = new ArrayList<>();
        List<Order> ordersToRemove = new ArrayList<>();
        
        for (Order order : pendingOrders) {
            if (order.status != OrderStatus.PENDING) continue;
            
            MarketPrice price = generateMarketPrice(order.symbol, order.assetType);
            if (order.shouldExecute(price)) {
                ordersToExecute.add(order);
            }
        }
        
        // Execute orders that should be filled
        for (Order order : ordersToExecute) {
            MarketPrice price = generateMarketPrice(order.symbol, order.assetType);
            double executionPrice = order.direction == TradeDirection.BUY ? price.ask : price.bid;
            
            // Default leverage for pending orders
            double leverage = 1.0;
            if (order instanceof LimitOrder) {
                executionPrice = ((LimitOrder) order).limitPrice;
            } else if (order instanceof StopOrder) {
                executionPrice = ((StopOrder) order).stopPrice;
            }
            
            long positionId = executeOrder(order.symbol, order.assetType, order.direction, 
                                         order.size, leverage, executionPrice, 
                                         null, null, null);
            
            if (positionId != -1) {
                order.status = OrderStatus.FILLED;
                ordersToRemove.add(order);
            } else {
                order.status = OrderStatus.REJECTED;
            }
        }
        
        // Remove filled orders
        pendingOrders.removeAll(ordersToRemove);
        
        // Check positions for SL/TP/trailing stops
        checkAndClosePositions();
    }
    
    private void checkAndClosePositions() {
        List<Integer> positionsToClose = new ArrayList<>();
        
        for (int i = 0; i < positions.size(); i++) {
            Position position = positions.get(i);
            MarketPrice price = generateMarketPrice(position.symbol, position.assetType);
            
            if (position.shouldClose(price)) {
                System.out.printf("\nAuto-closing position #%d %s %s due to SL/TP/trailing stop%n",
                                 position.positionId, position.direction, position.symbol);
                positionsToClose.add(i);
            }
        }
        
        // Close from highest index to lowest to avoid shifting issues
        Collections.sort(positionsToClose, Collections.reverseOrder());
        for (int index : positionsToClose) {
            closePosition(index);
        }
    }
    
    private String formatPrice(Double price) {
        return price == null ? "N/A" : String.format("%.5f", price);
    }
    
    private double getMarginMultiplier(AssetType assetType) {
        switch (assetType) {
            case FOREX: return 1.0;
            case STOCK: return 1.5;
            case CRYPTO: return 2.0;
            default: return 1.0;
        }
    }
    
    private double getUsedMargin() {
        return positions.stream()
                .mapToDouble(p -> p.marginUsed)
                .sum();
    }
    
    private double getAvailableMargin() {
        return accountBalance - getUsedMargin();
    }
    
    public void closePosition(int positionIndex) {
        if (positionIndex < 0 || positionIndex >= positions.size()) {
            throw new IllegalArgumentException("Invalid position index");
        }
        
        Position position = positions.get(positionIndex);
        MarketPrice price = generateMarketPrice(position.symbol, position.assetType);
        double exitPrice = position.direction == TradeDirection.BUY ? price.bid : price.ask;
        
        // Calculate P&L
        double priceDifference = position.direction == TradeDirection.BUY ? 
            exitPrice - position.entryPrice : position.entryPrice - exitPrice;
        double grossPnL = priceDifference * position.size;
        
        // Calculate swap (overnight funding)
        long daysHeld = ChronoUnit.DAYS.between(position.entryTime, LocalDateTime.now());
        double swap = calculateSwap(position, daysHeld);
        
        // 3-day swap adjustment for forex positions held over Wednesday night
        if (position.assetType == AssetType.FOREX && daysHeld >= 3) {
            swap *= 3;
        }
        
        // Net P&L
        double netPnL = grossPnL - position.commissionPaid + swap;
        
        // Tax calculation
        double tax = netPnL > 0 ? netPnL * taxRate : 0;
        double afterTaxPnL = netPnL - tax;
        
        // Update account balance
        accountBalance += afterTaxPnL;
        
        System.out.printf("Closed position #%d: %s %s %s (%.0fx leverage)%n", 
                         position.positionId, position.direction, position.symbol, 
                         position.assetType, position.leverage);
        System.out.printf("Entry: %.5f, Exit: %.5f, Size: %.2f%n", 
                         position.entryPrice, exitPrice, position.size);
        System.out.printf("Gross P&L: %.2f, Commission: %.2f, Swap: %.2f%n", 
                         grossPnL, position.commissionPaid, swap);
        System.out.printf("Net P&L: %.2f, Tax (%.0f%%): %.2f, After-tax P&L: %.2f%n", 
                         netPnL, taxRate * 100, tax, afterTaxPnL);
        System.out.printf("Margin released: %.2f, New account balance: %.2f%n",
                         position.marginUsed, accountBalance);
        
        positions.remove(positionIndex);
    }
    
    public void checkMarginLevels() {
        double usedMargin = getUsedMargin();
        double marginLevel = usedMargin > 0 ? (accountBalance / usedMargin) * 100 : 0;
        
        System.out.printf("\nMargin Report: Used=%.2f, Available=%.2f, Balance=%.2f, Level=%.1f%%%n",
                         usedMargin, getAvailableMargin(), accountBalance, marginLevel);
        
        if (marginLevel < 100) {
            System.out.println("WARNING: Margin call territory!");
        } else if (marginLevel < 150) {
            System.out.println("Warning: Approaching margin call levels");
        }
    }
    
    private double calculateSwap(Position position, long daysHeld) {
        if (!swapRates.containsKey(position.symbol)) {
            return 0;
        }
        
        String swapType = position.direction == TradeDirection.BUY ? "long" : "short";
        double swapRate = swapRates.get(position.symbol).get(swapType);
        
        return swapRate * position.size * daysHeld;
    }
    
    public void printOrderStatus() {
        System.out.println("\n=== Pending Orders ===");
        if (pendingOrders.isEmpty()) {
            System.out.println("No pending orders");
        } else {
            pendingOrders.stream()
                .filter(o -> o.status == OrderStatus.PENDING)
                .forEach(o -> {
                    String price = o instanceof LimitOrder ? 
                        String.format("%.5f", ((LimitOrder) o).limitPrice) :
                        o instanceof StopOrder ? 
                        String.format("%.5f", ((StopOrder) o).stopPrice) : "MARKET";
                    System.out.printf("Order #%d: %s %s %s %s, Size: %.2f, Expires: %s%n",
                                    o.orderId, o.orderType, o.direction, o.symbol, 
                                    price, o.size, 
                                    o.expirationTime != null ? o.expirationTime.toString() : "GTC");
                });
        }
        
        System.out.println("\n=== Open Positions ===");
        if (positions.isEmpty()) {
            System.out.println("No open positions");
        } else {
            positions.forEach(p -> {
                System.out.printf("Position #%d: %s %s %s, Entry: %.5f, Size: %.2f, Leverage: %.0fx%n",
                                 p.positionId, p.direction, p.symbol, p.assetType, 
                                 p.entryPrice, p.size, p.leverage);
                System.out.printf("  SL: %s, TP: %s, Trailing: %s, Current P&L: %.2f%n",
                                 formatPrice(p.stopLoss), formatPrice(p.takeProfit), 
                                 formatPrice(p.trailingStop), 
                                 calculateCurrentPnL(p));
            });
        }
    }
    
    private double calculateCurrentPnL(Position position) {
        MarketPrice price = generateMarketPrice(position.symbol, position.assetType);
        double currentPrice = position.direction == TradeDirection.BUY ? price.bid : price.ask;
        double priceDifference = position.direction == TradeDirection.BUY ? 
            currentPrice - position.entryPrice : position.entryPrice - currentPrice;
        return priceDifference * position.size;
    }
    
    private static class MarketPrice {
        double bid;
        double ask;
        
        MarketPrice(double bid, double ask) {
            this.bid = bid;
            this.ask = ask;
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
    	TradingEngine2 engine = new TradingEngine2();
        
        System.out.printf("Initial account balance: %.2f%n", engine.accountBalance);
        
        // Place various types of orders
        long limitOrderId = engine.placeLimitOrder(
            "EURUSD", AssetType.FOREX, TradeDirection.BUY, 
            50000, 1.0800, LocalDateTime.now().plusHours(24));
        
        long stopOrderId = engine.placeStopOrder(
            "AAPL", AssetType.STOCK, TradeDirection.SELL, 
            30, 150.0, LocalDateTime.now().plusHours(48));
        
        // Place a market order
        long marketPositionId = engine.placeMarketOrder(
            "BTCUSD", AssetType.CRYPTO, TradeDirection.BUY, 
            0.2, 10, 28000.0, 35000.0, 2000.0);
        
        // Simulate market movements
        for (int i = 0; i < 10; i++) {
            System.out.println("\n=== Market Cycle " + (i+1) + " ===");
            engine.checkAndProcessOrders();
            engine.printOrderStatus();
            engine.checkMarginLevels();
            Thread.sleep(1000);
            
            // Modify an order after a few cycles
            if (i == 3) {
                engine.modifyOrder(limitOrderId, 1.0850, null, null);
            }
            
            // Cancel an order after a few cycles
            if (i == 6) {
                engine.cancelOrder(stopOrderId);
            }
        }
        
        // Final account status
        System.out.println("\nFinal account balance: " + engine.accountBalance);
    }
}