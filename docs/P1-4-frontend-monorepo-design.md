# P1-4: 前端 Monorepo 重构设计文档 (pnpm + Vite + Turborepo)

## 1. 现状分析

### 当前前端结构
```
smartclass-frontend/           # 单仓库，单应用
├── src/
│   ├── views/                 # 页面级组件 (home, course, circle, profile...)
│   ├── components/            # 通用组件 (Button, Modal, Card...)
│   ├── api/                   # API 请求封装 (index.ts, user.ts, course.ts...)
│   ├── stores/                # Pinia 状态管理 (userStore, courseStore...)
│   ├── utils/                 # 工具函数
│   ├── styles/                # 全局样式、变量
│   └── router/                # 路由配置
├── package.json
├── vite.config.ts
├── tsconfig.json
└── .eslintrc.js
```

**并行存在的 Admin 后台**：
```
smartclass-admin/              # 独立仓库/目录
├── src/                       # 结构类似但组件/工具函数大量重复
├── package.json
└── ...
```

### 痛点
| 问题 | 影响 |
|------|------|
| **代码重复率 > 40%** | 组件、工具函数、API 类型、状态管理在 Mobile/Admin 间复制粘贴 |
| **版本不一致** | 共享依赖 (Vue, Pinia, Axios, Element Plus) 版本漂移，升级困难 |
| **构建缓存无效** | 单仓库全量构建，修改一个工具函数触发全量重建 |
| **类型共享困难** | 后端 OpenAPI 生成的 TS 类型无法在多应用间复用 |
| **发布流程割裂** | Mobile/Admin 独立 CI/CD，无法原子化发布 |
| **团队协作冲突** | 多人改同一文件 (如 api/index.ts) 频繁冲突 |

---

## 2. 目标架构

### 2.1 Monorepo 结构设计
```
smartclass-monorepo/
├── package.json                    # 根配置、workspaces、scripts
├── pnpm-workspace.yaml             # pnpm 工作空间配置
├── turbo.json                      # Turborepo 流水线配置
├── tsconfig.base.json              # TypeScript 基础配置
├── .eslintrc.cjs                   # ESLint 统一配置
├── .prettierrc                     # Prettier 统一配置
├── .github/workflows/              # 统一 CI/CD
│
├── apps/                           # 应用层 (可独立部署)
│   ├── mobile/                     # 移动端 H5 / PWA (Vue 3 + Vite + UnoCSS)
│   │   ├── src/
│   │   ├── index.html
│   │   ├── vite.config.ts
│   │   ├── package.json
│   │   └── tsconfig.json
│   ├── admin/                      # 管理后台 (Vue 3 + Vite + Element Plus)
│   │   ├── src/
│   │   ├── index.html
│   │   ├── vite.config.ts
│   │   ├── package.json
│   │   └── tsconfig.json
│   └── docs/                       # 文档站 (VitePress)
│
├── packages/                       # 包层 (内部依赖，发布到私有 npm)
│   ├── ui/                         # 通用 UI 组件库
│   │   ├── src/
│   │   │   ├── components/         # Button, Input, Modal, Card, Table...
│   │   │   ├── composables/        # useDebounce, useScroll, usePermission...
│   │   │   ├── directives/         # v-permission, v-copy, v-lazy...
│   │   │   └── index.ts
│   │   ├── package.json
│   │   ├── vite.config.ts          # 库模式构建
│   │   └── tsconfig.json
│   ├── shared/                     # 纯工具函数、常量、类型 (零依赖)
│   │   ├── src/
│   │   │   ├── utils/              # date, string, validation, storage...
│   │   │   ├── constants/          # enums, errorCodes, regex...
│   │   │   ├── types/              # 通用 TS 类型 (分页、响应、用户...)
│   │   │   └── index.ts
│   │   ├── package.json
│   │   └── tsconfig.json
│   ├── api/                        # API 客户端 + 类型 (OpenAPI 生成)
│   │   ├── src/
│   │   │   ├── generated/          # OpenAPI Generator 产物 (不可手改)
│   │   │   ├── client.ts           # Axios 实例、拦截器、错误处理
│   │   │   ├── endpoints/          # 业务封装: userApi, courseApi...
│   │   │   └── index.ts
│   │   ├── package.json
│   │   └── openapi-generator-config.json
│   ├── stores/                     # 共享 Pinia Store
│   │   ├── src/
│   │   │   ├── userStore.ts
│   │   │   ├── appStore.ts         # 全局 UI 状态 (sidebar, theme, loading)
│   │   │   └── index.ts
│   │   ├── package.json
│   │   └── tsconfig.json
│   ├── styles/                     # 共享样式系统
│   │   ├── src/
│   │   │   ├── variables.css       # CSS Variables (色板、间距、字体)
│   │   │   ├── reset.css
│   │   │   ├── mixins.css
│   │   │   └── index.css
│   │   ├── package.json
│   │   └── vite.config.ts
│   └── config/                     # 共享工程化配置
│       ├── eslint-config/
│       ├── tsconfig/
│       ├── vite-config/
│       └── tailwind-config/        # 或 UnoCSS preset
│
└── tools/                          # 构建/发布工具脚本
    ├── release.js                  # 语义化版本发布
    ├── gen-api.js                  # OpenAPI 生成脚本
    └── sync-version.js             # 版本同步
```

