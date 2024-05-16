// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  faChevronDown,
  faChevronLeft,
  faChevronUp,
  faPen,
  faPlus
} from '@fortawesome/free-solid-svg-icons'
import { faTrash } from '@fortawesome/free-solid-svg-icons/faTrash'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { differenceInYears } from 'date-fns/differenceInYears'
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
import { useWarnOnUnsavedChanges } from '../shared/useWarnOnUnsavedChanges'

import { StudentForm } from './StudentForm'
import {
  apiDeleteStudent,
  apiGetStudent,
  apiPutStudent,
  StudentInput,
  StudentResponse
} from './api'
import { StatusChip } from './cases/StatusChip'
import { StudentCaseForm } from './cases/StudentCaseForm'
import {
  apiDeleteStudentCase,
  apiPostStudentCase,
  apiPutStudentCase,
  StudentCase,
  StudentCaseInput
} from './cases/api'
import { CaseEvents } from './cases/events/CaseEvents'
import { CaseStatusForm } from './cases/status/CaseStatusForm'
import { apiPutStudentCaseStatus, CaseStatusInput } from './cases/status/api'

const CollapsableRow = styled(FlexLeftRight)<{ $disabled?: boolean }>`
  ${(p) => (p.$disabled ? '' : 'cursor: pointer;')}
  user-select: none;

  .collapse-icon {
    color: ${colors.grayscale.g35};
    font-size: 16px;
  }
`

