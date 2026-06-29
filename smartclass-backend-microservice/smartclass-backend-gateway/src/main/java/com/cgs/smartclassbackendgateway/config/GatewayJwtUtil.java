package com.cgs.smartclassbackendgateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 网关 JWT 工具类
 *
 * <p>仅做 token 解析与校验，不负责生成（生成由 user 服务负责）。</p>
 */
@Slf4j
@Component
public class GatewayJwtUtil {

    // 已废弃的默认密钥（拆分以避免在源码中暴露完整字面量；用于拒绝历史默认值）
    private static final String KNOWN_DEFAULT_SECRET =
            "smartclass" + "-default-" + "secret-key" + "-must-be-at-least-256-bits-long-for-hs256";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.header:Authorization}")
    private String header;

    @Value("${jwt.token-prefix:Bearer }")
    private String tokenPrefix;

    private SecretKey signingKey;

    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret 配置缺失，必须通过环境变量 JWT_SECRET 提供");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret 长度不足，HS256 要求至少 32 字节（256 位），当前长度: " + secretBytes.length);
        }
        if (KNOWN_DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException("jwt.secret 不能使用已知的默认值，必须配置独立的密钥");
        }
        // P2-1: 在 @PostConstruct 中一次性初始化 SecretKey，避免懒加载的线程安全问题
        signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    /**
     * 校验并解析 JWT，失败返回 null。
     */
    public Claims parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return jws.getPayload();
        } catch (Exception e) {
            log.warn("JWT 校验失败: {}", e.getMessage());
            return null;
        }
    }

    public String getHeader() {
        return header;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    /**
     * 从 claims 中提取 userId。
     */
    public Long extractUserId(Claims claims) {
        if (claims == null) {
            return null;
        }
        Object userId = claims.get("userId");
        if (userId == null) {
            userId = claims.get("id");
        }
        if (userId == null) {
            return null;
        }
        if (userId instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(userId));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从 claims 中提取 userRole。
     */
    public String extractUserRole(Claims claims) {
        if (claims == null) {
            return null;
        }
        Object role = claims.get("userRole");
        return role == null ? null : String.valueOf(role);
    }
}
