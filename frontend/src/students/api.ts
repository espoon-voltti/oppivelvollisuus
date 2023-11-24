import { formatISO, parseISO } from 'date-fns'

import { apiClient } from '../api-client'
import { JsonOf } from '../shared/api-utils'

export interface StudentInput {
  valpasLink: string
  ssn: string
  firstName: string
  lastName: string
  dateOfBirth: Date | null
  phone: string
  email: string
  address: string
}

export interface StudentAndCaseInput {
  student: StudentInput
  studentCase: StudentCaseInput
}

export const apiPostStudent = (data: StudentAndCaseInput): Promise<string> => {
  const body: JsonOf<StudentAndCaseInput> = {
    ...data,
    student: {
      ...data.student,
      dateOfBirth: data.student.dateOfBirth
        ? formatISO(data.student.dateOfBirth, { representation: 'date' })
        : null
    },
    studentCase: {
      ...data.studentCase,
      openedAt: formatISO(data.studentCase.openedAt, { representation: 'date' })
    }
  }
  return apiClient.post<string>('/students', body).then((res) => res.data)
}

export const apiPutStudent = (
  id: string,
  data: StudentInput
): Promise<void> => {
  const body: JsonOf<StudentInput> = {
    ...data,
    dateOfBirth: data.dateOfBirth
      ? formatISO(data.dateOfBirth, { representation: 'date' })
      : null
  }
  return apiClient.put(`/students/${id}`, body)
}

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
  phone: string
  email: string
  address: string
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
): Promise<string> => {
  const body: JsonOf<StudentCaseInput> = {
    ...data,
    openedAt: formatISO(data.openedAt, { representation: 'date' })
  }
  return apiClient
    .post<string>(`/students/${studentId}/cases`, body)
    .then((res) => res.data)
}

export const apiPutStudentCase = (
  studentId: string,
  caseId: string,
  data: StudentCaseInput
): Promise<string> => {
  const body: JsonOf<StudentCaseInput> = {
    ...data,
    openedAt: formatISO(data.openedAt, { representation: 'date' })
  }
  return apiClient.put(`/students/${studentId}/cases/${caseId}`, body)
}
