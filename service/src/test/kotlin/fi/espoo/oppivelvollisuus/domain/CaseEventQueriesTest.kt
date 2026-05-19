// SPDX-FileCopyrightText: 2026-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.PureJdbiTest
import fi.espoo.oppivelvollisuus.shared.dev.DevCaseEvent
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

class CaseEventQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0))
    private val user = DevUser()
    private val student = DevStudent(createdBy = user.id, created = now)
    private val case = DevStudentCase(studentId = student.id, createdBy = user.id, created = now)

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(user)
            tx.insert(student)
            tx.insert(case)
        }
    }

    private fun caseEvents() = db.read { it.getStudentCasesByStudent(student.id) }.first().events

    @Test
    fun `insertCaseEvent persists event and returns id`() {
        val id = db.transaction {
            it.insertCaseEvent(
                studentCaseId = case.id,
                data =
                    CaseEventInput(
                        date = LocalDate.of(2026, 2, 3),
                        type = CaseEventType.HEARING_LETTER,
                        notes = "hello",
                    ),
                createdBy = user.id,
                now = now,
            )
        }

        val events = caseEvents()
        assertEquals(listOf(id), events.map { it.id })
        assertEquals(LocalDate.of(2026, 2, 3), events.first().date)
        assertEquals(CaseEventType.HEARING_LETTER, events.first().type)
        assertEquals("hello", events.first().notes)
    }

    @Test
    fun `updateCaseEvent overwrites the fields`() {
        val event =
            DevCaseEvent(
                studentCaseId = case.id,
                createdBy = user.id,
                created = now,
                type = CaseEventType.NOTE,
                notes = "before",
                date = LocalDate.of(2026, 1, 1),
            )
        db.transaction { tx -> tx.insert(event) }

        db.transaction {
            it.updateCaseEvent(
                id = event.id,
                data =
                    CaseEventInput(
                        date = LocalDate.of(2026, 2, 2),
                        type = CaseEventType.MEETING,
                        notes = "after",
                    ),
                updatedBy = user.id,
                now = now,
            )
        }

        val persisted = caseEvents().first()
        assertEquals(LocalDate.of(2026, 2, 2), persisted.date)
        assertEquals(CaseEventType.MEETING, persisted.type)
        assertEquals("after", persisted.notes)
    }

    @Test
    fun `deleteCaseEvent removes the row`() {
        val event = DevCaseEvent(studentCaseId = case.id, createdBy = user.id, created = now)
        db.transaction { tx -> tx.insert(event) }
        assertEquals(1, caseEvents().size)

        db.transaction { it.deleteCaseEvent(event.id) }

        assertEquals(0, caseEvents().size)
    }
}
