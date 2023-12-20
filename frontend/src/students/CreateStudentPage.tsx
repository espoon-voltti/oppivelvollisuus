import { faChevronLeft } from '@fortawesome/free-solid-svg-icons'
import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { apiGetEmployees, EmployeeUser } from '../employees/api'
import { Button } from '../shared/buttons/Button'
import { InlineButton } from '../shared/buttons/InlineButton'
import {
  FlexRight,
  PageContainer,
  SectionContainer,
  VerticalGap
} from '../shared/layout'
import { H2, H3 } from '../shared/typography'

import { StudentForm } from './StudentForm'
import { apiPostStudent, StudentInput } from './api'
import { StudentCaseForm } from './cases/StudentCaseForm'
import { StudentCaseInput } from './cases/api'

export const CreateStudentPage = React.memo(function CreateStudentPage() {
  const navigate = useNavigate()

  const [employees, setEmployees] = useState<EmployeeUser[] | null>(null)
  useEffect(() => {
    void apiGetEmployees().then(setEmployees)
  }, [])

  const [studentInput, setStudentInput] = useState<StudentInput | null>(null)
  const [studentCaseInput, setStudentCaseInput] =
    useState<StudentCaseInput | null>(null)
  const [submitting, setSubmitting] = useState(false)

  if (!employees) return <div>...</div>

  return (
    <PageContainer>
      <SectionContainer>
        <InlineButton
          text="Takaisin"
          icon={faChevronLeft}
          onClick={() => navigate('/oppivelvolliset')}
        />
        <VerticalGap $size="m" />
        <H2>Uusi oppivelvollinen</H2>
      </SectionContainer>

      <VerticalGap $size="m" />

      <SectionContainer>
        <H3>Oppivelvollisen tiedot</H3>
        <VerticalGap $size="m" />
        <StudentForm mode="CREATE" onChange={setStudentInput} />
      </SectionContainer>

      <VerticalGap $size="m" />

      <SectionContainer>
        <H3>Ilmoituksen tiedot</H3>
        <VerticalGap $size="m" />
        <StudentCaseForm
          mode="CREATE"
          onChange={setStudentCaseInput}
          employees={employees}
        />
      </SectionContainer>

      <VerticalGap $size="m" />

      <SectionContainer>
        <FlexRight>
          <Button
            text="Tallenna"
            data-qa="save-button"
            primary
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
          />
        </FlexRight>
      </SectionContainer>
    </PageContainer>
  )
})
