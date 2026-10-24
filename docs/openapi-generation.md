# OpenAPI 客户端生成指南 (P1-5)

微服务接口文档由 knife4j/springdoc 自动暴露（`/v3/api-docs`），
TypeScript/Axios 客户端通过 `openapi-generator-maven-plugin` 按需生成。

## 1. 导出 OpenAPI 规范

启动需要的服务后，运行：

```bash
./scripts/export-openapi.sh              # 导出全部服务
./scripts/export-openapi.sh user course  # 只导出部分服务
OPENAPI_HOST=192.168.1.10 ./scripts/export-openapi.sh  # 指定主机
```

导出结果写入 `smartclass-backend-microservice/smartclass-backend-api-contracts/openapi/<service>.json`。

## 2. 生成 TypeScript/Axios 客户端

```bash
cd smartclass-backend-microservice

# 用指定服务的规范生成客户端（输出到 smartclass-backend-api-contracts/generated）
mvn generate-sources -Popenapi \
    -Dopenapi.inputSpec=smartclass-backend-api-contracts/openapi/course.json

# 不指定 inputSpec 时默认使用 smartclass-backend-api-contracts/openapi.json
# （聚合格式，可自行合并各服务规范后放置于此）
```

> 注意：该插件在独立 profile `openapi` 中，不会在常规 `mvn compile/install` 时执行。
> 生成产物不应手工编辑；将其复制或链接到前端 `packages/api` 后按需提交。

## 3. 破坏性变更检测

CI 中的 `openapi-diff` 工作流（`.github/workflows/openapi-diff.yml`）在 PR 修改
`smartclass-backend-api-contracts/openapi/` 下的规范时，使用 [oasdiff](https://github.com/oasdiff/oasdiff)
对比目标分支，检测 Breaking Change（删除接口/必填字段变更/类型变更等）并阻塞合并。

约定：规范文件是**生成的基线（baseline）**，接口变更后需重新导出并提交，
CI 才能对比出破坏性差异。
