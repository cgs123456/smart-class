package com.cgs.smartclassbackendserviceclient.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import feign.RequestInterceptor;

/**
 * service-client 模块自动配置：注册 Feign 内部调用拦截器，
 * 在所有 Feign 调用中注入 X-Internal-Call: true 头。
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class SmartclassServiceClientAutoConfiguration {
    @Bean
    public FeignInternalCallInterceptor feignInternalCallInterceptor() {
        return new FeignInternalCallInterceptor();
    }
}
