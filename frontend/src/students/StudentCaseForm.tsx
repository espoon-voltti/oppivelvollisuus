import React, { useEffect, useMemo, useState } from 'react'

import { formatDate, parseDate } from '../shared/dates'
import { FlexColWithGaps } from '../shared/layout'
import { Label, P } from '../shared/typography'

import { StudentCase, StudentCaseInput } from './api'

interface CreateProps {
  onChange: (validInput: StudentCaseInput | null) => void
}
interface ViewProps {
  studentCase: StudentCase
  editing: false
}
interface EditProps {
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

  const validInput: StudentCaseInput | null = useMemo(() => {
    const openedAtDate = parseDate(openedAt.trim())

    if (!openedAtDate) return null

    return {
      openedAt: openedAtDate,
      info
    }
  }, [openedAt, info])

  useEffect(() => {
    if (!isViewing(props)) {
      props.onChange(validInput)
    }
  }, [validInput, props])

  return (
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
          <P>{props.studentCase.info || '-'}</P>
        ) : (
          <textarea onChange={(e) => setInfo(e.target.value)} value={info} />
        )}
      </FlexColWithGaps>
    </FlexColWithGaps>
  )
})
