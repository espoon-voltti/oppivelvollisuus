import React, { createContext, useMemo } from 'react'

import { User } from './auth-status'

export interface UserState {
  user: User | null
}

export const UserContext = createContext<UserState>({
  user: null
})

export const UserContextProvider = React.memo(function UserContextProvider({
  children,
  user
}: {
  children: React.JSX.Element
  user: User | null
}) {
  const value = useMemo(
    () => ({
      user
    }),
    [user]
  )

  return <UserContext.Provider value={value}>{children}</UserContext.Provider>
})
