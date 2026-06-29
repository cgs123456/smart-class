package com.cgs.smartclassbackendserviceclient.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Feign 内部调用拦截器：注入 X-Internal-Call 头标识为服务间调用，
 * 让下游 AuthInterceptor 跳过 @AuthCheck 角色校验。
 *
 * <p>由 {@link SmartclassServiceClientAutoConfiguration} 通过 Spring Boot 自动配置机制注册，
 * 不再使用 {@code @Component}（因各消费服务的 {@code @SpringBootApplication} 包名
 * 不会扫描到 {@code com.cgs.smartclassbackendserviceclient.config} 包）。</p>
 */
@ConditionalOnClass(RequestInterceptor.class)
public class FeignInternalCallInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        template.header("X-Internal-Call", "true");
    }
}
