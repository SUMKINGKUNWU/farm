package com.farm.exchange.market;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class MarketTradeRequest {

    @NotBlank(message = "商品编码不能为空")
    private String itemCode;

    @NotNull(message = "交易数量不能为空")
    @Min(value = 1, message = "交易数量必须大于 0")
    private Long quantity;

    @NotBlank(message = "交易密码不能为空")
    @Size(min = 6, max = 64, message = "交易密码长度不合法")
    private String tradePassword;

    private String bulkTokenCode;

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

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
