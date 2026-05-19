// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.FullApplicationTest
import fi.espoo.oppivelvollisuus.StudentCaseId
import fi.espoo.oppivelvollisuus.StudentId
import fi.espoo.oppivelvollisuus.UserBasics
import fi.espoo.oppivelvollisuus.shared.BadRequest
import fi.espoo.oppivelvollisuus.shared.dev.DevStudent
import fi.espoo.oppivelvollisuus.shared.dev.DevStudentCase
import fi.espoo.oppivelvollisuus.shared.dev.DevUser
import fi.espoo.oppivelvollisuus.shared.dev.insert
import fi.espoo.oppivelvollisuus.shared.isUniqueConstraintViolation
import fi.espoo.oppivelvollisuus.shared.time.HelsinkiDateTime
import fi.espoo.oppivelvollisuus.shared.time.MockAppClock
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class StudentCaseTests : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: AppController

    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0))
    private val clock = MockAppClock(now)

    private val testUser = DevUser()
    private val testStudent = DevStudent(createdBy = testUser.id, created = now)

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(testUser)
            tx.insert(testStudent)
        }
    }

    private fun seedCase(
        openedAt: LocalDate = LocalDate.of(2023, 12, 7),
        status: CaseStatus = CaseStatus.TODO,
    ): StudentCaseId {
        val case =
            DevStudentCase(
                studentId = testStudent.id,
                createdBy = testUser.id,
                created = now,
                openedAt = openedAt,
                status = status,
            )
        db.transaction { tx -> tx.insert(case) }
        return case.id
    }

    private fun createStudentCase(body: StudentCaseInput): StudentCaseId =
        controller.createStudentCase(dbInstance(), testUser.user, clock, testStudent.id, body)

    private fun updateStudentCase(caseId: StudentCaseId, body: StudentCaseInput) =
        controller.updateStudentCase(
            dbInstance(),
            testUser.user,
            clock,
            testStudent.id,
            caseId,
            body,
        )

    private fun updateStudentCaseStatus(caseId: StudentCaseId, body: CaseStatusInput) =
        controller.updateStudentCaseStatus(
            dbInstance(),
            testUser.user,
            clock,
            testStudent.id,
            caseId,
            body,
        )

    private fun deleteStudentCase(caseId: StudentCaseId) =
        controller.deleteStudentCase(dbInstance(), testUser.user, testStudent.id, caseId)

    private fun createCaseEvent(caseId: StudentCaseId, body: CaseEventInput) =
        controller.createCaseEvent(dbInstance(), testUser.user, clock, caseId, body)

    private fun getStudent(studentId: StudentId = testStudent.id) =
        controller.getStudent(dbInstance(), testUser.user, studentId)

    private fun getStudents() =
        controller.getStudents(
            dbInstance(),
            testUser.user,
            StudentSearchParams(
                query = "",
                statuses = CaseStatus.entries,
                sources = CaseSource.entries,
                assignee = null,
            ),
        )

    @Test
    fun `create another student case with all data`() {
        val firstCaseId = seedCase()
        updateStudentCaseStatus(
            firstCaseId,
            CaseStatusInput(
                CaseStatus.FINISHED,
                FinishedInfo(CaseFinishedReason.OTHER, null, null, "other reason"),
            ),
        )

        val caseId =
            createStudentCase(
                StudentCaseInput(
                    openedAt = LocalDate.of(2023, 12, 8),
                    assignedTo = testUser.id,
                    source = CaseSource.OTHER,
                    sourceValpas = null,
                    sourceOther = OtherNotifier.LASTENSUOJELU,
                    sourceContact = "Lastensuojelu, Minna Mikkola",
                    schoolBackground = SchoolBackground.entries.toSet(),
                    caseBackgroundReasons = CaseBackgroundReason.entries.toSet(),
                    notInSchoolReason = NotInSchoolReason.KATSOTTU_ERONNEEKSI_OPPILAITOKSESTA,
                )
            )

        val studentResponse = getStudent()
        assertEquals(2, studentResponse.cases.size)
        studentResponse.cases.first().let { studentCase ->
            assertEquals(
                StudentCase(
                    id = caseId,
                    studentId = testStudent.id,
                    openedAt = LocalDate.of(2023, 12, 8),
                    assignedTo = UserBasics(id = testUser.id, name = testUser.name),
                    status = CaseStatus.TODO,
                    finishedInfo = null,
                    source = CaseSource.OTHER,
                    sourceValpas = null,
                    sourceOther = OtherNotifier.LASTENSUOJELU,
                    sourceContact = "Lastensuojelu, Minna Mikkola",
                    schoolBackground = SchoolBackground.entries.toSet(),
                    caseBackgroundReasons = CaseBackgroundReason.entries.toSet(),
                    notInSchoolReason = NotInSchoolReason.KATSOTTU_ERONNEEKSI_OPPILAITOKSESTA,
                    events = emptyList(),
                ),
                studentCase,
            )
        }
    }

    @Test
    fun `create another student case with minimal data and update it`() {
        val firstCaseId = seedCase()
        updateStudentCaseStatus(
            firstCaseId,
            CaseStatusInput(
                CaseStatus.FINISHED,
                FinishedInfo(CaseFinishedReason.OTHER, null, null, ""),
            ),
        )

        val caseId =
            createStudentCase(
                StudentCaseInput(
                    openedAt = LocalDate.of(2023, 12, 8),
                    assignedTo = null,
                    source = CaseSource.VALPAS_AUTOMATIC_CHECK,
                    sourceValpas = null,
                    sourceOther = null,
                    sourceContact = "",
                    schoolBackground = emptySet(),
                    caseBackgroundReasons = emptySet(),
                    notInSchoolReason = null,
                )
            )

        var studentResponse = getStudent()
        assertEquals(2, studentResponse.cases.size)
        studentResponse.cases.first().let { studentCase ->
            assertEquals(
                StudentCase(
                    id = caseId,
                    studentId = testStudent.id,
                    openedAt = LocalDate.of(2023, 12, 8),
                    assignedTo = null,
                    status = CaseStatus.TODO,
                    finishedInfo = null,
                    source = CaseSource.VALPAS_AUTOMATIC_CHECK,
                    sourceValpas = null,
                    sourceOther = null,
                    sourceContact = "",
                    schoolBackground = emptySet(),
                    caseBackgroundReasons = emptySet(),
                    notInSchoolReason = null,
                    events = emptyList(),
                ),
                studentCase,
            )
        }

        updateStudentCase(
            caseId,
            StudentCaseInput(
                openedAt = LocalDate.of(2023, 12, 9),
                assignedTo = testUser.id,
                source = CaseSource.VALPAS_NOTICE,
                sourceValpas = ValpasNotifier.LUKIO,
                sourceOther = null,
                sourceContact = "Espoon lukio",
                schoolBackground = setOf(SchoolBackground.EI_PERUSKOULUN_PAATTOTODISTUSTA),
                caseBackgroundReasons =
                    setOf(CaseBackgroundReason.MOTIVAATION_PUUTE, CaseBackgroundReason.MUU_SYY),
                notInSchoolReason =
                    NotInSchoolReason.EI_OLE_ALOITTANUT_VASTAANOTTAMASSAAN_OPISKELUPAIKASSA,
            ),
        )

        studentResponse = getStudent()
        assertEquals(2, studentResponse.cases.size)
        studentResponse.cases.first().let { studentCase ->
            assertEquals(
                StudentCase(
                    id = caseId,
                    studentId = testStudent.id,
                    openedAt = LocalDate.of(2023, 12, 9),
                    assignedTo = UserBasics(id = testUser.id, name = testUser.name),
                    status = CaseStatus.TODO,
                    finishedInfo = null,
                    source = CaseSource.VALPAS_NOTICE,
                    sourceValpas = ValpasNotifier.LUKIO,
                    sourceOther = null,
                    sourceContact = "Espoon lukio",
                    schoolBackground = setOf(SchoolBackground.EI_PERUSKOULUN_PAATTOTODISTUSTA),
                    caseBackgroundReasons =
                        setOf(CaseBackgroundReason.MOTIVAATION_PUUTE, CaseBackgroundReason.MUU_SYY),
                    notInSchoolReason =
                        NotInSchoolReason.EI_OLE_ALOITTANUT_VASTAANOTTAMASSAAN_OPISKELUPAIKASSA,
                    events = emptyList(),
                ),
                studentCase,
            )
        }
    }

    @Test
    fun `cannot create another student case if all others are not finished`() {
        seedCase()

        assertThrows<UnableToExecuteStatementException> { createStudentCase(minimalCaseInput()) }
            .also { assertTrue { it.isUniqueConstraintViolation() } }
    }

    @Test
    fun `change status to ON_HOLD`() {
        val caseId = seedCase()

        updateStudentCaseStatus(caseId, CaseStatusInput(CaseStatus.ON_HOLD, null))

        val updatedCase = getStudent().cases.first()
        assertEquals(CaseStatus.ON_HOLD, updatedCase.status)
        assertNull(updatedCase.finishedInfo)
    }

    @Test
    fun `change status to FINISHED`() {
        val caseId = seedCase()

        updateStudentCaseStatus(
            caseId,
            CaseStatusInput(
                CaseStatus.FINISHED,
                FinishedInfo(CaseFinishedReason.OTHER, null, null, "other reason"),
            ),
        )

        val updatedCase = getStudent().cases.first()
        assertEquals(CaseStatus.FINISHED, updatedCase.status)
        assertEquals(CaseFinishedReason.OTHER, updatedCase.finishedInfo?.reason)
        assertEquals("other reason", updatedCase.finishedInfo?.otherReason)
        assertNull(updatedCase.finishedInfo?.startedAtSchool)
    }

    @Test
    fun `change status to FINISHED with BEGAN_STUDIES`() {
        val caseId = seedCase()

        updateStudentCaseStatus(
            caseId,
            CaseStatusInput(
                CaseStatus.FINISHED,
                FinishedInfo(CaseFinishedReason.BEGAN_STUDIES, SchoolType.LUKIO, null, null),
            ),
        )

        val updatedCase = getStudent().cases.first()
        assertEquals(CaseStatus.FINISHED, updatedCase.status)
        assertEquals(CaseFinishedReason.BEGAN_STUDIES, updatedCase.finishedInfo?.reason)
        assertEquals(SchoolType.LUKIO, updatedCase.finishedInfo?.startedAtSchool)
    }

    @Test
    fun `change status to FINISHED with COMPULSORY_EDUCATION_ENDED`() {
        val caseId = seedCase()

        val givenFollowUpMeasures =
            setOf(FollowUpMeasure.SOCIAL_SERVICES, FollowUpMeasure.LANGUAGE_COURSE)
        updateStudentCaseStatus(
            caseId,
            CaseStatusInput(
                CaseStatus.FINISHED,
                FinishedInfo(
                    CaseFinishedReason.COMPULSORY_EDUCATION_ENDED,
                    null,
                    givenFollowUpMeasures,
                    null,
                ),
            ),
        )

        val updatedCase = getStudent().cases.first()
        assertEquals(CaseStatus.FINISHED, updatedCase.status)
        assertEquals(
            CaseFinishedReason.COMPULSORY_EDUCATION_ENDED,
            updatedCase.finishedInfo?.reason,
        )
        assertEquals(givenFollowUpMeasures, updatedCase.finishedInfo?.followUpMeasures)
    }

    @Test
    fun `cannot change status to COMPULSORY_EDUCATION_ENDED without follow up measure`() {
        val caseId = seedCase()

        assertThrows<BadRequest> {
            updateStudentCaseStatus(
                caseId,
                CaseStatusInput(
                    CaseStatus.FINISHED,
                    FinishedInfo(CaseFinishedReason.COMPULSORY_EDUCATION_ENDED, null, null, null),
                ),
            )
        }
    }

    @Test
    fun `cannot change status to FINISHED without reason`() {
        val caseId = seedCase()

        assertThrows<BadRequest> {
            updateStudentCaseStatus(caseId, CaseStatusInput(CaseStatus.FINISHED, null))
        }
    }

    @Test
    fun `cannot change status to FINISHED with BEGAN_STUDIES without school type`() {
        val caseId = seedCase()

        assertThrows<BadRequest> {
            updateStudentCaseStatus(
                caseId,
                CaseStatusInput(
                    CaseStatus.FINISHED,
                    FinishedInfo(CaseFinishedReason.BEGAN_STUDIES, null, null, null),
                ),
            )
        }
    }

    @Test
    fun `cannot provide startedAtSchool when reason is not BEGAN_STUDIES`() {
        val caseId = seedCase()

        assertThrows<BadRequest> {
            updateStudentCaseStatus(
                caseId,
                CaseStatusInput(
                    CaseStatus.FINISHED,
                    FinishedInfo(
                        CaseFinishedReason.COMPULSORY_EDUCATION_SUSPENDED,
                        SchoolType.LUKIO,
                        null,
                        null,
                    ),
                ),
            )
        }
    }

    @Test
    fun `reset status after finishing`() {
        val caseId = seedCase()
        updateStudentCaseStatus(
            caseId,
            CaseStatusInput(
                CaseStatus.FINISHED,
                FinishedInfo(CaseFinishedReason.BEGAN_STUDIES, SchoolType.LUKIO, null, null),
            ),
        )

        updateStudentCaseStatus(caseId, CaseStatusInput(CaseStatus.TODO, null))

        val updatedCase = getStudent().cases.first()
        assertEquals(CaseStatus.TODO, updatedCase.status)
        assertNull(updatedCase.finishedInfo)
    }

    @Test
    fun `cannot reset status after finishing if there already is another unfinished case`() {
        val caseId = seedCase()
        updateStudentCaseStatus(
            caseId,
            CaseStatusInput(
                CaseStatus.FINISHED,
                FinishedInfo(CaseFinishedReason.BEGAN_STUDIES, SchoolType.LUKIO, null, null),
            ),
        )
        createStudentCase(minimalCaseInput())

        assertThrows<UnableToExecuteStatementException> {
                updateStudentCaseStatus(caseId, CaseStatusInput(CaseStatus.TODO, null))
            }
            .also { assertTrue { it.isUniqueConstraintViolation() } }
    }

    @Test
    fun `deleting student case without events`() {
        val caseId = seedCase()

        deleteStudentCase(caseId)

        assertEquals(0, getStudent().cases.size)
        assertEquals(
            listOf(
                StudentSummary(
                    id = testStudent.id,
                    firstName = testStudent.firstName,
                    lastName = testStudent.lastName,
                    openedAt = null,
                    status = null,
                    source = null,
                    assignedTo = null,
                    lastEvent = null,
                )
            ),
            getStudents(),
        )
    }

    @Test
    fun `deleting student case with events fails`() {
        val caseId = seedCase()
        createCaseEvent(
            caseId,
            CaseEventInput(
                date = LocalDate.of(2023, 12, 8),
                type = CaseEventType.NOTE,
                notes = "test",
            ),
        )

        assertThrows<UnableToExecuteStatementException> { deleteStudentCase(caseId) }
    }

    private fun minimalCaseInput(): StudentCaseInput =
        StudentCaseInput(
            openedAt = LocalDate.of(2023, 12, 7),
            assignedTo = null,
            source = CaseSource.VALPAS_AUTOMATIC_CHECK,
            sourceValpas = null,
            sourceOther = null,
            sourceContact = "",
            schoolBackground = emptySet(),
            caseBackgroundReasons = emptySet(),
            notInSchoolReason = null,
        )
}
