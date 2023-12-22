import { faChevronLeft } from '@fortawesome/free-solid-svg-icons'
import { faDownload } from '@fortawesome/free-solid-svg-icons/faDownload'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useEffect, useState } from 'react'
import { CSVLink } from 'react-csv'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { InlineButton } from '../shared/buttons/InlineButton'
import { formatDate } from '../shared/dates'
import {
  FlexRowWithGaps,
  PageContainer,
  SectionContainer,
  VerticalGap
} from '../shared/layout'
import { H2 } from '../shared/typography'
import { caseSourceNames } from '../students/cases/enums'
import {
  caseFinishedReasonNames,
  caseStatusNames,
  schoolTypeNames
} from '../students/cases/status/enums'
import { genderNames } from '../students/enums'

import { apiGetCasesReport, CasesReportRow } from './api'

const StyledDownloadButton = styled(CSVLink)`
  font-weight: 600;
`

export const ReportsPage = React.memo(function ReportsPage() {
  const navigate = useNavigate()

  const [rows, setRows] = useState<CasesReportRow[] | null>(null)
  useEffect(() => {
    void apiGetCasesReport().then(setRows)
  }, [])

  return (
    <PageContainer>
      <SectionContainer>
        <InlineButton
          text="Takaisin"
          icon={faChevronLeft}
          onClick={() => navigate('/oppivelvolliset')}
        />
        <VerticalGap $size="m" />
        <H2>Ilmoitukset raportti</H2>
        <VerticalGap $size="L" />
        {rows && (
          <StyledDownloadButton
            data={rows.map((r) => ({
              openedAt: formatDate(r.openedAt),
              source: caseSourceNames[r.source],
              gender: r.gender ? genderNames[r.gender] : '',
              language: r.language,
              status: caseStatusNames[r.status],
              finishedReason: r.finishedReason
                ? caseFinishedReasonNames[r.finishedReason]
                : '',
              startedAtSchool: r.startedAtSchool
                ? schoolTypeNames[r.startedAtSchool]
                : ''
            }))}
            headers={[
              { key: 'openedAt', label: 'Vastaanotettu' },
              { key: 'source', label: 'Lähde' },
              { key: 'gender', label: 'Sukupuoli' },
              { key: 'language', label: 'Äidinkieli' },
              { key: 'status', label: 'Ohjauksen tila' },
              { key: 'finishedReason', label: 'Ohjauksen päättymisen syy' },
              { key: 'startedAtSchool', label: 'Siirtynyt opiskelemaan' }
            ]}
            separator=";"
            filename="oppivelvollisuusilmoitukset.csv"
          >
            <FlexRowWithGaps>
              <FontAwesomeIcon icon={faDownload} />
              <span>Lataa raportti</span>
            </FlexRowWithGaps>
          </StyledDownloadButton>
        )}
      </SectionContainer>
    </PageContainer>
  )
})
