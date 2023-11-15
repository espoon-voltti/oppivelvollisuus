import React, { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'

import {
  FlexColWithGaps,
  FlexLeftRight,
  FlexRowWithGaps,
  VerticalGap
} from '../shared/layout'
import { H1, H2, Label } from '../shared/typography'

import { StudentCaseForm } from './StudentCaseForm'
import {
  apiGetStudent,
  apiGetStudentCasesByStudent,
  apiPutStudent,
  StudentBasics,
  StudentCase
} from './api'

export const StudentPage = React.memo(function StudentPage() {
  const { id } = useParams()
  if (!id) throw Error('Id not found in path')

  const [studentResponse, setStudentResponse] = useState<StudentBasics | null>(
    null
  )
  const loadStudent = useCallback(() => {
    setStudentResponse(null)
    void apiGetStudent(id).then(setStudentResponse)
  }, [id])
  useEffect(loadStudent, [loadStudent])

  const [studentCasesResponse, setStudentCasesResponse] = useState<
    StudentCase[] | null
  >(null)
  const loadStudentCases = useCallback(() => {
    setStudentCasesResponse(null)
    void apiGetStudentCasesByStudent(id).then(setStudentCasesResponse)
  }, [id])
  useEffect(loadStudentCases, [loadStudentCases])

  const [editing, setEditing] = useState(false)
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const [editingCase, setEditingCase] = useState<boolean | string>(false)

  const startEditing = () => {
    if (!studentResponse) return

    setFirstName(studentResponse.firstName)
    setLastName(studentResponse.lastName)
    setEditing(true)
  }

  const valid = firstName.trim() && lastName.trim()

  return (
    <div>
      <H1>Oppivelvollinen</H1>

      <VerticalGap $size="L" />

      {studentResponse && (
        <div>
          <FlexColWithGaps $gapSize="m">
            <FlexColWithGaps>
              <Label>Etunimi</Label>
              {editing ? (
                <input
                  type="text"
                  onChange={(e) => setFirstName(e.target.value)}
                  value={firstName}
                />
              ) : (
                <span>{studentResponse.firstName}</span>
              )}
            </FlexColWithGaps>
            <FlexColWithGaps>
              <Label>Sukunimi</Label>
              {editing ? (
                <input
                  type="text"
                  onChange={(e) => setLastName(e.target.value)}
                  value={lastName}
                />
              ) : (
                <span>{studentResponse.lastName}</span>
              )}
            </FlexColWithGaps>
          </FlexColWithGaps>

          <VerticalGap $size="m" />

          {editing ? (
            <FlexRowWithGaps>
              <button
                disabled={submitting || !valid}
                onClick={() => {
                  setSubmitting(true)
                  void apiPutStudent(id, {
                    firstName: firstName.trim(),
                    lastName: lastName.trim()
                  })
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
            <button onClick={() => startEditing()}>Muokkaa</button>
          )}

          <VerticalGap $size="L" />

          {studentCasesResponse && (
            <div>
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
                    loadStudentCases()
                  }}
                  onCancelled={() => setEditingCase(false)}
                />
              )}
              <FlexColWithGaps>
                {studentCasesResponse.map((studentCase) => (
                  <StudentCaseForm
                    key={studentCase.id}
                    studentCase={studentCase}
                    editing={editingCase === studentCase.id}
                    onStartEdit={() => setEditingCase(studentCase.id)}
                    onSaved={() => {
                      setEditingCase(false)
                      loadStudentCases()
                    }}
                    onCancelled={() => setEditingCase(false)}
                  />
                ))}
              </FlexColWithGaps>
            </div>
          )}
        </div>
      )}
    </div>
  )
})
