package com.farm.exchange.admin;

import java.util.List;

public class AdminAuditLogFilterOptionsResponse {

    private final List<String> reasonOptions;

    public AdminAuditLogFilterOptionsResponse(List<String> reasonOptions) {
        this.reasonOptions = reasonOptions;
    }

    public List<String> getReasonOptions() {
        return reasonOptions;
    }
}
