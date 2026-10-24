# P1-5: OpenAPI Generator 契约优先开发设计文档

## 1. 现状与痛点

### 当前接口开发模式
```
后端 (Java)                    前端 (TypeScript)
├── Controller 定义 @RequestMapping    ├── 手写 api/user.ts
├── DTO 类 (Lombok + Swagger 注解)     ├── 手写 types/user.ts
├── Swagger/OpenAPI 自动生成            ├── 手写拦截器、错误处理
└── 文档: /v3/api-docs                 └── 类型不一致、字段缺失、重命名烦人
```

### 核心痛点
| 问题 | 影响 |
|------|------|
| **双端类型不同步** | 后端改字段/枚举/必填，前端运行时才报错 |
| **手写样板代码多** | 每个接口需写：类型、请求函数、参数校验、错误处理 |
| **文档不可信** | Swagger UI 仅供参考，实际调用靠口头/Postman |
| **重构恐惧** | 重命名字段需全局搜索前后端，极易遗漏 |
| **多端复用难** | Mobile/Admin/小程序/第三方各自维护一套类型 |

---

## 2. 目标：契约优先

```
┌─────────────────────────────────────────────────────────────┐
│                    Single Source of Truth                     │
│  OpenAPI 3.1 Spec (YAML/JSON) ← 后端代码注解/手写 Spec 生成   │
└─────────────────────┬───────────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
   ┌─────────┐  ┌─────────┐  ┌─────────┐
   │ Java    │  │TypeScript│  │ 其他    │
   │ Server  │  │ Client  │  │ 语言    │
   │ Stub    │  │ (Axios) │  │ (Go/Kotlin|
   │ 生成    │  │ 生成    │  │  Dart...) │
   └─────────┘  └─────────┘  └─────────┘
        │             │             │
        ▼             ▼             ▼
   编译期检查    编译期类型安全   多端一致性
```

---

## 3. 后端规范化：SpringDoc OpenAPI 3 配置

### 3.1 依赖引入
```xml
<!-- 统一版本管理 -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.6.0</version>
</dependency>
<!-- 可选：Kotlin 支持 -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-kotlin</artifactId>
    <version>2.6.0</version>
</dependency>
```

### 3.2 全局配置
```java
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "智云星课 API",
        version = "v1",
        description = "统一 API 网关聚合文档",
        contact = @Contact(name = "Platform Team", email = "support@example.com"),
        license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0.html")
    ),
    servers = {
        @Server(url = "https://api.smartclass.cn", description = "生产环境"),
        @Server(url = "https://staging-api.smartclass.cn", description = "预发环境"),
        @Server(url = "http://localhost:12345", description = "本地开发")
    },
    tags = {
        @Tag(name = "Auth", description = "认证授权"),
        @Tag(name = "User", description = "用户管理"),
        @Tag(name = "Course", description = "课程学习"),
        @Tag(name = "AI", description = "AI 对话/习题/RAG"),
        @Tag(name = "Circle", description = "圈子社区"),
        @Tag(name = "Pay", description = "支付订阅")
    }
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT Token: Bearer <token>"))
                .addSchemas("ApiResponse", schemaOfApiResponse())
                .addSchemas("PageResult", schemaOfPageResult())
                .addSchemas("ErrorCode", schemaOfErrorCode())
            );
    }

    // 统一响应包装 Schema
    private Schema<?> schemaOfApiResponse() { ... }
    private Schema<?> schemaOfPageResult() { ... }
    private Schema<?> schemaOfErrorCode() { ... }
}
```

### 3.3 Controller 规范

