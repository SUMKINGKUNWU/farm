package com.farm.exchange.production;

import java.util.UUID;

public class ItemResponse {

    private final UUID id;
    private final String code;
    private final String name;
    private final String itemType;
    private final long basePrice;
    private final long currentPrice;
    private final Integer growSeconds;
    private final Long outputQuantity;

    public ItemResponse(UUID id, String code, String name, String itemType, long basePrice, long currentPrice, Integer growSeconds, Long outputQuantity) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.itemType = itemType;
        this.basePrice = basePrice;
        this.currentPrice = currentPrice;
        this.growSeconds = growSeconds;
        this.outputQuantity = outputQuantity;
    }

    public UUID getId() {
        return id;
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

    public long getBasePrice() {
        return basePrice;
    }

    public long getCurrentPrice() {
        return currentPrice;
    }

    public Integer getGrowSeconds() {
        return growSeconds;
    }

    public Long getOutputQuantity() {
        return outputQuantity;
    }
}

