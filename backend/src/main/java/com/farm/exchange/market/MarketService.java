package com.farm.exchange.market;

import com.farm.exchange.common.ApiException;
import com.farm.exchange.user.TradePasswordService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketService {

    private final JdbcTemplate jdbcTemplate;
    private final TradePasswordService tradePasswordService;

    public MarketService(JdbcTemplate jdbcTemplate, TradePasswordService tradePasswordService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tradePasswordService = tradePasswordService;
    }

    @Transactional
    public MarketTradeResponse trade(UUID userId, String side, MarketTradeRequest request) {
        tradePasswordService.verify(userId, request.getTradePassword());
        MarketItem item = marketItem(request.getItemCode());
        long grossAmount = Math.multiplyExact(item.currentPrice, request.getQuantity());
        int taxRate = marketTaxRate();
        long taxAmount = grossAmount * taxRate / 10000L;

        WalletSnapshot wallet = lockedWallet(userId);
        long balanceAfter;
        long availableQuantityAfter;
        long netAmount;
        if ("BUY".equals(side)) {
            long totalCost = grossAmount + taxAmount;
            if (wallet.balance < totalCost) {
                throw new ApiException(HttpStatus.CONFLICT, "金币不足，无法买入");
            }
            balanceAfter = wallet.balance - totalCost;
            netAmount = totalCost;
            updateWallet(userId, wallet.version, balanceAfter);
            availableQuantityAfter = addInventory(userId, item.id, request.getQuantity());
            writeCoinLedger(userId, -totalCost, balanceAfter, "MARKET_BUY", item.id);
            writeItemLedger(userId, item.id, request.getQuantity(), availableQuantityAfter, "MARKET_BUY", item.id);
        } else {
            availableQuantityAfter = consumeInventory(userId, item.id, request.getQuantity());
            long income = grossAmount - taxAmount;
            balanceAfter = wallet.balance + income;
            netAmount = income;
            updateWallet(userId, wallet.version, balanceAfter);
            writeCoinLedger(userId, income, balanceAfter, "MARKET_SELL", item.id);
            writeItemLedger(userId, item.id, -request.getQuantity(), availableQuantityAfter, "MARKET_SELL", item.id);
        }

        UUID tradeId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into market_trades (id, user_id, item_id, side, quantity, unit_price, gross_amount, tax_amount, net_amount) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                tradeId,
                userId,
                item.id,
                side,
                request.getQuantity(),
                item.currentPrice,
                grossAmount,
                taxAmount,
                netAmount
        );
        jdbcTemplate.update(
                "insert into tax_records (trade_type, ref_type, ref_id, payer_user_id, tax_rate_basis_points, trade_amount, tax_amount) values ('MARKET', 'MARKET_TRADE', ?, ?, ?, ?, ?)",
                tradeId,
                userId,
                taxRate,
                grossAmount,
                taxAmount
        );

        return new MarketTradeResponse(tradeId, side, item.id, item.code, request.getQuantity(), item.currentPrice, grossAmount, taxAmount, netAmount, balanceAfter, availableQuantityAfter);
    }

    private MarketItem marketItem(String itemCode) {
        return jdbcTemplate.query(
                "select id, code, item_type, current_price, trade_enabled from items where code = ? and status = 'ACTIVE'",
                rs -> {
                    if (!rs.next()) {
                        throw new ApiException(HttpStatus.NOT_FOUND, "交易商品不存在或不可用");
                    }
                    MarketItem item = new MarketItem(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("code"),
                            rs.getString("item_type"),
                            rs.getLong("current_price"),
                            rs.getBoolean("trade_enabled")
                    );
                    if (!item.tradeEnabled) {
                        throw new ApiException(HttpStatus.CONFLICT, "商品暂不可交易");
                    }
                    if (!"HARVEST".equals(item.itemType)) {
                        throw new ApiException(HttpStatus.CONFLICT, "交易站 MVP 仅支持收获物交易");
                    }
                    return item;
                },
                itemCode
        );
    }

    private int marketTaxRate() {
        Integer rate = jdbcTemplate.queryForObject(
                "select rate_basis_points from tax_config where trade_type = 'MARKET'",
                Integer.class
        );
        if (rate == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "交易站税率配置缺失");
        }
        return rate;
    }

    private WalletSnapshot lockedWallet(UUID userId) {
        return jdbcTemplate.query(
                "select balance, version from wallets where user_id = ? for update",
                rs -> {
                    if (!rs.next()) {
                        throw new ApiException(HttpStatus.NOT_FOUND, "钱包不存在");
                    }
                    return new WalletSnapshot(rs.getLong("balance"), rs.getInt("version"));
                },
                userId
        );
    }

    private void updateWallet(UUID userId, int version, long balanceAfter) {
        int updated = jdbcTemplate.update(
                "update wallets set balance = ?, updated_at = now(), version = version + 1 where user_id = ? and version = ?",
                balanceAfter,
                userId,
                version
        );
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "钱包状态已变化，请重试");
        }
    }

    private long addInventory(UUID userId, UUID itemId, long quantity) {
        jdbcTemplate.update(
                "insert into player_inventory (user_id, item_id, available_quantity, locked_quantity) values (?, ?, ?, 0) " +
                        "on conflict (user_id, item_id) do update set available_quantity = player_inventory.available_quantity + excluded.available_quantity, updated_at = now(), version = player_inventory.version + 1",
                userId,
                itemId,
                quantity
        );
        return inventoryQuantity(userId, itemId);
    }

    private long consumeInventory(UUID userId, UUID itemId, long quantity) {
        int updated = jdbcTemplate.update(
                "update player_inventory set available_quantity = available_quantity - ?, updated_at = now(), version = version + 1 where user_id = ? and item_id = ? and available_quantity >= ?",
                quantity,
                userId,
                itemId,
                quantity
        );
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "库存不足，无法卖出");
        }
        return inventoryQuantity(userId, itemId);
    }

    private long inventoryQuantity(UUID userId, UUID itemId) {
        Long available = jdbcTemplate.queryForObject(
                "select available_quantity from player_inventory where user_id = ? and item_id = ?",
                Long.class,
                userId,
                itemId
        );
        return available == null ? 0L : available;
    }

    private void writeCoinLedger(UUID userId, long amount, long balanceAfter, String reason, UUID refId) {
        jdbcTemplate.update(
                "insert into asset_ledger (user_id, asset_type, change_amount, balance_after, reason, ref_type, ref_id) values (?, 'COIN', ?, ?, ?, 'MARKET_TRADE', ?)",
                userId,
                amount,
                balanceAfter,
                reason,
                refId
        );
    }

    private void writeItemLedger(UUID userId, UUID itemId, long amount, long balanceAfter, String reason, UUID refId) {
        jdbcTemplate.update(
                "insert into asset_ledger (user_id, asset_type, item_id, change_amount, balance_after, reason, ref_type, ref_id) values (?, 'ITEM', ?, ?, ?, ?, 'MARKET_TRADE', ?)",
                userId,
                itemId,
                amount,
                balanceAfter,
                reason,
                refId
        );
    }

    private static class MarketItem {
        private final UUID id;
        private final String code;
        private final String itemType;
        private final long currentPrice;
        private final boolean tradeEnabled;

        private MarketItem(UUID id, String code, String itemType, long currentPrice, boolean tradeEnabled) {
            this.id = id;
            this.code = code;
            this.itemType = itemType;
            this.currentPrice = currentPrice;
            this.tradeEnabled = tradeEnabled;
        }
    }

    private static class WalletSnapshot {
        private final long balance;
        private final int version;

        private WalletSnapshot(long balance, int version) {
            this.balance = balance;
            this.version = version;
        }
    }
}

