#!/usr/bin/env node
import { generate } from '@openapitools/openapi-generator-cli';
import { execSync } from 'child_process';
import { readFileSync, writeFileSync } from 'fs';
import { resolve } from 'path';

async function generateApi() {
  console.log('🔄 开始生成 API 客户端...');
  
  const specUrl = process.env.OPENAPI_SPEC_URL || 'http://localhost:12345/v3/api-docs/aggregate';
  const configPath = resolve(__dirname, '../packages/api/openapi-generator-config.json');
  const outputDir = resolve(__dirname, '../packages/api/src/generated');
  
  try {
    // 1. 生成代码
    await generate({
      generatorName: 'typescript-axios',
      inputSpec: specUrl,
      output: outputDir,
      config: configPath
    });
    
    console.log('✅ 代码生成完成');
    
    // 2. 格式化生成的代码
    console.log('🔧 格式化代码...');
    execSync('pnpm prettier --write "packages/api/src/generated/**/*.ts"', { 
      cwd: resolve(__dirname, '..'),
      stdio: 'inherit'
    });
    
    // 3. 类型检查
    console.log('🔍 类型检查...');
    execSync('pnpm --filter @smartclass/api run typecheck', { 
      cwd: resolve(__dirname, '..'),
      stdio: 'inherit'
    });
    
    // 4. 修复生成代码中的常见问题
    fixGeneratedCode(outputDir);
    
    console.log('✨ API 客户端生成完成！');
    
  } catch (error) {
    console.error('❌ 生成失败:', error);
    process.exit(1);
  }
}

function fixGeneratedCode(outputDir: string) {
  // 读取所有生成的文件并修复常见问题
  const { readdirSync, statSync } = require('fs');
  
  function processDir(dir: string) {
    const files = readdirSync(dir);
    for (const file of files) {
      const fullPath = resolve(dir, file);
      if (statSync(fullPath).isDirectory()) {
        processDir(fullPath);
      } else if (file.endsWith('.ts')) {
        let content = readFileSync(fullPath, 'utf-8');
        
        // 修复 1: 移除不必要的导入
        content = content.replace(/import \{ Configuration \} from '\.\/configuration';\n?/g, '');
        content = content.replace(/import \{.*\} from '\.\/base';\n?/g, '');
        
        // 修复 2: 将 Configuration 类型替换为内联类型
        content = content.replace(/Configuration/g, 'ApiConfiguration');
        
        // 修复 3: 添加缺失的导入
        if (content.includes('axios') && !content.includes("import axios")) {
          content = "import axios from 'axios';\n" + content;
        }
        
        writeFileSync(fullPath, content, 'utf-8');
      }
    }
  }
  
  processDir(outputDir);
  console.log('🔧 生成代码修复完成');
}

// 如果直接运行此脚本
if (import.meta.url === `file://${process.argv[1]}`) {
  generateApi();
}

export { generateApi };