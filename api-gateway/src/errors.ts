import { ErrorRequestHandler } from 'express'
import { csrfCookieName } from './config.js'
import { logError } from './logging.js'

export const errorHandler: ErrorRequestHandler = (error, req, res, next) => {
  // https://github.com/expressjs/csurf#custom-error-handling
  if (error.code === 'EBADCSRFTOKEN') {
    console.warn(
      'CSRF token error',
      req,
      {
        xsrfCookie: req.cookies[csrfCookieName],
        xsrfHeader: req.header('x-xsrf-token')
      },
      error
    )
    if (!res.headersSent) {
      res.status(403).send({ message: 'CSRF token error' })
    }
    return
  }

  return fallbackErrorHandler(error, req, res, next)
}

export const fallbackErrorHandler: ErrorRequestHandler = (
  error,
  req,
  res,
  _next
) => {
  logError(
    `Internal server error: ${error.message || error || 'No error object'}`,
    req,
    undefined,
    error
  )
  if (!res.headersSent) {
    res.status(500).json({ message: 'Internal server error' })
  }
}
