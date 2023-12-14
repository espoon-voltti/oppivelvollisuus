import React, { useEffect, useMemo, useState } from 'react'
import styled, { css } from 'styled-components'

import { formatDate, parseDate } from '../../../shared/dates'
import { InputField } from '../../../shared/form/InputField'
import { Select } from '../../../shared/form/Select'
import { ReadOnlyTextArea, TextArea } from '../../../shared/form/TextArea'
import {
  GroupOfInputRows,
  LabeledInput,
  RowOfInputs
} from '../../../shared/layout'
import { colors } from '../../../shared/theme'
import { Label } from '../../../shared/typography'

import { CaseEvent, CaseEventInput } from './api'
import { CaseEventType, caseEventTypeNames, caseEventTypes } from './enums'

interface CreateProps {
  mode: 'CREATE'
  onChange: (validInput: CaseEventInput | null) => void
}
interface ViewProps {
  mode: 'VIEW'
  caseEvent: CaseEvent
}
interface EditProps {
  mode: 'EDIT'
  caseEvent: CaseEvent
  onChange: (validInput: CaseEventInput | null) => void
}
type Props = CreateProps | ViewProps | EditProps

const UpdaterInfo = styled.span`
  color: ${colors.grayscale.g35};
`

export const CaseEventForm = React.memo(function CaseEventForm(props: Props) {
  const [date, setDate] = useState(
    formatDate(props.mode === 'CREATE' ? new Date() : props.caseEvent.date)
  )
  const [type, setType] = useState<CaseEventType>(
    props.mode === 'CREATE' ? 'NOTE' : props.caseEvent.type
  )
  const [notes, setNotes] = useState(
    props.mode === 'CREATE' ? '' : props.caseEvent.notes
  )

  const validInput: CaseEventInput | null = useMemo(() => {
    const parsedDate = parseDate(date.trim())

    if (!parsedDate) return null

    return {
      date: parsedDate,
      type,
      notes: notes.trim()
    }
  }, [date, type, notes])

  useEffect(() => {
    if (props.mode !== 'VIEW') {
      props.onChange(validInput)
    }
  }, [validInput, props])

  if (props.mode === 'VIEW') {
    return (
      <RowOfInputs>
        <LabeledInput $cols={2}>
          <TypeLabel $type={props.caseEvent.type}>
            {formatDate(props.caseEvent.date)}
          </TypeLabel>
          <span>{props.caseEvent.created.name}</span>
        </LabeledInput>
        <LabeledInput $cols={9}>
          <TypeLabel $type={props.caseEvent.type}>
            {caseEventTypeNames[props.caseEvent.type]}
          </TypeLabel>
          <ReadOnlyTextArea text={props.caseEvent.notes} />
          {props.caseEvent.updated && (
            <UpdaterInfo>
              (Viimeisin muokkaus {formatDate(props.caseEvent.updated.time)}{' '}
              {props.caseEvent.updated.name})
            </UpdaterInfo>
          )}
        </LabeledInput>
      </RowOfInputs>
    )
  }

  return (
    <GroupOfInputRows>
      <RowOfInputs>
        <LabeledInput $cols={2}>
          <Label>Päivämäärä</Label>
          <InputField onChange={setDate} value={date} />
        </LabeledInput>
        <LabeledInput $cols={4}>
          <Label>Merkinnän tyyppi</Label>
          <Select<CaseEventType>
            items={caseEventTypes}
            selectedItem={type}
            getItemLabel={(item) => caseEventTypeNames[item]}
            onChange={(e) => setType(e ?? 'NOTE')}
          />
        </LabeledInput>
      </RowOfInputs>
      <RowOfInputs>
        <LabeledInput $cols={9}>
          <Label>Kommentti</Label>
          <TextArea onChange={setNotes} value={notes} />
        </LabeledInput>
      </RowOfInputs>
    </GroupOfInputRows>
  )
})

const TypeLabel = styled.span<{ $type: CaseEventType }>`
  ${(p) =>
    p.$type !== 'NOTE'
      ? css`
          font-weight: 600;
        `
      : ''}
`
