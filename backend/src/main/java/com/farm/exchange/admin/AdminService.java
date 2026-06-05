package com.farm.exchange.admin;

import com.farm.exchange.bulk.BulkTokenResponse;
import com.farm.exchange.bulk.BulkTokenService;
import com.farm.exchange.common.ApiException;
import com.farm.exchange.common.ErrorCode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final JdbcTemplate jdbcTemplate;
    private final BulkTokenService bulkTokenService;

    public AdminService(JdbcTemplate jdbcTemplate, BulkTokenService bulkTokenService) {
        this.jdbcTemplate = jdbcTemplate;
        this.bulkTokenService = bulkTokenService;
    }

    public List<TaxConfigResponse> taxConfigs(UUID adminUserId) {
        ensureAdmin(adminUserId);
        return jdbcTemplate.query(
                "select trade_type, rate_basis_points, updated_by, updated_reason, effective_at, updated_at from tax_config order by trade_type",
                (rs, rowNum) -> new TaxConfigResponse(
                        rs.getString("trade_type"),
                        rs.getInt("rate_basis_points"),
                        nullableUuid(rs.getString("updated_by")),
                        rs.getString("updated_reason"),
                        rs.getObject("effective_at", OffsetDateTime.class),
                        rs.getObject("updated_at", OffsetDateTime.class)
                )
        );
    }

    @Transactional
    public TaxConfigResponse updateTaxConfig(UUID adminUserId, String tradeType, TaxConfigRequest request) {
        ensureAdmin(adminUserId);
        String normalizedTradeType = tradeType.trim().toUpperCase(Locale.ROOT);
        if (!"MARKET".equals(normalizedTradeType) && !"PRIVATE".equals(normalizedTradeType) && !"BULK".equals(normalizedTradeType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_OPERATION, "不支持的税率类型");
        }

        int updated = jdbcTemplate.update(
                "update tax_config set rate_basis_points = ?, effective_at = now(), updated_by = ?, updated_reason = ?, updated_at = now() where trade_type = ?",
                request.getRateBasisPoints(),
                adminUserId,
                request.getReason(),
                normalizedTradeType
        );
        if (updated != 1) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.CONFIG_MISSING, "税率配置不存在");
        }
        writeAuditLog(adminUserId, "UPDATE_TAX_CONFIG", "TAX_CONFIG", null, request.getReason());
        return taxConfig(normalizedTradeType);
    }

    @Transactional
    public BulkTokenResponse issueBulkToken(UUID adminUserId, UUID targetUserId, AdminTokenIssueRequest request) {
        ensureAdmin(adminUserId);
        BulkTokenResponse response = bulkTokenService.issue(targetUserId, request);
        writeAuditLog(adminUserId, "ISSUE_BULK_TOKEN", "APP_USER", targetUserId, "管理员发放大宗交易令牌");
        return response;
    }

    public AdminUserAssetResponse userAssets(UUID adminUserId, UUID targetUserId) {
        ensureAdmin(adminUserId);
        UserAssetHeader header = userAssetHeader(targetUserId);
        List<AdminInventoryItemResponse> inventory = jdbcTemplate.query(
                "select i.id, i.code, i.name, i.item_type, pi.available_quantity, pi.locked_quantity " +
                        "from player_inventory pi join items i on i.id = pi.item_id where pi.user_id = ? order by i.item_type, i.code",
                (rs, rowNum) -> new AdminInventoryItemResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("item_type"),
                        rs.getLong("available_quantity"),
                        rs.getLong("locked_quantity")
                ),
                targetUserId
        );
        return new AdminUserAssetResponse(header.userId, header.username, header.nickname, header.status, header.balance, header.lockedBalance, inventory);
    }

    public List<AdminTradeRecordResponse> userTrades(UUID adminUserId, UUID targetUserId) {
        ensureAdmin(adminUserId);
        ensureUserExists(targetUserId);
        return jdbcTemplate.query(
                "select 'MARKET' as trade_source, mt.id as trade_id, mt.item_id, i.code as item_code, mt.side, mt.quantity, mt.gross_amount as trade_amount, mt.tax_amount, mt.status, mt.created_at " +
                        "from market_trades mt join items i on i.id = mt.item_id where mt.user_id = ? " +
                        "union all " +
                        "select 'PRIVATE' as trade_source, p.id as trade_id, p.item_id, i.code as item_code, " +
                        "case when p.seller_user_id = ? then 'SELL' else 'BUY' end as side, p.quantity, p.price_amount as trade_amount, p.tax_amount, p.status, p.created_at " +
                        "from private_trade_offers p join items i on i.id = p.item_id where p.seller_user_id = ? or p.buyer_user_id = ? " +
                        "order by created_at desc limit 100",
                (rs, rowNum) -> new AdminTradeRecordResponse(
                        rs.getString("trade_source"),
                        UUID.fromString(rs.getString("trade_id")),
                        UUID.fromString(rs.getString("item_id")),
                        rs.getString("item_code"),
                        rs.getString("side"),
                        rs.getLong("quantity"),
                        rs.getLong("trade_amount"),
                        rs.getLong("tax_amount"),
                        rs.getString("status"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                targetUserId,
                targetUserId,
                targetUserId,
                targetUserId
        );
    }

    private TaxConfigResponse taxConfig(String tradeType) {
        return jdbcTemplate.query(
                "select trade_type, rate_basis_points, updated_by, updated_reason, effective_at, updated_at from tax_config where trade_type = ?",
                rs -> {
                    if (!rs.next()) {
                        throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.CONFIG_MISSING, "税率配置不存在");
                    }
                    return new TaxConfigResponse(
                            rs.getString("trade_type"),
                            rs.getInt("rate_basis_points"),
                            nullableUuid(rs.getString("updated_by")),
                            rs.getString("updated_reason"),
                            rs.getObject("effective_at", OffsetDateTime.class),
                            rs.getObject("updated_at", OffsetDateTime.class)
                    );
                },
                tradeType
        );
    }

    private UserAssetHeader userAssetHeader(UUID targetUserId) {
        return jdbcTemplate.query(
                "select u.id, u.username, u.nickname, u.status, w.balance, w.locked_balance " +
                        "from app_users u join wallets w on w.user_id = u.id where u.id = ?",
                rs -> {
                    if (!rs.next()) {
                        throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND, "用户不存在");
                    }
                    return new UserAssetHeader(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("username"),
                            rs.getString("nickname"),
                            rs.getString("status"),
                            rs.getLong("balance"),
                            rs.getLong("locked_balance")
                    );
                },
                targetUserId
        );
    }

    private void ensureAdmin(UUID adminUserId) {
        Integer exists = jdbcTemplate.queryForObject(
                "select count(*) from app_users where id = ? and role = 'ADMIN' and status = 'ACTIVE'",
                Integer.class,
                adminUserId
        );
        if (exists == null || exists == 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.PERMISSION_DENIED, "需要管理员权限");
        }
    }

    private void ensureUserExists(UUID userId) {
        Integer exists = jdbcTemplate.queryForObject("select count(*) from app_users where id = ?", Integer.class, userId);
        if (exists == null || exists == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
    }

    private void writeAuditLog(UUID adminUserId, String action, String targetType, UUID targetId, String reason) {
        jdbcTemplate.update(
                "insert into admin_audit_logs (admin_user_id, action, target_type, target_id, reason) values (?, ?, ?, ?, ?)",
                adminUserId,
                action,
                targetType,
                targetId,
                reason
        );
    }

    private UUID nullableUuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    private static class UserAssetHeader {
        private final UUID userId;
        private final String username;
        private final String nickname;
        private final String status;
        private final long balance;
        private final long lockedBalance;

        private UserAssetHeader(UUID userId, String username, String nickname, String status, long balance, long lockedBalance) {
            this.userId = userId;
            this.username = username;
            this.nickname = nickname;
            this.status = status;
            this.balance = balance;
            this.lockedBalance = lockedBalance;
        }
    }
}
