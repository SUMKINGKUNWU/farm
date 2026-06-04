package com.farm.exchange.private_trade;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class AcceptPrivateTradeRequest {

    @NotBlank(message = "交易密码不能为空")
    @Size(min = 6, max = 64, message = "交易密码长度不合法")
    private String tradePassword;

    public String getTradePassword() {
        return tradePassword;
    }

    public void setTradePassword(String tradePassword) {
        this.tradePassword = tradePassword;
    }
}

