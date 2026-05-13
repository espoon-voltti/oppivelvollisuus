// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { RequestHandler, Router } from 'express'
import type express from 'express'
import _ from 'lodash'
import { z } from 'zod'

import { createDevAuthRouter } from '../shared/auth/dev-auth.ts'
import type { EspooUserSessionUser } from '../shared/auth/index.js'
import { assertStringProp } from '../shared/express.ts'
import type { EspooUserLoginRequest } from '../shared/service-client.ts'
import { espooUserLogin } from '../shared/service-client.ts'
import type { Sessions } from '../shared/session.ts'

const EspooUserLoginRequestParser = z.object({
  externalId: z.string(),
  firstName: z.string(),
  lastName: z.string(),
  email: z.string().optional()
})

const devUsers: EspooUserLoginRequest[] = [
  {
    externalId: 'ad:001',
    firstName: 'Sanna',
    lastName: 'Suunnittelija'
  },
  {
    externalId: 'ad:002',
    firstName: 'Olli Oiva Otto',
    lastName: 'Ohjaaja'
  }
]

const loginFormHandler: RequestHandler = (req, res) => {
  const userOptions = devUsers.map((user, idx) => {
    const { externalId, firstName, lastName } = user
    const json = JSON.stringify(user)
    return `<div>
            <input
              type="radio"
              id="${externalId}"
              name="preset"
              ${idx == 0 ? 'checked' : ''}
              value="${_.escape(json)}" />
            <label for="${externalId}">${firstName} ${lastName}</label>
          </div>`
  })

  const formQuery =
    typeof req.query.RelayState === 'string'
      ? `?RelayState=${encodeURIComponent(req.query.RelayState)}`
      : ''
  const formUri = `${req.baseUrl}/login/callback${formQuery}`

  res.contentType('text/html').send(`
          <html lang='fi'>
          <body>
            <h1>Devausympäristön AD-kirjautuminen</h1>
            <form action="${formUri}" method="post">
                ${userOptions.join('\n')}
                <div style="margin-top: 20px">
                  <button type="submit">Kirjaudu</button>
                </div>
            </form>
          </body>
          </html>
        `)
}

const verifyUser = async (
  req: express.Request
): Promise<EspooUserSessionUser> => {
  const preset = assertStringProp(req.body, 'preset')
  const person = await espooUserLogin(
    req,
    EspooUserLoginRequestParser.parse(JSON.parse(preset))
  )
  return {
    id: person.id,
    authType: 'dev',
    userType: 'ESPOO_USER'
  }
}

export function createDevAdRouter(sessions: Sessions<'espoo-user'>): Router {
  return createDevAuthRouter({
    sessions,
    root: '/kirjaudu',
    loginFormHandler,
    verifyUser
  })
}
