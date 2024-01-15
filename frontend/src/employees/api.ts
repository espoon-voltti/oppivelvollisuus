// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { apiClient } from '../api-client'
import { JsonOf } from '../shared/api-utils'

export interface EmployeeUser {
  id: string
  externalId: string
  firstName: string
  lastName: string
  email?: string | null
}

export interface EmployeeBasics {
  id: string
  name: string
}

export const apiGetEmployees = (): Promise<EmployeeUser[]> =>
  apiClient.get<JsonOf<EmployeeUser[]>>('/employees').then((res) => res.data)
