// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.domain.AppController
import fi.espoo.oppivelvollisuus.domain.CaseBackgroundReason
import fi.espoo.oppivelvollisuus.domain.CaseEventInput
import fi.espoo.oppivelvollisuus.domain.CaseEventType
import fi.espoo.oppivelvollisuus.domain.CaseFinishedReason
import fi.espoo.oppivelvollisuus.domain.CaseReportRow
import fi.espoo.oppivelvollisuus.domain.CaseSource
import fi.espoo.oppivelvollisuus.domain.CaseStatus
import fi.espoo.oppivelvollisuus.domain.CaseStatusInput
import fi.espoo.oppivelvollisuus.domain.FinishedInfo
import fi.espoo.oppivelvollisuus.domain.Gender
import fi.espoo.oppivelvollisuus.domain.NotInSchoolReason
import fi.espoo.oppivelvollisuus.domain.SchoolBackground
import fi.espoo.oppivelvollisuus.domain.SchoolType
import fi.espoo.oppivelvollisuus.domain.StudentCaseInput
import fi.espoo.oppivelvollisuus.domain.StudentInput
import fi.espoo.oppivelvollisuus.domain.ValpasNotifier
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import testUser
import java.time.LocalDate
import kotlin.test.assertEquals

class CasesReportTests : FullApplicationTest() {
    @Autowired
    lateinit var controller: AppController

    @Test
    fun `create new case event, then update it and finally delete it`() {
        val studentId =
            controller.createStudent(
                user = testUser,
                body =
                    AppController.StudentAndCaseInput(
                        student =
                            StudentInput(
                                valpasLink = "",
                                firstName = "Tupu",
                                lastName = "Ankka",
                                ssn = "",
                                dateOfBirth = LocalDate.of(2007, 3, 1),
                                language = "englanti",
                                phone = "",
                                email = "",
                                gender = Gender.MALE,
                                address = "",
                                municipalityInFinland = false,
                                guardianInfo = "",
                                supportContactsInfo = ""
                            ),
                        studentCase =
                            StudentCaseInput(
                                openedAt = LocalDate.of(2022, 5, 1),
                                assignedTo = null,
                                source = CaseSource.VALPAS_NOTICE,
                                sourceValpas = ValpasNotifier.PERUSOPETUS,
                                sourceOther = null,
                                sourceContact = "",
                                schoolBackground = setOf(SchoolBackground.PERUSKOULUN_PAATTOTODISTUS),
                                caseBackgroundReasons = setOf(CaseBackgroundReason.POISSAOLOT),
                                notInSchoolReason = NotInSchoolReason.EI_OLE_HAKEUTUNUT_JATKO_OPINTOIHIN
                            )
                    )
            )
        val caseId = controller.getStudent(testUser, studentId).cases.first().id

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
                    eventTypes = emptySet()
                )
            ),
            controller.getCasesReport(
                user = testUser,
                start = LocalDate.of(2022, 1, 1),
                end = LocalDate.of(2022, 12, 31)
            )
        )

        controller.createCaseEvent(
            testUser,
            caseId,
            CaseEventInput(
                date = LocalDate.of(2022, 5, 15),
                type = CaseEventType.HEARING_LETTER,
                notes = ""
            )
        )
        controller.createCaseEvent(
            testUser,
            caseId,
            CaseEventInput(
                date = LocalDate.of(2022, 5, 22),
                type = CaseEventType.HEARING,
                notes = ""
            )
        )
        controller.createCaseEvent(
            testUser,
            caseId,
            CaseEventInput(
                date = LocalDate.of(2022, 5, 25),
                type = CaseEventType.HEARING,
                notes = ""
            )
        )
        controller.updateStudentCaseStatus(
            testUser,
            studentId,
            caseId,
            CaseStatusInput(
                status = CaseStatus.FINISHED,
                finishedInfo =
                    FinishedInfo(
                        reason = CaseFinishedReason.BEGAN_STUDIES,
                        startedAtSchool = SchoolType.LUKIO
                    )
            )
        )

        controller.getCasesReport(user = testUser, start = null, end = null).first().also { row ->
            assertEquals(CaseStatus.FINISHED, row.status)
            assertEquals(CaseFinishedReason.BEGAN_STUDIES, row.finishedReason)
            assertEquals(SchoolType.LUKIO, row.startedAtSchool)
            assertEquals(setOf(CaseEventType.HEARING_LETTER, CaseEventType.HEARING), row.eventTypes)
        }
    }
}
