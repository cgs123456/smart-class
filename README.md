# 智云星课 - 智能学习平台

## 项目概述

智云星课是一个基于 Spring Boot 和 AI 技术的智能学习平台，集成 Dify AI 接口，支持 AI 聊天、课程学习、每日文章、单词记忆、社交圈子、好友私信等功能，为用户提供全方位的智能化学习体验。

系统采用前后端分离架构，包含四个子项目：后端单体服务、后端微服务版本、移动端前端、管理后台前端。后端提供单体和微服务两种部署模式，可根据业务规模灵活选择。

### 核心功能

- **AI 智能对话** - 集成 Dify AI，支持流式/阻塞式响应、多智能体切换、聊天历史管理、会话总结、多模态输入（文本/图片/文件）
- **课程学习** - 课程分类/章节/小节管理、学习进度跟踪、收藏与评价、课程表、课程资料管理
- **每日学习** - 每日文章推送、单词学习与记忆、生词本、学习打卡、个性化学习推荐
- **社交圈子** - 帖子发布、评论/回复、点赞/收藏、推荐/关注/热榜/问答分类
- **好友与私信** - 好友请求、私聊、SSE 实时消息推送、在线状态通知
- **用户体系** - 注册登录（含手机号验证码）、用户等级与成长体系、微信公众号集成
- **支付系统** - 微信支付/支付宝双渠道、订单管理、VIP 会员
- **管理后台** - 数据看板、用户管理、课程管理、单词库管理、每日美文管理、AI 分身助教管理、帖子管理、用户反馈管理

## 技术栈

### 后端

- **框架** - Spring Boot、Spring Cloud、Spring Cloud Alibaba（微服务版）
- **语言** - Java 17
- **ORM** - MyBatis Plus
- **搜索** - Spring Data Elasticsearch（CriteriaQuery API）
- **会话** - Spring Session + Redis（分布式会话）
- **AI 集成** - Dify AI API（OkHttp + SSE 流式响应）
- **API 文档** - Knife4j OpenAPI 3（Jakarta）
- **微信公众号** - WxJava
- **对象存储** - 腾讯云 COS
- **实时通信** - Netty WebSocket、SSE
- **安全** - Spring Security、JWT（jjwt）、BCrypt
- **监控** - Spring Boot Actuator
- **服务治理** - Nacos 服务发现、Spring Cloud LoadBalancer、Sentinel 熔断降级（微服务版）
- **工具库** - Hutool、fastjson2、EasyExcel、EasyCaptcha

### 移动端前端

- **框架** - Vue + TypeScript
- **UI 组件库** - Vant
- **状态管理** - Pinia
- **路由** - Vue Router
- **HTTP 请求** - Axios
- **构建工具** - Rsbuild（SWC 压缩）
- **跨端打包** - Capacitor（Android 原生）
- **接口生成** - openapi-typescript-codegen（从后端 OpenAPI 自动生成）
- **代码规范** - ESLint + Prettier + Biome

### 管理后台前端

- **框架** - React + TypeScript
- **UI 组件库** - Ant Design 5 + Ant Design Pro
- **应用框架** - UmiJS Max
- **数据可视化** - ECharts
- **样式方案** - Less + Emotion
- **接口生成** - OpenAPI 自动生成
- **国际化** - 简体中文、繁体中文

### 数据库与中间件

- **关系数据库** - MySQL
- **缓存** - Redis（分布式会话、数据缓存）
- **搜索引擎** - Elasticsearch
- **服务注册中心** - Nacos（微服务版）

### 开发与部署工具

- **构建** - Maven、npm
- **容器化** - Docker、Docker Compose
- **Web 服务器** - Nginx（反向代理、SSL 终止、静态资源服务）

## 项目架构

### 整体架构

