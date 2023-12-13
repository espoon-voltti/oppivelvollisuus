import React, { useEffect, useMemo, useState } from 'react'

import { Select } from '../shared/form/Select'
import {
  FlexRowWithGaps,
  LabeledInputFull,
  LabeledInputM
} from '../shared/layout'
import { Label } from '../shared/typography'

import { StatusChip } from './StatusChip'
import { CaseStatusInput, StudentCase } from './api'
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
      <FlexRowWithGaps $gapSize="L">
        <LabeledInputM>
          <Label>Tila</Label>
          <StatusChip status={props.studentCase.status} />
        </LabeledInputM>
        {props.studentCase.status === 'FINISHED' && (
          <LabeledInputM>
            <Label>Syy ohjauksen päättymiselle</Label>
            <span>
              {caseFinishedReasonNames[props.studentCase.finishedInfo.reason]}
            </span>
          </LabeledInputM>
        )}
        {props.studentCase.status === 'FINISHED' &&
          props.studentCase.finishedInfo.reason === 'BEGAN_STUDIES' && (
            <LabeledInputM>
              <Label>Oppilaitos</Label>
              <span>
                {
                  schoolTypeNames[
                    props.studentCase.finishedInfo.startedAtSchool
                  ]
                }
              </span>
            </LabeledInputM>
          )}
      </FlexRowWithGaps>
    )
  }

  return (
    <FlexRowWithGaps $gapSize="L">
      <LabeledInputM>
        <Label>Tila</Label>
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
      </LabeledInputM>
      {status === 'FINISHED' && (
        <LabeledInputM>
          <Label>Syy ohjauksen päättymiselle</Label>
          <Select<CaseFinishedReason>
            items={caseFinishedReasons}
            selectedItem={finishedReason}
            getItemLabel={(item) => caseFinishedReasonNames[item]}
            placeholder="Valitse"
            onChange={(item) => setFinishedReason(item)}
          />
        </LabeledInputM>
      )}
      {status === 'FINISHED' && finishedReason === 'BEGAN_STUDIES' && (
        <LabeledInputFull>
          <Label>Oppilaitos</Label>
          <Select<SchoolType>
            items={schoolTypes}
            selectedItem={startedAtSchool}
            getItemLabel={(item) => schoolTypeNames[item]}
            placeholder="Valitse"
            onChange={(item) => setStartedAtSchool(item)}
          />
        </LabeledInputFull>
      )}
    </FlexRowWithGaps>
  )
})
