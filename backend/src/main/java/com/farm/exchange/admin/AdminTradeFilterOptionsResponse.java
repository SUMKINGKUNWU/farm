package com.farm.exchange.admin;

import java.util.List;

public class AdminTradeFilterOptionsResponse {

    private final List<String> reasons;

    public AdminTradeFilterOptionsResponse(List<String> reasons) {
        this.reasons = reasons;
    }

    public List<String> getReasons() {
        return reasons;
    }
}
