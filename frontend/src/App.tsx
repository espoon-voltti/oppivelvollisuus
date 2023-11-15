import React from 'react'
import { Navigate, createBrowserRouter, Outlet, Link } from 'react-router-dom'
import styled from 'styled-components'

import { CreateStudentPage } from './students/CreateStudentPage'
import { StudentCasesSearchPage } from './students/StudentCasesSearchPage'
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
  height: 64px;
  display: flex;
  flex-direction: row;
  justify-content: flex-start;
  align-items: center;
  border-bottom: 2px double #888;
  margin-bottom: 16px;
  > * {
    margin-right: 32px;
  }
`

function App() {
  return (
    <AppContainer>
      <Header>
        <Link to="/tapaukset">Tapaukset</Link>
        <Link to="/oppivelvolliset">Oppivelvolliset</Link>
      </Header>
      <Outlet />
    </AppContainer>
  )
}

export const appRouter = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      {
        path: '/oppivelvolliset',
        element: <StudentsSearchPage />
      },
      {
        path: '/tapaukset',
        element: <StudentCasesSearchPage />
      },
      {
        path: '/oppivelvolliset/uusi',
        element: <CreateStudentPage />
      },
      {
        path: '/oppivelvolliset/:id',
        element: <StudentPage />
      },
      {
        path: '/*',
        element: <Navigate replace to="/tapaukset" />
      },
      {
        index: true,
        element: <Navigate replace to="/tapaukset" />
      }
    ]
  }
])
