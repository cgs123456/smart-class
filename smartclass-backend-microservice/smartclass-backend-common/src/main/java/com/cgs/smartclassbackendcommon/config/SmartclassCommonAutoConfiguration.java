package com.cgs.smartclassbackendcommon.config;

import com.cgs.smartclassbackendcommon.aop.AuthInterceptor;
import com.cgs.smartclassbackendcommon.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * common 模块自动配置
 *
 * <p>各微服务引入 {@code smartclass-backend-common} 依赖后，会自动注册：</p>
 * <ul>
 *   <li>{@link AuthInterceptor} — {@code @AuthCheck} 注解 AOP 切面</li>
 *   <li>{@link GlobalExceptionHandler} — 全局异常处理器</li>
 * </ul>
 *
 * <p>由于 common 模块的包名与各微服务主类包名不同，
 * 默认的 {@code @SpringBootApplication} 组件扫描不会覆盖到此处的类，
 * 因此通过 {@code META-INF/spring/AutoConfiguration.imports} 机制注册。</p>
 *
 * <p>{@code @ConditionalOnMissingBean} 确保即使微服务自身组件扫描已覆盖也不会重复注册。</p>
 */
@Configuration
public class SmartclassCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthInterceptor authInterceptor() {
        return new AuthInterceptor();
    }

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}

