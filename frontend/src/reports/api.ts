// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { formatISO, parseISO } from 'date-fns'

import { apiClient } from '../api-client'
import { JsonOf } from '../shared/api-utils'
import {
  CaseBackgroundReason,
  CaseSource,
  NotInSchoolReason,
  OtherNotifier,
  PartnerOrganisation,
  SchoolBackground,
  ValpasNotifier
} from '../students/cases/enums'
import { CaseEventType } from '../students/cases/events/enums'
import {
  CaseFinishedReason,
  CaseStatus,
  FollowUpMeasure,
  SchoolType
} from '../students/cases/status/enums'
import { Gender } from '../students/enums'

export interface CasesReportRequest {
  start: Date | null
  end: Date | null
}

export interface CasesReportRow {
  openedAt: Date
  birthYear: number | null
  ageAtCaseOpened: number | null
  gender: Gender | null
  language: string
  municipalityInFinland: boolean
  status: CaseStatus
  finishedReason: CaseFinishedReason | null
  startedAtSchool: SchoolType | null
  source: CaseSource
  sourceValpas: ValpasNotifier | null
  sourceOther: OtherNotifier | null
  schoolBackground: SchoolBackground[]
  caseBackgroundReasons: CaseBackgroundReason[]
  notInSchoolReason: NotInSchoolReason | null
  partnerOrganisations: PartnerOrganisation[]
  eventTypes: CaseEventType[]
  followUpMeasures: FollowUpMeasure[] | null
  otherReason: string | null
}

export const apiGetCasesReport = async (
  request: CasesReportRequest
): Promise<CasesReportRow[]> => {
  const params: JsonOf<CasesReportRequest> = {
    start: request.start
      ? formatISO(request.start, { representation: 'date' })
      : null,
    end: request.end ? formatISO(request.end, { representation: 'date' }) : null
  }
  return apiClient
    .get<JsonOf<CasesReportRow[]>>('/reports/student-cases', { params })
    .then((res) =>
      res.data.map((row) => ({
        ...row,
        openedAt: parseISO(row.openedAt)
      }))
    )
}
