import sourceMapSupport from 'source-map-support'
import express from 'express'
import helmet from 'helmet'
import { httpPort } from './config.js'
import { fallbackErrorHandler } from './errors.js'
import { createRouter } from './router.js'

sourceMapSupport.install()

const app = express()
app.set('etag', false)

app.use(helmet())
app.get('/health', (_, res) => {
  res.status(200).json({ status: 'UP' })
})

app.use('/api', createRouter())
app.use(fallbackErrorHandler)

const server = app.listen(httpPort, () => {
  console.log(`Oppivelvollisuus API Gateway listening on port ${httpPort}`)
})

server.keepAliveTimeout = 70 * 1000
server.headersTimeout = 75 * 1000
