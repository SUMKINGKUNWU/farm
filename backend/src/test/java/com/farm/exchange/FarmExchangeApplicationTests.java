package com.farm.exchange;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    private String lastRegisteredUsername;

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
    void loginReturnsTokenAndCurrentUser() throws Exception {
        String userId = registerTestUser();
        String username = lastRegisteredUsername;

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"bad-password\"}"))
                .andExpect(status().isUnauthorized());

        String token = login(username);
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("PLAYER"));
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
    void meEndpointsUseBearerTokenForPlayerOperations() throws Exception {
        String userId = registerTestUser();
        String username = lastRegisteredUsername;
        String token = login(username);

        mockMvc.perform(get("/api/me/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"))
                .andExpect(jsonPath("$.path").value("/api/me/summary"));

        mockMvc.perform(get("/api/me/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.balance").value(10000))
                .andExpect(jsonPath("$.farmSlots").value(4));

        mockMvc.perform(post("/api/me/trade-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.tradePasswordSet").value(true));

        mockMvc.perform(post("/api/me/shop/purchase")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"itemCode\":\"WHEAT_SEED\",\"quantity\":2,\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCode").value("WHEAT_SEED"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.balanceAfter").value(9840));

        mockMvc.perform(get("/api/me/inventory")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("WHEAT_SEED"))
                .andExpect(jsonPath("$[0].availableQuantity").value(2));

        mockMvc.perform(get("/api/me/market/items/WHEAT/quote")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCode").value("WHEAT"));
    }

    @Test
    void validationErrorsReturnFieldDetails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"x\",\"nickname\":\"\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/auth/register"))
                .andExpect(jsonPath("$.fieldErrors.length()").isNumber());
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
        resetMarketTaxRate();
        jdbcTemplate.update("update items set current_price = base_price where code = 'WHEAT'");
        mockMvc.perform(post("/api/users/" + userId + "/trade-password")
                        .contentType("application/json")
                        .content("{\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/" + userId + "/market/items/WHEAT/quote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCode").value("WHEAT"))
                .andExpect(jsonPath("$.basePrice").value(25))
                .andExpect(jsonPath("$.currentPrice").value(25));

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

        mockMvc.perform(get("/api/users/" + userId + "/market/items/WHEAT/quote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPrice").value(26))
                .andExpect(jsonPath("$.volume24h").isNumber())
                .andExpect(jsonPath("$.tradeCount24h").isNumber());

        mockMvc.perform(post("/api/users/" + userId + "/market/sell")
                        .contentType("application/json")
                        .content("{\"itemCode\":\"WHEAT\",\"quantity\":40,\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.side").value("SELL"))
                .andExpect(jsonPath("$.unitPrice").value(26))
                .andExpect(jsonPath("$.grossAmount").value(1040))
                .andExpect(jsonPath("$.taxAmount").value(31))
                .andExpect(jsonPath("$.netAmount").value(1009))
                .andExpect(jsonPath("$.balanceAfter").value(8434))
                .andExpect(jsonPath("$.availableQuantityAfter").value(60));

        mockMvc.perform(get("/api/users/" + userId + "/market/items/WHEAT/quote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPrice").value(25));

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

        Assertions.assertEquals(8434L, balance);
        Assertions.assertEquals(60L, wheatQuantity);
        Assertions.assertEquals(2, tradeCount);
        Assertions.assertEquals(2, taxCount);
        Assertions.assertEquals(4, ledgerCount);

        Integer snapshotCount = jdbcTemplate.queryForObject(
                "select count(*) from market_price_snapshots mps join items i on i.id = mps.item_id where i.code = 'WHEAT'",
                Integer.class
        );
        Assertions.assertTrue(snapshotCount != null && snapshotCount >= 2);
    }

    @Test
    void bulkMarketTradeRequiresTokenAndConsumesIt() throws Exception {
        String userId = registerTestUser();
        setTradePassword(userId);
        resetMarketTaxRate();
        jdbcTemplate.update("update wallets set balance = 300000 where user_id = ?::uuid", userId);
        jdbcTemplate.update("update items set current_price = base_price where code = 'WHEAT'");

        mockMvc.perform(post("/api/users/" + userId + "/market/buy")
                        .contentType("application/json")
                        .content("{\"itemCode\":\"WHEAT\",\"quantity\":5000,\"tradePassword\":\"654321\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("大宗交易需要提供有效令牌"));

        MvcResult tokenResult = mockMvc.perform(post("/api/users/" + userId + "/bulk-tokens")
                        .contentType("application/json")
                        .content("{\"allowedItemType\":\"HARVEST\",\"singleTradeLimit\":200000,\"totalLimit\":200000,\"remainingUses\":1,\"expireHours\":24}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingUses").value(1))
                .andReturn();
        String tokenCode = extractJsonString(tokenResult.getResponse().getContentAsString(), "tokenCode");

        mockMvc.perform(get("/api/users/" + userId + "/bulk-tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tokenCode").value(tokenCode));

        mockMvc.perform(post("/api/users/" + userId + "/market/buy")
                        .contentType("application/json")
                        .content("{\"itemCode\":\"WHEAT\",\"quantity\":5000,\"tradePassword\":\"654321\",\"bulkTokenCode\":\"" + tokenCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossAmount").value(125000))
                .andExpect(jsonPath("$.taxAmount").value(3750));

        Integer remainingUses = jdbcTemplate.queryForObject(
                "select remaining_uses from bulk_trade_tokens where token_code = ?",
                Integer.class,
                tokenCode
        );
        Long usedAmount = jdbcTemplate.queryForObject(
                "select used_amount from bulk_trade_tokens where token_code = ?",
                Long.class,
                tokenCode
        );
        String status = jdbcTemplate.queryForObject(
                "select status from bulk_trade_tokens where token_code = ?",
                String.class,
                tokenCode
        );

        Assertions.assertEquals(0, remainingUses);
        Assertions.assertEquals(125000L, usedAmount);
        Assertions.assertEquals("USED", status);
    }

    @Test
    void privateTradeCreateCancelAndAcceptWork() throws Exception {
        String sellerId = registerTestUser();
        String buyerId = registerTestUser();
        setTradePassword(sellerId);
        setTradePassword(buyerId);
        grantInventory(sellerId, "WHEAT", 100);

        MvcResult cancelOfferResult = mockMvc.perform(post("/api/users/" + sellerId + "/private-trades")
                        .contentType("application/json")
                        .content("{\"buyerUserId\":\"" + buyerId + "\",\"itemCode\":\"WHEAT\",\"quantity\":10,\"priceAmount\":500,\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCode").value("WHEAT"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.priceAmount").value(500))
                .andExpect(jsonPath("$.taxAmount").value(25))
                .andExpect(jsonPath("$.status").value("WAIT_ACCEPT"))
                .andReturn();

        String cancelOfferId = extractJsonString(cancelOfferResult.getResponse().getContentAsString(), "offerId");
        Long lockedAfterCreate = jdbcTemplate.queryForObject(
                "select pi.locked_quantity from player_inventory pi join items i on i.id = pi.item_id where pi.user_id = ?::uuid and i.code = 'WHEAT'",
                Long.class,
                sellerId
        );
        Assertions.assertEquals(10L, lockedAfterCreate);

        mockMvc.perform(post("/api/users/" + sellerId + "/private-trades/" + cancelOfferId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Long lockedAfterCancel = jdbcTemplate.queryForObject(
                "select pi.locked_quantity from player_inventory pi join items i on i.id = pi.item_id where pi.user_id = ?::uuid and i.code = 'WHEAT'",
                Long.class,
                sellerId
        );
        Long availableAfterCancel = jdbcTemplate.queryForObject(
                "select pi.available_quantity from player_inventory pi join items i on i.id = pi.item_id where pi.user_id = ?::uuid and i.code = 'WHEAT'",
                Long.class,
                sellerId
        );
        Assertions.assertEquals(0L, lockedAfterCancel);
        Assertions.assertEquals(100L, availableAfterCancel);

        MvcResult offerResult = mockMvc.perform(post("/api/users/" + sellerId + "/private-trades")
                        .contentType("application/json")
                        .content("{\"buyerUserId\":\"" + buyerId + "\",\"itemCode\":\"WHEAT\",\"quantity\":20,\"priceAmount\":1000,\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxAmount").value(50))
                .andReturn();

        String offerId = extractJsonString(offerResult.getResponse().getContentAsString(), "offerId");
        mockMvc.perform(post("/api/users/" + buyerId + "/private-trades/" + offerId + "/accept")
                        .contentType("application/json")
                        .content("{\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.priceAmount").value(1000))
                .andExpect(jsonPath("$.taxAmount").value(50))
                .andExpect(jsonPath("$.buyerBalanceAfter").value(8950))
                .andExpect(jsonPath("$.sellerBalanceAfter").value(11000))
                .andExpect(jsonPath("$.buyerQuantityAfter").value(20));

        Long sellerAvailable = jdbcTemplate.queryForObject(
                "select pi.available_quantity from player_inventory pi join items i on i.id = pi.item_id where pi.user_id = ?::uuid and i.code = 'WHEAT'",
                Long.class,
                sellerId
        );
        Long sellerLocked = jdbcTemplate.queryForObject(
                "select pi.locked_quantity from player_inventory pi join items i on i.id = pi.item_id where pi.user_id = ?::uuid and i.code = 'WHEAT'",
                Long.class,
                sellerId
        );
        Integer taxCount = jdbcTemplate.queryForObject(
                "select count(*) from tax_records where ref_id = ?::uuid and trade_type = 'PRIVATE'",
                Integer.class,
                offerId
        );
        Integer completedCount = jdbcTemplate.queryForObject(
                "select count(*) from private_trade_offers where id = ?::uuid and status = 'COMPLETED'",
                Integer.class,
                offerId
        );

        Assertions.assertEquals(80L, sellerAvailable);
        Assertions.assertEquals(0L, sellerLocked);
        Assertions.assertEquals(1, taxCount);
        Assertions.assertEquals(1, completedCount);

        mockMvc.perform(post("/api/users/" + buyerId + "/private-trades/" + offerId + "/accept")
                        .contentType("application/json")
                        .content("{\"tradePassword\":\"654321\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void bulkPrivateTradeRequiresBuyerTokenOnAccept() throws Exception {
        String sellerId = registerTestUser();
        String buyerId = registerTestUser();
        setTradePassword(sellerId);
        setTradePassword(buyerId);
        grantInventory(sellerId, "WHEAT", 6000);
        jdbcTemplate.update("update wallets set balance = 300000 where user_id = ?::uuid", buyerId);

        MvcResult offerResult = mockMvc.perform(post("/api/users/" + sellerId + "/private-trades")
                        .contentType("application/json")
                        .content("{\"buyerUserId\":\"" + buyerId + "\",\"itemCode\":\"WHEAT\",\"quantity\":5000,\"priceAmount\":125000,\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String offerId = extractJsonString(offerResult.getResponse().getContentAsString(), "offerId");

        mockMvc.perform(post("/api/users/" + buyerId + "/private-trades/" + offerId + "/accept")
                        .contentType("application/json")
                        .content("{\"tradePassword\":\"654321\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("大宗交易需要提供有效令牌"));

        MvcResult tokenResult = mockMvc.perform(post("/api/users/" + buyerId + "/bulk-tokens")
                        .contentType("application/json")
                        .content("{\"allowedItemType\":\"HARVEST\",\"singleTradeLimit\":200000,\"totalLimit\":200000,\"remainingUses\":1,\"expireHours\":24}"))
                .andExpect(status().isOk())
                .andReturn();
        String tokenCode = extractJsonString(tokenResult.getResponse().getContentAsString(), "tokenCode");

        mockMvc.perform(post("/api/users/" + buyerId + "/private-trades/" + offerId + "/accept")
                        .contentType("application/json")
                        .content("{\"tradePassword\":\"654321\",\"bulkTokenCode\":\"" + tokenCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.priceAmount").value(125000))
                .andExpect(jsonPath("$.taxAmount").value(6250));

        Integer linkedOfferCount = jdbcTemplate.queryForObject(
                "select count(*) from private_trade_offers p join bulk_trade_tokens b on b.id = p.bulk_token_id where p.id = ?::uuid and b.token_code = ?",
                Integer.class,
                offerId,
                tokenCode
        );
        Integer remainingUses = jdbcTemplate.queryForObject(
                "select remaining_uses from bulk_trade_tokens where token_code = ?",
                Integer.class,
                tokenCode
        );

        Assertions.assertEquals(1, linkedOfferCount);
        Assertions.assertEquals(0, remainingUses);
    }

    @Test
    void adminCanUpdateTaxIssueTokenAndQueryUserAssetsTrades() throws Exception {
        String adminId = registerTestUser();
        String adminUsername = lastRegisteredUsername;
        String playerId = registerTestUser();
        String playerUsername = lastRegisteredUsername;
        makeAdmin(adminId);
        setTradePassword(playerId);
        jdbcTemplate.update("update items set current_price = base_price where code = 'WHEAT'");
        String adminToken = login(adminUsername);
        String playerToken = login(playerUsername);

        mockMvc.perform(get("/api/admin/tax-configs")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("需要管理员权限"));

        mockMvc.perform(get("/api/admin/tax-configs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(put("/api/admin/tax-configs/MARKET")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"rateBasisPoints\":250,\"reason\":\"测试调整交易站税率\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tradeType").value("MARKET"))
                .andExpect(jsonPath("$.rateBasisPoints").value(250))
                .andExpect(jsonPath("$.updatedBy").value(adminId));

        MvcResult tokenResult = mockMvc.perform(post("/api/admin/users/" + playerId + "/bulk-tokens")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"allowedItemType\":\"HARVEST\",\"singleTradeLimit\":200000,\"totalLimit\":200000,\"remainingUses\":2,\"expireHours\":24}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingUses").value(2))
                .andReturn();
        String tokenCode = extractJsonString(tokenResult.getResponse().getContentAsString(), "tokenCode");

        mockMvc.perform(post("/api/users/" + playerId + "/market/buy")
                        .contentType("application/json")
                        .content("{\"itemCode\":\"WHEAT\",\"quantity\":100,\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxAmount").value(62));

        mockMvc.perform(get("/api/admin/users/" + playerId + "/assets")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(playerId))
                .andExpect(jsonPath("$.inventory[0].itemCode").value("WHEAT"));

        mockMvc.perform(get("/api/admin/users/" + playerId + "/trades")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tradeSource").value("MARKET"))
                .andExpect(jsonPath("$[0].itemCode").value("WHEAT"));

        Integer auditCount = jdbcTemplate.queryForObject(
                "select count(*) from admin_audit_logs where admin_user_id = ?::uuid and action in ('UPDATE_TAX_CONFIG', 'ISSUE_BULK_TOKEN')",
                Integer.class,
                adminId
        );
        Integer tokenCount = jdbcTemplate.queryForObject(
                "select count(*) from bulk_trade_tokens where user_id = ?::uuid and token_code = ?",
                Integer.class,
                playerId,
                tokenCode
        );

        Assertions.assertEquals(2, auditCount);
        Assertions.assertEquals(1, tokenCount);
    }

    private String registerTestUser() throws Exception {
        String username = "tester_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        lastRegisteredUsername = username;

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

    private void setTradePassword(String userId) throws Exception {
        mockMvc.perform(post("/api/users/" + userId + "/trade-password")
                        .contentType("application/json")
                        .content("{\"tradePassword\":\"654321\"}"))
                .andExpect(status().isOk());
    }

    private String login(String username) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn();
        return extractJsonString(loginResult.getResponse().getContentAsString(), "accessToken");
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

    private void makeAdmin(String userId) {
        jdbcTemplate.update("update app_users set role = 'ADMIN' where id = ?::uuid", userId);
    }

    private void resetMarketTaxRate() {
        jdbcTemplate.update("update tax_config set rate_basis_points = 300, updated_by = null, updated_reason = 'Test reset market tax', updated_at = now() where trade_type = 'MARKET'");
    }

    private String extractJsonString(String json, String fieldName) {
        return json.replaceAll(".*\"" + fieldName + "\":\"([^\"]+)\".*", "$1");
    }
}