```
                    Nginx 反向代理 (HTTPS)
                    ┌──────────────────────────┐
                    │     80 -> 443 强制跳转      │
                    │     SSL + 安全响应头         │
                    └──────────┬───────────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
     移动端前端 (Vue)    管理后台前端 (React)   API 网关
     Nginx 静态服务       Nginx 静态服务      (微服务版)
              │                │                │
              └────────────────┼────────────────┘
                               │
                    ┌──────────┴───────────────┐
                    │                          │
              单体后端服务               微服务后端集群
              Spring Boot            Spring Cloud Gateway
              端口 12345              端口 8101
                    │                    │
                    │           ┌────────┼────────┐
                    │           │        │        │
                    │        user(10001) circle  intelligence
                    │           │     (10003)  (10008)
                    │           │        │        │
                    └─────┬─────┴────────┴────────┘
                          │
              ┌───────────┼───────────┐
              │           │           │
           MySQL        Redis     Elasticsearch
```

### 后端两种部署模式

**单体模式**（smartclass-backend）适合快速开发和中小规模部署，所有业务逻辑在一个 Spring Boot 应用中，通过 Docker Compose 编排应用、MySQL、Redis、Elasticsearch。

**微服务模式**（smartclass-backend-microservice）适合大规模生产部署，按业务域拆分为 14 个微服务，通过 Spring Cloud Gateway 统一入口，Nacos 服务发现，Feign 跨服务调用，Redis 共享分布式 Session。

### 微服务拆分

| 服务 | 端口 | 职责 |
|------|------|------|
| gateway | 8101 | API 网关，统一鉴权、路由转发、CORS、文档聚合 |
| user | 10001 | 用户注册/登录、JWT 签发、用户 CRUD |
| announcement | 10002 | 公告发布与已读管理 |
| circle | 10003 | 帖子/评论/点赞/收藏、好友、私信、WebSocket |
| dailyarticle | 10004 | 每日文章推送、点赞、收藏、阅读记录 |
| dailyword | 10005 | 每日单词、生词本、学习记录 |
| feedback | 10006 | 用户反馈与回复管理 |
| file | 10007 | 文件上传（腾讯 COS） |
| intelligence | 10008 | AI 数字人对话（Dify 对接）、聊天历史 |
| course | 10009 | 课程/章节/小节/资料/评价、教师管理 |
| pay | 10011 | 商品、支付订单、回调 |
| search | 10012 | Elasticsearch 搜索（文章/单词/帖子） |
| stats | 10013 | 学习统计 |
| learn-schedule | 10014 | 学习计划管理 |

### 数据流

1. 客户端请求通过 Nginx SSL 终止后转发至后端
2. 单体模式直接到达 Spring Boot 应用；微服务模式先经网关鉴权
3. 网关解析 JWT，将 userId/userRole 注入请求头转发至下游服务
4. 下游服务通过 `@AuthCheck` AOP 切面做方法级权限校验
5. 业务数据持久化至 MySQL，热点数据缓存至 Redis，全文检索走 Elasticsearch
6. AI 对话通过 OkHttp 调用 Dify 平台 API，支持 SSE 流式响应
7. 实时消息通过 Netty WebSocket 长连接推送

## 目录结构

```
智云星课/
├── smartclass-backend/                    # 后端单体服务
├── smartclass-backend-microservice/       # 后端微服务版本
├── smartclass-frontend/                   # 移动端前端
├── smartclass-manage-frontend/            # 管理后台前端
└── README.md
```

### 后端单体服务（smartclass-backend）

