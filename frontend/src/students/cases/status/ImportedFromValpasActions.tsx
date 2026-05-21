// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useState } from 'react'

import { AlertBox, InfoBox } from '../../../shared/MessageBoxes'
import { Button } from '../../../shared/buttons/Button'
import { formatDate } from '../../../shared/dates'
import { FlexRight, FlexRowWithGaps, VerticalGap } from '../../../shared/layout'
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
  cases: StudentCase[]
  student: StudentDetails
  onApprove: () => Promise<void>
  onMarkAsDuplicate: (targetCaseId: string) => Promise<void>
}

export const ImportedFromValpasActions = React.memo(
  function ImportedFromValpasActions(props: Props) {
    const [submitting, setSubmitting] = useState(false)

    const importedCase = props.cases.find(
      (c) => c.status === 'IMPORTED_FROM_VALPAS'
    )
    if (!importedCase) return null

    const activeCase = props.cases.find(
      (c) => c.status === 'TODO' || c.status === 'ON_HOLD'
    )
    const hasActiveCase = activeCase !== undefined
    const latestFinishedCase = orderBy(
      props.cases.filter((c) => c.status === 'FINISHED'),
      [(c) => c.openedAt, (c) => c.id],
      ['desc', 'desc']
    )[0]
    const duplicateTarget = activeCase ?? latestFinishedCase
    const canMarkAsDuplicate =
      duplicateTarget !== undefined &&
      duplicateTarget.valpasNotificationId === null
    const missingStudentFields = getMissingStudentFields(props.student)
    const missingCaseFields = getMissingCaseFields(importedCase)
    const allMissingFields = [...missingStudentFields, ...missingCaseFields]
    const approveDisabled = hasActiveCase || allMissingFields.length > 0

    const handleApprove = () => {
      setSubmitting(true)
      void props.onApprove().finally(() => setSubmitting(false))
    }

    const handleMarkAsDuplicate = () => {
      if (
        duplicateTarget &&
        window.confirm(
          `Poistetaanko tämä Valppaasta tuotu ilmoitus manuaalisesti luodun ilmoituksen (${formatDate(duplicateTarget.openedAt)}) duplikaattina?`
        )
      ) {
        setSubmitting(true)
        void props
          .onMarkAsDuplicate(duplicateTarget.id)
          .finally(() => setSubmitting(false))
      }
    }

    return (
      <div>
        <VerticalGap />
        {hasActiveCase && (
          <InfoBox title="Aiemmin luotu aktiivinen ilmoitus on ensin päätettävä, jos haluat hyväksyä tämän." />
        )}
        {!hasActiveCase && allMissingFields.length > 0 && (
          <AlertBox
            title={`Pakollisia tietoja puuttuu: ${allMissingFields.map((f) => f.label).join(', ')}`}
          />
        )}
        <VerticalGap $size="L" />
        <FlexRight>
          <FlexRowWithGaps $gapSize="m">
            {canMarkAsDuplicate && (
              <Button
                data-qa="mark-as-duplicate-button"
                text="Hylkää duplikaattina"
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
      </div>
    )
  }
)
