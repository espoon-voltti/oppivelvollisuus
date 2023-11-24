import React, { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { formatDate } from '../shared/dates'
import {
  FlexColWithGaps,
  FlexLeftRight,
  FlexRowWithGaps,
  VerticalGap
} from '../shared/layout'
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

export const StudentPage = React.memo(function StudentPage() {
  const { id } = useParams()
  if (!id) throw Error('Id not found in path')

  const [studentResponse, setStudentResponse] =
    useState<StudentResponse | null>(null)
  const loadStudent = useCallback(() => {
    setStudentResponse(null)
    void apiGetStudent(id).then(setStudentResponse)
  }, [id])
  useEffect(loadStudent, [loadStudent])

  const [editing, setEditing] = useState(false)
  const [studentInput, setStudentInput] = useState<StudentInput | null>(null)

  const [editingCase, setEditingCase] = useState<boolean | string>(false)
  const [studentCaseInput, setStudentCaseInput] =
    useState<StudentCaseInput | null>(null)

  const [submitting, setSubmitting] = useState(false)

  return (
    <div>
      <Link to="/oppivelvolliset">Takaisin</Link>
      <VerticalGap $size="L" />

      <H2>
        {studentResponse
          ? `${studentResponse.student.firstName} ${studentResponse.student.lastName}`
          : ''}
      </H2>

      <VerticalGap $size="L" />

      {studentResponse && (
        <div>
          <H3>Oppivelvollisen tiedot</H3>
          <VerticalGap $size="m" />
          <StudentForm
            key={editing ? 'student-form' : 'student-read-view'}
            editing={editing}
            student={studentResponse.student}
            onChange={setStudentInput}
          />
          <VerticalGap $size="m" />
          {editing ? (
            <FlexRowWithGaps>
              <button
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
              >
                Tallenna
              </button>
              <button disabled={submitting} onClick={() => setEditing(false)}>
                Peruuta
              </button>
            </FlexRowWithGaps>
          ) : (
            <button
              disabled={editingCase !== false}
              onClick={() => setEditing(true)}
            >
              Muokkaa
            </button>
          )}

          <VerticalGap $size="L" />

          <hr />

          <VerticalGap $size="L" />

          <FlexLeftRight>
            <H3>Seurantatapaukset</H3>
            <button
              onClick={() => setEditingCase(true)}
              disabled={editing || editingCase !== false}
            >
              Luo uusi
            </button>
          </FlexLeftRight>
          <VerticalGap $size="m" />
          {editingCase === true && (
            <FlexColWithGaps>
              <H3>Uusi ilmoitus</H3>
              <StudentCaseForm onChange={setStudentCaseInput} />
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
              <VerticalGap $size="m" />
            </FlexColWithGaps>
          )}
          <FlexColWithGaps $gapSize="L">
            {studentResponse.cases.map((studentCase) => (
              <FlexColWithGaps key={studentCase.id}>
                <FlexRowWithGaps>
                  <H3>Ilmoitus {formatDate(studentCase.openedAt)}</H3>
                  {editingCase !== studentCase.id && (
                    <button
                      disabled={editing || editingCase !== false}
                      onClick={() => setEditingCase(studentCase.id)}
                    >
                      Muokkaa
                    </button>
                  )}
                </FlexRowWithGaps>
                <StudentCaseForm
                  key={`${studentCase.id}-${
                    editingCase ? 'form' : 'read-view'
                  }`}
                  studentCase={studentCase}
                  editing={editingCase === studentCase.id}
                  onChange={setStudentCaseInput}
                />
                {editingCase === studentCase.id && (
                  <FlexRowWithGaps>
                    <button
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
                )}
              </FlexColWithGaps>
            ))}
          </FlexColWithGaps>
        </div>
      )}
    </div>
  )
})
