import {
  faChevronDown,
  faChevronLeft,
  faChevronUp,
  faPen,
  faPlus
} from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { apiGetEmployees, EmployeeUser } from '../employees/api'
import Button from '../shared/buttons/Button'
import InlineButton from '../shared/buttons/InlineButton'
import { formatDate } from '../shared/dates'
import {
  FlexColWithGaps,
  FlexLeftRight,
  FlexRight,
  FlexRowWithGaps,
  PageContainer,
  SectionContainer,
  VerticalGap
} from '../shared/layout'
import { colors } from '../shared/theme'
import { H2, H3 } from '../shared/typography'

import { StudentCaseForm } from './StudentCaseForm'
import { StudentForm } from './StudentForm'
import {
  apiGetStudent,
  apiPutStudent,
  StudentInput,
  StudentResponse,
  StudentCaseInput,
  apiPostStudentCase,
  apiPutStudentCase
} from './api'

const AccordionRow = styled(FlexLeftRight)<{ $disabled: boolean }>`
  ${(p) => (p.$disabled ? '' : 'cursor: pointer;')}
  user-select: none;

  .accordion-chevron {
    color: ${colors.grayscale.g35};
    font-size: 16px;
  }

  border-top: 1px dashed ${colors.grayscale.g35};
  padding: 8px 0;
`

