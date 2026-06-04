package com.farm.exchange.private_trade;

import java.util.UUID;

public class PrivateTradeSettlementResponse {

    private final UUID offerId;
    private final String status;
    private final long priceAmount;
    private final long taxAmount;
    private final long buyerBalanceAfter;
    private final long sellerBalanceAfter;
    private final long buyerQuantityAfter;

    public PrivateTradeSettlementResponse(UUID offerId, String status, long priceAmount, long taxAmount, long buyerBalanceAfter, long sellerBalanceAfter, long buyerQuantityAfter) {
        this.offerId = offerId;
        this.status = status;
        this.priceAmount = priceAmount;
        this.taxAmount = taxAmount;
        this.buyerBalanceAfter = buyerBalanceAfter;
        this.sellerBalanceAfter = sellerBalanceAfter;
        this.buyerQuantityAfter = buyerQuantityAfter;
    }

    public UUID getOfferId() {
        return offerId;
    }

    public String getStatus() {
        return status;
    }

    public long getPriceAmount() {
        return priceAmount;
    }

    public long getTaxAmount() {
        return taxAmount;
    }

    public long getBuyerBalanceAfter() {
        return buyerBalanceAfter;
    }

    public long getSellerBalanceAfter() {
        return sellerBalanceAfter;
    }

    public long getBuyerQuantityAfter() {
        return buyerQuantityAfter;
    }
}

