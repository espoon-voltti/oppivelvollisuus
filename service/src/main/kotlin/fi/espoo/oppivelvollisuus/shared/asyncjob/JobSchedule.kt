// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.asyncjob

import com.github.kagkarlsson.scheduler.task.schedule.CronSchedule
import com.github.kagkarlsson.scheduler.task.schedule.Daily
import com.github.kagkarlsson.scheduler.task.schedule.Schedule
import fi.espoo.oppivelvollisuus.shared.db.Database
import fi.espoo.oppivelvollisuus.shared.time.AppClock
import fi.espoo.oppivelvollisuus.shared.time.europeHelsinki
import java.time.LocalTime

interface JobSchedule {
    val jobs: List<ScheduledJobDefinition>

    companion object {
        fun daily(at: LocalTime): Schedule = Daily(europeHelsinki, at)

        fun cron(expression: String): Schedule = CronSchedule(expression, europeHelsinki)
    }
}

data class ScheduledJobDefinition(
    val job: Enum<*>,
    val settings: ScheduledJobSettings,
    val jobFn: (db: Database.Connection, clock: AppClock) -> Unit,
)

data class ScheduledJobSettings(
    val enabled: Boolean,
    val schedule: Schedule,
    val retryCount: Int? = null,
)
