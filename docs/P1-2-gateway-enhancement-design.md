# P1-2: Spring Cloud Gateway 统一网关增强设计文档

## 1. 现状分析

### 当前网关架构
- **组件**: Spring Cloud Gateway (SCG) 单实例/集群
- **路由配置**: 硬编码 YAML + 少量动态路由
- **鉴权**: 自定义 Filter 校验 JWT，逻辑分散在各微服务
- **限流**: Sentinel 接入不完整，仅有基础 QPS 限制
- **熔断**: 依赖 Resilience4j，配置分散
- **日志/追踪**: 手动添加 traceId，无统一结构化日志
- **协议**: 仅支持 HTTP/1.1 -> HTTP/1.1 转发

### 痛点
| 问题 | 影响 |
|------|------|
| 鉴权逻辑重复 | 每个微服务都要校验 JWT，维护成本高 |
| 限流/熔断配置分散 | 无法全局视角做流控，容易配错漏配 |
| 无统一日志格式 | 排查跨服务问题极难关联 |
| 无协议转换能力 | 无法支持 gRPC/Dubbo Triple 透传 (P1-1 需求) |
| 无请求/响应转换 | 无法做字段脱敏、版本兼容、聚合编排 |

---

## 2. 目标架构

```
                    ┌─────────────────────────────────────┐
                    │        Spring Cloud Gateway         │
                    │   (集群模式，Nacos 注册、配置中心)   │
                    └──────────────┬──────────────────────┘
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        ▼                          ▼                          ▼
┌───────────────┐         ┌───────────────┐         ┌───────────────┐
│  Global Filter │         │  Route Filter │         │  Plugin Chain │
│  Chain         │         │  Chain        │         │  (可插拔)     │
├───────────────┤         ├───────────────┤         ├───────────────┤
│ 1. TraceId    │         │ 1. Auth       │         │ - RateLimit   │
│ 2. Logging    │         │ 2. RateLimit  │         │ - CircuitBrk  │
│ 3. Cors       │         │ 3. CircuitBrk │         │ - Transform   │
│ 4. Security   │         │ 4. Rewrite    │         │ - Aggregation │
│   Headers     │         │ 5. Transform  │         │ - Mock/Chaos  │
└───────────────┤         └───────────────┘         └───────────────┘
        │                          │                          │
        └──────────────────────────┼──────────────────────────┘
                                   ▼
                    ┌─────────────────────────────────────┐
                    │      服务发现 + 负载均衡             │
                    │  (Nacos + Dubbo Triple / Feign)     │
                    └─────────────────────────────────────┘
```

---

## 3. 核心功能设计

### 3.1 统一鉴权 (Auth Filter)

**设计原则**: 网关完成认证，微服务只做授权 (RBAC/ABAC)

```java
@Component
@Order(-100) // 最先执行
public class JwtAuthenticationFilter implements GatewayFilter, Ordered {
    
    private final JwtTokenProvider tokenProvider;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final Set<String> publicPaths = Set.of(
        "/api/auth/**", "/api/public/**", "/actuator/**"
    );
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // 1. 公开路径放行
        if (publicPaths.stream().anyMatch(path::matches)) {
            return chain.filter(exchange);
        }
        
        // 2. 提取并校验 Token
        String token = extractToken(exchange);
        if (token == null || !tokenProvider.validateToken(token)) {
            return unauthorizedResponse(exchange, "Invalid or expired token");
        }
        
        // 3. 解析用户上下文，注入 Header 传给下游
        Claims claims = tokenProvider.parseClaims(token);
        Long userId = claims.get("userId", Long.class);
        String roles = claims.get("roles", String.class);
        
        // 4. Redis 二级校验 (黑名单/单点登录)
        String blacklistKey = "token:blacklist:" + token;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey).block())) {
            return unauthorizedResponse(exchange, "Token revoked");
        }
        
        // 5. 透传用户上下文
        ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header("X-User-Id", String.valueOf(userId))
            .header("X-User-Roles", roles)
            .header("X-Trace-Id", exchange.getRequest().getHeaders().getFirst("X-Trace-Id"))
            .build();
        
        return chain.filter(exchange.mutate().request(mutated).build());
    }
}
```

