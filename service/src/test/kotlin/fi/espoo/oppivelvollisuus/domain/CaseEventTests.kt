// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.CaseEventId
import fi.espoo.oppivelvollisuus.FullApplicationTest
import fi.espoo.oppivelvollisuus.StudentCaseId
import fi.espoo.oppivelvollisuus.shared.dev.DevStudent
import fi.espoo.oppivelvollisuus.shared.dev.DevStudentCase
import fi.espoo.oppivelvollisuus.shared.dev.DevUser
import fi.espoo.oppivelvollisuus.shared.dev.insert
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import fi.espoo.oppivelvollisuus.shared.time.MockAppClock
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CaseEventTests : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: AppController

    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0))
    private val clock = MockAppClock(now)

    private val testUser = DevUser()
    private val testStudent = DevStudent(createdBy = testUser.id, created = now)
    private val testCase =
        DevStudentCase(studentId = testStudent.id, createdBy = testUser.id, created = now)

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(testUser)
            tx.insert(testStudent)
            tx.insert(testCase)
        }
    }

    private fun createCaseEvent(caseId: StudentCaseId, body: CaseEventInput): CaseEventId =
        controller.createCaseEvent(dbInstance(), testUser.user, clock, caseId, body)

    private fun updateCaseEvent(eventId: CaseEventId, body: CaseEventInput) =
        controller.updateCaseEvent(dbInstance(), testUser.user, clock, eventId, body)

    private fun deleteCaseEvent(eventId: CaseEventId) =
        controller.deleteCaseEvent(dbInstance(), testUser.user, eventId)

    private fun getStudentEvents() =
        controller.getStudent(dbInstance(), testUser.user, testStudent.id).cases.first().events

    @Test
    fun `create new case event, then update it and finally delete it`() {
        assertEquals(emptyList(), getStudentEvents())

        val eventId =
            createCaseEvent(
                testCase.id,
                CaseEventInput(
                    date = LocalDate.of(2023, 12, 8),
                    type = CaseEventType.NOTE,
                    notes = "test",
                ),
            )

        getStudentEvents().also { events ->
            assertEquals(1, events.size)
            events.first().let { event ->
                assertEquals(eventId, event.id)
                assertEquals(testUser.name, event.created.name)
                assertEquals(LocalDate.of(2023, 12, 8), event.date)
                assertEquals(CaseEventType.NOTE, event.type)
                assertEquals("test", event.notes)
                assertNull(event.updated)
            }
        }

        updateCaseEvent(
            eventId,
            CaseEventInput(
                date = LocalDate.of(2023, 12, 7),
                type = CaseEventType.EXPLANATION_REQUEST,
                notes = "test2",
            ),
        )

        getStudentEvents().also { events ->
            assertEquals(1, events.size)
            events.first().let { event ->
                assertEquals(eventId, event.id)
                assertEquals(testUser.name, event.created.name)
                assertEquals(LocalDate.of(2023, 12, 7), event.date)
                assertEquals(CaseEventType.EXPLANATION_REQUEST, event.type)
                assertEquals("test2", event.notes)
                assertEquals(testUser.name, event.updated?.name)
            }
        }

        deleteCaseEvent(eventId)

        assertEquals(0, getStudentEvents().size)
    }
}
