// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.asyncjob

import fi.espoo.oppivelvollisuus.ScheduledJobsEnv
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.time.AppClock
import org.springframework.stereotype.Component

enum class ScheduledJob(
    val fn: (ScheduledJobs, Database.Connection, AppClock) -> Unit,
    val defaultSettings: ScheduledJobSettings,
)

@Component
class ScheduledJobs(
    env: ScheduledJobsEnv<ScheduledJob>
) : JobSchedule {
    override val jobs: List<ScheduledJobDefinition> =
        env.jobs.map {
            ScheduledJobDefinition(it.key, it.value) { db, clock -> it.key.fn(this, db, clock) }
        }
}
