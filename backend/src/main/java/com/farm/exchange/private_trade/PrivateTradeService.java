package com.farm.exchange.private_trade;

import com.farm.exchange.common.ApiException;
import com.farm.exchange.user.TradePasswordService;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivateTradeService {

    private final JdbcTemplate jdbcTemplate;
    private final TradePasswordService tradePasswordService;

    public PrivateTradeService(JdbcTemplate jdbcTemplate, TradePasswordService tradePasswordService) {
        this.jdbcTemplate = jdbcTemplate;
        this.tradePasswordService = tradePasswordService;
    }

    @Transactional
    public PrivateTradeResponse create(UUID sellerUserId, CreatePrivateTradeRequest request) {
        if (sellerUserId.equals(request.getBuyerUserId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不能和自己进行私下交易");
        }
        tradePasswordService.verify(sellerUserId, request.getTradePassword());
        ensureActiveUser(request.getBuyerUserId());

        PrivateTradeItem item = tradeItem(request.getItemCode());
        freezeSellerInventory(sellerUserId, item.id, request.getQuantity());

        long taxAmount = privateTaxAmount(request.getPriceAmount());
        UUID offerId = UUID.randomUUID();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(24);
        jdbcTemplate.update(
                "insert into private_trade_offers (id, seller_user_id, buyer_user_id, item_id, quantity, price_amount, tax_amount, expires_at, status) values (?, ?, ?, ?, ?, ?, ?, ?, 'WAIT_ACCEPT')",
                offerId,
                sellerUserId,
                request.getBuyerUserId(),
                item.id,
                request.getQuantity(),
                request.getPriceAmount(),
                taxAmount,
                Timestamp.from(expiresAt.toInstant())
        );
        jdbcTemplate.update(
                "insert into asset_ledger (user_id, asset_type, item_id, change_amount, reason, ref_type, ref_id) values (?, 'ITEM', ?, ?, 'PRIVATE_TRADE_LOCK', 'PRIVATE_TRADE', ?)",
                sellerUserId,
                item.id,
                -request.getQuantity(),
                offerId
        );

        return new PrivateTradeResponse(offerId, sellerUserId, request.getBuyerUserId(), item.id, item.code, request.getQuantity(), request.getPriceAmount(), taxAmount, "WAIT_ACCEPT", expiresAt);
    }

    @Transactional
    public PrivateTradeSettlementResponse accept(UUID buyerUserId, UUID offerId, AcceptPrivateTradeRequest request) {
        tradePasswordService.verify(buyerUserId, request.getTradePassword());
        OfferSnapshot offer = lockOffer(offerId);
        if (!buyerUserId.equals(offer.buyerUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有买方可以接受该报价");
        }
        if (!"WAIT_ACCEPT".equals(offer.status)) {
            throw new ApiException(HttpStatus.CONFLICT, "报价单当前不可接受");
        }
        if (offer.expiresAt.isBefore(OffsetDateTime.now())) {
            expireOffer(offer);
            throw new ApiException(HttpStatus.CONFLICT, "报价单已过期");
        }

        WalletSnapshot buyerWallet = lockedWallet(offer.buyerUserId);
        WalletSnapshot sellerWallet = lockedWallet(offer.sellerUserId);
        long buyerCost = offer.priceAmount + offer.taxAmount;
        if (buyerWallet.balance < buyerCost) {
            throw new ApiException(HttpStatus.CONFLICT, "买方金币不足");
        }

        long buyerBalanceAfter = buyerWallet.balance - buyerCost;
        long sellerBalanceAfter = sellerWallet.balance + offer.priceAmount;
        updateWallet(offer.buyerUserId, buyerWallet.version, buyerBalanceAfter);
        updateWallet(offer.sellerUserId, sellerWallet.version, sellerBalanceAfter);

        consumeLockedSellerInventory(offer.sellerUserId, offer.itemId, offer.quantity);
        long buyerQuantityAfter = addInventory(offer.buyerUserId, offer.itemId, offer.quantity);

        jdbcTemplate.update(
                "update private_trade_offers set status = 'COMPLETED', accepted_at = now(), updated_at = now(), version = version + 1 where id = ?",
                offer.id
        );
        jdbcTemplate.update(
                "insert into tax_records (trade_type, ref_type, ref_id, payer_user_id, receiver_user_id, tax_rate_basis_points, trade_amount, tax_amount) values ('PRIVATE', 'PRIVATE_TRADE', ?, ?, ?, ?, ?, ?)",
                offer.id,
                offer.buyerUserId,
                offer.sellerUserId,
                privateTaxRate(),
                offer.priceAmount,
                offer.taxAmount
        );

        writeCoinLedger(offer.buyerUserId, -buyerCost, buyerBalanceAfter, "PRIVATE_TRADE_BUY", offer.id);
        writeCoinLedger(offer.sellerUserId, offer.priceAmount, sellerBalanceAfter, "PRIVATE_TRADE_SELL", offer.id);
        writeItemLedger(offer.buyerUserId, offer.itemId, offer.quantity, buyerQuantityAfter, "PRIVATE_TRADE_BUY", offer.id);
        writeItemLedger(offer.sellerUserId, offer.itemId, -offer.quantity, null, "PRIVATE_TRADE_SELL", offer.id);

        return new PrivateTradeSettlementResponse(offer.id, "COMPLETED", offer.priceAmount, offer.taxAmount, buyerBalanceAfter, sellerBalanceAfter, buyerQuantityAfter);
    }

    @Transactional
    public PrivateTradeResponse cancel(UUID sellerUserId, UUID offerId) {
        OfferSnapshot offer = lockOffer(offerId);
        if (!sellerUserId.equals(offer.sellerUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "只有卖方可以取消该报价");
        }
        if (!"WAIT_ACCEPT".equals(offer.status)) {
            throw new ApiException(HttpStatus.CONFLICT, "报价单当前不可取消");
        }
        releaseSellerInventory(offer.sellerUserId, offer.itemId, offer.quantity);
        jdbcTemplate.update(
                "update private_trade_offers set status = 'CANCELLED', cancelled_at = now(), updated_at = now(), version = version + 1 where id = ?",
                offer.id
        );
        jdbcTemplate.update(
                "insert into asset_ledger (user_id, asset_type, item_id, change_amount, reason, ref_type, ref_id) values (?, 'ITEM', ?, ?, 'PRIVATE_TRADE_RELEASE', 'PRIVATE_TRADE', ?)",
                offer.sellerUserId,
                offer.itemId,
                offer.quantity,
                offer.id
        );
        return new PrivateTradeResponse(offer.id, offer.sellerUserId, offer.buyerUserId, offer.itemId, offer.itemCode, offer.quantity, offer.priceAmount, offer.taxAmount, "CANCELLED", offer.expiresAt);
    }

    private void ensureActiveUser(UUID userId) {
        Integer exists = jdbcTemplate.queryForObject(
                "select count(*) from app_users where id = ? and status = 'ACTIVE'",
                Integer.class,
                userId
        );
        if (exists == null || exists == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在或状态不可用");
        }
    }

    private PrivateTradeItem tradeItem(String itemCode) {
        return jdbcTemplate.query(
                "select id, code, item_type, trade_enabled from items where code = ? and status = 'ACTIVE'",
                rs -> {
                    if (!rs.next()) {
                        throw new ApiException(HttpStatus.NOT_FOUND, "交易商品不存在或不可用");
                    }
                    PrivateTradeItem item = new PrivateTradeItem(UUID.fromString(rs.getString("id")), rs.getString("code"), rs.getString("item_type"), rs.getBoolean("trade_enabled"));
                    if (!item.tradeEnabled) {
                        throw new ApiException(HttpStatus.CONFLICT, "商品暂不可交易");
                    }
                    if (!"HARVEST".equals(item.itemType)) {
                        throw new ApiException(HttpStatus.CONFLICT, "私下交易 MVP 仅支持收获物交易");
                    }
                    return item;
                },
                itemCode
        );
    }

    private int privateTaxRate() {
        Integer rate = jdbcTemplate.queryForObject("select rate_basis_points from tax_config where trade_type = 'PRIVATE'", Integer.class);
        if (rate == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "私下交易税率配置缺失");
        }
        return rate;
    }

    private long privateTaxAmount(long priceAmount) {
        return priceAmount * privateTaxRate() / 10000L;
    }

    private void freezeSellerInventory(UUID userId, UUID itemId, long quantity) {
        int updated = jdbcTemplate.update(
                "update player_inventory set available_quantity = available_quantity - ?, locked_quantity = locked_quantity + ?, updated_at = now(), version = version + 1 where user_id = ? and item_id = ? and available_quantity >= ?",
                quantity,
                quantity,
                userId,
                itemId,
                quantity
        );
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "卖方库存不足");
        }
    }

    private void releaseSellerInventory(UUID userId, UUID itemId, long quantity) {
        int updated = jdbcTemplate.update(
                "update player_inventory set available_quantity = available_quantity + ?, locked_quantity = locked_quantity - ?, updated_at = now(), version = version + 1 where user_id = ? and item_id = ? and locked_quantity >= ?",
                quantity,
                quantity,
                userId,
                itemId,
                quantity
        );
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "释放冻结库存失败");
        }
    }

    private void consumeLockedSellerInventory(UUID userId, UUID itemId, long quantity) {
        int updated = jdbcTemplate.update(
                "update player_inventory set locked_quantity = locked_quantity - ?, updated_at = now(), version = version + 1 where user_id = ? and item_id = ? and locked_quantity >= ?",
                quantity,
                userId,
                itemId,
                quantity
        );
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "扣减冻结库存失败");
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
        Long available = jdbcTemplate.queryForObject("select available_quantity from player_inventory where user_id = ? and item_id = ?", Long.class, userId, itemId);
        return available == null ? 0L : available;
    }

    private OfferSnapshot lockOffer(UUID offerId) {
        return jdbcTemplate.query(
                "select p.id, p.seller_user_id, p.buyer_user_id, p.item_id, i.code as item_code, p.quantity, p.price_amount, p.tax_amount, p.expires_at, p.status " +
                        "from private_trade_offers p join items i on i.id = p.item_id where p.id = ? for update",
                rs -> {
                    if (!rs.next()) {
                        throw new ApiException(HttpStatus.NOT_FOUND, "报价单不存在");
                    }
                    return new OfferSnapshot(
                            UUID.fromString(rs.getString("id")),
                            UUID.fromString(rs.getString("seller_user_id")),
                            UUID.fromString(rs.getString("buyer_user_id")),
                            UUID.fromString(rs.getString("item_id")),
                            rs.getString("item_code"),
                            rs.getLong("quantity"),
                            rs.getLong("price_amount"),
                            rs.getLong("tax_amount"),
                            rs.getObject("expires_at", OffsetDateTime.class),
                            rs.getString("status")
                    );
                },
                offerId
        );
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

    private void expireOffer(OfferSnapshot offer) {
        releaseSellerInventory(offer.sellerUserId, offer.itemId, offer.quantity);
        jdbcTemplate.update("update private_trade_offers set status = 'EXPIRED', updated_at = now(), version = version + 1 where id = ?", offer.id);
    }

    private void writeCoinLedger(UUID userId, long amount, long balanceAfter, String reason, UUID refId) {
        jdbcTemplate.update(
                "insert into asset_ledger (user_id, asset_type, change_amount, balance_after, reason, ref_type, ref_id) values (?, 'COIN', ?, ?, ?, 'PRIVATE_TRADE', ?)",
                userId,
                amount,
                balanceAfter,
                reason,
                refId
        );
    }

    private void writeItemLedger(UUID userId, UUID itemId, long amount, Long balanceAfter, String reason, UUID refId) {
        jdbcTemplate.update(
                "insert into asset_ledger (user_id, asset_type, item_id, change_amount, balance_after, reason, ref_type, ref_id) values (?, 'ITEM', ?, ?, ?, ?, 'PRIVATE_TRADE', ?)",
                userId,
                itemId,
                amount,
                balanceAfter,
                reason,
                refId
        );
    }

    private static class PrivateTradeItem {
        private final UUID id;
        private final String code;
        private final String itemType;
        private final boolean tradeEnabled;

        private PrivateTradeItem(UUID id, String code, String itemType, boolean tradeEnabled) {
            this.id = id;
            this.code = code;
            this.itemType = itemType;
            this.tradeEnabled = tradeEnabled;
        }
    }

    private static class OfferSnapshot {
        private final UUID id;
        private final UUID sellerUserId;
        private final UUID buyerUserId;
        private final UUID itemId;
        private final String itemCode;
        private final long quantity;
        private final long priceAmount;
        private final long taxAmount;
        private final OffsetDateTime expiresAt;
        private final String status;

        private OfferSnapshot(UUID id, UUID sellerUserId, UUID buyerUserId, UUID itemId, String itemCode, long quantity, long priceAmount, long taxAmount, OffsetDateTime expiresAt, String status) {
            this.id = id;
            this.sellerUserId = sellerUserId;
            this.buyerUserId = buyerUserId;
            this.itemId = itemId;
            this.itemCode = itemCode;
            this.quantity = quantity;
            this.priceAmount = priceAmount;
            this.taxAmount = taxAmount;
            this.expiresAt = expiresAt;
            this.status = status;
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

