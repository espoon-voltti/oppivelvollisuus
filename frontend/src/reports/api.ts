import { parseISO } from 'date-fns'

import { apiClient } from '../api-client'
import { JsonOf } from '../shared/api-utils'
import { CaseSource } from '../students/cases/enums'
import {
  CaseFinishedReason,
  CaseStatus,
  SchoolType
} from '../students/cases/status/enums'
import { Gender } from '../students/enums'

export interface CasesReportRow {
  openedAt: Date
  source: CaseSource
  gender: Gender | null
  language: string
  status: CaseStatus
  finishedReason: CaseFinishedReason | null
  startedAtSchool: SchoolType | null
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
