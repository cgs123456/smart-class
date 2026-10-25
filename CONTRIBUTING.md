# 贡献指南（CONTRIBUTING）

感谢参与智云星课（SmartClass）的开发。请先阅读 [README](README.md) 完成本地启动，再按以下约定提交变更。

## 开发环境

- JDK 17、Maven 3.8+、Node ≥ 18、MySQL 8、Redis 7
- 后端：`cd smartclass-backend && mvn package -DskipTests` 后按 README 注入环境变量启动
- 前端：`smartclass-frontend`（移动端）与 `smartclass-manage-frontend`（管理端）各自 `npm install && npm run dev`
- 接口契约变更走 OpenAPI 流程：先改后端 → `bash scripts/export-openapi.sh` 重新生成前端客户端（见 [docs/openapi-generation.md](docs/openapi-generation.md)）

## 分支与提交

- 分支命名：`feat/xxx`、`fix/xxx`、`docs/xxx`
- 提交信息用祈使句概括变更主题，例如 `修复圈子页标签解析导致的 500`
- 一次提交聚焦一件事；混入的无关格式化改动请拆分

## 提交前检查

1. `cd smartclass-backend && mvn test` 全绿（需注入 `MYSQL_PASSWORD`/`REDIS_PASSWORD`/`JWT_SECRET`，详见 README「测试」节）
2. 涉及前端的改动：`npm run build` 通过
3. 涉及数据库的改动：新增 Flyway 迁移脚本（`V<n>__描述.sql`），禁止修改已合入的历史脚本
4. 不在代码、日志、文档中写入明文密码/密钥，一律走环境变量

## 代码约定

- 遵循现有分层：controller 只做参数校验与组装，业务在 service，跨层模型不得互相泄漏
- 新接口必须带参数校验注解与统一响应包装（`BaseResponse`）
- 对外行为变更（`/api/*` 契约、数据库迁移）需在 PR 描述中标注兼容性影响

## 提交 PR

- 描述变更动机、影响范围与验证方式（贴出关键测试/自测输出）
- 关联已知问题编号（如有）；CI 通过后等待 review
