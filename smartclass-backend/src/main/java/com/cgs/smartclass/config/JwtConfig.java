package com.cgs.smartclass.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret;
    private long expiration = 7 * 24 * 60 * 60 * 1000L; // 7天
    private String header = "Authorization";
    private String tokenPrefix = "Bearer ";

    /**
     * 已知默认密钥的 SHA-256 摘要（十六进制小写）。
     * 仅保存摘要，避免在源码中以明文形式出现该默认值，便于被扫描工具识别为已清除。
     * 若用户误将 JWT_SECRET 配置为该已知默认值，启动时直接拒绝。
     */
    private static final String KNOWN_DEFAULT_SECRET_SHA256 =
            "ad386a9820952b06f04ab261d9ff41517b2a3f92989ec829723e1cce2b0c7146";

    @PostConstruct
    public void validate() {
        if (ObjectUtils.isEmpty(secret)) {
            throw new IllegalStateException("JWT 密钥未配置，请通过环境变量 JWT_SECRET 注入");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT 密钥长度不足 32 字节，无法满足 HS256 安全要求");
        }
        if (KNOWN_DEFAULT_SECRET_SHA256.equals(sha256Hex(secret))) {
            throw new IllegalStateException("JWT 密钥使用了已知默认值，请通过环境变量 JWT_SECRET 注入安全密钥");
        }
    }

    /**
     * 计算字符串的 SHA-256 十六进制摘要，用于与已知默认值摘要比较。
     */
    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] raw = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("JWT 密钥校验摘要计算失败", e);
        }
    }
}
