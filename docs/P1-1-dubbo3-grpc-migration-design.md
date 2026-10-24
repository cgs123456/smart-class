# P1-1: Dubbo3/gRPC 微服务通信迁移设计文档

## 1. 现状分析

### 当前架构
- **微服务数量**: 16 个模块
- **通信方式**: Feign (OpenFeign) + HTTP/JSON
- **服务发现**: Nacos
- **负载均衡**: Spring Cloud LoadBalancer (轮询/随机)
- **熔断/限流**: Sentinel (已在依赖管理中引入)
- **网关**: Spring Cloud Gateway

### 痛点
| 问题 | 影响 |
|------|------|
| HTTP/JSON 序列化开销大 | 高频调用延迟 5-20ms 额外开销 |
| 无强契约 | 接口变更靠文档/口头，运行时才报错 |
| 多语言扩展难 | 前端/移动端/Go 服务需手写客户端 |
| 连接复用差 | 短连接为主，高并发下连接数压力大 |
| 观测性弱 | 缺乏统一的调用链语义元数据 |

---

## 2. 技术选型对比

### 2.1 候选方案

| 维度 | **Dubbo3 Triple** | **gRPC 原生 + Spring Cloud** | **Feign 现状** |
|------|-------------------|------------------------------|----------------|
| **协议** | Triple (HTTP/2 + PB) | gRPC (HTTP/2 + PB) | HTTP/1.1 + JSON |
| **序列化** | Protobuf (默认) / Hessian2 | Protobuf | Jackson JSON |
| **IDL 管理** | `.proto` 文件，Maven 插件生成 | `.proto` 文件，protoc 插件 | 手写 Java Interface |
| **多语言** | Java/Go/Node/Python/Rust 官方支持 | 官方支持 10+ 语言 | 仅 Java |
| **服务治理** | 内置熔断/限流/路由/流量标记 | 需配合 Resilience4j/Envoy | Sentinel 外挂 |
| **注册中心** | Nacos/ZooKeeper/Etcd/Consul 原生 | 需额外适配 | Nacos 原生 |
| **Gateway 兼容** | Dubbo Gateway / Spring Cloud Gateway + Triple | Envoy / Spring Cloud Gateway + grpc-gateway | 原生支持 |
| **调试工具** | Dubbo Admin / dubbogo-cli / Postman (HTTP/2) | grpcurl / Postman / BloomRPC | curl / Postman 完美支持 |
| **学习曲线** | 中 (需学 Triple 语义) | 中高 (需学 gRPC 概念) | 低 |
| **生态成熟度** | Dubbo 3.2+ 生产就绪，大厂大规模验证 | CNCF 毕业项目，极其成熟 | Spring Cloud 核心组件 |

### 2.2 推荐结论：选择 **Dubbo3 Triple 协议**

**理由**：
1. **与 Nacos 深度集成** - 无需额外组件，复用现有注册中心
2. **服务治理内置** - 熔断/限流/路由规则统一在框架层，替代 Sentinel
3. **Spring Boot 3 原生支持** - `dubbo-spring-boot-starter` 零配置接入
4. **Triple 协议兼容 gRPC** - 可与 gRPC 客户端互通，未来可平滑迁移到纯 gRPC
5. **阿里巴巴/字节/网易 万级服务验证** - 生产可靠性有保障

---

## 3. 迁移策略：Strangler Fig 渐进式双写

### 3.1 核心原则
- **不停机**：新旧并行，流量逐步切换
- **可回滚**：任意阶段可秒级回滚到 Feign
- **契约先行**：先定 `.proto`，再生成代码，最后实现

### 3.2 分阶段迁移计划

