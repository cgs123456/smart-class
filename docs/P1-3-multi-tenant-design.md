# P1-3: 多租户架构准备设计文档

## 1. 业务背景与驱动因素

### 1.1 为什么要多租户？
| 场景 | 当前痛点 | 多租户价值 |
|------|----------|------------|
| **SaaS 化交付** | 单套代码部署多套环境，配置维护地狱 | 单实例多租户，配置隔离，边际成本趋零 |
| **教育局/学校级部署** | 数据混存，合规风险高 | 物理/逻辑隔离，满足等保/数据主权要求 |
| **合作伙伴白标** | 需 Fork 代码定制，升级困难 | 租户级主题/功能/品牌配置，核心代码不分叉 |
| **跨地域部署** | 数据跨境合规难 | 租户绑定数据区域，自动路由 |

### 1.2 目标租户模型
```
Platform (平台方)
├── Tenant A: 某市教育局 (独立部署/共享部署)
│   ├── School A1: 实验中学
│   ├── School A2: 第一小学
│   └── Teacher/Admin/Student (用户归属学校)
├── Tenant B: 某培训机构 (SaaS 共享)
│   ├── Campus B1: 总部校区
│   └── Campus B2: 分校区
└── Tenant C: 合作伙伴白标
    └── 自有品牌/域名/功能开关
```

---

## 2. 架构设计：三层隔离策略

### 2.1 隔离层级对比

| 维度 | **共享数据库 + 共享 Schema (Row-Level)** | **共享数据库 + 独立 Schema** | **独立数据库** |
|------|------------------------------------------|------------------------------|----------------|
| 隔离度 | 低 (逻辑隔离) | 中 (Schema 隔离) | 高 (物理隔离) |
| 资源利用率 | 最高 | 高 | 低 |
| 运维复杂度 | 低 | 中 | 高 |
| 数据迁移/备份 | 简单 | 中等 | 复杂 |
| 合规满足度 | 一般 | 较好 | 最好 |
| 扩展性 | 受限 | 良好 | 最佳 |
| **推荐场景** | 中小 SaaS 租户、数据量 < 100万 | 中型租户、有合规要求 | 大型客户、强合规、独立部署 |

### 2.2 推荐：混合模式 (按租户分级)

```
租户分级策略:
├── Tier 1: 共享库共享表 (Row-Level Security)
│   - 免费/基础版 SaaS 租户
│   - 数据量小、无强合规要求
│   - 成本最低，密度最高
├── Tier 2: 共享库独立 Schema
│   - 标准版/专业版 SaaS 租户
│   - 需要独立备份/恢复、部分定制
├── Tier 3: 独立数据库 (可独立部署)
│   - 旗舰版/教育局/大型机构
│   - 数据主权、等保三级、私有化部署
└── Tier 4: 完全独立部署 (K8s Namespace/Cluster)
    - 核心战略客户、白标合作伙伴
    - 代码级隔离、独立域名/SSL/品牌
```

---

## 3. 核心技术方案

### 3.1 租户识别与上下文传递

**识别来源优先级**：
```java
public enum TenantResolutionStrategy {
    HEADER("X-Tenant-Id"),           // 1. 网关注入 (最高优先级)
    SUBDOMAIN("tenant.smartclass.cn"), // 2. 子域名解析
    PATH("/t/{tenantId}/**"),         // 3. URL 路径
    CLAIM("tenant_id"),               // 4. JWT Claim
    DEFAULT("default");               // 5. 默认租户 (开发/测试)
}
```

**上下文持有者** (ThreadLocal + Reactor Context 双模式)：
```java
public final class TenantContext {
    
    private static final ThreadLocal<TenantInfo> THREAD_LOCAL = new ThreadLocal<>();
    private static final String CONTEXT_KEY = "tenantInfo";
    
    public static void set(TenantInfo info) {
        THREAD_LOCAL.set(info);
        // Reactor 上下文传递 (WebFlux/Dubbo Triple)
        Context.of(CONTEXT_KEY, info);
    }
    
    public static TenantInfo get() {
        TenantInfo info = THREAD_LOCAL.get();
        if (info == null) {
            // 尝试从 Reactor Context 获取
            info = Context.getOrEmpty(CONTEXT_KEY).orElse(null);
        }
        return info != null ? info : TenantInfo.defaultTenant();
    }
    
    public static void clear() { THREAD_LOCAL.remove(); }
    
    // 自动清理切面
    @Component
    @Aspect
    public static class TenantContextCleanupAspect {
        @Around("@annotation(org.springframework.web.bind.annotation.RestController) || " +
                "@annotation(org.springframework.stereotype.Service)")
        public Object clean(ProceedingJoinPoint pjp) throws Throwable {
            try { return pjp.proceed(); }
            finally { TenantContext.clear(); }
        }
    }
}
```

