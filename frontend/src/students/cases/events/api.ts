// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { formatISO } from 'date-fns'

import { apiClient } from '../../../api-client'
import { JsonOf } from '../../../shared/api-utils'

import { CaseEventType } from './enums'

export const apiPostCaseEvent = (
  studentCaseId: string,
  data: CaseEventInput
): Promise<string> => {
  const body: JsonOf<CaseEventInput> = {
    ...data,
    date: formatISO(data.date, { representation: 'date' })
  }
  return apiClient
    .post<string>(`/student-cases/${studentCaseId}/case-events`, body)
    .then((res) => res.data)
}

export const apiPutCaseEvent = (
  caseEventId: string,
  data: CaseEventInput
): Promise<void> => {
  const body: JsonOf<CaseEventInput> = {
    ...data,
    date: formatISO(data.date, { representation: 'date' })
  }
  return apiClient.put(`/case-events/${caseEventId}`, body)
}

export const apiDeleteCaseEvent = (caseEventId: string): Promise<void> =>
  apiClient.delete(`/case-events/${caseEventId}`)

export interface CaseEvent extends CaseEventInput {
  id: string
  studentCaseId: string
  created: ModifyInfo
  updated: ModifyInfo | null
}

export interface CaseEventInput {
  date: Date
  type: CaseEventType
  notes: string
}

interface ModifyInfo {
  name: string
  time: Date
}
