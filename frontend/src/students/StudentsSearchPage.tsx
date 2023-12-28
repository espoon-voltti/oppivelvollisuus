import { faBroom } from '@fortawesome/free-solid-svg-icons/faBroom'
import { faChartPie } from '@fortawesome/free-solid-svg-icons/faChartPie'
import { faMagnifyingGlass } from '@fortawesome/free-solid-svg-icons/faMagnifyingGlass'
import React, { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { apiGetEmployees, EmployeeUser } from '../employees/api'
import { SelectionChip } from '../shared/Chip'
import { AddButton } from '../shared/buttons/AddButton'
import { InlineButton } from '../shared/buttons/InlineButton'
import { formatDate } from '../shared/dates'
import { InputField } from '../shared/form/InputField'
import { Select } from '../shared/form/Select'
import {
  FlexColWithGaps,
  FlexLeftRight,
  FlexRowWithGaps,
  LabeledInput,
  PageContainer,
  SectionContainer,
  Table,
  VerticalGap
} from '../shared/layout'
import { Label } from '../shared/typography'
import { useDebouncedState } from '../shared/useDebouncedState'

import { apiDeleteOldStudents, apiGetStudents, StudentSummary } from './api'
import { StatusChip } from './cases/StatusChip'
import { CaseStatus, caseStatuses, caseStatusNames } from './cases/status/enums'

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

  const loadStudents = useCallback(() => {
    setStudentsResponse(null)
    void apiGetStudents({
      query: debouncedQuery,
      statuses,
      assignedTo: assignedTo?.id ?? null
    }).then(setStudentsResponse)
  }, [debouncedQuery, statuses, assignedTo])

  useEffect(() => {
    loadStudents()
  }, [loadStudents])

  const [deletingOldStudents, setDeletingOldStudents] = useState(false)

  return (
    <PageContainer>
      <SectionContainer $minHeight="600px">
        <FlexColWithGaps $gapSize="m">
          <FlexLeftRight>
            <FlexRowWithGaps $gapSize="L">
              <InlineButton
                text="Raportointi"
                icon={faChartPie}
                onClick={() => navigate('/raportointi')}
              />
              <InlineButton
                text="Poista yli 21-vuotiaat"
                icon={faBroom}
                disabled={deletingOldStudents}
                onClick={() => {
                  setDeletingOldStudents(true)
                  void apiDeleteOldStudents()
                    .then(() => window.alert('Siivousoperaatio onnistui'))
                    .catch(() =>
                      window.alert(
                        'Siivousoperaatio epäonnistui: yritä uudelleen'
                      )
                    )
                    .finally(() => {
                      setDeletingOldStudents(false)
                      loadStudents()
                    })
                }}
              />
            </FlexRowWithGaps>
            <AddButton
              text="Lisää oppivelvollinen"
              onClick={() => navigate('/oppivelvolliset/uusi')}
              data-qa="create-student-button"
            />
          </FlexLeftRight>
          <LabeledInput $cols={6}>
            <Label>Haku nimellä tai hetulla</Label>
            <InputField
              value={query}
              onChange={setQuery}
              icon={faMagnifyingGlass}
            />
          </LabeledInput>
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
            <LabeledInput $cols={6}>
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
            </LabeledInput>
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
                    <Th style={{ width: '200px' }} />
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
