// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ValidateInResponseTo } from '@node-saml/node-saml'
import type { RedisClientOptions } from '@redis/client'

type EnvVariables = typeof envVariables
const envVariables = {
  // ----- Miscellaneous configuration -----
  /**
   * Environment name.
   *
   * This gets passed to log events in the `env` field, and to DataDog config `env` field.
   */
  VOLTTI_ENV: unset<string>(),
  /**
   * The port the HTTP server listens on
   */
  HTTP_PORT: 3000,

  // ----- Session configuration -----
  /**
   * Default timeout (in minutes) for idle sessions.
   */
  SESSION_TIMEOUT_MINUTES: 32,
  /**
   * If true, all cookies are set with the `Secure` flag.
   *
   * **Do not disable in production**
   */
  USE_SECURE_COOKIES: true,
  /**
   * Secret used for signing session cookies.
   *
   * Recommended to be a string of at least 32 random characters.
   */
  COOKIE_SECRET: unset<string>(),

  // ----- Redis configuration -----
  /**
   * Redis server hostname
   */
  REDIS_HOST: unset<string>(),
  /**
   * Redis server port
   */
  REDIS_PORT: unset<number>(),
  /**
   * Redis password
   */
  REDIS_PASSWORD: unset<string>(),
  /**
   * If true, connections to Redis are made without using TLS encryption
   */
  REDIS_DISABLE_SECURITY: false,
  /**
   * Redis server TLS hostname.
   *
   * Required if REDIS_DISABLE_SECURITY is false.
   */
  REDIS_TLS_SERVER_NAME: unset<string>(),

  // ----- Logging / debugging -----
  /**
   * Value for the `appBuild` field in log events.
   *
   * Normally set to a fixed value during Docker container build.
   */
  APP_BUILD: 'UNDEFINED',
  /**
   * Value for the `appCommit` field in log events.
   *
   * Normally set to a fixed value during Docker container build.
   */
  APP_COMMIT: 'UNDEFINED',
  /**
   * Value for the `hostIp` field in log events.
   *
   * Normally set by Docker container entrypoint script at container startup.
   */
  HOST_IP: 'UNDEFINED',
  /**
   * If true, includes the error message in all error responses.
   *
   * **Do not enable in production**
   */
  INCLUDE_ALL_ERROR_MESSAGES: false,
  /**
   * If true, outputs pretty-printed simple logs instead of JSON.
   */
  PRETTY_LOGS: false,

  // ----- DataDog tracing/profiling support -----
  /**
   * If true, enables DataDog tracing.
   */
  DD_TRACE_ENABLED: unset<boolean>(),
  /**
   * If true, enables DataDog profiling.
   */
  DD_PROFILING_ENABLED: unset<boolean>(),
  /**
   * Hostname for the DataDog agent
   */
  DD_TRACE_AGENT_HOSTNAME: 'localhost',
  /**
   * Port for the DataDog agent
   */
  DD_TRACE_AGENT_PORT: 8126,

  // ----- JWT token configuration -----
  /**
   * Path to a private key PEM file for JWT signing
   */
  JWT_PRIVATE_KEY: unset<string>(),
  /**
   * Value for the `keyid` field in generated JWT tokens
   */
  JWT_KID: 'oppivelvollisuus-api-gateway',
  /**
   * If true, automatically refreshes JWT tokens when near expiration.
   *
   * **Do not disable in production**
   */
  JWT_REFRESH_ENABLED: true,

  // ----- oppivelvollisuus frontend/backend URLs -----
  /**
   * Base URL for oppivelvollisuus frontend (e.g. https://oppivelvollisuus.example.com)
   */
  BASE_URL: unset<string>(),
  /**
   * Base URL for oppivelvollisuus-service backend (e.g. http://oppivelvollisuus-service.example.local)
   */
  SERVICE_URL: unset<string>(),

  // ----- Active Directory SAML authentication -----
  /**
   * If true, uses a mock authentication provider for AD, meant for local development only.
   *
   * **Do not enable in production**
   */
  AD_MOCK: false,
  /**
   * Hard-coded prefix for external IDs that originate from AD SAML authentication.
   *
   * When an AD user authenticates, a full external ID that looks like "prefix:id" is sent to oppivelvollisuus-service.
   */
  AD_SAML_EXTERNAL_ID_PREFIX: 'espoo-ad',
  /**
   * SAML authentication response attribute containing a persistent identifier for AD users.
   *
   * This attribute must be present in the SAML response or authentication will fail. When an AD user authenticates,
   * a full external ID that looks like "prefix:id" is sent to oppivelvollisuus-service.
   */
  AD_USER_ID_KEY:
    'http://schemas.microsoft.com/identity/claims/objectidentifier',
  /**
   * If true, assumes SAML responses from AD are encrypted using our SP public certificate.
   *
   * This depends on how AD is configured and may not be necessary.
   */
  AD_DECRYPT_ASSERTIONS: false,
  /**
   * URN for the NameIDFormat used in AD SAML requests
   */
  AD_NAME_ID_FORMAT: 'urn:oasis:names:tc:SAML:2.0:nameid-format:transient',
  /**
   * SAML service provider callback URL for AD authentication.
   */
  AD_SAML_CALLBACK_URL: unset<string>(),
  /**
   * SAML identity provider entrypoint URL for AD authentication.
   */
  AD_SAML_ENTRYPOINT_URL: unset<string>(),
  /**
   * SAML identity provider logout URL for AD authentication.
   */
  AD_SAML_LOGOUT_URL: unset<string>(),
  /**
   * SAML issuer for AD authentication.
   */
  AD_SAML_ISSUER: unset<string>(),
  /**
   * Comma-separated paths to SAML IDP public certificates in PEM format.
   */
  AD_SAML_PUBLIC_CERT: unset<string[]>(),
  /**
   * Path to SAML SP private certificate in PEM format
   */
  AD_SAML_PRIVATE_CERT: unset<string>()
}

