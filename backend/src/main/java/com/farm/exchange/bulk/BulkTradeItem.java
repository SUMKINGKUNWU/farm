package com.farm.exchange.bulk;

import java.util.UUID;

public class BulkTradeItem {

    private final UUID itemId;
    private final String itemCode;
    private final String itemType;
    private final Long bulkQuantityThreshold;
    private final Long bulkAmountThreshold;

    public BulkTradeItem(UUID itemId, String itemCode, String itemType, Long bulkQuantityThreshold, Long bulkAmountThreshold) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemType = itemType;
        this.bulkQuantityThreshold = bulkQuantityThreshold;
        this.bulkAmountThreshold = bulkAmountThreshold;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemType() {
        return itemType;
    }

    public Long getBulkQuantityThreshold() {
        return bulkQuantityThreshold;
    }

    public Long getBulkAmountThreshold() {
        return bulkAmountThreshold;
    }
}
