import React, { useEffect, useMemo, useState } from 'react'

import { EmployeeUser } from '../employees/api'
import { formatDate, parseDate } from '../shared/dates'
import { InputField } from '../shared/form/InputField'
import { Select } from '../shared/form/Select'
import {
  GroupOfInputRows,
  LabeledInputM,
  LabeledInputS,
  RowOfInputs
} from '../shared/layout'
import { Label } from '../shared/typography'

import { StudentCase, StudentCaseInput } from './api'

interface SharedProps {
  employees: EmployeeUser[]
}
interface CreateProps extends SharedProps {
  mode: 'CREATE'
  onChange: (validInput: StudentCaseInput | null) => void
}
interface ViewProps extends SharedProps {
  mode: 'VIEW'
  studentCase: StudentCase
}
interface EditProps extends SharedProps {
  mode: 'EDIT'
  studentCase: StudentCase
  onChange: (validInput: StudentCaseInput | null) => void
}
type Props = CreateProps | ViewProps | EditProps

export const StudentCaseForm = React.memo(function StudentCaseForm(
  props: Props
) {
  const [openedAt, setOpenedAt] = useState(
    formatDate(
      props.mode === 'CREATE' ? new Date() : props.studentCase.openedAt
    )
  )
  const [assignedTo, setAssignedTo] = useState(
    props.mode === 'CREATE' || !props.studentCase.assignedTo
      ? null
      : props.employees.find(
          (e) => e.id === props.studentCase.assignedTo?.id
        ) ?? null
  )

  const validInput: StudentCaseInput | null = useMemo(() => {
    const openedAtDate = parseDate(openedAt.trim())

    if (!openedAtDate) return null

    return {
      openedAt: openedAtDate,
      assignedTo: assignedTo?.id ?? null
    }
  }, [openedAt, assignedTo])

  useEffect(() => {
    if (props.mode !== 'VIEW') {
      props.onChange(validInput)
    }
  }, [validInput, props])

  return (
    <GroupOfInputRows $gapSize="m">
      <RowOfInputs $gapSize="L">
        <LabeledInputS>
          <Label>Ilmoitettu</Label>
          {props.mode === 'VIEW' ? (
            <span>{formatDate(props.studentCase.openedAt)}</span>
          ) : (
            <InputField onChange={setOpenedAt} value={openedAt} />
          )}
        </LabeledInputS>
        <LabeledInputM>
          <Label>Ohjaaja</Label>
          {props.mode === 'VIEW' ? (
            <span>{props.studentCase.assignedTo?.name ?? '-'}</span>
          ) : (
            <Select<EmployeeUser>
              items={props.employees}
              selectedItem={assignedTo}
              getItemValue={(e) => e.id}
              getItemLabel={(e) => `${e.firstName} ${e.lastName}`}
              placeholder="Ei ohjaajaa"
              onChange={setAssignedTo}
            />
          )}
        </LabeledInputM>
      </RowOfInputs>
    </GroupOfInputRows>
  )
})
