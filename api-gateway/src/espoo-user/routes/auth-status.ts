// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { appCommit } from '../../shared/config.ts'
import { toRequestHandler } from '../../shared/express.ts'
import type { EspooUserResponse } from '../../shared/service-client.ts'
import { getEspooUserDetails } from '../../shared/service-client.ts'
import type { Sessions } from '../../shared/session.ts'

export interface AuthStatus {
  loggedIn: boolean
  user?: EspooUserResponse
  apiVersion: string
}

export const espooUserAuthStatus = (sessions: Sessions<'espoo-user'>) =>
  toRequestHandler(async (req, res) => {
    const user = sessions.getUser(req)
    let status: AuthStatus
    if (user && user.id) {
      const data = await getEspooUserDetails(req, user.id)
      status = {
        loggedIn: true,
        user: data,
        apiVersion: appCommit
      }
    } else {
      status = { loggedIn: false, apiVersion: appCommit }
    }
    res.status(200).send(status)
  })