// helper function to specify the type of undefined without casting
function unset<T>(): T | undefined {
  return undefined
}

/**
 * Returns environment variable overrides that are used only in local development
 */
function createLocalDevelopmentOverrides(): Partial<EnvVariables> {
  const isLocal = process.env.NODE_ENV === 'local'
  const isTest = process.env.NODE_ENV === 'test'

  return isLocal || isTest
    ? {
        VOLTTI_ENV: 'local',

        COOKIE_SECRET: 'A very hush hush cookie secret.',
        USE_SECURE_COOKIES: false,

        REDIS_HOST: '127.0.0.1',
        REDIS_PORT: 6379,
        REDIS_DISABLE_SECURITY: true,

        INCLUDE_ALL_ERROR_MESSAGES: true,
        PRETTY_LOGS: isLocal,

        JWT_PRIVATE_KEY: 'config/test-cert/jwt_private_key.pem',
        JWT_REFRESH_ENABLED: !isTest,

        BASE_URL: 'http://localhost:9000',
        SERVICE_URL: 'http://localhost:8080',

        AD_MOCK: true
      }
    : {}
}

export interface Config {
  espooUser: SessionConfig
  ad:
    | { type: 'mock' | 'disabled' }
    | {
        type: 'saml'
        externalIdPrefix: string
        userIdKey: string
        saml: OppivelvollisuusSamlConfig
      }
  redis: {
    host: string
    port: number | undefined
    password: string | undefined
    tlsServerName: string | undefined
    disableSecurity: boolean
  }
}

export interface SessionConfig {
  useSecureCookies: boolean
  cookieSecret: string
  sessionTimeoutMinutes: number
}

export const toRedisClientOpts = (config: Config): RedisClientOptions => ({
  socket: {
    host: config.redis.host,
    port: config.redis.port,
    ...(config.redis.disableSecurity
      ? undefined
      : { tls: true, servername: config.redis.tlsServerName })
  },
  ...(config.redis.disableSecurity
    ? undefined
    : { password: config.redis.password })
})

export interface OppivelvollisuusSamlConfig {
  callbackUrl: string
  entryPoint: string
  logoutUrl: string
  issuer: string
  publicCert: string | string[]
  privateCert: string
  validateInResponseTo: ValidateInResponseTo
  decryptAssertions: boolean
  acceptedClockSkewMs: number
  nameIdFormat?: string | undefined
}

function nonNullable<T>(
  value: T | undefined,
  errorMessage: string
): NonNullable<T> {
  if (value == null) {
    throw new Error(errorMessage)
  }
  return value
}

type Parser<T> = (value: string) => T

const unchanged: Parser<string> = (value) => value

const parseInteger: Parser<number> = (value) => {
  const result = Number.parseInt(value, 10)
  if (Number.isNaN(result)) throw new Error('Invalid integer')
  return result
}

const booleans: Record<string, boolean> = {
  1: true,
  0: false,
  true: true,
  false: false
}

const parseBoolean: Parser<boolean> = (value) => {
  if (value in booleans) return booleans[value]
  throw new Error('Invalid boolean')
}