```java
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "用户管理")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Operation(
        summary = "获取用户详情",
        description = "根据用户 ID 查询详细信息，包含等级、统计、成就摘要",
        responses = {
            @ApiResponse(responseCode = "200", description = "成功",
                content = @Content(schema = @Schema(implementation = UserDetailVO.class))),
            @ApiResponse(responseCode = "404", description = "用户不存在",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "无权限访问",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    @GetMapping("/{id}")
    public ApiResponse<UserDetailVO> getUserDetail(
        @Parameter(description = "用户 ID", required = true, example = "12345")
        @PathVariable Long id,
        @Parameter(hidden = true) @CurrentUserId Long currentUserId
    ) {
        return ApiResponse.success(userService.getDetail(id, currentUserId));
    }

    @Operation(summary = "更新用户资料")
    @PutMapping("/{id}/profile")
    public ApiResponse<Void> updateProfile(
        @PathVariable Long id,
        @Valid @RequestBody UpdateProfileDTO dto,
        @Parameter(hidden = true) @CurrentUserId Long currentUserId
    ) {
        userService.updateProfile(id, dto, currentUserId);
        return ApiResponse.success();
    }
}
```

### 3.4 DTO 规范

```java
@Data
@Schema(description = "更新用户资料请求")
public class UpdateProfileDTO {
    
    @Schema(description = "昵称", maxLength = 32, example = "张三")
    @NotBlank @Length(max = 32)
    private String nickname;

    @Schema(description = "性别: 0-男 1-女 2-保密", allowableValues = {"0", "1", "2"})
    @NotNull @Range(min = 0, max = 2)
    private Integer gender;

    @Schema(description = "生日", format = "date", example = "1990-01-01")
    @Past
    private LocalDate birthday;

    @Schema(description = "个人简介", maxLength = 500)
    @Length(max = 500)
    private String profile;
}
```

### 3.5 枚举规范：生成 TypeScript Enum

```java
@Schema(enumAsRef = true) // 关键：生成独立 Schema 引用
public enum UserRole {
    STUDENT("student", "学生"),
    TEACHER("teacher", "教师"),
    ADMIN("admin", "管理员"),
    BAN("ban", "封禁");

    private final String code;
    private final String desc;
}
```

---

## 4. 网关层聚合：统一入口

### 4.1 Spring Cloud Gateway 聚合配置
```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
      routes:
        - id: aggregate-docs
          uri: http://localhost:12345
          predicates:
            - Path=/v3/api-docs/**
          filters:
            - RewritePath=/v3/api-docs/(?<segment>.*), /$\{segment}/v3/api-docs
```

### 4.2 聚合控制器 (或用 springdoc-openapi-gateway)
```java
@RestController
@RequestMapping("/v3/api-docs")
public class AggregateApiDocsController {

    @Autowired private RouteLocator routeLocator;
    @Autowired private OpenApiResource openApiResource;

    @GetMapping("/aggregate")
    public OpenAPI aggregate() {
        OpenAPI merged = new OpenAPI().info(new Info().title("Aggregated API").version("1.0"));
        
        routeLocator.getRoutes()
            .filter(route -> route.getUri().getHost() != null)
            .subscribe(route -> {
                String serviceName = route.getUri().getHost();
                try {
                    OpenAPI spec = fetchSpec(serviceName);
                    mergeSpec(merged, spec, serviceName);
                } catch (Exception e) {
                    log.warn("Failed to fetch spec from {}", serviceName, e);
                }
            });
        
        return merged;
    }
}
```

---

## 5. 前端代码生成：@smartclass/api 包

### 5.1 生成器配置 (P1-4 Monorepo 集成)
```json
// packages/api/openapi-generator-config.json
{
  "generatorName": "typescript-axios",
  "inputSpec": "https://gateway.smartclass.cn/v3/api-docs/aggregate",
  "outputDir": "./src/generated",
  "configOptions": {
    "supportsES6": true,
    "withInterfaces": true,
    "withSeparateModelsAndApi": true,
    "modelPropertyNaming": "camelCase",
    "paramNaming": "camelCase",
    "enumPropertyNaming": "UPPERCASE",
    "axiosInstance": "axios",
    "useSingleRequestParameter": false,
    "generateFormDataContentType": true,
    "dateTimeFormat": "iso"
  },
  "templateDir": "./templates/custom",
  "additionalProperties": {
    "npmName": "@smartclass/api",
    "npmVersion": "1.0.0",
    "withSeparateModelsAndApi": true,
    "supportsES6": true,
    "stringEnums": true
  }
}
```

