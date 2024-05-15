// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.common.BadRequest
import fi.espoo.oppivelvollisuus.common.UserBasics
import fi.espoo.oppivelvollisuus.common.isUniqueConstraintViolation
import fi.espoo.oppivelvollisuus.domain.AppController
import fi.espoo.oppivelvollisuus.domain.CaseBackgroundReason
import fi.espoo.oppivelvollisuus.domain.CaseEventInput
import fi.espoo.oppivelvollisuus.domain.CaseEventType
import fi.espoo.oppivelvollisuus.domain.CaseFinishedReason
import fi.espoo.oppivelvollisuus.domain.CaseSource
import fi.espoo.oppivelvollisuus.domain.CaseStatus
import fi.espoo.oppivelvollisuus.domain.CaseStatusInput
import fi.espoo.oppivelvollisuus.domain.FinishedInfo
import fi.espoo.oppivelvollisuus.domain.NotInSchoolReason
import fi.espoo.oppivelvollisuus.domain.OtherNotifier
import fi.espoo.oppivelvollisuus.domain.SchoolBackground
import fi.espoo.oppivelvollisuus.domain.SchoolType
import fi.espoo.oppivelvollisuus.domain.StudentCase
import fi.espoo.oppivelvollisuus.domain.StudentCaseInput
import fi.espoo.oppivelvollisuus.domain.StudentSearchParams
import fi.espoo.oppivelvollisuus.domain.StudentSummary
import fi.espoo.oppivelvollisuus.domain.ValpasNotifier
import minimalStudentAndCaseTestInput
import minimalStudentCaseTestInput
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import testUser
import testUserName
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudentCaseTests : FullApplicationTest() {
    @Autowired
    lateinit var controller: AppController

    @Test
    fun `create another student case with all data`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)
        controller.getStudent(testUser, studentId).cases.first().id.also { firstCaseId ->
            controller.updateStudentCaseStatus(
                testUser,
                studentId,
                firstCaseId,
                CaseStatusInput(CaseStatus.FINISHED, FinishedInfo(CaseFinishedReason.OTHER, null))
            )
        }

        val caseId =
            controller.createStudentCase(
                testUser,
                studentId,
                StudentCaseInput(
                    openedAt = LocalDate.of(2023, 12, 8),
                    assignedTo = testUser.id,
                    source = CaseSource.OTHER,
                    sourceValpas = null,
                    sourceOther = OtherNotifier.LASTENSUOJELU,
                    sourceContact = "Lastensuojelu, Minna Mikkola",
                    schoolBackground = SchoolBackground.entries.toSet(),
                    caseBackgroundReasons = CaseBackgroundReason.entries.toSet(),
                    notInSchoolReason = NotInSchoolReason.KATSOTTU_ERONNEEKSI_OPPILAITOKSESTA
                )
            )

        val studentResponse = controller.getStudent(testUser, studentId)
        assertEquals(2, studentResponse.cases.size)
        studentResponse.cases.first().let { studentCase ->
            assertEquals(
                StudentCase(
                    id = caseId,
                    studentId = studentId,
                    openedAt = LocalDate.of(2023, 12, 8),
                    assignedTo = UserBasics(id = testUser.id, name = testUserName),
                    status = CaseStatus.TODO,
                    finishedInfo = null,
                    source = CaseSource.OTHER,
                    sourceValpas = null,
                    sourceOther = OtherNotifier.LASTENSUOJELU,
                    sourceContact = "Lastensuojelu, Minna Mikkola",
                    schoolBackground = SchoolBackground.entries.toSet(),
                    caseBackgroundReasons = CaseBackgroundReason.entries.toSet(),
                    notInSchoolReason = NotInSchoolReason.KATSOTTU_ERONNEEKSI_OPPILAITOKSESTA,
                    events = emptyList()
                ),
                studentCase
            )
        }
    }

    @Test
    fun `create another student case with minimal data and update it`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)
        controller.getStudent(testUser, studentId).cases.first().id.also { firstCaseId ->
            controller.updateStudentCaseStatus(
                testUser,
                studentId,
                firstCaseId,
                CaseStatusInput(CaseStatus.FINISHED, FinishedInfo(CaseFinishedReason.OTHER, null))
            )
        }

        val caseId =
            controller.createStudentCase(
                testUser,
                studentId,
                StudentCaseInput(
                    openedAt = LocalDate.of(2023, 12, 8),
                    assignedTo = null,
                    source = CaseSource.VALPAS_AUTOMATIC_CHECK,
                    sourceValpas = null,
                    sourceOther = null,
                    sourceContact = "",
                    schoolBackground = emptySet(),
                    caseBackgroundReasons = emptySet(),
                    notInSchoolReason = null
                )
            )

        var studentResponse = controller.getStudent(testUser, studentId)
        assertEquals(2, studentResponse.cases.size)
        studentResponse.cases.first().let { studentCase ->
            assertEquals(
                StudentCase(
                    id = caseId,
                    studentId = studentId,
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
                    events = emptyList()
                ),
                studentCase
            )
        }

        controller.updateStudentCase(
            testUser,
            studentId,
            caseId,
            StudentCaseInput(
                openedAt = LocalDate.of(2023, 12, 9),
                assignedTo = testUser.id,
                source = CaseSource.VALPAS_NOTICE,
                sourceValpas = ValpasNotifier.LUKIO,
                sourceOther = null,
                sourceContact = "Espoon lukio",
                schoolBackground = setOf(SchoolBackground.EI_PERUSKOULUN_PAATTOTODISTUSTA),
                caseBackgroundReasons = setOf(CaseBackgroundReason.MOTIVAATION_PUUTE, CaseBackgroundReason.MUU_SYY),
                notInSchoolReason = NotInSchoolReason.EI_OLE_ALOITTANUT_VASTAANOTTAMASSAAN_OPISKELUPAIKASSA
            )
        )

        studentResponse = controller.getStudent(testUser, studentId)
        assertEquals(2, studentResponse.cases.size)
        studentResponse.cases.first().let { studentCase ->
            assertEquals(
                StudentCase(
                    id = caseId,
                    studentId = studentId,
                    openedAt = LocalDate.of(2023, 12, 9),
                    assignedTo = UserBasics(id = testUser.id, name = testUserName),
                    status = CaseStatus.TODO,
                    finishedInfo = null,
                    source = CaseSource.VALPAS_NOTICE,
                    sourceValpas = ValpasNotifier.LUKIO,
                    sourceOther = null,
                    sourceContact = "Espoon lukio",
                    schoolBackground = setOf(SchoolBackground.EI_PERUSKOULUN_PAATTOTODISTUSTA),
                    caseBackgroundReasons = setOf(CaseBackgroundReason.MOTIVAATION_PUUTE, CaseBackgroundReason.MUU_SYY),
                    notInSchoolReason = NotInSchoolReason.EI_OLE_ALOITTANUT_VASTAANOTTAMASSAAN_OPISKELUPAIKASSA,
                    events = emptyList()
                ),
                studentCase
            )
        }
    }

    @Test
    fun `cannot create another student case if all others are not finished`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)

        assertThrows<UnableToExecuteStatementException> {
            controller.createStudentCase(
                testUser,
                studentId,
                minimalStudentCaseTestInput
            )
        }.also { assertTrue { it.isUniqueConstraintViolation() } }
    }

    @Test
    fun `change status to ON_HOLD`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)
        val caseId = controller.getStudent(testUser, studentId).cases.first().id

        controller.updateStudentCaseStatus(testUser, studentId, caseId, CaseStatusInput(CaseStatus.ON_HOLD, null))

        val updatedCase = controller.getStudent(testUser, studentId).cases.first()
        assertEquals(CaseStatus.ON_HOLD, updatedCase.status)
        assertNull(updatedCase.finishedInfo)
    }

    @Test
    fun `change status to FINISHED`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)
        val caseId = controller.getStudent(testUser, studentId).cases.first().id

        controller.updateStudentCaseStatus(
            testUser,
            studentId,
            caseId,
            CaseStatusInput(CaseStatus.FINISHED, FinishedInfo(CaseFinishedReason.OTHER, null))
        )

        val updatedCase = controller.getStudent(testUser, studentId).cases.first()
        assertEquals(CaseStatus.FINISHED, updatedCase.status)
        assertEquals(CaseFinishedReason.OTHER, updatedCase.finishedInfo?.reason)
        assertNull(updatedCase.finishedInfo?.startedAtSchool)
    }

    @Test
    fun `change status to FINISHED with BEGAN_STUDIES`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)
        val caseId = controller.getStudent(testUser, studentId).cases.first().id

        controller.updateStudentCaseStatus(
            testUser,
            studentId,
            caseId,
            CaseStatusInput(
                CaseStatus.FINISHED,
                FinishedInfo(CaseFinishedReason.BEGAN_STUDIES, SchoolType.LUKIO)
            )
        )

        val updatedCase = controller.getStudent(testUser, studentId).cases.first()
        assertEquals(CaseStatus.FINISHED, updatedCase.status)
        assertEquals(CaseFinishedReason.BEGAN_STUDIES, updatedCase.finishedInfo?.reason)
        assertEquals(SchoolType.LUKIO, updatedCase.finishedInfo?.startedAtSchool)
    }

    @Test
    fun `cannot change status to FINISHED without reason`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)
        val caseId = controller.getStudent(testUser, studentId).cases.first().id

        assertThrows<BadRequest> {
            controller.updateStudentCaseStatus(
                testUser,
                studentId,
                caseId,
                CaseStatusInput(
                    CaseStatus.FINISHED,
                    null
                )
            )
        }
    }

    @Test
    fun `cannot change status to FINISHED with BEGAN_STUDIES without school type`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)
        val caseId = controller.getStudent(testUser, studentId).cases.first().id

        assertThrows<BadRequest> {
            controller.updateStudentCaseStatus(
                testUser,
                studentId,
                caseId,
                CaseStatusInput(
                    CaseStatus.FINISHED,
                    FinishedInfo(
                        CaseFinishedReason.BEGAN_STUDIES,
                        null
                    )
                )
            )
        }
    }

    @Test
    fun `cannot provide startedAtSchool when reason is not BEGAN_STUDIES`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)
        val caseId = controller.getStudent(testUser, studentId).cases.first().id

        assertThrows<BadRequest> {
            controller.updateStudentCaseStatus(
                testUser,
                studentId,
                caseId,
                CaseStatusInput(
                    CaseStatus.FINISHED,
                    FinishedInfo(
                        CaseFinishedReason.COMPULSORY_EDUCATION_SUSPENDED,
                        SchoolType.LUKIO
                    )
                )
            )
        }
    }

    @Test
    fun `reset status after finishing`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)
        val caseId = controller.getStudent(testUser, studentId).cases.first().id
        controller.updateStudentCaseStatus(
            testUser,
            studentId,
            caseId,
            CaseStatusInput(CaseStatus.FINISHED, FinishedInfo(CaseFinishedReason.BEGAN_STUDIES, SchoolType.LUKIO))
        )

        controller.updateStudentCaseStatus(
            testUser,
            studentId,
            caseId,
            CaseStatusInput(CaseStatus.TODO, null)
        )

        val updatedCase = controller.getStudent(testUser, studentId).cases.first()
        assertEquals(CaseStatus.TODO, updatedCase.status)
        assertNull(updatedCase.finishedInfo)
    }

    @Test
    fun `cannot reset status after finishing if there already is another unfinished case`() {
        val studentId = controller.createStudent(testUser, minimalStudentAndCaseTestInput)
        val caseId = controller.getStudent(testUser, studentId).cases.first().id
        controller.updateStudentCaseStatus(
            testUser,
            studentId,
            caseId,
            CaseStatusInput(CaseStatus.FINISHED, FinishedInfo(CaseFinishedReason.BEGAN_STUDIES, SchoolType.LUKIO))
        )
        controller.createStudentCase(testUser, studentId, minimalStudentCaseTestInput)

        assertThrows<UnableToExecuteStatementException> {
            controller.updateStudentCaseStatus(
                testUser,
                studentId,
                caseId,
                CaseStatusInput(CaseStatus.TODO, null)
            )
        }.also { assertTrue { it.isUniqueConstraintViolation() } }
    }

    @Test
    fun `deleting student case without events`() {
        val studentId =
            controller.createStudent(
                user = testUser,
                body = minimalStudentAndCaseTestInput
            )
        val caseId = controller.getStudent(testUser, studentId).cases.first().id

        controller.deleteStudentCase(testUser, studentId, caseId)

        assertEquals(0, controller.getStudent(testUser, studentId).cases.size)
        assertEquals(
            listOf(
                StudentSummary(
                    id = studentId,
                    firstName = minimalStudentAndCaseTestInput.student.firstName,
                    lastName = minimalStudentAndCaseTestInput.student.lastName,
                    openedAt = null,
                    status = null,
                    source = null,
                    assignedTo = null,
                    lastEvent = null
                )
            ),
            controller.getStudents(
                testUser,
                StudentSearchParams(
                    query = "",
                    statuses = CaseStatus.entries,
                    sources = CaseSource.entries,
                    assignee = null
                )
            )
        )
    }

    @Test
    fun `deleting student case with events fails`() {
        val studentId =
            controller.createStudent(
                user = testUser,
                body = minimalStudentAndCaseTestInput
            )
        val caseId = controller.getStudent(testUser, studentId).cases.first().id
        controller.createCaseEvent(
            testUser,
            caseId,
            CaseEventInput(
                date = LocalDate.of(2023, 12, 8),
                type = CaseEventType.NOTE,
                notes = "test"
            )
        )

        assertThrows<UnableToExecuteStatementException> {
            controller.deleteStudentCase(testUser, studentId, caseId)
        }
    }
}
