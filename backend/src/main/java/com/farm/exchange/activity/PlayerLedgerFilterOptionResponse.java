package com.farm.exchange.activity;

import java.util.List;

public class PlayerLedgerFilterOptionResponse {

    private final List<String> reasons;

    public PlayerLedgerFilterOptionResponse(List<String> reasons) {
        this.reasons = reasons;
    }

    public List<String> getReasons() {
        return reasons;
    }
}