```
smartclass-backend/
├── sql/                                   # 数据库脚本
│   ├── create_table.sql                   # 基础建表脚本
│   ├── p2_features_tables.sql             # P2 阶段功能表
│   ├── payment_tables.sql                 # 支付相关表
│   └── post_es_mapping.json               # Elasticsearch 帖子索引映射
├── src/main/java/com/cgs/smartclass/
│   ├── SmartClassApplication.java         # 主启动类
│   ├── annotation/
│   │   └── AuthCheck.java                 # 权限校验注解（支持单/多角色）
│   ├── aop/
│   │   ├── AuthInterceptor.java           # 权限拦截切面
│   │   └── LogInterceptor.java            # 请求日志切面（含敏感字段脱敏）
│   ├── common/                            # 通用响应类
│   │   ├── BaseResponse.java              # 统一响应封装
│   │   ├── ErrorCode.java                 # 错误码枚举
│   │   ├── PageRequest.java               # 分页请求基类
│   │   └── ResultUtils.java               # 响应工具类
│   ├── config/                            # 配置类（13 个）
│   │   ├── CorsConfig.java                # 跨域配置（生产强制校验）
│   │   ├── JwtConfig.java                 # JWT 安全配置（启动校验密钥）
│   │   ├── SecurityConfig.java            # Spring Security 配置
│   │   ├── MyBatisPlusConfig.java         # MyBatis-Plus 分页（含 setMaxLimit）
│   │   ├── DifyConfig.java                # Dify AI 配置
│   │   ├── RedisConfig.java               # Redis 序列化配置
│   │   ├── SseConfig.java                 # SSE 流式配置
│   │   ├── NettyWebSocketConfig.java      # WebSocket 配置
│   │   ├── TraceIdFilter.java             # 链路追踪过滤器
│   │   └── ...
│   ├── controller/                        # 控制器（43 个）
│   ├── filter/
│   │   ├── JwtAuthenticationFilter.java   # JWT 认证过滤器
│   │   ├── RateLimitFilter.java           # 限流过滤器
│   │   └── XssFilter.java                # XSS 防护过滤器
│   ├── netty/                             # WebSocket 服务
│   │   ├── NettyWebSocketServer.java      # WebSocket 服务器
│   │   ├── WebSocketAuthHandler.java      # 认证处理器（双模式 + 超时熔断）
│   │   ├── WebSocketMessageHandler.java   # 消息处理器
│   │   ├── ChannelManager.java            # 连接管理
│   │   └── HeartbeatHandler.java          # 心跳处理
│   ├── manager/
│   │   └── CosManager.java                # 腾讯云 COS（自定义线程池）
│   ├── mapper/                            # MyBatis Mapper（41 个接口）
│   ├── model/
│   │   ├── entity/                        # 实体类（39 个）
│   │   ├── dto/                           # 数据传输对象
│   │   ├── vo/                            # 视图对象（34 个）
│   │   └── enums/                         # 枚举
│   ├── service/                           # 服务层
│   │   ├── dify/                          # Dify AI 服务（6 个）
│   │   └── impl/                          # 实现类（46 个）
│   ├── esdao/                             # Elasticsearch DAO（3 个）
│   ├── exception/                         # 全局异常处理
│   └── utils/                             # 工具类（9 个）
├── src/main/resources/
│   ├── application.yml                    # 主配置（默认 prod 环境）
│   ├── application-dev.yml                # 开发环境配置
│   ├── application-prod.yml                # 生产环境配置
│   ├── application-test.yml               # 测试环境配置
│   ├── mapper/                            # MyBatis XML（30 个）
│   └── templates/                         # FreeMarker 代码生成模板
├── src/test/java/                         # 测试代码（含代码生成器）
├── .env.example                           # 环境变量模板
├── Dockerfile                             # 多阶段 Docker 构建
├── docker-compose.yml                     # 服务编排
└── pom.xml                                # Maven 依赖配置
```

### 后端微服务版本（smartclass-backend-microservice）

