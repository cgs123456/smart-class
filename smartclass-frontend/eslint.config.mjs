import js from '@eslint/js';
import globals from 'globals';
import ts from 'typescript-eslint';

export default [
  { languageOptions: { globals: globals.browser } },
  js.configs.recommended,
  ...ts.configs.recommended,
  // iconfont.js 为 iconfont.cn 导出的第三方打包产物，不参与 lint
  { ignores: ['dist/', 'public/'] },
  {
    // 声明文件中的 {} / any 是 Vue SFC 与第三方模块声明的惯用写法
    files: ['**/*.d.ts'],
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
      '@typescript-eslint/no-empty-object-type': 'off',
    },
  },
];
