package com.smartclass.smartclass.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * 全局跨域配置。
 *
 * <p>以 {@link CorsConfigurationSource} Bean 的形式供 Spring Security 的
 * {@code http.cors()} 消费：预检 OPTIONS 请求在认证之前由 CorsFilter 处理，
 * 避免受保护端点的预检被安全链 403 拦截（MVC 级 addCorsMappings 只能处理
 * 已进入 DispatcherServlet 的请求）。</p>
 *
 * <p>生产环境应通过 {@code cors.allowed-origins} 显式指定允许的来源，
 * 不再使用 {@code *} 以避免与 {@code allowCredentials(true)} 冲突带来的安全风险。</p>
 */
@Configuration
public class CorsConfig {

    /**
     * 允许的来源列表，按逗号分隔。
     * 例如：https://smartclass.example.com,https://admin.smartclass.example.com
     */
    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许发送 Cookie
        config.setAllowCredentials(true);
        config.setAllowedOrigins(Arrays.asList(resolveAllowedOrigins()));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * 解析允许的来源数组。
     *
     * <p>未配置时使用默认值（本地开发常用域名），生产环境必须通过环境变量显式指定，
     * 否则启动失败以避免使用通配来源带来的安全风险。</p>
     */
    private String[] resolveAllowedOrigins() {
        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            String[] origins = allowedOrigins.split(",");
            for (int i = 0; i < origins.length; i++) {
                origins[i] = origins[i].trim();
            }
            return origins;
        }
        // 生产环境必须显式配置允许的来源
        if ("prod".equalsIgnoreCase(activeProfile)) {
            throw new IllegalStateException(
                    "生产环境(cors.allowed-origins)未配置，禁止使用默认开发域名启动");
        }
        // 默认放行本地开发环境
        return new String[]{
                "http://localhost:5173",
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000",
                "http://localhost:8080"
        };
    }
}
