import js from '@eslint/js'
import globals from 'globals'
import hooks from 'eslint-plugin-react-hooks'
import i18next from 'eslint-plugin-i18next'
import tseslint from 'typescript-eslint'

export default tseslint.config(
  { ignores: ['dist/**', 'playwright-report/**', 'test-results/**', 'src/api/types.gen.ts'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: { globals: { ...globals.browser, ...globals.node } },
    plugins: { 'react-hooks': hooks, i18next },
    rules: {
      ...hooks.configs.recommended.rules,
      'i18next/no-literal-string': ['error', { mode: 'jsx-text-only' }],
    },
  },
)
