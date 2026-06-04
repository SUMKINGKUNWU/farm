package com.farm.exchange.shop;

import com.farm.exchange.common.ApiException;
import com.farm.exchange.user.TradePasswordService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopService {

    private final JdbcTemplate jdbcTemplate;
    private final TradePasswordService tradePasswordService;

    public ShopService(JdbcTemplate jdbcTemplate, TradePasswordService tradePasswordService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tradePasswordService = tradePasswordService;
    }

    @Transactional
    public PurchaseResponse purchase(UUID userId, PurchaseRequest request) {
        tradePasswordService.verify(userId, request.getTradePassword());

        ShopItem item = shopItem(request.getItemCode());
        validatePurchasable(item);
        long totalAmount = Math.multiplyExact(item.currentPrice, request.getQuantity());
        WalletSnapshot wallet = lockedWallet(userId);
        if (wallet.balance < totalAmount) {
            throw new ApiException(HttpStatus.CONFLICT, "金币不足，无法购买");
        }

        long balanceAfter = wallet.balance - totalAmount;
        int walletUpdated = jdbcTemplate.update(
                "update wallets set balance = ?, updated_at = now(), version = version + 1 where user_id = ? and version = ?",
                balanceAfter,
                userId,
                wallet.version
        );
        if (walletUpdated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "钱包状态已变化，请重试");
        }

        long availableAfter = addInventory(userId, item.id, request.getQuantity());
        jdbcTemplate.update(
                "insert into asset_ledger (user_id, asset_type, change_amount, balance_after, reason, ref_type, ref_id) values (?, 'COIN', ?, ?, 'SHOP_PURCHASE', 'ITEM', ?)",
                userId,
                -totalAmount,
                balanceAfter,
                item.id
        );
        jdbcTemplate.update(
                "insert into asset_ledger (user_id, asset_type, item_id, change_amount, balance_after, reason, ref_type, ref_id) values (?, 'ITEM', ?, ?, ?, 'SHOP_PURCHASE', 'ITEM', ?)",
                userId,
                item.id,
                request.getQuantity(),
                availableAfter,
                item.id
        );

        return new PurchaseResponse(item.id, item.code, request.getQuantity(), item.currentPrice, totalAmount, balanceAfter, availableAfter);
    }

    private ShopItem shopItem(String itemCode) {
        return jdbcTemplate.query(
                "select id, code, item_type, current_price, trade_enabled from items where code = ? and status = 'ACTIVE'",
                rs -> {
                    if (!rs.next()) {
                        throw new ApiException(HttpStatus.NOT_FOUND, "商品不存在或不可用");
                    }
                    return new ShopItem(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("code"),
                            rs.getString("item_type"),
                            rs.getLong("current_price"),
                            rs.getBoolean("trade_enabled")
                    );
                },
                itemCode
        );
    }

    private void validatePurchasable(ShopItem item) {
        if (!item.tradeEnabled) {
            throw new ApiException(HttpStatus.CONFLICT, "商品暂不可购买");
        }
        if (!"SEED".equals(item.itemType) && !"ANIMAL".equals(item.itemType)) {
            throw new ApiException(HttpStatus.CONFLICT, "该商品不能在商店购买");
        }
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

    private long addInventory(UUID userId, UUID itemId, long quantity) {
        jdbcTemplate.update(
                "insert into player_inventory (user_id, item_id, available_quantity, locked_quantity) values (?, ?, ?, 0) " +
                        "on conflict (user_id, item_id) do update set available_quantity = player_inventory.available_quantity + excluded.available_quantity, updated_at = now(), version = player_inventory.version + 1",
                userId,
                itemId,
                quantity
        );
        Long available = jdbcTemplate.queryForObject(
                "select available_quantity from player_inventory where user_id = ? and item_id = ?",
                Long.class,
                userId,
                itemId
        );
        return available == null ? 0L : available;
    }

    private static class ShopItem {
        private final UUID id;
        private final String code;
        private final String itemType;
        private final long currentPrice;
        private final boolean tradeEnabled;

        private ShopItem(UUID id, String code, String itemType, long currentPrice, boolean tradeEnabled) {
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

