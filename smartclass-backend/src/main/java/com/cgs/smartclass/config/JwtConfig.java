package com.cgs.smartclass.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret = "smartclass-default-secret-key-must-be-at-least-256-bits-long-for-hs256";
    private long expiration = 7 * 24 * 60 * 60 * 1000L; // 7天
    private String header = "Authorization";
    private String tokenPrefix = "Bearer ";
}
