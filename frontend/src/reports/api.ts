import { CaseSource } from '../students/cases/enums'
import { Gender } from '../students/enums'
import { JsonOf } from '../shared/api-utils'
import { apiClient } from '../api-client'
import { parseISO } from 'date-fns'
import { CaseFinishedReason, CaseStatus, SchoolType } from '../students/cases/status/enums'

export interface CasesReportRow {
  openedAt: Date
  source: CaseSource
  gender: Gender | null
  language: string
  status: CaseStatus
  finishedReason: CaseFinishedReason | null
  startedAtSchool: SchoolType | null
}

export const apiGetCasesReport = (): Promise<CasesReportRow[]> => {
  return apiClient
    .get<JsonOf<CasesReportRow[]>>('/reports/student-cases')
    .then((res) =>
      res.data.map((row) => ({
        ...row,
        openedAt: parseISO(row.openedAt)
      }))
    )
}
