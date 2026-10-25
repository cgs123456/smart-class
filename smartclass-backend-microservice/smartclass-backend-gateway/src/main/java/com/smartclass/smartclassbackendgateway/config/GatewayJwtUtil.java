package com.smartclass.smartclassbackendgateway.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT 工具类
 */
@Component
public class GatewayJwtUtil {

    private static final Logger log = LoggerFactory.getLogger(GatewayJwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.header:Authorization}")
    private String header;

    @Value("${jwt.token-prefix:Bearer }")
    private String tokenPrefix;

    /**
     * HS256 要求密钥不少于 32 字节；缺失或过短直接启动失败，
     * 杜绝硬编码兜底密钥被用于伪造 token。
     */
    @PostConstruct
    void validateSecret() {
        int length = secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length;
        if (length < 32) {
            throw new IllegalStateException(
                    "jwt.secret 未配置或长度不足：请通过环境变量 JWT_SECRET 设置不少于 32 字节的密钥");
        }
    }

    public String getHeader() { return header; }
    public String getTokenPrefix() { return tokenPrefix; }

    public Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Jws<Claims> parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
    }

    public Claims parseAndValidate(String token) {
        try {
            Jws<Claims> jws = parseToken(token);
            Claims claims = jws.getBody();
            // 签发方必须写入 exp；缺失 exp 视为不合规 token 直接拒绝，
            // 避免对无过期字段的历史 token 无条件放行
            if (claims.getExpiration() == null) {
                log.warn("Token 缺少 exp 声明，拒绝");
                return null;
            }
            if (claims.getExpiration().before(new Date())) {
                log.warn("Token 已过期");
                return null;
            }
            return claims;
        } catch (Exception e) {
            log.warn("Token 解析失败: {}", e.getMessage());
            return null;
        }
    }

    public Long extractUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        if (userId instanceof String) {
            try {
                return Long.parseLong((String) userId);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public String extractUserRole(Claims claims) {
        Object role = claims.get("userRole");
        return role != null ? role.toString() : "student";
    }
}