### 5.2 自定义模板关键改进

**templates/custom/api.mustache** - 统一错误处理 + 请求取消：
```mustache
{{#operations}}
export const {{operationId}} = ({{#params}}{{{param}}} {{/params}}options?: AxiosRequestConfig) => {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), {{timeout}});
  
  return apiInstance.{{operationId}}({{#params}}{{{param}}}, {{/params}}{
    ...options,
    signal: controller.signal
  }).then(response => {
    clearTimeout(timeoutId);
    return response.data; // 直接返回 data，统一解包
  }).catch(error => {
    clearTimeout(timeoutId);
    // 统一错误转换
    throw normalizeApiError(error);
  });
};
{{/operations}}
```

**templates/custom/model.mustache** - 严格类型 + 品牌化：
```mustache
{{#enums}}
export enum {{name}} {
  {{#values}}
  {{name}} = "{{value}}",
  {{/values}}
}
{{/enums}}

{{#models}}
export interface {{name}} {
  {{#vars}}
  {{#required}}/**
   * {{description}}
   */
  {{name}}{{^required}}?{{/required}}: {{type}};
  {{/vars}}
}
{{/models}}
```

### 5.3 手写封装层 (不可生成部分)
```typescript
// packages/api/src/client.ts
import axios, { AxiosError, AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus'; // 或统一的提示组件

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

export const createApiClient = (baseURL = API_BASE_URL): AxiosInstance => {
  const instance = axios.create({
    baseURL,
    timeout: 30000,
    headers: { 'Content-Type': 'application/json' },
    withCredentials: true
  });

  // 请求拦截：注入 Token、租户 ID、Trace ID
  instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    const token = useAuthStore().token;
    if (token) config.headers.Authorization = `Bearer ${token}`;
    
    const tenantId = getCurrentTenantId(); // 从 URL/Header/Store 获取
    if (tenantId) config.headers['X-Tenant-Id'] = tenantId;
    
    config.headers['X-Trace-Id'] = config.headers['X-Trace-Id'] || generateTraceId();
    config.metadata = { startTime: Date.now() };
    return config;
  });

  // 响应拦截：统一解包、错误码映射、Token 刷新
  instance.interceptors.response.use(
    response => response.data, // 直接返回 ApiResponse.data
    async (error: AxiosError<ApiErrorResponse>) => {
      const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
      
      // 401: 尝试刷新 Token
      if (error.response?.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true;
        try {
          await refreshToken();
          return instance(originalRequest);
        } catch {
          useAuthStore().logout();
          router.push('/login');
        }
      }
      
      // 业务错误码映射
      const bizCode = error.response?.data?.code;
      const message = error.response?.data?.message || error.message;
      
      // 静默错误码不弹 Toast (如 404、特定业务码)
      const silentCodes = ['USER_NOT_FOUND', 'RESOURCE_NOT_FOUND'];
      if (!silentCodes.includes(bizCode)) {
        ElMessage.error(message);
      }
      
      return Promise.reject(new ApiError(bizCode, message, error.response?.data));
    }
  );

  return instance;
};

// 单例导出
export const apiClient = createApiClient();

// 类型安全的错误类
export class ApiError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly rawData?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export const normalizeApiError = (error: any): ApiError => {
  if (error instanceof ApiError) return error;
  if (axios.isAxiosError(error)) {
    return new ApiError(
      error.response?.data?.code || 'NETWORK_ERROR',
      error.response?.data?.message || error.message,
      error.response?.data
    );
  }
  return new ApiError('UNKNOWN_ERROR', error?.message || 'Unknown error');
};
```

