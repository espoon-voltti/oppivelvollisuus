import { formatISO, parseISO } from 'date-fns'

import { apiClient } from '../api-client'
import { EmployeeBasics } from '../employees/api'
import { JsonOf } from '../shared/api-utils'

import {
  CaseEventType,
  CaseFinishedReason,
  CaseStatus,
  SchoolType
} from './enums'

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
  status: CaseStatus | null
  assignedTo: EmployeeBasics | null
}

export interface StudentSearchParams {
  query: string
  statuses: CaseStatus[]
  assignedTo: string | null
}

export const apiGetStudents = (
  params: StudentSearchParams
): Promise<StudentSummary[]> => {
  const body: JsonOf<StudentSearchParams> = params
  return apiClient
    .post<JsonOf<StudentSummary[]>>('/students/search', body)
    .then((res) =>
      res.data.map((s) => ({
        ...s,
        openedAt: s.openedAt ? parseISO(s.openedAt) : null
      }))
    )
}

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
  assignedTo: string | null
}

export type StudentCase = StudentCaseInput &
  CaseStatusInput & {
    id: string
    studentId: string
    assignedTo: EmployeeBasics | null
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
): Promise<void> => {
  const body: JsonOf<StudentCaseInput> = {
    ...data,
    openedAt: formatISO(data.openedAt, { representation: 'date' })
  }
  return apiClient.put(`/students/${studentId}/cases/${caseId}`, body)
}

export interface CaseEventInput {
  date: Date
  type: CaseEventType
  notes: string
}

export interface CaseEvent extends CaseEventInput {
  id: string
  studentCaseId: string
  created: ModifyInfo
  updated: ModifyInfo | null
}

interface ModifyInfo {
  name: string
  time: Date
}

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

export type FinishedInfo =
  | {
      reason: Exclude<CaseFinishedReason, 'BEGAN_STUDIES'>
      startedAtSchool: null
    }
  | {
      reason: 'BEGAN_STUDIES'
      startedAtSchool: SchoolType
    }

export type CaseStatusInput =
  | {
      status: Exclude<CaseStatus, 'FINISHED'>
      finishedInfo: null
    }
  | {
      status: 'FINISHED'
      finishedInfo: FinishedInfo
    }

export const apiPutStudentCaseStatus = (
  studentId: string,
  caseId: string,
  data: CaseStatusInput
): Promise<void> => {
  const body: JsonOf<CaseStatusInput> = data
  return apiClient.put(`/students/${studentId}/cases/${caseId}/status`, body)
}
