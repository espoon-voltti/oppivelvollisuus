import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { FlexLeftRight, Table, VerticalGap } from '../shared/layout'
import { H1 } from '../shared/typography'

import { apiGetStudents, StudentBasics } from './api'

export const StudentsSearchPage = React.memo(function StudentsSearchPage() {
  const navigate = useNavigate()
  const [studentsResponse, setStudentsResponse] = useState<
    StudentBasics[] | null
  >(null)
  useEffect(() => {
    void apiGetStudents().then(setStudentsResponse)
  }, [])

  return (
    <div>
      <FlexLeftRight>
        <H1>Oppivelvolliset</H1>
        <button onClick={() => navigate('/oppivelvolliset/uusi')}>
          Lisää uusi
        </button>
      </FlexLeftRight>

      <VerticalGap $size="L" />

      {studentsResponse && (
        <Table>
          <tr>
            <th>Nimi</th>
          </tr>
          {studentsResponse.map((student) => (
            <tr key={student.id}>
              <td>
                <Link to={`/oppivelvolliset/${student.id}`}>
                  {student.lastName} {student.firstName}
                </Link>
              </td>
            </tr>
          ))}
        </Table>
      )}
    </div>
  )
})
