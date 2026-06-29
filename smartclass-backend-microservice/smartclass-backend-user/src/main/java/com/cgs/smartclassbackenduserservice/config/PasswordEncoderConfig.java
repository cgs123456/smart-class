package com.cgs.smartclassbackenduserservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置
 *
 * <p>使用 BCrypt 替代原 MD5 + 硬编码 SALT 方案。BCrypt 内部自带随机盐，
 * 同一明文每次加密结果不同，可有效防御彩虹表攻击。</p>
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
