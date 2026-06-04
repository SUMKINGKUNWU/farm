package com.farm.exchange.user;

import com.farm.exchange.common.ApiException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class TradePasswordService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public TradePasswordService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public void verify(UUID userId, String rawTradePassword) {
        TradePasswordSnapshot snapshot = jdbcTemplate.query(
                "select trade_password_hash, trade_password_locked_until from app_users where id = ? and status = 'ACTIVE'",
                rs -> {
                    if (!rs.next()) {
                        throw new ApiException(HttpStatus.NOT_FOUND, "用户不存在或状态不可用");
                    }
                    return new TradePasswordSnapshot(
                            rs.getString("trade_password_hash"),
                            rs.getObject("trade_password_locked_until", OffsetDateTime.class)
                    );
                },
                userId
        );

        if (snapshot.hash == null || snapshot.hash.trim().isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "请先设置交易密码");
        }
        if (snapshot.lockedUntil != null && snapshot.lockedUntil.isAfter(OffsetDateTime.now())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "交易密码已锁定，请稍后再试");
        }
        if (!passwordEncoder.matches(rawTradePassword, snapshot.hash)) {
            jdbcTemplate.update(
                    "update app_users set trade_password_failed_count = trade_password_failed_count + 1, updated_at = now(), version = version + 1 where id = ?",
                    userId
            );
            throw new ApiException(HttpStatus.FORBIDDEN, "交易密码错误");
        }

        jdbcTemplate.update(
                "update app_users set trade_password_failed_count = 0, trade_password_locked_until = null, updated_at = now(), version = version + 1 where id = ?",
                userId
        );
    }

    private static class TradePasswordSnapshot {
        private final String hash;
        private final OffsetDateTime lockedUntil;

        private TradePasswordSnapshot(String hash, OffsetDateTime lockedUntil) {
            this.hash = hash;
            this.lockedUntil = lockedUntil;
        }
    }
}

