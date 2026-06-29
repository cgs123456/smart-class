import { defineConfig } from '@rsbuild/core';
import { pluginVue } from '@rsbuild/plugin-vue';
import type { RsbuildConfig } from '@rsbuild/core';

export default defineConfig({
  plugins: [pluginVue()],
  source: {
    entry: {
      index: './src/main.ts',
    },
  },
  html: {
    mountId: 'app',
    title: '智云星课',
    favicon: './public/logo.svg',
  },
  tools: {
    swc: {
      jsc: {
        minify: {
          compress: {
            // 仅去除 console.log，保留 console.error / console.warn
            // SWC 压缩仅在生产环境（minify 启用）时生效
            pure_funcs: ['console.log'],
          },
        },
      },
    },
    bundlerChain: (chain, utils) => {
      // 注意：当前通过手动 patch define 插件 args 的方式注入 VITE_ 前缀环境变量。
      // 后续可优化为 Rsbuild 原生支持的 source.define 配置项，例如：
      //   source: { define: { 'import.meta.env.VITE_XXX': JSON.stringify(process.env.VITE_XXX) } }
      // 现暂保留该逻辑以避免破坏现有功能。
      const env = process.env;
      const envKeys = Object.keys(env).filter((key) => key.startsWith('VITE_'));

      envKeys.forEach((key) => {
        const definePluginArgs = chain.plugin('define').get('args');
        if (definePluginArgs && definePluginArgs[0]) {
          definePluginArgs[0][`import.meta.env.${key}`] = JSON.stringify(env[key]);
        }
      });
    },
    // 说明：当前未集成 bundle 体积分析工具。
    // 如需分析产物体积，可安装 @rsbuild/plugin-bundle-analyzer 并在 plugins 数组中启用：
    //   import { pluginBundleAnalyzer } from '@rsbuild/plugin-bundle-analyzer';
    //   plugins: [pluginVue(), pluginBundleAnalyzer()]
    // 默认在 build 后会生成可视化分析报告，便于定位体积瓶颈。
  },
  server: {
    // 代理配置已移除，前端直接请求后端地址
    port: 8080,
    host: 'localhost'
  },
});
