// SPDX-FileCopyrightText: 2023-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CasesReportTests : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: AppController

    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0))
    private val clock = MockAppClock(now)

    private val testUser = DevUser()

    @BeforeEach
    fun setup() {
        db.transaction { tx -> tx.insert(testUser) }
    }

    private fun getCasesReport(start: LocalDate? = null, end: LocalDate? = null) =
        controller.getCasesReport(dbInstance(), testUser.user, start, end)

    private fun createCaseEvent(caseId: StudentCaseId, body: CaseEventInput) =
        controller.createCaseEvent(dbInstance(), testUser.user, clock, caseId, body)

    private fun updateStudentCaseStatus(
        studentId: fi.espoo.oppivelvollisuus.StudentId,
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

    @Test
    fun `create new case event, then update it and finally delete it`() {
        val student =
            DevStudent(
                createdBy = testUser.id,
                created = now,
                firstName = "Tupu",
                lastName = "Ankka",
                language = "englanti",
                dateOfBirth = LocalDate.of(2007, 3, 1),
                gender = Gender.MALE,
                municipalityInFinland = false,
            )
        val case =
            DevStudentCase(
                studentId = student.id,
                createdBy = testUser.id,
                created = now,
                openedAt = LocalDate.of(2022, 5, 1),
                source = CaseSource.VALPAS_NOTICE,
                sourceValpas = ValpasNotifier.PERUSOPETUS,
                schoolBackground = setOf(SchoolBackground.PERUSKOULUN_PAATTOTODISTUS),
                caseBackgroundReasons = setOf(CaseBackgroundReason.POISSAOLOT),
                notInSchoolReason = NotInSchoolReason.EI_OLE_HAKEUTUNUT_JATKO_OPINTOIHIN,
            )
        db.transaction { tx ->
            tx.insert(student)
            tx.insert(case)
        }

        assertEquals(
            listOf(
                CaseReportRow(
                    openedAt = LocalDate.of(2022, 5, 1),
                    birthYear = 2007,
                    ageAtCaseOpened = 15,
                    gender = Gender.MALE,
                    language = "englanti",
                    municipalityInFinland = false,
                    status = CaseStatus.TODO,
                    finishedReason = null,
                    startedAtSchool = null,
                    source = CaseSource.VALPAS_NOTICE,
                    sourceValpas = ValpasNotifier.PERUSOPETUS,
                    sourceOther = null,
                    schoolBackground = setOf(SchoolBackground.PERUSKOULUN_PAATTOTODISTUS),
                    caseBackgroundReasons = setOf(CaseBackgroundReason.POISSAOLOT),
                    notInSchoolReason = NotInSchoolReason.EI_OLE_HAKEUTUNUT_JATKO_OPINTOIHIN,
                    eventTypes = emptySet(),
                    followUpMeasures = null,
                    partnerOrganisations = emptySet(),
                )
            ),
            getCasesReport(start = LocalDate.of(2022, 1, 1), end = LocalDate.of(2022, 12, 31)),
        )

        createCaseEvent(
            case.id,
            CaseEventInput(
                date = LocalDate.of(2022, 5, 15),
                type = CaseEventType.HEARING_LETTER,
                notes = "",
            ),
        )
        createCaseEvent(
            case.id,
            CaseEventInput(
                date = LocalDate.of(2022, 5, 22),
                type = CaseEventType.HEARING,
                notes = "",
            ),
        )
        createCaseEvent(
            case.id,
            CaseEventInput(
                date = LocalDate.of(2022, 5, 25),
                type = CaseEventType.HEARING,
                notes = "",
            ),
        )
        updateStudentCaseStatus(
            student.id,
            case.id,
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
        )
        getCasesReport().first().also { row ->
            assertEquals(CaseStatus.FINISHED, row.status)
            assertEquals(CaseFinishedReason.BEGAN_STUDIES, row.finishedReason)
            assertEquals(SchoolType.LUKIO, row.startedAtSchool)
            assertEquals(setOf(CaseEventType.HEARING_LETTER, CaseEventType.HEARING), row.eventTypes)
        }

        updateStudentCaseStatus(
            student.id,
            case.id,
            CaseStatusInput(
                status = CaseStatus.FINISHED,
                finishedInfo =
                    FinishedInfo(
                        reason = CaseFinishedReason.COMPULSORY_EDUCATION_ENDED,
                        startedAtSchool = null,
                        followUpMeasures =
                            setOf(FollowUpMeasure.SOCIAL_SERVICES, FollowUpMeasure.LANGUAGE_COURSE),
                        otherReason = null,
                    ),
            ),
        )

        getCasesReport().first().also { row ->
            assertEquals(CaseStatus.FINISHED, row.status)
            assertEquals(CaseFinishedReason.COMPULSORY_EDUCATION_ENDED, row.finishedReason)
            assertEquals(
                setOf(FollowUpMeasure.SOCIAL_SERVICES, FollowUpMeasure.LANGUAGE_COURSE),
                row.followUpMeasures,
            )
            assertEquals(setOf(CaseEventType.HEARING_LETTER, CaseEventType.HEARING), row.eventTypes)
        }
    }
}
