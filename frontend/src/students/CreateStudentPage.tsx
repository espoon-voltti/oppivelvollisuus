import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { VerticalGap } from '../shared/layout'
import { H2, H3 } from '../shared/typography'

import { StudentCaseForm } from './StudentCaseForm'
import { StudentForm } from './StudentForm'
import { apiPostStudent, StudentCaseInput, StudentInput } from './api'

export const CreateStudentPage = React.memo(function CreateStudentPage() {
  const navigate = useNavigate()

  const [studentInput, setStudentInput] = useState<StudentInput | null>(null)
  const [studentCaseInput, setStudentCaseInput] =
    useState<StudentCaseInput | null>(null)
  const [submitting, setSubmitting] = useState(false)

  return (
    <div>
      <Link to="/oppivelvolliset">Takaisin</Link>
      <VerticalGap $size="L" />

      <H2>Uusi oppivelvollinen</H2>

      <VerticalGap $size="L" />

      <H3>Oppivelvollisen tiedot</H3>
      <VerticalGap $size="m" />
      <StudentForm onChange={setStudentInput} />
      <VerticalGap $size="m" />
      <H3>Ilmoituksen tiedot</H3>
      <VerticalGap $size="m" />
      <StudentCaseForm onChange={setStudentCaseInput} />
      <VerticalGap $size="m" />

      <button
        disabled={submitting || !studentInput || !studentCaseInput}
        onClick={() => {
          if (!studentInput || !studentCaseInput) return

          setSubmitting(true)
          apiPostStudent({
            student: studentInput,
            studentCase: studentCaseInput
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
