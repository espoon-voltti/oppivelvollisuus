// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo, useState } from 'react'

import { EmployeeUser } from '../employees/api'
import { useDebouncedState } from '../shared/useDebouncedState'

import { CaseSource, caseSources } from './cases/enums'
import { CaseStatus } from './cases/status/enums'

export type Assignee = EmployeeUser | 'NONE'

interface StudentSearchState {
  statuses: CaseStatus[]
  setStatuses: React.Dispatch<React.SetStateAction<CaseStatus[]>>
  sources: CaseSource[]
  setSources: React.Dispatch<React.SetStateAction<CaseSource[]>>
  query: string
  debouncedQuery: string
  setQuery: React.Dispatch<React.SetStateAction<string>>
  assignedTo: Assignee | null
  setAssignedTo: React.Dispatch<React.SetStateAction<Assignee | null>>
}

const defaultState: StudentSearchState = {
  statuses: ['TODO', 'ON_HOLD'],
  setStatuses: () => undefined,
  sources: [...caseSources],
  setSources: () => undefined,
  query: '',
  debouncedQuery: '',
  setQuery: () => undefined,
  assignedTo: null,
  setAssignedTo: () => undefined
}

export const StudentSearchContext =
  createContext<StudentSearchState>(defaultState)

export const StudentSearchProvider = React.memo(function StudentSearchProvider({
  children
}: {
  children: React.JSX.Element
}) {
  const [statuses, setStatuses] = useState<CaseStatus[]>(defaultState.statuses)
  const [sources, setSources] = useState<CaseSource[]>(defaultState.sources)
  const [query, setQuery, debouncedQuery] = useDebouncedState<string>(
    defaultState.query
  )
  const [assignedTo, setAssignedTo] = useState<Assignee | null>(
    defaultState.assignedTo
  )

  const state = useMemo(
    () => ({
      statuses,
      setStatuses,
      sources,
      setSources,
      query,
      debouncedQuery,
      setQuery,
      assignedTo,
      setAssignedTo
    }),
    [
      statuses,
      setStatuses,
      sources,
      setSources,
      query,
      debouncedQuery,
      setQuery,
      assignedTo,
      setAssignedTo
    ]
  )
  return (
    <StudentSearchContext.Provider value={state}>
      {children}
    </StudentSearchContext.Provider>
  )
})
