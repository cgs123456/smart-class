import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  build: {
    lib: {
      entry: resolve(__dirname, 'src/index.ts'),
      name: 'SmartClassStores',
      fileName: (format) => (format === 'es' ? 'index.mjs' : 'index.js'),
      formats: ['es', 'cjs'],
    },
    rollupOptions: {
      external: ['vue', 'pinia', '@smartclass/api', '@smartclass/shared'],
      output: {
        globals: {
          vue: 'Vue',
          pinia: 'Pinia',
        },
      },
    },
  },
});
