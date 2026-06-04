package com.farm.exchange.farm;

import java.util.UUID;

public class SlotResponse {

    private final UUID id;
    private final int slotIndex;
    private final String status;
    private final int level;

    public SlotResponse(UUID id, int slotIndex, String status, int level) {
        this.id = id;
        this.slotIndex = slotIndex;
        this.status = status;
        this.level = level;
    }

    public UUID getId() {
        return id;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public String getStatus() {
        return status;
    }

    public int getLevel() {
        return level;
    }
}

