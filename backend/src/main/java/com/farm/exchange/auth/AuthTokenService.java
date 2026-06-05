package com.farm.exchange.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.farm.exchange.common.ApiException;
import com.farm.exchange.common.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long ttlSeconds;

    public AuthTokenService(
            ObjectMapper objectMapper,
            @Value("${farm.auth.token-secret:farm-exchange-local-dev-secret-change-me}") String tokenSecret,
            @Value("${farm.auth.token-ttl-seconds:86400}") long ttlSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secret = tokenSecret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(AuthPrincipal principal) {
        long expiresAt = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "FARM_TOKEN");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", principal.getUserId().toString());
        payload.put("username", principal.getUsername());
        payload.put("role", principal.getRole());
        payload.put("exp", expiresAt);

        String headerPart = encodeJson(header);
        String payloadPart = encodeJson(payload);
        String signaturePart = sign(headerPart + "." + payloadPart);
        return headerPart + "." + payloadPart + "." + signaturePart;
    }

    public AuthPrincipal require(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_REQUIRED, "请先登录");
        }
        return parse(authorizationHeader.substring("Bearer ".length()).trim());
    }

    private AuthPrincipal parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_INVALID, "登录凭证格式不正确");
            }
            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_INVALID, "登录凭证签名无效");
            }

            Map<String, Object> payload = objectMapper.readValue(base64Decode(parts[1]), MAP_TYPE);
            long expiresAt = ((Number) payload.get("exp")).longValue();
            if (expiresAt < Instant.now().getEpochSecond()) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_EXPIRED, "登录凭证已过期");
            }

            return new AuthPrincipal(
                    UUID.fromString((String) payload.get("sub")),
                    (String) payload.get("username"),
                    (String) payload.get("role")
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_INVALID, "登录凭证无效");
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return base64Encode(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.CONFIG_MISSING, "生成登录凭证失败");
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return base64Encode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.CONFIG_MISSING, "签名登录凭证失败");
        }
    }

    private String base64Encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] base64Decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int result = 0;
        for (int index = 0; index < leftBytes.length; index++) {
            result |= leftBytes[index] ^ rightBytes[index];
        }
        return result == 0;
    }
}
