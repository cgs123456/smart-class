import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'SmartClassUI',
      fileName: (format) => (format === 'es' ? 'index.mjs' : 'index.js'),
      formats: ['es', 'umd'],
    },
    cssCodeSplit: false,
    rollupOptions: {
      external: ['vue', 'pinia', '@vueuse/core', 'element-plus'],
      output: {
        globals: {
          vue: 'Vue',
          pinia: 'Pinia',
          '@vueuse/core': 'VueUse',
          'element-plus': 'ElementPlus',
        },
      },
    },
  },
});
