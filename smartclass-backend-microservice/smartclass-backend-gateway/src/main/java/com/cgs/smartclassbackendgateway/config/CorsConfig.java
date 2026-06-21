package com.cgs.smartclassbackendgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 跨域配置类
 * 处理网关的全局跨域配置
 */
@Configuration
public class CorsConfig {

    /**
     * 跨域过滤器配置
     * 使用 CorsWebFilter 统一处理跨域，包括 OPTIONS 预检请求
     * 注意：不再使用额外的 WebFilter 处理 OPTIONS，避免与 CorsWebFilter 冲突
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // 允许所有域名进行跨域调用
        corsConfig.addAllowedOriginPattern("*");
        
        // 允许所有头信息
        corsConfig.addAllowedHeader("*");
        
        // 允许所有HTTP方法
        corsConfig.addAllowedMethod("*");
        
        // 允许携带认证信息
        corsConfig.setAllowCredentials(true);
        
        // 预检请求的缓存时间（秒）
        corsConfig.setMaxAge(3600L);
        
        // 暴露的头信息，让前端能够获取到
        corsConfig.addExposedHeader("Content-Length");
        corsConfig.addExposedHeader("Cache-Control");
        corsConfig.addExposedHeader("Content-Language");
        corsConfig.addExposedHeader("Content-Type");
        corsConfig.addExposedHeader("Expires");
        corsConfig.addExposedHeader("Last-Modified");
        corsConfig.addExposedHeader("Pragma");
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}
