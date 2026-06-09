package com.farm.exchange.activity;

import java.util.List;

public class PlayerLedgerQueryResponse {

    private final List<PlayerLedgerEntryResponse> records;
    private final long total;
    private final int page;
    private final int pageSize;
    private final boolean hasNext;

    public PlayerLedgerQueryResponse(List<PlayerLedgerEntryResponse> records, long total, int page, int pageSize, boolean hasNext) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.hasNext = hasNext;
    }

    public List<PlayerLedgerEntryResponse> getRecords() {
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