### 2.2 技术栈选型

| 类别 | 选择 | 理由 |
|------|------|------|
| **包管理** | **pnpm 9+** | 硬链接节省磁盘、严格依赖解析防幽灵依赖、原生支持 workspace 协议 |
| **构建工具** | **Vite 5+ (库模式 + 应用模式)** | 统一构建工具、HMR 极快、Rollup 库模式成熟 |
| **任务编排** | **Turborepo 2+** | 智能缓存、远程缓存、任务拓扑并行、GitHub Actions 原生支持 |
| **代码生成** | **OpenAPI Generator (TypeScript-Axios)** | 后端契约同步、类型安全、减少手写 API 代码 80%+ |
| **UI 库** | **自建 @smartclass/ui** | 统一设计语言、Tree-shaking、主题定制 |
| **样式方案** | **UnoCSS + CSS Variables** | 原子化按需生成、主题切换零成本、无运行时 |
| **状态管理** | **Pinia 2+** | Vue 官方、TypeScript 友好、模块化 |
| **路由** | **Vue Router 4** | 标准选择 |
| **HTTP 客户端** | **Axios + 拦截器统一封装** | 成熟、取消请求、进度条、重试策略 |
| **类型检查** | **TypeScript 5.4+ (Project References)** | 增量编译、跨包类型引用 |
| **代码规范** | **ESLint 9 (Flat Config) + Prettier + Stylelint** | 统一风格、CI 阻断 |
| **提交规范** | **Commitlint + Husky + lint-staged** | 规范 Commit Message、预提交检查 |
| **测试** | **Vitest + Vue Test Utils + Playwright** | 单元/组件/E2E 全覆盖 |

---

## 3. 核心配置详解

### 3.1 pnpm-workspace.yaml
```yaml
packages:
  - apps/*
  - packages/*
  - tools/*

# 只对外发布的包
publishConfig:
  access: restricted
  registry: https://npm.smartclass.cn/
```

### 3.2 turbo.json (Turborepo Pipeline)
```json
{
  "$schema": "https://turbo.build/schema.json",
  "pipeline": {
    "build": {
      "dependsOn": ["^build"],
      "outputs": ["dist/**", ".vite/build/**"],
      "cache": true
    },
    "dev": {
      "cache": false,
      "persistent": true
    },
    "lint": {
      "outputs": [],
      "cache": true
    },
    "typecheck": {
      "dependsOn": ["^build"],
      "outputs": [],
      "cache": true
    },
    "test": {
      "dependsOn": ["^build"],
      "outputs": ["coverage/**"],
      "cache": true
    },
    "test:e2e": {
      "dependsOn": ["build"],
      "cache": false
    },
    "generate:api": {
      "cache": false,
      "outputs": ["packages/api/src/generated/**"]
    },
    "release": {
      "dependsOn": ["build", "lint", "typecheck", "test"],
      "cache": false
    }
  },
  "globalEnv": ["NODE_ENV", "VITE_APP_TITLE", "VITE_API_BASE_URL"]
}
```