const AccordionRow = styled(CollapsableRow)`
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
    })
  }, [id])
  useEffect(loadStudent, [loadStudent])

  const [expandedStudentDetails, setExpandedStudentDetails] = useState(false)
  const [editingStudent, setEditingStudent] = useState(false)
  const [studentInput, setStudentInput] = useState<StudentInput | null>(null)

  const [expandedCase, setExpandedCase] = useState<string | null | undefined>(
    undefined
  )
  useEffect(() => {
    if (expandedCase === undefined && studentResponse) {
      setExpandedCase(
        studentResponse.cases.find((c) => c.status !== 'FINISHED')?.id ?? null
      )
    }
  }, [studentResponse, expandedCase])

  // true = creating new, string = id of the edited case
  const [editingCase, setEditingCase] = useState<boolean | string>(false)
  const [studentCaseInput, setStudentCaseInput] =
    useState<StudentCaseInput | null>(null)

  const [editingCaseStatus, setEditingCaseStatus] = useState<string | null>(
    null
  )

  // true = creating new, string = id of the edited case event
  const [editingCaseEvent, setEditingCaseEvent] = useState<boolean | string>(
    false
  )

  const [submitting, setSubmitting] = useState(false)

  const editingSomething = !!(
    editingStudent ||
    editingCase ||
    editingCaseStatus ||
    editingCaseEvent
  )

  const activeCase = studentResponse?.cases?.find(
    (c) => c.status !== 'FINISHED'
  )
  const finishedCases =
    studentResponse?.cases?.filter((c) => c.status === 'FINISHED') ?? []
  const activeCaseExists = activeCase !== undefined

  useWarnOnUnsavedChanges(editingSomething)

  return (
    <PageContainer>
      <SectionContainer>
        <InlineButton
          text="Takaisin"
          icon={faChevronLeft}
          onClick={() => navigate('/oppivelvolliset')}
          disabled={editingSomething}
        />
        <VerticalGap $size="m" />
        <FlexLeftRight>
          <H2 data-qa="student-name">
            {studentResponse
              ? `${studentResponse.student.lastName} ${studentResponse.student.firstName} 
              (${formatDate(studentResponse.student.dateOfBirth)} - 
              ${differenceInYears(new Date(), studentResponse.student.dateOfBirth)}v)`
              : ''}
          </H2>
          {studentResponse && (
            <InlineButton
              text="Poista oppivelvollisen tiedot"
              icon={faTrash}
              disabled={editingSomething}
              onClick={() => {
                if (studentResponse.cases.length > 0) {
                  window.alert(
                    'Jos haluat poistaa oppivelvollisen tiedot, poista ensin kaikki ilmoitukset'
                  )
                } else {
                  if (
                    window.confirm(
                      'Haluatko varmasti poistaa oppivelvollisen tiedot?'
                    )
                  ) {
                    void apiDeleteStudent(id).then(() =>
                      navigate('/oppivelvolliset')
                    )
                  }
                }
              }}
            />
          )}
        </FlexLeftRight>
      </SectionContainer>

      <VerticalGap $size="m" />

      {studentResponse && employees && (
        <>
          <SectionContainer>
            <CollapsableRow
              onClick={() => setExpandedStudentDetails((prev) => !prev)}
            >
              <H3>Oppivelvollisen tiedot</H3>
              <FlexRowWithGaps $gapSize="m">
                <FontAwesomeIcon
                  icon={expandedStudentDetails ? faChevronUp : faChevronDown}
                  className="collapse-icon"
                />
              </FlexRowWithGaps>
            </CollapsableRow>
            {expandedStudentDetails && (
              <FlexColWithGaps $gapSize="s">
                <VerticalGap $size="s" />
                <FlexRight>
                  <FlexRowWithGaps $gapSize="m">
                    <InlineButton
                      text="Muokkaa"
                      icon={faPen}
                      disabled={editingSomething}
                      onClick={() => setEditingStudent(true)}
                    />
                  </FlexRowWithGaps>
                </FlexRight>
                <StudentForm
                  key={editingStudent ? 'EDIT' : 'VIEW'}
                  mode={editingStudent ? 'EDIT' : 'VIEW'}
                  student={studentResponse.student}
                  onChange={setStudentInput}
                />
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
              </FlexColWithGaps>
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

            <CasesList
              studentId={studentResponse.student.id}
              cases={activeCase ? [activeCase] : []}
              employees={employees}
              expandedCase={expandedCase ?? null}
              setExpandedCase={setExpandedCase}
              editingCase={editingCase}
              setEditingCase={setEditingCase}
              editingCaseStatus={editingCaseStatus}
              setEditingCaseStatus={setEditingCaseStatus}
              editingCaseEvent={editingCaseEvent}
              setEditingCaseEvent={setEditingCaseEvent}
              editingSomething={editingSomething}
              activeCaseExists={activeCaseExists}
              submitting={submitting}
              setSubmitting={setSubmitting}
              loadStudent={loadStudent}
            />

            <VerticalGap $size="L" />

            <H4>Vanhat ilmoitukset ({finishedCases.length})</H4>
            <VerticalGap $size="m" />
            <CasesList
              studentId={studentResponse.student.id}
              cases={finishedCases}
              employees={employees}
              expandedCase={expandedCase ?? null}
              setExpandedCase={setExpandedCase}
              editingCase={editingCase}
              setEditingCase={setEditingCase}
              editingCaseStatus={editingCaseStatus}
              setEditingCaseStatus={setEditingCaseStatus}
              editingCaseEvent={editingCaseEvent}
              setEditingCaseEvent={setEditingCaseEvent}
              editingSomething={editingSomething}
              activeCaseExists={activeCaseExists}
              submitting={submitting}
              setSubmitting={setSubmitting}
              loadStudent={loadStudent}
            />
          </SectionContainer>
        </>
      )}
    </PageContainer>
  )
})

const CasesList = React.memo(function CasesList({
  studentId,
  cases,
  employees,
  expandedCase,
  setExpandedCase,
  editingCase,
  setEditingCase,
  editingCaseStatus,
  setEditingCaseStatus,
  editingCaseEvent,
  setEditingCaseEvent,
  editingSomething,
  activeCaseExists,
  submitting,
  setSubmitting,
  loadStudent
}: {
  studentId: string
  cases: StudentCase[]
  employees: EmployeeUser[]
  expandedCase: string | null
  setExpandedCase: (id: string | null) => void
  editingCase: boolean | string
  setEditingCase: (editing: boolean | string) => void
  editingCaseStatus: string | null
  setEditingCaseStatus: (editing: string | null) => void
  editingCaseEvent: boolean | string
  setEditingCaseEvent: (editing: boolean | string) => void
  editingSomething: boolean
  activeCaseExists: boolean
  submitting: boolean
  setSubmitting: (value: boolean) => void
  loadStudent: () => void
}) {
  const [studentCaseInput, setStudentCaseInput] =
    useState<StudentCaseInput | null>(null)
  const [caseStatusInput, setCaseStatusInput] =
    useState<CaseStatusInput | null>(null)

  return (
    <FlexColWithGaps $gapSize="L">
      {cases.map((studentCase) => (
        <FlexColWithGaps key={studentCase.id}>
          <AccordionRow
            $disabled={
              editingCase !== false ||
              editingCaseStatus !== null ||
              !!editingCaseEvent
            }
            onClick={() => {
              if (
                editingCase !== false ||
                editingCaseStatus !== null ||
                !!editingCaseEvent
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
                <span>{studentCase.assignedTo?.name ?? 'Ei ohjaajaa'}</span>
                <StatusChip status={studentCase.status} />
              </FlexRowWithGaps>
              <FontAwesomeIcon
                icon={
                  expandedCase === studentCase.id ? faChevronUp : faChevronDown
                }
                className="collapse-icon"
              />
            </FlexRowWithGaps>
          </AccordionRow>
          {expandedCase === studentCase.id && (
            <FlexColWithGaps $gapSize="XL">
              <FlexColWithGaps>
                {editingCase !== studentCase.id && (
                  <FlexRight style={{ marginBottom: '-24px', zIndex: 1 }}>
                    <FlexRowWithGaps $gapSize="m">
                      <InlineButton
                        text="Muokkaa"
                        icon={faPen}
                        disabled={editingSomething}
                        onClick={() => setEditingCase(studentCase.id)}
                      />
                      <InlineButton
                        text="Poista"
                        icon={faTrash}
                        disabled={editingSomething}
                        onClick={() => {
                          if (studentCase.events.length > 0) {
                            window.alert(
                              'Jos haluat poistaa ilmoituksen, poista ensin kaikki ilmoituksen muistiinpanot ja toimenpiteet'
                            )
                          } else {
                            if (
                              window.confirm(
                                'Haluatko varmasti poistaa ilmoituksen?'
                              )
                            ) {
                              void apiDeleteStudentCase(
                                studentId,
                                studentCase.id
                              ).then(() => loadStudent())
                            }
                          }
                        }}
                      />
                    </FlexRowWithGaps>
                  </FlexRight>
                )}
                <StudentCaseForm
                  key={editingCase === studentCase.id ? 'EDIT' : 'VIEW'}
                  mode={editingCase === studentCase.id ? 'EDIT' : 'VIEW'}
                  studentCase={studentCase}
                  onChange={setStudentCaseInput}
                  employees={employees}
                />
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
                            studentId,
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

              <FlexColWithGaps>
                <FlexLeftRight>
                  <H4>Ohjauksen tila</H4>
                  {editingCaseStatus !== studentCase.id && (
                    <InlineButton
                      text="Vaihda tilaa"
                      icon={faPen}
                      disabled={editingSomething}
                      onClick={() => setEditingCaseStatus(studentCase.id)}
                    />
                  )}
                </FlexLeftRight>
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
                  <CaseStatusForm mode="VIEW" studentCase={studentCase} />
                )}
              </FlexColWithGaps>

              <CaseEvents
                events={studentCase.events}
                studentCaseId={studentCase.id}
                reload={loadStudent}
                disabled={editingSomething}
                editingCaseEvent={editingCaseEvent}
                setEditingCaseEvent={setEditingCaseEvent}
              />
            </FlexColWithGaps>
          )}
        </FlexColWithGaps>
      ))}
    </FlexColWithGaps>
  )
})
