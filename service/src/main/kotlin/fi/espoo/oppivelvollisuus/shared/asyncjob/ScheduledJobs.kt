// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.asyncjob

import fi.espoo.oppivelvollisuus.ScheduledJobsEnv
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.time.AppClock
import fi.espoo.oppivelvollisuus.valpas.ValpasIntegrationService
import org.springframework.stereotype.Component

enum class ScheduledJob(
    val fn: (ScheduledJobs, Database.Connection, AppClock) -> Unit,
    val defaultSettings: ScheduledJobSettings,
) {
    StartValpasImport(
        ScheduledJobs::startValpasImport,
        ScheduledJobSettings(
            enabled = true,
            schedule = JobSchedule.cron("0 0 3 * * ?"),
            retryCount = 1,
        ),
    ),
    AdvanceValpasImport(
        ScheduledJobs::advanceValpasImport,
        ScheduledJobSettings(
            enabled = true,
            schedule = JobSchedule.cron("0 */5 * * * ?"),
            retryCount = 1,
        ),
    ),
}

@Component
class ScheduledJobs(
    env: ScheduledJobsEnv<ScheduledJob>,
    private val valpasIntegrationService: ValpasIntegrationService,
) : JobSchedule {
    override val jobs: List<ScheduledJobDefinition> =
        env.jobs.map {
            ScheduledJobDefinition(it.key, it.value) { db, clock -> it.key.fn(this, db, clock) }
        }

    fun startValpasImport(db: Database.Connection, clock: AppClock) = db.transaction { tx ->
        valpasIntegrationService.scheduleStartValpasImport(tx, clock)
    }

    fun advanceValpasImport(db: Database.Connection, clock: AppClock) =
        valpasIntegrationService.advanceValpasImport(db, clock)
}
