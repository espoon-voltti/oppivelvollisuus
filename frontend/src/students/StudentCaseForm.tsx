import React, { useState } from 'react'

import { formatDate, parseDate } from '../shared/dates'
import { FlexColWithGaps, FlexRowWithGaps, VerticalGap } from '../shared/layout'
import { H3, Label, P } from '../shared/typography'

import { apiPostStudentCase, apiPutStudentCase, StudentCase } from './api'

interface CreateProps {
  studentId: string
  onSaved: () => void
  onCancelled: () => void
}
interface ViewProps {
  studentCase: StudentCase
  editing: false
  onStartEdit: () => void
}
interface EditProps {
  studentCase: StudentCase
  editing: true
  onSaved: () => void
  onCancelled: () => void
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
  const [submitting, setSubmitting] = useState(false)

  const valid = parseDate(openedAt) !== undefined

  return (
    <div>
      <FlexRowWithGaps $gapSize="m">
        <H3>
          {isCreating(props)
            ? 'Uusi tapaus'
            : `Tapaus ${formatDate(props.studentCase.openedAt)}`}
        </H3>
        {isViewing(props) && (
          <button onClick={() => props.onStartEdit()}>Muokkaa</button>
        )}
      </FlexRowWithGaps>

      <VerticalGap $size="m" />

      <FlexColWithGaps $gapSize="m">
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
          <Label>Tapauksen tiedot</Label>
          {isViewing(props) ? (
            <P>{props.studentCase.info}</P>
          ) : (
            <textarea onChange={(e) => setInfo(e.target.value)} value={info} />
          )}
        </FlexColWithGaps>
      </FlexColWithGaps>

      <VerticalGap $size="m" />

      {!isViewing(props) && (
        <FlexRowWithGaps>
          <button
            disabled={submitting || !valid}
            onClick={() => {
              setSubmitting(true)
              const data = {
                openedAt: parseDate(openedAt)!,
                info: info
              }
              const req = isCreating(props)
                ? apiPostStudentCase(props.studentId, data)
                : apiPutStudentCase(
                    props.studentCase.studentId,
                    props.studentCase.id,
                    data
                  )

              req.then(() => props.onSaved()).catch(() => setSubmitting(false))
            }}
          >
            Tallenna
          </button>
          <button disabled={submitting} onClick={() => props.onCancelled()}>
            Peruuta
          </button>
        </FlexRowWithGaps>
      )}
    </div>
  )
})
