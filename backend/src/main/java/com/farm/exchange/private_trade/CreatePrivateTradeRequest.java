package com.farm.exchange.private_trade;

import java.util.UUID;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CreatePrivateTradeRequest {

    @NotNull(message = "买方用户不能为空")
    private UUID buyerUserId;

    @NotBlank(message = "商品编码不能为空")
    private String itemCode;

    @NotNull(message = "交易数量不能为空")
    @Min(value = 1, message = "交易数量必须大于 0")
    private Long quantity;

    @NotNull(message = "交易金额不能为空")
    @Min(value = 1, message = "交易金额必须大于 0")
    private Long priceAmount;

    @NotBlank(message = "交易密码不能为空")
    @Size(min = 6, max = 64, message = "交易密码长度不合法")
    private String tradePassword;

    public UUID getBuyerUserId() {
        return buyerUserId;
    }

    public void setBuyerUserId(UUID buyerUserId) {
        this.buyerUserId = buyerUserId;
    }

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

    public Long getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(Long priceAmount) {
        this.priceAmount = priceAmount;
    }

    public String getTradePassword() {
        return tradePassword;
    }

    public void setTradePassword(String tradePassword) {
        this.tradePassword = tradePassword;
    }
}