### 3.3 TypeScript Project References (跨包类型复用)
```json
// packages/shared/tsconfig.json
{
  "extends": "../../tsconfig.base.json",
  "compilerOptions": {
    "composite": true,
    "declaration": true,
    "declarationMap": true,
    "outDir": "dist",
    "rootDir": "src"
  },
  "include": ["src/**/*"],
  "references": []
}

// packages/api/tsconfig.json
{
  "extends": "../../tsconfig.base.json",
  "compilerOptions": {
    "composite": true,
    "declaration": true,
    "outDir": "dist",
    "rootDir": "src"
  },
  "references": [{ "path": "../shared" }],
  "include": ["src/**/*"]
}

// apps/mobile/tsconfig.json
{
  "extends": "../../tsconfig.base.json",
  "compilerOptions": {
    "composite": false,
    "baseUrl": ".",
    "paths": {
      "@smartclass/shared/*": ["../../packages/shared/src/*"],
      "@smartclass/api/*": ["../../packages/api/src/*"],
      "@smartclass/ui/*": ["../../packages/ui/src/*"],
      "@smartclass/stores/*": ["../../packages/stores/src/*"]
    }
  },
  "references": [
    { "path": "../../packages/shared" },
    { "path": "../../packages/api" },
    { "path": "../../packages/ui" },
    { "path": "../../packages/stores" }
  ],
  "include": ["src/**/*", "vite.config.ts"]
}
```

### 3.4 共享 UI 库构建配置
```typescript
// packages/ui/vite.config.ts
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import dts from 'vite-plugin-dts';
import { resolve } from 'path';

export default defineConfig({
  plugins: [
    vue(),
    dts({
      insertTypesEntry: true,
      tsconfigPath: './tsconfig.json',
      outDir: 'dist/types'
    })
  ],
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'SmartClassUI',
      fileName: (format) => `smartclass-ui.${format}.js`,
      formats: ['es', 'umd']
    },
    rollupOptions: {
      external: ['vue', 'pinia', '@vueuse/core'],
      output: {
        globals: {
          vue: 'Vue',
          pinia: 'Pinia',
          '@vueuse/core': 'VueUse'
        }
      }
    },
    cssCodeSplit: true,
    minify: 'esbuild'
  }
});
```

### 3.5 API 包 OpenAPI 生成集成
```json
// packages/api/openapi-generator-config.json
{
  "generatorName": "typescript-axios",
  "inputSpec": "https://api.smartclass.cn/v3/api-docs",
  "outputDir": "./src/generated",
  "configOptions": {
    "supportsES6": true,
    "withInterfaces": true,
    "withSeparateModelsAndApi": true,
    "modelPropertyNaming": "camelCase",
    "paramNaming": "camelCase",
    "enumPropertyNaming": "UPPERCASE",
    "axiosInstance": "axios"
  },
  "templateDir": "./templates",
  "additionalProperties": {
    "npmName": "@smartclass/api",
    "npmVersion": "1.0.0",
    "supportsES6": true,
    "withSeparateModelsAndApi": true
  }
}
```

```javascript
// tools/gen-api.js
import { generate } from '@openapitools/openapi-generator-cli';
import { execSync } from 'child_process';

async function generateApi() {
  // 1. 从网关/注册中心聚合所有服务的 OpenAPI Spec
  const specUrl = process.env.OPENAPI_SPEC_URL || 'http://gateway:8080/v3/api-docs/aggregate';
  
  await generate({
    generatorName: 'typescript-axios',
    inputSpec: specUrl,
    output: './packages/api/src/generated',
    config: './packages/api/openapi-generator-config.json'
  });
  
  // 2. 格式化生成代码
  execSync('pnpm --filter @smartclass/api exec prettier --write "src/generated/**/*.ts"');
  
  // 3. 类型检查
  execSync('pnpm --filter @smartclass/api run typecheck');
}

generateApi().catch(console.error);
```

---

## 4. 迁移策略：Strangler Fig 渐进式

### 4.1 迁移阶段

```
Phase 0 (Week 1): Monorepo 基建
├── 创建 Monorepo 仓库结构
├── 配置 pnpm + Turborepo + TypeScript Project References
├── 搭建共享包: shared, styles, config
├── 配置 CI/CD (GitHub Actions + Turborepo Remote Cache)
├── 建立 OpenAPI 生成流水线 (每日定时/手动触发)
└── 团队培训: pnpm 工作流、包发布规范

Phase 1 (Week 2-3): 核心共享包迁移
├── 迁移 shared (utils, constants, types) -> 0 破坏性变更
├── 迁移 styles (CSS Variables, UnoCSS preset) -> 视觉回归测试
├── 迁移 stores (userStore, appStore) -> 状态同步测试
├── Mobile/Admin 同步切换依赖到 workspace:*
└── 验证: 两个应用正常启动、构建、测试通过

Phase 2 (Week 4-5): UI 组件库 + API 客户端
├── 提取通用组件 -> @smartclass/ui (Button, Modal, Form, Table...)
├── 迁移 API 客户端 -> @smartclass/api (OpenAPI 生成 + 手写封装)
├── Mobile/Admin 逐页面替换组件/调用方式
├── 建立 Storybook 文档站 (apps/docs)
└── 视觉回归测试 (Chromatic/Playwright)

Phase 3 (Week 6): 应用层重构 + 优化
├── Mobile: 路由懒加载、代码分包、PWA 配置
├── Admin: 权限指令、表单生成器、图表组件复用
├── 移除旧仓库重复代码、清理死代码
├── 性能基线建立 (Lighthouse CI)
└── 文档完善: 组件用法、贡献指南、发布流程

Phase 4 (Week 7): 发布自动化 + 运维
├── Changesets 语义化版本管理
├── 自动发布到私有 npm (GitHub Packages / Nexus)
├── 依赖更新自动化 (Renovate/Dependabot)
├── 监控大盘: 包体积、构建时长、类型错误率
└── 团队交接
```

