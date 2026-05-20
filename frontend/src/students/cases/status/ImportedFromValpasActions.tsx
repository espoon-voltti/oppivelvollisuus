// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { InfoBox } from '../../../shared/MessageBoxes'
import { Button } from '../../../shared/buttons/Button'
import {
  FlexColWithGaps,
  FlexRight,
  FlexRowWithGaps
} from '../../../shared/layout'
import { StudentDetails } from '../../api'
import { StudentCase } from '../api'

interface MissingField {
  label: string
}

function getMissingStudentFields(student: StudentDetails): MissingField[] {
  const missing: MissingField[] = []
  if (!student.firstName?.trim()) missing.push({ label: 'Etunimet' })
  if (!student.lastName?.trim()) missing.push({ label: 'Sukunimi' })
  if (!student.ssn?.trim()) missing.push({ label: 'Henkilötunnus' })
  return missing
}

function getMissingCaseFields(studentCase: StudentCase): MissingField[] {
  const missing: MissingField[] = []
  if (studentCase.source === 'VALPAS_NOTICE' && !studentCase.sourceValpas) {
    missing.push({ label: 'Ilmoittanut taho' })
  }
  return missing
}

interface Props {
  studentCase: StudentCase
  student: StudentDetails
  otherCases: StudentCase[]
  onApprove: () => Promise<void>
  onMarkAsDuplicate: () => Promise<void>
}

export const ImportedFromValpasActions = React.memo(
  function ImportedFromValpasActions(props: Props) {
    const hasActiveOther = props.otherCases.some(
      (c) => c.status === 'TODO' || c.status === 'ON_HOLD'
    )
    const missingStudent = getMissingStudentFields(props.student)
    const missingCase = getMissingCaseFields(props.studentCase)
    const allMissing = [...missingStudent, ...missingCase]
    const approveDisabled = hasActiveOther || allMissing.length > 0

    const [submitting, setSubmitting] = useState(false)

    const handleApprove = () => {
      setSubmitting(true)
      void props.onApprove().finally(() => setSubmitting(false))
    }

    const handleMarkAsDuplicate = () => {
      if (
        window.confirm(
          'Haluatko varmasti merkitä tämän ilmoituksen duplikaatiksi? Ilmoitus suljetaan ja merkitään aktiivisen ilmoituksen duplikaatiksi.'
        )
      ) {
        setSubmitting(true)
        void props.onMarkAsDuplicate().finally(() => setSubmitting(false))
      }
    }

    return (
      <FlexColWithGaps $gapSize="s">
        {hasActiveOther && (
          <InfoBox
            title="Alla oleva aktiivinen ilmoitus on ensin päätettävä, jos haluat hyväksyä tämän."
            wide
          />
        )}
        {!hasActiveOther && allMissing.length > 0 && (
          <InfoBox
            title={`Pakollisia tietoja puuttuu: ${allMissing.map((f) => f.label).join(', ')}`}
            wide
          />
        )}
        <FlexRight>
          <FlexRowWithGaps $gapSize="m">
            {hasActiveOther && (
              <Button
                data-qa="mark-as-duplicate-button"
                text="Merkitse duplikaatiksi"
                disabled={submitting}
                onClick={handleMarkAsDuplicate}
              />
            )}
            <Button
              data-qa="approve-valpas-case-button"
              text="Hyväksy"
              primary
              disabled={approveDisabled || submitting}
              onClick={handleApprove}
            />
          </FlexRowWithGaps>
        </FlexRight>
      </FlexColWithGaps>
    )
  }
)
