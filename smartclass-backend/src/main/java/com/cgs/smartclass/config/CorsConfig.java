package com.cgs.smartclass.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置
 *
 * <p>生产环境应通过 {@code cors.allowed-origins} 显式指定允许的来源，
 * 不再使用 {@code *} 以避免与 {@code allowCredentials(true)} 冲突带来的安全风险。</p>
*/
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 允许的来源列表，按逗号分隔。
     * 例如：https://smartclass.cgs.cn,https://admin.smartclass.cgs.cn
     */
    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 覆盖所有请求
        registry.addMapping("/**")
                // 允许发送 Cookie
                .allowCredentials(true)
                // 放行哪些域名：优先使用配置；未配置时退化为 dev 本地域名
                .allowedOrigins(resolveAllowedOrigins())
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Disposition")
                .maxAge(3600L);
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
