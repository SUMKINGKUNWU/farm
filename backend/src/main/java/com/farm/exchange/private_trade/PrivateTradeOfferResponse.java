package com.farm.exchange.private_trade;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PrivateTradeOfferResponse {

    private final UUID offerId;
    private final UUID sellerUserId;
    private final String sellerUsername;
    private final UUID buyerUserId;
    private final String buyerUsername;
    private final UUID itemId;
    private final String itemCode;
    private final long quantity;
    private final long priceAmount;
    private final long taxAmount;
    private final String status;
    private final OffsetDateTime expiresAt;
    private final OffsetDateTime acceptedAt;
    private final OffsetDateTime cancelledAt;
    private final OffsetDateTime createdAt;

    public PrivateTradeOfferResponse(UUID offerId, UUID sellerUserId, String sellerUsername, UUID buyerUserId, String buyerUsername, UUID itemId, String itemCode, long quantity, long priceAmount, long taxAmount, String status, OffsetDateTime expiresAt, OffsetDateTime acceptedAt, OffsetDateTime cancelledAt, OffsetDateTime createdAt) {
        this.offerId = offerId;
        this.sellerUserId = sellerUserId;
        this.sellerUsername = sellerUsername;
        this.buyerUserId = buyerUserId;
        this.buyerUsername = buyerUsername;
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.quantity = quantity;
        this.priceAmount = priceAmount;
        this.taxAmount = taxAmount;
        this.status = status;
        this.expiresAt = expiresAt;
        this.acceptedAt = acceptedAt;
        this.cancelledAt = cancelledAt;
        this.createdAt = createdAt;
    }

    public UUID getOfferId() {
        return offerId;
    }

    public UUID getSellerUserId() {
        return sellerUserId;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public UUID getBuyerUserId() {
        return buyerUserId;
    }

    public String getBuyerUsername() {
        return buyerUsername;
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

    public OffsetDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
