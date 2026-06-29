package com.cgs.smartclassbackenduserservice.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 工具类（user 服务专用，负责签发 token）
 *
 * <p>密钥必须通过 {@code jwt.secret} 配置项注入（环境变量 JWT_SECRET），
 * 与网关 {@code GatewayJwtUtil} 使用相同密钥，确保网关可解析 user 服务签发的 token。</p>
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret 配置缺失，必须通过环境变量 JWT_SECRET 提供");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret 长度不足，HS256 要求至少 32 字节（256 位），当前长度: " + secretBytes.length);
        }
        signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    /**
     * 生成 JWT。
     *
     * @param userId   用户 ID（写入 subject 与 userId claim，便于网关解析）
     * @param userRole 用户角色
     * @return 签名后的 JWT 字符串
     */
    public String generateToken(Long userId, String userRole) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("userRole", userRole)
                .signWith(signingKey)
                .compact();
    }
}
