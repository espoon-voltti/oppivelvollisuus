import React, { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import {
  FlexColWithGaps,
  FlexLeftRight,
  FlexRowWithGaps,
  VerticalGap
} from '../shared/layout'
import { H1, H2 } from '../shared/typography'

import { StudentCaseForm } from './StudentCaseForm'
import { StudentForm } from './StudentForm'
import {
  apiGetStudent,
  apiPutStudent,
  StudentInput,
  StudentResponse
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

  const [submitting, setSubmitting] = useState(false)

  return (
    <div>
      <Link to="/oppivelvolliset">Takaisin</Link>
      <VerticalGap $size="L" />

      <H1>
        {studentResponse
          ? `${studentResponse.student.firstName} ${studentResponse.student.lastName}`
          : ''}
      </H1>

      <VerticalGap $size="L" />

      {studentResponse && (
        <div>
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
                  if (submitting || !studentInput) return

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
            <button onClick={() => setEditing(true)}>Muokkaa</button>
          )}

          <VerticalGap $size="L" />

          <FlexLeftRight>
            <H2>Seurantatapaukset</H2>
            <button onClick={() => setEditingCase(true)} disabled={editing}>
              Luo uusi
            </button>
          </FlexLeftRight>
          <VerticalGap $size="m" />
          {editingCase === true && (
            <StudentCaseForm
              studentId={id}
              onSaved={() => {
                setEditingCase(false)
                loadStudent()
              }}
              onCancelled={() => setEditingCase(false)}
            />
          )}
          <FlexColWithGaps>
            {studentResponse.cases.map((studentCase) => (
              <StudentCaseForm
                key={studentCase.id}
                studentCase={studentCase}
                editing={editingCase === studentCase.id}
                onStartEdit={() => setEditingCase(studentCase.id)}
                onSaved={() => {
                  setEditingCase(false)
                  loadStudent()
                }}
                onCancelled={() => setEditingCase(false)}
              />
            ))}
          </FlexColWithGaps>
        </div>
      )}
    </div>
  )
})
