package com.farm.exchange.user;

import java.util.UUID;

public class TradePasswordResponse {

    private final UUID userId;
    private final boolean tradePasswordSet;

    public TradePasswordResponse(UUID userId, boolean tradePasswordSet) {
        this.userId = userId;
        this.tradePasswordSet = tradePasswordSet;
    }

    public UUID getUserId() {
        return userId;
    }

    public boolean isTradePasswordSet() {
        return tradePasswordSet;
    }
}

