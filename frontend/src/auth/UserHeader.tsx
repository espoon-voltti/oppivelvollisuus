import React, { useContext } from 'react'

import { FlexRowWithGaps } from '../shared/layout'

import { UserContext } from './UserContext'

export const logoutUrl = `/api/auth/saml/logout?RelayState=/kirjaudu`

export const UserHeader = React.memo(function UserHeader() {
  const { user } = useContext(UserContext)

  if (!user) return null

  return (
    <FlexRowWithGaps>
      <span>
        {user.firstName} {user.lastName}
      </span>
      <a href={logoutUrl}>Kirjaudu ulos</a>
    </FlexRowWithGaps>
  )
})
