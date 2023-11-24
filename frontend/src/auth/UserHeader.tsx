import React, { useContext } from 'react'

import { FlexRowWithGaps } from '../shared/layout'

import { UserContext } from './UserContext'

export const UserHeader = React.memo(function UserHeader() {
  const { user, setLoggedOut } = useContext(UserContext)

  if (!user) return null

  return (
    <FlexRowWithGaps>
      <span>{user.name}</span>
      <a href="#" onClick={setLoggedOut}>
        Kirjaudu ulos
      </a>
    </FlexRowWithGaps>
  )
})
