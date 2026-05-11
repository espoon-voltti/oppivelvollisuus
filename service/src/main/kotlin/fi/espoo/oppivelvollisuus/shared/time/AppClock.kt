// SPDX-FileCopyrightText: 2025-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.shared.time

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

interface AppClock {
    fun today(): LocalDate

    fun now(): HelsinkiDateTime
}

class MockAppClock(private var now: HelsinkiDateTime) : AppClock {
    constructor(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
    ) : this(
        HelsinkiDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute, second))
    )

    override fun today(): LocalDate = now.toLocalDate()

    override fun now(): HelsinkiDateTime = now

    fun tick(duration: Duration = Duration.ofSeconds(1)) {
        now += duration
    }
}

class RealAppClock(private val clock: Clock = Clock.systemUTC()) : AppClock {
    override fun today(): LocalDate = LocalDate.now(europeHelsinki)

    override fun now(): HelsinkiDateTime = HelsinkiDateTime.now(clock)
}
