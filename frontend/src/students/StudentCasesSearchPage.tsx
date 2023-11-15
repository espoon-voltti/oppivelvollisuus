import sortBy from 'lodash/sortBy'
import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

import { formatDate } from '../shared/dates'
import { Table, VerticalGap } from '../shared/layout'
import { H1 } from '../shared/typography'

import { apiGetStudentCases, StudentCaseSummary } from './api'

export const StudentCasesSearchPage = React.memo(
  function StudentCasesSearchPage() {
    const [studentCasesResponse, setStudentCasesResponse] = useState<
      StudentCaseSummary[] | null
    >(null)
    useEffect(() => {
      void apiGetStudentCases().then(setStudentCasesResponse)
    }, [])

    const sorted = studentCasesResponse
      ? sortBy(
          studentCasesResponse,
          (c) => c.openedAt,
          (c) => c.lastName,
          (c) => c.firstName
        )
      : null

    return (
      <div>
        <H1>Oppivelvollisuuden seurantatapaukset</H1>

        <VerticalGap $size="L" />

        {sorted && (
          <Table>
            <tr>
              <th>Tapaus vastaanotettu</th>
              <th>Nimi</th>
            </tr>
            {sorted.map((studentCase) => (
              <tr key={studentCase.id}>
                <td>{formatDate(studentCase.openedAt)}</td>
                <td>
                  <Link to={`/oppivelvolliset/${studentCase.studentId}`}>
                    {studentCase.lastName} {studentCase.firstName}
                  </Link>
                </td>
              </tr>
            ))}
          </Table>
        )}
      </div>
    )
  }
)
