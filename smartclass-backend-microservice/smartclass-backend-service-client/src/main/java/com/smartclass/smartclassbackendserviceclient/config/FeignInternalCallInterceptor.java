package com.smartclass.smartclassbackendserviceclient.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Feign 内部调用拦截器：以共享令牌标识服务间调用。
 *
 * <p>注入 {@code X-Internal-Call: true} 与 {@code X-Internal-Token: <令牌>}；
 * 下游 AuthInterceptor 只在令牌匹配时才采信内部头（见 common 模块）。
 * 未配置 INTERNAL_CALL_TOKEN 时只注入 X-Internal-Call，下游将拒绝信任。</p>
 *
 * <p>由 {@link SmartclassServiceClientAutoConfiguration} 通过 Spring Boot 自动配置机制注册，
 * 不再使用 {@code @Component}（因各消费服务的 {@code @SpringBootApplication} 包名
 * 不会扫描到 {@code com.smartclass.smartclassbackendserviceclient.config} 包）。</p>
 */
@ConditionalOnClass(RequestInterceptor.class)
public class FeignInternalCallInterceptor implements RequestInterceptor {

    @Value("${INTERNAL_CALL_TOKEN:}")
    private String internalCallToken;

    @Override
    public void apply(RequestTemplate template) {
        template.header("X-Internal-Call", "true");
        if (!internalCallToken.isEmpty()) {
            template.header("X-Internal-Token", internalCallToken);
        }
    }
}
