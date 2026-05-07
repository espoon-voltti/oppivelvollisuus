<!--
SPDX-FileCopyrightText: 2023-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# oppivelvollisuus

Monorepo: `api-gateway` (Node/Express), `frontend` (React + esbuild), `service` (Kotlin/Spring Boot).

## Dev environment

Tooling is managed by `mise`. Install tools with `mise install`.

Start the local stack (docker-compose + pm2 via mise tasks):

```
mise start      # starts postgres, redis + pm2 apps
mise stop
mise restart [all|frontend|api-gateway|service]
```

Frontend: http://localhost:9000.
API gateway: http://localhost:3000.
Service: http://localhost:8080.

## E2E tests (service)

The e2e test starts its own Spring Boot service instance, so stop the pm2 `service` app first if it's running on the same port:

```
mise exec -- pm2 stop service
```

The test talks to the already-running frontend + api-gateway.

```
cd service
./gradlew e2eTestDeps     # one-time: installs Playwright browser system deps
xvfb-run -a ./gradlew e2eTest
```

On macOS with a display, `./gradlew e2eTest` works without xvfb.

## Unit / integration tests

```
cd service && ./gradlew test
```

api-gateway and frontend have no test suites.

## Lint / format / type-check

Run in every changed project before committing.

**frontend** (`cd frontend`):
```
yarn lint --fix
yarn type-check
```

**api-gateway** (`cd api-gateway`):
```
yarn lint --fix
```

**service**:
```
./gradlew ktlintFormat compileKotlin compileTestKotlin compileE2eTestKotlin
```