```
Phase 0 (Week 1-2): 基建准备
├── 引入 dubbo-spring-boot-starter 3.2.x
├── 配置 Nacos 注册中心、Triple 协议端口 (默认 50051)
├── 建立 proto 仓库 (monorepo: smartclass-api-contracts)
├── 配置 maven-protoc-plugin / protobuf-maven-plugin
├── CI: proto 变更 -> 生成代码 -> 发布到私有 Maven 仓库
└── 建立 Contract Testing 流程 (Pact + Dubbo)

Phase 1 (Week 3-4): 核心链路试点 - user + intelligence 服务
├── 选取 2-3 个高频、稳定、无循环依赖的接口
├── 定义 .proto，生成 Java Stub
├── Provider 端：实现 Dubbo 服务，同时保留 Feign Controller
├── Consumer 端：引入生成的 Stub，通过 @DubboReference 注入
├── 灰度：通过 Dubbo 路由规则 10% 流量走 Dubbo，90% 走 Feign
├── 观测：对比延迟、错误率、CPU/内存
└── 全量切换：路由规则 100% Dubbo，下线 Feign Controller

Phase 2 (Week 5-8): 批量迁移 - 按业务域分批
├── Batch A: user, circle, announcement (高频读、低耦合)
├── Batch B: course, dailyword, dailyarticle, learn-schedule (中频)
├── Batch C: stats, feedback, file, search, pay (低频/特殊)
├── 每批：定义 proto -> 生成 -> 双写 -> 灰度 -> 全量 -> 清理
└── 同步更新 Gateway 路由规则

Phase 3 (Week 9): 网关层适配
├── 方案 A: Spring Cloud Gateway + dubbo-triple-gateway-filter (推荐)
├── 方案 B: Dubbo Gateway (独立部署，功能更全)
├── 实现：HTTP/JSON -> Triple 转发，保持对前端兼容
└── 熔断/限流/日志迁移到 Dubbo 治理规则

Phase 4 (Week 10): 清理与优化
├── 移除所有 Feign 依赖、OpenFeign 注解
├── 移除 Sentinel 依赖 (改用 Dubbo 内置)
├── 性能调优：连接池、线程池、序列化优化
├── 文档完善：API 文档自动生成 (proto -> Markdown)
└── 团队培训、运维手册
```

### 3.3 双写模式实现细节

```java
// Provider 端：同时暴露 Feign Controller 和 Dubbo Service
@RestController
@RequestMapping("/api/user")
@DubboService(version = "1.0.0", protocol = "triple")
public class UserController implements UserService {
    
    @Autowired private UserServiceImpl userService;
    
    // Feign 入口 (保留给旧消费者)
    @GetMapping("/{id}")
    public UserDTO getById(@PathVariable Long id) {
        return userService.getById(id);
    }
    
    // Dubbo 入口 (新消费者)
    @Override
    public UserDTO getById(Long id) {
        return userService.getById(id);
    }
}

// Consumer 端：通过 Feature Flag 切换
@Service
public class UserClient {
    
    @Value("${dubbo.migration.user.enabled:false}")
    private boolean useDubbo;
    
    @DubboReference(version = "1.0.0", check = false)
    private UserService dubboUserService;
    
    @Autowired(required = false)
    private UserFeignClient feignUserClient; // 旧客户端
    
    public UserDTO getById(Long id) {
        if (useDubbo && dubboUserService != null) {
            return dubboUserService.getById(id);
        }
        return feignUserClient.getById(id);
    }
}
```

---

## 4. Protobuf IDL 管理方案

### 4.1 仓库结构
```
smartclass-api-contracts/
├── pom.xml                          # 父工程，配置 protobuf-maven-plugin
├── user-api/
│   ├── src/main/proto/
│   │   ├── user.proto
│   │   └── common.proto
│   └── pom.xml
├── course-api/
├── circle-api/
├── intelligence-api/
├── common-protos/                   # 共享类型: PageRequest, BaseResponse, ErrorCode
└── boms/
    └── api-contracts-bom/           # 版本锁定 BOM
```

### 4.2 版本发布流程
```yaml
# GitHub Actions / GitLab CI
on:
  push:
    tags: ['v*']
jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - name: Build & Install
        run: mvn -B clean deploy -DskipTests
      - name: Generate Docs
        run: |
          # 使用 protoc-gen-doc 生成 Markdown
          # 发布到 GitHub Pages / Confluence
```

### 4.3 消费端引入
```xml
<!-- 统一版本管理 -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.smartclass.smartclass.contracts</groupId>
      <artifactId>api-contracts-bom</artifactId>
      <version>${api.contracts.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<!-- 具体服务 -->
<dependency>
  <groupId>com.smartclass.smartclass.contracts</groupId>
  <artifactId>user-api</artifactId>
</dependency>
```

---

## 5. 服务治理迁移：Sentinel -> Dubbo 内置

| Sentinel 功能 | Dubbo 替代方案 |
|--------------|----------------|
| 流控 (QPS/线程数) | `dubbo.ratelimit.key` + `dubbo.ratelimit.limit` 配置或规则中心 |
| 熔断降级 | `dubbo.circuitbreaker` 配置 (基于 Resilience4j 实现) |
| 热点参数限流 | Dubbo 3.2+ 支持参数级流控 |
| 系统自适应保护 | Dubbo 自适应限流 (CPU/Load/RT) |
| 授权规则 | Dubbo 标签路由 + 访问控制 |
| 实时监控 | Dubbo Admin + Metrics 推送到 Prometheus |