### 4.2 兼容性保障

```json
// 根 package.json - 版本锁定策略
{
  "engines": { "node": ">=20.0.0", "pnpm": ">=9.0.0" },
  "packageManager": "pnpm@9.6.0",
  "dependencies": {
    "vue": "^3.4.0",
    "pinia": "^2.1.0",
    "vue-router": "^4.3.0",
    "axios": "^1.6.0",
    "@vueuse/core": "^10.9.0"
  },
  "devDependencies": {
    "typescript": "^5.4.0",
    "vite": "^5.2.0",
    "turbo": "^2.0.0"
  }
}
```

**workspace 协议**：
```json
// apps/mobile/package.json
{
  "dependencies": {
    "@smartclass/shared": "workspace:*",
    "@smartclass/api": "workspace:*",
    "@smartclass/ui": "workspace:*",
    "@smartclass/stores": "workspace:*",
    "@smartclass/styles": "workspace:*"
  }
}
```

---

## 5. CI/CD 流水线

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  TURBO_TOKEN: ${{ secrets.TURBO_TOKEN }}
  TURBO_TEAM: ${{ secrets.TURBO_TEAM }}
  NODE_VERSION: 20

jobs:
  # 1. 变更检测 - 只构建受影响的包
  detect-changes:
    runs-on: ubuntu-latest
    outputs:
      affected: ${{ steps.filter.outputs.affected }}
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - uses: pnpm/action-setup@v3
      - uses: actions/setup-node@v4
        with: { node-version: 20, cache: 'pnpm' }
      - run: pnpm install --frozen-lockfile
      - id: filter
        run: |
          affected=$(pnpm turbo run build --dry-run --filter=...[HEAD~1] --json | jq -r '.[] | select(.name != "root") | .name' | tr '\n' ' ')
          echo "affected=$affected" >> $GITHUB_OUTPUT

  # 2. 并行执行受影响包的任务
  lint:
    needs: detect-changes
    if: needs.detect-changes.outputs.affected != ''
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v3
      - uses: actions/setup-node@v4
        with: { node-version: 20, cache: 'pnpm' }
      - run: pnpm install --frozen-lockfile
      - run: pnpm turbo run lint --filter="${{ needs.detect-changes.outputs.affected }}"

  typecheck:
    needs: detect-changes
    if: needs.detect-changes.outputs.affected != ''
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v3
      - uses: actions/setup-node@v4
        with: { node-version: 20, cache: 'pnpm' }
      - run: pnpm install --frozen-lockfile
      - run: pnpm turbo run typecheck --filter="${{ needs.detect-changes.outputs.affected }}"

  test:
    needs: detect-changes
    if: needs.detect-changes.outputs.affected != ''
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v3
      - uses: actions/setup-node@v4
        with: { node-version: 20, cache: 'pnpm' }
      - run: pnpm install --frozen-lockfile
      - run: pnpm turbo run test --filter="${{ needs.detect-changes.outputs.affected }}"

  build:
    needs: [lint, typecheck, test]
    if: needs.detect-changes.outputs.affected != ''
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v3
      - uses: actions/setup-node@v4
        with: { node-version: 20, cache: 'pnpm' }
      - run: pnpm install --frozen-lockfile
      - run: pnpm turbo run build --filter="${{ needs.detect-changes.outputs.affected }}"

  # 3. E2E 测试 (仅主分支)
  e2e:
    needs: build
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v3
      - uses: actions/setup-node@v4
        with: { node-version: 20, cache: 'pnpm' }
      - run: pnpm install --frozen-lockfile
      - run: pnpm turbo run build
      - uses: cypress-io/github-action@v6
        with: { build: pnpm run test:e2e }

  # 4. 发布 (Tag 触发)
  release:
    needs: [build, e2e]
    if: startsWith(github.ref, 'refs/tags/v')
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - uses: pnpm/action-setup@v3
      - uses: actions/setup-node@v4
        with: { node-version: 20, registry-url: 'https://npm.smartclass.cn' }
      - run: pnpm install --frozen-lockfile
      - run: pnpm turbo run build
      - run: pnpm changeset publish
        env: { NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }} }
