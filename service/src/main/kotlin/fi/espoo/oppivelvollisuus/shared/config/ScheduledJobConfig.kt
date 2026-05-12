// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.config

import fi.espoo.oppivelvollisuus.shared.asyncjob.AsyncJob
import fi.espoo.oppivelvollisuus.shared.asyncjob.AsyncJobRunner
import fi.espoo.oppivelvollisuus.shared.asyncjob.JobSchedule
import fi.espoo.oppivelvollisuus.shared.asyncjob.ScheduledJobRunner
import io.opentelemetry.api.trace.Tracer
import org.jdbi.v3.core.Jdbi
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import javax.sql.DataSource

@Configuration
class ScheduledJobConfig {
    @Bean
    fun scheduledJobRunner(
        jdbi: Jdbi,
        tracer: Tracer,
        asyncJobRunner: AsyncJobRunner<AsyncJob>,
        dataSource: DataSource,
        schedules: List<JobSchedule>,
    ) = ScheduledJobRunner(jdbi, tracer, asyncJobRunner, schedules, dataSource)

    @Bean
    @Profile("production")
    fun scheduledJobRunnerStart(runner: ScheduledJobRunner) =
        object {
            @EventListener
            fun onApplicationReady(
                @Suppress("UNUSED_PARAMETER") event: ApplicationReadyEvent
            ) {
                runner.scheduler.start()
            }
        }
}
