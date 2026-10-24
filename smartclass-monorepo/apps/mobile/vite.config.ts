import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  plugins: [
    vue(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'robots.txt', 'apple-touch-icon.png'],
      manifest: {
        name: '智云星课',
        short_name: '智云星课',
        description: '智云星课 - 智能学习平台',
        theme_color: '#6366f1',
        background_color: '#ffffff',
        display: 'standalone',
        scope: '/',
        start_url: '/',
        icons: [
          {
            src: '/pwa-192x192.png',
            sizes: '192x192',
            type: 'image/png'
          },
          {
            src: '/pwa-512x512.png',
            sizes: '512x512',
            type: 'image/png'
          },
          {
            src: '/pwa-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'any maskable'
          }
        ]
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
        cleanupOutdatedCaches: true,
        runtimeCaching: [
          {
            urlPattern: /^https:\/\/api\.smartclass\.cn\/.*/i,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-cache',
              expiration: {
                maxEntries: 100,
                maxAgeSeconds: 60 * 60 * 24 // 24小时
              },
              networkTimeoutSeconds: 10
            }
          },
          {
            urlPattern: /^https:\/\/fonts\.googleapis\.com\/.*/i,
            handler: 'CacheFirst',
            options: {
              cacheName: 'google-fonts-cache',
              expiration: {
                maxEntries: 10,
                maxAgeSeconds: 60 * 60 * 24 * 365 // 1年
              }
            }
          },
          {
            urlPattern: /^https:\/\/fonts\.gstatic\.com\/.*/i,
            handler: 'CacheFirst',
            options: {
              cacheName: 'gstatic-fonts-cache',
              expiration: {
                maxEntries: 10,
                maxAgeSeconds: 60 * 60 * 24 * 365
              }
            }
          }
        ]
      }
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '@smartclass/shared': resolve(__dirname, '../../packages/shared/src'),
      '@smartclass/api': resolve(__dirname, '../../packages/api/src'),
      '@smartclass/ui': resolve(__dirname, '../../packages/ui/src'),
      '@smartclass/stores': resolve(__dirname, '../../packages/stores/src'),
      '@smartclass/styles': resolve(__dirname, '../../packages/styles/src')
    }
  },
  build: {
    target: 'es2022',
    minify: 'esbuild',
    cssCodeSplit: true,
    rollupOptions: {
      output: {
        // 函数形式：仅对实际进入依赖图的模块分组，避免把未安装模块当作 chunk 入口
        manualChunks(id: string) {
          if (id.includes('node_modules')) {
            if (/[\/](vue|vue-router|pinia)[\/]/.test(id)) return 'vendor-vue';
            if (/[\/](element-plus|@element-plus)[\/]/.test(id)) return 'vendor-ui';
            if (/[\/](axios|@vueuse|dayjs|lodash-es)[\/]/.test(id)) return 'vendor-utils';
            return undefined;
          }
          if (id.includes('packages/shared')) return 'smartclass-shared';
          if (id.includes('packages/api')) return 'smartclass-api';
          if (id.includes('packages/ui')) return 'smartclass-ui';
          if (id.includes('packages/stores')) return 'smartclass-stores';
          return undefined;
        }
      }
    },
    chunkSizeWarningLimit: 1000
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
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@smartclass/styles/src/variables.css" as *;`
      }
    }
  }
});