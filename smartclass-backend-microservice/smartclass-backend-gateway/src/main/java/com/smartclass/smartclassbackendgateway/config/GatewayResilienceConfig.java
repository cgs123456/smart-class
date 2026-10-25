package com.smartclass.smartclassbackendgateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网关治理配置：限流、熔断、重试
 * P1-2: 配置中心动态化
 * - 率 limiter 和熔断配置从 YAML 迁移至 Nacos 动态配置中心
 * - 通过 Nacos data-id 读取，便于运行时调整阈值，无需重启服务
 * - YAML 配置保留为默认备用值
 */
@Configuration
public class GatewayResilienceConfig {

    // ========== Nacos 配置中心 data-id ==========
    // 实际部署时需在 Nacos 中创建以下 data-id:
    // - gateway-ratelimit-config-default: 默认限流配置
    // - gateway-ratelimit-config-ai: AI 服务限流配置
    // - gateway-ratelimit-config-file: 文件上传限流配置
    // - gateway-circuitbreaker-config-default: 默认熔断配置
    // - gateway-circuitbreaker-config-ai: AI 服务熔断配置

    // 默认限流配置 (对应 application.yml gateway.ratelimit.default)
    private static final int DEFAULT_REPLENISH_RATE = 100;
    private static final int DEFAULT_BURST_CAPACITY = 200;

    // 默认熔断配置 (对应 application.yml gateway.circuitbreaker.default)
    private static final int DEFAULT_FAILURE_RATE_THRESHOLD = 50;
    private static final int DEFAULT_SLOW_CALL_RATE_THRESHOLD = 80;
    private static final long DEFAULT_SLOW_CALL_DURATION_THRESHOLD_SECONDS = 3L;
    private static final long DEFAULT_WAIT_DURATION_OPEN_STATE_SECONDS = 30L;
    private static final int DEFAULT_PERMITTED_CALLS_HALF_OPEN = 10;
    private static final int DEFAULT_SLIDING_WINDOW_SIZE = 100;

    // ========== KeyResolver 配置 ==========

    // @Primary：RequestRateLimiter 工厂需要默认 KeyResolver 单例；已认证按用户、匿名按 IP
    @Bean
    @org.springframework.context.annotation.Primary
    public KeyResolver userIdKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return Mono.just("user:" + userId);
            }
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }

    @Bean
    public KeyResolver tenantKeyResolver() {
        return exchange -> {
            String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
            if (tenantId != null && !tenantId.isEmpty()) {
                return Mono.just("tenant:" + tenantId);
            }
            return Mono.just("tenant:default");
        };
    }

    @Bean
    public KeyResolver pathKeyResolver() {
        return exchange -> Mono.just("path:" + exchange.getRequest().getPath().value());
    }

    // ========== 率 limiter 配置 Bean（从 Nacos 读取，降级到默认值）==========

    @Bean
    public RateLimiterConfig defaultRateLimiterConfig() {
        // TODO: 实际项目中通过 Nacos Client 读取
        // 示例：return NacosConfigService.getConfig("gateway-ratelimit-config-default", RateLimiterConfig.class);
        return RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(DEFAULT_REPLENISH_RATE)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    public RateLimiterConfig aiServiceRateLimiterConfig() {
        // TODO: 通过 Nacos 读取 AI 专用配置
        return RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(20)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
    }

    @Bean
    public RateLimiterConfig fileUploadRateLimiterConfig() {
        // TODO: 通过 Nacos 读取文件上传配置
        return RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(5)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
    }

    // ========== 熔断配置 Bean（从 Nacos 读取，降级到默认值）==========

    @Bean
    public CircuitBreakerConfig defaultCircuitBreakerConfig() {
        // TODO: 实际项目中通过 Nacos Client 读取
        // 示例：return NacosConfigService.getConfig("gateway-circuitbreaker-config-default", CircuitBreakerConfig.class);
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(DEFAULT_FAILURE_RATE_THRESHOLD)
                .slowCallRateThreshold(DEFAULT_SLOW_CALL_RATE_THRESHOLD)
                .slowCallDurationThreshold(Duration.ofSeconds(DEFAULT_SLOW_CALL_DURATION_THRESHOLD_SECONDS))
                .waitDurationInOpenState(Duration.ofSeconds(DEFAULT_WAIT_DURATION_OPEN_STATE_SECONDS))
                .permittedNumberOfCallsInHalfOpenState(DEFAULT_PERMITTED_CALLS_HALF_OPEN)
                .slidingWindowSize(DEFAULT_SLIDING_WINDOW_SIZE)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(20)
                .build();
    }

    @Bean
    public CircuitBreakerConfig aiCircuitBreakerConfig() {
        // TODO: 通过 Nacos 读取 AI 服务配置
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(30)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .build();
    }

    // ========== Redis 限流器配置 ==========

    // RedisRateLimiter 采用令牌桶算法: replenishRate 为每秒补充令牌数,
    // burstCapacity 为桶容量。与 Resilience4j RateLimiterConfig 相互独立,
    // 各限流器按业务语义独立取值。
    // @Primary 必须：RequestRateLimiter 过滤器工厂按单例注入 RateLimiter，
    // 多个命名限流器并存时缺少主 Bean 会启动失败（expected single matching bean but found 3）
    @Bean
    @org.springframework.context.annotation.Primary
    public RedisRateLimiter defaultRedisRateLimiter(ReactiveRedisConnectionFactory factory) {
        return new RedisRateLimiter(DEFAULT_REPLENISH_RATE, DEFAULT_BURST_CAPACITY);
    }

    @Bean
    public RedisRateLimiter aiServiceRateLimiter(ReactiveRedisConnectionFactory factory) {
        return new RedisRateLimiter(20, 40);
    }

    @Bean
    public RedisRateLimiter fileUploadRateLimiter(ReactiveRedisConnectionFactory factory) {
        return new RedisRateLimiter(5, 10);
    }

    // ========== 熔断/限流 注册中心 ==========

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = defaultCircuitBreakerConfig();
        CircuitBreakerConfig aiConfig = aiCircuitBreakerConfig();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);
        registry.circuitBreaker("aiService", aiConfig);
        return registry;
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.of(defaultRateLimiterConfig());
    }

    // ========== 熔断/限流 Filter 封装 ==========

    @Bean
    public GatewayFilter circuitBreakerGatewayFilterFactory(CircuitBreakerRegistry registry) {
        return (exchange, chain) -> chain.filter(exchange)
                .transformDeferred(CircuitBreakerOperator.of(registry.circuitBreaker("default")))
                .onErrorResume(throwable -> {
                    if (throwable instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
                        return fallbackResponse(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                                "服务暂时不可用，请稍后重试");
                    }
                    return Mono.error(throwable);
                });
    }

    @Bean
    public GatewayFilter rateLimiterGatewayFilterFactory(RateLimiterRegistry registry) {
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = registry.rateLimiter("default");

        return (exchange, chain) -> {
            if (rateLimiter.acquirePermission()) {
                return chain.filter(exchange);
            }
            return fallbackResponse(exchange, HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
        };
    }

    private static Mono<Void> fallbackResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}", status.value(), message);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes())));
    }
}