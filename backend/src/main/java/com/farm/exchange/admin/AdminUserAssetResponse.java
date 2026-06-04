package com.farm.exchange.admin;

import java.util.List;
import java.util.UUID;

public class AdminUserAssetResponse {

    private final UUID userId;
    private final String username;
    private final String nickname;
    private final String status;
    private final long balance;
    private final long lockedBalance;
    private final List<AdminInventoryItemResponse> inventory;

    public AdminUserAssetResponse(UUID userId, String username, String nickname, String status, long balance, long lockedBalance, List<AdminInventoryItemResponse> inventory) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.status = status;
        this.balance = balance;
        this.lockedBalance = lockedBalance;
        this.inventory = inventory;
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

    public String getStatus() {
        return status;
    }

    public long getBalance() {
        return balance;
    }

    public long getLockedBalance() {
        return lockedBalance;
    }

    public List<AdminInventoryItemResponse> getInventory() {
        return inventory;
    }
}
