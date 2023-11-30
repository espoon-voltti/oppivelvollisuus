import React, { useEffect, useMemo, useState } from 'react'

import { EmployeeUser } from '../employees/api'
import { formatDate, parseDate } from '../shared/dates'
import { InputField } from '../shared/form/InputField'
import { Select } from '../shared/form/Select'
import { ReadOnlyTextArea, TextArea } from '../shared/form/TextArea'
import {
  GroupOfInputRows,
  LabeledInputL,
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
  onChange: (validInput: StudentCaseInput | null) => void
}
interface ViewProps extends SharedProps {
  studentCase: StudentCase
  editing: false
}
interface EditProps extends SharedProps {
  studentCase: StudentCase
  editing: true
  onChange: (validInput: StudentCaseInput | null) => void
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
    isCreating(props) || !props.studentCase.assignedTo
      ? null
      : props.employees.find(
          (e) => e.externalId === props.studentCase.assignedTo?.id
        ) ?? null
  )

  const validInput: StudentCaseInput | null = useMemo(() => {
    const openedAtDate = parseDate(openedAt.trim())

    if (!openedAtDate) return null

    return {
      openedAt: openedAtDate,
      info,
      assignedTo: assignedTo?.externalId ?? null
    }
  }, [openedAt, info, assignedTo])

  useEffect(() => {
    if (!isViewing(props)) {
      props.onChange(validInput)
    }
  }, [validInput, props])

  return (
    <GroupOfInputRows $gapSize="m">
      <RowOfInputs $gapSize="L">
        <LabeledInputS>
          <Label>Ilmoitettu</Label>
          {isViewing(props) ? (
            <span>{formatDate(props.studentCase.openedAt)}</span>
          ) : (
            <InputField onChange={setOpenedAt} value={openedAt} />
          )}
        </LabeledInputS>
        <LabeledInputM>
          <Label>Ohjaaja</Label>
          {isViewing(props) ? (
            <span>{props.studentCase.assignedTo?.name ?? '-'}</span>
          ) : (
            <Select<EmployeeUser>
              items={props.employees}
              selectedItem={assignedTo}
              getItemValue={(e) => e.externalId}
              getItemLabel={(e) => `${e.firstName} ${e.lastName}`}
              placeholder="Ei ohjaajaa"
              onChange={setAssignedTo}
            />
          )}
        </LabeledInputM>
      </RowOfInputs>
      <RowOfInputs $gapSize="L">
        <LabeledInputL>
          <Label>Tapauksen tiedot</Label>
          {isViewing(props) ? (
            <ReadOnlyTextArea text={props.studentCase.info ?? '-'} />
          ) : (
            <TextArea onChange={setInfo} value={info} />
          )}
        </LabeledInputL>
      </RowOfInputs>
    </GroupOfInputRows>
  )
})
