package com.farm.exchange.production;

import java.util.UUID;

public class HarvestResponse {

    private final UUID growthId;
    private final String outputItemCode;
    private final long outputQuantity;
    private final long availableQuantityAfter;

    public HarvestResponse(UUID growthId, String outputItemCode, long outputQuantity, long availableQuantityAfter) {
        this.growthId = growthId;
        this.outputItemCode = outputItemCode;
        this.outputQuantity = outputQuantity;
        this.availableQuantityAfter = availableQuantityAfter;
    }

    public UUID getGrowthId() {
        return growthId;
    }

    public String getOutputItemCode() {
        return outputItemCode;
    }

    public long getOutputQuantity() {
        return outputQuantity;
    }

    public long getAvailableQuantityAfter() {
        return availableQuantityAfter;
    }
}

