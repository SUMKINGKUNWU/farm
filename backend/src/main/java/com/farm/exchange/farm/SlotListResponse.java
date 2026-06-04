package com.farm.exchange.farm;

import java.util.List;

public class SlotListResponse {

    private final String slotType;
    private final int currentSlots;
    private final int maxSlots;
    private final long nextExpandCost;
    private final List<SlotResponse> slots;

    public SlotListResponse(String slotType, int currentSlots, int maxSlots, long nextExpandCost, List<SlotResponse> slots) {
        this.slotType = slotType;
        this.currentSlots = currentSlots;
        this.maxSlots = maxSlots;
        this.nextExpandCost = nextExpandCost;
        this.slots = slots;
    }

    public String getSlotType() {
        return slotType;
    }

    public int getCurrentSlots() {
        return currentSlots;
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public long getNextExpandCost() {
        return nextExpandCost;
    }

    public List<SlotResponse> getSlots() {
        return slots;
    }
}

