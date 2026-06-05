package com.farm.exchange.production;

import java.time.OffsetDateTime;
import java.util.UUID;

public class GrowthInstanceResponse {

    private final UUID growthId;
    private final String slotType;
    private final UUID slotId;
    private final String inputItemCode;
    private final String outputItemCode;
    private final long outputQuantity;
    private final OffsetDateTime startedAt;
    private final OffsetDateTime readyAt;
    private final OffsetDateTime harvestedAt;
    private final String status;

    public GrowthInstanceResponse(UUID growthId, String slotType, UUID slotId, String inputItemCode, String outputItemCode, long outputQuantity, OffsetDateTime startedAt, OffsetDateTime readyAt, OffsetDateTime harvestedAt, String status) {
        this.growthId = growthId;
        this.slotType = slotType;
        this.slotId = slotId;
        this.inputItemCode = inputItemCode;
        this.outputItemCode = outputItemCode;
        this.outputQuantity = outputQuantity;
        this.startedAt = startedAt;
        this.readyAt = readyAt;
        this.harvestedAt = harvestedAt;
        this.status = status;
    }

    public UUID getGrowthId() {
        return growthId;
    }

    public String getSlotType() {
        return slotType;
    }

    public UUID getSlotId() {
        return slotId;
    }

    public String getInputItemCode() {
        return inputItemCode;
    }

    public String getOutputItemCode() {
        return outputItemCode;
    }

    public long getOutputQuantity() {
        return outputQuantity;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getReadyAt() {
        return readyAt;
    }

    public OffsetDateTime getHarvestedAt() {
        return harvestedAt;
    }

    public String getStatus() {
        return status;
    }
}
