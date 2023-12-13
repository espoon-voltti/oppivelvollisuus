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
import { H2, H3, H4 } from '../shared/typography'

import { CaseEvents } from './CaseEvents'
import { CaseStatusForm } from './CaseStatusForm'
import { StatusChip } from './StatusChip'
import { StudentCaseForm } from './StudentCaseForm'
import { StudentForm } from './StudentForm'
import {
  apiGetStudent,
  apiPutStudent,
  StudentInput,
  StudentResponse,
  StudentCaseInput,
  apiPostStudentCase,
  apiPutStudentCase,
  CaseStatusInput,
  apiPutStudentCaseStatus
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

  const [expandedCase, setExpandedCase] = useState<string | null>(null)

  // true = creating new, string = id of the edited case
  const [editingCase, setEditingCase] = useState<boolean | string>(false)
  const [studentCaseInput, setStudentCaseInput] =
    useState<StudentCaseInput | null>(null)

  const [editingCaseStatus, setEditingCaseStatus] = useState<string | null>(
    null
  )
  const [caseStatusInput, setCaseStatusInput] =
    useState<CaseStatusInput | null>(null)

  const [editingCaseEvent, setEditingCaseEvent] = useState(false)

  const [submitting, setSubmitting] = useState(false)

  const editingSomething = !!(
    editingStudent ||
    editingCase ||
    editingCaseStatus ||
    editingCaseEvent
  )
  const activeCaseExists =
    studentResponse?.cases?.some((c) => c.status !== 'FINISHED') ?? false

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
            ? `${studentResponse.student.lastName} ${studentResponse.student.firstName}`
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
                disabled={editingSomething}
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
                  disabled={editingSomething || activeCaseExists}
                  onClick={() => {
                    setEditingCase(true)
                    setExpandedCase(null)
                  }}
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
                    $disabled={
                      editingCase !== false ||
                      editingCaseStatus !== null ||
                      editingCaseEvent
                    }
                    onClick={() => {
                      if (
                        editingCase !== false ||
                        editingCaseStatus !== null ||
                        editingCaseEvent
                      )
                        return

                      if (expandedCase === studentCase.id) {
                        setExpandedCase(null)
                      } else {
                        setExpandedCase(studentCase.id)
                      }
                    }}
                  >
                    <H3>Ilmoitus {formatDate(studentCase.openedAt)}</H3>
                    <FlexRowWithGaps $gapSize="m">
                      <FlexRowWithGaps $gapSize="XL">
                        <span>
                          {studentCase.assignedTo?.name ?? 'Ei ohjaajaa'}
                        </span>
                        <StatusChip status={studentCase.status} />
                      </FlexRowWithGaps>
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
                    <FlexColWithGaps $gapSize="XL">
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
                            disabled={editingSomething}
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

                      <FlexColWithGaps>
                        <H4>Ohjauksen tila</H4>
                        <FlexLeftRight style={{ alignItems: 'flex-start' }}>
                          <div style={{ flexGrow: 1 }}>
                            {editingCaseStatus === studentCase.id ? (
                              <FlexColWithGaps>
                                <CaseStatusForm
                                  mode="EDIT"
                                  studentCase={studentCase}
                                  onChange={setCaseStatusInput}
                                  activeCaseExists={activeCaseExists}
                                />
                                <FlexRight>
                                  <FlexRowWithGaps>
                                    <Button
                                      text="Peruuta"
                                      disabled={submitting}
                                      onClick={() => setEditingCaseStatus(null)}
                                    />
                                    <Button
                                      text="Tallenna"
                                      primary
                                      disabled={submitting || !caseStatusInput}
                                      onClick={() => {
                                        if (!caseStatusInput) return

                                        setSubmitting(true)
                                        void apiPutStudentCaseStatus(
                                          studentCase.studentId,
                                          studentCase.id,
                                          caseStatusInput
                                        )
                                          .then(() => {
                                            setEditingCaseStatus(null)
                                            loadStudent()
                                          })
                                          .finally(() => setSubmitting(false))
                                      }}
                                    />
                                  </FlexRowWithGaps>
                                </FlexRight>
                              </FlexColWithGaps>
                            ) : (
                              <CaseStatusForm
                                mode="VIEW"
                                studentCase={studentCase}
                              />
                            )}
                          </div>
                          {editingCaseStatus !== studentCase.id && (
                            <InlineButton
                              text="Vaihda tilaa"
                              icon={faPen}
                              disabled={editingSomething}
                              onClick={() =>
                                setEditingCaseStatus(studentCase.id)
                              }
                            />
                          )}
                        </FlexLeftRight>
                      </FlexColWithGaps>

                      <CaseEvents
                        studentCaseId={studentCase.id}
                        disabled={editingSomething}
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
