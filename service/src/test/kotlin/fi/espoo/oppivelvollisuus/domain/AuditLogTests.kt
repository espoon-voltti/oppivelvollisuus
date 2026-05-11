// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import fi.espoo.oppivelvollisuus.FullApplicationTest
import fi.espoo.oppivelvollisuus.shared.logging.AUDIT_MARKER
import minimalStudentAndCaseTestInput
import minimalStudentCaseTestInput
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import testUser
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Locks in audit log emission behavior.
 *
 * Audit events are emitted via [fi.espoo.oppivelvollisuus.config.AUDIT_MARKER] ("AUDIT_EVENT")
 * at WARN level on the logger named "fi.espoo.oppivelvollisuus.domain.AppController".
 *
 * The audit() extension function uses:
 *   logger.warn(AUDIT_MARKER, eventCode, StructuredArguments.entries(data))
 * where data = { "userId": user.id, "meta": { ... } }.
 *
 * Phase 2 swaps Audit.kt; these tests must stay green.
 */
class AuditLogTests : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: AppController

    private val listAppender = ListAppender<ILoggingEvent>()

    // Attach to the root logger to capture all loggers (including AppController's)
    private val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger

    @BeforeEach
    fun attachAppender() {
        // Clear any events accumulated from previous tests (shared instance, PER_CLASS lifecycle)
        listAppender.list.clear()
        if (!listAppender.isStarted) {
            listAppender.start()
        }
        rootLogger.addAppender(listAppender)
    }

    @AfterEach
    fun detachAppender() {
        rootLogger.detachAppender(listAppender)
    }

    private fun auditEvents(): List<ILoggingEvent> =
        listAppender.list.filter { event ->
            event.markerList?.any { it == AUDIT_MARKER } == true
        }

    // -------------------------------------------------------------------------
    // CREATE_STUDENT emits an audit event
    // -------------------------------------------------------------------------

    @Test
    fun `createStudent emits CREATE_STUDENT audit event`() {
        controller.createStudent(testUser, minimalStudentAndCaseTestInput, dbInstance())

        val events = auditEvents()
        assertTrue(events.isNotEmpty(), "Expected at least one AUDIT_EVENT marker log, got none")

        val createEvent = events.find { it.message == "CREATE_STUDENT" }
        assertNotNull(createEvent, "Expected CREATE_STUDENT audit event, got: ${events.map { it.message }}")
    }

    @Test
    fun `createStudent audit event carries userId in arguments`() {
        controller.createStudent(testUser, minimalStudentAndCaseTestInput, dbInstance())

        val event =
            auditEvents().find { it.message == "CREATE_STUDENT" }
                ?: error("No CREATE_STUDENT event found")

        // The audit helper logs userId via StructuredArguments.entries(data)
        val argString = event.argumentArray?.joinToString(" ") { it.toString() } ?: ""
        assertTrue(
            argString.contains(testUser.id.toString()),
            "Expected userId ${testUser.id} in audit event arguments: $argString"
        )
    }

    @Test
    fun `createStudent audit event uses WARN level`() {
        controller.createStudent(testUser, minimalStudentAndCaseTestInput, dbInstance())

        val event =
            auditEvents().find { it.message == "CREATE_STUDENT" }
                ?: error("No CREATE_STUDENT event found")

        assertEquals(
            ch.qos.logback.classic.Level.WARN,
            event.level,
            "Audit events must be logged at WARN level"
        )
    }

    @Test
    fun `createStudent audit event has AUDIT_EVENT marker`() {
        controller.createStudent(testUser, minimalStudentAndCaseTestInput, dbInstance())

        val event =
            auditEvents().find { it.message == "CREATE_STUDENT" }
                ?: error("No CREATE_STUDENT event found")

        assertTrue(
            event.markerList?.any { it.name == "AUDIT_EVENT" } == true,
            "Audit event must have AUDIT_EVENT marker"
        )
    }

    // -------------------------------------------------------------------------
    // UPDATE_STUDENT_CASE emits an audit event with caseId in meta
    // -------------------------------------------------------------------------

    @Test
    fun `updateStudentCase emits UPDATE_STUDENT_CASE audit event with caseId`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput, dbInstance())
        val caseId =
            controller
                .getStudent(testUser, studentId, dbInstance())
                .cases
                .first()
                .id

        // Clear events generated by setup calls
        listAppender.list.clear()

        controller.updateStudentCase(testUser, studentId, caseId, minimalStudentCaseTestInput, dbInstance())

        val events = auditEvents()
        val updateEvent = events.find { it.message == "UPDATE_STUDENT_CASE" }
        assertNotNull(updateEvent, "Expected UPDATE_STUDENT_CASE audit event, got: ${events.map { it.message }}")

        // caseId and studentId must appear in the structured arguments (meta map)
        val argString = updateEvent.argumentArray?.joinToString(" ") { it.toString() } ?: ""
        assertTrue(
            argString.contains(caseId.toString()),
            "Expected caseId $caseId in audit event arguments: $argString"
        )
        assertTrue(
            argString.contains(studentId.toString()),
            "Expected studentId $studentId in audit event arguments: $argString"
        )
    }

    // -------------------------------------------------------------------------
    // Logger name is the AppController class name
    // -------------------------------------------------------------------------

    @Test
    fun `audit event logger name is AppController class name`() {
        controller.createStudent(testUser, minimalStudentAndCaseTestInput, dbInstance())

        val event =
            auditEvents().find { it.message == "CREATE_STUDENT" }
                ?: error("No CREATE_STUDENT event found")

        assertEquals(
            "fi.espoo.oppivelvollisuus.domain.AppController",
            event.loggerName,
            "Audit logger name must be the AppController class name"
        )
    }

    // -------------------------------------------------------------------------
    // Non-mutation endpoint also emits audit
    // -------------------------------------------------------------------------

    @Test
    fun `getEmployeeUsers emits GET_EMPLOYEES audit event`() {
        controller.getEmployeeUsers(testUser, dbInstance())

        val events = auditEvents()
        assertNotNull(
            events.find { it.message == "GET_EMPLOYEES" },
            "Expected GET_EMPLOYEES audit event, got: ${events.map { it.message }}"
        )
    }

    // -------------------------------------------------------------------------
    // No audit events before any controller call
    // -------------------------------------------------------------------------

    @Test
    fun `no spurious audit events are emitted without controller calls`() {
        val events = auditEvents()
        assertTrue(events.isEmpty(), "Expected no audit events, got: ${events.map { it.message }}")
    }
}
