import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

export const baseViteConfig = defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@smartclass/shared': resolve(__dirname, '../../../packages/shared/src'),
      '@smartclass/api': resolve(__dirname, '../../../packages/api/src'),
      '@smartclass/ui': resolve(__dirname, '../../../packages/ui/src'),
      '@smartclass/stores': resolve(__dirname, '../../../packages/stores/src'),
      '@smartclass/styles': resolve(__dirname, '../../../packages/styles/src')
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@smartclass/styles/src/variables.scss" as *;`
      }
    }
  },
  build: {
    target: 'es2022',
    minify: 'esbuild',
    cssCodeSplit: true,
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          'vendor-ui': ['element-plus', '@element-plus/icons-vue'],
          'vendor-utils': ['axios', '@vueuse/core', 'dayjs', 'lodash-es']
        }
      }
    }
  },
  server: {
    port: 5173,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:12345',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api')
      },
      '/ws': {
        target: 'ws://localhost:12346',
        ws: true
      }
    }
  }
});

export const libraryViteConfig = defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: resolve(__dirname, '../../packages/ui/src/index.ts'),
      name: 'SmartClassUI',
      fileName: (format) => `smartclass-ui.${format}.js`,
      formats: ['es', 'umd']
    },
    rollupOptions: {
      external: ['vue', 'pinia', '@vueuse/core', 'element-plus'],
      output: {
        globals: {
          vue: 'Vue',
          pinia: 'Pinia',
          '@vueuse/core': 'VueUse',
          'element-plus': 'ElementPlus'
        }
      }
    },
    cssCodeSplit: true
  }
});

export default baseViteConfig;