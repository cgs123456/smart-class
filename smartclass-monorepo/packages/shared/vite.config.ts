import { defineConfig } from 'vite';
import { resolve } from 'path';

// 多入口构建，与 package.json exports 的子路径一一对应
export default defineConfig({
  build: {
    lib: {
      entry: {
        index: resolve(__dirname, 'src/index.ts'),
        utils: resolve(__dirname, 'src/utils/index.ts'),
        constants: resolve(__dirname, 'src/constants/index.ts'),
        types: resolve(__dirname, 'src/types/index.ts'),
      },
      name: 'SmartClassShared',
      fileName: (format, entryName) =>
        format === 'es' ? `${entryName}.mjs` : `${entryName}.js`,
      formats: ['es', 'cjs'],
    },
    rollupOptions: {
      external: ['vue'],
      output: {
        globals: {
          vue: 'Vue',
        },
      },
    },
  },
});
