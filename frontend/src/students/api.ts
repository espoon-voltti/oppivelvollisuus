import { formatISO, parseISO } from 'date-fns'

import { apiClient } from '../api-client'
import { JsonOf } from '../shared/api-utils'

export interface StudentInput {
  valpasLink: string
  ssn: string
  firstName: string
  lastName: string
  dateOfBirth: Date | null
}

export const apiPostStudent = (data: StudentInput): Promise<string> =>
  apiClient
    .post<string>('/students', {
      ...data,
      dateOfBirth: data.dateOfBirth
        ? formatISO(data.dateOfBirth, { representation: 'date' })
        : null
    })
    .then((res) => res.data)

export const apiPutStudent = (id: string, data: StudentInput): Promise<void> =>
  apiClient.put(`/students/${id}`, {
    ...data,
    dateOfBirth: data.dateOfBirth
      ? formatISO(data.dateOfBirth, { representation: 'date' })
      : null
  })

export interface StudentSummary {
  id: string
  firstName: string
  lastName: string
  openedAt: Date | null
}

export const apiGetStudents = (): Promise<StudentSummary[]> =>
  apiClient.get<JsonOf<StudentSummary[]>>('/students').then((res) =>
    res.data.map((s) => ({
      ...s,
      openedAt: s.openedAt ? parseISO(s.openedAt) : null
    }))
  )

export interface StudentDetails {
  id: string
  valpasLink: string
  ssn: string
  firstName: string
  lastName: string
  dateOfBirth: Date | null
}

export interface StudentResponse {
  student: StudentDetails
  cases: StudentCase[]
}

export const apiGetStudent = (id: string): Promise<StudentResponse> =>
  apiClient.get<JsonOf<StudentResponse>>(`/students/${id}`).then((res) => ({
    ...res.data,
    student: {
      ...res.data.student,
      dateOfBirth: res.data.student.dateOfBirth
        ? parseISO(res.data.student.dateOfBirth)
        : null
    },
    cases: res.data.cases.map((c) => ({
      ...c,
      openedAt: parseISO(c.openedAt)
    }))
  }))

export interface StudentCaseInput {
  openedAt: Date
  info: string
}

export interface StudentCase extends StudentCaseInput {
  id: string
  studentId: string
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

export const apiPutStudentCase = (
  studentId: string,
  caseId: string,
  data: StudentCaseInput
): Promise<string> =>
  apiClient.put(`/students/${studentId}/cases/${caseId}`, {
    ...data,
    openedAt: formatISO(data.openedAt, { representation: 'date' })
  })
