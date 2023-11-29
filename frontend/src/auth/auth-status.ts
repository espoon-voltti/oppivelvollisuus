import { useEffect, useState } from 'react'

import { apiClient } from '../api-client'
import { JsonOf } from '../shared/api-utils'

export interface User {
  externalId: string
  firstName: string
  lastName: string
  email?: string | null
}

export interface AuthStatus {
  loggedIn: boolean
  user?: User
  apiVersion: string
}

async function getAuthStatus(): Promise<AuthStatus> {
  return apiClient
    .get<JsonOf<AuthStatus>>('/auth/status')
    .then((res) => res.data)
}

export function useAuthStatus(): AuthStatus | undefined {
  const [authStatus, setAuthStatus] = useState<AuthStatus>()

  useEffect(() => {
    void getAuthStatus().then(setAuthStatus)
  }, [])

  return authStatus
}
