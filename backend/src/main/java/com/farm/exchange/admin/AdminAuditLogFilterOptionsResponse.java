package com.farm.exchange.admin;

import java.util.List;

public class AdminAuditLogFilterOptionsResponse {

    private final List<String> actionOptions;
    private final List<String> targetTypeOptions;
    private final List<String> reasonOptions;

    public AdminAuditLogFilterOptionsResponse(List<String> actionOptions, List<String> targetTypeOptions, List<String> reasonOptions) {
        this.actionOptions = actionOptions;
        this.targetTypeOptions = targetTypeOptions;
        this.reasonOptions = reasonOptions;
    }

    public List<String> getActionOptions() {
        return actionOptions;
    }

    public List<String> getTargetTypeOptions() {
        return targetTypeOptions;
    }

    public List<String> getReasonOptions() {
        return reasonOptions;
    }
}
