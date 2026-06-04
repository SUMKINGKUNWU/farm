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
}
