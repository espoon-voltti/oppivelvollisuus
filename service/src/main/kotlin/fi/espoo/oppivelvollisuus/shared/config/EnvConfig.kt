// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.config

import fi.espoo.oppivelvollisuus.AppEnv
import fi.espoo.oppivelvollisuus.DatabaseEnv
import fi.espoo.oppivelvollisuus.JwtEnv
import fi.espoo.oppivelvollisuus.ScheduledJobsEnv
import fi.espoo.oppivelvollisuus.shared.asyncjob.ScheduledJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.env.Environment

@Configuration
@Lazy
class EnvConfig {
    @Bean fun oppivelvollisuusEnv(env: Environment) = AppEnv.fromEnvironment(env)

    @Bean fun databaseEnv(env: Environment): DatabaseEnv = DatabaseEnv.fromEnvironment(env)

    @Bean fun jwtEnv(env: Environment): JwtEnv = JwtEnv.fromEnvironment(env)

    @Bean
    fun scheduledJobsEnvEnv(env: Environment): ScheduledJobsEnv<ScheduledJob> =
        ScheduledJobsEnv.fromEnvironment(
            ScheduledJob.entries.associateWith { it.defaultSettings },
            "app.job",
            env,
        )
}
