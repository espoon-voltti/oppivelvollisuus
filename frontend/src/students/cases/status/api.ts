// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { apiClient } from '../../../api-client'
import { JsonOf } from '../../../shared/api-utils'

import { CaseFinishedReason, CaseStatus, SchoolType } from './enums'

export const apiMarkCaseAsDuplicate = (
  caseId: string,
  targetCaseId: string
): Promise<void> => {
  return apiClient.post(`/student-cases/${caseId}/mark-as-duplicate`, {
    targetCaseId
  })
}

export const apiPutStudentCaseStatus = (
  studentId: string,
  caseId: string,
  data: CaseStatusInput
): Promise<void> => {
  const body: JsonOf<CaseStatusInput> = data
  return apiClient.put(`/students/${studentId}/cases/${caseId}/status`, body)
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

export type FinishedInfo =
  | {
      reason: Exclude<CaseFinishedReason, 'BEGAN_STUDIES' | 'OTHER'>
      startedAtSchool: null
      otherReason: null
    }
  | {
      reason: 'BEGAN_STUDIES'
      startedAtSchool: SchoolType
      otherReason: null
    }
  | {
      reason: 'OTHER'
      startedAtSchool: null
      otherReason: string | null
    }
