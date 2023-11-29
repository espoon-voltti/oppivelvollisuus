import React, { useEffect, useMemo, useState } from 'react'

import { EmployeeUser } from '../employees/api'
import { formatDate, parseDate } from '../shared/dates'
import { FlexColWithGaps, FlexRowWithGaps } from '../shared/layout'
import { Label, P } from '../shared/typography'

import { StudentCase, StudentCaseInput } from './api'

interface CreateProps {
  onChange: (validInput: StudentCaseInput | null) => void
  employees: EmployeeUser[]
}
interface ViewProps {
  studentCase: StudentCase
  editing: false
}
interface EditProps {
  studentCase: StudentCase
  editing: true
  onChange: (validInput: StudentCaseInput | null) => void
  employees: EmployeeUser[]
}
type Props = CreateProps | ViewProps | EditProps

function isCreating(p: Props): p is CreateProps {
  return !('studentCase' in p)
}

function isViewing(p: Props): p is ViewProps {
  return 'studentCase' in p && !p.editing
}

export const StudentCaseForm = React.memo(function StudentCaseForm(
  props: Props
) {
  const [openedAt, setOpenedAt] = useState(
    formatDate(isCreating(props) ? new Date() : props.studentCase.openedAt)
  )
  const [info, setInfo] = useState(
    isCreating(props) ? '' : props.studentCase.info
  )
  const [assignedTo, setAssignedTo] = useState(
    isCreating(props) ? null : props.studentCase.assignedTo?.id ?? null
  )

  const validInput: StudentCaseInput | null = useMemo(() => {
    const openedAtDate = parseDate(openedAt.trim())

    if (!openedAtDate) return null

    return {
      openedAt: openedAtDate,
      info,
      assignedTo
    }
  }, [openedAt, info, assignedTo])

  useEffect(() => {
    if (!isViewing(props)) {
      props.onChange(validInput)
    }
  }, [validInput, props])

  return (
    <FlexColWithGaps $gapSize="m">
      <FlexRowWithGaps $gapSize="L">
        <FlexColWithGaps>
          <Label>Tapaus vastaanotettu</Label>
          {isViewing(props) ? (
            <span>{formatDate(props.studentCase.openedAt)}</span>
          ) : (
            <input
              type="text"
              onChange={(e) => setOpenedAt(e.target.value)}
              value={openedAt}
            />
          )}
        </FlexColWithGaps>
        <FlexColWithGaps>
          <Label>Ohjaaja</Label>
          {isViewing(props) ? (
            <span>{props.studentCase.assignedTo?.name ?? '-'}</span>
          ) : (
            <select
              onChange={(e) => setAssignedTo(e.target.value || null)}
              value={assignedTo ?? undefined}
            >
              <option value="">-</option>
              {props.employees.map((e) => (
                <option key={e.externalId} value={e.externalId}>
                  {e.firstName} {e.lastName}
                </option>
              ))}
            </select>
          )}
        </FlexColWithGaps>
      </FlexRowWithGaps>
      <FlexColWithGaps>
        <Label>Tapauksen tiedot</Label>
        {isViewing(props) ? (
          <P>{props.studentCase.info || '-'}</P>
        ) : (
          <textarea onChange={(e) => setInfo(e.target.value)} value={info} />
        )}
      </FlexColWithGaps>
    </FlexColWithGaps>
  )
})
