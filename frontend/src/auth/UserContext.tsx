import React, { createContext, useMemo } from 'react'

import { EmployeeUser } from '../employees/api'

export interface UserState {
  user: EmployeeUser | null
}

export const UserContext = createContext<UserState>({
  user: null
})

export const UserContextProvider = React.memo(function UserContextProvider({
  children,
  user
}: {
  children: React.JSX.Element
  user: EmployeeUser | null
}) {
  const value = useMemo(
    () => ({
      user
    }),
    [user]
  )

  return <UserContext.Provider value={value}>{children}</UserContext.Provider>
})
