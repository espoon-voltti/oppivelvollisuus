import jwt from 'jsonwebtoken'
import { readFileSync } from 'node:fs'
import { jwtKid, jwtPrivateKey } from '../config.js'

const privateKey = readFileSync(jwtPrivateKey)

export function createJwt(payload: { sub: string }): string {
  return jwt.sign(payload, privateKey, {
    algorithm: 'RS256',
    expiresIn: '48h',
    keyid: jwtKid
  })
}
