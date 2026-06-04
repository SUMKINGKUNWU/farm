package com.farm.exchange.admin;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class TaxConfigRequest {

    @NotNull(message = "税率不能为空")
    @Min(value = 0, message = "税率不能小于 0")
    @Max(value = 5000, message = "税率不能超过 50%")
    private Integer rateBasisPoints;

    @Size(max = 255, message = "调整原因长度不能超过 255")
    private String reason;

    public Integer getRateBasisPoints() {
        return rateBasisPoints;
    }

    public void setRateBasisPoints(Integer rateBasisPoints) {
        this.rateBasisPoints = rateBasisPoints;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
