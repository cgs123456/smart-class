// @smartclass/styles 构建脚本：CSS 设计令牌包无需打包，直接复制产物
import { mkdirSync, copyFileSync, writeFileSync } from 'node:fs';

mkdirSync('dist', { recursive: true });
copyFileSync('src/variables.css', 'dist/index.css');
copyFileSync('src/variables.css', 'dist/variables.css');
// 供 TS 消费 CSS 模块声明
writeFileSync('dist/index.d.ts', 'export {};\n');
writeFileSync('dist/variables.css.d.ts', 'export {};\n');
console.log('@smartclass/styles built');
