import { formatISO, parseISO } from 'date-fns'

import { apiClient } from '../api-client'
import { EmployeeBasics } from '../employees/api'
import { JsonOf } from '../shared/api-utils'

import { StudentCase, StudentCaseInput } from './cases/api'
import { CaseStatus } from './cases/status/enums'
import { Gender } from './enums'

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

export const apiGetPossibleDuplicateStudents = (
  input: DuplicateStudentCheckInput
): Promise<DuplicateStudent[]> => {
  const body: JsonOf<DuplicateStudentCheckInput> = input
  return apiClient
    .post<JsonOf<DuplicateStudent[]>>('/students/duplicates', body)
    .then((res) =>
      res.data.map((s) => ({
        ...s,
        dateOfBirth: s.dateOfBirth ? parseISO(s.dateOfBirth) : null
      }))
    )
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
      openedAt: parseISO(c.openedAt),
      events: c.events.map((e) => ({
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
    }))
  }))

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

export const apiDeleteStudent = (id: string): Promise<void> =>
  apiClient.delete(`/students/${id}`)

export interface StudentResponse {
  student: StudentDetails
  cases: StudentCase[]
}

export interface StudentDetails extends StudentInput {
  id: string
}

export interface StudentInput {
  valpasLink: string
  ssn: string
  firstName: string
  lastName: string
  language: string
  dateOfBirth: Date | null
  phone: string
  email: string
  gender: Gender | null
  address: string
  municipalityInFinland: boolean
  guardianInfo: string
  supportContactsInfo: string
}

export interface StudentAndCaseInput {
  student: StudentInput
  studentCase: StudentCaseInput
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

export interface DuplicateStudentCheckInput {
  ssn: string
  valpasLink: string
  firstName: string
  lastName: string
}

export interface DuplicateStudent {
  id: string
  name: string
  dateOfBirth: Date | null
  matchingSsn: boolean
  matchingValpasLink: boolean
  matchingName: boolean
}
