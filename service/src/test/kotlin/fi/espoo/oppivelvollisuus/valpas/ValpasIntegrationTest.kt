// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.valpas

import fi.espoo.oppivelvollisuus.FullApplicationTest
import fi.espoo.oppivelvollisuus.StudentCaseId
import fi.espoo.oppivelvollisuus.StudentId
import fi.espoo.oppivelvollisuus.domain.AppController
import fi.espoo.oppivelvollisuus.domain.CaseEventInput
import fi.espoo.oppivelvollisuus.domain.CaseEventType
import fi.espoo.oppivelvollisuus.domain.CaseFinishedReason
import fi.espoo.oppivelvollisuus.domain.CaseSource
import fi.espoo.oppivelvollisuus.domain.CaseStatus
import fi.espoo.oppivelvollisuus.domain.CaseStatusInput
import fi.espoo.oppivelvollisuus.domain.FinishedInfo
import fi.espoo.oppivelvollisuus.domain.StudentCase
import fi.espoo.oppivelvollisuus.domain.StudentCaseInput
import fi.espoo.oppivelvollisuus.domain.ValpasNotifier
import fi.espoo.oppivelvollisuus.domain.findStudentIdBySsn
import fi.espoo.oppivelvollisuus.shared.BadRequest
import fi.espoo.oppivelvollisuus.shared.Conflict
import fi.espoo.oppivelvollisuus.shared.dev.DevStudent
import fi.espoo.oppivelvollisuus.shared.dev.DevStudentCase
import fi.espoo.oppivelvollisuus.shared.dev.DevUser
import fi.espoo.oppivelvollisuus.shared.dev.insert
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import fi.espoo.oppivelvollisuus.shared.time.MockAppClock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.convention.TestBean

