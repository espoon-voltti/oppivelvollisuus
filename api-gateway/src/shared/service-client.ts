// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios from 'axios'
import type express from 'express'

import { systemUserHeader } from './auth/index.ts'
import { getJwt } from './auth/jwt.ts'
import { oppivelvollisuusServiceUrl } from './config.ts'

export const client = axios.create({
  baseURL: oppivelvollisuusServiceUrl
})

export type UUID = string

export type ServiceRequestHeader = 'Authorization' | 'X-Request-ID' | 'X-User'

export type ServiceRequestHeaders = Partial<
  Record<ServiceRequestHeader, string>
>

export function createServiceRequestHeaders(
  req: express.Request,
  userHeader: string | undefined
) {
  const headers: ServiceRequestHeaders = {
    Authorization: `Bearer ${getJwt()}`
  }
  if (userHeader) {
    headers['X-User'] = userHeader
  }
  if (req.traceId) {
    headers['X-Request-ID'] = req.traceId
  }
  return headers
}

export interface EspooUserLoginRequest {
  externalId: string
  firstName: string
  lastName: string
  email?: string
}

export interface EspooUserResponse {
  id: string
  externalId: string
  firstName: string
  lastName: string
  email: string | null
}

export async function espooUserLogin(
  req: express.Request,
  loginRequest: EspooUserLoginRequest
): Promise<EspooUserResponse> {
  const { data } = await client.post<EspooUserResponse>(
    `/system/user-login`,
    loginRequest,
    { headers: createServiceRequestHeaders(req, systemUserHeader) }
  )
  return data
}

export async function getEspooUserDetails(
  req: express.Request,
  espooUserId: string
): Promise<EspooUserResponse | undefined> {
  try {
    const { data } = await client.get<EspooUserResponse>(
      `/system/users/${espooUserId}`,
      { headers: createServiceRequestHeaders(req, systemUserHeader) }
    )
    return data
  } catch (e: unknown) {
    if (axios.isAxiosError(e) && e.response?.status === 404) {
      return undefined
    } else {
      throw e
    }
  }
}
