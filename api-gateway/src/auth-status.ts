import express from 'express'
import { toRequestHandler } from './express.js'
import { EmployeeUser, getEmployeeDetails } from './service-client.js'
import { Sessions } from './session.js'
import { appCommit } from './config.js'
import { logout } from './auth/index.js'

interface AuthStatus {
  loggedIn: boolean
  user?: EmployeeUser
  apiVersion: string
}

async function validateUser(
  req: express.Request
): Promise<EmployeeUser | undefined> {
  const user = req.user
  if (!user || !user.id) return undefined
  return getEmployeeDetails(req, user.id)
}

export default (sessions: Sessions) =>
  toRequestHandler(async (req, res) => {
    const sessionUser = req.user
    const validUser = sessionUser && (await validateUser(req))

    let status: AuthStatus
    if (validUser) {
      status = {
        loggedIn: true,
        user: validUser,
        apiVersion: appCommit
      }
    } else {
      if (sessionUser) {
        await logout(sessions, req, res)
      }
      status = {
        loggedIn: false,
        apiVersion: appCommit
      }
    }

    res.status(200).json(status)
  })
