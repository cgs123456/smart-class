package com.cgs.smartclassbackendgateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 跨域配置类
 * 处理网关的全局跨域配置
 *
 * <p>生产环境通过 {@code cors.allowed-origins} 显式指定允许来源，
 * 不再使用 {@code *} 与 {@code allowCredentials(true)} 同时存在的危险配置。</p>
 */
@Configuration
public class CorsConfig {

    /**
     * 允许的来源列表，按逗号分隔。
     */
    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    /**
     * 跨域过滤器配置
     * 使用 CorsWebFilter 统一处理跨域，包括 OPTIONS 预检请求
     * 注意：不再使用额外的 WebFilter 处理 OPTIONS，避免与 CorsWebFilter 冲突
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // 显式指定允许的来源，未配置时退化为本地开发常用域名
        String[] origins = allowedOrigins.split(",");
        for (String origin : origins) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                corsConfig.addAllowedOrigin(trimmed);
            }
        }
        // 兜底：未配置时至少允许本地开发
        if (corsConfig.getAllowedOrigins() == null || corsConfig.getAllowedOrigins().isEmpty()) {
            corsConfig.addAllowedOrigin("http://localhost:5173");
            corsConfig.addAllowedOrigin("http://localhost:3000");
        }

        // 允许所有头信息
        corsConfig.addAllowedHeader("*");

        // 允许所有HTTP方法
        corsConfig.addAllowedMethod("*");

        // 允许携带认证信息
        corsConfig.setAllowCredentials(true);

        // 预检请求的缓存时间（秒）
        corsConfig.setMaxAge(3600L);

        // 暴露的头信息，让前端能够获取到
        corsConfig.addExposedHeader(HttpHeaders.CONTENT_LENGTH);
        corsConfig.addExposedHeader(HttpHeaders.CACHE_CONTROL);
        corsConfig.addExposedHeader(HttpHeaders.CONTENT_LANGUAGE);
        corsConfig.addExposedHeader(HttpHeaders.CONTENT_TYPE);
        corsConfig.addExposedHeader(HttpHeaders.EXPIRES);
        corsConfig.addExposedHeader(HttpHeaders.LAST_MODIFIED);
        corsConfig.addExposedHeader(HttpHeaders.PRAGMA);
        corsConfig.addExposedHeader(HttpHeaders.AUTHORIZATION);
        corsConfig.addExposedHeader("Content-Disposition");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
