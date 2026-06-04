package com.farm.exchange.user;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class SetTradePasswordRequest {

    @NotBlank(message = "交易密码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "交易密码必须是 6 位数字")
    private String tradePassword;

    public String getTradePassword() {
        return tradePassword;
    }

    public void setTradePassword(String tradePassword) {
        this.tradePassword = tradePassword;
    }
}

