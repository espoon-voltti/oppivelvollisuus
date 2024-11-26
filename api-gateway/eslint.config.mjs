// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import eslint from '@eslint/js'
import tsParser from '@typescript-eslint/parser'
import eslintPluginPrettierRecommended from 'eslint-plugin-prettier/recommended'
import globals from 'globals'
import typescriptEslint from 'typescript-eslint'

export default [
  { ignores: ['.yarn', 'dist', 'eslint.config.mjs'] },
  eslint.configs.recommended,
  ...typescriptEslint.configs.recommended,
  ...typescriptEslint.configs.stylisticTypeChecked,
  {
    languageOptions: {
      parser: tsParser,
      ecmaVersion: 2018,
      sourceType: 'module',
      parserOptions: {
        projectService: true,
        project: './tsconfig.json',
        tsconfigRootDir: import.meta.dirname,
        globals: globals.node,
        ecmaFeatures: {
          modules: true
        }
      }
    }
  },
  {
    files: ['**/*.ts'],
    rules: {
      '@typescript-eslint/no-unused-vars': [
        'warn',
        {
          args: 'all',
          argsIgnorePattern: '^_',
          caughtErrors: 'all',
          caughtErrorsIgnorePattern: '^.*',
          destructuredArrayIgnorePattern: '^_',
          varsIgnorePattern: '^_',
          ignoreRestSiblings: true
        }
      ]
    }
  },
  {
    files: ['**/*.{ts,js,mjs}'],
    rules: {
      '@typescript-eslint/consistent-type-definitions': 'off',
      '@typescript-eslint/no-empty-object-type': [
        'error',
        { allowInterfaces: 'always' }
      ],
      '@typescript-eslint/no-misused-promises': [
        'error',
        { checksVoidReturn: false }
      ],
      '@typescript-eslint/prefer-nullish-coalescing': 'off',
      '@typescript-eslint/prefer-optional-chain': 'off',
      '@typescript-eslint/prefer-promise-reject-errors': 'off'
    }
  },
  {
    files: ['**/*.{js,mjs}'],
    rules: {
      '@typescript-eslint/no-var-requires': 'off'
    }
  },
  {
    files: ['**/*.js'],
    rules: {
      '@typescript-eslint/no-require-imports': 'off'
    }
  },
  eslintPluginPrettierRecommended
]
