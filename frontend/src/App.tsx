import React from 'react'
import { Navigate, createBrowserRouter, Outlet } from 'react-router-dom'
import styled from 'styled-components'

import { AuthGuard } from './auth/AuthGuard'
import { UserContextProvider } from './auth/UserContext'
import { UserHeader } from './auth/UserHeader'
import { useAuthStatus } from './auth/auth-status'
import { H1 } from './shared/typography'
import { CreateStudentPage } from './students/CreateStudentPage'
import { LoginPage } from './students/LoginPage'
import { StudentPage } from './students/StudentPage'
import { StudentsSearchPage } from './students/StudentsSearchPage'

const AppContainer = styled.div`
  padding: 0 16px;
  max-width: 1024px;
  margin: 0 auto;
  background-color: #fff;
  min-height: 600px;
`

const Header = styled.nav`
  height: 80px;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  border-bottom: 2px double #888;
  margin-bottom: 32px;
  padding: 0 32px;
  background-color: #fff;
`

function App() {
  const authStatus = useAuthStatus()
  if (!authStatus) return null

  const user = authStatus.loggedIn && authStatus.user ? authStatus.user : null

  return (
    <UserContextProvider user={user}>
      <div>
        <Header>
          <H1>Espoon kaupunki - Oppivelvollisuuden seuranta</H1>
          <UserHeader />
        </Header>
        <AppContainer>
          <Outlet />
        </AppContainer>
      </div>
    </UserContextProvider>
  )
}

export const appRouter = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      {
        path: '/kirjaudu',
        element: (
          <AuthGuard allow="UNAUTHENTICATED_ONLY">
            <LoginPage />
          </AuthGuard>
        )
      },
      {
        path: '/oppivelvolliset',
        element: (
          <AuthGuard allow="AUTHENTICATED_ONLY">
            <StudentsSearchPage />
          </AuthGuard>
        )
      },
      {
        path: '/oppivelvolliset/uusi',
        element: (
          <AuthGuard allow="AUTHENTICATED_ONLY">
            <CreateStudentPage />
          </AuthGuard>
        )
      },
      {
        path: '/oppivelvolliset/:id',
        element: (
          <AuthGuard allow="AUTHENTICATED_ONLY">
            <StudentPage />
          </AuthGuard>
        )
      },
      {
        path: '/*',
        element: <Navigate replace to="/oppivelvolliset" />
      },
      {
        index: true,
        element: <Navigate replace to="/oppivelvolliset" />
      }
    ]
  }
])