**配置化路由级鉴权**：
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://smartclass-user
          predicates: [Path=/api/user/**]
          filters:
            - name: Authentication
              args:
                requiredRoles: "STUDENT,TEACHER"  # 路由级角色要求
                skipPaths: "/api/user/public/**"  # 路由内豁免路径
```

### 3.2 统一限流 (RateLimit Filter)

**多维度限流策略**：
```java
public enum RateLimitDimension {
    USER_ID("userId", "按用户限流"),
    IP("ip", "按 IP 限流"),
    API_PATH("path", "按接口限流"),
    TENANT("tenant", "按租户限流 (P1-3)");
}
```

**配置示例** (Nacos 动态配置)：
```yaml
gateway:
  ratelimit:
    default:
      replenishRate: 100      # 令牌桶填充速率 (req/s)
      burstCapacity: 200      # 桶容量
      keyResolver: userId     # 默认按用户
    routes:
      - routeId: ai-chat-service
        replenishRate: 20     # AI 聊天更严格
        burstCapacity: 30
        keyResolver: userId
      - routeId: file-upload
        replenishRate: 5
        burstCapacity: 10
        keyResolver: ip
    custom:
      - path: /api/ai/**      # 精确路径规则
        replenishRate: 10
        keyResolver: userId
```

**实现**: 基于 `RequestRateLimiterGatewayFilterFactory` 扩展，支持 Redis + Lua 脚本原子操作，兼容 Sentinel 规则迁移。

### 3.3 统一熔断 (CircuitBreaker Filter)

```yaml
gateway:
  circuitbreaker:
    default:
      failureRateThreshold: 50      # 失败率阈值
      slowCallRateThreshold: 80     # 慢调用阈值
      slowCallDurationThreshold: 3s # 慢调用判定
      waitDurationInOpenState: 30s  # 熔断持续时间
      permittedNumberOfCallsInHalfOpenState: 10
      slidingWindowSize: 100
      slidingWindowType: COUNT_BASED
    routes:
      - routeId: intelligence-service
        failureRateThreshold: 30    # AI 服务更敏感
        waitDurationInOpenState: 60s
```

**降级策略**：
- 熔断开启时返回预设静态响应或重定向降级页面
- 支持按路由配置不同的 Fallback 响应

### 3.4 请求/响应转换 (Transform Filter)

```yaml
gateway:
  transform:
    request:
      - routeId: user-service
        addHeaders:
          X-Source: "gateway"
        removeHeaders: ["Cookie", "Authorization"]  # 脱敏
        rewritePath: "/api/user/(?<segment>.*)", "/${segment}"
    response:
      - routeId: "*"
        addHeaders:
          X-Response-Time: "#{T(System).currentTimeMillis() - requestStartTime}"
        modifyBody:
          # 统一响应格式包装
          enable: true
          wrapperClass: "com.smartclass.smartclass.common.ApiResponse"
```

### 3.5 结构化日志 + 链路追踪

```yaml
logging:
  pattern:
    gateway: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] [%X{spanId}] %-5level [%thread] %logger{36} - %msg%n"
  level:
    org.springframework.cloud.gateway: DEBUG
    reactor.netty.http.server: INFO

gateway:
  logging:
    enabled: true
    logRequestHeaders: true
    logResponseHeaders: false
    logRequestBody: false    # 生产关闭，调试开启
    logResponseBody: false
    sensitiveHeaders: ["Authorization", "Cookie", "X-Api-Key"]
    sampleRate: 1.0          # 全量采样，高流量可调 0.1
```

**输出样例** (JSON 格式，ELK 友好)：
```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "traceId": "a1b2c3d4e5f6",
  "spanId": "s1p2a3n4",
  "level": "INFO",
  "service": "gateway",
  "routeId": "user-service",
  "method": "GET",
  "path": "/api/user/profile",
  "status": 200,
  "latencyMs": 45,
  "userId": 12345,
  "clientIp": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "requestSize": 512,
  "responseSize": 1024
}
```

### 3.6 Dubbo Triple / gRPC 协议转发 (P1-1 配合)

```java
@Component
public class TripleProtocolFilter implements GatewayFilter {
    
    private final DubboTripleClient dubboClient; // 基于 Netty 的 Triple 客户端
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if (route == null || !"triple".equals(route.getMetadata().get("protocol"))) {
            return chain.filter(exchange); // 非 Triple 路由，走默认 HTTP
        }
        
        // 1. 解析 HTTP 请求 -> Protobuf Request
        // 2. 调用 Dubbo Triple 服务
        // 3. Protobuf Response -> HTTP 响应
        // 4. 透传 Metadata (traceId, userId, etc.)
    }
}
```

**路由配置**：
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: intelligence-triple
          uri: dubbo://smartclass-intelligence/com.smartclass.smartclass.intelligence.AiChatService
          predicates: [Path=/api/ai/**]
          metadata:
            protocol: triple
            method: sendChatMessage
          filters:
            - Authentication
            - RateLimit
            - CircuitBreaker
            - TripleProtocol
```

---

## 4. 配置管理：Nacos 动态化

```yaml
# nacos 配置: gateway-routes.yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes: []  # 由 Nacos 下发，不在本地配置
```

**Nacos 配置结构**：
```
Data ID: gateway-routes
Group: DEFAULT_GROUP
内容:
  routes:
    - id: user-service
      uri: lb://smartclass-user
      predicates: [...]
      filters: [...]
      metadata:
        protocol: http
    - id: intelligence-triple
      uri: dubbo://...
      metadata:
        protocol: triple
```

**热刷新**: `@RefreshScope` + `ApplicationListener<RefreshEvent>` 实现路由零停机更新。

---

## 5. 观测性集成

| 指标 | 采集方式 | 告警阈值 |
|------|----------|----------|
| 网关 QPS | Micrometer + Prometheus | > 10000/s |
| 网关延迟 P99 | Micrometer Timer | > 500ms |
| 熔断器状态 | Micrometer Gauge | OPEN 状态持续 > 1min |
| 限流拒绝率 | Counter | > 5% |
| 鉴权失败率 | Counter | > 1% |
| 路由错误率 | Counter | > 0.5% |

**Grafana Dashboard**: 预置 Gateway 标准大盘 (请求热力图、延迟分位数、错误拓扑)。

---

## 6. 迁移计划

| 阶段 | 周期 | 内容 | 验收 |
|------|------|------|------|
| Phase 1 | Week 1-2 | 统一鉴权 Filter + 结构化日志 | 所有流量走网关鉴权，日志可关联 |
| Phase 2 | Week 3-4 | 限流/熔断 Filter + Nacos 动态配置 | 核心接口限流生效，熔断可观测 |
| Phase 3 | Week 5-6 | 请求/响应转换 + 协议转发 (Triple) | AI 服务走 Triple，响应统一包装 |
| Phase 4 | Week 7 | 压测调优 + 文档 + 运维演练 | 单实例 20k QPS，P99 < 100ms |

---

## 7. 风险与缓解

| 风险 | 缓解 |
|------|------|
| Filter 顺序冲突导致逻辑异常 | 统一 `@Order` 常量管理，单元测试覆盖链路 |
| Nacos 配置推送延迟导致路由不一致 | 本地缓存 + 版本号校验，启动校验配置完整性 |
| Triple 协议转发性能不达标 | Netty 共享 EventLoop、连接池复用、异步非阻塞全链路 |
| 旧服务未接入网关 (直连) | 强制网关层入口，K8s NetworkPolicy 限制服务间直连 |

---

## 8. 工作量预估

| 活动 | 人天 |
|------|------|
| 统一鉴权 Filter 开发测试 | 3 |
| 限流/熔断 Filter + Nacos 动态配置 | 5 |
| 请求响应转换 + 结构化日志 | 3 |
| Triple/gRPC 协议转发适配 | 5 |
| 集成测试 + 压测调优 | 4 |
| 文档 + 运维手册 | 2 |
| **合计** | **~22 人天 (约 3-4 周, 1-2 人)** |

---

## 9. 与其他 P1 项依赖关系

| 依赖项 | 关系 |
|--------|------|
| **P1-1 Dubbo3** | Gateway 霐 Triple Filter 透传 Dubbo 调用 |
| **P1-3 多租户** | 网关层识别租户 ID，注入 Header，限流/日志按租户隔离 |
| **P1-4 Monorepo** | 网关代码迁移到 Monorepo 统一管理 |
| **P1-5 OpenAPI** | 网关路由可从 OpenAPI Spec 自动生成 |