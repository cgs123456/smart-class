/**
 * OpenAPI 生成产物占位模块
 *
 * 实际内容由 openapi-generator-maven-plugin (typescript-axios) 生成：
 *   1. 启动微服务后运行 ../../scripts/export-openapi.sh 导出规范
 *   2. 运行 mvn generate-sources -Popenapi 生成客户端
 *   3. 将生成产物复制到本目录并在此处统一导出
 *
 * 在规范导出前，此占位保证 @smartclass/api 可正常构建。
 */

export interface GeneratedClientInfo {
  /** 生成时间 */
  generatedAt: string;
  /** 各服务规范文件名 */
  services: string[];
}

export const GENERATED_CLIENT_INFO: GeneratedClientInfo = {
  generatedAt: 'placeholder',
  services: [],
};