```
smartclass-backend-microservice/
├── pom.xml                                # 父 POM（聚合 16 个模块）
├── Dockerfile                             # 通用多阶段构建模板
├── docker-compose.yml                     # 全量服务编排
├── sql/
│   └── learn_schedule_tables.sql          # 学习计划建表脚本
├── smartclass-backend-common/             # 公共模块（自动配置）
│   └── src/main/java/.../common/
│       ├── annotation/AuthCheck.java      # 权限校验注解
│       ├── aop/AuthInterceptor.java       # 权限拦截切面
│       ├── common/                        # 通用响应与错误码
│       ├── config/SmartclassCommonAutoConfiguration.java
│       ├── constant/                      # 常量定义
│       └── exception/                     # 全局异常处理
├── smartclass-backend-model/              # 实体与 DTO 模块
│   └── src/main/java/.../model/
│       ├── entity/                        # 实体类
│       ├── dto/                           # 请求 DTO（按子域分包）
│       ├── vo/                            # 返回 VO
│       ├── enums/                        # 枚举
│       └── esdao/                         # Elasticsearch DAO
├── smartclass-backend-service-client/     # Feign 客户端模块
│   └── src/main/java/.../serviceclient/
│       ├── config/FeignInternalCallInterceptor.java  # 内部调用拦截器
│       └── service/                      # Feign 客户端接口
│           ├── UserFeignClient.java       # 用户服务客户端
│           ├── FileFeignClient.java       # 文件服务客户端
│           └── WebSocketNotificationService.java
├── smartclass-backend-gateway/           # API 网关
│   └── src/main/java/.../gateway/
│       ├── config/GatewayJwtUtil.java    # JWT 工具（仅解析，启动校验密钥）
│       ├── config/CorsConfig.java         # 跨域配置
│       └── filter/JwtAuthGlobalFilter.java  # 全局鉴权过滤器
├── smartclass-backend-user/              # 用户服务（端口 10001）
├── smartclass-backend-announcement/      # 公告服务（端口 10002）
├── smartclass-backend-circle/            # 圈子服务（端口 10003，含 WebSocket）
├── smartclass-backend-dailyarticle/      # 每日文章（端口 10004）
├── smartclass-backend-dailyword/         # 每日单词（端口 10005）
├── smartclass-backend-feedback/          # 用户反馈（端口 10006）
├── smartclass-backend-file/              # 文件服务（端口 10007）
├── smartclass-backend-intelligence/      # AI 智能服务（端口 10008）
├── smartclass-backend-course/            # 课程服务（端口 10009）
├── smartclass-backend-pay/               # 支付服务（端口 10011）
├── smartclass-backend-search/            # 搜索服务（端口 10012）
├── smartclass-backend-stats/             # 统计服务（端口 10013）
└── smartclass-backend-learn-schedule/    # 学习计划（端口 10014）
```

### 移动端前端（smartclass-frontend）

```
smartclass-frontend/
├── android/                               # Capacitor Android 原生工程
│   └── app/
│       ├── build.gradle                   # Android 构建配置（含混淆）
│       ├── proguard-rules.pro             # 代码混淆规则
│       └── src/main/AndroidManifest.xml
├── src/
│   ├── main.ts                            # 应用入口（Pinia/Router/Vant/Lazyload）
│   ├── App.vue                            # 根组件（5 tab + 全局字号控制）
│   ├── api/
│   │   └── index.ts                       # Axios 实例 + 401 拦截器
│   ├── capacitor/
│   │   └── index.ts                       # Capacitor 原生能力封装
│   ├── components/                        # 业务组件库（按域划分）
│   │   ├── Chat/                          # 聊天域
│   │   ├── Circle/                        # 圈子域（帖子/评论 8 个组件）
│   │   ├── Common/                        # 通用组件（含二维码扫描）
│   │   ├── Course/                        # 课程域
│   │   ├── Dialogue/                      # 对话域（AI + 好友，12 个组件）
│   │   ├── Home/                          # 首页域（8 个组件）
│   │   ├── Profile/                       # 个人中心域（10 个组件）
│   │   └── Search/                        # 搜索结果域
│   ├── router/
│   │   └── index.ts                       # 路由配置（全懒加载 + 鉴权守卫）
│   ├── services/                          # OpenAPI 自动生成层
│   │   ├── core/
│   │   │   ├── OpenAPI.ts                 # OpenAPI 全局配置
│   │   │   └── request.ts                 # 请求核心 + 401 拦截器
│   │   ├── models/                        # 类型定义（180+ 个）
│   │   └── services/                      # ControllerService（36 个）
│   ├── stores/                            # Pinia 状态管理
│   │   ├── userStore.ts                   # 用户状态（登录/登出/401 处理）
│   │   ├── settingsStore.ts              # 设置（字号/位置）
│   │   ├── searchStore.ts                 # 搜索状态
│   │   └── collectedWordsStore.ts        # 生词本
│   ├── views/                             # 页面级视图
│   │   ├── home/                          # 首页（文章/词汇）
│   │   ├── chat/                          # 聊天（AI 对话/好友）
│   │   ├── course/                        # 课程
│   │   ├── circle/                       # 圈子
│   │   ├── my-profile/                   # 个人中心（设置/成就）
│   │   └── user/                          # 登录/注册
│   └── utils/                             # 工具函数
├── .env.development                       # 开发环境变量
├── .env.production                        # 生产环境变量（HTTPS）
├── capacitor.config.ts                    # Capacitor 配置
├── rsbuild.config.ts                      # 构建配置（SWC 压缩去 console.log）
├── nginx.conf                             # Nginx 配置（HSTS/安全头）
├── Dockerfile                             # 多阶段构建 + 非 root 运行
├── docker-compose.yml                     # 服务编排
└── package.json
```

