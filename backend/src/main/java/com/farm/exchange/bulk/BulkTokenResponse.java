package com.farm.exchange.bulk;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BulkTokenResponse {

    private final UUID tokenId;
    private final String tokenCode;
    private final String allowedItemType;
    private final Long singleTradeLimit;
    private final Long totalLimit;
    private final long usedAmount;
    private final int remainingUses;
    private final OffsetDateTime expiresAt;
    private final String status;

    public BulkTokenResponse(UUID tokenId, String tokenCode, String allowedItemType, Long singleTradeLimit, Long totalLimit, long usedAmount, int remainingUses, OffsetDateTime expiresAt, String status) {
        this.tokenId = tokenId;
        this.tokenCode = tokenCode;
        this.allowedItemType = allowedItemType;
        this.singleTradeLimit = singleTradeLimit;
        this.totalLimit = totalLimit;
        this.usedAmount = usedAmount;
        this.remainingUses = remainingUses;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public UUID getTokenId() {
        return tokenId;
    }

    public String getTokenCode() {
        return tokenCode;
    }

    public String getAllowedItemType() {
        return allowedItemType;
    }

    public Long getSingleTradeLimit() {
        return singleTradeLimit;
    }

    public Long getTotalLimit() {
        return totalLimit;
    }

    public long getUsedAmount() {
        return usedAmount;
    }

    public int getRemainingUses() {
        return remainingUses;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getStatus() {
        return status;
    }
}
