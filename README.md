# Oppivelvollisuus

A tool for tracking the monitoring and guidance cases related to compulsory education.

## Local environment and development

### Requirements

- Docker or similar
- Docker-Compose
- Node.js (recommended setup using NVM)
- yarn
- Java JDK (17+)

### Compose

To start database and redis
- `cd compose`
- `docker-compose up -d`

### Service

To start service in http://localhost:8080
- `cd service`
- `./gradlew bootRun`

To run unit/integration tests (requires DB running through compose)
- `./gradlew test`

To run E2E tests (requires DB, api-gateway and frontend running)
- `./gradlew e2eTest`

To format code
- `./gradlew ktlintFormat`

### API-gateway

To start API-gateway in http://localhost:3000
- `cd api-gateway`
- `yarn`
- `yarn dev`

To lint and format code
- `yarn lint --fix`

### Frontend

To start frontend in http://localhost:9000
- `cd frontend`
- `yarn`
- `yarn dev`

To lint and format code
- `yarn lint --fix`
