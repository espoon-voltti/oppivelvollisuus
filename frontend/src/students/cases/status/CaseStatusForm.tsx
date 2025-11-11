// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import { Checkbox } from 'shared/form/Checkbox'
import { InputField } from 'shared/form/InputField'

import { Select } from '../../../shared/form/Select'
import {
  FlexColWithGaps,
  LabeledInput,
  RowOfInputs
} from '../../../shared/layout'
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
  FollowUpMeasure,
  followUpMeasureNames,
  followUpMeasureValues,
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
  const [followUpMeasures, setFollowUpMeasures] = useState<FollowUpMeasure[]>(
    props.studentCase.finishedInfo?.followUpMeasures ?? []
  )
  const [otherReason, setOtherReason] = useState<string>(
    props.studentCase.finishedInfo?.otherReason ?? ''
  )

  const validInput: CaseStatusInput | null = useMemo(() => {
    if (status === 'FINISHED') {
      if (finishedReason === null) return null

      if (finishedReason === 'BEGAN_STUDIES') {
        if (startedAtSchool === null) return null
        return {
          status,
          finishedInfo: {
            reason: finishedReason,
            startedAtSchool,
            followUpMeasures: null,
            otherReason: null
          }
        }
      }
      if (finishedReason === 'COMPULSORY_EDUCATION_ENDED') {
        if (followUpMeasures.length === 0) return null
        return {
          status,
          finishedInfo: {
            reason: finishedReason,
            followUpMeasures: followUpMeasures,
            startedAtSchool: null,
            otherReason: null
          }
        }
      }
      if (finishedReason === 'OTHER') {
        if (otherReason.trim() === '') return null
        return {
          status,
          finishedInfo: {
            reason: finishedReason,
            followUpMeasures: null,
            startedAtSchool: null,
            otherReason: otherReason.trim()
          }
        }
      }

      return {
        status,
        finishedInfo: {
          reason: finishedReason,
          followUpMeasures: null,
          startedAtSchool: null,
          otherReason: null
        }
      }
    }

    return { status, finishedInfo: null }
  }, [status, finishedReason, startedAtSchool, followUpMeasures, otherReason])

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
        {props.studentCase.status === 'FINISHED' &&
          props.studentCase.finishedInfo.reason ===
            'COMPULSORY_EDUCATION_ENDED' && (
            <LabeledInput $cols={4}>
              <Label>Jatkotoimenpiteet</Label>
              <span>
                <span>
                  {followUpMeasures !== null && followUpMeasures.length > 0 ? (
                    <ul>
                      {followUpMeasures.map((opt) => (
                        <li key={opt}>{followUpMeasureNames[opt]}</li>
                      ))}
                    </ul>
                  ) : (
                    '-'
                  )}
                </span>
              </span>
            </LabeledInput>
          )}

        {props.studentCase.status === 'FINISHED' &&
          props.studentCase.finishedInfo.reason === 'OTHER' && (
            <LabeledInput $cols={4}>
              <Label>Selite</Label>
              <span>{props.studentCase.finishedInfo.otherReason || '-'}</span>
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
      {status === 'FINISHED' &&
        finishedReason === 'COMPULSORY_EDUCATION_ENDED' && (
          <LabeledInput $cols={4}>
            <Label>Jatkotoimenpiteet *</Label>
            <FlexColWithGaps>
              {followUpMeasureValues.map((option) => (
                <Checkbox
                  key={option}
                  label={followUpMeasureNames[option]}
                  checked={followUpMeasures.includes(option)}
                  onChange={(checked) =>
                    setFollowUpMeasures((prev) =>
                      checked
                        ? [...prev, option]
                        : prev?.filter((cbr) => cbr !== option)
                    )
                  }
                />
              ))}
            </FlexColWithGaps>
          </LabeledInput>
        )}

      {status === 'FINISHED' && finishedReason === 'OTHER' && (
        <LabeledInput $cols={4}>
          <Label>Selite</Label>
          <InputField onChange={setOtherReason} value={otherReason} />
        </LabeledInput>
      )}
    </RowOfInputs>
  )
})
