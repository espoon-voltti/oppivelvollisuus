import sourceMapSupport from 'source-map-support'
import express from 'express'
import helmet from 'helmet'
import * as redis from 'redis'
import { configFromEnv, httpPort, toRedisClientOpts } from './config.js'
import { fallbackErrorHandler } from './errors.js'
import { createRouter } from './router.js'
import { logError, loggingMiddleware } from './logging.js'
import { trustReverseProxy } from './reverse-proxy.js'
import { assertRedisConnection } from './redis-client.js'
import passport from 'passport'

sourceMapSupport.install()
const config = configFromEnv()

const redisClient = redis.createClient(toRedisClientOpts(config.redis))
redisClient.on('error', (err) =>
  logError('Redis error', undefined, undefined, err)
)
redisClient.connect().catch((err) => {
  logError('Unable to connect to redis', undefined, undefined, err)
})
// Don't prevent the app from exiting if a redis connection is alive.
redisClient.unref()

const app = express()
trustReverseProxy(app)
app.set('etag', false)

app.use(
  helmet({
    // Content-Security-Policy is set by the nginx proxy
    contentSecurityPolicy: false
  })
)
app.get('/health', (_, res) => {
  assertRedisConnection(redisClient)
    .then(() => {
      res.status(200).json({ status: 'UP' })
    })
    .catch(() => {
      res.status(503).json({ status: 'DOWN' })
    })
})
app.use(loggingMiddleware)

passport.serializeUser<Express.User>((user, done) => done(null, user))
passport.deserializeUser<Express.User>((user, done) => done(null, user))

app.use('/api', createRouter(config, redisClient))
app.use(fallbackErrorHandler)

const server = app.listen(httpPort, () => {
  console.log(`Oppivelvollisuus API Gateway listening on port ${httpPort}`)
})

server.keepAliveTimeout = 70 * 1000
server.headersTimeout = 75 * 1000
