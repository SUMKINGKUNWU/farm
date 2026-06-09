package com.farm.exchange.activity;

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

@Service
public class PlayerActivityService {

    private final JdbcTemplate jdbcTemplate;

    public PlayerActivityService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PlayerTradeQueryResponse trades(UUID userId, String source, String status, int page, int pageSize) {
        ensureActiveUser(userId);
        String tradeSource = normalizeTradeSource(source);
        String tradeStatus = normalizeTradeStatus(status);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (safePage - 1) * safePageSize;

        String filteredSql =
                "from (" +
                        "select 'MARKET' as trade_source, mt.id as trade_id, mt.item_id, i.code as item_code, mt.side, mt.quantity, mt.gross_amount as trade_amount, mt.tax_amount, mt.status, " +
                        "null::uuid as counterparty_user_id, null::varchar as counterparty_username, mt.created_at " +
                        "from market_trades mt join items i on i.id = mt.item_id where mt.user_id = ? " +
                        "union all " +
                        "select 'PRIVATE' as trade_source, p.id as trade_id, p.item_id, i.code as item_code, " +
                        "case when p.seller_user_id = ? then 'SELL' else 'BUY' end as side, p.quantity, p.price_amount as trade_amount, p.tax_amount, p.status, " +
                        "case when p.seller_user_id = ? then p.buyer_user_id else p.seller_user_id end as counterparty_user_id, " +
                        "case when p.seller_user_id = ? then buyer.username else seller.username end as counterparty_username, p.created_at " +
                        "from private_trade_offers p " +
                        "join items i on i.id = p.item_id " +
                        "join app_users seller on seller.id = p.seller_user_id " +
                        "join app_users buyer on buyer.id = p.buyer_user_id " +
                        "where p.seller_user_id = ? or p.buyer_user_id = ? " +
                        ") trade_records where 1 = 1";

        List<Object> baseParams = List.of(userId, userId, userId, userId, userId, userId);
        List<Object> filters = new ArrayList<>();
        if (!"ALL".equals(tradeSource)) {
            filteredSql += " and trade_source = ?";
            filters.add(tradeSource);
        }
        if (!"ALL".equals(tradeStatus)) {
            filteredSql += " and status = ?";
            filters.add(tradeStatus);
        }

        List<Object> countParams = new ArrayList<>(baseParams);
        countParams.addAll(filters);
        Long total = jdbcTemplate.queryForObject("select count(*) " + filteredSql, Long.class, countParams.toArray());

        List<Object> queryParams = new ArrayList<>(countParams);
        queryParams.add(safePageSize);
        queryParams.add(offset);
        List<PlayerTradeRecordResponse> records = jdbcTemplate.query(
                "select trade_source, trade_id, item_id, item_code, side, quantity, trade_amount, tax_amount, status, counterparty_user_id, counterparty_username, created_at " +
                        filteredSql +
                        " order by created_at desc limit ? offset ?",
                (rs, rowNum) -> new PlayerTradeRecordResponse(
                        rs.getString("trade_source"),
                        UUID.fromString(rs.getString("trade_id")),
                        UUID.fromString(rs.getString("item_id")),
                        rs.getString("item_code"),
                        rs.getString("side"),
                        rs.getLong("quantity"),
                        rs.getLong("trade_amount"),
                        rs.getLong("tax_amount"),
                        rs.getString("status"),
                        nullableUuid(rs.getString("counterparty_user_id")),
                        rs.getString("counterparty_username"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                queryParams.toArray()
        );

        long totalCount = total == null ? 0L : total;
        return new PlayerTradeQueryResponse(records, totalCount, safePage, safePageSize, offset + records.size() < totalCount);
    }

    public PlayerLedgerQueryResponse ledger(UUID userId, String assetType, String direction, int page, int pageSize) {
        ensureActiveUser(userId);
        String normalizedAssetType = normalizeAssetType(assetType);
        String normalizedDirection = normalizeLedgerDirection(direction);
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 50);
        int offset = (safePage - 1) * safePageSize;

        String filteredSql =
                "from asset_ledger l left join items i on i.id = l.item_id where l.user_id = ?";
        List<Object> params = new ArrayList<>();
        params.add(userId);
        if (!"ALL".equals(normalizedAssetType)) {
            filteredSql += " and l.asset_type = ?";
            params.add(normalizedAssetType);
        }
        if ("IN".equals(normalizedDirection)) {
            filteredSql += " and l.change_amount >= 0";
        } else if ("OUT".equals(normalizedDirection)) {
            filteredSql += " and l.change_amount < 0";
        }

        Long total = jdbcTemplate.queryForObject("select count(*) " + filteredSql, Long.class, params.toArray());

        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(safePageSize);
        queryParams.add(offset);
        List<PlayerLedgerEntryResponse> records = jdbcTemplate.query(
                "select l.id, l.asset_type, l.item_id, i.code as item_code, l.change_amount, l.balance_after, l.reason, l.ref_type, l.ref_id, l.created_at " +
                        filteredSql +
                        " order by l.created_at desc limit ? offset ?",
                (rs, rowNum) -> new PlayerLedgerEntryResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("asset_type"),
                        nullableUuid(rs.getString("item_id")),
                        rs.getString("item_code"),
                        rs.getLong("change_amount"),
                        nullableLong(rs, "balance_after"),
                        rs.getString("reason"),
                        rs.getString("ref_type"),
                        nullableUuid(rs.getString("ref_id")),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                queryParams.toArray()
        );

        long totalCount = total == null ? 0L : total;
        return new PlayerLedgerQueryResponse(records, totalCount, safePage, safePageSize, offset + records.size() < totalCount);
    }

    private void ensureActiveUser(UUID userId) {
        Integer exists = jdbcTemplate.queryForObject(
                "select count(*) from app_users where id = ? and status = 'ACTIVE'",
                Integer.class,
                userId
        );
        if (exists == null || exists == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.USER_NOT_FOUND, "用户不存在或状态不可用");
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

    private String normalizeAssetType(String assetType) {
        String normalized = assetType == null ? "ALL" : assetType.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized) || "COIN".equals(normalized) || "ITEM".equals(normalized) || "TOKEN".equals(normalized)) {
            return normalized;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_OPERATION, "不支持的资产类型");
    }

    private String normalizeLedgerDirection(String direction) {
        String normalized = direction == null ? "ALL" : direction.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized) || "IN".equals(normalized) || "OUT".equals(normalized)) {
            return normalized;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_OPERATION, "不支持的流水方向");
    }

    private UUID nullableUuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    private Long nullableLong(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }
}
