package com.farm.exchange.activity;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PlayerLedgerEntryResponse {

    private final UUID ledgerId;
    private final String assetType;
    private final UUID itemId;
    private final String itemCode;
    private final long changeAmount;
    private final Long balanceAfter;
    private final String reason;
    private final String refType;
    private final UUID refId;
    private final OffsetDateTime createdAt;

    public PlayerLedgerEntryResponse(UUID ledgerId, String assetType, UUID itemId, String itemCode, long changeAmount, Long balanceAfter, String reason, String refType, UUID refId, OffsetDateTime createdAt) {
        this.ledgerId = ledgerId;
        this.assetType = assetType;
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.changeAmount = changeAmount;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
        this.refType = refType;
        this.refId = refId;
        this.createdAt = createdAt;
    }

    public UUID getLedgerId() {
        return ledgerId;
    }

    public String getAssetType() {
        return assetType;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public long getChangeAmount() {
        return changeAmount;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }

    public String getReason() {
        return reason;
    }

    public String getRefType() {
        return refType;
    }

    public UUID getRefId() {
        return refId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
