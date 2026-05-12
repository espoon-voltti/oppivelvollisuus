// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import com.fasterxml.jackson.annotation.JsonValue
import fi.espoo.oppivelvollisuus.shared.asyncjob.JobSchedule
import fi.espoo.oppivelvollisuus.shared.asyncjob.ScheduledJobSettings
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.env.Environment
import java.net.URI
import java.time.Duration
import java.util.Locale

/**
 * A type-safe configuration parsed from environment variables / other property sources supported by
 * Spring Boot by default
 */
data class AppEnv(
    val frontendBaseUrlFi: String?,
    val mockClock: Boolean,
    val asyncJobRunnerDisabled: Boolean,
    val skipAttachmentRequirements: Boolean,
) {
    companion object {
        fun fromEnvironment(env: Environment): AppEnv =
            AppEnv(
                frontendBaseUrlFi = env.lookup("app.frontend.base_url.fi"),
                mockClock = env.lookup("app.clock.mock") ?: false,
                asyncJobRunnerDisabled = env.lookup("app.async_job_runner.disable_runner") ?: false,
                skipAttachmentRequirements = env.lookup("app.skip_attachment_requirements") ?: false,
            )
    }
}

data class JwtEnv(
    val publicKeysUrl: URI,
) {
    companion object {
        fun fromEnvironment(env: Environment) = JwtEnv(publicKeysUrl = env.lookup("app.jwt.public_keys_url"))
    }
}

data class DatabaseEnv(
    val url: String,
    val username: String,
    val password: Sensitive<String>,
    val flywayUsername: String,
    val flywayPassword: Sensitive<String>,
    val flywayLocations: List<String>,
    val flywayIgnoreFutureMigrations: Boolean,
    val leakDetectionThreshold: Long,
    val defaultStatementTimeout: Duration,
    val maximumPoolSize: Int,
    val logSql: Boolean,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            DatabaseEnv(
                url = env.lookup("app.database.url"),
                username = env.lookup("app.database.username"),
                password = Sensitive(env.lookup("app.database.password")),
                flywayUsername = env.lookup("app.database.flyway.username"),
                flywayPassword = Sensitive(env.lookup("app.database.flyway.password")),
                flywayLocations =
                    env.lookup("app.database.flyway.locations") ?: listOf("db/migration"),
                flywayIgnoreFutureMigrations =
                    env.lookup("app.database.flyway.ignore-future-migrations") ?: true,
                leakDetectionThreshold = env.lookup("app.database.leak_detection_threshold") ?: 0,
                defaultStatementTimeout =
                    env.lookup("app.database.default_statement_timeout") ?: Duration.ofSeconds(60),
                maximumPoolSize = env.lookup("app.database.maximum_pool_size") ?: 10,
                logSql = env.lookup("app.database.log_sql") ?: false,
            )

        fun testConfigFromSystemEnv() =
            DatabaseEnv(
                url =
                    System.getenv("APP_DATABASE_URL")
                        ?: "jdbc:postgresql://localhost:5432/oppivelvollisuus_it",
                username = System.getenv("APP_DATABASE_USERNAME") ?: "oppivelvollisuus",
                password = Sensitive(System.getenv("APP_DATABASE_PASSWORD") ?: "postgres"),
                flywayUsername = System.getenv("APP_DATABASE_FLYWAY_USERNAME") ?: "oppivelvollisuus",
                flywayPassword =
                    Sensitive(System.getenv("APP_DATABASE_FLYWAY_PASSWORD") ?: "postgres"),
                flywayLocations = listOf("db/migration"),
                flywayIgnoreFutureMigrations = true,
                leakDetectionThreshold = 0,
                defaultStatementTimeout = Duration.ofSeconds(10),
                maximumPoolSize = 10,
                logSql = false,
            )
    }
}

data class ScheduledJobsEnv<T : Enum<T>>(
    val jobs: Map<T, ScheduledJobSettings>,
) {
    companion object {
        fun <T : Enum<T>> fromEnvironment(
            defaults: Map<T, ScheduledJobSettings>,
            prefix: String,
            env: Environment,
        ) = ScheduledJobsEnv(
            defaults.mapValues { (job, default) ->
                val envPrefix = "$prefix.${snakeCaseName(job)}"
                logger.info { "Creating ScheduledJobSettings for $envPrefix" }
                ScheduledJobSettings(
                    enabled = env.lookup("$envPrefix.enabled") ?: default.enabled,
                    schedule =
                        env.lookup<String?>("$envPrefix.cron")?.let(JobSchedule::cron)
                            ?: default.schedule,
                    retryCount = env.lookup("$envPrefix.retry_count") ?: default.retryCount,
                )
            }
        )
    }
}

data class Sensitive<T>(
    @JsonValue val value: T,
) {
    override fun toString(): String = "**REDACTED**"
}

inline fun <reified T> Environment.lookup(
    key: String,
    vararg deprecatedKeys: String,
): T {
    val value = lookup(key, deprecatedKeys, T::class.java)
    if (value == null && null !is T) {
        error("Missing required configuration: $key (environment variable ${key.toSystemEnvKey()})")
    } else {
        return value as T
    }
}

private val logger = KotlinLogging.logger {}

fun <T> Environment.lookup(
    key: String,
    deprecatedKeys: Array<out String>,
    clazz: Class<out T>,
): T? =
    deprecatedKeys
        .asSequence()
        .mapNotNull { legacyKey ->
            try {
                getProperty(legacyKey, clazz)?.also {
                    logger.warn {
                        "Using deprecated configuration key $legacyKey instead of $key (environment variable ${key.toSystemEnvKey()})"
                    }
                }
            } catch (e: Exception) {
                throw EnvLookupException(legacyKey, e)
            }
        }.firstOrNull()
        ?: try {
            getProperty(key, clazz)
        } catch (e: Exception) {
            throw EnvLookupException(key, e)
        }

class EnvLookupException(
    key: String,
    cause: Throwable,
) : RuntimeException(
        "Failed to lookup configuration key $key (environment variable ${key.toSystemEnvKey()})",
        cause,
    )

// Reference: Spring SystemEnvironmentPropertySource
fun String.toSystemEnvKey() = uppercase(Locale.ENGLISH).replace('.', '_').replace('-', '_')

private fun snakeCaseName(job: Enum<*>): String =
    job.name
        .flatMapIndexed { idx, ch ->
            when {
                idx == 0 -> listOf(ch.lowercaseChar())
                ch.isUpperCase() -> listOf('_', ch.lowercaseChar())
                else -> listOf(ch)
            }
        }.joinToString(separator = "")
