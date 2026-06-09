package com.farm.exchange.activity;

import java.util.List;

public class PlayerTradeFilterOptionResponse {

    private final List<String> reasons;

    public PlayerTradeFilterOptionResponse(List<String> reasons) {
        this.reasons = reasons;
    }

    public List<String> getReasons() {
        return reasons;
    }
}
