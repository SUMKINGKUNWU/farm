package com.farm.exchange.activity;

import com.farm.exchange.common.ApiException;
import com.farm.exchange.common.ErrorCode;
import java.time.OffsetDateTime;
import java.util.List;
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

    public List<PlayerTradeRecordResponse> trades(UUID userId) {
        ensureActiveUser(userId);
        return jdbcTemplate.query(
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
                        "order by created_at desc limit 100",
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
                userId,
                userId,
                userId,
                userId,
                userId,
                userId
        );
    }

    public List<PlayerLedgerEntryResponse> ledger(UUID userId) {
        ensureActiveUser(userId);
        return jdbcTemplate.query(
                "select l.id, l.asset_type, l.item_id, i.code as item_code, l.change_amount, l.balance_after, l.reason, l.ref_type, l.ref_id, l.created_at " +
                        "from asset_ledger l left join items i on i.id = l.item_id " +
                        "where l.user_id = ? order by l.created_at desc limit 100",
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
                userId
        );
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

    private UUID nullableUuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    private Long nullableLong(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }
}
