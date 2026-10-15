// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    environment: 'node',
    // Vite sets BASE_URL='/' by default, which conflicts with our BASE_URL config
    env: { BASE_URL: 'http://localhost:9000' },
    include: ['src/**/__tests__/**/*.ts']
  }
})