**网关层注入** (P1-2 配合)：
```java
@Component
@Order(-90) // 紧跟鉴权后
public class TenantResolutionFilter implements GatewayFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tenantId = resolveTenantId(exchange); // Header/Subdomain/Path/JWT
        TenantInfo tenant = tenantRegistry.get(tenantId); // 缓存/DB 查询
        
        // 校验租户状态：ACTIVE/SUSPENDED/EXPIRED
        if (tenant == null || tenant.getStatus() != TenantStatus.ACTIVE) {
            return tenantNotFoundResponse(exchange);
        }
        
        // 注入 Header 透传给下游
        ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header("X-Tenant-Id", tenant.getId())
            .header("X-Tenant-Tier", tenant.getTier().name())
            .header("X-Tenant-Features", String.join(",", tenant.getEnabledFeatures()))
            .build();
        
        return chain.filter(exchange.mutate().request(mutated).build());
    }
}
```

### 3.2 数据隔离实现

#### 方案 A: MyBatis-Plus 租户插件 (Row-Level)
```java
@Configuration
public class MybatisPlusTenantConfig {
    
    @Bean
    public TenantLineHandler tenantLineHandler() {
        return new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(TenantContext.get().getId());
            }
            
            @Override
            public String getTenantIdColumn() {
                return "tenant_id"; // 统一列名
            }
            
            @Override
            public boolean ignoreTable(String tableName) {
                // 系统表、字典表、不需要隔离的表
                return Set.of("system_config", "dict_data", "tenant_info").contains(tableName);
            }
        };
    }
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantLineHandler handler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(handler));
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor()); // 防全表更新
        return interceptor;
    }
}
```

**实体类标准**：
```java
@Data
@TableName(value = "user", schema = "${tenant.schema:public}")
public class User {
    @TableId
    private Long id;
    
    @TenantId // 自动填充
    private Long tenantId;
    
    // ...
}
```

#### 方案 B: 独立 Schema / 数据库 - AbstractRoutingDataSource
```java
@Configuration
public class MultiTenantDataSourceConfig {
    
    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties props, TenantRegistry registry) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        
        // 默认共享库
        targetDataSources.put("default", createSharedDataSource(props));
        
        // Tier 2/3 租户独立库
        registry.getTenantsByTier(TenantTier.SCHEMA, TenantTier.DATABASE)
            .forEach(tenant -> targetDataSources.put(tenant.getId(), 
                createTenantDataSource(tenant)));
        
        AbstractRoutingDataSource routingDs = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                return TenantContext.get().getId(); // 租户 ID 作为 Key
            }
        };
        routingDs.setTargetDataSources(targetDataSources);
        routingDs.setDefaultTargetDataSource(targetDataSources.get("default"));
        return routingDs;
    }
}
```

#### 方案 C: Flyway 多租户迁移 (P0-4 配合)
```java
@Component
public class MultiTenantFlywayMigration implements CommandLineRunner {
    
    @Autowired private TenantRegistry tenantRegistry;
    @Autowired private DataSource dataSource;
    
    @Override
    public void run(String... args) {
        // 1. 先迁移共享库 (公共表、系统表)
        migrateSharedDatabase();
        
        // 2. 并行迁移每个租户库
        tenantRegistry.getTenantsByTier(TenantTier.SCHEMA, TenantTier.DATABASE)
            .parallelStream()
            .forEach(this::migrateTenantDatabase);
    }
    
    private void migrateTenantDatabase(TenantInfo tenant) {
        Flyway flyway = Flyway.configure()
            .dataSource(tenant.getDataSource())
            .locations("classpath:db/migration/tenant") // 租户专用迁移脚本
            .baselineOnMigrate(true)
            .table("flyway_schema_history_" + tenant.getId())
            .load();
        flyway.migrate();
    }
}
```

---

## 4. 租户管理域模型

### 4.1 核心实体
```java
@Entity
@Table(name = "tenant_info")
public class TenantInfo {
    @Id
    private String id;                    // 租户唯一标识 (如: edu-beijing, corp-abc)
    
    private String name;                  // 显示名称
    private String code;                  // 编码 (用于子域名)
    
    @Enumerated(EnumType.STRING)
    private TenantTier tier;              // SHARED / SCHEMA / DATABASE / DEDICATED
    
    @Enumerated(EnumType.STRING)
    private TenantStatus status;          // ACTIVE / SUSPENDED / EXPIRED / TRIAL
    
    private String adminId;               // 租户管理员用户 ID
    
    // 数据库连接配置 (Tier 2/3 使用)
    private String dbUrl;
    private String dbUsername;
    private String dbPassword;            // 加密存储
    
    // 功能开关 (Feature Flags)
    @ElementCollection
    @CollectionTable(name = "tenant_features")
    private Set<String> enabledFeatures;  // AI_CHAT, RAG, COURSE_MARKET, PAYMENT...
    
    // 品牌定制
    @Embedded
    private TenantBranding branding;      // logo, colors, domain, favicon
    
    // 资源配额
    @Embedded
    private TenantQuota quota;            // maxUsers, maxStorage, apiQps...
    
    // 审计字段
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;      // 订阅到期时间
}
```