export const StudentPage = React.memo(function StudentPage() {
  const { id } = useParams()
  if (!id) throw Error('Id not found in path')

  const navigate = useNavigate()

  const [employees, setEmployees] = useState<EmployeeUser[] | null>(null)
  useEffect(() => {
    void apiGetEmployees().then(setEmployees)
  }, [])

  const [studentResponse, setStudentResponse] =
    useState<StudentResponse | null>(null)
  const loadStudent = useCallback(() => {
    setStudentResponse(null)
    void apiGetStudent(id).then(setStudentResponse)
  }, [id])
  useEffect(loadStudent, [loadStudent])

  const [editing, setEditing] = useState(false)
  const [studentInput, setStudentInput] = useState<StudentInput | null>(null)

  // true = creating new, string = id of the edited case
  const [editingCase, setEditingCase] = useState<boolean | string>(false)
  const [studentCaseInput, setStudentCaseInput] =
    useState<StudentCaseInput | null>(null)
  const [expandedCase, setExpandedCase] = useState<string | null>(null)

  const [submitting, setSubmitting] = useState(false)

  return (
    <PageContainer>
      <SectionContainer>
        <InlineButton
          text="Takaisin"
          icon={faChevronLeft}
          onClick={() => navigate('/oppivelvolliset')}
        />
        <VerticalGap $size="m" />
        <H2>
          {studentResponse
            ? `${studentResponse.student.firstName} ${studentResponse.student.lastName}`
            : ''}
        </H2>
      </SectionContainer>

      <VerticalGap $size="m" />

      {studentResponse && employees && (
        <>
          <SectionContainer>
            <FlexLeftRight>
              <H3>Oppivelvollisen tiedot</H3>
              <InlineButton
                text="Muokkaa"
                icon={faPen}
                disabled={editingCase !== false}
                onClick={() => setEditing(true)}
              />
            </FlexLeftRight>
            <VerticalGap $size="L" />
            <StudentForm
              key={editing ? 'student-form' : 'student-read-view'}
              editing={editing}
              student={studentResponse.student}
              onChange={setStudentInput}
            />
            <VerticalGap $size="m" />
            {editing && (
              <FlexRight>
                <FlexRowWithGaps>
                  <Button
                    text="Peruuta"
                    disabled={submitting}
                    onClick={() => setEditing(false)}
                  />
                  <Button
                    text="Tallenna"
                    primary
                    disabled={submitting || !studentInput}
                    onClick={() => {
                      if (!studentInput) return

                      setSubmitting(true)
                      void apiPutStudent(id, studentInput)
                        .then(() => {
                          setEditing(false)
                          loadStudent()
                        })
                        .finally(() => setSubmitting(false))
                    }}
                  />
                </FlexRowWithGaps>
              </FlexRight>
            )}
          </SectionContainer>

          <VerticalGap $size="m" />

          <SectionContainer>
            <FlexLeftRight>
              <H3>Oppivelvollisuusilmoitukset</H3>
              <InlineButton
                text="Lisää ilmoitus"
                icon={faPlus}
                onClick={() => {
                  setEditingCase(true)
                  setExpandedCase(null)
                }}
                disabled={editing || editingCase !== false}
              />
            </FlexLeftRight>

            <VerticalGap $size="m" />

            {editingCase === true && (
              <FlexColWithGaps>
                <H3>Uusi ilmoitus</H3>
                <StudentCaseForm
                  onChange={setStudentCaseInput}
                  employees={employees}
                />
                <FlexRowWithGaps>
                  <button
                    disabled={submitting || !studentCaseInput}
                    onClick={() => {
                      if (!studentCaseInput) return

                      setSubmitting(true)
                      void apiPostStudentCase(id, studentCaseInput)
                        .then(() => {
                          setEditingCase(false)
                          loadStudent()
                        })
                        .finally(() => setSubmitting(false))
                    }}
                  >
                    Tallenna
                  </button>
                  <button
                    disabled={submitting}
                    onClick={() => {
                      setEditingCase(false)
                    }}
                  >
                    Peruuta
                  </button>
                </FlexRowWithGaps>
              </FlexColWithGaps>
            )}

            <VerticalGap $size="m" />

            <FlexColWithGaps $gapSize="L">
              {studentResponse.cases.map((studentCase) => (
                <FlexColWithGaps key={studentCase.id}>
                  <AccordionRow
                    $disabled={editingCase !== false}
                    onClick={() => {
                      if (editingCase !== false) return

                      if (expandedCase === studentCase.id) {
                        setExpandedCase(null)
                      } else {
                        setExpandedCase(studentCase.id)
                      }
                    }}
                  >
                    <H3>Ilmoitus {formatDate(studentCase.openedAt)}</H3>
                    <FlexRowWithGaps $gapSize="m">
                      <span>
                        {studentCase.assignedTo?.name ?? 'Ei ohjaajaa'}
                      </span>
                      <FontAwesomeIcon
                        icon={
                          expandedCase === studentCase.id
                            ? faChevronUp
                            : faChevronDown
                        }
                        className="accordion-chevron"
                      />
                    </FlexRowWithGaps>
                  </AccordionRow>
                  {expandedCase === studentCase.id && (
                    <FlexColWithGaps $gapSize="m">
                      <FlexLeftRight style={{ alignItems: 'flex-start' }}>
                        <StudentCaseForm
                          key={`${studentCase.id}-${
                            editingCase ? 'form' : 'read-view'
                          }`}
                          studentCase={studentCase}
                          editing={editingCase === studentCase.id}
                          onChange={setStudentCaseInput}
                          employees={employees}
                        />
                        {editingCase !== studentCase.id && (
                          <InlineButton
                            text="Muokkaa"
                            icon={faPen}
                            disabled={editing || editingCase !== false}
                            onClick={() => setEditingCase(studentCase.id)}
                          />
                        )}
                      </FlexLeftRight>
                      {editingCase === studentCase.id && (
                        <FlexRight>
                          <FlexRowWithGaps>
                            <Button
                              text="Peruuta"
                              disabled={submitting}
                              onClick={() => {
                                setEditingCase(false)
                              }}
                            />
                            <Button
                              text="Tallenna"
                              primary
                              disabled={submitting || !studentCaseInput}
                              onClick={() => {
                                if (!studentCaseInput) return

                                setSubmitting(true)
                                void apiPutStudentCase(
                                  id,
                                  studentCase.id,
                                  studentCaseInput
                                )
                                  .then(() => {
                                    setEditingCase(false)
                                    loadStudent()
                                  })
                                  .finally(() => setSubmitting(false))
                              }}
                            />
                          </FlexRowWithGaps>
                        </FlexRight>
                      )}
                    </FlexColWithGaps>
                  )}
                </FlexColWithGaps>
              ))}
            </FlexColWithGaps>
          </SectionContainer>
        </>
      )}
    </PageContainer>
  )
})
