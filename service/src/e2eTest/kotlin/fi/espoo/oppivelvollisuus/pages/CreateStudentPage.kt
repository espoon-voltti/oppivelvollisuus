// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.oppivelvollisuus.pages

import com.microsoft.playwright.Page
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import fi.espoo.oppivelvollisuus.baseUrl
import fi.espoo.oppivelvollisuus.dataQa
import kotlin.test.assertEquals

class CreateStudentPage(private val page: Page) {
    val saveButton = page.dataQa("save-button")
    val dateOfBirthInput = page.dataQa("date-of-birth-input")
    val lastNameInput = page.dataQa("last-name-input")
    val firstNameInput = page.dataQa("first-name-input")
    val sourceSelect = page.dataQa("source-select")
    val employeeSelect = page.dataQa("employee-select")

    fun assertUrl() {
        assertThat(page).hasURL("$baseUrl/oppivelvolliset/uusi")
    }

    fun assertEmployeeSelectOptions(expected: List<String>) {
        assertEquals(expected.joinToString(""), employeeSelect.allTextContents().joinToString())
    }
}
