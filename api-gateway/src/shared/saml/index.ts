// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { readFileSync } from 'node:fs'

import type { CacheProvider, Profile, SamlConfig } from '@node-saml/node-saml'
import type express from 'express'
import { z } from 'zod'

import type { OppivelvollisuusSessionUser } from '../auth/index.ts'
import type { OppivelvollisuusSamlConfig } from '../config.ts'
import { oppivelvollisuusBaseUrl } from '../config.ts'
import { logError } from '../logging.ts'
import { parseUrlWithOrigin } from '../parse-url-with-origin.ts'

export function createSamlConfig(
  config: OppivelvollisuusSamlConfig,
  cacheProvider?: CacheProvider,
  wantAuthnResponseSigned = true
): SamlConfig {
  const privateCert = readFileSync(config.privateCert, {
    encoding: 'utf8'
  })
  const loadPublicCert = (cert: string) =>
    readFileSync(cert, { encoding: 'utf8' })
  const publicCert = Array.isArray(config.publicCert)
    ? config.publicCert.map(loadPublicCert)
    : loadPublicCert(config.publicCert)

  return {
    acceptedClockSkewMs: config.acceptedClockSkewMs,
    audience: config.issuer,
    cacheProvider,
    callbackUrl: config.callbackUrl,
    idpCert: publicCert,
    disableRequestedAuthnContext: true,
    decryptionPvk: config.decryptAssertions ? privateCert : undefined,
    entryPoint: config.entryPoint,
    identifierFormat:
      config.nameIdFormat ??
      'urn:oasis:names:tc:SAML:2.0:nameid-format:transient',
    issuer: config.issuer,
    logoutUrl: config.logoutUrl,
    privateKey: privateCert,
    signatureAlgorithm: 'sha256',
    validateInResponseTo: config.validateInResponseTo,
    wantAssertionsSigned: true,
    wantAuthnResponseSigned
  }
}

export type AuthenticateProfile = (
  req: express.Request,
  profile: Profile
) => Promise<OppivelvollisuusSessionUser>

export function authenticateProfile<T>(
  schema: z.ZodType<T>,
  authenticate: (
    req: express.Request,
    samlSession: SamlSession,
    profile: T
  ) => Promise<OppivelvollisuusSessionUser>
): AuthenticateProfile {
  return async (req, profile) => {
    const samlSession = SamlSessionSchema.parse(profile)
    const parseResult = schema.safeParse(profile)
    if (parseResult.success) {
      return await authenticate(req, samlSession, parseResult.data)
    } else {
      throw new Error(
        `SAML ${profile.issuer} profile parsing failed: ${parseResult.error.message}`
      )
    }
  }
}

export const SamlProfileIdSchema = z.object({
  nameID: z.string(),
  nameIDFormat: z.string()
})

export type SamlSession = z.infer<typeof SamlSessionSchema>

// A subset of SAML Profile fields that are expected to be present in valid SAML sessions
export const SamlSessionSchema = z.object({
  issuer: z.string(),
  nameID: z.string(),
  nameIDFormat: z.string(),
  sessionIndex: z.string().optional(),
  nameQualifier: z.string().optional(),
  spNameQualifier: z.string().optional()
})

export function getRawUnvalidatedRelayState(
  req: express.Request
): string | undefined {
  // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access,@typescript-eslint/no-unsafe-assignment
  const relayState = req.body?.RelayState || req.query.RelayState
  return typeof relayState === 'string' ? relayState : undefined
}

// SAML RelayState is an arbitrary string that gets passed in a SAML transaction.
// In our case, we specify it to be a redirect URL where the user should be
// redirected to after the SAML transaction is complete. Since the RelayState
// is not signed or encrypted, we must make sure the URL points to our application
// and not to some 3rd party domain
export function validateRelayStateUrl(req: express.Request): URL | undefined {
  const relayState = getRawUnvalidatedRelayState(req)
  if (relayState) {
    const url = parseUrlWithOrigin(oppivelvollisuusBaseUrl, relayState)
    if (url) return url
    logError('Invalid RelayState in request', req)
  }
  return undefined
}
