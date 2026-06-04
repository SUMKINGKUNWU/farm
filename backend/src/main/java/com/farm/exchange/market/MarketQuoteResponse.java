package com.farm.exchange.market;

import java.util.UUID;

public class MarketQuoteResponse {

    private final UUID itemId;
    private final String itemCode;
    private final String itemName;
    private final long basePrice;
    private final long currentPrice;
    private final long volume24h;
    private final long tradeCount24h;
    private final int changeBasisPoints;

    public MarketQuoteResponse(UUID itemId, String itemCode, String itemName, long basePrice, long currentPrice, long volume24h, long tradeCount24h, int changeBasisPoints) {
        this.itemId = itemId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.basePrice = basePrice;
        this.currentPrice = currentPrice;
        this.volume24h = volume24h;
        this.tradeCount24h = tradeCount24h;
        this.changeBasisPoints = changeBasisPoints;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public long getBasePrice() {
        return basePrice;
    }

    public long getCurrentPrice() {
        return currentPrice;
    }

    public long getVolume24h() {
        return volume24h;
    }

    public long getTradeCount24h() {
        return tradeCount24h;
    }

    public int getChangeBasisPoints() {
        return changeBasisPoints;
    }
}
