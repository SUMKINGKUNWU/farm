package com.farm.exchange.bulk;

import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

public class BulkTokenRequest {

    @Size(max = 32, message = "允许商品类型长度不能超过 32")
    private String allowedItemType;

    @Min(value = 1, message = "单笔限额必须大于 0")
    private Long singleTradeLimit;

    @Min(value = 1, message = "总限额必须大于 0")
    private Long totalLimit;

    @Min(value = 1, message = "可用次数必须大于 0")
    private Integer remainingUses;

    @Min(value = 1, message = "有效小时数必须大于 0")
    private Integer expireHours;

    public String getAllowedItemType() {
        return allowedItemType;
    }

    public void setAllowedItemType(String allowedItemType) {
        this.allowedItemType = allowedItemType;
    }

    public Long getSingleTradeLimit() {
        return singleTradeLimit;
    }

    public void setSingleTradeLimit(Long singleTradeLimit) {
        this.singleTradeLimit = singleTradeLimit;
    }

    public Long getTotalLimit() {
        return totalLimit;
    }

    public void setTotalLimit(Long totalLimit) {
        this.totalLimit = totalLimit;
    }

    public Integer getRemainingUses() {
        return remainingUses;
    }

    public void setRemainingUses(Integer remainingUses) {
        this.remainingUses = remainingUses;
    }

    public Integer getExpireHours() {
        return expireHours;
    }

    public void setExpireHours(Integer expireHours) {
        this.expireHours = expireHours;
    }
}
