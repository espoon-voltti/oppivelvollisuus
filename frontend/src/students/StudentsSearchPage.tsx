import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { apiGetEmployees, EmployeeUser } from '../employees/api'
import { SelectionChip } from '../shared/Chip'
import { AddButton } from '../shared/buttons/AddButton'
import { formatDate } from '../shared/dates'
import { InputField } from '../shared/form/InputField'
import { Select } from '../shared/form/Select'
import {
  FlexColWithGaps,
  FlexLeftRight,
  FlexRight,
  FlexRowWithGaps,
  LabeledInputL,
  PageContainer,
  SectionContainer,
  Table,
  VerticalGap
} from '../shared/layout'
import { Label } from '../shared/typography'
import { useDebouncedState } from '../shared/useDebouncedState'

import { StatusChip } from './StatusChip'
import { apiGetStudents, StudentSummary } from './api'
import { CaseStatus, caseStatuses, caseStatusNames } from './enums'
import { faMagnifyingGlass } from '@fortawesome/free-solid-svg-icons/faMagnifyingGlass'
import styled from 'styled-components'

export const StudentsSearchPage = React.memo(function StudentsSearchPage() {
  const navigate = useNavigate()

  const [employees, setEmployees] = useState<EmployeeUser[] | null>(null)
  useEffect(() => {
    void apiGetEmployees().then(setEmployees)
  }, [])

  const [statuses, setStatuses] = useState<CaseStatus[]>(['TODO', 'ON_HOLD'])
  const [query, setQuery, debouncedQuery] = useDebouncedState<string>('')
  const [assignedTo, setAssignedTo] = useState<EmployeeUser | null>(null)

  const [studentsResponse, setStudentsResponse] = useState<
    StudentSummary[] | null
  >(null)
  useEffect(() => {
    setStudentsResponse(null)
    void apiGetStudents({
      query: debouncedQuery,
      statuses,
      assignedTo: assignedTo?.id ?? null
    }).then(setStudentsResponse)
  }, [debouncedQuery, statuses, assignedTo])

  return (
    <PageContainer>
      <SectionContainer $minHeight="600px">
        <FlexColWithGaps $gapSize="m">
          <FlexLeftRight style={{ alignItems: 'flex-start' }}>
            <LabeledInputL>
              <Label>Haku nimellä tai hetulla</Label>
              <InputField value={query} onChange={setQuery} icon={faMagnifyingGlass} />
            </LabeledInputL>
            <FlexRight>
              <AddButton
                text="Lisää oppivelvollinen"
                onClick={() => navigate('/oppivelvolliset/uusi')}
              />
            </FlexRight>
          </FlexLeftRight>
          <FlexRowWithGaps
            $gapSize="L"
            style={{ justifyContent: 'space-between' }}
          >
            <FlexColWithGaps>
              <Label>Näytettävät tilat</Label>
              <FlexRowWithGaps $gapSize="s">
                {caseStatuses.map((status) => (
                  <SelectionChip
                    key={status}
                    text={caseStatusNames[status]}
                    selected={statuses.includes(status)}
                    onChange={(checked) => {
                      if (checked) {
                        setStatuses((prev) => [...prev, status])
                      } else {
                        setStatuses((prev) => prev.filter((s) => s !== status))
                      }
                    }}
                  />
                ))}
              </FlexRowWithGaps>
            </FlexColWithGaps>
            <LabeledInputL>
              <Label>Ohjaaja</Label>
              {employees ? (
                <Select<EmployeeUser>
                  items={employees}
                  selectedItem={assignedTo}
                  getItemValue={(e) => e.id}
                  getItemLabel={(e) => `${e.firstName} ${e.lastName}`}
                  placeholder="Näytä kaikki"
                  onChange={setAssignedTo}
                />
              ) : (
                <span>...</span>
              )}
            </LabeledInputL>
          </FlexRowWithGaps>
        </FlexColWithGaps>

        <VerticalGap $size="L" />

        <Label>
          Oppivelvollisia{' '}
          {studentsResponse ? `(${studentsResponse.length})` : ''}
        </Label>

        <VerticalGap />

        {studentsResponse
          ? studentsResponse.length > 0 && (
              <Table style={{ width: '100%' }}>
                <thead>
                  <tr>
                    <Th style={{ width: '160px' }}>Ilmoitettu</Th>
                    <Th style={{ width: '40%' }}>Nimi</Th>
                    <Th>Ohjaaja</Th>
                    <Th style={{ width: '200px' }}/>
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
                      <td>{student.assignedTo?.name ?? 'Ei ohjaajaa'}</td>
                      <td>
                        {student.status ? (
                          <StatusChip status={student.status} />
                        ) : (
                          <span>Ei ilmoitusta</span>
                        )}
                      </td>
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

const Th = styled.th`
  text-align: left;
`