### 管理后台前端（smartclass-manage-frontend）

```
smartclass-manage-frontend/
├── config/                                # 构建配置
│   ├── config.ts                          # Umi Max 主配置
│   ├── routes.ts                          # 路由表（含权限守卫）
│   ├── proxy.ts                           # 开发代理
│   └── defaultSettings.ts                 # ProLayout 设置
├── src/
│   ├── app.tsx                            # 运行时入口（ErrorBoundary 包裹）
│   ├── access.ts                          # 权限定义（canUser/canAdmin）
│   ├── requestConfig.ts                   # 请求配置（CSRF 防护）
│   ├── requestErrorConfig.ts              # 错误处理（ErrorShowType 分级）
│   ├── components/
│   │   ├── ErrorBoundary.tsx              # React 错误边界
│   │   ├── Footer/
│   │   └── RightContent/                 # 顶栏头像下拉
│   ├── constants/
│   │   └── index.ts                       # 后端地址常量（环境变量注入）
│   ├── locales/                           # 国际化（简体中文 + 繁体中文）
│   ├── pages/
│   │   ├── DataPanel.tsx                  # 数据看板（ECharts 可视化）
│   │   ├── Admin/                         # 管理后台业务
│   │   │   ├── User/                      # 用户管理
│   │   │   ├── Course/                    # 课程管理
│   │   │   ├── AiAvatar/                  # AI 分身管理（含统计）
│   │   │   ├── DailyWord/                 # 每日单词管理（含批量导入）
│   │   │   ├── DailyArticle/              # 每日美文管理
│   │   │   ├── Post/                      # 帖子管理
│   │   │   ├── Feedback/                  # 用户反馈管理
│   │   │   └── Class/                     # 班级管理
│   │   ├── DailyArticle/                  # 美文展示端（含 XSS 防护）
│   │   └── User/                          # 登录/注册（含开放重定向防护）
│   └── services/                          # 接口层（OpenAPI 自动生成 30+ Controller）
├── .env.production                        # 生产环境变量
├── nginx.conf                             # Nginx 配置（HTTPS + 安全响应头）
├── Dockerfile                             # 多阶段构建 + 非 root 运行
├── docker-compose.yml                     # 服务编排
└── package.json
```

## 核心文件说明

### 后端单体服务

