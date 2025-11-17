// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { apiClient } from '../../../api-client'
import { JsonOf } from '../../../shared/api-utils'

import {
  CaseFinishedReason,
  CaseStatus,
  FollowUpMeasure,
  SchoolType
} from './enums'

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
      reason: Exclude<
        CaseFinishedReason,
        'BEGAN_STUDIES' | 'COMPULSORY_EDUCATION_ENDED' | 'OTHER'
      >
      startedAtSchool: null
      followUpMeasures: null
      otherReason: null
    }
  | {
      reason: 'BEGAN_STUDIES'
      startedAtSchool: SchoolType
      followUpMeasures: null
      otherReason: null
    }
  | {
      reason: 'COMPULSORY_EDUCATION_ENDED'
      startedAtSchool: null
      followUpMeasures: FollowUpMeasure[]
      otherReason: null
    }
  | {
      reason: 'OTHER'
      startedAtSchool: null
      followUpMeasures: null
      otherReason: string | null
    }
