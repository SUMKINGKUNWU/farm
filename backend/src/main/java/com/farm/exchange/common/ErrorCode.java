package com.farm.exchange.common;

public final class ErrorCode {

    public static final String BUSINESS_ERROR = "BUSINESS_ERROR";
    public static final String AUTH_REQUIRED = "AUTH_REQUIRED";
    public static final String AUTH_INVALID = "AUTH_INVALID";
    public static final String AUTH_EXPIRED = "AUTH_EXPIRED";
    public static final String AUTH_FAILED = "AUTH_FAILED";
    public static final String PERMISSION_DENIED = "PERMISSION_DENIED";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_INACTIVE = "USER_INACTIVE";
    public static final String USERNAME_EXISTS = "USERNAME_EXISTS";
    public static final String TRADE_PASSWORD_REQUIRED = "TRADE_PASSWORD_REQUIRED";
    public static final String TRADE_PASSWORD_LOCKED = "TRADE_PASSWORD_LOCKED";
    public static final String TRADE_PASSWORD_INVALID = "TRADE_PASSWORD_INVALID";
    public static final String INVALID_OPERATION = "INVALID_OPERATION";
    public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
    public static final String INSUFFICIENT_INVENTORY = "INSUFFICIENT_INVENTORY";
    public static final String BULK_TOKEN_REQUIRED = "BULK_TOKEN_REQUIRED";
    public static final String BULK_TOKEN_INVALID = "BULK_TOKEN_INVALID";
    public static final String BULK_TOKEN_EXPIRED = "BULK_TOKEN_EXPIRED";
    public static final String BULK_TOKEN_LIMIT_EXCEEDED = "BULK_TOKEN_LIMIT_EXCEEDED";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String ITEM_NOT_FOUND = "ITEM_NOT_FOUND";
    public static final String ITEM_NOT_TRADABLE = "ITEM_NOT_TRADABLE";
    public static final String WALLET_NOT_FOUND = "WALLET_NOT_FOUND";
    public static final String STATE_CONFLICT = "STATE_CONFLICT";
    public static final String CONFIG_MISSING = "CONFIG_MISSING";

    private ErrorCode() {
    }
}
