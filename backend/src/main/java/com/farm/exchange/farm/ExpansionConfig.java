package com.farm.exchange.farm;

public class ExpansionConfig {

    private final int initialSlots;
    private final int maxSlots;
    private final long firstExpandCost;
    private final int costMultiplier;

    public ExpansionConfig(int initialSlots, int maxSlots, long firstExpandCost, int costMultiplier) {
        this.initialSlots = initialSlots;
        this.maxSlots = maxSlots;
        this.firstExpandCost = firstExpandCost;
        this.costMultiplier = costMultiplier;
    }

    public int getInitialSlots() {
        return initialSlots;
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public long getFirstExpandCost() {
        return firstExpandCost;
    }

    public int getCostMultiplier() {
        return costMultiplier;
    }
}

