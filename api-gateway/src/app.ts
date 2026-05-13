// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'

import { createSamlAdIntegration } from './espoo-user/ad-saml.ts'
import { createDevAdRouter } from './espoo-user/dev-ad-auth.ts'
import { espooUserAuthStatus } from './espoo-user/routes/auth-status.ts'
import type { Config } from './shared/config.ts'
import { appCommit } from './shared/config.ts'
import { toRequestHandler } from './shared/express.ts'
import { cacheControl } from './shared/middleware/cache-control.ts'
import { csrf } from './shared/middleware/csrf.ts'
import { errorHandler } from './shared/middleware/error-handler.ts'
import { createProxy } from './shared/proxy-utils.ts'
import type { RedisClient } from './shared/redis-client.ts'
import { handleCspReport } from './shared/routes/csp.ts'
import type { SamlIntegration } from './shared/routes/saml.ts'
import { sessionSupport } from './shared/session.ts'

export function apiRouter(config: Config, redisClient: RedisClient) {
  const router = express.Router()

  router.post(
    '/csp',
    express.json({ type: 'application/csp-report' }),
    handleCspReport
  )

  router.get('/version', (_, res) => {
    res.send({ commitId: appCommit })
  })

  router.use(cacheControl(() => 'forbid-cache'))

  const sessions = sessionSupport('espoo-user', redisClient, config.espooUser)
  const proxy = createProxy({
    getUserHeader: (req) => sessions.getUserHeader(req)
  })

  let adIntegration: SamlIntegration | undefined
  if (config.ad.type === 'mock') {
    router.use('/auth/saml', createDevAdRouter(sessions))
  } else if (config.ad.type === 'saml') {
    adIntegration = createSamlAdIntegration(sessions, config.ad, redisClient)
    router.use('/auth/saml', adIntegration.router)
  }

  router.get(
    '/auth/logout',
    sessions.middleware,
    toRequestHandler(async (req, res) => {
      const user = sessions.getUser(req)
      switch (user?.authType) {
        case 'ad':
          if (adIntegration) return adIntegration.logout(req, res)
          break
        case 'dev':
          // no need for special handling
          break
      }
      await sessions.destroy(req, res)
      res.redirect('/')
    })
  )

  // CSRF checks apply to all the API endpoints that frontend uses
  router.use(csrf)

  router.use(sessions.middleware)
  router.get('/auth/status', espooUserAuthStatus(sessions))
  router.all('/auth/{*rest}', (_, res) => res.redirect('/'))
  router.all('/{*rest}', sessions.requireAuthentication, proxy)

  // global error middleware
  router.use(errorHandler(false))
  return router
}
