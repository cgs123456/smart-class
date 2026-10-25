package com.smartclass.smartclass.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器独立配置。
 * <p>
 * 必须与 SecurityConfig 分离：UserServiceImpl 依赖 PasswordEncoder，
 * 若该 Bean 定义在 SecurityConfig 内，会形成
 * jwtAuthenticationFilter -> userServiceImpl -> securityConfig -> jwtAuthenticationFilter
 * 的构造环（SecurityConfig 构造器注入过滤器）。Bean 下沉到无依赖的独立配置类即可断环。
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
