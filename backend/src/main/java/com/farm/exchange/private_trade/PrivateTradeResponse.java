package com.farm.exchange.private_trade;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PrivateTradeResponse {

    private final UUID offerId;
    private final UUID sellerUserId;
    private final UUID buyerUserId;
    private final UUID itemId;
    private final String itemCode;
    private final long quantity;
    private final long priceAmount;
    private final long taxAmount;
    private final String status;
    private final OffsetDateTime expiresAt;

    public PrivateTradeResponse(UUID offerId, UUID sellerUserId, UUID buyerUserId, UUID itemId, String itemCode, long quantity, long priceAmount, long taxAmount, String status, OffsetDateTime expiresAt) {
        this.offerId = offerId;
        this.sellerUserId = sellerUserId;
        this.buyerUserId = buyerUserId;
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.quantity = quantity;
        this.priceAmount = priceAmount;
        this.taxAmount = taxAmount;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public UUID getOfferId() {
        return offerId;
    }

    public UUID getSellerUserId() {
        return sellerUserId;
    }

    public UUID getBuyerUserId() {
        return buyerUserId;
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

    public long getPriceAmount() {
        return priceAmount;
    }

    public long getTaxAmount() {
        return taxAmount;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }
}

