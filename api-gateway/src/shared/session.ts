// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RedisStore } from 'connect-redis'
import { addMinutes } from 'date-fns/addMinutes'
import { differenceInMinutes } from 'date-fns/differenceInMinutes'
import { differenceInSeconds } from 'date-fns/differenceInSeconds'
import { isDate } from 'date-fns/isDate'
import type express from 'express'
import session from 'express-session'

import type { OppivelvollisuusSessionUser } from './auth/index.ts'
import { createUserHeader } from './auth/index.ts'
import type { SessionConfig } from './config.ts'
import { createSha256Hash } from './crypto.ts'
import type { LogoutToken } from './express.ts'
import { toMiddleware } from './express.ts'
import { logAuditEvent, logDebug } from './logging.ts'
import { fromCallback } from './promise-utils.ts'
import type { RedisClient } from './redis-client.ts'

export type SessionType = 'espoo-user'

function cookiePrefix(sessionType: SessionType) {
  switch (sessionType) {
    case 'espoo-user':
      return 'oppivelvollisuus.espoo-user'
  }
}

function sessionKey(id: string) {
  return `sess:${id}`
}

function logoutKey(token: LogoutToken['value']) {
  return `slo:${token}`
}

export function sessionCookie(sessionType: SessionType) {
  return `${cookiePrefix(sessionType)}.session`
}

export interface Sessions<T extends SessionType> {
  sessionType: T
  cookieName: string
  middleware: express.RequestHandler
  requireAuthentication: express.RequestHandler

  save(req: express.Request): Promise<void>

  saveLogoutToken(
    req: express.Request,
    logoutToken?: LogoutToken['value']
  ): Promise<void>
  logoutWithToken(token: LogoutToken['value']): Promise<unknown>

  login(req: express.Request, user: OppivelvollisuusSessionUser): Promise<void>
  destroy(req: express.Request, res: express.Response): Promise<void>

  updateUser(
    req: express.Request,
    user: OppivelvollisuusSessionUser
  ): Promise<void>
  getUser(req: express.Request): OppivelvollisuusSessionUser | undefined
  getUserHeader(req: express.Request): string | undefined
  isAuthenticated(req: express.Request): boolean
}

export function sessionSupport<T extends SessionType>(
  sessionType: T,
  redisClient: RedisClient,
  config: SessionConfig
): Sessions<T> {
  const cookieName = sessionCookie(sessionType)

  // Base session support middleware from express-session
  const baseMiddleware = session({
    cookie: {
      path: '/',
      httpOnly: true,
      secure: config.useSecureCookies,
      sameSite: 'lax',
      maxAge: config.sessionTimeoutMinutes * 60000
    },
    resave: false,
    rolling: true,
    saveUninitialized: false,
    secret: config.cookieSecret,
    name: cookieName,
    store: new RedisStore({ client: redisClient })
  })

  const extraMiddleware = toMiddleware(async (req) => {
    // Touch maxAge to guarantee session is rolling (= doesn't expire as long as you are active)
    req.session?.touch()
    req.user = getUser(req)

    await refreshLogoutToken(req)
  })

  const middleware: express.RequestHandler = (req, res, next) => {
    baseMiddleware(req, res, (errOrDefer) => {
      if (errOrDefer) next(errOrDefer)
      else extraMiddleware(req, res, next)
    })
  }

  const requireAuthentication: express.RequestHandler = (req, res, next) => {
    const user = getUser(req)
    if (!user || !user.id) {
      logAuditEvent(
        `oppivelvollisuus.api-gateway.auth.not_found`,
        req,
        'Could not find user'
      )
      res.sendStatus(401)
      return
    }
    return next()
  }

  async function save(req: express.Request) {
    const session = req.session
    if (session) {
      await fromCallback((cb) => session.save(cb))
    }
  }

  async function saveLogoutToken(
    req: express.Request,
    logoutToken?: LogoutToken['value']
  ): Promise<void> {
    if (!req.session) return

    const token = logoutToken || req.session.logoutToken?.value
    if (!token) return

    const now = new Date()
    const expires = addMinutes(now, config.sessionTimeoutMinutes + 60)
    const newToken = {
      expiresAt: expires.valueOf(),
      value: token
    }
    req.session.logoutToken = newToken

    const key = logoutKey(token)
    const ttlSeconds = differenceInSeconds(expires, now)
    await redisClient.set(key, req.session.id, { EX: ttlSeconds })
  }

  async function refreshLogoutToken(req: express.Request): Promise<void> {
    if (!req.session) return
    if (!req.session.logoutToken) return
    if (!isDate(req.session.cookie.expires)) return
    const sessionExpires = req.session.cookie.expires
    const logoutExpires = new Date(req.session.logoutToken.expiresAt)
    if (differenceInMinutes(logoutExpires, sessionExpires) < 30) {
      await saveLogoutToken(req)
    }
  }

  async function logoutWithToken(
    logoutToken: LogoutToken['value']
  ): Promise<unknown> {
    if (!logoutToken) return
    const sid = await redisClient.get(logoutKey(logoutToken))
    if (!sid) return
    const session = await redisClient.get(sessionKey(sid))
    await redisClient.del([sessionKey(sid), logoutKey(logoutToken)])
    if (!session) return

    // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-unsafe-assignment
    const user = JSON.parse(session)?.oppivelvollisuus?.user
    if (!user) return

    return user
  }

  async function login(
    req: express.Request,
    user: OppivelvollisuusSessionUser
  ): Promise<void> {
    await fromCallback<void>((cb) => req.session.regenerate(cb))
    // express-session has now regenerated the active session, so the ID has changed, and it's empty
    await saveUser(req, {
      ...user,
      // spread saml session fields for backwards compatibility
      ...(user.authType === 'ad' ? user.samlSession : undefined)
    })
  }

  async function destroy(req: express.Request, res: express.Response) {
    logDebug('Destroying session', req)
    const logoutToken = req.session?.logoutToken?.value

    if (req.session) {
      await fromCallback((cb) => req.session.destroy(cb))
      res.clearCookie(cookieName)
    }

    if (logoutToken) {
      const sid = await redisClient.get(logoutKey(logoutToken))
      if (sid) {
        await redisClient.del([sessionKey(sid), logoutKey(logoutToken)])
      }
    }
  }

  async function updateUser(
    req: express.Request,
    user: OppivelvollisuusSessionUser
  ): Promise<void> {
    if (!req.session)
      throw new Error("Can't update user without an existing session")
    await saveUser(req, user)
  }

  async function saveUser(
    req: express.Request,
    user: OppivelvollisuusSessionUser
  ) {
    req.session.oppivelvollisuus = {
      user,
      userIdHash: createSha256Hash(user.id)
    }
    await save(req)
    req.user = user
  }

  function getUser(
    req: express.Request
  ): OppivelvollisuusSessionUser | undefined {
    return req.session?.oppivelvollisuus?.user ?? undefined
  }

  function getUserHeader(req: express.Request): string | undefined {
    const user = getUser(req)
    return user ? createUserHeader(user) : undefined
  }

  function isAuthenticated(req: express.Request): boolean {
    return !!req.session?.oppivelvollisuus?.user
  }

  return {
    sessionType,
    cookieName,
    middleware,
    requireAuthentication,
    save,
    saveLogoutToken,
    logoutWithToken,
    login,
    destroy,
    updateUser,
    getUser,
    getUserHeader,
    isAuthenticated
  }
}
