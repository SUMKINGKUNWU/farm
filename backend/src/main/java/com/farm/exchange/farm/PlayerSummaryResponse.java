package com.farm.exchange.farm;

import java.util.UUID;

public class PlayerSummaryResponse {

    private final UUID userId;
    private final long balance;
    private final long lockedBalance;
    private final int farmSlots;
    private final int ranchSlots;
    private final long nextFarmExpandCost;
    private final long nextRanchExpandCost;
    private final boolean tradePasswordSet;

    public PlayerSummaryResponse(UUID userId, long balance, long lockedBalance, int farmSlots, int ranchSlots, long nextFarmExpandCost, long nextRanchExpandCost, boolean tradePasswordSet) {
        this.userId = userId;
        this.balance = balance;
        this.lockedBalance = lockedBalance;
        this.farmSlots = farmSlots;
        this.ranchSlots = ranchSlots;
        this.nextFarmExpandCost = nextFarmExpandCost;
        this.nextRanchExpandCost = nextRanchExpandCost;
        this.tradePasswordSet = tradePasswordSet;
    }

    public UUID getUserId() {
        return userId;
    }

    public long getBalance() {
        return balance;
    }

    public long getLockedBalance() {
        return lockedBalance;
    }

    public int getFarmSlots() {
        return farmSlots;
    }

    public int getRanchSlots() {
        return ranchSlots;
    }

    public long getNextFarmExpandCost() {
        return nextFarmExpandCost;
    }

    public long getNextRanchExpandCost() {
        return nextRanchExpandCost;
    }

    public boolean isTradePasswordSet() {
        return tradePasswordSet;
    }
}

