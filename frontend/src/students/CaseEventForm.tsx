import React, { useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'

import { formatDate, parseDate } from '../shared/dates'
import { InputField } from '../shared/form/InputField'
import { Select } from '../shared/form/Select'
import { ReadOnlyTextArea, TextArea } from '../shared/form/TextArea'
import {
  FlexColWithGaps,
  FlexRowWithGaps,
  GroupOfInputRows,
  LabeledInputFull,
  LabeledInputL,
  LabeledInputS,
  RowOfInputs
} from '../shared/layout'
import { colors } from '../shared/theme'
import { Label } from '../shared/typography'

import {
  CaseEvent,
  CaseEventInput,
  caseEventTypeNames,
  CaseEventType,
  caseEventTypes
} from './api'

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
      <FlexRowWithGaps $gapSize="L" style={{ alignItems: 'flex-start' }}>
        <FlexColWithGaps
          $gapSize="s"
          style={{ width: '200px', flexShrink: '0' }}
        >
          <strong>{formatDate(props.caseEvent.date)}</strong>
          <span>{props.caseEvent.created.name}</span>
        </FlexColWithGaps>
        <FlexColWithGaps $gapSize="s" style={{ flexGrow: '1' }}>
          <strong>{caseEventTypeNames[props.caseEvent.type]}</strong>
          <ReadOnlyTextArea text={props.caseEvent.notes} />
          {props.caseEvent.updated && (
            <UpdaterInfo>
              (Viimeisin muokkaus {formatDate(props.caseEvent.updated.time)}{' '}
              {props.caseEvent.updated.name})
            </UpdaterInfo>
          )}
        </FlexColWithGaps>
      </FlexRowWithGaps>
    )
  }

  return (
    <GroupOfInputRows $gapSize="m">
      <RowOfInputs $gapSize="L">
        <LabeledInputS>
          <Label>Päivämäärä</Label>
          <InputField onChange={setDate} value={date} />
        </LabeledInputS>
        <LabeledInputL>
          <Label>Merkinnän tyyppi</Label>
          <Select<CaseEventType>
            items={caseEventTypes}
            selectedItem={type}
            getItemLabel={(item) => caseEventTypeNames[item]}
            onChange={(e) => setType(e ?? 'NOTE')}
          />
        </LabeledInputL>
      </RowOfInputs>
      <RowOfInputs $gapSize="L">
        <LabeledInputFull>
          <Label>Kommentti</Label>
          <TextArea onChange={setNotes} value={notes} />
        </LabeledInputFull>
      </RowOfInputs>
    </GroupOfInputRows>
  )
})
