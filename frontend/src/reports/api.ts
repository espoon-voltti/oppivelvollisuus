import { parseISO } from 'date-fns'

import { apiClient } from '../api-client'
import { JsonOf } from '../shared/api-utils'
import {
  CaseBackgroundReason,
  CaseSource,
  NotInSchoolReason,
  OtherNotifier,
  SchoolBackground,
  ValpasNotifier
} from '../students/cases/enums'
import { CaseEventType } from '../students/cases/events/enums'
import {
  CaseFinishedReason,
  CaseStatus,
  SchoolType
} from '../students/cases/status/enums'
import { Gender } from '../students/enums'

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
  eventTypes: CaseEventType[]
}

export const apiGetCasesReport = (): Promise<CasesReportRow[]> =>
  apiClient
    .get<JsonOf<CasesReportRow[]>>('/reports/student-cases')
    .then((res) =>
      res.data.map((row) => ({
        ...row,
        openedAt: parseISO(row.openedAt)
      }))
    )
