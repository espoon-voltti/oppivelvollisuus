import { ErrorRequestHandler } from 'express'
import { csrfCookieName } from './config.js'

export const errorHandler: ErrorRequestHandler = (error, req, res, _next) => {
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

  res.status(error.response?.status ?? 500).json(null)
}

export const fallbackErrorHandler: ErrorRequestHandler = (
  error,
  req,
  res,
  _next
) => {
  console.error(
    `Internal server error: ${error.message || error || 'No error object'}`,
    req,
    undefined,
    error
  )
  if (!res.headersSent) {
    res.status(500).json({ message: 'Internal server error' })
  }
}