### 5.4 业务端点封装 (消费者友好)
```typescript
// packages/api/src/endpoints/user.ts
import { apiClient } from '../client';
import type { 
  UserDetailVO, 
  UpdateProfileDTO, 
  UserPageQuery,
  PageResult 
} from '../generated';

// 类型安全的分页查询
export const userApi = {
  getDetail: (id: number) => 
    apiClient.get<UserDetailVO>(`/users/${id}`),
  
  updateProfile: (id: number, dto: UpdateProfileDTO) => 
    apiClient.put<void>(`/users/${id}/profile`, dto),
  
  pageQuery: (params: UserPageQuery) => 
    apiClient.get<PageResult<UserSummaryVO>>('/users/page', { params }),
  
  // 文件上传特殊处理
  uploadAvatar: (file: File, onProgress?: (p: number) => void) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post<string>('/users/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: e => onProgress?.(Math.round((e.loaded * 100) / (e.total || 1)))
    });
  }
};
```

---

## 6. 自动化流水线

### 6.1 后端变更触发前端类型更新
```yaml
# .github/workflows/api-contract.yml
name: API Contract Sync

on:
  # 后端推送时触发
  workflow_dispatch:
  # 定时同步 (每日晨间)
  schedule:
    - cron: '0 2 * * *'  # 每天 10:00 CST
  # 监听后端仓库 Release
  repository_dispatch:
    types: [backend-api-changed]

jobs:
  generate-and-publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { token: ${{ secrets.GH_PAT }} } # 需写入权限
      
      - uses: pnpm/action-setup@v3
      - uses: actions/setup-node@v4
        with: { node-version: 20, registry-url: 'https://npm.smartclass.cn' }
      
      - name: Install dependencies
        run: pnpm install --frozen-lockfile
      
      - name: Generate API Client
        run: |
          cd packages/api
          pnpm run generate:api
      
      - name: Type Check
        run: pnpm --filter @smartclass/api run typecheck
      
      - name: Run Tests
        run: pnpm --filter @smartclass/api run test
      
      - name: Create Changeset
        run: |
          pnpm changeset add --empty --message "chore(api): sync types from backend ${{ github.event.client_payload?.version || 'latest' }}"
      
      - name: Version & Publish
        uses: changesets/action@v1
        with:
          publish: pnpm changeset publish
          version: pnpm changeset version
        env:
          GITHUB_TOKEN: ${{ secrets.GH_PAT }}
          NPM_TOKEN: ${{ secrets.NPM_TOKEN }}
```

### 6.2 破坏性变更检测
```bash
# tools/breaking-change-detector.js
import { OpenAPI } from 'openapi-types';
import { readFileSync } from 'fs';

function detectBreakingChanges(oldSpec: OpenAPI.Document, newSpec: OpenAPI.Document) {
  const breaking = [];
  
  // 1. 路径移除
  Object.keys(oldSpec.paths || {}).forEach(path => {
    if (!newSpec.paths?.[path]) {
      breaking.push({ type: 'PATH_REMOVED', path });
    }
  });
  
  // 2. 必填字段新增
  Object.entries(newSpec.components?.schemas || {}).forEach(([name, schema]) => {
    const oldSchema = oldSpec.components?.schemas?.[name];
    if (oldSchema && schema.required) {
      const newRequired = new Set(schema.required);
      const oldRequired = new Set(oldSchema.required || []);
      const added = [...newRequired].filter(r => !oldRequired.has(r));
      if (added.length) breaking.push({ type: 'REQUIRED_FIELD_ADDED', schema: name, fields: added });
    }
  });
  
  // 3. 枚举值移除
  // 4. 响应类型收窄
  // 5. 参数类型变更
  
  return breaking;
}

// CI 中集成：如有破坏性变更，阻断发布，要求人工确认
```

---

## 7. 多环境/多版本管理

