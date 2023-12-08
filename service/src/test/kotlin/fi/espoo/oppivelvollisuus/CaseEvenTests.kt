package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.domain.AppController
import fi.espoo.oppivelvollisuus.domain.CaseEventInput
import fi.espoo.oppivelvollisuus.domain.CaseEventType
import minimalStudentAndCaseTestInput
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import testUser
import testUserName
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CaseEvenTests : FullApplicationTest() {
    @Autowired
    lateinit var controller: AppController

    @Test
    fun `create new case event, then update it and finally delete it`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)
        val caseId = controller.getStudent(studentId).cases.first().id
        assertEquals(emptyList(), controller.getCaseEvents(caseId))

        val eventId = controller.createCaseEvent(
            testUser,
            caseId,
            CaseEventInput(
                date = LocalDate.of(2023, 12, 8),
                type = CaseEventType.NOTE,
                notes = "test"
            )
        )

        var events = controller.getCaseEvents(caseId)
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
                notes = "test2"
            )
        )

        events = controller.getCaseEvents(caseId)
        assertEquals(1, events.size)
        events.first().let { event ->
            assertEquals(eventId, event.id)
            assertEquals(testUserName, event.created.name)
            assertEquals(LocalDate.of(2023, 12, 7), event.date)
            assertEquals(CaseEventType.EXPLANATION_REQUEST, event.type)
            assertEquals("test2", event.notes)
            assertEquals(testUserName, event.updated?.name)
        }

        controller.deleteCaseEvent(eventId)

        events = controller.getCaseEvents(caseId)
        assertEquals(0, events.size)
    }
}
