package com.farm.exchange.private_trade;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AcceptPrivateTradeRequest {

    @NotBlank(message = "交易密码不能为空")
    @Size(min = 6, max = 64, message = "交易密码长度不合法")
    private String tradePassword;

    private String bulkTokenCode;

    public String getTradePassword() {
        return tradePassword;
    }

    public void setTradePassword(String tradePassword) {
        this.tradePassword = tradePassword;
    }

    public String getBulkTokenCode() {
        return bulkTokenCode;
    }

    public void setBulkTokenCode(String bulkTokenCode) {
        this.bulkTokenCode = bulkTokenCode;
    }
}
