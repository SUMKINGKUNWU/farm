package com.farm.exchange.shop;

import java.util.UUID;

public class PurchaseResponse {

    private final UUID itemId;
    private final String itemCode;
    private final long quantity;
    private final long unitPrice;
    private final long totalAmount;
    private final long balanceAfter;
    private final long availableQuantityAfter;

    public PurchaseResponse(UUID itemId, String itemCode, long quantity, long unitPrice, long totalAmount, long balanceAfter, long availableQuantityAfter) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = totalAmount;
        this.balanceAfter = balanceAfter;
        this.availableQuantityAfter = availableQuantityAfter;
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

    public long getTotalAmount() {
        return totalAmount;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public long getAvailableQuantityAfter() {
        return availableQuantityAfter;
    }
}

