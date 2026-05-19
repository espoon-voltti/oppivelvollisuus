// SPDX-FileCopyrightText: 2026-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.PureJdbiTest
import fi.espoo.oppivelvollisuus.shared.dev.DevStudent
import fi.espoo.oppivelvollisuus.shared.dev.DevStudentCase
import fi.espoo.oppivelvollisuus.shared.dev.DevUser
import fi.espoo.oppivelvollisuus.shared.dev.insert
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReportQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0))

    private val user = DevUser()

    @BeforeEach
    fun setup() {
        db.transaction { tx -> tx.insert(user) }
    }

    private fun seedCaseOpenedAt(openedAt: LocalDate): LocalDate {
        val student = DevStudent(createdBy = user.id, created = now)
        db.transaction { tx ->
            tx.insert(student)
            tx.insert(
                DevStudentCase(
                    studentId = student.id,
                    createdBy = user.id,
                    created = now,
                    openedAt = openedAt,
                )
            )
        }
        return openedAt
    }

    @Test
    fun `start-only filter excludes cases opened before start`() {
        seedCaseOpenedAt(LocalDate.of(2022, 1, 1))
        val included1 = seedCaseOpenedAt(LocalDate.of(2022, 6, 1))
        val included2 = seedCaseOpenedAt(LocalDate.of(2023, 1, 1))

        val result = db.read {
            it.getCasesReport(CaseReportRequest(start = LocalDate.of(2022, 6, 1), end = null))
        }

        assertEquals(setOf(included1, included2), result.map { it.openedAt }.toSet())
    }

    @Test
    fun `end-only filter excludes cases opened after end`() {
        val included1 = seedCaseOpenedAt(LocalDate.of(2022, 1, 1))
        val included2 = seedCaseOpenedAt(LocalDate.of(2022, 6, 1))
        seedCaseOpenedAt(LocalDate.of(2023, 1, 1))

        val result = db.read {
            it.getCasesReport(CaseReportRequest(start = null, end = LocalDate.of(2022, 6, 1)))
        }

        assertEquals(setOf(included1, included2), result.map { it.openedAt }.toSet())
    }
}
