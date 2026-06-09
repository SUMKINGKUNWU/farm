package com.farm.exchange.admin;

import com.farm.exchange.bulk.BulkTokenResponse;
import com.farm.exchange.bulk.BulkTokenService;
import com.farm.exchange.common.ApiException;
import com.farm.exchange.common.ErrorCode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private static final List<String> ALLOWED_ITEM_TYPES = List.of("SEED", "ANIMAL", "FEED", "HARVEST", "TOKEN", "CONSUMABLE");
    private static final List<String> ALLOWED_AUDIT_ACTIONS = List.of("UPDATE_TAX_CONFIG", "ISSUE_BULK_TOKEN");
    private static final List<String> ALLOWED_AUDIT_TARGET_TYPES = List.of("TAX_CONFIG", "APP_USER");

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

    public AdminUserAssetResponse userAssets(UUID adminUserId, UUID targetUserId, String itemType) {
        ensureAdmin(adminUserId);
        UserAssetHeader header = userAssetHeader(targetUserId);
        String normalizedItemType = normalizeItemType(itemType);

        String inventorySql =
                "select i.id, i.code, i.name, i.item_type, pi.available_quantity, pi.locked_quantity " +
                        "from player_inventory pi join items i on i.id = pi.item_id where pi.user_id = ?";
        List<Object> params = new ArrayList<>();
        params.add(targetUserId);
        if (!"ALL".equals(normalizedItemType)) {
            inventorySql += " and i.item_type = ?";
            params.add(normalizedItemType);
        }
        inventorySql += " order by i.item_type, i.code";

        List<AdminInventoryItemResponse> inventory = jdbcTemplate.query(
                inventorySql,
                (rs, rowNum) -> new AdminInventoryItemResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("item_type"),
                        rs.getLong("available_quantity"),
                        rs.getLong("locked_quantity")
                ),
                params.toArray()
        );
        return new AdminUserAssetResponse(header.userId, header.username, header.nickname, header.status, header.balance, header.lockedBalance, inventory);
    }

    public AdminTradeQueryResponse userTrades(UUID adminUserId, UUID targetUserId, String source, String status, int page, int pageSize) {
        ensureAdmin(adminUserId);
        ensureUserExists(targetUserId);
        String tradeSource = normalizeTradeSource(source);
        String tradeStatus = normalizeTradeStatus(status);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (safePage - 1) * safePageSize;

        String filteredSql =
                "from (" +
                        "select 'MARKET' as trade_source, mt.id as trade_id, mt.item_id, i.code as item_code, mt.side, mt.quantity, mt.gross_amount as trade_amount, mt.tax_amount, mt.status, mt.created_at " +
                        "from market_trades mt join items i on i.id = mt.item_id where mt.user_id = ? " +
                        "union all " +
                        "select 'PRIVATE' as trade_source, p.id as trade_id, p.item_id, i.code as item_code, " +
                        "case when p.seller_user_id = ? then 'SELL' else 'BUY' end as side, p.quantity, p.price_amount as trade_amount, p.tax_amount, p.status, p.created_at " +
                        "from private_trade_offers p join items i on i.id = p.item_id where p.seller_user_id = ? or p.buyer_user_id = ? " +
                        ") trade_records where 1 = 1";
        List<Object> filters = new ArrayList<>();
        if (!"ALL".equals(tradeSource)) {
            filteredSql += " and trade_source = ?";
            filters.add(tradeSource);
        }
        if (!"ALL".equals(tradeStatus)) {
            filteredSql += " and status = ?";
            filters.add(tradeStatus);
        }

        List<Object> baseParams = List.of(targetUserId, targetUserId, targetUserId, targetUserId);
        List<Object> countParams = new ArrayList<>(baseParams);
        countParams.addAll(filters);
        Long total = jdbcTemplate.queryForObject("select count(*) " + filteredSql, Long.class, countParams.toArray());

        List<Object> queryParams = new ArrayList<>(countParams);
        queryParams.add(safePageSize);
        queryParams.add(offset);
        List<AdminTradeRecordResponse> records = jdbcTemplate.query(
                "select trade_source, trade_id, item_id, item_code, side, quantity, trade_amount, tax_amount, status, created_at " +
                        filteredSql +
                        " order by created_at desc limit ? offset ?",
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
                queryParams.toArray()
        );
        long totalCount = total == null ? 0L : total;
        return new AdminTradeQueryResponse(records, totalCount, safePage, safePageSize, offset + records.size() < totalCount);
    }

    public AdminAuditLogQueryResponse auditLogs(UUID adminUserId, String action, String targetType, int page, int pageSize) {
        ensureAdmin(adminUserId);
        String normalizedAction = normalizeAuditAction(action);
        String normalizedTargetType = normalizeAuditTargetType(targetType);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (safePage - 1) * safePageSize;

        String filteredSql =
                "from admin_audit_logs l left join app_users u on u.id = l.admin_user_id where 1 = 1";
        List<Object> filters = new ArrayList<>();
        if (!"ALL".equals(normalizedAction)) {
            filteredSql += " and l.action = ?";
            filters.add(normalizedAction);
        }
        if (!"ALL".equals(normalizedTargetType)) {
            filteredSql += " and l.target_type = ?";
            filters.add(normalizedTargetType);
        }

        Long total = jdbcTemplate.queryForObject("select count(*) " + filteredSql, Long.class, filters.toArray());

        List<Object> queryParams = new ArrayList<>(filters);
        queryParams.add(safePageSize);
        queryParams.add(offset);
        List<AdminAuditLogResponse> records = jdbcTemplate.query(
                "select l.id, l.admin_user_id, u.username as admin_username, l.action, l.target_type, l.target_id, l.reason, l.created_at " +
                        filteredSql +
                        " order by l.created_at desc limit ? offset ?",
                (rs, rowNum) -> new AdminAuditLogResponse(
                        UUID.fromString(rs.getString("id")),
                        nullableUuid(rs.getString("admin_user_id")),
                        rs.getString("admin_username"),
                        rs.getString("action"),
                        rs.getString("target_type"),
                        nullableUuid(rs.getString("target_id")),
                        rs.getString("reason"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                queryParams.toArray()
        );
        long totalCount = total == null ? 0L : total;
        return new AdminAuditLogQueryResponse(records, totalCount, safePage, safePageSize, offset + records.size() < totalCount);
    }

    public List<AdminUserSearchResponse> searchUsers(UUID adminUserId, String query) {
        ensureAdmin(adminUserId);
        String keyword = query == null ? "" : query.trim();
        if (keyword.isEmpty()) {
            return List.of();
        }
        String like = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
        return jdbcTemplate.query(
                "select id, username, nickname, role, status from app_users " +
                        "where lower(username) like ? or lower(coalesce(nickname, '')) like ? " +
                        "order by case when lower(username) = ? then 0 when lower(username) like ? then 1 else 2 end, created_at desc limit 20",
                (rs, rowNum) -> new AdminUserSearchResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("username"),
                        rs.getString("nickname"),
                        rs.getString("role"),
                        rs.getString("status")
                ),
                like,
                like,
                keyword.toLowerCase(Locale.ROOT),
                keyword.toLowerCase(Locale.ROOT) + "%"
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

    private String normalizeTradeSource(String source) {
        String normalized = source == null ? "ALL" : source.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized) || "MARKET".equals(normalized) || "PRIVATE".equals(normalized)) {
            return normalized;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_OPERATION, "不支持的交易来源");
    }

    private String normalizeTradeStatus(String status) {
        String normalized = status == null ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized)
                || "COMPLETED".equals(normalized)
                || "WAIT_ACCEPT".equals(normalized)
                || "SETTLING".equals(normalized)
                || "CANCELLED".equals(normalized)
                || "EXPIRED".equals(normalized)
                || "FAILED".equals(normalized)) {
            return normalized;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_OPERATION, "不支持的交易状态");
    }

    private String normalizeItemType(String itemType) {
        String normalized = itemType == null ? "ALL" : itemType.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized) || ALLOWED_ITEM_TYPES.contains(normalized)) {
            return normalized;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_OPERATION, "不支持的物品类型");
    }

    private String normalizeAuditAction(String action) {
        String normalized = action == null ? "ALL" : action.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized) || ALLOWED_AUDIT_ACTIONS.contains(normalized)) {
            return normalized;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_OPERATION, "不支持的审计动作");
    }

    private String normalizeAuditTargetType(String targetType) {
        String normalized = targetType == null ? "ALL" : targetType.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized) || ALLOWED_AUDIT_TARGET_TYPES.contains(normalized)) {
            return normalized;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_OPERATION, "不支持的审计目标类型");
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
