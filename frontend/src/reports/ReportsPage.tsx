import React, { useEffect, useState } from 'react'
import { PageContainer, SectionContainer } from '../shared/layout'
import { H2 } from '../shared/typography'
import { apiGetCasesReport, CasesReportRow } from './api'
import { CSVLink } from 'react-csv'
import { formatDate } from '../shared/dates'
import { caseSourceNames } from '../students/cases/enums'
import { genderNames } from '../students/enums'
import { caseFinishedReasonNames, caseStatusNames, schoolTypeNames } from '../students/cases/status/enums'

export const ReportsPage = React.memo(function ReportsPage() {
  const [rows, setRows] = useState<CasesReportRow[] | null>(null)
  useEffect(() => {
    void apiGetCasesReport().then(setRows)
  }, [])

  return (
    <PageContainer>
      <SectionContainer>
        <H2>Ilmoitukset raportti</H2>
        {rows && (
          <CSVLink
            data={rows.map(r => ({
              openedAt: formatDate(r.openedAt),
              source: caseSourceNames[r.source],
              gender: r.gender ? genderNames[r.gender] : '',
              language: r.language,
              status: caseStatusNames[r.status],
              finishedReason: r.finishedReason ? caseFinishedReasonNames[r.finishedReason] : '',
              startedAtSchool: r.startedAtSchool ? schoolTypeNames[r.startedAtSchool] : '',
            }))}
            headers={[
              {key: 'openedAt', label: 'Vastaanotettu'},
              {key: 'source', label: 'Lähde'},
              {key: 'gender', label: 'Sukupuoli'},
              {key: 'language', label: 'Äidinkieli'},
              {key: 'status', label: 'Ohjauksen tila'},
              {key: 'finishedReason', label: 'Ohjauksen päättymisen syy'},
              {key: 'startedAtSchool', label: 'Siirtynyt opiskelemaan'},
            ]}
            separator=";"
            filename="oppivelvollisuusilmoitukset.csv"
          >
            Lataa raportti
          </CSVLink>
        )}
      </SectionContainer>
    </PageContainer>
  )
})
