package com.farm.exchange.activity;

import java.util.List;

public class PlayerTradeQueryResponse {

    private final List<PlayerTradeRecordResponse> records;
    private final long total;
    private final int page;
    private final int pageSize;
    private final boolean hasNext;

    public PlayerTradeQueryResponse(List<PlayerTradeRecordResponse> records, long total, int page, int pageSize, boolean hasNext) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.hasNext = hasNext;
    }

    public List<PlayerTradeRecordResponse> getRecords() {
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