| 文件 | 说明 |
|------|------|
| `SmartClassApplication.java` | 主启动类，启用 MapperScan、异步、定时任务、AOP |
| `application.yml` | 主配置，默认 prod 环境，集成 MySQL/Redis/ES/微信/COS/Dify |
| `SecurityConfig.java` | Spring Security 配置，默认拒绝所有请求，仅白名单放行 |
| `JwtConfig.java` | JWT 配置，启动时校验密钥长度和默认值 |
| `CorsConfig.java` | 跨域配置，生产环境必须显式配置允许来源 |
| `JwtAuthenticationFilter.java` | JWT 认证过滤器，解析 Token 并注入 SecurityContext |
| `XssFilter.java` | 全局 XSS 过滤器，惰性转义 QueryString 和表单参数 |
| `RateLimitFilter.java` | 限流过滤器 |
| `LogInterceptor.java` | 请求日志切面，16 类敏感字段脱敏，超长参数截断 |
| `AuthCheck.java` | 权限注解，支持 mustRole 单角色和 mustRoles 多角色 |
| `AuthInterceptor.java` | 权限切面，优先校验多角色，兼容单角色 |
| `WebSocketAuthHandler.java` | WebSocket 认证，双模式（URL token + 消息认证），10 秒超时 |
| `CosManager.java` | 腾讯云 COS 管理，自定义线程池（有界队列 + CallerRunsPolicy） |
| `PaymentOrderServiceImpl.java` | 支付服务，微信 HMAC-SHA256 + 支付宝 RSA-SHA256 验签 |
| `ErrorCode.java` | 统一错误码枚举 |

### 后端微服务版本

| 文件 | 说明 |
|------|------|
| `pom.xml` | 父 POM，聚合 16 个模块，统一版本管理 |
| `GatewayJwtUtil.java` | 网关 JWT 工具，仅解析不生成，@PostConstruct 校验密钥 |
| `JwtAuthGlobalFilter.java` | 网关核心过滤器，拦截内部接口、白名单放行、注入用户信息 |
| `JwtUtil.java`（user 服务） | JWT 签发工具，与网关共享密钥 |
| `FeignInternalCallInterceptor.java` | Feign 拦截器，注入 X-Internal-Call 头 |
| `SmartclassCommonAutoConfiguration.java` | 公共模块自动配置，注册 AuthInterceptor 和全局异常处理 |
| `AuthInterceptor.java` | 公共权限切面，兼容 Session 和网关切面两种场景 |
| `UserFeignClient.java` | 用户服务 Feign 客户端，混合本地逻辑与远程调用 |

### 移动端前端

| 文件 | 说明 |
|------|------|
| `main.ts` | 应用入口，注册 Pinia/Router/Vant/Lazyload，全局错误处理器 |
| `App.vue` | 根组件，5 tab 导航，全局字号控制（5 档可调） |
| `router/index.ts` | 路由配置，全懒加载，beforeEach 鉴权守卫 |
| `api/index.ts` | Axios 实例，请求注入 Bearer token，401 自动跳登录 |
| `request.ts` | OpenAPI 请求核心，全局 axios 401 拦截器 |
| `OpenAPI.ts` | OpenAPI 配置，BASE 从环境变量读取，开启 WITH_CREDENTIALS |
| `userStore.ts` | 用户状态管理，登录/登出/401 清空并跳转 |
| `rsbuild.config.ts` | 构建配置，SWC pure_funcs 去除 console.log |
| `capacitor.config.ts` | Capacitor 配置，强制 HTTPS，AAB 发布构建 |

### 管理后台前端

| 文件 | 说明 |
|------|------|
| `app.tsx` | 运行时入口，getInitialState 拉取用户信息，ErrorBoundary 包裹 |
| `access.ts` | 权限定义，canUser/canAdmin 基于用户角色 |
| `requestConfig.ts` | 请求配置，CSRF 双重提交防护，401 自动跳登录 |
| `requestErrorConfig.ts` | 错误处理，按 ErrorShowType 分级提示 |
| `ErrorBoundary.tsx` | React 错误边界，捕获渲染异常展示 500 页面 |
| `routes.ts` | 路由表，声明式权限守卫（access 字段） |
| `DataPanel.tsx` | 数据看板，ECharts 多维度可视化 |
| `Login/index.tsx` | 登录页，getSafeRedirect 防止开放重定向 |
| `DailyArticle/detail/index.tsx` | 美文详情，sanitizeHtml XSS 防护 |

## 安全特性

### 认证与授权

