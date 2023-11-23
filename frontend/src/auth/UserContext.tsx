import React, { createContext, useMemo, useState } from 'react'

export interface User {
  name: string
}

export interface UserState {
  user: User | null
  setLoggedIn: (user: User) => void
  setLoggedOut: () => void
}

export const UserContext = createContext<UserState>({
  user: {
    name: 'Tessa Testaaja'
  },
  setLoggedIn: () => undefined,
  setLoggedOut: () => undefined
})

export const UserContextProvider = React.memo(function UserContextProvider({
  children
}: {
  children: React.JSX.Element
}) {
  const [user, setUser] = useState<User | null>({
    name: 'Tessa Testaaja'
  })

  const value = useMemo(
    () => ({
      user,
      setLoggedIn: setUser,
      setLoggedOut: () => setUser(null)
    }),
    [user, setUser]
  )

  return <UserContext.Provider value={value}>{children}</UserContext.Provider>
})
