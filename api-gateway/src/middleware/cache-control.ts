import express from 'express'
import nocache from 'nocache'

export const cacheControl = (
  allowCaching: (req: express.Request) => 'allow-cache' | 'forbid-cache'
): express.RequestHandler => {
  const forbidCaching = nocache()
  return (req, res, next) => {
    return allowCaching(req) === 'allow-cache'
      ? next()
      : forbidCaching(req, res, next)
  }
}
