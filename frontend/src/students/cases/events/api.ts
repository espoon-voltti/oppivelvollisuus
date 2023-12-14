import { formatISO, parseISO } from 'date-fns'

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

export const apiGetCaseEvents = (studentCaseId: string): Promise<CaseEvent[]> =>
  apiClient
    .get<JsonOf<CaseEvent[]>>(`/student-cases/${studentCaseId}/case-events`)
    .then((res) =>
      res.data.map((e) => ({
        ...e,
        date: parseISO(e.date),
        created: {
          ...e.created,
          time: parseISO(e.created.time)
        },
        updated: e.updated
          ? {
              ...e.updated,
              time: parseISO(e.updated.time)
            }
          : null
      }))
    )

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
