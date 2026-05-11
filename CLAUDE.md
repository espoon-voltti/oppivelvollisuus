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

Before running, make sure the dev stack is up (`mise start` if not). The e2e test starts its own Spring Boot service instance, so the pm2 `frontend` and `api-gateway` apps must be running but `service` must be stopped:

```
mise exec -- pm2 stop service
```

```
cd service
xvfb-run -a ./gradlew e2eTest
```

On macOS with a display, `./gradlew e2eTest` works without xvfb.

`xvfb` and the Playwright browser system packages (`./gradlew e2eTestDeps`) are pre-installed by the sandbox `.sandbox/post-create.sh`, so no extra setup is needed in a fresh sandbox.## Unit / integration tests

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
