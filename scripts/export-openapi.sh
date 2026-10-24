#!/usr/bin/env bash
# 从运行中的微服务导出 OpenAPI 规范 (springdoc /v3/api-docs)
#
# 用法:
#   ./scripts/export-openapi.sh                     # 导出全部服务
#   ./scripts/export-openapi.sh user course         # 只导出指定服务
#
# 前置条件:
#   1. 各服务已启动 (本地默认 127.0.0.1，可通过 OPENAPI_HOST 覆盖)
#   2. 服务引入了 knife4j-openapi3-spring-boot-starter (springdoc 自动暴露 /v3/api-docs)
#
# 导出结果: smartclass-backend-api-contracts/openapi/<service>.json
# 之后可用以下命令生成 TypeScript/Axios 客户端:
#   mvn generate-sources -Popenapi -Dopenapi.inputSpec=smartclass-backend-api-contracts/openapi/<service>.json

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/smartclass-backend-microservice/smartclass-backend-api-contracts/openapi"
HOST="${OPENAPI_HOST:-127.0.0.1}"

# 服务名:端口 (与各模块 application.yml 中 server.port 一致)
declare -A SERVICES=(
  [user]=10001
  [announcement]=10002
  [circle]=10003
  [course]=10009
  [dailyword]=10005
  [dailyarticle]=10004
  [feedback]=10006
  [file]=10007
  [intelligence]=10008
  [learn-schedule]=10014
  [pay]=10011
  [search]=10012
  [stats]=10013
)

mkdir -p "${OUT_DIR}"

REQUESTED=("$@")
if [ ${#REQUESTED[@]} -eq 0 ]; then
  REQUESTED=("${!SERVICES[@]}")
fi

FAILED=()
for svc in "${REQUESTED[@]}"; do
  port="${SERVICES[${svc}]:-}"
  if [ -z "${port}" ]; then
    echo "未知服务: ${svc} (可选: ${!SERVICES[@]})"
    FAILED+=("${svc}")
    continue
  fi
  url="http://${HOST}:${port}/v3/api-docs"
  out="${OUT_DIR}/${svc}.json"
  echo "导出 ${svc} <- ${url}"
  if curl -fsS --max-time 30 "${url}" -o "${out}"; then
    echo "  -> ${out}"
  else
    echo "  !! 导出失败 (服务未启动?) ${url}"
    rm -f "${out}"
    FAILED+=("${svc}")
  fi
done

if [ ${#FAILED[@]} -gt 0 ]; then
  echo "以下服务导出失败: ${FAILED[*]}" >&2
  exit 1
fi

echo "全部导出完成: ${OUT_DIR}"
