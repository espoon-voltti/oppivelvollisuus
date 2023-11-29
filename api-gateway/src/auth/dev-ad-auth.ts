import _ from 'lodash'
import { Request, Router, urlencoded } from 'express'
import {
  assertStringProp,
  AsyncRequestHandler,
  toRequestHandler
} from '../express.js'
import { employeeLogin, EmployeeUser } from '../service-client.js'
import { Sessions } from '../session.js'
import passport, { Strategy } from 'passport'
import { AppSessionUser, authenticate, login, logout } from './index.js'
import { parseRelayState } from '../saml/index.js'
import { appBaseUrl } from '../config.js'

class DevStrategy extends Strategy {
  constructor(private verifyUser: (req: Request) => Promise<AppSessionUser>) {
    super()
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  authenticate(req: Request, _options?: any): any {
    this.verifyUser(req)
      .then((user) => this.success(user))
      .catch((err) => this.error(err))
  }
}

const devEmployees: EmployeeUser[] = [
  {
    externalId: '12345678-0000-0000-0000-000000000000',
    firstName: 'Sanna',
    lastName: 'Suunnittelija'
  }
]

const loginFormHandler: AsyncRequestHandler = async (req, res) => {
  const employeeInputs = devEmployees.map((employee, idx) => {
    const { externalId, firstName, lastName } = employee
    const json = JSON.stringify(employee)
    return `<div>
            <input
              type="radio"
              id="${externalId}"
              name="preset"
              ${idx == 0 ? 'checked' : ''}
              value="${_.escape(json)}" />
            <label for="${externalId}">${firstName} ${lastName}</label>
          </div>`
  })

  const formQuery =
    typeof req.query.RelayState === 'string'
      ? `?RelayState=${encodeURIComponent(req.query.RelayState)}`
      : ''
  const formUri = `${req.baseUrl}/login/callback${formQuery}`

  res.contentType('text/html').send(`
          <html lang='fi'>
          <body>
            <h1>Devausympäristön AD-kirjautuminen</h1>
            <form action="${formUri}" method="post">
                ${employeeInputs.join('\n')}
                <div style="margin-top: 20px">
                  <button type="submit">Kirjaudu</button>
                </div>
            </form>
          </body>
          </html>
        `)
}

const verifyUser = async (req: Request): Promise<AppSessionUser> => {
  const preset = assertStringProp(req.body, 'preset')
  const person = await employeeLogin(JSON.parse(preset))
  return {
    id: person.externalId
  }
}

export function createDevAdRouter(sessions: Sessions): Router {
  const strategyName = 'dev-ad'
  passport.use(strategyName, new DevStrategy(verifyUser))

  const router = Router()

  router.get('/login', toRequestHandler(loginFormHandler))
  router.post(
    `/login/callback`,
    urlencoded({ extended: false }), // needed to parse the POSTed form
    toRequestHandler(async (req, res) => {
      try {
        const user = await authenticate(strategyName, req, res)
        if (!user) {
          res.redirect(`${appBaseUrl}?loginError=true`)
        } else {
          await login(req, user)
          res.redirect(parseRelayState(req) ?? appBaseUrl)
        }
      } catch (err) {
        if (!res.headersSent) {
          res.redirect(`${appBaseUrl}?loginError=true`)
        }
        throw err
      }
    })
  )

  router.get(
    `/logout`,
    toRequestHandler(async (req, res) => {
      await logout(sessions, req, res)
      res.redirect('/kirjaudu')
    })
  )

  return router
}