package com.farm.exchange.farm;

import com.farm.exchange.common.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmService {

    private final JdbcTemplate jdbcTemplate;

    public FarmService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PlayerSummaryResponse summary(UUID userId) {
        ensureActiveUser(userId);
        WalletSnapshot wallet = walletSnapshot(userId);
        int farmSlots = countSlots(userId, SlotType.FARM);
        int ranchSlots = countSlots(userId, SlotType.RANCH);
        boolean tradePasswordSet = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select trade_password_hash is not null from app_users where id = ?",
                Boolean.class,
                userId
        ));

        return new PlayerSummaryResponse(
                userId,
                wallet.balance,
                wallet.lockedBalance,
                farmSlots,
                ranchSlots,
                nextExpandCost(SlotType.FARM, farmSlots),
                nextExpandCost(SlotType.RANCH, ranchSlots),
                tradePasswordSet
        );
    }

    public SlotListResponse farmPlots(UUID userId) {
        ensureActiveUser(userId);
        return slots(userId, SlotType.FARM);
    }

    public SlotListResponse ranchSlots(UUID userId) {
        ensureActiveUser(userId);
        return slots(userId, SlotType.RANCH);
    }

    @Transactional
    public ExpansionResponse expand(UUID userId, SlotType slotType) {
        ensureActiveUser(userId);
        ExpansionConfig config = expansionConfig(slotType);
        int currentSlots = countSlots(userId, slotType);
        if (currentSlots >= config.getMaxSlots()) {
            throw new ApiException(HttpStatus.CONFLICT, "栏位已达到最大数量");
        }

        long cost = calculateExpandCost(config, currentSlots);
        WalletSnapshot wallet = lockedWallet(userId);
        if (wallet.balance < cost) {
            throw new ApiException(HttpStatus.CONFLICT, "金币不足，无法扩建");
        }

        long balanceAfter = wallet.balance - cost;
        int walletUpdated = jdbcTemplate.update(
                "update wallets set balance = ?, updated_at = now(), version = version + 1 where user_id = ? and version = ?",
                balanceAfter,
                userId,
                wallet.version
        );
        if (walletUpdated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "钱包状态已变化，请重试");
        }

        int nextSlotIndex = currentSlots + 1;
        if (slotType == SlotType.FARM) {
            jdbcTemplate.update(
                    "insert into farm_plots (user_id, slot_index, status) values (?, ?, 'EMPTY')",
                    userId,
                    nextSlotIndex
            );
        } else {
            jdbcTemplate.update(
                    "insert into ranch_slots (user_id, slot_index, status) values (?, ?, 'EMPTY')",
                    userId,
                    nextSlotIndex
            );
        }

        jdbcTemplate.update(
                "insert into asset_ledger (user_id, asset_type, change_amount, balance_after, reason, ref_type, ref_id) values (?, 'COIN', ?, ?, ?, ?, ?)",
                userId,
                -cost,
                balanceAfter,
                slotType == SlotType.FARM ? "EXPAND_FARM" : "EXPAND_RANCH",
                slotType.name(),
                userId
        );

        int newCurrentSlots = currentSlots + 1;
        return new ExpansionResponse(
                slotType.name(),
                newCurrentSlots,
                config.getMaxSlots(),
                cost,
                balanceAfter,
                newCurrentSlots >= config.getMaxSlots() ? 0L : calculateExpandCost(config, newCurrentSlots)
        );
    }

    private SlotListResponse slots(UUID userId, SlotType slotType) {
        ExpansionConfig config = expansionConfig(slotType);
        String tableName = slotType == SlotType.FARM ? "farm_plots" : "ranch_slots";
        List<SlotResponse> slots = jdbcTemplate.query(
                "select id, slot_index, status, level from " + tableName + " where user_id = ? order by slot_index",
                (rs, rowNum) -> new SlotResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getInt("slot_index"),
                        rs.getString("status"),
                        rs.getInt("level")
                ),
                userId
        );
        int currentSlots = slots.size();
        long nextExpandCost = currentSlots >= config.getMaxSlots() ? 0L : calculateExpandCost(config, currentSlots);
        return new SlotListResponse(slotType.name(), currentSlots, config.getMaxSlots(), nextExpandCost, slots);
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

    private WalletSnapshot walletSnapshot(UUID userId) {
        return jdbcTemplate.queryForObject(
                "select balance, locked_balance, version from wallets where user_id = ?",
                (rs, rowNum) -> new WalletSnapshot(
                        rs.getLong("balance"),
                        rs.getLong("locked_balance"),
                        rs.getInt("version")
                ),
                userId
        );
    }

    private WalletSnapshot lockedWallet(UUID userId) {
        return jdbcTemplate.queryForObject(
                "select balance, locked_balance, version from wallets where user_id = ? for update",
                (rs, rowNum) -> new WalletSnapshot(
                        rs.getLong("balance"),
                        rs.getLong("locked_balance"),
                        rs.getInt("version")
                ),
                userId
        );
    }

    private int countSlots(UUID userId, SlotType slotType) {
        String tableName = slotType == SlotType.FARM ? "farm_plots" : "ranch_slots";
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from " + tableName + " where user_id = ?",
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    private ExpansionConfig expansionConfig(SlotType slotType) {
        return jdbcTemplate.queryForObject(
                "select initial_slots, max_slots, first_expand_cost, cost_multiplier from expansion_config where slot_type = ?",
                (rs, rowNum) -> new ExpansionConfig(
                        rs.getInt("initial_slots"),
                        rs.getInt("max_slots"),
                        rs.getLong("first_expand_cost"),
                        rs.getInt("cost_multiplier")
                ),
                slotType.name()
        );
    }

    private long nextExpandCost(SlotType slotType, int currentSlots) {
        ExpansionConfig config = expansionConfig(slotType);
        return currentSlots >= config.getMaxSlots() ? 0L : calculateExpandCost(config, currentSlots);
    }

    private long calculateExpandCost(ExpansionConfig config, int currentSlots) {
        int expansionIndex = currentSlots - config.getInitialSlots();
        if (expansionIndex < 0) {
            expansionIndex = 0;
        }
        long cost = config.getFirstExpandCost();
        for (int i = 0; i < expansionIndex; i++) {
            cost = Math.multiplyExact(cost, config.getCostMultiplier());
        }
        return cost;
    }

    private static class WalletSnapshot {
        private final long balance;
        private final long lockedBalance;
        private final int version;

        private WalletSnapshot(long balance, long lockedBalance, int version) {
            this.balance = balance;
            this.lockedBalance = lockedBalance;
            this.version = version;
        }
    }
}

