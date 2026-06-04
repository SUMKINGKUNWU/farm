package com.farm.exchange;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class FarmExchangeApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void registerInitializesWalletSlotsAndTradePassword() throws Exception {
        String userId = registerTestUser();

        Integer walletCount = jdbcTemplate.queryForObject(
                "select count(*) from wallets where user_id = ?::uuid and balance = 10000",
                Integer.class,
                userId
        );
        Integer farmPlotCount = jdbcTemplate.queryForObject(
                "select count(*) from farm_plots where user_id = ?::uuid",
                Integer.class,
                userId
        );
        Integer ranchSlotCount = jdbcTemplate.queryForObject(
                "select count(*) from ranch_slots where user_id = ?::uuid",
                Integer.class,
                userId
        );
        Integer ledgerCount = jdbcTemplate.queryForObject(
                "select count(*) from asset_ledger where user_id = ?::uuid and reason = 'INITIAL_GRANT'",
                Integer.class,
                userId
        );

        Assertions.assertEquals(1, walletCount);
        Assertions.assertEquals(4, farmPlotCount);
        Assertions.assertEquals(2, ranchSlotCount);
        Assertions.assertEquals(1, ledgerCount);

        mockMvc.perform(post("/api/users/" + userId + "/trade-password")
                        .contentType("application/json")
                        .content("{\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.tradePasswordSet").value(true));

        String tradePasswordHash = jdbcTemplate.queryForObject(
                "select trade_password_hash from app_users where id = ?::uuid",
                String.class,
                userId
        );
        Assertions.assertNotNull(tradePasswordHash);
        Assertions.assertNotEquals("654321", tradePasswordHash);
    }

    @Test
    void summarySlotsAndExpansionWork() throws Exception {
        String userId = registerTestUser();

        mockMvc.perform(get("/api/users/" + userId + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10000))
                .andExpect(jsonPath("$.farmSlots").value(4))
                .andExpect(jsonPath("$.ranchSlots").value(2))
                .andExpect(jsonPath("$.nextFarmExpandCost").value(1000))
                .andExpect(jsonPath("$.nextRanchExpandCost").value(2000));

        mockMvc.perform(get("/api/users/" + userId + "/farm/plots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotType").value("FARM"))
                .andExpect(jsonPath("$.currentSlots").value(4))
                .andExpect(jsonPath("$.maxSlots").value(16))
                .andExpect(jsonPath("$.nextExpandCost").value(1000))
                .andExpect(jsonPath("$.slots.length()").value(4));

        mockMvc.perform(get("/api/users/" + userId + "/ranch/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotType").value("RANCH"))
                .andExpect(jsonPath("$.currentSlots").value(2))
                .andExpect(jsonPath("$.maxSlots").value(16))
                .andExpect(jsonPath("$.nextExpandCost").value(2000))
                .andExpect(jsonPath("$.slots.length()").value(2));

        mockMvc.perform(post("/api/users/" + userId + "/farm/expand"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotType").value("FARM"))
                .andExpect(jsonPath("$.currentSlots").value(5))
                .andExpect(jsonPath("$.cost").value(1000))
                .andExpect(jsonPath("$.balanceAfter").value(9000))
                .andExpect(jsonPath("$.nextExpandCost").value(2000));

        mockMvc.perform(post("/api/users/" + userId + "/ranch/expand"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotType").value("RANCH"))
                .andExpect(jsonPath("$.currentSlots").value(3))
                .andExpect(jsonPath("$.cost").value(2000))
                .andExpect(jsonPath("$.balanceAfter").value(7000))
                .andExpect(jsonPath("$.nextExpandCost").value(4000));

        Integer farmPlotCount = jdbcTemplate.queryForObject(
                "select count(*) from farm_plots where user_id = ?::uuid",
                Integer.class,
                userId
        );
        Integer ranchSlotCount = jdbcTemplate.queryForObject(
                "select count(*) from ranch_slots where user_id = ?::uuid",
                Integer.class,
                userId
        );
        Long balance = jdbcTemplate.queryForObject(
                "select balance from wallets where user_id = ?::uuid",
                Long.class,
                userId
        );
        Integer expansionLedgerCount = jdbcTemplate.queryForObject(
                "select count(*) from asset_ledger where user_id = ?::uuid and reason in ('EXPAND_FARM', 'EXPAND_RANCH')",
                Integer.class,
                userId
        );

        Assertions.assertEquals(5, farmPlotCount);
        Assertions.assertEquals(3, ranchSlotCount);
        Assertions.assertEquals(7000L, balance);
        Assertions.assertEquals(2, expansionLedgerCount);
    }

    @Test
    void plantingRaisingAndHarvestingWork() throws Exception {
        String userId = registerTestUser();
        grantInventory(userId, "WHEAT_SEED", 2);
        grantInventory(userId, "CHICKEN", 1);

        String farmPlotId = jdbcTemplate.queryForObject(
                "select id::text from farm_plots where user_id = ?::uuid order by slot_index limit 1",
                String.class,
                userId
        );
        String ranchSlotId = jdbcTemplate.queryForObject(
                "select id::text from ranch_slots where user_id = ?::uuid order by slot_index limit 1",
                String.class,
                userId
        );

        MvcResult plantResult = mockMvc.perform(post("/api/users/" + userId + "/farm/plots/" + farmPlotId + "/plant")
                        .param("itemCode", "WHEAT_SEED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotType").value("FARM"))
                .andExpect(jsonPath("$.inputItemCode").value("WHEAT_SEED"))
                .andExpect(jsonPath("$.outputItemCode").value("WHEAT"))
                .andExpect(jsonPath("$.outputQuantity").value(12))
                .andExpect(jsonPath("$.status").value("GROWING"))
                .andReturn();

        MvcResult raiseResult = mockMvc.perform(post("/api/users/" + userId + "/ranch/slots/" + ranchSlotId + "/raise")
                        .param("itemCode", "CHICKEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotType").value("RANCH"))
                .andExpect(jsonPath("$.inputItemCode").value("CHICKEN"))
                .andExpect(jsonPath("$.outputItemCode").value("EGG"))
                .andExpect(jsonPath("$.outputQuantity").value(20))
                .andReturn();

        String growthId = extractJsonString(plantResult.getResponse().getContentAsString(), "growthId");
        String ranchGrowthId = extractJsonString(raiseResult.getResponse().getContentAsString(), "growthId");

        Long wheatSeedAfterPlant = jdbcTemplate.queryForObject(
                "select pi.available_quantity from player_inventory pi join items i on i.id = pi.item_id where pi.user_id = ?::uuid and i.code = 'WHEAT_SEED'",
                Long.class,
                userId
        );
        Assertions.assertEquals(1L, wheatSeedAfterPlant);

        mockMvc.perform(post("/api/users/" + userId + "/growth/" + growthId + "/harvest"))
                .andExpect(status().isConflict());

        jdbcTemplate.update("update growth_instances set started_at = now() - interval '2 seconds', ready_at = now() - interval '1 second' where id = ?::uuid", growthId);
        jdbcTemplate.update("update growth_instances set started_at = now() - interval '2 seconds', ready_at = now() - interval '1 second' where id = ?::uuid", ranchGrowthId);

        mockMvc.perform(post("/api/users/" + userId + "/growth/" + growthId + "/harvest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outputItemCode").value("WHEAT"))
                .andExpect(jsonPath("$.outputQuantity").value(12))
                .andExpect(jsonPath("$.availableQuantityAfter").value(12));

        mockMvc.perform(post("/api/users/" + userId + "/growth/" + ranchGrowthId + "/harvest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outputItemCode").value("EGG"))
                .andExpect(jsonPath("$.outputQuantity").value(20))
                .andExpect(jsonPath("$.availableQuantityAfter").value(20));

        mockMvc.perform(post("/api/users/" + userId + "/growth/" + growthId + "/harvest"))
                .andExpect(status().isConflict());

        String farmStatus = jdbcTemplate.queryForObject(
                "select status from farm_plots where id = ?::uuid",
                String.class,
                farmPlotId
        );
        String ranchStatus = jdbcTemplate.queryForObject(
                "select status from ranch_slots where id = ?::uuid",
                String.class,
                ranchSlotId
        );
        Integer harvestLedgerCount = jdbcTemplate.queryForObject(
                "select count(*) from asset_ledger where user_id = ?::uuid and reason = 'HARVEST_OUTPUT'",
                Integer.class,
                userId
        );

        Assertions.assertEquals("EMPTY", farmStatus);
        Assertions.assertEquals("EMPTY", ranchStatus);
        Assertions.assertEquals(2, harvestLedgerCount);
    }

    @Test
    void shopPurchaseRequiresTradePasswordAndUpdatesWalletInventory() throws Exception {
        String userId = registerTestUser();

        mockMvc.perform(post("/api/users/" + userId + "/shop/purchase")
                        .contentType("application/json")
                        .content("{\"itemCode\":\"WHEAT_SEED\",\"quantity\":3,\"tradePassword\":\"654321\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("请先设置交易密码"));

        mockMvc.perform(post("/api/users/" + userId + "/trade-password")
                        .contentType("application/json")
                        .content("{\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/" + userId + "/shop/purchase")
                        .contentType("application/json")
                        .content("{\"itemCode\":\"WHEAT_SEED\",\"quantity\":3,\"tradePassword\":\"000000\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("交易密码错误"));

        mockMvc.perform(post("/api/users/" + userId + "/shop/purchase")
                        .contentType("application/json")
                        .content("{\"itemCode\":\"WHEAT\",\"quantity\":1,\"tradePassword\":\"654321\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("该商品不能在商店购买"));

        mockMvc.perform(post("/api/users/" + userId + "/shop/purchase")
                        .contentType("application/json")
                        .content("{\"itemCode\":\"WHEAT_SEED\",\"quantity\":3,\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCode").value("WHEAT_SEED"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.unitPrice").value(80))
                .andExpect(jsonPath("$.totalAmount").value(240))
                .andExpect(jsonPath("$.balanceAfter").value(9760))
                .andExpect(jsonPath("$.availableQuantityAfter").value(3));

        Long balance = jdbcTemplate.queryForObject(
                "select balance from wallets where user_id = ?::uuid",
                Long.class,
                userId
        );
        Long seedQuantity = jdbcTemplate.queryForObject(
                "select pi.available_quantity from player_inventory pi join items i on i.id = pi.item_id where pi.user_id = ?::uuid and i.code = 'WHEAT_SEED'",
                Long.class,
                userId
        );
        Integer ledgerCount = jdbcTemplate.queryForObject(
                "select count(*) from asset_ledger where user_id = ?::uuid and reason = 'SHOP_PURCHASE'",
                Integer.class,
                userId
        );

        Assertions.assertEquals(9760L, balance);
        Assertions.assertEquals(3L, seedQuantity);
        Assertions.assertEquals(2, ledgerCount);
    }

    @Test
    void marketBuyAndSellApplyTaxAndLedgers() throws Exception {
        String userId = registerTestUser();
        mockMvc.perform(post("/api/users/" + userId + "/trade-password")
                        .contentType("application/json")
                        .content("{\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/" + userId + "/market/buy")
                        .contentType("application/json")
                        .content("{\"itemCode\":\"WHEAT\",\"quantity\":100,\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.itemCode").value("WHEAT"))
                .andExpect(jsonPath("$.quantity").value(100))
                .andExpect(jsonPath("$.unitPrice").value(25))
                .andExpect(jsonPath("$.grossAmount").value(2500))
                .andExpect(jsonPath("$.taxAmount").value(75))
                .andExpect(jsonPath("$.netAmount").value(2575))
                .andExpect(jsonPath("$.balanceAfter").value(7425))
                .andExpect(jsonPath("$.availableQuantityAfter").value(100));

        mockMvc.perform(post("/api/users/" + userId + "/market/sell")
                        .contentType("application/json")
                        .content("{\"itemCode\":\"WHEAT\",\"quantity\":40,\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.side").value("SELL"))
                .andExpect(jsonPath("$.grossAmount").value(1000))
                .andExpect(jsonPath("$.taxAmount").value(30))
                .andExpect(jsonPath("$.netAmount").value(970))
                .andExpect(jsonPath("$.balanceAfter").value(8395))
                .andExpect(jsonPath("$.availableQuantityAfter").value(60));

        Long balance = jdbcTemplate.queryForObject(
                "select balance from wallets where user_id = ?::uuid",
                Long.class,
                userId
        );
        Long wheatQuantity = jdbcTemplate.queryForObject(
                "select pi.available_quantity from player_inventory pi join items i on i.id = pi.item_id where pi.user_id = ?::uuid and i.code = 'WHEAT'",
                Long.class,
                userId
        );
        Integer tradeCount = jdbcTemplate.queryForObject(
                "select count(*) from market_trades where user_id = ?::uuid",
                Integer.class,
                userId
        );
        Integer taxCount = jdbcTemplate.queryForObject(
                "select count(*) from tax_records where payer_user_id = ?::uuid and trade_type = 'MARKET'",
                Integer.class,
                userId
        );
        Integer ledgerCount = jdbcTemplate.queryForObject(
                "select count(*) from asset_ledger where user_id = ?::uuid and reason in ('MARKET_BUY', 'MARKET_SELL')",
                Integer.class,
                userId
        );

        Assertions.assertEquals(8395L, balance);
        Assertions.assertEquals(60L, wheatQuantity);
        Assertions.assertEquals(2, tradeCount);
        Assertions.assertEquals(2, taxCount);
        Assertions.assertEquals(4, ledgerCount);
    }

    private String registerTestUser() throws Exception {
        String username = "tester_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"nickname\":\"测试农夫\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.balance").value(10000))
                .andExpect(jsonPath("$.farmSlots").value(4))
                .andExpect(jsonPath("$.ranchSlots").value(2))
                .andExpect(jsonPath("$.tradePasswordSet").value(false))
                .andReturn();

        String body = registerResult.getResponse().getContentAsString();
        return body.replaceAll(".*\"userId\":\"([^\"]+)\".*", "$1");
    }

    private void grantInventory(String userId, String itemCode, long quantity) {
        jdbcTemplate.update(
                "insert into player_inventory (user_id, item_id, available_quantity, locked_quantity) " +
                        "select ?::uuid, id, ?, 0 from items where code = ? " +
                        "on conflict (user_id, item_id) do update set available_quantity = player_inventory.available_quantity + excluded.available_quantity",
                userId,
                quantity,
                itemCode
        );
    }

    private String extractJsonString(String json, String fieldName) {
        return json.replaceAll(".*\"" + fieldName + "\":\"([^\"]+)\".*", "$1");
    }
}
