package com.farm.exchange.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

public class TaxConfigResponse {

    private final String tradeType;
    private final int rateBasisPoints;
    private final UUID updatedBy;
    private final String updatedReason;
    private final OffsetDateTime effectiveAt;
    private final OffsetDateTime updatedAt;

    public TaxConfigResponse(String tradeType, int rateBasisPoints, UUID updatedBy, String updatedReason, OffsetDateTime effectiveAt, OffsetDateTime updatedAt) {
        this.tradeType = tradeType;
        this.rateBasisPoints = rateBasisPoints;
        this.updatedBy = updatedBy;
        this.updatedReason = updatedReason;
        this.effectiveAt = effectiveAt;
        this.updatedAt = updatedAt;
    }

    public String getTradeType() {
        return tradeType;
    }

    public int getRateBasisPoints() {
        return rateBasisPoints;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public String getUpdatedReason() {
        return updatedReason;
    }

    public OffsetDateTime getEffectiveAt() {
        return effectiveAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
