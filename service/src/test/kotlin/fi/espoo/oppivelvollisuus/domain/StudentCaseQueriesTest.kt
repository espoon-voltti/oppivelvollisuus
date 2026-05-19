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
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StudentCaseQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0))
    private val user = DevUser()
    private val student = DevStudent(createdBy = user.id, created = now)

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(user)
            tx.insert(student)
        }
    }

    private fun minimalCaseInput(openedAt: LocalDate = LocalDate.of(2023, 12, 7)) =
        StudentCaseInput(
            openedAt = openedAt,
            assignedTo = null,
            source = CaseSource.VALPAS_AUTOMATIC_CHECK,
            sourceValpas = null,
            sourceOther = null,
            sourceContact = "",
            schoolBackground = emptySet(),
            caseBackgroundReasons = emptySet(),
            notInSchoolReason = null,
        )

    @Test
    fun `insertStudentCase persists case and returns id`() {
        val id = db.transaction {
            it.insertStudentCase(student.id, minimalCaseInput(), createdBy = user.id, now = now)
        }

        val cases = db.read { it.getStudentCasesByStudent(student.id) }
        assertEquals(listOf(id), cases.map { it.id })
        assertEquals(CaseStatus.TODO, cases.first().status)
        assertEquals(LocalDate.of(2023, 12, 7), cases.first().openedAt)
    }

    @Test
    fun `getStudentCasesByStudent returns empty list for student without cases`() {
        assertEquals(emptyList(), db.read { it.getStudentCasesByStudent(student.id) })
    }

    @Test
    fun `getStudentCasesByStudent returns cases ordered by opened_at DESC`() {
        val older =
            DevStudentCase(
                studentId = student.id,
                createdBy = user.id,
                created = now,
                openedAt = LocalDate.of(2022, 1, 1),
            )
        db.transaction { tx -> tx.insert(older) }
        // partial unique index allows at most one non-FINISHED case per student
        db.transaction {
            it.updateStudentCaseStatus(
                id = older.id,
                studentId = student.id,
                data =
                    CaseStatusInput(
                        status = CaseStatus.FINISHED,
                        finishedInfo =
                            FinishedInfo(
                                reason = CaseFinishedReason.OTHER,
                                startedAtSchool = null,
                                followUpMeasures = null,
                                otherReason = "test",
                            ),
                    ),
                updatedBy = user.id,
                now = now,
            )
        }
        val newer =
            DevStudentCase(
                studentId = student.id,
                createdBy = user.id,
                created = now,
                openedAt = LocalDate.of(2023, 1, 1),
            )
        db.transaction { tx -> tx.insert(newer) }

        val cases = db.read { it.getStudentCasesByStudent(student.id) }

        assertEquals(listOf(newer.id, older.id), cases.map { it.id })
    }

    @Test
    fun `updateStudentCase overwrites the fields`() {
        val case = DevStudentCase(studentId = student.id, createdBy = user.id, created = now)
        db.transaction { tx -> tx.insert(case) }

        db.transaction {
            it.updateStudentCase(
                id = case.id,
                studentId = student.id,
                data =
                    StudentCaseInput(
                        openedAt = LocalDate.of(2024, 6, 6),
                        assignedTo = user.id,
                        source = CaseSource.VALPAS_NOTICE,
                        sourceValpas = ValpasNotifier.LUKIO,
                        sourceOther = null,
                        sourceContact = "contact",
                        schoolBackground = setOf(SchoolBackground.PERUSKOULUN_PAATTOTODISTUS),
                        caseBackgroundReasons = setOf(CaseBackgroundReason.POISSAOLOT),
                        notInSchoolReason = NotInSchoolReason.EI_OLE_HAKEUTUNUT_JATKO_OPINTOIHIN,
                    ),
                updatedBy = user.id,
                now = now,
            )
        }

        val persisted = db.read { it.getStudentCasesByStudent(student.id) }.first()
        assertEquals(LocalDate.of(2024, 6, 6), persisted.openedAt)
        assertEquals(CaseSource.VALPAS_NOTICE, persisted.source)
        assertEquals(ValpasNotifier.LUKIO, persisted.sourceValpas)
        assertEquals("contact", persisted.sourceContact)
        assertEquals(user.id, persisted.assignedTo?.id)
    }

    @Test
    fun `updateStudentCaseStatus to FINISHED writes finished info`() {
        val case = DevStudentCase(studentId = student.id, createdBy = user.id, created = now)
        db.transaction { tx -> tx.insert(case) }

        db.transaction {
            it.updateStudentCaseStatus(
                id = case.id,
                studentId = student.id,
                data =
                    CaseStatusInput(
                        status = CaseStatus.FINISHED,
                        finishedInfo =
                            FinishedInfo(
                                reason = CaseFinishedReason.BEGAN_STUDIES,
                                startedAtSchool = SchoolType.LUKIO,
                                followUpMeasures = null,
                                otherReason = null,
                            ),
                    ),
                updatedBy = user.id,
                now = now,
            )
        }

        val persisted = db.read { it.getStudentCasesByStudent(student.id) }.first()
        assertEquals(CaseStatus.FINISHED, persisted.status)
        assertEquals(CaseFinishedReason.BEGAN_STUDIES, persisted.finishedInfo?.reason)
        assertEquals(SchoolType.LUKIO, persisted.finishedInfo?.startedAtSchool)
    }

    @Test
    fun `updateStudentCaseStatus to TODO clears finished info`() {
        val case = DevStudentCase(studentId = student.id, createdBy = user.id, created = now)
        db.transaction { tx -> tx.insert(case) }
        db.transaction {
            it.updateStudentCaseStatus(
                id = case.id,
                studentId = student.id,
                data =
                    CaseStatusInput(
                        status = CaseStatus.FINISHED,
                        finishedInfo =
                            FinishedInfo(
                                reason = CaseFinishedReason.OTHER,
                                startedAtSchool = null,
                                followUpMeasures = null,
                                otherReason = "x",
                            ),
                    ),
                updatedBy = user.id,
                now = now,
            )
        }

        db.transaction {
            it.updateStudentCaseStatus(
                id = case.id,
                studentId = student.id,
                data = CaseStatusInput(status = CaseStatus.TODO, finishedInfo = null),
                updatedBy = user.id,
                now = now,
            )
        }

        val persisted = db.read { it.getStudentCasesByStudent(student.id) }.first()
        assertEquals(CaseStatus.TODO, persisted.status)
        assertNull(persisted.finishedInfo)
    }

    @Test
    fun `deleteStudentCase removes the row`() {
        val case = DevStudentCase(studentId = student.id, createdBy = user.id, created = now)
        db.transaction { tx -> tx.insert(case) }

        db.transaction { it.deleteStudentCase(case.id, student.id) }

        assertEquals(emptyList(), db.read { it.getStudentCasesByStudent(student.id) })
    }
}
