import React, { useContext } from 'react'

import { FlexRowWithGaps } from '../shared/layout'
import { Label } from '../shared/typography'

import { UserContext } from './UserContext'

export const UserHeader = React.memo(function UserHeader() {
  const { user, setLoggedOut } = useContext(UserContext)

  if (!user) return null

  return (
    <FlexRowWithGaps>
      <Label>{user.name}</Label>
      <button onClick={setLoggedOut}>Kirjaudu ulos</button>
    </FlexRowWithGaps>
  )
})
