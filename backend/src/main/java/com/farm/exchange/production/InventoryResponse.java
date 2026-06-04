package com.farm.exchange.production;

import java.util.UUID;

public class InventoryResponse {

    private final UUID itemId;
    private final String code;
    private final String name;
    private final String itemType;
    private final long availableQuantity;
    private final long lockedQuantity;

    public InventoryResponse(UUID itemId, String code, String name, String itemType, long availableQuantity, long lockedQuantity) {
        this.itemId = itemId;
        this.code = code;
        this.name = name;
        this.itemType = itemType;
        this.availableQuantity = availableQuantity;
        this.lockedQuantity = lockedQuantity;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
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

