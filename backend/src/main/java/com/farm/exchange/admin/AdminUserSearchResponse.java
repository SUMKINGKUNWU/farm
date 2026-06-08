package com.farm.exchange.admin;

import java.util.UUID;

public class AdminUserSearchResponse {

    private final UUID userId;
    private final String username;
    private final String nickname;
    private final String role;
    private final String status;

    public AdminUserSearchResponse(UUID userId, String username, String nickname, String role, String status) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
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

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }
}
