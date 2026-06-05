package com.farm.exchange.bulk;

import com.farm.exchange.common.ApiException;
import com.farm.exchange.common.ErrorCode;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BulkTokenService {

    private final JdbcTemplate jdbcTemplate;

    public BulkTokenService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BulkTokenResponse> list(UUID userId) {
        return jdbcTemplate.query(
                "select id, token_code, allowed_item_type, single_trade_limit, total_limit, used_amount, remaining_uses, expires_at, status " +
                        "from bulk_trade_tokens where user_id = ? order by created_at desc",
                (rs, rowNum) -> new BulkTokenResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("token_code"),
                        rs.getString("allowed_item_type"),
                        nullableLong(rs, "single_trade_limit"),
                        nullableLong(rs, "total_limit"),
                        rs.getLong("used_amount"),
                        rs.getInt("remaining_uses"),
                        rs.getObject("expires_at", OffsetDateTime.class),
                        rs.getString("status")
                ),
                userId
        );
    }

    @Transactional
    public BulkTokenResponse issue(UUID userId, BulkTokenRequest request) {
        ensureActiveUser(userId);
        String allowedItemType = normalizeItemType(request.getAllowedItemType());
        int remainingUses = request.getRemainingUses() == null ? 1 : request.getRemainingUses();
        int expireHours = request.getExpireHours() == null ? 168 : request.getExpireHours();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(expireHours);
        String tokenCode = "BULK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);

        jdbcTemplate.update(
                "insert into bulk_trade_tokens (user_id, token_code, allowed_item_type, single_trade_limit, total_limit, remaining_uses, expires_at) values (?, ?, ?, ?, ?, ?, ?)",
                userId,
                tokenCode,
                allowedItemType,
                request.getSingleTradeLimit(),
                request.getTotalLimit(),
                remainingUses,
                Timestamp.from(expiresAt.toInstant())
        );
        return findByCode(userId, tokenCode);
    }

    @Transactional
    public UUID consumeIfRequired(UUID userId, BulkTradeItem item, long quantity, long tradeAmount, String tokenCode) {
        if (!isBulkTrade(item, quantity, tradeAmount)) {
            return null;
        }
        if (!StringUtils.hasText(tokenCode)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.BULK_TOKEN_REQUIRED, "大宗交易需要提供有效令牌");
        }

        BulkTokenSnapshot token = lockActiveToken(userId, tokenCode);
        if (token.expiresAt.isBefore(OffsetDateTime.now())) {
            jdbcTemplate.update("update bulk_trade_tokens set status = 'EXPIRED', updated_at = now(), version = version + 1 where id = ?", token.id);
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.BULK_TOKEN_EXPIRED, "大宗交易令牌已过期");
        }
        if (token.remainingUses <= 0 || !"ACTIVE".equals(token.status)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.BULK_TOKEN_INVALID, "大宗交易令牌不可用");
        }
        if (StringUtils.hasText(token.allowedItemType) && !token.allowedItemType.equals(item.getItemType())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.BULK_TOKEN_INVALID, "大宗交易令牌不支持该商品类型");
        }
        if (token.singleTradeLimit != null && tradeAmount > token.singleTradeLimit) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.BULK_TOKEN_LIMIT_EXCEEDED, "交易金额超过令牌单笔限额");
        }
        if (token.totalLimit != null && token.usedAmount + tradeAmount > token.totalLimit) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.BULK_TOKEN_LIMIT_EXCEEDED, "交易金额超过令牌总限额");
        }

        int remainingAfter = token.remainingUses - 1;
        jdbcTemplate.update(
                "update bulk_trade_tokens set used_amount = used_amount + ?, remaining_uses = ?, status = case when ? = 0 then 'USED' else status end, updated_at = now(), version = version + 1 where id = ?",
                tradeAmount,
                remainingAfter,
                remainingAfter,
                token.id
        );
        jdbcTemplate.update(
                "insert into asset_ledger (user_id, asset_type, change_amount, balance_after, reason, ref_type, ref_id) values (?, 'TOKEN', -1, ?, 'BULK_TOKEN_USE', 'BULK_TOKEN', ?)",
                userId,
                remainingAfter,
                token.id
        );
        return token.id;
    }

    private boolean isBulkTrade(BulkTradeItem item, long quantity, long tradeAmount) {
        Long quantityThreshold = item.getBulkQuantityThreshold();
        Long amountThreshold = item.getBulkAmountThreshold();
        return (quantityThreshold != null && quantity >= quantityThreshold)
                || (amountThreshold != null && tradeAmount >= amountThreshold);
    }

    private BulkTokenResponse findByCode(UUID userId, String tokenCode) {
        return jdbcTemplate.query(
                "select id, token_code, allowed_item_type, single_trade_limit, total_limit, used_amount, remaining_uses, expires_at, status from bulk_trade_tokens where user_id = ? and token_code = ?",
                rs -> {
                    if (!rs.next()) {
                        throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.BULK_TOKEN_INVALID, "大宗交易令牌不存在");
                    }
                    return new BulkTokenResponse(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("token_code"),
                            rs.getString("allowed_item_type"),
                            nullableLong(rs, "single_trade_limit"),
                            nullableLong(rs, "total_limit"),
                            rs.getLong("used_amount"),
                            rs.getInt("remaining_uses"),
                            rs.getObject("expires_at", OffsetDateTime.class),
                            rs.getString("status")
                    );
                },
                userId,
                tokenCode
        );
    }

    private BulkTokenSnapshot lockActiveToken(UUID userId, String tokenCode) {
        return jdbcTemplate.query(
                "select id, allowed_item_type, single_trade_limit, total_limit, used_amount, remaining_uses, expires_at, status " +
                        "from bulk_trade_tokens where user_id = ? and token_code = ? for update",
                rs -> {
                    if (!rs.next()) {
                        throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.BULK_TOKEN_INVALID, "大宗交易令牌不存在或不属于当前用户");
                    }
                    return new BulkTokenSnapshot(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("allowed_item_type"),
                            nullableLong(rs, "single_trade_limit"),
                            nullableLong(rs, "total_limit"),
                            rs.getLong("used_amount"),
                            rs.getInt("remaining_uses"),
                            rs.getObject("expires_at", OffsetDateTime.class),
                            rs.getString("status")
                    );
                },
                userId,
                tokenCode
        );
    }

    private void ensureActiveUser(UUID userId) {
        Integer exists = jdbcTemplate.queryForObject("select count(*) from app_users where id = ? and status = 'ACTIVE'", Integer.class, userId);
        if (exists == null || exists == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND, "用户不存在或状态不可用");
        }
    }

    private String normalizeItemType(String itemType) {
        if (!StringUtils.hasText(itemType)) {
            return null;
        }
        return itemType.trim().toUpperCase(Locale.ROOT);
    }

    private Long nullableLong(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private static class BulkTokenSnapshot {
        private final UUID id;
        private final String allowedItemType;
        private final Long singleTradeLimit;
        private final Long totalLimit;
        private final long usedAmount;
        private final int remainingUses;
        private final OffsetDateTime expiresAt;
        private final String status;

        private BulkTokenSnapshot(UUID id, String allowedItemType, Long singleTradeLimit, Long totalLimit, long usedAmount, int remainingUses, OffsetDateTime expiresAt, String status) {
            this.id = id;
            this.allowedItemType = allowedItemType;
            this.singleTradeLimit = singleTradeLimit;
            this.totalLimit = totalLimit;
            this.usedAmount = usedAmount;
            this.remainingUses = remainingUses;
            this.expiresAt = expiresAt;
            this.status = status;
        }
    }
}
