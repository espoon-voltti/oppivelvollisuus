import React, { useContext } from 'react'

import { UserContext } from '../auth/UserContext'

export const LoginPage = React.memo(function LoginPage() {
  const { setLoggedIn } = useContext(UserContext)

  return (
    <div>
      <button onClick={() => setLoggedIn({ name: 'Tessa Testaaja' })}>
        Kirjaudu sisään
      </button>
    </div>
  )
})
