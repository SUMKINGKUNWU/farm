package com.farm.exchange.production;

import com.farm.exchange.common.ApiException;
import com.farm.exchange.common.ErrorCode;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductionService {

    private final JdbcTemplate jdbcTemplate;

    public ProductionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ItemResponse> items(String itemType) {
        if (itemType == null || itemType.trim().isEmpty()) {
            return jdbcTemplate.query(
                    "select id, code, name, item_type, base_price, current_price, grow_seconds, output_quantity from items where status = 'ACTIVE' order by item_type, code",
                    (rs, rowNum) -> new ItemResponse(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("item_type"),
                            rs.getLong("base_price"),
                            rs.getLong("current_price"),
                            nullableInteger(rs.getObject("grow_seconds")),
                            nullableLong(rs.getObject("output_quantity"))
                    )
            );
        }
        return jdbcTemplate.query(
                "select id, code, name, item_type, base_price, current_price, grow_seconds, output_quantity from items where status = 'ACTIVE' and item_type = ? order by code",
                (rs, rowNum) -> new ItemResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("item_type"),
                        rs.getLong("base_price"),
                        rs.getLong("current_price"),
                        nullableInteger(rs.getObject("grow_seconds")),
                        nullableLong(rs.getObject("output_quantity"))
                ),
                itemType
        );
    }

    public List<InventoryResponse> inventory(UUID userId) {
        ensureActiveUser(userId);
        return jdbcTemplate.query(
                "select i.id, i.code, i.name, i.item_type, pi.available_quantity, pi.locked_quantity " +
                        "from player_inventory pi join items i on i.id = pi.item_id " +
                        "where pi.user_id = ? order by i.item_type, i.code",
                (rs, rowNum) -> new InventoryResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("item_type"),
                        rs.getLong("available_quantity"),
                        rs.getLong("locked_quantity")
                ),
                userId
        );
    }

    public List<GrowthInstanceResponse> growthInstances(UUID userId) {
        ensureActiveUser(userId);
        return jdbcTemplate.query(
                "select g.id, g.slot_type, g.slot_id, input_item.code as input_code, output_item.code as output_code, " +
                        "g.output_quantity, g.started_at, g.ready_at, g.harvested_at, g.status " +
                        "from growth_instances g " +
                        "join items input_item on input_item.id = g.input_item_id " +
                        "join items output_item on output_item.id = g.output_item_id " +
                        "where g.user_id = ? " +
                        "order by case when g.status in ('GROWING', 'READY') then 0 else 1 end, g.ready_at desc",
                (rs, rowNum) -> new GrowthInstanceResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("slot_type"),
                        UUID.fromString(rs.getString("slot_id")),
                        rs.getString("input_code"),
                        rs.getString("output_code"),
                        rs.getLong("output_quantity"),
                        rs.getObject("started_at", OffsetDateTime.class),
                        rs.getObject("ready_at", OffsetDateTime.class),
                        rs.getObject("harvested_at", OffsetDateTime.class),
                        rs.getString("status")
                ),
                userId
        );
    }

    @Transactional
    public ProductionResponse start(UUID userId, String slotType, UUID slotId, String itemCode) {
        ensureActiveUser(userId);
        ItemPlan item = itemPlan(itemCode);
        validateInputType(slotType, item.itemType);
        lockAndValidateSlot(userId, slotType, slotId);
        consumeInputItem(userId, item);

        OffsetDateTime startedAt = OffsetDateTime.now();
        OffsetDateTime readyAt = startedAt.plusSeconds(item.growSeconds);
        UUID growthId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into growth_instances (id, user_id, slot_type, slot_id, input_item_id, output_item_id, output_quantity, started_at, ready_at, status) " +
                        "values (?, ?, ?, ?, ?, ?, ?, ?, ?, 'GROWING')",
                growthId,
                userId,
                slotType,
                slotId,
                item.id,
                item.outputItemId,
                item.outputQuantity,
                Timestamp.from(startedAt.toInstant()),
                Timestamp.from(readyAt.toInstant())
        );

        updateSlotStatus(slotType, slotId, "GROWING");
        jdbcTemplate.update(
                "insert into asset_ledger (user_id, asset_type, item_id, change_amount, reason, ref_type, ref_id) values (?, 'ITEM', ?, -1, ?, 'GROWTH', ?)",
                userId,
                item.id,
                slotType.equals("FARM") ? "PLANT_INPUT" : "RAISE_INPUT",
                growthId
        );

        return new ProductionResponse(growthId, slotType, slotId, item.code, item.outputItemCode, item.outputQuantity, startedAt, readyAt, "GROWING");
    }

    @Transactional
    public HarvestResponse harvest(UUID userId, UUID growthId) {
        ensureActiveUser(userId);
        GrowthSnapshot growth = lockGrowth(userId, growthId);
        if (!"GROWING".equals(growth.status) && !"READY".equals(growth.status)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.STATE_CONFLICT, "该生产批次不可收获");
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(growth.readyAt)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.STATE_CONFLICT, "尚未成熟，不能收获");
        }

        jdbcTemplate.update(
                "update growth_instances set status = 'HARVESTED', harvested_at = now(), updated_at = now(), version = version + 1 where id = ? and status in ('GROWING', 'READY')",
                growthId
        );
        updateSlotStatus(growth.slotType, growth.slotId, "EMPTY");
        long availableAfter = addInventory(userId, growth.outputItemId, growth.outputQuantity);
        jdbcTemplate.update(
                "insert into asset_ledger (user_id, asset_type, item_id, change_amount, balance_after, reason, ref_type, ref_id) values (?, 'ITEM', ?, ?, ?, 'HARVEST_OUTPUT', 'GROWTH', ?)",
                userId,
                growth.outputItemId,
                growth.outputQuantity,
                availableAfter,
                growthId
        );

        return new HarvestResponse(growthId, growth.outputItemCode, growth.outputQuantity, availableAfter);
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

    private ItemPlan itemPlan(String itemCode) {
        return jdbcTemplate.query(
                "select i.id, i.code, i.item_type, i.grow_seconds, i.output_item_id, i.output_quantity, o.code as output_code " +
                        "from items i join items o on o.id = i.output_item_id " +
                        "where i.code = ? and i.status = 'ACTIVE'",
                rs -> {
                    if (!rs.next()) {
                        throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.ITEM_NOT_FOUND, "生产商品不存在或不可用");
                    }
                    Integer growSeconds = nullableInteger(rs.getObject("grow_seconds"));
                    Long outputQuantity = nullableLong(rs.getObject("output_quantity"));
                    if (growSeconds == null || growSeconds <= 0 || outputQuantity == null || outputQuantity <= 0) {
                        throw new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFIG_MISSING, "商品生产配置不完整");
                    }
                    return new ItemPlan(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("code"),
                            rs.getString("item_type"),
                            growSeconds,
                            UUID.fromString(rs.getString("output_item_id")),
                            rs.getString("output_code"),
                            outputQuantity
                    );
                },
                itemCode
        );
    }

    private void validateInputType(String slotType, String itemType) {
        if ("FARM".equals(slotType) && !"SEED".equals(itemType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_OPERATION, "农场只能种植种子");
        }
        if ("RANCH".equals(slotType) && !"ANIMAL".equals(itemType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_OPERATION, "牧场只能养殖动物");
        }
    }

    private void lockAndValidateSlot(UUID userId, String slotType, UUID slotId) {
        String tableName = "FARM".equals(slotType) ? "farm_plots" : "ranch_slots";
        String status = jdbcTemplate.query(
                "select status from " + tableName + " where id = ? and user_id = ? for update",
                rs -> rs.next() ? rs.getString("status") : null,
                slotId,
                userId
        );
        if (status == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "栏位不存在");
        }
        if (!"EMPTY".equals(status)) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.STATE_CONFLICT, "栏位不是空闲状态");
        }
    }

    private void consumeInputItem(UUID userId, ItemPlan item) {
        Integer updated = jdbcTemplate.update(
                "update player_inventory set available_quantity = available_quantity - 1, updated_at = now(), version = version + 1 " +
                        "where user_id = ? and item_id = ? and available_quantity >= 1",
                userId,
                item.id
        );
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.INSUFFICIENT_INVENTORY, "库存不足，无法开始生产");
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
        Long available = jdbcTemplate.queryForObject(
                "select available_quantity from player_inventory where user_id = ? and item_id = ?",
                Long.class,
                userId,
                itemId
        );
        return available == null ? 0L : available;
    }

    private void updateSlotStatus(String slotType, UUID slotId, String status) {
        String tableName = "FARM".equals(slotType) ? "farm_plots" : "ranch_slots";
        jdbcTemplate.update(
                "update " + tableName + " set status = ?, updated_at = now(), version = version + 1 where id = ?",
                status,
                slotId
        );
    }

    private GrowthSnapshot lockGrowth(UUID userId, UUID growthId) {
        return jdbcTemplate.query(
                "select g.id, g.slot_type, g.slot_id, g.output_item_id, g.output_quantity, g.ready_at, g.status, i.code as output_code " +
                        "from growth_instances g join items i on i.id = g.output_item_id " +
                        "where g.id = ? and g.user_id = ? for update",
                rs -> {
                    if (!rs.next()) {
                        throw new ApiException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, "生产批次不存在");
                    }
                    return new GrowthSnapshot(
                            rs.getString("slot_type"),
                            UUID.fromString(rs.getString("slot_id")),
                            UUID.fromString(rs.getString("output_item_id")),
                            rs.getLong("output_quantity"),
                            rs.getTimestamp("ready_at").toInstant().atOffset(OffsetDateTime.now().getOffset()),
                            rs.getString("status"),
                            rs.getString("output_code")
                    );
                },
                growthId,
                userId
        );
    }

    private Integer nullableInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private Long nullableLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static class ItemPlan {
        private final UUID id;
        private final String code;
        private final String itemType;
        private final int growSeconds;
        private final UUID outputItemId;
        private final String outputItemCode;
        private final long outputQuantity;

        private ItemPlan(UUID id, String code, String itemType, int growSeconds, UUID outputItemId, String outputItemCode, long outputQuantity) {
            this.id = id;
            this.code = code;
            this.itemType = itemType;
            this.growSeconds = growSeconds;
            this.outputItemId = outputItemId;
            this.outputItemCode = outputItemCode;
            this.outputQuantity = outputQuantity;
        }
    }

    private static class GrowthSnapshot {
        private final String slotType;
        private final UUID slotId;
        private final UUID outputItemId;
        private final long outputQuantity;
        private final OffsetDateTime readyAt;
        private final String status;
        private final String outputItemCode;

        private GrowthSnapshot(String slotType, UUID slotId, UUID outputItemId, long outputQuantity, OffsetDateTime readyAt, String status, String outputItemCode) {
            this.slotType = slotType;
            this.slotId = slotId;
            this.outputItemId = outputItemId;
            this.outputQuantity = outputQuantity;
            this.readyAt = readyAt;
            this.status = status;
            this.outputItemCode = outputItemCode;
        }
    }
}