- JWT 密钥启动期强制校验（长度 >= 32 字节，拒绝已知默认值）
- Spring Security 默认拒绝策略，仅白名单放行
- `@AuthCheck` AOP 注解支持单角色和多角色校验
- 网关统一鉴权，用户信息透传下游服务（微服务版）
- Feign 内部调用注入 X-Internal-Call 头，外部请求拦截 /inner/** 路径
- WebSocket 双模式认证 + 10 秒认证超时自动断连
- BCrypt 密码加密，移除 MD5 + 固定盐值兼容代码

### 请求安全

- CORS 生产环境强制配置允许来源，未配置时启动失败
- XSS 过滤器对 QueryString、表单参数、危险 Header 做 HTML 转义
- CSRF 双重提交防护（Cookie csrfToken + X-CSRF-Token 请求头）
- 开放重定向防护（仅允许本站相对路径）
- 请求日志 16 类敏感字段脱敏
- 分页参数上限保护（setMaxLimit 100）

### 数据安全

- Session 不存储完整用户实体（写入前清除密码字段）
- User 实体 userPassword 字段 @JsonProperty(WRITE_ONLY)
- 支付回调 HMAC-SHA256 / RSA-SHA256 验签 + 时间戳防重放
- 敏感配置全部环境变量化（数据库密码、COS 密钥、微信密钥、JWT 密钥等）

### 部署安全

- Docker 非 root 用户运行
- 生产环境数据库/Redis/ES 端口不暴露宿主机
- MySQL 使用应用专用用户（非 root）
- Nginx 强制 HTTPS + HSTS + 安全响应头
- Knife4j 文档生产环境关闭
- spring-boot-devtools 从生产 JAR 排除
- Android 代码混淆 + 签名 + allowBackup=false

### 可观测性

- 请求链路追踪（TraceId + MDC）
- Spring Boot Actuator 监控（仅暴露 health/info）
- Feign 超时配置 + Sentinel 熔断降级（微服务版）
- 全局异常处理（参数校验/数据库/超时/解析错误）

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- Node.js 18.0+
- npm 9.0+

### 1. 后端启动

创建数据库并导入初始化脚本：

```sql
CREATE DATABASE smart_class DEFAULT CHARACTER SET utf8mb4;
```

```bash
mysql -u root -p smart_class < smartclass-backend/sql/create_table.sql
```

配置环境变量（参考 `.env.example`）：

```bash
export SPRING_PROFILES_ACTIVE=dev
export MYSQL_PASSWORD=your_password
export REDIS_PASSWORD=your_redis_password
export JWT_SECRET=your_jwt_secret_at_least_32_bytes
export COS_ACCESS_KEY=your_cos_key
export COS_SECRET_KEY=your_cos_secret
export WX_MP_TOKEN=your_wx_token
export WX_MP_AES_KEY=your_wx_aes_key
export WX_MP_APP_ID=your_wx_appid
export WX_MP_SECRET=your_wx_secret
```

启动后端（单体模式）：

```bash
cd smartclass-backend
mvn spring-boot:run
```

启动后端（微服务模式）：

```bash
cd smartclass-backend-microservice
# 先启动 Nacos，再依次启动各服务
mvn -pl smartclass-backend-gateway spring-boot:run
mvn -pl smartclass-backend-user spring-boot:run
# ... 其他服务
```

### 2. 移动端前端启动

```bash
cd smartclass-frontend
npm install
npm run dev
```

访问：`http://localhost:8080`

### 3. 管理后台启动

```bash
cd smartclass-manage-frontend
npm install
npm run start:dev
```

访问：`http://localhost:8000`

### 4. Docker 一键部署

```bash
# 单体后端
cd smartclass-backend
docker-compose up -d

# 或微服务后端
cd smartclass-backend-microservice
docker-compose up -d
```

## Dify AI 集成

系统通过 Dify API 实现智能对话功能。添加 AI 智能体的步骤：

1. 在 Dify 平台创建对话型应用并配置提示词
2. 发布智能体并获取访问 URL
3. 创建 API 密钥
4. 在管理后台「AI 分身管理」中添加智能体，配置 Base URL、API Key、App ID
5. 在移动端测试对话功能
