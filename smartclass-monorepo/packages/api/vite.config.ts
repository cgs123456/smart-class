import { defineConfig } from 'vite';
import { resolve } from 'path';

// 多入口构建，与 package.json exports 的子路径一一对应
export default defineConfig({
  build: {
    lib: {
      entry: {
        index: resolve(__dirname, 'src/index.ts'),
        endpoints: resolve(__dirname, 'src/endpoints/index.ts'),
        generated: resolve(__dirname, 'src/generated/index.ts'),
      },
      name: 'SmartClassAPI',
      fileName: (format, entryName) =>
        format === 'es' ? `${entryName}.mjs` : `${entryName}.js`,
      formats: ['es', 'cjs'],
    },
    rollupOptions: {
      external: ['axios'],
      output: {
        globals: {
          axios: 'axios',
        },
      },
    },
  },
});
