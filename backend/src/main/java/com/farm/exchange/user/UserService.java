package com.farm.exchange.user;

import com.farm.exchange.common.ApiException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final long INITIAL_BALANCE = 10000L;

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public UserService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        UUID userId = UUID.randomUUID();
        String passwordHash = passwordEncoder.encode(request.getPassword());

        try {
            jdbcTemplate.update(
                    "insert into app_users (id, username, nickname, password_hash) values (?, ?, ?, ?)",
                    userId, request.getUsername(), request.getNickname(), passwordHash
            );
        } catch (DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "用户名已存在");
        }

        jdbcTemplate.update(
                "insert into wallets (user_id, balance, locked_balance) values (?, ?, 0)",
                userId, INITIAL_BALANCE
        );

        int farmSlots = getInitialSlots("FARM");
        int ranchSlots = getInitialSlots("RANCH");
        createFarmPlots(userId, farmSlots);
        createRanchSlots(userId, ranchSlots);

        jdbcTemplate.update(
                "insert into asset_ledger (user_id, asset_type, change_amount, balance_after, reason, ref_type, ref_id) values (?, 'COIN', ?, ?, 'INITIAL_GRANT', 'USER', ?)",
                userId, INITIAL_BALANCE, INITIAL_BALANCE, userId
        );

        return new RegisterResponse(userId, request.getUsername(), request.getNickname(), INITIAL_BALANCE, farmSlots, ranchSlots, false);
    }

    @Transactional
    public TradePasswordResponse setTradePassword(UUID userId, SetTradePasswordRequest request) {
        Integer exists = jdbcTemplate.queryForObject(
                "select count(*) from app_users where id = ? and status = 'ACTIVE'",
                Integer.class,
                userId
        );
        if (exists == null || exists == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在或状态不可用");
        }

        String tradePasswordHash = passwordEncoder.encode(request.getTradePassword());
        int updated = jdbcTemplate.update(
                "update app_users set trade_password_hash = ?, trade_password_set_at = ?, trade_password_failed_count = 0, trade_password_locked_until = null, updated_at = now(), version = version + 1 where id = ?",
                tradePasswordHash,
                Timestamp.from(OffsetDateTime.now().toInstant()),
                userId
        );
        if (updated != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "交易密码设置失败，请重试");
        }

        return new TradePasswordResponse(userId, true);
    }

    private int getInitialSlots(String slotType) {
        Integer slots = jdbcTemplate.queryForObject(
                "select initial_slots from expansion_config where slot_type = ?",
                Integer.class,
                slotType
        );
        if (slots == null || slots <= 0) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "扩建配置缺失：" + slotType);
        }
        return slots;
    }

    private void createFarmPlots(UUID userId, int count) {
        for (int index = 1; index <= count; index++) {
            jdbcTemplate.update(
                    "insert into farm_plots (user_id, slot_index, status) values (?, ?, 'EMPTY')",
                    userId, index
            );
        }
    }

    private void createRanchSlots(UUID userId, int count) {
        for (int index = 1; index <= count; index++) {
            jdbcTemplate.update(
                    "insert into ranch_slots (user_id, slot_index, status) values (?, ?, 'EMPTY')",
                    userId, index
            );
        }
    }
}

