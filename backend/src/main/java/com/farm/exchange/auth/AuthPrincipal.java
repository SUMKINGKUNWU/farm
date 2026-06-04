package com.farm.exchange.auth;

import java.util.UUID;

public class AuthPrincipal {

    private final UUID userId;
    private final String username;
    private final String role;

    public AuthPrincipal(UUID userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
