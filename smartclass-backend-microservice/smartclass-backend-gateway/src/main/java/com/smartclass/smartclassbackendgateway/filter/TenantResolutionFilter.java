package com.smartclass.smartclassbackendgateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 租户识别与上下文注入 Filter
 * 
 * 识别优先级：
 * 1. Header: X-Tenant-Id (最高，网关/内部调用显式指定)
 * 2. Subdomain: tenant.smartclass.cn
 * 3. Path: /t/{tenantId}/**
 * 4. JWT Claim: tenant_id
 * 5. Default: "default" (开发/测试环境)
 */
@Component
public class TenantResolutionFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TenantResolutionFilter.class);

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // 租户缓存 (生产环境应用 Redis 分布式缓存)
    private final Map<String, TenantInfo> tenantCache = new ConcurrentHashMap<>();
    
    // 公开路径不需要租户
    private static final List<String> TENANT_WHITELIST = List.of(
            "/api/user/login",
            "/api/user/register",
            "/api/captcha",
            "/api/actuator/health",
            "/api/doc.html",
            "/api/webjars/**",
            "/api/v3/api-docs/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        // 1. 白名单跳过租户校验
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }
        
        // 2. 解析租户 ID
        String tenantId = resolveTenantId(request);
        
        // 3. 校验租户状态
        TenantInfo tenant = getTenantInfo(tenantId);
        if (tenant == null) {
            log.warn("租户不存在: {}", tenantId);
            return writeError(exchange, HttpStatus.NOT_FOUND, "租户不存在: " + tenantId);
        }
        
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            log.warn("租户状态异常: {} - {}", tenantId, tenant.getStatus());
            return writeError(exchange, HttpStatus.FORBIDDEN, "租户已禁用或过期: " + tenantId);
        }
        
        // 4. 注入租户上下文到下游 Header
        ServerHttpRequest mutated = request.mutate()
                .header("X-Tenant-Id", tenant.getId())
                .header("X-Tenant-Tier", tenant.getTier().name())
                .header("X-Tenant-Features", String.join(",", tenant.getEnabledFeatures()))
                .build();
        
        // 5. 将租户信息存入 exchange 属性供后续 Filter 使用
        exchange.getAttributes().put("tenantInfo", tenant);
        
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    /**
     * 解析租户 ID (按优先级)
     */
    private String resolveTenantId(ServerHttpRequest request) {
        // 优先级 1: Header
        String tenantId = request.getHeaders().getFirst("X-Tenant-Id");
        if (tenantId != null && !tenantId.isEmpty()) {
            return tenantId;
        }
        
        // 优先级 2: Subdomain (tenant.smartclass.cn)
        String host = request.getURI().getHost();
        if (host != null) {
            String subdomain = extractSubdomain(host);
            if (subdomain != null && !subdomain.equals("www") && !subdomain.equals("api")) {
                // 验证子域名是否为合法租户编码
                if (tenantCache.containsKey(subdomain)) {
                    return subdomain;
                }
            }
        }
        
        // 优先级 3: Path (/t/{tenantId}/...)
        String path = request.getURI().getPath();
        if (pathMatcher.match("/t/{tenantId}/**", path)) {
            Map<String, String> vars = pathMatcher.extractUriTemplateVariables("/t/{tenantId}/**", path);
            String pathTenantId = vars.get("tenantId");
            if (pathTenantId != null && tenantCache.containsKey(pathTenantId)) {
                return pathTenantId;
            }
        }
        
        // 优先级 4: JWT Claim (需要解析 JWT，这里简化处理)
        // 实际项目中应从 JWT 中提取 tenant_id claim
        // String jwtTenantId = extractTenantFromJwt(request);
        // if (jwtTenantId != null) return jwtTenantId;
        
        // 优先级 5: 默认租户
        return "default";
    }

    private String extractSubdomain(String host) {
        // host 格式: tenant.example.com:8080
        int portIndex = host.indexOf(':');
        String hostname = portIndex > 0 ? host.substring(0, portIndex) : host;
        String[] parts = hostname.split("\\.");
        if (parts.length >= 3) {
            return parts[0];
        }
        return null;
    }

    private boolean isWhitelisted(String path) {
        for (String pattern : TENANT_WHITELIST) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private TenantInfo getTenantInfo(String tenantId) {
        // 先查缓存
        TenantInfo cached = tenantCache.get(tenantId);
        if (cached != null) {
            return cached;
        }
        
        // 缓存未命中，实际项目应查询数据库/配置中心
        // 这里模拟默认租户配置
        if ("default".equals(tenantId)) {
            TenantInfo defaultTenant = TenantInfo.builder()
                    .id("default")
                    .name("默认租户")
                    .tier(TenantTier.SHARED)
                    .status(TenantStatus.ACTIVE)
                    .enabledFeatures(List.of("AI_CHAT", "RAG_SEARCH", "COURSE_MARKET", "CIRCLE"))
                    .build();
            tenantCache.put("default", defaultTenant);
            return defaultTenant;
        }
        
        // 模拟演示租户
        if ("demo".equals(tenantId)) {
            TenantInfo demoTenant = TenantInfo.builder()
                    .id("demo")
                    .name("演示租户")
                    .tier(TenantTier.SHARED)
                    .status(TenantStatus.ACTIVE)
                    .enabledFeatures(List.of("AI_CHAT", "COURSE_MARKET"))
                    .build();
            tenantCache.put("demo", demoTenant);
            return demoTenant;
        }
        
        return null;
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}", status.value(), message);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        // 在鉴权之后、业务逻辑之前
        return -80;
    }

    // ========== 内部数据类 ==========

    enum TenantStatus {
        ACTIVE, SUSPENDED, EXPIRED, TRIAL
    }

    enum TenantTier {
        SHARED, SCHEMA, DATABASE, DEDICATED
    }

    static class TenantInfo {
        private String id;
        private String name;
        private TenantTier tier;
        private TenantStatus status;
        private List<String> enabledFeatures;
        private String dbUrl;
        private String adminId;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public TenantTier getTier() { return tier; }
        public void setTier(TenantTier tier) { this.tier = tier; }
        public TenantStatus getStatus() { return status; }
        public void setStatus(TenantStatus status) { this.status = status; }
        public List<String> getEnabledFeatures() { return enabledFeatures; }
        public void setEnabledFeatures(List<String> enabledFeatures) { this.enabledFeatures = enabledFeatures; }
        public String getDbUrl() { return dbUrl; }
        public void setDbUrl(String dbUrl) { this.dbUrl = dbUrl; }
        public String getAdminId() { return adminId; }
        public void setAdminId(String adminId) { this.adminId = adminId; }

        public static TenantInfoBuilder builder() {
            return new TenantInfoBuilder();
        }

        public static class TenantInfoBuilder {
            private String id;
            private String name;
            private TenantTier tier;
            private TenantStatus status;
            private List<String> enabledFeatures;
            private String dbUrl;
            private String adminId;

            public TenantInfoBuilder id(String id) { this.id = id; return this; }
            public TenantInfoBuilder name(String name) { this.name = name; return this; }
            public TenantInfoBuilder tier(TenantTier tier) { this.tier = tier; return this; }
            public TenantInfoBuilder status(TenantStatus status) { this.status = status; return this; }
            public TenantInfoBuilder enabledFeatures(List<String> enabledFeatures) { this.enabledFeatures = enabledFeatures; return this; }
            public TenantInfoBuilder dbUrl(String dbUrl) { this.dbUrl = dbUrl; return this; }
            public TenantInfoBuilder adminId(String adminId) { this.adminId = adminId; return this; }

            public TenantInfo build() {
                TenantInfo info = new TenantInfo();
                info.id = this.id;
                info.name = this.name;
                info.tier = this.tier;
                info.status = this.status;
                info.enabledFeatures = this.enabledFeatures;
                info.dbUrl = this.dbUrl;
                info.adminId = this.adminId;
                return info;
            }
        }
    }
}