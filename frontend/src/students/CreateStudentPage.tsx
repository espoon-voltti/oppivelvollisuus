import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { FlexColWithGaps, VerticalGap } from '../shared/layout'
import { H1, Label } from '../shared/typography'

import { apiPostStudent } from './api'

export const CreateStudentPage = React.memo(function CreateStudentPage() {
  const navigate = useNavigate()
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const valid = firstName.trim() && lastName.trim()

  return (
    <div>
      <H1>Uusi oppivelvollinen</H1>

      <VerticalGap $size="L" />

      <FlexColWithGaps $gapSize="m">
        <FlexColWithGaps>
          <Label>Etunimi</Label>
          <input
            type="text"
            onChange={(e) => setFirstName(e.target.value)}
            value={firstName}
          />
        </FlexColWithGaps>
        <FlexColWithGaps>
          <Label>Sukunimi</Label>
          <input
            type="text"
            onChange={(e) => setLastName(e.target.value)}
            value={lastName}
          />
        </FlexColWithGaps>
      </FlexColWithGaps>

      <VerticalGap $size="m" />

      <button
        disabled={submitting || !valid}
        onClick={() => {
          setSubmitting(true)
          apiPostStudent({
            firstName: firstName.trim(),
            lastName: lastName.trim()
          })
            .then((id) => navigate(`/oppivelvolliset/${id}`))
            .catch(() => setSubmitting(false))
        }}
      >
        Tallenna
      </button>
    </div>
  )
})
