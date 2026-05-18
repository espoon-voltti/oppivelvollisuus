// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.FullApplicationTestOld
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import minimalStudentAndCaseTestInput
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import testUser
import testUserName

class CaseEventTests : FullApplicationTestOld() {
    @Autowired lateinit var controller: AppController

    @Test
    fun `create new case event, then update it and finally delete it`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)
        val caseId = controller.getStudent(testUser, studentId).cases.first().id

        var events = controller.getStudent(testUser, studentId).cases.first().events
        assertEquals(emptyList(), events)

        val eventId =
            controller.createCaseEvent(
                testUser,
                caseId,
                CaseEventInput(
                    date = LocalDate.of(2023, 12, 8),
                    type = CaseEventType.NOTE,
                    notes = "test",
                ),
            )

        events = controller.getStudent(testUser, studentId).cases.first().events
        assertEquals(1, events.size)
        events.first().let { event ->
            assertEquals(eventId, event.id)
            assertEquals(testUserName, event.created.name)
            assertEquals(LocalDate.of(2023, 12, 8), event.date)
            assertEquals(CaseEventType.NOTE, event.type)
            assertEquals("test", event.notes)
            assertNull(event.updated)
        }

        controller.updateCaseEvent(
            testUser,
            eventId,
            CaseEventInput(
                date = LocalDate.of(2023, 12, 7),
                type = CaseEventType.EXPLANATION_REQUEST,
                notes = "test2",
            ),
        )

        events = controller.getStudent(testUser, studentId).cases.first().events
        assertEquals(1, events.size)
        events.first().let { event ->
            assertEquals(eventId, event.id)
            assertEquals(testUserName, event.created.name)
            assertEquals(LocalDate.of(2023, 12, 7), event.date)
            assertEquals(CaseEventType.EXPLANATION_REQUEST, event.type)
            assertEquals("test2", event.notes)
            assertEquals(testUserName, event.updated?.name)
        }

        controller.deleteCaseEvent(testUser, eventId)

        events = controller.getStudent(testUser, studentId).cases.first().events
        assertEquals(0, events.size)
    }
}
