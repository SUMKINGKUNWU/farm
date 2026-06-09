package com.farm.exchange.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AdminTradeRecordResponse {

    private final String tradeSource;
    private final UUID tradeId;
    private final UUID itemId;
    private final String itemCode;
    private final String side;
    private final long quantity;
    private final long tradeAmount;
    private final long taxAmount;
    private final String tradeReason;
    private final String status;
    private final OffsetDateTime createdAt;

    public AdminTradeRecordResponse(String tradeSource, UUID tradeId, UUID itemId, String itemCode, String side, long quantity, long tradeAmount, long taxAmount, String tradeReason, String status, OffsetDateTime createdAt) {
        this.tradeSource = tradeSource;
        this.tradeId = tradeId;
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.side = side;
        this.quantity = quantity;
        this.tradeAmount = tradeAmount;
        this.taxAmount = taxAmount;
        this.tradeReason = tradeReason;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getTradeSource() {
        return tradeSource;
    }

    public UUID getTradeId() {
        return tradeId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getSide() {
        return side;
    }

    public long getQuantity() {
        return quantity;
    }

    public long getTradeAmount() {
        return tradeAmount;
    }

    public long getTaxAmount() {
        return taxAmount;
    }

    public String getTradeReason() {
        return tradeReason;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
