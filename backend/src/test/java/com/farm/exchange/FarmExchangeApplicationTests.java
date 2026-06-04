package com.farm.exchange;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        String userId = body.replaceAll(".*\"userId\":\"([^\"]+)\".*", "$1");

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
}
