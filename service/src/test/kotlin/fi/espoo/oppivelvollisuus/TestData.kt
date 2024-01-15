// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fi.espoo.oppivelvollisuus.config.AuthenticatedUser
import fi.espoo.oppivelvollisuus.domain.AppController
import fi.espoo.oppivelvollisuus.domain.CaseSource
import fi.espoo.oppivelvollisuus.domain.StudentCaseInput
import fi.espoo.oppivelvollisuus.domain.StudentInput
import java.time.LocalDate
import java.util.UUID

val testUser = AuthenticatedUser(UUID.randomUUID())
val testUserName = "Teija Testaaja"

val minimalStudentTestInput =
    StudentInput(
        valpasLink = "",
        ssn = "",
        firstName = "Testi",
        lastName = "Testilä",
        language = "",
        dateOfBirth = LocalDate.now().minusYears(16),
        phone = "",
        email = "",
        gender = null,
        address = "",
        municipalityInFinland = true,
        guardianInfo = "",
        supportContactsInfo = ""
    )
val minimalStudentCaseTestInput =
    StudentCaseInput(
        openedAt = LocalDate.of(2023, 12, 7),
        assignedTo = null,
        source = CaseSource.VALPAS_AUTOMATIC_CHECK,
        sourceValpas = null,
        sourceOther = null,
        sourceContact = "",
        schoolBackground = emptySet(),
        caseBackgroundReasons = emptySet(),
        notInSchoolReason = null
    )
val minimalStudentAndCaseTestInput =
    AppController.StudentAndCaseInput(
        student = minimalStudentTestInput,
        studentCase = minimalStudentCaseTestInput
    )