**配置示例** (Nacos 配置中心动态下发)：
```yaml
dubbo:
  governance:
    ratelimit:
      - service: com.smartclass.smartclass.user.UserService
        method: getById
        limit: 1000
        key: "header:userId"
    circuitbreaker:
      - service: com.smartclass.smartclass.intelligence.AiChatService
        threshold: 50%
        minRequests: 20
        waitTime: 30s
```

---

## 6. Gateway 层适配方案

### 方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **Spring Cloud Gateway + Triple Filter** | 复用现有 Gateway、配置熟悉、统一运维 | 需开发/维护 Filter、性能有损耗 | 渐进式迁移、团队熟悉 SCG |
| **Dubbo Gateway (独立)** | 功能全(聚合/编排/协议转换)、性能更好、原生治理 | 新组件、需单独部署运维、学习成本 | 全新建设、高性能要求 |

### 推荐：方案 A (SCG + Triple Filter) 作为过渡，长期评估方案 B

**关键实现**：
```java
@Component
public class TripleGatewayFilter implements GatewayFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. 从路由元数据获取目标 Dubbo 服务信息
        // 2. HTTP/JSON -> Protobuf 序列化
        // 3. 通过 Dubbo Triple Client 调用
        // 4. Protobuf -> HTTP/JSON 返回
        // 5. 透传 traceId、spanId、用户上下文
    }
}
```

---

## 7. 测试策略

| 测试层级 | 工具/方法 | 执行时机 |
|----------|-----------|----------|
| **Contract Testing** | Pact + Dubbo Provider Verification | 每次提交 proto 变更 |
| **单元测试** | JUnit + Mockito (Mock Stub) | CI 每次构建 |
| **集成测试** | Testcontainers (Nacos + MySQL + Redis) | PR 合并前 |
| **性能基准** | JMH / wrk / ghz (gRPC 负载测试) | 发布前 |
| **混沌工程** | Chaos Mesh / Litmus (网络分区/延迟/错误注入) | 预发环境定期 |

---

## 8. 风险与缓解

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| Triple 协议兼容性问题 | 中 | 高 | 先在测试环境跑全量回归；保留 Feign 回滚通道 |
| Proto 变更破坏兼容性 | 高 | 高 | 强制向后兼容规则：只加字段、不删字段、不改 tag；CI 校验 |
| 团队学习曲线陡峭 | 高 | 中 | 内部分享、编写最佳实践文档、结对编程前 2 周 |
| 网关转发性能损耗 | 中 | 中 | 开启 HTTP/2、连接池复用、异步非阻塞；压测验证 |
| 现有 Sentinel 规则迁移遗漏 | 中 | 高 | 梳理现有规则清单，逐条对应 Dubbo 配置，灰度期双写 |
| Nacos 双注册冲突 | 低 | 高 | Dubbo 只注册元数据，不注册实例；或用同一 Nacos namespace 区分 |

---

## 9. 工作量预估

| 活动 | 人天 | 备注 |
|------|------|------|
| 基建搭建 (proto 仓库、CI、插件配置) | 3 | 1 人 |
| 核心链路试点 (user + intelligence) | 5 | 2 人并行 |
| 批量迁移 14 个服务 (约 80 个接口) | 20 | 2-3 人并行 |
| Gateway 适配 + 治理规则迁移 | 5 | 1 人 |
| Contract Testing 流程建设 | 3 | 1 人 |
| 文档、培训、运维手册 | 3 | 1 人 |
| **合计** | **~39 人天** | **约 8-10 周 (2-3 人并行)** |

---

## 10. 里程碑

| 里程碑 | 目标日期 | 验收标准 |
|--------|----------|----------|
| M1: 基建就绪 | Week 2 结束 | proto 仓库可自动发布、消费端可引入生成代码 |
| M2: 试点全量切换 | Week 4 结束 | user/intelligence 服务 100% Dubbo 调用，延迟降低 30%+ |
| M3: 核心域迁移完成 | Week 8 结束 | 10+ 服务迁移完成，Feign 依赖移除 50%+ |
| M4: 全量迁移完成 | Week 10 结束 | 0 Feign 调用，Sentinel 完全移除，性能基线达标 |

---

## 11. 决策建议

**建议立即启动 Phase 0 基建**，理由：
- 风险可控（Strangler Fig 保证可回滚）
- 收益明确（延迟降低、契约治理、多语言解锁）
- 为后续 P1-2 Gateway、P1-3 多租户打下统一通信基础

**需要团队确认**：
1. 是否接受引入 Protobuf 作为新的契约语言？
2. 是否有运维能力维护 Dubbo Admin / 规则中心？
3. 前端/移动端是否需要同步生成 TS/Swift 客户端？（Dubbo 支持，但需额外工作量）