```

---

## 6. 包发布策略

### 6.1 版本管理：Changesets
```bash
# 开发者流程
pnpm changeset                    # 交互式生成 .changeset/*.md
git add .changeset && git commit -m "chore: version bump"
git push

# 发布流程 (CI 自动)
# 1. 合并到 main 触发 Version Packages workflow
# 2. Changesets 计算版本、更新 package.json、生成 CHANGELOG
# 3. 创建 Git Tag (v1.2.3)
# 4. Tag 触发 Release workflow -> 发布到私有 npm
```

### 6.2 发布矩阵

| 包 | 发布频率 | 版本策略 | 发布目标 |
|----|----------|----------|----------|
| `@smartclass/shared` | 低 (月级) | Patch/Minor | 私有 npm |
| `@smartclass/styles` | 低 | Patch/Minor | 私有 npm |
| `@smartclass/ui` | 中 (双周) | Minor/Major | 私有 npm + Storybook |
| `@smartclass/api` | 高 (每日/每次后端变更) | Patch (自动) | 私有 npm |
| `@smartclass/stores` | 中 | Patch/Minor | 私有 npm |
| `mobile` / `admin` | 按需 | 不发布 (Docker 镜像) | Docker Registry |

---

## 7. 性能优化目标

| 指标 | 现状 | 目标 | 优化手段 |
|------|------|------|----------|
| **冷启动构建** | ~180s | < 60s | Turborepo 远程缓存、增量构建、pnpm 硬链接 |
| **热更新 (HMR)** | ~800ms | < 200ms | Vite 5 + 依赖预构建优化 |
| **Mobile 首屏 JS** | ~450 KB gzip | < 200 KB | 代码分包、动态导入、Tree-shaking、UI 库按需 |
| **Admin 首屏 JS** | ~600 KB gzip | < 300 KB | 路由懒加载、组件库按需、移除 moment.js |
| **类型检查** | ~45s | < 15s | TypeScript Project References 增量编译 |
| **包安装** | ~120s | < 30s | pnpm 硬链接 + 离线缓存 |

---

## 8. 风险与缓解

| 风险 | 缓解 |
|------|------|
| **循环依赖** | 严格分层：apps → packages/ui/api/stores → shared；ESLint 规则 `no-circular-dependencies` |
| **版本地狱** | 根 package.json 统一版本、workspace:* 协议、Changesets 自动同步 |
| **类型不一致** | OpenAPI Generator 单一源头、Project References 强制类型检查 |
| **构建缓存失效** | Turbo 精确哈希输入、显式声明 outputs、远程缓存加速 |
| **团队学习成本** | 内部文档、示例 PR、结对编程前 2 周、迁移清单 Checklist |

---

## 9. 工作量预估

| 活动 | 人天 | 备注 |
|------|------|------|
| Monorepo 基建 (pnpm/Turbo/TS/ESLint) | 5 | 含 CI/CD |
| shared/styles/config 包迁移 | 3 | 低风险 |
| stores 迁移 + 状态同步验证 | 3 | 中风险 |
| UI 组件库建设 (20+ 组件) | 10 | 核心工作量 |
| API 客户端 + OpenAPI 生成流水线 | 5 | 含模板定制 |
| Mobile/Admin 逐页面迁移 | 12 | 并行化、视觉回归 |
| 发布自动化 + 文档 | 4 | Changesets + Storybook |
| 性能调优 + E2E 覆盖 | 4 | Lighthouse CI + Playwright |
| **合计** | **~46 人天 (约 7-9 周, 2-3 人并行)** | |

---

## 10. 与其他 P1 项依赖

| 依赖项 | 关系 |
|--------|------|
| **P1-1 Dubbo3** | API 包生成的 TS 类型需同步 Dubbo Triple 接口变更 |
| **P1-2 Gateway** | 网关统一鉴权/错误码 -> API 客户端拦截器统一处理 |
| **P1-3 多租户** | 租户标识 Header -> API 客户端自动注入、UI 主题按租户切换 |
| **P1-5 OpenAPI** | 本项包含 OpenAPI Generator 集成，P1-5 为后端规范化配套 |