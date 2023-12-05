import { faPen } from '@fortawesome/free-solid-svg-icons'
import { faPlus } from '@fortawesome/free-solid-svg-icons/faPlus'
import { faTrash } from '@fortawesome/free-solid-svg-icons/faTrash'
import React, { Fragment, useCallback, useEffect, useState } from 'react'

import { IconButton } from '../shared/buttons/IconButton'
import { InlineButton } from '../shared/buttons/InlineButton'
import { FlexColWithGaps, FlexRowWithGaps, Separator } from '../shared/layout'
import { H4 } from '../shared/typography'

import { CaseEventForm } from './CaseEventForm'
import {
  apiDeleteCaseEvent,
  apiGetCaseEvents,
  apiPostCaseEvent,
  apiPutCaseEvent,
  CaseEvent,
  CaseEventInput
} from './api'

export const CaseEvents = React.memo(function CaseEvents({
  studentCaseId,
  disabled,
  onChangeEditState
}: {
  studentCaseId: string
  disabled: boolean
  onChangeEditState: (editing: boolean) => unknown
}) {
  const [caseEventsResponse, setCaseEventsResponse] = useState<
    CaseEvent[] | null | undefined
  >(undefined)
  const loadCaseEvents = useCallback(() => {
    setCaseEventsResponse(null)
    void apiGetCaseEvents(studentCaseId).then(setCaseEventsResponse)
  }, [studentCaseId])
  useEffect(() => {
    loadCaseEvents()
  }, [loadCaseEvents])

  // true = creating new, string = id of the edited case event
  const [editingCaseEvent, setEditingCaseEvent] = useState<boolean | string>(
    false
  )
  useEffect(() => {
    onChangeEditState(!!editingCaseEvent)
  }, [onChangeEditState, editingCaseEvent])
  const [caseEventInput, setCaseEventInput] = useState<CaseEventInput | null>(
    null
  )
  const [submitting, setSubmitting] = useState(false)

  if (!caseEventsResponse) return null

  return (
    <FlexColWithGaps $gapSize="L">
      <FlexColWithGaps>
        <H4>Muistiinpanot ja toimenpiteet</H4>
        {editingCaseEvent === true ? (
          <>
            <FlexRowWithGaps $gapSize="L" style={{ alignItems: 'flex-end' }}>
              <CaseEventForm mode="CREATE" onChange={setCaseEventInput} />
              <FlexRowWithGaps $gapSize="m">
                <InlineButton
                  text="Peruuta"
                  disabled={submitting}
                  onClick={() => setEditingCaseEvent(false)}
                />
                <InlineButton
                  text="Tallenna"
                  disabled={submitting || !caseEventInput}
                  onClick={() => {
                    if (!caseEventInput) return

                    setSubmitting(true)
                    void apiPostCaseEvent(studentCaseId, caseEventInput)
                      .then(() => {
                        setEditingCaseEvent(false)
                        loadCaseEvents()
                      })
                      .finally(() => setSubmitting(false))
                  }}
                />
              </FlexRowWithGaps>
            </FlexRowWithGaps>
            <Separator />
          </>
        ) : (
          <InlineButton
            text="Lisää merkintä"
            disabled={editingCaseEvent !== false || submitting || disabled}
            onClick={() => setEditingCaseEvent(true)}
            icon={faPlus}
          />
        )}
      </FlexColWithGaps>

      <FlexColWithGaps $gapSize="L">
        {caseEventsResponse.map((caseEvent, idx) => (
          <Fragment key={caseEvent.id}>
            <FlexRowWithGaps $gapSize="L">
              <div style={{ flexGrow: '1' }}>
                {editingCaseEvent === caseEvent.id ? (
                  <CaseEventForm
                    key="EDIT"
                    mode="EDIT"
                    caseEvent={caseEvent}
                    onChange={setCaseEventInput}
                  />
                ) : (
                  <CaseEventForm key="VIEW" mode="VIEW" caseEvent={caseEvent} />
                )}
              </div>
              {editingCaseEvent === caseEvent.id ? (
                <FlexRowWithGaps $gapSize="m" style={{ alignSelf: 'flex-end' }}>
                  <InlineButton
                    text="Peruuta"
                    disabled={submitting}
                    onClick={() => setEditingCaseEvent(false)}
                  />
                  <InlineButton
                    text="Tallenna"
                    disabled={submitting || !caseEventInput}
                    onClick={() => {
                      if (!caseEventInput) return

                      setSubmitting(true)
                      void apiPutCaseEvent(caseEvent.id, caseEventInput)
                        .then(() => {
                          setEditingCaseEvent(false)
                          loadCaseEvents()
                        })
                        .finally(() => setSubmitting(false))
                    }}
                  />
                </FlexRowWithGaps>
              ) : (
                <FlexRowWithGaps
                  $gapSize="m"
                  style={{ alignSelf: 'flex-start' }}
                >
                  <IconButton
                    icon={faPen}
                    aria-label="Muokkaa"
                    disabled={disabled || editingCaseEvent !== false}
                    onClick={() => setEditingCaseEvent(caseEvent.id)}
                  />
                  <IconButton
                    icon={faTrash}
                    aria-label="Poista"
                    disabled={disabled || editingCaseEvent !== false}
                    onClick={() => {
                      if (
                        window.confirm('Haluatko varmasti poistaa merkinnän?')
                      ) {
                        setSubmitting(true)
                        void apiDeleteCaseEvent(caseEvent.id).finally(() => {
                          setSubmitting(false)
                          loadCaseEvents()
                        })
                      }
                    }}
                  />
                </FlexRowWithGaps>
              )}
            </FlexRowWithGaps>
            {idx < caseEventsResponse.length - 1 && <Separator />}
          </Fragment>
        ))}
      </FlexColWithGaps>
    </FlexColWithGaps>
  )
})
