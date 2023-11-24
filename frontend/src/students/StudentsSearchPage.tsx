import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { formatDate } from '../shared/dates'
import { FlexLeftRight, Table, VerticalGap } from '../shared/layout'
import { Label } from '../shared/typography'

import { apiGetStudents, StudentSummary } from './api'

export const StudentsSearchPage = React.memo(function StudentsSearchPage() {
  const navigate = useNavigate()
  const [studentsResponse, setStudentsResponse] = useState<
    StudentSummary[] | null
  >(null)
  useEffect(() => {
    void apiGetStudents().then(setStudentsResponse)
  }, [])

  return (
    <div>
      <VerticalGap $size="L" />
      <FlexLeftRight>
        <Label>
          Oppivelvollisia{' '}
          {studentsResponse ? `(${studentsResponse.length})` : ''}
        </Label>
        <button onClick={() => navigate('/oppivelvolliset/uusi')}>
          Lisää uusi
        </button>
      </FlexLeftRight>

      <VerticalGap />

      {studentsResponse && (
        <Table>
          <tr>
            <th>Nimi</th>
            <th>Ilmoitettu</th>
          </tr>
          {studentsResponse.map((student) => (
            <tr key={student.id}>
              <td>
                <Link to={`/oppivelvolliset/${student.id}`}>
                  {student.lastName} {student.firstName}
                </Link>
              </td>
              <td>{student.openedAt ? formatDate(student.openedAt) : '-'}</td>
            </tr>
          ))}
        </Table>
      )}
    </div>
  )
})
