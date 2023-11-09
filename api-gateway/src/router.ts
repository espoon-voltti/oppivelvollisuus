import { Router } from 'express'
import expressHttpProxy from 'express-http-proxy'

import { errorHandler } from './errors.js'
import { serviceUrl } from './config.js'

export function createRouter(): Router {
  const router = Router()

  router.use(
    expressHttpProxy(serviceUrl, {
      parseReqBody: false
    })
  )
  router.use(errorHandler)

  return router
}
