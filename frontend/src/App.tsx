import React from 'react'
import { Navigate, createBrowserRouter, Outlet } from 'react-router-dom'

import { HelloWorldPage } from './hello/HelloWorldPage'

function App() {
  return <Outlet />
}

export const appRouter = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      {
        path: '/children',
        element: <HelloWorldPage />
      },
      {
        path: '/*',
        element: <Navigate replace to="/children" />
      },
      {
        index: true,
        element: <Navigate replace to="/children" />
      }
    ]
  }
])
