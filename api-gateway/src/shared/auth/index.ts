// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Profile } from '@node-saml/node-saml'

import type { SamlSession } from '../saml/index.ts'

export type EspooUserSessionUser =
  | {
      id: string
      authType: 'ad'
      userType: 'ESPOO_USER'
      samlSession: SamlSession
    }
  | {
      id: string
      authType: 'dev'
      userType: 'ESPOO_USER'
    }

export type OppivelvollisuusSessionUser = EspooUserSessionUser

export function createUserHeader(user: OppivelvollisuusSessionUser): string {
  return JSON.stringify({ type: 'espooUser', id: user.id })
}

export const systemUserHeader = JSON.stringify({ type: 'system' })

export function createLogoutToken(profile: Profile) {
  return `${profile.nameID}:::${profile.sessionIndex}`
}
