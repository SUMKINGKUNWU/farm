package com.farm.exchange.user;

import java.util.UUID;

public class LoginResponse {

    private final UUID userId;
    private final String username;
    private final String nickname;
    private final String role;
    private final String tokenType;
    private final String accessToken;

    public LoginResponse(UUID userId, String username, String nickname, String role, String tokenType, String accessToken) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.role = role;
        this.tokenType = tokenType;
        this.accessToken = accessToken;
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

    public String getTokenType() {
        return tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
