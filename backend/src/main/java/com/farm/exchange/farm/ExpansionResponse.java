package com.farm.exchange.farm;

public class ExpansionResponse {

    private final String slotType;
    private final int currentSlots;
    private final int maxSlots;
    private final long cost;
    private final long balanceAfter;
    private final long nextExpandCost;

    public ExpansionResponse(String slotType, int currentSlots, int maxSlots, long cost, long balanceAfter, long nextExpandCost) {
        this.slotType = slotType;
        this.currentSlots = currentSlots;
        this.maxSlots = maxSlots;
        this.cost = cost;
        this.balanceAfter = balanceAfter;
        this.nextExpandCost = nextExpandCost;
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

    public long getCost() {
        return cost;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public long getNextExpandCost() {
        return nextExpandCost;
    }
}

