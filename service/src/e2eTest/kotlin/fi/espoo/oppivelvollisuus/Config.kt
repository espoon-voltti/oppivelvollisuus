package fi.espoo.oppivelvollisuus

val runningInDocker = System.getenv("E2E_ENV") == "docker"
val baseUrl = if (runningInDocker) "http://frontend" else "http://localhost:9000"

const val E2E_DEBUG_LOGGING = false