### 7.1 版本策略
```
API 版本控制：
├── URL Path: /api/v1/..., /api/v2/... (主版本)
├── Header: Accept: application/vnd.smartclass.v1+json (次版本)
└── Deprecation: Sunset Header + 文档标记

生成策略：
├── 主版本: 独立包 @smartclass/api-v1, @smartclass/api-v2
├── 次版本: 同包 Patch 版本 (向后兼容)
└── 废弃: 标记 @deprecated, 文档生成警告
```

### 7.2 多环境 Spec 源
```typescript
// packages/api/src/config.ts
const ENV_SPECS = {
  local: 'http://localhost:12345/v3/api-docs/aggregate',
  dev: 'https://dev-api.smartclass.cn/v3/api-docs/aggregate',
  staging: 'https://staging-api.smartclass.cn/v3/api-docs/aggregate',
  prod: 'https://api.smartclass.cn/v3/api-docs/aggregate'
} as const;

export const getSpecUrl = () => ENV_SPECS[import.meta.env.VITE_API_ENV || 'dev'];
```

---

## 8. 质量门禁

| 检查项 | 工具 | 阈值 | 失败处理 |
|--------|------|------|----------|
| **Spec 有效性** | `swagger-codegen validate` | 0 Errors | 阻断构建 |
| **破坏性变更** | 自定义脚本 | 0 Unapproved | 阻断发布，需人工审批 |
| **类型覆盖率** | `type-coverage` | ≥ 95% | 警告 |
| **生成代码 Lint** | ESLect + Prettier | 0 Errors | 阻断构建 |
| **单元测试覆盖** | Vitest | ≥ 80% (生成层) | 警告 |
| **包体积** | `bundle-phobia` | < 50 KB gzip | 警告 |

---

## 9. 迁移计划

| 阶段 | 时间 | 范围 | 验收 |
|------|------|------|------|
| **Phase 1** | Week 1 | 后端规范化：统一注解、全局响应 Schema、枚举规范 | 所有 Controller 通过 `springdoc` 生成有效 Spec |
| **Phase 2** | Week 2 | 网关聚合 + 前端生成器配置 + CI 流水线 | `@smartclass/api` 可自动发布、类型检查通过 |
| **Phase 3** | Week 3 | 手写封装层 + 业务端点封装 + 替换现有手写 API | Mobile/Admin 核心链路 (登录、用户、课程) 切换 |
| **Phase 4** | Week 4 | 全量替换 + 破坏性变更检测 + 文档站 | 0 手写 API 调用、破坏性变更零漏报 |
| **Phase 5** | Week 5 | 多版本支持 + 性能优化 + 团队培训 | v1/v2 并存、生成耗时 < 30s |

---

## 10. 工作量预估

| 活动 | 人天 | 备注 |
|------|------|------|
| 后端 OpenAPI 规范化 (全量 Controller) | 8 | 16 微服务 × 平均 5 Controller |
| 网关聚合配置 + 统一错误 Schema | 3 |  |
| 前端生成器配置 + 自定义模板 | 5 | Axios 封装、错误处理、取消请求 |
| CI/CD 流水线 + 破坏性变更检测 | 4 |  |
| 手写 API 替换 + 回归测试 | 10 | 并行化、分模块验证 |
| 多版本/多环境支持 | 3 |  |
| 文档 + 培训 | 2 |  |
| **合计** | **~35 人天 (约 5-6 周, 2 人)** | |

---

## 11. 决策建议

**强烈建议立即启动**，理由：
- **ROI 极高**：一次投入，永久消除手写类型同步成本
- **风险可控**：生成代码隔离在 `@smartclass/api`，不影响业务代码
- **P1-4 Monorepo 先决条件**：API 包是 Monorepo 核心共享包
- **P1-1 Dubbo3 配套**：Dubbo Triple 也需 IDL，OpenAPI 可作 HTTP 网关层契约

**需要团队确认**：
1. 后端是否接受强制规范 Swagger 注解？(需 Code Review 强制)
2. 是否有旧版本 API 需长期维护？(决定多版本包策略)
3. 第三方对接方是否需要 OpenAPI Spec？(决定是否发布公开 Spec)