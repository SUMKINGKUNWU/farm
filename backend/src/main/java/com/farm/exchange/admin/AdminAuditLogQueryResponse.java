package com.farm.exchange.admin;

import java.util.List;

public class AdminAuditLogQueryResponse {

    private final List<AdminAuditLogResponse> records;
    private final long total;
    private final int page;
    private final int pageSize;
    private final boolean hasNext;

    public AdminAuditLogQueryResponse(List<AdminAuditLogResponse> records, long total, int page, int pageSize, boolean hasNext) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.hasNext = hasNext;
    }

    public List<AdminAuditLogResponse> getRecords() {
        return records;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public boolean isHasNext() {
        return hasNext;
    }
}
