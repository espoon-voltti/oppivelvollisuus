import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const names = [
  'espooad-internal-prod.2022.pem',
  'espooad-internal-staging.2022.pem',
  'idp.test.espoon-voltti.fi.pem'
] as const

export type TrustedCertificates = (typeof names)[number]

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const certificates: Record<TrustedCertificates, string> = {} as any
for (const name of names) {
  certificates[name] = fs.readFileSync(
    path.resolve(__dirname, '../../../config/certificates', name),
    'utf-8'
  )
}

export default certificates