@TestPropertySource(
    properties =
        [
            "app.integration.valpas.enabled=true",
            "app.integration.valpas.opintopolku_base_url=http://mock-valpas.invalid",
            "app.integration.valpas.username=test-user",
            "app.integration.valpas.password=test-pass",
            "app.integration.valpas.kunta_oid=1.2.3.4",
        ]
)
class ValpasIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @TestBean(name = "valpasClient") private lateinit var mockValpasClient: ValpasClient

    companion object {
        @JvmStatic fun valpasClient(): ValpasClient = MockValpasClient()
    }

    private val mock: MockValpasClient
        get() = mockValpasClient as MockValpasClient

    @Autowired private lateinit var valpasIntegrationService: ValpasIntegrationService

    @Autowired private lateinit var controller: AppController

    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 1, 15), LocalTime.of(10, 0))
    private val clock = MockAppClock(now)

    private val testUser = DevUser()

    @BeforeEach
    fun setup() {
        mock.statusByQueryId.clear()
        mock.fileContentsByUrl.clear()
        db.transaction { tx -> tx.insert(testUser) }
    }

    // --- Helpers ---

    private fun sampleOppija(
        hetu: String? = "170108A927R",
        notificationId: UUID = UUID.randomUUID(),
        aikaleima: LocalDate = LocalDate.of(2026, 1, 10),
        etunimet: String = "Testi",
        sukunimi: String = "Testilä",
    ): ValpasOppija =
        ValpasOppija(
            oppijanumero = "1.2.246.562.24.${UUID.randomUUID()}",
            etunimet = etunimet,
            sukunimi = sukunimi,
            syntymäaika = LocalDate.of(2008, 1, 17),
            hetu = hetu,
            aktiivinenKuntailmoitus =
                ValpasKuntailmoitus(
                    id = notificationId,
                    aikaleima = aikaleima,
                    oppijanYhteystiedot =
                        ValpasYhteystiedot(
                            puhelinnumero = "040123456",
                            email = "testi@example.com",
                            lähiosoite = "Testikatu 1",
                            postinumero = "02100",
                            postitoimipaikka = "Espoo",
                        ),
                ),
        )

    private fun runFullImport(queryId: String = "test-query-${UUID.randomUUID()}") {
        // Schedule the StartValpasImport job
        db.transaction { tx -> valpasIntegrationService.scheduleStartValpasImport(tx, clock) }
        // Run the StartValpasImport job (which calls startQuery and inserts valpas_query_runs row)
        asyncJobRunner.runPendingJobsSync(clock)
        // Advance from STARTED → FILES_READY → COMPLETED, and schedule ImportValpasOppija jobs
        valpasIntegrationService.advanceValpasImport(db, clock)
        // Run the ImportValpasOppija jobs
        asyncJobRunner.runPendingJobsSync(clock)
    }

    private fun getStudentCases(ssn: String): List<StudentCase> {
        val studentId = db.read { it.findStudentIdBySsn(ssn) } ?: return emptyList()
        return getStudent(studentId).cases
    }

    private fun getStudent(studentId: StudentId) =
        controller.getStudent(dbInstance(), testUser.user, studentId)

    private fun updateStudentCase(
        studentId: StudentId,
        caseId: StudentCaseId,
        body: StudentCaseInput,
    ) = controller.updateStudentCase(dbInstance(), testUser.user, clock, studentId, caseId, body)

    private fun updateStudentCaseStatus(
        studentId: StudentId,
        caseId: StudentCaseId,
        body: CaseStatusInput,
    ) =
        controller.updateStudentCaseStatus(
            dbInstance(),
            testUser.user,
            clock,
            studentId,
            caseId,
            body,
        )

    private fun deleteStudentCase(studentId: StudentId, caseId: StudentCaseId) =
        controller.deleteStudentCase(dbInstance(), testUser.user, studentId, caseId)

    private fun createCaseEvent(caseId: StudentCaseId, body: CaseEventInput) =
        controller.createCaseEvent(dbInstance(), testUser.user, clock, caseId, body)

    private fun markAsDuplicate(caseId: StudentCaseId, targetCaseId: StudentCaseId) =
        controller.markAsDuplicate(
            dbInstance(),
            testUser.user,
            clock,
            caseId,
            AppController.MarkAsDuplicateBody(targetCaseId),
        )

    private fun countCases(): Int = db.read { tx ->
        tx.createQuery { sql("SELECT count(*) FROM student_cases") }.exactlyOne<Int>()
    }

    private fun countStudents(): Int = db.read { tx ->
        tx.createQuery { sql("SELECT count(*) FROM students") }.exactlyOne<Int>()
    }

    @Test
    fun `happy path imports a new student and case`() {
        val notificationId = UUID.randomUUID()
        val aikaleima = LocalDate.of(2026, 1, 10)
        val oppija = sampleOppija(notificationId = notificationId, aikaleima = aikaleima)

        mock.nextStartReturnsQueryId = "query-happy"
        mock.stageCompleteResult("query-happy", listOf(oppija))

        runFullImport("query-happy")

        val cases = getStudentCases("170108A927R")
        assertEquals(1, cases.size)
        val case = cases.first()
        assertEquals(CaseStatus.IMPORTED_FROM_VALPAS, case.status)
        assertEquals(notificationId, case.valpasNotificationId)
        assertEquals(aikaleima, case.openedAt)
        assertEquals(CaseSource.VALPAS_NOTICE, case.source)
    }

    @Test
    fun `missing hetu skips row`() {
        val oppija = sampleOppija(hetu = null)

        mock.nextStartReturnsQueryId = "query-no-hetu"
        mock.stageCompleteResult("query-no-hetu", listOf(oppija))

        runFullImport("query-no-hetu")

        assertEquals(0, countStudents())
        assertEquals(0, countCases())
    }

    @Test
    fun `existing student matched by ssn is reused, no fields overwritten`() {
        // Pre-seed a student with the same SSN but different name
        val existingStudent =
            DevStudent(
                createdBy = testUser.id,
                created = now,
                ssn = "170108A927R",
                firstName = "OriginalFirst",
                lastName = "OriginalLast",
            )
        db.transaction { tx -> tx.insert(existingStudent) }

        val notificationId = UUID.randomUUID()
        val oppija =
            sampleOppija(
                hetu = "170108A927R",
                notificationId = notificationId,
                etunimet = "ValpasFirst",
                sukunimi = "ValpasLast",
            )

        mock.nextStartReturnsQueryId = "query-reuse"
        mock.stageCompleteResult("query-reuse", listOf(oppija))

        runFullImport("query-reuse")

        // Student count should still be 1 (reused, not created new)
        assertEquals(1, countStudents())

        // The student's manual fields should be unchanged
        val student = getStudent(existingStudent.id).student
        assertEquals("OriginalFirst", student.firstName)
        assertEquals("OriginalLast", student.lastName)

        // A new case should have been created for the student
        val cases = getStudent(existingStudent.id).cases
        assertEquals(1, cases.size)
        assertEquals(CaseStatus.IMPORTED_FROM_VALPAS, cases.first().status)
        assertEquals(notificationId, cases.first().valpasNotificationId)
    }

    @Test
    fun `re-running with same notification id is a no-op`() {
        val notificationId = UUID.randomUUID()
        val oppija = sampleOppija(notificationId = notificationId)

        mock.nextStartReturnsQueryId = "query-idempotent-1"
        mock.stageCompleteResult("query-idempotent-1", listOf(oppija))
        runFullImport("query-idempotent-1")

        assertEquals(1, countCases())

        // Second run with same notification id
        mock.nextStartReturnsQueryId = "query-idempotent-2"
        mock.stageCompleteResult("query-idempotent-2", listOf(oppija))
        runFullImport("query-idempotent-2")

        // Case count should still be 1
        assertEquals(1, countCases())
    }

    @Test
    fun `second notification for same student replaces the first IMPORTED_FROM_VALPAS case`() {
        val notificationIdA = UUID.randomUUID()
        val notificationIdB = UUID.randomUUID()
        val oppija = sampleOppija(hetu = "170108A927R")

        // First import with notification A
        val oppijaA =
            oppija.copy(
                aktiivinenKuntailmoitus =
                    oppija.aktiivinenKuntailmoitus!!.copy(id = notificationIdA)
            )
        mock.nextStartReturnsQueryId = "query-replace-1"
        mock.stageCompleteResult("query-replace-1", listOf(oppijaA))
        runFullImport("query-replace-1")

        assertEquals(1, countCases())
        val casesAfterFirst = getStudentCases("170108A927R")
        assertEquals(notificationIdA, casesAfterFirst.first().valpasNotificationId)

        // Second import with notification B for same student
        val oppijaB =
            oppija.copy(
                aktiivinenKuntailmoitus = oppija.aktiivinenKuntailmoitus.copy(id = notificationIdB)
            )
        mock.nextStartReturnsQueryId = "query-replace-2"
        mock.stageCompleteResult("query-replace-2", listOf(oppijaB))
        runFullImport("query-replace-2")

        // Only one IMPORTED_FROM_VALPAS case should exist, with notification B
        val casesAfterSecond = getStudentCases("170108A927R")
        assertEquals(1, casesAfterSecond.size)
        assertEquals(CaseStatus.IMPORTED_FROM_VALPAS, casesAfterSecond.first().status)
        assertEquals(notificationIdB, casesAfterSecond.first().valpasNotificationId)
    }

    @Test
    fun `approving via PUT status endpoint requires sourceValpas`() {
        val notificationId = UUID.randomUUID()
        val oppija = sampleOppija(notificationId = notificationId)

        mock.nextStartReturnsQueryId = "query-approve"
        mock.stageCompleteResult("query-approve", listOf(oppija))
        runFullImport("query-approve")

        val cases = getStudentCases("170108A927R")
        assertEquals(1, cases.size)
        val importedCase = cases.first()
        val studentId = importedCase.studentId
        val caseId = importedCase.id

        // Trying to approve without sourceValpas should fail (400)
        assertThrows<BadRequest> {
            updateStudentCaseStatus(studentId, caseId, CaseStatusInput(CaseStatus.TODO, null))
        }

        // Update the case to set sourceValpas
        updateStudentCase(
            studentId,
            caseId,
            StudentCaseInput(
                openedAt = importedCase.openedAt,
                assignedTo = null,
                source = CaseSource.VALPAS_NOTICE,
                sourceValpas = ValpasNotifier.PERUSOPETUS,
                sourceOther = null,
                sourceContact = "",
                schoolBackground = emptySet(),
                caseBackgroundReasons = emptySet(),
                notInSchoolReason = null,
            ),
        )

        // Now approve should succeed
        updateStudentCaseStatus(studentId, caseId, CaseStatusInput(CaseStatus.TODO, null))

        val updatedCases = getStudent(studentId).cases
        assertEquals(CaseStatus.TODO, updatedCases.first().status)
    }

    @Test
    fun `approving blocked by another active case`() {
        val notificationId = UUID.randomUUID()
        val oppija = sampleOppija(hetu = "170108A927R", notificationId = notificationId)

        // Pre-seed a student with an active TODO case
        val existingStudent =
            DevStudent(createdBy = testUser.id, created = now, ssn = "170108A927R")
        val existingCase =
            DevStudentCase(
                studentId = existingStudent.id,
                createdBy = testUser.id,
                created = now,
                status = CaseStatus.TODO,
            )
        db.transaction { tx ->
            tx.insert(existingStudent)
            tx.insert(existingCase)
        }

        // Import creates an IMPORTED_FROM_VALPAS case for the same student
        mock.nextStartReturnsQueryId = "query-conflict"
        mock.stageCompleteResult("query-conflict", listOf(oppija))
        runFullImport("query-conflict")

        // Find the IMPORTED_FROM_VALPAS case
        val allCases = getStudent(existingStudent.id).cases
        assertEquals(2, allCases.size)
        val importedCase = allCases.first { it.status == CaseStatus.IMPORTED_FROM_VALPAS }

        // Set sourceValpas on the imported case first
        updateStudentCase(
            existingStudent.id,
            importedCase.id,
            StudentCaseInput(
                openedAt = importedCase.openedAt,
                assignedTo = null,
                source = CaseSource.VALPAS_NOTICE,
                sourceValpas = ValpasNotifier.PERUSOPETUS,
                sourceOther = null,
                sourceContact = "",
                schoolBackground = emptySet(),
                caseBackgroundReasons = emptySet(),
                notInSchoolReason = null,
            ),
        )

        // Trying to approve should fail with Conflict because student already has a TODO case
        assertThrows<Conflict> {
            updateStudentCaseStatus(
                existingStudent.id,
                importedCase.id,
                CaseStatusInput(CaseStatus.TODO, null),
            )
        }
    }

    @Test
    fun `case events and DELETE refused on IMPORTED_FROM_VALPAS`() {
        val notificationId = UUID.randomUUID()
        val oppija = sampleOppija(notificationId = notificationId)

        mock.nextStartReturnsQueryId = "query-refuse"
        mock.stageCompleteResult("query-refuse", listOf(oppija))
        runFullImport("query-refuse")

        val cases = getStudentCases("170108A927R")
        assertEquals(1, cases.size)
        val importedCase = cases.first()

        // POST case-event should fail with 400
        assertThrows<BadRequest> {
            createCaseEvent(
                importedCase.id,
                CaseEventInput(
                    date = LocalDate.of(2026, 1, 15),
                    type = CaseEventType.NOTE,
                    notes = "test note",
                ),
            )
        }

        // DELETE the case should fail with 400
        assertThrows<BadRequest> { deleteStudentCase(importedCase.studentId, importedCase.id) }
    }

    @Test
    fun `mark-as-duplicate copies id and deletes imported case`() {
        val notificationId = UUID.randomUUID()

        // Pre-seed student with active TODO case (no valpas_notification_id)
        val existingStudent =
            DevStudent(createdBy = testUser.id, created = now, ssn = "170108A927R")
        val activeCaseId = StudentCaseId(UUID.randomUUID())
        val activeCase =
            DevStudentCase(
                id = activeCaseId,
                studentId = existingStudent.id,
                createdBy = testUser.id,
                created = now,
                status = CaseStatus.TODO,
            )
        db.transaction { tx ->
            tx.insert(existingStudent)
            tx.insert(activeCase)
        }

        // Import a notification for the same student
        val oppija = sampleOppija(hetu = "170108A927R", notificationId = notificationId)
        mock.nextStartReturnsQueryId = "query-dup"
        mock.stageCompleteResult("query-dup", listOf(oppija))
        runFullImport("query-dup")

        // Find the IMPORTED_FROM_VALPAS case
        val allCases = getStudent(existingStudent.id).cases
        assertEquals(2, allCases.size)
        val importedCase = allCases.first { it.status == CaseStatus.IMPORTED_FROM_VALPAS }
        val importedCaseId = importedCase.id

        // Mark as duplicate of the active case
        markAsDuplicate(importedCaseId, activeCaseId)

        // Imported case should be deleted
        val casesAfter = getStudent(existingStudent.id).cases
        assertEquals(1, casesAfter.size)
        assertEquals(activeCaseId, casesAfter.first().id)

        // Active case's valpas_notification_id should now be set to the imported case's
        // notification id
        assertEquals(notificationId, casesAfter.first().valpasNotificationId)
    }

    private fun seedFinishedCase(studentId: StudentId, openedAt: LocalDate): StudentCaseId {
        val id = StudentCaseId(UUID.randomUUID())
        db.transaction { tx ->
            tx.insert(
                DevStudentCase(
                    id = id,
                    studentId = studentId,
                    createdBy = testUser.id,
                    created = now,
                    openedAt = openedAt,
                    status = CaseStatus.TODO,
                )
            )
        }
        updateStudentCaseStatus(
            studentId,
            id,
            CaseStatusInput(
                CaseStatus.FINISHED,
                FinishedInfo(CaseFinishedReason.COMPULSORY_EDUCATION_SUSPENDED, null, null, null),
            ),
        )
        return id
    }

    @Test
    fun `mark-as-duplicate merges into the specified FINISHED case`() {
        val notificationId = UUID.randomUUID()

        val existingStudent =
            DevStudent(createdBy = testUser.id, created = now, ssn = "170108A927R")
        db.transaction { tx -> tx.insert(existingStudent) }
        val olderFinishedId = seedFinishedCase(existingStudent.id, LocalDate.of(2024, 1, 1))
        val newerFinishedId = seedFinishedCase(existingStudent.id, LocalDate.of(2024, 6, 1))

        val oppija = sampleOppija(hetu = "170108A927R", notificationId = notificationId)
        mock.nextStartReturnsQueryId = "query-dup-finished"
        mock.stageCompleteResult("query-dup-finished", listOf(oppija))
        runFullImport("query-dup-finished")

        val allCases = getStudent(existingStudent.id).cases
        val importedCase = allCases.first { it.status == CaseStatus.IMPORTED_FROM_VALPAS }

        markAsDuplicate(importedCase.id, newerFinishedId)

        val casesAfter = getStudent(existingStudent.id).cases
        assertEquals(2, casesAfter.size)
        val newerAfter = casesAfter.first { it.id == newerFinishedId }
        val olderAfter = casesAfter.first { it.id == olderFinishedId }
        assertEquals(notificationId, newerAfter.valpasNotificationId)
        assertEquals(null, olderAfter.valpasNotificationId)
    }

    @Test
    fun `mark-as-duplicate rejected when target already has notification id`() {
        val previousNotificationId = UUID.randomUUID()

        val existingStudent =
            DevStudent(createdBy = testUser.id, created = now, ssn = "170108A927R")
        val activeCaseId = StudentCaseId(UUID.randomUUID())
        val activeCase =
            DevStudentCase(
                id = activeCaseId,
                studentId = existingStudent.id,
                createdBy = testUser.id,
                created = now,
                status = CaseStatus.TODO,
                valpasNotificationId = previousNotificationId,
            )
        db.transaction { tx ->
            tx.insert(existingStudent)
            tx.insert(activeCase)
        }

        val oppija = sampleOppija(hetu = "170108A927R", notificationId = UUID.randomUUID())
        mock.nextStartReturnsQueryId = "query-dup-claimed"
        mock.stageCompleteResult("query-dup-claimed", listOf(oppija))
        runFullImport("query-dup-claimed")

        val importedCase =
            getStudent(existingStudent.id).cases.first {
                it.status == CaseStatus.IMPORTED_FROM_VALPAS
            }

        assertThrows<Conflict> { markAsDuplicate(importedCase.id, activeCaseId) }
    }

    @Test
    fun `poll timeout marks query run FAILED`() {
        // Insert a STARTED row with started_polling_at 7 hours in the past
        val pastNow = now.minusHours(7)
        val queryRunId = db.transaction { tx ->
            tx.insertValpasQueryRun("query-timeout-poll", pastNow)
        }

        // Configure mock to return Pending (it would never complete)
        mock.statusByQueryId["query-timeout-poll"] = ValpasQueryStatus.Pending

        // advanceValpasImport should detect timeout and mark FAILED
        valpasIntegrationService.advanceValpasImport(db, clock)

        val result = db.read { it.getMostRecentValpasQueryRun() }
        assertNotNull(result)
        assertEquals(ValpasQueryRunState.FAILED, result.state)
    }

    @Test
    fun `valpas failed marks query run FAILED`() {
        val oppija = sampleOppija()

        // Start a query
        val queryId = "query-valpas-failed"
        mock.nextStartReturnsQueryId = queryId
        // Stage a FAILED status
        mock.statusByQueryId[queryId] = ValpasQueryStatus.Failed

        // Schedule and run the StartValpasImport job
        db.transaction { tx -> valpasIntegrationService.scheduleStartValpasImport(tx, clock) }
        asyncJobRunner.runPendingJobsSync(clock)

        // Now advance — it should detect ValpasQueryStatus.Failed and mark FAILED
        valpasIntegrationService.advanceValpasImport(db, clock)

        val result = db.read { it.getMostRecentValpasQueryRun() }
        assertNotNull(result)
        assertEquals(ValpasQueryRunState.FAILED, result.state)
    }

    @Test
    fun `download timeout marks query run FAILED`() {
        // Insert a FILES_READY row with started_downloading_at 4 hours in the past
        val fileUrl = "https://mock-valpas/timeout-file.json"
        val pastNow = now.minusHours(4)

        val queryRunId = db.transaction { tx ->
            tx.insertValpasQueryRun("query-timeout-dl", pastNow)
        }
        db.transaction { tx ->
            tx.markValpasQueryRunFilesReady(queryRunId, listOf(fileUrl), pastNow)
        }

        // advanceValpasImport should detect download timeout and mark FAILED
        valpasIntegrationService.advanceValpasImport(db, clock)

        val result = db.read { it.getMostRecentValpasQueryRun() }
        assertNotNull(result)
        assertEquals(ValpasQueryRunState.FAILED, result.state)
    }

    @Test
    fun `StartValpasImport fails stale STARTED row`() {
        // Insert a stale STARTED row
        val staleRunId = db.transaction { tx ->
            tx.insertValpasQueryRun("query-stale", now.minusHours(1))
        }

        // Schedule and run scheduleStartValpasImport — it should fail the stale row
        db.transaction { tx -> valpasIntegrationService.scheduleStartValpasImport(tx, clock) }

        val result = db.read { it.getMostRecentValpasQueryRun() }
        assertNotNull(result)
        // The stale row should have been marked FAILED
        assertEquals(ValpasQueryRunState.FAILED, result.state)
        assertEquals(staleRunId, result.id)
    }
}
