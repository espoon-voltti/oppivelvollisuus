import { formatISO, parseISO } from 'date-fns'

import { apiClient } from '../api-client'

export interface StudentInput {
  firstName: string
  lastName: string
}

export const apiPostStudent = (data: StudentInput): Promise<string> =>
  apiClient.post<string>('/students', data).then((res) => res.data)

export const apiPutStudent = (id: string, data: StudentInput): Promise<void> =>
  apiClient.put(`/students/${id}`, data)

export interface StudentBasics {
  id: string
  firstName: string
  lastName: string
}

export const apiGetStudents = (): Promise<StudentBasics[]> =>
  apiClient.get<StudentBasics[]>('/students').then((res) => res.data)

export const apiGetStudent = (id: string): Promise<StudentBasics> =>
  apiClient.get<StudentBasics>(`/students/${id}`).then((res) => res.data)

export interface StudentCaseInput {
  openedAt: Date
  info: string
}

export const apiPostStudentCase = (
  studentId: string,
  data: StudentCaseInput
): Promise<string> =>
  apiClient
    .post<string>(`/students/${studentId}/cases`, {
      ...data,
      openedAt: formatISO(data.openedAt, { representation: 'date' })
    })
    .then((res) => res.data)

export interface StudentCase extends StudentCaseInput {
  id: string
  studentId: string
}

type StudentCaseJson = StudentCase & {
  openedAt: string
}

export const apiGetStudentCasesByStudent = (
  studentId: string
): Promise<StudentCase[]> =>
  apiClient.get<StudentCaseJson[]>(`/students/${studentId}/cases`).then((res) =>
    res.data.map((row) => ({
      ...row,
      openedAt: parseISO(row.openedAt)
    }))
  )

export const apiPutStudentCase = (
  studentId: string,
  caseId: string,
  data: StudentCaseInput
): Promise<string> =>
  apiClient.put(`/students/${studentId}/cases/${caseId}`, {
    ...data,
    openedAt: formatISO(data.openedAt, { representation: 'date' })
  })

export interface StudentCaseSummary {
  id: string
  studentId: string
  firstName: string
  lastName: string
  openedAt: Date
}
type StudentCaseSummaryJson = StudentCaseSummary & {
  openedAt: string
}
export const apiGetStudentCases = (): Promise<StudentCaseSummary[]> =>
  apiClient.get<StudentCaseSummaryJson[]>('/students-cases').then((res) =>
    res.data.map((row) => ({
      ...row,
      openedAt: parseISO(row.openedAt)
    }))
  )
