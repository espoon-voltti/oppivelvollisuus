import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { AddButton } from '../shared/buttons/AddButton'
import { formatDate } from '../shared/dates'
import {
  FlexRight,
  PageContainer,
  SectionContainer,
  Table,
  VerticalGap
} from '../shared/layout'
import { Label } from '../shared/typography'

import { StatusChip } from './StatusChip'
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
    <PageContainer>
      <SectionContainer $minHeight="600px">
        <FlexRight>
          <AddButton
            text="Lisää oppivelvollinen"
            onClick={() => navigate('/oppivelvolliset/uusi')}
          />
        </FlexRight>

        <Label>
          Oppivelvollisia{' '}
          {studentsResponse ? `(${studentsResponse.length})` : ''}
        </Label>

        <VerticalGap />

        {studentsResponse
          ? studentsResponse.length > 0 && (
              <Table>
                <thead>
                  <tr>
                    <th>Ilmoitettu</th>
                    <th>Nimi</th>
                    <th>Tila</th>
                    <th>Ohjaaja</th>
                  </tr>
                </thead>
                <tbody>
                  {studentsResponse.map((student) => (
                    <tr key={student.id}>
                      <td>
                        {student.openedAt ? formatDate(student.openedAt) : '-'}
                      </td>
                      <td>
                        <Link to={`/oppivelvolliset/${student.id}`}>
                          {student.lastName} {student.firstName}
                        </Link>
                      </td>
                      <td>
                        {student.status ? (
                          <StatusChip status={student.status} />
                        ) : (
                          <span>Ei ilmoitusta</span>
                        )}
                      </td>
                      <td>{student.assignedTo?.name ?? 'Ei ohjaajaa'}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            )
          : '...'}
      </SectionContainer>
    </PageContainer>
  )
})
