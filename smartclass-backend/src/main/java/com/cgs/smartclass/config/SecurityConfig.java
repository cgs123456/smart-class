package com.cgs.smartclass.config;

import com.cgs.smartclass.filter.JwtAuthenticationFilter;
import com.cgs.smartclass.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 *
 * <p>安全策略：</p>
 * <ul>
 *   <li>所有请求默认需要认证（anyRequest().authenticated()），不再放行全部</li>
 *   <li>仅显式白名单内的公开接口允许匿名访问</li>
 *   <li>敏感接口仍由 {@code @AuthCheck} AOP 切面做角色校验</li>
 *   <li>登录态通过 Session + JwtAuthenticationFilter 协同维护</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    /**
     * 白名单路径，按逗号分隔配置。
     * 默认包含登录、注册、验证码、微信回调、Swagger 文档、健康检查等公开接口。
     */
    @Value("${security.whitelist:}")
    private String whitelistConfig;

    /**
     * 默认白名单：即使没有配置也必须放行的最小集合。
     */
    private static final String[] DEFAULT_WHITELIST = {
            // 登录注册
            "/user/login",
            "/user/login/phone",
            "/user/register",
            "/user/register/phone",
            // 验证码
            "/captcha",
            "/captcha/**",
            // 微信公众号入口（根路径）
            "/",
            // 接口文档 Knife4j / Swagger
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/favicon.ico",
            // 健康检查
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            // 支付回调（依赖微信签名验签保护）
            "/payment/order/callback/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> {
                // 默认白名单
                auth.requestMatchers(DEFAULT_WHITELIST).permitAll();
                // 通过环境变量扩展的白名单
                if (whitelistConfig != null && !whitelistConfig.isBlank()) {
                    String[] extra = whitelistConfig.split(",");
                    for (int i = 0; i < extra.length; i++) {
                        extra[i] = extra[i].trim();
                    }
                    auth.requestMatchers(extra).permitAll();
                }
                // 其余所有请求必须认证
                auth.anyRequest().authenticated();
            })
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