### 4.2 租户注册表 (缓存 + DB 双层)
```java
@Service
public class TenantRegistry {
    
    private final Cache<String, TenantInfo> cache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(10))
        .build();
    
    public TenantInfo get(String tenantId) {
        return cache.get(tenantId, id -> tenantRepository.findById(id)
            .orElseThrow(() -> new TenantNotFoundException(id)));
    }
    
    public void refresh(String tenantId) {
        cache.invalidate(tenantId);
        get(tenantId); // 重新加载
    }
    
    // 启动预热
    @PostConstruct
    public void warmup() {
        tenantRepository.findAllByStatus(TenantStatus.ACTIVE)
            .forEach(t -> cache.put(t.getId(), t));
    }
}
```

---

## 5. 功能开关

```java
public enum TenantFeature {
    // 核心功能
    AI_CHAT("AI 对话", "intelligence"),
    RAG_SEARCH("RAG 检索", "intelligence"),
    COURSE_MARKET("课程市场", "course"),
    COURSE_CREATE("创建课程", "course"),
    LIVE_TEACHING("直播教学", "course"),
    
    // 社交功能
    CIRCLE("圈子社区", "circle"),
    PRIVATE_CHAT("私信", "chat"),
    
    // 运营功能
    PAYMENT("支付/订阅", "pay"),
    COUPON("优惠券", "pay"),
    ACHIEVEMENT("成就系统", "stats"),
    
    // 高级功能
    WHITE_LABEL("白标定制", "platform"),
    CUSTOM_DOMAIN("自定义域名", "platform"),
    SSO("单点登录", "platform"),
    AUDIT_LOG("审计日志", "platform"),
    
    // 数据功能
    DATA_EXPORT("数据导出", "stats"),
    BI_REPORT("BI 报表", "stats");
}
```

**使用方式**：
```java
@PreAuthorize("@tenantFeatureChecker.hasFeature('AI_CHAT')")
@GetMapping("/ai/chat")
public Flux<ChatResponse> chat() { ... }

// 或编程式
if (!tenantFeatureChecker.hasFeature(TenantFeature.RAG_SEARCH)) {
    throw new FeatureDisabledException("RAG_SEARCH");
}
```

---

## 6. 迁移路线图

| 阶段 | 时间 | 范围 | 关键产出 |
|------|------|------|----------|
| **Phase 0** | Week 1 | 基础设施 | TenantContext、TenantRegistry、租户识别 Filter |
| **Phase 1** | Week 2-3 | Row-Level 隔离 | MyBatis-Plus Tenant 插件、所有表加 tenant_id、数据回填 |
| **Phase 2** | Week 4-5 | Schema/DB 隔离 | AbstractRoutingDataSource、Flyway 多租户迁移、Tier 2/3 租户上线 |
| **Phase 3** | Week 6 | 功能开关 + 配额 | Feature Flag 体系、配额校验、管理后台 |
| **Phase 4** | Week 7 | 白标/域名/SSO | 自定义域名路由、主题配置、SAML/OIDC 接入 |
| **Phase 5** | Week 8 | 测试/文档/演练 | 租户隔离渗透测试、数据泄露测试、运维手册 |

---

## 7. 关键风险与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| **跨租户数据泄露** | 极高 (合规/法律) | 1. 数据库层 RLS 强制策略 2. 代码层 TenantContext 强制校验 3. 定期渗透测试 4. 审计日志全覆盖 |
| **租户间性能干扰** | 高 | 1. 共享库配额限制 (连接数/CPU/IO) 2. 核心租户升级独立库 3. 熔断/限流按租户维度 |
| **Schema 变更同步失败** | 高 | 1. Flyway 版本强制对齐 2. 迁移前自动备份 3. 灰度发布租户组 |
| **租户上下文丢失 (异步/线程池)** | 中 | 1. Reactor Context 传递 2. 线程池装饰器自动传递 3. 切面兜底清理 |
| **运维复杂度爆炸** | 中 | 1. 租户级运维 API (备份/恢复/迁移) 2. 统一监控大盘按租户分组 3. 自动化巡检 |

---

## 8. 工作量预估

| 模块 | 人天 | 备注 |
|------|------|------|
| 租户上下文 + 识别 Filter | 3 | 核心基础设施 |
| MyBatis-Plus Row-Level 隔离 | 5 | 含现有数据回填脚本 |
| 多数据源路由 + Flyway 多租户 | 8 | 最复杂部分 |
| 功能开关 + 配额体系 | 4 | 管理后台联动 |
| 白标/域名/SSO | 6 | 涉及前端配合 |
| 测试 (隔离性/性能/渗透) | 5 | 专项测试 |
| **合计** | **~31 人天 (约 6-8 周, 2 人)** | |

---

## 9. 决策建议

**建议从 Phase 0+1 启动 (Row-Level 共享库模式)**：
- 成本最低，覆盖 90% 租户场景
- 代码改动最小，风险可控
- 为后续 Tier 2/3 奠基

**需要确认**：
1. 是否有现存大客户必须独立部署？(决定是否并行做 Tier 3)
2. 现有表结构是否允许加 `tenant_id` 非空列？(需评估数据回填工作量)
3. 是否需要支持租户自助注册/试用？(影响注册流程设计)