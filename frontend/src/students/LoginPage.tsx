import React, { useContext } from 'react'
import styled from 'styled-components'

import { UserContext } from '../auth/UserContext'
import { FlexColWithGaps } from '../shared/layout'
import { H2 } from '../shared/typography'

const Wrapper = styled.div`
  width: 100%;
  height: 600px;
  display: flex;
  align-items: center;
  justify-content: center;
`

export const LoginPage = React.memo(function LoginPage() {
  const { setLoggedIn } = useContext(UserContext)

  return (
    <Wrapper>
      <FlexColWithGaps $gapSize="L">
        <H2>Kirjaudu sisään Espoo-AD:lla</H2>
        <button onClick={() => setLoggedIn({ name: 'Tessa Testaaja' })}>
          Kirjaudu sisään
        </button>
      </FlexColWithGaps>
    </Wrapper>
  )
})
