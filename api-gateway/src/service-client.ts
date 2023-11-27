import express from 'express'
import axios from 'axios'
import { createAuthHeader, AppSessionUser } from './auth/index.js'
import { serviceUrl } from './config.js'

export const client = axios.create({
  baseURL: serviceUrl
})

const systemUser: AppSessionUser = {
  id: 'oppivelvollisuus-system-user'
}

export type ServiceRequestHeader = 'Authorization' | 'X-Request-ID'

export type ServiceRequestHeaders = { [H in ServiceRequestHeader]?: string }

export function createServiceRequestHeaders(
  req: express.Request | undefined,
  user: AppSessionUser | undefined | null = req?.user
) {
  const headers: ServiceRequestHeaders = {}
  if (user) {
    headers.Authorization = createAuthHeader(user)
  }
  return headers
}

export interface AdLoginRequest {
  externalId: string
  firstName: string
  lastName: string
  email?: string | null
}

// currently same
export type EmployeeUser = AdLoginRequest

export async function employeeLogin(employee: AdLoginRequest) {
  const res = await client.post<EmployeeUser>(
    `/system/employee-login`,
    employee,
    {
      headers: createServiceRequestHeaders(undefined, systemUser)
    }
  )
  return res.data
}

export async function getEmployeeDetails(
  req: express.Request,
  employeeId: string
) {
  const { data } = await client.get<EmployeeUser | undefined>(
    `/system/employee/${employeeId}`,
    {
      headers: createServiceRequestHeaders(req, systemUser)
    }
  )
  return data
}