function parseArray<T>(elementParser: Parser<T>, separator = ','): Parser<T[]> {
  return (value) => value.split(separator).map(elementParser)
}

function parseEnv<K extends keyof EnvVariables, T>(
  key: K,
  f: (value: string | undefined) => T
): T {
  const value = process.env[key]?.trim()
  try {
    return f(value == null || value === '' ? undefined : value)
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err)
    throw new Error(`${message}: ${key}=${value}`, { cause: err })
  }
}

const defaultEnvVariables: EnvVariables = {
  ...envVariables,
  ...createLocalDevelopmentOverrides()
}

const optional = <K extends keyof EnvVariables>(
  key: K,
  parser: Parser<EnvVariables[K]>
): EnvVariables[K] | undefined =>
  parseEnv(key, (value) => (value ? parser(value) : defaultEnvVariables[key]))

const required = <K extends keyof EnvVariables>(
  key: K,
  parser: Parser<NonNullable<EnvVariables[K]>>
): NonNullable<EnvVariables[K]> =>
  parseEnv(key, (value) =>
    value
      ? parser(value)
      : nonNullable(defaultEnvVariables[key], `${key} must be set`)
  )

export function configFromEnv(): Config {
  const adMock = required('AD_MOCK', parseBoolean)
  const ad: Config['ad'] = {
    ...(adMock
      ? { type: 'mock' }
      : {
          type: 'saml',
          externalIdPrefix: required('AD_SAML_EXTERNAL_ID_PREFIX', unchanged),
          userIdKey: required('AD_USER_ID_KEY', unchanged),
          saml: {
            callbackUrl: required('AD_SAML_CALLBACK_URL', unchanged),
            entryPoint: required('AD_SAML_ENTRYPOINT_URL', unchanged),
            logoutUrl: required('AD_SAML_LOGOUT_URL', unchanged),
            issuer: required('AD_SAML_ISSUER', unchanged),
            publicCert: required('AD_SAML_PUBLIC_CERT', parseArray(unchanged)),
            privateCert: required('AD_SAML_PRIVATE_CERT', unchanged),
            validateInResponseTo: ValidateInResponseTo.always,
            decryptAssertions: required('AD_DECRYPT_ASSERTIONS', parseBoolean),
            acceptedClockSkewMs: 0,
            nameIdFormat: required('AD_NAME_ID_FORMAT', unchanged)
          }
        })
  }

  const sessionTimeoutMinutes = required(
    'SESSION_TIMEOUT_MINUTES',
    parseInteger
  )

  return {
    espooUser: {
      useSecureCookies,
      cookieSecret: nonNullable(
        optional('COOKIE_SECRET', unchanged),
        'COOKIE_SECRET must be set'
      ),
      sessionTimeoutMinutes
    },
    ad,
    redis: {
      host: required('REDIS_HOST', unchanged),
      port: optional('REDIS_PORT', parseInteger),
      password: optional('REDIS_PASSWORD', unchanged),
      disableSecurity: required('REDIS_DISABLE_SECURITY', parseBoolean),
      tlsServerName: optional('REDIS_TLS_SERVER_NAME', unchanged)
    }
  }
}

export const appBuild = required('APP_BUILD', unchanged)
export const appCommit = required('APP_COMMIT', unchanged)
export const hostIp = required('HOST_IP', unchanged)
export const includeAllErrorMessages = required(
  'INCLUDE_ALL_ERROR_MESSAGES',
  parseBoolean
)

export const tracingEnabled = optional('DD_TRACE_ENABLED', parseBoolean)
export const profilingEnabled = optional('DD_PROFILING_ENABLED', parseBoolean)
export const traceAgentHostname = optional('DD_TRACE_AGENT_HOSTNAME', unchanged)
export const traceAgentPort = optional('DD_TRACE_AGENT_PORT', parseInteger)

export const jwtPrivateKey = required('JWT_PRIVATE_KEY', unchanged)
export const jwtRefreshEnabled = required('JWT_REFRESH_ENABLED', parseBoolean)

export const serviceName = 'oppivelvollisuus-api-gateway'
export const jwtKid = required('JWT_KID', unchanged)

export const oppivelvollisuusBaseUrl = new URL(required('BASE_URL', unchanged))
export const oppivelvollisuusServiceUrl = required('SERVICE_URL', unchanged)
export const useSecureCookies = required('USE_SECURE_COOKIES', parseBoolean)

export const prettyLogs = required('PRETTY_LOGS', parseBoolean)

export const volttiEnv = optional('VOLTTI_ENV', unchanged)

export const httpPort = required('HTTP_PORT', parseInteger)
