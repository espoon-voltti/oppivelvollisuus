// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.config

import fi.espoo.oppivelvollisuus.AppEnv
import fi.espoo.oppivelvollisuus.shared.asyncjob.AsyncJob
import fi.espoo.oppivelvollisuus.shared.asyncjob.AsyncJobRunner
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Tracer
import java.time.Duration
import org.jdbi.v3.core.Jdbi
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private fun emailThrottleInterval(maxEmailsPerSecondRate: Int) =
    Duration.ofSeconds(1).dividedBy(maxEmailsPerSecondRate.toLong())

@Configuration
class AsyncJobConfig {
    @Bean
    fun asyncJobRunner(jdbi: Jdbi, tracer: Tracer): AsyncJobRunner<AsyncJob> =
        AsyncJobRunner(
            AsyncJob::class,
            listOf(
                AsyncJob.main,
                // this is a reasonable default but should probably be configurable
                AsyncJob.email.withThrottleInterval(
                    emailThrottleInterval(maxEmailsPerSecondRate = 14)
                ),
            ),
            jdbi,
            tracer,
        )

    @Bean
    fun asyncJobRunnerStarter(
        asyncJobRunners: List<AsyncJobRunner<*>>,
        appEnv: AppEnv,
        meterRegistry: MeterRegistry,
    ) =
        ApplicationListener<ApplicationReadyEvent> {
            val logger = KotlinLogging.logger {}
            if (appEnv.asyncJobRunnerDisabled) {
                logger.info { "Async job runners disabled" }
            } else {
                asyncJobRunners.forEach {
                    it.registerMeters(meterRegistry)
                    it.enableAfterCommitHooks()
                    it.startBackgroundPolling()
                    logger.info { "Async job runner ${it.name} started" }
                }
            }
        }
}
