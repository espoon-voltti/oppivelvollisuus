// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'

import { Select } from '../../../shared/form/Select'
import { LabeledInput, RowOfInputs } from '../../../shared/layout'
import { Label } from '../../../shared/typography'
import { StatusChip } from '../StatusChip'
import { StudentCase } from '../api'

import { CaseStatusInput } from './api'
import {
  CaseFinishedReason,
  caseFinishedReasonNames,
  caseFinishedReasons,
  CaseStatus,
  caseStatuses,
  caseStatusNames,
  SchoolType,
  schoolTypeNames,
  schoolTypes
} from './enums'

interface ViewProps {
  mode: 'VIEW'
  studentCase: StudentCase
}
interface EditProps {
  mode: 'EDIT'
  studentCase: StudentCase
  onChange: (validInput: CaseStatusInput | null) => void
  activeCaseExists: boolean
}
type Props = ViewProps | EditProps

export const CaseStatusForm = React.memo(function CaseStatusForm(props: Props) {
  const [status, setStatus] = useState<CaseStatus>(props.studentCase.status)
  const [finishedReason, setFinishedReason] =
    useState<CaseFinishedReason | null>(
      props.studentCase.finishedInfo?.reason ?? null
    )
  const [startedAtSchool, setStartedAtSchool] = useState<SchoolType | null>(
    props.studentCase.finishedInfo?.startedAtSchool ?? null
  )

  const validInput: CaseStatusInput | null = useMemo(() => {
    if (status === 'FINISHED') {
      if (finishedReason === null) return null

      if (finishedReason === 'BEGAN_STUDIES') {
        if (startedAtSchool === null) return null
        return {
          status,
          finishedInfo: { reason: finishedReason, startedAtSchool }
        }
      }

      return {
        status,
        finishedInfo: { reason: finishedReason, startedAtSchool: null }
      }
    }

    return { status, finishedInfo: null }
  }, [status, finishedReason, startedAtSchool])

  useEffect(() => {
    if (props.mode !== 'VIEW') {
      props.onChange(validInput)
    }
  }, [validInput, props])

  if (props.mode === 'VIEW') {
    return (
      <RowOfInputs>
        <LabeledInput $cols={4}>
          <Label>Tila</Label>
          <span>{caseStatusNames[props.studentCase.status]}</span>
        </LabeledInput>
        {props.studentCase.status === 'FINISHED' && (
          <LabeledInput $cols={4}>
            <Label>Syy ohjauksen päättymiselle</Label>
            <span>
              {caseFinishedReasonNames[props.studentCase.finishedInfo.reason]}
            </span>
          </LabeledInput>
        )}
        {props.studentCase.status === 'FINISHED' &&
          props.studentCase.finishedInfo.reason === 'BEGAN_STUDIES' && (
            <LabeledInput $cols={4}>
              <Label>Oppilaitos</Label>
              <span>
                {
                  schoolTypeNames[
                    props.studentCase.finishedInfo.startedAtSchool
                  ]
                }
              </span>
            </LabeledInput>
          )}
      </RowOfInputs>
    )
  }

  return (
    <RowOfInputs>
      <LabeledInput $cols={4}>
        <Label>Tila *</Label>
        {props.studentCase.status === 'FINISHED' && props.activeCaseExists ? (
          <StatusChip status={props.studentCase.status} />
        ) : (
          <Select<CaseStatus>
            items={caseStatuses}
            selectedItem={status}
            getItemLabel={(item) => caseStatusNames[item]}
            onChange={(item) => setStatus(item)}
          />
        )}
      </LabeledInput>
      {status === 'FINISHED' && (
        <LabeledInput $cols={4}>
          <Label>Syy ohjauksen päättymiselle *</Label>
          <Select<CaseFinishedReason>
            items={caseFinishedReasons}
            selectedItem={finishedReason}
            getItemLabel={(item) => caseFinishedReasonNames[item]}
            placeholder="Valitse"
            onChange={(item) => setFinishedReason(item)}
          />
        </LabeledInput>
      )}
      {status === 'FINISHED' && finishedReason === 'BEGAN_STUDIES' && (
        <LabeledInput $cols={4}>
          <Label>Oppilaitos *</Label>
          <Select<SchoolType>
            items={schoolTypes}
            selectedItem={startedAtSchool}
            getItemLabel={(item) => schoolTypeNames[item]}
            placeholder="Valitse"
            onChange={(item) => setStartedAtSchool(item)}
          />
        </LabeledInput>
      )}
    </RowOfInputs>
  )
})
