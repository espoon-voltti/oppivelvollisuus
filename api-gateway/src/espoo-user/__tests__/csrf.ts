// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  afterAll,
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  expect,
  it
} from '@jest/globals'

import { configFromEnv } from '../../shared/config.ts'
import type { EspooUserResponse } from '../../shared/service-client.ts'
import { GatewayTester } from '../../shared/test/gateway-tester.ts'

const mockUser: EspooUserResponse = {
  id: '8fc11215-6d55-4059-bd59-038bfa36f294',
  externalId: '',
  firstName: '',
  lastName: '',
  email: null
}

describe('CSRF middleware and cookie handling in oppivelvollisuus-gateway', () => {
  let tester: GatewayTester
  beforeAll(async () => {
    tester = await GatewayTester.start(configFromEnv(), 'espoo-user')
  })
  beforeEach(async () => tester.login(mockUser))
  afterEach(async () => tester.afterEach())
  afterAll(async () => tester?.stop())

  it('should fail POST to a proxied API when there is no CSRF header', async () => {
    const res = await tester.client.post('/api/some-proxied-api', undefined, {
      validateStatus: () => true
    })
    expect(res.status).toBe(403)
  })
  it('should pass GET to a proxied API when there is no CSRF token', async () => {
    tester.nockScope.get('/some-proxied-api').reply(200)
    const res = await tester.client.get('/api/some-proxied-api')
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
  it('should pass POST to a proxied API when CSRF header is present', async () => {
    tester.setCsrfHeader = true
    tester.nockScope.post('/some-proxied-api').reply(200)
    const res = await tester.client.post('/api/some-proxied-api')
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
})
