// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faChevronLeft } from '@fortawesome/free-solid-svg-icons'
import { faDownload } from '@fortawesome/free-solid-svg-icons/faDownload'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useMemo, useState } from 'react'
import { CSVLink } from 'react-csv'
import { useNavigate } from 'react-router'
import styled from 'styled-components'

import { Button } from '../shared/buttons/Button'
import { InlineButton } from '../shared/buttons/InlineButton'
import { formatDate, parseDate } from '../shared/dates'
import { InputField } from '../shared/form/InputField'
import {
  FlexRowWithGaps,
  LabeledInput,
  PageContainer,
  SectionContainer,
  VerticalGap
} from '../shared/layout'
import { H2, Label, P } from '../shared/typography'
import {
  caseBackgroundReasonNames,
  caseBackgroundReasonValues,
  caseSourceNames,
  notInSchoolReasonNames,
  otherNotifierNames,
  schoolBackgroundNames,
  schoolBackgrounds,
  valpasNotifierNames
} from '../students/cases/enums'
import {
  caseEventTypeNames,
  caseEventTypes
} from '../students/cases/events/enums'
import {
  caseFinishedReasonNames,
  caseStatusNames,
  followUpMeasureNames,
  schoolTypeNames
} from '../students/cases/status/enums'
import { genderNames } from '../students/enums'

import { apiGetCasesReport, CasesReportRequest, CasesReportRow } from './api'

const StyledDownloadButton = styled(CSVLink)`
  font-weight: 600;
`

export const ReportsPage = React.memo(function ReportsPage() {
  const navigate = useNavigate()

  const [startDate, setStartDate] = useState<string>('')
  const [endDate, setEndDate] = useState<string>('')
  const [submitting, setSubmitting] = useState(false)
  const validRequest: CasesReportRequest | null = useMemo(() => {
    const start = startDate.length > 0 ? parseDate(startDate) : null
    const end = endDate.length > 0 ? parseDate(endDate) : null
    if (start === undefined || end === undefined) {
      return null
    }
    return { start, end }
  }, [startDate, endDate])
  const [rows, setRows] = useState<CasesReportRow[] | null>(null)

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
        <P>
          Raportti listaa oppivelvollisuusilmoituksia ja niihin liittyviä
          tietoja. Raportin voi ladata .csv muodossa, avata esimerkiksi
          Excelissä ja siellä jalostaa tiedoista kulloinkin tarvittavat
          tilastot.
        </P>
        <VerticalGap $size="L" />
        <FlexRowWithGaps $gapSize="L">
          <LabeledInput $cols={2}>
            <Label>Alkupäivä</Label>
            <InputField
              value={startDate}
              onChange={(value) => {
                setRows(null)
                setStartDate(value)
              }}
            />
          </LabeledInput>
          <LabeledInput $cols={2}>
            <Label>Loppupäivä</Label>
            <InputField
              value={endDate}
              onChange={(value) => {
                setRows(null)
                setEndDate(value)
              }}
            />
          </LabeledInput>
          <Button
            text="Hae raportti"
            disabled={submitting || !validRequest}
            onClick={() => {
              if (!validRequest) return
              setSubmitting(true)
              void apiGetCasesReport(validRequest)
                .then(setRows)
                .finally(() => setSubmitting(false))
            }}
          />
        </FlexRowWithGaps>
        <VerticalGap $size="L" />
        {rows && (
          <StyledDownloadButton
            data={rows.map((r) => ({
              openedAt: formatDate(r.openedAt),
              birthYear: r.birthYear !== null ? r.birthYear : '',
              ageAtCaseOpened:
                r.ageAtCaseOpened !== null ? r.ageAtCaseOpened : '',
              gender: r.gender ? genderNames[r.gender] : '',
              language: r.language,
              municipalityInFinland: r.municipalityInFinland ? 'Kyllä' : 'Ei',
              status: caseStatusNames[r.status],
              finishedReason: r.finishedReason
                ? caseFinishedReasonNames[r.finishedReason]
                : '',
              startedAtSchool: r.startedAtSchool
                ? schoolTypeNames[r.startedAtSchool]
                : '',
              followUpMeasures: r.followUpMeasures
                ? r.followUpMeasures
                    .map((m) => m && followUpMeasureNames[m])
                    .join(', ')
                : '',
              source: caseSourceNames[r.source],
              sourceDetails: r.sourceValpas
                ? valpasNotifierNames[r.sourceValpas]
                : r.sourceOther
                  ? otherNotifierNames[r.sourceOther]
                  : '',
              ...schoolBackgrounds.reduce(
                (acc, val) => ({
                  ...acc,
                  [`schoolBackground_${val}`]: r.schoolBackground.includes(val)
                    ? 'Kyllä'
                    : 'Ei'
                }),
                {}
              ),
              ...caseBackgroundReasonValues.reduce(
                (acc, val) => ({
                  ...acc,
                  [`caseBackgroundReason_${val}`]:
                    r.caseBackgroundReasons.includes(val) ? 'Kyllä' : 'Ei'
                }),
                {}
              ),
              notInSchoolReason: r.notInSchoolReason
                ? notInSchoolReasonNames[r.notInSchoolReason]
                : '',
              ...caseEventTypes
                .filter((t) => t !== 'NOTE')
                .reduce(
                  (acc, val) => ({
                    ...acc,
                    [`eventType_${val}`]: r.eventTypes.includes(val)
                      ? 'Kyllä'
                      : 'Ei'
                  }),
                  {}
                )
            }))}
            headers={[
              { key: 'openedAt', label: 'Vastaanotettu' },
              { key: 'birthYear', label: 'Syntymävuosi' },
              { key: 'ageAtCaseOpened', label: 'Ikä ilmoitushetkellä' },
              { key: 'gender', label: 'Sukupuoli' },
              { key: 'language', label: 'Äidinkieli' },
              { key: 'municipalityInFinland', label: 'Kotikunta Suomessa' },
              { key: 'status', label: 'Ohjauksen tila' },
              { key: 'finishedReason', label: 'Ohjauksen päättymisen syy' },
              { key: 'startedAtSchool', label: 'Siirtynyt opiskelemaan' },
              { key: 'followUpMeasures', label: 'Jatkotoimenpiteet' },
              { key: 'source', label: 'Lähde' },
              { key: 'sourceDetails', label: 'Lähde tarkennus' },
              ...schoolBackgrounds.map((val) => ({
                key: `schoolBackground_${val}`,
                label: schoolBackgroundNames[val]
              })),
              ...caseBackgroundReasonValues.map((val) => ({
                key: `caseBackgroundReason_${val}`,
                label: caseBackgroundReasonNames[val]
              })),
              {
                key: 'notInSchoolReason',
                label: 'Ei suorita oppivelvollisuutta, koska'
              },
              ...caseEventTypes
                .filter((t) => t !== 'NOTE')
                .map((val) => ({
                  key: `eventType_${val}`,
                  label: caseEventTypeNames[val]
                }))
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
