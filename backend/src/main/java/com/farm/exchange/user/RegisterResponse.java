package com.farm.exchange.user;

import java.util.UUID;

public class RegisterResponse {

    private final UUID userId;
    private final String username;
    private final String nickname;
    private final long balance;
    private final int farmSlots;
    private final int ranchSlots;
    private final boolean tradePasswordSet;

    public RegisterResponse(UUID userId, String username, String nickname, long balance, int farmSlots, int ranchSlots, boolean tradePasswordSet) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.balance = balance;
        this.farmSlots = farmSlots;
        this.ranchSlots = ranchSlots;
        this.tradePasswordSet = tradePasswordSet;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public long getBalance() {
        return balance;
    }

    public int getFarmSlots() {
        return farmSlots;
    }

    public int getRanchSlots() {
        return ranchSlots;
    }

    public boolean isTradePasswordSet() {
        return tradePasswordSet;
    }
}

