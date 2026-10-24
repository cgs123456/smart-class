package com.smartclass.smartclassbackendgateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

/**
 * JWT 工具类
 */
@Component
public class GatewayJwtUtil {

    private static final Logger log = LoggerFactory.getLogger(GatewayJwtUtil.class);

    private String secret = "smartclass-secret-key-2024";
    private String header = "Authorization";
    private String tokenPrefix = "Bearer ";

    public String getHeader() { return header; }
    public String getTokenPrefix() { return tokenPrefix; }

    public void setSecret(String secret) { this.secret = secret; }
    public void setHeader(String header) { this.header = header; }
    public void setTokenPrefix(String tokenPrefix) { this.tokenPrefix = tokenPrefix; }

    public Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
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