package com.farm.exchange.market;

import java.util.UUID;

public class MarketTradeResponse {

    private final UUID tradeId;
    private final String side;
    private final UUID itemId;
    private final String itemCode;
    private final long quantity;
    private final long unitPrice;
    private final long grossAmount;
    private final long taxAmount;
    private final long netAmount;
    private final long balanceAfter;
    private final long availableQuantityAfter;

    public MarketTradeResponse(UUID tradeId, String side, UUID itemId, String itemCode, long quantity, long unitPrice, long grossAmount, long taxAmount, long netAmount, long balanceAfter, long availableQuantityAfter) {
        this.tradeId = tradeId;
        this.side = side;
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.grossAmount = grossAmount;
        this.taxAmount = taxAmount;
        this.netAmount = netAmount;
        this.balanceAfter = balanceAfter;
        this.availableQuantityAfter = availableQuantityAfter;
    }

    public UUID getTradeId() {
        return tradeId;
    }

    public String getSide() {
        return side;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public long getQuantity() {
        return quantity;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public long getGrossAmount() {
        return grossAmount;
    }

    public long getTaxAmount() {
        return taxAmount;
    }

    public long getNetAmount() {
        return netAmount;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public long getAvailableQuantityAfter() {
        return availableQuantityAfter;
    }
}

