import { formatISO } from 'date-fns'

import { apiClient } from '../../api-client'
import { EmployeeBasics } from '../../employees/api'
import { JsonOf } from '../../shared/api-utils'

import {
  CaseBackgroundReason,
  NotInSchoolReason,
  OtherNotifier,
  SchoolBackground,
  ValpasNotifier
} from './enums'
import { CaseEvent } from './events/api'
import { CaseStatusInput } from './status/api'

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

export const apiDeleteStudentCase = (
  studentId: string,
  caseId: string
): Promise<void> => apiClient.delete(`/students/${studentId}/cases/${caseId}`)

export type StudentCase = StudentCaseInput &
  CaseStatusInput & {
    id: string
    studentId: string
    assignedTo: EmployeeBasics | null
    events: CaseEvent[]
  }

export type StudentCaseInput = {
  openedAt: Date
  assignedTo: string | null
  sourceContact: string
  schoolBackground: SchoolBackground[]
  caseBackgroundReasons: CaseBackgroundReason[]
  notInSchoolReason: NotInSchoolReason | null
} & CaseSourceFields

export type CaseSourceFields =
  | {
      source: 'VALPAS_NOTICE'
      sourceValpas: ValpasNotifier
      sourceOther: null
    }
  | {
      source: 'VALPAS_AUTOMATIC_CHECK'
      sourceValpas: null
      sourceOther: null
    }
  | {
      source: 'OTHER'
      sourceValpas: null
      sourceOther: OtherNotifier
    }
