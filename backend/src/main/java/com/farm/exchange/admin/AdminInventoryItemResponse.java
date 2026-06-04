package com.farm.exchange.admin;

import java.util.UUID;

public class AdminInventoryItemResponse {

    private final UUID itemId;
    private final String itemCode;
    private final String itemName;
    private final String itemType;
    private final long availableQuantity;
    private final long lockedQuantity;

    public AdminInventoryItemResponse(UUID itemId, String itemCode, String itemName, String itemType, long availableQuantity, long lockedQuantity) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.itemType = itemType;
        this.availableQuantity = availableQuantity;
        this.lockedQuantity = lockedQuantity;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemType() {
        return itemType;
    }

    public long getAvailableQuantity() {
        return availableQuantity;
    }

    public long getLockedQuantity() {
        return lockedQuantity;
    }
}
