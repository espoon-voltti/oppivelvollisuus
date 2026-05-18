// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.pages

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import fi.espoo.oppivelvollisuus.StudentCaseId
import fi.espoo.oppivelvollisuus.baseUrl
import fi.espoo.oppivelvollisuus.dataQa
import java.util.regex.Pattern

class StudentPage(private val page: Page) {
    val studentName = page.dataQa("student-name")
    val toggleStudentDetails = page.dataQa("toggle-student-details")
    val editStudentButton = page.dataQa("edit-student-button")
    val saveStudentButton = page.dataQa("save-student-button")
    val deleteStudentButton = page.dataQa("delete-student-button")
    val ssnValue = page.dataQa("ssn-value")
    val ssnInput = page.dataQa("ssn-input")
    val addCaseButton = page.dataQa("add-case-button")
    val saveCaseButton = page.dataQa("save-case-button")
    val caseRows = page.locator("[data-qa^='case-row-']")

    fun caseRow(id: StudentCaseId) = CaseRow(page.dataQa("case-row-$id"))

    fun assertUrl() {
        assertThat(page).hasURL(Pattern.compile("$baseUrl/oppivelvolliset/[a-f0-9\\-]+"))
    }
}
