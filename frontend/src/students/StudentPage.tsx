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
import { Button } from '../shared/buttons/Button'
import { InlineButton } from '../shared/buttons/InlineButton'
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

import { CaseEvents } from './CaseEvents'
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

  border-top: 1px solid ${colors.grayscale.g15};
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
    void apiGetStudent(id).then((response) => {
      setStudentResponse(response)
      setExpandedCase(response.cases.length > 0 ? response.cases[0].id : null)
    })
  }, [id])
  useEffect(loadStudent, [loadStudent])

  const [editingStudent, setEditingStudent] = useState(false)
  const [studentInput, setStudentInput] = useState<StudentInput | null>(null)

  // true = creating new, string = id of the edited case
  const [editingCase, setEditingCase] = useState<boolean | string>(false)
  const [studentCaseInput, setStudentCaseInput] =
    useState<StudentCaseInput | null>(null)
  const [expandedCase, setExpandedCase] = useState<string | null>(null)

  const [editingCaseEvent, setEditingCaseEvent] = useState(false)

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
                disabled={editingCase !== false || editingCaseEvent}
                onClick={() => setEditingStudent(true)}
              />
            </FlexLeftRight>
            <VerticalGap $size="L" />
            <StudentForm
              key={editingStudent ? 'EDIT' : 'VIEW'}
              mode={editingStudent ? 'EDIT' : 'VIEW'}
              student={studentResponse.student}
              onChange={setStudentInput}
            />
            <VerticalGap $size="m" />
            {editingStudent && (
              <FlexRight>
                <FlexRowWithGaps>
                  <Button
                    text="Peruuta"
                    disabled={submitting}
                    onClick={() => setEditingStudent(false)}
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
                          setEditingStudent(false)
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
            <FlexColWithGaps>
              <H3>Oppivelvollisuusilmoitukset</H3>
              {editingCase !== true && (
                <InlineButton
                  text="Lisää ilmoitus"
                  icon={faPlus}
                  onClick={() => {
                    setEditingCase(true)
                    setExpandedCase(null)
                  }}
                  disabled={
                    editingStudent || editingCase !== false || editingCaseEvent
                  }
                />
              )}
            </FlexColWithGaps>

            {editingCase === true && (
              <FlexColWithGaps>
                <H3>Uusi ilmoitus</H3>
                <StudentCaseForm
                  mode="CREATE"
                  onChange={setStudentCaseInput}
                  employees={employees}
                />
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
                        void apiPostStudentCase(id, studentCaseInput)
                          .then(() => {
                            setEditingCase(false)
                            loadStudent()
                          })
                          .finally(() => setSubmitting(false))
                      }}
                    />
                  </FlexRowWithGaps>
                </FlexRight>
              </FlexColWithGaps>
            )}

            <VerticalGap $size="L" />

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
                    <FlexColWithGaps
                      $gapSize="L"
                      style={{ paddingLeft: '24px' }}
                    >
                      <FlexLeftRight style={{ alignItems: 'flex-start' }}>
                        <StudentCaseForm
                          key={editingCase === studentCase.id ? 'EDIT' : 'VIEW'}
                          mode={
                            editingCase === studentCase.id ? 'EDIT' : 'VIEW'
                          }
                          studentCase={studentCase}
                          onChange={setStudentCaseInput}
                          employees={employees}
                        />
                        {editingCase !== studentCase.id && (
                          <InlineButton
                            text="Muokkaa"
                            icon={faPen}
                            disabled={
                              editingStudent ||
                              editingCase !== false ||
                              editingCaseEvent
                            }
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
                      <CaseEvents
                        studentCaseId={studentCase.id}
                        disabled={editingStudent || editingCaseEvent}
                        onChangeEditState={setEditingCaseEvent}
                      />
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
