// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.domain

import fi.espoo.oppivelvollisuus.FullApplicationTestOld
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import testUser

class CasesReportTests : FullApplicationTestOld() {
    @Autowired lateinit var controller: AppController

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
                                supportContactsInfo = "",
                            ),
                        studentCase =
                            StudentCaseInput(
                                openedAt = LocalDate.of(2022, 5, 1),
                                assignedTo = null,
                                source = CaseSource.VALPAS_NOTICE,
                                sourceValpas = ValpasNotifier.PERUSOPETUS,
                                sourceOther = null,
                                sourceContact = "",
                                schoolBackground =
                                    setOf(SchoolBackground.PERUSKOULUN_PAATTOTODISTUS),
                                caseBackgroundReasons = setOf(CaseBackgroundReason.POISSAOLOT),
                                notInSchoolReason =
                                    NotInSchoolReason.EI_OLE_HAKEUTUNUT_JATKO_OPINTOIHIN,
                            ),
                    ),
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
                    eventTypes = emptySet(),
                    followUpMeasures = null,
                    partnerOrganisations = emptySet(),
                )
            ),
            controller.getCasesReport(
                user = testUser,
                start = LocalDate.of(2022, 1, 1),
                end = LocalDate.of(2022, 12, 31),
            ),
        )

        controller.createCaseEvent(
            testUser,
            caseId,
            CaseEventInput(
                date = LocalDate.of(2022, 5, 15),
                type = CaseEventType.HEARING_LETTER,
                notes = "",
            ),
        )
        controller.createCaseEvent(
            testUser,
            caseId,
            CaseEventInput(
                date = LocalDate.of(2022, 5, 22),
                type = CaseEventType.HEARING,
                notes = "",
            ),
        )
        controller.createCaseEvent(
            testUser,
            caseId,
            CaseEventInput(
                date = LocalDate.of(2022, 5, 25),
                type = CaseEventType.HEARING,
                notes = "",
            ),
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
                        startedAtSchool = SchoolType.LUKIO,
                        followUpMeasures = null,
                        otherReason = null,
                    ),
            ),
        )
        controller.getCasesReport(user = testUser, start = null, end = null).first().also { row ->
            assertEquals(CaseStatus.FINISHED, row.status)
            assertEquals(CaseFinishedReason.BEGAN_STUDIES, row.finishedReason)
            assertEquals(SchoolType.LUKIO, row.startedAtSchool)
            assertEquals(setOf(CaseEventType.HEARING_LETTER, CaseEventType.HEARING), row.eventTypes)
        }

        controller.updateStudentCaseStatus(
            testUser,
            studentId,
            caseId,
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

        controller.getCasesReport(user = testUser, start = null, end = null).first().also { row ->
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
